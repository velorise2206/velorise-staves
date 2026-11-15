package com.velorise.staves.element;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages Cryo Archon Tsaritsa's weapon mechanics (Tsaritsa's Bow & Spear),
 * including Stellar Reactions (Stellar-Conduct & Polestar Field, Stellar Swirl Lv.2),
 * Bow NA & 3-stage CA (Frostbite Arrow + 3 converging Cryo arrows on hit),
 * Tap E (4-sided Azure Star Crown overhead firing Cryo Star projectiles off-field + +16% CRIT DMG),
 * Hold E (Spear Stance transformation for 12.0s with Cryo melee infusion + +48% CRIT DMG),
 * and Dual Elemental Burst [R] (Support Burst with Starry Snow Dome & Unsealing Burst vs
 * DPS Burst with Trident-Throw Colossal Ice Spear, 10s Staff Seal & 3x Stellar Punishment).
 */
public final class TsaritsaManager {

    public static final int AZURE_CROWN_DURATION_TICKS = 400; // 20.0s
    public static final int AZURE_CROWN_FIRE_INTERVAL_TICKS = 40; // 2.0s
    public static final int SPEAR_STANCE_DURATION_TICKS = 240; // 12.0s
    public static final int HOLD_E_CHARGE_TICKS = 16; // 0.8s hold to transform into Spear Stance
    public static final int BOW_FULL_CHARGE_TICKS = 18; // 3 stages: 0..5 (_0), 6..17 (_1), 18+ (_2)

    public static final int BURST_IFRAME_TICKS = 32; // 1.6s I-frame
    public static final int BURST_COOLDOWN_TICKS = 300; // 15.0s CD
    public static final int SUPPORT_BURST_DURATION_TICKS = 240; // 12.0s
    public static final int DPS_BURST_SEAL_TICKS = 200; // 10.0s
    public static final int POLESTAR_FIELD_DURATION_TICKS = 200; // 10.0s
    public static final double POLESTAR_FIELD_RADIUS = 6.0D;

    public static final class TsaritsaPlayerState {
        public long azureCrownUntilTick = 0L;
        public long nextCrownStarTick = 0L;
        public long spearStanceUntilTick = 0L;
        public int spearComboStep = 0;
        public long lastSpearSlashTick = 0L;
        public long lastBowNaTick = -100L;

        public long burstIframeUntilTick = 0L;
        public long burstCooldownUntilTick = 0L;

        // Support Burst & Seal state
        public long supportBurstUntilTick = 0L;
        public Vec3 supportDomeCenter = Vec3.ZERO;
        public long supportSealUntilTick = 0L;
        public boolean supportUnsealReady = false;
        public boolean wasHoldingTsaritsaLastTick = false;

        // DPS Burst & Stellar Punishment state
        public long dpsSealUntilTick = 0L;
        public Vec3 dpsIceFieldCenter = Vec3.ZERO;
        public int punishmentCharges = 0;
        public long lastPunishmentTick = 0L;

        public boolean netherite = false;
        public int lastSyncedMask = -1;
    }

    public static final class PolestarFieldState {
        public final UUID ownerId;
        public final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        public Vec3 center;
        public long expireTick;
        public int stacks; // 1..4 Polestar stacks (+10% Crit Rate & +14% Base DMG per stack)
        public boolean netheriteOwner;

        public PolestarFieldState(UUID ownerId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 center, long expireTick, boolean netheriteOwner) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.center = center;
            this.expireTick = expireTick;
            this.stacks = 1;
            this.netheriteOwner = netheriteOwner;
        }
    }

    private static final Map<UUID, TsaritsaPlayerState> PLAYER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, PolestarFieldState> POLESTAR_FIELDS = new ConcurrentHashMap<>();

    private record PendingArrowStrike(
            long triggerTick,
            UUID playerId,
            UUID targetId,
            Vec3 origin,
            Vec3 impactPos,
            float damage,
            boolean isNetherite
    ) {}

    private static final List<PendingArrowStrike> PENDING_ARROWS = new ArrayList<>();

    private TsaritsaManager() {}

    public static TsaritsaPlayerState getOrCreateState(Player player) {
        return PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new TsaritsaPlayerState());
    }

    public static boolean isHoldingTsaritsaWeapon(Player player) {
        if (player == null) return false;
        return isTsaritsaItem(player.getMainHandItem()) || isTsaritsaItem(player.getOffhandItem());
    }

    public static boolean isTsaritsaItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ArcaneStaffItem.Kind eff = ArcaneStaffItem.getEffectiveKind(stack);
        return eff == ArcaneStaffItem.Kind.TSARITSA_BOW || eff == ArcaneStaffItem.Kind.TSARITSA_SPEAR;
    }

    public static boolean isHoldingNetheriteTsaritsa(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (isTsaritsaItem(main)) {
            return ArcaneStaffItem.getEffectiveNetherite(main);
        }
        ItemStack off = player.getOffhandItem();
        if (isTsaritsaItem(off)) {
            return ArcaneStaffItem.getEffectiveNetherite(off);
        }
        return false;
    }

    /**
     * Checks whether the player has Tsaritsa's weapon in their active staff loadout (up to 4 staves).
     */
    public static boolean hasTsaritsaInParty(Player player) {
        if (player == null) return false;
        if (ElementalResonanceManager.isOverloaded(player)) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArcaneStaffItem staff) {
                if (staff.getKind() == ArcaneStaffItem.Kind.TSARITSA_BOW
                        || staff.getKind() == ArcaneStaffItem.Kind.TSARITSA_SPEAR
                        || staff.getKind() == ArcaneStaffItem.Kind.OMNI) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasNetheriteTsaritsaInParty(Player player) {
        if (player == null) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArcaneStaffItem staff && ArcaneStaffItem.getEffectiveNetherite(stack)) {
                if (staff.getKind() == ArcaneStaffItem.Kind.TSARITSA_BOW
                        || staff.getKind() == ArcaneStaffItem.Kind.TSARITSA_SPEAR
                        || staff.getKind() == ArcaneStaffItem.Kind.OMNI) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSpearStanceActive(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
            return true;
        }
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.spearStanceUntilTick;
    }

    public static boolean isAzureCrownActive(Player player) {
        if (player == null) return false;
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.azureCrownUntilTick;
    }

    public static boolean isTsaritsaIframeActive(Player player) {
        if (player == null) return false;
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.burstIframeUntilTick;
    }

    public static boolean isOtherStavesSealed(Player player) {
        if (player == null) return false;
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.dpsSealUntilTick;
    }

    /**
     * Returns +1 EM level for non-Cryo staves when Netherite Tsaritsa's Azure Star Crown is active (or preserved during Seal).
     */
    public static int getBonusEmLevels(Player player, Element attackElement) {
        if (player == null) return 0;
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        if (state == null || !state.netherite) return 0;
        long now = player.level().getGameTime();
        boolean activeOrSealed = now < state.azureCrownUntilTick || now < state.supportSealUntilTick;
        if (!activeOrSealed) return 0;
        if (attackElement != null && attackElement != Element.CRYO) {
            return 1;
        }
        return 0;
    }

    /**
     * Computes Tsaritsa's CRIT Rate, CRIT DMG, and Base DMG multiplier for an outgoing elemental/staff hit.
     */
    public static float applyStellarAndCrownMultipliers(Player attacker, LivingEntity target, Element element, float baseDamage) {
        if (attacker == null || baseDamage <= 0.0F) return baseDamage;
        long now = attacker.level().getGameTime();
        TsaritsaPlayerState state = PLAYER_STATES.get(attacker.getUUID());

        float damageMultiplier = 1.0F;
        float critRate = 0.05F; // 5% base crit chance
        float critDmgBonus = 0.50F; // +50% base crit damage

        // Check Polestar Field stacks at target position
        if (target != null) {
            PolestarFieldState field = getActivePolestarFieldAt(target.level(), target.position());
            if (field != null) {
                int stacks = Math.max(1, Math.min(4, field.stacks));
                critRate += stacks * 0.10F; // +10% Crit Rate per stack (up to +40%)
                damageMultiplier += stacks * 0.14F; // +14% Base DMG per stack (up to +56%)
                if (element == Element.CRYO || element == Element.ELECTRO) {
                    if (field.stacks < 4) {
                        field.stacks++;
                    }
                }
            }
        }

        if (state != null) {
            boolean holdingTsaritsa = isHoldingTsaritsaWeapon(attacker);
            boolean spearActive = now < state.spearStanceUntilTick
                    || ArcaneStaffItem.getEffectiveKind(attacker.getMainHandItem()) == ArcaneStaffItem.Kind.TSARITSA_SPEAR;
            boolean crownActive = now < state.azureCrownUntilTick;
            boolean sealPreserved = state.netherite && now < state.supportSealUntilTick;

            if (spearActive && holdingTsaritsa) {
                // Glorious Blessing (On-field Spear DPS): +48% CRIT DMG for herself
                critRate += 0.25F;
                critDmgBonus += 0.48F;
            } else if ((crownActive || sealPreserved) && !holdingTsaritsa) {
                // Benevolent Blessing (Off-field Sub-DPS): +16% CRIT DMG for other staves
                critRate += 0.15F;
                critDmgBonus += 0.16F;
            }

            // Netherite C1: +15% DMG to Cryo staves while Crown or Seal is active
            if (state.netherite && (crownActive || sealPreserved || spearActive) && element == Element.CRYO) {
                damageMultiplier += 0.15F;
            }
        }

        float result = baseDamage * damageMultiplier;
        if (ThreadLocalRandom.current().nextFloat() < Math.min(0.95F, critRate)) {
            result *= (1.0F + critDmgBonus);
            if (target != null && target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                        6, 0.25D, 0.25D, 0.25D, 0.08D);
            }
        }
        return result;
    }

    /**
     * Returns the Stellar Reaction damage multiplier (+45% or +50% during Support Burst).
     */
    public static float getStellarReactionMultiplier(Player attacker) {
        if (attacker == null) return 1.0F;
        TsaritsaPlayerState state = PLAYER_STATES.get(attacker.getUUID());
        if (state == null) return 1.0F;
        long now = attacker.level().getGameTime();
        if (now < state.supportBurstUntilTick) {
            return state.netherite ? 1.50F : 1.45F;
        }
        return 1.0F;
    }

    /**
     * Returns Physical Resistance shred multiplier (1.40x physical damage inside Polestar Field).
     */
    public static float getPolestarPhysicalMultiplier(LivingEntity target) {
        if (target == null) return 1.0F;
        PolestarFieldState field = getActivePolestarFieldAt(target.level(), target.position());
        return field != null ? 1.40F : 1.0F;
    }

    public static PolestarFieldState getActivePolestarFieldAt(net.minecraft.world.level.Level level, Vec3 pos) {
        if (level == null || pos == null || POLESTAR_FIELDS.isEmpty()) return null;
        long now = level.getGameTime();
        for (PolestarFieldState field : POLESTAR_FIELDS.values()) {
            if (field.dimension.equals(level.dimension()) && now < field.expireTick) {
                if (field.center.distanceToSqr(pos) <= (POLESTAR_FIELD_RADIUS * POLESTAR_FIELD_RADIUS)) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Triggered when Stellar-Conduct (Cryo + Electro) occurs with Tsaritsa in the party.
     */
    public static void onStellarConductTriggered(ServerLevel level, LivingEntity target, Player attacker, float reactionDamage) {
        Vec3 center = target.position();
        long now = level.getGameTime();
        UUID ownerId = attacker != null ? attacker.getUUID() : target.getUUID();
        boolean netherite = attacker != null && hasNetheriteTsaritsaInParty(attacker);

        PolestarFieldState field = POLESTAR_FIELDS.get(ownerId);
        if (field != null && field.dimension.equals(level.dimension()) && now < field.expireTick
                && field.center.distanceToSqr(center) <= (POLESTAR_FIELD_RADIUS * POLESTAR_FIELD_RADIUS)) {
            field.expireTick = now + POLESTAR_FIELD_DURATION_TICKS;
            field.stacks = Math.min(4, field.stacks + 1);
            field.center = center;
        } else {
            field = new PolestarFieldState(ownerId, level.dimension(), center, now + POLESTAR_FIELD_DURATION_TICKS, netherite);
            POLESTAR_FIELDS.put(ownerId, field);
        }

        // Visual Polestar Field ring & starry burst
        ModNetwork.sendTsaritsaPolarField(level, center, POLESTAR_FIELD_RADIUS, 40, 3);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.5D, center.z, 24, 2.2D, 0.4D, 2.2D, 0.04D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.6D, center.z, 20, 2.0D, 0.5D, 2.0D, 0.06D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8D, center.z, 14, 1.8D, 0.4D, 1.8D, 0.03D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.35F);

        if (attacker != null) {
            onStellarReactionShortenSeal(attacker);
        }
    }

    /**
     * Triggered when Stellar Swirl (Cryo + Anemo) occurs with Tsaritsa in the party.
     */
    public static void onStellarSwirlTriggered(ServerLevel level, LivingEntity primaryTarget, Player attacker, float swirlDamage) {
        Vec3 center = primaryTarget.position().add(0.0D, primaryTarget.getBbHeight() * 0.5D, 0.0D);
        ModNetwork.sendVortexSphere(level, center, 5.5D, 35, 0xFF66E6FF);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 28, 2.4D, 0.8D, 2.4D, 0.05D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 16, 2.0D, 0.6D, 2.0D, 0.04D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1.0F, 1.25F);

        // Lv.2 Stellar Vortex: gently pull nearby enemies inward & spread Stellar Cryo aura
        AABB box = new AABB(center.x - 6.0D, center.y - 3.0D, center.z - 6.0D,
                center.x + 6.0D, center.y + 3.0D, center.z + 6.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && isValidCombatTarget(attacker, e));
        for (LivingEntity e : targets) {
            Vec3 pull = center.subtract(e.position());
            double dist = Math.max(0.5D, pull.length());
            Vec3 pullVel = pull.normalize().scale(Math.min(0.42D, 1.2D / dist));
            e.setDeltaMovement(e.getDeltaMovement().scale(0.4D).add(pullVel.x, 0.08D, pullVel.z));
            e.hurtMarked = true;
            if (e != primaryTarget) {
                ElementalGaugeManager.applyElement(e, Element.CRYO, attacker, swirlDamage * 0.65F, 1.0F);
            }
        }

        if (attacker != null) {
            onStellarReactionShortenSeal(attacker);
        }
    }

    private static void onStellarReactionShortenSeal(Player player) {
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) return;
        long now = player.level().getGameTime();
        if (now < state.supportSealUntilTick) {
            state.supportSealUntilTick = Math.max(now, state.supportSealUntilTick - 40L); // Shorten Seal by 2.0s
            state.supportUnsealReady = true;
        }
    }

    /**
     * Left-Click Normal Attack:
     * - In Spear Stance: 3-hit melee Cryo spear combo with starlight-blue slash trails.
     * - In Bow Stance: quick stellar Cryo arrow shot.
     */
    public static void performNormalAttack(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        if (!isHoldingTsaritsaWeapon(player)) return;
        if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) return;

        // If another staff is clicked during DPS Seal, trigger Stellar Punishment
        TsaritsaPlayerState state = getOrCreateState(player);
        state.netherite = isHoldingNetheriteTsaritsa(player);
        long now = level.getGameTime();

        if (isSpearStanceActive(player)) {
            performSpearNormalAttack(player, level, state, now);
        } else {
            performBowNormalAttack(player, level, state, now);
        }
    }

    private static void performBowNormalAttack(ServerPlayer player, ServerLevel level, TsaritsaPlayerState state, long now) {
        if (now - state.lastBowNaTick < 10L) return;
        state.lastBowNaTick = now;

        float baseDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW);
        if (state.netherite) baseDmg *= 1.15F;

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_BOW_NA);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.25F);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double range = StavesConfig.getRange(ArcaneStaffItem.Kind.TSARITSA_BOW);

        LivingEntity hitTarget = raycastTarget(level, player, eye, look, range);
        Vec3 endPos = hitTarget != null
                ? hitTarget.position().add(0.0D, hitTarget.getBbHeight() * 0.55D, 0.0D)
                : eye.add(look.scale(range));

        // Dedicated 3D Cryo Arrow projectile & frost particles
        ModNetwork.sendCryoArrow(level, eye.add(look.scale(0.6D)), endPos, 8, 1);
        int steps = Math.max(4, (int) (eye.distanceTo(endPos) * 2.0D));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = eye.lerp(endPos, t);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        }

        if (hitTarget != null) {
            float finalDmg = applyStellarAndCrownMultipliers(player, hitTarget, Element.CRYO, baseDmg);
            hitTarget.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(hitTarget, Element.CRYO, player, finalDmg, 1.0F);
        }
    }

    private static void performSpearNormalAttack(ServerPlayer player, ServerLevel level, TsaritsaPlayerState state, long now) {
        if (now - state.lastSpearSlashTick < 8L) return;
        if (now - state.lastSpearSlashTick > 28L) {
            state.spearComboStep = 0;
        }
        int step = state.spearComboStep;
        state.spearComboStep = (state.spearComboStep + 1) % 3;
        state.lastSpearSlashTick = now;

        int anim = switch (step) {
            case 0 -> ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1;
            case 1 -> ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_2;
            default -> ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3;
        };
        ModNetwork.sendStaffAnimation(player, anim);

        float baseDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * (step == 2 ? 1.45F : 1.15F);
        if (state.netherite) baseDmg *= 1.15F;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();

        // Starlight-blue spear slash arc in front of player
        Vec3 arcStart = eye.add(look.scale(1.6D)).add(right.scale(step == 1 ? -1.4D : 1.4D)).add(0.0D, -0.25D, 0.0D);
        Vec3 arcEnd = eye.add(look.scale(2.8D)).add(right.scale(step == 1 ? 1.4D : -1.4D)).add(0.0D, -0.35D, 0.0D);
        ModNetwork.sendElectricArc(level, arcStart, arcEnd, 8, 0xFF44CCFF);

        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0D;
            Vec3 p = arcStart.lerp(arcEnd, t);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.85F, 1.35F + step * 0.1F);

        Vec3 center = player.position().add(look.scale(2.2D)).add(0.0D, 0.8D, 0.0D);
        AABB box = new AABB(center.x - 2.6D, center.y - 1.6D, center.z - 2.6D,
                center.x + 2.6D, center.y + 1.8D, center.z + 2.6D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            float finalDmg = applyStellarAndCrownMultipliers(player, target, Element.CRYO, baseDmg);
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(target, Element.CRYO, player, finalDmg, step == 2 ? 1.5F : 1.0F);
        }
    }

    /**
     * Charged Attack (Hold Left-Click in Bow Stance, released after 3-stage bow pull):
     * Fires Frostbite Arrow (2U Cryo); on hitting a mob, spawns 3 additional Cryo arrows
     * from 3 directions around the target converging inward + Cryo snowflake AoE.
     * Locked while in Spear Stance.
     */
    public static void performChargedBowShot(ServerPlayer player, boolean fullCharge) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        if (!isHoldingTsaritsaWeapon(player)) return;
        if (isSpearStanceActive(player)) {
            // Charged Attack is locked in Spear Stance; fallback to Spear NA
            performNormalAttack(player);
            return;
        }
        if (!fullCharge) {
            performNormalAttack(player);
            return;
        }

        TsaritsaPlayerState state = getOrCreateState(player);
        state.netherite = isHoldingNetheriteTsaritsa(player);

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_CA_SHOT);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.1F, 1.4F);

        float baseDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * 1.75F;
        if (state.netherite) baseDmg *= 1.15F;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double range = StavesConfig.getRange(ArcaneStaffItem.Kind.TSARITSA_BOW);

        LivingEntity hitTarget = raycastTarget(level, player, eye, look, range);
        Vec3 impactPos;
        if (hitTarget != null) {
            impactPos = hitTarget.position().add(0.0D, hitTarget.getBbHeight() * 0.55D, 0.0D);
        } else {
            BlockHitResult bhr = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            impactPos = bhr.getType() != HitResult.Type.MISS ? bhr.getLocation() : eye.add(look.scale(range));
        }

        // Primary Frostbite 3D Cryo Arrow
        ModNetwork.sendCryoArrow(level, eye.add(look.scale(0.6D)), impactPos, 12, 2);

        if (hitTarget != null) {
            float primaryDmg = applyStellarAndCrownMultipliers(player, hitTarget, Element.CRYO, baseDmg);
            hitTarget.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(hitTarget, Element.CRYO, player, primaryDmg, 2.0F); // 2U Cryo

            // Schedule 3 additional 3D Stellar Cryo Arrows from 3 directions (120 degrees apart)
            // in 3 distinct staggered waves (+3, +6, +9 ticks) to bypass Minecraft iframes!
            double baseAngle = player.getYRot() * (Math.PI / 180.0D);
            float arrowSubDmg = baseDmg * 0.55F;
            for (int i = 0; i < 3; i++) {
                double angle = baseAngle + (i * 2.0D * Math.PI / 3.0D);
                Vec3 origin = impactPos.add(Math.cos(angle) * 4.2D, 2.4D, Math.sin(angle) * 4.2D);
                PENDING_ARROWS.add(new PendingArrowStrike(
                        level.getGameTime() + 3L + (i * 3L),
                        player.getUUID(),
                        hitTarget.getUUID(),
                        origin,
                        impactPos,
                        arrowSubDmg,
                        state.netherite
                ));
            }

            // Initial visual burst at impact
            level.sendParticles(ParticleTypes.SNOWFLAKE, impactPos.x, impactPos.y, impactPos.z, 24, 1.2D, 0.6D, 1.2D, 0.05D);
            level.sendParticles(ParticleTypes.FIREWORK, impactPos.x, impactPos.y, impactPos.z, 12, 0.8D, 0.4D, 0.8D, 0.04D);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.45F);
        }
    }

    /**
     * Tap E (Quick Right-Click — Benevolent Blessing / Off-field Sub-DPS):
     * Summons the 4-sided Azure Star Crown floating above the player's head for 20.0s.
     * Fires Cryo Star Projectiles every 2.0s even when switched to another staff, and grants +16% CRIT DMG to other staves.
     */
    public static void performTapE(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        TsaritsaPlayerState state = getOrCreateState(player);
        state.netherite = isHoldingNetheriteTsaritsa(player);
        long now = level.getGameTime();

        // Exit Spear Stance if active and summon Azure Star Crown
        state.spearStanceUntilTick = 0L;
        state.azureCrownUntilTick = now + AZURE_CROWN_DURATION_TICKS;
        state.nextCrownStarTick = now + 10L;

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_TAP_E);
        syncStateToClients(player, state, now);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.55F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 2.1D, player.getZ(),
                18, 0.5D, 0.25D, 0.5D, 0.03D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 2.2D, player.getZ(),
                12, 0.4D, 0.2D, 0.4D, 0.02D);

        player.displayClientMessage(
                Component.translatable("message.velorise_staves.tsaritsa_tap_e").withStyle(ChatFormatting.AQUA),
                true
        );
    }

    /**
     * Hold E (Hold Right-Click — Glorious Blessing / On-field Spear DPS, 12.0s):
     * Summons the Azure Star Crown and immediately absorbs it to transform the weapon into Spear Stance (tsaritsa_spear)
     * for 12.0s, infusing Normal Attacks with Cryo and granting +48% CRIT DMG to herself.
     */
    public static void performHoldE(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        TsaritsaPlayerState state = getOrCreateState(player);
        state.netherite = isHoldingNetheriteTsaritsa(player);
        long now = level.getGameTime();

        state.azureCrownUntilTick = now + SPEAR_STANCE_DURATION_TICKS;
        state.spearStanceUntilTick = now + SPEAR_STANCE_DURATION_TICKS;
        state.spearComboStep = 0;

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_HOLD_E);
        syncStateToClients(player, state, now);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.9F, 1.4F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.2D, player.getZ(),
                30, 0.8D, 0.6D, 0.8D, 0.05D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.4D, player.getZ(),
                18, 0.6D, 0.5D, 0.6D, 0.04D);

        player.displayClientMessage(
                Component.translatable("message.velorise_staves.tsaritsa_hold_e").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                true
        );
    }

    /**
     * Elemental Burst [R] (I-frame 1.6s, CD 15.0s):
     * - Variant 1 (Support Burst): When Benevolent Blessing (Crown without Spear Stance) is active.
     * - Variant 2 (DPS Burst): When Glorious Blessing (Spear Stance) is active.
     */
    public static boolean triggerTsaritsaUlti(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return false;
        if (!isHoldingTsaritsaWeapon(player)) return false;

        TsaritsaPlayerState state = getOrCreateState(player);
        state.netherite = isHoldingNetheriteTsaritsa(player);
        long now = level.getGameTime();

        if (now < state.burstCooldownUntilTick) {
            int remSec = (int) Math.ceil((state.burstCooldownUntilTick - now) / 20.0D);
            player.displayClientMessage(
                    Component.translatable("message.velorise_staves.tsaritsa_burst_cd", remSec).withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        state.burstCooldownUntilTick = now + BURST_COOLDOWN_TICKS;
        state.burstIframeUntilTick = now + BURST_IFRAME_TICKS;
        player.invulnerableTime = Math.max(player.invulnerableTime, BURST_IFRAME_TICKS);
        ArchonUltiManager.grantUltiIframe(player, BURST_IFRAME_TICKS);

        boolean spearMode = isSpearStanceActive(player);
        if (!spearMode) {
            // Variant 1: Support Burst (Benevolent Blessing — Flat Ice Field + Central Polar Pillar)
            state.azureCrownUntilTick = Math.max(state.azureCrownUntilTick, now + SUPPORT_BURST_DURATION_TICKS);
            state.supportBurstUntilTick = now + SUPPORT_BURST_DURATION_TICKS;
            state.supportDomeCenter = player.position();
            state.supportSealUntilTick = now + 160L; // 8.0s Seal, shortened by Stellar Reactions
            state.supportUnsealReady = true;

            ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_BURST_SUPPORT);
            ModNetwork.sendTsaritsaPolarField(level, state.supportDomeCenter, 10.0D, SUPPORT_BURST_DURATION_TICKS, 1);
            syncStateToClients(player, state, now);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.3F, 1.25F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 2.0D, player.getZ(),
                    65, 4.5D, 2.0D, 4.5D, 0.05D);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 2.5D, player.getZ(),
                    40, 4.0D, 2.0D, 4.0D, 0.04D);

            player.displayClientMessage(
                    Component.translatable("message.velorise_staves.tsaritsa_burst_support").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    true
            );
        } else {
            // Variant 2: DPS Burst (Glorious Blessing / Spear Stance)
            state.spearStanceUntilTick = Math.max(state.spearStanceUntilTick, now + DPS_BURST_SEAL_TICKS);
            state.dpsSealUntilTick = now + DPS_BURST_SEAL_TICKS;
            state.dpsIceFieldCenter = player.position();
            state.punishmentCharges = 3;

            ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_BURST_DPS);
            syncStateToClients(player, state, now);

            // Apply 10.0s cooldown lock onto all other ArcaneStaffItems in inventory (hotbar switching for food/blocks stays free!)
            sealAllOtherStaves(player, DPS_BURST_SEAL_TICKS);

            // Hurl Colossal Ice Spear impact -> Flat Ice Field + Central Polar Pillar at impact point
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            BlockHitResult bhr = level.clip(new ClipContext(eye, eye.add(look.scale(14.0D)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 spearImpact = bhr.getType() != HitResult.Type.MISS ? bhr.getLocation() : player.position().add(look.scale(6.0D));
            state.dpsIceFieldCenter = spearImpact;

            ModNetwork.sendTsaritsaPolarField(level, spearImpact, 7.5D, DPS_BURST_SEAL_TICKS, 2);
            ModNetwork.sendCryoArrow(level, player.position().add(0.0D, 3.2D, 0.0D), spearImpact, 22, 3);
            level.sendParticles(ParticleTypes.SNOWFLAKE, spearImpact.x, spearImpact.y + 0.5D, spearImpact.z,
                    70, 3.5D, 1.2D, 3.5D, 0.08D);
            level.sendParticles(ParticleTypes.END_ROD, spearImpact.x, spearImpact.y + 1.0D, spearImpact.z,
                    40, 3.0D, 1.5D, 3.0D, 0.05D);
            level.playSound(null, spearImpact.x, spearImpact.y, spearImpact.z,
                    SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.4F, 1.15F);

            float burstDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * (state.netherite ? 3.4F : 2.9F);
            AABB box = new AABB(spearImpact.x - 7.5D, spearImpact.y - 3.0D, spearImpact.z - 7.5D,
                    spearImpact.x + 7.5D, spearImpact.y + 4.0D, spearImpact.z + 7.5D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
            for (LivingEntity target : targets) {
                float finalDmg = applyStellarAndCrownMultipliers(player, target, Element.CRYO, burstDmg);
                target.invulnerableTime = 0;
                ElementalGaugeManager.applyElement(target, Element.CRYO, player, finalDmg, 2.0F);
            }

            player.displayClientMessage(
                    Component.translatable("message.velorise_staves.tsaritsa_burst_dps").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    true
            );
        }
        return true;
    }

    private static void sealAllOtherStaves(ServerPlayer player, int sealTicks) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ArcaneStaffItem staff) {
                if (staff.getKind() != ArcaneStaffItem.Kind.TSARITSA_BOW
                        && staff.getKind() != ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
                    player.getCooldowns().addCooldown(staff, sealTicks);
                }
            }
        }
    }

    /**
     * Triggers a Stellar Punishment strike (up to 3 times during the 10.0s DPS Burst window)
     * when the player presses Right-Click or clicks with a sealed staff.
     */
    public static boolean tryTriggerStellarPunishment(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) return false;
        long now = level.getGameTime();
        if (now >= state.dpsSealUntilTick || state.punishmentCharges <= 0) return false;
        if (now - state.lastPunishmentTick < 10L) return false;

        state.punishmentCharges--;
        state.lastPunishmentTick = now;

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_PUNISHMENT);
        syncStateToClients(player, state, now);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = raycastTarget(level, player, eye, look, 20.0D);
        Vec3 strikePos = target != null
                ? target.position()
                : player.position().add(look.scale(5.0D));

        // Colossal Ice Spear descending from the sky onto strikePos
        Vec3 skyOrigin = strikePos.add(0.0D, 9.0D, 0.0D);
        ModNetwork.sendCryoArrow(level, skyOrigin, strikePos.add(0.0D, 0.3D, 0.0D), 16, 3);
        level.sendParticles(ParticleTypes.SNOWFLAKE, strikePos.x, strikePos.y + 0.5D, strikePos.z,
                40, 2.0D, 0.8D, 2.0D, 0.06D);
        level.sendParticles(ParticleTypes.END_ROD, strikePos.x, strikePos.y + 1.0D, strikePos.z,
                24, 1.5D, 1.2D, 1.5D, 0.04D);
        level.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.2F, 0.85F);

        float strikeDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * (state.netherite ? 2.25F : 1.95F);
        AABB box = new AABB(strikePos.x - 4.0D, strikePos.y - 2.0D, strikePos.z - 4.0D,
                strikePos.x + 4.0D, strikePos.y + 3.0D, strikePos.z + 4.0D);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
        for (LivingEntity victim : victims) {
            float finalDmg = applyStellarAndCrownMultipliers(player, victim, Element.CRYO, strikeDmg);
            victim.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(victim, Element.CRYO, player, finalDmg, 1.5F);
        }

        player.displayClientMessage(
                Component.translatable("message.velorise_staves.tsaritsa_punishment", state.punishmentCharges).withStyle(ChatFormatting.AQUA),
                true
        );
        return true;
    }

    /**
     * Server tick for Tsaritsa states and Polestar Fields.
     */
    public static void tickServer(ServerLevel level) {
        long now = level.getGameTime();

        // Tick active Polestar Fields
        if (!POLESTAR_FIELDS.isEmpty()) {
            Iterator<Map.Entry<UUID, PolestarFieldState>> it = POLESTAR_FIELDS.entrySet().iterator();
            while (it.hasNext()) {
                PolestarFieldState field = it.next().getValue();
                if (!field.dimension.equals(level.dimension())) continue;
                if (now >= field.expireTick) {
                    it.remove();
                    continue;
                }
                if (now % 10L == 0L) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (now * 0.05D) + (i * Math.PI / 6.0D);
                        double rx = field.center.x + Math.cos(angle) * POLESTAR_FIELD_RADIUS;
                        double rz = field.center.z + Math.sin(angle) * POLESTAR_FIELD_RADIUS;
                        level.sendParticles(ParticleTypes.SNOWFLAKE, rx, field.center.y + 0.15D, rz, 1, 0.05D, 0.02D, 0.05D, 0.0D);
                        if (i % 2 == 0) {
                            level.sendParticles(ParticleTypes.WITCH, rx, field.center.y + 0.2D, rz, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                        }
                    }
                }
            }
        }

        // Tick staggered converging Cryo Arrows (bypassing Minecraft iframes via sequential waves)
        if (!PENDING_ARROWS.isEmpty()) {
            Iterator<PendingArrowStrike> arrowIt = PENDING_ARROWS.iterator();
            while (arrowIt.hasNext()) {
                PendingArrowStrike pending = arrowIt.next();
                if (now >= pending.triggerTick) {
                    arrowIt.remove();
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer(pending.playerId);
                    if (owner != null && owner.isAlive() && owner.level() == level) {
                        ModNetwork.sendCryoArrow(level, pending.origin, pending.impactPos, 14, 2);
                        level.playSound(null, pending.impactPos.x, pending.impactPos.y, pending.impactPos.z,
                                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.55F);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, pending.impactPos.x, pending.impactPos.y, pending.impactPos.z,
                                20, 1.2D, 0.6D, 1.2D, 0.05D);
                        level.sendParticles(ParticleTypes.END_ROD, pending.impactPos.x, pending.impactPos.y, pending.impactPos.z,
                                8, 0.8D, 0.4D, 0.8D, 0.03D);

                        AABB aoeBox = new AABB(pending.impactPos.x - 2.8D, pending.impactPos.y - 1.8D, pending.impactPos.z - 2.8D,
                                pending.impactPos.x + 2.8D, pending.impactPos.y + 1.8D, pending.impactPos.z + 2.8D);
                        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, aoeBox,
                                e -> e != owner && e.isAlive() && isValidCombatTarget(owner, e));
                        for (LivingEntity sub : nearby) {
                            float finalSubDmg = applyStellarAndCrownMultipliers(owner, sub, Element.CRYO, pending.damage);
                            sub.invulnerableTime = 0;
                            ElementalGaugeManager.applyElement(sub, Element.CRYO, owner, finalSubDmg, 1.0F);
                        }
                    }
                }
            }
        }

        for (ServerPlayer player : level.players()) {
            TsaritsaPlayerState state = PLAYER_STATES.get(player.getUUID());
            boolean holding = isHoldingTsaritsaWeapon(player);
            if (state == null && !holding) continue;
            if (state == null) {
                state = getOrCreateState(player);
            }
            if (holding) {
                state.netherite = isHoldingNetheriteTsaritsa(player);
            }

            // Check Support Burst Unsealing detonation when swapping hotbar back to Tsaritsa
            if (holding && !state.wasHoldingTsaritsaLastTick && state.supportUnsealReady) {
                state.supportUnsealReady = false;
                state.supportSealUntilTick = 0L;
                triggerUnsealDetonation(player, level, state);
            }
            state.wasHoldingTsaritsaLastTick = holding;

            // Tick Azure Star Crown (fires 2-3 Cryo Star Projectiles every 2.0s from above the player's head, even off-field!)
            if (now < state.azureCrownUntilTick && now >= state.nextCrownStarTick) {
                state.nextCrownStarTick = now + AZURE_CROWN_FIRE_INTERVAL_TICKS;
                fireAzureCrownStars(player, level, state, now < state.supportBurstUntilTick);
            }

            // Ambient snow & starlight during Support Burst dome
            if (now < state.supportBurstUntilTick && now % 10L == 0L) {
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        state.supportDomeCenter.x, state.supportDomeCenter.y + 3.2D, state.supportDomeCenter.z,
                        18, 5.5D, 1.5D, 5.5D, 0.02D);
                level.sendParticles(ParticleTypes.END_ROD,
                        state.supportDomeCenter.x, state.supportDomeCenter.y + 3.5D, state.supportDomeCenter.z,
                        8, 5.0D, 1.2D, 5.0D, 0.01D);
            }

            syncStateToClients(player, state, now);
        }
    }

    private static void fireAzureCrownStars(ServerPlayer player, ServerLevel level, TsaritsaPlayerState state, boolean supportBurstBoosted) {
        Vec3 crownPos = player.position().add(0.0D, player.getBbHeight() + 0.55D, 0.0D);
        AABB searchBox = player.getBoundingBox().inflate(12.0D, 6.0D, 12.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
        if (candidates.isEmpty()) return;

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        int starCount = supportBurstBoosted ? 3 : 2;

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_CROWN_FLASH);
        level.playSound(null, crownPos.x, crownPos.y, crownPos.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.75F, 1.55F);

        float starDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * (supportBurstBoosted ? 0.95F : 0.75F);
        if (state.netherite) starDmg *= 1.15F;

        for (int i = 0; i < starCount; i++) {
            LivingEntity target = candidates.get(i % candidates.size());
            Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            double offsetAngle = (i * 2.0D * Math.PI / starCount) + (level.getGameTime() * 0.15D);
            Vec3 startPos = crownPos.add(Math.cos(offsetAngle) * 0.35D, 0.1D, Math.sin(offsetAngle) * 0.35D);

            ModNetwork.sendCryoArrow(level, startPos, hitPos, 10, 1);
            level.sendParticles(ParticleTypes.END_ROD, startPos.x, startPos.y, startPos.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.x, hitPos.y, hitPos.z, 8, 0.25D, 0.25D, 0.25D, 0.03D);

            float finalDmg = applyStellarAndCrownMultipliers(player, target, Element.CRYO, starDmg);
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(target, Element.CRYO, player, finalDmg, 1.0F);
        }
    }

    private static void triggerUnsealDetonation(ServerPlayer player, ServerLevel level, TsaritsaPlayerState state) {
        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_TSARITSA_UNSEAL);
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 45, 2.5D, 1.0D, 2.5D, 0.07D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 28, 2.2D, 1.0D, 2.2D, 0.05D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.15F, 1.2F);

        float unsealDmg = (float) StavesConfig.getDamage(ArcaneStaffItem.Kind.TSARITSA_BOW) * (state.netherite ? 2.4F : 2.0F);
        AABB box = player.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            float finalDmg = applyStellarAndCrownMultipliers(player, target, Element.CRYO, unsealDmg);
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(target, Element.CRYO, player, finalDmg, 1.5F);
        }
    }

    private static void syncStateToClients(ServerPlayer player, TsaritsaPlayerState state, long now) {
        int mask = 0;
        if (now < state.azureCrownUntilTick) mask |= 1;
        if (now < state.spearStanceUntilTick) mask |= 2;
        if (now < state.supportBurstUntilTick) mask |= 4;
        if (now < state.dpsSealUntilTick) mask |= 8;
        if (state.netherite) mask |= 16;
        if (now < state.dpsSealUntilTick) {
            mask |= (Math.max(0, Math.min(3, state.punishmentCharges)) << 5);
        }
        if (mask != state.lastSyncedMask || (mask > 0 && now % 10L == 0L)) {
            state.lastSyncedMask = mask;
            ModNetwork.sendStaffAnimation(player, 5000 + mask);
        }
    }

    private static LivingEntity raycastTarget(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 dir, double maxRange) {
        Vec3 end = start.add(dir.scale(maxRange));
        AABB searchBox = new AABB(start, end).inflate(1.5D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && isValidCombatTarget(player, e));
        LivingEntity best = null;
        double bestDist = maxRange + 1.0D;
        for (LivingEntity cand : candidates) {
            Vec3 toCand = cand.position().add(0.0D, cand.getBbHeight() * 0.5D, 0.0D).subtract(start);
            double proj = toCand.dot(dir);
            if (proj < 0.0D || proj > maxRange) continue;
            Vec3 closest = start.add(dir.scale(proj));
            double perpDist = closest.distanceTo(cand.position().add(0.0D, cand.getBbHeight() * 0.5D, 0.0D));
            if (perpDist <= Math.max(0.95D, cand.getBbWidth() * 0.85D) && proj < bestDist) {
                bestDist = proj;
                best = cand;
            }
        }
        return best;
    }

    public static boolean isValidCombatTarget(Entity attacker, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == attacker) return false;
        if (candidate instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (candidate instanceof Enemy) return true;
        return !(candidate instanceof net.minecraft.world.entity.npc.AbstractVillager)
                && !(candidate instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame());
    }

    public static void clearPlayer(UUID playerId) {
        PLAYER_STATES.remove(playerId);
        POLESTAR_FIELDS.remove(playerId);
    }

    public static void clearAll() {
        PLAYER_STATES.clear();
        POLESTAR_FIELDS.clear();
    }
}
