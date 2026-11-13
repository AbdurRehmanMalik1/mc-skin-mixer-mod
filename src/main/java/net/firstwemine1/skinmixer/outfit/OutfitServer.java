package net.firstwemine1.skinmixer.outfit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server half: keeps every player's outfit, and tells every client with the mod who wears what.
 * Clients without the mod are never sent anything and see real skins.
 */
public final class OutfitServer {
	/** Half a second between changes, so a client can't flood everyone with outfit packets. */
	private static final int CHANGE_COOLDOWN_TICKS = 10;

	private static OutfitStore store;
	private static final Map<UUID, Integer> LAST_CHANGE = new HashMap<>();
	/** The newest change asked for during a player's cooldown, applied when it ends. */
	private static final Map<UUID, Optional<Outfit>> PENDING = new HashMap<>();

	private OutfitServer() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SetOutfitPayload.TYPE, SetOutfitPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OutfitPayload.TYPE, OutfitPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SetOutfitPayload.TYPE,
				(payload, context) -> requestChange(context.player(), payload.outfit()));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> store = OutfitStore.load(server.getWorldPath(LevelResource.ROOT)));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			store = null;
			LAST_CHANGE.clear();
			PENDING.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(OutfitServer::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player, server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			// A change still waiting is applied anyway: the player pressed Apply before leaving.
			Optional<Outfit> waiting = PENDING.remove(handler.player.getUUID());
			if (waiting != null) {
				setOutfit(server, handler.player.getUUID(), waiting.orElse(null));
			}
			LAST_CHANGE.remove(handler.player.getUUID());
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
	}

	/** A client pressed Apply. */
	private static void requestChange(ServerPlayer player, Optional<Outfit> outfit) {
		if (store == null) {
			return;
		}
		if (outfit.isPresent() && !outfit.get().isValid()) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		Integer last = LAST_CHANGE.get(player.getUUID());
		if (last != null && server.getTickCount() - last < CHANGE_COOLDOWN_TICKS) {
			// Too soon: the newest request waits for the cooldown, and replaces any older waiting one.
			PENDING.put(player.getUUID(), outfit);
			return;
		}
		LAST_CHANGE.put(player.getUUID(), server.getTickCount());
		setOutfit(server, player.getUUID(), outfit.orElse(null));
	}

	/** Applies waiting requests whose cooldown is over. */
	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Optional<Outfit>>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Optional<Outfit>> entry = it.next();
			Integer last = LAST_CHANGE.get(entry.getKey());
			if (last == null || server.getTickCount() - last >= CHANGE_COOLDOWN_TICKS) {
				it.remove();
				LAST_CHANGE.put(entry.getKey(), server.getTickCount());
				setOutfit(server, entry.getKey(), entry.getValue().orElse(null));
			}
		}
	}

	/** Stores the outfit and tells every client with the mod. Null means the player's own skin. */
	public static void setOutfit(MinecraftServer server, UUID player, Outfit outfit) {
		if (store == null) {
			return;
		}
		store.put(player, outfit);
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			sendTo(viewer, player, Optional.ofNullable(outfit));
		}
	}

	/** The newcomer learns everyone's outfit; everyone else learns the newcomer's. */
	private static void onJoin(ServerPlayer joined, MinecraftServer server) {
		if (store == null) {
			return;
		}
		Outfit own = store.get(joined.getUUID());
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			if (other == joined) {
				continue;
			}
			Outfit theirs = store.get(other.getUUID());
			if (theirs != null) {
				sendTo(joined, other.getUUID(), Optional.of(theirs));
			}
			if (own != null) {
				sendTo(other, joined.getUUID(), Optional.of(own));
			}
		}
		if (own != null) {
			sendTo(joined, joined.getUUID(), Optional.of(own));
		}
	}

	private static void sendTo(ServerPlayer viewer, UUID player, Optional<Outfit> outfit) {
		if (ServerPlayNetworking.canSend(viewer, OutfitPayload.TYPE)) {
			ServerPlayNetworking.send(viewer, new OutfitPayload(player, outfit));
		}
	}

	/** {@code /skinmixer reset} for yourself; {@code /skinmixer reset <players>} for operators. */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("skinmixer")
				.then(Commands.literal("reset")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							setOutfit(context.getSource().getServer(), player.getUUID(), null);
							context.getSource().sendSuccess(() -> Component.literal("Your outfit was reset to your own skin."), false);
							return 1;
						})
						.then(Commands.argument("players", EntityArgument.players())
								.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
								.executes(context -> {
									Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
									for (ServerPlayer player : players) {
										setOutfit(context.getSource().getServer(), player.getUUID(), null);
									}
									context.getSource().sendSuccess(
											() -> Component.literal("Reset the outfit of " + players.size() + " player(s)."), true);
									return players.size();
								}))));
	}
}
