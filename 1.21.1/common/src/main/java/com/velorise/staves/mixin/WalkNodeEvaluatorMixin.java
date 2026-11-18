package com.velorise.staves.mixin;

import com.velorise.staves.element.MobHazardAvoidanceManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @Inject(
            method = "getPathTypeOfMob(Lnet/minecraft/world/level/pathfinder/PathfindingContext;IIILnet/minecraft/world/entity/Mob;)Lnet/minecraft/world/level/pathfinder/PathType;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void velorise$applyElementalHazardMalus(
            PathfindingContext context,
            int x,
            int y,
            int z,
            Mob mob,
            CallbackInfoReturnable<PathType> cir
    ) {
        PathType current = cir.getReturnValue();
        PathType adjusted = MobHazardAvoidanceManager.adjustPathTypeForMob(mob, x, y, z, current);
        if (adjusted != current) {
            cir.setReturnValue(adjusted);
        }
    }
}
