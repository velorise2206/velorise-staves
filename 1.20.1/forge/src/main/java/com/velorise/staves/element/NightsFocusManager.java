package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * Manages Layla's Night's Focus Cryo shield, Shooting Stars accumulation,
 * burst star barrages, and homing star projectiles.
 */
public final class NightsFocusManager {

    private NightsFocusManager() {}

    public static final java.util.List<NightsFocusStarProjectile> NIGHTS_FOCUS_PROJECTILES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            NIGHTS_FOCUS_PROJECTILES.removeIf(p -> p.owner != null && playerId.equals(p.owner.getUUID()));
        }
    }

    public static void clearAll() {
        NIGHTS_FOCUS_PROJECTILES.clear();
    }

    public static void activateNightsFocusShield(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        NIGHTS_FOCUS_UNTIL.put(id, now + 240L); // 12s Cryo Shield
        NIGHTS_FOCUS_NETHERITE.put(id, isNetherite);
        NIGHTS_FOCUS_STARS.put(id, 0);
        NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);

        float maxHp = Math.max(20.0F, player.getMaxHealth());
        float shieldHp = maxHp * (isNetherite ? 0.60F : 0.40F);
        if (ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.GEO)) {
            shieldHp *= 1.15F;
        }
        NIGHTS_FOCUS_BASE_HP.put(id, shieldHp);
        JADE_SHIELD_UNTIL.remove(id);

        com.velorise.staves.effect.ModMobEffects.removeAllShields(player);
        var cryoShieldObj = com.velorise.staves.effect.ModMobEffects.getShieldForElement(Element.CRYO);
        if (cryoShieldObj != null && cryoShieldObj.isPresent()) {
            player.addEffect(new MobEffectInstance(cryoShieldObj.get(), 240, isNetherite ? 1 : 0, false, false, true));
        }
        setPlayerShieldAbsorption(player, Math.max(player.getAbsorptionAmount(), shieldHp));
        CrystallizeShardManager.onShieldApplied(player, Element.CRYO, 240);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10);
    }

    public static void activateNightsFocusBurstStars(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ArchonUltiManager.grantUltiIframe(player, 24);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        if (now >= NIGHTS_FOCUS_UNTIL.getOrDefault(id, 0L)) {
            activateNightsFocusShield(player, isNetherite);
        }
        NIGHTS_FOCUS_STARS.put(id, 4);
        LAYLA_DREAMSPHERE_UNTIL.put(id, now + 240L); // 12.0s Celestial Dreamsphere
        LAYLA_DREAMSPHERE_NETHERITE.put(id, isNetherite);
        LAYLA_DREAMSPHERE_NEXT_SHOT.put(id, now + 20L);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 14);
    }

    public static void clearNightsFocusShield(ServerPlayer player, boolean playBreakSound) {
        UUID id = player.getUUID();
        NIGHTS_FOCUS_UNTIL.remove(id);
        NIGHTS_FOCUS_NETHERITE.remove(id);
        NIGHTS_FOCUS_STARS.remove(id);
        NIGHTS_FOCUS_LAST_AUTO_STAR.remove(id);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_BASE_HP.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
        MAVUIKA_AURA_UNTIL.remove(id);
        MAVUIKA_AURA_NETHERITE.remove(id);
        MAVUIKA_AURA_NEXT_STRIKE.remove(id);
        MAVUIKA_NIGHTSOUL_POINTS.remove(id);
        MAVUIKA_SKILL_COOLDOWN_UNTIL.remove(id);
        RAIDEN_EYE_UNTIL.remove(id);
        RAIDEN_EYE_NETHERITE.remove(id);
        RAIDEN_EYE_NEXT_PROC.remove(id);
        RAIDEN_RESOLVE.remove(id);
        RAIDEN_MUSOU_UNTIL.remove(id);
        RAIDEN_MUSOU_NETHERITE.remove(id);
        RAIDEN_SKILL_CD_UNTIL.remove(id);
        RAIDEN_BURST_CD_UNTIL.remove(id);
        RAIDEN_IFRAME_UNTIL.remove(id);
        RAIDEN_NA_COMBO.remove(id);
        RAIDEN_LAST_MELEE_TICK.remove(id);
        RAIDEN_NEXT_SWING_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
        long now = player.level().getGameTime();
        Element cryst = CrystallizeShardManager.getActiveShield(player, now);
        if (cryst == null) {
            com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(player, null, 0);
        }
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 19);
        if (playBreakSound && player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, 1.35F);
        }
    }

    public static boolean hasActiveNightsFocusShield(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Long until = NIGHTS_FOCUS_UNTIL.get(player.getUUID());
        if (until == null) return false;
        long now = player.level().getGameTime();
        if (now >= until) {
            if (player instanceof ServerPlayer sp) {
                clearNightsFocusShield(sp, false);
            } else {
                NIGHTS_FOCUS_UNTIL.remove(player.getUUID());
            }
            return false;
        }
        if (!player.isCreative() && player.getAbsorptionAmount() <= 0.01F) {
            if (player instanceof ServerPlayer sp) {
                clearNightsFocusShield(sp, true);
            } else {
                NIGHTS_FOCUS_UNTIL.remove(player.getUUID());
            }
            return false;
        }
        return true;
    }

    public static void addNightsFocusStars(ServerPlayer player, int amount, long now) {
        UUID id = player.getUUID();
        int prev = NIGHTS_FOCUS_STARS.getOrDefault(id, 0);
        int next = Math.min(4, prev + amount);
        if (next > prev) {
            int gained = next - prev;
            NIGHTS_FOCUS_STARS.put(id, next);
            float baseHp = NIGHTS_FOCUS_BASE_HP.getOrDefault(id, Math.max(20.0F, player.getMaxHealth()) * 0.40F);
            float bonus = baseHp * 0.06F * gained;
            if (bonus > 0.0F && player.getAbsorptionAmount() > 0.0F) {
                setPlayerShieldAbsorption(player, Math.min(baseHp * 1.24F, player.getAbsorptionAmount() + bonus));
            }
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.5D, player.getZ(),
                        4 * gained, 0.45D, 0.3D, 0.45D, 0.02D);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.4F + 0.1F * next);
            }
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10 + next);
        }
    }

    public static void tickNightsFocusShield(ServerPlayer player, long now) {
        UUID dsId = player.getUUID();
        long dsUntil = LAYLA_DREAMSPHERE_UNTIL.getOrDefault(dsId, 0L);
        if (now < dsUntil && player.level() instanceof ServerLevel dsLevel) {
            long nextShot = LAYLA_DREAMSPHERE_NEXT_SHOT.getOrDefault(dsId, 0L);
            if (now >= nextShot) {
                LAYLA_DREAMSPHERE_NEXT_SHOT.put(dsId, now + 30L); // Fires Starlight Slug every 1.5s (30 ticks)
                LivingEntity dsTarget = findNearestNightsFocusTarget(dsLevel, player);
                if (dsTarget != null) {
                    boolean dsNeth = LAYLA_DREAMSPHERE_NETHERITE.getOrDefault(dsId, false);
                    float slugDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.NIGHTS_FOCUS, dsNeth) * 0.90F;
                    float slugUnits = dsNeth ? 1.0F : 0.75F;
                    Vec3 spherePos = player.position().add(0.0D, player.getBbHeight() + 1.15D, 0.0D);
                    NIGHTS_FOCUS_PROJECTILES.add(new NightsFocusStarProjectile(dsLevel, player, dsTarget, spherePos, slugDmg, slugUnits));
                    dsLevel.sendParticles(ParticleTypes.SNOWFLAKE, spherePos.x, spherePos.y, spherePos.z, 6, 0.18D, 0.18D, 0.18D, 0.03D);
                    dsLevel.sendParticles(ParticleTypes.END_ROD, spherePos.x, spherePos.y, spherePos.z, 3, 0.12D, 0.12D, 0.12D, 0.02D);
                    dsLevel.playSound(null, spherePos.x, spherePos.y, spherePos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.70F, 1.60F);
                    if (hasActiveNightsFocusShield(player)) {
                        addNightsFocusStars(player, 1, now);
                    }
                }
            }
        } else if (dsUntil > 0L) {
            LAYLA_DREAMSPHERE_UNTIL.remove(dsId);
            LAYLA_DREAMSPHERE_NETHERITE.remove(dsId);
            LAYLA_DREAMSPHERE_NEXT_SHOT.remove(dsId);
        }
        if (!hasActiveNightsFocusShield(player)) return;
        UUID id = player.getUUID();
        if (player.level() instanceof ServerLevel sl) {
            if (now % 10 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                        3, 0.35D, 0.45D, 0.35D, 0.01D);
            }
            int pending = NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0);
            if (pending > 0 && now % 9 == 0) {
                if (fireSingleNightsFocusStar(sl, player)) {
                    int remaining = pending - 1;
                    if (remaining <= 0) {
                        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
                        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10);
                    } else {
                        NIGHTS_FOCUS_PENDING_SHOTS.put(id, remaining);
                        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10 + remaining);
                    }
                }
            }
        }

        if (NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0) > 0) {
            NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
        } else {
            long lastAuto = NIGHTS_FOCUS_LAST_AUTO_STAR.getOrDefault(id, now);
            if (now - lastAuto >= 30L) { // 1.5s per Night Star
                NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
                addNightsFocusStars(player, 1, now);
            }
        }

        int stars = NIGHTS_FOCUS_STARS.getOrDefault(id, 0);
        if (stars >= 4 && NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0) <= 0 && player.level() instanceof ServerLevel sl) {
            LivingEntity nearest = findNearestNightsFocusTarget(sl, player);
            if (nearest != null) {
                NIGHTS_FOCUS_STARS.put(id, 0);
                NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
                if (fireSingleNightsFocusStar(sl, player)) {
                    NIGHTS_FOCUS_PENDING_SHOTS.put(id, 3);
                    com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 13);
                } else {
                    NIGHTS_FOCUS_PENDING_SHOTS.put(id, 4);
                    com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 14);
                }
            }
        }
    }

    public static @Nullable LivingEntity findNearestNightsFocusTarget(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(16.0D);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                ent -> ent.isAlive() && !ent.isSpectator() && ent != player && !player.isAlliedTo(ent)
                        && (ent instanceof net.minecraft.world.entity.monster.Enemy || ent instanceof Player))) {
            double d = e.distanceToSqr(player);
            if (d <= 256.0D && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    public static final class NightsFocusStarProjectile {
        final ServerLevel level;
        final ServerPlayer owner;
        @Nullable LivingEntity target;
        Vec3 pos;
        Vec3 lastTargetPos;
        final float damage;
        final float units;
        int age;

        NightsFocusStarProjectile(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 pos, float damage, float units) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.pos = pos;
            this.lastTargetPos = target.getBoundingBox().getCenter();
            this.damage = damage;
            this.units = units;
        }
    }

    public static boolean fireSingleNightsFocusStar(ServerLevel level, ServerPlayer player) {
        LivingEntity target = findNearestNightsFocusTarget(level, player);
        if (target == null) return false;
        boolean isNetherite = NIGHTS_FOCUS_NETHERITE.getOrDefault(player.getUUID(), false);
        float dmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.NIGHTS_FOCUS, isNetherite) * 0.75F;
        float units = isNetherite ? 0.75F : 0.5F;

        Vec3 start = player.position().add(0.0D, player.getBbHeight() + 0.65D, 0.0D);
        NIGHTS_FOCUS_PROJECTILES.add(new NightsFocusStarProjectile(level, player, target, start, dmg, units));
        level.sendParticles(ParticleTypes.SNOWFLAKE, start.x, start.y, start.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 0.6F, 1.55F);
        return true;
    }

    public static void tickNightsFocusProjectiles() {
        if (NIGHTS_FOCUS_PROJECTILES.isEmpty()) return;
        for (NightsFocusStarProjectile p : NIGHTS_FOCUS_PROJECTILES) {
            p.age++;
            Vec3 targetPos = (p.target != null && p.target.isAlive() && !p.target.isRemoved())
                    ? p.target.getBoundingBox().getCenter()
                    : p.lastTargetPos;
            p.lastTargetPos = targetPos;

            Vec3 toTarget = targetPos.subtract(p.pos);
            double dist = toTarget.length();
            boolean hit = dist <= 0.75D;
            if (!hit && p.target != null && p.target.isAlive()) {
                AABB box = new AABB(p.pos.x - 0.35D, p.pos.y - 0.35D, p.pos.z - 0.35D,
                        p.pos.x + 0.35D, p.pos.y + 0.35D, p.pos.z + 0.35D);
                hit = p.target.getBoundingBox().intersects(box);
            }

            if (hit || p.age > 65) {
                NIGHTS_FOCUS_PROJECTILES.remove(p);
                if (hit && p.target != null && p.target.isAlive()) {
                    p.level.sendParticles(ParticleTypes.SNOWFLAKE, p.pos.x, p.pos.y, p.pos.z, 8, 0.18D, 0.18D, 0.18D, 0.04D);
                    p.level.sendParticles(ParticleTypes.END_ROD, p.pos.x, p.pos.y, p.pos.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
                    p.level.playSound(null, p.pos.x, p.pos.y, p.pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.7F);
                    applyElement(p.level, p.target, Element.CRYO, p.units, p.owner, p.damage);
                }
                continue;
            }

            Vec3 dir = dist > 1.0E-4D ? toTarget.scale(1.0D / dist) : new Vec3(0.0D, 1.0D, 0.0D);
            if (p.age <= 4) {
                dir = dir.add(0.0D, 0.35D, 0.0D).normalize();
            }
            p.pos = p.pos.add(dir.scale(0.55D)); // Moderate flight speed (~11 blocks/s)
            p.level.sendParticles(ParticleTypes.SNOWFLAKE, p.pos.x, p.pos.y, p.pos.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            p.level.sendParticles(ParticleTypes.END_ROD, p.pos.x, p.pos.y, p.pos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }
}
