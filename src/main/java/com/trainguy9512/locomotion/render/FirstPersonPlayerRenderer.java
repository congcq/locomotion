package com.trainguy9512.locomotion.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trainguy9512.locomotion.access.MatrixModelPart;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.entity.FirstPersonPlayerJointAnimator;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.access.AlternateSingleBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class FirstPersonPlayerRenderer implements RenderLayerParent<PlayerRenderState, PlayerModel> {

    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderer;
    private final ItemModelResolver itemModelResolver;
    private final JointAnimatorDispatcher jointAnimatorDispatcher;

    public static boolean SHOULD_FLIP_ITEM_TRANSFORM = false;

    public FirstPersonPlayerRenderer(EntityRendererProvider.Context context) {
        this.minecraft = Minecraft.getInstance();
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.itemRenderer = minecraft.getItemRenderer();
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.itemModelResolver = context.getItemModelResolver();
        this.jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();
    }

    public void render(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer, LocalPlayer playerEntity, int combinedLight) {

        JointAnimatorDispatcher jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();

        JointAnimatorDispatcher.getInstance().getFirstPersonPlayerDataContainer().ifPresent(
                dataContainer -> jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().ifPresent(
                        animationPose -> {

                            JointChannel rightArmPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.RIGHT_ARM_JOINT);
                            JointChannel leftArmPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.LEFT_ARM_JOINT);
                            JointChannel rightItemPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.RIGHT_ITEM_JOINT);
                            JointChannel leftItemPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.LEFT_ITEM_JOINT);

                            poseStack.pushPose();
                            poseStack.mulPose(Axis.ZP.rotationDegrees(180));


                            AbstractClientPlayer abstractClientPlayer = this.minecraft.player;
                            PlayerRenderer playerRenderer = (PlayerRenderer)this.entityRenderDispatcher.getRenderer(abstractClientPlayer);
                            PlayerModel playerModel = playerRenderer.getModel();
                            playerModel.resetPose();

                            ((MatrixModelPart)(Object) playerModel.rightArm).locomotion$setMatrix(rightArmPose.getTransform());
                            ((MatrixModelPart)(Object) playerModel.leftArm).locomotion$setMatrix(leftArmPose.getTransform());

                            playerModel.body.visible = false;

                            this.renderArm(abstractClientPlayer, playerModel, HumanoidArm.LEFT, poseStack, buffer, combinedLight);
                            this.renderArm(abstractClientPlayer, playerModel, HumanoidArm.RIGHT, poseStack, buffer, combinedLight);

                            //this.entityRenderDispatcher.render(abstractClientPlayer, 0, 0, 0, partialTicks, poseStack, buffer, combinedLight);

                            boolean leftHanded = this.minecraft.options.mainHand().get() == HumanoidArm.LEFT;

                            ItemStack leftHandRenderedItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.RENDERED_MAIN_HAND_ITEM : FirstPersonPlayerJointAnimator.RENDERED_OFF_HAND_ITEM);
                            ItemStack rightHandRenderedItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.RENDERED_OFF_HAND_ITEM : FirstPersonPlayerJointAnimator.RENDERED_MAIN_HAND_ITEM);
                            ItemStack leftHandItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.MAIN_HAND_ITEM : FirstPersonPlayerJointAnimator.OFF_HAND_ITEM);
                            ItemStack rightHandItem = dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.OFF_HAND_ITEM : FirstPersonPlayerJointAnimator.MAIN_HAND_ITEM);

                            FirstPersonPlayerJointAnimator.GenericItemPose leftHandGenericItemPose = dataContainer.getDriverValue(FirstPersonPlayerJointAnimator.getGenericItemPoseDriver(leftHanded ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
                            FirstPersonPlayerJointAnimator.GenericItemPose rightHandGenericItemPose = dataContainer.getDriverValue(FirstPersonPlayerJointAnimator.getGenericItemPoseDriver(!leftHanded ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
                            FirstPersonPlayerJointAnimator.HandPose leftHandPose = dataContainer.getDriverValue(FirstPersonPlayerJointAnimator.getHandPoseDriver(leftHanded ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
                            FirstPersonPlayerJointAnimator.HandPose rightHandPose = dataContainer.getDriverValue(FirstPersonPlayerJointAnimator.getHandPoseDriver(!leftHanded ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));

                            leftHandItem = ItemStack.isSameItemSameComponents(leftHandItem, leftHandRenderedItem) ? leftHandItem : leftHandRenderedItem;
                            rightHandItem = ItemStack.isSameItemSameComponents(rightHandItem, rightHandRenderedItem) ? rightHandItem : rightHandRenderedItem;

                            this.renderItem(
                                    abstractClientPlayer,
                                    rightHandItem,
                                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                    poseStack,
                                    rightItemPose,
                                    buffer,
                                    combinedLight,
                                    HumanoidArm.RIGHT,
                                    rightHandPose,
                                    rightHandGenericItemPose,
                                    dataContainer.getDriverValue(!leftHanded ? FirstPersonPlayerJointAnimator.RENDER_MAIN_HAND_ITEM_AS_STATIC : FirstPersonPlayerJointAnimator.RENDER_OFF_HAND_ITEM_AS_STATIC)
                            );
                            this.renderItem(
                                    abstractClientPlayer,
                                    leftHandItem,
                                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                                    poseStack,
                                    leftItemPose,
                                    buffer,
                                    combinedLight,
                                    HumanoidArm.LEFT,
                                    leftHandPose,
                                    leftHandGenericItemPose,
                                    dataContainer.getDriverValue(leftHanded ? FirstPersonPlayerJointAnimator.RENDER_MAIN_HAND_ITEM_AS_STATIC : FirstPersonPlayerJointAnimator.RENDER_OFF_HAND_ITEM_AS_STATIC)
                            );


//                            if (!this.minecraft.isPaused()) {
//                                LocomotionMain.LOGGER.info(rightItemPose.getTransform().getScale(new Vector3f()));
//                            }

                            //this.renderItemInHand(abstractClientPlayer, ItemStack.EMPTY, poseStack, HumanoidArm.LEFT, animationPose, bufferSource, i);


                            //playerRenderer.renderRightHand(poseStack, bufferSource, i, abstractClientPlayer);
                            //poseStack.popPose();
                            poseStack.popPose();
                        }
                )
        );

        buffer.endBatch();
    }

    private void renderArm(AbstractClientPlayer abstractClientPlayer, PlayerModel playerModel, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int combinedLight) {
        PlayerSkin skin = abstractClientPlayer.getSkin();
        poseStack.pushPose();
        switch(arm){
            case LEFT -> {
                if (skin.model() == PlayerSkin.Model.SLIM) {
                    poseStack.translate(0.5 / 16f, 0, 0);
                }
                playerModel.leftSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
                playerModel.leftArm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), combinedLight, OverlayTexture.NO_OVERLAY);
            }
            case RIGHT -> {
                if (skin.model() == PlayerSkin.Model.SLIM) {
                    poseStack.translate(-0.5 / 16f, 0, 0);
                }
                playerModel.rightSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
                playerModel.rightArm.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(skin.texture())), combinedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        poseStack.popPose();
    }

    public void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            JointChannel jointChannel,
            MultiBufferSource bufferSource,
            int combinedLight,
            HumanoidArm side,
            FirstPersonPlayerJointAnimator.HandPose handPose,
            FirstPersonPlayerJointAnimator.GenericItemPose genericItemPose,
            boolean overrideStatic
    ) {
        if (!itemStack.isEmpty()) {
            ItemRenderType renderType = ItemRenderType.fromItemStack(itemStack, handPose, genericItemPose);
            if (overrideStatic) {
                renderType = ItemRenderType.THIRD_PERSON_ITEM_STATIC;
            }
            ItemStack itemStackToRender = renderType == ItemRenderType.THIRD_PERSON_ITEM_STATIC ? itemStack.copy() : itemStack;

            poseStack.pushPose();
            jointChannel.transformPoseStack(poseStack, 16f);

            if (shouldMirrorItem(side, handPose, genericItemPose)) {
                SHOULD_FLIP_ITEM_TRANSFORM = true;
            }
            switch (renderType) {
                case THIRD_PERSON_ITEM, THIRD_PERSON_ITEM_STATIC -> {
                    //? if >= 1.21.5 {
                    this.itemRenderer.renderStatic(entity, itemStackToRender, displayContext, poseStack, bufferSource, entity.level(), combinedLight, OverlayTexture.NO_OVERLAY, entity.getId() + displayContext.ordinal());
                    //?} else
                    /*this.itemRenderer.renderStatic(entity, itemStackToRender, displayContext, side == HumanoidArm.LEFT, poseStack, buffer, entity.level(), combinedLight, OverlayTexture.NO_OVERLAY, entity.getId() + displayContext.ordinal());*/
                }
                case DEFAULT_BLOCK_STATE -> {
                    Block block = ((BlockItem)itemStack.getItem()).getBlock();
                    BlockState blockState = this.getDefaultBlockState(block);

                    if (side == HumanoidArm.LEFT) {
                        poseStack.translate(-1, 0, 0);
                    }
                    if (block instanceof FenceGateBlock || block instanceof ConduitBlock) {
                        poseStack.translate(0, -0.4f, 0);
                    }
                    if (block instanceof SporeBlossomBlock) {
                        poseStack.translate(0, 1, 1);
                        poseStack.mulPose(Axis.XP.rotation(Mth.PI));
                    }

                    if (block instanceof WallBlock) {
                        this.renderWallBlock(blockState, poseStack, bufferSource, combinedLight);
                    } else if (block instanceof FenceBlock) {
                        this.renderFenceBlock(blockState, poseStack, bufferSource, combinedLight);
                    } else if (block instanceof BedBlock) {
                        this.renderBedBlock(blockState, poseStack, bufferSource, combinedLight);
                    } else if (block instanceof DoorBlock) {
                        this.renderDoorBlock(blockState, poseStack, bufferSource, combinedLight);
                    } else {
                        ((AlternateSingleBlockRenderer)(this.blockRenderer)).locomotion$renderSingleBlockWithEmission(blockState, poseStack, bufferSource, combinedLight);
//                        this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
                    }
                }
            }
            SHOULD_FLIP_ITEM_TRANSFORM = false;
            poseStack.popPose();
        }
    }



    private boolean shouldMirrorItem(HumanoidArm side, FirstPersonPlayerJointAnimator.HandPose handPose, FirstPersonPlayerJointAnimator.GenericItemPose genericItemPose) {
        if (side == HumanoidArm.RIGHT) {
            return false;
        }
        if (handPose != FirstPersonPlayerJointAnimator.HandPose.GENERIC_ITEM) {
            return false;
        }
        if (genericItemPose == FirstPersonPlayerJointAnimator.GenericItemPose.ARROW) {
            return true;
        }
        return false;
    }

    private BlockState getDefaultBlockState(Block block) {
        BlockState blockState = block.defaultBlockState();
        blockState = blockState.trySetValue(BlockStateProperties.ROTATION_16, 8);
        blockState = blockState.trySetValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        blockState = blockState.trySetValue(BlockStateProperties.DOWN, true);
        if (block instanceof StairBlock) {
            blockState = blockState.trySetValue(BlockStateProperties.FACING, Direction.NORTH);
            blockState = blockState.trySetValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        } else {
            blockState = blockState.trySetValue(BlockStateProperties.FACING, Direction.SOUTH);
            blockState = blockState.trySetValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        return blockState;
    }

    private void renderBedBlock(
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
        poseStack.translate(1f, -0.25f, 0.5f);
        poseStack.mulPose(Axis.YP.rotation(Mth.PI));
        this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
    }

    private void renderDoorBlock(
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
        this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
        this.renderUpperHalfBlock(blockState, poseStack, bufferSource, combinedLight);
    }

    private void renderFenceBlock(
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
        blockState = blockState.setValue(BlockStateProperties.EAST, true);
        blockState = blockState.setValue(BlockStateProperties.WEST, true);
        this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
    }

    private void renderWallBlock(
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
         blockState = blockState.setValue(BlockStateProperties.EAST_WALL, WallSide.LOW);
         blockState = blockState.setValue(BlockStateProperties.WEST_WALL, WallSide.LOW);
         this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
    }

    private void renderUpperHalfBlock(
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
        poseStack.translate(0, 1, 0);
        blockState = blockState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        this.blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, combinedLight, OverlayTexture.NO_OVERLAY);
    }

    public void transformCamera(PoseStack poseStack){
        if(this.minecraft.options.getCameraType().isFirstPerson()){
            this.jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().ifPresent(animationPose -> {
                JointChannel cameraPose = animationPose.getJointChannel(FirstPersonPlayerJointAnimator.CAMERA_JOINT);

                Vector3f cameraRot = cameraPose.getEulerRotationZYX();
                cameraRot.z *= -1;
                cameraPose.rotate(cameraRot, JointChannel.TransformSpace.LOCAL, JointChannel.TransformType.REPLACE);

                cameraPose.transformPoseStack(poseStack, 16f);
                //poseStack.mulPose(cameraPose.getTransform().setTranslation(cameraPose.getTransform().getTranslation(new Vector3f().div(16f))));
            });
        }
    }

    @Override
    public @NotNull PlayerModel getModel() {
        return ((PlayerRenderer)entityRenderDispatcher.getRenderer(minecraft.player)).getModel();
    }

    private enum ItemRenderType {
        THIRD_PERSON_ITEM,
        THIRD_PERSON_ITEM_STATIC,
        DEFAULT_BLOCK_STATE;

        public static final List<Item> STATIC_ITEMS = List.of(
                Items.SHIELD
        );

        public static ItemRenderType fromItemStack(ItemStack itemStack, FirstPersonPlayerJointAnimator.HandPose handPose, FirstPersonPlayerJointAnimator.GenericItemPose genericItemPose) {
            Item item = itemStack.getItem();
            if (genericItemPose.rendersBlockState && itemStack.getItem() instanceof BlockItem && handPose == FirstPersonPlayerJointAnimator.HandPose.GENERIC_ITEM) {
                return DEFAULT_BLOCK_STATE;
            }
            if (STATIC_ITEMS.contains(item)) {
                return THIRD_PERSON_ITEM_STATIC;
            }
            return THIRD_PERSON_ITEM;
        }
    }
}
