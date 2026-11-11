package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.DendroCoreEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Self-illuminated Minecraft-style 16-faceted lantern-bud renderer for the Dendro Core.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DendroCoreRenderer extends EntityRenderer<DendroCoreEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/entity/dendro_core.png");
    private static final RenderType RENDER_TYPE = CustomRenderType.create(TEXTURE);
    private static final float P = 1.0F / 32.0F;
    private static final float RENDER_SCALE = 0.75F;
    private static final int RADIAL_SEGMENTS = 16;

    private record Quad(
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3
    ) {}

    private static final List<Quad> MESH = buildMesh();

    public DendroCoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.DENDRO_CORE_ENTITY.get(), DendroCoreRenderer::new);
    }

    @Override
    protected int getSkyLightLevel(DendroCoreEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getBlockLightLevel(DendroCoreEntity entity, BlockPos pos) {
        return 8;
    }

    @Override
    public void render(
            DendroCoreEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        if (!entity.isHyperbloomVisual()) {
            float bob = Mth.sin((entity.getAge() + partialTicks) / 10.0F + entity.bobOffs) * 0.04F;
            poseStack.translate(0.0F, bob + 0.055F, 0.0F);
        } else {
            poseStack.translate(0.0F, -0.20F, 0.0F);
        }
        poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
        poseStack.mulPose(Axis.YP.rotation(entity.getSpin(partialTicks)));

        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        Matrix4f pose = poseStack.last().pose();
        for (Quad q : MESH) {
            consumer.addVertex(pose, q.x0, q.y0, q.z0).setUv(q.u0, q.v0);
            consumer.addVertex(pose, q.x1, q.y1, q.z1).setUv(q.u1, q.v1);
            consumer.addVertex(pose, q.x2, q.y2, q.z2).setUv(q.u2, q.v2);
            consumer.addVertex(pose, q.x3, q.y3, q.z3).setUv(q.u3, q.v3);
        }
        poseStack.popPose();
    }

    private static List<Quad> buildMesh() {
        List<Quad> quads = new ArrayList<>();

        // 10 smooth profile rings -> 9 continuous tapered lantern-bud bands
        float[] ringY = {0.80F, 2.05F, 3.55F, 5.25F, 7.05F, 8.85F, 10.55F, 11.95F, 12.95F, 13.55F};
        float[] ringR = {0.30F, 1.65F, 3.20F, 4.65F, 5.72F, 6.10F,  5.75F,  4.60F,  2.90F,  1.35F};
        int nRings = ringY.length;
        int quarterSegs = RADIAL_SEGMENTS / 4;

        float[][][] verts = new float[nRings][RADIAL_SEGMENTS][3];
        for (int ri = 0; ri < nRings; ri++) {
            float yNorm = (float) ri / (nRings - 1);
            double envelope = Math.pow(Math.sin(Math.PI * yNorm), 0.7D);
            for (int si = 0; si < RADIAL_SEGMENTS; si++) {
                double theta = si * (2.0D * Math.PI / RADIAL_SEGMENTS);
                double c4 = Math.cos(4.0D * theta);
                double c8 = Math.cos(8.0D * theta);
                double rib = 0.048D * Math.max(0.0D, c4) + 0.022D * Math.max(0.0D, -c4) - 0.015D * (1.0D - c8) * 0.5D;
                float r = (float) (ringR[ri] * (1.0D + envelope * rib)) * P;
                verts[ri][si][0] = r * (float) Math.cos(theta);
                verts[ri][si][1] = ringY[ri] * P;
                verts[ri][si][2] = r * (float) Math.sin(theta);
            }
        }

        // Main lofted bud husk (u: 0..0.5, v: 0..0.5 per 90-deg petal panel)
        for (int ri = 0; ri < nRings - 1; ri++) {
            float v0 = 0.5F * (1.0F - (float) ri / (nRings - 1));
            float v1 = 0.5F * (1.0F - (float) (ri + 1) / (nRings - 1));
            for (int si = 0; si < RADIAL_SEGMENTS; si++) {
                int next = (si + 1) % RADIAL_SEGMENTS;
                int qLocal = si % quarterSegs;
                float u0 = 0.5F * ((float) qLocal / quarterSegs);
                float u1 = 0.5F * ((float) (qLocal + 1) / quarterSegs);
                addRectQuad(quads, verts[ri + 1][si], verts[ri + 1][next], verts[ri][next], verts[ri][si], u0, v1, u1, v0);
            }
        }

        // Bottom pointed tip cap (Region 3: 0..0.5, 0.5..1.0) & top collar crown cap (Region 2: 0.5..1.0, 0..0.5)
        float[] botCenter = {0.0F, 0.52F * P, 0.0F};
        float[] topCenter = {0.0F, 13.75F * P, 0.0F};
        for (int si = 0; si < RADIAL_SEGMENTS; si++) {
            int next = (si + 1) % RADIAL_SEGMENTS;
            addTriQuad(quads, verts[0][next], verts[0][si], botCenter, 0.15F, 0.65F, 0.35F, 0.65F, 0.25F, 0.75F);
            addTriQuad(quads, topCenter, verts[nRings - 1][si], verts[nRings - 1][next], 0.75F, 0.25F, 0.65F, 0.15F, 0.85F, 0.15F);
        }

        // 4 gracefully arched & tapered calyx sepal leaves at 45°, 135°, 225°, 315°
        float[] sRad   = {0.65F,  2.85F,  4.85F,  6.08F};
        float[] sY     = {13.50F, 13.95F, 12.90F, 11.20F};
        float[] sWidth = {0.82F,  1.42F,  0.96F,  0.20F};
        float[] sDrop  = {0.00F,  0.24F,  0.20F,  0.04F};
        float[] sCrown = {0.26F,  0.22F,  0.18F,  0.14F};
        float[] sThick = {0.30F,  0.27F,  0.24F,  0.20F};

        float lu0 = 0.50F, lv0 = 0.50F, lu1 = 0.75F, lv1 = 1.00F;
        float luMid = 0.5F * (lu0 + lu1);
        for (int li = 0; li < 4; li++) {
            double ang = Math.toRadians(45.0D + li * 90.0D);
            float ca = (float) Math.cos(ang);
            float sa = (float) Math.sin(ang);
            for (int st = 0; st < sRad.length - 1; st++) {
                float vA = lv0 + (lv1 - lv0) * ((float) st / (sRad.length - 1));
                float vB = lv0 + (lv1 - lv0) * ((float) (st + 1) / (sRad.length - 1));

                float[] c0  = leafPt(ca, sa, sRad[st],     sY[st] + sCrown[st], 0.0F);
                float[] l0  = leafPt(ca, sa, sRad[st],     sY[st] - sDrop[st], -sWidth[st]);
                float[] r0  = leafPt(ca, sa, sRad[st],     sY[st] - sDrop[st],  sWidth[st]);
                float[] c1  = leafPt(ca, sa, sRad[st + 1], sY[st + 1] + sCrown[st + 1], 0.0F);
                float[] l1  = leafPt(ca, sa, sRad[st + 1], sY[st + 1] - sDrop[st + 1], -sWidth[st + 1]);
                float[] r1  = leafPt(ca, sa, sRad[st + 1], sY[st + 1] - sDrop[st + 1],  sWidth[st + 1]);
                addRectQuad(quads, l0, c0, c1, l1, lu0, vA, luMid, vB);
                addRectQuad(quads, c0, r0, r1, c1, luMid, vA, lu1, vB);

                float[] lb0 = leafPt(ca, sa, sRad[st],     sY[st] - sDrop[st] - sThick[st],         -sWidth[st] * 0.86F);
                float[] lb1 = leafPt(ca, sa, sRad[st + 1], sY[st + 1] - sDrop[st + 1] - sThick[st + 1], -sWidth[st + 1] * 0.84F);
                float[] rb0 = leafPt(ca, sa, sRad[st],     sY[st] - sDrop[st] - sThick[st],          sWidth[st] * 0.86F);
                float[] rb1 = leafPt(ca, sa, sRad[st + 1], sY[st + 1] - sDrop[st + 1] - sThick[st + 1],  sWidth[st + 1] * 0.84F);
                addRectQuad(quads, l0, l1, lb1, lb0, lu0, vA, lu0 + 0.05F, vB);
                addRectQuad(quads, r1, r0, rb0, rb1, lu1 - 0.05F, vB, lu1, vA);
                addRectQuad(quads, lb0, lb1, rb1, rb0, lu0, vA, lu1, vB);
            }
        }

        // Tapered 2-stage slightly curved octagonal stem stalk
        float[] stemY = {13.30F, 15.35F, 17.45F};
        float[] stemR = {0.96F,  0.76F,  0.52F};
        float[] stemX = {0.00F,  0.10F,  0.42F};
        float su0 = 0.75F, sv0 = 0.50F, su1 = 1.00F, sv1 = 1.00F;
        for (int st = 0; st < stemY.length - 1; st++) {
            float vA = sv1 - (sv1 - sv0) * ((float) st / (stemY.length - 1));
            float vB = sv1 - (sv1 - sv0) * ((float) (st + 1) / (stemY.length - 1));
            for (int k = 0; k < 8; k++) {
                double a0 = k * Math.PI / 4.0D;
                double a1 = (k + 1) * Math.PI / 4.0D;
                float uA = su0 + (su1 - su0) * (k / 8.0F);
                float uB = su0 + (su1 - su0) * ((k + 1) / 8.0F);
                float[] p00 = {(stemR[st] * (float) Math.cos(a0) + stemX[st]) * P, stemY[st] * P, stemR[st] * (float) Math.sin(a0) * P};
                float[] p10 = {(stemR[st] * (float) Math.cos(a1) + stemX[st]) * P, stemY[st] * P, stemR[st] * (float) Math.sin(a1) * P};
                float[] p11 = {(stemR[st + 1] * (float) Math.cos(a1) + stemX[st + 1]) * P, stemY[st + 1] * P, stemR[st + 1] * (float) Math.sin(a1) * P};
                float[] p01 = {(stemR[st + 1] * (float) Math.cos(a0) + stemX[st + 1]) * P, stemY[st + 1] * P, stemR[st + 1] * (float) Math.sin(a0) * P};
                addRectQuad(quads, p01, p11, p10, p00, uA, vB, uB, vA);
            }
        }
        float[] stemTopCenter = {stemX[2] * P, (stemY[2] + 0.10F) * P, 0.0F};
        for (int k = 0; k < 8; k++) {
            double a0 = k * Math.PI / 4.0D;
            double a1 = (k + 1) * Math.PI / 4.0D;
            float[] p0 = {(stemR[2] * (float) Math.cos(a0) + stemX[2]) * P, stemY[2] * P, stemR[2] * (float) Math.sin(a0) * P};
            float[] p1 = {(stemR[2] * (float) Math.cos(a1) + stemX[2]) * P, stemY[2] * P, stemR[2] * (float) Math.sin(a1) * P};
            addTriQuad(quads, stemTopCenter, p0, p1, 0.875F, 0.56F, 0.80F, 0.62F, 0.94F, 0.62F);
        }

        return Collections.unmodifiableList(quads);
    }

    private static float[] leafPt(float ca, float sa, float radP, float yP, float latP) {
        return new float[]{
                (radP * ca - latP * sa) * P,
                yP * P,
                (radP * sa + latP * ca) * P
        };
    }

    private static void addRectQuad(
            List<Quad> out,
            float[] tl, float[] tr, float[] br, float[] bl,
            float u0, float v0, float u1, float v1
    ) {
        out.add(new Quad(
                tl[0], tl[1], tl[2], u0, v0,
                tr[0], tr[1], tr[2], u1, v0,
                br[0], br[1], br[2], u1, v1,
                bl[0], bl[1], bl[2], u0, v1
        ));
    }

    private static void addTriQuad(
            List<Quad> out,
            float[] a, float[] b, float[] c,
            float ua, float va, float ub, float vb, float uc, float vc
    ) {
        out.add(new Quad(
                a[0], a[1], a[2], ua, va,
                b[0], b[1], b[2], ub, vb,
                c[0], c[1], c[2], uc, vc,
                c[0], c[1], c[2], uc, vc
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(DendroCoreEntity entity) {
        return TEXTURE;
    }

    protected static class CustomRenderType extends RenderType {
        public static RenderType create(ResourceLocation texture) {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_TEX_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .createCompositeState(true);
            return create("velorise_dendro_core", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, true, false, state);
        }

        private CustomRenderType(
                String name,
                VertexFormat format,
                VertexFormat.Mode mode,
                int bufferSize,
                boolean affectsCrumbling,
                boolean sortOnUpload,
                Runnable setupState,
                Runnable clearState
        ) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }
}
