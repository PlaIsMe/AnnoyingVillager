package com.pla.annoyingvillagers.mixin.compat.punchy;

import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "punchy.client.state.TridentStateMachine", remap = false)
public abstract class TridentStateMachineMixin {
    // Punchy 2.6.2's first boolean local is its vanilla TridentItem type check.
    // Reuse its charge/hold/release state machine, which follows getUsedItemHand().
    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0, require = 1, allow = 1)
    private boolean av$recognizeBlueDemonTrident(boolean isTrident, Minecraft client) {
        return isTrident || (client.player != null
                && BlueDemonTridentItem.isBlueDemonTrident(client.player.getUseItem()));
    }
}
