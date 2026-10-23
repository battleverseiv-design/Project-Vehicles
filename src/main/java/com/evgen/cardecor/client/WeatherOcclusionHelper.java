package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class WeatherOcclusionHelper {

    private static final List<DrivableVehicleEntity> NEARBY_VEHICLES = new ArrayList<>();
    private static long lastUpdateTime = 0;
    private static double lastCamX = Double.NaN;
    private static double lastCamY = Double.NaN;
    private static double lastCamZ = Double.NaN;

    public static void prepareFrame(Level level, double camX, double camY, double camZ) {
        if (level == null) {
            NEARBY_VEHICLES.clear();
            return;
        }

        long now = System.currentTimeMillis();
        double dx = camX - lastCamX;
        double dy = camY - lastCamY;
        double dz = camZ - lastCamZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Re-cache every 50ms or when camera moves > 2.0 blocks
        if (now - lastUpdateTime > 50 || distSq > 4.0 || Double.isNaN(lastCamX)) {
            lastUpdateTime = now;
            lastCamX = camX;
            lastCamY = camY;
            lastCamZ = camZ;

            AABB box = new AABB(camX - 32.0, camY - 16.0, camZ - 32.0,
                                camX + 32.0, camY + 24.0, camZ + 32.0);
            NEARBY_VEHICLES.clear();
            List<DrivableVehicleEntity> found = level.getEntitiesOfClass(DrivableVehicleEntity.class, box);
            if (found != null && !found.isEmpty()) {
                NEARBY_VEHICLES.addAll(found);
            }
        }
    }

    public static int getOccludedHeight(Level level, int x, int z, int originalHeight) {
        if (NEARBY_VEHICLES.isEmpty()) {
            return originalHeight;
        }

        double sampleX = x + 0.5;
        double sampleZ = z + 0.5;
        int maxRoofHeight = originalHeight;

        for (int i = 0; i < NEARBY_VEHICLES.size(); i++) {
            DrivableVehicleEntity v = NEARBY_VEHICLES.get(i);
            if (v == null || !v.isAlive()) continue;

            double roofY = getVehicleRoofYAt(v, sampleX, sampleZ);
            if (roofY > maxRoofHeight) {
                maxRoofHeight = (int) Math.ceil(roofY);
            }
        }

        return maxRoofHeight;
    }

    public static BlockPos getOccludedHeightmapPos(Level level, BlockPos pos, BlockPos original) {
        if (level == null || original == null) return original;
        int occludedY = getOccludedHeight(level, original.getX(), original.getZ(), original.getY());
        if (occludedY > original.getY()) {
            return new BlockPos(original.getX(), occludedY, original.getZ());
        }
        return original;
    }

    public static double getVehicleRoofYAt(DrivableVehicleEntity vehicle, double worldX, double worldZ) {
        if (vehicle == null || !vehicle.isAlive()) return -1000.0;

        VehicleDefinition def = null;
        if (vehicle instanceof DrivablePackVehicleEntity packVeh) {
            def = com.evgen.cardecor.pack.VehiclePackManager.get(packVeh.getVehicleId());
        }

        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        if (cb != null && !cb.hasRoof) {
            return -1000.0;
        }

        // Transform world coordinate into vehicle local space
        float deg = 180.0F - vehicle.getYRot();
        float rad = (float) Math.toRadians(deg);
        double cosYaw = Math.cos(rad);
        double sinYaw = Math.sin(rad);

        double dx = worldX - vehicle.getX();
        double dz = worldZ - vehicle.getZ();

        // Local X (lateral)
        double localX = dx * cosYaw - dz * sinYaw;
        // Local Z (longitudinal: negative front, positive rear)
        double localZ = dx * sinYaw + dz * cosYaw;

        double halfW = (cb != null && cb.halfWidth > 0.1) ? cb.halfWidth : 1.15;
        if (Math.abs(localX) > halfW) {
            return -1000.0;
        }

        if (cb != null) {
            double cFront = cb.cabinFrontZ;
            double cRear = cb.cabinRearZ;
            double minZ = Math.min(cFront, cRear);
            double maxZ = Math.max(cFront, cRear);
            double frontDist = (cb.frontDist > 0.5) ? cb.frontDist : 2.85;
            double rearDist  = (cb.rearDist > 0.5)  ? cb.rearDist  : 2.85;

            if (localZ >= minZ && localZ <= maxZ) {
                // Inside cabin roof area
                double roofH = cb.roofHeight > 0.5 ? cb.roofHeight : cb.totalHeight;
                return vehicle.getY() + roofH;
            } else if (localZ < minZ && localZ >= -frontDist) {
                // Hood area: rain hits hood
                return vehicle.getY() + (cb.hoodHeight > 0.2 ? cb.hoodHeight : 1.0);
            } else if (localZ > maxZ && localZ <= rearDist) {
                // Cargo bed or trunk area
                if (cb.hasCargoBed) {
                    return vehicle.getY() + (cb.bedFloorHeight > 0.2 ? cb.bedFloorHeight : 0.75);
                } else {
                    double trunkH = cb.trunkHeight > 0.5 ? cb.trunkHeight : cb.roofHeight;
                    return vehicle.getY() + trunkH;
                }
            }
        } else {
            AABB bb = vehicle.getBoundingBox();
            if (worldX >= bb.minX && worldX <= bb.maxX && worldZ >= bb.minZ && worldZ <= bb.maxZ) {
                return bb.maxY;
            }
        }

        return -1000.0;
    }

    public static boolean isInsideVehicleCabin(double worldX, double worldY, double worldZ) {
        if (NEARBY_VEHICLES.isEmpty()) return false;
        for (int i = 0; i < NEARBY_VEHICLES.size(); i++) {
            DrivableVehicleEntity v = NEARBY_VEHICLES.get(i);
            if (v == null || !v.isAlive()) continue;

            VehicleDefinition def = null;
            if (v instanceof DrivablePackVehicleEntity packVeh) {
                def = com.evgen.cardecor.pack.VehiclePackManager.get(packVeh.getVehicleId());
            }
            VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
            if (cb != null && !cb.hasRoof) continue;

            float deg = 180.0F - v.getYRot();
            float rad = (float) Math.toRadians(deg);
            double cosYaw = Math.cos(rad);
            double sinYaw = Math.sin(rad);

            double dx = worldX - v.getX();
            double dz = worldZ - v.getZ();

            double localX = dx * cosYaw - dz * sinYaw;
            double localZ = dx * sinYaw + dz * cosYaw;

            double halfW = (cb != null && cb.halfWidth > 0.1) ? cb.halfWidth * 0.95 : 1.10;
            if (Math.abs(localX) > halfW) continue;

            double cFront = (cb != null) ? cb.cabinFrontZ : -1.40;
            double cRear = (cb != null) ? cb.cabinRearZ : 0.45;
            double minZ = Math.min(cFront, cRear) - 0.20;
            double maxZ = Math.max(cFront, cRear) + 0.10;

            if (localZ >= minZ && localZ <= maxZ) {
                double roofY = v.getY() + ((cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : 2.15);
                // Suppress any particle inside cabin footprint anywhere from ground/floor up to roof
                if (worldY >= v.getY() - 1.0 && worldY <= roofY + 0.2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static double getVehicleSplashY(double worldX, double worldY, double worldZ) {
        if (NEARBY_VEHICLES.isEmpty()) return worldY;
        for (int i = 0; i < NEARBY_VEHICLES.size(); i++) {
            DrivableVehicleEntity v = NEARBY_VEHICLES.get(i);
            if (v == null || !v.isAlive()) continue;

            double roofY = getVehicleRoofYAt(v, worldX, worldZ);
            if (roofY > -900.0 && worldY <= roofY + 0.25 && worldY >= v.getY() - 1.0) {
                return roofY + 0.02; // Splash on exterior metal surface
            }
        }
        return worldY;
    }
}
