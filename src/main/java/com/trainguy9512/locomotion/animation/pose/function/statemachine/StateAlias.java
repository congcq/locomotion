package com.trainguy9512.locomotion.animation.pose.function.statemachine;

import com.trainguy9512.locomotion.LocomotionMain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record StateAlias<S extends Enum<S>>(Set<S> originStates, List<StateTransition<S>> outboundTransitions) {

    private static final Logger LOGGER = LogManager.getLogger("Locomotion/StateAlias");

    /**
     * Creates a new state alias builder.
     *
     * @param originStates      States that the alias' transitions can originate from.
     */
    public static <S extends Enum<S>> Builder<S> builder(Set<S> originStates) {
        return new Builder<>(originStates);
    }

    public static class Builder<S extends Enum<S>> {

        private final Set<S> originStates;
        private final List<StateTransition<S>> outboundTransitions;

        private Builder(Set<S> originStates) {
            this.originStates = new HashSet<>(originStates);
            this.outboundTransitions = new ArrayList<>();
        }

        /**
         * Adds a set of states that the alias' transitions can originate from.
         * @param states        State identifiers
         */
        public final Builder<S> addOriginatingStates(Set<S> states) {
            this.originStates.addAll(states);
            return this;
        }

        /**
         * Adds a state that the alias' transitions can originate from.
         * @param state         State identifier
         */
        public final Builder<S> addOriginatingState(S state) {
            this.originStates.add(state);
            return this;
        }

        /**
         * Assigns a potential outbound transitions to this state alias.
         *
         * @param transition    Outbound transitions.
         */
        public Builder<S> addOutboundTransition(StateTransition<S> transition) {
            if (this.originStates.contains(transition.target())) {
                LOGGER.warn("Cannot add state transition to state {} from state alias that contains it already: {}", transition.target(), this.originStates);
            }
            this.outboundTransitions.add(transition);
            return this;
        }

        public StateAlias<S> build() {
            return new StateAlias<>(this.originStates, this.outboundTransitions);
        }
    }
}
