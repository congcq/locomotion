package com.trainguy9512.locomotion.config;

import com.trainguy9512.locomotion.LocomotionMain;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;

public class LocomotionConfigScreen {
    public static Screen createConfigScreen(Screen parentScreen) {

        LocomotionConfig config = LocomotionMain.CONFIG;
        config.load();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("locomotion.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("locomotion.config.category.general.name"))
                        .tooltip(Component.translatable("locomotion.config.category.general.tooltip"))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("locomotion.config.category.first_person_player.name"))
                        .tooltip(Component.translatable("locomotion.config.category.first_person_player.tooltip"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("locomotion.config.option.enable_first_person_renderer.name"))
                                .description(OptionDescription.createBuilder()
                                        .text(Component.translatable("locomotion.config.option.enable_first_person_renderer.description"))
                                        .build())
                                .binding(true, () -> config.data().firstPersonPlayer.enableRenderer, newValue -> config.data().firstPersonPlayer.enableRenderer = newValue)
                                .controller(option -> BooleanControllerBuilder.create(option)
                                        .formatValue(state -> state ? Component.translatable("addServer.resourcePack.enabled") : Component.translatable("addServer.resourcePack.disabled"))
                                        .coloured(true))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("locomotion.config.group.first_person_arm_camera_damping.name"))
                                .description(OptionDescription.createBuilder()
                                        .text(Component.translatable("locomotion.config.group.first_person_arm_camera_damping.description"))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("locomotion.config.option.enable_first_person_arm_camera_damping.name"))
                                        .description(OptionDescription.createBuilder()
                                                .text(Component.translatable("locomotion.config.option.enable_first_person_arm_camera_damping.description"))
                                                .build())
                                        .binding(true, () -> config.data().firstPersonPlayer.enableCameraRotationDamping, newValue -> config.data().firstPersonPlayer.enableCameraRotationDamping = newValue)
                                        .controller(TickBoxControllerBuilderImpl::new)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("locomotion.config.option.first_person_arm_camera_stiffness_factor.name"))
                                        .description(OptionDescription.createBuilder()
                                                .text(Component.translatable("locomotion.config.option.first_person_arm_camera_stiffness_factor.description"))
                                                .build())
                                        .binding(0.3f, () -> config.data().firstPersonPlayer.cameraRotationStiffnessFactor, newValue -> config.data().firstPersonPlayer.cameraRotationStiffnessFactor = newValue)
                                        .controller(option -> FloatSliderControllerBuilder.create(option)
                                                .formatValue(value -> Component.literal(new DecimalFormat("0.00").format(value)))
                                                .range(0.01f, 1.00f)
                                                .step(0.01f))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("locomotion.config.option.first_person_arm_camera_damping_factor.name"))
                                        .description(OptionDescription.createBuilder()
                                                .text(Component.translatable("locomotion.config.option.first_person_arm_camera_damping_factor.description"))
                                                .build())
                                        .binding(0.7f, () -> config.data().firstPersonPlayer.cameraRotationDampingFactor, newValue -> config.data().firstPersonPlayer.cameraRotationDampingFactor = newValue)
                                        .controller(option -> FloatSliderControllerBuilder.create(option)
                                                .formatValue(value -> Component.literal(new DecimalFormat("0.00").format(value)))
                                                .range(0.01f, 1.00f)
                                                .step(0.01f))
                                        .build())
                                .build())
                        .build())
                .save(config::save)
                .build()
                .generateScreen(parentScreen);
    }
}
