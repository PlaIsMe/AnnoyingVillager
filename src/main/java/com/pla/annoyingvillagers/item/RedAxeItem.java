package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class RedAxeItem extends SwordItem implements RigCombatProfileProvider {
    public static final int VANILLA_ULT_COOLDOWN_TICKS = 20 * 15;
    private static final int GIANT_FORM_TICKS = 10;
    private static final double VANILLA_MELEE_RANGE = 5.0D;
    private static final String GIANT_FORM_UNTIL_TAG = "AnnoyingVillagersRedAxeGiantUntil";

    public RedAxeItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
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
                return Ingredient.of(new ItemStack(Items.WOODEN_PICKAXE));
            }
        }, 3, -3.0F, (new Properties()));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()
                || hand != InteractionHand.MAIN_HAND
                || player.getCooldowns().isOnCooldown(this)) {
            return super.use(level, player, hand);
        }

        if (level instanceof ServerLevel serverLevel) {
            stack.getOrCreateTag().putLong(GIANT_FORM_UNTIL_TAG, level.getGameTime() + GIANT_FORM_TICKS);
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_TWO_HANDED_SLAM);
            LivingEntity target = VanillaWeaponAbilityUtil.findLookTarget(player, VANILLA_MELEE_RANGE);
            if (target != null) {
                float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
                target.hurt(serverLevel.damageSources().playerAttack(player), damage);
            }
            player.getCooldowns().addCooldown(this, VANILLA_ULT_COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean isGiantForm(ItemStack stack, Level level) {
        return level != null
                && stack.hasTag()
                && level.getGameTime() < stack.getTag().getLong(GIANT_FORM_UNTIL_TAG);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack pStack) {
        return true;
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.RED_AXE;
    }
}
