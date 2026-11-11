package com.velorise.staves.config;

import com.velorise.staves.item.ArcaneStaffItem;
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
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StavesConfig {
    public static final ModConfigSpec SPEC;

    // Mechanics
    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ARC_CASTER_CHARGE_CREEPER;
    public static final ModConfigSpec.BooleanValue BASIC_MIRROR_CROSS_DIMENSION;
    public static final ModConfigSpec.IntValue TEMPORARY_WEB_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue VOID_TRANSLOCATOR_RANGE_BASIC;
    public static final ModConfigSpec.DoubleValue VOID_TRANSLOCATOR_RANGE_NETHERITE;
    public static final ModConfigSpec.DoubleValue VOID_TRANSLOCATOR_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue MAGIC_MIRROR_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue MAGIC_MIRROR_CHANNEL_SECONDS;

    // Elemental Gauge Theory (EGT)
    public static final ModConfigSpec.BooleanValue ENABLE_ELEMENTAL_GAUGE;
    public static final ModConfigSpec.BooleanValue ENABLE_ELEMENTAL_RESONANCE;
    public static final ModConfigSpec.IntValue MAX_STAVES_IN_INVENTORY;
    public static final ModConfigSpec.BooleanValue SHOW_ELEMENTAL_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_OVERHEAD_ELEMENT_ICONS;
    public static final ModConfigSpec.BooleanValue SHOW_REACTION_TEXT;
    public static final ModConfigSpec.BooleanValue SHOW_DAMAGE_NUMBERS;
    public static final ModConfigSpec.DoubleValue INNATE_MUTATION_CHANCE;

    // Innate Mutation Rules & Whitelist/Blacklist
    public static final ModConfigSpec.BooleanValue ALLOW_HOSTILE_MOB_MUTATION;
    public static final ModConfigSpec.BooleanValue ALLOW_GOLEM_MUTATION;
    public static final ModConfigSpec.BooleanValue ALLOW_NEUTRAL_AND_PASSIVE_MUTATION;
    public static final ModConfigSpec.DoubleValue OMNI_MUTATION_SHARE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MUTABLE_MOB_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MUTABLE_MOB_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_MOB_MUTATION_RATES;

    // Cooldowns
    public static final ModConfigSpec.DoubleValue DRAGONFIRE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ECHO_COOLDOWN;
    public static final ModConfigSpec.DoubleValue INFERNO_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GHASTFIRE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WITHER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SHULKER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GUARDIAN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue EVOKER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ARC_CASTER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FROSTBOLT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WEBCASTER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WIND_COOLDOWN;
    public static final ModConfigSpec.DoubleValue OMNI_COOLDOWN;

    // Durabilities
    public static final ModConfigSpec.IntValue DRAGONFIRE_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue DRAGONFIRE_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue ECHO_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue ECHO_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue INFERNO_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue INFERNO_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue GHASTFIRE_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue GHASTFIRE_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue WITHER_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue WITHER_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue SHULKER_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue SHULKER_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue GUARDIAN_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue GUARDIAN_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue EVOKER_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue EVOKER_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue ARC_CASTER_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue ARC_CASTER_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue FROSTBOLT_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue FROSTBOLT_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue WEBCASTER_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue WEBCASTER_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue WIND_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue WIND_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue OMNI_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue OMNI_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue VOID_TRANSLOCATOR_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue VOID_TRANSLOCATOR_DURABILITY_NETHERITE;
    public static final ModConfigSpec.IntValue MAGIC_MIRROR_DURABILITY_BASIC;
    public static final ModConfigSpec.IntValue MAGIC_MIRROR_DURABILITY_NETHERITE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Velorise's Staves Configuration").push("mechanics");

        DAMAGE_MULTIPLIER = builder
            .comment("Global damage multiplier for all combat staves (0.1 to 10.0). Default: 1.0")
            .defineInRange("damage_multiplier", 1.0D, 0.1D, 10.0D);

        COOLDOWN_MULTIPLIER = builder
            .comment("Global cooldown multiplier for all staves and artifacts (0.1 to 10.0). Default: 1.0")
            .defineInRange("cooldown_multiplier", 1.0D, 0.1D, 10.0D);

        ARC_CASTER_CHARGE_CREEPER = builder
            .comment("Whether Arc Caster lightning strikes turn creepers into charged creepers and trigger mob lightning conversions. Default: true")
            .define("arc_caster_charge_creeper", true);

        BASIC_MIRROR_CROSS_DIMENSION = builder
            .comment("Allow basic Magic Mirror to teleport across dimensions (Netherite tier always can). Default: false")
            .define("basic_mirror_cross_dimension", false);

        TEMPORARY_WEB_DURATION_TICKS = builder
            .comment("Base duration in ticks before temporary cobwebs placed by Webcaster vanish (20 ticks = 1s). Default: 100 (5s)")
            .defineInRange("temporary_web_duration_ticks", 100, 20, 1200);

        VOID_TRANSLOCATOR_RANGE_BASIC = builder
            .comment("Forward teleport blink distance in blocks for basic Void Translocator. Default: 24.0")
            .defineInRange("void_translocator_range_basic", 24.0D, 4.0D, 128.0D);

        VOID_TRANSLOCATOR_RANGE_NETHERITE = builder
            .comment("Forward teleport blink distance in blocks for Netherite Void Translocator. Default: 36.0")
            .defineInRange("void_translocator_range_netherite", 36.0D, 4.0D, 128.0D);

        VOID_TRANSLOCATOR_COOLDOWN_SECONDS = builder
            .comment("Cooldown in seconds for Void Translocator. Default: 5.0")
            .defineInRange("void_translocator_cooldown_seconds", 5.0D, 0.5D, 60.0D);

        MAGIC_MIRROR_COOLDOWN_SECONDS = builder
            .comment("Cooldown in seconds for Magic Mirror. Default: 15.0")
            .defineInRange("magic_mirror_cooldown_seconds", 15.0D, 1.0D, 300.0D);

        MAGIC_MIRROR_CHANNEL_SECONDS = builder
            .comment("Channeling time in seconds before Magic Mirror teleports player home. Default: 2.5")
            .defineInRange("magic_mirror_channel_seconds", 2.5D, 0.5D, 10.0D);

        builder.pop();

        builder.comment("Elemental Gauge Theory (EGT) Combat Mechanics").push("elemental_gauge");
        ENABLE_ELEMENTAL_GAUGE = builder
            .comment("Enable Genshin-style Elemental Gauge Theory (EGT) reactions, U-consumption, and decay. Default: true")
            .define("enable_elemental_gauge", true);
        ENABLE_ELEMENTAL_RESONANCE = builder
            .comment("Enable 8-type Genshin Elemental Resonance buffs & 4-Staff inventory limit. Default: true")
            .define("enable_elemental_resonance", true);
        MAX_STAVES_IN_INVENTORY = builder
            .comment("Maximum Arcane Staves carried directly in inventory before Arcane Overload triggers (0 = unlimited). Default: 4")
            .defineInRange("max_staves_in_inventory", 4, 0, 36);
        SHOW_ELEMENTAL_PARTICLES = builder
            .comment("Show ambient elemental particles hovering around affected entities. Default: true")
            .define("show_elemental_particles", true);
        SHOW_OVERHEAD_ELEMENT_ICONS = builder
            .comment("Render Genshin-style overhead elemental aura icons above afflicted mobs. Default: true")
            .define("show_overhead_element_icons", true);
        SHOW_REACTION_TEXT = builder
            .comment("Render floating elemental reaction titles (MELT, VAPORIZE, OVERLOADED, etc.). Default: true")
            .define("show_reaction_text", true);
        SHOW_DAMAGE_NUMBERS = builder
            .comment("Render floating elemental & physical damage numbers in combat. Default: true")
            .define("show_damage_numbers", true);
        INNATE_MUTATION_CHANCE = builder
            .comment("Base chance (0.0 to 1.0) for eligible mobs to spawn mutated with an innate element. Default: 0.07 (7%: 6% Elemental + 1% Omni Mutated)")
            .defineInRange("innate_mutation_chance", 0.07D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("Innate Elemental Mutation Categories, Whitelist/Blacklist & Per-Mob Rates").push("innate_mutation");
        ALLOW_HOSTILE_MOB_MUTATION = builder
            .comment("Allow standard hostile mobs (Enemy / MONSTER category, excluding Bosses) to mutate naturally. Default: true")
            .define("allow_hostile_mob_mutation", true);
        ALLOW_GOLEM_MUTATION = builder
            .comment("Allow Golems (AbstractGolem such as minecraft:iron_golem) without natural innate elements to mutate. Default: true")
            .define("allow_golem_mutation", true);
        ALLOW_NEUTRAL_AND_PASSIVE_MUTATION = builder
            .comment("Allow non-hostile neutral/passive mobs (excluding Bosses, Villagers/NPCs, and passive Aquatic/Ambient) to mutate unless whitelisted. Default: false")
            .define("allow_neutral_and_passive_mutation", false);
        OMNI_MUTATION_SHARE = builder
            .comment("Fraction (0.0 to 1.0) of successful mutations that become Omni Mutated (remaining become 1 of the 7 elements). Default: 0.143 (~1/7 -> 1% Omni at 7% rate)")
            .defineInRange("omni_mutation_share", 0.143D, 0.0D, 1.0D);
        MUTABLE_MOB_WHITELIST = builder
            .comment("Explicit entity IDs (e.g. \"minecraft:iron_golem\", \"minecraft:zombie\") or entity tags (\"#minecraft:raiders\") always allowed to mutate.")
            .defineListAllowEmpty("mutable_mob_whitelist", List.of("minecraft:iron_golem"), () -> "", o -> o instanceof String s && !s.trim().isEmpty());
        MUTABLE_MOB_BLACKLIST = builder
            .comment("Entity IDs or tags (\"#...\") excluded from innate mutation even if their category is enabled. Example: [\"minecraft:creeper\", \"minecraft:phantom\"]")
            .defineListAllowEmpty("mutable_mob_blacklist", List.of(), () -> "", o -> o instanceof String s && !s.trim().isEmpty());
        CUSTOM_MOB_MUTATION_RATES = builder
            .comment("Per-mob mutation chance overrides in \"entity_id=rate\" format (0.0 to 1.0). Example: [\"minecraft:iron_golem=0.15\", \"minecraft:creeper=0.20\"]")
            .defineListAllowEmpty("custom_mob_mutation_rates", List.of(), () -> "", o -> o instanceof String s && s.contains("="));
        builder.pop();

        builder.comment("Individual staff cooldowns in seconds").push("cooldowns");
        DRAGONFIRE_COOLDOWN = builder.defineInRange("dragonfire_cooldown", 8.0D, 0.5D, 60.0D);
        ECHO_COOLDOWN = builder.defineInRange("echo_cooldown", 8.0D, 0.5D, 60.0D);
        INFERNO_COOLDOWN = builder.defineInRange("inferno_cooldown", 4.0D, 0.5D, 60.0D);
        GHASTFIRE_COOLDOWN = builder.defineInRange("ghastfire_cooldown", 5.0D, 0.5D, 60.0D);
        WITHER_COOLDOWN = builder.defineInRange("wither_cooldown", 6.0D, 0.5D, 60.0D);
        SHULKER_COOLDOWN = builder.defineInRange("shulker_cooldown", 6.0D, 0.5D, 60.0D);
        GUARDIAN_COOLDOWN = builder.defineInRange("guardian_cooldown", 5.0D, 0.5D, 60.0D);
        EVOKER_COOLDOWN = builder.defineInRange("evoker_cooldown", 5.0D, 0.5D, 60.0D);
        ARC_CASTER_COOLDOWN = builder.defineInRange("arc_caster_cooldown", 6.0D, 0.5D, 60.0D);
        FROSTBOLT_COOLDOWN = builder.defineInRange("frostbolt_cooldown", 4.0D, 0.5D, 60.0D);
        WEBCASTER_COOLDOWN = builder.defineInRange("webcaster_cooldown", 5.0D, 0.5D, 60.0D);
        WIND_COOLDOWN = builder.defineInRange("wind_cooldown", 1.5D, 0.2D, 60.0D);
        OMNI_COOLDOWN = builder.defineInRange("omni_cooldown", 6.0D, 0.2D, 60.0D);
        builder.pop();

        builder.comment("Item maximum durability values").push("durability");
        DRAGONFIRE_DURABILITY_BASIC = builder.defineInRange("dragonfire_basic", 130, 10, 5000);
        DRAGONFIRE_DURABILITY_NETHERITE = builder.defineInRange("dragonfire_netherite", 500, 10, 5000);
        ECHO_DURABILITY_BASIC = builder.defineInRange("echo_basic", 130, 10, 5000);
        ECHO_DURABILITY_NETHERITE = builder.defineInRange("echo_netherite", 500, 10, 5000);
        INFERNO_DURABILITY_BASIC = builder.defineInRange("inferno_basic", 150, 10, 5000);
        INFERNO_DURABILITY_NETHERITE = builder.defineInRange("inferno_netherite", 550, 10, 5000);
        GHASTFIRE_DURABILITY_BASIC = builder.defineInRange("ghastfire_basic", 120, 10, 5000);
        GHASTFIRE_DURABILITY_NETHERITE = builder.defineInRange("ghastfire_netherite", 450, 10, 5000);
        WITHER_DURABILITY_BASIC = builder.defineInRange("wither_basic", 140, 10, 5000);
        WITHER_DURABILITY_NETHERITE = builder.defineInRange("wither_netherite", 500, 10, 5000);
        SHULKER_DURABILITY_BASIC = builder.defineInRange("shulker_basic", 130, 10, 5000);
        SHULKER_DURABILITY_NETHERITE = builder.defineInRange("shulker_netherite", 500, 10, 5000);
        GUARDIAN_DURABILITY_BASIC = builder.defineInRange("guardian_basic", 130, 10, 5000);
        GUARDIAN_DURABILITY_NETHERITE = builder.defineInRange("guardian_netherite", 500, 10, 5000);
        EVOKER_DURABILITY_BASIC = builder.defineInRange("evoker_basic", 120, 10, 5000);
        EVOKER_DURABILITY_NETHERITE = builder.defineInRange("evoker_netherite", 450, 10, 5000);
        ARC_CASTER_DURABILITY_BASIC = builder.defineInRange("arc_caster_basic", 130, 10, 5000);
        ARC_CASTER_DURABILITY_NETHERITE = builder.defineInRange("arc_caster_netherite", 500, 10, 5000);
        FROSTBOLT_DURABILITY_BASIC = builder.defineInRange("frostbolt_basic", 150, 10, 5000);
        FROSTBOLT_DURABILITY_NETHERITE = builder.defineInRange("frostbolt_netherite", 500, 10, 5000);
        WEBCASTER_DURABILITY_BASIC = builder.defineInRange("webcaster_basic", 140, 10, 5000);
        WEBCASTER_DURABILITY_NETHERITE = builder.defineInRange("webcaster_netherite", 500, 10, 5000);
        WIND_DURABILITY_BASIC = builder.defineInRange("wind_basic", 140, 10, 5000);
        WIND_DURABILITY_NETHERITE = builder.defineInRange("wind_netherite", 500, 10, 5000);
        OMNI_DURABILITY_BASIC = builder.defineInRange("omni_basic", 250, 10, 5000);
        OMNI_DURABILITY_NETHERITE = builder.defineInRange("omni_netherite", 900, 10, 5000);
        VOID_TRANSLOCATOR_DURABILITY_BASIC = builder.defineInRange("void_translocator_basic", 80, 10, 5000);
        VOID_TRANSLOCATOR_DURABILITY_NETHERITE = builder.defineInRange("void_translocator_netherite", 250, 10, 5000);
        MAGIC_MIRROR_DURABILITY_BASIC = builder.defineInRange("magic_mirror_basic", 40, 10, 5000);
        MAGIC_MIRROR_DURABILITY_NETHERITE = builder.defineInRange("magic_mirror_netherite", 150, 10, 5000);
        builder.pop();

        SPEC = builder.build();
    }

    private StavesConfig() {}

    public static void save() {
        save(true);
    }

    public static void save(boolean refreshSyncedSnapshot) {
        try {
            SPEC.save();
        } catch (Exception ignored) {}
        invalidateMutationRulesCache();
        if (refreshSyncedSnapshot && syncedServerData != null) {
            syncedServerData = createSnapshotFromLocal();
        }
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
                int maxStavesInInventory
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
            return new SyncedData(dmgMult, cdMult, vBasic, vNeth, vCd, mCd, mCh, cds, dBasic, dNeth, vdBasic, vdNeth, mdBasic, mdNeth, egtOn, mutChance, resOn, maxCap);
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
        for (int i = 0; i < kinds.length; i++) {
            cds[i] = getLocalStaffCooldownSeconds(kinds[i]);
            dBasic[i] = getLocalMaxDurability(kinds[i], false);
            dNeth[i] = getLocalMaxDurability(kinds[i], true);
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
                MAX_STAVES_IN_INVENTORY.get()
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
            case WITHER -> isNetherite ? WITHER_DURABILITY_NETHERITE.get() : WITHER_DURABILITY_BASIC.get();
            case SHULKER -> isNetherite ? SHULKER_DURABILITY_NETHERITE.get() : SHULKER_DURABILITY_BASIC.get();
            case GUARDIAN -> isNetherite ? GUARDIAN_DURABILITY_NETHERITE.get() : GUARDIAN_DURABILITY_BASIC.get();
            case EVOKER -> isNetherite ? EVOKER_DURABILITY_NETHERITE.get() : EVOKER_DURABILITY_BASIC.get();
            case LIGHTNING -> isNetherite ? ARC_CASTER_DURABILITY_NETHERITE.get() : ARC_CASTER_DURABILITY_BASIC.get();
            case FROST -> isNetherite ? FROSTBOLT_DURABILITY_NETHERITE.get() : FROSTBOLT_DURABILITY_BASIC.get();
            case WEB -> isNetherite ? WEBCASTER_DURABILITY_NETHERITE.get() : WEBCASTER_DURABILITY_BASIC.get();
            case WIND -> isNetherite ? WIND_DURABILITY_NETHERITE.get() : WIND_DURABILITY_BASIC.get();
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
            case WITHER -> WITHER_COOLDOWN.get();
            case SHULKER -> SHULKER_COOLDOWN.get();
            case GUARDIAN -> GUARDIAN_COOLDOWN.get();
            case EVOKER -> EVOKER_COOLDOWN.get();
            case LIGHTNING -> ARC_CASTER_COOLDOWN.get();
            case FROST -> FROSTBOLT_COOLDOWN.get();
            case WEB -> WEBCASTER_COOLDOWN.get();
            case WIND -> WIND_COOLDOWN.get();
            case OMNI -> OMNI_COOLDOWN.get();
        };
    }


    public static boolean isServerSynced() {
        return syncedServerData != null;
    }

    public static double getRangeMultiplier() {
        // NeoForge loader: range multiplier is currently always 1.0 (not a configurable field)
        return 1.0D;
    }

    public static int getBasicStaffDurability() {
        return getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, false);
    }

    public static int getNetheriteStaffDurability() {
        return getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, true);
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

    public static final class MutationRules {
        public final boolean allowHostile;
        public final boolean allowGolems;
        public final boolean allowNeutralAndPassive;
        public final double defaultChance;
        public final double omniShare;
        private final List<? extends String> whitelistRef;
        private final List<? extends String> blacklistRef;
        private final List<? extends String> customRatesRef;
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
                List<? extends String> whitelistRef,
                List<? extends String> blacklistRef,
                List<? extends String> customRatesRef
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
                List<? extends String> rawList,
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
                List<? extends String> wl, List<? extends String> bl, List<? extends String> cr
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
        List<? extends String> wl = MUTABLE_MOB_WHITELIST.get();
        List<? extends String> bl = MUTABLE_MOB_BLACKLIST.get();
        List<? extends String> cr = CUSTOM_MOB_MUTATION_RATES.get();
        MutationRules snap = cachedMutationRules;
        if (snap != null && snap.matches(ah, ag, anp, dc, os, wl, bl, cr)) {
            return snap;
        }
        MutationRules rebuilt = new MutationRules(ah, ag, anp, dc, os, wl, bl, cr);
        cachedMutationRules = rebuilt;
        return rebuilt;
    }

}
