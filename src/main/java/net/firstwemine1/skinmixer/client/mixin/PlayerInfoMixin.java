package net.firstwemine1.skinmixer.client.mixin;

import com.mojang.authlib.GameProfile;
import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every place the client draws a player's skin asks {@code PlayerInfo.getSkin()}: the player model
 * ({@code AbstractClientPlayer.getSkin()} delegates here), the first-person hand and the tab list.
 * Swapping the answer here dresses the player everywhere at once.
 */
@Mixin(PlayerInfo.class)
abstract class PlayerInfoMixin {
	@Shadow
	public abstract GameProfile getProfile();

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void skinmixer$wearOutfit(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerSkin original = cir.getReturnValue();
		PlayerSkin dressed = ClientOutfits.apply(getProfile().getId(), original);
		if (dressed != original) {
			cir.setReturnValue(dressed);
		}
	}
}
