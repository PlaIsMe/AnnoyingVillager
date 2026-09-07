package com.pla.annoyingvillagers.clazz;

import com.pla.annoyingvillagers.client.layer.VillagerRigArrowLayer;
import com.pla.annoyingvillagers.client.layer.VillagerRigItemInHandLayer;
import com.pla.annoyingvillagers.client.model.ModelRigArmor;
import com.pla.annoyingvillagers.client.model.ModelRigVillager;
import com.pla.annoyingvillagers.client.renderer.RigLighting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;

public abstract class RigVillagerRenderer<T extends Mob> extends HumanoidMobRenderer<T, ModelRigVillager<T>> {

    protected RigVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelRigVillager<>(context.bakeLayer(ModelRigVillager.LAYER_LOCATION)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelRigArmor.INNER_LAYER_LOCATION)),
                new HumanoidModel<>(context.bakeLayer(ModelRigArmor.OUTER_LAYER_LOCATION)),
                context.getModelManager()));
        this.layers.removeIf(layer -> layer instanceof ItemInHandLayer<?, ?>);
        this.addLayer(new VillagerRigItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new VillagerRigArrowLayer<>(context, this));
    }

    @Override
    protected int getBlockLightLevel(@NotNull T entity, @NotNull BlockPos pos) {
        return entity.isOnFire() ? 15 : RigLighting.getBrightness(entity.level(), LightLayer.BLOCK, pos);
    }

    @Override
    protected int getSkyLightLevel(@NotNull T entity, @NotNull BlockPos pos) {
        return RigLighting.getBrightness(entity.level(), LightLayer.SKY, pos);
    }

    @Override
    protected float getFlipDegrees(@NotNull T entity) {
        return 0.0F;
    }
}
