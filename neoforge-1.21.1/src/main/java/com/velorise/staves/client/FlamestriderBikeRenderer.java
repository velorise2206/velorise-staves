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

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.resources.ResourceLocation pullId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull");
            net.minecraft.resources.ResourceLocation pullingId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling");
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.THE_DAYBREAK_CHRONICLES.get(),
                    VeloriseStavesMod.NETHERITE_THE_DAYBREAK_CHRONICLES.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.getDaybreakPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.isDaybreakPulling(entity, stack) ? 1.0F : 0.0F);
            }
            net.minecraft.resources.ResourceLocation spearStanceId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("velorise_staves", "spear_stance");
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.TSARITSA_BOW.get(),
                    VeloriseStavesMod.NETHERITE_TSARITSA_BOW.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.isTsaritsaPulling(entity, stack) ? 1.0F : 0.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                        (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
            }
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.OMNI_STAFF.get(),
                    VeloriseStavesMod.NETHERITE_OMNI_STAFF.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(stack) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK
                                ? ClientAuraRenderer.getDaybreakPullProgress(entity, stack)
                                : ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> (ClientAuraRenderer.isDaybreakPulling(entity, stack) || ClientAuraRenderer.isTsaritsaPulling(entity, stack)) ? 1.0F : 0.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                        (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
            }
        });
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

        renderBikeMesh(
                poseStack,
                bufferSource,
                this.getFont(),
                entity.isNetherite(),
                entity.wheelSpinAngle,
                (float) entity.getCurrentSpeedBlocksPerSec(),
                (float) entity.getDistanceTraveledBlocks(),
                entity.isGliding(),
                entity.isBikeSprinting() || entity.isSpinning()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    public static void renderBikeMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            net.minecraft.client.gui.Font font,
            boolean neth,
            float wheelSpinAngle,
            float speedBps,
            float odoMeters,
            boolean gliding,
            boolean sprinting
    ) {
        VertexConsumer vc = bufferSource.getBuffer(BIKE_RENDER_TYPE);

        // Rich multi-tone Flamestrider palette (Deep Maroon Nose, Crimson Armor, Molten Gold, Obsidian, Fiery Orange, Bright Amber)
        float mrR = 0.36F, mrG = 0.07F, mrB = 0.10F;
        float crR = neth ? 0.86F : 0.76F, crG = 0.13F, crB = 0.11F;
        float gdR = 1.00F, gdG = 0.78F, gdB = 0.18F;
        float obR = 0.11F, obG = 0.09F, obB = 0.12F;
        float trR = 0.19F, trG = 0.18F, trB = 0.22F;
        float flR = 1.00F, flG = 0.45F, flB = 0.06F;
        float amR = 1.00F, amG = 0.88F, amB = 0.26F;

        // 1. Rear & Front Wheels (with chunky treads)
        renderWheel(poseStack, vc, 0.0F, 0.34F, -0.82F, wheelSpinAngle, obR, obG, obB, crR, crG, crB, gdR, gdG, gdB, flR, flG, flB);
        renderWheel(poseStack, vc, 0.0F, 0.34F, 0.86F, wheelSpinAngle, obR, obG, obB, crR, crG, crB, gdR, gdG, gdB, flR, flG, flB);

        // 2. Rear Swingarm, Sun-Emblem Hub, Engine Block & Radiator Vents
        drawBox(poseStack, vc, -0.18F, 0.30F, -0.82F, -0.11F, 0.40F, -0.30F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, 0.11F, 0.30F, -0.82F, 0.18F, 0.40F, -0.30F, obR, obG, obB, 1.0F);
        // Rear glowing Sun-Emblem side fairings & winglets
        for (int side : new int[]{-1, 1}) {
            float sx0 = side < 0 ? -0.24F : 0.16F;
            float sx1 = side < 0 ? -0.16F : 0.24F;
            drawBox(poseStack, vc, sx0, 0.28F, -0.94F, sx1, 0.44F, -0.64F, crR, crG, crB, 1.0F);
            float gx0 = side < 0 ? -0.255F : 0.235F;
            float gx1 = side < 0 ? -0.235F : 0.255F;
            drawBox(poseStack, vc, gx0, 0.30F, -0.88F, gx1, 0.42F, -0.74F, gdR, gdG, gdB, 1.0F);
            drawBox(poseStack, vc, gx0 - side * 0.006F, 0.33F, -0.85F, gx1 - side * 0.006F, 0.39F, -0.77F, amR, amG, amB, 1.0F);
            // Swept rear winglet fin
            poseStack.pushPose();
            poseStack.translate(side * 0.22F, 0.46F, -0.78F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            drawBox(poseStack, vc, -0.025F, -0.02F, -0.22F, 0.025F, 0.05F, 0.12F, flR, flG, flB, 1.0F);
            poseStack.popPose();
        }
        drawBox(poseStack, vc, -0.20F, 0.28F, -0.45F, 0.20F, 0.60F, 0.48F, obR, obG, obB, 1.0F); // Lower engine
        drawBox(poseStack, vc, -0.22F, 0.40F, -0.18F, 0.22F, 0.56F, 0.32F, gdR * 0.85F, gdG * 0.85F, gdB * 0.85F, 1.0F); // Gold engine ribs
        // Side radiator intake gills & fiery core strips
        for (int g = 0; g < 3; g++) {
            float gz = 0.02F + g * 0.11F;
            drawBox(poseStack, vc, -0.235F, 0.33F, gz, -0.205F, 0.48F, gz + 0.05F, flR, flG, flB, 1.0F);
            drawBox(poseStack, vc, 0.205F, 0.33F, gz, 0.235F, 0.48F, gz + 0.05F, flR, flG, flB, 1.0F);
        }
        // Angled rear tail & fender
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.72F, -0.64F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        drawBox(poseStack, vc, -0.16F, -0.04F, -0.34F, 0.16F, 0.08F, 0.18F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.10F, 0.06F, -0.38F, 0.10F, 0.12F, -0.22F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.12F, 0.01F, -0.42F, 0.12F, 0.06F, -0.34F, flR, flG, flB, 1.0F); // Rear taillight bar
        poseStack.popPose();

        // 3. Sculpted Sportbike Tank, Fairing & Contoured Saddle
        drawBox(poseStack, vc, -0.22F, 0.58F, -0.12F, 0.22F, 0.84F, 0.58F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.16F, 0.82F, 0.02F, 0.16F, 0.90F, 0.50F, gdR, gdG, gdB, 1.0F); // Tank spine
        // Gold & Fiery-Orange Natlan side trim fins + flame decals
        drawBox(poseStack, vc, -0.25F, 0.62F, -0.08F, -0.22F, 0.78F, 0.52F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.22F, 0.62F, -0.08F, 0.25F, 0.78F, 0.52F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.265F, 0.67F, 0.04F, -0.245F, 0.73F, 0.44F, flR, flG, flB, 1.0F);
        drawBox(poseStack, vc, 0.245F, 0.67F, 0.04F, 0.265F, 0.73F, 0.44F, flR, flG, flB, 1.0F);
        // Contoured Saddle seat + backrest lip
        drawBox(poseStack, vc, -0.18F, 0.60F, -0.56F, 0.18F, 0.69F, -0.10F, 0.16F, 0.11F, 0.12F, 1.0F);
        drawBox(poseStack, vc, -0.15F, 0.68F, -0.58F, 0.15F, 0.74F, -0.46F, crR, crG, crB, 1.0F);

        // 4. Front Suspension Forks, Dragonic Cowling, Swept Side Blades, Headlight & Handlebars
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.34F, 0.86F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
        // Twin front forks with dark collar sleeves
        drawBox(poseStack, vc, -0.18F, 0.0F, -0.05F, -0.11F, 0.62F, 0.05F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.11F, 0.0F, -0.05F, 0.18F, 0.62F, 0.05F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.19F, 0.22F, -0.06F, -0.10F, 0.30F, 0.06F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, 0.10F, 0.22F, -0.06F, 0.19F, 0.30F, 0.06F, obR, obG, obB, 1.0F);
        // Aggressive pointed dark-maroon & charcoal-armored shark-nose beak
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.42F, 0.18F);
        poseStack.mulPose(Axis.XP.rotationDegrees(22.0F));
        drawBox(poseStack, vc, -0.17F, -0.08F, -0.14F, 0.17F, 0.12F, 0.36F, mrR, mrG, mrB, 1.0F);
        drawBox(poseStack, vc, -0.12F, 0.06F, 0.02F, 0.12F, 0.14F, 0.48F, trR, trG, trB, 1.0F);
        drawBox(poseStack, vc, -0.08F, 0.02F, 0.36F, 0.08F, 0.11F, 0.56F, obR, obG, obB, 1.0F);
        // Fiery-orange flame trim lines along the nose
        drawBox(poseStack, vc, -0.165F, 0.04F, -0.06F, -0.145F, 0.08F, 0.30F, flR, flG, flB, 1.0F);
        drawBox(poseStack, vc, 0.145F, 0.04F, -0.06F, 0.165F, 0.08F, 0.30F, flR, flG, flB, 1.0F);
        drawBox(poseStack, vc, -0.08F, 0.09F, 0.08F, 0.08F, 0.125F, 0.36F, gdR, gdG, gdB, 1.0F);
        poseStack.popPose();
        // Long, sweeping crimson & gold saber-blades flanking the front wheel down toward the ground
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.21F, 0.32F, 0.02F);
            poseStack.mulPose(Axis.XP.rotationDegrees(44.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 8.0F));
            drawBox(poseStack, vc, -0.04F, -0.52F, -0.10F, 0.04F, 0.24F, 0.12F, mrR, mrG, mrB, 1.0F);
            drawBox(poseStack, vc, -0.045F, -0.46F, -0.06F, 0.045F, 0.20F, 0.09F, crR, crG, crB, 1.0F);
            drawBox(poseStack, vc, -0.052F, -0.42F, 0.04F, 0.052F, 0.16F, 0.11F, flR, flG, flB, 1.0F);
            drawBox(poseStack, vc, -0.03F, -0.58F, -0.04F, 0.03F, -0.46F, 0.08F, amR, amG, amB, 1.0F);
            poseStack.popPose();
        }
        // Dragonic cowling & glowing headlight + smoked visor
        drawBox(poseStack, vc, -0.18F, 0.38F, -0.10F, 0.18F, 0.66F, 0.30F, crR, crG, crB, 1.0F);
        drawBox(poseStack, vc, -0.13F, 0.44F, 0.24F, 0.13F, 0.56F, 0.34F, amR, amG, amB, 1.0F);
        drawBox(poseStack, vc, -0.14F, 0.66F, -0.02F, 0.14F, 0.72F, 0.22F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.11F, 0.70F, -0.06F, 0.11F, 0.78F, 0.10F, obR, obG, obB, 0.90F);
        // Swept Handlebars + gold grips
        drawBox(poseStack, vc, -0.44F, 0.62F, -0.18F, 0.44F, 0.67F, -0.10F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, -0.48F, 0.61F, -0.19F, -0.34F, 0.68F, -0.09F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, 0.34F, 0.61F, -0.19F, 0.48F, 0.68F, -0.09F, gdR, gdG, gdB, 1.0F);

        // 4B. Illuminated Digital Cockpit Dashboard (y = 0.63F, z = -0.14F, angled ~35 deg toward rider)
        float rpmRatio = Mth.clamp(speedBps / 28.0F, 0.12F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.63F, -0.14F);
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
        drawBox(poseStack, vc, -0.16F, -0.02F, -0.02F, 0.16F, 0.11F, 0.025F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, -0.17F, -0.025F, -0.015F, 0.17F, 0.115F, 0.015F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.145F, -0.005F, -0.024F, 0.145F, 0.095F, -0.018F, 0.06F, 0.08F, 0.12F, 1.0F);
        int totalBars = 8;
        int litBars = Math.max(1, Math.round(rpmRatio * totalBars));
        for (int b = 0; b < litBars; b++) {
            float bx0 = -0.132F + b * 0.033F;
            float bx1 = bx0 + 0.025F;
            float frac = b / (float) (totalBars - 1);
            float br = 1.0F;
            float bg = frac < 0.5F ? Mth.lerp(frac * 2.0F, 0.78F, 0.46F) : Mth.lerp((frac - 0.5F) * 2.0F, 0.46F, 0.14F);
            float bb = frac < 0.5F ? Mth.lerp(frac * 2.0F, 0.16F, 0.08F) : 0.10F;
            drawBox(poseStack, vc, bx0, 0.068F, -0.027F, bx1, 0.086F, -0.022F, br, bg, bb, 1.0F);
        }
        poseStack.popPose();
        poseStack.popPose();

        // 5. Twin Rear Exhausts + Dynamic Velocity Flame Cores + Rear Natlan License Plate
        float exhaustBoost = Mth.clamp(speedBps / 24.0F, 0.0F, 1.25F);
        for (float sx : new float[]{-0.22F, 0.22F}) {
            poseStack.pushPose();
            poseStack.translate(sx, 0.48F, -0.84F);
            poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
            drawBox(poseStack, vc, -0.065F, -0.065F, -0.26F, 0.065F, 0.065F, 0.18F, obR, obG, obB, 1.0F);
            float coreHalf = 0.042F + 0.016F * exhaustBoost;
            float coreLen = -0.32F - 0.14F * exhaustBoost;
            drawBox(poseStack, vc, -coreHalf, -coreHalf, coreLen, coreHalf, coreHalf, -0.24F, flR, Mth.clamp(flG + 0.22F * exhaustBoost, 0.0F, 1.0F), flB, 1.0F);
            poseStack.popPose();
        }

        // 5B. Rear Natlan License Plate Frame (z = -0.92F, y = 0.42F)
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.42F, -0.92F);
        poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
        drawBox(poseStack, vc, -0.15F, -0.055F, -0.015F, 0.15F, 0.055F, 0.015F, gdR, gdG, gdB, 1.0F);
        drawBox(poseStack, vc, -0.138F, -0.045F, -0.020F, 0.138F, 0.045F, 0.010F, obR, obG, obB, 1.0F);
        drawBox(poseStack, vc, -0.115F, 0.015F, -0.023F, 0.115F, 0.030F, -0.018F, flR, flG, flB, 1.0F);
        drawBox(poseStack, vc, -0.085F, -0.025F, -0.023F, 0.085F, -0.008F, -0.018F, gdR, gdG, gdB, 1.0F);
        poseStack.popPose();

        // 6. Deployable Side Jet-Wings (folded flush when grounded, flared wide open when gliding)
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

        // 7. Digital cockpit text & License plate text
        if (font != null) {
            String driveState = gliding ? "[AIR]" : (sprinting ? "[S]" : (speedBps > 0.5F ? "[D]" : "[N]"));
            String spdText = String.format(java.util.Locale.ROOT, "%.1f B/s %s", speedBps, driveState);
            String odoText = String.format(java.util.Locale.ROOT, "ODO: %d m", Math.round(odoMeters));

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.34F, 0.86F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            poseStack.translate(0.0F, 0.63F, -0.14F);
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
            poseStack.translate(0.0F, 0.052F, -0.029F);
            poseStack.scale(-0.0032F, -0.0032F, 0.0032F);
            Matrix4f dashMat = poseStack.last().pose();
            font.drawInBatch(spdText, -font.width(spdText) / 2.0F, -2.0F, 0xFFFFCC44, false, dashMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(odoText, -font.width(odoText) / 2.0F, 7.0F, 0xFF66EEFF, false, dashMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();

            String plateText = "NATLAN • 500";
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.42F, -0.92F);
            poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
            poseStack.translate(0.0F, 0.006F, -0.025F);
            poseStack.scale(-0.0034F, -0.0034F, 0.0034F);
            Matrix4f plateMat = poseStack.last().pose();
            font.drawInBatch(plateText, -font.width(plateText) / 2.0F, -3.0F, 0xFFFFB833, false, plateMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();
        }
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
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(i * 45.0F));
            drawBox(poseStack, vc, -0.115F, -0.34F, -0.12F, 0.115F, 0.34F, 0.12F, tireR, tireG, tireB, 1.0F);
            drawBox(poseStack, vc, -0.122F, -0.35F, -0.045F, 0.122F, 0.35F, 0.045F, tireR * 1.55F, tireG * 1.55F, tireB * 1.55F, 1.0F);
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
            float u = Math.max(0.0F, (float) ultiT - partialTicks);
            if (u > 8.0F) {
                if (u >= 19.0F) {
                    // Half 1 (u: 30.0 -> 19.0): steady climb + 180-deg half-donut spin behind/beside rider
                    float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                    float theta = s1 * (float) Math.PI;
                    float sideX = Mth.sin(theta) * 1.15F;
                    float backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                    float arcY = p1 * 0.85F;
                    poseStack.translate(sideX, arcY, backZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-s1 * 180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(theta) * 32.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.sin(theta) * 20.0F));
                } else {
                    // Half 2 (u: 19.0 -> 8.0): complete loop 180 -> 360 deg forward, descend, and cleave at touchdown (u == 8)
                    float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                    float theta = (float) Math.PI * (1.0F + s2);
                    float sideX = Mth.sin(theta) * 1.15F;
                    float backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                    float arcY = (1.0F - p2 * p2) * 0.85F;
                    poseStack.translate(sideX, arcY, backZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F - s2 * 180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(s2 * (float) Math.PI) * 28.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(s2 * (float) Math.PI) * 22.0F));
                }
            } else {
                float rec = Mth.clamp(u / 8.0F, 0.0F, 1.0F);
                poseStack.translate(0.0F, 0.0F, rec * 0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(rec * 14.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(rec * 8.0F));
            }
        }

        if (naT > 0) {
            int step = entity.getNaStep();
            if (step == 1) {
                float t = naT / 10.0F;
                float pitch = (t > 0.5F) ? Mth.lerp((t - 0.5F) * 2.0F, 18.0F, -24.0F) : Mth.lerp(t * 2.0F, 0.0F, 18.0F);
                poseStack.translate(0.0D, Math.sin(t * Math.PI) * 0.20D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            } else if (step == 2) {
                float p = Mth.clamp(1.0F - (((float) naT - partialTicks) / 10.0F), 0.0F, 1.0F);
                float swing = Mth.sin(p * (float) Math.PI);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(p, -28.0F, 32.0F) * swing));
                poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F * swing));
            } else if (step == 3) {
                float p = Mth.clamp(1.0F - (((float) naT - partialTicks) / 10.0F), 0.0F, 1.0F);
                float swing = Mth.sin(p * (float) Math.PI);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(p, 32.0F, -32.0F) * swing));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing));
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
                float yawWhip;
                float pitch;
                float roll;
                if (naTf > 7.0F) {
                    float w = Mth.clamp((16.0F - naTf) / 9.0F, 0.0F, 1.0F);
                    float jumpArc = (float) Math.sin(w * Math.PI * 0.85F) * 0.34F;
                    yawWhip = Mth.lerp(w, 0.0F, -18.0F);
                    pitch = Mth.lerp(w, 0.0F, -18.0F);
                    roll = Mth.lerp(w, 0.0F, -12.0F);
                    poseStack.translate(0.0D, jumpArc, Math.sin(w * Math.PI) * 0.14D);
                } else if (naTf > 2.0F) {
                    float s = Mth.clamp((7.0F - naTf) / 5.0F, 0.0F, 1.0F);
                    float dropY = (1.0F - s) * 0.16F;
                    yawWhip = Mth.lerp(s, -18.0F, 24.0F);
                    pitch = Mth.lerp(s, -18.0F, 11.0F);
                    roll = Mth.lerp(s, -12.0F, 14.0F);
                    poseStack.translate(0.0D, dropY, 0.0D);
                } else {
                    float r = Mth.clamp((2.0F - naTf) / 2.0F, 0.0F, 1.0F);
                    yawWhip = Mth.lerp(r, 24.0F, 0.0F);
                    pitch = Mth.lerp(r, 11.0F, 0.0F);
                    roll = Mth.lerp(r, 14.0F, 0.0F);
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(yawWhip));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            }
        }
    }

    /**
     * Orbits and leans the rider's rendered model alongside the Flamestrider during Donut Spin,
     * NA combo flips, Finisher, and Ulti while keeping the entity/player hitbox at center.
     */
    public static boolean pushRiderBikeTransform(net.minecraft.world.entity.LivingEntity rider, float partialTicks, PoseStack poseStack) {
        if (!(rider.getVehicle() instanceof FlamestriderBikeEntity bike)) {
            if (ClientAuraTracker.isXilonenSkating(rider.getId())
                    && ClientAuraTracker.isXilonenJumpFlipping(rider.getId())
                    && !rider.onGround()
                    && !ClientAuraTracker.isXilonenWallClimbing(rider.getId())
                    && !ClientAuraTracker.isPlunging(rider.getId())
                    && !ClientAuraTracker.isGliding(rider.getId())) {
                poseStack.pushPose();
                float bodyYaw = Mth.rotLerp(partialTicks, rider.yBodyRotO, rider.yBodyRot);
                float flipDeg = ((rider.tickCount + partialTicks) * 30.0F) % 360.0F;
                poseStack.translate(0.0D, 0.90D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(flipDeg));
                poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
                poseStack.translate(0.0D, -0.90D, 0.0D);
                return true;
            }
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
