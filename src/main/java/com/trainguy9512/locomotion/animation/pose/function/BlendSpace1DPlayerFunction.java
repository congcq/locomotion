package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;
import java.util.function.Function;

public class BlendSpace1DPlayerFunction extends TimeBasedPoseFunction<LocalSpacePose> {

    private final TreeMap<Float, BlendSpace1DEntry> blendSpaceEntries;
    private final Function<FunctionEvaluationState, Float> blendPositionFunction;
    private final VariableDriver<Float> blendPosition;

    private BlendSpace1DPlayerFunction(Function<FunctionEvaluationState, Boolean> isPlayingFunction, Function<FunctionEvaluationState, Float> playRateFunction, float resetStartTimeOffsetTicks, TreeMap<Float, BlendSpace1DEntry> blendSpaceEntries, Function<FunctionEvaluationState, Float> blendPositionFunction) {
        super(isPlayingFunction, playRateFunction, resetStartTimeOffsetTicks);
        this.blendSpaceEntries = blendSpaceEntries;
        this.blendPositionFunction = blendPositionFunction;
        this.blendPosition = VariableDriver.ofFloat(() -> 0f);
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        float position = this.blendPositionFunction.apply(evaluationState);
        this.blendPosition.prepareForNextTick();
        this.blendPosition.setValue(position);

        this.isPlaying = isPlayingFunction.apply(evaluationState);
        this.playRate = this.isPlaying ? playRateFunction.apply(evaluationState) * this.getPlayRateAtPosition(position) : 0;

        super.updateTime(evaluationState);
    }

    private float getPlayRateAtPosition(float position){
        var floorEntry = this.blendSpaceEntries.floorEntry(position);
        var ceilingEntry = this.blendSpaceEntries.ceilingEntry(position);

        if (floorEntry == null)
            return ceilingEntry.getValue().playRate();
        if (ceilingEntry == null)
            return floorEntry.getValue().playRate();


        // If they're both the same frame
        if (floorEntry.getKey().equals(ceilingEntry.getKey()))
            return floorEntry.getValue().playRate();

        float relativeTime = (position - floorEntry.getKey()) / (ceilingEntry.getKey() - floorEntry.getKey());
        return Mth.lerp(relativeTime, floorEntry.getValue().playRate(), ceilingEntry.getValue().playRate());
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        float interpolatedPosition = this.blendPosition.getValueInterpolated(context.partialTicks());
        TimeSpan time = this.getInterpolatedTimeElapsed(context);

        var floorEntry = this.blendSpaceEntries.floorEntry(interpolatedPosition);
        var ceilingEntry = this.blendSpaceEntries.ceilingEntry(interpolatedPosition);

        if (floorEntry == null)
            return LocalSpacePose.fromAnimationSequence(context.dataContainer().getJointSkeleton(), ceilingEntry.getValue().animationSequence(), time, true);
        if (ceilingEntry == null)
            return LocalSpacePose.fromAnimationSequence(context.dataContainer().getJointSkeleton(), floorEntry.getValue().animationSequence(), time, true);

        // If they're both the same frame
        if (floorEntry.getKey().equals(ceilingEntry.getKey()))
            return LocalSpacePose.fromAnimationSequence(context.dataContainer().getJointSkeleton(), floorEntry.getValue().animationSequence(), time, true);

        float relativeTime = (interpolatedPosition - floorEntry.getKey()) / (ceilingEntry.getKey() - floorEntry.getKey());
        LocalSpacePose floorPose = LocalSpacePose.fromAnimationSequence(context.dataContainer().getJointSkeleton(), floorEntry.getValue().animationSequence(), time, true);
        LocalSpacePose ceilingPose = LocalSpacePose.fromAnimationSequence(context.dataContainer().getJointSkeleton(), ceilingEntry.getValue().animationSequence(), time, true);

        return floorPose.interpolated(ceilingPose, relativeTime);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new BlendSpace1DPlayerFunction(this.isPlayingFunction, this.playRateFunction, this.resetStartTimeOffsetTicks, this.blendSpaceEntries, this.blendPositionFunction);
    }

    public static Builder<?> builder(Function<FunctionEvaluationState, Float> blendValueFunction) {
        return new Builder<>(blendValueFunction);
    }

    private record BlendSpace1DEntry(ResourceLocation animationSequence, float playRate) {

    }

    public static class Builder<B extends Builder<B>> extends TimeBasedPoseFunction.Builder<B> {

        private final TreeMap<Float, BlendSpace1DEntry> blendSpaceEntries;
        private final Function<FunctionEvaluationState, Float> blendValueFunction;


        private Builder(Function<FunctionEvaluationState, Float> blendValueFunction) {
            this.blendSpaceEntries = new TreeMap<>();
            this.blendValueFunction = blendValueFunction;
        }

        @SuppressWarnings("unchecked")
        public B addEntry(float position, ResourceLocation animationSequence, float playRate) {
            this.blendSpaceEntries.put(position, new BlendSpace1DEntry(animationSequence, playRate));
            return (B) this;
        }

        public BlendSpace1DPlayerFunction build() {
            if (this.blendSpaceEntries.isEmpty()) {
                throw new IllegalArgumentException("Blend space player has no added entries.");
            }
            return new BlendSpace1DPlayerFunction(this.isPlayingFunction, this.playRateFunction, this.resetStartTimeOffsetTicks, this.blendSpaceEntries, this.blendValueFunction);
        }
    }
}
