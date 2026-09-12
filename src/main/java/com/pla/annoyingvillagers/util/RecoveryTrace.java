package com.pla.annoyingvillagers.util;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.entity.ai.RecoveryAi;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigStunController;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Explicit, temporary per-NPC diagnostics. Never invoke goal eligibility/pathfinding from tracing. */
@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID)
public final class RecoveryTrace {
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private static final class Session {
        final AVNpc npc;
        final long expires;
        final Map<String, String> decisions = new LinkedHashMap<>();
        Session(AVNpc npc, long expires) { this.npc = npc; this.expires = expires; }
    }

    public static void note(AVNpc npc, String stage, String reason) {
        Session session = SESSIONS.get(npc.getUUID());
        if (session != null) {
            session.decisions.put(stage, reason + "@tick=" + npc.tickCount);
            // Keep short-lived jump/landing events visible between the one-second samples.
            if (reason.startsWith("jump_started")) session.decisions.put("lastJump", reason + "@tick=" + npc.tickCount);
            if (reason.startsWith("landed")) session.decisions.put("lastLanding", reason + "@tick=" + npc.tickCount);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("avrecoverytrace")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("off").executes(context -> {
                    SESSIONS.clear();
                    context.getSource().sendSuccess(() -> Component.literal("AV recovery tracing disabled."), false);
                    return 1;
                }))
                .then(Commands.argument("npc", EntityArgument.entity()).executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "npc");
                    if (!(entity instanceof AVNpc npc)) {
                        context.getSource().sendFailure(Component.literal("Select an AvNpc such as Steve."));
                        return 0;
                    }
                    if (SESSIONS.size() >= 8 && !SESSIONS.containsKey(npc.getUUID())) {
                        context.getSource().sendFailure(Component.literal("Already tracing 8 NPCs. Use /avrecoverytrace off first."));
                        return 0;
                    }
                    Session session = new Session(npc, context.getSource().getServer().overworld().getGameTime() + 12000);
                    SESSIONS.put(npc.getUUID(), session);
                    snapshot(session);
                    context.getSource().sendSuccess(() -> Component.literal("Tracing " + npc.getName().getString()
                            + " for 10 minutes in logs/latest.log (AV recovery trace). Stop: /avrecoverytrace off"), false);
                    return 1;
                })));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 20 != 0) return;
        var iterator = SESSIONS.values().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            if (now >= session.expires || session.npc.isRemoved()) {
                AnnoyingVillagers.LOGGER.info("AV recovery trace ended: npc={} uuid={} reason={}",
                        session.npc.getName().getString(), session.npc.getUUID(),
                        session.npc.isRemoved() ? "entity_removed" : "trace_expired");
                iterator.remove();
            } else snapshot(session);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }

    private static void snapshot(Session session) {
        AVNpc npc = session.npc;
        var target = npc.getTarget();
        StringBuilder blocks = new StringBuilder();
        for (int slot = 0; slot < npc.getInventory().getContainerSize(); slot++) {
            var stack = npc.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem) blocks.append(slot).append(':')
                    .append(ForgeRegistries.ITEMS.getKey(stack.getItem())).append('x').append(stack.getCount()).append(' ');
        }
        String goals = npc.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning())
                .map(g -> g.getPriority() + ":" + g.getGoal().getClass().getSimpleName()
                        + ":interruptible=" + g.isInterruptable() + ":flags=" + g.getFlags())
                .collect(Collectors.joining(","));
        AnnoyingVillagers.LOGGER.info("AV recovery trace: npc={}#{} uuid={} tick={} dim={} pos={} velocity={} ground={} collision={} noAI={} passenger={} healing={} rigLocked={} stunned={} recovery={} rig={} target={} hand={} blocks=[{}] eligibleBlockSlot={} navDone={} running=[{}] decisions={}",
                npc.getName().getString(), npc.getId(), npc.getUUID(), npc.tickCount, npc.level().dimension().location(),
                npc.position(), npc.getDeltaMovement(), npc.onGround(), npc.horizontalCollision, npc.isNoAi(), npc.isPassenger(),
                npc.isHealing(), npc.isLocked(), RigStunController.isStunned(npc), npc.isRecoveryActionActive(),
                RigAnimationController.getActiveAnimationId(npc),
                target == null ? "none" : target.getName().getString() + "#" + target.getId() + " pos=" + target.position()
                        + " dy=" + (target.getY() - npc.getY()) + " valid=" + RecoveryAi.validTarget(npc, target),
                npc.getMainHandItem(), blocks, new RecoveryAi(npc).blockSlot(npc.blockPosition()), npc.getNavigation().isDone(), goals,
                session.decisions.isEmpty() ? "not_checked_since_enabled" : session.decisions);
    }
}
