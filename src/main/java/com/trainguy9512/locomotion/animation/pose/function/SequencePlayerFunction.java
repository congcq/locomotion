package com.trainguy9512.locomotion.animation.pose.function;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.animation.data.AnimationSequenceData;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class SequencePlayerFunction extends TimeBasedPoseFunction<LocalSpacePose> implements AnimationPlayer {

    private final ResourceLocation animationSequence;
    private final boolean looping;
    private final boolean ignoredByRelevancyTest;
    private final Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings;

    protected SequencePlayerFunction(
            Function<FunctionEvaluationState, Boolean> isPlayingFunction,
            Function<FunctionEvaluationState, Float> playRateFunction,
            float resetStartTimeOffsetTicks,
            ResourceLocation animationSequence,
            boolean looping,
            boolean ignoredByRelevancyTest,
            Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings
    ) {
        super(isPlayingFunction, playRateFunction, resetStartTimeOffsetTicks);
        this.animationSequence = animationSequence;
        this.looping = looping;
        this.timeMarkerBindings = timeMarkerBindings;
        this.ignoredByRelevancyTest = false;
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        return LocalSpacePose.fromAnimationSequence(
                context.driverContainer().getJointSkeleton(),
                this.animationSequence,
                this.getInterpolatedTimeElapsed(context),
                this.looping
        );
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        super.tick(evaluationState);
        Set<String> timeMarkersToFire = AnimationSequenceData.INSTANCE.getOrThrow(this.animationSequence).getMarkersInRange(TimeSpan.ofTicks(this.ticksElapsed.getCurrentValue()), TimeSpan.ofTicks(this.ticksElapsed.getCurrentValue() + this.playRate), this.looping);
        for (String timeMarker : timeMarkersToFire) {
            if (this.timeMarkerBindings.containsKey(timeMarker)) {
                this.timeMarkerBindings.get(timeMarker).accept(evaluationState);
            }
        }
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        return new SequencePlayerFunction(
                this.isPlayingFunction,
                this.playRateFunction,
                this.resetStartTimeOffsetTicks,
                this.animationSequence,
                this.looping,
                this.ignoredByRelevancyTest,
                this.timeMarkerBindings
        );
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        return this.ignoredByRelevancyTest ? Optional.empty() : Optional.of(this);
    }

    public static Builder<?> builder(ResourceLocation animationSequence) {
        return new Builder<>(animationSequence);
    }

    @Override
    public Tuple<TimeSpan, TimeSpan> getRemainingTime() {
        float lengthInTicks = AnimationSequenceData.INSTANCE.getOrThrow(animationSequence).length().inTicks();
        float remainingTimePreviously;
        float remainingTimeCurrently;
        if (this.looping) {
            remainingTimePreviously = lengthInTicks - ((this.ticksElapsed.getCurrentValue() - this.playRate) % lengthInTicks);
            remainingTimeCurrently = lengthInTicks - (this.ticksElapsed.getCurrentValue() % lengthInTicks);
        } else {
            remainingTimePreviously = lengthInTicks - (Mth.clamp(this.ticksElapsed.getCurrentValue() - this.playRate, 0, lengthInTicks));
            remainingTimeCurrently = lengthInTicks - (Mth.clamp(this.ticksElapsed.getCurrentValue(), 0, lengthInTicks));
        }
        return new Tuple<>(TimeSpan.ofTicks(remainingTimePreviously), TimeSpan.ofTicks(remainingTimeCurrently));
    }

    @Override
    public TimeSpan getAnimationLength() {
        return AnimationSequenceData.INSTANCE.getOrThrow(this.animationSequence).length();
    }

    public static class Builder<B extends Builder<B>> extends TimeBasedPoseFunction.Builder<B>{

        private final ResourceLocation animationSequence;
        private boolean looping;
        private boolean ignoredForRelevancyTest;
        private final Map<String, Consumer<FunctionEvaluationState>> timeMarkerBindings;

        protected Builder(ResourceLocation animationSequence){
            super();
            this.animationSequence = animationSequence;
            this.looping = false;
            this.ignoredForRelevancyTest = false;
            this.timeMarkerBindings = Maps.newHashMap();
        }

        /**
         * Sets the animation sequence player to loop when the end of the animation is reached.
         * @implNote                    The animation sequence will always be looped in full, the reset start time only
         *                              affects where the animation starts when reset.
         */
        @SuppressWarnings("unchecked")
        public B looping(boolean looping) {
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

        /**
         * Marks this sequence player function to be ignored by {@link PoseFunction#testForMostRelevantAnimationPlayer()}.
         * <p>
         * If multiple sequence players of equal relevance are used by a state that has an automatic out-transition, use this
         * to exclude players that shouldn't be retrieved.
         */
        @SuppressWarnings("unchecked")
        public B ignoreForRelevancyTest() {
            this.ignoredForRelevancyTest = true;
            return (B) this;
        }

        public SequencePlayerFunction build() {
            return new SequencePlayerFunction(
                    this.isPlayingFunction,
                    this.playRateFunction,
                    this.resetStartTimeOffsetTicks,
                    this.animationSequence,
                    this.looping,
                    this.ignoredForRelevancyTest,
                    this.timeMarkerBindings
            );
        }
    }
}
