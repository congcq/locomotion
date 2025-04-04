package com.trainguy9512.locomotion.animation.pose.function.montage;

import com.trainguy9512.locomotion.animation.data.AnimationSequenceData;
import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.joint.JointSkeleton;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * Plays a montage of the given configuration.
     * @param configuration         Montage configuration to use as the template for the montage.
     * @param driverContainer       Driver container to use for getting the play rate.
     */
    public void playMontage(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
        for (MontageInstance instance : this.montageStack) {
            if (Objects.equals(instance.configuration.identifier(), configuration.identifier())) {
                if (instance.ticksElapsed.getCurrentValue() < configuration.cooldownDuration().inTicks()) {
                    return;
                }
            }
        }
        this.montageStack.addLast(MontageInstance.of(configuration, driverContainer));
    }

    /**
     * Immediately stops and removes any montages currently playing within the provided slot.
     * @param slot                  Slot identifier
     */
    public void interruptMontagesInSlot(String slot) {
        this.montageStack.removeIf(instance -> instance.configuration.slots().contains(slot));
    }

    /**
     * Returns whether a montage of the provided identifier is playing or not.
     * @param identifier            Montage configuration identifier
     */
    public boolean isMontagePlaying(String identifier) {
        for (MontageInstance montageInstance : this.montageStack) {
            if (montageInstance.configuration.identifier().equals(identifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether any montage is playing in the provided slot
     * @param slot                  Slot identifier
     */
    public boolean isAnythingPlayingInSlot(String slot) {
        for (MontageInstance montageInstance : this.montageStack) {
            if (montageInstance.configuration.slots().contains(slot)) {
                return true;
            }
        }
        return false;
    }

    public LocalSpacePose getLayeredSlotPose(LocalSpacePose basePose, String slot, JointSkeleton jointSkeleton, float partialTicks) {
        LocalSpacePose slotPose = LocalSpacePose.of(basePose);
        for (MontageInstance montageInstance : this.montageStack) {
            if (montageInstance.configuration.slots().contains(slot)) {
                slotPose = slotPose.interpolated(montageInstance.getPose(jointSkeleton, partialTicks), montageInstance.getInterpolatedWeight(partialTicks));
            }
        }
        return slotPose;
    }

    public boolean areAnyMontagesInSlotFullyOverriding(String slot) {
        for (MontageInstance montageInstance : this.montageStack) {
            if (montageInstance.configuration.slots().contains(slot)) {
                if (montageInstance.weight.getCurrentValue() == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class MontageInstance {
        private final VariableDriver<Float> weight;
        private final VariableDriver<Float> ticksElapsed;
        private final MontageConfiguration configuration;

        private final float playRate;
        private final float tickLength;

        private final ResourceLocation additiveBasePoseLocation;
        private LocalSpacePose additiveBasePose;
        private LocalSpacePose additiveSubtractionPose;

        private MontageInstance(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
            this.weight = VariableDriver.ofFloat(() -> 0f);
            this.ticksElapsed = VariableDriver.ofFloat(() -> configuration.startTimeOffset().inTicks());
            this.configuration = configuration;

            this.playRate = configuration.playRateFunction().apply(driverContainer);
            this.tickLength = AnimationSequenceData.INSTANCE.getOrThrow(configuration.animationSequence()).length().inTicks();

            if (configuration.isAdditive()) {
                this.additiveBasePoseLocation = configuration.additiveBasePoseProvider().apply(driverContainer);
            } else {
                this.additiveBasePoseLocation = null;
            }
            this.additiveBasePose = null;
            this.additiveSubtractionPose = null;
        }

        private static MontageInstance of(MontageConfiguration configuration, OnTickDriverContainer driverContainer) {
            return new MontageInstance(configuration, driverContainer);
        }

        private void tick() {
            this.ticksElapsed.pushCurrentToPrevious();
            this.ticksElapsed.modifyValue(currentValue -> currentValue + this.playRate);
            this.weight.pushCurrentToPrevious();
            this.weight.setValue(this.getWeight(this.ticksElapsed.getCurrentValue()));
        }

        private float getWeight(float ticksElapsed) {
            float offsetEndTransitionStartTime = this.tickLength - (this.configuration.transitionOut().duration().inTicks() * this.configuration.transitionOutCrossfadeWeight());
            float startTimeOffsetTicks = this.configuration.startTimeOffset().inTicks();

            if (ticksElapsed < this.configuration.transitionIn().duration().inTicks() + startTimeOffsetTicks) {
                return (ticksElapsed - startTimeOffsetTicks) / this.configuration.transitionIn().duration().inTicks();
            } else if (ticksElapsed > offsetEndTransitionStartTime) {
                return Math.max(1f - ((ticksElapsed - offsetEndTransitionStartTime) / this.configuration.transitionOut().duration().inTicks()), 0f);
            }
            return 1;
        }

        private LocalSpacePose getPose(JointSkeleton jointSkeleton, float partialTicks) {
            LocalSpacePose pose = LocalSpacePose.fromAnimationSequence(
                    jointSkeleton,
                    this.configuration.animationSequence(),
                    TimeSpan.ofTicks(this.ticksElapsed.getValueInterpolated(partialTicks)),
                    false
            );
            if (this.configuration.isAdditive()) {
                // If the additive base pose and the additive subtraction poses are null, initialize them (only initialized when needed.
                if (this.additiveBasePose == null) {
                    this.additiveBasePose = LocalSpacePose.fromAnimationSequence(
                            jointSkeleton,
                            this.additiveBasePoseLocation,
                            TimeSpan.ofTicks(0),
                            false
                    );
                }
                if (this.additiveSubtractionPose == null) {
                    this.additiveSubtractionPose = LocalSpacePose.fromAnimationSequence(
                            jointSkeleton,
                            this.configuration.animationSequence(),
                            this.configuration.startTimeOffset(),
                            false
                    );
                    this.additiveSubtractionPose.invert();
                }

                pose.multiply(this.additiveSubtractionPose, JointChannel.TransformSpace.COMPONENT);
                pose.multiply(this.additiveBasePose, JointChannel.TransformSpace.COMPONENT);
            }
            return pose;
        }

        private float getInterpolatedWeight(float partialTicks) {
            float weight = this.weight.getValueInterpolated(partialTicks);
            float startTimeOffsetTicks = this.configuration.startTimeOffset().inTicks();

            if (weight == 1 || weight == 0) {
                return weight;
            } else if (this.ticksElapsed.getValueInterpolated(partialTicks) < this.configuration.transitionIn().duration().inTicks() + startTimeOffsetTicks) {
                return this.configuration.transitionIn().applyEasement(weight);
            } else {
                return 1 - this.configuration.transitionOut().applyEasement(1 - weight);
            }
        }
    }
}
