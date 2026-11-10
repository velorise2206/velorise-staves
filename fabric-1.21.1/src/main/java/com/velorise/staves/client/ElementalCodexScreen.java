package com.velorise.staves.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interactive in-game guidebook ("Elemental Codex") with categorized sidebar navigation,
 * element/item icons, live 3D entity renders for innate mobs, visual reaction equations,
 * and live server-synced config multipliers.
 */
public class ElementalCodexScreen extends Screen {
    private static final int WIN_W = 436;
    private static final int WIN_H = 246;
    private static final int SIDEBAR_W = 138;
    private static final int HEADER_H = 26;

    private static final Map<Element, ResourceLocation> ELEMENT_ICONS = new EnumMap<>(Element.class);

    static {
        for (Element e : Element.values()) {
            String lower = e.name().toLowerCase(Locale.ROOT);
            ELEMENT_ICONS.put(e, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/" + lower + ".png"));
        }
    }

    public enum Tab {
        BASICS("basics", 0xFF55EFC4, null),
        ELEMENTS("elements", 0xFFFF6B4A, Element.PYRO),
        REACTIONS_AMP("reactions_amp", 0xFF0A84FF, Element.HYDRO),
        REACTIONS_DENDRO("reactions_dendro", 0xFF32D74B, Element.DENDRO),
        INNATE("innate", 0xFFFF9F0A, null),
        ENVIRONMENT("environment", 0xFF64D2FF, null),
        STAVES("staves", 0xFFBF5AF2, null),
        MASTERY("mastery", 0xFFFFD60A, null);

        public final String key;
        public final int accentColor;
        public final Element iconElement;

        Tab(String key, int accentColor, Element iconElement) {
            this.key = key;
            this.accentColor = accentColor;
            this.iconElement = iconElement;
        }
    }

    private record GuideCard(
            String title,
            int borderColor,
            Element elem1,
            Element elem2,
            List<Element> elemGroup,
            ItemStack item1,
            ItemStack item2,
            EntityType<? extends LivingEntity> mob1,
            EntityType<? extends LivingEntity> mob2,
            String badgeText,
            int badgeColor,
            List<String> paragraphs
    ) {
        GuideCard(
                String title,
                int borderColor,
                Element elem1,
                Element elem2,
                ItemStack item1,
                ItemStack item2,
                String badgeText,
                int badgeColor,
                List<String> paragraphs
        ) {
            this(title, borderColor, elem1, elem2, Collections.emptyList(), item1, item2, null, null, badgeText, badgeColor, paragraphs);
        }

        GuideCard(
                String title,
                int borderColor,
                Element elem1,
                Element elem2,
                EntityType<? extends LivingEntity> mob1,
                EntityType<? extends LivingEntity> mob2,
                String badgeText,
                int badgeColor,
                List<String> paragraphs
        ) {
            this(title, borderColor, elem1, elem2, Collections.emptyList(), ItemStack.EMPTY, ItemStack.EMPTY, mob1, mob2, badgeText, badgeColor, paragraphs);
        }
    }

    private final Screen parent;
    private Tab currentTab = Tab.BASICS;
    private double scrollOffset = 0.0D;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;
    private final Map<EntityType<?>, LivingEntity> dummyMobs = new HashMap<>();

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
        dummyMobs.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private LivingEntity getDummyMob(EntityType<? extends LivingEntity> type) {
        if (type == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        return dummyMobs.computeIfAbsent(type, t -> {
            try {
                Entity e = t.create(mc.level);
                if (e instanceof LivingEntity le) {
                    le.yBodyRot = 210.0F;
                    le.yBodyRotO = 210.0F;
                    le.setYRot(210.0F);
                    le.yRotO = 210.0F;
                    le.yHeadRot = 210.0F;
                    le.yHeadRotO = 210.0F;
                    return le;
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    private void renderGuiMob(GuiGraphics guiGraphics, EntityType<? extends LivingEntity> type, int x, int y, float size) {
        LivingEntity entity = getDummyMob(type);
        if (entity == null) return;
        Minecraft mc = Minecraft.getInstance();
        float maxDim = Math.max(0.85F, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        float scale = size / maxDim;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8.0F, y + 15.0F, 50.0F);
        guiGraphics.pose().scale(scale, scale, -scale);
        Quaternionf rotZ = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf rotX = Axis.XP.rotationDegrees(20.0F);
        rotZ.mul(rotX);
        guiGraphics.pose().mulPose(rotZ);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() ->
                dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0)
        );
        guiGraphics.flush();
        dispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private static ItemStack getModItemStack(String id) {
        try {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, id));
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
            case ENVIRONMENT -> new ItemStack(Items.SCULK_SENSOR);
            case STAVES -> getModItemStack("netherite_dragonfire_scepter");
            case MASTERY -> new ItemStack(Items.ENCHANTED_BOOK);
            default -> ItemStack.EMPTY;
        };
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
                        getModItemStack("inferno_rod"), ItemStack.EMPTY,
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
                        Element.ELECTRO, null, getModItemStack("arc_caster"), getModItemStack("shulker_seeker"),
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
                        Element.ANEMO, null, getModItemStack("wind_staff"), getModItemStack("resonant_echo"),
                        "ANEMO", 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.elem.anemo.p1"), tr("codex.velorise_staves.elem.anemo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.geo.title"), 0xFFFFD60A,
                        Element.GEO, null, getModItemStack("evoker_fang_staff"), ItemStack.EMPTY,
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
                        Element.HYDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
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
                        Element.HYDRO, Element.CRYO, ItemStack.EMPTY, ItemStack.EMPTY,
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
                // Crystallize: Geo + ( Pyro | Hydro | Cryo | Electro ) = Crystallized
                cards.add(new GuideCard(
                        "= Crystallized", 0xFFFFD60A,
                        Element.GEO, null,
                        List.of(Element.PYRO, Element.HYDRO, Element.CRYO, Element.ELECTRO),
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        null, null,
                        "+" + fmt(2.0 * dmgMult) + " Shield HP", 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.react.crystallize.p1"), tr("codex.velorise_staves.react.crystallize.p2"))
                ));
            }
            case INNATE -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.rules.title"), 0xFFFFD60A,
                        Element.PYRO, Element.HYDRO, EntityType.BLAZE, EntityType.CREEPER,
                        "IMMUNE & 6% Mutate", 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.innate.rules.p1"), tr("codex.velorise_staves.innate.rules.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.pyro.title"), 0xFFFF453A,
                        Element.PYRO, null, EntityType.BLAZE, EntityType.MAGMA_CUBE,
                        "1.0U Pyro", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.innate.pyro.p1"), tr("codex.velorise_staves.innate.pyro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.hydro.title"), 0xFF0A84FF,
                        Element.HYDRO, null, EntityType.GUARDIAN, EntityType.DROWNED,
                        "1.0U Hydro", 0xFF0A84FF,
                        List.of(tr("codex.velorise_staves.innate.hydro.p1"), tr("codex.velorise_staves.innate.hydro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.electro.title"), 0xFFBF5AF2,
                        Element.ELECTRO, null, EntityType.ENDERMAN, EntityType.SHULKER,
                        "1.0U–1.5U Electro", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.innate.electro.p1"), tr("codex.velorise_staves.innate.electro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.cryo.title"), 0xFF64D2FF,
                        Element.CRYO, null, EntityType.STRAY, EntityType.SNOW_GOLEM,
                        "1.0U Cryo", 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.innate.cryo.p1"), tr("codex.velorise_staves.innate.cryo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.dendro.title"), 0xFF32D74B,
                        Element.DENDRO, null, EntityType.CAVE_SPIDER, EntityType.SLIME,
                        "1.0U Dendro", 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.innate.dendro.p1"), tr("codex.velorise_staves.innate.dendro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.anemo_geo.title"), 0xFF55EFC4,
                        Element.ANEMO, Element.GEO, EntityType.WARDEN, EntityType.IRON_GOLEM,
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
                Object[][] staffDefs = new Object[][]{
                        {"inferno_rod", ArcaneStaffItem.Kind.INFERNO, Element.PYRO,
                                fmt(3.0 * dmgMult) + "x3", fmt(4.5 * dmgMult) + "x3", "30m", "codex.velorise_staves.staff.inferno"},
                        {"ghastfire_staff", ArcaneStaffItem.Kind.GHASTFIRE, Element.PYRO,
                                fmt(6.0 * dmgMult), fmt(9.0 * dmgMult), "36m (AoE)", "codex.velorise_staves.staff.ghastfire"},
                        {"guardian_lens", ArcaneStaffItem.Kind.GUARDIAN, Element.HYDRO,
                                fmt(0.5 * dmgMult) + "–" + fmt(1.5 * dmgMult) + "x10",
                                fmt(0.8 * dmgMult) + "–" + fmt(2.0 * dmgMult) + "x10", "24m➔28m", "codex.velorise_staves.staff.guardian"},
                        {"frostbolt_wand", ArcaneStaffItem.Kind.FROST, Element.CRYO,
                                fmt(3.0 * dmgMult) + "x3", fmt(4.5 * dmgMult) + "x3", "28m", "codex.velorise_staves.staff.frostbolt"},
                        {"arc_caster", ArcaneStaffItem.Kind.LIGHTNING, Element.ELECTRO,
                                fmt(0.5 * dmgMult) + "/hit", fmt(1.0 * dmgMult) + "/hit", "30m➔36m", "codex.velorise_staves.staff.arc"},
                        {"dragonfire_scepter", ArcaneStaffItem.Kind.DRAGONFIRE, Element.ELECTRO,
                                fmt(7.0 * dmgMult) + "+Cloud", fmt(10.0 * dmgMult) + "+Cloud", "32m (AoE)", "codex.velorise_staves.staff.dragonfire"},
                        {"shulker_seeker", ArcaneStaffItem.Kind.SHULKER, Element.ELECTRO,
                                fmt(4.0 * dmgMult) + "x2", fmt(6.0 * dmgMult) + "x2", "30m", "codex.velorise_staves.staff.shulker"},
                        {"wither_scepter", ArcaneStaffItem.Kind.WITHER, Element.DENDRO,
                                fmt(4.0 * dmgMult) + "x3", fmt(6.0 * dmgMult) + "x3", "32m (AoE)", "codex.velorise_staves.staff.wither"},
                        {"webcaster", ArcaneStaffItem.Kind.WEB, Element.DENDRO,
                                "Trap+Poison", "Trap+Poison", "28m", "codex.velorise_staves.staff.webcaster"},
                        {"wind_staff", ArcaneStaffItem.Kind.WIND, Element.ANEMO,
                                fmt(1.5 * dmgMult), fmt(2.5 * dmgMult), "28m (AoE)", "codex.velorise_staves.staff.wind"},
                        {"resonant_echo", ArcaneStaffItem.Kind.ECHO, Element.ANEMO,
                                fmt(11.0 * dmgMult), fmt(16.0 * dmgMult), "28m➔32m", "codex.velorise_staves.staff.echo"},
                        {"evoker_fang_staff", ArcaneStaffItem.Kind.EVOKER, Element.GEO,
                                fmt(3.0 * dmgMult) + "x9", fmt(4.5 * dmgMult) + "x13", "13.5m➔19.5m", "codex.velorise_staves.staff.evoker"}
                };
                for (Object[] def : staffDefs) {
                    String baseId = (String) def[0];
                    ArcaneStaffItem.Kind kind = (ArcaneStaffItem.Kind) def[1];
                    Element el = (Element) def[2];
                    String bDmg = (String) def[3];
                    String nDmg = (String) def[4];
                    String rngStr = (String) def[5];
                    String descKey = (String) def[6];

                    double cdSec = StavesConfig.getStaffCooldownSeconds(kind) * cdMult;
                    int bDur = StavesConfig.getMaxDurability(kind, false);
                    int nDur = StavesConfig.getMaxDurability(kind, true);

                    ItemStack basicStack = getModItemStack(baseId);
                    String name = tr("item.velorise_staves." + baseId);
                    String stats = String.format(Locale.ROOT, "§eDMG:§r %s ➔ §6%s§r | §bCD:§r %.1fs | §dRng:§r %s | §7Dur: %d/%d",
                            bDmg, nDmg, cdSec, rngStr, bDur, nDur);
                    cards.add(new GuideCard(
                            name, ClientAuraRenderer.getElementBorderColor(el),
                            el, null, basicStack, ItemStack.EMPTY,
                            el.name(), ClientAuraRenderer.getElementBorderColor(el),
                            List.of(stats, tr(descKey))
                    ));
                }
                // Utility Artifacts
                double vBasic = StavesConfig.getVoidTranslocatorRange(false);
                double vNeth = StavesConfig.getVoidTranslocatorRange(true);
                double mCd = StavesConfig.getMagicMirrorCooldownSeconds() * cdMult;
                cards.add(new GuideCard(
                        tr("item.velorise_staves.void_translocator") + " & " + tr("item.velorise_staves.magic_mirror"),
                        0xFFDA70D6, null, null,
                        getModItemStack("void_translocator"), getModItemStack("magic_mirror"),
                        "UTILITY", 0xFFDA70D6,
                        List.of(
                                tr("codex.velorise_staves.staff.utility.p1", fmt(vBasic), fmt(vNeth)),
                                tr("codex.velorise_staves.staff.utility.p2", fmt(mCd))
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
                                String.format(Locale.ROOT, "• §eDamage Multiplier:§r §a%.2fx§r | §bCooldown Multiplier:§r §a%.2fx§r",
                                        StavesConfig.getDamageMultiplier(), StavesConfig.getCooldownMultiplier()),
                                String.format(Locale.ROOT, "• §dElemental Reactions (EGT):§r %s | §6Mutation Rate:§r §e%d%%§r",
                                        StavesConfig.isElementalGaugeEnabled() ? "§aON§r" : "§cOFF§r",
                                        Math.round(StavesConfig.INNATE_MUTATION_CHANCE.get() * 100.0)),
                                tr("codex.velorise_staves.mastery.card3.p1")
                        )
                ));
            }
        }
        return cards;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

        // Header Right Buttons: [Config] and [✕]
        int closeBtnW = 20;
        int closeBtnX = left + winW - closeBtnW - 4;
        int closeBtnY = top + 4;
        boolean hoverClose = mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + 18;
        guiGraphics.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + 18, hoverClose ? 0xFF882222 : 0xFF2B1B24);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, closeBtnX, closeBtnY, closeBtnW, 18, hoverClose ? 0xFFFF5555 : 0xFF773333);
        guiGraphics.drawCenteredString(this.font, "✕", closeBtnX + closeBtnW / 2, closeBtnY + 5, 0xFFFFFFFF);

        int cfgBtnW = 48;
        int cfgBtnX = closeBtnX - cfgBtnW - 4;
        int cfgBtnY = top + 4;
        boolean hoverCfg = mouseX >= cfgBtnX && mouseX <= cfgBtnX + cfgBtnW && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 18;
        guiGraphics.fill(cfgBtnX, cfgBtnY, cfgBtnX + cfgBtnW, cfgBtnY + 18, hoverCfg ? 0xFF253B5E : 0xFF18243A);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, cfgBtnX, cfgBtnY, cfgBtnW, 18, hoverCfg ? 0xFF64D2FF : 0xFF3A5A88);
        guiGraphics.drawCenteredString(this.font, "Config", cfgBtnX + cfgBtnW / 2, cfgBtnY + 5, 0xFFE0F2FF);

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

            // Tab Icon (16x16 Element texture, 3D Blaze entity render, or ItemStack)
            int iconOffsetY = ty + (tabH - 16) / 2;
            if (tab.iconElement != null) {
                drawElementIcon(guiGraphics, tab.iconElement, tx + 5, iconOffsetY);
            } else if (tab == Tab.INNATE) {
                renderGuiMob(guiGraphics, EntityType.BLAZE, tx + 5, iconOffsetY, 12.5F);
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

        // 4A. Render Visual Diagram Showcase Box at top of tab
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
        int h = 48;
        guiGraphics.fill(x, y, x + w, y + h, 0xFF131A2C);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, h, tab.accentColor);

        String diagramTitle = tr("codex.velorise_staves.diagram." + tab.key);
        drawCenteredFitString(guiGraphics, "§e✦ " + diagramTitle + " ✦", x + w / 2, y + 6, w - 10, 0xFFFFE082);

        int midY = y + 23;
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
                int sx = cx - 114;
                drawElementIcon(guiGraphics, Element.DENDRO, sx, midY);
                guiGraphics.drawString(this.font, "+", sx + 18, midY + 4, 0xFFFFFFFF, true);
                drawElementIcon(guiGraphics, Element.HYDRO, sx + 26, midY);
                guiGraphics.drawString(this.font, "➔", sx + 45, midY + 4, 0xFF32D74B, true);
                guiGraphics.renderItem(getModItemStack("dendro_core"), sx + 55, midY);
                guiGraphics.drawString(this.font, "➔", sx + 74, midY + 4, 0xFFFFD60A, true);
                drawElementIcon(guiGraphics, Element.PYRO, sx + 85, midY);
                guiGraphics.drawString(this.font, "§6Burgeon", sx + 103, midY + 4, 0xFFFF9F0A, true);
                drawElementIcon(guiGraphics, Element.ELECTRO, sx + 150, midY);
                guiGraphics.drawString(this.font, "§dHyperbloom", sx + 168, midY + 4, 0xFFDA70D6, true);
            }
            case INNATE -> {
                renderGuiMob(guiGraphics, EntityType.BLAZE, cx - 118, midY, 13.0F);
                drawElementIcon(guiGraphics, Element.PYRO, cx - 99, midY);
                guiGraphics.drawString(this.font, "➔ §cIMMUNE", cx - 80, midY + 4, 0xFFFF5555, true);

                guiGraphics.drawString(this.font, "|", cx - 22, midY + 4, 0xFF556688, false);

                renderGuiMob(guiGraphics, EntityType.BLAZE, cx - 12, midY, 13.0F);
                drawElementIcon(guiGraphics, Element.HYDRO, cx + 6, midY);
                guiGraphics.drawString(this.font, "➔ §b§lVAPORIZE x2!", cx + 25, midY + 4, 0xFFFFFFFF, true);
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
        if (card.elemGroup != null && !card.elemGroup.isEmpty()) {
            iconOffset += 6 + 4 + card.elemGroup.size() * 16 + (card.elemGroup.size() - 1) * 4 + 5;
        }
        if (!card.item1.isEmpty()) iconOffset += 17;
        if (!card.item2.isEmpty()) iconOffset += 17;
        if (card.mob1 != null) iconOffset += 18;
        if (card.mob2 != null) iconOffset += 18;
        int titleStartX = x + iconOffset + 2;

        int badgeW = (card.badgeText != null && !card.badgeText.isEmpty()) ? (this.font.width(card.badgeText) + 8) : 0;
        int rightLimitWithBadge = (badgeW > 0) ? (x + w - badgeW - 7) : (x + w - 5);
        int availTitleW = rightLimitWithBadge - titleStartX;
        int fullTitleW = Math.max(60, (x + w - 5) - titleStartX);

        String boldTitle = "§l" + card.title;
        List<FormattedCharSequence> titleLines;
        boolean badgeBelowTitle = false;
        if (badgeW > 0 && availTitleW < 65) {
            titleLines = this.font.split(Component.literal(boldTitle), fullTitleW);
            badgeBelowTitle = true;
        } else {
            titleLines = this.font.split(Component.literal(boldTitle), Math.max(60, availTitleW));
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
        if (card.elemGroup != null && !card.elemGroup.isEmpty()) {
            guiGraphics.drawString(this.font, "+", iconX, iconY + 4, 0xFFFFD60A, true);
            iconX += 6;
            guiGraphics.drawString(this.font, "(", iconX, iconY + 4, 0xFFA0B0C8, true);
            iconX += 4;
            for (int i = 0; i < card.elemGroup.size(); i++) {
                drawElementIcon(guiGraphics, card.elemGroup.get(i), iconX, iconY);
                iconX += 16;
                if (i < card.elemGroup.size() - 1) {
                    guiGraphics.drawString(this.font, "|", iconX + 1, iconY + 4, 0xFF708098, false);
                    iconX += 4;
                }
            }
            guiGraphics.drawString(this.font, ")", iconX + 1, iconY + 4, 0xFFA0B0C8, true);
            iconX += 5;
        }
        if (!card.item1.isEmpty()) {
            guiGraphics.renderItem(card.item1, iconX, iconY);
            iconX += 17;
        }
        if (!card.item2.isEmpty()) {
            guiGraphics.renderItem(card.item2, iconX, iconY);
            iconX += 17;
        }
        if (card.mob1 != null) {
            renderGuiMob(guiGraphics, card.mob1, iconX, iconY, 12.5F);
            iconX += 18;
        }
        if (card.mob2 != null) {
            renderGuiMob(guiGraphics, card.mob2, iconX, iconY, 12.5F);
            iconX += 18;
        }

        // Card Title
        int titleY = (titleLines.size() == 1 && !badgeBelowTitle) ? (y + 7) : (y + 4);
        for (FormattedCharSequence tSeq : titleLines) {
            guiGraphics.drawString(this.font, tSeq, titleStartX, titleY, 0xFFFFFFFF, true);
            titleY += 10;
        }

        // Badge on Top-Right of Card
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
            int cfgBtnW = 48;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll > 0) {
            this.scrollOffset = Math.max(0.0D, Math.min(this.maxScroll, this.scrollOffset - scrollY * 22.0D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
