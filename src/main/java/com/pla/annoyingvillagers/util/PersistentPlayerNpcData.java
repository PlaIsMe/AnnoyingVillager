package com.pla.annoyingvillagers.util;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PersistentPlayerNpcData extends SavedData {
    private static final String DATA_NAME = AnnoyingVillagers.MODID + "_persistent_player_npcs";
    private static final String NPCS_TAG = "Npcs";
    private static final String ID_TAG = "Id";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String CHUNK_X_TAG = "ChunkX";
    private static final String CHUNK_Z_TAG = "ChunkZ";
    private static final String IDENTITY_TAG = "Identity";

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public static PersistentPlayerNpcData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PersistentPlayerNpcData::load,
                PersistentPlayerNpcData::new,
                DATA_NAME
        );
    }

    public static PersistentPlayerNpcData load(CompoundTag tag) {
        PersistentPlayerNpcData data = new PersistentPlayerNpcData();
        ListTag npcs = tag.getList(NPCS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < npcs.size(); i++) {
            CompoundTag npcTag = npcs.getCompound(i);
            if (!npcTag.hasUUID(ID_TAG) || !npcTag.contains(DIMENSION_TAG, Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation dimensionId = ResourceLocation.tryParse(npcTag.getString(DIMENSION_TAG));
            if (dimensionId == null) {
                continue;
            }

            UUID npcId = npcTag.getUUID(ID_TAG);
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            ChunkPos centerChunk = new ChunkPos(npcTag.getInt(CHUNK_X_TAG), npcTag.getInt(CHUNK_Z_TAG));
            String identity = npcTag.contains(IDENTITY_TAG, Tag.TAG_STRING)
                    ? npcTag.getString(IDENTITY_TAG)
                    : "";
            data.entries.put(npcId, new Entry(npcId, levelKey, centerChunk, identity));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag npcs = new ListTag();
        for (Entry entry : this.entries.values()) {
            CompoundTag npcTag = new CompoundTag();
            npcTag.putUUID(ID_TAG, entry.npcId());
            npcTag.putString(DIMENSION_TAG, entry.levelKey().location().toString());
            npcTag.putInt(CHUNK_X_TAG, entry.centerChunk().x);
            npcTag.putInt(CHUNK_Z_TAG, entry.centerChunk().z);
            if (!entry.identity().isBlank()) {
                npcTag.putString(IDENTITY_TAG, entry.identity());
            }
            npcs.add(npcTag);
        }
        tag.put(NPCS_TAG, npcs);
        return tag;
    }

    public List<Entry> entries() {
        return new ArrayList<>(this.entries.values());
    }

    public void put(UUID npcId, ResourceKey<Level> levelKey, ChunkPos centerChunk, String identity) {
        Entry nextEntry = new Entry(npcId, levelKey, centerChunk, Objects.requireNonNullElse(identity, ""));
        if (Objects.equals(this.entries.get(npcId), nextEntry)) {
            return;
        }

        this.entries.put(npcId, nextEntry);
        this.setDirty();
    }

    public boolean contains(UUID npcId) { return this.entries.containsKey(npcId); }

    public boolean containsIdentity(String identity) {
        return this.entries.values().stream().anyMatch(entry -> entry.identity().equals(identity));
    }

    public void remove(UUID npcId) {
        if (this.entries.remove(npcId) != null) {
            this.setDirty();
        }
    }

    public record Entry(UUID npcId, ResourceKey<Level> levelKey, ChunkPos centerChunk, String identity) {
    }
}
