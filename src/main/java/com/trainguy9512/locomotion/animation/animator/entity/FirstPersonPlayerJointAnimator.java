package com.trainguy9512.locomotion.animation.animator.entity;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.data.*;
import com.trainguy9512.locomotion.animation.driver.SpringDriver;
import com.trainguy9512.locomotion.animation.driver.TriggerDriver;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
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
                .addJointUnderParent(ARM_BUFFER_JOINT, CAMERA_JOINT)
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

    @Override
    public PoseFunction<LocalSpacePose> constructPoseFunction(CachedPoseContainer cachedPoseContainer) {
        cachedPoseContainer.register(ADDITIVE_GROUND_MOVEMENT_CACHE, constructAdditiveGroundMovementPoseFunction(cachedPoseContainer));

        PoseFunction<LocalSpacePose> mainHandPose = this.constructHandPoseFunction(cachedPoseContainer, InteractionHand.MAIN_HAND);
        PoseFunction<LocalSpacePose> offHandPose = this.constructHandPoseFunction(cachedPoseContainer, InteractionHand.OFF_HAND);

        PoseFunction<LocalSpacePose> combinedHandPose = BlendPosesFunction.builder(mainHandPose)
                .addBlendInput(MirrorFunction.of(offHandPose), evaluationState -> 1f, LEFT_SIDE_JOINTS)
                .build();

        PoseFunction<LocalSpacePose> handPoseWithAdditive = ApplyAdditiveFunction.of(combinedHandPose, cachedPoseContainer.getOrThrow(ADDITIVE_GROUND_MOVEMENT_CACHE));

        PoseFunction<LocalSpacePose> mirroredBasedOnHandednessPose = MirrorFunction.of(handPoseWithAdditive, context -> Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT);

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

    public enum HandPose {
        EMPTY (HandPoseStates.EMPTY_RAISE, HandPoseStates.EMPTY_LOWER, HandPoseStates.EMPTY, HAND_EMPTY_POSE, HAND_TOOL_ATTACK_PICKAXE_MONTAGE),
        GENERIC_ITEM (HandPoseStates.GENERIC_ITEM_RAISE, HandPoseStates.GENERIC_ITEM_LOWER, HandPoseStates.GENERIC_ITEM, HAND_GENERIC_ITEM_POSE, HAND_TOOL_ATTACK_PICKAXE_MONTAGE),
        TOOL (HandPoseStates.TOOL_RAISE, HandPoseStates.TOOL_LOWER, HandPoseStates.TOOL, HAND_TOOL_POSE, HAND_TOOL_ATTACK_PICKAXE_MONTAGE),
        SHIELD (HandPoseStates.SHIELD_RAISE, HandPoseStates.SHIELD_LOWER, HandPoseStates.SHIELD, HAND_SHIELD_POSE, HAND_TOOL_ATTACK_PICKAXE_MONTAGE);

        public final HandPoseStates raisingState;
        public final HandPoseStates loweringState;
        public final HandPoseStates poseState;
        public final ResourceLocation basePoseLocation;
        public final MontageConfiguration attackMontage;

        HandPose(HandPoseStates raisingState, HandPoseStates loweringState, HandPoseStates poseState, ResourceLocation basePoseLocation, MontageConfiguration attackMontage) {
            this.raisingState = raisingState;
            this.loweringState = loweringState;
            this.poseState = poseState;
            this.basePoseLocation = basePoseLocation;
            this.attackMontage = attackMontage;
        }

        private static final List<TagKey<Item>> TOOL_ITEM_TAGS = List.of(
                ItemTags.PICKAXES,
                ItemTags.AXES,
                ItemTags.SHOVELS,
                ItemTags.HOES,
                ItemTags.SWORDS
        );

        private static HandPose fromItem(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return EMPTY;
            }
            if (itemStack.is(Items.SHIELD)) {
                return SHIELD;
            }
            for (TagKey<Item> tag : TOOL_ITEM_TAGS) {
                if (itemStack.is(tag)) {
                    return TOOL;
                }
            }
            return GENERIC_ITEM;
        }

        private PoseFunction<LocalSpacePose> getMiningStateMachine(CachedPoseContainer cachedPoseContainer, InteractionHand interactionHand) {
            return switch (interactionHand) {
                case MAIN_HAND -> switch (this) {
                    case TOOL -> makeMiningLoopStateMachine(
                            cachedPoseContainer,
                            SequenceEvaluatorFunction.of(HAND_TOOL_POSE),
                            SequencePlayerFunction.builder(HAND_TOOL_PICKAXE_MINE_SWING)
                                    .looping(true)
                                    .setResetStartTimeOffsetTicks(TimeSpan.of60FramesPerSecond(16))
                                    .setPlayRate(evaluationState -> 1.15f * LocomotionMain.CONFIG.data().firstPersonPlayer.miningAnimationSpeedMultiplier)
                                    .build(),
                            SequencePlayerFunction.builder(HAND_TOOL_PICKAXE_MINE_FINISH).build(),
                            Transition.of(TimeSpan.of60FramesPerSecond(6), Easing.SINE_OUT));
                    default -> ApplyAdditiveFunction.of(SequenceEvaluatorFunction.of(basePoseLocation), MakeDynamicAdditiveFunction.of(
                            makeMiningLoopStateMachine(
                                    cachedPoseContainer,
                                    SequenceEvaluatorFunction.of(HAND_EMPTY_POSE),
                                    SequencePlayerFunction.builder(HAND_EMPTY_MINE_SWING)
                                            .looping(true)
                                            .setResetStartTimeOffsetTicks(TimeSpan.of60FramesPerSecond(20))
                                            .setPlayRate(evaluationState -> 1.35f * LocomotionMain.CONFIG.data().firstPersonPlayer.miningAnimationSpeedMultiplier)
                                            .build(),
                                    SequencePlayerFunction.builder(HAND_EMPTY_MINE_FINISH).build(),
                                    Transition.of(TimeSpan.of60FramesPerSecond(6), Easing.SINE_OUT)),
                            SequenceEvaluatorFunction.of(HAND_EMPTY_POSE)));
                };
                case OFF_HAND -> SequenceEvaluatorFunction.of(this.basePoseLocation);
            };
        }
    }

    public enum HandPoseStates {
        DROPPING_LAST_ITEM,
        USING_LAST_ITEM,
        EMPTY,
        EMPTY_RAISE,
        EMPTY_LOWER,
        GENERIC_ITEM,
        GENERIC_ITEM_RAISE,
        GENERIC_ITEM_LOWER,
        TOOL,
        TOOL_RAISE,
        TOOL_LOWER,
        SHIELD,
        SHIELD_RAISE,
        SHIELD_LOWER,
    }

    public static final ResourceLocation HAND_EMPTY_LOWERED = makeAnimationSequenceResourceLocation("hand/empty/lowered");
    public static final ResourceLocation HAND_EMPTY_POSE = makeAnimationSequenceResourceLocation("hand/empty/pose");
    public static final ResourceLocation HAND_EMPTY_LOWER = makeAnimationSequenceResourceLocation("hand/empty/lower");
    public static final ResourceLocation HAND_EMPTY_RAISE = makeAnimationSequenceResourceLocation("hand/empty/raise");
    public static final ResourceLocation HAND_EMPTY_MINE_SWING = makeAnimationSequenceResourceLocation("hand/empty/mine_swing");
    public static final ResourceLocation HAND_EMPTY_MINE_FINISH = makeAnimationSequenceResourceLocation("hand/empty/mine_finish");

    public static final ResourceLocation HAND_TOOL_POSE = makeAnimationSequenceResourceLocation("hand/tool/pose");
    public static final ResourceLocation HAND_TOOL_LOWER = makeAnimationSequenceResourceLocation("hand/tool/lower");
    public static final ResourceLocation HAND_TOOL_RAISE = makeAnimationSequenceResourceLocation("hand/tool/raise");
    public static final ResourceLocation HAND_TOOL_PICKAXE_MINE_SWING = makeAnimationSequenceResourceLocation("hand/tool/pickaxe/mine_swing");
    public static final ResourceLocation HAND_TOOL_PICKAXE_MINE_FINISH = makeAnimationSequenceResourceLocation("hand/tool/pickaxe/mine_finish");
    public static final ResourceLocation HAND_TOOL_ATTACK = makeAnimationSequenceResourceLocation("hand/tool/attack");
    public static final ResourceLocation HAND_TOOL_USE = makeAnimationSequenceResourceLocation("hand/tool/use");

    public static final ResourceLocation HAND_GENERIC_ITEM_POSE = makeAnimationSequenceResourceLocation("hand/generic_item/pose");


    public PoseFunction<LocalSpacePose> constructHandPoseFunction(CachedPoseContainer cachedPoseContainer, InteractionHand interactionHand) {

        StateMachineFunction.Builder<HandPoseStates> handPoseStateMachineBuilder = StateMachineFunction.builder(
                evaluationState -> HandPose.fromItem(evaluationState.driverContainer().getDriverValue(interactionHand == InteractionHand.MAIN_HAND ? RENDERED_MAIN_HAND_ITEM : RENDERED_OFF_HAND_ITEM)).poseState
        );
        handPoseStateMachineBuilder.resetUponRelevant(true);
        StateAlias.Builder<HandPoseStates> stateAliasBuilder = StateAlias.builder(Set.of(HandPoseStates.EMPTY_LOWER));

        this.addStatesForHandPose(
                handPoseStateMachineBuilder,
                stateAliasBuilder,
                interactionHand,
                HandPose.GENERIC_ITEM,
                HandPose.GENERIC_ITEM.getMiningStateMachine(cachedPoseContainer, interactionHand),
                makeDynamicAdditiveLowerSequencePlayer(HAND_GENERIC_ITEM_POSE),
                makeDynamicAdditiveRaiseSequencePlayer(HAND_GENERIC_ITEM_POSE),
                Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT),
                Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT)
        );
        this.addStatesForHandPose(
                handPoseStateMachineBuilder,
                stateAliasBuilder,
                interactionHand,
                HandPose.TOOL,
                HandPose.TOOL.getMiningStateMachine(cachedPoseContainer, interactionHand),
                SequencePlayerFunction.builder(HAND_TOOL_LOWER).build(),
                SequencePlayerFunction.builder(HAND_TOOL_RAISE).build(),
                Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT),
                Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT)
        );
        this.addStatesForHandPose(
                handPoseStateMachineBuilder,
                stateAliasBuilder,
                interactionHand,
                HandPose.SHIELD,
                handShieldPoseFunction(cachedPoseContainer, interactionHand),
                makeDynamicAdditiveLowerSequencePlayer(HAND_SHIELD_POSE),
                makeDynamicAdditiveRaiseSequencePlayer(HAND_SHIELD_POSE),
                Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT),
                Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT)
        );
        this.addStatesForHandPose(
                handPoseStateMachineBuilder,
                stateAliasBuilder,
                interactionHand,
                HandPose.EMPTY,
                switch (interactionHand) {
                    case MAIN_HAND -> HandPose.EMPTY.getMiningStateMachine(cachedPoseContainer, interactionHand);
                    case OFF_HAND -> SequenceEvaluatorFunction.of(HAND_EMPTY_LOWERED);
                },
                switch (interactionHand) {
                    case MAIN_HAND -> SequencePlayerFunction.builder(HAND_EMPTY_LOWER).build();
                    case OFF_HAND -> SequencePlayerFunction.builder(HAND_EMPTY_LOWERED).build();
                },
                switch (interactionHand) {
                    case MAIN_HAND -> SequencePlayerFunction.builder(HAND_EMPTY_RAISE).build();
                    case OFF_HAND -> SequencePlayerFunction.builder(HAND_EMPTY_LOWERED).build();
                },
                switch (interactionHand) {
                    case MAIN_HAND -> Transition.of(TimeSpan.of60FramesPerSecond(7), Easing.SINE_IN_OUT);
                    case OFF_HAND -> Transition.INSTANT;
                },
                switch (interactionHand) {
                    case MAIN_HAND -> Transition.of(TimeSpan.of60FramesPerSecond(18), Easing.SINE_IN_OUT);
                    case OFF_HAND -> Transition.INSTANT;
                }
        );
        handPoseStateMachineBuilder.addStateAlias(stateAliasBuilder.build())
                .defineState(State.builder(HandPoseStates.DROPPING_LAST_ITEM,
                                ApplyAdditiveFunction.of(SequenceEvaluatorFunction.of(HAND_EMPTY_POSE), MakeDynamicAdditiveFunction.of(SequencePlayerFunction.builder(HAND_TOOL_USE).build(), SequenceEvaluatorFunction.of(HAND_TOOL_POSE)))
                        )
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .defineState(State.builder(HandPoseStates.USING_LAST_ITEM,
                                ApplyAdditiveFunction.of(SequenceEvaluatorFunction.of(HAND_EMPTY_POSE), MakeDynamicAdditiveFunction.of(SequencePlayerFunction.builder(HAND_TOOL_USE).build(), SequenceEvaluatorFunction.of(HAND_TOOL_POSE)))
                        )
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.EMPTY)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.SINE_IN_OUT))
                                .build())
                        .build());


        return handPoseStateMachineBuilder.build();
    }

    public PoseFunction<LocalSpacePose> makeDynamicAdditiveLowerSequencePlayer(ResourceLocation basePoseLocation) {
        return ApplyAdditiveFunction.of(SequenceEvaluatorFunction.of(basePoseLocation), MakeDynamicAdditiveFunction.of(SequencePlayerFunction.builder(HAND_EMPTY_LOWER).build(), SequenceEvaluatorFunction.of(HAND_EMPTY_POSE)));
    }

    public PoseFunction<LocalSpacePose> makeDynamicAdditiveRaiseSequencePlayer(ResourceLocation basePoseLocation) {
        return ApplyAdditiveFunction.of(SequenceEvaluatorFunction.of(basePoseLocation), MakeDynamicAdditiveFunction.of(SequencePlayerFunction.builder(HAND_EMPTY_RAISE).build(), SequenceEvaluatorFunction.of(HAND_EMPTY_POSE)));
    }

    public void addStatesForHandPose(
            StateMachineFunction.Builder<HandPoseStates> stateMachineBuilder,
            StateAlias.Builder<HandPoseStates> fromLoweringAliasBuilder,
            InteractionHand interactionHand,
            HandPose handPose,
            PoseFunction<LocalSpacePose> posePoseFunction,
            PoseFunction<LocalSpacePose> loweringPoseFunction,
            PoseFunction<LocalSpacePose> raisingPoseFunction,
            Transition poseToLoweringTiming,
            Transition raisingToPoseTiming
    ) {
        DriverKey<VariableDriver<ItemStack>> itemDriver = switch (interactionHand) {
            case MAIN_HAND -> MAIN_HAND_ITEM;
            case OFF_HAND -> OFF_HAND_ITEM;
        };
        DriverKey<VariableDriver<ItemStack>> renderedItemDriver = switch (interactionHand) {
            case MAIN_HAND -> RENDERED_MAIN_HAND_ITEM;
            case OFF_HAND -> RENDERED_OFF_HAND_ITEM;
        };
        Predicate<StateTransition.TransitionContext> switchHandsCondition = context -> {

            if (interactionHand == InteractionHand.MAIN_HAND) {
                // Don't switch hands if the player has just dropped an item.
//                if (context.driverContainer().getDriver(HAS_DROPPED_ITEM).hasBeenTriggered()) {
//                    return false;

//                }
                if (context.driverContainer().getDriver(HOTBAR_SLOT).hasValueChanged()) {
                    if (!context.driverContainer().getDriverValue(itemDriver).isEmpty() && !context.driverContainer().getDriverValue(renderedItemDriver).isEmpty()) {
                        // If this hand pose function is the main hand item, and the selected hot bar slot has changed, and the old and new items are not empty, play the item switch animation
                        return true;
                    }
                }
            }
            return context.driverContainer().getDriverValue(itemDriver).getItem() != context.driverContainer().getDriverValue(renderedItemDriver).getItem();
        };
        Predicate<StateTransition.TransitionContext> skipRaiseAnimationCondition = StateTransition.booleanDriverPredicate(IS_MINING).or(StateTransition.booleanDriverPredicate(HAS_ATTACKED)).or(StateTransition.booleanDriverPredicate(HAS_USED_MAIN_HAND_ITEM));

        Consumer<PoseFunction.FunctionEvaluationState> updateRenderedItem = evaluationState -> evaluationState.driverContainer().getDriver(renderedItemDriver).setValue(evaluationState.driverContainer().getDriverValue(itemDriver).copy());
        Consumer<PoseFunction.FunctionEvaluationState> clearAttackMontages = evaluationState -> evaluationState.montageManager().interruptMontagesInSlot(MAIN_HAND_ATTACK_SLOT);

        State.Builder<HandPoseStates> raisingStateBuilder = State.builder(handPose.raisingState, raisingPoseFunction)
                .resetsPoseFunctionUponEntry(true)
                .addOutboundTransition(StateTransition.builder(handPose.poseState)
                        .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                        .setTiming(raisingToPoseTiming)
                        .build());
        if (interactionHand == InteractionHand.MAIN_HAND) {
            raisingStateBuilder.addOutboundTransition(StateTransition.builder(handPose.poseState)
                    .isTakenIfTrue(skipRaiseAnimationCondition)
                    .setTiming(Transition.SINGLE_TICK)
                    .build()
            );
        }
        stateMachineBuilder
                .defineState(State.builder(handPose.poseState, MontageSlotFunction.of(posePoseFunction, interactionHand == InteractionHand.MAIN_HAND ? MAIN_HAND_ATTACK_SLOT : OFF_HAND_ATTACK_SLOT))
                        .resetsPoseFunctionUponEntry(true)
                        .build())
                .defineState(State.builder(handPose.loweringState, loweringPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        .build())
                .defineState(raisingStateBuilder.build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                handPose.poseState,
                                handPose.raisingState
                        ))
                        .addOutboundTransition(StateTransition.builder(handPose.loweringState)
                                .isTakenIfTrue(switchHandsCondition)
                                .setTiming(poseToLoweringTiming)
                                .setPriority(50)
                                .build())
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.DROPPING_LAST_ITEM)
                                .isTakenIfTrue(transitionContext -> {
                                    if (interactionHand == InteractionHand.MAIN_HAND) {
                                        if (transitionContext.driverContainer().getDriver(itemDriver).getCurrentValue().isEmpty()) {
                                            return transitionContext.driverContainer().getDriver(HAS_DROPPED_ITEM).hasBeenTriggered();
                                        }
                                    }
                                    return false;
                                })
                                .setPriority(60)
                                .setTiming(Transition.of(TimeSpan.ofTicks(2)))
                                .bindToOnTransitionTaken(updateRenderedItem)
                                .bindToOnTransitionTaken(clearAttackMontages)
                                .build())
                        .addOutboundTransition(StateTransition.builder(HandPoseStates.USING_LAST_ITEM)
                                .isTakenIfTrue(transitionContext -> {
                                    if (transitionContext.driverContainer().getDriver(itemDriver).getCurrentValue().isEmpty()) {
                                        if (interactionHand == InteractionHand.MAIN_HAND) {
                                            if (transitionContext.driverContainer().getDriver(HAS_ATTACKED).hasBeenTriggered()) {
                                                return true;
                                            }
                                        }
                                        if (transitionContext.driverContainer().getDriver(interactionHand == InteractionHand.MAIN_HAND ? HAS_USED_MAIN_HAND_ITEM : HAS_USED_OFF_HAND_ITEM).hasBeenTriggered()) {
                                            return true;
                                        }
                                    }
                                    return false;
                                })
                                .setPriority(60)
                                .setTiming(Transition.of(TimeSpan.ofTicks(2)))
                                .bindToOnTransitionTaken(updateRenderedItem)
                                .bindToOnTransitionTaken(clearAttackMontages)
                                .build())
                        .build());
        fromLoweringAliasBuilder
                .addOriginatingState(handPose.loweringState)
                .addOutboundTransition(StateTransition.builder(handPose.raisingState)
                        .setTiming(Transition.INSTANT)
                        .isTakenIfTrue(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING
                                .and(context -> HandPose.fromItem(context.driverContainer().getDriverValue(itemDriver)) == handPose)
                        )
                        .bindToOnTransitionTaken(updateRenderedItem)
                        .bindToOnTransitionTaken(clearAttackMontages)
                        .build());
    }

    public static final ResourceLocation HAND_SHIELD_POSE = makeAnimationSequenceResourceLocation("hand/shield/pose");
    public static final ResourceLocation HAND_SHIELD_BLOCK_IN = makeAnimationSequenceResourceLocation("hand/shield/block_in");
    public static final ResourceLocation HAND_SHIELD_BLOCK_OUT = makeAnimationSequenceResourceLocation("hand/shield/block_out");
    public static final ResourceLocation HAND_SHIELD_DISABLE_IN = makeAnimationSequenceResourceLocation("hand/shield/disable_in");
    public static final ResourceLocation HAND_SHIELD_DISABLE_OUT = makeAnimationSequenceResourceLocation("hand/shield/disable_out");
    public static final ResourceLocation HAND_SHIELD_IMPACT = makeAnimationSequenceResourceLocation("hand/shield/impact");

    public enum ShieldStates {
        LOWERED,
        BLOCKING_IN,
        BLOCKING,
        BLOCKING_OUT,
        DISABLED_IN,
        DISABLED,
        DISABLED_OUT
    }

    public static PoseFunction<LocalSpacePose> handShieldPoseFunction(CachedPoseContainer cachedPoseContainer, InteractionHand interactionHand) {
        DriverKey<VariableDriver<Boolean>> usingItemDriverKey = switch (interactionHand) {
            case MAIN_HAND -> IS_USING_MAIN_HAND_ITEM;
            case OFF_HAND -> IS_USING_OFF_HAND_ITEM;
        };
        DriverKey<VariableDriver<Boolean>> isHandOnCooldownKey = switch (interactionHand) {
            case MAIN_HAND -> IS_MAIN_HAND_ON_COOLDOWN;
            case OFF_HAND -> IS_OFF_HAND_ON_COOLDOWN;
        };
        PoseFunction<LocalSpacePose> shieldBlockingStateMachine = StateMachineFunction.builder(evaluationState -> ShieldStates.LOWERED)
                .resetUponRelevant(true)
                .defineState(State.builder(ShieldStates.LOWERED, HandPose.SHIELD.getMiningStateMachine(cachedPoseContainer, interactionHand))
                        .build())
                .defineState(State.builder(ShieldStates.BLOCKING_IN, SequencePlayerFunction.builder(HAND_SHIELD_BLOCK_IN).build())
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(ShieldStates.BLOCKING)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(5)))
                                .build())
                        .build())
                .defineState(State.builder(ShieldStates.BLOCKING, MontageSlotFunction.of(SequenceEvaluatorFunction.of(HAND_SHIELD_BLOCK_OUT), SHIELD_BLOCK_SLOT))
                        .resetsPoseFunctionUponEntry(true)
                        .build())
                .defineState(State.builder(ShieldStates.BLOCKING_OUT, SequencePlayerFunction.builder(HAND_SHIELD_BLOCK_OUT).build())
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(ShieldStates.LOWERED)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(15)))
                                .build())
                        .build())
                .defineState(State.builder(ShieldStates.DISABLED_IN, SequencePlayerFunction.builder(HAND_SHIELD_DISABLE_IN).build())
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(ShieldStates.DISABLED)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(0)
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .defineState(State.builder(ShieldStates.DISABLED, SequenceEvaluatorFunction.of(HAND_SHIELD_DISABLE_OUT))
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(ShieldStates.DISABLED_OUT)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(isHandOnCooldownKey).negate())
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .defineState(State.builder(ShieldStates.DISABLED_OUT, SequencePlayerFunction.builder(HAND_SHIELD_DISABLE_OUT).build())
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(ShieldStates.LOWERED)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(20)))
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                ShieldStates.BLOCKING_IN,
                                ShieldStates.BLOCKING,
                                ShieldStates.BLOCKING_OUT,
                                ShieldStates.LOWERED,
                                ShieldStates.DISABLED_OUT
                        ))
                        .addOutboundTransition(StateTransition.builder(ShieldStates.DISABLED_IN)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(isHandOnCooldownKey).and(transitionContext -> transitionContext.driverContainer().getDriver(usingItemDriverKey).getPreviousValue()))
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                ShieldStates.BLOCKING_IN,
                                ShieldStates.BLOCKING
                        ))
                        .addOutboundTransition(StateTransition.builder(ShieldStates.BLOCKING_OUT)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(usingItemDriverKey).negate()
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED)
                                )
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(6)))
                                .setPriority(50)
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                ShieldStates.BLOCKING_IN,
                                ShieldStates.BLOCKING,
                                ShieldStates.BLOCKING_OUT
                        ))
                        .addOutboundTransition(StateTransition.builder(ShieldStates.LOWERED)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(usingItemDriverKey).negate()
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED)
                                        .and(StateTransition.booleanDriverPredicate(IS_MINING))
                                )
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(6)))
                                .setPriority(60)
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                ShieldStates.BLOCKING_OUT,
                                ShieldStates.DISABLED_OUT
                        ))
                        .addOutboundTransition(StateTransition.builder(ShieldStates.BLOCKING_IN)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(usingItemDriverKey)
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED))
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(13), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .addStateAlias(StateAlias.builder(
                        Set.of(
                                ShieldStates.LOWERED
                        ))
                        .addOutboundTransition(StateTransition.builder(ShieldStates.BLOCKING_IN)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(usingItemDriverKey))
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(13), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .build();
        return shieldBlockingStateMachine;
    }

    public enum MiningStates {
        IDLE,
        SWING,
        FINISH
    }

    public static PoseFunction<LocalSpacePose> makeMiningLoopStateMachine(
            CachedPoseContainer cachedPoseContainer,
            PoseFunction<LocalSpacePose> idlePoseFunction,
            PoseFunction<LocalSpacePose> swingPoseFunction,
            PoseFunction<LocalSpacePose> finishPoseFunction,
            Transition idleToMiningTiming
    ) {
        return StateMachineFunction.builder(evaluationState -> MiningStates.IDLE)
                .resetUponRelevant(true)
                .defineState(State.builder(MiningStates.IDLE, idlePoseFunction)
                        .addOutboundTransition(StateTransition.builder(MiningStates.SWING)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_MINING))
                                .setTiming(idleToMiningTiming)
                                .build())
                        .build())
                .defineState(State.builder(MiningStates.SWING, swingPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(MiningStates.FINISH)
                                .isTakenIfTrue(
                                        StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_IS_FINISHING.and(StateTransition.booleanDriverPredicate(IS_MINING).negate())
                                )
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(50)
                                .build())
                        .build())
                .defineState(State.builder(MiningStates.FINISH, finishPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
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

    public static final ResourceLocation GROUND_MOVEMENT_POSE = makeAnimationSequenceResourceLocation("ground_movement/pose");
    public static final ResourceLocation GROUND_MOVEMENT_IDLE = makeAnimationSequenceResourceLocation("ground_movement/idle");
    public static final ResourceLocation GROUND_MOVEMENT_WALKING = makeAnimationSequenceResourceLocation("ground_movement/walking");
    public static final ResourceLocation GROUND_MOVEMENT_WALK_TO_STOP = makeAnimationSequenceResourceLocation("ground_movement/walk_to_stop");
    public static final ResourceLocation GROUND_MOVEMENT_JUMP = makeAnimationSequenceResourceLocation("ground_movement/jump");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING = makeAnimationSequenceResourceLocation("ground_movement/falling");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING_DOWN = makeAnimationSequenceResourceLocation("ground_movement/falling_down");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING_IN_PLACE = makeAnimationSequenceResourceLocation("ground_movement/falling_in_place");
    public static final ResourceLocation GROUND_MOVEMENT_FALLING_UP = makeAnimationSequenceResourceLocation("ground_movement/falling_up");
    public static final ResourceLocation GROUND_MOVEMENT_LAND = makeAnimationSequenceResourceLocation("ground_movement/land");

    public PoseFunction<LocalSpacePose> constructAdditiveGroundMovementPoseFunction(CachedPoseContainer cachedPoseContainer) {

        PoseFunction<LocalSpacePose> idleAnimationPlayer = BlendPosesFunction.builder(SequenceEvaluatorFunction.of(GROUND_MOVEMENT_IDLE))
                .addBlendInput(SequencePlayerFunction.builder(GROUND_MOVEMENT_IDLE).looping(true).build(), evaluationState -> 0.6f)
                .build();
        PoseFunction<LocalSpacePose> walkToStopPoseFunction = SequencePlayerFunction.builder(GROUND_MOVEMENT_WALK_TO_STOP).setPlayRate(0.6f).build();
        PoseFunction<LocalSpacePose> jumpPoseFunction = SequencePlayerFunction.builder(GROUND_MOVEMENT_JUMP).build();
        PoseFunction<LocalSpacePose> fallingPoseFunction = BlendedSequencePlayerFunction.builder(VERTICAL_MOVEMENT_SPEED)
                .addEntry(0.5f, GROUND_MOVEMENT_FALLING_UP)
                .addEntry(-0f, GROUND_MOVEMENT_FALLING_IN_PLACE)
                .addEntry(-1f, GROUND_MOVEMENT_FALLING_DOWN)
                .build();
        PoseFunction<LocalSpacePose> walkingPoseFunction = BlendedSequencePlayerFunction.builder(MODIFIED_WALK_SPEED)
                .addEntry(0f, GROUND_MOVEMENT_WALKING, 0.5f)
                .addEntry(0.5f, GROUND_MOVEMENT_WALKING, 2f)
                .addEntry(0.86f, GROUND_MOVEMENT_WALKING, 2.25f)
                .addEntry(1f, GROUND_MOVEMENT_WALKING, 3.5f)
                .build();

        PoseFunction<LocalSpacePose> landPoseFunction = SequencePlayerFunction.builder(GROUND_MOVEMENT_LAND).build();
        PoseFunction<LocalSpacePose> softLandPoseFunction = BlendPosesFunction.builder(SequenceEvaluatorFunction.of(GROUND_MOVEMENT_POSE, TimeSpan.ofSeconds(0)))
                .addBlendInput(SequencePlayerFunction.builder(GROUND_MOVEMENT_LAND).setPlayRate(1f).build(), evaluationState -> 0.5f)
                .build();

        Predicate<StateTransition.TransitionContext> walkingCondition = transitionContext -> transitionContext.driverContainer().getDriverValue(IS_MOVING);


        PoseFunction<LocalSpacePose> movementStateMachine = StateMachineFunction.builder(evaluationState -> GroundMovementStates.IDLE)
                .defineState(State.builder(GroundMovementStates.IDLE, idleAnimationPlayer)
                        .resetsPoseFunctionUponEntry(false)
                        // Begin walking if the player is moving horizontally
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(walkingCondition)
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.SINE_OUT))
                                .build())
                        .build())
                .defineState(State.builder(GroundMovementStates.WALKING, walkingPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
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
                .defineState(State.builder(GroundMovementStates.STOPPING, walkToStopPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.IDLE)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(0f)
                                .setTiming(Transition.of(TimeSpan.ofSeconds(1f), Easing.SINE_IN_OUT))
                                .build())
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(walkingCondition.and(StateTransition.CURRENT_TRANSITION_FINISHED))
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.3f), Easing.SINE_IN_OUT))
                                .build())
                        .build())
                .defineState(State.builder(GroundMovementStates.JUMP, jumpPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        // Automatically move into the falling animation player
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.FALLING)
                                .isTakenIfMostRelevantAnimationPlayerFinishing(1f)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(19), Easing.CUBIC_OUT))
                                .build())
                        // If the player lands before it can move into the falling animation, go straight to the landing animation as long as the jump state is fully transitioned.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.LAND)
                                .isTakenIfTrue(StateTransition.CURRENT_TRANSITION_FINISHED
                                        .and(StateTransition.booleanDriverPredicate(IS_GROUNDED))
                                )
                                .setTiming(Transition.SINGLE_TICK)
                                .build())
                        .build())
                .defineState(State.builder(GroundMovementStates.FALLING, fallingPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        // Move into the landing animation if the player is no longer falling
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.LAND)
                                .isTakenIfTrue(
                                        StateTransition.booleanDriverPredicate(IS_GROUNDED)
                                )
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(50)
                                .build())
                        // Move into the landing animation if the player is no longer falling, but only just began falling.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.SOFT_LAND)
                                .isTakenIfTrue(
                                        StateTransition.booleanDriverPredicate(IS_GROUNDED)
                                        .and(StateTransition.CURRENT_TRANSITION_FINISHED.negate())
                                )
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(60)
                                .build())
                        // Transition to the jumping animation if the player is jumping.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.JUMP)
                                .isTakenIfTrue(StateTransition.booleanDriverPredicate(IS_JUMPING).and(StateTransition.booleanDriverPredicate(IS_GROUNDED)))
                                .setTiming(Transition.SINGLE_TICK)
                                .setPriority(70)
                                .build())
                        .build())
                .defineState(State.builder(GroundMovementStates.SOFT_LAND, softLandPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
                        .build())
                .defineState(State.builder(GroundMovementStates.LAND, landPoseFunction)
                        .resetsPoseFunctionUponEntry(true)
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
                                .setTiming(Transition.of(TimeSpan.ofSeconds(0.2f), Easing.SINE_OUT))
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
                                .isTakenIfTrue(walkingCondition.negate().and(StateTransition.MOST_RELEVANT_ANIMATION_PLAYER_HAS_FINISHED))
                                .setTiming(Transition.of(TimeSpan.ofSeconds(1), Easing.SINE_IN_OUT))
                                .setPriority(50)
                                .build())
                        // If the falling animation is finishing and the player is walking, play the walking animation.
                        .addOutboundTransition(StateTransition.builder(GroundMovementStates.WALKING)
                                .isTakenIfTrue(walkingCondition)
                                .setTiming(Transition.of(TimeSpan.of60FramesPerSecond(40), Easing.CUBIC_IN_OUT))
                                .setPriority(60)
                                .build())
                        .build())
                .build();

//        return movementStateMachine;

        return MakeDynamicAdditiveFunction.of(
                movementStateMachine,
                SequenceEvaluatorFunction.of(GROUND_MOVEMENT_POSE)
        );
    }

    public static final DriverKey<SpringDriver<Vector3f>> DAMPED_VELOCITY = DriverKey.of("damped_velocity", () -> SpringDriver.ofVector3f(0.8f, 0.6f, 1f, Vector3f::new, false));
    public static final DriverKey<VariableDriver<Vector3f>> MOVEMENT_DIRECTION_OFFSET = DriverKey.of("movement_direction_offset", () -> VariableDriver.ofVector(Vector3f::new));
    public static final DriverKey<SpringDriver<Vector3f>> CAMERA_ROTATION_DAMPING = DriverKey.of("camera_rotation_damping", () -> SpringDriver.ofVector3f(LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationStiffnessFactor, LocomotionMain.CONFIG.data().firstPersonPlayer.cameraRotationDampingFactor, 1f, Vector3f::new, true));

    public static final DriverKey<VariableDriver<Integer>> HOTBAR_SLOT = DriverKey.of("hotbar_slot", () -> VariableDriver.ofConstant(() -> 0));
    public static final DriverKey<VariableDriver<ItemStack>> MAIN_HAND_ITEM = DriverKey.of("main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> OFF_HAND_ITEM = DriverKey.of("off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_MAIN_HAND_ITEM = DriverKey.of("rendered_main_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));
    public static final DriverKey<VariableDriver<ItemStack>> RENDERED_OFF_HAND_ITEM = DriverKey.of("rendered_off_hand_item", () -> VariableDriver.ofConstant(() -> ItemStack.EMPTY));

    public static final DriverKey<VariableDriver<Float>> HORIZONTAL_MOVEMENT_SPEED = DriverKey.of("horizontal_movement_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Float>> VERTICAL_MOVEMENT_SPEED = DriverKey.of("vertical_movement_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Float>> MODIFIED_WALK_SPEED = DriverKey.of("modified_walk_speed", () -> VariableDriver.ofFloat(() -> 0f));
    public static final DriverKey<VariableDriver<Boolean>> IS_MOVING = DriverKey.of("is_moving", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_GROUNDED = DriverKey.of("is_grounded", () -> VariableDriver.ofBoolean(() -> true));
    public static final DriverKey<VariableDriver<Boolean>> IS_JUMPING = DriverKey.of("is_jumping", () -> VariableDriver.ofBoolean(() -> false));

    public static final DriverKey<VariableDriver<Boolean>> IS_MINING = DriverKey.of("is_mining", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<TriggerDriver> HAS_ATTACKED = DriverKey.of("has_attacked", TriggerDriver::of);
    public static final DriverKey<TriggerDriver> HAS_USED_MAIN_HAND_ITEM = DriverKey.of("has_used_main_hand_item", TriggerDriver::of);
    public static final DriverKey<TriggerDriver> HAS_USED_OFF_HAND_ITEM = DriverKey.of("has_used_off_hand_item", TriggerDriver::of);
    public static final DriverKey<TriggerDriver> HAS_BLOCKED_ATTACK = DriverKey.of("has_blocked_attack", TriggerDriver::of);
    public static final DriverKey<TriggerDriver> HAS_DROPPED_ITEM = DriverKey.of("has_dropped_item", TriggerDriver::of);
    public static final DriverKey<VariableDriver<Boolean>> IS_USING_MAIN_HAND_ITEM = DriverKey.of("is_using_main_hand_item", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_USING_OFF_HAND_ITEM = DriverKey.of("is_using_off_hand_item", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_MAIN_HAND_ON_COOLDOWN = DriverKey.of("is_main_hand_on_cooldown", () -> VariableDriver.ofBoolean(() -> false));
    public static final DriverKey<VariableDriver<Boolean>> IS_OFF_HAND_ON_COOLDOWN = DriverKey.of("is_off_hand_on_cooldown", () -> VariableDriver.ofBoolean(() -> false));

    public static final String MAIN_HAND_ATTACK_SLOT = "main_hand_attack";
    public static final String OFF_HAND_ATTACK_SLOT = "off_hand_attack";
    public static final String SHIELD_BLOCK_SLOT = "shield_block";

    public static final MontageConfiguration HAND_TOOL_ATTACK_PICKAXE_MONTAGE = MontageConfiguration.builder("hand_tool_attack_pickaxe", HAND_TOOL_ATTACK)
            .playsInSlot(MAIN_HAND_ATTACK_SLOT)
            .setCooldownDuration(TimeSpan.of60FramesPerSecond(8))
            .setTransitionIn(Transition.of(TimeSpan.of60FramesPerSecond(3), Easing.SINE_OUT))
            .setTransitionOut(Transition.of(TimeSpan.of60FramesPerSecond(12), Easing.SINE_IN_OUT))
            .makeAdditive(driverContainer -> HandPose.fromItem(driverContainer.getDriverValue(RENDERED_MAIN_HAND_ITEM)).basePoseLocation)
            .build();
    public static final MontageConfiguration USE_MAIN_HAND_MONTAGE = MontageConfiguration.builder("hand_use_main_hand", HAND_TOOL_USE)
            .playsInSlot(MAIN_HAND_ATTACK_SLOT)
            .setCooldownDuration(TimeSpan.of60FramesPerSecond(5))
            .setTransitionIn(Transition.of(TimeSpan.of60FramesPerSecond(3), Easing.SINE_OUT))
            .setTransitionOut(Transition.of(TimeSpan.of60FramesPerSecond(16), Easing.SINE_IN_OUT))
            .makeAdditive(driverContainer -> HandPose.fromItem(driverContainer.getDriverValue(RENDERED_MAIN_HAND_ITEM)).basePoseLocation)
            .build();
    public static final MontageConfiguration USE_OFF_HAND_MONTAGE = MontageConfiguration.builder("hand_use_off_hand", HAND_TOOL_USE)
            .playsInSlot(OFF_HAND_ATTACK_SLOT)
            .setCooldownDuration(TimeSpan.of60FramesPerSecond(5))
            .setTransitionIn(Transition.of(TimeSpan.of60FramesPerSecond(3), Easing.SINE_OUT))
            .setTransitionOut(Transition.of(TimeSpan.of60FramesPerSecond(16), Easing.SINE_IN_OUT))
            .makeAdditive(driverContainer -> HandPose.fromItem(driverContainer.getDriverValue(RENDERED_OFF_HAND_ITEM)).basePoseLocation)
            .build();
    public static final MontageConfiguration SHIELD_BLOCK_IMPACT_MONTAGE = MontageConfiguration.builder("shield_block_impact", HAND_SHIELD_IMPACT)
            .playsInSlot(SHIELD_BLOCK_SLOT)
            .setCooldownDuration(TimeSpan.of60FramesPerSecond(5))
            .setTransitionIn(Transition.of(TimeSpan.of60FramesPerSecond(2), Easing.SINE_IN_OUT))
            .setTransitionOut(Transition.of(TimeSpan.of60FramesPerSecond(8), Easing.SINE_IN_OUT))
            .build();

    @Override
    public void extractAnimationData(LocalPlayer dataReference, OnTickDriverContainer driverContainer, MontageManager montageManager){

        driverContainer.getDriver(MODIFIED_WALK_SPEED).setValue(dataReference.walkAnimation.speed());
        driverContainer.getDriver(HORIZONTAL_MOVEMENT_SPEED).setValue(new Vector3f((float) (dataReference.getX() - dataReference.xo), 0.0f, (float) (dataReference.getZ() - dataReference.zo)).length());
        driverContainer.getDriver(VERTICAL_MOVEMENT_SPEED).setValue((float) (dataReference.getY() - dataReference.yo));

//        LocomotionMain.LOGGER.info(dataReference.getMainHandItem().);
        driverContainer.getDriver(MAIN_HAND_ITEM).setValue(dataReference.getMainHandItem());
        driverContainer.getDriver(OFF_HAND_ITEM).setValue(dataReference.getOffhandItem());

        //? if >= 1.21.5 {
        driverContainer.getDriver(HOTBAR_SLOT).setValue(dataReference.getInventory().getSelectedSlot());
        //?} else
        /*driverContainer.getDriver(HOTBAR_SLOT).setValue(dataReference.getInventory().selected);*/


        driverContainer.getDriver(HAS_USED_MAIN_HAND_ITEM).runIfTriggered(() -> montageManager.playMontage(USE_MAIN_HAND_MONTAGE, driverContainer));
        driverContainer.getDriver(HAS_USED_OFF_HAND_ITEM).runIfTriggered(() -> montageManager.playMontage(USE_OFF_HAND_MONTAGE, driverContainer));
        driverContainer.getDriver(HAS_DROPPED_ITEM).runIfTriggered(() -> montageManager.playMontage(USE_MAIN_HAND_MONTAGE, driverContainer));

        driverContainer.getDriver(HAS_ATTACKED).runIfTriggered(() -> montageManager.playMontage(HandPose.fromItem(driverContainer.getDriverValue(MAIN_HAND_ITEM)).attackMontage, driverContainer));
        driverContainer.getDriver(HAS_BLOCKED_ATTACK).runIfTriggered(() -> montageManager.playMontage(SHIELD_BLOCK_IMPACT_MONTAGE, driverContainer));
        driverContainer.getDriver(IS_USING_MAIN_HAND_ITEM).setValue(dataReference.isUsingItem() && dataReference.getUsedItemHand() == InteractionHand.MAIN_HAND);
        driverContainer.getDriver(IS_USING_OFF_HAND_ITEM).setValue(dataReference.isUsingItem() && dataReference.getUsedItemHand() == InteractionHand.OFF_HAND);

        driverContainer.getDriver(IS_MAIN_HAND_ON_COOLDOWN).setValue(dataReference.getCooldowns().isOnCooldown(driverContainer.getDriverValue(RENDERED_MAIN_HAND_ITEM)));
        driverContainer.getDriver(IS_OFF_HAND_ON_COOLDOWN).setValue(dataReference.getCooldowns().isOnCooldown(driverContainer.getDriverValue(RENDERED_OFF_HAND_ITEM)));

        driverContainer.getDriver(IS_MOVING).setValue(dataReference.input.keyPresses.forward() || dataReference.input.keyPresses.backward() || dataReference.input.keyPresses.left() || dataReference.input.keyPresses.right());
        driverContainer.getDriver(IS_GROUNDED).setValue(dataReference.onGround());
        driverContainer.getDriver(IS_JUMPING).setValue(dataReference.input.keyPresses.jump());


        Vector3f velocity = new Vector3f((float) (dataReference.getX() - dataReference.xo), (float) (dataReference.getY() - dataReference.yo), (float) (dataReference.getZ() - dataReference.zo));
        // We don't want vertical velocity to be factored into the movement direction offset as much as the horizontal velocity.
        velocity.mul(1, 0f, 1).mul(dataReference.isSprinting() ? 4f : 3f).min(new Vector3f(1)).max(new Vector3f(-1));
        driverContainer.getDriver(DAMPED_VELOCITY).setValue(velocity);

        Vector3f dampedVelocity = new Vector3f(driverContainer.getDriverValue(DAMPED_VELOCITY));
        Quaternionf rotation = new Quaternionf().rotationYXZ(Mth.PI - dataReference.getYRot() * Mth.DEG_TO_RAD, -dataReference.getXRot() * Mth.DEG_TO_RAD, 0.0F);
        Vector3f movementDirection = new Vector3f(
                dampedVelocity.dot(new Vector3f(1, 0, 0).rotate(rotation)),
                dampedVelocity.dot(new Vector3f(0, 1, 0).rotate(rotation)),
                dampedVelocity.dot(new Vector3f(0, 0, -1).rotate(rotation))
        );
        driverContainer.getDriver(MOVEMENT_DIRECTION_OFFSET).setValue(movementDirection);
        driverContainer.getDriver(CAMERA_ROTATION_DAMPING).setValue(new Vector3f(dataReference.getXRot(), dataReference.getYRot(), dataReference.getYRot()).mul(Mth.DEG_TO_RAD));

    }

    private static ResourceLocation makeAnimationSequenceResourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "sequences/entity/player/first_person/".concat(path).concat(".json"));
    }
}
