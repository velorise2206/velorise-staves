package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated client-side VFX & animation renderer for Dragonfire Scepter (Ender Dragon):
 * - Mini Ender Dragon pet with world-space free flight (independent of player camera/body rotation),
 *   stationary combat hover when fighting monsters, and exact world-space snout/mouth lightning origin
 * - Giant Ender Dragon Ulti Variants 1 (Skyfall Dive) and 2 (Sweeping Strafe)
 * - Giant Ender Dragon Snout-Petting Idle (3rd-person locked, 1st-person free look)
 */
public final class DragonfireVisualRenderer {
    public static final ResourceLocation ENDER_DRAGON_TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    public static final ResourceLocation ENDER_DRAGON_EYES_TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");

    // --- Tunable Mini Ender Dragon Pet constants (with units) ---
    public static final float FOLLOW_ORBIT_RADIUS = 1.65F;        // base blocks around owner in FOLLOW
    public static final float FOLLOW_HEIGHT_OFFSET = 0.55F;       // blocks above owner eye/top in FOLLOW
    public static final float FOLLOW_BOB_AMP = 0.16F;             // blocks vertical bobbing amplitude in FOLLOW
    public static final float FOLLOW_BOB_FREQ = 1.50F;            // rad/s vertical bobbing frequency in FOLLOW
    public static final float FOLLOW_OMEGA = 1.05F;               // rad/s world-space orbit angular speed in FOLLOW

    public static final double ENGAGE_DETECT_RANGE = 20.0D;       // blocks from owner to engage target
    public static final float ENGAGE_ORBIT_RADIUS = 4.20F;        // compatibility constant
    public static final float ENGAGE_HEIGHT_OFFSET = 1.85F;       // compatibility constant
    public static final float ENGAGE_BOB_AMP = 0.30F;             // compatibility constant
    public static final float ENGAGE_BOB_FREQ = 1.70F;            // compatibility constant
    public static final float ENGAGE_OMEGA = 1.35F;               // compatibility constant
    public static final float ATTACK_DASH_RADIUS = 2.60F;         // compatibility constant
    public static final long ENGAGE_HYSTERESIS_MS = 250L;         // brief grace window on target loss
    public static final double TELEPORT_DIST = 32.0D;             // blocks from anchor triggering instant teleport

    public static final double MAX_SPEED_FOLLOW = 7.0D;           // blocks/s (= 0.35 block/tick)
    public static final double MAX_SPEED_COMBAT = 10.0D;          // blocks/s when repositioning if owner walks far
    public static final double MAX_ACCEL = 32.0D;                 // blocks/s^2 (= 0.08 block/tick^2)
    public static final double ARRIVAL_K = 4.2D;                  // 1/s arrival braking gain
    public static final double DRAG = 1.1D;                       // 1/s exponential aerodynamic drag
    public static final float MAX_TURN_DEG_PER_SEC = 270.0F;      // deg/s yaw turn rate in FOLLOW
    public static final float COMBAT_TURN_DEG_PER_SEC = 720.0F;   // deg/s yaw turn rate in ENGAGE/ATTACK

    public static final float MODEL_SCALE_MINI = 0.085F;          // scale factor for 1.0-block wingspan mini dragon
    public static final float MOUTH_FORWARD = 5.666F;             // unscaled model blocks from pose origin to snout gap
    public static final float MOUTH_UP = -0.877F;                 // unscaled model blocks along pose up vector to mouth gap

    public enum PetState {
        FOLLOW,
        ENGAGE,
        ATTACK
    }

    public static final class PetPose {
        public Vec3 pos = Vec3.ZERO;
        public Vec3 prevPos = Vec3.ZERO;
        public Vec3 vel = Vec3.ZERO;
        public Vec3 anchor = Vec3.ZERO;
        public Vec3 combatHoverPos = null;
        public float yaw = 0.0F;          // Minecraft yaw convention (0 = +Z, -90 = +X, 180 = -Z)
        public float prevYaw = 0.0F;
        public float pitch = 0.0F;        // Minecraft pitch convention (+ = nose down, - = nose up)
        public float prevPitch = 0.0F;
        public float roll = 0.0F;         // Wing bank angle in degrees
        public float prevRoll = 0.0F;
        public float orbitAngle = 0.0F;   // World-space flight angle (radians), independent of player rotation
        public float flapTicks = 0.0F;    // Continuous time-driven animation ticks (never freezes)
        public float stateTime = 0.0F;
        public PetState state = PetState.FOLLOW;
        public int targetEntityId = -1;
        public int lastOwnerHurtMobId = -1;
        public int lastOwnerHurtMobTs = -1;
        public Vec3 lastTargetSeenPos = null;
        public float lastTargetHeight = 1.8F;
        public long lastTargetSeenMs = 0L;
        public long lastTargetScanMs = 0L;
        public Vec3 mouthWorldPos = Vec3.ZERO;
        public boolean initialized = false;
        public long lastUpdateNs = 0L;
        public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastDimension = null;
    }

    private static final Map<Integer, PetPose> PET_POSES = new ConcurrentHashMap<>();

    public static void clearClientCaches() {
        PET_POSES.clear();
    }

    public static float approachAngle(float cur, float tgt, float maxStep) {
        float d = Mth.wrapDegrees(tgt - cur);
        return cur + Mth.clamp(d, -maxStep, maxStep);
    }

    public static @org.jetbrains.annotations.Nullable Vec3 getActivePetMouthWorldPos(int ownerEntityId) {
        PetPose pose = PET_POSES.get(ownerEntityId);
        if (pose == null || !pose.initialized) return null;
        return pose.mouthWorldPos;
    }

    /**
     * Called immediately when an attack lightning packet arrives on the client so the pet dragon
     * stops in place, aims directly at the target, opens its jaw, and returns the exact world-space
     * mouth origin for frame 0 of the lightning bolt.
     */
    public static @org.jetbrains.annotations.Nullable Vec3 snapPetToAttackAndGetMouthPos(
            int ownerEntityId,
            int targetEntityId,
            @org.jetbrains.annotations.Nullable Vec3 fallbackTargetCenter
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity ownerEnt = mc.level.getEntity(ownerEntityId);
        if (!(ownerEnt instanceof LivingEntity owner) || !owner.isAlive()) return null;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 ownerWorld = owner.getPosition(pt);
        PetPose pose = PET_POSES.computeIfAbsent(ownerEntityId, k -> new PetPose());
        if (pose.lastDimension != owner.level().dimension()) {
            pose.initialized = false;
            pose.lastDimension = owner.level().dimension();
        }
        if (!pose.initialized) {
            initializePetPose(pose, owner, ownerWorld, pt, System.nanoTime());
        }

        Vec3 aimPt = fallbackTargetCenter;
        if (targetEntityId > 0 && mc.level.getEntity(targetEntityId) instanceof LivingEntity tgt && tgt.isAlive()) {
            Vec3 tgtPos = tgt.getPosition(pt);
            pose.targetEntityId = targetEntityId;
            pose.lastTargetSeenPos = tgtPos;
            pose.lastTargetHeight = tgt.getBbHeight();
            pose.lastTargetSeenMs = System.currentTimeMillis();
            aimPt = tgtPos.add(0.0D, tgt.getBbHeight() * 0.55D, 0.0D);
        }

        // Stop immediately in place to fire
        pose.state = PetState.ATTACK;
        pose.combatHoverPos = pose.pos;
        pose.anchor = pose.pos;
        pose.vel = Vec3.ZERO;
        pose.roll = 0.0F;
        pose.prevRoll = 0.0F;

        if (aimPt != null) {
            Vec3 toTgt = aimPt.subtract(pose.pos);
            double hLen = Math.hypot(toTgt.x, toTgt.z);
            double totalDist = Math.max(0.65D, toTgt.length());
            double mouthPitchCorrDeg = Math.toDegrees(Math.atan2(0.0745D, totalDist));
            pose.yaw = (float) Math.toDegrees(Math.atan2(-toTgt.x, toTgt.z));
            pose.prevYaw = pose.yaw;
            pose.pitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(-toTgt.y, Math.max(1.0E-4D, hLen))) - mouthPitchCorrDeg, -55.0D, 55.0D);
            pose.prevPitch = pose.pitch;
        }

        Vec3 exactMouth = computeExactMouthWorldPos(
                pose, 0.50F, 0.085F,
                pose.pitch, pose.roll, 0.0F, 0.0F, 0.0F,
                0.0F, 36.0F, pose.flapTicks, 1.0F
        );
        pose.mouthWorldPos = exactMouth;
        return exactMouth;
    }

    private static void initializePetPose(PetPose pose, LivingEntity entity, Vec3 ownerWorld, float pt, long nowNs) {
        pose.orbitAngle = (entity.getId() * 1.618F) % ((float) Math.PI * 2.0F);
        Vec3 initPos = ownerWorld.add(
                Math.cos(pose.orbitAngle) * FOLLOW_ORBIT_RADIUS,
                entity.getBbHeight() + FOLLOW_HEIGHT_OFFSET,
                Math.sin(pose.orbitAngle) * FOLLOW_ORBIT_RADIUS
        );
        double tx = -Math.sin(pose.orbitAngle);
        double tz = Math.cos(pose.orbitAngle);
        float initYaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
        pose.pos = initPos;
        pose.prevPos = initPos;
        pose.anchor = initPos;
        pose.combatHoverPos = null;
        pose.vel = Vec3.ZERO;
        pose.yaw = initYaw;
        pose.prevYaw = initYaw;
        pose.pitch = 0.0F;
        pose.prevPitch = 0.0F;
        pose.roll = 0.0F;
        pose.prevRoll = 0.0F;
        pose.flapTicks = entity.tickCount + pt;
        pose.stateTime = 0.0F;
        pose.state = PetState.FOLLOW;
        pose.lastUpdateNs = nowNs;
        pose.initialized = true;
    }

    private static ModelPart dragonModelRoot = null;
    private static ModelPart dragonHead = null;
    private static ModelPart dragonJaw = null;
    private static ModelPart dragonNeck = null;
    private static ModelPart dragonBody = null;
    private static ModelPart dragonLeftWing = null;
    private static ModelPart dragonLeftWingTip = null;
    private static ModelPart dragonRightWing = null;
    private static ModelPart dragonRightWingTip = null;
    private static ModelPart dragonLeftFrontLeg = null;
    private static ModelPart dragonLeftFrontLegTip = null;
    private static ModelPart dragonLeftFrontFoot = null;
    private static ModelPart dragonRightFrontLeg = null;
    private static ModelPart dragonRightFrontLegTip = null;
    private static ModelPart dragonRightFrontFoot = null;
    private static ModelPart dragonLeftHindLeg = null;
    private static ModelPart dragonLeftHindLegTip = null;
    private static ModelPart dragonLeftHindFoot = null;
    private static ModelPart dragonRightHindLeg = null;
    private static ModelPart dragonRightHindLegTip = null;
    private static ModelPart dragonRightHindFoot = null;

    private DragonfireVisualRenderer() {}

    private static boolean ensureDragonPartsBaked() {
        if (dragonModelRoot != null) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getEntityModels() == null) return false;
        ModelPart root = mc.getEntityModels().bakeLayer(ModelLayers.ENDER_DRAGON);
        dragonModelRoot = root;
        dragonHead = root.getChild("head");
        dragonJaw = dragonHead.getChild("jaw");
        dragonNeck = root.getChild("neck");
        dragonBody = root.getChild("body");
        dragonLeftWing = root.getChild("left_wing");
        dragonLeftWingTip = dragonLeftWing.getChild("left_wing_tip");
        dragonRightWing = root.getChild("right_wing");
        dragonRightWingTip = dragonRightWing.getChild("right_wing_tip");
        dragonLeftFrontLeg = root.getChild("left_front_leg");
        dragonLeftFrontLegTip = dragonLeftFrontLeg.getChild("left_front_leg_tip");
        dragonLeftFrontFoot = dragonLeftFrontLegTip.getChild("left_front_foot");
        dragonRightFrontLeg = root.getChild("right_front_leg");
        dragonRightFrontLegTip = dragonRightFrontLeg.getChild("right_front_leg_tip");
        dragonRightFrontFoot = dragonRightFrontLegTip.getChild("right_front_foot");
        dragonLeftHindLeg = root.getChild("left_hind_leg");
        dragonLeftHindLegTip = dragonLeftHindLeg.getChild("left_hind_leg_tip");
        dragonLeftHindFoot = dragonLeftHindLegTip.getChild("left_hind_foot");
        dragonRightHindLeg = root.getChild("right_hind_leg");
        dragonRightHindLegTip = dragonRightHindLeg.getChild("right_hind_leg_tip");
        dragonRightHindFoot = dragonRightHindLegTip.getChild("right_hind_foot");
        return true;
    }

    private static void renderDragonGeometryPass(
            PoseStack poseStack,
            VertexConsumer vc,
            float time,
            float flapRad,
            float walkBlend,
            float jawOpenDeg,
            float headPitchDeg,
            float headYawDeg,
            float headRollDeg,
            boolean stationaryLegs,
            boolean steadyAim
    ) {
        float jawRad = jawOpenDeg * ((float) Math.PI / 180.0F);
        float headPitchRad = headPitchDeg * ((float) Math.PI / 180.0F);
        float headYawRad = headYawDeg * ((float) Math.PI / 180.0F);
        float headRollRad = headRollDeg * ((float) Math.PI / 180.0F);

        float ny = 20.0F;
        float nz = -12.0F;
        float nx = 0.0F;
        float breath = stationaryLegs ? Mth.sin(time * 0.12F) : 0.0F;
        float breathCos = stationaryLegs ? Mth.cos(time * 0.12F) : 0.0F;
        float neckSway = steadyAim ? 0.0F : (stationaryLegs ? (Mth.sin(time * 0.09F) * 0.012F) : Mth.sin(time * 0.14F) * 0.035F);
        float neckPitchBase = steadyAim ? 0.0F : Mth.lerp(walkBlend, 0.04F - Mth.cos(flapRad) * 0.05F, stationaryLegs ? (0.02F + breath * 0.016F) : -0.07F);
        dragonJaw.xRot = jawRad + (stationaryLegs ? Math.max(0.0F, breath) * 0.025F : 0.0F);

        for (int i = 0; i < 5; ++i) {
            float segPitch = steadyAim ? (headPitchRad * 0.12F) : (neckPitchBase + (i - 2) * 0.025F + headPitchRad * 0.12F);
            float segYaw = headYawRad * (0.08F * (i + 1)) + neckSway * (i + 1);
            dragonNeck.xRot = segPitch;
            dragonNeck.yRot = segYaw;
            dragonNeck.zRot = headRollRad * 0.08F * (i + 1);
            dragonNeck.x = nx;
            dragonNeck.y = ny;
            dragonNeck.z = nz;
            dragonNeck.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            nx -= Mth.sin(segYaw) * Mth.cos(segPitch) * 10.0F;
            ny += Mth.sin(segPitch) * 10.0F;
            nz -= Mth.cos(segPitch) * Mth.cos(segYaw) * 10.0F;
        }

        dragonHead.x = nx;
        dragonHead.y = ny;
        dragonHead.z = nz;
        dragonHead.xRot = headPitchRad + neckPitchBase * 0.5F + (stationaryLegs ? breath * 0.018F : 0.0F);
        dragonHead.yRot = headYawRad + neckSway * 5.0F;
        dragonHead.zRot = headRollRad;
        dragonHead.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        dragonBody.xRot = stationaryLegs ? (breath * 0.012F) : 0.0F;
        dragonBody.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        float flyWingX = 0.125F - Mth.cos(flapRad) * 0.2F;
        float flyWingY = 0.25F;
        float flyWingZ = (Mth.sin(flapRad) + 0.125F) * 0.8F;
        float flyTipZ = -(Mth.sin(flapRad + 2.0F) + 0.5F) * 0.75F;

        dragonLeftWing.xRot = Mth.lerp(walkBlend, flyWingX, stationaryLegs ? (0.42F + breath * 0.032F) : 0.42F);
        dragonLeftWing.yRot = Mth.lerp(walkBlend, flyWingY, stationaryLegs ? (0.35F + breathCos * 0.025F) : 0.35F);
        dragonLeftWing.zRot = Mth.lerp(walkBlend, flyWingZ, stationaryLegs ? (-0.95F + breath * 0.045F) : -0.95F);
        dragonLeftWingTip.zRot = Mth.lerp(walkBlend, flyTipZ, stationaryLegs ? (1.35F - breathCos * 0.055F) : 1.35F);

        dragonRightWing.xRot = dragonLeftWing.xRot;
        dragonRightWing.yRot = -dragonLeftWing.yRot;
        dragonRightWing.zRot = -dragonLeftWing.zRot;
        dragonRightWingTip.zRot = -dragonLeftWingTip.zRot;

        dragonLeftWing.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        dragonRightWing.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        float walkCycle = time * 0.40F;
        float strideA = stationaryLegs ? 0.0F : Mth.sin(walkCycle) * 0.46F;
        float strideB = stationaryLegs ? 0.0F : -strideA;
        float flyFront = 1.15F + Mth.sin(flapRad) * 0.08F;
        float flyHind = 1.05F + Mth.sin(flapRad) * 0.08F;

        dragonLeftFrontLeg.xRot = Mth.lerp(walkBlend, flyFront, stationaryLegs ? (0.25F - breath * 0.025F) : (0.10F + strideA));
        dragonLeftFrontLegTip.xRot = Mth.lerp(walkBlend, -0.45F, stationaryLegs ? (-0.35F + breath * 0.030F) : (-0.30F - Math.max(0.0F, strideA) * 0.40F));
        dragonLeftFrontFoot.xRot = Mth.lerp(walkBlend, 0.75F, stationaryLegs ? 0.10F : 0.22F);

        dragonRightFrontLeg.xRot = Mth.lerp(walkBlend, flyFront, stationaryLegs ? (0.25F - breath * 0.025F) : (0.10F + strideB));
        dragonRightFrontLegTip.xRot = Mth.lerp(walkBlend, -0.45F, stationaryLegs ? (-0.35F + breath * 0.030F) : (-0.30F - Math.max(0.0F, strideB) * 0.40F));
        dragonRightFrontFoot.xRot = Mth.lerp(walkBlend, 0.75F, stationaryLegs ? 0.10F : 0.22F);

        dragonLeftHindLeg.xRot = Mth.lerp(walkBlend, flyHind, stationaryLegs ? (0.18F - breath * 0.025F) : (0.08F + strideB));
        dragonLeftHindLegTip.xRot = Mth.lerp(walkBlend, 0.45F, stationaryLegs ? (0.30F + breath * 0.030F) : 0.28F);
        dragonLeftHindFoot.xRot = Mth.lerp(walkBlend, 0.75F, stationaryLegs ? -0.28F : -0.25F);

        dragonRightHindLeg.xRot = Mth.lerp(walkBlend, flyHind, stationaryLegs ? (0.18F - breath * 0.025F) : (0.08F + strideA));
        dragonRightHindLegTip.xRot = Mth.lerp(walkBlend, 0.45F, stationaryLegs ? (0.30F + breath * 0.030F) : 0.28F);
        dragonRightHindFoot.xRot = Mth.lerp(walkBlend, 0.75F, stationaryLegs ? -0.28F : -0.25F);

        dragonLeftFrontLeg.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        dragonRightFrontLeg.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        dragonLeftHindLeg.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        dragonRightHindLeg.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        float tx = 0.0F;
        float ty = 10.0F;
        float tz = 55.0F;
        for (int i = 0; i < 12; ++i) {
            float wave = stationaryLegs ? (time * 0.09F - i * 0.28F) : (time * 0.18F - i * 0.35F);
            float tYaw = stationaryLegs ? Mth.sin(wave) * 0.035F : (Mth.sin(wave) * (0.045F + 0.012F * i) - headYawRad * 0.04F);
            float tPitch = stationaryLegs ? (-0.04F + walkBlend * 0.05F + Mth.cos(wave) * 0.014F) : (-0.06F + Mth.cos(flapRad - i * 0.30F) * 0.045F + walkBlend * 0.07F);
            dragonNeck.xRot = tPitch;
            dragonNeck.yRot = tYaw;
            dragonNeck.zRot = 0.0F;
            dragonNeck.x = tx;
            dragonNeck.y = ty;
            dragonNeck.z = tz;
            dragonNeck.render(poseStack, vc, WindGliderRenderer.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            tx += Mth.sin(tYaw) * Mth.cos(tPitch) * 10.0F;
            ty -= Mth.sin(tPitch) * 10.0F;
            tz += Mth.cos(tPitch) * Mth.cos(tYaw) * 10.0F;
        }
        dragonNeck.x = 0.0F;
        dragonNeck.y = 0.0F;
        dragonNeck.z = 0.0F;
        dragonNeck.xRot = 0.0F;
        dragonNeck.yRot = 0.0F;
        dragonNeck.zRot = 0.0F;
    }

    /**
     * Computes the exact world-space position of the dragon's open mouth cavity using an
     * identity-rooted PoseStack that mirrors the exact model hierarchy (immune to camera rotation,
     * camera distance, or player crouch render offsets).
     */
    public static Vec3 computeExactMouthWorldPos(
            PetPose pose,
            float halfWingspan,
            float flapSpeed,
            float bodyPitchDeg,
            float bankRollDeg,
            float headYawDeg,
            float headPitchDeg,
            float headRollDeg,
            float walkBlend,
            float jawOpenDeg,
            float time,
            float scaleMul
    ) {
        boolean stationaryLegs = (flapSpeed <= 0.001F);
        boolean steadyAim = (halfWingspan <= 0.60F && jawOpenDeg >= 13.5F);
        float baseScale = (halfWingspan <= 0.60F) ? MODEL_SCALE_MINI : (halfWingspan / 2.50F) * 0.28F;
        float scale = baseScale * Mth.clamp(scaleMul, 0.05F, 3.60F);
        float flapCycle = stationaryLegs ? 0.0F : ((time * flapSpeed) % 1.0F);
        float flapRad = flapCycle * ((float) Math.PI * 2.0F);
        float bounceY = steadyAim ? 0.12F : (stationaryLegs ? (Mth.sin(time * 0.12F) * 0.035F) : Mth.lerp(walkBlend, (float) (Math.sin(flapRad) + 1.0D) * 0.2F,
                Mth.abs(Mth.sin(time * 0.40F)) * 0.08F));

        float jawRad = jawOpenDeg * ((float) Math.PI / 180.0F);
        float headPitchRad = headPitchDeg * ((float) Math.PI / 180.0F);
        float headYawRad = headYawDeg * ((float) Math.PI / 180.0F);
        float headRollRad = headRollDeg * ((float) Math.PI / 180.0F);

        float ny = 20.0F;
        float nz = -12.0F;
        float nx = 0.0F;
        float breath = stationaryLegs ? Mth.sin(time * 0.12F) : 0.0F;
        float neckSway = steadyAim ? 0.0F : (stationaryLegs ? (Mth.sin(time * 0.09F) * 0.012F) : Mth.sin(time * 0.14F) * 0.035F);
        float neckPitchBase = steadyAim ? 0.0F : Mth.lerp(walkBlend, 0.04F - Mth.cos(flapRad) * 0.05F, stationaryLegs ? (0.02F + breath * 0.016F) : -0.07F);
        float effectiveJawRad = jawRad + (stationaryLegs ? Math.max(0.0F, breath) * 0.025F : 0.0F);

        for (int i = 0; i < 5; ++i) {
            float segPitch = steadyAim ? (headPitchRad * 0.12F) : (neckPitchBase + (i - 2) * 0.025F + headPitchRad * 0.12F);
            float segYaw = headYawRad * (0.08F * (i + 1)) + neckSway * (i + 1);
            nx -= Mth.sin(segYaw) * Mth.cos(segPitch) * 10.0F;
            ny += Mth.sin(segPitch) * 10.0F;
            nz -= Mth.cos(segPitch) * Mth.cos(segYaw) * 10.0F;
        }

        float hxRot = headPitchRad + neckPitchBase * 0.5F + (stationaryLegs ? breath * 0.018F : 0.0F);
        float hyRot = headYawRad + neckSway * 5.0F;
        float hzRot = headRollRad;

        PoseStack mouthStack = new PoseStack();
        mouthStack.mulPose(Axis.YP.rotationDegrees(180.0F - pose.yaw));
        if (bodyPitchDeg != 0.0F) mouthStack.mulPose(Axis.XP.rotationDegrees(-bodyPitchDeg));
        if (bankRollDeg != 0.0F) mouthStack.mulPose(Axis.ZP.rotationDegrees(bankRollDeg));
        mouthStack.scale(-scale, -scale, scale);
        mouthStack.translate(0.0F, -1.00F + bounceY, -0.50F);
        mouthStack.translate(nx / 16.0F, ny / 16.0F, nz / 16.0F);
        if (hzRot != 0.0F || hyRot != 0.0F || hxRot != 0.0F) {
            mouthStack.mulPose(new Quaternionf().rotationZYX(hzRot, hyRot, hxRot));
        }

        float yMouth = 0.25F + 0.4375F * Mth.sin(effectiveJawRad);
        float zMouth = -0.50F - 0.875F * (0.5F + 0.5F * Mth.cos(effectiveJawRad));
        Vector4f mp = new Vector4f(0.0F, yMouth, zMouth, 1.0F);
        mp.mul(mouthStack.last().pose());
        return pose.pos.add(mp.x(), mp.y(), mp.z());
    }

    public static void renderStylizedEnderDragonModel(
            PoseStack poseStack,
            MultiBufferSource buffer,
            float halfWingspan,
            float flapSpeed,
            float bodyPitchDeg,
            float bankRollDeg,
            float headYawDeg,
            float headPitchDeg,
            float headRollDeg,
            float walkBlend,
            float jawOpenDeg,
            float time,
            float scaleMul
    ) {
        if (!ensureDragonPartsBaked()) return;

        boolean stationaryLegs = (flapSpeed <= 0.001F);
        boolean steadyAim = (halfWingspan <= 0.60F && jawOpenDeg >= 13.5F);
        float baseScale = (halfWingspan <= 0.60F) ? MODEL_SCALE_MINI : (halfWingspan / 2.50F) * 0.28F;
        float scale = baseScale * Mth.clamp(scaleMul, 0.05F, 3.60F);
        float flapCycle = stationaryLegs ? 0.0F : ((time * flapSpeed) % 1.0F);
        float flapRad = flapCycle * ((float) Math.PI * 2.0F);
        float bounceY = steadyAim ? 0.12F : (stationaryLegs ? (Mth.sin(time * 0.12F) * 0.035F) : Mth.lerp(walkBlend, (float) (Math.sin(flapRad) + 1.0D) * 0.2F,
                Mth.abs(Mth.sin(time * 0.40F)) * 0.08F));

        poseStack.pushPose();
        if (bodyPitchDeg != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(-bodyPitchDeg));
        if (bankRollDeg != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(bankRollDeg));
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.00F + bounceY, -0.50F);

        VertexConsumer skinVc = buffer.getBuffer(RenderType.entityCutoutNoCull(ENDER_DRAGON_TEX));
        renderDragonGeometryPass(poseStack, skinVc, time, flapRad, walkBlend, jawOpenDeg, headPitchDeg, headYawDeg, headRollDeg, stationaryLegs, steadyAim);
        VertexConsumer eyesVc = buffer.getBuffer(RenderType.eyes(ENDER_DRAGON_EYES_TEX));
        renderDragonGeometryPass(poseStack, eyesVc, time, flapRad, walkBlend, jawOpenDeg, headPitchDeg, headYawDeg, headRollDeg, stationaryLegs, steadyAim);

        poseStack.popPose();
    }

    public static void renderThirdPersonDragonfire(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        Vec3 renderOffset = renderer != null ? renderer.getRenderOffset(entity, pt) : Vec3.ZERO;
        poseStack.pushPose();
        if (renderOffset.lengthSqr() > 1.0E-7D) {
            poseStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);
        }
        renderDragonfireEffects(entity, poseStack, buffer);
        poseStack.popPose();
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.entityCutoutNoCull(ENDER_DRAGON_TEX));
            bs.endBatch(RenderType.eyes(ENDER_DRAGON_EYES_TEX));
        }
    }

    public static void renderFirstPersonWorldDragonfire(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientAuraRenderer.isRenderingScreen() || mc.level == null || mc.player == null
                || !mc.player.isAlive() || mc.player.isSpectator() || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 pPos = mc.player.getPosition(pt).subtract(camPos);
        poseStack.pushPose();
        poseStack.translate(pPos.x, pPos.y, pPos.z);
        renderDragonfireEffects(mc.player, poseStack, bufferSource);
        poseStack.popPose();
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.entityCutoutNoCull(ENDER_DRAGON_TEX));
            bs.endBatch(RenderType.eyes(ENDER_DRAGON_EYES_TEX));
        }
    }

    public static boolean applyDragonfireIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity == null || entity.isUsingItem() || entity.swinging) return false;
        if (!ClientAuraTracker.isDragonfireIdle(entity.getId())) return false;
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        if (data == null) return false;
        float w = WindGliderRenderer.smoothStep01(data.dragonfireIdleFade);
        if (w <= 0.01F) return false;

        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        HumanoidArm mainArm = entity.getMainArm();
        var mainPart = (mainArm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        var offPart = (mainArm == HumanoidArm.RIGHT) ? model.leftArm : model.rightArm;
        float castDir = (mainArm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;

        float petBob = Mth.sin(ageInTicks * 0.16F) * 0.06F;
        float petSway = Mth.cos(ageInTicks * 0.16F) * 0.05F;
        float breathe = Mth.sin(ageInTicks * 0.08F);

        model.body.xRot = Mth.lerp(w, model.body.xRot, 0.04F + breathe * 0.015F);
        mainPart.xRot = Mth.lerp(w, mainPart.xRot, -2.18F + petBob);
        mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.14F);
        mainPart.zRot = Mth.lerp(w, mainPart.zRot, petSway);
        offPart.xRot = Mth.lerp(w, offPart.xRot, 0.08F + breathe * 0.02F);
        offPart.zRot = Mth.lerp(w, offPart.zRot, -castDir * 0.08F);

        model.head.xRot = Mth.lerp(w, model.head.xRot, -0.48F + breathe * 0.02F);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.35F, 0.35F), 0.0F);
        model.hat.copyFrom(model.head);
        return true;
    }

    public static boolean setupFirstPersonDragonfireIdleArmPose(
            AbstractClientPlayer player,
            float partialTicks,
            HumanoidArm mainArm,
            PoseStack poseStack
    ) {
        if (player == null || player.isUsingItem() || player.swinging) return false;
        if (!ClientAuraTracker.isDragonfireIdle(player.getId())) return false;
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        if (data == null) return false;
        float w = WindGliderRenderer.smoothStep01(data.dragonfireIdleFade);
        if (w <= 0.02F) return false;

        if (!data.dragonfireIdleYawSet) {
            data.dragonfireIdleYaw = player.getViewYRot(partialTicks);
            data.dragonfireIdleYawSet = true;
        }
        float dYaw = Mth.wrapDegrees(player.getViewYRot(partialTicks) - data.dragonfireIdleYaw);
        float dPitch = player.getViewXRot(partialTicks) - (-26.0F);
        if (Math.abs(dYaw) > 95.0F || Math.abs(dPitch) > 85.0F) {
            return false;
        }

        float armDir = (mainArm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        float t = player.tickCount + partialTicks;
        float petBob = Mth.sin(t * 0.16F) * 0.022F;
        float petSwayDeg = Mth.cos(t * 0.16F) * 3.5F;

        poseStack.mulPose(Axis.XP.rotationDegrees(dPitch * w));
        poseStack.mulPose(Axis.YP.rotationDegrees(dYaw * w));
        poseStack.translate(-armDir * 0.14F * w, -0.08F * (1.0F - w) + 0.04F * w + petBob * w, -0.28F * w);
        poseStack.mulPose(Axis.ZP.rotationDegrees(armDir * (26.0F + petSwayDeg) * w));
        poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * w));
        poseStack.mulPose(Axis.YP.rotationDegrees(-armDir * 14.0F * w));
        return true;
    }

    public static void renderDragonfireEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        if (data == null) return;

        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;

        float targetIdleFade = data.dragonfireIdle ? 1.0F : 0.0F;
        data.dragonfireIdleFade = Mth.lerp(0.12F, data.dragonfireIdleFade, targetIdleFade);

        boolean petActive = now < data.dragonfirePetUntil;
        boolean idleVisible = data.dragonfireIdle || data.dragonfireIdleFade > 0.02F;
        int activeAnim = now < data.staffAnimUntil ? data.staffAnimType : 0;

        if (!petActive && !idleVisible
                && activeAnim != ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SKYFALL
                && activeAnim != ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SWEEP) {
            return;
        }

        float elapsedMs = Math.max(0.0F, (float) (now - data.staffAnimStartTime));

        // 1. Ulti Variant 1 — Skyfall Dive
        if (activeAnim == ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SKYFALL && elapsedMs < 850.0F) {
            if (!data.dragonfireUltiYawSet) {
                data.dragonfireUltiYaw = entity.getViewYRot(pt);
                data.dragonfireUltiYawSet = true;
            }
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.dragonfireUltiYaw));
            if (elapsedMs < 600.0F) {
                float u = Mth.clamp(elapsedMs / 600.0F, 0.0F, 1.0F);
                float easeOut = 1.0F - (1.0F - u) * (1.0F - u);
                float diveY = Mth.lerp(easeOut, 14.5F, 0.50F);
                float diveZ = Mth.lerp(easeOut, 2.5F, -5.5F);
                float pitch = Mth.lerp(WindGliderRenderer.smoothStep01(u), 56.0F, 2.0F);
                poseStack.translate(0.0F, diveY, diveZ);
                renderStylizedEnderDragonModel(poseStack, buffer, 2.50F, 0.11F, pitch, 0.0F, 0.0F, -8.0F, 0.0F, 0.0F, 28.0F, time, 1.0F);
            } else {
                float groundU = Mth.clamp((elapsedMs - 600.0F) / 250.0F, 0.0F, 1.0F);
                float settlePitch = Mth.lerp(WindGliderRenderer.smoothStep01(groundU), 4.0F, -6.0F);
                poseStack.translate(0.0F, 0.50F, -5.5F);
                renderStylizedEnderDragonModel(poseStack, buffer, 2.50F, 0.04F, settlePitch, 0.0F, 0.0F, 0.0F, 0.0F, 0.35F, 34.0F, time, 1.0F);
            }
            poseStack.popPose();
            return;
        }

        // 2. Ulti Variant 2 — Sweeping Strafe
        if (activeAnim == ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SWEEP && elapsedMs < 1500.0F) {
            if (!data.dragonfireUltiYawSet) {
                data.dragonfireUltiYaw = entity.getViewYRot(pt);
                data.dragonfireUltiYawSet = true;
            }
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.dragonfireUltiYaw));
            if (elapsedMs < 1000.0F) {
                float u = Mth.clamp(elapsedMs / 1000.0F, 0.0F, 1.0F);
                float forwardZ = Mth.lerp(u, 1.5F, -20.0F);
                float swoopX = Mth.sin(u * (float) Math.PI) * 0.45F;
                float swoopY;
                float pitchDeg;
                if (u < 0.45F) {
                    float t = u / 0.45F;
                    float d = 1.0F - t;
                    swoopY = 0.70F + 6.2F * d * d;
                    pitchDeg = Mth.lerp(WindGliderRenderer.smoothStep01(t), 42.0F, 0.0F);
                } else {
                    float t = (u - 0.45F) / 0.55F;
                    swoopY = 0.70F + 2.4F * t * t;
                    pitchDeg = Mth.lerp(WindGliderRenderer.smoothStep01(t), 0.0F, -32.0F);
                }
                float bankDeg = Mth.sin(u * (float) Math.PI) * 14.0F;
                poseStack.translate(swoopX, swoopY, forwardZ);
                renderStylizedEnderDragonModel(poseStack, buffer, 2.50F, 0.12F, pitchDeg, bankDeg, 0.0F, 0.0F, 0.0F, 0.0F, 30.0F, time, 1.0F);
            } else {
                float u = Mth.clamp((elapsedMs - 1000.0F) / 500.0F, 0.0F, 1.0F);
                float climbEase = u * u;
                float climbZ = Mth.lerp(u, -20.0F, -28.0F);
                float climbY = Mth.lerp(climbEase, 3.10F, 22.0F);
                float pitchDeg = Mth.lerp(WindGliderRenderer.smoothStep01(u), -32.0F, -82.0F);
                poseStack.translate(0.0F, climbY, climbZ);
                renderStylizedEnderDragonModel(poseStack, buffer, 2.50F, 0.14F, pitchDeg, 0.0F, 0.0F, -10.0F, 0.0F, 0.0F, 24.0F, time, 1.0F);
            }
            poseStack.popPose();
            return;
        }

        float idleW = data.dragonfireIdleFade;

        // 3. Dragonfire Idle: Full Ender-Dragon-scale dragon stands in world space closer for petting
        if (idleVisible) {
            if (!data.dragonfireIdleYawSet) {
                data.dragonfireIdleYaw = entity.getViewYRot(pt);
                data.dragonfireIdleYawSet = true;
            }
            float idleScale = 3.20F * WindGliderRenderer.smoothStep01(idleW);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.dragonfireIdleYaw));
            poseStack.translate(0.0F, 2.05F, -5.85F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            renderStylizedEnderDragonModel(
                    poseStack, buffer, 2.50F, 0.0F,
                    -8.0F, 0.0F, 0.0F, 18.0F, 0.0F,
                    1.0F, 2.0F, time, idleScale
            );
            poseStack.popPose();
            return;
        }

        // 4. Mini Ender Dragon (1.0-block wingspan): world-space free flight & stationary combat hover
        if (!petActive) {
            PET_POSES.remove(entity.getId());
            return;
        }
        updateAndRenderMiniDragonPet(entity, data, poseStack, buffer, pt, now, mc);
    }

    private static boolean isValidClientMiniDragonTarget(LivingEntity owner, @org.jetbrains.annotations.Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target == owner || target.isSpectator() || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof ArmorStand) return false;
        if (target instanceof Mob mob && mob.isNoAi()) return false;
        if (owner.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal ta && ta.isTame()) return false;
        if (target instanceof OwnableEntity oe && oe.getOwnerUUID() != null) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return true;
    }

    private static @org.jetbrains.annotations.Nullable LivingEntity resolveMiniDragonTarget(
            LivingEntity owner,
            ClientAuraTracker.AuraDisplayData data,
            PetPose pose,
            Vec3 ownerWorld,
            Minecraft mc
    ) {
        if (mc.level == null) return null;
        double maxLockDistSqr = (ENGAGE_DETECT_RANGE + 4.0D) * (ENGAGE_DETECT_RANGE + 4.0D);
        double maxScanSqr = ENGAGE_DETECT_RANGE * ENGAGE_DETECT_RANGE;

        LivingEntity lastHurt = owner.getLastHurtMob();
        if (lastHurt != null) {
            int hurtId = lastHurt.getId();
            int hurtTs = owner.getLastHurtMobTimestamp();
            if (hurtId != pose.lastOwnerHurtMobId || hurtTs != pose.lastOwnerHurtMobTs) {
                pose.lastOwnerHurtMobId = hurtId;
                pose.lastOwnerHurtMobTs = hurtTs;
                if (isValidClientMiniDragonTarget(owner, lastHurt)
                        && lastHurt.position().distanceToSqr(ownerWorld) <= maxScanSqr) {
                    data.dragonfirePetTargetId = hurtId;
                    pose.targetEntityId = hurtId;
                    return lastHurt;
                }
            }
        }

        int[] candidateIds = { data.dragonfirePetTargetId, pose.targetEntityId };
        for (int cid : candidateIds) {
            if (cid > 0) {
                Entity ent = mc.level.getEntity(cid);
                if (ent instanceof LivingEntity le
                        && isValidClientMiniDragonTarget(owner, le)
                        && le.position().distanceToSqr(ownerWorld) <= maxLockDistSqr) {
                    return le;
                } else {
                    if (data.dragonfirePetTargetId == cid) data.dragonfirePetTargetId = -1;
                    if (pose.targetEntityId == cid) {
                        pose.targetEntityId = -1;
                        pose.lastTargetScanMs = 0L;
                    }
                }
            }
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - pose.lastTargetScanMs < 150L) {
            return null;
        }
        pose.lastTargetScanMs = nowMs;

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(ENGAGE_DETECT_RANGE, 8.0D, ENGAGE_DETECT_RANGE),
                ent -> isValidClientMiniDragonTarget(owner, ent))) {
            boolean hostile = e instanceof Enemy
                    || e == owner.getLastHurtMob()
                    || e == owner.getLastHurtByMob();
            if (!hostile) continue;
            double d = e.position().distanceToSqr(ownerWorld);
            if (d <= maxScanSqr && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static void updateAndRenderMiniDragonPet(
            LivingEntity entity,
            ClientAuraTracker.AuraDisplayData data,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float pt,
            long now,
            Minecraft mc
    ) {
        PetPose pose = PET_POSES.computeIfAbsent(entity.getId(), k -> new PetPose());
        Vec3 ownerWorld = entity.getPosition(pt);

        if (pose.lastDimension != entity.level().dimension()) {
            pose.initialized = false;
            pose.lastDimension = entity.level().dimension();
        }

        long nowNs = System.nanoTime();
        float dt = 0.0F;
        if (!pose.initialized) {
            initializePetPose(pose, entity, ownerWorld, pt, nowNs);
        } else {
            long diffNs = nowNs - pose.lastUpdateNs;
            if (diffNs > 500_000L) {
                dt = (float) Math.min(0.05D, diffNs / 1_000_000_000.0D);
                pose.lastUpdateNs = nowNs;
            }
        }

        boolean attacking = now < data.dragonfirePetAttackUntilMs;
        LivingEntity target = resolveMiniDragonTarget(entity, data, pose, ownerWorld, mc);
        Vec3 targetPos = null;
        float targetHeight = pose.lastTargetHeight;

        if (target != null) {
            targetPos = target.getPosition(pt);
            targetHeight = target.getBbHeight();
        } else if (attacking && data.dragonfirePetAttackTarget != null) {
            targetPos = data.dragonfirePetAttackTarget.subtract(0.0D, targetHeight * 0.55D, 0.0D);
        }

        if (dt > 0.0F) {
            pose.stateTime += dt;

            if (target != null) {
                pose.targetEntityId = target.getId();
                pose.lastTargetSeenPos = targetPos;
                pose.lastTargetHeight = targetHeight;
                pose.lastTargetSeenMs = now;
                pose.state = attacking ? PetState.ATTACK : PetState.ENGAGE;
            } else if (attacking && data.dragonfirePetAttackTarget != null) {
                pose.lastTargetSeenPos = targetPos;
                pose.lastTargetSeenMs = now;
                pose.state = PetState.ATTACK;
            } else {
                pose.state = PetState.FOLLOW;
                pose.targetEntityId = -1;
                pose.combatHoverPos = null;
            }

            // Layer 1: Anchor
            // - In FOLLOW: pure world-space free flight around the player (never depends on player view/body yaw)
            // - In ENGAGE/ATTACK: stands still at combatHoverPos (only repositions if master walks > 6.0m away while not mid-shot)
            if (pose.state == PetState.FOLLOW || targetPos == null) {
                pose.combatHoverPos = null;
                pose.orbitAngle = (float) ((pose.orbitAngle + FOLLOW_OMEGA * dt) % (Math.PI * 2.0D));
                double dynRadius = FOLLOW_ORBIT_RADIUS
                        + Math.sin(pose.stateTime * 0.65D) * 0.55D
                        + Math.cos(pose.stateTime * 1.15D) * 0.25D;
                double bobY = Math.sin(pose.stateTime * FOLLOW_BOB_FREQ) * FOLLOW_BOB_AMP
                        + Math.cos(pose.stateTime * 0.85D) * 0.10D;
                pose.anchor = ownerWorld.add(
                        Math.cos(pose.orbitAngle) * dynRadius,
                        entity.getBbHeight() + FOLLOW_HEIGHT_OFFSET + bobY,
                        Math.sin(pose.orbitAngle) * dynRadius
                );
            } else {
                if (pose.combatHoverPos == null) {
                    pose.combatHoverPos = pose.pos;
                }
                if (pose.state == PetState.ATTACK) {
                    // Lock 100% stationary at current position while firing lightning
                    pose.combatHoverPos = pose.pos;
                    pose.anchor = pose.pos;
                } else {
                    double distToOwner = pose.combatHoverPos.distanceTo(ownerWorld);
                    double distToTarget = pose.combatHoverPos.distanceTo(targetPos);
                    if (distToOwner > 6.0D || distToTarget > 18.0D) {
                        Vec3 toTgtHoriz = new Vec3(targetPos.x - ownerWorld.x, 0.0D, targetPos.z - ownerWorld.z);
                        Vec3 dirToTgt = toTgtHoriz.lengthSqr() > 1.0E-4D ? toTgtHoriz.normalize() : Vec3.ZERO;
                        Vec3 desiredVantage = ownerWorld.add(dirToTgt.scale(1.65D))
                                .add(0.0D, entity.getBbHeight() + FOLLOW_HEIGHT_OFFSET, 0.0D);
                        if (pose.pos.distanceTo(desiredVantage) <= 0.85D) {
                            pose.combatHoverPos = pose.pos;
                        } else {
                            pose.combatHoverPos = desiredVantage;
                        }
                    }
                    pose.anchor = pose.combatHoverPos;
                }
            }

            // Layer 2: Movement
            Vec3 toAnchor = pose.anchor.subtract(pose.pos);
            double dist = toAnchor.length();
            if (dist > TELEPORT_DIST || pose.pos.distanceTo(ownerWorld) > TELEPORT_DIST + 4.0D) {
                pose.prevPos = pose.anchor;
                pose.pos = pose.anchor;
                pose.vel = Vec3.ZERO;
                if (pose.state != PetState.FOLLOW) {
                    pose.combatHoverPos = pose.pos;
                }
            } else if (pose.state != PetState.FOLLOW && (pose.state == PetState.ATTACK || dist <= 0.25D)) {
                // Stationary hover in combat
                pose.vel = Vec3.ZERO;
                pose.prevPos = pose.pos;
                pose.combatHoverPos = pose.pos;
                pose.anchor = pose.pos;
            } else {
                double maxSpeed = (pose.state == PetState.FOLLOW) ? MAX_SPEED_FOLLOW : MAX_SPEED_COMBAT;
                double speed = Math.min(maxSpeed, dist * ARRIVAL_K);
                Vec3 desired = dist > 1.0E-4D ? toAnchor.scale(speed / dist) : Vec3.ZERO;
                Vec3 steer = desired.subtract(pose.vel);
                double maxDv = MAX_ACCEL * dt;
                double steerLen = steer.length();
                if (steerLen > maxDv && steerLen > 1.0E-6D) {
                    steer = steer.scale(maxDv / steerLen);
                }
                pose.vel = pose.vel.add(steer).scale(Math.exp(-DRAG * dt));
                double vLen = pose.vel.length();
                if (vLen > maxSpeed && vLen > 1.0E-6D) {
                    pose.vel = pose.vel.scale(maxSpeed / vLen);
                }
                pose.prevPos = pose.pos;
                pose.pos = pose.pos.add(pose.vel.scale(dt));
            }

            // Layer 3: Decoupled orientation & continuous time-driven animation
            float targetYaw;
            float targetPitch;
            double horizSpeed = Math.hypot(pose.vel.x, pose.vel.z);
            if (pose.state != PetState.FOLLOW && targetPos != null) {
                Vec3 aimPt = (attacking && data.dragonfirePetAttackTarget != null)
                        ? data.dragonfirePetAttackTarget
                        : targetPos.add(0.0D, targetHeight * 0.55D, 0.0D);
                Vec3 toTgt = aimPt.subtract(pose.pos);
                double hLen = Math.hypot(toTgt.x, toTgt.z);
                double totalDist = Math.max(0.65D, toTgt.length());
                double mouthPitchCorrDeg = Math.toDegrees(Math.atan2(0.0745D, totalDist));
                targetYaw = (float) Math.toDegrees(Math.atan2(-toTgt.x, toTgt.z));
                targetPitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(-toTgt.y, Math.max(1.0E-4D, hLen))) - mouthPitchCorrDeg, -55.0D, 55.0D);
            } else if (horizSpeed > 0.15D) {
                targetYaw = (float) Math.toDegrees(Math.atan2(-pose.vel.x, pose.vel.z));
                targetPitch = (float) Mth.clamp(-pose.vel.y * 5.0D, -35.0D, 35.0D);
            } else {
                double tx = -Math.sin(pose.orbitAngle);
                double tz = Math.cos(pose.orbitAngle);
                targetYaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
                targetPitch = 0.0F;
            }

            float turnRate = (pose.state == PetState.ATTACK)
                    ? 1800.0F
                    : ((pose.state == PetState.ENGAGE) ? COMBAT_TURN_DEG_PER_SEC : MAX_TURN_DEG_PER_SEC);
            float maxTurn = turnRate * dt;
            float nextYaw = approachAngle(pose.yaw, targetYaw, maxTurn);
            float yawRate = Mth.wrapDegrees(nextYaw - pose.yaw) / Math.max(0.001F, dt);
            pose.prevYaw = pose.yaw;
            pose.yaw = nextYaw;
            pose.prevPitch = pose.pitch;
            float pitchSpeed = (pose.state == PetState.ATTACK) ? 1400.0F : ((pose.state == PetState.ENGAGE) ? 540.0F : 220.0F);
            pose.pitch = approachAngle(pose.pitch, targetPitch, pitchSpeed * dt);

            float targetRoll = (pose.state != PetState.FOLLOW) ? 0.0F : Mth.clamp(-yawRate * 0.12F, -30.0F, 30.0F);
            float rollAlpha = 1.0F - (float) Math.exp(((pose.state != PetState.FOLLOW) ? -16.0D : -8.0D) * dt);
            pose.prevRoll = pose.roll;
            pose.roll = Mth.lerp(rollAlpha, pose.roll, targetRoll);

            double speedNorm = Mth.clamp(pose.vel.length() / MAX_SPEED_COMBAT, 0.0D, 1.0D);
            float animRate = (float) Mth.lerp(speedNorm, 0.90D, 1.45D);
            pose.flapTicks += dt * 20.0F * animRate;
        }

        float jawOpenDeg = (pose.state == PetState.ATTACK)
                ? 36.0F
                : ((pose.state == PetState.ENGAGE) ? 16.0F
                : (8.0F + Mth.sin(pose.flapTicks * 0.25F) * 4.0F));
        Vec3 rel = pose.pos.subtract(ownerWorld);

        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y, rel.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - pose.yaw));
        renderStylizedEnderDragonModel(
                poseStack, buffer, 0.50F, 0.085F,
                pose.pitch, pose.roll, 0.0F, 0.0F, 0.0F,
                0.0F, jawOpenDeg, pose.flapTicks, 1.0F
        );
        poseStack.popPose();

        Vec3 resolvedMouth = computeExactMouthWorldPos(
                pose, 0.50F, 0.085F,
                pose.pitch, pose.roll, 0.0F, 0.0F, 0.0F,
                0.0F, jawOpenDeg, pose.flapTicks, 1.0F
        );
        pose.mouthWorldPos = resolvedMouth;
        data.dragonfirePetMouthWorldPos = resolvedMouth;
        int tagOwner = -10000 - entity.getId();
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId == tagOwner) {
                arc.start = resolvedMouth;
            }
        }
    }
}
