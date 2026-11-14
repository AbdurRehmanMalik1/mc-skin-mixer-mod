package net.firstwemine1.skinmixer.skin;

import java.util.function.Function;

/**
 * Builds one skin out of parts of other skins, by copying pixels between the fixed rectangles of
 * {@link Part}. Pure Java on plain pixel arrays, so it can be tested without the game.
 *
 * <p>Pixels are 64×64, row by row, one int per pixel. The colour format doesn't matter as long as the
 * caller reads and writes the same one.
 */
public final class SkinCompositor {
	public static final int SIZE = 64;

	private SkinCompositor() {
	}

	/** A source skin: its pixels, and whether its arms are slim. */
	public static final class SourceSkin {
		private final int[] pixels;
		private final boolean slim;

		public SourceSkin(int[] pixels, boolean slim) {
			if (pixels.length != SIZE * SIZE) {
				throw new IllegalArgumentException("Skin must be 64x64, got " + pixels.length + " pixels");
			}
			this.pixels = pixels;
			this.slim = slim;
		}

		public int[] pixels() {
			return pixels;
		}

		public boolean slim() {
			return slim;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof SourceSkin)) {
				return false;
			}
			SourceSkin other = (SourceSkin) o;
			return java.util.Objects.equals(pixels, other.pixels) && java.util.Objects.equals(slim, other.slim);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(pixels, slim);
		}

		@Override
		public String toString() {
			return "SourceSkin[" + "pixels=" + pixels + ", " + "slim=" + slim + "]";
		}
	}

	/**
	 * The skin made of each part's base and overlay layers, taken from the source chosen for that part.
	 * Everything outside the parts stays transparent (0). A part whose source is null is left out.
	 *
	 * @param slim whether the result has slim arms; arms from a skin of the other model are resized
	 */
	public static int[] compose(boolean slim, Function<Part, SourceSkin> sources) {
		int[] out = new int[SIZE * SIZE];
		for (Part part : Part.values()) {
			SourceSkin source = sources.apply(part);
			if (source == null) {
				continue;
			}
			int sourceWidth = part.width(source.slim());
			int targetWidth = part.width(slim);
			copyBox(source.pixels(), part.baseU(), part.baseV(), sourceWidth, out, part.baseU(), part.baseV(), targetWidth,
					part.height(), part.depth());
			copyBox(source.pixels(), part.overlayU(), part.overlayV(), sourceWidth, out, part.overlayU(), part.overlayV(),
					targetWidth, part.height(), part.depth());
		}
		return out;
	}

	/**
	 * Copies the six faces of one unfolded box. Faces that span the box's width are resized when the
	 * widths differ (an arm moving between the wide and slim models); depth and height never change.
	 *
	 * <pre>
	 *          d        d+w
	 *   0      [ top w×d ][ bottom w×d ]
	 *   d  [right d×h][ front w×h ][ left d×h ][ back w×h ]
	 *      0          d            d+w         2d+w
	 * </pre>
	 */
	static void copyBox(int[] src, int su, int sv, int sw, int[] dst, int du, int dv, int dw, int h, int d) {
		// Top and bottom.
		copyFace(src, su + d, sv, sw, dst, du + d, dv, dw, d);
		copyFace(src, su + d + sw, sv, sw, dst, du + d + dw, dv, dw, d);
		// Right side, front, left side, back.
		copyFace(src, su, sv + d, d, dst, du, dv + d, d, h);
		copyFace(src, su + d, sv + d, sw, dst, du + d, dv + d, dw, h);
		copyFace(src, su + d + sw, sv + d, d, dst, du + d + dw, dv + d, d, h);
		copyFace(src, su + 2 * d + sw, sv + d, sw, dst, du + 2 * d + dw, dv + d, dw, h);
	}

	/**
	 * A skin file as downloaded, cleaned up the way vanilla does when it loads one: an old 64×32 skin is
	 * widened to 64×64 (left arm and leg mirrored from the right), and the base layer is made opaque.
	 * Only the alpha byte (the top 8 bits) is touched, so it works for ARGB and ABGR pixels alike.
	 *
	 * @return the 64×64 pixels, or null if the image isn't a skin
	 */
	public static int[] normalizeDownloaded(int[] pixels, int width, int height) {
		if (width != SIZE || (height != SIZE && height != SIZE / 2) || pixels.length != width * height) {
			return null;
		}
		int[] out = new int[SIZE * SIZE];
		System.arraycopy(pixels, 0, out, 0, pixels.length);
		boolean legacy = height == SIZE / 2;
		if (legacy) {
			copyRect(out, 4, 16, 16, 32, 4, 4);
			copyRect(out, 8, 16, 16, 32, 4, 4);
			copyRect(out, 0, 20, 24, 32, 4, 12);
			copyRect(out, 4, 20, 16, 32, 4, 12);
			copyRect(out, 8, 20, 8, 32, 4, 12);
			copyRect(out, 12, 20, 16, 32, 4, 12);
			copyRect(out, 44, 16, -8, 32, 4, 4);
			copyRect(out, 48, 16, -8, 32, 4, 4);
			copyRect(out, 40, 20, 0, 32, 4, 12);
			copyRect(out, 44, 20, -8, 32, 4, 12);
			copyRect(out, 48, 20, -16, 32, 4, 12);
			copyRect(out, 52, 20, -8, 32, 4, 12);
		}
		setNoAlpha(out, 0, 0, 32, 16);
		if (legacy) {
			clearHatIfSolid(out, 32, 0, 64, 32);
		}
		setNoAlpha(out, 0, 16, 64, 32);
		setNoAlpha(out, 16, 48, 48, 64);
		return out;
	}

	/** Vanilla's {@code copyRect(x, y, dx, dy, w, h, mirrorX=true, mirrorY=false)}. */
	private static void copyRect(int[] pixels, int x, int y, int dx, int dy, int width, int height) {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int value = pixels[(y + row) * SIZE + x + col];
				pixels[(y + dy + row) * SIZE + x + dx + width - 1 - col] = value;
			}
		}
	}

	private static void setNoAlpha(int[] pixels, int x0, int y0, int x1, int y1) {
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				pixels[y * SIZE + x] |= 0xFF000000;
			}
		}
	}

	/** Old skins often filled the hat area with a solid colour; vanilla treats that as "no hat". */
	private static void clearHatIfSolid(int[] pixels, int x0, int y0, int x1, int y1) {
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if ((pixels[y * SIZE + x] >>> 24) < 128) {
					return;
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				pixels[y * SIZE + x] &= 0x00FFFFFF;
			}
		}
	}

	/** Copies a {@code sw × h} face into a {@code dw × h} one, picking the nearest source column. */
	static void copyFace(int[] src, int sx, int sy, int sw, int[] dst, int dx, int dy, int dw, int h) {
		for (int row = 0; row < h; row++) {
			for (int col = 0; col < dw; col++) {
				int sourceCol = col * sw / dw;
				dst[(dy + row) * SIZE + dx + col] = src[(sy + row) * SIZE + sx + sourceCol];
			}
		}
	}
}
