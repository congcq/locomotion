package com.trainguy9512.locomotion.util;

/**
 * Represents the properties of a transition, including the duration and easing function used to
 * blend between animation poses.
 *
 * <p>In most implementations of transitions, the underlying transition itself is a linear gradient from 0 to 1
 * with the easing function applied on top.</p>
 *
 * <p>The transition duration specifies how long the transition takes to complete, as a {@link TimeSpan}.
 * The transition easement defines the type of {@link Easing} function used to modify the interpolation
 * of the transition, allowing for more interesting or stylized transitions.</p>
 *
 * @param duration          The duration of the transition.
 * @param easement          The type of {@link Easing} function to apply to the transition.
 */
public record Transition(TimeSpan duration, Easing easement) {

    public static final Transition INSTANT = Transition.builder(TimeSpan.ofTicks(1)).setEasement(Easing.CONSTANT).build();
    public static final Transition SINGLE_TICK = Transition.builder(TimeSpan.ofTicks(1)).setEasement(Easing.LINEAR).build();

    /**
     * Creates a new {@link Transition.Builder} with the provided duration.
     * @param duration      The duration of the transition
     */
    public static Builder builder(TimeSpan duration) {
        return new Builder(duration);
    }

    public float applyEasement(float input) {
        return this.easement.ease(input);
    }

    public static class Builder {

        private final TimeSpan duration;
        private Easing easement;

        private Builder(TimeSpan duration) {
            this.duration = duration;
            this.easement = Easing.LINEAR;
        }

        public Builder setEasement(Easing easement) {
            this.easement = easement;
            return this;
        }

        public Transition build() {
            return new Transition(
                    this.duration,
                    this.easement
            );
        }
    }
}
