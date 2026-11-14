package net.firstwemine1.skinmixer;

import net.fabricmc.api.ModInitializer;
import net.firstwemine1.skinmixer.outfit.OutfitServer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Common entrypoint: runs on dedicated servers and on clients (for the single player server). */
public final class SkinMixer implements ModInitializer {
	public static final String MOD_ID = "skinmixer";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		OutfitServer.register();
	}
}
