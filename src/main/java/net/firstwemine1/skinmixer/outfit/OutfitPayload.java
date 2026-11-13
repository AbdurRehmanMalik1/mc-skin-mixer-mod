package net.firstwemine1.skinmixer.outfit;

import java.util.Optional;
import java.util.UUID;

import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: what a player wears now. Empty means their own skin. */
public record OutfitPayload(UUID player, Optional<Outfit> outfit) implements CustomPacketPayload {
	public static final Type<OutfitPayload> TYPE = new Type<>(SkinMixer.id("outfit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OutfitPayload> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC.cast(), OutfitPayload::player,
			ByteBufCodecs.optional(Outfit.STREAM_CODEC), OutfitPayload::outfit,
			OutfitPayload::new);

	@Override
	public Type<OutfitPayload> type() {
		return TYPE;
	}
}
