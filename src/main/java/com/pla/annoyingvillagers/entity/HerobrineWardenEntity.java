package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.ForceTickEntity;
import com.pla.annoyingvillagers.util.CommonUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.UUID;

public class HerobrineWardenEntity extends Warden implements ForceTickEntity {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState eatingAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    private int eatingAnimationTimeout = 0;
    private EliteHerobrineKnockedEntity eatingHerobrine;
    private UUID eatingUUID;
    private boolean burrowStarted = false;
    private int burrowRemoveAt = -1;
    private static final int DIG_TICKS = 100;
    private static final EntityDataAccessor<Boolean> DATA_BONE_OPEN =
            SynchedEntityData.defineId(HerobrineWardenEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean infectedSculk = false;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BONE_OPEN, false);
    }

    private boolean isBoneOpen() {
        return this.entityData.get(DATA_BONE_OPEN);
    }

    private void setBoneOpen(boolean open) {
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_BONE_OPEN, open);
        }
    }

    public void setEatingUUID(UUID eatingUUID) {
        this.eatingUUID = eatingUUID;
    }

    public HerobrineWardenEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setupIdleAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    private void setupEatingAnimationStates() {
        if (this.eatingAnimationTimeout <= 0) {
            this.eatingAnimationTimeout = 40;
            this.eatingAnimationState.start(this.tickCount);
        } else {
            --this.eatingAnimationTimeout;
        }
    }

    public void burrowThenDespawn() {
        if (this.level().isClientSide() || burrowStarted) return;
        burrowStarted = true;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        Brain<Warden> brain = this.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        brain.eraseMemory(MemoryModuleType.SNIFF_COOLDOWN);
        brain.eraseMemory(MemoryModuleType.DIG_COOLDOWN);

        this.setPose(Pose.DIGGING);
        burrowRemoveAt = this.tickCount + DIG_TICKS + 1;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level().isClientSide()) return;

        if (eatingHerobrine == null && eatingUUID != null) {
            Entity e = ((ServerLevel) level()).getEntity(eatingUUID);
            if (e instanceof EliteHerobrineKnockedEntity herobrine && herobrine.isAlive()) {
                eatingHerobrine = herobrine;
            }
        }

        if (eatingUUID == null || eatingHerobrine == null || !eatingHerobrine.isAlive()) {
            return;
        }

        LivingEntity brainTarget = this.getTarget();
        if (brainTarget == null || !eatingUUID.equals(brainTarget.getUUID())) {
            this.setAttackTarget(eatingHerobrine);
        }

        int bump = AngerLevel.ANGRY.getMinimumAnger() + 20;
        this.increaseAngerAt(eatingHerobrine, bump, false);

        this.getEntityAngryAt().ifPresent(angry -> {
            if (!eatingUUID.equals(angry.getUUID())) {
                this.clearAnger(angry);
            }
        });

        var brain = this.getBrain();
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        brain.eraseMemory(MemoryModuleType.RECENT_PROJECTILE);

        WardenAi.updateActivity(this);
        brain.getMemory(MemoryModuleType.ATTACK_TARGET);
        this.getEntityAngryAt().map(Entity::getUUID);
    }

    @Override
    public boolean canTargetEntity(@org.jetbrains.annotations.Nullable Entity e) {
        if (!(e instanceof LivingEntity living)) return false;
        if (eatingUUID == null) return false;
        return eatingUUID.equals(living.getUUID()) && super.canTargetEntity(e);
    }

    @Override
    public void setAttackTarget(@NotNull LivingEntity target) {
        if (eatingUUID != null && !eatingUUID.equals(target.getUUID())) {
            return;
        }
        super.setAttackTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount == 1 && !level().isClientSide() && !infectedSculk) {
            final RandomSource randomSource = this.getRandom();

            final int attempts = new Random().nextInt(25, 50);
            for (int i = 0; i < attempts; i++) {
                int dx = Mth.nextInt(randomSource, -5, 5);
                int dz = Mth.nextInt(randomSource, -5, 5);

                BlockPos pos = new BlockPos((int) (this.getX() + dx), this.getOnPos().getY(), (int) (this.getZ() + dz));
                BlockState current = level().getBlockState(pos);

                if (current.isAir()) continue;
                if (!current.getFluidState().isEmpty()) continue;
                if (current.getDestroySpeed(level(), pos) == -1.0F) continue;
                if (current.hasBlockEntity()) continue;

                BlockState newState = (randomSource.nextFloat() < 0.20f)
                        ? Blocks.SCULK_CATALYST.defaultBlockState()
                        : Blocks.SCULK.defaultBlockState();

                level().setBlock(pos, newState, 3);
            }
            infectedSculk = true;
        }
        if (level().isClientSide) {
            this.setupIdleAnimationStates();
            if (isBoneOpen()) {
                this.setupEatingAnimationStates();
            }
        }
        if (burrowStarted && this.tickCount >= burrowRemoveAt) {
            super.remove(RemovalReason.DISCARDED);
        }
        if (eatingHerobrine != null && !eatingHerobrine.isAlive()) {
            eatingHerobrine = null;
            eatingUUID = null;
            if (isBoneOpen()) setBoneOpen(false);
            burrowThenDespawn();
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (!level().isClientSide() && !isBoneOpen()) {
            setBoneOpen(true);
        }
        return super.doHurtTarget(pEntity);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (eatingUUID != null) {
            tag.putUUID("EatingUUID", eatingUUID);
        }
        tag.putBoolean("BurrowStarted", burrowStarted);
        tag.putInt("BurrowRemoveAt", burrowRemoveAt);
        tag.putBoolean("InfectedSculk", infectedSculk);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("EatingUUID")) {
            eatingUUID = tag.getUUID("EatingUUID");
        }
        burrowStarted = tag.getBoolean("BurrowStarted");
        burrowRemoveAt = tag.getInt("BurrowRemoveAt");
        infectedSculk = tag.getBoolean("InfectedSculk");
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_POSE.equals(key)) {
            if (this.hasPose(Pose.DIGGING)) {
                this.diggingAnimationState.start(this.tickCount);
            }
        }

        if (level().isClientSide() && DATA_BONE_OPEN.equals(key)) {
            if (isBoneOpen()) {
                this.eatingAnimationTimeout = 0;
                this.setupEatingAnimationStates();
            } else {
                this.eatingAnimationState.stop();
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData returnSpawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        this.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);

        this.setPose(Pose.EMERGING);
        this.getBrain().setMemoryWithExpiry(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, WardenAi.EMERGE_DURATION);

        return returnSpawnGroupData;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypes.FELL_OUT_OF_WORLD)
                || pSource.is(DamageTypes.GENERIC_KILL)) {
            return super.hurt(pSource, pAmount);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            CommonUtil.damageBlocked(pSource, this, serverLevel);
        }
        return false;
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();

        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.26D);
        builder = builder.add(Attributes.MAX_HEALTH, 500.0D);
        builder = builder.add(Attributes.ARMOR, 20.0D);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 0.0D);
        builder = builder.add(Attributes.FOLLOW_RANGE, 24.0D);
        return builder;
    }
}
