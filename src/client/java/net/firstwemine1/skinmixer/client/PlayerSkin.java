package net.firstwemine1.skinmixer.client;

import net.minecraft.resources.ResourceLocation;

/**
 * A skin as this version of Minecraft knows it: a texture, and the arm model ("default" or "slim").
 * Mirrors the {@code PlayerSkin} record newer versions have, so the rest of the mod reads the same.
 */
public record PlayerSkin(ResourceLocation texture, Model model) {
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
