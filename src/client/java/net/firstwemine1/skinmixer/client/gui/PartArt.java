package net.firstwemine1.skinmixer.client.gui;

import java.util.List;

import net.firstwemine1.skinmixer.skin.Part;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws skin parts flat, straight from a skin texture: the front face of each part, base layer with
 * its overlay on top. Used for the body map, the category tabs and every choice in the grid.
 */
public final class PartArt {
	/** The flat front view of a player is 16 skin pixels wide and 32 tall. */
	public static final int DOLL_WIDTH = 16;
	public static final int DOLL_HEIGHT = 32;
	private static final int TEXTURE_SIZE = 64;

	private PartArt() {
	}

	/**
	 * Where a part sits on the flat front view, in skin pixels: {x, y, width, height}. The player faces
	 * the viewer, so their right arm and leg are on the viewer's left.
	 */
	public static int[] dollRect(Part part, boolean slim) {
		int arm = part.width(slim);
		return switch (part) {
			case HEAD -> new int[] {4, 0, 8, 8};
			case BODY -> new int[] {4, 8, 8, 12};
			case RIGHT_ARM -> new int[] {4 - arm, 8, arm, 12};
			case LEFT_ARM -> new int[] {12, 8, arm, 12};
			case RIGHT_LEG -> new int[] {4, 20, 4, 12};
			case LEFT_LEG -> new int[] {8, 20, 4, 12};
		};
	}

	/** One part's front face at (x, y), each skin pixel drawn {@code scale} GUI pixels wide. */
	public static void drawFront(GuiGraphics graphics, ResourceLocation texture, Part part, boolean slim,
			float x, float y, float scale) {
		int width = part.width(slim);
		int height = part.height();
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.blit(RenderType::guiTextured, texture, 0, 0, part.frontU(false), part.frontV(false),
				width, height, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
		graphics.blit(RenderType::guiTextured, texture, 0, 0, part.frontU(true), part.frontV(true),
				width, height, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
		graphics.pose().popPose();
	}

	/** The whole flat front view with its top-left corner at (x, y). */
	public static void drawDoll(GuiGraphics graphics, ResourceLocation texture, boolean slim, float x, float y, float scale) {
		for (Part part : Part.values()) {
			int[] rect = dollRect(part, slim);
			drawFront(graphics, texture, part, slim, x + rect[0] * scale, y + rect[1] * scale, scale);
		}
	}

	/**
	 * Just the given parts, as large as fits in the box and centred in it. A pair of arms is drawn side
	 * by side rather than a body-width apart, so it isn't drawn tiny.
	 */
	public static void drawItem(GuiGraphics graphics, ResourceLocation texture, boolean slim, List<Part> parts,
			int boxX, int boxY, int boxWidth, int boxHeight) {
		int[][] rects = new int[parts.size()][];
		boolean armPair = parts.size() == 2 && parts.get(0).isArm() && parts.get(1).isArm();
		for (int i = 0; i < parts.size(); i++) {
			rects[i] = dollRect(parts.get(i), slim);
			if (armPair && i == 1) {
				// Left arm right next to the right arm, one pixel apart.
				rects[i] = new int[] {rects[0][0] + rects[0][2] + 1, rects[0][1], rects[i][2], rects[i][3]};
			}
		}
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int[] rect : rects) {
			minX = Math.min(minX, rect[0]);
			minY = Math.min(minY, rect[1]);
			maxX = Math.max(maxX, rect[0] + rect[2]);
			maxY = Math.max(maxY, rect[1] + rect[3]);
		}
		float scale = Math.min((float) boxWidth / (maxX - minX), (float) boxHeight / (maxY - minY));
		if (scale >= 2) {
			// Whole numbers keep every skin pixel the same size.
			scale = (float) Math.floor(scale);
		}
		float x = boxX + (boxWidth - (maxX - minX) * scale) / 2;
		float y = boxY + (boxHeight - (maxY - minY) * scale) / 2;
		for (int i = 0; i < parts.size(); i++) {
			drawFront(graphics, texture, parts.get(i), slim, x + (rects[i][0] - minX) * scale, y + (rects[i][1] - minY) * scale, scale);
		}
	}
}
