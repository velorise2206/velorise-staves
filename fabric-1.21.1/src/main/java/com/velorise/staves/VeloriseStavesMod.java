package com.velorise.staves;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import com.velorise.staves.manager.TemporaryWebManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VeloriseStavesMod implements ModInitializer {
    public static final String MODID = "velorise_staves";

    static {
        StavesConfig.load();
    }

    // Sounds
    public static final SoundEvent MAGIC_MIRROR_USE_SOUND = registerSound("magic_mirror_use");

    // Basic Combat Staves
    public static final Item DRAGONFIRE_SCEPTER = registerItem("dragonfire_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, false), false));

    public static final Item RESONANT_ECHO = registerItem("resonant_echo",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.ECHO, false), false));

    public static final Item INFERNO_ROD = registerItem("inferno_rod",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.INFERNO, false), false));

    public static final Item GHASTFIRE_STAFF = registerItem("ghastfire_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.GHASTFIRE, false), false));

    public static final Item WITHER_SCEPTER = registerItem("wither_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WITHER, false), false));

    public static final Item SHULKER_SEEKER = registerItem("shulker_seeker",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.SHULKER, false), false));

    public static final Item GUARDIAN_LENS = registerItem("guardian_lens",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.GUARDIAN, false), false));

    public static final Item EVOKER_FANG_STAFF = registerItem("evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.EVOKER, false), false));

    public static final Item ARC_CASTER = registerItem("arc_caster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.LIGHTNING, false), false));

    public static final Item FROSTBOLT_WAND = registerItem("frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.FROST, false), false));

    public static final Item WEBCASTER = registerItem("webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WEB, false), false));

    public static final Item WIND_STAFF = registerItem("wind_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WIND, false), false));

    // Basic Utility Artifacts
    public static final Item VOID_TRANSLOCATOR = registerItem("void_translocator",
        new VoidTranslocatorItem(StavesConfig.getArtifactMaxDurability(true, false), 24.0D, false));

    public static final Item MAGIC_MIRROR = registerItem("magic_mirror",
        new MagicMirrorItem(StavesConfig.getArtifactMaxDurability(false, false), false));

    // Elemental Reaction Entities & Materials
    public static final Item DENDRO_CORE = registerItem("dendro_core",
        new Item(new Item.Properties()));
    public static final Item PYRO_CRYSTALLIZE_SHARD = registerItem("pyro_crystallize_shard",
        new Item(new Item.Properties()));
    public static final Item HYDRO_CRYSTALLIZE_SHARD = registerItem("hydro_crystallize_shard",
        new Item(new Item.Properties()));
    public static final Item ELECTRO_CRYSTALLIZE_SHARD = registerItem("electro_crystallize_shard",
        new Item(new Item.Properties()));
    public static final Item CRYO_CRYSTALLIZE_SHARD = registerItem("cryo_crystallize_shard",
        new Item(new Item.Properties()));

    // Netherite Upgraded Combat Staves
    public static final Item NETHERITE_DRAGONFIRE_SCEPTER = registerItem("netherite_dragonfire_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.DRAGONFIRE, true), true));

    public static final Item NETHERITE_RESONANT_ECHO = registerItem("netherite_resonant_echo",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.ECHO, true), true));

    public static final Item NETHERITE_INFERNO_ROD = registerItem("netherite_inferno_rod",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.INFERNO, true), true));

    public static final Item NETHERITE_GHASTFIRE_STAFF = registerItem("netherite_ghastfire_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.GHASTFIRE, true), true));

    public static final Item NETHERITE_WITHER_SCEPTER = registerItem("netherite_wither_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WITHER, true), true));

    public static final Item NETHERITE_SHULKER_SEEKER = registerItem("netherite_shulker_seeker",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.SHULKER, true), true));

    public static final Item NETHERITE_GUARDIAN_LENS = registerItem("netherite_guardian_lens",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.GUARDIAN, true), true));

    public static final Item NETHERITE_EVOKER_FANG_STAFF = registerItem("netherite_evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.EVOKER, true), true));

    public static final Item NETHERITE_ARC_CASTER = registerItem("netherite_arc_caster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.LIGHTNING, true), true));

    public static final Item NETHERITE_FROSTBOLT_WAND = registerItem("netherite_frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.FROST, true), true));

    public static final Item NETHERITE_WEBCASTER = registerItem("netherite_webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WEB, true), true));

    public static final Item NETHERITE_WIND_STAFF = registerItem("netherite_wind_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WIND, true), true));

    // Netherite Upgraded Utility Artifacts
    public static final Item NETHERITE_VOID_TRANSLOCATOR = registerItem("netherite_void_translocator",
        new VoidTranslocatorItem(StavesConfig.getArtifactMaxDurability(true, true), 36.0D, true));

    public static final Item NETHERITE_MAGIC_MIRROR = registerItem("netherite_magic_mirror",
        new MagicMirrorItem(StavesConfig.getArtifactMaxDurability(false, true), true));

    // Creative Tab
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(MODID, "velorise_staves_tab")
    );

    public static final CreativeModeTab TAB = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.velorise_staves"))
        .icon(() -> new ItemStack(NETHERITE_DRAGONFIRE_SCEPTER))
        .displayItems((parameters, output) -> {
            // Basic tier
            output.accept(DRAGONFIRE_SCEPTER);
            output.accept(RESONANT_ECHO);
            output.accept(INFERNO_ROD);
            output.accept(GHASTFIRE_STAFF);
            output.accept(WITHER_SCEPTER);
            output.accept(SHULKER_SEEKER);
            output.accept(GUARDIAN_LENS);
            output.accept(EVOKER_FANG_STAFF);
            output.accept(ARC_CASTER);
            output.accept(FROSTBOLT_WAND);
            output.accept(WEBCASTER);
            output.accept(WIND_STAFF);
            output.accept(VOID_TRANSLOCATOR);
            output.accept(MAGIC_MIRROR);

            // Netherite tier
            output.accept(NETHERITE_DRAGONFIRE_SCEPTER);
            output.accept(NETHERITE_RESONANT_ECHO);
            output.accept(NETHERITE_INFERNO_ROD);
            output.accept(NETHERITE_GHASTFIRE_STAFF);
            output.accept(NETHERITE_WITHER_SCEPTER);
            output.accept(NETHERITE_SHULKER_SEEKER);
            output.accept(NETHERITE_GUARDIAN_LENS);
            output.accept(NETHERITE_EVOKER_FANG_STAFF);
            output.accept(NETHERITE_ARC_CASTER);
            output.accept(NETHERITE_FROSTBOLT_WAND);
            output.accept(NETHERITE_WEBCASTER);
            output.accept(NETHERITE_WIND_STAFF);
            output.accept(NETHERITE_VOID_TRANSLOCATOR);
            output.accept(NETHERITE_MAGIC_MIRROR);

            // Reactions
            output.accept(DENDRO_CORE);
        })
        .build();

    @Override
    public void onInitialize() {
        StavesConfig.load();
        com.velorise.staves.network.ModNetwork.registerCommon();
        com.velorise.staves.effect.ModMobEffects.init();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);

        // Also add combat staves to vanilla Combat tab so players never miss them
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(DRAGONFIRE_SCEPTER);
            entries.accept(RESONANT_ECHO);
            entries.accept(INFERNO_ROD);
            entries.accept(GHASTFIRE_STAFF);
            entries.accept(WITHER_SCEPTER);
            entries.accept(SHULKER_SEEKER);
            entries.accept(GUARDIAN_LENS);
            entries.accept(EVOKER_FANG_STAFF);
            entries.accept(ARC_CASTER);
            entries.accept(FROSTBOLT_WAND);
            entries.accept(WEBCASTER);
            entries.accept(WIND_STAFF);
            entries.accept(NETHERITE_DRAGONFIRE_SCEPTER);
            entries.accept(NETHERITE_RESONANT_ECHO);
            entries.accept(NETHERITE_INFERNO_ROD);
            entries.accept(NETHERITE_GHASTFIRE_STAFF);
            entries.accept(NETHERITE_WITHER_SCEPTER);
            entries.accept(NETHERITE_SHULKER_SEEKER);
            entries.accept(NETHERITE_GUARDIAN_LENS);
            entries.accept(NETHERITE_EVOKER_FANG_STAFF);
            entries.accept(NETHERITE_ARC_CASTER);
            entries.accept(NETHERITE_FROSTBOLT_WAND);
            entries.accept(NETHERITE_WEBCASTER);
            entries.accept(NETHERITE_WIND_STAFF);
        });

        // Also add artifacts to vanilla Tools & Utilities tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(VOID_TRANSLOCATOR);
            entries.accept(MAGIC_MIRROR);
            entries.accept(NETHERITE_VOID_TRANSLOCATOR);
            entries.accept(NETHERITE_MAGIC_MIRROR);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TemporaryWebManager.tick(server);
            ElementalGaugeManager.tick(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            TemporaryWebManager.clearAll(server);
            ElementalGaugeManager.clearAll();
            ArcaneStaffItem.clearAll();
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs
                    && fangs.getTags().contains("velorise_staves:geo_fangs")) {
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    boolean isNetherite = fangs.getTags().contains("velorise_staves:netherite_geo_fangs");
                    float baseDmg = isNetherite ? 4.5F : 3.0F;
                    float attackU = isNetherite ? 1.5F : 1.0F;
                    net.minecraft.server.level.ServerPlayer attacker = source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null;
                    ElementalGaugeManager.applyElement(sl, entity, com.velorise.staves.element.Element.GEO, attackU, attacker, baseDmg);
                }
                return false;
            }
            return true;
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living && !world.isClientSide()) {
                ElementalGaugeManager.checkInnateMutation(living);
            }
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, damage, afterDamage, blocked) -> {
            if (!blocked && !entity.level().isClientSide()) {
                ElementalGaugeManager.handleLivingIncomingDamage(entity, source);
            }
        });
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living && !entity.level().isClientSide()) {
                java.util.Collection<com.velorise.staves.element.Element> elements = ElementalGaugeManager.getEntityElements(living.getUUID());
                if (!elements.isEmpty()) {
                    int duration = ElementalGaugeManager.getEntityMaxDurationTicks(living.getUUID());
                    com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, living, elements, duration);
                }
            }
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            java.util.Collection<com.velorise.staves.element.Element> elements = ElementalGaugeManager.getEntityElements(player.getUUID());
            if (!elements.isEmpty()) {
                int duration = ElementalGaugeManager.getEntityMaxDurationTicks(player.getUUID());
                com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, player, elements, duration);
            }
        });
    }

    private static Item registerItem(String path, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, path), item);
    }

    private static SoundEvent registerSound(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
