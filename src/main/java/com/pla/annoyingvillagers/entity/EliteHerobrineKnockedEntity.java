package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.ForceTickEntity;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigStunnableEntity;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EliteHerobrineKnockedEntity extends PathfinderMob implements ForceTickEntity, RigStunnableEntity {
    private int wardenCallingCooldown;
    private int eatCount = 0;
    private boolean initialSpawn = false;
    private final List<Item> listWeapons = new ArrayList<>(Arrays.asList(
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
    ));

    public EliteHerobrineKnockedEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.ELITE_HEROBRINE_KNOCKED.get(), level);
    }

    public EliteHerobrineKnockedEntity(EntityType<EliteHerobrineKnockedEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(0.6F);
        this.xpReward = 0;
        this.setNoAi(true);
        this.setCustomNameVisible(false);
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AnnoyingVillagersModItems.ELITE_OBSIDIAN_LONG.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(AnnoyingVillagersModItems.ELITE_OBSIDIAN_BIG.get()));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AnnoyingVillagersModItems.ELITE_OBSIDIAN_BODY.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        wardenCallingCooldown = pCompound.getInt("WardenCallingCooldown");
        eatCount = pCompound.getInt("EatCount");
        initialSpawn = pCompound.getBoolean("InitialSpawn");
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("WardenCallingCooldown", wardenCallingCooldown);
        pCompound.putInt("EatCount", eatCount);
        pCompound.putBoolean("InitialSpawn", this.initialSpawn);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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

    public SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.hurt"));
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.getEntity() instanceof HerobrineWardenEntity) {
            eatCount = eatCount + 1;
            playBeingEatenAnimation();
            if (this.level() instanceof ServerLevel serverLevel) {
                if (this.eatCount == 1) {
                    if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                        this.playSound(AnnoyingVillagersModSounds.KNOCKED_ELITE_HEROBRINE_SAY_ON_BEING_EATEN.get(), 0.5F, 1.0F);
                    }
                }
                serverLevel.playSound(
                        null,
                        BlockPos.containing(this.getX(), this.getY(), this.getZ()),
                        SoundEvents.GENERIC_EAT,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );
            }
            if (eatCount == 10) {
                this.remove(RemovalReason.DISCARDED);
            }
            return super.hurt(pSource, 0.0F);
        }
        if (pSource.is(DamageTypes.IN_WALL)) return false;
        return super.hurt(pSource, 1.0F);
    }

    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.death"));
    }

    private void solidifyFeetAndStandOnTop() {
        if (this.level().isClientSide()) return;

        BlockPos feet = this.getOnPos();
        FluidState fluid = this.level().getFluidState(feet);
        if (fluid.isEmpty()) return;

        this.level().setBlockAndUpdate(feet, Blocks.CRYING_OBSIDIAN.defaultBlockState());

        BlockState bs = this.level().getBlockState(feet);
        VoxelShape shape = bs.getCollisionShape(this.level(), feet, CollisionContext.of(this));
        double top = shape.isEmpty() ? 1.0D : shape.max(Direction.Axis.Y);

        double surfaceY = feet.getY() + top + 1.0E-3;
        double x = this.getX();
        double z = this.getZ();

        this.fallDistance = 0.0F;
        this.setDeltaMovement(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z);
        this.setPos(x, surfaceY, z);
        this.setOnGround(true);



        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos around = feet.offset(dx, 0, dz);
                FluidState fs = this.level().getFluidState(around);
                if (fs.isEmpty()) continue;
                this.level().setBlockAndUpdate(around, Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }
        }
    }

    private void playBeingEatenAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (livingentitypatch != null) {
//            if (eatCount == 1 || eatCount == 2) {
//                livingentitypatch.playAnimationSynchronized(AVAnimations.EATING_ELITE_1, 0.0F);
//            } else if (eatCount == 3 || eatCount == 4) {
//                livingentitypatch.playAnimationSynchronized(AVAnimations.EATING_ELITE_2, 0.0F);
//            } else if (eatCount == 5 || eatCount == 6) {
//                livingentitypatch.playAnimationSynchronized(AVAnimations.EATING_ELITE_3, 0.0F);
//            } else if (eatCount > 6) {
//                livingentitypatch.playAnimationSynchronized(AVAnimations.EATING_ELITE_4, 0.0F);
//            }
//        }

//        Create VANILLA_ANIMATION
        RigAnimationId animationId = this.eatCount <= 2 ? RigAnimationId.EATING_ELITE_1 : this.eatCount <= 4 ? RigAnimationId.EATING_ELITE_2 : this.eatCount <= 6 ? RigAnimationId.EATING_ELITE_3 : RigAnimationId.EATING_ELITE_4;
        RigAnimationController.play(this,animationId);
    }

    private void ensureKnockedAnimation() {
        if (!RigAnimationController.hasActiveAnimation(this)) RigAnimationController.playHeldPose(this,RigAnimationId.KNOCKED_ELITE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (this.tickCount == 1 && !this.initialSpawn) {
                if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                    this.playSound(AnnoyingVillagersModSounds.KNOCKED_ELITE_HEROBRINE_SAY_ON_SPAWN.get(), 0.5F, 1.0F);
                }
                this.initialSpawn = true;
            }
            solidifyFeetAndStandOnTop();
            if (this.wardenCallingCooldown >= 0) {
                this.wardenCallingCooldown = this.wardenCallingCooldown - 1;
            }

            CommonUtil.stunImmunity(this,3,3);
            ensureKnockedAnimation();
            if (this.wardenCallingCooldown == 0) {
                ServerLevel level = (ServerLevel) this.level();
                HerobrineWardenEntity warden =
                        new HerobrineWardenEntity(AnnoyingVillagersModEntities.HEROBRINE_WARDEN.get(), level);
                double dist = (this.getBbWidth() + warden.getBbWidth()) * 0.5D + 0.5D;
                Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
                Vec3 spawn = this.position()
                        .subtract(forward.scale(dist))
                        .add(0.0D, 0.001D, 0.0D);

                for (int i = 0; i < 5; i++) {
                    warden.setPos(spawn.x, this.getY(), spawn.z);
                    if (level.noCollision(warden)) break;
                    spawn = spawn.subtract(forward.scale(0.3D));
                }

                warden.moveTo(spawn.x, this.getY(), spawn.z, this.yBodyRot, 0.0F);
                warden.yBodyRot = this.yBodyRot;
                warden.setYHeadRot(this.yBodyRot);

                warden.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null, null);

                warden.setEatingUUID(this.getUUID());
                level.addFreshEntity(warden);
            }
        }
    }

    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        if (this.getPersistentData().contains("FromElite")) {
            String fromElite = this.getPersistentData().getString("FromElite");
            HerobrineUtil.dropEliteHerobrineLoot(this.level(), this.getX(), this.getY(), this.getZ(), fromElite);
        }
    }

    private ItemStack randomDamage(ItemStack itemStack) {
        int maxDamage = itemStack.getMaxDamage();
        itemStack.setDamageValue(new Random().nextInt(maxDamage / 3, maxDamage * 3 / 4));
        return itemStack;
    }

    private void equipGearForLowClone(LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntity, boolean randomWeapon) {
        if (random.nextFloat() < 0.3f) {
            lowShadowHerobrineCloneEntity.setItemSlot(EquipmentSlot.HEAD, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_HELMET.get())));
        }
        if (random.nextFloat() < 0.3f) {
            lowShadowHerobrineCloneEntity.setItemSlot(EquipmentSlot.CHEST, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_CHESTPLATE.get())));
        }
        if (random.nextFloat() < 0.3f) {
            lowShadowHerobrineCloneEntity.setItemSlot(EquipmentSlot.LEGS, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_LEGGINGS.get())));
        }
        if (random.nextFloat() < 0.3f) {
            lowShadowHerobrineCloneEntity.setItemSlot(EquipmentSlot.FEET, randomDamage(new ItemStack(AnnoyingVillagersModItems.BROKEN_DIAMOND_BOOTS.get())));
        }
        if (randomWeapon) {
            lowShadowHerobrineCloneEntity.setItemSlot(EquipmentSlot.MAINHAND, randomDamage(new ItemStack(listWeapons.get(random.nextInt(listWeapons.size())))));
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData spawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        if (!pLevel.isClientSide()) {
            int d0 = (int) this.getX();
            int d1 = (int) this.getY();
            int d2 = (int) this.getZ();
            RandomSource randomSource = RandomSource.create();

            ServerLevel server = (ServerLevel) this.level();

            final BlockPos feetPos = this.blockPosition();
            final int yFeet = feetPos.getY();
            final int radius = 2;
            final int clearHeight = 3;

            final int totalProjectiles = new Random().nextInt(10, 20);
            for (int i = 0; i < totalProjectiles; i++) {
                double x = this.getX() + Mth.nextDouble(randomSource, -1.5D, 1.5D);
                double y = this.getY() + Mth.nextDouble(randomSource, 1.0D, 1.5D);
                double z = this.getZ() + Mth.nextDouble(randomSource, -1.5D, 1.5D);
                BlockProjectileEntity blockProjectileEntity = new BlockProjectileEntity(pLevel.getLevel(), this, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                blockProjectileEntity.moveTo(new Vec3(x, y, z));
                server.addFreshEntity(blockProjectileEntity);
            }

            server.explode(this, this.getX(), this.getY() + 3, this.getZ(), 3.0F, Level.ExplosionInteraction.MOB);

            for (int dy = 1; dy <= clearHeight; dy++) {
                int y = yFeet + dy;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos pos = new BlockPos(feetPos.getX() + dx, y, feetPos.getZ() + dz);
                        var state = server.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(server, pos) != -1.0F) {
                            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }

            final double forwardDist = Math.max(1.5D, this.getBbWidth() * 1.5D);
            final double sideDist = Math.max(1.2D, this.getBbWidth() * 1.2D);

            final float yaw = this.getYRot();
            final Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
            final Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            final Vec3 base = this.position().add(forward.scale(forwardDist));

            LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntityLeft = new LowShadowHerobrineCloneEntity(AnnoyingVillagersModEntities.LOW_SHADOW_HEROBRINE_CLONE.get(), pLevel.getLevel());
            lowShadowHerobrineCloneEntityLeft.moveTo(base.subtract(right.scale(sideDist)));
            equipGearForLowClone(lowShadowHerobrineCloneEntityLeft, true);
            lowShadowHerobrineCloneEntityLeft.setProtectUUID(this.getUUID());
            lowShadowHerobrineCloneEntityLeft.setProtectEntity(this);
            pLevel.getLevel().addFreshEntity(lowShadowHerobrineCloneEntityLeft);

            LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntityMiddle = new LowShadowHerobrineCloneEntity(AnnoyingVillagersModEntities.LOW_SHADOW_HEROBRINE_CLONE.get(), pLevel.getLevel());
            lowShadowHerobrineCloneEntityMiddle.moveTo(base);
            equipGearForLowClone(lowShadowHerobrineCloneEntityMiddle, false);
            lowShadowHerobrineCloneEntityMiddle.setProtectUUID(this.getUUID());
            lowShadowHerobrineCloneEntityMiddle.setProtectEntity(this);
            lowShadowHerobrineCloneEntityMiddle.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AnnoyingVillagersModItems.DIAMOND_CLAW.get()));
            lowShadowHerobrineCloneEntityMiddle.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AnnoyingVillagersModItems.ELITE_OBSIDIAN.get()));
            pLevel.getLevel().addFreshEntity(lowShadowHerobrineCloneEntityMiddle);

            LowShadowHerobrineCloneEntity lowShadowHerobrineCloneEntityRight = new LowShadowHerobrineCloneEntity(AnnoyingVillagersModEntities.LOW_SHADOW_HEROBRINE_CLONE.get(), pLevel.getLevel());
            lowShadowHerobrineCloneEntityRight.moveTo(base.add(right.scale(sideDist)));
            equipGearForLowClone(lowShadowHerobrineCloneEntityRight, true);
            lowShadowHerobrineCloneEntityRight.setProtectUUID(this.getUUID());
            lowShadowHerobrineCloneEntityRight.setProtectEntity(this);
            pLevel.getLevel().addFreshEntity(lowShadowHerobrineCloneEntityRight);

            if (this.getPersistentData().contains("FromElite") && this.getPersistentData().getString("FromElite").equals("DemoniacVoltageReaver")) {
                ItemEntity itemEntity = new ItemEntity(this.level(), d0 + Mth.nextDouble(randomSource, -5.0D, 5.0D), d1, d2 + Mth.nextDouble(randomSource, -5.0D, 5.0D), new ItemStack(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER_FRAGMENT.get()));
                itemEntity.setPickUpDelay(10);
                pLevel.addFreshEntity(itemEntity);

                ItemEntity itemEntity1 = new ItemEntity(this.level(), d0 + Mth.nextDouble(randomSource, -5.0D, 5.0D), d1, d2 + Mth.nextDouble(randomSource, -5.0D, 5.0D), new ItemStack(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER_FRAGMENT.get()));
                itemEntity1.setPickUpDelay(10);
                pLevel.addFreshEntity(itemEntity1);

                ItemEntity itemEntity2 = new ItemEntity(this.level(), d0 + Mth.nextDouble(randomSource, -5.0D, 5.0D), d1, d2 + Mth.nextDouble(randomSource, -5.0D, 5.0D), new ItemStack(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER_FRAGMENT.get()));
                itemEntity2.setPickUpDelay(10);
                pLevel.addFreshEntity(itemEntity2);

                ItemEntity itemEntity3 = new ItemEntity(this.level(), d0 + Mth.nextDouble(randomSource, -5.0D, 5.0D), d1, d2 + Mth.nextDouble(randomSource, -5.0D, 5.0D), new ItemStack(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER_BLADE.get()));
                itemEntity3.setPickUpDelay(10);
                pLevel.addFreshEntity(itemEntity3);
            }

            this.wardenCallingCooldown = 1200;
        }
        return spawnGroupData;
    }

    @Override
    public void baseTick() {
        super.baseTick();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity other) {
    }

    @Override
    public void push(@NotNull Entity other) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes();

        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.06D);
        builder = builder.add(Attributes.MAX_HEALTH, 20.0D);
        builder = builder.add(Attributes.ARMOR, 0.0D);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 1.0D);
        builder = builder.add(Attributes.FOLLOW_RANGE, 24.0D);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        return builder;
    }
}
