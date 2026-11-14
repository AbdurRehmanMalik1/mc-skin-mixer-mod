package net.firstwemine1.skinmixer.client.gui;

import java.util.function.Supplier;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Vector3f;
import net.firstwemine1.skinmixer.client.PlayerSkin;
import net.minecraft.util.Util;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.LivingEntity;

/**
 * A rotatable 3D preview of the outfit, with a camera that glides in on the part being edited: the head
 * fills the view while choosing a head, the legs while choosing legs. Drag to turn it.
 *
 * <p>This version has no skin preview widget, so this draws the player model itself, the way newer
 * versions' preview does: centred, {@code height / 2.125} pixels per model unit. Zooming scales that
 * about the widget's centre, shifts it so the focused part sits in the middle, and clips it.
 */
public final class OutfitPreview extends Widget {
	private static final float MODEL_HEIGHT = 2.125f;
	/** Full sky and block light, packed. */
	private static final int FULL_BRIGHT = 15728880;
	private static final float PIXELS_PER_UNIT = 16;
	private static final float MODEL_MIDDLE = 16;
	private static final float EASE_MILLIS = 90;
	private static final float ROTATION_SENSITIVITY = 2.5f;
	private static final float ROTATION_X_LIMIT = 50;

	private final PlayerModel<LivingEntity> wideModel;
	private final PlayerModel<LivingEntity> slimModel;
	private final Supplier<PlayerSkin> skin;
	private float rotationX = -5;
	private float rotationY = 30;
	private float zoom = 1;
	private float focus = MODEL_MIDDLE;
	private float targetZoom = 1;
	private float targetFocus = MODEL_MIDDLE;
	private float targetTurn;
	private boolean autoTurn;
	private long lastFrame = -1;

	public OutfitPreview(int width, int height, Supplier<PlayerSkin> skin) {
		super(0, 0, width, height, StringTextComponent.EMPTY);
		this.wideModel = new PlayerModel<>(0, false);
		this.slimModel = new PlayerModel<>(0, true);
		// Grown-up proportions; models start out as babies.
		this.wideModel.young = false;
		this.slimModel.young = false;
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
			rotationY = targetTurn;
		}
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		rotationX = MathHelper.clamp(rotationX - (float) dragY * ROTATION_SENSITIVITY, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
		rotationY += (float) dragX * ROTATION_SENSITIVITY;
		// The player turned it by hand: leave it where they put it until the next part is framed.
		autoTurn = false;
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
			GuiGraphics graphics = new GuiGraphics(poseStack);
		long now = Util.getMillis();
		if (lastFrame >= 0) {
			float step = 1 - (float) Math.exp(-(now - lastFrame) / EASE_MILLIS);
			zoom += (targetZoom - zoom) * step;
			focus += (targetFocus - focus) * step;
			if (autoTurn) {
				// The short way round, however far it was dragged before.
				rotationY += MathHelper.wrapDegrees(targetTurn - rotationY) * step;
			}
		}
		lastFrame = now;

		PlayerSkin current = skin.get();
		PlayerModel<LivingEntity> model = current.model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
		float centreX = this.x + getWidth() / 2f;
		float centreY = this.y + getHeight() / 2f;
		float scale = getHeight() / MODEL_HEIGHT;
		float pixel = scale / PIXELS_PER_UNIT;

		graphics.enableScissor(this.x, this.y, this.x + getWidth(), this.y + getHeight());
		MatrixStack pose = graphics.pose();
		pose.pushPose();
		// The zoom: scale about the centre, shifted so the focused part lands there.
		pose.translate(centreX, centreY - (focus - MODEL_MIDDLE) * pixel * zoom, 100);
		pose.scale(zoom, zoom, 1);
		// Vanilla's preview transform from here on: feet at the bottom of the box, middle in the middle.
		pose.translate(0, getHeight() / 2f, 0);
		pose.scale(scale, scale, -scale);
		pose.translate(0, -0.0625f, 0);
		pose.translate(0, -1.0625f, 0);
		pose.mulPose(Vector3f.XP.rotationDegrees(rotationX));
		pose.translate(0, 1.0625f, 0);
		// Without newer versions' z flip, turning goes the other way round: negated so positive still turns
		// the player's right side forward, as on every version.
		pose.mulPose(Vector3f.YP.rotationDegrees(-rotationY));
		graphics.flush();

		pose.pushPose();
		// As the inventory draws a player on this version: no extra z flip (newer GUIs need one).
		pose.translate(0, -1.501f, 0);
		model.renderToBuffer(pose, graphics.bufferSource().getBuffer(model.renderType(current.texture())),
				FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		pose.popPose();
		graphics.flush();
		pose.popPose();
		graphics.disableScissor();
	}

}
