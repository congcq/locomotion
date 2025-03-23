package com.trainguy9512.locomotion.animation.pose.function;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.OnTickDriverContainer;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.util.TimeSpan;
import com.trainguy9512.locomotion.util.Transition;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
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
    private final List<S> activeStates;
    private final Function<OnTickDriverContainer, S> initialState;

    private StateMachineFunction(Map<S, State<S>> states, List<S> activeStates, Function<OnTickDriverContainer, S> initialState) {
        super(evaluationState -> true, evaluationState -> 1f, 0);
        this.states = states;
        this.activeStates = activeStates;
        this.initialState = initialState;
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
                        this.states.get(stateIdentifier).currentTransition.transition.applyEasement(
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
            S initialStateIdentifier = this.initialState.apply(evaluationState.dataContainer());
            if (this.states.containsKey(initialStateIdentifier)) {
                this.activeStates.add(initialStateIdentifier);
                State<S> intialState = this.states.get(initialStateIdentifier);
                intialState.isActive = true;
                intialState.weight.setValue(1f);
                intialState.weight.pushCurrentToPrevious();
                intialState.weight.setValue(1f);
            } else {
                throw new IllegalStateException("Initial state " + initialStateIdentifier + " not found to be present in the state machine");
            }
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
                    boolean transitionTargetIncludedInThisMachine = this.states.containsKey(stateTransition.target);
                    boolean targetIsNotCurrentActiveState = stateTransition.target() != currentActiveStateIdentifier;
                    if(transitionTargetIncludedInThisMachine && targetIsNotCurrentActiveState){
                        StateTransition.TransitionContext transitionContext = StateTransition.TransitionContext.of(
                                evaluationState.dataContainer(),
                                TimeSpan.ofTicks(this.ticksElapsed.getCurrentValue()),
                                this.states.get(currentActiveStateIdentifier).weight.getCurrentValue(),
                                this.states.get(currentActiveStateIdentifier).inputFunction,
                                stateTransition.transition.duration()
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
                stateTransition -> state.tick(evaluationState, state == this.states.get(stateTransition.target)),
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
        Builder<S> builder = StateMachineFunction.builder();
        this.states.forEach((stateIdentifier, state) -> builder.addState(stateIdentifier, state.inputFunction.wrapUnique(), state.resetUponEntry, state.outboundTransitions));
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
        private final List<S> activeStates;


        protected Builder(Function<OnTickDriverContainer, S> initialState) {
            this.initialState = initialState;
            this.states = Maps.newHashMap();
            this.activeStates = new ArrayList<>();
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

        public Builder<S> addTransitionsFromMultipleStates(Set<S> originStates, Set<StateTransition<S>> outboundTransitions) {
            return this;
        }

        public Builder<S> addTransitionsFromAnyState(Set<StateTransition<S>> outboundTransitions) {
            return this;
        }

        public StateMachineFunction<S> build(){
            return new StateMachineFunction<>(this.states, this.activeStates, this.initialState);
        }
    }

    public static class State<S extends Enum<S>> {

        private final S identifier;
        private final PoseFunction<LocalSpacePose> inputFunction;
        private final List<StateTransition<S>> outboundTransitions;
        private final boolean resetUponEntry;

        private boolean isActive;
        private final VariableDriver<Float> weight;
        private StateTransition<S> currentTransition;

        private State(S identifier, PoseFunction<LocalSpacePose> inputFunction, List<StateTransition<S>> outboundTransitions, boolean resetUponEntry){
            this.identifier = identifier;
            this.inputFunction = inputFunction;
            this.outboundTransitions = outboundTransitions;
            this.resetUponEntry = resetUponEntry;

            this.isActive = false;
            this.weight = VariableDriver.ofFloat(() -> 0f);
            this.currentTransition = null;

            if(!resetUponEntry){
                for(StateTransition<S> transition : outboundTransitions){
                    if(transition.isAutomaticTransition()){
                        LocomotionMain.LOGGER.warn("State transition to state {} in a state machine is set to be automatic based on the input sequence player, but the origin state is not set to reset upon entry. Automatic transitions are intended to be used with reset-upon-entry states, beware of unexpected behavior!", transition.target);
                    }
                }
            }
        }

        private void tick(FunctionEvaluationState evaluationState, boolean isEntering){
            if(this.currentTransition != null){
                this.weight.pushCurrentToPrevious();

                // Make the minimum transition time 0.01 to prevent a divide by zero error
                float increment = 1 / Math.max(this.currentTransition.transition.duration().inTicks(), 0.01f);
                float increaseDecreaseMultiplier = this.isActive ? 1 : -1;
                float nextWeightValue = this.weight.getPreviousValue() + increment * increaseDecreaseMultiplier;
                nextWeightValue = Mth.clamp(nextWeightValue, 0, 1);

                // If the state is just now becoming active after being de-active, and
                // the state is set to reset upon entry, set the evaluation state for child functions to reset.
                if (this.resetUponEntry && isEntering) {
                    evaluationState = evaluationState.markedForReset();
                }
                this.weight.setValue(nextWeightValue);
            }
            if (this.weight.getCurrentValue() > 0 || this.weight.getPreviousValue() > 0) {
                // Tick the child functions if the current weight value is greater than zero.
                this.inputFunction.tick(evaluationState);
            }
        }

        /**
         * Creates a new state builder.
         *
         * @param identifier            Enum identifier that is associated with this state. Used for identifying transition targets.
         * @param inputFunction         Pose function used for this state when it's active.
         */
        public static <S extends Enum<S>> Builder<S> builder(S identifier, PoseFunction<LocalSpacePose> inputFunction) {
            return new Builder<>(identifier, inputFunction);
        }

        /**
         * Creates a new state builder with the properties of the provided state.
         */
        private static <S extends Enum<S>> Builder<S> builder(State<S> state) {
            return new Builder<>(state);
        }

        public static class Builder<S extends Enum<S>> {

            private final S identifier;
            private PoseFunction<LocalSpacePose> inputFunction;
            private final List<StateTransition<S>> outboundTransitions;
            private boolean resetUponEntry;

            private Builder(S identifier, PoseFunction<LocalSpacePose> inputFunction) {
                this.identifier = identifier;
                this.inputFunction = inputFunction;
                this.outboundTransitions = new ArrayList<>();
                this.resetUponEntry = false;
            }

            private Builder(State<S> state) {
                this.identifier = state.identifier;
                this.inputFunction = state.inputFunction;
                this.outboundTransitions = state.outboundTransitions;
                this.resetUponEntry = state.resetUponEntry;
            }

            /**
             * If true, this state will reset its pose function every time it is entered.
             */
            public Builder<S> resetUponEntry(boolean resetUponEntry) {
                this.resetUponEntry = resetUponEntry;
                return this;
            }

            /**
             * Assigns a set of potential outbound transitions to this state.
             * @param outboundTransitions       Set of individual transitions.
             */
            public Builder<S> withOutboundTransitions(List<StateTransition<S>> outboundTransitions) {
                outboundTransitions.forEach(transition -> {
                    if (transition.target != this.identifier) {
                        this.outboundTransitions.add(transition);
                    } else {
                        LocomotionMain.LOGGER.warn("Cannot add state transition to state {} from the same state {}", transition.target, this.identifier);
                    }
                });
                return this;
            }

            /**
             * Assigns a set of potential outbound transitions to this state.
             * @param outboundTransitions       Set of individual transitions.
             */
            @SafeVarargs
            public final Builder<S> withOutboundTransitions(StateTransition<S>... outboundTransitions) {
                return this.withOutboundTransitions(List.of(outboundTransitions));
            }

            private Builder<S> wrapUniquePoseFunction() {
                this.inputFunction = inputFunction.wrapUnique();
                return this;
            }

            public State<S> build() {
                if (this.outboundTransitions.isEmpty()) {
                    LocomotionMain.LOGGER.warn("State {} in state machine contains no outbound transitions. If this state is entered, it will have no valid path out without re-initializing the state!", this.identifier);
                }
                return new State<>(this.identifier, this.inputFunction, this.outboundTransitions, this.resetUponEntry);
            }
        }
    }

    public record StateTransition<S extends Enum<S>> (
            S target,
            Predicate<TransitionContext> conditionPredicate,
            Transition transition,
            int priority,
            boolean isAutomaticTransition
    ) implements Comparable<StateTransition<S>> {

        public static final Predicate<StateTransition.TransitionContext> CURRENT_TRANSITION_FINISHED = transitionContext -> transitionContext.currentStateWeight() == 1;
        public static final Predicate<StateTransition.TransitionContext> MOST_RELEVANT_ANIMATION_PLAYER_FINISHING = makeMostRelevantAnimationPlayerFinishedCondition(1f);

        public static Predicate<StateTransition.TransitionContext> makeMostRelevantAnimationPlayerFinishedCondition(float crossFadeWeight) {
            return transitionContext -> {
                var potentialPlayer = transitionContext.currentStateInput.testForMostRelevantAnimationPlayer();
                if (potentialPlayer.isPresent()) {
                    AnimationPlayer player = potentialPlayer.get();
                    float transitionTimeTicks = transitionContext.transitionDuration().inTicks() * crossFadeWeight;
                    Tuple<TimeSpan, TimeSpan> remainingTime = player.getRemainingTime();

                    // Mid-animation
                    if (remainingTime.getA().inTicks() > remainingTime.getB().inTicks()) {
                        return transitionTimeTicks < remainingTime.getA().inTicks() && transitionTimeTicks >= remainingTime.getB().inTicks();
                        // Looping (remaining time wrapping around 0), but NOT stopped.
                    } else if(remainingTime.getA().inTicks() < remainingTime.getB().inTicks()) {
                        return transitionTimeTicks < remainingTime.getA().inTicks();
                    }
                }
                return false;
            };
        }

        @SafeVarargs
        public static <S extends Enum<S>> Builder<S> builder(S target, Predicate<TransitionContext>... conditionPredicates){
            return new Builder<>(target, conditionPredicates);
        }

        @Override
        public int compareTo(@NotNull StateTransition other) {
            return Integer.compare(this.priority(), other.priority());
        }

        public static class Builder<S extends Enum<S>> {
            private Predicate<TransitionContext> conditionPredicate;
            private final S target;
            private Transition transition;
            private int priority;
            private boolean automaticTransition;

            @SafeVarargs
            private Builder(S target, Predicate<TransitionContext>... conditionPredicates){
                Predicate<TransitionContext> compiledPredicates = context -> true;
                for(Predicate<TransitionContext> predicate : conditionPredicates){
                    compiledPredicates = compiledPredicates.and(predicate);
                }
                this.conditionPredicate = compiledPredicates;
                this.target = target;
                this.transition = Transition.SINGLE_TICK;
                this.priority = 50;
                this.automaticTransition = false;
            }

            /**
             * Sets the transition to be passable as an OR condition if the most relevant
             * animation player is within the the transition duration of finishing.
             * <p>
             * In other words, if the sequence player in the current active state loops or ends,
             * this becomes true.
             * @param crossFadeWeight       Weight of how the transition duration affects the condition. 1 = Full crossfade, 0 = Always at end of animation
             */
            public Builder<S> automaticallyTransitionIfAnimationPlayerFinishing(float crossFadeWeight) {
                this.conditionPredicate = this.conditionPredicate.or(makeMostRelevantAnimationPlayerFinishedCondition(crossFadeWeight));
                this.automaticTransition = true;
                return this;
            }

            @SafeVarargs
            public final Builder<S> setCondition(Predicate<TransitionContext>... conditionPredicates) {

            }

            /**
             * Sets the transition timing properties for the state transition. Default is a single-tick linear transition.
             *
             * @param transition            The {@link Transition} to use
             */
            public Builder<S> setTiming(Transition transition) {
                this.transition = transition;
                return this;
            }

            /**
             * Sets the transition priority for the state transition, for when more than one transition is active on the same tick.
             *
             * <p>Lower integers specify a higher priority. If more than one transition has the same priority, then it is picked at random.</p>
             *
             * <p>Default priority is <code>50</code>.</p>
             *
             * @param priority                  Priority integer
             */
            public Builder<S> setPriority(int priority) {
                this.priority = priority;
                return this;
            }

            public StateTransition<S> build() {
                return new StateTransition<>(this.target, this.conditionPredicate, this.transition, this.priority, this.automaticTransition);
            }
        }

        public record TransitionContext(
                OnTickDriverContainer dataContainer,
                TimeSpan timeElapsedInCurrentState,
                float currentStateWeight,
                PoseFunction<LocalSpacePose> currentStateInput,
                TimeSpan transitionDuration
        )
        {
            public static TransitionContext of(OnTickDriverContainer dataContainer, TimeSpan timeElapsedInCurrentState, float currentStateWeight, PoseFunction<LocalSpacePose> currentStateInput, TimeSpan transitionDuration){
                return new TransitionContext(dataContainer, timeElapsedInCurrentState, currentStateWeight, currentStateInput, transitionDuration);
            }
        }
    }
}
