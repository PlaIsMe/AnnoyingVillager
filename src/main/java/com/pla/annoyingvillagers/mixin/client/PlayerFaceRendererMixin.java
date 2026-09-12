package com.pla.annoyingvillagers.mixin.client;

import com.pla.annoyingvillagers.util.NpcTabSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerFaceRenderer.class)
public abstract class PlayerFaceRendererMixin {
    @Shadow
    private static void drawHat(GuiGraphics graphics, ResourceLocation texture,
                                int x, int y, int size, boolean upsideDown) {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(method = "draw(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIZZ)V",
            at = @At("TAIL"))
    private static void av$drawMissingNpcHeadOverlay(GuiGraphics graphics, ResourceLocation texture,
                                                    int x, int y, int size, boolean drawHat,
                                                    boolean upsideDown, CallbackInfo callback) {
        // Packet-only NPCs have no Player HAT flag. Add their overlay only when
        // vanilla did not draw it, preserving normal player faces and render state.
        if (!drawHat && NpcTabSkin.isNpcTexture(texture)) {
            drawHat(graphics, texture, x, y, size, upsideDown);
        }
    }
}
