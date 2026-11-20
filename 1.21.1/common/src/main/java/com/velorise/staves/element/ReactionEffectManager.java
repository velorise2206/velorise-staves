package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Executes core elemental reaction side effects (Overloaded, Superconduct,
 * Electro-Charged, Swirl, Crystallize, Bloom, Burgeon, Hyperbloom, Burning, Voltaic Arcs).
 */
public final class ReactionEffectManager {

    private ReactionEffectManager() {}

    public static void executeReactionEffectInternal(
            ServerLevel level,
            LivingEntity target,
            ElementalReaction reaction,
            Element auraElement,
            float removedAuraU,
            @Nullable ServerPlayer attacker,
            double dmgMult
    ) {
        if (attacker != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
            notifyResonanceReaction(reaction, attacker, level.getGameTime());
        }
        onHydroReactionTriggered(level, target.position(), reaction, auraElement, null, attacker);
        if (reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
            NahidaManager.onElementalReactionOrCoreDamage(level, target, attacker);
        }
        Vec3 pos = target.position();
        boolean emitVisual = canEmitReactionVisual(level, pos, 3);
        int emLevel = getElementalMasteryLevel(attacker);
        float transEmMult = getTransformativeMultiplier(emLevel);
        switch (reaction) {
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE -> {
                if (reaction == ElementalReaction.FORWARD_VAPORIZE) {
                    target.clearFire();
                }
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.8D, pos.z, 8, 0.3D, 0.3D, 0.3D, 0.04D);
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.3F);
                }
            }
            case FORWARD_MELT, REVERSE_MELT -> {
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 8, 0.35D, 0.35D, 0.35D, 0.04D);
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
                }
            }
            case OVERLOADED -> {
                BURNING_UNTIL.remove(target.getUUID());
                BURNING_EM_LEVEL.remove(target.getUUID());
                BURNING_ATTACKER_UUID.remove(target.getUUID());
                LAST_BURN_PYRO_REFRESH.remove(target.getUUID());
                if (!target.isInLava()) {
                    target.clearFire();
                }
                triggerOverloadExplosion(level, target, attacker, dmgMult);
            }
            case ELECTRO_CHARGED -> {
                if (emitVisual) {
                    // ParticleTypes.ELECTRIC_SPARK removed
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.7F);
                }
            }
            case STELLAR_CONDUCT -> {
                long debuffEnd = level.getGameTime() + 240L;
                SUPERCONDUCT_UNTIL.put(target.getUUID(), debuffEnd);
                float stellarMult = attacker != null ? TsaritsaManager.getStellarReactionMultiplier(attacker) : 1.0F;
                float rawStellarDmg = (float) (2.5F * dmgMult) * transEmMult * stellarMult;
                TsaritsaManager.onStellarConductTriggered(level, target, attacker, rawStellarDmg);
                AABB box = target.getBoundingBox().inflate(4.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
                    if (isOmniInnate(nearby)) {
                        notifyOmniImmuneEnvironmental(level, nearby);
                        continue;
                    }
                    float dmg = applyMobElementalResistance(nearby, Element.CRYO, rawStellarDmg);
                    nearby.invulnerableTime = 0;
                    LivingEntity scCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
                    hurtWithPreAppliedElementalRes(nearby, ElementalDeathMessageHelper.createTypedDamageSource(
                            level.damageSources().freeze(), scCredit, ElementalReaction.STELLAR_CONDUCT, Element.CRYO), dmg);
                    SUPERCONDUCT_UNTIL.put(nearby.getUUID(), debuffEnd);
                    if (nearby != target && dmg > 0.0F) {
                        syncAuras(nearby, getEntityElements(nearby.getUUID()), Element.CRYO, Element.ELECTRO, ElementalReaction.STELLAR_CONDUCT, dmg);
                    }
                }
            }
            case STELLAR_SWIRL -> {
                float stellarMult = attacker != null ? TsaritsaManager.getStellarReactionMultiplier(attacker) : 1.0F;
                float swirlAoeDmg = (float) (4.0F * 1.15F * dmgMult) * transEmMult * stellarMult;
                TsaritsaManager.onStellarSwirlTriggered(level, target, attacker, swirlAoeDmg);
            }
            case SUPERCONDUCT -> {
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 10, 1.0D, 0.4D, 1.0D, 0.03D);
                    // ParticleTypes.ELECTRIC_SPARK removed
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                long debuffEnd = level.getGameTime() + 240L; // 12 seconds Physical RES shred (-40% Physical RES)
                SUPERCONDUCT_UNTIL.put(target.getUUID(), debuffEnd);
                AABB box = target.getBoundingBox().inflate(3.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
                    if (isOmniInnate(nearby)) {
                        notifyOmniImmuneEnvironmental(level, nearby);
                        continue;
                    }
                    float dmg = applyMobElementalResistance(nearby, Element.CRYO,
                            (float) (1.0F * dmgMult) * transEmMult); // 0.5 heart Cryo AoE damage scaled by EM
                    if (nearby instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.CRYO) {
                        dmg *= 0.4F;
                    }
                    nearby.invulnerableTime = 0;
                    LivingEntity scCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
                    hurtWithPreAppliedElementalRes(nearby, ElementalDeathMessageHelper.createTypedDamageSource(
                            level.damageSources().freeze(), scCredit, ElementalReaction.SUPERCONDUCT, Element.CRYO), dmg);
                    SUPERCONDUCT_UNTIL.put(nearby.getUUID(), debuffEnd);
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
                    if (nearby != target && dmg > 0.0F) {
                        syncAuras(nearby, getEntityElements(nearby.getUUID()), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, dmg);
                    }
                }
            }
            case FROZEN -> {
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 10, 0.4D, 0.6D, 0.4D, 0.02D);
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
                if (isEntitySubmergedInWater(target)) {
                    com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(level, target.position(), 1);
                }
            }
            case QUICKEN -> {
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.8D, pos.z, 8, 0.35D, 0.5D, 0.35D, 0.04D);
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            }
            case BLOOM, REVERSE_BLOOM -> {
                long now = level.getGameTime();
                Long lastSpawn = LAST_BLOOM_SPAWN.get(target.getUUID());
                if (lastSpawn == null || now < lastSpawn || (now - lastSpawn) >= 10L) { // 0.5s ICD per target
                    DendroCoreManager.spawn(level, target.position(), attacker, (float) dmgMult * transEmMult);
                    LAST_BLOOM_SPAWN.put(target.getUUID(), now);
                }
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.8D, pos.z, 10, 0.7D, 0.4D, 0.7D, 0.03D);
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.1F);
                }
            }
            case BURNING -> {
                if (getInnateElement(target) != Element.PYRO && !target.fireImmune() && !(target instanceof Warden)) {
                    long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                            target, 200L);
                    BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + burnTicks);
                    BURNING_EM_LEVEL.put(target.getUUID(), emLevel);
                    LivingEntity burnCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
                    if (burnCredit != null && burnCredit != target) {
                        BURNING_ATTACKER_UUID.put(target.getUUID(), burnCredit.getUUID());
                    }
                    EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, pos, 1.5D);
                    target.igniteForSeconds(5);
                    if (emitVisual) {
                        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.8D, pos.z, 6, 0.3D, 0.4D, 0.3D, 0.03D);
                    }
                }
            }
            case SWIRL -> {
                if (emitVisual) {
                    level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.1F);
                }
                AABB box = target.getBoundingBox().inflate(5.0D);
                // Genshin Impact rule: Pyro, Electro, Cryo Swirls deal AoE damage + spread element; Hydro Swirl only spreads Wet aura!
                // Spread gauge formula: U_Swirl = (U_removed - 0.04) * 1.25 + 1.5
                float swirlSpreadU = Math.max(0.5F, (Math.max(0.04F, removedAuraU) - 0.04F) * 1.25F + 1.5F);
                if (auraElement == Element.PYRO) {
                    EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, pos, 3.5D);
                }
                if (auraElement == Element.PYRO || auraElement == Element.ELECTRO) {
                    DendroCoreManager.checkReactionsNear(level, pos, 5.0D, auraElement, attacker);
                }
                float swirlAoeDmg = (auraElement == Element.HYDRO) ? 0.0F : (float) (4.0F * dmgMult) * transEmMult;
                int swirledCount = 0;
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != target && e != attacker)) {
                    if (swirledCount++ >= 8) break;
                    applyElement(level, nearby, auraElement, swirlSpreadU, attacker, swirlAoeDmg);
                }
            }
            case CRYSTALLIZE -> {
                double popY = target.getY() + target.getBbHeight() * 0.80D;
                if (emitVisual) {
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), popY, target.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.05D);
                    level.playSound(null, target.getX(), popY, target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
                CrystallizeShardManager.spawn(level, target, auraElement, target.getUUID(), emLevel);
                XilonenManager.onCrystallizeTriggered(attacker, auraElement, level.getGameTime());
            }
            default -> {}
        }
    }

    public static void triggerOverloadExplosion(ServerLevel level, LivingEntity target, @Nullable ServerPlayer attacker, double dmgMult) {
        Vec3 pos = target.position();
        if (canEmitReactionVisual(level, pos, 2)) {
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5D, pos.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            // ParticleTypes.ELECTRIC_SPARK removed
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        EnvironmentalBlockManager.applyOverloadEnvironmentEffects(level, pos, 3.5D);

        AABB box = target.getBoundingBox().inflate(3.5D);
        float transEmMult = getTransformativeMultiplier(getElementalMasteryLevel(attacker));
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
            if (isOmniInnate(victim)) {
                notifyOmniImmuneEnvironmental(level, victim);
                continue;
            }
            float dmg = applyMobElementalResistance(victim, Element.PYRO, (float) (6.0F * dmgMult) * transEmMult);
            if (victim instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.PYRO) {
                dmg *= 0.4F;
            }
            victim.invulnerableTime = 0;
            LivingEntity olCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
            if (dmg > 0.0F) {
                ElementalDeathMessageHelper.recordReactionForDeath(victim, ElementalReaction.OVERLOADED, Element.PYRO, olCredit);
            }
            hurtWithPreAppliedElementalRes(victim, getElementalDamageSource(level, attacker, ElementalReaction.OVERLOADED, Element.PYRO), dmg);
            if (victim != target && dmg > 0.0F) {
                syncAuras(victim, getEntityElements(victim.getUUID()), Element.ELECTRO, Element.PYRO, ElementalReaction.OVERLOADED, dmg);
            }
            Vec3 diff = victim == target && attacker != null
                ? target.position().subtract(attacker.position())
                : victim.position().subtract(pos);
            Vec3 knock = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1, 0);
            victim.setDeltaMovement(victim.getDeltaMovement().add(knock.x * 0.85D, 0.45D, knock.z * 0.85D));
            victim.hurtMarked = true;
        }
    }

    public static void drawVoltaicArc(ServerLevel level, Vec3 start, Vec3 end) {
        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.0D, false);
    }

    public static void drawVoltaicArc(ServerLevel level, Entity from, Entity to) {
        ElectrifiedWaterManager.drawElectricArc(level, from, to, 8);
    }

    public static void handleCreeperExplosionBurst(
            ServerLevel level,
            Creeper creeper,
            Vec3 center,
            float explosionRadius
    ) {
        if (level == null || creeper == null || center == null) return;
        UUID creeperId = creeper.getUUID();
        long now = level.getGameTime();
        Long prevBurst = EXPLODED_CREEPERS.putIfAbsent(creeperId, now);
        if (prevBurst != null) return;
        if (creeper.getTags().contains("velorise_staves:explosion_burst_done")) return;
        creeper.addTag("velorise_staves:explosion_burst_done");

        boolean isOmni = isOmniInnate(creeper);
        EnumSet<Element> innateElements = getInnateElements(creeper);
        if (!isOmni && innateElements.isEmpty()) {
            return;
        }

        boolean powered = creeper.isPowered();
        double baseRad = explosionRadius > 0.0F ? explosionRadius : (powered ? 6.0D : 3.0D);
        double burstRadius = powered ? Math.max(6.0D, baseRad * 1.5D) : Math.max(3.5D, baseRad * 1.6D);
        float applyU = powered ? 2.0F : 1.5F;
        double dmgMult = 1.0D;
        try {
            dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();
        } catch (Exception ignored) {}
        float baseElemDmg = (float) ((powered ? 3.0F : 2.0F) * dmgMult);

        // Environmental element signatures around crater center
        if (isOmni) {
            level.sendParticles(ParticleTypes.FIREWORK, center.x, center.y + 0.6D, center.z, 26, 1.0D, 0.8D, 1.0D, 0.08D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.6D, center.z, 18, 0.9D, 0.7D, 0.9D, 0.05D);
        } else {
            if (innateElements.contains(Element.PYRO)) {
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.6D, center.z, 32, 1.2D, 0.6D, 1.2D, 0.06D);
                BlockPos bp = BlockPos.containing(center);
                for (BlockPos p : BlockPos.betweenClosed(bp.offset(-2, -1, -2), bp.offset(2, 2, 2))) {
                    if (!level.hasChunkAt(p)) continue;
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.FROSTED_ICE) || st.is(Blocks.ICE)) {
                        level.setBlockAndUpdate(p, level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
                    }
                }
            }
            if (innateElements.contains(Element.HYDRO)) {
                level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.6D, center.z, 34, 1.2D, 0.6D, 1.2D, 0.1D);
                BlockPos bp = BlockPos.containing(center);
                for (BlockPos p : BlockPos.betweenClosed(bp.offset(-2, -1, -2), bp.offset(2, 2, 2))) {
                    if (level.hasChunkAt(p) && level.getBlockState(p).is(Blocks.FIRE)) {
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            if (innateElements.contains(Element.ELECTRO)) {
                // ParticleTypes.ELECTRIC_SPARK removed
                ElectrifiedWaterManager.electrify(level, BlockPos.containing(center), powered ? 3.5D : 2.5D, 60, null, 1.5F);
            }
            if (innateElements.contains(Element.CRYO)) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.6D, center.z, 32, 1.2D, 0.7D, 1.2D, 0.05D);
                com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(level, center, powered ? 4 : 3);
            }
            if (innateElements.contains(Element.DENDRO)) {
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.7D, center.z, 28, 1.1D, 0.7D, 1.1D, 0.05D);
                BlockPos bp = BlockPos.containing(center);
                if (level.getFluidState(bp).is(FluidTags.WATER) || level.getFluidState(bp.below()).is(FluidTags.WATER)) {
                    DendroCoreManager.spawn(level, center, null, 1.0F);
                }
            }
            if (innateElements.contains(Element.ANEMO)) {
                level.sendParticles(ParticleTypes.GUST, center.x, center.y + 0.7D, center.z, 4, 0.7D, 0.4D, 0.7D, 0.0D);
                level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.7D, center.z, 18, 1.0D, 0.5D, 1.0D, 0.08D);
            }
            if (innateElements.contains(Element.GEO)) {
                level.sendParticles(ParticleTypes.WAX_ON, center.x, center.y + 0.6D, center.z, 24, 1.1D, 0.6D, 1.1D, 0.05D);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.2F);
            }
        }

        AABB area = new AABB(center.x - burstRadius, center.y - burstRadius, center.z - burstRadius,
                center.x + burstRadius, center.y + burstRadius, center.z + burstRadius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e.isAlive() && !e.isRemoved() && e != creeper && !e.isSpectator()
                        && !(e instanceof Player p && p.isCreative()));

        LivingEntity prevAttacker = CURRENT_REACTION_ATTACKER.get();
        CURRENT_REACTION_ATTACKER.set(creeper);
        try {
            for (LivingEntity victim : victims) {
                double dist = Math.sqrt(victim.distanceToSqr(center));
                if (dist > burstRadius) continue;
                float seen = net.minecraft.world.level.Explosion.getSeenPercent(center, victim);
                if (seen <= 0.0F) continue;
                float falloff = (float) Math.max(0.35D, 1.0D - (dist / (burstRadius + 0.5D)));
                float elemDmg = baseElemDmg * falloff * seen;

                if (isOmni) {
                    Element first = chooseOmniMutatedAttackElement(victim, creeper);
                    victim.invulnerableTime = 0;
                    if (first == Element.PYRO) ensurePreDeathPyroIgnite(victim, 4);
                    applyElementBypassIcd(level, victim, first, applyU, null, elemDmg);
                    if (powered && victim.isAlive()) {
                        Element second = Element.ELECTRO != first ? Element.ELECTRO : Element.PYRO;
                        victim.invulnerableTime = 0;
                        applyElementBypassIcd(level, victim, second, 1.5F, null, elemDmg * 0.7F);
                    }
                    victim.invulnerableTime = 0;
                } else {
                    for (Element elem : innateElements) {
                        if (!victim.isAlive()) break;
                        victim.invulnerableTime = 0;
                        if (elem == Element.PYRO) {
                            ensurePreDeathPyroIgnite(victim, 4);
                        }
                        applyElementBypassIcd(level, victim, elem, applyU, null, elemDmg);
                        if (elem == Element.ANEMO) {
                            Vec3 dir = victim.position().subtract(center);
                            Vec3 norm = dir.lengthSqr() > 1.0E-4D ? dir.normalize() : new Vec3(0.0D, 0.3D, 0.0D);
                            victim.setDeltaMovement(victim.getDeltaMovement().add(norm.x * 0.75D * falloff, 0.35D * falloff, norm.z * 0.75D * falloff));
                            victim.hurtMarked = true;
                        }
                    }
                    victim.invulnerableTime = 0;
                }
            }
        } finally {
            if (prevAttacker == null) CURRENT_REACTION_ATTACKER.remove();
            else CURRENT_REACTION_ATTACKER.set(prevAttacker);
        }
    }
}
