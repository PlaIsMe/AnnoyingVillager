package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.item.BlueDemonChestplateItem;
import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import com.pla.annoyingvillagers.item.HerobrineEnderEyeItem;
import com.pla.annoyingvillagers.item.NullWeaponItem;
import com.pla.annoyingvillagers.item.TransporterFragmentItem;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class SpecialAttackOnKeyHeldEvent {
    private static boolean efmConditionToExecute(Entity entity) {
//        Add this in AV_EFM
//        PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(entity, PlayerPatch.class);
//        LivingEntityPatch<?> livingEntityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
//        if (livingEntityPatch == null) return false;
//        AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(livingEntityPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
//        if (EpicfightUtil.isLongHitAnimation(dynamicAnimation, livingEntityPatch)) {
//            return false;
//        }
//        if (entity.level() instanceof ServerLevel) {
//            if (dynamicAnimation != Animations.EMPTY_ANIMATION) {
//                return false;
//            }
//        }
        return true;
    }

    private static void playPortalSummonAnimation() {
//        Add this in AV_EFM
//        livingEntityPatch.playAnimationSynchronized(AnimsSculkSteve.PORTAL_SUMMON, 0.0F);
//        create VANILLA_ANIMATION
    }

    private static void playChestplateActivationAnimation() {
//        Add this in AV_EFM
//        livingEntityPatch.playAnimationSynchronized(AnimsWom.CUT_ANTITHEUS_ASCENSION, 0.0F);
//        create VANILLA_ANIMATION
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(world, entity, null);
    }

    public static void execute(LevelAccessor world, Entity entity, Vec3 crosshairTarget) {
        if (entity == null) return;
        if (!efmConditionToExecute(entity)) {
            return;
        }


        if (entity instanceof Player player && !player.level().isClientSide()) {
            if (HerobrineEnderEyeItem.activateVanillaHeldSpecial(player)) return;
            TransporterFragmentItem.UseResult transporterUseResult = TransporterFragmentItem.tryUseHeldSpecialAttack(player, crosshairTarget);
            if (transporterUseResult.consumed()) {
                if (transporterUseResult.activated()) {
                    if (transporterUseResult.mode() == TransporterFragmentItem.UseMode.OFF_HAND) VanillaWeaponAbilityUtil.swingOffHand(player);
                    else VanillaWeaponAbilityUtil.swingMainHand(player);
                    playPortalSummonAnimation();
                }
                return;
            }
            if (BlueDemonTridentItem.activateVanillaFestival(player)) return;
            if (NullWeaponItem.activateHeldSpecial(player)) return;
        }

        if (entity instanceof Player player) {
            if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BlueDemonChestplateItem && BlueDemonChestplateItem.isBlueDemonChestplate(player.getItemBySlot(EquipmentSlot.CHEST))) {
                if (entity.level() instanceof ServerLevel) {
                    playChestplateActivationAnimation();
                }
            }
        }
    }
}
