package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.*;
import com.pla.annoyingvillagers.compat.SmartNpc;
import com.pla.annoyingvillagers.entity.goal.EscapeAvoidGoal;
import com.pla.annoyingvillagers.entity.goal.FollowEscapeLeaderGoal;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModMobEffects;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.util.TeamUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.UUID;

public class BbqEntity extends Chicken implements ForceTickEntity, BurstProtectEntity, CombatVoiceLineEntity {
    @Nullable
    private BlueDemonEntity leader;
    @Nullable
    private UUID leaderUUID;
    @Nullable
    private UUID combatTargetUUID;
    private BbqCombatMode combatMode = BbqCombatMode.IDLE;
    private int formationSide = 1;
    private int chainShotsRemaining;
    private int chainShotInterval;
    private int chainShotCooldown;
    private int combatModeTicks;
    private int meleeCooldown;
    private float orbitAngle;
    private float orbitRadius = 5.0F;
    private SauceType sauceType = SauceType.BBQ_SAUCE;
    private int retreatTicks;
    @Nullable
    private BbqEntity sauceLeader;
    @Nullable
    private UUID sauceLeaderUUID;
    private boolean escapeMode;
    private boolean escapeFlying;
    private int escapeLocomotionTicks;
    private float escapeFlightHeight = 1.5F;

    private boolean deathAssemblyMode;
    private int deathAssemblyTicks;
    private double deathAssemblyX;
    private double deathAssemblyY;
    private double deathAssemblyZ;
    private boolean pendingDeathMainhandTrident;
    private boolean pendingDeathOffhandChestplate;
    @Nullable
    private UUID pendingDeathEscapeLeaderUUID;
    private boolean deathWatchMode;
    private boolean selfKill = false;
    private int voiceCooldown = 0;

    @Override
    public int getVoiceCooldown() {
        return voiceCooldown;
    }

    @Override
    public void setVoiceCooldown(int cooldown) {
        this.voiceCooldown = cooldown;
    }

    public boolean isEscapeFlying() {
        return this.escapeMode && this.escapeFlying;
    }

    public double getEscapeFlightHeight() {
        return this.escapeFlightHeight;
    }

    public void tickEscapeLocomotionMode() {
        if (!this.escapeMode) {
            this.escapeFlying = false;
            this.escapeLocomotionTicks = 0;
            this.escapeFlightHeight = 1.5F;
            this.setNoGravity(false);
            return;
        }

        if (this.escapeLocomotionTicks > 0) {
            this.escapeLocomotionTicks--;
            return;
        }

        float airChance;

        if (this.getSauceLeader() != null) {
            airChance = 0.45F;
        } else {
            airChance = 0.30F;
        }

        if (this.sauceType == SauceType.SWEET_ONION_SAUCE) {
            airChance += 0.20F;
        }

        this.escapeFlying = this.random.nextFloat() < airChance;
        this.escapeLocomotionTicks = this.random.nextInt(25, 60);
        this.escapeFlightHeight = 1.0F + this.random.nextFloat() * 4.0F; // 1 to 5 blocks

        if (!this.escapeFlying) {
            this.setNoGravity(false);
            this.fallDistance = 0.0F;
        }
    }

    public void moveEscapeAerialTowards(double x, double y, double z, double accel, double drag) {
        this.moveAerialTowards(x, y, z, accel, drag);
    }

    public BbqEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.BBQ.get(), level);
    }

    public BbqEntity(EntityType<? extends BbqEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6F);
        this.xpReward = 0;
        this.setNoAi(false);
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    public void startLeaderDeathWatch(@Nullable BlueDemonEntity leader) {
        if (leader == null || !leader.isAlive()) {
            return;
        }

        this.deathWatchMode = true;
        this.deathAssemblyMode = false;
        this.escapeMode = false;
        this.retreatTicks = 0;
        this.clearCombat();

        this.setLeader(leader);
        this.sauceLeader = null;
        this.sauceLeaderUUID = null;

        this.escapeFlying = false;
        this.escapeLocomotionTicks = 0;

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
    }

    private void tickLeaderDeathWatch() {
        BlueDemonEntity leader = this.getLeader();
        if (leader == null || !leader.isAlive() || !leader.isInFinalDeathSequence()) {
            this.deathWatchMode = false;
            return;
        }

        this.clearCombat();
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
        this.getLookControl().setLookAt(leader, 45.0F, 45.0F);

        float baseOffset = switch (this.sauceType) {
            case BBQ_SAUCE -> 0.0F;
            case HONEY_MUSTARD_SAUCE -> (float)(Math.PI * 0.5D);
            case SOY_SAUCE -> (float)Math.PI;
            case SWEET_ONION_SAUCE -> (float)(Math.PI * 1.5D);
        };

        double radius = switch (this.sauceType) {
            case BBQ_SAUCE -> 1.1D;
            case HONEY_MUSTARD_SAUCE -> 1.45D;
            case SOY_SAUCE -> 1.45D;
            case SWEET_ONION_SAUCE -> 1.8D;
        };

        float angle = (leader.tickCount * 0.18F) + baseOffset;
        double x = leader.getX() + Mth.cos(angle) * radius;
        double z = leader.getZ() + Mth.sin(angle) * radius;

        if (this.distanceToSqr(leader) > 16.0D) {
            this.getNavigation().moveTo(leader, 1.8D);
        } else {
            this.getNavigation().moveTo(x, leader.getY(), z, 1.35D);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new EscapeAvoidGoal<>(this, Player.class, 12.0F, 2.0D, 2.0D));
        this.goalSelector.addGoal(1, new EscapeAvoidGoal<>(this, HerobrineMob.class, 12.0F, 2.0D, 2.0D));
        this.goalSelector.addGoal(1, new EscapeAvoidGoal<>(this, Monster.class, 12.0F, 2.0D, 2.0D));
        if (ModList.get().isLoaded("smart_npc")) {
            SmartNpc.bbqEscapeAvoid(this);
        }
        this.goalSelector.addGoal(1, new EscapeAvoidGoal<>(this, AVNpc.class, 12.0F, 2.0D, 2.0D));

        this.goalSelector.addGoal(2, new FollowEscapeLeaderGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON) {
            if (!this.level().isClientSide && this.isAlive()) {
                this.heal(4.0F);
            }
            return false;
        }
        return super.canBeAffected(effect);
    }

    public void setLeader(@Nullable BlueDemonEntity leader) {
        this.leader = leader;
        this.leaderUUID = leader == null ? null : leader.getUUID();
    }

    public boolean isEscapeMode() {
        return this.escapeMode;
    }

    public void enterEscapeMode(@Nullable BbqEntity sauceLeader) {
        this.escapeMode = true;
        this.deathWatchMode = false;
        this.retreatTicks = 0;
        this.clearCombat();

        this.leader = null;
        this.leaderUUID = null;

        if (sauceLeader != null && sauceLeader.isAlive() && sauceLeader != this) {
            this.sauceLeader = sauceLeader;
            this.sauceLeaderUUID = sauceLeader.getUUID();
        } else {
            this.sauceLeader = null;
            this.sauceLeaderUUID = null;
        }

        this.escapeFlying = false;
        this.escapeLocomotionTicks = 0;
        this.escapeFlightHeight = 1.5F;

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
    }

    public void startDeathAssembly(double x, double y, double z,
                                   boolean giveMainhandTrident,
                                   boolean giveOffhandChestplate,
                                   @Nullable BbqEntity escapeLeader) {
        this.escapeMode = false;
        this.clearCombat();
        this.retreatTicks = 0;
        this.deathWatchMode = false;

        this.deathAssemblyMode = true;
        this.deathAssemblyTicks = 1;
        this.deathAssemblyX = x;
        this.deathAssemblyY = y;
        this.deathAssemblyZ = z;

        this.pendingDeathMainhandTrident = giveMainhandTrident;
        this.pendingDeathOffhandChestplate = giveOffhandChestplate;
        this.pendingDeathEscapeLeaderUUID =
                escapeLeader != null && escapeLeader != this && escapeLeader.isAlive()
                        ? escapeLeader.getUUID()
                        : null;

        this.leader = null;
        this.leaderUUID = null;
        this.sauceLeader = null;
        this.sauceLeaderUUID = null;

        this.escapeFlying = false;
        this.escapeLocomotionTicks = 0;

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        double offsetX = 0.0D;
        double offsetZ = 0.0D;

        switch (this.sauceType) {
            case BBQ_SAUCE -> {
                offsetX = 0.0D;
                offsetZ = 0.0D;
            }
            case HONEY_MUSTARD_SAUCE -> {
                offsetX = 0.9D;
                offsetZ = 0.0D;
            }
            case SOY_SAUCE -> {
                offsetX = -0.9D;
                offsetZ = 0.0D;
            }
            case SWEET_ONION_SAUCE -> {
                offsetX = 0.0D;
                offsetZ = 0.9D;
            }
        }

        this.moveTo(x + offsetX, y, z + offsetZ);
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
    }

    private void tickDeathAssembly() {
        this.clearCombat();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
        this.getLookControl().setLookAt(this.deathAssemblyX, this.deathAssemblyY + 0.5D, this.deathAssemblyZ);

        if (this.deathAssemblyTicks > 0) {
            this.deathAssemblyTicks--;
        }

        if (this.deathAssemblyTicks > 0) {
            return;
        }

        this.deathAssemblyMode = false;

        if (this.pendingDeathMainhandTrident) {
            this.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
        }

        if (this.pendingDeathOffhandChestplate) {
            this.setItemInHand(InteractionHand.OFF_HAND,
                    new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get()));
        }

        BbqEntity escapeLeader = null;
        if (this.pendingDeathEscapeLeaderUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.pendingDeathEscapeLeaderUUID);
            if (entity instanceof BbqEntity bbq && bbq.isAlive()) {
                escapeLeader = bbq;
            }
        }

        this.pendingDeathMainhandTrident = false;
        this.pendingDeathOffhandChestplate = false;
        this.pendingDeathEscapeLeaderUUID = null;

        this.enterEscapeMode(escapeLeader);
    }

    @Nullable
    public BbqEntity getSauceLeader() {
        if (this.sauceLeader != null && this.sauceLeader.isAlive()) {
            return this.sauceLeader;
        }

        if (!this.level().isClientSide && this.sauceLeaderUUID != null) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.sauceLeaderUUID);
            if (entity instanceof BbqEntity bbq && bbq.isAlive()) {
                this.sauceLeader = bbq;
                return bbq;
            }
        }

        this.sauceLeader = null;
        this.sauceLeaderUUID = null;
        return null;
    }

    @Nullable
    public BlueDemonEntity getLeader() {
        if (this.leader != null && this.leader.isAlive()) {
            return this.leader;
        }

        if (!this.level().isClientSide && this.leaderUUID != null) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.leaderUUID);
            if (entity instanceof BlueDemonEntity blueDemon && blueDemon.isAlive()) {
                this.leader = blueDemon;
                return blueDemon;
            }
        }

        return null;
    }

    public SauceType getSauceType() {
        return this.sauceType;
    }

    public void setSauceType(SauceType sauceType) {
        this.sauceType = sauceType == null ? SauceType.BBQ_SAUCE : sauceType;
        this.setCustomName(Component.translatable(this.sauceType.getTranslationKey()));

        if (this.sauceType.isShockSauce() && this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
        }
    }

    public BbqCombatMode getCombatMode() {
        return combatMode;
    }

    public void setCombatTarget(@Nullable LivingEntity target) {
        this.combatTargetUUID = target == null ? null : target.getUUID();
    }

    @Nullable
    public LivingEntity getCombatTarget() {
        if (this.escapeMode) {
            return null;
        }

        if (!this.level().isClientSide && this.combatTargetUUID != null) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.combatTargetUUID);
            if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                return livingEntity;
            }
            this.combatTargetUUID = null;
        }

        BlueDemonEntity leader = this.getLeader();
        if (leader != null) {
            LivingEntity target = leader.getTarget();
            if (target != null && target.isAlive()) {
                return target;
            }
        }

        return null;
    }

    public void startRetreat() {
        if (!this.isAlive()) {
            return;
        }

        this.clearCombat();
        this.retreatTicks = 60 + this.random.nextInt(20);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("<" + this.getName().getString() + "> " + Component.translatable("subtitles.bbq_retreat").getString()),
                    false
            );
        }
    }

    private void tickRetreat() {
        BlueDemonEntity leader = this.getLeader();
        if (leader == null || !leader.isAlive()) {
            this.discard();
            return;
        }

        Vec3 away = this.position().subtract(leader.position());
        away = new Vec3(away.x, 0.0D, away.z);

        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }

        away = away.normalize();

        double x = this.getX() + away.x * 10.0D;
        double z = this.getZ() + away.z * 10.0D;

        if (this.sauceType == SauceType.SWEET_ONION_SAUCE) {
            this.setNoGravity(true);
            this.getNavigation().stop();
            this.moveAerialTowards(x, this.getY() + 1.5D, z, 0.22D, 0.88D);
        } else {
            this.setNoGravity(false);
            this.getNavigation().moveTo(x, this.getY(), z, 1.45D);
        }

        this.retreatTicks--;
        if (this.retreatTicks <= 0) {
            this.discard();
        }
    }

    public void startOrbit(@Nullable LivingEntity target, int ticks) {
        if (target == null) {
            this.clearCombat();
            return;
        }

        this.setCombatTarget(target);
        this.combatMode = BbqCombatMode.ORBIT;
        this.combatModeTicks = Math.max(this.combatModeTicks, ticks);

        if (this.random.nextInt(4) == 0) {
            this.orbitRadius = new Random().nextFloat(3.5F, 6.5F);
        }

        if (this.random.nextInt(6) == 0) {
            this.formationSide = -this.formationSide;
        }
    }

    public void startHeadAttack(@Nullable LivingEntity target, int ticks) {
        if (target == null) {
            this.clearCombat();
            return;
        }

        this.setCombatTarget(target);
        this.combatMode = BbqCombatMode.HEAD_ATTACK;
        this.combatModeTicks = ticks;
        this.chainShotsRemaining = 0;
        this.chainShotCooldown = 0;
        this.orbitAngle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        this.getNavigation().stop();
    }

    public boolean isHeadAttacking() {
        return this.combatMode == BbqCombatMode.HEAD_ATTACK && this.combatModeTicks > 0;
    }

    public void clearCombat() {
        this.combatMode = BbqCombatMode.IDLE;
        this.combatModeTicks = 0;
        this.combatTargetUUID = null;
        this.chainShotsRemaining = 0;
        this.chainShotCooldown = 0;
        this.setNoGravity(false);
    }

    public void shootChain(LivingEntity target, int shots, int intervalTicks) {
        if (target == null || this.chainShotsRemaining > 0 || this.chainShotCooldown > 0) {
            return;
        }

        this.setCombatTarget(target);
        this.chainShotsRemaining = Math.max(0, shots);
        this.chainShotInterval = Math.max(1, intervalTicks);
        this.chainShotCooldown = this.chainShotInterval;
    }

    public void shootCluster(LivingEntity target, int eggCount, float power, float inaccuracy) {
        if (target == null || this.chainShotsRemaining > 0 || this.chainShotCooldown > 0) {
            return;
        }

        this.setCombatTarget(target);

        for (int i = 0; i < eggCount; i++) {
            this.firePoisonEgg(target, power, inaccuracy);
        }

        this.chainShotCooldown = 45;
    }

    private void tickManualAttacks() {
        if (this.chainShotsRemaining <= 0) {
            return;
        }

        LivingEntity target = this.getCombatTarget();
        if (target == null) {
            this.chainShotsRemaining = 0;
            return;
        }

        if (this.chainShotCooldown > 0) {
            return;
        }

        this.firePoisonEgg(target, 1.45F, 6.0F);
        this.chainShotsRemaining--;

        if (this.chainShotsRemaining > 0) {
            this.chainShotCooldown = this.chainShotInterval;
        } else {
            this.chainShotCooldown = 35;
        }
    }

    private void tickShockTouch(LivingEntity target) {
        if (!this.sauceType.isShockSauce()) {
            return;
        }

        if (this.meleeCooldown > 0) {
            return;
        }

        if (this.distanceToSqr(target.getX(), target.getEyeY(), target.getZ()) > 2.25D) {
            return;
        }

        target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

        if (this.random.nextFloat() < 0.35F) {
            target.addEffect(new MobEffectInstance(AnnoyingVillagersModMobEffects.ELECTRIFY.get(), 20, 1));
        }

        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
        this.meleeCooldown = 12;
    }

    private void tickSweetOnionSupport() {
        BlueDemonEntity leader = this.getLeader();
        LivingEntity enemy = this.getCombatTarget();

        this.setNoGravity(false);
        this.fallDistance = 0.0F;

        if (leader == null || !leader.isAlive()) {
            this.tickLeaderFollow();
            return;
        }

        if (this.getMainHandItem().isEmpty()) {
            BlueDemonThrownTridentEntity trident = leader.getNearestGroundedOwnedTrident(12.0D);

            if (trident != null) {
                if (this.distanceToSqr(trident) > 2.25D) {
                    this.getNavigation().moveTo(trident.getX(), trident.getY(), trident.getZ(), 1.35D);
                } else {
                    ItemStack carried = new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get());
                    carried.getOrCreateTag().putString("CarriedTridentMode", trident.getMode().name());

                    trident.discard();
                    this.setItemSlot(EquipmentSlot.MAINHAND, carried);
                    this.getNavigation().stop();
                }
                return;
            }
        }

        if (this.getMainHandItem().is(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get())) {
            if (this.distanceTo(leader) > 2.5D) {
                this.getNavigation().moveTo(leader, 1.35D);
            } else if (this.level() instanceof ServerLevel serverLevel) {
                TridentMode mode = TridentMode.DEFAULT;
                CompoundTag tag = this.getMainHandItem().getTag();

                if (tag != null && tag.contains("CarriedTridentMode")) {
                    try {
                        mode = TridentMode.valueOf(tag.getString("CarriedTridentMode"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                BlockPos standPos = serverLevel.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        BlockPos.containing(
                                leader.getX() + (this.random.nextDouble() - 0.5D) * 2.0D,
                                leader.getY(),
                                leader.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D
                        )
                );

                BlueDemonThrownTridentEntity placed = new BlueDemonThrownTridentEntity(
                        AnnoyingVillagersModEntities.BLUE_DEMON_THROWN_TRIDENT.get(),
                        serverLevel
                );
                placed.setMode(mode);
                placed.assignSpawnSequence(leader);

                serverLevel.addFreshEntity(placed);
                placed.placeAsGroundedSupport(leader, standPos);
                placed.trimOldGroundedTridentsAroundOwnerOnSpawn();

                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.getNavigation().stop();
            }
            return;
        }

        if (enemy != null && enemy.isAlive()) {
            Vec3 away = leader.position().subtract(enemy.position());
            away = new Vec3(away.x, 0.0D, away.z);

            if (away.lengthSqr() < 1.0E-4D) {
                away = this.position().subtract(enemy.position());
                away = new Vec3(away.x, 0.0D, away.z);
            }

            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }

            away = away.normalize().scale(8.0D + this.random.nextDouble() * 3.0D);
            Vec3 desired = leader.position().add(away);

            this.getNavigation().moveTo(desired.x, leader.getY(), desired.z, 1.4D);
            this.getLookControl().setLookAt(enemy, 30.0F, 30.0F);
            return;
        }

        this.tickLeaderFollow();
    }

    private void firePoisonEgg(LivingEntity target, float power, float inaccuracy) {
        if (this.level().isClientSide) {
            return;
        }

        ThrownPoisonEggEntity projectile = new ThrownPoisonEggEntity(
                AnnoyingVillagersModEntities.THROWN_POISON_EGG.get(),
                this,
                this.level()
        );

        double dX = target.getX() - this.getX();
        double dY = target.getEyeY() - projectile.getY();
        double dZ = target.getZ() - this.getZ();

        projectile.setOwner(this);
        projectile.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        projectile.shoot(dX, dY, dZ, power, inaccuracy);
        this.level().addFreshEntity(projectile);
    }

    private void moveAerialTowards(double x, double y, double z, double accel, double drag) {
        Vec3 wanted = new Vec3(x - this.getX(), y - this.getY(), z - this.getZ());
        double len = wanted.length();

        if (len < 0.05D) {
            this.setDeltaMovement(this.getDeltaMovement().scale(drag));
            return;
        }

        Vec3 desired = wanted.normalize().scale(accel);
        Vec3 next = this.getDeltaMovement().scale(drag).add(desired);
        this.setDeltaMovement(next);
        this.hasImpulse = true;

        float yaw = (float)(Mth.atan2(next.z, next.x) * (180.0F / (float)Math.PI)) - 90.0F;
        this.setYRot(Mth.rotLerp(0.3F, this.getYRot(), yaw));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    public void startGroundOrbit(@Nullable LivingEntity target, int ticks) {
        if (target == null) {
            this.clearCombat();
            return;
        }

        this.setCombatTarget(target);
        this.combatMode = BbqCombatMode.GROUND_ORBIT;
        this.combatModeTicks = Math.max(this.combatModeTicks, ticks);
        this.setNoGravity(false);

        if (this.random.nextInt(4) == 0) {
            this.orbitRadius = new Random().nextFloat(3.5F, 6.5F);
        }

        if (this.random.nextInt(6) == 0) {
            this.formationSide = -this.formationSide;
        }
    }

    private void tickGroundOrbit(LivingEntity target) {
        this.setNoGravity(false);
        this.fallDistance = 0.0F;

        if (this.random.nextInt(70) == 0) {
            this.formationSide = -this.formationSide;
        }

        if (this.random.nextInt(50) == 0) {
            this.orbitRadius = new Random().nextFloat(3.5F, 6.5F);
        }

        this.orbitAngle += 0.12F * this.formationSide;

        double x = target.getX() + Mth.cos(this.orbitAngle) * this.orbitRadius;
        double z = target.getZ() + Mth.sin(this.orbitAngle) * this.orbitRadius;

        if (this.distanceTo(target) < 2.5D) {
            Vec3 away = this.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() > 1.0E-4D) {
                Vec3 desired = this.position().add(away.normalize().scale(1.75D));
                this.getNavigation().moveTo(desired.x, this.getY(), desired.z, 1.35D);
                return;
            }
        }

        this.getNavigation().moveTo(x, target.getY(), z, 1.25D);
    }

    private void tickOrbit(LivingEntity target) {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        this.getNavigation().stop();

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.32D, 0.0D));
        }

        if (this.random.nextInt(70) == 0) {
            this.formationSide = -this.formationSide;
        }

        if (this.random.nextInt(50) == 0) {
            this.orbitRadius = new Random().nextFloat(3.5F, 6.5F);
        }

        this.orbitAngle += 0.16F * this.formationSide;

        double x = target.getX() + Mth.cos(this.orbitAngle) * this.orbitRadius;
        double z = target.getZ() + Mth.sin(this.orbitAngle) * this.orbitRadius;
        double y = target.getEyeY() + 0.4D + Mth.sin((this.tickCount + this.getId()) * 0.25F) * 0.9D;

        this.moveAerialTowards(x, y, z, 0.18D, 0.86D);
    }

    private void dropSpecialHeldItemsBeforeDeath() {
        if (!(this.escapeMode || this.deathAssemblyMode)) {
            return;
        }

        ItemStack main = this.getMainHandItem();
        if (main.is(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get())) {
            this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()));
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        ItemStack off = this.getOffhandItem();
        if (off.is(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get())) {
            this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.BLUE_DEMON_CHESTPLATE.get()));
            this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!this.level().isClientSide) {
            this.dropSpecialHeldItemsBeforeDeath();
        }

        super.die(source);
    }

    private void tickHeadAttack(LivingEntity target) {
        if (this.combatModeTicks <= 0) {
            this.startOrbit(target, 20);
            return;
        }

        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        this.getNavigation().stop();

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.38D, 0.0D));
        }

        this.orbitAngle += 0.42F * this.formationSide;

        double x = target.getX() + Mth.cos(this.orbitAngle) * 0.85D;
        double z = target.getZ() + Mth.sin(this.orbitAngle) * 0.85D;
        double y = target.getEyeY() + 0.15D + Mth.sin((this.tickCount + this.getId()) * 0.55F) * 0.35D;

        this.moveAerialTowards(x, y, z, 0.26D, 0.82D);

        if (this.meleeCooldown <= 0 && this.distanceToSqr(target.getX(), target.getEyeY(), target.getZ()) < 2.25D) {
            target.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            this.doEnchantDamageEffects(this, target);
            this.playSound(SoundEvents.CHICKEN_HURT, 1.0F, 1.1F + this.random.nextFloat() * 0.2F);
            this.playSound(SoundEvents.CHICKEN_AMBIENT, 0.75F, 1.2F + this.random.nextFloat() * 0.3F);
            this.meleeCooldown = 8;
        }
    }

    private void tickLeaderFollow() {
        BlueDemonEntity leader = this.getLeader();
        this.setNoGravity(false);

        if (leader == null || !leader.isAlive()) {
            this.getNavigation().stop();
            return;
        }

        if (this.distanceTo(leader) > 5.0D) {
            this.getNavigation().moveTo(leader, 1.35D);
        } else {
            this.getNavigation().stop();
        }
    }

    public void startParallelPursuit(@Nullable LivingEntity target, int ticks) {
        if (target == null) {
            this.clearCombat();
            return;
        }

        this.setCombatTarget(target);
        this.combatMode = BbqCombatMode.PARALLEL;
        this.combatModeTicks = Math.max(this.combatModeTicks, ticks);
        this.setNoGravity(false);

        if (this.random.nextInt(8) == 0) {
            this.formationSide = -this.formationSide;
        }
    }

    private void tickParallelPursuit(LivingEntity target) {
        BlueDemonEntity leader = this.getLeader();
        this.setNoGravity(false);

        if (leader == null || !leader.isAlive()) {
            this.tickLeaderFollow();
            return;
        }

        if (this.random.nextInt(80) == 0) {
            this.formationSide = -this.formationSide;
        }

        Vec3 forward = target.position().subtract(leader.position());
        forward = new Vec3(forward.x, 0.0D, forward.z);

        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(leader.getLookAngle().x, 0.0D, leader.getLookAngle().z);
        }

        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(1.0D, 0.0D, 0.0D);
        }

        forward = forward.normalize();

        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).scale(2.2D * this.formationSide);
        Vec3 back = forward.scale(-0.75D);
        Vec3 desired = leader.position().add(side).add(back);

        this.getLookControl().setLookAt(target, 45.0F, 45.0F);

        if (this.distanceToSqr(desired.x, leader.getY(), desired.z) > 2.25D) {
            this.getNavigation().moveTo(desired.x, leader.getY(), desired.z, 1.45D);
        } else {
            this.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.selfKill) {
            this.kill();
            return;
        }

        this.tickVoiceCooldown();
        if (this.deathWatchMode) {
            this.tickLeaderDeathWatch();
            return;
        }

        if (this.deathAssemblyMode) {
            this.tickDeathAssembly();
            return;
        }

        if (this.escapeMode) {
            this.tickEscapeLocomotionMode();

            if (this.sauceLeaderUUID != null && this.getSauceLeader() == null) {
                this.sauceLeaderUUID = null;
                this.sauceLeader = null;
                this.escapeLocomotionTicks = 0;
            }

            if (!this.escapeFlying) {
                this.setNoGravity(false);
                this.fallDistance = 0.0F;
            }

            return;
        }

        if (this.retreatTicks > 0) {
            this.tickRetreat();
            return;
        }

        this.teleportNearLeaderIfTooFar();
        BlueDemonEntity leader = this.getLeader();

        if (this.combatModeTicks > 0) {
            this.combatModeTicks--;
        }

        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }

        if (this.chainShotCooldown > 0) {
            this.chainShotCooldown--;
        }

        if (this.sauceType.isSupport()) {
            this.tickSweetOnionSupport();
            return;
        }

        LivingEntity target = this.getCombatTarget();

        if (target != null && target.isAlive()) {
            if (leader != null && leader.isAlive() && leader.getTarget() != null && leader.distanceTo(leader.getTarget()) > 10.0D && this.combatMode != BbqCombatMode.PARALLEL) {
                this.startParallelPursuit(target, 20);
            }

            this.getLookControl().setLookAt(target, 45.0F, 45.0F);

            if (this.sauceType == SauceType.BBQ_SAUCE) {
                this.tickManualAttacks();
            }

            switch (this.combatMode) {
                case HEAD_ATTACK -> {
                    if (this.combatModeTicks > 0) {
                        this.tickHeadAttack(target);
                    } else {
                        this.startGroundOrbit(target, 20);
                        this.tickGroundOrbit(target);
                    }
                }
                case ORBIT -> this.tickOrbit(target);
                case GROUND_ORBIT -> this.tickGroundOrbit(target);
                case PARALLEL -> this.tickParallelPursuit(target);
                default -> {
                    if (leader != null && leader.isAlive() && leader.distanceTo(target) > 10.0D) {
                        this.startParallelPursuit(target, 20);
                        this.tickParallelPursuit(target);
                    } else {
                        this.startGroundOrbit(target, 20);
                        this.tickGroundOrbit(target);
                    }
                }
            }

            this.tickShockTouch(target);
            return;
        }

        this.clearCombat();
        this.tickLeaderFollow();
    }

    public void teleportNearLeaderIfTooFar() {
        if (this.level().isClientSide || this.retreatTicks > 0 || this.escapeMode) {
            return;
        }

        BlueDemonEntity leader = this.getLeader();
        if (leader == null || !leader.isAlive() || leader.isSauceArrivalPending()) {
            return;
        }

        if (this.distanceToSqr(leader) <= 400.0D) {
            return;
        }

        float angle = leader.getSauceSquadAngle();
        double laneOffset = switch (this.getSauceType()) {
            case BBQ_SAUCE -> -2.25D;
            case HONEY_MUSTARD_SAUCE -> -0.75D;
            case SOY_SAUCE -> 0.75D;
            case SWEET_ONION_SAUCE -> 2.25D;
        };

        double forwardX = Mth.cos(angle);
        double forwardZ = Mth.sin(angle);
        double sideX = -forwardZ;
        double sideZ = forwardX;

        double radius = 1.8D;
        double x = leader.getX() - forwardX * radius + sideX * laneOffset;
        double z = leader.getZ() - forwardZ * radius + sideZ * laneOffset;
        double y = leader.getY();

        this.teleportTo(x, y, z);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (damageSource.is(DamageTypes.LIGHTNING_BOLT)) return false;
        if (damageSource.is(DamageTypes.EXPLOSION)) return false;
        if (damageSource.is(DamageTypes.FELL_OUT_OF_WORLD)
                || damageSource.is(DamageTypes.GENERIC_KILL)) {
            boolean result = super.hurt(damageSource, amount);
            if (result) {
                this.sayHurtSound(this, damageSource);
            }
            return result;
        }
        if (this.getHealth() <= 1.0F && !selfKill) {
            this.selfKill = true;
            return false;
        }
        boolean result = super.hurt(damageSource, amount);

        if (result && !this.level().isClientSide && damageSource.getEntity() instanceof LivingEntity livingEntity) {
            if (this.deathAssemblyMode || this.deathWatchMode) {
                return false;
            }

            if (this.escapeMode) {
                return true;
            }

            BlueDemonEntity leader = this.getLeader();
            this.startOrbit(livingEntity, 40);
            if (leader != null) {
                leader.setTarget(livingEntity);
            }
        }
        if (result) {
            this.sayHurtSound(this, damageSource);
        }
        return result;
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
        if (!pDamageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float cap = this.getMaxHealth() * 0.025F;
            f1 = Mth.clamp(f1, 0.0F, cap);
        }
        if (f1 <= 0.0F) {
            return;
        }
        this.getCombatTracker().recordDamage(pDamageSource, f1);
        this.setHealth(this.getHealth() - f1);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                 @NotNull DifficultyInstance difficulty,
                                                 @NotNull MobSpawnType reason,
                                                 @Nullable SpawnGroupData spawnData,
                                                 @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        if (!this.level().isClientSide()) {
            TeamUtil.addOrJoinTeam(this, "blue_demon");
        }

        return data;
    }

    @Override
    public @Nullable SoundEvent getHurtVoiceSound() {
        return AnnoyingVillagersModSounds.BBQ_SAY.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (this.leaderUUID != null) {
            tag.putUUID("LeaderUUID", this.leaderUUID);
        }

        tag.putString("SauceType", this.sauceType.name());
        tag.putBoolean("EscapeMode", this.escapeMode);

        if (this.sauceLeaderUUID != null) {
            tag.putUUID("SauceLeaderUUID", this.sauceLeaderUUID);
        }
        tag.putInt("VoiceCooldown", this.voiceCooldown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("LeaderUUID")) {
            this.leaderUUID = tag.getUUID("LeaderUUID");
        }

        if (tag.contains("SauceType")) {
            try {
                this.setSauceType(SauceType.valueOf(tag.getString("SauceType")));
            } catch (IllegalArgumentException ignored) {
                this.setSauceType(SauceType.BBQ_SAUCE);
            }
        } else {
            this.setSauceType(SauceType.BBQ_SAUCE);
        }
        this.escapeMode = tag.getBoolean("EscapeMode");

        if (tag.hasUUID("SauceLeaderUUID")) {
            this.sauceLeaderUUID = tag.getUUID("SauceLeaderUUID");
        } else {
            this.sauceLeaderUUID = null;
        }
        this.sauceLeader = null;
        voiceCooldown = tag.getInt("VoiceCooldown");
    }

    public static @NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.MAX_HEALTH, 75.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D);
    }
}
