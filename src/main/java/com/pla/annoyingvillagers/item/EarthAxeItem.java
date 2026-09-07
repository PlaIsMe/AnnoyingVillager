package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.entity.FloatingLookBlockEntity;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import com.pla.annoyingvillagers.entity.RisingWallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EarthAxeItem extends SwordItem implements RigCombatProfileProvider {
    public static final int VANILLA_WALL_COOLDOWN_TICKS = 20 * 60;
    public static final int VANILLA_LIFT_COOLDOWN_TICKS = 20 * 10;
    private static final int WALL_WIDTH = 5;
    private static final int WALL_HEIGHT = 4;
    private static final int WALL_DISTANCE = 2;
    private static final int RISE_TICKS = 10;

    public EarthAxeItem() {
        super(new Tier() {
            public int getUses() {
                return 2031;
            }

            public float getSpeed() {
                return 6.0F;
            }

            public float getAttackDamageBonus() {
                return 6.0F;
            }

            public int getLevel() {
                return 5;
            }

            public int getEnchantmentValue() {
                return 21;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.NETHERITE_INGOT));
            }
        }, 3, -2.8F, (new Properties()));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()
                || hand != InteractionHand.MAIN_HAND
                || player.getCooldowns().isOnCooldown(this)) {
            return super.use(level, player, hand);
        }

        if (level instanceof ServerLevel serverLevel) {
            summonEarthWall(serverLevel, player);
            VanillaWeaponAbilityUtil.swingMainHand(player);
            VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
            player.getCooldowns().addCooldown(this, VANILLA_WALL_COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()
                || player.level().isClientSide()
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EarthAxeItem item)
                || player.getCooldowns().isOnCooldown(item)) {
            return false;
        }

        Vec3 bladePos = CommonUtil.getVanillaSwordOrBodyPosition(player)
                .add(player.getLookAngle().scale(0.5D));
        BlockPos liftPos = findLiftableBlockUnderPoint(serverLevel, bladePos, 6, 1);
        if (liftPos != null) {
            liftBlockAt(serverLevel, liftPos, player);
        }

        VanillaWeaponAbilityUtil.swingMainHand(player);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        player.getCooldowns().addCooldown(item, VANILLA_LIFT_COOLDOWN_TICKS);
        return true;
    }

    public static void summonEarthWall(ServerLevel level, LivingEntity caster) {
        if (caster.isInWater()) {
            return;
        }

        BlockPos groundPos = caster.getOnPos();
        BlockState groundState = level.getBlockState(groundPos);

        if (groundState.getFluidState().is(FluidTags.WATER)) {
            return;
        }

        BlockState wallState = chooseWallBlock(groundState);

        if (wallState == null) {
            return;
        }

        Direction forward = caster.getDirection();
        Direction right = forward.getClockWise();

        BlockPos casterFeet = caster.blockPosition();

        BlockPos centerBase = new BlockPos(
                casterFeet.getX(),
                groundPos.getY() + 1,
                casterFeet.getZ()
        ).relative(forward, WALL_DISTANCE);

        int halfWidth = WALL_WIDTH / 2;
        boolean spawnedAny = false;

        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int x = -halfWidth; x <= halfWidth; x++) {
                BlockPos finalPos = centerBase.relative(right, x).above(y);

                if (!canSpawnWallBlockAt(level, finalPos)) {
                    continue;
                }

                int delay = y * 3 + Math.abs(x) * 2;

                RisingWallBlockEntity blockEntity = new RisingWallBlockEntity(
                        level,
                        finalPos,
                        wallState,
                        delay,
                        RISE_TICKS
                );

                level.addFreshEntity(blockEntity);
                spawnedAny = true;
            }
        }

    }

    private static boolean canLiftBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.getRenderShape() != RenderShape.MODEL) {
            return false;
        }

        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.STRUCTURE_VOID)
                || state.is(Blocks.JIGSAW)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof Container) {
            return false;
        }

        return true;
    }

    @Nullable
    public static BlockPos findLiftableBlockUnderPoint(
            ServerLevel level,
            Vec3 worldPos,
            int maxDown,
            int horizontalRadius
    ) {
        BlockPos center = BlockPos.containing(worldPos);

        for (int dy = 0; dy <= maxDown; dy++) {
            int y = center.getY() - dy;

            BlockPos bestPos = null;
            double bestDistance = Double.MAX_VALUE;

            for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    BlockPos candidate = new BlockPos(
                            center.getX() + dx,
                            y,
                            center.getZ() + dz
                    );

                    if (!level.getWorldBorder().isWithinBounds(candidate)) {
                        continue;
                    }

                    if (!level.hasChunkAt(candidate)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(candidate);

                    if (!canLiftBlock(level, candidate, state)) {
                        continue;
                    }

                    double distance = Vec3.atCenterOf(candidate).distanceToSqr(worldPos);

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = candidate;
                    }
                }
            }

            if (bestPos != null) {
                return bestPos.immutable();
            }
        }

        return null;
    }

    private static boolean canSpawnWallBlockAt(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        return current.isAir() || current.canBeReplaced();
    }

    @Nullable
    private static BlockState chooseWallBlock(BlockState groundState) {
        if (groundState.is(BlockTags.SAND)
                || groundState.is(Blocks.SAND)
                || groundState.is(Blocks.RED_SAND)
                || groundState.is(Blocks.SANDSTONE)
                || groundState.is(Blocks.RED_SANDSTONE)
                || groundState.is(Blocks.SMOOTH_SANDSTONE)
                || groundState.is(Blocks.SMOOTH_RED_SANDSTONE)) {
            return Blocks.SAND.defaultBlockState();
        }

        if (groundState.is(BlockTags.DIRT)
                || groundState.is(Blocks.GRASS_BLOCK)
                || groundState.is(Blocks.DIRT)
                || groundState.is(Blocks.COARSE_DIRT)
                || groundState.is(Blocks.ROOTED_DIRT)
                || groundState.is(Blocks.PODZOL)
                || groundState.is(Blocks.MYCELIUM)
                || groundState.is(Blocks.FARMLAND)
                || groundState.is(Blocks.MUD)) {
            return Blocks.DIRT.defaultBlockState();
        }

        if (isWoodRelated(groundState)) {
            return Blocks.DIRT.defaultBlockState();
        }

        if (isStoneRelated(groundState)
                || groundState.is(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }

        return null;
    }

    private static boolean isWoodRelated(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES)
                || state.is(BlockTags.WOODEN_BUTTONS);
    }

    private static boolean isStoneRelated(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.END_STONE);
    }

    public static boolean liftBlockAt(ServerLevel level, BlockPos pos, @Nullable LivingEntity owner) {
        return liftBlockAt(level, pos, owner == null ? null : owner.getUUID());
    }

    public static boolean liftBlockAt(ServerLevel level, BlockPos pos, @Nullable UUID ownerUuid) {
        pos = pos.immutable();

        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        if (!level.hasChunkAt(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!canLiftBlock(level, pos, state)) {
            return false;
        }
        CompoundTag blockEntityTag = saveBlockEntityTag(level, pos);
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        FloatingLookBlockEntity floatingBlock = new FloatingLookBlockEntity(
                level,
                pos,
                state,
                ownerUuid,
                blockEntityTag
        );
        level.addFreshEntity(floatingBlock);
        return true;
    }

    @Nullable
    private static CompoundTag saveBlockEntityTag(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity == null) {
            return null;
        }

        return blockEntity.saveWithFullMetadata();
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.EARTH_AXE;
    }
}
