package com.pla.annoyingvillagers.mixin.compat.punchy;

import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = punchy.client.state.UseItemStateMachine.class, remap = false)
public abstract class UseItemStateMachineMixin {
    @Inject(method = "isChargeItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void av$reserveTridentCharge(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Generic right-click poses must not replace the trident aiming pose.
        if (stack != null && BlueDemonTridentItem.isBlueDemonTrident(stack)) cir.setReturnValue(true);
    }
}
