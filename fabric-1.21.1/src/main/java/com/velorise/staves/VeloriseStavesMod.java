package com.velorise.staves;

import com.velorise.staves.advancement.VeloriseCriterionTrigger;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.ElementItem;
import com.velorise.staves.item.ElementalCodexItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import com.velorise.staves.manager.TemporaryWebManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import org.jetbrains.annotations.Nullable;

public final class VeloriseStavesMod implements ModInitializer {
    public static final String MODID = "velorise_staves";

    static {
        StavesConfig.load();
    }

    public static final VeloriseCriterionTrigger ADVANCEMENT_TRIGGER = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            ResourceLocation.fromNamespaceAndPath(MODID, "advancement_event"),
            new VeloriseCriterionTrigger()
    );

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

    public static final Item SPLENDOR_OF_TRANQUIL_WATERS = registerItem("splendor_of_tranquil_waters",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SPLENDOR, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.SPLENDOR, false), false));

    public static final Item EVOKER_FANG_STAFF = registerItem("evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.EVOKER, false), false));

    public static final Item VORTEX_VANQUISHER = registerItem("vortex_vanquisher",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.VORTEX, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.VORTEX, false), false));

    public static final Item ARC_CASTER = registerItem("arc_caster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.LIGHTNING, false), false));

    public static final Item FROSTBOLT_WAND = registerItem("frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.FROST, false), false));

    public static final Item NIGHTS_FOCUS_STAFF = registerItem("nights_focus_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.NIGHTS_FOCUS, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.NIGHTS_FOCUS, false), false));

    public static final Item FROST_ROD = registerItem("frost_rod",
        new Item(new Item.Properties()));

    public static final Item WEBCASTER = registerItem("webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WEB, false), false));

    public static final Item WIND_STAFF = registerItem("wind_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WIND, false), false));

    public static final Item OMNI_STAFF = registerItem("omni_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.OMNI, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.OMNI, false), false));

    // Guidebook & Utility Artifacts
    public static final Item ELEMENTAL_CODEX = registerItem("elemental_codex",
        new ElementalCodexItem());
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

    // Elemental Essences
    public static final Item PYRO_ELEMENT = registerItem("pyro_element", new ElementItem(Element.PYRO));
    public static final Item HYDRO_ELEMENT = registerItem("hydro_element", new ElementItem(Element.HYDRO));
    public static final Item ANEMO_ELEMENT = registerItem("anemo_element", new ElementItem(Element.ANEMO));
    public static final Item ELECTRO_ELEMENT = registerItem("electro_element", new ElementItem(Element.ELECTRO));
    public static final Item DENDRO_ELEMENT = registerItem("dendro_element", new ElementItem(Element.DENDRO));
    public static final Item CRYO_ELEMENT = registerItem("cryo_element", new ElementItem(Element.CRYO));
    public static final Item GEO_ELEMENT = registerItem("geo_element", new ElementItem(Element.GEO));
    public static final Item OMNI_ELEMENT = registerItem("omni_element", new ElementItem(null));

    public static Item getElementItem(@Nullable Element element) {
        if (element == null) return OMNI_ELEMENT;
        return switch (element) {
            case PYRO -> PYRO_ELEMENT;
            case HYDRO -> HYDRO_ELEMENT;
            case ANEMO -> ANEMO_ELEMENT;
            case ELECTRO -> ELECTRO_ELEMENT;
            case DENDRO -> DENDRO_ELEMENT;
            case CRYO -> CRYO_ELEMENT;
            case GEO -> GEO_ELEMENT;
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

    public static final Item NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS = registerItem("netherite_splendor_of_tranquil_waters",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SPLENDOR, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.SPLENDOR, true), true));

    public static final Item NETHERITE_EVOKER_FANG_STAFF = registerItem("netherite_evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.EVOKER, true), true));

    public static final Item NETHERITE_VORTEX_VANQUISHER = registerItem("netherite_vortex_vanquisher",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.VORTEX, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.VORTEX, true), true));

    public static final Item NETHERITE_ARC_CASTER = registerItem("netherite_arc_caster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.LIGHTNING, true), true));

    public static final Item NETHERITE_FROSTBOLT_WAND = registerItem("netherite_frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.FROST, true), true));

    public static final Item NETHERITE_NIGHTS_FOCUS_STAFF = registerItem("netherite_nights_focus_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.NIGHTS_FOCUS, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.NIGHTS_FOCUS, true), true));

    public static final Item NETHERITE_WEBCASTER = registerItem("netherite_webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WEB, true), true));

    public static final Item NETHERITE_WIND_STAFF = registerItem("netherite_wind_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WIND, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.WIND, true), true));

    public static final Item NETHERITE_OMNI_STAFF = registerItem("netherite_omni_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.OMNI, StavesConfig.getMaxDurability(ArcaneStaffItem.Kind.OMNI, true), true));

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
            // Guidebook
            output.accept(ELEMENTAL_CODEX);

            // Basic tier
            output.accept(DRAGONFIRE_SCEPTER);
            output.accept(RESONANT_ECHO);
            output.accept(INFERNO_ROD);
            output.accept(GHASTFIRE_STAFF);
            output.accept(WITHER_SCEPTER);
            output.accept(SHULKER_SEEKER);
            output.accept(GUARDIAN_LENS);
            output.accept(SPLENDOR_OF_TRANQUIL_WATERS);
            output.accept(EVOKER_FANG_STAFF);
            output.accept(VORTEX_VANQUISHER);
            output.accept(ARC_CASTER);
            output.accept(FROSTBOLT_WAND);
            output.accept(NIGHTS_FOCUS_STAFF);
            output.accept(WEBCASTER);
            output.accept(WIND_STAFF);
            output.accept(OMNI_STAFF);
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
            output.accept(NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS);
            output.accept(NETHERITE_EVOKER_FANG_STAFF);
            output.accept(NETHERITE_VORTEX_VANQUISHER);
            output.accept(NETHERITE_ARC_CASTER);
            output.accept(NETHERITE_FROSTBOLT_WAND);
            output.accept(NETHERITE_NIGHTS_FOCUS_STAFF);
            output.accept(NETHERITE_WEBCASTER);
            output.accept(NETHERITE_WIND_STAFF);
            output.accept(NETHERITE_OMNI_STAFF);
            output.accept(NETHERITE_VOID_TRANSLOCATOR);
            output.accept(NETHERITE_MAGIC_MIRROR);

            // Reactions & Elements
            output.accept(DENDRO_CORE);
            output.accept(PYRO_ELEMENT);
            output.accept(HYDRO_ELEMENT);
            output.accept(ANEMO_ELEMENT);
            output.accept(ELECTRO_ELEMENT);
            output.accept(DENDRO_ELEMENT);
            output.accept(CRYO_ELEMENT);
            output.accept(GEO_ELEMENT);
            output.accept(OMNI_ELEMENT);
            output.accept(FROST_ROD);

            // Elemental Mastery I-III Enchanted Books
            parameters.holders().lookup(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(MODID, "elemental_mastery")
                )))
                .ifPresent(holder -> {
                    for (int lvl = 1; lvl <= 3; lvl++) {
                        output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                            new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, lvl)
                        ));
                    }
                });

            // Elemental Protection I-IV Enchanted Books
            parameters.holders().lookup(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(MODID, "elemental_protection")
                )))
                .ifPresent(holder -> {
                    for (int lvl = 1; lvl <= 4; lvl++) {
                        output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                            new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, lvl)
                        ));
                    }
                });
        })
        .build();

    public static final net.minecraft.world.entity.EntityType<com.velorise.staves.entity.GeoFangPillarEntity> GEO_FANG_PILLAR =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MODID, "geo_fang_pillar"),
                    net.minecraft.world.entity.EntityType.Builder
                            .<com.velorise.staves.entity.GeoFangPillarEntity>of(
                                    com.velorise.staves.entity.GeoFangPillarEntity::new,
                                    net.minecraft.world.entity.MobCategory.MISC
                            )
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("geo_fang_pillar")
            );

    public static final net.minecraft.world.entity.EntityType<com.velorise.staves.entity.DendroCoreEntity> DENDRO_CORE_ENTITY =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MODID, "dendro_core"),
                    net.minecraft.world.entity.EntityType.Builder
                            .<com.velorise.staves.entity.DendroCoreEntity>of(
                                    com.velorise.staves.entity.DendroCoreEntity::new,
                                    net.minecraft.world.entity.MobCategory.MISC
                            )
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("dendro_core")
            );

    public static final net.minecraft.world.entity.EntityType<com.velorise.staves.entity.OmniOrbEntity> OMNI_ORB =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MODID, "omni_orb"),
                    net.minecraft.world.entity.EntityType.Builder
                            .<com.velorise.staves.entity.OmniOrbEntity>of(
                                    com.velorise.staves.entity.OmniOrbEntity::new,
                                    net.minecraft.world.entity.MobCategory.MISC
                            )
                            .sized(0.45F, 0.45F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("omni_orb")
            );

    public static final net.minecraft.world.entity.EntityType<com.velorise.staves.entity.HydroSalonSummonEntity> HYDRO_SALON_SUMMON =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MODID, "hydro_salon_summon"),
                    net.minecraft.world.entity.EntityType.Builder
                            .<com.velorise.staves.entity.HydroSalonSummonEntity>of(
                                    com.velorise.staves.entity.HydroSalonSummonEntity::new,
                                    net.minecraft.world.entity.MobCategory.MISC
                            )
                            .sized(0.75F, 0.95F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("hydro_salon_summon")
            );

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
            entries.accept(SPLENDOR_OF_TRANQUIL_WATERS);
            entries.accept(EVOKER_FANG_STAFF);
            entries.accept(VORTEX_VANQUISHER);
            entries.accept(ARC_CASTER);
            entries.accept(FROSTBOLT_WAND);
            entries.accept(NIGHTS_FOCUS_STAFF);
            entries.accept(WEBCASTER);
            entries.accept(WIND_STAFF);
            entries.accept(OMNI_STAFF);
            entries.accept(NETHERITE_DRAGONFIRE_SCEPTER);
            entries.accept(NETHERITE_RESONANT_ECHO);
            entries.accept(NETHERITE_INFERNO_ROD);
            entries.accept(NETHERITE_GHASTFIRE_STAFF);
            entries.accept(NETHERITE_WITHER_SCEPTER);
            entries.accept(NETHERITE_SHULKER_SEEKER);
            entries.accept(NETHERITE_GUARDIAN_LENS);
            entries.accept(NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS);
            entries.accept(NETHERITE_EVOKER_FANG_STAFF);
            entries.accept(NETHERITE_VORTEX_VANQUISHER);
            entries.accept(NETHERITE_ARC_CASTER);
            entries.accept(NETHERITE_FROSTBOLT_WAND);
            entries.accept(NETHERITE_NIGHTS_FOCUS_STAFF);
            entries.accept(NETHERITE_WEBCASTER);
            entries.accept(NETHERITE_WIND_STAFF);
            entries.accept(NETHERITE_OMNI_STAFF);
        });

        // Also add artifacts to vanilla Tools & Utilities tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(ELEMENTAL_CODEX);
            entries.accept(VOID_TRANSLOCATOR);
            entries.accept(MAGIC_MIRROR);
            entries.accept(NETHERITE_VOID_TRANSLOCATOR);
            entries.accept(NETHERITE_MAGIC_MIRROR);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(PYRO_ELEMENT);
            entries.accept(HYDRO_ELEMENT);
            entries.accept(ANEMO_ELEMENT);
            entries.accept(ELECTRO_ELEMENT);
            entries.accept(DENDRO_ELEMENT);
            entries.accept(CRYO_ELEMENT);
            entries.accept(GEO_ELEMENT);
            entries.accept(OMNI_ELEMENT);
            entries.accept(FROST_ROD);
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
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()
                    && entity instanceof net.minecraft.world.entity.LivingEntity living
                    && ElementalGaugeManager.isEntityFrozen(living)) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!entity.level().isClientSide()
                    && ElementalGaugeManager.isOmniInnate(entity)
                    && ElementalGaugeManager.isElementalOrMagicDamage(source)) {
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    ElementalGaugeManager.notifyOmniImmuneEnvironmental(sl, entity);
                }
                return false;
            }
            if (!entity.level().isClientSide()
                    && !(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.EvokerFangs f && f.getTags().contains("velorise_staves:geo_fangs"))
                    && com.velorise.staves.entity.GeoFangPillarEntity.interceptIncomingDamage(entity, source, amount)) {
                return false;
            }
            if (!entity.level().isClientSide()
                    && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                    && !source.is(net.minecraft.world.damagesource.DamageTypes.STALAGMITE)
                    && ElementalGaugeManager.isAnemoInnateMob(entity)) {
                entity.fallDistance = 0.0F;
                return false;
            }
            if (!entity.level().isClientSide()
                    && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    && ElementalGaugeManager.isPyroInnate(entity)) {
                ElementalGaugeManager.extinguishPyroInnate(entity);
                return false;
            }
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
                com.velorise.staves.element.MobHazardAvoidanceManager.attachGoalIfEligible(living);
            }
            if (entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs && world instanceof net.minecraft.server.level.ServerLevel sl) {
                ElementalGaugeManager.onEvokerFangJoin(sl, fangs);
            }
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, damage, afterDamage, blocked) -> {
            if (!blocked && !entity.level().isClientSide()) {
                ElementalGaugeManager.trySendPhysicalDamageFloat(entity, source, afterDamage);
                ElementalGaugeManager.handleLivingIncomingDamage(entity, source);
                ElementalGaugeManager.enforceFrozenPoseIfFrozen(entity);
            }
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!entity.level().isClientSide()) {
                ElementalGaugeManager.tryDropElementOnDeath(entity, damageSource);
                ElementalGaugeManager.clearEntityState(entity);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (oldPlayer.getTags().contains("velorise_staves:received_codex")) {
                newPlayer.addTag("velorise_staves:received_codex");
            }
            ElementalGaugeManager.clearEntityState(oldPlayer);
            ElementalGaugeManager.clearEntityState(newPlayer);
            com.velorise.staves.element.ElementalResonanceManager.syncPlayerImmediate(newPlayer);
        });
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living && !entity.level().isClientSide()) {
                ElementalGaugeManager.syncEntityOnStartTracking(player, living);
            }
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
            com.velorise.staves.network.ModNetwork.sendConfigSyncToPlayer(player);
            ElementalGaugeManager.syncEntityOnStartTracking(player, player);
            com.velorise.staves.element.ElementalResonanceManager.syncPlayerImmediate(player);
            if (!player.getTags().contains("velorise_staves:received_codex")) {
                player.addTag("velorise_staves:received_codex");
                player.getInventory().add(new ItemStack(ELEMENTAL_CODEX));
            }
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ElementalGaugeManager.clearEntityState(handler.getPlayer());
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
