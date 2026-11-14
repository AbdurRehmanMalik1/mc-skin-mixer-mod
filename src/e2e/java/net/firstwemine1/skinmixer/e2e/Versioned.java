package net.firstwemine1.skinmixer.e2e;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;

/** The calls {@link SkinMixerClientE2E} makes that differ between Minecraft versions (this is 1.16.5). */
final class Versioned {
	private Versioned() {
	}

	/** Creates and opens a fresh creative flat-ish world from the title screen. */
	static boolean openWorld(Minecraft client) {
		// Not before the resources have finished loading, or block updates hit an empty model manager.
		if (!(client.screen instanceof MainMenuScreen) || client.getOverlay() != null) {
			return false;
		}
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		WorldSettings settings = new WorldSettings("skinmixerE2E" + System.currentTimeMillis(), GameType.CREATIVE, false,
				Difficulty.PEACEFUL, true, rules, DatapackCodec.DEFAULT);
		DynamicRegistries.Impl registries = DynamicRegistries.builtin();
		client.createLevel(settings.levelName(), settings, registries, DimensionGeneratorSettings.makeDefault(
				registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
				registries.registryOrThrow(Registry.BIOME_REGISTRY),
				registries.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY)));
		return true;
	}

	static List<Widget> widgets(Minecraft client) {
		List<Widget> widgets = new java.util.ArrayList<>();
		for (net.minecraft.client.gui.IGuiEventListener child : client.screen.children()) {
			if (child instanceof Widget) {
				widgets.add((Widget) child);
			}
		}
		return widgets;
	}

	static void screenshot(Minecraft client, String name) {
		ScreenShotHelper.grab(client.gameDirectory, name + ".png", client.getWindow().getWidth(), client.getWindow().getHeight(),
				client.getMainRenderTarget(), message -> {
		});
	}
}
