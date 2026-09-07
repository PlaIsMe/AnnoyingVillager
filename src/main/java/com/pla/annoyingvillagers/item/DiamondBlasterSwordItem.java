package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DiamondBlasterSwordItem extends SwordItem implements RigCombatProfileProvider {

    public DiamondBlasterSwordItem() {
        super(new Tier() {
            public int getUses() { return 1561; }
            public float getSpeed() { return 6.0F; }
            public float getAttackDamageBonus() { return 4.4F; }
            public int getLevel() { return 5; }
            public int getEnchantmentValue() { return 21; }
            public @NotNull Ingredient getRepairIngredient() { return Ingredient.of(new ItemStack(Items.DIAMOND)); }
        }, 3, -2.8F, new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            VanillaWeaponAbilityUtil.swingMainHand(player);
            LivingEntity target = VanillaWeaponAbilityUtil.performVanillaMeleeHit(player, 5.0D);
            if (target instanceof Mob mob && RigStunController.supports(mob)) RigStunController.applyStun(mob, RigAnimationId.SUPER_KNOCK_BACK);
            if (target != null) CommonUtil.pushEntityFromCaster(target, player);
            player.getCooldowns().addCooldown(this, 20 * 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.DIAMOND_BLASTER;
    }
}
