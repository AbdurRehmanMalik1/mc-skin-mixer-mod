package net.firstwemine1.skinmixer.outfit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraft.core.UUIDUtil;

/** Every player's outfit on this server, saved in the world folder as {@code skinmixer/outfits.json}. */
public final class OutfitStore {
	private static final Codec<Map<UUID, Outfit>> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Outfit.CODEC);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;
	private final Map<UUID, Outfit> outfits = new HashMap<>();

	private OutfitStore(Path file) {
		this.file = file;
	}

	public static OutfitStore load(Path worldFolder) {
		OutfitStore store = new OutfitStore(worldFolder.resolve(SkinMixer.MOD_ID).resolve("outfits.json"));
		if (Files.isRegularFile(store.file)) {
			try {
				JsonElement json = JsonParser.parseString(Files.readString(store.file));
				CODEC.parse(JsonOps.INSTANCE, json)
						.resultOrPartial(error -> SkinMixer.LOGGER.warn("Skipping bad outfits in {}: {}", store.file, error))
						.ifPresent(loaded -> loaded.forEach((id, outfit) -> {
							if (outfit.isValid()) {
								store.outfits.put(id, outfit);
							}
						}));
			} catch (IOException | RuntimeException e) {
				SkinMixer.LOGGER.error("Couldn't read outfits from {}", store.file, e);
			}
		}
		return store;
	}

	public Outfit get(UUID player) {
		return outfits.get(player);
	}

	public void put(UUID player, Outfit outfit) {
		if (outfit == null) {
			outfits.remove(player);
		} else {
			outfits.put(player, outfit);
		}
		save();
	}

	private void save() {
		try {
			JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, outfits).getOrThrow();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(json));
		} catch (IOException | RuntimeException e) {
			SkinMixer.LOGGER.error("Couldn't save outfits to {}", file, e);
		}
	}
}
