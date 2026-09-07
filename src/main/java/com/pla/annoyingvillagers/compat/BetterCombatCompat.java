package com.pla.annoyingvillagers.compat;

import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;

public final class BetterCombatCompat {
    private BetterCombatCompat() {
    }

    public static float getAttackCooldownTicks(Player player) {
        return PlayerAttackHelper.getAttackCooldownTicksCapped(player);
    }
}
