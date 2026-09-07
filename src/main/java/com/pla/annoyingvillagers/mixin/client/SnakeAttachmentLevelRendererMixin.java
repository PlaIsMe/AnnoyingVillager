package com.pla.annoyingvillagers.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.annoyingvillagers.client.compat.BetterCombatSnakeAttachment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SnakeAttachmentLevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void av$beginSnakeFrame(PoseStack pose, float partialTick, long finishNanoTime,
                                    boolean blockOutline, Camera camera, GameRenderer gameRenderer,
                                    LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        if (ModList.get().isLoaded("bettercombat")) {
            BetterCombatSnakeAttachment.beginFrame(pose, projection, camera.getPosition());
        }
    }
}
