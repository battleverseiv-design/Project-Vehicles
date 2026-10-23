package com.evgen.cardecor.client.render;

import com.evgen.cardecor.block.TireChangerBlock;
import com.evgen.cardecor.block.TireChangerBlockEntity;
import com.evgen.cardecor.client.DirectObjModel;
import com.evgen.cardecor.client.VehicleRenderTypes;
import com.evgen.cardecor.item.VehicleRimItem;
import com.evgen.cardecor.item.VehicleTireItem;
import com.evgen.cardecor.item.VehicleWheelItem;
import com.evgen.cardecor.pack.ModularWheelRegistry;
import com.evgen.cardecor.pack.RimDefinition;
import com.evgen.cardecor.pack.TireDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TireChangerBlockEntityRenderer implements BlockEntityRenderer<TireChangerBlockEntity> {

    private static final ResourceLocation STATION_TEXTURE = new ResourceLocation("cardecor", "textures/block/tire_changer_station.png");
    private static final ResourceLocation STATION_MODEL = new ResourceLocation("cardecor", "models/block/tire_changer_station.obj");
    private static final ResourceLocation WHEEL_PALETTE = new ResourceLocation("cardecor", "textures/wheel_palette.png");

    private static DirectObjModel stationModel;
    private static final Map<String, DirectObjModel> MODEL_CACHE = new ConcurrentHashMap<>();

    public TireChangerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static DirectObjModel getStationModel() {
        if (stationModel == null) {
            stationModel = new DirectObjModel(STATION_MODEL);
        }
        return stationModel;
    }

    private static DirectObjModel getModel(String relPath) {
        if (relPath == null || relPath.isEmpty()) return null;
        return MODEL_CACHE.computeIfAbsent(relPath, p -> {
            String path = p.startsWith("models/") ? p : "models/" + p;
            return new DirectObjModel(new ResourceLocation("cardecor", path));
        });
    }

    @Override
    public void render(TireChangerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(TireChangerBlock.FACING) ? state.getValue(TireChangerBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        float rot = -facing.toYRot() + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        // 1. Render Tire Changer Station Chassis
        DirectObjModel station = getStationModel();
        VertexConsumer stationVc = bufferSource.getBuffer(VehicleRenderTypes.getVehicleTriangles(STATION_TEXTURE));
        station.render(poseStack, stationVc, packedLight, packedOverlay);

        // 2. Render Wheel / Tire / Rim resting horizontally on Turntable Platform
        ItemStack wheelStack = blockEntity.getStoredWheel();
        ItemStack tireStack = blockEntity.getStoredTire();
        ItemStack rimStack = blockEntity.getStoredRim();

        String tireId = null;
        String rimId = null;

        if (!wheelStack.isEmpty()) {
            tireId = VehicleWheelItem.getTireId(wheelStack);
            rimId = VehicleWheelItem.getRimId(wheelStack);
        } else {
            if (!tireStack.isEmpty()) tireId = VehicleTireItem.getTireId(tireStack);
            if (!rimStack.isEmpty()) rimId = VehicleRimItem.getRimId(rimStack);
        }

        if (tireId != null || rimId != null) {
            TireDefinition tDef = (tireId != null) ? ModularWheelRegistry.getTire(tireId) : null;
            RimDefinition rDef = (rimId != null) ? ModularWheelRegistry.getRim(rimId) : null;

            double widthM = (tDef != null && tDef.width > 0.05) ? tDef.width : 0.22;
            // Technical standard: Turntable platform is strictly at Y = 7.0 px = 0.4375 m
            double tableY = 0.4375;
            double centerY = tableY + (widthM / 2.0);

            poseStack.pushPose();
            // Centered strictly at spindle [X = 0.0, Z = 0.0], raised to table height + half width
            poseStack.translate(0.0, centerY, 0.0);

            // Rotate 90 degrees around Z to lay the wheel horizontally flat with outer lip facing UP
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            VertexConsumer partVc = bufferSource.getBuffer(VehicleRenderTypes.getVehicleTriangles(WHEEL_PALETTE));

            // Render Tire if present
            if (tDef != null && tDef.model != null) {
                DirectObjModel tireM = getModel(tDef.model);
                if (tireM != null) {
                    tireM.render(poseStack, partVc, packedLight, packedOverlay);
                }
            }

            // Render Rim if present
            if (rDef != null && rDef.model != null) {
                DirectObjModel rimM = getModel(rDef.model);
                if (rimM != null) {
                    rimM.render(poseStack, partVc, packedLight, packedOverlay);
                }
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
