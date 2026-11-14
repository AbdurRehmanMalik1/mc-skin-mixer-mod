package net.firstwemine1.skinmixer.outfit;

import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * The two packets, as plain channels: this version predates payload records.
 * <ul>
 * <li>{@link #SET_OUTFIT}, client to server: the player pressed Apply (an outfit, or none for their own skin).</li>
 * <li>{@link #OUTFIT}, server to client: what a player wears now (a player id, then an outfit or none).</li>
 * </ul>
 */
public final class OutfitNetwork {
	public static final ResourceLocation SET_OUTFIT = SkinMixer.id("set_outfit");
	public static final ResourceLocation OUTFIT = SkinMixer.id("outfit");
	private static final int MAX_ID_LENGTH = 64;

	private OutfitNetwork() {
	}

	public static FriendlyByteBuf setOutfit(Optional<Outfit> outfit) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		writeOptional(buf, outfit);
		return buf;
	}

	public static FriendlyByteBuf outfit(UUID player, Optional<Outfit> outfit) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(player);
		writeOptional(buf, outfit);
		return buf;
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
		java.util.List<String> parts = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			parts.add(buf.readUtf(MAX_ID_LENGTH));
		}
		return Optional.of(new Outfit(slim, parts));
	}
}
