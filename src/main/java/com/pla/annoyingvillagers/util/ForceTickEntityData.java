package com.pla.annoyingvillagers.util;

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

import java.util.*;

/** Restoration coordinates only. Runtime tickets never enter the save. */
public final class ForceTickEntityData extends SavedData {
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public static ForceTickEntityData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ForceTickEntityData::load, ForceTickEntityData::new, "annoyingvillagers_force_tick_entities");
    }

    public static ForceTickEntityData load(CompoundTag tag) {
        ForceTickEntityData data = new ForceTickEntityData();
        ListTag list = tag.getList("Entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saved = list.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(saved.getString("Dimension"));
            if (!saved.hasUUID("Id") || dimension == null) continue;
            UUID id = saved.getUUID("Id");
            data.entries.put(id, new Entry(id, ResourceKey.create(Registries.DIMENSION, dimension),
                    new ChunkPos(saved.getInt("ChunkX"), saved.getInt("ChunkZ"))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("Id", entry.id());
            saved.putString("Dimension", entry.dimension().location().toString());
            saved.putInt("ChunkX", entry.center().x);
            saved.putInt("ChunkZ", entry.center().z);
            list.add(saved);
        }
        tag.put("Entities", list);
        return tag;
    }

    public List<Entry> entries() { return new ArrayList<>(entries.values()); }

    public void put(UUID id, ResourceKey<Level> dimension, ChunkPos center) {
        Entry next = new Entry(id, dimension, center);
        if (!next.equals(entries.get(id))) {
            entries.put(id, next);
            setDirty();
        }
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) setDirty();
    }

    public record Entry(UUID id, ResourceKey<Level> dimension, ChunkPos center) {}
}
