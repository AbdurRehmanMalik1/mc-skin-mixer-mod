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
	public record SourceSkin(int[] pixels, boolean slim) {
		public SourceSkin {
			if (pixels.length != SIZE * SIZE) {
				throw new IllegalArgumentException("Skin must be 64x64, got " + pixels.length + " pixels");
			}
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
