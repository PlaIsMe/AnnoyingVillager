package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.client.particle.smoke_wave.SmokeWaveOptions;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.potion.GroundStuckMobEffect;
import com.pla.annoyingvillagers.task.DelayedTask;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ObsidianSledgehammerItem extends SwordItem implements RigCombatProfileProvider {

    public ObsidianSledgehammerItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
            }

            public float getSpeed() {
                return 4.0F;
            }

            public float getAttackDamageBonus() {
                return 5.0F;
            }

            public int getLevel() {
                return 1;
            }

            public int getEnchantmentValue() {
                return 32;
            }

            public @NotNull Ingredient getRepairIngredient() {
                return Ingredient.of(AnnoyingVillagersModItems.ELITE_OBSIDIAN.get());
            }
        }, 1, -2.6F, (new Properties()).fireResistant());
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            LivingEntity target = VanillaWeaponAbilityUtil.performVanillaMeleeHit(player, 5.0D);
            if (target != null) GroundStuckMobEffect.apply(target);
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_TWO_HANDED_SLAM);
            player.getCooldowns().addCooldown(this, 20 * 15);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean activateVanillaSpecial(Player player) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || player.level().isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ObsidianSledgehammerItem item) || player.getCooldowns().isOnCooldown(item)) return false;
        VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_TWO_HANDED_SLAM);
        VanillaWeaponAbilityUtil.damageHeldItem(player, InteractionHand.MAIN_HAND, 1);
        float yaw = player.getYRot();
        spawnWave(player, yaw, 0.0F, 4.0F, 18);
        new DelayedTask(2) { @Override public void run() { if (player.isAlive() && !player.isRemoved()) spawnWave(player, yaw, 4.0F, 8.0F, 24); } };
        new DelayedTask(4) { @Override public void run() { if (player.isAlive() && !player.isRemoved()) spawnWave(player, yaw, 8.0F, 12.0F, 30); } };
        new DelayedTask(6) { @Override public void run() { if (player.isAlive() && !player.isRemoved()) spawnWave(player, yaw, 12.0F, 16.0F, 36); } };
        player.getCooldowns().addCooldown(item, 20 * 60);
        return true;
    }

    public static void spawnWave(LivingEntity caster, float yaw, float innerRadius, float radius, int particleCount) {
        if (!(caster.level() instanceof ServerLevel level) || particleCount <= 0) return;

        Vec3 center = caster.position();
        double yawRad = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        for (int i = 0; i < particleCount; i++) {
            double angle = Math.PI * 2.0D * i / particleCount;
            double localX = Math.cos(angle) * radius;
            double localZ = Math.sin(angle) * radius;
            double x = center.x + forwardX * localX + rightX * localZ;
            double z = center.z + forwardZ * localX + rightZ * localZ;
            float particleYaw = yaw - 360.0F / particleCount * i;
            float particlePitch = -30.0F + level.random.nextFloat() * 60.0F - 30.0F;
            level.sendParticles(new SmokeWaveOptions(particleYaw, particlePitch, 0.0D),
                    x, center.y + 0.4D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                caster.getBoundingBox().inflate(radius, 4.0D, radius),
                target -> target != caster
                        && target.isAlive()
                        && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                        && !caster.isAlliedTo(target))) {
            double dx = target.getX() - caster.getX();
            double dz = target.getZ() - caster.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if ((innerRadius > 0.0F && distance <= innerRadius) || distance > radius) continue;
            int amplifier = Mth.clamp(Math.round(5.0F * (1.0F - (float) (distance / 16.0D))), 0, 5);
            GroundStuckMobEffect.apply(target, GroundStuckMobEffect.DEFAULT_DURATION, amplifier);
        }
    }

    public void inventoryTick(@NotNull ItemStack itemstack, @NotNull Level level, @NotNull Entity entity, int i, boolean flag) {
        super.inventoryTick(itemstack, level, entity, i, flag);
//        Add this code in AV_EFM
//        if (flag && entity instanceof Player player) {
//            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
//            if (playerPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                SkillContainer skillContainer = serverPlayerPatch.getSkill(AVSkills.OBSIDIAN_SLEDGEHAMMER);
//                if (skillContainer.isActivated()) {
//                    HerobrineUtil.spawnEliteEffect(level, entity.getX(), entity.getY(), entity.getZ(), entity);
//                }
//            }
//        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, level, list, tooltipflag);
        list.add(Component.literal(Component.translatable("tooltip.annoyingvillagers.obsidian_sledgehammer").getString()));
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.SLEDGEHAMMER_HEROBRINE;
    }
}
