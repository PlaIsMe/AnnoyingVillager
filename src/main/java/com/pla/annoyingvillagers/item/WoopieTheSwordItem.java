package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.network.ClientboundMuteExplosionAtPos;
import com.pla.annoyingvillagers.network.ClientboundWoopieSwordWindFx;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigDualWieldGroup;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WoopieTheSwordItem extends SwordItem implements RigCombatProfileProvider {
    public static final int VANILLA_ULT_COOLDOWN_TICKS = 20 * 30;
    public static final int VANILLA_EXTRA_ULT_COOLDOWN_TICKS = 20 * 15;
    private static final int VANILLA_ULT_HIT_TICK = 10;
    private static final int VANILLA_ULT_WIND_TICK = 15;
    private static final double VANILLA_MELEE_RANGE = 5.0D;
    private static final double VANILLA_RUSH_SPEED = 2.2D;

    public WoopieTheSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 1850;
            }

            public float getSpeed() {
                return 8.0F;
            }

            public float getAttackDamageBonus() {
                return 3.5F;
            }

            public int getLevel() {
                return 3;
            }

            public int getEnchantmentValue() {
                return 10;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.DIAMOND));
            }
        }, 3, -2.8F, (new Properties()).fireResistant());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return super.use(level, player, hand);
        if (level instanceof ServerLevel serverLevel) {
            VanillaWeaponAbilityUtil.swingMainHand(player);
            VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
            player.getCooldowns().addCooldown(this, VANILLA_ULT_COOLDOWN_TICKS);
            new DelayedTask(VANILLA_ULT_HIT_TICK) { @Override public void run() { if (canContinueVanillaAbility(player, serverLevel, stack)) damageVanillaUltTarget(serverLevel, player); } };
            new DelayedTask(VANILLA_ULT_WIND_TICK) { @Override public void run() { if (canContinueVanillaAbility(player, serverLevel, stack)) spawnVanillaWindBurst(serverLevel, player, getForwardWindPosition(player)); } };
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WoopieTheSwordItem item) || player.getCooldowns().isOnCooldown(item)) return false;

        VanillaWeaponAbilityUtil.swingMainHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        player.getCooldowns().addCooldown(item, VANILLA_EXTRA_ULT_COOLDOWN_TICKS);
        spawnVanillaWindBurst(serverLevel, player, CommonUtil.getVanillaSwordOrBodyPosition(player));

        LivingEntity lookTarget = VanillaWeaponAbilityUtil.findLookTarget(player, 12.0D);
        Vec3 dashDirection = lookTarget != null ? lookTarget.position().subtract(player.position()) : player.getLookAngle();
        dashDirection = new Vec3(dashDirection.x, 0.0D, dashDirection.z);
        if (dashDirection.lengthSqr() < 1.0E-6D) dashDirection = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 dash = dashDirection.normalize().scale(VANILLA_RUSH_SPEED);
        Set<UUID> hitTargets = new HashSet<>();

        for (int tick = 1; tick <= 6; tick++) {
            new DelayedTask(tick) { @Override public void run() { if (canContinueVanillaAbility(player, serverLevel, stack)) applyRushMotion(player, dash); } };
        }
        for (int tick = 1; tick <= 8; tick++) {
            new DelayedTask(tick) { @Override public void run() { if (canContinueVanillaAbility(player, serverLevel, stack)) damageRushTargets(serverLevel, player, hitTargets); } };
        }
        return true;
    }

    private static boolean canContinueVanillaAbility(Player player, ServerLevel serverLevel, ItemStack stack) {
        return VanillaWeaponAbilityUtil.abilitiesEnabled() && player.isAlive() && !player.isRemoved() && player.level() == serverLevel && !stack.isEmpty();
    }

    private static Vec3 getForwardWindPosition(Player player) {
        Vec3 swordPos = CommonUtil.getVanillaSwordOrBodyPosition(player);
        Vec3 look = player.getLookAngle();
        return swordPos.add(look.x * 3.5D, look.y * 1.2D, look.z * 3.5D);
    }

    private static void spawnVanillaWindBurst(ServerLevel serverLevel, Player player, Vec3 windPos) {
        if (windPos == null) windPos = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
        AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new ClientboundMuteExplosionAtPos(BlockPos.containing(windPos), 4));
        serverLevel.explode(player, windPos.x, windPos.y, windPos.z, 2.0F, false, Level.ExplosionInteraction.NONE);
        AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new ClientboundWoopieSwordWindFx(windPos));
    }

    private static void applyRushMotion(Player player, Vec3 dash) {
        Vec3 currentMotion = player.getDeltaMovement();
        player.setDeltaMovement(dash.x, currentMotion.y, dash.z);
        player.hasImpulse = true;
        player.hurtMarked = true;
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void damageVanillaUltTarget(ServerLevel serverLevel, Player player) {
        LivingEntity target = VanillaWeaponAbilityUtil.findLookTarget(player, VANILLA_MELEE_RANGE);
        if (target == null) return;
        float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.1F;
        target.hurt(serverLevel.damageSources().playerAttack(player), damage);
    }

    private static void damageRushTargets(ServerLevel serverLevel, Player player, Set<UUID> hitTargets) {
        AABB hitBox = player.getBoundingBox().inflate(1.35D, 0.75D, 1.35D);
        float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, candidate -> VanillaWeaponAbilityUtil.isValidTarget(player, candidate))) {
            if (!hitTargets.add(target.getUUID())) continue;
            target.hurt(serverLevel.damageSources().playerAttack(player), damage);
        }
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.WOOPIE_THE_SWORD;
    }

    @Override
    public RigDualWieldGroup getDualWieldGroup(ItemStack stack) {
        return RigDualWieldGroup.LEGENDARY_SWORD;
    }
}
