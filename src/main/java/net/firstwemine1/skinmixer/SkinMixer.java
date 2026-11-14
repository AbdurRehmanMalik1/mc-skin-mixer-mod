package net.firstwemine1.skinmixer;

import net.firstwemine1.skinmixer.client.SkinMixerClient;
import net.firstwemine1.skinmixer.outfit.OutfitNetwork;
import net.firstwemine1.skinmixer.outfit.OutfitServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entry: runs on dedicated servers and on clients (for the single player server too). */
@Mod(SkinMixer.MOD_ID)
public final class SkinMixer {
	public static final String MOD_ID = "skinmixer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public SkinMixer() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		OutfitNetwork.register();
		OutfitServer.register();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkinMixerClient.init(modBus));
	}
}
