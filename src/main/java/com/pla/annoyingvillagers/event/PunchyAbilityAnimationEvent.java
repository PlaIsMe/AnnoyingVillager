package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.client.compat.PunchyClientCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, value = Dist.CLIENT)
public final class PunchyAbilityAnimationEvent {
    private PunchyAbilityAnimationEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Follow Punchy's input processing so generic use/attack poses do not replace the ability.
        if (event.phase == TickEvent.Phase.END && ModList.get().isLoaded("bettercombat")
                && ModList.get().isLoaded("punchy")) PunchyClientCompat.flushAbility();
    }
}
