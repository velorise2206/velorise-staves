package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.entity.CryoIcicleProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders glowing crystalline Cryo icicle shards launched by Cryo Regisvine.
 */
public class CryoIcicleRenderer extends EntityRenderer<CryoIcicleProjectile> {
    private static final ResourceLocation TEX = new ResourceLocation("velorise_staves", "textures/entity/cryo_regisvine_boss.png");

    public CryoIcicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            CryoIcicleProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        Vec3 vel = entity.getDeltaMovement();
        if (vel.lengthSqr() > 1e-5D) {
            float yaw = (float) (Mth.atan2(vel.x, vel.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(vel.y, Math.sqrt(vel.x * vel.x + vel.z * vel.z)) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }
        float spin = (entity.tickCount + partialTicks) * 18.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(TEX));
        Matrix4f mat = poseStack.last().pose();
        drawCrystalPrism(vc, mat, 0.14F, 0.55F, 145, 232, 255, 235);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void v(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.vertex(mat, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    private static void drawCrystalPrism(VertexConsumer vc, Matrix4f mat, float w, float len, int r, int g, int b, int a) {
        quad(vc, mat, 0, 0, len,  w, 0, 0,  0, w, 0,  0, 0, len, r, g, b, a);
        quad(vc, mat, 0, 0, len,  0, w, 0, -w, 0, 0,  0, 0, len, r, g, b, a);
        quad(vc, mat, 0, 0, len, -w, 0, 0,  0,-w, 0,  0, 0, len, r, g, b, a);
        quad(vc, mat, 0, 0, len,  0,-w, 0,  w, 0, 0,  0, 0, len, r, g, b, a);
        float tail = -len * 0.6F;
        quad(vc, mat, 0, 0, tail,  0, w, 0,  w, 0, 0,  0, 0, tail, r, g, b, a);
        quad(vc, mat, 0, 0, tail, -w, 0, 0,  0, w, 0,  0, 0, tail, r, g, b, a);
        quad(vc, mat, 0, 0, tail,  0,-w, 0, -w, 0, 0,  0, 0, tail, r, g, b, a);
        quad(vc, mat, 0, 0, tail,  w, 0, 0,  0,-w, 0,  0, 0, tail, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int r, int g, int b, int a) {
        v(vc, mat, x1, y1, z1, 0.0F, 0.0F, r, g, b, a);
        v(vc, mat, x2, y2, z2, 0.05F, 0.0F, r, g, b, a);
        v(vc, mat, x3, y3, z3, 0.05F, 0.05F, r, g, b, a);
        v(vc, mat, x4, y4, z4, 0.0F, 0.05F, r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(CryoIcicleProjectile entity) {
        return TEX;
    }
}
