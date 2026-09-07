package com.pla.annoyingvillagers.network;

import java.util.Objects;
import java.util.function.Supplier;

import com.pla.annoyingvillagers.event.SpecialAttackOnKeyHeldEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent.Context;
import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.event.SpecialAttackOnKeyPressedEvent;

@EventBusSubscriber(bus = Bus.MOD)
public class SpecialAttackMessage {
    int type;
    int presses;
    boolean hasCrosshairTarget;
    double crosshairTargetX;
    double crosshairTargetY;
    double crosshairTargetZ;

    public SpecialAttackMessage(int type, int presses) {
        this(type, presses, null);
    }

    public SpecialAttackMessage(int type, int presses, Vec3 crosshairTarget) {
        this.type = type;
        this.presses = presses;
        this.hasCrosshairTarget = crosshairTarget != null;
        if (crosshairTarget != null) {
            this.crosshairTargetX = crosshairTarget.x;
            this.crosshairTargetY = crosshairTarget.y;
            this.crosshairTargetZ = crosshairTarget.z;
        }
    }

    public SpecialAttackMessage(FriendlyByteBuf friendlybytebuf) {
        this.type = friendlybytebuf.readInt();
        this.presses = friendlybytebuf.readInt();
        this.hasCrosshairTarget = friendlybytebuf.readBoolean();
        if (this.hasCrosshairTarget) {
            this.crosshairTargetX = friendlybytebuf.readDouble();
            this.crosshairTargetY = friendlybytebuf.readDouble();
            this.crosshairTargetZ = friendlybytebuf.readDouble();
        }
    }

    public static void buffer(SpecialAttackMessage specialAttackMessage, FriendlyByteBuf friendlybytebuf) {
        friendlybytebuf.writeInt(specialAttackMessage.type);
        friendlybytebuf.writeInt(specialAttackMessage.presses);
        friendlybytebuf.writeBoolean(specialAttackMessage.hasCrosshairTarget);
        if (specialAttackMessage.hasCrosshairTarget) {
            friendlybytebuf.writeDouble(specialAttackMessage.crosshairTargetX);
            friendlybytebuf.writeDouble(specialAttackMessage.crosshairTargetY);
            friendlybytebuf.writeDouble(specialAttackMessage.crosshairTargetZ);
        }
    }

    public static void handler(SpecialAttackMessage specialAttackMessage, Supplier<Context> supplier) {
        Context context = supplier.get();

        context.enqueueWork(() -> {
            pressAction(Objects.requireNonNull(context.getSender()), specialAttackMessage.type, specialAttackMessage.presses, specialAttackMessage.getCrosshairTarget());
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player, int type, int presses) {
        pressAction(player, type, presses, null);
    }

    public static void pressAction(Player player, int type, int presses, Vec3 crosshairTarget) {
        Level level = player.level();
        if (type == 0) {
            SpecialAttackOnKeyPressedEvent.execute(level, player, crosshairTarget);
        } else if (type == 1) {
            SpecialAttackOnKeyHeldEvent.execute(level, player, crosshairTarget);
        }
    }

    private Vec3 getCrosshairTarget() {
        return this.hasCrosshairTarget ? new Vec3(this.crosshairTargetX, this.crosshairTargetY, this.crosshairTargetZ) : null;
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent fmlcommonsetupevent) {
        AnnoyingVillagers.addNetworkMessage(SpecialAttackMessage.class, SpecialAttackMessage::buffer, SpecialAttackMessage::new, SpecialAttackMessage::handler);
    }
}
