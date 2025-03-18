package com.trainguy9512.locomotion.animation.pose.function;

import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class SequenceEvaluatorFunction implements PoseFunction<LocalSpacePose> {

    private final ResourceLocation animationSequenceLocation;
    private final Function<FunctionInterpolationContext, TimeSpan> sequenceTimeFunction;

    private SequenceEvaluatorFunction(ResourceLocation animationSequenceLocation, Function<FunctionInterpolationContext, TimeSpan> sequenceTimeFunction) {
        this.animationSequenceLocation = animationSequenceLocation;
        this.sequenceTimeFunction = sequenceTimeFunction;
    }

    public static SequenceEvaluatorFunction of(ResourceLocation animationSequenceLocation, Function<FunctionInterpolationContext, TimeSpan> sequenceTimeFunction) {
        return new SequenceEvaluatorFunction(animationSequenceLocation, sequenceTimeFunction);
    }

    public static SequenceEvaluatorFunction of(ResourceLocation animationSequenceLocation, TimeSpan sequenceTime) {
        return new SequenceEvaluatorFunction(animationSequenceLocation, context -> sequenceTime);
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        TimeSpan time = this.sequenceTimeFunction.apply(context);
        return LocalSpacePose.fromAnimationSequence(
                context.dataContainer().getJointSkeleton(),
                this.animationSequenceLocation,
                time,
                false
        );
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {

    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new SequenceEvaluatorFunction(this.animationSequenceLocation, this.sequenceTimeFunction);
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return Optional.empty();
    }
}
