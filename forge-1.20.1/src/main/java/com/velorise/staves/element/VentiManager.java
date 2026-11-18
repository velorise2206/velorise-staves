package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
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
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */

public final class VentiManager {
    private VentiManager() {}

    public record SourcewaterDroplet(ServerLevel level, Vec3 pos, long spawnTick, @Nullable UUID ownerId) {}

    public static final java.util.List<SourcewaterDroplet> SOURCEWATER_DROPLETS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final java.util.List<DaybreakVortexField> DAYBREAK_VORTICES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            VENTI_IDLE_STATES.remove(playerId);
            DAYBREAK_VORTICES.removeIf(v -> playerId.equals(v.ownerId));
            SOURCEWATER_DROPLETS.removeIf(d -> playerId.equals(d.ownerId));
        }
    }

    public static void clearAll() {
        SOURCEWATER_DROPLETS.clear();
        DAYBREAK_VORTICES.clear();
        VENTI_IDLE_STATES.clear();
    }

    public static final class DaybreakVortexField {
        final ServerLevel level;
        final Vec3 center;
        final UUID ownerId;
        final boolean isNetherite;
        final long expireTick;
        final double radius;
        @Nullable Element absorbedElement;

        DaybreakVortexField(ServerLevel level, Vec3 center, UUID ownerId, boolean isNetherite, long expireTick, double radius) {
            this.level = level;
            this.center = center;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
            this.expireTick = expireTick;
            this.radius = radius;
        }
    }

    public static void updatePlayerHpTracking(ServerPlayer player, long now) {
        FurinaManager.updatePlayerHpTracking(player, now);
    }

    public static void recordFurinaHpFluctuation(ServerLevel level, LivingEntity changedEntity, float hpFractionDelta) {
        FurinaManager.recordFurinaHpFluctuation(level, changedEntity, hpFractionDelta);
    }

    public static void activateFurinaUniversalRevelry(ServerPlayer player, boolean isNetherite) {
        FurinaManager.activateFurinaUniversalRevelry(player, isNetherite);
    }

    public static float getFanfareBonusMultiplier(@Nullable LivingEntity entity) {
        return FurinaManager.getFanfareBonusMultiplier(entity);
    }

    public static void drainHealthSilently(LivingEntity entity, float newHealth) {
        FurinaManager.drainHealthSilently(entity, newHealth);
    }

    public static boolean isChannelingGuardianLens(Player player) {
        if (player == null) return false;
        if (NeuvilletteManager.isChannelingOrFiring(player)) return true;
        if (!player.isUsingItem()) return false;
        ItemStack active = player.getUseItem();
        return ArcaneStaffItem.getEffectiveKind(active) == ArcaneStaffItem.Kind.GUARDIAN;
    }

    public static void applyFurinaWaterWalking(Player player) {
        FurinaManager.applyFurinaWaterWalking(player);
    }

    public static boolean isStandingOnWaterSurface(Player player) {
        return FurinaManager.isStandingOnWaterSurface(player);
    }

    public static boolean hasRecentHpChangeBuff(ServerPlayer player) {
        return FurinaManager.hasRecentHpChangeBuff(player);
    }

    public static boolean hasVentiWeapon(@Nullable Player player) {
        if (player == null) return false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK) {
                return true;
            }
        }
        return false;
    }

    public static boolean carriesGuardianLens(Player player) {
        if (player == null) return false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN) {
                return true;
            }
        }
        return false;
    }

    public static void onHydroReactionTriggered(
            ServerLevel level,
            Vec3 targetPos,
            ElementalReaction reaction,
            @Nullable Element reactedAura,
            @Nullable Element triggerElem,
            @Nullable ServerPlayer attacker
    ) {
        if (level == null || reaction == null) return;
        boolean isHydroReaction = switch (reaction) {
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, OVERVAPE, FROZEN, ELECTRO_CHARGED, BLOOM, REVERSE_BLOOM -> true;
            case SWIRL, CRYSTALLIZE -> reactedAura == Element.HYDRO || triggerElem == Element.HYDRO;
            default -> false;
        };
        if (!isHydroReaction) return;

        long now = level.getGameTime();
        if (attacker != null) {
            String stackKey = switch (reaction) {
                case FORWARD_VAPORIZE, REVERSE_VAPORIZE, OVERVAPE -> "VAPORIZE";
                case FROZEN -> "FROZEN";
                case ELECTRO_CHARGED -> "ELECTRO_CHARGED";
                case BLOOM, REVERSE_BLOOM -> "BLOOM";
                case SWIRL -> "HYDRO_SWIRL";
                case CRYSTALLIZE -> "HYDRO_CRYSTALLIZE";
                default -> reaction.name();
            };
            Map<String, Long> stacks = DRACONIC_GLORIES_STACKS.computeIfAbsent(attacker.getUUID(), k -> new ConcurrentHashMap<>());
            stacks.entrySet().removeIf(e -> now >= e.getValue());
            stacks.put(stackKey, now + 600L); // 30s Past Draconic Glories stack
        }

        // Item 6: Hydro reactions ONLY grant Past Draconic Glories stacks; Sourcewater Droplets spawn exclusively from Skill E & Burst [R].
    }

    public static boolean hasNetheriteGuardianLens(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty()
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(s)) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty()
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(s)) {
                return true;
            }
        }
        return false;
    }

    public static void tryInterruptGuardianChanneling(LivingEntity victim, float damageDealt) {
        NeuvilletteManager.tryInterruptChanneling(victim, damageDealt);
        if (damageDealt <= 0.0F || !(victim instanceof ServerPlayer sp)) return;
        if (sp.getAbsorptionAmount() > 0.0F) return;
        if (!sp.isUsingItem()) return;
        ItemStack active = sp.getUseItem();
        if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(active) == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN
                && !com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(active)) {
            sp.stopUsingItem();
            sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.55F, 1.25F);
        }
    }

    public static int getDraconicGloriesStacks(@Nullable LivingEntity entity) {
        if (entity == null) return 0;
        int c1Base = hasNetheriteGuardianLens(entity) ? 1 : 0;
        Map<String, Long> stacks = DRACONIC_GLORIES_STACKS.get(entity.getUUID());
        if (stacks == null || stacks.isEmpty()) return c1Base;
        long now = entity.level().getGameTime();
        stacks.entrySet().removeIf(e -> now >= e.getValue());
        return Math.min(3, stacks.size() + c1Base);
    }

    public static float getDraconicGloriesMultiplier(@Nullable LivingEntity entity) {
        int stacks = getDraconicGloriesStacks(entity);
        return switch (stacks) {
            case 1 -> 1.15F;
            case 2 -> 1.30F;
            case 3 -> 1.60F;
            default -> 1.00F;
        };
    }

    public static void spawnSourcewaterDroplet(ServerLevel level, Vec3 basePos, @Nullable UUID ownerId, long now) {
        SOURCEWATER_DROPLETS.removeIf(d -> d.level() != level || (now - d.spawnTick()) >= 300L);
        long activeInLevel = SOURCEWATER_DROPLETS.stream()
                .filter(d -> d.level() == level && java.util.Objects.equals(d.ownerId(), ownerId))
                .count();
        if (activeInLevel >= 9) {
            for (SourcewaterDroplet d : SOURCEWATER_DROPLETS) {
                if (d.level() == level && java.util.Objects.equals(d.ownerId(), ownerId)) {
                    SOURCEWATER_DROPLETS.remove(d);
                    break;
                }
            }
        }
        double ox = (level.random.nextDouble() - 0.5D) * 1.6D;
        double oz = (level.random.nextDouble() - 0.5D) * 1.6D;
        Vec3 dropPos = new Vec3(basePos.x + ox, basePos.y + 0.45D, basePos.z + oz);
        SOURCEWATER_DROPLETS.add(new SourcewaterDroplet(level, dropPos, now, ownerId));
        level.sendParticles(ParticleTypes.SPLASH, dropPos.x, dropPos.y, dropPos.z, 10, 0.2D, 0.2D, 0.2D, 0.04D);
        level.sendParticles(ParticleTypes.GLOW, dropPos.x, dropPos.y, dropPos.z, 6, 0.15D, 0.15D, 0.15D, 0.02D);
    }

    public static int consumeSourcewaterDropletsForChannel(ServerLevel level, ServerPlayer caster, double radius, int maxCount) {
        if (level == null || caster == null || SOURCEWATER_DROPLETS.isEmpty()) return 0;
        double r2 = radius * radius;
        Vec3 cPos = caster.position();
        UUID casterId = caster.getUUID();
        int absorbed = 0;
        for (SourcewaterDroplet droplet : SOURCEWATER_DROPLETS) {
            if (droplet.level() != level) continue;
            if (droplet.ownerId() == null || !droplet.ownerId().equals(casterId)) continue;
            if (droplet.pos().distanceToSqr(cPos) <= r2) {
                SOURCEWATER_DROPLETS.remove(droplet);
                absorbed++;
                level.sendParticles(ParticleTypes.BUBBLE_POP, droplet.pos().x, droplet.pos().y, droplet.pos().z, 12, 0.2D, 0.2D, 0.2D, 0.04D);
                level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.2D, caster.getZ(), 1, 0.2D, 0.2D, 0.2D, 0.02D);
                if (absorbed >= maxCount) break;
            }
        }
        if (absorbed > 0) {
            caster.heal(caster.getMaxHealth() * 0.16F * absorbed);
            level.playSound(null, caster.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.85F, 1.35F);
        }
        return absorbed;
    }

    public static void tickSourcewaterDroplets(MinecraftServer server, long serverNow) {
        if (SOURCEWATER_DROPLETS.isEmpty()) return;
        for (SourcewaterDroplet droplet : SOURCEWATER_DROPLETS) {
            ServerLevel lvl = droplet.level();
            if (lvl == null || serverNow < droplet.spawnTick() || (serverNow - droplet.spawnTick()) >= 300L) {
                SOURCEWATER_DROPLETS.remove(droplet);
                continue;
            }
            if (serverNow % 5 == 0) {
                lvl.sendParticles(ParticleTypes.DRIPPING_WATER, droplet.pos().x, droplet.pos().y, droplet.pos().z, 3, 0.12D, 0.12D, 0.12D, 0.01D);
                lvl.sendParticles(ParticleTypes.GLOW, droplet.pos().x, droplet.pos().y, droplet.pos().z, 2, 0.10D, 0.10D, 0.10D, 0.01D);
            }
            UUID ownerId = droplet.ownerId();
            if (ownerId == null) continue;
            for (ServerPlayer player : lvl.getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.getUUID().equals(ownerId) && p.position().distanceToSqr(droplet.pos()) <= 2.25D)) {
                if (player.getHealth() < player.getMaxHealth()) {
                    SOURCEWATER_DROPLETS.remove(droplet);
                    player.heal(player.getMaxHealth() * 0.16F);
                    lvl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.1D, player.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
                    lvl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.65F, 1.4F);
                    break;
                }
            }
        }
    }

    public static @Nullable Element getElementForEffect(MobEffect effect) {
        if (effect == null) return null;
        if (effect == MobEffects.MOVEMENT_SPEED) return Element.ANEMO;
        if (effect == MobEffects.MOVEMENT_SLOWDOWN) return Element.CRYO;
        if (effect == MobEffects.DAMAGE_RESISTANCE) return Element.GEO;
        if (effect == MobEffects.POISON) return Element.DENDRO;
        String desc = effect.getDescriptionId();
        if (desc != null) {
            if (desc.contains("wind_charged")) return Element.ANEMO;
            if (desc.contains("oozing")) return Element.DENDRO;
            if (desc.contains("infested")) return Element.GEO;
        }
        return null;
    }

    public static List<Element> getElementsFromPotionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        Potion potion = PotionUtils.getPotion(stack);
        if (potion == Potions.WATER) {
            result.add(Element.HYDRO);
        }
        for (MobEffectInstance inst : PotionUtils.getMobEffects(stack)) {
            Element mapped = getElementForEffect(inst.getEffect());
            if (mapped != null && !result.contains(mapped)) {
                result.add(mapped);
            } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                result.add(Element.ELECTRO);
            }
        }
        return result;
    }

    public static List<Element> getElementsFromArrow(ServerLevel level, Arrow arrow) {
        if (arrow == null) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        try {
            CompoundTag nbt = new CompoundTag();
            arrow.saveWithoutId(nbt);
            for (MobEffectInstance inst : PotionUtils.getAllEffects(nbt)) {
                Element mapped = getElementForEffect(inst.getEffect());
                if (mapped != null && !result.contains(mapped)) {
                    result.add(mapped);
                } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                    result.add(Element.ELECTRO);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    public static List<Element> getElementsFromAreaEffectCloud(ServerLevel level, AreaEffectCloud cloud) {
        if (cloud == null) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        try {
            if (cloud.getPotion() == Potions.WATER) {
                result.add(Element.HYDRO);
            }
            CompoundTag nbt = new CompoundTag();
            cloud.saveWithoutId(nbt);
            for (MobEffectInstance inst : PotionUtils.getAllEffects(nbt)) {
                Element mapped = getElementForEffect(inst.getEffect());
                if (mapped != null && !result.contains(mapped)) {
                    result.add(mapped);
                } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                    result.add(Element.ELECTRO);
                }
            }
        } catch (Throwable ignored) {}
        return result;
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
        if (!(stack.getItem() instanceof PotionItem)) return;
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
        if (mainHand.isEmpty()) return 0;
        String desc = mainHand.getItem().getDescriptionId();
        if (desc == null || !desc.contains("mace")) return 0;
        var enchTags = mainHand.getEnchantmentTags();
        for (int i = 0; i < enchTags.size(); i++) {
            CompoundTag tag = enchTags.getCompound(i);
            if (tag.getString("id").contains("wind_burst")) {
                return Math.max(1, tag.getInt("lvl"));
            }
        }
        return 0;
    }

    public static int getElementalMasteryLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var enchTags = stack.getEnchantmentTags();
        for (int i = 0; i < enchTags.size(); i++) {
            CompoundTag tag = enchTags.getCompound(i);
            if (tag.getString("id").contains("elemental_mastery")) {
                return Math.min(3, Math.max(0, tag.getInt("lvl")));
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
        var enchTags = stack.getEnchantmentTags();
        for (int i = 0; i < enchTags.size(); i++) {
            CompoundTag tag = enchTags.getCompound(i);
            if (tag.getString("id").contains("elemental_protection")) {
                return Math.min(4, Math.max(0, tag.getInt("lvl")));
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
                            piece.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(slot));
                        }
                    }
                }
            }
        }
        return amount;
    }

    public static void summonDaybreakVortex(ServerPlayer owner, Vec3 center, boolean isNetherite) {
        ServerLevel level = (ServerLevel) owner.level();
        long now = level.getServer().overworld().getGameTime();
        long duration = 160L; // 8.0s Genshin Wind's Grand Ode (same for Basic & Netherite)
        double radius = isNetherite ? 6.0D : 4.0D; // 4m Basic, 6m Netherite
        DAYBREAK_VORTICES.removeIf(v -> v.ownerId.equals(owner.getUUID()));
        DAYBREAK_VORTICES.add(new DaybreakVortexField(level, center, owner.getUUID(), isNetherite, now + duration, radius));
        com.velorise.staves.network.ModNetwork.sendVortexSphere(level, center.add(0.0D, 1.0D, 0.0D), radius, (int) duration, 0xFF66FFD8);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.9D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, VeloriseStavesMod.WIND_CHARGE_BURST_SOUND.get(), SoundSource.PLAYERS, 1.2F, 0.85F);
    }

    public static void tickDaybreakVortices(MinecraftServer server, long serverNow) {
        if (DAYBREAK_VORTICES.isEmpty()) return;
        DAYBREAK_VORTICES.removeIf(v -> serverNow >= v.expireTick || v.level == null);
        for (DaybreakVortexField vortex : DAYBREAK_VORTICES) {
            ServerLevel level = vortex.level;
            Vec3 center = vortex.center;
            double radius = vortex.radius;
            double radiusSqr = radius * radius;
            ServerPlayer owner = server.getPlayerList().getPlayer(vortex.ownerId);

            if (serverNow % 2L == 0L) {
                AABB pullBox = new AABB(center, center).inflate(radius);
                Vec3 eyeTarget = center.add(0.0D, 0.85D, 0.0D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e.isAlive() && !e.isRemoved())) {
                    if (target.getUUID().equals(vortex.ownerId)) continue;
                    if (target instanceof net.minecraft.world.entity.TamableAnimal tame && vortex.ownerId.equals(tame.getOwnerUUID())) continue;
                    if (target instanceof ServerPlayer && !server.isPvpAllowed()) continue;
                    if (isEntityFrozen(target)) continue;
                    double distSqr = target.position().distanceToSqr(center);
                    if (distSqr > radiusSqr) continue;

                    Vec3 diff = eyeTarget.subtract(target.getBoundingBox().getCenter());
                    double dist = diff.length();
                    if (dist > 0.25D) {
                        Vec3 dir = diff.scale(1.0D / dist);
                        double bossScale = target.getMaxHealth() >= 100.0F ? 0.35D : 1.0D;
                        double pullStrength = Math.min(0.42D, 0.16D + (dist / radius) * 0.26D) * bossScale;
                        Vec3 cur = target.getDeltaMovement();
                        double nextY = (target.getMaxHealth() >= 100.0F)
                                ? cur.y
                                : Math.max(-0.08D, Math.min(0.32D, cur.y * 0.65D + dir.y * 0.22D + 0.06D));
                        target.setDeltaMovement(
                                cur.x * 0.72D + dir.x * pullStrength,
                                nextY,
                                cur.z * 0.72D + dir.z * pullStrength
                        );
                        target.fallDistance = 0.0F;
                        target.hurtMarked = true;
                    }
                }
                for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pullBox, e -> e.isAlive() && !e.isRemoved())) {
                    if (item.position().distanceToSqr(center) > radiusSqr) continue;
                    Vec3 diff = eyeTarget.subtract(item.position());
                    double dist = diff.length();
                    if (dist > 0.35D) {
                        Vec3 dir = diff.scale(1.0D / dist);
                        double pull = Math.min(0.32D, 0.12D + (dist / radius) * 0.20D);
                        Vec3 cur = item.getDeltaMovement();
                        item.setDeltaMovement(cur.x * 0.72D + dir.x * pull, Math.max(-0.05D, Math.min(0.24D, cur.y * 0.65D + dir.y * 0.18D + 0.04D)), cur.z * 0.72D + dir.z * pull);
                    }
                }
            }

            if (serverNow % 10L == 0L) {
                AABB tickBox = new AABB(center, center).inflate(radius);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, tickBox, e -> {
                    if (!e.isAlive() || e.isRemoved() || e.getUUID().equals(vortex.ownerId)) return false;
                    if (e instanceof net.minecraft.world.entity.TamableAnimal tame && vortex.ownerId.equals(tame.getOwnerUUID())) return false;
                    if (e instanceof ServerPlayer && !server.isPvpAllowed()) return false;
                    return e.position().distanceToSqr(center) <= radiusSqr;
                });

                if (vortex.absorbedElement == null) {
                    Element absorbed = detectDaybreakAbsorption(level, center, targets);
                    if (absorbed != null) {
                        vortex.absorbedElement = absorbed;
                        int absorbedColor = switch (absorbed) {
                            case PYRO -> 0xFFFF5522;
                            case HYDRO -> 0xFF18BFFF;
                            case ELECTRO -> 0xFFC44DFF;
                            case CRYO -> 0xFF7CF4FF;
                            default -> 0xFF66FFD8;
                        };
                        int remainingTicks = (int) Math.max(5L, vortex.expireTick - serverNow);
                        com.velorise.staves.network.ModNetwork.sendVortexSphere(
                                level, center.add(0.0D, 1.0D, 0.0D), radius, remainingTicks, absorbedColor);
                        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
                    }
                }

                float baseDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK, vortex.isNetherite);
                float anemoTickDmg = baseDmg * 0.6F;
                float absorbedTickDmg = baseDmg * 0.45F;
                for (LivingEntity target : targets) {
                    applyElement(level, target, Element.ANEMO, 1.0F, owner, anemoTickDmg);
                    if (vortex.absorbedElement != null) {
                        applyElement(level, target, vortex.absorbedElement, 1.0F, owner, absorbedTickDmg);
                    }
                }
                level.playSound(null, center.x, center.y, center.z, VeloriseStavesMod.WIND_CHARGE_BURST_SOUND.get(), SoundSource.PLAYERS, 0.35F, 1.4F);
            }
        }
    }

    public static @Nullable Element detectDaybreakAbsorption(ServerLevel level, Vec3 center, List<LivingEntity> targets) {
        Element[] priority = {Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO};
        for (Element candidate : priority) {
            for (LivingEntity target : targets) {
                Map<Element, GaugeState> gauges = GAUGES.get(target.getUUID());
                if (gauges != null && gauges.containsKey(candidate)) {
                    return candidate;
                }
                if (candidate == Element.PYRO && target.isOnFire()) {
                    return Element.PYRO;
                }
                if (candidate == Element.HYDRO && target.isInWaterOrRain()) {
                    return Element.HYDRO;
                }
            }
        }
        // Environmental fire absorption near vortex center (Fire, Soul Fire, Lava, lit Campfire, Torches)
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.LAVA) || st.is(Blocks.MAGMA_BLOCK)
                    || st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)
                    || st.is(Blocks.SOUL_TORCH) || st.is(Blocks.SOUL_WALL_TORCH)
                    || (st.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                        && (!st.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
                            || st.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)))) {
                return Element.PYRO;
            }
        }
        return null;
    }

    public static boolean isInsideAnemoUpdraft(ServerLevel level, Vec3 pos) {
        if (level == null || pos == null || DAYBREAK_VORTICES.isEmpty()) return false;
        for (DaybreakVortexField v : DAYBREAK_VORTICES) {
            if (v.level != level) continue;
            double dx = pos.x - v.center.x;
            double dz = pos.z - v.center.z;
            double r = v.radius + 1.5D;
            if ((dx * dx + dz * dz) <= r * r && pos.y >= v.center.y - 3.0D && pos.y <= v.center.y + 14.0D) {
                return true;
            }
        }
        return false;
    }

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
                if (!hasInnateElement(target, Element.PYRO) && !target.fireImmune() && !(target instanceof Warden)) {
                    long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                            target, 200L);
                    BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + burnTicks);
                    BURNING_EM_LEVEL.put(target.getUUID(), emLevel);
                    LivingEntity burnCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
                    if (burnCredit != null && burnCredit != target) {
                        BURNING_ATTACKER_UUID.put(target.getUUID(), burnCredit.getUUID());
                    }
                    EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, pos, 1.5D);
                    target.setSecondsOnFire(5);
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

    /**
     * Server tick loop for decay, environmental water/rain extinguish, and periodic reactions.
     */

    private static final class ArchonStillState {
        net.minecraft.world.phys.Vec3 lastStillPos = net.minecraft.world.phys.Vec3.ZERO;
        int stillTicks = 0;
        int activeCode = 0;
        int lastSyncedCode = -1;
        int idleVariant = 0;
        int songIndex = 0;
    }

    private static final java.util.Map<java.util.UUID, ArchonStillState> VENTI_IDLE_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        ArchonStillState vst = VENTI_IDLE_STATES.get(player.getUUID());
        if (vst != null && (vst.activeCode == 4051 || vst.stillTicks > 0)) {
            vst.stillTicks = 0;
            vst.lastStillPos = player.position();
            vst.activeCode = 4050;
            vst.lastSyncedCode = 4050;
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4050);
        }
    }

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            // Venti (DAYBREAK)
            ArchonStillState vst = VENTI_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new ArchonStillState());
            boolean holdingVenti = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.DAYBREAK;
            if (vst.lastStillPos == null || vst.lastStillPos.equals(net.minecraft.world.phys.Vec3.ZERO)) {
                vst.lastStillPos = player.position();
            }
            double vDistSq = player.position().distanceToSqr(vst.lastStillPos);
            boolean canVenti = holdingVenti && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canVenti && vDistSq < 0.0025D) {
                vst.stillTicks++;
                if (vst.stillTicks == 60) {
                    vst.idleVariant = (vst.idleVariant + 1) % 2;
                    if (vst.idleVariant == 0) {
                        vst.songIndex = (vst.songIndex + 1) % 2;
                    }
                }
                if (vst.stillTicks >= 60) {
                    if (vst.idleVariant == 1) {
                        // Levitation idle hovers for ~5s (ticks 60..160) then gently descends
                        if (vst.stillTicks <= 160) {
                            vst.activeCode = 4052;
                        } else {
                            vst.activeCode = 4050;
                            vst.stillTicks = 0;
                        }
                    } else {
                        vst.activeCode = 4051;
                    }
                } else {
                    vst.activeCode = 4050;
                }
            } else {
                vst.stillTicks = 0;
                vst.lastStillPos = player.position();
                vst.activeCode = 4050;
            }
            if (vst.activeCode != vst.lastSyncedCode || (vst.activeCode == 4051 && now % 20L == 0L)) {
                vst.lastSyncedCode = vst.activeCode;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, vst.activeCode);
            }
            if (vst.activeCode == 4051 && vst.stillTicks >= 60) {
                // {note, durationTicks} pairs for expressive rhythm (2t fast runs, 3-4t normal, 6-8t held notes)
                int[][][] melodies = {
                    // 1. Venti's Theme (Bard's Adventure / A New Day with Hope)
                    {
                        {17,3},{20,3},{22,3},{24,7}, {22,2},{20,3},{17,3},{15,6},
                        {17,3},{20,3},{22,3},{20,3},{17,2},{15,3},{12,7},
                        {15,3},{17,3},{20,3},{22,3},{24,7}, {22,2},{20,3},{17,6},
                        {17,3},{20,3},{22,3},{24,5}, {22,2},{24,3},{27,6},{24,6},
                        {22,2},{20,3},{17,3},{15,3},{17,3},{20,6},
                        // Chorus
                        {24,4},{24,2},{22,3},{20,3},{22,3},{24,7}, {27,4},{24,3},{22,3},{20,3},{17,6},
                        {20,2},{22,2},{24,3},{27,6},{24,3},{22,3},{20,3},{17,3},{15,6},
                        {17,3},{20,3},{22,3},{20,3},{17,3},{15,3},{12,8}, {17,8}
                    },
                    // 2. Furina Execution (Sinner's Finale / La Vaguelette)
                    {
                        {13,4},{18,4},{20,4},{21,8}, {20,3},{18,3},{17,3},{18,7},
                        {13,4},{14,3},{13,3},{11,3},{9,3},{8,3},{6,8},
                        {13,4},{18,4},{20,4},{21,7}, {20,3},{21,3},{23,6},{21,7},
                        {20,3},{18,3},{17,3},{18,3},{20,7},
                        // Chorus
                        {25,5},{25,3},{23,3},{21,3},{20,3},{21,8}, {23,4},{21,3},{20,3},{18,3},{17,7},
                        {18,3},{20,3},{21,4},{25,7},{23,3},{21,3},{20,3},{18,3},{17,7},
                        {13,4},{18,4},{20,4},{21,6},{20,3},{18,3},{17,4},{18,10}
                    }
                };
                int[][] song = melodies[Math.floorMod(vst.songIndex, melodies.length)];
                int elapsed = vst.stillTicks - 60;
                int acc = 0;
                boolean played = false;
                for (int[] pair : song) {
                    if (elapsed == acc) {
                        int note = pair[0];
                        if (note >= 0 && player.level() instanceof ServerLevel sl) {
                            float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
                            sl.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                                    SoundEvents.NOTE_BLOCK_HARP.get(), SoundSource.PLAYERS, 0.55F, pitch);
                            if (pair[1] >= 6) {
                                sl.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                                        SoundEvents.NOTE_BLOCK_FLUTE.get(), SoundSource.PLAYERS, 0.25F, pitch);
                            }
                        }
                        played = true;
                        break;
                    }
                    acc += pair[1];
                }
                if (!played && elapsed >= acc) {
                    vst.activeCode = 4050;
                    vst.stillTicks = 0;
                }
            }

        }
    }
}
