package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Guardian;

/**
 * Manages elemental potion/arrow/cloud extraction, elemental enchantments
 * (Elemental Mastery, Elemental Protection, Thorns reflection), and imbued sword hits.
 */
public final class PotionAndEnchantmentManager {

    private PotionAndEnchantmentManager() {}

    public static @Nullable Element getElementForEffect(Holder<MobEffect> effectHolder) {
        if (effectHolder == null) return null;
        if (effectHolder.is(MobEffects.MOVEMENT_SPEED) || effectHolder.is(MobEffects.WIND_CHARGED)) {
            return Element.ANEMO;
        }
        if (effectHolder.is(MobEffects.MOVEMENT_SLOWDOWN)) {
            return Element.CRYO;
        }
        if (effectHolder.is(MobEffects.DAMAGE_RESISTANCE) || effectHolder.is(MobEffects.INFESTED)) {
            return Element.GEO;
        }
        if (effectHolder.is(MobEffects.POISON) || effectHolder.is(MobEffects.OOZING)) {
            return Element.DENDRO;
        }
        return null;
    }

    public static List<Element> getElementsFromPotionContents(PotionContents contents) {
        if (contents == null || contents == PotionContents.EMPTY) {
            return Collections.emptyList();
        }
        List<Element> result = new ArrayList<>();
        if (contents.is(Potions.WATER)) {
            result.add(Element.HYDRO);
        }
        for (MobEffectInstance inst : contents.getAllEffects()) {
            Element mapped = getElementForEffect(inst.getEffect());
            if (mapped != null && !result.contains(mapped)) {
                result.add(mapped);
            } else if (inst.getEffect().is(MobEffects.HARM) && !result.contains(Element.ELECTRO)) {
                result.add(Element.ELECTRO);
            }
        }
        return result;
    }

    public static List<Element> getElementsFromPotionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return getElementsFromPotionContents(contents);
    }

    public static List<Element> getElementsFromArrow(ServerLevel level, Arrow arrow) {
        if (arrow == null) return Collections.emptyList();
        try {
            CompoundTag nbt = new CompoundTag();
            arrow.saveWithoutId(nbt);
            if (nbt.contains("item", 10)) {
                ItemStack arrowStack = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("item"));
                List<Element> fromItem = getElementsFromPotionStack(arrowStack);
                if (!fromItem.isEmpty()) return fromItem;
            }
            if (nbt.contains("potion_contents")) {
                PotionContents contents = PotionContents.CODEC.parse(
                        level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                        nbt.get("potion_contents")
                ).result().orElse(PotionContents.EMPTY);
                List<Element> fromTag = getElementsFromPotionContents(contents);
                if (!fromTag.isEmpty()) return fromTag;
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    public static List<Element> getElementsFromAreaEffectCloud(ServerLevel level, AreaEffectCloud cloud) {
        if (cloud == null) return Collections.emptyList();
        try {
            CompoundTag nbt = new CompoundTag();
            cloud.saveWithoutId(nbt);
            if (nbt.contains("potion_contents")) {
                PotionContents contents = PotionContents.CODEC.parse(
                        level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                        nbt.get("potion_contents")
                ).result().orElse(PotionContents.EMPTY);
                return getElementsFromPotionContents(contents);
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    public static void handleTippedArrowHit(ServerLevel level, Arrow arrow, LivingEntity target) {
        if (arrow == null || target == null || !target.isAlive() || arrow.getTags().contains("velorise_staves:arrow_elem_applied")) {
            return;
        }
        List<Element> elements = getElementsFromArrow(level, arrow);
        if (elements.isEmpty()) return;
        arrow.addTag("velorise_staves:arrow_elem_applied");
        ServerPlayer shooter = arrow.getOwner() instanceof ServerPlayer sp ? sp : null;
        for (Element elem : elements) {
            applyElementBypassIcd(level, target, elem, 1.0F, shooter, 0.0F);
        }
    }

    public static void handleItemUseFinish(ServerLevel level, LivingEntity entity, ItemStack stack) {
        if (level == null || entity == null || !entity.isAlive() || stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof PotionItem) && !stack.has(DataComponents.POTION_CONTENTS)) return;
        List<Element> elements = getElementsFromPotionStack(stack);
        if (elements.isEmpty()) return;
        ServerPlayer player = entity instanceof ServerPlayer sp ? sp : null;
        for (Element elem : elements) {
            applyElementBypassIcd(level, entity, elem, 1.0F, player, 0.0F);
        }
    }

    public static int getMaceWindBurstLevel(LivingEntity attacker) {
        if (attacker == null) return 0;
        ItemStack mainHand = attacker.getMainHandItem();
        if (mainHand.isEmpty() || !mainHand.is(Items.MACE)) return 0;
        var enchantments = mainHand.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(Enchantments.WIND_BURST)
                    || entry.getKey().unwrapKey().map(k -> k.location().getPath().equals("wind_burst")).orElse(false)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static int getElementalMasteryLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().map(k -> k.location().getPath().equals("elemental_mastery")).orElse(false)) {
                return Math.min(3, Math.max(0, entry.getIntValue()));
            }
        }
        return 0;
    }

    public static int getElementalMasteryLevel(@Nullable LivingEntity attacker) {
        if (attacker == null) return 0;
        int mainLvl = getElementalMasteryLevel(attacker.getMainHandItem());
        int base = mainLvl > 0 ? mainLvl : getElementalMasteryLevel(attacker.getOffhandItem());
        int bonus = ElementalResonanceManager.getBonusElementalMasteryLevels(attacker)
                + NahidaManager.getBonusEmLevels(attacker);
        if (attacker instanceof Player p) {
            Element heldElem = p.getMainHandItem().getItem() instanceof ArcaneStaffItem s ? s.getElement() : null;
            bonus += TsaritsaManager.getBonusEmLevels(p, heldElem);
        }
        return base + bonus;
    }

    public static int getElementalProtectionLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().map(k -> k.location().getPath().equals("elemental_protection")).orElse(false)) {
                return Math.min(4, Math.max(0, entry.getIntValue()));
            }
        }
        return 0;
    }

    public static int getElementalProtectionTotalLevel(@Nullable LivingEntity entity) {
        if (entity == null) return 0;
        int total = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            total += getElementalProtectionLevel(armor);
        }
        return Math.min(16, total);
    }

    public static float applyElementalProtectionAndArmorWear(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0.0F || entity == null || !entity.isAlive()) return amount;
        boolean isElem = (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource)
                || "velorise_elemental".equals(source.type().msgId())
                || isElementalOrMagicDamage(source);
        if (!isElem) return amount;

        int epTotal = getElementalProtectionTotalLevel(entity);
        if (epTotal > 0) {
            float reduction = Math.min(0.80F, epTotal * 0.05F);
            amount *= (1.0F - reduction);
        }

        // Configurable armor durability wear on elemental damage ("auto" = Hard difficulty, "always", "never")
        if (!entity.level().isClientSide()) {
            String wearMode = StavesConfig.ELEMENTAL_ARMOR_DURABILITY_WEAR.get();
            boolean shouldWear = "always".equalsIgnoreCase(wearMode)
                    || ("auto".equalsIgnoreCase(wearMode) && entity.level().getDifficulty() == net.minecraft.world.Difficulty.HARD);
            if (shouldWear) {
                long now = entity.level().getGameTime();
                Long lastWear = LAST_ELEMENTAL_ARMOR_WEAR.get(entity.getUUID());
                if (lastWear == null || now < lastWear || (now - lastWear) >= 20L) {
                    LAST_ELEMENTAL_ARMOR_WEAR.put(entity.getUUID(), now);
                    net.minecraft.world.entity.EquipmentSlot[] slots = {
                            net.minecraft.world.entity.EquipmentSlot.HEAD,
                            net.minecraft.world.entity.EquipmentSlot.CHEST,
                            net.minecraft.world.entity.EquipmentSlot.LEGS,
                            net.minecraft.world.entity.EquipmentSlot.FEET
                    };
                    for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
                        ItemStack piece = entity.getItemBySlot(slot);
                        if (!piece.isEmpty() && piece.isDamageableItem()) {
                            piece.hurtAndBreak(1, entity, slot);
                        }
                    }
                }
            }
        }
        return amount;
    }

    public static int getThornsEnchantmentLevel(LivingEntity entity) {
        if (entity == null) return 0;
        int maxLevel = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.isEmpty()) continue;
            var enchantments = armor.getEnchantments();
            for (var entry : enchantments.entrySet()) {
                int lvl = entry.getIntValue();
                if (lvl > maxLevel
                        && (entry.getKey().is(Enchantments.THORNS)
                                || entry.getKey().unwrapKey().map(k -> k.location().getPath().equals("thorns")).orElse(false))) {
                    maxLevel = lvl;
                }
            }
        }
        return maxLevel;
    }

    public static boolean hasThornsEnchantment(LivingEntity entity) {
        return getThornsEnchantmentLevel(entity) > 0;
    }

    public static Element pickDefenderAuraForThorns(LivingEntity defender) {
        if (defender == null) return null;
        UUID id = defender.getUUID();
        Map<Element, GaugeState> gauges = GAUGES.get(id);
        if (gauges != null && !gauges.isEmpty()) {
            for (Element elem : REACTION_PRIORITY_ORDER) {
                GaugeState gs = gauges.get(elem);
                if (gs != null && !gs.isExpired()) {
                    return elem;
                }
            }
            for (Map.Entry<Element, GaugeState> entry : gauges.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isExpired()) {
                    return entry.getKey();
                }
            }
        }
        GaugeState frozen = FROZEN_GAUGES.get(id);
        if (frozen != null && !frozen.isExpired()) {
            return Element.CRYO;
        }
        GaugeState quicken = QUICKEN_GAUGES.get(id);
        if (quicken != null && !quicken.isExpired()) {
            return Element.DENDRO;
        }
        return null;
    }

    public static void reflectThornsElement(
            ServerLevel level,
            LivingEntity defender,
            LivingEntity attacker,
            long now,
            @Nullable Element fallbackElement
    ) {
        if (level == null || defender == null || attacker == null
                || defender == attacker || !attacker.isAlive() || attacker.isRemoved()
                || IS_PROCESSING_THORNS.get()) {
            return;
        }

        Element reflected = getInnateElement(defender);
        if (reflected == null) {
            reflected = pickDefenderAuraForThorns(defender);
        }
        if (reflected == null) {
            reflected = fallbackElement;
        }
        if (reflected == null && defender.getRemainingFireTicks() > 0 && !defender.fireImmune() && !isEntityInWater(defender)) {
            reflected = Element.PYRO;
        }
        if (reflected == null) {
            return;
        }

        long dedupKey = (((long) attacker.getId()) << 32) | (defender.getId() & 0xFFFFFFFFL);
        Long lastTick = LAST_THORNS_REFLECT.get(dedupKey);
        if (lastTick != null && now >= lastTick && (now - lastTick) < 2L) {
            return;
        }
        LAST_THORNS_REFLECT.put(dedupKey, now);

        int thornsLevel = getThornsEnchantmentLevel(defender);
        if (thornsLevel <= 0) {
            thornsLevel = 1; // e.g., Guardian natural Thorns spikes
        }
        float reflectU = 0.5F * thornsLevel;

        IS_PROCESSING_THORNS.set(true);
        try {
            ServerPlayer defenderPlayer = defender instanceof ServerPlayer sp ? sp : null;
            applyElementBypassIcd(level, attacker, reflected, reflectU, defenderPlayer, 0.0F);
            Vec3 hitPos = attacker.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPos.x, hitPos.y, hitPos.z,
                    6, 0.25D, 0.25D, 0.25D, 0.05D);
        } finally {
            IS_PROCESSING_THORNS.set(false);
        }
    }

    public static void recordPreAttackStrength(@Nullable Player player, float scale) {
        if (player != null) {
            PRE_ATTACK_STRENGTH.put(player.getUUID(), scale);
        }
    }

    public static void clearPreAttackStrength(@Nullable Player player) {
        if (player != null) {
            PRE_ATTACK_STRENGTH.remove(player.getUUID());
        }
    }

    public static float getEffectiveAttackStrengthScale(Player attacker) {
        Float recorded = PRE_ATTACK_STRENGTH.get(attacker.getUUID());
        float current = attacker.getAttackStrengthScale(0.5F);
        return recorded != null ? Math.max(recorded, current) : current;
    }

    public static float applyImbuedSwordHit(LivingEntity target, DamageSource source, float incomingDamage) {
        if (target == null || !target.isAlive() || incomingDamage <= 0.0F) return incomingDamage;
        if (!(target.level() instanceof ServerLevel level)) return incomingDamage;
        if (Boolean.TRUE.equals(IS_PROCESSING_SWORD_IMBUE.get())) return incomingDamage;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return incomingDamage;
        if (!(source.getDirectEntity() instanceof ServerPlayer attacker)) return incomingDamage;
        if (attacker == target) return incomingDamage;

        ItemStack weapon = attacker.getMainHandItem();
        boolean hasFireAspect = com.velorise.staves.item.SwordImbueHelper.hasFireAspect(weapon);
        String imbueKey = com.velorise.staves.item.SwordImbueHelper.getImbuedKey(weapon);
        if (imbueKey == null && !hasFireAspect) {
            return incomingDamage;
        }
        if (com.velorise.staves.item.SwordImbueHelper.isNonPyroImbued(weapon) && hasFireAspect) {
            ensurePreDeathPyroIgnite(target, 4);
            return incomingDamage;
        }

        if (getEffectiveAttackStrengthScale(attacker) < 0.9F) {
            if (hasFireAspect) {
                ensurePreDeathPyroIgnite(target, 4);
            }
            return incomingDamage;
        }

        Element chosen;
        if (imbueKey == null) {
            chosen = Element.PYRO; // Non-imbued Fire Aspect weapon applies 1U Pyro on full-charge swing
        } else if (com.velorise.staves.item.SwordImbueHelper.OMNI_KEY.equals(imbueKey)) {
            chosen = com.velorise.staves.entity.OmniOrbEntity.chooseBestElementForTarget(target, attacker);
        } else {
            chosen = com.velorise.staves.item.SwordImbueHelper.getImbuedElement(weapon);
        }
        if (chosen == null) return incomingDamage;

        float[] outDamage = new float[] { incomingDamage };
        IS_PROCESSING_SWORD_IMBUE.set(true);
        try {
            applyElement(level, target, chosen, 1.0F, attacker, incomingDamage, true, outDamage);
        } finally {
            IS_PROCESSING_SWORD_IMBUE.set(false);
        }
        if ((chosen == Element.PYRO || hasFireAspect) && !hasElement(target, Element.ELECTRO) && !hasInnateElement(target, Element.ELECTRO)) {
            ensurePreDeathPyroIgnite(target, 4);
        }
        return outDamage[0];
    }
}
