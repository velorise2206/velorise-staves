package com.velorise.staves.element;

import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated server-side idle manager for Hydro Archon Furina (Splendor of Tranquil Waters):
 * - 4041: Ousia (Salon Members playful orbit)
 * - 4042: Pneuma (Hydro Bubble seat + 3-tier Cake Tower)
 */
public final class FurinaManager {
    private FurinaManager() {}

    private static final class FurinaIdleState {
        Vec3 lastStillPos = Vec3.ZERO;
        int stillTicks = 0;
        int activeCode = 4040;
        int lastSyncedCode = -1;
    }

    private static final Map<UUID, FurinaIdleState> FURINA_IDLE_STATES = new ConcurrentHashMap<>();

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            ItemStack furinaStack = ItemStack.EMPTY;
            if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.SPLENDOR) {
                furinaStack = main;
            } else if (!ArcaneStaffItem.isGenshinCharacterWeapon(main)
                    && ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.SPLENDOR) {
                furinaStack = off;
            }
            boolean holding = !furinaStack.isEmpty();

            FurinaIdleState st = FURINA_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new FurinaIdleState());
            if (st.lastStillPos == null || st.lastStillPos.equals(Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holding && player.onGround() && !player.isUsingItem() && !player.swinging && player.getVehicle() == null;
            if (canIdle && distSq < 0.16D) {
                st.stillTicks++;
                if (st.stillTicks >= 60) {
                    st.activeCode = ArcaneStaffItem.isSplendorPneuma(furinaStack) ? 4042 : 4041;
                } else {
                    st.activeCode = 4040;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.activeCode = 4040;
            }
            if (st.activeCode != st.lastSyncedCode || (st.activeCode > 4040 && now % 20L == 0L)) {
                st.lastSyncedCode = st.activeCode;
                ModNetwork.sendStaffAnimation(player, st.activeCode);
            }
        }
    }
}
