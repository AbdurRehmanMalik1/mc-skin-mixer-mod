package net.firstwemine1.skinmixer.client.mixin;

import java.io.File;

import net.minecraft.client.renderer.texture.DownloadingTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a downloaded skin was cached on disk: its texture keeps no pixels, so the mixer reads the file. */
@Mixin(DownloadingTexture.class)
public interface HttpTextureAccessor {
	@Accessor("file")
	File skinmixer$file();
}
