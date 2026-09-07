package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.clazz.NullWeapon;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NullWeaponItem extends SwordItem implements RigCombatProfileProvider {
    private static final String CHARGE_TAG = "AVNullWeaponCharge";
    private static final String RELEASE_UNTIL_TAG = "AVNullWeaponReleaseUntil";
    private static final String RECOVERY_UNTIL_TAG = "AVNullWeaponRecoveryUntil";
    private static final String LOGIN_RECONCILE_UNTIL_TAG = "AVNullWeaponLoginReconcileUntil";
    private static final int LOGIN_RECONCILE_TICKS = 20 * 5;
    private static final int RELEASE_DURATION_TICKS = 20 * 30;
    private static final int RECOVERY_COOLDOWN_TICKS = 20 * 60;
    private static final String[] OWNED_KEYS = {"NullPickaxeUUID", "NullHoeUUID", "NullAxeUUID", "NullShovelUUID", "NullSwordUUID"};

    public NullWeaponItem() {
        super(new Tier() {
            public int getUses() { return 1561; }
            public float getSpeed() { return 4.0F; }
            public float getAttackDamageBonus() { return 3.0F; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 4; }
            public @NotNull Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        }, 3, -3.0F, new Properties());
    }

    public static int getCharge(ItemStack stack) {
        return VanillaWeaponAbilityUtil.getCharge(stack, CHARGE_TAG, 100);
    }

    public static void setCharge(ItemStack stack, int charge) {
        VanillaWeaponAbilityUtil.setCharge(stack, CHARGE_TAG, charge, 100);
    }

    public static void beginLoginReconciliation(Player player) {
        if (!player.level().isClientSide()) {
            player.getPersistentData().putLong(LOGIN_RECONCILE_UNTIL_TAG, player.level().getGameTime() + LOGIN_RECONCILE_TICKS);
        }
    }

    public static void onOwnedWeaponHit(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return;
        ItemStack stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof NullWeaponItem)) return;
        VanillaWeaponAbilityUtil.addCharge(stack, CHARGE_TAG, 1, 100);
        if (player.level() instanceof ServerLevel serverLevel) syncOwnedWeapons(serverLevel, player, stack);
    }

    public static void onPlayerMeleeHit(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return;
        ItemStack stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof NullWeaponItem item) || player.getCooldowns().isOnCooldown(item)) return;
        VanillaWeaponAbilityUtil.addCharge(stack, CHARGE_TAG, 1, 100);
        if (player.level() instanceof ServerLevel serverLevel) syncOwnedWeapons(serverLevel, player, stack);
    }

    public static boolean activateHeldSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) return false;
        ItemStack stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof NullWeaponItem item) || player.getCooldowns().isOnCooldown(item) || getCharge(stack) < 20) return false;
        syncOwnedWeapons(serverLevel, player, stack);
        List<NullWeapon> weapons = getOwnedWeapons(serverLevel, player);
        if (weapons.isEmpty()) return false;
        LivingEntity target = VanillaWeaponAbilityUtil.findCommandTarget(player, 24.0D);
        for (NullWeapon weapon : weapons) weapon.releaseForTicks(target, RELEASE_DURATION_TICKS);
        stack.getOrCreateTag().putLong(RELEASE_UNTIL_TAG, player.level().getGameTime() + RELEASE_DURATION_TICKS);
        stack.getOrCreateTag().remove(RECOVERY_UNTIL_TAG);
        VanillaWeaponAbilityUtil.swingOffHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.OFF_HAND, 1);
        player.getCooldowns().addCooldown(item, RELEASE_DURATION_TICKS);
        new DelayedTask(RELEASE_DURATION_TICKS) {
            @Override public void run() {
                if (!player.isAlive() || player.isRemoved()) return;
                if (player.level() instanceof ServerLevel level) {
                    for (NullWeapon weapon : getOwnedWeapons(level, player)) weapon.stopRelease();
                    setCharge(stack, 0);
                    stack.getOrCreateTag().remove(RELEASE_UNTIL_TAG);
                    stack.getOrCreateTag().putLong(RECOVERY_UNTIL_TAG, player.level().getGameTime() + RECOVERY_COOLDOWN_TICKS);
                    if (player.getOffhandItem() == stack) syncOwnedWeapons(level, player, stack);
                    else discardOwnedWeapons(level, player);
                }
                player.getCooldowns().addCooldown(item, RECOVERY_COOLDOWN_TICKS);
            }
        };
        return true;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()) {
            setCharge(stack, 0);
            discardOwnedWeapons(serverLevel, player);
            return;
        }
        long now = level.getGameTime();
        long releaseUntil = stack.getOrCreateTag().getLong(RELEASE_UNTIL_TAG);
        long recoveryUntil = stack.getOrCreateTag().getLong(RECOVERY_UNTIL_TAG);
        if (releaseUntil > 0L && now >= releaseUntil) {
            stack.getOrCreateTag().remove(RELEASE_UNTIL_TAG);
            setCharge(stack, 0);
            for (NullWeapon weapon : getOwnedWeapons(serverLevel, player)) weapon.stopRelease();
            if (recoveryUntil <= now) {
                recoveryUntil = now + RECOVERY_COOLDOWN_TICKS;
                stack.getOrCreateTag().putLong(RECOVERY_UNTIL_TAG, recoveryUntil);
            }
        }
        long cooldownUntil = releaseUntil > now ? releaseUntil : recoveryUntil;
        long remaining = cooldownUntil - now;
        if (remaining > 0L && player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F) <= 0.0F) {
            player.getCooldowns().addCooldown(stack.getItem(), (int)Math.min(Integer.MAX_VALUE, remaining));
        } else if (recoveryUntil > 0L && recoveryUntil <= now) {
            stack.getOrCreateTag().remove(RECOVERY_UNTIL_TAG);
        }
        if (player.getOffhandItem() != stack) {
            setCharge(stack, 0);
            discardOwnedWeapons(serverLevel, player);
            return;
        }
        if (player.getOffhandItem() == stack) syncOwnedWeapons(serverLevel, player, stack);
    }

    private static void syncOwnedWeapons(ServerLevel level, Player player, ItemStack stack) {
        int charge = getCharge(stack);
        ensureOwnedWeapon(level, player, "NullPickaxeUUID", AnnoyingVillagersModEntities.NULL_PICKAXE.get());
        if (charge >= 40) ensureOwnedWeapon(level, player, "NullHoeUUID", AnnoyingVillagersModEntities.NULL_HOE.get());
        if (charge >= 60) ensureOwnedWeapon(level, player, "NullAxeUUID", AnnoyingVillagersModEntities.NULL_AXE.get());
        if (charge >= 80) ensureOwnedWeapon(level, player, "NullShovelUUID", AnnoyingVillagersModEntities.NULL_SHOVEL.get());
        if (charge >= 100) ensureOwnedWeapon(level, player, "NullSwordUUID", AnnoyingVillagersModEntities.NULL_SWORD.get());
        discardOwnedWeaponAboveCharge(level, player, "NullHoeUUID", charge, 40);
        discardOwnedWeaponAboveCharge(level, player, "NullAxeUUID", charge, 60);
        discardOwnedWeaponAboveCharge(level, player, "NullShovelUUID", charge, 80);
        discardOwnedWeaponAboveCharge(level, player, "NullSwordUUID", charge, 100);
    }

    private static void discardOwnedWeaponAboveCharge(ServerLevel level, Player player, String key, int charge, int requiredCharge) {
        if (charge >= requiredCharge) return;
        NullWeapon weapon = getOwnedWeapon(level, player, key);
        if (weapon != null) weapon.discard();
        player.getPersistentData().remove(key);
    }

    private static void ensureOwnedWeapon(ServerLevel level, Player player, String key, EntityType<? extends NullWeapon> type) {
        NullWeapon existing = getOwnedWeapon(level, player, key);
        if (existing != null && existing.isAlive() && !existing.isRemoved()) return;
        // During login the player inventory may tick before nearby saved entities are
        // inserted into ServerLevel's UUID index. Preserve their UUID briefly instead
        // of spawning a replacement in that load-order window.
        if (player.getPersistentData().hasUUID(key)
                && level.getGameTime() < player.getPersistentData().getLong(LOGIN_RECONCILE_UNTIL_TAG)) return;
        NullWeapon weapon = type.create(level);
        if (weapon == null) return;
        weapon.setPlayer(player);
        weapon.setPlayerUUID(player.getUUID());
        weapon.moveTo(player.getX(), player.getY() + 1.5D, player.getZ(), player.getYRot(), 0.0F);
        level.addFreshEntity(weapon);
        player.getPersistentData().putUUID(key, weapon.getUUID());
    }

    @Nullable
    private static NullWeapon getOwnedWeapon(ServerLevel level, Player player, String key) {
        if (!player.getPersistentData().hasUUID(key)) return null;
        Entity entity = level.getEntity(player.getPersistentData().getUUID(key));
        if (entity instanceof NullWeapon weapon && player.getUUID().equals(weapon.getPlayerUUID())) return weapon;
        if (entity == null && level.getGameTime() < player.getPersistentData().getLong(LOGIN_RECONCILE_UNTIL_TAG)) return null;
        player.getPersistentData().remove(key);
        return null;
    }

    private static List<NullWeapon> getOwnedWeapons(ServerLevel level, Player player) {
        List<NullWeapon> result = new ArrayList<>();
        for (String key : OWNED_KEYS) {
            NullWeapon weapon = getOwnedWeapon(level, player, key);
            if (weapon != null) result.add(weapon);
        }
        return result;
    }

    public static void discardOwnedWeapons(ServerLevel level, Player player) {
        for (String key : OWNED_KEYS) {
            NullWeapon weapon = getOwnedWeapon(level, player, key);
            if (weapon != null) weapon.discard();
            player.getPersistentData().remove(key);
        }
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) { return RigCombatStyle.NULL_HEROBRINE; }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) { return true; }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.translatable("tooltip.annoyingvillagers.null_weapon_full"));
    }
}
