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
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + 0.016F / 0.55F);
            }
        } else if (nowMs > d.furinaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.furinaIdleLastMs) / 1000.0F);
            d.furinaIdleLastMs = nowMs;
            if (d.furinaIdleMode > 0) {
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + dt / 0.55F);
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

        if (mode == 2) {
            // Mode 2 Cycle: 10 seconds total
            // 0.0 - 1.2s: sit down on bubble
            // 1.2 - 4.5s: 6 cakes wheel in front
            // 4.5 - 8.0s: pick 1 cake & eat
            // 8.0 - 9.8s: stand back up
            float cycleSec = elapsedSec % 10.0F;
            float sitProg;
            if (cycleSec < 1.2F) {
                sitProg = WindGliderRenderer.smoothStep01(cycleSec / 1.2F);
            } else if (cycleSec < 8.0F) {
                sitProg = 1.0F;
            } else if (cycleSec < 9.6F) {
                sitProg = 1.0F - WindGliderRenderer.smoothStep01((cycleSec - 8.0F) / 1.6F);
            } else {
                sitProg = 0.0F;
            }

            float bob = Mth.sin(ageInTicks * 0.12F) * 0.30F * w * sitProg;
            float sitDropY = 3.3F * w * sitProg + bob;
            model.body.y += sitDropY;
            model.head.y += sitDropY;
            model.hat.y += sitDropY;
            model.rightArm.y += sitDropY;
            model.leftArm.y += sitDropY;
            model.rightLeg.y += sitDropY;
            model.leftLeg.y += sitDropY;

            model.body.xRot = Mth.lerp(w * sitProg, model.body.xRot, 0.08F);
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

            // Seated leg pose
            model.rightLeg.xRot = Mth.lerp(w * sitProg, model.rightLeg.xRot, -1.32F);
            model.leftLeg.xRot = Mth.lerp(w * sitProg, model.leftLeg.xRot, -1.18F);
            model.rightLeg.yRot = Mth.lerp(w * sitProg, model.rightLeg.yRot, 0.12F);
            model.leftLeg.yRot = Mth.lerp(w * sitProg, model.leftLeg.yRot, -0.12F);

            if (cycleSec >= 4.5F && cycleSec < 8.0F) {
                // Eating phase: right arm brings cake to mouth, nibbles happily
                float eatNibble = Mth.sin(ageInTicks * 0.45F) * 0.05F;
                model.head.xRot = Mth.lerp(w * sitProg, model.head.xRot, 0.24F + eatNibble * 0.25F);
                model.head.zRot = Mth.lerp(w * sitProg, model.head.zRot, 0.08F);
                model.rightArm.xRot = Mth.lerp(w * sitProg, model.rightArm.xRot, -1.38F + eatNibble);
                model.rightArm.yRot = Mth.lerp(w * sitProg, model.rightArm.yRot, -0.42F);
                model.rightArm.zRot = Mth.lerp(w * sitProg, model.rightArm.zRot, 0.26F);
                model.leftArm.xRot = Mth.lerp(w * sitProg, model.leftArm.xRot, -0.90F);
                model.leftArm.yRot = Mth.lerp(w * sitProg, model.leftArm.yRot, 0.30F);
                model.leftArm.zRot = Mth.lerp(w * sitProg, model.leftArm.zRot, -0.16F);
            } else {
                // Seated observing cakes or standing transition
                float sway = Mth.sin(ageInTicks * 0.10F) * 0.08F;
                model.head.xRot = Mth.lerp(w * sitProg, model.head.xRot, 0.15F);
                model.head.zRot = Mth.lerp(w * sitProg, model.head.zRot, sway);
                model.rightArm.xRot = Mth.lerp(w * sitProg, model.rightArm.xRot, -0.62F);
                model.rightArm.yRot = Mth.lerp(w * sitProg, model.rightArm.yRot, -0.22F);
                model.rightArm.zRot = Mth.lerp(w * sitProg, model.rightArm.zRot, 0.20F);
                model.leftArm.xRot = Mth.lerp(w * sitProg, model.leftArm.xRot, -0.62F);
                model.leftArm.yRot = Mth.lerp(w * sitProg, model.leftArm.yRot, 0.22F);
                model.leftArm.zRot = Mth.lerp(w * sitProg, model.leftArm.zRot, -0.20F);
            }
        } else {
            // Mode 1: Furina cradles Surintendante Chevalmarin with both hands, gently sways side-to-side
            float sway = Mth.sin(ageInTicks * 0.12F);
            model.body.yRot = Mth.lerp(w, model.body.yRot, 0.10F * sway);
            model.head.zRot = Mth.lerp(w, model.head.zRot, 0.12F * sway);
            model.body.zRot = Mth.lerp(w, model.body.zRot, 0.05F * sway);
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

            // Hands raised up in front of chest/chin holding the seahorse
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.25F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.32F + sway * 0.06F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.38F + sway * 0.08F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.25F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.32F + sway * 0.06F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.38F + sway * 0.08F);
        }
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

        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());

        if (mode == 2) {
            // Mode 2: Bubble seat + 6 cakes wheel + cake eating
            float cycleSec = elapsedSec % 10.0F;
            float sitProg;
            if (cycleSec < 1.2F) {
                sitProg = WindGliderRenderer.smoothStep01(cycleSec / 1.2F);
            } else if (cycleSec < 8.0F) {
                sitProg = 1.0F;
            } else if (cycleSec < 9.6F) {
                sitProg = 1.0F - WindGliderRenderer.smoothStep01((cycleSec - 8.0F) / 1.6F);
            } else {
                sitProg = 0.0F;
            }

            float bob = Mth.sin(time * 0.12F) * 0.035F * sitProg;

            // 1. Large translucent glowing Water Bubble seat underneath Furina
            if (sitProg > 0.02F) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.44F + bob, 0.0F);
                poseStack.scale(sitProg, sitProg, sitProg);
                drawWaterBubble(transVc, poseStack.last().pose(), 0.46F, 110, 215, 255, Math.round(145.0F * fade));
                // Inner glowing star in bubble
                drawCentralHydroStar(transVc, poseStack.last().pose(), 0.12F, time * 1.5F, 220, 245, 255, Math.round(180.0F * fade));
                poseStack.popPose();
            }

            // 2. Wheel of 6 Cakes in Water Bubbles rotating in vertical circle in front of Furina
            if (cycleSec >= 1.0F && cycleSec < 4.8F) {
                float wheelAlpha = 1.0F;
                if (cycleSec < 1.6F) {
                    wheelAlpha = WindGliderRenderer.smoothStep01((cycleSec - 1.0F) / 0.6F);
                } else if (cycleSec > 4.2F) {
                    wheelAlpha = 1.0F - WindGliderRenderer.smoothStep01((cycleSec - 4.2F) / 0.6F);
                }
                float wheelFade = fade * wheelAlpha;
                if (wheelFade > 0.02F) {
                    poseStack.pushPose();
                    poseStack.translate(0.0F, 0.88F + bob * 0.8F, 0.52F);
                    Matrix4f wMat = poseStack.last().pose();

                    // Central hydro star core
                    drawCentralHydroStar(transVc, wMat, 0.14F, time * 2.2F, 180, 235, 255, Math.round(210.0F * wheelFade));

                    float wheelRadius = 0.38F;
                    float baseAngle = time * 0.065F;
                    for (int k = 0; k < 6; k++) {
                        float angle = baseAngle + (float) (k * Math.PI * 2.0D / 6.0D);
                        float cx = Mth.cos(angle) * wheelRadius;
                        float cy = Mth.sin(angle) * wheelRadius;

                        poseStack.pushPose();
                        poseStack.translate(cx, cy, 0.0F);
                        // Mini bubble surrounding cake
                        drawWaterBubble(transVc, poseStack.last().pose(), 0.11F, 130, 220, 255, Math.round(135.0F * wheelFade));
                        // 3D Miniature Fontaine Delicacy / Cake
                        poseStack.scale(0.55F, 0.55F, 0.55F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.0F + k * 60.0F));
                        drawFontaineCake(solidVc, poseStack.last().pose(), k);
                        poseStack.popPose();
                    }
                    poseStack.popPose();
                }
            }

            // 3. Furina takes 1 cake & eats it with crumbs / sparkles
            if (cycleSec >= 4.5F && cycleSec < 8.0F) {
                float eatFade = 1.0F;
                if (cycleSec < 4.8F) {
                    eatFade = WindGliderRenderer.smoothStep01((cycleSec - 4.5F) / 0.3F);
                } else if (cycleSec > 7.6F) {
                    eatFade = 1.0F - WindGliderRenderer.smoothStep01((cycleSec - 7.6F) / 0.4F);
                }
                float totalEatFade = fade * eatFade;
                if (totalEatFade > 0.02F) {
                    poseStack.pushPose();
                    float eatNibble = Mth.sin(time * 0.45F) * 0.02F;
                    poseStack.translate(0.08F, 1.25F + bob + eatNibble, 0.32F);
                    poseStack.scale(0.52F * totalEatFade, 0.52F * totalEatFade, 0.52F * totalEatFade);
                    poseStack.mulPose(Axis.YP.rotationDegrees(35.0F + Mth.sin(time * 0.2F) * 10.0F));
                    drawFontaineCake(solidVc, poseStack.last().pose(), 0); // Delicious strawberry shortcake
                    poseStack.popPose();

                    // Sweet crumbs / sparkling hydro stars falling down
                    Matrix4f pMat = poseStack.last().pose();
                    for (int c = 0; c < 4; c++) {
                        float cAge = ((time * 0.05F + c * 0.25F) % 1.0F);
                        float cx = 0.08F + (Mth.sin(c * 1.7F) * 0.08F);
                        float cy = 1.22F + bob - cAge * 0.28F;
                        float cz = 0.32F + (Mth.cos(c * 2.3F) * 0.08F);
                        float cSize = 0.012F * (1.0F - cAge * 0.6F);
                        int cAlpha = Math.max(15, Math.round(220.0F * totalEatFade * (1.0F - cAge)));
                        ClientAuraRenderer.emitCrystalQuad(transVc, pMat,
                                cx - cSize, cy, cz - cSize, 255, 235, 185, cAlpha,
                                cx + cSize, cy, cz - cSize, 255, 235, 185, cAlpha,
                                cx + cSize, cy + cSize, cz + cSize, 255, 195, 215, cAlpha,
                                cx - cSize, cy + cSize, cz + cSize, 255, 195, 215, cAlpha);
                    }
                }
            }
        } else {
            // Mode 1: Furina cradles Surintendante Chevalmarin with both hands, sways gently, seahorse squirts water
            float sway = Mth.sin(time * 0.12F);
            float hover = Mth.sin(time * 0.18F);

            poseStack.pushPose();
            // Position seahorse right between Furina's raised hands
            poseStack.translate(sway * 0.05F, 1.20F + hover * 0.035F, 0.42F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sway * 14.0F));
            poseStack.scale(0.85F, 0.85F, 0.85F);

            // Draw 3D Surintendante Chevalmarin
            drawChevalmarinModel(solidVc, transVc, poseStack, time);

            // "cá ngựa phụt nước" - Playful water squirts arching out from the seahorse snout
            int squirtCycle = Math.round(time) % 36;
            if (squirtCycle < 18) {
                float squirtProg = squirtCycle / 18.0F;
                Matrix4f wMat = poseStack.last().pose();
                for (int dDrop = 0; dDrop < 5; dDrop++) {
                    float dropProg = Mth.clamp(squirtProg * 1.3F - dDrop * 0.12F, 0.0F, 1.0F);
                    if (dropProg <= 0.01F || dropProg >= 0.98F) continue;
                    float dropDist = dropProg * 0.58F;
                    float dropY = 0.28F + Mth.sin(dropProg * (float) Math.PI * 0.85F) * 0.22F - dropProg * 0.15F;
                    float dropZ = 0.16F + dropDist;
                    float dropX = Mth.sin(time * 0.3F + dDrop) * 0.03F;
                    float dropRad = 0.022F * (1.0F - dropProg * 0.4F);
                    int dropAlpha = Math.max(15, Math.round(230.0F * fade * (1.0F - dropProg * 0.6F)));

                    ClientAuraRenderer.emitCrystalQuad(transVc, wMat,
                            dropX - dropRad, dropY, dropZ - dropRad, 130, 225, 255, dropAlpha,
                            dropX + dropRad, dropY, dropZ - dropRad, 130, 225, 255, dropAlpha,
                            dropX + dropRad, dropY + dropRad * 1.5F, dropZ + dropRad, 210, 245, 255, dropAlpha,
                            dropX - dropRad, dropY + dropRad * 1.5F, dropZ + dropRad, 210, 245, 255, dropAlpha);
                }
            }

            poseStack.popPose();
        }

        poseStack.popPose();
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

    private static void drawCentralHydroStar(VertexConsumer vc, Matrix4f mat, float size, float spin, int r, int g, int b, int alpha) {
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            float a0 = spin + (float) (i * Math.PI * 2.0D / rays);
            float a1 = spin + (float) ((i + 1) * Math.PI * 2.0D / rays);
            float rOut = (i % 2 == 0) ? size : size * 0.38F;
            float rNext = ((i + 1) % 2 == 0) ? size : size * 0.38F;
            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    0.0F, 0.0F, 0.0F,
                    Mth.cos(a0) * rOut, Mth.sin(a0) * rOut, 0.0F,
                    Mth.cos(a1) * rNext, Mth.sin(a1) * rNext, 0.0F,
                    0.0F, 0.0F, 0.0F,
                    r, g, b, alpha);
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

    private static void drawChevalmarinModel(VertexConsumer solidVc, VertexConsumer transVc, PoseStack poseStack, float time) {
        Matrix4f mat = poseStack.last().pose();

        // 1. Cerulean seahorse torso & curled tail
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.075F, -0.16F, -0.065F, 0.075F, 0.08F, 0.065F, 55, 155, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.055F, 0.08F, -0.055F, 0.055F, 0.22F, 0.055F, 75, 175, 255, 255);
        // Snout pointing forward
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.035F, 0.12F, 0.055F, 0.035F, 0.18F, 0.16F, 95, 195, 255, 255);
        // Cute big eyes
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.060F, 0.14F, 0.01F, -0.054F, 0.19F, 0.045F, 25, 35, 75, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, 0.054F, 0.14F, 0.01F, 0.060F, 0.19F, 0.045F, 25, 35, 75, 255);
        // Curled tail
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.045F, -0.26F, -0.035F, 0.045F, -0.16F, 0.035F, 45, 135, 245, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.035F, -0.32F, 0.01F, 0.035F, -0.24F, 0.085F, 45, 135, 245, 255);

        // 2. Translucent fluttering dorsal fin
        float finFlutter = Mth.sin(time * 0.4F) * 0.02F;
        ClientAuraRenderer.emitCrystalQuad(transVc, mat,
                0.0F, -0.12F, -0.065F, 185, 235, 255, 205,
                0.0F, -0.12F, -0.15F + finFlutter, 185, 235, 255, 205,
                0.0F, 0.06F, -0.13F + finFlutter, 220, 245, 255, 225,
                0.0F, 0.06F, -0.065F, 220, 245, 255, 225);

        // 3. Cute 3D Maid Ribbon Bow on top of head
        // Center pearl knot
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.035F, 0.22F, -0.025F, 0.035F, 0.27F, 0.025F, 245, 250, 255, 255);
        // Left & right bow wings
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.12F, 0.21F, -0.02F, -0.035F, 0.28F, 0.02F, 65, 165, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, 0.035F, 0.21F, -0.02F, 0.12F, 0.28F, 0.02F, 65, 165, 255, 255);
    }
}
