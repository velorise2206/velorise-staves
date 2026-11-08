package com.velorise.staves.element;

import net.minecraft.ChatFormatting;

/**
 * Elemental reactions and their mathematical modifiers according to Elemental Gauge Theory.
 */
public enum ElementalReaction {
    NONE("", "", 0.0F, 1.0F, ChatFormatting.WHITE),
    FORWARD_VAPORIZE("Bốc Hơi Thuận", "💧 + 🔥", 2.0F, 2.0F, ChatFormatting.BLUE),
    REVERSE_VAPORIZE("Bốc Hơi Nghịch", "🔥 + 💧", 0.5F, 1.5F, ChatFormatting.RED),
    FORWARD_MELT("Tan Chảy Thuận", "🔥 + ❄️", 2.0F, 2.0F, ChatFormatting.RED),
    REVERSE_MELT("Tan Chảy Nghịch", "❄️ + 🔥", 0.5F, 1.5F, ChatFormatting.AQUA),
    OVERLOADED("Quá Tải", "🔥 + ⚡", 1.0F, 1.0F, ChatFormatting.GOLD),
    SUPERCONDUCT("Siêu Dẫn", "❄️ + ⚡", 1.0F, 1.0F, ChatFormatting.AQUA),
    FROZEN("Đóng Băng", "💧 + ❄️", 1.0F, 1.0F, ChatFormatting.AQUA),
    SHATTER("Phá Băng", "🪨 + 🧊", 1.0F, 1.0F, ChatFormatting.WHITE),
    ELECTRO_CHARGED("Điện Cảm", "💧 + ⚡", 0.4F, 1.0F, ChatFormatting.YELLOW),
    BURNING("Thiêu Đốt", "🔥 + 🌿", 0.4F, 1.0F, ChatFormatting.RED),
    QUICKEN("Sinh Trưởng", "🌿 + ⚡", 1.0F, 1.0F, ChatFormatting.GREEN),
    AGGRAVATE("Tăng Cường", "⚡ + Sinh Trưởng", 0.0F, 1.0F, ChatFormatting.YELLOW),
    SPREAD("Lan Tràn", "🌿 + Sinh Trưởng", 0.0F, 1.0F, ChatFormatting.DARK_GREEN),
    BLOOM("Sum Xuê", "💧 + 🌿", 1.0F, 1.0F, ChatFormatting.GREEN),
    OVERVAPE("Overvape", "🔥 + (💧 & ⚡)", 0.5F, 1.5F, ChatFormatting.GOLD),
    SWIRL("Khuếch Tán", "🌪️ + Ấn", 0.5F, 1.0F, ChatFormatting.DARK_AQUA),
    CRYSTALLIZE("Kết Tinh", "🪨 + Ấn", 0.5F, 1.0F, ChatFormatting.GOLD);

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
