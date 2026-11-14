package net.firstwemine1.skinmixer.e2e;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Feeds client ticks to {@link SkinMixerClientE2E}, only when the game runs with {@code -Dskinmixer.clientE2E=true}. */
@EventBusSubscriber(modid = "skinmixer", value = Dist.CLIENT)
public final class E2EHook {
	private static SkinMixerClientE2E test;

	private E2EHook() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!Boolean.getBoolean("skinmixer.clientE2E")) {
			return;
		}
		if (test == null) {
			test = new SkinMixerClientE2E();
		}
		test.tick(Minecraft.getInstance());
	}
}
