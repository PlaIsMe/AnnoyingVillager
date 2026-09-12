package com.pla.annoyingvillagers.client.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Non-damaging hand actions for both humanoid and villager rigs. No root motion or colliders. */
public final class RecoveryAnimations {
    public static final AnimationDefinition DIG_MAINHAND = AnimationDefinition.Builder.withLength(.5F)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    rotation(0F, -35F, -10F, 0F), rotation(.15F, -115F, -15F, 8F),
                    rotation(.30F, -30F, 12F, -6F), rotation(.5F, -35F, -10F, 0F)))
            .addAnimation("right_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    rotation(0F, -15F, 0F, 0F), rotation(.15F, -60F, 0F, 0F),
                    rotation(.30F, 5F, 0F, 0F), rotation(.5F, -15F, 0F, 0F))).build();
    public static final AnimationDefinition USE_MAINHAND = AnimationDefinition.Builder.withLength(.4F)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    rotation(0F, -20F, 0F, 0F), rotation(.15F, -75F, -10F, 0F),
                    rotation(.4F, 0F, 0F, 0F))).build();

    private static Keyframe rotation(float time, float x, float y, float z) {
        return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.LINEAR);
    }

    private RecoveryAnimations() { }
}
