package net.firstwemine1.skinmixer;

import net.fabricmc.api.ModInitializer;
import net.firstwemine1.skinmixer.outfit.OutfitServer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint: runs on dedicated servers and on clients (for the single player server). */
public final class SkinMixer implements ModInitializer {
	public static final String MOD_ID = "skinmixer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		OutfitServer.register();
	}
}
