package com.pla.annoyingvillagers.client.engine;

import com.pla.annoyingvillagers.block.FractureBlock;
import com.pla.annoyingvillagers.block.FractureBlockState;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModParticleTypes;
import com.pla.annoyingvillagers.network.ClientboundGroundFracture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class GroundFractureClient {
    private static final Vec3 IMPACT_DIRECTION = new Vec3(0.0D, -1.0D, 0.0D);

    private GroundFractureClient() {}

    public static void handle(ClientboundGroundFracture msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        circleSlamFracture(level, msg.location(), msg.radius(), msg.noSound(), msg.noParticle());
    }

    public static boolean circleSlamFracture(ClientLevel level, Vec3 center, double radius, boolean noSound, boolean noParticle) {
        if (level == null || center == null) return false;

        center = snapSlamCenter(center);
        BlockPos origin = BlockPos.containing(center);
        BlockState originState = level.getBlockState(origin);
        if (!canTransferShockWave(level, origin, originState)) return false;

        radius = Math.max(0.5D, radius);
        int xFrom = Mth.floor(center.x - radius);
        int xTo = Mth.ceil(center.x + radius);
        int zFrom = Mth.floor(center.z - radius);
        int zTo = Mth.ceil(center.z + radius);

        for (int z = zFrom; z <= zTo; z++) {
            for (int x = xFrom; x <= xTo; x += z == zFrom || z == zTo ? 1 : xTo - xFrom) {
                Vec3 direction = new Vec3(x - center.x + 0.1D, 0.0D, z - center.z);
                spreadShockwave(level, center, direction, radius, x, z, noParticle);
            }
        }

        boolean smallSlam = radius < 1.5D;
        if (!noSound) {
            level.playLocalSound(center.x, center.y, center.z, smallSlam ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, smallSlam ? 0.8F : 1.4F, smallSlam ? 1.1F : 0.75F + level.random.nextFloat() * 0.1F, false);
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.9F, 0.75F + level.random.nextFloat() * 0.2F, false);
        }

        if (!smallSlam && !noParticle) level.addParticle(AnnoyingVillagersModParticleTypes.GROUND_SLAM.get(), center.x, center.y, center.z, 1.0D, radius * 10.0D, 0.5D);
        return true;
    }

    private static void spreadShockwave(ClientLevel level, Vec3 center, Vec3 direction, double length, int edgeX, int edgeZ, boolean noParticle) {
        if (direction.lengthSqr() < 1.0E-8D) return;

        Vec3 edgeOfShockwave = center.add(direction.normalize().scale(length));
        int xFrom = Math.min(Mth.floor(center.x), edgeX);
        int xTo = Math.max(Mth.floor(center.x), edgeX);
        int zFrom = Math.min(Mth.floor(center.z), edgeZ);
        int zTo = Math.max(Mth.floor(center.z), edgeZ);
        List<BlockPos> affectedBlocks = new ArrayList<>();
        double bounceExponentCoef = Math.min(1.0D / (length * length), 0.1D);

        for (int z = zFrom; z <= zTo; z++) {
            for (int x = xFrom; x <= xTo; x++) {
                if (isBlockOverlapLine(x, z, center, edgeOfShockwave)) affectedBlocks.add(new BlockPos(x, 0, z));
            }
        }

        affectedBlocks.sort(Comparator.comparingDouble(pos -> Math.pow(pos.getX() - center.x, 2.0D) + Math.pow(pos.getZ() - center.z, 2.0D)));
        double y = center.y;

        for (BlockPos coordinate : affectedBlocks) {
            BlockPos blockPos = BlockPos.containing(coordinate.getX(), y, coordinate.getZ());
            BlockState blockState = level.getBlockState(blockPos);
            BlockPos abovePos = blockPos.above();
            BlockState aboveState = level.getBlockState(abovePos);

            if (canTransferShockWave(level, abovePos, aboveState)) {
                BlockPos aboveTwoPos = abovePos.above();
                BlockState aboveTwoState = level.getBlockState(aboveTwoPos);
                if (!canTransferShockWave(level, aboveTwoPos, aboveTwoState)) {
                    y++;
                    blockPos = abovePos;
                    blockState = aboveState;
                } else {
                    break;
                }
            }

            if (!canTransferShockWave(level, blockPos, blockState)) {
                BlockPos belowPos = blockPos.below();
                BlockState belowState = level.getBlockState(belowPos);
                if (canTransferShockWave(level, belowPos, belowState)) {
                    y--;
                    blockPos = belowPos;
                    blockState = belowState;
                } else {
                    break;
                }
            }

            Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D);
            Vec3 centerToBlock = blockCenter.subtract(center);
            double distance = centerToBlock.horizontalDistance();
            if (length < distance) continue;
            if (!canTransferShockWave(level, blockPos, blockState) || blockState instanceof FractureBlockState || blockState.getBlock() instanceof EntityBlock) continue;

            Vec3 rotationAxis = IMPACT_DIRECTION.cross(centerToBlock);
            if (rotationAxis.lengthSqr() < 1.0E-8D) rotationAxis = new Vec3(1.0D, 0.0D, 0.0D);
            rotationAxis = rotationAxis.normalize();
            Vector3f axis = new Vector3f((float) rotationAxis.x, (float) rotationAxis.y, (float) rotationAxis.z);
            Vector3f translator = new Vector3f(0.0F, Math.max(0.0F, (float) (distance / length) - 0.5F) * 0.5F, 0.0F);
            Quaternionf rotator = rotationDegrees(axis, (float) (distance / length) * 15.0F + level.random.nextFloat() * 10.0F - 5.0F);
            rotator.mul(rotationDegrees(new Vector3f(1.0F, 0.0F, 0.0F), level.random.nextFloat() * 15.0F - 7.5F));
            rotator.mul(rotationDegrees(new Vector3f(0.0F, 1.0F, 0.0F), level.random.nextFloat() * 40.0F - 20.0F));
            rotator.mul(rotationDegrees(new Vector3f(0.0F, 0.0F, 1.0F), level.random.nextFloat() * 15.0F - 7.5F));
            int lifeTime = 30 + level.random.nextInt(Math.max(1, (int) length * 80));
            double bouncing = distance * distance * bounceExponentCoef;

            FractureBlockState fractureState = FractureBlock.getDefaultFractureBlockState(null);
            if (fractureState == null) return;
            fractureState.setFractureInfo(blockPos, blockState, translator, rotator, bouncing, lifeTime);
            level.setBlock(blockPos, fractureState, 0);
            if (!noParticle && blockState.shouldSpawnParticlesOnBreak()) createParticle(level, blockPos, blockState);
        }
    }

    private static void createParticle(ClientLevel level, BlockPos blockPos, BlockState blockState) {
        Minecraft minecraft = Minecraft.getInstance();
        int count = 1 + level.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double x = blockPos.getX() + level.random.nextDouble();
            double z = blockPos.getZ() + level.random.nextDouble();
            Particle particle = new TerrainParticle(level, x, blockPos.getY() + 1.0D, z, 0.0D, 0.0D, 0.0D, blockState, blockPos);
            particle.setParticleSpeed((level.random.nextDouble() - 0.5D) * 0.3D, level.random.nextDouble() * 0.5D, (level.random.nextDouble() - 0.5D) * 0.3D);
            particle.setLifetime(10 + level.random.nextInt(60));
            minecraft.particleEngine.add(particle);
        }
    }

    private static boolean canTransferShockWave(ClientLevel level, BlockPos blockPos, BlockState blockState) {
        return Block.isFaceFull(blockState.getCollisionShape(level, blockPos, CollisionContext.empty()), Direction.DOWN) || blockState instanceof FractureBlockState;
    }

    private static Vec3 snapSlamCenter(Vec3 center) {
        Vec3 closestEdge = new Vec3(Math.round(center.x), Math.floor(center.y), Math.round(center.z));
        Vec3 centerOfBlock = new Vec3(Math.floor(center.x) + 0.5D, Math.floor(center.y), Math.floor(center.z) + 0.5D);
        return closestEdge.distanceToSqr(center) < centerOfBlock.distanceToSqr(center) ? closestEdge : centerOfBlock;
    }

    private static Quaternionf rotationDegrees(Vector3f axis, float degrees) {
        return new Quaternionf().setAngleAxis(degrees * (float) Math.PI / 180.0F, axis.x(), axis.y(), axis.z());
    }

    private static boolean isBlockOverlapLine(int x, int z, Vec3 from, Vec3 to) {
        return isLinesCross(x, z, x + 1, z, from.x, from.z, to.x, to.z)
                || isLinesCross(x, z, x, z + 1, from.x, from.z, to.x, to.z)
                || isLinesCross(x + 1, z, x + 1, z + 1, from.x, from.z, to.x, to.z)
                || isLinesCross(x, z + 1, x + 1, z + 1, from.x, from.z, to.x, to.z);
    }

    private static boolean isLinesCross(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double denominator = (x2 - x1) * (y4 - y3) - (x4 - x3) * (y2 - y1);
        if (Math.abs(denominator) < 1.0E-8D) return false;
        double u = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
        double v = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator;
        return 0.0D < u && u < 1.0D && 0.0D < v && v < 1.0D;
    }
}
