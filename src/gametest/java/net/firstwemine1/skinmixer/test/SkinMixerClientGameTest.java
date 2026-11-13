package net.firstwemine1.skinmixer.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.firstwemine1.skinmixer.client.RemoteSkins;
import net.firstwemine1.skinmixer.client.SkinLibrary;
import net.firstwemine1.skinmixer.client.gui.AddSkinScreen;
import net.firstwemine1.skinmixer.skin.Part;
import net.firstwemine1.skinmixer.client.SkinBaker;
import net.firstwemine1.skinmixer.client.SkinMixerClient;
import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Drives the mod in a real client: the menu, the baked skin, the server round trip, and persistence on
 * a dedicated server. Screenshots land in build/run/clientGameTest/screenshots for looking at.
 */
public class SkinMixerClientGameTest implements FabricClientGameTest {
	private static final List<String> MIX = List.of("kai", "steve", "zuri", "zuri", "alex", "alex");

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> failures = new ArrayList<>();
		singleplayer(context, failures);
		dedicatedServer(context, failures);
		SkinMixer.LOGGER.info("[skinmixer-test] {} failed", failures.size());
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed checks:\n  " + String.join("\n  ", failures));
		}
	}

	private static void singleplayer(ClientGameTestContext context, List<String> failures) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
			context.waitTicks(5);

			// The key opens the menu.
			context.getInput().pressKey(SkinMixerClient.OPEN_MENU);
			context.waitTicks(5);
			expect(failures, "key opens the menu",
					context.computeOnClient(minecraft -> minecraft.screen instanceof SkinMixerScreen));
			context.takeScreenshot("menu-open");

			// Tabs zoom the camera; hovering a choice tries it on; clicking keeps it. All real input.
			click(context, "Head");
			context.waitTicks(10);
			context.takeScreenshot("tab-head");
			hover(context, "Kai");
			context.waitTicks(3);
			context.takeScreenshot("tab-head-hover-kai");
			click(context, "Kai");
			click(context, "Arms");
			click(context, "Zuri");
			context.waitTicks(10);
			context.takeScreenshot("tab-arms-zuri");
			click(context, "Legs");
			context.waitTicks(10);
			context.takeScreenshot("tab-legs");
			click(context, "Body");
			context.waitTicks(10);
			context.takeScreenshot("tab-body");
			click(context, "Apply");
			context.waitTicks(5);
			Outfit picked = context.computeOnClient(minecraft -> ClientOutfits.get(minecraft.player.getUUID()));
			expect(failures, "picked head and arms are worn, the rest is my own skin", picked != null
					&& picked.parts().equals(List.of("kai", "self", "zuri", "zuri", "self", "self")));

			// Randomize, then Apply.
			context.waitTicks(10);
			context.getInput().pressKey(SkinMixerClient.OPEN_MENU);
			context.waitTicks(5);
			click(context, "Randomize");
			context.waitTicks(3);
			context.takeScreenshot("menu-randomized");
			click(context, "Apply");
			context.waitTicks(5);
			expect(failures, "Apply closes the menu", context.computeOnClient(minecraft -> minecraft.screen == null));
			Outfit random = context.computeOnClient(minecraft -> ClientOutfits.get(minecraft.player.getUUID()));
			expect(failures, "Apply reaches the server and comes back", random != null && random.isValid());
			expect(failures, "Randomize uses default skins only", random != null && !random.usesSelf());

			// A known mix, checked pixel by pixel against the default skins.
			context.runOnClient(minecraft -> ClientOutfits.choose(new Outfit(false, MIX)));
			context.waitTicks(5);
			checkBaked(context, failures, "wide mix", false);
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("worn-wide-mix");

			// Inside the server's half-second cooldown: the change must wait, not get lost.
			context.runOnClient(minecraft -> ClientOutfits.choose(new Outfit(true, MIX)));
			context.waitTicks(15);
			checkBaked(context, failures, "slim mix, sent during the cooldown", true);
			context.takeScreenshot("worn-slim-mix");

			// Own head on another body: the offline test player's real skin is a default skin too.
			context.runOnClient(minecraft -> ClientOutfits.choose(
					new Outfit(false, List.of("self", "noor", "noor", "noor", "noor", "noor"))));
			context.waitTicks(5);
			context.takeScreenshot("worn-own-head");
			PlayerSkin ownHead = context.computeOnClient(minecraft -> minecraft.player.getSkin());
			expect(failures, "own head outfit is baked", ownHead.body().texturePath().getNamespace().equals(SkinMixer.MOD_ID));

			// The menu again, now showing the worn outfit, then cancel.
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(SkinMixerClient.OPEN_MENU);
			context.waitTicks(5);
			context.takeScreenshot("menu-reopened");
			click(context, "Cancel");
			context.waitTicks(2);

			// The reset command puts the real skin back.
			server.runCommand("skinmixer reset @a");
			context.waitTicks(5);
			PlayerSkin reset = context.computeOnClient(minecraft -> minecraft.player.getSkin());
			expect(failures, "reset command restores the real skin",
					!reset.body().texturePath().getNamespace().equals(SkinMixer.MOD_ID));

			addByUsername(context, failures);
		}
	}

	/**
	 * Adds a skin by username through the menu and wears its head. Needs Mojang's servers: if they can't
	 * be reached the check is skipped, not failed.
	 */
	private static void addByUsername(ClientGameTestContext context, List<String> failures) {
		context.runOnClient(minecraft -> SkinLibrary.entries().forEach(entry -> SkinLibrary.remove(entry.id())));
		context.getInput().pressKey(SkinMixerClient.OPEN_MENU);
		context.waitTicks(5);
		click(context, "Add a skin");
		context.waitTicks(3);
		expect(failures, "+ card opens the add screen", context.computeOnClient(minecraft -> minecraft.screen instanceof AddSkinScreen));
		context.getInput().typeChars("Notch");
		context.takeScreenshot("add-typed");
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		boolean added = false;
		for (int i = 0; i < 120 && !added; i++) {
			context.waitTicks(5);
			added = context.computeOnClient(minecraft ->
					minecraft.screen instanceof SkinMixerScreen && !SkinLibrary.entries().isEmpty());
		}
		if (!added) {
			context.takeScreenshot("add-not-found");
			SkinMixer.LOGGER.info("[skinmixer-test] SKIP add by username: Mojang couldn't be reached");
			context.runOnClient(minecraft -> minecraft.setScreen(null));
			return;
		}
		SkinLibrary.Entry notch = context.computeOnClient(minecraft -> SkinLibrary.entries().get(0));
		expect(failures, "looked-up name is Mojang's spelling", notch.name().equals("Notch"));
		context.waitTicks(5);
		context.takeScreenshot("added-outfit-tab");
		click(context, "Head");
		context.waitTicks(10);
		hover(context, "Notch");
		context.waitTicks(3);
		context.takeScreenshot("added-head-hover");
		click(context, "Notch");
		click(context, "Apply");
		context.waitTicks(15);
		String problem = context.computeOnClient(minecraft -> {
			Outfit worn = ClientOutfits.get(minecraft.player.getUUID());
			if (worn == null || !worn.source(Part.HEAD).equals(Outfit.mojangSource(notch.id()))) {
				return "head isn't the added skin: " + worn;
			}
			PlayerSkin skin = minecraft.player.getSkin();
			PlayerSkin fetched = RemoteSkins.get(notch.id());
			if (fetched == null) {
				return "added skin not loaded";
			}
			AbstractTexture baked = minecraft.getTextureManager().getTexture(skin.body().texturePath());
			AbstractTexture source = minecraft.getTextureManager().getTexture(fetched.body().texturePath());
			if (!(baked instanceof DynamicTexture bakedPixels) || !(source instanceof DynamicTexture sourcePixels)) {
				return "textures have no pixels";
			}
			int expected = sourcePixels.getPixels().getPixel(10, 10);
			int actual = bakedPixels.getPixels().getPixel(10, 10);
			return expected == actual ? null : "face pixel " + Integer.toHexString(actual) + " != " + Integer.toHexString(expected);
		});
		expect(failures, "wearing an added skin's head" + (problem == null ? "" : ": " + problem), problem == null);
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(5);
		context.takeScreenshot("worn-added-head");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
	}

	/** An outfit set on a dedicated server is still there after reconnecting. */
	private static void dedicatedServer(ClientGameTestContext context, List<String> failures) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (var connection = server.connect()) {
				connection.getClientLevel().waitForChunksRender();
				expect(failures, "dedicated server has the mod", context.computeOnClient(minecraft -> ClientOutfits.serverHasMod()));
				context.runOnClient(minecraft -> ClientOutfits.choose(new Outfit(false, MIX)));
				context.waitTicks(10);
				checkBaked(context, failures, "dedicated server mix", false);
			}
			try (var connection = server.connect()) {
				connection.getClientLevel().waitForChunksRender();
				context.waitTicks(10);
				Outfit back = context.computeOnClient(minecraft -> ClientOutfits.get(minecraft.player.getUUID()));
				expect(failures, "outfit survives reconnecting", back != null && back.parts().equals(MIX));
				context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				context.waitTicks(5);
				context.takeScreenshot("dedicated-reconnected");
				context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
			}
		}
	}

	/** The player's drawn skin is the baked texture, and each part's pixels come from the right default skin. */
	private static void checkBaked(ClientGameTestContext context, List<String> failures, String name, boolean slim) {
		String problem = context.computeOnClient(minecraft -> {
			PlayerSkin skin = minecraft.player.getSkin();
			if (!skin.body().texturePath().getNamespace().equals(SkinMixer.MOD_ID)) {
				return "not baked: " + skin.body().texturePath();
			}
			if ((skin.model() == PlayerModelType.SLIM) != slim) {
				return "wrong arm model " + skin.model();
			}
			AbstractTexture texture = minecraft.getTextureManager().getTexture(skin.body().texturePath());
			if (!(texture instanceof DynamicTexture dynamic) || dynamic.getPixels() == null) {
				return "baked texture has no pixels";
			}
			NativeImage baked = dynamic.getPixels();
			// One pixel inside each part's front face: head, body, right arm, left arm, right leg, left leg.
			int[][] probes = {{10, 10}, {22, 24}, {45, 24}, {37, 56}, {5, 24}, {21, 56}};
			for (int i = 0; i < probes.length; i++) {
				int expected = pixel(minecraft, SkinBaker.defaultTexture(MIX.get(i), slim), probes[i][0], probes[i][1]);
				int actual = baked.getPixel(probes[i][0], probes[i][1]);
				if (expected != actual) {
					return "part " + i + " at " + probes[i][0] + "," + probes[i][1] + ": expected "
							+ Integer.toHexString(expected) + " from " + MIX.get(i) + ", got " + Integer.toHexString(actual);
				}
			}
			return null;
		});
		expect(failures, name + (problem == null ? "" : ": " + problem), problem == null);
	}

	private static int pixel(Minecraft minecraft, Identifier texture, int x, int y) {
		try (InputStream in = minecraft.getResourceManager().getResource(texture).orElseThrow().open();
				NativeImage image = NativeImage.read(in)) {
			return image.getPixel(x, y);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Moves the real cursor onto the widget labelled {@code label} and clicks, like a player would. */
	private static void click(ClientGameTestContext context, String label) {
		hover(context, label);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	/** Moves the real cursor onto the widget labelled {@code label}. */
	private static void hover(ClientGameTestContext context, String label) {
		double[] at = context.computeOnClient(minecraft -> {
			double scale = minecraft.getWindow().getGuiScale();
			for (AbstractWidget widget : Screens.getWidgets(minecraft.screen)) {
				if (widget.getMessage().getString().equals(label)) {
					return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale,
							(widget.getY() + widget.getHeight() / 2.0) * scale};
				}
			}
			throw new AssertionError("No widget labelled " + label);
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(1);
	}

	private static void expect(List<String> failures, String name, boolean ok) {
		SkinMixer.LOGGER.info("[skinmixer-test] {} {}", ok ? "PASS" : "FAIL", name);
		if (!ok) {
			failures.add(name);
		}
	}
}
