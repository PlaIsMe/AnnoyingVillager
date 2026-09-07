package com.pla.annoyingvillagers.util;

import com.pla.annoyingvillagers.clazz.HookDisarmLaunch;
import com.pla.annoyingvillagers.potion.ObedienceMobEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HookSwordVanillaAbilityUtil {
    private HookSwordVanillaAbilityUtil() {}

    public static InteractionResultHolder<ItemStack> useHookSword(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            VanillaWeaponAbilityUtil.swingMainHand(player);
            LivingEntity target = VanillaWeaponAbilityUtil.performVanillaMeleeHit(player, 5.0D);
            if (target != null && level.random.nextBoolean()) {
                HookDisarmLaunch launch = level.random.nextBoolean() ? HookDisarmLaunch.LEFT : HookDisarmLaunch.RIGHT;
                CommonUtil.applyHookClashDisarmLogic(player, target, serverLevel, launch);
            }
            player.getCooldowns().addCooldown(stack.getItem(), 20 * 45);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static InteractionResultHolder<ItemStack> useDnaxSword(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            VanillaWeaponAbilityUtil.swingMainHand(player);
            LivingEntity target = VanillaWeaponAbilityUtil.performVanillaMeleeHit(player, 5.0D);
            if (target instanceof Mob mob) ObedienceMobEffect.applyObedience(mob, player, player.getOffhandItem().getItem() instanceof com.pla.annoyingvillagers.item.DNAxHookedSwordItem ? 20 * 10 : 20 * 5);
            player.getCooldowns().addCooldown(stack.getItem(), 20 * 15);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
