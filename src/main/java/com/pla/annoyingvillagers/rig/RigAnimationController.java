package com.pla.annoyingvillagers.rig;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.network.ClientboundRigAnimation;
import com.pla.annoyingvillagers.rig.pose.RigPoseLibrary;
import com.pla.annoyingvillagers.task.DelayedTask;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Team;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RigAnimationController {
    // Held poses have no natural end; this packet duration only keeps the client pose alive
    // until an explicit stop/replacement packet arrives. One day is effectively unbounded
    // for a hook session without risking integer overflow in client blend calculations.
    private static final int HELD_POSE_DURATION_TICKS = 20 * 60 * 60 * 24;
    private static final Map<UUID, ActiveAnimationState> ACTIVE_ANIMATIONS = new HashMap<>();

    private RigAnimationController() {
    }

    public static void play(Mob mob, RigAnimationId animationId) {
        play(mob, RigAnimationSpecs.get(animationId), null);
    }

    public static void playHeldPose(Mob mob, RigAnimationId animationId) {
        RigAnimationSpec baseSpec = RigAnimationSpecs.get(animationId);
        if (baseSpec.damagesTarget()) {
            throw new IllegalArgumentException("Held rig poses must be non-damaging: " + animationId);
        }
        if (RigStunController.isStunned(mob) || mob.level().isClientSide || !mob.isAlive() || mob.isRemoved()) {
            return;
        }

        RigAnimationSpec heldSpec = RigAnimationSpec.nonDamaging(
                animationId,
                HELD_POSE_DURATION_TICKS,
                baseSpec.playbackType()
        );
        recordActiveAnimation(mob, heldSpec);
        sendAnimation(mob, animationId, HELD_POSE_DURATION_TICKS);
    }

    public static void stop(Mob mob, RigAnimationId animationId) {
        if (animationId == null || mob.level().isClientSide) {
            return;
        }

        ActiveAnimationState state = ACTIVE_ANIMATIONS.get(mob.getUUID());
        if (state == null || state.spec().animationId() != animationId) {
            return;
        }

        if (ACTIVE_ANIMATIONS.remove(mob.getUUID(), state)) {
            sendAnimation(mob, animationId, 0);
        }
    }

    public static void play(Mob mob, RigAnimationSpec spec, LivingEntity target) {
        if (isInvulnerable(mob) && RigStunController.isStunAnimation(spec.animationId())) return;
        if (RigStunController.isStunned(mob) || mob.level().isClientSide || !mob.isAlive() || mob.isRemoved() || !canPlayWhileMounted(mob, spec) || isProfileAttackLocked(mob, spec.animationId())) return;
        playNow(mob, spec, target);
    }

    static void playStunAnimation(Mob mob, RigAnimationId animationId) {
        RigAnimationSpec spec = RigAnimationSpecs.get(animationId);
        if (isInvulnerable(mob) || mob.level().isClientSide || !mob.isAlive() || mob.isRemoved()) return;
        playNow(mob, spec, null);
    }

    public static void lockProfileAttacksFor(Mob mob, int ticks) {
        if (mob.level().isClientSide || !(mob instanceof LockableRigAttackAnimation lockable) || ticks <= 0) return;
        lockable.lock();
        new DelayedTask(ticks) {
            @Override
            public void run() {
                lockable.unlock();
            }
        };
    }

    public static void lockProfileAttacksFor(Mob mob, RigAnimationId animationId) {
        lockProfileAttacksFor(mob, RigAnimationSpecs.get(animationId).durationTicks());
    }

    public static int animationPlaybackTicks(RigAnimationSpec spec) {
        return spec.durationTicks();
    }

    private static void playNow(Mob mob, RigAnimationSpec spec, LivingEntity target) {
        if (mob.level().isClientSide || !mob.isAlive() || mob.isRemoved() || !canPlayWhileMounted(mob, spec) || isProfileAttackLocked(mob, spec.animationId())) return;
        if (mob instanceof com.pla.annoyingvillagers.clazz.AVNpc npc
                && net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new com.pla.annoyingvillagers.event.AVNpcRigAnimationEvent(npc, spec, target))) return;
        if (target != null && target.isAlive()) faceTarget(mob, target);
        if (spec.animationId().isAttack()) mob.swing(InteractionHand.MAIN_HAND, true);
        if (spec.jumpOnStart() && mob.onGround()) mob.getJumpControl().jump();

        ActiveAnimationState state = recordActiveAnimation(mob, spec);
        runHooks(mob, spec, RigAnimationSpec.RigTimedAnimationHook.START);
        scheduleTimedHooks(mob, spec, state);
        scheduleAnimationEnd(mob, spec, state);
        sendAnimation(mob, spec.animationId(), spec.durationTicks());
        scheduleRigSounds(mob, spec);
        scheduleAnimationMotion(mob, target, spec, state);
        if (spec.damagesTarget()) scheduleCollisions(mob, spec, state);
    }

    public static boolean isInActiveAttackWindow(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        if (state == null || !state.spec().damagesTarget()) return false;
        int elapsedTicks = state.elapsedTicks(mob);
        for (RigAttackWindow attackWindow : state.spec().attackWindows()) if (attackWindow.contains(elapsedTicks)) return true;
        return false;
    }

    public static boolean hasActiveAnimation(Mob mob) {
        return getActiveAnimationState(mob) != null;
    }

    public static boolean isInvulnerable(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state != null && state.spec().invulnerableDuringAnimation();
    }

    public static boolean isDangerous(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state != null && state.spec().dangerousDuringAnimation();
    }

    public static int getActiveAnimationStartTick(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state == null ? -1 : state.startTick();
    }

    public static RigAnimationId getActiveAnimationId(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state == null ? null : state.spec().animationId();
    }

    public static boolean isAttackChainReady(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state != null && state.spec().damagesTarget() && state.elapsedTicks(mob) >= state.spec().lastAttackWindowEndTick();
    }

    public static int remainingTicksUntilAttackChainReady(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        if (state == null) return 0;
        int releaseTick = state.spec().damagesTarget() ? state.spec().lastAttackWindowEndTick() : state.spec().durationTicks();
        return Math.max(0, releaseTick - state.elapsedTicks(mob));
    }

    public static boolean hasActiveProfileAttack(Mob mob) {
        ActiveAnimationState state = getActiveAnimationState(mob);
        return state != null && RigCombatProfiles.isProfileAttack(state.spec().animationId());
    }

    public static void clearActiveAnimations() {
        ACTIVE_ANIMATIONS.clear();
    }

    private static boolean isProfileAttackLocked(Mob mob, RigAnimationId animationId) {
        return RigCombatProfiles.isProfileAttack(animationId) && mob instanceof LockableRigAttackAnimation lockable && lockable.isLocked();
    }

    private static boolean canPlayWhileMounted(Mob mob, RigAnimationSpec spec) {
        return !mob.isPassenger() || !spec.animationId().isAttack() || spec.animationId().isMountedAttack();
    }

    private static ActiveAnimationState recordActiveAnimation(Mob mob, RigAnimationSpec spec) {
        ActiveAnimationState state = new ActiveAnimationState(spec, mob.tickCount);
        ACTIVE_ANIMATIONS.put(mob.getUUID(), state);
        return state;
    }

    private static ActiveAnimationState getActiveAnimationState(Mob mob) {
        ActiveAnimationState state = ACTIVE_ANIMATIONS.get(mob.getUUID());
        if (state == null) return null;
        int elapsedTicks = state.elapsedTicks(mob);
        if (elapsedTicks < 0 || elapsedTicks > state.spec().durationTicks()) {
            ACTIVE_ANIMATIONS.remove(mob.getUUID(), state);
            return null;
        }
        return state;
    }

    private record ActiveAnimationState(RigAnimationSpec spec, int startTick) {
        private int elapsedTicks(Mob mob) {
            return mob.tickCount - this.startTick;
        }
    }

    private static boolean isCurrentAnimation(Mob mob, ActiveAnimationState state) {
        return ACTIVE_ANIMATIONS.get(mob.getUUID()) == state;
    }

    private static void sendAnimation(Mob mob, RigAnimationId animationId, int durationTicks) {
        AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundRigAnimation(mob.getId(), animationId, durationTicks));
    }

    private static void scheduleAnimationEnd(Mob mob, RigAnimationSpec spec, ActiveAnimationState state) {
        new DelayedTask(spec.durationTicks()) {
            @Override
            public void run() {
                if (!mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying()) {
                    ACTIVE_ANIMATIONS.remove(mob.getUUID(), state);
                    return;
                }
                if (ACTIVE_ANIMATIONS.remove(mob.getUUID(), state)) runHooks(mob, spec, RigAnimationSpec.RigTimedAnimationHook.END);
            }
        };
    }

    private static void scheduleTimedHooks(Mob mob, RigAnimationSpec spec, ActiveAnimationState state) {
        for (RigAnimationSpec.RigTimedAnimationHook timedHook : spec.timedHooks()) {
            if (!timedHook.isTimed()) continue;
            new DelayedTask(timedHook.tick()) {
                @Override
                public void run() {
                    if (!mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying() || !isCurrentAnimation(mob, state)) return;
                    runHook(mob, timedHook.action());
                }
            };
        }
    }

    private static void runHooks(Mob mob, RigAnimationSpec spec, int tick) {
        for (RigAnimationSpec.RigTimedAnimationHook timedHook : spec.timedHooks()) if (timedHook.tick() == tick) runHook(mob, timedHook.action());
    }

    private static void runHook(Mob mob, RigAnimationSpec.RigAnimationHook hook) {
        try {
            hook.run(mob);
        } catch (Exception e) {
            AnnoyingVillagers.LOGGER.error("[AV MOD DEBUG] Rig animation hook failed for {}", mob.getName().getString(), e);
        }
    }

    private static void runHitHook(Mob mob, LivingEntity target, boolean critical, RigAnimationSpec.RigHitHook hitHook) {
        try {
            hitHook.onHit(mob, target, critical);
        } catch (Exception e) {
            AnnoyingVillagers.LOGGER.error("[AV MOD DEBUG] Rig hit hook failed for {} -> {}", mob.getName().getString(), target.getName().getString(), e);
        }
    }

    private static void scheduleRigSounds(Mob mob, RigAnimationSpec spec) {
        RigAnimationId animationId = spec.animationId();
        if (animationId.isAttack()) {
            scheduleAttackSwingSounds(mob, spec);
            return;
        }
        if (animationId.isRollAnimation()) {
            playSound(mob, AnnoyingVillagersModSounds.ROLL.get(), 0.9F, randomPitch(mob, 0.95F, 0.1F));
            return;
        }
        if (!animationId.isUtilityAnimation()) playStepSound(mob);
    }

    private static void scheduleAttackSwingSounds(Mob mob, RigAnimationSpec spec) {
        RigAttackWindow[] attackWindows = spec.attackWindows();
        SoundEvent sound = getAttackSwingSound(spec.animationId(), attackWindows.length);

        if (attackWindows.length == 0) {
            playSound(mob, sound, 0.9F, randomPitch(mob, 0.95F, 0.1F));
            return;
        }

        for (RigAttackWindow attackWindow : attackWindows) {
            new DelayedTask(attackWindow.startTickInclusive()) {
                @Override
                public void run() {
                    if (mob.isAlive() && !mob.isRemoved() && !mob.isDeadOrDying()) playSound(mob, sound, 0.9F, randomPitch(mob, 0.95F, 0.1F));
                }
            };
        }
    }

    private static SoundEvent getAttackSwingSound(RigAnimationId animationId, int attackWindowCount) {
        if (usesUnarmedSound(animationId)) return AnnoyingVillagersModSounds.WHOOSH.get();
        return attackWindowCount > 1 ? AnnoyingVillagersModSounds.WHOOSH_SHARP.get() : AnnoyingVillagersModSounds.SWORD_WHOOSH.get();
    }

    private static boolean usesUnarmedSound(RigAnimationId animationId) {
        return switch (animationId) {
            case FIST_ATTACK1,
                 FIST_ATTACK2,
                 FIST_ATTACK3,
                 FIST_ATTACK4,
                 FIST_ATTACK5,
                 FIST_DASH_ATTACK,
                 FIST_JUMP_ATTACK,
                 FIST_ULT,
                 FIST_EXTRA_ATTACK,
                 OBSIDIAN_DASH_ATTACK,
                 OBSIDIAN_JUMP_ATTACK,
                 OBSIDIAN_EXTRA_ATTACK,
                 OBSIDIAN_PILLAR_EXTRA_ATTACK,
                 KICK_ATTACK1,
                 KICK_ATTACK2,
                 KICK_ATTACK3,
                 KICK_ATTACK4,
                 KICK_COMBO_ATTACK,
                 KICK_DASH_ATTACK,
                 NULL_ATTACK1,
                 NULL_ATTACK2,
                 NULL_ATTACK3,
                 NULL_ATTACK4,
                 NULL_ATTACK5,
                 NULL_DASH_ATTACK,
                 NULL_JUMP_ATTACK,
                 NULL_EXTRA_ATTACK,
                 NULL_EXTRA_ULT,
                 NULL_SKELETON_SPAWN,
                 AEGIS_HEROBRINE_ATTACK1,
                 AEGIS_HEROBRINE_ATTACK2,
                 AEGIS_HEROBRINE_ATTACK3,
                 AEGIS_HEROBRINE_ATTACK4,
                 AEGIS_HEROBRINE_ATTACK5,
                 AEGIS_HEROBRINE_DASH_ATTACK,
                 AEGIS_HEROBRINE_JUMP_ATTACK,
                 AEGIS_HEROBRINE_EXTRA_ATTACK,
                 AEGIS_HEROBRINE_ULT -> true;
            default -> false;
        };
    }

    private static void playStepSound(Mob mob) {
        BlockPos onPos = mob.getOnPos();
        BlockState state = mob.level().getBlockState(onPos);
        SoundType soundType = state.getSoundType();
        playSound(mob, soundType.getHitSound(), soundType.getVolume() * 0.35F, soundType.getPitch());
    }

    private static void playHitSound(LivingEntity target, RigAnimationId animationId) {
        SoundEvent sound = usesUnarmedSound(animationId) ? AnnoyingVillagersModSounds.BLUNT_HIT.get() : AnnoyingVillagersModSounds.BLADE_HIT.get();
        playSound(target, sound, 0.9F, randomPitch(target, 0.95F, 0.1F));
    }

    private static float randomPitch(Entity entity, float basePitch, float variance) {
        return basePitch + (entity.level().random.nextFloat() * 2.0F - 1.0F) * variance;
    }

    private static void playSound(Entity entity, SoundEvent sound, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void scheduleCollisions(Mob mob, RigAnimationSpec spec, ActiveAnimationState state) {
        RigAttackWindow[] attackWindows = spec.attackWindows();
        boolean forceDamageThroughHurtCooldown = attackWindows.length > 1;
        for (RigAttackWindow attackWindow : attackWindows) scheduleAttackWindow(mob, attackWindow, state, forceDamageThroughHurtCooldown);
    }

    private static void scheduleAttackWindow(Mob mob, RigAttackWindow attackWindow, ActiveAnimationState state, boolean forceDamageThroughHurtCooldown) {
        Set<UUID> hitEntities = new HashSet<>();
        for (int elapsedTick = attackWindow.startTickInclusive(); elapsedTick < attackWindow.endTickExclusive(); elapsedTick++) {
            final int sampleTick = elapsedTick;
            new DelayedTask(sampleTick) {
                @Override
                public void run() {
                    if (!isCurrentAnimation(mob, state) || !mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying()) return;
                    for (LivingEntity target : RigColliderSystem.findHits(mob, state.spec(), attackWindow, sampleTick)) {
                        if (!canDamageTarget(mob, target) || !hitEntities.add(target.getUUID())) continue;
                        boolean critical = mob.getRandom().nextFloat() < state.spec().criticalChance();
                        if (!hurtTarget(mob, target, state.spec(), critical, forceDamageThroughHurtCooldown)) continue;
                        target.setLastHurtByMob(mob);
                        mob.setLastHurtMob(target);
                        playHitSound(target, state.spec().animationId());
                        runHitHook(mob, target, critical, state.spec().hitHook());
                    }
                }
            };
        }
    }

    private static boolean hurtTarget(Mob mob, LivingEntity target, RigAnimationSpec spec, boolean critical, boolean forceDamageThroughHurtCooldown) {
        RigDamageContext.push(mob, target, spec.damageMultiplier(), critical);
        try {
            if (!forceDamageThroughHurtCooldown) {
                boolean hurt = mob.doHurtTarget(target);
                if (hurt) spawnHitParticle(mob, target);
                return hurt;
            }

            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            try {
                boolean hurt = mob.doHurtTarget(target);
                if (hurt) spawnHitParticle(mob, target);
                return hurt;
            } finally {
                target.invulnerableTime = previousInvulnerableTime;
            }
        } finally {
            RigDamageContext.pop();
        }
    }

    private static void spawnHitParticle(Mob mob, LivingEntity target) {
        if (!(mob.level() instanceof ServerLevel)) return;
//        AnnoyingVillagersModParticleTypes.BLADE_RUSH.get().spawnParticleWithArgument(...);
    }

    private static boolean canDamageTarget(Mob mob, LivingEntity target) {
        if (!mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying()) return false;
        if (!target.isAlive() || target.isRemoved() || target.isDeadOrDying()) return false;
        if (target == mob.getVehicle()) return false;
        if (areAllied(mob, target)) return false;
        return mob.canAttack(target);
    }

    private static boolean areAllied(Mob mob, LivingEntity target) {
        Team mobTeam = mob.getTeam();
        Team targetTeam = target.getTeam();
        if (mobTeam != null && targetTeam != null && (mobTeam == targetTeam || mobTeam.isAlliedTo(targetTeam) || targetTeam.isAlliedTo(mobTeam))) return true;
        return mob.isAlliedTo(target) || target.isAlliedTo(mob);
    }

    private static void scheduleAnimationMotion(Mob mob, LivingEntity target, RigAnimationSpec spec, ActiveAnimationState state) {
        RigAnimationId animationId = spec.animationId();
        if (!RigPoseLibrary.hasMotion(animationId)) return;
        Vec3 forward = horizontalDirection(mob, target);
        for (int elapsedTick = 1; elapsedTick <= spec.durationTicks(); elapsedTick++) {
            final int currentElapsedTick = elapsedTick;
            new DelayedTask(currentElapsedTick - 1) {
                @Override
                public void run() {
                    if (!mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying() || !isCurrentAnimation(mob, state)) return;
                    Vec3 delta = RigPoseLibrary.worldMotionDelta(animationId, currentElapsedTick - 1.0F, currentElapsedTick, forward, spec.moveVertical());
                    if (delta.lengthSqr() < 1.0E-8D) return;
                    if (spec.moveVertical()) {
                        Vec3 currentMotion = mob.getDeltaMovement();
                        delta = delta.add(0.0D, -currentMotion.y, 0.0D);
                        mob.setDeltaMovement(currentMotion.x, 0.0D, currentMotion.z);
                        mob.fallDistance = 0.0F;
                    }
                    mob.move(MoverType.SELF, delta);
                    mob.hasImpulse = true;
                    mob.hurtMarked = true;
                }
            };
        }
    }

    private static void faceTarget(Mob mob, LivingEntity target) {
        mob.getLookControl().setLookAt(target, 60.0F, 60.0F);
        mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        if (mob.isPassenger() && mob.getVehicle() instanceof Mob mount) {
            mount.getLookControl().setLookAt(target, 60.0F, 60.0F);
            mount.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        }
    }

    private static Vec3 horizontalDirection(Mob mob, Entity target) {
        Vec3 direction = target != null ? target.position().subtract(mob.position()) : mob.getLookAngle();
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = Vec3.directionFromRotation(0.0F, mob.getYRot());
            direction = new Vec3(direction.x, 0.0D, direction.z);
        }
        return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }
}
