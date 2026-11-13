package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Genshin Impact Elemental Resonance (8 types) and the 4-Staff Inventory Cap (Arcane Overload).
 */
public final class ElementalResonanceManager {
    private static final UUID PYRO_ATK_ID = UUID.fromString("a8d3c901-5b40-4d1c-8e7a-3f62a9b10001");
    private static final UUID HYDRO_HP_ID = UUID.fromString("a8d3c901-5b40-4d1c-8e7a-3f62a9b10002");
    private static final UUID ANEMO_SPD_ID = UUID.fromString("a8d3c901-5b40-4d1c-8e7a-3f62a9b10003");

    public enum ResonanceType {
        PYRO(Element.PYRO, "resonance.velorise_staves.pyro"),
        HYDRO(Element.HYDRO, "resonance.velorise_staves.hydro"),
        ANEMO(Element.ANEMO, "resonance.velorise_staves.anemo"),
        ELECTRO(Element.ELECTRO, "resonance.velorise_staves.electro"),
        DENDRO(Element.DENDRO, "resonance.velorise_staves.dendro"),
        CRYO(Element.CRYO, "resonance.velorise_staves.cryo"),
        GEO(Element.GEO, "resonance.velorise_staves.geo"),
        PROTECTIVE_CANOPY(null, "resonance.velorise_staves.canopy");

        private final @Nullable Element element;
        private final String translationKey;

        ResonanceType(@Nullable Element element, String translationKey) {
            this.element = element;
            this.translationKey = translationKey;
        }

        public @Nullable Element element() {
            return element;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public static final class PlayerResonanceState {
        public final Set<ResonanceType> activeResonances;
        public final boolean overloaded;
        public final int staffCount;
        public final int mask;

        public PlayerResonanceState(Set<ResonanceType> activeResonances, boolean overloaded, int staffCount) {
            this.activeResonances = Collections.unmodifiableSet(activeResonances);
            this.overloaded = overloaded;
            this.staffCount = staffCount;
            this.mask = toMask(activeResonances);
        }

        private static int toMask(Set<ResonanceType> types) {
            int m = 0;
            for (ResonanceType t : types) {
                m |= (1 << t.ordinal());
            }
            return m;
        }
    }

    private static final class CooldownTrack {
        int totalTicks;

        CooldownTrack(int totalTicks) {
            this.totalTicks = Math.max(1, totalTicks);
        }
    }

    private record SyncSnapshot(int mask, boolean overloaded, int staffCount) {}

    private static final Map<UUID, PlayerResonanceState> PLAYER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, SyncSnapshot> LAST_SYNCED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ELECTRO_PARTICLE_ICD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DENDRO_BONUS_EM_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GEO_RES_SHRED_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Item, CooldownTrack>> RECORDED_STAFF_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_EXHAUSTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SELECTED_SLOT = new ConcurrentHashMap<>();

    private static volatile int clientResonanceMask = 0;
    private static volatile boolean clientOverloaded = false;
    private static volatile int clientStaffCount = 0;

    private ElementalResonanceManager() {}

    public static void clearAll() {
        PLAYER_STATES.clear();
        LAST_SYNCED.clear();
        ELECTRO_PARTICLE_ICD.clear();
        DENDRO_BONUS_EM_UNTIL.clear();
        GEO_RES_SHRED_UNTIL.clear();
        RECORDED_STAFF_COOLDOWNS.clear();
        LAST_EXHAUSTION.clear();
        LAST_SELECTED_SLOT.clear();
    }

    public static void clearClientState() {
        clientResonanceMask = 0;
        clientOverloaded = false;
        clientStaffCount = 0;
    }

    public static void setClientResonanceState(int mask, boolean overloaded, int staffCount) {
        clientResonanceMask = mask;
        clientOverloaded = overloaded;
        clientStaffCount = staffCount;
    }

    public static boolean hasClientResonance(ResonanceType type) {
        return StavesConfig.isElementalResonanceEnabled()
                && !clientOverloaded
                && (clientResonanceMask & (1 << type.ordinal())) != 0;
    }

    public static boolean isClientOverloaded() {
        return StavesConfig.isElementalResonanceEnabled() && clientOverloaded;
    }

    public static int getClientStaffCount() {
        return clientStaffCount;
    }

    public static int getMaxStavesCap() {
        return StavesConfig.getMaxStavesInInventory();
    }

    public static boolean isOverloadExempt(@Nullable Player player) {
        return player != null && (player.isCreative() || player.isSpectator());
    }

    public static void clearPlayerState(UUID playerId) {
        PLAYER_STATES.remove(playerId);
        LAST_SYNCED.remove(playerId);
        ELECTRO_PARTICLE_ICD.remove(playerId);
        DENDRO_BONUS_EM_UNTIL.remove(playerId);
        RECORDED_STAFF_COOLDOWNS.remove(playerId);
        LAST_EXHAUSTION.remove(playerId);
        LAST_SELECTED_SLOT.remove(playerId);
    }

    private static List<ItemStack> buildPrioritySlotOrder(Inventory inv) {
        List<ItemStack> ordered = new ArrayList<>(inv.items.size() + inv.offhand.size());
        int selected = inv.selected;
        if (selected >= 0 && selected < inv.items.size()) {
            ordered.add(inv.items.get(selected));
        }
        ordered.addAll(inv.offhand);
        int hotbarEnd = Math.min(9, inv.items.size());
        for (int i = 0; i < hotbarEnd; i++) {
            if (i != selected) {
                ordered.add(inv.items.get(i));
            }
        }
        for (int i = 9; i < inv.items.size(); i++) {
            if (i != selected) {
                ordered.add(inv.items.get(i));
            }
        }
        return ordered;
    }

    private static @Nullable Element resolveSlotElement(ItemStack stack, boolean allowTridentPartner) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof ArcaneStaffItem staff) {
            if (staff.kind() == ArcaneStaffItem.Kind.OMNI) {
                return ArcaneStaffItem.getOmniActiveElement(stack);
            }
            return staff.getElement();
        }
        if (allowTridentPartner && stack.is(Items.TRIDENT)) {
            return Element.HYDRO;
        }
        return null;
    }

    /**
     * Evaluates a player's main inventory (36 slots) + offhand (1 slot).
     * Duplicate backup copies of the same staff type only count once toward the cap.
     * Items stored inside backpacks, Shulker Boxes, or Ender Chests are NOT counted.
     */
    public static PlayerResonanceState evaluatePlayerInventory(Player player) {
        if (!StavesConfig.isElementalResonanceEnabled()) {
            return new PlayerResonanceState(Collections.emptySet(), false, 0);
        }

        List<ItemStack> orderedSlots = buildPrioritySlotOrder(player.getInventory());
        Set<String> distinctStaffKeys = new HashSet<>();
        Map<Element, Set<String>> distinctByElement = new LinkedHashMap<>();
        boolean hasWildcardOmni = false;
        boolean hasTrident = false;

        for (ItemStack stack : orderedSlots) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ArcaneStaffItem staff) {
                ArcaneStaffItem.Kind kind = staff.kind();
                if (kind == ArcaneStaffItem.Kind.OMNI) {
                    Element locked = ArcaneStaffItem.getOmniActiveElement(stack);
                    if (locked != null) {
                        String key = "OMNI_" + locked.name();
                        distinctStaffKeys.add(key);
                        distinctByElement.computeIfAbsent(locked, k -> new HashSet<>()).add(key);
                    } else {
                        distinctStaffKeys.add("OMNI_AUTO");
                        hasWildcardOmni = true;
                    }
                } else {
                    distinctStaffKeys.add(kind.name());
                    Element elem = staff.getElement();
                    if (elem != null) {
                        distinctByElement.computeIfAbsent(elem, k -> new HashSet<>()).add(kind.name());
                    }
                }
            } else if (stack.is(Items.TRIDENT)) {
                hasTrident = true;
            }
        }

        int uniqueStaffCount = distinctStaffKeys.size();
        if (hasTrident && uniqueStaffCount > 0) {
            distinctByElement.computeIfAbsent(Element.HYDRO, k -> new HashSet<>()).add("VANILLA_TRIDENT");
        }

        int cap = getMaxStavesCap();
        if (!isOverloadExempt(player) && cap > 0 && uniqueStaffCount > cap) {
            return new PlayerResonanceState(Collections.emptySet(), true, uniqueStaffCount);
        }

        EnumSet<ResonanceType> active = EnumSet.noneOf(ResonanceType.class);

        for (Map.Entry<Element, Set<String>> entry : distinctByElement.entrySet()) {
            if (entry.getValue().size() >= 2 && active.size() < 2) {
                active.add(toResonanceType(entry.getKey()));
            }
        }

        if (hasWildcardOmni && active.size() < 2) {
            for (ItemStack stack : orderedSlots) {
                Element slotElem = resolveSlotElement(stack, uniqueStaffCount > 0);
                if (slotElem == null) continue;
                ResonanceType rType = toResonanceType(slotElem);
                if (active.contains(rType)) continue;
                Set<String> kinds = distinctByElement.get(slotElem);
                if (kinds != null && kinds.size() == 1) {
                    active.add(rType);
                    break;
                }
            }
        }

        if (active.isEmpty() && distinctByElement.size() >= 4) {
            active.add(ResonanceType.PROTECTIVE_CANOPY);
        }

        return new PlayerResonanceState(active, false, uniqueStaffCount);
    }

    private static ResonanceType toResonanceType(Element element) {
        return switch (element) {
            case PYRO -> ResonanceType.PYRO;
            case HYDRO -> ResonanceType.HYDRO;
            case ANEMO -> ResonanceType.ANEMO;
            case ELECTRO -> ResonanceType.ELECTRO;
            case DENDRO -> ResonanceType.DENDRO;
            case CRYO -> ResonanceType.CRYO;
            case GEO -> ResonanceType.GEO;
        };
    }

    public static void syncPlayerImmediate(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        LAST_SELECTED_SLOT.put(player.getUUID(), player.getInventory().selected);
        PlayerResonanceState state = evaluatePlayerInventory(player);
        PLAYER_STATES.put(player.getUUID(), state);
        applyAttributeModifiers(player, state.activeResonances);
        SyncSnapshot snap = new SyncSnapshot(state.mask, state.overloaded, state.staffCount);
        LAST_SYNCED.put(player.getUUID(), snap);
        ModNetwork.sendResonanceSyncToPlayer(player, state.mask, state.overloaded, state.staffCount);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        int selected = player.getInventory().selected;
        Integer prevSelected = LAST_SELECTED_SLOT.put(player.getUUID(), selected);
        boolean slotSwitched = prevSelected == null || prevSelected.intValue() != selected;

        if (slotSwitched || (now % 5L == 0L)) {
            PlayerResonanceState state = evaluatePlayerInventory(player);
            PLAYER_STATES.put(player.getUUID(), state);
            applyAttributeModifiers(player, state.activeResonances);

            SyncSnapshot next = new SyncSnapshot(state.mask, state.overloaded, state.staffCount);
            SyncSnapshot prev = LAST_SYNCED.get(player.getUUID());
            if (!next.equals(prev)) {
                LAST_SYNCED.put(player.getUUID(), next);
                ModNetwork.sendResonanceSyncToPlayer(player, next.mask, next.overloaded, next.staffCount);
            }
        }

        FoodData food = player.getFoodData();
        float curExh = food.getExhaustionLevel();
        Float prevExh = LAST_EXHAUSTION.get(player.getUUID());
        if (prevExh != null && curExh > prevExh && hasResonance(player, ResonanceType.ANEMO)) {
            float gain = curExh - prevExh;
            if (gain < 3.5F && (player.isSprinting() || !player.onGround())) {
                float updated = Math.max(0.0F, curExh - gain * 0.15F);
                food.setExhaustion(updated);
                curExh = updated;
            }
        }
        LAST_EXHAUSTION.put(player.getUUID(), curExh);

        if (hasResonance(player, ResonanceType.HYDRO)
                && player.getRemainingFireTicks() > 2
                && !player.isInLava()
                && (now % 3L == 0L)) {
            player.setRemainingFireTicks(Math.max(1, player.getRemainingFireTicks() - 2));
        }

        if (now % 100L == 0L) {
            GEO_RES_SHRED_UNTIL.entrySet().removeIf(e -> now >= e.getValue() || (e.getValue() - now) > 600L);
            DENDRO_BONUS_EM_UNTIL.entrySet().removeIf(e -> now >= e.getValue() || (e.getValue() - now) > 300L);
            ELECTRO_PARTICLE_ICD.entrySet().removeIf(e -> Math.abs(now - e.getValue()) > 100L);
        }
    }

    private static void applyAttributeModifiers(ServerPlayer player, Set<ResonanceType> active) {
        AttributeInstance atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null && atkAttr.getModifier(PYRO_ATK_ID) != null) {
            atkAttr.removeModifier(PYRO_ATK_ID);
        }

        AttributeInstance hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            boolean wantHydro = active.contains(ResonanceType.HYDRO);
            AttributeModifier curHydro = hpAttr.getModifier(HYDRO_HP_ID);
            if (wantHydro) {
                if (curHydro == null || Math.abs(curHydro.getAmount() - 0.25D) > 0.001D) {
                    if (curHydro != null) hpAttr.removeModifier(HYDRO_HP_ID);
                    hpAttr.addTransientModifier(new AttributeModifier(
                            HYDRO_HP_ID, "Resonance Hydro HP", 0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            } else if (curHydro != null) {
                hpAttr.removeModifier(HYDRO_HP_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        AttributeInstance spdAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdAttr != null) {
            boolean wantAnemo = active.contains(ResonanceType.ANEMO);
            boolean hasAnemo = spdAttr.getModifier(ANEMO_SPD_ID) != null;
            if (wantAnemo && !hasAnemo) {
                spdAttr.addTransientModifier(new AttributeModifier(
                        ANEMO_SPD_ID, "Resonance Anemo Speed", 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!wantAnemo && hasAnemo) {
                spdAttr.removeModifier(ANEMO_SPD_ID);
            }
        }
    }

    public static boolean hasResonance(@Nullable LivingEntity entity, ResonanceType type) {
        if (!StavesConfig.isElementalResonanceEnabled() || !(entity instanceof Player player)) return false;
        if (player.level().isClientSide()) {
            return hasClientResonance(type);
        }
        PlayerResonanceState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) {
            state = evaluatePlayerInventory(player);
            PLAYER_STATES.put(player.getUUID(), state);
        }
        return !state.overloaded && state.activeResonances.contains(type);
    }

    public static boolean isOverloaded(@Nullable Player player) {
        if (player == null || isOverloadExempt(player) || !StavesConfig.isElementalResonanceEnabled()) return false;
        PlayerResonanceState state = evaluatePlayerInventory(player);
        if (!player.level().isClientSide()) {
            PLAYER_STATES.put(player.getUUID(), state);
        }
        return state.overloaded;
    }

    public static void applyCooldown(Player player, Item item, int baseTicks) {
        int finalTicks = hasResonance(player, ResonanceType.ANEMO)
                ? Math.max(1, (int) Math.round(baseTicks * 0.90D))
                : Math.max(1, baseTicks);
        player.getCooldowns().addCooldown(item, finalTicks);
        if (!player.level().isClientSide() && item instanceof ArcaneStaffItem) {
            RECORDED_STAFF_COOLDOWNS
                    .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                    .put(item, new CooldownTrack(finalTicks));
        }
    }

    public static int getClientAdjustedCooldownTicks(int baseTicks) {
        return hasClientResonance(ResonanceType.ANEMO)
                ? Math.max(1, (int) Math.round(baseTicks * 0.90D))
                : Math.max(1, baseTicks);
    }

    public static void onElectroReactionTriggered(@Nullable ServerPlayer attacker, long now) {
        if (attacker == null || !hasResonance(attacker, ResonanceType.ELECTRO)) return;
        Long last = ELECTRO_PARTICLE_ICD.get(attacker.getUUID());
        if (last != null && now >= last && (now - last) < 100L) return;
        ELECTRO_PARTICLE_ICD.put(attacker.getUUID(), now);

        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof ArcaneStaffItem && held.isDamaged())) {
            held = attacker.getOffhandItem();
        }
        if (held.getItem() instanceof ArcaneStaffItem && held.isDamaged()) {
            held.setDamageValue(Math.max(0, held.getDamageValue() - 1));
        }

        Map<Item, CooldownTrack> tracks = RECORDED_STAFF_COOLDOWNS.get(attacker.getUUID());
        if (tracks != null && !tracks.isEmpty()) {
            Iterator<Map.Entry<Item, CooldownTrack>> it = tracks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Item, CooldownTrack> entry = it.next();
                Item staffItem = entry.getKey();
                CooldownTrack track = entry.getValue();
                float pct = attacker.getCooldowns().getCooldownPercent(staffItem, 0.0F);
                if (pct <= 0.0F) {
                    it.remove();
                    continue;
                }
                int remaining = Math.round(pct * track.totalTicks);
                int reduced = remaining - 20;
                attacker.getCooldowns().removeCooldown(staffItem);
                if (reduced > 0) {
                    attacker.getCooldowns().addCooldown(staffItem, reduced);
                    track.totalTicks = reduced;
                } else {
                    it.remove();
                }
            }
        }

        attacker.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                attacker.getX(), attacker.getY() + 1.0D, attacker.getZ(),
                8, 0.35D, 0.45D, 0.35D, 0.05D);
    }

    public static void onDendroReactionTriggered(@Nullable ServerPlayer attacker, long now) {
        if (attacker == null || !hasResonance(attacker, ResonanceType.DENDRO)) return;
        DENDRO_BONUS_EM_UNTIL.put(attacker.getUUID(), now + 120L);
    }

    public static int getBonusElementalMasteryLevels(@Nullable LivingEntity attacker) {
        if (!hasResonance(attacker, ResonanceType.DENDRO)) return 0;
        int bonus = 1;
        Long until = DENDRO_BONUS_EM_UNTIL.get(attacker.getUUID());
        if (until != null && attacker.level().getGameTime() < until) {
            bonus += 1;
        }
        return bonus;
    }

    public static float getPlayerIncomingAuraDurationScale(@Nullable LivingEntity target, Element auraElement) {
        if (target == null) return 1.0F;
        float scale = 1.0F;
        if (target instanceof ServerPlayer sp) {
            if (auraElement == Element.CRYO && hasResonance(sp, ResonanceType.PYRO)) scale *= 0.6F;
            else if (auraElement == Element.PYRO && hasResonance(sp, ResonanceType.HYDRO)) scale *= 0.6F;
            else if (auraElement == Element.HYDRO && hasResonance(sp, ResonanceType.ELECTRO)) scale *= 0.6F;
            else if (auraElement == Element.ELECTRO && hasResonance(sp, ResonanceType.CRYO)) scale *= 0.6F;
        }
        int epLevels = ElementalGaugeManager.getElementalProtectionTotalLevel(target);
        if (epLevels > 0) {
            scale *= Math.max(0.40F, 1.0F - epLevels * 0.03F);
        }
        return scale;
    }

    public static long getBurningDurationTicksForTarget(LivingEntity target, long baseTicks) {
        if (target instanceof ServerPlayer sp && hasResonance(sp, ResonanceType.HYDRO)) {
            return Math.max(20L, Math.round(baseTicks * 0.6D));
        }
        return baseTicks;
    }

    public static void applyGeoResShred(LivingEntity target, long now) {
        GEO_RES_SHRED_UNTIL.put(target.getUUID(), now + 300L);
    }

    public static boolean hasGeoResShred(LivingEntity target, long now) {
        Long until = GEO_RES_SHRED_UNTIL.get(target.getUUID());
        return until != null && now < until;
    }

    private static boolean hasActiveGeoShield(ServerPlayer player, long now) {
        return CrystallizeShardManager.getActiveShield(player, now) != null || player.getAbsorptionAmount() > 0.0F;
    }

    private static float applyCryoCrit(ServerLevel level, @Nullable ServerPlayer attacker, LivingEntity target, float damage) {
        if (attacker == null || damage <= 0.0F || !hasResonance(attacker, ResonanceType.CRYO)) return damage;
        boolean cold = ElementalGaugeManager.isEntityFrozen(target)
                || ElementalGaugeManager.hasElement(target, Element.CRYO)
                || ElementalGaugeManager.getInnateElement(target) == Element.CRYO;
        if (cold && level.random.nextFloat() < 0.15F) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.08D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 1.2F);
            return damage * 1.5F;
        }
        return damage;
    }

    public static float applyOffensiveElementalResonance(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            @Nullable ServerPlayer attacker,
            float damage
    ) {
        if (damage <= 0.0F) return damage;
        if (attacker != null) {
            damage *= ElementalGaugeManager.getFanfareBonusMultiplier(attacker);
        }
        if (!StavesConfig.isElementalResonanceEnabled()) return damage;
        long now = level.getGameTime();
        if (attacker != null) {
            if (hasResonance(attacker, ResonanceType.PYRO)) {
                damage *= (float) (1.0D + StavesConfig.PYRO_RESONANCE_DAMAGE_BONUS.get());
            }
            if (hasResonance(attacker, ResonanceType.GEO) && hasActiveGeoShield(attacker, now)) {
                damage *= 1.15F;
                applyGeoResShred(target, now);
            }
        }
        if (attackElement == Element.GEO && hasGeoResShred(target, now)) {
            damage *= 1.20F;
        }
        return applyCryoCrit(level, attacker, target, damage);
    }

    public static float modifyPhysicalAttackDamage(LivingEntity target, DamageSource src, float amount) {
        if (amount <= 0.0F) return amount;
        if (!(target.level() instanceof ServerLevel level)) return amount;
        if (!(src.getEntity() instanceof ServerPlayer attacker) || attacker == target) return amount;
        if (ElementalGaugeManager.isElementalOrMagicDamage(src)) return amount;
        amount *= ElementalGaugeManager.getFanfareBonusMultiplier(attacker);
        if (!StavesConfig.isElementalResonanceEnabled()) return amount;

        long now = level.getGameTime();
        if (hasResonance(attacker, ResonanceType.PYRO)) {
            amount *= (float) (1.0D + StavesConfig.PYRO_RESONANCE_DAMAGE_BONUS.get());
        }
        if (hasResonance(attacker, ResonanceType.GEO) && hasActiveGeoShield(attacker, now)) {
            amount *= 1.15F;
            applyGeoResShred(target, now);
        }
        return applyCryoCrit(level, attacker, target, amount);
    }

    /** Hydro Resonance (Soothing Water): +25% Incoming Healing Effectiveness. */
    public static float modifyIncomingHeal(@Nullable LivingEntity entity, float amount) {
        if (amount <= 0.0F || entity == null) return amount;
        amount *= ElementalGaugeManager.getFanfareBonusMultiplier(entity);
        if (!StavesConfig.isElementalResonanceEnabled()) return amount;
        if (hasResonance(entity, ResonanceType.HYDRO)) {
            return amount * 1.25F;
        }
        return amount;
    }

    public static float modifyIncomingDefenseDamage(LivingEntity victim, DamageSource src, float amount) {
        if (amount <= 0.0F || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        if (victim instanceof ServerPlayer sp && hasResonance(sp, ResonanceType.PROTECTIVE_CANOPY)) {
            return amount * 0.85F;
        }
        return amount;
    }
}
