package com.pla.annoyingvillagers.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class VanillaWeaponAbilityUtil {
    public static final String EPIC_FIGHT_COMPAT_MOD_ID = "epicfight_annoyingvillagers";

    private VanillaWeaponAbilityUtil() {
    }

    public static boolean abilitiesEnabled() {
        return !ModList.get().isLoaded(EPIC_FIGHT_COMPAT_MOD_ID);
    }

    public static void swingMainHand(Player player) {
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    public static void swingOffHand(Player player) {
        player.swing(InteractionHand.OFF_HAND, true);
    }

    public static void swingBothHands(Player player) {
        swingMainHand(player);
        swingOffHand(player);
    }

    public static void damageHeldItem(Player player, InteractionHand hand, int amount) {
        if (amount <= 0) return;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;
        stack.hurtAndBreak(amount, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
    }

    public static boolean isInternalCooldownReady(Player player, String tag) {
        return player.level().getGameTime() >= player.getPersistentData().getLong(tag);
    }

    public static void setInternalCooldown(Player player, String tag, int ticks) {
        player.getPersistentData().putLong(tag, player.level().getGameTime() + Math.max(0, ticks));
    }

    public static int getInternalCooldownTicks(Player player, String tag) {
        return (int)Math.max(0L, player.getPersistentData().getLong(tag) - player.level().getGameTime());
    }

    public static void clearInternalCooldown(Player player, String tag) {
        player.getPersistentData().remove(tag);
    }

    @Nullable
    public static LivingEntity findLookTarget(Player player, double range) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity closest = null;
        double closestDistanceSqr = range * range;

        for (Entity entity : player.level().getEntities(player, searchBox, candidate -> candidate instanceof LivingEntity && candidate.isAlive() && !candidate.isSpectator())) {
            if (!(entity instanceof LivingEntity livingEntity) || !isValidTarget(player, livingEntity) || !player.hasLineOfSight(livingEntity)) continue;
            AABB targetBox = entity.getBoundingBox().inflate(Math.max(0.3D, entity.getPickRadius()));
            Vec3 hit = targetBox.contains(start) ? start : targetBox.clip(start, end).orElse(null);
            if (hit == null) continue;
            double distanceSqr = start.distanceToSqr(hit);
            if (distanceSqr < closestDistanceSqr) {
                closest = livingEntity;
                closestDistanceSqr = distanceSqr;
            }
        }

        return closest;
    }

    @Nullable
    public static LivingEntity findCommandTarget(Player player, double range) {
        LivingEntity target = player.getLastHurtMob();
        if (isValidTarget(player, target) && player.distanceToSqr(target) <= range * range) return target;
        target = player.getLastHurtByMob();
        if (isValidTarget(player, target) && player.distanceToSqr(target) <= range * range) return target;
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;

        AABB area = player.getBoundingBox().inflate(range);
        LivingEntity nearest = null;
        double nearestDistanceSqr = range * range;
        for (LivingEntity candidate : serverLevel.getEntitiesOfClass(LivingEntity.class, area, entity -> isValidTarget(player, entity))) {
            double distanceSqr = player.distanceToSqr(candidate);
            if (distanceSqr < nearestDistanceSqr) {
                nearest = candidate;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest;
    }

    public static boolean isValidTarget(LivingEntity owner, @Nullable LivingEntity target) {
        if (target == null || target == owner || !target.isAlive() || target.isSpectator()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        return !owner.isAlliedTo(target) && !target.isAlliedTo(owner);
    }

    public static boolean isVanillaCriticalAttack(Player player) {
        return player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
    }

    @Nullable
    public static LivingEntity performVanillaMeleeHit(Player player, double range) {
        LivingEntity target = findLookTarget(player, range);
        if (target == null) return null;
        player.attack(target);
        return target;
    }

    public static int getCharge(ItemStack stack, String tag, int max) {
        return Math.max(0, Math.min(max, stack.getOrCreateTag().getInt(tag)));
    }

    public static int addCharge(ItemStack stack, String tag, int amount, int max) {
        int charge = Math.max(0, Math.min(max, getCharge(stack, tag, max) + amount));
        stack.getOrCreateTag().putInt(tag, charge);
        return charge;
    }

    public static void setCharge(ItemStack stack, String tag, int amount, int max) {
        stack.getOrCreateTag().putInt(tag, Math.max(0, Math.min(max, amount)));
    }

    public static boolean hasPersistentFlag(Entity entity, String tag) {
        CompoundTag data = entity.getPersistentData();
        return data.getBoolean(tag);
    }
}
