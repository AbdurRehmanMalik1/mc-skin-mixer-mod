package net.firstwemine1.skinmixer.outfit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.firstwemine1.skinmixer.skin.Part;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a player wears: the arm model, and which skin each of the six parts comes from.
 *
 * <p>Only names are stored, never pixels. Every client turns the names into the same image from its
 * own copy of the skins, so an outfit is a few bytes to save and to send.
 *
 * @param slim  whether the outfit uses slim (3 px) arms
 * @param parts one skin id per {@link Part}, in {@link Part} order
 */
public record Outfit(boolean slim, List<String> parts) {
	/** The player's own real skin. On each client it means the wearer's skin, not the viewer's. */
	public static final String SELF = "self";
	/** The default skins every Minecraft client ships, in a wide and a slim version each. */
	public static final List<String> DEFAULT_SKINS =
			List.of("steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri");
	/** The built-in skin ids: the wearer's own skin first, then the defaults. */
	public static final List<String> SOURCES;
	/**
	 * Any Minecraft player's current skin, by account id: {@code mojang:} and the UUID without dashes.
	 * Every client fetches it from Mojang itself, so it still travels as a name, never as pixels.
	 */
	public static final String MOJANG_PREFIX = "mojang:";
	private static final Pattern MOJANG_ID = Pattern.compile("mojang:[0-9a-f]{32}");

	static {
		List<String> sources = new ArrayList<>();
		sources.add(SELF);
		sources.addAll(DEFAULT_SKINS);
		SOURCES = List.copyOf(sources);
	}

	private static final int MAX_ID_LENGTH = 64;

	public static final Codec<Outfit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("slim").forGetter(Outfit::slim),
			Codec.STRING.listOf().fieldOf("parts").forGetter(Outfit::parts)
	).apply(instance, Outfit::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Outfit> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Outfit::slim,
			ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).apply(ByteBufCodecs.list(Part.values().length)), Outfit::parts,
			Outfit::new);

	public Outfit {
		parts = List.copyOf(parts);
	}

	/** Every part from the wearer's own skin: looks the same as wearing no outfit. */
	public static Outfit ofSelf(boolean slim) {
		return new Outfit(slim, Collections.nCopies(Part.values().length, SELF));
	}

	public String source(Part part) {
		return parts.get(part.ordinal());
	}

	public Outfit with(Part part, String source) {
		List<String> copy = new ArrayList<>(parts);
		copy.set(part.ordinal(), source);
		return new Outfit(slim, copy);
	}

	public Outfit withSlim(boolean slim) {
		return new Outfit(slim, parts);
	}

	public boolean usesSelf() {
		return parts.contains(SELF);
	}

	public boolean allSelf() {
		return parts.stream().allMatch(SELF::equals);
	}

	/** One id per part, and every id one the mod knows. Anything from the network or disk is checked. */
	public boolean isValid() {
		return parts.size() == Part.values().length && parts.stream().allMatch(Outfit::isKnownSource);
	}

	public static boolean isKnownSource(String id) {
		return SOURCES.contains(id) || MOJANG_ID.matcher(id).matches();
	}

	public static String mojangSource(UUID player) {
		return MOJANG_PREFIX + player.toString().replace("-", "");
	}

	/** The account a {@code mojang:} id names, or null for any other id. */
	public static UUID mojangPlayer(String id) {
		if (!MOJANG_ID.matcher(id).matches()) {
			return null;
		}
		String hex = id.substring(MOJANG_PREFIX.length());
		return UUID.fromString(hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
				+ hex.substring(16, 20) + "-" + hex.substring(20));
	}
}
