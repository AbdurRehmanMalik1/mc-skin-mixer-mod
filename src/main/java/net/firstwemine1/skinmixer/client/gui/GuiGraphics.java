package net.firstwemine1.skinmixer.client.gui;

import net.minecraft.client.MainWindow;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;

/**
 * The few drawing calls the mixer's screens make, under the names Minecraft 1.20 gave them. This version
 * draws through {@link AbstractGui}'s static helpers on a pose stack instead.
 */
public final class GuiGraphics {
	private final MatrixStack pose;
	private final IRenderTypeBuffer.Impl buffers = Minecraft.getInstance().renderBuffers().bufferSource();

	public GuiGraphics(MatrixStack pose) {
		this.pose = pose;
	}

	public MatrixStack pose() {
		return pose;
	}

	public IRenderTypeBuffer.Impl bufferSource() {
		return buffers;
	}

	public void flush() {
		buffers.endBatch();
	}

	public void fill(int x0, int y0, int x1, int y1, int colour) {
		AbstractGui.fill(pose, x0, y0, x1, y1, colour);
	}

	public void renderOutline(int x, int y, int width, int height, int colour) {
		fill(x, y, x + width, y + 1, colour);
		fill(x, y + height - 1, x + width, y + height, colour);
		fill(x, y + 1, x + 1, y + height - 1, colour);
		fill(x + width - 1, y + 1, x + width, y + height - 1, colour);
	}

	public void drawString(FontRenderer font, ITextComponent text, int x, int y, int colour) {
		AbstractGui.drawString(pose, font, text, x, y, colour);
	}

	public void drawString(FontRenderer font, String text, int x, int y, int colour) {
		AbstractGui.drawString(pose, font, text, x, y, colour);
	}

	public void drawCenteredString(FontRenderer font, ITextComponent text, int x, int y, int colour) {
		AbstractGui.drawCenteredString(pose, font, text, x, y, colour);
	}

	public void drawCenteredString(FontRenderer font, String text, int x, int y, int colour) {
		AbstractGui.drawCenteredString(pose, font, text, x, y, colour);
	}

	/** Draws a {@code uWidth × vHeight} region of a texture stretched to {@code width × height}. */
	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight,
			int textureWidth, int textureHeight) {
		Minecraft.getInstance().getTextureManager().bind(texture);
		RenderSystem.color4f(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		AbstractGui.blit(pose, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
	}

	/** Clips to a rectangle in GUI coordinates; this version only clips in window pixels, from the bottom. */
	public void enableScissor(int x0, int y0, int x1, int y1) {
		MainWindow window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor((int) (x0 * scale), (int) (window.getHeight() - y1 * scale),
				Math.max(0, (int) ((x1 - x0) * scale)), Math.max(0, (int) ((y1 - y0) * scale)));
	}

	public void disableScissor() {
		RenderSystem.disableScissor();
	}
}
