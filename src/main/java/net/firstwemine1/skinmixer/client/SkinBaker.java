package net.firstwemine1.skinmixer.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.firstwemine1.skinmixer.skin.Part;
import net.firstwemine1.skinmixer.skin.SkinCompositor;
import net.firstwemine1.skinmixer.skin.SkinCompositor.SourceSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Turns outfits into ordinary 64×64 skin textures. The game then draws the baked texture exactly as it
 * draws any skin, so armour, capes, the first-person hand and other mods keep working.
 *
 * <p>Render thread only: it creates GPU textures.
 */
public final class SkinBaker {
	private static final Identifier PREVIEW = SkinMixer.id("preview");

	/** Baked textures by {@link #key}: identical outfits share one texture. */
	private static final Map<String, Identifier> BAKED = new HashMap<>();
	/** Pixels of the default skins, read once from the game's resources. */
	private static final Map<Identifier, int[]> RESOURCE_PIXELS = new HashMap<>();
	private static int nextId;

	private SkinBaker() {
	}

	/** The default skin {@code name}'s texture, in the wide or slim version. */
	public static Identifier defaultTexture(String name, boolean slim) {
		return Identifier.withDefaultNamespace("textures/entity/player/" + (slim ? "slim/" : "wide/") + name + ".png");
	}

	/** A {@link PlayerSkin} drawing {@code texture}, keeping the cape and elytra of {@code base}. */
	public static PlayerSkin skinOf(Identifier texture, boolean slim, PlayerSkin base) {
		ClientAsset.Texture body = new ClientAsset.ResourceTexture(texture, texture);
		PlayerModelType model = slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
		return base == null
				? PlayerSkin.insecure(body, null, null, model)
				: new PlayerSkin(body, base.cape(), base.elytra(), model, base.secure());
	}

	/** Same outfit on the same real skin, same key, so they share one baked texture. */
	static String key(Outfit outfit, PlayerSkin self) {
		String key = (outfit.slim() ? "slim:" : "wide:") + String.join(",", outfit.parts());
		return outfit.usesSelf() && self != null ? key + "|" + self.body().texturePath() : key;
	}

	/**
	 * The texture for {@code outfit} worn by a player whose real skin is {@code self}, baking it if needed.
	 * Null if a source skin couldn't be read; the caller then shows the real skin.
	 */
	public static Identifier bake(Outfit outfit, PlayerSkin self) {
		String key = key(outfit, self);
		Identifier existing = BAKED.get(key);
		if (existing != null) {
			return existing;
		}
		int[] pixels = compose(outfit, self);
		if (pixels == null) {
			return null;
		}
		Identifier id = SkinMixer.id("baked/" + nextId++);
		upload(id, pixels);
		BAKED.put(key, id);
		return id;
	}

	/** Bakes into the one texture the menu's preview reuses, so trying outfits doesn't pile up textures. */
	public static Identifier bakePreview(Outfit outfit, PlayerSkin self) {
		int[] pixels = compose(outfit, self);
		if (pixels == null) {
			return null;
		}
		upload(PREVIEW, pixels);
		return PREVIEW;
	}

	/** Frees baked textures no player wears any more. */
	static void retainOnly(Set<String> keysInUse) {
		for (Iterator<Map.Entry<String, Identifier>> it = BAKED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Identifier> entry = it.next();
			if (!keysInUse.contains(entry.getKey())) {
				Minecraft.getInstance().getTextureManager().release(entry.getValue());
				it.remove();
			}
		}
	}

	/** Leaving a world: every baked texture goes. Default skin pixels are kept (resource packs aside, they don't change). */
	static void clear() {
		retainOnly(Set.of());
	}

	/** Forgets cached default skins, so a resource pack that replaces them is picked up. */
	static void clearResourceCache() {
		RESOURCE_PIXELS.clear();
	}

	private static int[] compose(Outfit outfit, PlayerSkin self) {
		Map<String, SourceSkin> sources = new HashMap<>();
		for (Part part : Part.values()) {
			String id = outfit.source(part);
			if (!sources.containsKey(id)) {
				SourceSkin source = source(id, outfit.slim(), self);
				if (source == null) {
					return null;
				}
				sources.put(id, source);
			}
		}
		return SkinCompositor.compose(outfit.slim(), part -> sources.get(outfit.source(part)));
	}

	private static SourceSkin source(String id, boolean slim, PlayerSkin self) {
		if (Outfit.SELF.equals(id)) {
			if (self == null) {
				return null;
			}
			int[] pixels = texturePixels(self.body().texturePath());
			return pixels == null ? null : new SourceSkin(pixels, self.model() == PlayerModelType.SLIM);
		}
		UUID remote = Outfit.mojangPlayer(id);
		if (remote != null) {
			PlayerSkin skin = RemoteSkins.get(remote);
			if (skin == null) {
				return null;
			}
			int[] pixels = texturePixels(skin.body().texturePath());
			return pixels == null ? null : new SourceSkin(pixels, skin.model() == PlayerModelType.SLIM);
		}
		// Default skins come in both models, so the arms never need resizing.
		int[] pixels = texturePixels(defaultTexture(id, slim));
		return pixels == null ? null : new SourceSkin(pixels, slim);
	}

	/**
	 * A skin texture's pixels. Downloaded skins live only in the texture manager (vanilla keeps their pixels
	 * after upload); default skins and resource-pack skins are read from the resources.
	 */
	public static int[] texturePixels(Identifier path) {
		Minecraft minecraft = Minecraft.getInstance();
		int[] cached = RESOURCE_PIXELS.get(path);
		if (cached != null) {
			return cached;
		}
		Optional<Resource> resource = minecraft.getResourceManager().getResource(path);
		if (resource.isPresent()) {
			try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
				int[] pixels = toPixels(image);
				if (pixels != null) {
					RESOURCE_PIXELS.put(path, pixels);
				}
				return pixels;
			} catch (IOException e) {
				SkinMixer.LOGGER.warn("Couldn't read skin {}", path, e);
				return null;
			}
		}
		AbstractTexture texture = minecraft.getTextureManager().getTexture(path);
		if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
			return toPixels(dynamic.getPixels());
		}
		return null;
	}

	/** Pixels as ARGB, or null for anything but a 64×64 skin (vanilla already widened legacy 64×32 ones). */
	private static int[] toPixels(NativeImage image) {
		if (image.getWidth() != SkinCompositor.SIZE || image.getHeight() != SkinCompositor.SIZE) {
			return null;
		}
		int[] pixels = new int[SkinCompositor.SIZE * SkinCompositor.SIZE];
		for (int y = 0; y < SkinCompositor.SIZE; y++) {
			for (int x = 0; x < SkinCompositor.SIZE; x++) {
				pixels[y * SkinCompositor.SIZE + x] = image.getPixel(x, y);
			}
		}
		return pixels;
	}

	private static void upload(Identifier id, int[] pixels) {
		NativeImage image = new NativeImage(SkinCompositor.SIZE, SkinCompositor.SIZE, true);
		for (int y = 0; y < SkinCompositor.SIZE; y++) {
			for (int x = 0; x < SkinCompositor.SIZE; x++) {
				image.setPixel(x, y, pixels[y * SkinCompositor.SIZE + x]);
			}
		}
		// register() closes whatever texture had this id before.
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
	}
}
