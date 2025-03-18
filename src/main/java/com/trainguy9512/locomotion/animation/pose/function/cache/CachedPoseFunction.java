package com.trainguy9512.locomotion.animation.pose.function.cache;

import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.AnimationPlayer;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CachedPoseFunction implements PoseFunction<LocalSpacePose> {

    private PoseFunction<LocalSpacePose> input;

    LocalSpacePose poseCache;
    boolean hasTickedAlready;

    private CachedPoseFunction(PoseFunction<LocalSpacePose> input){
        this.input = input;
        this.poseCache = null;
        this.hasTickedAlready = false;
    }

    protected static CachedPoseFunction of(PoseFunction<LocalSpacePose> input){
        return new CachedPoseFunction(input);
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        if(this.poseCache == null){
            this.poseCache = this.input.compute(context);
        }
        return LocalSpacePose.of(this.poseCache);
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        if(!this.hasTickedAlready){
            this.input.tick(evaluationState.cancelMarkedForReset());
            this.hasTickedAlready = true;
        }
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        this.input = input.wrapUnique();
        return this;
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return Optional.empty();
    }

    public void clearCache(){
        this.poseCache = null;
        this.hasTickedAlready = false;
    }
}
