package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.character.MavuikaVisualRenderer;
import com.velorise.staves.client.character.NahidaVisualRenderer;
import com.velorise.staves.client.character.NeuvilletteVisualRenderer;
import com.velorise.staves.client.character.TsaritsaVisualRenderer;
import com.velorise.staves.client.character.ZhongliVisualRenderer;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated HUD renderer for staff charge bars, custom charge icons (Furina, Ghastfire, Arc Caster, Echo),
 * Genshin stamina gauge, and Shulker target lock-on indicator.
 */
public final class StaffHudRenderer {
    private static final ResourceLocation FURINA_PNEUMA_CHARGE_TEX =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/furina_pneuma_charge.png");
    private static final ResourceLocation FURINA_OUSIA_CHARGE_TEX =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/furina_ousia_charge.png");
    private static final ResourceLocation GHASTFIRE_CHARGE_TEX =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/ghastfire_charge.png");
    private static final ResourceLocation ARC_CASTER_CHARGE_TEX =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/arc_caster_charge.png");
    private static final ResourceLocation ECHO_CHARGE_TEX =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/echo_charge.png");

    private StaffHudRenderer() {}

    public static void renderGenericChargeBar(GuiGraphics guiGraphics, ArcaneStaffItem staff, float pct) {
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int barW = 56;
        int barH = 4;
        int x = (sw - barW) / 2;
        int y = sh / 2 + 12;
        guiGraphics.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0x90000000);
        Element elem = staff.getElement();
        Integer rgb = elem != null ? elem.color().getColor() : null;
        int elemColor = (rgb != null ? rgb : 0xFFFFFF) | 0xFF000000;
        int fillW = Math.max(1, Math.round(barW * pct));
        guiGraphics.fill(x, y, x + fillW, y + barH, elemColor);
    }

    public static void renderChargeProgressHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        renderGenshinStaminaHud(guiGraphics, mc);
        if (NahidaVisualRenderer.renderViewfinderHud(guiGraphics, mc)) return;
        if (TsaritsaVisualRenderer.renderHoldEChargeHud(guiGraphics)) return;
        if (NeuvilletteVisualRenderer.renderChannelingHud(guiGraphics, mc)) return;
        float furinaCaPct = ClientAuraTracker.localFurinaCaHoldProgress;
        if (furinaCaPct > 0.01F) {
            renderFurinaChargeHud(guiGraphics, mc.player.getMainHandItem(), guiGraphics.guiWidth(), guiGraphics.guiHeight(), furinaCaPct);
            return;
        }
        if (!mc.player.isUsingItem()) return;
        ItemStack stack = mc.player.getUseItem();
        ArcaneStaffItem.Kind hudKind = ArcaneStaffItem.getEffectiveKind(stack);
        if (!(stack.getItem() instanceof ArcaneStaffItem staff) || hudKind == null) return;
        int elapsed = Math.max(0, staff.getUseDuration(stack, mc.player) - mc.player.getUseItemRemainingTicks());
        int targetTicks = switch (hudKind) {
            case VORTEX -> 28;
            case SPLENDOR -> 10;
            case THOUSAND_BLAZING_SUNS -> 7;
            case GHASTFIRE -> 40;
            case LIGHTNING -> 60;
            case GUARDIAN -> ArcaneStaffItem.isGuardianInstant(stack) ? 60 : (elapsed < 60 ? 60 : 120);
            default -> 30;
        };
        float pct = Mth.clamp((float) elapsed / (float) targetTicks, 0.0F, 1.0F);
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        if (hudKind == ArcaneStaffItem.Kind.GUARDIAN) {
            NeuvilletteVisualRenderer.renderUseChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.SPLENDOR) {
            renderFurinaChargeHud(guiGraphics, stack, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.DAYBREAK || hudKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                || hudKind == ArcaneStaffItem.Kind.TSARITSA_BOW || hudKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.VORTEX) {
            ZhongliVisualRenderer.renderChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            MavuikaVisualRenderer.renderChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.ECHO) {
            renderEchoChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.GHASTFIRE) {
            renderGhastfireChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.LIGHTNING) {
            renderArcCasterChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        renderGenericChargeBar(guiGraphics, staff, pct);
    }

    public static void renderFurinaChargeHud(GuiGraphics guiGraphics, ItemStack stack, int sw, int sh, float pct) {
        ResourceLocation furinaTex = ArcaneStaffItem.isSplendorPneuma(stack)
                ? FURINA_PNEUMA_CHARGE_TEX
                : FURINA_OUSIA_CHARGE_TEX;
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.20F, 0.30F, 0.45F, 0.58F);
        guiGraphics.blit(furinaTex, iconX, iconY, 0, 0, 16, 16, 16, 16);

        float clamped = Mth.clamp(pct, 0.0F, 1.0F);
        if (clamped > 0.01F) {
            int fillH = Math.max(1, Math.round(16 * clamped));
            int topY = iconY + (16 - fillH);
            float pulse = clamped >= 1.0F ? (0.86F + 0.14F * Mth.sin(System.currentTimeMillis() * 0.03F)) : 1.0F;
            RenderSystem.setShaderColor((0.68F + 0.32F * clamped) * pulse, (0.86F + 0.14F * clamped) * pulse, 1.0F, 1.0F);
            guiGraphics.blit(furinaTex, iconX, topY, 0, 16 - fillH, 16, fillH, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderShulkerTargetLockWorld(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientAuraRenderer.isRenderingScreen() || mc.player == null || mc.level == null) return;
        if (ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) != ArcaneStaffItem.Kind.SHULKER) return;
        LivingEntity locked = ArcaneStaffItem.findShulkerAimTarget(mc.player, 30.0D, 2.0D);
        if (locked == null) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        double tx = Mth.lerp(pt, locked.xo, locked.getX());
        double ty = Mth.lerp(pt, locked.yo, locked.getY()) + locked.getBbHeight() + 0.42D
                + 0.06D * Mth.sin((mc.player.tickCount + pt) * 0.22F);
        double tz = Mth.lerp(pt, locked.zo, locked.getZ());

        VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.CROSSHAIR_CONTRAST);
        poseStack.pushPose();
        poseStack.translate((float) (tx - camPos.x), (float) (ty - camPos.y), (float) (tz - camPos.z));
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        Matrix4f mat = poseStack.last().pose();

        float armW = 0.24F;
        float topY = 0.24F;
        float tipY = -0.02F;
        float thick = 0.055F;
        WindGliderRenderer.emitDoubleQuad(vc, mat,
                -armW, topY, 0.0F,
                -armW + thick, topY, 0.0F,
                 0.0F, tipY + thick * 1.15F, 0.0F,
                -thick * 0.5F, tipY, 0.0F,
                255, 255, 255, 255);
        WindGliderRenderer.emitDoubleQuad(vc, mat,
                 armW - thick, topY, 0.0F,
                 armW, topY, 0.0F,
                 thick * 0.5F, tipY, 0.0F,
                 0.0F, tipY + thick * 1.15F, 0.0F,
                255, 255, 255, 255);
        poseStack.popPose();
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(ClientAuraRenderer.ShieldRenderTypes.CROSSHAIR_CONTRAST);
        }
    }

    public static void renderGhastfireChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation ghastTex = GHASTFIRE_CHARGE_TEX;
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.32F, 0.20F, 0.18F, 0.60F);
        guiGraphics.blit(ghastTex, iconX, iconY, 0, 0, 16, 16, 16, 16);

        float clamped = Mth.clamp(pct, 0.0F, 1.0F);
        if (clamped > 0.01F) {
            int fillH = Math.max(1, Math.round(16 * clamped));
            int topY = iconY + (16 - fillH);
            float pulse = clamped >= 1.0F ? (0.88F + 0.12F * Mth.sin(System.currentTimeMillis() * 0.025F)) : 1.0F;
            RenderSystem.setShaderColor(1.0F, (0.76F + 0.24F * clamped) * pulse, (0.58F + 0.42F * clamped) * pulse, 1.0F);
            guiGraphics.blit(ghastTex, iconX, topY, 0, 16 - fillH, 16, fillH, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderArcCasterChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation arcTex = ARC_CASTER_CHARGE_TEX;
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.24F, 0.14F, 0.36F, 0.58F);
        guiGraphics.blit(arcTex, iconX, iconY, 0, 0, 16, 16, 16, 16);

        float clamped = Mth.clamp(pct, 0.0F, 1.0F);
        if (clamped > 0.01F) {
            int fillH = Math.max(1, Math.round(16 * clamped));
            float flicker = 0.85F + 0.15F * Mth.sin(System.currentTimeMillis() * 0.045F);
            RenderSystem.setShaderColor(1.0F * flicker, (0.72F + 0.28F * clamped) * flicker, 1.0F, 1.0F);
            guiGraphics.blit(arcTex, iconX, iconY, 0, 0, 16, fillH, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderEchoChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation echoTex = ECHO_CHARGE_TEX;
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.22F, 0.32F, 0.38F, 0.55F);
        guiGraphics.blit(echoTex, iconX, iconY, 0, 0, 16, 16, 16, 16);

        float clamped = Mth.clamp(pct, 0.0F, 1.0F);
        if (clamped > 0.01F) {
            if (clamped > 0.66F) {
                float r3 = Mth.clamp((clamped - 0.66F) / 0.34F, 0.0F, 1.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F + 0.65F * r3);
                guiGraphics.blit(echoTex, iconX, iconY, 0, 0, 16, 16, 16, 16);
            }
            if (clamped > 0.33F) {
                float r2 = clamped >= 0.66F ? 1.0F : Mth.clamp((clamped - 0.33F) / 0.33F, 0.0F, 1.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F + 0.65F * r2);
                guiGraphics.blit(echoTex, iconX + 4, iconY + 4, 4, 4, 8, 8, 16, 16);
            }
            float r1 = clamped >= 0.33F ? 1.0F : Mth.clamp(clamped / 0.33F, 0.0F, 1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.40F + 0.60F * r1);
            guiGraphics.blit(echoTex, iconX + 6, iconY + 6, 6, 6, 4, 4, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderGenshinStaminaHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !StavesConfig.isEnableCustomStamina() || mc.player.isCreative() || mc.player.isSpectator()) return;
        float maxStamina = Math.max(100.0F, ClientAuraTracker.localMaxStamina);
        float stamina = Mth.clamp(ClientAuraTracker.localStamina, 0.0F, maxStamina);
        boolean isFull = stamina >= maxStamina - 0.05F;
        if (isFull && ClientAuraTracker.localStaminaFullHideTicks <= 0) return;

        float alpha = isFull ? Mth.clamp(ClientAuraTracker.localStaminaFullHideTicks / 15.0F, 0.0F, 1.0F) : 1.0F;
        if (alpha <= 0.02F) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int x = sw / 2 + 26;
        int totalH = 38;
        int y = sh / 2 - totalH / 2;
        int barW = 4;

        int bgA = Math.round(0x88 * alpha);
        int bgColor = (bgA << 24);
        guiGraphics.fill(x - 1, y - 1, x + barW + 1, y + totalH + 1, bgColor);

        float pct = Mth.clamp(stamina / maxStamina, 0.0F, 1.0F);
        int fillH = Math.round(totalH * pct);
        if (fillH > 0) {
            int baseRgb = (ClientAuraTracker.localStaminaExhausted || pct < 0.22F) ? 0xFF5540 : (pct < 0.45F ? 0xFFCC44 : 0xA8F060);
            int fillA = Math.round(255 * alpha);
            int fillColor = (fillA << 24) | baseRgb;
            guiGraphics.fill(x, y + totalH - fillH, x + barW, y + totalH, fillColor);
        }

        int segA = Math.round(0xAA * alpha);
        int segColor = (segA << 24);
        if (maxStamina > 105.0F) {
            int y100 = y + totalH - Math.round(totalH * (100.0F / maxStamina));
            guiGraphics.fill(x, y100, x + barW, y100 + 1, segColor);
        }
        if (maxStamina > 175.0F) {
            int y170 = y + totalH - Math.round(totalH * (170.0F / maxStamina));
            guiGraphics.fill(x, y170, x + barW, y170 + 1, segColor);
        }
    }
}
