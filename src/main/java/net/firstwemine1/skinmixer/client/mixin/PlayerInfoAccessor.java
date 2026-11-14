package net.firstwemine1.skinmixer.client.mixin;

import java.util.function.Supplier;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The real skin, bypassing {@link PlayerInfoMixin}: the menu and "My skin" parts need it. */
@Mixin(PlayerInfo.class)
public interface PlayerInfoAccessor {
	@Accessor("skinLookup")
	Supplier<PlayerSkin> skinmixer$skinLookup();
}
