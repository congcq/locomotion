package com.trainguy9512.locomotion.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.trainguy9512.locomotion.LocomotionMain;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LocomotionMain.CONFIG.getConfigScreen()::apply;
    }
}
