package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.*;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.rig.RigAnimationController;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.spawnhandler.SteveData;
import com.pla.annoyingvillagers.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public class AngrySteveEntity extends AVNpc implements PersistentPlayerNpc, BurstProtectEntity, RollItemUser
        , FishingRodUser, DangerousReaction {
    private static final int LEGENDARY_AWAKEN_DURATION = 20 * 30;
    private static final EntityDataAccessor<Integer> LEGENDARY_AWAKENED =
            SynchedEntityData.defineId(AngrySteveEntity.class, EntityDataSerializers.INT);
    private final FishingRodUser.State combatFishingRodState = new FishingRodUser.State();
    private boolean neverLeave = false;
    private int leaveTicks = 0;
    private int swapWeaponCooldown;
    private int legendaryAwakenTicks;

    public void setLeaveTicks(int leaveTicks) {
        this.leaveTicks = leaveTicks;
    }

    public int getLeaveTicks() {
        return leaveTicks;
    }

    public void setNeverLeave(boolean neverLeave) {
        this.neverLeave = neverLeave;
    }

    @Override
    public String persistentPlayerIdentity() { return "Steve"; }

    @Override
    public com.pla.annoyingvillagers.util.NpcTabSkin tabSkin() {
        return com.pla.annoyingvillagers.util.NpcTabSkin.ANGRY_STEVE;
    }

    public AngrySteveEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.ANGRY_STEVE.get(), level);
    }

    public AngrySteveEntity(EntityType<AngrySteveEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 8;
        this.setNoAi(false);
        this.setCustomName(this.getDisplayName());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setPlaceBlockToParryChance(1.0);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LEGENDARY_AWAKENED, 0);
    }

    public int getLegendaryAwakened() {
        return this.entityData.get(LEGENDARY_AWAKENED);
    }

    public boolean isLegendaryAwakened() {
        return this.getLegendaryAwakened() == 1;
    }

    public void setLegendaryAwakened(int legendaryAwakened) {
        this.entityData.set(LEGENDARY_AWAKENED, legendaryAwakened);
    }

    public void startLegendaryAwakening() {
        if (!(this.level() instanceof ServerLevel) || this.isLegendaryAwakened()) return;

        this.setLegendaryAwakened(1);
        this.legendaryAwakenTicks = LEGENDARY_AWAKEN_DURATION;
    }

    private void tickLegendaryAwakening() {
        if (this.legendaryAwakenTicks <= 0) return;

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 2, false, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, 2, false, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 2, false, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, 2, false, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2, 0, false, false, true));

        this.legendaryAwakenTicks--;
        if (this.legendaryAwakenTicks == 0) this.setLegendaryAwakened(0);
    }

    @Override
    public boolean canRollItem() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.getBlockDamage() == null && !this.isCombatFishingRodSessionActive() && this.swapWeaponCooldown == 0;
    }

    @Override
    public void rollItem() {
        if (this.getRandom().nextBoolean()) {
            this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get()));
        } else {
            ItemStack woopieTheSword = new ItemStack(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
            woopieTheSword.enchant(Enchantments.SHARPNESS, 5);
            woopieTheSword.enchant(Enchantments.SMITE, 5);
            woopieTheSword.enchant(Enchantments.SWEEPING_EDGE, 5);
            this.setItemInHand(InteractionHand.OFF_HAND, woopieTheSword);
        }

        this.setMainWeaponItem(this.getMainHandItem().copy());
        this.setOffWeaponItem(this.getOffhandItem().copy());
        this.swapWeaponCooldown = new Random().nextInt(100, 200);
    }

    @Override
    public FishingRodUser.State getCombatFishingRodState() {
        return this.combatFishingRodState;
    }

    @Override
    public Item getCombatFishingRodItem() {
        return AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get();
    }

    protected void registerGoals() {
        super.registerGoals();
        CommonGoals.registerDangerousReactionGoals(this);
        CommonGoals.registerGoalForCrazyNpc(this);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        leaveTicks = pCompound.getInt("LeaveTicks");
        neverLeave = pCompound.getBoolean("NeverLeave");
        swapWeaponCooldown = pCompound.getInt("SwapWeaponCooldown");
        legendaryAwakenTicks = pCompound.getInt("LegendaryAwakenTicks");
        this.setLegendaryAwakened(legendaryAwakenTicks > 0 && pCompound.getInt("LegendaryAwakened") != 0 ? 1 : 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("LeaveTicks", leaveTicks);
        pCompound.putBoolean("NeverLeave", neverLeave);
        pCompound.putInt("SwapWeaponCooldown", swapWeaponCooldown);
        pCompound.putInt("LegendaryAwakenTicks", legendaryAwakenTicks);
        pCompound.putInt("LegendaryAwakened", this.getLegendaryAwakened());
    }

    @Override
    public @Nullable SoundEvent getAttackVoiceSound() {
        return AnnoyingVillagersModSounds.ANGRY_STEVE_SAY.get();
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
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.hurt"));
    }

    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.death"));
    }

    @Override
    public float getBurstProtectCapRatio() {
        return 0.05F;
    }

    public boolean canBeAffected(MobEffectInstance mobeffectinstance) {
        return (mobeffectinstance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL || mobeffectinstance.getEffect() == MobEffects.GLOWING) && super.canBeAffected(mobeffectinstance);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (!this.level().isClientSide() && pEntity instanceof LivingEntity living) {
            ArmorUtil.damageArmor(living, new Random().nextInt(1, 5));
        }
        return super.doHurtTarget(pEntity);
    }

    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel) {
            if (AnnoyingVillagersConfig.TURN_ON_NPC_VOICE.get()) {
                this.playSound(AnnoyingVillagersModSounds.STEVE_SAY_ON_DEATH.get(), 0.5F, 1.0F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final double x = this.getX();
        final double y = this.getY() + 1.0D;
        final double z = this.getZ();

        Consumer<ItemStack> dropStack = (stack) -> {
            ItemEntity drop = new ItemEntity(serverLevel, x, y, z, stack);
            drop.setPickUpDelay(10);
            serverLevel.addFreshEntity(drop);
        };

        List<ItemStack> damagedStacks = new ArrayList<>();

        ItemStack compressedDiamondHelmet = new ItemStack(AnnoyingVillagersModItems.COMPRESSED_DIAMOND_HELMET.get());
        compressedDiamondHelmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
        compressedDiamondHelmet.enchant(Enchantments.PROJECTILE_PROTECTION, 5);
        compressedDiamondHelmet.enchant(Enchantments.FIRE_PROTECTION, 5);
        compressedDiamondHelmet.enchant(Enchantments.BLAST_PROTECTION, 5);
        damagedStacks.add(compressedDiamondHelmet);

        ItemStack compressedDiamondChestplate = new ItemStack(AnnoyingVillagersModItems.COMPRESSED_DIAMOND_CHESTPLATE.get());
        compressedDiamondChestplate.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
        compressedDiamondChestplate.enchant(Enchantments.PROJECTILE_PROTECTION, 5);
        compressedDiamondChestplate.enchant(Enchantments.FIRE_PROTECTION, 5);
        compressedDiamondChestplate.enchant(Enchantments.BLAST_PROTECTION, 5);
        damagedStacks.add(compressedDiamondChestplate);

        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        diamondSword.enchant(Enchantments.SHARPNESS, 5);
        diamondSword.enchant(Enchantments.SMITE, 5);
        damagedStacks.add(diamondSword);

        if (new Random().nextBoolean()) {
            damagedStacks.add(diamondSword);
        }

        ItemStack bow = this.getBowItem();
        bow.enchant(Enchantments.POWER_ARROWS, 5);
        bow.enchant(Enchantments.PUNCH_ARROWS, 5);
        damagedStacks.add(bow);

        double chance = new Random().nextDouble(0.0, 1.0);
        if (chance < 0.2) {
            ItemStack woodenDoor = new ItemStack(AnnoyingVillagersModItems.WOODEN_DOOR.get());
            woodenDoor.enchant(Enchantments.SHARPNESS, 5);
            woodenDoor.enchant(Enchantments.KNOCKBACK, 3);
            woodenDoor.enchant(Enchantments.MENDING, 5);
            damagedStacks.add(woodenDoor);
        } else if (chance < 0.4) {
            ItemStack craftingTable = new ItemStack(AnnoyingVillagersModItems.CRAFTING_TABLE.get());
            craftingTable.enchant(Enchantments.SMITE, 5);
            craftingTable.enchant(Enchantments.KNOCKBACK, 3);
            craftingTable.enchant(Enchantments.MENDING, 5);
            damagedStacks.add(craftingTable);
        } else if (chance < 0.6) {
            ItemStack ladder = new ItemStack(AnnoyingVillagersModItems.LADDER.get());
            ladder.enchant(Enchantments.SMITE, 5);
            ladder.enchant(Enchantments.SWEEPING_EDGE, 3);
            ladder.enchant(Enchantments.MENDING, 5);
            damagedStacks.add(ladder);
        } else if (chance < 0.8) {
            ItemStack trapDoor = new ItemStack(AnnoyingVillagersModItems.TRAPDOOR.get());
            trapDoor.enchant(Enchantments.KNOCKBACK, 5);
            trapDoor.enchant(Enchantments.SWEEPING_EDGE, 3);
            trapDoor.enchant(Enchantments.MENDING, 5);
            damagedStacks.add(trapDoor);
        } else {
            ItemStack mendingDiamondSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingDiamondSword.enchant(Enchantments.SHARPNESS, 5);
            mendingDiamondSword.enchant(Enchantments.SMITE, 5);
            mendingDiamondSword.enchant(Enchantments.MENDING, 5);
            damagedStacks.add(mendingDiamondSword);
        }

        ItemStack legendarySword = new ItemStack(AnnoyingVillagersModItems.LEGENDARY_SWORD.get());
        legendarySword.enchant(Enchantments.SHARPNESS, 5);
        legendarySword.enchant(Enchantments.SMITE, 5);
        legendarySword.enchant(Enchantments.SWEEPING_EDGE, 5);
        damagedStacks.add(legendarySword);
        damagedStacks.add(new ItemStack(AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get()));

        for (ItemStack stack : damagedStacks) {
            stack.setDamageValue(CommonUtil.getRandomDamage(stack));
            dropStack.accept(stack);
        }
    }

    public void playGuardBreakAttackAnimation() {
//      ADD THIS CODE IN AV_EFM
//        if (this.getLivingEntityPatch() != null) {
//            this.getLivingEntityPatch().playAnimationSynchronized(AnimsPugilistSteve.GUARD_BREAK_ATTACK, 0.0F);
//        }

        if (!this.level().isClientSide) {
            RigAnimationController.play(this, RigAnimationId.STUN_BACK);
        }
    }

    public void playTriedAnimation() {
//      ADD THIS CODE IN AV_EFM
//        Objects.requireNonNull(this.getLivingEntityPatch()).playAnimationSynchronized(AnimsPugilistSteve.TRIED, 0.0F);

        if (!this.level().isClientSide) {
            RigAnimationController.play(this, RigAnimationId.LEGENDARY_SWORD_KNOCKDOWN);
        }
    }

    @Override
    protected void implementFirstTick(ServerLevel serverLevel) {
        super.implementFirstTick(serverLevel);
        this.playSound(
                AnnoyingVillagersModSounds.ANGRY_STEVE_SAY_ON_SPAWN.get(),
                0.5F, 1.0F
        );
        playGuardBreakAttackAnimation();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel) {
            CommonUtil.stunImmunity(this, 3, 3);
            if (swapWeaponCooldown > 0) swapWeaponCooldown--;
            this.tickLegendaryAwakening();
            if (!neverLeave) {
                this.leaveTicks = this.leaveTicks - 1;
                int remaining = this.leaveTicks;

                if (remaining == 40) {
                    this.setNoAi(true);
                    playTriedAnimation();
                }
                if (remaining <= 0) {
                    Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal("<Steve> " + Component.translatable("subtitles.angry_steve_retreat")), false);
                    this.discard();
                }
            }
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
        ItemStack legendarySword = new ItemStack(AnnoyingVillagersModItems.LEGENDARY_SWORD.get());
        legendarySword.enchant(Enchantments.SHARPNESS, 5);
        legendarySword.enchant(Enchantments.SMITE, 5);
        legendarySword.enchant(Enchantments.SWEEPING_EDGE, 5);
        this.setItemInHand(InteractionHand.MAIN_HAND, legendarySword);
        this.setItemSlot(EquipmentSlot.MAINHAND, legendarySword);
        this.setMainWeaponItem(legendarySword);
        this.rollItem();
        TeamUtil.addOrJoinTeam(this, "steve");
        int min = AnnoyingVillagersConfig.ANGRY_STEVE_LEAVE_MIN_TIME.get();
        int max = AnnoyingVillagersConfig.ANGRY_STEVE_LEAVE_MAX_TIME.get();
        int randomMin = Math.min(min, max);
        int randomMax = Math.max(min, max);
        this.leaveTicks = (randomMin + new Random().nextInt(randomMax - randomMin + 1)) * 60 * 20;
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawngroupdata, compoundtag);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            SteveData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
        }
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

            InventoryUtils.addItem(this.inventory, new ItemStack(Items.ARROW, random.nextInt(64, 128)));
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

            InventoryUtils.addItem(this.inventory, new ItemStack(Items.REDSTONE, random.nextInt(0, 12)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.LAPIS_LAZULI, random.nextInt(0, 12)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.EMERALD, random.nextInt(0, 12)));
            InventoryUtils.addItem(this.inventory, new ItemStack(Items.DIAMOND, random.nextInt(0, 8)));
            InventoryUtils.addItem(this.inventory, new ItemStack(AnnoyingVillagersModItems.COMPRESSED_DIAMOND.get(), random.nextInt(0, 8)));
            return true;
        } else {
            return false;
        }
    }

    public static Builder addEpicFightAttributes(Builder builder) {
//      ADD THIS CODE IN AV_EFM
//        return builder.add(EpicFightAttributes.IMPACT.get(), 4.0D)
//                .add(EpicFightAttributes.ARMOR_NEGATION.get(), 10.0D)
//                .add(EpicFightAttributes.STUN_ARMOR.get(), 20.0D)
//                .add(EpicFightAttributes.MAX_STRIKES.get(), 100.0D)
//                .add(EpicFightAttributes.MAX_STAMINA.get(), 60.0D)
//                .add(EpicFightAttributes.STAMINA_REGEN.get(), 1.5D);

        return builder;
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 50.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        return addEpicFightAttributes(builder);
    }
}
