package net.firstwemine1.skinmixer.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.firstwemine1.skinmixer.client.mixin.PlayerInfoAccessor;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.firstwemine1.skinmixer.outfit.SetOutfitPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Who wears what, on this client, and the skin the game should draw for each of them. Fed by the
 * server, or, on a server without the mod, by this client alone for its own player.
 */
public final class ClientOutfits {
	private static final Map<UUID, Outfit> OUTFITS = new HashMap<>();
	private static final Map<UUID, Applied> APPLIED = new HashMap<>();

	/** The last skin worked out for a player, reused until their outfit or real skin changes. */
	private record Applied(Outfit outfit, PlayerSkin original, PlayerSkin result, String key) {
	}

	private ClientOutfits() {
	}

	/** Called for every skin lookup (every frame, per player): must stay cheap when nothing changed. */
	public static PlayerSkin apply(UUID player, PlayerSkin original) {
		Outfit outfit = OUTFITS.get(player);
		if (outfit == null) {
			return original;
		}
		Applied applied = APPLIED.get(player);
		if (applied != null && applied.outfit().equals(outfit) && applied.original().equals(original)) {
			return applied.result();
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			return applied != null ? applied.result() : original;
		}
		ResourceLocation texture = SkinBaker.bake(outfit, original);
		// A source that can't be read yet (a skin still downloading) shows the real skin; the lookup
		// is retried when the real skin changes.
		PlayerSkin result = texture == null ? original : SkinBaker.skinOf(texture, outfit.slim(), original);
		APPLIED.put(player, new Applied(outfit, original, result, SkinBaker.key(outfit, original)));
		return result;
	}

	public static Outfit get(UUID player) {
		return OUTFITS.get(player);
	}

	/** The server says what {@code player} wears now. */
	public static void receive(UUID player, Optional<Outfit> outfit) {
		if (outfit.isPresent() && outfit.get().isValid()) {
			OUTFITS.put(player, outfit.get());
		} else {
			OUTFITS.remove(player);
		}
		APPLIED.remove(player);
	}

	/** Whether the server has Skin Mixer: if not, outfits are only seen by this client. */
	public static boolean serverHasMod() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getConnection() != null && minecraft.getConnection().hasChannel(SetOutfitPayload.TYPE);
	}

	/**
	 * The local player chose an outfit (null for their own skin). Remembered for servers without the mod,
	 * and sent to the server if it has the mod; otherwise applied here only.
	 */
	public static void choose(Outfit outfit) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		LocalOutfit.save(outfit);
		if (serverHasMod()) {
			PacketDistributor.sendToServer(new SetOutfitPayload(Optional.ofNullable(outfit)));
		} else {
			receive(minecraft.player.getUUID(), Optional.ofNullable(outfit));
		}
	}

	/** Joined a server without the mod: wear the last outfit, visible to this client only. */
	static void joinedWithoutServerMod(UUID self) {
		Outfit saved = LocalOutfit.load();
		if (saved != null) {
			receive(self, Optional.of(saved));
		}
	}

	/** A player's real skin, ignoring any outfit. */
	public static PlayerSkin originalSkin(UUID player) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
		if (info == null) {
			return DefaultPlayerSkin.get(player);
		}
		return ((PlayerInfoAccessor) info).skinmixer$skinLookup().get();
	}

	/** A skin fetched from Mojang arrived (or failed): outfits that waited for it are worked out again. */
	static void remoteSkinChanged() {
		// Only the ones that fell back to the real skin; finished outfits keep their texture.
		APPLIED.values().removeIf(applied -> applied.result() == applied.original());
	}

	/** Every so often: free baked textures no one wears any more. */
	static void collectGarbage() {
		Set<String> inUse = new HashSet<>();
		for (Applied applied : APPLIED.values()) {
			inUse.add(applied.key());
		}
		SkinBaker.retainOnly(inUse);
	}

	static void clear() {
		OUTFITS.clear();
		APPLIED.clear();
		SkinBaker.clear();
		RemoteSkins.forgetFailures();
	}
}
