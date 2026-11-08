package com.velorise.staves.config;

import com.velorise.staves.item.ArcaneStaffItem;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StavesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("velorise_staves_config");
    private static final Map<String, ConfigEntry<?>> ENTRIES = new LinkedHashMap<>();

    // Mechanics
    public static final DoubleValue DAMAGE_MULTIPLIER = defineDouble("mechanics", "damage_multiplier", 1.0D, 0.1D, 10.0D);
    public static final DoubleValue COOLDOWN_MULTIPLIER = defineDouble("mechanics", "cooldown_multiplier", 1.0D, 0.1D, 10.0D);
    public static final BooleanValue STORMCALLER_CHARGE_CREEPER = defineBool("mechanics", "stormcaller_charge_creeper", true);
    public static final BooleanValue BASIC_MIRROR_CROSS_DIMENSION = defineBool("mechanics", "basic_mirror_cross_dimension", false);
    public static final IntValue TEMPORARY_WEB_DURATION_TICKS = defineInt("mechanics", "temporary_web_duration_ticks", 100, 20, 1200);
    public static final DoubleValue VOID_TRANSLOCATOR_RANGE_BASIC = defineDouble("mechanics", "void_translocator_range_basic", 24.0D, 4.0D, 128.0D);
    public static final DoubleValue VOID_TRANSLOCATOR_RANGE_NETHERITE = defineDouble("mechanics", "void_translocator_range_netherite", 36.0D, 4.0D, 128.0D);
    public static final DoubleValue VOID_TRANSLOCATOR_COOLDOWN_SECONDS = defineDouble("mechanics", "void_translocator_cooldown_seconds", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue MAGIC_MIRROR_COOLDOWN_SECONDS = defineDouble("mechanics", "magic_mirror_cooldown_seconds", 15.0D, 1.0D, 300.0D);
    public static final DoubleValue MAGIC_MIRROR_CHANNEL_SECONDS = defineDouble("mechanics", "magic_mirror_channel_seconds", 2.5D, 0.5D, 10.0D);

    // Elemental Gauge Theory (EGT)
    public static final BooleanValue ENABLE_ELEMENTAL_GAUGE = defineBool("elemental_gauge", "enable_elemental_gauge", true);
    public static final BooleanValue SHOW_ACTIONBAR_FEEDBACK = defineBool("elemental_gauge", "show_actionbar_feedback", false);
    public static final BooleanValue SHOW_ELEMENTAL_PARTICLES = defineBool("elemental_gauge", "show_elemental_particles", true);
    public static final BooleanValue SHOW_OVERHEAD_ELEMENT_ICONS = defineBool("elemental_gauge", "show_overhead_element_icons", true);

    // Cooldowns
    public static final DoubleValue DRAGONFIRE_COOLDOWN = defineDouble("cooldowns", "dragonfire_cooldown", 8.0D, 0.5D, 60.0D);
    public static final DoubleValue ECHO_COOLDOWN = defineDouble("cooldowns", "echo_cooldown", 8.0D, 0.5D, 60.0D);
    public static final DoubleValue INFERNO_COOLDOWN = defineDouble("cooldowns", "inferno_cooldown", 4.0D, 0.5D, 60.0D);
    public static final DoubleValue GHASTFIRE_COOLDOWN = defineDouble("cooldowns", "ghastfire_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue WITHER_COOLDOWN = defineDouble("cooldowns", "wither_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue SHULKER_COOLDOWN = defineDouble("cooldowns", "shulker_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue GUARDIAN_COOLDOWN = defineDouble("cooldowns", "guardian_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue EVOKER_COOLDOWN = defineDouble("cooldowns", "evoker_cooldown", 5.0D, 0.5D, 60.0D);
    public static final DoubleValue STORMCALLER_COOLDOWN = defineDouble("cooldowns", "stormcaller_cooldown", 6.0D, 0.5D, 60.0D);
    public static final DoubleValue FROSTBOLT_COOLDOWN = defineDouble("cooldowns", "frostbolt_cooldown", 4.0D, 0.5D, 60.0D);
    public static final DoubleValue WEBCASTER_COOLDOWN = defineDouble("cooldowns", "webcaster_cooldown", 5.0D, 0.5D, 60.0D);

    // Durabilities
    public static final IntValue DRAGONFIRE_DURABILITY_BASIC = defineInt("durability", "dragonfire_basic", 130, 10, 5000);
    public static final IntValue DRAGONFIRE_DURABILITY_NETHERITE = defineInt("durability", "dragonfire_netherite", 500, 10, 5000);
    public static final IntValue ECHO_DURABILITY_BASIC = defineInt("durability", "echo_basic", 130, 10, 5000);
    public static final IntValue ECHO_DURABILITY_NETHERITE = defineInt("durability", "echo_netherite", 500, 10, 5000);
    public static final IntValue INFERNO_DURABILITY_BASIC = defineInt("durability", "inferno_basic", 150, 10, 5000);
    public static final IntValue INFERNO_DURABILITY_NETHERITE = defineInt("durability", "inferno_netherite", 550, 10, 5000);
    public static final IntValue GHASTFIRE_DURABILITY_BASIC = defineInt("durability", "ghastfire_basic", 120, 10, 5000);
    public static final IntValue GHASTFIRE_DURABILITY_NETHERITE = defineInt("durability", "ghastfire_netherite", 450, 10, 5000);
    public static final IntValue WITHER_DURABILITY_BASIC = defineInt("durability", "wither_basic", 140, 10, 5000);
    public static final IntValue WITHER_DURABILITY_NETHERITE = defineInt("durability", "wither_netherite", 500, 10, 5000);
    public static final IntValue SHULKER_DURABILITY_BASIC = defineInt("durability", "shulker_basic", 130, 10, 5000);
    public static final IntValue SHULKER_DURABILITY_NETHERITE = defineInt("durability", "shulker_netherite", 500, 10, 5000);
    public static final IntValue GUARDIAN_DURABILITY_BASIC = defineInt("durability", "guardian_basic", 130, 10, 5000);
    public static final IntValue GUARDIAN_DURABILITY_NETHERITE = defineInt("durability", "guardian_netherite", 500, 10, 5000);
    public static final IntValue EVOKER_DURABILITY_BASIC = defineInt("durability", "evoker_basic", 120, 10, 5000);
    public static final IntValue EVOKER_DURABILITY_NETHERITE = defineInt("durability", "evoker_netherite", 450, 10, 5000);
    public static final IntValue STORMCALLER_DURABILITY_BASIC = defineInt("durability", "stormcaller_basic", 130, 10, 5000);
    public static final IntValue STORMCALLER_DURABILITY_NETHERITE = defineInt("durability", "stormcaller_netherite", 500, 10, 5000);
    public static final IntValue FROSTBOLT_DURABILITY_BASIC = defineInt("durability", "frostbolt_basic", 150, 10, 5000);
    public static final IntValue FROSTBOLT_DURABILITY_NETHERITE = defineInt("durability", "frostbolt_netherite", 500, 10, 5000);
    public static final IntValue WEBCASTER_DURABILITY_BASIC = defineInt("durability", "webcaster_basic", 140, 10, 5000);
    public static final IntValue WEBCASTER_DURABILITY_NETHERITE = defineInt("durability", "webcaster_netherite", 500, 10, 5000);
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
                    String val = line.substring(eqIdx + 1).trim();
                    if (val.contains("#")) {
                        val = val.substring(0, val.indexOf('#')).trim();
                    }
                    String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    ConfigEntry<?> entry = ENTRIES.get(fullKey);
                    if (entry != null) {
                        entry.parseAndSet(val);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse velorise_staves.toml, falling back to defaults", e);
            save();
        }
    }

    public static void save() {
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
                writer.write("# Whether Stormcaller lightning strikes charge creepers and trigger mob conversions\n");
                writer.write("stormcaller_charge_creeper = " + STORMCALLER_CHARGE_CREEPER.get() + "\n");
                writer.write("# Allow basic Magic Mirror to teleport across dimensions (Netherite tier always can)\n");
                writer.write("basic_mirror_cross_dimension = " + BASIC_MIRROR_CROSS_DIMENSION.get() + "\n");
                writer.write("# Duration in ticks before temporary cobwebs vanish (20 ticks = 1s)\n");
                writer.write("temporary_web_duration_ticks = " + TEMPORARY_WEB_DURATION_TICKS.get() + "\n");
                writer.write("void_translocator_range_basic = " + VOID_TRANSLOCATOR_RANGE_BASIC.get() + "\n");
                writer.write("void_translocator_range_netherite = " + VOID_TRANSLOCATOR_RANGE_NETHERITE.get() + "\n");
                writer.write("void_translocator_cooldown_seconds = " + VOID_TRANSLOCATOR_COOLDOWN_SECONDS.get() + "\n");
                writer.write("magic_mirror_cooldown_seconds = " + MAGIC_MIRROR_COOLDOWN_SECONDS.get() + "\n");
                writer.write("magic_mirror_channel_seconds = " + MAGIC_MIRROR_CHANNEL_SECONDS.get() + "\n\n");

                writer.write("[cooldowns]\n");
                for (Map.Entry<String, ConfigEntry<?>> entry : ENTRIES.entrySet()) {
                    if (entry.getKey().startsWith("cooldowns.")) {
                        String shortKey = entry.getKey().substring("cooldowns.".length());
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
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write velorise_staves.toml", e);
        }
    }

    public static double getStaffCooldownSeconds(ArcaneStaffItem.Kind kind) {
        return switch (kind) {
            case DRAGONFIRE -> DRAGONFIRE_COOLDOWN.get();
            case ECHO -> ECHO_COOLDOWN.get();
            case INFERNO -> INFERNO_COOLDOWN.get();
            case GHASTFIRE -> GHASTFIRE_COOLDOWN.get();
            case WITHER -> WITHER_COOLDOWN.get();
            case SHULKER -> SHULKER_COOLDOWN.get();
            case GUARDIAN -> GUARDIAN_COOLDOWN.get();
            case EVOKER -> EVOKER_COOLDOWN.get();
            case LIGHTNING -> STORMCALLER_COOLDOWN.get();
            case FROST -> FROSTBOLT_COOLDOWN.get();
            case WEB -> WEBCASTER_COOLDOWN.get();
        };
    }

    public static int getMaxDurability(ArcaneStaffItem.Kind kind, boolean isNetherite) {
        return switch (kind) {
            case DRAGONFIRE -> isNetherite ? DRAGONFIRE_DURABILITY_NETHERITE.get() : DRAGONFIRE_DURABILITY_BASIC.get();
            case ECHO -> isNetherite ? ECHO_DURABILITY_NETHERITE.get() : ECHO_DURABILITY_BASIC.get();
            case INFERNO -> isNetherite ? INFERNO_DURABILITY_NETHERITE.get() : INFERNO_DURABILITY_BASIC.get();
            case GHASTFIRE -> isNetherite ? GHASTFIRE_DURABILITY_NETHERITE.get() : GHASTFIRE_DURABILITY_BASIC.get();
            case WITHER -> isNetherite ? WITHER_DURABILITY_NETHERITE.get() : WITHER_DURABILITY_BASIC.get();
            case SHULKER -> isNetherite ? SHULKER_DURABILITY_NETHERITE.get() : SHULKER_DURABILITY_BASIC.get();
            case GUARDIAN -> isNetherite ? GUARDIAN_DURABILITY_NETHERITE.get() : GUARDIAN_DURABILITY_BASIC.get();
            case EVOKER -> isNetherite ? EVOKER_DURABILITY_NETHERITE.get() : EVOKER_DURABILITY_BASIC.get();
            case LIGHTNING -> isNetherite ? STORMCALLER_DURABILITY_NETHERITE.get() : STORMCALLER_DURABILITY_BASIC.get();
            case FROST -> isNetherite ? FROSTBOLT_DURABILITY_NETHERITE.get() : FROSTBOLT_DURABILITY_BASIC.get();
            case WEB -> isNetherite ? WEBCASTER_DURABILITY_NETHERITE.get() : WEBCASTER_DURABILITY_BASIC.get();
        };
    }

    public static int getArtifactMaxDurability(boolean isTranslocator, boolean isNetherite) {
        if (isTranslocator) {
            return isNetherite ? VOID_TRANSLOCATOR_DURABILITY_NETHERITE.get() : VOID_TRANSLOCATOR_DURABILITY_BASIC.get();
        } else {
            return isNetherite ? MAGIC_MIRROR_DURABILITY_NETHERITE.get() : MAGIC_MIRROR_DURABILITY_BASIC.get();
        }
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
}
