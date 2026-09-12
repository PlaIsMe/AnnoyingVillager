package com.pla.annoyingvillagers.mixin.client;

import com.mojang.authlib.GameProfile;
import com.pla.annoyingvillagers.util.NpcTabSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
    @Shadow public abstract GameProfile getProfile();

    @Inject(method = "getSkinLocation", at = @At("HEAD"), cancellable = true)
    private void av$useNpcSkin(CallbackInfoReturnable<ResourceLocation> callback) {
        NpcTabSkin skin = NpcTabSkin.fromProfile(getProfile());
        if (skin != null) callback.setReturnValue(skin.texture());
    }
}
