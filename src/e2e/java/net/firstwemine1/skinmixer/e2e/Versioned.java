package net.firstwemine1.skinmixer.e2e;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** The calls {@link SkinMixerClientE2E} makes that differ between Minecraft versions (this is 26.2). */
final class Versioned {
	private Versioned() {
	}

	/** Creates and opens a fresh creative flat-ish world from the title screen. */
	static boolean openWorld(Minecraft client) {
		if (!(client.gui.screen() instanceof TitleScreen)) {
			return false;
		}
		LevelSettings settings = new LevelSettings("skinmixerE2E" + System.currentTimeMillis(), GameType.CREATIVE,
				new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
		client.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1, false, false),
				WorldPresets::createNormalWorldDimensions, client.gui.screen());
		return true;
	}

	static List<AbstractWidget> widgets(Minecraft client) {
		List<AbstractWidget> widgets = new java.util.ArrayList<>();
		for (net.minecraft.client.gui.components.events.GuiEventListener child : client.gui.screen().children()) {
			if (child instanceof AbstractWidget widget) {
				widgets.add(widget);
			}
		}
		return widgets;
	}

	static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {
		});
	}
}
