package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.entity.BlockProjectileEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModBlocks;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class HerobrineEnderEyeItem extends Item {
    public static final int VANILLA_PILLAR_COOLDOWN_TICKS = 20 * 10;
    public static final int VANILLA_MACHINE_GUN_COOLDOWN_TICKS = 20 * 60;

    public HerobrineEnderEyeItem() {
        super((new Properties()).stacksTo(1).durability(300));
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.translatable("tooltip.annoyingvillagers.herobrine_ender_eye"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel serverLevel) {
            startShadowObsidianMachineGun(serverLevel, player);
            VanillaWeaponAbilityUtil.swingMainHand(player);
            VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 10);
            player.getCooldowns().addCooldown(this, VANILLA_MACHINE_GUN_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean activateVanillaPressedSpecial(Player player) {
        return false;
    }

    public static boolean activateVanillaHeldSpecial(Player player) {
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) return false;
        ItemStack stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof HerobrineEnderEyeItem item) || player.getCooldowns().isOnCooldown(item)) return false;
        spawnAndShootDarkObPillars(serverLevel, player, 10);
        VanillaWeaponAbilityUtil.swingOffHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.OFF_HAND, 5);
        player.getCooldowns().addCooldown(item, VANILLA_PILLAR_COOLDOWN_TICKS);
        return true;
    }

    public static void spawnAndShootDarkObPillars(ServerLevel level, LivingEntity shooter, int delayTicks) {
        BlockState block = Objects.requireNonNull(AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_SHORT_PILLAR.get()).defaultBlockState();

        final double lateral = 2.0;
        final double sideY   = 0.0;
        final double upY     = 2.0;

        Vec3 eye = shooter.getEyePosition(1.0F);
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);

        if (horiz.lengthSqr() < 1.0e-6) {
            float yaw = shooter.getYRot() * ((float)Math.PI / 180F);
            horiz = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        }
        Vec3 upAxis = new Vec3(0, 1, 0);
        Vec3 rightAxis = horiz.cross(upAxis).normalize();
        Vec3 leftAxis  = rightAxis.scale(-1);

        Vec3 upPos = eye.add(0.0, upY, 0.0);
        Vec3 leftPos = eye.add(leftAxis.scale(lateral)).add(0.0, sideY, 0.0);
        Vec3 rightPos = eye.add(rightAxis.scale(lateral)).add(0.0, sideY, 0.0);

        BlockProjectileEntity up = makeFloating(level, shooter, block, upPos.x, upPos.y, upPos.z);
        BlockProjectileEntity left = makeFloating(level, shooter, block, leftPos.x, leftPos.y, leftPos.z);
        BlockProjectileEntity right = makeFloating(level, shooter, block, rightPos.x, rightPos.y, rightPos.z);

        new DelayedTask(delayTicks) {
            @Override public void run() {
                if (!shooter.isAlive()) {
                    if (up != null && up.isAlive()) up.discard();
                    if (left != null && left.isAlive()) left.discard();
                    if (right != null && right.isAlive()) right.discard();
                    return;
                }

                Vec3 to = lookTarget(shooter, 16.0);
                shootOne(up,   to, 2.0F);
                shootOne(left, to, 2.0F);
                shootOne(right,to, 2.0F);
            }
        };
    }

    public static void startShadowObsidianMachineGun(ServerLevel level, Player player) {
        final int[] remaining = {20};
        fireChainTick(level, player, remaining);
    }

    private static void fireChainTick(ServerLevel level, Player player, int[] remaining) {
        if (remaining[0] <= 0 || !player.isAlive()) return;

        BlockState block = Objects.requireNonNull(AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get()).defaultBlockState();
        player.setDeltaMovement(Vec3.ZERO);
        shootChain(player, block, 2.5F, 5);

        remaining[0]--;
        new DelayedTask(1) { @Override public void run() { fireChainTick(level, player, remaining); } };
    }

    private static BlockProjectileEntity makeFloating(Level level, LivingEntity owner, BlockState block, double x, double y, double z) {
        BlockProjectileEntity blockProjectileEntity = new BlockProjectileEntity(level, owner, block);
        blockProjectileEntity.setNoGravity(true);
        blockProjectileEntity.setNotReadyForShoot(true);
        blockProjectileEntity.moveTo(x, y, z, 0.0F, 0.0F);
        if (owner instanceof Player player) blockProjectileEntity.setOwnerUUID(player.getUUID());
        level.addFreshEntity(blockProjectileEntity);
        return blockProjectileEntity;
    }

    private static void shootOne(BlockProjectileEntity blockProjectileEntity, Vec3 to, double speed) {
        if (blockProjectileEntity == null || !blockProjectileEntity.isAlive()) return;
        Vec3 dir = to.subtract(blockProjectileEntity.position());
        if (dir.lengthSqr() < 1.0e-6) dir = blockProjectileEntity.getLookAngle();
        blockProjectileEntity.setNoGravity(false);
        blockProjectileEntity.setDeltaMovement(dir.normalize().scale(speed));
        blockProjectileEntity.setNotReadyForShoot(false);
    }

    private static Vec3 lookTarget(LivingEntity shooter, double distance) {
        return shooter.getEyePosition().add(shooter.getLookAngle().normalize().scale(distance));
    }

    private static void shootChain(LivingEntity shooter, BlockState block, float velocity, int length) {
        Level level = shooter.level();
        if (level.isClientSide) return;

        double eyeY = shooter.getEyeY();
        Vec3 look = shooter.getLookAngle().normalize();
        Random rand = new Random();

        for (int i = 0; i < length; i++) {
            BlockProjectileEntity proj = new BlockProjectileEntity(level, shooter, block);

            Vec3 forward = look.scale(i * 1.0);

            double sideX = (rand.nextDouble() - 0.5) * 2.0;
            double sideY = (rand.nextDouble() - 0.5) * 2.0;
            double sideZ = (rand.nextDouble() - 0.5) * 2.0;

            proj.setPos(
                    shooter.getX() + forward.x + sideX,
                    eyeY + forward.y + sideY,
                    shooter.getZ() + forward.z + sideZ
            );
            if (shooter instanceof Player player) proj.setOwnerUUID(player.getUUID());
            proj.setDeltaMovement(look.scale(velocity));

            level.addFreshEntity(proj);
        }
    }
}
