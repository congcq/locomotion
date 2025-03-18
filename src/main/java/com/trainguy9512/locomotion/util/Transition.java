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

    public static final Transition INSTANT = Transition.of(TimeSpan.ofTicks(1), Easing.CONSTANT);
    public static final Transition SINGLE_TICK = Transition.of(TimeSpan.ofTicks(1), Easing.LINEAR);

    /**
     * Creates a new {@link Transition} with the provided duration and easement.
     * @param duration      The duration of the transition
     * @param easement      The type of {@link Easing} function to apply to the transition.
     */
    public static Transition of(TimeSpan duration, Easing easement) {
        return new Transition(duration, easement);
    }

    /**
     * Creates a new {@link Transition} with the provided duration with a linear easing function.
     * @param duration      The duration of the transition
     */
    public static Transition of(TimeSpan duration) {
        return new Transition(duration, Easing.LINEAR);
    }

    public float applyEasement(float input) {
        return this.easement.ease(input);
    }
}
