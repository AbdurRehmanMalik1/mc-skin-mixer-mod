package net.firstwemine1.skinmixer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * The last outfit this player chose, in {@code config/skinmixer-outfit.json}. Worn on servers without the
 * mod (where only this client sees it); servers with the mod keep their own copy per world.
 */
final class LocalOutfit {
	private LocalOutfit() {
	}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(SkinMixer.MOD_ID + "-outfit.json");
	}

	static Outfit load() {
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try {
			JsonElement json = JsonParser.parseString(Files.readString(file));
			return Outfit.CODEC.parse(JsonOps.INSTANCE, json).result().filter(Outfit::isValid).orElse(null);
		} catch (IOException | RuntimeException e) {
			SkinMixer.LOGGER.warn("Couldn't read {}", file, e);
			return null;
		}
	}

	/** Null deletes the file: back to the real skin everywhere. */
	static void save(Outfit outfit) {
		Path file = file();
		try {
			if (outfit == null) {
				Files.deleteIfExists(file);
				return;
			}
			JsonElement json = Outfit.CODEC.encodeStart(JsonOps.INSTANCE, outfit).getOrThrow(false, error -> { });
			Files.createDirectories(file.getParent());
			Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
		} catch (IOException | RuntimeException e) {
			SkinMixer.LOGGER.warn("Couldn't save {}", file, e);
		}
	}
}
