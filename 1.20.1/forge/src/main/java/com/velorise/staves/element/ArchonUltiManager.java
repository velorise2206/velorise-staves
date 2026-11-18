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
    private static final Map<UUID, Long> VENTI_BURST_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ZHONGLI_IFRAME_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTI_IFRAME_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FURINA_BURST_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAYLA_BURST_CD_UNTIL = new ConcurrentHashMap<>();

    public record BurstRootState(ServerLevel level, Vec3 pos, float yRot, float xRot, long untilTick) {}
    public record PendingBurstAction(ServerLevel level, UUID casterId, long executeTick, Runnable action) {}

    private static final Map<UUID, BurstRootState> BURST_ROOT_STATES = new ConcurrentHashMap<>();
    private static final List<PendingBurstAction> PENDING_BURST_ACTIONS = new CopyOnWriteArrayList<>();

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

    /**
     * Roots a player in place with full invulnerability frames for the duration of a 5-star Burst cast animation.
     */
    public static void applyBurstRoot(ServerPlayer player, int rootTicks) {
        if (player == null || !(player.level() instanceof ServerLevel sl) || sl.getServer() == null) return;
        long now = sl.getServer().overworld().getGameTime();
        grantUltiIframe(player, rootTicks + 2);
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        BURST_ROOT_STATES.put(player.getUUID(), new BurstRootState(
                sl, player.position(), player.getYRot(), player.getXRot(), now + rootTicks));
    }

    public static boolean isBurstRooted(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return com.velorise.staves.client.ClientAuraTracker.isFiveStarBurstRootActive(player.getId());
        }
        if (!(player.level() instanceof ServerLevel sl) || sl.getServer() == null) return false;
        BurstRootState root = BURST_ROOT_STATES.get(player.getUUID());
        return root != null && sl.getServer().overworld().getGameTime() < root.untilTick();
    }

    public static void scheduleBurstAction(ServerPlayer player, int delayTicks, Runnable action) {
        if (player == null || !(player.level() instanceof ServerLevel sl) || sl.getServer() == null || action == null) return;
        long now = sl.getServer().overworld().getGameTime();
        PENDING_BURST_ACTIONS.add(new PendingBurstAction(sl, player.getUUID(), now + delayTicks, action));
    }

    public static boolean isGenshinCharacterWeapon(ItemStack stack) {
        return ArcaneStaffItem.isGenshinCharacterWeapon(stack);
    }

    public static void sendMinimalistBurstCooldownAlert(ServerPlayer player, Element element, long remainingTicks) {
        if (player == null || remainingTicks <= 0L) return;
        int sec = Math.max(1, (int) Math.ceil(remainingTicks / 20.0D));
        String color = switch (element != null ? element : Element.ANEMO) {
            case DENDRO -> "§a";
            case GEO -> "§6";
            case HYDRO -> "§b";
            case CRYO -> "§b";
            case PYRO -> "§c";
            case ELECTRO -> "§d";
            case ANEMO -> "§3";
        };
        player.displayClientMessage(Component.literal(color + sec + "s"), true);
    }

    public static boolean isStandingOnGeoConstruct(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        AABB probe = entity.getBoundingBox().inflate(0.12D, 0.35D, 0.12D).move(0.0D, -0.20D, 0.0D);
        List<com.velorise.staves.entity.GeoFangPillarEntity> pillars = entity.level().getEntitiesOfClass(
                com.velorise.staves.entity.GeoFangPillarEntity.class,
                probe,
                e -> e.isAlive() && !e.isRemoved()
        );
        for (com.velorise.staves.entity.GeoFangPillarEntity pillar : pillars) {
            double topY = pillar.getBoundingBox().maxY;
            if (entity.getY() >= topY - 0.40D && entity.getY() <= topY + 0.25D) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerGroundedForBurst(Player player) {
        if (player == null) return false;
        if (player.onGround()) return true;
        if (player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof com.velorise.staves.entity.FlamestriderBikeEntity
                    || vehicle.onGround()
                    || isStandingOnGeoConstruct(vehicle)) return true;
        }
        if (isStandingOnGeoConstruct(player)) return true;
        if (ElementalGaugeManager.isStandingOnWaterSurface(player)) return true;
        return false;
    }

    /** Dispatches the shared [R] burst keybind exclusively to the active Genshin weapon (main-hand priority, off-hand fallback only if main-hand is non-Genshin). */
    public static void triggerUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (!isPlayerGroundedForBurst(player)) {
            return;
        }
        resetAllArchonIdleTimers(player);
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        if (ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack main = player.getMainHandItem();
        if (!isGenshinCharacterWeapon(main)) return;

        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(main);
        if (effKind == null) return;
        switch (effKind) {
            case THOUSAND_BLAZING_SUNS -> ElementalGaugeManager.triggerMavuikaUlti(player);
            case ENGULFING_LIGHTNING -> ElementalGaugeManager.triggerRaidenUlti(player);
            case VORTEX -> triggerZhongliUlti(player);
            case DAYBREAK -> triggerVentiUlti(player);
            case SPLENDOR -> triggerFurinaUlti(player);
            case NIGHTS_FOCUS -> triggerLaylaUlti(player);
            case PEAK_PATROL_SONG -> XilonenManager.triggerXilonenUlti(player);
            case GUARDIAN -> NeuvilletteManager.triggerNeuvilletteUlti(player);
            case THOUSAND_FLOATING_DREAMS -> NahidaManager.triggerNahidaUlti(player);
            case TSARITSA_BOW, TSARITSA_SPEAR -> TsaritsaManager.triggerTsaritsaUlti(player);
            case DRAGONFIRE -> DragonfireCloudManager.triggerDragonfireUlti(player);
            default -> {}
        }
    }

    /** Plays Venti hold-[R] wind-charge cadence feedback while charging Skyward Harp. */
    public static void tickVentiUltiCharge(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;
        if (!isPlayerGroundedForBurst(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        long t = level.getGameTime();
        if (t % 10L == 0L) {
            level.playSound(null, player.blockPosition(), VeloriseStavesMod.WIND_CHARGE_THROW_SOUND.get(), SoundSource.PLAYERS, 0.45F, 1.25F);
        }
        if (t % 20L == 0L) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.45F);
        }
    }

    /** Casts Venti's Wind's Grand Ode on pressing [R] (2.0s / 40-tick Burst animation & root, firing Stormeye at end of cast). */
    public static void triggerVentiUlti(ServerPlayer player) {
        VentiManager.cancelIdle(player);
        if (player == null || !player.isAlive()) return;
        if (!isPlayerGroundedForBurst(player)) {
            return;
        }
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = VENTI_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            sendMinimalistBurstCooldownAlert(player, Element.ANEMO, cdUntil - now);
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.DAYBREAK
                && main.getItem() instanceof ArcaneStaffItem mainStaff) {
            ArcaneStaffItem targetStaff = mainStaff.kind() == ArcaneStaffItem.Kind.OMNI
                    ? ArcaneStaffItem.getAbsorbedStaffForElement(main, Element.ANEMO)
                    : mainStaff;
            if (targetStaff != null && targetStaff.triggerDaybreakVortexFromKey(player, main)) {
                long baseCd = targetStaff.getDaybreakCooldownTicks(true);
                long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                        ? Math.max(20L, Math.round(baseCd * 0.90D)) : baseCd;
                VENTI_BURST_CD_UNTIL.put(id, now + cdTicks);
                applyBurstRoot(player, 40);
                ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_VENTI_BURST);
            }
            return;
        }
    }

    /** Furina [R] Burst: Let the People Rejoice (15s CD, 1.6s I-frames, AoE Hydro stage burst + Universal Revelry Fanfare). */
    public static void triggerFurinaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.SPLENDOR) {
            return;
        }
        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);

        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = FURINA_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            sendMinimalistBurstCooldownAlert(player, Element.HYDRO, cdUntil - now);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO) ? 270L : 300L;
        FURINA_BURST_CD_UNTIL.put(id, now + cdTicks);
        applyBurstRoot(player, 36); // 1.8s (36t) stage fanfare pose + root
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_FURINA_BURST);
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, hand);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);

        final boolean finalIsNeth = isNeth;
        scheduleBurstAction(player, 34, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
            com.velorise.staves.advancement.ModAdvancements.trigger(player, com.velorise.staves.advancement.ModAdvancements.EVENT_LET_THE_PEOPLE_REJOICE);
            double radius = finalIsNeth ? 8.0D : 6.0D;
            float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.SPLENDOR, finalIsNeth);
            float burstDmg = (6.0F + player.getMaxHealth() * 0.12F) * scale;
            float gaugeU = finalIsNeth ? 2.5F : 2.0F;
            AABB box = player.getBoundingBox().inflate(radius, 3.0D, radius);
            for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
                target.invulnerableTime = 0;
                ElementalGaugeManager.applyElement(sl, target, Element.HYDRO, gaugeU, player, burstDmg);
            }

            ElementalGaugeManager.activateFurinaUniversalRevelry(player, finalIsNeth);
            for (int i = 0; i < 28; i++) {
                double ang = (Math.PI * 2.0D * i) / 28.0D;
                double rx = player.getX() + Math.cos(ang) * (radius * 0.65D);
                double rz = player.getZ() + Math.sin(ang) * (radius * 0.65D);
                sl.sendParticles(ParticleTypes.SPLASH, rx, player.getY() + 0.5D, rz, 5, 0.25D, 0.5D, 0.25D, 0.06D);
                sl.sendParticles(ParticleTypes.END_ROD, rx, player.getY() + 1.1D, rz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
            }
            sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.85F, 1.15F);
        });
    }

    /** Layla [R] Burst: Dream of the Star-Stream Shaker (12s CD, 1.6s I-frames, AoE Cryo Dreamsphere + 4 Night Stars). */
    public static void triggerLaylaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.NIGHTS_FOCUS) {
            return;
        }
        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);

        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = LAYLA_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            sendMinimalistBurstCooldownAlert(player, Element.CRYO, cdUntil - now);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO) ? 216L : 240L;
        LAYLA_BURST_CD_UNTIL.put(id, now + cdTicks);
        grantUltiIframe(player, 32);
        applyBurstRoot(player, 24);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_LAYLA_BURST);
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, hand);

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
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_LAYLA_DREAMSPHERE);
        for (int i = 0; i < 24; i++) {
            double ang = (Math.PI * 2.0D * i) / 24.0D;
            double rx = player.getX() + Math.cos(ang) * (radius * 0.6D);
            double rz = player.getZ() + Math.sin(ang) * (radius * 0.6D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, rx, player.getY() + 1.8D, rz, 3, 0.2D, 0.3D, 0.2D, 0.03D);
            level.sendParticles(ParticleTypes.END_ROD, rx, player.getY() + 2.2D, rz, 1, 0.1D, 0.1D, 0.1D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.50F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.75F, 1.45F);
    }

    /** Checks for an equipped Vortex Vanquisher (or Omni Staff locked to Vortex) and launches Planet Befall. */
    public static void triggerZhongliUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.VORTEX) {
            return;
        }
        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        activateZhongliPlanetBefall(player, stack, hand, isNeth);
    }

    private static void activateZhongliPlanetBefall(ServerPlayer player, ItemStack stack, InteractionHand hand, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = ZHONGLI_BURST_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            sendMinimalistBurstCooldownAlert(player, Element.GEO, cdUntil - now);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? 216L : 240L;
        ZHONGLI_BURST_CD_UNTIL.put(id, now + cdTicks);
        ZHONGLI_IFRAME_UNTIL.put(id, now + 52L);
        applyBurstRoot(player, 50);
        ElementalGaugeManager.onPlayerCastStaffSkill(player);
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, isNetherite ? 3 : 2, player, hand);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizFwd = new Vec3(look.x, 0.0D, look.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) {
            horizFwd = Vec3.directionFromRotation(0.0F, player.getYRot());
        } else {
            horizFwd = horizFwd.normalize();
        }
        Vec3 targetPoint = player.position().add(horizFwd.scale(5.0D));
        BlockHitResult downHit = level.clip(new ClipContext(
                targetPoint.add(0.0D, 3.0D, 0.0D), targetPoint.subtract(0.0D, 8.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = downHit.getType() == HitResult.Type.BLOCK
                ? downHit.getLocation()
                : targetPoint;
        Vec3 horizRight = new Vec3(-horizFwd.z, 0.0D, horizFwd.x);
        double backFromPlayer = 2.14D;
        Vec3 skyStart = new Vec3(
                player.getX() - horizFwd.x * backFromPlayer + horizRight.x * 0.45D,
                player.getY() + 12.25D,
                player.getZ() - horizFwd.z * backFromPlayer + horizRight.z * 0.45D
        );

        double aoeRadius = isNetherite ? 8.5D : 6.0D;
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.VORTEX, isNetherite);
        float maxHpBonus = Math.max(0.0F, player.getMaxHealth() - 20.0F) * (isNetherite ? 0.15F : 0.10F);
        float meteorDmg = (float) (((isNetherite ? 7.5D : 5.5D) * scale + maxHpBonus) * StavesConfig.getDamageMultiplier());
        int petrifyTicks = isNetherite ? 100 : 80;

        boolean firstBurstInWorld = !player.getTags().contains("VeloriseFirstZhongliBurstDone");
        if (firstBurstInWorld) {
            player.addTag("VeloriseFirstZhongliBurstDone");
        }
        ModNetwork.sendStaffAnimation(player, firstBurstInWorld
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL_FIRST
                : com.velorise.staves.client.ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL);
        ModNetwork.sendPlanetBefallMeteor(level, player.getId(), skyStart, impactPos, (float) aoeRadius, 56);
        ZHONGLI_PENDING_BEFALL.add(new PendingPlanetBefall(level, id, impactPos, isNetherite, aoeRadius, meteorDmg, petrifyTicks, now + 50L));

        DustParticleOptions goldAura = new DustParticleOptions(new Vector3f(0.95F, 0.72F, 0.20F), 1.4F);
        level.sendParticles(goldAura, player.getX(), player.getY() + 1.0D, player.getZ(), 14, 0.4D, 0.35D, 0.4D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 0.70F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.85F, 0.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BASALT_PLACE, SoundSource.PLAYERS, 1.00F, 0.55F);
    }

    private static void executeZhongliPlanetBefallImpact(PendingPlanetBefall pending) {
        ServerLevel level = pending.level();
        if (level.getServer() == null) return;
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(pending.casterId());
        if (caster != null && caster.isAlive()) {
            com.velorise.staves.advancement.ModAdvancements.trigger(caster, com.velorise.staves.advancement.ModAdvancements.EVENT_THIS_IS_ORDER);
        }
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
                    SoundEvents.BASALT_PLACE, SoundSource.PLAYERS, 1.10F, 0.60F);
            double shieldRadiusSq = 8.5D * 8.5D;
            for (ServerPlayer ally : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p != caster)) {
                if (!ElementalGaugeManager.isRaidenProtectedAlly(caster, ally) && !caster.isAlliedTo(ally)) continue;
                if (ally.position().distanceToSqr(impact) <= shieldRadiusSq || ally.distanceToSqr(caster) <= shieldRadiusSq) {
                    ElementalGaugeManager.activateJadeShield(ally, true);
                    level.playSound(null, ally.getX(), ally.getY(), ally.getZ(),
                            SoundEvents.BASALT_PLACE, SoundSource.PLAYERS, 1.05F, 0.65F);
                }
            }
        }

        DustParticleOptions goldBurst = new DustParticleOptions(new Vector3f(0.96F, 0.74F, 0.22F), pending.isNetherite() ? 2.2F : 1.8F);
        DustParticleOptions basaltBurst = new DustParticleOptions(new Vector3f(0.24F, 0.18F, 0.12F), 1.6F);
        net.minecraft.core.particles.BlockParticleOption deepslateDebris =
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState());
        net.minecraft.core.particles.BlockParticleOption stoneDebris =
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        level.sendParticles(goldBurst, impact.x, impact.y + 0.65D, impact.z, 42, aoeRadius * 0.45D, 0.55D, aoeRadius * 0.45D, 0.06D);
        level.sendParticles(basaltBurst, impact.x, impact.y + 0.45D, impact.z, 36, aoeRadius * 0.42D, 0.40D, aoeRadius * 0.42D, 0.05D);
        level.sendParticles(deepslateDebris, impact.x, impact.y + 0.35D, impact.z, 55, aoeRadius * 0.45D, 0.45D, aoeRadius * 0.45D, 0.18D);
        level.sendParticles(stoneDebris, impact.x, impact.y + 0.30D, impact.z, 45, aoeRadius * 0.50D, 0.35D, aoeRadius * 0.50D, 0.16D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y + 0.20D, impact.z, 28, aoeRadius * 0.45D, 0.18D, aoeRadius * 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.15D, impact.z, 32, aoeRadius * 0.50D, 0.12D, aoeRadius * 0.50D, 0.05D);
        int ringPoints = 28;
        for (int i = 0; i < ringPoints; i++) {
            double ang = (Math.PI * 2.0D * i) / ringPoints;
            double rx = impact.x + Math.cos(ang) * (aoeRadius * 0.85D);
            double rz = impact.z + Math.sin(ang) * (aoeRadius * 0.85D);
            level.sendParticles(deepslateDebris, rx, impact.y + 0.20D, rz, 3, 0.25D, 0.15D, 0.25D, 0.12D);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, rx, impact.y + 0.12D, rz, 2, 0.20D, 0.08D, 0.20D, 0.02D);
            level.sendParticles(ParticleTypes.CLOUD, rx, impact.y + 0.10D, rz, 2, 0.22D, 0.06D, 0.22D, 0.03D);
        }
        // Majestic deep sub-bass seismic rumble, thunder impact, and heavy stone shockwave (smooth & punchy - no harsh screeching)
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.15F, 0.40F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.95F, 0.42F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.10F, 0.45F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, 1.10F, 0.48F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.95F, 0.50F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ANCIENT_DEBRIS_BREAK, SoundSource.PLAYERS, 0.85F, 0.46F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.80F, 0.50F);
    }

    /** Processes pending delayed Archon Burst impacts and 5-star Burst Root states on the server tick. */
    public static void tick(MinecraftServer server, long serverNow) {
        if (!BURST_ROOT_STATES.isEmpty()) {
            for (Map.Entry<UUID, BurstRootState> entry : BURST_ROOT_STATES.entrySet()) {
                BurstRootState root = entry.getValue();
                if (serverNow >= root.untilTick()) {
                    BURST_ROOT_STATES.remove(entry.getKey());
                    continue;
                }
                if (server != null) {
                    ServerPlayer sp = server.getPlayerList().getPlayer(entry.getKey());
                    if (sp != null && sp.isAlive() && sp.level() == root.level()) {
                        sp.setShiftKeyDown(false);
                        sp.setPose(net.minecraft.world.entity.Pose.STANDING);
                        sp.setDeltaMovement(Vec3.ZERO);
                        sp.fallDistance = 0.0F;
                        if (sp.position().distanceToSqr(root.pos()) > 0.04D) {
                            sp.teleportTo(root.pos().x, root.pos().y, root.pos().z);
                        }
                    } else {
                        BURST_ROOT_STATES.remove(entry.getKey());
                    }
                }
            }
        }
        if (!PENDING_BURST_ACTIONS.isEmpty()) {
            for (PendingBurstAction pending : PENDING_BURST_ACTIONS) {
                if (serverNow >= pending.executeTick()) {
                    PENDING_BURST_ACTIONS.remove(pending);
                    try {
                        pending.action().run();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
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
        VENTI_BURST_CD_UNTIL.remove(id);
        ZHONGLI_IFRAME_UNTIL.remove(id);
        ULTI_IFRAME_UNTIL.remove(id);
        FURINA_BURST_CD_UNTIL.remove(id);
        LAYLA_BURST_CD_UNTIL.remove(id);
        BURST_ROOT_STATES.remove(id);
        PENDING_BURST_ACTIONS.removeIf(p -> p.casterId().equals(id));
        ZHONGLI_PENDING_BEFALL.removeIf(p -> p.casterId().equals(id));
    }

    /** Clears only position-locked root/pending cast actions when a player changes dimensions, preserving cooldowns. */
    public static void clearDimensionBoundState(UUID id) {
        if (id == null) return;
        BURST_ROOT_STATES.remove(id);
        PENDING_BURST_ACTIONS.removeIf(p -> p.casterId().equals(id));
        ZHONGLI_PENDING_BEFALL.removeIf(p -> p.casterId().equals(id));
    }

    /** Clears all transient Archon Burst timers and pending impacts on server stop. */
    public static void clearAll() {
        ZHONGLI_BURST_CD_UNTIL.clear();
        VENTI_BURST_CD_UNTIL.clear();
        ZHONGLI_IFRAME_UNTIL.clear();
        ULTI_IFRAME_UNTIL.clear();
        FURINA_BURST_CD_UNTIL.clear();
        LAYLA_BURST_CD_UNTIL.clear();
        ZHONGLI_PENDING_BEFALL.clear();
        BURST_ROOT_STATES.clear();
        PENDING_BURST_ACTIONS.clear();
    }

    public static void resetAllArchonIdleTimers(@Nullable ServerPlayer player) {
        if (player == null) return;
        VentiManager.cancelIdle(player);
        ZhongliManager.cancelIdle(player);
        RaidenManager.cancelIdle(player);
        NahidaManager.cancelIdle(player);
        FurinaManager.cancelIdle(player);
        MavuikaManager.cancelIdle(player);
        TsaritsaManager.cancelIdle(player);
        DragonfireCloudManager.cancelIdle(player);
    }
}
