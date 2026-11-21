package com.velorise.staves.client;

import com.velorise.staves.entity.FlamestriderBikeEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * Raw Hardware Genshin Input Wrapper shared across NeoForge 1.21.1, Fabric 1.21.1, and Forge 1.20.1.
 *
 * Completely decouples mod combat inputs (LMB = Normal/Charged Attack, RMB = Elemental Skill / Bike Dash,
 * Space = Jump, Sprint = Dash) from Minecraft's block-mining auto-repeat (continueAttack /
 * InteractionKeyMappingTriggered).
 *
 * Physical presses are recorded strictly from raw GLFW_PRESS events (via MouseButton.Pre on NeoForge/Forge
 * and MouseHandlerMixin on Fabric), with clean rising/falling edge detection per client tick.
 */
public final class GenshinInputState {

    public static final class ButtonChannel {
        private volatile boolean rawDown = false;
        private int pendingRawPresses = 0;

        private boolean isDown = false;
        private boolean prevDown = false;
        private boolean justPressed = false;
        private boolean justReleased = false;
        private int holdTicks = 0;
        private int physicalPresses = 0;

        void onRawButton(int action) {
            if (action == GLFW.GLFW_PRESS) {
                rawDown = true;
                pendingRawPresses++;
            } else if (action == GLFW.GLFW_RELEASE) {
                rawDown = false;
            }
        }

        void sample(boolean fallbackKeyDown) {
            prevDown = isDown;
            isDown = rawDown || fallbackKeyDown;
            justPressed = isDown && !prevDown;
            justReleased = !isDown && prevDown;
            if (isDown) {
                holdTicks++;
            } else {
                holdTicks = 0;
            }
            int raw = pendingRawPresses;
            pendingRawPresses = 0;
            // Pure physical press count: either raw GLFW_PRESS events (works even for <50ms double-taps)
            // or a keyboard remap rising edge. NEVER inflated by Minecraft continueAttack hold ticks.
            physicalPresses = Math.max(raw, justPressed ? 1 : 0);
        }

        void reset() {
            rawDown = false;
            pendingRawPresses = 0;
            isDown = false;
            prevDown = false;
            justPressed = false;
            justReleased = false;
            holdTicks = 0;
            physicalPresses = 0;
        }

        public boolean isDown() {
            return isDown;
        }

        public boolean justPressed() {
            return justPressed;
        }

        public boolean justReleased() {
            return justReleased;
        }

        public int holdTicks() {
            return holdTicks;
        }

        public int physicalPresses() {
            return physicalPresses;
        }
    }

    private static final ButtonChannel ATTACK = new ButtonChannel();
    private static final ButtonChannel SKILL = new ButtonChannel();
    private static final ButtonChannel JUMP = new ButtonChannel();
    private static final ButtonChannel SPRINT = new ButtonChannel();

    private GenshinInputState() {}

    public static ButtonChannel attack() {
        return ATTACK;
    }

    public static ButtonChannel skill() {
        return SKILL;
    }

    public static ButtonChannel jump() {
        return JUMP;
    }

    public static ButtonChannel sprint() {
        return SPRINT;
    }

    /**
     * Returns true whenever the local player is riding the Flamestrider or holding any Velorise Genshin weapon
     * (where LMB = Attack/CA and RMB = Skill/Dash instead of vanilla Minecraft block mining).
     */
    public static boolean isGenshinModeActive(Player player) {
        if (player == null) return false;
        if (player.getVehicle() instanceof FlamestriderBikeEntity) return true;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) != null
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) != null;
    }

    /**
     * Called directly from raw GLFW mouse hooks (InputEvent.MouseButton.Pre / MouseHandlerMixin.onPress).
     * Never triggered by Minecraft's continueAttack mining loop.
     */
    public static void onRawMouseButton(int button, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null) {
            if (action == GLFW.GLFW_RELEASE) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) ATTACK.onRawButton(action);
                else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) SKILL.onRawButton(action);
            }
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ATTACK.onRawButton(action);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            SKILL.onRawButton(action);
        }
    }

    /**
     * Samples all Genshin input channels once per client tick.
     */
    public static void sampleTick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.screen != null) {
            reset();
            return;
        }
        long window = mc.getWindow().getWindow();
        boolean lmbPhys = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmbPhys = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!lmbPhys) ATTACK.rawDown = false;
        if (!rmbPhys) SKILL.rawDown = false;

        ATTACK.sample(mc.options.keyAttack.isDown() || lmbPhys);
        SKILL.sample(mc.options.keyUse.isDown() || rmbPhys);
        JUMP.sample(mc.options.keyJump.isDown());
        SPRINT.sample(mc.options.keySprint.isDown());
    }

    public static void reset() {
        ATTACK.reset();
        SKILL.reset();
        JUMP.reset();
        SPRINT.reset();
    }
}
