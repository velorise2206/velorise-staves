package com.velorise.staves.compat.jade;

import com.mojang.blaze3d.systems.RenderSystem;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.GeoFangPillarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

/**
 * Jade helper displaying Geo Construct vanilla hearts and remaining duration.
 */
public enum GeoConstructJadeHelper implements IEntityComponentProvider {
    INSTANCE;

    public static final ResourceLocation UID =
            new ResourceLocation(VeloriseStavesMod.MODID, "geo_construct_health");
    private static final ResourceLocation GEO_TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/geo.png");
    private static final ResourceLocation VANILLA_ICONS =
            new ResourceLocation("textures/gui/icons.png");

    private static final class VanillaHeartsElement extends Element {
        private static final int ICONS_PER_LINE = 10;
        private static final int MAX_RENDERED_HP = 40;

        private final float hp;
        private final float maxHp;
        private final int heartCount;
        private final int iconsPerLine;
        private final int lineCount;
        private final String compactText;

        private VanillaHeartsElement(float hp, float maxHp) {
            this.hp = Math.max(0.0F, hp);
            this.maxHp = Math.max(1.0F, maxHp);
            if (this.maxHp > MAX_RENDERED_HP) {
                this.compactText = fmt(this.hp) + " / " + fmt(this.maxHp);
                this.heartCount = 1;
                this.iconsPerLine = 1;
                this.lineCount = 1;
            } else {
                this.compactText = null;
                this.heartCount = Math.max(1, Mth.ceil(this.maxHp * 0.5F));
                this.iconsPerLine = Math.min(ICONS_PER_LINE, this.heartCount);
                this.lineCount = Math.max(1, Mth.ceil((float) this.heartCount / ICONS_PER_LINE));
            }
        }

        @Override
        public Vec2 getSize() {
            if (this.compactText != null) {
                return new Vec2(11 + Minecraft.getInstance().font.width(this.compactText), 9);
            }
            return new Vec2(8 * this.iconsPerLine + 1, this.lineCount == 1 ? 9 : this.lineCount * 10 - 1);
        }

        @Override
        public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
            int baseX = Math.round(x);
            int baseY = Math.round(y);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (this.compactText != null) {
                guiGraphics.blit(VANILLA_ICONS, baseX, baseY, 16, 0, 9, 9, 256, 256);
                if (this.hp > 0.0F) {
                    guiGraphics.blit(VANILLA_ICONS, baseX, baseY, 52, 0, 9, 9, 256, 256);
                }
                RenderSystem.disableBlend();
                guiGraphics.drawString(Minecraft.getInstance().font, this.compactText, baseX + 11, baseY + 1, 0xFFFFFFFF, true);
                return;
            }

            int filledHalfHearts = this.hp > 0.0F ? Mth.ceil(Math.min(this.hp, this.maxHp)) : 0;
            for (int i = 0; i < this.heartCount; i++) {
                int col = i % ICONS_PER_LINE;
                int row = i / ICONS_PER_LINE;
                int hx = baseX + col * 8;
                int hy = baseY + row * 10;
                guiGraphics.blit(VANILLA_ICONS, hx, hy, 16, 0, 9, 9, 256, 256);
                int fullThreshold = (i + 1) * 2;
                if (filledHalfHearts >= fullThreshold) {
                    guiGraphics.blit(VANILLA_ICONS, hx, hy, 52, 0, 9, 9, 256, 256);
                } else if (filledHalfHearts == fullThreshold - 1) {
                    guiGraphics.blit(VANILLA_ICONS, hx, hy, 61, 0, 9, 9, 256, 256);
                }
            }
            RenderSystem.disableBlend();
        }
    }

    public static void appendServerData(CompoundTag tag, GeoFangPillarEntity pillar) {
        tag.putFloat("vs_pillar_hp", pillar.getHealth());
        tag.putFloat("vs_pillar_max_hp", pillar.getMaxConstructHealth());
        int remTicks = Math.max(0, GeoFangPillarEntity.MAX_LIFETIME_TICKS - pillar.getAgeTicks());
        tag.putInt("vs_pillar_rem_sec", (remTicks + 19) / 20);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof GeoFangPillarEntity pillar)) return;
        CompoundTag data = accessor.getServerData();

        float hp = (data != null && data.contains("vs_pillar_hp")) ? data.getFloat("vs_pillar_hp") : pillar.getHealth();
        float maxHp = Math.max(1.0F, (data != null && data.contains("vs_pillar_max_hp"))
                ? data.getFloat("vs_pillar_max_hp") : pillar.getMaxConstructHealth());
        int remSec = (data != null && data.contains("vs_pillar_rem_sec"))
                ? data.getInt("vs_pillar_rem_sec")
                : Math.max(0, (GeoFangPillarEntity.MAX_LIFETIME_TICKS - pillar.tickCount + 19) / 20);

        IElementHelper helper = IElementHelper.get();
        tooltip.add(new VanillaHeartsElement(hp, maxHp));
        tooltip.add(new AuraComponentProvider.ElementIcon(GEO_TEXTURE, 10, 10));
        tooltip.append(helper.spacer(3, 0));
        tooltip.append(helper.text(Component.literal(remSec + "s").withStyle(ChatFormatting.GOLD)));
    }

    private static String fmt(float val) {
        return Math.abs(val - Math.round(val)) < 0.05F
                ? String.valueOf(Math.round(val))
                : String.format(Locale.ROOT, "%.1f", val);
    }
}
