package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.item.NullWeaponItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NullWeaponCleanupEvent {
    private static final String NBT_SPENT_STACKS = "AV_NullWeaponSpentStacks";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = player.getPersistentData();
        data.remove(NBT_SPENT_STACKS);
        NullWeaponItem.beginLoginReconciliation(player);
    }
}
