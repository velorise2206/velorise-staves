package com.velorise.staves;

import com.velorise.staves.advancement.VeloriseCriterionTrigger;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.ElementItem;
import com.velorise.staves.item.ElementalCodexItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import com.velorise.staves.item.WindChargeItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import com.velorise.staves.config.StavesConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Mod(VeloriseStavesMod.MODID)
public final class VeloriseStavesMod {
    public static final String MODID = "velorise_staves";

    public static final VeloriseCriterionTrigger ADVANCEMENT_TRIGGER =
            CriteriaTriggers.register(new VeloriseCriterionTrigger());

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.GeoFangPillarEntity>> GEO_FANG_PILLAR =
            ENTITY_TYPES.register("geo_fang_pillar", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.GeoFangPillarEntity>of(com.velorise.staves.entity.GeoFangPillarEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("geo_fang_pillar"));

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.DendroCoreEntity>> DENDRO_CORE_ENTITY =
            ENTITY_TYPES.register("dendro_core", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.DendroCoreEntity>of(com.velorise.staves.entity.DendroCoreEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("dendro_core"));

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.OmniOrbEntity>> OMNI_ORB =
            ENTITY_TYPES.register("omni_orb", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.OmniOrbEntity>of(com.velorise.staves.entity.OmniOrbEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("omni_orb"));

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.HydroSalonSummonEntity>> HYDRO_SALON_SUMMON =
            ENTITY_TYPES.register("hydro_salon_summon", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.HydroSalonSummonEntity>of(com.velorise.staves.entity.HydroSalonSummonEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.75F, 0.95F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("hydro_salon_summon"));

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<com.velorise.staves.entity.WindChargeEntity>> WIND_CHARGE_ENTITY =
            ENTITY_TYPES.register("wind_charge", () -> net.minecraft.world.entity.EntityType.Builder
                    .<com.velorise.staves.entity.WindChargeEntity>of(com.velorise.staves.entity.WindChargeEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.3125F, 0.3125F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("wind_charge"));

    public static final DeferredRegister<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    // Enchantments
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.enchantment.Enchantment> ELEMENTAL_MASTERY =
        ENCHANTMENTS.register("elemental_mastery", com.velorise.staves.enchantment.ElementalMasteryEnchantment::new);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.enchantment.Enchantment> ELEMENTAL_PROTECTION =
        ENCHANTMENTS.register("elemental_protection", com.velorise.staves.enchantment.ElementalProtectionEnchantment::new);

    // Sounds
    public static final Supplier<SoundEvent> MAGIC_MIRROR_USE_SOUND = SOUND_EVENTS.register("magic_mirror_use",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "magic_mirror_use")));
    public static final Supplier<SoundEvent> WIND_CHARGE_THROW_SOUND = SOUND_EVENTS.register("wind_charge_throw",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "wind_charge_throw")));
    public static final Supplier<SoundEvent> WIND_CHARGE_BURST_SOUND = SOUND_EVENTS.register("wind_charge_burst",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "wind_charge_burst")));

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

    public static final Supplier<Item> SPLENDOR_OF_TRANQUIL_WATERS = ITEMS.register("splendor_of_tranquil_waters",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.SPLENDOR, 140, false));

    public static final Supplier<Item> EVOKER_FANG_STAFF = ITEMS.register("evoker_fang_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 120, false));

    public static final Supplier<Item> VORTEX_VANQUISHER = ITEMS.register("vortex_vanquisher",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.VORTEX, 140, false));

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

    // 1.20.1 Wind Backport Materials
    public static final Supplier<Item> BREEZE_ROD = ITEMS.register("breeze_rod",
        () -> new Item(new Item.Properties()));
    public static final Supplier<Item> WIND_CHARGE = ITEMS.register("wind_charge",
        WindChargeItem::new);

    // Elemental Essences
    public static final Supplier<Item> PYRO_ELEMENT = ITEMS.register("pyro_element",
        () -> new ElementItem(Element.PYRO));
    public static final Supplier<Item> HYDRO_ELEMENT = ITEMS.register("hydro_element",
        () -> new ElementItem(Element.HYDRO));
    public static final Supplier<Item> ANEMO_ELEMENT = ITEMS.register("anemo_element",
        () -> new ElementItem(Element.ANEMO));
    public static final Supplier<Item> ELECTRO_ELEMENT = ITEMS.register("electro_element",
        () -> new ElementItem(Element.ELECTRO));
    public static final Supplier<Item> DENDRO_ELEMENT = ITEMS.register("dendro_element",
        () -> new ElementItem(Element.DENDRO));
    public static final Supplier<Item> CRYO_ELEMENT = ITEMS.register("cryo_element",
        () -> new ElementItem(Element.CRYO));
    public static final Supplier<Item> GEO_ELEMENT = ITEMS.register("geo_element",
        () -> new ElementItem(Element.GEO));
    public static final Supplier<Item> OMNI_ELEMENT = ITEMS.register("omni_element",
        () -> new ElementItem(null));

    public static Item getElementItem(@Nullable Element element) {
        if (element == null) return OMNI_ELEMENT.get();
        return switch (element) {
            case PYRO -> PYRO_ELEMENT.get();
            case HYDRO -> HYDRO_ELEMENT.get();
            case ANEMO -> ANEMO_ELEMENT.get();
            case ELECTRO -> ELECTRO_ELEMENT.get();
            case DENDRO -> DENDRO_ELEMENT.get();
            case CRYO -> CRYO_ELEMENT.get();
            case GEO -> GEO_ELEMENT.get();
        };
    }

    public static @Nullable Element elementFromItem(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ElementItem elemItem) {
            return elemItem.getElement();
        }
        return null;
    }

    public static boolean isOmniElementItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ElementItem elemItem && elemItem.isOmni();
    }

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

    public static final Supplier<Item> NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS = ITEMS.register("netherite_splendor_of_tranquil_waters",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.SPLENDOR, 500, true));

    public static final Supplier<Item> NETHERITE_EVOKER_FANG_STAFF = ITEMS.register("netherite_evoker_fang_staff",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 450, true));

    public static final Supplier<Item> NETHERITE_VORTEX_VANQUISHER = ITEMS.register("netherite_vortex_vanquisher",
        () -> new ArcaneStaffItem(ArcaneStaffItem.Kind.VORTEX, 500, true));

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
                output.accept(SPLENDOR_OF_TRANQUIL_WATERS.get());
                output.accept(EVOKER_FANG_STAFF.get());
                output.accept(VORTEX_VANQUISHER.get());
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
                output.accept(NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS.get());
                output.accept(NETHERITE_EVOKER_FANG_STAFF.get());
                output.accept(NETHERITE_VORTEX_VANQUISHER.get());
                output.accept(NETHERITE_ARC_CASTER.get());
                output.accept(NETHERITE_FROSTBOLT_WAND.get());
                output.accept(NETHERITE_WEBCASTER.get());
                output.accept(NETHERITE_WIND_STAFF.get());
                output.accept(NETHERITE_OMNI_STAFF.get());
                output.accept(NETHERITE_VOID_TRANSLOCATOR.get());
                output.accept(NETHERITE_MAGIC_MIRROR.get());

                // Reactions & Materials
                output.accept(DENDRO_CORE.get());
                output.accept(BREEZE_ROD.get());
                output.accept(WIND_CHARGE.get());
                output.accept(PYRO_ELEMENT.get());
                output.accept(HYDRO_ELEMENT.get());
                output.accept(ANEMO_ELEMENT.get());
                output.accept(ELECTRO_ELEMENT.get());
                output.accept(DENDRO_ELEMENT.get());
                output.accept(CRYO_ELEMENT.get());
                output.accept(GEO_ELEMENT.get());
                output.accept(OMNI_ELEMENT.get());

                // Elemental Mastery I-III Enchanted Books
                for (int lvl = 1; lvl <= 3; lvl++) {
                    output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(ELEMENTAL_MASTERY.get(), lvl)
                    ));
                }

                // Elemental Protection I-IV Enchanted Books
                for (int lvl = 1; lvl <= 4; lvl++) {
                    output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(ELEMENTAL_PROTECTION.get(), lvl)
                    ));
                }
            })
            .build());

    public VeloriseStavesMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StavesConfig.SPEC, "velorise_staves.toml");
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new com.velorise.staves.client.VeloriseConfigScreen(parent)
                )
            );
        });
        com.velorise.staves.network.ModNetwork.register();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(VeloriseStavesMod::onBuildCreativeTabContents);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENCHANTMENTS.register(modEventBus);
        com.velorise.staves.effect.ModMobEffects.MOB_EFFECTS.register(modEventBus);
    }

    private static void onBuildCreativeTabContents(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        var tabKey = event.getTabKey();
        if (tabKey == net.minecraft.world.item.CreativeModeTabs.COMBAT) {
            event.accept(DRAGONFIRE_SCEPTER.get());
            event.accept(RESONANT_ECHO.get());
            event.accept(INFERNO_ROD.get());
            event.accept(GHASTFIRE_STAFF.get());
            event.accept(WITHER_SCEPTER.get());
            event.accept(SHULKER_SEEKER.get());
            event.accept(GUARDIAN_LENS.get());
            event.accept(SPLENDOR_OF_TRANQUIL_WATERS.get());
            event.accept(EVOKER_FANG_STAFF.get());
            event.accept(VORTEX_VANQUISHER.get());
            event.accept(ARC_CASTER.get());
            event.accept(FROSTBOLT_WAND.get());
            event.accept(WEBCASTER.get());
            event.accept(WIND_STAFF.get());
            event.accept(OMNI_STAFF.get());
            event.accept(NETHERITE_DRAGONFIRE_SCEPTER.get());
            event.accept(NETHERITE_RESONANT_ECHO.get());
            event.accept(NETHERITE_INFERNO_ROD.get());
            event.accept(NETHERITE_GHASTFIRE_STAFF.get());
            event.accept(NETHERITE_WITHER_SCEPTER.get());
            event.accept(NETHERITE_SHULKER_SEEKER.get());
            event.accept(NETHERITE_GUARDIAN_LENS.get());
            event.accept(NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS.get());
            event.accept(NETHERITE_EVOKER_FANG_STAFF.get());
            event.accept(NETHERITE_VORTEX_VANQUISHER.get());
            event.accept(NETHERITE_ARC_CASTER.get());
            event.accept(NETHERITE_FROSTBOLT_WAND.get());
            event.accept(NETHERITE_WEBCASTER.get());
            event.accept(NETHERITE_WIND_STAFF.get());
            event.accept(NETHERITE_OMNI_STAFF.get());
            event.accept(WIND_CHARGE.get());
        } else if (tabKey == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ELEMENTAL_CODEX.get());
            event.accept(VOID_TRANSLOCATOR.get());
            event.accept(MAGIC_MIRROR.get());
            event.accept(NETHERITE_VOID_TRANSLOCATOR.get());
            event.accept(NETHERITE_MAGIC_MIRROR.get());
        } else if (tabKey == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) {
            event.accept(BREEZE_ROD.get());
            event.accept(WIND_CHARGE.get());
            event.accept(PYRO_ELEMENT.get());
            event.accept(HYDRO_ELEMENT.get());
            event.accept(ANEMO_ELEMENT.get());
            event.accept(ELECTRO_ELEMENT.get());
            event.accept(DENDRO_ELEMENT.get());
            event.accept(CRYO_ELEMENT.get());
            event.accept(GEO_ELEMENT.get());
            event.accept(OMNI_ELEMENT.get());
        }
    }
}
