package com.velorise.staves.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Custom lightweight criterion trigger for Velorise's Staves EGT, construct, environment, and artifact milestones.
 */
public class VeloriseCriterionTrigger extends SimpleCriterionTrigger<VeloriseCriterionTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        if (player == null || event == null || event.isEmpty()) return;
        this.trigger(player, instance -> instance.matches(event));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event)
                ).apply(instance, TriggerInstance::new)
        );

        public boolean matches(String triggeredEvent) {
            return this.event.equals(triggeredEvent);
        }
    }
}
