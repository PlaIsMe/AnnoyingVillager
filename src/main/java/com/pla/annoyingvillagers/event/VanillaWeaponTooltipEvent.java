package com.pla.annoyingvillagers.event;

import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.item.*;
import com.pla.annoyingvillagers.util.VanillaWeaponAbilityUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VanillaWeaponTooltipEvent {
    private static final int METER_STEPS = 18;

    private VanillaWeaponTooltipEvent() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!VanillaWeaponAbilityUtil.abilitiesEnabled()) return;
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<Component> tooltip = event.getToolTip();

        if (item instanceof NullWeaponItem) addChargeTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.null_charge", NullWeaponItem.getCharge(stack), 100);
        if (item instanceof EnderAegisItem) addChargeTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.aegis_charge", Math.round(EnderAegisItem.getCharge(stack)), 100);

        if (item instanceof RedAxeItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.red_axe.right_click");
        else if (item instanceof EarthAxeItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.earth_axe.right_click", "tooltip.annoyingvillagers.vanilla_combat.earth_axe.special");
        else if (item instanceof WoopieTheSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.woopie.right_click", "tooltip.annoyingvillagers.vanilla_combat.woopie.special");
        else if (item instanceof ThunderDiamondBladeItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.thunder_diamond_blade.right_click");
        else if (item instanceof ShadowObsidianPillarItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_pillar.critical", "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_pillar.right_click", "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_pillar.special");
        else if (item instanceof ShadowObsidianSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_sword.right_click", "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_sword.special");
        else if (item instanceof ShadowObsidianWeaponItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_weapon.critical", "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_weapon.right_click", "tooltip.annoyingvillagers.vanilla_combat.shadow_obsidian_weapon.special");
        else if (item instanceof ObsidianWeaponItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.obsidian_weapon.critical", "tooltip.annoyingvillagers.vanilla_combat.obsidian_weapon.right_click", "tooltip.annoyingvillagers.vanilla_combat.obsidian_weapon.special");
        else if (item instanceof EnderGlaiveItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.ender_glaive.right_click");
        else if (item instanceof EnderSlayerScytheItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.ender_slayer_scythe.special", "tooltip.annoyingvillagers.vanilla_combat.ender_slayer_scythe.left_click", "tooltip.annoyingvillagers.vanilla_combat.ender_slayer_scythe.right_click", "tooltip.annoyingvillagers.vanilla_combat.ender_slayer_scythe.lifetime");
        else if (item instanceof DemoniacVoltageReaverItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.demoniac_voltage_reaver.special", "tooltip.annoyingvillagers.vanilla_combat.demoniac_voltage_reaver.left_click", "tooltip.annoyingvillagers.vanilla_combat.demoniac_voltage_reaver.right_click");
        else if (item instanceof ObsidianSledgehammerItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.obsidian_sledgehammer.right_click", "tooltip.annoyingvillagers.vanilla_combat.obsidian_sledgehammer.special");
        else if (item instanceof EnderAegisItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.ender_aegis.block", "tooltip.annoyingvillagers.vanilla_combat.ender_aegis.full", "tooltip.annoyingvillagers.vanilla_combat.ender_aegis.second_form_block");
        else if (item instanceof NullWeaponItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.null_weapon.offhand", "tooltip.annoyingvillagers.vanilla_combat.null_weapon.thresholds", "tooltip.annoyingvillagers.vanilla_combat.null_weapon.hold_special", "tooltip.annoyingvillagers.vanilla_combat.null_weapon.recovery");
        else if (item instanceof LegendarySwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.legendary_sword.right_click", "tooltip.annoyingvillagers.vanilla_combat.legendary_sword.special");
        else if (item instanceof BlueDemonTridentItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.blue_demon_trident.right_click", "tooltip.annoyingvillagers.vanilla_combat.blue_demon_trident.dual_throw", "tooltip.annoyingvillagers.vanilla_combat.blue_demon_trident.special", "tooltip.annoyingvillagers.vanilla_combat.blue_demon_trident.shift_special", "tooltip.annoyingvillagers.vanilla_combat.blue_demon_trident.festival");
        else if (item instanceof BlackFireSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.black_fire_sword.right_click");
        else if (item instanceof BlueFlameSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.blue_flame_sword.melee");
        else if (item instanceof ClowSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.clow_sword.melee");
        else if (item instanceof DiamondAttractorSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.diamond_attractor_sword.right_click");
        else if (item instanceof DiamondBlasterSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.diamond_blaster_sword.right_click");
        else if (item instanceof HookedDiamondSwordItem || item instanceof HookedGoldenSwordItem || item instanceof HookedIronSwordItem || item instanceof FlankerHookedSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.hook_sword.right_click");
        else if (item instanceof DNAxHookedSwordItem) addCombatTooltip(tooltip, "tooltip.annoyingvillagers.vanilla_combat.dnax_hooked_sword.right_click");
    }

    private static void addCombatTooltip(List<Component> tooltip, String... translationKeys) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.annoyingvillagers.vanilla_combat.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        for (String key : translationKeys) tooltip.add(Component.literal("  ").append(Component.translatable(key)).withStyle(ChatFormatting.GRAY));
    }

    private static void addChargeTooltip(List<Component> tooltip, String labelKey, int charge, int maxCharge) {
        int clamped = Mth.clamp(charge, 0, maxCharge);
        tooltip.add(Component.translatable(labelKey).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.annoyingvillagers.vanilla_combat.charge_value", clamped, maxCharge).withStyle(ChatFormatting.WHITE));
        int filled = Mth.clamp(Math.round((clamped / (float)maxCharge) * METER_STEPS), 0, METER_STEPS);
        MutableComponent meter = Component.literal("⚡ ").withStyle(ChatFormatting.AQUA);
        for (int i = 0; i < METER_STEPS; i++) meter.append(Component.literal(i < filled ? "▰" : "▱").withStyle(i < filled ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.add(meter);
        if (clamped >= maxCharge) tooltip.add(Component.translatable("tooltip.annoyingvillagers.vanilla_combat.charged").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
    }
}
