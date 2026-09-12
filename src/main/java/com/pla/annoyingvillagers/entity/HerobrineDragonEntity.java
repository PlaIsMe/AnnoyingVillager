package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.ForceTickEntity;
import com.pla.annoyingvillagers.client.animation.DragonAnimator;
import com.pla.annoyingvillagers.client.engine.MountCameraManager;
import com.pla.annoyingvillagers.client.engine.MountControlsMessenger;
import com.pla.annoyingvillagers.config.AnnoyingVillagersClientConfig;
import com.pla.annoyingvillagers.config.AnnoyingVillagersClientConfig.VfxEffect;
import com.pla.annoyingvillagers.entity.goal.DragonOrbitLeaderGoal;
import com.pla.annoyingvillagers.entity.goal.DragonSummonRiseGoal;
import com.pla.annoyingvillagers.entity.goal.RecallLandGoal;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModKeyMappings;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.item.EnderSlayerScytheItem;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;

/**
 * Here be dragons.
 * <p>
 * Let the legacy live on.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @author Kay9
 */
@SuppressWarnings({"deprecation", "SameReturnValue"})
public class HerobrineDragonEntity extends TamableAnimal implements ForceTickEntity, FlyingAnimal, PlayerRideable
{
    public static final double BASE_SPEED_GROUND = 0.3;
    public static final double BASE_SPEED_FLYING = 0.32;
    public static final double BASE_DAMAGE = 8;
    public static final double BASE_HEALTH = 150;
    public static final double BASE_FOLLOW_RANGE = 16;
    public static final int BASE_KB_RESISTANCE = 1;
    public static final float BASE_WIDTH = 2.75f;
    public static final float BASE_HEIGHT = 2.75f;

    public static final int GROUND_CLEARENCE_THRESHOLD = 3;

    private final DragonAnimator animator;
    private boolean flying;
    private boolean nearGround;

    private UUID summonerUUID;
    private LivingEntity summoner;

    private final GroundPathNavigation groundNavigation;
    private final FlyingPathNavigation flyingNavigation;

    private LivingEntity breathHoverTarget;
    private Vec3 breathHoverPos;
    private int breathHoverTimeToLiveTicks;

    private boolean recallActive = false;
    private boolean recallAutoMount = false;
    private Vec3 recallLandPos = null;

    private int summonRiseTimeToLiveTicks;
    private double summonRisePhysicsReleaseY;
    private Vec3 summonRiseTarget;

    public boolean isRecallAutoMount() {
        return recallAutoMount;
    }

    public void setRecallLandPos(Vec3 recallLandPos) {
        this.recallLandPos = recallLandPos;
    }

    public Vec3 getRecallLandPos() {
        return recallLandPos;
    }

    @Nullable
    public EndCrystal nearestCrystal;

    private static final EntityDataAccessor<Boolean> DATA_CONTROL_LOCKED = SynchedEntityData.defineId(HerobrineDragonEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean isRecallActive() {
        return recallActive;
    }

    public boolean isSummonRising() {
        return summonRiseTimeToLiveTicks > 0 && summonRiseTarget != null;
    }

    @Nullable
    public Vec3 getSummonRiseTarget() {
        return summonRiseTarget;
    }

    public void startSummonRise(Vec3 target, int durationTicks, double physicsReleaseY) {
        if (this.level().isClientSide()) return;

        this.summonRiseTarget = target;
        this.summonRiseTimeToLiveTicks = Math.max(1, durationTicks);
        this.summonRisePhysicsReleaseY = physicsReleaseY;

        this.getNavigation().stop();
        this.setNoGravity(true);
        this.noPhysics = true;
        if (!this.isFlying() && this.canFly()) this.liftOff();
        this.setFlying(true);
        this.setNavigation(true);
    }

    public void tickSummonRiseState(boolean reachedTarget) {
        if (!isSummonRising()) {
            stopSummonRise();
            return;
        }

        summonRiseTimeToLiveTicks--;

        // Allow the summoned dragon to pass through the ground until it reaches
        // the caster's original Y level, then restore normal collision.
        if (noPhysics && this.getY() >= summonRisePhysicsReleaseY) {
            noPhysics = false;
        }

        if (reachedTarget || summonRiseTimeToLiveTicks <= 0) {
            stopSummonRise();
        }
    }

    public void stopSummonRise() {
        summonRiseTimeToLiveTicks = 0;
        summonRisePhysicsReleaseY = 0.0D;
        summonRiseTarget = null;
        noPhysics = false;
        if (isNoGravity()) setNoGravity(false);
    }

    public void setRecallActive(boolean recallActive) {
        this.recallActive = recallActive;
    }

    public HerobrineDragonEntity(EntityType<? extends HerobrineDragonEntity> type, Level level)
    {
        super(type, level);

        noCulling = true;

        moveControl = new DragonMoveController(this);
        animator = level.isClientSide? new DragonAnimator(this) : null;

        flyingNavigation = new FlyingPathNavigation(this, level);
        groundNavigation = new GroundPathNavigation(this, level);

        flyingNavigation.setCanFloat(true);
        groundNavigation.setCanFloat(true);

        navigation = groundNavigation;
    }

    @Override
    @NotNull
    public BodyRotationControl createBodyControl()
    {
        return new DragonBodyController(this);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(0, new DragonSummonRiseGoal(this));
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(1, new RecallLandGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new DragonOrbitLeaderGoal(this, 1.15, 20f, 50f, 180f));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.85f));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_CONTROL_LOCKED, false);
    }

    private boolean isControlLocked() {
        return this.entityData.get(DATA_CONTROL_LOCKED);
    }

    private void setControlLocked(boolean locked) {
        this.entityData.set(DATA_CONTROL_LOCKED, locked);
    }

    private boolean shouldApplyControlLocked() {
        return this.summoner instanceof Player;
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data)
    {
        if (DATA_FLAGS_ID.equals(data)) refreshDimensions();
        else super.onSyncedDataUpdated(data);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound)
    {
        super.addAdditionalSaveData(compound);
        if (summonerUUID != null) {
            compound.putUUID("SummonerUUID", summonerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound)
    {
        super.readAdditionalSaveData(compound);
        setAge(0);
        if (compound.hasUUID("SummonerUUID")) {
            summonerUUID = compound.getUUID("SummonerUUID");
        }
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    public void setSummonerUUID(UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
    }

    public UUID getSummonerUUID() {
        return summonerUUID;
    }

    public void setSummoner(LivingEntity summoner) {
        this.summoner = summoner;
    }

    public LivingEntity getSummoner() {
        return summoner;
    }

    public boolean canFly()
    {
        return true;
    }

    public boolean shouldFly()
    {
        if (isSummonRising()) return true;
        if (isFlying()) return !onGround();
        return canFly() && !isInWater() && !isNearGround();
    }

    public boolean isFlying()
    {
        return flying;
    }

    public void setFlying(boolean flying)
    {
        this.flying = flying;
    }

    public boolean isNearGround()
    {
        return nearGround;
    }

    public void setNavigation(boolean flying)
    {
        navigation = flying ?
                flyingNavigation :
                groundNavigation;
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @org.jetbrains.annotations.Nullable CompoundTag pDataTag) {
        setAge(0);

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static LivingEntity getNearestLivingEntity(Level level, Entity sourceEntity, double range) {
        AABB searchBox = sourceEntity.getBoundingBox().inflate(range);

        return level.getNearestEntity(
                level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != sourceEntity
                                && !(e instanceof HerobrineDragonEntity)
                                && !e.isAlliedTo(sourceEntity)
                                && e.isAlive()),
                TargetingConditions.DEFAULT,
                (LivingEntity) sourceEntity,
                sourceEntity.getX(), sourceEntity.getY(), sourceEntity.getZ()
        );
    }

    private void aimBodyAndHeadAt(LivingEntity target) {
        Vec3 from = this.getEyePosition(1.0F);
        Vec3 to = target.getEyePosition(1.0F);

        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double dy = to.y - from.y;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float wantYaw   = (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        float wantPitch = (float)(-(Mth.atan2(dy, distXZ) * (180F / Math.PI)));

        float yaw   = Mth.approachDegrees(this.getYRot(), wantYaw, (float) 10.0);
        float pitch = Mth.approachDegrees(this.getXRot(), wantPitch, (float) 6.0);

        this.setYRot(yaw);
        this.setXRot(pitch);

        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    public Vec3 beamMouthPos(float partial) {
        Vec3 eye = new Vec3(
                Mth.lerp(partial, this.xOld, this.getX()),
                Mth.lerp(partial, this.yOld, this.getY()) + this.getEyeHeight(),
                Mth.lerp(partial, this.zOld, this.getZ())
        );

        float headYaw = Mth.rotLerp(partial, this.yHeadRotO, this.yHeadRot);
        float headPitch = Mth.lerp(partial, this.xRotO, this.getXRot());
        Vec3 look = Vec3.directionFromRotation(headPitch, headYaw);

        double baseForward = Math.max(1.0, this.getBbWidth() * 0.7);

        boolean hasPlayerRider = this.getFirstPassenger() instanceof Player;
        boolean usePhotonOffset = AnnoyingVillagersClientConfig.isPhotonModLoaded();
        if (this.level().isClientSide()) {
            usePhotonOffset = AnnoyingVillagersClientConfig.shouldUsePhotonWhenAvailable(VfxEffect.DRAGON_BEAM);
        }

        double extraUp = (hasPlayerRider ? 1.0 : usePhotonOffset ? 3.0: 4.0) * this.getScale();
        double extraForward = 5.2 * this.getScale();

        double forward = baseForward + extraForward;

        return eye.add(look.scale(forward))
                .add(0.0, extraUp, 0.0);
    }

    public void shootThunderBreathAtTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (target == null || !target.isAlive()) return;

        stopSummonRise();
        this.breathHoverTarget = target;

        Vec3 position = this.position();
        if (this.onGround()) position = position.add(0.0, 10.0, 0.0);
        this.breathHoverPos = position;

        this.breathHoverTimeToLiveTicks = 110;

        this.getNavigation().stop();

        if (!this.isFlying() && this.canFly()) this.liftOff();
        this.setFlying(true);
        this.setNavigation(true);

        Vec3 mouth = this.getEyePosition()
                .add(this.getLookAngle().scale(Math.max(1.0, this.getBbWidth() * 0.6)));

        DragonBeamEntity beam = new DragonBeamEntity(
                AnnoyingVillagersModEntities.DRAGON_BEAM.get(),
                serverLevel,
                this,
                target,
                mouth.x, mouth.y, mouth.z,
                100, 2
        );

        serverLevel.addFreshEntity(beam);
        this.playSound(AnnoyingVillagersModSounds.DRAGON_THUNDER_SHOOT_SOUND.get(), 2.0F, 1.0F);
    }

    public void shootMeteoriteAtTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (target == null || !target.isAlive()) return;

        stopSummonRise();
        this.breathHoverTarget = target;

        Vec3 position = this.position();
        if (this.onGround()) position = position.add(0.0, 10.0, 0.0);
        this.breathHoverPos = position;

        this.breathHoverTimeToLiveTicks = 20;

        this.getNavigation().stop();

        if (!this.isFlying() && this.canFly()) this.liftOff();
        this.setFlying(true);
        this.setNavigation(true);

        Vec3 look = this.getLookAngle();
        double baseForward = Math.max(1.0, this.getBbWidth() * 0.6);
        double extraForward = 7.5 * this.getScale();
        double heightOffset = -1.0 * this.getScale();
        Vec3 spawnPos = this.getEyePosition()
                .add(look.scale(baseForward + extraForward))
                .add(0.0, heightOffset, 0.0);

        DragonMeteoriteEntity dragonMeteoriteEntity = new DragonMeteoriteEntity(AnnoyingVillagersModEntities.DRAGON_METEORITE.get(), serverLevel);
        dragonMeteoriteEntity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0F, 0F);
        Vec3 aimPosition = new Vec3(target.getX(), target.getY(0.5D), target.getZ());
        Vec3 portalAimPosition = HerobrineUtil.getProjectilePortalAim(this, target);
        if (portalAimPosition != null) {
            aimPosition = portalAimPosition;
        }
        dragonMeteoriteEntity.setPosToAim(aimPosition);
        dragonMeteoriteEntity.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        dragonMeteoriteEntity.setOwner(this);
        serverLevel.addFreshEntity(dragonMeteoriteEntity);

        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 1.0F);
    }

    public void recallAndLand(boolean autoMount) {
        if (this.level().isClientSide()) return;
        if (this.summoner == null || !this.summoner.isAlive()) return;

        stopSummonRise();
        this.recallActive = true;
        this.recallAutoMount = autoMount;
        this.recallLandPos = null;

        this.breathHoverTimeToLiveTicks = 0;
        this.breathHoverTarget = null;
        this.breathHoverPos = null;
    }

    private static boolean hasEnderSlayerScythe(Player p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() instanceof EnderSlayerScytheItem) return true;
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (s.getItem() instanceof EnderSlayerScytheItem) return true;
        }
        return false;
    }

    private static boolean isAllowedEpicFightHeldCategory(Player p) {
        ItemStack main = p.getMainHandItem();
//      ADD THIS CODE IN AV_EFM
//        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(main);
//        if (!(cap instanceof WeaponCapability weaponCap)) return true;
//
//        var cat = weaponCap.getWeaponCategory();
//        return cat == CapabilityItem.WeaponCategories.BOW
//                || cat == CapabilityItem.WeaponCategories.CROSSBOW
//                || cat == CapabilityItem.WeaponCategories.NOT_WEAPON;

        return false;
    }


    private static boolean isAllowedHeldCategory(Player p) {
        ItemStack main = p.getMainHandItem();

        if (main.getItem() instanceof EnderSlayerScytheItem) return true;
        return isAllowedEpicFightHeldCategory(p);
    }

    private void checkCrystals() {
        if (this.getFirstPassenger() != null && this.getFirstPassenger() instanceof EndCrystal) {
            return;
        }
        if (this.nearestCrystal != null) {
            if (this.nearestCrystal.isRemoved()) {
                this.nearestCrystal = null;
            } else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(this.getHealth() + 1.0F);
            }
        }

        if (this.random.nextInt(10) == 0) {
            List<EndCrystal> list = this.level().getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate((double)32.0F));
            EndCrystal endcrystalTemp = null;
            double d0 = Double.MAX_VALUE;

            for(EndCrystal endCrystal : list) {
                double d1 = endCrystal.distanceToSqr(this);
                if (d1 < d0) {
                    d0 = d1;
                    endcrystalTemp = endCrystal;
                }
            }

            if (endcrystalTemp == null && this.nearestCrystal != null) {
                this.nearestCrystal.setBeamTarget(null);
            }
            this.nearestCrystal = endcrystalTemp;
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel)
        {
            this.checkCrystals();
            if (this.nearestCrystal != null && !this.nearestCrystal.isRemoved()) {
                this.nearestCrystal.setBeamTarget(this.blockPosition());
            }

            if (isSummonRising()) {
                if (isControlLocked()) setControlLocked(false);
            } else if (breathHoverTimeToLiveTicks > 0) {
                if (shouldApplyControlLocked()) {
                    if (!isControlLocked()) setControlLocked(true);
                } else {
                    if (isControlLocked()) setControlLocked(false);
                }
                breathHoverTimeToLiveTicks--;

                if (breathHoverPos == null) breathHoverPos = this.position();

                if (!this.isFlying() && this.canFly()) this.liftOff();
                this.setFlying(true);
                this.setNavigation(true);

                this.getNavigation().stop();
                this.setNoGravity(true);

                if (breathHoverTarget != null) {
                    aimBodyAndHeadAt(breathHoverTarget);
                }

                double y = breathHoverPos.y + Math.sin((double) this.tickCount * 0.25) * 0.25;
                this.getMoveControl().setWantedPosition(breathHoverPos.x, y, breathHoverPos.z, 0.12D);

                Vec3 dv = this.getDeltaMovement();
                this.setDeltaMovement(dv.x * 0.2, dv.y * 0.6, dv.z * 0.2);

                if (breathHoverTimeToLiveTicks <= 0) {
                    this.setNoGravity(false);
                    breathHoverTarget = null;
                    breathHoverPos = null;
                }
            } else {
                if (this.isNoGravity()) this.setNoGravity(false);
                if (isControlLocked()) {
                    setControlLocked(false);
                }
            }

            if (summoner == null && summonerUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(summonerUUID);
                if (!(entity instanceof Player) && entity instanceof LivingEntity livingEntity) {
                    summoner = livingEntity;
                } else {
                    Player player = serverLevel.getPlayerByUUID(summonerUUID);

                    if (player != null) {
                        summoner = player;
                    }
                }
            }

            if (summoner != null && !summoner.isAlive()) {
                summoner = null;
                summonerUUID = null;
                this.discard();
            }

            if (summoner != null && summoner.isAlive() && summoner instanceof Player player) {
                if (summoner.getPersistentData().contains("DragonUUID")
                        && !this.getUUID().equals(summoner.getPersistentData().getUUID("DragonUUID"))) {
                    this.discard();
                    return;
                } else if (!summoner.getPersistentData().contains("DragonUUID")) {
                    this.discard();
                    return;
                }

                if (!hasEnderSlayerScythe(player) || !isAllowedHeldCategory(player)) {
                    player.getPersistentData().remove("DragonUUID");
                    this.discard();
                    return;
                }
            }

            if (summoner != null && summoner.isAlive() && !isLeashed() && !isPassenger() && !hasControllingPassenger()) {
                double distSqr = this.distanceToSqr(summoner);
                double farDist = 320.0D;

                if (distSqr > farDist * farDist) {
                    if (!isFlying() && canFly()) liftOff();
                    getNavigation().stop();

                    double toY = Mth.clamp(
                            summoner.getY() + 18.0D,
                            level().getMinBuildHeight() + 6.0D,
                            level().getMaxBuildHeight() - 6.0D
                    );

                    getMoveControl().setWantedPosition(summoner.getX(), toY, summoner.getZ(), 1.8D);
                }
            }
        }
        else {
            animator.tick();
        }

        nearGround = onGround() || !level().noCollision(this, new AABB(getX(), getY(), getZ(), getX(), getY() - (GROUND_CLEARENCE_THRESHOLD * getScale()), getZ()));
        boolean flying = shouldFly();
        if (flying != isFlying())
        {
            setFlying(flying);
            if (!this.level().isClientSide()) setNavigation(flying);
        }
    }

    @Override
    public void travel(@NotNull Vec3 vec3)
    {
        if (isFlying())
        {
            if (isControlledByLocalInstance())
            {
                moveRelative(getSpeed(), vec3);
                move(MoverType.SELF, getDeltaMovement());
                if (getDeltaMovement().lengthSqr() < 0.1)
                    setDeltaMovement(getDeltaMovement().add(0, Math.sin(tickCount / 4f) * 0.03, 0));
                setDeltaMovement(getDeltaMovement().scale(0.9f));
            }

            calculateEntityAnimation(true);
        }
        else super.travel(vec3);
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player driver, @NotNull Vec3 move)
    {
        if (isControlLocked()) {
            return Vec3.ZERO;
        }
        double moveSideways = move.x;
        double moveY = move.y;
        double moveForward = Math.min(Math.abs(driver.zza) + Math.abs(driver.xxa), 1);

        if (isFlying() && hasLocalDriver())
        {
            moveForward = moveForward > 0? moveForward : 0;
            if (driver.jumping) moveY = 1;
            else if (AnnoyingVillagersModKeyMappings.DRAGON_FLIGHT_DESCENT_KEY.isDown()) moveY = -1;
            else if (moveForward > 0) moveY = -driver.getXRot() / 90;
        }

        var speed = getRiddenSpeed(driver);
        return new Vec3(moveSideways * speed, moveY * speed, moveForward * speed);
    }

    @Override
    protected void tickRidden(@NotNull Player driver, @NotNull Vec3 move)
    {
        if (isControlLocked()) {
            return;
        }

        float yaw = driver.yHeadRot;
        if (move.z > 0)
            yaw += (float) Mth.atan2(driver.zza, driver.xxa) * (180f / (float) Math.PI) - 90;
        yHeadRot = yaw;
        setXRot(driver.getXRot() * 0.68f);

        setYRot(Mth.rotateIfNecessary(yHeadRot, getYRot(), 4));

        if (isControlledByLocalInstance())
        {
            if (!isFlying() && canFly() && driver.jumping) liftOff();
        }
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (isControlLocked()) {
            return false;
        }
        return super.isControlledByLocalInstance();
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player driver)
    {
        return (float) getAttributeValue(isFlying()? FLYING_SPEED : MOVEMENT_SPEED);
    }

    public void liftOff()
    {
        if (!canFly() || isInWater()) return;

        Vec3 dv = this.getDeltaMovement();
        if (this.onGround() || dv.y < 0.15D) {
            this.setDeltaMovement(dv.x, 0.42D, dv.z);
        }

        this.setFlying(true);
        if (!this.level().isClientSide()) {
            this.setNavigation(true);
        }
    }

    @Override
    protected float getJumpPower()
    {
        return super.getJumpPower() * (canFly()? 3 : 1);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, @NotNull DamageSource pSource)
    {
        return !canFly() && super.causeFallDamage(pFallDistance, pMultiplier, pSource);
    }

    @Override
    protected void tickDeath()
    {
        ejectPassengers();

        setDeltaMovement(Vec3.ZERO);
        setYRot(yRotO);
        setYHeadRot(yHeadRotO);

        if (deathTime >= getMaxDeathTime()) remove(RemovalReason.KILLED);

        deathTime++;
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn)
    {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    public SoundEvent getStepSound()
    {
        return AnnoyingVillagersModSounds.DRAGON_STEP_SOUND.get();
    }

    /**
     * Returns the sound this mob makes on death.
     */
    @Override
    protected SoundEvent getDeathSound()
    {
        return AnnoyingVillagersModSounds.DRAGON_DEATH_SOUND.get();
    }

    @Override
    public @NotNull SoundEvent getEatingSound(@NotNull ItemStack itemStackIn)
    {
        return SoundEvents.GENERIC_EAT;
    }

    public SoundEvent getWingsSound()
    {
        return SoundEvents.ENDER_DRAGON_FLAP;
    }

    /**
     * Plays step sound at given x, y, z for the entity
     */
    @Override
    protected void playStepSound(@NotNull BlockPos entityPos, @NotNull BlockState state)
    {
        if (isInWater()) return;

        var soundType = state.getSoundType();
        if (level().getBlockState(entityPos.above()).getBlock() == Blocks.SNOW)
            soundType = Blocks.SNOW.getSoundType(state, level(), entityPos, this);

        playSound(getStepSound(), soundType.getVolume(), soundType.getPitch() * getVoicePitch());
    }

    @Override
    public int getAmbientSoundInterval()
    {
        return 240;
    }

    @Override
    protected float getSoundVolume()
    {
        return getScale();
    }

    @Override
    public float getVoicePitch()
    {
        return 2 - getScale();
    }

    public boolean isTamedFor(Player player)
    {
        return isTame() && isOwnedBy(player);
    }

    /**
     * Returns the height of the eyes. Used for looking at other entities.
     */
    @Override
    protected float getStandingEyeHeight(@NotNull Pose poseIn, EntityDimensions sizeIn)
    {
        return sizeIn.height * 1.2f;
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    @Override
    public double getPassengersRidingOffset()
    {
        return getBbHeight() - 0.175;
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    /**
     * returns true if this entity is by a ladder, false otherwise
     */
    @Override
    public boolean onClimbable()
    {
        return false;
    }

    public void onWingsDown(float speed)
    {
        if (!isInWater())
        {
            float pitch = (1 - speed);
            float volume = 0.3f + (1 - speed) * 0.2f;
            float loudMul = 5.0f;
            pitch *= getVoicePitch();
            volume *= getSoundVolume();
            volume *= loudMul;
            level().playLocalSound(getX(), getY(), getZ(), getWingsSound(), SoundSource.HOSTILE, volume, pitch, true);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource src, float par2)
    {
        if (isInvulnerableTo(src)) return false;
        if (src.getEntity() == summoner) return false;
        if (src.getEntity() instanceof Projectile) return super.hurt(src, par2 * 0.1F);
        return super.hurt(src, par2);
    }

    @Override
    public boolean canMate(@NotNull Animal mate)
    {
        if (mate == this) return false;
        if (!(mate instanceof HerobrineDragonEntity)) return false;
        return isInLove() && mate.isInLove();
    }


    @Override
    @SuppressWarnings("ConstantConditions")
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob mob)
    {
        return AnnoyingVillagersModEntities.HEROBRINE_DRAGON.get().create(level);
    }

    @Override
    public boolean wantsToAttack(@NotNull LivingEntity target, @NotNull LivingEntity owner)
    {
        return false;
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target)
    {
        return false;
    }

    @Override
    public LivingEntity getControllingPassenger()
    {
        Entity rider = getFirstPassenger();
        return (rider instanceof Player player) ? player : null;
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger)
    {
        super.addPassenger(passenger);

        if (passenger instanceof Player)
        {
            passenger.setYRot(getYRot());
            passenger.setXRot(getXRot());
        }

        if (hasLocalDriver())
        {
            MountControlsMessenger.sendControlsMessage();
            MountCameraManager.onDragonMount();
        }
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger)
    {
        if (hasLocalDriver()) MountCameraManager.onDragonDismount();
        super.removePassenger(passenger);
    }

    @Override
    protected void positionRider(@NotNull Entity ridden, @NotNull MoveFunction pCallback)
    {
        if (hasPassenger(ridden))
        {
            var rePos = new Vec3(0, getPassengersRidingOffset() + ridden.getMyRidingOffset(), getScale())
                    .yRot((float) Math.toRadians(-yBodyRot))
                    .add(position());
            pCallback.accept(ridden, rePos.x, rePos.y, rePos.z);

            if (getFirstPassenger() instanceof LivingEntity)
            {
                ridden.xRotO = ridden.getXRot();
                ridden.yRotO = ridden.getYRot();
                ridden.setYBodyRot(yBodyRot);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src)
    {
        Entity srcEnt = src.getEntity();
        if (srcEnt != null && (srcEnt == this || hasPassenger(srcEnt))) return true;

        return super.isInvulnerableTo(src);
    }

    public float getHealthFraction()
    {
        return getHealth() / getMaxHealth();
    }

    public int getMaxDeathTime()
    {
        return 120;
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        if (this.getFirstPassenger() instanceof EndCrystal endCrystal
                && this.level() instanceof ServerLevel serverLevel) {
            endCrystal.hurt(serverLevel.damageSources().generic(), 1.0F);
        }
        if (this.getFirstPassenger() != null && this.getFirstPassenger() instanceof EndCrystal) {
            return;
        }
        if (this.nearestCrystal != null) {
            this.nearestCrystal.setBeamTarget(null);
        }

        super.remove(pReason);
    }

    @Override
    public void refreshDimensions()
    {
        double posXTmp = getX();
        double posYTmp = getY();
        double posZTmp = getZ();
        boolean onGroundTmp = onGround();

        super.refreshDimensions();
        setPos(posXTmp, posYTmp, posZTmp);
        setOnGround(onGroundTmp);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose poseIn)
    {
        var height = isInSittingPose()? 2.15f : BASE_HEIGHT;
        var scale = getScale();
        return new EntityDimensions(BASE_WIDTH * scale, height * scale, false);
    }

    @Override
    public boolean isBaby()
    {
        return false;
    }

    public DragonAnimator getAnimator()
    {
        return animator;
    }

    @Override
    public boolean canBreatheUnderwater()
    {
        return true;
    }

    @Override
    public boolean fireImmune()
    {
        return true;
    }

    @Override
    public boolean isInWall()
    {
        if (noPhysics) return false;
        else
        {
            var collider = getBoundingBox().deflate(getBbWidth() * 0.2f);
            return BlockPos.betweenClosedStream(collider).anyMatch((pos) ->
            {
                BlockState state = level().getBlockState(pos);
                return !state.isAir() && state.isSuffocating(level(), pos) && Shapes.joinIsNotEmpty(state.getCollisionShape(level(), pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(collider), BooleanOp.AND);
            });
        }
    }

    @Override
    public @NotNull Vec3 getLightProbePosition(float p_20309_)
    {
        return new Vec3(getX(), getY() + getBbHeight(), getZ());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean hasLocalDriver()
    {
        return getFirstPassenger() instanceof Player p && p.isLocalPlayer();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(MOVEMENT_SPEED, BASE_SPEED_GROUND)
                .add(MAX_HEALTH, BASE_HEALTH)
                .add(FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(KNOCKBACK_RESISTANCE, BASE_KB_RESISTANCE)
                .add(ATTACK_DAMAGE, BASE_DAMAGE)
                .add(FLYING_SPEED, BASE_SPEED_FLYING);
    }

    public void aimBodyAndHeadAt(Vec3 to, float maxYawStep, float maxPitchStep) {
        Vec3 from = this.getEyePosition(1.0F);

        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double dy = to.y - from.y;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float wantYaw   = (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        float wantPitch = (float)(-(Mth.atan2(dy, distXZ) * (180F / Math.PI)));

        float yaw   = Mth.approachDegrees(this.getYRot(), wantYaw, maxYawStep);
        float pitch = Mth.approachDegrees(this.getXRot(), wantPitch, maxPitchStep);

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (this.level() instanceof ServerLevel) {
            if (this.summoner != null && this.summoner instanceof Player player && player.isAlive()) {
                EnderSlayerScytheItem.onSummonedDragonDeath(player, this.getUUID());
            }
        }
        super.die(source);
    }

    public static class DragonMoveController extends MoveControl
    {
        private final HerobrineDragonEntity dragon;

        public DragonMoveController(HerobrineDragonEntity dragon)
        {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick()
        {
            if (!dragon.isFlying())
            {
                super.tick();
                return;
            }

            if (operation == Operation.MOVE_TO)
            {
                operation = Operation.WAIT;
                double xDif = wantedX - mob.getX();
                double yDif = wantedY - mob.getY();
                double zDif = wantedZ - mob.getZ();
                double sq = xDif * xDif + yDif * yDif + zDif * zDif;
                if (sq < (double) 2.5000003E-7F)
                {
                    mob.setYya(0.0F);
                    mob.setZza(0.0F);
                    return;
                }

                float speed = (float) (speedModifier * mob.getAttributeValue(Attributes.FLYING_SPEED));
                double distSq = Math.sqrt(xDif * xDif + zDif * zDif);
                mob.setSpeed(speed);
                if (Math.abs(yDif) > (double) 1.0E-5F || Math.abs(distSq) > (double) 1.0E-5F)
                    mob.setYya((float) yDif * speed);

                float yaw = (float) (Mth.atan2(zDif, xDif) * (double) (180F / (float) Math.PI)) - 90.0F;
                mob.setYRot(rotlerp(mob.getYRot(), yaw, 6));
            }
            else
            {
                mob.setYya(0);
                mob.setZza(0);
            }
        }
    }

    public static class DragonBodyController extends BodyRotationControl
    {
        private final HerobrineDragonEntity dragon;

        public DragonBodyController(HerobrineDragonEntity dragon)
        {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void clientTick()
        {
            dragon.yBodyRot = dragon.getYRot();
            dragon.yHeadRot = Mth.rotateIfNecessary(dragon.yHeadRot, dragon.yBodyRot, dragon.getMaxHeadYRot());
        }
    }
}
