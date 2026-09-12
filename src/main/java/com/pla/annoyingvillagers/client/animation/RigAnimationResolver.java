package com.pla.annoyingvillagers.client.animation;

import com.pla.annoyingvillagers.client.animation.rig_animation.axe.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.basic_attack.BasicAttackAnimations1;
import com.pla.annoyingvillagers.client.animation.rig_animation.basic_attack.BasicAttackAnimations2;
import com.pla.annoyingvillagers.client.animation.rig_animation.blue_demon.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.dagger.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.swordsman_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.aegis_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.glaive_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.reaper_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.unarmed.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.greatsword.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.legendary_sword.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.living.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.longsword.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.null_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.sledgehammer_herobrine.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.obsidian.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.spear.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.sword.*;
import com.pla.annoyingvillagers.client.animation.rig_animation.tachi.*;
import com.pla.annoyingvillagers.rig.RigAnimationId;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RigAnimationResolver {
    private RigAnimationResolver() {}

    public static AnimationDefinition get(RigAnimationId animationId) {
        return switch (animationId) {
            case BOW_AIM_DOWN -> BowAnimations.BOW_AIM_DOWN;
            case BOW_AIM_MID -> BowAnimations.BOW_AIM_MID;
            case BOW_AIM_UP -> BowAnimations.BOW_AIM_UP;
            case BOW_ATTACK_DOWN -> BowAnimations.BOW_ATTACK_DOWN;
            case BOW_ATTACK_MID -> BowAnimations.BOW_ATTACK_MID;
            case BOW_ATTACK_UP -> BowAnimations.BOW_ATTACK_UP;

            case AXE_ATTACK1 -> AxeAnimations1.AXE_ATTACK1;
            case AXE_ATTACK2 -> AxeAnimations1.AXE_ATTACK2;
            case AXE_ATTACK3 -> AxeAnimations1.AXE_ATTACK3;
            case AXE_ATTACK4 -> AxeAnimations1.AXE_ATTACK4;
            case AXE_ATTACK5 -> AxeAnimations1.AXE_ATTACK5;
            case AXE_DASH_ATTACK -> AxeAnimations2.AXE_DASH_ATTACK;
            case AXE_JUMP_ATTACK -> AxeAnimations2.AXE_JUMP_ATTACK;
            case AXE_ULT -> AxeAnimations2.AXE_ULT;
            case DUAL_AXE_ULT -> AxeAnimations2.DUAL_AXE_ULT;
            case EARTH_AXE_ULT -> AxeAnimations2.EARTH_AXE_ULT;
            case EARTH_AXE_EXTRA_ATTACK -> AxeAnimations2.EARTH_AXE_EXTRA_ATTACK;

            case GREATSWORD_IDLE -> GreatswordAnimations1.GREATSWORD_IDLE;
            case GREATSWORD_RUN -> GreatswordAnimations1.GREATSWORD_RUN;
            case GREATSWORD_WALK -> GreatswordAnimations1.GREATSWORD_WALK;
            case CARRY -> GreatswordAnimations1.CARRY;
            case GREATSWORD_EXTRA_ATTACK -> GreatswordAnimations1.GREATSWORD_EXTRA_ATTACK;
            case GREATSWORD_ATTACK1 -> GreatswordAnimations2.GREATSWORD_ATTACK1;
            case GREATSWORD_ATTACK2 -> GreatswordAnimations2.GREATSWORD_ATTACK2;
            case GREATSWORD_ATTACK3 -> GreatswordAnimations2.GREATSWORD_ATTACK3;
            case GREATSWORD_ATTACK4 -> GreatswordAnimations3.GREATSWORD_ATTACK4;
            case GREATSWORD_ATTACK5 -> GreatswordAnimations3.GREATSWORD_ATTACK5;
            case GREATSWORD_DASH_ATTACK -> GreatswordAnimations3.GREATSWORD_DASH_ATTACK;
            case GREATSWORD_JUMP_ATTACK -> GreatswordAnimations3.GREATSWORD_JUMP_ATTACK;
            case GREATSWORD_ULT -> GreatswordAnimations3.GREATSWORD_ULT;
            case GREATAXE_ATTACK4 -> GreatswordAnimations4.GREATAXE_ATTACK4;
            case GREATAXE_ATTACK5 -> GreatswordAnimations4.GREATAXE_ATTACK5;
            case GREATAXE_DASH_ATTACK -> GreatswordAnimations4.GREATAXE_DASH_ATTACK;
            case GREATAXE_JUMP_ATTACK -> GreatswordAnimations4.GREATAXE_JUMP_ATTACK;
            case GREATAXE_ULT -> GreatswordAnimations4.GREATAXE_ULT;

            case LONGSWORD_ATTACK1 -> LongswordAnimations1.LONGSWORD_ATTACK1;
            case LONGSWORD_ATTACK2 -> LongswordAnimations1.LONGSWORD_ATTACK2;
            case LONGSWORD_ATTACK3 -> LongswordAnimations1.LONGSWORD_ATTACK3;
            case LONGSWORD_ATTACK4 -> LongswordAnimations1.LONGSWORD_ATTACK4;
            case LONGSWORD_ATTACK5 -> LongswordAnimations1.LONGSWORD_ATTACK5;
            case LONGSWORD_EXTRA_ATTACK -> LongswordAnimations2.LONGSWORD_EXTRA_ATTACK;
            case LONGSWORD_DASH_ATTACK -> LongswordAnimations2.LONGSWORD_DASH_ATTACK;
            case LONGSWORD_JUMP_ATTACK -> LongswordAnimations2.LONGSWORD_JUMP_ATTACK;
            case LONGSWORD_ULT -> LongswordAnimations2.LONGSWORD_ULT;

            case DUAL_LONGSWORD_ATTACK1 -> LongswordAnimations3.DUAL_LONGSWORD_ATTACK1;
            case DUAL_LONGSWORD_ATTACK2 -> LongswordAnimations3.DUAL_LONGSWORD_ATTACK2;
            case DUAL_LONGSWORD_ATTACK3 -> LongswordAnimations3.DUAL_LONGSWORD_ATTACK3;
            case DUAL_LONGSWORD_ATTACK4 -> LongswordAnimations3.DUAL_LONGSWORD_ATTACK4;
            case DUAL_LONGSWORD_ATTACK5 -> LongswordAnimations4.DUAL_LONGSWORD_ATTACK5;
            case DUAL_LONGSWORD_DASH_ATTACK -> LongswordAnimations4.DUAL_LONGSWORD_DASH_ATTACK;
            case DUAL_LONGSWORD_JUMP_ATTACK -> LongswordAnimations4.DUAL_LONGSWORD_JUMP_ATTACK;
            case DUAL_LONGSWORD_EXTRA_ATTACK -> LongswordAnimations4.DUAL_LONGSWORD_EXTRA_ATTACK;
            case DUAL_LONGSWORD_ULT -> LongswordAnimations4.DUAL_LONGSWORD_ULT;

            case SPEAR_IDLE -> SpearAnimations1.SPEAR_IDLE;
            case SPEAR_WALK -> SpearAnimations1.SPEAR_WALK;
            case SPEAR_RUN -> SpearAnimations1.SPEAR_RUN;
            case SPEAR_ATTACK1 -> SpearAnimations2.SPEAR_ATTACK1;
            case SPEAR_ATTACK2 -> SpearAnimations2.SPEAR_ATTACK2;
            case SPEAR_ATTACK3 -> SpearAnimations2.SPEAR_ATTACK3;
            case SPEAR_ATTACK4 -> SpearAnimations2.SPEAR_ATTACK4;
            case SPEAR_ATTACK5 -> SpearAnimations2.SPEAR_ATTACK5;
            case SPEAR_DASH_ATTACK -> SpearAnimations3.SPEAR_DASH_ATTACK;
            case SPEAR_JUMP_ATTACK -> SpearAnimations3.SPEAR_JUMP_ATTACK;
            case SPEAR_EXTRA_ATTACK -> SpearAnimations3.SPEAR_EXTRA_ATTACK;
            case SPEAR_ULT -> SpearAnimations3.SPEAR_ULT;
            case STAFF_ULT -> SpearAnimations1.STAFF_ULT;
            case SICKLE_ULT -> SpearAnimations1.SICKLE_ULT;
            case SPINNING_WEAPON -> LivingAnimations.SPINNING_WEAPON;

            case DAGGER_ATTACK1 -> DaggerAnimations1.DAGGER_ATTACK1;
            case DAGGER_ATTACK2 -> DaggerAnimations1.DAGGER_ATTACK2;
            case DAGGER_ATTACK3 -> DaggerAnimations1.DAGGER_ATTACK3;
            case DAGGER_ATTACK4 -> DaggerAnimations1.DAGGER_ATTACK4;
            case DAGGER_ATTACK5 -> DaggerAnimations1.DAGGER_ATTACK5;
            case DAGGER_DASH_ATTACK -> DaggerAnimations2.DAGGER_DASH_ATTACK;
            case DAGGER_EXTRA_ATTACK -> DaggerAnimations2.DAGGER_EXTRA_ATTACK;
            case DAGGER_JUMP_ATTACK -> DaggerAnimations2.DAGGER_JUMP_ATTACK;
            case DAGGER_ULT -> DaggerAnimations2.DAGGER_ULT;

            case SWORD_ATTACK1 -> SwordAnimations1.SWORD_ATTACK1;
            case SWORD_ATTACK2 -> SwordAnimations1.SWORD_ATTACK2;
            case SWORD_ATTACK3 -> SwordAnimations1.SWORD_ATTACK3;
            case SWORD_ATTACK4 -> SwordAnimations1.SWORD_ATTACK4;
            case SWORD_ATTACK5 -> SwordAnimations1.SWORD_ATTACK5;
            case SWORD_DASH_ATTACK -> SwordAnimations2.SWORD_DASH_ATTACK;
            case SWORD_JUMP_ATTACK -> SwordAnimations2.SWORD_JUMP_ATTACK;
            case SWORD_EXTRA_ATTACK -> SwordAnimations2.SWORD_EXTRA_ATTACK;
            case SWORD_ULT -> SwordAnimations2.SWORD_ULT;

            case DUAL_SWORD_ATTACK1 -> SwordAnimations3.DUAL_SWORD_ATTACK1;
            case DUAL_SWORD_ATTACK2 -> SwordAnimations3.DUAL_SWORD_ATTACK2;
            case DUAL_SWORD_ATTACK3 -> SwordAnimations3.DUAL_SWORD_ATTACK3;
            case DUAL_SWORD_ATTACK4 -> SwordAnimations3.DUAL_SWORD_ATTACK4;
            case DUAL_SWORD_ATTACK5 -> SwordAnimations3.DUAL_SWORD_ATTACK5;
            case DUAL_SWORD_DASH_ATTACK -> SwordAnimations4.DUAL_SWORD_DASH_ATTACK;
            case DUAL_SWORD_JUMP_ATTACK -> SwordAnimations4.DUAL_SWORD_JUMP_ATTACK;
            case DUAL_SWORD_EXTRA_ATTACK -> SwordAnimations4.DUAL_SWORD_EXTRA_ATTACK;
            case DUAL_SWORD_ULT -> SwordAnimations4.DUAL_SWORD_ULT;

            case BLACK_FIRE_SWORD_ULT -> SwordAnimations5.BLACK_FIRE_SWORD_ULT;
            case DIAMOND_ATTRACTOR_ULT -> SwordAnimations5.DIAMOND_ATTRACTOR_ULT;
            case DIAMOND_BLASTER_ULT -> SwordAnimations5.DIAMOND_BLASTER_ULT;
            case FLANKER_HOOK_SWORD_ULT -> SwordAnimations5.FLANKER_HOOK_SWORD_ULT;
            case GREAT_SWORD_ULT -> SwordAnimations5.GREAT_SWORD_ULT;
            case HOOK_SWORD_ULT1 -> SwordAnimations6.HOOK_SWORD_ULT1;
            case HOOK_SWORD_ULT2 -> SwordAnimations6.HOOK_SWORD_ULT2;
            case HOOK_SWORD_DUAL_ULT -> SwordAnimations5.HOOK_SWORD_DUAL_ULT;
            case HACKER_SWORD_ULT -> SwordAnimations6.HACKER_SWORD_ULT;
            case WOOPIE_THE_SWORD_EXTRA_ULT -> SwordAnimations6.WOOPIE_THE_SWORD_EXTRA_ULT;
            case WOOPIE_THE_SWORD_EXTRA_ULT_LEGENDARY -> SwordAnimations6.WOOPIE_THE_SWORD_EXTRA_ULT_LEGENDARY;
            case WOOPIE_THE_SWORD_FLY -> SwordAnimations6.WOOPIE_THE_SWORD_FLY;
            case WOOPIE_THE_SWORD_ULT -> SwordAnimations6.WOOPIE_THE_SWORD_ULT;

            case BASIC_ATTACK1 -> BasicAttackAnimations1.BASIC_ATTACK1;
            case BASIC_ATTACK2 -> BasicAttackAnimations1.BASIC_ATTACK2;
            case BASIC_ATTACK3 -> BasicAttackAnimations1.BASIC_ATTACK3;
            case BASIC_ATTACK4 -> BasicAttackAnimations1.BASIC_ATTACK4;
            case BASIC_DASH_ATTACK -> BasicAttackAnimations1.BASIC_DASH_ATTACK;
            case BASIC_JUMP_ATTACK -> BasicAttackAnimations1.BASIC_JUMP_ATTACK;
            case BASIC_ULT -> BasicAttackAnimations1.BASIC_ULT;
            case DUAL_BASIC_ATTACK1 -> BasicAttackAnimations2.DUAL_BASIC_ATTACK1;
            case DUAL_BASIC_ATTACK2 -> BasicAttackAnimations2.DUAL_BASIC_ATTACK2;
            case DUAL_BASIC_ATTACK3 -> BasicAttackAnimations2.DUAL_BASIC_ATTACK3;
            case DUAL_BASIC_DASH_ATTACK -> BasicAttackAnimations2.DUAL_BASIC_DASH_ATTACK;
            case DUAL_BASIC_JUMP_ATTACK -> BasicAttackAnimations2.DUAL_BASIC_JUMP_ATTACK;
            case DUAL_BASIC_ULT -> BasicAttackAnimations2.DUAL_BASIC_ULT;
            case BASIC_MOUNT_ATTACK -> BasicAttackAnimations2.BASIC_MOUNT_ATTACK;

            case TACHI_IDLE -> TachiAnimations1.TACHI_IDLE;
            case TACHI_RUN -> TachiAnimations1.TACHI_RUN;
            case TACHI_WALK -> TachiAnimations1.TACHI_WALK;
            case TACHI_ATTACK1 -> TachiAnimations2.TACHI_ATTACK1;
            case TACHI_ATTACK2 -> TachiAnimations2.TACHI_ATTACK2;
            case TACHI_ATTACK3 -> TachiAnimations2.TACHI_ATTACK3;
            case TACHI_ATTACK4 -> TachiAnimations2.TACHI_ATTACK4;
            case TACHI_ATTACK5 -> TachiAnimations2.TACHI_ATTACK5;
            case TACHI_EXTRA_ATTACK -> TachiAnimations3.TACHI_EXTRA_ATTACK;
            case TACHI_DASH_ATTACK -> TachiAnimations3.TACHI_DASH_ATTACK;
            case TACHI_JUMP_ATTACK -> TachiAnimations3.TACHI_JUMP_ATTACK;
            case TACHI_ULT -> TachiAnimations1.TACHI_ULT;

            case FIST_ATTACK1 -> FistAnimations.FIST_ATTACK1;
            case FIST_ATTACK2 -> FistAnimations.FIST_ATTACK2;
            case FIST_ATTACK3 -> FistAnimations.FIST_ATTACK3;
            case FIST_ATTACK4 -> FistAnimations.FIST_ATTACK4;
            case FIST_ATTACK5 -> FistAnimations.FIST_ATTACK5;
            case FIST_DASH_ATTACK -> FistAnimations.FIST_DASH_ATTACK;
            case FIST_JUMP_ATTACK -> FistAnimations.FIST_JUMP_ATTACK;
            case FIST_EXTRA_ATTACK -> FistAnimations.FIST_EXTRA_ATTACK;
            case FIST_ULT -> FistAnimations.FIST_ULT;

            case KICK_ATTACK1 -> KickAnimations.KICK_ATTACK1;
            case KICK_ATTACK2 -> KickAnimations.KICK_ATTACK2;
            case KICK_ATTACK3 -> KickAnimations.KICK_ATTACK3;
            case KICK_ATTACK4 -> KickAnimations.KICK_ATTACK4;
            case KICK_COMBO_ATTACK -> KickAnimations.KICK_COMBO_ATTACK;
            case KICK_DASH_ATTACK -> KickAnimations.KICK_DASH_ATTACK;

            case ROLL_BACKWARD -> DodgeAnimations.ROLL_BACKWARD;
            case ROLL_FORWARD -> DodgeAnimations.ROLL_FORWARD;
            case STEP_FORWARD -> DodgeAnimations.STEP_FORWARD;
            case STEP_BACKWARD -> DodgeAnimations.STEP_BACKWARD;
            case STEP_LEFT -> DodgeAnimations.STEP_LEFT;
            case STEP_RIGHT -> DodgeAnimations.STEP_RIGHT;
            case KNOCKDOWN_WAKEUP_LEFT -> DodgeAnimations.KNOCKDOWN_WAKEUP_LEFT;
            case KNOCKDOWN_WAKEUP_RIGHT -> DodgeAnimations.KNOCKDOWN_WAKEUP_RIGHT;

            case JUMP -> LivingAnimations.JUMP;
            case EAT_OFFHAND -> LivingAnimations.EAT_OFFHAND;
            case EAT_MAINHAND -> LivingAnimations.EAT_MAINHAND;

            case DEATH -> LivingAnimations.DEATH;
            case FALL -> LivingAnimations.FALL;
            case LANDING -> LivingAnimations.LANDING;
            case LAYING_DEATH -> LivingAnimations.LAYING_DEATH;
            case LAYING_DEATH_DEAD -> LivingAnimations.LAYING_DEATH_DEAD;
            case IDLE -> LivingAnimations.IDLE;
            case KNEEL -> LivingAnimations.KNEEL;
            case MOUNT -> LivingAnimations.MOUNT;
            case SNEAK -> LivingAnimations.SNEAK;
            case SWIM -> LivingAnimations.SWIM;
            case WALK -> LivingAnimations.WALK;
            case IDLE_DUAL -> LivingAnimations.IDLE_DUAL;
            case POINT_LEFT_HAND_TOWARD -> LivingAnimations.POINT_LEFT_HAND_TOWARD;
            case POINT_LEFT_HAND_MIDDLE -> LivingAnimations.POINT_LEFT_HAND_MIDDLE;
            case POINT_LEFT_HAND_UP -> LivingAnimations.POINT_LEFT_HAND_UP;

            case RUN -> RunAnimations.RUN;
            case RUN_HOLDING_DUAL_WEAPON -> RunAnimations.RUN_HOLDING_DUAL_WEAPON;
            case RUN_HOLDING_WEAPON -> RunAnimations.RUN_HOLDING_WEAPON;

            case SHIELD_OFFHAND -> ShieldAnimations.SHIELD_OFFHAND;
            case BLOCK_SHIELD_OFFHAND -> ShieldAnimations.BLOCK_SHIELD_OFFHAND;
            case SHIELD_MAINHAND -> ShieldAnimations.SHIELD_MAINHAND;
            case BLOCK_SHIELD_MAINHAND -> ShieldAnimations.BLOCK_SHIELD_MAINHAND;

            case HOOK_GUN -> HookGunAnimations.HOOK_GUN;
            case LEFT_HAND_HOOK -> HookGunAnimations.LEFT_HAND_HOOK;
            case LEFT_HAND_HOOK_TOP -> HookGunAnimations.LEFT_HAND_HOOK_TOP;
            case RIGHT_HAND_HOOK -> HookGunAnimations.RIGHT_HAND_HOOK;
            case RIGHT_HAND_HOOK_TOP -> HookGunAnimations.RIGHT_HAND_HOOK_TOP;

            case HIT_BACKWARD -> StunAnimations1.HIT_BACKWARD;
            case HIT_LEFT -> StunAnimations1.HIT_LEFT;
            case HIT_RIGHT -> StunAnimations1.HIT_RIGHT;
            case KNOCKDOWN_FORWARD -> StunAnimations1.KNOCKDOWN_FORWARD;
            case KNOCKDOWN_BACKWARD -> StunAnimations1.KNOCKDOWN_BACKWARD;
            case KNOCKDOWN_LEFT -> StunAnimations1.KNOCKDOWN_LEFT;
            case KNOCKDOWN_RIGHT -> StunAnimations1.KNOCKDOWN_RIGHT;
            case STUN_BACK -> StunAnimations2.STUN_BACK;
            case SUPER_KNOCK_BACK -> StunAnimations2.SUPER_KNOCK_BACK;
            case LEGENDARY_SWORD_KNOCKDOWN -> StunAnimations2.LEGENDARY_SWORD_KNOCKDOWN;
            case SHOCKED -> StunAnimations2.SHOCKED;
            case SHOCKED_LONG -> StunAnimations2.SHOCKED_LONG;

            case EATING_ELITE_1 -> HerobrineLivingAnimations.EATING_ELITE_1;
            case EATING_ELITE_2 -> HerobrineLivingAnimations.EATING_ELITE_2;
            case EATING_ELITE_3 -> HerobrineLivingAnimations.EATING_ELITE_3;
            case EATING_ELITE_4 -> HerobrineLivingAnimations.EATING_ELITE_4;
            case ELITE_HOLD_WEAPON -> HerobrineLivingAnimations.ELITE_HOLD_WEAPON;
            case ELITE_RUN_WEAPON -> HerobrineLivingAnimations.ELITE_RUN_WEAPON;
            case HEROBRINE_ANIMATE -> HerobrineLivingAnimations.HEROBRINE_ANIMATE;
            case HEROBRINE_ASSISTANCE -> HerobrineLivingAnimations.HEROBRINE_ASSISTANCE;
            case HEROBRINE_RUN -> HerobrineLivingAnimations.HEROBRINE_RUN;
            case HEROBRINE_SACRIFICING -> HerobrineLivingAnimations.HEROBRINE_SACRIFICING;
            case HEROBRINE_STAGE_CHANGE -> HerobrineLivingAnimations.HEROBRINE_STAGE_CHANGE;
            case KNOCKED_ELITE -> HerobrineLivingAnimations.KNOCKED_ELITE;
            case LOW_CLONE_ESCAPE -> HerobrineLivingAnimations.LOW_CLONE_ESCAPE;
            case PLAYER_HEROBRINE_POSSESSION -> HerobrineLivingAnimations.PLAYER_HEROBRINE_POSSESSION;
            case PORTAL_SUMMON -> HerobrineLivingAnimations.PORTAL_SUMMON;

            case BLUE_DEMON_DIE -> BlueDemonAnimations1.BLUE_DEMON_DIE;
            case BLUE_DEMON_DIE_START -> BlueDemonAnimations1.BLUE_DEMON_DIE_START;
            case BLUE_DEMON_DIE_TICK -> BlueDemonAnimations1.BLUE_DEMON_DIE_TICK;
            case BLUE_DEMON_STATE_TRANSFORM -> BlueDemonAnimations1.BLUE_DEMON_STATE_TRANSFORM;
            case BLUE_DEMON_STATE_TRANSFORM_END -> BlueDemonAnimations1.BLUE_DEMON_STATE_TRANSFORM_END;
            case BLUE_DEMON_TWOHAND_RUN -> BlueDemonAnimations1.BLUE_DEMON_TWOHAND_RUN;
            case BLUE_DEMON_EXTRA_ATTACK -> BlueDemonAnimations1.BLUE_DEMON_EXTRA_ATTACK;
            case BLUE_DEMON_EXTRA_ATTACK_LEGENDARY -> BlueDemonAnimations1.BLUE_DEMON_EXTRA_ATTACK_LEGENDARY;
            case BLUE_DEMON_TRIDENT_FESTIVAL -> BlueDemonAnimations1.BLUE_DEMON_TRIDENT_FESTIVAL;
            case BLUE_DEMON_ATTACK1 -> BlueDemonAnimations2.BLUE_DEMON_ATTACK1;
            case BLUE_DEMON_ATTACK2 -> BlueDemonAnimations2.BLUE_DEMON_ATTACK2;
            case BLUE_DEMON_ATTACK3 -> BlueDemonAnimations2.BLUE_DEMON_ATTACK3;
            case BLUE_DEMON_ATTACK4 -> BlueDemonAnimations2.BLUE_DEMON_ATTACK4;
            case BLUE_DEMON_ATTACK5 -> BlueDemonAnimations2.BLUE_DEMON_ATTACK5;
            case BLUE_DEMON_ATTACK6 -> BlueDemonAnimations3.BLUE_DEMON_ATTACK6;
            case BLUE_DEMON_DASH_ATTACK -> BlueDemonAnimations3.BLUE_DEMON_DASH_ATTACK;
            case BLUE_DEMON_JUMP_ATTACK -> BlueDemonAnimations3.BLUE_DEMON_JUMP_ATTACK;
            case BLUE_DEMON_ULT -> BlueDemonAnimations3.BLUE_DEMON_ULT;
            case BLUE_DEMON_THROW_ATTACK1 -> BlueDemonAnimations4.BLUE_DEMON_THROW_ATTACK1;
            case BLUE_DEMON_THROW_ATTACK2 -> BlueDemonAnimations4.BLUE_DEMON_THROW_ATTACK2;
            case BLUE_DEMON_THROW_ATTACK3 -> BlueDemonAnimations4.BLUE_DEMON_THROW_ATTACK3;
            case BLUE_DEMON_THROW_ATTACK4 -> BlueDemonAnimations4.BLUE_DEMON_THROW_ATTACK4;
            case BLUE_DEMON_THROW_ATTACK5 -> BlueDemonAnimations4.BLUE_DEMON_THROW_ATTACK5;
            case BLUE_DEMON_THROW_DASH_ATTACK -> BlueDemonAnimations5.BLUE_DEMON_THROW_DASH_ATTACK;
            case BLUE_DEMON_THROW_JUMP_ATTACK -> BlueDemonAnimations5.BLUE_DEMON_THROW_JUMP_ATTACK;
            case BLUE_DEMON_THROW_ULT -> BlueDemonAnimations5.BLUE_DEMON_THROW_ULT;

            case SWORDSMAN_HEROBRINE_ATTACK1 -> SwordsmanHerobrineAnimations1.SWORDSMAN_HEROBRINE_ATTACK1;
            case SWORDSMAN_HEROBRINE_ATTACK2 -> SwordsmanHerobrineAnimations1.SWORDSMAN_HEROBRINE_ATTACK2;
            case SWORDSMAN_HEROBRINE_ATTACK3 -> SwordsmanHerobrineAnimations1.SWORDSMAN_HEROBRINE_ATTACK3;
            case SWORDSMAN_HEROBRINE_ATTACK4 -> SwordsmanHerobrineAnimations1.SWORDSMAN_HEROBRINE_ATTACK4;
            case SWORDSMAN_HEROBRINE_ATTACK5 -> SwordsmanHerobrineAnimations1.SWORDSMAN_HEROBRINE_ATTACK5;
            case SWORDSMAN_HEROBRINE_DASH_ATTACK -> SwordsmanHerobrineAnimations2.SWORDSMAN_HEROBRINE_DASH_ATTACK;
            case SWORDSMAN_HEROBRINE_JUMP_ATTACK -> SwordsmanHerobrineAnimations2.SWORDSMAN_HEROBRINE_JUMP_ATTACK;
            case SWORDSMAN_HEROBRINE_EXTRA_ATTACK -> SwordsmanHerobrineAnimations2.SWORDSMAN_HEROBRINE_EXTRA_ATTACK;
            case SWORDSMAN_HEROBRINE_ULT -> SwordsmanHerobrineAnimations2.SWORDSMAN_HEROBRINE_ULT;
            case SWORDSMAN_HEROBRINE_EXTRA_ULT -> SwordsmanHerobrineAnimations2.SWORDSMAN_HEROBRINE_EXTRA_ULT;

            case AEGIS_HEROBRINE_IDLE -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_IDLE;
            case AEGIS_HEROBRINE_GUARD -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_GUARD;
            case AEGIS_HEROBRINE_ATTACK1 -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_ATTACK1;
            case AEGIS_HEROBRINE_ATTACK2 -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_ATTACK2;
            case AEGIS_HEROBRINE_ATTACK3 -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_ATTACK3;
            case AEGIS_HEROBRINE_ATTACK4 -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_ATTACK4;
            case AEGIS_HEROBRINE_ATTACK5 -> AegisHerobrineAnimations1.AEGIS_HEROBRINE_ATTACK5;
            case AEGIS_HEROBRINE_DASH_ATTACK -> AegisHerobrineAnimations2.AEGIS_HEROBRINE_DASH_ATTACK;
            case AEGIS_HEROBRINE_JUMP_ATTACK -> AegisHerobrineAnimations2.AEGIS_HEROBRINE_JUMP_ATTACK;
            case AEGIS_HEROBRINE_ULT -> AegisHerobrineAnimations2.AEGIS_HEROBRINE_ULT;
            case AEGIS_HEROBRINE_EXTRA_ATTACK -> AegisHerobrineAnimations2.AEGIS_HEROBRINE_EXTRA_ATTACK;

            case GLAIVE_HEROBRINE_ATTACK1 -> GlaiveHerobrineAnimations1.GLAIVE_HEROBRINE_ATTACK1;
            case GLAIVE_HEROBRINE_ATTACK2 -> GlaiveHerobrineAnimations1.GLAIVE_HEROBRINE_ATTACK2;
            case GLAIVE_HEROBRINE_ATTACK3 -> GlaiveHerobrineAnimations1.GLAIVE_HEROBRINE_ATTACK3;
            case GLAIVE_HEROBRINE_ATTACK4 -> GlaiveHerobrineAnimations1.GLAIVE_HEROBRINE_ATTACK4;
            case GLAIVE_HEROBRINE_ATTACK5 -> GlaiveHerobrineAnimations2.GLAIVE_HEROBRINE_ATTACK5;
            case GLAIVE_HEROBRINE_DASH_ATTACK -> GlaiveHerobrineAnimations3.GLAIVE_HEROBRINE_DASH_ATTACK;
            case GLAIVE_HEROBRINE_JUMP_ATTACK -> GlaiveHerobrineAnimations3.GLAIVE_HEROBRINE_JUMP_ATTACK;
            case GLAIVE_HEROBRINE_ULT -> GlaiveHerobrineAnimations3.GLAIVE_HEROBRINE_ULT;
            case GLAIVE_HEROBRINE_EXTRA_ULT -> GlaiveHerobrineAnimations3.GLAIVE_HEROBRINE_EXTRA_ULT;
            case GLAIVE_HEROBRINE_EXTRA_ATTACK -> GlaiveHerobrineAnimations3.GLAIVE_HEROBRINE_EXTRA_ATTACK;

            case REAPER_HEROBRINE_IDLE -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_IDLE;
            case REAPER_HEROBRINE_ATTACK1 -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_ATTACK1;
            case REAPER_HEROBRINE_ATTACK2 -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_ATTACK2;
            case REAPER_HEROBRINE_ATTACK3 -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_ATTACK3;
            case REAPER_HEROBRINE_ATTACK4 -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_ATTACK4;
            case REAPER_HEROBRINE_ATTACK5 -> ReaperHerobrineAnimations1.REAPER_HEROBRINE_ATTACK5;
            case REAPER_HEROBRINE_DASH_ATTACK -> ReaperHerobrineAnimations2.REAPER_HEROBRINE_DASH_ATTACK;
            case REAPER_HEROBRINE_JUMP_ATTACK -> ReaperHerobrineAnimations2.REAPER_HEROBRINE_JUMP_ATTACK;
            case REAPER_HEROBRINE_EXTRA_ATTACK -> ReaperHerobrineAnimations2.REAPER_HEROBRINE_EXTRA_ATTACK;
            case REAPER_HEROBRINE_ULT -> ReaperHerobrineAnimations2.REAPER_HEROBRINE_ULT;
            case REAPER_HEROBRINE_EXTRA_ULT -> ReaperHerobrineAnimations2.REAPER_HEROBRINE_EXTRA_ULT;

            case LEGENDARY_SWORD_IDLE -> LegendarySwordAnimations1.LEGENDARY_SWORD_IDLE;
            case LEGENDARY_SWORD_WALK -> LegendarySwordAnimations1.LEGENDARY_SWORD_WALK;
            case LEGENDARY_SWORD_RUN -> LegendarySwordAnimations1.LEGENDARY_SWORD_RUN;
            case LEGENDARY_SWORD_ULT -> LegendarySwordAnimations1.LEGENDARY_SWORD_ULT;
            case LEGENDARY_SWORD_EXTRA_ULT -> LegendarySwordAnimations1.LEGENDARY_SWORD_EXTRA_ULT;
            case LEGENDARY_SWORD_DASH_ATTACK -> LegendarySwordAnimations1.LEGENDARY_SWORD_DASH_ATTACK;
            case LEGENDARY_SWORD_JUMP_ATTACK -> LegendarySwordAnimations1.LEGENDARY_SWORD_JUMP_ATTACK;
            case LEGENDARY_SWORD_ATTACK1 -> LegendarySwordAnimations2.LEGENDARY_SWORD_ATTACK1;
            case LEGENDARY_SWORD_ATTACK2 -> LegendarySwordAnimations2.LEGENDARY_SWORD_ATTACK2;
            case LEGENDARY_SWORD_ATTACK3 -> LegendarySwordAnimations2.LEGENDARY_SWORD_ATTACK3;
            case LEGENDARY_SWORD_ATTACK4 -> LegendarySwordAnimations3.LEGENDARY_SWORD_ATTACK4;
            case LEGENDARY_SWORD_ATTACK5 -> LegendarySwordAnimations3.LEGENDARY_SWORD_ATTACK5;
            case LEGENDARY_SWORD_EXTRA_ATTACK -> LegendarySwordAnimations3.LEGENDARY_SWORD_EXTRA_ATTACK;
            case LEGENDARY_SWORD_DUAL_AUTO1 -> LegendarySwordAnimations4.LEGENDARY_SWORD_DUAL_AUTO1;
            case LEGENDARY_SWORD_DUAL_AUTO2 -> LegendarySwordAnimations4.LEGENDARY_SWORD_DUAL_AUTO2;
            case LEGENDARY_SWORD_DUAL_AUTO3 -> LegendarySwordAnimations4.LEGENDARY_SWORD_DUAL_AUTO3;

            case NULL_IDLE -> NullAnimations1.NULL_IDLE;
            case NULL_WALK -> NullAnimations1.NULL_WALK;
            case NULL_RUN -> NullAnimations1.NULL_RUN;
            case NULL_ATTACK1 -> NullAnimations1.NULL_ATTACK1;
            case NULL_ATTACK2 -> NullAnimations1.NULL_ATTACK2;
            case NULL_ATTACK3 -> NullAnimations1.NULL_ATTACK3;
            case NULL_ATTACK4 -> NullAnimations1.NULL_ATTACK4;
            case NULL_ATTACK5 -> NullAnimations1.NULL_ATTACK5;
            case NULL_DASH_ATTACK -> NullAnimations2.NULL_DASH_ATTACK;
            case NULL_JUMP_ATTACK -> NullAnimations2.NULL_JUMP_ATTACK;
            case NULL_EXTRA_ATTACK -> NullAnimations2.NULL_EXTRA_ATTACK;
            case NULL_EXTRA_ULT -> NullAnimations2.NULL_EXTRA_ULT;
            case NULL_SKELETON_SPAWN -> NullAnimations2.NULL_SKELETON_SPAWN;

            case SLEDGEHAMMER_HEROBRINE_ATTACK1 -> SledgehammerHerobrineAnimations1.SLEDGEHAMMER_HEROBRINE_ATTACK1;
            case SLEDGEHAMMER_HEROBRINE_ATTACK2 -> SledgehammerHerobrineAnimations1.SLEDGEHAMMER_HEROBRINE_ATTACK2;
            case SLEDGEHAMMER_HEROBRINE_ATTACK3 -> SledgehammerHerobrineAnimations1.SLEDGEHAMMER_HEROBRINE_ATTACK3;
            case SLEDGEHAMMER_HEROBRINE_ATTACK4 -> SledgehammerHerobrineAnimations1.SLEDGEHAMMER_HEROBRINE_ATTACK4;
            case SLEDGEHAMMER_HEROBRINE_ATTACK5 -> SledgehammerHerobrineAnimations1.SLEDGEHAMMER_HEROBRINE_ATTACK5;
            case SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK -> SledgehammerHerobrineAnimations2.SLEDGEHAMMER_HEROBRINE_EXTRA_ATTACK;
            case SLEDGEHAMMER_HEROBRINE_DASH_ATTACK -> SledgehammerHerobrineAnimations2.SLEDGEHAMMER_HEROBRINE_DASH_ATTACK;
            case SLEDGEHAMMER_HEROBRINE_JUMP_ATTACK -> SledgehammerHerobrineAnimations2.SLEDGEHAMMER_HEROBRINE_JUMP_ATTACK;
            case SLEDGEHAMMER_HEROBRINE_ULT -> SledgehammerHerobrineAnimations2.SLEDGEHAMMER_HEROBRINE_ULT;
            case SLEDGEHAMMER_HEROBRINE_EXTRA_ULT -> SledgehammerHerobrineAnimations2.SLEDGEHAMMER_HEROBRINE_EXTRA_ULT;

            case OBSIDIAN_MACHINE_GUN -> ObsidianAnimations1.OBSIDIAN_MACHINE_GUN;
            case OBSIDIAN_JUMP_ATTACK -> ObsidianAnimations1.OBSIDIAN_JUMP_ATTACK;
            case OBSIDIAN_DASH_ATTACK -> ObsidianAnimations1.OBSIDIAN_DASH_ATTACK;
            case OBSIDIAN_EXTRA_ULT -> ObsidianAnimations1.OBSIDIAN_EXTRA_ULT;
            case OBSIDIAN_EXTRA_ATTACK -> ObsidianAnimations1.OBSIDIAN_EXTRA_ATTACK;
            case OBSIDIAN_PILLAR_EXTRA_ATTACK -> ObsidianAnimations1.OBSIDIAN_PILLAR_EXTRA_ATTACK;
            case OBSIDIAN_ULT1 -> ObsidianAnimations1.OBSIDIAN_ULT1;
            case OBSIDIAN_ULT2 -> ObsidianAnimations1.OBSIDIAN_ULT2;

            case OBSIDIAN_SWORD_ATTACK1 -> ObsidianAnimations2.OBSIDIAN_SWORD_ATTACK1;
            case OBSIDIAN_SWORD_ATTACK2 -> ObsidianAnimations2.OBSIDIAN_SWORD_ATTACK2;
            case OBSIDIAN_SWORD_ATTACK3 -> ObsidianAnimations2.OBSIDIAN_SWORD_ATTACK3;
            case OBSIDIAN_SWORD_ATTACK4 -> ObsidianAnimations2.OBSIDIAN_SWORD_ATTACK4;
            case OBSIDIAN_SWORD_JUMP_ATTACK -> ObsidianAnimations2.OBSIDIAN_SWORD_JUMP_ATTACK;
            case OBSIDIAN_SWORD_DASH_ATTACK -> ObsidianAnimations2.OBSIDIAN_SWORD_DASH_ATTACK;
            case OBSIDIAN_SWORD_ULT -> ObsidianAnimations3.OBSIDIAN_SWORD_ULT;

            case DUAL_OBSIDIAN_SWORD_ATTACK1 -> ObsidianAnimations3.DUAL_OBSIDIAN_SWORD_ATTACK1;
            case DUAL_OBSIDIAN_SWORD_ATTACK2 -> ObsidianAnimations3.DUAL_OBSIDIAN_SWORD_ATTACK2;
            case DUAL_OBSIDIAN_SWORD_ATTACK3 -> ObsidianAnimations3.DUAL_OBSIDIAN_SWORD_ATTACK3;
            case DUAL_OBSIDIAN_SWORD_ATTACK4 -> ObsidianAnimations4.DUAL_OBSIDIAN_SWORD_ATTACK4;
            case DUAL_OBSIDIAN_SWORD_JUMP_ATTACK -> ObsidianAnimations4.DUAL_OBSIDIAN_SWORD_JUMP_ATTACK;
            case DUAL_OBSIDIAN_SWORD_DASH_ATTACK -> ObsidianAnimations4.DUAL_OBSIDIAN_SWORD_DASH_ATTACK;
            case DUAL_OBSIDIAN_SWORD_EXTRA_ATTACK -> ObsidianAnimations4.DUAL_OBSIDIAN_SWORD_EXTRA_ATTACK;
            case DUAL_OBSIDIAN_SWORD_ULT -> ObsidianAnimations4.DUAL_OBSIDIAN_SWORD_ULT;
            case DIG_MAINHAND -> RecoveryAnimations.DIG_MAINHAND;
            case USE_MAINHAND -> RecoveryAnimations.USE_MAINHAND;
        };
    }
}
