package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.DendroCoreEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Self-illuminated 3D OBJ renderer for the Dendro Core matching Genshin Craft's
 * geometry, bobbing, spin, and unlit shader with a 32x32 texture.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DendroCoreRenderer extends EntityRenderer<DendroCoreEntity> {
    private static final ResourceLocation OBJ_MODEL =
            new ResourceLocation(VeloriseStavesMod.MODID, "models/entity/dendro_core.obj");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/dendro_core.png");
    private static final RenderType RENDER_TYPE = CustomRenderType.create(TEXTURE);

    private static volatile List<Tri> cachedMesh;

    private record Tri(
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2
    ) {}

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
        List<Tri> mesh = getMesh();
        if (mesh.isEmpty()) return;

        poseStack.pushPose();
        poseStack.scale(2.5F, 2.5F, 2.5F);
        if (!entity.isHyperbloomVisual()) {
            float bob = Mth.sin((entity.getAge() + partialTicks) / 10.0F + entity.bobOffs) * 0.05F;
            poseStack.translate(0.0F, bob + 0.04F, 0.0F);
        } else {
            poseStack.translate(0.0F, -0.14F, 0.0F);
        }
        poseStack.mulPose(Axis.YP.rotation(entity.getSpin(partialTicks)));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        for (Tri tri : mesh) {
            consumer.vertex(pose, tri.x0, tri.y0, tri.z0).uv(tri.u0, tri.v0).endVertex();
            consumer.vertex(pose, tri.x1, tri.y1, tri.z1).uv(tri.u1, tri.v1).endVertex();
            consumer.vertex(pose, tri.x2, tri.y2, tri.z2).uv(tri.u2, tri.v2).endVertex();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DendroCoreEntity entity) {
        return TEXTURE;
    }

    private static List<Tri> getMesh() {
        List<Tri> local = cachedMesh;
        if (local != null) return local;

        synchronized (DendroCoreRenderer.class) {
            if (cachedMesh != null) return cachedMesh;
            cachedMesh = loadObjMesh();
            return cachedMesh;
        }
    }

    private static List<Tri> loadObjMesh() {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(OBJ_MODEL);
            if (res.isEmpty()) return Collections.emptyList();

            List<float[]> verts = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<Tri> tris = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.get().open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\s+");
                    switch (parts[0]) {
                        case "v" -> {
                            if (parts.length >= 4) {
                                verts.add(new float[]{
                                        Float.parseFloat(parts[1]),
                                        Float.parseFloat(parts[2]),
                                        Float.parseFloat(parts[3])
                                });
                            }
                        }
                        case "vt" -> {
                            if (parts.length >= 3) {
                                uvs.add(new float[]{
                                        Float.parseFloat(parts[1]),
                                        1.0F - Float.parseFloat(parts[2])
                                });
                            }
                        }
                        case "f" -> {
                            if (parts.length >= 4) {
                                int count = parts.length - 1;
                                int[] vi = new int[count];
                                int[] ti = new int[count];
                                for (int i = 0; i < count; i++) {
                                    String[] idx = parts[i + 1].split("/");
                                    vi[i] = Integer.parseInt(idx[0]) - 1;
                                    ti[i] = (idx.length > 1 && !idx[1].isEmpty()) ? (Integer.parseInt(idx[1]) - 1) : -1;
                                }
                                for (int i = 1; i + 1 < count; i++) {
                                    float[] p0 = verts.get(vi[0]);
                                    float[] p1 = verts.get(vi[i]);
                                    float[] p2 = verts.get(vi[i + 1]);
                                    float[] uv0 = (ti[0] >= 0 && ti[0] < uvs.size()) ? uvs.get(ti[0]) : new float[]{0.0F, 0.0F};
                                    float[] uv1 = (ti[i] >= 0 && ti[i] < uvs.size()) ? uvs.get(ti[i]) : new float[]{0.0F, 0.0F};
                                    float[] uv2 = (ti[i + 1] >= 0 && ti[i + 1] < uvs.size()) ? uvs.get(ti[i + 1]) : new float[]{0.0F, 0.0F};
                                    tris.add(new Tri(
                                            p0[0], p0[1], p0[2], uv0[0], uv0[1],
                                            p1[0], p1[1], p1[2], uv1[0], uv1[1],
                                            p2[0], p2[1], p2[2], uv2[0], uv2[1]
                                    ));
                                }
                            }
                        }
                        default -> {}
                    }
                }
            }
            return tris;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
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
            return create("velorise_dendro_core", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES, 256, true, false, state);
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
