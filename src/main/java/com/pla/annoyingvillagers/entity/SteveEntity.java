package com.pla.annoyingvillagers.entity;

import javax.annotation.Nullable;

import com.pla.annoyingvillagers.clazz.*;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.spawnhandler.SteveData;
import com.pla.annoyingvillagers.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class SteveEntity extends AVNpc implements PersistentPlayerNpc, BurstProtectEntity, RollItemUser
        , FishingRodUser, DangerousReaction {
    // 0: normal
    // 1: second
    private int state = 0;
    private int swapWeaponCooldown;
    private boolean sayLegendary = false;
    private final FishingRodUser.State combatFishingRodState = new FishingRodUser.State();

    @Override
    public float getBurstProtectCapRatio() {
        return 0.15F;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String persistentPlayerIdentity() { return "Steve"; }

    public SteveEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.STEVE.get(), level);
    }

    public SteveEntity(EntityType<SteveEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 8;
        this.setNoAi(false);
        this.setCustomName(this.getDisplayName());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setPlaceBlockToParryChance(0.8);
        this.setMainWeaponItem(new ItemStack(Items.DIAMOND_SWORD));
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void registerGoals() {
        super.registerGoals();
        CommonGoals.registerDangerousReactionGoals(this);
        CommonGoals.registerGoalForNeutralNpc(this);
    }

    @Override
    public @Nullable SoundEvent getAttackVoiceSound() {
        return AnnoyingVillagersModSounds.STEVE_SAY.get();
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

    public int getSwapWeaponCooldown() {
        return swapWeaponCooldown;
    }

    @Override
    public boolean canRollItem() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        return (this.getBlockDamage() == null && this.swapWeaponCooldown == 0)
                || (this.state == 0
                && this.getHealth() <= 20.0F
                && !this.getMainHandItem().is(Items.DIAMOND_SWORD));
    }

    @Override
    public FishingRodUser.State getCombatFishingRodState() {
        return this.combatFishingRodState;
    }

    @Override
    public Item getCombatFishingRodItem() {
        return AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get();
    }

    @Override
    public boolean canStartCombatFishingRodSession(Mob self) {
        return this.state == 1;
    }

    @Override
    public boolean canUseStickyCombatFishingRodTarget() {
        return this.state == 1;
    }

    @Override
    public boolean canUseJessicaCombatFishingRodHook() {
        return this.state == 1;
    }

    public SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.hurt"));
    }

    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.generic.death"));
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
    public void die(@NotNull DamageSource pDamageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (ProgressionUtil.isAtLeastDifficulty(Difficulty.HARD) && new Random().nextFloat() <= AnnoyingVillagersConfig.ANGRY_STEVE_CHANCE.get()) {
                LivingEntity target = null;
                if (pDamageSource.getEntity() instanceof LivingEntity living && living.isAlive()) {
                    target = living;
                } else if (this.getTarget() != null && this.getTarget().isAlive()) {
                    target = this.getTarget();
                } else if (this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
                    target = this.getLastHurtByMob();
                }

                AngrySteveEntity angrySteveEntity = new AngrySteveEntity(AnnoyingVillagersModEntities.ANGRY_STEVE.get(), serverLevel);

                angrySteveEntity.moveTo(this.blockPosition(), this.getYRot(), this.getXRot());
                InventoryUtils.transferInventory(this.getInventory(), angrySteveEntity.getInventory());
                com.pla.annoyingvillagers.util.RemoteNpcDeparture.copy(this, angrySteveEntity);
                this.discard();
                SteveData steveData = SteveData.get(serverLevel);
                steveData.forceClaim(serverLevel, angrySteveEntity.getUUID());

                angrySteveEntity.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(angrySteveEntity.blockPosition()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
                serverLevel.addFreshEntity(angrySteveEntity);
                if (target != null) {
                    angrySteveEntity.setTarget(target);
                    angrySteveEntity.setLastHurtByMob(target);
                }
            } else {
                this.playSound(
                        AnnoyingVillagersModSounds.STEVE_SAY_ON_DEATH.get(),
                        0.5F, 1.0F
                );
            }
        }

        super.die(pDamageSource);
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

        double rareWeaponRoll = new Random().nextDouble(0.0D, 1.0D);
        if (rareWeaponRoll < 0.3D) {
            ItemStack diamondGreatsword = new ItemStack(AnnoyingVillagersModItems.DIAMOND_GREATSWORD.get());
            diamondGreatsword.enchant(Enchantments.SHARPNESS, 5);
            diamondGreatsword.enchant(Enchantments.SMITE, 5);
            diamondGreatsword.enchant(Enchantments.SWEEPING_EDGE, 5);
            damagedStacks.add(diamondGreatsword);
        } else if (rareWeaponRoll < 0.6D) {
            ItemStack samanthaTheKillerAxe = new ItemStack(AnnoyingVillagersModItems.SAMANTHA_THE_KILLER_AXE.get());
            samanthaTheKillerAxe.enchant(Enchantments.SHARPNESS, 5);
            samanthaTheKillerAxe.enchant(Enchantments.SMITE, 5);
            samanthaTheKillerAxe.enchant(Enchantments.SWEEPING_EDGE, 5);
            damagedStacks.add(samanthaTheKillerAxe);
        } else {
            ItemStack woopieTheSword = new ItemStack(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
            woopieTheSword.enchant(Enchantments.SHARPNESS, 5);
            woopieTheSword.enchant(Enchantments.SMITE, 5);
            woopieTheSword.enchant(Enchantments.SWEEPING_EDGE, 5);
            damagedStacks.add(woopieTheSword);
        }
        damagedStacks.add(new ItemStack(AnnoyingVillagersModItems.JESSICA_THE_DARK_SHIELD.get()));
        damagedStacks.add(new ItemStack(AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get()));

        for (ItemStack stack : damagedStacks) {
            stack.setDamageValue(CommonUtil.getRandomDamage(stack));
            dropStack.accept(stack);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("State", this.state);
        tag.putInt("SwapWeaponCooldown", this.swapWeaponCooldown);
        tag.putBoolean("SayLegendary", sayLegendary);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.state = tag.getInt("State");
        this.swapWeaponCooldown = tag.getInt("SwapWeaponCooldown");
        this.sayLegendary = tag.getBoolean("SayLegendary");
    }

    public void rollItem() {
        double chance;
        boolean setWeapon = false;
        if (this.state == 1) {
            chance = new Random().nextDouble(0.0, 1.0);
            if (this.getHealth() > this.getMaxHealth() / 2) {
                if (chance < 0.2) {
                    ItemStack woopieTheSword = new ItemStack(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
                    woopieTheSword.enchant(Enchantments.SHARPNESS, 5);
                    woopieTheSword.enchant(Enchantments.SMITE, 5);
                    woopieTheSword.enchant(Enchantments.SWEEPING_EDGE, 5);
                    this.setItemInHand(InteractionHand.MAIN_HAND, woopieTheSword);

                    this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AnnoyingVillagersModItems.JESSICA_THE_DARK_SHIELD.get()));
                    this.setOffWeaponItem(this.getOffWeaponItem().copy());
                    setWeapon = true;
                } else if (chance < 0.4) {
                    ItemStack diamondGreatsword = new ItemStack(AnnoyingVillagersModItems.DIAMOND_GREATSWORD.get());
                    diamondGreatsword.enchant(Enchantments.SHARPNESS, 5);
                    diamondGreatsword.enchant(Enchantments.KNOCKBACK, 5);
                    this.setItemInHand(InteractionHand.MAIN_HAND, diamondGreatsword);

                    this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    this.setOffWeaponItem(this.getOffWeaponItem().copy());
                    setWeapon = true;
                } else if (chance < 0.6) {
                    ItemStack killerAxe = new ItemStack(AnnoyingVillagersModItems.SAMANTHA_THE_KILLER_AXE.get());
                    killerAxe.enchant(Enchantments.SHARPNESS, 5);
                    killerAxe.enchant(Enchantments.FIRE_ASPECT, 2);
                    this.setItemInHand(InteractionHand.MAIN_HAND, killerAxe);

                    this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AnnoyingVillagersModItems.JESSICA_THE_DARK_SHIELD.get()));
                    this.setOffWeaponItem(this.getOffWeaponItem().copy());
                    setWeapon = true;
                } else {
                    ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
                    diamondSword.enchant(Enchantments.SHARPNESS, 5);
                    diamondSword.enchant(Enchantments.SMITE, 5);
                    this.setItemInHand(InteractionHand.MAIN_HAND, diamondSword);
                    this.setItemInHand(InteractionHand.OFF_HAND, diamondSword);
                    setWeapon = true;
                }
            } else {
                if (chance <= 0.4) {
                    ItemStack woopieTheSword = new ItemStack(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
                    woopieTheSword.enchant(Enchantments.SHARPNESS, 5);
                    woopieTheSword.enchant(Enchantments.SMITE, 5);
                    woopieTheSword.enchant(Enchantments.SWEEPING_EDGE, 5);
                    this.setItemInHand(InteractionHand.MAIN_HAND, woopieTheSword);

                    this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AnnoyingVillagersModItems.JESSICA_THE_DARK_SHIELD.get()));
                    this.setOffWeaponItem(this.getOffWeaponItem().copy());
                    setWeapon = true;
                } else if (this.level() instanceof ServerLevel) {
                    if (!this.sayLegendary) {
                        this.playSound(
                                AnnoyingVillagersModSounds.STEVE_SAY_I_NOT_BELIEVE.get(),
                                0.5F, 1.0F
                        );
                        this.sayLegendary = true;
                    }
                    ItemStack legendarySword = new ItemStack(AnnoyingVillagersModItems.LEGENDARY_SWORD.get());
                    this.setItemInHand(InteractionHand.MAIN_HAND, legendarySword);
                    this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    this.setOffWeaponItem(this.getOffWeaponItem().copy());
                    setWeapon = true;
                }
            }
        } else if (this.state == 0 && this.getHealth() <= 20) {
            ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
            diamondSword.enchant(Enchantments.SHARPNESS, 5);
            diamondSword.enchant(Enchantments.SMITE, 5);
            this.setItemInHand(InteractionHand.MAIN_HAND, diamondSword);
            this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            setWeapon = true;
        }

        if (!setWeapon) {
            chance = new Random().nextDouble(0.0, 1.0);
            if (chance <= 0.2) {
                ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
                diamondSword.enchant(Enchantments.SHARPNESS, 5);
                diamondSword.enchant(Enchantments.SMITE, 5);
                this.setItemInHand(InteractionHand.MAIN_HAND, diamondSword);
            } else if (chance <= 0.4) {
                ItemStack woodenDoor = new ItemStack(AnnoyingVillagersModItems.WOODEN_DOOR.get());
                woodenDoor.enchant(Enchantments.SHARPNESS, 5);
                woodenDoor.enchant(Enchantments.KNOCKBACK, 3);
                this.setItemInHand(InteractionHand.MAIN_HAND, woodenDoor);
            } else if (chance <= 0.6) {
                ItemStack craftingTable = new ItemStack(AnnoyingVillagersModItems.CRAFTING_TABLE.get());
                craftingTable.enchant(Enchantments.SMITE, 5);
                craftingTable.enchant(Enchantments.KNOCKBACK, 3);
                this.setItemInHand(InteractionHand.MAIN_HAND, craftingTable);
            } else if (chance <= 0.8) {
                ItemStack ladder = new ItemStack(AnnoyingVillagersModItems.LADDER.get());
                ladder.enchant(Enchantments.SMITE, 5);
                ladder.enchant(Enchantments.SWEEPING_EDGE, 3);
                this.setItemInHand(InteractionHand.MAIN_HAND, ladder);
            } else {
                ItemStack trapDoor = new ItemStack(AnnoyingVillagersModItems.TRAPDOOR.get());
                trapDoor.enchant(Enchantments.KNOCKBACK, 5);
                trapDoor.enchant(Enchantments.SWEEPING_EDGE, 3);
                this.setItemInHand(InteractionHand.MAIN_HAND, trapDoor);
            }
        }
        this.setMainWeaponItem(this.getMainHandItem().copy());
        this.setOffWeaponItem(this.getOffhandItem().copy());
        this.swapWeaponCooldown = new Random().nextInt(100, 200);
    }

    @Override
    protected void implementFirstTick(ServerLevel serverLevel) {
        super.implementFirstTick(serverLevel);
        this.playSound(
                AnnoyingVillagersModSounds.STEVE_SAY_ON_SPAWN.get(),
                0.5F, 1.0F
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel) {
            if (this.getTarget() != null && this.getTarget().isAlive() && this.getMainHandItem().isEmpty()) {
                rollItem();
                this.playSound(
                        AnnoyingVillagersModSounds.STEVE_SAY_WHAT.get(),
                        0.5F, 1.0F
                );
            }
            if (this.getState() != 2 && this.getTarget() == null && !this.getMainHandItem().isEmpty()) {
                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (this.state == 0
                    && this.getHealth() <= 20
                    && !this.getItemInHand(InteractionHand.OFF_HAND).getItem().equals(Items.TOTEM_OF_UNDYING)) {
                this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
            if (this.getTarget() != null && this.state == 0
                    && this.getHealth() > 20
                    && this.getItemInHand(InteractionHand.OFF_HAND).getItem().equals(Items.TOTEM_OF_UNDYING)
                    && !(this.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ShieldItem)) {
                this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (swapWeaponCooldown > 0) swapWeaponCooldown--;
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
            SteveData steveData = SteveData.get(serverLevel);

            if (!steveData.tryClaim(serverLevel, this.getUUID())) {
                this.discard();
                return null;
            }
        }

        TeamUtil.addOrJoinTeam(this, "steve");
        this.swapWeaponCooldown = new Random().nextInt(100, 200);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawngroupdata, compoundtag);
    }

    public static boolean canSpawn(EntityType<SteveEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        if (SteveData.get(serverLevel).isOccupied(serverLevel)) {
            return false;
        }
        return PathfinderMob.checkMobSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel &&
                (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            SteveData.get(serverLevel).releaseIfMatches(serverLevel, this.getUUID());
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
