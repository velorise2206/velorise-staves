package com.velorise.staves.mixin;

import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    ordinal = 0
            )
    )
    private void velorise$onExplosionDetonate(CallbackInfo ci) {
        Explosion self = (Explosion) (Object) this;
        if (this.level instanceof ServerLevel sl
                && self.getDirectSourceEntity() instanceof Creeper creeper) {
            ElementalGaugeManager.handleCreeperExplosionBurst(sl, creeper, self.center(), self.radius());
        }
    }
}
