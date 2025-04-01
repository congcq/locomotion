package com.trainguy9512.locomotion.forge;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceDataLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(LocomotionMain.MOD_ID)
public class LocomotionForge {
    public LocomotionForge() {
        LocomotionMain.initialize();
    }
}
