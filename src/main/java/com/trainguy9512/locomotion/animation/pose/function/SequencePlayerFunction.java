package com.trainguy9512.locomotion.animation.pose.function;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceData;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class SequencePlayerFunction extends TimeBasedPoseFunction<LocalSpacePose> {

    private final ResourceLocation animationSequence;
    private final boolean looping;
    private final Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings;

    protected SequencePlayerFunction(Function<FunctionEvaluationState, Boolean> isPlayingFunction, Function<FunctionEvaluationState, Float> playRateFunction, float resetStartTimeOffsetTicks, ResourceLocation animationSequence, boolean looping, Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings) {
        super(isPlayingFunction, playRateFunction, resetStartTimeOffsetTicks);
        this.animationSequence = animationSequence;
        this.looping = looping;
        this.timeMarkerBindings = timeMarkerBindings;
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        return LocalSpacePose.fromAnimationSequence(
                context.dataContainer().getJointSkeleton(),
                this.animationSequence,
                this.getInterpolatedTimeElapsed(context),
                this.looping
        );
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        Set<String> timeMarkersToFire = AnimationSequenceData.INSTANCE.getOrThrow(this.animationSequence).getMarkersInRange(TimeSpan.ofTicks(this.timeTicksElapsed), TimeSpan.ofTicks(this.timeTicksElapsed + this.playRate), this.looping);
        for (String timeMarker : timeMarkersToFire) {
            if (this.timeMarkerBindings.containsKey(timeMarker)) {
                this.timeMarkerBindings.get(timeMarker).accept(evaluationState);
            }
        }
        super.tick(evaluationState);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new SequencePlayerFunction(this.isPlayingFunction, this.playRateFunction, this.resetStartTimeOffsetTicks, this.animationSequence, this.looping, this.timeMarkerBindings);
    }

    /**
     * Returns whether this sequence player has just looped or finished in the previous tick.
     * Meant to be called in contexts just prior to this sequence player updating, like {@link StateMachineFunction#SEQUENCE_PLAYER_IN_ACTIVE_STATE_HAS_FINISHED}
     */
    public boolean hasJustLoopedOrFinished() {
        float lengthInTicks = AnimationSequenceData.INSTANCE.getOrThrow(animationSequence).length().inTicks();
        boolean hasProgressedToOrPastLength = this.timeTicksElapsed >= lengthInTicks;
        if(this.looping){
            return this.timeTicksElapsed % lengthInTicks - this.playRate <= 0 && hasProgressedToOrPastLength;
        } else {
            return this.timeTicksElapsed - this.playRate >= lengthInTicks && hasProgressedToOrPastLength;
        }
    }

    public static Builder<?> builder(ResourceLocation animationSequence) {
        return new Builder<>(animationSequence);
    }

    public static class Builder<B extends Builder<B>> extends TimeBasedPoseFunction.Builder<B>{

        private final ResourceLocation animationSequence;
        private boolean looping;
        private final Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings;

        protected Builder(ResourceLocation animationSequence){
            super();
            this.animationSequence = animationSequence;
            this.looping = false;
            this.timeMarkerBindings = Maps.newHashMap();
        }

        /**
         * Sets whether the animation sequence function will loop or not when the end of the animation is reached.
         * @implNote                    The animation sequence will always be looped in full, the reset start time only
         *                              affects where the animation starts when reset.
         */
        @SuppressWarnings("unchecked")
        public B setLooping(boolean looping) {
            this.looping = looping;
            return (B) this;
        }

        /**
         * Binds an event to fire every time the sequence player passes a time marker of the given identifier.
         * <p>
         * Time markers can be defined by animation sequences within Maya. A time marker can have multiple
         * time points defined, so binding an event to an identifier will bind it for every instance of it
         * within the sequence.
         * <p>
         * Multiple bindings can be bound to the same time marker. When the marker is triggered, it will fire the events in
         * the sequence in which they were bound.
         * @param timeMarkerIdentifier  String identifier for the time marker, pointing to the associated time marker in the sequence file.
         * @param binding               Event to fire every time this time marker is passed when the sequence player is playing.
         */
        @SuppressWarnings("unchecked")
        public B bindToTimeMarker(String timeMarkerIdentifier, Consumer<FunctionEvaluationState> binding) {
            this.timeMarkerBindings.computeIfPresent(timeMarkerIdentifier, (identifier, existingBinding) -> existingBinding.andThen(binding));
            this.timeMarkerBindings.putIfAbsent(timeMarkerIdentifier, binding);
            return (B) this;
        }

        public SequencePlayerFunction build() {
            return new SequencePlayerFunction(this.isPlayingFunction, this.playRateFunction, this.resetStartTimeOffsetTicks, this.animationSequence, this.looping, this.timeMarkerBindings);
        }
    }
}
