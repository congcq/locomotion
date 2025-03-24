package com.trainguy9512.locomotion.animation.pose.function.statemachine;

import com.trainguy9512.locomotion.LocomotionMain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record StateAlias<S extends Enum<S>>(Set<S> originStates, List<StateTransition<S>> outboundTransitions) {

    /**
     * Creates a new state alias builder.
     *
     * @param originStates List of origin states.
     */
    public static <S extends Enum<S>> Builder<S> builder(Set<S> originStates) {
        return new Builder<>(originStates);
    }

    public static class Builder<S extends Enum<S>> {

        private final Set<S> originStates;
        private final List<StateTransition<S>> outboundTransitions;

        private Builder(Set<S> originStates) {
            this.originStates = originStates;
            this.outboundTransitions = new ArrayList<>();
        }

        /**
         * Assigns a potential outbound transitions to this state alias.
         *
         * @param transition Outbound transitions.
         */
        public Builder<S> addOutboundTransition(StateTransition<S> transition) {
            if (this.originStates.contains(transition.target())) {
                LocomotionMain.LOGGER.warn("Cannot add state transition to state {} from state alias that contains it already: {}", transition.target(), this.originStates);
            }
            this.outboundTransitions.add(transition);
            return this;
        }

        public StateAlias<S> build() {
            return new StateAlias<>(this.originStates, this.outboundTransitions);
        }
    }
}
