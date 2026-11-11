package com.velorise.staves;

import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.ElementalCodexItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(VeloriseStavesMod.MODID)
public final class VeloriseStavesMod {
    public static final String MODID = "velorise_staves";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.GeoFangPillarEntity>> GEO_FANG_PILLAR =
            ENTITY_TYPES.register("geo_fang_pillar", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.GeoFangPillarEntity>of(com.velorise.staves.entity.GeoFangPillarEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1.0F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("geo_fang_pillar"));

    public static final Supplier<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.DendroCoreEntity>> DENDRO_CORE_ENTITY =
            ENTITY_TYPES.register("dendro_core", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.DendroCoreEntity>of(com.velorise.staves.entity.DendroCoreEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("dendro_core"));

    public static final Supplier<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.OmniOrbEntity>> OMNI_ORB =
            ENTITY_TYPES.register("omni_orb", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.OmniOrbEntity>of(com.velorise.staves.entity.OmniOrbEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("omni_orb"));

    // Sounds
    public static final Supplier<SoundEvent> MAGIC_MIRROR_USE_SOUND = SOUND_EVENTS.register("magic_mirror_use",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "magic_mirror_use")));

    // Basic Combat Staves
    public static final Supplier<Item> DRAGONFIRE_SCEPTER = ITEMS.register("dragonfire_scepter",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, 130, false));

    public static final Supplier<Item> RESONANT_ECHO = ITEMS.register("resonant_echo",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, 130, false));

    public static final Supplier<Item> INFERNO_ROD = ITEMS.register("inferno_rod",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, 150, false));

    public static final Supplier<Item> GHASTFIRE_STAFF = ITEMS.register("ghastfire_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, 120, false));

    public static final Supplier<Item> WITHER_SCEPTER = ITEMS.register("wither_scepter",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, 140, false));

    public static final Supplier<Item> SHULKER_SEEKER = ITEMS.register("shulker_seeker",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, 130, false));

    public static final Supplier<Item> GUARDIAN_LENS = ITEMS.register("guardian_lens",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, 130, false));

    public static final Supplier<Item> EVOKER_FANG_STAFF = ITEMS.register("evoker_fang_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 120, false));

    public static final Supplier<Item> ARC_CASTER = ITEMS.register("arc_caster",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, 130, false));

    public static final Supplier<Item> FROSTBOLT_WAND = ITEMS.register("frostbolt_wand",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, 150, false));

    public static final Supplier<Item> WEBCASTER = ITEMS.register("webcaster",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, 140, false));

    public static final Supplier<Item> WIND_STAFF = ITEMS.register("wind_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, 140, false));

    public static final Supplier<Item> OMNI_STAFF = ITEMS.register("omni_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.OMNI, 250, false));

    // Guidebook & Utility Artifacts
    public static final Supplier<Item> ELEMENTAL_CODEX = ITEMS.register("elemental_codex",
        ElementalCodexItem::new);
    public static final Supplier<Item> VOID_TRANSLOCATOR = ITEMS.register("void_translocator",
        () -> new VoidTranslocatorItem(80, 24.0D, false));

    public static final Supplier<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
        () -> new MagicMirrorItem(40, false));

    // Elemental Reaction Entities & Materials
    public static final Supplier<Item> DENDRO_CORE = ITEMS.register("dendro_core",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> PYRO_CRYSTALLIZE_SHARD = ITEMS.register("pyro_crystallize_shard",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HYDRO_CRYSTALLIZE_SHARD = ITEMS.register("hydro_crystallize_shard",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ELECTRO_CRYSTALLIZE_SHARD = ITEMS.register("electro_crystallize_shard",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CRYO_CRYSTALLIZE_SHARD = ITEMS.register("cryo_crystallize_shard",
        () -> new Item(new Item.Properties()));

    // Netherite Upgraded Combat Staves
    public static final Supplier<Item> NETHERITE_DRAGONFIRE_SCEPTER = ITEMS.register("netherite_dragonfire_scepter",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, 500, true));

    public static final Supplier<Item> NETHERITE_RESONANT_ECHO = ITEMS.register("netherite_resonant_echo",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, 500, true));

    public static final Supplier<Item> NETHERITE_INFERNO_ROD = ITEMS.register("netherite_inferno_rod",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, 550, true));

    public static final Supplier<Item> NETHERITE_GHASTFIRE_STAFF = ITEMS.register("netherite_ghastfire_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, 450, true));

    public static final Supplier<Item> NETHERITE_WITHER_SCEPTER = ITEMS.register("netherite_wither_scepter",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, 500, true));

    public static final Supplier<Item> NETHERITE_SHULKER_SEEKER = ITEMS.register("netherite_shulker_seeker",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, 500, true));

    public static final Supplier<Item> NETHERITE_GUARDIAN_LENS = ITEMS.register("netherite_guardian_lens",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, 500, true));

    public static final Supplier<Item> NETHERITE_EVOKER_FANG_STAFF = ITEMS.register("netherite_evoker_fang_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 450, true));

    public static final Supplier<Item> NETHERITE_ARC_CASTER = ITEMS.register("netherite_arc_caster",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, 500, true));

    public static final Supplier<Item> NETHERITE_FROSTBOLT_WAND = ITEMS.register("netherite_frostbolt_wand",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, 500, true));

    public static final Supplier<Item> NETHERITE_WEBCASTER = ITEMS.register("netherite_webcaster",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, 500, true));

    public static final Supplier<Item> NETHERITE_WIND_STAFF = ITEMS.register("netherite_wind_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, 500, true));

    public static final Supplier<Item> NETHERITE_OMNI_STAFF = ITEMS.register("netherite_omni_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.OMNI, 900, true));

    // Netherite Upgraded Utility Artifacts
    public static final Supplier<Item> NETHERITE_VOID_TRANSLOCATOR = ITEMS.register("netherite_void_translocator",
        () -> new VoidTranslocatorItem(250, 36.0D, true));

    public static final Supplier<Item> NETHERITE_MAGIC_MIRROR = ITEMS.register("netherite_magic_mirror",
        () -> new MagicMirrorItem(150, true));

    // Creative Tab
    public static final Supplier<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("velorise_staves_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.velorise_staves"))
            .icon(() -> new ItemStack(NETHERITE_DRAGONFIRE_SCEPTER.get()))
            .displayItems((parameters, output) -> {
                // Guidebook
                output.accept(ELEMENTAL_CODEX.get());

                // Basic tier
                output.accept(DRAGONFIRE_SCEPTER.get());
                output.accept(RESONANT_ECHO.get());
                output.accept(INFERNO_ROD.get());
                output.accept(GHASTFIRE_STAFF.get());
                output.accept(WITHER_SCEPTER.get());
                output.accept(SHULKER_SEEKER.get());
                output.accept(GUARDIAN_LENS.get());
                output.accept(EVOKER_FANG_STAFF.get());
                output.accept(ARC_CASTER.get());
                output.accept(FROSTBOLT_WAND.get());
                output.accept(WEBCASTER.get());
                output.accept(WIND_STAFF.get());
                output.accept(OMNI_STAFF.get());
                output.accept(VOID_TRANSLOCATOR.get());
                output.accept(MAGIC_MIRROR.get());

                // Netherite tier
                output.accept(NETHERITE_DRAGONFIRE_SCEPTER.get());
                output.accept(NETHERITE_RESONANT_ECHO.get());
                output.accept(NETHERITE_INFERNO_ROD.get());
                output.accept(NETHERITE_GHASTFIRE_STAFF.get());
                output.accept(NETHERITE_WITHER_SCEPTER.get());
                output.accept(NETHERITE_SHULKER_SEEKER.get());
                output.accept(NETHERITE_GUARDIAN_LENS.get());
                output.accept(NETHERITE_EVOKER_FANG_STAFF.get());
                output.accept(NETHERITE_ARC_CASTER.get());
                output.accept(NETHERITE_FROSTBOLT_WAND.get());
                output.accept(NETHERITE_WEBCASTER.get());
                output.accept(NETHERITE_WIND_STAFF.get());
                output.accept(NETHERITE_OMNI_STAFF.get());
                output.accept(NETHERITE_VOID_TRANSLOCATOR.get());
                output.accept(NETHERITE_MAGIC_MIRROR.get());

                // Reactions
                output.accept(DENDRO_CORE.get());

                // Elemental Mastery I-III Enchanted Books
                parameters.holders().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(MODID, "elemental_mastery")
                    )))
                    .ifPresent(holder -> {
                        for (int lvl = 1; lvl <= 3; lvl++) {
                            output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                                new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, lvl)
                            ));
                        }
                    });
            })
            .build());

    public VeloriseStavesMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, StavesConfig.SPEC, "velorise_staves.toml");
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modContainer.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (container, parent) -> new com.velorise.staves.client.VeloriseConfigScreen(parent)
            );
        }
        modEventBus.addListener(com.velorise.staves.network.ModNetwork::registerPayloads);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        com.velorise.staves.effect.ModMobEffects.MOB_EFFECTS.register(modEventBus);
    }
}
