package com.pla.annoyingvillagers.mixin.client;

import com.pla.annoyingvillagers.item.BlueDemonTridentItem;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererBlueDemonPoseMixin {
    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void annoyingVillagers$aimOffhandBlueDemonTrident(AbstractClientPlayer player, CallbackInfo ci) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || !player.isUsingItem() || !player.isShiftKeyDown()) return;
        if (!BlueDemonTridentItem.isBlueDemonTrident(player.getMainHandItem()) || !BlueDemonTridentItem.isBlueDemonTrident(player.getOffhandItem())) return;
        PlayerRenderer renderer = (PlayerRenderer)(Object)this;
        renderer.getModel().rightArmPose = HumanoidModel.ArmPose.ITEM;
        renderer.getModel().leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
    }
}
