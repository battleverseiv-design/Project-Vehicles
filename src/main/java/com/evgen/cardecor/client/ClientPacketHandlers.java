package com.evgen.cardecor.client;

import com.evgen.cardecor.client.gui.VehicleTuningScreen;
import net.minecraft.client.Minecraft;

public class ClientPacketHandlers {
    public static void handleOpenTuningGui(String packId) {
        Minecraft.getInstance().setScreen(new VehicleTuningScreen(packId));
    }

    public static void handleOpenVehicleMechanics(int vehicleId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(vehicleId) instanceof com.evgen.cardecor.entity.DrivableVehicleEntity vehicle) {
            mc.setScreen(new com.evgen.cardecor.client.gui.VehicleMechanicsScreen(vehicle));
        }
    }

    public static void handleOpenEngineWorkbench(net.minecraft.core.BlockPos pos) {
        Minecraft.getInstance().setScreen(new com.evgen.cardecor.client.gui.EngineWorkbenchScreen(pos));
    }
}
