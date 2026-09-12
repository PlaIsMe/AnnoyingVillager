package com.pla.annoyingvillagers.entity;

import com.pla.annoyingvillagers.clazz.BurstProtectEntity;
import com.pla.annoyingvillagers.clazz.DangerousReaction;
import com.pla.annoyingvillagers.clazz.ForceTickEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.util.CommonGoals;
import com.pla.annoyingvillagers.util.HookGunCombatUtil;
import com.pla.annoyingvillagers.util.InventoryUtils;
import com.pla.annoyingvillagers.util.TeamUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class JevEntity extends AVNpc implements ForceTickEntity, BurstProtectEntity, DangerousReaction {
    private static final int JEV_POTION_STACK_SIZE = 16;
    private static final List<ItemLike> JEV_HOOKABLE_BLOCKS = List.of(
            Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS,
            Blocks.GLASS,
            Blocks.GLASS_PANE,
            Blocks.OAK_FENCE,
            Blocks.OAK_FENCE_GATE,
            Blocks.OAK_DOOR,
            Blocks.OAK_TRAPDOOR,
            Blocks.OAK_LEAVES,
            Blocks.HAY_BLOCK,
            Blocks.BARREL,
            Blocks.CRAFTING_TABLE,
            Blocks.PUMPKIN,
            Blocks.JACK_O_LANTERN,
            Blocks.LANTERN,
            Blocks.FLOWER_POT,
            Blocks.POPPY,
            Blocks.DANDELION,
            Blocks.OAK_SAPLING,
            Blocks.AZALEA,
            Blocks.CACTUS,
            Blocks.DEAD_BUSH
    );

    private UUID followTargetUUID;
    private AlexEntity followTarget;
    @Override
    public float getBurstProtectCapRatio() {
        return 0.15F;
    }

    public void setFollowTarget(AlexEntity followTarget) {
        this.followTarget = followTarget;
    }

    public AlexEntity getFollowTarget() {
        return followTarget;
    }

    public void setFollowTargetUUID(UUID followTargetUUID) {
        this.followTargetUUID = followTargetUUID;
    }

    public JevEntity(SpawnEntity spawnEntity, Level level) {
        this(AnnoyingVillagersModEntities.JEV.get(), level);
    }

    public JevEntity(EntityType<JevEntity> entitytype, Level level) {
        super(entitytype, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 10;
        this.setNoAi(false);
        this.setCustomName(this.getDisplayName());
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(AnnoyingVillagersModItems.JEV_BOOK.get()));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AnnoyingVillagersModItems.JEV_PENCIL.get()));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AnnoyingVillagersModItems.JEV_GLASSES.get()));
        this.setPlaceBlockToParryChance(0.0);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void registerGoals() {
        super.registerGoals();
        CommonGoals.registerDangerousReactionGoals(this);
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, AlexEntity.class, 12.0F));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 5.0F, 1.2D, 1.8D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.2D, 1.8D));
        this.goalSelector.addGoal(2, new Goal() {
            @Override
            public boolean canUse() {
                return followTarget != null && followTarget.isAlive() && distanceTo(followTarget) > (float)20.0D * 0.9F;
            }

            @Override
            public void tick() {
                if (followTarget != null && followTarget.isAlive()) {
                    getNavigation().moveTo(followTarget, 2.0D);
                    getLookControl().setLookAt(followTarget, 30.0F, 30.0F);
                    if (distanceToSqr(followTarget) > 20.0D) {
                        if (getNavigation().isDone()) {
                            getNavigation().moveTo(followTarget, 2.0D);
                        }
                    } else {
                        getNavigation().stop();
                    }
                }
            }

            @Override
            public boolean canContinueToUse() {
                return followTarget != null && followTarget.isAlive() && distanceTo(followTarget) > 50.0D;
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(6, new FollowMobGoal(this, 1.0D, 10.0F, 5.0F));
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

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData returnSpawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        TeamUtil.addOrJoinTeam(this, "alex");
        setMainWeaponItem(new ItemStack(AnnoyingVillagersModItems.JEV_PENCIL.get()));
        setOffWeaponItem(new ItemStack(AnnoyingVillagersModItems.JEV_BOOK.get()));
        return returnSpawnGroupData;
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        if (!this.level().isClientSide) {
            HookGunCombatUtil.onJevDeath(this);
        }
        super.die(pDamageSource);
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.JEV_GLASSES.get()));
        this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.JEV_PENCIL.get()));
        this.spawnAtLocation(new ItemStack(AnnoyingVillagersModItems.JEV_BOOK.get()));
        this.dropHookGunForAlex();
    }

    @Override
    protected boolean seedInventory() {
        if (!InventoryUtils.isEmpty(this.getInventory())) {
            return false;
        }

        Random random = new Random();
        addJevSeedItem(new ItemStack(Items.GOLDEN_APPLE, random.nextInt(16, 32)));
        addJevSeedItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, random.nextInt(16, 32)));

        List<ItemLike> foods = new ArrayList<>(REGULAR_FOODS);
        for (int i = 0; i < 2 && !foods.isEmpty(); i++) {
            ItemLike food = foods.remove(random.nextInt(foods.size()));
            addJevSeedItem(new ItemStack(food, random.nextInt(16, 32)));
        }

        addJevSeedItem(withRandomCount(new ItemStack(Items.POISONOUS_POTATO), 16, 32, random));
        addJevSeedItem(withRandomCount(new ItemStack(Items.PUFFERFISH), 16, 32, random));

        List<ItemLike> blocks = new ArrayList<>(JEV_HOOKABLE_BLOCKS);
        int blockStacks = random.nextInt(5, 8);
        for (int i = 0; i < blockStacks && !blocks.isEmpty(); i++) {
            ItemLike block = blocks.remove(random.nextInt(blocks.size()));
            addJevSeedItem(new ItemStack(block, random.nextInt(12, 24)));
        }

        addJevSeedPotionStacks(random);
        addJevSeedItem(new ItemStack(Items.WATER_BUCKET));
        addJevSeedItem(new ItemStack(Items.FLINT_AND_STEEL));
        addJevSeedItem(new ItemStack(Items.BONE_MEAL, random.nextInt(16, 32)));

        return true;
    }

    private static ItemStack withRandomCount(ItemStack stack, int minCount, int randomCount, Random random) {
        if (stack.isEmpty()) {
            return stack;
        }

        stack.setCount(random.nextInt(minCount, randomCount));
        return stack;
    }

    private void addJevSeedPotionStacks(Random random) {
        List<ItemStack> goodPotions = new ArrayList<>(List.of(
                createStrongHealingPotion(),
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_STRENGTH),
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_SWIFTNESS),
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_LEAPING),
                createHastePotion(),
                createGoodBuffPotion(),
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_REGENERATION)
        ));
        List<ItemStack> badPotions = new ArrayList<>(List.of(
                createPoisonPotion(),
                createWeaknessPotion(),
                createSlownessPotion(),
                createNauseaPotion(),
                createBlindnessPotion(),
                createStrongHarmingPotion(),
                createWitherPotion()
        ));

        Collections.shuffle(goodPotions, random);
        Collections.shuffle(badPotions, random);
        for (int i = 0; i < 4; i++) {
            addJevSeedItem(fullPotionStack(goodPotions.get(i)));
            addJevSeedItem(fullPotionStack(badPotions.get(i)));
        }
    }

    private boolean addJevSeedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        SimpleContainer inventory = this.getInventory();
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = inventory.getItem(slot);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameTags(slotStack, remaining)) {
                continue;
            }

            int stackLimit = getJevSeedStackLimit(slotStack);
            int transferable = Math.min(remaining.getCount(), stackLimit - slotStack.getCount());
            if (transferable <= 0) {
                continue;
            }

            slotStack.grow(transferable);
            remaining.shrink(transferable);
        }

        for (int slot = 0; slot < inventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                continue;
            }

            ItemStack inserted = remaining.copy();
            inserted.setCount(Math.min(remaining.getCount(), getJevSeedStackLimit(inserted)));
            inventory.setItem(slot, inserted);
            remaining.shrink(inserted.getCount());
        }

        return remaining.isEmpty();
    }

    private static int getJevSeedStackLimit(ItemStack stack) {
        if (stack.getItem() instanceof ThrowablePotionItem) {
            return JEV_POTION_STACK_SIZE;
        }
        return stack.getMaxStackSize();
    }

    private static ItemStack fullPotionStack(ItemStack stack) {
        stack.setCount(JEV_POTION_STACK_SIZE);
        return stack;
    }

    public static ItemStack createRandomJevLootBlock(Random random) {
        ItemLike block = JEV_HOOKABLE_BLOCKS.get(random.nextInt(JEV_HOOKABLE_BLOCKS.size()));
        return new ItemStack(block);
    }

    public static ItemStack createRandomJevLootFood(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> new ItemStack(Items.BREAD);
            case 1 -> new ItemStack(Items.POTATO);
            case 2 -> new ItemStack(Items.COOKED_BEEF);
            case 3 -> new ItemStack(Items.COOKED_CHICKEN);
            case 4 -> new ItemStack(Items.CARROT);
            case 5 -> new ItemStack(Items.GOLDEN_APPLE);
            case 6 -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case 7 -> new ItemStack(Items.POISONOUS_POTATO);
            default -> new ItemStack(Items.PUFFERFISH);
        };
    }

    public static ItemStack createRandomJevLootPotion(Random random) {
        return switch (random.nextInt(13)) {
            case 0 -> createStrongHealingPotion();
            case 1 -> PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_STRENGTH);
            case 2 -> PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_SWIFTNESS);
            case 3 -> PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_LEAPING);
            case 4 -> createHastePotion();
            case 5 -> createGoodBuffPotion();
            case 6 -> PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_REGENERATION);
            case 7 -> createPoisonPotion();
            case 8 -> createWeaknessPotion();
            case 9 -> createSlownessPotion();
            case 10 -> createNauseaPotion();
            case 11 -> createBlindnessPotion();
            default -> createStrongHarmingPotion();
        };
    }

    private static ItemStack createStrongHealingPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HEALING);
    }

    private static ItemStack createPoisonPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_POISON);
    }

    private static ItemStack createGoodBuffPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_REGENERATION);
    }

    private static ItemStack createWeaknessPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WEAKNESS);
    }

    private static ItemStack createSlownessPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_SLOWNESS);
    }

    private static ItemStack createStrongHarmingPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HARMING);
    }

    private static ItemStack createNauseaPotion() {
        return customSplashPotion(new MobEffectInstance(MobEffects.CONFUSION, 220, 0));
    }

    private static ItemStack createBlindnessPotion() {
        return customSplashPotion(new MobEffectInstance(MobEffects.BLINDNESS, 180, 0));
    }

    private static ItemStack createWitherPotion() {
        return customSplashPotion(new MobEffectInstance(MobEffects.WITHER, 160, 0));
    }

    private static ItemStack createHastePotion() {
        return customSplashPotion(new MobEffectInstance(MobEffects.DIG_SPEED, 360, 1));
    }

    private static ItemStack customSplashPotion(MobEffectInstance effect) {
        ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER);
        PotionUtils.setCustomEffects(potion, List.of(effect));
        return potion;
    }

    @Override
    protected void implementFirstTick(ServerLevel serverLevel) {
        super.implementFirstTick(serverLevel);
        this.playSound(
                AnnoyingVillagersModSounds.JEV_SAY_ON_SPAWN.get(),
                0.5F, 1.0F
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (followTarget == null && followTargetUUID != null) {
                Entity entity = serverLevel.getEntity(followTargetUUID);
                if (entity instanceof AlexEntity alex) {
                    followTarget = alex;
                } else {
                    followTargetUUID = null;
                }
            }
            if (followTarget != null && !followTarget.isAlive()) {
                followTarget = null;
                followTargetUUID = null;
            }
            if (followTarget != null && followTarget.isAlive()) {
                double distanceSq = this.distanceToSqr(followTarget);

                if (distanceSq > 600.0D) {
                    this.teleportTo(
                            followTarget.getX(),
                            followTarget.getY(),
                            followTarget.getZ()
                    );
                }
            }

            HookGunCombatUtil.tickJev(this, serverLevel);
        }
    }

    public @NotNull MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (followTargetUUID != null) {
            tag.putUUID("FollowTarget", followTargetUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FollowTarget")) {
            followTargetUUID = tag.getUUID("FollowTarget");
        }
    }

    private void dropHookGunForAlex() {
        ItemStack hookGun = HookGunCombatUtil.createBoundHookGun(HookGunCombatUtil.createJevPickaxe());
        this.spawnAtLocation(hookGun);
    }

    public boolean removeWhenFarAway(double d0) {
        return false;
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    public SoundEvent getAmbientSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.villager.ambient"));
    }

    public SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.villager.hurt"));
    }

    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft","entity.villager.death"));
    }

    public boolean hurt(@NotNull DamageSource damageSource, float f) {
        return super.hurt(damageSource, f);
    }

    public static Builder createAttributes() {
        Builder builder = Mob.createMobAttributes();

        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.35D);
        builder = builder.add(Attributes.MAX_HEALTH, 50.0D);
        builder = builder.add(Attributes.ARMOR, 20.0D);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 0.0D);
        builder = builder.add(Attributes.FOLLOW_RANGE, 48.0D);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 5.0D);
        return builder;
    }
}
