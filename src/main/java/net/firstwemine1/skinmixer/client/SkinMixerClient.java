package net.firstwemine1.skinmixer.client;

import net.firstwemine1.skinmixer.client.gui.SkinMixerScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.CustomizeSkinScreen;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/** Client setup: the menu key, the menu button in Skin Customization, and joining and leaving servers. */
public final class SkinMixerClient {
	public static final KeyBinding OPEN_MENU = new KeyBinding("key.skinmixer.open", GLFW.GLFW_KEY_U, "key.categories.skinmixer");
	private static final int GARBAGE_INTERVAL_TICKS = 200;

	private static int ticks;

	private SkinMixerClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ClientRegistry.registerKeyBinding(OPEN_MENU)));

		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedInEvent event) -> {
			Minecraft client = Minecraft.getInstance();
			if (!ClientOutfits.serverHasMod() && client.player != null) {
				ClientOutfits.joinedWithoutServerMod(client.player.getUUID());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) ->
				Minecraft.getInstance().execute(ClientOutfits::clear));

		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				tick(Minecraft.getInstance());
			}
		});

		MinecraftForge.EVENT_BUS.addListener((GuiScreenEvent.InitGuiEvent.Post event) -> {
			Minecraft client = Minecraft.getInstance();
			if (event.getGui() instanceof CustomizeSkinScreen && client.player != null) {
				event.addWidget(new Button(6, 6, 100, 20, new TranslationTextComponent("skinmixer.screen.open"),
						b -> client.setScreen(new SkinMixerScreen(event.getGui()))));
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
