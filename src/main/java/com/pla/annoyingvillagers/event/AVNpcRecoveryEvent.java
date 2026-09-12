package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.clazz.AVNpc;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/** Optional combat/renderer integration without a dependency on Epic Fight in the core mod. */
@Cancelable
public final class AVNpcRecoveryEvent extends EntityEvent {
    // START is a notification after ownership is acquired; veto admission at CHECK_START.
    public enum Action { CHECK_START, DIG, STOP_DIG, USE, START }

    private final Action action;

    public AVNpcRecoveryEvent(AVNpc npc, Action action) {
        super(npc);
        this.action = action;
    }

    public AVNpc getNpc() { return (AVNpc) getEntity(); }
    public Action getAction() { return action; }
}
