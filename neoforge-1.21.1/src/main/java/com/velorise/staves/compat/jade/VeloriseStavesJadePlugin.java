package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.GeoFangPillarEntity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(VeloriseStavesMod.MODID)
public final class VeloriseStavesJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(AuraServerDataProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityDataProvider(AuraServerDataProvider.INSTANCE, GeoFangPillarEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(AuraComponentProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(GeoConstructJadeHelper.INSTANCE, GeoFangPillarEntity.class);
    }
}
