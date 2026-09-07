package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.item.*;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class SpecialAttackOnKeyPressedEvent {
    private static void playHookGunAnimation(Player player) {
//        Add this in AV_EFM
//        LivingEntityPatch<?> freshPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
//                if (freshPatch != null) {
//                    freshPatch.playAnimationSynchronized(AnimsPugilistSteve.HOOK_GUN, 0.0F);
//                }
//        create VANILLA_ANIMATION
    }

    private static void playPortalSummonAnimation(Player player) {
//        Add this in AV_EFM
//        LivingEntityPatch<?> freshPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
//                if (freshPatch != null) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsSculkSteve.PORTAL_SUMMON, 0.0F);
//                }
//        create VANILLA_ANIMATION
    }

    private static void playSinglePortalSummonAnimation(Player player) {
//        Add this in AV_EFM
//        LivingEntityPatch<?> freshPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
//                if (freshPatch != null) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsEpicFightIronSpell.CASTING_ONE_HAND_TOP, 0.0F);
//                }
//        create VANILLA_ANIMATION
    }

    private static void playHookGunBindAnimationAfterHandRefresh(Player player) {
        new DelayedTask(2) {
            @Override
            public void run() {
                if (player.isRemoved() || player.level().isClientSide()) {
                    return;
                }

                playHookGunAnimation(player);
            }
        };
    }

    private static void playTransporterFragmentAnimation(Player player, TransporterFragmentItem.UseMode useMode) {
        switch (useMode) {
            case BOTH_HANDS, MAIN_HAND -> {
                playPortalSummonAnimation(player);
            }
            case OFF_HAND -> playSinglePortalSummonAnimation(player);
            case NONE -> {
            }
        }
    }

    private static boolean efmConditionToExecute(Entity entity) {
//        Add this in AV_EFM
//        PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(entity, PlayerPatch.class);
//        LivingEntityPatch<?> livingEntityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
//        if (livingEntityPatch == null) return false;
//        AssetAccessor<? extends StaticAnimation> dynamicAnimation = Objects.requireNonNull(livingEntityPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
//        if (EpicfightUtil.isLongHitAnimation(dynamicAnimation, livingEntityPatch)) {
//            return false;
//        }
//        if (entity.level() instanceof ServerLevel) {
//            if (dynamicAnimation != Animations.EMPTY_ANIMATION) {
//                return;
//            }
//        }
        return true;
    }

    public static void execute(LevelAccessor world, Entity entity, Vec3 crosshairTarget) {
        if (entity == null) return;

        if (!efmConditionToExecute(entity)) {
            return;
        }

        if (entity instanceof Player player
                && !player.level().isClientSide()
                && HookGunItem.tryBindFromSpecialAttack(player)) {
            playHookGunBindAnimationAfterHandRefresh(player);
            return;
        }


        addEfmSpecialAttackCompat(entity);
        addVanillaSpecialAttack(entity);
    }

    private static void addVanillaSpecialAttack(Entity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        if (player.getMainHandItem().getItem() instanceof BlueDemonTridentItem) {
            if (player.isShiftKeyDown()) BlueDemonTridentItem.activateVanillaThunderAttack(player);
            else BlueDemonTridentItem.activateVanillaElectricField(player);
            return;
        }
        if (WoopieTheSwordItem.activateVanillaSpecial(player)) return;
        if (ShadowObsidianSwordItem.activateVanillaSpecial(player)) return;
        if (EnderSlayerScytheItem.activateVanillaSpecial(player)) return;
        if (DemoniacVoltageReaverItem.activateVanillaSpecial(player)) return;
        if (ObsidianSledgehammerItem.activateVanillaSpecial(player)) return;
        if (ShadowObsidianPillarItem.activateVanillaSpecial(player)) return;
        if (ShadowObsidianWeaponItem.activateVanillaSpecial(player)) return;
        if (ObsidianWeaponItem.activateVanillaSpecial(player)) return;
        LegendarySwordItem.activateVanillaSpecial(player);
    }

    private static void addEfmSpecialAttackCompat(Entity entity) {
//       Add this code in AV_EFM

//        if (entity instanceof Player player) {
//            // Spawn special effect without playing animation
//            ItemStack holdingItem = player.getMainHandItem();
//            ItemStack offHandItem = player.getOffhandItem();
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.BLACK_FIRE_SWORD.get())) {
//                PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                    SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.BLACK_FIRE_SWORD);
//                    if (skillContainer != null
//                            && entity.level() instanceof ServerLevel serverLevel) {
//                        if (skillContainer.getResource() >= 5){
//                            Skill.setSkillConsumptionSynchronize(
//                                    skillContainer,
//                                    skillContainer.getResource() - 5
//                            );
//                            BlackFireEntity.spawnOnOwnerSword(serverLevel, player);
//                            return;
//                        }
//                    }
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.THUNDER_DIAMOND_BLADE.get())) {
//                PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                    SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.THUNDER_DIAMOND_BLADE);
//                    if (skillContainer != null
//                            && entity.level() instanceof ServerLevel serverLevel) {
//                        if (skillContainer.getResource() >= 10){
//                            Skill.setSkillConsumptionSynchronize(
//                                    skillContainer,
//                                    skillContainer.getResource() - 10
//                            );
//                            ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, player);
//                            return;
//                        }
//                    }
//
//                    skillContainer = serverPlayerPatch.getSkill(AVSkills.DUAL_THUNDER_DIAMOND_BLADE);
//                    if (skillContainer != null
//                            && entity.level() instanceof ServerLevel serverLevel) {
//                        if (skillContainer.getResource() >= 10){
//                            Skill.setSkillConsumptionSynchronize(
//                                    skillContainer,
//                                    skillContainer.getResource() - 10
//                            );
//                            ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, player);
//                            if (offHandItem.getItem().equals(AnnoyingVillagersModItems.THUNDER_DIAMOND_BLADE.get())) {
//                                ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, player, true);
//                            }
//                            return;
//                        }
//                    }
//                }
//            }
//        }
//
//        if (entity instanceof Player player) {
//            // Check by item
//            ItemStack holdingItem = player.getMainHandItem();
//            ItemStack offHandItem = player.getOffhandItem();
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (offHandItem.getItem().equals(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get())) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsWom.CUT_ENDERBLASTER_TWOHAND_RELOAD, 0.0F);
//                        PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                        if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                            SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.TRIDENT_FESTIVAL);
//                            if (skillContainer != null && skillContainer.getSkill() instanceof TridentFestivalSkill tridentFestivalSkill) {
//                                tridentFestivalSkill.toggleMode(skillContainer);
//                            }
//                        }
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsEpicFightBattleArts.TRIDENT_THROW_3, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.ENDER_AEGIS.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsWom.ENDER_AEGIS_BULL_CHARGE, 0.0F);
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.EARTH_AXE.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AVAnimations.EARTH_AXE_SHOOT, 0.0F);
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.TWIN_DIAMOND_SPEAR.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SPEAR_THRUST, 0.0F);
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.ENDER_GLAIVE.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.ENDER_GLAIVE);
//                        if (skillContainer != null && skillContainer.getSkill() instanceof EnderGlaiveSkill enderGlaiveSkill) {
//                            if (skillContainer.getStack() >= 1) {
//                                livingEntityPatch.playAnimationSynchronized(AnimsWom.ENDER_GLAIVE_NAPOLEON_SHOOT_3, 0.0F);
//                                enderGlaiveSkill.getResourceType().consumer
//                                        .consume(skillContainer, serverPlayerPatch, enderGlaiveSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                                success = true;
//                            }
//                        }
//                    }
//                    if (!success) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsAgony.AGONY_RISING_EAGLE, 0.0F);
//                        new DelayedTask(10) {
//                            @Override
//                            public void run() {
//                                livingEntityPatch.playAnimationSynchronized(AnimsAgony.AGONY_RIPPING_FANGS, 0.0F);
//                            }
//                        };
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get())) {
//                if (entity.level() instanceof ServerLevel
//                        && holdingItem.getTag() != null && !holdingItem.getTag().getBoolean("SnakeAnimation")) {
//                    livingEntityPatch.playAnimationSynchronized(WOMAnimations.TORMENT_CHARGED_ATTACK_1, 0.0F);
//                }
//                return;
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.OBSIDIAN_SLEDGEHAMMER.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.OBSIDIAN_SLEDGEHAMMER);
//                        if (skillContainer != null && skillContainer.getSkill() instanceof ObsidianSledgeHammerSkill) {
//                            if (skillContainer.isActivated()) {
//                                livingEntityPatch.playAnimationSynchronized(AnimsWom.SLEDGEHAMMER_SOLAR_AUTO_3, 0.0F);
//                                success = true;
//                            }
//                        }
//                    }
//                    if (!success) {
//                        livingEntityPatch.playAnimationSynchronized(WOMAnimations.TORMENT_BERSERK_DASH, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.ENDER_SLAYER_SCYTHE.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.ENDER_SLAYER_SCYTHE);
//                        if (skillContainer != null
//                                && entity.level() instanceof ServerLevel serverLevel
//                                && entity.onGround()
//                                && skillContainer.getSkill() instanceof EnderSlayerScytheSkill) {
//                            if (entity.getPersistentData().contains("DragonUUID") && !player.getCooldowns().isOnCooldown(holdingItem.getItem())) {
//                                Entity dragon = serverLevel.getEntity(player.getPersistentData().getUUID("DragonUUID"));
//
//                                if (dragon instanceof HerobrineDragonEntity herobrineDragonEntity && herobrineDragonEntity.getPassengers().isEmpty()) {
//                                    livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.POSE_UP, 0.0F);
//                                    herobrineDragonEntity.recallAndLand(true);
//                                    player.getCooldowns().addCooldown(holdingItem.getItem(), 60);
//                                }
//                            }
//                        }
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.NULL_WEAPON.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.NULL_WEAPON);
//                        if (skillContainer != null && skillContainer.getSkill() instanceof NullWeaponSkill && !skillContainer.isActivated()) {
//                            livingEntityPatch.playAnimationSynchronized(AnimsWom.CLONE_ANTITHEUS_SHOOT, 0.0F);
//                        } else {
//                            livingEntityPatch.playAnimationSynchronized(AnimsWom.NULL_SKELETON_ANTITHEUS_ASCENSION, 0.0F);
//                        }
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.OBSIDIAN_WEAPON.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.OBSIDIAN_WEAPON);
//                        if (skillContainer != null && skillContainer.getStack() >= 1
//                                && entity.level() instanceof ServerLevel
//                                && skillContainer.getSkill() instanceof ObsidianWeaponSkill obsidianWeaponSkill) {
//                            success = true;
//                            obsidianWeaponSkill.getResourceType().consumer
//                                    .consume(skillContainer, serverPlayerPatch, obsidianWeaponSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                        }
//                    }
//                    if (success) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsWom.OBSIDIAN_ANTITHEUS_ASCENDED_DEATHFALL, 0.0F);
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.OBSIDIAN_FIST_DASH, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_WEAPON.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.OBSIDIAN_WEAPON);
//                        if (skillContainer != null && skillContainer.getStack() >= 1
//                                && entity.level() instanceof ServerLevel
//                                && skillContainer.getSkill() instanceof ObsidianWeaponSkill obsidianWeaponSkill) {
//                            success = true;
//                            obsidianWeaponSkill.getResourceType().consumer
//                                    .consume(skillContainer, serverPlayerPatch, obsidianWeaponSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                        }
//                    }
//                    if (success) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsWom.OBSIDIAN_ANTITHEUS_ASCENDED_DEATHFALL, 0.0F);
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.OBSIDIAN_FIST_DASH, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.BEDROCK_WEAPON.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SUPER_PUNCH, 0.0F);
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_PILLAR.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.SHADOW_OBSIDIAN_PILLAR);
//                        if (skillContainer != null && skillContainer.getStack() >= 1
//                                && entity.level() instanceof ServerLevel
//                                && skillContainer.getSkill() instanceof ShadowObsidianPillarSkill shadowObsidianPillarSkill) {
//                            success = true;
//                            shadowObsidianPillarSkill.getResourceType().consumer
//                                    .consume(skillContainer, serverPlayerPatch, shadowObsidianPillarSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                        }
//                    }
//                    if (success) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsWom.OBSIDIAN_ANTITHEUS_ASCENDED_DEATHFALL, 0.0F);
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsEpicFightInfernalGainer.OBSIDIAN_INFERNAL_AUTO_2, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_SWORD.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (offHandItem.getItem().equals(AnnoyingVillagersModItems.SHADOW_OBSIDIAN_SWORD.get())) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsWom.SHADOW_OBSIDIAN_SWORD_GESETZ_AUTO_3, 0.0F);
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.OBSIDIAN_FIST_DASH, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.HEROBRINE_ENDER_EYE.get())
//                    || offHandItem.getItem().equals(AnnoyingVillagersModItems.HEROBRINE_ENDER_EYE.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(Animations.BIPED_LANDING, 0.0F);
//                    HerobrineEnderEyeItem.startShadowObsidianMachineGun((ServerLevel) player.level(), player);
//                    if (player.getMainHandItem().getItem().equals(AnnoyingVillagersModItems.HEROBRINE_ENDER_EYE.get())) {
//                        player.getMainHandItem().hurtAndBreak(10, player, p -> {
//                        });
//                    } else if (player.getOffhandItem().getItem().equals(AnnoyingVillagersModItems.HEROBRINE_ENDER_EYE.get())) {
//                        player.getOffhandItem().hurtAndBreak(10, player, p -> {
//                        });
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem() instanceof BowItem) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsEpicFightACG.BOW_AUTO_2, 0.0F);
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.LEGENDARY_SWORD.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    boolean holdingTridentOffhand = offHandItem.getItem().equals(AnnoyingVillagersModItems.BLUE_DEMON_TRIDENT.get());
//
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.LEGENDARY_SWORD);
//                        if (skillContainer != null && skillContainer.getSkill() instanceof LegendarySwordSkill legendarySwordSkill && player.level() instanceof ServerLevel) {
//                            if (skillContainer.getStack() >= 1) {
//                                if (holdingTridentOffhand) {
//                                    livingEntityPatch.playAnimationSynchronized(AnimsWom.ELECTRIC_FIELD, 0.0F);
//                                } else {
//                                    livingEntityPatch.playAnimationSynchronized(AnimsWom.YELLOW_TORMENT_CHARGED_ATTACK_3, 0.0F);
//                                }
//                                legendarySwordSkill.getResourceType().consumer
//                                        .consume(skillContainer, serverPlayerPatch, legendarySwordSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                                success = true;
//                            }
//                        }
//                    }
//
//                    if (!success) {
//                        if (holdingTridentOffhand) {
//                            livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.TRIDENT_THROW_LEGENDARY, 0.0F);
//                        } else {
//                            player.addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY.get(), 60, 2));
//                            livingEntityPatch.playAnimationSynchronized(AnimsWom.CLONE_NAPOLEON_WATERLOW_SHOOT, 0.0F);
//                        }
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    boolean success = false;
//                    PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//                    if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                        SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.WOOPIE_THE_SWORD);
//                        if (skillContainer != null && skillContainer.getStack() == 1
//                                && entity.level() instanceof ServerLevel
//                                && skillContainer.getSkill() instanceof WoopieTheSwordSkill woopieTheSwordSkill) {
//                            success = true;
//                            woopieTheSwordSkill.getResourceType().consumer
//                                    .consume(skillContainer, serverPlayerPatch, woopieTheSwordSkill.getDefaultConsumptionAmount(serverPlayerPatch));
//                        }
//                    }
//                    if (success) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.RUSH_SWORD, 0.0F);
//                    } else {
//                        livingEntityPatch.playAnimationSynchronized(AnimsRuine.RUINE_AUTO_4, 0.0F);
//                    }
//                    return;
//                }
//            }
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.GREAT_SWORD.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsHerrscher.HERRSCHER_AUTO_2, 0.0F);
//                    return;
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.WOODEN_DOOR.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(WOMAnimations.TORMENT_CHARGED_ATTACK_2, 0.0F);
//                    return;
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.CRAFTING_TABLE.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(WOMAnimations.TORMENT_AIRSLAM, 0.0F);
//                    return;
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.LADDER.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(Animations.VINDICATOR_SWING_AXE3, 0.0F);
//                    return;
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.TRAPDOOR.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(Animations.VINDICATOR_SWING_AXE2, 0.0F);
//                    return;
//                }
//            }
//
//            if (holdingItem.getItem().equals(AnnoyingVillagersModItems.BLUE_FLAME_SWORD.get())) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (player.level() instanceof ServerLevel level) {
//                        double reach = player.getBlockReach();
//                        HitResult hitResult = player.pick(reach, 0.0F, false);
//
//                        if (hitResult.getType() == HitResult.Type.BLOCK) {
//                            BlockHitResult blockHit = (BlockHitResult) hitResult;
//                            BlockPos lookedPos = blockHit.getBlockPos();
//
//                            BlockState lookedState = level.getBlockState(lookedPos);
//
//                            if (lookedState.is(Blocks.SOUL_SAND) || lookedState.is(Blocks.SOUL_SOIL)) {
//                                BlockPos firePos = lookedPos.above();
//                                if (level.isEmptyBlock(firePos)) {
//                                    BlockState soulFireState = Blocks.SOUL_FIRE.defaultBlockState();
//                                    if (soulFireState.canSurvive(level, firePos)) {
//                                        level.setBlock(firePos, soulFireState, Block.UPDATE_ALL);
//                                        livingEntityPatch.playAnimationSynchronized(AVAnimations.BLUE_FLAME_SWORD, 0.0F);
//                                        holdingItem.hurtAndBreak(1, player, (serverPlayer1) -> serverPlayer1.broadcastBreakEvent(InteractionHand.MAIN_HAND));
//                                        return;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Check by categories
//            if (playerpatch == null) return;
//
//            ResourceLocation key = BuiltInRegistries.ITEM.getKey(holdingItem.getItem());
//            if (ModList.get().isLoaded("efn") && key.getNamespace().equals("efn")) return;
//
//            if (playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.AXE) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (!entity.getPersistentData().contains("AxeCombo")) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.AXE_HEAVY_AUTO_1, 0.0F);
//                        entity.getPersistentData().putDouble("AxeCombo", 1.0);
//                    } else if (entity.getPersistentData().getDouble("AxeCombo") == 1.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.AXE_HEAVY_AUTO_2, 0.0F);
//                        entity.getPersistentData().putDouble("AxeCombo", 2.0);
//                    } else if (entity.getPersistentData().getDouble("AxeCombo") == 2.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.AXE_FUN_SKILL, 0.0F);
//                        entity.getPersistentData().remove("AxeCombo");
//                    }
//                    return;
//                }
//            }
//
//            if ((playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.SWORD
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.LONGSWORD
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.TACHI
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.DAGGER)
//                    && (playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() == WeaponCategories.SWORD
//                    || playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() == WeaponCategories.TACHI
//                    || playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() == WeaponCategories.AXE)) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (!entity.getPersistentData().contains("DualSwordCombo")) {
//                        livingEntityPatch.playAnimationSynchronized(Animations.DAGGER_DUAL_DASH, 0.0F);
//                        entity.getPersistentData().putDouble("DualSwordCombo", 1.0);
//                    } else if (entity.getPersistentData().getDouble("DualSwordCombo") == 1.0) {
//                        livingEntityPatch.playAnimationSynchronized(Animations.LONGSWORD_AUTO2, 0.0F);
//                        entity.getPersistentData().putDouble("DualSwordCombo", 2.0);
//                    } else if (entity.getPersistentData().getDouble("DualSwordCombo") == 2.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.DUAL_DANCING_EDGE, 0.0F);
//                        entity.getPersistentData().putDouble("DualSwordCombo", 3.0);
//                    } else if (entity.getPersistentData().getDouble("DualSwordCombo") == 3.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.DUAL_SWORD_DANCING_EDGE, 0.0F);
//                        entity.getPersistentData().remove("DualSwordCombo");
//                    }
//                    return;
//                }
//            }
//
//            if ((playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.SWORD
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.LONGSWORD
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.TACHI
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.DAGGER
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.UCHIGATANA)
//                    && playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() != WeaponCategories.SWORD
//                    && playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() != WeaponCategories.TACHI
//                    && playerpatch.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() != WeaponCategories.AXE) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (!entity.getPersistentData().contains("SwordCombo")) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SWORD_HEAVY_AUTO_1, 0.0F);
//                        entity.getPersistentData().putDouble("SwordCombo", 1.0);
//                    } else if (entity.getPersistentData().getDouble("SwordCombo") == 1.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SWORD_HEAVY_AUTO_2, 0.0F);
//                        entity.getPersistentData().putDouble("SwordCombo", 2.0);
//                    } else if (entity.getPersistentData().getDouble("SwordCombo") == 2.0) {
//                        livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SWORD_HEAVY_AUTO_3, 0.0F);
//                        entity.getPersistentData().remove("SwordCombo");
//                    }
//                    return;
//                }
//            }
//
//            if (playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.GREATSWORD) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.GIANT_WHIRLWIND, 0.0F);
//                    return;
//                }
//            }
//
//            if (playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.FIST
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.NOT_WEAPON
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.BOW
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.CROSSBOW) {
//                if (entity.level() instanceof ServerLevel) {
//                    if (entity.isSprinting()) {
//                        if (entity.isShiftKeyDown()) {
//                            if (entity.level() instanceof ServerLevel) {
//                                livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.WHIRLWIND_KICK_LEFT, 0.0F);
//                            }
//                        } else {
//                            if (entity.level() instanceof ServerLevel) {
//                                livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.WHIRLWIND_KICK, 0.0F);
//                            }
//                        }
//                    } else {
//                        if (!entity.getPersistentData().contains("FistCombo")) {
//                            livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.FIST_LEFT, 0.0F);
//                            entity.getPersistentData().putDouble("FistCombo", 1.0);
//                        } else if (entity.getPersistentData().getDouble("FistCombo") == 1.0) {
//                            livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.FIST_UP, 0.0F);
//                            entity.getPersistentData().putDouble("FistCombo", 2.0);
//                        } else if (entity.getPersistentData().getDouble("FistCombo") == 2.0) {
//                            livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.FIST_DASH, 0.0F);
//                            entity.getPersistentData().remove("FistCombo");
//                        }
//                    }
//                    return;
//                }
//            }
//
//            if (playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.SPEAR
//                    || playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory() == WeaponCategories.TRIDENT) {
//                if (entity.level() instanceof ServerLevel) {
//                    livingEntityPatch.playAnimationSynchronized(AnimsPugilistSteve.SPEAR_THRUST, 0.0F);
//                }
//            }
//
//            if (ModList.get().isLoaded("refm")) {
//                if (EpicFightRapierMoveset.addRefmSpecialAttack(playerpatch, entity, livingEntityPatch)) {
//                    return;
//                }
//            }
//
//            if (ModList.get().isLoaded("cdmoveset")) {
//                if (EpicFightResurrection.addMoreSpecialAttack(playerpatch, entity, livingEntityPatch)) {
//                    return;
//                }
//            }
//        }
    }
}
