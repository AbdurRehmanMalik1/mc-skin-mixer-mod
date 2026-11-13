package net.firstwemine1.skinmixer.client.gui;

import java.util.function.Supplier;

import net.firstwemine1.skinmixer.client.mixin.PlayerSkinWidgetAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Vanilla's rotatable 3D preview, with a camera that glides in on the part being edited: the head
 * fills the view while choosing a head, the legs while choosing legs.
 *
 * <p>Zooming draws the model into a box larger than the widget, placed so the focused part sits in the
 * middle, and clips it to the widget.
 */
public final class OutfitPreview extends PlayerSkinWidget {
	/** From vanilla's preview: the box is 2.125 model units tall and the model fills 97% of it. */
	private static final float MODEL_HEIGHT = 2.125f;
	private static final float FIT_SCALE = 0.97f;
	private static final float PIVOT_Y = -MODEL_HEIGHT / 2;
	/** Skin pixels per model unit, and the middle of the model, in skin pixels from the top of the head. */
	private static final float PIXELS_PER_UNIT = 16;
	private static final float MODEL_MIDDLE = 16;
	/** How quickly the camera settles: about 63% of the way every this many milliseconds. */
	private static final float EASE_MILLIS = 90;

	private final Supplier<PlayerSkin> skin;
	private float zoom = 1;
	private float focus = MODEL_MIDDLE;
	private float targetZoom = 1;
	private float targetFocus = MODEL_MIDDLE;
	private float targetTurn;
	private boolean autoTurn;
	private long lastFrame = -1;

	public OutfitPreview(int width, int height, EntityModelSet models, Supplier<PlayerSkin> skin) {
		super(width, height, models, skin);
		this.skin = skin;
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
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		super.onDrag(event, dragX, dragY);
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
				float delta = Mth.wrapDegrees(targetTurn - current);
				turning.skinmixer$setRotationY(current + delta * step);
			}
		}
		lastFrame = now;

		PlayerSkinWidgetAccessor self = (PlayerSkinWidgetAccessor) (Object) this;
		PlayerSkin current = skin.get();
		PlayerModel model = current.model() == PlayerModelType.SLIM ? self.skinmixer$slimModel() : self.skinmixer$wideModel();

		float boxHeight = getHeight() * zoom;
		float boxWidth = getWidth() * zoom;
		float scale = FIT_SCALE * boxHeight / MODEL_HEIGHT;
		// Shift the box so the focus point, not the middle of the model, lands in the middle of the widget.
		float shift = (focus - MODEL_MIDDLE) * scale / PIXELS_PER_UNIT;
		int left = Math.round(getX() + (getWidth() - boxWidth) / 2);
		int top = Math.round(getY() + (getHeight() - boxHeight) / 2 - shift);

		graphics.enableScissor(getX(), getY(), getRight(), getBottom());
		graphics.submitSkinRenderState(model, current.body().texturePath(), scale, self.skinmixer$rotationX(), self.skinmixer$rotationY(),
				PIVOT_Y, left, top, left + Math.round(boxWidth), top + Math.round(boxHeight));
		graphics.disableScissor();
	}
}
