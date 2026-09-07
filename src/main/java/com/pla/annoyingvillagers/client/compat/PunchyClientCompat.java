package com.pla.annoyingvillagers.client.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.network.ClientboundBetterCombatAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.config.PunchyConfig;

import java.io.IOException;
import java.io.Reader;

/** Loaded only when Punchy is installed; ability packets bypass its ordinary BC attack events. */
@OnlyIn(Dist.CLIENT)
public final class PunchyClientCompat {
    private static final ResourceLocation ABILITY_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "punchy/annoyingvillagers/bettercombat_abilities.json");
    private static PendingAbility pending;
    private static boolean reportedFailure;

    private PunchyClientCompat() {}

    public static void queueAbility(LocalPlayer player, ClientboundBetterCombatAnimation message) {
        pending = new PendingAbility(player, message);
    }

    public static void flushAbility() {
        PendingAbility ability = pending;
        pending = null;
        Minecraft mc = Minecraft.getInstance();
        if (ability == null || mc.player != ability.player || mc.level == null
                || !mc.options.getCameraType().isFirstPerson()) return;

        try {
            if (!PunchyConfig.isModEnabled() || !PunchyConfig.isBetterCombatCompatEnabled()) return;
            ClientboundBetterCombatAnimation message = ability.message;
            InteractionHand hand = message.animatedHand() == ClientboundBetterCombatAnimation.AnimatedHand.OFF_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            boolean bothHands = message.animatedHand() == ClientboundBetterCombatAnimation.AnimatedHand.TWO_HANDED;
            if (PunchyConfig.isHandBlacklisted(mc.player, hand)
                    || (bothHands && PunchyConfig.isHandBlacklisted(mc.player, InteractionHand.OFF_HAND))) return;

            // Resolve through the resource manager on each ability so F3+T and pack overrides work.
            var resource = mc.getResourceManager().getResource(ABILITY_ANIMATIONS);
            if (resource.isEmpty()) return;
            String clipName;
            try (Reader reader = resource.get().openAsReader()) {
                JsonObject mappings = JsonParser.parseReader(reader).getAsJsonObject();
                if (!mappings.has(message.animation())) return;
                clipName = mappings.get(message.animation()).getAsString();
            }
            AnimationClip clip = PunchyAnimationManager.resolveNamedClip(clipName, mc, hand);
            if (clip == null) return;

            HumanoidArm arm = hand == InteractionHand.OFF_HAND
                    ? mc.player.getMainArm().getOpposite() : mc.player.getMainArm();
            PunchyAnimationManager.setSourceHand(mc, hand);
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(bothHands ? null : arm);
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.play(clip,
                    PunchyAnimationManager.clipSpeedMultiplier(clip), false, false);
        } catch (IOException | RuntimeException | LinkageError error) {
            if (!reportedFailure) {
                reportedFailure = true;
                AnnoyingVillagers.LOGGER.warn("Could not play Punchy weapon ability; keeping Better Combat playback", error);
            }
        }
    }

    private record PendingAbility(LocalPlayer player, ClientboundBetterCombatAnimation message) {}
}
