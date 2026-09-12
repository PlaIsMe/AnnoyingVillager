package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigAnimationSpec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import javax.annotation.Nullable;

/** Cancel before native playback/colliders when another animation engine owns this action. */
@Cancelable
public final class AVNpcRigAnimationEvent extends EntityEvent {
    private final RigAnimationSpec spec;
    private final LivingEntity target;

    public AVNpcRigAnimationEvent(AVNpc npc, RigAnimationSpec spec, @Nullable LivingEntity target) {
        super(npc);
        this.spec = spec;
        this.target = target;
    }

    public AVNpc getNpc() { return (AVNpc) getEntity(); }
    public RigAnimationSpec getSpec() { return spec; }
    public RigAnimationId getAnimationId() { return spec.animationId(); }
    @Nullable public LivingEntity getTarget() { return target; }
}
