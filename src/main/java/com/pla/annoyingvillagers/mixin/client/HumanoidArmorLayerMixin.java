package com.pla.annoyingvillagers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.annoyingvillagers.client.renderer.ColoredGlintRenderTypes;
import com.pla.annoyingvillagers.client.renderer.ColoredGlintState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<
        T extends LivingEntity,
        M extends HumanoidModel<T>,
        A extends HumanoidModel<T>> {

    @Inject(method = "getArmorModelHook", at = @At("HEAD"), remap = false)
    private void av$setForgeArmorTarget(T entity, ItemStack stack, EquipmentSlot slot, A model,
                                        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<A> cir) {
        // Forge supplies the exact stack that this armor pass is about to render. This
        // also covers player armor and custom RigArmorLayer subclasses.
        ColoredGlintState.setTargetStack(stack);
    }

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD")
    )
    private void av$setArmorTarget(PoseStack poseStack, MultiBufferSource bufferSource, T entity,
                                   EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        ColoredGlintState.setTargetStack(entity.getItemBySlot(slot));
    }

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("RETURN")
    )
    private void av$clearArmorTarget(PoseStack poseStack, MultiBufferSource bufferSource, T entity,
                                     EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        ColoredGlintState.clear();
    }

    @ModifyExpressionValue(
            method = "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorEntityGlint()Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType av$replaceArmorGlint(RenderType original) {
        return ColoredGlintRenderTypes.getArmorEntityGlint(ColoredGlintState.getMode(), original);
    }
}
