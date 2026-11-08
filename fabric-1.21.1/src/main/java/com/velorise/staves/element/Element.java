package com.velorise.staves.element;

import net.minecraft.ChatFormatting;

/**
 * Elemental Gauge Theory (EGT) Element representation.
 * Pyro, Hydro, Cryo, Electro, and Dendro are lingering Auras.
 * Anemo and Geo are strictly Trigger elements and never linger on entities.
 */
public enum Element {
    PYRO("🔥", "Hỏa", ChatFormatting.RED, true),
    HYDRO("💧", "Thủy", ChatFormatting.BLUE, true),
    CRYO("❄️", "Băng", ChatFormatting.AQUA, true),
    ELECTRO("⚡", "Lôi", ChatFormatting.YELLOW, true),
    DENDRO("🌿", "Thảo", ChatFormatting.GREEN, true),
    ANEMO("🌪️", "Phong", ChatFormatting.DARK_AQUA, false),
    GEO("🪨", "Nham", ChatFormatting.GOLD, false);

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
