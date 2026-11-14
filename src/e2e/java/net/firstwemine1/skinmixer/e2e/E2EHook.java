package net.firstwemine1.skinmixer.e2e;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Feeds client ticks to {@link SkinMixerClientE2E}, only when the game runs with {@code -Dskinmixer.clientE2E=true}. */
@Mod.EventBusSubscriber(modid = "skinmixer", value = Dist.CLIENT)
public final class E2EHook {
	private static SkinMixerClientE2E test;

	private E2EHook() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("skinmixer.clientE2E")) {
			return;
		}
		if (test == null) {
			test = new SkinMixerClientE2E();
		}
		test.tick(Minecraft.getInstance());
	}
}
