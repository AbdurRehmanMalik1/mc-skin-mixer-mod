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
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** The calls {@link SkinMixerClientE2E} makes that differ between Minecraft versions (this is 1.21.1). */
final class Versioned {
	private Versioned() {
	}

	/** Creates and opens a fresh creative flat-ish world from the title screen. */
	static boolean openWorld(Minecraft client) {
		if (!(client.screen instanceof TitleScreen)) {
			return false;
		}
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		LevelSettings settings = new LevelSettings("skinmixerE2E" + System.currentTimeMillis(), GameType.CREATIVE, false,
				Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
		client.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1, false, false),
				WorldPresets::createNormalWorldDimensions, client.screen);
		return true;
	}

	static List<AbstractWidget> widgets(Minecraft client) {
		List<AbstractWidget> widgets = new java.util.ArrayList<>();
		for (net.minecraft.client.gui.components.events.GuiEventListener child : client.screen.children()) {
			if (child instanceof AbstractWidget widget) {
				widgets.add(widget);
			}
		}
		return widgets;
	}

	static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name + ".png", client.getMainRenderTarget(), message -> {
		});
	}
}
