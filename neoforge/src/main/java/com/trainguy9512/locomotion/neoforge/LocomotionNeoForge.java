package com.trainguy9512.locomotion.neoforge;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceDataLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LocomotionMain.MOD_ID, dist = Dist.CLIENT)
public class LocomotionNeoForge {

    public LocomotionNeoForge(ModContainer modContainer, IEventBus modEventBus) {
        modEventBus.addListener(this::onClientInitialize);
        modEventBus.addListener(this::onResourceReload);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> LocomotionMain.CONFIG.getConfigScreen(ModList.get()::isLoaded).apply(parent));
    }

    public void onClientInitialize(FMLClientSetupEvent event) {
        LocomotionMain.initialize();

    }

    public void onResourceReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "animation_sequence_loader"), AnimationSequenceDataLoader::reload);
    }

}
