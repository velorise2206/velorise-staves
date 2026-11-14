package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * Renders Mavuika's Flamestrider motorcycle in crisp Minecraft voxel/pixel blocks:
 * spinning front/rear wheels, crimson-gold-obsidian chassis, dragonic front cowling,
 * twin exhausts, deployable side jet-wings, 360-degree spin CA, and Ulti slam pitch.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FlamestriderBikeRenderer extends EntityRenderer<FlamestriderBikeEntity> {
    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType BIKE_RENDER_TYPE = BikeRenderType.create();

    public FlamestriderBikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.75F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.FLAMESTRIDER_BIKE.get(), FlamestriderBikeRenderer::new);
    }

    @Override
    public void render(
            FlamestriderBikeEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        float spinLean = Mth.lerp(partialTicks, entity.prevSpinLean, entity.spinLean);
        if (Math.abs(spinLean) > 0.05F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinLean));
        }
        int ultiT = entity.getUltiTicks();
        int naT = entity.getNaTicks();
        applyActionTransforms(entity, ultiT, naT, partialTicks, poseStack);

        VertexConsumer vc = bufferSource.getBuffer(BIKE_RENDER_TYPE);
        boolean neth = entity.isNetherite();

        // Color palette (Crimson Red, Molten Gold, Obsidian Black, Flame Core)
        float crR = neth ? 0.85F : 0.75F, crG = 0.14F, crB = 0.10F;
        float gdR = 1.00F, gdG = 0.76F, gdB = 0.16F;
        float obR = 0.12F, obG = 0.10F, obB = 0.12F;
        float flR = 1.00F, flG = 0.48F, flB = 0.08F;

        // 1. Rear & Front Wheels (spinning)
        renderWheel(poseStack, vc, 0.0F, 0.34F, -0.82F, entity.wheelSpinAngle, obR, obG, obB, crR, crG, crB, gdR, gdG, gdB, flR, flG, flB);
        renderWheel(poseStack, vc, 0.0F, 0.34F, 0.86F, entity.wheelSpinAngle, obR, obG, obB, crR, crG, crB, gdR, gdG, gdB, flR, flG, flB);

        // 2. Rear Swingarm, Engine Block & Tail Fender
        drawBox(poseStack, vc, -0.18F, 0.30F, -0.82F, -0.11F, 0.40F, -0.30F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, 0.11F, 0.30F, -0.82F, 0.18F, 0.40F, -0.30F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, -0.20F, 0.28F, -0.45F, 0.20F, 0.60F, 0.48F, obR, obG, obB, 1.0F); // Lower engine
        drawBox(poseStack, vc, -0.22F, 0.40F, -0.18F, 0.22F, 0.56F, 0.32F, gdR * 0.85F, gdG * 0.85F, gdB * 0.85F, 1.0F); // Gold engine ribs
        // Angled rear tail & fender
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.72F, -0.64F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        drawBox(poseStack, vc, -0.16F, -0.04F, -0.34F, 0.16F, 0.08F, 0.18F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.10F, 0.06F, -0.38F, 0.10F, 0.12F, -0.22F, gdR, gdG, gdB, 1.0F);
        poseStack.popPose();

        // 3. Sculpted Sportbike Tank, Fairing & Contoured Saddle
        drawBox(poseStack, vc, -0.22F, 0.58F, -0.12F, 0.22F, 0.84F, 0.58F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.16F, 0.82F, 0.02F, 0.16F, 0.90F, 0.50F, gdR, gdG, gdB, 1.0F); // Tank spine
        // Gold Natlan side trim fins
        drawBox(poseStack, vc, -0.25F, 0.62F, -0.08F, -0.22F, 0.78F, 0.52F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.22F, 0.62F, -0.08F, 0.25F, 0.78F, 0.52F, gdR, gdG, gdB, 1.0F);
        // Contoured Saddle seat
        drawBox(poseStack, vc, -0.18F, 0.60F, -0.56F, 0.18F, 0.69F, -0.10F, 0.16F, 0.11F, 0.12F, 1.0F);

        // 4. Front Suspension Forks, Dragonic Cowling, Headlight & Handlebars
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.34F, 0.86F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
        // Twin front forks connecting wheel axle up to triple clamp
        drawBox(poseStack, vc, -0.18F, 0.0F, -0.05F, -0.11F, 0.62F, 0.05F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.11F, 0.0F, -0.05F, 0.18F, 0.62F, 0.05F, gdR, gdG, gdB, 1.0F);
        // Front mudguard
        drawBox(poseStack, vc, -0.14F, 0.30F, -0.14F, 0.14F, 0.36F, 0.18F, crR, crG, crB, 1.0F);
        // Dragonic cowling & glowing headlight
        drawBox(poseStack, vc, -0.18F, 0.38F, -0.10F, 0.18F, 0.66F, 0.30F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.12F, 0.46F, 0.26F, 0.12F, 0.60F, 0.36F, flR, flG, flB, 1.0F); // Headlight
        drawBox(poseStack, vc, -0.14F, 0.66F, -0.02F, 0.14F, 0.72F, 0.22F, gdR, gdG, gdB, 1.0F); // Dragon crest
        // Swept Handlebars + gold grips
        drawBox(poseStack, vc, -0.44F, 0.62F, -0.18F, 0.44F, 0.67F, -0.10F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, -0.48F, 0.61F, -0.19F, -0.34F, 0.68F, -0.09F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.34F, 0.61F, -0.19F, 0.48F, 0.68F, -0.09F, gdR, gdG, gdB, 1.0F);
        poseStack.popPose();

        // 5. Twin Rear Exhausts + Flame Cores
        for (float sx : new float[]{-0.22F, 0.22F}) {
            poseStack.pushPose();
            poseStack.translate(sx, 0.48F, -0.84F);
            poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
            drawBox(poseStack, vc, -0.065F, -0.065F, -0.26F, 0.065F, 0.065F, 0.18F, obR, obG, obB, 1.0F);
            drawBox(poseStack, vc, -0.042F, -0.042F, -0.32F, 0.042F, 0.042F, -0.24F, flR, flG, flB, 1.0F);
            poseStack.popPose();
        }

        // 6. Deployable Side Jet-Wings (folded flush when grounded, flared wide open when gliding)
        boolean gliding = entity.isGliding();
        float wingSpread = gliding ? 12.0F : 78.0F;
        float wingLift = gliding ? 14.0F : 0.0F;
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.23F, 0.64F, -0.04F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * wingSpread));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * wingLift));
            float x0 = side < 0 ? -0.58F : 0.0F;
            float x1 = side < 0 ? 0.0F : 0.58F;
            drawBox(poseStack, vc, x0, -0.03F, -0.22F, x1, 0.04F, 0.12F, crR, crG, crB, 1.0F);
            float tx0 = side < 0 ? -0.68F : 0.46F;
            float tx1 = side < 0 ? -0.46F : 0.68F;
            drawBox(poseStack, vc, tx0, -0.02F, -0.26F, tx1, 0.03F, 0.06F, gdR, gdG, gdB, 0.95F);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void renderWheel(
            PoseStack poseStack, VertexConsumer vc,
            float x, float y, float z, float spinRad,
            float tireR, float tireG, float tireB,
            float rimR, float rimG, float rimB,
            float goldR, float goldG, float goldB,
            float flameR, float flameG, float flameB
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotation(spinRad));
        // Octagonal tire profile (3 rotated segments)
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(i * 60.0F));
            drawBox(poseStack, vc, -0.11F, -0.33F, -0.14F, 0.11F, 0.33F, 0.14F, tireR, tireG, tireB, 1.0F);
            poseStack.popPose();
        }
        // Fiery rim ring + crimson spoke + gold axle hub
        drawBox(poseStack, vc, -0.12F, -0.21F, -0.21F, 0.12F, 0.21F, 0.21F, flameR, flameG, flameB, 1.0F);
        drawBox(poseStack, vc, -0.13F, -0.16F, -0.16F, 0.13F, 0.16F, 0.16F, rimR, rimG, rimB, 1.0F);
        drawBox(poseStack, vc, -0.14F, -0.07F, -0.07F, 0.14F, 0.07F, 0.07F, goldR, goldG, goldB, 1.0F);
        poseStack.popPose();
    }

    private static void drawBox(
            PoseStack poseStack, VertexConsumer vc,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        Matrix4f mat = poseStack.last().pose();
        // Top (+Y)
        emit(vc, mat, x0, y1, z1, r, g, b, a);
        emit(vc, mat, x1, y1, z1, r, g, b, a);
        emit(vc, mat, x1, y1, z0, r, g, b, a);
        emit(vc, mat, x0, y1, z0, r, g, b, a);
        // Bottom (-Y)
        emit(vc, mat, x0, y0, z0, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x1, y0, z0, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x1, y0, z1, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x0, y0, z1, r * 0.65F, g * 0.65F, b * 0.65F, a);
        // Front (+Z)
        emit(vc, mat, x0, y0, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x1, y0, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x1, y1, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x0, y1, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        // Back (-Z)
        emit(vc, mat, x1, y0, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x0, y0, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x0, y1, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x1, y1, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        // Left (-X)
        emit(vc, mat, x0, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        // Right (+X)
        emit(vc, mat, x1, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
    }

    private static void emit(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(mat, x, y, z).setColor(r, g, b, a);
    }

    private static void applyActionTransforms(FlamestriderBikeEntity entity, int ultiT, int naT, float partialTicks, PoseStack poseStack) {
        if (entity.isGliding() && !entity.isPlunging() && ultiT <= 0 && naT <= 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        }
        if (entity.isPlunging()) {
            int windup = entity.getPlungeWindup();
            if (windup > 0) {
                float w = (5.0F - windup) / 5.0F;
                poseStack.translate(0.0D, Math.sin(w * Math.PI) * 0.18D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(w, -28.0F, 62.0F)));
            } else {
                poseStack.mulPose(Axis.XP.rotationDegrees(62.0F));
            }
        }
        if (ultiT > 0) {
            float pitch;
            if (ultiT > 20) {
                float soar = Mth.clamp((30.0F - ultiT) / 10.0F, 0.0F, 1.0F);
                pitch = Mth.lerp(soar, 0.0F, -50.0F);
            } else if (ultiT > 14) {
                float apex = (20.0F - ultiT) / 6.0F;
                pitch = Mth.lerp(apex, -50.0F, 16.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F * Mth.sin(apex * (float) Math.PI)));
            } else if (ultiT > 8) {
                float dive = (14.0F - ultiT) / 6.0F;
                pitch = Mth.lerp(dive, 16.0F, 54.0F);
            } else {
                float rec = ultiT / 8.0F;
                pitch = Mth.lerp(rec, 0.0F, 54.0F);
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }

        if (naT > 0) {
            int step = entity.getNaStep();
            if (step == 1) {
                float t = naT / 10.0F;
                float pitch = (t > 0.5F) ? Mth.lerp((t - 0.5F) * 2.0F, 18.0F, -24.0F) : Mth.lerp(t * 2.0F, 0.0F, 18.0F);
                poseStack.translate(0.0D, Math.sin(t * Math.PI) * 0.20D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            } else if (step == 2) {
                float t = naT / 10.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(t, 45.0F, -45.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * Mth.sin(t * (float) Math.PI)));
            } else if (step == 3) {
                float t = naT / 10.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(t, -50.0F, 50.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(22.0F * Mth.sin(t * (float) Math.PI)));
            } else if (step == 4) {
                float t = naT / 10.0F;
                float pitch = (t > 0.5F) ? Mth.lerp((t - 0.5F) * 2.0F, 20.0F, -32.0F) : Mth.lerp(t * 2.0F, 0.0F, 20.0F);
                poseStack.translate(0.0D, Math.sin(t * Math.PI) * 0.35D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            } else if (step == 5) {
                float t = naT / 14.0F;
                poseStack.translate(0.0D, Math.sin(t * Math.PI) * 0.85D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees((1.0F - t) * 360.0F));
            } else if (step == 6) {
                float naTf = Math.max(0.0F, naT - partialTicks);
                float pitch;
                if (naTf > 6.0F) {
                    float w = Mth.clamp((16.0F - naTf) / 10.0F, 0.0F, 1.0F);
                    pitch = Mth.lerp(w, 0.0F, -26.0F);
                    poseStack.translate(0.0D, Math.sin(w * Math.PI * 0.5D) * 0.12D, 0.0D);
                } else if (naTf > 3.0F) {
                    float s = Mth.clamp((6.0F - naTf) / 3.0F, 0.0F, 1.0F);
                    pitch = Mth.lerp(s, -26.0F, 28.0F);
                } else {
                    float r = Mth.clamp((3.0F - naTf) / 3.0F, 0.0F, 1.0F);
                    pitch = Mth.lerp(r, 28.0F, 0.0F);
                }
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            }
        }
    }

    /**
     * Orbits and leans the rider's rendered model alongside the Flamestrider during Donut Spin,
     * NA combo flips, Finisher, and Ulti while keeping the entity/player hitbox at center.
     */
    public static boolean pushRiderBikeTransform(net.minecraft.world.entity.LivingEntity rider, float partialTicks, PoseStack poseStack) {
        if (!(rider.getVehicle() instanceof FlamestriderBikeEntity bike)) {
            return false;
        }
        boolean spinning = bike.isSpinning();
        float spinLean = Mth.lerp(partialTicks, bike.prevSpinLean, bike.spinLean);
        int ultiT = bike.getUltiTicks();
        int naT = bike.getNaTicks();
        if (!spinning && Math.abs(spinLean) <= 0.05F && ultiT <= 0 && naT <= 0 && !bike.isGliding() && !bike.isPlunging()) {
            return false;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.55D, 0.0D);

        float yaw = Mth.lerp(partialTicks, bike.yRotO, bike.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        if (Math.abs(spinLean) > 0.05F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinLean));
        }

        applyActionTransforms(bike, ultiT, naT, partialTicks, poseStack);

        float bodyYaw = Mth.lerp(partialTicks, rider.yBodyRotO, rider.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        poseStack.translate(0.0D, 0.55D, 0.0D);
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FlamestriderBikeEntity entity) {
        return DUMMY_TEXTURE;
    }

    protected static class BikeRenderType extends RenderType {
        public static RenderType create() {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false);
            return create("velorise_flamestrider_bike", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 512, false, true, state);
        }

        private BikeRenderType(
                String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
        ) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }
}
