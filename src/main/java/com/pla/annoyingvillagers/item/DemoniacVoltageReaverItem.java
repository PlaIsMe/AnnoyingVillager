package com.pla.annoyingvillagers.item;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.pla.annoyingvillagers.client.compat.BetterCombatSnakeAttachment;
import com.pla.annoyingvillagers.capabilities.SnakeBladeCapability;
import com.pla.annoyingvillagers.entity.PortalEntity;
import com.pla.annoyingvillagers.entity.SnakeBladeEntity;
import com.pla.annoyingvillagers.entity.SwordsmanHerobrineEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModCapabilities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.LockableRigAttackAnimation;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.RigPoseUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DemoniacVoltageReaverItem extends SwordItem implements RigCombatProfileProvider {
    private static final String TAG_PREFERRED_PORTAL_GROUP = "PreferredPortalGroup";
    private static final String TAG_PREFERRED_PORTAL_OWNER = "PreferredPortalOwner";
    private static final String TAG_SNAKE_PROFILE_ATTACK_LOCK = "SnakeBladeProfileAttackLock";
    private static final double TARGET_SEARCH_RADIUS = 16.0D;
    private static final double PORTAL_TARGET_SEARCH_RADIUS = 64.0D;
    private static final String VANILLA_AWAKEN_EXPIRES_TAG = "AVDemoniacVoltageReaverAwakenExpires";
    private static final int VANILLA_AWAKEN_DURATION_TICKS = 20 * 30;
    private static final String VANILLA_RECOVERY_UNTIL_TAG = "AVDemoniacVoltageReaverRecoveryUntil";
    private static final int VANILLA_RECOVERY_DURATION_TICKS = 20 * 60;

    public DemoniacVoltageReaverItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
            }

            public float getSpeed() {
                return 4.0F;
            }

            public float getAttackDamageBonus() {
                return 3.0F;
            }

            public int getLevel() {
                return 1;
            }

            public int getEnchantmentValue() {
                return 4;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(AnnoyingVillagersModItems.ELITE_OBSIDIAN.get());
            }
        }, 3, -3.0F, (new Properties()));
    }

    public static boolean checkNearbyTarget(LivingEntity attacker) {
        Level level = attacker.level();
        Entity closestValid = null;

        Vec3 attackerEyes = attacker.getEyePosition(1.0F);
        level.clip(new ClipContext(
                attackerEyes,
                attackerEyes.add(attacker.getLookAngle().scale(16.0D)),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                attacker
        ));

        for (Entity entity : level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(TARGET_SEARCH_RADIUS))) {
            if (isValidSnakeBladeTarget(attacker, entity)) {
                if (closestValid == null || attacker.distanceTo(entity) < attacker.distanceTo(closestValid)) {
                    closestValid = entity;
                }
            }
        }
        return closestValid != null || findClosestPortalTarget(attacker) != null;
    }

    public static boolean hasSnakeAnimation(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean("SnakeAnimation");
    }

    public static void clearSnakeAnimation(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        stack.removeTagKey("SnakeAnimation");
        clearPreferredPortalTarget(stack);
    }

    public static boolean tryStartSnakeAnimation(ItemStack stack, LivingEntity livingEntity, boolean guard) {
        boolean launched = guard ? processGuard(stack, livingEntity) : process(stack, livingEntity);
        if (launched || getLastFragment(livingEntity) != null) {
            stack.getOrCreateTag().putBoolean("SnakeAnimation", true);
            acquireSnakeProfileAttackLock(livingEntity);
            return true;
        }

        clearSnakeAnimation(stack);
        setLastFragment(livingEntity, null);
        releaseSnakeProfileAttackLock(livingEntity);
        return false;
    }

    private static void acquireSnakeProfileAttackLock(LivingEntity livingEntity) {
        if (!(livingEntity instanceof LockableRigAttackAnimation lockable)) {
            return;
        }

        if (livingEntity.getPersistentData().getBoolean(TAG_SNAKE_PROFILE_ATTACK_LOCK)) {
            return;
        }

        lockable.lock();
        livingEntity.getPersistentData().putBoolean(TAG_SNAKE_PROFILE_ATTACK_LOCK, true);
    }

    public static boolean hasSnakeProfileAttackLock(LivingEntity livingEntity) {
        return livingEntity.getPersistentData().getBoolean(TAG_SNAKE_PROFILE_ATTACK_LOCK);
    }

    public static void releaseSnakeProfileAttackLock(LivingEntity livingEntity) {
        if (!livingEntity.getPersistentData().getBoolean(TAG_SNAKE_PROFILE_ATTACK_LOCK)) {
            return;
        }

        if (livingEntity instanceof LockableRigAttackAnimation lockable) {
            lockable.unlock();
        }
        livingEntity.getPersistentData().remove(TAG_SNAKE_PROFILE_ATTACK_LOCK);
    }

    /**
     * Clears transient snake-blade state when a mob entity is freshly loaded.
     * The LockableRigAttackAnimation counter itself is runtime-only and is not
     * persisted, while Forge persistent data is. Leaving the marker behind after
     * a world unload would therefore make the next snake action think it already
     * owns a lock even though the runtime lock count has reset to zero.
     */
    public static void resetSnakeAnimationAfterEntityLoad(LivingEntity livingEntity) {
        ItemStack stack = livingEntity.getMainHandItem();
        if (stack.getItem() instanceof DemoniacVoltageReaverItem) {
            clearSnakeAnimation(stack);
        }
        setLastFragment(livingEntity, null);
        livingEntity.getPersistentData().remove(TAG_SNAKE_PROFILE_ATTACK_LOCK);
    }

    public static void clearInterruptedSnakeAnimation(LivingEntity livingEntity) {
        ItemStack stack = livingEntity.getMainHandItem();
        if (!(stack.getItem() instanceof DemoniacVoltageReaverItem) || !hasSnakeAnimation(stack)) {
            return;
        }

        SnakeBladeEntity lastFragment = getLastFragment(livingEntity);
        if (lastFragment != null && lastFragment.isAlive() && !lastFragment.isRemoved()) {
            return;
        }
        if (isPlayingSnakeBladeAnimation(livingEntity)) {
            return;
        }

        clearSnakeAnimation(stack);
        setLastFragment(livingEntity, null);
        releaseSnakeProfileAttackLock(livingEntity);
    }

    private static boolean isPlayingSnakeBladeAnimation(LivingEntity livingEntity) {
//        Add this code in AV_EFM

//        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(livingEntity, LivingEntityPatch.class);
//        if (patch == null || patch.getAnimator() == null) {
//            return false;
//        }
//
//        var animationPlayer = patch.getAnimator().getPlayerFor(null);
//        if (animationPlayer == null) {
//            return false;
//        }
//
//        var dynamicAnimation = animationPlayer.getRealAnimation();
//        return dynamicAnimation == AVAnimations.SNAKE_BLADE
//                || dynamicAnimation == AVAnimations.SNAKE_BLADE_GUARD;

        if (livingEntity instanceof Mob mob) {
            RigAnimationId active = RigAnimationController.getActiveAnimationId(mob);
            return active == RigAnimationId.SWORDSMAN_HEROBRINE_ULT
                    || active == RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ULT;
        }

        return false;
    }

    public static boolean process(ItemStack stack, LivingEntity attacker) {
        Level level = attacker.level();
        Entity closestValid = findPreferredPortalTarget(stack, attacker);
        if (closestValid == null) {
            closestValid = findClosestPortalTarget(attacker);
        }

        Vec3 attackerEyes = attacker.getEyePosition(1.0F);
        level.clip(new ClipContext(
                attackerEyes,
                attackerEyes.add(attacker.getLookAngle().scale(16.0D)),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                attacker
        ));

        if (closestValid == null) {
            for (Entity entity : level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(TARGET_SEARCH_RADIUS))) {
                if (isValidSnakeBladeTarget(attacker, entity)) {
                    if (closestValid == null || attacker.distanceTo(entity) < attacker.distanceTo(closestValid)) {
                        closestValid = entity;
                    }
                }
            }
        }
        return launchSnakeBladeAt(attacker, closestValid, stack);
    }

    public static void setPreferredPortalTarget(ItemStack stack, UUID portalGroupUuid, @Nullable UUID portalOwnerUuid) {
        if (portalGroupUuid == null) {
            clearPreferredPortalTarget(stack);
            return;
        }

        stack.getOrCreateTag().putUUID(TAG_PREFERRED_PORTAL_GROUP, portalGroupUuid);
        if (portalOwnerUuid != null) {
            stack.getOrCreateTag().putUUID(TAG_PREFERRED_PORTAL_OWNER, portalOwnerUuid);
        } else if (stack.hasTag()) {
            stack.getTag().remove(TAG_PREFERRED_PORTAL_OWNER);
        }
    }

    public static void clearPreferredPortalTarget(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        stack.removeTagKey(TAG_PREFERRED_PORTAL_GROUP);
        stack.removeTagKey(TAG_PREFERRED_PORTAL_OWNER);
    }

    private static PortalEntity findPreferredPortalTarget(ItemStack stack, LivingEntity attacker) {
        if (!stack.hasTag() || !stack.getTag().hasUUID(TAG_PREFERRED_PORTAL_GROUP)) {
            return null;
        }

        UUID preferredGroup = stack.getTag().getUUID(TAG_PREFERRED_PORTAL_GROUP);
        UUID preferredOwner = stack.getTag().hasUUID(TAG_PREFERRED_PORTAL_OWNER)
                ? stack.getTag().getUUID(TAG_PREFERRED_PORTAL_OWNER)
                : null;
        PortalEntity bestPortal = null;

        for (PortalEntity portal : attacker.level().getEntitiesOfClass(PortalEntity.class, attacker.getBoundingBox().inflate(PORTAL_TARGET_SEARCH_RADIUS))) {
            if (portal.isRemoved() || !preferredGroup.equals(portal.getPortalGroupUUID())) {
                continue;
            }
            if (preferredOwner != null && !preferredOwner.equals(portal.getOwnerUUID())) {
                continue;
            }
            if (!HerobrineUtil.canUsePortalOwnedBy(attacker, portal.getOwnerUUID())) {
                continue;
            }

            if (bestPortal == null || isBetterPreferredPortal(attacker, portal, bestPortal)) {
                bestPortal = portal;
            }
        }

        if (bestPortal != null) {
            clearPreferredPortalTarget(stack);
        }
        return bestPortal;
    }

    private static PortalEntity findClosestPortalTarget(LivingEntity attacker) {
        Level level = attacker.level();
        PortalEntity closestPortal = null;
        UUID attackerUuid = attacker.getUUID();

        for (PortalEntity portal : level.getEntitiesOfClass(PortalEntity.class, attacker.getBoundingBox().inflate(PORTAL_TARGET_SEARCH_RADIUS))) {
            if (portal.isRemoved()) {
                continue;
            }

            UUID ownerUuid = portal.getOwnerUUID();
            if (ownerUuid != null
                    && !ownerUuid.equals(attackerUuid)
                    && !HerobrineUtil.canUsePortalOwnedBy(attacker, ownerUuid)) {
                continue;
            }

            if (closestPortal == null || isBetterInitialPortal(attacker, portal, closestPortal)) {
                closestPortal = portal;
            }
        }

        return closestPortal;
    }

    private static boolean isBetterInitialPortal(LivingEntity attacker, PortalEntity candidate, PortalEntity current) {
        double candidateDistance = attacker.distanceTo(candidate);
        double currentDistance = attacker.distanceTo(current);
        if (candidateDistance < currentDistance) {
            return true;
        }
        if (candidateDistance > currentDistance) {
            return false;
        }

        if (candidate.isStarterPortal() != current.isStarterPortal()) {
            return candidate.isStarterPortal();
        }

        int candidateOrder = candidate.getPortalOrder() < 0 ? Integer.MAX_VALUE : candidate.getPortalOrder();
        int currentOrder = current.getPortalOrder() < 0 ? Integer.MAX_VALUE : current.getPortalOrder();
        if (candidateOrder != currentOrder) {
            return candidateOrder < currentOrder;
        }

        return false;
    }

    private static boolean isBetterPreferredPortal(LivingEntity attacker, PortalEntity candidate, PortalEntity current) {
        if (candidate.isStarterPortal() != current.isStarterPortal()) {
            return candidate.isStarterPortal();
        }

        int candidateOrder = candidate.getPortalOrder() < 0 ? Integer.MAX_VALUE : candidate.getPortalOrder();
        int currentOrder = current.getPortalOrder() < 0 ? Integer.MAX_VALUE : current.getPortalOrder();
        if (candidateOrder != currentOrder) {
            return candidateOrder < currentOrder;
        }

        return isBetterInitialPortal(attacker, candidate, current);
    }

    private static boolean isValidSnakeBladeTarget(LivingEntity attacker, Entity entity) {
        if (entity.equals(attacker)
                || entity.isSpectator()
                || !(entity instanceof Mob || entity instanceof Player)
                || (entity instanceof Player player && player.isCreative())
                || !attacker.hasLineOfSight(entity)) {
            return false;
        }

        if (HerobrineUtil.isHerobrineSide(attacker)
                && HerobrineUtil.isHerobrineSide(entity)) {
            return false;
        }

        return !attacker.isAlliedTo(entity) && !entity.isAlliedTo(attacker);
    }

    public static boolean processGuard(ItemStack stack, LivingEntity entityToGuard) {
        if (entityToGuard instanceof SwordsmanHerobrineEntity swordsmanHerobrineEntity
                && ((swordsmanHerobrineEntity.getGregUUID() != null
                && HerobrineUtil.hasNearbyPortalGroup(swordsmanHerobrineEntity, swordsmanHerobrineEntity.getGregUUID(), 6, 48.0D))
                || HerobrineUtil.hasNearbyPortalGroup(swordsmanHerobrineEntity, null, 6, 48.0D))) {
            return false;
        }

        Level level = entityToGuard.level();
        SnakeBladeCapability.ISnakeBladeCapability snakeBladeCapability =
                AnnoyingVillagersModCapabilities.getCapability(entityToGuard, AnnoyingVillagersModCapabilities.SNAKE_BLADE_CAPABILITY);

        if (snakeBladeCapability != null) {
            if (canLaunchSnakeBlades(level, entityToGuard)) {
                retractFarFragments(level, entityToGuard);
                if (!level.isClientSide) {
                    return launchSnakeBladeAt(entityToGuard, stack);
                }
            }
        }
        return false;
    }

    public static boolean launchSnakeBladeAt(LivingEntity attacker, Entity closestValid, ItemStack stack) {
        Level level = attacker.level();
        SnakeBladeCapability.ISnakeBladeCapability snakeBladeCapability =
                AnnoyingVillagersModCapabilities.getCapability(attacker, AnnoyingVillagersModCapabilities.SNAKE_BLADE_CAPABILITY);

        if (snakeBladeCapability != null) {
            if (canLaunchSnakeBlades(level, attacker)) {
                retractFarFragments(level, attacker);
                if (!level.isClientSide) {
                    if (closestValid != null) {
                        SnakeBladeEntity snakeBladeEntity = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(level);
                        if (snakeBladeEntity != null) {
                            if (stack.hasFoil()) {
                                snakeBladeEntity.setEnchanted(true);
                            }
                            if (attacker instanceof Player) {
                                snakeBladeEntity.copyPosition(attacker);
                            } else {
                                Vec3 spawn = getToolTipPos(attacker, 1.0F, 1.0F);
                                if (spawn == null) spawn = attacker.getEyePosition().add(attacker.getLookAngle().scale(0.8D));
                                snakeBladeEntity.setPos(spawn.x, spawn.y, spawn.z);
                            }
                            snakeBladeEntity.setCreatorEntityUUID(attacker.getUUID());
                            snakeBladeEntity.setFromEntityID(attacker.getId());
                            snakeBladeEntity.setToEntityID(closestValid.getId());
                            snakeBladeEntity.setProgress(0.0F);
                            level.addFreshEntity(snakeBladeEntity);
                            setLastFragment(attacker, snakeBladeEntity);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean launchSnakeBladeAt(LivingEntity attacker, ItemStack stack) {
        Level level = attacker.level();
        SnakeBladeEntity snakeBladeEntity = AnnoyingVillagersModEntities.SNAKE_BLADE.get().create(level);
        if (snakeBladeEntity == null) return false;

        if (stack.hasFoil()) {
            snakeBladeEntity.setEnchanted(true);
        }

        snakeBladeEntity.setCreatorEntityUUID(attacker.getUUID());
        snakeBladeEntity.setFromEntityID(attacker.getId());
        snakeBladeEntity.setToEntityID(-1);
        snakeBladeEntity.setProgress(0.0F);
        snakeBladeEntity.setGuardDirection("forward_left");

        Vec3 spawn = guardTargetFor(attacker, "forward_left");
        snakeBladeEntity.setPos(spawn.x, spawn.y, spawn.z);

        level.addFreshEntity(snakeBladeEntity);
        setLastFragment(attacker, snakeBladeEntity);
        return true;
    }

    public static Vec3 guardTargetFor(LivingEntity ent, String direction) {
        Random random = new Random();
        if ("forward_left".equalsIgnoreCase(direction)) {
            return LocalSpace.localOffsetPos(ent, 1, 0, -1);
        } else if ("forward_right".equalsIgnoreCase(direction)) {
            return LocalSpace.localOffsetPos(ent, 2, 1, 1);
        } else if ("backward_right".equalsIgnoreCase(direction)) {
            return LocalSpace.localOffsetPos(ent, -1, 0, 2);
        } else {
            return LocalSpace.localOffsetPos(ent, -1, 2, -1);
        }
    }

    public static void setLastFragment(LivingEntity entity, SnakeBladeEntity snakeBladeEntity) {
        SnakeBladeCapability.ISnakeBladeCapability snakeBladeCapability =
                AnnoyingVillagersModCapabilities.getCapability(entity, AnnoyingVillagersModCapabilities.SNAKE_BLADE_CAPABILITY);

        if (snakeBladeCapability != null) {
            snakeBladeCapability.setHasSnakeBlade(snakeBladeEntity != null);

            if (snakeBladeEntity != null) {
                snakeBladeCapability.setLastSnakeBladeID(snakeBladeEntity.getId());
                snakeBladeCapability.setLastSnakeBladeUUID(snakeBladeEntity.getUUID());
            } else {
                snakeBladeCapability.setLastSnakeBladeID(-1);
                snakeBladeCapability.setLastSnakeBladeUUID(null);
            }
        }
    }

    public static void retractFarFragments(Level level, LivingEntity livingEntity) {
        SnakeBladeEntity last = getLastFragment(livingEntity);
        if (last != null) {
            last.remove(Entity.RemovalReason.DISCARDED);
            setLastFragment(livingEntity, null);
        }
    }

    public static boolean canLaunchSnakeBlades(Level level, LivingEntity livingEntity) {
        SnakeBladeEntity last = getLastFragment(livingEntity);
        if (last != null) {
            return last.isRemoved();
        }
        return true;
    }

    public static SnakeBladeEntity getLastFragment(LivingEntity livingEntity) {
        SnakeBladeCapability.ISnakeBladeCapability snakeBladeCapability =
                AnnoyingVillagersModCapabilities.getCapability(livingEntity, AnnoyingVillagersModCapabilities.SNAKE_BLADE_CAPABILITY);

        if (snakeBladeCapability == null) return null;

        UUID uuid = snakeBladeCapability.getLastSnakeBladeUUID();
        int id = snakeBladeCapability.getLastSnakeBladeID();
        Level level = livingEntity.level();

        Entity found = null;

        if (!level.isClientSide) {
            if (uuid != null && level instanceof ServerLevel serverLevel) {
                found = serverLevel.getEntity(uuid);
            }
            if (found == null && id != -1) {
                found = level.getEntity(id);
            }
        } else {
            if (id != -1) {
                found = level.getEntity(id);
            }
        }

        if (!(found instanceof SnakeBladeEntity snakeBladeEntity) || !found.isAlive()) {
            return null;
        }
        return snakeBladeEntity;
    }

    public static Vec3 getToolTipPos(Entity ent, float partialTicks, float handToTip) {
//        Add this code in AV_EFM
//        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(ent, LivingEntityPatch.class);
//        if (patch == null) return null;
//
//        OpenMatrix4f joint = patch.getArmature()
//                .getBoundTransformFor(patch.getAnimator().getPose(partialTicks), Armatures.BIPED.get().toolR);
//
//        OpenMatrix4f localOffset = new OpenMatrix4f().translate(new Vec3f(0.0F, 0.0F, -handToTip));
//        OpenMatrix4f.mul(joint, localOffset, joint);
//
//        float yawRad = (float) -Math.toRadians(((LivingEntity) ent).yBodyRotO + 180.0F);
//        OpenMatrix4f worldYaw = new OpenMatrix4f().rotate(yawRad, new Vec3f(0.0F, 1.0F, 0.0F));
//        OpenMatrix4f.mul(worldYaw, joint, joint);
//
//        return new Vec3(
//                joint.m30 + ent.getX(),
//                joint.m31 + (ent.getY() + (ent.getBbHeight() / 1.8F) - 1.0F),
//                joint.m32 + ent.getZ()
//        );

        if (ent instanceof Player player) {
            if (ModList.get().isLoaded("bettercombat") && player.level().isClientSide()) {
                Vec3 animatedTip = BetterCombatSnakeAttachment.getToolTipPos(player);
                if (animatedTip != null) return animatedTip;
            }
            float bodyYaw = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            double sinYaw = Mth.sin(bodyYaw);
            double cosYaw = Mth.cos(bodyYaw);
            double x = Mth.lerp((double)partialTicks, player.xo, player.getX());
            double y = Mth.lerp((double)partialTicks, player.yo, player.getY());
            double z = Mth.lerp((double)partialTicks, player.zo, player.getZ());
            double crouch = player.isCrouching() ? -0.1875D : 0.0D;
            Vec3 hand = new Vec3(x - cosYaw * 0.35D - sinYaw * 0.55D, y + player.getEyeHeight() - 0.45D + crouch, z - sinYaw * 0.35D + cosYaw * 0.55D);
            Vec3 forward = new Vec3(-sinYaw, 0.0D, cosYaw);
            return hand.add(forward.scale(Math.max(0.0F, handToTip)));
        }
        if (ent instanceof Mob mob) {
            RigAnimationId active = RigAnimationController.getActiveAnimationId(mob);
            int startTick = RigAnimationController.getActiveAnimationStartTick(mob);
            if (active != null && startTick >= 0) {
                float elapsedTicks = Math.max(0.0F, mob.tickCount - startTick + partialTicks);
                Vec3 toolTip = RigPoseUtil.getRightWeaponPosition(mob, active, elapsedTicks, handToTip);
                if (ent instanceof SwordsmanHerobrineEntity && (active == RigAnimationId.SWORDSMAN_HEROBRINE_ULT || active == RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ULT)) {
                    return toolTip.add(0.0D, 0.2D, 0.0D);
                }
            }
        }
        if (!(ent instanceof LivingEntity)) return null;
        return CommonUtil.getVanillaSwordOrBodyPosition(ent, partialTicks);
    }

    public static boolean isVanillaAwakened(ItemStack stack, Level level) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean("SecondForm") && level.getGameTime() < stack.getTag().getLong(VANILLA_AWAKEN_EXPIRES_TAG);
    }

    public static boolean isVanillaRecovering(ItemStack stack, Level level) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().contains(VANILLA_RECOVERY_UNTIL_TAG) && level.getGameTime() < stack.getTag().getLong(VANILLA_RECOVERY_UNTIL_TAG);
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DemoniacVoltageReaverItem) || isVanillaAwakened(stack, player.level()) || isVanillaRecovering(stack, player.level())) return false;
        stack.getOrCreateTag().putBoolean("SecondForm", true);
        stack.getOrCreateTag().putLong(VANILLA_AWAKEN_EXPIRES_TAG, player.level().getGameTime() + VANILLA_AWAKEN_DURATION_TICKS);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
        HerobrineUtil.spawnEliteEffect(player.level(), player.getX(), player.getY(), player.getZ(), player);
        player.getCooldowns().addCooldown(stack.getItem(), VANILLA_AWAKEN_DURATION_TICKS);
        return true;
    }

    public static void activateVanillaNormalAttack(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DemoniacVoltageReaverItem) || !isVanillaAwakened(stack, player.level())) return;
        if (!tryStartSnakeAnimation(stack, player, false)) return;
        VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || !isVanillaAwakened(stack, level)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            tryStartSnakeAnimation(stack, player, true);
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
            VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.literal(Component.translatable("tooltip.annoyingvillagers.demoniac_voltage_reaver").getString()));
    }

    private void secondFormNbtTag(@NotNull ItemStack itemstack, @NotNull Level level, @NotNull Entity entity) {
//        Add this code in AV_EFM
//        PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//        if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//            SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.DEMONIAC_VOLTAGE_REAVER);
//            if (skillContainer != null) {
//                if (skillContainer.getStack() >= 1) {
//                    HerobrineUtil.spawnEliteEffect(level, entity.getX(), entity.getY(), entity.getZ(), entity);
//                    if (itemstack.getTag() != null && !itemstack.getTag().getBoolean("SecondForm")) {
//                        itemstack.getTag().putBoolean("SecondForm", true);
//                    }
//                } else if (skillContainer.getStack() < 1 && itemstack.getTag() != null && itemstack.getTag().getBoolean("SecondForm")) {
//                    itemstack.getTag().remove("SecondForm");
//                }
//            }
//        }
//        Handle vanilla code
    }

    public void inventoryTick(@NotNull ItemStack itemstack, @NotNull Level level, @NotNull Entity entity, int i, boolean flag) {
        super.inventoryTick(itemstack, level, entity, i, flag);
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && !level.isClientSide() && itemstack.hasTag() && itemstack.getTag() != null && itemstack.getTag().getBoolean("SecondForm") && (!itemstack.getTag().contains(VANILLA_AWAKEN_EXPIRES_TAG) || level.getGameTime() >= itemstack.getTag().getLong(VANILLA_AWAKEN_EXPIRES_TAG))) {
            itemstack.getTag().remove("SecondForm");
            itemstack.getTag().remove(VANILLA_AWAKEN_EXPIRES_TAG);
            itemstack.getOrCreateTag().putLong(VANILLA_RECOVERY_UNTIL_TAG, level.getGameTime() + VANILLA_RECOVERY_DURATION_TICKS);
            if (entity instanceof Player player) player.getCooldowns().addCooldown(itemstack.getItem(), VANILLA_RECOVERY_DURATION_TICKS);
            clearSnakeAnimation(itemstack);
            if (entity instanceof Player player) releaseSnakeProfileAttackLock(player);
        }
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && !level.isClientSide() && entity instanceof Player player && itemstack.hasTag() && itemstack.getTag() != null) {
            long cooldownUntil = isVanillaAwakened(itemstack, level)
                    ? itemstack.getTag().getLong(VANILLA_AWAKEN_EXPIRES_TAG)
                    : itemstack.getTag().getLong(VANILLA_RECOVERY_UNTIL_TAG);
            long remaining = cooldownUntil - level.getGameTime();
            if (remaining > 0L && player.getCooldowns().getCooldownPercent(itemstack.getItem(), 0.0F) <= 0.0F) {
                player.getCooldowns().addCooldown(itemstack.getItem(), (int)Math.min(Integer.MAX_VALUE, remaining));
            }
            if (!isVanillaAwakened(itemstack, level) && itemstack.getTag().contains(VANILLA_RECOVERY_UNTIL_TAG) && remaining <= 0L) {
                itemstack.getTag().remove(VANILLA_RECOVERY_UNTIL_TAG);
            }
        }
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && flag && entity instanceof Player player && isVanillaAwakened(itemstack, level)) HerobrineUtil.spawnEliteEffect(level, entity.getX(), entity.getY(), entity.getZ(), entity);
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && entity instanceof Player player && !flag && itemstack.hasTag() && itemstack.getTag().getBoolean("SnakeAnimation")) {
            clearSnakeAnimation(itemstack);
            releaseSnakeProfileAttackLock(player);
        }
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.SWORDSMAN_HEROBRINE;
    }

    public static final class LocalSpace {
        private static final Vec3 UP = new Vec3(0, 1, 0);

        public static Vec3 forward(LivingEntity e) {
            float yawRad = e.yBodyRot * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
        }

        public static Vec3 right(LivingEntity e) {
            Vec3 f = forward(e);
            return UP.cross(f).normalize();
        }

        public static Vec3 left(LivingEntity e) {
            return right(e).scale(-1.0D);
        }

        public static Vec3 back(LivingEntity e) {
            return forward(e).scale(-1.0D);
        }

        public static Vec3 localOffsetPos(LivingEntity e, double leftU, double upU, double forwardU) {
            Vec3 base = e.position();
            Vec3 off = left(e).scale(leftU)
                    .add(UP.scale(upU))
                    .add(forward(e).scale(forwardU));
            return base.add(off);
        }
    }
}
