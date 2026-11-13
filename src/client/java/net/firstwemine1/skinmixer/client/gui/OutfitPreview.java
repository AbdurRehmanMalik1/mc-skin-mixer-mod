package net.firstwemine1.skinmixer.client.gui;

import java.util.function.Supplier;

import net.firstwemine1.skinmixer.client.mixin.PlayerSkinWidgetAccessor;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;

/**
 * Vanilla's rotatable 3D preview, with a camera that glides in on the part being edited: the head
 * fills the view while choosing a head, the legs while choosing legs.
 *
 * <p>Vanilla draws the model centred in the widget. Zooming scales that drawing about the widget's
 * centre, shifts it so the focused part sits in the middle, and clips it to the widget.
 */
public final class OutfitPreview extends PlayerSkinWidget {
	/** From vanilla's preview: the model is drawn {@code height / 2.125} pixels per model unit. */
	private static final float MODEL_HEIGHT = 2.125f;
	/** Skin pixels per model unit, and the middle of the model, in skin pixels from the top of the head. */
	private static final float PIXELS_PER_UNIT = 16;
	private static final float MODEL_MIDDLE = 16;
	/** How quickly the camera settles: about 63% of the way every this many milliseconds. */
	private static final float EASE_MILLIS = 90;

	private float zoom = 1;
	private float focus = MODEL_MIDDLE;
	private float targetZoom = 1;
	private float targetFocus = MODEL_MIDDLE;
	private float targetTurn;
	private boolean autoTurn;
	private long lastFrame = -1;

	public OutfitPreview(int width, int height, EntityModelSet models, Supplier<PlayerSkin> skin) {
		super(width, height, models, skin);
	}

	/** Frames the part being edited: zooms in on it and turns it towards the viewer. */
	public void frame(Category category, Category.Side side, boolean instantly) {
		targetZoom = category.zoom();
		targetFocus = category.focus();
		targetTurn = category.turn(side);
		autoTurn = true;
		if (instantly) {
			zoom = targetZoom;
			focus = targetFocus;
			((PlayerSkinWidgetAccessor) (Object) this).skinmixer$setRotationY(targetTurn);
		}
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		super.onDrag(mouseX, mouseY, dragX, dragY);
		// The player turned it by hand: leave it where they put it until the next part is framed.
		autoTurn = false;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		if (lastFrame >= 0) {
			float step = 1 - (float) Math.exp(-(now - lastFrame) / EASE_MILLIS);
			zoom += (targetZoom - zoom) * step;
			focus += (targetFocus - focus) * step;
			if (autoTurn) {
				PlayerSkinWidgetAccessor turning = (PlayerSkinWidgetAccessor) (Object) this;
				// The short way round, however far it was dragged before.
				float current = turning.skinmixer$rotationY();
				turning.skinmixer$setRotationY(current + Mth.wrapDegrees(targetTurn - current) * step);
			}
		}
		lastFrame = now;

		float centreX = getX() + getWidth() / 2f;
		float centreY = getY() + getHeight() / 2f;
		float pixel = getHeight() / MODEL_HEIGHT / PIXELS_PER_UNIT;
		graphics.enableScissor(getX(), getY(), getRight(), getBottom());
		graphics.pose().pushPose();
		graphics.pose().translate(centreX, centreY - (focus - MODEL_MIDDLE) * pixel * zoom, 0);
		// Depth isn't scaled, so a zoomed-in model can't poke through the GUI's near and far planes.
		graphics.pose().scale(zoom, zoom, 1);
		graphics.pose().translate(-centreX, -centreY, 0);
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popPose();
		graphics.disableScissor();
	}
}
