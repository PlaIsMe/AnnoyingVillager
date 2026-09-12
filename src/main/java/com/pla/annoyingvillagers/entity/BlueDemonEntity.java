package com.pla.annoyingvillagers.entity;

import javax.annotation.Nullable;

import com.pla.annoyingvillagers.clazz.*;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.entity.goal.RetargetCloserThreatGoal;
import com.pla.annoyingvillagers.entity.goal.RollItemGoal;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.item.BlueDemonChestplateItem;
import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.rig.LockableRigAttackAnimation;
import com.pla.annoyingvillagers.rig.RigStunEscapeEntity;
import com.pla.annoyingvillagers.spawnhandler.BluedemonData;
import com.pla.annoyingvillagers.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;

import java.util.*;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class BlueDemonEntity extends Monster implements ForceTickEntity, BurstProtectEntity, CombatVoiceLineEntity,
        LockableRigAttackAnimation, RigStunEscapeEntity, RollItemUser, DangerousReaction {
    private static final float WATER_SWIM_ACCELERATION = 0.08F;
    private static final double WATER_SWIM_HORIZONTAL_SPEED = 0.42D;

    @Nullable
    private BbqEntity bbqSauce;
    @Nullable
    private UUID bbqSauceUUID;

    @Nullable
    private BbqEntity honeyMustardSauce;
    @Nullable
    private UUID honeyMustardSauceUUID;

    @Nullable
    private BbqEntity soySauce;
    @Nullable
    private UUID soySauceUUID;

    @Nullable
    private BbqEntity sweetOnionSauce;
    @Nullable
    private UUID sweetOnionSauceUUID;

    private int bbqResolveCooldown;
    private int bbqOrderCooldown;
    private int bbqHeadAttackCooldown;
    private int bbqModeCooldown;

    private int healingTick = 0;
    private int healingCooldown = 0;
    private int stunEscapeCooldown = 0;
    private Entity blockDamage = null;
    private int swapWeaponCooldown;
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(BlueDemonEntity.class, EntityDataSerializers.INT);
    private int stateTransformCooldown = -1;
    @Nullable
    private UUID savedTargetUUID;

    private int squadArrivalTicks = -1;
    private float sauceSquadAngle = 0.0F;
    private boolean spawnedBbqSauce = false;
    private int dieTick = -1;
    @Nullable
    private UUID savedKillerUUID;
    private boolean neverLeave = false;
    private int leaveTicks = 0;
    private Vec3 leaveDirection = Vec3.ZERO;
    private int voiceCooldown = 0;
    private int rigAttackAnimationLockCount;

    @Override
    public void lock() {
        this.rigAttackAnimationLockCount++;
    }

    @Override
    public void unlock() {
        if (this.rigAttackAnimationLockCount > 0) this.rigAttackAnimationLockCount--;
    }

    @Override
    public boolean isLocked() {
        return this.rigAttackAnimationLockCount > 0;
    }

    @Override
    public int getVoiceCooldown() {
        return voiceCooldown;
    }

    @Override
    public void setVoiceCooldown(int cooldown) {
        this.voiceCooldown = cooldown;
    }

    public void setLeaveTicks(int leaveTicks) {
        this.leaveTicks = leaveTicks;
    }

    public int getLeaveTicks() {
        return leaveTicks;
    }

    public void setNeverLeave(boolean neverLeave) {
        this.neverLeave = neverLeave;
    }

    public void setStateTransformCooldown(int stateTransformCooldown) {
        this.stateTransformCooldown = stateTransformCooldown;

        if (stateTransformCooldown > 0) {
            this.sauceSquadAngle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        }
    }

    private double getSauceLaneOffset(SauceType sauceType) {
        return switch (sauceType) {
            case BBQ_SAUCE -> -2.25D;
            case HONEY_MUSTARD_SAUCE -> -0.75D;
            case SOY_SAUCE -> 0.75D;
            case SWEET_ONION_SAUCE -> 2.25D;
        };
    }

    public float getSauceSquadAngle() {
        return this.sauceSquadAngle;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
    }

    public int getStunEscapeCooldown() {
        return stunEscapeCooldown;
    }

    public void setStunEscapeCooldown(int stunEscapeCooldown) {
        this.stunEscapeCooldown = stunEscapeCooldown;
    }

    public void setBlockDamage(Entity blockDamage) {
        this.blockDamage = blockDamage;
    }

    public Entity getBlockDamage() {
        return blockDamage;
    }

    public int getSwapWeaponCooldown() {
        return swapWeaponCooldown;
    }

    public void setSwapWeaponCooldown(int swapWeaponCooldown) {
        this.swapWeaponCooldown = swapWeaponCooldown;
    }

    public int getHealingTick() {
        return healingTick;
    }

    public void setHealingTick(int healingTick) {
        this.healingTick = healingTick;
    }

    public void setHealingCooldown() {
        this.healingCooldown = random.nextInt(900, 1500);
    }

    public int getHealingCooldown() {
        return healingCooldown;
    }

    public boolean isSauceArrivalPending() {
        return this.squadArrivalTicks > 0;
    }

    private void backupCurrentTarget() {
        LivingEntity target = this.getTarget();
        this.savedTargetUUID = target != null && target.isAlive() ? target.getUUID() : null;
    }

    private void restoreBackedUpTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.savedTargetUUID == null) {
            return;
        }

        Entity entity = serverLevel.getEntity(this.savedTargetUUID);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            this.setTarget(living);
        }

        this.savedTargetUUID = null;
    }

    private boolean isLeavingNow() {
        return !this.neverLeave && this.leaveTicks > 0 && this.leaveTicks <= 40;
    }

    private void updateLeaveDirectionFromThreat() {
        Vec3 away;

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            away = this.position().subtract(target.position());
        } else if (this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
            away = this.position().subtract(this.getLastHurtByMob().position());
        } else {
            away = new Vec3(-this.getLookAngle().x, 0.0D, -this.getLookAngle().z);
        }

        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }

        this.leaveDirection = away.normalize();
    }

    private void tickLeaveRetreat() {
        if (this.leaveDirection.lengthSqr() < 1.0E-4D) {
            this.updateLeaveDirectionFromThreat();
        }

        this.setTarget(null);

        double x = this.getX() + this.leaveDirection.x * 12.0D;
        double z = this.getZ() + this.leaveDirection.z * 12.0D;

        this.getNavigation().moveTo(x, this.getY(), z, 1.45D);
        this.getLookControl().setLookAt(x, this.getEyeY(), z);
    }

    private void discardAllSauces(ServerLevel serverLevel) {
        BbqEntity bbq = this.resolveAliveSauce(serverLevel, SauceType.BBQ_SAUCE);
        BbqEntity honey = this.resolveAliveSauce(serverLevel, SauceType.HONEY_MUSTARD_SAUCE);
        BbqEntity soy = this.resolveAliveSauce(serverLevel, SauceType.SOY_SAUCE);
        BbqEntity sweet = this.resolveAliveSauce(serverLevel, SauceType.SWEET_ONION_SAUCE);

        if (bbq != null) bbq.discard();
        if (honey != null) honey.discard();
        if (soy != null) soy.discard();
        if (sweet != null) sweet.discard();

        this.setSauce(SauceType.BBQ_SAUCE, null);
        this.setSauce(SauceType.HONEY_MUSTARD_SAUCE, null);
        this.setSauce(SauceType.SOY_SAUCE, null);
        this.setSauce(SauceType.SWEET_ONION_SAUCE, null);
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity pTarget) {
        if (this.isLeavingNow()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(pTarget);
    }

    private void startSauceRetreat(SauceType sauceType) {
        BbqEntity sauce = this.getSauce(sauceType);
        if (sauce != null && sauce.isAlive()) {
            sauce.startRetreat();
        }

        this.setSauce(sauceType, null);
    }

    private void retreatAllSauces() {
        this.startSauceRetreat(SauceType.BBQ_SAUCE);
        this.startSauceRetreat(SauceType.HONEY_MUSTARD_SAUCE);
        this.startSauceRetreat(SauceType.SOY_SAUCE);
        this.startSauceRetreat(SauceType.SWEET_ONION_SAUCE);
    }

    public void beginStateTwoTransform() {
        this.backupCurrentTarget();
        this.retreatAllSauces();

        this.setNoAi(true);
        this.setHealingTick(0);
        this.setState(2);
        this.setStateTransformCooldown(600);
    }

    private void finishStateTwoTransform(ServerLevel serverLevel) {
        this.setHealingTick(-1);
        this.setNoAi(false);
        this.setState(3);
        this.restoreBackedUpTarget();

        this.squadArrivalTicks = 20 * 20;

        ItemStack armorStack = new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get());
        armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
        armorStack.enchant(Enchantments.PROJECTILE_PROTECTION, 5);
        armorStack.enchant(Enchantments.FIRE_PROTECTION, 5);
        armorStack.enchant(Enchantments.BLAST_PROTECTION, 5);
        this.setItemSlot(EquipmentSlot.CHEST, armorStack);

        this.setSwapWeaponCooldown(new Random().nextInt(200, 600));

        this.ensureStateSauces();
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("<" + Component.translatable(SauceType.BBQ_SAUCE.getTranslationKey()).getString() + "> " + Component.translatable("subtitles.bbq_sauce_squad_arrived").getString()),
                false
        );
    }

    private void tickSquadArrival(ServerLevel serverLevel) {
        if (this.squadArrivalTicks <= 0) {
            return;
        }

        this.squadArrivalTicks--;
        if (this.squadArrivalTicks > 0) {
            return;
        }

        BbqEntity bbq = this.getSauce(SauceType.BBQ_SAUCE);
        BbqEntity honey = this.getSauce(SauceType.HONEY_MUSTARD_SAUCE);
        BbqEntity soy = this.getSauce(SauceType.SOY_SAUCE);
        BbqEntity onion = this.getSauce(SauceType.SWEET_ONION_SAUCE);

        if (bbq != null) bbq.teleportNearLeaderIfTooFar();
        if (honey != null) honey.teleportNearLeaderIfTooFar();
        if (soy != null) soy.teleportNearLeaderIfTooFar();
        if (onion != null) onion.teleportNearLeaderIfTooFar();
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    public void setState(int state) {
        this.entityData.set(STATE, state);
    }

    private @Nullable BbqEntity getSauce(SauceType sauceType) {
        return switch (sauceType) {
            case BBQ_SAUCE -> this.bbqSauce;
            case HONEY_MUSTARD_SAUCE -> this.honeyMustardSauce;
            case SOY_SAUCE -> this.soySauce;
            case SWEET_ONION_SAUCE -> this.sweetOnionSauce;
        };
    }

    private @Nullable UUID getSauceUUID(SauceType sauceType) {
        return switch (sauceType) {
            case BBQ_SAUCE -> this.bbqSauceUUID;
            case HONEY_MUSTARD_SAUCE -> this.honeyMustardSauceUUID;
            case SOY_SAUCE -> this.soySauceUUID;
            case SWEET_ONION_SAUCE -> this.sweetOnionSauceUUID;
        };
    }

    private void setSauce(SauceType sauceType, @Nullable BbqEntity sauce) {
        UUID uuid = sauce == null ? null : sauce.getUUID();

        switch (sauceType) {
            case BBQ_SAUCE -> {
                this.bbqSauce = sauce;
                this.bbqSauceUUID = uuid;
            }
            case HONEY_MUSTARD_SAUCE -> {
                this.honeyMustardSauce = sauce;
                this.honeyMustardSauceUUID = uuid;
            }
            case SOY_SAUCE -> {
                this.soySauce = sauce;
                this.soySauceUUID = uuid;
            }
            case SWEET_ONION_SAUCE -> {
                this.sweetOnionSauce = sauce;
                this.sweetOnionSauceUUID = uuid;
            }
        }
    }

    private void ensureSauceExists(SauceType sauceType) {
        BbqEntity current = this.getSauce(sauceType);
        if (current != null && current.isAlive()) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID uuid = this.getSauceUUID(sauceType);
        if (uuid != null) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof BbqEntity sauce && sauce.isAlive()) {
                this.setSauce(sauceType, sauce);
                return;
            }
        }

        BbqEntity sauce = new BbqEntity(AnnoyingVillagersModEntities.BBQ.get(), serverLevel);

        double baseAngle = switch (sauceType) {
            case BBQ_SAUCE -> 0.0D;
            case HONEY_MUSTARD_SAUCE -> Math.PI * 0.5D;
            case SOY_SAUCE -> Math.PI;
            case SWEET_ONION_SAUCE -> Math.PI * 1.5D;
        };

        double spawnX;
        double spawnZ;

        if (this.getState() == 3) {
            float angle = this.getSauceSquadAngle();
            double distance = 18.0D + this.random.nextDouble() * 4.0D;
            double laneOffset = this.getSauceLaneOffset(sauceType);

            double forwardX = Mth.cos(angle);
            double forwardZ = Mth.sin(angle);
            double sideX = -forwardZ;
            double sideZ = forwardX;

            spawnX = this.getX() + forwardX * distance + sideX * laneOffset;
            spawnZ = this.getZ() + forwardZ * distance + sideZ * laneOffset;
        } else {
            double angle = this.random.nextDouble() * (Math.PI * 2.0D);
            double radius = 2.5D + this.random.nextDouble() * 1.5D;
            spawnX = this.getX() + Math.cos(angle) * radius;
            spawnZ = this.getZ() + Math.sin(angle) * radius;
        }

        int spawnY = serverLevel.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(spawnX, this.getY(), spawnZ)
        ).getY();

        sauce.moveTo(spawnX, spawnY, spawnZ, this.random.nextFloat() * 360.0F, 0.0F);
        sauce.setLeader(this);
        sauce.setSauceType(sauceType);
        sauce.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(sauce.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );

        serverLevel.addFreshEntity(sauce);
        this.setSauce(sauceType, sauce);
    }

    private void ensureStateSauces() {
        if (this.getState() == 0 || this.getState() == 1) {
            this.ensureSauceExists(SauceType.BBQ_SAUCE);
            return;
        }

        if (this.getState() == 3) {
            this.ensureSauceExists(SauceType.BBQ_SAUCE);
            this.ensureSauceExists(SauceType.HONEY_MUSTARD_SAUCE);
            this.ensureSauceExists(SauceType.SOY_SAUCE);
            this.ensureSauceExists(SauceType.SWEET_ONION_SAUCE);
        }
    }

    @Nullable
    private LivingEntity resolveSauceTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return null;
        }

        if (target instanceof HerobrineMob herobrineMob
                && (herobrineMob.isSacrificing() || herobrineMob.isHealing())) {
            if (herobrineMob.getFirstPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getSecondPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getThirdPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getFourthPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }

        if (target instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity
                && lowHerobrineCloneEntity.isHealing()
                && lowHerobrineCloneEntity.getPossessedByEntity() != null
                && lowHerobrineCloneEntity.getPossessedByEntity().isAlive()) {
            return lowHerobrineCloneEntity.getPossessedByEntity();
        }

        if (target instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity
                && (lowShadowHerobrineCloneEntity.isSacrificing() || lowShadowHerobrineCloneEntity.isHealing())
                && lowShadowHerobrineCloneEntity.getPossessedByEntity() != null
                && lowShadowHerobrineCloneEntity.getPossessedByEntity().isAlive()) {
            return lowShadowHerobrineCloneEntity.getPossessedByEntity();
        }

        return target;
    }

    public @Nullable BlueDemonThrownTridentEntity getNearestGroundedOwnedTrident(double radius) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        AABB box = this.getBoundingBox().inflate(radius);

        List<BlueDemonThrownTridentEntity> tridents = serverLevel.getEntitiesOfClass(
                BlueDemonThrownTridentEntity.class,
                box,
                trident -> trident.isAlive()
                        && trident.isGroundedTrident()
                        && trident.belongsToOwner(this)
                        && !trident.isAbsorbingToWearer()
                        && trident.distanceToSqr(this) > 9.0D
        );

        BlueDemonThrownTridentEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlueDemonThrownTridentEntity trident : tridents) {
            double distance = this.distanceToSqr(trident);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = trident;
            }
        }

        return best;
    }

    public BlueDemonEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.BLUE_DEMON.get(), level);
    }

    public BlueDemonEntity(EntityType<? extends BlueDemonEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(3.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.xpReward = 0;
        this.setNoAi(false);
        this.setCustomName(this.getDisplayName());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new BlueDemonWaterPathNavigation(this, level);
    }

    private static class BlueDemonWaterPathNavigation extends GroundPathNavigation {
        public BlueDemonWaterPathNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }

        @Override
        protected boolean hasValidPathType(@NotNull BlockPathTypes type) {
            if (type == BlockPathTypes.WATER || type == BlockPathTypes.WATER_BORDER) {
                return true;
            }
            return super.hasValidPathType(type);
        }

        @Override
        public boolean isStableDestination(@NotNull BlockPos blockPos) {
            return this.level.getFluidState(blockPos).is(FluidTags.WATER) || super.isStableDestination(blockPos);
        }
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (this.isInWater() && !this.isNoAi() && !this.isPassenger()) {
            this.moveRelative(WATER_SWIM_ACCELERATION, travelVector);
            super.travel(travelVector);
            this.setDeltaMovement(this.limitWaterHorizontalMotion(this.getDeltaMovement()));
            return;
        }
        super.travel(travelVector);
    }

    private Vec3 limitWaterHorizontalMotion(Vec3 motion) {
        double horizontalSpeedSqr = motion.x * motion.x + motion.z * motion.z;
        double maxSpeedSqr = WATER_SWIM_HORIZONTAL_SPEED * WATER_SWIM_HORIZONTAL_SPEED;
        if (horizontalSpeedSqr <= maxSpeedSqr) {
            return motion;
        }

        double horizontalSpeed = Math.sqrt(horizontalSpeedSqr);
        return new Vec3(
                motion.x / horizontalSpeed * WATER_SWIM_HORIZONTAL_SPEED,
                motion.y,
                motion.z / horizontalSpeed * WATER_SWIM_HORIZONTAL_SPEED
        );
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return this.isInWater() ? SoundEvents.DROWNED_HURT_WATER : SoundEvents.DROWNED_HURT;
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return this.isInWater() ? SoundEvents.DROWNED_DEATH_WATER : SoundEvents.DROWNED_DEATH;
    }

    @Override
    protected @NotNull SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(0, new RetargetCloserThreatGoal(this));
        CommonGoals.registerDangerousReactionGoals(this);
        this.goalSelector.addGoal(1, new RollItemGoal(this));
        CommonGoals.registerGoalForBlueDemonNpc(this);
    }

    @Override
    public boolean canRollItem() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.getState() == 3 && this.swapWeaponCooldown == 0;
    }

    public boolean canBeAffected(MobEffectInstance mobeffectinstance) {
        return (mobeffectinstance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL || mobeffectinstance.getEffect() == MobEffects.GLOWING) && super.canBeAffected(mobeffectinstance);
    }

    private boolean isAliveSauce(@Nullable BbqEntity sauce) {
        return sauce != null && sauce.isAlive();
    }

    private void clearSweetTemporaryCarriedTrident(@Nullable BbqEntity sweet) {
        if (!this.isAliveSauce(sweet)) {
            return;
        }

        ItemStack stack = sweet.getMainHandItem();
        if (!stack.is(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get())) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CarriedTridentMode")) {
            sweet.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    public boolean isInFinalDeathSequence() {
        return this.dieTick > 0;
    }

    private void playFinalDeathAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            if (this.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
//                this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.BLUE_DEMON_DIE, 0.0F);
//            } else {
//                this.getLivingEntityPatch().playAnimationSynchronized(AnimsEpicFight.BLUE_DEMON_DIE_LEGENDARY_SWORD_START, 0.0F);
//            }
//        }

        if (this.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
            RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_DIE);
        } else {
            RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_DIE_START);
        }
    }

    private void playDeathAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null && (this.dieTick <= 180 && this.dieTick % 10 == 0)) {
//            if (this.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
//                this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.BLUE_DEMON_STATE_TRANSFORM, 0.0F);
//            } else {
//                this.getLivingEntityPatch().playAnimationSynchronized(AnimsEpicFight.BLUE_DEMON_DIE_LEGENDARY_SWORD_TICK, 0.0F);
//            }
//        }

        if (this.dieTick <= 180 && this.dieTick % 10 == 0) {
            if (this.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
                RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_STATE_TRANSFORM);
            } else {
                RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_DIE_TICK);
            }
        }
    }

    private void startFinalDeathSequence(ServerLevel serverLevel, DamageSource damageSource) {
        if (this.dieTick > 0) {
            return;
        }

        this.dieTick = 200;
        this.setHealth(1.0F);
        this.setNoAi(true);
        this.setTarget(null);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);

        if (damageSource.getEntity() != null) {
            this.savedKillerUUID = damageSource.getEntity().getUUID();
        } else {
            this.savedKillerUUID = null;
        }

        playFinalDeathAnimation();

        this.startSauceDeathWatch(serverLevel);
    }

    private void startSauceDeathWatch(ServerLevel serverLevel) {
        BbqEntity bbq = this.resolveAliveSauce(serverLevel, SauceType.BBQ_SAUCE);
        BbqEntity honey = this.resolveAliveSauce(serverLevel, SauceType.HONEY_MUSTARD_SAUCE);
        BbqEntity soy = this.resolveAliveSauce(serverLevel, SauceType.SOY_SAUCE);
        BbqEntity sweet = this.resolveAliveSauce(serverLevel, SauceType.SWEET_ONION_SAUCE);

        if (bbq != null) {
            bbq.startLeaderDeathWatch(this);
        }
        if (honey != null) {
            honey.startLeaderDeathWatch(this);
        }
        if (soy != null) {
            soy.startLeaderDeathWatch(this);
        }
        if (sweet != null) {
            sweet.startLeaderDeathWatch(this);
        }
    }

    private void tickFinalDeathSequence(ServerLevel serverLevel) {
        this.setNoAi(true);
        this.setTarget(null);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);

        playDeathAnimation();

        if (this.dieTick % 5 == 0 && new Random().nextBoolean()) {
            this.strikeDeathLightning(serverLevel);
        }

        this.dieTick--;
        if (this.dieTick <= 0) {
            this.finishFinalDeathSequence(serverLevel);
        }
    }

    private void strikeDeathLightning(ServerLevel serverLevel) {
        TridentLightningBolt tridentLightningBolt = new TridentLightningBolt(AnnoyingVillagersModEntities.TRIDENT_LIGHTNING_BOLT.get(), serverLevel);
        tridentLightningBolt.setOwner(this);
        tridentLightningBolt.moveTo(
                this.getX(),
                this.getY(),
                this.getZ()
        );
        serverLevel.addFreshEntity(tridentLightningBolt);
    }

    private DamageSource buildSavedKillerDamageSource(ServerLevel serverLevel) {
        if (this.savedKillerUUID != null) {
            Entity entity = serverLevel.getEntity(this.savedKillerUUID);

            if (entity instanceof Player player) {
                return this.damageSources().playerAttack(player);
            }

            if (entity instanceof LivingEntity livingEntity) {
                return this.damageSources().mobAttack(livingEntity);
            }
        }

        return this.damageSources().generic();
    }

    private void finishFinalDeathSequence(ServerLevel serverLevel) {
        DamageSource finalSource = this.buildSavedKillerDamageSource(serverLevel);
        this.setHealth(0.0F);
        this.handleSaucesWhenBlueDemonDies(serverLevel);
        this.dieTick = -1;
        this.die(finalSource);
        this.remove(RemovalReason.KILLED);
    }

    private void handleSaucesWhenBlueDemonDies(ServerLevel serverLevel) {
        BbqEntity bbq = this.resolveAliveSauce(serverLevel, SauceType.BBQ_SAUCE);
        BbqEntity honey = this.resolveAliveSauce(serverLevel, SauceType.HONEY_MUSTARD_SAUCE);
        BbqEntity soy = this.resolveAliveSauce(serverLevel, SauceType.SOY_SAUCE);
        BbqEntity sweet = this.resolveAliveSauce(serverLevel, SauceType.SWEET_ONION_SAUCE);

        this.clearSweetTemporaryCarriedTrident(sweet);

        boolean bbqAlive = this.isAliveSauce(bbq);
        boolean honeyAlive = this.isAliveSauce(honey);
        boolean soyAlive = this.isAliveSauce(soy);
        boolean sweetAlive = this.isAliveSauce(sweet);

        int aliveCount =
                (bbqAlive ? 1 : 0) +
                        (honeyAlive ? 1 : 0) +
                        (soyAlive ? 1 : 0) +
                        (sweetAlive ? 1 : 0);

        int existingTridents = (honeyAlive ? 1 : 0) + (soyAlive ? 1 : 0);
        int missingTridents = Math.max(0, 2 - existingTridents);

        boolean giveBbqMainTrident = false;
        boolean giveSweetMainTrident = false;
        boolean giveBbqOffhandChestplate = false;
        boolean giveSweetOffhandChestplate = false;

        int rawTridentDrops = 0;
        boolean rawChestplateDrop = false;

        if (aliveCount == 4) {
            giveSweetOffhandChestplate = true;
        } else if (aliveCount == 3) {
            if (sweetAlive) {
                giveSweetOffhandChestplate = true;
            } else if (bbqAlive) {
                giveBbqOffhandChestplate = true;
            } else {
                rawChestplateDrop = true;
            }
        } else {
            rawChestplateDrop = true;
        }

        while (missingTridents > 0) {
            if (sweetAlive && !giveSweetMainTrident) {
                giveSweetMainTrident = true;
            } else if (bbqAlive && !giveBbqMainTrident) {
                giveBbqMainTrident = true;
            } else {
                rawTridentDrops++;
            }
            missingTridents--;
        }

        double deathX = this.getX();
        double deathY = this.getY();
        double deathZ = this.getZ();

        if (bbqAlive) {
            bbq.startDeathAssembly(
                    deathX, deathY, deathZ,
                    giveBbqMainTrident,
                    giveBbqOffhandChestplate,
                    null
            );
        }

        if (honeyAlive) {
            honey.startDeathAssembly(
                    deathX, deathY, deathZ,
                    false,
                    false,
                    bbqAlive ? bbq : null
            );
        }

        if (soyAlive) {
            soy.startDeathAssembly(
                    deathX, deathY, deathZ,
                    false,
                    false,
                    bbqAlive ? bbq : null
            );
        }

        if (sweetAlive) {
            sweet.startDeathAssembly(
                    deathX, deathY, deathZ,
                    giveSweetMainTrident,
                    giveSweetOffhandChestplate,
                    bbqAlive ? bbq : null
            );
        }

        if (rawChestplateDrop) {
            this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get()));
        }

        for (int i = 0; i < rawTridentDrops; i++) {
            this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
        }
    }

    @Nullable
    private BbqEntity resolveAliveSauce(ServerLevel serverLevel, SauceType sauceType) {
        BbqEntity current = this.getSauce(sauceType);
        if (current != null && current.isAlive()) {
            return current;
        }

        UUID uuid = this.getSauceUUID(sauceType);
        if (uuid == null) {
            return null;
        }

        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof BbqEntity sauce && sauce.isAlive()) {
            this.setSauce(sauceType, sauce);
            return sauce;
        }

        return null;
    }

    @Override
    public float getBurstProtectCapRatio() {
        return 0.05F;
    }

    public boolean hurt(DamageSource damagesource, float f) {
        if (damagesource.is(DamageTypes.FALL)) return false;
        if (damagesource.is(DamageTypes.CACTUS)) return false;
        if (damagesource.is(DamageTypes.DROWN)) return false;
        if (damagesource.is(DamageTypes.LIGHTNING_BOLT)) return false;
        if (damagesource.is(DamageTypes.WITHER)) return false;
        if (damagesource.is(DamageTypes.TRIDENT)) return false;
        if (damagesource.is(DamageTypes.WITHER_SKULL)) return false;
        if (damagesource.getDirectEntity() instanceof ThrownPoisonEggEntity) return false;

        // Keep void damage as the one hard escape hatch. Everything else, including
        // /kill (GENERIC_KILL), must respect rig-animation invulnerability and the
        // Blue Demon phase-transition locks below.
        if (damagesource.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            boolean result = super.hurt(damagesource, f);
            if (result) {
                this.sayHurtSound(this, damagesource);
            }
            return result;
        }

        if (RigAnimationController.isInvulnerable(this)) {
            if (this.level() instanceof ServerLevel serverLevel) {
                CommonUtil.damageBlocked(damagesource, this, serverLevel);
            }
            return false;
        }

        if (this.level() instanceof ServerLevel serverLevel && (this.getState() == 2 || this.getState() == 1)) {
            CommonUtil.damageBlocked(damagesource, this, serverLevel);
            return false;
        }

        if (this.dieTick > 0) {
            if (this.level() instanceof ServerLevel serverLevel) {
                CommonUtil.damageBlocked(damagesource, this, serverLevel);
            }
            return false;
        }

        if (damagesource.is(DamageTypes.GENERIC_KILL)) {
            boolean result = super.hurt(damagesource, f);
            if (result) {
                this.sayHurtSound(this, damagesource);
            }
            return result;
        }
        if (ignoreDamageForSomeEpicFightAnimation()) {
            boolean result = super.hurt(damagesource, f);
            if (result) {
                this.sayHurtSound(this, damagesource);
            }
            return result;
        } else {
            return false;
        }
    }

    private boolean ignoreDamageForSomeEpicFightAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (this.level() instanceof ServerLevel serverLevel && this.getLivingEntityPatch() != null && this.dieTick <= 0) {
//            AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(this.getLivingEntityPatch().getAnimator().getPlayerFor(null)).getRealAnimation();
//            if (dynamicAnimation == AnimsWom.CUT_ANTITHEUS_ASCENSION
//                    || dynamicAnimation == AVAnimations.TRIDENT_ATTACK
//                    || dynamicAnimation == AnimsWom.ELECTRIC_FIELD
//                    || dynamicAnimation == AnimsPugilistSteve.TRIDENT_FESTIVAL
//                    || dynamicAnimation == AnimsPugilistSteve.BLUE_DEMON_STATE_TRANSFORM
//                    || dynamicAnimation == AnimsPugilistSteve.BLUE_DEMON_STATE_TRANSFORM_END) {
//                EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(serverLevel, HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO,
//                        this, damagesource.getEntity());
//                return false;
//            }
//        }

        return true;
    }

    @Nullable
    public BbqEntity getBbqEntity() {
        if (this.bbqSauce != null && this.bbqSauce.isAlive()) {
            return this.bbqSauce;
        }

        if (this.bbqResolveCooldown > 0) {
            this.bbqResolveCooldown--;
            return null;
        }

        this.bbqResolveCooldown = 20;

        if (!this.level().isClientSide && this.bbqSauceUUID != null) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.bbqSauceUUID);
            if (entity instanceof BbqEntity bbqEntity && bbqEntity.isAlive()) {
                this.bbqSauce = bbqEntity;
                return bbqEntity;
            }
        }

        return null;
    }

    public void setBbqEntity(@Nullable BbqEntity bbqEntity) {
        this.bbqSauce = bbqEntity;
        this.bbqSauceUUID = bbqEntity == null ? null : bbqEntity.getUUID();
    }

    @Nullable
    private LivingEntity resolveBbqTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return null;
        }

        if (target instanceof HerobrineMob herobrineMob
                && (herobrineMob.isSacrificing() || herobrineMob.isHealing())) {
            if (herobrineMob.getFirstPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getSecondPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getThirdPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            if (herobrineMob.getFourthPossessedHerobrine() instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }

        if (target instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity
                && lowHerobrineCloneEntity.isHealing()
                && lowHerobrineCloneEntity.getPossessedByEntity() != null) {
            if (!lowHerobrineCloneEntity.isAlive()) {
                return lowHerobrineCloneEntity.getPossessedByEntity();
            }
        }

        if (target instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity
                && (lowShadowHerobrineCloneEntity.isSacrificing() || lowShadowHerobrineCloneEntity.isHealing())
                && lowShadowHerobrineCloneEntity.getPossessedByEntity() != null) {
            if (!lowShadowHerobrineCloneEntity.isAlive()) {
                return lowShadowHerobrineCloneEntity.getPossessedByEntity();
            }
        }

        return target;
    }

    private void tickBbqOrders(@Nullable BbqEntity bbqEntity) {
        if (bbqEntity == null) {
            return;
        }

        if (this.isSauceArrivalPending()) {
            bbqEntity.clearCombat();
            return;
        }

        LivingEntity blueDemonTarget = this.getTarget();
        if (blueDemonTarget == null || !blueDemonTarget.isAlive()) {
            bbqEntity.clearCombat();
            return;
        }

        LivingEntity bbqTarget = this.resolveSauceTarget(blueDemonTarget);
        if (bbqTarget == null || !bbqTarget.isAlive()) {
            bbqEntity.clearCombat();
            return;
        }

        bbqEntity.setCombatTarget(bbqTarget);

        if (this.bbqHeadAttackCooldown > 0) {
            this.bbqHeadAttackCooldown--;
        }

        if (this.bbqOrderCooldown > 0) {
            this.bbqOrderCooldown--;
        }

        if (this.bbqModeCooldown > 0) {
            this.bbqModeCooldown--;
        }

        double blueDistance = this.distanceTo(blueDemonTarget);
        double bbqDistance = bbqEntity.distanceTo(bbqTarget);

        if (blueDistance > 10.0D) {
            bbqEntity.startParallelPursuit(bbqTarget, 25);

            if (this.bbqOrderCooldown <= 0) {
                bbqEntity.shootChain(bbqTarget, 3, 10);
                this.bbqOrderCooldown = 40;
            }
            return;
        }

        if (this.bbqHeadAttackCooldown <= 0 && blueDistance < 4.5D && bbqDistance < 6.5D && !bbqEntity.isHeadAttacking()) {
            bbqEntity.startHeadAttack(bbqTarget, 35);
            this.bbqHeadAttackCooldown = 110;
            this.bbqModeCooldown = 20;
            this.bbqOrderCooldown = 16;
            return;
        }

        if (!bbqEntity.isHeadAttacking() && this.bbqModeCooldown <= 0) {
            int roll = this.random.nextInt(100);

            if (roll < 25) {
                bbqEntity.startParallelPursuit(bbqTarget, 28);
                this.bbqModeCooldown = 40;
            } else if (roll < 60) {
                bbqEntity.startGroundOrbit(bbqTarget, 36);
                this.bbqModeCooldown = 42;
            } else {
                bbqEntity.startOrbit(bbqTarget, 28);
                this.bbqModeCooldown = 38;
            }
        }

        if (bbqEntity.isHeadAttacking() || this.bbqOrderCooldown > 0) {
            return;
        }

        switch (bbqEntity.getCombatMode()) {
            case PARALLEL -> {
                bbqEntity.shootChain(bbqTarget, 3, 10);
                this.bbqOrderCooldown = 40;
            }
            case GROUND_ORBIT -> {
                if (bbqDistance > 7.0D) {
                    bbqEntity.shootChain(bbqTarget, 3, 9);
                    this.bbqOrderCooldown = 36;
                } else {
                    bbqEntity.shootCluster(bbqTarget, 3, 1.05F, 10.0F);
                    this.bbqOrderCooldown = 48;
                }
            }
            case ORBIT -> {
                if (bbqDistance > 7.0D) {
                    bbqEntity.shootChain(bbqTarget, 4, 8);
                    this.bbqOrderCooldown = 38;
                } else {
                    bbqEntity.shootCluster(bbqTarget, 4, 1.1F, 10.0F);
                    this.bbqOrderCooldown = 52;
                }
            }
            default -> {
            }
        }
    }

    private void tickShockSauceOrders(@Nullable BbqEntity sauce) {
        if (sauce == null) {
            return;
        }

        if (this.isSauceArrivalPending()) {
            sauce.clearCombat();
            return;
        }

        LivingEntity blueDemonTarget = this.getTarget();
        if (blueDemonTarget == null || !blueDemonTarget.isAlive()) {
            sauce.clearCombat();
            return;
        }

        LivingEntity sauceTarget = this.resolveSauceTarget(blueDemonTarget);
        if (sauceTarget == null || !sauceTarget.isAlive()) {
            sauce.clearCombat();
            return;
        }

        sauce.setCombatTarget(sauceTarget);

        double blueDistance = this.distanceTo(blueDemonTarget);
        double sauceDistance = sauce.distanceTo(sauceTarget);

        if (blueDistance > 10.0D) {
            sauce.startParallelPursuit(sauceTarget, 25);
            return;
        }

        if (!sauce.isHeadAttacking() && this.tickCount % 60 == 0 && blueDistance < 4.5D && sauceDistance < 6.5D && this.random.nextInt(4) == 0) {
            sauce.startHeadAttack(sauceTarget, 28);
            return;
        }

        if (!sauce.isHeadAttacking() && this.tickCount % 40 == 0) {
            if (this.random.nextBoolean()) {
                sauce.startGroundOrbit(sauceTarget, 34);
            } else {
                sauce.startOrbit(sauceTarget, 28);
            }
        }
    }

    private void tickSweetOnionOrders(@Nullable BbqEntity sauce) {
        if (sauce == null) {
            return;
        }

        if (this.isSauceArrivalPending()) {
            sauce.clearCombat();
            return;
        }

        LivingEntity blueDemonTarget = this.getTarget();
        if (blueDemonTarget == null || !blueDemonTarget.isAlive()) {
            sauce.clearCombat();
            return;
        }

        LivingEntity sauceTarget = this.resolveSauceTarget(blueDemonTarget);
        if (sauceTarget == null || !sauceTarget.isAlive()) {
            sauce.clearCombat();
            return;
        }

        sauce.setCombatTarget(sauceTarget);
    }

    private void tickArmorBuff(ServerLevel serverLevel) {
        this.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                1,
                1,
                false,
                false,
                false
        ));

        this.addEffect(new MobEffectInstance(
                MobEffects.JUMP,
                1,
                1,
                false,
                false,
                false
        ));

        this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                1,
                2,
                false,
                false,
                false
        ));

        if (serverLevel.random.nextDouble() <= 0.1D) {
            BlueDemonUtil.spawnBlueDemonEffect(serverLevel, this);

            if (serverLevel.random.nextDouble() <= 0.8D) {
                float volume = (float) Mth.nextDouble(serverLevel.random, 0.05D, 0.5D);
                float pitch = (float) Mth.nextDouble(serverLevel.random, 0.8D, 1.1D);

                serverLevel.playSound(
                        null,
                        BlockPos.containing(this.getX(), this.getY(), this.getZ()),
                        AnnoyingVillagersModSounds.ELECTRIFY.get(),
                        SoundSource.NEUTRAL,
                        volume,
                        pitch
                );
            }
        }

        if (this.tickCount % 2 == 0) {
            if (conditionToAbsorbNearbyGroundedTridents()) {
                this.absorbNearbyGroundedOwnerTridents(serverLevel);
            }
        }
    }

    private boolean conditionToAbsorbNearbyGroundedTridents() {
//      ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(getLivingEntityPatch().getAnimator().getPlayerFor(null)).getRealAnimation();
//            if (dynamicAnimation != AVAnimations.TRIDENT_ATTACK && dynamicAnimation != AnimsPugilistSteve.TRIDENT_FESTIVAL) {
//                return true;
//            }
//        }

        RigAnimationId animationId = RigAnimationController.getActiveAnimationId(this);
        return animationId != RigAnimationId.BLUE_DEMON_EXTRA_ATTACK
                && animationId != RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL;
    }

    private void absorbNearbyGroundedOwnerTridents(ServerLevel serverLevel) {
        AABB box = new AABB(
                this.getX() - BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF,
                this.getY() - BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF,
                this.getZ() - BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF,
                this.getX() + BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF,
                this.getY() + BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF,
                this.getZ() + BlueDemonChestplateItem.CHEST_TRIDENT_ABSORB_BOX_HALF
        );

        List<BlueDemonThrownTridentEntity> tridents = serverLevel.getEntitiesOfClass(
                BlueDemonThrownTridentEntity.class,
                box,
                trident -> trident.isAlive()
                        && trident.isGroundedTrident()
                        && trident.belongsToOwner(this)
                        && !trident.isAbsorbingToWearer()
        );

        for (BlueDemonThrownTridentEntity trident : tridents) {
            trident.beginAbsorbToWearer(this);
        }
    }

    private void tickStateTwoPhysics() {
        this.setNoGravity(false);

        Vec3 motion = this.getDeltaMovement();
        motion = new Vec3(0.0D, motion.y, 0.0D);

        if (!this.onGround() && !this.isInWater() && !this.isInLava()) {
            motion = motion.add(0.0D, -0.08D, 0.0D);
        }

        motion = new Vec3(
                motion.x * 0.91D,
                motion.y * 0.98D,
                motion.z * 0.91D
        );

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        if (this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z);
        }
    }

    private void playStateTransformAnimation() {
//        ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.BLUE_DEMON_STATE_TRANSFORM, 0.0F);
//        }

        if (RigAnimationController.getActiveAnimationId(this) != RigAnimationId.BLUE_DEMON_STATE_TRANSFORM) {
            RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_STATE_TRANSFORM);
        }
    }

    private void playStateTransformEndAnimation() {
//        ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.BLUE_DEMON_STATE_TRANSFORM_END, 0.0F);
//        }

        RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_STATE_TRANSFORM_END);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getState() == 2 || this.dieTick > 0) {
            this.tickStateTwoPhysics();
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            /*
             * State 1 exists only for the Trident Festival transition. Keep the
             * required rig animation alive so state 1 can never become a permanent
             * damage-blocked 1 HP state.
             */
            if (this.getState() == 1) {
                ensureTridentFestivalPhase();
            }

            this.tickVoiceCooldown();
            if (!this.spawnedBbqSauce) {
                this.ensureSauceExists(SauceType.BBQ_SAUCE);
                this.spawnedBbqSauce = true;
            }
            if (stunEscapeCooldown > 0) stunEscapeCooldown--;
            if (swapWeaponCooldown > 0) swapWeaponCooldown--;
            if (healingCooldown > 0) healingCooldown--;
            if (stateTransformCooldown > 0) {
                if (stateTransformCooldown > 20) {
                    playStateTransformAnimation();
                } else if (stateTransformCooldown == 20) {
                    if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                        this.playSound(AnnoyingVillagersModSounds.BLUE_DEMON_SAY_PHASE_2_RELEASE.get(), 0.5F, 1.0F);
                    }
                    playStateTransformEndAnimation();
                } else if (stateTransformCooldown == 10) {
                    ItemStack legendaryStack = new ItemStack(AnnoyingVillagersModItems.LEGENDARY_SWORD.get());
                    legendaryStack.enchant(Enchantments.SHARPNESS, 5);
                    legendaryStack.enchant(Enchantments.SMITE, 5);
                    legendaryStack.enchant(Enchantments.SWEEPING_EDGE, 5);

                    ItemStack tridentStack = new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get());
                    tridentStack.enchant(Enchantments.SHARPNESS, 5);
                    tridentStack.enchant(Enchantments.SMITE, 5);
                    tridentStack.enchant(Enchantments.SWEEPING_EDGE, 5);

                    this.setItemInHand(InteractionHand.MAIN_HAND, legendaryStack);
                    this.setItemInHand(InteractionHand.OFF_HAND, tridentStack);
                    this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4, 300));
                }
                if (stateTransformCooldown % 2 == 0) {
                    this.heal(1.0F);
                }

                if (stateTransformCooldown <= 200 && stateTransformCooldown % 50 == 0) {
                    BlueDemonTridentItem.spawnDamageZones(serverLevel, this);
                }

                stateTransformCooldown--;
                if (stateTransformCooldown == 0) {
                    this.finishStateTwoTransform(serverLevel);
                }
            }
            if (this.dieTick > 0) {
                this.tickFinalDeathSequence(serverLevel);
                return;
            }

            if (!neverLeave) {
                this.leaveTicks = this.leaveTicks - 1;
                int remaining = this.leaveTicks;

                if (remaining == 40) {
                    this.setTarget(null);
                    this.updateLeaveDirectionFromThreat();
                    this.retreatAllSauces();
                }

                if (remaining <= 0) {
                    if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                        this.playSound(AnnoyingVillagersModSounds.BLUE_DEMON_SAY_WHEN_RETREAT.get(), 0.5F, 1.0F);
                    }
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("<" + this.getName().getString() + "> " + Component.translatable("subtitles.blue_demon_retreat").getString()),
                            false
                    );

                    this.discardAllSauces(serverLevel);
                    this.discard();
                    return;
                }

                if (this.isLeavingNow()) {
                    this.tickLeaveRetreat();
                    return;
                }
            }

            this.tickSquadArrival(serverLevel);
            if (healingTick != 0) {
                if (healingTick > 0) {
                    healingTick--;
                }
                tickArmorBuff(serverLevel);
            }

            this.syncChestplateHealingFoil();

            CommonUtil.stunEscapeAi(this);

            if (this.getState() == 3) {
                this.tickShockSauceOrders(this.getSauce(SauceType.HONEY_MUSTARD_SAUCE));
                this.tickShockSauceOrders(this.getSauce(SauceType.SOY_SAUCE));
                this.tickSweetOnionOrders(this.getSauce(SauceType.SWEET_ONION_SAUCE));
            }

            this.tickBbqOrders(this.getSauce(SauceType.BBQ_SAUCE));
        }
    }

    @Override
    public SoundEvent getAttackVoiceSound() {
        return AnnoyingVillagersModSounds.BLUE_DEMON_SAY.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (this.bbqSauceUUID != null) {
            tag.putUUID("BbqSauceUUID", this.bbqSauceUUID);
        }
        if (this.honeyMustardSauceUUID != null) {
            tag.putUUID("HoneyMustardSauceUUID", this.honeyMustardSauceUUID);
        }
        if (this.soySauceUUID != null) {
            tag.putUUID("SoySauceUUID", this.soySauceUUID);
        }
        if (this.sweetOnionSauceUUID != null) {
            tag.putUUID("SweetOnionSauceUUID", this.sweetOnionSauceUUID);
        }

        tag.putInt("HealingCooldown", healingCooldown);
        tag.putInt("HealingTick", healingTick);
        tag.putInt("StateTransformCooldown", stateTransformCooldown);
        tag.putInt("State", getState());

        if (this.savedTargetUUID != null) {
            tag.putUUID("SavedTargetUUID", this.savedTargetUUID);
        }
        if (this.savedKillerUUID != null) {
            tag.putUUID("SavedKillerUUID", this.savedKillerUUID);
        }

        tag.putInt("SquadArrivalTicks", this.squadArrivalTicks);
        tag.putBoolean("SpawnedBbqSauce", this.spawnedBbqSauce);
        tag.putInt("DieTick", this.dieTick);
        tag.putInt("LeaveTicks", leaveTicks);
        tag.putBoolean("NeverLeave", neverLeave);
        tag.putInt("VoiceCooldown", this.voiceCooldown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("BbqSauceUUID")) {
            this.bbqSauceUUID = tag.getUUID("BbqSauceUUID");
        }
        if (tag.hasUUID("HoneyMustardSauceUUID")) {
            this.honeyMustardSauceUUID = tag.getUUID("HoneyMustardSauceUUID");
        }
        if (tag.hasUUID("SoySauceUUID")) {
            this.soySauceUUID = tag.getUUID("SoySauceUUID");
        }
        if (tag.hasUUID("SweetOnionSauceUUID")) {
            this.sweetOnionSauceUUID = tag.getUUID("SweetOnionSauceUUID");
        }

        healingCooldown = tag.getInt("HealingCooldown");
        healingTick = tag.getInt("HealingTick");
        stateTransformCooldown = tag.getInt("StateTransformCooldown");
        setState(tag.contains("State") ? tag.getInt("State") : 0);

        if (tag.hasUUID("SavedTargetUUID")) {
            this.savedTargetUUID = tag.getUUID("SavedTargetUUID");
        } else {
            this.savedTargetUUID = null;
        }

        if (tag.hasUUID("SavedKillerUUID")) {
            this.savedKillerUUID = tag.getUUID("SavedKillerUUID");
        } else {
            this.savedKillerUUID = null;
        }

        this.squadArrivalTicks = tag.contains("SquadArrivalTicks") ? tag.getInt("SquadArrivalTicks") : -1;
        this.spawnedBbqSauce = tag.getBoolean("SpawnedBbqSauce");
        this.dieTick = tag.getInt("DieTick");
        leaveTicks = tag.getInt("LeaveTicks");
        neverLeave = tag.getBoolean("NeverLeave");
        voiceCooldown = tag.getInt("VoiceCooldown");
    }

    @Override
    public void rollItem() {
        ItemStack legendaryStack = new ItemStack(AnnoyingVillagersModItems.LEGENDARY_SWORD.get());
        legendaryStack.enchant(Enchantments.SHARPNESS, 5);
        legendaryStack.enchant(Enchantments.SMITE, 5);
        legendaryStack.enchant(Enchantments.SWEEPING_EDGE, 5);

        ItemStack tridentStack = new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get());
        tridentStack.enchant(Enchantments.SHARPNESS, 5);
        tridentStack.enchant(Enchantments.SMITE, 5);
        tridentStack.enchant(Enchantments.SWEEPING_EDGE, 5);
        if (this.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
            this.setItemInHand(InteractionHand.MAIN_HAND, legendaryStack);
            this.setItemInHand(InteractionHand.OFF_HAND, tridentStack);
        } else {
            this.setItemInHand(InteractionHand.MAIN_HAND, tridentStack);
            this.setItemInHand(InteractionHand.OFF_HAND, tridentStack);
        }
        this.swapWeaponCooldown = new Random().nextInt(600, 900);
    }

    private void syncChestplateHealingFoil() {
        ItemStack chest = this.getItemBySlot(EquipmentSlot.CHEST);
        if (BlueDemonChestplateItem.isBlueDemonChestplate(chest)) {
            BlueDemonChestplateItem.setBlueDemonHealingFoil(chest, this.healingTick != 0);
        }
    }

    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor serverlevelaccessor, @NotNull DifficultyInstance difficultyinstance, @NotNull MobSpawnType mobspawntype, @Nullable SpawnGroupData spawngroupdata, @Nullable CompoundTag compoundtag) {
        SpawnGroupData data = super.finalizeSpawn(serverlevelaccessor, difficultyinstance, mobspawntype, spawngroupdata, compoundtag);
        this.setLeftHanded(false);
        if (!this.level().isClientSide()) {
            TeamUtil.addOrJoinTeam(this, "blue_demon");
        }
        if (mobspawntype == MobSpawnType.NATURAL || mobspawntype == MobSpawnType.CHUNK_GENERATION) {
            ServerLevel serverLevel = serverlevelaccessor.getLevel();
            BluedemonData bluedemonData = BluedemonData.get(serverLevel);

            if (!bluedemonData.tryClaim(serverLevel, this.getUUID())) {
                this.discard();
                return data;
            }

            BlockPos blockPos = this.getOnPos();
            int surfaceY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos).getY();
            BlockPos spawnPos = new BlockPos(blockPos.getX(), surfaceY, blockPos.getZ());
            this.moveTo(spawnPos, this.getYRot(), this.getXRot());
        }
        int min = AnnoyingVillagersConfig.BLUE_DEMON_LEAVE_MIN_TIME.get();
        int max = AnnoyingVillagersConfig.BLUE_DEMON_LEAVE_MAX_TIME.get();
        int randomMin = Math.min(min, max);
        int randomMax = Math.max(min, max);
        this.leaveTicks = (randomMin + new Random().nextInt(randomMax - randomMin + 1)) * 60 * 20;
        return data;
    }

    public static boolean canSpawn(EntityType<BlueDemonEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        if (!serverLevel.isThundering()) return false;
        if (serverLevel.isNight()) {
            return false;
        }
        if (BluedemonData.get(serverLevel).isOccupied(serverLevel)) {
            return false;
        }
        return ProgressionUtil.isAtLeastDifficulty(Difficulty.HARD) && Monster.checkAnyLightMonsterSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    protected void actuallyHurt(@NotNull DamageSource pDamageSource, float pDamageAmount) {
        if (pDamageSource.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            super.actuallyHurt(pDamageSource, pDamageAmount);
            return;
        }

        if (this.isInvulnerableTo(pDamageSource)) {
            return;
        }

        pDamageAmount = ForgeHooks.onLivingHurt(this, pDamageSource, pDamageAmount);
        if (pDamageAmount <= 0.0F) {
            return;
        }

        pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSource, pDamageAmount);
        pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSource, pDamageAmount);

        float f1 = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0F);
        float absorbed = pDamageAmount - f1;
        if (absorbed > 0.0F) {
            this.setAbsorptionAmount(this.getAbsorptionAmount() - absorbed);
            if (this.getAbsorptionAmount() < 0.0F) this.setAbsorptionAmount(0.0F);
        }
        f1 = ForgeHooks.onLivingDamage(this, pDamageSource, f1);
        f1 = this.applyBurstProtection(this, pDamageSource, f1);
        if (this.level() instanceof ServerLevel
                && this.getState() == 0 && (this.getHealth() - f1) <= 1.0F) {
            this.setHealth(1.0F);
            BlueDemonTridentItem.addStormEnergy(this.getMainHandItem(), 100);
            BlueDemonTridentItem.addStormEnergy(this.getOffhandItem(), 100);

            /*
             * ForgeHooks.onLivingDamage(...) has already fired above. That event can
             * apply a rig stun from the very same hit that pushed Blue Demon to 1 HP.
             * A normal RigAnimationController.play(...) refuses to start while stunned,
             * which used to leave him stranded at 1 HP.
             */
            beginTridentFestivalPhase();
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel
                && this.getState() == 3
                && (this.getHealth() - f1) <= 1.0F) {
            this.startFinalDeathSequence(serverLevel, pDamageSource);
            return;
        }
        if (f1 <= 0.0F) {
            return;
        }
        this.getCombatTracker().recordDamage(pDamageSource, f1);
        this.setHealth(this.getHealth() - f1);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    private void beginTridentFestivalPhase() {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }

        /*
         * The lethal hit may already have entered RigStunController through
         * ForgeHooks.onLivingDamage before the 1 HP phase check runs.
         *
         * Clear that stun first. Its delayed cleanup task is reference-checked, so
         * clearing it here is safe and prevents the stun from restoring NoAI later.
         */
        RigStunController.clear(this);

        RigAnimationId activeAnimation = RigAnimationController.getActiveAnimationId(this);
        if (activeAnimation != null
                && activeAnimation != RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL) {
            RigAnimationController.stop(this, activeAnimation);
        }

        /*
         * Enter the protected transition before giving normal AI another tick.
         * NoAI remains true through state 1 and state 2; finishStateTwoTransform()
         * restores it when Blue Demon reaches state 3.
         */
        this.setState(1);
        this.setNoAi(true);

        playTridentFestivalAnimation();
    }

    private void ensureTridentFestivalPhase() {
        if (!(this.level() instanceof ServerLevel) || this.getState() != 1) {
            return;
        }

        if (RigAnimationController.getActiveAnimationId(this)
                == RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL) {
            return;
        }

        /*
         * Recovery path for an interrupted animation or a world reload.
         * ACTIVE_ANIMATIONS is runtime-only, while Blue Demon's State is saved to NBT.
         * Without this watchdog, loading a state-1 Blue Demon could make him
         * permanently damage-blocked at 1 HP with no Festival hooks left to run.
         */
        beginTridentFestivalPhase();
    }

    private void playTridentFestivalAnimation() {
//        ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.TRIDENT_FESTIVAL, 0.0F);
//        }

//        CREATE VANILLA_ANIMATION
        if (RigAnimationController.getActiveAnimationId(this)
                == RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL) {
            return;
        }

        RigAnimationController.play(this, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            BluedemonData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
        }
    }

    public static Builder addEpicFightAttributes(Builder builder) {
//      ADD THIS CODE IN AV_EFM
//        return builder.add(EpicFightAttributes.IMPACT.get(), 4.0D)
//                .add(EpicFightAttributes.ARMOR_NEGATION.get(), 10.0D)
//                .add(EpicFightAttributes.STUN_ARMOR.get(), 20.0D)
//                .add(EpicFightAttributes.MAX_STRIKES.get(), 100.0D)
//                .add(EpicFightAttributes.MAX_STAMINA.get(), 60.0D)
//                .add(EpicFightAttributes.STAMINA_REGEN.get(), 1.5D);

        return builder;
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        return addEpicFightAttributes(builder);
    }
}
