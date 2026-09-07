package com.pla.annoyingvillagers.mixin.compat.punchy;

import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "punchy.client.state.SpearStateMachine", remap = false)
public abstract class SpearStateMachineMixin {
    @Inject(method = "isSpearStack", at = @At("HEAD"), cancellable = true, require = 1)
    private static void av$leaveTridentToThrowAnimation(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Blue Demon uses SPEAR like vanilla tridents, but is not a melee charging spear.
        if (stack != null && BlueDemonTridentItem.isBlueDemonTrident(stack)) cir.setReturnValue(false);
    }
}
