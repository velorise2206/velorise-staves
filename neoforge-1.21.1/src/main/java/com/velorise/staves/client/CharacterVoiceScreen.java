package com.velorise.staves.client;

import com.velorise.staves.audio.CharacterVoiceManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CharacterVoiceScreen extends Screen {
    private final Screen parent;

    public CharacterVoiceScreen(Screen parent) {
        super(Component.translatable("config.velorise_staves.voice_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
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

        this.addRenderableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_language." + val))
                .withValues(List.of("auto", "en_us", "zh_cn", "ja_jp"))
                .withInitialValue(CharacterVoiceManager.getGlobalLanguage())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.voice_language.desc")))
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
        List<String> langOpts = List.of("inherit", "en_us", "zh_cn", "ja_jp");

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
            this.addRenderableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_char_lang." + val))
                    .withValues(langOpts)
                    .withInitialValue(CharacterVoiceManager.getCharacterVoiceLanguage(charId))
                    .create(x + toggleW + 2, y, langW, 20, Component.empty(), (btn, val) -> {
                        CharacterVoiceManager.setCharacterVoiceLanguage(charId, val);
                    }));
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("config.velorise_staves.voice_settings.subtitle"), this.width / 2, 22, 0x88CCFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("config.velorise_staves.voice_char_title"), this.width / 2, 36 + 23 * 2 + 6, 0xE8CAFF);
    }

    @Override
    public void onClose() {
        CharacterVoiceManager.saveConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
