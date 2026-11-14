package net.firstwemine1.skinmixer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * The skins this player added to the mixer by username, kept in {@code config/skinmixer-library.json}.
 * Only the account id and name are saved; the skin itself is fetched from Mojang when needed.
 */
public final class SkinLibrary {
	/** One added skin. */
	public static final class Entry {
		private final UUID id;
		private final String name;

		public Entry(UUID id, String name) {
			this.id = id;
			this.name = name;
		}

		public UUID id() {
			return id;
		}

		public String name() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry other = (Entry) o;
			return java.util.Objects.equals(id, other.id) && java.util.Objects.equals(name, other.name);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(id, name);
		}

		@Override
		public String toString() {
			return "Entry[" + "id=" + id + ", " + "name=" + name + "]";
		}

		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(Entry::id),
				Codec.STRING.fieldOf("name").forGetter(Entry::name)
		).apply(instance, Entry::new));
	}

	private static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();
	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static boolean loaded;

	private SkinLibrary() {
	}

	public static List<Entry> entries() {
		load();
		return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(ENTRIES));
	}

	/** Adds a skin, or moves it to the end if it's already there. */
	public static void add(UUID id, String name) {
		load();
		ENTRIES.removeIf(entry -> entry.id().equals(id));
		ENTRIES.add(new Entry(id, name));
		save();
	}

	public static void remove(UUID id) {
		load();
		ENTRIES.removeIf(entry -> entry.id().equals(id));
		save();
	}

	public static String nameOf(UUID id) {
		load();
		return ENTRIES.stream().filter(entry -> entry.id().equals(id)).map(Entry::name).findFirst().orElse(null);
	}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(SkinMixer.MOD_ID + "-library.json");
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			JsonElement json = new JsonParser().parse(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
			CODEC.parse(JsonOps.INSTANCE, json).result().ifPresent(ENTRIES::addAll);
		} catch (IOException | RuntimeException e) {
			SkinMixer.LOGGER.warn("Couldn't read {}", file, e);
		}
	}

	private static void save() {
		Path file = file();
		try {
			JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, ENTRIES).getOrThrow(false, error -> { });
			Files.createDirectories(file.getParent());
			Files.write(file, (new GsonBuilder().setPrettyPrinting().create().toJson(json)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (IOException | RuntimeException e) {
			SkinMixer.LOGGER.warn("Couldn't save {}", file, e);
		}
	}
}
