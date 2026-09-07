package com.pla.annoyingvillagers.mixin.client;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import com.pla.annoyingvillagers.item.DemoniacVoltageReaverItem;
import com.pla.annoyingvillagers.item.EnderSlayerScytheItem;
import com.pla.annoyingvillagers.network.VanillaAttackKeyMessage;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    private static boolean annoyingVillagers$blueDemonOffhandNext;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void annoyingVillagers$handleVanillaAbilityAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = (Minecraft)(Object)this;
        LocalPlayer player = minecraft.player;
        if (player == null || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;

        if (player.getMainHandItem().getItem() instanceof EnderSlayerScytheItem && EnderSlayerScytheItem.isDragonActive(player.getMainHandItem())) {
            AnnoyingVillagers.PACKET_HANDLER.sendToServer(new VanillaAttackKeyMessage());
            VanillaWeaponAbilityUtil.swingOffHand(player);
            cir.setReturnValue(false);
            return;
        }

        if (player.getMainHandItem().getItem() instanceof DemoniacVoltageReaverItem && DemoniacVoltageReaverItem.isVanillaAwakened(player.getMainHandItem(), player.level())) {
            AnnoyingVillagers.PACKET_HANDLER.sendToServer(new VanillaAttackKeyMessage());
            VanillaWeaponAbilityUtil.swingMainHand(player);
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void annoyingVillagers$alternateBlueDemonTridentSwing(LocalPlayer player, InteractionHand originalHand) {
        if (VanillaWeaponAbilityUtil.abilitiesEnabled() && BlueDemonTridentItem.isBlueDemonTrident(player.getMainHandItem()) && BlueDemonTridentItem.isBlueDemonTrident(player.getOffhandItem())) {
            InteractionHand hand = annoyingVillagers$blueDemonOffhandNext ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            annoyingVillagers$blueDemonOffhandNext = !annoyingVillagers$blueDemonOffhandNext;
            player.swing(hand);
            return;
        }
        annoyingVillagers$blueDemonOffhandNext = false;
        player.swing(originalHand);
    }
}
