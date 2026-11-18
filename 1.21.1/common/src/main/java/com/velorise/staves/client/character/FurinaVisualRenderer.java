package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * Dedicated client-side idle visual renderer for Hydro Archon Furina (Splendor of Tranquil Waters):
 * - Mode 2 (Pneuma): Floating Hydro Bubble seat + 6 rotating cakes ring wheel -> Furina takes 1 cake & eats -> stands back up.
 * - Mode 1 (Ousia): Furina cradles Surintendante Chevalmarin with both hands, gently sways side-to-side, seahorse squirts playful water arcs.
 */
public final class FurinaVisualRenderer {
    private FurinaVisualRenderer() {}

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.furinaIdleLastMs <= 0L) {
            d.furinaIdleLastMs = nowMs;
            if (d.furinaIdleMode > 0) {
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + 0.016F / 1.10F);
            }
        } else if (nowMs > d.furinaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.furinaIdleLastMs) / 1000.0F);
            d.furinaIdleLastMs = nowMs;
            if (d.furinaIdleMode > 0) {
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + dt / 1.10F);
            } else {
                d.furinaIdleFade = Math.max(0.0F, d.furinaIdleFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.furinaIdleFade);
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (!ClientAuraTracker.isFurinaIdle(entity.getId())) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F || d == null) return false;
        int mode = d.furinaIdleMode > 0 ? d.furinaIdleMode : d.furinaIdleLastMode;

        long now = System.currentTimeMillis();
        float elapsedSec = (d.furinaIdleStartMs > 0L)
                ? Math.max(0.0F, (now - d.furinaIdleStartMs) / 1000.0F)
                : ((entity.tickCount % 200) / 20.0F);

        // Seated on indented water bubble chair: crossed legs, left arm across waist, right hand resting on chin
        float sitProg = WindGliderRenderer.smoothStep01(elapsedSec / 0.85F);
        float bob = Mth.sin(ageInTicks * 0.12F) * 0.24F * w * sitProg;
        float sitDropY = 4.5F * w * sitProg + bob;
        model.body.y += sitDropY;
        model.head.y += sitDropY;
        model.hat.y += sitDropY;
        model.rightArm.y += sitDropY;
        model.leftArm.y += sitDropY;
        model.rightLeg.y += sitDropY;
        model.leftLeg.y += sitDropY;

        model.body.xRot = Mth.lerp(w * sitProg, model.body.xRot, 0.06F);
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

        // Crossed thighs/legs seated gracefully on the bubble
        model.rightLeg.xRot = Mth.lerp(w * sitProg, model.rightLeg.xRot, -1.48F);
        model.leftLeg.xRot = Mth.lerp(w * sitProg, model.leftLeg.xRot, -1.28F);
        model.rightLeg.yRot = Mth.lerp(w * sitProg, model.rightLeg.yRot, -0.34F);
        model.leftLeg.yRot = Mth.lerp(w * sitProg, model.leftLeg.yRot, 0.14F);
        model.rightLeg.zRot = Mth.lerp(w * sitProg, model.rightLeg.zRot, -0.18F);
        model.leftLeg.zRot = Mth.lerp(w * sitProg, model.leftLeg.zRot, 0.08F);

        // Both normal arms resting naturally on her thighs/lap, head locked in 3rd-person
        float sway = Mth.sin(ageInTicks * 0.10F) * 0.04F;
        model.head.xRot = Mth.lerp(w * sitProg, model.head.xRot, 0.12F);
        model.head.yRot = Mth.lerp(w * sitProg, Mth.clamp(model.head.yRot, -0.45F, 0.45F), 0.0F);
        model.head.zRot = Mth.lerp(w * sitProg, model.head.zRot, sway);
        model.hat.copyFrom(model.head);
        model.rightArm.xRot = Mth.lerp(w * sitProg, model.rightArm.xRot, -0.62F);
        model.rightArm.yRot = Mth.lerp(w * sitProg, model.rightArm.yRot, -0.22F);
        model.rightArm.zRot = Mth.lerp(w * sitProg, model.rightArm.zRot, -0.18F);
        model.leftArm.xRot = Mth.lerp(w * sitProg, model.leftArm.xRot, -0.62F);
        model.leftArm.yRot = Mth.lerp(w * sitProg, model.leftArm.yRot, 0.22F);
        model.leftArm.zRot = Mth.lerp(w * sitProg, model.leftArm.zRot, 0.18F);
        return true;
    }

    public static void renderIdleEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (!ClientAuraTracker.isFurinaIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.01F || d == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float bodyYaw = (d.furinaIdleYawSet) ? d.furinaIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int mode = d.furinaIdleMode > 0 ? d.furinaIdleMode : d.furinaIdleLastMode;

        long now = System.currentTimeMillis();
        float elapsedSec = (d.furinaIdleStartMs > 0L)
                ? Math.max(0.0F, (now - d.furinaIdleStartMs) / 1000.0F)
                : ((entity.tickCount % 200) / 20.0F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.scale(fade, fade, fade);

        // Bubble chair + rotating 6-cake Ferris wheel 1.5 blocks away (no real pet summons)
        float sitProg = WindGliderRenderer.smoothStep01(elapsedSec / 0.85F);
        float bob = -Mth.sin(time * 0.12F) * 0.025F * sitProg;
        float wheelFade = fade * WindGliderRenderer.smoothStep01((elapsedSec - 0.35F) / 0.6F);
        float wheelRadius = 0.44F;
        float baseAngle = time * 0.045F;

        // PASS 1 (Solid): Draw all 6 vivid miniature Fontaine cakes directly in front (1.5 blocks)
        if (wheelFade > 0.02F) {
            VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.05F + bob * 0.6F, 1.50F);
            for (int k = 0; k < 6; k++) {
                float angle = baseAngle + (float) (k * Math.PI * 2.0D / 6.0D);
                float cx = Mth.cos(angle) * wheelRadius;
                float cy = Mth.sin(angle) * wheelRadius;
                poseStack.pushPose();
                poseStack.translate(cx, cy, 0.0F);
                poseStack.scale(0.76F, 0.76F, 0.76F);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F + k * 20.0F));
                drawFontaineCake(solidVc, poseStack.last().pose(), k);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // PASS 2 (Depth-Writing Translucent): Indented sapphire-cyan Water Bubble chair + Ferris wheel
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentDepthWrite());

        // 1. Indented Sapphire-Azure Water Bubble seat fitted to Furina's hips (no spinning star inside)
        if (sitProg > 0.02F) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.30F + bob, 0.02F);
            poseStack.scale(sitProg, sitProg, sitProg);
            drawIndentedSeatBubble(transVc, poseStack.last().pose(), 0.34F, fade);
            poseStack.popPose();
        }

        // 2. Rotating Ferris Wheel Hydro structure & crisp-bordered 6 cake bubbles (1.5 blocks directly in front)
        if (wheelFade > 0.02F) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.05F + bob * 0.6F, 1.50F);
            Matrix4f wMat = poseStack.last().pose();

            // Delicate stationary Hydro hub dot (no spinning star blob)
            float hubR = 0.042F;
            int hubA = Math.max(20, Math.round(215.0F * wheelFade));
            WindGliderRenderer.emitDoubleQuad(transVc, wMat,
                    0.0F, -hubR, 0.0F, hubR, 0.0F, 0.0F, 0.0F, hubR, 0.0F, -hubR, 0.0F, 0.0F,
                    225, 250, 255, hubA);

            // Double-layered Hydro wheel rim (soft outer sapphire glow + crisp inner cyan-white ring)
            int rimOuterA = Math.max(12, Math.round(95.0F * wheelFade));
            int rimInnerA = Math.max(20, Math.round(210.0F * wheelFade));
            int rimSegs = 28;
            for (int r = 0; r < rimSegs; r++) {
                float ra0 = (float) (r * Math.PI * 2.0D / rimSegs);
                float ra1 = (float) ((r + 1) * Math.PI * 2.0D / rimSegs);
                float rOIn = wheelRadius - 0.018F, rOOut = wheelRadius + 0.018F;
                WindGliderRenderer.emitDoubleQuad(transVc, wMat,
                        Mth.cos(ra0) * rOIn, Mth.sin(ra0) * rOIn, 0.0F,
                        Mth.cos(ra0) * rOOut, Mth.sin(ra0) * rOOut, 0.0F,
                        Mth.cos(ra1) * rOOut, Mth.sin(ra1) * rOOut, 0.0F,
                        Mth.cos(ra1) * rOIn, Mth.sin(ra1) * rOIn, 0.0F,
                        75, 175, 255, rimOuterA);
                float rIIn = wheelRadius - 0.007F, rIOut = wheelRadius + 0.007F;
                WindGliderRenderer.emitDoubleQuad(transVc, wMat,
                        Mth.cos(ra0) * rIIn, Mth.sin(ra0) * rIIn, 0.0F,
                        Mth.cos(ra0) * rIOut, Mth.sin(ra0) * rIOut, 0.0F,
                        Mth.cos(ra1) * rIOut, Mth.sin(ra1) * rIOut, 0.0F,
                        Mth.cos(ra1) * rIIn, Mth.sin(ra1) * rIIn, 0.0F,
                        215, 248, 255, rimInnerA);
            }

            int spokeA = Math.max(15, Math.round(150.0F * wheelFade));
            for (int k = 0; k < 6; k++) {
                float angle = baseAngle + (float) (k * Math.PI * 2.0D / 6.0D);
                float cx = Mth.cos(angle) * wheelRadius;
                float cy = Mth.sin(angle) * wheelRadius;
                float nx = -Mth.sin(angle) * 0.007F;
                float ny = Mth.cos(angle) * 0.007F;
                WindGliderRenderer.emitDoubleQuad(transVc, wMat,
                        -nx, -ny, 0.0F,
                        nx, ny, 0.0F,
                        cx + nx, cy + ny, 0.0F,
                        cx - nx, cy - ny, 0.0F,
                        165, 232, 255, spokeA);

                poseStack.pushPose();
                poseStack.translate(cx, cy, 0.0F);
                // Very sheer translucent bubble interior so the cake pops + crisp glowing rim border
                drawWaterBubble(transVc, poseStack.last().pose(), 0.135F, 95, 195, 255, Math.round(48.0F * wheelFade));
                drawBubbleRimRing(transVc, poseStack.last().pose(), 0.136F, 215, 248, 255, Math.round(205.0F * wheelFade));
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawIndentedSeatBubble(VertexConsumer vc, Matrix4f mat, float radius, float fade) {
        int segs = 16;
        int rings = 8;
        for (int i = 0; i < rings; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / rings);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / rings);
            float rawY0 = Mth.sin(phi0) * radius;
            float rawY1 = Mth.sin(phi1) * radius;
            float r0 = Mth.cos(phi0) * radius;
            float r1 = Mth.cos(phi1) * radius;
            // Flatten & dimple the top surface where Furina sits ("lõm xuống khi ngồi lên ở đít")
            float y0 = (rawY0 > 0.0F) ? rawY0 * 0.62F - 0.055F * Mth.clamp(1.0F - (r0 / radius) * 1.15F, 0.0F, 1.0F) : rawY0 * 0.88F;
            float y1 = (rawY1 > 0.0F) ? rawY1 * 0.62F - 0.055F * Mth.clamp(1.0F - (r1 / radius) * 1.15F, 0.0F, 1.0F) : rawY1 * 0.88F;
            // Rich sapphire-azure bottom -> luminous cyan-blue mid -> pearlescent highlight top
            float hFrac = Mth.clamp((i + 0.5F) / rings, 0.0F, 1.0F);
            int cr = Math.round(Mth.lerp(hFrac, 52.0F, 195.0F));
            int cg = Math.round(Mth.lerp(hFrac, 118.0F, 238.0F));
            int cb = 255;
            int ca = Math.max(15, Math.round(Mth.lerp(hFrac, 138.0F, 92.0F) * fade));
            for (int j = 0; j < segs; j++) {
                float theta0 = (float) (j * 2.0D * Math.PI / segs);
                float theta1 = (float) ((j + 1) * 2.0D * Math.PI / segs);
                WindGliderRenderer.emitDoubleQuad(vc, mat,
                        Mth.cos(theta0) * r0, y0, Mth.sin(theta0) * r0,
                        Mth.cos(theta1) * r0, y0, Mth.sin(theta1) * r0,
                        Mth.cos(theta1) * r1, y1, Mth.sin(theta1) * r1,
                        Mth.cos(theta0) * r1, y1, Mth.sin(theta0) * r1,
                        cr, cg, cb, ca);
            }
        }
    }

    private static void drawBubbleRimRing(VertexConsumer vc, Matrix4f mat, float radius, int r, int g, int b, int alpha) {
        int segs = 20;
        float rIn = radius - 0.010F;
        float rOut = radius + 0.008F;
        for (int s = 0; s < segs; s++) {
            float a0 = (float) (s * Math.PI * 2.0D / segs);
            float a1 = (float) ((s + 1) * Math.PI * 2.0D / segs);
            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    Mth.cos(a0) * rIn,  Mth.sin(a0) * rIn,  0.0F,
                    Mth.cos(a0) * rOut, Mth.sin(a0) * rOut, 0.0F,
                    Mth.cos(a1) * rOut, Mth.sin(a1) * rOut, 0.0F,
                    Mth.cos(a1) * rIn,  Mth.sin(a1) * rIn,  0.0F,
                    r, g, b, alpha);
        }
    }

    private static void drawWaterBubble(VertexConsumer vc, Matrix4f mat, float radius, int r, int g, int b, int alpha) {
        int segs = 12;
        int rings = 6;
        for (int i = 0; i < rings; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / rings);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / rings);
            float y0 = Mth.sin(phi0) * radius;
            float y1 = Mth.sin(phi1) * radius;
            float r0 = Mth.cos(phi0) * radius;
            float r1 = Mth.cos(phi1) * radius;

            for (int j = 0; j < segs; j++) {
                float theta0 = (float) (j * 2.0D * Math.PI / segs);
                float theta1 = (float) ((j + 1) * 2.0D * Math.PI / segs);
                float x00 = Mth.cos(theta0) * r0, z00 = Mth.sin(theta0) * r0;
                float x10 = Mth.cos(theta1) * r0, z10 = Mth.sin(theta1) * r0;
                float x11 = Mth.cos(theta1) * r1, z11 = Mth.sin(theta1) * r1;
                float x01 = Mth.cos(theta0) * r1, z01 = Mth.sin(theta0) * r1;

                WindGliderRenderer.emitDoubleQuad(vc, mat,
                        x00, y0, z00,
                        x10, y0, z10,
                        x11, y1, z11,
                        x01, y1, z01,
                        r, g, b, alpha);
            }
        }
    }

    private static void drawFontaineCake(VertexConsumer vc, Matrix4f mat, int cakeIdx) {
        switch (cakeIdx % 6) {
            case 0 -> {
                // Strawberry Shortcake (sponge + cream + red strawberry)
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.09F, -0.06F, -0.09F, 0.09F, 0.0F, 0.09F, 245, 218, 168, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.085F, 0.0F, -0.085F, 0.085F, 0.05F, 0.085F, 255, 250, 245, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.025F, 0.05F, -0.025F, 0.025F, 0.095F, 0.025F, 235, 45, 65, 255);
            }
            case 1 -> {
                // Blueberry Mousse (blue & cream layers + blueberry)
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.085F, -0.06F, -0.085F, 0.085F, -0.01F, 0.085F, 125, 145, 225, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.08F, -0.01F, -0.08F, 0.08F, 0.045F, 0.08F, 240, 245, 255, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.02F, 0.045F, -0.02F, 0.02F, 0.075F, 0.02F, 65, 75, 185, 255);
            }
            case 2 -> {
                // Mont Blanc / Chocolate Tart (rich brown chocolate + cream peak)
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.09F, -0.06F, -0.09F, 0.09F, -0.01F, 0.09F, 110, 65, 42, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.07F, -0.01F, -0.07F, 0.07F, 0.05F, 0.07F, 165, 115, 75, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.03F, 0.05F, -0.03F, 0.03F, 0.085F, 0.03F, 255, 242, 220, 255);
            }
            case 3 -> {
                // Rainbow Pastel Macaron / Cupcake
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.085F, -0.06F, -0.085F, 0.085F, -0.01F, 0.085F, 255, 175, 205, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.075F, -0.01F, -0.075F, 0.075F, 0.025F, 0.075F, 255, 255, 225, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.085F, 0.025F, -0.085F, 0.085F, 0.075F, 0.085F, 175, 225, 255, 255);
            }
            case 4 -> {
                // Caramel Pudding with glossy top & cherry
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.08F, -0.06F, -0.08F, 0.08F, 0.02F, 0.08F, 255, 235, 135, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.075F, 0.02F, -0.075F, 0.075F, 0.045F, 0.075F, 175, 95, 35, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.022F, 0.045F, -0.022F, 0.022F, 0.085F, 0.022F, 235, 35, 55, 255);
            }
            default -> {
                // Matcha Green Tea Opera
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.09F, -0.06F, -0.09F, 0.09F, -0.01F, 0.09F, 125, 185, 95, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.085F, -0.01F, -0.085F, 0.085F, 0.035F, 0.085F, 235, 245, 215, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.08F, 0.035F, -0.08F, 0.08F, 0.065F, 0.08F, 65, 125, 45, 255);
            }
        }
    }

}
