package com.velorise.staves.item;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.DendroCoreManager;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalResonanceManager;
import com.velorise.staves.element.ElectrifiedWaterManager;
import com.velorise.staves.element.EnvironmentalBlockManager;
import com.velorise.staves.entity.OmniOrbEntity;
import com.velorise.staves.event.ArcaneStaffEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Standalone magic staves powered by vanilla durability and cooldowns. */
public final class ArcaneStaffItem extends Item {
    private static final String TAG_INFERNO_SHOTS = "ArcaneStaffInfernoShots";
    private static final String TAG_INFERNO_LAST = "ArcaneStaffInfernoLast";
    private static final String TAG_WEB_SHOTS = "ArcaneStaffWebShots";
    private static final String TAG_WEB_LAST = "ArcaneStaffWebLast";
    private static final String TAG_WITHER_SHOTS = "ArcaneStaffWitherShots";
    private static final String TAG_WITHER_LAST = "ArcaneStaffWitherLast";
    private static final String TAG_FROST_SHOTS = "ArcaneStaffFrostShots";
    private static final String TAG_FROST_LAST = "ArcaneStaffFrostLast";
    private static final String TAG_SHULKER_SHOTS = "ArcaneStaffShulkerShots";
    private static final String TAG_SHULKER_LAST = "ArcaneStaffShulkerLast";
    private static final int BURST_RESET_TICKS = 100; // 5.0s window to hold remaining burst shots
    private static final int BURST_INTERVAL_TICKS = 11; // 11 ticks (0.55s) between burst shots to bypass the vanilla 10-tick damage iframe

    public enum Kind {
        DRAGONFIRE, ECHO, INFERNO, GHASTFIRE,
        WITHER, SHULKER, GUARDIAN, SPLENDOR, EVOKER, VORTEX, LIGHTNING, FROST, WEB, WIND, OMNI
    }

    public static final String TAG_OMNI_MODE = "OmniMode";
    public static final String TAG_SPLENDOR_PNEUMA = "SplendorPneuma";

    public static boolean isSplendorPneuma(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(TAG_SPLENDOR_PNEUMA);
    }

    public static void setSplendorPneuma(ItemStack stack, boolean pneuma) {
        if (stack.isEmpty()) return;
        if (!pneuma) {
            CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
            if (existing == null) return;
            CompoundTag copy = existing.copyTag();
            copy.remove(TAG_SPLENDOR_PNEUMA);
            if (copy.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
            else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(copy));
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(TAG_SPLENDOR_PNEUMA, true));
        }
    }

    private static final Map<UUID, ItemStack> ACTIVE_CHANNELING = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SPLENDOR_SWITCHED_THIS_HOLD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SPLENDOR_LAST_SWITCH_TIME = new ConcurrentHashMap<>();
    private static final long SPLENDOR_SWITCH_COOLDOWN_TICKS = 20L;
    private static volatile java.lang.reflect.Method cachedMaxHpMethod;
    private static volatile boolean maxHpMethodResolved;

    public static void clearAll() {
        ACTIVE_CHANNELING.clear();
        OmniOrbEntity.clearCycleCache();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId == null) return;
        ACTIVE_CHANNELING.remove(playerId);
    }

    public static boolean isOmniStaff(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArcaneStaffItem staff && staff.kind == Kind.OMNI;
    }

    public static int getOmniModeIndex(ItemStack stack) {
        if (!isOmniStaff(stack)) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        return OmniOrbEntity.clampMode(data.copyTag().getInt(TAG_OMNI_MODE));
    }

    public static void setOmniModeIndex(ItemStack stack, int mode) {
        if (!isOmniStaff(stack)) return;
        int clamped = OmniOrbEntity.clampMode(mode);
        if (clamped == 0) {
            CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
            if (existing == null) return;
            CompoundTag copy = existing.copyTag();
            copy.remove(TAG_OMNI_MODE);
            if (copy.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(copy));
            }
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_OMNI_MODE, clamped));
        }
    }

    public static @Nullable Element getOmniActiveElement(ItemStack stack) {
        return OmniOrbEntity.modeToElement(getOmniModeIndex(stack));
    }

    private final Kind kind;
    private final boolean isNetherite;

    public ArcaneStaffItem(Kind kind, int maxDurability) {
        this(kind, maxDurability, false);
    }

    public ArcaneStaffItem(Kind kind, int maxDurability, boolean isNetherite) {
        super(isNetherite
            ? new Item.Properties().stacksTo(1).durability(maxDurability).fireResistant()
            : new Item.Properties().stacksTo(1).durability(maxDurability));
        this.kind = kind;
        this.isNetherite = isNetherite;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isNetherite() {
        return isNetherite;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElementalResonanceManager.isOverloaded(player)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.velorise_staves.staff_overload", StavesConfig.getMaxStavesInInventory())
                                .withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (kind == Kind.SPLENDOR && player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                long now = level.getGameTime();
                long lastSwitch = SPLENDOR_LAST_SWITCH_TIME.getOrDefault(sp.getUUID(), -100L);
                if (now - lastSwitch >= SPLENDOR_SWITCH_COOLDOWN_TICKS) {
                    SPLENDOR_LAST_SWITCH_TIME.put(sp.getUUID(), now);
                    switchSplendorArkhe(sp, stack);
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (kind != Kind.SPLENDOR && player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (isCharged()) {
            if (kind == Kind.SPLENDOR && !level.isClientSide()) {
                SPLENDOR_SWITCHED_THIS_HOLD.put(player.getUUID(), false);
            }
            player.startUsingItem(hand);
            if (!level.isClientSide()) {
                if (isChannelingKind()) {
                    ACTIVE_CHANNELING.put(player.getUUID(), stack);
                    if (kind == Kind.GUARDIAN && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                        int absorbed = ElementalGaugeManager.consumeSourcewaterDropletsForChannel(sl, sp, 12.0D, 3);
                        sp.getPersistentData().putBoolean("VeloriseGuardianInstant", absorbed > 0);
                    }
                }
                SoundEvent cueSound = switch (kind) {
                    case ECHO -> SoundEvents.WARDEN_SONIC_CHARGE;
                    case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
                    case GHASTFIRE -> SoundEvents.GHAST_WARN;
                    case VORTEX -> SoundEvents.DEEPSLATE_STEP;
                    case SPLENDOR -> SoundEvents.AMETHYST_BLOCK_RESONATE;
                    default -> SoundEvents.GUARDIAN_AMBIENT;
                };
                float cuePitch = switch (kind) {
                    case ECHO -> 0.85F;
                    case LIGHTNING -> 1.5F;
                    case GHASTFIRE -> 0.9F;
                    case VORTEX -> 0.75F;
                    case SPLENDOR -> 1.15F;
                    default -> 1.25F;
                };
                level.playSound(null, player.blockPosition(), cueSound, SoundSource.PLAYERS, 0.8F, cuePitch);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);

        if (isBurstKind()) {
            if (prepareBurst(serverPlayer, burstShotsTag(), burstLastTag(), burstSlotTag(), BURST_RESET_TICKS)) {
                return InteractionResultHolder.fail(stack);
            }
        }

        boolean shouldConsumeDurability = !isBurstKind() || isFirstBurstShot(serverPlayer, burstShotsTag());

        boolean activated = switch (kind) {
            case DRAGONFIRE -> shootDragonfire(serverPlayer);
            case INFERNO -> shootInferno(serverPlayer);
            case GHASTFIRE -> shootGhastfire(serverPlayer);
            case WITHER -> shootWither(serverPlayer);
            case SHULKER -> shootShulker(serverPlayer);
            case EVOKER -> castFangs(serverPlayer);
            case VORTEX -> castVortexVanquisher(serverPlayer);
            case FROST -> shootSnowball(serverPlayer, "frost", 1.45F);
            case WEB -> shootSnowball(serverPlayer, "web", 1.35F);
            case WIND -> shootWind(serverPlayer);
            case SPLENDOR -> castSplendorSummon(serverPlayer, stack);
            case OMNI -> shootOmni(serverPlayer, stack);
            case ECHO, GUARDIAN, LIGHTNING -> false;
        };

        if (!activated) return InteractionResultHolder.fail(stack);

        if (shouldConsumeDurability) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        if (isBurstKind()) {
            advanceBurst(serverPlayer, burstShotsTag(), burstLastTag(), burstSlotTag(), maxBurstShots(), getCooldownTicks());
        } else {
            ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand, true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (kind == Kind.SPLENDOR && entity instanceof Player p && !p.isShiftKeyDown()
                && com.velorise.staves.entity.HydroSalonSummonEntity.hasActiveSummon(p)) {
            com.velorise.staves.element.ElementalGaugeManager.applyFurinaWaterWalking(p);
        }
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (level.getGameTime() % 20L == 0L && ElementalGaugeManager.getElementalMasteryLevel(stack) > 0) {
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    player, com.velorise.staves.advancement.ModAdvancements.EVENT_ELEMENTAL_MASTERY);
        }

        if (isChannelingKind()) {
            ItemStack chStack = ACTIVE_CHANNELING.get(player.getUUID());
            if (chStack == stack) {
                if (!player.isUsingItem() || player.getUseItem() != stack) {
                    ACTIVE_CHANNELING.remove(player.getUUID());
                    if (!player.getCooldowns().isOnCooldown(this)) {
                        ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
                    }
                }
            }
        }

        if (!isBurstKind()) return;
        String tagShots = burstShotsTag();
        String tagLast = burstLastTag();
        String tagSlot = burstSlotTag();
        int shots = player.getPersistentData().getInt(tagShots);
        if (shots <= 0) return;

        int activeSlot = player.getPersistentData().getInt(tagSlot);
        boolean slotChanged = player.getInventory().selected != activeSlot
                || (player.getMainHandItem() != stack && player.getOffhandItem() != stack);

        long elapsed = level.getGameTime() - player.getPersistentData().getLong(tagLast);
        if (slotChanged || elapsed < 0L || elapsed >= BURST_RESET_TICKS) {
            int cdForShots = Math.max(1, (int) Math.round((double) getCooldownTicks() * shots / (double) maxBurstShots()));
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
            player.getPersistentData().remove(tagSlot);
            ElementalResonanceManager.applyCooldown(player, this, cdForShots);
        }
    }

    private boolean isBurstKind() {
        return kind == Kind.INFERNO || kind == Kind.WEB || kind == Kind.WITHER || kind == Kind.FROST || kind == Kind.SHULKER;
    }

    private int maxBurstShots() {
        return kind == Kind.SHULKER ? 2 : 3;
    }

    private String burstShotsTag() {
        String base = switch (kind) {
            case INFERNO -> TAG_INFERNO_SHOTS;
            case WEB -> TAG_WEB_SHOTS;
            case WITHER -> TAG_WITHER_SHOTS;
            case FROST -> TAG_FROST_SHOTS;
            case SHULKER -> TAG_SHULKER_SHOTS;
            default -> "";
        };
        return isNetherite ? base + "Netherite" : base;
    }

    private String burstLastTag() {
        String base = switch (kind) {
            case INFERNO -> TAG_INFERNO_LAST;
            case WEB -> TAG_WEB_LAST;
            case WITHER -> TAG_WITHER_LAST;
            case FROST -> TAG_FROST_LAST;
            case SHULKER -> TAG_SHULKER_LAST;
            default -> "";
        };
        return isNetherite ? base + "Netherite" : base;
    }

    private String burstSlotTag() {
        String base = switch (kind) {
            case INFERNO -> "BurstSlotInferno";
            case WEB -> "BurstSlotWeb";
            case WITHER -> "BurstSlotWither";
            case FROST -> "BurstSlotFrost";
            case SHULKER -> "BurstSlotShulker";
            default -> "BurstSlot";
        };
        return isNetherite ? base + "Netherite" : base;
    }

    private boolean isFirstBurstShot(ServerPlayer player, String tagShots) {
        return player.getPersistentData().getInt(tagShots) <= 0;
    }

    private boolean isCharged() {
        return kind == Kind.ECHO || kind == Kind.GUARDIAN || kind == Kind.LIGHTNING || kind == Kind.GHASTFIRE || kind == Kind.VORTEX || kind == Kind.SPLENDOR;
    }

    private boolean isChannelingKind() {
        return kind == Kind.LIGHTNING || kind == Kind.GUARDIAN;
    }

    public int getCooldownTicks() {
        double seconds = StavesConfig.getStaffCooldownSeconds(kind);
        double mult = StavesConfig.getCooldownMultiplier();
        return Math.max(1, (int) Math.round(seconds * 20.0D * mult));
    }

    public int getVortexCooldownTicks(boolean holdJadeShield) {
        double tapSeconds = StavesConfig.getStaffCooldownSeconds(Kind.VORTEX); // 4.0s default
        double seconds = holdJadeShield ? (tapSeconds * 3.0D) : tapSeconds; // Tap = 4.0s, Hold = 12.0s
        double mult = StavesConfig.getCooldownMultiplier();
        return Math.max(1, (int) Math.round(seconds * 20.0D * mult));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return StavesConfig.getMaxDurability(kind, isNetherite);
    }

    private boolean prepareBurst(ServerPlayer player, String tagShots, String tagLast, String tagSlot, int resetTicks) {
        int shots = player.getPersistentData().getInt(tagShots);
        if (shots <= 0) return false;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(tagLast);
        int activeSlot = player.getPersistentData().getInt(tagSlot);
        boolean slotChanged = player.getInventory().selected != activeSlot;
        if (elapsed < 0L || elapsed >= resetTicks || slotChanged) {
            int cd = Math.max(1, (int) Math.round((double) getCooldownTicks() * shots / (double) maxBurstShots()));
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
            player.getPersistentData().remove(tagSlot);
            if (!player.getCooldowns().isOnCooldown(this)) {
                ElementalResonanceManager.applyCooldown(player, this, cd);
            }
            return true;
        }
        return false;
    }

    private void advanceBurst(ServerPlayer player, String tagShots, String tagLast, String tagSlot, int maxShots, int cdTicks) {
        int shots = player.getPersistentData().getInt(tagShots) + 1;
        player.getPersistentData().putLong(tagLast, player.level().getGameTime());
        player.getPersistentData().putInt(tagSlot, player.getInventory().selected);
        if (shots >= maxShots) {
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
            player.getPersistentData().remove(tagSlot);
            ElementalResonanceManager.applyCooldown(player, this, cdTicks);
        } else {
            player.getPersistentData().putInt(tagShots, shots);
            player.getCooldowns().addCooldown(this, BURST_INTERVAL_TICKS); // 11 ticks between burst shots to bypass vanilla 10-tick damage iframe
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (kind == Kind.LIGHTNING) return 60;
        if (kind == Kind.GUARDIAN) return 70;
        if (kind == Kind.GHASTFIRE) return 40; // 2 seconds charge
        if (kind == Kind.VORTEX) return 30; // 1.5s hold for Zhongli Jade Shield + AoE Geo shockwave
        if (kind == Kind.SPLENDOR) return 72000; // Hold locks after 0.5s Arkhe switch until player releases and presses again
        return isCharged() ? 30 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (kind == Kind.VORTEX || kind == Kind.SPLENDOR) {
            return UseAnim.NONE;
        }
        return isCharged() ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!isCharged() || level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) return;
        if (ElementalResonanceManager.isOverloaded(player)) {
            player.stopUsingItem();
            return;
        }
        if (kind == Kind.LIGHTNING) {
            int elapsed = 60 - count;
            if (elapsed < 0 || elapsed % 5 != 0) return;
            fireChanneledLightning(serverLevel, player, elapsed);
            if (elapsed % 20 == 0) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            return;
        }
        if (kind == Kind.GUARDIAN) {
            int elapsed = 70 - count;
            if (elapsed < 0) return;
            boolean instant = player.getPersistentData().getBoolean("VeloriseGuardianInstant");
            if (!instant && elapsed < 10) {
                // Brief 0.5s wind-up when no Sourcewater Droplets were absorbed
                if (elapsed % 2 == 0) {
                    Vec3 eye = entity.getEyePosition().add(entity.getLookAngle().scale(0.75D));
                    serverLevel.sendParticles(ParticleTypes.SPLASH, eye.x, eye.y, eye.z, 4, 0.15D, 0.15D, 0.15D, 0.03D);
                }
                return;
            }
            int beamElapsed = instant ? elapsed : (elapsed - 10);
            if (beamElapsed >= 60) {
                player.stopUsingItem();
                return;
            }
            if (beamElapsed % 8 != 0) return; // 8 hits over 3.0s (60 ticks): every 8 ticks (0.4s)
            LivingEntity target = findTarget(player, isNetherite ? 28.0D : 24.0D, 1.2D);
            fireGuardian(player, target);
            if (beamElapsed == 0 || beamElapsed == 24 || beamElapsed == 48) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            return;
        }
        if (kind == Kind.VORTEX) {
            int elapsed = 30 - count;
            if (count % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        4, 0.45D, 0.5D, 0.45D, 0.02D);
            }
            if (elapsed == 10 || elapsed == 20) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.BASALT_STEP, SoundSource.PLAYERS, 0.65F, 0.75F + (elapsed * 0.01F));
            }
            return;
        }
        if (kind == Kind.SPLENDOR) {
            int elapsed = 72000 - count;
            if (!Boolean.TRUE.equals(SPLENDOR_SWITCHED_THIS_HOLD.get(player.getUUID()))) {
                if (elapsed >= 10) {
                    SPLENDOR_SWITCHED_THIS_HOLD.put(player.getUUID(), true);
                    long now = serverLevel.getGameTime();
                    long lastSwitch = SPLENDOR_LAST_SWITCH_TIME.getOrDefault(player.getUUID(), -100L);
                    if (now - lastSwitch >= SPLENDOR_SWITCH_COOLDOWN_TICKS) {
                        SPLENDOR_LAST_SWITCH_TIME.put(player.getUUID(), now);
                        switchSplendorArkhe(player, stack);
                    }
                } else if (count % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            5, 0.45D, 0.45D, 0.45D, 0.03D);
                }
            }
            return;
        }
        if (kind == Kind.GHASTFIRE) {
            if (count % 3 == 0) {
                Vec3 eye = entity.getEyePosition().add(entity.getLookAngle().scale(0.8D));
                serverLevel.sendParticles(ParticleTypes.FLAME, eye.x, eye.y, eye.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.SMOKE, eye.x, eye.y, eye.z, 1, 0.08D, 0.08D, 0.08D, 0.01D);
            }
            return;
        }
        if (count % 5 != 0) return;
        Vec3 eye = entity.getEyePosition();
        serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, eye.x, eye.y, eye.z, 2, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!isCharged() || level.isClientSide() || !(entity instanceof ServerPlayer player)) return stack;
        if (kind == Kind.LIGHTNING) {
            finishLightningStorm(player);
            return stack;
        }
        if (kind == Kind.GUARDIAN) {
            finishGuardianBeam(player);
            return stack;
        }
        if (ElementalResonanceManager.isOverloaded(player)) return stack;
        if (kind == Kind.SPLENDOR) {
            SPLENDOR_SWITCHED_THIS_HOLD.remove(player.getUUID());
            return stack;
        }
        if (kind == Kind.VORTEX) {
            if (!player.getCooldowns().isOnCooldown(this) && castVortexVanquisher(player, true)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
                ElementalResonanceManager.applyCooldown(player, this, getVortexCooldownTicks(true));
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            return stack;
        }
        if (kind == Kind.GHASTFIRE) {
            shootGhastfire(player);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
            player.awardStat(Stats.ITEM_USED.get(this));
            return stack;
        }
        fireEcho(player);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (kind == Kind.LIGHTNING) {
                finishLightningStorm(player);
            } else if (kind == Kind.GUARDIAN) {
                finishGuardianBeam(player);
            } else if (kind == Kind.SPLENDOR) {
                boolean alreadySwitched = Boolean.TRUE.equals(SPLENDOR_SWITCHED_THIS_HOLD.remove(player.getUUID()));
                int elapsed = this.getUseDuration(stack, entity) - timeLeft;
                if (alreadySwitched) {
                    SPLENDOR_LAST_SWITCH_TIME.put(player.getUUID(), level.getGameTime());
                    player.getCooldowns().addCooldown(this, 10);
                } else if (elapsed >= 10 || player.isShiftKeyDown()) {
                    long now = level.getGameTime();
                    long lastSwitch = SPLENDOR_LAST_SWITCH_TIME.getOrDefault(player.getUUID(), -100L);
                    if (now - lastSwitch >= SPLENDOR_SWITCH_COOLDOWN_TICKS) {
                        SPLENDOR_LAST_SWITCH_TIME.put(player.getUUID(), now);
                        switchSplendorArkhe(player, stack);
                        player.getCooldowns().addCooldown(this, 10);
                    }
                } else if (!ElementalResonanceManager.isOverloaded(player) && !player.getCooldowns().isOnCooldown(this)) {
                    if (castSplendorSummon(player, stack)) {
                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
                        ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
                        player.awardStat(Stats.ITEM_USED.get(this));
                    }
                }
            } else if (kind == Kind.VORTEX) {
                if (!ElementalResonanceManager.isOverloaded(player) && !player.getCooldowns().isOnCooldown(this)) {
                    int elapsed = this.getUseDuration(stack, entity) - timeLeft;
                    boolean holdShield = elapsed >= 28 || player.isShiftKeyDown();
                    if (castVortexVanquisher(player, holdShield)) {
                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
                        ElementalResonanceManager.applyCooldown(player, this, getVortexCooldownTicks(holdShield));
                        player.awardStat(Stats.ITEM_USED.get(this));
                    }
                }
            }
        }
    }

    private void finishLightningStorm(ServerPlayer player) {
        ACTIVE_CHANNELING.remove(player.getUUID());
        ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private void finishGuardianBeam(ServerPlayer player) {
        ACTIVE_CHANNELING.remove(player.getUUID());
        player.getPersistentData().remove("VeloriseGuardianInstant");
        ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean shootDragonfire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        DragonFireball projectile = EntityType.DRAGON_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_dragonfire" : "dragonfire", 32.0D, 1.35F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootGhastfire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LargeFireball projectile = EntityType.FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_ghastfire" : "ghastfire", 36.0D, 1.2F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootInferno(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SmallFireball projectile = EntityType.SMALL_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_inferno" : "inferno", 30.0D, 1.5F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootWither(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WitherSkull projectile = EntityType.WITHER_SKULL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_wither" : "wither", 32.0D, 1.35F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.85F, 1.15F);
        return true;
    }

    /**
     * Fires 1 homing Shulker Bullet per right-click (2-shot sequential burst / double-tap).
     */
    private boolean shootShulker(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double range = 30.0D;
        LivingEntity target = findTarget(player, range, 1.5D);
        String kindTag = isNetherite ? "netherite_shulker" : "shulker";
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 spawnPos = player.getEyePosition().add(direction.scale(0.8D));
        ShulkerBullet projectile;
        if (target != null) {
            projectile = new ShulkerBullet(level, player, target, Direction.Axis.Y);
            projectile.moveTo(spawnPos.x, spawnPos.y - 0.1D, spawnPos.z, player.getYRot(), player.getXRot());
            projectile.setDeltaMovement(direction.scale(0.55D));
            projectile.getPersistentData().putString(ArcaneStaffEvents.TAG_KIND, kindTag);
            ArcaneStaffEvents.markRange(projectile, player.position(), range);
        } else {
            projectile = new ShulkerBullet(EntityType.SHULKER_BULLET, level);
            prepareProjectile(projectile, player, kindTag, range, 1.25F, direction);
            projectile.setPos(spawnPos.x, spawnPos.y - 0.1D, spawnPos.z);
        }
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.15F);
        return true;
    }

    private boolean shootWind(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        net.minecraft.world.entity.projectile.windcharge.WindCharge projectile =
            new net.minecraft.world.entity.projectile.windcharge.WindCharge(player, level, player.getX(), player.getEyeY() - 0.1D, player.getZ());
        String tag = isNetherite ? "netherite_wind" : "wind";
        prepareProjectile(projectile, player, tag, 28.0D, 1.6F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootOmni(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        int mode = getOmniModeIndex(stack);
        OmniOrbEntity orb = new OmniOrbEntity(VeloriseStavesMod.OMNI_ORB.get(), level, player, isNetherite, mode);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawn = player.getEyePosition().add(look.scale(0.65D));
        orb.setPos(spawn.x, spawn.y - 0.1D, spawn.z);
        orb.shoot(look.x, look.y, look.z, isNetherite ? 2.1F : 1.85F, 0.0F);
        level.addFreshEntity(orb);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, isNetherite ? 1.35F : 1.15F);
        return true;
    }

    private boolean shootSnowball(ServerPlayer player, String kind, float velocity) {
        ServerLevel level = player.serverLevel();
        Snowball projectile = EntityType.SNOWBALL.create(level);
        if (projectile == null) return false;
        String tag = isNetherite ? "netherite_" + kind : kind;
        prepareProjectile(projectile, player, tag, 28.0D, velocity);
        level.addFreshEntity(projectile);
        SoundEvent sound = "web".equals(kind) ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SNOW_GOLEM_SHOOT;
        float pitch = "web".equals(kind) ? 1.45F : 1.1F;
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, pitch);
        return true;
    }

    private static void prepareProjectile(Projectile projectile, ServerPlayer player, String kind, double range, float velocity) {
        prepareProjectile(projectile, player, kind, range, velocity, player.getLookAngle());
    }

    private static void prepareProjectile(Projectile projectile, ServerPlayer player, String kind, double range, float velocity, Vec3 direction) {
        Vec3 shot = direction.normalize();
        projectile.setOwner(player);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        projectile.shoot(shot.x, shot.y, shot.z, velocity, 0.0F);
        projectile.getPersistentData().putString(ArcaneStaffEvents.TAG_KIND, kind);
        ArcaneStaffEvents.markRange(projectile, player.position(), range);
    }

    private boolean castFangs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 0.001D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        int count = isNetherite ? 13 : 9;
        float yaw = (float)Math.atan2(flat.z, flat.x);

        for (int i = 0; i < count; i++) {
            double distance = 1.5D + i * 1.5D;
            double x = player.getX() + flat.x * distance;
            double z = player.getZ() + flat.z * distance;
            double y = findGroundY(level, x, player.getY(), z);
            EvokerFangs fangs = new EvokerFangs(level, x, y, z, yaw, i * 2, player);
            fangs.addTag("velorise_staves:geo_fangs");
            if (isNetherite) {
                fangs.addTag("velorise_staves:netherite_geo_fangs");
            }
            level.addFreshEntity(fangs);
        }
        boolean castUnderFeet = player.getXRot() >= 50.0F;
        double constructX;
        double constructZ;
        if (castUnderFeet) {
            constructX = player.getX();
            constructZ = player.getZ();
        } else {
            double firstX = player.getX() + flat.x * 1.5D;
            double firstZ = player.getZ() + flat.z * 1.5D;
            constructX = Math.floor(firstX) + 0.5D;
            constructZ = Math.floor(firstZ) + 0.5D;
        }
        double constructY = findGroundY(level, constructX, player.getY(), constructZ);
        com.velorise.staves.entity.GeoFangPillarEntity.enforceMaxConstructsForOwner(
                level, com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR.get(), player.getUUID());
        com.velorise.staves.entity.GeoFangPillarEntity construct =
                new com.velorise.staves.entity.GeoFangPillarEntity(
                        com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR.get(),
                        level,
                        constructX, constructY, constructZ,
                        (float) Math.toDegrees(yaw),
                        player,
                        isNetherite
                );
        level.addFreshEntity(construct);
        if (castUnderFeet) {
            construct.liftEntityOntoPillar(player);
        }
        com.velorise.staves.advancement.ModAdvancements.trigger(
                player, com.velorise.staves.advancement.ModAdvancements.EVENT_GEO_FANG_PILLAR);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private boolean castVortexVanquisher(ServerPlayer player) {
        return castVortexVanquisher(player, player.isShiftKeyDown());
    }

    private boolean castVortexVanquisher(ServerPlayer player, boolean holdJadeShield) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 0.001D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        float yaw = (float) Math.atan2(flat.z, flat.x);

        int maxConstructs = com.velorise.staves.entity.GeoFangPillarEntity.MAX_CONSTRUCTS_PER_OWNER;
        int currentConstructs = com.velorise.staves.entity.GeoFangPillarEntity.countActiveConstructsForOwner(
                level, com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR.get(), player.getUUID());

        // Genshin Dominus Lapidis + Default Geo Construct Cap (3):
        // - Press (Tap): always summons a Stone Stele (replacing oldest construct if at 3-construct cap).
        // - Hold (1.5s): forms Jade Shield + AoE Geo shockwave, and summons a Stone Stele if below 3-construct cap!
        boolean shouldSpawnStele = !holdJadeShield || (currentConstructs < maxConstructs);
        boolean castUnderFeet = !holdJadeShield && player.getXRot() >= 50.0F;

        double steleX = player.getX() + flat.x * 2.5D;
        double steleZ = player.getZ() + flat.z * 2.5D;
        double steleY = player.getY();

        if (shouldSpawnStele) {
            net.minecraft.core.Direction attachDir = net.minecraft.core.Direction.UP;
            if (castUnderFeet) {
                steleX = player.getX();
                steleZ = player.getZ();
                steleY = findGroundY(level, steleX, player.getY(), steleZ);
            } else {
                HitResult hit = player.pick(12.0D, 0.0F, false);
                if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
                    BlockPos bp = bhr.getBlockPos();
                    net.minecraft.core.Direction face = bhr.getDirection();
                    if (face.getAxis().isHorizontal()) {
                        attachDir = face;
                        steleX = bp.getX() + 0.5D + face.getStepX() * 0.5D;
                        steleY = bp.getY() + 0.05D;
                        steleZ = bp.getZ() + 0.5D + face.getStepZ() * 0.5D;
                    } else if (!holdJadeShield) {
                        steleX = bp.getX() + 0.5D;
                        steleZ = bp.getZ() + 0.5D;
                        steleY = findGroundY(level, steleX, bp.getY() + 1.0D, steleZ);
                    } else {
                        double tx = player.getX() + flat.x * 2.5D;
                        double tz = player.getZ() + flat.z * 2.5D;
                        steleX = Math.floor(tx) + 0.5D;
                        steleZ = Math.floor(tz) + 0.5D;
                        steleY = findGroundY(level, steleX, player.getY(), steleZ);
                    }
                } else {
                    double tx = player.getX() + flat.x * 2.5D;
                    double tz = player.getZ() + flat.z * 2.5D;
                    steleX = Math.floor(tx) + 0.5D;
                    steleZ = Math.floor(tz) + 0.5D;
                    steleY = findGroundY(level, steleX, player.getY(), steleZ);
                }
            }

            com.velorise.staves.entity.GeoFangPillarEntity.enforceMaxConstructsForOwner(
                    level, com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR.get(), player.getUUID());

            com.velorise.staves.entity.GeoFangPillarEntity stele =
                    new com.velorise.staves.entity.GeoFangPillarEntity(
                            com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR.get(),
                            level,
                            steleX, steleY, steleZ,
                            (float) Math.toDegrees(yaw),
                            player,
                            isNetherite
                    );
            stele.setStoneStele(true);
            stele.setAttachDirection(attachDir);
            level.addFreshEntity(stele);
            if (castUnderFeet) {
                stele.liftEntityOntoPillar(player);
            }
        }

        // Initial Dominus Lapidis Geo shockwave (Basic: 2.0 hearts [4.0 HP], Netherite: 3.0 hearts [6.0 HP] + 1.9% Max HP)
        float maxHp = Math.max(20.0F, player.getMaxHealth());
        float baseDmg = (isNetherite ? 6.0F : 4.0F) + maxHp * 0.019F;
        float gaugeU = isNetherite ? 1.5F : 1.0F;
        Vec3 shockCenter = holdJadeShield ? player.position() : new Vec3(steleX, steleY, steleZ);
        AABB box = new AABB(
                shockCenter.x - 4.0D, shockCenter.y - 1.5D, shockCenter.z - 4.0D,
                shockCenter.x + 4.0D, shockCenter.y + 3.0D, shockCenter.z + 4.0D
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
            ElementalGaugeManager.applyElement(level, target, Element.GEO, gaugeU, player, baseDmg);
        }

        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, holdJadeShield ? 3 : 4);
        if (holdJadeShield) {
            ElementalGaugeManager.activateJadeShield(player, isNetherite);
            // Authentic earthen stone/bedrock formation sounds for Jade Shield
            level.playSound(null, player.blockPosition(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.0F, 0.65F);
            level.playSound(null, player.blockPosition(), SoundEvents.DEEPSLATE_TILES_PLACE, SoundSource.PLAYERS, 1.0F, 0.72F);
            level.playSound(null, player.blockPosition(), SoundEvents.BASALT_PLACE, SoundSource.PLAYERS, 0.95F, 0.60F);
            level.playSound(null, player.blockPosition(), SoundEvents.GILDED_BLACKSTONE_STEP, SoundSource.PLAYERS, 0.9F, 0.85F);
        } else {
            BlockPos soundPos = BlockPos.containing(steleX, steleY, steleZ);
            level.playSound(null, soundPos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 0.95F, 0.70F);
            level.playSound(null, soundPos, SoundEvents.DEEPSLATE_BRICKS_PLACE, SoundSource.PLAYERS, 1.0F, 0.75F);
            level.playSound(null, soundPos, SoundEvents.BASALT_PLACE, SoundSource.PLAYERS, 0.9F, 0.65F);
        }
        com.velorise.staves.advancement.ModAdvancements.trigger(
                player, com.velorise.staves.advancement.ModAdvancements.EVENT_GEO_FANG_PILLAR);
        return true;
    }

    private static double findGroundY(ServerLevel level, double x, double originY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos((int)Math.floor(x), (int)Math.floor(originY) + 2, (int)Math.floor(z));
        for (int i = 0; i < 7; i++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                return below.getY() + 1.0D;
            }
            cursor.move(Direction.DOWN);
        }
        return originY;
    }

    private void fireChanneledLightning(ServerLevel level, ServerPlayer player, int elapsed) {
        double range = isNetherite ? 36.0D : 30.0D;
        double targetInflation = isNetherite ? 2.0D : 1.0D;
        LivingEntity target = findTarget(player, range, targetInflation);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
        Vec3 aimPoint = target != null ? target.getBoundingBox().getCenter() : rayEnd(player, range);

        // Basic tier has a small offset around crosshair/target; Netherite has slightly more wild arcs
        double jitter = isNetherite ? 0.45D : 0.20D;
        double offX = (level.random.nextDouble() - 0.5D) * jitter;
        double offY = (level.random.nextDouble() - 0.5D) * jitter;
        double offZ = (level.random.nextDouble() - 0.5D) * jitter;
        Vec3 strikePos = aimPoint.add(offX, offY, offZ);

        // Draw horizontal primary arc from staff tip to target
        drawJaggedArc(level, start, strikePos, isNetherite ? 0.35D : 0.20D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strikePos.x, strikePos.y, strikePos.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);

        // High-voltage electric crackle sound
        level.playSound(null, strikePos.x, strikePos.y, strikePos.z,
            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.55F + level.random.nextFloat() * 0.45F);
        if (elapsed % 4 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.45F, 1.75F);
        }

        float damagePerTick = isNetherite ? 1.5F : 1.0F;
        float attackU = isNetherite ? 1.5F : 1.0F;
        boolean chargeCreeper = StavesConfig.ARC_CASTER_CHARGE_CREEPER.get();

        LightningBolt sharedBolt = null;

        // If target was found directly, damage primary target
        if (target != null && target.isAlive()) {
            if (target instanceof Creeper creeper && chargeCreeper && !creeper.isPowered()) {
                sharedBolt = EntityType.LIGHTNING_BOLT.create(level);
                if (sharedBolt != null) creeper.thunderHit(level, sharedBolt);
                creeper.clearFire();
            }
            target.clearFire();
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, attackU, player, damagePerTick);
            target.clearFire();
        }

        // Netherite level can chain lightning to up to 3 nearby mobs within 6.0m without needing water
        if (isNetherite) {
            double chainRadius = 6.0D;
            AABB aabb = new AABB(strikePos.x - chainRadius, strikePos.y - 2.5D, strikePos.z - chainRadius,
                                 strikePos.x + chainRadius, strikePos.y + 3.0D, strikePos.z + chainRadius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e.isAlive() && e != player && e != target && !e.isAlliedTo(player));

            int chained = 0;
            for (LivingEntity victim : nearby) {
                if (chained >= 3) break;
                drawJaggedArc(level, strikePos, victim.getBoundingBox().getCenter(), 0.25D);
                chained++;

                if (victim instanceof Creeper creeper && chargeCreeper && !creeper.isPowered()) {
                    if (sharedBolt == null) {
                        sharedBolt = EntityType.LIGHTNING_BOLT.create(level);
                    }
                    if (sharedBolt != null) creeper.thunderHit(level, sharedBolt);
                    creeper.clearFire();
                }
                victim.clearFire();
                victim.invulnerableTime = 0;
                ElementalGaugeManager.applyElement(level, victim, Element.ELECTRO, attackU * 0.75F, player, damagePerTick);
                victim.clearFire();
            }
        }

        double aoeRadius = isNetherite ? 4.5D : 2.0D;
        DendroCoreManager.checkReactionsNear(level, strikePos, aoeRadius, Element.ELECTRO, player);
        EnvironmentalBlockManager.activateTeslaCoilsNear(level, strikePos, 1.5D);
        // Electrify water if strikePos is in water
        BlockPos strikeBlock = BlockPos.containing(strikePos);
        ElectrifiedWaterManager.electrify(level, strikeBlock, aoeRadius, 70, player, damagePerTick);
    }

    private static void drawJaggedArc(ServerLevel level, Vec3 start, Vec3 end, double jitter) {
        com.velorise.staves.element.ElectrifiedWaterManager.drawElectricArc(level, start, end, jitter, true);
    }

    private static Vec3 rayEnd(ServerPlayer player, double range) {
        return rayEnd(player, range, ClipContext.Block.COLLIDER);
    }

    private static Vec3 rayEndOutline(ServerPlayer player, double range) {
        return rayEnd(player, range, ClipContext.Block.OUTLINE);
    }

    private static Vec3 rayEnd(ServerPlayer player, double range, ClipContext.Block blockMode) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
            blockMode, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private void fireGuardian(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        // Self HP drain while channeling Equitable Judgment down to 50% Max HP cap
        if (!player.isCreative() && player.getHealth() > player.getMaxHealth() * 0.50F) {
            float newHp = Math.max(player.getMaxHealth() * 0.50F, player.getHealth() - 0.5F);
            ElementalGaugeManager.drainHealthSilently(player, newHp);
        }
        Vec3 start = player.getEyePosition();
        double range = isNetherite ? 28.0D : 24.0D;
        Vec3 end = target == null ? rayEndOutline(player, range) : target.getBoundingBox().getCenter();
        if (target != null) {
            float draconicMult = ElementalGaugeManager.getDraconicGloriesMultiplier(player);
            boolean hpBuff = ElementalGaugeManager.hasRecentHpChangeBuff(player);
            float totalMult = Math.max(draconicMult, hpBuff ? 1.25F : 1.0F);
            float baseDmg = computeGuardianBaseDamage(player.getMaxHealth(), isNetherite) * totalMult;
            float attackU = isNetherite ? 1.5F : 1.0F;
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.HYDRO, attackU, player, baseDmg);
            if (totalMult > 1.05F) {
                level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }
        EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, start, end, isNetherite ? 2.0D : 1.35D);
        drawBeam(level, start, end, ParticleTypes.ELECTRIC_SPARK, 0.45D);
        level.playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.9F, 1.2F);
    }

    private void fireEcho(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double range = isNetherite ? 32.0D : 28.0D;
        double beamRadius = isNetherite ? 2.5D : 2.0D;
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 1.0D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player)
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .sorted(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .toList();

        float baseDmg = isNetherite ? 16.0F : 11.0F;
        float attackU = isNetherite ? 4.0F : 2.0F;
        for (LivingEntity target : targets) {
            ElementalGaugeManager.applyElement(level, target, Element.ANEMO, attackU, player, baseDmg);
        }
        Vec3 end = start.add(direction.scale(range));
        drawBeam(level, start, end, ParticleTypes.SONIC_BOOM, 1.5D);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void drawBeam(ServerLevel level, Vec3 start, Vec3 end,
                                 net.minecraft.core.particles.SimpleParticleType particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.001D) return;
        Vec3 direction = delta.scale(1.0D / length);
        for (double d = 0.5D; d <= length; d += spacing) {
            Vec3 point = start.add(direction.scale(d));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range, double beamRadius) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 2.0D);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player && player.hasLineOfSight(target))
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .min(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .orElse(null);
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double distanceFromRay(Vec3 start, Vec3 direction, Vec3 point, double range) {
        double projection = projection(start, direction, point);
        if (projection < 0.0D || projection > range) return Double.MAX_VALUE;
        return point.distanceTo(start.add(direction.scale(projection)));
    }

    @Override
    public int getEnchantmentValue() {
        return isNetherite ? 20 : 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (VeloriseStavesMod.isOmniElementItem(repair)) return true;
        Element repairElem = VeloriseStavesMod.elementFromItem(repair);
        if (repairElem != null && repairElem == getElement()) return true;
        if (isNetherite && repair.is(Items.NETHERITE_INGOT)) return true;
        boolean validBase = switch (kind) {
            case DRAGONFIRE, INFERNO, GHASTFIRE, WITHER -> repair.is(Items.BLAZE_ROD);
            case ECHO -> repair.is(Items.ECHO_SHARD) || repair.is(Items.BLAZE_ROD);
            case GUARDIAN, SPLENDOR -> repair.is(Items.PRISMARINE_SHARD) || repair.is(Items.PRISMARINE_CRYSTALS) || repair.is(Items.HEART_OF_THE_SEA) || repair.is(Items.END_ROD);
            case SHULKER -> repair.is(Items.END_ROD) || repair.is(Items.SHULKER_SHELL);
            case WIND -> repair.is(Items.BREEZE_ROD) || repair.is(Items.WIND_CHARGE);
            case LIGHTNING -> repair.is(Items.AMETHYST_SHARD) || repair.is(Items.LIGHTNING_ROD);
            case EVOKER, FROST, WEB -> repair.is(Items.BLAZE_ROD) || repair.is(Items.STICK);
            case VORTEX -> repair.is(Items.GOLD_INGOT) || repair.is(Items.GOLD_BLOCK) || repair.is(Items.BLAZE_ROD);
            case OMNI -> repair.is(Items.AMETHYST_SHARD) || repair.is(Items.DIAMOND) || repair.is(Items.NETHER_STAR);
        };
        return validBase || super.isValidRepairItem(toRepair, repair);
    }

    public @Nullable Element getElement() {
        return switch (kind) {
            case DRAGONFIRE, SHULKER, LIGHTNING -> Element.ELECTRO;
            case ECHO, WIND -> Element.ANEMO;
            case INFERNO, GHASTFIRE -> Element.PYRO;
            case GUARDIAN, SPLENDOR -> Element.HYDRO;
            case EVOKER, VORTEX -> Element.GEO;
            case FROST -> Element.CRYO;
            case WEB, WITHER -> Element.DENDRO;
            case OMNI -> null;
        };
    }

    public String getRangeDisplay() {
        return switch (kind) {
            case DRAGONFIRE -> isNetherite ? "32m (4.5m AoE)" : "32m (3.5m AoE)";
            case ECHO -> isNetherite ? "32m" : "28m";
            case INFERNO -> "30m";
            case GHASTFIRE -> isNetherite ? "36m (4.5m AoE / 5s Field)" : "36m (3.5m AoE / 5s Field)";
            case WITHER -> isNetherite ? "32m (4.0m AoE)" : "32m (3.0m AoE)";
            case SHULKER -> "30m";
            case GUARDIAN -> isNetherite ? "28m" : "24m";
            case SPLENDOR -> isNetherite ? "16m Ousia / 8m Pneuma (30s)" : "16m Ousia / 6m Pneuma (30s)";
            case EVOKER -> isNetherite ? "19.5m (13 Fangs)" : "13.5m (9 Fangs)";
            case VORTEX -> "12m (Max 3 Constructs, 6m Pulse / Jade Shield)";
            case LIGHTNING -> isNetherite ? "36m Arc (6.0m Chain)" : "30m Arc (Single Target)";
            case FROST -> isNetherite ? "28m (2.0m AoE)" : "28m";
            case WEB -> "28m";
            case WIND -> isNetherite ? "28m (4.5m AoE)" : "28m (3.5m AoE)";
            case OMNI -> isNetherite ? "48m (2.5m AoE)" : "36m (1.75m AoE)";
        };
    }

    private static String fmtHearts(double baseDamage) {
        double scaledHearts = (baseDamage * StavesConfig.getDamageMultiplier()) / 2.0D;
        double rounded = Math.round(scaledHearts * 100.0D) / 100.0D;
        if (Math.abs(rounded - Math.round(rounded)) < 0.005D) {
            return String.valueOf(Math.round(rounded));
        }
        double tenths = Math.round(rounded * 10.0D) / 10.0D;
        if (Math.abs(rounded - tenths) < 0.005D) {
            return String.format(java.util.Locale.ROOT, "%.1f", tenths);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", rounded);
    }

    private void switchSplendorArkhe(ServerPlayer sp, ItemStack stack) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        boolean nextPneuma = !isSplendorPneuma(stack);
        setSplendorPneuma(stack, nextPneuma);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(sp, 2);
        String modeKey = nextPneuma
                ? "tooltip.velorise_staves.splendor.mode_pneuma"
                : "tooltip.velorise_staves.splendor.mode_ousia";
        sp.displayClientMessage(
                Component.translatable("message.velorise_staves.splendor_arkhe_switched",
                        Component.translatable(modeKey)).withStyle(ChatFormatting.AQUA),
                true);
        sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.95F, nextPneuma ? 1.35F : 0.95F);
        sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.55F, 1.35F);
        sl.sendParticles(ParticleTypes.SPLASH, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 18, 0.55D, 0.35D, 0.55D, 0.06D);
        sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 10, 0.45D, 0.35D, 0.45D, 0.03D);
        if (com.velorise.staves.entity.HydroSalonSummonEntity.hasAnySummonForOwner(sl, sp.getUUID())) {
            com.velorise.staves.entity.HydroSalonSummonEntity.summonOrSwitchArkhe(sl, sp, isNetherite, nextPneuma);
        }
    }

    private boolean castSplendorSummon(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        boolean pneuma = isSplendorPneuma(stack);
        com.velorise.staves.entity.HydroSalonSummonEntity.summonOrSwitchArkhe(serverLevel, player, isNetherite, pneuma);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 1);
        return true;
    }

    public static float computeGuardianBaseDamage(float maxHp, boolean netherite) {
        float basic = Math.max(0.0F, maxHp) * 0.10F;
        return netherite ? basic * 1.5F : basic;
    }

    private static float getDisplayMaxHp() {
        try {
            if (!maxHpMethodResolved) {
                maxHpMethodResolved = true;
                Class<?> cls = Class.forName("com.velorise.staves.client.ElementalCodexScreen");
                cachedMaxHpMethod = cls.getMethod("getClientPlayerMaxHp");
            }
            java.lang.reflect.Method m = cachedMaxHpMethod;
            if (m != null) {
                Object res = m.invoke(null);
                if (res instanceof Float f && f > 0.0F) {
                    return f;
                }
            }
        } catch (Throwable ignored) {
        }
        return 20.0F;
    }

    public String getDamageDisplay() {
        return getDamageDisplay(getElement());
    }

    public String getDamageDisplay(@Nullable Element overrideElement) {
        Element e = overrideElement;
        String icon = e != null
                ? ChatFormatting.WHITE.toString() + e.iconChar() + ChatFormatting.BLUE + " "
                : (kind == Kind.OMNI ? ChatFormatting.WHITE + Element.omniIconString() + ChatFormatting.BLUE + " " : "");
        String heart = ChatFormatting.WHITE.toString() + "\uE008" + ChatFormatting.BLUE;
        String cloudSuffix = Component.translatable("tooltip.velorise_staves.dmg_cloud").getString();
        String swirlTag = " (" + Component.translatable("reaction.velorise_staves.swirl").getString() + ")";
        String witherTag = " " + Component.translatable(isNetherite ? "tooltip.velorise_staves.wither_ii" : "tooltip.velorise_staves.wither_i").getString();
        String perFang = " " + Component.translatable("tooltip.velorise_staves.per_fang").getString() + " ";
        String perHit = " " + Component.translatable("tooltip.velorise_staves.per_hit").getString() + " ";
        String trapPoison = Component.translatable("tooltip.velorise_staves.trap_poison").getString();
        return switch (kind) {
            case DRAGONFIRE -> (isNetherite ? fmtHearts(10.0) : fmtHearts(7.0)) + heart + " " + cloudSuffix + " " + icon + "2U";
            case ECHO -> (isNetherite ? fmtHearts(16.0) : fmtHearts(11.0)) + heart + " " + icon + (isNetherite ? "4U" : "2U") + swirlTag;
            case INFERNO -> (isNetherite ? fmtHearts(4.5) : fmtHearts(3.0)) + heart + " x 3 " + icon + (isNetherite ? "1.5U" : "1U");
            case GHASTFIRE -> (isNetherite ? fmtHearts(9.0) : fmtHearts(6.0)) + heart + " " + icon + (isNetherite ? "4U" : "2U");
            case WITHER -> (isNetherite ? fmtHearts(6.0) : fmtHearts(4.0)) + heart + " x 3 " + icon + (isNetherite ? "1.5U" : "1U") + witherTag;
            case SHULKER -> (isNetherite ? fmtHearts(6.0) : fmtHearts(4.0)) + heart + " x 2 " + icon + (isNetherite ? "1.5U" : "1U");
            case GUARDIAN -> {
                float basicDmg = computeGuardianBaseDamage(getDisplayMaxHp(), isNetherite);
                yield (fmtHearts(basicDmg) + "-" + fmtHearts(basicDmg * 2.0F)) + heart + " x 8 (3s) " + icon + (isNetherite ? "1.5U" : "1U");
            }
            case SPLENDOR -> {
                float maxHp = getDisplayMaxHp();
                float minHit = maxHp * (isNetherite ? 0.075F : 0.05F);
                float maxHit = maxHp * (isNetherite ? 0.19F : 0.14F) * 1.4F;
                yield (fmtHearts(minHit) + "-" + fmtHearts(maxHit)) + heart + " " + icon + (isNetherite ? "1.5U" : "1U");
            }
            case EVOKER -> (isNetherite ? fmtHearts(4.5) : fmtHearts(3.0)) + heart + perFang + icon + (isNetherite ? "1.5U" : "1U");
            case VORTEX -> {
                float maxHp = getDisplayMaxHp();
                float slam = (isNetherite ? 6.0F : 4.0F) + maxHp * 0.019F;
                float pulse = (isNetherite ? 3.0F : 2.0F) + maxHp * 0.019F;
                yield fmtHearts(slam) + heart + " (+" + fmtHearts(pulse) + heart + "/2s) " + icon + (isNetherite ? "1.5U" : "1U");
            }
            case LIGHTNING -> (isNetherite ? fmtHearts(1.0) : fmtHearts(0.5)) + heart + perHit + icon + (isNetherite ? "1.5U" : "1U");
            case FROST -> (isNetherite ? fmtHearts(4.5) : fmtHearts(3.0)) + heart + " x 3 " + icon + (isNetherite ? "1.5U" : "1U");
            case WEB -> trapPoison + " " + icon + (isNetherite ? "1.5U" : "1U");
            case WIND -> (isNetherite ? fmtHearts(2.5) : fmtHearts(1.5)) + heart + " " + icon + (isNetherite ? "1.5U" : "1U") + swirlTag;
            case OMNI -> (isNetherite ? fmtHearts(15.0) : fmtHearts(10.0)) + heart + " " + icon + (isNetherite ? "3U" : "2U");
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        if (kind == Kind.OMNI) {
            Element active = getOmniActiveElement(stack);
            return Component.translatable(this.getDescriptionId(stack)).withStyle(active != null ? active.color() : ChatFormatting.GOLD);
        }
        Element elem = getElement();
        if (elem != null) {
            return Component.translatable(this.getDescriptionId(stack)).withStyle(elem.color());
        } else if (kind == Kind.WITHER) {
            return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.DARK_GRAY);
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.OMNI) {
            Element active = getOmniActiveElement(stack);
            if (active != null) {
                Component iconComp = Component.literal(active.iconString()).withStyle(ChatFormatting.WHITE);
                String subtitleKey = isNetherite ? "tooltip.velorise_staves.staff_omni.locked.netherite" : "tooltip.velorise_staves.staff_omni.locked";
                Component textComp = Component.literal(" ").append(Component.translatable(subtitleKey, Component.translatable(active.translationKey()))).withStyle(active.color());
                tooltip.add(Component.empty().append(iconComp).append(textComp));
            } else {
                Component iconComp = Component.literal(Element.omniIconString()).withStyle(ChatFormatting.WHITE);
                String subtitleKey = isNetherite ? "tooltip.velorise_staves.staff_omni.netherite" : "tooltip.velorise_staves.staff_omni";
                Component textComp = Component.literal(" ").append(Component.translatable(subtitleKey)).withStyle(ChatFormatting.GOLD);
                tooltip.add(Component.empty().append(iconComp).append(textComp));
            }
        } else {
            Element elem = getElement();
            if (elem != null) {
                Component iconComp = Component.literal(elem.iconString()).withStyle(ChatFormatting.WHITE);
                String subtitleKey = isNetherite ? "tooltip.velorise_staves.staff_element.netherite" : "tooltip.velorise_staves.staff_element";
                Component textComp = Component.literal(" ").append(Component.translatable(subtitleKey, Component.translatable(elem.translationKey()))).withStyle(elem.color());
                tooltip.add(Component.empty().append(iconComp).append(textComp));
            } else if (kind == Kind.WITHER) {
                String subtitleKey = isNetherite ? "tooltip.velorise_staves.staff_wither.netherite" : "tooltip.velorise_staves.staff_wither";
                tooltip.add(Component.literal("☠ ").append(Component.translatable(subtitleKey)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.velorise_staves.when_used").withStyle(ChatFormatting.GRAY));
        Element dispElem = kind == Kind.OMNI ? getOmniActiveElement(stack) : getElement();
        tooltip.add(Component.literal("  " + getDamageDisplay(dispElem)).withStyle(ChatFormatting.BLUE));
        if (kind == Kind.GUARDIAN) {
            tooltip.add(Component.translatable("tooltip.velorise_staves.guardian_hp_scale").withStyle(ChatFormatting.AQUA));
        } else if (kind == Kind.SPLENDOR) {
            String modeKey = isSplendorPneuma(stack)
                    ? "tooltip.velorise_staves.splendor.mode_pneuma"
                    : "tooltip.velorise_staves.splendor.mode_ousia";
            tooltip.add(Component.translatable("tooltip.velorise_staves.splendor_arkhe", Component.translatable(modeKey)).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.velorise_staves.splendor_hp_scale").withStyle(ChatFormatting.AQUA));
        } else if (kind == Kind.VORTEX) {
            tooltip.add(Component.translatable("tooltip.velorise_staves.vortex_jade_shield").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.velorise_staves.vortex_res_shred").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.velorise_staves.range", getRangeDisplay()).withStyle(ChatFormatting.BLUE));
        if (kind == Kind.VORTEX) {
            float tapSec = ElementalResonanceManager.getClientAdjustedCooldownTicks(getVortexCooldownTicks(false)) / 20.0F;
            float holdSec = ElementalResonanceManager.getClientAdjustedCooldownTicks(getVortexCooldownTicks(true)) / 20.0F;
            String cdStr = String.format(java.util.Locale.ROOT, "%.1fs (Tap) / %.1fs (Hold)", tapSec, holdSec);
            tooltip.add(Component.translatable("tooltip.velorise_staves.cooldown_custom", cdStr).withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component.translatable("tooltip.velorise_staves.cooldown", String.format(java.util.Locale.ROOT, "%.1f", ElementalResonanceManager.getClientAdjustedCooldownTicks(getCooldownTicks()) / 20.0F)).withStyle(ChatFormatting.BLUE));
        }
        int emLvl = ElementalGaugeManager.getElementalMasteryLevel(stack);
        if (emLvl > 0) {
            appendWrappedTooltip(tooltip,
                    Component.translatable("tooltip.velorise_staves.elemental_mastery_bonus", emLvl * 15, emLvl * 30, emLvl * 25),
                    ChatFormatting.GOLD, 44, "    ");
        }
        if (kind == Kind.OMNI) {
            tooltip.add(Component.translatable("tooltip.velorise_staves.omni_wheel_hint").withStyle(ChatFormatting.YELLOW));
        }

        if (flag.isAdvanced()) {
            tooltip.add(Component.empty());
            appendWrappedTooltip(tooltip, Component.translatable(getDescriptionId() + ".desc"), ChatFormatting.DARK_GRAY);
        }
    }

    public static void appendWrappedTooltip(List<Component> tooltip, Component component, ChatFormatting color) {
        appendWrappedTooltip(tooltip, component, color, 44, "");
    }

    public static void appendWrappedTooltip(List<Component> tooltip, Component component, ChatFormatting color, int maxUnits, String contIndent) {
        String raw = component.getString();
        if (raw == null || raw.isEmpty()) return;
        for (String paragraph : raw.split("\n")) {
            if (paragraph.isEmpty()) {
                tooltip.add(Component.empty());
                continue;
            }
            StringBuilder line = new StringBuilder();
            double lineUnits = 0.0D;
            boolean firstLine = true;
            int i = 0;
            int n = paragraph.length();
            while (i < n) {
                char c = paragraph.charAt(i);
                if (c == ' ' && line.isEmpty() && firstLine) {
                    line.append(c);
                    lineUnits += 0.7D;
                    i++;
                    continue;
                }
                if (c == ' ' && line.isEmpty()) {
                    i++;
                    continue;
                }
                if (isWideTooltipChar(c)) {
                    double w = 1.85D;
                    if (lineUnits > contIndent.length() * 0.7D + 0.01D && lineUnits + w > maxUnits) {
                        tooltip.add(Component.literal(line.toString()).withStyle(color));
                        line.setLength(0);
                        line.append(contIndent);
                        lineUnits = contIndent.length() * 0.7D;
                        firstLine = false;
                    }
                    line.append(c);
                    lineUnits += w;
                    i++;
                    continue;
                }
                int end = i;
                double wordUnits = 0.0D;
                while (end < n) {
                    char wc = paragraph.charAt(end);
                    if (wc == ' ' || isWideTooltipChar(wc)) break;
                    if (wc == '\u00A7' && end + 1 < n) {
                        end += 2;
                        continue;
                    }
                    wordUnits += charTooltipUnits(wc);
                    end++;
                }
                String word = paragraph.substring(i, end);
                boolean hasSpaceBefore = !line.isEmpty() && line.charAt(line.length() - 1) != ' ';
                double added = (hasSpaceBefore ? 0.7D : 0.0D) + wordUnits;
                if (lineUnits > contIndent.length() * 0.7D + 0.01D && lineUnits + added > maxUnits) {
                    tooltip.add(Component.literal(line.toString()).withStyle(color));
                    line.setLength(0);
                    line.append(contIndent);
                    lineUnits = contIndent.length() * 0.7D;
                    firstLine = false;
                } else if (hasSpaceBefore) {
                    line.append(' ');
                    lineUnits += 0.7D;
                }
                line.append(word);
                lineUnits += wordUnits;
                i = end;
                while (i < n && paragraph.charAt(i) == ' ') {
                    i++;
                }
            }
            if (!line.isEmpty()) {
                tooltip.add(Component.literal(line.toString()).withStyle(color));
            }
        }
    }

    private static boolean isWideTooltipChar(char c) {
        return (c >= '\u2E80' && c <= '\u9FFF')
                || (c >= '\uAC00' && c <= '\uD7AF')
                || (c >= '\uF900' && c <= '\uFAFF')
                || (c >= '\uFF00' && c <= '\uFFEF');
    }

    private static double charTooltipUnits(char c) {
        if (c == 'i' || c == 'l' || c == '.' || c == ',' || c == ':' || c == ';' || c == '!' || c == '\'') {
            return 0.55D;
        }
        return Character.isUpperCase(c) ? 1.08D : 0.95D;
    }
}
