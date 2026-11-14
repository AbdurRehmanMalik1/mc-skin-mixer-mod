package net.firstwemine1.skinmixer;

import net.firstwemine1.skinmixer.outfit.OutfitServer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entry: runs on dedicated servers and on clients (for the single player server too). */
@Mod(SkinMixer.MOD_ID)
public final class SkinMixer {
	public static final String MOD_ID = "skinmixer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public SkinMixer(IEventBus modBus) {
		OutfitServer.register(modBus);
	}
}
