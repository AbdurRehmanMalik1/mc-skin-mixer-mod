package net.firstwemine1.skinmixer.outfit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The two packets, on one Forge channel that either side may lack: clients without the mod can join a
 * server with it, and the other way round.
 * <ul>
 * <li>{@link SetOutfit}, client to server: the player pressed Apply (an outfit, or none for their own skin).</li>
 * <li>{@link OutfitOf}, server to client: what a player wears now (a player id, then an outfit or none).</li>
 * </ul>
 */
public final class OutfitNetwork {
	private static final String PROTOCOL = "1";
	private static final int MAX_ID_LENGTH = 64;
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SkinMixer.id("main"), () -> PROTOCOL,
			NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

	private OutfitNetwork() {
	}

	public static void register() {
		CHANNEL.registerMessage(0, SetOutfit.class, SetOutfit::write, SetOutfit::read, SetOutfit::handle,
				Optional.of(NetworkDirection.PLAY_TO_SERVER));
		CHANNEL.registerMessage(1, OutfitOf.class, OutfitOf::write, OutfitOf::read, OutfitOf::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

	/** Whether this player's client has the mod. */
	public static boolean canSend(ServerPlayer player) {
		return CHANNEL.isRemotePresent(player.connection.connection);
	}

	public static void sendTo(ServerPlayer viewer, UUID player, Optional<Outfit> outfit) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new OutfitOf(player, outfit));
	}

	/** Client side: whether the server we're on has the mod. */
	public static boolean serverHasMod() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getConnection() != null && CHANNEL.isRemotePresent(minecraft.getConnection().getConnection());
	}

	public static void sendToServer(Optional<Outfit> outfit) {
		CHANNEL.sendToServer(new SetOutfit(outfit));
	}

	/** Client to server: the player pressed Apply. */
	public static final class SetOutfit {
		final Optional<Outfit> outfit;

		SetOutfit(Optional<Outfit> outfit) {
			this.outfit = outfit;
		}

		static void write(SetOutfit message, FriendlyByteBuf buf) {
			writeOptional(buf, message.outfit);
		}

		static SetOutfit read(FriendlyByteBuf buf) {
			return new SetOutfit(readOptional(buf));
		}

		static void handle(SetOutfit message, Supplier<NetworkEvent.Context> context) {
			ServerPlayer player = context.get().getSender();
			if (player != null) {
				context.get().enqueueWork(() -> OutfitServer.requestChange(player, message.outfit));
			}
			context.get().setPacketHandled(true);
		}
	}

	/** Server to client: what a player wears now. */
	public static final class OutfitOf {
		final UUID player;
		final Optional<Outfit> outfit;

		OutfitOf(UUID player, Optional<Outfit> outfit) {
			this.player = player;
			this.outfit = outfit;
		}

		static void write(OutfitOf message, FriendlyByteBuf buf) {
			buf.writeUUID(message.player);
			writeOptional(buf, message.outfit);
		}

		static OutfitOf read(FriendlyByteBuf buf) {
			return new OutfitOf(buf.readUUID(), readOptional(buf));
		}

		static void handle(OutfitOf message, Supplier<NetworkEvent.Context> context) {
			context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> ClientOutfits.receive(message.player, message.outfit)));
			context.get().setPacketHandled(true);
		}
	}

	public static void writeOptional(FriendlyByteBuf buf, Optional<Outfit> outfit) {
		buf.writeBoolean(outfit.isPresent());
		outfit.ifPresent(o -> {
			buf.writeBoolean(o.slim());
			buf.writeVarInt(o.parts().size());
			for (String part : o.parts()) {
				buf.writeUtf(part, MAX_ID_LENGTH);
			}
		});
	}

	/** Reads what {@link #writeOptional} wrote. A malformed outfit throws, which drops the packet. */
	public static Optional<Outfit> readOptional(FriendlyByteBuf buf) {
		if (!buf.readBoolean()) {
			return Optional.empty();
		}
		boolean slim = buf.readBoolean();
		int count = buf.readVarInt();
		if (count < 0 || count > 16) {
			throw new IllegalArgumentException("Bad outfit size " + count);
		}
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			parts.add(buf.readUtf(MAX_ID_LENGTH));
		}
		return Optional.of(new Outfit(slim, parts));
	}
}
