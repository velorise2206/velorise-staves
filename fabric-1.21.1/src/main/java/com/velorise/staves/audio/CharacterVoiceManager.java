package com.velorise.staves.audio;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class CharacterVoiceManager {
    private CharacterVoiceManager() {}

    public static boolean isClientChineseVoice() {
        String cfg = StavesConfig.getCharacterVoiceLanguage();
        if ("zh_cn".equalsIgnoreCase(cfg) || "chinese".equalsIgnoreCase(cfg)) {
            return true;
        }
        if ("en_us".equalsIgnoreCase(cfg) || "english".equalsIgnoreCase(cfg)) {
            return false;
        }
        if ("disabled".equalsIgnoreCase(cfg) || "off".equalsIgnoreCase(cfg) || "none".equalsIgnoreCase(cfg)) {
            return false;
        }
        // "auto": determine from local Minecraft client language
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getLanguageManager() != null) {
                String selected = mc.getLanguageManager().getSelected();
                if (selected != null && selected.toLowerCase().startsWith("zh")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static final java.util.Map<Integer, Long> LAST_VOICE_TIME = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isVoiceEnabled() {
        String cfg = StavesConfig.getCharacterVoiceLanguage();
        return !"disabled".equalsIgnoreCase(cfg) && !"off".equalsIgnoreCase(cfg) && !"none".equalsIgnoreCase(cfg);
    }

    public static void playClientZhongliVoice(int entityId, boolean holdShield) {
        if (!isVoiceEnabled()) return;
        long now = System.currentTimeMillis();
        Long last = LAST_VOICE_TIME.get(entityId);
        if (last != null && (now - last) < 1400L) {
            return;
        }
        LAST_VOICE_TIME.put(entityId, now);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        double x = entity != null ? entity.getX() : (mc.player != null ? mc.player.getX() : 0.0D);
        double y = entity != null ? entity.getY() : (mc.player != null ? mc.player.getY() : 0.0D);
        double z = entity != null ? entity.getZ() : (mc.player != null ? mc.player.getZ() : 0.0D);

        boolean cn = isClientChineseVoice();
        SoundEvent sound = holdShield
                ? (cn ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_CN) : getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_EN))
                : (cn ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_CN) : getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_EN));

        if (sound != null) {
            mc.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    public static void playClientZhongliBurstVoice(int entityId) {
        if (!isVoiceEnabled()) return;
        long now = System.currentTimeMillis();
        Long last = LAST_VOICE_TIME.get(entityId);
        if (last != null && (now - last) < 2200L) {
            return;
        }
        LAST_VOICE_TIME.put(entityId, now);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        double x = entity != null ? entity.getX() : (mc.player != null ? mc.player.getX() : 0.0D);
        double y = entity != null ? entity.getY() : (mc.player != null ? mc.player.getY() : 0.0D);
        double z = entity != null ? entity.getZ() : (mc.player != null ? mc.player.getZ() : 0.0D);

        boolean cn = isClientChineseVoice();
        SoundEvent sound = cn
                ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_CN)
                : getSound(VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_EN);

        if (sound != null) {
            mc.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    private static SoundEvent getSound(Object soundObj) {
        if (soundObj instanceof SoundEvent se) return se;
        if (soundObj instanceof java.util.function.Supplier<?> sup) {
            Object val = sup.get();
            if (val instanceof SoundEvent se) return se;
        }
        return null;
    }
}
