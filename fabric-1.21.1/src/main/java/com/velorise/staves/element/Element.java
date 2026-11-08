package com.velorise.staves.element;

import net.minecraft.ChatFormatting;

/**
 * Elemental Gauge Theory (EGT) Element representation.
 * Pyro, Hydro, Cryo, Electro, and Dendro are lingering Auras.
 * Anemo and Geo are strictly Trigger elements and never linger on entities.
 */
public enum Element {
    PYRO("🔥", "Pyro", ChatFormatting.RED, true),
    HYDRO("💧", "Hydro", ChatFormatting.BLUE, true),
    CRYO("❄️", "Cryo", ChatFormatting.AQUA, true),
    ELECTRO("⚡", "Electro", ChatFormatting.YELLOW, true),
    DENDRO("🌿", "Dendro", ChatFormatting.GREEN, true),
    ANEMO("🌪️", "Anemo", ChatFormatting.DARK_AQUA, false),
    GEO("🪨", "Geo", ChatFormatting.GOLD, false);

    private final String symbol;
    private final String displayName;
    private final ChatFormatting color;
    private final boolean isAura;

    Element(String symbol, String displayName, ChatFormatting color, boolean isAura) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.color = color;
        this.isAura = isAura;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    public boolean isAura() {
        return isAura;
    }

    public String formatted() {
        return color.toString() + symbol + " " + displayName + ChatFormatting.RESET;
    }
}
