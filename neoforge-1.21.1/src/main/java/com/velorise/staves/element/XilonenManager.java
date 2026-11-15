package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Xilonen's Peak Patrol Song mechanics:
 * - Skill E (Yohual's Scratch): Roller Skate Nightsoul state (starts at 45/90 pts, ~9.0s Basic / 13.0s Netherite),
 *   +65% skating speed, release-and-repress straight-line Skate Dash, boosted jump height for flat-ground Plunge Attack
 *   with smooth X-axis Front Flip, vertical wall-climbing + Space wall-leap, and E -> N2 (2 melee kicks = +35 pts each -> 90/90)
 *   which depletes all 90 Nightsoul points at once, feeds +90 Fighting Spirit to Mavuika, and activates Source Samples (15s).
 * - Source Samples: -36% Elemental RES shred aura (8m) for sampled elements in the 4-staff inventory (persists off-field).
 * - Scroll of the Hero of Cinder City: triggering Crystallize grants +40% Elemental DMG for 18s to Geo + reacted element.
 * - Burst [R] (Ocelotlicue Point!): 1.6s I-frames + DJ turntable animation + 1.0 HP (0.5 heart) AoE Geo wave
 *   + 8 ticks of single-target active-player healing over 12s (when >= 2 PHEC samples) or 2 extra Geo shockwaves (< 2 PHEC).
 * - Netherite C1 & C2: -30% Nightsoul consumption, +0.5 Knockback Resistance, and C2 elemental buffs per sample.
 */
public final class XilonenManager {
    private static final ResourceLocation SKATE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "xilonen_skate_speed");
    private static final ResourceLocation SKATE_STEP_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "xilonen_skate_step_height");
    private static final ResourceLocation C1_KB_RES_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "xilonen_c1_kb_res");
    private static final ResourceLocation C2_HYDRO_HP_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "xilonen_c2_hydro_hp");

    public static final class XilonenPlayerState {
        public boolean skating = false;
        public boolean netherite = false;
        public float nightsoulPoints = 0.0F;
        public long skateUntilTick = 0L;
        public long lastDashTick = -100L;
        public int kickCombo = 0;
        public long lastKickTick = -100L;
        public boolean wallClimbing = false;
        public long sourceSamplesUntilTick = 0L;
        public boolean samplesNetherite = false;
        public final EnumSet<Element> activeSamples = EnumSet.of(Element.GEO);
        public int phecCount = 0;
        public long ultiCooldownUntilTick = 0L;
        public long iframeUntilTick = 0L;
        public long burstHealUntilTick = 0L;
        public long nextBurstHealTick = 0L;
        public boolean burstNetherite = false;
        public long extraGeoPulse1Tick = 0L;
        public long extraGeoPulse2Tick = 0L;
        public int lastSyncedCode = -1;
        public int lastSyncedSampleCode = -1;
    }

    public record SampleLoadout(EnumSet<Element> elements, int phecCount, boolean hasNetheriteXilonen) {}

    private static final Map<UUID, XilonenPlayerState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, EnumMap<Element, Long>> CRYSTALLIZE_SCROLL_BUFFS = new ConcurrentHashMap<>();

    private XilonenManager() {}

    public static void clearAll() {
        STATES.clear();
        CRYSTALLIZE_SCROLL_BUFFS.clear();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId == null) return;
        STATES.remove(playerId);
        CRYSTALLIZE_SCROLL_BUFFS.remove(playerId);
    }

    public static boolean isHoldingXilonen(Player player) {
        if (player == null) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG;
    }

    private static boolean isEffectiveNetheriteHeld(Player player) {
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
            return ArcaneStaffItem.getEffectiveNetherite(main);
        }
        ItemStack off = player.getOffhandItem();
        if (ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
            return ArcaneStaffItem.getEffectiveNetherite(off);
        }
        return false;
    }

    public static boolean isXilonenIframeActive(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        XilonenPlayerState st = STATES.get(player.getUUID());
        return st != null && player.level().getGameTime() < st.iframeUntilTick;
    }

    public static SampleLoadout scanInventorySamples(Player player) {
        EnumSet<Element> elems = EnumSet.of(Element.GEO);
        int phec = 0;
        boolean nethXilo = false;
        int countedStaves = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof ArcaneStaffItem staff)) continue;
            if (ArcaneStaffItem.getEffectiveKind(s) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                if (ArcaneStaffItem.getEffectiveNetherite(s)) nethXilo = true;
                continue;
            }
            if (countedStaves >= 3) continue;
            Element el = staff.kind() == ArcaneStaffItem.Kind.OMNI
                    ? ArcaneStaffItem.getOmniActiveElement(s)
                    : staff.getElement();
            if (el == null) continue;
            countedStaves++;
            if (el == Element.PYRO || el == Element.HYDRO || el == Element.ELECTRO || el == Element.CRYO) {
                elems.add(el);
                phec++;
            } else if (el == Element.GEO) {
                elems.add(Element.GEO);
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (s.isEmpty() || !(s.getItem() instanceof ArcaneStaffItem staff)) continue;
            if (ArcaneStaffItem.getEffectiveKind(s) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                if (ArcaneStaffItem.getEffectiveNetherite(s)) nethXilo = true;
                continue;
            }
            if (countedStaves >= 3) continue;
            Element el = staff.kind() == ArcaneStaffItem.Kind.OMNI
                    ? ArcaneStaffItem.getOmniActiveElement(s)
                    : staff.getElement();
            if (el == null) continue;
            countedStaves++;
            if (el == Element.PYRO || el == Element.HYDRO || el == Element.ELECTRO || el == Element.CRYO) {
                elems.add(el);
                phec++;
            }
        }
        return new SampleLoadout(elems, phec, nethXilo);
    }

    public static boolean isSkating(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return com.velorise.staves.client.ClientAuraTracker.isXilonenSkating(player.getId());
        }
        XilonenPlayerState st = STATES.get(player.getUUID());
        return st != null && st.skating && player.level().getGameTime() < st.skateUntilTick && st.nightsoulPoints > 0.01F;
    }

    public static boolean castYohualsScratch(ServerPlayer player, ItemStack stack, boolean isNetherite) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return false;
        long now = level.getGameTime();
        XilonenPlayerState st = STATES.computeIfAbsent(player.getUUID(), k -> new XilonenPlayerState());

        // If already skating: Sneak + Right-Click exits Roller Skate state cleanly; normal repress performs Skate Dash
        if (st.skating && now < st.skateUntilTick) {
            if (player.isShiftKeyDown()) {
                exitRollerSkateState(player, st, true);
                return true;
            }
            handleSkateDash(player);
            return false;
        }

        SampleLoadout loadout = scanInventorySamples(player);
        st.skating = true;
        st.netherite = isNetherite || loadout.hasNetheriteXilonen();
        st.nightsoulPoints = 45.0F; // Starts at 45.0 / 90.0 Nightsoul points (~9.0s skate duration)
        long maxDurationTicks = st.netherite ? 260L : 190L; // ~9.0s Basic, ~13.0s Netherite (C1)
        st.skateUntilTick = now + maxDurationTicks;
        st.lastDashTick = now;
        st.kickCombo = 0;
        st.wallClimbing = false;
        st.activeSamples.clear();
        st.activeSamples.addAll(loadout.elements());
        st.phecCount = loadout.phecCount();

        executeForwardSkateLunge(level, player, st, 1.25D, 1.0F);
        syncXilonenToClients(player, st, now, true);
        return true;
    }

    public static void handleSkateDash(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        XilonenPlayerState st = STATES.get(player.getUUID());
        if (st == null || !st.skating || now >= st.skateUntilTick || st.nightsoulPoints <= 0.01F) return;
        if (now - st.lastDashTick < 10L) return; // Require release & repress + 0.5s ICD (prevents hold-drain)
        st.lastDashTick = now;

        float dashCost = 4.0F * (st.netherite ? 0.70F : 1.0F);
        float consumed = Math.min(st.nightsoulPoints, dashCost);
        st.nightsoulPoints = Math.max(0.0F, st.nightsoulPoints - dashCost);
        ElementalGaugeManager.addMavuikaFightingSpiritFromExternalNightsoul(player, consumed);

        executeForwardSkateLunge(level, player, st, 1.32D, 0.85F);
        if (st.nightsoulPoints <= 0.01F) {
            exitRollerSkateState(player, st, true);
        } else {
            syncXilonenToClients(player, st, now, true);
        }
    }

    public static void handleSkateJumpBoost(ServerPlayer player) {
        if (player == null) return;
        XilonenPlayerState st = STATES.get(player.getUUID());
        long now = player.level().getGameTime();
        if (st == null || !st.skating || now >= st.skateUntilTick || st.nightsoulPoints <= 0.01F) return;
        if (st.wallClimbing) return;

        Vec3 dv = player.getDeltaMovement();
        player.setDeltaMovement(dv.x * 1.12D, Math.max(dv.y, 0.58D), dv.z * 1.12D);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_XILONEN_JUMP_FLIP);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 0.65F, 1.45F);
    }

    private static void executeForwardSkateLunge(ServerLevel level, ServerPlayer player, XilonenPlayerState st, double speed, float dmgScale) {
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() < 1.0E-4D) {
            float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
            horiz = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        } else {
            horiz = horiz.normalize();
        }
        Vec3 lungeVel = new Vec3(horiz.x * speed, Math.max(0.12D, player.getDeltaMovement().y), horiz.z * speed);
        player.setDeltaMovement(lungeVel);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.PEAK_PATROL_SONG, st.netherite) * dmgScale;
        float gaugeU = st.netherite ? 1.5F : 1.0F;
        AABB hitBox = player.getBoundingBox().expandTowards(horiz.scale(3.4D)).inflate(1.6D, 0.8D, 1.6D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.GEO, gaugeU, player, baseDmg);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    8, 0.25D, 0.25D, 0.25D, 0.05D);
        }

        level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.25D, player.getZ(),
                14, 0.35D, 0.15D, 0.35D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.95F, 1.35F);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.70F, 1.45F);

        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_XILONEN_SKATE_DASH);
    }

    public static void onMeleeHit(ServerPlayer player, LivingEntity target, ItemStack stack, boolean isNetherite) {
        if (player == null || target == null || !(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        XilonenPlayerState st = STATES.get(player.getUUID());
        if (st == null || !st.skating || now >= st.skateUntilTick) return;
        if (now - st.lastKickTick < 3L) return;
        st.lastKickTick = now;

        st.kickCombo = (st.kickCombo % 2) + 1;
        int anim = (st.kickCombo == 1)
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_XILONEN_KICK_1
                : com.velorise.staves.client.ClientAuraTracker.ANIM_XILONEN_KICK_2;
        ModNetwork.sendStaffAnimation(player, anim);

        float kickDmg = (isNetherite ? 5.25F : 3.5F) * (float) StavesConfig.getDamageMultiplier();
        float gaugeU = isNetherite ? 1.5F : 1.0F;
        target.invulnerableTime = 0;
        ElementalGaugeManager.applyElement(level, target, Element.GEO, gaugeU, player, kickDmg);
        level.sendParticles(ParticleTypes.WAX_ON, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                10, 0.28D, 0.28D, 0.28D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.85F, st.kickCombo == 1 ? 1.15F : 1.40F);

        // +35.0 Nightsoul points per normal attack kick -> 45 + 35 + 35 = 115 >= 90 after 2 kicks!
        st.nightsoulPoints = Math.min(90.0F, st.nightsoulPoints + 35.0F);
        if (st.nightsoulPoints >= 90.0F) {
            st.nightsoulPoints = 0.0F;
            ElementalGaugeManager.addMavuikaFightingSpiritFromExternalNightsoul(player, 90.0F);

            SampleLoadout loadout = scanInventorySamples(player);
            st.activeSamples.clear();
            st.activeSamples.addAll(loadout.elements());
            st.phecCount = loadout.phecCount();
            st.samplesNetherite = st.netherite || isNetherite || loadout.hasNetheriteXilonen();
            st.sourceSamplesUntilTick = now + 300L; // 15.0s (300 ticks) active even off-field

            exitRollerSkateState(player, st, true);

            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                    22, 0.55D, 0.45D, 0.55D, 0.08D);
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.5D, player.getZ(),
                    20, 0.65D, 0.25D, 0.65D, 0.05D);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.90F, 1.55F);
            player.displayClientMessage(
                    Component.translatable("message.velorise_staves.xilonen_samples_activated").withStyle(ChatFormatting.GOLD),
                    true
            );
            syncXilonenToClients(player, st, now, true);
        } else {
            syncXilonenToClients(player, st, now, true);
        }
    }

    public static void handleWallLeap(ServerPlayer player) {
        if (player == null) return;
        XilonenPlayerState st = STATES.get(player.getUUID());
        long now = player.level().getGameTime();
        if (st == null || !st.skating || now >= st.skateUntilTick || st.nightsoulPoints <= 0.0F) return;
        if (!isAgainstClimbableWall(player)) return;

        float cost = st.netherite ? 5.5F : 8.0F;
        float consumed = Math.min(st.nightsoulPoints, cost);
        st.nightsoulPoints = Math.max(0.0F, st.nightsoulPoints - cost);
        ElementalGaugeManager.addMavuikaFightingSpiritFromExternalNightsoul(player, consumed);

        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() > 1.0E-4D) horiz = horiz.normalize();
        player.setDeltaMovement(horiz.x * 0.48D, 0.72D, horiz.z * 0.48D);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 0.85F, 1.35F);
        player.serverLevel().sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.2D, player.getZ(),
                8, 0.25D, 0.15D, 0.25D, 0.03D);
        if (st.nightsoulPoints <= 0.01F) {
            exitRollerSkateState(player, st, true);
        } else {
            syncXilonenToClients(player, st, now, true);
        }
    }

    private static boolean isAgainstClimbableWall(Player player) {
        if (player.horizontalCollision) return true;
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() < 1.0E-4D) return false;
        horiz = horiz.normalize();
        AABB frontBox = player.getBoundingBox().move(horiz.x * 0.35D, 0.05D, horiz.z * 0.35D);
        return !player.level().noCollision(player, frontBox);
    }

    private static void exitRollerSkateState(ServerPlayer player, XilonenPlayerState st, boolean applyCooldown) {
        st.skating = false;
        st.wallClimbing = false;
        st.nightsoulPoints = 0.0F;
        st.skateUntilTick = 0L;
        if (applyCooldown) {
            int cdTicks = Math.max(20, (int) Math.round(StavesConfig.getStaffCooldownSeconds(ArcaneStaffItem.Kind.PEAK_PATROL_SONG) * 20.0D * StavesConfig.getCooldownMultiplier()));
            for (ItemStack s : player.getInventory().items) {
                if (!s.isEmpty() && s.getItem() instanceof ArcaneStaffItem staff) {
                    if (staff.kind() == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                        ElementalResonanceManager.applyCooldown(player, staff, cdTicks);
                    } else if (staff.kind() == ArcaneStaffItem.Kind.OMNI && ArcaneStaffItem.getEffectiveKind(s) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                        ElementalResonanceManager.applyOmniModeCooldown(player, s, cdTicks);
                    }
                }
            }
            for (ItemStack s : player.getInventory().offhand) {
                if (!s.isEmpty() && s.getItem() instanceof ArcaneStaffItem staff) {
                    if (staff.kind() == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                        ElementalResonanceManager.applyCooldown(player, staff, cdTicks);
                    } else if (staff.kind() == ArcaneStaffItem.Kind.OMNI && ArcaneStaffItem.getEffectiveKind(s) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG) {
                        ElementalResonanceManager.applyOmniModeCooldown(player, s, cdTicks);
                    }
                }
            }
        }
        syncXilonenToClients(player, st, player.level().getGameTime(), true);
    }

    public static boolean triggerXilonenUlti(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return false;
        if (!isHoldingXilonen(player)) return false;

        boolean isNetherite = isEffectiveNetheriteHeld(player);
        long now = level.getGameTime();
        XilonenPlayerState st = STATES.computeIfAbsent(player.getUUID(), k -> new XilonenPlayerState());
        if (now < st.ultiCooldownUntilTick) {
            double rem = (st.ultiCooldownUntilTick - now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "§6✦ Ocelotlicue Point! CD: %.1fs", rem)), true);
            return false;
        }

        SampleLoadout loadout = scanInventorySamples(player);
        boolean hasElectroC2 = (isNetherite || loadout.hasNetheriteXilonen()) && loadout.elements().contains(Element.ELECTRO);
        int baseCdTicks = hasElectroC2 ? 180 : 300; // 15.0s base CD, or 9.0s (-6s) with Netherite C2 Electro sample
        int adjustedCd = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? Math.max(20, (int) Math.round(baseCdTicks * 0.90D))
                : baseCdTicks;
        st.ultiCooldownUntilTick = now + adjustedCd;
        st.iframeUntilTick = now + 38L; // 1.8s (36 ticks) Genshin Burst I-frame & root
        player.invulnerableTime = Math.max(player.invulnerableTime, 38);
        ArchonUltiManager.applyBurstRoot(player, 36);

        if (hasElectroC2) {
            ElementalGaugeManager.addMavuikaFightingSpiritFromExternalNightsoul(player, 30.0F);
        }

        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_XILONEN_BURST);
        final boolean fNeth = isNetherite;
        final int fPhec = loadout.phecCount();
        final boolean fHasNethXilo = loadout.hasNetheriteXilonen();
        ArchonUltiManager.scheduleBurstAction(player, 34, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
            long eruptNow = sl.getGameTime();
            float burstDmg = 1.0F;
            float gaugeU = fNeth ? 1.5F : 1.0F;
            triggerBurstGeoSoundwave(sl, player, burstDmg, gaugeU);

            if (fPhec >= 2) {
                st.burstHealUntilTick = eruptNow + 240L;
                st.nextBurstHealTick = eruptNow + 30L;
                st.burstNetherite = fNeth || fHasNethXilo;
                float firstHeal = st.burstNetherite ? 4.0F : 3.0F;
                player.heal(firstHeal);
                sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(), 2, 0.25D, 0.25D, 0.25D, 0.02D);
            } else {
                st.extraGeoPulse1Tick = eruptNow + 15L;
                st.extraGeoPulse2Tick = eruptNow + 30L;
                st.burstNetherite = fNeth || fHasNethXilo;
            }
        });

        return true;
    }

    private static void triggerBurstGeoSoundwave(ServerLevel level, ServerPlayer player, float dmg, float gaugeU) {
        Vec3 pos = player.position();
        AABB box = player.getBoundingBox().inflate(5.0D, 2.5D, 5.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.GEO, gaugeU, player, dmg);
        }
        for (int i = 0; i < 24; i++) {
            double ang = (Math.PI * 2.0D * i) / 24.0D;
            double rx = pos.x + Math.cos(ang) * 3.2D;
            double rz = pos.z + Math.sin(ang) * 3.2D;
            level.sendParticles(ParticleTypes.WAX_ON, rx, pos.y + 0.15D, rz, 2, 0.1D, 0.05D, 0.1D, 0.02D);
            level.sendParticles(ParticleTypes.NOTE, rx, pos.y + 0.45D, rz, 1, 0.0D, 0.0D, 0.0D, (i % 8) / 8.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.15F);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45F, 1.65F);
    }

    public static void onCrystallizeTriggered(@Nullable ServerPlayer attacker, Element reactedAuraElement, long now) {
        if (attacker == null || reactedAuraElement == null) return;
        XilonenPlayerState st = STATES.get(attacker.getUUID());
        boolean holdingXilo = isHoldingXilonen(attacker);
        boolean activeXilo = holdingXilo || (st != null && (st.skating || now < st.sourceSamplesUntilTick));
        if (!activeXilo) return;

        long expire = now + 360L;
        if (attacker.level() instanceof ServerLevel level) {
            for (ServerPlayer ally : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(attacker) <= 144.0D)) {
                EnumMap<Element, Long> map = CRYSTALLIZE_SCROLL_BUFFS.computeIfAbsent(ally.getUUID(), k -> new EnumMap<>(Element.class));
                map.put(Element.GEO, expire);
                map.put(reactedAuraElement, expire);
            }
        } else {
            EnumMap<Element, Long> map = CRYSTALLIZE_SCROLL_BUFFS.computeIfAbsent(attacker.getUUID(), k -> new EnumMap<>(Element.class));
            map.put(Element.GEO, expire);
            map.put(reactedAuraElement, expire);
        }
    }

    public static float getXilonenDamageMultiplier(@Nullable ServerPlayer attacker, @Nullable Element attackElement, long now) {
        if (attacker == null || attackElement == null) return 1.0F;
        float mult = 1.0F;

        EnumMap<Element, Long> scroll = CRYSTALLIZE_SCROLL_BUFFS.get(attacker.getUUID());
        if (scroll != null) {
            Long until = scroll.get(attackElement);
            if (until != null && now < until) {
                mult += 0.40F;
            }
        }

        XilonenPlayerState st = STATES.get(attacker.getUUID());
        boolean c2Active = st != null && st.samplesNetherite && (now < st.sourceSamplesUntilTick || st.skating);
        if (!c2Active && attacker.level() instanceof ServerLevel level) {
            for (ServerPlayer ally : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(attacker) <= 64.0D)) {
                XilonenPlayerState allySt = STATES.get(ally.getUUID());
                if (allySt != null && allySt.samplesNetherite && (now < allySt.sourceSamplesUntilTick || allySt.skating)) {
                    st = allySt;
                    c2Active = true;
                    break;
                }
            }
        }
        if (c2Active && st != null && st.activeSamples.contains(attackElement)) {
            if (attackElement == Element.GEO) {
                mult += 0.50F;
            } else if (attackElement == Element.PYRO) {
                mult += 0.45F;
            } else if (attackElement == Element.CRYO) {
                mult += 0.45F;
            }
        }
        return mult;
    }

    public static float getXilonenResShred(@Nullable LivingEntity target, @Nullable Element element, long now) {
        if (target == null || element == null || !(target.level() instanceof ServerLevel level)) return 0.0F;
        for (ServerPlayer player : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(target) <= 64.0D)) {
            XilonenPlayerState st = STATES.get(player.getUUID());
            if (st == null) continue;
            boolean samplesActive = now < st.sourceSamplesUntilTick;
            boolean skatingActive = st.skating && now < st.skateUntilTick;
            if (!samplesActive && !skatingActive) continue;

            if (st.phecCount >= 2) {
                if (samplesActive && st.activeSamples.contains(element)) {
                    return 0.36F;
                }
            } else {
                if (element == Element.GEO && (skatingActive || samplesActive)) {
                    return 0.36F;
                }
            }
        }
        return 0.0F;
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            XilonenPlayerState st = STATES.get(player.getUUID());
            if (st == null) continue;
            ServerLevel level = player.serverLevel();

            // 1. Roller Skate Nightsoul state tick (~9.0s Basic / ~13.0s Netherite C1 from 45 points)
            if (st.skating) {
                boolean holdingXilo = isHoldingXilonen(player);
                if (!holdingXilo || now >= st.skateUntilTick || st.nightsoulPoints <= 0.01F) {
                    exitRollerSkateState(player, st, true);
                } else {
                    float c1DrainMult = st.netherite ? 0.70F : 1.0F; // C1: -30% Nightsoul consumption
                    Vec3 dv = player.getDeltaMovement();
                    double horizSpeed = Math.hypot(dv.x, dv.z);
                    boolean climbing = isAgainstClimbableWall(player) && (horizSpeed > 0.015D || player.zza > 0.0F || !player.onGround());
                    st.wallClimbing = climbing;
                    float drain;
                    if (climbing) {
                        drain = 0.28F * c1DrainMult;
                        player.setDeltaMovement(dv.x * 0.88D, 0.35D, dv.z * 0.88D);
                        player.fallDistance = 0.0F;
                        player.hasImpulse = true;
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                        if (now % 3L == 0L) {
                            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.2D, player.getZ(),
                                    3, 0.2D, 0.1D, 0.2D, 0.02D);
                            level.playSound(null, player.blockPosition(), SoundEvents.BASALT_STEP, SoundSource.PLAYERS, 0.45F, 1.35F);
                        }
                    } else {
                        // 45.0 pts / 0.24 pts/tick = 187.5 ticks (~9.3s) continuous skating (or ~13.3s on Netherite C1)
                        drain = (horizSpeed > 0.02D ? 0.24F : 0.12F) * c1DrainMult;
                        if (now % 4L == 0L && horizSpeed > 0.05D) {
                            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 0.1D, player.getZ(),
                                    2, 0.18D, 0.05D, 0.18D, 0.02D);
                        }
                    }
                    float actualDrained = Math.min(st.nightsoulPoints, drain);
                    st.nightsoulPoints = Math.max(0.0F, st.nightsoulPoints - drain);
                    ElementalGaugeManager.addMavuikaFightingSpiritFromExternalNightsoul(player, actualDrained);
                    if (st.nightsoulPoints <= 0.01F) {
                        exitRollerSkateState(player, st, true);
                    }
                }
            } else {
                st.wallClimbing = false;
            }

            // 2. Burst periodic single-target self-healing (active player ONLY)
            if (now < st.burstHealUntilTick && now >= st.nextBurstHealTick) {
                st.nextBurstHealTick = now + 30L;
                float healAmt = st.burstNetherite ? 4.0F : 3.0F;
                player.heal(healAmt);
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.15D, player.getZ(),
                        2, 0.25D, 0.25D, 0.25D, 0.02D);
                level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 0.3D, player.getZ(),
                        3, 0.4D, 0.1D, 0.4D, 0.3D);
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.65F, 1.35F);
            }

            // 3. Burst extra Geo pulses when < 2 PHEC samples
            if (st.extraGeoPulse1Tick > 0L && now >= st.extraGeoPulse1Tick) {
                st.extraGeoPulse1Tick = 0L;
                triggerBurstGeoSoundwave(level, player, 1.0F, st.burstNetherite ? 1.5F : 1.0F);
            }
            if (st.extraGeoPulse2Tick > 0L && now >= st.extraGeoPulse2Tick) {
                st.extraGeoPulse2Tick = 0L;
                triggerBurstGeoSoundwave(level, player, 1.0F, st.burstNetherite ? 1.5F : 1.0F);
            }

            // 4. Attribute modifiers (Skate Speed +65%, C1 Knockback Resistance +0.5, C2 Hydro +45% Max HP)
            updateAttributes(player, st, now);

            // 5. Sync state to client
            syncXilonenToClients(player, st, now, false);
        }
    }

    private static void updateAttributes(ServerPlayer player, XilonenPlayerState st, long now) {
        AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            boolean wantSkate = st.skating && now < st.skateUntilTick;
            AttributeModifier existing = spd.getModifier(SKATE_SPEED_ID);
            if (wantSkate) {
                if (existing == null || Math.abs(existing.amount() - 0.65D) > 0.001D) {
                    if (existing != null) spd.removeModifier(SKATE_SPEED_ID);
                    spd.addTransientModifier(new AttributeModifier(SKATE_SPEED_ID, 0.65D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else if (existing != null) {
                spd.removeModifier(SKATE_SPEED_ID);
            }
        }

        AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            boolean wantSkateStep = st.skating && now < st.skateUntilTick;
            AttributeModifier existingStep = stepAttr.getModifier(SKATE_STEP_ID);
            if (wantSkateStep) {
                if (existingStep == null || Math.abs(existingStep.amount() - 0.65D) > 0.001D) {
                    if (existingStep != null) stepAttr.removeModifier(SKATE_STEP_ID);
                    stepAttr.addTransientModifier(new AttributeModifier(SKATE_STEP_ID, 0.65D, AttributeModifier.Operation.ADD_VALUE));
                }
            } else if (existingStep != null) {
                stepAttr.removeModifier(SKATE_STEP_ID);
            }
        }

        boolean samplesActive = now < st.sourceSamplesUntilTick;
        AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            boolean wantKb = (samplesActive || st.skating) && st.samplesNetherite;
            boolean hasKb = kb.getModifier(C1_KB_RES_ID) != null;
            if (wantKb && !hasKb) {
                kb.addTransientModifier(new AttributeModifier(C1_KB_RES_ID, 0.50D, AttributeModifier.Operation.ADD_VALUE));
            } else if (!wantKb && hasKb) {
                kb.removeModifier(C1_KB_RES_ID);
            }
        }

        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            boolean wantHydroC2 = samplesActive && st.samplesNetherite && st.activeSamples.contains(Element.HYDRO);
            boolean hasHydroC2 = hp.getModifier(C2_HYDRO_HP_ID) != null;
            if (wantHydroC2 && !hasHydroC2) {
                hp.addTransientModifier(new AttributeModifier(C2_HYDRO_HP_ID, 0.45D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!wantHydroC2 && hasHydroC2) {
                hp.removeModifier(C2_HYDRO_HP_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    private static void syncXilonenToClients(ServerPlayer player, XilonenPlayerState st, long now, boolean force) {
        int ptsCode;
        if (st.skating && now < st.skateUntilTick && st.nightsoulPoints > 0.01F) {
            int pts = Mth.clamp(Math.round(st.nightsoulPoints), 1, 90);
            ptsCode = 1000 + pts + (st.wallClimbing ? 100 : 0) + (st.netherite ? 200 : 0);
        } else {
            ptsCode = 1999;
        }
        if (force || ptsCode != st.lastSyncedCode) {
            st.lastSyncedCode = ptsCode;
            ModNetwork.sendStaffAnimation(player, ptsCode);
        }

        int sampleCode;
        if (now < st.sourceSamplesUntilTick) {
            int mask = 0;
            if (st.activeSamples.contains(Element.GEO)) mask |= 1;
            if (st.activeSamples.contains(Element.PYRO)) mask |= 2;
            if (st.activeSamples.contains(Element.HYDRO)) mask |= 4;
            if (st.activeSamples.contains(Element.ELECTRO)) mask |= 8;
            if (st.activeSamples.contains(Element.CRYO)) mask |= 16;
            if (st.samplesNetherite) mask |= 32;
            sampleCode = 2000 + mask;
        } else {
            sampleCode = 2000;
        }
        if (force || sampleCode != st.lastSyncedSampleCode) {
            st.lastSyncedSampleCode = sampleCode;
            ModNetwork.sendStaffAnimation(player, sampleCode);
        }
    }
}
