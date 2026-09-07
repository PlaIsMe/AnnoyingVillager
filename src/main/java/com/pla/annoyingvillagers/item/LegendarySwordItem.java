package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.entity.ShockWaveBlockEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.ArmorUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LegendarySwordItem extends SwordItem implements RigCombatProfileProvider {
    private static final String AWAKENED_TAG = "LegendarySwordAwakened";
    private static final String AWAKEN_UNTIL_TAG = "LegendarySwordAwakenUntil";
    private static final int ACTIVE_DURATION_TICKS = 20 * 30;
    private static final int RECOVERY_COOLDOWN_TICKS = 20 * 60 * 2;
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("db78a10d-0191-4728-8a2c-2cb8efe69dfa");

    public LegendarySwordItem() {
        super(new Tier() {
            public int getUses() { return 1561; }
            public float getSpeed() { return 4.0F; }
            public float getAttackDamageBonus() { return 6.0F; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 2; }
            public @NotNull Ingredient getRepairIngredient() { return Ingredient.of(new ItemStack(AnnoyingVillagersModItems.COMPRESSED_DIAMOND.get())); }
        }, 3, -2.32F, new Properties().fireResistant());
    }

    public static boolean isAwakened(ItemStack stack, Level level) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(AWAKENED_TAG) && level.getGameTime() < stack.getTag().getLong(AWAKEN_UNTIL_TAG);
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof LegendarySwordItem item) || player.getCooldowns().isOnCooldown(item)) return false;
        stack.getOrCreateTag().putBoolean(AWAKENED_TAG, true);
        stack.getOrCreateTag().putLong(AWAKEN_UNTIL_TAG, player.level().getGameTime() + ACTIVE_DURATION_TICKS);
        stack.getOrCreateTag().putInt("CustomModelData", 1);
        VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
        applyAttackSpeed(player);
        refreshBuffs(player);
        player.getCooldowns().addCooldown(item, ACTIVE_DURATION_TICKS);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_TWO_HANDED_SLAM_HEAVY);
            VanillaWeaponAbilityUtil.performVanillaMeleeHit(player, 5.0D);
            BlockPos center = player.blockPosition();
            for (int radius = 1; radius <= 6; radius++) {
                int ringRadius = radius;
                new DelayedTask((radius - 1) * 2) { @Override public void run() { if (player.isAlive() && !player.isRemoved()) spawnCircleRing(serverLevel, center, ringRadius, player); } };
            }
            player.getCooldowns().addCooldown(this, ACTIVE_DURATION_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || level.isClientSide() || !(entity instanceof Player player) || !stack.hasTag() || stack.getTag() == null || !stack.getTag().getBoolean(AWAKENED_TAG)) return;
        if (isAwakened(stack, level)) {
            if (player.tickCount % 10 == 0) refreshBuffs(player);
            applyAttackSpeed(player);
            return;
        }
        clearAwakening(stack, player);
        player.getCooldowns().addCooldown(this, RECOVERY_COOLDOWN_TICKS);
    }

    private static void refreshBuffs(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 25, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 2, false, false, true));
    }

    private static void applyAttackSpeed(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;
        attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
        attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MODIFIER_UUID, "Legendary sword awakening attack speed", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void clearAwakening(ItemStack stack, Player player) {
        stack.getTag().remove(AWAKENED_TAG);
        stack.getTag().remove(AWAKEN_UNTIL_TAG);
        stack.getTag().remove("CustomModelData");
        clearVanillaAttackSpeed(player);
    }

    public static boolean hasActiveVanillaAwakening(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof LegendarySwordItem && isAwakened(stack, player.level())) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof LegendarySwordItem && isAwakened(stack, player.level())) return true;
        }
        return false;
    }

    public static void clearVanillaAttackSpeed(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (!attacker.level().isClientSide()) ArmorUtil.damageArmor(target, new Random().nextInt(1, 5));
        return super.hurtEnemy(stack, target, attacker);
    }

    public void appendHoverText(@NotNull ItemStack itemStack, Level level, @NotNull List<Component> componentList, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, componentList, tooltipFlag);
        componentList.add(Component.translatable("tooltip.annoyingvillagers.legendary_sword"));
    }

    public static void spawnCircleRing(ServerLevel level, BlockPos centerPos, int radius, LivingEntity owner) {
        double inner = (radius - 0.5D) * (radius - 0.5D);
        double outer = (radius + 0.5D) * (radius + 0.5D);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist2 = (double)dx * dx + (double)dz * dz;
                if (dist2 >= inner && dist2 <= outer) spawnShockWaveBlock(level, centerPos.offset(dx, 0, dz), owner);
            }
        }
    }

    private static void spawnShockWaveBlock(ServerLevel level, BlockPos startPos, LivingEntity owner) {
        final int BLOCK_SEARCH_DEPTH = 256;
        final int ENTITY_GROUND_LIFETIME = 10;
        BlockPos pos = startPos;
        BlockState state = level.getBlockState(pos);
        int minY = level.getMinBuildHeight();
        for (int i = 0; i < BLOCK_SEARCH_DEPTH && pos.getY() > minY && state.getRenderShape() != RenderShape.MODEL; i++) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (state.getRenderShape() != RenderShape.MODEL) return;
        ShockWaveBlockEntity blockEntity = new ShockWaveBlockEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, state, ENTITY_GROUND_LIFETIME);
        blockEntity.setOwnerUuid(owner.getUUID());
        level.addFreshEntity(blockEntity);
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) { return RigCombatStyle.LEGENDARY_SWORD; }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) { return RigDualWieldGroup.LEGENDARY_SWORD; }

    @Override
    public RigCombatStyle getDualRigCombatStyle(ItemStack self, ItemStack other) {
        if (other.getItem() instanceof WoopieTheSwordItem) return RigCombatStyle.LEGENDARY_SWORD_WOOPIE;
        if (other.getItem() instanceof BlueDemonTridentItem) return RigCombatStyle.BLUE_DEMON_LEGENDARY_SWORD;
        return RigCombatStyle.LEGENDARY_SWORD;
    }
}
