package com.trainguy9512.locomotion.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.trainguy9512.locomotion.LocomotionMain;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LocomotionMain.CONFIG.getConfigScreen(FabricLoader.getInstance()::isModLoaded)::apply;
    }
}
