package com.velorise.staves.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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
    private static final ResourceLocation OMNI_ICON =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni.png");
    public static final ResourceLocation RESONANCE_ICON =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/resonance.png");
    public static final ResourceLocation MOONSIGN_ICON =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/moonsign.png");

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
        MASTERY("mastery", 0xFFFFD60A, null),
        RESONANCE("resonance", 0xFF40E0D0, null);

        public final String key;
        public final int accentColor;
        public final Element iconElement;

        Tab(String key, int accentColor, Element iconElement) {
            this.key = key;
            this.accentColor = accentColor;
            this.iconElement = iconElement;
        }
    }

    private static final String LINK_MARKER_PREFIX = "vl_link:";

    private record ReactionLinkSpec(Tab tab, String cardKey, String[] reactionKeys, String[] enFallbacks) {}
    private record ReactionLink(String keyword, boolean cjk, Tab tab, String cardKey) {}

    private static final List<ReactionLinkSpec> REACTION_LINK_SPECS = List.of(
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "overvape", new String[]{"overvape"}, new String[]{"overvape"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "vaporize", new String[]{"vaporize"}, new String[]{"vaporize"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "melt", new String[]{"melt"}, new String[]{"melt"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "overloaded", new String[]{"overloaded"}, new String[]{"overloaded", "overload"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "electro_charged", new String[]{"electro_charged"}, new String[]{"electro-charged"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "frozen", new String[]{"frozen", "shatter"}, new String[]{"frozen", "freeze", "shatters", "shatter"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "superconduct", new String[]{"superconduct"}, new String[]{"superconduct"}),
            new ReactionLinkSpec(Tab.REACTIONS_AMP, "swirl", new String[]{"swirl"}, new String[]{"swirl"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "hyperbloom", new String[]{"hyperbloom"}, new String[]{"hyperbloom"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "burgeon", new String[]{"burgeon"}, new String[]{"burgeon"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "bloom", new String[]{"bloom"}, new String[]{"bloom", "dendro core", "dendro cores", "hạt nhân thảo"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "catalyze", new String[]{"quicken", "aggravate", "spread"}, new String[]{"catalyze", "quicken", "quickened", "aggravate", "spread"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "burning", new String[]{"burning"}, new String[]{"burning"}),
            new ReactionLinkSpec(Tab.REACTIONS_DENDRO, "crystallize", new String[]{"crystallize"}, new String[]{"crystallize", "crystallized", "crystallize shield", "crystallize shards", "mảnh khiên kết tinh", "mảnh kết tinh", "khiên kết tinh", "khiên tim vàng"}),
            new ReactionLinkSpec(Tab.RESONANCE, "tab_resonance", new String[]{}, new String[]{"elemental resonance", "resonance", "cộng hưởng nguyên tố", "cộng hưởng"}),
            new ReactionLinkSpec(Tab.MASTERY, "mastery_em", new String[]{}, new String[]{"elemental mastery", "tinh thông nguyên tố", "ttnt"}),
            new ReactionLinkSpec(Tab.MASTERY, "mastery_prot", new String[]{}, new String[]{"elemental protection", "bảo vệ nguyên tố"}),
            new ReactionLinkSpec(Tab.INNATE, "innate_rules", new String[]{}, new String[]{"tab 5", "mục 5", "quái bẩm sinh", "innate mobs", "omni mutated", "đột biến omni"}),
            new ReactionLinkSpec(Tab.INNATE, "innate_drops", new String[]{}, new String[]{"omni element", "nguyên tố omni"}),
            new ReactionLinkSpec(Tab.ENVIRONMENT, "env_sculk", new String[]{}, new String[]{"calibrated sculk sensor", "sculk sensor", "sculk sensors", "cảm biến sculk hiệu chỉnh", "cảm biến sculk"}),
            new ReactionLinkSpec(Tab.ENVIRONMENT, "env_redstone", new String[]{}, new String[]{"tesla coil", "tesla coils", "cuộn tesla", "cột thu lôi"}),
            new ReactionLinkSpec(Tab.ENVIRONMENT, "env_amethyst", new String[]{}, new String[]{"amethyst cluster", "amethyst clusters", "cụm thạch anh tím", "hốc thạch anh tím"}),
            new ReactionLinkSpec(Tab.STAVES, "omni_staff", new String[]{}, new String[]{"omni staff", "netherite omni staff", "quyền trượng vạn năng", "trượng omni"}),
            new ReactionLinkSpec(Tab.STAVES, "vortex_vanquisher", new String[]{}, new String[]{"vortex vanquisher", "giáo nịnh thần", "stone stele", "vách đá", "jade shield", "khiên ngọc"}),
            new ReactionLinkSpec(Tab.STAVES, "splendor_of_tranquil_waters", new String[]{}, new String[]{"splendor of tranquil waters", "ánh nước tĩnh lặng", "salon solitaire", "salon cô độc", "singer of many waters", "ca sĩ nước nguồn"}),
            new ReactionLinkSpec(Tab.STAVES, "guardian_lens", new String[]{}, new String[]{"guardian lens", "thấu kính hộ vệ", "sourcewater droplets", "sourcewater", "nguồn nước trong", "vinh quang rồng"}),
            new ReactionLinkSpec(Tab.STAVES, "evoker_fang_staff", new String[]{}, new String[]{"evoker fang staff", "gậy nanh ma quái", "geo fang pillar", "trụ nanh nham"}),
            new ReactionLinkSpec(Tab.STAVES, "utility_relics", new String[]{}, new String[]{"void translocator", "magic mirror", "bộ dịch chuyển hư không", "gương ma thuật"})
    );

    private static boolean containsCjkOrHangul(String s) {
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    private static void registerLinkWord(Map<String, ReactionLink> dedup, String word, Tab tab, String cardKey) {
        if (word == null) return;
        String clean = word.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) return;
        dedup.putIfAbsent(clean, new ReactionLink(clean, containsCjkOrHangul(clean), tab, cardKey));
    }

    private static List<ReactionLink> buildReactionLinksForActiveLanguage() {
        Map<String, ReactionLink> dedup = new LinkedHashMap<>();
        for (ReactionLinkSpec spec : REACTION_LINK_SPECS) {
            String linksKey = "codex.velorise_staves.links." + spec.cardKey();
            if (I18n.exists(linksKey)) {
                for (String token : I18n.get(linksKey).split("\\|")) {
                    registerLinkWord(dedup, token, spec.tab(), spec.cardKey());
                }
            }
            for (String rKey : spec.reactionKeys()) {
                String trKey = "reaction.velorise_staves." + rKey;
                if (I18n.exists(trKey)) {
                    registerLinkWord(dedup, I18n.get(trKey), spec.tab(), spec.cardKey());
                }
            }
            for (String fb : spec.enFallbacks()) {
                registerLinkWord(dedup, fb, spec.tab(), spec.cardKey());
            }
        }
        // Automatically register localized item names for all 15 Staves, Utility Relics, Dendro Core, Omni Element & Enchantments
        String[] allStaffIds = {
                "inferno_rod", "ghastfire_staff", "guardian_lens", "splendor_of_tranquil_waters",
                "frostbolt_wand", "arc_caster", "dragonfire_scepter", "shulker_seeker",
                "wither_scepter", "webcaster", "wind_staff", "resonant_echo",
                "evoker_fang_staff", "vortex_vanquisher", "omni_staff"
        };
        for (String sId : allStaffIds) {
            if (I18n.exists("item.velorise_staves." + sId)) {
                registerLinkWord(dedup, I18n.get("item.velorise_staves." + sId), Tab.STAVES, sId);
            }
            if (I18n.exists("item.velorise_staves.netherite_" + sId)) {
                registerLinkWord(dedup, I18n.get("item.velorise_staves.netherite_" + sId), Tab.STAVES, sId);
            }
        }
        for (String uId : new String[]{"void_translocator", "netherite_void_translocator", "magic_mirror", "netherite_magic_mirror"}) {
            if (I18n.exists("item.velorise_staves." + uId)) {
                registerLinkWord(dedup, I18n.get("item.velorise_staves." + uId), Tab.STAVES, "utility_relics");
            }
        }
        if (I18n.exists("item.velorise_staves.dendro_core")) {
            registerLinkWord(dedup, I18n.get("item.velorise_staves.dendro_core"), Tab.REACTIONS_DENDRO, "bloom");
        }
        if (I18n.exists("item.velorise_staves.omni_element")) {
            registerLinkWord(dedup, I18n.get("item.velorise_staves.omni_element"), Tab.INNATE, "innate_drops");
        }
        if (I18n.exists("enchantment.velorise_staves.elemental_mastery")) {
            registerLinkWord(dedup, I18n.get("enchantment.velorise_staves.elemental_mastery"), Tab.MASTERY, "mastery_em");
        }
        if (I18n.exists("enchantment.velorise_staves.elemental_protection")) {
            registerLinkWord(dedup, I18n.get("enchantment.velorise_staves.elemental_protection"), Tab.MASTERY, "mastery_prot");
        }
        for (Element el : Element.values()) {
            String lowerEl = el.name().toLowerCase(Locale.ROOT);
            String targetCard = "elem_" + lowerEl;
            registerLinkWord(dedup, lowerEl, Tab.ELEMENTS, targetCard);
            if (I18n.exists(el.translationKey())) {
                String trEl = I18n.get(el.translationKey()).trim();
                if (containsCjkOrHangul(trEl) && trEl.length() == 1) {
                    registerLinkWord(dedup, trEl + "元素", Tab.ELEMENTS, targetCard);
                } else {
                    registerLinkWord(dedup, trEl, Tab.ELEMENTS, targetCard);
                }
            }
        }
        List<ReactionLink> sorted = new ArrayList<>(dedup.values());
        sorted.sort(Comparator.comparingInt((ReactionLink l) -> l.keyword().length()).reversed());
        return List.copyOf(sorted);
    }

    private record GuideCard(
            String cardKey,
            String title,
            int borderColor,
            Element elem1,
            Element elem2,
            List<Element> elemGroup,
            boolean dynamicElemGroup,
            boolean plusBeforeItem1,
            boolean omniElem1,
            ResourceLocation customIcon,
            String customIconTitleKey,
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
            this(null, title, borderColor, elem1, elem2, Collections.emptyList(), false, false, false, null, null, item1, item2, null, null, badgeText, badgeColor, paragraphs);
        }

        GuideCard(
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
            this(null, title, borderColor, elem1, elem2, elemGroup, false, false, false, null, null, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
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
            this(null, title, borderColor, elem1, elem2, Collections.emptyList(), false, false, false, null, null, ItemStack.EMPTY, ItemStack.EMPTY, mob1, mob2, badgeText, badgeColor, paragraphs);
        }

        GuideCard withKey(String key) {
            return new GuideCard(key, title, borderColor, elem1, elem2, elemGroup, dynamicElemGroup, plusBeforeItem1, omniElem1, customIcon, customIconTitleKey, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
        }

        GuideCard withDynamicElemGroup() {
            return new GuideCard(cardKey, title, borderColor, elem1, elem2, elemGroup, true, plusBeforeItem1, omniElem1, customIcon, customIconTitleKey, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
        }

        GuideCard withPlusItem1() {
            return new GuideCard(cardKey, title, borderColor, elem1, elem2, elemGroup, dynamicElemGroup, true, omniElem1, customIcon, customIconTitleKey, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
        }

        GuideCard withOmniIcon() {
            return new GuideCard(cardKey, title, borderColor, elem1, elem2, elemGroup, dynamicElemGroup, plusBeforeItem1, true, customIcon, customIconTitleKey, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
        }

        GuideCard withCustomIcon(ResourceLocation icon, String tooltipKey) {
            return new GuideCard(cardKey, title, borderColor, elem1, elem2, elemGroup, dynamicElemGroup, plusBeforeItem1, omniElem1, icon, tooltipKey, item1, item2, mob1, mob2, badgeText, badgeColor, paragraphs);
        }
    }

    private final Screen parent;
    private Tab currentTab = Tab.BASICS;
    private double scrollOffset = 0.0D;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;
    private final Map<EntityType<?>, LivingEntity> dummyMobs = new HashMap<>();
    private ItemStack pendingTooltipItem = ItemStack.EMPTY;
    private List<Component> pendingTooltipLines = null;
    private boolean canTriggerContentHover = false;
    private Tab pendingClickTab = null;
    private String pendingClickCardKey = null;
    private String cachedLinkLang = null;
    private List<ReactionLink> cachedReactionLinks = Collections.emptyList();
    private final Map<Tab, CachedTabContent> tabCache = new EnumMap<>(Tab.class);
    private long lastDynamicGroupCycle = -1L;
    private List<Element> dynamicGroupElements = List.of(Element.PYRO, Element.HYDRO, Element.ANEMO, Element.DENDRO);

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

    public static float getClientPlayerMaxHp() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getMaxHealth() : 20.0F;
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

    private static String fmtHearts(double hp) {
        double hearts = hp / 2.0D;
        double rounded = Math.round(hearts * 100.0D) / 100.0D;
        if (Math.abs(rounded - Math.round(rounded)) < 0.005D) {
            return String.valueOf(Math.round(rounded));
        }
        double tenths = Math.round(rounded * 10.0D) / 10.0D;
        if (Math.abs(rounded - tenths) < 0.005D) {
            return String.format(Locale.ROOT, "%.1f", tenths);
        }
        return String.format(Locale.ROOT, "%.2f", rounded);
    }

    private static String elemTitle(Element e) {
        return tr(e.translationKey());
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
                        tr("codex.velorise_staves.badge.basics1"), 0xFF55EFC4,
                        List.of(
                                tr("codex.velorise_staves.basics.card1.p1"),
                                tr("codex.velorise_staves.basics.card1.p2"),
                                tr("codex.velorise_staves.basics.card1.p3")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card2.title"),
                        0xFFFFD60A,
                        null, null,
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        "1.0U / 1.5U / 2.0U", 0xFFFFD60A,
                        List.of(
                                tr("codex.velorise_staves.basics.card2.p1"),
                                tr("codex.velorise_staves.basics.card2.p2")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card3.title"),
                        0xFFBF5AF2,
                        null, null,
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.basics3"), 0xFFBF5AF2,
                        List.of(
                                tr("codex.velorise_staves.basics.card3.p1"),
                                tr("codex.velorise_staves.basics.card3.p2")
                        )
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.basics.card4.title"),
                        0xFFFFD60A,
                        null, null,
                        new ItemStack(Items.NETHERITE_SWORD), getModItemStack("omni_element"),
                        tr("codex.velorise_staves.badge.sword_imbue"), 0xFFFFD60A,
                        List.of(
                                tr("codex.velorise_staves.basics.card4.p1"),
                                tr("codex.velorise_staves.basics.card4.p2"),
                                tr("codex.velorise_staves.basics.card4.p3")
                        )
                ));
            }
            case ELEMENTS -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.pyro.title"), 0xFFFF453A,
                        Element.PYRO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.PYRO).toUpperCase(Locale.ROOT), 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.elem.pyro.p1"), tr("codex.velorise_staves.elem.pyro.p2"))
                ).withKey("elem_pyro"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.hydro.title"), 0xFF0A84FF,
                        Element.HYDRO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.HYDRO).toUpperCase(Locale.ROOT), 0xFF0A84FF,
                        List.of(tr("codex.velorise_staves.elem.hydro.p1"), tr("codex.velorise_staves.elem.hydro.p2"))
                ).withKey("elem_hydro"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.anemo.title"), 0xFF55EFC4,
                        Element.ANEMO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.ANEMO).toUpperCase(Locale.ROOT), 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.elem.anemo.p1"), tr("codex.velorise_staves.elem.anemo.p2"))
                ).withKey("elem_anemo"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.electro.title"), 0xFFBF5AF2,
                        Element.ELECTRO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.ELECTRO).toUpperCase(Locale.ROOT), 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.elem.electro.p1"), tr("codex.velorise_staves.elem.electro.p2"))
                ).withKey("elem_electro"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.dendro.title"), 0xFF32D74B,
                        Element.DENDRO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.DENDRO).toUpperCase(Locale.ROOT), 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.elem.dendro.p1"), tr("codex.velorise_staves.elem.dendro.p2"))
                ).withKey("elem_dendro"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.cryo.title"), 0xFF64D2FF,
                        Element.CRYO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.CRYO).toUpperCase(Locale.ROOT), 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.elem.cryo.p1"), tr("codex.velorise_staves.elem.cryo.p2"))
                ).withKey("elem_cryo"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.elem.geo.title"), 0xFFFFD60A,
                        Element.GEO, null, ItemStack.EMPTY, ItemStack.EMPTY,
                        elemTitle(Element.GEO).toUpperCase(Locale.ROOT), 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.elem.geo.p1"), tr("codex.velorise_staves.elem.geo.p2"))
                ).withKey("elem_geo"));
            }
            case REACTIONS_AMP -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.vaporize.title"), 0xFFFF8C33,
                        Element.PYRO, Element.HYDRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "x2.0 / x1.5", 0xFFFF8C33,
                        List.of(tr("codex.velorise_staves.react.vaporize.p1"), tr("codex.velorise_staves.react.vaporize.p2"))
                ).withKey("vaporize"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.melt.title"), 0xFFFF3B30,
                        Element.CRYO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "x2.0 / x1.5", 0xFFFF3B30,
                        List.of(tr("codex.velorise_staves.react.melt.p1"), tr("codex.velorise_staves.react.melt.p2"))
                ).withKey("melt"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.overvape.title"), 0xFFFF6B4A,
                        Element.HYDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.overvape", fmtHearts(6.0 * dmgMult) + "§f\uE008§r"), 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.react.overvape.p1"))
                ).withKey("overvape"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.overloaded.title"), 0xFFFF6B4A,
                        Element.PYRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.overloaded", fmtHearts(6.0 * dmgMult) + "§f\uE008§r"), 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.react.overloaded.p1"))
                ).withKey("overloaded"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.electro_charged.title"), 0xFFBF5AF2,
                        Element.HYDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmtHearts(1.0 * dmgMult) + "§f\uE008§r/s (5.0m)", 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.react.electro_charged.p1"))
                ).withKey("electro_charged"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.frozen.title"), 0xFF64D2FF,
                        Element.HYDRO, Element.CRYO, ItemStack.EMPTY, ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.frozen", fmtHearts(10.0 * dmgMult) + "§f\uE008§r"), 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.react.frozen.p1"), tr("codex.velorise_staves.react.frozen.p2"))
                ).withKey("frozen"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.superconduct.title"), 0xFF5AC8FA,
                        Element.CRYO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.superconduct", fmtHearts(1.0 * dmgMult) + "§f\uE008§r"), 0xFF5AC8FA,
                        List.of(tr("codex.velorise_staves.react.superconduct.p1"))
                ).withKey("superconduct"));
                // Swirl: Anemo + ( Pyro | Hydro | Cryo | Electro )
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.swirl.title"), 0xFF55EFC4,
                        Element.ANEMO, null,
                        List.of(Element.PYRO, Element.HYDRO, Element.CRYO, Element.ELECTRO),
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        null, null,
                        tr("codex.velorise_staves.badge.swirl", fmtHearts(4.0 * dmgMult) + "§f\uE008§r"), 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.react.swirl.p1"))
                ).withKey("swirl"));
            }
            case REACTIONS_DENDRO -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.bloom.title"), 0xFF32D74B,
                        Element.DENDRO, Element.HYDRO, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.bloom", fmtHearts(4.0 * dmgMult) + "§f\uE008§r"), 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.react.bloom.p1"))
                ).withKey("bloom"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.burgeon.title"), 0xFFFF9F0A,
                        Element.PYRO, null, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.burgeon", fmtHearts(8.0 * dmgMult) + "§f\uE008§r"), 0xFFFF9F0A,
                        List.of(tr("codex.velorise_staves.react.burgeon.p1"))
                ).withKey("burgeon").withPlusItem1());
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.hyperbloom.title"), 0xFFDA70D6,
                        Element.ELECTRO, null, getModItemStack("dendro_core"), ItemStack.EMPTY,
                        tr("codex.velorise_staves.badge.hyperbloom", fmtHearts(8.0 * dmgMult) + "§f\uE008§r"), 0xFFDA70D6,
                        List.of(tr("codex.velorise_staves.react.hyperbloom.p1"))
                ).withKey("hyperbloom").withPlusItem1());
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.catalyze.title"), 0xFFA8E063,
                        Element.DENDRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        "+" + fmtHearts(1.0 * dmgMult) + "§f\uE008§r / +" + fmtHearts(1.25 * dmgMult) + "§f\uE008§r", 0xFFA8E063,
                        List.of(tr("codex.velorise_staves.react.catalyze.p1"), tr("codex.velorise_staves.react.catalyze.p2"))
                ).withKey("catalyze"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.burning.title"), 0xFFFF453A,
                        Element.DENDRO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                        fmtHearts(3.0 * dmgMult) + "§f\uE008§r / s", 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.react.burning.p1"))
                ).withKey("burning"));
                // Crystallize: Geo + ( Pyro | Hydro | Cryo | Electro ) Crystallized
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.react.crystallize.title"), 0xFFFFD60A,
                        Element.GEO, null,
                        List.of(Element.PYRO, Element.HYDRO, Element.CRYO, Element.ELECTRO),
                        getModItemStack("pyro_crystallize_shard"), ItemStack.EMPTY,
                        null, null,
                        "§f\uE009§r +" + fmtHearts(2.0), 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.react.crystallize.p1"), tr("codex.velorise_staves.react.crystallize.p2"))
                ).withKey("crystallize"));
            }
            case INNATE -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.rules.title"), 0xFFFFD60A,
                        null, null, EntityType.BLAZE, EntityType.CREEPER,
                        tr("codex.velorise_staves.badge.innate_rules"), 0xFFFFD60A,
                        List.of(tr("codex.velorise_staves.innate.rules.p1"), tr("codex.velorise_staves.innate.rules.p2"))
                ).withKey("innate_rules"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.drops.title"), 0xFFFF9F0A,
                        null, null, getModItemStack("pyro_element"), getModItemStack("omni_element"),
                        "5% / 15% / 25%", 0xFFFF9F0A,
                        List.of(
                                tr("codex.velorise_staves.innate.drops.p1"),
                                tr("codex.velorise_staves.innate.drops.p2"),
                                tr("codex.velorise_staves.innate.drops.p3")
                        )
                ).withKey("innate_drops"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.pyro.title"), 0xFFFF453A,
                        Element.PYRO, null, EntityType.BLAZE, EntityType.MAGMA_CUBE,
                        "1.0U " + elemTitle(Element.PYRO), 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.innate.pyro.p1"), tr("codex.velorise_staves.innate.pyro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.hydro.title"), 0xFF0A84FF,
                        Element.HYDRO, null, EntityType.GUARDIAN, EntityType.DROWNED,
                        "1.0U " + elemTitle(Element.HYDRO), 0xFF0A84FF,
                        List.of(tr("codex.velorise_staves.innate.hydro.p1"), tr("codex.velorise_staves.innate.hydro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.electro.title"), 0xFFBF5AF2,
                        Element.ELECTRO, null, EntityType.ENDERMAN, EntityType.SHULKER,
                        "1.0U–1.5U " + elemTitle(Element.ELECTRO), 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.innate.electro.p1"), tr("codex.velorise_staves.innate.electro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.cryo.title"), 0xFF64D2FF,
                        Element.CRYO, null, EntityType.STRAY, EntityType.SNOW_GOLEM,
                        "1.0U " + elemTitle(Element.CRYO), 0xFF64D2FF,
                        List.of(tr("codex.velorise_staves.innate.cryo.p1"), tr("codex.velorise_staves.innate.cryo.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.dendro.title"), 0xFF32D74B,
                        Element.DENDRO, null, EntityType.CAVE_SPIDER, EntityType.SLIME,
                        "1.0U " + elemTitle(Element.DENDRO), 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.innate.dendro.p1"), tr("codex.velorise_staves.innate.dendro.p2"))
                ));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.innate.anemo_geo.title"), 0xFF55EFC4,
                        Element.ANEMO, Element.GEO, EntityType.WARDEN, EntityType.IRON_GOLEM,
                        elemTitle(Element.ANEMO) + " & " + elemTitle(Element.GEO), 0xFF55EFC4,
                        List.of(tr("codex.velorise_staves.innate.anemo_geo.p1"), tr("codex.velorise_staves.innate.anemo_geo.p2"))
                ));
            }
            case ENVIRONMENT -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.sculk.title"), 0xFF00D2D3,
                        Element.ELECTRO, null, new ItemStack(Items.SCULK_SENSOR), new ItemStack(Items.CALIBRATED_SCULK_SENSOR),
                        tr("codex.velorise_staves.badge.env_sculk"), 0xFF00D2D3,
                        List.of(tr("codex.velorise_staves.env.sculk.p1"), tr("codex.velorise_staves.env.sculk.p2"))
                ).withKey("env_sculk"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.redstone.title"), 0xFFFF453A,
                        Element.ELECTRO, null, new ItemStack(Items.LIGHTNING_ROD), new ItemStack(Items.OBSERVER),
                        tr("codex.velorise_staves.badge.env_redstone"), 0xFFFF453A,
                        List.of(tr("codex.velorise_staves.env.redstone.p1"), tr("codex.velorise_staves.env.redstone.p2"))
                ).withKey("env_redstone"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.amethyst.title"), 0xFFBF5AF2,
                        Element.ELECTRO, Element.PYRO, new ItemStack(Items.AMETHYST_CLUSTER), new ItemStack(Items.AMETHYST_SHARD),
                        tr("codex.velorise_staves.badge.env_amethyst"), 0xFFBF5AF2,
                        List.of(tr("codex.velorise_staves.env.amethyst.p1"), tr("codex.velorise_staves.env.amethyst.p2"))
                ).withKey("env_amethyst"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.pyro_melt.title"), 0xFFFF6B4A,
                        Element.PYRO, Element.CRYO, new ItemStack(Items.ICE), new ItemStack(Items.WATER_BUCKET),
                        tr("codex.velorise_staves.badge.env_pyro_melt"), 0xFFFF6B4A,
                        List.of(tr("codex.velorise_staves.env.pyro_melt.p1"), tr("codex.velorise_staves.env.pyro_melt.p2"))
                ).withKey("env_pyro_melt"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.pyro_burn.title"), 0xFFFF8C33,
                        Element.PYRO, null, new ItemStack(Items.TNT), new ItemStack(Items.CAMPFIRE),
                        tr("codex.velorise_staves.badge.env_pyro_burn"), 0xFFFF8C33,
                        List.of(tr("codex.velorise_staves.env.pyro_burn.p1"), tr("codex.velorise_staves.env.pyro_burn.p2"))
                ).withKey("env_pyro_burn"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.env.world_traps.title"), 0xFF32D74B,
                        Element.HYDRO, Element.DENDRO, new ItemStack(Items.SOUL_CAMPFIRE), new ItemStack(Items.POINTED_DRIPSTONE),
                        tr("codex.velorise_staves.badge.env_world"), 0xFF32D74B,
                        List.of(tr("codex.velorise_staves.env.world_traps.p1"), tr("codex.velorise_staves.env.world_traps.p2"))
                ).withKey("env_world_traps"));
            }
            case STAVES -> {
                String perHit = tr("codex.velorise_staves.staff.per_hit");
                String plusCloud = tr("codex.velorise_staves.staff.plus_cloud");
                String trapPoison = tr("codex.velorise_staves.staff.trap_poison");
                float maxHp = getClientPlayerMaxHp();
                float gBasic = ArcaneStaffItem.computeGuardianBaseDamage(maxHp, false);
                float gNeth = ArcaneStaffItem.computeGuardianBaseDamage(maxHp, true);
                Object[][] staffDefs = new Object[][]{
                        {"inferno_rod", ArcaneStaffItem.Kind.INFERNO, Element.PYRO,
                                fmtHearts(3.0 * dmgMult) + "§f\uE008§rx3", "§6" + fmtHearts(4.5 * dmgMult) + "§f\uE008§6x3", "30m", "codex.velorise_staves.staff.inferno"},
                        {"ghastfire_staff", ArcaneStaffItem.Kind.GHASTFIRE, Element.PYRO,
                                fmtHearts(6.0 * dmgMult) + "§f\uE008§r", "§6" + fmtHearts(9.0 * dmgMult) + "§f\uE008§6", "36m (AoE)", "codex.velorise_staves.staff.ghastfire"},
                        {"guardian_lens", ArcaneStaffItem.Kind.GUARDIAN, Element.HYDRO,
                                fmtHearts(gBasic * dmgMult) + "–" + fmtHearts(gBasic * 2.0F * dmgMult) + "§f\uE008§rx8 (3s)",
                                "§6" + fmtHearts(gNeth * dmgMult) + "–" + fmtHearts(gNeth * 2.0F * dmgMult) + "§f\uE008§6x8 (3s)", "24m➔28m", "codex.velorise_staves.staff.guardian"},
                        {"splendor_of_tranquil_waters", ArcaneStaffItem.Kind.SPLENDOR, Element.HYDRO,
                                fmtHearts(maxHp * 0.05F * dmgMult) + "–" + fmtHearts(maxHp * 0.14F * 1.4F * dmgMult) + "§f\uE008§r",
                                "§6" + fmtHearts(maxHp * 0.075F * dmgMult) + "–" + fmtHearts(maxHp * 0.21F * 1.4F * dmgMult) + "§f\uE008§6", "16m / 30s", "codex.velorise_staves.staff.splendor"},
                        {"frostbolt_wand", ArcaneStaffItem.Kind.FROST, Element.CRYO,
                                fmtHearts(3.0 * dmgMult) + "§f\uE008§rx3", "§6" + fmtHearts(4.5 * dmgMult) + "§f\uE008§6x3", "28m", "codex.velorise_staves.staff.frostbolt"},
                        {"arc_caster", ArcaneStaffItem.Kind.LIGHTNING, Element.ELECTRO,
                                fmtHearts(1.0 * dmgMult) + "§f\uE008§r" + perHit, "§6" + fmtHearts(1.5 * dmgMult) + "§f\uE008§6" + perHit, "30m➔36m", "codex.velorise_staves.staff.arc"},
                        {"dragonfire_scepter", ArcaneStaffItem.Kind.DRAGONFIRE, Element.ELECTRO,
                                fmtHearts(7.0 * dmgMult) + "§f\uE008§r" + plusCloud, "§6" + fmtHearts(10.5 * dmgMult) + "§f\uE008§6" + plusCloud, "32m (AoE)", "codex.velorise_staves.staff.dragonfire"},
                        {"shulker_seeker", ArcaneStaffItem.Kind.SHULKER, Element.ELECTRO,
                                fmtHearts(4.0 * dmgMult) + "§f\uE008§rx2", "§6" + fmtHearts(6.0 * dmgMult) + "§f\uE008§6x2", "30m", "codex.velorise_staves.staff.shulker"},
                        {"wither_scepter", ArcaneStaffItem.Kind.WITHER, Element.DENDRO,
                                fmtHearts(4.0 * dmgMult) + "§f\uE008§rx3", "§6" + fmtHearts(6.0 * dmgMult) + "§f\uE008§6x3", "32m (AoE)", "codex.velorise_staves.staff.wither"},
                        {"webcaster", ArcaneStaffItem.Kind.WEB, Element.DENDRO,
                                trapPoison, trapPoison, "28m", "codex.velorise_staves.staff.webcaster"},
                        {"wind_staff", ArcaneStaffItem.Kind.WIND, Element.ANEMO,
                                fmtHearts(1.5 * dmgMult) + "§f\uE008§r", "§6" + fmtHearts(2.25 * dmgMult) + "§f\uE008§6", "28m (AoE)", "codex.velorise_staves.staff.wind"},
                        {"resonant_echo", ArcaneStaffItem.Kind.ECHO, Element.ANEMO,
                                fmtHearts(11.0 * dmgMult) + "§f\uE008§r", "§6" + fmtHearts(16.5 * dmgMult) + "§f\uE008§6", "28m➔32m", "codex.velorise_staves.staff.echo"},
                        {"evoker_fang_staff", ArcaneStaffItem.Kind.EVOKER, Element.GEO,
                                fmtHearts(3.0 * dmgMult) + "§f\uE008§rx9", "§6" + fmtHearts(4.5 * dmgMult) + "§f\uE008§6x13", "13.5m➔19.5m", "codex.velorise_staves.staff.evoker"},
                        {"vortex_vanquisher", ArcaneStaffItem.Kind.VORTEX, Element.GEO,
                                fmtHearts((4.0 + maxHp * 0.019) * dmgMult) + "§f\uE008§r",
                                "§6" + fmtHearts((6.0 + maxHp * 0.0285) * dmgMult) + "§f\uE008§6", "12m (Max 3 / 4m Pulse)", "codex.velorise_staves.staff.vortex"},
                        {"omni_staff", ArcaneStaffItem.Kind.OMNI, null,
                                fmtHearts(10.0 * dmgMult) + "§f\uE008§r 2U", "§6" + fmtHearts(15.0 * dmgMult) + "§f\uE008§6 3U", "36m➔48m", "codex.velorise_staves.staff.omni"}
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
                    int color = el != null ? ClientAuraRenderer.getElementBorderColor(el) : 0xFFFFD60A;
                    String badge = el != null ? elemTitle(el).toUpperCase(Locale.ROOT) : "§f" + Element.omniIconString() + "§r " + tr("element.velorise_staves.omni").toUpperCase(Locale.ROOT);
                    String cdDisplay = (kind == ArcaneStaffItem.Kind.VORTEX)
                            ? String.format(Locale.ROOT, "%.1f/%.1f", cdSec, cdSec * 3.0D)
                            : String.format(Locale.ROOT, "%.1f", cdSec);
                    String statsCombat = tr("codex.velorise_staves.staff.stats_combat",
                            bDmg, nDmg, cdDisplay, rngStr);
                    String statsDur = tr("codex.velorise_staves.staff.stats_durability",
                            String.valueOf(bDur), String.valueOf(nDur));
                    GuideCard staffCard = new GuideCard(
                            name, color,
                            el, null, basicStack, ItemStack.EMPTY,
                            badge, color,
                            List.of(statsCombat, statsDur, tr(descKey))
                    ).withKey(baseId);
                    cards.add(kind == ArcaneStaffItem.Kind.OMNI ? staffCard.withOmniIcon() : staffCard);
                }
                // Utility Artifacts
                double vBasic = StavesConfig.getVoidTranslocatorRange(false);
                double vNeth = StavesConfig.getVoidTranslocatorRange(true);
                double mCd = StavesConfig.getMagicMirrorCooldownSeconds() * cdMult;
                cards.add(new GuideCard(
                        tr("item.velorise_staves.void_translocator") + " & " + tr("item.velorise_staves.magic_mirror"),
                        0xFFDA70D6, null, null,
                        getModItemStack("void_translocator"), getModItemStack("magic_mirror"),
                        tr("codex.velorise_staves.badge.utility"), 0xFFDA70D6,
                        List.of(
                                tr("codex.velorise_staves.staff.utility.p1", fmt(vBasic), fmt(vNeth)),
                                tr("codex.velorise_staves.staff.utility.p2", fmt(mCd))
                        )
                ).withKey("utility_relics"));
            }
            case MASTERY -> {
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card1.title"), 0xFFFFD60A,
                        null, null, new ItemStack(Items.ENCHANTED_BOOK), getModItemStack("netherite_arc_caster"),
                        tr("codex.velorise_staves.badge.max_level"), 0xFFFFD60A,
                        List.of(
                                tr("codex.velorise_staves.mastery.card1.p1"),
                                tr("codex.velorise_staves.mastery.card1.p2"),
                                tr("codex.velorise_staves.mastery.card1.p3"),
                                tr("codex.velorise_staves.mastery.card1.p4")
                        )
                ).withKey("mastery_em"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.prot.title"), 0xFF64D2FF,
                        null, null, new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.NETHERITE_CHESTPLATE),
                        tr("codex.velorise_staves.badge.max_level_4"), 0xFF64D2FF,
                        List.of(
                                tr("codex.velorise_staves.mastery.prot.p1"),
                                tr("codex.velorise_staves.mastery.prot.p2"),
                                tr("codex.velorise_staves.mastery.prot.p3")
                        )
                ).withKey("mastery_prot"));
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card2.title"), 0xFF55EFC4,
                        null, null, new ItemStack(Items.ENCHANTING_TABLE), new ItemStack(Items.EMERALD),
                        tr("codex.velorise_staves.badge.survival_sources"), 0xFF55EFC4,
                        List.of(
                                tr("codex.velorise_staves.mastery.card2.p1"),
                                tr("codex.velorise_staves.mastery.card2.p2")
                        )
                ));
                String syncMode = StavesConfig.isServerSynced()
                        ? tr("codex.velorise_staves.sync.server")
                        : tr("codex.velorise_staves.sync.local");
                String egtStatus = tr(StavesConfig.isElementalGaugeEnabled()
                        ? "codex.velorise_staves.status.on"
                        : "codex.velorise_staves.status.off");
                cards.add(new GuideCard(
                        tr("codex.velorise_staves.mastery.card3.title"), 0xFF0A84FF,
                        null, null, getModItemStack("elemental_codex"), ItemStack.EMPTY,
                        syncMode, StavesConfig.isServerSynced() ? 0xFF32D74B : 0xFF64D2FF,
                        List.of(
                                tr("codex.velorise_staves.mastery.card3.dmg_cd",
                                        String.format(Locale.ROOT, "%.2f", StavesConfig.getDamageMultiplier()),
                                        String.format(Locale.ROOT, "%.2f", StavesConfig.getCooldownMultiplier())),
                                tr("codex.velorise_staves.mastery.card3.egt_mutate",
                                        egtStatus,
                                        Math.round(StavesConfig.getInnateMutationChance() * 100.0)),
                                tr("codex.velorise_staves.mastery.card3.p1")
                        )
                ));
            }
            case RESONANCE -> addResonanceCards(cards);
        }
        return cards;
    }

    private void addResonanceCards(List<GuideCard> cards) {
        String capStr = StavesConfig.getMaxStavesInInventory() <= 0
                ? "∞"
                : String.valueOf(StavesConfig.getMaxStavesInInventory());
        String statusStr = tr(StavesConfig.isElementalResonanceEnabled()
                ? "codex.velorise_staves.status.on"
                : "codex.velorise_staves.status.off");
        cards.add(new GuideCard(
                tr("codex.velorise_staves.res.rules.title"), 0xFFFF5555,
                null, null, getModItemStack("omni_staff"), new ItemStack(Items.TRIDENT),
                tr("codex.velorise_staves.res.rules.badge", statusStr, capStr),
                StavesConfig.isElementalResonanceEnabled() ? 0xFF32D74B : 0xFFFF5555,
                List.of(
                        tr("codex.velorise_staves.res.rules.p1"),
                        tr("codex.velorise_staves.res.rules.p2", capStr, capStr),
                        tr("codex.velorise_staves.res.rules.p3")
                )
        ).withCustomIcon(RESONANCE_ICON, "config.velorise_staves.resonance"));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.pyro"), 0xFFFF4500,
                Element.PYRO, Element.PYRO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFFFF6B4A,
                List.of(tr("codex.velorise_staves.res.pyro.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.hydro"), 0xFF00C8FF,
                Element.HYDRO, Element.HYDRO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFF0A84FF,
                List.of(tr("codex.velorise_staves.res.hydro.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.anemo"), 0xFF40E0D0,
                Element.ANEMO, Element.ANEMO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFF40E0D0,
                List.of(tr("codex.velorise_staves.res.anemo.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.electro"), 0xFFBF40FF,
                Element.ELECTRO, Element.ELECTRO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFFBF40FF,
                List.of(tr("codex.velorise_staves.res.electro.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.dendro"), 0xFF32CD32,
                Element.DENDRO, Element.DENDRO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFF32CD32,
                List.of(tr("codex.velorise_staves.res.dendro.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.cryo"), 0xFF4DF0FF,
                Element.CRYO, Element.CRYO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFF4DF0FF,
                List.of(tr("codex.velorise_staves.res.cryo.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.geo"), 0xFFFFD700,
                Element.GEO, Element.GEO, ItemStack.EMPTY, ItemStack.EMPTY,
                tr("codex.velorise_staves.res.badge.pair"), 0xFFFFD700,
                List.of(tr("codex.velorise_staves.res.geo.p1"))
        ));
        cards.add(new GuideCard(
                tr("resonance.velorise_staves.canopy"), 0xFFFFD60A,
                null, null,
                List.of(Element.PYRO, Element.HYDRO, Element.ANEMO, Element.DENDRO),
                ItemStack.EMPTY, ItemStack.EMPTY, null, null,
                tr("codex.velorise_staves.res.badge.four"), 0xFFFFD60A,
                List.of(tr("codex.velorise_staves.res.canopy.p1"))
        ).withDynamicElemGroup());
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
        int closeBtnW = 20;
        int closeBtnX = left + winW - closeBtnW - 4;
        int closeBtnY = top + 4;
        int cfgBtnW = 58;
        int cfgBtnX = closeBtnX - cfgBtnW - 4;
        drawLeftFitString(guiGraphics, this.title.getString(), left + 26, top + 9, Math.max(40, cfgBtnX - (left + 26) - 6), 0xFFFFE082, true);

        // Header Right Buttons: [Config] and [✕]
        boolean hoverClose = mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + 18;
        guiGraphics.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + 18, hoverClose ? 0xFF882222 : 0xFF2B1B24);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, closeBtnX, closeBtnY, closeBtnW, 18, hoverClose ? 0xFFFF5555 : 0xFF773333);
        guiGraphics.drawCenteredString(this.font, "✕", closeBtnX + closeBtnW / 2, closeBtnY + 5, 0xFFFFFFFF);

        int cfgBtnY = top + 4;
        boolean hoverCfg = mouseX >= cfgBtnX && mouseX <= cfgBtnX + cfgBtnW && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 18;
        guiGraphics.fill(cfgBtnX, cfgBtnY, cfgBtnX + cfgBtnW, cfgBtnY + 18, hoverCfg ? 0xFF253B5E : 0xFF18243A);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, cfgBtnX, cfgBtnY, cfgBtnW, 18, hoverCfg ? 0xFF64D2FF : 0xFF3A5A88);
        drawCenteredFitString(guiGraphics, tr("codex.velorise_staves.btn.config"), cfgBtnX + cfgBtnW / 2, cfgBtnY + 5, cfgBtnW - 6, 0xFFE0F2FF);

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

            // Tab Icon (16x16 Element/GUI texture, 3D Blaze entity render, or ItemStack)
            int iconOffsetY = ty + (tabH - 16) / 2;
            if (tab.iconElement != null) {
                drawElementIcon(guiGraphics, tab.iconElement, tx + 5, iconOffsetY);
            } else if (tab == Tab.INNATE) {
                renderGuiMob(guiGraphics, EntityType.BLAZE, tx + 5, iconOffsetY, 12.5F);
            } else if (tab == Tab.RESONANCE) {
                drawGuiTextureIcon(guiGraphics, ClientAuraRenderer.getResonanceIconTexture(), tx + 5, iconOffsetY);
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

        this.pendingTooltipItem = ItemStack.EMPTY;
        this.pendingTooltipLines = null;
        this.pendingClickTab = null;
        this.pendingClickCardKey = null;
        this.canTriggerContentHover = mouseX >= contentX + 2 && mouseX <= contentX + contentW - 2
                && mouseY >= contentY + 2 && mouseY <= contentY + contentH - 2;

        guiGraphics.enableScissor(contentX + 2, contentY + 2, contentX + contentW - 2, contentY + contentH - 2);

        int curY = startY;

        // 4A. Render Visual Diagram Showcase Box at top of tab
        curY += renderIllustrationBox(guiGraphics, this.currentTab, innerX, curY, innerW, mouseX, mouseY) + 6;

        // 4B. Render Guide Cards (from cache)
        CachedTabContent tabContent = getOrBuildTabContent(this.currentTab, innerW);
        for (PreparedCard pCard : tabContent.cards()) {
            curY += renderPreparedCard(guiGraphics, pCard, innerX, curY, innerW, mouseX, mouseY) + 6;
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

        // 6. Render Hover Tooltip (outside Scissor region)
        if (this.pendingTooltipItem != null && !this.pendingTooltipItem.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.pendingTooltipItem, mouseX, mouseY);
        } else if (this.pendingTooltipLines != null && !this.pendingTooltipLines.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, this.pendingTooltipLines, mouseX, mouseY);
        }
    }

    private int renderIllustrationBox(GuiGraphics guiGraphics, Tab tab, int x, int y, int w, int mouseX, int mouseY) {
        int h = 48;
        guiGraphics.fill(x, y, x + w, y + h, 0xFF131A2C);
        if (tab == Tab.ELEMENTS) {
            renderElementsSegmentedBorder(guiGraphics, x, y, w, h);
        } else {
            ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, h, tab.accentColor);
        }

        String diagramTitle = tr("codex.velorise_staves.diagram." + tab.key);
        drawCenteredFitString(guiGraphics, "§e✦ " + diagramTitle + " ✦", x + w / 2, y + 6, w - 10, 0xFFFFE082);

        int midY = y + 23;
        int cx = x + w / 2;

        switch (tab) {
            case BASICS -> {
                renderHoveredItem(guiGraphics, getModItemStack("inferno_rod"), cx - 114, midY, mouseX, mouseY);
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 94, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, tr("codex.velorise_staves.diagram.basics.aura"), cx - 75, midY + 4, 36, 0xFFFF6B4A, true);
                guiGraphics.drawString(this.font, "+ ➔", cx - 37, midY + 4, 0xFFFFD60A, true);
                renderHoveredItem(guiGraphics, getModItemStack("guardian_lens"), cx - 16, midY, mouseX, mouseY);
                renderHoveredElement(guiGraphics, Element.HYDRO, cx + 2, midY, mouseX, mouseY);
                String vapUpper = tr("reaction.velorise_staves.vaporize").toUpperCase(Locale.ROOT);
                drawLeftFitString(guiGraphics, "➔ §b§l" + vapUpper + " x2!", cx + 21, midY + 4, Math.max(40, (x + w - 4) - (cx + 21)), 0xFFFFFFFF, true);
            }
            case ELEMENTS -> {
                Element[] all = GENSHIN_ELEMENTS_ORDER;
                int totalW = all.length * 28;
                int sx = cx - totalW / 2 + 4;
                for (int i = 0; i < all.length; i++) {
                    int ex = sx + i * 28;
                    guiGraphics.fill(ex - 2, midY - 2, ex + 18, midY + 18, 0x88101828);
                    ClientAuraRenderer.renderColoredBorder(guiGraphics, ex - 2, midY - 2, 20, 20, ClientAuraRenderer.getElementBorderColor(all[i]));
                    renderHoveredElement(guiGraphics, all[i], ex, midY, mouseX, mouseY);
                }
            }
            case REACTIONS_AMP -> {
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 96, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", cx - 76, midY + 4, 0xFFFFFFFF, true);
                renderHoveredElement(guiGraphics, Element.HYDRO, cx - 66, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "§6= x2.0", cx - 46, midY + 4, 0xFFFFAA00, true);

                guiGraphics.drawString(this.font, "|", cx - 4, midY + 4, 0xFF556688, false);

                renderHoveredElement(guiGraphics, Element.HYDRO, cx + 8, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", cx + 28, midY + 4, 0xFFFFFFFF, true);
                renderHoveredElement(guiGraphics, Element.CRYO, cx + 38, midY, mouseX, mouseY);
                String frozenUpper = tr("reaction.velorise_staves.frozen").toUpperCase(Locale.ROOT);
                drawLeftFitString(guiGraphics, "§b= " + frozenUpper, cx + 58, midY + 4, Math.max(36, (x + w - 4) - (cx + 58)), 0xFF64D2FF, true);
            }
            case REACTIONS_DENDRO -> {
                int sx = cx - 114;
                renderHoveredElement(guiGraphics, Element.DENDRO, sx, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", sx + 18, midY + 4, 0xFFFFFFFF, true);
                renderHoveredElement(guiGraphics, Element.HYDRO, sx + 26, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", sx + 45, midY + 4, 0xFF32D74B, true);
                renderHoveredItem(guiGraphics, getModItemStack("dendro_core"), sx + 55, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", sx + 74, midY + 4, 0xFFFFD60A, true);
                renderHoveredElement(guiGraphics, Element.PYRO, sx + 85, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, "§6" + tr("reaction.velorise_staves.burgeon"), sx + 103, midY + 4, 45, 0xFFFF9F0A, true);
                renderHoveredElement(guiGraphics, Element.ELECTRO, sx + 150, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, "§d" + tr("reaction.velorise_staves.hyperbloom"), sx + 168, midY + 4, Math.max(36, (x + w - 4) - (sx + 168)), 0xFFDA70D6, true);
            }
            case INNATE -> {
                renderHoveredMob(guiGraphics, EntityType.BLAZE, cx - 118, midY, 13.0F, mouseX, mouseY);
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 99, midY, mouseX, mouseY);
                String immuneUpper = tr("reaction.velorise_staves.immune").toUpperCase(Locale.ROOT);
                drawLeftFitString(guiGraphics, "➔ §c" + immuneUpper, cx - 80, midY + 4, 56, 0xFFFF5555, true);

                guiGraphics.drawString(this.font, "|", cx - 22, midY + 4, 0xFF556688, false);

                renderHoveredMob(guiGraphics, EntityType.BLAZE, cx - 12, midY, 13.0F, mouseX, mouseY);
                renderHoveredElement(guiGraphics, Element.HYDRO, cx + 6, midY, mouseX, mouseY);
                String vapUpper = tr("reaction.velorise_staves.vaporize").toUpperCase(Locale.ROOT);
                drawLeftFitString(guiGraphics, "➔ §b§l" + vapUpper + " x2!", cx + 25, midY + 4, Math.max(40, (x + w - 4) - (cx + 25)), 0xFFFFFFFF, true);
            }
            case ENVIRONMENT -> {
                renderHoveredItem(guiGraphics, new ItemStack(Items.SCULK_SENSOR), cx - 118, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx - 99, midY + 4, 0xFF00D2D3, true);
                renderHoveredElement(guiGraphics, Element.ELECTRO, cx - 88, midY, mouseX, mouseY);

                guiGraphics.drawString(this.font, "|", cx - 68, midY + 4, 0xFF556688, false);

                renderHoveredItem(guiGraphics, new ItemStack(Items.AMETHYST_CLUSTER), cx - 58, midY, mouseX, mouseY);
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 40, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx - 22, midY + 4, 0xFFFF8C33, true);
                renderHoveredItem(guiGraphics, new ItemStack(Items.AMETHYST_SHARD), cx - 11, midY, mouseX, mouseY);

                guiGraphics.drawString(this.font, "|", cx + 9, midY + 4, 0xFF556688, false);

                renderHoveredElement(guiGraphics, Element.PYRO, cx + 18, midY, mouseX, mouseY);
                renderHoveredItem(guiGraphics, new ItemStack(Items.ICE), cx + 36, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx + 54, midY + 4, 0xFF0A84FF, true);
                renderHoveredItem(guiGraphics, new ItemStack(Items.WATER_BUCKET), cx + 65, midY, mouseX, mouseY);
            }
            case STAVES -> {
                renderHoveredItem(guiGraphics, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), cx - 106, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", cx - 87, midY + 4, 0xFFFFFFFF, true);
                renderHoveredItem(guiGraphics, getModItemStack("dragonfire_scepter"), cx - 76, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", cx - 57, midY + 4, 0xFFFFFFFF, true);
                renderHoveredItem(guiGraphics, new ItemStack(Items.NETHERITE_INGOT), cx - 46, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx - 26, midY + 4, 0xFFFFD60A, true);
                renderHoveredItem(guiGraphics, getModItemStack("netherite_dragonfire_scepter"), cx - 14, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, "§6+50% §f\uE008§6 / +50% U", cx + 6, midY + 4, Math.max(40, (x + w - 4) - (cx + 6)), 0xFFFFD60A, true);
            }
            case MASTERY -> {
                renderHoveredItem(guiGraphics, new ItemStack(Items.ENCHANTING_TABLE), cx - 110, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx - 91, midY + 4, 0xFFFFD60A, true);
                renderHoveredItem(guiGraphics, new ItemStack(Items.ENCHANTED_BOOK), cx - 79, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, tr("codex.velorise_staves.diagram.mastery.summary"), cx - 59, midY + 4, Math.max(60, (x + w - 4) - (cx - 59)), 0xFFE0B0FF, true);
            }
            case RESONANCE -> {
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 122, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "+", cx - 104, midY + 4, 0xFFFFFFFF, true);
                renderHoveredElement(guiGraphics, Element.PYRO, cx - 96, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "➔", cx - 78, midY + 4, 0xFFFF6B4A, true);
                renderHoveredResonanceIcon(guiGraphics, cx - 68, midY, mouseX, mouseY);
                drawLeftFitString(guiGraphics, tr("codex.velorise_staves.diagram.resonance.pair"), cx - 50, midY + 4, 42, 0xFFFF9F68, true);

                guiGraphics.drawString(this.font, "|", cx - 6, midY + 4, 0xFF556688, false);

                renderHoveredOmniElement(guiGraphics, cx + 3, midY, mouseX, mouseY);
                guiGraphics.drawString(this.font, "|", cx + 21, midY + 4, 0xFF556688, false);
                renderHoveredItem(guiGraphics, new ItemStack(Items.TRIDENT), cx + 28, midY, mouseX, mouseY);
                String capStr = StavesConfig.getMaxStavesInInventory() <= 0
                        ? "∞"
                        : String.valueOf(StavesConfig.getMaxStavesInInventory());
                drawLeftFitString(guiGraphics, tr("codex.velorise_staves.diagram.resonance.summary", capStr),
                        cx + 47, midY + 4, Math.max(48, (x + w - 4) - (cx + 47)), 0xFF40E0D0, true);
            }
        }

        return h;
    }

    private static final Element[] GENSHIN_ELEMENTS_ORDER = {
            Element.PYRO, Element.HYDRO, Element.ANEMO, Element.ELECTRO, Element.DENDRO, Element.CRYO, Element.GEO
    };

    private static void renderElementsSegmentedBorder(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        Element[] all = GENSHIN_ELEMENTS_ORDER;
        int cx = x + w / 2;
        int totalW = all.length * 28;
        int sx = cx - totalW / 2 + 4;
        for (int i = 0; i < all.length; i++) {
            int segX0 = (i == 0) ? x : (sx + (i - 1) * 28 + 22);
            int segX1 = (i == all.length - 1) ? (x + w) : (sx + i * 28 + 22);
            int color = ClientAuraRenderer.getElementBorderColor(all[i]);
            int alpha = (color >> 24) & 0xFF;
            int glow = ((Math.max(40, alpha / 3)) << 24) | (color & 0x00FFFFFF);

            int g0 = segX0 - (i == 0 ? 1 : 0);
            int g1 = segX1 + (i == all.length - 1 ? 1 : 0);
            guiGraphics.fill(g0, y - 1, g1, y, glow);
            guiGraphics.fill(g0, y + h, g1, y + h + 1, glow);
            guiGraphics.fill(segX0, y, segX1, y + 1, color);
            guiGraphics.fill(segX0, y + h - 1, segX1, y + h, color);

            if (i == 0) {
                guiGraphics.fill(x - 1, y, x, y + h, glow);
                guiGraphics.fill(x, y, x + 1, y + h, color);
            }
            if (i == all.length - 1) {
                guiGraphics.fill(x + w, y, x + w + 1, y + h, glow);
                guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
            }

            int wash0 = (i == 0) ? x + 1 : segX0;
            int wash1 = (i == all.length - 1) ? (x + w - 1) : segX1;
            guiGraphics.fill(wash0, y + 1, wash1, y + h - 1, (0x16 << 24) | (color & 0x00FFFFFF));
        }
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

    private record CacheKey(
            Tab tab,
            int innerW,
            String lang,
            double dmgMult,
            double cdMult,
            boolean egtEnabled,
            double mutateChance,
            boolean resonanceEnabled,
            int maxStavesCap,
            boolean serverSynced,
            float playerMaxHp
    ) {}

    private record PreparedLink(int linkId, Tab tab, String cardKey, int relX0, int relX1) {}

    private record PreparedLine(
            FormattedCharSequence seq,
            List<Integer> cps,
            List<Style> styles,
            int[] linkIds,
            List<PreparedLink> links
    ) {
        boolean containsLinkId(int linkId) {
            if (linkId < 0) return false;
            for (PreparedLink link : links) {
                if (link.linkId() == linkId) return true;
            }
            return false;
        }
    }

    private record PreparedCard(
            GuideCard card,
            List<FormattedCharSequence> titleLines,
            List<PreparedLine> lines,
            int badgeW,
            int titleRelStartX,
            boolean badgeBelowTitle,
            int headerH,
            int cardH
    ) {}

    private record CachedTabContent(
            CacheKey key,
            List<PreparedCard> cards,
            Map<String, Integer> cardOffsets,
            int totalHeight
    ) {}

    private List<ReactionLink> getActiveReactionLinks(String lang) {
        if (!Objects.equals(this.cachedLinkLang, lang) || this.cachedReactionLinks.isEmpty()) {
            this.cachedLinkLang = lang;
            this.cachedReactionLinks = buildReactionLinksForActiveLanguage();
        }
        return this.cachedReactionLinks;
    }

    private CachedTabContent getOrBuildTabContent(Tab tab, int innerW) {
        String lang = (this.minecraft != null && this.minecraft.getLanguageManager() != null)
                ? this.minecraft.getLanguageManager().getSelected()
                : "en_us";
        CacheKey currentKey = new CacheKey(
                tab,
                innerW,
                lang,
                StavesConfig.getDamageMultiplier(),
                StavesConfig.getCooldownMultiplier(),
                StavesConfig.isElementalGaugeEnabled(),
                StavesConfig.INNATE_MUTATION_CHANCE.get(),
                StavesConfig.isElementalResonanceEnabled(),
                StavesConfig.getMaxStavesInInventory(),
                StavesConfig.isServerSynced(),
                getClientPlayerMaxHp()
        );
        CachedTabContent cached = this.tabCache.get(tab);
        if (cached != null && currentKey.equals(cached.key())) {
            return cached;
        }

        List<ReactionLink> activeLinks = getActiveReactionLinks(lang);
        int[] linkCounter = new int[]{0};
        List<GuideCard> cards = buildCardsForTab(tab);
        List<PreparedCard> prepared = new ArrayList<>(cards.size());
        Map<String, Integer> offsets = new HashMap<>();
        int totalH = 48 + 6;
        for (GuideCard card : cards) {
            PreparedCard pc = prepareGuideCard(card, tab, innerW, activeLinks, linkCounter);
            if (card.cardKey != null) {
                offsets.put(card.cardKey, totalH);
            }
            prepared.add(pc);
            totalH += pc.cardH() + 6;
        }
        totalH += 8;
        CachedTabContent built = new CachedTabContent(currentKey, List.copyOf(prepared), Map.copyOf(offsets), totalH);
        this.tabCache.put(tab, built);
        return built;
    }

    private List<Element> getDynamicRandomGroup(int count) {
        long cycle = System.currentTimeMillis() / 1200L;
        if (cycle != this.lastDynamicGroupCycle || this.dynamicGroupElements.size() != count) {
            this.lastDynamicGroupCycle = cycle;
            List<Element> pool = new ArrayList<>(Arrays.asList(GENSHIN_ELEMENTS_ORDER));
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            List<Element> picked = this.dynamicGroupElements;
            int drawCount = Math.min(count, pool.size());
            for (int attempt = 0; attempt < 4; attempt++) {
                Collections.shuffle(pool, rng);
                List<Element> candidate = List.copyOf(pool.subList(0, drawCount));
                if (!candidate.equals(picked)) {
                    picked = candidate;
                    break;
                }
                picked = candidate;
            }
            this.dynamicGroupElements = picked;
        }
        return this.dynamicGroupElements;
    }

    private PreparedCard prepareGuideCard(GuideCard card, Tab ownTab, int w, List<ReactionLink> links, int[] linkCounter) {
        int pad = 6;
        int textW = w - pad * 2 - 4;

        int iconOffset = 6;
        if (card.customIcon != null) iconOffset += 17;
        if (card.elem1 != null || card.omniElem1) iconOffset += 17;
        if (card.elem2 != null) iconOffset += 6 + 17;
        if (card.elemGroup != null && !card.elemGroup.isEmpty()) {
            boolean hasLeftOperand = card.elem1 != null || card.omniElem1;
            int groupSize = card.elemGroup.size();
            if (hasLeftOperand) {
                iconOffset += 6 + 5 + groupSize * 16 + (groupSize - 1) * 5 + 7;
            } else {
                iconOffset += groupSize * 16 + (groupSize - 1) * 5 + 2;
            }
        }
        if (card.plusBeforeItem1 && !card.item1.isEmpty()) iconOffset += 6;
        if (!card.item1.isEmpty()) iconOffset += 17;
        if (!card.item2.isEmpty()) iconOffset += 17;
        if (card.mob1 != null) iconOffset += 18;
        if (card.mob2 != null) iconOffset += 18;
        int titleRelStartX = iconOffset + 2;

        int rawBadgeW = (card.badgeText != null && !card.badgeText.isEmpty()) ? (this.font.width(card.badgeText) + 8) : 0;
        int badgeW = Math.min(rawBadgeW, Math.max(24, w - 12));
        int rightLimitWithBadge = (badgeW > 0) ? (w - badgeW - 7) : (w - 5);
        int availTitleW = rightLimitWithBadge - titleRelStartX;
        int fullTitleW = Math.max(60, (w - 5) - titleRelStartX);

        String boldTitle = "§l" + card.title;
        List<FormattedCharSequence> titleLines;
        boolean badgeBelowTitle = false;
        int singleW = this.font.width(boldTitle);
        if (badgeW > 0 && (availTitleW < 40 || (availTitleW < 65 && singleW > availTitleW) || (!card.title.contains(" ") && singleW > availTitleW))) {
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

        List<PreparedLine> wrappedLines = new ArrayList<>();
        for (String p : card.paragraphs) {
            MutableComponent paraComp = buildParagraphComponent(p, ownTab, card.cardKey, links, linkCounter);
            for (FormattedCharSequence seq : this.font.split(paraComp, textW)) {
                wrappedLines.add(prepareLine(seq));
            }
        }

        int bodyH = wrappedLines.size() * 11 + 6;
        int cardH = headerH + bodyH;
        return new PreparedCard(card, List.copyOf(titleLines), List.copyOf(wrappedLines), badgeW, titleRelStartX, badgeBelowTitle, headerH, cardH);
    }

    private int renderPreparedCard(GuiGraphics guiGraphics, PreparedCard pCard, int x, int y, int w, int mouseX, int mouseY) {
        GuideCard card = pCard.card();
        int pad = 6;
        int badgeW = pCard.badgeW();
        int titleStartX = x + pCard.titleRelStartX();
        boolean badgeBelowTitle = pCard.badgeBelowTitle();
        int headerH = pCard.headerH();
        int cardH = pCard.cardH();

        int effectiveBorderColor = card.omniElem1
                ? (0xFF000000 | com.velorise.staves.entity.OmniOrbEntity.getIridescentRgb((System.currentTimeMillis() % 100000L) / 55.0F))
                : card.borderColor;
        int effectiveBadgeColor = card.omniElem1 ? effectiveBorderColor : card.badgeColor;

        // Card Background & Uniform 1px Border
        guiGraphics.fill(x, y, x + w, y + cardH, 0xE6141B2D);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, x, y, w, cardH, effectiveBorderColor);
        // Header divider
        guiGraphics.fill(x + 1, y + headerH, x + w - 1, y + headerH + 1, 0x44FFFFFF);

        int iconX = x + 6;
        int iconY = y + 3;

        if (card.customIcon != null) {
            if (RESONANCE_ICON.equals(card.customIcon)) {
                renderHoveredResonanceIcon(guiGraphics, iconX, iconY, mouseX, mouseY);
            } else {
                String titleKey = card.customIconTitleKey != null ? card.customIconTitleKey : "config.velorise_staves.resonance";
                renderHoveredTextureBadge(guiGraphics, MOONSIGN_ICON.equals(card.customIcon) ? ClientAuraRenderer.getMoonsignIconTexture() : card.customIcon, Element.moonsignIconString(), titleKey, ChatFormatting.AQUA, iconX, iconY, mouseX, mouseY);
            }
            iconX += 17;
        }
        if (card.omniElem1) {
            renderHoveredOmniElement(guiGraphics, iconX, iconY, mouseX, mouseY);
            iconX += 17;
        } else if (card.elem1 != null) {
            renderHoveredElement(guiGraphics, card.elem1, iconX, iconY, mouseX, mouseY);
            iconX += 17;
        }
        if (card.elem2 != null) {
            guiGraphics.drawString(this.font, "+", iconX, iconY + 4, 0xFFFFD60A, true);
            iconX += 6;
            renderHoveredElement(guiGraphics, card.elem2, iconX, iconY, mouseX, mouseY);
            iconX += 17;
        }
        if (card.elemGroup != null && !card.elemGroup.isEmpty()) {
            List<Element> displayGroup = card.dynamicElemGroup
                    ? getDynamicRandomGroup(card.elemGroup.size())
                    : card.elemGroup;
            boolean hasLeftOperand = card.elem1 != null || card.omniElem1;
            if (hasLeftOperand) {
                guiGraphics.drawString(this.font, "+", iconX, iconY + 4, 0xFFFFD60A, true);
                iconX += 6;
                guiGraphics.drawString(this.font, "(", iconX, iconY + 4, 0xFFA0B0C8, true);
                iconX += 5;
            }
            for (int i = 0; i < displayGroup.size(); i++) {
                renderHoveredElement(guiGraphics, displayGroup.get(i), iconX, iconY, mouseX, mouseY);
                iconX += 16;
                if (i < displayGroup.size() - 1) {
                    guiGraphics.drawString(this.font, "|", iconX + 2, iconY + 4, 0xFF708098, false);
                    iconX += 5;
                }
            }
            if (hasLeftOperand) {
                guiGraphics.drawString(this.font, ")", iconX + 1, iconY + 4, 0xFFA0B0C8, true);
                iconX += 7;
            } else {
                iconX += 2;
            }
        }
        if (card.plusBeforeItem1 && !card.item1.isEmpty()) {
            guiGraphics.drawString(this.font, "+", iconX, iconY + 4, 0xFFFFD60A, true);
            iconX += 6;
        }
        if (!card.item1.isEmpty()) {
            renderHoveredItem(guiGraphics, card.item1, iconX, iconY, mouseX, mouseY);
            iconX += 17;
        }
        if (!card.item2.isEmpty()) {
            renderHoveredItem(guiGraphics, card.item2, iconX, iconY, mouseX, mouseY);
            iconX += 17;
        }
        if (card.mob1 != null) {
            renderHoveredMob(guiGraphics, card.mob1, iconX, iconY, 12.5F, mouseX, mouseY);
            iconX += 18;
        }
        if (card.mob2 != null) {
            renderHoveredMob(guiGraphics, card.mob2, iconX, iconY, 12.5F, mouseX, mouseY);
            iconX += 18;
        }

        // Card Title
        int titleY = (pCard.titleLines().size() == 1 && !badgeBelowTitle) ? (y + 7) : (y + 4);
        for (FormattedCharSequence tSeq : pCard.titleLines()) {
            guiGraphics.drawString(this.font, tSeq, titleStartX, titleY, 0xFFFFFFFF, true);
            titleY += 10;
        }

        // Badge on Top-Right of Card
        if (badgeW > 0) {
            int bx = x + w - badgeW - 5;
            int by = badgeBelowTitle ? (titleY + 1) : (y + 4);
            guiGraphics.fill(bx, by, bx + badgeW, by + 14, 0xAA080B12);
            ClientAuraRenderer.renderColoredBorder(guiGraphics, bx, by, badgeW, 14, effectiveBadgeColor);
            drawLeftFitString(guiGraphics, card.badgeText, bx + 4, by + 3, Math.max(8, badgeW - 8), effectiveBadgeColor, true);
        }

        // Wrapped Paragraphs (pre-cached hoverable reaction links, underlines even across line wraps)
        int baseLineY = y + headerH + 5;
        int lineX = x + pad + 2;
        int hoveredLinkId = -1;
        if (this.canTriggerContentHover) {
            int scanY = baseLineY;
            for (PreparedLine line : pCard.lines()) {
                for (PreparedLink link : line.links()) {
                    if (isMouseOverBox(lineX + link.relX0(), scanY - 1, Math.max(1, link.relX1() - link.relX0()), 10, mouseX, mouseY)) {
                        hoveredLinkId = link.linkId();
                        this.pendingClickTab = link.tab();
                        this.pendingClickCardKey = link.cardKey();
                        break;
                    }
                }
                if (hoveredLinkId >= 0) break;
                scanY += 11;
            }
        }

        int drawY = baseLineY;
        for (PreparedLine line : pCard.lines()) {
            if (hoveredLinkId >= 0 && line.containsLinkId(hoveredLinkId)) {
                guiGraphics.drawString(this.font, buildUnderlinedLineComponent(line, hoveredLinkId), lineX, drawY, 0xFFD4DEEE, false);
            } else {
                guiGraphics.drawString(this.font, line.seq(), lineX, drawY, 0xFFD4DEEE, false);
            }
            drawY += 11;
        }

        return cardH;
    }

    private static Style applyLegacyFormatting(Style current, ChatFormatting fmt) {
        if (fmt == ChatFormatting.RESET) {
            return Style.EMPTY;
        }
        if (fmt.isColor()) {
            return Style.EMPTY.withColor(fmt);
        }
        return switch (fmt) {
            case BOLD -> current.withBold(true);
            case ITALIC -> current.withItalic(true);
            case UNDERLINE -> current.withUnderlined(true);
            case STRIKETHROUGH -> current.withStrikethrough(true);
            case OBFUSCATED -> current.withObfuscated(true);
            default -> current;
        };
    }

    private static MutableComponent buildParagraphComponent(
            String raw,
            Tab ownTab,
            String ownCardKey,
            List<ReactionLink> links,
            int[] linkCounter
    ) {
        List<Integer> cps = new ArrayList<>();
        List<Style> cpStyles = new ArrayList<>();
        Style curStyle = Style.EMPTY;

        for (int i = 0; i < raw.length(); ) {
            char ch = raw.charAt(i);
            if (ch == '§' && i + 1 < raw.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (fmt != null) {
                    curStyle = applyLegacyFormatting(curStyle, fmt);
                    i += 2;
                    continue;
                }
            }
            int cp = raw.codePointAt(i);
            cps.add(cp);
            cpStyles.add(curStyle);
            i += Character.charCount(cp);
        }

        int n = cps.size();
        if (n == 0) return Component.empty();

        StringBuilder plainBuilder = new StringBuilder(n);
        int[] cpToUtf16 = new int[n + 1];
        for (int k = 0; k < n; k++) {
            cpToUtf16[k] = plainBuilder.length();
            plainBuilder.appendCodePoint(cps.get(k));
        }
        cpToUtf16[n] = plainBuilder.length();

        int[] utf16ToCp = new int[plainBuilder.length() + 1];
        for (int k = 0; k < n; k++) {
            int startU = cpToUtf16[k];
            int endU = cpToUtf16[k + 1];
            for (int u = startU; u < endU; u++) {
                utf16ToCp[u] = k;
            }
        }
        utf16ToCp[plainBuilder.length()] = n;

        String lower = plainBuilder.toString().toLowerCase(Locale.ROOT);
        boolean[] used = new boolean[n];
        for (ReactionLink link : links) {
            if (ownCardKey != null && ownCardKey.equals(link.cardKey())) continue;
            if (ownTab == Tab.RESONANCE && link.tab() == Tab.RESONANCE) continue;
            String kw = link.keyword();
            int len = kw.length();
            if (len == 0 || len > lower.length()) continue;
            int from = 0;
            while (from <= lower.length() - len) {
                int pos = lower.indexOf(kw, from);
                if (pos < 0) break;
                int end = pos + len;
                int cpStart = utf16ToCp[pos];
                int cpEnd = utf16ToCp[end];
                boolean boundaryOk = link.cjk()
                        || ((cpStart == 0 || !Character.isLetterOrDigit(cps.get(cpStart - 1)))
                        && (cpEnd >= n || !Character.isLetterOrDigit(cps.get(cpEnd))));
                if (boundaryOk && cpEnd > cpStart && cpEnd <= n) {
                    boolean overlap = false;
                    for (int k = cpStart; k < cpEnd; k++) {
                        if (used[k]) {
                            overlap = true;
                            break;
                        }
                    }
                    if (!overlap) {
                        int linkId = linkCounter[0]++;
                        String marker = LINK_MARKER_PREFIX + linkId + ":" + link.tab().name() + ":" + link.cardKey();
                        for (int k = cpStart; k < cpEnd; k++) {
                            used[k] = true;
                            cpStyles.set(k, cpStyles.get(k).withInsertion(marker));
                        }
                    }
                }
                from = pos + 1;
            }
        }

        return buildStyledComponent(cps, cpStyles, 0, n);
    }

    private PreparedLine prepareLine(FormattedCharSequence seq) {
        List<Integer> cps = new ArrayList<>();
        List<Style> cleanStyles = new ArrayList<>();
        List<String> markers = new ArrayList<>();

        seq.accept((idx, style, cp) -> {
            cps.add(cp);
            String ins = style.getInsertion();
            if (ins != null && ins.startsWith(LINK_MARKER_PREFIX)) {
                markers.add(ins);
                cleanStyles.add(style.withInsertion(null));
            } else {
                markers.add(null);
                cleanStyles.add(style);
            }
            return true;
        });

        int n = cps.size();
        int[] linkIds = new int[n];
        List<PreparedLink> links = new ArrayList<>();
        for (int i = 0; i < n; ) {
            String m = markers.get(i);
            if (m == null) {
                linkIds[i] = -1;
                i++;
                continue;
            }
            int j = i + 1;
            while (j < n && m.equals(markers.get(j))) {
                j++;
            }
            String payload = m.substring(LINK_MARKER_PREFIX.length());
            int c1 = payload.indexOf(':');
            int c2 = payload.indexOf(':', c1 + 1);
            int linkId = -1;
            Tab tab = null;
            String cardKey = null;
            if (c1 > 0 && c2 > c1) {
                try {
                    linkId = Integer.parseInt(payload.substring(0, c1));
                    tab = Tab.valueOf(payload.substring(c1 + 1, c2));
                    cardKey = payload.substring(c2 + 1);
                } catch (IllegalArgumentException ignored) {
                    linkId = -1;
                }
            }
            for (int k = i; k < j; k++) {
                linkIds[k] = linkId;
            }
            if (linkId >= 0 && tab != null && cardKey != null) {
                int relX0 = widthOfSlice(cps, cleanStyles, 0, i);
                int relX1 = widthOfSlice(cps, cleanStyles, 0, j);
                links.add(new PreparedLink(linkId, tab, cardKey, relX0, relX1));
            }
            i = j;
        }

        return new PreparedLine(seq, List.copyOf(cps), List.copyOf(cleanStyles), linkIds, List.copyOf(links));
    }

    private int widthOfSlice(List<Integer> cps, List<Style> styles, int start, int end) {
        if (start >= end) return 0;
        FormattedCharSequence slice = sink -> {
            for (int i = start; i < end; i++) {
                if (!sink.accept(i, styles.get(i), cps.get(i))) {
                    return false;
                }
            }
            return true;
        };
        return this.font.width(slice);
    }

    private static MutableComponent buildUnderlinedLineComponent(PreparedLine line, int targetLinkId) {
        List<Integer> cps = line.cps();
        List<Style> styles = line.styles();
        int[] linkIds = line.linkIds();
        int n = cps.size();
        MutableComponent out = Component.empty();
        if (n == 0) return out;

        StringBuilder run = new StringBuilder();
        Style runStyle = (linkIds[0] == targetLinkId) ? styles.get(0).withUnderlined(true) : styles.get(0);
        for (int i = 0; i < n; i++) {
            Style st = (linkIds[i] == targetLinkId) ? styles.get(i).withUnderlined(true) : styles.get(i);
            if (!Objects.equals(st, runStyle)) {
                out.append(Component.literal(run.toString()).withStyle(runStyle));
                run.setLength(0);
                runStyle = st;
            }
            run.appendCodePoint(cps.get(i));
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(runStyle));
        }
        return out;
    }

    private static MutableComponent buildStyledComponent(List<Integer> cps, List<Style> styles, int start, int end) {
        MutableComponent out = Component.empty();
        if (start >= end) return out;
        StringBuilder run = new StringBuilder();
        Style runStyle = styles.get(start);
        for (int i = start; i < end; i++) {
            Style st = styles.get(i);
            if (!Objects.equals(st, runStyle)) {
                out.append(Component.literal(run.toString()).withStyle(runStyle));
                run.setLength(0);
                runStyle = st;
            }
            run.appendCodePoint(cps.get(i));
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(runStyle));
        }
        return out;
    }

    private int computeCardScrollOffset(Tab tab, String targetCardKey, int innerW, int contentH) {
        CachedTabContent content = getOrBuildTabContent(tab, innerW);
        int cardOffset = (targetCardKey != null) ? content.cardOffsets().getOrDefault(targetCardKey, 0) : 0;
        int maxS = Math.max(0, content.totalHeight() - contentH);
        return Math.max(0, Math.min(maxS, cardOffset));
    }

    private boolean isMouseOverBox(int x, int y, int w, int h, int mouseX, int mouseY) {
        return this.canTriggerContentHover && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void renderHoveredItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack == null || stack.isEmpty()) return;
        if (isMouseOverBox(x, y, 16, 16, mouseX, mouseY)) {
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x33FFFFFF);
            this.pendingTooltipItem = stack;
            this.pendingTooltipLines = null;
        }
        guiGraphics.renderItem(stack, x, y);
    }

    private void renderHoveredElement(GuiGraphics guiGraphics, Element element, int x, int y, int mouseX, int mouseY) {
        if (element == null) return;
        Component tooltip = Component.empty()
                .append(Component.literal(element.iconString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(Component.translatable(element.translationKey()).withStyle(element.color()));
        renderHoveredIconTexture(guiGraphics, ClientAuraRenderer.getElementIconTexture(element), tooltip, x, y, mouseX, mouseY);
    }

    private void renderHoveredOmniElement(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        renderHoveredTextureBadge(guiGraphics, ClientAuraRenderer.getOmniIconTexture(), Element.omniIconString(), "element.velorise_staves.omni", ChatFormatting.GOLD, x, y, mouseX, mouseY);
    }

    private void renderHoveredResonanceIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        renderHoveredTextureBadge(guiGraphics, ClientAuraRenderer.getResonanceIconTexture(), Element.resonanceIconString(), "config.velorise_staves.resonance", ChatFormatting.AQUA, x, y, mouseX, mouseY);
    }

    private void renderHoveredTextureBadge(
            GuiGraphics guiGraphics,
            ResourceLocation tex,
            String glyph,
            String translationKey,
            ChatFormatting color,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        MutableComponent tooltip = Component.empty();
        if (glyph != null && !glyph.isEmpty()) {
            tooltip.append(Component.literal(glyph).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" "));
        }
        tooltip.append(Component.translatable(translationKey).withStyle(color));
        renderHoveredIconTexture(guiGraphics, tex, tooltip, x, y, mouseX, mouseY);
    }

    private void renderHoveredIconTexture(
            GuiGraphics guiGraphics,
            ResourceLocation tex,
            Component tooltip,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        if (tex == null) return;
        if (isMouseOverBox(x, y, 16, 16, mouseX, mouseY)) {
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x33FFFFFF);
            this.pendingTooltipItem = ItemStack.EMPTY;
            this.pendingTooltipLines = List.of(tooltip);
        }
        drawGuiTextureIcon(guiGraphics, tex, x, y);
    }

    private void renderHoveredMob(GuiGraphics guiGraphics, EntityType<? extends LivingEntity> type, int x, int y, float size, int mouseX, int mouseY) {
        if (type == null) return;
        if (isMouseOverBox(x, y, 16, 16, mouseX, mouseY)) {
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0x33FFFFFF);
            this.pendingTooltipItem = ItemStack.EMPTY;
            this.pendingTooltipLines = List.of(type.getDescription().copy().withStyle(ChatFormatting.GOLD));
        }
        renderGuiMob(guiGraphics, type, x, y, size);
    }

    private void drawElementIcon(GuiGraphics guiGraphics, Element element, int x, int y) {
        drawGuiTextureIcon(guiGraphics, ClientAuraRenderer.getElementIconTexture(element), x, y);
    }

    private void drawGuiTextureIcon(GuiGraphics guiGraphics, ResourceLocation tex, int x, int y) {
        if (tex != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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

            // Check Clickable Reaction Name Links inside Content Area
            int contentX = sideX + SIDEBAR_W + 4;
            int contentW = winW - SIDEBAR_W - 12;
            int sbH = winH - HEADER_H - 8;
            if (this.pendingClickTab != null) {
                this.currentTab = this.pendingClickTab;
                int innerW = contentW - 18;
                this.scrollOffset = computeCardScrollOffset(this.currentTab, this.pendingClickCardKey, innerW, sbH);
                this.pendingClickTab = null;
                this.pendingClickCardKey = null;
                playClickSound();
                return true;
            }

            // Check Scrollbar drag start
            int sbX = contentX + contentW - 10;
            int sbY = sideY;
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
