package com.pla.annoyingvillagers.spawnhandler;

import com.pla.annoyingvillagers.entity.ChrisEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChrisData extends SavedData {
    public static final String ID = "av_singleton_chris";
    private UUID activeId = null;
    private boolean confirmed;
    private long claimTick = 0L;
    private static final long COOLDOWN_TICKS = 20L * 60L * 10L;

    public static ChrisData get(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(ChrisData::load, ChrisData::new, ID);
    }

    public static ChrisData load(CompoundTag compoundTag) {
        ChrisData chrisData = new ChrisData();
        if (compoundTag.hasUUID("activeId")){
            chrisData.activeId = compoundTag.getUUID("activeId");
        }
        if (compoundTag.contains("claimTick", Tag.TAG_LONG)) {
            chrisData.claimTick = compoundTag.getLong("claimTick");
        }
        // Legacy records describe an existing singleton but contain no chunk coordinates.
        chrisData.confirmed = compoundTag.contains("confirmed")
                ? compoundTag.getBoolean("confirmed") : chrisData.activeId != null;
        return chrisData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        if (activeId != null){
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
                .containsIdentity("Chris")) return true;
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
            if (entity instanceof ChrisEntity && entity.isAlive()) {
                return true;
            }
            activeId = null;
            setDirty();
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

    public void forceClaim(ServerLevel level, UUID id) {
        if (id.equals(activeId) && confirmed) return;
        confirmed = true;
        activeId = id;
        claimTick = now(level);
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
