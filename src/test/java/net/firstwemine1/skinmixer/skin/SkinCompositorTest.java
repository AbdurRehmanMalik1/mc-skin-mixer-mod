package net.firstwemine1.skinmixer.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import net.firstwemine1.skinmixer.skin.SkinCompositor.SourceSkin;
import org.junit.jupiter.api.Test;

class SkinCompositorTest {
	private static final int SIZE = SkinCompositor.SIZE;

	/** A skin where every pixel holds its own index plus a tag, so each copied pixel shows where it came from. */
	private static int[] tagged(int tag) {
		int[] pixels = new int[SIZE * SIZE];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = tag << 16 | i;
		}
		return pixels;
	}

	private static int at(int[] pixels, int x, int y) {
		return pixels[y * SIZE + x];
	}

	/** Marks every pixel the faces of a part's two layers cover; fails if two faces share a pixel. */
	private static int coverage(boolean slim, boolean[] covered) {
		int count = 0;
		for (Part part : Part.values()) {
			int[] layer = SkinCompositor.compose(slim, p -> p == part ? new SourceSkin(tagged(1), slim) : null);
			for (int i = 0; i < layer.length; i++) {
				if (layer[i] != 0) {
					assertTrue(!covered[i], part + " overlaps another part at " + (i % SIZE) + "," + (i / SIZE));
					covered[i] = true;
					count++;
				}
			}
		}
		return count;
	}

	@Test
	void partsNeverOverlap() {
		// Index 0 is pixel (0,0), which is in no face, so a tagged pixel is never 0.
		int wide = coverage(false, new boolean[SIZE * SIZE]);
		int slim = coverage(true, new boolean[SIZE * SIZE]);
		// Face area: head 2×(2·8·8 + 2·8·8 + 2·8·8), body 2×(2·8·4 + 2·4·12 + 2·8·12), each arm/leg
		// 2×(2·w·4 + 2·4·12 + 2·w·12) with w = 4, or 3 for slim arms.
		int head = 2 * (128 + 128 + 128);
		int body = 2 * (64 + 96 + 192);
		int limb = 2 * (32 + 96 + 96);
		int slimArm = 2 * (24 + 96 + 72);
		assertEquals(head + body + 4 * limb, wide);
		assertEquals(head + body + 2 * limb + 2 * slimArm, slim);
	}

	@Test
	void sameModelCopiesEveryPartExactly() {
		int[] source = tagged(1);
		int[] out = SkinCompositor.compose(false, p -> new SourceSkin(source, false));
		for (int i = 0; i < out.length; i++) {
			if (out[i] != 0) {
				assertEquals(source[i], out[i]);
			}
		}
		// Front of the face, base layer, and of the hat.
		assertEquals(at(source, 8, 8), at(out, 8, 8));
		assertEquals(at(source, 40, 8), at(out, 40, 8));
	}

	@Test
	void eachPartComesFromItsOwnSource() {
		int[] a = tagged(1);
		int[] b = tagged(2);
		Function<Part, SourceSkin> sources = p -> new SourceSkin(p == Part.HEAD ? a : b, false);
		int[] out = SkinCompositor.compose(false, sources);
		assertEquals(at(a, 8, 8), at(out, 8, 8));     // face
		assertEquals(at(a, 40, 8), at(out, 40, 8));   // hat
		assertEquals(at(b, 20, 20), at(out, 20, 20)); // chest
		assertEquals(at(b, 20, 36), at(out, 20, 36)); // jacket
		assertEquals(at(b, 4, 20), at(out, 4, 20));   // right leg
	}

	@Test
	void wideArmShrinksToSlim() {
		int[] wide = tagged(1);
		int[] out = SkinCompositor.compose(true, p -> new SourceSkin(wide, false));
		// Right arm front: wide at x 44..47, slim at x 44..46; columns 0,1,2 come from 0,1,2.
		for (int col = 0; col < 3; col++) {
			assertEquals(at(wide, 44 + col * 4 / 3, 25), at(out, 44 + col, 25));
		}
		// Slim back face sits at x 51..53 and comes from the wide back face at 52..55.
		assertEquals(at(wide, 52, 25), at(out, 51, 25));
		// The column the slim arm frees up stays empty.
		assertEquals(0, at(out, 54, 25));
		assertEquals(0, at(out, 55, 25));
	}

	@Test
	void legacySkinGetsMirroredLeftLimbs() {
		int[] legacy = new int[SIZE * SIZE / 2];
		for (int i = 0; i < legacy.length; i++) {
			legacy[i] = 0x80000000 | i;
		}
		int[] out = SkinCompositor.normalizeDownloaded(legacy, SIZE, SIZE / 2);
		// Left leg front (x 20..23, y 52..63) is the right leg front (x 4..7, y 20..31), mirrored.
		for (int col = 0; col < 4; col++) {
			assertEquals((legacy[25 * SIZE + 4 + col] | 0xFF000000), out[57 * SIZE + 23 - col]);
		}
		// Base layers come out opaque; the new overlay rows stay empty.
		assertEquals(0xFF, out[10 * SIZE + 10] >>> 24);
		assertEquals(0, out[40 * SIZE + 4]);
	}

	@Test
	void modernSkinOnlyGetsAnOpaqueBase() {
		int[] skin = tagged(1);
		int[] out = SkinCompositor.normalizeDownloaded(skin, SIZE, SIZE);
		assertEquals(skin[10 * SIZE + 40] | 0xFF000000, out[10 * SIZE + 40] | 0xFF000000);
		assertEquals(skin[36 * SIZE + 20], out[36 * SIZE + 20]); // jacket untouched
		assertEquals(null, SkinCompositor.normalizeDownloaded(new int[16], 4, 4));
	}

	@Test
	void slimArmWidensToWide() {
		int[] slim = tagged(1);
		int[] out = SkinCompositor.compose(false, p -> new SourceSkin(slim, true));
		// Right arm front: slim columns 0,0,1,2 fill wide columns 0..3.
		int[] expected = {0, 0, 1, 2};
		for (int col = 0; col < 4; col++) {
			assertEquals(at(slim, 44 + expected[col], 25), at(out, 44 + col, 25));
		}
		// Left arm (slim) side face moves from x 39 to x 40 on the wide model.
		assertEquals(at(slim, 32 + 4 + 3, 52 + 2), at(out, 32 + 4 + 4, 52 + 2));
	}
}
