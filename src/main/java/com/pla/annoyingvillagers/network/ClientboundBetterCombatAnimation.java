package com.pla.annoyingvillagers.network;

import com.pla.annoyingvillagers.client.compat.BetterCombatClientCompat;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundBetterCombatAnimation(
        int playerId,
        AnimatedHand animatedHand,
        String animation,
        float swingDurationTicks,
        float upswing
) {
    public enum AnimatedHand {
        MAIN_HAND,
        OFF_HAND,
        TWO_HANDED
    }

    public static void encode(ClientboundBetterCombatAnimation msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.playerId);
        buf.writeEnum(msg.animatedHand);
        buf.writeUtf(msg.animation);
        buf.writeFloat(msg.swingDurationTicks);
        buf.writeFloat(msg.upswing);
    }

    public static ClientboundBetterCombatAnimation decode(FriendlyByteBuf buf) {
        return new ClientboundBetterCombatAnimation(
                buf.readVarInt(),
                buf.readEnum(AnimatedHand.class),
                buf.readUtf(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(ClientboundBetterCombatAnimation msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (ModList.get().isLoaded(VanillaWeaponAbilityUtil.BETTER_COMBAT_MOD_ID)) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BetterCombatClientCompat.playAnimation(msg));
            }
        });
        context.setPacketHandled(true);
    }
}
