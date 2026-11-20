package com.velorise.staves.client;

import static com.velorise.staves.client.ClientAuraTracker.*;

/**
 * Synchronizes locked 3rd-person body/head yaw snapshots during Archon
 * idle and burst animations on the client.
 */
public final class ClientIdleYawTracker {

    private ClientIdleYawTracker() {}

    public static void syncIdleLockedYaw(Object mcObj) {
        if (mcObj == null) return;
        ClientAccess.syncIdleLockedYawInternal();
    }

    static void syncIdleLockedYawWithPlayer(net.minecraft.world.entity.player.Player localPlayer, boolean isFirstPerson, boolean hasMovementInput) {
        if (localPlayer == null || localPlayer.level() == null) return;
        AuraDisplayData d = DATA.get(localPlayer.getId());
        if (d == null) return;
        boolean movingOrActing = localPlayer.isShiftKeyDown() || localPlayer.isCrouching() || localPlayer.isUsingItem() || localPlayer.swinging
                || !localPlayer.onGround() || localPlayer.getVehicle() != null
                || hasMovementInput;
        long nowUlti = System.currentTimeMillis();
        boolean inUlti = nowUlti < d.staffAnimUntil && (
                d.staffAnimType == ANIM_RAIDEN_DRAW
                || d.staffAnimType == ANIM_ZHONGLI_PLANET_BEFALL
                || d.staffAnimType == ANIM_ZHONGLI_PLANET_BEFALL_FIRST
                || d.staffAnimType == ANIM_VORTEX_HOLD_BURST
                || d.staffAnimType == ANIM_VENTI_BURST
                || d.staffAnimType == ANIM_NAHIDA_BURST
                || d.staffAnimType == ANIM_MAVUIKA_BURST
                || d.staffAnimType == ANIM_TSARITSA_BURST_SUPPORT
                || d.staffAnimType == ANIM_TSARITSA_BURST_DPS
        );
        boolean isFirstPersonUlti = isFirstPerson;
        if (inUlti) {
            if (isFirstPersonUlti || !d.ultiYawSet) {
                d.ultiYaw = localPlayer.getYRot();
                d.ultiYawSet = true;
            }
            localPlayer.yBodyRot = d.ultiYaw;
            localPlayer.yBodyRotO = d.ultiYaw;
        } else {
            d.ultiYawSet = false;
        }
        if (movingOrActing) {
            d.tsaritsaIdle = false; d.tsaritsaIdleFade = 0.0F; d.tsaritsaIdleYawSet = false;
            d.raidenIdle = false; d.raidenIdleFade = 0.0F; d.raidenIdleYawSet = false;
            d.mavuikaIdle = false; d.mavuikaIdleFade = 0.0F; d.mavuikaIdleYawSet = false;
            d.ventiIdle = false; d.ventiIdleFade = 0.0F; d.ventiIdleYawSet = false; d.ventiIdleMode = 0;
            d.zhongliIdleMeteor = false; d.zhongliIdleFade = 0.0F; d.zhongliIdleYawSet = false;
            d.furinaIdleMode = 0; d.furinaIdleFade = 0.0F; d.furinaIdleYawSet = false;
            d.nahidaIdleSwing = false; d.nahidaIdleSwingFade = 0.0F; d.nahidaIdleYawSet = false;
            d.dragonfireIdle = false; d.dragonfireIdleFade = 0.0F; d.dragonfireIdleYawSet = false;
            return;
        }
        if (isTsaritsaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.tsaritsaIdleYawSet) {
                d.tsaritsaIdleYaw = localPlayer.getYRot();
                d.tsaritsaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.tsaritsaIdleYaw;
            localPlayer.yBodyRotO = d.tsaritsaIdleYaw;
        } else {
            d.tsaritsaIdleYawSet = false;
        }
        if (isRaidenIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.raidenIdleYawSet) {
                d.raidenIdleYaw = localPlayer.getYRot();
                d.raidenIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.raidenIdleYaw;
            localPlayer.yBodyRotO = d.raidenIdleYaw;
        } else {
            d.raidenIdleYawSet = false;
        }
        if (isMavuikaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.mavuikaIdleYawSet) {
                d.mavuikaIdleYaw = localPlayer.getYRot();
                d.mavuikaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.mavuikaIdleYaw;
            localPlayer.yBodyRotO = d.mavuikaIdleYaw;
        } else {
            d.mavuikaIdleYawSet = false;
        }
        if (isVentiIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.ventiIdleYawSet) {
                d.ventiIdleYaw = localPlayer.getYRot();
                d.ventiIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.ventiIdleYaw;
            localPlayer.yBodyRotO = d.ventiIdleYaw;
        } else {
            d.ventiIdleYawSet = false;
        }
        if (isZhongliIdleMeteor(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.zhongliIdleYawSet) {
                d.zhongliIdleYaw = localPlayer.getYRot();
                d.zhongliIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.zhongliIdleYaw;
            localPlayer.yBodyRotO = d.zhongliIdleYaw;
        } else {
            d.zhongliIdleYawSet = false;
        }
        if (isFurinaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.furinaIdleYawSet) {
                d.furinaIdleYaw = localPlayer.getYRot();
                d.furinaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.furinaIdleYaw;
            localPlayer.yBodyRotO = d.furinaIdleYaw;
        } else {
            d.furinaIdleYawSet = false;
        }
        if (isNahidaIdleSwing(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.nahidaIdleYawSet) {
                d.nahidaIdleYaw = localPlayer.getYRot();
                d.nahidaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.nahidaIdleYaw;
            localPlayer.yBodyRotO = d.nahidaIdleYaw;
        } else {
            d.nahidaIdleYawSet = false;
        }
        if (isDragonfireIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (!d.dragonfireIdleYawSet) {
                d.dragonfireIdleYaw = localPlayer.getYRot();
                d.dragonfireIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.dragonfireIdleYaw;
            localPlayer.yBodyRotO = d.dragonfireIdleYaw;
        } else {
            d.dragonfireIdleYawSet = false;
        }
    }
}
