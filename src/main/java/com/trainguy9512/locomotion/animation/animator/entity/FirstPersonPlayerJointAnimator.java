package com.trainguy9512.locomotion.animation.animator.entity;

import com.trainguy9512.locomotion.animation.data.*;
import com.trainguy9512.locomotion.animation.driver.SpringDriver;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.driver.DriverKey;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.*;
import com.trainguy9512.locomotion.animation.pose.function.cache.CachedPoseContainer;
import com.trainguy9512.locomotion.animation.joint.JointSkeleton;
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

import java.util.Random;
import java.util.Set;

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

    public static final DriverKey<VariableDriver<Float>> TIME_TEST = DriverKey.of("time_test", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<SpringDriver<Float>> SPRING_TEST = DriverKey.of("spring", () -> SpringDriver.ofFloat(0.0f, 0.7f, 1, () -> 0f, false));

    public static final DriverKey<SpringDriver<Vector3f>> MOVEMENT_DIRECTION_OFFSET = DriverKey.of("movement_direction_offset", () -> SpringDriver.ofVector(0.7f, 0.6f, 1f, Vector3f::new, false));
    public static final DriverKey<SpringDriver<Vector3f>> CAMERA_ROTATION_DAMPING = DriverKey.of("camera_rotation_lag", () -> SpringDriver.ofVector(0.5f, 0.8f, 1f, Vector3f::new, true));

    public static final DriverKey<VariableDriver<ItemStack>> MAIN_HAND_ITEM = DriverKey.of("main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> OFF_HAND_ITEM = DriverKey.of("off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_MAIN_HAND_ITEM = DriverKey.of("rendered_main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_OFF_HAND_ITEM = DriverKey.of("rendered_off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));

    public static final DriverKey<VariableDriver<Float>> WALK_SPEED = DriverKey.of("walk_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Boolean>> IS_GROUNDED = DriverKey.of("is_grounded", () -> VariableDriver.ofBoolean(() -> false));

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

    public static final ResourceLocation ROM_TEST = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "rom_test");
    public static final ResourceLocation POSE_TEST = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "pose_test");
    public static final ResourceLocation ADDITIVE_TEST_BASE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "additive_test_base");
    public static final ResourceLocation ADDITIVE_TEST_ADDITIVE = AnimationSequenceData.getNativeResourceLocation(AnimationSequenceData.FIRST_PERSON_PLAYER_PATH, "additive_test_additive");

    public enum TestStates {
        IDLE,
        MOVING
    }

    @Override
    public PoseFunction<LocalSpacePose> constructPoseFunction(CachedPoseContainer cachedPoseContainer) {
        Random random = new Random();
        PoseFunction<LocalSpacePose> testSequencePlayer = SequencePlayerFunction.builder(ROM_TEST).setLooping(true).setPlayRate((context) -> 0f).build();
        PoseFunction<LocalSpacePose> movingSequencePlayer = SequencePlayerFunction.builder(POSE_TEST)
                .setLooping(true)
                .setPlayRate((context) -> 1f)
                .bindToTimeMarker("clear_main_hand_item", evaluationState ->
                        evaluationState.dataContainer().getDriver(RENDERED_MAIN_HAND_ITEM).setValue(ItemStack.EMPTY))
                .bindToTimeMarker("switch_main_hand_item", evaluationState ->
                        evaluationState.dataContainer().getDriver(RENDERED_MAIN_HAND_ITEM).setValue(evaluationState.dataContainer().getDriverValue(MAIN_HAND_ITEM)))
                .build();
        //cachedPoseContainer.register("TEST_SEQ_PLAYER", testSequencePlayer);


        PoseFunction<LocalSpacePose> testIdlePlayer = SequenceEvaluatorFunction.of(POSE_TEST, context -> TimeSpan.ofSeconds(0));
        PoseFunction<LocalSpacePose> testMovingPlayer = SequencePlayerFunction.builder(ADDITIVE_TEST_ADDITIVE).build();

        PoseFunction<LocalSpacePose> testStateMachine = StateMachineFunction.builder(TestStates.values())
                .addState(TestStates.IDLE, testIdlePlayer, true,
                        StateMachineFunction.StateTransition.builder(TestStates.MOVING,
                                        transitionContext -> transitionContext.dataContainer().getDriverValue(WALK_SPEED) >= 0.2f,
                                        StateMachineFunction.CURRENT_TRANSITION_FINISHED)
                                .setTransition(Transition.INSTANT)
                                .build()
                )
                .addState(TestStates.MOVING, testMovingPlayer, true,
                        StateMachineFunction.StateTransition.builder(TestStates.IDLE, context -> false)
                                .automaticallyTransitionIfAnimationPlayerFinishing(1f)
                                .setTransition(Transition.of(TimeSpan.of24FramesPerSecond(7), Easing.ELASTIC_OUT))
                                .build()
                )
                .build();




        PoseFunction<LocalSpacePose> additivePoseFunction = ComposeAdditiveFunction.of(
                SequencePlayerFunction.builder(ADDITIVE_TEST_BASE).setLooping(true).setPlayRate(evaluationState -> 0.25f).build(),
                BlendSpace1DPlayerFunction.builder(evaluationState -> evaluationState.dataContainer().getDriverValue(WALK_SPEED))
                        .addEntry(0, ADDITIVE_TEST_ADDITIVE, 1)
                        .addEntry(1, ADDITIVE_TEST_ADDITIVE, 2)
                        .build(),
                SequenceEvaluatorFunction.of(ADDITIVE_TEST_ADDITIVE, TimeSpan.ofSeconds(0))
        );




        PoseFunction<LocalSpacePose> movementDirectionOffsetTransformer = LocalConversionFunction.of(
                JointTransformerFunction.componentSpaceBuilder(ComponentConversionFunction.of(
                        testStateMachine
                                ),
                                ARM_BUFFER_JOINT)
                        .setTranslation(
                                context -> context.dataContainer().getDriverValue(MOVEMENT_DIRECTION_OFFSET, context.partialTicks()),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .setRotationEuler(
                                context -> context.dataContainer().getDriverValue(CAMERA_ROTATION_DAMPING, context.partialTicks()).mul(-0.15f, -0.15f, -0.075f),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .build());

        //PoseFunction<LocalSpacePose> cached = cachedPoseContainer.getOrThrow("TEST_SEQ_PLAYER");
        PoseFunction<LocalSpacePose> blendMultipleFunction = BlendFunction.builder(testSequencePlayer).addBlendInput(testSequencePlayer, (evaluationState) -> 0.5f).build();

        return movementDirectionOffsetTransformer;
    }


    @Override
    public void extractAnimationData(LocalPlayer dataReference, OnTickDriverContainer driverContainer){
        driverContainer.getDriver(SPRING_TEST).setValue(dataReference.walkAnimation.speed());

        driverContainer.getDriver(WALK_SPEED).setValue(dataReference.walkAnimation.speed());
        driverContainer.getDriver(TIME_TEST).setValue(driverContainer.getDriver(TIME_TEST).getPreviousValue() + 1);
        driverContainer.getDriver(MAIN_HAND_ITEM).setValue(dataReference.getMainHandItem());
        driverContainer.getDriver(OFF_HAND_ITEM).setValue(dataReference.getOffhandItem());

        driverContainer.getDriver(IS_GROUNDED).setValue(dataReference.onGround());

        Vector3f velocity = new Vector3f((float) (dataReference.getX() - dataReference.xo), (float) (dataReference.getY() - dataReference.yo), (float) (dataReference.getZ() - dataReference.zo));
        velocity.mul(1, 0.5f, 1);
        Quaternionf rotation = new Quaternionf().rotationYXZ(Mth.PI - dataReference.getYRot() * Mth.DEG_TO_RAD, -dataReference.getXRot() * Mth.DEG_TO_RAD, 0.0F);
        Vector3f movementDirection;
        movementDirection = new Vector3f(
                velocity.dot(new Vector3f(1, 0, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 1, 0).rotate(rotation)),
                velocity.dot(new Vector3f(0, 0, -1).rotate(rotation))
        );
        movementDirection.mul(dataReference.isSprinting() ? 5f : 3f);
        driverContainer.getDriver(MOVEMENT_DIRECTION_OFFSET).setValue(movementDirection);
        driverContainer.getDriver(CAMERA_ROTATION_DAMPING).setValue(new Vector3f(dataReference.getXRot(), dataReference.getYRot(), dataReference.getYRot()).mul(Mth.DEG_TO_RAD));

    }
}
