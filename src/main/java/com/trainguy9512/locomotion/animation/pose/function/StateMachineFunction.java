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

    private StateMachineFunction(Map<S, State<S>> states, List<S> activeStates) {
        super(evaluationState -> true, evaluationState -> 1f, 0);
        this.states = states;
        this.activeStates = activeStates;
    }

    @Override
    public @NotNull LocalSpacePose compute(FunctionInterpolationContext context) {
        // Throw an error if the active states are empty, this should never happen but this should help with debugging.
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
        // Don't evaluate if the state machine has no states
        if(this.activeStates.isEmpty()){
            throw new IllegalStateException("State machine's active states list found to be empty. This should never happen, so something went very wrong!");
        }

        // Add to the current elapsed ticks
        super.tick(evaluationState);

        // Get the current active state
        S currentActiveStateIdentifier = this.activeStates.getLast();
        State<S> currentActiveState = this.states.get(currentActiveStateIdentifier);


        // Filter each potential state transition by whether it's valid, then filter by whether its condition predicate is true,
        // then shuffle it in order to make equal priority transitions randomized and re-order the valid transitions by filter order.
        Optional<StateTransition<S>> potentialStateTransition = currentActiveState.potentialStateTransitions.stream()
                .filter(stateTransition -> {
                    boolean transitionTargetIncludedInThisMachine = this.states.containsKey(stateTransition.target);
                    boolean targetIsNotCurrentActiveState = stateTransition.target() != currentActiveStateIdentifier;
                    if(transitionTargetIncludedInThisMachine && targetIsNotCurrentActiveState){
                        StateTransition.TransitionContext transitionContext = StateTransition.TransitionContext.of(
                                evaluationState.dataContainer(),
                                this.timeTicksElapsed,
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
        this.states.forEach((stateIdentifier, state) -> state.tick(evaluationState));

        // Evaluated last, remove states from the active state list that have a weight of 0.
        List<S> statesToRemove = this.activeStates.stream().filter((stateIdentifier) -> this.states.get(stateIdentifier).weight.getPreviousValue() == 0 && this.states.get(stateIdentifier).weight.getCurrentValue() == 0).toList();
        this.activeStates.removeAll(statesToRemove);
    }

    @Override
    public PoseFunction<LocalSpacePose> wrapUnique() {
        Builder<S> builder = this.builder();
        this.states.forEach((stateIdentifier, state) -> builder.addState(stateIdentifier, state.inputFunction.wrapUnique(), state.resetUponEntry, state.potentialStateTransitions));
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

    public static <S extends Enum<S>> Builder<S> builder(Enum<S>[] values) {
        return new Builder<>();
    }

    private Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends Enum<S>> {

        private final Map<S, State<S>> states;
        private final List<S> activeStates;


        protected Builder() {
            this.states = Maps.newHashMap();
            this.activeStates = new ArrayList<>();
        }

        /**
         * Adds a state to the state machine builder with its outgoing transitions.
         *
         * @param stateIdentifier       Enum identifier that is associated with the state machine's enum type
         * @param inputFunction         Pose function for this state
         * @param resetUponEntry        Whether to reset the functions within the state upon the state becoming active.
         * @param stateTransitions      Outbound transition paths from this state to other states
         */
        @SafeVarargs
        public final Builder<S> addState(S stateIdentifier, PoseFunction<LocalSpacePose> inputFunction, boolean resetUponEntry, StateTransition<S>... stateTransitions){
            return this.addState(stateIdentifier, inputFunction, resetUponEntry, Set.of(stateTransitions));
        }

        /**
         * Adds a state to the state machine builder with its outgoing transitions.
         *
         * @param stateIdentifier       Enum identifier that is associated with the state machine's enum type
         * @param inputFunction         Pose function for this state
         * @param resetUponEntry        Whether to reset the functions within the state upon the state becoming active.
         * @param stateTransitions      Outbound transition paths from this state to other states
         */
        public final Builder<S> addState(S stateIdentifier, PoseFunction<LocalSpacePose> inputFunction, boolean resetUponEntry, Set<StateTransition<S>> stateTransitions){
            State<S> state = new State<>(inputFunction, stateTransitions, resetUponEntry, this.states.isEmpty());

            // If the state machine already has this state defined, then throw an error.
            if(this.states.containsKey(stateIdentifier)){
                throw new IllegalStateException("Cannot add state " + stateIdentifier.toString() + " twice to the same state machine.");
            }

            // If this is the first state to be added, set it to be active.
            if (this.activeStates.isEmpty()){
                this.activeStates.add(stateIdentifier);
            }
            this.states.put(stateIdentifier, state);
            return this;
        }

        public StateMachineFunction<S> build(){
            return new StateMachineFunction<>(this.states, this.activeStates);
        }
    }

    private static class State<S extends Enum<S>> {

        private final PoseFunction<LocalSpacePose> inputFunction;
        private final Set<StateTransition<S>> potentialStateTransitions;
        private final boolean resetUponEntry;

        private boolean isActive;
        private final VariableDriver<Float> weight;
        private StateTransition<S> currentTransition;

        private State(PoseFunction<LocalSpacePose> inputFunction, Set<StateTransition<S>> potentialStateTransitions, boolean resetUponEntry, boolean isActive){
            this.inputFunction = inputFunction;
            this.potentialStateTransitions = potentialStateTransitions;
            this.resetUponEntry = resetUponEntry;

            this.isActive = isActive;
            this.weight = isActive ? VariableDriver.ofFloat(() -> 1f) : VariableDriver.ofFloat(() -> 0f);
            this.currentTransition = null;

            if(!resetUponEntry){
                for(StateTransition<S> transition : potentialStateTransitions){
                    if(transition.isAutomaticTransition()){
                        LocomotionMain.LOGGER.warn("State transition to state {} in a state machine is set to be automatic based on the input sequence player, but the origin state is not set to reset upon entry. Automatic transitions are intended to be used with reset-upon-entry states, beware of unexpected behavior!", transition.target);
                    }
                }
            }
        }

        private void tick(FunctionEvaluationState evaluationState){
            if(this.currentTransition != null){
                this.weight.prepareForNextTick();

                // Make the minimum transition time 0.01 to prevent a divide by zero error
                float increment = 1 / Math.max(this.currentTransition.transition.duration().inTicks(), 0.01f);
                float increaseDecreaseMultiplier = this.isActive ? 1 : -1;
                float nextWeightValue = this.weight.getPreviousValue() + increment * increaseDecreaseMultiplier;
                nextWeightValue = Mth.clamp(nextWeightValue, 0, 1);

                // If the state is just now becoming active after being de-active, and
                // the state is set to reset upon entry, set the evaluation state for child functions to reset.
                if(this.resetUponEntry && nextWeightValue > 0 && this.weight.getPreviousValue() == 0){
                    evaluationState = evaluationState.markedForReset();
                }
                this.weight.setValue(nextWeightValue);
            }
            if(this.weight.getCurrentValue() > 0){
                // Tick the child functions if the current weight value is greater than zero.
                this.inputFunction.tick(evaluationState);
            }
        }
    }

    public static final Predicate<StateTransition.TransitionContext> CURRENT_TRANSITION_FINISHED = transitionContext -> transitionContext.currentStateWeight() == 1;

    public static Predicate<StateTransition.TransitionContext> makeMostRelevantAnimationPlayerFinishedCondition(final float crossFadeWeight) {
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

    public record StateTransition<S extends Enum<S>> (
            S target,
            Predicate<TransitionContext> conditionPredicate,
            Transition transition,
            int priority,
            boolean isAutomaticTransition
    ) implements Comparable<StateTransition<S>> {

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
            public Builder<S> automaticallyTransitionIfAnimationPlayerFinishing(float crossFadeWeight){
                this.conditionPredicate = this.conditionPredicate.or(makeMostRelevantAnimationPlayerFinishedCondition(crossFadeWeight));
                this.automaticTransition = true;
                return this;
            }

            /**
             * Sets the transition timing properties for the state transition. Default is a single-tick linear transition.
             *
             * @param transition            The {@link Transition} to use
             */
            public Builder<S> setTransition(Transition transition){
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
            public Builder<S> setPriority(int priority){
                this.priority = priority;
                return this;
            }

            public StateTransition<S> build(){
                return new StateTransition<>(this.target, this.conditionPredicate, this.transition, this.priority, this.automaticTransition);
            }
        }

        public record TransitionContext(
                OnTickDriverContainer dataContainer,
                float ticksElapsedInCurrentState,
                float currentStateWeight,
                PoseFunction<LocalSpacePose> currentStateInput,
                TimeSpan transitionDuration
        )
        {
            public static TransitionContext of(OnTickDriverContainer dataContainer, float ticksElapsedInCurrentState, float currentStateWeight, PoseFunction<LocalSpacePose> currentStateInput, TimeSpan transitionDuration){
                return new TransitionContext(dataContainer, ticksElapsedInCurrentState, currentStateWeight, currentStateInput, transitionDuration);
            }
        }
    }
}
