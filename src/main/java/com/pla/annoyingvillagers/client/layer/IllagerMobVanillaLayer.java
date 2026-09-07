package com.pla.annoyingvillagers.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;

public class IllagerMobVanillaLayer<T extends AbstractIllager, M extends IllagerModel<T>> extends RenderLayer<T, M> {
    public IllagerMobVanillaLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation texture = VanillaOverlayTexturePicker.pickIllagerTexture(entity);
        if (texture == null) return;
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(texture));
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
