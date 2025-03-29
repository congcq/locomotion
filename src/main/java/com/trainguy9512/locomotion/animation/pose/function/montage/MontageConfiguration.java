package com.trainguy9512.locomotion.animation.pose.function.montage;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;
import com.trainguy9512.locomotion.util.Easing;
import com.trainguy9512.locomotion.util.TimeSpan;
import com.trainguy9512.locomotion.util.Transition;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Configuration for a triggerable animation, otherwise known as a Montage in Unreal Engine.
 * @param slot
 * @param animationSequence
 * @param playRate
 * @param timeMarkerBindings
 * @param startTimeOffset
 * @param endTimeOffset
 * @param transitionIn
 * @param transitionOut
 */
public record MontageConfiguration(
        String slot,
        ResourceLocation animationSequence,
        float playRate,
        Map<String, Consumer<PoseFunction.FunctionEvaluationState>> timeMarkerBindings,
        TimeSpan startTimeOffset,
        TimeSpan endTimeOffset,
        Transition transitionIn,
        Transition transitionOut
) {

    public static Builder builder(String targetTrack, ResourceLocation animationSequence) {
        return new Builder(targetTrack, animationSequence);
    }

    public static class Builder {

        private final String slot;
        private final ResourceLocation animationSequence;
        private float playRate;
        private final Map<String, Consumer<PoseFunction.FunctionEvaluationState>> timeMarkerBindings;
        private TimeSpan startTimeOffset;
        private TimeSpan endTimeOffset;
        private TimeSpan transitionInDuration;
        private TimeSpan transitionOutDuration;
        private Easing transitionInEasing;
        private Easing transitionOutEasing;

        private Builder(String slot, ResourceLocation animationSequence) {
            this.slot = slot;
            this.animationSequence = animationSequence;
            this.playRate = 1;
            this.timeMarkerBindings = Maps.newHashMap();
            this.startTimeOffset = TimeSpan.ofSeconds(0);
            this.endTimeOffset = TimeSpan.ofSeconds(0);
            this.transitionInDuration = TimeSpan.ofTicks(1);
            this.transitionOutDuration = TimeSpan.ofTicks(1);
            this.transitionInEasing = Easing.LINEAR;
            this.transitionOutEasing = Easing.LINEAR;
        }

        /**
         * Sets the rate at which the montage plays.
         * @param playRate              Play rate.
         */
        public Builder setPlayRate(Float playRate) {
            this.playRate = playRate;
            return this;
        }

        /**
         * Sets the duration and easing of the entrance transition.
         * @param duration              Length of the transition.
         * @param easing                Easing function applied to the transition.
         */
        public Builder setTransitionIn(TimeSpan duration, Easing easing) {
            this.transitionInDuration = duration;
            this.transitionInEasing = easing;
            return this;
        }

        /**
         * Sets the duration and easing of the exit transition.
         * @param duration              Length of the transition.
         * @param easing                Easing function applied to the transition.
         */
        public Builder setTransitionOut(TimeSpan duration, Easing easing) {
            this.transitionOutDuration = duration;
            this.transitionOutEasing = easing;
            return this;
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
        public Builder bindToTimeMarker(String timeMarkerIdentifier, Consumer<PoseFunction.FunctionEvaluationState> binding) {
            this.timeMarkerBindings.computeIfPresent(timeMarkerIdentifier, (identifier, existingBinding) -> existingBinding.andThen(binding));
            this.timeMarkerBindings.putIfAbsent(timeMarkerIdentifier, binding);
            return this;
        }

        /**
         * Offsets the time in the animation where the montage starts.
         * @param offset                Offset time
         */
        public Builder setStartTimeOffset(TimeSpan offset) {
            this.startTimeOffset = offset;
            return this;
        }

        /**
         * Offsets the time in the animation where the exit transition begins.
         * @param offset                Offset time
         */
        public Builder setEndTimeOffset(TimeSpan offset) {
            this.endTimeOffset = offset;
            return this;
        }

//        public MontageConfiguration build() {
//            return new MontageConfiguration(
//                    this.slot,
//                    this.animationSequence,
//                    this.playRate,
//                    this.timeMarkerBindings,
//                    this.startTimeOffset,
//                    this.endTimeOffset,
//                    this.transitionInDuration,
//                    this.transitionOutDuration,
//                    this.transitionInEasing,
//                    this.transitionOutEasing
//            );
//        }
    }
}
