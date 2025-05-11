package com.trainguy9512.locomotion.access;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

public interface AlternateSingleBlockRenderer {
    void renderShadedSingleBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight);
}
