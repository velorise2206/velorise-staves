package com.velorise.staves.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CharacterVoiceManager {
    private CharacterVoiceManager() {}

    public static final List<String> CHARACTERS = List.of(
            "zhongli", "raiden", "venti", "nahida", "furina",
            "neuvillette", "mavuika", "xilonen", "tsaritsa", "layla"
    );

    public static final class VoiceConfigData {
        public String masterMode = "all"; // "all", "self_only", "disabled"
        public String globalLanguage = "auto"; // "auto", "en_us", "zh_cn", "ja_jp"
        public float volume = 1.0F;
        public Map<String, Boolean> enabledCharacters = new ConcurrentHashMap<>();
        public Map<String, String> characterLanguages = new ConcurrentHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VoiceConfigData configData = null;
    private static final java.util.Map<Integer, Long> LAST_VOICE_TIME = new ConcurrentHashMap<>();

    private static synchronized VoiceConfigData getConfig() {
        if (configData == null) {
            loadConfig();
        }
        return configData;
    }

    private static File getConfigFile() {
        File dir = Minecraft.getInstance().gameDirectory;
        return new File(dir, "config/velorise_staves_voice.json");
    }

    public static synchronized void loadConfig() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                configData = GSON.fromJson(reader, VoiceConfigData.class);
            } catch (Throwable ignored) {}
        }
        if (configData == null) {
            configData = new VoiceConfigData();
            String cfgLang = StavesConfig.getCharacterVoiceLanguage();
            if (cfgLang != null && !cfgLang.isBlank()) {
                if ("disabled".equalsIgnoreCase(cfgLang) || "off".equalsIgnoreCase(cfgLang)) {
                    configData.masterMode = "disabled";
                } else {
                    configData.globalLanguage = cfgLang.toLowerCase(Locale.ROOT);
                }
            }
            saveConfig();
        }
    }

    public static synchronized void saveConfig() {
        if (configData == null) return;
        try {
            File file = getConfigFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(configData, writer);
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized void resetDefaults() {
        configData = new VoiceConfigData();
        saveConfig();
    }

    public static String getMasterMode() {
        return getConfig().masterMode;
    }

    public static void setMasterMode(String mode) {
        getConfig().masterMode = mode;
        saveConfig();
    }

    public static String getGlobalLanguage() {
        return getConfig().globalLanguage;
    }

    public static void setGlobalLanguage(String lang) {
        getConfig().globalLanguage = lang;
        saveConfig();
    }

    public static float getVoiceVolume() {
        return getConfig().volume;
    }

    public static void setVoiceVolume(float vol) {
        getConfig().volume = Math.max(0.0F, Math.min(1.0F, vol));
        saveConfig();
    }

    public static boolean isCharacterVoiceEnabled(String charId) {
        Boolean b = getConfig().enabledCharacters.get(charId);
        return b == null || b;
    }

    public static void setCharacterVoiceEnabled(String charId, boolean enabled) {
        getConfig().enabledCharacters.put(charId, enabled);
        saveConfig();
    }

    public static String getCharacterVoiceLanguage(String charId) {
        return getConfig().characterLanguages.getOrDefault(charId, "inherit");
    }

    public static void setCharacterVoiceLanguage(String charId, String lang) {
        getConfig().characterLanguages.put(charId, lang);
        saveConfig();
    }

    public static boolean isVoiceMuted(int entityId) {
        String mode = getMasterMode();
        if ("disabled".equalsIgnoreCase(mode) || "off".equalsIgnoreCase(mode) || "none".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("self_only".equalsIgnoreCase(mode)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && entityId != mc.player.getId()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVoiceAllowed(int entityId, String charId) {
        if (isVoiceMuted(entityId)) return false;
        if (getVoiceVolume() <= 0.001F) return false;
        return isCharacterVoiceEnabled(charId);
    }

    public static String getEffectiveVoiceLanguage(String charId) {
        String charLang = getCharacterVoiceLanguage(charId);
        String target = "inherit".equalsIgnoreCase(charLang) ? getGlobalLanguage() : charLang;
        if ("auto".equalsIgnoreCase(target)) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getLanguageManager() != null) {
                    String selected = mc.getLanguageManager().getSelected();
                    if (selected != null) {
                        String lower = selected.toLowerCase(Locale.ROOT);
                        if (lower.startsWith("zh")) return "zh_cn";
                        if (lower.startsWith("ja")) return "ja_jp";
                        if (lower.startsWith("ko")) return "ko_kr";
                    }
                }
            } catch (Throwable ignored) {}
            return "en_us";
        }
        return target.toLowerCase(Locale.ROOT);
    }

    public static boolean isClientChineseVoice() {
        return "zh_cn".equals(getEffectiveVoiceLanguage("zhongli"));
    }

    public static boolean isVoiceEnabled() {
        return !"disabled".equalsIgnoreCase(getMasterMode());
    }

    public static void playVoice(int entityId, String charId, long debounceMs, Object soundEn, Object soundCn, Object soundJp) {
        if (!isVoiceAllowed(entityId, charId)) return;
        long now = System.currentTimeMillis();
        Long last = LAST_VOICE_TIME.get(entityId);
        if (last != null && (now - last) < debounceMs) {
            return;
        }
        LAST_VOICE_TIME.put(entityId, now);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        double x = entity != null ? entity.getX() : (mc.player != null ? mc.player.getX() : 0.0D);
        double y = entity != null ? entity.getY() : (mc.player != null ? mc.player.getY() : 0.0D);
        double z = entity != null ? entity.getZ() : (mc.player != null ? mc.player.getZ() : 0.0D);

        String lang = getEffectiveVoiceLanguage(charId);
        Object chosen = soundEn;
        if ("zh_cn".equals(lang) && soundCn != null) chosen = soundCn;
        else if ("ja_jp".equals(lang) && soundJp != null) chosen = soundJp;

        SoundEvent se = getSound(chosen);
        if (se != null) {
            float vol = getVoiceVolume();
            mc.level.playLocalSound(x, y, z, se, SoundSource.PLAYERS, vol, 1.0F, false);
        }
    }

    public static void playClientZhongliVoice(int entityId, boolean holdShield) {
        playVoice(entityId, "zhongli", 1400L,
                holdShield ? VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_EN : VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_EN,
                holdShield ? VeloriseStavesMod.VOICE_ZHONGLI_E_HOLD_CN : VeloriseStavesMod.VOICE_ZHONGLI_E_TAP_CN,
                null);
    }

    public static void playClientZhongliBurstVoice(int entityId) {
        playVoice(entityId, "zhongli", 2200L,
                VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_EN,
                VeloriseStavesMod.VOICE_ZHONGLI_Q_BURST_CN,
                null);
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
