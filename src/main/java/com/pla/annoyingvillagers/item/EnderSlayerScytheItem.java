package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.entity.HerobrineDragonEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EnderSlayerScytheItem extends SwordItem implements RigCombatProfileProvider {
    public static final String DRAGON_UUID_TAG = "DragonUUID";
    private static final String ACTIVE_SCYTHE_TAG = "AVEnderSlayerActiveScythe";
    private static final String DRAGON_EXPIRES_TAG = "AVEnderSlayerDragonExpires";
    private static final String RECOVERY_EXPIRES_TAG = "AVEnderSlayerRecoveryExpires";
    public static final String COMMAND_COOLDOWN_TAG = "AVEnderSlayerCommandCooldown";
    public static final int DRAGON_LIFETIME_TICKS = 20 * 60 * 5;
    public static final int POST_DRAGON_COOLDOWN_TICKS = 20 * 60 * 2;
    public static final int COMMAND_COOLDOWN_TICKS = 20 * 10;
    private static final int SUMMON_RISE_DURATION_TICKS = 120;
    private static final double SUMMON_UNDERGROUND_DISTANCE = 5.0D;
    private static final double SUMMON_RISE_DISTANCE = 15.0D;

    public EnderSlayerScytheItem() {
        super(new Tier() {
            public int getUses() { return 1561; }
            public float getSpeed() { return 4.0F; }
            public float getAttackDamageBonus() { return 4.0F; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 2; }
            public @NotNull Ingredient getRepairIngredient() { return Ingredient.of(AnnoyingVillagersModItems.ELITE_OBSIDIAN.get()); }
        }, 3, -2.3F, new Properties().fireResistant());
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EnderSlayerScytheItem item) || !(player.level() instanceof ServerLevel serverLevel)) return false;

        HerobrineDragonEntity dragon = getTrackedDragon(player);
        if (dragon != null) {
            dragon.recallAndLand(true);
            VanillaWeaponAbilityUtil.swingMainHand(player);
            return true;
        }

        long recoveryUntil = Math.max(player.getPersistentData().getLong(RECOVERY_EXPIRES_TAG), stack.getOrCreateTag().getLong(RECOVERY_EXPIRES_TAG));
        if (player.level().getGameTime() < recoveryUntil) return false;
        clearDragonTracking(player);
        player.getPersistentData().remove(RECOVERY_EXPIRES_TAG);
        stack.getOrCreateTag().remove(RECOVERY_EXPIRES_TAG);
        dragon = spawnEnderDragon(player, serverLevel);
        if (dragon == null) return false;

        player.getPersistentData().putUUID(DRAGON_UUID_TAG, dragon.getUUID());
        player.getPersistentData().putLong(DRAGON_EXPIRES_TAG, player.level().getGameTime() + DRAGON_LIFETIME_TICKS);
        stack.getOrCreateTag().putBoolean(ACTIVE_SCYTHE_TAG, true);
        player.getCooldowns().addCooldown(item, DRAGON_LIFETIME_TICKS);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        VanillaWeaponAbilityUtil.swingMainHand(player);
        return true;
    }

    public static boolean commandMeteor(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        if (!(player.getMainHandItem().getItem() instanceof EnderSlayerScytheItem)) return false;
        HerobrineDragonEntity dragon = getTrackedDragon(player);
        if (dragon == null || !VanillaWeaponAbilityUtil.isInternalCooldownReady(player, COMMAND_COOLDOWN_TAG)) return dragon != null;
        LivingEntity target = findDragonCommandTarget(player, dragon, 48.0D);
        if (target == null) return true;
        dragon.shootMeteoriteAtTarget(target);
        VanillaWeaponAbilityUtil.swingOffHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        VanillaWeaponAbilityUtil.setInternalCooldown(player, COMMAND_COOLDOWN_TAG, COMMAND_COOLDOWN_TICKS);
        return true;
    }

    public static boolean commandThunder(Player player, @Nullable LivingEntity requestedTarget) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        if (!(player.getMainHandItem().getItem() instanceof EnderSlayerScytheItem)) return false;
        HerobrineDragonEntity dragon = getTrackedDragon(player);
        if (dragon == null) return false;
        if (!VanillaWeaponAbilityUtil.isInternalCooldownReady(player, COMMAND_COOLDOWN_TAG)) return true;
        LivingEntity target = isValidDragonCommandTarget(player, dragon, requestedTarget) ? requestedTarget : findDragonCommandTarget(player, dragon, 48.0D);
        if (target == null) return true;
        dragon.shootThunderBreathAtTarget(target);
        VanillaWeaponAbilityUtil.swingOffHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        VanillaWeaponAbilityUtil.setInternalCooldown(player, COMMAND_COOLDOWN_TAG, COMMAND_COOLDOWN_TICKS);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && commandMeteor(player)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof Player player) || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        long recoveryUntil = Math.max(tag.getLong(RECOVERY_EXPIRES_TAG), player.getPersistentData().getLong(RECOVERY_EXPIRES_TAG));
        if (recoveryUntil > level.getGameTime()) {
            tag.putLong(RECOVERY_EXPIRES_TAG, recoveryUntil);
            player.getPersistentData().putLong(RECOVERY_EXPIRES_TAG, recoveryUntil);
            int remainingTicks = (int)Math.min(Integer.MAX_VALUE, recoveryUntil - level.getGameTime());
            if (player.getCooldowns().getCooldownPercent(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get(), 0.0F) <= 0.0F) {
                player.getCooldowns().addCooldown(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get(), remainingTicks);
            }
        } else if (recoveryUntil > 0L) {
            tag.remove(RECOVERY_EXPIRES_TAG);
            player.getPersistentData().remove(RECOVERY_EXPIRES_TAG);
        }
        if (!tag.getBoolean(ACTIVE_SCYTHE_TAG)) return;

        HerobrineDragonEntity dragon = getTrackedDragon(player);
        boolean switchedAway = player.getMainHandItem() != stack;
        boolean expired = player.getPersistentData().getLong(DRAGON_EXPIRES_TAG) > 0L && level.getGameTime() >= player.getPersistentData().getLong(DRAGON_EXPIRES_TAG);
        if (switchedAway || expired || dragon == null || dragon.isRemoved()) {
            discardTrackedDragon(player, true);
            tag.remove(ACTIVE_SCYTHE_TAG);
            tag.putLong(RECOVERY_EXPIRES_TAG, player.getPersistentData().getLong(RECOVERY_EXPIRES_TAG));
            return;
        }

        long expiresAt = player.getPersistentData().getLong(DRAGON_EXPIRES_TAG);
        int remainingTicks = (int)Math.max(0L, expiresAt - level.getGameTime());
        if (remainingTicks > 0 && player.getCooldowns().getCooldownPercent(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get(), 0.0F) <= 0.0F) player.getCooldowns().addCooldown(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get(), remainingTicks);
        if (selected) HerobrineUtil.spawnEliteEffect(level, entity.getX(), entity.getY(), entity.getZ(), entity);
    }

    @Nullable
    public static HerobrineDragonEntity getTrackedDragon(Player player) {
        if (!player.getPersistentData().hasUUID(DRAGON_UUID_TAG)) return null;
        UUID dragonId = player.getPersistentData().getUUID(DRAGON_UUID_TAG);
        Entity entity = findTrackedEntity(player, dragonId);
        return entity instanceof HerobrineDragonEntity dragon && dragon.isAlive() && !dragon.isRemoved() ? dragon : null;
    }

    public static void discardTrackedDragon(Player player, boolean addRecoveryCooldown) {
        HerobrineDragonEntity dragon = getTrackedDragon(player);
        if (dragon != null) {
            dragon.ejectPassengers();
            dragon.discard();
        }
        clearDragonTracking(player);
        if (addRecoveryCooldown) {
            startRecoveryCooldown(player);
        }
    }

    public static void onSummonedDragonDeath(Player player, UUID dragonUuid) {
        if (player.level().isClientSide()
                || !player.getPersistentData().hasUUID(DRAGON_UUID_TAG)
                || !dragonUuid.equals(player.getPersistentData().getUUID(DRAGON_UUID_TAG))) return;

        long recoveryUntil = player.level().getGameTime() + POST_DRAGON_COOLDOWN_TICKS;
        for (ItemStack stack : player.getInventory().items) {
            markScytheRecovering(stack, recoveryUntil);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            markScytheRecovering(stack, recoveryUntil);
        }

        clearDragonTracking(player);
        startRecoveryCooldown(player, recoveryUntil);
    }

    private static void markScytheRecovering(ItemStack stack, long recoveryUntil) {
        if (!(stack.getItem() instanceof EnderSlayerScytheItem) || !isDragonActive(stack)) return;
        stack.getOrCreateTag().remove(ACTIVE_SCYTHE_TAG);
        stack.getOrCreateTag().putLong(RECOVERY_EXPIRES_TAG, recoveryUntil);
    }

    private static void startRecoveryCooldown(Player player) {
        startRecoveryCooldown(player, player.level().getGameTime() + POST_DRAGON_COOLDOWN_TICKS);
    }

    private static void startRecoveryCooldown(Player player, long recoveryUntil) {
        player.getPersistentData().putLong(RECOVERY_EXPIRES_TAG, recoveryUntil);
        player.getCooldowns().removeCooldown(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get());
        player.getCooldowns().addCooldown(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get(), POST_DRAGON_COOLDOWN_TICKS);
    }

    private static void clearDragonTracking(Player player) {
        player.getPersistentData().remove(DRAGON_UUID_TAG);
        player.getPersistentData().remove(DRAGON_EXPIRES_TAG);
        VanillaWeaponAbilityUtil.clearInternalCooldown(player, COMMAND_COOLDOWN_TAG);
    }

    public static boolean isDragonActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(ACTIVE_SCYTHE_TAG);
    }

    private static boolean isValidDragonCommandTarget(Player player, HerobrineDragonEntity dragon, @Nullable LivingEntity target) {
        return target != null && target != dragon && !(target instanceof HerobrineDragonEntity) && VanillaWeaponAbilityUtil.isValidTarget(player, target);
    }

    @Nullable
    private static LivingEntity findDragonCommandTarget(Player player, HerobrineDragonEntity dragon, double range) {
        LivingEntity target = VanillaWeaponAbilityUtil.findLookTarget(player, range);
        if (isValidDragonCommandTarget(player, dragon, target)) return target;
        target = player.getLastHurtMob();
        if (isValidDragonCommandTarget(player, dragon, target) && player.distanceToSqr(target) <= range * range) return target;
        target = player.getLastHurtByMob();
        if (isValidDragonCommandTarget(player, dragon, target) && player.distanceToSqr(target) <= range * range) return target;
        LivingEntity nearest = null;
        double nearestDistanceSqr = range * range;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), entity -> isValidDragonCommandTarget(player, dragon, entity))) {
            double distanceSqr = player.distanceToSqr(candidate);
            if (distanceSqr < nearestDistanceSqr) {
                nearest = candidate;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest;
    }

    @Nullable
    private static Entity findTrackedEntity(Player player, UUID dragonId) {
        if (player.level() instanceof ServerLevel currentLevel) {
            Entity entity = currentLevel.getEntity(dragonId);
            if (entity != null) return entity;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return null;
        for (ServerLevel serverLevel : server.getAllLevels()) {
            Entity entity = serverLevel.getEntity(dragonId);
            if (entity != null) return entity;
        }
        return null;
    }

    @Nullable
    private static HerobrineDragonEntity spawnEnderDragon(Player player, ServerLevel serverLevel) {
        if (!player.isAlive()) return null;
        HerobrineDragonEntity dragon = new HerobrineDragonEntity(AnnoyingVillagersModEntities.HEROBRINE_DRAGON.get(), serverLevel);
        Vec3 spawnPos = findSummonSpawnPos(serverLevel, player);
        dragon.setPos(spawnPos);
        dragon.setYRot(player.getYRot());
        dragon.setYHeadRot(player.getYRot());
        dragon.setYBodyRot(player.getYRot());
        dragon.setXRot(-85.0F);
        dragon.setSummoner(player);
        dragon.setSummonerUUID(player.getUUID());
        dragon.startSummonRise(findSummonRiseTarget(serverLevel, player, dragon), SUMMON_RISE_DURATION_TICKS, player.getY());
        serverLevel.addFreshEntity(dragon);
        return dragon;
    }

    private static Vec3 findSummonSpawnPos(ServerLevel serverLevel, Player player) {
        double y = Mth.clamp(player.getY() - SUMMON_UNDERGROUND_DISTANCE, serverLevel.getMinBuildHeight() + 2.0D, serverLevel.getMaxBuildHeight() - 8.0D);
        return new Vec3(player.getX(), y, player.getZ());
    }

    private static Vec3 findSummonRiseTarget(ServerLevel serverLevel, Player player, HerobrineDragonEntity dragon) {
        double minY = serverLevel.getMinBuildHeight() + 6.0D;
        double maxY = serverLevel.getMaxBuildHeight() - 6.0D;
        if (serverLevel.dimensionType().hasCeiling()) {
            BlockPos col = BlockPos.containing(player.getX(), 0.0D, player.getZ());
            int roofAirY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col).getY();
            maxY = Math.min(maxY, roofAirY - dragon.getBbHeight() - 2.0D);
        }
        if (maxY < minY) maxY = minY;
        double desiredY = Mth.clamp(player.getY() + SUMMON_RISE_DISTANCE, minY, maxY);
        int start = Mth.floor(desiredY);
        int end = Mth.floor(Math.max(minY, player.getY() + 8.0D));
        for (int y = start; y >= end; y--) {
            if (canDragonFitAt(serverLevel, dragon, player.getX(), y, player.getZ())) return new Vec3(player.getX(), y, player.getZ());
        }
        return new Vec3(player.getX(), desiredY, player.getZ());
    }

    private static boolean canDragonFitAt(ServerLevel serverLevel, HerobrineDragonEntity dragon, double x, double y, double z) {
        AABB movedBox = dragon.getBoundingBox().move(x - dragon.getX(), y - dragon.getY(), z - dragon.getZ());
        return serverLevel.noCollision(dragon, movedBox) && !serverLevel.containsAnyLiquid(movedBox);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.translatable("tooltip.annoyingvillagers.ender_slayer_scythe"));
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.REAPER_HEROBRINE;
    }
}
