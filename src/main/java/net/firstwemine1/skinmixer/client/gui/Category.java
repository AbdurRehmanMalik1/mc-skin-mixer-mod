package net.firstwemine1.skinmixer.client.gui;

import java.util.List;

import net.firstwemine1.skinmixer.skin.Part;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * What the menu is editing: the whole outfit, or one region of the body. Like the categories of a
 * character creator, each has its own tab, its own grid of choices, and its own camera framing.
 */
public enum Category {
	OUTFIT("outfit", 16, 1.0f, 30, Part.values()),
	HEAD("head", 4.5f, 2.1f, 25, Part.HEAD),
	BODY("body", 14, 1.6f, 15, Part.BODY),
	ARMS("arms", 14, 1.6f, 0, Part.RIGHT_ARM, Part.LEFT_ARM),
	LEGS("legs", 25, 1.6f, 15, Part.RIGHT_LEG, Part.LEFT_LEG);

	/** How far the model turns to show one arm or leg instead of the pair. Positive turns the right side forward. */
	private static final float ONE_SIDE_TURN = 55;

	private final String key;
	private final float focus;
	private final float zoom;
	private final float turn;
	private final List<Part> parts;

	Category(String key, float focus, float zoom, float turn, Part... parts) {
		this.key = key;
		this.focus = focus;
		this.zoom = zoom;
		this.turn = turn;
		this.parts = java.util.Arrays.asList(parts);
	}

	/** Which way the model faces while this is edited, in degrees: the edited side turned towards the viewer. */
	public float turn(Side side) {
		if (!isPaired() || side == Side.BOTH) {
			return turn;
		}
		return side == Side.RIGHT ? ONE_SIDE_TURN : -ONE_SIDE_TURN;
	}

	public Component title() {
		return new TranslatableComponent("skinmixer.category." + key);
	}

	/** Height on the model the camera centres on, in skin pixels from the top of the head (feet at 32). */
	public float focus() {
		return focus;
	}

	/** How far the camera zooms in: 1 shows the whole player. */
	public float zoom() {
		return zoom;
	}

	/** Arms and legs come in pairs that can be changed together or one at a time. */
	public boolean isPaired() {
		return parts.size() == 2;
	}

	/** The parts a choice in this category changes, given which side(s) are being edited. */
	public List<Part> parts(Side side) {
		if (!isPaired() || side == Side.BOTH) {
			return parts;
		}
		return parts.stream().filter(part -> part.isRight() == (side == Side.RIGHT)).collect(java.util.stream.Collectors.toList());
	}

	public static Category of(Part part) {
		switch (part) {
			case HEAD:
				return HEAD;
			case BODY:
				return BODY;
			case RIGHT_ARM:
			case LEFT_ARM:
				return ARMS;
			case RIGHT_LEG:
			case LEFT_LEG:
				return LEGS;
			default:
				throw new IllegalStateException();
		}
	}

	/** Which of a pair of arms or legs the choices apply to. */
	public enum Side {
		BOTH, RIGHT, LEFT;

		public Component title() {
			return new TranslatableComponent("skinmixer.side." + name().toLowerCase(java.util.Locale.ROOT));
		}

		public Side next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}
}
