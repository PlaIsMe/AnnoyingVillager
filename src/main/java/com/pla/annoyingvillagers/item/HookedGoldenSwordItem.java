package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.util.HookSwordVanillaAbilityUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class HookedGoldenSwordItem extends SwordItem implements RigCombatProfileProvider {

    public HookedGoldenSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 32;
            }

            public float getSpeed() {
                return 6.0F;
            }

            public float getAttackDamageBonus() {
                return 2.4F;
            }

            public int getLevel() {
                return 5;
            }

            public int getEnchantmentValue() {
                return 21;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.GOLD_INGOT));
            }
        }, 3, -2.4F, (new Properties()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return HookSwordVanillaAbilityUtil.useHookSword(level, player, hand);
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.HOOKED_SWORD;
    }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) {
        return RigDualWieldGroup.SWORD;
    }

    @Override
    public RigCombatStyle getDualRigCombatStyle(ItemStack self, ItemStack other) {
        return RigCombatStyle.DUAL_HOOKED_SWORD;
    }
}