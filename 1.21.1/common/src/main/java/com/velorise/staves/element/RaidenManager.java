package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */

public final class RaidenManager {
    private RaidenManager() {}
    private static final DustParticleOptions RAIDEN_EYE_DUST_STD = new DustParticleOptions(new Vector3f(0.76F, 0.25F, 1.0F), 1.25F);
    private static final DustParticleOptions RAIDEN_EYE_DUST_NETH = new DustParticleOptions(new Vector3f(0.76F, 0.25F, 1.0F), 1.6F);
    private static final DustParticleOptions RAIDEN_CHARGE_DUST_STD = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), 1.25F);
    private static final DustParticleOptions RAIDEN_CHARGE_DUST_NETH = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), 1.5F);
    private static final DustParticleOptions RAIDEN_SLASH_DUST_STD = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), 0.58F);
    private static final DustParticleOptions RAIDEN_SLASH_DUST_NETH = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), 0.72F);
    private static final DustParticleOptions RAIDEN_AURA_DUST_STD = new DustParticleOptions(new Vector3f(0.85F, 0.35F, 1.0F), 0.48F);
    private static final DustParticleOptions RAIDEN_AURA_DUST_NETH = new DustParticleOptions(new Vector3f(0.85F, 0.35F, 1.0F), 0.60F);

    public static void onPlayerCastStaffSkill(@Nullable ServerPlayer player) {
        if (player == null) return;
        grantRaidenResolveOnStaffSkill(player);
        if (!hasActiveNightsFocusShield(player)) return;
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        long lastSkill = NIGHTS_FOCUS_LAST_SKILL_STAR.getOrDefault(id, -100L);
        if (now >= lastSkill && now - lastSkill < 6L) return; // 0.3s cooldown
        NIGHTS_FOCUS_LAST_SKILL_STAR.put(id, now);
        addNightsFocusStars(player, 2, now);
    }

    public static void grantRaidenResolveOnStaffSkill(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        UUID id = player.getUUID();
        long now = player.serverLevel().getServer().overworld().getGameTime();
        long last = RAIDEN_LAST_STAFF_RESOLVE_TICK.getOrDefault(id, -100L);
        if (now >= last && now - last < 20L) return;
        boolean hasRaiden = now < RAIDEN_EYE_UNTIL.getOrDefault(id, 0L)
                || ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (!hasRaiden) {
            for (ItemStack invStack : player.getInventory().items) {
                if (ArcaneStaffItem.getEffectiveKind(invStack) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    hasRaiden = true;
                    break;
                }
            }
        }
        if (!hasRaiden) return;
        RAIDEN_LAST_STAFF_RESOLVE_TICK.put(id, now);
        addRaidenResolve(player, 6);
    }

    @Nullable
    static boolean isRaidenProtectedAlly(ServerPlayer owner, LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID())) {
            return true;
        }
        return e instanceof ServerPlayer && owner.getServer() != null && !owner.getServer().isPvpAllowed();
    }

    public static boolean isRaidenSkillOnCooldown(UUID id, long now) {
        return now < RAIDEN_SKILL_CD_UNTIL.getOrDefault(id, 0L);
    }

    public static long getRaidenSkillRemainingTicks(UUID id, long now) {
        return Math.max(0L, RAIDEN_SKILL_CD_UNTIL.getOrDefault(id, 0L) - now);
    }

    public static boolean isRaidenBurstOnCooldown(UUID id, long now) {
        return now < RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L);
    }

    public static long getRaidenBurstRemainingTicks(UUID id, long now) {
        return Math.max(0L, RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L) - now);
    }

    public static boolean isRaidenMusouActive(UUID id, long now) {
        return now < RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L);
    }

    public static boolean isRaidenIframeActive(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel sl)) return false;
        long now = sl.getServer().overworld().getGameTime();
        return now < RAIDEN_IFRAME_UNTIL.getOrDefault(sp.getUUID(), 0L)
                || ArchonUltiManager.isZhongliIframeActive(entity)
                || NeuvilletteManager.isNeuvilletteIframeActive(sp);
    }

    public static int getRaidenResolve(UUID id) {
        return RAIDEN_RESOLVE.getOrDefault(id, 0);
    }

    public static void syncRaidenState(ServerPlayer player, long now) {
        if (player == null) return;
        UUID id = player.getUUID();
        int eyeRem = (int) Math.max(0L, RAIDEN_EYE_UNTIL.getOrDefault(id, 0L) - now);
        int musouRem = (int) Math.max(0L, RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L) - now);
        int resolve = RAIDEN_RESOLVE.getOrDefault(id, 0);
        boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false) || RAIDEN_EYE_NETHERITE.getOrDefault(id, false);
        int burstCd = (int) Math.max(0L, RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L) - now);
        com.velorise.staves.network.ModNetwork.sendRaidenSync(player, eyeRem, musouRem, resolve, neth, burstCd);
    }

    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span, boolean isLeftArm) {
        sendRaidenSlashArc(level, center, forward, comboStep, isMusouBurst, isCharged, span, isLeftArm, false);
    }

    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span, boolean isLeftArm, boolean isEyeProc) {
        Vec3 fwd = new Vec3(forward.x, 0.0D, forward.z);
        if (fwd.lengthSqr() < 1.0E-4D) fwd = new Vec3(0.0D, 0.0D, 1.0D);
        else fwd = fwd.normalize();
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x).normalize();
        if (isLeftArm) right = right.scale(-1.0D);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        int color = isEyeProc ? 0xFF9E42FF : ((isMusouBurst || isCharged) ? 0xFFE0B0FF : 0xFFB84DFF);
        int stepIdxForDur = Math.floorMod(comboStep, 5);
        int dur = isEyeProc ? 8 : (isMusouBurst ? 22 : (stepIdxForDur == 4 ? 14 : (stepIdxForDur == 2 ? 11 : 10)));

        if (isCharged) {
            // Authentic 3-stage Genshin CA choreography (8 visual space tears, 2 damage hits):
            //   comboStep 0 (T+1): Stage 1 opening upward diagonal slash (00:04-00:06)
            //   comboStep 1 (T+4): Stage 2a flurry first 2 cuts (steep left + shallow right, 00:08)
            //   comboStep 2 (T+5): Stage 2b flurry next 3 crossing web cuts (00:09-00:10)
            //   comboStep 3 (T+8): Stage 3 finale giant asymmetric X-rift + cross-star flash (00:11-00:14)
            if (comboStep == 0) {
                Vec3 s = center.add(right.scale(-span * 0.78D)).add(up.scale(-span * 0.42D));
                Vec3 e = center.add(right.scale(span * 0.92D)).add(up.scale(span * 0.64D));
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, s, e, 9, 0xFFB84DFF);
            } else if (comboStep == 1) {
                // Flurry A: steep left upward slash + shallow right upward slash
                Vec3 sL = center.add(right.scale(-span * 0.72D)).add(up.scale(-span * 0.48D));
                Vec3 eL = center.add(right.scale(-span * 0.38D)).add(up.scale(span * 0.76D));
                Vec3 sR = center.add(right.scale(-span * 0.22D)).add(up.scale(-span * 0.56D));
                Vec3 eR = center.add(right.scale(span * 1.02D)).add(up.scale(span * 0.32D));
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, sL, eL, 11, 0xFFB84DFF);
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, sR, eR, 11, 0xFFB84DFF);
            } else if (comboStep == 2) {
                // Flurry B: 3 intersecting dimensional web cuts (low horizontal, mid diagonal, steep right)
                Vec3 sH = center.add(right.scale(-span * 1.05D)).add(up.scale(span * 0.04D));
                Vec3 eH = center.add(right.scale(span * 0.75D)).add(up.scale(-span * 0.34D));
                Vec3 sM = center.add(right.scale(-span * 0.65D)).add(up.scale(span * 0.66D));
                Vec3 eM = center.add(right.scale(span * 0.62D)).add(up.scale(-span * 0.48D));
                Vec3 sV = center.add(right.scale(span * 0.38D)).add(up.scale(span * 0.86D));
                Vec3 eV = center.add(right.scale(span * 0.82D)).add(up.scale(-span * 0.52D));
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, sH, eH, 11, 0xFFB84DFF);
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, sM, eM, 11, 0xFFB84DFF);
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, sV, eV, 11, 0xFFB84DFF);
            } else {
                // Stage 3 Finale: giant asymmetric X-rift + blinding cross-star flash at upper-right intersection
                int finDur = 18;
                Vec3 crossAnchor = center.add(right.scale(span * 0.16D)).add(up.scale(span * 0.16D));
                Vec3 s1 = center.add(right.scale(-span * 1.15D)).add(up.scale(-span * 0.44D));
                Vec3 e1 = center.add(right.scale(span * 1.08D)).add(up.scale(span * 0.60D));
                Vec3 s2 = crossAnchor.add(right.scale(-span * 0.46D)).add(up.scale(span * 1.05D));
                Vec3 e2 = crossAnchor.add(right.scale(span * 0.46D)).add(up.scale(-span * 1.05D));
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, s1, e1, finDur, 0xFFE0B0FF);
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, s2, e2, finDur, 0xFFE0B0FF);
                level.sendParticles(ParticleTypes.END_ROD, crossAnchor.x, crossAnchor.y, crossAnchor.z, 12, 0.28D, 0.28D, 0.28D, 0.08D);
                level.sendParticles(RAIDEN_AURA_DUST_STD, crossAnchor.x, crossAnchor.y, crossAnchor.z, 8, 0.40D, 0.40D, 0.40D, 0.03D);
            }
            return;
        }
        if (isMusouBurst) {
            // Musou no Hitotachi: massive wide horizontal slash from opposite side to sword-hand side
            Vec3 s = center.add(right.scale(-span)).add(up.scale(0.05D));
            Vec3 e = center.add(right.scale(span)).add(up.scale(0.05D));
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s, e, dur, color);
            return;
        }
        // N1..N5 Musou Isshin combo angles matching 1st & 3rd person hand swings
        int step = Math.floorMod(comboStep, 5);
        if (step == 3) {
            // N4: rapid 2-hit cross flurry
            Vec3 s1 = center.add(right.scale(-span * 0.85D)).add(up.scale(-span * 0.42D));
            Vec3 e1 = center.add(right.scale(span * 0.85D)).add(up.scale(span * 0.45D));
            Vec3 s2 = center.add(right.scale(span * 0.85D)).add(up.scale(-span * 0.42D));
            Vec3 e2 = center.add(right.scale(-span * 0.85D)).add(up.scale(span * 0.45D));
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s1, e1, dur, color);
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s2, e2, dur, color);
            return;
        }
        Vec3 s;
        Vec3 e;
        int arcColor = color;
        switch (step) {
            case 0 -> { // N1: diagonal top-left -> bottom-right
                s = center.add(right.scale(-span * 0.85D)).add(up.scale(span * 0.58D));
                e = center.add(right.scale(span * 0.85D)).add(up.scale(-span * 0.52D));
            }
            case 1 -> { // N2: flat horizontal right -> left
                s = center.add(right.scale(span * 1.05D)).add(up.scale(0.06D));
                e = center.add(right.scale(-span * 1.05D)).add(up.scale(0.02D));
            }
            case 2 -> { // N3: steep rising vertical upward slash (bottom-left -> straight up)
                s = center.add(right.scale(-span * 0.25D)).add(up.scale(-span * 0.72D));
                e = center.add(right.scale(span * 0.18D)).add(up.scale(span * 1.18D));
            }
            default -> { // N5: massive low horizontal space-cleaving iai slash (left -> right)
                s = center.add(right.scale(-span * 1.35D)).add(up.scale(-0.18D));
                e = center.add(right.scale(span * 1.35D)).add(up.scale(-0.12D));
                arcColor = 0xFFE0B0FF;
            }
        }
        com.velorise.staves.network.ModNetwork.sendElectricArc(level, s, e, dur, arcColor);
    }

    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span) {
        sendRaidenSlashArc(level, center, forward, comboStep, isMusouBurst, isCharged, span, false);
    }

    public static void addRaidenResolve(ServerPlayer player, int baseAmount) {
        if (player == null || baseAmount <= 0) return;
        boolean hasNetherite = ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                && ArcaneStaffItem.getEffectiveNetherite(player.getMainHandItem());
        int gained = hasNetherite ? (int) Math.ceil(baseAmount * 1.5D) : baseAmount;
        UUID id = player.getUUID();
        int updated = Math.min(60, RAIDEN_RESOLVE.getOrDefault(id, 0) + gained);
        RAIDEN_RESOLVE.put(id, updated);
        if (player.level() instanceof ServerLevel sl) {
            syncRaidenState(player, sl.getServer().overworld().getGameTime());
        }
    }

    public static boolean activateRaidenEye(ServerPlayer player, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (isRaidenSkillOnCooldown(id, now)) {
            double rem = getRaidenSkillRemainingTicks(id, now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "§dBaleful Omen CD: %.1fs", rem)), true);
            return false;
        }
        RAIDEN_EYE_UNTIL.put(id, now + 300L); // Balanced E duration: 15.0s
        RAIDEN_EYE_NETHERITE.put(id, isNetherite);
        RAIDEN_EYE_NEXT_PROC.put(id, now + 18L); // Genshin coordinated strike ICD: 0.9s
        RAIDEN_SKILL_CD_UNTIL.put(id, now + 200L); // Genshin E cooldown: 10.0s
        addRaidenResolve(player, 10);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float dmg = (float) (3.5D * scale * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        double radius = isNetherite ? 6.75D : 4.5D;
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = player.getBoundingBox().inflate(radius, 2.5D, radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                applyElement(level, target, Element.ELECTRO, attackU, player, dmg);
            }
            EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, center, radius, player, dmg);
            cutWildGrassAlongSlash(level, player, center, radius);
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }

        DustParticleOptions electroDust = isNetherite ? RAIDEN_EYE_DUST_NETH : RAIDEN_EYE_DUST_STD;
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0D * i) / 24.0D;
            double px = center.x + Math.cos(angle) * (radius * 0.6D);
            double pz = center.z + Math.sin(angle) * (radius * 0.6D);
            level.sendParticles(electroDust, px, center.y, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        // ParticleTypes.ELECTRIC_SPARK removed
        Vec3 frontEyePos = center.add(player.getLookAngle().scale(1.2D));
        level.sendParticles(ParticleTypes.END_ROD, frontEyePos.x, frontEyePos.y + 0.2D, frontEyePos.z, 10, 0.35D, 0.35D, 0.35D, 0.05D);
        sendRaidenSlashArc(level, frontEyePos, player.getLookAngle(), 0, false, false, isNetherite ? 2.6D : 1.9D, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6F, 1.55F);
        syncRaidenState(player, now);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 6);
        return true;
    }

    public static boolean activateRaidenMusouBurst(ServerPlayer player, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (isRaidenBurstOnCooldown(id, now)) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.ELECTRO, getRaidenBurstRemainingTicks(id, now));
            return false;
        }
        int resolve = RAIDEN_RESOLVE.getOrDefault(id, 0);
        RAIDEN_RESOLVE.put(id, 0);
        float resolveMult = 1.0F + (resolve / 60.0F) * 1.50F;

        RAIDEN_MUSOU_UNTIL.put(id, now + 172L); // 32t draw windup + 7.0s (140t) Musou Isshin stance
        RAIDEN_MUSOU_NETHERITE.put(id, isNetherite);
        long burstCdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? Math.round(360L * 0.90F)
                : 360L;
        RAIDEN_BURST_CD_UNTIL.put(id, now + burstCdTicks); // Genshin Q cooldown: 18.0s (-10% with Anemo Resonance)
        RAIDEN_IFRAME_UNTIL.put(id, now + 34L); // 1.6s Burst i-frame covering full draw animation
        RAIDEN_NEXT_SWING_TICK.put(id, now + 30L); // Lock NA attacks until chest draw + slash finishes
        player.invulnerableTime = Math.max(player.invulnerableTime, 34);
        ArchonUltiManager.applyBurstRoot(player, 25);

        // Smooth forward micro-lunge (~1.1 blocks over ticks 26..28) right as Raiden unleashes Musou no Hitotachi
        for (int lungeStep = 0; lungeStep < 3; lungeStep++) {
            final int stepIdx = lungeStep;
            ArchonUltiManager.scheduleBurstAction(player, 26 + lungeStep, () -> {
                if (!player.isAlive()) return;
                Vec3 l = player.getLookAngle();
                Vec3 h = new Vec3(l.x, 0.0D, l.z);
                if (h.lengthSqr() < 1.0E-4D) h = Vec3.directionFromRotation(0.0F, player.getYRot());
                else h = h.normalize();
                double speed = (stepIdx == 0) ? 0.44D : (stepIdx == 1 ? 0.40D : 0.24D);
                AABB nextBox = player.getDimensions(player.getPose()).makeBoundingBox(player.position().add(h.scale(speed)));
                if (level.noCollision(player, nextBox)) {
                    double vy = Math.max(player.getDeltaMovement().y * 0.25D, 0.01D);
                    player.setDeltaMovement(h.x * speed, vy, h.z * speed);
                    player.fallDistance = 0.0F;
                    player.hurtMarked = true;
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                }
            });
        }

        RAIDEN_PENDING_HITOTACHI_TICK.put(id, now + 28L);
        RAIDEN_PENDING_HITOTACHI_MULT.put(id, resolveMult);

        DustParticleOptions chargeGlow = isNetherite ? RAIDEN_CHARGE_DUST_NETH : RAIDEN_CHARGE_DUST_STD;
        level.sendParticles(chargeGlow, player.getX(), player.getY() + 1.15D, player.getZ(), 12, 0.28D, 0.25D, 0.28D, 0.02D);
        // ParticleTypes.ELECTRIC_SPARK removed
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.35F);
        syncRaidenState(player, now);
        return true;
    }


    /**
     * Engulfing Lightning ("Grasscutter's Light" / Đoạn Thảo) environmental mechanic:
     * Raiden's Electro slashes cleanly shear wild grass, ferns, dead bushes, vines, and hanging roots
     * without harming player crops, flowers, saplings, or solid terrain.
     */
    public static boolean isRaidenCuttableWeed(BlockState st) {
        return st.is(Blocks.SHORT_GRASS)
                || st.is(Blocks.TALL_GRASS)
                || st.is(Blocks.FERN)
                || st.is(Blocks.LARGE_FERN)
                || st.is(Blocks.DEAD_BUSH)
                || st.is(Blocks.VINE)
                || st.is(Blocks.HANGING_ROOTS);
    }

    public static void handleNormalWeedMine(ServerLevel level, ServerPlayer player, BlockState minedState, BlockPos minedPos) {
        if (level == null || player == null || minedState == null || minedPos == null) return;
        if (!isRaidenCuttableWeed(minedState)) return;
        boolean doDrops = !player.isCreative() && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        if (doDrops) {
            BlockState dropState = minedState;
            if (dropState.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)
                    && dropState.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                dropState = dropState.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            }
            net.minecraft.world.level.block.Block.dropResources(dropState, level, minedPos, null, player, new ItemStack(net.minecraft.world.item.Items.SHEARS));
        }
        cutWildGrassAlongSlash(level, player, Vec3.atCenterOf(minedPos), 1.5D);
    }

    public static void cutWildGrassAlongSlash(ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        if (level == null || player == null || center == null || !player.mayBuild()) return;
        double clampedRadius = Math.min(Math.max(1.5D, radius), 5.5D);
        int r = (int) Math.ceil(clampedRadius);
        double r2 = (clampedRadius + 0.5D) * (clampedRadius + 0.5D);
        BlockPos origin = BlockPos.containing(center);
        boolean doDrops = !player.isCreative() && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        ItemStack shears = doDrops ? new ItemStack(net.minecraft.world.item.Items.SHEARS) : ItemStack.EMPTY;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -1, -r), origin.offset(r, 2, r))) {
            if (!level.hasChunkAt(p) || !level.mayInteract(player, p)) continue;
            if (Vec3.atCenterOf(p).distanceToSqr(center) > r2) continue;
            BlockState st = level.getBlockState(p);
            if (st.isAir() || !isRaidenCuttableWeed(st)) continue;
            BlockPos targetPos = p;
            BlockState targetState = st;
            if (st.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)
                    && st.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = p.below();
                BlockState lowerState = level.getBlockState(lowerPos);
                if (isRaidenCuttableWeed(lowerState)) {
                    targetPos = lowerPos;
                    targetState = lowerState;
                }
            }
            if (!com.velorise.staves.event.ArcaneStaffEvents.canPlayerBreakBlock(level, player, targetPos, targetState)) continue;
            if (doDrops) {
                net.minecraft.world.level.block.Block.dropResources(targetState, level, targetPos, null, player, shears);
            }
            level.destroyBlock(targetPos, false, player);
        }
    }

    private static void electrifyWaterAlongSlash(ServerLevel level, ServerPlayer player, Vec3 slashCenter, double radius, float damage) {
        if (level == null || player == null || slashCenter == null) return;
        Vec3 playerPos = player.position();
        int steps = 3;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 sample = playerPos.lerp(slashCenter, t);
            BlockPos base = BlockPos.containing(sample);
            for (int dy = 1; dy >= -3; dy--) {
                BlockPos check = base.offset(0, dy, 0);
                if (level.getFluidState(check).is(FluidTags.WATER)) {
                    ElectrifiedWaterManager.electrify(level, check, radius, 80, player, damage);
                    break;
                }
            }
        }
    }

    public static void executeRaidenHitotachiImpact(ServerPlayer player, boolean isNetherite, float resolveMult) {
        ServerLevel level = player.serverLevel();
        com.velorise.staves.advancement.ModAdvancements.trigger(player, com.velorise.staves.advancement.ModAdvancements.EVENT_INAZUMA_SHINES_ETERNAL);
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float hitotachiDmg = (float) (12.0D * scale * resolveMult * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 3.0F : 2.0F;
        double reach = isNetherite ? 9.0D : 6.0D;
        double aoeRadius = isNetherite ? 5.25D : 3.5D;
        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizLook = new Vec3(look.x, 0.0D, look.z);
        if (horizLook.lengthSqr() < 1.0E-4D) horizLook = Vec3.directionFromRotation(0.0F, player.getYRot());
        else horizLook = horizLook.normalize();
        Vec3 slashCenter = player.getEyePosition().add(horizLook.scale(isNetherite ? 3.8D : 3.2D)).add(0.0D, -0.35D, 0.0D);

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = new AABB(slashCenter, slashCenter).inflate(aoeRadius, 3.0D, aoeRadius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                applyElement(level, target, Element.ELECTRO, attackU, player, hitotachiDmg);
            }
            EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, slashCenter, aoeRadius, player, hitotachiDmg);
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }
        electrifyWaterAlongSlash(level, player, slashCenter, aoeRadius, hitotachiDmg * 0.35F);
        cutWildGrassAlongSlash(level, player, slashCenter, aoeRadius);

        DustParticleOptions violetSlash = isNetherite ? RAIDEN_SLASH_DUST_NETH : RAIDEN_SLASH_DUST_STD;
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-4D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        for (int i = -10; i <= 10; i++) {
            double t = i / 10.0D;
            Vec3 arcPos = slashCenter.add(right.scale(t * aoeRadius)).add(0.0D, Math.cos(t * 1.4D) * 0.6D - 0.3D, 0.0D);
            level.sendParticles(violetSlash, arcPos.x, arcPos.y, arcPos.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
            // ParticleTypes.ELECTRIC_SPARK removed
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0F, 1.15F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.70F);
        sendRaidenSlashArc(level, slashCenter, horizLook, 0, true, false, isNetherite ? 5.8D : 4.6D, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
    }

    public static void triggerRaidenUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) return;
        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(main);
        if (activateRaidenMusouBurst(player, isNetherite)) {
            ArcaneStaffItem.hurtStaffAndCheckBreak(main, 1, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(main.getItem()));
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 8);
        }
    }

    public static void tryTriggerRaidenEye(LivingEntity target, ServerPlayer attacker) {
        if (target == null || attacker == null) return;
        triggerRaidenEyeCoordinatedStrike(target, attacker.damageSources().playerAttack(attacker));
    }

    public static void triggerRaidenEyeCoordinatedStrike(LivingEntity target, DamageSource source) {
        if (target == null || !target.isAlive() || !(target.level() instanceof ServerLevel level)) return;
        if (Boolean.TRUE.equals(IS_PROCESSING_RAIDEN_PROC.get())) return;
        if (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource erds && erds.getReaction() != null) return;
        ServerPlayer attacker = null;
        if (source.getEntity() instanceof ServerPlayer sp) attacker = sp;
        else if (source.getDirectEntity() instanceof ServerPlayer sp) attacker = sp;
        if (attacker == null || attacker == target) return;

        long now = level.getServer().overworld().getGameTime();
        UUID id = attacker.getUUID();
        if (now >= RAIDEN_EYE_UNTIL.getOrDefault(id, 0L)) return;
        if (now < RAIDEN_EYE_NEXT_PROC.getOrDefault(id, 0L)) return;

        RAIDEN_EYE_NEXT_PROC.put(id, now + 18L); // 0.9s Genshin coordinated attack interval
        boolean isNetherite = RAIDEN_EYE_NETHERITE.getOrDefault(id, false);
        addRaidenResolve(attacker, 4);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float dmg = (float) (2.5D * scale * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        double aoe = isNetherite ? 1.5D : 1.0D;
        Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);

        final ServerPlayer finalAttacker = attacker;
        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = target.getBoundingBox().inflate(aoe, 1.0D, aoe);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != finalAttacker && !isRaidenProtectedAlly(finalAttacker, e))) {
                applyElement(level, victim, Element.ELECTRO, attackU, finalAttacker, dmg);
            }
            EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, pos, aoe + 1.5D, finalAttacker, dmg);
            cutWildGrassAlongSlash(level, finalAttacker, pos, aoe + 1.0D);
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }


        // ParticleTypes.ELECTRIC_SPARK removed
        double eyeSpan = isNetherite ? 1.35D : 1.0D; // Kéo dài gấp rưỡi (1.5x) từ 2/3 span lên 1.0 span
        sendRaidenSlashArc(level, pos, attacker.getLookAngle(), 1, false, false, eyeSpan, attacker.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT, true);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.35F, 1.8F);
    }

    public static float applyRaidenMusouIsshinHit(LivingEntity target, DamageSource source, float incomingDamage) {
        if (target == null || !target.isAlive() || incomingDamage <= 0.0F) return incomingDamage;
        if (!(target.level() instanceof ServerLevel level)) return incomingDamage;
        if (Boolean.TRUE.equals(IS_PROCESSING_RAIDEN_PROC.get())) return incomingDamage;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return incomingDamage;
        if (!(source.getDirectEntity() instanceof ServerPlayer attacker) || attacker == target) return incomingDamage;

        long now = level.getServer().overworld().getGameTime();
        UUID id = attacker.getUUID();
        if (!isRaidenMusouActive(id, now)) return incomingDamage;
        ItemStack mainHand = attacker.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            return incomingDamage;
        }

        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(mainHand);
        boolean isCharged = false;
        if (now < RAIDEN_NEXT_SWING_TICK.getOrDefault(id, 0L)) {
            return 0.0F;
        }
        RAIDEN_LAST_MELEE_TICK.put(id, now);
        cancelIdle(attacker);
        RAIDEN_NEXT_SWING_TICK.put(id, now + (isCharged ? 11L : 8L));
        int comboStep = RAIDEN_NA_COMBO.getOrDefault(id, 0);
        RAIDEN_NA_COMBO.put(id, isCharged ? 0 : ((comboStep + 1) % 5));
        float atkStrength = Math.max(0.35F, getEffectiveAttackStrengthScale(attacker));
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float finisherMult = (!isCharged && comboStep == 4) ? 1.25F : 1.0F;
        float chargedMult = isCharged ? 1.35F : finisherMult;
        float musouResMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
        float bonusDmg = (float) (5.0D * scale * musouResMult * atkStrength * chargedMult * StavesConfig.getDamageMultiplier());
        float totalDmg = bonusDmg;
        float attackU = isNetherite ? 1.5F : 1.0F;
        float[] outDamage = new float[] { totalDmg };

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            applyElement(level, target, Element.ELECTRO, attackU, attacker, totalDmg, true, outDamage);
            double sweepRadius = (isNetherite ? 5.25D : 3.5D) * (isCharged ? 1.35D : 1.0D);
            AABB sweepBox = target.getBoundingBox().inflate(sweepRadius, 1.75D, sweepRadius);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e.isAlive() && !e.isSpectator() && e != attacker && e != target && !isRaidenProtectedAlly(attacker, e))) {
                applyElement(level, nearby, Element.ELECTRO, attackU, attacker, bonusDmg * 0.75F);
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }

        Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        electrifyWaterAlongSlash(level, attacker, hitPos, isNetherite ? 4.5D : 3.5D, bonusDmg * 0.45F);
        cutWildGrassAlongSlash(level, attacker, hitPos, isNetherite ? 3.5D : 2.5D);
        sendRaidenSlashArc(level, hitPos, attacker.getLookAngle(), comboStep, false, isCharged, (isNetherite ? 2.4D : 1.75D) * (isCharged ? 1.3D : 1.0D), attacker.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);

        // ParticleTypes.ELECTRIC_SPARK removed
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, isCharged ? 0.65F : 0.48F, isCharged ? 1.55F : 1.85F);
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, isCharged ? 0.55F : 0.40F, isCharged ? 1.65F : 1.95F);
        return outDamage[0];
    }

    public static void performRaidenMusouSwing(ServerPlayer player, boolean isCharged) {
        if (player == null || !player.isAlive()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        boolean musouActive = isRaidenMusouActive(id, now);
        if (!musouActive) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack mainHand = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            return;
        }
        if (now < RAIDEN_NEXT_SWING_TICK.getOrDefault(id, 0L)) return;
        long lastMelee = RAIDEN_LAST_MELEE_TICK.getOrDefault(id, -100L);
        if (now >= lastMelee && now - lastMelee <= 1L) return;

        cancelIdle(player);
        RAIDEN_NEXT_SWING_TICK.put(id, now + (isCharged ? 11L : 8L));
        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(mainHand);
        int comboStep = RAIDEN_NA_COMBO.getOrDefault(id, 0);
        RAIDEN_NA_COMBO.put(id, isCharged ? 0 : ((comboStep + 1) % 5));

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float finisherMult = (!isCharged && comboStep == 4) ? 1.25F : 1.0F;
        float chargedMult = isCharged ? 1.35F : finisherMult;
        float musouResMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
        float slashDmg = (float) (5.0D * scale * musouResMult * chargedMult * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        Vec3 look = player.getLookAngle().normalize();
        double reach = isCharged ? (isNetherite ? 9.5D : 7.5D) : ((isNetherite ? 5.25D : 3.75D) * finisherMult);
        Vec3 rawNaHoriz = new Vec3(look.x, 0.0D, look.z);
        final Vec3 naHoriz = rawNaHoriz.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : rawNaHoriz.normalize();
        double forwardDist = (comboStep == 4) ? (isNetherite ? 3.1D : 2.75D) : (isNetherite ? 2.7D : 2.4D);
        double verticalOff = (comboStep == 4) ? -0.72D : (comboStep == 2 ? -0.14D : -0.32D);
        Vec3 slashPos = player.getEyePosition().add(naHoriz.scale(forwardDist)).add(0.0D, verticalOff, 0.0D);

        if (isCharged) {
            cancelIdle(player);
            RAIDEN_IFRAME_UNTIL.put(id, Math.max(RAIDEN_IFRAME_UNTIL.getOrDefault(id, 0L), now + 10L));
            player.invulnerableTime = Math.max(player.invulnerableTime, 10);
            RAIDEN_NEXT_SWING_TICK.put(id, now + 14L);

            Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
            if (horiz.lengthSqr() < 1.0E-4D) horiz = Vec3.directionFromRotation(0.0F, player.getYRot());
            else horiz = horiz.normalize();
            final Vec3 lungeHoriz = horiz;
            final boolean leftArm = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            final double cleaveReach = isNetherite ? 5.8D : 4.4D;
            final double cleaveHalfWidth = isNetherite ? 3.2D : 2.6D;
            final double arcSpan = isNetherite ? 3.4D : 2.75D;

            // Phase 1 (T0..T2): Short, poised forward iai lunge (~1.8 blocks total, collision-checked)
            for (int tick = 0; tick < 3; tick++) {
                final double stepSpeed = (tick == 0) ? 0.68D : (tick == 1 ? 0.62D : 0.34D);
                final boolean isEnd = (tick == 2);
                Runnable lungeMove = () -> {
                    if (!player.isAlive()) return;
                    AABB nextBox = player.getDimensions(player.getPose()).makeBoundingBox(player.position().add(lungeHoriz.scale(stepSpeed)));
                    if (level.noCollision(player, nextBox)) {
                        double vy = isEnd ? 0.0D : Math.max(player.getDeltaMovement().y * 0.3D, 0.02D);
                        player.setDeltaMovement(lungeHoriz.x * stepSpeed, vy, lungeHoriz.z * stepSpeed);
                        player.fallDistance = 0.0F;
                        player.hurtMarked = true;
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                    }
                };
                if (tick == 0) lungeMove.run();
                else ArchonUltiManager.scheduleBurstAction(player, tick, lungeMove);
            }

            // Stage 1 (T01): Opening upward diagonal slash (visual lead-in matching slow-mo 00:04-00:06)
            ArchonUltiManager.scheduleBurstAction(player, 1, () -> {
                if (!player.isAlive()) return;
                Vec3 liveLook = player.getLookAngle().normalize();
                Vec3 liveHoriz = new Vec3(liveLook.x, 0.0D, liveLook.z);
                if (liveHoriz.lengthSqr() < 1.0E-4D) liveHoriz = lungeHoriz;
                else liveHoriz = liveHoriz.normalize();
                Vec3 openCenter = player.getEyePosition().add(liveHoriz.scale(isNetherite ? 2.9D : 2.5D)).add(0.0D, -0.28D, 0.0D);
                sendRaidenSlashArc(level, openCenter, liveHoriz, 0, false, true, arcSpan * 0.86D, leftArm);
                level.playSound(null, openCenter.x, openCenter.y, openCenter.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.42F, 1.92F);
            });

            // Stage 2a (T04): Flurry Dimensional Web (first 2 cuts) + CA Damage Hit 1 (0.58x)
            ArchonUltiManager.scheduleBurstAction(player, 4, () -> {
                if (!player.isAlive()) return;
                Vec3 liveLook = player.getLookAngle().normalize();
                Vec3 liveHoriz = new Vec3(liveLook.x, 0.0D, liveLook.z);
                if (liveHoriz.lengthSqr() < 1.0E-4D) liveHoriz = lungeHoriz;
                else liveHoriz = liveHoriz.normalize();
                Vec3 liveRight = new Vec3(-liveHoriz.z, 0.0D, liveHoriz.x);
                Vec3 hitCenter = player.getEyePosition().add(liveHoriz.scale(isNetherite ? 3.2D : 2.8D)).add(0.0D, -0.32D, 0.0D);
                float hit1Dmg = slashDmg * 0.58F;
                AABB box = player.getBoundingBox().expandTowards(liveHoriz.scale(cleaveReach)).inflate(cleaveHalfWidth, 2.0D, cleaveHalfWidth);
                IS_PROCESSING_RAIDEN_PROC.set(true);
                try {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                        Vec3 toT = target.position().subtract(player.position());
                        double proj = toT.dot(liveHoriz);
                        double side = Math.abs(toT.dot(liveRight));
                        if (proj >= -0.4D && proj <= cleaveReach + 0.5D && side <= cleaveHalfWidth) {
                            applyElement(level, target, Element.ELECTRO, attackU, player, hit1Dmg);
                        }
                    }
                } finally {
                    IS_PROCESSING_RAIDEN_PROC.set(false);
                }
                electrifyWaterAlongSlash(level, player, hitCenter, isNetherite ? 4.2D : 3.2D, hit1Dmg * 0.45F);
                cutWildGrassAlongSlash(level, player, hitCenter, isNetherite ? 3.8D : 3.0D);
                sendRaidenSlashArc(level, hitCenter, liveHoriz, 1, false, true, arcSpan * 0.94D, leftArm);
                level.playSound(null, hitCenter.x, hitCenter.y, hitCenter.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.60F, 1.70F);
                level.playSound(null, hitCenter.x, hitCenter.y, hitCenter.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.46F, 1.85F);
            });

            // Stage 2b (T05): Flurry Dimensional Web (next 3 intersecting cuts matching slow-mo 00:09-00:10)
            ArchonUltiManager.scheduleBurstAction(player, 5, () -> {
                if (!player.isAlive()) return;
                Vec3 liveLook = player.getLookAngle().normalize();
                Vec3 liveHoriz = new Vec3(liveLook.x, 0.0D, liveLook.z);
                if (liveHoriz.lengthSqr() < 1.0E-4D) liveHoriz = lungeHoriz;
                else liveHoriz = liveHoriz.normalize();
                Vec3 webCenter = player.getEyePosition().add(liveHoriz.scale(isNetherite ? 3.25D : 2.85D)).add(0.0D, -0.32D, 0.0D);
                sendRaidenSlashArc(level, webCenter, liveHoriz, 2, false, true, arcSpan * 0.96D, leftArm);
                level.playSound(null, webCenter.x, webCenter.y, webCenter.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.52F, 1.82F);
            });

            // Stage 3 (T08): Finale Giant Asymmetric X-Rift + Cross-Star Flash + CA Damage Hit 2 (0.68x, bypasses MC i-frame)
            ArchonUltiManager.scheduleBurstAction(player, 8, () -> {
                if (!player.isAlive()) return;
                Vec3 liveLook = player.getLookAngle().normalize();
                Vec3 liveHoriz = new Vec3(liveLook.x, 0.0D, liveLook.z);
                if (liveHoriz.lengthSqr() < 1.0E-4D) liveHoriz = lungeHoriz;
                else liveHoriz = liveHoriz.normalize();
                Vec3 liveRight = new Vec3(-liveHoriz.z, 0.0D, liveHoriz.x);
                Vec3 hitCenter = player.getEyePosition().add(liveHoriz.scale(isNetherite ? 3.3D : 2.9D)).add(0.0D, -0.32D, 0.0D);
                float hit2Dmg = slashDmg * 0.68F;
                AABB box = player.getBoundingBox().expandTowards(liveHoriz.scale(cleaveReach + 0.4D)).inflate(cleaveHalfWidth, 2.0D, cleaveHalfWidth);
                IS_PROCESSING_RAIDEN_PROC.set(true);
                try {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                        Vec3 toT = target.position().subtract(player.position());
                        double proj = toT.dot(liveHoriz);
                        double side = Math.abs(toT.dot(liveRight));
                        if (proj >= -0.4D && proj <= cleaveReach + 0.8D && side <= cleaveHalfWidth) {
                            target.invulnerableTime = 0;
                            applyElement(level, target, Element.ELECTRO, attackU, player, hit2Dmg);
                        }
                    }
                } finally {
                    IS_PROCESSING_RAIDEN_PROC.set(false);
                }
                electrifyWaterAlongSlash(level, player, hitCenter, isNetherite ? 4.5D : 3.5D, hit2Dmg * 0.45F);
                cutWildGrassAlongSlash(level, player, hitCenter, isNetherite ? 4.2D : 3.2D);
                sendRaidenSlashArc(level, hitCenter, liveHoriz, 3, false, true, arcSpan * 1.08D, leftArm);
                level.playSound(null, hitCenter.x, hitCenter.y, hitCenter.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.74F, 1.40F);
                level.playSound(null, hitCenter.x, hitCenter.y, hitCenter.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.60F, 1.52F);
            });
            return;
        }
        // Authentic Genshin 2-hit N4 (comboStep == 3): Hit 1 at T+2 (0.52x) & Hit 2 at T+6 (0.52x, bypasses MC i-frame)
        if (comboStep == 3) {
            final float n4HitDmg = slashDmg * 0.52F;
            final boolean leftArm = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            final double n4Span = isNetherite ? 2.65D : 2.05D;
            for (int hitIdx = 0; hitIdx < 2; hitIdx++) {
                final int hIdx = hitIdx;
                ArchonUltiManager.scheduleBurstAction(player, hitIdx == 0 ? 2 : 6, () -> {
                    if (!player.isAlive()) return;
                    Vec3 liveLook = player.getLookAngle().normalize();
                    Vec3 horiz = new Vec3(liveLook.x, 0.0D, liveLook.z);
                    if (horiz.lengthSqr() < 1.0E-4D) horiz = Vec3.directionFromRotation(0.0F, player.getYRot());
                    else horiz = horiz.normalize();
                    Vec3 right = new Vec3(-horiz.z, 0.0D, horiz.x);
                    double forwardReach = 3.2D;
                    double halfWidth = 2.5D;
                    AABB sweepBox = player.getBoundingBox().expandTowards(horiz.scale(forwardReach)).inflate(halfWidth, 1.75D, halfWidth);
                    IS_PROCESSING_RAIDEN_PROC.set(true);
                    try {
                        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                                e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                            Vec3 toTarget = target.position().subtract(player.position());
                            double proj = toTarget.dot(horiz);
                            double side = Math.abs(toTarget.dot(right));
                            if (proj < -0.3D || proj > forwardReach || side > halfWidth) continue;
                            if (hIdx == 1) target.invulnerableTime = 0;
                            applyElement(level, target, Element.ELECTRO, attackU, player, n4HitDmg);
                        }
                    } finally {
                        IS_PROCESSING_RAIDEN_PROC.set(false);
                    }
                    Vec3 livePos = player.getEyePosition().add(horiz.scale(isNetherite ? 2.7D : 2.4D)).add(0.0D, -0.32D, 0.0D);
                    electrifyWaterAlongSlash(level, player, livePos, isNetherite ? 4.2D : 3.2D, n4HitDmg * 0.45F);
                    cutWildGrassAlongSlash(level, player, livePos, isNetherite ? 3.2D : 2.4D);
                    sendRaidenSlashArc(level, livePos, horiz, hIdx == 0 ? 0 : 1, false, false, n4Span, leftArm);
                    level.playSound(null, livePos.x, livePos.y, livePos.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.46F, hIdx == 0 ? 1.90F : 1.72F);
                    level.playSound(null, livePos.x, livePos.y, livePos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.36F, hIdx == 0 ? 2.0F : 1.82F);
                });
            }
            return;
        }
        ArchonUltiManager.scheduleBurstAction(player, 2, () -> {
            if (!player.isAlive()) return;
            IS_PROCESSING_RAIDEN_PROC.set(true);
            try {
                if (isCharged) {
                    java.util.Set<UUID> hitIds = new java.util.HashSet<>();
                    for (double frac : new double[]{0.35D, 0.75D}) {
                        Vec3 boxCenter = player.getEyePosition().add(look.scale(reach * frac)).add(0.0D, -0.25D, 0.0D);
                        AABB sweepBox = new AABB(boxCenter, boxCenter).inflate(2.4D, 1.85D, 2.4D);
                        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                                e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                            if (hitIds.add(target.getUUID())) {
                                applyElement(level, target, Element.ELECTRO, attackU, player, slashDmg);
                            }
                        }
                    }
                } else {
                    Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
                    if (horiz.lengthSqr() < 1.0E-4D) horiz = Vec3.directionFromRotation(0.0F, player.getYRot());
                    else horiz = horiz.normalize();
                    Vec3 right = new Vec3(-horiz.z, 0.0D, horiz.x);
                    double forwardReach = 3.0D; // 3 blocks forward
                    double halfWidth = 2.5D;    // 5 blocks wide
                    AABB sweepBox = player.getBoundingBox().expandTowards(horiz.scale(forwardReach)).inflate(halfWidth, 1.75D, halfWidth);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                            e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                        Vec3 toTarget = target.position().subtract(player.position());
                        double proj = toTarget.dot(horiz);
                        double side = Math.abs(toTarget.dot(right));
                        if (proj < -0.3D || proj > forwardReach || side > halfWidth) continue;
                        applyElement(level, target, Element.ELECTRO, attackU, player, slashDmg);
                    }
                }
            } finally {
                IS_PROCESSING_RAIDEN_PROC.set(false);
            }

            electrifyWaterAlongSlash(level, player, slashPos, isNetherite ? 4.5D : 3.5D, slashDmg * 0.45F);
            cutWildGrassAlongSlash(level, player, slashPos, (isNetherite ? 3.5D : 2.6D) * (comboStep == 4 ? 1.25D : 1.0D));
            sendRaidenSlashArc(level, slashPos, naHoriz, comboStep, false, isCharged, (isNetherite ? 2.85D : 2.15D) * (comboStep == 4 ? 1.35D : 1.0D), player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
            if (isCharged) {
                Vec3 farSlashPos = player.getEyePosition().add(look.scale(reach * 0.85D)).add(0.0D, -0.15D, 0.0D);
                sendRaidenSlashArc(level, farSlashPos, look, (comboStep + 2) % 4, false, true, (isNetherite ? 2.6D : 2.0D) * 1.3D, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
                // ParticleTypes.ELECTRIC_SPARK removed
            }

            // ParticleTypes.ELECTRIC_SPARK removed
            level.playSound(null, slashPos.x, slashPos.y, slashPos.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, isCharged ? 0.65F : 0.48F, isCharged ? 1.55F : 1.85F);
            level.playSound(null, slashPos.x, slashPos.y, slashPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, isCharged ? 0.55F : 0.40F, isCharged ? 1.65F : 1.95F);
        });
    }

    public static void tickRaidenState(MinecraftServer server, long serverNow) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            boolean eyeActive = serverNow < RAIDEN_EYE_UNTIL.getOrDefault(id, 0L);
            boolean musouActive = serverNow < RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L);

            if (musouActive) {
                ItemStack mainHand = player.getMainHandItem();
                if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    RAIDEN_MUSOU_UNTIL.remove(id);
                    RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                    RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
                    musouActive = false;
                    syncRaidenState(player, serverNow);
                } else {
                    Long pendingHitTick = RAIDEN_PENDING_HITOTACHI_TICK.get(id);
                    if (pendingHitTick != null && serverNow >= pendingHitTick) {
                        RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                        Float pendingMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
                        if (player.isAlive()) {
                            boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false);
                            executeRaidenHitotachiImpact(player, neth, pendingMult);
                        }
                    }
                    if (serverNow % 3 == 0 && player.level() instanceof ServerLevel sl) {
                        boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false);
                        DustParticleOptions aura = neth ? RAIDEN_AURA_DUST_NETH : RAIDEN_AURA_DUST_STD;
                        sl.sendParticles(aura, player.getX(), player.getY() + 1.05D, player.getZ(), 3, 0.35D, 0.4D, 0.35D, 0.01D);
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.05D, player.getZ(), 1, 0.25D, 0.35D, 0.25D, 0.01D);
                        // ParticleTypes.ELECTRIC_SPARK removed
                    }
                }
            }
            if (!musouActive) {
                RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
            }

            if ((eyeActive || musouActive || RAIDEN_RESOLVE.getOrDefault(id, 0) > 0 || isRaidenBurstOnCooldown(id, serverNow)) && serverNow % 10 == 0) {
                syncRaidenState(player, serverNow);
            }
            if (!eyeActive && RAIDEN_EYE_UNTIL.containsKey(id)) {
                RAIDEN_EYE_UNTIL.remove(id);
                syncRaidenState(player, serverNow);
            }
        }
    }

    private static final class RaidenIdleState {
        net.minecraft.world.phys.Vec3 lastStillPos = net.minecraft.world.phys.Vec3.ZERO;
        int stillTicks = 0;
        long lastActionTick = 0L;
        boolean idleActive = false;
        int lastSyncedCode = -1;
    }

    private static final java.util.Map<java.util.UUID, RaidenIdleState> RAIDEN_IDLE_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            RAIDEN_IDLE_STATES.remove(playerId);
        }
    }

    public static void clearAll() {
        RAIDEN_IDLE_STATES.clear();
    }

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            RaidenIdleState st = RAIDEN_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new RaidenIdleState());
            ItemStack main = player.getMainHandItem();
            boolean holding = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
            if (st.lastStillPos == null || st.lastStillPos.equals(net.minecraft.world.phys.Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holding && (now - st.lastActionTick) >= 60L && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 200 && !st.idleActive) {
                    st.idleActive = true;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.idleActive = false;
            }
            int code = st.idleActive ? 4021 : 4020;
            if (code != st.lastSyncedCode || (st.idleActive && now % 20L == 0L)) {
                st.lastSyncedCode = code;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, code);
            }
        }
    }

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        RaidenIdleState st = RAIDEN_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new RaidenIdleState());
        if (st != null) {
            st.stillTicks = 0;
            st.lastActionTick = player.level().getGameTime();
            st.lastStillPos = player.position();
            if (st.idleActive || st.lastSyncedCode == 4021) {
                st.idleActive = false;
                st.lastSyncedCode = 4020;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4020);
            }
        }
    }
}
