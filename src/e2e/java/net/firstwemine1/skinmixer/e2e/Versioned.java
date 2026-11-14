package net.firstwemine1.skinmixer.e2e;

import java.util.List;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.WorldGenSettings;

/** The calls {@link SkinMixerClientE2E} makes that differ between Minecraft versions (this is 1.16.5). */
final class Versioned {
	private Versioned() {
	}

	/** Creates and opens a fresh creative flat-ish world from the title screen. */
	static boolean openWorld(Minecraft client) {
		// Not before the resources have finished loading, or block updates hit an empty model manager.
		if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) {
			return false;
		}
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		LevelSettings settings = new LevelSettings("skinmixerE2E" + System.currentTimeMillis(), GameType.CREATIVE, false,
				Difficulty.PEACEFUL, true, rules, DataPackConfig.DEFAULT);
		RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
		client.createLevel(settings.levelName(), settings, registries, WorldGenSettings.makeDefault(
				registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
				registries.registryOrThrow(Registry.BIOME_REGISTRY),
				registries.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY)));
		return true;
	}

	static List<AbstractWidget> widgets(Minecraft client) {
		return Screens.getButtons(client.screen);
	}

	static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name + ".png", client.getWindow().getWidth(), client.getWindow().getHeight(),
				client.getMainRenderTarget(), message -> {
		});
	}
}
