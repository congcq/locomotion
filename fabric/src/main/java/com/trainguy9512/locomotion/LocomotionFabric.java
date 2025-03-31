package com.trainguy9512.locomotion;

import net.fabricmc.api.ModInitializer;
import com.trainguy9512.locomotion.LocomotionMain;

public class LocomotionFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LocomotionMain.initialize();
    }
}