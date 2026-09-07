package com.pla.annoyingvillagers.mixin;

import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldowns.class)
public class ItemCooldownsMixin {
    @Inject(method = "isOnCooldown", at = @At("HEAD"), cancellable = true)
    private void ignoreCooldownForVanillaAbilityItems(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        if (item == AnnoyingVillagersModItems.ENDER_AEGIS.get() || item == AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get() || item == AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get() || item == AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get()) cir.setReturnValue(false);
    }
}