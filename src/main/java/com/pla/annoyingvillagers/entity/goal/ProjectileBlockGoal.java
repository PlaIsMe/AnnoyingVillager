package com.pla.annoyingvillagers.entity.goal;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ProjectileBlockGoal extends Goal {
    private static final double MAX_PLACE_BLOCK_GROUND_GAP = 2.0D;
    private static final double PROJECTILE_SEARCH_RADIUS = 12.0D;
    private static final double PROJECTILE_PREDICTION_TICKS = 8.0D;
    private static final double TARGET_BOX_INFLATE = 0.45D;
    private static final int MAX_PARRY_TICKS = 10;

    private final AVNpc avNpc;
    private Projectile projectile;
    private UUID lastConsideredProjectile;
    private int parryTicks;

    public ProjectileBlockGoal(AVNpc avNpc) {
        this.avNpc = avNpc;
    }

    @Override
    public boolean canUse() {
        if (this.avNpc.isRecoveryActionActive()) return false;
        if (!(this.avNpc.level() instanceof ServerLevel)
                || !this.avNpc.isAlive()
                || this.avNpc.isRemoved()
                || this.avNpc.isDeadOrDying()
                || this.avNpc.isNoAi()
                || RigStunController.isStunned(this.avNpc)
                || RigAnimationController.isInvulnerable(this.avNpc)
                || !this.avNpc.onGround()
                || !CommonUtil.isGroundWithin(this.avNpc, MAX_PLACE_BLOCK_GROUND_GAP)
                || this.avNpc.isPassenger()
                || this.avNpc.getBlockDamage() != null
                || this.avNpc.hasPlaceBlockParryCooldown()
                || !InventoryUtils.hasPlaceableBlock(this.avNpc)
                || this.avNpc.isUsingItem() && this.avNpc.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            return false;
        }

        Projectile incomingProjectile = this.findIncomingProjectile();
        if (incomingProjectile == null) {
            this.lastConsideredProjectile = null;
            return false;
        }

        if (incomingProjectile.getUUID().equals(this.lastConsideredProjectile)) {
            return false;
        }

        this.lastConsideredProjectile = incomingProjectile.getUUID();
        if (this.avNpc.getRandom().nextDouble() > this.avNpc.getPlaceBlockToParryChance()) {
            return false;
        }

        this.projectile = incomingProjectile;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.avNpc.isRecoveryActionActive()) return false;
        return this.parryTicks < MAX_PARRY_TICKS
                && this.projectile != null
                && this.avNpc.isAlive()
                && !this.avNpc.isRemoved()
                && !this.avNpc.isDeadOrDying()
                && this.avNpc.getBlockDamage() == this.projectile;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.parryTicks = 0;

        if (!(this.avNpc.level() instanceof ServerLevel serverLevel) || this.projectile == null) {
            return;
        }

        ItemStack blockStack = InventoryUtils.peekPlaceableBlock(this.avNpc).orElse(ItemStack.EMPTY);
        if (blockStack.isEmpty()) {
            this.projectile = null;
            return;
        }

        ItemStack previousMainHand = this.avNpc.getMainHandItem().copy();
        boolean placedBlock;
        try {
            this.avNpc.setItemInHand(InteractionHand.MAIN_HAND, blockStack.copy());
            placedBlock = this.placeProjectileWall(serverLevel, this.projectile);
        } finally {
            this.avNpc.setItemInHand(InteractionHand.MAIN_HAND, previousMainHand);
        }

        if (!placedBlock) {
            this.projectile = null;
            return;
        }

        this.avNpc.setBlockDamage(this.projectile);
        this.avNpc.setPlaceBlockParryCooldown();
        this.playPostPlaceAnimation();
    }

    @Override
    public void tick() {
        this.parryTicks++;
        if (this.projectile == null || this.projectile.isRemoved() || !this.projectile.isAlive()) {
            this.parryTicks = MAX_PARRY_TICKS;
        }
    }

    @Override
    public void stop() {
        if (this.avNpc.getBlockDamage() == this.projectile) {
            this.avNpc.setBlockDamage(null);
        }
        this.projectile = null;
        this.parryTicks = 0;
    }

    private void playPostPlaceAnimation() {
        RigAnimationController.play(this.avNpc, this.avNpc.getRandom().nextBoolean()
                ? RigAnimationId.ROLL_BACKWARD
                : RigAnimationId.STEP_BACKWARD);
    }

    private Projectile findIncomingProjectile() {
        AABB targetBox = this.avNpc.getBoundingBox().inflate(TARGET_BOX_INFLATE);
        Projectile closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Projectile candidate : this.avNpc.level().getEntitiesOfClass(
                Projectile.class,
                this.avNpc.getBoundingBox().inflate(PROJECTILE_SEARCH_RADIUS),
                this::isThreateningProjectile
        )) {
            Vec3 velocity = candidate.getDeltaMovement();
            Vec3 start = candidate.position();
            Vec3 targetCenter = targetBox.getCenter();
            Vec3 toTarget = targetCenter.subtract(start);

            if (velocity.lengthSqr() < 1.0E-6D || velocity.dot(toTarget) <= 0.0D) {
                continue;
            }

            Vec3 end = start.add(velocity.scale(PROJECTILE_PREDICTION_TICKS));
            if (targetBox.clip(start, end).isEmpty()) {
                continue;
            }

            double distance = this.avNpc.distanceToSqr(candidate);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    private boolean isThreateningProjectile(Projectile projectile) {
        if (projectile == null || !projectile.isAlive() || projectile.isRemoved()) {
            return false;
        }

        Entity owner = projectile.getOwner();
        if (owner == this.avNpc || owner != null && this.avNpc.isAlliedTo(owner)) {
            return false;
        }

        if (projectile instanceof ThrownPotion thrownPotion) {
            var effects = PotionUtils.getMobEffects(thrownPotion.getItem());
            if (effects.isEmpty() || effects.stream().allMatch(effect -> effect.getEffect().isBeneficial())) {
                return false;
            }
        }

        return true;
    }

    private boolean placeProjectileWall(ServerLevel serverLevel, Projectile projectile) {
        Direction threatDirection = this.getThreatDirection(projectile);
        Direction sideDirection = threatDirection.getClockWise();
        BlockPos baseXZ = this.avNpc.blockPosition().relative(threatDirection);
        int surfaceY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseXZ).getY();
        int topY = Math.max(surfaceY, Mth.floor(this.avNpc.getY() + this.avNpc.getBbHeight()));
        int impactY = Mth.clamp(Mth.floor(projectile.getY()), surfaceY, topY);
        boolean placed = false;

        placed |= this.placeIfReplaceable(serverLevel, new BlockPos(baseXZ.getX(), impactY, baseXZ.getZ()));

        for (int y = surfaceY; y <= topY; y++) {
            if (y == impactY) continue;
            placed |= this.placeIfReplaceable(serverLevel, new BlockPos(baseXZ.getX(), y, baseXZ.getZ()));
        }

        BlockPos impactPos = new BlockPos(baseXZ.getX(), impactY, baseXZ.getZ());
        placed |= this.placeIfReplaceable(serverLevel, impactPos.relative(sideDirection));
        placed |= this.placeIfReplaceable(serverLevel, impactPos.relative(sideDirection.getOpposite()));

        if (impactY > surfaceY) {
            BlockPos lowerImpact = impactPos.below();
            placed |= this.placeIfReplaceable(serverLevel, lowerImpact.relative(sideDirection));
            placed |= this.placeIfReplaceable(serverLevel, lowerImpact.relative(sideDirection.getOpposite()));
        }

        return placed;
    }

    private boolean placeIfReplaceable(ServerLevel serverLevel, BlockPos pos) {
        if (!serverLevel.getBlockState(pos).canBeReplaced()) {
            return false;
        }

        ItemStack blockStack = InventoryUtils.consumePlaceableBlock(this.avNpc).orElse(ItemStack.EMPTY);
        if (blockStack.isEmpty()) {
            return false;
        }

        BlockState placeState = InventoryUtils.getBlockState(blockStack);
        if (placeState == null) {
            return false;
        }

        this.avNpc.swing(InteractionHand.MAIN_HAND, true);
        this.avNpc.playSound(SoundEvents.STONE_PLACE, 2.0F, 1.0F);
        serverLevel.setBlockAndUpdate(pos, placeState);
        return true;
    }

    private Direction getThreatDirection(Projectile projectile) {
        double dx = projectile.getX() - this.avNpc.getX();
        double dz = projectile.getZ() - this.avNpc.getZ();

        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        }

        if (Math.abs(dz) > 1.0E-6D) {
            return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
        }

        return this.avNpc.getDirection();
    }
}
