package net.firstwemine1.skinmixer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client setup: the menu key, the menu button in Skin Customization, and joining and leaving servers. */
public final class SkinMixerClient {
	public static final KeyMapping OPEN_MENU = new KeyMapping("key.skinmixer.open", InputConstants.KEY_U, "key.categories.skinmixer");
	private static final int GARBAGE_INTERVAL_TICKS = 200;

	private static int ticks;

	private SkinMixerClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN_MENU));

		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			Minecraft client = Minecraft.getInstance();
			if (!ClientOutfits.serverHasMod() && client.player != null) {
				ClientOutfits.joinedWithoutServerMod(client.player.getUUID());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
				Minecraft.getInstance().execute(ClientOutfits::clear));

		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				tick(Minecraft.getInstance());
			}
		});

		MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
			Minecraft client = Minecraft.getInstance();
			if (event.getScreen() instanceof SkinCustomizationScreen && client.player != null) {
				event.addListener(Button.builder(Component.translatable("skinmixer.screen.open"),
						b -> client.setScreen(new SkinMixerScreen(event.getScreen()))).bounds(6, 6, 100, 20).build());
			}
		});
	}

	private static void tick(Minecraft client) {
		while (OPEN_MENU.consumeClick()) {
			if (client.player != null && client.screen == null) {
				client.setScreen(new SkinMixerScreen(null));
			}
		}
		if (++ticks % GARBAGE_INTERVAL_TICKS == 0) {
			ClientOutfits.collectGarbage();
		}
	}
}
