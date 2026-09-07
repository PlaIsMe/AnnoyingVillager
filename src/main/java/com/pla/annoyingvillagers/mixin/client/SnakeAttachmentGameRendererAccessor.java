package com.pla.annoyingvillagers.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface SnakeAttachmentGameRendererAccessor {
    @Invoker("getFov")
    double av$handFov(Camera camera, float partialTicks, boolean useFovSetting);

    @Invoker("bobHurt")
    void av$bobHurt(PoseStack pose, float partialTicks);

    @Invoker("bobView")
    void av$bobView(PoseStack pose, float partialTicks);
}
