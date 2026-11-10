package com.velorise.staves.config;

import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraftforge.common.ForgeConfigSpec;

public final class StavesConfig {
    public static final ForgeConfigSpec SPEC;

    // Mechanics
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ARC_CASTER_CHARGE_CREEPER;
    public static final ForgeConfigSpec.BooleanValue BASIC_MIRROR_CROSS_DIMENSION;
    public static final ForgeConfigSpec.IntValue TEMPORARY_WEB_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue VOID_TRANSLOCATOR_RANGE_BASIC;
    public static final ForgeConfigSpec.DoubleValue VOID_TRANSLOCATOR_RANGE_NETHERITE;
    public static final ForgeConfigSpec.DoubleValue VOID_TRANSLOCATOR_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MAGIC_MIRROR_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MAGIC_MIRROR_CHANNEL_SECONDS;

    // Elemental Gauge Theory (EGT)
    public static final ForgeConfigSpec.BooleanValue ENABLE_ELEMENTAL_GAUGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_ELEMENTAL_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue SHOW_OVERHEAD_ELEMENT_ICONS;
    public static final ForgeConfigSpec.BooleanValue SHOW_REACTION_TEXT;
    public static final ForgeConfigSpec.BooleanValue SHOW_DAMAGE_NUMBERS;
    public static final ForgeConfigSpec.DoubleValue INNATE_MUTATION_CHANCE;

    // Cooldowns
    public static final ForgeConfigSpec.DoubleValue DRAGONFIRE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ECHO_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue INFERNO_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue GHASTFIRE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WITHER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue SHULKER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue GUARDIAN_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue EVOKER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ARC_CASTER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue FROSTBOLT_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WEBCASTER_COOLDOWN;

    // Durabilities
    public static final ForgeConfigSpec.IntValue DRAGONFIRE_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue DRAGONFIRE_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue ECHO_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue ECHO_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue INFERNO_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue INFERNO_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue GHASTFIRE_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue GHASTFIRE_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue WITHER_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue WITHER_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue SHULKER_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue SHULKER_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue GUARDIAN_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue GUARDIAN_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue EVOKER_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue EVOKER_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue ARC_CASTER_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue ARC_CASTER_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue FROSTBOLT_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue FROSTBOLT_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue WEBCASTER_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue WEBCASTER_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue VOID_TRANSLOCATOR_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue VOID_TRANSLOCATOR_DURABILITY_NETHERITE;
    public static final ForgeConfigSpec.IntValue MAGIC_MIRROR_DURABILITY_BASIC;
    public static final ForgeConfigSpec.IntValue MAGIC_MIRROR_DURABILITY_NETHERITE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
            .comment("Render floating elemental damage numbers in combat. Default: true")
            .define("show_damage_numbers", true);
        INNATE_MUTATION_CHANCE = builder
            .comment("Chance (0.0 to 1.0) for normal mobs to spawn mutated with an innate element. Default: 0.06 (6%)")
            .defineInRange("innate_mutation_chance", 0.06D, 0.0D, 1.0D);
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
        VOID_TRANSLOCATOR_DURABILITY_BASIC = builder.defineInRange("void_translocator_basic", 80, 10, 5000);
        VOID_TRANSLOCATOR_DURABILITY_NETHERITE = builder.defineInRange("void_translocator_netherite", 250, 10, 5000);
        MAGIC_MIRROR_DURABILITY_BASIC = builder.defineInRange("magic_mirror_basic", 40, 10, 5000);
        MAGIC_MIRROR_DURABILITY_NETHERITE = builder.defineInRange("magic_mirror_netherite", 150, 10, 5000);
        builder.pop();

        SPEC = builder.build();
    }

    private StavesConfig() {}

    public static void save() {
        try {
            SPEC.save();
        } catch (Exception ignored) {}
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
                int mirrorDurabilityNetherite
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
            return new SyncedData(dmgMult, cdMult, vBasic, vNeth, vCd, mCd, mCh, cds, dBasic, dNeth, vdBasic, vdNeth, mdBasic, mdNeth);
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
            dBasic[i] = getMaxDurability(kinds[i], false);
            dNeth[i] = getMaxDurability(kinds[i], true);
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
                MAGIC_MIRROR_DURABILITY_NETHERITE.get()
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
        return getMaxDurability(kind, isNetherite);
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
        };
    }


    // --- Compatibility helpers for ElementalCodexScreen ---

    public static boolean isServerSynced() {
        return false; // Forge uses ForgeConfigSpec; server-sync is not supported
    }

    public static int getBasicStaffDurability() {
        return DRAGONFIRE_DURABILITY_BASIC.get();
    }

    public static int getNetheriteStaffDurability() {
        return DRAGONFIRE_DURABILITY_NETHERITE.get();
    }

    public static double getRangeMultiplier() {
        return 1.0;
    }


    public static boolean isElementalGaugeEnabled() {
        return ENABLE_ELEMENTAL_GAUGE.get();
    }

}
