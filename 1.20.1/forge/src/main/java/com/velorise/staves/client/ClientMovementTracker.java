package com.velorise.staves.client;

import static com.velorise.staves.client.ClientAuraTracker.*;
import org.jetbrains.annotations.Nullable;

/**
 * Manages client-side stamina, sprint-dash impulses, and Steve wall-climbing
 * state machine and mantle kinematics.
 */
public final class ClientMovementTracker {

    private ClientMovementTracker() {}

    private static boolean wasSprintKeyDownForDash = false;
    private static int localDashCooldownTicks = 0;
    private static int localDashChainCount = 0;
    private static int localDashChainResetTicks = 0;
    private static boolean localSteveWallClimbing = false;
    private static double localWallVelX = 0.0D;
    private static double localWallVelY = 0.0D;
    private static double localWallVelZ = 0.0D;
    private static double localMantleStartX = 0.0D;
    private static double localMantleStartY = 0.0D;
    private static double localMantleStartZ = 0.0D;
    private static double localMantleDestX = 0.0D;
    private static double localMantleDestY = Double.NaN;
    private static double localMantleDestZ = 0.0D;
    private static int localMantleElapsedTicks = 0;
    private static final int LOCAL_MANTLE_DURATION_TICKS = 10;
    private static com.velorise.staves.fsm.MovementActionState localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
    private static int localMoveElapsedTicks = 0;
    private static int localWallJumpTicks = 0;
    private static int localMantleTicks = 0;
    private static int localWallLostGraceTicks = 0;
    private static net.minecraft.core.Direction localWallNormalFace = null;
    private static boolean wasWallJumpKeyDown = false;
    private static boolean localExhaustedClimbFall = false;
    private static final com.velorise.staves.fsm.InputBuffer<com.velorise.staves.fsm.MovementActionState.BufferedInput> localWallInputBuffer = new com.velorise.staves.fsm.InputBuffer<>();

    public static void resetLocalStaminaState() {
        localStamina = localMaxStamina;
        localStaminaRegenDelayTicks = 0;
        localStaminaExhausted = false;
        localStaminaFullHideTicks = 0;
        wasSprintKeyDownForDash = false;
        localDashCooldownTicks = 0;
        localDashChainCount = 0;
        localDashChainResetTicks = 0;
        localSteveWallClimbing = false;
        localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
        localMoveElapsedTicks = 0;
        localWallJumpTicks = 0;
        localMantleTicks = 0;
        localWallLostGraceTicks = 0;
        localWallNormalFace = null;
        localWallVelX = 0.0D;
        localWallVelY = 0.0D;
        localWallVelZ = 0.0D;
        localMantleDestY = Double.NaN;
        localMantleElapsedTicks = 0;
        wasWallJumpKeyDown = false;
        localExhaustedClimbFall = false;
        localWallInputBuffer.clear();
    }

    public static void tickLocalStaminaAndDash(
            net.minecraft.world.entity.player.Player player,
            boolean sprintKeyDown,
            byte toggleGliderAction,
            byte dashAction
    ) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            wasSprintKeyDownForDash = false;
            localDashChainCount = 0;
            localDashChainResetTicks = 0;
            if (localSteveWallClimbing) {
                localSteveWallClimbing = false;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 30);
            }
            return;
        }
        if (localDashCooldownTicks > 0) {
            localDashCooldownTicks--;
        }
        if (localDashChainResetTicks > 0) {
            localDashChainResetTicks--;
            if (localDashChainResetTicks == 0) {
                localDashChainCount = 0;
            }
        }

        boolean customStamina = com.velorise.staves.config.StavesConfig.isEnableCustomStamina();
        boolean customDash = com.velorise.staves.config.StavesConfig.isEnableCustomDash();
        boolean creative = player.isCreative();
        int pid = player.getId();

        if (!customStamina) {
            localStamina = localMaxStamina;
            localStaminaExhausted = false;
            localStaminaRegenDelayTicks = 0;
            localStaminaFullHideTicks = 0;
        }

        boolean blocked = isUltiCastActive(pid)
                || isFrozen(pid)
                || player.getVehicle() != null
                || isPlunging(pid)
                || isGliding(pid)
                || isXilonenSkating(pid)
                || isNeuvilletteChargingOrFiring(pid)
                || player.getAbilities().flying
                || player.isUsingItem();

        if (customDash && sprintKeyDown && !wasSprintKeyDownForDash && !blocked && localDashCooldownTicks <= 0
                && (player.onGround() || player.isInWater())) {
            boolean enoughStamina = !customStamina || creative || (!localStaminaExhausted && localStamina >= 18.0F);
            if (enoughStamina) {
                if (customStamina && !creative) {
                    localStamina = Math.max(0.0F, localStamina - 18.0F);
                    localStaminaRegenDelayTicks = 30;
                    if (localStamina <= 0.01F) {
                        localStamina = 0.0F;
                        localStaminaExhausted = true;
                    }
                }
                localDashChainCount++;
                if (localDashChainCount >= 2) {
                    localDashChainCount = 0;
                    localDashChainResetTicks = 0;
                    localDashCooldownTicks = 16;
                } else {
                    localDashChainResetTicks = 28;
                    localDashCooldownTicks = 3;
                }
                triggerStaffAnimation(pid, ANIM_DASH_IMPULSE);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(dashAction);

                float fwdIn = ClientAccess.getForwardImpulse(player);
                float leftIn = ClientAccess.getLeftImpulse(player);
                double yawRad = Math.toRadians(player.getYRot());
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                double leftX = Math.cos(yawRad);
                double leftZ = Math.sin(yawRad);
                double dirX = fwdX * fwdIn + leftX * leftIn;
                double dirZ = fwdZ * fwdIn + leftZ * leftIn;
                double len = Math.hypot(dirX, dirZ);
                if (len < 1.0E-4D) {
                    dirX = fwdX;
                    dirZ = fwdZ;
                } else {
                    dirX /= len;
                    dirZ /= len;
                }
                double dashSpeed = player.isInWater() ? 0.44D : 0.72D;
                net.minecraft.world.phys.Vec3 curVel = player.getDeltaMovement();
                player.setDeltaMovement(dirX * dashSpeed, Math.max(curVel.y, 0.0D), dirZ * dashSpeed);
                if (!customStamina || creative || !localStaminaExhausted) {
                    player.setSprinting(true);
                }
            }
        }
        wasSprintKeyDownForDash = sprintKeyDown;

        if (customDash && sprintKeyDown && !blocked && (!customStamina || creative || !localStaminaExhausted)
                && ClientAccess.getForwardImpulse(player) > 0.01F && !player.isShiftKeyDown()) {
            player.setSprinting(true);
        }

        tickLocalWallClimbMachine(player, customStamina, creative, blocked);

        if (customStamina && !creative) {
            boolean glidingNow = isGliding(pid) && !player.onGround() && !player.getAbilities().flying && player.getVehicle() == null;
            boolean wallClimbingNow = localSteveWallClimbing || localMovementState.isWallState();
            boolean sprintingNow = !glidingNow && !wallClimbingNow && player.isSprinting() && !player.getAbilities().flying
                    && player.getVehicle() == null && !isXilonenSkating(pid) && !blocked;
            if (glidingNow) {
                float drain = com.velorise.staves.element.VentiManager.hasVentiWeapon(player) ? 0.15F : 0.30F;
                localStamina = Math.max(0.0F, localStamina - drain);
                localStaminaRegenDelayTicks = 25;
                if (localStamina <= 0.0F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    stopGliding(pid);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(toggleGliderAction);
                }
            } else if (wallClimbingNow) {
                localStaminaRegenDelayTicks = 25;
            } else if (sprintingNow) {
                localStamina = Math.max(0.0F, localStamina - 0.45F);
                localStaminaRegenDelayTicks = 25;
                if (localStamina <= 0.0F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    player.setSprinting(false);
                }
            } else {
                if (localStaminaRegenDelayTicks > 0) {
                    localStaminaRegenDelayTicks--;
                } else if (localStamina < localMaxStamina) {
                    localStamina = Math.min(localMaxStamina, localStamina + 1.25F);
                }
                if (localStaminaExhausted && localStamina >= 20.0F) {
                    localStaminaExhausted = false;
                }
            }
            if (localStaminaExhausted) {
                player.setSprinting(false);
            }
            if (localStamina < localMaxStamina - 0.01F) {
                localStaminaFullHideTicks = 35;
            } else if (localStaminaFullHideTicks > 0) {
                localStaminaFullHideTicks--;
            }
        }
    }

    public static boolean isSteveWallClimbing(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.steveWallClimbing;
    }

    public static boolean isLocalWallClimbing() {
        return localSteveWallClimbing || localMovementState.isWallState();
    }

    /**
     * Runs in ClientTick Pre (before LocalPlayer.aiStep / LivingEntity.travel) so wall-climb
     * velocity, noGravity, and body yaw are applied BEFORE vanilla movement integrates them,
     * eliminating 20Hz tug-of-war jitter and body-rotation snapping.
     */
    public static void prepareLocalWallClimbPreTick() {
        net.minecraft.world.entity.player.Player player = ClientAccess.getLocalPlayer();
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (!localSteveWallClimbing && !localMovementState.isWallState()) return;
        if (player.getAbilities().flying || player.getVehicle() != null) return;
        player.setSprinting(false);
        player.fallDistance = 0.0F;
    }

    /**
     * ParCool!-style enforced movement displacement queried directly from LocalPlayerMixin.move()
     * (MoverType.SELF). Returning a non-null Vec3 cancels vanilla gravity/drag displacement for
     * this tick and applies our exact wall-climb or 2-phase cubic-eased ClimbUp trajectory.
     */
    public static @Nullable net.minecraft.world.phys.Vec3 getEnforcedWallClimbMove(net.minecraft.client.player.LocalPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        if (player.getAbilities().flying || player.getVehicle() != null) return null;

        if (localMovementState == com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE
                && !Double.isNaN(localMantleDestY)) {
            double t = net.minecraft.util.Mth.clamp(
                    (localMantleElapsedTicks + 1.0D) / (double) LOCAL_MANTLE_DURATION_TICKS,
                    0.0D,
                    1.0D
            );
            // ParCool ClimbUp cubic easeInOut with 2-phase trajectory:
            // Phase 1 (0..0.58): lift straight UP past block lip
            // Phase 2 (0.58..1.0): glide cleanly onto top surface with +0.0625 clearance
            double phase = (t < 0.5D)
                    ? (4.0D * t * t * t)
                    : (1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) * 0.5D);
            double borderPhase = 0.58D;
            double targetX;
            double targetY;
            double targetZ;
            if (phase < borderPhase) {
                double p1 = phase / borderPhase;
                targetX = localMantleStartX;
                targetY = net.minecraft.util.Mth.lerp(p1, localMantleStartY, localMantleDestY + 0.0625D);
                targetZ = localMantleStartZ;
            } else {
                double p2 = (phase - borderPhase) / (1.0D - borderPhase);
                targetX = net.minecraft.util.Mth.lerp(p2, localMantleStartX, localMantleDestX);
                targetY = localMantleDestY + 0.0625D;
                targetZ = net.minecraft.util.Mth.lerp(p2, localMantleStartZ, localMantleDestZ);
            }
            return new net.minecraft.world.phys.Vec3(
                    targetX - player.getX(),
                    targetY - player.getY(),
                    targetZ - player.getZ()
            );
        }

        if ((localMovementState == com.velorise.staves.fsm.MovementActionState.WALL_CLING
                || localMovementState == com.velorise.staves.fsm.MovementActionState.WALL_CLIMB)
                && localWallLostGraceTicks == 0) {
            return new net.minecraft.world.phys.Vec3(localWallVelX, localWallVelY, localWallVelZ);
        }
        return null;
    }

    public static boolean isLocalExhaustedClimbFall() {
        return localExhaustedClimbFall;
    }

    public static net.minecraft.core.Direction getWallNormalFace(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null ? data.wallNormalFace : null;
    }

    private static void exitLocalWallClimb(net.minecraft.world.entity.player.Player player, boolean exhausted) {
        int pid = player.getId();
        boolean wasClimbing = localSteveWallClimbing || localMovementState.isWallState();
        localSteveWallClimbing = false;
        localWallNormalFace = null;
        localWallVelX = 0.0D;
        localWallVelY = 0.0D;
        localWallVelZ = 0.0D;
        localMoveElapsedTicks = 0;
        localWallJumpTicks = 0;
        localMantleTicks = 0;
        localMantleDestY = Double.NaN;
        localMantleElapsedTicks = 0;
        localWallInputBuffer.clear();
        AuraDisplayData entry = DATA.get(pid);
        if (entry != null) {
            entry.steveWallClimbing = false;
            entry.wallNormalFace = null;
        }
        if (exhausted) {
            localMovementState = com.velorise.staves.fsm.MovementActionState.EXHAUSTED_FALL;
            localExhaustedClimbFall = true;
            if (wasClimbing) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 32);
            }
        } else {
            localMovementState = player.onGround()
                    ? com.velorise.staves.fsm.MovementActionState.GROUNDED
                    : com.velorise.staves.fsm.MovementActionState.GROUNDED;
            if (wasClimbing) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 30);
            }
        }
    }

    private static void tickLocalWallClimbMachine(
            net.minecraft.world.entity.player.Player player,
            boolean customStamina,
            boolean creative,
            boolean blockedByOtherLayer
    ) {
        int pid = player.getId();
        localWallInputBuffer.tick();

        // Step previous-tick animation snapshot for smooth partialTick interpolation
        AuraDisplayData animEntry = DATA.get(pid);
        if (animEntry != null) {
            animEntry.prevWallClimbFade = animEntry.steveWallClimbFade;
            animEntry.prevWallClimbDist = animEntry.steveWallClimbDist;
            animEntry.prevWallMoveX = animEntry.steveWallMoveX;
            animEntry.prevWallMoveY = animEntry.steveWallMoveY;
            if (localSteveWallClimbing || localMovementState.isWallState()) {
                animEntry.steveWallClimbFade = Math.min(1.0F, animEntry.steveWallClimbFade + 0.35F);
            } else {
                animEntry.steveWallClimbFade = Math.max(0.0F, animEntry.steveWallClimbFade - 0.25F);
                animEntry.steveWallMoveX *= 0.5F;
                animEntry.steveWallMoveY *= 0.5F;
            }
        }

        if (player.onGround() || player.isInWater() || player.isInLava()) {
            localExhaustedClimbFall = false;
            if (localMovementState == com.velorise.staves.fsm.MovementActionState.EXHAUSTED_FALL) {
                localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
            }
        }

        boolean wallClimbEnabled = com.velorise.staves.config.StavesConfig.isEnableWallClimb();
        boolean higherPriorityActive = blockedByOtherLayer
                || !wallClimbEnabled
                || player.onClimbable()
                || player.isInWater()
                || player.isInLava()
                || player.getAbilities().flying
                || player.getVehicle() != null
                || isXilonenSkating(pid)
                || isGliding(pid)
                || isPlunging(pid);

        if (higherPriorityActive) {
            if (localSteveWallClimbing || localMovementState.isWallState()) {
                exitLocalWallClimb(player, false);
            }
            wasWallJumpKeyDown = false;
            return;
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean jumpDown = mc != null && mc.options != null && mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasWallJumpKeyDown;
        wasWallJumpKeyDown = jumpDown;

        float fwdIn = 0.0F;
        float leftIn = 0.0F;
        boolean sneakDown = player.isShiftKeyDown();
        if (mc != null && mc.options != null && mc.screen == null) {
            fwdIn = (mc.options.keyUp.isDown() ? 1.0F : 0.0F) - (mc.options.keyDown.isDown() ? 1.0F : 0.0F);
            leftIn = (mc.options.keyLeft.isDown() ? 1.0F : 0.0F) - (mc.options.keyRight.isDown() ? 1.0F : 0.0F);
            sneakDown = sneakDown || mc.options.keyShift.isDown();
        } else {
            fwdIn = ClientAccess.getForwardImpulse(player);
            leftIn = ClientAccess.getLeftImpulse(player);
        }

        if (localSteveWallClimbing && jumpPressed) {
            localWallInputBuffer.push(com.velorise.staves.fsm.MovementActionState.BufferedInput.CLIMB_JUMP, 4);
        }

        // Instant Shift drop-off works in ALL wall states (never traps the player)
        if ((localSteveWallClimbing || localMovementState.isWallState()) && sneakDown) {
            exitLocalWallClimb(player, false);
            return;
        }

        // Active ParCool! ClimbUp 2-phase cubic-eased ledge mantle (driven by LocalPlayerMixin.move)
        if (localMovementState == com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE) {
            localMoveElapsedTicks++;
            localMantleElapsedTicks++;
            player.fallDistance = 0.0F;
            if (Double.isNaN(localMantleDestY) || localMantleElapsedTicks >= LOCAL_MANTLE_DURATION_TICKS) {
                exitLocalWallClimb(player, false);
                player.setDeltaMovement(0.0D, -0.04D, 0.0D);
            }
            return;
        }

        // Active climb jump (8 ticks, cancel window at tick 5)
        if (localMovementState == com.velorise.staves.fsm.MovementActionState.WALL_JUMP) {
            localMoveElapsedTicks++;
            localWallJumpTicks--;
            player.fallDistance = 0.0F;
            if (localWallJumpTicks <= 0) {
                net.minecraft.core.Direction regrab = com.velorise.staves.element.WallClimbHelper.findClimbWall(
                        player, com.velorise.staves.element.WallClimbHelper.CLIENT_WALL_PROBE_DIST + 0.15D);
                if (regrab != null && (!customStamina || creative || localStamina > 0.01F)) {
                    localWallNormalFace = regrab;
                    localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_CLING;
                    localMoveElapsedTicks = 0;
                    AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
                    entry.wallNormalFace = regrab;
                } else {
                    exitLocalWallClimb(player, customStamina && !creative && localStamina <= 0.01F);
                }
            }
            return;
        }

        // Manual drop-off via Shift
        if (localSteveWallClimbing && sneakDown) {
            exitLocalWallClimb(player, false);
            return;
        }

        net.minecraft.core.Direction wallFace = com.velorise.staves.element.WallClimbHelper.findClimbWall(
                player, com.velorise.staves.element.WallClimbHelper.CLIENT_WALL_PROBE_DIST, localWallNormalFace);

        if (!localSteveWallClimbing) {
            boolean enoughToStart = !customStamina || creative || (!localStaminaExhausted && !localExhaustedClimbFall && localStamina >= 5.0F);
            boolean wantsAttach = wallFace != null && !sneakDown && enoughToStart
                    && fwdIn > 0.10F;
            if (wantsAttach) {
                localSteveWallClimbing = true;
                localWallNormalFace = wallFace;
                localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_CLING;
                localMoveElapsedTicks = 0;
                AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
                entry.steveWallClimbing = true;
                entry.wallNormalFace = wallFace;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 29);
            } else {
                return;
            }
        }

        // Already climbing: validate wall contact with 2-tick seam hysteresis
        if (wallFace == null) {
            localWallLostGraceTicks++;
            if (localWallLostGraceTicks > 2 || localWallNormalFace == null) {
                exitLocalWallClimb(player, false);
                return;
            }
            wallFace = localWallNormalFace;
        } else {
            localWallLostGraceTicks = 0;
            localWallNormalFace = wallFace;
        }
        AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
        entry.steveWallClimbing = true;
        entry.wallNormalFace = wallFace;
        localMoveElapsedTicks++;

        // ParCool! ClimbUp enforced 2-phase cubic-eased trajectory onto the ledge top:
        if (fwdIn > 0.10F) {
            double ledgeTopY = com.velorise.staves.element.WallClimbHelper.findLedgeTopY(player, wallFace);
            if (!Double.isNaN(ledgeTopY)) {
                net.minecraft.core.Direction inward = wallFace.getOpposite();
                localMantleStartX = player.getX();
                localMantleStartY = player.getY();
                localMantleStartZ = player.getZ();
                localMantleDestX = player.getX() + inward.getStepX() * 0.72D;
                localMantleDestY = ledgeTopY;
                localMantleDestZ = player.getZ() + inward.getStepZ() * 0.72D;
                localMantleElapsedTicks = 0;
                localMovementState = com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE;
                localMoveElapsedTicks = 0;
                long nowMs = System.currentTimeMillis();
                entry.steveWallMantleStartMs = nowMs;
                entry.steveWallMantleUntilMs = nowMs + (LOCAL_MANTLE_DURATION_TICKS * 50L);
                player.fallDistance = 0.0F;
                return;
            }
        }

        // Consume buffered Climb Jump (Space = -25 Stamina)
        if (localWallInputBuffer.consumeIf(i -> i == com.velorise.staves.fsm.MovementActionState.BufferedInput.CLIMB_JUMP) != null) {
            boolean canJump = !customStamina || creative || localStamina >= 25.0F;
            if (canJump) {
                if (customStamina && !creative) {
                    localStamina = Math.max(0.0F, localStamina - 25.0F);
                    localStaminaRegenDelayTicks = 30;
                }
                if (fwdIn < -0.20F) {
                    // Authentic Genshin: downward + jump kicks away from wall
                    player.setDeltaMovement(wallFace.getStepX() * 0.42D, 0.38D, wallFace.getStepZ() * 0.42D);
                    exitLocalWallClimb(player, false);
                    return;
                }
                // Upward / diagonal climb leap along the wall
                double rightX = wallFace.getStepZ();
                double rightZ = -wallFace.getStepX();
                net.minecraft.core.Direction inward = wallFace.getOpposite();
                double strafe = -leftIn * 0.22D;
                player.setDeltaMovement(
                        inward.getStepX() * 0.08D + rightX * strafe,
                        0.46D,
                        inward.getStepZ() * 0.08D + rightZ * strafe
                );
                player.fallDistance = 0.0F;
                localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_JUMP;
                localMoveElapsedTicks = 0;
                localWallJumpTicks = com.velorise.staves.fsm.MovementActionState.WALL_JUMP_DURATION_TICKS;
                entry.steveWallJumpUntilMs = System.currentTimeMillis() + 420L;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 31);
                if (customStamina && !creative && localStamina <= 0.01F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    exitLocalWallClimb(player, true);
                }
                return;
            }
        }

        // Compute tangent-plane climbing velocity (W/S = up/down, A/D = strafe along wall)
        boolean movingOnWall = Math.abs(fwdIn) > 0.05F || Math.abs(leftIn) > 0.05F;
        // Advance tick-driven fade, climb cycle phase, and smoothed movement vector (zero setupAnim mutation)
        entry.steveWallClimbFade = Math.min(1.0F, entry.steveWallClimbFade + 0.28F);
        entry.steveWallMoveY = net.minecraft.util.Mth.lerp(0.45F, entry.steveWallMoveY, fwdIn);
        entry.steveWallMoveX = net.minecraft.util.Mth.lerp(0.45F, entry.steveWallMoveX, -leftIn);
        if (movingOnWall) {
            float dirSign = (fwdIn < -0.05F) ? -1.0F : 1.0F;
            entry.steveWallClimbDist += 0.42F * dirSign; // 8.4 rad/s at 20 TPS
        }
        localMovementState = movingOnWall
                ? com.velorise.staves.fsm.MovementActionState.WALL_CLIMB
                : com.velorise.staves.fsm.MovementActionState.WALL_CLING;

        net.minecraft.core.Direction inward = wallFace.getOpposite();
        double rightX = wallFace.getStepZ();
        double rightZ = -wallFace.getStepX();

        double vy = fwdIn * 0.165D;
        if (vy > 0.0D && com.velorise.staves.element.WallClimbHelper.hasOverhangAbove(player)) {
            vy = 0.0D;
        }
        double strafe = -leftIn * 0.135D;
        double vx;
        double vz;
        if (!movingOnWall) {
            // ParCool HangOn seating: gentle inward press keeps contact flush with zero vertical drift
            vx = inward.getStepX() * 0.04D;
            vy = 0.0D;
            vz = inward.getStepZ() * 0.04D;
        } else {
            double inwardBias = 0.06D;
            vx = inward.getStepX() * inwardBias + rightX * strafe;
            vz = inward.getStepZ() * inwardBias + rightZ * strafe;
        }

        localWallVelX = vx;
        localWallVelY = vy;
        localWallVelZ = vz;

        player.setSprinting(false);
        player.setDeltaMovement(vx, vy, vz);
        player.fallDistance = 0.0F;

        // Lightweight keepalive every 10 ticks so server never drops noGravity/anti-kick on 1-tick lag
        if (localMoveElapsedTicks % 10 == 0) {
            com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 29);
        }

        // If descending onto solid ground, exit cleanly
        if (fwdIn < -0.10F && player.onGround()) {
            exitLocalWallClimb(player, false);
            return;
        }

        // Authentic Genshin stamina rule: clinging idle costs 0 but pauses regen; moving drains stamina
        if (customStamina && !creative) {
            localStaminaRegenDelayTicks = 25;
            if (movingOnWall) {
                float drain = 0.45F; // ~9 Stamina/s
                localStamina = Math.max(0.0F, localStamina - drain);
                if (localStamina <= 0.01F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    exitLocalWallClimb(player, true);
                }
            }
        }
    }
}
