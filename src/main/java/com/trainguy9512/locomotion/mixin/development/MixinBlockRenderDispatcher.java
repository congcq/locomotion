package com.trainguy9512.locomotion.mixin.development;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trainguy9512.locomotion.access.AlternateSingleBlockRenderer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(BlockRenderDispatcher.class)
public abstract class MixinBlockRenderDispatcher implements AlternateSingleBlockRenderer {

    @Shadow public abstract BlockStateModel getBlockModel(BlockState arg);

    @Shadow @Final private BlockColors blockColors;

    public void renderShadedSingleBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight) {
        RenderShape renderShape = blockState.getRenderShape();
        if (renderShape == RenderShape.INVISIBLE) {
            return;
        }
        BlockStateModel blockStateModel = this.getBlockModel(blockState);
        int i = this.blockColors.getColor(blockState, null, null, 0);
        float f = (float)(i >> 16 & 0xFF) / 255.0f;
        float g = (float)(i >> 8 & 0xFF) / 255.0f;
        float h = (float)(i & 0xFF) / 255.0f;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(blockState));
        for (BlockModelPart blockModelPart : blockStateModel.collectParts(RandomSource.create(42L))) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad bakedQuad : blockModelPart.getQuads(direction)) {
                    if (bakedQuad.isTinted()) {
                        f = Mth.clamp(f, 0.0f, 1.0f);
                        g = Mth.clamp(g, 0.0f, 1.0f);
                        h = Mth.clamp(h, 0.0f, 1.0f);
                    } else {
                        f = 1.0f;
                        g = 1.0f;
                        h = 1.0f;
                    }
                    vertexConsumer.putBulkData(poseStack.last(), bakedQuad, f, g, h, 1.0f, combinedLight, OverlayTexture.NO_OVERLAY);
                }
            }
//            ModelBlockRenderer.renderQuadList(poseStack, bufferSource, f, g, h, blockModelPart.getQuads(null), combinedLight, OverlayTexture.NO_OVERLAY);
        }
//        ModelBlockRenderer.renderModel(poseStack.last(), bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(blockState)), blockStateModel, f, g, h, packedLight, packedOverlay);
//        this.blockRenderer.specialBlockModelRenderer.get().renderByBlock(state.getBlock(), ItemDisplayContext.NONE, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
