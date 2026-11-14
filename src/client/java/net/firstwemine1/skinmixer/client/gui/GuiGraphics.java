package net.firstwemine1.skinmixer.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The few drawing calls the mixer's screens make, under the names Minecraft 1.20 gave them. This version
 * draws through {@link GuiComponent}'s static helpers on a pose stack instead.
 */
public final class GuiGraphics {
	private final PoseStack pose;
	private final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

	public GuiGraphics(PoseStack pose) {
		this.pose = pose;
	}

	public PoseStack pose() {
		return pose;
	}

	public MultiBufferSource.BufferSource bufferSource() {
		return buffers;
	}

	public void flush() {
		buffers.endBatch();
	}

	public void fill(int x0, int y0, int x1, int y1, int colour) {
		GuiComponent.fill(pose, x0, y0, x1, y1, colour);
	}

	public void renderOutline(int x, int y, int width, int height, int colour) {
		fill(x, y, x + width, y + 1, colour);
		fill(x, y + height - 1, x + width, y + height, colour);
		fill(x, y + 1, x + 1, y + height - 1, colour);
		fill(x + width - 1, y + 1, x + width, y + height - 1, colour);
	}

	public void drawString(Font font, Component text, int x, int y, int colour) {
		GuiComponent.drawString(pose, font, text, x, y, colour);
	}

	public void drawString(Font font, String text, int x, int y, int colour) {
		GuiComponent.drawString(pose, font, text, x, y, colour);
	}

	public void drawCenteredString(Font font, Component text, int x, int y, int colour) {
		GuiComponent.drawCenteredString(pose, font, text, x, y, colour);
	}

	public void drawCenteredString(Font font, String text, int x, int y, int colour) {
		GuiComponent.drawCenteredString(pose, font, text, x, y, colour);
	}

	/** Draws a {@code uWidth × vHeight} region of a texture stretched to {@code width × height}. */
	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight,
			int textureWidth, int textureHeight) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GuiComponent.blit(pose, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
	}

	/** Clips to a rectangle in GUI coordinates; this version only clips in window pixels, from the bottom. */
	public void enableScissor(int x0, int y0, int x1, int y1) {
		Window window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor((int) (x0 * scale), (int) (window.getHeight() - y1 * scale),
				Math.max(0, (int) ((x1 - x0) * scale)), Math.max(0, (int) ((y1 - y0) * scale)));
	}

	public void disableScissor() {
		RenderSystem.disableScissor();
	}
}
