package net.firstwemine1.skinmixer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.firstwemine1.skinmixer.SkinMixer;
import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry: the menu key, the menu button in Skin Customization, and joining and leaving servers. */
@Mod(value = SkinMixer.MOD_ID, dist = Dist.CLIENT)
public final class SkinMixerClient {
	public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(SkinMixer.id(SkinMixer.MOD_ID));
	public static final KeyMapping OPEN_MENU = new KeyMapping("key.skinmixer.open", InputConstants.KEY_U, KEY_CATEGORY);
	private static final int GARBAGE_INTERVAL_TICKS = 200;

	private static int ticks;

	public SkinMixerClient(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(KEY_CATEGORY);
			event.register(OPEN_MENU);
		});

		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			Minecraft client = Minecraft.getInstance();
			if (!ClientOutfits.serverHasMod() && client.player != null) {
				ClientOutfits.joinedWithoutServerMod(client.player.getUUID());
			}
		});
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
				Minecraft.getInstance().execute(ClientOutfits::clear));

		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick(Minecraft.getInstance()));

		NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
			Minecraft client = Minecraft.getInstance();
			if (event.getScreen() instanceof SkinCustomizationScreen && client.player != null) {
				event.addListener(Button.builder(Component.translatable("skinmixer.screen.open"),
						b -> client.gui.setScreen(new SkinMixerScreen(event.getScreen()))).bounds(6, 6, 100, 20).build());
			}
		});
	}

	private static void tick(Minecraft client) {
		while (OPEN_MENU.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) {
				client.gui.setScreen(new SkinMixerScreen(null));
			}
		}
		if (++ticks % GARBAGE_INTERVAL_TICKS == 0) {
			ClientOutfits.collectGarbage();
		}
	}
}
