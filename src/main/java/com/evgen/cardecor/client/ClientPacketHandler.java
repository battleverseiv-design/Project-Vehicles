package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class ClientPacketHandler {
    public static void handleDamageSparks(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            VehicleVFX.spawnBulletImpactSparks(mc.level, x, y, z);
        }
    }

    public static void handleExhaustFlash(double x, double y, double z, double fwdX, double fwdZ, boolean isHeavyPop) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            VehicleVFX.spawnExhaustFlashAndSmoke(mc.level, x, y, z, fwdX, fwdZ, isHeavyPop);
        }
    }

    public static void handleImpactDeform(int entityId, float localX, float localY, float localZ, float dirX, float dirY, float dirZ, float radius, float depth) {
        // Disabled: mesh deformation is currently disabled to preserve vehicle texture and material fidelity
    }

    public static void handleVehicleRepair(int entityId) {
        DrivablePackVehicleRenderer.deformedChassisMeshes.remove(entityId);
    }
}
