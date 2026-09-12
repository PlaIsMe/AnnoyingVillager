package com.pla.annoyingvillagers.mixin.client;

import com.pla.annoyingvillagers.util.PersistentPlayerNpcManager;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
    @Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
    private void av$hideNpcTabProfilesFromCommandSuggestions(CallbackInfoReturnable<Collection<String>> cir) {
        Collection<String> names = cir.getReturnValue();
        if (names == null || names.isEmpty()) {
            return;
        }

        ArrayList<String> filtered = new ArrayList<>(names.size());
        boolean changed = false;
        for (String name : names) {
            if (PersistentPlayerNpcManager.isTabProfileName(name)) {
                changed = true;
                continue;
            }
            filtered.add(name);
        }

        if (changed) {
            cir.setReturnValue(filtered);
        }
    }
}
