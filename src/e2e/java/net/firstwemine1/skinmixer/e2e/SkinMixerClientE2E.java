package net.firstwemine1.skinmixer.e2e;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.firstwemine1.skinmixer.client.RemoteSkins;
import net.firstwemine1.skinmixer.client.SkinBaker;
import net.firstwemine1.skinmixer.client.SkinLibrary;
import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.firstwemine1.skinmixer.skin.Part;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Scripted client check for the versions without Fabric's client game tests: opens a world, drives the
 * menu with clicks, checks the baked skin pixel by pixel, adds a skin by username, and screenshots to
 * build/run/clientE2E/screenshots. Run with ./gradlew runClientE2E; exits with status 1 if a check fails.
 */
public class SkinMixerClientE2E {
	private static final int STEP_TIMEOUT = 600;
	private static final List<String> MIX = Arrays.asList("kai", "steve", "zuri", "zuri", "alex", "alex");

	private final Deque<Step> steps = new ArrayDeque<>();
	private final List<String> failures = new ArrayList<>();
	private Minecraft client;
	private int stepTicks;
	private int checks;
	private Optional<RemoteSkins.Found> notch;

	private static final class Step {
		final String name;
		final BooleanSupplier done;

		Step(String name, BooleanSupplier done) {
			this.name = name;
			this.done = done;
		}
	}

	public SkinMixerClientE2E() {
		script();
	}

	/** Once per client tick, at the end of the tick. */
	public void tick(Minecraft mc) {
		client = mc;
		tick();
	}

	private void script() {
		step("open a world", () -> Versioned.openWorld(client));
		wait("the world to load", () -> client.player != null && client.level != null && client.screen == null
				&& client.getSingleplayerServer() != null);
		pause(40);

		run("open the menu", () -> client.setScreen(new SkinMixerScreen(null)));
		pause(5);
		run("menu is open", () -> check(client.screen instanceof SkinMixerScreen, "menu opens"));
		screenshot("menu-open");

		click("Head");
		pause(10);
		screenshot("tab-head");
		click("Kai");
		click("Arms");
		click("Zuri");
		pause(10);
		screenshot("tab-arms-zuri");
		click("Legs");
		pause(10);
		screenshot("tab-legs");
		click("Apply");
		wait("the picked outfit to come back from the server", () -> ClientOutfits.get(self()) != null);
		run("picked outfit", () -> check(ClientOutfits.get(self()).parts().equals(
				Arrays.asList("kai", "self", "zuri", "zuri", "self", "self")), "picked head and arms are worn"));

		pause(12);
		run("wear a known mix", () -> ClientOutfits.choose(new Outfit(false, MIX)));
		wait("the mix to be worn", () -> MIX.equals(worn()));
		pause(5);
		run("check the baked mix", () -> checkBaked(false));
		run("third person", () -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		pause(10);
		screenshot("worn-wide-mix");

		pause(12);
		run("wear the slim mix", () -> ClientOutfits.choose(new Outfit(true, MIX)));
		wait("the slim mix to be worn", () -> ClientOutfits.get(self()) != null && ClientOutfits.get(self()).slim());
		pause(5);
		run("check the slim mix", () -> checkBaked(true));
		screenshot("worn-slim-mix");

		pause(12);
		run("own head on another body", () -> ClientOutfits.choose(
				new Outfit(false, Arrays.asList("self", "noor", "noor", "noor", "noor", "noor"))));
		wait("own head outfit", () -> "self".equals(firstPart()));
		pause(5);
		run("own head is baked", () -> check(isBaked(client.player.getSkin()), "own head outfit is baked"));
		screenshot("worn-own-head");

		run("look up Notch", () -> {
			notch = null;
			RemoteSkins.lookUp("Notch").thenAccept(found -> notch = found);
		});
		wait("the lookup", () -> notch != null);
		run("wear an added skin's head", () -> {
			if (!notch.isPresent()) {
				SkinMixer.LOGGER.info("[skinmixer-e2e] SKIP add by username: Mojang couldn't be reached");
				return;
			}
			SkinLibrary.add(notch.get().id(), notch.get().name());
			check("Notch".equals(notch.get().name()), "looked-up name is Mojang's spelling");
			Outfit outfit = Outfit.ofSelf(false).with(Part.HEAD, Outfit.mojangSource(notch.get().id()));
			ClientOutfits.choose(outfit);
		});
		pause(30);
		run("check the added head", () -> {
			if (notch.isPresent()) {
				checkRemoteHead(notch.get().id());
			}
		});
		screenshot("worn-added-head");
		run("menu with the added skin", () -> {
			client.options.setCameraType(CameraType.FIRST_PERSON);
			client.setScreen(new SkinMixerScreen(null));
		});
		pause(5);
		click("Head");
		pause(10);
		screenshot("menu-added-skin");
		click("Cancel");

		pause(12);
		run("back to the real skin", () -> ClientOutfits.choose(null));
		wait("the real skin", () -> ClientOutfits.get(self()) == null);
		pause(5);
		run("real skin restored", () -> check(!isBaked(client.player.getSkin()), "reset restores the real skin"));
	}

	// ---- checks ----

	/** The player's drawn skin is baked, with each part's pixels from the right default skin. */
	private void checkBaked(boolean slim) {
		PlayerSkin skin = client.player.getSkin();
		check(isBaked(skin), "mix is baked");
		check((skin.model() == PlayerSkin.Model.SLIM) == slim, "arm model is " + (slim ? "slim" : "wide"));
		AbstractTexture texture = client.getTextureManager().getTexture(skin.texture());
		if (!(texture instanceof DynamicTexture dynamic) || dynamic.getPixels() == null) {
			fail("baked texture has no pixels");
			return;
		}
		int[][] probes = {{10, 10}, {22, 24}, {45, 24}, {37, 56}, {5, 24}, {21, 56}};
		for (int i = 0; i < probes.length; i++) {
			int[] source = SkinBaker.texturePixels(SkinBaker.defaultTexture(MIX.get(i), slim));
			int expected = source[probes[i][1] * 64 + probes[i][0]];
			int actual = dynamic.getPixels().getPixelRGBA(probes[i][0], probes[i][1]);
			check(expected == actual, "part " + i + " of the " + (slim ? "slim" : "wide") + " mix comes from " + MIX.get(i));
		}
	}

	private void checkRemoteHead(UUID player) {
		PlayerSkin fetched = RemoteSkins.get(player);
		PlayerSkin skin = client.player.getSkin();
		if (fetched == null || !isBaked(skin)) {
			fail("added skin's head isn't worn");
			return;
		}
		int[] source = SkinBaker.texturePixels(fetched.texture());
		AbstractTexture texture = client.getTextureManager().getTexture(skin.texture());
		if (source == null || !(texture instanceof DynamicTexture dynamic)) {
			fail("added skin has no pixels");
			return;
		}
		check(source[10 * 64 + 10] == dynamic.getPixels().getPixelRGBA(10, 10), "added skin's face is worn");
	}

	private static boolean isBaked(PlayerSkin skin) {
		return skin.texture().getNamespace().equals(SkinMixer.MOD_ID);
	}

	private UUID self() {
		return client.player.getUUID();
	}

	private List<String> worn() {
		Outfit outfit = ClientOutfits.get(self());
		return outfit == null ? null : outfit.parts();
	}

	private String firstPart() {
		List<String> parts = worn();
		return parts == null ? null : parts.get(0);
	}

	// ---- steps ----

	/** Clicks the widget labelled {@code label} on the open screen, at its centre. */
	private void click(String label) {
		run("click " + label, () -> {
			for (AbstractWidget widget : Versioned.widgets(client)) {
				if (widget.visible && widget.getMessage().getString().equals(label)) {
					double x = widget.getX() + widget.getWidth() / 2.0;
					double y = widget.getY() + widget.getHeight() / 2.0;
					Screen screen = client.screen;
					screen.mouseClicked(x, y, 0);
					// The click may have closed the screen (Apply, Cancel): the release still goes to it.
					screen.mouseReleased(x, y, 0);
					return;
				}
			}
			fail("no widget labelled " + label);
		});
		pause(2);
	}

	private void screenshot(String name) {
		run("screenshot " + name, () -> Versioned.screenshot(client, name));
	}

	private void tick() {
		Step step = steps.peek();
		if (step == null) {
			finish();
			return;
		}
		stepTicks++;
		try {
			if (step.done.getAsBoolean()) {
				steps.poll();
				stepTicks = 0;
			} else if (stepTicks > STEP_TIMEOUT) {
				fail("timed out: " + step.name);
				steps.poll();
				stepTicks = 0;
			}
		} catch (RuntimeException e) {
			SkinMixer.LOGGER.error("Client E2E step {} threw", step.name, e);
			fail(step.name + " threw " + e);
			steps.poll();
			stepTicks = 0;
		}
	}

	private void finish() {
		if (failures.isEmpty()) {
			SkinMixer.LOGGER.info("CLIENT E2E PASSED: {} checks", checks);
			client.stop();
		} else {
			for (String failure : failures) {
				SkinMixer.LOGGER.error("CLIENT E2E FAILURE: {}", failure);
			}
			SkinMixer.LOGGER.error("CLIENT E2E FAILED: {} of {} checks", failures.size(), checks);
			Runtime.getRuntime().halt(1);
		}
	}

	private void step(String name, BooleanSupplier done) {
		steps.add(new Step(name, done));
	}

	private void wait(String name, BooleanSupplier condition) {
		step(name, condition);
	}

	private void run(String name, Runnable action) {
		step(name, () -> {
			action.run();
			return true;
		});
	}

	private void pause(int ticks) {
		final int[] left = {ticks};
		step("wait " + ticks + " ticks", () -> --left[0] <= 0);
	}

	private void check(boolean condition, String message) {
		checks++;
		SkinMixer.LOGGER.info("[skinmixer-e2e] {} {}", condition ? "PASS" : "FAIL", message);
		if (!condition) {
			fail(message);
		}
	}

	private void fail(String message) {
		if (!failures.contains(message)) {
			SkinMixer.LOGGER.error("CLIENT E2E FAILURE: {}", message);
			failures.add(message);
		}
	}
}
