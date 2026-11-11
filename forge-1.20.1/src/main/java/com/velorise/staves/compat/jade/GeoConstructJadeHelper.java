package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.GeoFangPillarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

/**
 * Jade helper displaying Geo Construct health progress bar, wave tier specs, and remaining duration.
 */
public enum GeoConstructJadeHelper implements IEntityComponentProvider {
    INSTANCE;

    public static final ResourceLocation UID =
            new ResourceLocation(VeloriseStavesMod.MODID, "geo_construct_health");
    private static final ResourceLocation GEO_TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/geo.png");

    public static void appendServerData(CompoundTag tag, GeoFangPillarEntity pillar) {
        tag.putFloat("vs_pillar_hp", pillar.getHealth());
        tag.putFloat("vs_pillar_max_hp", pillar.getMaxConstructHealth());
        tag.putBoolean("vs_pillar_neth", pillar.isNetherite());
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
        boolean neth = (data != null && data.contains("vs_pillar_neth")) ? data.getBoolean("vs_pillar_neth") : pillar.isNetherite();
        int remSec = (data != null && data.contains("vs_pillar_rem_sec"))
                ? data.getInt("vs_pillar_rem_sec")
                : Math.max(0, (GeoFangPillarEntity.MAX_LIFETIME_TICKS - pillar.tickCount + 19) / 20);

        float ratio = Mth.clamp(hp / maxHp, 0.0F, 1.0F);
        int cTop = ratio > 0.5F ? 0xFFE6A82E : (ratio > 0.25F ? 0xFFE67E22 : 0xFFE03E3E);
        int cBot = ratio > 0.5F ? 0xFFB87814 : (ratio > 0.25F ? 0xFF9C4D0E : 0xFF961B1B);

        IElementHelper helper = IElementHelper.get();
        Component barLabel = Component.literal(fmt(hp) + " / " + fmt(maxHp) + " \u2665");
        tooltip.add(new AuraComponentProvider.ElementIcon(GEO_TEXTURE, 12, 12));
        tooltip.append(helper.spacer(3, 0));
        tooltip.append(helper.progress(
                ratio,
                barLabel,
                helper.progressStyle().color(cTop, cBot).textColor(0xFFFFFFFF),
                BoxStyle.DEFAULT,
                true
        ));

        String specs = neth ? "1x2x1 • 5m (1\u2665 / 1U/2s)" : "1x1x1 • 3m (0.5\u2665 / 0.5U/2s)";
        tooltip.add(Component.literal(specs).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" • " + remSec + "s").withStyle(ChatFormatting.GRAY)));
    }

    private static String fmt(float val) {
        return Math.abs(val - Math.round(val)) < 0.05F
                ? String.valueOf(Math.round(val))
                : String.format(Locale.ROOT, "%.1f", val);
    }
}
