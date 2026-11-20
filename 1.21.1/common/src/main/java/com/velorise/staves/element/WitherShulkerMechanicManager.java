package com.velorise.staves.element;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Wither (Carapace projectile deflection, Contagion death burst)
 * and Shulker (Gravity Slam, Shell Guard damage reduction) staff mechanics.
 */
public final class WitherShulkerMechanicManager {

    private WitherShulkerMechanicManager() {}

    public record WitherContagionState(@Nullable UUID attackerId, boolean isNetherite, long untilTick) {}

    public static final class ShulkerSlamState {
        public final ServerLevel level;
        public final UUID targetId;
        public final @Nullable UUID casterId;
        public final boolean isNetherite;
        public final long slamDownTick;
        public final long impactTick;
        public boolean slammedDown = false;

        public ShulkerSlamState(ServerLevel level, UUID targetId, @Nullable UUID casterId, boolean isNetherite, long slamDownTick, long impactTick) {
            this.level = level;
            this.targetId = targetId;
            this.casterId = casterId;
            this.isNetherite = isNetherite;
            this.slamDownTick = slamDownTick;
            this.impactTick = impactTick;
        }
    }

    private static final Map<UUID, WitherContagionState> WITHER_CONTAGION_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ShulkerSlamState> SHULKER_SLAM_STATES = new ConcurrentHashMap<>();

    public static void tick(MinecraftServer server, long serverNow) {
        if (server == null) return;
        // 1. Tick Shulker 3rd-Shot Gravity Slams
        if (!SHULKER_SLAM_STATES.isEmpty()) {
            for (Map.Entry<UUID, ShulkerSlamState> entry : SHULKER_SLAM_STATES.entrySet()) {
                ShulkerSlamState slam = entry.getValue();
                Entity ent = slam.level.getEntity(slam.targetId);
                if (!(ent instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
                    SHULKER_SLAM_STATES.remove(entry.getKey());
                    continue;
                }
                if (!slam.slammedDown && serverNow >= slam.slamDownTick) {
                    slam.slammedDown = true;
                    target.removeEffect(MobEffects.LEVITATION);
                    Vec3 dv = target.getDeltaMovement();
                    target.setDeltaMovement(dv.x * 0.25D, -1.45D, dv.z * 0.25D);
                    target.hurtMarked = true;
                    if (target instanceof ServerPlayer sp) {
                        sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                    }
                    slam.level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            16, 0.3D, 0.3D, 0.3D, 0.1D);
                    slam.level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.75F);
                }
                if (slam.slammedDown && (target.onGround() || serverNow >= slam.impactTick)) {
                    SHULKER_SLAM_STATES.remove(entry.getKey());
                    ServerPlayer caster = slam.casterId != null ? server.getPlayerList().getPlayer(slam.casterId) : null;
                    double radius = slam.isNetherite ? 3.0D : 2.5D;
                    float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.SHULKER, slam.isNetherite);
                    float slamDmg = (slam.isNetherite ? 7.5F : 5.0F) * scale;
                    float slamU = slam.isNetherite ? 2.5F : 2.0F;
                    Vec3 hitPos = target.position();
                    AABB box = new AABB(hitPos, hitPos).inflate(radius, 1.8D, radius);
                    for (LivingEntity nearby : slam.level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && !e.isSpectator() && (caster == null || (e != caster && !caster.isAlliedTo(e))))) {
                        nearby.invulnerableTime = 0;
                        ElementalGaugeManager.applyElementBypassIcd(slam.level, nearby, Element.ELECTRO, slamU, caster, slamDmg);
                    }
                    DendroCoreManager.checkReactionsNear(slam.level, hitPos, radius + 0.5D, Element.ELECTRO, caster);
                    slam.level.sendParticles(ParticleTypes.END_ROD, hitPos.x, hitPos.y + 0.25D, hitPos.z, 24, radius * 0.45D, 0.2D, radius * 0.45D, 0.06D);
                    slam.level.sendParticles(ParticleTypes.PORTAL, hitPos.x, hitPos.y + 0.35D, hitPos.z, 28, radius * 0.5D, 0.25D, radius * 0.5D, 0.08D);
                    slam.level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.65F, 1.35F);
                    slam.level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.95F, 0.75F);
                }
            }
        }

        // 2. Prune expired Wither Contagion states every 40 ticks
        if (serverNow % 40L == 0L && !WITHER_CONTAGION_STATES.isEmpty()) {
            WITHER_CONTAGION_STATES.entrySet().removeIf(e -> serverNow >= e.getValue().untilTick());
        }
    }

    /**
     * Wither Scepter Passive 1 — Wither Carapace:
     * When HP <= 50% Max HP while holding Wither Scepter (or Omni Staff locked to Wither Scepter)
     * in main hand or offhand, grants 100% immunity to incoming projectiles.
     */
    public static boolean isWitherCarapaceActive(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.isAlive() || player.isSpectator()) return false;
        if (player.getHealth() > player.getMaxHealth() * 0.50F + 0.001F) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.WITHER
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.WITHER;
    }

    public static boolean tryDeflectProjectileWithWitherCarapace(LivingEntity entity, DamageSource source) {
        if (entity == null || entity.level().isClientSide() || !isWitherCarapaceActive(entity)) return false;
        boolean isProj = source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile;
        if (!isProj) return false;

        if (source.getDirectEntity() instanceof Projectile proj) {
            Vec3 dv = proj.getDeltaMovement();
            proj.setDeltaMovement(dv.scale(-0.65D));
            proj.hurtMarked = true;
        }
        if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.03D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    6, 0.25D, 0.25D, 0.25D, 0.02D);
            sl.playSound(null, entity.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.70F, 1.55F);
        }
        return true;
    }

    /**
     * Wither Scepter Passive 2 — Withering Contagion:
     * Marks hit enemies for 8.0s; if they die while marked (or while afflicted with Wither + Dendro),
     * they detonate in a 3.0m Withering Contagion cloud spreading 1.5U Dendro + Wither II (80t).
     */
    public static void markWitherContagion(LivingEntity target, @Nullable ServerPlayer attacker, boolean isNetherite, int durationTicks) {
        if (target == null || !target.isAlive() || target.level().isClientSide()) return;
        long now = target.level().getGameTime();
        WITHER_CONTAGION_STATES.put(target.getUUID(),
                new WitherContagionState(attacker != null ? attacker.getUUID() : null, isNetherite, now + durationTicks));
    }

    public static void tryTriggerWitherContagionOnDeath(LivingEntity deadEntity, DamageSource source) {
        if (deadEntity == null || !(deadEntity.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        WitherContagionState st = WITHER_CONTAGION_STATES.remove(deadEntity.getUUID());
        boolean validMark = st != null && now <= st.untilTick();
        boolean naturalCombo = deadEntity.hasEffect(MobEffects.WITHER)
                && ElementalGaugeManager.hasElement(deadEntity, Element.DENDRO);
        if (!validMark && !naturalCombo) return;

        boolean isNeth = st != null && st.isNetherite();
        ServerPlayer attacker = (st != null && st.attackerId() != null && level.getServer() != null)
                ? level.getServer().getPlayerList().getPlayer(st.attackerId())
                : (source.getEntity() instanceof ServerPlayer sp ? sp : null);

        double radius = isNeth ? 3.8D : 3.0D;
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.WITHER, isNeth);
        float popDmg = (isNeth ? 4.5F : 3.0F) * scale; // 1.5 hearts Basic / 2.25 hearts Netherite
        float gaugeU = isNeth ? 2.0F : 1.5F;
        Vec3 pos = deadEntity.position().add(0.0D, deadEntity.getBbHeight() * 0.5D, 0.0D);

        AABB box = new AABB(pos, pos).inflate(radius, 2.0D, radius);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != deadEntity && !e.isSpectator()
                        && (attacker == null || (e != attacker && !attacker.isAlliedTo(e)))
                        && !(e instanceof Player p && p.isCreative()))) {
            if (nearby.position().distanceToSqr(pos) > radius * radius) continue;
            ElementalGaugeManager.applyElementBypassIcd(level, nearby, Element.DENDRO, gaugeU, attacker, popDmg);
            if (ElementalGaugeManager.getInnateElement(nearby) != Element.DENDRO) {
                nearby.addEffect(new MobEffectInstance(MobEffects.WITHER, isNeth ? 120 : 80, 1)); // Wither II
            }
            markWitherContagion(nearby, attacker, isNeth, 120);
        }
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 32, radius * 0.4D, 0.35D, radius * 0.4D, 0.04D);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y, pos.z, 24, radius * 0.4D, 0.35D, radius * 0.4D, 0.04D);
        level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 12, radius * 0.3D, 0.3D, radius * 0.3D, 0.03D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.75F, 1.35F);
    }

    /**
     * Shulker Seeker Active — 3rd-Shot Gravity Slam:
     * Launches hit target rapidly upward (+0.78Y) and after 10 ticks slams them into the ground (-1.45Y)
     * for 2.5m AoE Electro Gravity Slam damage.
     */
    public static void scheduleShulkerGravitySlam(ServerLevel level, LivingEntity target, @Nullable ServerPlayer caster, boolean isNetherite) {
        if (level == null || target == null || !target.isAlive() || ElementalGaugeManager.isBossEntity(target)
                || ElementalGaugeManager.isEntityFrozen(target)) {
            return;
        }
        target.removeEffect(MobEffects.LEVITATION);
        Vec3 dv = target.getDeltaMovement();
        target.setDeltaMovement(dv.x * 0.4D, 0.78D, dv.z * 0.4D);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        long serverNow = level.getServer() != null ? level.getServer().overworld().getGameTime() : level.getGameTime();
        SHULKER_SLAM_STATES.put(target.getUUID(), new ShulkerSlamState(
                level,
                target.getUUID(),
                caster != null ? caster.getUUID() : null,
                isNetherite,
                serverNow + 9L,
                serverNow + 18L
        ));
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.5D, target.getZ(), 14, 0.25D, 0.3D, 0.25D, 0.06D);
        level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.9F, 1.45F);
    }

    /**
     * Shulker Seeker Passive — Shulker Shell Guard:
     * While inspecting items inside the portable Shulker Seeker storage menu,
     * incoming damage is reduced by 30% (40% on Netherite), like a closed Shulker shell.
     */
    public static float modifyShulkerShellGuardDamage(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0.0F || !(entity instanceof Player player) || !player.isAlive()
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        if (!(player.containerMenu instanceof com.velorise.staves.inventory.ShulkerStaffMenu shulkerMenu)) {
            return amount;
        }
        boolean isNeth = shulkerMenu.isNetherite();
        float mult = isNeth ? 0.60F : 0.70F; // -40% Netherite / -30% Basic incoming damage
        if (amount >= 1.0F && entity.level() instanceof ServerLevel sl) {
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.SHULKER_HURT_CLOSED, SoundSource.PLAYERS, 0.45F, 1.35F);
        }
        return amount * mult;
    }

    public static void clearPlayerState(UUID id) {
        if (id == null) return;
        WITHER_CONTAGION_STATES.remove(id);
        SHULKER_SLAM_STATES.remove(id);
    }

    public static void clearAll() {
        WITHER_CONTAGION_STATES.clear();
        SHULKER_SLAM_STATES.clear();
    }
}
