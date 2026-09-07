package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.AVNpc;
import com.pla.annoyingvillagers.compat.SmartNpc;
import com.pla.annoyingvillagers.entity.ItemProjectile;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.network.ClientboundDiamondAttractorFx;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.rig.RigStunController;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DiamondAttractorSwordItem extends SwordItem implements RigCombatProfileProvider {

    public DiamondAttractorSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
            }

            public float getSpeed() {
                return 6.0F;
            }

            public float getAttackDamageBonus() {
                return 2.4F;
            }

            public int getLevel() {
                return 5;
            }

            public int getEnchantmentValue() {
                return 21;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.DIAMOND));
            }
        }, 3, -2.8F, (new Properties()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), AnnoyingVillagersModSounds.DIAMOND_ATTRACTOR.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new ClientboundDiamondAttractorFx(player));
            pullWeapons(player);
            player.getCooldowns().addCooldown(this, 20 * 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.DIAMOND_ATTRACTOR;
    }

    public static void pullWeapons(LivingEntity owner) {
        Level rawLevel = owner.level();

        if (!(rawLevel instanceof ServerLevel level)) {
            return;
        }

        double radius = 7;
        AABB area = owner.getBoundingBox().inflate(radius);

        pullDroppedWeapons(level, owner, area);
        pullHeldWeapons(level, owner, area);
    }

    private static void pullDroppedWeapons(ServerLevel level, LivingEntity owner, AABB area) {
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                itemEntity -> itemEntity.isAlive() && CommonUtil.isPullableWeapon(itemEntity.getItem())
        );

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stackInWorld = itemEntity.getItem();

            if (stackInWorld.isEmpty()) {
                continue;
            }

            if (CommonUtil.isBlacklistedWeapon(stackInWorld)) {
                CommonUtil.fallBackOnBlackListWeapon(owner, itemEntity, stackInWorld);
                continue;
            }

            ItemStack pulledStack = stackInWorld.copy();
            pulledStack.setCount(1);

            ItemStack remainder = stackInWorld.copy();
            remainder.shrink(1);

            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }

            spawnPulledWeapon(level, owner, pulledStack, itemEntity.position());
        }
    }

    private static void knockDownFromPulling(LivingEntity target) {
//        Add this to AV_EFM

//        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
//        if (targetPatch != null) {
//            AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(targetPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
//            if (!EpicfightUtil.isLongHitAnimation(dynamicAnimation, targetPatch)) {
//                targetPatch.playAnimationSynchronized(AnimsPugilistSteve.KNOCKDOWN_FORWARD, 0.0F);
//            }
//        }

        if (!(target instanceof Mob mob)) return;
        if (!RigStunController.supports(mob)) return;

        RigStunController.applyKnockdown(mob, RigAnimationId.KNOCKDOWN_FORWARD);
    }

    private static void pullHeldWeapons(ServerLevel level, LivingEntity owner, AABB area) {
        List<LivingEntity> livingEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != owner
        );

        for (LivingEntity target : livingEntities) {
            CommonUtil.pullEntityTowardCaster(target, owner);
            knockDownFromPulling(target);
            if (CommonUtil.entityCanBeDisarmed(target)) {
                List<InteractionHand> candidateHands = new ArrayList<>(2);

                ItemStack mainHand = target.getMainHandItem();
                ItemStack offHand = target.getOffhandItem();

                if (CommonUtil.isPullableWeapon(mainHand)) {
                    candidateHands.add(InteractionHand.MAIN_HAND);
                }

                if (CommonUtil.isPullableWeapon(offHand)) {
                    candidateHands.add(InteractionHand.OFF_HAND);
                }

                if (candidateHands.isEmpty()) {
                    continue;
                }

                InteractionHand chosenHand = candidateHands.get(
                        level.random.nextInt(candidateHands.size())
                );

                ItemStack chosenStack = target.getItemInHand(chosenHand);

                if (CommonUtil.isBlacklistedWeapon(chosenStack)) {
                    CommonUtil.fallBackOnBlackListWeapon(owner, target, chosenStack);
                    continue;
                }

                ItemStack pulledStack = chosenStack.copy();
                target.setItemInHand(chosenHand, ItemStack.EMPTY);
                if (target instanceof AVNpc avNpc) {
                    if (chosenHand == InteractionHand.MAIN_HAND) {
                        avNpc.setMainWeaponItem(ItemStack.EMPTY);
                    } else {
                        avNpc.setOffWeaponItem(ItemStack.EMPTY);
                    }
                }

                if (ModList.get().isLoaded("smart_npc")) {
                    SmartNpc.clearPlayerNpcItems(target, chosenHand);
                }
                Vec3 spawnPos = getHeldWeaponSpawnPos(target);
                spawnPulledWeapon(level, owner, pulledStack, spawnPos);
            }
        }
    }

    private static Vec3 getHeldWeaponSpawnPos(LivingEntity target) {
        return target.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
    }

    private static void spawnPulledWeapon(
            ServerLevel level,
            LivingEntity owner,
            ItemStack stack,
            Vec3 spawnPos
    ) {
        if (stack.isEmpty()) {
            return;
        }

        ItemProjectile projectile =
                new ItemProjectile(
                        level,
                        owner,
                        stack,
                        spawnPos.add(0.0D, 0.15D, 0.0D)
                );

        level.addFreshEntity(projectile);
    }
}
