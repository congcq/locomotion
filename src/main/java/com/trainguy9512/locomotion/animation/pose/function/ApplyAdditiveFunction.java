package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public class ApplyAdditiveFunction implements PoseFunction<LocalSpacePose> {

    private final PoseFunction<LocalSpacePose> basePoseInput;
    private final PoseFunction<LocalSpacePose> additivePoseInput;

    private final Function<FunctionInterpolationContext, Float> weightFunction;

    public ApplyAdditiveFunction(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput, Function<FunctionInterpolationContext, Float> weightFunction) {
        this.basePoseInput = basePoseInput;
        this.additivePoseInput = additivePoseInput;
        this.weightFunction = weightFunction;
    }

    public static ApplyAdditiveFunction of(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput, Function<FunctionInterpolationContext, Float> weightFunction) {
        return new ApplyAdditiveFunction(basePoseInput, additivePoseInput, weightFunction);
    }

    public static ApplyAdditiveFunction of(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput) {
        return new ApplyAdditiveFunction(basePoseInput, additivePoseInput, context -> 1f);
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        LocalSpacePose basePose = this.basePoseInput.compute(context);

        LocalSpacePose additivePose = this.additivePoseInput.compute(context);
        additivePose.multiply(basePose, JointChannel.TransformSpace.COMPONENT);

        float weight = this.weightFunction.apply(context);
        if (weight == 1f) {
            return additivePose;
        } else if (weight == 0f) {
            return basePose;
        } else {
            return basePose.interpolated(additivePose, weight);
        }
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        this.basePoseInput.tick(evaluationState);
        this.additivePoseInput.tick(evaluationState);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new ApplyAdditiveFunction(this.basePoseInput.wrapUnique(), this.additivePoseInput.wrapUnique(), this.weightFunction);
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        // Test the base pose input first. If it does not have a relevant animation player, then test the additive pose input.
        Optional<AnimationPlayer> test = this.basePoseInput.testForMostRelevantAnimationPlayer();
        if (test.isPresent()) {
            return test;
        }
        return this.additivePoseInput.testForMostRelevantAnimationPlayer();
    }
}
