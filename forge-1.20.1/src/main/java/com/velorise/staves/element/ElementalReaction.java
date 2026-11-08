package com.velorise.staves.element;

import net.minecraft.ChatFormatting;

/**
 * Elemental reactions and their mathematical modifiers according to Elemental Gauge Theory.
 */
public enum ElementalReaction {
    NONE("", "", 0.0F, 1.0F, ChatFormatting.WHITE),
    FORWARD_VAPORIZE("Vaporize", "💧 + 🔥", 2.0F, 2.0F, ChatFormatting.BLUE),
    REVERSE_VAPORIZE("Vaporize", "🔥 + 💧", 0.5F, 1.5F, ChatFormatting.RED),
    FORWARD_MELT("Melt", "🔥 + ❄️", 2.0F, 2.0F, ChatFormatting.RED),
    REVERSE_MELT("Melt", "❄️ + 🔥", 0.5F, 1.5F, ChatFormatting.AQUA),
    OVERLOADED("Overloaded", "🔥 + ⚡", 1.0F, 1.0F, ChatFormatting.GOLD),
    SUPERCONDUCT("Superconduct", "❄️ + ⚡", 1.0F, 1.0F, ChatFormatting.AQUA),
    FROZEN("Frozen", "💧 + ❄️", 1.0F, 1.0F, ChatFormatting.AQUA),
    SHATTER("Shatter", "🪨 + 🧊", 1.0F, 1.0F, ChatFormatting.WHITE),
    ELECTRO_CHARGED("Electro-Charged", "💧 + ⚡", 0.4F, 1.0F, ChatFormatting.YELLOW),
    BURNING("Burning", "🔥 + 🌿", 0.4F, 1.0F, ChatFormatting.RED),
    QUICKEN("Quicken", "🌿 + ⚡", 1.0F, 1.0F, ChatFormatting.GREEN),
    AGGRAVATE("Aggravate", "⚡ + Quicken", 0.0F, 1.0F, ChatFormatting.YELLOW),
    SPREAD("Spread", "🌿 + Quicken", 0.0F, 1.0F, ChatFormatting.DARK_GREEN),
    BLOOM("Bloom", "💧 + 🌿", 1.0F, 1.0F, ChatFormatting.GREEN),
    OVERVAPE("Overvape", "🔥 + (💧 & ⚡)", 0.5F, 1.5F, ChatFormatting.GOLD),
    SWIRL("Swirl", "🌪️ + Aura", 0.5F, 1.0F, ChatFormatting.DARK_AQUA),
    CRYSTALLIZE("Crystallize", "🪨 + Aura", 0.5F, 1.0F, ChatFormatting.GOLD);

    private final String name;
    private final String formula;
    private final float consumptionModifier;
    private final float damageMultiplier;
    private final ChatFormatting color;

    ElementalReaction(String name, String formula, float consumptionModifier, float damageMultiplier, ChatFormatting color) {
        this.name = name;
        this.formula = formula;
        this.consumptionModifier = consumptionModifier;
        this.damageMultiplier = damageMultiplier;
        this.color = color;
    }

    public String reactionName() {
        return name;
    }

    public String formula() {
        return formula;
    }

    public float getConsumptionModifier() {
        return consumptionModifier;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public ChatFormatting color() {
        return color;
    }
}
