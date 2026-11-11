package com.velorise.staves.element;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Formats localized death messages for elemental reactions and innate/mutated elemental mobs,
 * upgrading vanilla CombatTracker entries without mutating mob nametags.
 */
public final class ElementalDeathMessageHelper {
    public record RecentReactionInfo(
            ElementalReaction reaction,
            @Nullable Element element,
            @Nullable UUID attackerUuid,
            long tick
    ) {}

    private static final Map<UUID, RecentReactionInfo> RECENT_REACTIONS = new ConcurrentHashMap<>();
    private static final Field COMBAT_TRACKER_ENTRIES_FIELD = resolveCombatEntriesField();

    private ElementalDeathMessageHelper() {}

    private static @Nullable Field resolveCombatEntriesField() {
        for (Field f : CombatTracker.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    public static void clearEntityState(@Nullable LivingEntity entity) {
        if (entity != null) {
            RECENT_REACTIONS.remove(entity.getUUID());
        }
    }

    public static void clearAll() {
        RECENT_REACTIONS.clear();
    }

    public static void cleanupStale(long now) {
        if (RECENT_REACTIONS.size() > 64 || now % 100L == 0L) {
            RECENT_REACTIONS.entrySet().removeIf(e -> now < e.getValue().tick() || (now - e.getValue().tick()) > 100L);
        }
    }

    public static void recordReactionForDeath(
            @Nullable LivingEntity target,
            @Nullable ElementalReaction reaction,
            @Nullable Element element,
            @Nullable LivingEntity attacker
    ) {
        if (target == null || target.level().isClientSide()) return;
        if (reaction == null || reaction == ElementalReaction.NONE || reaction == ElementalReaction.IMMUNE) return;
        UUID attackerId = (attacker != null && attacker != target) ? attacker.getUUID() : null;
        RECENT_REACTIONS.put(target.getUUID(), new RecentReactionInfo(reaction, element, attackerId, target.level().getGameTime()));
    }

    public static DamageSource createDamageSource(
            ServerLevel level,
            @Nullable LivingEntity attacker,
            @Nullable ElementalReaction reaction,
            @Nullable Element element
    ) {
        DamageSource base = attacker != null
                ? level.damageSources().indirectMagic(attacker, attacker)
                : level.damageSources().magic();
        return new ElementalReactionDamageSource(
                base.typeHolder(), base.getDirectEntity(), base.getEntity(), base.sourcePositionRaw(),
                reaction, element, null
        );
    }

    public static DamageSource createTypedDamageSource(
            DamageSource base,
            @Nullable LivingEntity attacker,
            @Nullable ElementalReaction reaction,
            @Nullable Element element
    ) {
        Entity direct = base.getDirectEntity() != null ? base.getDirectEntity() : attacker;
        Entity causing = base.getEntity() != null ? base.getEntity() : attacker;
        return new ElementalReactionDamageSource(
                base.typeHolder(), direct, causing, base.sourcePositionRaw(),
                reaction, element, base
        );
    }

    public static boolean isInnateOrMutatedMob(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living) || living instanceof Player) return false;
        return ElementalGaugeManager.isOmniInnate(living) || !ElementalGaugeManager.getInnateElements(living).isEmpty();
    }

    public static Component formatKillerDisplayName(Entity killer) {
        Component base = killer.getDisplayName();
        if (!(killer instanceof LivingEntity living) || living instanceof Player) {
            return base;
        }
        net.minecraft.network.chat.MutableComponent elemComp = null;
        if (ElementalGaugeManager.isOmniInnate(living)) {
            elemComp = Component.translatable("element.velorise_staves.omni").withStyle(ChatFormatting.GOLD);
        } else {
            java.util.EnumSet<Element> innates = ElementalGaugeManager.getInnateElements(living);
            for (Element innate : innates) {
                net.minecraft.network.chat.MutableComponent part = Component.translatable(innate.translationKey()).withStyle(innate.color());
                if (elemComp == null) {
                    elemComp = part;
                } else {
                    elemComp = elemComp.append(Component.literal("-").withStyle(ChatFormatting.WHITE)).append(part);
                }
            }
        }
        if (elemComp == null) return base;
        if (living.hasCustomName() && base.getString().toLowerCase(Locale.ROOT).contains(elemComp.getString().toLowerCase(Locale.ROOT))) {
            return base;
        }
        return Component.translatable("death.velorise_staves.innate_mob_name", elemComp, base).withStyle(base.getStyle());
    }

    @SuppressWarnings("unchecked")
    public static void enhanceLivingDeath(@Nullable LivingEntity killed) {
        if (killed == null || killed.level().isClientSide() || COMBAT_TRACKER_ENTRIES_FIELD == null) return;
        try {
            Object raw = COMBAT_TRACKER_ENTRIES_FIELD.get(killed.getCombatTracker());
            if (raw instanceof List<?> list) {
                ServerLevel sl = killed.level() instanceof ServerLevel serverLevel ? serverLevel : null;
                enhanceCombatTrackerEntries(sl, killed, (List<CombatEntry>) list);
            }
        } catch (Throwable ignored) {}
    }

    public static void enhanceCombatTrackerEntries(
            @Nullable ServerLevel level,
            @Nullable LivingEntity killed,
            @Nullable List<CombatEntry> entries
    ) {
        if (killed == null || entries == null || entries.isEmpty()) return;
        int lastIdx = entries.size() - 1;
        CombatEntry last = entries.get(lastIdx);
        DamageSource src = last.source();
        if (src == null) return;

        long now = killed.level().getGameTime();
        RecentReactionInfo recent = RECENT_REACTIONS.get(killed.getUUID());
        boolean hasFreshRecent = recent != null && now >= recent.tick() && (now - recent.tick()) <= 2L;

        if (src instanceof ElementalReactionDamageSource erds) {
            if ((erds.getReaction() == null || erds.getReaction() == ElementalReaction.NONE) && hasFreshRecent) {
                LivingEntity credit = resolveCredit(level, erds.getEntity(), recent.attackerUuid());
                DamageSource upgraded = createTypedDamageSource(
                        src, credit, recent.reaction(),
                        recent.element() != null ? recent.element() : erds.getElement()
                );
                entries.set(lastIdx, new CombatEntry(upgraded, last.damage(), last.fallLocation(), last.fallDistance()));
            }
            return;
        }

        if (src.is(DamageTypeTags.IS_FIRE)) {
            if (hasFreshRecent && (recent.reaction() == ElementalReaction.FORWARD_MELT
                    || recent.reaction() == ElementalReaction.REVERSE_VAPORIZE
                    || recent.reaction() == ElementalReaction.BURNING)) {
                LivingEntity credit = resolveCredit(level, src.getEntity(), recent.attackerUuid());
                DamageSource upgraded = createTypedDamageSource(src, credit, recent.reaction(), Element.PYRO);
                entries.set(lastIdx, new CombatEntry(upgraded, last.damage(), last.fallLocation(), last.fallDistance()));
                return;
            }
            if (ElementalGaugeManager.hasBurning(killed.getUUID())) {
                UUID burnAttackerId = ElementalGaugeManager.getBurningAttackerUuid(killed.getUUID());
                LivingEntity credit = resolveCredit(level, src.getEntity(), burnAttackerId);
                DamageSource upgraded = createTypedDamageSource(src, credit, ElementalReaction.BURNING, Element.PYRO);
                entries.set(lastIdx, new CombatEntry(upgraded, last.damage(), last.fallLocation(), last.fallDistance()));
                return;
            }
        }

        if (hasFreshRecent && (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.FREEZE))) {
            LivingEntity credit = resolveCredit(level, src.getEntity(), recent.attackerUuid());
            DamageSource upgraded = createTypedDamageSource(src, credit, recent.reaction(), recent.element());
            entries.set(lastIdx, new CombatEntry(upgraded, last.damage(), last.fallLocation(), last.fallDistance()));
            return;
        }

        Entity credit = src.getEntity() != null ? src.getEntity() : src.getDirectEntity();
        if (isInnateOrMutatedMob(credit)) {
            DamageSource upgraded = new ElementalReactionDamageSource(
                    src.typeHolder(), src.getDirectEntity(), src.getEntity(), src.sourcePositionRaw(),
                    null, null, src
            );
            entries.set(lastIdx, new CombatEntry(upgraded, last.damage(), last.fallLocation(), last.fallDistance()));
        }
    }

    private static @Nullable LivingEntity resolveCredit(
            @Nullable ServerLevel level,
            @Nullable Entity existingCredit,
            @Nullable UUID storedCreditId
    ) {
        if (existingCredit instanceof LivingEntity living) return living;
        if (level != null && storedCreditId != null) {
            Entity resolved = level.getEntity(storedCreditId);
            if (resolved instanceof LivingEntity living) return living;
        }
        return null;
    }

    public static final class ElementalReactionDamageSource extends DamageSource {
        private final @Nullable Vec3 storedSourcePos;
        private final @Nullable ElementalReaction reaction;
        private final @Nullable Element element;
        private final @Nullable DamageSource delegate;

        public ElementalReactionDamageSource(
                Holder<DamageType> type,
                @Nullable Entity directEntity,
                @Nullable Entity causingEntity,
                @Nullable Vec3 damageSourcePosition,
                @Nullable ElementalReaction reaction,
                @Nullable Element element,
                @Nullable DamageSource delegate
        ) {
            super(type, directEntity, causingEntity);
            this.storedSourcePos = damageSourcePosition;
            this.reaction = reaction;
            this.element = element;
            this.delegate = delegate;
        }

        @Override
        public @Nullable Vec3 sourcePositionRaw() {
            return this.storedSourcePos != null ? this.storedSourcePos : super.sourcePositionRaw();
        }

        @Override
        public @Nullable Vec3 getSourcePosition() {
            if (this.storedSourcePos != null) {
                return this.storedSourcePos;
            }
            return super.getSourcePosition();
        }

        public @Nullable ElementalReaction getReaction() {
            return reaction;
        }

        public @Nullable Element getElement() {
            return element;
        }

        public boolean isExplosionReaction() {
            return reaction == ElementalReaction.OVERLOADED
                    || reaction == ElementalReaction.BLOOM
                    || reaction == ElementalReaction.REVERSE_BLOOM
                    || reaction == ElementalReaction.BURGEON;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity killedEntity) {
            Entity causing = this.getEntity();
            Entity direct = this.getDirectEntity();
            Entity rawKiller = causing != null ? causing : direct;
            Component killerComp = (rawKiller != null && rawKiller != killedEntity)
                    ? formatKillerDisplayName(rawKiller)
                    : null;

            if (reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
                String suffix = switch (reaction) {
                    case OVERLOADED, OVERVAPE -> "overloaded";
                    case ELECTRO_CHARGED -> "electro_charged";
                    case BURNING -> "burning";
                    case SUPERCONDUCT -> "superconduct";
                    case SWIRL -> "swirl";
                    case SHATTER -> "shatter";
                    case BLOOM, REVERSE_BLOOM -> "bloom";
                    case HYPERBLOOM -> "hyperbloom";
                    case BURGEON -> "burgeon";
                    default -> null;
                };
                String baseKey = suffix != null
                        ? "death.velorise_staves.reaction." + suffix
                        : "death.velorise_staves.reaction";
                Component reactComp = reaction.translationKey().isEmpty()
                        ? Component.literal(reaction.reactionName())
                        : Component.translatable(reaction.translationKey());
                ChatFormatting reactColor = switch (reaction) {
                    case OVERLOADED, OVERVAPE, BURNING, BURGEON, FORWARD_MELT, REVERSE_MELT -> ChatFormatting.RED;
                    case ELECTRO_CHARGED, HYPERBLOOM, QUICKEN, AGGRAVATE -> ChatFormatting.LIGHT_PURPLE;
                    case BLOOM, REVERSE_BLOOM, SPREAD -> ChatFormatting.GREEN;
                    case SUPERCONDUCT, FROZEN, SHATTER -> ChatFormatting.AQUA;
                    case SWIRL -> ChatFormatting.DARK_AQUA;
                    case FORWARD_VAPORIZE, REVERSE_VAPORIZE -> ChatFormatting.BLUE;
                    default -> ChatFormatting.GOLD;
                };
                Component styledReact = reactComp.copy().withStyle(reactColor);
                if (killerComp != null) {
                    return Component.translatable(baseKey + ".player", killedEntity.getDisplayName(), styledReact, killerComp);
                }
                return Component.translatable(baseKey, killedEntity.getDisplayName(), styledReact);
            }

            if (element != null && delegate == null) {
                Component elemComp = Component.translatable(element.translationKey()).withStyle(element.color());
                if (killerComp != null) {
                    return Component.translatable("death.velorise_staves.element.player", killedEntity.getDisplayName(), elemComp, killerComp);
                }
                return Component.translatable("death.velorise_staves.element", killedEntity.getDisplayName(), elemComp);
            }

            if (rawKiller != null && isInnateOrMutatedMob(rawKiller)) {
                Component innateName = formatKillerDisplayName(rawKiller);
                String attackKey = "death.attack." + this.getMsgId();
                ItemStack weapon = causing instanceof LivingEntity livingKiller ? livingKiller.getMainHandItem() : ItemStack.EMPTY;
                if (!weapon.isEmpty() && weapon.has(DataComponents.CUSTOM_NAME)) {
                    return Component.translatable(attackKey + ".item", killedEntity.getDisplayName(), innateName, weapon.getDisplayName());
                }
                return Component.translatable(attackKey, killedEntity.getDisplayName(), innateName);
            }

            if (delegate != null) {
                return delegate.getLocalizedDeathMessage(killedEntity);
            }
            return super.getLocalizedDeathMessage(killedEntity);
        }
    }
}
