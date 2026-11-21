package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.model.CryoRegisvineAnimations;
import com.velorise.staves.client.model.CryoRegisvineModel;
import com.velorise.staves.entity.CryoRegisvineEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Renders CryoRegisvineEntity at Genshin-canon 1.6x scale (~8.35 blocks tall),
 * hiding the blocky placeholder laser cubes and anchoring procedural multi-layered
 * Cryo Frost Laser beams directly to the live animated bone transforms of
 * rotary_beam, lash_beam_r, and lash_beam_l so beams always match the boss orientation 1:1.
 */
public class CryoRegisvineRenderer extends EntityRenderer<CryoRegisvineEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("velorise_staves", "textures/entity/cryo_regisvine_boss.png");
    private static final float BOSS_RENDER_SCALE = 1.6F;
    private static CryoRegisvineModel<CryoRegisvineEntity> cachedModel;

    private static final String[] CHAIN_ROTARY = {
            "root", "root_base", "stem_1", "stem_2", "stem_3", "neck", "corolla_head", "rotary_beam"
    };
    private static final String[] CHAIN_LASH_R = {
            "root", "root_base", "stem_1", "stem_2", "stem_3", "neck", "corolla_head",
            "lash_r_base", "lash_r_mid", "lash_r_tip", "lash_beam_r"
    };
    private static final String[] CHAIN_LASH_L = {
            "root", "root_base", "stem_1", "stem_2", "stem_3", "neck", "corolla_head",
            "lash_l_base", "lash_l_mid", "lash_l_tip", "lash_beam_l"
    };

    public CryoRegisvineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.4F;
    }

    private static CryoRegisvineModel<CryoRegisvineEntity> getModel() {
        if (cachedModel == null) {
            cachedModel = new CryoRegisvineModel<>(CryoRegisvineModel.createBodyLayer().bakeRoot());
        }
        return cachedModel;
    }

    private static void hideBone(CryoRegisvineModel<?> model, String name) {
        ModelPart part = model.getBone(name);
        if (part != null) {
            part.visible = false;
            part.xScale = 0.0F;
            part.yScale = 0.0F;
            part.zScale = 0.0F;
        }
    }

    @Override
    public void render(
            CryoRegisvineEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float yaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float animTicksF = entity.getAnimTick() + partialTicks;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(-BOSS_RENDER_SCALE, -BOSS_RENDER_SCALE, BOSS_RENDER_SCALE);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        CryoRegisvineModel<CryoRegisvineEntity> model = getModel();
        CryoRegisvineAnimations.apply(model, entity.getCurrentAnimName(), animTicksF / 20.0F);

        // Hide the blocky placeholder laser meshes baked in the .bbmodel
        hideBone(model, "rotary_beam");
        hideBone(model, "lash_beam_r");
        hideBone(model, "lash_beam_l");

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
        model.renderToBuffer(poseStack, vc, packedLight, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        // Render procedural 3D Cryo lasers directly in the live animated bone coordinate space
        renderBoneAnchoredLasers(entity, model, animTicksF, poseStack, bufferSource);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static Vec3[] sampleBoneRay(CryoRegisvineModel<?> model, PoseStack baseStack, String[] chain) {
        baseStack.pushPose();
        // Temporarily ensure scale=1 on the terminal laser bone so its rotation/pivot matrix is non-singular
        ModelPart terminal = model.getBone(chain[chain.length - 1]);
        float sx = 1.0F, sy = 1.0F, sz = 1.0F;
        if (terminal != null) {
            sx = terminal.xScale; sy = terminal.yScale; sz = terminal.zScale;
            terminal.xScale = 1.0F; terminal.yScale = 1.0F; terminal.zScale = 1.0F;
        }
        for (String name : chain) {
            ModelPart part = model.getBone(name);
            if (part != null) {
                part.translateAndRotate(baseStack);
            }
        }
        Matrix4f m = new Matrix4f(baseStack.last().pose());
        if (terminal != null) {
            terminal.xScale = sx; terminal.yScale = sy; terminal.zScale = sz;
        }
        baseStack.popPose();

        // Transform origin (0,0,0) and local -Z forward (0,0,-1) into view/render space
        Vector4f o = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F).mul(m);
        Vector4f f = new Vector4f(0.0F, 0.0F, -1.0F, 1.0F).mul(m);
        Vec3 origin = new Vec3(o.x(), o.y(), o.z());
        Vec3 dir = new Vec3(f.x() - o.x(), f.y() - o.y(), f.z() - o.z());
        if (dir.lengthSqr() < 1e-6D) {
            dir = new Vec3(0.0D, 0.0D, -1.0D);
        } else {
            dir = dir.normalize();
        }
        return new Vec3[]{ origin, dir };
    }

    private static void renderBoneAnchoredLasers(
            CryoRegisvineEntity entity,
            CryoRegisvineModel<?> model,
            float t,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        int anim = entity.getAnimId();
        if (anim != CryoRegisvineEntity.ANIM_ICE_BEAMS_CROSS && anim != CryoRegisvineEntity.ANIM_ROTARY_SPRAY) {
            return;
        }

        VertexConsumer vc = bufferSource.getBuffer(RenderType.lightning());
        // Draw using identity matrix because sampleBoneRay already transformed into view/camera space!
        Matrix4f identity = new Matrix4f();

        if (anim == CryoRegisvineEntity.ANIM_ICE_BEAMS_CROSS) {
            Vec3[] rayR = sampleBoneRay(model, poseStack, CHAIN_LASH_R);
            Vec3[] rayL = sampleBoneRay(model, poseStack, CHAIN_LASH_L);

            if (t >= 6.0F && t < 16.0F) {
                float charge = (t - 6.0F) / 10.0F;
                drawChargeOrb(vc, identity, rayR[0], 0.14F + 0.38F * charge, t);
                drawChargeOrb(vc, identity, rayL[0], 0.14F + 0.38F * charge, -t);
            } else if (t >= 16.0F && t <= 44.0F) {
                float fade = (t < 19.0F) ? (t - 16.0F) / 3.0F : (t > 40.0F ? Math.max(0.0F, (44.0F - t) / 4.0F) : 1.0F);
                Vec3 endR = rayR[0].add(rayR[1].scale(17.0D));
                Vec3 endL = rayL[0].add(rayL[1].scale(17.0D));
                drawFrostLaserBeam(vc, identity, rayR[0], endR, 0.16F * fade, 0.38F * fade, t, 3);
                drawFrostLaserBeam(vc, identity, rayL[0], endL, 0.16F * fade, 0.38F * fade, -t, 3);
            }
        } else if (anim == CryoRegisvineEntity.ANIM_ROTARY_SPRAY) {
            Vec3[] ray = sampleBoneRay(model, poseStack, CHAIN_ROTARY);

            if (t >= 6.0F && t < 16.0F) {
                float charge = (t - 6.0F) / 10.0F;
                drawChargeOrb(vc, identity, ray[0], 0.24F + 0.65F * charge, t * 1.3F);
            } else if (t >= 16.0F && t <= 86.0F) {
                float fade = (t < 19.0F) ? (t - 16.0F) / 3.0F : (t > 81.0F ? Math.max(0.0F, (86.0F - t) / 5.0F) : 1.0F);
                Vec3 end = ray[0].add(ray[1].scale(18.0D));
                drawFrostLaserBeam(vc, identity, ray[0], end, 0.28F * fade, 0.68F * fade, t * 1.4F, 5);
            }
        }
    }

    private static void v(VertexConsumer vc, Matrix4f mat, float x, float y, float z, int r, int g, int b, int a) {
        vc.vertex(mat, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void drawChargeOrb(VertexConsumer vc, Matrix4f mat, Vec3 center, float radius, float time) {
        for (int ring = 0; ring < 3; ring++) {
            float spin = time * 0.28F + ring * 1.047F;
            Vec3 u = new Vec3(Math.cos(spin), Math.sin(spin) * 0.6D, Math.sin(spin)).normalize();
            Vec3 ref = (ring == 0) ? new Vec3(0, 1, 0) : (ring == 1 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1));
            Vec3 vAxis = u.cross(ref).normalize();
            Vec3 wAxis = u.cross(vAxis).normalize();
            drawRing(vc, mat, center, vAxis, wAxis, radius * 0.72F, radius, 12, 185, 245, 255, 190);
        }
    }

    private static void drawFrostLaserBeam(
            VertexConsumer vc,
            Matrix4f mat,
            Vec3 start,
            Vec3 end,
            float coreRadius,
            float sheathRadius,
            float time,
            int pulseRings
    ) {
        Vec3 delta = end.subtract(start);
        double len = delta.length();
        if (len < 0.2D || coreRadius < 0.01F) return;
        Vec3 dir = delta.scale(1.0D / len);
        Vec3 upRef = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(upRef).normalize();
        Vec3 up = right.cross(dir).normalize();

        float pulse = 1.0F + 0.12F * Mth.sin(time * 0.85F);
        drawCylinder(vc, mat, start, end, right, up, coreRadius * pulse, 8, 235, 252, 255, 235);
        drawCylinder(vc, mat, start, end, right, up, sheathRadius * (0.95F + 0.08F * Mth.cos(time * 0.65F)), 8, 65, 195, 255, 105);
        drawHelixRibbon(vc, mat, start, dir, right, up, len, sheathRadius * 1.15F, coreRadius * 0.55F, time * 0.45F, 1.0F, 165, 238, 255, 175);
        drawHelixRibbon(vc, mat, start, dir, right, up, len, sheathRadius * 1.05F, coreRadius * 0.45F, -time * 0.35F + 3.14F, -1.0F, 90, 210, 255, 145);
        for (int i = 0; i < pulseRings; i++) {
            float frac = ((time * 0.075F) + (float) i / pulseRings) % 1.0F;
            if (frac < 0.0F) frac += 1.0F;
            Vec3 pos = start.add(dir.scale(len * frac));
            float rIn = sheathRadius * (0.95F + 0.35F * frac);
            float rOut = rIn + coreRadius * 0.65F;
            int alpha = (int) (170 * (1.0F - frac * 0.75F));
            drawRing(vc, mat, pos, right, up, rIn, rOut, 12, 190, 245, 255, alpha);
        }
        drawRing(vc, mat, start.add(dir.scale(0.35D)), right, up, coreRadius * 0.5F, sheathRadius * 1.65F, 12, 220, 250, 255, 200);
    }

    private static void drawCylinder(
            VertexConsumer vc, Matrix4f mat,
            Vec3 a, Vec3 b, Vec3 right, Vec3 up,
            float radius, int segs, int r, int g, int bl, int al
    ) {
        for (int i = 0; i < segs; i++) {
            double ang0 = (Math.PI * 2.0D * i) / segs;
            double ang1 = (Math.PI * 2.0D * (i + 1)) / segs;
            Vec3 off0 = right.scale(Math.cos(ang0) * radius).add(up.scale(Math.sin(ang0) * radius));
            Vec3 off1 = right.scale(Math.cos(ang1) * radius).add(up.scale(Math.sin(ang1) * radius));
            Vec3 p0 = a.add(off0), p1 = a.add(off1), p2 = b.add(off1), p3 = b.add(off0);
            v(vc, mat, (float) p0.x, (float) p0.y, (float) p0.z, r, g, bl, al);
            v(vc, mat, (float) p1.x, (float) p1.y, (float) p1.z, r, g, bl, al);
            v(vc, mat, (float) p2.x, (float) p2.y, (float) p2.z, r, g, bl, al);
            v(vc, mat, (float) p3.x, (float) p3.y, (float) p3.z, r, g, bl, al);
        }
    }

    private static void drawHelixRibbon(
            VertexConsumer vc, Matrix4f mat,
            Vec3 start, Vec3 dir, Vec3 right, Vec3 up,
            double len, float radius, float width, float phase, float twistSign,
            int r, int g, int bl, int al
    ) {
        int steps = 28;
        Vec3 prevA = null, prevB = null;
        for (int i = 0; i <= steps; i++) {
            double frac = (double) i / steps;
            double ang = phase + twistSign * frac * Math.PI * 6.0D;
            Vec3 radial = right.scale(Math.cos(ang)).add(up.scale(Math.sin(ang)));
            Vec3 center = start.add(dir.scale(len * frac)).add(radial.scale(radius));
            Vec3 pA = center.add(radial.scale(width * 0.5F));
            Vec3 pB = center.subtract(radial.scale(width * 0.5F));
            if (prevA != null) {
                int segAlpha = (int) (al * (1.0D - frac * 0.45D));
                v(vc, mat, (float) prevA.x, (float) prevA.y, (float) prevA.z, r, g, bl, segAlpha);
                v(vc, mat, (float) prevB.x, (float) prevB.y, (float) prevB.z, r, g, bl, segAlpha);
                v(vc, mat, (float) pB.x, (float) pB.y, (float) pB.z, r, g, bl, segAlpha);
                v(vc, mat, (float) pA.x, (float) pA.y, (float) pA.z, r, g, bl, segAlpha);
            }
            prevA = pA;
            prevB = pB;
        }
    }

    private static void drawRing(
            VertexConsumer vc, Matrix4f mat,
            Vec3 center, Vec3 uAxis, Vec3 vAxis,
            float rIn, float rOut, int segs,
            int r, int g, int bl, int al
    ) {
        for (int i = 0; i < segs; i++) {
            double ang0 = (Math.PI * 2.0D * i) / segs;
            double ang1 = (Math.PI * 2.0D * (i + 1)) / segs;
            Vec3 d0 = uAxis.scale(Math.cos(ang0)).add(vAxis.scale(Math.sin(ang0)));
            Vec3 d1 = uAxis.scale(Math.cos(ang1)).add(vAxis.scale(Math.sin(ang1)));
            Vec3 i0 = center.add(d0.scale(rIn)), o0 = center.add(d0.scale(rOut));
            Vec3 o1 = center.add(d1.scale(rOut)), i1 = center.add(d1.scale(rIn));
            v(vc, mat, (float) i0.x, (float) i0.y, (float) i0.z, r, g, bl, al);
            v(vc, mat, (float) o0.x, (float) o0.y, (float) o0.z, r, g, bl, al);
            v(vc, mat, (float) o1.x, (float) o1.y, (float) o1.z, r, g, bl, al);
            v(vc, mat, (float) i1.x, (float) i1.y, (float) i1.z, r, g, bl, al);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CryoRegisvineEntity entity) {
        return TEXTURE;
    }
}
