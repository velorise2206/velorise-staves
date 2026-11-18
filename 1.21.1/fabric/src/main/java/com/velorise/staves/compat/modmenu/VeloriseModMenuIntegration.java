package com.velorise.staves.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.velorise.staves.client.VeloriseConfigScreen;

public class VeloriseModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VeloriseConfigScreen::new;
    }
}
