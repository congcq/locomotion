package com.trainguy9512.locomotion.animation.pose.function.montage;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;
import com.trainguy9512.locomotion.util.Easing;
import com.trainguy9512.locomotion.util.TimeSpan;
import com.trainguy9512.locomotion.util.Transition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration for a triggerable animation, otherwise known as a Montage in Unreal Engine.
 */
public record MontageConfiguration(
        String identifier,
        List<String> slots,
        ResourceLocation animationSequence,
        Function<OnTickDriverContainer, Float> playRateFunction,
        Map<String, Consumer<PoseFunction.FunctionEvaluationState>> timeMarkerBindings,
        Transition transitionIn,
        Transition transitionOut,
        TimeSpan startTimeOffset,
        float transitionOutCrossfadeWeight
) {

    public static Builder builder(String targetTrack, ResourceLocation animationSequence) {
        return new Builder(targetTrack, animationSequence);
    }

    public static class Builder {


        private final String identifier;
        private final ResourceLocation animationSequence;
        private final List<String> slots;
        private Function<OnTickDriverContainer, Float> playRateFunction;
        private Map<String, Consumer<PoseFunction.FunctionEvaluationState>> timeMarkerBindings;
        private Transition transitionIn;
        private Transition transitionOut;
        private TimeSpan startTimeOffset;
        private float transitionOutCrossfadeWeight;

        private Builder(String identifier, ResourceLocation animationSequence) {
            this.identifier = identifier;
            this.animationSequence = animationSequence;
            this.slots = new ArrayList<>();
            this.playRateFunction = driverContainer -> 1f;
            this.timeMarkerBindings = Maps.newHashMap();
            this.transitionIn = Transition.SINGLE_TICK;
            this.transitionOut = Transition.SINGLE_TICK;
            this.startTimeOffset = TimeSpan.ofSeconds(0);
            this.transitionOutCrossfadeWeight = 1f;
        }

        /**
         * Adds a slot identifier that the animation will play in.
         * Multiple slots can be added, so that the montage is played in multiple different places.
         * @param slotIdentifier        String slot identifier
         */
        public Builder playsInSlot(String slotIdentifier) {
            this.slots.add(slotIdentifier);
            return this;
        }

        /**
         * Adds a list of slot identifiers that the animation will play in.
         * @param slotIdentifiers       String slot identifiers
         */
        public Builder playsInSlots(String... slotIdentifiers) {
            this.slots.addAll(List.of(slotIdentifiers));
            return this;
        }

        /**
         * Sets the rate at which the montage plays. The provided function is only called once each
         * time a montage is played, with the constant play rate for the whole animation decided then.
         * @param playRate              Play rate function.
         */
        public Builder setPlayRate(Function<OnTickDriverContainer, Float> playRate) {
            this.playRateFunction = playRate;
            return this;
        }

        /**
         * Sets the timing of the exit transition
         * @param transitionIn         Exit transition timing
         */
        public Builder setTransitionIn(Transition transitionIn) {
            this.transitionIn = transitionIn;
            return this;
        }

        /**
         * Sets the timing of the exit transition
         * @param transitionOut         Exit transition timing
         */
        public Builder setTransitionOut(Transition transitionOut) {
            this.transitionOut = transitionOut;
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
         * Adjusts the weight of how the transition duration affects the beginning of the exit transition.
         *
         * <p>At <code>1</code>, the exit transition will begin as the animation is finishing and will end at the same time as the animation.
         * At <code>0</code>, the exit transition will begin when the animation is fully finished, and end after the animation has finished.</p>
         *
         * @param weight                Crossfade weight
         */
        public Builder setTransitionOutCrossfadeWeight(float weight) {
            this.transitionOutCrossfadeWeight = weight;
            return this;
        }

        public MontageConfiguration build() {
            return new MontageConfiguration(
                    this.identifier,
                    this.slots,
                    this.animationSequence,
                    this.playRateFunction,
                    this.timeMarkerBindings,
                    this.transitionIn,
                    this.transitionOut,
                    this.startTimeOffset,
                    this.transitionOutCrossfadeWeight
            );
        }
    }
}
