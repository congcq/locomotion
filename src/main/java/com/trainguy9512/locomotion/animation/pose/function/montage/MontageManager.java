package com.trainguy9512.locomotion.animation.pose.function.montage;

import com.trainguy9512.locomotion.animation.data.AnimationSequenceData;
import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;

import java.util.ArrayList;
import java.util.List;

public class MontageManager {

    private final List<MontageInstance> montageStack;

    public MontageManager() {
        this.montageStack = new ArrayList<>();
    }

    public void tick() {
        this.montageStack.forEach(MontageInstance::tick);
        this.montageStack.removeIf(montageInstance -> montageInstance.weight.getPreviousValue() == 0 && montageInstance.weight.getCurrentValue() == 0);
    }

    public static MontageManager of() {
        return new MontageManager();
    }

    public void playMontage(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
        this.montageStack.addLast(MontageInstance.of(configuration, driverContainer));
    }

    public boolean isAnythingPlayingInSlot(String slot) {
        return false;
    }

    private static class MontageInstance {
        private final VariableDriver<Float> weight;
        private final VariableDriver<Float> ticksElapsed;
        private final MontageConfiguration configuration;

        private final float playRate;
        private final float tickLength;

        private MontageInstance(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
            this.weight = VariableDriver.ofFloat(() -> 0f);
            this.ticksElapsed = VariableDriver.ofFloat(() -> 0f);
            this.configuration = configuration;

            this.playRate = configuration.playRateFunction().apply(driverContainer);
            this.tickLength = AnimationSequenceData.INSTANCE.getOrThrow(configuration.animationSequence()).length().inTicks();
            this.tick();
        }

        private static MontageInstance of(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
            return new MontageInstance(configuration, driverContainer);
        }

        private void tick() {
            this.ticksElapsed.pushCurrentToPrevious();
            this.ticksElapsed.modifyValue(currentValue -> currentValue + this.playRate);
        }

        private float getWeight(float ticksElapsed){
            float offsetEndTransitionStartTime = this.tickLength - this.configuration.transitionOut().duration().inTicks();
            if(this.ticksElapsed.getCurrentValue() < this.transitionInDuration){
                return this.transitionInEasing.ease(timeElapsed / this.transitionInDuration);
            } else if(timeElapsed > offsetEndTransitionStartTime){
                return this.transitionOutEasing.ease(1 - ((timeElapsed - offsetEndTransitionStartTime) / this.transitionOutDuration));
            }
            return 1;
        }
    }
}
