package com.trainguy9512.locomotion.animation.animator.entity;

import com.mojang.math.Axis;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.*;
import com.trainguy9512.locomotion.animation.driver.SpringDriver;
import com.trainguy9512.locomotion.animation.driver.VariableDriver;
import com.trainguy9512.locomotion.animation.driver.DriverKey;
import com.trainguy9512.locomotion.animation.pose.AnimationPose;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.*;
import com.trainguy9512.locomotion.animation.pose.function.cache.SavedCachedPoseContainer;
import com.trainguy9512.locomotion.animation.joint.JointSkeleton;
import com.trainguy9512.locomotion.util.Easing;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
    public static final DriverKey<SpringDriver<Float>> SPRING_TEST = DriverKey.of("spring", () -> SpringDriver.ofFloat(1.0f, 0.4f, 1, () -> 0f, false));

    public static final DriverKey<SpringDriver<Vector3f>> MOVEMENT_DIRECTION_OFFSET = DriverKey.of("movement_direction_offset", () -> SpringDriver.ofVector(0.7f, 0.6f, 1f, () -> new Vector3f(0f), false));

    public static final DriverKey<VariableDriver<Vector3f>> CAMERA_ROTATION = DriverKey.of("camera_rotation", () -> VariableDriver.ofVector(() -> new Vector3f(0)));
    public static final DriverKey<VariableDriver<Vector3f>> DAMPENED_CAMERA_ROTATION = DriverKey.of("dampened_camera_rotation", () -> VariableDriver.ofVector(() -> new Vector3f(0)));
    public static final DriverKey<VariableDriver<ItemStack>> MAIN_HAND_ITEM = DriverKey.of("main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> OFF_HAND_ITEM = DriverKey.of("off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<Float>> WALK_SPEED = DriverKey.of("walk_speed", () -> VariableDriver.ofFloat(() -> 0f));

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

    public enum TestStates {
        IDLE,
        MOVING
    }

    @Override
    public PoseFunction<LocalSpacePose> constructPoseFunction(SavedCachedPoseContainer cachedPoseContainer) {
        Random random = new Random();
        PoseFunction<LocalSpacePose> testSequencePlayer = SequencePlayerFunction.builder(ROM_TEST).setLooping(true).setPlayRate((context) -> 0f).build();
        PoseFunction<LocalSpacePose> movingSequencePlayer = SequencePlayerFunction.builder(ROM_TEST).setLooping(true).setPlayRate((context) -> 1f).build();
        //cachedPoseContainer.register("TEST_SEQ_PLAYER", testSequencePlayer);


        PoseFunction<LocalSpacePose> testStateMachine = StateMachineFunction.builder(TestStates.values())
                .addState(TestStates.IDLE, testSequencePlayer, true,
                        StateMachineFunction.StateTransition.builder(TestStates.MOVING,
                                        transitionContext -> transitionContext.dataContainer().getDriverValue(WALK_SPEED) >= 0.5f,
                                        StateMachineFunction.CURRENT_TRANSITION_FINISHED)
                                .setTransitionDuration(TimeSpan.ofSeconds(0.01f))
                                .setEasing(Easing.SINE_IN_OUT)
                                .build()
                )
                .addState(TestStates.MOVING, movingSequencePlayer, true,
                        StateMachineFunction.StateTransition.builder(TestStates.IDLE, context -> false)
                                .makeAutomaticTransitionBasedOnActiveSequencePlayer()
                                .setTransitionDuration(TimeSpan.ofSeconds(0.5f))
                                .setEasing(Easing.easeOut(Easing.Elastic.easeInOf(6, 3)))
                                .build()
                )
                .build();

        PoseFunction<LocalSpacePose> movementDirectionOffsetTransformer = LocalConversionFunction.of(
                JointTransformerFunction.componentSpaceBuilder(ComponentConversionFunction.of(testStateMachine), ARM_BUFFER_JOINT)
                        .setTranslation(
                                context -> context.dataContainer().getDriverValue(MOVEMENT_DIRECTION_OFFSET, context.partialTicks()),
                                JointChannel.TransformType.ADD,
                                JointChannel.TransformSpace.COMPONENT
                        )
                        .build());

        //PoseFunction<LocalSpacePose> cached = cachedPoseContainer.getOrThrow("TEST_SEQ_PLAYER");
        PoseFunction<LocalSpacePose> blendMultipleFunction = BlendMultipleFunction.builder(testSequencePlayer).addBlendInput(testSequencePlayer, (evaluationState) -> 0.5f).build();

        return movementDirectionOffsetTransformer;
    }


    @Override
    public void extractAnimationData(LocalPlayer dataReference, OnTickDriverContainer driverContainer){
        driverContainer.getDriver(SPRING_TEST).setValue(dataReference.walkAnimation.speed());

        driverContainer.getDriver(WALK_SPEED).setValue(dataReference.walkAnimation.speed());
        driverContainer.getDriver(TIME_TEST).setValue(driverContainer.getDriver(TIME_TEST).getPreviousValue() + 1);
        driverContainer.getDriver(MAIN_HAND_ITEM).setValue(dataReference.getMainHandItem());
        driverContainer.getDriver(OFF_HAND_ITEM).setValue(dataReference.getOffhandItem());


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




        //Tick the dampened camera rotation.
        Vector3f dampenSpeed = new Vector3f(0.5F, 0.5F, 0.2F);

        // First, set the target camera rotation from the living entity.
        Vector3f targetRotation = new Vector3f(dataReference.getXRot(), dataReference.getYRot(), dataReference.getYRot());
        driverContainer.getDriver(CAMERA_ROTATION).setValue(targetRotation);


        Vector3f dampenedCameraRotation = new Vector3f(driverContainer.getDriver(DAMPENED_CAMERA_ROTATION).getPreviousValue());


        // If the dampened camera rotation is 0 (which is what it is upon initialization), set it to the target
        if(dampenedCameraRotation.x() == 0F && dampenedCameraRotation.y() == 0F){
            dampenedCameraRotation = targetRotation;
        } else {
            // Lerp the dampened camera rotation towards the normal camera rotation
            dampenedCameraRotation.set(
                    Mth.lerp(dampenSpeed.x(), dampenedCameraRotation.x(), targetRotation.x()),
                    Mth.lerp(dampenSpeed.y(), dampenedCameraRotation.y(), targetRotation.y()),
                    Mth.lerp(dampenSpeed.z(), dampenedCameraRotation.z(), targetRotation.z())
            );
            //dampenedCameraRotation.lerp(targetRotation, 0.5F);
        }
        driverContainer.getDriver(DAMPENED_CAMERA_ROTATION).setValue(dampenedCameraRotation);

    }



    /*
    Get the pose with the added dampened camera rotation
     */
    private void dampenArmRotation(AnimationPose pose, PoseCalculationDataContainer dataContainer, float partialTicks){
        Vector3f cameraRotation = dataContainer.getDriverValue(CAMERA_ROTATION, partialTicks);
        Vector3f dampenedCameraRotation = dataContainer.getDriverValue(DAMPENED_CAMERA_ROTATION, partialTicks);

        Vector3f cameraDampWeight = new Vector3f(0.6F, 0.3F, 0.1F);

        JointChannel jointChannel = pose.getJointTransform(ARM_BUFFER_JOINT);
        jointChannel.rotate(
                new Vector3f(
                        (dampenedCameraRotation.x() - cameraRotation.x()) * (cameraDampWeight.x() * 0.01F),
                        (dampenedCameraRotation.y() - cameraRotation.y()) * (cameraDampWeight.y() * 0.01F),
                        (dampenedCameraRotation.z() - cameraRotation.z()) * (cameraDampWeight.z() * 0.01F)
                ),
                JointChannel.TransformSpace.COMPONENT, JointChannel.TransformType.ADD
        );
        pose.setJointTransform(ARM_BUFFER_JOINT, jointChannel);
    }
}
