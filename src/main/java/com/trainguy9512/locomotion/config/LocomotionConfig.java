package com.trainguy9512.locomotion.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trainguy9512.locomotion.LocomotionMain;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocomotionConfig {

    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve(LocomotionMain.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    private Data configData;

    public LocomotionConfig() {
        this.configData = new Data();
    }

    public Data data() {
        return configData;
    }

    public void load() {
        if (Files.exists(CONFIG_FILE_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_FILE_PATH.toFile())) {
                this.configData = GSON.fromJson(reader, LocomotionConfig.Data.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load locomotion config file", e);
            }
        } else {
            configData = new LocomotionConfig.Data();
        }
        save();

    }

    public void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE_PATH)) {
            writer.write(GSON.toJson(this.configData));
        } catch (Exception e) {
            LocomotionMain.LOGGER.error("Failed to write config to path {}", CONFIG_FILE_PATH.toAbsolutePath());
        }
    }


    public static class Data {

        public final FirstPersonPlayerSettings firstPersonPlayerSettings = new FirstPersonPlayerSettings();

        public TestType type = TestType.ONE_TEST;

        public enum TestType {
            ONE_TEST,
            TWO_TEST

        }

        public static class FirstPersonPlayerSettings {
            public boolean useLocomotionFirstPersonRenderer = true;
        }
    }
}
