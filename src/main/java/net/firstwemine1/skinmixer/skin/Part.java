package net.firstwemine1.skinmixer.skin;

/**
 * The six body parts of a player skin, and where each one sits on the 64×64 texture.
 *
 * <p>Every part is a box {@code w × h × d}. Its base layer and its overlay layer (hat, jacket, sleeves,
 * trousers) are each unfolded at a fixed corner {@code (u, v)} of the texture. "Right" and "left" are the
 * player's own sides. Arms are 4 pixels wide on the wide ("Steve") model and 3 on the slim ("Alex") one.
 */
public enum Part {
	HEAD("head", 8, 8, 8, 0, 0, 32, 0, false),
	BODY("body", 8, 12, 4, 16, 16, 16, 32, false),
	RIGHT_ARM("right_arm", 4, 12, 4, 40, 16, 40, 32, true),
	LEFT_ARM("left_arm", 4, 12, 4, 32, 48, 48, 48, true),
	RIGHT_LEG("right_leg", 4, 12, 4, 0, 16, 0, 32, false),
	LEFT_LEG("left_leg", 4, 12, 4, 16, 48, 0, 48, false);

	private final String key;
	private final int width;
	private final int height;
	private final int depth;
	private final int baseU;
	private final int baseV;
	private final int overlayU;
	private final int overlayV;
	private final boolean arm;

	Part(String key, int width, int height, int depth, int baseU, int baseV, int overlayU, int overlayV, boolean arm) {
		this.key = key;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.baseU = baseU;
		this.baseV = baseV;
		this.overlayU = overlayU;
		this.overlayV = overlayV;
		this.arm = arm;
	}

	/** Used in translation keys and saved data. */
	public String key() {
		return key;
	}

	/** Box width in pixels: arms are one pixel narrower on the slim model. */
	public int width(boolean slim) {
		return arm && slim ? width - 1 : width;
	}

	public int height() {
		return height;
	}

	public int depth() {
		return depth;
	}

	public int baseU() {
		return baseU;
	}

	public int baseV() {
		return baseV;
	}

	public int overlayU() {
		return overlayU;
	}

	public int overlayV() {
		return overlayV;
	}

	public boolean isArm() {
		return arm;
	}

	/** Whether this is the right arm or leg; false for the left ones, the head and the body. */
	public boolean isRight() {
		return this == RIGHT_ARM || this == RIGHT_LEG;
	}

	/** Texture x of the front face, the side people see: base layer or overlay. */
	public int frontU(boolean overlay) {
		return (overlay ? overlayU : baseU) + depth;
	}

	/** Texture y of the front face. */
	public int frontV(boolean overlay) {
		return (overlay ? overlayV : baseV) + depth;
	}

	/** The same part on the other side of the body, or this part for the head and body. */
	public Part mirror() {
		return switch (this) {
			case RIGHT_ARM -> LEFT_ARM;
			case LEFT_ARM -> RIGHT_ARM;
			case RIGHT_LEG -> LEFT_LEG;
			case LEFT_LEG -> RIGHT_LEG;
			default -> this;
		};
	}
}
