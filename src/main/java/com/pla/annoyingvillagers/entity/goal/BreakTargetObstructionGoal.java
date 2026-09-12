package com.pla.annoyingvillagers.entity.goal;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.entity.ai.RecoveryAi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Opens a body-sized passage to an unreachable enemy, including the sill below an opened window. */
public final class BreakTargetObstructionGoal extends AbstractRecoveryGoal {
    private LivingEntity target;
    private BlockPos block;
    private int ray;
    private int nextDiscovery;
    private int nextApproach;
    private int approachCursor;

    public BreakTargetObstructionGoal(AVNpc npc) { super(npc); }

    @Override public boolean canUse() {
        if (!canCheck()) return false;
        LivingEntity enemy = npc.getTarget();
        if (!RecoveryAi.validTarget(npc, enemy) || enemy.getY() - npc.getY() > 2
                || !RecoveryAi.loadedCorridor(npc, enemy.position()) || !RecoveryAi.admitPath(npc)) return false;
        if (RecoveryAi.reachable(RecoveryAi.targetPath(npc, enemy.blockPosition()))) return false;
        target = enemy;
        block = null;
        return selectBlock();
    }

    @Override public void start() {
        super.start();
        nextDiscovery = npc.tickCount + 20;
        nextApproach = npc.tickCount;
        approachCursor = 0;
    }

    @Override public boolean canContinueToUse() {
        return super.canContinueToUse() && RecoveryAi.validTarget(npc, target)
                && npc.getTarget() == target && target.getY() - npc.getY() <= 2;
    }

    @Override public void tick() {
        if (!canContinueToUse()) return;
        elapsed++;
        if (!RecoveryAi.loadedCorridor(npc, target.position())) { finished = true; return; }
        if (npc.tickCount >= nextDiscovery) {
            nextDiscovery = npc.tickCount + 20;
            if (RecoveryAi.admitPath(npc)) {
                if (RecoveryAi.reachable(RecoveryAi.targetPath(npc, target.blockPosition()))) { finished = true; return; }
                if (!selectBlock()) { finished = true; return; }
            }
        }
        if (block == null) return;
        if (!RecoveryAi.canBreak(npc, block)) { recovery.stopBreaking(); block = null; return; }
        if (!block.equals(rayHit(ray))) { recovery.pauseBreaking(); return; }
        if (!RecoveryAi.inReach(npc, block)) {
            recovery.pauseBreaking();
            if (npc.tickCount >= nextApproach) {
                nextApproach = npc.tickCount + 20;
                approachBlock();
            }
            return;
        }
        npc.getNavigation().stop();
        if (recovery.breakBlock(block, false)) block = null;
    }

    private boolean selectBlock() {
        BlockPos selected = null;
        int selectedRay = 0;
        double best = Double.MAX_VALUE;
        int count = Math.abs(npc.getY() - target.getY()) <= .25D ? 10 : 1;
        for (int i = 0; i < count; i++) {
            BlockPos candidate = rayHit(i);
            if (candidate == null || candidate.getY() < npc.blockPosition().getY()
                    || candidate.equals(npc.blockPosition().below()) || !RecoveryAi.canBreak(npc, candidate)) continue;
            // Keep a valid active block even when another sample becomes marginally closer.
            if (candidate.equals(block) && RecoveryAi.inReach(npc, candidate)) { ray = i; return true; }
            double score = npc.getEyePosition().distanceToSqr(Vec3.atCenterOf(candidate))
                    + (RecoveryAi.inReach(npc, candidate) ? 0 : 1000);
            if (score < best) { best = score; selected = candidate; selectedRay = i; }
        }
        if (!java.util.Objects.equals(block, selected)) recovery.stopBreaking();
        block = selected; ray = selectedRay;
        return block != null;
    }

    private BlockPos rayHit(int index) {
        Vec3 start = npc.getEyePosition();
        Vec3 end = target.getEyePosition();
        if (index > 0) {
            Vec3 forward = target.position().subtract(npc.position()).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(((index - 1) % 3 - 1) * npc.getBbWidth() * .4);
            double height = new double[] { .1D, npc.getBbHeight() * .5D, npc.getBbHeight() - .1D }[(index - 1) / 3];
            start = npc.position().add(side).add(0, height, 0);
            end = new Vec3(target.getX(), npc.getY() + height, target.getZ()).add(side);
        }
        var hit = npc.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    private void approachBlock() {
        int attempts = 0;
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int inspected = 0; inspected < 8; inspected++) {
            int sample = approachCursor++ % 8;
            BlockPos stand = block.relative(sides[sample % 4]).below(sample / 4);
            if (!npc.level().hasChunkAt(stand) || !npc.level().hasChunkAt(stand.above())
                    || !npc.level().hasChunkAt(stand.below())
                    || !npc.level().getBlockState(stand.below()).isFaceSturdy(npc.level(), stand.below(), Direction.UP)
                    || !npc.level().getBlockState(stand).getCollisionShape(npc.level(), stand).isEmpty()
                    || !npc.level().getBlockState(stand.above()).getCollisionShape(npc.level(), stand.above()).isEmpty()) continue;
            if (++attempts > 2 || !RecoveryAi.admitPath(npc)) return;
            var path = RecoveryAi.targetPath(npc, stand);
            if (RecoveryAi.reachable(path)) { npc.getNavigation().moveTo(path, 1.0D); return; }
        }
    }

    @Override public void stop() { super.stop(); block = null; target = null; }
}
