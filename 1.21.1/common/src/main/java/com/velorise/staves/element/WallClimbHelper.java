package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

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
    @Nullable
    public static Direction findClimbWall(Player player, double probeDist) {
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
            if (!chestValid && !shoulderValid) {
                continue;
            }

            // Score by alignment with player's horizontal look direction so facing walls win cleanly
            double dot = inward.getStepX() * lookX + inward.getStepZ() * lookZ;
            if (chestValid && shoulderValid) {
                dot += 0.25D;
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
        if (player == null || wallNormalFace == null) return false;
        Level level = player.level();
        Direction inward = wallNormalFace.getOpposite();
        AABB bb = player.getBoundingBox();
        double halfW = Math.max(0.28D, (bb.maxX - bb.minX) * 0.5D);
        double sampleX = player.getX() + inward.getStepX() * (halfW + CLIENT_WALL_PROBE_DIST);
        double sampleZ = player.getZ() + inward.getStepZ() * (halfW + CLIENT_WALL_PROBE_DIST);

        BlockPos chestWall = BlockPos.containing(sampleX, player.getY() + 0.78D, sampleZ);
        if (!isSturdyClimbableFace(level, chestWall, wallNormalFace)) {
            return false;
        }
        BlockPos topClear1 = chestWall.above();
        BlockPos topClear2 = chestWall.above(2);
        BlockPos eyeClear = BlockPos.containing(sampleX, player.getY() + 1.68D, sampleZ);

        return level.getBlockState(topClear1).getCollisionShape(level, topClear1).isEmpty()
                && level.getBlockState(topClear2).getCollisionShape(level, topClear2).isEmpty()
                && level.getBlockState(eyeClear).getCollisionShape(level, eyeClear).isEmpty();
    }

    /**
     * Converts an outward wall normal (wall -> player) to the yaw facing directly into the wall.
     */
    public static float getFacingWallYaw(Direction wallNormalFace) {
        if (wallNormalFace == null) return 0.0F;
        return wallNormalFace.getOpposite().toYRot();
    }
}
