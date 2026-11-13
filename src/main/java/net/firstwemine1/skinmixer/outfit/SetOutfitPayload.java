package net.firstwemine1.skinmixer.outfit;

import java.util.Optional;

import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed Apply. Empty means "back to my own skin". */
public record SetOutfitPayload(Optional<Outfit> outfit) implements CustomPacketPayload {
	public static final Type<SetOutfitPayload> TYPE = new Type<>(SkinMixer.id("set_outfit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetOutfitPayload> CODEC =
			ByteBufCodecs.optional(Outfit.STREAM_CODEC).map(SetOutfitPayload::new, SetOutfitPayload::outfit);

	@Override
	public Type<SetOutfitPayload> type() {
		return TYPE;
	}
}
