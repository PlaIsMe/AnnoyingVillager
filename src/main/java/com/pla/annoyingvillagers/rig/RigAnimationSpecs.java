package com.pla.annoyingvillagers.rig;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.clazz.HerobrineMob;
import com.pla.annoyingvillagers.clazz.HerobrineObsidianBlock;
import com.pla.annoyingvillagers.clazz.TridentMode;
import com.pla.annoyingvillagers.entity.*;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModBlocks;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModEntities;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModParticleTypes;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModSounds;
import com.pla.annoyingvillagers.item.*;
import com.pla.annoyingvillagers.network.ClientboundDiamondAttractorFx;
import com.pla.annoyingvillagers.network.ClientboundMuteExplosionAtPos;
import com.pla.annoyingvillagers.network.ClientboundWoopieSwordWindFx;
import com.pla.annoyingvillagers.potion.GroundStuckMobEffect;
import com.pla.annoyingvillagers.potion.ObedienceMobEffect;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.BlueDemonUtil;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.HerobrineUtil;
import com.pla.annoyingvillagers.util.RigPoseUtil;
import com.pla.annoyingvillagers.util.ScreenShakeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.pla.annoyingvillagers.rig.RigColliderAnchor.*;
import static com.pla.annoyingvillagers.rig.RigColliderPreset.*;
import static com.pla.annoyingvillagers.rig.RigColliderPreset.BODY;

public final class RigAnimationSpecs {
    private static final Map<RigAnimationId, RigAnimationSpec> SPECS = new EnumMap<>(RigAnimationId.class);

    private static final RigCollider RIGHT_FIST = RigCollider.of(RIGHT_HAND, FIST);
    private static final RigCollider LEFT_FIST = RigCollider.of(LEFT_HAND, FIST);
    private static final RigCollider RIGHT_DAGGER = RigCollider.of(RIGHT_TOOL, DAGGER);
    private static final RigCollider RIGHT_SWORD = RigCollider.of(RIGHT_TOOL, SWORD);
    private static final RigCollider LEFT_SWORD = RigCollider.of(LEFT_TOOL, SWORD);
    private static final RigCollider RIGHT_LONGSWORD = RigCollider.of(RIGHT_TOOL, LONGSWORD);
    private static final RigCollider LEFT_LONGSWORD = RigCollider.of(LEFT_TOOL, LONGSWORD);
    private static final RigCollider RIGHT_GREATSWORD = RigCollider.of(RIGHT_TOOL, GREATSWORD);
    private static final RigCollider RIGHT_SPEAR = RigCollider.of(RIGHT_TOOL, SPEAR);
    private static final RigCollider LEFT_SPEAR = RigCollider.of(LEFT_TOOL, SPEAR);
    private static final RigCollider RIGHT_AXE = RigCollider.of(RIGHT_TOOL, AXE);
    private static final RigCollider LEFT_AXE = RigCollider.of(LEFT_TOOL, AXE);
    private static final RigCollider RIGHT_TACHI = RigCollider.of(RIGHT_TOOL, TACHI);
    private static final RigCollider RIGHT_GLAIVE = RigCollider.of(RIGHT_TOOL, GLAIVE);
    private static final RigCollider RIGHT_SCYTHE = RigCollider.of(RIGHT_TOOL, SCYTHE);
    private static final RigCollider RIGHT_SLEDGEHAMMER = RigCollider.of(RIGHT_TOOL, SLEDGEHAMMER);
    private static final RigCollider RIGHT_FOOT = RigCollider.of(RIGHT_LOWER_LEG, FOOT);
    private static final RigCollider RIGHT_KNEE = RigCollider.of(RIGHT_LEG, FOOT);
    private static final RigCollider LEFT_FOOT = RigCollider.of(LEFT_LOWER_LEG, FOOT);
    private static final RigCollider RIGHT_ELBOW = RigCollider.of(RIGHT_ARM, BODY);
    private static final RigCollider LEFT_ELBOW = RigCollider.of(LEFT_ARM, BODY);

    static {
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_AIM_DOWN, 14, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_AIM_MID, 14, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_AIM_UP, 14, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_ATTACK_DOWN, 2, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_ATTACK_MID, 2, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BOW_ATTACK_UP, 2, RigAnimationPlaybackType.UPPER_BODY));

        put(RigAnimationSpec.attack(RigAnimationId.BASIC_ATTACK1, 12, false,
                RigAttackWindow.of(3, 8, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_ATTACK2, 12, false,
                RigAttackWindow.of(3, 8, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_ATTACK3, 12, false,
                RigAttackWindow.of(3, 5, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_ATTACK4, 13, false,
                RigAttackWindow.of(3, 5, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_DASH_ATTACK, 13, false,
                        RigAttackWindow.of(3, 8, RIGHT_SWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_JUMP_ATTACK, 13, true,
                        RigAttackWindow.of(3, 8, RIGHT_SWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_ULT, 33, false,
                        hookAt(2, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            if (mob.getMainHandItem().getItem() instanceof ThunderDiamondBladeItem) {
                                ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, mob, false);
                            }
                        }),
                        RigAttackWindow.of(5, 15, RIGHT_SWORD))
                .damageMultiplier(2.0F)
                .criticalChance(0.5F)
                .onHit((attacker, target, critical) -> {
                    if (!(attacker.getMainHandItem().getItem() instanceof DNAxHookedSwordItem)) return;
                    if (!(target instanceof Mob targetMob)) return;
                    ObedienceMobEffect.applyObedience(targetMob, attacker, 20 * 5);
                }));
        put(RigAnimationSpec.attack(RigAnimationId.BASIC_MOUNT_ATTACK, 12, false,
                RigAttackWindow.of(2, 6, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_ATTACK1, 12, false,
                RigAttackWindow.of(3, 8, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_ATTACK2, 12, false,
                RigAttackWindow.of(3, 8, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_ATTACK3, 15, false,
                RigAttackWindow.of(5, 12, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_DASH_ATTACK, 15, false,
                        RigAttackWindow.of(3, 8, RIGHT_SWORD, LEFT_SWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_JUMP_ATTACK, 13, true,
                        RigAttackWindow.of(3, 8, RIGHT_SWORD, LEFT_SWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_BASIC_ULT, 33, false,
                        hookAt(2, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            if (mob.getMainHandItem().getItem() instanceof ThunderDiamondBladeItem) {
                                ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, mob, false);
                            }

                            if (mob.getOffhandItem().getItem() instanceof ThunderDiamondBladeItem) {
                                ElectricPhaseEntity.spawnOnOwnerSword(serverLevel, mob, true);
                            }
                        }),
                        RigAttackWindow.of(5, 8, RIGHT_SWORD),
                        RigAttackWindow.of(8, 13, LEFT_SWORD),
                        RigAttackWindow.of(13, 18, RIGHT_SWORD))
                .damageMultiplier(2.0F)
                .criticalChance(0.5F)
                .onHit((attacker, target, critical) -> {
                    if (!(attacker.getMainHandItem().getItem() instanceof DNAxHookedSwordItem)) return;
                    if (!(target instanceof Mob targetMob)) return;

                    int durationTicks = attacker.getOffhandItem().getItem() instanceof DNAxHookedSwordItem ? 20 * 10 : 20 * 5;
                    ObedienceMobEffect.applyObedience(targetMob, attacker, durationTicks);
                }));

        put(RigAnimationSpec.attack(RigAnimationId.FIST_ATTACK1, 17, false,
                hookAt(4, mob -> summonObsidianHandBlocks(mob, RigAnimationId.FIST_ATTACK1, 2, true, 2)),
                RigAttackWindow.of(3, 8, LEFT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.FIST_ATTACK2, 17, false,
                hookAt(4, mob -> summonObsidianHandBlocks(mob, RigAnimationId.FIST_ATTACK2, 2, false, 2)),
                RigAttackWindow.of(3, 8, RIGHT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.FIST_ATTACK3, 17, false,
                hookAt(5, mob -> summonObsidianHandBlocks(mob, RigAnimationId.FIST_ATTACK3, 2, true, 3)),
                RigAttackWindow.of(3, 8, LEFT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.FIST_ATTACK4, 22, false,
                hookAt(10, mob -> {
                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                    BlockState state = getObsidianFistState(mob);
                    if (state == null) return;
                    HerobrineUtil.summonObsidianVerticalColumnInFront(serverLevel, mob, state, 3);
                }),
                RigAttackWindow.of(9, 17, RIGHT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.FIST_ATTACK5, 37, false,
                hookAt(12, mob -> {
                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                    BlockState state = getObsidianFistState(mob);
                    if (state == null) return;
                    HerobrineUtil.summonObsidianArcInFront(serverLevel, mob, state);
                }),
                RigAttackWindow.of(12, 20, LEFT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.FIST_DASH_ATTACK, 27, false,
                        RigAttackWindow.of(6, 15, RIGHT_FIST))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.FIST_JUMP_ATTACK, 17, true,
                        RigAttackWindow.of(6, 12, RIGHT_FIST))
                .damageMultiplier(1.5F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.FIST_ULT, 33, false,
                        RigAttackWindow.of(1, 2, LEFT_FIST),
                        RigAttackWindow.of(3, 4, RIGHT_FIST),
                        RigAttackWindow.of(5, 6, LEFT_FIST),
                        RigAttackWindow.of(7, 8, RIGHT_FIST),
                        RigAttackWindow.of(9, 10, LEFT_FIST),
                        RigAttackWindow.of(11, 12, RIGHT_FIST),
                        RigAttackWindow.of(13, 14, LEFT_FIST),
                        RigAttackWindow.of(15, 16, RIGHT_FIST))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.FIST_EXTRA_ATTACK, 32, false,
                hookAt(14, mob -> {
                    Vec3 legPosition = RigPoseUtil.getPartPosition(mob, RigAnimationId.FIST_EXTRA_ATTACK, 14.0F, RigPart.RIGHT_LOWER_LEG);
                    if (legPosition != null) throwObsidianProjectile(mob, legPosition);
                }),
                RigAttackWindow.of(9, 17, RIGHT_FOOT)));

        put(RigAnimationSpec.attack(RigAnimationId.KICK_ATTACK1, 25, false,
                RigAttackWindow.of(5, 12, RIGHT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.KICK_ATTACK2, 17, false,
                RigAttackWindow.of(5, 12, LEFT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.KICK_ATTACK3, 20, false,
                RigAttackWindow.of(5, 12, RIGHT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.KICK_ATTACK4, 17, false,
                RigAttackWindow.of(5, 12, LEFT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.KICK_COMBO_ATTACK, 28, false,
                        RigAttackWindow.of(8, 9, LEFT_FOOT),
                        RigAttackWindow.of(10, 11, LEFT_FOOT),
                        RigAttackWindow.of(12, 13, LEFT_FOOT),
                        RigAttackWindow.of(14, 15, LEFT_FOOT),
                        RigAttackWindow.of(16, 17, LEFT_FOOT))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.KICK_DASH_ATTACK, 19, false,
                        RigAttackWindow.of(2, 12, RIGHT_FOOT))
                .damageMultiplier(2.5F)
                .criticalChance(0.8F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.AXE_ATTACK1, 25, false,
                RigAttackWindow.of(5, 16, RIGHT_AXE)));
        put(RigAnimationSpec.attack(RigAnimationId.AXE_ATTACK2, 26, false,
                RigAttackWindow.of(6, 16, RIGHT_AXE)));
        put(RigAnimationSpec.attack(RigAnimationId.AXE_ATTACK3, 22, false,
                RigAttackWindow.of(5, 12, RIGHT_AXE)));
        put(RigAnimationSpec.attack(RigAnimationId.AXE_ATTACK4, 20, false,
                RigAttackWindow.of(5, 12, RIGHT_AXE)));
        put(RigAnimationSpec.attack(RigAnimationId.AXE_ATTACK5, 28, false,
                RigAttackWindow.of(5, 18, RIGHT_AXE)));
        put(RigAnimationSpec.attack(RigAnimationId.AXE_DASH_ATTACK, 32, false,
                        RigAttackWindow.of(11, 24, RIGHT_AXE))
                .damageMultiplier(1.2F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.AXE_JUMP_ATTACK, 32, true,
                        RigAttackWindow.of(4, 24, RIGHT_AXE))
                .damageMultiplier(2.5F)
                .criticalChance(0.8F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.AXE_ULT, 30, false,
                        RigAttackWindow.of(8, 25, RIGHT_AXE))
                .damageMultiplier(1.5F)
                .criticalChance(0.4F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_AXE_ULT, 60, false,
                        RigAttackWindow.of(3, 5, RIGHT_AXE),
                        RigAttackWindow.of(5, 8, LEFT_AXE),
                        RigAttackWindow.of(10, 12, LEFT_AXE),
                        RigAttackWindow.of(12, 15, RIGHT_AXE),
                        RigAttackWindow.of(15, 16, LEFT_AXE),
                        RigAttackWindow.of(18, 20, LEFT_AXE),
                        RigAttackWindow.of(20, 22, RIGHT_AXE),
                        RigAttackWindow.of(22, 24, LEFT_AXE),
                        RigAttackWindow.of(24, 27, RIGHT_AXE),
                        RigAttackWindow.of(27, 28, LEFT_AXE),
                        RigAttackWindow.of(28, 30, RIGHT_AXE),
                        RigAttackWindow.of(30, 32, LEFT_AXE),
                        RigAttackWindow.of(32, 34, RIGHT_AXE),
                        RigAttackWindow.of(34, 36, LEFT_AXE),
                        RigAttackWindow.of(36, 38, RIGHT_AXE),
                        RigAttackWindow.of(40, 44, LEFT_AXE))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EARTH_AXE_ULT, 33, RigAnimationPlaybackType.DEFAULT,
                hookAt(4, mob -> {
                    if (mob.level() instanceof ServerLevel serverLevel) {
                        EarthAxeItem.summonEarthWall(serverLevel, mob);
                    }
                })));
        put(RigAnimationSpec.attack(RigAnimationId.EARTH_AXE_EXTRA_ATTACK, 33, false,
                hookAt(20, mob -> {
                    if (mob.level() instanceof ServerLevel serverLevel) {
                        Vec3 bladePos = RigPoseUtil.getPartPosition(mob, RigAnimationId.EARTH_AXE_EXTRA_ATTACK, 20.0F, RigPart.RIGHT_WEAPON, Vec3.ZERO, 0.5D, 0.0D);
                        BlockPos liftPos = EarthAxeItem.findLiftableBlockUnderPoint(serverLevel, bladePos, 6, 1);
                        if (liftPos != null) {
                            EarthAxeItem.liftBlockAt(serverLevel, liftPos, mob);
                        }
                    }
                }),
                RigAttackWindow.of(6, 8, RIGHT_AXE)));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.GREATSWORD_IDLE, 60));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.GREATSWORD_RUN, 10));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.GREATSWORD_WALK, 14));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.CARRY, 54));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ATTACK1, 42, false,
                RigAttackWindow.of(12, 20, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ATTACK2, 38, false,
                RigAttackWindow.of(10, 17, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ATTACK3, 39, false,
                RigAttackWindow.of(9, 20, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ATTACK4, 32, false,
                RigAttackWindow.of(3, 12, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ATTACK5, 38, false,
                        groundSlamHook(18, RigAnimationId.GREATSWORD_ATTACK5, 1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(3, 6, RIGHT_GREATSWORD),
                        RigAttackWindow.of(8, 22, RIGHT_GREATSWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_DASH_ATTACK, 40, false,
                        RigAttackWindow.of(11, 20, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_JUMP_ATTACK, 48, true,
                        groundSlamHook(12, RigAnimationId.GREATSWORD_JUMP_ATTACK, 1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(9, 12, RIGHT_GREATSWORD))
                .damageMultiplier(1.8F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_EXTRA_ATTACK, 50, false,
                groundSlamHook(23, RigAnimationId.GREATSWORD_EXTRA_ATTACK, 1.4D, 0.7D, 35, 0.7D, 2.0D),
                RigAttackWindow.of(15, 25, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATSWORD_ULT, 47, false,
                        RigAttackWindow.of(2, 5, RIGHT_GREATSWORD),
                        RigAttackWindow.of(5, 8, RIGHT_GREATSWORD),
                        RigAttackWindow.of(10, 12, RIGHT_GREATSWORD),
                        RigAttackWindow.of(12, 15, RIGHT_GREATSWORD),
                        RigAttackWindow.of(15, 16, RIGHT_GREATSWORD),
                        RigAttackWindow.of(18, 20, RIGHT_GREATSWORD),
                        RigAttackWindow.of(20, 22, RIGHT_GREATSWORD),
                        RigAttackWindow.of(22, 24, RIGHT_GREATSWORD),
                        RigAttackWindow.of(24, 27, RIGHT_GREATSWORD),
                        RigAttackWindow.of(27, 28, RIGHT_GREATSWORD),
                        RigAttackWindow.of(28, 30, RIGHT_GREATSWORD),
                        RigAttackWindow.of(30, 32, RIGHT_GREATSWORD),
                        RigAttackWindow.of(32, 34, RIGHT_GREATSWORD),
                        RigAttackWindow.of(34, 36, RIGHT_GREATSWORD),
                        RigAttackWindow.of(37, 40, RIGHT_GREATSWORD))
                .damageMultiplier(2.5F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATAXE_ATTACK4, 42, false,
                RigAttackWindow.of(9, 20, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATAXE_ATTACK5, 31, false,
                RigAttackWindow.of(6, 15, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.GREATAXE_DASH_ATTACK, 48, false,
                        groundSlamHook(18, RigAnimationId.GREATAXE_DASH_ATTACK, 1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(9, 18, RIGHT_GREATSWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATAXE_JUMP_ATTACK, 42, true,
                        groundSlamHook(18, RigAnimationId.GREATAXE_JUMP_ATTACK, 1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(16, 23, RIGHT_GREATSWORD))
                .damageMultiplier(2.5F)
                .criticalChance(0.4F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GREATAXE_ULT, 67, false,
                        groundSlamHook(28, RigAnimationId.GREATAXE_ULT, 1.4D, 1.0D, 50, 1.0D, 3.5D),
                        RigAttackWindow.of(20, 28, RIGHT_GREATSWORD))
                .damageMultiplier(2.5F)
                .criticalChance(1.0F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ATTACK1, 28, false,
                RigAttackWindow.of(5, 12, RIGHT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ATTACK2, 31, false,
                RigAttackWindow.of(3, 12, RIGHT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ATTACK3, 38, false,
                RigAttackWindow.of(5, 12, RIGHT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ATTACK4, 37, false,
                RigAttackWindow.of(8, 18, RIGHT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ATTACK5, 33, false,
                        RigAttackWindow.of(6, 15, RIGHT_LONGSWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_DASH_ATTACK, 43, false,
                        RigAttackWindow.of(12, 25, RIGHT_LONGSWORD))
                .damageMultiplier(1.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_JUMP_ATTACK, 23, true,
                        RigAttackWindow.of(2, 8, RIGHT_LONGSWORD))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_EXTRA_ATTACK, 28, false,
                RigAttackWindow.of(5, 9, RIGHT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LONGSWORD_ULT, 32, false,
                        RigAttackWindow.of(7, 15, RIGHT_LONGSWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ATTACK1, 33, false,
                RigAttackWindow.of(10, 20, RIGHT_LONGSWORD, LEFT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ATTACK2, 47, false,
                RigAttackWindow.of(12, 20, RIGHT_LONGSWORD),
                RigAttackWindow.of(16, 25, LEFT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ATTACK3, 37, false,
                RigAttackWindow.of(13, 20, RIGHT_LONGSWORD),
                RigAttackWindow.of(14, 20, LEFT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ATTACK4, 31, false,
                RigAttackWindow.of(14, 20, RIGHT_LONGSWORD),
                RigAttackWindow.of(14, 20, LEFT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ATTACK5, 42, false,
                        RigAttackWindow.of(14, 20, RIGHT_LONGSWORD),
                        RigAttackWindow.of(14, 20, LEFT_LONGSWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_DASH_ATTACK, 42, false,
                        RigAttackWindow.of(14, 24, RIGHT_KNEE))
                .damageMultiplier(1.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_JUMP_ATTACK, 37, false,
                        RigAttackWindow.of(2, 4, RIGHT_LONGSWORD, LEFT_LONGSWORD),
                        RigAttackWindow.of(9, 20, RIGHT_LONGSWORD, LEFT_LONGSWORD))
                .criticalChance(0.8F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_EXTRA_ATTACK, 36, false,
                RigAttackWindow.of(10, 20, RIGHT_LONGSWORD),
                RigAttackWindow.of(10, 20, LEFT_LONGSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_LONGSWORD_ULT, 35, false,
                        RigAttackWindow.of(5, 15, RIGHT_LONGSWORD, LEFT_LONGSWORD))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.nonDamaging(RigAnimationId.SPEAR_IDLE, 60));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SPEAR_WALK, 13));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SPEAR_RUN, 13));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ATTACK1, 40, false,
                RigAttackWindow.of(11, 18, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ATTACK2, 36, false,
                RigAttackWindow.of(12, 19, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ATTACK3, 33, false,
                RigAttackWindow.of(4, 18, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ATTACK4, 38, false,
                RigAttackWindow.of(5, 10, RIGHT_SPEAR),
                RigAttackWindow.of(11, 16, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ATTACK5, 80, false,
                        RigAttackWindow.of(15, 30, RIGHT_SPEAR))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_DASH_ATTACK, 40, false,
                        RigAttackWindow.of(10, 20, RIGHT_SPEAR))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_JUMP_ATTACK, 40, true,
                        RigAttackWindow.of(10, 15, RIGHT_SPEAR),
                        RigAttackWindow.of(16, 23, RIGHT_SPEAR))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_EXTRA_ATTACK, 26, false,
                RigAttackWindow.of(6, 8, RIGHT_SPEAR),
                RigAttackWindow.of(10, 13, RIGHT_SPEAR),
                RigAttackWindow.of(15, 22, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.SPEAR_ULT, 33, false,
                        RigAttackWindow.of(12, 20, RIGHT_SPEAR))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.STAFF_ULT, 51, false,
                        RigAttackWindow.of(10, 15, RIGHT_SPEAR),
                        RigAttackWindow.of(33, 42, RIGHT_SPEAR))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SICKLE_ULT, 53, false,
                        RigAttackWindow.of(13, 16, RIGHT_SPEAR),
                        RigAttackWindow.of(16, 20, RIGHT_SPEAR),
                        RigAttackWindow.of(21, 28, RIGHT_SPEAR))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ATTACK1, 23, false,
                RigAttackWindow.of(1, 10, RIGHT_DAGGER)));
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ATTACK2, 17, false,
                RigAttackWindow.of(1, 10, RIGHT_DAGGER)));
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ATTACK3, 20, false,
                RigAttackWindow.of(3, 12, RIGHT_DAGGER)));
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ATTACK4, 20, false,
                RigAttackWindow.of(2, 10, RIGHT_DAGGER)));
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ATTACK5, 20, false,
                        RigAttackWindow.of(2, 3, RIGHT_DAGGER),
                        RigAttackWindow.of(4, 10, RIGHT_DAGGER))
                .criticalChance(0.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_DASH_ATTACK, 30, false,
                        RigAttackWindow.of(5, 7, RIGHT_DAGGER),
                        RigAttackWindow.of(8, 14, RIGHT_DAGGER))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_JUMP_ATTACK, 17, true,
                        RigAttackWindow.of(3, 12, RIGHT_DAGGER))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_EXTRA_ATTACK, 26, false,
                RigAttackWindow.of(2, 12, RIGHT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.DAGGER_ULT, 34, false,
                        RigAttackWindow.of(4, 6, RIGHT_DAGGER),
                        RigAttackWindow.of(7, 15, RIGHT_DAGGER))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.nonDamaging(RigAnimationId.TACHI_IDLE, 60));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.TACHI_RUN, 13));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.TACHI_WALK, 14));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ATTACK1, 31, false,
                RigAttackWindow.of(6, 15, RIGHT_TACHI)));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ATTACK2, 30, false,
                RigAttackWindow.of(6, 15, RIGHT_TACHI)));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ATTACK3, 30, false,
                RigAttackWindow.of(8, 15, RIGHT_TACHI)));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ATTACK4, 40, false,
                RigAttackWindow.of(6, 15, RIGHT_TACHI)));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ATTACK5, 50, false,
                        RigAttackWindow.of(6, 15, RIGHT_TACHI))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_DASH_ATTACK, 33, false,
                        RigAttackWindow.of(6, 15, RIGHT_TACHI))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_JUMP_ATTACK, 38, true,
                        RigAttackWindow.of(6, 15, RIGHT_TACHI))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_EXTRA_ATTACK, 41, false,
                RigAttackWindow.of(13, 22, RIGHT_TACHI)));
        put(RigAnimationSpec.attack(RigAnimationId.TACHI_ULT, 66, false,
                        RigAttackWindow.of(10, 18, RIGHT_TACHI),
                        RigAttackWindow.of(26, 38, RIGHT_TACHI))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ATTACK1, 23, false,
                RigAttackWindow.of(3, 12, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ATTACK2, 30, false,
                RigAttackWindow.of(6, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ATTACK3, 25, false,
                RigAttackWindow.of(4, 14, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ATTACK4, 33, false,
                RigAttackWindow.of(5, 16, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ATTACK5, 30, false,
                        RigAttackWindow.of(3, 15, RIGHT_SWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_DASH_ATTACK, 37, false,
                        RigAttackWindow.of(11, 20, RIGHT_SWORD))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_JUMP_ATTACK, 27, true,
                        RigAttackWindow.of(7, 15, RIGHT_SWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_EXTRA_ATTACK, 33, false,
                RigAttackWindow.of(8, 13, RIGHT_SWORD),
                RigAttackWindow.of(14, 20, RIGHT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORD_ULT, 27, false,
                        RigAttackWindow.of(1, 16, RIGHT_SWORD))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ATTACK1, 40, false,
                RigAttackWindow.of(7, 15, LEFT_SWORD),
                RigAttackWindow.of(11, 20, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ATTACK2, 40, false,
                RigAttackWindow.of(13, 22, RIGHT_SWORD),
                RigAttackWindow.of(13, 22, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ATTACK3, 40, false,
                RigAttackWindow.of(9, 18, RIGHT_SWORD),
                RigAttackWindow.of(9, 18, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ATTACK4, 40, false,
                RigAttackWindow.of(7, 9, LEFT_SWORD),
                RigAttackWindow.of(11, 15, LEFT_SWORD),
                RigAttackWindow.of(16, 22, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ATTACK5, 40, false,
                        RigAttackWindow.of(6, 10, RIGHT_SWORD),
                        RigAttackWindow.of(10, 16, LEFT_SWORD))
                .damageMultiplier(1.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_DASH_ATTACK, 33, false,
                        RigAttackWindow.of(5, 8, LEFT_SWORD),
                        RigAttackWindow.of(9, 15, RIGHT_SWORD))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_JUMP_ATTACK, 30, true,
                        RigAttackWindow.of(5, 9, RIGHT_SWORD),
                        RigAttackWindow.of(11, 20, LEFT_SWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_EXTRA_ATTACK, 38, false,
                RigAttackWindow.of(10, 20, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_SWORD_ULT, 33, false,
                        RigAttackWindow.of(2, 4, RIGHT_SWORD, LEFT_SWORD),
                        RigAttackWindow.of(4, 6, RIGHT_SWORD, LEFT_SWORD),
                        RigAttackWindow.of(6, 7, RIGHT_SWORD, LEFT_SWORD),
                        RigAttackWindow.of(9, 18, RIGHT_SWORD, LEFT_SWORD))
                .damageMultiplier(2.5F)
                .criticalChance(0.5F)
        );

        put(RigAnimationSpec.attack(RigAnimationId.BLACK_FIRE_SWORD_ULT, 30, false,
                hookAt(8, mob -> {
                    if (mob.level() instanceof ServerLevel serverLevel) {
                        BlackFireEntity.shootFromOwnerLook(serverLevel, mob);
                    }
                }),
                RigAttackWindow.of(2, 4)));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.DIAMOND_ATTRACTOR_ULT, 33, RigAnimationPlaybackType.DEFAULT,
                hookAt(2, mob -> {
                    if (mob.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), AnnoyingVillagersModSounds.DIAMOND_ATTRACTOR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                        AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundDiamondAttractorFx(mob));
                        DiamondAttractorSwordItem.pullWeapons(mob);
                    }
                })));
        put(RigAnimationSpec.attack(RigAnimationId.DIAMOND_BLASTER_ULT, 17, false,
                        RigAttackWindow.of(1, 15, RIGHT_SWORD))
                .onHit((attacker, target, critical) -> {
                    if (target instanceof Mob targetMob && RigStunController.supports(targetMob)) {
                        RigStunController.applyStun(targetMob, RigAnimationId.SUPER_KNOCK_BACK);
                    }
                    CommonUtil.pushEntityFromCaster(target, attacker);
                })
                .damageMultiplier(1.5F)
                .criticalChance(0.7F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.HOOK_SWORD_ULT1, 33, false,
                RigAttackWindow.of(8, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.HOOK_SWORD_ULT2, 40, false,
                RigAttackWindow.of(8, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.HOOK_SWORD_DUAL_ULT, 33, false,
                RigAttackWindow.of(5, 8, RIGHT_SWORD),
                RigAttackWindow.of(8, 13, LEFT_SWORD),
                RigAttackWindow.of(13, 18, RIGHT_SWORD))
        );
        put(RigAnimationSpec.attack(RigAnimationId.FLANKER_HOOK_SWORD_ULT, 33, false,
                RigAttackWindow.of(8, 18, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.HACKER_SWORD_ULT, 48, false,
                whiteAfterimageHooks(0, 7, 17, 27, 37),
                RigAttackWindow.of(9, 13, RIGHT_SWORD),
                RigAttackWindow.of(16, 24, RIGHT_SWORD),
                RigAttackWindow.of(27, 35, RIGHT_SWORD),
                RigAttackWindow.of(36, 45, RIGHT_SWORD),
                RigAttackWindow.of(46, 47, RIGHT_SWORD))
        );
        put(RigAnimationSpec.nonDamaging(RigAnimationId.GREAT_SWORD_ULT, 178));
        put(RigAnimationSpec.attack(RigAnimationId.WOOPIE_THE_SWORD_EXTRA_ULT, 24, false,
                woopieRushStartHook(RigAnimationId.WOOPIE_THE_SWORD_EXTRA_ULT),
                RigAttackWindow.of(5, 15, RIGHT_SWORD)));

        put(RigAnimationSpec.attack(RigAnimationId.WOOPIE_THE_SWORD_EXTRA_ULT_LEGENDARY, 21, false,
                woopieRushStartHook(RigAnimationId.WOOPIE_THE_SWORD_EXTRA_ULT_LEGENDARY),
                RigAttackWindow.of(5, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.WOOPIE_THE_SWORD_FLY, 13, false,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(0, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    mob.setDeltaMovement(Vec3.ZERO);
                                    mob.hasImpulse = true;
                                    mob.hurtMarked = true;

                                    Vec3 offHandPos = RigPoseUtil.getLeftWeaponPosition(mob, RigAnimationId.WOOPIE_THE_SWORD_FLY, 0.0F);
                                    Vec3 windPos = new Vec3(offHandPos.x, mob.getY() + 0.05D, offHandPos.z);
                                    AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundMuteExplosionAtPos(BlockPos.containing(windPos), 4));
                                    serverLevel.explode(mob, windPos.x, windPos.y, windPos.z, 2.0F, false, Level.ExplosionInteraction.NONE);
                                    AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundWoopieSwordWindFx(windPos));
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(12, mob -> RigAnimationController.play(mob, RigAnimationId.LEGENDARY_SWORD_ULT))
                        ),
                        RigAttackWindow.of(0, 10, LEFT_SWORD)).withVerticalMotion()
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.WOOPIE_THE_SWORD_ULT, 39, false,
                        hookAt(15, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                            Vec3 windPos = RigPoseUtil.getPartPosition(mob, RigAnimationId.WOOPIE_THE_SWORD_ULT,
                                    4.0F, RigPart.RIGHT_WEAPON, Vec3.ZERO, 4.3D, 0.5D);
                            AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                                    new ClientboundMuteExplosionAtPos(BlockPos.containing(windPos), 4));
                            serverLevel.explode(mob, windPos.x, windPos.y, windPos.z, 2.0F, false, Level.ExplosionInteraction.NONE);
                            AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob),
                                    new ClientboundWoopieSwordWindFx(windPos));
                        }),
                        RigAttackWindow.of(9, 20, RIGHT_SWORD))
                .damageMultiplier(2.1F));

        put(RigAnimationSpec.rolling(RigAnimationId.ROLL_BACKWARD, 13));
        put(RigAnimationSpec.rolling(RigAnimationId.ROLL_FORWARD, 13));
        put(RigAnimationSpec.rolling(RigAnimationId.STEP_FORWARD, 8));
        put(RigAnimationSpec.rolling(RigAnimationId.STEP_BACKWARD, 8));
        put(RigAnimationSpec.rolling(RigAnimationId.STEP_LEFT, 8));
        put(RigAnimationSpec.rolling(RigAnimationId.STEP_RIGHT, 8));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_WAKEUP_LEFT, 12));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_WAKEUP_RIGHT, 12));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.JUMP, 10));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EAT_OFFHAND, 32, RigAnimationPlaybackType.LEFT_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EAT_MAINHAND, 32, RigAnimationPlaybackType.MAIN_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.DIG_MAINHAND, 10, RigAnimationPlaybackType.MAIN_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.USE_MAINHAND, 8, RigAnimationPlaybackType.MAIN_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.DEATH, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.FALL, 167));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LANDING, 17));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LAYING_DEATH, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LAYING_DEATH_DEAD, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.IDLE, 48));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.IDLE_DUAL, 54));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNEEL, 48));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.MOUNT, 48));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SNEAK, 16));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SWIM, 20));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.WALK, 16));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.RUN, 12));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.RUN_HOLDING_DUAL_WEAPON, 11));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.RUN_HOLDING_WEAPON, 12));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SHIELD_OFFHAND, 56, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLOCK_SHIELD_OFFHAND, 4, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SHIELD_MAINHAND, 56, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLOCK_SHIELD_MAINHAND, 4, RigAnimationPlaybackType.UPPER_BODY));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SPINNING_WEAPON, 16, RigAnimationPlaybackType.MAIN_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.POINT_LEFT_HAND_TOWARD, 10, RigAnimationPlaybackType.LEFT_HAND,
                hookAt(5, mob -> {
                    if (mob instanceof ReaperHerobrineEntity reaper) {
                        reaper.castThunderFromSecondForm();
                    } else if (mob instanceof ShadowHerobrineEntity shadowHerobrine) {
                        shadowHerobrine.shootDarkObsAtTarget(2.0D);
                    }
                })));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.POINT_LEFT_HAND_MIDDLE, 10, RigAnimationPlaybackType.LEFT_HAND,
                hookAt(5, mob -> {
                    if (mob instanceof ReaperHerobrineEntity reaper) {
                        reaper.respawnHealingCrystalFromSecondForm();
                    } else if (mob instanceof ShadowHerobrineEntity shadowHerobrine) {
                        shadowHerobrine.spawnDarkObEntities();
                    }
                })));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.POINT_LEFT_HAND_UP, 10, RigAnimationPlaybackType.LEFT_HAND,
                hookAt(5, mob -> {
                    if (mob instanceof ReaperHerobrineEntity reaper) reaper.castMeteoriteFromSecondForm();
                })));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.HOOK_GUN, 23));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEFT_HAND_HOOK, 20, RigAnimationPlaybackType.LEFT_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEFT_HAND_HOOK_TOP, 20, RigAnimationPlaybackType.LEFT_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.RIGHT_HAND_HOOK, 20, RigAnimationPlaybackType.MAIN_HAND));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.RIGHT_HAND_HOOK_TOP, 20, RigAnimationPlaybackType.MAIN_HAND));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.EATING_ELITE_1, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EATING_ELITE_2, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EATING_ELITE_3, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.EATING_ELITE_4, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.ELITE_HOLD_WEAPON, 68));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.ELITE_RUN_WEAPON, 68));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HEROBRINE_ANIMATE, 100));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HEROBRINE_ASSISTANCE, 54));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HEROBRINE_RUN, 11));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HEROBRINE_SACRIFICING, 54));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HEROBRINE_STAGE_CHANGE, 54));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKED_ELITE, 27));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LOW_CLONE_ESCAPE, 20));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.PLAYER_HEROBRINE_POSSESSION, 80));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.PORTAL_SUMMON, 80));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.HIT_BACKWARD, 21));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HIT_LEFT, 19));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.HIT_RIGHT, 17));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_FORWARD, 78));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_BACKWARD, 47));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_LEFT, 78));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.KNOCKDOWN_RIGHT, 78));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.STUN_BACK, 37));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SUPER_KNOCK_BACK, 50));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEGENDARY_SWORD_KNOCKDOWN, 113));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SHOCKED, 17));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SHOCKED_LONG, 30));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_TWOHAND_RUN, 11));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_DIE, 660).invulnerable());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_DIE_START, 20).invulnerable());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_DIE_TICK, 20).invulnerable());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_STATE_TRANSFORM, 1117).invulnerable().dangerous());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_STATE_TRANSFORM_END, 20).invulnerable().dangerous());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL, 100, RigAnimationPlaybackType.DEFAULT,
                        List.of(
                                new RigAnimationSpec.RigTimedAnimationHook(RigAnimationSpec.RigTimedAnimationHook.START, mob -> {
                                    if (!(mob.level() instanceof ServerLevel)) return;
                                    if (!(mob instanceof BlueDemonEntity blueDemonEntity)) return;
                                    blueDemonEntity.setState(1);
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(2, mob -> {
                                    if (!(mob.level() instanceof ServerLevel)) return;
                                    if (!(mob instanceof BlueDemonEntity blueDemonEntity)) return;
                                    blueDemonEntity.playSound(AnnoyingVillagersModSounds.BLUE_DEMON_SAY_TRIDENT_FESTIVAL.get(), 1.0F, 1.0F);
                                }),
                                blueDemonBothHandEffectHook(4, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                RigAnimationSpec.RigTimedAnimationHook.at(6, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    if (mob instanceof BlueDemonEntity) BlueDemonTridentItem.summonMissingTridentAndAnimate(serverLevel, mob);
                                    ScreenShakeUtil.applyScreenShake(serverLevel, mob.blockPosition().getCenter(), 12.0D, 80, 8);
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(10, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    BlueDemonTridentItem.spawnDamageZones(serverLevel, mob);
                                    BlueDemonTridentItem.relaunchGroundedTridents(serverLevel, mob, true);
                                }),
                                blueDemonBothHandEffectHook(16, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                RigAnimationSpec.RigTimedAnimationHook.at(24, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    BlueDemonTridentItem.relaunchGroundedTridents(serverLevel, mob, true);
                                    playBlueDemonTridentEffect(mob, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL, 24, RigPart.RIGHT_HAND);
                                    playBlueDemonTridentEffect(mob, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL, 24, RigPart.LEFT_HAND);
                                }),
                                blueDemonBothHandEffectHook(36, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                blueDemonBothHandEffectHook(44, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                blueDemonBothHandEffectHook(56, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                blueDemonBothHandEffectHook(64, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL),
                                RigAnimationSpec.RigTimedAnimationHook.at(70, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    BlueDemonTridentItem.summonSuperLightningAtGroundedTridents(serverLevel, mob);
                                    BlueDemonTridentItem.setStormEnergy(mob.getMainHandItem(), 0);
                                    BlueDemonTridentItem.setStormEnergy(mob.getOffhandItem(), 0);
                                    if (mob instanceof BlueDemonEntity blueDemonEntity) {
                                        blueDemonEntity.beginStateTwoTransform();
                                        RigAnimationController.play(blueDemonEntity, RigAnimationId.BLUE_DEMON_STATE_TRANSFORM);
                                    }
                                }),
                                blueDemonBothHandEffectHook(76, RigAnimationId.BLUE_DEMON_TRIDENT_FESTIVAL)))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_EXTRA_ATTACK, 14, RigAnimationPlaybackType.DEFAULT,
                List.of(
                        blueDemonSpinHook(4),
                        blueDemonBothHandEffectHook(4, RigAnimationId.BLUE_DEMON_EXTRA_ATTACK),
                        blueDemonSpinHook(5),
                        blueDemonSpinHook(6),
                        blueDemonSpinHook(7),
                        blueDemonBothHandEffectHook(7, RigAnimationId.BLUE_DEMON_EXTRA_ATTACK),
                        blueDemonSpinHook(8),
                        blueDemonSpinHook(10))));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_EXTRA_ATTACK_LEGENDARY, 47, false,
                List.of(
                        blueDemonHandEffectHook(2, RigAnimationId.BLUE_DEMON_EXTRA_ATTACK_LEGENDARY, RigPart.LEFT_HAND),
                        blueDemonThrowTridentHook(8, RigAnimationId.BLUE_DEMON_EXTRA_ATTACK_LEGENDARY, InteractionHand.OFF_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(8)),
                RigAttackWindow.of(10, 13, RIGHT_SPEAR),
                RigAttackWindow.of(14, 16, LEFT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK1, 23, false,
                RigAttackWindow.of(5, 15, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK2, 38, false,
                RigAttackWindow.of(10, 13, RIGHT_SPEAR),
                RigAttackWindow.of(14, 25, LEFT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK3, 32, false,
                List.of(
                        blueDemonHandEffectHook(2, RigAnimationId.BLUE_DEMON_ATTACK3, RigPart.RIGHT_HAND),
                        blueDemonHandEffectHook(6, RigAnimationId.BLUE_DEMON_ATTACK3, RigPart.RIGHT_HAND),
                        blueDemonSoundHook(10, SoundEvents.TRIDENT_HIT_GROUND, 1.0F, 1.0F),
                        blueDemonHandEffectHook(10, RigAnimationId.BLUE_DEMON_ATTACK3, RigPart.RIGHT_HAND),
                        RigAnimationSpec.RigTimedAnimationHook.at(12, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            Vec3 forward = Vec3.directionFromRotation(0.0F, mob.yBodyRot).scale(2.0D);
                            Vec3 weaponEdge = mob.position().add(forward.x, forward.y - 0.24D, forward.z);
                            BlockHitResult hitResult = serverLevel.clip(new ClipContext(mob.position().add(0.0D, 0.1D, 0.0D), weaponEdge, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                            Vec3 slamStartPos;

                            if (hitResult.getType() == HitResult.Type.BLOCK) {
                                Direction direction = hitResult.getDirection();
                                BlockPos collidePos = hitResult.getBlockPos().offset(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                                if (!CommonUtil.canTransferShockWave(serverLevel, collidePos, serverLevel.getBlockState(collidePos))) collidePos = collidePos.below();
                                slamStartPos = new Vec3(collidePos.getX(), collidePos.getY(), collidePos.getZ());
                            } else {
                                slamStartPos = weaponEdge.subtract(0.0D, 1.0D, 0.0D);
                            }

                            CommonUtil.circleSlamFracture(mob, serverLevel, slamStartPos, 1.2D);
                        })),
                RigAttackWindow.of(10, 20, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK4, 17, false,
                RigAttackWindow.of(5, 15, LEFT_SPEAR, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK5, 27, false,
                List.of(
                        blueDemonSoundHook(4, SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F),
                        blueDemonHandEffectHook(4, RigAnimationId.BLUE_DEMON_ATTACK5, RigPart.RIGHT_HAND),
                        blueDemonSoundHook(7, SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F),
                        blueDemonHandEffectHook(7, RigAnimationId.BLUE_DEMON_ATTACK5, RigPart.RIGHT_HAND),
                        RigAnimationSpec.RigTimedAnimationHook.at(16, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            Vec3 tridentTip = RigPoseUtil.getRightWeaponPosition(mob, RigAnimationId.BLUE_DEMON_ATTACK5, 16, 1.2D);
                            if (tridentTip == null) return;

                            BlockPos.MutableBlockPos checkPos = BlockPos.containing(tridentTip).mutable();
                            while (checkPos.getY() > serverLevel.getMinBuildHeight() && !serverLevel.getBlockState(checkPos).isSolidRender(serverLevel, checkPos)) checkPos.move(0, -1, 0);
                            if (!serverLevel.getBlockState(checkPos).isSolidRender(serverLevel, checkPos)) return;

                            TridentLightningBolt lightningBolt = new TridentLightningBolt(AnnoyingVillagersModEntities.TRIDENT_LIGHTNING_BOLT.get(), serverLevel);
                            lightningBolt.setOwner(mob);
                            lightningBolt.moveTo(checkPos.getX() + 0.5D, checkPos.getY() + 1.0D, checkPos.getZ() + 0.5D);
                            serverLevel.addFreshEntity(lightningBolt);
                        })),
                RigAttackWindow.of(15, 25, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_ATTACK6, 44, false,
                List.of(
                        blueDemonBothHandEffectHook(8, RigAnimationId.BLUE_DEMON_ATTACK6),
                        blueDemonBothHandEffectHook(14, RigAnimationId.BLUE_DEMON_ATTACK6),
                        blueDemonBothHandEffectHook(20, RigAnimationId.BLUE_DEMON_ATTACK6)),
                RigAttackWindow.of(6, 8, RIGHT_SPEAR),
                RigAttackWindow.of(8, 10, LEFT_SPEAR),
                RigAttackWindow.of(10, 12, RIGHT_SPEAR),
                RigAttackWindow.of(12, 14, LEFT_SPEAR),
                RigAttackWindow.of(14, 16, RIGHT_SPEAR),
                RigAttackWindow.of(16, 18, LEFT_SPEAR),
                RigAttackWindow.of(25, 35, RIGHT_SPEAR, LEFT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_DASH_ATTACK, 29, false,
                RigAttackWindow.of(7, 20, RIGHT_SPEAR, LEFT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_JUMP_ATTACK, 41, true,
                RigAttackWindow.of(5, 15, RIGHT_SPEAR, LEFT_SPEAR),
                RigAttackWindow.of(18, 25, RIGHT_SPEAR, LEFT_SPEAR),
                RigAttackWindow.of(28, 35, RIGHT_SPEAR, LEFT_SPEAR)));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_ULT, 100, RigAnimationPlaybackType.DEFAULT,
                        List.of(
                                blueDemonBothHandEffectHook(4, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(16, RigAnimationId.BLUE_DEMON_ULT),
                                RigAnimationSpec.RigTimedAnimationHook.at(20, mob -> {
                                    if (mob.level() instanceof ServerLevel serverLevel) BlueDemonTridentItem.spawnDamageZones(serverLevel, mob);
                                }),
                                blueDemonBothHandEffectHook(24, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(36, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(44, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(56, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(64, RigAnimationId.BLUE_DEMON_ULT),
                                blueDemonBothHandEffectHook(76, RigAnimationId.BLUE_DEMON_ULT)))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_ATTACK1, 28, false,
                List.of(
                        blueDemonHandEffectHook(0, RigAnimationId.BLUE_DEMON_THROW_ATTACK1, RigPart.RIGHT_HAND),
                        blueDemonThrowTridentHook(1, RigAnimationId.BLUE_DEMON_THROW_ATTACK1, InteractionHand.MAIN_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(1)),
                RigAttackWindow.of(4, 6, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_ATTACK2, 43, false,
                List.of(
                        blueDemonHandEffectHook(2, RigAnimationId.BLUE_DEMON_THROW_ATTACK2, RigPart.LEFT_HAND),
                        blueDemonThrowTridentHook(12, RigAnimationId.BLUE_DEMON_THROW_ATTACK2, InteractionHand.OFF_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(12)),
                RigAttackWindow.of(10, 13, RIGHT_SPEAR),
                RigAttackWindow.of(14, 16, LEFT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_ATTACK3, 23, false,
                List.of(
                        blueDemonHandEffectHook(2, RigAnimationId.BLUE_DEMON_THROW_ATTACK3, RigPart.RIGHT_WEAPON),
                        RigAnimationSpec.RigTimedAnimationHook.at(6, mob -> {
                            playBlueDemonTridentEffect(mob, RigAnimationId.BLUE_DEMON_THROW_ATTACK3, 6, RigPart.RIGHT_WEAPON);
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                            BlueDemonThunderBeamEntity beam = new BlueDemonThunderBeamEntity(AnnoyingVillagersModEntities.BLUE_DEMON_THUNDER_BEAM.get(), serverLevel, mob, 10, 6, 7.5F);
                            beam.initSpawnState();
                            serverLevel.addFreshEntity(beam);
                        }),
                        blueDemonHandEffectHook(10, RigAnimationId.BLUE_DEMON_THROW_ATTACK3, RigPart.RIGHT_WEAPON)),
                RigAttackWindow.of(6, 10, RIGHT_FIST),
                RigAttackWindow.of(10, 14, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_ATTACK4, 43, false,
                List.of(
                        blueDemonBothHandEffectHook(9, RigAnimationId.BLUE_DEMON_THROW_ATTACK4),
                        blueDemonThrowTridentHook(9, RigAnimationId.BLUE_DEMON_THROW_ATTACK4, InteractionHand.OFF_HAND, TridentMode.LIGHTNING),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(9),
                        blueDemonThrowTridentHook(9, RigAnimationId.BLUE_DEMON_THROW_ATTACK4, InteractionHand.MAIN_HAND, TridentMode.LIGHTNING),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(9)),
                RigAttackWindow.of(11, 24)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_ATTACK5, 40, false,
                List.of(
                        blueDemonSoundHook(4, SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F),
                        blueDemonHandEffectHook(4, RigAnimationId.BLUE_DEMON_THROW_ATTACK5, RigPart.RIGHT_HAND),
                        blueDemonSoundHook(7, SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F),
                        blueDemonHandEffectHook(7, RigAnimationId.BLUE_DEMON_THROW_ATTACK5, RigPart.RIGHT_HAND),
                        blueDemonHandEffectHook(6, RigAnimationId.BLUE_DEMON_THROW_ATTACK5, RigPart.RIGHT_HAND),
                        blueDemonThrowTridentHook(14, RigAnimationId.BLUE_DEMON_THROW_ATTACK5, InteractionHand.MAIN_HAND, TridentMode.EXPLOSION),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(15)),
                RigAttackWindow.of(15, 25, RIGHT_SPEAR)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_DASH_ATTACK, 42, false,
                List.of(
                        blueDemonHandEffectHook(3, RigAnimationId.BLUE_DEMON_THROW_DASH_ATTACK, RigPart.LEFT_HAND),
                        blueDemonThrowTridentHook(3, RigAnimationId.BLUE_DEMON_THROW_DASH_ATTACK, InteractionHand.OFF_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(3),
                        blueDemonHandEffectHook(5, RigAnimationId.BLUE_DEMON_THROW_DASH_ATTACK, RigPart.RIGHT_HAND),
                        blueDemonThrowTridentHook(5, RigAnimationId.BLUE_DEMON_THROW_DASH_ATTACK, InteractionHand.MAIN_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(5)),
                RigAttackWindow.of(7, 27)));
        put(RigAnimationSpec.attack(RigAnimationId.BLUE_DEMON_THROW_JUMP_ATTACK, 30, true,
                List.of(
                        blueDemonBothHandEffectHook(4, RigAnimationId.BLUE_DEMON_THROW_JUMP_ATTACK),
                        blueDemonThrowTridentHook(4, RigAnimationId.BLUE_DEMON_THROW_JUMP_ATTACK, InteractionHand.OFF_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(4),
                        blueDemonThrowTridentHook(4, RigAnimationId.BLUE_DEMON_THROW_JUMP_ATTACK, InteractionHand.MAIN_HAND, TridentMode.DEFAULT),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(4)),
                RigAttackWindow.of(7, 9, RIGHT_SPEAR, LEFT_SPEAR),
                RigAttackWindow.of(9, 12, RIGHT_SPEAR, LEFT_SPEAR)));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.BLUE_DEMON_THROW_ULT, 100, RigAnimationPlaybackType.DEFAULT,
                        List.of(
                                blueDemonBothHandEffectHook(4, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(16, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                RigAnimationSpec.RigTimedAnimationHook.at(20, mob -> {
                                    if (mob.level() instanceof ServerLevel serverLevel) BlueDemonTridentItem.relaunchGroundedTridents(serverLevel, mob);
                                }),
                                blueDemonBothHandEffectHook(24, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(36, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(44, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(56, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(64, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                blueDemonBothHandEffectHook(76, RigAnimationId.BLUE_DEMON_THROW_ULT),
                                RigAnimationSpec.RigTimedAnimationHook.at(80, mob -> {
                                    if (mob.level() instanceof ServerLevel serverLevel) BlueDemonTridentItem.summonLightningAtGroundedTridents(serverLevel, mob);
                                })))
                .invulnerable()
                .dangerous()
        );

        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEGENDARY_SWORD_IDLE, 54));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEGENDARY_SWORD_WALK, 17));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.LEGENDARY_SWORD_RUN, 11));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ATTACK1, 50, false,
                RigAttackWindow.of(12, 20, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ATTACK2, 44, false,
                RigAttackWindow.of(10, 18, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ATTACK3, 44, false,
                RigAttackWindow.of(9, 19, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ATTACK4, 39, false,
                        RigAttackWindow.of(5, 15, RIGHT_GREATSWORD))
                .onHit((attacker, target, critical) ->
                        knockUpTarget(target, 2.85D))
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ATTACK5, 41, false,
                List.of(
                        RigAnimationSpec.RigTimedAnimationHook.at(0, mob -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, false));
                            CommonUtil.stunImmunity(mob, 30, 10);
                        }),
                        groundSlamTimedHook(14, RigAnimationId.LEGENDARY_SWORD_ATTACK5, 1.5D, 0.7D, 35, 0.7D, 2.5D),
                        RigAnimationSpec.RigTimedAnimationHook.at(14, mob -> {
                            if (mob.level() instanceof ServerLevel serverLevel) ScreenShakeUtil.applyScreenShake(serverLevel, mob.position(), 12.0D, 20, 2);
                        })),
                RigAttackWindow.of(3, 15, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_EXTRA_ATTACK, 46, false,
                RigAttackWindow.of(10, 20, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_JUMP_ATTACK, 53, true,
                List.of(
                        whiteAfterimageHook(3),
                        whiteAfterimageHook(5),
                        whiteAfterimageHook(7),
                        whiteAfterimageHook(9),
                        whiteAfterimageHook(11),
                        whiteAfterimageHook(13),
                        whiteAfterimageHook(15),
                        whiteAfterimageHook(17),
                        groundSlamTimedHook(18, RigAnimationId.LEGENDARY_SWORD_JUMP_ATTACK, 1.5D, 0.8D, 45, 0.7D, 2.5D)),
                RigAttackWindow.of(2, 6, RIGHT_GREATSWORD),
                RigAttackWindow.of(16, 18, RIGHT_GREATSWORD),
                RigAttackWindow.of(19, 25, RIGHT_GREATSWORD))
                .invulnerable()
        );
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_DASH_ATTACK, 25, false,
                whiteAfterimageHooks(3, 5, 7, 9, 11),
                RigAttackWindow.of(3, 16, RIGHT_GREATSWORD)).invulnerable());
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_ULT, 40, false,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(0, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                                    serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), AnnoyingVillagersModSounds.HEAVY_ATTACK_START.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                                    serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), AnnoyingVillagersModSounds.HEAVY_ATTACK_LEGENDARY_SWORD.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                                    serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), AnnoyingVillagersModSounds.HEAVY_ATTACK_LEGENDARY_SWORD_2.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, mob.getX(), mob.getY(), mob.getZ(), 15, 0.0D, 0.0D, 0.0D, 0.2D);
                                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, mob.getX(), mob.getEyeY(), mob.getZ(), 100, 0.0D, 0.0D, 0.0D, 0.5D);
                                }),
                                groundSlamTimedHook(10, RigAnimationId.LEGENDARY_SWORD_ULT, 2.0D, 0.8D, 50, 0.6D, 2.5D),
                                RigAnimationSpec.RigTimedAnimationHook.at(10, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                                    Vec3 legendarySwordPos = RigPoseUtil.getRightWeaponPosition(mob, RigAnimationId.LEGENDARY_SWORD_ULT, 10, 1.5D);

                                    BlockPos centerPos = BlockPos.containing(legendarySwordPos);
                                    for (int radius = 1; radius <= 6; radius++) {
                                        int delayTicks = (radius - 1) * 2;
                                        int ringRadius = radius;
                                        new DelayedTask(delayTicks) {
                                            @Override
                                            public void run() {
                                                if (mob.isRemoved()) return;
                                                LegendarySwordItem.spawnCircleRing(serverLevel, centerPos, ringRadius, mob);
                                            }
                                        };
                                    }
                                })),
                        RigAttackWindow.of(10, 20, RIGHT_GREATSWORD))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_EXTRA_ULT, 45, false,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(11, mob -> {
                                    if (mob instanceof AngrySteveEntity angrySteve) angrySteve.startLegendaryAwakening();
                                })),
                        RigAttackWindow.of(11, 15, RIGHT_GREATSWORD))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_DUAL_AUTO1, 49, false,
                RigAttackWindow.of(10, 13, RIGHT_GREATSWORD),
                RigAttackWindow.of(14, 16, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_DUAL_AUTO2, 42, false,
                RigAttackWindow.of(7, 10, LEFT_SWORD),
                RigAttackWindow.of(11, 18, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.LEGENDARY_SWORD_DUAL_AUTO3, 40, false,
                RigAttackWindow.of(9, 13, RIGHT_GREATSWORD),
                RigAttackWindow.of(14, 20, LEFT_SWORD)));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.AEGIS_HEROBRINE_IDLE, 50));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.AEGIS_HEROBRINE_GUARD, 50));
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_ATTACK1, 33, false,
                RigAttackWindow.of(2, 10, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_ATTACK2, 23, false,
                RigAttackWindow.of(2, 12, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_ATTACK3, 23, false,
                RigAttackWindow.of(4, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_ATTACK4, 40, false,
                        RigAttackWindow.of(6, 13, RIGHT_FIST),
                        RigAttackWindow.of(14, 22, RIGHT_SWORD))
                .damageMultiplier(2.0F)
                .criticalChance(0.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_ATTACK5, 80, false,
                        groundSlamHook(28, RigAnimationId.AEGIS_HEROBRINE_ATTACK5,
                                1.4D, 1.0D, 50, 1.0D, 3.5D),
                        RigAttackWindow.of(14, 28, RIGHT_SWORD))
                .damageMultiplier(2.0F)
                .criticalChance(0.5F)
                .onHit((attacker, target, critical) ->
                        knockUpTarget(target, 1.85D))
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_DASH_ATTACK, 45, false,
                RigAttackWindow.of(3, 8, LEFT_FOOT),
                RigAttackWindow.of(9, 20, LEFT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_JUMP_ATTACK, 70, true,
                groundSlamHook(18, RigAnimationId.AEGIS_HEROBRINE_JUMP_ATTACK,
                        1.4D, 1.0D, 50, 1.0D, 3.5D),
                RigAttackWindow.of(12, 20, RIGHT_SWORD)));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.AEGIS_HEROBRINE_ULT, 13, RigAnimationPlaybackType.DEFAULT,
                hookAt(2, mob -> {
                    if (!(mob.level() instanceof ServerLevel)) return;
                    if (mob instanceof AegisHerobrineEntity aegisHerobrineEntity) {
                        aegisHerobrineEntity.fireSecondFormShieldShot();
                    }
                })
        ).dangerous().invulnerable());
        put(RigAnimationSpec.attack(RigAnimationId.AEGIS_HEROBRINE_EXTRA_ATTACK, 33, false,
                        RigAttackWindow.of(4, 5, RIGHT_SWORD),
                        RigAttackWindow.of(6, 7, RIGHT_SWORD),
                        RigAttackWindow.of(8, 9, RIGHT_SWORD),
                        RigAttackWindow.of(10, 11, RIGHT_SWORD),
                        RigAttackWindow.of(12, 13, RIGHT_SWORD),
                        RigAttackWindow.of(14, 15, RIGHT_SWORD),
                        RigAttackWindow.of(16, 17, RIGHT_SWORD),
                        RigAttackWindow.of(20, 22, RIGHT_SWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
                .dangerous()
                .invulnerable()
        );

        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ATTACK1, 42, false,
                RigAttackWindow.of(3, 9, RIGHT_GLAIVE),
                RigAttackWindow.of(11, 18, RIGHT_GLAIVE)));
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ATTACK2, 41, false,
                RigAttackWindow.of(12, 20, RIGHT_GLAIVE)));
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ATTACK3, 50, false,
                RigAttackWindow.of(13, 18, RIGHT_GLAIVE),
                RigAttackWindow.of(19, 27, RIGHT_GLAIVE)));
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ATTACK4, 47, false,
                        RigAttackWindow.of(10, 18, RIGHT_GLAIVE),
                        RigAttackWindow.of(21, 30, RIGHT_GLAIVE))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ATTACK5, 42, false,
                        RigAttackWindow.of(2, 4, RIGHT_GLAIVE),
                        RigAttackWindow.of(5, 10, RIGHT_GLAIVE),
                        RigAttackWindow.of(11, 20, RIGHT_GLAIVE))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_DASH_ATTACK, 44, false,
                RigAttackWindow.of(10, 13, RIGHT_GLAIVE),
                RigAttackWindow.of(14, 20, RIGHT_GLAIVE),
                RigAttackWindow.of(22, 28, RIGHT_GLAIVE)));
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_JUMP_ATTACK,40,true,
                        List.of(RigAnimationSpec.RigTimedAnimationHook.at(8,mob -> {
                            if (mob.onGround()) return;
                            Vec3 motion = mob.getDeltaMovement();
                            mob.setDeltaMovement(motion.x,Math.min(motion.y,-0.6D),motion.z);
                            mob.hasImpulse = true;
                            mob.hurtMarked = true;
                        }), groundSlamTimedHook(13,RigAnimationId.GLAIVE_HEROBRINE_JUMP_ATTACK,1.4D,0.8D,45,0.7D,2.5D)),
                        RigAttackWindow.of(1,5,RIGHT_GLAIVE),
                        RigAttackWindow.of(6,9,RIGHT_GLAIVE),
                        RigAttackWindow.of(10,18,RIGHT_GLAIVE))
                .damageMultiplier(1.5F)
                .criticalChance(0.5F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_ULT,49,false,
                        hookAt(23,mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                            EnderGlaiveItem.spawnVacumSlise(serverLevel, mob, EnderGlaiveItem.DEFAULT_DAMAGE);
                            if (mob instanceof GlaiveHerobrineEntity glaiveHerobrineEntity && glaiveHerobrineEntity.getState() == 1) glaiveHerobrineEntity.consumeSecondFormAction();
                        }),
                        RigAttackWindow.of(17,22,RIGHT_GLAIVE))
                .damageMultiplier(2.0F)
                .criticalChance(0.7F)
                .dangerous()
                .invulnerable()
        );
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_EXTRA_ULT,54,false,
                        hookAt(27,mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                            EnderGlaiveItem.spawnVacumSlise(serverLevel, mob, EnderGlaiveItem.DEFAULT_DAMAGE);
                            if (mob instanceof GlaiveHerobrineEntity glaiveHerobrineEntity && glaiveHerobrineEntity.getState() == 1) glaiveHerobrineEntity.consumeSecondFormAction();
                        }),
                        RigAttackWindow.of(23,26,RIGHT_GLAIVE))
                .damageMultiplier(2.0F)
                .criticalChance(0.7F)
                .dangerous()
                .invulnerable()
        );
        put(RigAnimationSpec.attack(RigAnimationId.GLAIVE_HEROBRINE_EXTRA_ATTACK, 31, false,
                RigAttackWindow.of(1, 8, RIGHT_GLAIVE)));

        put(RigAnimationSpec.nonDamaging(RigAnimationId.REAPER_HEROBRINE_IDLE, 40));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ATTACK1, 42, false,
                RigAttackWindow.of(12, 20, RIGHT_SCYTHE)));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ATTACK2, 33, false,
                RigAttackWindow.of(3, 15, RIGHT_SCYTHE)));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ATTACK3, 29, false,
                RigAttackWindow.of(4, 11, RIGHT_SCYTHE),
                RigAttackWindow.of(12, 20, RIGHT_SCYTHE)));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ATTACK4, 64, false,
                        RigAttackWindow.of(12, 20, RIGHT_SCYTHE))
                .damageMultiplier(1.2F)
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ATTACK5, 40, false,
                        RigAttackWindow.of(10, 22, RIGHT_SCYTHE))
                .damageMultiplier(1.2F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_DASH_ATTACK, 43, false,
                RigAttackWindow.of(12, 20, RIGHT_SCYTHE)));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_JUMP_ATTACK, 43, true,
                RigAttackWindow.of(11, 18, RIGHT_SCYTHE)));
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_EXTRA_ATTACK, 32, false,
                        RigAttackWindow.of(3, 5, RIGHT_SCYTHE),
                        RigAttackWindow.of(6, 8, RIGHT_SCYTHE),
                        RigAttackWindow.of(9, 11, RIGHT_SCYTHE),
                        RigAttackWindow.of(12, 14, RIGHT_SCYTHE),
                        RigAttackWindow.of(15, 22, RIGHT_SCYTHE))
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.REAPER_HEROBRINE_ULT, 58, false,
                        hookAt(22, mob -> {
                            if (!(mob instanceof ReaperHerobrineEntity reaper) || !(mob.level() instanceof ServerLevel serverLevel)) return;
                            CommonUtil.spawnGroundSlamFracture(reaper, serverLevel, reaper.position(),
                                    1.25D, 60, 1.0D, 5.0D);
                            reaper.completePendingDragonSummon();
                        }),
                        RigAttackWindow.of(20, 26))
                .dangerous()
                .invulnerable());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.REAPER_HEROBRINE_EXTRA_ULT, 23).invulnerable());

        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK1, 42, false,
                        RigAttackWindow.of(3, 15, RIGHT_SLEDGEHAMMER))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK2, 33, false,
                        RigAttackWindow.of(3, 14, RIGHT_SLEDGEHAMMER))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK3, 46, false,
                groundSlamHook(15, RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK3,
                        1.4D, 0.7D, 35, 0.7D, 1.2D),
                RigAttackWindow.of(14, 25, RIGHT_SLEDGEHAMMER)));
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK4, 35, false,
                RigAttackWindow.of(3, 18, RIGHT_SLEDGEHAMMER)));
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK5, 32, false,
                        groundSlamHook(11, RigAnimationId.SLEDGEHAMMER_HEROBRINE_ATTACK5,
                                1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(3, 7, RIGHT_SLEDGEHAMMER),
                        RigAttackWindow.of(8, 16, RIGHT_SLEDGEHAMMER))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK, 51, false,
                        List.of(
                                groundSlamTimedHook(10, RigAnimationId.SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK,
                                        1.4D, 0.7D, 35, 0.7D, 2.0D),
                                groundSlamTimedHook(17, RigAnimationId.SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK,
                                        1.4D, 0.7D, 35, 0.7D, 2.0D),
                                groundSlamTimedHook(28, RigAnimationId.SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK,
                                        1.4D, 0.7D, 35, 0.7D, 2.0D)
                        ),
                        RigAttackWindow.of(9, 14, RIGHT_SLEDGEHAMMER),
                        RigAttackWindow.of(16, 25, RIGHT_SLEDGEHAMMER),
                        RigAttackWindow.of(27, 36, RIGHT_SLEDGEHAMMER))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_DASH_ATTACK, 38, false,
                RigAttackWindow.of(11, 20, RIGHT_SLEDGEHAMMER)));
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_JUMP_ATTACK, 45, true,
                        List.of(
                                groundSlamTimedHook(25, RigAnimationId.SLEDGEHAMMER_HEROBRINE_JUMP_ATTACK,
                                        1.4D, 0.8D, 45, 0.7D, 3.0D)
                        ),
                        RigAttackWindow.of(16, 25, RIGHT_SLEDGEHAMMER))
                .damageMultiplier(1.5F)
                .criticalChance(0.4F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SLEDGEHAMMER_HEROBRINE_ULT, 45, false,
                        List.of(
                                groundSlamTimedHook(20, RigAnimationId.SLEDGEHAMMER_HEROBRINE_ULT,
                                        1.6D, 0.9D, 48, 0.8D, 2.5D),
                                RigAnimationSpec.RigTimedAnimationHook.at(20, mob -> {
                                    if (mob instanceof SledgehammerHerobrineEntity sledgehammer
                                            && sledgehammer.canUseSecondFormAction()) {
                                        sledgehammer.consumeSecondFormAction();
                                    }
                                })
                        ),
                        RigAttackWindow.of(11, 20, RIGHT_SLEDGEHAMMER))
                .onHit((attacker, target, critical) -> GroundStuckMobEffect.apply(target))
                .dangerous()
                .invulnerable()
                .damageMultiplier(2.5F)
        );
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SLEDGEHAMMER_HEROBRINE_EXTRA_ULT, 91, RigAnimationPlaybackType.DEFAULT,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(30, mob -> {
                                    if (!(mob instanceof SledgehammerHerobrineEntity sledgehammer)) return;
                                    sledgehammer.playSound(AnnoyingVillagersModSounds.SLEDGE_HAMMER.get(), 1.0F, 1.0F);
                                    if (sledgehammer.canUseSecondFormAction()) sledgehammer.consumeSecondFormAction();
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(34, mob ->
                                        ObsidianSledgehammerItem.spawnWave(mob, mob.getYRot(), 0.0F, 4.0F, 18)),
                                RigAnimationSpec.RigTimedAnimationHook.at(36, mob ->
                                        ObsidianSledgehammerItem.spawnWave(mob, mob.getYRot(), 4.0F, 8.0F, 24)),
                                RigAnimationSpec.RigTimedAnimationHook.at(38, mob ->
                                        ObsidianSledgehammerItem.spawnWave(mob, mob.getYRot(), 8.0F, 12.0F, 30)),
                                RigAnimationSpec.RigTimedAnimationHook.at(40, mob ->
                                        ObsidianSledgehammerItem.spawnWave(mob, mob.getYRot(), 12.0F, 16.0F, 36))
                        ))
                .dangerous()
                .invulnerable());

        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK1, 21, false,
                        RigAttackWindow.of(4, 16, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK2, 33, false,
                        RigAttackWindow.of(4, 16, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK3, 36, false,
                        groundSlamHook(9, RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK3,
                                1.4D, 1.0D, 50, 1.0D, 3.5D),
                        RigAttackWindow.of(4, 15, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK4, 30, false,
                        RigAttackWindow.of(5, 20, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK5, 54, false,
                        groundSlamHook(16, RigAnimationId.SWORDSMAN_HEROBRINE_ATTACK5,
                                1.4D, 0.7D, 35, 0.7D, 2.0D),
                        RigAttackWindow.of(13, 25, RIGHT_GREATSWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_DASH_ATTACK, 44, false,
                groundSlamHook(9, RigAnimationId.SWORDSMAN_HEROBRINE_DASH_ATTACK,
                        1.4D, 0.7D, 35, 0.7D, 2.0D),
                RigAttackWindow.of(3, 15, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_JUMP_ATTACK, 26, true,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(5, mob -> {}),
                                groundSlamTimedHook(10, RigAnimationId.SWORDSMAN_HEROBRINE_JUMP_ATTACK,
                                        1.4D, 0.8D, 45, 0.7D, 2.5D)
                        ),
                        RigAttackWindow.of(5, 17, RIGHT_GREATSWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ATTACK, 52, false,
                groundSlamHook(7, RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ATTACK,
                        1.4D, 0.7D, 35, 0.7D, 2.0D),
                RigAttackWindow.of(7, 18, RIGHT_GREATSWORD)));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SWORDSMAN_HEROBRINE_ULT, 82, RigAnimationPlaybackType.UPPER_BODY,
                        hookAt(0, mob -> {
                            DemoniacVoltageReaverItem.tryStartSnakeAnimation(mob.getMainHandItem(), mob, false);
                            if (mob instanceof HerobrineMob herobrineMob) {
                                herobrineMob.consumeSecondFormAction();
                            }
                        }))
                .dangerous()
                .invulnerable());
        put(RigAnimationSpec.nonDamaging(RigAnimationId.SWORDSMAN_HEROBRINE_EXTRA_ULT, 81, RigAnimationPlaybackType.UPPER_BODY,
                        hookAt(0, mob -> {
                            DemoniacVoltageReaverItem.tryStartSnakeAnimation(mob.getMainHandItem(), mob, true);
                            if (mob instanceof HerobrineMob herobrineMob) {
                                herobrineMob.consumeSecondFormAction();
                            }
                        }))
                .dangerous()
                .invulnerable());

        put(RigAnimationSpec.nonDamaging(RigAnimationId.NULL_IDLE, 40));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.NULL_WALK, 20));
        put(RigAnimationSpec.nonDamaging(RigAnimationId.NULL_RUN, 22));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_ATTACK1, 20, false,
                RigAttackWindow.of(6, 15, LEFT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_ATTACK2, 22, false,
                RigAttackWindow.of(6, 11, RIGHT_ELBOW),
                RigAttackWindow.of(12, 20, LEFT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_ATTACK3, 34, false,
                RigAttackWindow.of(4, 7, LEFT_ELBOW),
                RigAttackWindow.of(8, 13, RIGHT_ELBOW),
                RigAttackWindow.of(14, 20, LEFT_ELBOW, RIGHT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_ATTACK4, 40, false,
                        RigAttackWindow.of(2, 10, LEFT_ELBOW, RIGHT_ELBOW),
                        RigAttackWindow.of(18, 20, LEFT_ELBOW, RIGHT_ELBOW),
                        RigAttackWindow.of(20, 22, LEFT_ELBOW, RIGHT_ELBOW),
                        RigAttackWindow.of(22, 24, LEFT_ELBOW, RIGHT_ELBOW),
                        RigAttackWindow.of(24, 32, LEFT_ELBOW, RIGHT_ELBOW))
                .criticalChance(0.2F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.NULL_ATTACK5, 60, false,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(1, mob -> {
                                    if (!(mob.level() instanceof ServerLevel)) return;
                                    mob.level().playSound(null, mob.blockPosition(), AnnoyingVillagersModSounds.BLACK_HOLE_CHARGE.get(), SoundSource.HOSTILE, 2.0F, 0.85F);
                                    spawnNullBlackHoleChargeParticles(mob, 1);
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(4, mob -> spawnNullBlackHoleChargeParticles(mob, 4)),
                                RigAnimationSpec.RigTimedAnimationHook.at(8, mob -> spawnNullBlackHoleChargeParticles(mob, 8)),
                                RigAnimationSpec.RigTimedAnimationHook.at(12, mob -> spawnNullBlackHoleChargeParticles(mob, 12)),
                                RigAnimationSpec.RigTimedAnimationHook.at(16, mob -> spawnNullBlackHoleChargeParticles(mob, 16)),
                                RigAnimationSpec.RigTimedAnimationHook.at(20, mob -> spawnNullBlackHoleChargeParticles(mob, 20)),
                                RigAnimationSpec.RigTimedAnimationHook.at(24, mob -> spawnNullBlackHoleChargeParticles(mob, 24)),
                                RigAnimationSpec.RigTimedAnimationHook.at(28, mob -> {
                                    spawnNullBlackHoleChargeParticles(mob, 28);
                                    if (!(mob.level() instanceof ServerLevel)) return;
                                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.7F, 0.7F);
                                    mob.level().playSound(null, mob.blockPosition(), AnnoyingVillagersModSounds.WHOOSH.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
                                    spawnNullBlackHoleChargeParticles(mob, 28);
                                }),
                                RigAnimationSpec.RigTimedAnimationHook.at(30, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                                    LivingEntity target = getNullCombatTarget(mob);
                                    Vec3 spawnPosition = getNullBlackHoleSpawnPosition(serverLevel, mob, target);
                                    serverLevel.addFreshEntity(new BlackHoleEntity(serverLevel, mob, spawnPosition));
                                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, spawnPosition.x, spawnPosition.y, spawnPosition.z, 48, 0.8D, 0.8D, 0.8D, 0.18D);
                                    serverLevel.sendParticles(AnnoyingVillagersModParticleTypes.NULL.get(), spawnPosition.x, spawnPosition.y, spawnPosition.z, 64, 1.4D, 1.4D, 1.4D, 0.12D);
                                    serverLevel.playSound(null, BlockPos.containing(spawnPosition), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.0F, 0.5F);
                                })
                        ),
                        RigAttackWindow.of(22, 38, RIGHT_ELBOW))
                .criticalChance(0.3F)
                .damageMultiplier(1.5F)
                .dangerous()
                .invulnerable()
        );
        put(RigAnimationSpec.attack(RigAnimationId.NULL_DASH_ATTACK, 34, false,
                RigAttackWindow.of(5, 9, RIGHT_ELBOW),
                RigAttackWindow.of(9, 16, LEFT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_JUMP_ATTACK, 30, true,
                        List.of(
                                RigAnimationSpec.RigTimedAnimationHook.at(1, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                                    serverLevel.playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.7F, 0.7F);
                                    serverLevel.playSound(null, mob.blockPosition(), AnnoyingVillagersModSounds.WHOOSH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                                }),

                                RigAnimationSpec.RigTimedAnimationHook.at(7, Mob::resetFallDistance),
                                RigAnimationSpec.RigTimedAnimationHook.at(9, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                                    serverLevel.playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.7F, 0.7F);
                                    serverLevel.playSound(null, mob.blockPosition(), AnnoyingVillagersModSounds.WHOOSH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

                                    for (int i = 0; i < 24; i++) {
                                        double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5D);
                                        double y = mob.getY() + 2.2D;
                                        double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5D);

                                        double velocityX = (mob.getRandom().nextDouble() - 0.5D) * 0.05D;
                                        double velocityY = -0.02D - mob.getRandom().nextDouble() * 0.06D;
                                        double velocityZ = (mob.getRandom().nextDouble() - 0.5D) * 0.05D;

                                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 0, velocityX, velocityY, velocityZ, 1.0D);
                                    }
                                }),

                                RigAnimationSpec.RigTimedAnimationHook.at(10, Mob::resetFallDistance),

                                RigAnimationSpec.RigTimedAnimationHook.at(11, mob -> {
                                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                                    mob.resetFallDistance();

                                    serverLevel.playSound(null, mob.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.7F, 0.5F);
                                    serverLevel.playSound(null, mob.blockPosition(), AnnoyingVillagersModSounds.BLUNT_HIT.get(), SoundSource.HOSTILE, 0.7F, 0.7F);

                                    for (int i = 0; i < 80; i++) {
                                        double angle = Math.PI * 2.0D * mob.getRandom().nextDouble();
                                        double radius = 0.6D * (mob.getRandom().nextDouble() + 0.4D);

                                        double offsetX = Math.cos(angle) * radius;
                                        double offsetZ = Math.sin(angle) * radius;
                                        double offsetY = (mob.getRandom().nextDouble() - 0.5D) * 0.06D;

                                        double x = mob.getX() + offsetX;
                                        double y = mob.getY() + 0.02D + offsetY;
                                        double z = mob.getZ() + offsetZ;

                                        serverLevel.sendParticles(
                                                ParticleTypes.LARGE_SMOKE,
                                                x, y, z,
                                                0,
                                                offsetX,
                                                offsetY,
                                                offsetZ,
                                                0.8D
                                        );
                                    }
                                })
                        ),
                        RigAttackWindow.of(10, 18, RIGHT_ELBOW, LEFT_ELBOW))
                .criticalChance(0.8F)
                .damageMultiplier(2.5F)
                .invulnerable()
        );
        put(RigAnimationSpec.attack(RigAnimationId.NULL_EXTRA_ATTACK, 20, false,
                List.of(RigAnimationSpec.RigTimedAnimationHook.at(RigAnimationSpec.RigTimedAnimationHook.START, mob -> {
                    if (!(mob instanceof NullEntity nullEntity)) return;
                    LivingEntity target = getNullCombatTarget(nullEntity);
                    if (target != null) nullEntity.releaseRandomNullWeapon(target);
                })),
                RigAttackWindow.of(1, 4, LEFT_FIST)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_EXTRA_ULT, 57, false,
                RigAttackWindow.of(10, 22, RIGHT_ELBOW, LEFT_ELBOW),
                RigAttackWindow.of(35, 46, RIGHT_ELBOW, LEFT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.NULL_SKELETON_SPAWN, 59, false,
                RigAttackWindow.of(13, 20, RIGHT_ELBOW, LEFT_ELBOW),
                RigAttackWindow.of(26, 32, RIGHT_ELBOW, LEFT_ELBOW),
                RigAttackWindow.of(35, 42, RIGHT_ELBOW, LEFT_ELBOW)));

        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_MACHINE_GUN, 50, false,
                hookAt(2, mob -> {
                    if (!(mob instanceof ShadowHerobrineEntity shadowHerobrine)) return;
                    if (!(mob.level() instanceof ServerLevel)) return;

                    shadowHerobrine.setObsidianMachineGunTick();
                    shadowHerobrine.playSound(AnnoyingVillagersModSounds.SHADOW_HEROBRINE_SAY_OBSIDIAN_MACHINE_GUN.get(), 1.0F, 1.0F);
                }),
                RigAttackWindow.of(0, 12))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_JUMP_ATTACK, 23, true,
                List.of(
                        smallGroundSlamTimedHook(10, 1.0D),
                        RigAnimationSpec.RigTimedAnimationHook.at(10, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            Item item = mob.getMainHandItem().getItem();
                            BlockState state;
                            if (item instanceof ShadowObsidianWeaponItem) {
                                state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                            } else if (item instanceof ShadowObsidianPillarItem) {
                                state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_LONG_PILLAR.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false).setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
                            } else if (item instanceof ShadowObsidianSwordItem || mob.getOffhandItem().getItem() instanceof ShadowObsidianSwordItem) {
                                state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_MIDDLE_PILLAR.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false).setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
                            } else if (item instanceof ObsidianWeaponItem) {
                                state = AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                            } else {
                                return;
                            }

                            HerobrineUtil.summonObsidianSmallCross(serverLevel, mob, state);
                        })
                ),
                RigAttackWindow.of(6, 20, RIGHT_FOOT)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_DASH_ATTACK, 19, false,
                hookAt(3, mob -> summonObsidianHandBlocks(mob, RigAnimationId.OBSIDIAN_DASH_ATTACK, 2, true, 6)),
                RigAttackWindow.of(2, 15, LEFT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_EXTRA_ULT, 30, false,
                hookAt(10, mob -> {
                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                    Item item = mob.getMainHandItem().getItem();
                    if (item instanceof ShadowObsidianPillarItem) {
                        HerobrineUtil.summonShadowObsidianLongPillarDefenseWide(serverLevel, mob);
                        return;
                    }

                    BlockState state;
                    if (item instanceof ShadowObsidianWeaponItem) {
                        state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                    } else if (item instanceof ObsidianWeaponItem) {
                        state = AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                    } else {
                        return;
                    }

                    HerobrineUtil.summonObsidianCross(serverLevel, mob, state);
                }),
                RigAttackWindow.of(10, 18, RIGHT_ELBOW, LEFT_ELBOW))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_EXTRA_ATTACK, 28, false,
                List.of(
                        RigAnimationSpec.RigTimedAnimationHook.at(12, mob -> throwObsidianProjectile(mob, RigAnimationId.OBSIDIAN_EXTRA_ATTACK, 12, false)),
                        RigAnimationSpec.RigTimedAnimationHook.hideRightToolAt(12)
                ),
                RigAttackWindow.of(9, 18, RIGHT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_PILLAR_EXTRA_ATTACK, 23, false,
                hookAt(4, RigAnimationSpecs::summonObsidianWallForWeapon),
                RigAttackWindow.of(2, 12, RIGHT_ELBOW)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_ULT1, 51, false,
                hookAt(2, mob -> {
                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                    Item item = mob.getMainHandItem().getItem();
                    if (item instanceof ShadowObsidianPillarItem || item instanceof ShadowObsidianSwordItem) {
                        HerobrineUtil.summonShadowObsidianLongPillarShootToward(serverLevel, mob);
                        return;
                    }

                    BlockState state;
                    if (item instanceof ShadowObsidianWeaponItem) {
                        state = AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                    } else if (item instanceof ObsidianWeaponItem) {
                        state = AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState().setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
                    } else {
                        return;
                    }

                    HerobrineUtil.summonObsidianPillarAtTarget(serverLevel, mob, state);
                }),
                RigAttackWindow.of(0, 12))
                .invulnerable()
                .dangerous()
        );
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_ULT2, 26, false,
                hookAt(13, mob -> {
                    if (!(mob.level() instanceof ServerLevel serverLevel)) return;
                    BlockState state = getObsidianWallState(mob);
                    if (state != null) HerobrineUtil.summonObsidianCube3x3x3(serverLevel, mob, state);
                }),
                RigAttackWindow.of(10, 22, RIGHT_ELBOW, LEFT_ELBOW))
                .invulnerable()
                .dangerous()
        );

        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_ATTACK1, 35, false,
                RigAttackWindow.of(4, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_ATTACK2, 37, false,
                RigAttackWindow.of(7, 17, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_ATTACK3, 42, false,
                RigAttackWindow.of(4, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_ATTACK4, 17, false,
                RigAttackWindow.of(3, 14, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_JUMP_ATTACK, 46, true,
                List.of(smallGroundSlamTimedHook(11, 1.0D)),
                RigAttackWindow.of(9, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_DASH_ATTACK, 36, false,
                RigAttackWindow.of(5, 15, RIGHT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.OBSIDIAN_SWORD_ULT, 50, false,
                        List.of(
                                smallGroundSlamTimedHook(10, 1.4D),
                                RigAnimationSpec.RigTimedAnimationHook.at(10, RigAnimationSpecs::summonObsidianWallForWeapon),
                                smallGroundSlamTimedHook(17, 1.4D),
                                RigAnimationSpec.RigTimedAnimationHook.at(17, RigAnimationSpecs::summonObsidianWallForWeapon),
                                smallGroundSlamTimedHook(28, 1.4D),
                                RigAnimationSpec.RigTimedAnimationHook.at(28, RigAnimationSpecs::summonObsidianWallForWeapon)
                        ),
                        RigAttackWindow.of(9, 14, RIGHT_SWORD),
                        RigAttackWindow.of(16, 25, RIGHT_SWORD),
                        RigAttackWindow.of(27, 36, RIGHT_SWORD))
                .damageMultiplier(1.5F)
                .criticalChance(0.3F)
                .dangerous()
        );

        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK1, 25, false,
                emptyHooks(6),
                RigAttackWindow.of(2, 12, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK2, 43, false,
                RigAttackWindow.of(10, 18, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK3, 48, false,
                RigAttackWindow.of(10, 16, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_ATTACK4, 44, false,
                List.of(smallGroundSlamTimedHook(18, 1.0D)),
                RigAttackWindow.of(7, 20, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_JUMP_ATTACK, 23, true,
                List.of(smallGroundSlamTimedHook(8, 1.0D)),
                RigAttackWindow.of(5, 15, RIGHT_SWORD, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_DASH_ATTACK, 38, false,
                        List.of(smallGroundSlamTimedHook(9, 1.0D)),
                        RigAttackWindow.of(4, 12, RIGHT_SWORD),
                        RigAttackWindow.of(13, 20, LEFT_SWORD))
                .damageMultiplier(1.2F)
                .criticalChance(0.1F)
        );
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_EXTRA_ATTACK, 42, false,
                List.of(
                        RigAnimationSpec.RigTimedAnimationHook.at(16, mob -> throwObsidianProjectile(mob, RigAnimationId.DUAL_OBSIDIAN_SWORD_EXTRA_ATTACK, 16, true)),
                        RigAnimationSpec.RigTimedAnimationHook.hideLeftToolAt(16)
                ),
                RigAttackWindow.of(6, 13, LEFT_FIST),
                RigAttackWindow.of(14, 22, LEFT_SWORD)));
        put(RigAnimationSpec.attack(RigAnimationId.DUAL_OBSIDIAN_SWORD_ULT, 42, false,
                List.of(
                        smallGroundSlamTimedHook(25, 1.0D),
                        RigAnimationSpec.RigTimedAnimationHook.at(25, mob -> {
                            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

                            Item item = mob.getMainHandItem().getItem();
                            if (item instanceof ShadowObsidianPillarItem) {
                                HerobrineUtil.summonShadowObsidianLongPillarCircle(serverLevel, mob, mob.getOnPos());
                                HerobrineUtil.summonShadowObsidianLongPillarShootToward(serverLevel, mob);
                            } else if (item instanceof ShadowObsidianSwordItem) {
                                HerobrineUtil.summonShadowObsidianLongPillarCircle(serverLevel, mob, mob.getOnPos());
                            }
                        })
                ),
                RigAttackWindow.of(22, 32, RIGHT_SWORD, LEFT_SWORD))
                .invulnerable()
                .dangerous()
        );
    }

    private RigAnimationSpecs() {}

    public static RigAnimationSpec get(RigAnimationId animationId) {
        RigAnimationSpec spec = SPECS.get(animationId);
        if (spec == null) throw new IllegalArgumentException("Missing rig animation spec for " + animationId);
        return spec;
    }

    public static boolean isAttack(RigAnimationId animationId) {
        RigAnimationSpec spec = SPECS.get(animationId);
        return spec != null && spec.damagesTarget();
    }

    private static List<RigAnimationSpec.RigTimedAnimationHook> hookAt(int tick, RigAnimationSpec.RigAnimationHook hook) {
        return List.of(RigAnimationSpec.RigTimedAnimationHook.at(tick, hook));
    }

    private static List<RigAnimationSpec.RigTimedAnimationHook> groundSlamHook(int tick, RigAnimationId animationId, double forwardOffset, double particleRadius, int particleCount, double spread, double fractureRadius) {
        return List.of(groundSlamTimedHook(tick, animationId, forwardOffset, particleRadius, particleCount, spread, fractureRadius));
    }

    private static RigAnimationSpec.RigTimedAnimationHook groundSlamTimedHook(int tick, RigAnimationId animationId, double forwardOffset, double particleRadius, int particleCount, double spread, double fractureRadius) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

            Vec3 impactPos = RigPoseUtil.getRightWeaponPosition(mob, animationId, tick, forwardOffset);
            CommonUtil.spawnGroundSlamFracture(mob, serverLevel, impactPos, particleRadius, particleCount, spread, fractureRadius);
        });
    }

    private static RigAnimationSpec.RigTimedAnimationHook smallGroundSlamTimedHook(int tick, double forwardOffset) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

            Vec3 look = mob.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0.0D, look.z);
            if (forward.lengthSqr() < 1.0E-6D) {
                Direction direction = mob.getDirection();
                forward = new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
            } else {
                forward = forward.normalize();
            }

            Vec3 impactPos = new Vec3(mob.getX(), mob.getY(), mob.getZ()).add(forward.scale(forwardOffset));
            CommonUtil.spawnGroundSlamFracture(mob, serverLevel, impactPos, 0.55D, 25, 0.5D, 1.5D);
        });
    }

    private static BlockState getObsidianFistState(Mob mob) {
        Item item = mob.getMainHandItem().getItem();
        if (!(item instanceof ObsidianWeaponItem || item instanceof ShadowObsidianWeaponItem || item instanceof ShadowObsidianPillarItem)) return null;
        if (item instanceof ShadowObsidianPillarItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_SHORT_PILLAR.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
        }
        if (item instanceof ShadowObsidianWeaponItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
        }
        return AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState()
                .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
    }

    private static BlockState getObsidianWallState(Mob mob) {
        Item item = mob.getMainHandItem().getItem();
        if (item instanceof ShadowObsidianPillarItem || item instanceof ShadowObsidianSwordItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_LONG_PILLAR.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
        }
        if (item instanceof ShadowObsidianWeaponItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
        }
        if (item instanceof ObsidianWeaponItem) {
            return AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
        }
        return null;
    }

    private static BlockState getObsidianProjectileState(Mob mob) {
        Item item = mob.getMainHandItem().getItem();
        if (item instanceof ShadowObsidianWeaponItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_BLOCK.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
        }
        if (item instanceof ShadowObsidianSwordItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_MIDDLE_PILLAR.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
        }
        if (item instanceof ShadowObsidianPillarItem) {
            return AnnoyingVillagersModBlocks.SHADOW_OBSIDIAN_LONG_PILLAR.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, mob.getDirection());
        }
        if (item instanceof ObsidianWeaponItem) {
            return AnnoyingVillagersModBlocks.OBSIDIAN_BLOCK.get().defaultBlockState()
                    .setValue(HerobrineObsidianBlock.FROM_PLAYER, false);
        }
        return null;
    }

    private static void summonObsidianHandBlocks(Mob mob, RigAnimationId animationId, int tick, boolean leftHand, int amount) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        BlockState state = getObsidianFistState(mob);
        if (state == null) return;

        Vec3 handPosition = leftHand
                ? RigPoseUtil.getLeftHandPosition(mob, animationId, tick)
                : RigPoseUtil.getRightHandPosition(mob, animationId, tick);
        if (handPosition == null) return;

        HerobrineUtil.summonObsidianBlocksFromPosition(serverLevel, mob, state, amount, handPosition);
    }

    private static void summonObsidianWallForWeapon(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        Item item = mob.getMainHandItem().getItem();
        if (item instanceof ShadowObsidianPillarItem || item instanceof ShadowObsidianSwordItem) {
            HerobrineUtil.summonShadowObsidianLongPillarDefense(serverLevel, mob);
            return;
        }

        BlockState state = getObsidianWallState(mob);
        if (state != null) HerobrineUtil.summonObsidianWall3x3(serverLevel, mob, state);
    }

    private static void throwObsidianProjectile(Mob mob, RigAnimationId animationId, int tick, boolean offhand) {
        Vec3 origin = offhand
                ? RigPoseUtil.getLeftHandPosition(mob, animationId, tick)
                : RigPoseUtil.getRightHandPosition(mob, animationId, tick);

        throwObsidianProjectile(mob, origin);
    }

    private static void throwObsidianProjectile(Mob mob, Vec3 origin) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        BlockState state = getObsidianProjectileState(mob);
        if (state == null) return;

        if (origin == null) origin = mob.getEyePosition(1.0F);

        Vec3 destination = mob.getEyePosition(1.0F).add(mob.getLookAngle().scale(16.0D));
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) destination = target.getEyePosition(1.0F);

        BlockProjectileEntity projectile = new BlockProjectileEntity(serverLevel, mob, state);
        projectile.setPos(origin.x, origin.y, origin.z);

        Vec3 direction = destination.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) direction = mob.getLookAngle();

        projectile.setDeltaMovement(direction.normalize().scale(2.0D));
        serverLevel.addFreshEntity(projectile);
    }

    private static RigAnimationSpec.RigTimedAnimationHook whiteAfterimageHook(int tick) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

            serverLevel.sendParticles(AnnoyingVillagersModParticleTypes.WHITE_AFTERIMAGE.get(), mob.getX(), mob.getY(), mob.getZ(), 0, mob.getId(), 0.0D, 0.0D, 1.0D);
        });
    }

    private static List<RigAnimationSpec.RigTimedAnimationHook> whiteAfterimageHooks(int... ticks) {
        List<RigAnimationSpec.RigTimedAnimationHook> hooks = new ArrayList<>(ticks.length);

        for (int tick : ticks) {
            hooks.add(whiteAfterimageHook(tick));
        }

        return List.copyOf(hooks);
    }

    private static List<RigAnimationSpec.RigTimedAnimationHook> woopieRushStartHook(RigAnimationId animationId) {
        return List.of(RigAnimationSpec.RigTimedAnimationHook.at(RigAnimationSpec.RigTimedAnimationHook.START, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

            boolean mainHandWoopie = mob.getMainHandItem().is(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
            boolean offHandWoopie = mob.getOffhandItem().is(AnnoyingVillagersModItems.WOOPIE_THE_SWORD.get());
            if (!mainHandWoopie && !offHandWoopie) return;

            Vec3 windPos = offHandWoopie ? RigPoseUtil.getLeftWeaponPosition(mob, animationId, 0.0F) : RigPoseUtil.getRightWeaponPosition(mob, animationId, 0.0F);
            if (windPos == null) return;

            AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundMuteExplosionAtPos(BlockPos.containing(windPos), 4));
            serverLevel.explode(mob, windPos.x, windPos.y, windPos.z, 2.0F, false, Level.ExplosionInteraction.NONE);
            AnnoyingVillagers.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundWoopieSwordWindFx(windPos));

            Vec3 dashDir = mob.getLookAngle();
            LivingEntity target = mob.getTarget();

            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.position().subtract(mob.position());
                dashDir = new Vec3(toTarget.x, 0.0D, toTarget.z);
            }

            dashDir = new Vec3(dashDir.x, 0.0D, dashDir.z);
            if (dashDir.lengthSqr() < 1.0E-6D) dashDir = Vec3.directionFromRotation(0.0F, mob.getYRot());

            Vec3 dash = dashDir.normalize().scale(2.2D);

            for (int tick = 1; tick <= 3; tick++) {
                new DelayedTask(tick) {
                    @Override
                    public void run() {
                        if (!mob.isAlive() || mob.isRemoved() || mob.isDeadOrDying()) return;
                        if (RigAnimationController.getActiveAnimationId(mob) != animationId) return;

                        Vec3 currentMotion = mob.getDeltaMovement();
                        mob.setDeltaMovement(dash.x, currentMotion.y, dash.z);
                        mob.hasImpulse = true;
                        mob.hurtMarked = true;
                    }
                };
            }
        }));
    }

    private static RigAnimationSpec.RigTimedAnimationHook blueDemonHandEffectHook(int tick, RigAnimationId animationId, RigPart part) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> playBlueDemonTridentEffect(mob, animationId, tick, part));
    }

    private static RigAnimationSpec.RigTimedAnimationHook blueDemonBothHandEffectHook(int tick, RigAnimationId animationId) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            playBlueDemonTridentEffect(mob, animationId, tick, RigPart.RIGHT_HAND);
            playBlueDemonTridentEffect(mob, animationId, tick, RigPart.LEFT_HAND);
        });
    }

    private static void playBlueDemonTridentEffect(Mob mob, RigAnimationId animationId, int tick, RigPart part) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (!(mob.getMainHandItem().getItem() instanceof BlueDemonTridentItem)) return;

        double forwardOffset = serverLevel.random.nextFloat() * 2.0F - 1.0F;
        Vec3 effectPos = RigPoseUtil.getPartPosition(mob, animationId, tick, part, Vec3.ZERO, forwardOffset, 0.0D);
        if (effectPos == null) return;

        BlueDemonUtil.spawnBlueDemonEffect(serverLevel, mob, effectPos, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        float volume = (float)Mth.nextDouble(serverLevel.random, 0.05D, 0.5D);
        float pitch = (float)Mth.nextDouble(serverLevel.random, 0.8D, 1.1D);
        serverLevel.playSound(null, BlockPos.containing(effectPos), AnnoyingVillagersModSounds.ELECTRIFY.get(), SoundSource.NEUTRAL, volume, pitch);
    }

    private static RigAnimationSpec.RigTimedAnimationHook blueDemonSpinHook(int tick) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;
            serverLevel.playSound(null, mob.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.NEUTRAL, 0.5F, 1.0F + serverLevel.random.nextFloat() * 0.2F);
        });
    }

    private static RigAnimationSpec.RigTimedAnimationHook blueDemonSoundHook(int tick, SoundEvent soundEvent, float volume, float pitch) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (mob.level() instanceof ServerLevel serverLevel) serverLevel.playSound(null, mob.blockPosition(), soundEvent, SoundSource.NEUTRAL, volume, pitch);
        });
    }

    private static RigAnimationSpec.RigTimedAnimationHook blueDemonThrowTridentHook(int tick, RigAnimationId animationId, InteractionHand hand, TridentMode mode) {
        return RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return;

            ItemStack stack = mob.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlueDemonTridentItem)) return;

            Vec3 spawnPos = hand == InteractionHand.MAIN_HAND
                    ? RigPoseUtil.getRightHandPosition(mob, animationId, tick)
                    : RigPoseUtil.getLeftHandPosition(mob, animationId, tick);
            if (spawnPos == null) return;

            Vec3 direction = BlueDemonTridentItem.getTridentThrowDirection(mob, spawnPos);
            if (direction == null || direction.lengthSqr() < 1.0E-7D) return;

            BlueDemonThrownTridentEntity trident = new BlueDemonThrownTridentEntity(serverLevel, mob, stack.copy());
            trident.assignSpawnSequence(mob);
            trident.trimOldGroundedTridentsAroundOwnerOnSpawn();
            trident.setMode(mode);
            trident.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            trident.setYRot((float)(Mth.atan2(direction.x, direction.z) * (180.0D / Math.PI)));
            trident.setXRot((float)(Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * (180.0D / Math.PI)));
            trident.pickup = AbstractArrow.Pickup.DISALLOWED;
            trident.shoot(direction.x, direction.y, direction.z, 2.5F, 1.0F);
            serverLevel.addFreshEntity(trident);
        });
    }

    private static List<RigAnimationSpec.RigTimedAnimationHook> emptyHooks(int... ticks) {
        List<RigAnimationSpec.RigTimedAnimationHook> hooks = new ArrayList<>(ticks.length);
        for (int tick : ticks) hooks.add(RigAnimationSpec.RigTimedAnimationHook.at(tick, mob -> {}));
        return List.copyOf(hooks);
    }


    private static void spawnNullBlackHoleChargeParticles(Mob mob, int tick) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        Vec3 handPosition = RigPoseUtil.getRightHandPosition(mob, RigAnimationId.NULL_ATTACK5, tick);
        double progress = Mth.clamp(tick / 36.0D, 0.0D, 1.0D);
        double radius = Mth.lerp(progress, 5.0D, 1.25D);
        int particles = 12 + (int)(progress * 12.0D);

        for (int i = 0; i < particles; i++) {
            double theta = Math.PI * 2.0D * mob.getRandom().nextDouble();
            double phi = Math.acos(2.0D * mob.getRandom().nextDouble() - 1.0D);
            double sinPhi = Math.sin(phi);
            Vec3 offset = new Vec3(radius * sinPhi * Math.cos(theta), radius * Math.cos(phi), radius * sinPhi * Math.sin(theta));
            Vec3 velocity = offset.scale(-0.12D - progress * 0.08D);
            Vec3 particlePosition = handPosition.add(offset);

            serverLevel.sendParticles(AnnoyingVillagersModParticleTypes.NULL.get(), particlePosition.x, particlePosition.y, particlePosition.z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
    }

    private static Vec3 getNullBlackHoleSpawnPosition(ServerLevel serverLevel, Mob mob, LivingEntity target) {
        Vec3 origin = RigPoseUtil.getRightHandPosition(mob, RigAnimationId.NULL_ATTACK5, 36);
        Vec3 desiredPosition;

        if (target != null && target.isAlive() && mob.distanceToSqr(target) <= 100.0D) {
            desiredPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        } else {
            Vec3 look = mob.getLookAngle();
            if (look.lengthSqr() <= 1.0E-7D) look = Vec3.directionFromRotation(0.0F, mob.yBodyRot);
            desiredPosition = origin.add(look.normalize().scale(4.0D));
        }

        BlockHitResult hitResult = serverLevel.clip(new ClipContext(origin, desiredPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        if (hitResult.getType() != HitResult.Type.BLOCK) return desiredPosition;

        Vec3 direction = desiredPosition.subtract(origin);
        if (direction.lengthSqr() <= 1.0E-7D) return hitResult.getLocation();
        return hitResult.getLocation().subtract(direction.normalize().scale(0.35D));
    }

    private static LivingEntity getNullCombatTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) target = mob.getLastHurtMob();
        if (target == null || !target.isAlive()) target = mob.getLastHurtByMob();
        return target != null && target.isAlive() ? target : null;
    }

    private static void put(RigAnimationSpec spec) {
        if (SPECS.put(spec.animationId(), spec) != null) throw new IllegalStateException("Duplicate rig animation spec for " + spec.animationId());
    }

    private static void knockUpTarget(LivingEntity target, double upwardVelocity) {
        if (target == null || !target.isAlive()) return;

        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(movement.x, Math.max(movement.y, upwardVelocity), movement.z);
        target.hurtMarked = true;
    }
}
