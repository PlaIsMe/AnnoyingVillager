package com.pla.annoyingvillagers.entity.goal;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.entity.ai.RecoveryAi;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

/** One owner for controls, the real temporary hand, and one counted rig attack lock. */
abstract class AbstractRecoveryGoal extends Goal {
    protected final AVNpc npc;
    protected final RecoveryAi recovery;
    protected boolean finished;
    protected int elapsed;
    protected int nextCheck;
    private boolean ownsLock;

    AbstractRecoveryGoal(AVNpc npc) {
        this.npc = npc;
        this.recovery = new RecoveryAi(npc);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    protected boolean canCheck() {
        if (npc.tickCount < nextCheck) return false;
        if (!RecoveryAi.canStart(npc)) return trace("admission_blocked");
        nextCheck = npc.tickCount + 20 + npc.getRandom().nextInt(11);
        trace("checking");
        return true;
    }

    protected boolean trace(String reason) {
        com.pla.annoyingvillagers.util.RecoveryTrace.note(npc, getClass().getSimpleName(), reason);
        return false;
    }

    @Override public void start() {
        elapsed = 0;
        ownsLock = npc.beginRecoveryAction(this);
        finished = !ownsLock;
        trace(ownsLock ? "started" : "start_lock_refused");
        if (ownsLock) RecoveryAi.startRecovery(npc);
        npc.getNavigation().stop();
    }

    @Override public boolean canContinueToUse() {
        return ownsLock && !finished && elapsed < 800 && RecoveryAi.canAct(npc);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void stop() {
        trace("stopped finished=" + finished + " elapsed=" + elapsed + " canAct=" + RecoveryAi.canAct(npc));
        recovery.stopBreaking();
        recovery.restoreHand();
        npc.getNavigation().stop();
        if (ownsLock) npc.endRecoveryAction(this);
        ownsLock = false;
        nextCheck = npc.tickCount + 20 + npc.getRandom().nextInt(11);
    }
}
