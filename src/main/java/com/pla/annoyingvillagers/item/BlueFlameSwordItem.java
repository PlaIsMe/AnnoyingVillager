package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BlueFlameSwordItem extends SwordItem implements RigCombatProfileProvider {
    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.SPECIAL_SWORD;
    }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) {
        return RigDualWieldGroup.SWORD;
    }

    @Override
    public RigCombatStyle getDualRigCombatStyle(ItemStack self, ItemStack other) {
        return RigCombatStyle.DUAL_SPECIAL_SWORD;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack pStack, @NotNull LivingEntity pTarget, @NotNull LivingEntity pAttacker) {
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && !pAttacker.level().isClientSide() && new Random().nextFloat() < 0.1F) {
            pTarget.setSecondsOnFire(2);
        }
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    public BlueFlameSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
            }

            public float getSpeed() {
                return 6.0F;
            }

            public float getAttackDamageBonus() {
                return 3.0F;
            }

            public int getLevel() {
                return 5;
            }

            public int getEnchantmentValue() {
                return 21;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.DIAMOND));
            }
        }, 3, -2.4F, (new Properties()));
    }
}