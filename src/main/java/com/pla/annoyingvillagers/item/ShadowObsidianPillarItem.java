package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.clazz.HerobrineObsidianBlock;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModBlocks;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShadowObsidianPillarItem extends SwordItem implements RigCombatProfileProvider {
    public static final int VANILLA_ABILITY_COOLDOWN_TICKS = 20 * 30;
    public static final String BURST_TAG = "ShadowObsidianBurst";
    private static final String BURST_UNTIL_TAG = "ShadowObsidianBurstUntil";
    private static final int BURST_VISUAL_TICKS = 20;

    public ShadowObsidianPillarItem() {
        super(new Tier() {
            public int getUses() { return 3000; }
            public float getSpeed() { return 50.0F; }
            public float getAttackDamageBonus() { return 2.0F; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 0; }
            public @NotNull Ingredient getRepairIngredient() { return Ingredient.of(new ItemStack(AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get())); }
        }, 3, 0.5F, new Properties().fireResistant());
    }

    public static boolean isBurst(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(BURST_TAG);
    }

    public static void onVanillaCriticalHit(ItemStack stack, Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || !(player.level() instanceof ServerLevel serverLevel)) return;
        BlockState state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_SHORT_PILLAR.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, true).setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
        Vec3 handPos = CommonUtil.getVanillaSwordOrBodyPosition(player);
        if (handPos == null) handPos = player.getEyePosition().add(player.getLookAngle().scale(0.5D));
        HerobrineUtil.summonObsidianBlocksFromPosition(serverLevel, player, state, 2, handPos);
        setBurstForTicks(stack, player.level(), BURST_VISUAL_TICKS);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return super.use(level, player, hand);
        setBurstForTicks(stack, level, BURST_VISUAL_TICKS);
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_LONG_PILLAR.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, true).setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
            HerobrineUtil.summonObsidianCube3x3x3(serverLevel, player, state);
            VanillaWeaponAbilityUtil.swingBothHands(player);
            player.getCooldowns().addCooldown(this, VANILLA_ABILITY_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }


    private static void setBurstForTicks(ItemStack stack, Level level, int ticks) {
        stack.getOrCreateTag().putBoolean(BURST_TAG, true);
        stack.getOrCreateTag().putLong(BURST_UNTIL_TAG, level.getGameTime() + Math.max(1, ticks));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!stack.hasTag() || stack.getTag() == null || !stack.getTag().getBoolean(BURST_TAG)) return;
        if (level.getGameTime() < stack.getTag().getLong(BURST_UNTIL_TAG)) return;
        stack.getTag().remove(BURST_TAG);
        stack.getTag().remove(BURST_UNTIL_TAG);
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || !(player.getMainHandItem().getItem() instanceof ShadowObsidianPillarItem item) || player.getCooldowns().isOnCooldown(item) || !(player.level() instanceof ServerLevel serverLevel)) return false;
        HerobrineUtil.summonShadowObsidianLongPillarShootToward(serverLevel, player);
        VanillaWeaponAbilityUtil.swingMainHand(player);
        player.getCooldowns().addCooldown(item, VANILLA_ABILITY_COOLDOWN_TICKS);
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.translatable("tooltip.annoyingvillagers.shadow_obsidian_pillar"));
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) { return RigCombatStyle.OBSIDIAN_WEAPON; }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) { return RigDualWieldGroup.OBSIDIAN_SWORD; }

    @Override
    public RigCombatStyle getDualRigCombatStyle(ItemStack self, ItemStack other) { return RigCombatStyle.DUAL_OBSIDIAN_SWORD; }
}
