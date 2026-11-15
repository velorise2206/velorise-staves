package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates the shared [R] Elemental Burst keybind across all Genshin weapons (1.20.1 Forge).
 */
public final class ArchonUltiManager {
    private static final Map<UUID, Long> ZHONGLI_BURST_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ZHONGLI_IFRAME_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTI_IFRAME_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FURINA_BURST_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAYLA_BURST_CD_UNTIL = new ConcurrentHashMap<>();

    private record PendingPlanetBefall(
            ServerLevel level,
            UUID casterId,
            Vec3 impactPos,
            boolean isNetherite,
            double aoeRadius,
            float damage,
            int petrifyTicks,
            long impactTick
    ) {}

    private static final List<PendingPlanetBefall> ZHONGLI_PENDING_BEFALL = new CopyOnWriteArrayList<>();

    private ArchonUltiManager() {}

    public static void grantUltiIframe(ServerPlayer player, int ticks) {
        if (player == null || !(player.level() instanceof ServerLevel sl) || sl.getServer() == null) return;
        long now = sl.getServer().overworld().getGameTime();
        ULTI_IFRAME_UNTIL.put(player.getUUID(), Math.max(ULTI_IFRAME_UNTIL.getOrDefault(player.getUUID(), 0L), now + ticks));
        player.invulnerableTime = Math.max(player.invulnerableTime, ticks);
    }

    /** Dispatches the shared [R] burst keybind to whichever Genshin weapon the player is holding. */
    public static void triggerUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ElementalGaugeManager.triggerMavuikaUlti(player);
        ElementalGaugeManager.triggerRaidenUlti(player);
        triggerZhongliUlti(player);
        triggerFurinaUlti(player);
        triggerLaylaUlti(player);
        XilonenManager.triggerXilonenUlti(player);
        NeuvilletteManager.triggerNeuvilletteUlti(player);
        NahidaManager.triggerNahidaUlti(player);
        TsaritsaManager.triggerTsaritsaUlti(player);
    }

    /** Plays Venti hold-[R] wind-charge cadence feedback while charging Skyward Harp. */
    public static void tickVentiUltiCharge(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;
        ServerLevel level = (ServerLevel) player.level();
        long t = level.getGameTime();
        if (t % 10L == 0L) {
            level.playSound(null, player.blockPosition(), VeloriseStavesMod.WIND_CHARGE_THROW_SOUND.get(), SoundSource.PLAYERS, 0.45F, 1.25F);
        }
        if (t % 20L == 0L) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.45F);
        }
    }

    /** Casts Venti's Wind's Grand Ode after holding [R] to full charge (with 1.6s I-frames & Burst animation). */
    public static void triggerVentiUlti(ServerPlayer player) {
        if (player == null || !player.isAlive() || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ArcaneStaffItem staff && staff.getKind() == ArcaneStaffItem.Kind.DAYBREAK) {
            if (staff.triggerDaybreakVortexFromKey(player, main)) {
                grantUltiIframe(player, 32);
                ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_VENTI_BURST);
            }
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof ArcaneStaffItem staff && staff.getKind() == ArcaneStaffItem.Kind.DAYBREAK) {
            if (staff.triggerDaybreakVortexFromKey(player, off)) {
                grantUltiIframe(player, 32);
                ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_VENTI_BURST);
            }
        }
    }

    /** Furina [R] Burst: Let the People Rejoice (15s CD, 1.6s I-frames, AoE Hydro stage burst + Universal Revelry Fanfare). */
    public static void triggerFurinaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.isOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        boolean isNeth;
        if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.SPLENDOR) {
            isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        } else {
            hand = InteractionHand.OFF_HAND;
            stack = player.getOffhandItem();
            if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.SPLENDOR) {
                isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
            } else {
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = FURINA_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            double rem = (cdUntil - now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "§b✦ Let the People Rejoice CD: %.1fs", rem)), true);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO) ? 270L : 300L;
        FURINA_BURST_CD_UNTIL.put(id, now + cdTicks);
        grantUltiIframe(player, 32);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_FURINA_BURST);
        final InteractionHand usedHand = hand;
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(usedHand));

        double radius = isNeth ? 8.0D : 6.0D;
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.SPLENDOR, isNeth);
        float burstDmg = (6.0F + player.getMaxHealth() * 0.12F) * scale;
        float gaugeU = isNeth ? 2.5F : 2.0F;
        AABB box = player.getBoundingBox().inflate(radius, 3.0D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.HYDRO, gaugeU, player, burstDmg);
        }

        ElementalGaugeManager.activateFurinaUniversalRevelry(player, isNeth);
        for (int i = 0; i < 28; i++) {
            double ang = (Math.PI * 2.0D * i) / 28.0D;
            double rx = player.getX() + Math.cos(ang) * (radius * 0.65D);
            double rz = player.getZ() + Math.sin(ang) * (radius * 0.65D);
            level.sendParticles(ParticleTypes.SPLASH, rx, player.getY() + 0.5D, rz, 5, 0.25D, 0.5D, 0.25D, 0.06D);
            level.sendParticles(ParticleTypes.END_ROD, rx, player.getY() + 1.1D, rz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.85F, 1.15F);
        player.displayClientMessage(Component.literal("§b✦ Let the People Rejoice! (Universal Revelry Fanfare)"), true);
    }

    /** Layla [R] Burst: Dream of the Star-Stream Shaker (12s CD, 1.6s I-frames, AoE Cryo Dreamsphere + 4 Night Stars). */
    public static void triggerLaylaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.isOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        boolean isNeth;
        if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.NIGHTS_FOCUS) {
            isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        } else {
            hand = InteractionHand.OFF_HAND;
            stack = player.getOffhandItem();
            if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.NIGHTS_FOCUS) {
                isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
            } else {
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = LAYLA_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            double rem = (cdUntil - now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "§b✦ Star-Stream Shaker CD: %.1fs", rem)), true);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO) ? 216L : 240L;
        LAYLA_BURST_CD_UNTIL.put(id, now + cdTicks);
        grantUltiIframe(player, 32);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_LAYLA_BURST);
        final InteractionHand usedHand = hand;
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(usedHand));

        double radius = isNeth ? 7.5D : 6.0D;
        float burstDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.NIGHTS_FOCUS, isNeth) * 1.5F;
        float gaugeU = isNeth ? 2.0F : 1.5F;
        AABB box = player.getBoundingBox().inflate(radius, 3.0D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.CRYO, gaugeU, player, burstDmg);
        }

        ElementalGaugeManager.activateNightsFocusBurstStars(player, isNeth);
        for (int i = 0; i < 24; i++) {
            double ang = (Math.PI * 2.0D * i) / 24.0D;
            double rx = player.getX() + Math.cos(ang) * (radius * 0.6D);
            double rz = player.getZ() + Math.sin(ang) * (radius * 0.6D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, rx, player.getY() + 1.8D, rz, 3, 0.2D, 0.3D, 0.2D, 0.03D);
            level.sendParticles(ParticleTypes.END_ROD, rx, player.getY() + 2.2D, rz, 1, 0.1D, 0.1D, 0.1D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.50F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.75F, 1.45F);
        player.displayClientMessage(Component.literal("§b✦ Dream of the Star-Stream Shaker! (+4 Night Stars)"), true);
    }

    /** Checks for an equipped Vortex Vanquisher and launches Planet Befall. */
    public static void triggerZhongliUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.isOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        ArcaneStaffItem vortexStaff;
        if (stack.getItem() instanceof ArcaneStaffItem staff && staff.kind() == ArcaneStaffItem.Kind.VORTEX) {
            vortexStaff = staff;
        } else {
            hand = InteractionHand.OFF_HAND;
            stack = player.getOffhandItem();
            if (stack.getItem() instanceof ArcaneStaffItem staffOff && staffOff.kind() == ArcaneStaffItem.Kind.VORTEX) {
                vortexStaff = staffOff;
            } else {
                return;
            }
        }
        activateZhongliPlanetBefall(player, stack, hand, vortexStaff.isNetherite());
    }

    private static void activateZhongliPlanetBefall(ServerPlayer player, ItemStack stack, InteractionHand hand, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = ZHONGLI_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            double rem = (cdUntil - now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "§6✦ Planet Befall CD: %.1fs", rem)), true);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? 216L : 240L;
        ZHONGLI_BURST_CD_UNTIL.put(id, now + cdTicks);
        ZHONGLI_IFRAME_UNTIL.put(id, now + 52L);
        grantUltiIframe(player, 52);
        ElementalGaugeManager.onPlayerCastStaffSkill(player);
        final InteractionHand usedHand = hand;
        stack.hurtAndBreak(isNetherite ? 3 : 2, player, p -> p.broadcastBreakEvent(usedHand));

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 rayEnd = eye.add(look.scale(14.0D));

        double bestDistSq = Double.MAX_VALUE;
        Vec3 bestEntityHit = null;
        LivingEntity hitLiving = null;
        AABB searchBox = new AABB(eye, rayEnd).inflate(1.25D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                ent -> ent.isAlive() && !ent.isSpectator() && ent != player && !ElementalGaugeManager.isRaidenProtectedAlly(player, ent))) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.45D).clip(eye, rayEnd);
            if (clip.isPresent()) {
                double d2 = eye.distanceToSqr(clip.get());
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    bestEntityHit = clip.get();
                    hitLiving = e;
                }
            }
        }

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDistSq = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceToSqr(blockHit.getLocation())
                : Double.MAX_VALUE;

        Vec3 impactPos;
        if (bestEntityHit != null && bestDistSq <= blockDistSq) {
            BlockHitResult downHit = level.clip(new ClipContext(
                    bestEntityHit.add(0.0D, 0.3D, 0.0D), bestEntityHit.subtract(0.0D, 4.0D, 0.0D),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            impactPos = downHit.getType() == HitResult.Type.BLOCK
                    ? downHit.getLocation()
                    : new Vec3(bestEntityHit.x, hitLiving.getY(), bestEntityHit.z);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            impactPos = blockHit.getLocation();
        } else {
            Vec3 fallback = eye.add(look.scale(8.5D));
            BlockHitResult downHit = level.clip(new ClipContext(
                    fallback.add(0.0D, 2.0D, 0.0D), fallback.subtract(0.0D, 18.0D, 0.0D),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            impactPos = downHit.getType() == HitResult.Type.BLOCK
                    ? downHit.getLocation()
                    : new Vec3(fallback.x, player.getY(), fallback.z);
        }

        Vec3 horizFwd = new Vec3(look.x, 0.0D, look.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) {
            horizFwd = Vec3.directionFromRotation(0.0F, player.getYRot());
        } else {
            horizFwd = horizFwd.normalize();
        }
        Vec3 horizRight = new Vec3(-horizFwd.z, 0.0D, horizFwd.x);
        double backFromPlayer = 5.2D;
        Vec3 skyStart = new Vec3(
                player.getX() - horizFwd.x * backFromPlayer + horizRight.x * 0.45D,
                player.getY() + 17.5D,
                player.getZ() - horizFwd.z * backFromPlayer + horizRight.z * 0.45D
        );

        double aoeRadius = isNetherite ? 8.5D : 6.0D;
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.VORTEX, isNetherite);
        float maxHpBonus = Math.max(0.0F, player.getMaxHealth() - 20.0F) * (isNetherite ? 0.15F : 0.10F);
        float meteorDmg = (float) (((isNetherite ? 7.5D : 5.5D) * scale + maxHpBonus) * StavesConfig.getDamageMultiplier());
        int petrifyTicks = isNetherite ? 100 : 80;

        ModNetwork.sendStaffAnimation(player, 15);
        ModNetwork.sendPlanetBefallMeteor(level, player.getId(), skyStart, impactPos, (float) aoeRadius, 56);
        ZHONGLI_PENDING_BEFALL.add(new PendingPlanetBefall(level, id, impactPos, isNetherite, aoeRadius, meteorDmg, petrifyTicks, now + 50L));

        DustParticleOptions goldAura = new DustParticleOptions(new Vector3f(0.95F, 0.72F, 0.20F), 1.4F);
        level.sendParticles(goldAura, player.getX(), player.getY() + 1.0D, player.getZ(), 14, 0.4D, 0.35D, 0.4D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 0.70F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.85F, 0.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.80F, 0.55F);
        player.displayClientMessage(Component.literal("§6✦ Planet Befall!"), true);
    }

    private static void executeZhongliPlanetBefallImpact(PendingPlanetBefall pending) {
        ServerLevel level = pending.level();
        if (level.getServer() == null) return;
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(pending.casterId());
        Vec3 impact = pending.impactPos();
        double aoeRadius = pending.aoeRadius();
        double horizLimitSq = (aoeRadius + 0.85D) * (aoeRadius + 0.85D);
        long levelNow = level.getGameTime();

        AABB box = new AABB(impact, impact).inflate(aoeRadius + 0.85D, 4.5D, aoeRadius + 0.85D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator()
                        && (caster == null || (e != caster && !ElementalGaugeManager.isRaidenProtectedAlly(caster, e))))) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            if ((dx * dx + dz * dz) > horizLimitSq) continue;

            ElementalGaugeManager.applyElement(level, target, Element.GEO, 4.0F, caster, pending.damage());
            if (target.isAlive() && !ElementalGaugeManager.isBossEntity(target)
                    && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
                ElementalGaugeManager.applyPetrifyState(target, pending.petrifyTicks(), levelNow);
            }
        }

        if (pending.isNetherite() && caster != null && caster.isAlive() && !caster.isSpectator()) {
            ElementalGaugeManager.activateJadeShield(caster, true);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 0.90F);
            double shieldRadiusSq = 8.5D * 8.5D;
            for (ServerPlayer ally : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p != caster)) {
                if (!ElementalGaugeManager.isRaidenProtectedAlly(caster, ally) && !caster.isAlliedTo(ally)) continue;
                if (ally.position().distanceToSqr(impact) <= shieldRadiusSq || ally.distanceToSqr(caster) <= shieldRadiusSq) {
                    ElementalGaugeManager.activateJadeShield(ally, true);
                    level.playSound(null, ally.getX(), ally.getY(), ally.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.80F, 0.95F);
                }
            }
        }

        DustParticleOptions goldBurst = new DustParticleOptions(new Vector3f(0.96F, 0.74F, 0.22F), pending.isNetherite() ? 2.2F : 1.8F);
        DustParticleOptions basaltBurst = new DustParticleOptions(new Vector3f(0.24F, 0.18F, 0.12F), 1.6F);
        level.sendParticles(goldBurst, impact.x, impact.y + 0.65D, impact.z, 36, aoeRadius * 0.42D, 0.55D, aoeRadius * 0.42D, 0.06D);
        level.sendParticles(basaltBurst, impact.x, impact.y + 0.45D, impact.z, 28, aoeRadius * 0.38D, 0.40D, aoeRadius * 0.38D, 0.05D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y + 0.25D, impact.z, 18, aoeRadius * 0.35D, 0.20D, aoeRadius * 0.35D, 0.03D);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.70F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.15F, 0.55F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.15F, 0.60F);
    }

    /** Processes pending delayed Archon Burst impacts on the server tick. */
    public static void tick(MinecraftServer server, long serverNow) {
        if (ZHONGLI_PENDING_BEFALL.isEmpty()) return;
        for (PendingPlanetBefall pending : ZHONGLI_PENDING_BEFALL) {
            if (serverNow >= pending.impactTick()) {
                ZHONGLI_PENDING_BEFALL.remove(pending);
                executeZhongliPlanetBefallImpact(pending);
            }
        }
    }

    /** Returns true while any Genshin weapon's Burst [R] cast animation i-frame is active. */
    public static boolean isZhongliIframeActive(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel sl) || sl.getServer() == null) return false;
        long now = sl.getServer().overworld().getGameTime();
        UUID id = sp.getUUID();
        return now < ZHONGLI_IFRAME_UNTIL.getOrDefault(id, 0L)
                || now < ULTI_IFRAME_UNTIL.getOrDefault(id, 0L)
                || XilonenManager.isXilonenIframeActive(sp)
                || NeuvilletteManager.isNeuvilletteIframeActive(sp)
                || NahidaManager.isNahidaIframeActive(sp)
                || TsaritsaManager.isTsaritsaIframeActive(sp);
    }

    /** Clears per-player Archon Burst state on death, disconnect, or dimension reset. */
    public static void clearEntityState(UUID id) {
        ZHONGLI_BURST_CD_UNTIL.remove(id);
        ZHONGLI_IFRAME_UNTIL.remove(id);
        ULTI_IFRAME_UNTIL.remove(id);
        FURINA_BURST_CD_UNTIL.remove(id);
        LAYLA_BURST_CD_UNTIL.remove(id);
    }

    /** Clears all transient Archon Burst timers and pending impacts on server stop. */
    public static void clearAll() {
        ZHONGLI_BURST_CD_UNTIL.clear();
        ZHONGLI_IFRAME_UNTIL.clear();
        ULTI_IFRAME_UNTIL.clear();
        FURINA_BURST_CD_UNTIL.clear();
        LAYLA_BURST_CD_UNTIL.clear();
        ZHONGLI_PENDING_BEFALL.clear();
    }
}
