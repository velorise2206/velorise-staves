package com.velorise.staves.element;

import net.minecraft.ChatFormatting;

/**
 * Elemental reactions and their mathematical modifiers according to Genshin Impact Elemental Gauge Theory (EGT).
 */
public enum ElementalReaction {
    NONE("", "", 0.0F, 1.0F, ChatFormatting.WHITE),
    IMMUNE("IMMUNE", "Immune", 0.0F, 0.0F, ChatFormatting.GRAY),
    FORWARD_VAPORIZE("VAPORIZE", "💧 + 🔥", 2.0F, 2.0F, ChatFormatting.BLUE),
    REVERSE_VAPORIZE("VAPORIZE", "🔥 + 💧", 0.5F, 1.5F, ChatFormatting.RED),
    FORWARD_MELT("MELT", "🔥 + ❄️", 2.0F, 2.0F, ChatFormatting.RED),
    REVERSE_MELT("MELT", "❄️ + 🔥", 0.5F, 1.5F, ChatFormatting.AQUA),
    OVERLOADED("OVERLOADED", "🔥 + ⚡", 1.0F, 1.0F, ChatFormatting.GOLD),
    SUPERCONDUCT("SUPERCONDUCT", "❄️ + ⚡", 1.0F, 1.0F, ChatFormatting.AQUA),
    FROZEN("FROZEN", "💧 + ❄️", 1.0F, 1.0F, ChatFormatting.AQUA),
    SHATTER("SHATTER", "🪨 + 🧊", 1.0F, 1.0F, ChatFormatting.WHITE),
    ELECTRO_CHARGED("ELECTRO-CHARGED", "💧 + ⚡", 0.4F, 1.0F, ChatFormatting.LIGHT_PURPLE),
    BURNING("BURNING", "🔥 + 🌿", 0.4F, 1.0F, ChatFormatting.RED),
    QUICKEN("QUICKEN", "🌿 + ⚡", 1.0F, 1.0F, ChatFormatting.GREEN),
    AGGRAVATE("AGGRAVATE", "⚡ + Quicken", 0.0F, 1.0F, ChatFormatting.LIGHT_PURPLE),
    SPREAD("SPREAD", "🌿 + Quicken", 0.0F, 1.0F, ChatFormatting.DARK_GREEN),
    BLOOM("BLOOM", "💧 + 🌿", 0.5F, 1.0F, ChatFormatting.GREEN),
    REVERSE_BLOOM("BLOOM", "🌿 + 💧", 2.0F, 1.0F, ChatFormatting.GREEN),
    HYPERBLOOM("HYPERBLOOM", "⚡ + 🌱", 1.0F, 1.0F, ChatFormatting.LIGHT_PURPLE),
    BURGEON("BURGEON", "🔥 + 🌱", 1.0F, 1.0F, ChatFormatting.GOLD),
    OVERVAPE("OVERVAPE", "🔥 + (💧 & ⚡)", 0.5F, 1.5F, ChatFormatting.GOLD),
    SWIRL("SWIRL", "🌪️ + Aura", 0.5F, 1.0F, ChatFormatting.DARK_AQUA),
    CRYSTALLIZE("CRYSTALLIZE", "🪨 + Aura", 0.5F, 1.0F, ChatFormatting.GOLD);

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
