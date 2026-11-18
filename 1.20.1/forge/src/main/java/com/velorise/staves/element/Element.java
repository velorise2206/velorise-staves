package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;

import java.util.Locale;

/**
 * Elemental Gauge Theory (EGT) Element representation.
 * Pyro, Hydro, Cryo, Electro, and Dendro are lingering Auras.
 * Anemo and Geo are strictly Trigger elements and never linger on entities.
 */
public enum Element {
    PYRO("🔥", "Pyro", ChatFormatting.RED, true, '\uE001'),
    HYDRO("💧", "Hydro", ChatFormatting.BLUE, true, '\uE002'),
    CRYO("❄️", "Cryo", ChatFormatting.AQUA, true, '\uE003'),
    ELECTRO("⚡", "Electro", ChatFormatting.LIGHT_PURPLE, true, '\uE004'),
    DENDRO("🌿", "Dendro", ChatFormatting.GREEN, true, '\uE007'),
    ANEMO("🌪️", "Anemo", ChatFormatting.DARK_AQUA, false, '\uE005'),
    GEO("🪨", "Geo", ChatFormatting.GOLD, false, '\uE006');

    private final String symbol;
    private final String displayName;
    private final ChatFormatting color;
    private final boolean isAura;
    private final char iconChar;

    Element(String symbol, String displayName, ChatFormatting color, boolean isAura, char iconChar) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.color = color;
        this.isAura = isAura;
        this.iconChar = iconChar;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return "element.velorise_staves." + name().toLowerCase(Locale.ROOT);
    }

    public ChatFormatting color() {
        return color;
    }

    public boolean isAura() {
        return isAura;
    }

    public char iconChar() {
        return StavesConfig.isUse32pxElementIcons() ? (char) (iconChar + 0x10) : iconChar;
    }

    public String iconString() {
        return String.valueOf(iconChar());
    }

    public static String omniIconString() {
        return StavesConfig.isUse32pxElementIcons() ? "\uE01A" : "\uE00A";
    }

    public static String resonanceIconString() {
        return StavesConfig.isUse32pxElementIcons() ? "\uE01B" : "\uE00B";
    }

    public static String moonsignIconString() {
        return StavesConfig.isUse32pxElementIcons() ? "\uE01C" : "\uE00C";
    }

    public String formatted() {
        return color.toString() + iconString() + " " + displayName + ChatFormatting.RESET;
    }
}
