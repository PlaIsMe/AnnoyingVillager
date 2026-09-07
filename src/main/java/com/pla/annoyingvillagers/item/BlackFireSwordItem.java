package com.pla.annoyingvillagers.item;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.entity.BlackFireEntity;
import com.pla.annoyingvillagers.rig.RigCombatProfileProvider;
import com.pla.annoyingvillagers.rig.RigCombatStyle;
import com.pla.annoyingvillagers.util.CommonUtil;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlackFireSwordItem extends SwordItem implements RigCombatProfileProvider {
    private static final int BLACK_FIRE_FALLBACK_LOOKUP_TICKS = 80;
    private static final Map<Integer, Long> ACTIVE_BLACK_FIRE_FALLBACKS = new HashMap<>();
    private static Level blackFireFallbackLevel;
    private static final DustParticleOptions BLACK_FIRE_DUST =
            new DustParticleOptions(new Vector3f(0.03F, 0.03F, 0.035F), 1.35F);
    private static final DustParticleOptions BLACK_FIRE_FLASH_DUST =
            new DustParticleOptions(new Vector3f(0.85F, 0.9F, 0.8F), 0.9F);

    public BlackFireSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 1561;
            }

            public float getSpeed() {
                return 6.0F;
            }

            public float getAttackDamageBonus() {
                return 3.5F;
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
        }, 3, -2.1F, (new Properties()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled() || hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            BlackFireEntity.shootFromOwnerLook(level, player);
            VanillaWeaponAbilityUtil.swingMainHand(player, VanillaWeaponAbilityUtil.BETTER_COMBAT_FIST_ATTACK);
            player.getCooldowns().addCooldown(this, 20 * 15);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public RigCombatStyle getRigCombatStyle(ItemStack stack) {
        return RigCombatStyle.BLACK_FIRE_SWORD;
    }

    public static Vec3 getSwordOrBodyPosition(Entity entity) {
//        Add this in AV_EFM

//        try {
//            Vec3 pos = EpicfightUtil.getJointWithTranslation(
//                    entity,
//                    new Vec3f(0.0F, 0.0F, 0.0F),
//                    Armatures.BIPED.get().toolR,
//                    1.0F,
//                    0.0F
//            );
//
//            if (pos != null) {
//                return pos;
//            }
//        } catch (Exception ignored) {
//        }

        return CommonUtil.getVanillaSwordOrBodyPosition(entity);
    }

    public static Vec3 getBlackFireFallbackPosition(Entity entity) {
        if (entity instanceof BlackFireEntity blackFire) {
            if (blackFire.isFollowOwnerSwordMode()) {
                Entity owner = blackFire.getOwnerEntity();

                if (owner != null && owner.isAlive() && !owner.isRemoved()) {
                    return getSwordOrBodyPosition(owner);
                }
            }

            return blackFire.position();
        }

        return getSwordOrBodyPosition(entity);
    }

    public static void startBlackFireFallback(Level level, int entityId) {
        if (level == null || !level.isClientSide()) {
            return;
        }

        resetBlackFireFallbacks(level);
        ACTIVE_BLACK_FIRE_FALLBACKS.put(entityId, level.getGameTime() + BLACK_FIRE_FALLBACK_LOOKUP_TICKS);

        Entity entity = level.getEntity(entityId);
        if (entity != null && entity.isAlive() && !entity.isRemoved()) {
            spawnBlackFireFallback(level, entity, true);
        }
    }

    public static void spawnBlackFireFallback(Level level, Entity entity) {
        spawnBlackFireFallback(level, entity, false);
    }

    public static void tickBlackFireFallbacks(Level level) {
        if (level == null) {
            ACTIVE_BLACK_FIRE_FALLBACKS.clear();
            blackFireFallbackLevel = null;
            return;
        }

        resetBlackFireFallbacks(level);
        if (ACTIVE_BLACK_FIRE_FALLBACKS.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> iterator = ACTIVE_BLACK_FIRE_FALLBACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> active = iterator.next();
            Entity entity = level.getEntity(active.getKey());

            if (entity == null) {
                if (now > active.getValue()) {
                    iterator.remove();
                }

                continue;
            }

            if (!entity.isAlive() || entity.isRemoved()) {
                iterator.remove();
                continue;
            }

            spawnBlackFireFallback(level, entity);
        }
    }

    private static void resetBlackFireFallbacks(Level level) {
        if (blackFireFallbackLevel != level) {
            ACTIVE_BLACK_FIRE_FALLBACKS.clear();
            blackFireFallbackLevel = level;
        }
    }

    private static void spawnBlackFireFallback(Level level, Entity entity, boolean burst) {
        if (level == null || entity == null) {
            return;
        }

        RandomSource rand = level.getRandom();
        Vec3 center = getBlackFireFallbackPosition(entity);
        double radius = Math.max(0.35D, entity.getBbWidth() * 0.85D);
        double height = Math.max(0.45D, entity.getBbHeight() * 0.75D);
        int ringParticles = burst ? 54 : 16;
        int coreParticles = burst ? 12 : 4;

        for (int i = 0; i < ringParticles; i++) {
            double angle = (i / (double) ringParticles) * Math.PI * 2.0D + rand.nextDouble() * 0.35D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double ringRadius = radius * (0.55D + rand.nextDouble() * 0.75D);

            Vec3 outward = new Vec3(cos, 0.0D, sin);
            Vec3 tangent = new Vec3(-sin, 0.0D, cos);
            Vec3 pos = center
                    .add(outward.scale(ringRadius))
                    .add(0.0D, (rand.nextDouble() - 0.35D) * height, 0.0D);
            Vec3 velocity = tangent.scale(0.035D + rand.nextDouble() * 0.055D)
                    .add(outward.scale((rand.nextDouble() - 0.45D) * 0.035D))
                    .add(0.0D, 0.025D + rand.nextDouble() * 0.055D, 0.0D);

            spawnParticle(level, rand.nextBoolean() ? ParticleTypes.SMOKE : ParticleTypes.LARGE_SMOKE, pos, velocity);

            if ((i & 3) == 0) {
                spawnParticle(level, BLACK_FIRE_DUST, pos, velocity.scale(0.35D));
            }

            if (i % 5 == 0) {
                spawnParticle(level, ParticleTypes.SOUL_FIRE_FLAME, pos, velocity.scale(0.45D));
            }
        }

        for (int i = 0; i < coreParticles; i++) {
            Vec3 offset = randomUnit(rand).scale(rand.nextDouble() * radius * 0.45D);
            Vec3 pos = center.add(offset);
            Vec3 velocity = offset.scale(0.03D).add(0.0D, 0.04D + rand.nextDouble() * 0.06D, 0.0D);

            spawnParticle(level, BLACK_FIRE_FLASH_DUST, pos, velocity);
            if ((i & 1) == 0) {
                spawnParticle(level, ParticleTypes.POOF, pos, velocity.scale(0.55D));
            }
        }
    }

    private static void spawnParticle(Level level, ParticleOptions particle, Vec3 pos, Vec3 velocity) {
        level.addParticle(particle, true, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
    }

    private static Vec3 randomUnit(RandomSource rand) {
        double z = rand.nextDouble() * 2.0D - 1.0D;
        double angle = rand.nextDouble() * Math.PI * 2.0D;
        double radius = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
        return new Vec3(radius * Math.cos(angle), z, radius * Math.sin(angle));
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                tickBlackFireFallbacks(Minecraft.getInstance().level);
            }
        }
    }
}
