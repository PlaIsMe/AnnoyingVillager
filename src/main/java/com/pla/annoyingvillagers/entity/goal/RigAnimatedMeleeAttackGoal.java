package com.pla.annoyingvillagers.entity.goal;

import com.pla.annoyingvillagers.clazz.DangerousReaction;
import com.pla.annoyingvillagers.entity.NullEntity;
import com.pla.annoyingvillagers.entity.ReaperHerobrineEntity;
import com.pla.annoyingvillagers.item.DemoniacVoltageReaverItem;
import com.pla.annoyingvillagers.rig.LockableRigAttackAnimation;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigAnimationSpec;
import com.pla.annoyingvillagers.rig.RigAnimationSpecs;
import com.pla.annoyingvillagers.rig.RigColliderSystem;
import com.pla.annoyingvillagers.rig.RigCombatProfile;
import com.pla.annoyingvillagers.rig.RigCombatProfiles;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.util.RidingUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RigAnimatedMeleeAttackGoal extends Goal {
    private static final int PATH_RECALCULATION_BASE_TICKS = 4;

    private final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;

    private Path path;
    private int ticksUntilNextPathRecalculation;
    private int normalComboIndex;
    private Item lastWeaponItem;
    private RigAnimationId previousAnimation;

    private int nullFlightModeTicks;
    private int nullFlightSpeedTicks;
    private boolean nullOrbiting;
    private double nullOrbitAngle;
    private double nullOrbitRadius = 3.5D;
    private double nullOrbitHeight = 1.5D;
    private double nullOrbitAngularSpeed = 0.13D;
    private double nullBaseFlightSpeed = 2.6D;
    private int nullOrbitDirection = 1;

    public RigAnimatedMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (!isValidMeleeState(target) || isMovementLocked()) return false;

        if (isProfileAttackLocked()) {
            if (!canMoveWhileProfileAttackLocked()) return false;
            this.path = RidingUtil.createNavigationPath(this.mob, target);
            return true;
        }

        RigCombatProfile profile = RigCombatProfiles.getCombatProfile(this.mob);
        if (hasBlockingAnimation(profile)) return false;
        if (DangerousReaction.hasDangerousTarget(this.mob)) {
            this.path = null;
            return canStartAnyAttack(target, profile);
        }
        if (this.mob instanceof NullEntity) {
            this.path = null;
            return true;
        }
        this.path = RidingUtil.createNavigationPath(this.mob, target);
        return this.path != null || canStartAnyAttack(target, profile);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (!isValidMeleeState(target) || isMovementLocked()) return false;
        if (isProfileAttackLocked()) return canMoveWhileProfileAttackLocked();

        RigCombatProfile profile = RigCombatProfiles.getCombatProfile(this.mob);
        if (DangerousReaction.hasDangerousTarget(this.mob)) {
            return RigAnimationController.hasActiveAnimation(this.mob) || canStartAnyAttack(target, profile);
        }
        if (this.mob instanceof NullEntity) return true;
        return this.followingTargetEvenIfNotSeen || !RidingUtil.isNavigationDone(this.mob) || RigAnimationController.hasActiveAnimation(this.mob) || canStartAnyAttack(target, profile);
    }

    @Override
    public void start() {
        if (this.path != null && !DangerousReaction.hasDangerousTarget(this.mob)) RidingUtil.moveToPath(this.mob, this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        if (this.mob instanceof NullEntity) {
            this.nullFlightModeTicks = 0;
            this.nullFlightSpeedTicks = 0;
        }
    }

    @Override
    public void stop() {
        // AvNpc utilities own higher goal priorities. Cancel only the clip this goal
        // started; a stun or scripted action may already have replaced it.
        if (this.mob instanceof com.pla.annoyingvillagers.clazz.AVNpc npc
                && !npc.isLocked() && !RigStunController.isStunned(this.mob)
                && this.previousAnimation == RigAnimationController.getActiveAnimationId(this.mob)
                && RigAnimationController.hasActiveProfileAttack(this.mob)) {
            RigAnimationController.stop(this.mob, this.previousAnimation);
        }
        this.mob.setAggressive(false);
        RidingUtil.stopNavigation(this.mob);
        this.path = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean isInterruptable() {
        // Waiting for the chain boundary can starve recovery: attack ticks may start
        // the next swing before the goal selector gets another admission pass.
        if (this.mob instanceof com.pla.annoyingvillagers.clazz.AVNpc npc
                && !npc.isLocked() && !RigStunController.isStunned(this.mob)
                && RigAnimationController.hasActiveProfileAttack(this.mob)) return true;
        if (!RigAnimationController.hasActiveAnimation(this.mob)) {
            return true;
        }
        return RigAnimationController.hasActiveProfileAttack(this.mob)
                && RigAnimationController.isAttackChainReady(this.mob);
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;
        if (isMovementLocked()) {
            RidingUtil.stopNavigation(this.mob);
            return;
        }

        updateWeaponComboState();
        RidingUtil.lookAtTarget(this.mob, target, 60.0F, 60.0F);

        // A profile attack lock only blocks attack selection. Keep closing the
        // distance so snake-blade users do not freeze in place for the whole action.
        if (isProfileAttackLocked()) {
            if (canMoveWhileProfileAttackLocked()) {
                repathToTargetWhileAttackLocked(target);
            } else {
                RidingUtil.stopNavigation(this.mob);
            }
            return;
        }

        RigCombatProfile profile = RigCombatProfiles.getCombatProfile(this.mob);
        if (hasBlockingAnimation(profile)) {
            RidingUtil.stopNavigation(this.mob);
            return;
        }

        RigAnimationId selectedAnimation = isNormalAttackChainReady(profile) ? selectQueuedNormalAttack(target, profile) : selectAnimation(target, profile);
        if (selectedAnimation != null) {
            playAnimationAttack(target, selectedAnimation);
            return;
        }
        repathToTarget(target);
    }

    private RigAnimationId selectAnimation(LivingEntity target, RigCombatProfile profile) {
        if (this.mob.isPassenger()) return canStartAttack(target, RigAnimationId.BASIC_MOUNT_ATTACK) ? RigAnimationId.BASIC_MOUNT_ATTACK : null;

        RigAnimationId normal = profile.normalAt(this.normalComboIndex);
        if (!canStartAttack(target, normal)) {
            RigAnimationId nullExtraAttack = this.selectNullExtraAttackWithoutApproach(target, profile);
            if (nullExtraAttack != null) return nullExtraAttack;
            RigAnimationId closing = profile.pickClosingAttack(this.mob.getRandom(), this.previousAnimation);
            return closing != null && canStartAttack(target, closing) ? closing : null;
        }

        RigAnimationId interrupt = profile.pickInterrupt(this.mob.getRandom(), this.previousAnimation);
        if (interrupt != null) return interrupt;
        this.normalComboIndex = (this.normalComboIndex + 1) % profile.normalAttacks().size();
        return normal;
    }

    private RigAnimationId selectNullExtraAttackWithoutApproach(LivingEntity target, RigCombatProfile profile) {
        if (!(this.mob instanceof NullEntity)) return null;
        if (!profile.ultimateAttacks().contains(RigAnimationId.NULL_EXTRA_ATTACK)) return null;
        if (this.previousAnimation == RigAnimationId.NULL_EXTRA_ATTACK) return null;
        if (this.mob.getRandom().nextDouble() >= profile.ultimateAttackChance()) return null;
        return canStartAttack(target, RigAnimationId.NULL_EXTRA_ATTACK) ? RigAnimationId.NULL_EXTRA_ATTACK : null;
    }

    private RigAnimationId selectQueuedNormalAttack(LivingEntity target, RigCombatProfile profile) {
        RigAnimationId normal = profile.normalAt(this.normalComboIndex);
        if (!canStartAttack(target, normal)) return null;
        this.normalComboIndex = (this.normalComboIndex + 1) % profile.normalAttacks().size();
        return normal;
    }

    private void playAnimationAttack(LivingEntity target, RigAnimationId animationId) {
        RigAnimationSpec spec = RigAnimationSpecs.get(animationId);
        RigAnimationController.play(this.mob, spec, target);
        this.previousAnimation = animationId;
    }

    private boolean hasBlockingAnimation(RigCombatProfile profile) {
        if (!RigAnimationController.hasActiveAnimation(this.mob)) return false;
        return !isNormalAttackChainReady(profile);
    }

    private boolean isNormalAttackChainReady(RigCombatProfile profile) {
        if (!RigAnimationController.isAttackChainReady(this.mob)) return false;
        RigAnimationId activeAnimation = RigAnimationController.getActiveAnimationId(this.mob);
        return activeAnimation != null && profile.normalAttacks().contains(activeAnimation);
    }

    private void repathToTarget(LivingEntity target) {
        if (DangerousReaction.hasDangerousTarget(this.mob)) {
            RidingUtil.stopNavigation(this.mob);
            return;
        }
        if (this.mob instanceof NullEntity) {
            this.tickNullCombatFlight(target);
            return;
        }
        if (--this.ticksUntilNextPathRecalculation > 0 && !RidingUtil.isNavigationDone(this.mob)) return;
        this.ticksUntilNextPathRecalculation = PATH_RECALCULATION_BASE_TICKS + this.mob.getRandom().nextInt(7);
        RidingUtil.moveTo(this.mob, target, this.speedModifier);
    }

    private void tickNullCombatFlight(LivingEntity target) {
        RidingUtil.stopNavigation(this.mob);

        Vec3 targetPosition = target.getEyePosition(1.0F);
        Vec3 targetMovement = target.getDeltaMovement();
        double distance = this.mob.position().distanceTo(targetPosition);
        double verticalToTarget = targetPosition.y - this.mob.getY();

        if (--this.nullFlightSpeedTicks <= 0) {
            this.nullBaseFlightSpeed = 2.2D + this.mob.getRandom().nextDouble() * 1.7D;
            this.nullFlightSpeedTicks = 12 + this.mob.getRandom().nextInt(25);
        }

        if (--this.nullFlightModeTicks <= 0) this.chooseNullFlightMode(target, distance);

        if (distance > 9.0D || verticalToTarget < -2.0D) {
            this.nullOrbiting = false;
            this.nullFlightModeTicks = Math.min(this.nullFlightModeTicks, 12);
        }

        if (this.nullOrbiting && distance > 2.5D && distance < 10.0D) {
            this.tickNullTargetOrbit(target);
            return;
        }

        Vec3 predictedTarget = targetPosition.add(targetMovement.x * 2.5D, targetMovement.y * 1.25D, targetMovement.z * 2.5D);
        double desiredVertical = predictedTarget.y - this.mob.getY();
        double flightSpeed = this.nullBaseFlightSpeed;

        if (desiredVertical < -1.0D) {
            flightSpeed = Math.max(3.6D, flightSpeed + 1.1D + this.mob.getRandom().nextDouble() * 0.45D);
        } else if (desiredVertical > 1.0D) {
            flightSpeed = Math.min(1.9D, flightSpeed * 0.62D);
        } else if (distance > 10.0D) {
            flightSpeed += 0.65D;
        }

        if (this.mob instanceof NullEntity nullEntity && nullEntity.getState() >= 2) flightSpeed *= 1.15D;
        flightSpeed = Mth.clamp(flightSpeed, 1.25D, 5.0D);
        this.mob.getMoveControl().setWantedPosition(predictedTarget.x, predictedTarget.y, predictedTarget.z, flightSpeed);
    }

    private void chooseNullFlightMode(LivingEntity target, double distance) {
        boolean canOrbit = distance > 3.0D && distance < 9.0D;
        this.nullOrbiting = canOrbit && this.mob.getRandom().nextFloat() < 0.42F;

        if (this.nullOrbiting) {
            double dx = this.mob.getX() - target.getX();
            double dz = this.mob.getZ() - target.getZ();
            this.nullOrbitAngle = Math.atan2(dz, dx);
            this.nullOrbitRadius = 2.75D + this.mob.getRandom().nextDouble() * 2.25D;
            this.nullOrbitHeight = 0.75D + this.mob.getRandom().nextDouble() * 2.75D;
            this.nullOrbitAngularSpeed = 0.10D + this.mob.getRandom().nextDouble() * 0.09D;
            this.nullOrbitDirection = this.mob.getRandom().nextBoolean() ? 1 : -1;
            this.nullFlightModeTicks = 16 + this.mob.getRandom().nextInt(25);
        } else {
            this.nullFlightModeTicks = 10 + this.mob.getRandom().nextInt(19);
        }
    }

    private void tickNullTargetOrbit(LivingEntity target) {
        if (this.mob.getRandom().nextInt(55) == 0) this.nullOrbitDirection = -this.nullOrbitDirection;
        if (this.mob.getRandom().nextInt(45) == 0) this.nullOrbitRadius = 2.75D + this.mob.getRandom().nextDouble() * 2.25D;

        this.nullOrbitAngle += this.nullOrbitAngularSpeed * this.nullOrbitDirection;

        double x = target.getX() + Math.cos(this.nullOrbitAngle) * this.nullOrbitRadius;
        double z = target.getZ() + Math.sin(this.nullOrbitAngle) * this.nullOrbitRadius;
        double y = target.getEyeY() + this.nullOrbitHeight + Math.sin((this.mob.tickCount + this.mob.getId()) * 0.18D) * 0.55D;
        double verticalDelta = y - this.mob.getY();
        double orbitSpeed = this.nullBaseFlightSpeed * 0.72D;

        if (verticalDelta > 0.75D) orbitSpeed = Math.min(1.8D, orbitSpeed);
        if (verticalDelta < -0.75D) orbitSpeed = Math.max(3.0D, orbitSpeed + 0.8D);
        if (this.mob instanceof NullEntity nullEntity && nullEntity.getState() >= 2) orbitSpeed *= 1.12D;

        this.mob.getMoveControl().setWantedPosition(x, y, z, Mth.clamp(orbitSpeed, 1.2D, 4.25D));
    }

    private void repathToTargetWhileAttackLocked(LivingEntity target) {
        if (this.mob.isPassenger()) return;
        if (--this.ticksUntilNextPathRecalculation > 0 && !RidingUtil.isNavigationDone(this.mob)) return;
        this.ticksUntilNextPathRecalculation = PATH_RECALCULATION_BASE_TICKS + this.mob.getRandom().nextInt(7);
        RidingUtil.moveTo(this.mob, target, this.speedModifier);
    }

    private void updateWeaponComboState() {
        ItemStack mainHand = this.mob.getMainHandItem();
        Item currentWeapon = mainHand.isEmpty() ? null : mainHand.getItem();
        if (currentWeapon == this.lastWeaponItem) return;
        this.lastWeaponItem = currentWeapon;
        this.normalComboIndex = 0;
        this.previousAnimation = null;
    }

    private boolean isMovementLocked() {
        return RigStunController.isStunned(this.mob);
    }

    private boolean isProfileAttackLocked() {
        return this.mob instanceof LockableRigAttackAnimation lockable && lockable.isLocked();
    }

    private boolean canMoveWhileProfileAttackLocked() {
        return DemoniacVoltageReaverItem.hasSnakeProfileAttackLock(this.mob);
    }

    private boolean isValidMeleeState(LivingEntity target) {
        if (this.mob instanceof ReaperHerobrineEntity reaper && reaper.isSecondFormDragonRider()) {
            return false;
        }
        return !this.mob.level().isClientSide && this.mob.isAlive() && !this.mob.isRemoved() && !this.mob.isDeadOrDying() && !this.mob.isNoAi()
                && !(this.mob.getMainHandItem().getItem() instanceof BowItem) && target != null && target.isAlive() && !target.isRemoved() && !target.isDeadOrDying();
    }

    private boolean canStartNormalAttack(LivingEntity target, RigCombatProfile profile) {
        return canStartAttack(target, profile.normalAt(this.normalComboIndex));
    }

    private boolean canStartAnyAttack(LivingEntity target, RigCombatProfile profile) {
        if (this.mob.isPassenger()) return canStartAttack(target, RigAnimationId.BASIC_MOUNT_ATTACK);
        for (RigAnimationId animationId : profile.normalAttacks()) if (canStartAttack(target, animationId)) return true;
        for (RigAnimationId animationId : profile.specialAttacks()) if (canStartAttack(target, animationId)) return true;
        for (RigAnimationId animationId : profile.ultimateAttacks()) if (canStartAttack(target, animationId)) return true;
        return false;
    }

    private boolean canStartAttack(LivingEntity target, RigAnimationId animationId) {
        RigAnimationSpec spec = RigAnimationSpecs.get(animationId);
        if (!spec.damagesTarget()) return false;
        if (this.mob instanceof NullEntity) {
            if (animationId == RigAnimationId.NULL_EXTRA_ATTACK) return RigColliderSystem.canStartAttackWithinDistance(this.mob, target, 24.0D);
            return RigColliderSystem.canStartAttack3D(this.mob, target, spec);
        }
        return RigColliderSystem.canStartAttack(this.mob, target, spec);
    }
}
