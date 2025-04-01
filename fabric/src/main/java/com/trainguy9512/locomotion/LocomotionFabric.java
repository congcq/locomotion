package com.trainguy9512.locomotion;

import com.google.gson.JsonElement;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceDataLoader;
import net.fabricmc.api.ModInitializer;
import com.trainguy9512.locomotion.LocomotionMain;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LocomotionFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LocomotionMain.initialize();
        this.registerResourceReloader();
    }
    
    private void registerResourceReloader() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleResourceReloadListener<Map<ResourceLocation, JsonElement>>() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "animation_sequence_loader");
            }

            @Override
            public CompletableFuture<Map<ResourceLocation, JsonElement>> load(ResourceManager resourceManager, Executor executor) {
                return AnimationSequenceDataLoader.load(resourceManager, executor);
            }

            @Override
            public CompletableFuture<Void> apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, Executor executor) {
                return AnimationSequenceDataLoader.apply(resourceLocationJsonElementMap, resourceManager, executor);
            }
        });
    }
}