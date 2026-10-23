package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Filters out rain droplets and splash particles (ParticleTypes.RAIN / WaterDropParticle)
 * that penetrate into closed vehicle cabins from the sky.
 */
public class VehicleCabinWeatherFilter {

    private static Field particlesField = null;
    private static Field particlesToAddField = null;
    private static boolean reflectionFailed = false;

    private static Field findParticlesMapField() {
        for (Field f : ParticleEngine.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Field findParticlesToAddField() {
        for (Field f : ParticleEngine.class.getDeclaredFields()) {
            if (Queue.class.isAssignableFrom(f.getType())) {
                String name = f.getName().toLowerCase();
                if (name.contains("add")) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        for (Field f : ParticleEngine.class.getDeclaredFields()) {
            if (Queue.class.isAssignableFrom(f.getType())) {
                String name = f.getName().toLowerCase();
                if (!name.contains("tracking") && !name.contains("emitter")) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    public static boolean isInsideCabin(DrivableVehicleEntity vehicle, double px, double py, double pz) {
        if (vehicle == null || !vehicle.isAlive()) return false;

        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(vehicle.getVehicleId())
            || (def != null && def.id != null && def.id.contains("pickup"));

        double cabinRearZ  = (cb != null && cb.cabinRearZ < -0.2) ? cb.cabinRearZ : (isPickup ? -0.70 : -1.35);
        double cabinFrontZ = (cb != null && cb.cabinFrontZ > 0.2) ? cb.cabinFrontZ : 0.85;
        double halfWidth   = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth * 0.95 : 1.15;
        double roofHeight  = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : 1.55;

        double dx = px - vehicle.getX();
        double dy = py - vehicle.getY();
        double dz = pz - vehicle.getZ();

        if (dy < 0.20 || dy > roofHeight + 0.15) return false;

        float radY = (float) Math.toRadians(vehicle.getYRot());
        double fX = -Math.sin(radY);
        double fZ = Math.cos(radY);
        double rX = Math.cos(radY);
        double rZ = Math.sin(radY);

        double localFwd = dx * fX + dz * fZ;
        double localLat = dx * rX + dz * rZ;

        return (localFwd >= cabinRearZ - 0.10 && localFwd <= cabinFrontZ + 0.10 && Math.abs(localLat) <= halfWidth);
    }

    public static void tickRainFilter(Minecraft mc) {
        if (mc == null || mc.level == null || mc.particleEngine == null || !mc.level.isRaining()) return;
        if (reflectionFailed) return;

        Player player = mc.player;
        if (player == null) return;

        List<DrivableVehicleEntity> nearbyVehicles = mc.level.getEntitiesOfClass(
            DrivableVehicleEntity.class,
            player.getBoundingBox().inflate(24.0, 8.0, 24.0)
        );
        if (nearbyVehicles.isEmpty()) return;

        try {
            if (particlesField == null) {
                particlesField = findParticlesMapField();
                if (particlesField == null) {
                    reflectionFailed = true;
                    return;
                }
            }
            if (particlesToAddField == null) {
                particlesToAddField = findParticlesToAddField();
            }

            @SuppressWarnings("unchecked")
            Map<?, Queue<Particle>> map = (Map<?, Queue<Particle>>) particlesField.get(mc.particleEngine);
            if (map != null) {
                for (Queue<Particle> q : map.values()) {
                    if (q == null || q.isEmpty()) continue;
                    for (Particle p : q) {
                        if (p == null || !p.isAlive()) continue;
                        String name = p.getClass().getSimpleName();
                        if (name.contains("Water") || name.contains("Rain") || name.contains("Drip")) {
                            Vec3 pos = p.getBoundingBox().getCenter();
                            for (DrivableVehicleEntity v : nearbyVehicles) {
                                if (isInsideCabin(v, pos.x, pos.y, pos.z)) {
                                    p.remove();
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (particlesToAddField != null) {
                @SuppressWarnings("unchecked")
                Queue<Particle> queue = (Queue<Particle>) particlesToAddField.get(mc.particleEngine);
                if (queue != null && !queue.isEmpty()) {
                    for (Particle p : queue) {
                        if (p == null || !p.isAlive()) continue;
                        String name = p.getClass().getSimpleName();
                        if (name.contains("Water") || name.contains("Rain") || name.contains("Drip")) {
                            Vec3 pos = p.getBoundingBox().getCenter();
                            for (DrivableVehicleEntity v : nearbyVehicles) {
                                if (isInsideCabin(v, pos.x, pos.y, pos.z)) {
                                    p.remove();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            reflectionFailed = true;
        }
    }
}
