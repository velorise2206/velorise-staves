package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.GaugeState;
import com.velorise.staves.entity.GeoFangPillarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.Map;
import java.util.UUID;

public enum AuraServerDataProvider implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = new ResourceLocation(VeloriseStavesMod.MODID, "elemental_aura");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (entity instanceof GeoFangPillarEntity pillar) {
            GeoConstructJadeHelper.appendServerData(tag, pillar);
            return;
        }
        if (!(entity instanceof LivingEntity living)) return;
        UUID id = living.getUUID();
        if (ElementalGaugeManager.isOmniInnate(living)) {
            tag.putBoolean("vs_omni", true);
        }
        java.util.EnumSet<Element> innates = ElementalGaugeManager.getInnateElements(living);
        if (!innates.isEmpty()) {
            Element first = innates.iterator().next();
            tag.putString("vs_innate", first.name());
            byte innateMask = 0;
            for (Element e : innates) {
                innateMask |= (byte) (1 << e.ordinal());
            }
            tag.putByte("vs_innate_mask", innateMask);
        }
        CompoundTag gaugesTag = new CompoundTag();
        Map<Element, GaugeState> gauges = ElementalGaugeManager.getEntityGauges(id);
        if (gauges != null) {
            for (Map.Entry<Element, GaugeState> entry : gauges.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getUnits() > 0.001F) {
                    gaugesTag.putFloat(entry.getKey().name(), entry.getValue().getUnits());
                }
            }
        }
        GaugeState quicken = ElementalGaugeManager.getQuickenGauge(id);
        if (quicken != null && quicken.getUnits() > 0.001F) {
            gaugesTag.putFloat("QUICKEN", quicken.getUnits());
        }
        GaugeState frozen = ElementalGaugeManager.getFrozenGauge(id);
        if (frozen != null && frozen.getUnits() > 0.001F) {
            gaugesTag.putFloat("FROZEN", frozen.getUnits());
        }
        if (!gaugesTag.isEmpty()) {
            tag.put("vs_gauges", gaugesTag);
        }
        if (!ElementalGaugeManager.isOmniInnate(living)
                && !ElementalGaugeManager.isPassiveAquaticOrAmbient(living)
                && ElementalGaugeManager.isEntityInWater(living)) {
            tag.putBoolean("vs_env_wet", true);
        }
    }
}
