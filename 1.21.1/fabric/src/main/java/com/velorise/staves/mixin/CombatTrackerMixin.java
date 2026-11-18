package com.velorise.staves.mixin;

import com.velorise.staves.element.ElementalDeathMessageHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin {
    @Shadow
    @Final
    private List<CombatEntry> entries;

    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "getDeathMessage", at = @At("HEAD"))
    private void velorise$enhanceDeathMessage(CallbackInfoReturnable<Component> cir) {
        if (this.mob != null && !this.mob.level().isClientSide()) {
            ServerLevel sl = this.mob.level() instanceof ServerLevel serverLevel ? serverLevel : null;
            ElementalDeathMessageHelper.enhanceCombatTrackerEntries(sl, this.mob, this.entries);
        }
    }
}
