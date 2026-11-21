package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


/**
 * Shared geometric wall-surface filter for authentic Genshin-style climbing.
 * Filters out fences, thin panes, open doors, torches, cobwebs, and vanilla climbables,
 * while detecting overhangs and ledge-mantle tops cleanly across all loaders.
 */
public final class WallClimbHelper {
    public static final double CLIENT_WALL_PROBE_DIST = 0.45D;
    public static final double SERVER_WALL_TOLERANCE = 0.72D;

    private static final Direction[] HORIZONTAL_DIRS = new Direction[] {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private WallClimbHelper() {}

    /**
     * Finds the best climbable wall face near the player, returning the outward surface normal
     * (pointing from the wall toward the player). Returns null if no sturdy wall is adjacent.
     */
    public static Direction findClimbWall(Player player, double probeDist) {
        return findClimbWall(player, probeDist, null);
    }

    public static Direction findClimbWall(Player player, double probeDist, Direction preferredNormal) {
        if (player == null) return null;
        Level level = player.level();
        Vec3 look = player.getLookAngle();
        double lookX = look.x;
        double lookZ = look.z;
        double lookLen = Math.hypot(lookX, lookZ);
        if (lookLen > 1.0E-4D) {
            lookX /= lookLen;
            lookZ /= lookLen;
        }

        AABB bb = player.getBoundingBox();
        double halfW = Math.max(0.28D, (bb.maxX - bb.minX) * 0.5D);
        double chestY = player.getY() + 0.85D;
        double shoulderY = player.getY() + 1.40D;

        Direction bestNormal = null;
        double bestScore = -2.0D;

        for (Direction inward : HORIZONTAL_DIRS) {
            // inward points from player -> wall; normalFace points from wall -> player
            Direction normalFace = inward.getOpposite();
            double sampleX = player.getX() + inward.getStepX() * (halfW + probeDist);
            double sampleZ = player.getZ() + inward.getStepZ() * (halfW + probeDist);

            BlockPos chestPos = BlockPos.containing(sampleX, chestY, sampleZ);
            BlockPos shoulderPos = BlockPos.containing(sampleX, shoulderY, sampleZ);
            boolean chestValid = isSturdyClimbableFace(level, chestPos, normalFace);
            boolean shoulderValid = isSturdyClimbableFace(level, shoulderPos, normalFace);
            if (player.onGround()) {
                if (!chestValid || !shoulderValid) continue;
            } else if (!chestValid && !shoulderValid) {
                continue;
            }

            // Score by alignment with player's horizontal look direction so facing walls win cleanly
            double dot = inward.getStepX() * lookX + inward.getStepZ() * lookZ;
            if (chestValid && shoulderValid) {
                dot += 0.25D;
            }
            if (preferredNormal != null && normalFace == preferredNormal) {
                dot += 0.55D;
            }
            if (dot > bestScore) {
                bestScore = dot;
                bestNormal = normalFace;
            }
        }
        return bestNormal;
    }

    public static boolean isSturdyClimbableFace(Level level, BlockPos pos, Direction outwardNormal) {
        if (level == null || pos == null || outwardNormal == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(BlockTags.CLIMBABLE)) return false;
        if (state.getCollisionShape(level, pos).isEmpty()) return false;
        return state.isFaceSturdy(level, pos, outwardNormal);
    }

    /**
     * Returns true if a solid overhang blocks upward climbing right above the player's head.
     */
    public static boolean hasOverhangAbove(Player player) {
        if (player == null) return false;
        Level level = player.level();
        BlockPos aboveHead = BlockPos.containing(player.getX(), player.getY() + 1.98D, player.getZ());
        return !level.getBlockState(aboveHead).getCollisionShape(level, aboveHead).isEmpty();
    }

    /**
     * Returns true when the player's chest still faces a sturdy wall block,
     * while eye-level and the 2 blocks above the wall edge are clear to mantle onto.
     */
    public static boolean canLedgeMantle(Player player, Direction wallNormalFace) {
        return !Double.isNaN(findLedgeTopY(player, wallNormalFace));
    }

    /**
     * Returns the exact top surface Y coordinate of a climbable ledge when the player's
     * feet are within 1.05 blocks of the top lip and 2 full blocks above the ledge are clear.
     * Returns Double.NaN if not ready to mantle.
     */
    public static double findLedgeTopY(Player player, Direction wallNormalFace) {
        if (player == null || wallNormalFace == null) return Double.NaN;
        Level level = player.level();
        Direction inward = wallNormalFace.getOpposite();
        AABB bb = player.getBoundingBox();
        double halfW = Math.max(0.28D, (bb.maxX - bb.minX) * 0.5D);
        double sampleX = player.getX() + inward.getStepX() * (halfW + CLIENT_WALL_PROBE_DIST);
        double sampleZ = player.getZ() + inward.getStepZ() * (halfW + CLIENT_WALL_PROBE_DIST);

        // Probe chest/shoulder/waist (y+1.35 -> y+0.95 -> y+0.55) so mantle triggers as soon as raised hands grip the top lip
        double[] probeHeights = new double[] { 1.35D, 0.95D, 0.55D, 0.15D };
        BlockPos ledgeBlock = null;
        for (double py : probeHeights) {
            BlockPos candidate = BlockPos.containing(sampleX, player.getY() + py, sampleZ);
            if (isSturdyClimbableFace(level, candidate, wallNormalFace)) {
                ledgeBlock = candidate;
                break;
            }
        }
        if (ledgeBlock == null) return Double.NaN;

        double topSurfaceY = ledgeBlock.getY() + 1.0D;
        // Trigger as soon as raised hands (~y+1.75) reach the top lip (feet within 1.65 blocks of top surface)
        if (player.getY() < topSurfaceY - 1.65D) {
            return Double.NaN;
        }
        BlockPos topClear1 = ledgeBlock.above();
        BlockPos topClear2 = ledgeBlock.above(2);
        // Also ensure headroom directly above player is clear to rise up to topSurfaceY
        BlockPos headClear = BlockPos.containing(player.getX(), topSurfaceY + 1.65D, player.getZ());

        boolean clear = level.getBlockState(topClear1).getCollisionShape(level, topClear1).isEmpty()
                && level.getBlockState(topClear2).getCollisionShape(level, topClear2).isEmpty()
                && level.getBlockState(headClear).getCollisionShape(level, headClear).isEmpty();
        return clear ? topSurfaceY : Double.NaN;
    }

    public record LowStepTarget(Direction wallNormalFace, double topSurfaceY) {}

    /**
     * Genshin-style low-obstacle auto-mantle detector (0 stamina):
     * Detects ANY solid obstacle taller than Vanilla's 0.60m auto-step and up to 1.15m
     * (full 1-block Dirt/Stone, Anvil, Chest, Enchanting Table, Cauldron, Lectern, etc.)
     * while cleanly skipping half-slabs/stairs (<= 0.60m) and fences/walls (1.50m).
     */
    public static LowStepTarget findLowStepMantle(Player player) {
        if (player == null || !player.onGround()) return null;
        Level level = player.level();
        Vec3 look = player.getLookAngle();
        double lookLen = Math.hypot(look.x, look.z);
        if (lookLen < 1.0E-4D) return null;
        double lookX = look.x / lookLen;
        double lookZ = look.z / lookLen;

        AABB bb = player.getBoundingBox();
        double halfW = Math.max(0.28D, (bb.maxX - bb.minX) * 0.5D);
        LowStepTarget best = null;
        double bestDot = 0.72D;

        for (Direction inward : HORIZONTAL_DIRS) {
            double dot = inward.getStepX() * lookX + inward.getStepZ() * lookZ;
            if (dot <= bestDot) continue;
            Direction normalFace = inward.getOpposite();
            double sampleX = player.getX() + inward.getStepX() * (halfW + 0.24D);
            double sampleZ = player.getZ() + inward.getStepZ() * (halfW + 0.24D);
            BlockPos lowBlock = BlockPos.containing(sampleX, player.getY() + 0.55D, sampleZ);
            BlockState state = level.getBlockState(lowBlock);
            if (state.isAir() || state.is(BlockTags.CLIMBABLE)) continue;
            var shape = state.getCollisionShape(level, lowBlock);
            if (shape.isEmpty()) continue;

            double topSurfaceY = lowBlock.getY() + shape.max(Direction.Axis.Y);
            double dy = topSurfaceY - player.getY();
            // Strictly taller than vanilla 0.60m step-up and <= 1.15m (excludes half-slabs 0.5m & fences 1.5m)
            if (dy <= 0.60D || dy > 1.15D) continue;

            // Verify destination standing box on top of the obstacle (and headroom above current pos) is completely clear
            AABB destBox = bb.move(inward.getStepX() * 0.72D, dy + 0.04D, inward.getStepZ() * 0.72D);
            BlockPos headClear = BlockPos.containing(player.getX(), topSurfaceY + 1.65D, player.getZ());
            if (level.noCollision(player, destBox)
                    && level.getBlockState(headClear).getCollisionShape(level, headClear).isEmpty()) {
                bestDot = dot;
                best = new LowStepTarget(normalFace, topSurfaceY);
            }
        }
        return best;
    }

    /**
     * Converts an outward wall normal (wall -> player) to the yaw facing directly into the wall.
     */
    public static float getFacingWallYaw(Direction wallNormalFace) {
        if (wallNormalFace == null) return 0.0F;
        return wallNormalFace.getOpposite().toYRot();
    }
}
