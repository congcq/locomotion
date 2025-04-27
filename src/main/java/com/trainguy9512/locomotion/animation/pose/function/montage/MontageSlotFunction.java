package com.trainguy9512.locomotion.animation.pose.function.montage;

import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.AnimationPlayer;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record MontageSlotFunction(PoseFunction<LocalSpacePose> inputPose, String slot) implements PoseFunction<LocalSpacePose> {

    public static MontageSlotFunction of(PoseFunction<LocalSpacePose> inputPose, String slot) {
        return new MontageSlotFunction(inputPose, slot);
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        return context.montageManager().getLayeredSlotPose(this.inputPose.compute(context), this.slot, context.driverContainer().getJointSkeleton(), context.partialTicks());
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        if (!evaluationState.montageManager().areAnyMontagesInSlotFullyOverriding(this.slot)) {
            this.inputPose.tick(evaluationState);
        }
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return MontageSlotFunction.of(this.inputPose, this.slot);
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return this.inputPose.testForMostRelevantAnimationPlayer();
    }
}
