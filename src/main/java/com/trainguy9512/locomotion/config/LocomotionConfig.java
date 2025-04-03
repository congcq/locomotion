package com.trainguy9512.locomotion.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocomotionConfig {

    public static ConfigClassHandler<Data> HANDLER = ConfigClassHandler.createBuilder(Data.class)
            .id(ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(LocomotionMain.MOD_ID + ".json"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting) // not needed, pretty print by default
                    .build())
            .build();

//    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve(LocomotionMain.MOD_ID + ".json");
//    private static final Gson GSON = new GsonBuilder()
//            .setPrettyPrinting()
//            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//            .excludeFieldsWithModifiers(Modifier.PRIVATE)
//            .create();

    private Data configData;

    public LocomotionConfig() {
        this.configData = new Data();
    }

    public Data data() {
        return configData;
    }

    public void load() {
        HANDLER.load();
//        if (Files.exists(CONFIG_FILE_PATH)) {
//            try (FileReader reader = new FileReader(CONFIG_FILE_PATH.toFile())) {
//                this.configData = GSON.fromJson(reader, LocomotionConfig.Data.class);
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to load locomotion config file", e);
//            }
//        } else {
//            configData = new LocomotionConfig.Data();
//        }
//        save();
    }

    public void save() {
        HANDLER.save();
//        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE_PATH)) {
//            writer.write(GSON.toJson(this.configData));
//        } catch (Exception e) {
//            LocomotionMain.LOGGER.error("Failed to write config to path {}", CONFIG_FILE_PATH.toAbsolutePath());
//        }
//        JointAnimatorDispatcher.getInstance().reInitializeData();
    }


    public static class Data {

        @SerialEntry
        public final FirstPersonPlayer firstPersonPlayer = new FirstPersonPlayer();

        @SerialEntry
        public TestType type = TestType.ONE_TEST;

        public enum TestType {
            ONE_TEST,
            TWO_TEST

        }

        public static class FirstPersonPlayer {

            public boolean enableRenderer = true;

            public boolean enableCameraRotationDamping = true;

            public float cameraRotationStiffnessFactor = 0.3f;

            public float cameraRotationDampingFactor = 0.7f;
        }
    }
}
