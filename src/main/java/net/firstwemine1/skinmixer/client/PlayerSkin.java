package net.firstwemine1.skinmixer.client;

import net.minecraft.util.ResourceLocation;

/**
 * A skin as this version of Minecraft knows it: a texture, and the arm model ("default" or "slim").
 * Mirrors the {@code PlayerSkin} record newer versions have, so the rest of the mod reads the same.
 */
public final class PlayerSkin {
	private final ResourceLocation texture;
	private final Model model;

	public PlayerSkin(ResourceLocation texture, Model model) {
		this.texture = texture;
		this.model = model;
	}

	public ResourceLocation texture() {
		return texture;
	}

	public Model model() {
		return model;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PlayerSkin)) {
			return false;
		}
		PlayerSkin other = (PlayerSkin) o;
		return java.util.Objects.equals(texture, other.texture) && java.util.Objects.equals(model, other.model);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(texture, model);
	}

	@Override
	public String toString() {
		return "PlayerSkin[" + "texture=" + texture + ", " + "model=" + model + "]";
	}

	public enum Model {
		SLIM("slim"), WIDE("default");

		private final String id;

		Model(String id) {
			this.id = id;
		}

		/** The model name vanilla uses: {@code "slim"} or {@code "default"}. */
		public String id() {
			return id;
		}

		public static Model byName(String name) {
			return "slim".equals(name) ? SLIM : WIDE;
		}
	}
}
