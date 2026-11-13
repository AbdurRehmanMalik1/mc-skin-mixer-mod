package net.firstwemine1.skinmixer.client.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's 3D skin preview, reused by the zooming preview: its baked models and drag rotation. */
@Mixin(PlayerSkinWidget.class)
public interface PlayerSkinWidgetAccessor {
	@Accessor("wideModel")
	Model.Simple skinmixer$wideModel();

	@Accessor("slimModel")
	Model.Simple skinmixer$slimModel();

	@Accessor("rotationX")
	float skinmixer$rotationX();

	@Accessor("rotationY")
	float skinmixer$rotationY();

	@Accessor("rotationY")
	void skinmixer$setRotationY(float rotationY);
}
