package com.pla.annoyingvillagers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.annoyingvillagers.client.renderer.ColoredGlintRenderTypes;
import com.pla.annoyingvillagers.client.renderer.ColoredGlintState;
import com.pla.annoyingvillagers.client.compat.BetterCombatSnakeAttachment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", at = @At("HEAD"))
    private void av$setSnakeItemOwner(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                      boolean leftHand, PoseStack pose, MultiBufferSource buffer,
                                      Level level, int light, int overlay, int seed, CallbackInfo ci) {
        if (ModList.get().isLoaded("bettercombat")) BetterCombatSnakeAttachment.setItemOwner(entity);
    }

    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", at = @At("RETURN"))
    private void av$clearSnakeItemOwner(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                        boolean leftHand, PoseStack pose, MultiBufferSource buffer,
                                        Level level, int light, int overlay, int seed, CallbackInfo ci) {
        if (ModList.get().isLoaded("bettercombat")) BetterCombatSnakeAttachment.setItemOwner(null);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;isCustomRenderer()Z"))
    private void av$captureSnakeSocket(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                       PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                                       BakedModel model, CallbackInfo ci) {
        if (ModList.get().isLoaded("bettercombat")) BetterCombatSnakeAttachment.capture(stack, context, pose);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void setTargetStack(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                PoseStack pose, MultiBufferSource buf, int light, int overlay,
                                BakedModel model, CallbackInfo ci) {
        ColoredGlintState.setTargetStack(stack);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void clearTargetStack(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                  PoseStack pose, MultiBufferSource buf, int light, int overlay,
                                  BakedModel model, CallbackInfo ci) {
        ColoredGlintState.clear();
    }

    @ModifyExpressionValue(
            method = "getFoilBufferDirect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;glintDirect()Lnet/minecraft/client/renderer/RenderType;")
    )
    private static RenderType glintDirect(RenderType original) {
        return ColoredGlintRenderTypes.getGlintDirect(ColoredGlintState.getMode(), original);
    }

    @ModifyExpressionValue(
            method = "getFoilBufferDirect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityGlintDirect()Lnet/minecraft/client/renderer/RenderType;")
    )
    private static RenderType entityGlintDirect(RenderType original) {
        return ColoredGlintRenderTypes.getEntityGlintDirect(ColoredGlintState.getMode(), original);
    }

    @ModifyExpressionValue(
            method = "getFoilBuffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;glint()Lnet/minecraft/client/renderer/RenderType;")
    )
    private static RenderType glint(RenderType original) {
        return ColoredGlintRenderTypes.getGlint(ColoredGlintState.getMode(), original);
    }

    @ModifyExpressionValue(
            method = "getFoilBuffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityGlint()Lnet/minecraft/client/renderer/RenderType;")
    )
    private static RenderType entityGlint(RenderType original) {
        return ColoredGlintRenderTypes.getEntityGlint(ColoredGlintState.getMode(), original);
    }

    @ModifyExpressionValue(
            method = "getArmorFoilBuffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;armorEntityGlint()Lnet/minecraft/client/renderer/RenderType;")
    )
    private static RenderType armorEntityGlint(RenderType original) {
        return ColoredGlintRenderTypes.getArmorEntityGlint(ColoredGlintState.getMode(), original);
    }
}
