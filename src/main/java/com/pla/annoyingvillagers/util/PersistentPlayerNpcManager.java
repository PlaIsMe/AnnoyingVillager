package com.pla.annoyingvillagers.util;

import com.mojang.authlib.GameProfile;
import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.clazz.PersistentPlayerNpc;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.spawnhandler.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** Owns runtime tickets and tab rows; SavedData owns only identity and restoration coordinates. */
@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID)
public final class PersistentPlayerNpcManager {
    private static final TicketType<UUID> TICKET = ForceTickEntityManager.TICKET;
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private static boolean stopping;
    private PersistentPlayerNpcManager() {}

    public static boolean isTabProfileName(String name) {
        if (name == null || name.length() != 16 || !name.startsWith("zzAVN")) return false;
        for (int i = 5; i < 16; i++) if (Character.digit(name.charAt(i), 16) < 0) return false;
        return true;
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (stopping || event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AVNpc npc) || !(npc instanceof PersistentPlayerNpc identity)
                || !npc.isAlive() || npc.isRemoved()) return;
        MinecraftServer server = level.getServer();
        PersistentPlayerNpcData data = PersistentPlayerNpcData.get(server);
        // Alternate forms share an identity. A dimension transfer retains the same entity UUID.
        if (data.entries().stream().anyMatch(entry -> entry.identity().equals(identity.persistentPlayerIdentity())
                && !entry.npcId().equals(npc.getUUID()))) {
            event.setCanceled(true);
            npc.discard();
            return;
        }
        Session session = SESSIONS.computeIfAbsent(npc.getUUID(),
                id -> new Session(id, identity.persistentPlayerIdentity(), level.dimension(), npc.chunkPosition()));
        session.update(server, npc);
        npc.setPersistenceRequired();
        claim(level, session.identity, npc.getUUID());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (stopping || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof PersistentPlayerNpc)) return;
        Entity npc = event.getEntity();
        Session session = SESSIONS.get(npc.getUUID());
        if (session == null) return;
        session.removeRow(level.getServer());
        session.releaseTicket(level.getServer());
        if (npc.getRemovalReason() == Entity.RemovalReason.KILLED
                || npc.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            SESSIONS.remove(npc.getUUID());
            PersistentPlayerNpcData.get(level.getServer()).remove(npc.getUUID());
            for (ServerLevel previous : level.getServer().getAllLevels()) switch (session.identity) {
                case "Steve" -> SteveData.get(previous).releaseIfMatches(previous, session.id);
                case "Alex" -> AlexData.get(previous).releaseIfMatches(previous, session.id);
                case "Chris" -> ChrisData.get(previous).releaseIfMatches(previous, session.id);
            }
        }
        // CHANGED_DIMENSION and unload retain identity. The destination join or restoration
        // refresh reattaches the runtime ticket, without freeing the singleton slot.
        else {
            session.dimension = level.dimension();
            session.center = npc.chunkPosition();
            PersistentPlayerNpcData.get(level.getServer()).put(
                    session.id, session.dimension, session.center, session.identity);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof PersistentPlayerNpc)
                || !(event.getEntity().level() instanceof ServerLevel level)) return;
        Session session = SESSIONS.get(event.getEntity().getUUID());
        if (session != null) session.removeRow(level.getServer());
        // Keep the ticket until actual removal so delayed death/transformation ticks finish.
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        stopping = false;
        MinecraftServer server = event.getServer();
        for (var entry : PersistentPlayerNpcData.get(server).entries()) {
            Session session = SESSIONS.computeIfAbsent(entry.npcId(), id ->
                    new Session(id, entry.identity(), entry.levelKey(), entry.centerChunk()));
            session.ensureTicket(server);
        }
        // One reconciliation per server start, never per viewer login.
        for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AVNpc npc && npc instanceof PersistentPlayerNpc identity && npc.isAlive()) {
                Session session = SESSIONS.computeIfAbsent(npc.getUUID(), id ->
                        new Session(id, identity.persistentPlayerIdentity(), level.dimension(), npc.chunkPosition()));
                session.update(server, npc);
                claim(level, identity.persistentPlayerIdentity(), npc.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (stopping || event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            if (Math.floorMod(server.getTickCount(), 20) != Math.floorMod(session.id.hashCode(), 20)) continue;
            ServerLevel level = server.getLevel(session.dimension);
            if (level == null) {
                session.removeRow(server);
                SESSIONS.remove(session.id);
                PersistentPlayerNpcData.get(server).remove(session.id);
                vacate(server.overworld(), session.identity, session.id);
                continue;
            }
            Entity entity = level.getEntity(session.id);
            if (entity instanceof AVNpc npc && !npc.isRemoved()) {
                session.unresolvedTicks = 0;
                if (!npc.isAlive()) continue;
                session.update(server, npc);
                if (RemoteNpcDeparture.tick(npc, session.ticketed,
                        AnnoyingVillagersConfig.REMOTE_NPC_DEPARTURE_ENABLED.get(),
                        AnnoyingVillagersConfig.REMOTE_NPC_DEPARTURE_MIN_MINUTES.get(),
                        AnnoyingVillagersConfig.REMOTE_NPC_DEPARTURE_MAX_MINUTES.get())) {
                    server.getPlayerList().broadcastSystemMessage(Component.translatable(
                            "multiplayer.player.left", Component.literal(session.identity))
                            .withStyle(ChatFormatting.YELLOW), false);
                    // Explicit departure immediately frees the spawn slot. Normal death
                    // retains the existing singleton respawn cooldown.
                    vacate(level, session.identity, session.id);
                    npc.discard();
                }
            } else {
                session.ensureTicket(server);
                // Saved coordinates may refer to an entity removed by an older version.
                // Only prune after the chunk has actually loaded and had time to load entities.
                if (level.hasChunk(session.center.x, session.center.z)) session.unresolvedTicks += 20;
                if (session.unresolvedTicks >= 600) {
                    session.removeRow(server);
                    session.releaseTicket(server);
                    SESSIONS.remove(session.id);
                    PersistentPlayerNpcData.get(server).remove(session.id);
                    vacate(level, session.identity, session.id);
                }
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<ServerPlayer> rows = SESSIONS.values().stream().filter(s -> s.listed && s.tabPlayer != null)
                .map(s -> (ServerPlayer) s.tabPlayer).toList();
        if (!rows.isEmpty()) player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(rows));
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        stopping = true;
        for (Session session : SESSIONS.values()) {
            ServerLevel level = event.getServer().getLevel(session.dimension);
            Entity npc = level == null ? null : level.getEntity(session.id);
            if (npc != null && !npc.isRemoved()) {
                PersistentPlayerNpcData.get(event.getServer()).put(
                        session.id, level.dimension(), npc.chunkPosition(), session.identity);
            }
            session.removeRow(event.getServer());
            session.releaseTicket(event.getServer());
        }
        SESSIONS.clear();
        ExternalChunkActivity.clear();
        // Entity join events during the next integrated world happen before ServerStarted.
        stopping = false;
    }

    private static void claim(ServerLevel level, String identity, UUID id) {
        switch (identity) {
            case "Steve" -> SteveData.get(level).forceClaim(level, id);
            case "Alex" -> AlexData.get(level).forceClaim(level, id);
            case "Chris" -> ChrisData.get(level).forceClaim(level, id);
        }
    }

    private static void vacate(ServerLevel level, String identity, UUID id) {
        // Clear matching claims in any previous dimension too.
        for (ServerLevel previous : level.getServer().getAllLevels()) switch (identity) {
            case "Steve" -> SteveData.get(previous).vacateIfMatches(id);
            case "Alex" -> AlexData.get(previous).vacateIfMatches(id);
            case "Chris" -> ChrisData.get(previous).vacateIfMatches(id);
        }
    }

    private static final class Session {
        final UUID id;
        final String identity;
        ResourceKey<Level> dimension;
        ChunkPos center;
        boolean ticketed;
        boolean listed;
        int unresolvedTicks;
        FakePlayer tabPlayer;

        Session(UUID id, String identity, ResourceKey<Level> dimension, ChunkPos center) {
            this.id = id;
            this.identity = identity;
            this.dimension = dimension;
            this.center = center;
        }

        void update(MinecraftServer server, AVNpc npc) {
            ServerLevel level = (ServerLevel) npc.level();
            if (!dimension.equals(level.dimension()) || !center.equals(npc.chunkPosition())) {
                releaseTicket(server);
                dimension = level.dimension();
                center = npc.chunkPosition();
            }
            ensureTicket(server);
            PersistentPlayerNpcData.get(server).put(id, dimension, center, identity);
            if (!listed) {
                // A packet-only fake player, never inserted into PlayerList or the world.
                GameProfile profile = new GameProfile(id,
                        ("zzAVN" + id.toString().replace("-", "")).substring(0, 16));
                NpcTabSkin skin = ((PersistentPlayerNpc) npc).tabSkin();
                if (skin != null) skin.apply(profile);
                tabPlayer = new FakePlayer(level, profile) {
                    @Override public Component getTabListDisplayName() {
                        return Component.literal("[NPC] ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(identity));
                    }
                };
                tabPlayer.setGameMode(GameType.SPECTATOR);
                server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(tabPlayer)));
                listed = true;
            }
        }

        void ensureTicket(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            if (!ticketed && level != null) {
                // Distance 2 already gives entity ticking at the center. false avoids
                // remote natural-spawning/random-tick work and matches removal identity.
                level.getChunkSource().addRegionTicket(TICKET, center, 2, id, false);
                ticketed = true;
            }
        }

        void releaseTicket(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            if (ticketed && level != null) level.getChunkSource().removeRegionTicket(TICKET, center, 2, id, false);
            ticketed = false;
        }

        void removeRow(MinecraftServer server) {
            if (listed) server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(id)));
            listed = false;
            tabPlayer = null;
        }
    }
}
