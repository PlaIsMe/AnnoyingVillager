package com.pla.annoyingvillagers.entity;

import javax.annotation.Nullable;

import com.pla.annoyingvillagers.clazz.BurstProtectEntity;
import com.pla.annoyingvillagers.clazz.DangerousReaction;
import com.pla.annoyingvillagers.clazz.PersistentPlayerNpc;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.item.HookGunItem;
import com.pla.annoyingvillagers.spawnhandler.AlexData;
import com.pla.annoyingvillagers.util.*;
import com.pla.annoyingvillagers.clazz.AVNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;


public class AlexEntity extends AVNpc implements PersistentPlayerNpc, BurstProtectEntity, DangerousReaction {
    private JevEntity jevToProtect;
    private UUID jevUUID;
    private boolean spawnJev = false;
    private int state = 0;
    private ItemStack currentBoundHook = ItemStack.EMPTY;

    @Override
    public float getBurstProtectCapRatio() {
        return 0.15F;
    }

    public void setProtectingJev(JevEntity jev) {
        this.jevToProtect = jev;
    }

    public JevEntity getProtectingJev() {
        return jevToProtect;
    }

    public void setJevUUID(UUID jevUUID) {
        this.jevUUID = jevUUID;
    }

    @Override
    public String persistentPlayerIdentity() { return "Alex"; }

    public AlexEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.ALEX.get(), level);
    }

    public int getState() {
        return state;
    }

    public ItemStack getCurrentBoundHook() {
        if (currentBoundHook.isEmpty()) {
            currentBoundHook = HookGunCombatUtil.createAlexDefaultPickaxe();
        }
        return currentBoundHook.copy();
    }

    public void setCurrentBoundHook(ItemStack currentBoundHook) {
        if (currentBoundHook.isEmpty()) {
            this.currentBoundHook = ItemStack.EMPTY;
        } else {
            ItemStack stored = currentBoundHook.copy();
            stored.setCount(1);
            this.currentBoundHook = stored;
        }
    }

    public boolean canDualHookInSecondPhase() {
        return this.state == 1 && this.hasHookGunInInventory();
    }

    public AlexEntity(EntityType<AlexEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 60;
        this.setNoAi(false);
        this.setCustomName(Component.translatable(this.getType().getDescriptionId()));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setPlaceBlockToParryChance(0.7);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> jevToProtect != null
                && jevToProtect.isAlive()
                && target != null
                && target.getLastHurtMob() == jevToProtect));
        CommonGoals.registerDangerousReactionGoals(this);
        CommonGoals.registerGoalForNeutralNpc(this);
    }

    public void setState(int state) {
        this.state = state;
        if (state == 1 && this.jevToProtect != null && this.level() instanceof ServerLevel) {
            jevToProtect.playSound(AnnoyingVillagersModSounds.JEV_SAY_WHEN_ALEX_SECOND_PHASE.get(),
                    0.5F, 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (jevUUID != null) {
            tag.putUUID("JevUUID", jevUUID);
        }
        tag.putInt("State", this.state);
        tag.putBoolean("SpawnJev", spawnJev);
        if (!this.currentBoundHook.isEmpty()) {
            CompoundTag hookTag = new CompoundTag();
            this.currentBoundHook.save(hookTag);
            tag.put("CurrentBoundHook", hookTag);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("JevUUID")) {
            jevUUID = tag.getUUID("JevUUID");
        }
        state = tag.getInt("State");
        spawnJev = tag.getBoolean("SpawnJev");
        if (tag.contains("CurrentBoundHook", 10)) {
            currentBoundHook = ItemStack.of(tag.getCompound("CurrentBoundHook"));
        } else {
            currentBoundHook = HookGunCombatUtil.createAlexDefaultPickaxe();
        }
    }

    @Override
    public SoundEvent getAttackVoiceSound() {
        return AnnoyingVillagersModSounds.ALEX_SAY.get();
    }

    public @NotNull MobType getMobType() {
        return MobType.UNDEFINED;
    }

    public boolean removeWhenFarAway(double d0) {
        return false;
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    public SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.hurt"));
    }

    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.death"));
    }

    @Override
    protected boolean afterBurstProtection(@NotNull ServerLevel serverLevel,
                                           @NotNull DamageSource source,
                                           float finalDamage) {
        if (this.state == 0
                && (this.getHealth() - finalDamage) <= 1.0F
                && !this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            this.setHealth(1.0F);
            return true;
        }
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.level() instanceof ServerLevel serverLevel) {
            final double x = this.getX();
            final double y = this.getY() + 1.0D;
            final double z = this.getZ();

            Consumer<ItemStack> dropStack = (stack) -> {
                if (InventoryUtils.isInventoryBackedSupplyDrop(stack)) {
                    return;
                }
                ItemEntity drop = new ItemEntity(serverLevel, x, y, z, stack);
                drop.setPickUpDelay(10);
                serverLevel.addFreshEntity(drop);
            };

            List<ItemStack> damagedStacks = new ArrayList<>();

            ItemStack sword = new ItemStack(AnnoyingVillagersModItems.THUNDER_DIAMOND_BLADE.get());
            sword.enchant(Enchantments.SHARPNESS, 5);
            sword.enchant(Enchantments.FIRE_ASPECT, 2);
            sword.enchant(Enchantments.KNOCKBACK, 2);
            sword.enchant(Enchantments.UNBREAKING, 5);
            damagedStacks.add(sword);

            ItemStack bow = this.getBowItem();
            bow.enchant(Enchantments.PUNCH_ARROWS, 3);
            bow.enchant(Enchantments.POWER_ARROWS, 3);
            bow.enchant(Enchantments.FLAMING_ARROWS, 2);
            damagedStacks.add(bow);

            for (ItemStack stack : damagedStacks) {
                stack.setDamageValue(CommonUtil.getRandomDamage(stack));
                dropStack.accept(stack);
            }

            dropStack.accept(HookGunCombatUtil.createBoundHookGun(this.getCurrentBoundHook()));
            dropStack.accept(this.getCurrentBoundHook());
        }
    }

    private void spawnJev() {
        if (this.level() instanceof ServerLevel serverLevel) {
            JevEntity jevEntity = new JevEntity(AnnoyingVillagersModEntities.JEV.get(), serverLevel);
            jevEntity.moveTo(this.getX() + new Random().nextDouble(1.0D, 10.0D), this.getY() + new Random().nextDouble(1.0D, 10.0D), this.getZ() + new Random().nextDouble(1.0D, 10.0D), serverLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            jevEntity.setFollowTarget(this);
            jevEntity.setFollowTargetUUID(this.getUUID());
            jevEntity.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            serverLevel.addFreshEntity(jevEntity);

            this.setJevUUID(jevEntity.getUUID());
            this.setProtectingJev(jevEntity);
        }
    }

    @Override
    protected void actuallyHurt(@NotNull DamageSource pDamageSource, float pDamageAmount) {
        if (pDamageSource.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            super.actuallyHurt(pDamageSource, pDamageAmount);
            return;
        }

        if (this.isInvulnerableTo(pDamageSource)) {
            return;
        }

        pDamageAmount = ForgeHooks.onLivingHurt(this, pDamageSource, pDamageAmount);
        if (pDamageAmount <= 0.0F) {
            return;
        }

        pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSource, pDamageAmount);
        pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSource, pDamageAmount);

        float finalDamage = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0F);
        float absorbed = pDamageAmount - finalDamage;
        if (absorbed > 0.0F) {
            this.setAbsorptionAmount(this.getAbsorptionAmount() - absorbed);
            if (this.getAbsorptionAmount() < 0.0F) {
                this.setAbsorptionAmount(0.0F);
            }
        }

        finalDamage = ForgeHooks.onLivingDamage(this, pDamageSource, finalDamage);
        finalDamage = this.applyBurstProtection(this, pDamageSource, finalDamage);

        if (this.level() instanceof ServerLevel serverLevel
                && this.afterBurstProtection(serverLevel, pDamageSource, finalDamage)) {
            return;
        }

        if (finalDamage <= 0.0F) {
            return;
        }

        this.getCombatTracker().recordDamage(pDamageSource, finalDamage);
        this.setHealth(this.getHealth() - finalDamage);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor serverLevelAccessor, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawngroupdata, @Nullable CompoundTag compoundtag) {
        if (mobSpawnType == MobSpawnType.NATURAL || mobSpawnType == MobSpawnType.CHUNK_GENERATION) {
            ServerLevel serverLevel = serverLevelAccessor.getLevel();
            AlexData alexData = AlexData.get(serverLevel);

            if (!alexData.tryClaim(serverLevel, this.getUUID())) {
                this.discard();
                return null;
            }
        }

        SpawnGroupData returnSpawnGroupData = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawngroupdata, compoundtag);
        TeamUtil.addOrJoinTeam(this, "alex");

        ItemStack sword = new ItemStack(AnnoyingVillagersModItems.THUNDER_DIAMOND_BLADE.get());
        sword.enchant(Enchantments.SHARPNESS, 5);
        sword.enchant(Enchantments.FIRE_ASPECT, 2);
        sword.enchant(Enchantments.KNOCKBACK, 2);
        sword.enchant(Enchantments.UNBREAKING, 5);
        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ENDER_PEARL));
        this.setMainWeaponItem(sword);
        this.setOffWeaponItem(new ItemStack(Items.ENDER_PEARL));
        this.setCurrentBoundHook(HookGunCombatUtil.createAlexDefaultPickaxe());
        return returnSpawnGroupData;
    }

    @Override
    protected boolean seedInventory() {
        if (super.seedInventory()) {
            Random random = new Random();
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.GOLDEN_APPLE, random.nextInt(16, 32)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, random.nextInt(16, 32)));

            List<ItemLike> foods = new ArrayList<>(REGULAR_FOODS);
            for (int i = 0; i < 2 && !foods.isEmpty(); i++) {
                ItemLike food = foods.remove(random.nextInt(foods.size()));
                InventoryUtils.addItem(this.inventory, new ItemStack(food, random.nextInt(16, 32)));
            }

            InventoryUtils.addItem(this.inventory, new ItemStack(Items.ARROW, random.nextInt(32, 64)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.ENDER_PEARL, random.nextInt(16, 32)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.WATER_BUCKET));
            if (this.isVillagerKnight() && random.nextFloat() < 0.45F) {
                InventoryUtils.addItem(this.inventory, new ItemStack(Items.LAVA_BUCKET));
            }

            List<ItemLike> blocks = new ArrayList<>(PLACEABLE_BLOCKS);
            int blockStacks = random.nextInt(1, 2);
            for (int i = 0; i < blockStacks && !blocks.isEmpty(); i++) {
                ItemLike block = blocks.remove(random.nextInt(blocks.size()));
                InventoryUtils.addItem(this.inventory, new ItemStack(block, random.nextInt(64, 128)));
            }

            InventoryUtils.addItem(this.inventory, new ItemStack(Items.COAL, random.nextInt(0, 8)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.IRON_INGOT, random.nextInt(0, 12)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.GOLD_INGOT, random.nextInt(0, 12)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.DIAMOND, random.nextInt(0, 8)));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!this.level().isClientSide) {
            HookGunCombatUtil.onAlexDeath(this);
        }
        super.die(damageSource);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            HookGunCombatUtil.tickAlex(this, serverLevel);
            if (!spawnJev) {
                this.spawnJev = true;
                spawnJev();
            }
            if (jevToProtect == null && jevUUID != null) {
                Entity entity = ((ServerLevel) level()).getEntity(jevUUID);
                if (entity instanceof JevEntity jev) {
                    jevToProtect = jev;
                } else {
                    jevUUID = null;
                }
            }
            if (jevToProtect != null && !jevToProtect.isAlive()) {
                jevToProtect = null;
                jevUUID = null;
            }
            if (this.state == 0
                    && this.getHealth() <= 20
                    && !this.getItemInHand(InteractionHand.OFF_HAND).getItem().equals(Items.TOTEM_OF_UNDYING)) {
                this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        }
    }

    public boolean canStoreInInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = this.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                return true;
            }

            if (ItemStack.isSameItemSameTags(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasHookGunInInventory() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.getItem() instanceof HookGunItem) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSpawn(EntityType<AlexEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        if (AlexData.get(serverLevel).isOccupied(serverLevel)) {
            return false;
        }
        return PathfinderMob.checkMobSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            AlexData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
        }
    }

    public static Builder addEpicFightAttributes(Builder builder) {
//      ADD THIS CODE IN AV_EFM
//        return builder.add(EpicFightAttributes.IMPACT.get(), 2.0D)
//                .add(EpicFightAttributes.ARMOR_NEGATION.get(), 5.0D)
//                .add(EpicFightAttributes.STUN_ARMOR.get(), 20.0D)
//                .add(EpicFightAttributes.MAX_STRIKES.get(), 50.0D)
//                .add(EpicFightAttributes.MAX_STAMINA.get(), 30.0D)
//                .add(EpicFightAttributes.STAMINA_REGEN.get(), 1.5D);

        return builder;
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        return addEpicFightAttributes(builder);
    }
}
