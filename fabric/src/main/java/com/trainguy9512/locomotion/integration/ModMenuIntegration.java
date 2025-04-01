package com.trainguy9512.locomotion.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.trainguy9512.locomotion.config.LocomotionConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return LocomotionConfigScreen::createConfigScreen;
        } else {
            return parent -> new AlertScreen(
                    () -> Minecraft.getInstance().setScreen(parent),
                    Component.translatable("locomotion.config.yacl_not_found.header"),
                    Component.translatable("locomotion.config.yacl_not_found.description"),
                    Component.translatable("locomotion.config.yacl_not_found.close"),
                    true
            );
        }
    }
}
