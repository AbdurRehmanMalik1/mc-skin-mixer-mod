package net.firstwemine1.skinmixer.client.mixin;

import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.firstwemine1.skinmixer.client.PlayerSkin;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every place the client draws a player's skin asks {@code NetworkPlayerInfo} for it: the player model and the
 * first-person hand (through {@code AbstractClientPlayer}), and the tab list. On this version the
 * texture and the arm model are asked for separately, so both answers are swapped.
 */
@Mixin(NetworkPlayerInfo.class)
abstract class PlayerInfoMixin {
	@Inject(method = "getSkinLocation", at = @At("RETURN"), cancellable = true)
	private void skinmixer$wearOutfit(CallbackInfoReturnable<ResourceLocation> cir) {
		PlayerSkin dressed = ClientOutfits.dressed((NetworkPlayerInfo) (Object) this);
		if (dressed != null) {
			cir.setReturnValue(dressed.texture());
		}
	}

	@Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
	private void skinmixer$wearOutfitModel(CallbackInfoReturnable<String> cir) {
		PlayerSkin dressed = ClientOutfits.dressed((NetworkPlayerInfo) (Object) this);
		if (dressed != null) {
			cir.setReturnValue(dressed.model().id());
		}
	}
}
