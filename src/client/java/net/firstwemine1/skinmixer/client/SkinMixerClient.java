package net.firstwemine1.skinmixer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.firstwemine1.skinmixer.SkinMixer;
import java.util.Optional;
import java.util.UUID;

import net.firstwemine1.skinmixer.outfit.Outfit;
import net.firstwemine1.skinmixer.outfit.OutfitNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;

/** Client entrypoint: the menu key, the menu button in Skin Customization, and outfit updates from the server. */
public final class SkinMixerClient implements ClientModInitializer {
	public static final KeyMapping OPEN_MENU = new KeyMapping("key.skinmixer.open", InputConstants.KEY_U, "key.categories.skinmixer");
	private static final int GARBAGE_INTERVAL_TICKS = 200;

	private int ticks;

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(OPEN_MENU);

		ClientPlayNetworking.registerGlobalReceiver(OutfitNetwork.OUTFIT, (client, handler, buf, sender) -> {
			UUID player = buf.readUUID();
			Optional<Outfit> outfit = OutfitNetwork.readOptional(buf);
			client.execute(() -> ClientOutfits.receive(player, outfit));
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!ClientOutfits.serverHasMod() && client.player != null) {
				ClientOutfits.joinedWithoutServerMod(client.player.getUUID());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientOutfits::clear));

		ClientTickEvents.END_CLIENT_TICK.register(this::tick);

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof SkinCustomizationScreen && client.player != null) {
				Screens.getButtons(screen).add(new Button(6, 6, 100, 20, Component.translatable("skinmixer.screen.open"),
						b -> client.setScreen(new SkinMixerScreen(screen))));
			}
		});
	}

	private void tick(Minecraft client) {
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
