package com.pla.annoyingvillagers.spawnhandler;

import com.pla.annoyingvillagers.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SteveData extends SavedData {
    public static final String ID = "av_singleton_steve";
    private UUID activeId = null;
    private boolean confirmed;
    private long claimTick = 0L;
    private static final long COOLDOWN_TICKS = 20L * 60L * 10L;

    public static SteveData get(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(SteveData::load, SteveData::new, ID);
    }

    public static SteveData load(CompoundTag compoundTag) {
        SteveData bluedemonData = new SteveData();
        if (compoundTag.hasUUID("activeId")) {
            bluedemonData.activeId = compoundTag.getUUID("activeId");
        }
        if (compoundTag.contains("claimTick", Tag.TAG_LONG)) {
            bluedemonData.claimTick = compoundTag.getLong("claimTick");
        }
        // Legacy records describe an existing singleton but contain no chunk coordinates.
        bluedemonData.confirmed = compoundTag.contains("confirmed")
                ? compoundTag.getBoolean("confirmed") : bluedemonData.activeId != null;
        return bluedemonData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        if (activeId != null) {
            compoundTag.putUUID("activeId", activeId);
        }
        compoundTag.putLong("claimTick", claimTick);
        compoundTag.putBoolean("confirmed", confirmed);
        return compoundTag;
    }

    private static long now(ServerLevel level) {
        return level.getGameTime();
    }

    public boolean isOccupied(ServerLevel serverLevel) {
        if (com.pla.annoyingvillagers.util.PersistentPlayerNpcData.get(serverLevel.getServer())
                .containsIdentity("Steve")) return true;
        if (activeId != null) {
            Entity entity = serverLevel.getEntity(activeId);
            // Absence from the loaded entity map does not prove death. The session manager
            // restores registered entities and explicitly vacates genuinely stale entries.
            if (entity == null) {
                if (confirmed || now(serverLevel) - claimTick < 600L) return true;
                // A reserved natural spawn never joined (for example another mod vetoed it).
                activeId = null;
                claimTick = 0L;
                setDirty();
                return false;
            }
            if ((entity instanceof SteveEntity || entity instanceof AngrySteveEntity) && entity.isAlive()) {
                return true;
            } else {
                activeId = null;
                setDirty();
                return false;
            }
        }
        if (claimTick <= 0L) return false;
        long elapsed = now(serverLevel) - claimTick;
        return elapsed < COOLDOWN_TICKS;
    }

    public boolean tryClaim(ServerLevel serverLevel, UUID id) {
        if (isOccupied(serverLevel)) {
            return false;
        }
        activeId = id;
        claimTick = now(serverLevel);
        confirmed = false;
        setDirty();
        return true;
    }

    public void forceClaim(ServerLevel serverLevel, UUID id) {
        if (id.equals(activeId) && confirmed) return;
        confirmed = true;
        activeId = id;
        claimTick = now(serverLevel);
        setDirty();
    }

    public void releaseIfMatches(ServerLevel serverLevel, UUID id) {
        if (activeId != null && activeId.equals(id)) {
            activeId = null;
            claimTick = now(serverLevel);
            setDirty();
        }
    }

    public void vacateIfMatches(UUID id) {
        if (id.equals(activeId)) {
            activeId = null;
            claimTick = 0L;
            setDirty();
        }
    }
}
