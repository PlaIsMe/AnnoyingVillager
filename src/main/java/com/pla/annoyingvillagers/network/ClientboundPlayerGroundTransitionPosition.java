package com.pla.annoyingvillagers.network;

import com.pla.annoyingvillagers.client.engine.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundPlayerGroundTransitionPosition(double x, double y, double z) {
    public static void encode(ClientboundPlayerGroundTransitionPosition msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static ClientboundPlayerGroundTransitionPosition decode(FriendlyByteBuf buf) {
        return new ClientboundPlayerGroundTransitionPosition(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(ClientboundPlayerGroundTransitionPosition msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handlePlayerGroundTransitionPosition(msg)));
        context.setPacketHandled(true);
    }
}
