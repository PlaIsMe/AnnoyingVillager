package com.pla.annoyingvillagers.entity.goal;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.entity.ai.RecoveryAi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Clear only collision shapes intersecting the body after a bad pearl landing or block placement. */
public final class EscapeWallGoal extends AbstractRecoveryGoal {
    private BlockPos block;
    public EscapeWallGoal(AVNpc npc) { super(npc); }

    @Override public boolean canUse() {
        if (!canCheck()) return false;
        block = findBodyBlock();
        return block != null;
    }

    private BlockPos findBodyBlock() {
        AABB body = npc.getBoundingBox().deflate(.02D);
        int checked = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY - 1, body.minZ),
                BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
            if (++checked > 20) break;
            if (RecoveryAi.intersectsBody(npc, pos) && RecoveryAi.canBreak(npc, pos)) return pos.immutable();
        }
        return null;
    }

    @Override public void tick() {
        if (!canContinueToUse()) return;
        elapsed++;
        if (block == null || !RecoveryAi.intersectsBody(npc, block)) {
            recovery.stopBreaking();
            if (npc.tickCount >= nextCheck) {
                nextCheck = npc.tickCount + 20;
                block = findBodyBlock();
                if (block == null) finished = true;
            }
            return;
        }
        npc.getNavigation().stop();
        if (recovery.breakBlock(block, true)) block = null;
    }
}
