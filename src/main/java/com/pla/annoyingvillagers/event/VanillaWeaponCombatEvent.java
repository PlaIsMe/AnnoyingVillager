package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.entity.TridentLightningBolt;
import com.pla.annoyingvillagers.entity.BlueDemonThrownTridentEntity;
import com.pla.annoyingvillagers.item.EnderAegisItem;
import com.pla.annoyingvillagers.item.EnderSlayerScytheItem;
import com.pla.annoyingvillagers.item.LegendarySwordItem;
import com.pla.annoyingvillagers.item.NullWeaponItem;
import com.pla.annoyingvillagers.item.ObsidianWeaponItem;
import com.pla.annoyingvillagers.item.ShadowObsidianPillarItem;
import com.pla.annoyingvillagers.item.ShadowObsidianSwordItem;
import com.pla.annoyingvillagers.item.ShadowObsidianWeaponItem;
import com.pla.annoyingvillagers.item.TransporterFragmentItem;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaWeaponCombatEvent {
    private VanillaWeaponCombatEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (!event.isVanillaCritical() || player.level().isClientSide() || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ShadowObsidianPillarItem) ShadowObsidianPillarItem.onVanillaCriticalHit(stack, player);
        else if (stack.getItem() instanceof ShadowObsidianSwordItem) ShadowObsidianSwordItem.onVanillaCriticalHit(stack, player);
        else if (stack.getItem() instanceof ShadowObsidianWeaponItem) ShadowObsidianWeaponItem.onVanillaCriticalHit(player);
        else if (stack.getItem() instanceof ObsidianWeaponItem) ObsidianWeaponItem.onVanillaCriticalHit(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        if (!(player.getMainHandItem().getItem() instanceof EnderSlayerScytheItem)) return;
        LivingEntity target = event.getTarget() instanceof LivingEntity livingEntity ? livingEntity : null;
        if (EnderSlayerScytheItem.commandThunder(player, target)) event.setCanceled(true);
    }


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide() || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        TransporterFragmentItem.tickPendingSavedTeleport(player);
        if (!LegendarySwordItem.hasActiveVanillaAwakening(player)) LegendarySwordItem.clearVanillaAttackSpeed(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof EnderAegisItem)) return;
        boolean wasSecondForm = EnderAegisItem.isSecondForm(stack);
        if (wasSecondForm) EnderAegisItem.shieldShoot(player.level(), player);
        else EnderAegisItem.addBlockedCharge(stack, player, event.getBlockedDamage());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getSource().getEntity() instanceof Player attacker && event.getSource().getDirectEntity() == attacker && event.getEntity() != attacker) NullWeaponItem.onPlayerMeleeHit(attacker);
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getSource().getDirectEntity() instanceof BlueDemonThrownTridentEntity trident && trident.getOwner() == player) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getDirectEntity() instanceof TridentLightningBolt lightning && lightning.getOwner() == player) event.setCanceled(true);
    }

}
