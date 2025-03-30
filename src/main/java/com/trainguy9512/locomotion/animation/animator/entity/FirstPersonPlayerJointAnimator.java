package com.trainguy9512.locomotion.animation.animator.entity;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.*;
import com.trainguy9512.locomotion.animation.driver.SpringDriver;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.driver.DriverKey;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.*;
import com.trainguy9512.locomotion.animation.pose.function.cache.CachedPoseContainer;
import com.trainguy9512.locomotion.animation.joint.JointSkeleton;
import com.trainguy9512.locomotion.animation.pose.function.montage.MontageConfiguration;
import com.trainguy9512.locomotion.animation.pose.function.montage.MontageManager;
import com.trainguy9512.locomotion.animation.pose.function.montage.MontageSlotFunction;
import com.trainguy9512.locomotion.animation.pose.function.statemachine.State;
import com.trainguy9512.locomotion.animation.pose.function.statemachine.StateAlias;
import com.trainguy9512.locomotion.animation.pose.function.statemachine.StateMachineFunction;
import com.trainguy9512.locomotion.animation.pose.function.statemachine.StateTransition;
import com.trainguy9512.locomotion.util.Easing;
import com.trainguy9512.locomotion.util.TimeSpan;
import com.trainguy9512.locomotion.util.Transition;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FirstPersonPlayerJointAnimator implements LivingEntityJointAnimator<LocalPlayer, PlayerRenderState> {

    public static final String ROOT_JOINT = "root_jnt";
    public static final String CAMERA_JOINT = "camera_jnt";
    public static final String ARM_BUFFER_JOINT = "arm_buffer_jnt";
    public static final String RIGHT_ARM_BUFFER_JOINT = "arm_R_buffer_jnt";
    public static final String RIGHT_ARM_JOINT = "arm_R_jnt";
    public static final String RIGHT_HAND_JOINT = "hand_R_jnt";
    public static final String RIGHT_ITEM_JOINT = "item_R_jnt";
    public static final String LEFT_ARM_BUFFER_JOINT = "arm_L_buffer_jnt";
    public static final String LEFT_ARM_JOINT = "arm_L_jnt";
    public static final String LEFT_HAND_JOINT = "hand_L_jnt";
    public static final String LEFT_ITEM_JOINT = "item_L_jnt";


    public static final Set<String> RIGHT_SIDE_JOINTS = Set.of(
            RIGHT_ARM_BUFFER_JOINT,
            RIGHT_ARM_JOINT,
            RIGHT_HAND_JOINT,
            RIGHT_ITEM_JOINT
    );

    public static final Set<String> LEFT_SIDE_JOINTS = Set.of(
            LEFT_ARM_BUFFER_JOINT,
            LEFT_ARM_JOINT,
            LEFT_HAND_JOINT,
            LEFT_ITEM_JOINT
    );

    public static final String ADDITIVE_GROUND_MOVEMENT_CACHE = "additive_ground_movement";

    @Override
    public void postProcessModelParts(EntityModel<PlayerRenderState> entityModel, PlayerRenderState entityRenderState) {
    }

    public JointSkeleton buildSkeleton() {
        return JointSkeleton.of(ROOT_JOINT)
                .addJointUnderRoot(CAMERA_JOINT)
                .addJointUnderRoot(ARM_BUFFER_JOINT)
                .addJointUnderParent(LEFT_ARM_BUFFER_JOINT, ARM_BUFFER_JOINT)
                .addJointUnderParent(RIGHT_ARM_BUFFER_JOINT, ARM_BUFFER_JOINT)
                .addJointUnderParent(LEFT_ARM_JOINT, LEFT_ARM_BUFFER_JOINT)
                .addJointUnderParent(RIGHT_ARM_JOINT, RIGHT_ARM_BUFFER_JOINT)
                .addJointUnderParent(LEFT_HAND_JOINT, LEFT_ARM_JOINT)
                .addJointUnderParent(RIGHT_HAND_JOINT, RIGHT_ARM_JOINT)
                .addJointUnderParent(LEFT_ITEM_JOINT, LEFT_HAND_JOINT)
                .addJointUnderParent(RIGHT_ITEM_JOINT, RIGHT_HAND_JOINT)
                .setMirrorJoint(RIGHT_ARM_BUFFER_JOINT, LEFT_ARM_BUFFER_JOINT)
                .setMirrorJoint(RIGHT_ARM_JOINT, LEFT_ARM_JOINT)
                .setMirrorJoint(RIGHT_HAND_JOINT, LEFT_HAND_JOINT)
                .setMirrorJoint(RIGHT_ITEM_JOINT, LEFT_ITEM_JOINT)
                .build();
    }

    @Override
    public PoseCalculationFrequency getPoseCalulationFrequency() {
        return PoseCalculationFrequency.CALCULATE_EVERY_FRAME;
    }

    public static final ResourceLocation POSE_TEST = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "pose_test");


    public enum TestStates {
        IDLE,
        MOVING
    }

    @Override
    public PoseFunction<LocalSpacePose> constructPoseFunction(CachedPoseContainer cachedPoseContainer) {
        cachedPoseContainer.register(ADDITIVE_GROUND_MOVEMENT_CACHE, constructAdditiveGroundMovementPoseFunction(cachedPoseContainer));

        PoseFunction<LocalSpacePose> mainHandPose = this.constructHandPoseFunction(cachedPoseContainer, InteractionHand.MAIN_HAND);
        PoseFunction<LocalSpacePose> offHandPose = this.constructHandPoseFunction(cachedPoseContainer, InteractionHand.OFF_HAND);

        PoseFunction<LocalSpacePose> combinedHandPose = BlendFunction.builder(mainHandPose)
                .addBlendInput(MirrorFunction.of(offHandPose), evaluationState -> 1f, LEFT_SIDE_JOINTS)
                .build();

        PoseFunction<LocalSpacePose> handPoseWithAdditive = ApplyAdditiveFunction.of(combinedHandPose, cachedPoseContainer.getOrThrow(ADDITIVE_GROUND_MOVEMENT_CACHE));

        PoseFunction<LocalSpacePose> mirroredBasedOnHandednessPose = MirrorFunction.of(handPoseWithAdditive, context -> context.dataContainer().getDriverValue(IS_LEFT_HANDED, context.partialTicks()));

        PoseFunction<LocalSpacePose> movementDirectionOffsetTransformer =
                JointTransformerFunction.localOrParentSpaceBuilder(mirroredBasedOnHandednessPose, ARM_BUFFER_JOINT)
                        .setTranslation(
                                context -> context.dataContainer().getDriverValue(MOVEMENT_DIRECTION_OFFSET, context.partialTicks()).mul(1.5f, new Vector3f()),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .setRotationEuler(
                                context -> context.dataContainer().getDriverValue(CAMERA_ROTATION_DAMPING, context.partialTicks()).mul(-0.15f, -0.15f, 0, new Vector3f()),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .setWeight(interpolationContext -> LocomotionMain.CONFIG.data().firstPersonPlayer.enableCameraRotationDamping ? 1f : 0f)
                        .build();

        return movementDirectionOffsetTransformer;
    }



    public enum HandPoseStates {
        EMPTY,
        EMPTY_RAISE,
        EMPTY_LOWER,
        TOOL,
        TOOL_RAISE,
        TOOL_LOWER
    }

    public static final ResourceLocation HAND_LOWERED_POSE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_lowered_pose");
    public static final ResourceLocation HAND_EMPTY_POSE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_empty_pose");
    public static final ResourceLocation HAND_EMPTY_LOWER = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_empty_lower");
    public static final ResourceLocation HAND_EMPTY_RAISE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_empty_raise");
    public static final ResourceLocation HAND_TOOL_POSE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_pose");
    public static final ResourceLocation HAND_TOOL_LOWER = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_lower");
    public static final ResourceLocation HAND_TOOL_RAISE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_raise");

    public PoseFunction<LocalSpacePose> constructHandPoseFunction(CachedPoseContainer cachedPoseContainer, InteractionHand interactionHand) {

        DriverKey<VariableDriver<ItemStack>> handItemDriver = switch (interactionHand) {
            case MAIN_HAND -> MAIN_HAND_ITEM;
            case OFF_HAND -> OFF_HAND_ITEM;
        };
        DriverKey<VariableDriver<ItemStack>> renderedHandItemDriver = switch (interactionHand) {
            case MAIN_HAND -> RENDERED_MAIN_HAND_ITEM;
            case OFF_HAND -> RENDERED_OFF_HAND_ITEM;
        };
        Consumer<PoseFunction.FunctionEvaluationState> updateRenderedItem = evaluationState -> evaluationState.dataContainer().getDriver(renderedHandItemDriver).setValue(evaluationState.dataContainer().getDriverValue(handItemDriver));
        Predicate<StateTransition.TransitionContext> switchHandsCondition = context -> (interactionHand == InteractionHand.MAIN_HAND && context.dataContainer().getDriver(HOTBAR_SLOT).hasValueChanged())
                || context.dataContainer().getDriverValue(handItemDriver).getItem() != context.dataContainer().getDriverValue(renderedHandItemDriver).getItem();

        StateMachineFunction.Builder<HandPoseStates> handPoseStateMachineBuilder = StateMachineFunction.builder(evaluationState -> HandPoseStates.EMPTY_LOWER)
                .addState(State.builder(HandPoseStates.TOOL, this.handToolPoseFunction(cachedPoseContainer, interactionHand))
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.TOOL_LOWER)
                                .isTakenIfTrue(switchHandsCondition)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .addState(State.builder(HandPoseStates.TOOL_RAISE, SequencePlayerFunction.builder(HAND_TOOL_RAISE).build())
                        .resetUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.TOOL)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .addState(State.builder(HandPoseStates.TOOL_LOWER, SequencePlayerFunction.builder(HAND_TOOL_LOWER).build())
                        .resetUponEntry(true)
                        .build())
                .addStateAlias(StateAlias.builder(Set.of(
                        HandPoseStates.EMPTY_LOWER,
                        HandPoseStates.TOOL_LOWER
                ))
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY_RAISE)
                                .isTakenIfTrue(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING)
                                .setTiming(Transition.INSTANT)
                                .setPriority(30)
                                .bindToOnTransitionTaken(updateRenderedItem)
                                .build())
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.TOOL_RAISE)
                                .isTakenIfTrue(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING
                                        .and(context -> context.dataContainer().getDriverValue(handItemDriver).is(ItemTags.PICKAXES)
                                                || context.dataContainer().getDriverValue(handItemDriver).is(ItemTags.AXES)
                                                || context.dataContainer().getDriverValue(handItemDriver).is(ItemTags.SHOVELS)
                                                || context.dataContainer().getDriverValue(handItemDriver).is(ItemTags.HOES)
                                                || context.dataContainer().getDriverValue(handItemDriver).is(ItemTags.SWORDS)
                                        )
                                )
                                .setTiming(Transition.INSTANT)
                                .setPriority(40)
                                .bindToOnTransitionTaken(updateRenderedItem)
                                .build())
                        .build());

        switch (interactionHand) {
            case MAIN_HAND ->
                    handPoseStateMachineBuilder
                            .addState(State.builder(HandPoseStates.EMPTY, SequenceEvaluatorFunction.of(HAND_EMPTY_POSE))
                                    .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY_LOWER)
                                            .isTakenIfTrue(switchHandsCondition)
                                            .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT))
                                            .build())
                                    .build())
                            .addState(State.builder(HandPoseStates.EMPTY_RAISE, SequencePlayerFunction.builder(HAND_EMPTY_RAISE).build())
                                    .resetUponEntry(true)
                                    .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY)
                                            .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                                            .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT))
                                            .build())
                                    .build())
                            .addState(State.builder(HandPoseStates.EMPTY_LOWER, SequencePlayerFunction.builder(HAND_EMPTY_LOWER).build())
                                    .resetUponEntry(true)
                                    .build());
            case OFF_HAND ->
                    handPoseStateMachineBuilder
                            .addState(State.builder(HandPoseStates.EMPTY, SequenceEvaluatorFunction.of(HAND_LOWERED_POSE))
                                    .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY_LOWER)
                                            .isTakenIfTrue(switchHandsCondition)
                                            .setTiming(Transition.INSTANT)
                                            .build())
                                    .build())
                            .addState(State.builder(HandPoseStates.EMPTY_RAISE, SequencePlayerFunction.builder(HAND_LOWERED_POSE).build())
                                    .resetUponEntry(true)
                                    .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY)
                                            .isTakenIfMostRelevantAnimationPlayerFinishing(0f)
                                            .setTiming(Transition.INSTANT)
                                            .build())
                                    .build())
                            .addState(State.builder(HandPoseStates.EMPTY_LOWER, SequencePlayerFunction.builder(HAND_LOWERED_POSE).build())
                                    .resetUponEntry(true)
                                    .build());

        }

        return handPoseStateMachineBuilder.build();
    }

//    public void addStatesForHandPose(
//            StateMachineFunction.Builder<HandPoseStates> stateMachineBuilder,
//            StateAlias.Builder<HandPoseStates> fromLoweringAliasBuilder,
//            HandPoseStates poseState,
//            HandPoseStates loweringState,
//            HandPoseStates raisingState,
//            PoseFunction<LocalSpacePose> posePoseFunction,
//            PoseFunction<LocalSpacePose> loweringPoseFunction,
//            PoseFunction<LocalSpacePose> raisingPoseFunction,
//            Predicate<StateTransition.TransitionContext> switchHandsCondition,
//            Predicate<ItemStack> usePoseIfItemStackMatches,
//            DriverKey<VariableDriver<ItemStack>> itemDriverKey
//            ) {
//        stateMachineBuilder
//                .addState(State.builder(poseState, posePoseFunction)
//                        .addOutboundTransition(StateTransition.builder(loweringState)
//                                .isTakenIfTrue(switchHandsCondition)
//                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT))
//                                .build())
//                        .build())
//                .addState(State.builder(raisingState, loweringPoseFunction)
//                        .resetUponEntry(true)
//                        .addOutboundTransition(StateTransition.builder(poseState)
//                                .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
//                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT))
//                                .build())
//                        .build())
//                .addState(State.builder(loweringState, raisingPoseFunction)
//                        .resetUponEntry(true)
//                        .build());
//        fromLoweringAliasBuilder
//                .addOutboundTransition(StateTransition.builder(raisingState)
//                        .isTakenIfTrue(context -> usePoseIfItemStackMatches.test(context.dataContainer().getDriverValue(itemDriverKey)))
//                        .build());
//    }

    public static final ResourceLocation HAND_TOOL_MINE_SWING = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_mine_swing");
    public static final ResourceLocation HAND_TOOL_MINE_IMPACT = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_mine_impact");
    public static final ResourceLocation HAND_TOOL_MINE_FINISH = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_mine_finish");
    public static final ResourceLocation HAND_TOOL_ATTACK_PICKAXE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "hand_tool_attack_pickaxe");


    public PoseFunction<LocalSpacePose> handToolPoseFunction(CachedPoseContainer cachedPoseContainer, InteractionHand interactionHand) {
        return switch (interactionHand) {
            case MAIN_HAND -> MontageSlotFunction.of(
                    miningLoopPoseFunction(
                            cachedPoseContainer,
                            SequenceEvaluatorFunction.of(HAND_TOOL_POSE),
                            SequencePlayerFunction.builder(HAND_TOOL_MINE_SWING)
                                    .setPlayRate(evaluationState -> evaluationState.dataContainer().getDriverValue(MINING_SPEED_PLAY_RATE))
                                    .build(),
                            SequencePlayerFunction.builder(HAND_TOOL_MINE_IMPACT).build(),
                            SequencePlayerFunction.builder(HAND_TOOL_MINE_FINISH).build(),
                            Transition.of(TimeSpan.of60FramesPerSecond(20), Easing.SINE_OUT)
                    ),
                    "attack"
            );
            case OFF_HAND -> SequenceEvaluatorFunction.of(HAND_TOOL_POSE);
        };
    }

    public enum MiningStates {
        IDLE,
        SWING,
        IMPACT,
        FINISH
    }

    public PoseFunction<LocalSpacePose> miningLoopPoseFunction(
            CachedPoseContainer cachedPoseContainer,
            PoseFunction<LocalSpacePose> idlePoseFunction,
            PoseFunction<LocalSpacePose> swingPoseFunction,
            PoseFunction<LocalSpacePose> impactPoseFunction,
            PoseFunction<LocalSpacePose> finishPoseFunction,
            Transition idleToMiningTiming
    ) {

        return StateMachineFunction.builder(evaluationState -> MiningStates.IDLE)
                .resetUponRelevant(true)
                .addState(State.builder(MiningStates.IDLE, idlePoseFunction)
                        .addOutboundTransition(StateTransition.builder(MiningStates.SWING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_MINING))
                                .setTiming(idleToMiningTiming)
                                .build())
                        .build())
                .addState(State.builder(MiningStates.SWING, swingPoseFunction)
                        .resetUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(MiningStates.IMPACT)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(0)
                                .setTiming(Transition.INSTANT)
                                .bindToOnTransitionTaken(evaluationState -> evaluationState.dataContainer().getDriver(IS_MINING_IMPACTING).setValue(true))
                                .build())
                        .build())
                .addState(State.builder(MiningStates.IMPACT, impactPoseFunction)
                        .resetUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(MiningStates.SWING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_MINING).and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_HAS_FINISHED))
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .addOutboundTransition(StateTransition.builder(MiningStates.FINISH)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_MINING).negate().and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_HAS_FINISHED))
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .addState(State.builder(MiningStates.FINISH, finishPoseFunction)
                        .resetUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(MiningStates.IDLE)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setPriority(50)
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .addOutboundTransition(StateTransition.builder(MiningStates.SWING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_MINING).and(StateTransition.CURRENT_TRANSITION_FINISHED))
                                .setPriority(60)
                                .setTiming(idleToMiningTiming)
                                .build())
                        .build())
                .build();
    }

    public enum GroundMovementStates {
        IDLE,
        WALKING,
        STOPPING,
        JUMP,
        FALLING,
        LAND,
        SOFT_LAND
    }

    public static final ResourceLocation GROUND_MOVEMENT_POSE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_pose");
    public static final ResourceLocation GROUND_MOVEMENT_IDLE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_idle");
    public static final ResourceLocation GROUND_MOVEMENT_WALKING = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_walking");
    public static final ResourceLocation GROUND_MOVEMENT_WALK_TO_STOP = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_walk_to_stop");
    public static final ResourceLocation GROUND_MOVEMENT_JUMP = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_jump");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_falling");
    public static final ResourceLocation GROUND_MOVEMENT_LAND = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_land");

    public PoseFunction<LocalSpacePose> constructAdditiveGroundMovementPoseFunction(CachedPoseContainer cachedPoseContainer) {

        PoseFunction<LocalSpacePose> idleAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_IDLE).looping(true).build();
        PoseFunction<LocalSpacePose> walkToStopAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_WALK_TO_STOP).setPlayRate(0.6f).build();
        PoseFunction<LocalSpacePose> jumpAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_JUMP).build();
        PoseFunction<LocalSpacePose> fallingAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_FALLING).looping(true).build();
        PoseFunction<LocalSpacePose> walkingBlendSpacePlayer = BlendSpace1DPlayerFunction.builder(evaluationState -> evaluationState.dataContainer().getDriverValue(MODIFIED_WALK_SPEED))
                .addEntry(0f, GROUND_MOVEMENT_WALKING, 0.5f)
                .addEntry(0.86f, GROUND_MOVEMENT_WALKING, 2.25f)
                .addEntry(1f, GROUND_MOVEMENT_WALKING, 3.5f)
                .build();

        PoseFunction<LocalSpacePose> landAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_LAND).build();
        PoseFunction<LocalSpacePose> softLandAnimationPlayer = BlendFunction.builder(SequenceEvaluatorFunction.of(GROUND_MOVEMENT_POSE, TimeSpan.ofSeconds(0)))
                .addBlendInput(SequencePlayerFunction.builder(GROUND_MOVEMENT_LAND).setPlayRate(1f).build(), evaluationState -> 0.5f)
                .build();

        Predicate<StateTransition.TransitionContext> walkingCondition = transitionContext -> transitionContext.dataContainer().getDriverValue(IS_MOVING);


        PoseFunction<LocalSpacePose> movementStateMachine = StateMachineFunction.builder(evaluationState -> GroundMovementStates.IDLE)
                .addState(State.builder(GroundMovementStates.IDLE, idleAnimationPlayer)
                        .resetUponEntry(false)
                        // Begin walking if the player is moving horizontally
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(
                                        walkingCondition
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED))
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.3f), Easing.SINE_OUT))
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.WALKING, walkingBlendSpacePlayer)
                        .resetUponEntry(false)
                        // Stop walking with the walk-to-stop animation if the player's already been walking for a bit.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.STOPPING)
                                .isTakenIfTrue(walkingCondition.negate()
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED))
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.SINE_IN_OUT))
                                .build())
                        // Stop walking directly into the idle animation if the player only just began walking.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.IDLE)
                                .isTakenIfTrue(walkingCondition.negate()
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED.negate()))
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.3f), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.STOPPING, walkToStopAnimationPlayer)
                        .resetUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.IDLE)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.3f), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.JUMP, jumpAnimationPlayer)
                        .resetUponEntry(true)
                        // Automatically move into the falling animation player
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.FALLING)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        // If the player lands before it can move into the falling animation, go straight to the landing animation as long as the jump state is fully transitioned.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.LAND)
                                .isTakenIfTrue(StateTransition.CURRENT_TRANSITION_FINISHED
                                        .and(StateTransition.booleanDriverPredicate(IS_GROUNDED))
                                )
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.FALLING, fallingAnimationPlayer)
                        .resetUponEntry(true)
                        // Move into the landing animation if the player is no longer falling
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.LAND)
                                .isTakenIfTrue(
                                        StateTransition.booleanDriverPredicate(IS_GROUNDED)
                                )
                                .setTiming(Transition.of(TimeSpan.ofTicks(1), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        // Move into the landing animation if the player is no longer falling, but only just began falling.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.SOFT_LAND)
                                .isTakenIfTrue(
                                        StateTransition.booleanDriverPredicate(IS_GROUNDED)
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED.negate())
                                )
                                .setTiming(Transition.of(TimeSpan.ofTicks(1), Easing.LINEAR))
                                .setPriority(60)
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.SOFT_LAND, softLandAnimationPlayer)
                        .resetUponEntry(true)
                        .build())
                .addState(State.builder(GroundMovementStates.LAND, landAnimationPlayer)
                        .resetUponEntry(true)
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                GroundMovementStates.IDLE,
                                GroundMovementStates.WALKING,
                                GroundMovementStates.STOPPING,
                                GroundMovementStates.LAND,
                                GroundMovementStates.SOFT_LAND
                        ))
                        // Transition to the jumping animation if the player is jumping.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.JUMP)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_JUMPING)
                                        .and(StateTransition.booleanDriverPredicate(IS_GROUNDED).negate()))
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(60)
                                .build())
                        // Transition to the jumping animation if the player is falling.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.FALLING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_GROUNDED).negate())
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.LINEAR))
                                .setPriority(50)
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                                Set.of(
                                        GroundMovementStates.LAND,
                                        GroundMovementStates.SOFT_LAND
                                ))
                        // If the falling animation is finishing and the player is not walking, play the idle animation.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.IDLE)
                                .isTakenIfTrue(walkingCondition.negate().and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING))
                                .setTiming(Transition.of(TimeSpan.of30FramesPerSecond(9), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        // If the falling animation is finishing and the player is walking, play the walking animation.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(walkingCondition.and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING))
                                .setTiming(Transition.of(TimeSpan.of30FramesPerSecond(9), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        .build())
                .build();

        return MakeDynamicAdditiveFunction.of(
                movementStateMachine,
                SequenceEvaluatorFunction.of(GROUND_MOVEMENT_POSE, TimeSpan.ofSeconds(0))
        );
    }




    public static final DriverKey<SpringDriver<Vector3f>> MOVEMENT_DIRECTION_OFFSET = DriverKey.of("movement_direction_offset", () -> SpringDriver.ofVector3f(0.5f, 0.6f, 1f, Vector3f::new, false));
    public static final DriverKey<SpringDriver<Vector3f>> CAMERA_ROTATION_DAMPING = DriverKey.of("camera_rotation_damping", () -> SpringDriver.ofVector3f(LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationStiffnessFactor, LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationDampingFactor, 1f, Vector3f::new, true));

    public static final DriverKey<VariableDriver<Integer>> HOTBAR_SLOT = DriverKey.of("hotbar_slot", () -> VariableDriver.ofConstant(() -> 0));
    public static final DriverKey<VariableDriver<ItemStack>> MAIN_HAND_ITEM = DriverKey.of("main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> OFF_HAND_ITEM = DriverKey.of("off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_MAIN_HAND_ITEM = DriverKey.of("rendered_main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_OFF_HAND_ITEM = DriverKey.of("rendered_off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));

    public static final DriverKey<VariableDriver<Float>> HORIZONTAL_MOVEMENT_SPEED = DriverKey.of("horizontal_movement_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Float>> MODIFIED_WALK_SPEED = DriverKey.of("modified_walk_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Boolean>> IS_MOVING = DriverKey.of("is_moving", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_GROUNDED = DriverKey.of("is_grounded", () -> VariableDriver.ofBoolean(() -> true));
    public static final DriverKey<VariableDriver<Boolean>> IS_JUMPING = DriverKey.of("is_jumping", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_LEFT_HANDED = DriverKey.of("is_left_handed", () -> VariableDriver.ofBoolean(() -> false));

    public static final DriverKey<VariableDriver<Boolean>> IS_MINING = DriverKey.of("is_mining", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Float>> MINING_SPEED_PLAY_RATE = DriverKey.of("mining_speed_play_rate", () -> VariableDriver.ofFloat(() -> 1f));
    public static final DriverKey<VariableDriver<Boolean>> IS_MINING_IMPACTING = DriverKey.of("is_mining_impacting", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_ATTACKING = DriverKey.of("is_attacking", () -> VariableDriver.ofBoolean(() -> false));

    public static final MontageConfiguration HAND_TOOL_ATTACK_PICKAXE_MONTAGE = MontageConfiguration.builder("hand_tool_attack_pickaxe_montage", HAND_TOOL_ATTACK_PICKAXE)
            .playsInSlot("attack")
            .setTransitionIn(Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT))
            .setTransitionOut(Transition.of(TimeSpan.of60FramesPerSecond(15), Easing.SINE_IN_OUT))
            .build();

    @Override
    public void extractAnimationData(LocalPlayer dataReference, OnTickDriverContainer driverContainer, MontageManager montageManager){

        driverContainer.getDriver(MODIFIED_WALK_SPEED).setValue(dataReference.walkAnimation.speed());
        driverContainer.getDriver(HORIZONTAL_MOVEMENT_SPEED).setValue(new Vector3f((float) (dataReference.getX() - dataReference.xo), 0.0f, (float) (dataReference.getZ() - dataReference.zo)).length());

        driverContainer.getDriver(HOTBAR_SLOT).setValue(dataReference.getInventory().getSelectedSlot());
        driverContainer.getDriver(MAIN_HAND_ITEM).setValue(dataReference.getMainHandItem());
        driverContainer.getDriver(OFF_HAND_ITEM).setValue(dataReference.getOffhandItem());
        driverContainer.getDriver(IS_MINING_IMPACTING).setValue(false);

        if (driverContainer.getDriver(IS_ATTACKING).getCurrentValue()) {
            montageManager.playMontage(HAND_TOOL_ATTACK_PICKAXE_MONTAGE, driverContainer);
            driverContainer.getDriver(IS_ATTACKING).setValue(false);
        }


        ItemStack item = driverContainer.getDriverValue(RENDERED_MAIN_HAND_ITEM);
        float miningSpeed = 2f;
        if (item.is(ItemTags.PICKAXES)) {
            miningSpeed = item.getDestroySpeed(Blocks.STONE.defaultBlockState());
        }
        if (item.is(ItemTags.AXES)) {
            miningSpeed = item.getDestroySpeed(Blocks.OAK_PLANKS.defaultBlockState());
        }
        if (item.is(ItemTags.SHOVELS)) {
            miningSpeed = item.getDestroySpeed(Blocks.DIRT.defaultBlockState());
        }
        if (item.is(ItemTags.HOES)) {
            miningSpeed = item.getDestroySpeed(Blocks.HAY_BLOCK.defaultBlockState());
        }
        miningSpeed -= 2f;
        miningSpeed *= 0.075f;
        miningSpeed += 1f;
        driverContainer.getDriver(MINING_SPEED_PLAY_RATE).setValue(miningSpeed);


        // Debug
        //driverContainer.getDriver(RENDERED_MAIN_HAND_ITEM).setValue(driverContainer.getDriverValue(MAIN_HAND_ITEM));
        //driverContainer.getDriver(RENDERED_OFF_HAND_ITEM).setValue(driverContainer.getDriverValue(OFF_HAND_ITEM));

        driverContainer.getDriver(IS_MOVING).setValue(dataReference.input.keyPresses.forward() || dataReference.input.keyPresses.backward() || dataReference.input.keyPresses.left() || dataReference.input.keyPresses.right());
        driverContainer.getDriver(IS_GROUNDED).setValue(dataReference.onGround());
        driverContainer.getDriver(IS_JUMPING).setValue(dataReference.input.keyPresses.jump());
        driverContainer.getDriver(IS_LEFT_HANDED).setValue(dataReference.getMainArm() == HumanoidArm.LEFT);

        Vector3f velocity = new Vector3f((float) (dataReference.getX() - dataReference.xo), (float) (dataReference.getY() - dataReference.yo), (float) (dataReference.getZ() - dataReference.zo));
        // We don't want vertical velocity to be factored into the movement direction offset as much as the horizontal velocity.
        velocity.mul(1, 0.25f, 1);
        Quaternionf rotation = new Quaternionf().rotationYXZ(Mth.PI - dataReference.getYRot() * Mth.DEG_TO_RAD, -dataReference.getXRot() * Mth.DEG_TO_RAD, 0.0F);
        Vector3f movementDirection = new Vector3f(
                velocity.dot(new Vector3f(1, 0, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 1, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 0, -1).rotate(rotation))
        );
        movementDirection.mul(dataReference.isSprinting() ? 4f : 3f);
        driverContainer.getDriver(MOVEMENT_DIRECTION_OFFSET).setValue(movementDirection);
        driverContainer.getDriver(CAMERA_ROTATION_DAMPING).setValue(new Vector3f(dataReference.getXRot(), dataReference.getYRot(), dataReference.getYRot()).mul(Mth.DEG_TO_RAD));

    }
}
