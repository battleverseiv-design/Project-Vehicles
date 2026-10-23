package com.evgen.cardecor.client.render;

import com.evgen.cardecor.block.EngineCraneBlock;
import com.evgen.cardecor.block.EngineCraneBlockEntity;
import com.evgen.cardecor.client.DirectObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

public class EngineCraneBlockEntityRenderer implements BlockEntityRenderer<EngineCraneBlockEntity> {

    private static final ResourceLocation CRANE_TEXTURE = new ResourceLocation("cardecor", "textures/block/engine_crane.png");
    private static final ResourceLocation CRANE_MODEL = new ResourceLocation("cardecor", "models/block/engine_crane.obj");

    private static DirectObjModel craneModel;

    public EngineCraneBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static DirectObjModel getCraneModel() {
        if (craneModel == null) {
            craneModel = new DirectObjModel(CRANE_MODEL);
        }
        return craneModel;
    }

    @Override
    public void render(EngineCraneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(EngineCraneBlock.FACING) ? state.getValue(EngineCraneBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        float rot = -facing.toYRot() + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        // +40% size as requested by user
        poseStack.scale(1.4F, 1.4F, 1.4F);

        // Render crane chassis with TRIANGLES buffer (fixes shredded texture bug caused by QUADS buffer)
        DirectObjModel model = getCraneModel();
        VertexConsumer consumer = bufferSource.getBuffer(com.evgen.cardecor.client.VehicleRenderTypes.getVehicleTriangles(CRANE_TEXTURE));
        model.render(poseStack, consumer, packedLight, packedOverlay);

        // Render suspended engine if present
        if (blockEntity.hasEngine()) {
            poseStack.pushPose();
            // Hook bottom is at (0.0, 0.30, -0.73)
            poseStack.translate(0.0, 0.28, -0.73);
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            Minecraft.getInstance().getItemRenderer().renderStatic(
                blockEntity.getHeldEngine(),
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
