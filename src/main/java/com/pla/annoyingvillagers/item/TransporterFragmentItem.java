package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.entity.PortalEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.network.ClientboundHerobrinePortalFx;
import com.pla.annoyingvillagers.network.ClientboundPlayerGroundTransitionPosition;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import com.pla.annoyingvillagers.util.HerobrinePortalUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransporterFragmentItem extends Item {
    public record PortalSpawnBatch(UUID portalGroup, int spawned) {
    }

    public static final int MAX_DURABILITY = 300;
    public static final int SAVED_TELEPORT_DURABILITY_COST = 10;
    public static final int SAVED_TELEPORT_SINK_TICKS = HerobrinePortalUtil.SHINK_TIME_START;
    public static final String NBT_SAVED_TELEPORT_PENDING = "TransporterFragmentTeleportPending";

    private static final int PORTAL_COUNT = 6;
    private static final int MAX_ACTIVE_PORTALS_PER_OWNER = 6;
    private static final int SINGLE_PORTAL_DURABILITY_COST = 1;
    private static final double LOOK_PORTAL_RANGE = 32.0D;
    private static final double SAVED_TELEPORT_ENTITY_RADIUS = 5.0D;
    private static final double SAVED_TELEPORT_SINK_SPEED = 0.06D;
    private static final double SAVED_TELEPORT_RISE_SPEED = 0.06D;
    private static final int HORIZONTAL_SEARCH_RADIUS = 30;
    private static final int VERTICAL_SEARCH_RADIUS = 15;
    private static final int TARGET_PRIORITY_RADIUS = 16;
    private static final double MIN_PORTAL_GAP = 3.0D;
    private static final double MAX_PORTAL_GAP = 6.0D;
    private static final double TARGET_CLUSTER_DISTANCE = 8.0D;
    private static final double CASTER_PORTAL_MIN_DISTANCE = 3.0D;
    private static final double CASTER_PORTAL_MAX_DISTANCE = 5.0D;
    private static final double DISTRIBUTED_PORTAL_NEAR_MIN_DISTANCE = 9.0D;
    private static final double DISTRIBUTED_PORTAL_NEAR_MAX_DISTANCE = 14.0D;
    private static final double DISTRIBUTED_PORTAL_MID_MIN_DISTANCE = 15.0D;
    private static final double DISTRIBUTED_PORTAL_MID_MAX_DISTANCE = 22.0D;
    private static final double DISTRIBUTED_PORTAL_FAR_MIN_DISTANCE = 22.0D;
    private static final double DISTRIBUTED_PORTAL_FAR_MAX_DISTANCE = 29.0D;
    private static final int COOLDOWN_TICKS = 20;
    private static final String TAG_SAVED_LOCATION = "TransporterSavedLocation";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";
    private static final String TAG_TELEPORT_ORIGIN_X = "TransporterFragmentOriginX";
    private static final String TAG_TELEPORT_ORIGIN_Y = "TransporterFragmentOriginY";
    private static final String TAG_TELEPORT_ORIGIN_Z = "TransporterFragmentOriginZ";
    private static final String TAG_TELEPORT_TARGET_X = "TransporterFragmentTargetX";
    private static final String TAG_TELEPORT_TARGET_Y = "TransporterFragmentTargetY";
    private static final String TAG_TELEPORT_TARGET_Z = "TransporterFragmentTargetZ";
    private static final String TAG_TELEPORT_ENTITIES = "TransporterFragmentEntities";
    private static final String TAG_TELEPORT_EXECUTE_TICK = "TransporterFragmentExecuteTick";
    private static final String TAG_ENTITY_COUNT = "Count";
    private static final String TAG_ENTITY_UUID = "UUID";
    private static final String TAG_ENTITY_DX = "DX";
    private static final String TAG_ENTITY_DY = "DY";
    private static final String TAG_ENTITY_DZ = "DZ";

    public TransporterFragmentItem() {
        super(new Properties().stacksTo(1).durability(MAX_DURABILITY).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    public static UseResult tryUseSpecialAttack(Player player) {
        return UseResult.missed();
    }

    public static UseResult tryUseSpecialAttack(Player player, Vec3 crosshairTarget) {
        return UseResult.missed();
    }

    public static UseResult tryUseHeldSpecialAttack(Player player) {
        return tryUseHeldSpecialAttack(player, null);
    }

    public static UseResult tryUseHeldSpecialAttack(Player player, Vec3 crosshairTarget) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()) return UseResult.missed();

        Item transporterFragment = AnnoyingVillagersModItems.TRANSPORTER_FRAGMENT.get();
        boolean mainHand = player.getMainHandItem().is(transporterFragment);
        boolean offHand = player.getOffhandItem().is(transporterFragment);
        if (!mainHand && !offHand) return UseResult.missed();

        if (mainHand) {
            ItemStack stack = player.getMainHandItem();
            UseMode mode = UseMode.MAIN_HAND;
            if (player.getCooldowns().isOnCooldown(transporterFragment)
                    || player.getPersistentData().getBoolean(NBT_SAVED_TELEPORT_PENDING)
                    || player.getPersistentData().getBoolean(HerobrinePortalUtil.NBT_RISING)
                    || player.getPersistentData().getBoolean(HerobrinePortalUtil.NBT_SINKING)
                    || !hasSavedLocation(stack)
                    || !hasDurability(stack, SAVED_TELEPORT_DURABILITY_COST)) {
                return UseResult.consumed(mode, false);
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) return UseResult.consumed(mode, false);

            CompoundTag savedLocation = stack.getTag().getCompound(TAG_SAVED_LOCATION);
            if (!savedLocation.getString(TAG_DIMENSION).equals(serverLevel.dimension().location().toString())) return UseResult.consumed(mode, false);
            Vec3 target = new Vec3(savedLocation.getDouble(TAG_X), savedLocation.getDouble(TAG_Y), savedLocation.getDouble(TAG_Z));
            if (!serverLevel.getWorldBorder().isWithinBounds(BlockPos.containing(target))) return UseResult.consumed(mode, false);

            beginSavedTeleport(serverLevel, player, target);
            damageStack(player, stack, InteractionHand.MAIN_HAND, SAVED_TELEPORT_DURABILITY_COST);
            player.getCooldowns().addCooldown(transporterFragment, COOLDOWN_TICKS);
            return UseResult.consumed(mode, true);
        }

        ItemStack stack = player.getOffhandItem();
        UseMode mode = UseMode.OFF_HAND;
        if (player.getCooldowns().isOnCooldown(transporterFragment) || !hasDurability(stack, SINGLE_PORTAL_DURABILITY_COST)) return UseResult.consumed(mode, false);
        if (!(player.level() instanceof ServerLevel serverLevel)) return UseResult.consumed(mode, false);

        List<PortalEntity> activePortals = findOwnedActivePortals(serverLevel, player);
        if (activePortals.size() + 1 > MAX_ACTIVE_PORTALS_PER_OWNER) return UseResult.consumed(mode, false);
        int spawned = spawnLookPortal(serverLevel, player, activePortals, crosshairTarget);
        if (spawned <= 0) return UseResult.consumed(mode, false);

        damageStack(player, stack, InteractionHand.OFF_HAND, spawned);
        player.getCooldowns().addCooldown(transporterFragment, COOLDOWN_TICKS);
        return UseResult.consumed(mode, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND || !stack.is(AnnoyingVillagersModItems.TRANSPORTER_FRAGMENT.get())) {
            return super.useOn(context);
        }

        if (!context.getLevel().isClientSide()) {
            if (player.isShiftKeyDown()) {
                clearSavedLocation(stack, player);
            } else {
                saveLocation(stack, context.getLevel(), Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace())), player);
            }
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !stack.is(AnnoyingVillagersModItems.TRANSPORTER_FRAGMENT.get())) {
            return super.interactLivingEntity(stack, player, target, hand);
        }

        if (!player.level().isClientSide()) {
            if (player.isShiftKeyDown()) {
                clearSavedLocation(stack, player);
            } else {
                saveLocation(stack, player.level(), target.position(), player);
            }
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !stack.is(AnnoyingVillagersModItems.TRANSPORTER_FRAGMENT.get())) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                clearSavedLocation(stack, player);
            } else if (level instanceof ServerLevel serverLevel) {
                LookPortalTarget target = findLookPortalTarget(serverLevel, player);
                saveLocation(stack, level, snapPortalPosition(target.portalPos), player);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.annoyingvillagers.transporter_fragment"));
        if (!hasSavedLocation(stack)) {
            tooltip.add(Component.literal("Saved Location: none").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        CompoundTag savedLocation = stack.getTag().getCompound(TAG_SAVED_LOCATION);
        tooltip.add(Component.literal("Saved Location").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Saved pos: "
                + Mth.floor(savedLocation.getDouble(TAG_X)) + " "
                + Mth.floor(savedLocation.getDouble(TAG_Y)) + " "
                + Mth.floor(savedLocation.getDouble(TAG_Z))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(savedLocation.getString(TAG_DIMENSION)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static UseMode getUseMode(Player player, Item transporterFragment) {
        boolean hasMainHandFragment = player.getMainHandItem().is(transporterFragment);
        boolean hasOffHandFragment = player.getOffhandItem().is(transporterFragment);

        if (hasMainHandFragment && hasOffHandFragment) {
            return UseMode.BOTH_HANDS;
        }
        if (hasMainHandFragment) {
            return UseMode.MAIN_HAND;
        }
        if (hasOffHandFragment) {
            return UseMode.OFF_HAND;
        }
        return UseMode.NONE;
    }

    private static boolean isSixPortalMode(UseMode mode) {
        return mode == UseMode.MAIN_HAND || mode == UseMode.BOTH_HANDS;
    }

    private static ItemStack getStackForMode(Player player, UseMode mode) {
        return mode == UseMode.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();
    }

    private static boolean hasDurability(ItemStack stack, int cost) {
        return !stack.isEmpty() && stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }

    private static void damageStack(Player player, ItemStack stack, InteractionHand hand, int damage) {
        if (damage <= 0) {
            return;
        }
        stack.hurtAndBreak(damage, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
    }

    private static boolean hasSavedLocation(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SAVED_LOCATION);
    }

    private static void saveLocation(ItemStack stack, Level level, Vec3 pos, Player player) {
        CompoundTag savedLocation = new CompoundTag();
        savedLocation.putDouble(TAG_X, pos.x);
        savedLocation.putDouble(TAG_Y, pos.y);
        savedLocation.putDouble(TAG_Z, pos.z);
        savedLocation.putString(TAG_DIMENSION, level.dimension().location().toString());
        stack.getOrCreateTag().put(TAG_SAVED_LOCATION, savedLocation);
        player.displayClientMessage(Component.literal("Saved Location: "
                + Mth.floor(pos.x) + " "
                + Mth.floor(pos.y) + " "
                + Mth.floor(pos.z)).withStyle(ChatFormatting.AQUA), true);
    }

    private static void clearSavedLocation(ItemStack stack, Player player) {
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_SAVED_LOCATION);
        }
        player.displayClientMessage(Component.literal("Saved Location cleared").withStyle(ChatFormatting.GRAY), true);
    }

    private static void beginSavedTeleport(ServerLevel level, Player player, Vec3 target) {
        Vec3 origin = player.position();
        List<Entity> teleportEntities = collectTeleportEntities(level, player);
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(NBT_SAVED_TELEPORT_PENDING, true);
        tag.putDouble(TAG_TELEPORT_ORIGIN_X, origin.x);
        tag.putDouble(TAG_TELEPORT_ORIGIN_Y, origin.y);
        tag.putDouble(TAG_TELEPORT_ORIGIN_Z, origin.z);
        tag.putDouble(TAG_TELEPORT_TARGET_X, target.x);
        tag.putDouble(TAG_TELEPORT_TARGET_Y, target.y);
        tag.putDouble(TAG_TELEPORT_TARGET_Z, target.z);
        tag.put(TAG_TELEPORT_ENTITIES, buildTeleportEntityTag(teleportEntities, origin));

        sendGroundPortalFx(player, origin);

        level.playSound(null, player.blockPosition(), AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        for (Entity entity : teleportEntities) {
            if (entity instanceof LivingEntity livingEntity) HerobrinePortalUtil.sinkIntoGround(level, livingEntity, SAVED_TELEPORT_SINK_SPEED);
        }

    }

    public static void tickPendingSavedTeleport(Player player) {
        if (player.level().isClientSide()) return;
        CompoundTag tag = player.getPersistentData();
        if (!tag.getBoolean(NBT_SAVED_TELEPORT_PENDING)) return;
        if (!player.isAlive() || player.isRemoved()) clearSavedTeleportState(tag);
    }

    private static List<Entity> collectTeleportEntities(ServerLevel level, Player player) {
        List<Entity> entities = new ArrayList<>();
        entities.add(player);
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(SAVED_TELEPORT_ENTITY_RADIUS), entity ->
                entity.isAlive() && !entity.isSpectator())) {
            entities.add(entity);
        }
        return entities;
    }

    private static CompoundTag buildTeleportEntityTag(List<Entity> entities, Vec3 origin) {
        CompoundTag entitiesTag = new CompoundTag();
        int count = 0;
        for (Entity entity : entities) {
            count = addTeleportEntity(entitiesTag, count, entity, origin);
        }
        entitiesTag.putInt(TAG_ENTITY_COUNT, count);
        return entitiesTag;
    }

    private static int addTeleportEntity(CompoundTag entitiesTag, int index, Entity entity, Vec3 origin) {
        CompoundTag entityTag = new CompoundTag();
        Vec3 offset = entity.position().subtract(origin);
        entityTag.putUUID(TAG_ENTITY_UUID, entity.getUUID());
        entityTag.putDouble(TAG_ENTITY_DX, offset.x);
        entityTag.putDouble(TAG_ENTITY_DY, offset.y);
        entityTag.putDouble(TAG_ENTITY_DZ, offset.z);
        entitiesTag.put(String.valueOf(index), entityTag);
        return index + 1;
    }

    public static void finishPendingSavedTeleport(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag tag = caster.getPersistentData();
        if (!tag.getBoolean(NBT_SAVED_TELEPORT_PENDING)) {
            return;
        }

        Vec3 target = new Vec3(
                tag.getDouble(TAG_TELEPORT_TARGET_X),
                tag.getDouble(TAG_TELEPORT_TARGET_Y),
                tag.getDouble(TAG_TELEPORT_TARGET_Z)
        );
        CompoundTag entitiesTag = tag.getCompound(TAG_TELEPORT_ENTITIES);
        int count = entitiesTag.getInt(TAG_ENTITY_COUNT);

        sendGroundPortalFx(caster, target);
        level.playSound(null, BlockPos.containing(target), AnnoyingVillagersModSounds.PORTAL_NATURAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        Vec3 casterDestination = target;
        UUID casterUuid = caster.getUUID();
        for (int i = 0; i < count; i++) {
            CompoundTag entityTag = entitiesTag.getCompound(String.valueOf(i));
            if (!entityTag.hasUUID(TAG_ENTITY_UUID) || !casterUuid.equals(entityTag.getUUID(TAG_ENTITY_UUID))) continue;
            casterDestination = target.add(entityTag.getDouble(TAG_ENTITY_DX), entityTag.getDouble(TAG_ENTITY_DY), entityTag.getDouble(TAG_ENTITY_DZ));
            break;
        }
        teleportEntityWithRise(level, caster, casterDestination);

        for (int i = 0; i < count; i++) {
            CompoundTag entityTag = entitiesTag.getCompound(String.valueOf(i));
            if (!entityTag.hasUUID(TAG_ENTITY_UUID)) {
                continue;
            }

            UUID entityUuid = entityTag.getUUID(TAG_ENTITY_UUID);
            if (casterUuid.equals(entityUuid)) continue;
            Entity entity = level.getEntity(entityUuid);
            if (entity == null || entity.isRemoved()) continue;

            Vec3 destination = target.add(
                    entityTag.getDouble(TAG_ENTITY_DX),
                    entityTag.getDouble(TAG_ENTITY_DY),
                    entityTag.getDouble(TAG_ENTITY_DZ)
            );
            teleportEntityWithRise(level, entity, destination);
        }

        clearSavedTeleportState(tag);
    }

    private static void teleportEntityWithRise(ServerLevel level, Entity entity, Vec3 destination) {
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof ServerPlayer serverPlayer) {
            startPlayerRise(level, serverPlayer, destination);
            return;
        }
        entity.teleportTo(destination.x, destination.y, destination.z);
        if (entity instanceof LivingEntity livingEntity) {
            clearSinkState(livingEntity);
            HerobrinePortalUtil.finishGroundTransition(livingEntity);
            HerobrinePortalUtil.spawnRising(level, livingEntity, destination.x, destination.z, SAVED_TELEPORT_RISE_SPEED);
        }
    }

    private static void startPlayerRise(ServerLevel level, ServerPlayer player, Vec3 destination) {
        clearSinkState(player);
        HerobrinePortalUtil.finishGroundTransition(player);
        movePlayerTransition(player, destination.x, destination.y - 2.0D, destination.z);
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setInvulnerable(true);
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(HerobrinePortalUtil.NBT_RISING, true);
        tag.putDouble(HerobrinePortalUtil.NBT_TARGET_Y, destination.y + 0.02D);
        tag.putDouble(HerobrinePortalUtil.NBT_SPEED, SAVED_TELEPORT_RISE_SPEED);
        tag.putInt(HerobrinePortalUtil.NBT_TICKS, 0);
        tag.putInt(HerobrinePortalUtil.NBT_MAX_TICKS, 20 * 5);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6F, 0.8F + level.random.nextFloat() * 0.2F);
    }

    public static void movePlayerTransition(ServerPlayer player, double x, double y, double z) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setPos(x, y, z);
        CompoundTag tag = player.getPersistentData();
        int transitionTicks = tag.getBoolean(HerobrinePortalUtil.NBT_RISING)
                ? tag.getInt(HerobrinePortalUtil.NBT_TICKS)
                : tag.getInt(HerobrinePortalUtil.NBT_SINK_TICKS);
        // Keep the server's connection position authoritative while the lightweight
        // client packet supplies smooth positions on the intervening ticks.
        if (transitionTicks == 0 || transitionTicks % 4 == 0) {
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        }
        AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundPlayerGroundTransitionPosition(x, y, z));
    }

    private static void clearSavedTeleportState(CompoundTag tag) {
        tag.remove(NBT_SAVED_TELEPORT_PENDING);
        tag.remove(TAG_TELEPORT_ORIGIN_X);
        tag.remove(TAG_TELEPORT_ORIGIN_Y);
        tag.remove(TAG_TELEPORT_ORIGIN_Z);
        tag.remove(TAG_TELEPORT_TARGET_X);
        tag.remove(TAG_TELEPORT_TARGET_Y);
        tag.remove(TAG_TELEPORT_TARGET_Z);
        tag.remove(TAG_TELEPORT_ENTITIES);
        tag.remove(TAG_TELEPORT_EXECUTE_TICK);
    }

    private static void clearSinkState(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.remove(HerobrinePortalUtil.NBT_SINKING);
        tag.remove(HerobrinePortalUtil.NBT_SINK_TARGET_Y);
        tag.remove(HerobrinePortalUtil.NBT_SINK_SPEED);
        tag.remove(HerobrinePortalUtil.NBT_SINK_TICKS);
        tag.remove(HerobrinePortalUtil.NBT_SINK_MAX_TICKS);
    }

    private static void sendGroundPortalFx(Entity trackedEntity, Vec3 pos) {
        AnnoyingVillagers.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> trackedEntity),
                new ClientboundHerobrinePortalFx(pos)
        );
    }

    public static PortalSpawnBatch spawnPortalPairsBatch(Level level, LivingEntity caster) {
        return spawnPortalPairsBatch(level, caster, caster);
    }

    public static PortalSpawnBatch spawnPortalPairsBatch(Level level, LivingEntity caster, LivingEntity placementAnchor) {
        if (level instanceof ServerLevel serverLevel
                && findOwnedActivePortals(serverLevel, caster).size() + PORTAL_COUNT > MAX_ACTIVE_PORTALS_PER_OWNER) {
            return new PortalSpawnBatch(null, 0);
        }

        RandomSource random = level.getRandom();
        List<LivingEntity> priorityTargets = clusterPriorityTargets(findPriorityTargets(level, placementAnchor));
        List<Vec3> portalPositions = buildPortalPositions(level, caster, placementAnchor, priorityTargets, PORTAL_COUNT, random);
        UUID portalGroup = UUID.randomUUID();
        int spawned = 0;

        for (int order = 0; order < portalPositions.size(); order += 2) {
            if (order + 1 < portalPositions.size()
                    && spawnPair(level, caster, portalGroup, order, portalPositions.get(order), portalPositions.get(order + 1))) {
                spawned += 2;
            } else if (order == portalPositions.size() - 1
                    && spawnSinglePortal(level, caster, portalGroup, order, portalPositions.get(order), yawFacing(portalPositions.get(order), caster.getEyePosition()), order == 0)) {
                spawned++;
            }
        }

        return new PortalSpawnBatch(spawned > 0 ? portalGroup : null, spawned);
    }

    public static int spawnPortalPairs(Level level, LivingEntity caster) {
        return spawnPortalPairsBatch(level, caster).spawned();
    }

    public static int spawnLinkedPortalPair(Level level, LivingEntity caster, Vec3 firstPreferredPos, Vec3 secondPreferredPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        List<PortalEntity> activePortals = findOwnedActivePortals(serverLevel, caster);
        if (activePortals.size() + 2 > MAX_ACTIVE_PORTALS_PER_OWNER) {
            return 0;
        }

        Vec3 firstPortalPos = findLookPortalPosition(level, firstPreferredPos);
        Vec3 secondPortalPos = findLookPortalPosition(level, secondPreferredPos);
        if (firstPortalPos == null || secondPortalPos == null) {
            return 0;
        }

        UUID portalGroup = selectPortalGroup(activePortals, null);
        int portalOrder = nextPortalOrder(activePortals);
        return spawnPair(level, caster, portalGroup, portalOrder, firstPortalPos, secondPortalPos) ? 2 : 0;
    }

    public static boolean canSpawnOwnedPortals(ServerLevel level, LivingEntity caster, int portalCount) {
        return findOwnedActivePortals(level, caster).size() + portalCount <= MAX_ACTIVE_PORTALS_PER_OWNER;
    }

    private static int spawnLookPortal(ServerLevel level, Player caster, List<PortalEntity> activePortals) {
        return spawnLookPortal(level, caster, activePortals, null);
    }

    private static int spawnLookPortal(ServerLevel level, Player caster, List<PortalEntity> activePortals, Vec3 crosshairTarget) {
        LookPortalTarget target = findLookPortalTarget(level, caster, crosshairTarget);
        Vec3 portalPos = findLookPortalPosition(level, target.portalPos);
        if (portalPos == null) {
            return 0;
        }

        PortalEntity pendingPortal = findPendingPortal(activePortals);
        UUID portalGroup = selectPortalGroup(activePortals, pendingPortal);
        int portalOrder = nextPortalOrder(activePortals);
        float yaw = yawFacing(portalPos, target.facingTarget);
        PortalEntity portal = createPortal(level, caster, portalGroup, portalOrder, portalPos, yaw, portalOrder == 0);
        if (portal == null) {
            return 0;
        }

        if (pendingPortal != null) {
            linkPortalPair(pendingPortal, portal, portalGroup);
        }

        return 1;
    }

    private static LookPortalTarget findLookPortalTarget(ServerLevel level, Player caster) {
        return findLookPortalTarget(level, caster, null);
    }

    private static LookPortalTarget findLookPortalTarget(ServerLevel level, Player caster, Vec3 crosshairTarget) {
        Vec3 eyePos = caster.getEyePosition(1.0F);
        Vec3 maxPos = resolveLookEnd(eyePos, caster, crosshairTarget);
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos,
                maxPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        double blockDistanceSqr = blockHit.getType() == HitResult.Type.BLOCK
                ? eyePos.distanceToSqr(blockHit.getLocation())
                : LOOK_PORTAL_RANGE * LOOK_PORTAL_RANGE;

        LookEntityHit entityHit = findLookEntity(level, caster, eyePos, maxPos);
        if (entityHit != null && entityHit.distanceSqr <= blockDistanceSqr) {
            return new LookPortalTarget(new Vec3(entityHit.entity.getX(), entityHit.entity.getY(), entityHit.entity.getZ()), getEntityCenter(entityHit.entity));
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos spawnBlock = blockHit.getBlockPos().relative(blockHit.getDirection());
            return new LookPortalTarget(Vec3.atBottomCenterOf(spawnBlock), eyePos);
        }

        return new LookPortalTarget(maxPos, eyePos);
    }

    private static Vec3 resolveLookEnd(Vec3 eyePos, Player caster, Vec3 crosshairTarget) {
        if (crosshairTarget != null) {
            Vec3 offset = crosshairTarget.subtract(eyePos);
            double distanceSqr = offset.lengthSqr();
            if (distanceSqr > 1.0E-6D) {
                double distance = Math.sqrt(distanceSqr);
                double clampedDistance = Math.min(distance, LOOK_PORTAL_RANGE);
                return eyePos.add(offset.scale(clampedDistance / distance));
            }
        }

        return eyePos.add(caster.getLookAngle().scale(LOOK_PORTAL_RANGE));
    }

    private static LookEntityHit findLookEntity(ServerLevel level, Player caster, Vec3 start, Vec3 end) {
        AABB searchBox = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closestEntity = null;
        double closestDistanceSqr = LOOK_PORTAL_RANGE * LOOK_PORTAL_RANGE;

        for (Entity entity : level.getEntities(caster, searchBox, TransporterFragmentItem::canLookTargetEntity)) {
            AABB targetBox = entity.getBoundingBox().inflate(Math.max(0.3D, entity.getPickRadius()));
            Optional<Vec3> clip = targetBox.clip(start, end);
            Vec3 hitPos = null;

            if (targetBox.contains(start)) {
                hitPos = start;
            } else if (clip.isPresent()) {
                hitPos = clip.get();
            }

            if (hitPos == null) {
                continue;
            }

            double distanceSqr = start.distanceToSqr(hitPos);
            if (distanceSqr < closestDistanceSqr) {
                closestEntity = entity;
                closestDistanceSqr = distanceSqr;
            }
        }

        return closestEntity == null ? null : new LookEntityHit(closestEntity, closestDistanceSqr);
    }

    private static boolean canLookTargetEntity(Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && entity.isPickable();
    }

    private static Vec3 findLookPortalPosition(Level level, Vec3 preferredPos) {
        Vec3 base = snapPortalPosition(preferredPos);
        if (isLookPortalPositionValid(level, base)) {
            return base;
        }

        for (int radius = 1; radius <= 3; radius++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }

                        Vec3 candidate = base.add(dx, dy, dz);
                        if (isLookPortalPositionValid(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static Vec3 snapPortalPosition(Vec3 pos) {
        return new Vec3(Math.floor(pos.x) + 0.5D, Math.floor(pos.y), Math.floor(pos.z) + 0.5D);
    }

    private static List<LivingEntity> findPriorityTargets(Level level, LivingEntity attacker) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(TARGET_PRIORITY_RADIUS))) {
            if (!entity.equals(attacker)
                    && !attacker.isAlliedTo(entity)
                    && !entity.isAlliedTo(attacker)
                    && !entity.isSpectator()
                    && !(entity instanceof Player player && player.isCreative())
                    && (entity instanceof Mob || entity instanceof Player)
                    && attacker.hasLineOfSight(entity)) {
                targets.add((LivingEntity) entity);
            }
        }

        targets.sort(Comparator.comparingDouble(attacker::distanceTo));
        return targets;
    }

    private static List<LivingEntity> clusterPriorityTargets(List<LivingEntity> targets) {
        List<LivingEntity> clusteredTargets = new ArrayList<>();

        for (LivingEntity target : targets) {
            boolean joinedExistingCluster = false;
            for (LivingEntity clusteredTarget : clusteredTargets) {
                if (target.distanceTo(clusteredTarget) <= TARGET_CLUSTER_DISTANCE) {
                    joinedExistingCluster = true;
                    break;
                }
            }

            if (!joinedExistingCluster) {
                clusteredTargets.add(target);
            }
        }

        return clusteredTargets;
    }

    private static List<Vec3> buildPortalPositions(
            Level level,
            LivingEntity owner,
            LivingEntity placementAnchor,
            List<LivingEntity> priorityTargets,
            int portalCount,
            RandomSource random
    ) {
        List<Vec3> positions = new ArrayList<>();
        Vec3 casterPortal = findCasterPortalPosition(level, owner, placementAnchor, positions, random);
        if (casterPortal == null) {
            return positions;
        }

        positions.add(casterPortal);

        int targetIndex = 0;
        while (positions.size() < portalCount) {
            Vec3 candidate = null;

            boolean exitSlot = positions.size() % 2 == 1;
            if (exitSlot && targetIndex < priorityTargets.size()) {
                candidate = findPortalNearTarget(level, owner, placementAnchor, priorityTargets.get(targetIndex), positions, random);
                targetIndex++;
            }

            if (candidate == null) {
                candidate = findRandomDistributedPortal(level, owner, placementAnchor, positions, random, positions.size());
            }

            if (candidate == null) {
                break;
            }

            positions.add(candidate);
        }

        return positions;
    }

    private static Vec3 findCasterPortalPosition(Level level, LivingEntity owner, LivingEntity placementAnchor, List<Vec3> usedPositions, RandomSource random) {
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = placementAnchor.getYRot() * Mth.DEG_TO_RAD + (attempt < 8
                    ? (Math.PI * 2.0D / 8.0D) * attempt
                    : random.nextDouble() * Math.PI * 2.0D);
            double distance = CASTER_PORTAL_MIN_DISTANCE + random.nextDouble() * (CASTER_PORTAL_MAX_DISTANCE - CASTER_PORTAL_MIN_DISTANCE);
            double y = Math.floor(placementAnchor.getY()) + (attempt > 30 ? random.nextInt(VERTICAL_SEARCH_RADIUS + 1) : random.nextInt(4));
            Vec3 candidate = new Vec3(
                    placementAnchor.getX() - Math.sin(angle) * distance,
                    Mth.clamp(y, Math.floor(placementAnchor.getY()), Math.floor(placementAnchor.getY()) + VERTICAL_SEARCH_RADIUS),
                    placementAnchor.getZ() + Math.cos(angle) * distance
            );

            if (isValidPortalPosition(level, placementAnchor, candidate, usedPositions)) {
                return candidate;
            }
        }

        return findRandomDistributedPortal(level, owner, placementAnchor, usedPositions, random, 0);
    }

    private static Vec3 findPortalNearTarget(
            Level level,
            LivingEntity owner,
            LivingEntity placementAnchor,
            LivingEntity target,
            List<Vec3> usedPositions,
            RandomSource random
    ) {
        for (int attempt = 0; attempt < 32; attempt++) {
            Vec3 candidate = randomPositionNearEntity(placementAnchor, target, random);
            if (isValidPortalPosition(level, placementAnchor, candidate, usedPositions)) {
                return candidate;
            }
        }

        return null;
    }

    private static Vec3 findRandomDistributedPortal(
            Level level,
            LivingEntity owner,
            LivingEntity placementAnchor,
            List<Vec3> usedPositions,
            RandomSource random,
            int slotIndex
    ) {
        for (int attempt = 0; attempt < 140; attempt++) {
            Vec3 candidate = randomDistributedPositionAroundCaster(placementAnchor, usedPositions, random, slotIndex, attempt);
            if (isValidPortalPosition(level, placementAnchor, candidate, usedPositions)) {
                return candidate;
            }
        }

        for (int attempt = 0; attempt < 120; attempt++) {
            Vec3 candidate = randomPositionAroundCaster(placementAnchor, random);
            if (isValidPortalPosition(level, placementAnchor, candidate, usedPositions)) {
                return candidate;
            }
        }

        return null;
    }

    private static Vec3 randomDistributedPositionAroundCaster(
            LivingEntity caster,
            List<Vec3> usedPositions,
            RandomSource random,
            int slotIndex,
            int attempt
    ) {
        double angle = preferredSpreadAngle(caster, usedPositions, random, attempt);
        double angleJitter = attempt < 60 ? 0.35D : 0.95D;
        angle += (random.nextDouble() - 0.5D) * angleJitter;

        int distanceTier = slotIndex <= 1 ? 0 : (slotIndex + attempt) % 3;
        double distance;
        if (attempt > 95) {
            distance = 8.0D + random.nextDouble() * 22.0D;
        } else if (distanceTier == 0) {
            distance = DISTRIBUTED_PORTAL_NEAR_MIN_DISTANCE
                    + random.nextDouble() * (DISTRIBUTED_PORTAL_NEAR_MAX_DISTANCE - DISTRIBUTED_PORTAL_NEAR_MIN_DISTANCE);
        } else if (distanceTier == 1) {
            distance = DISTRIBUTED_PORTAL_MID_MIN_DISTANCE
                    + random.nextDouble() * (DISTRIBUTED_PORTAL_MID_MAX_DISTANCE - DISTRIBUTED_PORTAL_MID_MIN_DISTANCE);
        } else {
            distance = DISTRIBUTED_PORTAL_FAR_MIN_DISTANCE
                    + random.nextDouble() * (DISTRIBUTED_PORTAL_FAR_MAX_DISTANCE - DISTRIBUTED_PORTAL_FAR_MIN_DISTANCE);
        }

        double y = Math.floor(caster.getY()) + random.nextInt(VERTICAL_SEARCH_RADIUS + 1);

        return new Vec3(
                caster.getX() + Math.cos(angle) * distance,
                Mth.clamp(y, Math.floor(caster.getY()), Math.floor(caster.getY()) + VERTICAL_SEARCH_RADIUS),
                caster.getZ() + Math.sin(angle) * distance
        );
    }

    private static double preferredSpreadAngle(LivingEntity caster, List<Vec3> usedPositions, RandomSource random, int attempt) {
        if (usedPositions.isEmpty()) {
            return random.nextDouble() * Math.PI * 2.0D;
        }

        List<Double> angles = new ArrayList<>(usedPositions.size());
        for (Vec3 used : usedPositions) {
            double angle = Math.atan2(used.z - caster.getZ(), used.x - caster.getX());
            if (angle < 0.0D) {
                angle += Math.PI * 2.0D;
            }
            angles.add(angle);
        }
        angles.sort(Double::compareTo);

        double bestStart = angles.get(0);
        double bestGap = -1.0D;
        for (int index = 0; index < angles.size(); index++) {
            double start = angles.get(index);
            double end = index == angles.size() - 1 ? angles.get(0) + Math.PI * 2.0D : angles.get(index + 1);
            double gap = end - start;
            if (gap > bestGap) {
                bestGap = gap;
                bestStart = start;
            }
        }

        double midpoint = bestStart + bestGap * 0.5D;
        if (attempt > 40) {
            midpoint += (attempt % 6) * (Math.PI / 12.0D);
        }

        midpoint %= Math.PI * 2.0D;
        return midpoint < 0.0D ? midpoint + Math.PI * 2.0D : midpoint;
    }

    private static Vec3 randomPositionNearEntity(LivingEntity caster, LivingEntity target, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_PORTAL_GAP + random.nextDouble() * (MAX_PORTAL_GAP - MIN_PORTAL_GAP);
        double y = Math.max(Math.floor(caster.getY()), Math.floor(target.getY()));

        return new Vec3(
                target.getX() + Math.cos(angle) * distance,
                Mth.clamp(y, Math.floor(caster.getY()), Math.floor(caster.getY()) + VERTICAL_SEARCH_RADIUS),
                target.getZ() + Math.sin(angle) * distance
        );
    }

    private static Vec3 randomPositionAroundCaster(LivingEntity caster, RandomSource random) {
        return new Vec3(
                caster.getX() + random.nextInt(HORIZONTAL_SEARCH_RADIUS * 2 + 1) - HORIZONTAL_SEARCH_RADIUS,
                Math.floor(caster.getY()) + random.nextInt(VERTICAL_SEARCH_RADIUS + 1),
                caster.getZ() + random.nextInt(HORIZONTAL_SEARCH_RADIUS * 2 + 1) - HORIZONTAL_SEARCH_RADIUS
        );
    }

    private static boolean isValidPortalPosition(Level level, LivingEntity caster, Vec3 pos, List<Vec3> usedPositions) {
        if (pos.y < Math.floor(caster.getY()) || pos.y > Math.floor(caster.getY()) + VERTICAL_SEARCH_RADIUS) {
            return false;
        }
        if (Math.abs(pos.x - caster.getX()) > HORIZONTAL_SEARCH_RADIUS || Math.abs(pos.z - caster.getZ()) > HORIZONTAL_SEARCH_RADIUS) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(pos))) {
            return false;
        }
        if (!isFarEnoughFromExisting(pos, usedPositions)) {
            return false;
        }
        return isAreaClear(level, pos);
    }

    private static boolean isLookPortalPositionValid(Level level, Vec3 pos) {
        return level.getWorldBorder().isWithinBounds(BlockPos.containing(pos)) && isAreaClear(level, pos);
    }

    private static boolean isFarEnoughFromExisting(Vec3 pos, List<Vec3> usedPositions) {
        for (Vec3 used : usedPositions) {
            if (used.distanceTo(pos) < MIN_PORTAL_GAP) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAreaClear(Level level, Vec3 pos) {
        if (pos.y < level.getMinBuildHeight() || pos.y + PortalEntity.HEIGHT + 1.0D >= level.getMaxBuildHeight()) {
            return false;
        }

        AABB portalBox = new AABB(
                pos.x - PortalEntity.WIDTH * 0.5D,
                pos.y,
                pos.z - PortalEntity.WIDTH * 0.5D,
                pos.x + PortalEntity.WIDTH * 0.5D,
                pos.y + PortalEntity.HEIGHT,
                pos.z + PortalEntity.WIDTH * 0.5D
        );
        if (!level.noCollision(portalBox)) {
            return false;
        }

        BlockPos min = BlockPos.containing(pos.x - 2.0D, pos.y, pos.z - 2.0D);
        BlockPos max = BlockPos.containing(pos.x + 2.0D, pos.y + PortalEntity.HEIGHT, pos.z + 2.0D);
        for (BlockPos checkPos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() || !level.getFluidState(checkPos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static boolean spawnSinglePortal(Level level, LivingEntity caster, UUID portalGroup, int order, Vec3 pos, float yaw, boolean starterPortal) {
        return createPortal(level, caster, portalGroup, order, pos, yaw, starterPortal) != null;
    }

    private static PortalEntity createPortal(Level level, LivingEntity caster, UUID portalGroup, int order, Vec3 pos, float yaw, boolean starterPortal) {
        PortalEntity portal = AnnoyingVillagersModEntities.PORTAL.get().create(level);
        if (portal == null) {
            return null;
        }

        portal.setOwnerUUID(caster.getUUID());
        portal.setPortalGroupUUID(portalGroup);
        portal.setPortalOrder(order);
        portal.setStarterPortal(starterPortal);

        placePortal(portal, pos, yaw);
        level.addFreshEntity(portal);
        return portal;
    }

    private static boolean spawnPair(Level level, LivingEntity caster, UUID portalGroup, int firstOrder, Vec3 firstPos, Vec3 secondPos) {
        PortalEntity first = AnnoyingVillagersModEntities.PORTAL.get().create(level);
        PortalEntity second = AnnoyingVillagersModEntities.PORTAL.get().create(level);
        if (first == null || second == null) {
            return false;
        }

        UUID owner = caster.getUUID();
        float firstYaw = yawFacing(firstPos, secondPos);
        float secondYaw = yawFacing(secondPos, firstPos);

        first.setOwnerUUID(owner);
        second.setOwnerUUID(owner);
        first.setLinkedPortalUUID(second.getUUID());
        second.setLinkedPortalUUID(first.getUUID());
        first.setPortalGroupUUID(portalGroup);
        second.setPortalGroupUUID(portalGroup);
        first.setPortalOrder(firstOrder);
        second.setPortalOrder(firstOrder + 1);
        first.setStarterPortal(firstOrder == 0);
        second.setStarterPortal(false);

        placePortal(first, firstPos, firstYaw);
        placePortal(second, secondPos, secondYaw);

        level.addFreshEntity(first);
        level.addFreshEntity(second);
        return true;
    }

    private static void placePortal(PortalEntity portal, Vec3 pos, float yaw) {
        portal.setPos(pos.x, pos.y, pos.z);
        setPortalYaw(portal, yaw);
    }

    private static void setPortalYaw(PortalEntity portal, float yaw) {
        portal.setYRot(yaw);
        portal.yRotO = yaw;
    }

    private static float yawFacing(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        return (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
    }

    private static Vec3 getEntityCenter(Entity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
    }

    private static List<PortalEntity> findOwnedActivePortals(ServerLevel level, LivingEntity caster) {
        List<PortalEntity> portals = new ArrayList<>();
        UUID owner = caster.getUUID();

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof PortalEntity portal
                    && !portal.isRemoved()
                    && portal.isAlive()
                    && portal.tickCount < PortalEntity.LIFETIME_TICKS
                    && owner.equals(portal.getOwnerUUID())) {
                portals.add(portal);
            }
        }

        portals.sort(Comparator
                .comparingInt(PortalEntity::getPortalOrder)
                .thenComparingInt(Entity::getId));
        return portals;
    }

    private static PortalEntity findPendingPortal(List<PortalEntity> activePortals) {
        PortalEntity pendingPortal = null;
        for (PortalEntity portal : activePortals) {
            if (portal.getLinkedPortalUUID() != null) {
                continue;
            }

            if (pendingPortal == null || portal.getPortalOrder() > pendingPortal.getPortalOrder()) {
                pendingPortal = portal;
            }
        }
        return pendingPortal;
    }

    private static UUID selectPortalGroup(List<PortalEntity> activePortals, PortalEntity pendingPortal) {
        if (pendingPortal != null && pendingPortal.getPortalGroupUUID() != null) {
            return pendingPortal.getPortalGroupUUID();
        }

        for (int i = activePortals.size() - 1; i >= 0; i--) {
            UUID portalGroup = activePortals.get(i).getPortalGroupUUID();
            if (portalGroup != null) {
                return portalGroup;
            }
        }

        return UUID.randomUUID();
    }

    private static int nextPortalOrder(List<PortalEntity> activePortals) {
        int nextOrder = 0;
        for (PortalEntity portal : activePortals) {
            nextOrder = Math.max(nextOrder, portal.getPortalOrder() + 1);
        }
        return nextOrder;
    }

    private static void linkPortalPair(PortalEntity first, PortalEntity second, UUID portalGroup) {
        first.setLinkedPortalUUID(second.getUUID());
        second.setLinkedPortalUUID(first.getUUID());
        first.setPortalGroupUUID(portalGroup);
        second.setPortalGroupUUID(portalGroup);
        setPortalYaw(first, yawFacing(first.position(), second.position()));
        setPortalYaw(second, yawFacing(second.position(), first.position()));
    }

    private static final class LookPortalTarget {
        private final Vec3 portalPos;
        private final Vec3 facingTarget;

        private LookPortalTarget(Vec3 portalPos, Vec3 facingTarget) {
            this.portalPos = portalPos;
            this.facingTarget = facingTarget;
        }
    }

    private static final class LookEntityHit {
        private final Entity entity;
        private final double distanceSqr;

        private LookEntityHit(Entity entity, double distanceSqr) {
            this.entity = entity;
            this.distanceSqr = distanceSqr;
        }
    }

    public enum UseMode {
        NONE,
        MAIN_HAND,
        OFF_HAND,
        BOTH_HANDS
    }

    public static final class UseResult {
        private static final UseResult MISSED = new UseResult(false, false, UseMode.NONE);

        private final boolean consumed;
        private final boolean activated;
        private final UseMode mode;

        private UseResult(boolean consumed, boolean activated, UseMode mode) {
            this.consumed = consumed;
            this.activated = activated;
            this.mode = mode;
        }

        public static UseResult missed() {
            return MISSED;
        }

        public static UseResult consumed(UseMode mode, boolean activated) {
            return new UseResult(true, activated, mode);
        }

        public boolean consumed() {
            return this.consumed;
        }

        public boolean activated() {
            return this.activated;
        }

        public UseMode mode() {
            return this.mode;
        }
    }
}
