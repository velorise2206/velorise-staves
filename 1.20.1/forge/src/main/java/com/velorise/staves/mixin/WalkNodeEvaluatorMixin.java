package com.velorise.staves.mixin;

import com.velorise.staves.element.MobHazardAvoidanceManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @Inject(
            method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;IIILnet/minecraft/world/entity/Mob;)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void velorise$applyElementalHazardMalus(
            BlockGetter level,
            int x,
            int y,
            int z,
            Mob mob,
            CallbackInfoReturnable<BlockPathTypes> cir
    ) {
        BlockPathTypes current = cir.getReturnValue();
        BlockPathTypes adjusted = MobHazardAvoidanceManager.adjustPathTypeForMob(mob, x, y, z, current);
        if (adjusted != current) {
            cir.setReturnValue(adjusted);
        }
    }
}
