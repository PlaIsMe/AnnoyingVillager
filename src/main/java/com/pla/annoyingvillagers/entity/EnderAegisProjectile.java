package com.pla.annoyingvillagers.entity;

import java.util.Objects;
import java.util.Random;

import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModParticleTypes;
import com.pla.annoyingvillagers.client.particle.HitParticleType;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.rig.RigStunController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class EnderAegisProjectile extends AbstractArrow implements ItemSupplier {
    private static final int SLAM_PULSE_INTERVAL_TICKS = 20;
    private static final int ELITE_FX_INTERVAL_TICKS = 4;
    private static final int MAX_LIFETIME_TICKS = 100;

    public EnderAegisProjectile(SpawnEntity spawnentity, Level level) {
        super(AnnoyingVillagersModEntities.ENDER_AEGIS_PROJECTILE.get(), level);
    }

    public EnderAegisProjectile(EntityType<? extends EnderAegisProjectile> entitytype, Level level) {
        super(entitytype, level);
    }

    public EnderAegisProjectile(EntityType<? extends EnderAegisProjectile> entitytype, double d0, double d1, double d2, Level level) {
        super(entitytype, d0, d1, d2, level);
    }

    public EnderAegisProjectile(EntityType<? extends EnderAegisProjectile> entitytype, LivingEntity livingentity, Level level) {
        super(entitytype, livingentity, level);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @OnlyIn(Dist.CLIENT)
    public @NotNull ItemStack getItem() {
        return new ItemStack(Blocks.AIR);
    }

    public @NotNull ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    public void tick() {
        super.tick();
        if (this.inGround || this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        if (!this.level().isClientSide()) {
            if (this.tickCount == 1 || this.tickCount % ELITE_FX_INTERVAL_TICKS == 0) {
                HerobrineUtil.spawnEliteEffect(this.level(), this.getX(), this.getY(), this.getZ(), this);
            }
            if (this.tickCount == 1 || (this.tickCount + this.getId()) % SLAM_PULSE_INTERVAL_TICKS == 0) {
                doGroundSlamAtSelf();
            }
        }
    }

    private void doGroundSlamAtSelf() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        BlockPos floor = BlockPos.containing(this.position().x, this.position().y - 1.0, this.position().z);
        Vec3 center = new Vec3(this.getX(), floor.getY(), this.getZ());
        Entity src = (this.getOwner() != null) ? this.getOwner() : this;
        if (src instanceof LivingEntity livingSrc) {
            CommonUtil.circleSlamFracture(livingSrc, serverLevel, center, 3.5D, true, true, true);
        }
    }

    public static EnderAegisProjectile shoot(Level level, LivingEntity livingentity, Random random, float f, double d0, int i) {
        EnderAegisProjectile enderAegisProjectile = new EnderAegisProjectile(AnnoyingVillagersModEntities.ENDER_AEGIS_PROJECTILE.get(), livingentity, level);

        enderAegisProjectile.shoot(livingentity.getViewVector(1.0F).x, livingentity.getViewVector(1.0F).y, livingentity.getViewVector(1.0F).z, f * 2.0F, 0.0F);
        enderAegisProjectile.setSilent(true);
        enderAegisProjectile.setCritArrow(false);
        enderAegisProjectile.setBaseDamage(d0);
        enderAegisProjectile.setKnockback(i);
        level.addFreshEntity(enderAegisProjectile);
        level.playSound(null, livingentity.getX(), livingentity.getY(), livingentity.getZ(), Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.arrow.shoot"))), SoundSource.PLAYERS, 1.0F, 1.0F / (random.nextFloat() * 0.5F + 1.0F) + f / 2.0F);
        return enderAegisProjectile;
    }

    public static EnderAegisProjectile shoot(LivingEntity livingentity, LivingEntity livingentity1) {
        EnderAegisProjectile enderAegisProjectile = new EnderAegisProjectile(AnnoyingVillagersModEntities.ENDER_AEGIS_PROJECTILE.get(), livingentity, livingentity.level());
        double d0 = livingentity1.getX() - livingentity.getX();
        double d1 = livingentity1.getY() + (double) livingentity1.getEyeHeight() - 1.1D;
        double d2 = livingentity1.getZ() - livingentity.getZ();

        enderAegisProjectile.shoot(d0, d1 - enderAegisProjectile.getY() + Math.hypot(d0, d2) * 0.20000000298023224D, d2, 2.0F, 12.0F);
        enderAegisProjectile.setSilent(true);
        enderAegisProjectile.setBaseDamage(18.0D);
        enderAegisProjectile.setKnockback(7);
        enderAegisProjectile.setCritArrow(false);
        livingentity.level().addFreshEntity(enderAegisProjectile);
        livingentity.level().playSound(null, livingentity.getX(), livingentity.getY(), livingentity.getZ(), Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.arrow.shoot"))), SoundSource.PLAYERS, 1.0F, 1.0F / ((new Random()).nextFloat() * 0.5F + 1.0F));
        return enderAegisProjectile;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        Entity owner = this.getOwner();
        if (entity == owner) return false;
        if (owner instanceof LivingEntity livingOwner && entity instanceof LivingEntity livingTarget && livingOwner.isAlliedTo(livingTarget)) return false;
        return super.canHitEntity(entity);
    }

    private void playStunAnimation(Entity victim) {
//          ADD THIS CODE IN AV_EFM
//            LivingEntityPatch<?> livingEntityPatch = EpicFightCapabilities.getEntityPatch(victim, LivingEntityPatch.class);
//            if (livingEntityPatch != null) {
//                livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.LONGEST_HIT, 0.0F);
//            }
        if (!this.level().isClientSide && victim instanceof LivingEntity livingVictim && livingVictim.isAlive()) {
            if (livingVictim instanceof Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            } else if (livingVictim instanceof Mob mob && RigStunController.supports(mob)) {
                RigStunController.applySuperKnockback(mob);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity vicTim = pResult.getEntity();
        Entity owner = this.getOwner();
        if (vicTim == owner) return;
        if (vicTim.level() instanceof ServerLevel serverLevel) {
            AnnoyingVillagersModParticleTypes.HIT_BLUNT.get().spawnParticleWithArgument(
                    serverLevel,
                    HitParticleType.FRONT_OF_EYES,
                    HitParticleType.ZERO,
                    vicTim,
                    owner == null ? this : owner
            );
            serverLevel.sendParticles(AnnoyingVillagersModParticleTypes.SPARK.get(),
                    vicTim.getX(), vicTim.getY() + 1.5, vicTim.getZ() + 0.8,
                    5,
                    0, 0, 0,
                    0.1);
            playStunAnimation(vicTim);
        }
        super.onHitEntity(pResult);
    }
}
