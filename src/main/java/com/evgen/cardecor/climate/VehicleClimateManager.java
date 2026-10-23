package com.evgen.cardecor.climate;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class VehicleClimateManager {

    /**
     * Ticked every 20 ticks (1 second) on server when vehicle engine is running and climate is active.
     */
    public static void tickVehicleClimate(DrivableVehicleEntity vehicle, byte climate) {
        if (climate == 0) {
            clearVehicleClimate(vehicle);
            return;
        }
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(vehicle.getVehicleId()) ||
                           (vehicle.getVehicleId() != null && vehicle.getVehicleId().contains("pickup"));

        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                if (living instanceof Player player) {
                    int seatIdx = vehicle.getSeatForPlayer(player);
                    // Open cargo bed of pickup is exposed to weather - not heated by cab heater!
                    if (isPickup && seatIdx >= 2) {
                        VehicleClimateCompat.clear(living);
                        continue;
                    }
                    VehicleClimateCompat.apply(living, climate);
                } else {
                    VehicleClimateCompat.apply(living, climate);
                }
            }
        }
    }

    public static void clearVehicleClimate(DrivableVehicleEntity vehicle) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                VehicleClimateCompat.clear(living);
            }
        }
    }

    public static void onPassengerDismount(Entity passenger) {
        if (passenger instanceof LivingEntity living) {
            VehicleClimateCompat.clear(living);
        }
    }
}
