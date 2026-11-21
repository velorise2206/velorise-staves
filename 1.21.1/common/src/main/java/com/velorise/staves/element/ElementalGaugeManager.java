package com.velorise.staves.element;

import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */
public final class ElementalGaugeManager {
    public static final TagKey<EntityType<?>> PYRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "pyro_creatures"));
    public static final TagKey<EntityType<?>> HYDRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "hydro_creatures"));
    public static final TagKey<EntityType<?>> ELECTRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "electro_creatures"));
    public static final TagKey<EntityType<?>> CRYO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "cryo_creatures"));
    public static final TagKey<EntityType<?>> DENDRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "dendro_creatures"));
    public static final TagKey<EntityType<?>> ANEMO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "anemo_creatures"));
    public static final TagKey<EntityType<?>> GEO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "geo_creatures"));
    public static final TagKey<Block> MUSHROOM_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "mushroom_blocks"));

    public static final Map<UUID, Map<Element, GaugeState>> GAUGES = new ConcurrentHashMap<>();
    public static final Map<UUID, GaugeState> FROZEN_GAUGES = new ConcurrentHashMap<>();
    public static final Map<UUID, GaugeState> QUICKEN_GAUGES = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> FROZEN_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> QUICKEN_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> BURNING_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> BURNING_EM_LEVEL = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> BURNING_ATTACKER_UUID = new ConcurrentHashMap<>();
    public static final ThreadLocal<Boolean> IS_PROCESSING_RAIDEN_PROC = ThreadLocal.withInitial(() -> Boolean.FALSE);
    public static final Map<UUID, Integer> ELECTRO_CHARGE_EM_LEVEL = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> SUPPRESSED_PHYSICAL_FLOAT_TICK = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> LAST_BURN_PYRO_REFRESH = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> INNATE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> ELECTRO_CHARGE_TICK = new ConcurrentHashMap<>();
    public static final Map<Long, Long> LAST_PROXIMITY_SHOCK = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> LAST_BLOOM_SPAWN = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> SUPERCONDUCT_UNTIL = new ConcurrentHashMap<>();
    public static final String TAG_FROZEN_STATUE = "velorise_staves:frozen_statue";
    public static final String TAG_INNATE_OMNI = "velorise_staves:innate_omni";
    public static final Set<UUID> OMNI_INNATE_ENTITIES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Map<UUID, Long> LAST_OMNI_ENV_IMMUNE = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> LAST_MUSHROOM_CONTACT = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> EXPLODED_CREEPERS = new ConcurrentHashMap<>();
    public static final long HP_CHANGE_BUFF_TICKS = 200L;

    public static final Map<UUID, FrozenPoseState> FROZEN_POSES = new ConcurrentHashMap<>();
    public static final ThreadLocal<Boolean> IS_PROCESSING_DAMAGE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_REACTION = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_THORNS = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_SWORD_IMBUE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_MODIFYING_ENV_DAMAGE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> ELEMENTAL_RES_APPLIED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    public static final ThreadLocal<Integer> REACTION_DEPTH = ThreadLocal.withInitial(() -> 0);
    public static final int MAX_REACTION_DEPTH = 4;
    public static final ThreadLocal<Element> THORNS_CONTEXT_ELEMENT = new ThreadLocal<>();
    public static final ThreadLocal<@Nullable LivingEntity> CURRENT_REACTION_ATTACKER = new ThreadLocal<>();
    public static final Map<Long, Long> LAST_THORNS_REFLECT = new ConcurrentHashMap<>();
    public static final ResourceLocation COOP_HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "coop_mob_health_scale");
    public static final ResourceLocation COOP_ATK_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "coop_mob_attack_scale");

    // Genshin Impact Simultaneous Reaction Priority order for coexisting auras
    public static final Element[] REACTION_PRIORITY_ORDER = {
        Element.ELECTRO,
        Element.PYRO,
        Element.HYDRO,
        Element.CRYO,
        Element.DENDRO
    };

    public static class IcdEntry {
        int hitCount;
        long firstHitTime;

        IcdEntry(int hitCount, long firstHitTime) {
            this.hitCount = hitCount;
            this.firstHitTime = firstHitTime;
        }
    }

    public static final Map<Long, IcdEntry> ICD_TRACKER = new ConcurrentHashMap<>();
    public static final Map<Long, Long> REACTION_ICD_TRACKER = new ConcurrentHashMap<>();

    private ElementalGaugeManager() {}

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        syncAuras(target, elements, reactedAura, trigger, reaction, 0.0F);
    }

    public static int getEntityMaxDurationTicks(UUID entityId) {
        int max = 0;
        Map<Element, GaugeState> gauges = GAUGES.get(entityId);
        if (gauges != null) {
            for (GaugeState gs : gauges.values()) {
                if (gs != null) {
                    int rem = gs.getRemainingTicks();
                    if (rem > max) max = rem;
                }
            }
        }
        GaugeState qg = QUICKEN_GAUGES.get(entityId);
        if (qg != null) {
            max = Math.max(max, qg.getRemainingTicks());
        }
        GaugeState fg = FROZEN_GAUGES.get(entityId);
        if (fg != null) {
            max = Math.max(max, fg.getRemainingTicks());
        }
        return max;
    }

    public static @Nullable Map<Element, GaugeState> getEntityGauges(UUID entityId) {
        return GAUGES.get(entityId);
    }

    public static @Nullable GaugeState getQuickenGauge(UUID entityId) {
        return QUICKEN_GAUGES.get(entityId);
    }

    public static @Nullable GaugeState getFrozenGauge(UUID entityId) {
        return FROZEN_GAUGES.get(entityId);
    }

    public static boolean isEntityFrozen(@Nullable Entity entity) {
        if (entity == null) return false;
        UUID id = entity.getUUID();
        GaugeState fg = FROZEN_GAUGES.get(id);
        if (fg != null && !fg.isExpired()) return true;
        Long until = FROZEN_UNTIL.get(id);
        return until != null && entity.level().getGameTime() < until;
    }

    public static boolean isEntityQuickened(@Nullable Entity entity) {
        if (entity == null) return false;
        GaugeState qg = QUICKEN_GAUGES.get(entity.getUUID());
        return qg != null && !qg.isExpired();
    }

    /**
     * Returns all active display elements on the entity (including Quicken Dendro aura and Frozen Cryo aura).
     */
    public static Collection<Element> getEffectiveElements(LivingEntity target, Collection<Element> baseElements) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        if (baseElements != null) {
            set.addAll(baseElements);
        }
        UUID id = target.getUUID();
        long now = target.level().getGameTime();
        Long quickenEnd = QUICKEN_UNTIL.get(id);
        GaugeState qg = QUICKEN_GAUGES.get(id);
        if ((quickenEnd != null && now < quickenEnd) || (qg != null && !qg.isExpired())) {
            set.add(Element.DENDRO);
        }
        Long frozenEnd = FROZEN_UNTIL.get(id);
        Long petrifiedEnd = PETRIFIED_UNTIL.get(id);
        GaugeState fg = FROZEN_GAUGES.get(id);
        boolean petrified = petrifiedEnd != null && now < petrifiedEnd;
        if (petrified) {
            set.add(Element.GEO);
        }
        if ((fg != null && !fg.isExpired()) || (!petrified && frozenEnd != null && now < frozenEnd)) {
            set.add(Element.CRYO);
        }
        set.addAll(getInnateElements(target));
        return set;
    }

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        UUID id = target.getUUID();
        long now = target.level().getGameTime();
        boolean alive = target.isAlive();
        Collection<Element> effective = alive ? getEffectiveElements(target, elements) : Collections.emptyList();
        int durationTicks = alive ? getEntityMaxDurationTicks(id) : 0;
        if (alive) {
            Long quickenEnd = QUICKEN_UNTIL.get(id);
            if (quickenEnd != null && now < quickenEnd) {
                durationTicks = Math.max(durationTicks, (int) (quickenEnd - now));
            }
            EnumSet<Element> innateSet = getInnateElements(target);
            if (isOmniInnate(target)) {
                durationTicks = -2; // -2 represents permanent Omni Mutated innate state
            } else if (!innateSet.isEmpty() && !effective.isEmpty() && innateSet.containsAll(effective)) {
                durationTicks = -1; // -1 represents pure permanent innate aura (no temporary coexisting auras)
            }
            if (isEntityFrozen(target)) {
                int freezeRem = getFrozenRemainingTicks(id, now);
                if (freezeRem > 0) {
                    durationTicks = freezeRem;
                }
            }
        }
        if (damage > 0.0F && (Boolean.TRUE.equals(IS_PROCESSING_SWORD_IMBUE.get()) || Boolean.TRUE.equals(IS_MODIFYING_ENV_DAMAGE.get()))) {
            SUPPRESSED_PHYSICAL_FLOAT_TICK.put(id, now);
        }
        if (reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
            ElementalDeathMessageHelper.recordReactionForDeath(target, reaction, trigger != null ? trigger : reactedAura, CURRENT_REACTION_ATTACKER.get());
        }
        if (alive && damage > 0.0F && reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE
                && reaction != ElementalReaction.BLOOM && reaction != ElementalReaction.REVERSE_BLOOM
                && reaction != ElementalReaction.BURGEON && reaction != ElementalReaction.HYPERBLOOM
                && !(reaction == ElementalReaction.BURNING
                        && (hasInnateElement(target, Element.PYRO) || target.fireImmune() || target instanceof Warden))) {
            MobHazardAvoidanceManager.reportReactionOrHazard(target, CURRENT_REACTION_ATTACKER.get(), target.position(), reactedAura, trigger);
        }
        com.velorise.staves.network.ModNetwork.sendAuraSync(target, effective, reactedAura, trigger, reaction, damage, durationTicks);
    }

    public static Collection<Element> getEntityElements(UUID entityId) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        Map<Element, GaugeState> gauges = GAUGES.get(entityId);
        if (gauges != null) {
            set.addAll(gauges.keySet());
        }
        GaugeState qg = QUICKEN_GAUGES.get(entityId);
        if (qg != null && !qg.isExpired()) {
            set.add(Element.DENDRO);
        }
        GaugeState fg = FROZEN_GAUGES.get(entityId);
        if (fg != null && !fg.isExpired()) {
            set.add(Element.CRYO);
        }
        return set;
    }

    public static boolean hasElement(LivingEntity target, Element element) {
        if (target == null) return false;
        return getEntityElements(target.getUUID()).contains(element);
    }

    public static boolean hasSuperconductDebuff(LivingEntity entity, long now) {
        Long until = SUPERCONDUCT_UNTIL.get(entity.getUUID());
        return until != null && now < until;
    }

    public static boolean hasBurning(@Nullable UUID id) {
        return id != null && BURNING_UNTIL.containsKey(id);
    }

    public static @Nullable UUID getBurningAttackerUuid(@Nullable UUID id) {
        return id != null ? BURNING_ATTACKER_UUID.get(id) : null;
    }

    /**
     * Authentic Genshin Impact 3-Tier Piecewise Resistance Formula:
     * - Negative RES (RES < 0): 1.0 - (RES / 2.0)
     * - Standard RES (0 <= RES <= 0.75): 1.0 - RES
     * - Extreme High RES (RES > 0.75): 1.0 / (4.0 * RES + 1.0)
     */

    public static void syncEntityOnStartTracking(ServerPlayer player, LivingEntity living) {
        if (player == null || living == null || living.level().isClientSide()) return;
        if (isOmniInnate(living)) {
            com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, living, Collections.emptyList(), -2);
            return;
        }
        EnumSet<Element> innateSet = getInnateElements(living);
        Collection<Element> elements = getEffectiveElements(living, getEntityElements(living.getUUID()));
        if (!elements.isEmpty()) {
            boolean hasPermInnate = false;
            for (Element inn : innateSet) {
                if (elements.contains(inn)) {
                    hasPermInnate = true;
                    break;
                }
            }
            int duration = hasPermInnate ? -1 : getEntityMaxDurationTicks(living.getUUID());
            com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, living, elements, duration);
        }
    }

    /**
     * Genshin Impact Spacebar Struggle: allows a Frozen player to mash Space to shave off freeze duration faster.
     */

    public static void clearQuickenState(UUID id) {
        QUICKEN_GAUGES.remove(id);
        QUICKEN_UNTIL.remove(id);
    }

    /**
     * Checks if the entity is physically submerged in water blocks/fluid (excluding rain on dry land).
     * Used for water surface freezing, electrified water zones, and underwater voltaic arcing.
     */

    /**
     * Checks if the entity is exposed to environmental Hydro (submerged in water, bubble column, or standing in rain).
     */

    /**
     * Checks whether the entity is physically standing in or touching an environmental fire source
     * (Lava, Fire/Soul Fire block, lit Campfire/Soul Campfire, or Magma Block) rather than merely
     * having residual Vanilla afterburn ticks from a weapon or staff hit.
     */

    /**
     * Silently adjusts an entity's health (e.g. Furina's Salon Solitaire HP offer or Neuvillette's beam)
     * without triggering any hurt sound, red hurt flash, camera tilt, or knockback impulse.
     */

    public static GaugeState createAuraGauge(LivingEntity target, Element elem, float appliedUnits) {
        float scale = ElementalResonanceManager.getPlayerIncomingAuraDurationScale(target, elem);
        return GaugeState.createWithDurationScale(elem, appliedUnits, scale);
    }

    public static void refreshAuraGauge(LivingEntity target, GaugeState existing, Element elem, float appliedUnits) {
        float scale = ElementalResonanceManager.getPlayerIncomingAuraDurationScale(target, elem);
        existing.refresh(appliedUnits, scale);
    }

    public static void notifyResonanceReaction(ElementalReaction reaction, @Nullable ServerPlayer attacker, long now) {
        if (attacker == null || reaction == ElementalReaction.NONE || reaction == ElementalReaction.IMMUNE) return;
        triggerHiddenReactionAdvancements(attacker, reaction);
        switch (reaction) {
            case OVERLOADED, OVERVAPE, SUPERCONDUCT, ELECTRO_CHARGED ->
                    ElementalResonanceManager.onElectroReactionTriggered(attacker, now);
            case BURNING, BLOOM, REVERSE_BLOOM, SPREAD ->
                    ElementalResonanceManager.onDendroReactionTriggered(attacker, now);
            case QUICKEN, AGGRAVATE -> {
                ElementalResonanceManager.onElectroReactionTriggered(attacker, now);
                ElementalResonanceManager.onDendroReactionTriggered(attacker, now);
            }
            default -> {}
        }
    }

    public static float getAmplifyingMultiplier(int emLevel) {
        return 1.0F + 0.15F * Math.max(0, emLevel);
    }

    public static float getTransformativeMultiplier(int emLevel) {
        return 1.0F + 0.30F * Math.max(0, emLevel);
    }

    public static void triggerHiddenReactionAdvancements(@Nullable ServerPlayer attacker, @Nullable ElementalReaction reaction) {
        if (attacker == null || reaction == null || reaction == ElementalReaction.NONE || reaction == ElementalReaction.IMMUNE) {
            return;
        }
        switch (reaction) {
            case OVERLOADED, OVERVAPE, FORWARD_VAPORIZE, REVERSE_VAPORIZE ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_PYRO);
            case ELECTRO_CHARGED, SUPERCONDUCT ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_ELECTRO);
            case FROZEN, SHATTER, FORWARD_MELT, REVERSE_MELT ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_CRYO);
            case CRYSTALLIZE ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_GEO);
            case BLOOM, REVERSE_BLOOM, BURNING, QUICKEN, AGGRAVATE, SPREAD ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_DENDRO);
            case SWIRL, STELLAR_SWIRL ->
                    com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_ANEMO);
            default -> {}
        }
        if (attacker.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                && staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.OMNI) {
            com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_HIDDEN_OMNI);
        }
    }

    public static float getCatalyzeMultiplier(int emLevel) {
        return 1.0F + 0.25F * Math.max(0, emLevel);
    }

    /**
     * Handles environmental damage types and innate elemental mob attacks.
     */

    /**
     * Pre-damage hook for environmental fire damage (sunlight burning on Stray, fire blocks, lava, magma).
     * Amplifies incoming fire damage when Forward Melt (2.0x) or Reverse Vaporize (1.5x) is triggered.
     */

    /**
     * Maintains Pyro 1U aura on burning entities in tick() without prematurely stealing Melt/Vaporize before the fire damage tick.
     */

    /**
     * Handles environmental fire damage (lava, fire block, campfire, magma, burning) on entities.
     */

    /**
     * Handles environmental water/rain on entities holding active auras.
     */

    /**
     * Returns all permanent innate elements held by the entity (empty for Omni or non-innate).
     * A Mutated Creeper struck by lightning retains both its mutated element and Charged Electro.
     */

    /**
     * Rolls fixed elemental essence drops on death for Hostile Enemies killed by a player
     * (no Looting scaling, split tiny/medium Slimes and Magma Cubes excluded):
     * Omni Mutated 25%, Regular Mutated 15%, Natural Innate 5%.
     */


    private static long lastVisualBudgetTick = -1L;
    private static final Map<Long, Integer> REACTION_VISUAL_CELL_BUDGET = new ConcurrentHashMap<>();

    /**
     * Spatial per-tick particle/sound budget per 8x8 block cell to prevent lag spikes when many mobs react simultaneously.
     */
    public static boolean canEmitReactionVisual(@Nullable ServerLevel level, @Nullable Vec3 pos, int maxPerCellPerTick) {
        if (level == null || pos == null) return true;
        long now = level.getGameTime();
        if (now != lastVisualBudgetTick) {
            lastVisualBudgetTick = now;
            REACTION_VISUAL_CELL_BUDGET.clear();
        }
        int cx = ((int) Math.floor(pos.x)) >> 3;
        int cz = ((int) Math.floor(pos.z)) >> 3;
        long cellKey = (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
        int count = REACTION_VISUAL_CELL_BUDGET.getOrDefault(cellKey, 0);
        if (count >= maxPerCellPerTick) {
            return false;
        }
        REACTION_VISUAL_CELL_BUDGET.put(cellKey, count + 1);
        return true;
    }

    public static boolean checkStandardIcd(LivingEntity target, Element element, @Nullable ServerPlayer attacker, long now) {
        int attackerKey = attacker != null ? attacker.getId() : 0;
        long key = (((long) attackerKey) << 36) | (((long) target.getId()) << 4) | (element.ordinal() & 0xFL);

        IcdEntry entry = ICD_TRACKER.get(key);
        if (entry == null || now < entry.firstHitTime || (now - entry.firstHitTime) >= 50) {
            ICD_TRACKER.put(key, new IcdEntry(1, now));
            return true;
        }

        entry.hitCount++;
        return (entry.hitCount % 3 == 1);
    }

    public static int getReactionCooldownTicks(ElementalReaction reaction) {
        return switch (reaction) {
            case OVERLOADED, SUPERCONDUCT, STELLAR_CONDUCT, SHATTER, SWIRL, STELLAR_SWIRL -> 10; // 0.5s (10 ticks)
            case ELECTRO_CHARGED -> 10; // 0.5s (10 ticks)
            case BLOOM, REVERSE_BLOOM, HYPERBLOOM, BURGEON -> 10; // 0.5s (10 ticks)
            case CRYSTALLIZE -> 20; // 1.0s (20 ticks - Genshin shard drop cooldown)
            case FROZEN -> 10; // 0.5s (10 ticks)
            case BURNING -> 40; // 2.0s
            case AGGRAVATE, SPREAD -> 5; // 0.25s
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, FORWARD_MELT, REVERSE_MELT, OVERVAPE -> 8; // 0.4s
            default -> 5;
        };
    }

    public static boolean checkReactionIcd(LivingEntity target, ElementalReaction reaction, long now) {
        long key = (((long) target.getId()) << 8) | (reaction.ordinal() & 0xFFL);
        Long lastTrigger = REACTION_ICD_TRACKER.get(key);
        int cdTicks = getReactionCooldownTicks(reaction);
        if (lastTrigger != null && now >= lastTrigger && (now - lastTrigger) < cdTicks) {
            return false;
        }
        REACTION_ICD_TRACKER.put(key, now);
        return true;
    }

    /**
     * Resolves an elemental attack hitting a target entity.
     */
    public static void applyElementBypassIcd(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, true);
    }

    public static void applyElement(
            LivingEntity target,
            Element attackElement,
            @Nullable Player attacker,
            float baseDamage,
            float attackU
    ) {
        if (target == null || !(target.level() instanceof ServerLevel level)) return;
        ServerPlayer sp = attacker instanceof ServerPlayer s ? s : null;
        applyElement(level, target, attackElement, attackU, sp, baseDamage, false, null);
    }

    public static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, false, null);
    }

    public static void applyElementWithCredit(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable LivingEntity creditAttacker,
            float baseDamage
    ) {
        LivingEntity prev = CURRENT_REACTION_ATTACKER.get();
        if (creditAttacker != null) {
            CURRENT_REACTION_ATTACKER.set(creditAttacker);
        }
        try {
            ServerPlayer playerAttacker = creditAttacker instanceof ServerPlayer sp ? sp : null;
            applyElement(level, target, attackElement, attackU, playerAttacker, baseDamage, false, null);
        } finally {
            if (prev == null) CURRENT_REACTION_ATTACKER.remove();
            else CURRENT_REACTION_ATTACKER.set(prev);
        }
    }

    public static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, bypassStandardIcd, null);
    }

    /**
     * Evaluates an imbued melee sword hit against a target inside incoming-damage handling.
     * Applies 1.0U and triggers EGT reactions only on full-strength swings (>= 0.9F scale);
     * returns the final incoming sword damage (amplified/boosted by reactions, or unchanged
     * physical damage when immune / below full charge).
     */

    public static float dealOrDeferDamage(
            ServerLevel level,
            LivingEntity target,
            @Nullable ServerPlayer attacker,
            float damage,
            Element attackElement,
            float @Nullable [] meleeOut
    ) {
        return dealOrDeferDamage(level, target, attacker, damage, attackElement, meleeOut, null);
    }

    public static float dealOrDeferDamage(
            ServerLevel level,
            LivingEntity target,
            @Nullable ServerPlayer attacker,
            float damage,
            Element attackElement,
            float @Nullable [] meleeOut,
            @Nullable ElementalReaction reaction
    ) {
        if (meleeOut != null) {
            meleeOut[0] = damage;
            return damage * getEffectivePhysicalMultiplier(target, level.getGameTime());
        }
        float resDamage = applyMobElementalResistance(target, attackElement, damage);
        if (resDamage > 0.0F) {
            if (reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
                ElementalDeathMessageHelper.recordReactionForDeath(target, reaction, attackElement, attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get());
                triggerHiddenReactionAdvancements(attacker != null ? attacker : (CURRENT_REACTION_ATTACKER.get() instanceof ServerPlayer spAtt ? spAtt : null), reaction);
            }
            hurtWithPreAppliedElementalRes(target, getElementalDamageSource(level, attacker, reaction, attackElement), resDamage);
        }
        return resDamage;
    }

    public static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd,
            float @Nullable [] meleeOut
    ) {
        if (!target.isAlive() || target.isInvulnerable()) return;
        int depth = REACTION_DEPTH.get();
        if (depth >= MAX_REACTION_DEPTH) {
            if (meleeOut != null) meleeOut[0] = baseDamage;
            return;
        }
        REACTION_DEPTH.set(depth + 1);
        LivingEntity prevAttacker = CURRENT_REACTION_ATTACKER.get();
        CURRENT_REACTION_ATTACKER.set(attacker != null ? attacker : prevAttacker);
        try {
            applyElementGuarded(level, target, attackElement, attackU, attacker, baseDamage, bypassStandardIcd, meleeOut);
        } finally {
            if (prevAttacker == null) CURRENT_REACTION_ATTACKER.remove();
            else CURRENT_REACTION_ATTACKER.set(prevAttacker);
            REACTION_DEPTH.set(depth);
        }
    }

    public static void applyElementGuarded(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd,
            float @Nullable [] meleeOut
    ) {

        boolean physicalMelee = meleeOut != null;
        if (meleeOut != null) {
            meleeOut[0] = baseDamage;
        }

        if (!physicalMelee && !Boolean.TRUE.equals(com.velorise.staves.entity.GeoFangPillarEntity.IS_GEO_PULSE.get())) {
            triggerMobAggro(level, target, attacker);
        }

        boolean egtEnabled = StavesConfig.ENABLE_ELEMENTAL_GAUGE.get();
        double dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();
        float effectiveDamage = physicalMelee ? baseDamage : (float) (baseDamage * dmgMult);

        if (!physicalMelee && target instanceof ServerPlayer sp && effectiveDamage > 0.0F) {
            Element activeShield = CrystallizeShardManager.getActiveShield(sp, level.getGameTime());
            if (activeShield == attackElement) {
                effectiveDamage *= 0.4F;
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        Element preThornsAura = pickDefenderAuraForThorns(target);
        Element thornsContext = preThornsAura != null ? preThornsAura : attackElement;

        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            enforceFrozenPoseIfFrozen(target);
            com.velorise.staves.network.ModNetwork.sendAuraSync(target, Collections.emptyList(), attackElement, attackElement, ElementalReaction.IMMUNE, 0.0F, -2);
            return;
        }

        Element innateElement = getInnateElement(target);
        if (innateElement != null && attackElement == innateElement) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>());
            if (innateElement.isAura() || isMutatedInnate(target)) {
                targetGauges.putIfAbsent(innateElement, new GaugeState(innateElement, 1.25F));
            }
            enforceFrozenPoseIfFrozen(target);
            syncAuras(target, targetGauges.keySet(), innateElement, attackElement, ElementalReaction.IMMUNE);
            if (!physicalMelee && attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, level.getGameTime(), innateElement);
            }
            return;
        }

        if (attackElement == Element.PYRO && isPyroResistant(target)) {
            if (physicalMelee) {
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
                enforceFrozenPoseIfFrozen(target);
                syncAuras(target, getEntityElements(target.getUUID()), null, Element.PYRO, ElementalReaction.IMMUNE);
                return;
            }
            baseDamage = 0.0F;
            effectiveDamage = 0.0F;
        } else if (attackElement == Element.PYRO && !hasElement(target, Element.ELECTRO) && !hasInnateElement(target, Element.ELECTRO)) {
            ensurePreDeathPyroIgnite(target, 3);
        }

        DendroCoreManager.checkReactionsNear(level, target.position(), 3.5D, attackElement, attacker);
        if (attackElement == Element.PYRO) {
            EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, target.position(), 1.5D);
        }

        if (!physicalMelee) {
            effectiveDamage = ElementalResonanceManager.applyOffensiveElementalResonance(
                    level, target, attackElement, attacker, effectiveDamage);
            if (attacker != null) {
                effectiveDamage = TsaritsaManager.applyStellarAndCrownMultipliers(attacker, target, attackElement, effectiveDamage);
            }
        }

        DamageSource elemDmgSource = getElementalDamageSource(level, attacker, null, attackElement);

        if (!egtEnabled) {
            if (physicalMelee) {
                meleeOut[0] = effectiveDamage;
                return;
            }
            float resDmg = applyMobElementalResistance(target, attackElement, effectiveDamage);
            Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
            THORNS_CONTEXT_ELEMENT.set(thornsContext);
            try {
                hurtWithPreAppliedElementalRes(target, elemDmgSource, resDmg);
                enforceFrozenPoseIfFrozen(target);
            } finally {
                if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
                else THORNS_CONTEXT_ELEMENT.set(prevCtx);
            }
            if (attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, level.getGameTime(), thornsContext);
            }
            return;
        }

        long now = level.getGameTime();

        boolean canApply = bypassStandardIcd || checkStandardIcd(target, attackElement, attacker, now);
        if (!canApply) {
            if (physicalMelee) {
                meleeOut[0] = effectiveDamage;
                return;
            }
            float resDmg = applyMobElementalResistance(target, attackElement, effectiveDamage);
            if (resDmg > 0.0F) {
                Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
                THORNS_CONTEXT_ELEMENT.set(thornsContext);
                try {
                    hurtWithPreAppliedElementalRes(target, elemDmgSource, resDmg);
                    enforceFrozenPoseIfFrozen(target);
                } finally {
                    if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
                    else THORNS_CONTEXT_ELEMENT.set(prevCtx);
                }
                syncAuras(target, getEntityElements(target.getUUID()), null, attackElement, null, resDmg);
            }
            if (attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, now, thornsContext);
            }
            return;
        }

        boolean prevReaction = IS_PROCESSING_REACTION.get();
        Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
        IS_PROCESSING_REACTION.set(true);
        THORNS_CONTEXT_ELEMENT.set(thornsContext);
        try {
            applyElementInternal(level, target, attackElement, attackU, attacker, baseDamage, effectiveDamage, dmgMult, now, meleeOut);
            enforceFrozenPoseIfFrozen(target);
        } finally {
            if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
            else THORNS_CONTEXT_ELEMENT.set(prevCtx);
            IS_PROCESSING_REACTION.set(prevReaction);
        }
        if (!physicalMelee && attacker != null && attacker != target && target.isAlive()
                && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
            reflectThornsElement(level, target, attacker, now, thornsContext);
        }
    }

    public static void applyElementInternal(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            float effectiveDamage,
            double dmgMult,
            long now,
            float @Nullable [] meleeOut
    ) {
        UUID targetId = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());
        int emLevel = getElementalMasteryLevel(attacker);
        float ampEmMult = getAmplifyingMultiplier(emLevel);
        float transEmMult = getTransformativeMultiplier(emLevel);
        float catEmMult = getCatalyzeMultiplier(emLevel);

        EnumSet<Element> innateElements = getInnateElements(target);
        boolean hasInnate = !innateElements.isEmpty();
        if (hasInnate) {
            if (innateElements.contains(attackElement)) {
                if ((attackElement.isAura() || isMutatedInnate(target)) && !targetGauges.containsKey(attackElement)) {
                    targetGauges.put(attackElement, new GaugeState(attackElement, 1.25F));
                }
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
                syncAuras(target, targetGauges.keySet(), attackElement, attackElement, ElementalReaction.IMMUNE);
                return;
            }

            if (innateElements.contains(Element.ANEMO) && (attackElement == Element.PYRO || attackElement == Element.HYDRO || attackElement == Element.ELECTRO || attackElement == Element.CRYO)) {
                if (checkReactionIcd(target, ElementalReaction.SWIRL, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.SWIRL);
                    float removedU = Math.min(1.0F, attackU * 0.5F);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, attackElement, removedU, attacker, dmgMult);
                    enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    syncAuras(target, targetGauges.keySet(), attackElement, Element.ANEMO, ElementalReaction.SWIRL, resDmg);
                    return;
                }
            } else if (innateElements.contains(Element.GEO) && (attackElement == Element.PYRO || attackElement == Element.HYDRO || attackElement == Element.ELECTRO || attackElement == Element.CRYO)) {
                if (checkReactionIcd(target, ElementalReaction.CRYSTALLIZE, now)) {
                    executeReactionEffect(level, target, ElementalReaction.CRYSTALLIZE, attackElement, 0.5F, attacker, dmgMult);
                }
                if (attackElement == Element.PYRO && (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava())) {
                    if (targetGauges.containsKey(Element.PYRO)) {
                        targetGauges.get(Element.PYRO).refresh(attackU);
                    } else {
                        targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, attackU));
                    }
                }
                if (!targetGauges.containsKey(Element.DENDRO) && innateElements.size() == 1) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.CRYSTALLIZE);
                    enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    syncAuras(target, targetGauges.keySet(), attackElement, Element.GEO, ElementalReaction.CRYSTALLIZE, resDmg);
                    return;
                }
            } else if (innateElements.contains(Element.GEO) && attackElement == Element.DENDRO && innateElements.size() == 1) {
                if (targetGauges.containsKey(Element.DENDRO)) {
                    targetGauges.get(Element.DENDRO).refresh(attackU);
                } else {
                    targetGauges.put(Element.DENDRO, new GaugeState(Element.DENDRO, attackU));
                }
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                syncAuras(target, targetGauges.keySet(), Element.GEO, Element.DENDRO, null, resDmg);
                return;
            }

            for (Element ie : innateElements) {
                if ((ie.isAura() || isMutatedInnate(target)) && !targetGauges.containsKey(ie)) {
                    targetGauges.put(ie, new GaugeState(ie, 1.25F));
                }
            }
        }

        if (isEntityInWater(target)) {
            if (targetGauges.remove(Element.PYRO) != null || BURNING_UNTIL.remove(targetId) != null) {
                LAST_BURN_PYRO_REFRESH.remove(targetId);
                target.clearFire();
            }
            if (!hasInnate && !targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, createAuraGauge(target, Element.HYDRO, 1.25F));
            }
        } else if (isTouchingEnvironmentalFireBlock(target)) {
            if (!hasInnate && !targetGauges.containsKey(Element.ELECTRO)) {
                if (!targetGauges.containsKey(Element.PYRO)) {
                    targetGauges.put(Element.PYRO, createAuraGauge(target, Element.PYRO, 1.25F));
                } else {
                    GaugeState existing = targetGauges.get(Element.PYRO);
                    if (existing != null) {
                        refreshAuraGauge(target, existing, Element.PYRO, 1.25F);
                    }
                }
            }
        } else if (isExposedToEnvironmentalCryo(target)) {
            if (!hasInnate && !targetGauges.containsKey(Element.CRYO)) {
                targetGauges.put(Element.CRYO, createAuraGauge(target, Element.CRYO, 1.25F));
            }
        }

        GaugeState quickenGauge = QUICKEN_GAUGES.get(targetId);
        Long quickenEnd = QUICKEN_UNTIL.get(targetId);
        boolean hasQuicken = (quickenGauge != null && !quickenGauge.isExpired()) && (quickenEnd == null || now < quickenEnd);
        ElementalReaction catalyzeReaction = null;

        if (hasQuicken) {
            if (attackElement == Element.ELECTRO) {
                if (checkReactionIcd(target, ElementalReaction.AGGRAVATE, now)) {
                    float bonus = (float) (1.0F * dmgMult) * catEmMult;
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.AGGRAVATE;
                    notifyResonanceReaction(ElementalReaction.AGGRAVATE, attacker, now);
                    NahidaManager.onElementalReactionOrCoreDamage(level, target, attacker);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                    // ParticleTypes.ELECTRIC_SPARK removed
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
                }
                if (!targetGauges.containsKey(Element.DENDRO) && !targetGauges.containsKey(Element.HYDRO) && !targetGauges.containsKey(Element.PYRO) && !targetGauges.containsKey(Element.CRYO)) {
                    if (!hasInnate) {
                        if (targetGauges.containsKey(Element.ELECTRO)) {
                            refreshAuraGauge(target, targetGauges.get(Element.ELECTRO), Element.ELECTRO, attackU);
                        } else {
                            targetGauges.put(Element.ELECTRO, createAuraGauge(target, Element.ELECTRO, attackU));
                        }
                    }
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, catalyzeReaction);
                    syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, catalyzeReaction, resDmg);
                    return;
                }
            } else if (attackElement == Element.DENDRO) {
                if (checkReactionIcd(target, ElementalReaction.SPREAD, now)) {
                    float bonus = (float) (1.25F * dmgMult) * catEmMult;
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.SPREAD;
                    notifyResonanceReaction(ElementalReaction.SPREAD, attacker, now);
                    NahidaManager.onElementalReactionOrCoreDamage(level, target, attacker);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                    level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.05D);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9F, 1.4F);
                }
                if (!targetGauges.containsKey(Element.ELECTRO) && !targetGauges.containsKey(Element.HYDRO) && !targetGauges.containsKey(Element.PYRO)) {
                    if (!hasInnate) {
                        if (targetGauges.containsKey(Element.DENDRO)) {
                            targetGauges.get(Element.DENDRO).refresh(attackU);
                        } else {
                            targetGauges.put(Element.DENDRO, new GaugeState(Element.DENDRO, attackU));
                        }
                    }
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, catalyzeReaction);
                    syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.DENDRO, catalyzeReaction, resDmg);
                    return;
                }
            } else if (attackElement == Element.PYRO || attackElement == Element.HYDRO) {
                if (!targetGauges.containsKey(Element.DENDRO) && quickenGauge != null) {
                    targetGauges.put(Element.DENDRO, quickenGauge);
                }
            }
        }

        GaugeState freezeGauge = FROZEN_GAUGES.get(targetId);
        Long frozenEnd = FROZEN_UNTIL.get(targetId);
        boolean isFrozen = (freezeGauge != null && !freezeGauge.isExpired()) && (frozenEnd == null || now < frozenEnd);

        if (isFrozen && freezeGauge != null) {
            if (attackElement == Element.GEO || meleeOut != null) {
                if (!checkReactionIcd(target, ElementalReaction.SHATTER, now)) {
                    if (attackElement == Element.GEO) {
                        float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                        if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                        return;
                    }
                } else {
                    freezeGauge.consume(8.0F);
                    if (freezeGauge.isExpired()) {
                        clearFreezeState(target, targetId);
                    }
                    float bonus = (float) (10.0F * dmgMult) * transEmMult;
                    effectiveDamage += bonus;
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 0.8D, target.getZ(), 30, 0.35D, 0.4D, 0.35D, 0.06D);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                    if (attacker != null) {
                        com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                    }

                    if (attackElement == Element.GEO) {
                        float resShatter = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.SHATTER);
                        Element underlying = null;
                        if (targetGauges.containsKey(Element.CRYO)) underlying = Element.CRYO;
                        else if (targetGauges.containsKey(Element.HYDRO)) underlying = Element.HYDRO;
                        else if (innateElements.contains(Element.CRYO)) underlying = Element.CRYO;
                        else if (innateElements.contains(Element.HYDRO)) underlying = Element.HYDRO;

                        if (underlying != null && checkReactionIcd(target, ElementalReaction.CRYSTALLIZE, now)) {
                            GaugeState underAura = targetGauges.get(underlying);
                            float underBefore = underAura != null ? underAura.getUnits() : 1.25F;
                            float consumed = attackU * ElementalReaction.CRYSTALLIZE.getConsumptionModifier();
                            float removedU = Math.min(underBefore, consumed);
                            if (underAura != null) {
                                underAura.consume(consumed);
                                if (underAura.isExpired()) {
                                    targetGauges.remove(underlying);
                                }
                            }
                            executeReactionEffect(level, target, ElementalReaction.CRYSTALLIZE, underlying, removedU, attacker, dmgMult);
                        }
                        enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                        syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER, resShatter);
                        return;
                    }
                    if (findPrimaryReactiveAura(targetGauges, attackElement) == null) {
                        if (attackElement.isAura() && innateElements.isEmpty()) {
                            targetGauges.put(attackElement, createAuraGauge(target, attackElement, attackU));
                        }
                        float resShatter = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.SHATTER);
                        enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                        syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER, resShatter);
                        return;
                    }
                }
            } else if (attackElement == Element.PYRO) {
                if (!checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                freezeGauge.consume(attackU * 2.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float meltDamage = effectiveDamage * 2.0F * ampEmMult;
                float resMelt = dealOrDeferDamage(level, target, attacker, meltDamage, attackElement, meleeOut, ElementalReaction.FORWARD_MELT);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.3D, 0.35D, 0.3D, 0.04D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, resMelt);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                return;
            } else if (attackElement == Element.ELECTRO) {
                if (!checkReactionIcd(target, ElementalReaction.SUPERCONDUCT, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                freezeGauge.consume(attackU * 1.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.SUPERCONDUCT);
                executeReactionEffect(level, target, ElementalReaction.SUPERCONDUCT, Element.CRYO, attackU, attacker, dmgMult);
                enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, resDmg);
                return;
            } else if (attackElement == Element.ANEMO) {
                if (checkReactionIcd(target, ElementalReaction.SWIRL, now)) {
                    float freezeBefore = freezeGauge.getUnits();
                    float removedFreezeU = Math.min(freezeBefore, attackU * 0.5F);
                    freezeGauge.consume(attackU * 0.5F);
                    if (freezeGauge.isExpired()) {
                        clearFreezeState(target, targetId);
                    }
                    float remainingAnemoU = Math.max(0.0F, attackU - (freezeBefore / 0.5F));
                    if (remainingAnemoU > 0.01F && targetGauges.containsKey(Element.HYDRO)) {
                        GaugeState hydro = targetGauges.get(Element.HYDRO);
                        float hydroBefore = hydro.getUnits();
                        float removedHydroU = Math.min(hydroBefore, remainingAnemoU * 0.5F);
                        hydro.consume(remainingAnemoU * 0.5F);
                        if (hydro.isExpired()) targetGauges.remove(Element.HYDRO);
                        executeReactionEffect(level, target, ElementalReaction.SWIRL, Element.HYDRO, removedHydroU, attacker, dmgMult);
                    }
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, ElementalReaction.SWIRL);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, Element.CRYO, removedFreezeU, attacker, dmgMult);
                    enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ANEMO, ElementalReaction.SWIRL, resDmg);
                    return;
                }
            }
        }

        if (attackElement == Element.PYRO && targetGauges.containsKey(Element.HYDRO) && targetGauges.containsKey(Element.ELECTRO)) {
            if (!checkReactionIcd(target, ElementalReaction.OVERVAPE, now)) {
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                return;
            }
            GaugeState electro = targetGauges.get(Element.ELECTRO);
            GaugeState hydro = targetGauges.get(Element.HYDRO);
            float electroBefore = electro.getUnits();
            electro.consume(attackU * 1.0F);
            if (electro.isExpired()) targetGauges.remove(Element.ELECTRO);

            float remainingPyroU = Math.max(0.0F, attackU - electroBefore);
            float vapeConsumed = Math.max(0.25F, remainingPyroU > 0.0F ? remainingPyroU * 0.5F : attackU * 0.5F);
            hydro.consume(vapeConsumed);
            if (hydro.isExpired()) targetGauges.remove(Element.HYDRO);

            float overvapeDmg = effectiveDamage * 1.5F * ampEmMult;
            float resOvervape = dealOrDeferDamage(level, target, attacker, overvapeDmg, attackElement, meleeOut, ElementalReaction.OVERVAPE);
            triggerOverloadExplosion(level, target, attacker, dmgMult);
            notifyResonanceReaction(ElementalReaction.OVERVAPE, attacker, now);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            enforceInnateLock(target, targetGauges, targetId, innateElements, now);
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, attackElement, ElementalReaction.OVERVAPE, resOvervape);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
            return;
        }

        // 4. Reaction Resolution with Simultaneous Reaction Priority (Electro -> Pyro -> Hydro -> Cryo -> Dendro)
        Element auraElement = findPrimaryReactiveAura(targetGauges, attackElement);
        if (auraElement != null && auraElement != attackElement) {
            ElementalReaction reaction = determineReaction(auraElement, attackElement);
            if (attacker != null && TsaritsaManager.hasTsaritsaInParty(attacker)) {
                if (reaction == ElementalReaction.SUPERCONDUCT) {
                    reaction = ElementalReaction.STELLAR_CONDUCT;
                } else if (reaction == ElementalReaction.SWIRL && (auraElement == Element.CRYO || attackElement == Element.CRYO)) {
                    reaction = ElementalReaction.STELLAR_SWIRL;
                }
            }
            if (reaction != ElementalReaction.NONE) {
                boolean alreadyBurning = reaction == ElementalReaction.BURNING && BURNING_UNTIL.containsKey(targetId);
                if (!alreadyBurning && !checkReactionIcd(target, reaction, now)) {
                    if (reaction == ElementalReaction.OVERLOADED) {
                        GaugeState blockedAura = targetGauges.get(auraElement);
                        if (blockedAura != null) {
                            blockedAura.consume(attackU * reaction.getConsumptionModifier());
                            if (blockedAura.isExpired()) {
                                targetGauges.remove(auraElement);
                            }
                        }
                        BURNING_UNTIL.remove(targetId);
                        BURNING_EM_LEVEL.remove(targetId);
                        BURNING_ATTACKER_UUID.remove(targetId);
                        LAST_BURN_PYRO_REFRESH.remove(targetId);
                        if (!target.isInLava()) {
                            target.clearFire();
                        }
                        enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    }
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                GaugeState aura = targetGauges.get(auraElement);

                if (reaction == ElementalReaction.ELECTRO_CHARGED) {
                    if (!hasInnate) {
                        GaugeState triggerState = targetGauges.get(attackElement);
                        if (triggerState == null) {
                            triggerState = createAuraGauge(target, attackElement, attackU);
                            targetGauges.put(attackElement, triggerState);
                        } else {
                            refreshAuraGauge(target, triggerState, attackElement, attackU);
                        }
                        aura.consume(0.4F);
                        triggerState.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        if (triggerState.isExpired()) targetGauges.remove(attackElement);
                    } else {
                        aura.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    }

                    ELECTRO_CHARGE_EM_LEVEL.put(targetId, emLevel);
                    float ecDmg = effectiveDamage + (float) (1.0F * dmgMult) * transEmMult;
                    float resEc = dealOrDeferDamage(level, target, attacker, ecDmg, attackElement, meleeOut, reaction);
                    executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                    ELECTRO_CHARGE_TICK.put(targetId, 0);
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, resEc);
                    return;
                }

                if (reaction == ElementalReaction.BURNING) {
                    boolean immuneToBurn = innateElements.contains(Element.PYRO) || target.fireImmune() || target instanceof Warden;
                    if (!immuneToBurn) {
                        long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                                target, 200L);
                        BURNING_UNTIL.put(targetId, now + burnTicks);
                        BURNING_EM_LEVEL.put(targetId, emLevel);
                        LivingEntity burnCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
                        if (burnCredit != null && burnCredit != target) {
                            BURNING_ATTACKER_UUID.put(targetId, burnCredit.getUUID());
                        }
                        if (!hasInnate || innateElements.contains(Element.DENDRO) || innateElements.contains(Element.GEO)) {
                            targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                            if (attackElement == Element.DENDRO && !innateElements.contains(Element.DENDRO)) {
                                if (targetGauges.containsKey(Element.DENDRO)) {
                                    refreshAuraGauge(target, targetGauges.get(Element.DENDRO), Element.DENDRO, attackU);
                                } else {
                                    targetGauges.put(Element.DENDRO, createAuraGauge(target, Element.DENDRO, attackU));
                                }
                            }
                        }
                        LAST_BURN_PYRO_REFRESH.put(targetId, now);
                        target.igniteForSeconds(5);
                    }
                    enforceInnateLock(target, targetGauges, targetId, innateElements, now);
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (!alreadyBurning) {
                        executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                        syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, resDmg);
                    } else {
                        syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    }
                    return;
                }

                float auraUnitsBefore = aura.getUnits();
                float consumed = (reaction == ElementalReaction.QUICKEN)
                        ? Math.min(auraUnitsBefore, attackU * 0.8F)
                        : attackU * reaction.getConsumptionModifier();
                float removedAuraU = Math.min(auraUnitsBefore, consumed);
                aura.consume(consumed);
                boolean auraDepleted = aura.isExpired();
                if (auraDepleted) {
                    targetGauges.remove(auraElement);
                    if (auraElement == Element.PYRO) {
                        BURNING_UNTIL.remove(targetId);
                        BURNING_EM_LEVEL.remove(targetId);
                        BURNING_ATTACKER_UUID.remove(targetId);
                        LAST_BURN_PYRO_REFRESH.remove(targetId);
                        if (!target.isInLava()) {
                            target.clearFire();
                        }
                    }
                }

                if (auraElement == Element.DENDRO && (reaction == ElementalReaction.BLOOM || reaction == ElementalReaction.REVERSE_BLOOM)) {
                    GaugeState qg = QUICKEN_GAUGES.get(targetId);
                    if (qg != null) {
                        qg.consume(consumed);
                        if (qg.isExpired()) {
                            clearQuickenState(targetId);
                        }
                    }
                }

                if (reaction == ElementalReaction.FROZEN) {
                    float freezeU = 2.0F * Math.min(auraUnitsBefore, attackU);
                    GaugeState newFreeze = GaugeState.createFrozen(Math.max(0.8F, freezeU));
                    int freezeTicks = newFreeze.getRemainingTicks();
                    if (!hasInnate && !isBossEntity(target)) {
                        applyFreezeState(target, targetId, newFreeze, freezeTicks, now);
                    }
                }

                if (reaction == ElementalReaction.QUICKEN) {
                    float postTaxAttackU = attackU * 0.8F;
                    float quickenU = Math.max(0.4F, Math.min(auraUnitsBefore, postTaxAttackU));
                    GaugeState newQuicken = GaugeState.createQuicken(quickenU);
                    GaugeState existingQuicken = QUICKEN_GAUGES.get(targetId);
                    if (existingQuicken == null || newQuicken.getRemainingTicks() > existingQuicken.getRemainingTicks()) {
                        QUICKEN_GAUGES.put(targetId, newQuicken);
                        QUICKEN_UNTIL.put(targetId, now + newQuicken.getRemainingTicks());
                    }

                    float leftoverTriggerU = postTaxAttackU - auraUnitsBefore;
                    if (leftoverTriggerU > 0.05F && !hasInnate) {
                        if (targetGauges.containsKey(attackElement)) {
                            targetGauges.get(attackElement).refresh(leftoverTriggerU);
                        } else {
                            targetGauges.put(attackElement, GaugeState.createLeftover(attackElement, leftoverTriggerU, attackU));
                        }
                    }
                }

                // Simultaneous Reaction Priority: if the first aura was completely consumed and trigger gauge remains,
                float neededTriggerU = reaction.getConsumptionModifier() > 0.0F
                        ? (auraUnitsBefore / reaction.getConsumptionModifier())
                        : attackU;
                float remainingAttackU = Math.max(0.0F, attackU - neededTriggerU);
                if (auraDepleted && remainingAttackU > 0.01F) {
                    Element secondaryAura = findPrimaryReactiveAura(targetGauges, attackElement);
                    if (secondaryAura != null && secondaryAura != auraElement) {
                        ElementalReaction secondaryReaction = determineReaction(secondaryAura, attackElement);
                        if (secondaryReaction != ElementalReaction.NONE) {
                            GaugeState aura2 = targetGauges.get(secondaryAura);
                            if (aura2 != null) {
                                float aura2Before = aura2.getUnits();
                                float consumed2 = remainingAttackU * secondaryReaction.getConsumptionModifier();
                                float removed2 = Math.min(aura2Before, consumed2);
                                aura2.consume(consumed2);
                                if (aura2.isExpired()) {
                                    targetGauges.remove(secondaryAura);
                                    if (secondaryAura == Element.PYRO) {
                                        BURNING_UNTIL.remove(targetId);
                                        BURNING_EM_LEVEL.remove(targetId);
                                        BURNING_ATTACKER_UUID.remove(targetId);
                                        LAST_BURN_PYRO_REFRESH.remove(targetId);
                                        if (!target.isInLava()) {
                                            target.clearFire();
                                        }
                                    }
                                }
                                if (secondaryReaction == ElementalReaction.FROZEN) {
                                    float freezeU2 = 2.0F * Math.min(aura2Before, remainingAttackU);
                                    GaugeState newFreeze2 = GaugeState.createFrozen(Math.max(0.8F, freezeU2));
                                    int freezeTicks2 = newFreeze2.getRemainingTicks();
                                    if (!hasInnate && !isBossEntity(target)) {
                                        applyFreezeState(target, targetId, newFreeze2, freezeTicks2, now);
                                    }
                                }
                                executeReactionEffect(level, target, secondaryReaction, secondaryAura, removed2, attacker, dmgMult);
                            }
                        }
                    }
                }

                if (reaction == ElementalReaction.REVERSE_VAPORIZE && isEntityInWater(target)) {
                    target.clearFire();
                }

                enforceInnateLock(target, targetGauges, targetId, innateElements, now);

                float reactionMult = reaction.getDamageMultiplier();
                if (reactionMult > 1.0F) {
                    reactionMult *= ampEmMult;
                }
                if ((reaction == ElementalReaction.STELLAR_CONDUCT || reaction == ElementalReaction.STELLAR_SWIRL) && attacker != null) {
                    reactionMult *= TsaritsaManager.getStellarReactionMultiplier(attacker);
                    if (reaction == ElementalReaction.STELLAR_SWIRL) {
                        reactionMult *= 1.15F;
                    }
                }
                float finalDmg = effectiveDamage * reactionMult;
                ElementalReaction displayReaction = (catalyzeReaction != null && reaction == ElementalReaction.QUICKEN)
                        ? catalyzeReaction : reaction;
                float resFinalDmg = 0.0F;
                if (finalDmg > 0.0F) {
                    resFinalDmg = dealOrDeferDamage(level, target, attacker, finalDmg, attackElement, meleeOut, displayReaction);
                }
                executeReactionEffect(level, target, reaction, auraElement, removedAuraU, attacker, dmgMult);

                syncAuras(target, targetGauges.keySet(), auraElement, attackElement, displayReaction, resFinalDmg);
                return;
            }
        }

        if (attackElement.isAura()) {
            if (attackElement == Element.PYRO && isEntityInWater(target)) {
                target.clearFire();
                float vapeDmg = effectiveDamage * 1.5F * ampEmMult;
                float resVape = dealOrDeferDamage(level, target, attacker, vapeDmg, attackElement, meleeOut, ElementalReaction.REVERSE_VAPORIZE);
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, resVape);
                return;
            }

            if (hasInnate && !innateElements.contains(attackElement)) {
                float resDmg = 0.0F;
                if (effectiveDamage > 0.0F) {
                    resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, catalyzeReaction);
                }
                syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, resDmg);
                return;
            }

            if (targetGauges.containsKey(attackElement)) {
                GaugeState existing = targetGauges.get(attackElement);
                refreshAuraGauge(target, existing, attackElement, attackU);
            } else {
                GaugeState newAura = createAuraGauge(target, attackElement, attackU);
                targetGauges.put(attackElement, newAura);
            }
            float previewResDmg = (meleeOut != null)
                    ? effectiveDamage * getEffectivePhysicalMultiplier(target, now)
                    : applyMobElementalResistance(target, attackElement, effectiveDamage);
            syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, previewResDmg);
            if (effectiveDamage > 0.0F) {
                dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, catalyzeReaction);
            }
            return;
        }

        float resNormalDmg = 0.0F;
        if (effectiveDamage > 0.0F) {
            resNormalDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut, catalyzeReaction);
        }
        syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, resNormalDmg);
    }

    public static void enforceInnateLock(
            @Nullable LivingEntity target,
            Map<Element, GaugeState> targetGauges,
            UUID targetId,
            EnumSet<Element> innateElements,
            long now
    ) {
        if (innateElements == null || innateElements.isEmpty()) return;
        boolean isBurning = target != null && (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava() || BURNING_UNTIL.containsKey(targetId));
        targetGauges.keySet().removeIf(e -> !innateElements.contains(e)
                && !(innateElements.contains(Element.GEO) && e == Element.DENDRO)
                && !(isBurning && e == Element.PYRO));
        for (Element ie : innateElements) {
            if ((ie.isAura() || isMutatedInnate(target)) && !targetGauges.containsKey(ie)) {
                targetGauges.put(ie, new GaugeState(ie, 1.25F));
            }
        }
    }

    /**
     * Selects the primary aura to react with the incoming trigger element according to
     * Genshin Impact's Simultaneous Reaction Priority (Electro -> Pyro -> Hydro -> Cryo -> Dendro).
     */
    public static @Nullable Element findPrimaryReactiveAura(Map<Element, GaugeState> gauges, Element trigger) {
        for (Element priorityElem : REACTION_PRIORITY_ORDER) {
            if (priorityElem == trigger) continue;
            GaugeState gs = gauges.get(priorityElem);
            if (gs != null && !gs.isExpired() && determineReaction(priorityElem, trigger) != ElementalReaction.NONE) {
                return priorityElem;
            }
        }
        return null;
    }

    public static ElementalReaction determineReaction(Element aura, Element trigger) {
        if (aura == Element.PYRO) {
            if (trigger == Element.HYDRO) return ElementalReaction.FORWARD_VAPORIZE;
            if (trigger == Element.CRYO) return ElementalReaction.REVERSE_MELT;
            if (trigger == Element.ELECTRO) return ElementalReaction.OVERLOADED;
            if (trigger == Element.DENDRO) return ElementalReaction.BURNING;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.HYDRO) {
            if (trigger == Element.PYRO) return ElementalReaction.REVERSE_VAPORIZE;
            if (trigger == Element.CRYO) return ElementalReaction.FROZEN;
            if (trigger == Element.ELECTRO) return ElementalReaction.ELECTRO_CHARGED;
            if (trigger == Element.DENDRO) return ElementalReaction.REVERSE_BLOOM; // Dendro on Hydro = 2.0x consumption
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.CRYO) {
            if (trigger == Element.PYRO) return ElementalReaction.FORWARD_MELT;
            if (trigger == Element.HYDRO) return ElementalReaction.FROZEN;
            if (trigger == Element.ELECTRO) return ElementalReaction.SUPERCONDUCT;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.ELECTRO) {
            if (trigger == Element.PYRO) return ElementalReaction.OVERLOADED;
            if (trigger == Element.HYDRO) return ElementalReaction.ELECTRO_CHARGED;
            if (trigger == Element.CRYO) return ElementalReaction.SUPERCONDUCT;
            if (trigger == Element.DENDRO) return ElementalReaction.QUICKEN;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.DENDRO) {
            if (trigger == Element.PYRO) return ElementalReaction.BURNING;
            if (trigger == Element.HYDRO) return ElementalReaction.BLOOM; // Hydro on Dendro = 0.5x consumption
            if (trigger == Element.ELECTRO) return ElementalReaction.QUICKEN;
        }
        return ElementalReaction.NONE;
    }

    public static void executeReactionEffect(
            ServerLevel level,
            LivingEntity target,
            ElementalReaction reaction,
            Element trigger,
            float reactedU,
            @Nullable ServerPlayer attacker,
            double dmgMult
    ) {
        ReactionEffectManager.executeReactionEffectInternal(level, target, reaction, trigger, reactedU, attacker, dmgMult);
    }

public static void tick(MinecraftServer server) {
        flushPendingEvokerConstructs(server);
        for (ServerLevel lvl : server.getAllLevels()) {
            DendroCoreManager.tick(lvl);
            CrystallizeShardManager.tick(lvl);
            ElectrifiedWaterManager.tick(lvl);
            DragonfireCloudManager.tick(lvl);
            GhastfireFieldManager.tick(lvl);
        }
        EnvironmentalBlockManager.tick(server);
        MobHazardAvoidanceManager.tick(server);

        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            PlayerGenshinState.pruneInactive(server);
            ICD_TRACKER.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue().firstHitTime) > 100L);
            REACTION_ICD_TRACKER.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            SUPERCONDUCT_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L);
            INNATE_COOLDOWN_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 200L);
            LAST_PROXIMITY_SHOCK.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 40L);
            LAST_BLOOM_SPAWN.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 40L);
            LAST_THORNS_REFLECT.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_OMNI_ENV_IMMUNE.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_MUSHROOM_CONTACT.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            EXPLODED_CREEPERS.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_FREEZE_STRUGGLE.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            PLAYER_HP_CHANGE_BUFF_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 400L);
            QUICKEN_UNTIL.entrySet().removeIf(e -> !QUICKEN_GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            FROZEN_UNTIL.entrySet().removeIf(e -> !FROZEN_GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            PETRIFIED_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L);
            BURNING_UNTIL.entrySet().removeIf(e -> !GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            BURNING_EM_LEVEL.keySet().removeIf(id -> !BURNING_UNTIL.containsKey(id));
            BURNING_ATTACKER_UUID.keySet().removeIf(id -> !BURNING_UNTIL.containsKey(id));
            LAST_BURN_PYRO_REFRESH.keySet().removeIf(id -> !BURNING_UNTIL.containsKey(id));
            SUPPRESSED_PHYSICAL_FLOAT_TICK.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 20L);
            ElementalDeathMessageHelper.cleanupStale(serverNow);
            ELECTRO_CHARGE_EM_LEVEL.keySet().removeIf(id -> !GAUGES.containsKey(id));
            ELECTRO_CHARGE_TICK.keySet().removeIf(id -> !GAUGES.containsKey(id));
            FROZEN_POSES.keySet().removeIf(id -> !FROZEN_GAUGES.containsKey(id) && !FROZEN_UNTIL.containsKey(id));
        }
        if (serverNow % 600 == 0 && !OMNI_INNATE_ENTITIES.isEmpty()) {
            OMNI_INNATE_ENTITIES.removeIf(id -> {
                for (ServerLevel lvl : server.getAllLevels()) {
                    Entity e = lvl.getEntity(id);
                    if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                        return false;
                    }
                }
                return true;
            });
        }

        tickSourcewaterDroplets(server, serverNow);
        tickDaybreakVortices(server, serverNow);
        XilonenManager.tick(server, serverNow);
        PlungeAndGliderManager.tick(server, serverNow);
        com.velorise.staves.advancement.ModAdvancements.tickVentiWindLaunches(server, serverNow);
        NeuvilletteManager.tick(server, serverNow);
        NahidaManager.tick(server, serverNow);
        ZhongliManager.tick(server, serverNow);
        RaidenManager.tickIdle(server, serverNow);
        MavuikaManager.tickIdle(server, serverNow);
        VentiManager.tickIdle(server, serverNow);
        FurinaManager.tickIdle(server, serverNow);
        TsaritsaManager.tickIdle(server, serverNow);
        for (ServerLevel lvl : server.getAllLevels()) {
            TsaritsaManager.tickServer(lvl);
        }
        ElementalTickManager.tickActiveEntityGauges(server, serverNow);
        ElementalTickManager.tickActivePlayersAndEnvironment(server, serverNow);
    }

    public static record FrozenPoseState(double x, double y, double z, float yRot, float xRot, float yHeadRot, float yBodyRot) {}
    public static boolean isPhysicalDamage(DamageSource source) { return DamageResistanceManager.isPhysicalDamage(source); }
    public static boolean isElementalOrMagicDamage(DamageSource source) { return DamageResistanceManager.isElementalOrMagicDamage(source); }
    public static void notifyOmniImmuneEnvironmental(ServerLevel level, LivingEntity target) { InnateMobManager.notifyOmniImmuneEnvironmental(level, target); }
    public static Element chooseOmniMutatedAttackElement(LivingEntity target, LivingEntity attacker) { return InnateMobManager.chooseOmniMutatedAttackElement(target, attacker); }
    public static DamageSource getElementalDamageSource(ServerLevel level, @Nullable LivingEntity attacker) { return DamageResistanceManager.getElementalDamageSource(level, attacker); }
    public static DamageSource getElementalDamageSource( ServerLevel level, @Nullable LivingEntity attacker, @Nullable ElementalReaction reaction, @Nullable Element element ) { return DamageResistanceManager.getElementalDamageSource(level, attacker, reaction, element); }
    public static boolean isGeoInnateMob(@Nullable Entity entity) { return DamageResistanceManager.isGeoInnateMob(entity); }
    public static boolean isAnemoInnateMob(@Nullable Entity entity) { return DamageResistanceManager.isAnemoInnateMob(entity); }
    public static boolean isProjectileDamageSource(@Nullable DamageSource source) { return DamageResistanceManager.isProjectileDamageSource(source); }
    public static boolean isFallDamageImmunityApplicable(@Nullable LivingEntity entity, @Nullable DamageSource source) { return DamageResistanceManager.isFallDamageImmunityApplicable(entity, source); }
    public static void beginDamageFloatTracking(@Nullable LivingEntity entity) { DamageResistanceManager.beginDamageFloatTracking(entity); }
    public static void trySendPhysicalDamageFloat(@Nullable LivingEntity target, @Nullable DamageSource source, float finalDamage) { DamageResistanceManager.trySendPhysicalDamageFloat(target, source, finalDamage); }
    public static boolean isVanillaPhysicalDisplayDamage(DamageSource source) { return DamageResistanceManager.isVanillaPhysicalDisplayDamage(source); }
    public static float getMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element) { return DamageResistanceManager.getMobElementalResistance(target, element); }
    public static float getMobPhysicalResistance(@Nullable LivingEntity target) { return DamageResistanceManager.getMobPhysicalResistance(target); }
    public static float computeResistanceMultiplier(float res) { return DamageResistanceManager.computeResistanceMultiplier(res); }
    public static float getEffectivePhysicalMultiplier(LivingEntity target, long now) { return DamageResistanceManager.getEffectivePhysicalMultiplier(target, now); }
    public static float getEffectivePhysicalMultiplier(LivingEntity target, @Nullable DamageSource source, long now) { return DamageResistanceManager.getEffectivePhysicalMultiplier(target, source, now); }
    public static float getEffectiveElementalResistance(@Nullable LivingEntity target, @Nullable Element element, long now) { return DamageResistanceManager.getEffectiveElementalResistance(target, element, now); }
    public static float applyMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element, float damage) { return DamageResistanceManager.applyMobElementalResistance(target, element, damage); }
    public static @Nullable Element inferElementFromDamageSource(DamageSource source) { return DamageResistanceManager.inferElementFromDamageSource(source); }
    public static boolean hurtWithPreAppliedElementalRes(LivingEntity target, DamageSource source, float amount) { return DamageResistanceManager.hurtWithPreAppliedElementalRes(target, source, amount); }
    public static float applyMobDamageResistances(LivingEntity entity, DamageSource source, float amount) { return DamageResistanceManager.applyMobDamageResistances(entity, source, amount); }
    public static void triggerMobAggro(ServerLevel level, LivingEntity target, @Nullable LivingEntity attacker) { DamageResistanceManager.triggerMobAggro(level, target, attacker); }
    public static void enforceFrozenPoseIfFrozen(LivingEntity target) { FreezeStateManager.enforceFrozenPoseIfFrozen(target); }
    public static void applyFreezeState(LivingEntity target, UUID targetId, GaugeState newFreeze, int freezeTicks, long now) { FreezeStateManager.applyFreezeState(target, targetId, newFreeze, freezeTicks, now); }
    public static void enforceFrozenPose(LivingEntity target, UUID id) { FreezeStateManager.enforceFrozenPose(target, id); }
    public static int getFrozenRemainingTicks(UUID id, long now) { return FreezeStateManager.getFrozenRemainingTicks(id, now); }
    public static void tickEntityFreezeGuard(LivingEntity entity) { FreezeStateManager.tickEntityFreezeGuard(entity); }
    public static void clearFreezeState(LivingEntity target, UUID id) { FreezeStateManager.clearFreezeState(target, id); }
    public static void handlePlayerFreezeStruggle(ServerPlayer player) { FreezeStateManager.handlePlayerFreezeStruggle(player); }
    public static boolean isEntitySubmergedInWater(LivingEntity target) { return EnvironmentalAuraManager.isEntitySubmergedInWater(target); }
    public static boolean isEntityInWater(LivingEntity target) { return EnvironmentalAuraManager.isEntityInWater(target); }
    public static boolean isEntityInSnowWeather(@Nullable LivingEntity target) { return EnvironmentalAuraManager.isEntityInSnowWeather(target); }
    public static boolean isExposedToEnvironmentalCryo(@Nullable LivingEntity target) { return EnvironmentalAuraManager.isExposedToEnvironmentalCryo(target); }
    public static void handleEnvironmentalCryo(LivingEntity target) { EnvironmentalAuraManager.handleEnvironmentalCryo(target); }
    public static boolean isTouchingSoulCampfire(LivingEntity target) { return EnvironmentalAuraManager.isTouchingSoulCampfire(target); }
    public static boolean isTouchingEnvironmentalFireBlock(LivingEntity target) { return EnvironmentalAuraManager.isTouchingEnvironmentalFireBlock(target); }
    public static boolean isTouchingMushroomBlock(LivingEntity target) { return EnvironmentalAuraManager.isTouchingMushroomBlock(target); }
    public static void tryApplyMushroomContactAura(ServerLevel level, LivingEntity target, long now) { EnvironmentalAuraManager.tryApplyMushroomContactAura(level, target, now); }
    public static void updatePlayerHpTracking(ServerPlayer player, long now) { FurinaManager.updatePlayerHpTracking(player, now); }
    public static void recordFurinaHpFluctuation(ServerLevel level, LivingEntity changedEntity, float hpFractionDelta) { FurinaManager.recordFurinaHpFluctuation(level, changedEntity, hpFractionDelta); }
    public static void activateFurinaUniversalRevelry(ServerPlayer player, boolean isNetherite) { FurinaManager.activateFurinaUniversalRevelry(player, isNetherite); }
    public static float getFanfareBonusMultiplier(@Nullable LivingEntity entity) { return FurinaManager.getFanfareBonusMultiplier(entity); }
    public static boolean isMavuikaOnCooldown(UUID id, long now) { return MavuikaManager.isMavuikaOnCooldown(id, now); }
    public static long getMavuikaSkillRemainingTicks(UUID id, long now) { return MavuikaManager.getMavuikaSkillRemainingTicks(id, now); }
    public static void applyMavuikaDismountOrRemainingCooldown(ServerPlayer player) { MavuikaManager.applyMavuikaDismountOrRemainingCooldown(player); }
    public static void activateMavuikaSunAura(ServerPlayer player, boolean isNetherite, int cooldownTicks) { MavuikaManager.activateMavuikaSunAura(player, isNetherite, cooldownTicks); }
    public static boolean isMavuikaInCombat(ServerPlayer player) { return MavuikaManager.isMavuikaInCombat(player); }
    public static void consumeMavuikaNightsoulFuel(ServerPlayer player, float amount) { MavuikaManager.consumeMavuikaNightsoulFuel(player, amount); }
    public static void addMavuikaFightingSpiritFromExternalNightsoul(ServerPlayer player, float nightsoulConsumed) { MavuikaManager.addMavuikaFightingSpiritFromExternalNightsoul(player, nightsoulConsumed); }
    public static boolean isInsideAnemoUpdraft(ServerLevel level, Vec3 pos) { return VentiManager.isInsideAnemoUpdraft(level, pos); }
    public static void addMavuikaNormalAttackFightingSpirit(ServerPlayer player) { MavuikaManager.addMavuikaNormalAttackFightingSpirit(player); }
    public static boolean isMavuikaAuraActive(UUID id, long now) { return MavuikaManager.isMavuikaAuraActive(id, now); }
    public static void summonAndMountFlamestrider(ServerPlayer player, boolean isNetherite, int cooldownTicks) { MavuikaManager.summonAndMountFlamestrider(player, isNetherite, cooldownTicks); }
    public static void summonAndMountFlamestrider(ServerPlayer player, boolean isNetherite, int cooldownTicks, boolean dealCastSweep) { MavuikaManager.summonAndMountFlamestrider(player, isNetherite, cooldownTicks, dealCastSweep); }
    public static void toggleMavuikaBike(ServerPlayer player) { MavuikaManager.toggleMavuikaBike(player); }
    public static int getMavuikaUltiCooldownTicks(net.minecraft.world.entity.Entity target) { return MavuikaManager.getMavuikaUltiCooldownTicks(target); }
    public static float getMavuikaCrucibleNaBonusMultiplier(ServerPlayer player) { return MavuikaManager.getMavuikaCrucibleNaBonusMultiplier(player); }
    public static float getMavuikaCrucibleChargedBonusMultiplier(ServerPlayer player) { return MavuikaManager.getMavuikaCrucibleChargedBonusMultiplier(player); }
    public static void triggerMavuikaUlti(ServerPlayer player) { MavuikaManager.triggerMavuikaUlti(player); }
    public static void executeMavuikaUltiImpact(ServerLevel level, ServerPlayer player, float ultiDmg) { MavuikaManager.executeMavuikaUltiImpact(level, player, ultiDmg); }
    public static void tickMavuikaSunAura(MinecraftServer server, long serverNow) { MavuikaManager.tickMavuikaSunAura(server, serverNow); }
    public static void drainHealthSilently(LivingEntity entity, float newHealth) { FurinaManager.drainHealthSilently(entity, newHealth); }
    public static void setPlayerShieldAbsorption(ServerPlayer player, float targetAbsorption) { ZhongliManager.setPlayerShieldAbsorption(player, targetAbsorption); }
    public static void activateJadeShield(ServerPlayer player, boolean isNetherite) { ZhongliManager.activateJadeShield(player, isNetherite); }
    public static void activateNightsFocusShield(ServerPlayer player, boolean isNetherite) { NightsFocusManager.activateNightsFocusShield(player, isNetherite); }
    public static void activateNightsFocusBurstStars(ServerPlayer player, boolean isNetherite) { NightsFocusManager.activateNightsFocusBurstStars(player, isNetherite); }
    public static void clearNightsFocusShield(ServerPlayer player, boolean playBreakSound) { NightsFocusManager.clearNightsFocusShield(player, playBreakSound); }
    public static boolean hasActiveNightsFocusShield(@Nullable LivingEntity entity) { return NightsFocusManager.hasActiveNightsFocusShield(entity); }
    public static void onPlayerCastStaffSkill(@Nullable ServerPlayer player) { RaidenManager.onPlayerCastStaffSkill(player); }
    public static void grantRaidenResolveOnStaffSkill(ServerPlayer player) { RaidenManager.grantRaidenResolveOnStaffSkill(player); }
    public static void addNightsFocusStars(ServerPlayer player, int amount, long now) { NightsFocusManager.addNightsFocusStars(player, amount, now); }
    public static void tickNightsFocusShield(ServerPlayer player, long now) { NightsFocusManager.tickNightsFocusShield(player, now); }
    public static @Nullable LivingEntity findNearestNightsFocusTarget(ServerLevel level, ServerPlayer player) { return NightsFocusManager.findNearestNightsFocusTarget(level, player); }
    public static boolean fireSingleNightsFocusStar(ServerLevel level, ServerPlayer player) { return NightsFocusManager.fireSingleNightsFocusStar(level, player); }
    public static void tickNightsFocusProjectiles() { NightsFocusManager.tickNightsFocusProjectiles(); }
    public static boolean hasActiveJadeShield(@Nullable LivingEntity entity) { return ZhongliManager.hasActiveJadeShield(entity); }
    public static boolean isNearJadeShieldAura(@Nullable LivingEntity target) { return ZhongliManager.isNearJadeShieldAura(target); }
    public static float getVortexGoldenMajestyMultiplier(@Nullable ServerPlayer attacker) { return ZhongliManager.getVortexGoldenMajestyMultiplier(attacker); }
    public static boolean isChannelingGuardianLens(Player player) { return NeuvilletteManager.isChannelingGuardianLens(player); }
    public static void applyFurinaWaterWalking(Player player) { FurinaManager.applyFurinaWaterWalking(player); }
    public static boolean isStandingOnWaterSurface(Player player) { return FurinaManager.isStandingOnWaterSurface(player); }
    public static boolean hasRecentHpChangeBuff(ServerPlayer player) { return FurinaManager.hasRecentHpChangeBuff(player); }
    public static boolean carriesGuardianLens(Player player) { return NeuvilletteManager.carriesGuardianLens(player); }
    public static void onHydroReactionTriggered( ServerLevel level, Vec3 targetPos, ElementalReaction reaction, @Nullable Element reactedAura, @Nullable Element triggerElem, @Nullable ServerPlayer attacker ) { NeuvilletteManager.onHydroReactionTriggered(level, targetPos, reaction, reactedAura, triggerElem, attacker); }
    public static boolean hasNetheriteGuardianLens(@Nullable LivingEntity entity) { return NeuvilletteManager.hasNetheriteGuardianLens(entity); }
    public static void tryInterruptGuardianChanneling(LivingEntity victim, float damageDealt) { NeuvilletteManager.tryInterruptGuardianChanneling(victim, damageDealt); }
    public static int getDraconicGloriesStacks(@Nullable LivingEntity entity) { return NeuvilletteManager.getDraconicGloriesStacks(entity); }
    public static float getDraconicGloriesMultiplier(@Nullable LivingEntity entity) { return NeuvilletteManager.getDraconicGloriesMultiplier(entity); }
    public static void spawnSourcewaterDroplet(ServerLevel level, Vec3 basePos, @Nullable UUID ownerId, long now) { NeuvilletteManager.spawnSourcewaterDroplet(level, basePos, ownerId, now); }
    public static int consumeSourcewaterDropletsForChannel(ServerLevel level, ServerPlayer caster, double radius, int maxCount) { return NeuvilletteManager.consumeSourcewaterDropletsForChannel(level, caster, radius, maxCount); }
    public static void tickSourcewaterDroplets(MinecraftServer server, long serverNow) { NeuvilletteManager.tickSourcewaterDroplets(server, serverNow); }
    public static @Nullable Element getElementForEffect(Holder<MobEffect> effectHolder) { return PotionAndEnchantmentManager.getElementForEffect(effectHolder); }
    public static List<Element> getElementsFromPotionContents(PotionContents contents) { return PotionAndEnchantmentManager.getElementsFromPotionContents(contents); }
    public static List<Element> getElementsFromPotionStack(ItemStack stack) { return PotionAndEnchantmentManager.getElementsFromPotionStack(stack); }
    public static List<Element> getElementsFromArrow(ServerLevel level, Arrow arrow) { return PotionAndEnchantmentManager.getElementsFromArrow(level, arrow); }
    public static List<Element> getElementsFromAreaEffectCloud(ServerLevel level, AreaEffectCloud cloud) { return PotionAndEnchantmentManager.getElementsFromAreaEffectCloud(level, cloud); }
    public static void handleTippedArrowHit(ServerLevel level, Arrow arrow, LivingEntity target) { PotionAndEnchantmentManager.handleTippedArrowHit(level, arrow, target); }
    public static void handleItemUseFinish(ServerLevel level, LivingEntity entity, ItemStack stack) { PotionAndEnchantmentManager.handleItemUseFinish(level, entity, stack); }
    public static int getMaceWindBurstLevel(LivingEntity attacker) { return PotionAndEnchantmentManager.getMaceWindBurstLevel(attacker); }
    public static int getElementalMasteryLevel(ItemStack stack) { return PotionAndEnchantmentManager.getElementalMasteryLevel(stack); }
    public static int getElementalMasteryLevel(@Nullable LivingEntity attacker) { return PotionAndEnchantmentManager.getElementalMasteryLevel(attacker); }
    public static int getElementalProtectionLevel(ItemStack stack) { return PotionAndEnchantmentManager.getElementalProtectionLevel(stack); }
    public static int getElementalProtectionTotalLevel(@Nullable LivingEntity entity) { return PotionAndEnchantmentManager.getElementalProtectionTotalLevel(entity); }
    public static float applyElementalProtectionAndArmorWear(LivingEntity entity, DamageSource source, float amount) { return PotionAndEnchantmentManager.applyElementalProtectionAndArmorWear(entity, source, amount); }
    public static void updateCoopMobScaling(ServerLevel level, LivingEntity entity) { DamageResistanceManager.updateCoopMobScaling(level, entity); }
    public static int getThornsEnchantmentLevel(LivingEntity entity) { return PotionAndEnchantmentManager.getThornsEnchantmentLevel(entity); }
    public static boolean hasThornsEnchantment(LivingEntity entity) { return PotionAndEnchantmentManager.hasThornsEnchantment(entity); }
    public static void reflectThornsElement( ServerLevel level, LivingEntity defender, LivingEntity attacker, long now, @Nullable Element fallbackElement ) { PotionAndEnchantmentManager.reflectThornsElement(level, defender, attacker, now, fallbackElement); }
    public static void handleLivingIncomingDamage(LivingEntity target, DamageSource source) { DamageResistanceManager.handleLivingIncomingDamage(target, source); }
    public static void processIncomingDamageSources(ServerLevel level, LivingEntity target, DamageSource source) { DamageResistanceManager.processIncomingDamageSources(level, target, source); }
    public static float modifyIncomingEnvironmentalDamage(LivingEntity target, DamageSource source, float amount) { return EnvironmentalAuraManager.modifyIncomingEnvironmentalDamage(target, source, amount); }
    public static float modifyIncomingEnvironmentalDamageInternal(LivingEntity target, DamageSource source, float amount) { return EnvironmentalAuraManager.modifyIncomingEnvironmentalDamageInternal(target, source, amount); }
    public static void maintainBurningPyroAura(LivingEntity target) { EnvironmentalAuraManager.maintainBurningPyroAura(target); }
    public static void handleEnvironmentalFireDamage(LivingEntity target) { EnvironmentalAuraManager.handleEnvironmentalFireDamage(target); }
    public static boolean isNaturallyWaterSensitive(LivingEntity target) { return EnvironmentalAuraManager.isNaturallyWaterSensitive(target); }
    public static void handleEnvironmentalWater(LivingEntity target) { EnvironmentalAuraManager.handleEnvironmentalWater(target); }
    public static void checkCryoWaterFreezing(LivingEntity entity) { EnvironmentalAuraManager.checkCryoWaterFreezing(entity); }
    public static boolean isHostileMob(LivingEntity entity) { return InnateMobManager.isHostileMob(entity); }
    public static void checkInnateMutation(LivingEntity entity) { InnateMobManager.checkInnateMutation(entity); }
    public static void rollInnateMutationOnce(LivingEntity entity) { InnateMobManager.rollInnateMutationOnce(entity); }
    public static boolean isOmniInnate(@Nullable Entity entity) { return InnateMobManager.isOmniInnate(entity); }
    public static @Nullable Element getInnateElement(LivingEntity target) { return InnateMobManager.getInnateElement(target); }
    public static EnumSet<Element> getInnateElements(@Nullable LivingEntity target) { return InnateMobManager.getInnateElements(target); }
    public static boolean hasInnateElement(@Nullable LivingEntity target, @Nullable Element element) { return InnateMobManager.hasInnateElement(target, element); }
    public static boolean isPyroInnate(@Nullable Entity entity) { return InnateMobManager.isPyroInnate(entity); }
    public static boolean isPyroResistant(@Nullable LivingEntity target) { return InnateMobManager.isPyroResistant(target); }
    public static void ensurePreDeathPyroIgnite(@Nullable LivingEntity target, int seconds) { InnateMobManager.ensurePreDeathPyroIgnite(target, seconds); }
    public static void extinguishPyroInnate(@Nullable LivingEntity entity) { InnateMobManager.extinguishPyroInnate(entity); }
    public static boolean isMutatedInnate(LivingEntity entity) { return InnateMobManager.isMutatedInnate(entity); }
    public static void tryDropElementOnDeath(LivingEntity entity, DamageSource source) { InnateMobManager.tryDropElementOnDeath(entity, source); }
    public static @Nullable Element getNaturalInnateElement(LivingEntity target) { return InnateMobManager.getNaturalInnateElement(target); }
    public static boolean isBossEntity(LivingEntity entity) { return InnateMobManager.isBossEntity(entity); }
    public static boolean isPassiveAquaticOrAmbient(LivingEntity entity) { return InnateMobManager.isPassiveAquaticOrAmbient(entity); }
    public static void recordPreAttackStrength(@Nullable Player player, float scale) { PotionAndEnchantmentManager.recordPreAttackStrength(player, scale); }
    public static void clearPreAttackStrength(@Nullable Player player) { PotionAndEnchantmentManager.clearPreAttackStrength(player); }
    public static float getEffectiveAttackStrengthScale(Player attacker) { return PotionAndEnchantmentManager.getEffectiveAttackStrengthScale(attacker); }
    public static float modifyFabricIncomingDamage(LivingEntity entity, DamageSource source, float amount) { return DamageResistanceManager.modifyFabricIncomingDamage(entity, source, amount); }
    public static float applyImbuedSwordHit(LivingEntity target, DamageSource source, float incomingDamage) { return PotionAndEnchantmentManager.applyImbuedSwordHit(target, source, incomingDamage); }
    public static void triggerOverloadExplosion(ServerLevel level, LivingEntity target, @Nullable ServerPlayer attacker, double dmgMult) { ReactionEffectManager.triggerOverloadExplosion(level, target, attacker, dmgMult); }
    public static void drawVoltaicArc(ServerLevel level, Vec3 start, Vec3 end) { ReactionEffectManager.drawVoltaicArc(level, start, end); }
    public static void drawVoltaicArc(ServerLevel level, Entity from, Entity to) { ReactionEffectManager.drawVoltaicArc(level, from, to); }
    public static Element pickDefenderAuraForThorns(LivingEntity defender) { return PotionAndEnchantmentManager.pickDefenderAuraForThorns(defender); }
    public static void updateCryoDebuff(LivingEntity target, UUID id, boolean hasCryo, @Nullable Element innate) { EnvironmentalAuraManager.updateCryoDebuff(target, id, hasCryo, innate); }
    public static void spawnAmbientParticles(ServerLevel level, LivingEntity target, Map<Element, GaugeState> gauges, long now) { ElementalTickManager.spawnAmbientParticles(level, target, gauges, now); }
    public static void spawnExpandingSquareParticles( ServerLevel level, Vec3 center, double halfExtent, double yOffset, ParticleOptions primary, @Nullable ParticleOptions accent, int accentStride ) { ElementalTickManager.spawnExpandingSquareParticles(level, center, halfExtent, yOffset, primary, accent, accentStride); }
    public static void handleCreeperExplosionBurst( ServerLevel level, Creeper creeper, Vec3 center, float explosionRadius ) { ReactionEffectManager.handleCreeperExplosionBurst(level, creeper, center, explosionRadius); }
    public static void onEvokerFangJoin(ServerLevel level, EvokerFangs fangs) { ZhongliManager.onEvokerFangJoin(level, fangs); }
    public static void flushPendingEvokerConstructs(MinecraftServer server) { ZhongliManager.flushPendingEvokerConstructs(server); }
    public static void summonDaybreakVortex(ServerPlayer owner, Vec3 center, boolean isNetherite) { VentiManager.summonDaybreakVortex(owner, center, isNetherite); }
    public static void tickDaybreakVortices(MinecraftServer server, long serverNow) { VentiManager.tickDaybreakVortices(server, serverNow); }
    public static @Nullable Element detectDaybreakAbsorption(ServerLevel level, Vec3 center, List<LivingEntity> targets) { return VentiManager.detectDaybreakAbsorption(level, center, targets); }
    public static boolean isRaidenProtectedAlly(ServerPlayer owner, LivingEntity e) { return RaidenManager.isRaidenProtectedAlly(owner, e); }
    public static boolean isRaidenSkillOnCooldown(UUID id, long now) { return RaidenManager.isRaidenSkillOnCooldown(id, now); }
    public static long getRaidenSkillRemainingTicks(UUID id, long now) { return RaidenManager.getRaidenSkillRemainingTicks(id, now); }
    public static boolean isRaidenBurstOnCooldown(UUID id, long now) { return RaidenManager.isRaidenBurstOnCooldown(id, now); }
    public static long getRaidenBurstRemainingTicks(UUID id, long now) { return RaidenManager.getRaidenBurstRemainingTicks(id, now); }
    public static boolean isRaidenMusouActive(UUID id, long now) { return RaidenManager.isRaidenMusouActive(id, now); }
    public static boolean isRaidenIframeActive(@Nullable Entity entity) { return RaidenManager.isRaidenIframeActive(entity); }
    public static boolean isMavuikaBikeIframeActive(@Nullable Entity entity, @Nullable DamageSource source) { return MavuikaManager.isMavuikaBikeIframeActive(entity, source); }
    public static int getRaidenResolve(UUID id) { return RaidenManager.getRaidenResolve(id); }
    public static void syncRaidenState(ServerPlayer player, long now) { RaidenManager.syncRaidenState(player, now); }
    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span, boolean isLeftArm) { RaidenManager.sendRaidenSlashArc(level, center, forward, comboStep, isMusouBurst, isCharged, span, isLeftArm); }
    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span) { RaidenManager.sendRaidenSlashArc(level, center, forward, comboStep, isMusouBurst, isCharged, span); }
    public static void addRaidenResolve(ServerPlayer player, int baseAmount) { RaidenManager.addRaidenResolve(player, baseAmount); }
    public static boolean activateRaidenEye(ServerPlayer player, boolean isNetherite) { return RaidenManager.activateRaidenEye(player, isNetherite); }
    public static boolean activateRaidenMusouBurst(ServerPlayer player, boolean isNetherite) { return RaidenManager.activateRaidenMusouBurst(player, isNetherite); }
    public static void executeRaidenHitotachiImpact(ServerPlayer player, boolean isNetherite, float resolveMult) { RaidenManager.executeRaidenHitotachiImpact(player, isNetherite, resolveMult); }
    public static void triggerRaidenUlti(ServerPlayer player) { RaidenManager.triggerRaidenUlti(player); }
    public static void triggerRaidenEyeCoordinatedStrike(LivingEntity target, DamageSource source) { RaidenManager.triggerRaidenEyeCoordinatedStrike(target, source); }
    public static float applyRaidenMusouIsshinHit(LivingEntity target, DamageSource source, float incomingDamage) { return RaidenManager.applyRaidenMusouIsshinHit(target, source, incomingDamage); }
    public static void performRaidenMusouSwing(ServerPlayer player, boolean isCharged) { RaidenManager.performRaidenMusouSwing(player, isCharged); }
    public static void tickRaidenState(MinecraftServer server, long serverNow) { RaidenManager.tickRaidenState(server, serverNow); }
    public static void applyPetrifyState(LivingEntity target, int petrifyTicks, long targetGameTime) { ZhongliManager.applyPetrifyState(target, petrifyTicks, targetGameTime); }
    public static void clearEntityState(@Nullable LivingEntity entity) { ElementalTickManager.clearEntityStateInternal(entity); }
    public static void clearAll() { ElementalTickManager.clearAllInternal(); }
}
