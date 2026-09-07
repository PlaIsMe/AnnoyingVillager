package com.pla.annoyingvillagers.client.renderer;

import com.pla.annoyingvillagers.client.animation.RigClientAnimationState;
import com.pla.annoyingvillagers.entity.AngrySteveEntity;
import com.pla.annoyingvillagers.entity.ShadowHerobrineEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.item.*;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class RigItemVisualResolver {
    private RigItemVisualResolver() {}

    public static ItemStack resolve(Entity entity, ItemStack originalStack, boolean leftHand) {
        if (originalStack.getItem() instanceof LegendarySwordItem
                && entity instanceof AngrySteveEntity angrySteve
                && angrySteve.isLegendaryAwakened()) {
            ItemStack awakenedStack = originalStack.copy();
            awakenedStack.getOrCreateTag().putInt("CustomModelData", 1);
            return awakenedStack;
        }

        RigClientAnimationState.Active active = RigClientAnimationState.getActive(entity, entity.tickCount);
        if (active == null) return originalStack;

        RigAnimationId animationId = active.animationId();
        float animationTick = active.elapsedTicks(entity.tickCount);

        if (originalStack.getItem() instanceof RedAxeItem
                && animationId == RigAnimationId.GREATAXE_ULT) {
            return new ItemStack(AnnoyingVillagersModItems.GIANT_RED_AXE.get());
        }

        if (originalStack.getItem() instanceof DNAxHookedSwordItem
                && (animationId == RigAnimationId.BASIC_ULT || animationId == RigAnimationId.DUAL_BASIC_ULT)) {
            return new ItemStack(AnnoyingVillagersModItems.DNAX_HOOKED_SWORD_ABILITY.get());
        }

        if (originalStack.getItem() instanceof LegendarySwordItem
                && (animationId == RigAnimationId.LEGENDARY_SWORD_ULT || animationId == RigAnimationId.LEGENDARY_SWORD_EXTRA_ULT)) {
            return new ItemStack(AnnoyingVillagersModItems.HEAVY_ATTACK_LEGENDARY_SWORD.get());
        }

        if ((originalStack.getItem() instanceof ObsidianWeaponItem
                || originalStack.getItem() instanceof ShadowObsidianWeaponItem)
                && (animationId == RigAnimationId.HEROBRINE_RUN
                || animationId == RigAnimationId.FIST_ATTACK1 || animationId == RigAnimationId.FIST_ATTACK2
                || animationId == RigAnimationId.FIST_ATTACK3 || animationId == RigAnimationId.FIST_ATTACK4
                || animationId == RigAnimationId.FIST_ATTACK5 || animationId == RigAnimationId.FIST_EXTRA_ATTACK
                || animationId == RigAnimationId.OBSIDIAN_EXTRA_ULT)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.getItem() instanceof ShadowObsidianPillarItem
                && (animationId == RigAnimationId.FIST_ATTACK1 || animationId == RigAnimationId.FIST_ATTACK3
                || animationId == RigAnimationId.FIST_ATTACK5 || animationId == RigAnimationId.OBSIDIAN_ULT1
                || animationId == RigAnimationId.OBSIDIAN_ULT2 || animationId == RigAnimationId.FIST_EXTRA_ATTACK
                || animationId == RigAnimationId.OBSIDIAN_EXTRA_ULT)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.getItem() instanceof ShadowObsidianPillarItem
                && animationId == RigAnimationId.FIST_ATTACK2
                && animationTick >= 4.0F) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_BURST.get());
        }

        if (originalStack.getItem() instanceof ShadowObsidianPillarItem
                && animationId == RigAnimationId.FIST_ATTACK4
                && animationTick >= 10.0F) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_BURST.get());
        }

        if (originalStack.getItem() instanceof ShadowObsidianPillarItem
                && animationId == RigAnimationId.OBSIDIAN_PILLAR_EXTRA_ATTACK
                && animationTick >= 12.0F) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_BURST.get());
        }

        if (originalStack.getItem() instanceof ShadowObsidianSwordItem
                && animationId == RigAnimationId.OBSIDIAN_PILLAR_EXTRA_ATTACK
                && animationTick >= 12.0F) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_STRAIGHT.get());
        }

        if (originalStack.getItem() instanceof ShadowObsidianSwordItem
                && animationId == RigAnimationId.OBSIDIAN_SWORD_ULT
                && animationTick >= 9.0F) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_STRAIGHT.get());
        }

        if (originalStack.getItem() instanceof ShadowObsidianSwordItem
                && (animationId == RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK2 || animationId == RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK3
                || animationId == RigAnimationId.DUAL_OBSIDIAN_SWORD_JUMP_ATTACK || animationId == RigAnimationId.DUAL_OBSIDIAN_SWORD_ULT)) {
            return new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_STRAIGHT.get());
        }

        if (entity instanceof ShadowHerobrineEntity
                && leftHand && animationId == RigAnimationId.OBSIDIAN_MACHINE_GUN) {
            return new ItemStack(AnnoyingVillagersModItems.HEROBRINE_ENDER_EYE.get());
        }

        return originalStack;
    }
}