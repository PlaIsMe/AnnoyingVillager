package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.entity.HerobrineGregEntity;
import com.pla.annoyingvillagers.entity.LowHerobrineCloneEntity;
import com.pla.annoyingvillagers.entity.LowShadowHerobrineCloneEntity;
import com.pla.annoyingvillagers.clazz.HerobrineMob;
import com.pla.annoyingvillagers.item.TransporterFragmentItem;
import com.pla.annoyingvillagers.util.HerobrinePortalUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiseFromGroundEvent {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        var level = entity.level();
        if (level.isClientSide()) return;

        var tag = entity.getPersistentData();
        if (tag.getBoolean(HerobrinePortalUtil.NBT_RISING)) {

            double targetY = tag.getDouble(HerobrinePortalUtil.NBT_TARGET_Y);
            double speed = tag.getDouble(HerobrinePortalUtil.NBT_SPEED);
            int ticks = tag.getInt(HerobrinePortalUtil.NBT_TICKS);
            int max = tag.getInt(HerobrinePortalUtil.NBT_MAX_TICKS);

            double ny = entity.getY() + speed;
            if (ny >= targetY || ticks > max) {
                moveTransitionEntity(entity, entity.getX(), targetY, entity.getZ());
                finishRise(entity);
            } else {
                moveTransitionEntity(entity, entity.getX(), ny, entity.getZ());
                tag.putInt(HerobrinePortalUtil.NBT_TICKS, ticks + 1);
            }
            return;
        }

        if (tag.getBoolean(HerobrinePortalUtil.NBT_SINKING)) {
            double speed = tag.getDouble(HerobrinePortalUtil.NBT_SINK_SPEED);
            int ticks = tag.getInt(HerobrinePortalUtil.NBT_SINK_TICKS);
            int nextTicks = ticks + 1;

            moveTransitionEntity(entity, entity.getX(), entity.getY() - speed, entity.getZ());
            tag.putInt(HerobrinePortalUtil.NBT_SINK_TICKS, nextTicks);

            if (tag.getBoolean(TransporterFragmentItem.NBT_SAVED_TELEPORT_PENDING)
                    && nextTicks >= TransporterFragmentItem.SAVED_TELEPORT_SINK_TICKS) {
                TransporterFragmentItem.finishPendingSavedTeleport(entity);
            }
        }
    }

    private static void finishRise(LivingEntity entity) {
        HerobrinePortalUtil.finishGroundTransition(entity);
        HerobrinePortalUtil.clearRiseTransitionData(entity);

        if (entity instanceof HerobrineMob herobrineMob) {
            if (herobrineMob.getGregUUID() != null) {
                Entity greg = ((ServerLevel) herobrineMob.level()).getEntity(herobrineMob.getGregUUID());
                if (greg instanceof HerobrineGregEntity herobrineGregEntity && herobrineGregEntity.isAlive()) {
                    if (herobrineGregEntity.isSummoning()) {
                        herobrineGregEntity.setSummoning(false);
                        herobrineGregEntity.setNoAi(false);
                    }
                }
            }
            herobrineMob.setInitialSpawn(false);
        }
        if (entity instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity) {
            lowHerobrineCloneEntity.setInitialSpawn(false);
        }
        if (entity instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
            lowShadowHerobrineCloneEntity.setInitialSpawn(false);
        }
    }
    private static void moveTransitionEntity(LivingEntity entity, double x, double y, double z) {
        if (entity instanceof ServerPlayer serverPlayer) TransporterFragmentItem.movePlayerTransition(serverPlayer, x, y, z);
        else HerobrinePortalUtil.moveTransitionEntity(entity, x, y, z);
    }

}
