package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.pose.ComponentSpacePose;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ComponentConversionFunction(PoseFunction<LocalSpacePose> input) implements PoseFunction<ComponentSpacePose> {
    @Override
    public @NotNull ComponentSpacePose compute(FunctionInterpolationContext context) {
        return this.input.compute(context).convertedToComponentSpace();
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        this.input.tick(evaluationState);
    }

    @Override
    public PoseFunction<ComponentSpacePose> wrapUnique() {
        return ComponentConversionFunction.of(this.input.wrapUnique());
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return this.input.testForMostRelevantAnimationPlayer();
    }

    public static ComponentConversionFunction of(PoseFunction<LocalSpacePose> input){
        return new ComponentConversionFunction(input);
    }
}