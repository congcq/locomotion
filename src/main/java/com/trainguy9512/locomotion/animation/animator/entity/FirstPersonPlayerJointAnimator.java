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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;
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

    public static final Set<String> ARM_BUFFER_JOINTS = Set.of(
            RIGHT_ARM_BUFFER_JOINT,
            LEFT_ARM_BUFFER_JOINT
    );

    public static final Set<String> ARM_JOINTS = Set.of(
            RIGHT_ARM_JOINT,
            LEFT_ARM_JOINT,
            RIGHT_ARM_BUFFER_JOINT,
            LEFT_ARM_BUFFER_JOINT,
            RIGHT_HAND_JOINT,
            LEFT_HAND_JOINT
    );

    public static final Set<String> ARM_POSE_JOINTS = Set.of(
            RIGHT_ARM_JOINT,
            LEFT_ARM_JOINT
    );

    public static final Set<String> HAND_JOINTS = Set.of(
            RIGHT_HAND_JOINT,
            LEFT_HAND_JOINT
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
        PoseFunction<LocalSpacePose> placeholderHandPose = SequenceEvaluatorFunction.of(POSE_TEST, TimeSpan.of30FramesPerSecond(70));
        PoseFunction<LocalSpacePose> handPoseWithAdditive = ApplyAdditiveFunction.of(placeholderHandPose, cachedPoseContainer.getOrThrow(ADDITIVE_GROUND_MOVEMENT_CACHE));



        PoseFunction<LocalSpacePose> movementDirectionOffsetTransformer =
                JointTransformerFunction.localOrParentSpaceBuilder(handPoseWithAdditive, ARM_BUFFER_JOINT)
                        .setTranslation(
                                context -> context.dataContainer().getDriverValue(MOVEMENT_DIRECTION_OFFSET, context.partialTicks()).mul(1.5f),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .setRotationEuler(
                                context -> context.dataContainer().getDriverValue(CAMERA_ROTATION_DAMPING, context.partialTicks()).mul(-0.15f, -0.15f, 0),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .setWeight(interpolationContext -> LocomotionMain.CONFIG.data().firstPersonPlayer.enableCameraRotationDamping ? 1f : 0f)
                        .build();

        return movementDirectionOffsetTransformer;
    }




    public enum GroundMovementStates {
        IDLE,
        WALKING,
        STOPPING,
        JUMP,
        FALLING,
        LAND
    }

    public static final ResourceLocation GROUND_MOVEMENT_POSE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_pose");
    public static final ResourceLocation GROUND_MOVEMENT_IDLE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_idle");
    public static final ResourceLocation GROUND_MOVEMENT_WALKING = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_walking");
    public static final ResourceLocation GROUND_MOVEMENT_WALK_TO_STOP = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_walk_to_stop");
    public static final ResourceLocation GROUND_MOVEMENT_JUMP = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_jump");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_falling");
    public static final ResourceLocation GROUND_MOVEMENT_LAND = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "ground_movement_land");

    public PoseFunction<LocalSpacePose> constructAdditiveGroundMovementPoseFunction(CachedPoseContainer cachedPoseContainer) {

        PoseFunction<LocalSpacePose> idleAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_IDLE).looping().build();
        PoseFunction<LocalSpacePose> walkToStopAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_WALK_TO_STOP).setPlayRate(0.6f).build();
        PoseFunction<LocalSpacePose> jumpAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_JUMP).build();
        PoseFunction<LocalSpacePose> fallingAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_FALLING).looping().build();
        PoseFunction<LocalSpacePose> landAnimationPlayer = SequencePlayerFunction.builder(GROUND_MOVEMENT_LAND).build();
        PoseFunction<LocalSpacePose> walkingBlendSpacePlayer = BlendSpace1DPlayerFunction.builder(evaluationState -> evaluationState.dataContainer().getDriverValue(MODIFIED_WALK_SPEED))
                .addEntry(0f, GROUND_MOVEMENT_WALKING, 0.5f)
                .addEntry(0.86f, GROUND_MOVEMENT_WALKING, 2.25f)
                .addEntry(1f, GROUND_MOVEMENT_WALKING, 3.5f)
                .build();


        Predicate<StateTransition.TransitionContext> walkingCondition = transitionContext -> transitionContext.dataContainer().getDriverValue(IS_MOVING);


        PoseFunction<LocalSpacePose> movementStateMachine = StateMachineFunction.builder(dataContainer -> GroundMovementStates.IDLE)
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
                        .resetUponEntry(true)
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
                                        .and(StateTransition.booleanDriverPredicate(IS_GROUNDED)))
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.FALLING, fallingAnimationPlayer)
                        .resetUponEntry(true)
                        // Move into the landing animation if the player is no longer falling
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.LAND)
                                .isTakenIfTrue(context -> context.dataContainer().getDriverValue(IS_GROUNDED))
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(50)
                                .build())
                        // Move into the jumping animation if the player is no longer falling, but is also jumping.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.JUMP)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_GROUNDED)
                                        .and(StateTransition.booleanDriverPredicate(IS_JUMPING)))
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(60)
                                .build())
                        .build())
                .addState(State.builder(GroundMovementStates.LAND, landAnimationPlayer)
                        .resetUponEntry(true)
                        // If the falling animation is finishing and the player is not walking, play the idle animation.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.IDLE)
                                .isTakenIfTrue(walkingCondition.negate().and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING))
                                .setTiming(Transition.of(TimeSpan.of30FramesPerSecond(5), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        // If the falling animation is finishing and the player is walking, play the walking animation.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(walkingCondition.and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING))
                                .setTiming(Transition.of(TimeSpan.of30FramesPerSecond(5), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                GroundMovementStates.IDLE,
                                GroundMovementStates.WALKING,
                                GroundMovementStates.STOPPING,
                                GroundMovementStates.LAND
                        ))
                        // Transition to the jumping animation if the player is jumping.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.JUMP)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_JUMPING))
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(60)
                                .build())
                        // Transition to the jumping animation if the player is falling.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.FALLING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_GROUNDED).negate())
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(50)
                                .build())
                        .build())
                .build();

        return MakeDynamicAdditiveFunction.of(
                movementStateMachine,
                SequenceEvaluatorFunction.of(GROUND_MOVEMENT_POSE, TimeSpan.ofSeconds(0))
        );
    }




    public static final DriverKey<SpringDriver<Vector3f>> MOVEMENT_DIRECTION_OFFSET = DriverKey.of("movement_direction_offset", () -> SpringDriver.ofVector(0.5f, 0.6f, 1f, Vector3f::new, false));
    public static final DriverKey<SpringDriver<Vector3f>> CAMERA_ROTATION_DAMPING = DriverKey.of("camera_rotation_damping", () -> SpringDriver.ofVector(LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationStiffnessFactor, LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationDampingFactor, 1f, Vector3f::new, true));

    public static final DriverKey<VariableDriver<ItemStack>> MAIN_HAND_ITEM = DriverKey.of("main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> OFF_HAND_ITEM = DriverKey.of("off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_MAIN_HAND_ITEM = DriverKey.of("rendered_main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_OFF_HAND_ITEM = DriverKey.of("rendered_off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));

    public static final DriverKey<VariableDriver<Float>> HORIZONTAL_MOVEMENT_SPEED = DriverKey.of("horizontal_movement_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Float>> MODIFIED_WALK_SPEED = DriverKey.of("modified_walk_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Boolean>> IS_MOVING = DriverKey.of("is_moving", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_GROUNDED = DriverKey.of("is_grounded", () -> VariableDriver.ofBoolean(() -> true));
    public static final DriverKey<VariableDriver<Boolean>> IS_JUMPING = DriverKey.of("is_jumping", () -> VariableDriver.ofBoolean(() -> false));


    @Override
    public void extractAnimationData(LocalPlayer dataReference, OnTickDriverContainer driverContainer){


        driverContainer.getDriver(MODIFIED_WALK_SPEED).setValue(dataReference.walkAnimation.speed());
        driverContainer.getDriver(HORIZONTAL_MOVEMENT_SPEED).setValue(new Vector3f((float) (dataReference.getX() - dataReference.xo), 0.0f, (float) (dataReference.getZ() - dataReference.zo)).length());
        driverContainer.getDriver(MAIN_HAND_ITEM).setValue(dataReference.getMainHandItem());
        driverContainer.getDriver(OFF_HAND_ITEM).setValue(dataReference.getOffhandItem());

        //DEBUG
        driverContainer.getDriver(RENDERED_MAIN_HAND_ITEM).setValue(driverContainer.getDriverValue(MAIN_HAND_ITEM));
        driverContainer.getDriver(RENDERED_OFF_HAND_ITEM).setValue(driverContainer.getDriverValue(OFF_HAND_ITEM));

        driverContainer.getDriver(IS_MOVING).setValue(dataReference.input.keyPresses.forward() || dataReference.input.keyPresses.backward() || dataReference.input.keyPresses.left() || dataReference.input.keyPresses.right());
        driverContainer.getDriver(IS_GROUNDED).setValue(dataReference.onGround());
        driverContainer.getDriver(IS_JUMPING).setValue(dataReference.input.keyPresses.jump());

        Vector3f velocity = new Vector3f((float) (dataReference.getX() - dataReference.xo), (float) (dataReference.getY() - dataReference.yo), (float) (dataReference.getZ() - dataReference.zo));
        // We don't want vertical velocity to be factored into the movement direction offset as much as the horizontal velocity.
        velocity.mul(1, 0.25f, 1);
        Quaternionf rotation = new Quaternionf().rotationYXZ(Mth.PI - dataReference.getYRot() * Mth.DEG_TO_RAD, -dataReference.getXRot() * Mth.DEG_TO_RAD, 0.0F);
        Vector3f movementDirection = new Vector3f(
                velocity.dot(new Vector3f(1, 0, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 1, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 0, -1).rotate(rotation))
        );
        movementDirection.mul(dataReference.isSprinting() ? 5f : 3f);
        driverContainer.getDriver(MOVEMENT_DIRECTION_OFFSET).setValue(movementDirection);
        driverContainer.getDriver(CAMERA_ROTATION_DAMPING).setValue(new Vector3f(dataReference.getXRot(), dataReference.getYRot(), dataReference.getYRot()).mul(Mth.DEG_TO_RAD));

    }
}
