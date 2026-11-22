package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages innate elemental mob affinities, mutations, Omni-Innate behavior,
 * elemental essence drops, and Wandering Trader elemental offers.
 */
public final class InnateMobManager {

    private InnateMobManager() {}

    private static final Map<UUID, UUID> TRADER_LAST_CUSTOMER = new ConcurrentHashMap<>();

    public static void clearEntity(UUID id) {
        TRADER_LAST_CUSTOMER.remove(id);
    }

    public static void clearAll() {
        TRADER_LAST_CUSTOMER.clear();
    }

    public static boolean isHostileMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof Npc) return false;
        if (entity instanceof Animal) return false;
        if (entity instanceof AbstractGolem) return false;
        if (isBossEntity(entity) || isPassiveAquaticOrAmbient(entity)) return false;
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    public static void tryPopulateWanderingTraderOffers(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.npc.WanderingTrader trader) || entity.level().isClientSide()) return;
        if (trader.getTags().contains("VeloriseTraderChecked")) return;
        trader.addTag("VeloriseTraderChecked");

        // 50% chance for a spawned Wandering Trader to offer 2 to 3 Elemental Essence slots
        if (trader.getRandom().nextDouble() >= 0.50D) return;

        net.minecraft.world.item.trading.MerchantOffers offers = trader.getOffers();
        if (offers == null) return;

        int slotCount = 2 + trader.getRandom().nextInt(2); // 2 to 3 slots
        Element[] allElements = Element.values();
        Set<net.minecraft.world.item.Item> addedItems = new HashSet<>();

        for (int s = 0; s < slotCount; s++) {
            // 7 parts Normal Element (87.5%) : 1 part Omni Element (12.5%)
            boolean rollOmni = (trader.getRandom().nextInt(8) == 0);
            net.minecraft.world.item.Item chosenItem = null;
            int emeraldPrice;
            int xpReward;

            if (rollOmni && !addedItems.contains(VeloriseStavesMod.getElementItem(null))) {
                chosenItem = VeloriseStavesMod.getElementItem(null);
                emeraldPrice = 48 + trader.getRandom().nextInt(17); // 48 - 64 Emeralds
                xpReward = 25;
            } else {
                for (int attempt = 0; attempt < 10; attempt++) {
                    Element pick = allElements[trader.getRandom().nextInt(allElements.length)];
                    net.minecraft.world.item.Item candidate = VeloriseStavesMod.getElementItem(pick);
                    if (candidate != null && !addedItems.contains(candidate)) {
                        chosenItem = candidate;
                        break;
                    }
                }
                if (chosenItem == null) {
                    chosenItem = VeloriseStavesMod.getElementItem(allElements[s % allElements.length]);
                }
                emeraldPrice = 24 + trader.getRandom().nextInt(9); // 24 - 32 Emeralds
                xpReward = 12;
            }

            if (chosenItem != null && addedItems.add(chosenItem)) {
                offers.add(new net.minecraft.world.item.trading.MerchantOffer(new ItemStack(Items.EMERALD, emeraldPrice), new ItemStack(chosenItem, 1), 1, xpReward, 0.05F));
            }
        }
    }

    public static void checkWanderingTraderProgress(ServerLevel level, net.minecraft.world.entity.npc.WanderingTrader trader) {
        if (level == null || trader == null || !trader.isAlive()) return;
        tryPopulateWanderingTraderOffers(trader);
        if (trader.getTradingPlayer() instanceof ServerPlayer activeCustomer) {
            TRADER_LAST_CUSTOMER.put(trader.getUUID(), activeCustomer.getUUID());
        }
        net.minecraft.world.item.trading.MerchantOffers offers = trader.getOffers();
        if (offers == null || offers.isEmpty()) return;
        for (int i = 0; i < offers.size(); i++) {
            net.minecraft.world.item.trading.MerchantOffer offer = offers.get(i);
            if (offer != null && offer.getUses() > 0 && offer.getResult().getItem() instanceof com.velorise.staves.item.ElementItem) {
                String grantTag = "VeloriseTraderSoldSlot:" + i;
                if (!trader.getTags().contains(grantTag)) {
                    trader.addTag(grantTag);
                    ServerPlayer buyer = null;
                    if (trader.getTradingPlayer() instanceof ServerPlayer sp) {
                        buyer = sp;
                    } else {
                        UUID lastId = TRADER_LAST_CUSTOMER.get(trader.getUUID());
                        if (lastId != null && level.getServer() != null) {
                            buyer = level.getServer().getPlayerList().getPlayer(lastId);
                        }
                    }
                    if (buyer != null) {
                        com.velorise.staves.advancement.ModAdvancements.trigger(
                                buyer, com.velorise.staves.advancement.ModAdvancements.EVENT_WHERE_IS_HE_FROM);
                    }
                }
            }
        }
    }

    public static void checkInnateMutation(LivingEntity entity) {
        if (entity == null || entity instanceof Player || entity.level().isClientSide()) return;
        if (entity instanceof net.minecraft.world.entity.npc.WanderingTrader) {
            tryPopulateWanderingTraderOffers(entity);
        }
        if (entity instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }

        CompoundTag data = entity.getPersistentData();
        boolean alreadyChecked = data.contains("VeloriseInnateChecked") || entity.getTags().contains("VeloriseInnateChecked");
        if (!alreadyChecked) {
            data.putBoolean("VeloriseInnateChecked", true);
            entity.addTag("VeloriseInnateChecked");
            rollInnateMutationOnce(entity, data);
        }

        if (entity.level() instanceof ServerLevel slInit) {
            updateCoopMobScaling(slInit, entity);
        }
    }

    public static void rollInnateMutationOnce(LivingEntity entity, CompoundTag data) {
        // If the entity already has a natural innate element (other than non-mutated Charged Creeper lightning), do not mutate it
        if (!(entity instanceof Creeper) && getNaturalInnateElement(entity) != null) return;

        StavesConfig.MutationRules rules = StavesConfig.getMutationRules();
        double mutateChance = rules.resolveMutationChance(entity);
        if (mutateChance <= 0.0D) return;

        double roll = entity.getRandom().nextDouble();
        if (roll < mutateChance) {
            double omniThreshold = mutateChance * rules.omniShare;
            if (omniThreshold > 0.0D && roll < omniThreshold) {
                data.putString("VeloriseInnate", "OMNI");
                entity.addTag(TAG_INNATE_OMNI);
                entity.addTag("VeloriseInnate:OMNI");
                OMNI_INNATE_ENTITIES.add(entity.getUUID());
                extinguishPyroInnate(entity);
                com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, -2);
                return;
            }
            Element[] elements = Element.values();
            Element chosen = elements[entity.getRandom().nextInt(elements.length)];
            data.putString("VeloriseInnate", chosen.name());
            entity.addTag("VeloriseInnate:" + chosen.name());
            if (chosen == Element.PYRO) {
                extinguishPyroInnate(entity);
            }

            // Immediately initialize mutated innate element(s) in GAUGES (including Anemo & Geo + Charged Creeper dual)
            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
            EnumSet<Element> innateSet = getInnateElements(entity);
            for (Element inn : innateSet) {
                gauges.put(inn, new GaugeState(inn, 1.25F));
            }
            syncAuras(entity, innateSet, null, null, null, 0.0F);
        }
    }

    public static boolean isOmniInnate(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living) || living instanceof Player) return false;
        if (!living.level().isClientSide() && !living.getPersistentData().contains("VeloriseInnateChecked") && !living.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(living);
        }
        if (OMNI_INNATE_ENTITIES.contains(living.getUUID())) return true;
        if (living.getTags().contains(TAG_INNATE_OMNI) || living.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(living.getUUID());
            return true;
        }
        if (living.getPersistentData().contains("VeloriseInnate")
                && "OMNI".equalsIgnoreCase(living.getPersistentData().getString("VeloriseInnate"))) {
            OMNI_INNATE_ENTITIES.add(living.getUUID());
            return true;
        }
        return false;
    }

    public static void setExplicitInnateElement(LivingEntity entity, @Nullable Element element) {
        if (entity == null || entity.level().isClientSide()) return;
        entity.addTag("VeloriseInnateChecked");
        OMNI_INNATE_ENTITIES.remove(entity.getUUID());
        entity.removeTag(TAG_INNATE_OMNI);
        entity.removeTag("VeloriseInnate:OMNI");
        entity.removeTag("VeloriseInnate:NONE");
        for (Element e : Element.values()) {
            entity.removeTag("VeloriseInnate:" + e.name());
        }
        if (element == null) {
            entity.addTag("VeloriseInnate:NONE");
            GAUGES.remove(entity.getUUID());
            syncAuras(entity, Collections.emptyList(), null, null, null, 0.0F);
            return;
        }
        entity.addTag("VeloriseInnate:" + element.name());
        if (element == Element.PYRO) {
            extinguishPyroInnate(entity);
        }
        Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
        gauges.put(element, new GaugeState(element, 1.25F));
        syncAuras(entity, gauges.keySet(), element, null, null, 0.0F);
    }

    public static void setOmniInnate(LivingEntity entity, boolean omni) {
        if (entity == null || entity.level().isClientSide()) return;
        entity.addTag("VeloriseInnateChecked");
        if (!omni) {
            OMNI_INNATE_ENTITIES.remove(entity.getUUID());
            entity.removeTag(TAG_INNATE_OMNI);
            entity.removeTag("VeloriseInnate:OMNI");
            syncAuras(entity, Collections.emptyList(), null, null, null, 0.0F);
            return;
        }
        for (Element e : Element.values()) {
            entity.removeTag("VeloriseInnate:" + e.name());
        }
        entity.removeTag("VeloriseInnate:NONE");
        entity.addTag(TAG_INNATE_OMNI);
        entity.addTag("VeloriseInnate:OMNI");
        OMNI_INNATE_ENTITIES.add(entity.getUUID());
        extinguishPyroInnate(entity);
        com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, -2);
    }

    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (target == null || target instanceof Player) return null;
        if (!target.level().isClientSide() && !target.getPersistentData().contains("VeloriseInnateChecked") && !target.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(target);
        }
        if (target.getTags().contains("VeloriseInnate:NONE")) {
            return null;
        }
        if (target.getTags().contains(TAG_INNATE_OMNI) || target.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(target.getUUID());
            return null;
        }
        if (target.getPersistentData().contains("VeloriseInnate")) {
            String elemName = target.getPersistentData().getString("VeloriseInnate");
            if ("OMNI".equalsIgnoreCase(elemName)) {
                OMNI_INNATE_ENTITIES.add(target.getUUID());
                return null;
            }
            try {
                return Element.valueOf(elemName);
            } catch (IllegalArgumentException ignored) {}
        }
        for (String tag : target.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) {
                String val = tag.substring("VeloriseInnate:".length());
                if ("OMNI".equalsIgnoreCase(val)) {
                    OMNI_INNATE_ENTITIES.add(target.getUUID());
                    return null;
                }
                try {
                    return Element.valueOf(val);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return getNaturalInnateElement(target);
    }

    public static EnumSet<Element> getInnateElements(@Nullable LivingEntity target) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        if (target == null) return set;
        if (isOmniInnate(target)) return set;
        Element primary = getInnateElement(target);
        if (primary != null) {
            set.add(primary);
        }
        if (target instanceof Creeper creeper && creeper.isPowered()) {
            set.add(Element.ELECTRO);
        }
        return set;
    }

    public static boolean hasInnateElement(@Nullable LivingEntity target, @Nullable Element element) {
        return target != null && element != null && getInnateElements(target).contains(element);
    }

    public static boolean isPyroInnate(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && hasInnateElement(living, Element.PYRO);
    }

    public static boolean isPyroResistant(@Nullable LivingEntity target) {
        return target != null && (target instanceof Warden || target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE));
    }

    public static void ensurePreDeathPyroIgnite(@Nullable LivingEntity target, int seconds) {
        if (target == null || !target.isAlive() || seconds <= 0) return;
        if (isPyroInnate(target) || isPyroResistant(target) || isEntityInWater(target)) return;
        if (hasElement(target, Element.ELECTRO) || hasInnateElement(target, Element.ELECTRO)) return;
        if (target.getRemainingFireTicks() < seconds * 20) {
            target.setSecondsOnFire(seconds);
        }
    }

    public static void extinguishPyroInnate(@Nullable LivingEntity entity) {
        if (entity == null || (!isPyroInnate(entity) && !isOmniInnate(entity))) return;
        if (entity.getRemainingFireTicks() > 0 || entity.isOnFire()) {
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
        }
        if (!entity.level().isClientSide()) {
            UUID id = entity.getUUID();
            BURNING_UNTIL.remove(id);
            BURNING_EM_LEVEL.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
        }
    }

    public static boolean isMutatedInnate(LivingEntity entity) {
        if (entity.getPersistentData().contains("VeloriseInnate")) return true;
        for (String tag : entity.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) return true;
        }
        return false;
    }

    public static void tryDropElementOnDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        WitherShulkerMechanicManager.tryTriggerWitherContagionOnDeath(entity, source);
        boolean mutatedOrOmni = isMutatedInnate(entity) || isOmniInnate(entity);
        if (!(entity instanceof Enemy) && !mutatedOrOmni) return;
        if (entity instanceof Slime slime && slime.getSize() < 3) return;
        boolean playerKill = (source.getEntity() instanceof Player) || (entity.getKillCredit() instanceof Player);
        if (!playerKill) return;

        double chance;
        net.minecraft.world.item.Item dropItem;
        if (isOmniInnate(entity)) {
            chance = 0.25D;
            dropItem = VeloriseStavesMod.getElementItem(null);
        } else if (isMutatedInnate(entity)) {
            Element mutated = getInnateElement(entity);
            if (mutated == null) return;
            chance = 0.15D;
            dropItem = VeloriseStavesMod.getElementItem(mutated);
        } else {
            Element natural = getNaturalInnateElement(entity);
            if (natural == null) return;
            chance = 0.05D;
            dropItem = VeloriseStavesMod.getElementItem(natural);
        }

        if (level.random.nextDouble() < chance) {
            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5D, entity.getZ(), new ItemStack(dropItem));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.6D, entity.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.04D);
        }
    }

    public static @Nullable Element getNaturalInnateElement(LivingEntity target) {
        EntityType<?> type = target.getType();
        if (type.is(NO_INNATE_CREATURES)) return null;
        if (type.is(PYRO_CREATURES)) return Element.PYRO;
        if (type.is(HYDRO_CREATURES)) return Element.HYDRO;
        if (type.is(CRYO_CREATURES)) return Element.CRYO;
        if (type.is(ELECTRO_CREATURES)) return Element.ELECTRO;
        if (type.is(DENDRO_CREATURES) && !(target instanceof MagmaCube)) return Element.DENDRO;
        if (type.is(ANEMO_CREATURES)) return Element.ANEMO;
        if (type.is(GEO_CREATURES)) return Element.GEO;

        // Fallback / dynamic state checks
        if (target instanceof Blaze
                || target instanceof MagmaCube
                || target instanceof Strider
                || target instanceof Ghast
                || target instanceof ZombifiedPiglin
                || target instanceof Zoglin
                || target instanceof WitherSkeleton) {
            return Element.PYRO;
        }
        if (target instanceof Stray || target instanceof SnowGolem || target instanceof PolarBear) {
            return Element.CRYO;
        }
        if (target instanceof Guardian || target instanceof ElderGuardian || target instanceof Drowned
                || target instanceof Axolotl) {
            return Element.HYDRO;
        }
        if (target.getType().getDescriptionId().contains("breeze")
                || target instanceof Phantom
                || target instanceof Vex) {
            return Element.ANEMO;
        }
        if ((target instanceof Creeper creeper && creeper.isPowered())
                || target instanceof EnderMan
                || target instanceof EnderDragon
                || target instanceof Endermite
                || target instanceof Shulker) {
            return Element.ELECTRO;
        }
        if (target instanceof MushroomCow
                || target instanceof Bee || target instanceof CaveSpider
                || (target instanceof Slime && !(target instanceof MagmaCube))
                || target.getType().getDescriptionId().contains("bogged")) {
            return Element.DENDRO;
        }
        if (target.getType().getDescriptionId().contains("armadillo")) {
            return Element.GEO;
        }
        return null;
    }

    public static boolean isBossEntity(LivingEntity entity) {
        return entity instanceof Warden
                || entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof ElderGuardian
                || entity instanceof com.velorise.staves.entity.CryoRegisvineEntity;
    }

    public static boolean isPassiveAquaticOrAmbient(LivingEntity entity) {
        return entity instanceof WaterAnimal || entity instanceof AmbientCreature;
    }

    public static void notifyOmniImmuneEnvironmental(ServerLevel level, LivingEntity target) {
        if (level == null || target == null || !target.isAlive()) return;
        extinguishPyroInnate(target);
        long now = level.getGameTime();
        Long last = LAST_OMNI_ENV_IMMUNE.get(target.getUUID());
        if (last == null || now < last || (now - last) >= 20L) {
            LAST_OMNI_ENV_IMMUNE.put(target.getUUID(), now);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            com.velorise.staves.network.ModNetwork.sendAuraSync(target, Collections.emptyList(), null, null, ElementalReaction.IMMUNE, 0.0F, -2);
        }
    }

    public static Element chooseOmniMutatedAttackElement(LivingEntity target, LivingEntity attacker) {
        EnumSet<Element> innates = getInnateElements(target);
        Collection<Element> active = getEntityElements(target.getUUID());
        boolean frozen = isEntityFrozen(target);
        boolean quickened = isEntityQuickened(target);
        boolean avoidPyroDamage = target.fireImmune() && !innates.contains(Element.CRYO);

        // 1. Cryo / Frozen target -> Forward Melt 2.0x with Pyro (or Superconduct if fire-immune)
        if (frozen || active.contains(Element.CRYO) || innates.contains(Element.CRYO)) {
            if (!innates.contains(Element.PYRO) && !target.fireImmune()) return Element.PYRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 2. Pyro aura / innate -> Forward Vaporize 2.0x with Hydro
        if (active.contains(Element.PYRO) || innates.contains(Element.PYRO)) {
            if (!innates.contains(Element.HYDRO)) return Element.HYDRO;
            if (!innates.contains(Element.CRYO)) return Element.CRYO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 3. Hydro aura / innate -> Reverse Vaporize 1.5x / Overvape with Pyro (or Dendro Bloom)
        if (active.contains(Element.HYDRO) || innates.contains(Element.HYDRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 4. Quicken or Dendro aura / innate -> Aggravate / Burning / Quicken
        if (quickened) {
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
        }
        if (active.contains(Element.DENDRO) || innates.contains(Element.DENDRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 5. Electro aura / innate -> Overloaded / Quicken / Superconduct
        if (active.contains(Element.ELECTRO) || innates.contains(Element.ELECTRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
            if (!innates.contains(Element.CRYO)) return Element.CRYO;
        }
        // 6. Anemo or Geo innate -> Swirl / Crystallize
        if (innates.contains(Element.ANEMO) || innates.contains(Element.GEO)) {
            return avoidPyroDamage ? Element.HYDRO : Element.PYRO;
        }
        // 7. No active aura on target -> pick a random element from all 7 elements
        Element[] all = Element.values();
        return all[attacker.getRandom().nextInt(all.length)];
    }
}
