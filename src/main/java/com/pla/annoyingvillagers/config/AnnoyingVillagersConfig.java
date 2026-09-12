package com.pla.annoyingvillagers.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class AnnoyingVillagersConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.BooleanValue FORCE_TICK_MOBS;
    public static ForgeConfigSpec.BooleanValue REMOTE_NPC_DEPARTURE_ENABLED;
    public static ForgeConfigSpec.IntValue REMOTE_NPC_DEPARTURE_MIN_MINUTES;
    public static ForgeConfigSpec.IntValue REMOTE_NPC_DEPARTURE_MAX_MINUTES;

    public static ForgeConfigSpec.ConfigValue<Double> HEROBRINE_POSSESS_RATE;
    public static ForgeConfigSpec.ConfigValue<Integer> HEROBRINE_RECALL_MIN_TIME;
    public static ForgeConfigSpec.ConfigValue<Integer> HEROBRINE_RECALL_MAX_TIME;
    public static ForgeConfigSpec.ConfigValue<Double> ANGRY_STEVE_CHANCE;
    public static ForgeConfigSpec.ConfigValue<Integer> ANGRY_STEVE_LEAVE_MIN_TIME;
    public static ForgeConfigSpec.ConfigValue<Integer> ANGRY_STEVE_LEAVE_MAX_TIME;
    public static ForgeConfigSpec.ConfigValue<Integer> BLUE_DEMON_LEAVE_MIN_TIME;
    public static ForgeConfigSpec.ConfigValue<Integer> BLUE_DEMON_LEAVE_MAX_TIME;
    public static ForgeConfigSpec.ConfigValue<Boolean> TRIDENT_FESTIVAL_CAN_BREAK_BLOCK;
    public static ForgeConfigSpec.ConfigValue<Boolean> TURN_ON_NPC_CHAT;
    public static ForgeConfigSpec.ConfigValue<Boolean> TURN_ON_NPC_VOICE;
    public static ForgeConfigSpec.ConfigValue<Boolean> AV_MOB_CAN_BURN_ITEM;
    public static ForgeConfigSpec.ConfigValue<Boolean> ARROW_CAN_BREAK_BLOCK;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> WEAPON_DISARMS_AFFECTED_ENTITY_TYPES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> WEAPON_DISARMS_BLACKLIST;

    static {
        FORCE_TICK_MOBS = BUILDER.comment(
                        "Keep ForceTickEntity mobs ticking remotely (Herobrine mobs, Null weapons, Blue Demon, BBQ and Jev).",
                        "Disabling releases their runtime chunk tickets; player-like Steve/Alex/Chris sessions have separate ownership.")
                .define("forceTickMobs", true);

        BUILDER.push("remoteNpcDeparture");
        REMOTE_NPC_DEPARTURE_ENABLED = BUILDER.comment("Allow force-ticked NPCs to leave when no external chunk loader covers them.")
                .define("enabled", true);
        REMOTE_NPC_DEPARTURE_MIN_MINUTES = BUILDER.comment("Minimum unattended online simulation time in minutes. Counts only while this NPC owns a force ticket.")
                .defineInRange("minMinutes", 10, 1, 10080);
        REMOTE_NPC_DEPARTURE_MAX_MINUTES = BUILDER.comment("Maximum unattended time in minutes. External loading resets the timer; reversed bounds are normalized.")
                .defineInRange("maxMinutes", 30, 1, 10080);
        BUILDER.pop();

        HEROBRINE_POSSESS_RATE = BUILDER.comment(
                        "[ONLY WORK WHEN SmartNpc is installed] Chance for Herobrine possess another player npc into Low Herobrine Clone")
                .defineInRange("herobrinePossessRate", 0.5, 0, 1);

        HEROBRINE_RECALL_MIN_TIME = BUILDER.comment(
                        "The minimum value (in minutes) for Herobrine's random recall time. This value should be lower than or equal the maximum. " +
                                "After a random time between min and max, Herobrine will vanish and return to the Herobrine dimension.")
                .defineInRange("herobrineRecallMinTime", 60, 1, 10080);

        HEROBRINE_RECALL_MAX_TIME = BUILDER.comment(
                        "The maximum value (in minutes) for Herobrine's random recall time. This value should be greater than or equal to the minimum. " +
                                "After a random time between min and max, Herobrine will vanish and return to the Herobrine dimension.")
                .defineInRange("herobrineRecallMaxTime", 300, 1, 10080);

        ANGRY_STEVE_CHANCE = BUILDER.comment(
                        "Chance for Steve to be Angry after getting killed")
                .defineInRange("angrySteveChance", 0.2, 0, 1);

        ANGRY_STEVE_LEAVE_MIN_TIME = BUILDER.comment(
                        "The minimum value (in minutes) for Angry Steve's random leave time. This value should be lower than or equal the maximum. " +
                                "After a random time between min and max, Angry Steve will feel exhausted and leave the game.")
                .defineInRange("angrySteveLeaveMinTime", 60, 1, 10080);

        ANGRY_STEVE_LEAVE_MAX_TIME = BUILDER.comment(
                        "The maximum value (in minutes) for Angry Steve's random leave time. This value should be greater than or equal to the minimum. " +
                                "After a random time between min and max, Angry Steve will feel exhausted and leave the game.")
                .defineInRange("angrySteveLeaveMaxTime", 300, 1, 10080);

        BLUE_DEMON_LEAVE_MIN_TIME = BUILDER.comment(
                        "The minimum value (in minutes) for Blue Demon's random leave time. This value should be lower than or equal the maximum. " +
                                "After a random time between min and max, Blue Demon will feel bored and go away.\"")
                .defineInRange("blueDemonLeaveMinTime", 60, 1, 10080);

        BLUE_DEMON_LEAVE_MAX_TIME = BUILDER.comment(
                        "The maximum value (in minutes) for Blue Demon's random leave time. This value should be greater than or equal to the minimum. " +
                                "After a random time between min and max, Blue Demon will feel bored and go away.")
                .defineInRange("blueDemonLeaveMaxTime", 300, 1, 10080);

        TRIDENT_FESTIVAL_CAN_BREAK_BLOCK = BUILDER.comment(
                        "Make Trident Festival can break block")
                .define("tridentFestivalCanBreakBlock", true);

        TURN_ON_NPC_CHAT = BUILDER.comment(
                        "Turn on all chatting for NPC")
                .define("turnOnNpcChat", true);

        TURN_ON_NPC_VOICE = BUILDER.comment(
                        "Turn on all voice for NPC")
                .define("turnOnNpcVoice", true);

        ARROW_CAN_BREAK_BLOCK = BUILDER.comment(
                        "Make arrow can break block")
                .define("arrowCanBreakBlock", true);
        AV_MOB_CAN_BURN_ITEM = BUILDER.comment(
                        "Enable burning items ability for all of Av NPCs and Mobs")
                .define("AvMobCanBurnItem", true);

        WEAPON_DISARMS_AFFECTED_ENTITY_TYPES = BUILDER.comment(
                        "Living entity types whose held weapons can be disarmed")
                .defineListAllowEmpty(
                        "affectedEntityTypes",
                        List.of(
                                "minecraft:player",
                                "minecraft:zombie",
                                "minecraft:skeleton",
                                "smart_npc:player_npc",
                                "annoyingvillagers:low_herobrine_clone",
                                "annoyingvillagers:low_shadow_herobrine_clone",
                                "annoyingvillagers:villager_scout",
                                "annoyingvillagers:villager_scout_captain",
                                "annoyingvillagers:blue_villager_knight",
                                "annoyingvillagers:green_villager_knight",
                                "annoyingvillagers:red_villager_knight",
                                "annoyingvillagers:purple_villager_knight"
                        ),
                        AnnoyingVillagersConfig::validResourceOrTagOrNamespace
                );

        WEAPON_DISARMS_BLACKLIST = BUILDER.comment(
                        "Weapons/items that cannot be disarmed")
                .defineListAllowEmpty(
                        "weaponBlacklist",
                        List.of(
                                "minecraft:wooden_sword",
                                "minecraft:wooden_shovel",
                                "minecraft:wooden_pickaxe",
                                "minecraft:wooden_axe",
                                "minecraft:wooden_hoe",
                                "minecraft:stone_sword",
                                "minecraft:stone_shovel",
                                "minecraft:stone_pickaxe",
                                "minecraft:stone_axe",
                                "minecraft:stone_hoe",
                                "efn",
                                "cataclysm",
                                "wom:agony",
                                "wom:tormented_mind",
                                "wom:ruine",
                                "wom:ender_blaster",
                                "wom:antitheus",
                                "wom:satsujin",
                                "wom:herrscher",
                                "wom:gesetz",
                                "wom:moonless",
                                "wom:solar",
                                "wom:napoleon",
                                "wom:evil_tachi",
                                "wom:hollow_longsword",
                                "wom:jabberwocky",
                                "wom:nova",
                                "wom:orbit",
                                "wom:blackstar",
                                "wom:nova",
                                "wom:orbit",
                                "wom:wooden_staff",
                                "wom:stone_staff",
                                "cdmoveset:yamato",
                                "cdmoveset:s_wooden_spear",
                                "cdmoveset:s_stone_spear",
                                "cdmoveset:s_wooden_tachi",
                                "cdmoveset:s_stone_tachi",
                                "cdmoveset:s_wooden_sword",
                                "cdmoveset:s_stone_sword",
                                "cdmoveset:s_wooden_longsword",
                                "cdmoveset:s_stone_longsword",
                                "cdmoveset:s_wooden_greatsword",
                                "cdmoveset:s_stone_greatsword",
                                "epicfight:wooden_spear",
                                "epicfight:stone_spear",
                                "epicfight:wooden_tachi",
                                "epicfight:stone_tachi",
                                "epicfight:wooden_dagger",
                                "epicfight:stone_dagger",
                                "epicfight:wooden_greatsword",
                                "epicfight:stone_greatsword",
                                "annoyingvillagers:legendary_sword",
                                "annoyingvillagers:demoniac_voltage_reaver",
                                "annoyingvillagers:shadow_obsidian_weapon",
                                "annoyingvillagers:obsidian_weapon",
                                "annoyingvillagers:bedrock_weapon",
                                "annoyingvillagers:crafting_table",
                                "annoyingvillagers:wooden_door",
                                "annoyingvillagers:ladder",
                                "annoyingvillagers:trapdoor",
                                "annoyingvillagers:null_weapon",
                                "annoyingvillagers:shadow_obsidian_pillar",
                                "annoyingvillagers:ender_glaive",
                                "annoyingvillagers:ender_slayer_scythe",
                                "annoyingvillagers:demoniac_voltage_reaver",
                                "annoyingvillagers:obsidian_sledgehammer",
                                "annoyingvillagers:shadow_obsidian_sword",
                                "annoyingvillagers:warden_axe",
                                "annoyingvillagers:blue_demon_trident",
                                "annoyingvillagers:darkness_sword",
                                "annoyingvillagers:red_steel_axe",
                                "annoyingvillagers:holy_llama_hammer",
                                "annoyingvillagers:woopie_the_sword",
                                "annoyingvillagers:jessica_the_dark_shield"
                        ),
                        AnnoyingVillagersConfig::validResourceOrTagOrNamespace
                );
        SPEC = BUILDER.build();
    }

    private static boolean validResourceOrTagOrNamespace(Object object) {
        if (!(object instanceof String value)) {
            return false;
        }

        if (value.isBlank()) {
            return false;
        }

        if (value.startsWith("#")) {
            return ResourceLocation.tryParse(value.substring(1)) != null;
        }

        if (value.endsWith(":*")) {
            String namespace = value.substring(0, value.length() - 2);
            return ResourceLocation.isValidNamespace(namespace);
        }

        if (!value.contains(":")) {
            return ResourceLocation.isValidNamespace(value);
        }

        return ResourceLocation.tryParse(value) != null;
    }
}
