package com.velorise.staves.client;

import com.velorise.staves.audio.CharacterVoiceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

public class CharacterVoiceScreen extends Screen {
    private final Screen parent;

    private record CharRowBounds(String charId, int x, int y, int width, int height) {}
    private final List<CharRowBounds> charRowBounds = new ArrayList<>();
    private int globalLangBtnX = -1;
    private int globalLangBtnY = -1;
    private int globalLangBtnW = 0;
    private int globalLangBtnH = 0;

    public CharacterVoiceScreen(Screen parent) {
        super(Component.translatable("config.velorise_staves.voice_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.charRowBounds.clear();
        int centerX = this.width / 2;
        int colWidth = 150;
        int colGap = 12;
        int leftColX = centerX - colWidth - (colGap / 2);
        int rightColX = centerX + (colGap / 2);
        int startY = 36;
        int rowHeight = 23;

        // Top Row 1: Master Voice (Left) & Global Language (Right)
        this.addRenderableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_master_mode." + val))
                .withValues(List.of("all", "self_only", "disabled"))
                .withInitialValue(CharacterVoiceManager.getMasterMode())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.voice_master_mode.desc")))
                .create(leftColX, startY, colWidth, 20, Component.translatable("config.velorise_staves.voice_master_mode", ""), (btn, val) -> {
                    CharacterVoiceManager.setMasterMode(val);
                }));

        List<String> globalLangOpts = List.of("auto", "en_us", "zh_cn", "ja_jp", "ko_kr");
        String curGlobalLang = CharacterVoiceManager.getGlobalLanguage();
        if (!globalLangOpts.contains(curGlobalLang)) {
            curGlobalLang = "auto";
        }
        this.globalLangBtnX = rightColX;
        this.globalLangBtnY = startY;
        this.globalLangBtnW = colWidth;
        this.globalLangBtnH = 20;
        this.addRenderableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_language." + val))
                .withValues(globalLangOpts)
                .withInitialValue(curGlobalLang)
                .create(rightColX, startY, colWidth, 20, Component.translatable("config.velorise_staves.voice_language", ""), (btn, val) -> {
                    CharacterVoiceManager.setGlobalLanguage(val);
                }));

        // Top Row 2: Voice Volume (Left) & Reset Defaults (Right)
        List<Float> volumes = List.of(1.0F, 0.8F, 0.6F, 0.4F, 0.2F, 0.0F);
        float curVol = CharacterVoiceManager.getVoiceVolume();
        Float initVol = volumes.stream().min((a, b) -> Float.compare(Math.abs(a - curVol), Math.abs(b - curVol))).orElse(1.0F);

        this.addRenderableWidget(CycleButton.<Float>builder(val -> Component.literal(Math.round(val * 100.0F) + "%"))
                .withValues(volumes)
                .withInitialValue(initVol)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.voice_volume.desc")))
                .create(leftColX, startY + rowHeight, colWidth, 20, Component.translatable("config.velorise_staves.voice_volume", ""), (btn, val) -> {
                    CharacterVoiceManager.setVoiceVolume(val);
                }));

        this.addRenderableWidget(Button.builder(Component.translatable("config.velorise_staves.reset"), btn -> {
            CharacterVoiceManager.resetDefaults();
            this.clearWidgets();
            this.init();
        }).bounds(rightColX, startY + rowHeight, colWidth, 20).build());

        // Middle Section: 10 Characters in 2 columns
        int charStartY = startY + rowHeight * 2 + 18;
        List<String> chars = CharacterVoiceManager.CHARACTERS;
        List<String> langOpts = List.of("inherit", "en_us", "zh_cn", "ja_jp", "ko_kr");

        for (int i = 0; i < chars.size(); i++) {
            String charId = chars.get(i);
            boolean isLeft = (i % 2 == 0);
            int rowIdx = i / 2;
            int x = isLeft ? leftColX : rightColX;
            int y = charStartY + rowIdx * rowHeight;

            int toggleW = 85;
            int langW = 63;

            // Character ON/OFF button
            this.addRenderableWidget(CycleButton.onOffBuilder(CharacterVoiceManager.isCharacterVoiceEnabled(charId))
                    .create(x, y, toggleW, 20, Component.translatable("config.velorise_staves.character." + charId), (btn, val) -> {
                        CharacterVoiceManager.setCharacterVoiceEnabled(charId, val);
                    }));

            // Character Language button
            String curCharLang = CharacterVoiceManager.getCharacterVoiceLanguage(charId);
            if (!langOpts.contains(curCharLang)) {
                curCharLang = "inherit";
            }
            this.addRenderableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_char_lang." + val))
                    .withValues(langOpts)
                    .withInitialValue(curCharLang)
                    .create(x + toggleW + 2, y, langW, 20, Component.empty(), (btn, val) -> {
                        CharacterVoiceManager.setCharacterVoiceLanguage(charId, val);
                    }));

            this.charRowBounds.add(new CharRowBounds(charId, x, y, toggleW + 2 + langW, 20));
        }

        // Bottom: Done Button
        int doneW = 120;
        int doneY = charStartY + 5 * rowHeight + 8;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            CharacterVoiceManager.saveConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX - doneW / 2, doneY, doneW, 20).build());
    }

    private List<Component> buildCharacterVoiceTooltip(String charId) {
        List<Component> lines = new ArrayList<>();
        int rgb = CharacterVoiceManager.getCharacterElementColorRgb(charId);
        boolean enabled = CharacterVoiceManager.isCharacterVoiceEnabled(charId);

        Component charName = Component.translatable("config.velorise_staves.character." + charId)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true));
        Component stateBadge = Component.translatable(enabled
                        ? "config.velorise_staves.voice_tooltip.state_on"
                        : "config.velorise_staves.voice_tooltip.state_off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        lines.add(Component.literal("✦ ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
                .append(charName)
                .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                .append(stateBadge));

        String effectiveLang = CharacterVoiceManager.getEffectiveVoiceLanguage(charId);
        boolean currentAvailable = CharacterVoiceManager.hasVoiceForLanguage(charId, effectiveLang);
        Component effLangName = Component.translatable("config.velorise_staves.voice_tooltip.lang_full." + effectiveLang)
                .withStyle(ChatFormatting.AQUA);
        Component currentStatus = Component.translatable(currentAvailable
                        ? "config.velorise_staves.voice_tooltip.available"
                        : "config.velorise_staves.voice_tooltip.unavailable")
                .withStyle(currentAvailable ? ChatFormatting.GREEN : ChatFormatting.RED);
        lines.add(Component.translatable("config.velorise_staves.voice_tooltip.current_lang", effLangName, currentStatus)
                .withStyle(ChatFormatting.GRAY));

        lines.add(Component.empty());
        lines.add(Component.translatable("config.velorise_staves.voice_tooltip.all_langs_header")
                .withStyle(ChatFormatting.GOLD));

        for (String langCode : CharacterVoiceManager.VOICE_LANGUAGES) {
            boolean hasVoice = CharacterVoiceManager.hasVoiceForLanguage(charId, langCode);
            boolean isSelected = langCode.equalsIgnoreCase(effectiveLang);
            Component bullet = Component.literal(isSelected ? " ▶ " : " • ")
                    .withStyle(isSelected ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY);
            Component langLabel = Component.translatable("config.velorise_staves.voice_tooltip.lang_full." + langCode)
                    .withStyle(isSelected ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            Component statusLabel = Component.translatable(hasVoice
                            ? "config.velorise_staves.voice_tooltip.available"
                            : "config.velorise_staves.voice_tooltip.unavailable")
                    .withStyle(hasVoice ? ChatFormatting.GREEN : ChatFormatting.RED);
            lines.add(Component.empty()
                    .append(bullet)
                    .append(langLabel)
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(statusLabel));
        }

        return lines;
    }

    private List<Component> buildGlobalLanguageTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("config.velorise_staves.voice_tooltip.global_title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        lines.add(Component.translatable("config.velorise_staves.voice_language.desc")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable("config.velorise_staves.voice_tooltip.all_langs_header")
                .withStyle(ChatFormatting.AQUA));
        int missingCount = 0;
        for (String charId : CharacterVoiceManager.CHARACTERS) {
            boolean anyAvailable = false;
            net.minecraft.network.chat.MutableComponent statusRow = Component.empty();
            for (int i = 0; i < CharacterVoiceManager.VOICE_LANGUAGES.size(); i++) {
                String langCode = CharacterVoiceManager.VOICE_LANGUAGES.get(i);
                boolean has = CharacterVoiceManager.hasVoiceForLanguage(charId, langCode);
                if (has) anyAvailable = true;
                if (i > 0) {
                    statusRow.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                statusRow.append(Component.translatable("config.velorise_staves.voice_char_lang." + langCode)
                        .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                statusRow.append(Component.literal(has ? " ✔" : " ✖")
                        .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            if (anyAvailable) {
                lines.add(Component.literal("• ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("config.velorise_staves.character." + charId)
                                .withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(statusRow));
            } else {
                missingCount++;
            }
        }
        if (missingCount > 0) {
            lines.add(Component.literal("• (" + missingCount + ") ").withStyle(ChatFormatting.RED)
                    .append(Component.translatable("config.velorise_staves.voice_tooltip.unavailable")
                            .withStyle(ChatFormatting.RED)));
        }
        return lines;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("config.velorise_staves.voice_settings.subtitle"), this.width / 2, 22, 0x88CCFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("config.velorise_staves.voice_char_title"), this.width / 2, 36 + 23 * 2 + 6, 0xE8CAFF);

        if (mouseX >= this.globalLangBtnX && mouseX < this.globalLangBtnX + this.globalLangBtnW
                && mouseY >= this.globalLangBtnY && mouseY < this.globalLangBtnY + this.globalLangBtnH) {
            guiGraphics.renderComponentTooltip(this.font, buildGlobalLanguageTooltip(), mouseX, mouseY);
            return;
        }
        for (CharRowBounds row : this.charRowBounds) {
            if (mouseX >= row.x() && mouseX < row.x() + row.width()
                    && mouseY >= row.y() && mouseY < row.y() + row.height()) {
                guiGraphics.renderComponentTooltip(this.font, buildCharacterVoiceTooltip(row.charId()), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void onClose() {
        CharacterVoiceManager.saveConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
