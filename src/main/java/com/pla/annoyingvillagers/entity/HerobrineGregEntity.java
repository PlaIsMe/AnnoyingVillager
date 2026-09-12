package com.pla.annoyingvillagers.entity;

import javax.annotation.Nullable;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.blockentity.CryingObsidianBlockEntity;
import com.pla.annoyingvillagers.blockentity.ObsidianBlockEntity;
import com.pla.annoyingvillagers.blockentity.ShadowObsidianBlockEntity;
import com.pla.annoyingvillagers.clazz.*;
import com.pla.annoyingvillagers.compat.SmartNpc;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.entity.goal.HerobrineGregSixPortalSupportGoal;
import com.pla.annoyingvillagers.entity.goal.HerobrineLowCloneSupportGoal;
import com.pla.annoyingvillagers.entity.goal.HerobrineProjectileCounterPortalGoal;
import com.pla.annoyingvillagers.entity.goal.HerobrineSupportApproachPortalGoal;
import com.pla.annoyingvillagers.entity.goal.HerobrineSupportEscapePortalGoal;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModBlocks;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.network.ClientboundHerobrinePortalFx;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigAnimationSpecs;
import com.pla.annoyingvillagers.rig.RigStunnableEntity;
import com.pla.annoyingvillagers.util.*;
import com.pla.annoyingvillagers.spawnhandler.GregData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class HerobrineGregEntity extends Monster implements ForceTickEntity, RigStunnableEntity, DangerousReaction, HerobrinePortalSupportCaster {
    private static final int MAX_COMBAT_LOW_CLONE_SUPPORT = 5;
    private static final float FISHING_HOOK_ESCAPE_CANCEL_CHANCE = 0.8F;
    private static final double SECOND_FORM_SUPPORT_SEARCH_RADIUS_SQR = 48.0D * 48.0D;
    private static final double FOLLOW_SUPPORT_SEARCH_RADIUS = 96.0D;
    private static final double FOLLOW_SUPPORT_LEASH_RADIUS_SQR = 128.0D * 128.0D;

    private static final EntityDataAccessor<Boolean> WHITE_EYE =
            SynchedEntityData.defineId(HerobrineGregEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USE_HEROBRINE_TEXTURE =
            SynchedEntityData.defineId(HerobrineGregEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SUPPORTING_HEROBRINE =
            SynchedEntityData.defineId(HerobrineGregEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOOKED =
            SynchedEntityData.defineId(HerobrineGregEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean summoning = false;
    private int summonTiming = -1;
    private int escapeTiming = -1;
    private int summonTimestamp = -1;
    private boolean combatMode = false;
    private int recallTime;
    private boolean fishingHookCancelledEscape;
    private boolean hookedWaitingForGround;
    private boolean hookedLeftGround;
    private int idleAvoidRepathCooldown;
    private int lowCloneSupportCooldown = 0;
    private int portalActionCooldown = 0;
    private int supportingHerobrineVisualTicks;
    private Entity firstSummonedHerobrine;
    private Entity secondSummonedHerobrine;
    private Entity thirdSummonedHerobrine;

    private UUID firstSummonedHerobrineUUID;
    private UUID secondSummonedHerobrineUUID;
    private UUID thirdSummonedHerobrineUUID;
    private final Entity[] combatLowCloneSupport = new Entity[MAX_COMBAT_LOW_CLONE_SUPPORT];
    private final UUID[] combatLowCloneSupportUUIDs = new UUID[MAX_COMBAT_LOW_CLONE_SUPPORT];

    private BlockPos lastFeetPos = null;
    private String chatName;

    public static final List<Item> listWeapons = List.of(
            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            AnnoyingVillagersModItems.DIAMOND_ATTRACTOR_SWORD.get(),
            AnnoyingVillagersModItems.DIAMOND_BLASTER_SWORD.get(),
            AnnoyingVillagersModItems.HOOKED_DIAMOND_SWORD.get(),
            AnnoyingVillagersModItems.DIAMOND_WARBLADE.get(),
            AnnoyingVillagersModItems.DIAMOND_FALCHION.get(),
            AnnoyingVillagersModItems.DIAMOND_GREAT_FALCHION.get(),
            AnnoyingVillagersModItems.DIAMOND_SABRE.get(),
            AnnoyingVillagersModItems.DIAMOND_LONGSWORD.get(),
            AnnoyingVillagersModItems.DIAMOND_CHIPPED_LONGSWORD.get(),
            AnnoyingVillagersModItems.PALADIN_SWORD.get(),
            AnnoyingVillagersModItems.DIAMOND_GREATAXE.get(),
            AnnoyingVillagersModItems.DIAMOND_ARMBLADE.get(),
            AnnoyingVillagersModItems.DIAMOND_SICKLE.get(),
            AnnoyingVillagersModItems.DOUBLE_DIAMOND_GLAIVE.get(),
            AnnoyingVillagersModItems.DIAMOND_MOON_BLADE.get()
    );

    public void setWhiteEye(boolean whiteEye) {
        this.entityData.set(WHITE_EYE, whiteEye);
    }

    public boolean isWhiteEye() {
        return this.entityData.get(WHITE_EYE);
    }

    public void setUseHerobrineTexture(boolean useHerobrineTexture) {
        this.entityData.set(USE_HEROBRINE_TEXTURE, useHerobrineTexture);
    }

    public int getEscapeTiming() {
        return escapeTiming;
    }

    public void setEscapeTiming(int escapeTiming) {
        this.escapeTiming = escapeTiming;
    }

    public boolean isUseHerobrineTexture() { return this.entityData.get(USE_HEROBRINE_TEXTURE); }

    public void markSupportingHerobrine() {
        this.supportingHerobrineVisualTicks = 40;
        this.setSupportingHerobrine(true);
    }

    public boolean isSupportingHerobrine() {
        return this.entityData.get(SUPPORTING_HEROBRINE);
    }

    private void setSupportingHerobrine(boolean supportingHerobrine) {
        this.entityData.set(SUPPORTING_HEROBRINE, supportingHerobrine);
    }

    public boolean isHooked() {
        return this.entityData.get(HOOKED);
    }

    private void setHooked(boolean hooked) {
        this.entityData.set(HOOKED, hooked);
        if (!hooked) {
            this.hookedWaitingForGround = false;
            this.hookedLeftGround = false;
        }
        if (hooked && !this.level().isClientSide() && !this.hookedWaitingForGround) {
            this.enforceHookedNoAiLock();
        }
    }

    private void releaseHookedPhysicsUntilGround() {
        this.hookedWaitingForGround = true;
        this.hookedLeftGround = this.hookedLeftGround || !this.onGround();
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setNoAi(false);
        this.setInvulnerable(false);
        this.getNavigation().stop();
    }

    private void tickHookedGroundRelock() {
        if (!this.hookedWaitingForGround) {
            this.enforceHookedNoAiLock();
            return;
        }

        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInvulnerable(false);
        if (!this.onGround()) {
            this.hookedLeftGround = true;
        }
        if (this.hookedLeftGround && this.onGround()) {
            this.hookedWaitingForGround = false;
            this.hookedLeftGround = false;
            this.enforceHookedNoAiLock();
        }
    }

    private void enforceHookedNoAiLock() {
        this.setNoAi(true);
        this.getNavigation().stop();
        this.setSprinting(false);
        this.setTarget(null);
        this.xxa = 0.0F;
        this.yya = 0.0F;
        this.zza = 0.0F;
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, deltaMovement.y, 0.0D);
    }


    private int randomCooldownSeconds(int minSeconds, int maxSeconds) {
        return minSeconds * 20 + this.random.nextInt((maxSeconds - minSeconds) * 20 + 1);
    }

    @Override
    public int getLowCloneSupportCooldown() {
        return this.lowCloneSupportCooldown;
    }

    @Override
    public void setLowCloneSupportCooldown() {
        this.lowCloneSupportCooldown = randomCooldownSeconds(90, 180);
    }

    @Override
    public int getPortalActionCooldown() {
        return this.portalActionCooldown;
    }

    @Override
    public void setPortalActionCooldown() {
        this.portalActionCooldown = randomCooldownSeconds(20, 40);
    }

    private void tickCombatActionCooldowns() {
        if (this.lowCloneSupportCooldown > 0) this.lowCloneSupportCooldown--;
        if (this.portalActionCooldown > 0) this.portalActionCooldown--;
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WHITE_EYE, false);
        this.entityData.define(USE_HEROBRINE_TEXTURE, false);
        this.entityData.define(SUPPORTING_HEROBRINE, false);
        this.entityData.define(HOOKED, false);
    }

    public boolean isSummoning() {
        return summoning;
    }

    @Override
    public boolean canUseSupportPortalAction() {
        return !this.summoning
                && this.escapeTiming < 0
                && this.summonTiming < 0
                && !this.isNoAi();
    }

    public boolean canSummonLowCloneSupport() {
        return !this.summoning
                && this.escapeTiming < 0
                && this.summonTiming < 0
                && !this.isNoAi()
                && this.onGround()
                && this.lowCloneSupportCooldown <= 0
                && this.hasAvailableCombatLowCloneSupportSlot();
    }


    @Override
    public Mob getPortalSupportMob() {
        return this;
    }

    @Override
    public boolean canSupportPortalAlly(LivingEntity ally) {
        return isGregFollowSupportTarget(ally);
    }

    @Override
    public void markPortalSupport() {
        this.markSupportingHerobrine();
    }

    @Override
    public void playPortalSupportAnimation(RigAnimationId animationId, @Nullable LivingEntity lookTarget) {
//      ADD THIS CODE IN AV_EFM
//        PORTAL_SUMMON -> AnimsSculkSteve.PORTAL_SUMMON
//        POINT_LEFT_HAND_TOWARD -> AnimsEpicFightIronSpell.CASTING_ONE_HAND_TOP

        if (lookTarget != null && lookTarget.isAlive()) this.getLookControl().setLookAt(lookTarget, 30.0F, 30.0F);
        RigAnimationController.play(this, RigAnimationSpecs.get(animationId), lookTarget);
    }

    @Override
    public RigAnimationId getDangerousReactionAnimation(Mob mob) {
        return RigAnimationId.STEP_BACKWARD;
    }

    private static boolean isGregFollowSupportTarget(LivingEntity entity) {
        return entity instanceof HerobrineMob
                && !(entity instanceof TransporterHerobrineCloneEntity)
                && !(entity instanceof LowHerobrineCloneEntity)
                && !(entity instanceof LowShadowHerobrineCloneEntity);
    }

    @Nullable
    public LivingEntity findGregFollowSupportHerobrine() {
        for (LivingEntity support : HerobrineUtil.findSupportHerobrines(this, FOLLOW_SUPPORT_SEARCH_RADIUS)) {
            if (support.isAlive()
                    && isGregFollowSupportTarget(support)
                    && !(support.isPassenger() && support.getVehicle() instanceof HerobrineDragonEntity)) {
                return support;
            }
        }
        return null;
    }

    public boolean isSupportingSecondFormCaster(LivingEntity support) {
        if (!(support instanceof HerobrineMob herobrineMob) || !support.isAlive()) {
            return false;
        }
        UUID supportGregUUID = herobrineMob.getGregUUID();
        boolean assignedSupport = supportGregUUID != null && supportGregUUID.equals(this.getUUID());
        return (assignedSupport || this.isSupportingHerobrine()) && this.distanceToSqr(support) <= SECOND_FORM_SUPPORT_SEARCH_RADIUS_SQR;
    }

    public void playSecondFormSupportCast(LivingEntity support) {
        this.markSupportingHerobrine();
        this.playPortalSupportAnimation(RigAnimationId.POINT_LEFT_HAND_TOWARD, support);
    }

    public boolean canFishingHookCancelEscape() {
        return this.escapeTiming >= 0 || this.getPersistentData().getBoolean(HerobrinePortalUtil.NBT_SINKING);
    }

    public boolean tryFishingHookCancelEscape() {
        if (!this.canFishingHookCancelEscape()) {
            return false;
        }
        if (this.random.nextFloat() >= FISHING_HOOK_ESCAPE_CANCEL_CHANCE) {
            return false;
        }

        this.escapeTiming = -1;
        this.summonTiming = -2;
        this.summoning = false;
        this.combatMode = false;
        this.recallTime = -1;
        this.fishingHookCancelledEscape = true;
        this.hookedWaitingForGround = true;
        this.hookedLeftGround = !this.onGround();
        this.setHooked(true);
        HerobrinePortalUtil.cancelSinkTransition(this);
        cancelPortalSummonAnimation();
        this.restoreGregHookedEscapeAppearance();
        this.releaseHookedPhysicsUntilGround();
        this.getNavigation().stop();
        return true;
    }

    private void cancelPortalSummonAnimation() {
        // Vanilla portal feedback is instant, so there is no long-running animation state to cancel.
    }

    private void restoreGregHookedEscapeAppearance() {
        this.setUseHerobrineTexture(false);
        this.setWhiteEye(true);
        this.setItemSlot(EquipmentSlot.HEAD, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_HELMET.get())));
        this.setItemSlot(EquipmentSlot.CHEST, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())));
        this.setItemSlot(EquipmentSlot.LEGS, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_LEGGINGS.get())));
        this.setItemSlot(EquipmentSlot.FEET, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_BOOTS.get())));
    }

    private void playSecondFormSupportCastAnimation() {
        if (!this.level().isClientSide()) {
            this.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), 1.0F, 1.0F);
            this.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    public void setSummoning(boolean summoning) {
        this.summoning = summoning;
    }

    public int getSummonTimestamp() {
        return summonTimestamp;
    }

    public HerobrineGregEntity(SpawnEntity spawnentity, Level level) {
        this(AnnoyingVillagersModEntities.HEROBRINE_GREG.get(), level);
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public HerobrineGregEntity(EntityType<HerobrineGregEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(2.5F);
        this.xpReward = 50;
        this.setNoAi(false);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("Greg"));
        this.setChatName(this.getDisplayName().getString());
        this.setCustomNameVisible(true);
        this.portalActionCooldown = randomCooldownSeconds(20, 40);
        this.lowCloneSupportCooldown = randomCooldownSeconds(90, 180);

        int min = AnnoyingVillagersConfig.HEROBRINE_RECALL_MIN_TIME.get();
        int max = AnnoyingVillagersConfig.HEROBRINE_RECALL_MAX_TIME.get();
        int randomMin = Math.min(min, max);
        int randomMax = Math.max(min, max);
        this.recallTime = (randomMin + new Random().nextInt(randomMax - randomMin + 1)) * 60 * 20;
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void registerGoals() {
        super.registerGoals();
        CommonGoals.registerDangerousReactionGoals(this);
        this.goalSelector.addGoal(-6, new HerobrineGregSixPortalSupportGoal(this));
        this.goalSelector.addGoal(-5, new HerobrineSupportEscapePortalGoal(this));
        this.goalSelector.addGoal(-4, new HerobrineProjectileCounterPortalGoal(this));
        this.goalSelector.addGoal(-3, new HerobrineSupportApproachPortalGoal(this));
        this.goalSelector.addGoal(-2, new HerobrineLowCloneSupportGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, target -> target != null && HerobrineUtil.isEnemyOf(this, target)));
        this.goalSelector.addGoal(0, new Goal() {
            private LivingEntity support;
            private Vec3 standPosition;
            private int repathCooldown;

            {
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                if (!canMoveForSupport()) {
                    return false;
                }

                this.support = findGregFollowSupportHerobrine();
                return isValidSupport(this.support);
            }

            @Override
            public boolean canContinueToUse() {
                return canMoveForSupport()
                        && isValidSupport(this.support)
                        && distanceToSqr(this.support) <= FOLLOW_SUPPORT_LEASH_RADIUS_SQR;
            }

            @Override
            public void tick() {
                if (!isValidSupport(this.support)) {
                    return;
                }

                markSupportingHerobrine();
                getLookControl().setLookAt(this.support, 30.0F, 30.0F);
                double distanceSqr = distanceToSqr(this.support);
                LivingEntity threat = findNearestSupportThreat(this.support);
                if (threat != null && threat.isAlive()) {
                    setTarget(threat);
                    getLookControl().setLookAt(threat, 30.0F, 30.0F);
                }
                boolean currentSpotSafe = isCurrentSupportSpotSafe(this.support, threat);
                double maxStandDistanceSqr = threat == null ? (10.0D * 10.0D) : (18.0D * 18.0D);
                if (distanceSqr <= maxStandDistanceSqr && currentSpotSafe) {
                    getNavigation().stop();
                    this.repathCooldown = 30;
                    return;
                }
                if (this.repathCooldown-- <= 0 || getNavigation().isDone() || hasReachedStandPosition()) {
                    moveToSupportStandPosition(this.support, threat);
                }
            }

            private boolean canMoveForSupport() {
                return !summoning && escapeTiming < 0 && summonTiming < 0 && !isNoAi();
            }

            private boolean isValidSupport(@Nullable LivingEntity entity) {
                return entity != null
                        && entity.isAlive()
                        && isGregFollowSupportTarget(entity)
                        && !(entity.isPassenger() && entity.getVehicle() instanceof HerobrineDragonEntity);
            }

            @Nullable
            private LivingEntity findNearestSupportThreat(LivingEntity support) {
                LivingEntity threat = HerobrineUtil.findThreateningEnemy(HerobrineGregEntity.this, support, 24.0D);
                if (threat != null) {
                    return threat;
                }
                return HerobrineUtil.findEnemyForSupport(support, getTarget(), 24.0D);
            }

            private boolean isCurrentSupportSpotSafe(LivingEntity support, @Nullable LivingEntity threat) {
                double maxSupportDistanceSqr = threat == null ? (10.0D * 10.0D) : (18.0D * 18.0D);
                if (distanceToSqr(support) > maxSupportDistanceSqr) {
                    return false;
                }
                return threat == null || distanceToSqr(threat) >= (12.0D * 12.0D);
            }

            private boolean hasReachedStandPosition() {
                return this.standPosition != null
                        && position().distanceToSqr(this.standPosition) <= (2.0D * 2.0D);
            }

            private void moveToSupportStandPosition(LivingEntity support, @Nullable LivingEntity threat) {
                this.standPosition = findSupportStandPosition(support, threat);
                getNavigation().moveTo(this.standPosition.x, this.standPosition.y, this.standPosition.z,
                        threat == null ? 1.15D : 1.25D);
                this.repathCooldown = threat == null
                        ? 30 + random.nextInt(10)
                        : 8 + random.nextInt(5);
            }

            private Vec3 findSupportStandPosition(LivingEntity support, @Nullable LivingEntity threat) {
                double baseAngle = Math.atan2(getZ() - support.getZ(), getX() - support.getX());
                if (Double.isNaN(baseAngle)) {
                    baseAngle = random.nextDouble() * Math.PI * 2.0D;
                }
                double standRadius = threat == null ? 7.0D : 15.0D;

                if (threat == null) {
                    Vec3 currentSidePosition = standPositionAt(support, baseAngle, standRadius);
                    if (currentSidePosition != null) {
                        return currentSidePosition;
                    }
                    return new Vec3(support.getX(), support.getY(), support.getZ());
                }

                double awayFromThreatAngle = Math.atan2(support.getZ() - threat.getZ(), support.getX() - threat.getX());
                if (Double.isNaN(awayFromThreatAngle)) {
                    awayFromThreatAngle = baseAngle;
                }

                Vec3 bestPosition = null;
                double bestScore = Double.NEGATIVE_INFINITY;
                for (int sample = 0; sample < 16; sample++) {
                    double angle = awayFromThreatAngle + (Math.PI * 2.0D * sample / 16);
                    Vec3 candidate = standPositionAt(support, angle, standRadius);
                    if (candidate == null) {
                        continue;
                    }

                    double threatDistanceSqr = candidate.distanceToSqr(threat.position());
                    double movePenalty = candidate.distanceToSqr(position()) * 0.08D;
                    double score = threatDistanceSqr - movePenalty;
                    if (threatDistanceSqr < (12.0D * 12.0D)) {
                        score -= 300.0D;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestPosition = candidate;
                    }
                }

                if (bestPosition != null) {
                    return bestPosition;
                }
                Vec3 fallback = standPositionAt(support, awayFromThreatAngle, standRadius);
                return fallback != null ? fallback : new Vec3(support.getX(), support.getY(), support.getZ());
            }

            @Nullable
            private Vec3 standPositionAt(LivingEntity support, double angle, double radius) {
                double x = support.getX() + Math.cos(angle) * radius;
                double z = support.getZ() + Math.sin(angle) * radius;
                double y = support.getY();
                if (level() instanceof ServerLevel serverLevel) {
                    y = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, y, z)).getY();
                }

                Vec3 candidate = new Vec3(x, y, z);
                AABB movedBox = getBoundingBox().move(candidate.subtract(position()));
                return level().noCollision(HerobrineGregEntity.this, movedBox) ? candidate : null;
            }
        });
        this.goalSelector.addGoal(1, new Goal() {
            private LivingEntity threat;

            {
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                if (!canIdleAvoid()) {
                    return false;
                }

                this.threat = findIdleThreat();
                return this.threat != null
                        && this.threat.isAlive()
                        && distanceToSqr(this.threat) <= (18.0D * 18.0D);
            }

            @Override
            public boolean canContinueToUse() {
                return canIdleAvoid()
                        && this.threat != null
                        && this.threat.isAlive()
                        && distanceToSqr(this.threat) <= 32.0D * 32.0D;
            }

            @Override
            public void tick() {
                if (idleAvoidRepathCooldown > 0) {
                    idleAvoidRepathCooldown--;
                }

                this.threat = findIdleThreat();
                setTarget(null);
                if (this.threat == null || !this.threat.isAlive()) {
                    return;
                }

                getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
                if (distanceToSqr(this.threat) > (18.0D * 18.0D)) {
                    return;
                }
                if (idleAvoidRepathCooldown > 0 && !getNavigation().isDone()) {
                    return;
                }

                Vec3 retreatPos = findIdleRetreatPosition(this.threat);
                if (retreatPos == null) {
                    return;
                }

                getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.15D);
                idleAvoidRepathCooldown = 15;
            }

            private boolean canIdleAvoid() {
                return !combatMode
                        && !summoning
                        && escapeTiming < 0
                        && summonTiming < 0
                        && !isNoAi()
                        && !isSupportingHerobrine()
                        && findGregFollowSupportHerobrine() == null;
            }

            @Nullable
            private LivingEntity findIdleThreat() {
                LivingEntity threat = HerobrineUtil.findThreateningEnemy(HerobrineGregEntity.this, null, 32.0D);
                if (threat != null) {
                    return threat;
                }
                return HerobrineUtil.findEnemyForSupport(HerobrineGregEntity.this, null, 32.0D);
            }

            @Nullable
            private Vec3 findIdleRetreatPosition(LivingEntity threat) {
                if (!(level() instanceof ServerLevel serverLevel)) {
                    return null;
                }

                double awayAngle = Math.atan2(getZ() - threat.getZ(), getX() - threat.getX());
                if (Double.isNaN(awayAngle)) {
                    awayAngle = getRandom().nextDouble() * Math.PI * 2.0D;
                }

                for (int attempt = 0; attempt < 10; attempt++) {
                    double angle = awayAngle + (getRandom().nextDouble() - 0.5D) * 1.4D;
                    double distance = 12.0D
                            + getRandom().nextDouble() * (20.0D - 12.0D);
                    double x = getX() + Math.cos(angle) * distance;
                    double z = getZ() + Math.sin(angle) * distance;
                    BlockPos surface = serverLevel.getHeightmapPos(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            BlockPos.containing(x, getY(), z)
                    );

                    if (!serverLevel.isLoaded(surface) || !serverLevel.getWorldBorder().isWithinBounds(surface)) {
                        continue;
                    }
                    if (!serverLevel.isEmptyBlock(surface)
                            || !serverLevel.isEmptyBlock(surface.above())
                            || serverLevel.isEmptyBlock(surface.below())) {
                        continue;
                    }

                    Vec3 candidate = new Vec3(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D);
                    if (candidate.distanceToSqr(threat.position()) <= position().distanceToSqr(threat.position())) {
                        continue;
                    }
                    if (!serverLevel.noCollision(HerobrineGregEntity.this,
                            getBoundingBox().move(candidate.subtract(position())).deflate(1.0E-4D))) {
                        continue;
                    }
                    return candidate;
                }

                return null;
            }
        });
        this.goalSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                return firstSummonedHerobrine != null
                        && firstSummonedHerobrine.isAlive()
                        && distanceTo(firstSummonedHerobrine) > 8.0D;
            }

            @Override
            public void tick() {
                if (firstSummonedHerobrine != null && firstSummonedHerobrine.isAlive()) {
                    getNavigation().moveTo(firstSummonedHerobrine, 2.0D);
                    getLookControl().setLookAt(firstSummonedHerobrine, 30.0F, 30.0F);
                    if (distanceToSqr(firstSummonedHerobrine) > 25.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(firstSummonedHerobrine, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return firstSummonedHerobrine != null
                        && firstSummonedHerobrine.isAlive()
                        && distanceTo(firstSummonedHerobrine) > 5.0D;
            }
        });
        this.goalSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                return secondSummonedHerobrine != null
                        && secondSummonedHerobrine.isAlive()
                        && distanceTo(secondSummonedHerobrine) > 8.0D;
            }

            @Override
            public void tick() {
                if (secondSummonedHerobrine != null && secondSummonedHerobrine.isAlive()) {
                    getNavigation().moveTo(secondSummonedHerobrine, 2.0D);
                    getLookControl().setLookAt(secondSummonedHerobrine, 30.0F, 30.0F);
                    if (distanceToSqr(secondSummonedHerobrine) > 25.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(secondSummonedHerobrine, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return secondSummonedHerobrine != null
                        && secondSummonedHerobrine.isAlive()
                        && distanceTo(secondSummonedHerobrine) > 5.0D;
            }
        });
        this.goalSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                return thirdSummonedHerobrine != null
                        && thirdSummonedHerobrine.isAlive()
                        && distanceTo(thirdSummonedHerobrine) > 8.0D;
            }

            @Override
            public void tick() {
                if (thirdSummonedHerobrine != null && thirdSummonedHerobrine.isAlive()) {
                    getNavigation().moveTo(thirdSummonedHerobrine, 2.0D);
                    getLookControl().setLookAt(thirdSummonedHerobrine, 30.0F, 30.0F);
                    if (distanceToSqr(thirdSummonedHerobrine) > 25.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(thirdSummonedHerobrine, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return thirdSummonedHerobrine != null
                        && thirdSummonedHerobrine.isAlive()
                        && distanceTo(thirdSummonedHerobrine) > 5.0D;
            }
        });
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, VillagerScoutEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, VillagerScoutCaptainEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BlueVillagerKnightEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, GreenVillagerKnightEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RedVillagerKnightEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, PurpleVillagerKnightEntity.class, 12.0F, 1.0D, 1.35D));
        if (ModList.get().isLoaded("smart_npc")) {
            SmartNpc.gregAvoid(this);
        }
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, SteveEntity.class, 24.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, AlexEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, JevEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, ChrisEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BlueDemonEntity.class, 12.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BbqEntity.class, 12.0F, 1.0D, 1.35D));

        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
    }

    public @NotNull MobType getMobType() {
        return MobType.UNDEFINED;
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new HerobrineMob.AnyFluidPathNavigation(this, level);
    }

    @Override
    public boolean isInWater() {
        FluidState fs = this.level().getFluidState(this.blockPosition());
        if (!fs.isEmpty() && this.canStandOnFluid(fs)) return false;
        return super.isInWater();
    }

    @Override
    public boolean canStandOnFluid(FluidState state) {
        return !state.isEmpty();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    private void tickSupportingHerobrineVisuals() {
        if (this.supportingHerobrineVisualTicks > 0) {
            this.supportingHerobrineVisualTicks--;
        }

        boolean supportingAppearance = this.supportingHerobrineVisualTicks > 0 || this.hasNearbyHerobrineToSupport();
        if (!supportingAppearance) {
            if (this.isSupportingHerobrine()) {
                this.setSupportingHerobrine(false);
            }
            return;
        }

        if (!this.isSupportingHerobrine()) {
            this.setSupportingHerobrine(true);
        }
        if (!this.isWhiteEye()) {
            this.setWhiteEye(true);
        }
        if (!this.getItemBySlot(EquipmentSlot.CHEST).is(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get()));
        }
    }

    private boolean hasNearbyHerobrineToSupport() {
        LivingEntity support = this.findGregFollowSupportHerobrine();
        return support != null
                && support.isAlive()
                && !(support.isPassenger() && support.getVehicle() instanceof HerobrineDragonEntity);
    }

    private void assignProtect(Entity entity, UUID protectUUID, EliteHerobrineKnockedEntity protectEntity) {
        if (entity != null && entity.isAlive()) {
            if (entity instanceof HerobrineMob herobrineMob) {
                herobrineMob.setProtectUUID(protectUUID);
                herobrineMob.setProtectEntity(protectEntity);
            } else if (entity instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                lowShadowHerobrineCloneEntity.setProtectUUID(protectUUID);
                lowShadowHerobrineCloneEntity.setProtectEntity(protectEntity);
            }
        }
    }

    public void requestProtect(UUID protectUUID, EliteHerobrineKnockedEntity protectEntity) {
        assignProtect(firstSummonedHerobrine, protectUUID, protectEntity);
        assignProtect(secondSummonedHerobrine, protectUUID, protectEntity);
        assignProtect(thirdSummonedHerobrine, protectUUID, protectEntity);
    }

    private static boolean isRidingHerobrineDragon(Entity entity) {
        return entity.isPassenger() && entity.getVehicle() instanceof HerobrineDragonEntity;
    }

    private void floatOnAnyFluid() {
        BlockPos pos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(pos);
        if (fluidState.isEmpty()) {
            return;
        }

        CollisionContext collisionContext = CollisionContext.of(this);
        Fluid typeHere = fluidState.getType();
        FluidState above = this.level().getFluidState(pos.above());

        if (collisionContext.isAbove(LiquidBlock.STABLE_SHAPE, pos, true) && above.getType() != typeHere) {
            this.setOnGround(true);

            double surfaceY = pos.getY() + fluidState.getHeight(this.level(), pos);
            double bottomY  = this.getBoundingBox().minY;
            double diff     = surfaceY - bottomY - 0.001D;

            if (diff > 0.0D) {
                Vec3 vel = this.getDeltaMovement();
                this.setDeltaMovement(vel.x, Math.max(vel.y, Math.min(0.2D, diff * 0.2D)), vel.z);
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(0.0D, 0.05D, 0.0D));
        }

        this.fallDistance = 0.0F;
    }

    private void placeObsidianBlockWhenInWater(Block block) {
        BlockPos feet = this.getOnPos();
        if (lastFeetPos == null) lastFeetPos = feet;
        if (!feet.equals(lastFeetPos)) {
            if (!this.level().getBlockState(lastFeetPos).is(block)) {
                FluidState fluidState = this.level().getFluidState(lastFeetPos);
                if (!fluidState.isEmpty()) {
                    int replace = fluidState.isSource()
                            ? (fluidState.is(FluidTags.WATER) ? 1 : (fluidState.is(FluidTags.LAVA) ? 2 : 0))
                            : 0;
                    BlockState state = block.defaultBlockState().setValue(HerobrineObsidianBlock.REPLACE_BY_LIQUID, replace);
                    this.level().setBlockAndUpdate(
                            lastFeetPos,
                            state
                    );
                    BlockEntity blockEntity = this.level().getBlockEntity(lastFeetPos);
                    if (blockEntity instanceof ObsidianBlockEntity obsidianBlockEntity) {
                        obsidianBlockEntity.setOwner(this.getUUID());
                        obsidianBlockEntity.setChanged();
                        this.level().sendBlockUpdated(lastFeetPos, state, state, 3);
                    }
                    if (blockEntity instanceof ShadowObsidianBlockEntity shadowObsidianBlockEntity) {
                        shadowObsidianBlockEntity.setOwner(this.getUUID());
                        shadowObsidianBlockEntity.setChanged();
                        this.level().sendBlockUpdated(lastFeetPos, state, state, 3);
                    }
                    if (blockEntity instanceof CryingObsidianBlockEntity cryingObsidianBlockEntity) {
                        cryingObsidianBlockEntity.setOwner(this.getUUID());
                        cryingObsidianBlockEntity.setChanged();
                        this.level().sendBlockUpdated(lastFeetPos, state, state, 3);
                    }
                }
            }
            lastFeetPos = feet;
        }
    }


    @Override
    public void tick() {
        if (!this.level().isClientSide && this.isHooked() && !this.hookedWaitingForGround) {
            this.enforceHookedNoAiLock();
        }
        super.tick();
        this.floatOnAnyFluid();
        this.checkInsideBlocks();
        if (!this.level().isClientSide) {
            this.tickCombatLowCloneSupportSlots();
            this.tickCombatActionCooldowns();
            if (this.isHooked()) {
                this.tickHookedGroundRelock();
                return;
            }
        }
        if (!this.level().isClientSide) {
            placeObsidianBlockWhenInWater(AnnoyingVillagersModBlocks.CRYING_OBSIDIAN_BLOCK.get());
            tickSupportingHerobrineVisuals();
            if (!isDay(this.level())) {
                if (!this.isWhiteEye()) {
                    setWhiteEye(true);
                }
                if (!this.getItemBySlot(EquipmentSlot.CHEST).getItem().equals(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())) {
                    this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get()));
                }
            } else {
                if (this.isWhiteEye()
                        && this.summonTiming == -1
                        && !this.isSupportingHerobrine()
                        && !this.isUseHerobrineTexture()) {
                    setWhiteEye(false);
                }
                if (!this.combatMode
                        && !this.isSupportingHerobrine()
                        && !this.isUseHerobrineTexture()
                        && this.getItemBySlot(EquipmentSlot.CHEST).getItem().equals(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())) {
                    this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                }
            }
            if (this.level().getDayTime() % 24000L == 13001 && this.summonTimestamp == -1) {
                if (new Random().nextBoolean()) {
                    Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal("<" + this.getChatName() + "> " +
                            Component.translatable("subtitles.herobrine_prepare_for_fight").getString()), false);
                    this.summonTimestamp = new Random().nextInt(13100, 22200);
                    AnnoyingVillagers.LOGGER.info("[AV MOD DEBUG]: Greg will summon elites at {}", this.summonTimestamp);
                } else {
                    Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal("<" + this.getChatName() + "> " +
                            Component.translatable("subtitles.herobrine_no_fight").getString()), false);
                }
            }

            if (this.level().getDayTime() % 24000L == this.summonTimestamp) {
                this.summonTimestamp = -2; // Greg will never summon again
                this.combatMode = true;
                this.setNoAi(true);
                this.setInvulnerable(true);
                this.summoning = true;
                this.summonTiming = 20;
                CommonUtil.stunImmunity(this, 120, 3);
            }

            if (this.getHealth() <= 2 && this.summonTiming == -1) {
                if (!isDay(this.level())) {
                    this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get()));
                }
                setWhiteEye(true);
                this.setNoAi(true);
                this.setInvulnerable(true);
                this.summoning = true;
                this.summonTiming = 20;
                this.setHealth(1);
                CommonUtil.stunImmunity(this, 120, 3);
            }

            if (this.summonTiming > 0) {
                this.summonTiming = this.summonTiming - 1;
            }
            if (this.summonTiming == 10) {
                this.playSound(AnnoyingVillagersModSounds.PORTAL_SUMMON.get(), 1.0F, 1.0F);
                Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal("<" + this.getChatName() + "> " +
                        Component.translatable("subtitles.herobrine_summon").getString()), false);
            }
            if (this.summonTiming == 1) {
                if (this.combatMode) {
                    summonHerobrines();
                } else {
                    summonHerobrinesAndEscape();
                }
            }

            if (this.escapeTiming > 0) {
                this.escapeTiming = this.escapeTiming - 1;
            }
            if (this.escapeTiming == 60 && this.combatMode) {
                this.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get());
                playSecondFormSupportCastAnimation();
                AnnoyingVillagers.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY.with(() -> this),
                        new ClientboundHerobrinePortalFx(this.getOnPos().getCenter().add(0.0, 0.5, 0.0))
                );
            }
            if (this.escapeTiming == 40) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    HerobrinePortalUtil.sinkIntoGround(serverLevel, this, 0.06);
                }
            }
            if (this.escapeTiming == 1) {
                this.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("<" + this.getChatName() + "> " +
                        Component.translatable("subtitles.herobrine_will_be_back").getString()), false);
                if (this.firstSummonedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                    lowShadowHerobrineCloneEntity.setAutoKill(true);
                    lowShadowHerobrineCloneEntity.kill();
                }
                if (this.secondSummonedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                    lowShadowHerobrineCloneEntity.setAutoKill(true);
                    lowShadowHerobrineCloneEntity.kill();
                }
                if (this.thirdSummonedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                    lowShadowHerobrineCloneEntity.setAutoKill(true);
                    lowShadowHerobrineCloneEntity.kill();
                }
                this.discard();
            }

            if (firstSummonedHerobrine == null && firstSummonedHerobrineUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(firstSummonedHerobrineUUID);
                if (entity instanceof HerobrineMob || entity instanceof LowShadowHerobrineCloneEntity) {
                    firstSummonedHerobrine = entity;
                } else {
                    firstSummonedHerobrineUUID = null;
                }
            }
            if (firstSummonedHerobrine != null && !firstSummonedHerobrine.isAlive()) {
                // handle logic when a summoned die
                firstSummonedHerobrineUUID = null;
            }

            if (secondSummonedHerobrine == null && secondSummonedHerobrineUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(secondSummonedHerobrineUUID);
                if (entity instanceof HerobrineMob || entity instanceof LowShadowHerobrineCloneEntity) {
                    secondSummonedHerobrine = entity;
                } else {
                    secondSummonedHerobrineUUID = null;
                }
            }
            if (secondSummonedHerobrine != null && !secondSummonedHerobrine.isAlive()) {
                // handle logic when a summoned die
                secondSummonedHerobrineUUID = null;
            }

            if (thirdSummonedHerobrine == null && thirdSummonedHerobrineUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(thirdSummonedHerobrineUUID);
                if (entity instanceof HerobrineMob || entity instanceof LowShadowHerobrineCloneEntity) {
                    thirdSummonedHerobrine = entity;
                } else {
                    thirdSummonedHerobrineUUID = null;
                }
            }
            if (thirdSummonedHerobrine != null && !thirdSummonedHerobrine.isAlive()) {
                // handle logic when a summoned die
                thirdSummonedHerobrineUUID = null;
            }

            if (this.combatMode && this.escapeTiming == -1 && this.summonTiming == -2
                    && !this.fishingHookCancelledEscape
                    && this.firstSummonedHerobrineUUID == null
                    && this.secondSummonedHerobrineUUID == null
                    && this.thirdSummonedHerobrineUUID == null) {
                this.escapeTiming = 80;
                this.setNoAi(true);
            }

            if (this.combatMode && !this.fishingHookCancelledEscape && this.escapeTiming == -1 && this.recallTime >= 0) {
                this.recallTime = this.recallTime - 1;
                if (this.recallTime == 20) {
                    this.setNoAi(true);
                }
                if (this.recallTime <= 0) {
                    this.escapeTiming = 61;
                }
            }

            if (this.combatMode) {
                CommonUtil.stunImmunity(this, 3, 3);
            }
        }
    }

    private void tickCombatLowCloneSupportSlots() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int i = 0; i < MAX_COMBAT_LOW_CLONE_SUPPORT; i++) {
            Entity tracked = this.combatLowCloneSupport[i];
            UUID trackedUuid = this.combatLowCloneSupportUUIDs[i];
            if (tracked == null && trackedUuid != null) {
                tracked = serverLevel.getEntity(trackedUuid);
                if (isTrackedCombatLowClone(tracked)) {
                    this.combatLowCloneSupport[i] = tracked;
                } else {
                    this.clearCombatLowCloneSupportSlot(i);
                    continue;
                }
            }

            if (tracked != null && (!tracked.isAlive() || tracked.isRemoved())) {
                this.clearCombatLowCloneSupportSlot(i);
            }
        }
    }

    private static boolean isTrackedCombatLowClone(@Nullable Entity entity) {
        return entity != null
                && entity.isAlive()
                && (entity instanceof LowHerobrineCloneEntity || entity instanceof LowShadowHerobrineCloneEntity);
    }

    private void clearCombatLowCloneSupportSlot(int index) {
        this.combatLowCloneSupport[index] = null;
        this.combatLowCloneSupportUUIDs[index] = null;
    }

    private int getAvailableCombatLowCloneSupportSlot() {
        for (int i = 0; i < MAX_COMBAT_LOW_CLONE_SUPPORT; i++) {
            if (this.combatLowCloneSupportUUIDs[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public int getAvailableCombatLowCloneSupportSlotCount() {
        int count = 0;
        for (UUID uuid : this.combatLowCloneSupportUUIDs) {
            if (uuid == null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasAvailableCombatLowCloneSupportSlot() {
        return this.getAvailableCombatLowCloneSupportSlot() >= 0;
    }

    public boolean claimCombatLowCloneSupportSlot(Entity clone) {
        int slot = this.getAvailableCombatLowCloneSupportSlot();
        if (slot < 0) {
            return false;
        }

        this.combatLowCloneSupport[slot] = clone;
        this.combatLowCloneSupportUUIDs[slot] = clone.getUUID();
        return true;
    }

    private void summonHerobrine(String herobrineMobId, double spawnX, double spawnY,
                                 double spawnZ, double summonLookX, double summonLookZ, boolean renderPortal) {
        if (this.level() instanceof ServerLevel levelaccessor) {
            String[] parts = herobrineMobId.split(":");
            ResourceLocation mobResourceLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(mobResourceLocation);
            if (type != null && type.create(level()) instanceof Mob herobrine) {
                if (herobrine instanceof HerobrineMob herobrineMob) {
                    herobrineMob.setGregUUID(this.getUUID());
                    herobrineMob.setRenderPortal(renderPortal);
                    herobrineMob.setRecallTicks(this.recallTime);
                } else if (herobrine instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity) {
                    lowHerobrineCloneEntity.setSummoned(true);
                    equipGearForLowHerobrineClone(lowHerobrineCloneEntity);
                } else if (herobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                    if (renderPortal) {
                        AnnoyingVillagers.PACKET_HANDLER.send(
                                PacketDistributor.TRACKING_ENTITY.with(() -> this),
                                new ClientboundHerobrinePortalFx(new Vec3(spawnX, spawnY, spawnZ))
                        );
                    } else {
                        equipGearForLowHerobrineClone(lowShadowHerobrineCloneEntity);
                    }
                    lowShadowHerobrineCloneEntity.setSummoned(true);
                }

                herobrine.moveTo(spawnX, spawnY, spawnZ, this.getYRot(), this.getXRot());
                herobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(summonLookX, spawnY, summonLookZ));
                herobrine.finalizeSpawn(levelaccessor, levelaccessor.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                levelaccessor.addFreshEntity(herobrine);

                if (this.combatMode) {
                    if (this.firstSummonedHerobrineUUID == null) {
                        this.firstSummonedHerobrineUUID = herobrine.getUUID();
                        this.firstSummonedHerobrine = herobrine;
                    } else if (this.secondSummonedHerobrineUUID == null) {
                        this.secondSummonedHerobrineUUID = herobrine.getUUID();
                        this.secondSummonedHerobrine = herobrine;
                    }  else {
                        this.thirdSummonedHerobrineUUID = herobrine.getUUID();
                        this.thirdSummonedHerobrine = herobrine;
                    }
                }
            }
        }
    }

    private void spawnHerobrineOffset(String id,
                                      double forwardDist, double lateralDist, double baseY,
                                      double fx, double fz, double lx, double lz) {
        double spawnX = this.getX() + fx * forwardDist + lx * lateralDist;
        double spawnZ = this.getZ() + fz * forwardDist + lz * lateralDist;

        double lookX = spawnX + fx * 10.0;
        double lookZ = spawnZ + fz * 10.0;

        summonHerobrine(id, spawnX, baseY, spawnZ, lookX, lookZ, false);
    }

    private void spawnRandomHerobrinesInRadius(String id, int count) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        int cx = Mth.floor(this.getX());
        int cz = Mth.floor(this.getZ());

        List<BlockPos> candidates = new ArrayList<>();
        int r2 = 20 * 20;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (dx * dx + dz * dz > r2) continue;
                int x = cx + dx;
                int z = cz + dz;

                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                candidates.add(new BlockPos(x, y, z));
            }
        }

        Collections.shuffle(candidates, new java.util.Random(this.getRandom().nextLong()));
        double yawRad = Math.toRadians(this.getYRot());
        double fx = -Math.sin(yawRad);
        double fz =  Math.cos(yawRad);

        int spawned = 0;
        for (BlockPos pos : candidates) {
            if (spawned >= count) break;

            if (!sl.isLoaded(pos)) continue;
            if (!sl.getWorldBorder().isWithinBounds(pos)) continue;

            if (!sl.isEmptyBlock(pos) || !sl.isEmptyBlock(pos.above())) continue;
            if (sl.isEmptyBlock(pos.below())) continue;

            double spawnX = pos.getX() + 0.5D;
            double spawnY = pos.getY();
            double spawnZ = pos.getZ() + 0.5D;

            double lookX = spawnX + fx * 10.0D;
            double lookZ = spawnZ + fz * 10.0D;

            summonHerobrine(id, spawnX, spawnY, spawnZ, lookX, lookZ, true);
            spawned++;
        }
    }

    private void summonEscapeAtDay() {
        this.escapeTiming = 70;
        AnnoyingVillagers.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new ClientboundHerobrinePortalFx(this.getOnPos().getCenter().add(0.0, 0.5, 0.0))
        );

        double yawRad = Math.toRadians(this.getYRot());

        double fx = -Math.sin(yawRad);
        double fz =  Math.cos(yawRad);
        double lx =  Math.cos(yawRad);
        double lz =  Math.sin(yawRad);

        double y = this.getY();
        double front = 1.0;
        double side  = 1.0;

        // Herobrine on the left side
        String leftHerobrine;
        if (Math.random() <= 0.5D) {
            leftHerobrine = "annoyingvillagers:low_herobrine_clone";
        } else {
            leftHerobrine = "annoyingvillagers:low_shadow_herobrine_clone";
        }
        spawnHerobrineOffset(leftHerobrine, 0.0, +side, y, fx, fz, lx, lz);

        String rightHerobrine;
        if (Math.random() <= 0.5D) {
            rightHerobrine = "annoyingvillagers:low_herobrine_clone";
        } else {
            rightHerobrine = "annoyingvillagers:low_shadow_herobrine_clone";
        }
        spawnHerobrineOffset(rightHerobrine, 0.0, -side, y, fx, fz, lx, lz);

        // 70% for 2 Herobrines, 30% for 3 Herobrines
        if (Math.random() >= 0.7D) {
            String frontHerobrine;
            if (Math.random() <= 0.5D) {
                frontHerobrine = "annoyingvillagers:low_herobrine_clone";
            } else {
                frontHerobrine = "annoyingvillagers:low_shadow_herobrine_clone";
            }

            spawnHerobrineOffset(frontHerobrine, front, 0.0, y, fx, fz, lx, lz);
        }
    }

    private void summonEscapeAtNight() {
        this.escapeTiming = 70;
        AnnoyingVillagers.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new ClientboundHerobrinePortalFx(this.getOnPos().getCenter().add(0.0, 0.5, 0.0))
        );

        List<String> herobrines = new ArrayList<>();
        herobrines.add("annoyingvillagers:herobrine_clone");
        herobrines.add("annoyingvillagers:shadow_herobrine_clone");
        herobrines.add("annoyingvillagers:herobrine_chris");
        herobrines.add("annoyingvillagers:herobrine_7");
        herobrines.add("annoyingvillagers:armored_herobrine");
        herobrines.add("annoyingvillagers:low_shadow_herobrine_clone");

        Random random = new Random();
        String herobrineId = herobrines.get(random.nextInt(herobrines.size()));

        // if low_shadow_herobrine_clone, spawn 10 to 20 low_shadow_herobrine_clone around
        if (herobrineId.equals("annoyingvillagers:low_shadow_herobrine_clone")) {
            spawnRandomHerobrinesInRadius(herobrineId, new Random().nextInt(10, 20));
        } else {
            double yawRad = Math.toRadians(this.getYRot());

            double fx = -Math.sin(yawRad);
            double fz =  Math.cos(yawRad);
            double lx =  Math.cos(yawRad);
            double lz =  Math.sin(yawRad);

            double y = this.getY();
            double front = 1.0;

            spawnHerobrineOffset(herobrineId, front, 0.0, y, fx, fz, lx, lz);
        }
    }

    private enum ElitePattern {
        SOLO_1E,
        ONEE_PLUS_1S,
        ONEE_PLUS_2S,
        TWO_E,
        TWOE_PLUS_1S,
        THREE_E
    }

    private ElitePattern pickWeightedElitePattern(Random random) {
        double roll = random.nextDouble();
        if (roll <= 0.1F) {
            return ElitePattern.THREE_E;
        } else if (roll <= 0.2F) {
            return ElitePattern.TWOE_PLUS_1S;
        } else if (roll <= 0.3F) {
            return ElitePattern.TWO_E;
        } else if (roll <= 0.4F) {
            return ElitePattern.ONEE_PLUS_2S;
        } else if (roll <= 0.5F) {
            return ElitePattern.ONEE_PLUS_1S;
        } else {
            return ElitePattern.SOLO_1E;
        }
    }

    private void clearSummonSpace(ServerLevel serverLevel) {
        BlockPos center = this.getOnPos();
        final int feetY = center.getY();

        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    BlockState state = serverLevel.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(serverLevel, pos) < 0.0F) continue;

                    serverLevel.destroyBlock(pos, true, this);
                }
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = new BlockPos(center.getX() + dx, feetY, center.getZ() + dz);

                if (serverLevel.getBlockState(pos).isAir()) {
                    serverLevel.setBlockAndUpdate(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                }
            }
        }
    }

    private static <T> T pickRandom(List<T> list, Random random) {
        return list.remove(random.nextInt(list.size()));
    }

    private void summonAtNight() {
        List<String> herobrines = new ArrayList<>();
        herobrines.add("annoyingvillagers:shadow_herobrine");
        herobrines.add("annoyingvillagers:elite");
        herobrines.add("annoyingvillagers:null");
        herobrines.add("annoyingvillagers:elite");

        List<String> elites = new ArrayList<>();
        elites.add("annoyingvillagers:swordsman_herobrine");
        elites.add("annoyingvillagers:aegis_herobrine");
        elites.add("annoyingvillagers:glaive_herobrine");
        elites.add("annoyingvillagers:reaper_herobrine");
        elites.add("annoyingvillagers:sledgehammer_herobrine");

        float yaw = this.getYRot();
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz =  Math.cos(rad);
        double lx =  Math.cos(rad);
        double lz =  Math.sin(rad);

        double baseY = this.getY();
        double centerForward = 3.0;
        double side = 1.0;
        double thirdForward = 4.0;

        double centerX = this.getX() + fx * centerForward;
        double centerZ = this.getZ() + fz * centerForward;
        double lookX   = centerX + fx * 10.0;
        double lookZ   = centerZ + fz * 10.0;

        this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(centerX, baseY, centerZ));

        if (!(this.level() instanceof ServerLevel)) return;
        AnnoyingVillagers.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new ClientboundHerobrinePortalFx(new Vec3(centerX, baseY, centerZ))
        );

        Random random = new Random();
        String pick = herobrines.get(random.nextInt(herobrines.size()));

        if (pick.equals("annoyingvillagers:shadow_herobrine") || pick.equals("annoyingvillagers:null")) {
            summonHerobrine(pick, centerX, baseY, centerZ, lookX, lookZ, false);
            return;
        }

        ElitePattern pattern = pickWeightedElitePattern(random);

        switch (pattern) {
            case SOLO_1E -> {
                summonHerobrine(pickRandom(elites, random), centerX, baseY, centerZ, lookX, lookZ, false);
            }
            case ONEE_PLUS_1S -> {
                // Two mobs: left + right around the portal; elite on left, shadow on right (arbitrary)
                // Left
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, +side, baseY, fx, fz, lx, lz);
                // Right
                spawnHerobrineOffset("annoyingvillagers:low_shadow_herobrine_clone", centerForward, -side, baseY, fx, fz, lx, lz);
            }
            case ONEE_PLUS_2S -> {
                // Three mobs: left (shadow), right (shadow), middle+1 forward (elite)
                spawnHerobrineOffset("annoyingvillagers:low_shadow_herobrine_clone", centerForward, +side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset("annoyingvillagers:low_shadow_herobrine_clone", centerForward, -side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), thirdForward, 0.0, baseY, fx, fz, lx, lz);
            }
            case TWO_E -> {
                // Two elites: left + right
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, +side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, -side, baseY, fx, fz, lx, lz);
            }
            case TWOE_PLUS_1S -> {
                // Three mobs: left (shadow), right (elite), middle+1 forward (elite)
                spawnHerobrineOffset("annoyingvillagers:low_shadow_herobrine_clone", centerForward, +side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, -side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), thirdForward, 0.0, baseY, fx, fz, lx, lz);
            }
            case THREE_E -> {
                // Three elites: left, right, and middle+1 forward (super rare)
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, +side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), centerForward, -side, baseY, fx, fz, lx, lz);
                spawnHerobrineOffset(pickRandom(elites, random), thirdForward, 0.0, baseY, fx, fz, lx, lz);
            }
        }
    }

    private void summonHerobrines() {
        playSecondFormSupportCastAnimation();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.clearSummonSpace(serverLevel);
        }
        this.setCustomNameVisible(false);
        setUseHerobrineTexture(true);
        summonAtNight();
        this.summonTiming = -2;
    }

    private void summonHerobrinesAndEscape() {
        playSecondFormSupportCastAnimation();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.clearSummonSpace(serverLevel);
        }
        if (isDay(this.level())) {
            summonEscapeAtDay();
        } else {
            summonEscapeAtNight();
        }
    }

    public boolean removeWhenFarAway(double d0) {
        return false;
    }

    public @NotNull SoundEvent getHurtSound(@NotNull DamageSource damagesource) {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.hurt")));
    }

    public @NotNull SoundEvent getDeathSound() {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.death")));
    }

    public boolean isDay(Level level) {
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay >= 0 && timeOfDay < 13000;
    }

    public boolean hurt(@NotNull DamageSource pSource, float f) {
        if (pSource.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(pSource, f);
        }
        if (this.shouldBlockPortalSummonDamage()) {
            this.blockPortalSummonDamage(pSource);
            return false;
        }
        if (this.fishingHookCancelledEscape) {
            return super.hurt(pSource, 1.0F);
        }
        if (this.getHealth() == 1 || this.combatMode) {
            if (this.level() instanceof ServerLevel serverLevel) {
                CommonUtil.damageBlocked(pSource, this, serverLevel);
            }
            return false;
        }
        return super.hurt(pSource, 1.0F);
    }

    private boolean shouldBlockPortalSummonDamage() {
        return this.summoning;
    }

    private void blockPortalSummonDamage(DamageSource source) {
        if (this.level() instanceof ServerLevel serverLevel) {
            CommonUtil.damageBlocked(source, this, serverLevel);
        }
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        this.hookedWaitingForGround = false;
        this.hookedLeftGround = false;
        this.setHooked(false);
        this.setNoAi(false);
        this.setInvulnerable(false);
        super.die(damageSource);
    }


    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        if (this.escapeTiming >= 0 || this.fishingHookCancelledEscape) {
            this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.TRANSPORTER_FRAGMENT.get()));
        }
    }

    private ItemStack randomDamage(ItemStack itemStack) {
        int maxDamage = itemStack.getMaxDamage();
        itemStack.setDamageValue(new Random().nextInt(maxDamage / 3, maxDamage * 3 / 4));
        return itemStack;
    }

    private void equipGearForLowHerobrineClone(Entity entity) {
        if (!(entity instanceof LowHerobrineCloneEntity) && !(entity instanceof LowShadowHerobrineCloneEntity)) {
            return;
        }
        if (random.nextFloat() < 0.3f) {
            entity.setItemSlot(EquipmentSlot.HEAD, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_HELMET.get())));
        }
        if (random.nextFloat() < 0.3f) {
            entity.setItemSlot(EquipmentSlot.CHEST, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())));
        }
        if (random.nextFloat() < 0.3f) {
            entity.setItemSlot(EquipmentSlot.LEGS, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_LEGGINGS.get())));
        }
        if (random.nextFloat() < 0.3f) {
            entity.setItemSlot(EquipmentSlot.FEET, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_BOOTS.get())));
        }
        entity.setItemSlot(EquipmentSlot.MAINHAND, randomDamage(new ItemStack(listWeapons.get(random.nextInt(listWeapons.size())))));
    }

    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor serverlevelaccessor, @NotNull DifficultyInstance difficultyinstance, @NotNull MobSpawnType mobspawntype, @Nullable SpawnGroupData spawngroupdata, @Nullable CompoundTag compoundtag) {
        if (mobspawntype == MobSpawnType.NATURAL || mobspawntype == MobSpawnType.CHUNK_GENERATION) {
            ServerLevel serverLevel = serverlevelaccessor.getLevel();
            GregData gregData = GregData.get(serverLevel);

            if (!gregData.tryClaim(serverLevel, this.getUUID())) {
                this.discard();
                return null;
            } else {
            }

            BlockPos blockPos = this.getOnPos();
            int surfaceY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos).getY();
            BlockPos spawnPos = new BlockPos(blockPos.getX(), surfaceY, blockPos.getZ());
            this.moveTo(spawnPos, this.getYRot(), this.getXRot());
        }

        ChatUtil.joinGame(this, "Greg");
        return super.finalizeSpawn(serverlevelaccessor, difficultyinstance, mobspawntype, spawngroupdata, compoundtag);
    }

    public void awardKillScore(@NotNull Entity entity, int i, @NotNull DamageSource damagesource) {
        super.awardKillScore(entity, i, damagesource);
    }

    public void baseTick() {
        super.baseTick();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setWhiteEye(pCompound.getBoolean("WhiteEye"));
        setUseHerobrineTexture(pCompound.getBoolean("UseHerobrineTexture"));
        summoning = pCompound.getBoolean("Summoning");
        summonTiming = pCompound.getInt("SummonTiming");
        escapeTiming = pCompound.getInt("EscapeTiming");
        summonTimestamp = pCompound.getInt("SummonTimestamp");
        combatMode = pCompound.getBoolean("CombatMode");
        recallTime = pCompound.getInt("RecallTime");
        fishingHookCancelledEscape = pCompound.getBoolean("FishingHookCancelledEscape");
        lowCloneSupportCooldown = pCompound.contains("LowCloneSupportCooldown") ? pCompound.getInt("LowCloneSupportCooldown") : randomCooldownSeconds(90, 180);
        portalActionCooldown = pCompound.contains("PortalActionCooldown") ? pCompound.getInt("PortalActionCooldown") : randomCooldownSeconds(20, 40);
        this.hookedWaitingForGround = pCompound.getBoolean("HookedWaitingForGround");
        this.hookedLeftGround = pCompound.getBoolean("HookedLeftGround");
        this.setHooked(pCompound.getBoolean("Hooked"));
        if (this.isHooked() && this.hookedWaitingForGround) {
            this.releaseHookedPhysicsUntilGround();
        }
        if (pCompound.hasUUID("FirstSummonedHerobrineUUID")) {
            firstSummonedHerobrineUUID = pCompound.getUUID("FirstSummonedHerobrineUUID");
        }
        if (pCompound.hasUUID("SecondSummonedHerobrineUUID")) {
            secondSummonedHerobrineUUID = pCompound.getUUID("SecondSummonedHerobrineUUID");
        }
        if (pCompound.hasUUID("ThirdSummonedHerobrineUUID")) {
            thirdSummonedHerobrineUUID = pCompound.getUUID("ThirdSummonedHerobrineUUID");
        }
        for (int i = 0; i < MAX_COMBAT_LOW_CLONE_SUPPORT; i++) {
            String key = "CombatLowCloneSupportUUID" + i;
            if (pCompound.hasUUID(key)) {
                this.combatLowCloneSupportUUIDs[i] = pCompound.getUUID(key);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("WhiteEye", isWhiteEye());
        pCompound.putBoolean("UseHerobrineTexture", isUseHerobrineTexture());
        pCompound.putBoolean("Summoning", summoning);
        pCompound.putInt("SummonTiming", summonTiming);
        pCompound.putInt("EscapeTiming", escapeTiming);
        pCompound.putInt("SummonTimestamp", summonTimestamp);
        pCompound.putBoolean("CombatMode", combatMode);
        pCompound.putInt("RecallTime", recallTime);
        pCompound.putBoolean("FishingHookCancelledEscape", fishingHookCancelledEscape);
        pCompound.putInt("LowCloneSupportCooldown", lowCloneSupportCooldown);
        pCompound.putInt("PortalActionCooldown", portalActionCooldown);
        pCompound.putBoolean("Hooked", this.isHooked());
        pCompound.putBoolean("HookedWaitingForGround", this.hookedWaitingForGround);
        pCompound.putBoolean("HookedLeftGround", this.hookedLeftGround);
        if (firstSummonedHerobrineUUID != null) {
            pCompound.putUUID("FirstSummonedHerobrineUUID", firstSummonedHerobrineUUID);
        }
        if (secondSummonedHerobrineUUID != null) {
            pCompound.putUUID("SecondSummonedHerobrineUUID", secondSummonedHerobrineUUID);
        }
        if (thirdSummonedHerobrineUUID != null) {
            pCompound.putUUID("ThirdSummonedHerobrineUUID", thirdSummonedHerobrineUUID);
        }
        for (int i = 0; i < MAX_COMBAT_LOW_CLONE_SUPPORT; i++) {
            if (this.combatLowCloneSupportUUIDs[i] != null) {
                pCompound.putUUID("CombatLowCloneSupportUUID" + i, this.combatLowCloneSupportUUIDs[i]);
            }
        }
    }

    public static boolean canSpawn(EntityType<HerobrineGregEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        if (GregData.get(serverLevel).isOccupied(serverLevel)) {
            return false;
        }
        return ProgressionUtil.isAtLeastDifficulty(Difficulty.HARD) && Monster.checkMonsterSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            GregData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
        }
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes();

        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.5D);
        builder = builder.add(Attributes.MAX_HEALTH, 40.0D);
        builder = builder.add(Attributes.ARMOR, 0.0D);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 0.0D);
        builder = builder.add(Attributes.FOLLOW_RANGE, 48.0D);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        return builder;
    }
}
