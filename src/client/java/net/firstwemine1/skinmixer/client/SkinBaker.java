package net.firstwemine1.skinmixer.client;

import java.io.File;
import java.io.FileInputStream;
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
import net.minecraft.client.renderer.texture.HttpTexture;
import net.firstwemine1.skinmixer.client.mixin.HttpTextureAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Turns outfits into ordinary 64×64 skin textures. The game then draws the baked texture exactly as it
 * draws any skin, so armour, capes, the first-person hand and other mods keep working.
 *
 * <p>Render thread only: it creates GPU textures.
 */
public final class SkinBaker {
	private static final ResourceLocation PREVIEW = SkinMixer.id("preview");

	/** Baked textures by {@link #key}: identical outfits share one texture. */
	private static final Map<String, ResourceLocation> BAKED = new HashMap<>();
	/** Pixels of the default skins, read once from the game's resources. */
	private static final Map<ResourceLocation, int[]> RESOURCE_PIXELS = new HashMap<>();
	private static int nextId;

	private SkinBaker() {
	}

	/** The default skin {@code name}'s texture. This version has one file per skin, whatever the arm model. */
	public static ResourceLocation defaultTexture(String name, boolean slim) {
		return new ResourceLocation("textures/entity/" + name + ".png");
	}

	/** Whether a default skin's arms are slim: only Alex's, on this version. */
	public static boolean defaultSlim(String name, boolean wanted) {
		return "alex".equals(name);
	}

	/** A {@link PlayerSkin} drawing {@code texture}, keeping the cape and elytra of {@code base}. */
	public static PlayerSkin skinOf(ResourceLocation texture, boolean slim, PlayerSkin base) {
		return new PlayerSkin(texture, slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE);
	}

	/** Same outfit on the same real skin, same key, so they share one baked texture. */
	static String key(Outfit outfit, PlayerSkin self) {
		String key = (outfit.slim() ? "slim:" : "wide:") + String.join(",", outfit.parts());
		return outfit.usesSelf() && self != null ? key + "|" + self.texture() : key;
	}

	/**
	 * The texture for {@code outfit} worn by a player whose real skin is {@code self}, baking it if needed.
	 * Null if a source skin couldn't be read; the caller then shows the real skin.
	 */
	public static ResourceLocation bake(Outfit outfit, PlayerSkin self) {
		String key = key(outfit, self);
		ResourceLocation existing = BAKED.get(key);
		if (existing != null) {
			return existing;
		}
		int[] pixels = compose(outfit, self);
		if (pixels == null) {
			return null;
		}
		ResourceLocation id = SkinMixer.id("baked/" + nextId++);
		upload(id, pixels);
		BAKED.put(key, id);
		return id;
	}

	/** Bakes into the one texture the menu's preview reuses, so trying outfits doesn't pile up textures. */
	public static ResourceLocation bakePreview(Outfit outfit, PlayerSkin self) {
		int[] pixels = compose(outfit, self);
		if (pixels == null) {
			return null;
		}
		upload(PREVIEW, pixels);
		return PREVIEW;
	}

	/** Frees baked textures no player wears any more. */
	static void retainOnly(Set<String> keysInUse) {
		for (Iterator<Map.Entry<String, ResourceLocation>> it = BAKED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, ResourceLocation> entry = it.next();
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
			int[] pixels = texturePixels(self.texture());
			return pixels == null ? null : new SourceSkin(pixels, self.model() == PlayerSkin.Model.SLIM);
		}
		UUID remote = Outfit.mojangPlayer(id);
		if (remote != null) {
			PlayerSkin skin = RemoteSkins.get(remote);
			if (skin == null) {
				return null;
			}
			int[] pixels = texturePixels(skin.texture());
			return pixels == null ? null : new SourceSkin(pixels, skin.model() == PlayerSkin.Model.SLIM);
		}
		// One model per default skin here: arms from the other model are resized.
		int[] pixels = texturePixels(defaultTexture(id, slim));
		return pixels == null ? null : new SourceSkin(pixels, defaultSlim(id, slim));
	}

	/**
	 * A skin texture's pixels. Default skins and resource-pack skins are read from the resources; a
	 * downloaded skin is read back from the file vanilla cached it in, since its texture keeps no pixels.
	 */
	public static int[] texturePixels(ResourceLocation path) {
		Minecraft minecraft = Minecraft.getInstance();
		int[] cached = RESOURCE_PIXELS.get(path);
		if (cached != null) {
			return cached;
		}
		if (minecraft.getResourceManager().hasResource(path)) {
			try (Resource resource = minecraft.getResourceManager().getResource(path);
					InputStream in = resource.getInputStream(); NativeImage image = NativeImage.read(in)) {
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
		if (texture instanceof HttpTexture download) {
			return downloadedPixels(((HttpTextureAccessor) download).skinmixer$file());
		}
		return null;
	}

	/** A cached skin download, cleaned up as vanilla does on load. Null until the download finished. */
	private static int[] downloadedPixels(File file) {
		if (file == null || !file.isFile()) {
			return null;
		}
		try (InputStream in = new FileInputStream(file); NativeImage image = NativeImage.read(in)) {
			int width = image.getWidth();
			int height = image.getHeight();
			if (width != SkinCompositor.SIZE || (height != SkinCompositor.SIZE && height != SkinCompositor.SIZE / 2)) {
				return null;
			}
			int[] raw = new int[width * height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					raw[y * width + x] = image.getPixelRGBA(x, y);
				}
			}
			return SkinCompositor.normalizeDownloaded(raw, width, height);
		} catch (IOException e) {
			SkinMixer.LOGGER.warn("Couldn't read downloaded skin {}", file, e);
			return null;
		}
	}

	/** Pixels as ABGR (whatever NativeImage stores), or null for anything but a 64×64 skin (vanilla already widened legacy 64×32 ones). */
	private static int[] toPixels(NativeImage image) {
		if (image.getWidth() != SkinCompositor.SIZE || image.getHeight() != SkinCompositor.SIZE) {
			return null;
		}
		int[] pixels = new int[SkinCompositor.SIZE * SkinCompositor.SIZE];
		for (int y = 0; y < SkinCompositor.SIZE; y++) {
			for (int x = 0; x < SkinCompositor.SIZE; x++) {
				pixels[y * SkinCompositor.SIZE + x] = image.getPixelRGBA(x, y);
			}
		}
		return pixels;
	}

	private static void upload(ResourceLocation id, int[] pixels) {
		NativeImage image = new NativeImage(SkinCompositor.SIZE, SkinCompositor.SIZE, true);
		for (int y = 0; y < SkinCompositor.SIZE; y++) {
			for (int x = 0; x < SkinCompositor.SIZE; x++) {
				image.setPixelRGBA(x, y, pixels[y * SkinCompositor.SIZE + x]);
			}
		}
		// register() closes whatever texture had this id before.
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
	}
}
