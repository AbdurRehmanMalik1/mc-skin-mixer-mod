package net.firstwemine1.skinmixer.e2e;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** The calls {@link SkinMixerClientE2E} makes that differ between Minecraft versions (this is 1.19.2). */
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
		RegistryAccess registries = RegistryAccess.BUILTIN.get();
		client.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, registries,
				WorldPresets.createNormalWorldFromPreset(registries, 1));
		return true;
	}

	static List<AbstractWidget> widgets(Minecraft client) {
		List<AbstractWidget> widgets = new java.util.ArrayList<>();
		for (net.minecraft.client.gui.components.events.GuiEventListener child : client.screen.children()) {
			if (child instanceof AbstractWidget) {
				widgets.add((AbstractWidget) child);
			}
		}
		return widgets;
	}

	static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name + ".png", client.getMainRenderTarget(), message -> {
		});
	}
}
