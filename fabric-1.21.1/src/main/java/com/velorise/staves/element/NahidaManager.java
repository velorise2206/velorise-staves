package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Dendro Archon Nahida (A Thousand Floating Dreams):
 * - NA (4-step Hopscotch combo): Cute hopscotch bounce with growing glowing Dendro keyboard tiles & sprouting grass (Steps 1-3)
 *   + Step 4 vine-heart & 4-petal flower AoE Dendro explosion.
 * - CA (Hold Left-Click): Hand-woven glowing digital green Dendro square frame in front of player -> AoE Dendro burst.
 * - Idle Animation (Dendro Vine Swing): Standing still for ~5.5s summons a glowing Dendro vine swing for the player to sit and sway on.
 * - Skill E (All Schemes to Know):
 *   - Tap Right-Click: Sweeps nearby enemies and links up to 8 targets with Seeds of Skandha (25s).
 *   - Hold Right-Click: Camera Viewfinder mode (HUD viewfinder frame with 4 corner stars & 4-petal flower reticle)
 *     that marks aimed enemies AND remotely harvests flowers, berries, mature crops, and vacuums ItemEntities into inventory!
 *   - Tri-Karma Purification: Triggering an Elemental Reaction (or Dendro Core hit) on a Seed of Skandha target
 *     unleashes linked Dendro damage across all connected targets (2.5s base ICD, reduced by Electro Shrine buff).
 * - Burst [R] (Illusory Heart — Shrine of Maya):
 *   - 1.7s (34 ticks) I-frame + prayer levitation animation expanding the emerald Shrine of Maya dome.
 *   - Grants +2 EM levels to self & allies inside the Shrine, plus 3 elemental buffs (Pyro: +Tri-Karma DMG;
 *     Electro: shorter Tri-Karma interval; Hydro: extended Shrine duration; Netherite C1 adds +1 to all 3 counts).
 */
public final class NahidaManager {
    private static final DustParticleOptions DENDRO_GLOW_DUST =
            new DustParticleOptions(new Vector3f(0.32F, 0.98F, 0.28F), 1.35F);
    private static final DustParticleOptions DENDRO_HEART_DUST =
            new DustParticleOptions(new Vector3f(0.52F, 1.00F, 0.42F), 1.55F);

    public static final class NahidaPlayerState {
        public int naComboStep = 0;
        public long lastNaTick = -100L;
        public long lastActionTick = 0L;
        public Vec3 lastStillPos = Vec3.ZERO;
        public int stillTicks = 0;
        public boolean idleSwingActive = false;
        public boolean holdingViewfinder = false;
        public boolean viewfinderNetherite = false;
        public final Set<UUID> viewfinderMarkedTargets = new HashSet<>();
        public long lastHarvestSweepTick = -100L;
        public long lastTriKarmaProcTick = -100L;
        public long shrineUntilTick = 0L;
        public Vec3 shrineCenter = Vec3.ZERO;
        public double shrineRadius = 12.0D;
        public boolean shrineNetherite = false;
        public int shrinePyroCount = 0;
        public int shrineElectroCount = 0;
        public int shrineHydroCount = 0;
        public long ultiCooldownUntilTick = 0L;
        public long iframeUntilTick = 0L;
        public int lastSyncedPlayerCode = -1;
        public int lastSyncedShrineCode = -1;
    }

    public static final class SkandhaMark {
        public final UUID ownerId;
        public final boolean netherite;
        public long expireTick;

        public SkandhaMark(UUID ownerId, boolean netherite, long expireTick) {
            this.ownerId = ownerId;
            this.netherite = netherite;
            this.expireTick = expireTick;
        }
    }

    private static final Map<UUID, NahidaPlayerState> PLAYER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, SkandhaMark> SKANDHA_MARKS = new ConcurrentHashMap<>();

    private NahidaManager() {}

    public static void clearAll() {
        PLAYER_STATES.clear();
        SKANDHA_MARKS.clear();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId == null) return;
        PLAYER_STATES.remove(playerId);
        SKANDHA_MARKS.entrySet().removeIf(e -> playerId.equals(e.getValue().ownerId));
    }

    public static boolean isHoldingNahida(@Nullable Player player) {
        return !getHeldNahidaStack(player).isEmpty();
    }

    public static ItemStack getHeldNahidaStack(@Nullable Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
            return main;
        }
        if (ArcaneStaffItem.isGenshinCharacterWeapon(main)) {
            return ItemStack.EMPTY;
        }
        ItemStack off = player.getOffhandItem();
        if (ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isNahidaIframeActive(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        NahidaPlayerState st = PLAYER_STATES.get(player.getUUID());
        return st != null && player.level().getGameTime() < st.iframeUntilTick;
    }

    public static void markActiveAction(ServerPlayer player, long now) {
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        st.lastActionTick = now;
        st.stillTicks = 0;
        if (st.idleSwingActive) {
            st.idleSwingActive = false;
            syncPlayerStateToClients(player, st, now, true);
        }
    }

    /**
     * Normal Attack (Tap Left-Click): 4-step hopscotch combo with growing Dendro keyboard tiles & grass sprouts,
     * culminating in Step 4's vine-heart + 4-petal flower AoE burst.
     */
    private static void fertilizeSaplingsInBox(ServerLevel level, ServerPlayer player, AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.SAPLINGS)) {
                net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, pos);
            }
        }
    }

    private static void forceGrowPlantsInBox(ServerLevel level, ServerPlayer player, AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bonemealable) {
                for (int attempts = 0; attempts < 15; attempts++) {
                    if (!bonemealable.isValidBonemealTarget(level, pos, state)) break;
                    bonemealable.performBonemeal(level, level.random, pos, state);
                    state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock b) || !b.isValidBonemealTarget(level, pos, state)) {
                        break;
                    }
                }
                level.levelEvent(1505, pos, 0);
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
                if (!crop.isMaxAge(state)) {
                    level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
                    level.levelEvent(1505, pos, 0);
                }
            }
        }
    }

    public static void handleNormalAttack(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack stack = getHeldNahidaStack(player);
        if (stack.isEmpty() || !(player.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        if (now - st.lastNaTick < 10L) return;
        if (now - st.lastNaTick > 36L) {
            st.naComboStep = 0;
        }
        st.naComboStep = (st.naComboStep % 4) + 1;
        st.lastNaTick = now;
        markActiveAction(player, now);

        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        float baseScale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS, isNeth);
        int step = st.naComboStep;

        // Cute hopscotch bounce when on ground
        if (player.onGround()) {
            Vec3 dv = player.getDeltaMovement();
            double hopY = (step == 4) ? 0.30D : 0.22D;
            player.setDeltaMovement(dv.x * 1.05D, Math.max(dv.y, hopY), dv.z * 1.05D);
            player.hasImpulse = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        int animCode = switch (step) {
            case 1 -> com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_NA_1;
            case 2 -> com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_NA_2;
            case 3 -> com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_NA_3;
            default -> com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_NA_4;
        };
        ModNetwork.sendStaffAnimation(player, animCode);

        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() < 1.0E-4D) {
            horiz = Vec3.directionFromRotation(0.0F, player.getYRot());
        } else {
            horiz = horiz.normalize();
        }
        Vec3 right = new Vec3(-horiz.z, 0.0D, horiz.x);

        if (step <= 3) {
            double reach = 3.8D + step * 1.6D;
            double halfWidth = 1.1D + step * 0.35D;
            float dmg = 1.0F; // Fixed 0.5 heart damage
            float gaugeU = isNeth ? 1.5F : 1.0F;

            for (int i = 1; i <= step + 1; i++) {
                double dist = i * 1.65D;
                Vec3 tileCenter = player.position().add(horiz.scale(dist));
                int pCount = 6 + step * 4;
                level.sendParticles(DENDRO_GLOW_DUST, tileCenter.x, tileCenter.y + 0.15D, tileCenter.z,
                        pCount, 0.35D + step * 0.12D, 0.12D, 0.35D + step * 0.12D, 0.02D);
                level.sendParticles(ParticleTypes.COMPOSTER, tileCenter.x, tileCenter.y + 0.25D, tileCenter.z,
                        4 + step * 2, 0.30D, 0.15D, 0.30D, 0.02D);
            }

            AABB hitBox = player.getBoundingBox().expandTowards(horiz.scale(reach)).inflate(halfWidth, 1.2D, halfWidth);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(player.position());
                double proj = toTarget.dot(horiz);
                if (proj < -0.5D || proj > reach + 1.0D) continue;
                target.invulnerableTime = 0;
                ElementalGaugeManager.applyElement(level, target, Element.DENDRO, gaugeU, player, dmg);
            }
            fertilizeSaplingsInBox(level, player, hitBox);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.75F, 1.25F + step * 0.12F);
            level.playSound(null, player.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 0.85F, 1.15F + step * 0.10F);
        } else {
            // Step 4 Finisher: Heart of Vines + 4-Petal Dendro Flower AoE Burst
            Vec3 burstCenter = player.position().add(horiz.scale(3.6D));
            double radius = isNeth ? 4.5D : 3.5D;
            float dmg = 1.0F; // Fixed 0.5 heart damage
            float gaugeU = isNeth ? 2.0F : 1.5F;

            // Heart curve particle outline + 4-petal flower burst
            for (int i = 0; i < 24; i++) {
                double t = (Math.PI * 2.0D * i) / 24.0D;
                double hx = 16.0D * Math.pow(Math.sin(t), 3) / 14.0D;
                double hy = (13.0D * Math.cos(t) - 5.0D * Math.cos(2.0D * t) - 2.0D * Math.cos(3.0D * t) - Math.cos(4.0D * t)) / 14.0D;
                Vec3 hp = burstCenter.add(right.scale(hx)).add(0.0D, 1.15D + hy * 0.65D, 0.0D);
                level.sendParticles(DENDRO_HEART_DUST, hp.x, hp.y, hp.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            }
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, burstCenter.x, burstCenter.y + 0.8D, burstCenter.z,
                    28, radius * 0.4D, 0.45D, radius * 0.4D, 0.05D);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, burstCenter.x, burstCenter.y + 0.6D, burstCenter.z,
                    18, radius * 0.35D, 0.35D, radius * 0.35D, 0.04D);

            AABB aoeBox = new AABB(burstCenter, burstCenter).inflate(radius, 2.2D, radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aoeBox,
                    e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e))) {
                target.invulnerableTime = 0;
                ElementalGaugeManager.applyElementBypassIcd(level, target, Element.DENDRO, gaugeU, player, dmg);
            }
            forceGrowPlantsInBox(level, player, aoeBox);
            level.playSound(null, BlockPos.containing(burstCenter), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.55F);
            level.playSound(null, BlockPos.containing(burstCenter), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.90F, 1.35F);
        }
    }

    /**
     * Charged Attack (Hold Left-Click): Draws a glowing digital green Dendro square frame in front of the player,
     * then bursts with AoE Dendro damage.
     */
    public static void handleChargedAttack(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack stack = getHeldNahidaStack(player);
        if (stack.isEmpty() || !(player.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        markActiveAction(player, now);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_CA);

        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        float baseScale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS, isNeth);
        float caDmg = 6.0F * baseScale;
        float gaugeU = isNeth ? 2.0F : 1.5F;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(8.5D)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation()
                : player.position().add(new Vec3(look.x, 0.0D, look.z).normalize().scale(4.5D));

        double halfSide = isNeth ? 2.4D : 1.8D;
        for (int i = -6; i <= 6; i++) {
            double o = (i / 6.0D) * halfSide;
            level.sendParticles(DENDRO_GLOW_DUST, center.x + o, center.y + 0.25D, center.z - halfSide, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            level.sendParticles(DENDRO_GLOW_DUST, center.x + o, center.y + 0.25D, center.z + halfSide, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            level.sendParticles(DENDRO_GLOW_DUST, center.x - halfSide, center.y + 0.25D, center.z + o, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            level.sendParticles(DENDRO_GLOW_DUST, center.x + halfSide, center.y + 0.25D, center.z + o, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        level.sendParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.6D, center.z, 24, halfSide * 0.5D, 0.35D, halfSide * 0.5D, 0.04D);

        AABB box = new AABB(center.x - halfSide - 0.8D, center.y - 1.5D, center.z - halfSide - 0.8D,
                center.x + halfSide + 0.8D, center.y + 2.8D, center.z + halfSide + 0.8D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e))) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElementBypassIcd(level, target, Element.DENDRO, gaugeU, player, caDmg);
        }

        InteractionHand hand = player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, hand);
        level.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.40F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.70F, 1.55F);
    }

    /** Starts holding Right-Click (All Schemes to Know — Viewfinder / Tap E). */
    public static void onStartHoldUse(ServerPlayer player, ItemStack stack, boolean isNetherite) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        markActiveAction(player, now);
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        st.holdingViewfinder = false;
        st.viewfinderNetherite = isNetherite;
        st.viewfinderMarkedTargets.clear();
    }

    /** Called every tick while holding Right-Click with Nahida's catalyst. */
    public static void onHoldUseTick(ServerLevel level, ServerPlayer player, ItemStack stack, int elapsedTicks, boolean isNetherite) {
        long now = level.getGameTime();
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        st.lastActionTick = now;
        st.stillTicks = 0;
        if (st.idleSwingActive) {
            st.idleSwingActive = false;
        }
        if (elapsedTicks >= 5 && !st.holdingViewfinder) {
            st.holdingViewfinder = true;
            syncPlayerStateToClients(player, st, now, true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.50F);
        }
        if (!st.holdingViewfinder) return;

        double maxRange = isNetherite ? 28.0D : 20.0D;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // 1. Scan cone for enemies to lock-on with Seed of Skandha (up to 8 enemies)
        if (st.viewfinderMarkedTargets.size() < 8) {
            AABB scanBox = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(3.5D);
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                    e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
            candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            for (LivingEntity cand : candidates) {
                if (st.viewfinderMarkedTargets.size() >= 8) break;
                if (st.viewfinderMarkedTargets.contains(cand.getUUID())) continue;
                Vec3 toCand = cand.getBoundingBox().getCenter().subtract(eye);
                double dist = toCand.length();
                if (dist < 0.5D || dist > maxRange) continue;
                double dot = toCand.scale(1.0D / dist).dot(look);
                if (dot >= 0.86D) { // ~30 degree camera viewfinder cone
                    st.viewfinderMarkedTargets.add(cand.getUUID());
                    applySkandhaMark(level, player, cand, isNetherite, now);
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.65F, 1.45F + st.viewfinderMarkedTargets.size() * 0.05F);
                }
            }
        }

        // 2. Remote Harvest & Item Vacuum along the viewfinder ray (flowers, berries, mature crops, ItemEntities)
        if (now - st.lastHarvestSweepTick >= 3L) {
            st.lastHarvestSweepTick = now;
            performViewfinderHarvestAndVacuum(level, player, eye, look, maxRange);
        }
    }

    private static void performViewfinderHarvestAndVacuum(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look, double maxRange) {
        // A. Vacuum dropped ItemEntities within the viewfinder cone directly into inventory
        AABB itemBox = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(2.5D);
        for (ItemEntity itemEnt : level.getEntitiesOfClass(ItemEntity.class, itemBox, e -> e.isAlive() && !e.getItem().isEmpty())) {
            Vec3 toItem = itemEnt.position().add(0.0D, 0.25D, 0.0D).subtract(eye);
            double dist = toItem.length();
            if (dist > maxRange) continue;
            if (dist < 2.5D || toItem.scale(1.0D / Math.max(0.01D, dist)).dot(look) >= 0.88D) {
                Vec3 startPos = itemEnt.position();
                ItemStack stack = itemEnt.getItem().copy();
                if (player.getInventory().add(stack)) {
                    if (stack.isEmpty()) {
                        itemEnt.discard();
                    } else {
                        itemEnt.setItem(stack);
                    }
                    level.sendParticles(DENDRO_GLOW_DUST, startPos.x, startPos.y + 0.3D, startPos.z, 6, 0.18D, 0.18D, 0.18D, 0.02D);
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.55F, 1.35F);
                }
            }
        }

        // B. Raycast blocks along look ray for flowers, sweet berry bushes, and mature crops
        int steps = (int) Math.ceil(maxRange * 1.5D);
        Set<BlockPos> checked = new HashSet<>();
        for (int i = 2; i <= steps; i++) {
            Vec3 sample = eye.add(look.scale(i / 1.5D));
            BlockPos basePos = BlockPos.containing(sample);
            for (BlockPos pos : new BlockPos[]{basePos, basePos.above(), basePos.below()}) {
                if (!checked.add(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                Block block = state.getBlock();

                // 1. Sweet Berry Bush (age >= 2)
                if (block instanceof SweetBerryBushBlock && state.hasProperty(SweetBerryBushBlock.AGE)) {
                    int age = state.getValue(SweetBerryBushBlock.AGE);
                    if (age >= 2) {
                        int count = 1 + level.random.nextInt(2) + (age == 3 ? 1 : 0);
                        giveOrDropHarvest(level, player, pos, new ItemStack(Items.SWEET_BERRIES, count));
                        level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
                        spawnHarvestParticles(level, pos);
                    }
                    continue;
                }

                // 2. Mature CropBlock (Wheat, Carrots, Potatoes, Beetroots, Torchflower/Pitcher crops)
                if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
                    List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        giveOrDropHarvest(level, player, pos, drop);
                    }
                    level.setBlock(pos, crop.getStateForAge(0), 2);
                    spawnHarvestParticles(level, pos);
                    continue;
                }

                // 3. Mature Cocoa / Nether Wart
                if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2) {
                    List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        giveOrDropHarvest(level, player, pos, drop);
                    }
                    level.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), 2);
                    spawnHarvestParticles(level, pos);
                    continue;
                }
                if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
                    List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        giveOrDropHarvest(level, player, pos, drop);
                    }
                    level.setBlock(pos, state.setValue(NetherWartBlock.AGE, 0), 2);
                    spawnHarvestParticles(level, pos);
                    continue;
                }

                // 4. Flowers (collects flower item & replants / leaves nature intact or harvests)
                if (state.is(BlockTags.FLOWERS) && !state.is(Blocks.WITHER_ROSE)) {
                    List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
                    if (!drops.isEmpty()) {
                        level.destroyBlock(pos, false, player);
                        for (ItemStack drop : drops) {
                            giveOrDropHarvest(level, player, pos, drop);
                        }
                        spawnHarvestParticles(level, pos);
                    }
                }
            }
        }
    }

    private static void giveOrDropHarvest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) {
            Block.popResource(level, player.blockPosition(), copy);
        }
    }

    private static void spawnHarvestParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(DENDRO_GLOW_DUST, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                5, 0.25D, 0.25D, 0.25D, 0.02D);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.75F, 1.35F);
    }

    /** Releases Right-Click: executes Tap E (< 8 ticks) or finishes Hold E Viewfinder (>= 8 ticks). */
    public static boolean onReleaseHoldUse(ServerPlayer player, ItemStack stack, int elapsedTicks, boolean isNetherite, ArcaneStaffItem cooldownItem) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        boolean wasViewfinder = st.holdingViewfinder || elapsedTicks >= 6;
        st.holdingViewfinder = false;
        syncPlayerStateToClients(player, st, now, true);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS, isNetherite);
        float skillDmg = (wasViewfinder ? 5.0F : 4.0F) * scale;
        float gaugeU = isNetherite ? 1.5F : 1.0F;

        if (!wasViewfinder) {
            // Tap E: 360-degree pirouette sweep marking nearby enemies within a tight ~2.8m radius with Seeds of Skandha
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_TAP_E);
            double radius = 2.8D;
            AABB box = player.getBoundingBox().inflate(radius, 2.0D, radius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
            nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            int marked = 0;
            for (LivingEntity target : nearby) {
                if (marked >= 8) break;
                marked++;
                applySkandhaMark(level, player, target, isNetherite, now);
                target.invulnerableTime = 0;
                ElementalGaugeManager.applyElement(level, target, Element.DENDRO, gaugeU, player, skillDmg);
            }
            for (int i = 0; i < 20; i++) {
                double ang = (Math.PI * 2.0D * i) / 20.0D;
                double rx = player.getX() + Math.cos(ang) * 2.0D;
                double rz = player.getZ() + Math.sin(ang) * 2.0D;
                level.sendParticles(DENDRO_GLOW_DUST, rx, player.getY() + 0.35D, rz, 2, 0.10D, 0.10D, 0.10D, 0.02D);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.45F);
            int cdTicks = Math.max(20, (int) Math.round(5.0D * 20.0D * StavesConfig.getCooldownMultiplier()));
            ElementalResonanceManager.applyCooldown(player, cooldownItem, cdTicks);
        } else {
            // Hold E release: Deal Dendro damage to all marked targets (and any nearby within 6m if none were aimed yet)
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_HOLD_E);
            if (st.viewfinderMarkedTargets.isEmpty()) {
                AABB fallbackBox = player.getBoundingBox().inflate(7.5D, 3.0D, 7.5D);
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, fallbackBox,
                        e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
                for (LivingEntity target : nearby) {
                    if (st.viewfinderMarkedTargets.size() >= 8) break;
                    st.viewfinderMarkedTargets.add(target.getUUID());
                    applySkandhaMark(level, player, target, isNetherite, now);
                }
            }
            List<LivingEntity> linkedEntities = new ArrayList<>();
            for (UUID targetId : st.viewfinderMarkedTargets) {
                if (level.getEntity(targetId) instanceof LivingEntity target && target.isAlive()) {
                    target.invulnerableTime = 0;
                    ElementalGaugeManager.applyElement(level, target, Element.DENDRO, gaugeU, player, skillDmg);
                    linkedEntities.add(target);
                }
            }
            drawSkandhaLinks(level, linkedEntities);
            st.viewfinderMarkedTargets.clear();
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.95F, 1.40F);
            int cdTicks = Math.max(20, (int) Math.round(6.0D * 20.0D * StavesConfig.getCooldownMultiplier()));
            ElementalResonanceManager.applyCooldown(player, cooldownItem, cdTicks);
        }
        return true;
    }

    public static void applySkandhaMark(ServerLevel level, ServerPlayer owner, LivingEntity target, boolean isNetherite, long now) {
        if (target == null || !target.isAlive()) return;
        SKANDHA_MARKS.put(target.getUUID(), new SkandhaMark(owner.getUUID(), isNetherite, now + 500L)); // 25.0s
        ModNetwork.sendStaffAnimation(target, 4101);
        level.sendParticles(DENDRO_GLOW_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                10, 0.30D, 0.30D, 0.30D, 0.03D);
    }

    private static void drawSkandhaLinks(ServerLevel level, List<LivingEntity> linked) {
        if (linked.size() < 2) return;
        for (int i = 0; i < linked.size(); i++) {
            LivingEntity a = linked.get(i);
            LivingEntity b = linked.get((i + 1) % linked.size());
            if (a != b && a.distanceToSqr(b) <= 576.0D) {
                ModNetwork.sendSkandhaLink(level, a, b, 16);
            }
        }
    }

    /**
     * Tri-Karma Purification: Triggered automatically whenever an enemy marked with a Seed of Skandha
     * suffers an Elemental Reaction or takes Dendro Core (Bloom / Hyperbloom / Burgeon) damage.
     */
    public static void onElementalReactionOrCoreDamage(
            ServerLevel level,
            LivingEntity triggeredTarget,
            @Nullable ServerPlayer triggerAttacker
    ) {
        if (level == null || triggeredTarget == null) return;
        SkandhaMark mark = SKANDHA_MARKS.get(triggeredTarget.getUUID());
        if (mark == null) return;
        long now = level.getGameTime();
        if (now >= mark.expireTick) {
            SKANDHA_MARKS.remove(triggeredTarget.getUUID());
            ModNetwork.sendStaffAnimation(triggeredTarget, 4100);
            return;
        }
        if (level.getServer() == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(mark.ownerId);
        if (owner == null || !owner.isAlive() || owner.level() != level) return;

        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(owner.getUUID(), k -> new NahidaPlayerState());
        boolean shrineActive = now < st.shrineUntilTick;
        int electroCnt = shrineActive ? st.shrineElectroCount : 0;
        long icdTicks = electroCnt >= 2 ? 32L : (electroCnt == 1 ? 40L : 50L); // 2.5s base -> 2.0s / 1.6s with Electro Shrine buff
        if (now - st.lastTriKarmaProcTick < icdTicks) return;
        st.lastTriKarmaProcTick = now;

        int pyroCnt = shrineActive ? st.shrinePyroCount : 0;
        float pyroMult = pyroCnt >= 2 ? 1.45F : (pyroCnt == 1 ? 1.30F : 1.0F);
        int emLvl = ElementalGaugeManager.getElementalMasteryLevel(owner);
        float emScale = 1.0F + emLvl * 0.20F;
        float baseScale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS, mark.netherite);
        float triKarmaDmg = 5.0F * baseScale * pyroMult * emScale;
        float gaugeU = mark.netherite ? 1.5F : 1.0F;

        List<LivingEntity> linkedTargets = new ArrayList<>();
        Iterator<Map.Entry<UUID, SkandhaMark>> it = SKANDHA_MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, SkandhaMark> entry = it.next();
            SkandhaMark m = entry.getValue();
            if (!mark.ownerId.equals(m.ownerId)) continue;
            if (now >= m.expireTick) {
                it.remove();
                continue;
            }
            if (level.getEntity(entry.getKey()) instanceof LivingEntity linked && linked.isAlive() && linked.distanceToSqr(triggeredTarget) <= 576.0D) {
                linkedTargets.add(linked);
            }
        }
        if (linkedTargets.isEmpty()) return;

        drawSkandhaLinks(level, linkedTargets);
        for (LivingEntity victim : linkedTargets) {
            victim.invulnerableTime = 0;
            ElementalGaugeManager.applyElementBypassIcd(level, victim, Element.DENDRO, gaugeU, owner, triKarmaDmg);
            level.sendParticles(DENDRO_GLOW_DUST, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6D, victim.getZ(),
                    12, 0.32D, 0.32D, 0.32D, 0.04D);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6D, victim.getZ(),
                    8, 0.28D, 0.28D, 0.28D, 0.03D);
        }
        level.playSound(null, triggeredTarget.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.90F, 1.50F);
    }

    /**
     * Burst [R] (Illusory Heart — Shrine of Maya):
     * Grants 1.7s (34 ticks) of I-frames, triggers 5-star prayer levitation + emerald dome expansion,
     * grants +2 EM levels inside the Shrine, and activates Pyro / Electro / Hydro Shrine buffs.
     */
    public static boolean triggerNahidaUlti(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return false;
        ItemStack stack = getHeldNahidaStack(player);
        if (stack.isEmpty()) return false;

        long now = level.getGameTime();
        NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
        if (now < st.ultiCooldownUntilTick) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.DENDRO, st.ultiCooldownUntilTick - now);
            return false;
        }

        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        int baseCdTicks = 270; // 13.5s
        int adjustedCd = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? Math.max(20, (int) Math.round(baseCdTicks * 0.90D))
                : baseCdTicks;
        st.ultiCooldownUntilTick = now + adjustedCd;
        st.iframeUntilTick = now + 42L; // 2.0s (40 ticks) Burst cast animation + root
        player.invulnerableTime = Math.max(player.invulnerableTime, 42);
        ArchonUltiManager.applyBurstRoot(player, 40);
        markActiveAction(player, now);

        // Count Pyro / Electro / Hydro staves in player's inventory + nearby allies
        int pyro = 0;
        int electro = 0;
        int hydro = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof ArcaneStaffItem staff)) continue;
            Element el = staff.kind() == ArcaneStaffItem.Kind.OMNI
                    ? ArcaneStaffItem.getOmniActiveElement(s)
                    : staff.getElement();
            if (el == Element.PYRO) pyro++;
            else if (el == Element.ELECTRO) electro++;
            else if (el == Element.HYDRO) hydro++;
        }
        // Netherite C1 (The Seed of Stored Knowledge): +1 bonus count to Pyro, Electro, and Hydro simultaneously!
        if (isNeth) {
            pyro++;
            electro++;
            hydro++;
        }

        long bonusHydroTicks = hydro >= 2 ? 160L : (hydro == 1 ? 100L : 0L); // +5s / +8s duration
        long totalDurationTicks = 300L + bonusHydroTicks; // 15s base -> up to 23s
        final int fPyro = Math.min(2, pyro);
        final int fElectro = Math.min(2, electro);
        final int fHydro = Math.min(2, hydro);

        InteractionHand hand = player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, hand);

        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NAHIDA_BURST);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);

        ArchonUltiManager.scheduleBurstAction(player, 38, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
            com.velorise.staves.advancement.ModAdvancements.trigger(player, com.velorise.staves.advancement.ModAdvancements.EVENT_ILLUSORY_HEART);
            long eruptNow = sl.getGameTime();
            st.shrineUntilTick = eruptNow + totalDurationTicks;
            st.shrineCenter = player.position();
            st.shrineRadius = isNeth ? 15.0D : 12.0D;
            st.shrineNetherite = isNeth;
            st.shrinePyroCount = fPyro;
            st.shrineElectroCount = fElectro;
            st.shrineHydroCount = fHydro;

            int shrineMask = 1 | (st.shrinePyroCount > 0 ? 2 : 0) | (st.shrineElectroCount > 0 ? 4 : 0)
                    | (st.shrineHydroCount > 0 ? 8 : 0) | (st.shrineNetherite ? 16 : 0);
            ModNetwork.sendNahidaShrine(sl, st.shrineCenter, st.shrineRadius, (int) totalDurationTicks, shrineMask);
            syncPlayerStateToClients(player, st, eruptNow, true);

            for (int i = 0; i < 36; i++) {
                double ang = (Math.PI * 2.0D * i) / 36.0D;
                double rx = st.shrineCenter.x + Math.cos(ang) * (st.shrineRadius * 0.75D);
                double rz = st.shrineCenter.z + Math.sin(ang) * (st.shrineRadius * 0.75D);
                sl.sendParticles(DENDRO_GLOW_DUST, rx, st.shrineCenter.y + 0.5D, rz, 3, 0.25D, 0.6D, 0.25D, 0.03D);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, rx, st.shrineCenter.y + 2.5D, rz, 2, 0.3D, 0.5D, 0.3D, 0.02D);
            }
            sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.25F);
        });
        return true;
    }

    /** Returns bonus EM levels (Basic +1, Netherite +2) if the entity is standing inside an active Shrine of Maya. */
    public static int getBonusEmLevels(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return 0;
        long now = level.getGameTime();
        int maxBonus = 0;
        for (ServerPlayer caster : level.players()) {
            NahidaPlayerState st = PLAYER_STATES.get(caster.getUUID());
            if (st != null && now < st.shrineUntilTick) {
                if (caster == player || caster.isAlliedTo(player) || ElementalGaugeManager.isRaidenProtectedAlly(caster, player)) {
                    if (player.position().distanceToSqr(st.shrineCenter) <= (st.shrineRadius + 1.5D) * (st.shrineRadius + 1.5D)) {
                        int bonus = st.shrineNetherite ? 2 : 1;
                        if (bonus > maxBonus) {
                            maxBonus = bonus;
                        }
                    }
                }
            }
        }
        return maxBonus;
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            NahidaPlayerState st = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new NahidaPlayerState());
            ServerLevel level = player.serverLevel();

            // 1. Check Idle Swing (standing still holding Nahida's catalyst for >= 70 ticks / 3.5s)
            boolean holdingNahida = isHoldingNahida(player);
            if (st.lastStillPos == null || st.lastStillPos.equals(Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distFromAnchorSq = player.position().distanceToSqr(st.lastStillPos);
            boolean onGround = player.onGround();
            boolean notAttacking = !player.isUsingItem() && !player.swinging && !st.holdingViewfinder && (now - st.lastActionTick) >= 30L;

            if (holdingNahida && onGround && notAttacking) {
                if (distFromAnchorSq < 0.16D) { // within 0.4 blocks of anchor position
                    st.stillTicks++;
                    if (st.stillTicks >= 70 && !st.idleSwingActive) {
                        st.idleSwingActive = true;
                        st.lastStillPos = player.position(); // lock anchor exactly here
                        syncPlayerStateToClients(player, st, now, true);
                    }
                } else {
                    // Player walked away
                    st.stillTicks = 0;
                    st.lastStillPos = player.position();
                    if (st.idleSwingActive) {
                        st.idleSwingActive = false;
                        syncPlayerStateToClients(player, st, now, true);
                    }
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                if (st.idleSwingActive) {
                    st.idleSwingActive = false;
                    syncPlayerStateToClients(player, st, now, true);
                }
            }

            // Periodically refresh idle swing state sync every 20 ticks so client never drops it
            if (st.idleSwingActive && now % 20L == 0L) {
                syncPlayerStateToClients(player, st, now, false);
            }

            if (st.holdingViewfinder && (!holdingNahida || !player.isUsingItem())) {
                st.holdingViewfinder = false;
                syncPlayerStateToClients(player, st, now, true);
            }

            // 2. Ambient Shrine of Maya particles
            if (now < st.shrineUntilTick && now % 10L == 0L) {
                for (int i = 0; i < 16; i++) {
                    double ang = (Math.PI * 2.0D * i) / 16.0D + (now * 0.02D);
                    double rx = st.shrineCenter.x + Math.cos(ang) * st.shrineRadius;
                    double rz = st.shrineCenter.z + Math.sin(ang) * st.shrineRadius;
                    level.sendParticles(DENDRO_GLOW_DUST, rx, st.shrineCenter.y + 0.25D, rz, 1, 0.1D, 0.2D, 0.1D, 0.01D);
                }
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        st.shrineCenter.x, st.shrineCenter.y + 3.5D, st.shrineCenter.z,
                        6, st.shrineRadius * 0.45D, 1.5D, st.shrineRadius * 0.45D, 0.02D);
            }

            syncPlayerStateToClients(player, st, now, false);
        }

        // Cleanup expired Skandha marks every 20 ticks
        if (now % 20L == 0L && !SKANDHA_MARKS.isEmpty()) {
            Iterator<Map.Entry<UUID, SkandhaMark>> it = SKANDHA_MARKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, SkandhaMark> entry = it.next();
                if (now >= entry.getValue().expireTick) {
                    UUID targetId = entry.getKey();
                    it.remove();
                    for (ServerLevel lvl : server.getAllLevels()) {
                        if (lvl.getEntity(targetId) instanceof LivingEntity le) {
                            ModNetwork.sendStaffAnimation(le, 4100);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void syncPlayerStateToClients(ServerPlayer player, NahidaPlayerState st, long now, boolean force) {
        int playerCode = st.holdingViewfinder ? 4002 : (st.idleSwingActive ? 4001 : 4000);
        if (force || playerCode != st.lastSyncedPlayerCode) {
            st.lastSyncedPlayerCode = playerCode;
            ModNetwork.sendStaffAnimation(player, playerCode);
        }

        int shrineCode;
        if (now < st.shrineUntilTick) {
            int mask = 1;
            if (st.shrinePyroCount > 0) mask |= 2;
            if (st.shrineElectroCount > 0) mask |= 4;
            if (st.shrineHydroCount > 0) mask |= 8;
            if (st.shrineNetherite) mask |= 16;
            shrineCode = 4200 + mask;
        } else {
            shrineCode = 4200;
        }
        if (force || shrineCode != st.lastSyncedShrineCode) {
            st.lastSyncedShrineCode = shrineCode;
            ModNetwork.sendStaffAnimation(player, shrineCode);
        }
    }
}
