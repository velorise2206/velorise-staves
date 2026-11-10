package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interactive in-game guidebook ("Elemental Codex") with categorized sidebar navigation,
 * element/item icons, beginner-friendly visual reaction equations, live server-synced
 * multipliers, and auto-detected custom screenshot slots.
 */
public class ElementalCodexScreen extends Screen {
    private static final int WIN_W = 436;
    private static final int WIN_H = 246;
    private static final int SIDEBAR_W = 138;
    private static final int HEADER_H = 26;

    private static final Map<Element, ResourceLocation> ELEMENT_ICONS = new EnumMap<>(Element.class);
    private static final ResourceLocation BLAZE_ICON = new ResourceLocation(VeloriseStavesMod.MODID, "textures/gui/codex/blaze_icon.png");
    private static final Map<String, Boolean> SCREENSHOT_CACHE = new HashMap<>();

    static {
        for (Element e : Element.values()) {
            String lower = e.name().toLowerCase(Locale.ROOT);
            ELEMENT_ICONS.put(e, new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/" + lower + ".png"));
        }
    }

    public enum Tab {
        BASICS("basics", "1.png", 1012, 778, 0xFF55EFC4, null),
        ELEMENTS("elements", null, 0, 0, 0xFFFF6B4A, Element.PYRO),
        REACTIONS_AMP("reactions_amp", "3.png", 1446, 785, 0xFF0A84FF, Element.HYDRO),
        REACTIONS_DENDRO("reactions_dendro", "4.png", 1146, 736, 0xFF32D74B, Element.DENDRO),
        INNATE("innate", "5.png", 1200, 879, 0xFFFF9F0A, null),
        ENVIRONMENT("environment", "6.png", 1114, 654, 0xFF64D2FF, null),
        STAVES("staves", null, 0, 0, 0xFFBF5AF2, null),
        MASTERY("mastery", null, 0, 0, 0xFFFFD60A, null);

        public final String key;
        public final String screenshotFile;
        public final int imgW;
        public final int imgH;
        public final int accentColor;
        public final Element iconElement;

        Tab(String key, String screenshotFile, int imgW, int imgH, int accentColor, Element iconElement) {
            this.key = key;
            this.screenshotFile = screenshotFile;
            this.imgW = imgW;
            this.imgH = imgH;
            this.accentColor = accentColor;
            this.iconElement = iconElement;
        }
    }

    private record GuideCard(
            String title,
            int borderColor,
            Element elem1,
            Element elem2,
            ItemStack item1,
            ItemStack item2,
            String badgeText,
            int badgeColor,
            List<String> paragraphs
    ) {}

    private final Screen parent;
    private Tab currentTab = Tab.BASICS;
    private double scrollOffset = 0.0D;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;

    public ElementalCodexScreen(Screen parent) {
        super(Component.translatable("codex.velorise_staves.title"));
        this.parent = parent;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ElementalCodexScreen(mc.screen));
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new ElementalCodexScreen(parent));
    }

    @Override
    protected void init() {
        super.init();
        SCREENSHOT_CACHE.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ItemStack getModItemStack(String id) {
        try {
            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(VeloriseStavesMod.MODID, id));
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {}
        return new ItemStack(Items.BOOK);
    }

    private ItemStack getTabIconStack(Tab tab) {
        return switch (tab) {
            case BASICS -> getModItemStack("elemental_codex");
            case REACTIONS_DENDRO -> getModItemStack("dendro_core");
            case INNATE -> new ItemStack(Items.BLAZE_POWDER);
            case ENVIRONMENT -> new ItemStack(Items.SCULK_SENSOR);
            case STAVES -> getModItemStack("netherite_dragonfire_scepter");
            case MASTERY -> new ItemStack(Items.ENCHANTED_BOOK);
            default -> ItemStack.EMPTY;
        };
    }

    private boolean hasCustomScreenshot(String filename) {
        return SCREENSHOT_CACHE.computeIfAbsent(filename, fn -> {
            try {
                ResourceLocation loc = new ResourceLocation(VeloriseStavesMod.MODID, "textures/gui/codex/" + fn);
                return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent();
            } catch (Exception e) {
                return false;
            }
        });
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String fmt(double val) {
        if (Math.abs(val - Math.round(val)) < 0.01D) {
            return String.valueOf(Math.round(val));
        }
        return String.format(Locale.ROOT, "%.1f", val);
    }

    private List<GuideCard> buildCardsForTab(Tab tab) {
        List<GuideCard> cards = new ArrayList<>();
        double dmgMult = StavesConfig.getDamageMultiplier();
        double cdMult = StavesConfig.getCooldownMultiplier();
        double rngMult = StavesConfig.getRangeMultiplier();

        switch (tab) {
            case BASICS -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card1.title"),
                        0xFF55EFC4,
                        Element.PYRO, Element.HYDRO,
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        "Aura + Trigger", 0xFF55EFC4,
                        List.of(
                                tr("codex.velorise_staves.basics.card1.p1"),
                                tr("codex.velorise_staves.basics.card1.p2"),
                                tr("codex.velorise_staves.basics.card1.p3")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card2.title"),
                        0xFFFFD60A,
                        Element.ELECTRO, Element.CRYO,
                        getModItemStack("inferno_rod"), getModItemStack("netherite_inferno_rod"),
                        "1.0U / 1.5U / 2.0U", 0xFFFFD60A,
                        List.of(
                                tr("codex.velorise_staves.basics.card2.p1"),
                                tr("codex.velorise_staves.basics.card2.p2")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card3.title"),
                        0xFFBF5AF2,
                        Element.GEO, Element.DENDRO,
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        "Innate & Mutation", 0xFFBF5AF2,
                        List.of(
                                tr("codex.velorise_staves.basics.card3.p1"),
                                tr("codex.velorise_staves.basics.card3.p2")
                        )
                ));
            }
            case ELEMENTS -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.pyro.title"), 0xFFFF453A,
                        Element.PYRO, null, getModItemStack("inferno_rod"), getModItemStack("ghastfire_staff"),
                        "PYRO", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.elem.pyro.p1"), tr("codex.velorise_staves.elem.pyro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.hydro.title"), 0xFF0A84FF,
                        Element.HYDRO, null, getModItemStack("guardian_lens"), ItemStack.EMPTY,
                        "HYDRO", 0xFF0A84FF,
                        List.of(tr("codex.velorise_staves.elem.hydro.p1"), tr("codex.velorise_staves.elem.hydro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.electro.title"), 0xFFBF5AF2,
                        Element.ELECTRO, null, getModItemStack("arc_caster"), getModItemStack("dragonfire_scepter"),
                        "ELECTRO", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.elem.electro.p1"), tr("codex.velorise_staves.elem.electro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.cryo.title"), 0xFF64D2FF,
                        Element.CRYO, null, getModItemStack("frostbolt_wand"), ItemStack.EMPTY,
                        "CRYO (-30% SPD)", 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.elem.cryo.p1"), tr("codex.velorise_staves.elem.cryo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.dendro.title"), 0xFF32D74B,
                        Element.DENDRO, null, getModItemStack("wither_scepter"), getModItemStack("webcaster"),
                        "DENDRO", 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.elem.dendro.p1"), tr("codex.velorise_staves.elem.dendro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.anemo.title"), 0xFF55EFC4,
                        Element.ANEMO, null, getModItemStack("resonant_echo"), getModItemStack("shulker_seeker"),
                        "ANEMO", 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.elem.anemo.p1"), tr("codex.velorise_staves.elem.anemo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.geo.title"), 0xFFFFD60A,
                        Element.GEO, null, getModItemStack("evoker_fang_staff"), getModItemStack("pyro_crystallize_shard"),
                        "GEO", 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.elem.geo.p1"), tr("codex.velorise_staves.elem.geo.p2"))
                ));
            }
            case REACTIONS_AMP -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.vaporize.title"), 0xFFFF8C33,
                        Element.PYRO, Element.HYDRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "x2.0 / x1.5 DMG", 0xFFFF8C33,
                        List.of(tr("codex.velorise_staves.react.vaporize.p1"), tr("codex.velorise_staves.react.vaporize.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.melt.title"), 0xFFFF3B30,
                        Element.CRYO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "x2.0 / x1.5 DMG", 0xFFFF3B30,
                        List.of(tr("codex.velorise_staves.react.melt.p1"), tr("codex.velorise_staves.react.melt.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.overvape.title"), 0xFFFF6B4A,
                        Element.HYDRO, Element.ELECTRO, getModItemStack("inferno_rod"), ItemStack.EMPTY,
                        "x2.0 + " + fmt(5.0 * dmgMult) + " AoE", 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.react.overvape.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.overloaded.title"), 0xFFFF6B4A,
                        Element.PYRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmt(5.0 * dmgMult) + " DMG (3.0m AoE)", 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.react.overloaded.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.electro_charged.title"), 0xFFBF5AF2,
                        Element.HYDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmt(2.0 * dmgMult) + " DMG/s (5.0m)", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.react.electro_charged.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.frozen.title"), 0xFF64D2FF,
                        Element.HYDRO, Element.CRYO, getModItemStack("evoker_fang_staff"), ItemStack.EMPTY,
                        "Freeze + " + fmt(6.0 * dmgMult) + " Shatter", 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.react.frozen.p1"), tr("codex.velorise_staves.react.frozen.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.superconduct.title"), 0xFF5AC8FA,
                        Element.CRYO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmt(2.5 * dmgMult) + " DMG +25% Vuln", 0xFF5AC8FA,
                        List.of(tr("codex.velorise_staves.react.superconduct.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.swirl.title"), 0xFF55EFC4,
                        Element.ANEMO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmt(2.5 * dmgMult) + " DMG (5.0m Spread)", 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.react.swirl.p1"))
                ));
            }
            case REACTIONS_DENDRO -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.bloom.title"), 0xFF32D74B,
                        Element.DENDRO, Element.HYDRO, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        "Core (" + fmt(4.0 * dmgMult) + " DMG / 5s)", 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.react.bloom.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.burgeon.title"), 0xFFFF9F0A,
                        Element.PYRO, Element.DENDRO, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        fmt(8.0 * dmgMult) + " DMG (4.0m AoE)", 0xFFFF9F0A,
                        List.of(tr("codex.velorise_staves.react.burgeon.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.hyperbloom.title"), 0xFFDA70D6,
                        Element.ELECTRO, Element.DENDRO, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        fmt(8.0 * dmgMult) + " Homing Shot", 0xFFDA70D6,
                        List.of(tr("codex.velorise_staves.react.hyperbloom.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.catalyze.title"), 0xFFA8E063,
                        Element.DENDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "+" + fmt(3.0 * dmgMult) + " / +" + fmt(3.5 * dmgMult) + " DMG", 0xFFA8E063,
                        List.of(tr("codex.velorise_staves.react.catalyze.p1"), tr("codex.velorise_staves.react.catalyze.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.burning.title"), 0xFFFF453A,
                        Element.DENDRO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmt(1.0 * dmgMult) + " DMG / 0.5s", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.react.burning.p1"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.crystallize.title"), 0xFFFFD60A,
                        Element.GEO, Element.PYRO, getModItemStack("pyro_crystallize_shard"), getModItemStack("hydro_crystallize_shard"),
                        "+" + fmt(2.0 * dmgMult) + " Shield HP (15s)", 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.react.crystallize.p1"), tr("codex.velorise_staves.react.crystallize.p2"))
                ));
            }
            case INNATE -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.rules.title"), 0xFFFFD60A,
                        Element.PYRO, Element.HYDRO, new ItemStack(Items.BLAZE_POWDER), ItemStack.EMPTY,
                        "IMMUNE & 6% Mutate", 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.innate.rules.p1"), tr("codex.velorise_staves.innate.rules.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.pyro.title"), 0xFFFF453A,
                        Element.PYRO, null, new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.MAGMA_CREAM),
                        "1.0U Pyro", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.innate.pyro.p1"), tr("codex.velorise_staves.innate.pyro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.hydro.title"), 0xFF0A84FF,
                        Element.HYDRO, null, new ItemStack(Items.PRISMARINE_SHARD), new ItemStack(Items.TRIDENT),
                        "1.0U Hydro", 0xFF0A84FF,
                        List.of(tr("codex.velorise_staves.innate.hydro.p1"), tr("codex.velorise_staves.innate.hydro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.electro.title"), 0xFFBF5AF2,
                        Element.ELECTRO, null, new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.DRAGON_BREATH),
                        "1.0U–1.5U Electro", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.innate.electro.p1"), tr("codex.velorise_staves.innate.electro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.cryo.title"), 0xFF64D2FF,
                        Element.CRYO, null, new ItemStack(Items.SNOWBALL), new ItemStack(Items.PACKED_ICE),
                        "1.0U Cryo", 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.innate.cryo.p1"), tr("codex.velorise_staves.innate.cryo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.dendro.title"), 0xFF32D74B,
                        Element.DENDRO, null, new ItemStack(Items.SLIME_BALL), new ItemStack(Items.SPIDER_EYE),
                        "1.0U Dendro", 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.innate.dendro.p1"), tr("codex.velorise_staves.innate.dendro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.anemo_geo.title"), 0xFF55EFC4,
                        Element.ANEMO, Element.GEO, new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.ECHO_SHARD),
                        "Anemo & Geo", 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.innate.anemo_geo.p1"), tr("codex.velorise_staves.innate.anemo_geo.p2"))
                ));
            }
            case ENVIRONMENT -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.sculk.title"), 0xFF00D2D3,
                        Element.ELECTRO, null, new ItemStack(Items.SCULK_SENSOR), new ItemStack(Items.CALIBRATED_SCULK_SENSOR),
                        "8m / 16m Zap", 0xFF00D2D3,
                        List.of(tr("codex.velorise_staves.env.sculk.p1"), tr("codex.velorise_staves.env.sculk.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.redstone.title"), 0xFFFF453A,
                        Element.ELECTRO, null, new ItemStack(Items.LIGHTNING_ROD), new ItemStack(Items.OBSERVER),
                        "Redstone Zap", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.env.redstone.p1"), tr("codex.velorise_staves.env.redstone.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.amethyst.title"), 0xFFBF5AF2,
                        Element.ELECTRO, Element.PYRO, new ItemStack(Items.AMETHYST_CLUSTER), new ItemStack(Items.AMETHYST_SHARD),
                        "2–4 Shards", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.env.amethyst.p1"), tr("codex.velorise_staves.env.amethyst.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.pyro_melt.title"), 0xFFFF6B4A,
                        Element.PYRO, Element.CRYO, new ItemStack(Items.ICE), new ItemStack(Items.WATER_BUCKET),
                        "Melt Physics", 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.env.pyro_melt.p1"), tr("codex.velorise_staves.env.pyro_melt.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.pyro_burn.title"), 0xFFFF8C33,
                        Element.PYRO, null, new ItemStack(Items.TNT), new ItemStack(Items.CAMPFIRE),
                        "Ignite & Blast", 0xFFFF8C33,
                        List.of(tr("codex.velorise_staves.env.pyro_burn.p1"), tr("codex.velorise_staves.env.pyro_burn.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.world_traps.title"), 0xFF32D74B,
                        Element.HYDRO, Element.DENDRO, new ItemStack(Items.SOUL_CAMPFIRE), new ItemStack(Items.POINTED_DRIPSTONE),
                        "World & Gear", 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.env.world_traps.p1"), tr("codex.velorise_staves.env.world_traps.p2"))
                ));
            }
            case STAVES -> {
                int basicDur = StavesConfig.getBasicStaffDurability();
                int nethDur = StavesConfig.getNetheriteStaffDurability();
                Object[][] staffDefs = new Object[][]{
                        {"inferno_rod", Element.PYRO, 4.0, 6.0, 0.75, 0.6, 28.0, 35.0, "codex.velorise_staves.staff.inferno"},
                        {"ghastfire_staff", Element.PYRO, 6.0, 9.0, 1.5, 1.2, 32.0, 40.0, "codex.velorise_staves.staff.ghastfire"},
                        {"guardian_lens", Element.HYDRO, 4.0, 6.0, 0.9, 0.7, 24.0, 30.0, "codex.velorise_staves.staff.guardian"},
                        {"frostbolt_wand", Element.CRYO, 3.0, 4.5, 0.6, 0.5, 24.0, 30.0, "codex.velorise_staves.staff.frostbolt"},
                        {"arc_caster", Element.ELECTRO, 4.0, 6.0, 0.9, 0.7, 20.0, 25.0, "codex.velorise_staves.staff.arc"},
                        {"dragonfire_scepter", Element.ELECTRO, 5.0, 7.5, 1.25, 1.0, 26.0, 32.5, "codex.velorise_staves.staff.dragonfire"},
                        {"wither_scepter", Element.DENDRO, 4.0, 6.0, 1.0, 0.8, 26.0, 32.5, "codex.velorise_staves.staff.wither"},
                        {"webcaster", Element.DENDRO, 2.0, 3.0, 0.9, 0.7, 22.0, 27.5, "codex.velorise_staves.staff.webcaster"},
                        {"wind_staff", Element.ANEMO, 3.0, 4.5, 0.7, 0.55, 24.0, 30.0, "codex.velorise_staves.staff.wind"},
                        {"resonant_echo", Element.ANEMO, 8.0, 12.0, 2.0, 1.6, 18.0, 22.5, "codex.velorise_staves.staff.echo"},
                        {"shulker_seeker", Element.ANEMO, 4.0, 6.0, 1.1, 0.9, 28.0, 35.0, "codex.velorise_staves.staff.shulker"},
                        {"evoker_fang_staff", Element.GEO, 6.0, 9.0, 1.25, 1.0, 18.0, 22.5, "codex.velorise_staves.staff.evoker"}
                };
                for (Object[] def : staffDefs) {
                    String baseId = (String) def[0];
                    Element el = (Element) def[1];
                    double bDmg = (Double) def[2] * dmgMult;
                    double nDmg = (Double) def[3] * dmgMult;
                    double bCd = (Double) def[4] * cdMult;
                    double nCd = (Double) def[5] * cdMult;
                    double bRng = (Double) def[6] * rngMult;
                    double nRng = (Double) def[7] * rngMult;
                    String descKey = (String) def[8];

                    ItemStack basicStack = getModItemStack(baseId);
                    ItemStack nethStack = getModItemStack("netherite_" + baseId);
                    String name = tr("item.velorise_staves." + baseId);
                    String stats = String.format(Locale.ROOT, "§eDMG:§r %s ➔ §6%s§r | §bCD:§r %.1fs ➔ §a%.1fs§r | §dRng:§r %sm ➔ %sm | §7Dur: %d/%d",
                            fmt(bDmg), fmt(nDmg), bCd, nCd, fmt(bRng), fmt(nRng), basicDur, nethDur);
                    cards.add(new GuideCard(
                            name, ClientAuraRenderer.getElementBorderColor(el),
                            el, null, basicStack, nethStack,
                            el.name(), ClientAuraRenderer.getElementBorderColor(el),
                            List.of(stats, tr(descKey))
                    ));
                }
                // Utility Artifacts
                cards.add(new GuideCard(
                        tr("item.velorise_staves.void_translocator") + " & " + tr("item.velorise_staves.magic_mirror"),
                        0xFFDA70D6, null, null,
                        getModItemStack("netherite_void_translocator"), getModItemStack("netherite_magic_mirror"),
                        "UTILITY", 0xFFDA70D6,
                        List.of(
                                tr("codex.velorise_staves.staff.utility.p1", fmt(24.0 * rngMult), fmt(36.0 * rngMult)),
                                tr("codex.velorise_staves.staff.utility.p2", fmt(StavesConfig.getMagicMirrorCooldownSeconds() * cdMult))
                        )
                ));
            }
            case MASTERY -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card1.title"), 0xFFFFD60A,
                        Element.PYRO, Element.ELECTRO, new ItemStack(Items.ENCHANTED_BOOK), getModItemStack("netherite_arc_caster"),
                        "Max Level: III", 0xFFFFD60A,
                        List.of(
                                tr("codex.velorise_staves.mastery.card1.p1"),
                                tr("codex.velorise_staves.mastery.card1.p2"),
                                tr("codex.velorise_staves.mastery.card1.p3"),
                                tr("codex.velorise_staves.mastery.card1.p4")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card2.title"), 0xFF55EFC4,
                        null, null, new ItemStack(Items.ENCHANTING_TABLE), new ItemStack(Items.EMERALD),
                        "Survival Sources", 0xFF55EFC4,
                        List.of(
                                tr("codex.velorise_staves.mastery.card2.p1"),
                                tr("codex.velorise_staves.mastery.card2.p2")
                        )
                ));
                String syncMode = StavesConfig.isServerSynced()
                        ? tr("codex.velorise_staves.sync.server")
                        : tr("codex.velorise_staves.sync.local");
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card3.title"), 0xFF0A84FF,
                        Element.HYDRO, Element.GEO, getModItemStack("elemental_codex"), ItemStack.EMPTY,
                        syncMode, StavesConfig.isServerSynced() ? 0xFF32D74B : 0xFF64D2FF,
                        List.of(
                                String.format(Locale.ROOT, "• Damage Multiplier: %.2fx | Cooldown Multiplier: %.2fx | Range Multiplier: %.2fx",
                                        StavesConfig.getDamageMultiplier(), StavesConfig.getCooldownMultiplier(), StavesConfig.getRangeMultiplier()),
                                String.format(Locale.ROOT, "• Elemental Reactions (EGT): %s | Staff Durability: %d (Basic) / %d (Netherite)",
                                        StavesConfig.isElementalGaugeEnabled() ? "ON" : "OFF",
                                        StavesConfig.getBasicStaffDurability(), StavesConfig.getNetheriteStaffDurability()),
                                tr("codex.velorise_staves.mastery.card3.p1")
                        )
                ));
            }
        }
        return cards;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int winW = Math.min(WIN_W, this.width - 12);
        int winH = Math.min(WIN_H, this.height - 12);
        int left = (this.width - winW) / 2;
        int top = (this.height - winH) / 2;

        // 1. Main Arcane Window Frame
        guiGraphics.fill(left - 2, top - 2, left + winW + 2, top + winH + 2, 0xCC05070C);
        guiGraphics.fill(left, top, left + winW, top + winH, 0xF4101522);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, left, top, winW, winH, 0xFFD4A02A);

        // 2. Top Header Bar
        guiGraphics.fill(left + 1, top + 1, left + winW - 1, top + HEADER_H, 0xFF171E30);
        guiGraphics.fill(left + 1, top + HEADER_H, left + winW - 1, top + HEADER_H + 1, 0xFF8C6B20);

        ItemStack codexIcon = getModItemStack("elemental_codex");
        guiGraphics.renderItem(codexIcon, left + 6, top + 5);
        guiGraphics.drawString(this.font, this.title, left + 26, top + 9, 0xFFFFE082, true);

        // Header Right Buttons: [⚙ Config] and [✕]
        int closeBtnW = 20;
        int closeBtnX = left + winW - closeBtnW - 4;
        int closeBtnY = top + 4;
        boolean hoverClose = mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + 18;
        guiGraphics.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + 18, hoverClose ? 0xFF882222 : 0xFF2B1B24);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, closeBtnX, closeBtnY, closeBtnW, 18, hoverClose ? 0xFFFF5555 : 0xFF773333);
        guiGraphics.drawCenteredString(this.font, "✕", closeBtnX + closeBtnW / 2, closeBtnY + 5, 0xFFFFFFFF);

        int cfgBtnW = 58;
        int cfgBtnX = closeBtnX - cfgBtnW - 4;
        int cfgBtnY = top + 4;
        boolean hoverCfg = mouseX >= cfgBtnX && mouseX <= cfgBtnX + cfgBtnW && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 18;
        guiGraphics.fill(cfgBtnX, cfgBtnY, cfgBtnX + cfgBtnW, cfgBtnY + 18, hoverCfg ? 0xFF253B5E : 0xFF18243A);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, cfgBtnX, cfgBtnY, cfgBtnW, 18, hoverCfg ? 0xFF64D2FF : 0xFF3A5A88);
        guiGraphics.drawCenteredString(this.font, "⚙ Config", cfgBtnX + cfgBtnW / 2, cfgBtnY + 5, 0xFFE0F2FF);

        // 3. Left Sidebar Category Tabs
        int sideX = left + 4;
        int sideY = top + HEADER_H + 4;
        int sideH = winH - HEADER_H - 8;
        guiGraphics.fill(sideX, sideY, sideX + SIDEBAR_W, sideY + sideH, 0xCC0B0E17);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, sideX, sideY, SIDEBAR_W, sideH, 0xFF2A3550);

        Tab[] tabs = Tab.values();
        int tabH = Math.min(24, (sideH - 6 - (tabs.length - 1) * 2) / tabs.length);
        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int ty = sideY + 3 + i * (tabH + 2);
            int tx = sideX + 3;
            int tw = SIDEBAR_W - 6;

            boolean selected = (tab == this.currentTab);
            boolean hovered = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + tabH;

            int bg = selected ? 0xFF1E2942 : (hovered ? 0xFF161F33 : 0xFF0E1320);
            int border = selected ? tab.accentColor : (hovered ? 0xFF4A5D80 : 0xFF20293D);
            guiGraphics.fill(tx, ty, tx + tw, ty + tabH, bg);
            ClientAuraRenderer.renderColoredBorder(guiGraphics, tx, ty, tw, tabH, border);

            // Tab Icon (16x16 Element texture, Blaze texture, or ItemStack)
            int iconOffsetY = ty + (tabH - 16) / 2;
            if (tab.iconElement != null) {
                drawElementIcon(guiGraphics, tab.iconElement, tx + 5, iconOffsetY);
            } else if (tab == Tab.INNATE) {
                RenderSystem.enableBlend();
                guiGraphics.blit(BLAZE_ICON, tx + 5, iconOffsetY, 0.0F, 0.0F, 16, 16, 16, 16);
            } else {
                ItemStack iconStack = getTabIconStack(tab);
                if (!iconStack.isEmpty()) {
                    guiGraphics.renderItem(iconStack, tx + 5, iconOffsetY);
                }
            }

            String tabLabel = tr("codex.velorise_staves.tab." + tab.key);
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFD8E6FF : 0xFFA0B0C8);
            drawLeftFitString(guiGraphics, tabLabel, tx + 24, ty + (tabH - 8) / 2, tw - 28, textColor, selected);
        }

        // 4. Right Content Area (Scrollable with Scissor)
        int contentX = sideX + SIDEBAR_W + 4;
        int contentY = sideY;
        int contentW = winW - SIDEBAR_W - 12;
        int contentH = sideH;

        guiGraphics.fill(contentX, contentY, contentX + contentW, contentY + contentH, 0xCC0C101B);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, contentX, contentY, contentW, contentH, 0xFF2E3B59);

        int innerX = contentX + 6;
        int innerW = contentW - 18; // leave 10px on right for scrollbar
        int startY = contentY + 6 - (int) Math.round(this.scrollOffset);

        guiGraphics.enableScissor(contentX + 2, contentY + 2, contentX + contentW - 2, contentY + contentH - 2);

        int curY = startY;

        // 4A. Render Smart Illustration / Screenshot Showcase Box at top of tab
        curY += renderIllustrationBox(guiGraphics, this.currentTab, innerX, curY, innerW) + 6;

        // 4B. Render Guide Cards
        List<GuideCard> cards = buildCardsForTab(this.currentTab);
        for (GuideCard card : cards) {
            curY += renderGuideCard(guiGraphics, card, innerX, curY, innerW) + 6;
        }

        guiGraphics.disableScissor();

        int totalContentHeight = (curY - startY) + 8;
        this.maxScroll = Math.max(0, totalContentHeight - contentH);
        if (this.scrollOffset > this.maxScroll) {
            this.scrollOffset = this.maxScroll;
        }

        // 5. Render Scrollbar
        int sbX = contentX + contentW - 8;
        int sbY = contentY + 4;
        int sbH = contentH - 8;
        guiGraphics.fill(sbX, sbY, sbX + 4, sbY + sbH, 0x8806080E);
        if (this.maxScroll > 0) {
            int thumbH = Math.max(20, (int) ((float) contentH / (float) totalContentHeight * sbH));
            int thumbY = sbY + (int) ((this.scrollOffset / this.maxScroll) * (sbH - thumbH));
            guiGraphics.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, this.currentTab.accentColor);
        } else {
            guiGraphics.fill(sbX, sbY, sbX + 4, sbY + sbH, 0x443A4A68);
        }
    }

    private int renderIllustrationBox(GuiGraphics guiGraphics, Tab tab, int x, int y, int w) {
        if (tab.screenshotFile != null && hasCustomScreenshot(tab.screenshotFile)) {
            int h = 136;
            guiGraphics.fill(x, y, x + w, y + h, 0xFF131A2C);
            ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, h, tab.accentColor);

            int drawH = h - 8;
            int drawW = (tab.imgW > 0 && tab.imgH > 0)
                    ? Math.min(w - 8, Math.round(drawH * (float) tab.imgW / (float) tab.imgH))
                    : (w - 8);
            int imgX = x + (w - drawW) / 2;
            int imgY = y + (h - drawH) / 2;

            guiGraphics.fill(imgX - 1, imgY - 1, imgX + drawW + 1, imgY + drawH + 1, (0x88 << 24) | (tab.accentColor & 0x00FFFFFF));
            ResourceLocation loc = new ResourceLocation(VeloriseStavesMod.MODID, "textures/gui/codex/" + tab.screenshotFile);
            RenderSystem.enableBlend();
            guiGraphics.blit(loc, imgX, imgY, 0.0F, 0.0F, drawW, drawH, drawW, drawH);
            return h;
        }

        int h = 48;
        guiGraphics.fill(x, y, x + w, y + h, 0xFF131A2C);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, h, tab.accentColor);

        // Built-in interactive visual diagram
        String diagramTitle = tr("codex.velorise_staves.diagram." + tab.key);
        drawCenteredFitString(guiGraphics, "§e✦ " + diagramTitle + " ✦", x + w / 2, y + 6, w - 10, 0xFFFFE082);

        int midY = y + 22;
        int cx = x + w / 2;

        switch (tab) {
            case BASICS -> {
                guiGraphics.renderItem(getModItemStack("inferno_rod"), cx - 114, midY);
                drawElementIcon(guiGraphics, Element.PYRO, cx - 94, midY);
                guiGraphics.drawString(this.font, "1.Aura", cx - 75, midY + 4, 0xFFFF6B4A, true);
                guiGraphics.drawString(this.font, "+ ➔", cx - 37, midY + 4, 0xFFFFD60A, true);
                guiGraphics.renderItem(getModItemStack("guardian_lens"), cx - 16, midY);
                drawElementIcon(guiGraphics, Element.HYDRO, cx + 2, midY);
                guiGraphics.drawString(this.font, "➔ §b§lVAPORIZE x2!", cx + 21, midY + 4, 0xFFFFFFFF, true);
            }
            case ELEMENTS -> {
                Element[] all = Element.values();
                int totalW = all.length * 28;
                int sx = cx - totalW / 2 + 4;
                for (int i = 0; i < all.length; i++) {
                    int ex = sx + i * 28;
                    guiGraphics.fill(ex - 2, midY - 2, ex + 18, midY + 18, 0x88101828);
                    ClientAuraRenderer.renderColoredBorder(guiGraphics, ex - 2, midY - 2, 20, 20, ClientAuraRenderer.getElementBorderColor(all[i]));
                    drawElementIcon(guiGraphics, all[i], ex, midY);
                }
            }
            case REACTIONS_AMP -> {
                drawElementIcon(guiGraphics, Element.PYRO, cx - 96, midY);
                guiGraphics.drawString(this.font, "+", cx - 76, midY + 4, 0xFFFFFFFF, true);
                drawElementIcon(guiGraphics, Element.HYDRO, cx - 66, midY);
                guiGraphics.drawString(this.font, "§6= x2.0", cx - 46, midY + 4, 0xFFFFAA00, true);

                guiGraphics.drawString(this.font, "|", cx - 4, midY + 4, 0xFF556688, false);

                drawElementIcon(guiGraphics, Element.HYDRO, cx + 8, midY);
                guiGraphics.drawString(this.font, "+", cx + 28, midY + 4, 0xFFFFFFFF, true);
                drawElementIcon(guiGraphics, Element.CRYO, cx + 38, midY);
                guiGraphics.drawString(this.font, "§b= FROZEN", cx + 58, midY + 4, 0xFF64D2FF, true);
            }
            case REACTIONS_DENDRO -> {
                drawElementIcon(guiGraphics, Element.DENDRO, cx - 104, midY);
                guiGraphics.drawString(this.font, "+", cx - 85, midY + 4, 0xFFFFFFFF, true);
                drawElementIcon(guiGraphics, Element.HYDRO, cx - 76, midY);
                guiGraphics.drawString(this.font, "➔", cx - 56, midY + 4, 0xFF32D74B, true);
                guiGraphics.renderItem(getModItemStack("dendro_core"), cx - 44, midY);
                guiGraphics.drawString(this.font, "➔", cx - 24, midY + 4, 0xFFFFD60A, true);
                drawElementIcon(guiGraphics, Element.PYRO, cx - 10, midY);
                guiGraphics.drawString(this.font, "§6Burgeon", cx + 8, midY - 1, 0xFFFF9F0A, true);
                drawElementIcon(guiGraphics, Element.ELECTRO, cx - 10, midY + 10);
                guiGraphics.drawString(this.font, "§dHyperbloom", cx + 8, midY + 9, 0xFFDA70D6, true);
            }
            case INNATE -> {
                guiGraphics.renderItem(new ItemStack(Items.BLAZE_ROD), cx - 118, midY);
                drawElementIcon(guiGraphics, Element.PYRO, cx - 99, midY);
                guiGraphics.drawString(this.font, "➔ §cIMMUNE", cx - 80, midY + 4, 0xFFFF5555, true);

                guiGraphics.drawString(this.font, "|", cx - 22, midY + 4, 0xFF556688, false);

                drawElementIcon(guiGraphics, Element.HYDRO, cx - 12, midY);
                guiGraphics.drawString(this.font, "➔ §b§lVAPORIZE x2!", cx + 8, midY + 4, 0xFFFFFFFF, true);
            }
            case ENVIRONMENT -> {
                guiGraphics.renderItem(new ItemStack(Items.SCULK_SENSOR), cx - 118, midY);
                guiGraphics.drawString(this.font, "➔", cx - 99, midY + 4, 0xFF00D2D3, true);
                drawElementIcon(guiGraphics, Element.ELECTRO, cx - 88, midY);

                guiGraphics.drawString(this.font, "|", cx - 68, midY + 4, 0xFF556688, false);

                guiGraphics.renderItem(new ItemStack(Items.AMETHYST_CLUSTER), cx - 58, midY);
                drawElementIcon(guiGraphics, Element.PYRO, cx - 40, midY);
                guiGraphics.drawString(this.font, "➔", cx - 22, midY + 4, 0xFFFF8C33, true);
                guiGraphics.renderItem(new ItemStack(Items.AMETHYST_SHARD), cx - 11, midY);

                guiGraphics.drawString(this.font, "|", cx + 9, midY + 4, 0xFF556688, false);

                drawElementIcon(guiGraphics, Element.PYRO, cx + 18, midY);
                guiGraphics.renderItem(new ItemStack(Items.ICE), cx + 36, midY);
                guiGraphics.drawString(this.font, "➔", cx + 54, midY + 4, 0xFF0A84FF, true);
                guiGraphics.renderItem(new ItemStack(Items.WATER_BUCKET), cx + 65, midY);
            }
            case STAVES -> {
                guiGraphics.renderItem(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), cx - 106, midY);
                guiGraphics.drawString(this.font, "+", cx - 87, midY + 4, 0xFFFFFFFF, true);
                guiGraphics.renderItem(getModItemStack("dragonfire_scepter"), cx - 76, midY);
                guiGraphics.drawString(this.font, "+", cx - 57, midY + 4, 0xFFFFFFFF, true);
                guiGraphics.renderItem(new ItemStack(Items.NETHERITE_INGOT), cx - 46, midY);
                guiGraphics.drawString(this.font, "➔", cx - 26, midY + 4, 0xFFFFD60A, true);
                guiGraphics.renderItem(getModItemStack("netherite_dragonfire_scepter"), cx - 14, midY);
                guiGraphics.drawString(this.font, "§6+50% DMG / +50% U", cx + 6, midY + 4, 0xFFFFD60A, true);
            }
            case MASTERY -> {
                guiGraphics.renderItem(new ItemStack(Items.ENCHANTING_TABLE), cx - 110, midY);
                guiGraphics.drawString(this.font, "➔", cx - 91, midY + 4, 0xFFFFD60A, true);
                guiGraphics.renderItem(new ItemStack(Items.ENCHANTED_BOOK), cx - 79, midY);
                guiGraphics.drawString(this.font, "§dEM I–III ➔ +45% Amp / +90% Trans", cx - 59, midY + 4, 0xFFE0B0FF, true);
            }
        }

        return h;
    }

    private void drawLeftFitString(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        int textW = this.font.width(text);
        if (textW <= maxWidth || textW <= 0) {
            guiGraphics.drawString(this.font, text, x, y, color, shadow);
        } else {
            float scale = (float) maxWidth / (float) textW;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y + 4.0F, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(this.font, text, 0, -4, color, shadow);
            guiGraphics.pose().popPose();
        }
    }

    private void drawCenteredFitString(GuiGraphics guiGraphics, String text, int centerX, int y, int maxWidth, int color) {
        int textW = this.font.width(text);
        if (textW <= maxWidth || textW <= 0) {
            guiGraphics.drawCenteredString(this.font, text, centerX, y, color);
        } else {
            float scale = (float) maxWidth / (float) textW;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, y + 4.0F, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(this.font, text, -textW / 2, -4, color, true);
            guiGraphics.pose().popPose();
        }
    }

    private int renderGuideCard(GuiGraphics guiGraphics, GuideCard card, int x, int y, int w) {
        int pad = 6;
        int textW = w - pad * 2 - 4;

        int iconOffset = 6;
        if (card.elem1 != null) iconOffset += 17;
        if (card.elem2 != null) iconOffset += 6 + 17;
        if (!card.item1.isEmpty()) iconOffset += 17;
        if (!card.item2.isEmpty()) iconOffset += 17;
        int titleStartX = x + iconOffset + 2;

        int badgeW = (card.badgeText != null && !card.badgeText.isEmpty()) ? (this.font.width(card.badgeText) + 8) : 0;
        int rightLimitWithBadge = (badgeW > 0) ? (x + w - badgeW - 7) : (x + w - 5);
        int availTitleW = rightLimitWithBadge - titleStartX;
        int fullTitleW = Math.max(80, (x + w - 5) - titleStartX);

        String boldTitle = "§l" + card.title;
        List<FormattedCharSequence> titleLines;
        boolean badgeBelowTitle = false;
        if (badgeW > 0 && availTitleW < 92) {
            titleLines = this.font.split(Component.literal(boldTitle), fullTitleW);
            badgeBelowTitle = true;
        } else {
            titleLines = this.font.split(Component.literal(boldTitle), Math.max(80, availTitleW));
            if (badgeW > 0 && titleLines.size() > 2) {
                titleLines = this.font.split(Component.literal(boldTitle), fullTitleW);
                badgeBelowTitle = true;
            }
        }

        int headerH;
        if (badgeBelowTitle) {
            headerH = Math.max(22, 4 + titleLines.size() * 10 + 16);
        } else if (titleLines.size() <= 1) {
            headerH = 22;
        } else {
            headerH = Math.max(22, 4 + titleLines.size() * 10 + 4);
        }

        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        for (String p : card.paragraphs) {
            wrappedLines.addAll(this.font.split(Component.literal(p), textW));
        }

        int bodyH = wrappedLines.size() * 11 + 6;
        int cardH = headerH + bodyH;

        // Card Background & Uniform 1px Border
        guiGraphics.fill(x, y, x + w, y + cardH, 0xE6141B2D);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, cardH, card.borderColor);
        // Header divider
        guiGraphics.fill(x + 1, y + headerH, x + w - 1, y + headerH + 1, 0x44FFFFFF);

        int iconX = x + 6;
        int iconY = y + 3;

        if (card.elem1 != null) {
            drawElementIcon(guiGraphics, card.elem1, iconX, iconY);
            iconX += 17;
        }
        if (card.elem2 != null) {
            guiGraphics.drawString(this.font, "+", iconX, iconY + 4, 0xFFFFD60A, true);
            iconX += 6;
            drawElementIcon(guiGraphics, card.elem2, iconX, iconY);
            iconX += 17;
        }
        if (!card.item1.isEmpty()) {
            guiGraphics.renderItem(card.item1, iconX, iconY);
            iconX += 17;
        }
        if (!card.item2.isEmpty()) {
            guiGraphics.renderItem(card.item2, iconX, iconY);
            iconX += 17;
        }

        // Card Title (wrapped so it never collides with badge or border)
        int titleY = (titleLines.size() == 1 && !badgeBelowTitle) ? (y + 7) : (y + 4);
        for (FormattedCharSequence tSeq : titleLines) {
            guiGraphics.drawString(this.font, tSeq, titleStartX, titleY, 0xFFFFFFFF, true);
            titleY += 10;
        }

        // Badge on Top-Right of Card (or right below title if header has 4 icons + wide badge)
        if (badgeW > 0) {
            int bx = x + w - badgeW - 5;
            int by = badgeBelowTitle ? (titleY + 1) : (y + 4);
            guiGraphics.fill(bx, by, bx + badgeW, by + 14, 0xAA080B12);
            ClientAuraRenderer.renderColoredBorder(guiGraphics, bx, by, badgeW, 14, card.badgeColor);
            guiGraphics.drawString(this.font, card.badgeText, bx + 4, by + 3, card.badgeColor, true);
        }

        // Wrapped Paragraphs
        int lineY = y + headerH + 5;
        for (FormattedCharSequence seq : wrappedLines) {
            guiGraphics.drawString(this.font, seq, x + pad + 2, lineY, 0xFFD4DEEE, false);
            lineY += 11;
        }

        return cardH;
    }

    private void drawElementIcon(GuiGraphics guiGraphics, Element element, int x, int y) {
        ResourceLocation tex = ELEMENT_ICONS.get(element);
        if (tex != null) {
            RenderSystem.enableBlend();
            guiGraphics.blit(tex, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int winW = Math.min(WIN_W, this.width - 12);
            int winH = Math.min(WIN_H, this.height - 12);
            int left = (this.width - winW) / 2;
            int top = (this.height - winH) / 2;

            // Check Close button
            int closeBtnW = 20;
            int closeBtnX = left + winW - closeBtnW - 4;
            int closeBtnY = top + 4;
            if (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + 18) {
                playClickSound();
                this.onClose();
                return true;
            }

            // Check Config button
            int cfgBtnW = 58;
            int cfgBtnX = closeBtnX - cfgBtnW - 4;
            int cfgBtnY = top + 4;
            if (mouseX >= cfgBtnX && mouseX <= cfgBtnX + cfgBtnW && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 18) {
                playClickSound();
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new VeloriseConfigScreen(this));
                }
                return true;
            }

            // Check Sidebar Tabs
            int sideX = left + 4;
            int sideY = top + HEADER_H + 4;
            int sideH = winH - HEADER_H - 8;
            Tab[] tabs = Tab.values();
            int tabH = Math.min(24, (sideH - 6 - (tabs.length - 1) * 2) / tabs.length);
            for (int i = 0; i < tabs.length; i++) {
                int ty = sideY + 3 + i * (tabH + 2);
                int tx = sideX + 3;
                int tw = SIDEBAR_W - 6;
                if (mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + tabH) {
                    if (this.currentTab != tabs[i]) {
                        this.currentTab = tabs[i];
                        this.scrollOffset = 0.0D;
                        playClickSound();
                    }
                    return true;
                }
            }

            // Check Scrollbar drag start
            int contentX = sideX + SIDEBAR_W + 4;
            int contentW = winW - SIDEBAR_W - 12;
            int sbX = contentX + contentW - 10;
            int sbY = sideY;
            int sbH = winH - HEADER_H - 8;
            if (mouseX >= sbX && mouseX <= sbX + 10 && mouseY >= sbY && mouseY <= sbY + sbH) {
                this.draggingScrollbar = true;
                updateScrollFromMouse(mouseY, sbY + 4, sbH - 8);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar && this.maxScroll > 0) {
            int winH = Math.min(WIN_H, this.height - 12);
            int top = (this.height - winH) / 2;
            int sbY = top + HEADER_H + 8;
            int sbH = winH - HEADER_H - 16;
            updateScrollFromMouse(mouseY, sbY, sbH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY, int sbY, int sbH) {
        if (this.maxScroll <= 0 || sbH <= 0) return;
        double ratio = Math.max(0.0D, Math.min(1.0D, (mouseY - sbY) / (double) sbH));
        this.scrollOffset = ratio * this.maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.maxScroll > 0) {
            this.scrollOffset = Math.max(0.0D, Math.min(this.maxScroll, this.scrollOffset - delta * 22.0D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
