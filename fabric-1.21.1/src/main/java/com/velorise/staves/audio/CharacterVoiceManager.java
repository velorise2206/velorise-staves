package com.velorise.staves.audio;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class CharacterVoiceManager {
    private CharacterVoiceManager() {}

    public static boolean isChineseVoice(Player player) {
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
        // "auto": determine from player's client language
        if (player instanceof ServerPlayer sp) {
            try {
                // In 1.21.1 Mojang mappings: sp.clientInformation().language()
                java.lang.reflect.Method mInfo = sp.getClass().getMethod("clientInformation");
                Object info = mInfo.invoke(sp);
                if (info != null) {
                    java.lang.reflect.Method mLang = info.getClass().getMethod("language");
                    String lang = (String) mLang.invoke(info);
                    if (lang != null && lang.toLowerCase().startsWith("zh")) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                try {
                    // In 1.20.1 / fallback
                    java.lang.reflect.Method mLang = sp.getClass().getMethod("getLanguage");
                    String lang = (String) mLang.invoke(sp);
                    if (lang != null && lang.toLowerCase().startsWith("zh")) {
                        return true;
                    }
                } catch (Throwable ignored2) {}
            }
        }
        return false;
    }

    public static boolean isVoiceEnabled() {
        String cfg = StavesConfig.getCharacterVoiceLanguage();
        return !"disabled".equalsIgnoreCase(cfg) && !"off".equalsIgnoreCase(cfg) && !"none".equalsIgnoreCase(cfg);
    }

    public static void playZhongliVoice(ServerPlayer player, boolean holdShield) {
        if (!isVoiceEnabled() || player == null || player.level().isClientSide()) return;
        boolean cn = isChineseVoice(player);
        SoundEvent sound = holdShield
                ? (cn ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_CN) : getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_EN))
                : (cn ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_CN) : getSound(VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_EN));
        if (sound != null) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void playZhongliBurstVoice(ServerPlayer player) {
        if (!isVoiceEnabled() || player == null || player.level().isClientSide()) return;
        boolean cn = isChineseVoice(player);
        SoundEvent sound = cn ? getSound(VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_CN) : getSound(VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_EN);
        if (sound != null) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void playMavuikaBurstVoice(ServerPlayer player) {
        if (!isVoiceEnabled() || player == null || player.level().isClientSide()) return;
        SoundEvent sound = getSound(VeloriseStavesMod.VOICE_MAVUIKA_Q_BURST_EN);
        if (sound != null) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
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
