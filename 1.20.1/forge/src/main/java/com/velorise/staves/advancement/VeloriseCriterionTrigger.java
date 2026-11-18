package com.velorise.staves.advancement;

import com.google.gson.JsonObject;
import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * Custom lightweight criterion trigger for Velorise's Staves EGT, construct, environment, and artifact milestones (1.20.1).
 */
public class VeloriseCriterionTrigger extends SimpleCriterionTrigger<VeloriseCriterionTrigger.TriggerInstance> {
    public static final ResourceLocation ID = new ResourceLocation(VeloriseStavesMod.MODID, "advancement_event");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        String event = GsonHelper.getAsString(json, "event", "");
        return new TriggerInstance(player, event);
    }

    public void trigger(ServerPlayer player, String event) {
        if (player == null || event == null || event.isEmpty()) return;
        this.trigger(player, instance -> instance.matches(event));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String event;

        public TriggerInstance(ContextAwarePredicate player, String event) {
            super(ID, player);
            this.event = event;
        }

        public boolean matches(String triggeredEvent) {
            return this.event.equals(triggeredEvent);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("event", this.event);
            return json;
        }
    }
}
