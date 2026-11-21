// Auto-compiled 14 keyframe animations from cryo_regisvine_boss.bbmodel
package com.velorise.staves.client.model;

import net.minecraft.client.model.geom.ModelPart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CryoRegisvineAnimations {
    private CryoRegisvineAnimations() {}

    public static final int CH_ROT = 0;
    public static final int CH_POS = 1;
    public static final int CH_SCALE = 2;

    private static final class Track {
        final String bone;
        final int channel;
        final float[] times;
        final float[] xs, ys, zs;
        Track(String bone, int channel, float[] times, float[] xs, float[] ys, float[] zs) {
            this.bone = bone; this.channel = channel; this.times = times;
            this.xs = xs; this.ys = ys; this.zs = zs;
        }
    }

    public static final class Clip {
        public final String name;
        public final float lengthSeconds;
        public final boolean loop;
        final List<Track> tracks = new ArrayList<>();
        Clip(String name, float lengthSeconds, boolean loop) {
            this.name = name; this.lengthSeconds = lengthSeconds; this.loop = loop;
        }
        void add(String bone, int ch, float[] t, float[] x, float[] y, float[] z) {
            tracks.add(new Track(bone, ch, t, x, y, z));
        }
    }

    private static final Map<String, Clip> CLIPS = new HashMap<>();
    private static final float DEG2RAD = (float) (Math.PI / 180.0D);

    private static void initClip_0() {
        Clip c = new Clip("dormant_buried", 3.0000F, true);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 1.5000F, 3.0000F}, new float[]{26.0000F, 28.0000F, 26.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 3.0000F}, new float[]{22.0000F, 22.0000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F});
        c.add("stem_3", CH_ROT, new float[]{0.0000F, 3.0000F}, new float[]{18.0000F, 18.0000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{-52.0000F}, new float[]{24.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{52.0000F}, new float[]{-24.0000F});
        c.add("petal_lr", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{-52.0000F}, new float[]{-24.0000F});
        c.add("petal_ll", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{52.0000F}, new float[]{24.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{-55.0000F}, new float[]{-28.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{55.0000F}, new float[]{28.0000F});
        CLIPS.put("dormant_buried", c);
    }

    private static void initClip_1() {
        Clip c = new Clip("awaken_emerge", 2.2000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.9000F, 1.6000F, 2.2000F}, new float[]{28.0000F, -16.0000F, 4.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 1.0000F, 2.2000F}, new float[]{22.0000F, -14.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("stem_3", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{18.0000F, -12.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-52.0000F, 18.0000F, 0.0000F}, new float[]{24.0000F, -12.0000F, 0.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{52.0000F, -18.0000F, 0.0000F}, new float[]{-24.0000F, 12.0000F, 0.0000F});
        c.add("petal_lr", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-52.0000F, 18.0000F, 0.0000F}, new float[]{-24.0000F, 12.0000F, 0.0000F});
        c.add("petal_ll", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{52.0000F, -18.0000F, 0.0000F}, new float[]{24.0000F, -12.0000F, 0.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-55.0000F, 24.0000F, 0.0000F}, new float[]{-28.0000F, 18.0000F, 0.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F, 1.1000F, 2.2000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{55.0000F, -24.0000F, 0.0000F}, new float[]{28.0000F, -18.0000F, 0.0000F});
        CLIPS.put("awaken_emerge", c);
    }

    private static void initClip_2() {
        Clip c = new Clip("idle_combat", 4.0000F, true);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{2.0000F, -4.0000F, 2.0000F}, new float[]{0.0000F, -4.0000F, 0.0000F}, new float[]{0.0000F, 2.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{3.0000F, -3.0000F, 3.0000F}, new float[]{0.0000F, 5.0000F, 0.0000F}, new float[]{0.0000F, -2.0000F, 0.0000F});
        c.add("corolla_head", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{-4.0000F, 4.0000F, -4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 6.0000F, 0.0000F}, new float[]{0.0000F, -4.0000F, 0.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -6.0000F, 0.0000F}, new float[]{0.0000F, 4.0000F, 0.0000F});
        c.add("petal_lr", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 6.0000F, 0.0000F}, new float[]{0.0000F, 4.0000F, 0.0000F});
        c.add("petal_ll", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -6.0000F, 0.0000F}, new float[]{0.0000F, -4.0000F, 0.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 10.0000F, 0.0000F}, new float[]{0.0000F, 8.0000F, 0.0000F});
        c.add("lash_r_mid", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -12.0000F, 0.0000F}, new float[]{0.0000F, -10.0000F, 0.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -10.0000F, 0.0000F}, new float[]{0.0000F, -8.0000F, 0.0000F});
        c.add("lash_l_mid", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 12.0000F, 0.0000F}, new float[]{0.0000F, 10.0000F, 0.0000F});
        CLIPS.put("idle_combat", c);
    }

    private static void initClip_3() {
        Clip c = new Clip("ice_beams_cross", 2.6000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.5000F, 1.6000F, 2.6000F}, new float[]{0.0000F, -10.0000F, 6.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F, 0.4500F, 1.2000F, 1.9000F, 2.6000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 25.0000F, -46.0000F, 32.0000F, 0.0000F}, new float[]{0.0000F, 34.0000F, -12.0000F, 18.0000F, 0.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F, 0.4500F, 1.2000F, 1.9000F, 2.6000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -25.0000F, 46.0000F, -32.0000F, 0.0000F}, new float[]{0.0000F, -34.0000F, 12.0000F, -18.0000F, 0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F, 0.4500F, 0.5500F, 2.1000F, 2.3000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.3000F, 1.3000F, 0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F, 0.4500F, 0.5500F, 2.1000F, 2.3000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.3000F, 1.3000F, 0.0000F});
        CLIPS.put("ice_beams_cross", c);
    }

    private static void initClip_4() {
        Clip c = new Clip("homing_icicles", 2.4000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.6000F, 1.8000F, 2.4000F}, new float[]{0.0000F, -8.0000F, -10.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F, 0.4000F, 0.8000F, 1.2000F, 1.6000F, 2.4000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -18.0000F, -28.0000F, -16.0000F, -28.0000F, 0.0000F}, new float[]{0.0000F, 12.0000F, 20.0000F, 10.0000F, 20.0000F, 0.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F, 0.4000F, 0.8000F, 1.2000F, 1.6000F, 2.4000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 18.0000F, 28.0000F, 16.0000F, 28.0000F, 0.0000F}, new float[]{0.0000F, -12.0000F, -20.0000F, -10.0000F, -20.0000F, 0.0000F});
        c.add("lash_r_tip", CH_SCALE, new float[]{0.0000F, 0.5000F, 1.0000F, 1.5000F, 2.4000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F});
        c.add("lash_l_tip", CH_SCALE, new float[]{0.0000F, 0.5000F, 1.0000F, 1.5000F, 2.4000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F}, new float[]{1.0000F, 1.3500F, 1.0000F, 1.3500F, 1.0000F});
        CLIPS.put("homing_icicles", c);
    }

    private static void initClip_5() {
        Clip c = new Clip("faceplant_single", 2.2000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.5500F, 0.9500F, 1.5500F, 2.2000F}, new float[]{0.0000F, -28.0000F, 46.0000F, 46.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.5500F, 0.9500F, 1.5500F, 2.2000F}, new float[]{0.0000F, -22.0000F, 34.0000F, 34.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_3", CH_ROT, new float[]{0.0000F, 0.5500F, 0.9500F, 1.5500F, 2.2000F}, new float[]{0.0000F, -14.0000F, 18.0000F, 18.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("frost_ring", CH_POS, new float[]{0.9000F, 1.6000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F}, new float[]{-28.0000F, -28.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 0.9000F, 1.0500F, 1.6000F, 1.9000F}, new float[]{0.0000F, 0.0000F, 1.1500F, 1.3000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 0.2000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.1500F, 1.3000F, 0.0000F});
        CLIPS.put("faceplant_single", c);
    }

    private static void initClip_6() {
        Clip c = new Clip("faceplant_triple", 4.2000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.4500F, 0.8500F, 1.4500F, 1.8500F, 2.4500F, 2.8500F, 3.5000F, 4.2000F}, new float[]{0.0000F, -24.0000F, 44.0000F, -22.0000F, 46.0000F, -24.0000F, 44.0000F, 44.0000F, 0.0000F}, new float[]{0.0000F, -25.0000F, -25.0000F, 0.0000F, 0.0000F, 25.0000F, 25.0000F, 25.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.4500F, 0.8500F, 1.4500F, 1.8500F, 2.4500F, 2.8500F, 3.5000F, 4.2000F}, new float[]{0.0000F, -18.0000F, 32.0000F, -18.0000F, 34.0000F, -18.0000F, 32.0000F, 32.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("frost_ring", CH_POS, new float[]{0.8000F, 1.8000F, 2.8000F}, new float[]{14.0000F, 0.0000F, -14.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-24.0000F, -28.0000F, -24.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 0.8000F, 0.9500F, 1.3000F, 1.8000F, 1.9500F, 2.3000F, 2.8000F, 2.9500F, 3.5000F}, new float[]{0.0000F, 0.0000F, 1.1000F, 0.0000F, 0.0000F, 1.2000F, 0.0000F, 0.0000F, 1.2500F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 0.0000F, 0.0000F, 1.0000F, 0.0000F, 0.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.1000F, 0.0000F, 0.0000F, 1.2000F, 0.0000F, 0.0000F, 1.2500F, 0.0000F});
        CLIPS.put("faceplant_triple", c);
    }

    private static void initClip_7() {
        Clip c = new Clip("spin_frost_ring", 2.8000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.5000F, 1.8000F, 2.8000F}, new float[]{0.0000F, 18.0000F, 18.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, -360.0000F, -360.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.5000F, 2.2000F, 2.8000F}, new float[]{0.0000F, -18.0000F, -18.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("corolla_head", CH_ROT, new float[]{0.0000F, 0.5000F, 2.1000F, 2.8000F}, new float[]{0.0000F, -76.0000F, -76.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 0.5000F, 2.2000F, 2.8000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 16.0000F, 16.0000F, 0.0000F}, new float[]{0.0000F, -18.0000F, -18.0000F, 0.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 0.5000F, 2.2000F, 2.8000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -16.0000F, -16.0000F, 0.0000F}, new float[]{0.0000F, 18.0000F, 18.0000F, 0.0000F});
        c.add("frost_ring", CH_ROT, new float[]{0.5000F, 2.4000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, -270.0000F}, new float[]{0.0000F, 0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 0.5000F, 1.5000F, 2.4000F, 2.8000F}, new float[]{0.0000F, 0.4000F, 1.4500F, 1.5000F, 0.0000F}, new float[]{0.0000F, 1.0000F, 1.1000F, 0.2000F, 0.0000F}, new float[]{0.0000F, 0.4000F, 1.4500F, 1.5000F, 0.0000F});
        CLIPS.put("spin_frost_ring", c);
    }

    private static void initClip_8() {
        Clip c = new Clip("blizzard_channel", 3.6000F, true);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_ROT, new float[]{0.0000F, 3.6000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, -180.0000F}, new float[]{0.0000F, 0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 1.8000F, 3.6000F}, new float[]{1.2000F, 1.4000F, 1.2000F}, new float[]{0.8000F, 1.1000F, 0.8000F}, new float[]{1.2000F, 1.4000F, 1.2000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 1.8000F, 3.6000F}, new float[]{-16.0000F, -22.0000F, -16.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("corolla_head", CH_ROT, new float[]{0.0000F, 1.8000F, 3.6000F}, new float[]{-68.0000F, -74.0000F, -68.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F, 1.8000F, 3.6000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{48.0000F, 60.0000F, 48.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F, 1.8000F, 3.6000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-48.0000F, -60.0000F, -48.0000F});
        c.add("corolla_core", CH_SCALE, new float[]{0.0000F, 0.9000F, 1.8000F, 2.7000F, 3.6000F}, new float[]{1.0000F, 1.3000F, 1.0000F, 1.3000F, 1.0000F}, new float[]{1.0000F, 1.3000F, 1.0000F, 1.3000F, 1.0000F}, new float[]{1.0000F, 1.3000F, 1.0000F, 1.3000F, 1.0000F});
        CLIPS.put("blizzard_channel", c);
    }

    private static void initClip_9() {
        Clip c = new Clip("flail_360", 2.8000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.5500F, 1.9500F, 2.8000F}, new float[]{0.0000F, 46.0000F, 46.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, -360.0000F, -360.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.5500F, 2.0000F, 2.8000F}, new float[]{0.0000F, 34.0000F, 34.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 0.5500F, 2.1000F, 2.6000F}, new float[]{0.0000F, 1.2000F, 1.3500F, 0.0000F}, new float[]{0.0000F, 1.0000F, 0.5000F, 0.0000F}, new float[]{0.0000F, 1.2000F, 1.3500F, 0.0000F});
        CLIPS.put("flail_360", c);
    }

    private static void initClip_10() {
        Clip c = new Clip("half_flail_combo", 4.0000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.4500F, 1.2000F, 1.9500F, 2.5000F, 2.9500F, 3.6000F, 4.0000F}, new float[]{0.0000F, 44.0000F, 44.0000F, 44.0000F, -22.0000F, 48.0000F, 48.0000F, 0.0000F}, new float[]{0.0000F, 90.0000F, -90.0000F, 90.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.4500F, 1.9500F, 2.5000F, 2.9500F, 3.6000F, 4.0000F}, new float[]{0.0000F, 32.0000F, 32.0000F, -18.0000F, 34.0000F, 34.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("frost_ring", CH_POS, new float[]{2.9000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{-28.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 2.9000F, 3.0500F, 3.7000F}, new float[]{0.0000F, 0.0000F, 1.2500F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.2500F, 0.0000F});
        CLIPS.put("half_flail_combo", c);
    }

    private static void initClip_11() {
        Clip c = new Clip("rotary_spray_beam", 4.8000F, false);
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.7000F, 4.1000F, 4.8000F}, new float[]{0.0000F, 26.0000F, 26.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, -360.0000F, -360.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.7000F, 4.1000F, 4.8000F}, new float[]{0.0000F, 14.0000F, 14.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("corolla_head", CH_ROT, new float[]{0.0000F, 0.7000F, 4.1000F, 4.8000F}, new float[]{0.0000F, -22.0000F, -22.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 0.7000F, 4.1000F, 4.8000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 22.0000F, 22.0000F, 0.0000F}, new float[]{0.0000F, -10.0000F, -10.0000F, 0.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 0.7000F, 4.1000F, 4.8000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, -22.0000F, -22.0000F, 0.0000F}, new float[]{0.0000F, 10.0000F, 10.0000F, 0.0000F});
        c.add("rotary_beam", CH_ROT, new float[]{0.8000F, 4.2000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 720.0000F});
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F, 0.6500F, 0.8500F, 2.4000F, 4.1000F, 4.4000F}, new float[]{0.0000F, 0.0000F, 1.2000F, 1.4000F, 1.2000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.2000F, 1.4000F, 1.2000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 1.4000F, 1.5000F, 1.4000F, 0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F, 0.8500F, 4.2000F, 4.6000F}, new float[]{0.0000F, 1.5000F, 1.6000F, 0.0000F}, new float[]{0.0000F, 0.5000F, 0.5000F, 0.0000F}, new float[]{0.0000F, 1.5000F, 1.6000F, 0.0000F});
        CLIPS.put("rotary_spray_beam", c);
    }

    private static void initClip_12() {
        Clip c = new Clip("stunned_paralyzed", 4.0000F, true);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{44.0000F, 46.0000F, 44.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{36.0000F, 38.0000F, 36.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("stem_3", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{20.0000F, 22.0000F, 20.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-22.0000F, -25.0000F, -22.0000F}, new float[]{18.0000F, 20.0000F, 18.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{22.0000F, 25.0000F, 22.0000F}, new float[]{-18.0000F, -20.0000F, -18.0000F});
        c.add("petal_lr", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{-18.0000F, -20.0000F, -18.0000F}, new float[]{-14.0000F, -16.0000F, -14.0000F});
        c.add("petal_ll", CH_ROT, new float[]{0.0000F, 2.0000F, 4.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F}, new float[]{18.0000F, 20.0000F, 18.0000F}, new float[]{14.0000F, 16.0000F, 14.0000F});
        c.add("lash_r_base", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{-35.0000F}, new float[]{-34.0000F});
        c.add("lash_l_base", CH_ROT, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{35.0000F}, new float[]{34.0000F});
        CLIPS.put("stunned_paralyzed", c);
    }

    private static void initClip_13() {
        Clip c = new Clip("death_wither", 2.6000F, false);
        c.add("rotary_beam", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_r", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("lash_beam_l", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("frost_ring", CH_SCALE, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F}, new float[]{0.0000F});
        c.add("stem_1", CH_ROT, new float[]{0.0000F, 0.5000F, 1.1000F, 2.0000F, 2.6000F}, new float[]{0.0000F, -18.0000F, 12.0000F, 52.0000F, 58.0000F}, new float[]{0.0000F, -10.0000F, 12.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("stem_2", CH_ROT, new float[]{0.0000F, 0.5000F, 2.0000F, 2.6000F}, new float[]{0.0000F, -12.0000F, 42.0000F, 48.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F}, new float[]{0.0000F, 0.0000F, 0.0000F, 0.0000F});
        c.add("petal_ur", CH_ROT, new float[]{0.0000F, 2.2000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, -62.0000F}, new float[]{0.0000F, 32.0000F});
        c.add("petal_ul", CH_ROT, new float[]{0.0000F, 2.2000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 62.0000F}, new float[]{0.0000F, -32.0000F});
        c.add("petal_lr", CH_ROT, new float[]{0.0000F, 2.2000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, -62.0000F}, new float[]{0.0000F, -32.0000F});
        c.add("petal_ll", CH_ROT, new float[]{0.0000F, 2.2000F}, new float[]{0.0000F, 0.0000F}, new float[]{0.0000F, 62.0000F}, new float[]{0.0000F, 32.0000F});
        c.add("root_core", CH_SCALE, new float[]{0.0000F, 2.0000F}, new float[]{1.0000F, 0.1000F}, new float[]{1.0000F, 0.1000F}, new float[]{1.0000F, 0.1000F});
        c.add("corolla_core", CH_SCALE, new float[]{0.0000F, 2.0000F}, new float[]{1.0000F, 0.1000F}, new float[]{1.0000F, 0.1000F}, new float[]{1.0000F, 0.1000F});
        CLIPS.put("death_wither", c);
    }

    static {
        initClip_0();
        initClip_1();
        initClip_2();
        initClip_3();
        initClip_4();
        initClip_5();
        initClip_6();
        initClip_7();
        initClip_8();
        initClip_9();
        initClip_10();
        initClip_11();
        initClip_12();
        initClip_13();
    }

    public static Clip getClip(String name) {
        return CLIPS.get(name);
    }

    public static void apply(CryoRegisvineModel<?> model, String clipName, float timeSeconds) {
        model.resetPose();
        Clip clip = CLIPS.get(clipName);
        if (clip == null) return;
        float t = timeSeconds;
        if (clip.lengthSeconds > 0.001F) {
            t = clip.loop ? (t % clip.lengthSeconds) : Math.min(t, clip.lengthSeconds);
            if (t < 0.0F) t = 0.0F;
        }
        for (Track tr : clip.tracks) {
            ModelPart part = model.getBone(tr.bone);
            if (part == null || tr.times.length == 0) continue;
            float vx, vy, vz;
            if (tr.times.length == 1 || t <= tr.times[0]) {
                vx = tr.xs[0]; vy = tr.ys[0]; vz = tr.zs[0];
            } else if (t >= tr.times[tr.times.length - 1]) {
                int last = tr.times.length - 1;
                vx = tr.xs[last]; vy = tr.ys[last]; vz = tr.zs[last];
            } else {
                int i = 0;
                while (i + 1 < tr.times.length && tr.times[i + 1] < t) i++;
                float span = tr.times[i + 1] - tr.times[i];
                float a = span > 1e-5F ? (t - tr.times[i]) / span : 0.0F;
                float s = a * a * (3.0F - 2.0F * a); // smoothstep
                vx = tr.xs[i] + (tr.xs[i + 1] - tr.xs[i]) * s;
                vy = tr.ys[i] + (tr.ys[i + 1] - tr.ys[i]) * s;
                vz = tr.zs[i] + (tr.zs[i + 1] - tr.zs[i]) * s;
            }
            if (tr.channel == CH_ROT) {
                part.xRot += vx * DEG2RAD;
                part.yRot += vy * DEG2RAD;
                part.zRot += vz * DEG2RAD;
            } else if (tr.channel == CH_POS) {
                part.x += vx;
                part.y -= vy;
                part.z += vz;
            } else if (tr.channel == CH_SCALE) {
                part.xScale = vx;
                part.yScale = vy;
                part.zScale = vz;
            }
        }
    }
}
