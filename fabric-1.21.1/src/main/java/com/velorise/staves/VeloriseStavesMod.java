package com.velorise.staves;

import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import com.velorise.staves.manager.TemporaryWebManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VeloriseStavesMod implements ModInitializer {
    public static final String MODID = "velorise_staves";

    // Sounds
    public static final SoundEvent MAGIC_MIRROR_USE_SOUND = registerSound("magic_mirror_use");

    // Basic Combat Staves
    public static final Item DRAGONFIRE_SCEPTER = registerItem("dragonfire_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, 130, false));

    public static final Item RESONANT_ECHO = registerItem("resonant_echo",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, 130, false));

    public static final Item INFERNO_ROD = registerItem("inferno_rod",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, 150, false));

    public static final Item GHASTFIRE_STAFF = registerItem("ghastfire_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, 120, false));

    public static final Item WITHER_SCEPTER = registerItem("wither_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, 140, false));

    public static final Item SHULKER_SEEKER = registerItem("shulker_seeker",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, 130, false));

    public static final Item GUARDIAN_LENS = registerItem("guardian_lens",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, 130, false));

    public static final Item EVOKER_FANG_STAFF = registerItem("evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 120, false));

    public static final Item STORMCALLER = registerItem("stormcaller",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, 130, false));

    public static final Item FROSTBOLT_WAND = registerItem("frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, 150, false));

    public static final Item WEBCASTER = registerItem("webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, 140, false));

    // Basic Utility Artifacts
    public static final Item VOID_TRANSLOCATOR = registerItem("void_translocator",
        new VoidTranslocatorItem(80, 24.0D, false));

    public static final Item MAGIC_MIRROR = registerItem("magic_mirror",
        new MagicMirrorItem(40, false));

    // Netherite Upgraded Combat Staves
    public static final Item NETHERITE_DRAGONFIRE_SCEPTER = registerItem("netherite_dragonfire_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.DRAGONFIRE, 500, true));

    public static final Item NETHERITE_RESONANT_ECHO = registerItem("netherite_resonant_echo",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.ECHO, 500, true));

    public static final Item NETHERITE_INFERNO_ROD = registerItem("netherite_inferno_rod",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.INFERNO, 550, true));

    public static final Item NETHERITE_GHASTFIRE_STAFF = registerItem("netherite_ghastfire_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GHASTFIRE, 450, true));

    public static final Item NETHERITE_WITHER_SCEPTER = registerItem("netherite_wither_scepter",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WITHER, 500, true));

    public static final Item NETHERITE_SHULKER_SEEKER = registerItem("netherite_shulker_seeker",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.SHULKER, 500, true));

    public static final Item NETHERITE_GUARDIAN_LENS = registerItem("netherite_guardian_lens",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.GUARDIAN, 500, true));

    public static final Item NETHERITE_EVOKER_FANG_STAFF = registerItem("netherite_evoker_fang_staff",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.EVOKER, 450, true));

    public static final Item NETHERITE_STORMCALLER = registerItem("netherite_stormcaller",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.LIGHTNING, 500, true));

    public static final Item NETHERITE_FROSTBOLT_WAND = registerItem("netherite_frostbolt_wand",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.FROST, 500, true));

    public static final Item NETHERITE_WEBCASTER = registerItem("netherite_webcaster",
        new ArcaneStaffItem(ArcaneStaffItem.Kind.WEB, 500, true));

    // Netherite Upgraded Utility Artifacts
    public static final Item NETHERITE_VOID_TRANSLOCATOR = registerItem("netherite_void_translocator",
        new VoidTranslocatorItem(250, 36.0D, true));

    public static final Item NETHERITE_MAGIC_MIRROR = registerItem("netherite_magic_mirror",
        new MagicMirrorItem(150, true));

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
            output.accept(STORMCALLER);
            output.accept(FROSTBOLT_WAND);
            output.accept(WEBCASTER);
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
            output.accept(NETHERITE_STORMCALLER);
            output.accept(NETHERITE_FROSTBOLT_WAND);
            output.accept(NETHERITE_WEBCASTER);
            output.accept(NETHERITE_VOID_TRANSLOCATOR);
            output.accept(NETHERITE_MAGIC_MIRROR);
        })
        .build();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
        ServerTickEvents.END_SERVER_TICK.register(TemporaryWebManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(TemporaryWebManager::clearAll);
    }

    private static Item registerItem(String path, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, path), item);
    }

    private static SoundEvent registerSound(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
