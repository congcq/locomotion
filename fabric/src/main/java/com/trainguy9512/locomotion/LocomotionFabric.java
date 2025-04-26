package com.trainguy9512.locomotion;

import com.google.gson.JsonElement;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceDataLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import com.trainguy9512.locomotion.LocomotionMain;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LocomotionFabric implements ClientModInitializer {
    
    private void registerResourceReloader() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public @NotNull CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return AnimationSequenceDataLoader.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }

            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "animation_sequence_loader");
            }
        });
    }

    @Override
    public void onInitializeClient() {
        LocomotionMain.initialize();
        this.registerResourceReloader();
    }
}