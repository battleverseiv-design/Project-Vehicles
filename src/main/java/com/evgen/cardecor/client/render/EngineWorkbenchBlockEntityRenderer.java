package com.evgen.cardecor.client.render;

import com.evgen.cardecor.block.EngineWorkbenchBlock;
import com.evgen.cardecor.block.EngineWorkbenchBlockEntity;
import com.evgen.cardecor.client.DirectObjModel;
import com.evgen.cardecor.client.VehicleRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class EngineWorkbenchBlockEntityRenderer implements BlockEntityRenderer<EngineWorkbenchBlockEntity> {

    private static final ResourceLocation WORKBENCH_TEXTURE = new ResourceLocation("cardecor", "textures/block/workbench_texture.png");
    private static final ResourceLocation WORKBENCH_MODEL = new ResourceLocation("cardecor", "models/block/mechanic_workbench.obj");

    private static DirectObjModel workbenchModel;

    public EngineWorkbenchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static DirectObjModel getWorkbenchModel() {
        if (workbenchModel == null) {
            workbenchModel = new DirectObjModel(WORKBENCH_MODEL);
        }
        return workbenchModel;
    }

    @Override
    public void render(EngineWorkbenchBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(EngineWorkbenchBlock.FACING) ? state.getValue(EngineWorkbenchBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        float rot = -facing.toYRot() + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        DirectObjModel model = getWorkbenchModel();
        VertexConsumer consumer = bufferSource.getBuffer(VehicleRenderTypes.getVehicleTriangles(WORKBENCH_TEXTURE));
        model.render(poseStack, consumer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
