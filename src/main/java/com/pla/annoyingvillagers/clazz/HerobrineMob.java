package com.pla.annoyingvillagers.clazz;

import javax.annotation.Nullable;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.blockentity.CryingObsidianBlockEntity;
import com.pla.annoyingvillagers.blockentity.ObsidianBlockEntity;
import com.pla.annoyingvillagers.blockentity.ShadowObsidianBlockEntity;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.entity.*;
import com.pla.annoyingvillagers.entity.goal.HerobrineHealingGoal;
import com.pla.annoyingvillagers.entity.goal.RetargetCloserThreatGoal;
import com.pla.annoyingvillagers.entity.goal.RollItemGoal;
import com.pla.annoyingvillagers.init.*;
import com.pla.annoyingvillagers.rig.LockableRigAttackAnimation;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigStunEscapeEntity;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.network.ClientboundHerobrineAssistanceFx;
import com.pla.annoyingvillagers.network.ClientboundHerobrinePortalFx;
import com.pla.annoyingvillagers.spawnhandler.HerobrineMobData;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.pla.annoyingvillagers.util.HerobrinePortalUtil.*;

public class HerobrineMob extends Monster implements ForceTickEntity, BurstProtectEntity, CombatVoiceLineEntity, LockableRigAttackAnimation, RigStunEscapeEntity, DangerousReaction {
    private boolean renderPortal = false;
    private int recallTicks = 0;
    private String chatName;
    private boolean neverRecall = false;
    private UUID gregUUID = null;
    private boolean initialSpawn = true;
    private BlockPos lastFeetPos = null;
    private EliteHerobrineKnockedEntity protectEntity;
    private UUID protectUUID;
    private int sacrificingAnimationCooldown = 0;
    private boolean sacrificing = false;
    private boolean healing = false;
    private int healingCooldown;
    private int stunEscapeCooldown = 0;
    private Entity blockDamage = null;
    private int swapWeaponCooldown;
    private int voiceCooldown = 0;
    private int rigAttackAnimationLockCount;
    private boolean spinningWeaponInitActive;

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

    private Entity firstPossessedHerobrine;
    private UUID firstPossessedHerobrineUuid;

    private Entity secondPossessedHerobrine;
    private UUID secondPossessedHerobrineUuid;

    private Entity thirdPossessedHerobrine;
    private UUID thirdPossessedHerobrineUuid;

    private Entity fourthPossessedHerobrine;
    private UUID fourthPossessedHerobrineUuid;

    private static final int MIN_SECOND_FORM_COOLDOWN_TICKS = 600;
    private static final int RANDOM_SECOND_FORM_COOLDOWN_TICKS = 601;

    // 0: normal
    // 1: limited second-form actions
    // 2: fully in second form
    private int state = 0;
    private int secondFormHitLeft;
    private int secondFormCooldown;

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getSecondFormHitLeft() {
        return this.secondFormHitLeft;
    }

    public void setSecondFormHitLeft(int secondFormHitLeft) {
        this.secondFormHitLeft = Math.max(0,secondFormHitLeft);
    }

    public int getSecondFormCooldown() {
        return this.secondFormCooldown;
    }

    public void setSecondFormCooldown(int secondFormCooldown) {
        this.secondFormCooldown = Math.max(0,secondFormCooldown);
    }

    public void resetSecondFormCooldown() {
        this.secondFormCooldown = MIN_SECOND_FORM_COOLDOWN_TICKS + this.getRandom().nextInt(RANDOM_SECOND_FORM_COOLDOWN_TICKS);
    }

    public boolean canStartSecondFormAction() {
        return switch (this.state) {
            case 0 -> this.secondFormCooldown == 0 && this.secondFormHitLeft == 0;
            case 1 -> this.secondFormHitLeft > 0;
            case 2 -> true;
            default -> false;
        };
    }

    public boolean canUseSecondFormAction() {
        return this.state == 2 || this.state == 1 && this.secondFormHitLeft > 0;
    }

    public boolean beginSecondFormActionWindow() {
        if (this.state != 0) return false;
        if (this.secondFormCooldown > 0 || this.secondFormHitLeft > 0) return false;
        this.state = 1;
        this.secondFormHitLeft = 2 + this.getRandom().nextInt(2);
        this.onSecondFormActionWindowOpened();
        return true;
    }

    public void consumeSecondFormAction() {
        if (this.state != 1 || this.secondFormHitLeft <= 0) return;
        this.secondFormHitLeft--;
        if (this.secondFormHitLeft <= 0) {
            this.secondFormHitLeft = 0;
            this.state = 0;
            this.resetSecondFormCooldown();
        }
    }

    protected void onSecondFormActionWindowOpened() {
    }

    public void setHealingCooldown() {
        this.healingCooldown = random.nextInt(300, 600);
    }

    public int getHealingCooldown() {
        return healingCooldown;
    }

    public Entity getFirstPossessedHerobrine() {
        return firstPossessedHerobrine;
    }

    public Entity getSecondPossessedHerobrine() {
        return secondPossessedHerobrine;
    }

    public Entity getThirdPossessedHerobrine() {
        return thirdPossessedHerobrine;
    }

    public Entity getFourthPossessedHerobrine() {
        return fourthPossessedHerobrine;
    }

    public List<Entity> getAliveBoundPossessedHerobrines() {
        List<Entity> bound = new ArrayList<>(4);
        if (this.firstPossessedHerobrine != null && this.firstPossessedHerobrine.isAlive()) bound.add(this.firstPossessedHerobrine);
        if (this.secondPossessedHerobrine != null && this.secondPossessedHerobrine.isAlive()) bound.add(this.secondPossessedHerobrine);
        if (this.thirdPossessedHerobrine != null && this.thirdPossessedHerobrine.isAlive()) bound.add(this.thirdPossessedHerobrine);
        if (this.fourthPossessedHerobrine != null && this.fourthPossessedHerobrine.isAlive()) bound.add(this.fourthPossessedHerobrine);
        return bound;
    }

    public int getSacrificingAnimationCooldown() {
        return sacrificingAnimationCooldown;
    }

    public void rollItem() {
        this.swapWeaponCooldown = new Random().nextInt(100, 200);
    }

    public boolean isAvailableSlot() {
        return firstPossessedHerobrineUuid == null || secondPossessedHerobrineUuid == null || thirdPossessedHerobrineUuid == null || fourthPossessedHerobrineUuid == null;
    }

    private int getEmptyBoundClone() {
        int returnValue = 0;
        if (firstPossessedHerobrineUuid == null) returnValue = returnValue + 1;
        if (secondPossessedHerobrineUuid == null) returnValue = returnValue + 1;
        if (thirdPossessedHerobrineUuid == null) returnValue = returnValue + 1;
        if (fourthPossessedHerobrineUuid == null) returnValue = returnValue + 1;
        return returnValue;
    }

    public boolean boundPossessed(Entity entity) {
        if (firstPossessedHerobrineUuid == null) {
            firstPossessedHerobrineUuid = entity.getUUID();
            firstPossessedHerobrine = entity;
            return true;
        } else if (secondPossessedHerobrineUuid == null) {
            secondPossessedHerobrineUuid = entity.getUUID();
            secondPossessedHerobrine = entity;
            return true;
        } else if (thirdPossessedHerobrineUuid == null) {
            thirdPossessedHerobrineUuid = entity.getUUID();
            thirdPossessedHerobrine = entity;
            return true;
        } else if (fourthPossessedHerobrineUuid == null) {
            fourthPossessedHerobrineUuid = entity.getUUID();
            fourthPossessedHerobrine = entity;
            return true;
        } else {
            return false;
        }
    }

    public void setProtectUUID(UUID protectUUID) {
        this.protectUUID = protectUUID;
    }

    public void setProtectEntity(EliteHerobrineKnockedEntity protectEntity) {
        this.protectEntity = protectEntity;
    }

    public void setGregUUID(UUID gregUUID) {
        this.gregUUID = gregUUID;
    }

    public UUID getGregUUID() {
        return gregUUID;
    }

    public void setRecallTicks(int recallTicks) {
        this.recallTicks = recallTicks;
    }

    public int getRecallTicks() {
        return recallTicks;
    }

    public void setRenderPortal(boolean renderPortal) {
        this.renderPortal = renderPortal;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public void setNeverRecall(boolean neverRecall) {
        this.neverRecall = neverRecall;
    }

    public void setInitialSpawn(boolean initialSpawn) {
        this.initialSpawn = initialSpawn;
    }

    public boolean isSacrificing() {
        return sacrificing;
    }

    public boolean isHealing() {
        return healing;
    }

    public void setHealing(boolean healing) {
        this.healing = healing;
    }

    protected HerobrineMob(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        this.resetSecondFormCooldown();
    }

    public static class AnyFluidPathNavigation extends GroundPathNavigation {
        public AnyFluidPathNavigation(Mob mob, Level level) {
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
            if (type == BlockPathTypes.WATER
                    || type == BlockPathTypes.WATER_BORDER
                    || type == BlockPathTypes.LAVA
                    || type == BlockPathTypes.DANGER_FIRE
                    || type == BlockPathTypes.DAMAGE_FIRE) {
                return true;
            }
            return super.hasValidPathType(type);
        }

        @Override
        public boolean isStableDestination(@NotNull BlockPos blockPos) {
            return this.level.getFluidState(blockPos).getType() != Fluids.EMPTY || super.isStableDestination(blockPos);
        }
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private Entity getHealingHerobrine() {
        if (isHealing()) {
            if (firstPossessedHerobrine != null) {
                if (firstPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity && lowShadowHerobrineCloneEntity.isHealing()) {
                    return lowShadowHerobrineCloneEntity;
                }
                if (firstPossessedHerobrine instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity && lowHerobrineCloneEntity.isHealing()) {
                    return lowHerobrineCloneEntity;
                }
            }
            if (secondPossessedHerobrine != null) {
                if (secondPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity && lowShadowHerobrineCloneEntity.isHealing()) {
                    return lowShadowHerobrineCloneEntity;
                }
                if (secondPossessedHerobrine instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity && lowHerobrineCloneEntity.isHealing()) {
                    return lowHerobrineCloneEntity;
                }
            }
            if (thirdPossessedHerobrine != null) {
                if (thirdPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity && lowShadowHerobrineCloneEntity.isHealing()) {
                    return lowShadowHerobrineCloneEntity;
                }
                if (thirdPossessedHerobrine instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity && lowHerobrineCloneEntity.isHealing()) {
                    return lowHerobrineCloneEntity;
                }
            }
            if (fourthPossessedHerobrine != null) {
                if (fourthPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity && lowShadowHerobrineCloneEntity.isHealing()) {
                    return lowShadowHerobrineCloneEntity;
                }
                if (fourthPossessedHerobrine instanceof LowHerobrineCloneEntity lowHerobrineCloneEntity && lowHerobrineCloneEntity.isHealing()) {
                    return lowHerobrineCloneEntity;
                }
            }
        }
        return null;
    }

    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new RetargetCloserThreatGoal(this));
        CommonGoals.registerDangerousReactionGoals(this);
        this.goalSelector.addGoal(1, new HerobrineHealingGoal(this));
        if (this instanceof RollItemUser) {
            this.goalSelector.addGoal(1, new RollItemGoal(this));
        }
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
                return protectEntity != null && getHealingHerobrine() != null && getHealingHerobrine().isAlive() && distanceTo(getHealingHerobrine()) > (float)10.0D * 0.9F;
            }

            @Override
            public void tick() {
                if (getHealingHerobrine() != null && getHealingHerobrine().isAlive()) {
                    getNavigation().moveTo(getHealingHerobrine(), 2.0D);
                    if (distanceToSqr(getHealingHerobrine()) > 10.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(getHealingHerobrine(), 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return isHealing() && getHealingHerobrine() != null && getHealingHerobrine().isAlive() && distanceTo(getHealingHerobrine()) > 50.0D;
            }
        });
        CommonGoals.registerGoalForHostileNpc(this);
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !DangerousReaction.hasDangerousTarget(HerobrineMob.this) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !DangerousReaction.hasDangerousTarget(HerobrineMob.this) && super.canContinueToUse();
            }
        });
    }

    @Override
    public RigAnimationId getDangerousReactionAnimation(Mob mob) {
        return RigAnimationId.STEP_BACKWARD;
    }

    @Override
    public void afterDangerousReaction(Mob mob, ServerLevel serverLevel) {
        if (this.tickCount % 10 != 0) return;

        new DelayedTask(1) {
            @Override
            public void run() {
                if (!HerobrineMob.this.isAlive() || HerobrineMob.this.isRemoved()) return;

                if (HerobrineMob.this instanceof AegisHerobrineEntity || HerobrineMob.this instanceof SledgehammerHerobrineEntity || HerobrineMob.this instanceof SwordsmanHerobrineEntity || HerobrineMob.this instanceof GlaiveHerobrineEntity || HerobrineMob.this instanceof ReaperHerobrineEntity) {
                    RigAnimationController.play(HerobrineMob.this, RigAnimationId.POINT_LEFT_HAND_MIDDLE);
                    LivingEntity target = HerobrineMob.this.getTarget();
                    Direction dir = target != null
                            ? Direction.getNearest(target.getX() - HerobrineMob.this.getX(), 0.0D, target.getZ() - HerobrineMob.this.getZ())
                            : HerobrineMob.this.getDirection();
                    int dist = 1 + HerobrineMob.this.getRandom().nextInt(3);
                    int rot = HerobrineMob.this.getRandom().nextInt(4);
                    java.util.function.BiFunction<Integer, Integer, int[]> toWorld = DangerousReaction.getIntegerIntegerBiFunction(HerobrineMob.this, rot);
                    int lateral = HerobrineMob.this.getRandom().nextInt(3) - 1;
                    int[] dxz = toWorld.apply(lateral, 0);
                    BlockPos baseXZ = HerobrineMob.this.blockPosition().relative(dir, dist).offset(dxz[0], 0, dxz[1]);
                    int surfaceY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseXZ).getY();
                    BlockPos spawnPos = new BlockPos(baseXZ.getX(), surfaceY, baseXZ.getZ());

                    LowShadowHerobrineCloneEntity clone = new LowShadowHerobrineCloneEntity(AnnoyingVillagersModEntities.LOW_SHADOW_HEROBRINE_CLONE.get(), serverLevel);
                    clone.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, dir.toYRot(), 0.0F);
                    clone.setRenderPortal(false);
                    clone.setForEscaping(true);
                    clone.setNoAi(true);
                    clone.setPossessedByEntity(HerobrineMob.this);
                    clone.setPossessedByUuid(HerobrineMob.this.getUUID());
                    serverLevel.addFreshEntity(clone);
                }
            }
        };
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

    protected void dropCustomDeathLoot(@NotNull DamageSource damagesource, int i, boolean flag) {
        super.dropCustomDeathLoot(damagesource, i, flag);
        this.spawnAtLocation(new ItemStack(Blocks.OBSIDIAN));
    }

    public @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.hurt")));
    }

    public @NotNull SoundEvent getDeathSound() {
        return Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.death")));
    }

    public boolean causeFallDamage(float f, float f1, @NotNull DamageSource damagesource) {
        return super.causeFallDamage(f, f1, damagesource);
    }

    @Override
    public float getBurstProtectCapRatio() {
        return 0.05F;
    }

    public boolean hurt(@NotNull DamageSource damageSource, float f) {
        if (this.getPersistentData().getBoolean(NBT_RISING) || this.getPersistentData().getBoolean(NBT_SINKING) || this.sacrificing) {
            if (this.level() instanceof ServerLevel serverLevel) {
                CommonUtil.damageBlocked(damageSource, this, serverLevel);
            }
            return false;
        }
        boolean result = super.hurt(damageSource, f);
        if (result) {
            this.sayHurtSound(this, damageSource);
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result) {
            this.sayAttackSound(this, target);
        }
        return result;
    }

    private void triggerSecondForm(ServerLevel serverLevel) {
        if (this.sacrificingAnimationCooldown != 0) return;

        this.sacrificingAnimationCooldown = 80;
        this.setNoAi(true);
        if (!(this instanceof NullEntity)) {
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        if (this instanceof ShadowHerobrineEntity shadowHerobrineEntity) {
            shadowHerobrineEntity.clearDarkOb();
        }
        CommonUtil.stunImmunity(this, 80, 2);

        if (this.gregUUID != null) {
            Entity entity = serverLevel.getEntity(this.gregUUID);
            if (entity instanceof HerobrineGregEntity herobrineGregEntity && entity.isAlive()) {
                if (this instanceof ShadowHerobrineEntity) {
                    if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                        this.playSound(AnnoyingVillagersModSounds.SHADOW_HEROBRINE_SAY_ON_PHASE_2.get(), 0.5F, 1.0F);
                    }
                } else {
                    herobrineGregEntity.playSound(AnnoyingVillagersModSounds.GREG_REQUESTING_ASSISTANCE.get(), 1.0F, 1.0F);
                }
                Objects.requireNonNull(herobrineGregEntity.level().getServer()).getPlayerList().broadcastSystemMessage(
                        Component.literal("<" + Component.translatable("entity.annoyingvillagers.herobrine_greg").getString() + "> "
                                + Component.translatable("subtitles.herobrine_request").getString()), false);
                return;
            }
        }

        if (this instanceof ShadowHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.SHADOW_HEROBRINE_SAY_ON_PHASE_2.get(), 0.5F, 1.0F);
            }
        } else {
            this.playSound(AnnoyingVillagersModSounds.SELF_REQUESTING_ASSISTANCE.get(), 1.0F, 1.0F);
        }
        Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(
                Component.literal("<" + this.getChatName() + "> " + Component.translatable("subtitles.herobrine_request").getString()),
                false);
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
        if (this.level() instanceof ServerLevel serverLevel
                && this.getState() < 2
                && (this instanceof AegisHerobrineEntity
                || this instanceof SledgehammerHerobrineEntity
                || this instanceof SwordsmanHerobrineEntity
                || this instanceof ReaperHerobrineEntity
                || this instanceof GlaiveHerobrineEntity
                || this instanceof NullEntity
                || this instanceof ShadowHerobrineEntity)
                && (this.getHealth() - f1) <= 1.0F) {
            this.setHealth(1.0F);
            this.sacrificing = true;
            this.triggerSecondForm(serverLevel);
            return;
        }
        if (f1 <= 0.0F) {
            return;
        }
        this.getCombatTracker().recordDamage(pDamageSource, f1);
        this.setHealth(this.getHealth() - f1);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        swapWeaponCooldown = pCompound.getInt("SwapWeaponCooldown");
        recallTicks = pCompound.getInt("RecallTicks");
        renderPortal = pCompound.getBoolean("RenderPortal");
        neverRecall = pCompound.getBoolean("NeverRecall");
        if (pCompound.contains("GregUUID")) {
            gregUUID = pCompound.getUUID("GregUUID");
        }
        initialSpawn = pCompound.getBoolean("InitialSpawn");
        if (pCompound.hasUUID("ProtectUUID")) {
            protectUUID = pCompound.getUUID("ProtectUUID");
        }
        if (pCompound.hasUUID("FirstPossessedHerobrineUuid")) {
            firstPossessedHerobrineUuid = pCompound.getUUID("FirstPossessedHerobrineUuid");
        }
        if (pCompound.hasUUID("SecondPossessedHerobrineUuid")) {
            secondPossessedHerobrineUuid = pCompound.getUUID("SecondPossessedHerobrineUuid");
        }
        if (pCompound.hasUUID("ThirdPossessedHerobrineUuid")) {
            thirdPossessedHerobrineUuid = pCompound.getUUID("ThirdPossessedHerobrineUuid");
        }
        if (pCompound.hasUUID("FourthPossessedHerobrineUuid")) {
            fourthPossessedHerobrineUuid = pCompound.getUUID("FourthPossessedHerobrineUuid");
        }
        sacrificing = pCompound.getBoolean("Sacrificing");
        healing = pCompound.getBoolean("Healing");
        sacrificingAnimationCooldown = pCompound.getInt("SacrificingAnimationCooldown");
        state = pCompound.getInt("State");
        secondFormHitLeft = Math.max(0,pCompound.getInt("SecondFormHitLeft"));
        if (pCompound.contains("SecondFormCooldown")) secondFormCooldown = Math.max(0,pCompound.getInt("SecondFormCooldown"));
        healingCooldown = pCompound.getInt("HealingCooldown");
        voiceCooldown = pCompound.getInt("VoiceCooldown");
    }

    public void jump() {
        this.jumpFromGround();
        Vec3 motion = this.getDeltaMovement();
        Vec3 forward = this.getForward();
        double strength = new Random().nextDouble(0.2, 0.4);
        this.setDeltaMovement(
                motion.x + forward.x * strength,
                motion.y,
                motion.z + forward.z * strength
        );
        this.hasImpulse = true;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("SwapWeaponCooldown", this.swapWeaponCooldown);
        pCompound.putInt("RecallTicks", recallTicks);
        pCompound.putBoolean("RenderPortal", renderPortal);
        pCompound.putBoolean("NeverRecall", neverRecall);
        if (gregUUID != null) {
            pCompound.putUUID("GregUUID", gregUUID);
        }
        pCompound.putBoolean("InitialSpawn", initialSpawn);
        if (protectUUID != null) {
            pCompound.putUUID("ProtectUUID", protectUUID);
        }
        if (firstPossessedHerobrineUuid != null) {
            pCompound.putUUID("FirstPossessedHerobrineUuid", firstPossessedHerobrineUuid);
        }
        if (secondPossessedHerobrineUuid != null) {
            pCompound.putUUID("SecondPossessedHerobrineUuid", secondPossessedHerobrineUuid);
        }
        if (thirdPossessedHerobrineUuid != null) {
            pCompound.putUUID("ThirdPossessedHerobrineUuid", thirdPossessedHerobrineUuid);
        }
        if (fourthPossessedHerobrineUuid != null) {
            pCompound.putUUID("FourthPossessedHerobrineUuid", fourthPossessedHerobrineUuid);
        }
        pCompound.putBoolean("Sacrificing", sacrificing);
        pCompound.putBoolean("Healing", healing);
        pCompound.putInt("SacrificingAnimationCooldown", sacrificingAnimationCooldown);
        pCompound.putInt("State", state);
        pCompound.putInt("SecondFormHitLeft", secondFormHitLeft);
        pCompound.putInt("SecondFormCooldown", secondFormCooldown);
        pCompound.putInt("HealingCooldown", healingCooldown);
        pCompound.putInt("VoiceCooldown", this.voiceCooldown);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new AnyFluidPathNavigation(this, level);
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

    private void playFallAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().applyStun(StunType.FALL, 0.0F);
//        }
        RigStunController.applyLanding(this);
    }

    private void recoverAfterSacrificing() {
        this.sacrificing = false;
        this.sacrificingAnimationCooldown = 0;
        this.setNoAi(false);
        this.removeAllEffects();
        playFallAnimation();
        if (this instanceof AegisHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.ELITE_HEROBRINE_SAY_SECOND_FORM_RELEASE.get(), 0.5F, 1.0F);
            }
            ItemStack enderAegis = new ItemStack(AnnoyingVillagersModItems.ENDER_AEGIS.get());
            enderAegis.enchant(Enchantments.SHARPNESS, 3);
            enderAegis.enchant(Enchantments.SWEEPING_EDGE, 3);
            enderAegis.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, enderAegis);
        }
        if (this instanceof SwordsmanHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.ELITE_HEROBRINE_SAY_SECOND_FORM_RELEASE.get(), 0.5F, 1.0F);
            }
            ItemStack demoniacVoltageReaver = new ItemStack(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get());
            demoniacVoltageReaver.enchant(Enchantments.SHARPNESS, 3);
            demoniacVoltageReaver.enchant(Enchantments.SWEEPING_EDGE, 3);
            demoniacVoltageReaver.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, demoniacVoltageReaver);
        }
        if (this instanceof SledgehammerHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.ELITE_HEROBRINE_SAY_SECOND_FORM_RELEASE.get(), 0.5F, 1.0F);
            }
            ItemStack obsidianSledgehammer = new ItemStack(AnnoyingVillagersModItems.OBSIDIAN_SLEDGEHAMMER.get());
            obsidianSledgehammer.enchant(Enchantments.SHARPNESS, 3);
            obsidianSledgehammer.enchant(Enchantments.SWEEPING_EDGE, 3);
            obsidianSledgehammer.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, obsidianSledgehammer);
        }
        if (this instanceof GlaiveHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.ELITE_HEROBRINE_SAY_SECOND_FORM_RELEASE.get(), 0.5F, 1.0F);
            }
            ItemStack enderGlaive = new ItemStack(AnnoyingVillagersModItems.ENDER_GLAIVE.get());
            enderGlaive.enchant(Enchantments.SHARPNESS, 3);
            enderGlaive.enchant(Enchantments.SWEEPING_EDGE, 3);
            enderGlaive.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, enderGlaive);
        }
        if (this instanceof ReaperHerobrineEntity reaperHerobrineEntity) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.ELITE_HEROBRINE_SAY_SECOND_FORM_RELEASE.get(), 0.5F, 1.0F);
            }
            ItemStack enderSlayerScythe = new ItemStack(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get());
            enderSlayerScythe.enchant(Enchantments.SHARPNESS, 3);
            enderSlayerScythe.enchant(Enchantments.SWEEPING_EDGE, 3);
            enderSlayerScythe.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, enderSlayerScythe);

            if (reaperHerobrineEntity.getThunderHerobrineDragon() == null && reaperHerobrineEntity.getThunderHerobrineDragonUUID() == null) {
                reaperHerobrineEntity.summonEnderDragon(0);
            }

            if (reaperHerobrineEntity.getMeteoriteHerobrineDragon() == null && reaperHerobrineEntity.getMeteoriteHerobrineDragonUUID() == null) {
                reaperHerobrineEntity.summonEnderDragon(1);
            }

            if (reaperHerobrineEntity.getHealingHerobrineDragon() == null && reaperHerobrineEntity.getHealingHerobrineDragonUUID() == null) {
                reaperHerobrineEntity.summonEnderDragon(2);
            }
        }
        if (this instanceof NullEntity nullEntity) {
            if (nullEntity.getNullSwordEntity() != null) {
                ItemStack nullSword = new ItemStack(AnnoyingVillagersModItems.NULL_SWORD.get());
                nullSword.enchant(Enchantments.SHARPNESS, 5);
                nullSword.enchant(Enchantments.SWEEPING_EDGE, 5);
                nullEntity.getNullSwordEntity().setItemInHand(InteractionHand.MAIN_HAND, nullSword);
            }
            if (nullEntity.getNullAxeEntity() != null) {
                ItemStack nullAxe = new ItemStack(AnnoyingVillagersModItems.NULL_AXE.get());
                nullAxe.enchant(Enchantments.SMITE, 5);
                nullAxe.enchant(Enchantments.FIRE_ASPECT, 2);
                nullEntity.getNullAxeEntity().setItemInHand(InteractionHand.MAIN_HAND, nullAxe);
            }
            if (nullEntity.getNullPickaxeEntity() != null) {
                ItemStack nullPickaxe = new ItemStack(AnnoyingVillagersModItems.NULL_PICKAXE.get());
                nullPickaxe.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                nullPickaxe.enchant(Enchantments.UNBREAKING, 3);
                nullEntity.getNullPickaxeEntity().setItemInHand(InteractionHand.MAIN_HAND, nullPickaxe);
            }
            if (nullEntity.getNullShovelEntity() != null) {
                ItemStack nullShovel = new ItemStack(AnnoyingVillagersModItems.NULL_SHOVEL.get());
                nullShovel.enchant(Enchantments.UNBREAKING, 5);
                nullShovel.enchant(Enchantments.MENDING, 1);
                nullEntity.getNullShovelEntity().setItemInHand(InteractionHand.MAIN_HAND, nullShovel);
            }
            if (nullEntity.getNullHoeEntity() != null) {
                ItemStack nullHoe = new ItemStack(AnnoyingVillagersModItems.NULL_HOE.get());
                nullHoe.enchant(Enchantments.KNOCKBACK, 5);
                nullHoe.enchant(Enchantments.BLOCK_EFFICIENCY, 1);
                nullEntity.getNullHoeEntity().setItemInHand(InteractionHand.MAIN_HAND, nullHoe);
            }
        }
        if (this instanceof ShadowHerobrineEntity shadowHerobrineEntity) {
            ItemStack shadowObsidianPillar = new ItemStack(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_PILLAR.get());
            shadowObsidianPillar.enchant(Enchantments.SHARPNESS, 5);
            shadowObsidianPillar.enchant(Enchantments.SWEEPING_EDGE, 5);
            shadowObsidianPillar.enchant(Enchantments.KNOCKBACK, 3);
            this.setItemInHand(InteractionHand.MAIN_HAND, shadowObsidianPillar);
            shadowHerobrineEntity.setSummonDarkObCooldown(0);
        }
        this.state = 2;
    }

    private void recoverAfterHealing() {
        this.setHealingCooldown();
        this.healing = false;
    }

    public boolean canBeAffected(MobEffectInstance mobeffectinstance) {
        return (mobeffectinstance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL || mobeffectinstance.getEffect() == MobEffects.GLOWING) && super.canBeAffected(mobeffectinstance);
    }

    private void playInitAnimation() {
//      ADD THIS CODE IN AV_EFM

//        if (getLivingEntityPatch() != null && !this.level().isClientSide()) {
//            if (this instanceof ReaperHerobrineEntity || this instanceof GlaiveHerobrineEntity) {
//                getLivingEntityPatch().playAnimationSynchronized(AnimsWom.GLOWING_AGONY_GUARD, 0.0F);
//            } else if (this instanceof AegisHerobrineEntity aegisHerobrineEntity) {
//                // For some reason the block animation can't be played inside finalize spawn
//                aegisHerobrineEntity.getPersistentData().putBoolean("init_animation", true);
//            } else if (this instanceof TransporterHerobrineCloneEntity) {
//                getLivingEntityPatch().playAnimationSynchronized(AnimsSculkSteve.PORTAL_SUMMON, 0.0F);
//            } else if (!(this instanceof SledgehammerHerobrineEntity) && !(this instanceof SwordsmanHerobrineEntity)) {
//                getLivingEntityPatch().playAnimationSynchronized(AVAnimations.HEROBRINE_ANIMATE, 0.0F);
//            }
//        }

        if (!this.level().isClientSide()) {
            if (this instanceof GlaiveHerobrineEntity) {
                this.spinningWeaponInitActive = true;
                RigAnimationController.playHeldPose(this,RigAnimationId.SPINNING_WEAPON);
            } else if (this instanceof TransporterHerobrineCloneEntity) {
                RigAnimationController.play(this, RigAnimationId.PORTAL_SUMMON);
            } else if (!(this instanceof SledgehammerHerobrineEntity)
                    && !(this instanceof SwordsmanHerobrineEntity)
                    && !(this instanceof AegisHerobrineEntity)
                    && !(this instanceof ReaperHerobrineEntity)
                    && !(this instanceof NullEntity)) {
                RigAnimationController.play(this, RigAnimationId.HEROBRINE_ANIMATE);
            }
        }
    }

    private void tickSpinningWeaponInitAnimation() {
        if (!this.spinningWeaponInitActive) return;

        if (!this.isNoAi() || this.getTarget() != null) {
            this.spinningWeaponInitActive = false;
            RigAnimationController.stop(this,RigAnimationId.SPINNING_WEAPON);
            return;
        }

        if (!RigAnimationController.hasActiveAnimation(this)) {
            RigAnimationController.playHeldPose(this,RigAnimationId.SPINNING_WEAPON);
        }
    }

    private void playStageChangeAnimation() {
//      ADD THIS CODE IN AV_EFM

//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().playAnimationSynchronized(AnimsSculkSteve.HEROBRINE_STAGE_CHANGE, 0.0F);
//        }

        if (!this.level().isClientSide()) {
            RigAnimationController.play(this, RigAnimationId.HEROBRINE_STAGE_CHANGE);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.floatOnAnyFluid();
        this.checkInsideBlocks();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.tickVoiceCooldown();
            if (stunEscapeCooldown > 0) stunEscapeCooldown--;
            if (swapWeaponCooldown > 0) swapWeaponCooldown--;
            if (this.secondFormCooldown > 0) this.secondFormCooldown--;

            CommonUtil.stunEscapeAi(this);

            if (this.state > 0 && this.tickCount % 20 == 0
                    && (this instanceof AegisHerobrineEntity
                    || this instanceof SledgehammerHerobrineEntity
                    || this instanceof SwordsmanHerobrineEntity
                    || this instanceof ReaperHerobrineEntity
                    || this instanceof GlaiveHerobrineEntity)) {
                HerobrineUtil.spawnEliteEffect(this.level(),this.getX(),this.getY(),this.getZ(),this);
            }

            if (this.state == 2 && (this instanceof AegisHerobrineEntity
                    || this instanceof SledgehammerHerobrineEntity
                    || this instanceof SwordsmanHerobrineEntity
                    || this instanceof ReaperHerobrineEntity
                    || this instanceof GlaiveHerobrineEntity
                    || this instanceof NullEntity
                    || this instanceof ShadowHerobrineEntity)) {
                CommonUtil.stunImmunity(this, 3, 3);
                if (this instanceof NullEntity || this instanceof ShadowHerobrineEntity) {
                    if (new Random().nextBoolean()) {
                        serverLevel.sendParticles(
                                AnnoyingVillagersModParticleTypes.FULL_COWL.get(),
                                this.getX(), this.getY(), this.getZ(),
                                1, 0.3D, 1.2D, 0.3D, 0D
                        );
                    }
                }
            }
            if (this.healingCooldown > 0) this.healingCooldown = this.healingCooldown - 1;

            if (this instanceof HerobrineCloneEntity) {
                placeObsidianBlockWhenInWater(AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get());
            } else if (this instanceof ShadowHerobrineCloneEntity || this instanceof Herobrine7Entity
                    || this instanceof ArmoredHerobrineEntity || this instanceof ShadowHerobrineEntity) {
                placeObsidianBlockWhenInWater(AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get());
            } else if (!(this instanceof NullEntity)) {
                placeObsidianBlockWhenInWater(AnnoyingVillagersModBlocks.CRYING_OBSIDIAN_BLOCK.get());
            }

            if (!(this instanceof NullEntity) && !(this instanceof ShadowHerobrineEntity)
                    && this.hasEffect(MobEffects.DAMAGE_BOOST) && this.hasEffect(MobEffects.MOVEMENT_SPEED)
                    && this.hasEffect(MobEffects.JUMP) && this.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                if (new Random().nextBoolean()) {
                    serverLevel.sendParticles(
                            AnnoyingVillagersModParticleTypes.FULL_COWL.get(),
                            this.getX(), this.getY(), this.getZ(),
                            1,
                            0.3D, 1.2D, 0.3D,
                            0.0D
                    );
                }
            }

            if (this.tickCount == 1) {
                if (this.renderPortal) {
                    AnnoyingVillagers.PACKET_HANDLER.send(
                            PacketDistributor.TRACKING_ENTITY.with(() -> this),
                            new ClientboundHerobrinePortalFx(this.getOnPos().getCenter().add(0.0, 1.5, 0.0))
                    );
                    this.renderPortal = false;
                }
                if (this.initialSpawn) {
                    this.setNoAi(true);
                    playInitAnimation();
                    this.initialSpawn = false;
                }
            }

            this.tickSpinningWeaponInitAnimation();

            if (!neverRecall) {
                this.recallTicks = this.recallTicks - 1;
                int remaining = this.recallTicks;

                if (remaining == SHINK_TIME_START) {
                    AnnoyingVillagers.PACKET_HANDLER.send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                            new ClientboundHerobrinePortalFx(new Vec3(this.getX(), this.getY(), this.getZ()))
                    );
                    this.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), 1.0F, 1.0F);
                    HerobrinePortalUtil.sinkIntoGround(serverLevel, this, 0.06);
                }
                if (remaining <= 0) {
                    Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(this.getChatName() + " " + Component.translatable("subtitles.herobrine_retreat")), false);
                    this.discard();
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
                this.recallTicks = 41;
            }

            if (firstPossessedHerobrine == null && firstPossessedHerobrineUuid != null) {
                this.firstPossessedHerobrine = ((ServerLevel) this.level()).getEntity(firstPossessedHerobrineUuid);
            }
            if (secondPossessedHerobrine == null && secondPossessedHerobrineUuid != null) {
                this.secondPossessedHerobrine = ((ServerLevel) this.level()).getEntity(secondPossessedHerobrineUuid);
            }
            if (thirdPossessedHerobrine == null && thirdPossessedHerobrineUuid != null) {
                this.thirdPossessedHerobrine = ((ServerLevel) this.level()).getEntity(thirdPossessedHerobrineUuid);
            }
            if (fourthPossessedHerobrine == null && fourthPossessedHerobrineUuid != null) {
                this.fourthPossessedHerobrine = ((ServerLevel) this.level()).getEntity(fourthPossessedHerobrineUuid);
            }

            if (firstPossessedHerobrine != null && !firstPossessedHerobrine.isAlive()) {
                firstPossessedHerobrine = null;
                firstPossessedHerobrineUuid = null;
                if (this.sacrificing && this.getEmptyBoundClone() == 4) {
                    recoverAfterSacrificing();
                }
                if (this.healing && getHealingHerobrine() == null) {
                    recoverAfterHealing();
                }
            }
            if (secondPossessedHerobrine != null && !secondPossessedHerobrine.isAlive()) {
                secondPossessedHerobrine = null;
                secondPossessedHerobrineUuid = null;
                if (this.sacrificing && this.getEmptyBoundClone() == 4) {
                    recoverAfterSacrificing();
                }
                if (this.healing && getHealingHerobrine() == null) {
                    recoverAfterHealing();
                }
            }
            if (thirdPossessedHerobrine != null && !thirdPossessedHerobrine.isAlive()) {
                thirdPossessedHerobrine = null;
                thirdPossessedHerobrineUuid = null;
                if (this.sacrificing && this.getEmptyBoundClone() == 4) {
                    recoverAfterSacrificing();
                }
                if (this.healing && getHealingHerobrine() == null) {
                    recoverAfterHealing();
                }
            }
            if (fourthPossessedHerobrine != null && !fourthPossessedHerobrine.isAlive()) {
                fourthPossessedHerobrine = null;
                fourthPossessedHerobrineUuid = null;
                if (this.sacrificing && this.getEmptyBoundClone() >= 4) {
                    recoverAfterSacrificing();
                }
                if (this.healing && getHealingHerobrine() == null) {
                    recoverAfterHealing();
                }
            }

            if (this.sacrificingAnimationCooldown > 0) {
                this.sacrificingAnimationCooldown = this.sacrificingAnimationCooldown - 1;
            }
            if (this.sacrificing && this.sacrificingAnimationCooldown == 60) {
                if (this instanceof NullEntity nullEntity) {
                    nullEntity.setSpinningToAllWeaponsAvailableFor5seconds();
                } else {
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }
                playStageChangeAnimation();
                AnnoyingVillagers.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new ClientboundHerobrineAssistanceFx(new Vec3(this.getX(), this.getY(), this.getZ()))
                );
                if (this.level() instanceof ServerLevel) {
                    this.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), 1.0F, 1.0F);;
                }
                if (this.level() instanceof ServerLevel supportServerLevel) {
                    this.playSecondFormSupportCasterAnimations(supportServerLevel);
                }
                this.summonClonesForNextStage();
            }
            if (this.sacrificing && this.sacrificingAnimationCooldown == 10) {
                this.setNoAi(true);
                if (this.firstPossessedHerobrine != null) {
                    CommonUtil.stunImmunity(((Mob) firstPossessedHerobrine), 30, 3);
                    this.clearHandAndDropItem(firstPossessedHerobrine);
                    if (firstPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                        lowShadowHerobrineCloneEntity.setSacrificing(true);
                    }
                    firstPossessedHerobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(this.getX(), this.getY(), this.getZ()));
                    firstPossessedHerobrine.playSound(AnnoyingVillagersModSounds.HEROBRINE_UNDERSTOOD.get(), 0.5F, 1.0F);
                }
                if (this.secondPossessedHerobrine != null) {
                    CommonUtil.stunImmunity(((Mob) secondPossessedHerobrine), 30, 3);
                    this.clearHandAndDropItem(secondPossessedHerobrine);
                    if (secondPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                        lowShadowHerobrineCloneEntity.setSacrificing(true);
                    }
                    secondPossessedHerobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(this.getX(), this.getY(), this.getZ()));
                    secondPossessedHerobrine.playSound(AnnoyingVillagersModSounds.HEROBRINE_UNDERSTOOD.get(), 0.5F, 1.0F);
                }
                if (this.thirdPossessedHerobrine != null) {
                    CommonUtil.stunImmunity(((Mob) thirdPossessedHerobrine), 30, 3);
                    this.clearHandAndDropItem(thirdPossessedHerobrine);
                    if (thirdPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                        lowShadowHerobrineCloneEntity.setSacrificing(true);
                    }
                    thirdPossessedHerobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(this.getX(), this.getY(), this.getZ()));
                    thirdPossessedHerobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(this.getX(), this.getY(), this.getZ()));
                    thirdPossessedHerobrine.playSound(AnnoyingVillagersModSounds.HEROBRINE_UNDERSTOOD.get(), 0.5F, 1.0F);
                }
                if (this.fourthPossessedHerobrine != null) {
                    CommonUtil.stunImmunity(((Mob) fourthPossessedHerobrine), 30, 3);
                    this.clearHandAndDropItem(fourthPossessedHerobrine);
                    this.clearHandAndDropItem(fourthPossessedHerobrine);
                    if (fourthPossessedHerobrine instanceof LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity) {
                        lowShadowHerobrineCloneEntity.setSacrificing(true);
                    }
                    fourthPossessedHerobrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(this.getX(), this.getY(), this.getZ()));
                    fourthPossessedHerobrine.playSound(AnnoyingVillagersModSounds.HEROBRINE_UNDERSTOOD.get(), 0.5F, 1.0F);
                }
            }
            if (this.sacrificing && this.sacrificingAnimationCooldown == 0) {
                if (this.getEmptyBoundClone() == 4) {
                    this.setNoAi(false);
                    return;
                }
                CommonUtil.stunImmunity(this, 5, 3);
                playStageChangeAnimation();
                if (this instanceof NullEntity nullEntity && this.tickCount % 100 == 0) {
                    nullEntity.setSpinningToAllWeaponsAvailableFor5seconds();
                }
            }
            if (this.secondFormHitLeft <= 0 && this.state == 1) {
                this.secondFormHitLeft = 0;
                this.state = 0;
                this.resetSecondFormCooldown();
            }
        }
    }

    private void playSecondFormSupportCasterAnimations(ServerLevel serverLevel) {
        for (HerobrineGregEntity greg : serverLevel.getEntitiesOfClass(
                HerobrineGregEntity.class,
                this.getBoundingBox().inflate(48.0D),
                greg -> greg.isAlive() && greg.isSupportingSecondFormCaster(this)
        )) {
            greg.playSecondFormSupportCast(this);
        }

        for (TransporterHerobrineCloneEntity transporter : serverLevel.getEntitiesOfClass(
                TransporterHerobrineCloneEntity.class,
                this.getBoundingBox().inflate(48.0D),
                transporter -> transporter.isAlive() && transporter.isSupportingSecondFormCaster(this)
        )) {
            transporter.playSecondFormSupportCast(this);
        }
    }

    private void clearHandAndDropItem(Entity entity) {
        ItemEntity itementity;
        LivingEntity livingentity = (LivingEntity)entity;
        ItemStack itemstack;

        if (!this.level().isClientSide()) {
            itemstack = livingentity.getMainHandItem();
            itementity = new ItemEntity(this.level(), entity.getX(), entity.getY() + 1.0D, entity.getZ(), itemstack);
            itementity.setPickUpDelay(10);
            this.level().addFreshEntity(itementity);
            ((LivingEntity) entity).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            itemstack = livingentity.getOffhandItem();
            itementity = new ItemEntity(this.level(), entity.getX(), entity.getY() + 1.0D, entity.getZ(), itemstack);
            itementity.setPickUpDelay(10);
            this.level().addFreshEntity(itementity);
            ((LivingEntity) entity).setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    public void summonClonesForNextStage() {
        if (!(this.level() instanceof ServerLevel server)) return;

        final float yaw = this.getYRot();
        final Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
        final Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();

        final double fwdDist = Math.max(3.0D, this.getBbWidth() * 3.0D);
        final double sideDist = Math.max(3.0D, this.getBbWidth() * 3.0D);

        final double y = this.getY() + 0.01D;

        final Vec3 posFront = this.position().add(forward.scale(fwdDist));
        final Vec3 posBack  = this.position().subtract(forward.scale(fwdDist));
        final Vec3 posLeft  = this.position().subtract(right.scale(sideDist));
        final Vec3 posRight = this.position().add(right.scale(sideDist));

        summonLowCloneAt(server, new Vec3(posFront.x, y, posFront.z), 1);
        summonLowCloneAt(server, new Vec3(posLeft.x,  y, posLeft.z), 2);
        summonLowCloneAt(server, new Vec3(posRight.x, y, posRight.z), 3);
        summonLowCloneAt(server, new Vec3(posBack.x, y, posBack.z), 4);
    }

    private ItemStack randomDamage(ItemStack itemStack) {
        int maxDamage = itemStack.getMaxDamage();
        itemStack.setDamageValue(new Random().nextInt(maxDamage / 3, maxDamage * 3 / 4));
        return itemStack;
    }

    private void equipGearForLowClone(Mob mob) {
        if (random.nextFloat() < 0.3f) {
            mob.setItemSlot(EquipmentSlot.HEAD, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_HELMET.get())));
        }
        if (random.nextFloat() < 0.3f) {
            mob.setItemSlot(EquipmentSlot.CHEST, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())));
        }
        if (random.nextFloat() < 0.3f) {
            mob.setItemSlot(EquipmentSlot.LEGS, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_LEGGINGS.get())));
        }
        if (random.nextFloat() < 0.3f) {
            mob.setItemSlot(EquipmentSlot.FEET, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_BOOTS.get())));
        }
    }

    private void summonLowCloneAt(ServerLevel server, Vec3 pos, int bindSlot) {
        LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity = new LowShadowHerobrineCloneEntity(AnnoyingVillagersModEntities.LOW_SHADOW_HEROBRINE_CLONE.get(), server);
        int surfaceY = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(pos)).getY();
        lowShadowHerobrineCloneEntity.moveTo(pos.x, surfaceY, pos.z, this.getYRot(), this.getXRot());
        lowShadowHerobrineCloneEntity.setRenderPortal(false);
        lowShadowHerobrineCloneEntity.setPossessedByEntity(this);
        lowShadowHerobrineCloneEntity.setPossessedByUuid(this.getUUID());
        equipGearForLowClone(lowShadowHerobrineCloneEntity);
        server.addFreshEntity(lowShadowHerobrineCloneEntity);
        lowShadowHerobrineCloneEntity.setNoAi(true);
        this.playSound(AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), 1.0F, 1.0F);
        if (bindSlot == 1) {
            firstPossessedHerobrine = lowShadowHerobrineCloneEntity;
            firstPossessedHerobrineUuid = lowShadowHerobrineCloneEntity.getUUID();
        } else if (bindSlot == 2) {
            secondPossessedHerobrine = lowShadowHerobrineCloneEntity;
            secondPossessedHerobrineUuid = lowShadowHerobrineCloneEntity.getUUID();
        } else if (bindSlot == 3) {
            thirdPossessedHerobrine = lowShadowHerobrineCloneEntity;
            thirdPossessedHerobrineUuid = lowShadowHerobrineCloneEntity.getUUID();
        } else {
            fourthPossessedHerobrine = lowShadowHerobrineCloneEntity;
            fourthPossessedHerobrineUuid = lowShadowHerobrineCloneEntity.getUUID();
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            HerobrineMobData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
        }
    }

    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor serverLevelAccessor, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag compoundTag) {
        if (mobSpawnType == MobSpawnType.NATURAL || mobSpawnType == MobSpawnType.CHUNK_GENERATION) {
            ServerLevel serverLevel = serverLevelAccessor.getLevel();
            HerobrineMobData herobrineMobData = HerobrineMobData.get(serverLevel);

            if (!herobrineMobData.tryClaim(serverLevel, this.getUUID())) {
                this.discard();
                return null;
            }

            BlockPos blockPos = this.getOnPos();
            int surfaceY = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos).getY();
            BlockPos spawnPos = new BlockPos(blockPos.getX(), surfaceY, blockPos.getZ());
            this.moveTo(spawnPos, this.getYRot(), this.getXRot());
            this.initialSpawn = false;
        }

        SpawnGroupData returnSpawnGroupData = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData, compoundTag);
        this.setLeftHanded(false);
        HerobrineUtil.initialSpawn(serverLevelAccessor, this, recallTicks, mobSpawnType);
        return returnSpawnGroupData;
    }

    public void awardKillScore(@NotNull Entity entity, int i, @NotNull DamageSource damagesource) {
        super.awardKillScore(entity, i, damagesource);
        HerobrineUtil.transformHerobrine(this.level(), this.getX(), this.getY(), this.getZ(), entity, this);
        this.heal(this.getMaxHealth() / 10);
    }
}
