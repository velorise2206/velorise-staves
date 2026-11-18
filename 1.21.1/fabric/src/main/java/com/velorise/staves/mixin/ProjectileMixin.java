package com.velorise.staves.mixin;

import com.velorise.staves.event.ArcaneStaffEvents;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void velorise$onTick(CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if (!projectile.isRemoved()) {
            ArcaneStaffEvents.onProjectileTick(projectile);
        }
    }

    @Inject(method = "hitTargetOrDeflectSelf", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"), cancellable = true)
    private void velorise$onHitTargetOrDeflect(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Projectile projectile = (Projectile) (Object) this;
        if (ArcaneStaffEvents.onProjectileHit(projectile, hitResult)) {
            cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void velorise$onHit(HitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if (projectile.isRemoved()) {
            ci.cancel();
            return;
        }
        if (ArcaneStaffEvents.onProjectileHit(projectile, hitResult)) {
            ci.cancel();
        }
    }
}
