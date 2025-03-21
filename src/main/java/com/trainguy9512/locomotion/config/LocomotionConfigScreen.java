package com.trainguy9512.locomotion.config;

import com.trainguy9512.locomotion.LocomotionMain;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LocomotionConfigScreen {
    public static Screen createConfigScreen(Screen parentScreen) {

        LocomotionConfig config = LocomotionMain.CONFIG;
        config.load();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Test title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("First Person Player"))
                        .tooltip(Component.literal("This text will appear as a tooltip when you hover or focus the button with Tab. There is no need to add \n to wrap as YACL will do it for you."))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("First person player group"))
                                .description(OptionDescription.of(Component.literal("First person player options")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enable First Person Player Animations"))
                                        .description(OptionDescription.of(Component.literal("This text will appear as a tooltip when you hover over the option.")))
                                        .binding(true, () -> config.data().firstPersonPlayerSettings.useLocomotionFirstPersonRenderer, newValue -> config.data().firstPersonPlayerSettings.useLocomotionFirstPersonRenderer = newValue)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .save(config::save)
                .build()
                .generateScreen(parentScreen);
    }
}
