package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.clazz.HerobrineObsidianBlock;
import com.pla.annoyingvillagers.entity.BlockProjectileEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModBlocks;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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

public class ShadowObsidianSwordItem extends SwordItem implements RigCombatProfileProvider {
    public static final int VANILLA_ULT_COOLDOWN_TICKS = 20 * 30;
    public static final int VANILLA_PROJECTILE_COOLDOWN_TICKS = 20 * 10;
    public static final int VANILLA_STRAIGHT_FORM_TICKS = 20;
    public static final String STRAIGHT_FORM_TAG = "ShadowObsidianSwordStraightForm";
    private static final String STRAIGHT_FORM_UNTIL_TAG = "ShadowObsidianSwordStraightFormUntil";

    public ShadowObsidianSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 3000;
            }

            public float getSpeed() {
                return 4.0F;
            }

            public float getAttackDamageBonus() {
                return 3.0F;
            }

            public int getLevel() {
                return 4;
            }

            public int getEnchantmentValue() {
                return 0;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of();
            }
        }, 3, -2.5F, new Properties());
    }

    public static boolean isStraightForm(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(STRAIGHT_FORM_TAG);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return super.use(level, player, hand);
        setStraightFormForTicks(stack, level, VANILLA_STRAIGHT_FORM_TICKS);
        if (level instanceof ServerLevel serverLevel) {
            VanillaWeaponAbilityUtil.swingMainHand(player);
            VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
            scheduleVanillaUltStrike(serverLevel, player, stack, 10);
            scheduleVanillaUltStrike(serverLevel, player, stack, 17);
            scheduleVanillaUltStrike(serverLevel, player, stack, 28);
            player.getCooldowns().addCooldown(this, VANILLA_ULT_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void scheduleVanillaUltStrike(ServerLevel serverLevel, Player player, ItemStack stack, int delayTicks) {
        new DelayedTask(delayTicks) {
            @Override
            public void run() {
                if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.isRemoved() || !player.isAlive() || stack.isEmpty() || player.level() != serverLevel) return;
                Vec3 look = player.getLookAngle();
                Vec3 forward = new Vec3(look.x, 0.0D, look.z);
                if (forward.lengthSqr() < 1.0E-6D) {
                    Direction direction = player.getDirection();
                    forward = new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
                } else {
                    forward = forward.normalize();
                }
                Vec3 impactPos = player.position().add(forward.scale(1.4D));
                CommonUtil.spawnGroundSlamFracture(player, serverLevel, impactPos, 0.55D, 25, 0.5D, 1.5D);
                HerobrineUtil.summonShadowObsidianLongPillarDefense(serverLevel, player);
            }
        };
    }

    public static void onVanillaCriticalHit(ItemStack stack, Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return;
        setStraightFormForTicks(stack, player.level(), VANILLA_STRAIGHT_FORM_TICKS);
    }

    private static void setStraightFormForTicks(ItemStack stack, Level level, int ticks) {
        stack.getOrCreateTag().putBoolean(STRAIGHT_FORM_TAG, true);
        stack.getOrCreateTag().putLong(STRAIGHT_FORM_UNTIL_TAG, level.getGameTime() + Math.max(1, ticks));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!stack.hasTag() || stack.getTag() == null || !stack.getTag().getBoolean(STRAIGHT_FORM_TAG)) return;
        if (level.getGameTime() < stack.getTag().getLong(STRAIGHT_FORM_UNTIL_TAG)) return;
        stack.getTag().remove(STRAIGHT_FORM_TAG);
        stack.getTag().remove(STRAIGHT_FORM_UNTIL_TAG);
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ShadowObsidianSwordItem item) || player.getCooldowns().isOnCooldown(item)) return false;

        VanillaWeaponAbilityUtil.swingMainHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        player.getCooldowns().addCooldown(item, VANILLA_PROJECTILE_COOLDOWN_TICKS);
        new DelayedTask(12) { @Override public void run() { if (player.isAlive() && !player.isRemoved() && player.level() == serverLevel) throwVanillaObsidianProjectile(serverLevel, player); } };
        return true;
    }

    private static void throwVanillaObsidianProjectile(ServerLevel serverLevel, Player player) {
        BlockState state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_MIDDLE_PILLAR.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, true).setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
        Vec3 origin = CommonUtil.getVanillaSwordOrBodyPosition(player);
        if (origin == null) origin = player.getEyePosition(1.0F);
        LivingEntity target = VanillaWeaponAbilityUtil.findLookTarget(player, 16.0D);
        Vec3 destination = target != null ? target.getEyePosition(1.0F) : player.getEyePosition(1.0F).add(player.getLookAngle().scale(16.0D));
        Vec3 direction = destination.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();

        BlockProjectileEntity projectile = new BlockProjectileEntity(serverLevel, player, state);
        projectile.setOwnerUUID(player.getUUID());
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.setDeltaMovement(direction.normalize().scale(2.0D));
        serverLevel.addFreshEntity(projectile);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.translatable("tooltip.annoyingvillagers.shadow_obsidian_sword"));
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.OBSIDIAN_SWORD;
    }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) {
        return RigDualWieldGroup.OBSIDIAN_SWORD;
    }

    @Override
    public RigCombatStyle getDualRigCombatStyle(ItemStack self, ItemStack other) {
        return RigCombatStyle.DUAL_OBSIDIAN_SWORD;
    }
}
