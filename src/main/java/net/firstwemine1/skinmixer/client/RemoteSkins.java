package net.firstwemine1.skinmixer.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicReference;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.firstwemine1.skinmixer.SkinMixer;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Any Minecraft player's current skin, fetched from Mojang by account id, the way vanilla fetches the
 * skin on a player head. Each client fetches for itself; nothing goes through the game server.
 *
 * <p>Main thread only, except the lookups themselves, which run in the background.
 */
public final class RemoteSkins {
	private enum State { LOADING, READY, FAILED }

	private static final class Entry {
		State state = State.LOADING;
		PlayerSkin skin;
		String name;
	}

	private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
	/** Goes up every time a skin finishes loading, so whatever waited for one can try again. */
	private static int version;

	private RemoteSkins() {
	}

	/** The skin, or null while it loads or if it couldn't be fetched. Starts fetching on first ask. */
	public static PlayerSkin get(UUID player) {
		Entry entry = ENTRIES.get(player);
		if (entry == null) {
			entry = new Entry();
			ENTRIES.put(player, entry);
			Entry loading = entry;
			CompletableFuture.supplyAsync(() -> fetchById(player), Util.backgroundExecutor())
					.thenCompose(RemoteSkins::skinOf)
					.whenCompleteAsync((skin, error) -> finish(loading, skin == null ? null : skin.orElse(null), error, player),
							Minecraft.getInstance());
		}
		return entry.state == State.READY ? entry.skin : null;
	}

	public static boolean isLoading(UUID player) {
		Entry entry = ENTRIES.get(player);
		return entry == null || entry.state == State.LOADING;
	}

	public static int version() {
		return version;
	}

	/** A found player: their account id, their name as Mojang spells it, and their skin. */
	public record Found(UUID id, String name, PlayerSkin skin) {
	}

	/**
	 * Looks a player up by name. Completes on the main thread, with empty if there's no such player or
	 * Mojang couldn't be reached.
	 */
	public static CompletableFuture<Optional<Found>> lookUp(String name) {
		Minecraft minecraft = Minecraft.getInstance();
		return CompletableFuture.supplyAsync(() -> fetchIdByName(name).flatMap(RemoteSkins::fetchById), Util.backgroundExecutor())
				.thenCompose(profile -> skinOf(profile).thenApply(skin -> skin.map(s -> new Found(profile.get().getId(), profile.get().getName(), s))))
				.exceptionally(error -> {
					SkinMixer.LOGGER.warn("Couldn't look up player {}", name, error);
					return Optional.empty();
				})
				.thenApplyAsync(found -> {
					found.ifPresent(f -> {
						Entry entry = new Entry();
						entry.state = State.READY;
						entry.skin = f.skin();
						entry.name = f.name();
						ENTRIES.put(f.id(), entry);
						version++;
					});
					return found;
				}, minecraft);
	}

	private static CompletableFuture<Optional<PlayerSkin>> skinOf(Optional<GameProfile> profile) {
		if (profile.isEmpty()) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return Minecraft.getInstance().getSkinManager().getOrLoad(profile.get()).thenApply(Optional::of);
	}

	/** The full profile, with the skin in it, from Mojang's session server. Blocking. */
	private static Optional<GameProfile> fetchById(UUID player) {
		ProfileResult result = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(player, false);
		return Optional.ofNullable(result).map(ProfileResult::profile);
	}

	/** A player's account id from their name, from Mojang's profile API. Blocking. */
	private static Optional<UUID> fetchIdByName(String name) {
		GameProfileRepository repository = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy()).createProfileRepository();
		AtomicReference<UUID> found = new AtomicReference<>();
		repository.findProfilesByNames(new String[] {name}, new ProfileLookupCallback() {
			@Override
			public void onProfileLookupSucceeded(GameProfile profile) {
				found.set(profile.getId());
			}

			@Override
			public void onProfileLookupFailed(String profileName, Exception exception) {
			}
		});
		return Optional.ofNullable(found.get());
	}

	private static void finish(Entry entry, PlayerSkin skin, Throwable error, UUID player) {
		if (skin != null) {
			entry.state = State.READY;
			entry.skin = skin;
		} else {
			entry.state = State.FAILED;
			SkinMixer.LOGGER.warn("Couldn't fetch the skin of player {}", player, error);
		}
		version++;
		ClientOutfits.remoteSkinChanged();
	}

	/** Leaving a world: forget failures so they're tried again next time; loaded skins stay cached. */
	static void forgetFailures() {
		ENTRIES.values().removeIf(entry -> entry.state == State.FAILED);
	}
}
