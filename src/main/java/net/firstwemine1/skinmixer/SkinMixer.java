package net.firstwemine1.skinmixer;

import net.firstwemine1.skinmixer.client.SkinMixerClient;
import net.firstwemine1.skinmixer.outfit.OutfitNetwork;
import net.firstwemine1.skinmixer.outfit.OutfitServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Mod entry: runs on dedicated servers and on clients (for the single player server too). */
@Mod(SkinMixer.MOD_ID)
public final class SkinMixer {
	public static final String MOD_ID = "skinmixer";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public SkinMixer() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		OutfitNetwork.register();
		OutfitServer.register();
		// Optional on either side: clients without it can join a server with it, and the other way round.
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
				() -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkinMixerClient.init(modBus));
	}
}
