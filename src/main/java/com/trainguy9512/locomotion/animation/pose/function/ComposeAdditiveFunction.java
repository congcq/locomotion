package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class ComposeAdditiveFunction implements PoseFunction<LocalSpacePose> {

    private final PoseFunction<LocalSpacePose> basePoseInput;
    private final PoseFunction<LocalSpacePose> additivePoseInput;
    private final PoseFunction<LocalSpacePose> additivePoseReferenceInput;

    private final Function<FunctionInterpolationContext, Float> weightFunction;

    public ComposeAdditiveFunction(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput, PoseFunction<LocalSpacePose> additivePoseReferenceInput, Function<FunctionInterpolationContext, Float> weightFunction) {
        this.basePoseInput = basePoseInput;
        this.additivePoseInput = additivePoseInput;
        this.additivePoseReferenceInput = additivePoseReferenceInput;
        this.weightFunction = weightFunction;
    }

    public static ComposeAdditiveFunction of(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput, PoseFunction<LocalSpacePose> additivePoseReferenceInput, Function<FunctionInterpolationContext, Float> weightFunction) {
        return new ComposeAdditiveFunction(basePoseInput, additivePoseInput, additivePoseReferenceInput, weightFunction);
    }

    public static ComposeAdditiveFunction of(PoseFunction<LocalSpacePose> basePoseInput, PoseFunction<LocalSpacePose> additivePoseInput, PoseFunction<LocalSpacePose> additivePoseReferenceInput) {
        return new ComposeAdditiveFunction(basePoseInput, additivePoseInput, additivePoseReferenceInput, context -> 1f);
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        float weight = this.weightFunction.apply(context);

        LocalSpacePose basePose = this.basePoseInput.compute(context);
        LocalSpacePose additivePose = this.additivePoseInput.compute(context);
        LocalSpacePose additivePoseReference = this.additivePoseReferenceInput.compute(context);

        additivePoseReference.invert();

        additivePose.multiply(additivePoseReference, JointChannel.TransformSpace.COMPONENT);
        additivePose.multiply(basePose, JointChannel.TransformSpace.COMPONENT);

        return additivePose;
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        this.basePoseInput.tick(evaluationState);
        this.additivePoseInput.tick(evaluationState);
        this.additivePoseReferenceInput.tick(evaluationState);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new ComposeAdditiveFunction(this.basePoseInput.wrapUnique(), this.additivePoseInput.wrapUnique(), this.additivePoseReferenceInput.wrapUnique(), this.weightFunction);
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
