package com.pla.annoyingvillagers.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Persisted online simulation time, sampled once per second by the owning manager/event. */
public final class RemoteNpcDeparture {
    private static final String KEY = "AvRemoteSession";
    private RemoteNpcDeparture() {}

    public static boolean tick(Mob npc, boolean ownsTicket, boolean enabled, int configuredMin, int configuredMax) {
        if (!enabled || !ownsTicket || !(npc.level() instanceof ServerLevel level)) {
            // Ticket restoration/allocation can lag behind entity loading on restart.
            // Ineligibility pauses the saved session; it must not erase elapsed time.
            return false;
        }
        CompoundTag root = npc.getPersistentData();
        CompoundTag state = root.getCompound(KEY);
        int min = Math.min(configuredMin, configuredMax);
        int max = Math.max(configuredMin, configuredMax);
        if (state.getInt("Limit") <= 0 || state.getInt("Min") != min || state.getInt("Max") != max) {
            state.putInt("Limit", min * 1200 + npc.getRandom().nextInt((max - min) * 1200 + 1));
            state.putInt("Min", min);
            state.putInt("Max", max);
            state.putInt("Elapsed", 0);
        }
        int elapsed = ExternalChunkActivity.isExternallyLoaded(level, npc.chunkPosition())
                ? 0 : state.getInt("Elapsed") + 20;
        boolean expired = elapsed >= state.getInt("Limit");
        // Cached observation never authorizes discard: recheck current tickets at commit.
        if (expired && ExternalChunkActivity.isExternallyLoadedNow(level, npc.chunkPosition())) {
            elapsed = 0;
            expired = false;
        }
        state.putInt("Elapsed", elapsed);
        root.put(KEY, state);
        return expired;
    }

    public static void copy(Mob from, Mob to) {
        if (from.getPersistentData().contains(KEY)) {
            to.getPersistentData().put(KEY, from.getPersistentData().getCompound(KEY).copy());
        }
    }
}
