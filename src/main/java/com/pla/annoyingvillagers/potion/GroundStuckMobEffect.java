package com.pla.annoyingvillagers.potion;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModMobEffects;
import com.pla.annoyingvillagers.network.ClientboundGroundStuckKnockoutFx;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.util.CommonUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroundStuckMobEffect extends MobEffect {
    public static final int DEFAULT_DURATION = 20 * 8;
    public static final int DEFAULT_AMPLIFIER = 0;
    public static final int VANILLA_FALLBACK_DURATION = 20 * 2;
    public static final float BASE_KNOCKOUT_CHANCE = 0.35F;
    public static final int KNOCKOUT_TICKS = 60;
    private static final double GROUND_STUCK_SLAM_RADIUS = 0.8D;
    private static final int GROUND_STUCK_SLAM_PARTICLES = 35;
    private static final double GROUND_STUCK_SLAM_SPREAD = 0.7D;
    private static final double GROUND_STUCK_FRACTURE_RADIUS = 2.5D;
    public static final String NBT_STUCK = "AVGroundStuck";
    public static final String NBT_ANCHOR_X = "AVGroundStuckX";
    public static final String NBT_ANCHOR_Y = "AVGroundStuckY";
    public static final String NBT_ANCHOR_Z = "AVGroundStuckZ";
    public static final String NBT_GROUND_POS = "AVGroundStuckGroundPos";
    public static final String NBT_GROUND_TOP = "AVGroundStuckGroundTop";
    public static final String NBT_STUN_TICKS = "AVGroundStuckStunTicks";
    public static final String NBT_KNOCKOUT_TICKS = "AVGroundStuckKnockoutTicks";
    public static final String NBT_KNOCKOUT_SOURCE = "AVGroundStuckKnockoutSource";

    public GroundStuckMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x594636);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "d5feeedf-a102-45d0-9528-5918efe5b5b5", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public @NotNull String getDescriptionId() {
        return "effect.annoyingvillagers.ground_stuck";
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        CompoundTag tag = entity.getPersistentData();

        /*
         * Knockout owns the entity until its launch/tumble finishes. If Ground Stuck
         * is reapplied during that window (another wave, another hit, or /effect spam),
         * do not let startStuck() zero the launch velocity and pin the entity back to
         * the ground. The old Ground Stuck instance is supposed to be gone once the
         * knockout succeeds.
         */
        if (tag.getInt(NBT_KNOCKOUT_TICKS) > 0) {
            entity.removeEffect(AnnoyingVillagersModMobEffects.GROUND_STUCK.get());
            return;
        }

        if (!tag.getBoolean(NBT_STUCK) && !startStuck(level, entity)) {
            entity.removeEffect(AnnoyingVillagersModMobEffects.GROUND_STUCK.get());
            return;
        }
        if (!isAnchorValid(level, entity)) {
            clear(entity);
            return;
        }

        double x = tag.getDouble(NBT_ANCHOR_X);
        double y = tag.getDouble(NBT_ANCHOR_Y);
        double z = tag.getDouble(NBT_ANCHOR_Z);
        double anchorDistanceSqr = entity.distanceToSqr(x, y, z);

        // Keep the old escape/forced-move behavior for mobs. A ServerPlayer is different:
        // client movement packets can move its server position away from a plain setPos(),
        // so it must be corrected instead of clearing the effect. Ender pearls still clear
        // Ground Stuck explicitly in GroundStuckEvent before their teleport happens.
        if (!(entity instanceof ServerPlayer) && anchorDistanceSqr > 6.25D) {
            clear(entity);
            return;
        }

        entity.xxa = 0.0F;
        entity.yya = 0.0F;
        entity.zza = 0.0F;
        entity.fallDistance = 0.0F;
        entity.setDeltaMovement(Vec3.ZERO);
        enforceAnchor(entity, x, y, z, anchorDistanceSqr > 1.0E-4D);
        entity.hasImpulse = true;
        entity.hurtMarked = true;

        int stunTicks = tag.getInt(NBT_STUN_TICKS);
        if (stunTicks <= 1) {
            tag.putInt(NBT_STUN_TICKS, 20);
            applyPeriodicStun(entity);
        } else {
            tag.putInt(NBT_STUN_TICKS, stunTicks - 1);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public static void apply(LivingEntity entity) {
        apply(entity, DEFAULT_DURATION, DEFAULT_AMPLIFIER);
    }

    public static void apply(LivingEntity entity, int duration, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        // A fresh Ground Stuck application must not cancel an active knockout launch.
        if (entity.getPersistentData().getInt(NBT_KNOCKOUT_TICKS) > 0) {
            return;
        }

        entity.addEffect(new MobEffectInstance(AnnoyingVillagersModMobEffects.GROUND_STUCK.get(), duration, amplifier, false, false, false));

        // Start the anchor immediately instead of waiting for the next potion tick. This
        // matters most for ServerPlayer because its own client is authoritative enough to
        // keep rendering the old position until the server sends a teleport packet.
        if (!entity.getPersistentData().getBoolean(NBT_STUCK)) {
            startStuck(level, entity);
        }

        // Players and mobs that do not implement the vanilla rig stun system cannot
        // play HIT_LEFT/HIT_RIGHT, so give them a short vanilla feedback package.
        if (!supportsRigHitAnimation(entity)) {
            applyVanillaFallback(entity);
        }
    }

    public static void clear(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            releasePlayerAnchor(entity);
            entity.removeEffect(AnnoyingVillagersModMobEffects.GROUND_STUCK.get());
        }
        clearData(entity);
    }

    public static void clearData(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.remove(NBT_STUCK);
        tag.remove(NBT_ANCHOR_X);
        tag.remove(NBT_ANCHOR_Y);
        tag.remove(NBT_ANCHOR_Z);
        tag.remove(NBT_GROUND_POS);
        tag.remove(NBT_GROUND_TOP);
        tag.remove(NBT_STUN_TICKS);
    }

    public static float getKnockoutChance(int amplifier) {
        return Math.max(0.03F, BASE_KNOCKOUT_CHANCE / (amplifier + 1.0F));
    }

    public static boolean isCustomGroundHitAnimation(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !RigStunController.supports(mob)) return false;
        RigAnimationId animation = RigAnimationController.getActiveAnimationId(mob);
        return animation == RigAnimationId.HIT_LEFT || animation == RigAnimationId.HIT_RIGHT;
    }

    /**
     * Rig mobs use the requested vanilla-rig HIT_LEFT/HIT_RIGHT reaction. Players
     * and other non-rig entities fall back to nausea/slowness/mining fatigue for 2s.
     */
    public static void playGroundHitReaction(LivingEntity entity) {
        if (entity instanceof Mob mob && RigStunController.supports(mob)) {
            if (!isCustomGroundHitAnimation(entity)) {
                RigStunController.applyHitAnimation(mob,
                        mob.getRandom().nextBoolean() ? RigAnimationId.HIT_LEFT : RigAnimationId.HIT_RIGHT);
            }
            return;
        }
        applyVanillaFallback(entity);
    }

    public static void knockOut(LivingEntity entity, @Nullable LivingEntity attacker) {
        clear(entity);

        CompoundTag tag = entity.getPersistentData();
        tag.putInt(NBT_KNOCKOUT_TICKS, KNOCKOUT_TICKS);
        if (attacker != null) tag.putUUID(NBT_KNOCKOUT_SOURCE, attacker.getUUID());

        float yaw = attacker != null ? attacker.getYRot() : entity.getYRot();
        double radians = Math.toRadians(yaw);
        Vec3 direction = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();

        double horizontalPower = 3.0D;
        double verticalPower = 1.30D;
        Vec3 requestedMotion = new Vec3(
                direction.x * horizontalPower,
                verticalPower,
                direction.z * horizontalPower
        );

        entity.setDeltaMovement(requestedMotion);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        syncKnockout(entity, KNOCKOUT_TICKS);
    }

    public static void syncKnockout(LivingEntity entity, int ticks) {
        if (!entity.level().isClientSide()) {
            AnnoyingVillagers.PACKET_HANDLER.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                    new ClientboundGroundStuckKnockoutFx(entity.getId(), ticks)
            );
        }
    }

    private static boolean startStuck(ServerLevel level, LivingEntity entity) {
        if (entity.getPersistentData().getInt(NBT_KNOCKOUT_TICKS) > 0) {
            return false;
        }

        Support support = findSupport(level, entity);
        if (support == null) return false;

        CompoundTag tag = entity.getPersistentData();
        double x = entity.getX();
        double z = entity.getZ();
        double y = support.topY - entity.getBbHeight() * 0.5D;
        tag.putBoolean(NBT_STUCK, true);
        tag.putDouble(NBT_ANCHOR_X, x);
        tag.putDouble(NBT_ANCHOR_Y, y);
        tag.putDouble(NBT_ANCHOR_Z, z);
        tag.putLong(NBT_GROUND_POS, support.pos.asLong());
        tag.putDouble(NBT_GROUND_TOP, support.topY);
        tag.putInt(NBT_STUN_TICKS, 20);

        entity.fallDistance = 0.0F;
        entity.setDeltaMovement(Vec3.ZERO);
        enforceAnchor(entity, x, y, z, true);
        entity.hasImpulse = true;
        entity.hurtMarked = true;

        // Give each successful anchor one medium ground-slam impact. This runs only
        // when the stuck anchor is first created, never on its subsequent effect ticks.
        CommonUtil.spawnGroundSlamFracture(
                entity,
                level,
                new Vec3(x, support.topY, z),
                GROUND_STUCK_SLAM_RADIUS,
                GROUND_STUCK_SLAM_PARTICLES,
                GROUND_STUCK_SLAM_SPREAD,
                GROUND_STUCK_FRACTURE_RADIUS
        );
        return true;
    }


    private static void releasePlayerAnchor(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        CompoundTag tag = entity.getPersistentData();
        if (!tag.getBoolean(NBT_STUCK) || !tag.contains(NBT_GROUND_TOP)) return;

        double x = tag.contains(NBT_ANCHOR_X) ? tag.getDouble(NBT_ANCHOR_X) : player.getX();
        double z = tag.contains(NBT_ANCHOR_Z) ? tag.getDouble(NBT_ANCHOR_Z) : player.getZ();
        double y = tag.getDouble(NBT_GROUND_TOP) + 0.01D;
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static void enforceAnchor(LivingEntity entity, double x, double y, double z, boolean syncPlayer) {
        if (entity instanceof ServerPlayer player) {
            if (syncPlayer) {
                player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
            } else {
                player.setPos(x, y, z);
            }
            return;
        }
        entity.setPos(x, y, z);
    }

    private static boolean isAnchorValid(ServerLevel level, LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(NBT_GROUND_POS)) return false;
        BlockPos pos = BlockPos.of(tag.getLong(NBT_GROUND_POS));
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.of(entity));
        if (shape.isEmpty()) return false;
        return Math.abs(pos.getY() + shape.max(Direction.Axis.Y) - tag.getDouble(NBT_GROUND_TOP)) <= 0.125D;
    }

    private static void applyPeriodicStun(LivingEntity entity) {
        if (entity instanceof Mob mob && RigStunController.supports(mob) && !isCustomGroundHitAnimation(entity)) {
            RigStunController.applyHitAnimation(mob,
                    mob.getRandom().nextBoolean() ? RigAnimationId.HIT_LEFT : RigAnimationId.HIT_RIGHT);
        }
    }

    private static boolean supportsRigHitAnimation(LivingEntity entity) {
        return entity instanceof Mob mob && RigStunController.supports(mob);
    }

    private static void applyVanillaFallback(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, VANILLA_FALLBACK_DURATION, 0, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, VANILLA_FALLBACK_DURATION, 1, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, VANILLA_FALLBACK_DURATION, 1, false, false, true));
    }

    @Nullable
    private static Support findSupport(CollisionGetter level, LivingEntity entity) {
        double footY = entity.getBoundingBox().minY;
        BlockPos start = BlockPos.containing(entity.getX(), footY - 0.05D, entity.getZ());
        for (int i = 0; i <= 2; i++) {
            BlockPos pos = start.below(i);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.of(entity));
            if (shape.isEmpty()) continue;
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (topY <= footY + 0.35D && footY - topY <= 1.5D) {
                return new Support(pos.immutable(), topY);
            }
        }
        return null;
    }

    private record Support(BlockPos pos, double topY) {}
}
