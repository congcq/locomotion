package com.trainguy9512.locomotion.animation.driver;

import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;

import java.util.function.Consumer;

/**
 * Boolean driver that can be triggered to get a one-tick "pulse". When triggered, it will automatically be un-triggered after pose function evaluation.
 */
public class TriggerDriver implements Driver<Boolean> {

    private boolean triggered;
    private boolean triggerConsumed;

    private TriggerDriver() {
        this.triggered = false;
        this.triggerConsumed = false;
    }

    public static TriggerDriver of() {
        return new TriggerDriver();
    }

    public void trigger() {
        this.triggered = true;
        this.triggerConsumed = false;
    }

    /**
     * Runs a function if the driver has been triggered, and then resets the driver after pose function evaluation.
     * @param runnable          Function to run if triggered.
     */
    public void runIfTriggered(Runnable runnable) {
        if (this.triggered && !this.triggerConsumed) {
            runnable.run();
            this.triggerConsumed = true;
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public Boolean getValueInterpolated(float partialTicks) {
        return this.triggered;
    }

    @Override
    public void pushCurrentToPrevious() {

    }

    @Override
    public void postTick() {
        if (this.triggerConsumed) {
            this.triggered = false;
            this.triggerConsumed = false;
        }
    }

    @Override
    public String toString() {
        return this.triggered ? "Triggered!" : "Waiting...";
    }
}
