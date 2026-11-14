package net.firstwemine1.skinmixer.client.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's 3D skin preview, reused by the zooming preview: its drag rotation. */
@Mixin(PlayerSkinWidget.class)
public interface PlayerSkinWidgetAccessor {
	@Accessor("rotationY")
	float skinmixer$rotationY();

	@Accessor("rotationY")
	void skinmixer$setRotationY(float rotationY);
}
