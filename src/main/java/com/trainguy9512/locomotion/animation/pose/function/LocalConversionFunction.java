package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.pose.ComponentSpacePose;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record LocalConversionFunction(PoseFunction<ComponentSpacePose> input) implements PoseFunction<LocalSpacePose> {
    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        return this.input.compute(context).convertedToLocalSpace();
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        this.input.tick(evaluationState);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return LocalConversionFunction.of(this.input.wrapUnique());
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return this.input.testForMostRelevantAnimationPlayer();
    }

    public static LocalConversionFunction of(PoseFunction<ComponentSpacePose> input){
        return new LocalConversionFunction(input);
    }
}
