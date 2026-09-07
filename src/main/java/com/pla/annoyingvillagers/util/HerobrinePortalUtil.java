package com.pla.annoyingvillagers.util;

import com.pla.annoyingvillagers.entity.ShadowHerobrineEntity;
import com.pla.annoyingvillagers.clazz.HerobrineMob;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class HerobrinePortalUtil {
    public static final int SHINK_TIME_START = 40;
    public static final String NBT_RISING = "rising";
    public static final String NBT_TARGET_Y = "rise_target_y";
    public static final String NBT_SPEED = "rise_speed";
    public static final String NBT_TICKS = "rise_ticks";
    public static final String NBT_MAX_TICKS= "rise_max_ticks";

    public static final String NBT_SINKING = "sinking";
    public static final String NBT_SINK_TARGET_Y = "sink_target_y";
    public static final String NBT_SINK_SPEED = "sink_speed";
    public static final String NBT_SINK_TICKS = "sink_ticks";
    public static final String NBT_SINK_MAX_TICKS= "sink_max_ticks";

    public static void spawnHerobrine(LivingEntity livingEntity) {
        if (livingEntity.level() instanceof ServerLevel serverLevel) {
            if (livingEntity instanceof HerobrineMob herobrineMob) {
                if (herobrineMob.getGregUUID() == null) {
                    herobrineMob.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), 1.0F, 1.0F);
                }
                if (herobrineMob instanceof ShadowHerobrineEntity shadowHerobrineEntity) {
                    shadowHerobrineEntity.spawnDarkObEntities();
                }
            }
            spawnRising(serverLevel, livingEntity, livingEntity.getX(), livingEntity.getZ(), 0.03);
        }
    }

    public static <T extends LivingEntity> void spawnRising(ServerLevel level, T entity, double x, double z, double speedPerTick) {
        double groundY = entity.getY();
        double startY = groundY - 2.0;
        moveTransitionEntity(entity, x, startY, z);

        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setInvulnerable(true);

        var tag = entity.getPersistentData();
        tag.putBoolean(NBT_RISING, true);
        tag.putDouble(NBT_TARGET_Y, groundY + 0.02);
        tag.putDouble(NBT_SPEED, Math.max(0.01, speedPerTick));
        tag.putInt(NBT_TICKS, 0);
        tag.putInt(NBT_MAX_TICKS, 20 * 5);

        level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 0.6f, 0.8f + level.random.nextFloat() * 0.2f);

    }

    public static <T extends LivingEntity> void sinkIntoGround(ServerLevel level, T entity, double speedPerTick) {
        double groundY = entity.getY();
        double targetY = groundY - 1.2;

        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        var tag = entity.getPersistentData();
        tag.putBoolean(NBT_SINKING, true);
        tag.putDouble(NBT_SINK_TARGET_Y, targetY);
        tag.putDouble(NBT_SINK_SPEED, Math.max(0.01, speedPerTick));
        tag.putInt(NBT_SINK_TICKS, 0);
        tag.putInt(NBT_SINK_MAX_TICKS, 20 * 5);

        level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 0.5f, 1.2f + level.random.nextFloat() * 0.2f);

    }

    public static void finishGroundTransition(LivingEntity entity) {
        entity.noPhysics = false;
        entity.setNoGravity(false);
        entity.setInvulnerable(false);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    public static void clearRiseTransitionData(LivingEntity entity) {
        var tag = entity.getPersistentData();
        tag.remove(NBT_RISING);
        tag.remove(NBT_TARGET_Y);
        tag.remove(NBT_SPEED);
        tag.remove(NBT_TICKS);
        tag.remove(NBT_MAX_TICKS);
    }

    public static void clearSinkTransitionData(LivingEntity entity) {
        var tag = entity.getPersistentData();
        tag.remove(NBT_SINKING);
        tag.remove(NBT_SINK_TARGET_Y);
        tag.remove(NBT_SINK_SPEED);
        tag.remove(NBT_SINK_TICKS);
        tag.remove(NBT_SINK_MAX_TICKS);
    }

    public static void cancelSinkTransition(LivingEntity entity) {
        finishGroundTransition(entity);
        clearSinkTransitionData(entity);
    }

    public static void moveTransitionEntity(LivingEntity entity, double x, double y, double z) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.setPos(x, y, z);
            var tag = serverPlayer.getPersistentData();
            int transitionTicks = tag.getBoolean(NBT_RISING) ? tag.getInt(NBT_TICKS) : tag.getInt(NBT_SINK_TICKS);
            if (transitionTicks == 0 || transitionTicks % 4 == 0) serverPlayer.connection.teleport(x, y, z, serverPlayer.getYRot(), serverPlayer.getXRot());
        } else {
            entity.setPos(x, y, z);
        }
    }
}
