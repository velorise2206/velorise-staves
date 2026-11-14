package com.velorise.staves.config;

import com.velorise.staves.item.ArcaneStaffItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StavesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("velorise_staves_config");
    private static final Map<String, ConfigEntry<?>> ENTRIES = new LinkedHashMap<>();

    // Mechanics
    public static final DoubleValue DAMAGE_MULTIPLIER = defineDouble("mechanics", "damage_multiplier", 1.0D, 0.1D, 10.0D);
    public static final DoubleValue COOLDOWN_MULTIPLIER = defineDouble("mechanics", "cooldown_multiplier", 1.0D, 0.1D, 10.0D);
    public static final BooleanValue ARC_CASTER_CHARGE_CREEPER = defineBool("mechanics", "arc_caster_charge_creeper", true);
    public static final BooleanValue BASIC_MIRROR_CROSS_DIMENSION = defineBool("mechanics", "basic_mirror_cross_dimension", false);
    public static final IntValue TEMPORARY_WEB_DURATION_TICKS = defineInt("mechanics", "temporary_web_duration_ticks", 100, 20, 1200);
    public static final DoubleValue VOID_TRANSLOCATOR_RANGE_BASIC = defineDouble("mechanics", "void_translocator_range_basic", 24.0D, 4.0D, 128.0D);
    public static final DoubleValue VOID_TRANSLOCATOR_RANGE_NETHERITE = defineDouble("mechanics", "void_translocator_range_netherite", 36.0D, 4.0D, 128.0D);
    public static final DoubleValue VOID_TRANSLOCATOR_COOLDOWN_SECONDS = defineDouble("mechanics", "void_translocator_cooldown_seconds", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue MAGIC_MIRROR_COOLDOWN_SECONDS = defineDouble("mechanics", "magic_mirror_cooldown_seconds", 15.0D, 1.0D, 300.0D);
    public static final DoubleValue MAGIC_MIRROR_CHANNEL_SECONDS = defineDouble("mechanics", "magic_mirror_channel_seconds", 2.5D, 0.5D, 10.0D);

    // Elemental Gauge Theory (EGT)
    public static final BooleanValue ENABLE_ELEMENTAL_GAUGE = defineBool("elemental_gauge", "enable_elemental_gauge", true);
    public static final BooleanValue ENABLE_ELEMENTAL_RESONANCE = defineBool("elemental_gauge", "enable_elemental_resonance", true);
    public static final IntValue MAX_STAVES_IN_INVENTORY = defineInt("elemental_gauge", "max_staves_in_inventory", 4, 0, 36);
    public static final DoubleValue PYRO_RESONANCE_DAMAGE_BONUS = defineDouble("elemental_gauge", "pyro_resonance_damage_bonus", 0.10D, 0.0D, 1.0D);
    public static final StringValue ELEMENTAL_ARMOR_DURABILITY_WEAR = defineString("elemental_gauge", "elemental_armor_durability_wear", "auto");

    // Co-Op Multi-Player Dynamic Mob Scaling
    public static final BooleanValue ENABLE_COOP_MOB_SCALING = defineBool("scaling", "enable_coop_mob_scaling", true);
    public static final DoubleValue HP_SCALE_PER_EXTRA_PLAYER = defineDouble("scaling", "hp_scale_per_extra_player", 0.50D, 0.0D, 5.0D);
    public static final BooleanValue ENABLE_COOP_ATK_SCALING = defineBool("scaling", "enable_coop_atk_scaling", true);
    public static final IntValue MAX_COOP_PLAYERS_CAP = defineInt("scaling", "max_coop_players_cap", 4, 1, 16);
    public static final DoubleValue COOP_SCAN_RADIUS = defineDouble("scaling", "coop_scan_radius", 40.0D, 8.0D, 128.0D);
    public static final BooleanValue SHOW_ELEMENTAL_PARTICLES = defineBool("elemental_gauge", "show_elemental_particles", true);
    public static final BooleanValue SHOW_OVERHEAD_ELEMENT_ICONS = defineBool("elemental_gauge", "show_overhead_element_icons", true);
    public static final BooleanValue USE_32PX_ELEMENT_ICONS = defineBool("elemental_gauge", "use_32px_element_icons", false);
    public static final BooleanValue SHOW_REACTION_TEXT = defineBool("elemental_gauge", "show_reaction_text", true);
    public static final BooleanValue SHOW_DAMAGE_NUMBERS = defineBool("elemental_gauge", "show_damage_numbers", true);
    public static final DoubleValue INNATE_MUTATION_CHANCE = defineDouble("elemental_gauge", "innate_mutation_chance", 0.07D, 0.0D, 1.0D);

    // Innate Mutation Rules & Whitelist/Blacklist
    public static final BooleanValue ALLOW_HOSTILE_MOB_MUTATION = defineBool("innate_mutation", "allow_hostile_mob_mutation", true);
    public static final BooleanValue ALLOW_GOLEM_MUTATION = defineBool("innate_mutation", "allow_golem_mutation", true);
    public static final BooleanValue ALLOW_NEUTRAL_AND_PASSIVE_MUTATION = defineBool("innate_mutation", "allow_neutral_and_passive_mutation", false);
    public static final DoubleValue OMNI_MUTATION_SHARE = defineDouble("innate_mutation", "omni_mutation_share", 0.143D, 0.0D, 1.0D);
    public static final StringListValue MUTABLE_MOB_WHITELIST = defineStringList("innate_mutation", "mutable_mob_whitelist", List.of("minecraft:iron_golem", "minecraft:wolf", "minecraft:fox"));
    public static final StringListValue MUTABLE_MOB_BLACKLIST = defineStringList("innate_mutation", "mutable_mob_blacklist", List.of());
    public static final StringListValue CUSTOM_MOB_MUTATION_RATES = defineStringList("innate_mutation", "custom_mob_mutation_rates", List.of());

    // Cooldowns
    public static final DoubleValue DRAGONFIRE_COOLDOWN = defineDouble("cooldowns", "dragonfire_cooldown", 8.0D, 0.5D, 60.0D);
    public static final DoubleValue ECHO_COOLDOWN = defineDouble("cooldowns", "echo_cooldown", 8.0D, 0.5D, 60.0D);
    public static final DoubleValue INFERNO_COOLDOWN = defineDouble("cooldowns", "inferno_cooldown", 4.0D, 0.5D, 60.0D);
    public static final DoubleValue GHASTFIRE_COOLDOWN = defineDouble("cooldowns", "ghastfire_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue THOUSAND_BLAZING_SUNS_COOLDOWN = defineDouble("cooldowns", "thousand_blazing_suns_cooldown", 10.0D, 0.5D, 60.0D);
    public static final DoubleValue WITHER_COOLDOWN = defineDouble("cooldowns", "wither_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue SHULKER_COOLDOWN = defineDouble("cooldowns", "shulker_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue GUARDIAN_COOLDOWN = defineDouble("cooldowns", "guardian_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue SPLENDOR_COOLDOWN = defineDouble("cooldowns", "splendor_cooldown", 20.0D, 0.5D, 60.0D);
    public static final DoubleValue EVOKER_COOLDOWN = defineDouble("cooldowns", "evoker_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue VORTEX_COOLDOWN = defineDouble("cooldowns", "vortex_cooldown", 4.0D, 0.5D, 60.0D);
    public static final DoubleValue ARC_CASTER_COOLDOWN = defineDouble("cooldowns", "arc_caster_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue FROSTBOLT_COOLDOWN = defineDouble("cooldowns", "frostbolt_cooldown", 4.0D, 0.5D, 60.0D);
    public static final DoubleValue WEBCASTER_COOLDOWN = defineDouble("cooldowns", "webcaster_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue WIND_COOLDOWN = defineDouble("cooldowns", "wind_cooldown", 1.5D, 0.2D, 60.0D);
    public static final DoubleValue DAYBREAK_COOLDOWN = defineDouble("cooldowns", "daybreak_cooldown", 2.0D, 0.5D, 60.0D);
    public static final DoubleValue ENGULFING_LIGHTNING_COOLDOWN = defineDouble("cooldowns", "engulfing_lightning_cooldown", 10.0D, 0.5D, 60.0D);
    public static final DoubleValue OMNI_COOLDOWN = defineDouble("cooldowns", "omni_cooldown", 6.0D, 0.2D, 60.0D);

    // Basic Staff Base Damages (HP, 2.0 = 1 heart; Netherite automatically scales x1.5)
    public static final DoubleValue DRAGONFIRE_DAMAGE = defineDouble("damage", "dragonfire_damage", 7.0D, 0.0D, 500.0D);
    public static final DoubleValue ECHO_DAMAGE = defineDouble("damage", "echo_damage", 11.0D, 0.0D, 500.0D);
    public static final DoubleValue INFERNO_DAMAGE = defineDouble("damage", "inferno_damage", 3.0D, 0.0D, 500.0D);
    public static final DoubleValue GHASTFIRE_DAMAGE = defineDouble("damage", "ghastfire_damage", 6.0D, 0.0D, 500.0D);
    public static final DoubleValue THOUSAND_BLAZING_SUNS_DAMAGE = defineDouble("damage", "thousand_blazing_suns_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue WITHER_DAMAGE = defineDouble("damage", "wither_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue SHULKER_DAMAGE = defineDouble("damage", "shulker_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue GUARDIAN_DAMAGE = defineDouble("damage", "guardian_damage", 2.0D, 0.0D, 500.0D);
    public static final DoubleValue SPLENDOR_DAMAGE = defineDouble("damage", "splendor_damage", 1.0D, 0.0D, 500.0D);
    public static final DoubleValue EVOKER_DAMAGE = defineDouble("damage", "evoker_damage", 3.0D, 0.0D, 500.0D);
    public static final DoubleValue VORTEX_DAMAGE = defineDouble("damage", "vortex_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue ARC_CASTER_DAMAGE = defineDouble("damage", "arc_caster_damage", 1.0D, 0.0D, 500.0D);
    public static final DoubleValue FROSTBOLT_DAMAGE = defineDouble("damage", "frostbolt_damage", 3.0D, 0.0D, 500.0D);
    public static final DoubleValue NIGHTS_FOCUS_DAMAGE = defineDouble("damage", "nights_focus_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue WEBCASTER_DAMAGE = defineDouble("damage", "webcaster_damage", 1.5D, 0.0D, 500.0D);
    public static final DoubleValue WIND_DAMAGE = defineDouble("damage", "wind_damage", 1.5D, 0.0D, 500.0D);
    public static final DoubleValue DAYBREAK_DAMAGE = defineDouble("damage", "daybreak_damage", 1.5D, 0.0D, 500.0D);
    public static final DoubleValue ENGULFING_LIGHTNING_DAMAGE = defineDouble("damage", "engulfing_lightning_damage", 4.0D, 0.0D, 500.0D);
    public static final DoubleValue OMNI_DAMAGE = defineDouble("damage", "omni_damage", 10.0D, 0.0D, 500.0D);

    // Durabilities
    public static final IntValue DRAGONFIRE_DURABILITY_BASIC = defineInt("durability", "dragonfire_basic", 130, 10, 5000);
    public static final IntValue DRAGONFIRE_DURABILITY_NETHERITE = defineInt("durability", "dragonfire_netherite", 500, 10, 5000);
    public static final IntValue ECHO_DURABILITY_BASIC = defineInt("durability", "echo_basic", 130, 10, 5000);
    public static final IntValue ECHO_DURABILITY_NETHERITE = defineInt("durability", "echo_netherite", 500, 10, 5000);
    public static final IntValue INFERNO_DURABILITY_BASIC = defineInt("durability", "inferno_basic", 150, 10, 5000);
    public static final IntValue INFERNO_DURABILITY_NETHERITE = defineInt("durability", "inferno_netherite", 550, 10, 5000);
    public static final IntValue GHASTFIRE_DURABILITY_BASIC = defineInt("durability", "ghastfire_basic", 120, 10, 5000);
    public static final IntValue GHASTFIRE_DURABILITY_NETHERITE = defineInt("durability", "ghastfire_netherite", 450, 10, 5000);
    public static final IntValue THOUSAND_BLAZING_SUNS_DURABILITY_BASIC = defineInt("durability", "thousand_blazing_suns_basic", 160, 10, 5000);
    public static final IntValue THOUSAND_BLAZING_SUNS_DURABILITY_NETHERITE = defineInt("durability", "thousand_blazing_suns_netherite", 520, 10, 5000);
    public static final IntValue WITHER_DURABILITY_BASIC = defineInt("durability", "wither_basic", 140, 10, 5000);
    public static final IntValue WITHER_DURABILITY_NETHERITE = defineInt("durability", "wither_netherite", 500, 10, 5000);
    public static final IntValue SHULKER_DURABILITY_BASIC = defineInt("durability", "shulker_basic", 130, 10, 5000);
    public static final IntValue SHULKER_DURABILITY_NETHERITE = defineInt("durability", "shulker_netherite", 500, 10, 5000);
    public static final IntValue GUARDIAN_DURABILITY_BASIC = defineInt("durability", "guardian_basic", 130, 10, 5000);
    public static final IntValue GUARDIAN_DURABILITY_NETHERITE = defineInt("durability", "guardian_netherite", 500, 10, 5000);
    public static final IntValue SPLENDOR_DURABILITY_BASIC = defineInt("durability", "splendor_basic", 140, 10, 5000);
    public static final IntValue SPLENDOR_DURABILITY_NETHERITE = defineInt("durability", "splendor_netherite", 500, 10, 5000);
    public static final IntValue EVOKER_DURABILITY_BASIC = defineInt("durability", "evoker_basic", 120, 10, 5000);
    public static final IntValue EVOKER_DURABILITY_NETHERITE = defineInt("durability", "evoker_netherite", 450, 10, 5000);
    public static final IntValue VORTEX_DURABILITY_BASIC = defineInt("durability", "vortex_basic", 140, 10, 5000);
    public static final IntValue VORTEX_DURABILITY_NETHERITE = defineInt("durability", "vortex_netherite", 500, 10, 5000);
    public static final IntValue ARC_CASTER_DURABILITY_BASIC = defineInt("durability", "arc_caster_basic", 130, 10, 5000);
    public static final IntValue ARC_CASTER_DURABILITY_NETHERITE = defineInt("durability", "arc_caster_netherite", 500, 10, 5000);
    public static final IntValue FROSTBOLT_DURABILITY_BASIC = defineInt("durability", "frostbolt_basic", 150, 10, 5000);
    public static final IntValue FROSTBOLT_DURABILITY_NETHERITE = defineInt("durability", "frostbolt_netherite", 500, 10, 5000);
    public static final IntValue WEBCASTER_DURABILITY_BASIC = defineInt("durability", "webcaster_basic", 140, 10, 5000);
    public static final IntValue WEBCASTER_DURABILITY_NETHERITE = defineInt("durability", "webcaster_netherite", 500, 10, 5000);
    public static final IntValue WIND_DURABILITY_BASIC = defineInt("durability", "wind_basic", 140, 10, 5000);
    public static final IntValue WIND_DURABILITY_NETHERITE = defineInt("durability", "wind_netherite", 500, 10, 5000);
    public static final IntValue OMNI_DURABILITY_BASIC = defineInt("durability", "omni_basic", 250, 10, 5000);
    public static final IntValue OMNI_DURABILITY_NETHERITE = defineInt("durability", "omni_netherite", 900, 10, 5000);
    public static final IntValue VOID_TRANSLOCATOR_DURABILITY_BASIC = defineInt("durability", "void_translocator_basic", 80, 10, 5000);
    public static final IntValue VOID_TRANSLOCATOR_DURABILITY_NETHERITE = defineInt("durability", "void_translocator_netherite", 250, 10, 5000);
    public static final IntValue MAGIC_MIRROR_DURABILITY_BASIC = defineInt("durability", "magic_mirror_basic", 40, 10, 5000);
    public static final IntValue MAGIC_MIRROR_DURABILITY_NETHERITE = defineInt("durability", "magic_mirror_netherite", 150, 10, 5000);

    private StavesConfig() {}

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("velorise_staves.toml");
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String currentSection = "";
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                    continue;
                }

                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = stripInlineComment(line.substring(eqIdx + 1)).trim();
                    if (val.startsWith("[") && !val.endsWith("]")) {
                        StringBuilder arrBuf = new StringBuilder(val);
                        String extra;
                        while ((extra = reader.readLine()) != null) {
                            String trimmed = stripInlineComment(extra).trim();
                            if (!trimmed.isEmpty()) {
                                arrBuf.append(' ').append(trimmed);
                            }
                            if (trimmed.contains("]")) {
                                break;
                            }
                        }
                        val = arrBuf.toString();
                    }
                    String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    ConfigEntry<?> entry = ENTRIES.get(fullKey);
                    if (entry == null && "mechanics.max_staves_in_inventory".equals(fullKey)) {
                        entry = ENTRIES.get("elemental_gauge.max_staves_in_inventory");
                    }
                    if (entry != null) {
                        entry.parseAndSet(val);
                    }
                }
            }
            invalidateMutationRulesCache();
        } catch (Exception e) {
            LOGGER.error("Failed to parse velorise_staves.toml, falling back to defaults", e);
            save();
        }
    }

    private static String stripInlineComment(String s) {
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == '#' && !inQuotes) {
                return s.substring(0, i);
            }
        }
        return s;
    }

    public static void save() {
        save(true);
    }

    public static void save(boolean refreshSyncedSnapshot) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("# Velorise's Staves Configuration (Fabric)\n\n");

                writer.write("[mechanics]\n");
                writer.write("# Global damage multiplier for all combat staves (0.1 to 10.0)\n");
                writer.write("damage_multiplier = " + DAMAGE_MULTIPLIER.get() + "\n");
                writer.write("# Global cooldown multiplier for all staves and artifacts (0.1 to 10.0)\n");
                writer.write("cooldown_multiplier = " + COOLDOWN_MULTIPLIER.get() + "\n");
                writer.write("# Whether Arc Caster lightning strikes charge creepers and trigger mob conversions\n");
                writer.write("arc_caster_charge_creeper = " + ARC_CASTER_CHARGE_CREEPER.get() + "\n");
                writer.write("# Allow basic Magic Mirror to teleport across dimensions (Netherite tier always can)\n");
                writer.write("basic_mirror_cross_dimension = " + BASIC_MIRROR_CROSS_DIMENSION.get() + "\n");
                writer.write("# Duration in ticks before temporary cobwebs vanish (20 ticks = 1s)\n");
                writer.write("temporary_web_duration_ticks = " + TEMPORARY_WEB_DURATION_TICKS.get() + "\n");
                writer.write("void_translocator_range_basic = " + VOID_TRANSLOCATOR_RANGE_BASIC.get() + "\n");
                writer.write("void_translocator_range_netherite = " + VOID_TRANSLOCATOR_RANGE_NETHERITE.get() + "\n");
                writer.write("void_translocator_cooldown_seconds = " + VOID_TRANSLOCATOR_COOLDOWN_SECONDS.get() + "\n");
                writer.write("magic_mirror_cooldown_seconds = " + MAGIC_MIRROR_COOLDOWN_SECONDS.get() + "\n");
                writer.write("magic_mirror_channel_seconds = " + MAGIC_MIRROR_CHANNEL_SECONDS.get() + "\n\n");

                writer.write("[elemental_gauge]\n");
                writer.write("# Enable Genshin-style Elemental Gauge Theory (EGT) reactions, U-consumption, and decay\n");
                writer.write("enable_elemental_gauge = " + ENABLE_ELEMENTAL_GAUGE.get() + "\n");
                writer.write("# Enable 8-type Genshin Elemental Resonance buffs & 4-Staff inventory limit\n");
                writer.write("enable_elemental_resonance = " + ENABLE_ELEMENTAL_RESONANCE.get() + "\n");
                writer.write("# Maximum Arcane Staves carried directly in inventory before Arcane Overload triggers (0 = unlimited)\n");
                writer.write("max_staves_in_inventory = " + MAX_STAVES_IN_INVENTORY.get() + "\n");
                writer.write("pyro_resonance_damage_bonus = " + PYRO_RESONANCE_DAMAGE_BONUS.get() + "\n");
                writer.write("elemental_armor_durability_wear = \"" + ELEMENTAL_ARMOR_DURABILITY_WEAR.get() + "\"\n");
                writer.write("# Show ambient elemental particles hovering around affected entities\n");
                writer.write("show_elemental_particles = " + SHOW_ELEMENTAL_PARTICLES.get() + "\n");
                writer.write("# Render Genshin-style overhead elemental aura icons above afflicted mobs\n");
                writer.write("show_overhead_element_icons = " + SHOW_OVERHEAD_ELEMENT_ICONS.get() + "\n");
                writer.write("# Client-side preference: use 32x32px textures for element icons instead of default 16x16px\n");
                writer.write("use_32px_element_icons = " + USE_32PX_ELEMENT_ICONS.get() + "\n");
                writer.write("# Render floating elemental reaction titles\n");
                writer.write("show_reaction_text = " + SHOW_REACTION_TEXT.get() + "\n");
                writer.write("# Render floating elemental damage numbers\n");
                writer.write("show_damage_numbers = " + SHOW_DAMAGE_NUMBERS.get() + "\n");
                writer.write("# Base chance (0.0 to 1.0) for eligible mobs to spawn mutated with an innate element (Default: 0.07 = 7%: 6% Elemental + 1% Omni Mutated)\n");
                writer.write("innate_mutation_chance = " + INNATE_MUTATION_CHANCE.get() + "\n\n");

                writer.write("[innate_mutation]\n");
                writer.write("# Allow standard hostile mobs (Enemy / MONSTER category, excluding Bosses) to mutate naturally\n");
                writer.write("allow_hostile_mob_mutation = " + ALLOW_HOSTILE_MOB_MUTATION.get() + "\n");
                writer.write("# Allow Golems (AbstractGolem such as minecraft:iron_golem) without natural innate elements to mutate\n");
                writer.write("allow_golem_mutation = " + ALLOW_GOLEM_MUTATION.get() + "\n");
                writer.write("# Allow non-hostile neutral/passive mobs (excluding Bosses, Villagers/NPCs, Aquatic/Ambient) to mutate unless whitelisted\n");
                writer.write("allow_neutral_and_passive_mutation = " + ALLOW_NEUTRAL_AND_PASSIVE_MUTATION.get() + "\n");
                writer.write("# Fraction (0.0 to 1.0) of successful mutations that become Omni Mutated (Default: 0.143 ~= 1/7)\n");
                writer.write("omni_mutation_share = " + OMNI_MUTATION_SHARE.get() + "\n");
                writer.write("# Explicit entity IDs (e.g. \"minecraft:iron_golem\", \"minecraft:wolf\", \"minecraft:fox\") or entity tags (\"#minecraft:raiders\") always allowed to mutate\n");
                writer.write("mutable_mob_whitelist = " + formatTomlStringList(MUTABLE_MOB_WHITELIST.get()) + "\n");
                writer.write("# Entity IDs or tags (\"#...\") excluded from innate mutation. Example: [\"minecraft:creeper\", \"minecraft:phantom\"]\n");
                writer.write("mutable_mob_blacklist = " + formatTomlStringList(MUTABLE_MOB_BLACKLIST.get()) + "\n");
                writer.write("# Per-mob mutation chance overrides in \"entity_id=rate\" format (0.0 to 1.0). Example: [\"minecraft:iron_golem=0.15\", \"minecraft:creeper=0.20\"]\n");
                writer.write("custom_mob_mutation_rates = " + formatTomlStringList(CUSTOM_MOB_MUTATION_RATES.get()) + "\n\n");

                writer.write("[cooldowns]\n");
                for (Map.Entry<String, ConfigEntry<?>> entry : ENTRIES.entrySet()) {
                    if (entry.getKey().startsWith("cooldowns.")) {
                        String shortKey = entry.getKey().substring("cooldowns.".length());
                        writer.write(shortKey + " = " + entry.getValue().get() + "\n");
                    }
                }
                writer.write("\n# Basic Staff Base Damages in HP (2.0 = 1 heart); Netherite automatically scales x1.5\n[damage]\n");
                for (Map.Entry<String, ConfigEntry<?>> entry : ENTRIES.entrySet()) {
                    if (entry.getKey().startsWith("damage.")) {
                        String shortKey = entry.getKey().substring("damage.".length());
                        writer.write(shortKey + " = " + entry.getValue().get() + "\n");
                    }
                }
                writer.write("\n[durability]\n");
                for (Map.Entry<String, ConfigEntry<?>> entry : ENTRIES.entrySet()) {
                    if (entry.getKey().startsWith("durability.")) {
                        String shortKey = entry.getKey().substring("durability.".length());
                        writer.write(shortKey + " = " + entry.getValue().get() + "\n");
                    }
                }

                writer.write("\n[scaling]\n");
                writer.write("enable_coop_mob_scaling = " + ENABLE_COOP_MOB_SCALING.get() + "\n");
                writer.write("hp_scale_per_extra_player = " + HP_SCALE_PER_EXTRA_PLAYER.get() + "\n");
                writer.write("enable_coop_atk_scaling = " + ENABLE_COOP_ATK_SCALING.get() + "\n");
                writer.write("max_coop_players_cap = " + MAX_COOP_PLAYERS_CAP.get() + "\n");
                writer.write("coop_scan_radius = " + COOP_SCAN_RADIUS.get() + "\n");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write velorise_staves.toml", e);
        }
        invalidateMutationRulesCache();
        if (refreshSyncedSnapshot && syncedServerData != null) {
            syncedServerData = createSnapshotFromLocal();
        }
    }

    private static String formatTomlStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(list.get(i).replace("\"", "\\\"")).append('"');
        }
        sb.append("]");
        return sb.toString();
    }

    public static final class SyncedData {
        public final double damageMultiplier;
        public final double cooldownMultiplier;
        public final double voidRangeBasic;
        public final double voidRangeNetherite;
        public final double voidCooldown;
        public final double mirrorCooldown;
        public final double mirrorChannel;
        public final double[] staffCooldowns;
        public final int[] staffDurabilitiesBasic;
        public final int[] staffDurabilitiesNetherite;
        public final int voidDurabilityBasic;
        public final int voidDurabilityNetherite;
        public final int mirrorDurabilityBasic;
        public final int mirrorDurabilityNetherite;
        public final boolean enableElementalGauge;
        public final double innateMutationChance;
        public final boolean enableElementalResonance;
        public final int maxStavesInInventory;
        public final double[] staffDamages;

        public SyncedData(
                double damageMultiplier,
                double cooldownMultiplier,
                double voidRangeBasic,
                double voidRangeNetherite,
                double voidCooldown,
                double mirrorCooldown,
                double mirrorChannel,
                double[] staffCooldowns,
                int[] staffDurabilitiesBasic,
                int[] staffDurabilitiesNetherite,
                int voidDurabilityBasic,
                int voidDurabilityNetherite,
                int mirrorDurabilityBasic,
                int mirrorDurabilityNetherite,
                boolean enableElementalGauge,
                double innateMutationChance,
                boolean enableElementalResonance,
                int maxStavesInInventory,
                double[] staffDamages
        ) {
            this.damageMultiplier = damageMultiplier;
            this.cooldownMultiplier = cooldownMultiplier;
            this.voidRangeBasic = voidRangeBasic;
            this.voidRangeNetherite = voidRangeNetherite;
            this.voidCooldown = voidCooldown;
            this.mirrorCooldown = mirrorCooldown;
            this.mirrorChannel = mirrorChannel;
            this.staffCooldowns = staffCooldowns;
            this.staffDurabilitiesBasic = staffDurabilitiesBasic;
            this.staffDurabilitiesNetherite = staffDurabilitiesNetherite;
            this.voidDurabilityBasic = voidDurabilityBasic;
            this.voidDurabilityNetherite = voidDurabilityNetherite;
            this.mirrorDurabilityBasic = mirrorDurabilityBasic;
            this.mirrorDurabilityNetherite = mirrorDurabilityNetherite;
            this.enableElementalGauge = enableElementalGauge;
            this.innateMutationChance = innateMutationChance;
            this.enableElementalResonance = enableElementalResonance;
            this.maxStavesInInventory = maxStavesInInventory;
            this.staffDamages = staffDamages;
        }

        public void write(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeDouble(damageMultiplier);
            buf.writeDouble(cooldownMultiplier);
            buf.writeDouble(voidRangeBasic);
            buf.writeDouble(voidRangeNetherite);
            buf.writeDouble(voidCooldown);
            buf.writeDouble(mirrorCooldown);
            buf.writeDouble(mirrorChannel);
            buf.writeVarInt(staffCooldowns.length);
            for (int i = 0; i < staffCooldowns.length; i++) {
                buf.writeDouble(staffCooldowns[i]);
                buf.writeVarInt(staffDurabilitiesBasic[i]);
                buf.writeVarInt(staffDurabilitiesNetherite[i]);
            }
            buf.writeVarInt(voidDurabilityBasic);
            buf.writeVarInt(voidDurabilityNetherite);
            buf.writeVarInt(mirrorDurabilityBasic);
            buf.writeVarInt(mirrorDurabilityNetherite);
            buf.writeBoolean(enableElementalGauge);
            buf.writeDouble(innateMutationChance);
            buf.writeBoolean(enableElementalResonance);
            buf.writeVarInt(maxStavesInInventory);
            buf.writeVarInt(staffDamages.length);
            for (double dmg : staffDamages) {
                buf.writeDouble(dmg);
            }
        }

        public static SyncedData read(net.minecraft.network.FriendlyByteBuf buf) {
            double dmgMult = buf.readDouble();
            double cdMult = buf.readDouble();
            double vBasic = buf.readDouble();
            double vNeth = buf.readDouble();
            double vCd = buf.readDouble();
            double mCd = buf.readDouble();
            double mCh = buf.readDouble();
            int len = buf.readVarInt();
            double[] cds = new double[len];
            int[] dBasic = new int[len];
            int[] dNeth = new int[len];
            for (int i = 0; i < len; i++) {
                cds[i] = buf.readDouble();
                dBasic[i] = buf.readVarInt();
                dNeth[i] = buf.readVarInt();
            }
            int vdBasic = buf.readVarInt();
            int vdNeth = buf.readVarInt();
            int mdBasic = buf.readVarInt();
            int mdNeth = buf.readVarInt();
            boolean egtOn = buf.isReadable() ? buf.readBoolean() : true;
            double mutChance = buf.isReadable() ? buf.readDouble() : 0.07D;
            boolean resOn = buf.isReadable() ? buf.readBoolean() : true;
            int maxCap = buf.isReadable() ? buf.readVarInt() : 4;
            double[] dmgs = new double[len];
            for (int i = 0; i < len; i++) {
                dmgs[i] = getDefaultBasicDamage(ArcaneStaffItem.Kind.values()[Math.min(i, ArcaneStaffItem.Kind.values().length - 1)]);
            }
            if (buf.isReadable()) {
                int dmgLen = buf.readVarInt();
                for (int i = 0; i < dmgLen && buf.isReadable(); i++) {
                    double v = buf.readDouble();
                    if (i < dmgs.length) dmgs[i] = v;
                }
            }
            return new SyncedData(dmgMult, cdMult, vBasic, vNeth, vCd, mCd, mCh, cds, dBasic, dNeth, vdBasic, vdNeth, mdBasic, mdNeth, egtOn, mutChance, resOn, maxCap, dmgs);
        }
    }

    private static volatile SyncedData syncedServerData = null;

    public static void applyServerSync(SyncedData data) {
        syncedServerData = data;
    }

    public static void clearServerSync() {
        syncedServerData = null;
    }

    public static SyncedData createSnapshotFromLocal() {
        ArcaneStaffItem.Kind[] kinds = ArcaneStaffItem.Kind.values();
        double[] cds = new double[kinds.length];
        int[] dBasic = new int[kinds.length];
        int[] dNeth = new int[kinds.length];
        double[] dmgs = new double[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            cds[i] = getLocalStaffCooldownSeconds(kinds[i]);
            dBasic[i] = getLocalMaxDurability(kinds[i], false);
            dNeth[i] = getLocalMaxDurability(kinds[i], true);
            dmgs[i] = getLocalStaffBasicDamage(kinds[i]);
        }
        return new SyncedData(
                DAMAGE_MULTIPLIER.get(),
                COOLDOWN_MULTIPLIER.get(),
                VOID_TRANSLOCATOR_RANGE_BASIC.get(),
                VOID_TRANSLOCATOR_RANGE_NETHERITE.get(),
                VOID_TRANSLOCATOR_COOLDOWN_SECONDS.get(),
                MAGIC_MIRROR_COOLDOWN_SECONDS.get(),
                MAGIC_MIRROR_CHANNEL_SECONDS.get(),
                cds,
                dBasic,
                dNeth,
                VOID_TRANSLOCATOR_DURABILITY_BASIC.get(),
                VOID_TRANSLOCATOR_DURABILITY_NETHERITE.get(),
                MAGIC_MIRROR_DURABILITY_BASIC.get(),
                MAGIC_MIRROR_DURABILITY_NETHERITE.get(),
                ENABLE_ELEMENTAL_GAUGE.get(),
                INNATE_MUTATION_CHANCE.get(),
                ENABLE_ELEMENTAL_RESONANCE.get(),
                MAX_STAVES_IN_INVENTORY.get(),
                dmgs
        );
    }

    public static double getDamageMultiplier() {
        SyncedData s = syncedServerData;
        return s != null ? s.damageMultiplier : DAMAGE_MULTIPLIER.get();
    }

    public static double getCooldownMultiplier() {
        SyncedData s = syncedServerData;
        return s != null ? s.cooldownMultiplier : COOLDOWN_MULTIPLIER.get();
    }

    public static double getVoidTranslocatorRange(boolean isNetherite) {
        SyncedData s = syncedServerData;
        if (s != null) {
            return isNetherite ? s.voidRangeNetherite : s.voidRangeBasic;
        }
        return isNetherite ? VOID_TRANSLOCATOR_RANGE_NETHERITE.get() : VOID_TRANSLOCATOR_RANGE_BASIC.get();
    }

    public static double getVoidTranslocatorCooldownSeconds() {
        SyncedData s = syncedServerData;
        return s != null ? s.voidCooldown : VOID_TRANSLOCATOR_COOLDOWN_SECONDS.get();
    }

    public static double getMagicMirrorCooldownSeconds() {
        SyncedData s = syncedServerData;
        return s != null ? s.mirrorCooldown : MAGIC_MIRROR_COOLDOWN_SECONDS.get();
    }

    public static double getMagicMirrorChannelSeconds() {
        SyncedData s = syncedServerData;
        return s != null ? s.mirrorChannel : MAGIC_MIRROR_CHANNEL_SECONDS.get();
    }

    public static double getStaffCooldownSeconds(ArcaneStaffItem.Kind kind) {
        SyncedData s = syncedServerData;
        if (s != null && kind.ordinal() < s.staffCooldowns.length) {
            return s.staffCooldowns[kind.ordinal()];
        }
        return getLocalStaffCooldownSeconds(kind);
    }

    public static double getDefaultBasicDamage(ArcaneStaffItem.Kind kind) {
        return switch (kind) {
            case DRAGONFIRE -> 7.0D;
            case ECHO -> 11.0D;
            case INFERNO -> 3.0D;
            case GHASTFIRE -> 6.0D;
            case THOUSAND_BLAZING_SUNS -> 4.0D;
            case WITHER -> 4.0D;
            case SHULKER -> 4.0D;
            case GUARDIAN -> 2.0D;
            case SPLENDOR -> 1.0D;
            case EVOKER -> 3.0D;
            case VORTEX -> 4.0D;
            case LIGHTNING -> 1.0D;
            case FROST -> 3.0D;
            case NIGHTS_FOCUS -> 4.0D;
            case WEB -> 1.5D;
            case WIND -> 1.5D;
            case DAYBREAK -> 1.5D;
            case ENGULFING_LIGHTNING -> 4.0D;
            case OMNI -> 10.0D;
        };
    }

    public static double getStaffBasicDamage(ArcaneStaffItem.Kind kind) {
        SyncedData s = syncedServerData;
        if (s != null && s.staffDamages != null && kind.ordinal() < s.staffDamages.length) {
            return s.staffDamages[kind.ordinal()];
        }
        return getLocalStaffBasicDamage(kind);
    }

    public static float getStaffBaseDamage(ArcaneStaffItem.Kind kind, boolean isNetherite) {
        return (float) (getStaffBasicDamage(kind) * (isNetherite ? 1.5D : 1.0D));
    }

    public static float getStaffDamageScale(ArcaneStaffItem.Kind kind, boolean isNetherite) {
        double def = Math.max(0.001D, getDefaultBasicDamage(kind));
        return (float) ((getStaffBasicDamage(kind) / def) * (isNetherite ? 1.5D : 1.0D));
    }

    private static double getLocalStaffBasicDamage(ArcaneStaffItem.Kind kind) {
        return switch (kind) {
            case DRAGONFIRE -> DRAGONFIRE_DAMAGE.get();
            case ECHO -> ECHO_DAMAGE.get();
            case INFERNO -> INFERNO_DAMAGE.get();
            case GHASTFIRE -> GHASTFIRE_DAMAGE.get();
            case THOUSAND_BLAZING_SUNS -> THOUSAND_BLAZING_SUNS_DAMAGE.get();
            case WITHER -> WITHER_DAMAGE.get();
            case SHULKER -> SHULKER_DAMAGE.get();
            case GUARDIAN -> GUARDIAN_DAMAGE.get();
            case SPLENDOR -> SPLENDOR_DAMAGE.get();
            case EVOKER -> EVOKER_DAMAGE.get();
            case VORTEX -> VORTEX_DAMAGE.get();
            case LIGHTNING -> ARC_CASTER_DAMAGE.get();
            case FROST -> FROSTBOLT_DAMAGE.get();
            case NIGHTS_FOCUS -> NIGHTS_FOCUS_DAMAGE.get();
            case WEB -> WEBCASTER_DAMAGE.get();
            case WIND -> WIND_DAMAGE.get();
            case DAYBREAK -> DAYBREAK_DAMAGE.get();
            case ENGULFING_LIGHTNING -> ENGULFING_LIGHTNING_DAMAGE.get();
            case OMNI -> OMNI_DAMAGE.get();
        };
    }

    public static int getMaxDurability(ArcaneStaffItem.Kind kind, boolean isNetherite) {
        SyncedData s = syncedServerData;
        if (s != null && kind.ordinal() < s.staffDurabilitiesBasic.length) {
            return isNetherite ? s.staffDurabilitiesNetherite[kind.ordinal()] : s.staffDurabilitiesBasic[kind.ordinal()];
        }
        return getLocalMaxDurability(kind, isNetherite);
    }

    private static int getLocalMaxDurability(ArcaneStaffItem.Kind kind, boolean isNetherite) {
        return switch (kind) {
            case DRAGONFIRE -> isNetherite ? DRAGONFIRE_DURABILITY_NETHERITE.get() : DRAGONFIRE_DURABILITY_BASIC.get();
            case ECHO -> isNetherite ? ECHO_DURABILITY_NETHERITE.get() : ECHO_DURABILITY_BASIC.get();
            case INFERNO -> isNetherite ? INFERNO_DURABILITY_NETHERITE.get() : INFERNO_DURABILITY_BASIC.get();
            case GHASTFIRE -> isNetherite ? GHASTFIRE_DURABILITY_NETHERITE.get() : GHASTFIRE_DURABILITY_BASIC.get();
            case THOUSAND_BLAZING_SUNS -> isNetherite ? THOUSAND_BLAZING_SUNS_DURABILITY_NETHERITE.get() : THOUSAND_BLAZING_SUNS_DURABILITY_BASIC.get();
            case WITHER -> isNetherite ? WITHER_DURABILITY_NETHERITE.get() : WITHER_DURABILITY_BASIC.get();
            case SHULKER -> isNetherite ? SHULKER_DURABILITY_NETHERITE.get() : SHULKER_DURABILITY_BASIC.get();
            case GUARDIAN -> isNetherite ? GUARDIAN_DURABILITY_NETHERITE.get() : GUARDIAN_DURABILITY_BASIC.get();
            case SPLENDOR -> isNetherite ? SPLENDOR_DURABILITY_NETHERITE.get() : SPLENDOR_DURABILITY_BASIC.get();
            case EVOKER -> isNetherite ? EVOKER_DURABILITY_NETHERITE.get() : EVOKER_DURABILITY_BASIC.get();
            case VORTEX -> isNetherite ? VORTEX_DURABILITY_NETHERITE.get() : VORTEX_DURABILITY_BASIC.get();
            case LIGHTNING -> isNetherite ? ARC_CASTER_DURABILITY_NETHERITE.get() : ARC_CASTER_DURABILITY_BASIC.get();
            case FROST, NIGHTS_FOCUS -> isNetherite ? FROSTBOLT_DURABILITY_NETHERITE.get() : FROSTBOLT_DURABILITY_BASIC.get();
            case WEB -> isNetherite ? WEBCASTER_DURABILITY_NETHERITE.get() : WEBCASTER_DURABILITY_BASIC.get();
            case WIND -> isNetherite ? WIND_DURABILITY_NETHERITE.get() : WIND_DURABILITY_BASIC.get();
            case DAYBREAK -> isNetherite ? WIND_DURABILITY_NETHERITE.get() : WIND_DURABILITY_BASIC.get();
            case ENGULFING_LIGHTNING -> isNetherite ? ARC_CASTER_DURABILITY_NETHERITE.get() : ARC_CASTER_DURABILITY_BASIC.get();
            case OMNI -> isNetherite ? OMNI_DURABILITY_NETHERITE.get() : OMNI_DURABILITY_BASIC.get();
        };
    }

    public static int getArtifactMaxDurability(boolean isTranslocator, boolean isNetherite) {
        SyncedData s = syncedServerData;
        if (s != null) {
            if (isTranslocator) {
                return isNetherite ? s.voidDurabilityNetherite : s.voidDurabilityBasic;
            } else {
                return isNetherite ? s.mirrorDurabilityNetherite : s.mirrorDurabilityBasic;
            }
        }
        if (isTranslocator) {
            return isNetherite ? VOID_TRANSLOCATOR_DURABILITY_NETHERITE.get() : VOID_TRANSLOCATOR_DURABILITY_BASIC.get();
        } else {
            return isNetherite ? MAGIC_MIRROR_DURABILITY_NETHERITE.get() : MAGIC_MIRROR_DURABILITY_BASIC.get();
        }
    }

    private static double getLocalStaffCooldownSeconds(ArcaneStaffItem.Kind kind) {
        return switch (kind) {
            case DRAGONFIRE -> DRAGONFIRE_COOLDOWN.get();
            case ECHO -> ECHO_COOLDOWN.get();
            case INFERNO -> INFERNO_COOLDOWN.get();
            case GHASTFIRE -> GHASTFIRE_COOLDOWN.get();
            case THOUSAND_BLAZING_SUNS -> THOUSAND_BLAZING_SUNS_COOLDOWN.get();
            case WITHER -> WITHER_COOLDOWN.get();
            case SHULKER -> SHULKER_COOLDOWN.get();
            case GUARDIAN -> GUARDIAN_COOLDOWN.get();
            case SPLENDOR -> SPLENDOR_COOLDOWN.get();
            case EVOKER -> EVOKER_COOLDOWN.get();
            case VORTEX -> VORTEX_COOLDOWN.get();
            case LIGHTNING -> ARC_CASTER_COOLDOWN.get();
            case FROST -> FROSTBOLT_COOLDOWN.get();
            case NIGHTS_FOCUS -> 12.0D;
            case WEB -> WEBCASTER_COOLDOWN.get();
            case WIND -> WIND_COOLDOWN.get();
            case DAYBREAK -> DAYBREAK_COOLDOWN.get();
            case ENGULFING_LIGHTNING -> ENGULFING_LIGHTNING_COOLDOWN.get();
            case OMNI -> OMNI_COOLDOWN.get();
        };
    }





    public static boolean isServerSynced() {
        return syncedServerData != null;
    }

    public static int getBasicStaffDurability() {
        return getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, false);
    }

    public static int getNetheriteStaffDurability() {
        return getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, true);
    }

    public static double getRangeMultiplier() {
        return 1.0;
    }

    public static boolean isUse32pxElementIcons() {
        return USE_32PX_ELEMENT_ICONS.get();
    }

    public static boolean isElementalGaugeEnabled() {
        SyncedData s = syncedServerData;
        return s != null ? s.enableElementalGauge : ENABLE_ELEMENTAL_GAUGE.get();
    }

    public static double getInnateMutationChance() {
        SyncedData s = syncedServerData;
        return s != null ? s.innateMutationChance : INNATE_MUTATION_CHANCE.get();
    }

    public static boolean isElementalResonanceEnabled() {
        SyncedData s = syncedServerData;
        return s != null ? s.enableElementalResonance : ENABLE_ELEMENTAL_RESONANCE.get();
    }

    public static int getMaxStavesInInventory() {
        SyncedData s = syncedServerData;
        return s != null ? s.maxStavesInInventory : MAX_STAVES_IN_INVENTORY.get();
    }

    public interface ConfigEntry<T> {
        T get();
        void parseAndSet(String raw);
    }

    public static final class DoubleValue implements ConfigEntry<Double> {
        private double value;
        private final double min;
        private final double max;

        DoubleValue(double def, double min, double max) {
            this.value = def;
            this.min = min;
            this.max = max;
        }

        @Override
        public Double get() {
            return value;
        }

        public void set(double newValue) {
            this.value = Math.max(min, Math.min(max, newValue));
        }

        @Override
        public void parseAndSet(String raw) {
            try {
                double parsed = Double.parseDouble(raw);
                this.value = Math.max(min, Math.min(max, parsed));
            } catch (NumberFormatException ignored) {}
        }
    }

    public static final class IntValue implements ConfigEntry<Integer> {
        private int value;
        private final int min;
        private final int max;

        IntValue(int def, int min, int max) {
            this.value = def;
            this.min = min;
            this.max = max;
        }

        @Override
        public Integer get() {
            return value;
        }

        public void set(int newValue) {
            this.value = Math.max(min, Math.min(max, newValue));
        }

        @Override
        public void parseAndSet(String raw) {
            try {
                int parsed = Integer.parseInt(raw);
                this.value = Math.max(min, Math.min(max, parsed));
            } catch (NumberFormatException ignored) {}
        }
    }

    public static final class BooleanValue implements ConfigEntry<Boolean> {
        private boolean value;

        BooleanValue(boolean def) {
            this.value = def;
        }

        @Override
        public Boolean get() {
            return value;
        }

        public void set(boolean newValue) {
            this.value = newValue;
        }

        @Override
        public void parseAndSet(String raw) {
            this.value = Boolean.parseBoolean(raw);
        }
    }

    private static DoubleValue defineDouble(String section, String key, double def, double min, double max) {
        DoubleValue v = new DoubleValue(def, min, max);
        ENTRIES.put(section + "." + key, v);
        return v;
    }

    private static IntValue defineInt(String section, String key, int def, int min, int max) {
        IntValue v = new IntValue(def, min, max);
        ENTRIES.put(section + "." + key, v);
        return v;
    }

    private static BooleanValue defineBool(String section, String key, boolean def) {
        BooleanValue v = new BooleanValue(def);
        ENTRIES.put(section + "." + key, v);
        return v;
    }

    public static final class StringValue implements ConfigEntry<String> {
        private String value;

        StringValue(String def) {
            this.value = def;
        }

        @Override
        public String get() {
            return value;
        }

        public void set(String newValue) {
            this.value = newValue != null ? newValue.trim() : "auto";
        }

        @Override
        public void parseAndSet(String raw) {
            if (raw == null) return;
            String s = raw.trim();
            if ((s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
                    || (s.startsWith("'") && s.endsWith("'") && s.length() >= 2)) {
                s = s.substring(1, s.length() - 1).trim();
            }
            if (!s.isEmpty()) {
                this.value = s;
            }
        }
    }

    private static StringValue defineString(String section, String key, String def) {
        StringValue v = new StringValue(def);
        ENTRIES.put(section + "." + key, v);
        return v;
    }

    public static final class StringListValue implements ConfigEntry<List<String>> {
        private List<String> value;

        StringListValue(List<String> def) {
            this.value = Collections.unmodifiableList(new ArrayList<>(def));
        }

        @Override
        public List<String> get() {
            return value;
        }

        public void set(List<String> newValue) {
            this.value = newValue == null
                    ? List.of()
                    : Collections.unmodifiableList(new ArrayList<>(newValue));
        }

        @Override
        public void parseAndSet(String raw) {
            if (raw == null) return;
            String s = raw.trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                s = s.substring(1, s.length() - 1).trim();
            }
            List<String> parsed = new ArrayList<>();
            if (!s.isEmpty()) {
                for (String part : s.split(",")) {
                    String item = part.trim();
                    if ((item.startsWith("\"") && item.endsWith("\"") && item.length() >= 2)
                            || (item.startsWith("'") && item.endsWith("'") && item.length() >= 2)) {
                        item = item.substring(1, item.length() - 1).trim();
                    }
                    if (!item.isEmpty()) {
                        parsed.add(item);
                    }
                }
            }
            this.value = Collections.unmodifiableList(parsed);
        }
    }

    private static StringListValue defineStringList(String section, String key, List<String> def) {
        StringListValue v = new StringListValue(def);
        ENTRIES.put(section + "." + key, v);
        return v;
    }

    public static final class MutationRules {
        public final boolean allowHostile;
        public final boolean allowGolems;
        public final boolean allowNeutralAndPassive;
        public final double defaultChance;
        public final double omniShare;
        private final List<String> whitelistRef;
        private final List<String> blacklistRef;
        private final List<String> customRatesRef;
        private final Set<ResourceLocation> whitelistIds;
        private final List<TagKey<EntityType<?>>> whitelistTags;
        private final Set<ResourceLocation> blacklistIds;
        private final List<TagKey<EntityType<?>>> blacklistTags;
        private final Map<ResourceLocation, Double> customRates;

        private MutationRules(
                boolean allowHostile,
                boolean allowGolems,
                boolean allowNeutralAndPassive,
                double defaultChance,
                double omniShare,
                List<String> whitelistRef,
                List<String> blacklistRef,
                List<String> customRatesRef
        ) {
            this.allowHostile = allowHostile;
            this.allowGolems = allowGolems;
            this.allowNeutralAndPassive = allowNeutralAndPassive;
            this.defaultChance = Math.max(0.0D, Math.min(1.0D, defaultChance));
            this.omniShare = Math.max(0.0D, Math.min(1.0D, omniShare));
            this.whitelistRef = whitelistRef;
            this.blacklistRef = blacklistRef;
            this.customRatesRef = customRatesRef;
            this.whitelistIds = new HashSet<>();
            this.whitelistTags = new ArrayList<>();
            this.blacklistIds = new HashSet<>();
            this.blacklistTags = new ArrayList<>();
            this.customRates = new HashMap<>();
            parseEntityList(whitelistRef, this.whitelistIds, this.whitelistTags);
            parseEntityList(blacklistRef, this.blacklistIds, this.blacklistTags);
            if (customRatesRef != null) {
                for (String raw : customRatesRef) {
                    if (raw == null) continue;
                    int eq = raw.indexOf('=');
                    if (eq <= 0 || eq >= raw.length() - 1) continue;
                    String idStr = raw.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                    String rateStr = raw.substring(eq + 1).trim();
                    ResourceLocation rl = ResourceLocation.tryParse(idStr);
                    if (rl == null) continue;
                    try {
                        double r = Double.parseDouble(rateStr);
                        this.customRates.put(rl, Math.max(0.0D, Math.min(1.0D, r)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        private static void parseEntityList(
                List<String> rawList,
                Set<ResourceLocation> ids,
                List<TagKey<EntityType<?>>> tags
        ) {
            if (rawList == null) return;
            for (String raw : rawList) {
                if (raw == null) continue;
                String s = raw.trim().toLowerCase(Locale.ROOT);
                if (s.isEmpty()) continue;
                if (s.startsWith("#")) {
                    ResourceLocation rl = ResourceLocation.tryParse(s.substring(1).trim());
                    if (rl != null) {
                        tags.add(TagKey.create(Registries.ENTITY_TYPE, rl));
                    }
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(s);
                    if (rl != null) {
                        ids.add(rl);
                    }
                }
            }
        }

        private boolean matches(
                boolean ah, boolean ag, boolean anp, double dc, double os,
                List<String> wl, List<String> bl, List<String> cr
        ) {
            return this.allowHostile == ah
                    && this.allowGolems == ag
                    && this.allowNeutralAndPassive == anp
                    && Double.compare(this.defaultChance, Math.max(0.0D, Math.min(1.0D, dc))) == 0
                    && Double.compare(this.omniShare, Math.max(0.0D, Math.min(1.0D, os))) == 0
                    && this.whitelistRef == wl
                    && this.blacklistRef == bl
                    && this.customRatesRef == cr;
        }

        public double resolveMutationChance(LivingEntity entity) {
            if (entity == null || entity instanceof Player || entity instanceof Npc) {
                return -1.0D;
            }
            EntityType<?> type = entity.getType();
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (blacklistIds.contains(key)) {
                return -1.0D;
            }
            for (TagKey<EntityType<?>> tag : blacklistTags) {
                if (type.is(tag)) {
                    return -1.0D;
                }
            }
            Double custom = customRates.get(key);
            if (custom != null) {
                return custom > 0.0D ? custom : -1.0D;
            }
            if (whitelistIds.contains(key)) {
                return defaultChance;
            }
            for (TagKey<EntityType<?>> tag : whitelistTags) {
                if (type.is(tag)) {
                    return defaultChance;
                }
            }
            boolean isBoss = entity instanceof Warden || entity instanceof EnderDragon
                    || entity instanceof WitherBoss || entity instanceof ElderGuardian;
            boolean isAquaticOrAmbient = entity instanceof WaterAnimal || entity instanceof AmbientCreature;
            if (isBoss || isAquaticOrAmbient) {
                return -1.0D;
            }
            if (entity instanceof AbstractGolem) {
                return allowGolems ? defaultChance : -1.0D;
            }
            if (entity instanceof Enemy || type.getCategory() == MobCategory.MONSTER) {
                return allowHostile ? defaultChance : -1.0D;
            }
            return allowNeutralAndPassive ? defaultChance : -1.0D;
        }
    }

    private static volatile MutationRules cachedMutationRules = null;

    public static void invalidateMutationRulesCache() {
        cachedMutationRules = null;
    }

    public static MutationRules getMutationRules() {
        boolean ah = ALLOW_HOSTILE_MOB_MUTATION.get();
        boolean ag = ALLOW_GOLEM_MUTATION.get();
        boolean anp = ALLOW_NEUTRAL_AND_PASSIVE_MUTATION.get();
        double dc = INNATE_MUTATION_CHANCE.get();
        double os = OMNI_MUTATION_SHARE.get();
        List<String> wl = MUTABLE_MOB_WHITELIST.get();
        List<String> bl = MUTABLE_MOB_BLACKLIST.get();
        List<String> cr = CUSTOM_MOB_MUTATION_RATES.get();
        MutationRules snap = cachedMutationRules;
        if (snap != null && snap.matches(ah, ag, anp, dc, os, wl, bl, cr)) {
            return snap;
        }
        MutationRules rebuilt = new MutationRules(ah, ag, anp, dc, os, wl, bl, cr);
        cachedMutationRules = rebuilt;
        return rebuilt;
    }
}
