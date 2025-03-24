package com.trainguy9512.locomotion.animation.pose.function.statemachine;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.AnimationPlayer;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;
import com.trainguy9512.locomotion.animation.pose.function.TimeBasedPoseFunction;
import com.trainguy9512.locomotion.util.TimeSpan;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pose function that manages transitions between a set of enum-identified states based on instance-defined transition logic.
 *
 * <p>Each state has its own pose function and a set of potential paths to other states that are taken
 * based on a condition predicate</p>
 *
 * <p>State machine functions are useful for animation functionality where specific animations
 * should be triggered in sequence based on an entity's actions, such as a jumping animation triggering when the entity
 * is both off the ground and moving upwards, which would transition into a falling animation and then
 * back to a standing animation once no longer falling.</p>
 * @param <S>           Enum type used as state identifiers.
 */
public class StateMachineFunction<S extends Enum<S>> extends TimeBasedPoseFunction<LocalSpacePose> {

    private final Map<S, State<S>> states;
    private final Function<OnTickDriverContainer, S> initialState;
    private final List<S> activeStates;

    private StateMachineFunction(Map<S, State<S>> states, Function<OnTickDriverContainer, S> initialState) {
        super(evaluationState -> true, evaluationState -> 1f, 0);
        this.states = states;
        this.initialState = initialState;
        this.activeStates = new ArrayList<>();
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        // If the list of active states is empty, throw an error because this should never be the case unless something has gone wrong.
        if(this.activeStates.isEmpty()){
            throw new IllegalStateException("State machine's active states list found to be empty.");
        }

        // Blend each active state's pose.
        LocalSpacePose pose = this.states.get(this.activeStates.getFirst()).inputFunction.compute(context);
        for(S stateIdentifier : this.activeStates){
            // We already got the first active state's pose.
            if(stateIdentifier != this.activeStates.getFirst()){
                pose = pose.interpolated(
                        this.states.get(stateIdentifier).inputFunction.compute(context),
                        this.states.get(stateIdentifier).currentTransition.transition().applyEasement(
                                this.states.get(stateIdentifier).weight.getValueInterpolated(context.partialTicks())
                        )
                );
            }
        }
        return pose;
    }

    @Override
    public void tick(FunctionEvaluationState evaluationState) {
        // If the state machine has no active states, initialize it using the initial state function.
        if(this.activeStates.isEmpty()){
            this.setToInitialState();
        }

        // Add to the current elapsed ticks
        super.tick(evaluationState);

        // Get the current active state
        S currentActiveStateIdentifier = this.activeStates.getLast();
        State<S> currentActiveState = this.states.get(currentActiveStateIdentifier);


        // Filter each potential state transition by whether it's valid, then filter by whether its condition predicate is true,
        // then shuffle it in order to make equal priority transitions randomized and re-order the valid transitions by filter order.
        Optional<StateTransition<S>> potentialStateTransition = currentActiveState.outboundTransitions.stream()
                .filter(stateTransition -> {
                    boolean transitionTargetIncludedInThisMachine = this.states.containsKey(stateTransition.target());
                    boolean targetIsNotCurrentActiveState = stateTransition.target() != currentActiveStateIdentifier;
                    if(transitionTargetIncludedInThisMachine && targetIsNotCurrentActiveState){
                        StateTransition.TransitionContext transitionContext = StateTransition.TransitionContext.of(
                                evaluationState.dataContainer(),
                                TimeSpan.ofTicks(this.ticksElapsed.getCurrentValue()),
                                this.states.get(currentActiveStateIdentifier).weight.getCurrentValue(),
                                this.states.get(currentActiveStateIdentifier).inputFunction,
                                stateTransition.transition().duration()
                        );
                        return stateTransition.conditionPredicate().test(transitionContext);
                    }
                    return false;
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected;
                }))
                .stream()
                .sorted()
                .findFirst();

        // Set all states to inactive except the new destination state. Also set the transition to all states for when they're ticked
        potentialStateTransition.ifPresent(stateTransition -> {
            this.resetTime();
            this.states.forEach((stateIdentifier, state) -> {
                state.currentTransition = stateTransition;
                state.isActive = state == this.states.get(stateTransition.target());
            });

            // Update the active states array
            // Make sure there already isn't this state present in active states

            this.activeStates.remove(stateTransition.target());
            this.activeStates.addLast(stateTransition.target());

        });

        // Tick each state
        this.states.forEach((stateIdentifier, state) -> potentialStateTransition.ifPresentOrElse(
                stateTransition -> state.tick(evaluationState, state == this.states.get(stateTransition.target())),
                () -> state.tick(evaluationState, false)
        ));

        // Evaluated last, remove states from the active state list that have a weight of 0.
        List<S> statesToRemove = this.activeStates.stream().filter((stateIdentifier) -> this.states.get(stateIdentifier).weight.getPreviousValue() == 0 && this.states.get(stateIdentifier).weight.getCurrentValue() == 0).toList();
        this.activeStates.removeAll(statesToRemove);

        /*
        if (this.activeStates.getLast() instanceof FirstPersonPlayerJointAnimator.GroundMovementStates) {
            LocomotionMain.LOGGER.info("{}\t\t{} {}\t{}",
                    this.activeStates.getLast(),
                    this.states.get(FirstPersonPlayerJointAnimator.GroundMovementStates.JUMP).weight.getPreviousValue(),
                    this.states.get(FirstPersonPlayerJointAnimator.GroundMovementStates.JUMP).weight.getCurrentValue(),
                    potentialStateTransition.isPresent());
        }
         */

    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        Builder<S> builder = StateMachineFunction.builder(this.initialState);
        this.states.forEach((identifier, state) ->
                builder.addState(
                        State.builder(state).wrapUniquePoseFunction().build()
                )
        );
        return builder.build();
    }

    @Override
    public Optional<AnimationPlayer> testForMostRelevantAnimationPlayer() {
        // Search for an animation player in the active states from most active to least active.
        for (int iteration = this.activeStates.size() - 1; iteration >= 0; iteration--) {
            var potentialPlayer = this.states.get(activeStates.get(iteration)).inputFunction.testForMostRelevantAnimationPlayer();
            if (potentialPlayer.isPresent()) {
                return potentialPlayer;
            }
        }
        return Optional.empty();
    }

    public static <S extends Enum<S>> Builder<S> builder(Function<OnTickDriverContainer, S> initialState) {
        return new Builder<>(initialState);
    }

    public static class Builder<S extends Enum<S>> {

        private final Function<OnTickDriverContainer, S> initialState;
        private final Map<S, State<S>> states;
        private final List<StateAlias<S>> stateAliases;

        protected Builder(Function<OnTickDriverContainer, S> initialState) {
            this.initialState = initialState;
            this.states = Maps.newHashMap();
            this.stateAliases = new ArrayList<>();
        }

        /**
         * Adds a state to the state machine builder.
         * @param state                 State created with a {@link State.Builder}
         */
        public Builder<S> addState(State<S> state) {
            if (this.states.containsKey(state.identifier)) {
                throw new IllegalStateException("Cannot add state " + state.identifier + " twice to the same state machine.");
            } else {
                this.states.put(state.identifier, state);
            }
            return this;
        }

        /**
         * Adds a state alias to the state machine builder.
         *
         * <p>A state alias is a shortcut function that allows you to add transitions from
         * multiple states at once, as a many-to-one transition.</p>
         *
         * <p>One example of how this could be used would be for a jumping animation state that
         * can be transitioned from the walking, idle, or crouching states with the same transition
         * properties and condition. Rather than individually adding the transition to each state,
         * this could be done through a state alias.</p>
         *
         * @param stateAlias            State alias created with a {@link StateAlias.Builder}
         */
        public Builder<S> addStateAlias(StateAlias<S> stateAlias) {
            this.stateAliases.add(stateAlias);
            return this;
        }

        public StateMachineFunction<S> build(){
            // Apply the state alias's outbound transitions to each of its origin states.
            for (StateAlias<S> stateAlias : this.stateAliases) {
                for (S originState : stateAlias.originStates()) {
                    if (this.states.containsKey(originState)) {
                        State.Builder<S> stateBuilder = State.builder(this.states.get(originState));
                        stateBuilder.addOutboundTransitions(stateAlias.outboundTransitions());
                        this.states.put(originState, stateBuilder.build());
                    } else {
                        LocomotionMain.LOGGER.error("Failed to apply state alias for state {}, as it hasn't been added to the state machine builder.", originState);
                    }
                }
            }
            // Check that every state's outbound transitions have valid identifiers.
            for (State<S> state : this.states.values()) {
                for (StateTransition<S> transition : state.outboundTransitions) {
                    if (!this.states.containsKey(transition.target())) {
                        LocomotionMain.LOGGER.error("State transition from states {} to {} not valid because state {} is not present in the state machine.", state.identifier, transition.target(), transition.target());
                    }
                }
            }
            return new StateMachineFunction<>(this.states, this.initialState);
        }
    }

}
