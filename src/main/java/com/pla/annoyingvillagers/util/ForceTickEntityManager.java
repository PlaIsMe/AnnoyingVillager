package com.pla.annoyingvillagers.util;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.ForceTickEntity;
import com.pla.annoyingvillagers.clazz.PersistentPlayerNpc;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** Configurable ticket ownership for ordinary mobs, independent of player-like sessions. */
@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID)
public final class ForceTickEntityManager {
    // Shared with PersistentPlayerNpcManager. Keep the established ticket name so both
    // mods' unattended-player checks recognize all AV-owned NPC tickets as non-attendance.
    static final TicketType<UUID> TICKET = TicketType.create(
            "annoyingvillagers:persistent_player_npc", Comparator.<UUID>naturalOrder());
    private static final Map<UUID, Tracked> TRACKED = new LinkedHashMap<>();
    private static Boolean lastEnabled;

    private ForceTickEntityManager() {}

    private static boolean eligible(Entity entity) {
        // An entity implementing both contracts must never acquire two owners.
        return entity instanceof Mob && entity instanceof ForceTickEntity
                && !(entity instanceof PersistentPlayerNpc);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !eligible(event.getEntity()) || event.getEntity().isRemoved()
                || !(event.getLevel() instanceof ServerLevel level)) return;
        track(level.getServer(), event.getEntity());
    }

    private static void track(MinecraftServer server, Entity entity) {
        Tracked tracked = TRACKED.computeIfAbsent(entity.getUUID(), id ->
                new Tracked(id, entity.level().dimension(), entity.chunkPosition()));
        tracked.update(server, entity);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!eligible(event.getEntity()) || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        Tracked tracked = TRACKED.get(entity.getUUID());
        if (tracked == null) return; // Includes entity unloads after server-stop cleanup.
        tracked.release(level.getServer());
        if (entity.getRemovalReason() == Entity.RemovalReason.KILLED
                || entity.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            forget(level.getServer(), tracked);
        } else {
            tracked.dimension = level.dimension();
            tracked.center = entity.chunkPosition();
            tracked.unresolvedTicks = 0;
            ForceTickEntityData.get(level.getServer()).put(tracked.id, tracked.dimension, tracked.center);
        }
    }

    @SubscribeEvent
    public static void allowDespawn(MobSpawnEvent.AllowDespawn event) {
        if (AnnoyingVillagersConfig.FORCE_TICK_MOBS.get() && eligible(event.getEntity())) {
            // Do not persist PersistenceRequired: disabling the feature must restore normal
            // distance-despawn rules. Explicit death/recall/discard remains entity-owned.
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        for (var entry : ForceTickEntityData.get(server).entries()) {
            TRACKED.putIfAbsent(entry.id(), new Tracked(entry.id(), entry.dimension(), entry.center()));
        }
        reconcile(server);
        boolean enabled = AnnoyingVillagersConfig.FORCE_TICK_MOBS.get();
        if (enabled) for (Tracked tracked : new ArrayList<>(TRACKED.values())) tracked.ensureTicket(server);
        lastEnabled = enabled;
    }

    private static void reconcile(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) {
            if (eligible(entity) && !entity.isRemoved()) track(server, entity);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        boolean enabled = AnnoyingVillagersConfig.FORCE_TICK_MOBS.get();
        if (!Objects.equals(lastEnabled, enabled)) {
            if (enabled) reconcile(server);
            else for (Tracked tracked : TRACKED.values()) tracked.release(server);
            lastEnabled = enabled;
        }
        for (Tracked tracked : new ArrayList<>(TRACKED.values())) {
            if (Math.floorMod(server.getTickCount(), 20) != Math.floorMod(tracked.id.hashCode(), 20)) continue;
            ServerLevel level = server.getLevel(tracked.dimension);
            if (level == null) { forget(server, tracked); continue; }
            Entity entity = level.getEntity(tracked.id);
            if (entity != null && !entity.isRemoved()) {
                if (!eligible(entity)) { forget(server, tracked); continue; }
                // Dying mobs retain ticking until their normal death animation/removal finishes.
                tracked.update(server, entity);
            } else if (enabled) {
                tracked.ensureTicket(server);
                if (level.hasChunk(tracked.center.x, tracked.center.z)) tracked.unresolvedTicks += 20;
                if (tracked.unresolvedTicks >= 600) forget(server, tracked);
            } else {
                tracked.unresolvedTicks = 0; // A disabled/unloaded mob is not a stale entry.
            }
        }
    }

    private static void forget(MinecraftServer server, Tracked tracked) {
        tracked.release(server);
        TRACKED.remove(tracked.id);
        ForceTickEntityData.get(server).remove(tracked.id);
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (Tracked tracked : TRACKED.values()) {
            ServerLevel level = server.getLevel(tracked.dimension);
            Entity entity = level == null ? null : level.getEntity(tracked.id);
            if (entity != null && !entity.isRemoved()) {
                ForceTickEntityData.get(server).put(tracked.id, level.dimension(), entity.chunkPosition());
            }
            tracked.release(server);
        }
        TRACKED.clear();
        lastEnabled = null;
    }

    private static final class Tracked {
        final UUID id;
        ResourceKey<Level> dimension;
        ChunkPos center;
        boolean ticketed;
        int unresolvedTicks;

        Tracked(UUID id, ResourceKey<Level> dimension, ChunkPos center) {
            this.id = id;
            this.dimension = dimension;
            this.center = center;
        }

        void update(MinecraftServer server, Entity entity) {
            if (!dimension.equals(entity.level().dimension()) || !center.equals(entity.chunkPosition())) {
                release(server);
                dimension = entity.level().dimension();
                center = entity.chunkPosition();
            }
            unresolvedTicks = 0;
            ForceTickEntityData.get(server).put(id, dimension, center);
            if (AnnoyingVillagersConfig.FORCE_TICK_MOBS.get()) ensureTicket(server);
        }

        void ensureTicket(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            if (!ticketed && level != null) {
                // Exactly one moving level-31 entity-ticking anchor, without additional
                // Forge natural-spawn/random-tick activation at a remote mob's center.
                level.getChunkSource().addRegionTicket(TICKET, center, 2, id, false);
                ticketed = true;
            }
        }

        void release(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            if (ticketed && level != null) level.getChunkSource().removeRegionTicket(TICKET, center, 2, id, false);
            ticketed = false;
        }
    }
}
