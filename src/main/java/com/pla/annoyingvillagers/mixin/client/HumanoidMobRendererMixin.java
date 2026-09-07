package com.pla.annoyingvillagers.mixin.client;

import com.pla.annoyingvillagers.client.layer.HumanoidMobVanillaLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public abstract class HumanoidMobRendererMixin<T extends Mob, M extends HumanoidModel<T>> extends MobRenderer<T, M> {
    protected HumanoidMobRendererMixin(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void annoyingVillagers$addOverlayLayer(EntityRendererProvider.Context context, M model,
                                                    float shadowRadius, CallbackInfo ci) {
        this.addLayer(new HumanoidMobVanillaLayer<>(this));
    }
}
