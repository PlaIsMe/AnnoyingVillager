package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.FakePlayer;
import com.pla.annoyingvillagers.clazz.BurstProtectEntity;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.init.*;
import com.pla.annoyingvillagers.network.ClientboundHerobrinePortalFx;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.util.*;
import com.pla.annoyingvillagers.clazz.HerobrineMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class LowHerobrineCloneEntity extends FakePlayer implements BurstProtectEntity {
    private boolean summoned = false;
    private boolean initialSpawn = true;
    private boolean autoKill = false;
    private HerobrineMob possessedByEntity;
    private UUID possessedByUuid;
    private boolean bound = false;
    private boolean healing = false;
    private EliteHerobrineKnockedEntity protectEntity;
    private UUID protectUUID;
    boolean renderPortal = false;

    public boolean isHealing() {
        return healing;
    }

    public HerobrineMob getPossessedByEntity() {
        return possessedByEntity;
    }

    public void setRenderPortal(boolean renderPortal) {
        this.renderPortal = renderPortal;
    }

    public boolean isSummoned() {
        return summoned;
    }

    public void setSummoned(boolean summoned) {
        this.summoned = summoned;
    }

    public void setInitialSpawn(boolean initialSpawn) {
        this.initialSpawn = initialSpawn;
    }

    public void setPossessedByUuid(UUID possessedByUuid) {
        this.possessedByUuid = possessedByUuid;
    }

    public void setPossessedByEntity(HerobrineMob possessedByEntity) {
        if (!isValidPossessedMaster(possessedByEntity)) {
            this.possessedByEntity = null;
            this.possessedByUuid = null;
            return;
        }
        this.possessedByEntity = possessedByEntity;
    }

    private static boolean isValidPossessedMaster(@Nullable Entity entity) {
        return entity instanceof HerobrineMob
                && !(entity instanceof TransporterHerobrineCloneEntity)
                && !(entity instanceof HerobrineGregEntity);
    }

    public void setHealing(boolean healing) {
        this.healing = healing;
    }

    public LowHerobrineCloneEntity(EntityType<? extends LowHerobrineCloneEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(3.0F);
        this.xpReward = 50;
        this.setPersistenceRequired();
        this.setCustomNameVisible(false);
    }

    public LowHerobrineCloneEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.LOW_HEROBRINE_CLONE.get(), level);
    }

    @Override
    public boolean shouldIgnoreBurstProtection(LivingEntity self,DamageSource source) {
        return true;
    }

    public boolean hurt(@NotNull DamageSource damageSource, float f) {
        if (damageSource.is(DamageTypes.FALL)) return super.hurt(damageSource, f);
        if (healing) {
            if (new Random().nextBoolean()
                    && this.level() instanceof ServerLevel serverLevel) {
                CommonUtil.damageBlocked(damageSource, this, serverLevel);
                return false;
            } else {
                float health = this.getHealth();
                if (health - f <= 5.0F && this.healing) {
                    protectEntity = null;
                    protectUUID = null;
                    autoKill = true;
                    this.kill();
                    return false;
                } else {
                    return super.hurt(damageSource, f / 2.0F);
                }
            }
        }
        if (damageSource.is(DamageTypes.CACTUS)) return false;
        if (damageSource.is(DamageTypes.WITHER)) return false;
        if (damageSource.is(DamageTypes.DROWN)) return false;
        if (damageSource.is(DamageTypes.WITHER_SKULL)) return false;
        if (damageSource.is(DamageTypes.DRAGON_BREATH)) return false;
        float health = this.getHealth();
        if (health - f <= 5.0F && this.healing) {
            protectEntity = null;
            protectUUID = null;
            autoKill = true;
            healing = false;
            this.kill();
            return false;
        } else {
            return super.hurt(damageSource, f / 2.0F);
        }
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("§5Low Herobrine Clone§r");
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void registerGoals() {
        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();
        this.goalSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                return protectEntity != null && protectEntity.isAlive() && distanceTo(protectEntity) > (float)10.0D * 0.9F;
            }

            @Override
            public void tick() {
                if (protectEntity != null && protectEntity.isAlive()) {
                    getNavigation().moveTo(protectEntity, 2.0D);
                    getLookControl().setLookAt(protectEntity, 30.0F, 30.0F);
                    if (distanceToSqr(protectEntity) > 10.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(protectEntity, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return protectEntity != null && protectEntity.isAlive() && distanceTo(protectEntity) > 50.0D;
            }
        });
        this.goalSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                return possessedByEntity != null && possessedByEntity.isAlive() && distanceTo(possessedByEntity) > (float)20.0D * 0.9F;
            }

            @Override
            public void tick() {
                if (possessedByEntity != null && possessedByEntity.isAlive()) {
                    getNavigation().moveTo(possessedByEntity, 2.0D);
                    getLookControl().setLookAt(possessedByEntity, 30.0F, 30.0F);
                    if (distanceToSqr(possessedByEntity) > 20.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(possessedByEntity, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return possessedByEntity != null && possessedByEntity.isAlive() && distanceTo(possessedByEntity) > 50.0D;
            }
        });
        CommonGoals.registerGoalForHostileNpc(this);
    }

    public @NotNull MobType getMobType() {
        return MobType.UNDEAD;
    }

    public boolean removeWhenFarAway(double d0) {
        return false;
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    public @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.hurt")));
    }

    public @NotNull SoundEvent getDeathSound() {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.death")));
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (!autoKill) {
                InfectedPlayerNpcEntity corpse = new InfectedPlayerNpcEntity(AnnoyingVillagersModEntities.INFECTED_PLAYER_NPC.get(), serverLevel);
                corpse.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                String killedName = this.getCustomName().getString();
                corpse.getPersistentData().putString("possessed_by", "low_herobrine_clone");
                corpse.setUsername(killedName);
                corpse.setCustomName(Component.literal(killedName));
                corpse.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null, null);
                this.setInvisible(true);
                this.remove(RemovalReason.KILLED);
                corpse.setItemSlot(EquipmentSlot.HEAD, this.getItemBySlot(EquipmentSlot.HEAD).copy());
                corpse.setItemSlot(EquipmentSlot.CHEST, this.getItemBySlot(EquipmentSlot.CHEST).copy());
                corpse.setItemSlot(EquipmentSlot.LEGS, this.getItemBySlot(EquipmentSlot.LEGS).copy());
                corpse.setItemSlot(EquipmentSlot.FEET, this.getItemBySlot(EquipmentSlot.FEET).copy());
                serverLevel.addFreshEntity(corpse);
            }

            ItemEntity itemEntity;
            ItemStack itemstack;

            itemstack = this.getMainHandItem();
            itemEntity = new ItemEntity(serverLevel, this.getX(), this.getY() + 1.0D, this.getZ(), itemstack);
            itemEntity.setPickUpDelay(10);
            serverLevel.addFreshEntity(itemEntity);

            itemstack = this.getOffhandItem();
            itemEntity = new ItemEntity(serverLevel, this.getX(), this.getY() + 1.0D, this.getZ(), itemstack);
            itemEntity.setPickUpDelay(10);
            serverLevel.addFreshEntity(itemEntity);
        }
        super.die(damageSource);
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor serverLevelAccessor, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag compoundTag) {
        HerobrineUtil.initialSpawn(serverLevelAccessor, this, 0, mobSpawnType);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData, compoundTag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        summoned = pCompound.getBoolean("Summoned");
        renderPortal = pCompound.getBoolean("RenderPortal");
        initialSpawn = pCompound.getBoolean("InitialSpawn");
        autoKill = pCompound.getBoolean("AutoKill");
        if (pCompound.hasUUID("ProtectUUID")) {
            protectUUID = pCompound.getUUID("ProtectUUID");
        }
        if (pCompound.hasUUID("PossessedByUuid")) {
            possessedByUuid = pCompound.getUUID("PossessedByUuid");
        }
        bound = pCompound.getBoolean("Bound");
        healing = pCompound.getBoolean("Healing");
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("Summoned", summoned);
        pCompound.putBoolean("InitialSpawn", initialSpawn);
        pCompound.putBoolean("RenderPortal", renderPortal);
        pCompound.putBoolean("AutoKill", autoKill);
        if (protectUUID != null) {
            pCompound.putUUID("ProtectUUID", protectUUID);
        }
        if (possessedByUuid != null) {
            pCompound.putUUID("PossessedByUuid", possessedByUuid);
        }
        pCompound.putBoolean("Bound", bound);
        pCompound.putBoolean("Healing", healing);
    }

    private void playHerobrineHealingAnimations() {
//      ADD THIS CODE IN AV_EFM
//        final LivingEntityPatch<?> livingentitypatch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
//        if (livingentitypatch != null && !this.level().isClientSide()) {
//            livingentitypatch.playAnimationSynchronized(AnimsSculkSteve.HEROBRINE_SACRIFICING, 0.0F);
//        }

        if (RigAnimationController.getActiveAnimationId(this) != RigAnimationId.HEROBRINE_SACRIFICING) RigAnimationController.play(this, RigAnimationId.HEROBRINE_SACRIFICING);
    }

    private void playHerobrinePossessAnimation() {
//      ADD THIS CODE IN AV_EFM
//        final LivingEntityPatch<?> livingentitypatch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
//        if (livingentitypatch != null && !this.level().isClientSide()) {
//            livingentitypatch.playAnimationSynchronized(AVAnimations.PLAYER_HEROBRINE_POSSESSION, 0.0F);
//        }
        if (RigAnimationController.getActiveAnimationId(this) != RigAnimationId.PLAYER_HEROBRINE_POSSESSION) RigAnimationController.play(this, RigAnimationId.PLAYER_HEROBRINE_POSSESSION);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.tickCount == 1) {
                if (this.initialSpawn) {
                    if (this.renderPortal) {
                        AnnoyingVillagers.PACKET_HANDLER.send(
                                PacketDistributor.TRACKING_ENTITY.with(() -> this),
                                new ClientboundHerobrinePortalFx(this.getOnPos().getCenter().add(0.0, 1.5, 0.0))
                        );
                        renderPortal = false;
                    }
                    if (this.summoned) {
                        this.setNoAi(true);
                    }
                    playHerobrinePossessAnimation();
                    this.initialSpawn = false;
                }
            }

            if (protectEntity == null && protectUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(protectUUID);
                if (entity instanceof EliteHerobrineKnockedEntity eliteHerobrineKnockedEntity) {
                    protectEntity = eliteHerobrineKnockedEntity;
                } else {
                    protectEntity = null;
                }
            }
            if (protectEntity != null && !protectEntity.isAlive()) {
                protectEntity = null;
                protectUUID = null;
                autoKill = true;
                this.kill();
            }

            if (possessedByEntity == null && possessedByUuid != null) {
                Entity entity = ((ServerLevel) level()).getEntity(possessedByUuid);
                if (isValidPossessedMaster(entity) && entity instanceof HerobrineMob herobrineMob) {
                    possessedByEntity = herobrineMob;
                } else {
                    possessedByEntity = null;
                    possessedByUuid = null;
                }
            }
            if (!bound && possessedByEntity != null
                    && possessedByEntity.isAlive()
                    && (!possessedByEntity.isSacrificing() || !possessedByEntity.isHealing())
                    && possessedByEntity.getSacrificingAnimationCooldown() == 0) {
                if (possessedByEntity.isAvailableSlot()) {
                    if (possessedByEntity.boundPossessed(this)) {
                        this.bound = true;
                    }
                }
            }
            if (possessedByEntity != null && !possessedByEntity.isAlive()) {
                AABB area = new AABB(this.blockPosition()).inflate(60);
                List<Entity> nearby = level().getEntities(this, area, entity ->
                        entity instanceof EliteHerobrineKnockedEntity
                );
                if (!nearby.isEmpty()) {
                    Entity entity = nearby.get(0);
                    if (entity instanceof EliteHerobrineKnockedEntity eliteHerobrineKnockedEntity) {
                        this.protectEntity = eliteHerobrineKnockedEntity;
                        this.protectUUID = eliteHerobrineKnockedEntity.getUUID();
                    } else {
                        possessedByEntity = null;
                        possessedByUuid = null;
                        autoKill = true;
                        this.kill();
                    }
                } else {
                    possessedByEntity = null;
                    possessedByUuid = null;
                    autoKill = true;
                    this.kill();
                }
            }
            if (this.healing) {
                if (this.getHealth() <= 2) {
                    this.healing = false;
                    autoKill = true;
                    this.kill();
                }
                CommonUtil.stunImmunity(this, 3, 3);
                playHerobrineHealingAnimations();
                if (this.tickCount % 140 == 0 && this.possessedByEntity.getHealth() < this.possessedByEntity.getMaxHealth() * 0.8) {
                    if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                        this.playSound(AnnoyingVillagersModSounds.HEROBRINE_UNDERSTOOD.get(), 0.5F, 1.0F);
                    }
                }
                if (this.tickCount % 20 == 0 && this.possessedByEntity != null) {
                    if (this.possessedByEntity.getMaxHealth() == this.possessedByEntity.getHealth()) {
                        this.healing = false;
                        autoKill = true;
                        this.kill();
                    }
                    if (this.getHealth() <= 4) {
                        this.healing = false;
                        autoKill = true;
                        this.kill();
                    } else {
                        this.setHealth(this.getHealth() - 2.0F);
                    }
                    this.possessedByEntity.heal(this.possessedByEntity.getMaxHealth() * 0.02F);
                    CombatBehaviour.forceLookAt(this, this.possessedByEntity, 60.0F, 60.0F);
                }
                if (this.possessedByEntity != null && this.possessedByEntity.isAlive()) {
                    ServerLevel server = (ServerLevel)this.level();
                    Vec3 from = getHealingArmPosition(this, Vec3.ZERO, HumanoidArm.RIGHT);
                    if (from == null) {
                        return;
                    }
                    Vec3 to = this.possessedByEntity.getEyePosition();

                    AABB box = this.possessedByEntity.getBoundingBox().inflate(0.05);
                    Vec3 end = box.clip(from, to).orElse(to);

                    Vec3 d = end.subtract(from);
                    double len = d.length();
                    if (len <= 1.0e-4) return;

                    Vec3 dir = d.scale(1.0 / len);

                    Vec3 any = Math.abs(dir.y) < 0.99 ? new Vec3(0,1,0) : new Vec3(1,0,0);
                    Vec3 u = dir.cross(any).normalize();
                    Vec3 v = dir.cross(u).normalize();

                    int steps = Mth.clamp((int)(len * 6.0), 6, 72);
                    double step = len / steps;

                    final int stride = 4;
                    int phase = (this.tickCount >> 1) % stride;
                    RandomSource r = this.getRandom();

                    for (int i = phase; i <= steps; i += stride) {
                        if (r.nextFloat() < 0.70f) continue;

                        double t = (i * step) / len;
                        double R = 0.05 + 0.20 * t;
                        double ang = r.nextDouble() * (Math.PI * 2);
                        double rad = R * Math.sqrt(r.nextDouble());
                        Vec3 off = u.scale(Math.cos(ang) * rad).add(v.scale(Math.sin(ang) * rad));

                        Vec3 p = from.add(dir.scale(i * step)).add(off);

                        double vx = dir.x * 0.02 + off.x * 0.10;
                        double vy = dir.y * 0.02 + off.y * 0.10;
                        double vz = dir.z * 0.02 + off.z * 0.10;

                        server.sendParticles(AnnoyingVillagersModParticleTypes.LIGHT.get(),
                                p.x, p.y, p.z,
                                1,
                                vx, vy, vz,
                                0.0);
                    }
                } else {
                    this.healing = false;
                    autoKill = true;
                    this.kill();
                }
            }
        }
    }

    private static Vec3 getHealingArmPosition(Entity entity, @Nullable Vec3 translation, HumanoidArm arm) {
//      ADD THIS CODE IN AV_EFM
//        float handToTip = 1.2F;
//        float yOffset = 0.0F;
//        LivingEntityPatch<?> livingEntityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
//        if (livingEntityPatch == null) return null;
//
//        float interpolation = 0.0F;
//        OpenMatrix4f m = livingEntityPatch.getArmature()
//                .getBoundTransformFor(livingEntityPatch.getAnimator().getPose(interpolation), joint);
//
//        if (translation != null) {
//            OpenMatrix4f tLocal = new OpenMatrix4f().translate(translation);
//            OpenMatrix4f.mul(m, tLocal, m);
//        }
//
//        OpenMatrix4f tipOffset = new OpenMatrix4f().translate(new Vec3f(0.0F, 0.0F, -handToTip));
//        OpenMatrix4f.mul(m, tipOffset, m);
//
//        float yawRad = (float) -Math.toRadians(livingEntityPatch.getOriginal().yBodyRotO + 180.0F);
//        OpenMatrix4f worldYaw = new OpenMatrix4f().rotate(yawRad, new Vec3f(0.0F, 1.0F, 0.0F));
//        OpenMatrix4f.mul(worldYaw, m, m);
//
//        LivingEntity base = livingEntityPatch.getOriginal();
//        return new Vec3(
//                m.m30 + base.getX(),
//                m.m31 + (base.getY() + (entity.getBbHeight() / 1.8) - 1.0) + yOffset,
//                m.m32 + base.getZ()
//        );

        if (!(entity instanceof LivingEntity living)) {
            Vec3 fallback = CommonUtil.getVanillaSwordOrBodyPosition(entity);
            return translation == null ? fallback : fallback.add(translation);
        }

        float partialTick = 1.0F;
        float bodyYaw = Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;
        double sinYaw = Mth.sin(bodyYaw);
        double cosYaw = Mth.cos(bodyYaw);
        double armSide = (arm == HumanoidArm.RIGHT ? 1.0D : -1.0D) * 0.35D;
        double forward = 0.65D;
        double crouchOffset = living.isCrouching() ? -0.1875D : 0.0D;
        double x = Mth.lerp((double) partialTick, living.xo, living.getX());
        double y = Mth.lerp((double) partialTick, living.yo, living.getY());
        double z = Mth.lerp((double) partialTick, living.zo, living.getZ());

        Vec3 armPosition = new Vec3(
                x - cosYaw * armSide - sinYaw * forward,
                y + living.getEyeHeight() - 0.55D + crouchOffset,
                z - sinYaw * armSide + cosYaw * forward
        );

        if (translation == null) {
            return armPosition;
        }

        return armPosition.add(
                cosYaw * translation.x - sinYaw * translation.z,
                translation.y,
                sinYaw * translation.x + cosYaw * translation.z
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();

        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.45D);
        builder = builder.add(Attributes.MAX_HEALTH, 40.0D);
        builder = builder.add(Attributes.ARMOR, 25.0D);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 5.0D);
        builder = builder.add(Attributes.FOLLOW_RANGE, 48.0D);
        return builder;
    }
}
