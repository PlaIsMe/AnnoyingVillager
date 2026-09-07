package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.init.*;
import com.pla.annoyingvillagers.item.DemoniacVoltageReaverItem;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.WeaponEnchantmentDamageUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SnakeBladeEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> CREATOR_ID =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> PORTAL_GROUP_ID =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> FROM_ID =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RENDER_FROM_ID =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_PORTAL_ORDER =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_COUNT =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CURRENT_TARGET_ID =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PROGRESS =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETRACTING =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_BLADE =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENCHANTED =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GUARD =
            SynchedEntityData.defineId(SnakeBladeEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float MAX_EXTEND_TIME = 5.0F;
    private static final int MAX_PORTAL_CHAIN_TARGETS = 24;
    private static final int MAX_NORMAL_CHAIN_TARGETS = 5;
    private static final int MAX_GUARD_CHAIN_TARGETS = 5;
    private static final int POST_HIT_CHAIN_DELAY_TICKS = 3;
    private static final double PORTAL_CHAIN_SEARCH_RADIUS = 64.0D;

    private final List<Entity> previouslyTouched = new ArrayList<>();
    private boolean hasChained = false;
    private boolean attemptedCurrentTargetHit = false;
    private int postHitChainDelayTicks = 0;

    public float prevProgress = 0.0F;

    private String guardDirection = null;

    public SnakeBladeEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public SnakeBladeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.SNAKE_BLADE.get(), level);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CREATOR_ID, Optional.empty());
        this.entityData.define(PORTAL_GROUP_ID, Optional.empty());
        this.entityData.define(FROM_ID, -1);
        this.entityData.define(RENDER_FROM_ID, -1);
        this.entityData.define(LAST_PORTAL_ORDER, -1);
        this.entityData.define(TARGET_COUNT, 0);
        this.entityData.define(CURRENT_TARGET_ID, -1);
        this.entityData.define(PROGRESS, 0.0F);
        this.entityData.define(DAMAGE, new Random().nextFloat(10.0F, 15.0F));
        this.entityData.define(RETRACTING, false);
        this.entityData.define(HAS_BLADE, true);
        this.entityData.define(ENCHANTED, false);
        this.entityData.define(GUARD, false);
    }

    public void setEnchanted(boolean enchanted) {
        this.entityData.set(ENCHANTED, enchanted);
    }

    public boolean isEnchanted() {
        return this.entityData.get(ENCHANTED);
    }

    private float getBaseDamage() {
        return this.entityData.get(DAMAGE);
    }

    private float getDamage(LivingEntity creator) {
        return WeaponEnchantmentDamageUtil.addSharpnessBonus(this.getBaseDamage(), creator, DemoniacVoltageReaverItem.class);
    }

    public void setGuard(boolean guard) {
        this.entityData.set(GUARD, guard);
    }

    public boolean isGuard() {
        return this.entityData.get(GUARD);
    }

    public void setGuardDirection(String direction) {
        this.guardDirection = direction;
        this.entityData.set(GUARD, direction != null);
    }

    @Override
    public void tick() {
        Entity creator = getCreatorEntity();
        if (!this.level().isClientSide()) {
            if (!(creator instanceof LivingEntity livingEntity)) {
                cleanupAndDiscard(creator);
                return;
            }
            if (!(livingEntity.getMainHandItem().getItem() instanceof DemoniacVoltageReaverItem) || !livingEntity.isAlive() || livingEntity.isRemoved()) {
                cleanupAndDiscard(creator);
                return;
            }
        }
        HerobrineUtil.spawnEliteEffect(this.level(), this.getX(), this.getY(), this.getZ(), this);

        float progressBefore = this.getProgress();
        this.prevProgress = progressBefore;

        super.tick();

        if (!this.level().isClientSide() && this.isGuard() && this.tickCount % 5 == 0) {
            tickGuardAoe(creator);
        }

        updateProgressAndHandleRemoval(creator);
        if (this.isRemoved()) return;

        updateMovementAndAttack(creator);

        if (!this.level().isClientSide()) {
            handleChaining(creator);
        }

        applyVelocity();
    }

    private void tickGuardAoe(Entity creator) {
        final double size = 2.0D;
        final double radiusSqr = size * size;

        LivingEntity owner = (creator instanceof LivingEntity living) ? living : null;

        for (LivingEntity target : this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(size, size, size),
                e -> e.isAlive() && !e.isSpectator()
        )) {
            if (target == owner) continue;
            if (owner instanceof SwordsmanHerobrineEntity && target instanceof HerobrineGregEntity) continue;
            if (owner != null && (owner.isAlliedTo(target) || target.isAlliedTo(owner))) continue;

            double dx0 = target.getX() - this.getX();
            double dy0 = target.getY(0.5D) - this.getY(0.5D);
            double dz0 = target.getZ() - this.getZ();
            if ((dx0 * dx0 + dy0 * dy0 + dz0 * dz0) > radiusSqr) continue;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        AnnoyingVillagersModParticleTypes.HIT_BLUNT.get(),
                        this.getX(), this.getY() + 1.5, this.getZ() + 0.8,
                        1,
                        0.1, 0.1, 0.1,
                        1
                );
            }

            this.playSound(AnnoyingVillagersModSounds.OBSIDIAN_HIT.get(), 0.5F, (float) (0.5 + Math.random() * 0.5));

            DamageSource src = (owner != null)
                    ? this.level().damageSources().indirectMagic(this, owner)
                    : this.level().damageSources().generic();
            target.hurt(src, this.getDamage(owner) / 2);
            dealStaminaDamage(target, src);
            knockBack(target);
        }
    }

    private void dealStaminaDamage(LivingEntity target, DamageSource src) {
//      ADD THIS CODE IN AV_EFM
//        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
//        EpicfightUtil.dealStaminaDamage(src, 1.0F, targetPatch, false);
    }

    private void knockBack(LivingEntity target) {
//      ADD THIS CODE IN AV_EFM
//        targetPatch.knockBackEntity(this.position(), 1.0);

        double kbX = this.getX() - target.getX();
        double kbZ = this.getZ() - target.getZ();
        target.knockback(1.0, kbX, kbZ);
    }

    private void updateProgressAndHandleRemoval(Entity creator) {
        float progress = this.getProgress();

        if (!this.isRetracting() && progress < MAX_EXTEND_TIME) {
            this.setProgress(progress + 1.0F);
        } else if (this.isRetracting() && progress > 0.0F) {
            this.setProgress(progress - 1.0F);
        }

        if (this.isRetracting() && this.getProgress() == 0.0F) {
            onFullyRetracted(creator);
        }
    }

    private void onFullyRetracted(Entity creator) {
        Entity from = this.getFromEntity();

        if (from instanceof SnakeBladeEntity parentSnakeBladeEntity) {
            parentSnakeBladeEntity.setRetracting(true);
            updateLastFragment(parentSnakeBladeEntity);
        } else {
            updateLastFragment(null);
            clearSnakeAnimationTag(creator);
            cancelAnimation(creator);
        }

        this.remove(RemovalReason.DISCARDED);
    }

    private void cancelAnimation(Entity creator) {
//      ADD THIS CODE IN AV_EFM
//        LivingEntityPatch<?> creatorPatch = EpicFightCapabilities.getEntityPatch(creator, LivingEntityPatch.class);
//        if (creatorPatch != null) {
//            AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(creatorPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
//            if (dynamicAnimation == AVAnimations.SNAKE_BLADE || dynamicAnimation == AVAnimations.SNAKE_BLADE_GUARD) {
//                creatorPatch.playAnimationSynchronized(AVAnimations.IDLE_BREAK, 0.0F);
//            }
//        }

        if (creator instanceof Mob mob && !mob.level().isClientSide) {
            RigAnimationId activeAnimation = RigAnimationController.getActiveAnimationId(mob);
            if (activeAnimation == RigAnimationId.SWORDSMAN_HEROBRINE_ULT
                    || activeAnimation == RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ULT) {
                RigAnimationController.stop(mob, activeAnimation);
            }
        }

        if (creator instanceof LivingEntity livingEntity) {
            DemoniacVoltageReaverItem.releaseSnakeProfileAttackLock(livingEntity);
        }
    }

    private void clearSnakeAnimationTag(Entity creator) {
        if (creator instanceof Player player) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get())) {
                    DemoniacVoltageReaverItem.clearSnakeAnimation(stack);
                }
            }
        } else if (creator instanceof LivingEntity living) {
            DemoniacVoltageReaverItem.clearSnakeAnimation(living.getMainHandItem());
        }
    }

    private void cleanupAndDiscard(Entity creator) {
        updateLastFragment(null);
        clearSnakeAnimationTag(creator);
        cancelAnimation(creator);
        this.remove(RemovalReason.DISCARDED);
    }

    private void updateMovementAndAttack(Entity creator) {
        if (!(creator instanceof LivingEntity livingCreator)) return;

        Entity currentTarget = getToEntity();
        Vec3 targetPos = null;

        if (currentTarget != null) {
            targetPos = targetCenterForCreator(currentTarget);
        } else if (this.guardDirection != null) {
            targetPos = DemoniacVoltageReaverItem.guardTargetFor(livingCreator, this.guardDirection);
        }

        if (targetPos != null) {
            Vec3 delta = targetPos.subtract(this.position());
            this.setDeltaMovement(delta.scale(0.5F));
        }

        if (currentTarget != null
                && !(currentTarget instanceof PortalEntity)
                && !this.level().isClientSide
                && this.getProgress() >= MAX_EXTEND_TIME) {
            if (this.postHitChainDelayTicks <= 0 && (!this.attemptedCurrentTargetHit || this.tickCount % 2 == 0)) {
                tryAttackTarget(livingCreator, currentTarget);
                this.attemptedCurrentTargetHit = true;
            }
        }
    }

    private void tryAttackTarget(LivingEntity creator, Entity target) {
        if (target == creator) return;
        if (target instanceof PortalEntity) return;
        if (creator instanceof SwordsmanHerobrineEntity && target instanceof HerobrineGregEntity) return;

        if (target.hurt(this.level().damageSources().indirectMagic(this, creator), this.getDamage(creator))) {
            markTouched(target);
            this.postHitChainDelayTicks = Math.max(this.postHitChainDelayTicks, POST_HIT_CHAIN_DELAY_TICKS);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        AnnoyingVillagersModParticleTypes.HIT_BLUNT.get(),
                        this.getX(), this.getY() + 1.5, this.getZ() + 0.8,
                        1,
                        0.1, 0.1, 0.1,
                        1
                );
            }

            this.playSound(AnnoyingVillagersModSounds.OBSIDIAN_HIT.get(), 0.5F, (float) (0.5 + Math.random() * 0.5));
            dealStaminaDamageByPercentage(creator, target);

            if (target instanceof LivingEntity livingTarget) {
                float strength = 3.0F;
                double dx = this.getX() - target.getX();
                double dz = this.getZ() - target.getZ();
                livingTarget.knockback(strength, dx, dz);
            }
        }
    }

    private void dealStaminaDamageByPercentage(LivingEntity creator, Entity target) {
//      ADD THIS CODE IN AV_EFM
//        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
//        if (targetPatch != null) {
//            EpicfightUtil.dealStaminaDamageByPercentage(
//                    this.level().damageSources().indirectMagic(this, creator), targetPatch, 0.5D, true);
//        }
    }

    private void handleChaining(Entity creator) {
        if (this.hasChained) return;

        int maxChainTargets = this.guardDirection != null
                ? MAX_GUARD_CHAIN_TARGETS
                : (isPortalChainMode() ? MAX_PORTAL_CHAIN_TARGETS : MAX_NORMAL_CHAIN_TARGETS);
        if (this.getTargetsHit() > maxChainTargets) {
            this.setRetracting(true);
            return;
        }

        if (!(creator instanceof LivingEntity livingCreator)) return;
        if (this.getProgress() < MAX_EXTEND_TIME) return;

        if (this.guardDirection != null) {
            String nextDirection = nextGuardDirection(this.guardDirection);
            createChainGuard(nextDirection);
            this.hasChained = true;
            return;
        }

        Entity currentTarget = this.getToEntity();
        if (currentTarget != null && !(currentTarget instanceof PortalEntity) && this.postHitChainDelayTicks > 0) {
            this.postHitChainDelayTicks--;
            return;
        }

        if (currentTarget instanceof PortalEntity portalEntity) {
            markTouched(portalEntity);
            if (createChainThroughPortal(livingCreator, portalEntity)) {
                this.hasChained = true;
            } else {
                this.setRetracting(true);
            }
            return;
        }

        PortalEntity orderedPortal = findNextOrderedPortal(
                livingCreator,
                this.position(),
                PORTAL_CHAIN_SEARCH_RADIUS,
                getActivePortalGroupUUID(),
                getLastPortalOrder()
        );
        if (orderedPortal != null) {
            createChainToPortal(orderedPortal);
            this.hasChained = true;
            return;
        }

        PortalEntity closestPortal = findClosestUsablePortal(livingCreator, this.position(), PORTAL_CHAIN_SEARCH_RADIUS, null);
        if (closestPortal != null) {
            createChainToPortal(closestPortal);
            this.hasChained = true;
            return;
        }

        Entity closestValid = null;
        for (Entity candidate : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0D))) {
            if (candidate.equals(creator)) continue;
            if (hasTouched(candidate)) continue;
            if (!isValidTarget(livingCreator, candidate)) continue;
            if (!hasLineOfSightTo(candidate)) continue;

            if (closestValid == null || this.position().distanceTo(targetCenter(candidate)) < this.position().distanceTo(targetCenter(closestValid))) {
                closestValid = candidate;
            }
        }

        if (closestValid != null) {
            createChain(closestValid);
            this.hasChained = true;
        } else {
            this.setRetracting(true);
        }
    }

    private boolean createChainThroughPortal(LivingEntity livingCreator, PortalEntity entrancePortal) {
        PortalEntity exitPortal = entrancePortal.getLinkedPortal();
        boolean hasExitPortal = exitPortal != null && !exitPortal.isRemoved();
        PortalEntity chainOriginPortal = hasExitPortal ? exitPortal : entrancePortal;

        markTouched(entrancePortal);
        if (hasExitPortal) {
            markTouched(exitPortal);
        }

        Vec3 chainOriginCenter = portalCenterForCreator(livingCreator, chainOriginPortal);
        Entity closestValid = findClosestValidTargetNear(livingCreator, chainOriginCenter, 14.0D);
        if (closestValid != null) {
            createChainFromPortalExit(chainOriginPortal, closestValid);
            return true;
        }

        UUID portalGroup = chainOriginPortal.getPortalGroupUUID();
        if (portalGroup == null) {
            portalGroup = entrancePortal.getPortalGroupUUID();
        }
        if (portalGroup == null) {
            portalGroup = getActivePortalGroupUUID();
        }

        int lastPortalOrder = hasExitPortal
                ? Math.max(entrancePortal.getPortalOrder(), exitPortal.getPortalOrder())
                : entrancePortal.getPortalOrder();
        PortalEntity nextPortal = findNextOrderedPortal(livingCreator, chainOriginCenter, PORTAL_CHAIN_SEARCH_RADIUS, portalGroup, lastPortalOrder);
        if (nextPortal != null) {
            createChainFromPortalExit(chainOriginPortal, nextPortal);
            return true;
        }

        nextPortal = findClosestUsablePortal(livingCreator, chainOriginCenter, PORTAL_CHAIN_SEARCH_RADIUS, chainOriginPortal);
        if (nextPortal != null) {
            createChainFromPortalExit(chainOriginPortal, nextPortal);
            return true;
        }

        return false;
    }

    private Entity findClosestValidTargetNear(LivingEntity livingCreator, Vec3 center, double radius) {
        Entity closestValid = null;
        AABB searchBox = new AABB(center, center).inflate(radius);

        for (Entity candidate : this.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (candidate.equals(livingCreator)) continue;
            if (hasTouched(candidate)) continue;
            if (!isValidTarget(livingCreator, candidate)) continue;
            if (!hasLineOfSightFrom(center, candidate)) continue;

            if (closestValid == null || center.distanceTo(targetCenter(candidate)) < center.distanceTo(targetCenter(closestValid))) {
                closestValid = candidate;
            }
        }

        return closestValid;
    }

    private PortalEntity findNextOrderedPortal(LivingEntity livingCreator, Vec3 center, double radius, UUID portalGroup, int lastPortalOrder) {
        if (portalGroup == null) {
            return null;
        }

        PortalEntity bestPortal = null;
        AABB searchBox = new AABB(center, center).inflate(radius);

        for (PortalEntity portalEntity : this.level().getEntitiesOfClass(PortalEntity.class, searchBox)) {
            if (hasTouched(portalEntity)) continue;
            if (portalEntity.isRemoved()) continue;
            if (!portalGroup.equals(portalEntity.getPortalGroupUUID())) continue;
            if (portalEntity.getPortalOrder() <= lastPortalOrder) continue;

            UUID ownerUuid = portalEntity.getOwnerUUID();
            if (!HerobrineUtil.canUsePortalOwnedBy(livingCreator, ownerUuid)) continue;

            if (bestPortal == null
                    || portalEntity.getPortalOrder() < bestPortal.getPortalOrder()
                    || (portalEntity.getPortalOrder() == bestPortal.getPortalOrder()
                    && center.distanceTo(portalEntity.position()) < center.distanceTo(bestPortal.position()))) {
                bestPortal = portalEntity;
            }
        }

        return bestPortal;
    }

    private PortalEntity findClosestUsablePortal(LivingEntity livingCreator, Vec3 center, double radius, PortalEntity excludedPortal) {
        PortalEntity closestPortal = null;
        AABB searchBox = new AABB(center, center).inflate(radius);

        for (PortalEntity portalEntity : this.level().getEntitiesOfClass(PortalEntity.class, searchBox)) {
            if (portalEntity == excludedPortal) continue;
            if (hasTouched(portalEntity)) continue;
            if (portalEntity.isRemoved()) continue;
            UUID ownerUuid = portalEntity.getOwnerUUID();
            if (!HerobrineUtil.canUsePortalOwnedBy(livingCreator, ownerUuid)) continue;

            if (closestPortal == null || center.distanceTo(portalEntity.position()) < center.distanceTo(closestPortal.position())) {
                closestPortal = portalEntity;
            }
        }

        return closestPortal;
    }

    private void applyVelocity() {
        Vec3 vel = this.getDeltaMovement();

        double x = this.getX() + vel.x;
        double y = this.getY() + vel.y;
        double z = this.getZ() + vel.z;

        this.setDeltaMovement(vel.scale(0.99F));
        this.setPos(x, y, z);
    }

    private boolean hasTouched(Entity entity) {
        if (entity == null) {
            return false;
        }

        UUID uuid = entity.getUUID();
        for (Entity touched : this.previouslyTouched) {
            if (touched != null && touched.getUUID().equals(uuid)) {
                return true;
            }
        }

        return false;
    }

    private void markTouched(Entity entity) {
        if (entity != null && !hasTouched(entity)) {
            this.previouslyTouched.add(entity);
        }
    }

    private UUID getActivePortalGroupUUID() {
        return this.entityData.get(PORTAL_GROUP_ID).orElse(null);
    }

    private int getLastPortalOrder() {
        return this.entityData.get(LAST_PORTAL_ORDER);
    }

    private void setPortalChainState(UUID portalGroupUuid, int lastPortalOrder) {
        this.entityData.set(PORTAL_GROUP_ID, Optional.ofNullable(portalGroupUuid));
        this.entityData.set(LAST_PORTAL_ORDER, lastPortalOrder);
    }

    private void copyPortalChainState(SnakeBladeEntity child) {
        child.setPortalChainState(this.getActivePortalGroupUUID(), this.getLastPortalOrder());
    }

    private boolean isPortalChainMode() {
        if (this.getActivePortalGroupUUID() != null || this.getToEntity() instanceof PortalEntity) {
            return true;
        }

        for (Entity touched : this.previouslyTouched) {
            if (touched instanceof PortalEntity) {
                return true;
            }
        }

        return false;
    }

    private static Vec3 targetCenter(Entity entity) {
        if (entity instanceof PortalEntity portalEntity) {
            return portalEntity.getPortalCenter();
        }

        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
    }

    private Vec3 targetCenterForCreator(Entity entity) {
        Entity creator = this.getCreatorEntity();
        if (creator instanceof LivingEntity livingCreator && entity instanceof PortalEntity portalEntity) return portalCenterForCreator(livingCreator, portalEntity);
        return targetCenter(entity);
    }

    private static Vec3 portalCenterForCreator(LivingEntity creator, PortalEntity portal) {
        if (creator instanceof Player) return portal.getSnakeBladeAnchor();
        return portal.getPortalCenter();
    }

    private boolean isValidTarget(LivingEntity creator, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        if (HerobrineUtil.isHerobrineSide(creator)
                && HerobrineUtil.isHerobrineSide(entity)) {
            return false;
        }
        if (!creator.isAlliedTo(entity)
                && !entity.isAlliedTo(creator)
                && (entity instanceof Mob || entity instanceof Player)) {
            return true;
        }

        return (creator.getLastHurtMob() != null && creator.getLastHurtMob().getUUID().equals(entity.getUUID()))
                || (creator.getLastHurtByMob() != null && creator.getLastHurtByMob().getUUID().equals(entity.getUUID()));
    }

    private boolean hasLineOfSightTo(Entity target) {
        if (target.level() != this.level()) return false;

        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = targetCenter(target);

        if (to.distanceTo(from) > 128.0D) return false;

        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private boolean hasLineOfSightFrom(Vec3 from, Entity target) {
        if (target.level() != this.level()) return false;

        Vec3 to = targetCenter(target);
        if (to.distanceTo(from) > 128.0D) return false;

        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private void updateLastFragment(SnakeBladeEntity lastSnakeBladeEntity) {
        Entity creator = getCreatorEntity();
        if (creator == null) {
            UUID uuid = this.getCreatorEntityUUID();
            if (uuid != null) {
                creator = this.level().getPlayerByUUID(uuid);
            }
        }

        if (creator instanceof LivingEntity livingCreator) {
            DemoniacVoltageReaverItem.setLastFragment(livingCreator, lastSnakeBladeEntity);
        }
    }

    private void createChain(Entity nextTarget) {
        this.entityData.set(HAS_BLADE, false);

        SnakeBladeEntity child = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(this.level());
        if (child == null) return;

        if (this.isEnchanted()) {
            child.setEnchanted(true);
        }

        child.previouslyTouched.addAll(this.previouslyTouched);
        copyPortalChainState(child);
        child.markTouched(nextTarget);

        child.setCreatorEntityUUID(this.getCreatorEntityUUID());
        child.setFromEntityID(this.getId());
        child.setToEntityID(nextTarget.getId());
        Vec3 nextTargetCenter = targetCenterForCreator(nextTarget);
        child.setPos(nextTargetCenter.x, nextTargetCenter.y, nextTargetCenter.z);
        child.setTargetsHit(this.getTargetsHit() + 1);

        updateLastFragment(child);
        this.level().addFreshEntity(child);
    }

    private void createChainToPortal(PortalEntity nextPortal) {
        this.entityData.set(HAS_BLADE, false);

        SnakeBladeEntity child = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(this.level());
        if (child == null) return;

        if (this.isEnchanted()) {
            child.setEnchanted(true);
        }

        child.previouslyTouched.addAll(this.previouslyTouched);
        copyPortalChainState(child);
        if (child.getActivePortalGroupUUID() == null && nextPortal.getPortalGroupUUID() != null) {
            child.setPortalChainState(nextPortal.getPortalGroupUUID(), nextPortal.getPortalOrder() - 1);
        }
        child.markTouched(nextPortal);

        child.setCreatorEntityUUID(this.getCreatorEntityUUID());
        child.setFromEntityID(this.getId());
        child.setToEntityID(nextPortal.getId());
        Vec3 portalCenter = targetCenterForCreator(nextPortal);
        child.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        child.setTargetsHit(this.getTargetsHit() + 1);

        updateLastFragment(child);
        this.level().addFreshEntity(child);
    }

    private void createChainFromPortalExit(PortalEntity exitPortal, Entity nextTarget) {
        this.entityData.set(HAS_BLADE, false);

        SnakeBladeEntity child = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(this.level());
        if (child == null) return;

        if (this.isEnchanted()) {
            child.setEnchanted(true);
        }

        child.previouslyTouched.addAll(this.previouslyTouched);
        UUID portalGroup = exitPortal.getPortalGroupUUID();
        if (portalGroup == null) {
            portalGroup = getActivePortalGroupUUID();
        }
        int portalOrder = exitPortal.getPortalOrder() >= 0 ? exitPortal.getPortalOrder() : getLastPortalOrder();
        child.setPortalChainState(portalGroup, portalOrder);
        child.markTouched(exitPortal);
        child.markTouched(nextTarget);

        child.setCreatorEntityUUID(this.getCreatorEntityUUID());
        child.setFromEntityID(this.getId());
        child.setRenderFromEntityID(exitPortal.getId());
        child.setToEntityID(nextTarget.getId());

        Vec3 nextTargetCenter = targetCenterForCreator(nextTarget);
        child.setPos(nextTargetCenter.x, nextTargetCenter.y, nextTargetCenter.z);

        child.setTargetsHit(this.getTargetsHit() + 1);

        updateLastFragment(child);
        this.level().addFreshEntity(child);
    }

    private void createChainGuard(String nextDirection) {
        this.entityData.set(HAS_BLADE, false);

        SnakeBladeEntity child = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(this.level());
        if (child == null) return;

        if (this.isEnchanted()) {
            child.setEnchanted(true);
        }

        child.previouslyTouched.addAll(this.previouslyTouched);
        copyPortalChainState(child);
        child.setCreatorEntityUUID(this.getCreatorEntityUUID());
        child.setFromEntityID(this.getId());
        child.setToEntityID(-1);
        child.setTargetsHit(this.getTargetsHit() + 1);
        child.setGuardDirection(nextDirection);

        Entity creator = getCreatorEntity();
        if (creator instanceof LivingEntity living) {
            Vec3 p = DemoniacVoltageReaverItem.guardTargetFor(living, nextDirection);
            child.setPos(p.x, p.y, p.z);
        } else {
            child.copyPosition(this);
        }

        updateLastFragment(child);
        this.level().addFreshEntity(child);
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float amount) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel && !pSource.is(DamageTypes.IN_WALL)) {
            CommonUtil.damageBlocked(pSource, this, serverLevel);
        }
        return false;
    }

    private static String nextGuardDirection(String current) {
        if ("forward_left".equalsIgnoreCase(current)) return "forward_right";
        if ("forward_right".equalsIgnoreCase(current)) return "backward_right";
        if ("backward_right".equalsIgnoreCase(current)) return "backward_left";
        return "forward_left";
    }

    public UUID getCreatorEntityUUID() {
        return this.entityData.get(CREATOR_ID).orElse(null);
    }

    public void setCreatorEntityUUID(UUID id) {
        this.entityData.set(CREATOR_ID, Optional.ofNullable(id));
    }

    public Entity getCreatorEntity() {
        UUID uuid = getCreatorEntityUUID();
        if (uuid == null) return null;
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity creator = serverLevel.getEntity(uuid);
            if (creator != null) return creator;
        }
        return this.level().getPlayerByUUID(uuid);
    }

    public int getFromEntityID() {
        return this.entityData.get(FROM_ID);
    }

    public void setFromEntityID(int id) {
        this.entityData.set(FROM_ID, id);
    }

    public Entity getFromEntity() {
        int id = getFromEntityID();
        return id == -1 ? null : this.level().getEntity(id);
    }

    public int getRenderFromEntityID() {
        return this.entityData.get(RENDER_FROM_ID);
    }

    public void setRenderFromEntityID(int id) {
        this.entityData.set(RENDER_FROM_ID, id);
    }

    public Entity getRenderFromEntity() {
        int id = getRenderFromEntityID();
        return id == -1 ? getFromEntity() : this.level().getEntity(id);
    }

    public int getToEntityID() {
        return this.entityData.get(CURRENT_TARGET_ID);
    }

    public void setToEntityID(int id) {
        this.entityData.set(CURRENT_TARGET_ID, id);
    }

    public Entity getToEntity() {
        int id = getToEntityID();
        return id == -1 ? null : this.level().getEntity(id);
    }

    public int getTargetsHit() {
        return this.entityData.get(TARGET_COUNT);
    }

    public void setTargetsHit(int count) {
        this.entityData.set(TARGET_COUNT, count);
    }

    public float getProgress() {
        return this.entityData.get(PROGRESS);
    }

    public void setProgress(float progress) {
        this.entityData.set(PROGRESS, progress);
    }

    public boolean isRetracting() {
        return this.entityData.get(RETRACTING);
    }

    public void setRetracting(boolean retract) {
        this.entityData.set(RETRACTING, retract);
    }

    public boolean hasBlade() {
        return this.entityData.get(HAS_BLADE);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    public boolean isCreator(Entity mob) {
        UUID creatorUuid = this.getCreatorEntityUUID();
        return creatorUuid != null && mob.getUUID().equals(creatorUuid);
    }
}
