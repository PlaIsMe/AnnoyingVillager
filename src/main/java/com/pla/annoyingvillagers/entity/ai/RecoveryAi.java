package com.pla.annoyingvillagers.entity.ai;

import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.event.AVNpcRecoveryEvent;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigStunController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared physical recovery mechanics adapted from SmartNpc's tool/break/place helpers. */
public final class RecoveryAi {
    private static final Map<ServerLevel, PathBudget> PATH_BUDGETS = new WeakHashMap<>();
    private final AVNpc npc;
    private ItemStack savedHand = ItemStack.EMPTY;
    private ItemStack workHand = ItemStack.EMPTY;
    private int swappedSlot = -1;
    private BlockPos breakingPos;
    private BlockState breakingState;
    private int breakTicks;
    private int breakStage = -1;

    public RecoveryAi(AVNpc npc) { this.npc = npc; }

    public static boolean canAct(AVNpc npc) {
        return npc.level() instanceof ServerLevel && npc.isAlive() && !npc.isRemoved()
                && !npc.isNoAi() && !npc.isPassenger() && !npc.isHealing() && !RigStunController.isStunned(npc);
    }

    public static boolean canStart(AVNpc npc) {
        String reason = !canAct(npc) ? "cannot_act_see_flags"
                : npc.isRecoveryActionActive() ? "recovery_already_owned"
                : npc.isLocked() ? "rig_lock"
                : npc.isUsingItem() ? "using_item"
                : npc.getBlockDamage() != null ? "block_damage_active"
                : RigAnimationController.hasActiveAnimation(npc) && !RigAnimationController.hasActiveProfileAttack(npc)
                    ? "non_profile_animation"
                : MinecraftForge.EVENT_BUS.post(new AVNpcRecoveryEvent(npc, AVNpcRecoveryEvent.Action.CHECK_START))
                    ? "compat_check_start_veto" : "allowed";
        com.pla.annoyingvillagers.util.RecoveryTrace.note(npc, "admission", reason);
        return reason.equals("allowed");
    }

    /** Commit the handoff only after a recovery goal has acquired its counted lock. */
    public static void startRecovery(AVNpc npc) {
        // Ordinary profile attacks yield to recovery. Removing this state
        // also invalidates its scheduled motion/collider callbacks before we jump or dig.
        if (RigAnimationController.hasActiveProfileAttack(npc)) {
            RigAnimationController.stop(npc, RigAnimationController.getActiveAnimationId(npc));
        }
        MinecraftForge.EVENT_BUS.post(new AVNpcRecoveryEvent(npc, AVNpcRecoveryEvent.Action.START));
    }

    public static boolean validTarget(AVNpc npc, LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved() && !npc.isAlliedTo(target)
                && target.level() == npc.level() && npc.distanceToSqr(target) <= 28 * 28
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    /** Shared per-level cap in addition to each goal's staggered >=20 tick discovery cadence. */
    public static boolean admitPath(AVNpc npc) {
        ServerLevel level = (ServerLevel) npc.level();
        PathBudget budget = PATH_BUDGETS.computeIfAbsent(level, ignored -> new PathBudget());
        if (budget.tick != level.getGameTime()) { budget.tick = level.getGameTime(); budget.count = 0; }
        if (budget.count >= 6) return false;
        budget.count++;
        return true;
    }

    private static final class PathBudget { long tick = Long.MIN_VALUE; int count; }

    public static boolean loadedCorridor(AVNpc npc, Vec3 end) {
        Vec3 start = npc.position();
        int steps = Mth.ceil(start.distanceTo(end));
        if (steps > 40) return false;
        // Navigation samples a region around the mob, not only the direct ray. Refuse an
        // incomplete cache rather than treating an unloaded edge as proof of an obstruction.
        int cx = npc.getBlockX();
        int cz = npc.getBlockZ();
        for (int x = (cx - 40) >> 4; x <= (cx + 40) >> 4; x++) {
            for (int z = (cz - 40) >> 4; z <= (cz + 40) >> 4; z++) {
                if (!npc.level().hasChunk(x, z)) return false;
            }
        }
        for (int i = 0; i <= steps; i++) {
            BlockPos cell = BlockPos.containing(start.lerp(end, i / (double) Math.max(1, steps)));
            if (!npc.level().hasChunkAt(cell) || !npc.level().hasChunkAt(cell.above(3))) return false;
        }
        return true;
    }

    public static Path targetPath(AVNpc npc, BlockPos pos) {
        // Caller owns the admission and cadence. Limit expansion; a null path alone never selects a block.
        var navigation = npc.getNavigation();
        navigation.setMaxVisitedNodesMultiplier(.15F);
        try {
            return navigation.createPath(pos, 0, 32);
        } finally {
            navigation.resetMaxVisitedNodesMultiplier();
        }
    }

    public static boolean reachable(Path path) { return path != null && path.canReach(); }

    public static boolean canBreak(AVNpc npc, BlockPos pos) {
        if (!npc.level().hasChunkAt(pos) || !npc.level().getWorldBorder().isWithinBounds(pos)
                || !npc.level().isInWorldBounds(pos)) return false;
        BlockState state = npc.level().getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getDestroySpeed(npc.level(), pos) >= 0.0F
                && !state.getCollisionShape(npc.level(), pos).isEmpty()
                && ForgeEventFactory.getMobGriefingEvent(npc.level(), npc);
    }

    public static boolean intersectsBody(AVNpc npc, BlockPos pos) {
        if (!npc.level().hasChunkAt(pos)) return false;
        var shape = npc.level().getBlockState(pos).getCollisionShape(npc.level(), pos, CollisionContext.of(npc));
        return !shape.isEmpty() && Shapes.joinIsNotEmpty(shape.move(pos.getX(), pos.getY(), pos.getZ()),
                Shapes.create(npc.getBoundingBox().deflate(0.02D)), BooleanOp.AND);
    }

    public static boolean inReach(AVNpc npc, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (npc.getEyePosition().distanceToSqr(center) > 3.25D * 3.25D) return false;
        var hit = npc.level().clip(new ClipContext(npc.getEyePosition(), center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    public void equipTool(BlockState state) {
        ItemStack best = npc.getMainHandItem();
        int slot = -1;
        float bestSpeed = best.getDestroySpeed(state);
        for (int i = 0; i < npc.getInventory().getContainerSize(); i++) {
            ItemStack candidate = npc.getInventory().getItem(i);
            float speed = candidate.getDestroySpeed(state);
            if (!candidate.isEmpty() && (speed > bestSpeed
                    || candidate.isCorrectToolForDrops(state) && !best.isCorrectToolForDrops(state))) {
                best = candidate; bestSpeed = speed; slot = i;
            }
        }
        if (slot >= 0) swapToSlot(slot);
    }

    /** Exchange real stacks. The weapon stays in the inventory even if the NPC dies while working. */
    public void swapToSlot(int slot) {
        if (swappedSlot == slot) return;
        restoreHand();
        savedHand = npc.getMainHandItem();
        workHand = npc.getInventory().getItem(slot);
        npc.getInventory().setItem(slot, savedHand);
        npc.setItemInHand(InteractionHand.MAIN_HAND, workHand);
        swappedSlot = slot;
    }

    public void restoreHand() {
        if (swappedSlot < 0) return;
        // Do not recreate a weapon if an external disarm/transform has already moved it.
        if (npc.getInventory().getItem(swappedSlot) == savedHand
                && (npc.getMainHandItem() == workHand || npc.getMainHandItem().isEmpty() && workHand.isEmpty())) {
            npc.getInventory().setItem(swappedSlot, npc.getMainHandItem());
            npc.setItemInHand(InteractionHand.MAIN_HAND, savedHand);
        }
        swappedSlot = -1;
        savedHand = ItemStack.EMPTY;
        workHand = ItemStack.EMPTY;
    }

    /** @return true when the selected block has gone; pauses retain earned progress. */
    public boolean breakBlock(BlockPos pos, boolean bodyOverlap) {
        if (!canBreak(npc, pos) || !canAct(npc)) { stopBreaking(); return true; }
        if (!(bodyOverlap ? intersectsBody(npc, pos) : inReach(npc, pos))) { pauseBreaking(); return false; }
        ServerLevel level = (ServerLevel) npc.level();
        BlockState state = level.getBlockState(pos);
        if (!pos.equals(breakingPos) || state != breakingState) {
            stopBreaking();
            breakingPos = pos.immutable(); breakingState = state;
            equipTool(state);
        }
        npc.getLookControl().setLookAt(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D);
        npc.setRecoveryDigging(true);
        if (breakTicks % 10 == 0 && !MinecraftForge.EVENT_BUS.post(new AVNpcRecoveryEvent(npc, AVNpcRecoveryEvent.Action.DIG))) {
            RigAnimationController.play(npc, com.pla.annoyingvillagers.rig.RigAnimationId.DIG_MAINHAND);
        }
        ItemStack tool = npc.getMainHandItem();
        boolean correct = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        float hardness = state.getDestroySpeed(level, pos);
        int needed = Mth.clamp(Mth.ceil(hardness * (correct ? 30.0F : 100.0F) / Math.max(1.0F, tool.getDestroySpeed(state))), 1, 600);
        int stage = Math.min(9, ++breakTicks * 10 / needed);
        if (stage != breakStage) { level.destroyBlockProgress(npc.getId(), pos, stage); breakStage = stage; }
        if (breakTicks < needed) return false;
        // Revalidate the exact selected state and Forge cancellation at mutation time.
        if (level.getBlockState(pos) == state && canBreak(npc, pos)
                && ForgeEventFactory.onEntityDestroyBlock(npc, pos, state)) {
            ItemStack usedTool = tool.copy();
            if (level.destroyBlock(pos, false, npc)) {
                if (correct) Block.dropResources(state, level, pos, null, npc, usedTool);
                tool.getItem().mineBlock(tool, level, state, pos, npc);
            }
        }
        stopBreaking();
        return true;
    }

    public void pauseBreaking() {
        if (breakingPos != null && breakStage >= 0) npc.level().destroyBlockProgress(npc.getId(), breakingPos, -1);
        breakStage = -1;
        if (npc.isRecoveryDigging()) {
            npc.setRecoveryDigging(false);
            RigAnimationController.stop(npc, com.pla.annoyingvillagers.rig.RigAnimationId.DIG_MAINHAND);
            MinecraftForge.EVENT_BUS.post(new AVNpcRecoveryEvent(npc, AVNpcRecoveryEvent.Action.STOP_DIG));
        }
    }

    public void stopBreaking() {
        pauseBreaking(); breakingPos = null; breakingState = null; breakTicks = 0;
    }

    public int blockSlot(BlockPos support) {
        if (placeable(npc.getMainHandItem(), support)) return -2;
        for (int i = 0; i < npc.getInventory().getContainerSize(); i++) {
            if (placeable(npc.getInventory().getItem(i), support)) return i;
        }
        return -1;
    }

    private boolean placeable(ItemStack stack, BlockPos support) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || !npc.level().hasChunkAt(support)) return false;
        BlockState state = item.getBlock().defaultBlockState();
        // Material/namespace independent, including modded masonry. This physical helper
        // cannot transfer a container/machine's BlockEntityTag, so keep those stacks intact.
        return state.isCollisionShapeFullBlock(npc.level(), support) && !state.hasBlockEntity()
                && state.getFluidState().isEmpty();
    }

    public boolean placeUnderFeet(BlockPos pos) {
        if (!canAct(npc) || !npc.level().hasChunkAt(pos) || !npc.level().hasChunkAt(pos.below())
                || !npc.level().getWorldBorder().isWithinBounds(pos) || !npc.level().isInWorldBounds(pos)
                || !ForgeEventFactory.getMobGriefingEvent(npc.level(), npc)) return placementTrace("world_or_mob_griefing_gate");
        ItemStack held = npc.getMainHandItem();
        if (!placeable(held, pos)) return placementTrace("held_item_not_suitable");
        BlockState state = ((BlockItem) held.getItem()).getBlock().defaultBlockState();
        BlockState previous = npc.level().getBlockState(pos);
        if (!previous.canBeReplaced() || !previous.getFluidState().isEmpty()) return placementTrace("cell_occupied_or_fluid");
        if (!npc.level().getBlockState(pos.below()).isFaceSturdy(npc.level(), pos.below(), Direction.UP)) return placementTrace("no_solid_support");
        if (!state.canSurvive(npc.level(), pos)) return placementTrace("block_cannot_survive");
        if (npc.getBoundingBox().intersects(new AABB(pos))) return placementTrace("npc_body_overlaps_cell");
        if (!npc.level().isUnobstructed(state, pos, CollisionContext.empty())
                || !npc.level().getEntities(npc, new AABB(pos)).isEmpty()) return placementTrace("entity_collision");
        if (state.getBlock() instanceof FallingBlock && !npc.level().getBlockState(pos.below()).getFluidState().isEmpty()) return placementTrace("falling_block_fluid_support");
        BlockSnapshot snapshot = BlockSnapshot.create(npc.level().dimension(), npc.level(), pos);
        if (!npc.level().setBlock(pos, state, 3)) return placementTrace("set_block_failed");
        if (ForgeEventFactory.onBlockPlace(npc, snapshot, Direction.UP)) {
            snapshot.restore(true, false);
            return placementTrace("forge_place_event_cancelled");
        }
        held.shrink(1);
        placementTrace("placed pos=" + pos);
        npc.level().playSound(null, pos, state.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, .8F, 1F);
        if (!MinecraftForge.EVENT_BUS.post(new AVNpcRecoveryEvent(npc, AVNpcRecoveryEvent.Action.USE))) {
            RigAnimationController.play(npc, com.pla.annoyingvillagers.rig.RigAnimationId.USE_MAINHAND);
        }
        return true;
    }

    private boolean placementTrace(String reason) {
        com.pla.annoyingvillagers.util.RecoveryTrace.note(npc, "placement", reason);
        return false;
    }
}
