package com.pla.annoyingvillagers.client.engine;

import com.pla.annoyingvillagers.compat.photon.PhotonClientFxUtil;
import com.pla.annoyingvillagers.client.animation.RigClientAnimationState;
import com.pla.annoyingvillagers.event.NoVfxPortalEvent;
import com.pla.annoyingvillagers.config.AnnoyingVillagersClientConfig.VfxEffect;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModParticleTypes;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.item.BlackFireSwordItem;
import com.pla.annoyingvillagers.item.EnderGlaiveItem;
import com.pla.annoyingvillagers.network.*;
import com.pla.annoyingvillagers.util.AAAParticlesUtil;
import com.pla.annoyingvillagers.util.ExplosionFxMute;
import com.pla.annoyingvillagers.util.HerobrineUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {
    private static final DustParticleOptions DIAMOND_GREEN_DUST =
            new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.3F), 1.15F);
    private static final DustParticleOptions DIAMOND_GLOW_DUST =
            new DustParticleOptions(new Vector3f(0.9F, 1.0F, 0.2F), 0.9F);

    private ClientPacketHandlers() {}

    private static Vec3 randomUnit(RandomSource rand) {
        double z = rand.nextDouble() * 2.0 - 1.0;
        double a = rand.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }

    private static void spawnParticle(Level level, ParticleOptions particle, Vec3 pos, Vec3 velocity) {
        level.addParticle(particle, true, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
    }

    private static void spawnSpreadParticle(Level level, ParticleOptions particle, Vec3 center, RandomSource rand,
                                            double xOffset, double yOffset, double zOffset, double speed) {
        Vec3 pos = center.add(
                (rand.nextDouble() * 2.0D - 1.0D) * xOffset,
                (rand.nextDouble() * 2.0D - 1.0D) * yOffset,
                (rand.nextDouble() * 2.0D - 1.0D) * zOffset);
        Vec3 velocity = speed == 0.0D ? Vec3.ZERO : randomUnit(rand).scale(speed);
        spawnParticle(level, particle, pos, velocity);
    }

    private static Vec3 randomRaisedSpread(Vec3 center, RandomSource rand,
                                           double xOffset, double minYOffset, double maxYOffset, double zOffset) {
        return center.add(
                (rand.nextDouble() * 2.0D - 1.0D) * xOffset,
                minYOffset + rand.nextDouble() * Math.max(0.0D, maxYOffset - minYOffset),
                (rand.nextDouble() * 2.0D - 1.0D) * zOffset);
    }

    private static String pulseKey(String prefix, int entityId, int tickCount, String suffix) {
        return prefix + ":" + entityId + ":" + tickCount + suffix;
    }

    private static boolean followPhotonEntity(Level level, String effectPath, String key, Entity entity,
                                              Vec3 offset, int lifetimeTicks) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }

        Vec3 fixedOffset = offset == null ? Vec3.ZERO : offset;
        return PhotonClientFxUtil.followPosition(key, level, effectPath,
                () -> entity.isRemoved() ? null : entity.position().add(fixedOffset), lifetimeTicks);
    }

    public static void handleMuteExplosionAtPos(ClientboundMuteExplosionAtPos msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        ExplosionFxMute.mark(msg.pos().asLong(), level.getGameTime() + msg.lifetimeTicks());
    }

    public static void handleHerobrinePortalFx(ClientboundHerobrinePortalFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.HEROBRINE_PORTAL,
                () -> PhotonClientFxUtil.spawnAt(level, "normalsummoning", msg.from().add(0.0D, 1.0D, 0.0D)),
                () -> {
                    AAAParticlesUtil.sendHerobrinePortal(level, msg.from().x, msg.from().y, msg.from().z);
                    return true;
                },
                () -> NoVfxPortalEvent.spawn(msg.from(), 60));
    }

    public static void handleHerobrineAssistanceFx(ClientboundHerobrineAssistanceFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.HEROBRINE_ASSISTANCE,
                () -> PhotonClientFxUtil.spawnAt(level, "requestingassistance",  msg.from().add(0.0D, 1.0D, 0.0D)),
                () -> {
                    AAAParticlesUtil.sendHerobrineAssistance(level, msg.from().x, msg.from().y, msg.from().z);
                    return true;
                },
                () -> HerobrineUtil.startHerobrineAssistanceFallback(level, msg.from()));
    }

    public static void handleEnderAegisSparkFx(ClientboundEnderAegisSparkFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.ENDER_AEGIS_SPARK,
                () -> PhotonClientFxUtil.spawnDirectional(level, "aegissparks", msg.from(), msg.to(), false),
                () -> {
                    RandomSource rand = level.getRandom();
                    for (int i = 0; i < 300; i++) {
                        Vec3 velocity = randomUnit(rand).scale(0.02D + rand.nextDouble() * 0.20D);
                        spawnParticle(level, AnnoyingVillagersModParticleTypes.SPARK.get(), msg.to(), velocity);
                    }
                });
    }

    public static void handleEliteHerobrineFx(ClientboundEliteHerobrineFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        RandomSource rand = level.getRandom();
        spawnSpreadParticle(level,AnnoyingVillagersModParticleTypes.PE.get(),msg.pos(),rand,0.4D,1.1D,0.4D,0.0D);
        if (msg.extraParticle()) {
            spawnSpreadParticle(level,AnnoyingVillagersModParticleTypes.PE.get(),msg.pos(),rand,0.45D,1.5D,0.3D,0.0D);
        }

        ClientVfxRouter.run(
                VfxEffect.ELITE_HEROBRINE,
                () -> {
                    if (!PhotonClientFxUtil.isLoaded()) {
                        return false;
                    }
                    if (msg.tickCount() % 5 != 0) {
                        return true;
                    }

                    Entity entity = level.getEntity(msg.entityId());
                    Vec3 mainOffset = randomRaisedSpread(Vec3.ZERO, rand, 0.4D, 0.65D, 1.6D, 0.4D);
                    boolean handled = followPhotonEntity(level, "reaverlightning",
                            pulseKey("elite-herobrine", msg.entityId(), msg.tickCount(), ":main"),
                            entity, mainOffset, 22);
                    if (!handled) {
                        handled = PhotonClientFxUtil.spawnAt(level, "reaverlightning", msg.pos().add(mainOffset));
                    }

                    if (msg.extraParticle()) {
                        Vec3 extraOffset = randomRaisedSpread(Vec3.ZERO, rand, 0.45D, 0.75D, 2.0D, 0.3D);
                        boolean extraHandled = followPhotonEntity(level, "reaverlightning",
                                pulseKey("elite-herobrine", msg.entityId(), msg.tickCount(), ":extra"),
                                entity, extraOffset, 22);
                        if (!extraHandled) {
                            extraHandled = PhotonClientFxUtil.spawnAt(level, "reaverlightning", msg.pos().add(extraOffset));
                        }

                        handled = handled || extraHandled;
                    }

                    return handled;
                },
                () -> {
                });
    }

    public static void handleBlueDemonEffectFx(ClientboundBlueDemonEffectFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.BLUE_DEMON_LIGHTNING,
                () -> {
                    if (!PhotonClientFxUtil.isLoaded()) {
                        return false;
                    }

                    boolean handled;
                    if (msg.followEntity()) {
                        if (msg.tickCount() % 5 != 0) {
                            return true;
                        }

                        handled = followPhotonEntity(level, "bluedemonlightning",
                                pulseKey("blue-demon-chestplate", msg.entityId(), msg.tickCount(), ""),
                                level.getEntity(msg.entityId()), Vec3.ZERO, 22);
                        if (!handled) {
                            handled = PhotonClientFxUtil.spawnAt(level, "bluedemonlightning", msg.pos());
                        }
                    } else {
                        handled = PhotonClientFxUtil.spawnAt(level, "bluedemonlightning", msg.pos());
                    }

                    return handled;
                },
                () -> {
                    RandomSource rand = level.getRandom();
                    int count = Math.max(1, msg.count());
                    for (int i = 0; i < count; i++) {
                        spawnSpreadParticle(level, AnnoyingVillagersModParticleTypes.ELECTRIC_SPARK.get(), msg.pos(), rand,
                                msg.xOffset(), msg.yOffset(), msg.zOffset(), msg.speed());
                    }
                });
    }

    public static void handleTeleportPortalFx(ClientboundTeleportPortalFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.TELEPORT_PORTAL,
                () -> PhotonClientFxUtil.spawnPortal(level, "snakeportal", msg.pos(), msg.normal()),
                () -> AAAParticlesUtil.sendTeleportPortal(level, msg.pos(), msg.normal()),
                () -> {
                });
    }

    public static void handleWoopieSwordWind(ClientboundWoopieSwordWindFx msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ClientVfxRouter.run(
                VfxEffect.WOOPIE_SWORD_WIND,
                () -> PhotonClientFxUtil.spawnAt(level, "whoopiewind", msg.from()),
                () -> {
                    AAAParticlesUtil.sendWoopieWind(level, msg.from().x, msg.from().y, msg.from().z);
                    return true;
                },
                () -> {
                    RandomSource rand = level.getRandom();

                    int rings = 3;
                    int pointsPerRing = 36;
                    double baseRadius = 0.9;
                    double radiusStep = 0.35;
                    double baseY = 0.15;
                    double yStep = 0.18;

                    double tangentialSpeed = 0.14;
                    double outwardSpeed = 0.03;

                    for (int r = 0; r < rings; r++) {
                        double radius = baseRadius + r * radiusStep;
                        double yOff = baseY + r * yStep;

                        for (int i = 0; i < pointsPerRing; i++) {
                            double a = (i / (double) pointsPerRing) * Math.PI * 2.0 + rand.nextDouble() * 0.12;
                            double cos = Math.cos(a);
                            double sin = Math.sin(a);

                            double px = msg.from().x + cos * radius;
                            double py = msg.from().y + yOff + (rand.nextDouble() - 0.5) * 0.06;
                            double pz = msg.from().z + sin * radius;
                            double vx = (-sin) * tangentialSpeed + cos * outwardSpeed;
                            double vy = 0.01 + rand.nextDouble() * 0.02;
                            double vz = ( cos) * tangentialSpeed + sin * outwardSpeed;

                            level.addParticle(ParticleTypes.CLOUD, true, px, py, pz, vx, vy, vz);
                            if ((i & 3) == 0) {
                                level.addParticle(ParticleTypes.SMOKE, true, px, py, pz, vx * 0.35, vy * 0.2, vz * 0.35);
                            }
                        }
                    }

                    for (int i = 0; i < 14; i++) {
                        double vx = (rand.nextDouble() - 0.5) * 0.25;
                        double vy = 0.03 + rand.nextDouble() * 0.18;
                        double vz = (rand.nextDouble() - 0.5) * 0.25;
                        level.addParticle(ParticleTypes.POOF, true, msg.from().x, msg.from().y + 0.25, msg.from().z, vx, vy, vz);
                    }

                    level.addParticle(ParticleTypes.EXPLOSION, true, msg.from().x, msg.from().y + 0.35, msg.from().z, 0.0, 0.0, 0.0);
                });

        level.playLocalSound(msg.from().x, msg.from().y, msg.from().z, AnnoyingVillagersModSounds.WOOPIE_WIND.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F, false);
    }

    public static void handleGroundFracture(ClientboundGroundFracture msg) {
        GroundFractureClient.handle(msg);
    }

    public static void handleRigAnimation(ClientboundRigAnimation msg) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        RigClientAnimationState.start(msg.entityId(), msg.animationId(), msg.durationTicks());
    }

    public static void handleBlackFire(ClientboundBlackFireFx msg) {
        Level level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(msg.entityId());
        ClientVfxRouter.run(
                VfxEffect.BLACK_FIRE,
                () -> entity != null && PhotonClientFxUtil.followPosition(
                        "blackfire:" + msg.entityId(),
                        level,
                        "blackfire",
                        () -> {
                            Entity current = level.getEntity(msg.entityId());
                            return current == null || !current.isAlive() || current.isRemoved()
                                    ? null
                                    : BlackFireSwordItem.getBlackFireFallbackPosition(current);
                        },
                        60),
                () -> {
                    if (entity == null) {
                        return false;
                    }

                    AAAParticlesUtil.sendBlackFire(level, entity);
                    return true;
                },
                () -> BlackFireSwordItem.startBlackFireFallback(level, msg.entityId()));
    }

    public static void handleDiamondAttractor(ClientboundDiamondAttractorFx msg) {
        Level level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        Entity entity = level.getEntity(msg.entityId());
        ClientVfxRouter.run(
                VfxEffect.DIAMOND_ATTRACTOR,
                () -> entity != null && PhotonClientFxUtil.spawnAt(level, "av_attractor", BlackFireSwordItem.getSwordOrBodyPosition(entity)),
                () -> {
                    if (entity == null) {
                        return false;
                    }

                    AAAParticlesUtil.sendDiamondAttractor(level, entity);
                    return true;
                },
                () -> {
                    if (entity == null) {
                        return;
                    }

                    RandomSource rand = level.getRandom();
                    Vec3 center = BlackFireSwordItem.getSwordOrBodyPosition(entity);
                    Vec3 forward = entity.getLookAngle();

                    if (forward.lengthSqr() < 1.0e-6D) {
                        forward = new Vec3(0.0D, 0.0D, 1.0D);
                    }

                    forward = forward.normalize();

                    Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
                    if (side.lengthSqr() < 1.0e-6D) {
                        side = new Vec3(1.0D, 0.0D, 0.0D);
                    }
                    side = side.normalize();

                    Vec3 up = side.cross(forward).normalize();

                    for (int i = 0; i < 86; i++) {
                        double progress = i / 85.0D;
                        double angle = progress * Math.PI * 4.0D + rand.nextDouble() * 0.18D;
                        double radius = 0.16D + Math.sin(progress * Math.PI) * 0.72D;

                        Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                        Vec3 pos = center
                                .add(forward.scale((progress - 0.5D) * 1.25D))
                                .add(radial.scale(radius));

                        Vec3 tangent = forward.cross(radial).normalize().scale(0.065D + rand.nextDouble() * 0.04D);
                        Vec3 pull = center.subtract(pos).normalize().scale(0.025D);
                        Vec3 velocity = tangent.add(pull).add(forward.scale(0.015D));

                        spawnParticle(level, rand.nextBoolean() ? DIAMOND_GREEN_DUST : DIAMOND_GLOW_DUST, pos, velocity);

                        if ((i & 3) == 0) {
                            spawnParticle(level, ParticleTypes.END_ROD, pos, velocity.scale(0.4D));
                        }

                        if (i % 6 == 0) {
                            spawnParticle(level, ParticleTypes.ELECTRIC_SPARK, pos, velocity.scale(0.7D));
                        }
                    }

                    for (int i = 0; i < 32; i++) {
                        Vec3 offset = randomUnit(rand).scale(0.85D + rand.nextDouble() * 1.25D);
                        Vec3 pos = center.add(offset);
                        Vec3 velocity = center.subtract(pos).normalize().scale(0.07D + rand.nextDouble() * 0.05D);

                        spawnParticle(level, ParticleTypes.ENCHANT, pos, velocity);
                        if ((i & 1) == 0) {
                            spawnParticle(level, DIAMOND_GREEN_DUST, pos, velocity);
                        }
                    }
                });
    }

    public static void handlePlayerGroundTransitionPosition(ClientboundPlayerGroundTransitionPosition msg) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.setDeltaMovement(Vec3.ZERO);
        minecraft.player.fallDistance = 0.0F;
        minecraft.player.setPos(msg.x(), msg.y(), msg.z());
    }

}
