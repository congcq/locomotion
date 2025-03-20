package com.trainguy9512.locomotion.config;

import com.google.gson.GsonBuilder;
import com.trainguy9512.locomotion.LocomotionMain;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public class LocomotionConfig {

    public static final ConfigClassHandler<LocomotionConfig> HANDLER = ConfigClassHandler.createBuilder(LocomotionConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("locomotion.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public boolean useLocomotionFirstPersonRenderer = true;
}
