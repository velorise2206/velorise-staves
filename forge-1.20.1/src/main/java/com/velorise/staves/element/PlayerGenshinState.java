package com.velorise.staves.element;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Cohesive per-player / per-entity state container storing all combat timers, stacks,
 * and ability gauges in a single lifecycle-managed object to prevent memory leaks.
 */
public final class PlayerGenshinState {
    private static final ConcurrentHashMap<UUID, PlayerGenshinState> STATES = new ConcurrentHashMap<>();

    public static final Map<UUID, Long> FURINA_FANFARE_UNTIL = createFieldView(s -> s.furinaFanfareUntil, (s, v) -> s.furinaFanfareUntil = v);
    public static final Map<UUID, Integer> FURINA_FANFARE_POINTS = createFieldView(s -> s.furinaFanfarePoints, (s, v) -> s.furinaFanfarePoints = v);
    public static final Map<UUID, Boolean> FURINA_FANFARE_NETHERITE = createFieldView(s -> s.furinaFanfareNetherite, (s, v) -> s.furinaFanfareNetherite = v);
    public static final Map<UUID, Long> JADE_SHIELD_UNTIL = createFieldView(s -> s.jadeShieldUntil, (s, v) -> s.jadeShieldUntil = v);
    public static final Map<UUID, Boolean> JADE_SHIELD_NETHERITE = createFieldView(s -> s.jadeShieldNetherite, (s, v) -> s.jadeShieldNetherite = v);
    public static final Map<UUID, Long> NIGHTS_FOCUS_UNTIL = createFieldView(s -> s.nightsFocusUntil, (s, v) -> s.nightsFocusUntil = v);
    public static final Map<UUID, Boolean> NIGHTS_FOCUS_NETHERITE = createFieldView(s -> s.nightsFocusNetherite, (s, v) -> s.nightsFocusNetherite = v);
    public static final Map<UUID, Integer> NIGHTS_FOCUS_STARS = createFieldView(s -> s.nightsFocusStars, (s, v) -> s.nightsFocusStars = v);
    public static final Map<UUID, Long> NIGHTS_FOCUS_LAST_AUTO_STAR = createFieldView(s -> s.nightsFocusLastAutoStar, (s, v) -> s.nightsFocusLastAutoStar = v);
    public static final Map<UUID, Long> NIGHTS_FOCUS_LAST_SKILL_STAR = createFieldView(s -> s.nightsFocusLastSkillStar, (s, v) -> s.nightsFocusLastSkillStar = v);
    public static final Map<UUID, Float> NIGHTS_FOCUS_BASE_HP = createFieldView(s -> s.nightsFocusBaseHp, (s, v) -> s.nightsFocusBaseHp = v);
    public static final Map<UUID, Integer> NIGHTS_FOCUS_PENDING_SHOTS = createFieldView(s -> s.nightsFocusPendingShots, (s, v) -> s.nightsFocusPendingShots = v);
    public static final Map<UUID, Long> LAYLA_DREAMSPHERE_UNTIL = createFieldView(s -> s.laylaDreamsphereUntil, (s, v) -> s.laylaDreamsphereUntil = v);
    public static final Map<UUID, Boolean> LAYLA_DREAMSPHERE_NETHERITE = createFieldView(s -> s.laylaDreamsphereNetherite, (s, v) -> s.laylaDreamsphereNetherite = v);
    public static final Map<UUID, Long> LAYLA_DREAMSPHERE_NEXT_SHOT = createFieldView(s -> s.laylaDreamsphereNextShot, (s, v) -> s.laylaDreamsphereNextShot = v);
    public static final Map<UUID, Long> MAVUIKA_AURA_UNTIL = createFieldView(s -> s.mavuikaAuraUntil, (s, v) -> s.mavuikaAuraUntil = v);
    public static final Map<UUID, Boolean> MAVUIKA_AURA_NETHERITE = createFieldView(s -> s.mavuikaAuraNetherite, (s, v) -> s.mavuikaAuraNetherite = v);
    public static final Map<UUID, Long> MAVUIKA_AURA_NEXT_STRIKE = createFieldView(s -> s.mavuikaAuraNextStrike, (s, v) -> s.mavuikaAuraNextStrike = v);
    public static final Map<UUID, Float> MAVUIKA_NIGHTSOUL_POINTS = createFieldView(s -> s.mavuikaNightsoulPoints, (s, v) -> s.mavuikaNightsoulPoints = v);
    public static final Map<UUID, Float> MAVUIKA_NIGHTSOUL_FUEL = createFieldView(s -> s.mavuikaNightsoulFuel, (s, v) -> s.mavuikaNightsoulFuel = v);
    public static final Map<UUID, Long> MAVUIKA_CRUCIBLE_UNTIL = createFieldView(s -> s.mavuikaCrucibleUntil, (s, v) -> s.mavuikaCrucibleUntil = v);
    public static final Map<UUID, Float> MAVUIKA_CRUCIBLE_POINTS = createFieldView(s -> s.mavuikaCruciblePoints, (s, v) -> s.mavuikaCruciblePoints = v);
    public static final Map<UUID, Long> MAVUIKA_LAST_NA_FS_TICK = createFieldView(s -> s.mavuikaLastNaFsTick, (s, v) -> s.mavuikaLastNaFsTick = v);
    public static final Map<UUID, Long> MAVUIKA_SKILL_COOLDOWN_UNTIL = createFieldView(s -> s.mavuikaSkillCooldownUntil, (s, v) -> s.mavuikaSkillCooldownUntil = v);
    public static final Map<UUID, Long> MAVUIKA_ULTI_COOLDOWN_UNTIL = createFieldView(s -> s.mavuikaUltiCooldownUntil, (s, v) -> s.mavuikaUltiCooldownUntil = v);
    public static final Map<UUID, Long> RAIDEN_EYE_UNTIL = createFieldView(s -> s.raidenEyeUntil, (s, v) -> s.raidenEyeUntil = v);
    public static final Map<UUID, Boolean> RAIDEN_EYE_NETHERITE = createFieldView(s -> s.raidenEyeNetherite, (s, v) -> s.raidenEyeNetherite = v);
    public static final Map<UUID, Long> RAIDEN_EYE_NEXT_PROC = createFieldView(s -> s.raidenEyeNextProc, (s, v) -> s.raidenEyeNextProc = v);
    public static final Map<UUID, Integer> RAIDEN_RESOLVE = createFieldView(s -> s.raidenResolve, (s, v) -> s.raidenResolve = v);
    public static final Map<UUID, Long> RAIDEN_MUSOU_UNTIL = createFieldView(s -> s.raidenMusouUntil, (s, v) -> s.raidenMusouUntil = v);
    public static final Map<UUID, Boolean> RAIDEN_MUSOU_NETHERITE = createFieldView(s -> s.raidenMusouNetherite, (s, v) -> s.raidenMusouNetherite = v);
    public static final Map<UUID, Long> RAIDEN_SKILL_CD_UNTIL = createFieldView(s -> s.raidenSkillCdUntil, (s, v) -> s.raidenSkillCdUntil = v);
    public static final Map<UUID, Long> RAIDEN_BURST_CD_UNTIL = createFieldView(s -> s.raidenBurstCdUntil, (s, v) -> s.raidenBurstCdUntil = v);
    public static final Map<UUID, Long> RAIDEN_IFRAME_UNTIL = createFieldView(s -> s.raidenIframeUntil, (s, v) -> s.raidenIframeUntil = v);
    public static final Map<UUID, Integer> RAIDEN_NA_COMBO = createFieldView(s -> s.raidenNaCombo, (s, v) -> s.raidenNaCombo = v);
    public static final Map<UUID, Long> RAIDEN_LAST_MELEE_TICK = createFieldView(s -> s.raidenLastMeleeTick, (s, v) -> s.raidenLastMeleeTick = v);
    public static final Map<UUID, Long> RAIDEN_NEXT_SWING_TICK = createFieldView(s -> s.raidenNextSwingTick, (s, v) -> s.raidenNextSwingTick = v);
    public static final Map<UUID, Long> RAIDEN_PENDING_HITOTACHI_TICK = createFieldView(s -> s.raidenPendingHitotachiTick, (s, v) -> s.raidenPendingHitotachiTick = v);
    public static final Map<UUID, Float> RAIDEN_PENDING_HITOTACHI_MULT = createFieldView(s -> s.raidenPendingHitotachiMult, (s, v) -> s.raidenPendingHitotachiMult = v);
    public static final Map<UUID, Long> PETRIFIED_UNTIL = createFieldView(s -> s.petrifiedUntil, (s, v) -> s.petrifiedUntil = v);
    public static final Map<UUID, Long> RAIDEN_LAST_STAFF_RESOLVE_TICK = createFieldView(s -> s.raidenLastStaffResolveTick, (s, v) -> s.raidenLastStaffResolveTick = v);
    public static final Map<UUID, Float> PLAYER_LAST_HP = createFieldView(s -> s.playerLastHp, (s, v) -> s.playerLastHp = v);
    public static final Map<UUID, Float> PRE_ATTACK_STRENGTH = createFieldView(s -> s.preAttackStrength, (s, v) -> s.preAttackStrength = v);
    public static final Map<UUID, Long> PLAYER_HP_CHANGE_BUFF_UNTIL = createFieldView(s -> s.playerHpChangeBuffUntil, (s, v) -> s.playerHpChangeBuffUntil = v);
    public static final Map<UUID, Map<String, Long>> DRACONIC_GLORIES_STACKS = createFieldView(s -> s.draconicGloriesStacks, (s, v) -> s.draconicGloriesStacks = v);
    public static final Map<UUID, Long> LAST_FREEZE_STRUGGLE = createFieldView(s -> s.lastFreezeStruggle, (s, v) -> s.lastFreezeStruggle = v);
    public static final Map<UUID, Long> LAST_ELEMENTAL_ARMOR_WEAR = createFieldView(s -> s.lastElementalArmorWear, (s, v) -> s.lastElementalArmorWear = v);

    // Furina combat state
    public Long furinaFanfareUntil;
    public Integer furinaFanfarePoints;
    public Boolean furinaFanfareNetherite;
    public Float playerLastHp;
    public Long playerHpChangeBuffUntil;

    // Zhongli combat state
    public Long jadeShieldUntil;
    public Boolean jadeShieldNetherite;
    public Long petrifiedUntil;

    // Layla combat state
    public Long nightsFocusUntil;
    public Boolean nightsFocusNetherite;
    public Integer nightsFocusStars;
    public Long nightsFocusLastAutoStar;
    public Long nightsFocusLastSkillStar;
    public Float nightsFocusBaseHp;
    public Integer nightsFocusPendingShots;
    public Long laylaDreamsphereUntil;
    public Boolean laylaDreamsphereNetherite;
    public Long laylaDreamsphereNextShot;

    // Mavuika combat state
    public Long mavuikaAuraUntil;
    public Boolean mavuikaAuraNetherite;
    public Long mavuikaAuraNextStrike;
    public Float mavuikaNightsoulPoints;
    public Float mavuikaNightsoulFuel;
    public Long mavuikaCrucibleUntil;
    public Float mavuikaCruciblePoints;
    public Long mavuikaLastNaFsTick;
    public Long mavuikaSkillCooldownUntil;
    public Long mavuikaUltiCooldownUntil;

    // Raiden combat state
    public Long raidenEyeUntil;
    public Boolean raidenEyeNetherite;
    public Long raidenEyeNextProc;
    public Integer raidenResolve;
    public Long raidenMusouUntil;
    public Boolean raidenMusouNetherite;
    public Long raidenSkillCdUntil;
    public Long raidenBurstCdUntil;
    public Long raidenIframeUntil;
    public Integer raidenNaCombo;
    public Long raidenLastMeleeTick;
    public Long raidenNextSwingTick;
    public Long raidenPendingHitotachiTick;
    public Float raidenPendingHitotachiMult;
    public Long raidenLastStaffResolveTick;

    // Neuvillette & general combat state
    public Map<String, Long> draconicGloriesStacks;
    public Float preAttackStrength;
    public Long lastElementalArmorWear;
    public Long lastFreezeStruggle;

    public PlayerGenshinState() {}

    public static PlayerGenshinState get(UUID id) {
        return STATES.computeIfAbsent(id, k -> new PlayerGenshinState());
    }

    public static PlayerGenshinState getOrNull(UUID id) {
        return id != null ? STATES.get(id) : null;
    }

    public static void remove(UUID id) {
        if (id != null) {
            STATES.remove(id);
        }
    }

    public static void onEntityRemoved(Entity entity) {
        if (entity != null) {
            if (entity instanceof ServerPlayer sp && sp.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION) {
                onPlayerChangedDimension(sp);
                return;
            }
            STATES.remove(entity.getUUID());
            if (entity instanceof LivingEntity living) {
                ElementalGaugeManager.clearEntityState(living);
            }
        }
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        if (player == null) return;
        UUID id = player.getUUID();
        ArchonUltiManager.clearDimensionBoundState(id);
        ArchonUltiManager.resetAllArchonIdleTimers(player);
        NeuvilletteManager.clearPlayerState(id);
        PlungeAndGliderManager.clearPlayerState(id);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public static void pruneInactive(MinecraftServer server) {
        if (server == null || STATES.isEmpty()) return;
        STATES.keySet().removeIf(id -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null && sp.isAlive() && !sp.isRemoved()) return false;
            for (ServerLevel lvl : server.getAllLevels()) {
                Entity e = lvl.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                    return false;
                }
            }
            return true;
        });
    }

    public static <V> Map<UUID, V> createFieldView(
            Function<PlayerGenshinState, V> getter,
            BiConsumer<PlayerGenshinState, V> setter
    ) {
        return new FieldMapView<>(getter, setter);
    }

    private static final class FieldMapView<V> extends AbstractMap<UUID, V> {
        private final Function<PlayerGenshinState, V> getter;
        private final BiConsumer<PlayerGenshinState, V> setter;

        FieldMapView(Function<PlayerGenshinState, V> getter, BiConsumer<PlayerGenshinState, V> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public V get(Object key) {
            if (!(key instanceof UUID id)) return null;
            PlayerGenshinState st = STATES.get(id);
            return st != null ? getter.apply(st) : null;
        }

        @Override
        public V getOrDefault(Object key, V defaultValue) {
            V v = get(key);
            return v != null ? v : defaultValue;
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public V computeIfAbsent(UUID key, Function<? super UUID, ? extends V> mappingFunction) {
            if (key == null) return null;
            PlayerGenshinState st = STATES.computeIfAbsent(key, k -> new PlayerGenshinState());
            V cur = getter.apply(st);
            if (cur == null) {
                V newVal = mappingFunction.apply(key);
                if (newVal != null) {
                    setter.accept(st, newVal);
                }
                return newVal;
            }
            return cur;
        }

        @Override
        public V put(UUID key, V value) {
            if (key == null) return null;
            if (value == null) return remove(key);
            PlayerGenshinState st = STATES.computeIfAbsent(key, k -> new PlayerGenshinState());
            V prev = getter.apply(st);
            setter.accept(st, value);
            return prev;
        }

        @Override
        public V remove(Object key) {
            if (!(key instanceof UUID id)) return null;
            PlayerGenshinState st = STATES.get(id);
            if (st == null) return null;
            V prev = getter.apply(st);
            setter.accept(st, null);
            return prev;
        }

        @Override
        public void clear() {
            for (PlayerGenshinState st : STATES.values()) {
                setter.accept(st, null);
            }
        }

        @Override
        public Set<Entry<UUID, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<UUID, V>> iterator() {
                    Iterator<Entry<UUID, PlayerGenshinState>> baseIt = STATES.entrySet().iterator();
                    return new Iterator<>() {
                        private Entry<UUID, PlayerGenshinState> nextEntry;
                        private Entry<UUID, PlayerGenshinState> currentEntry;

                        private void advance() {
                            nextEntry = null;
                            while (baseIt.hasNext()) {
                                Entry<UUID, PlayerGenshinState> cand = baseIt.next();
                                if (getter.apply(cand.getValue()) != null) {
                                    nextEntry = cand;
                                    break;
                                }
                            }
                        }

                        {
                            advance();
                        }

                        @Override
                        public boolean hasNext() {
                            return nextEntry != null;
                        }

                        @Override
                        public Entry<UUID, V> next() {
                            currentEntry = nextEntry;
                            advance();
                            final UUID k = currentEntry.getKey();
                            final PlayerGenshinState st = currentEntry.getValue();
                            return new SimpleEntry<>(k, getter.apply(st)) {
                                @Override
                                public V setValue(V newVal) {
                                    V old = getter.apply(st);
                                    setter.accept(st, newVal);
                                    return old;
                                }
                            };
                        }

                        @Override
                        public void remove() {
                            if (currentEntry != null) {
                                setter.accept(currentEntry.getValue(), null);
                            }
                        }
                    };
                }

                @Override
                public int size() {
                    int c = 0;
                    for (PlayerGenshinState st : STATES.values()) {
                        if (getter.apply(st) != null) c++;
                    }
                    return c;
                }
            };
        }
    }
}
