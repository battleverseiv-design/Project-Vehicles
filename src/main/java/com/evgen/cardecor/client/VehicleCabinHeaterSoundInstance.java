package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class VehicleCabinHeaterSoundInstance extends AbstractTickableSoundInstance {
    private final DrivableVehicleEntity vehicle;
    public float targetVolume = 0.0F;

    public VehicleCabinHeaterSoundInstance(DrivableVehicleEntity vehicle) {
        super(ModSounds.CAR_HEATER.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.15F;
        this.targetVolume = 0.15F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (vehicle.isRemoved() || !vehicle.isAlive() || mc.player == null || mc.player.getVehicle() != vehicle) {
            this.volume = 0.0F;
            stop();
            return;
        }

        int seat = vehicle.getSeatForPlayer(mc.player);
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(vehicle.getVehicleId())
            || (vehicle.getVehicleId() != null && vehicle.getVehicleId().contains("pickup"));
        boolean inCabin = !isPickup || (seat == 0 || seat == 1);

        byte climateMode = vehicle.getClimateMode();
        boolean heaterActive = vehicle.isEngineOn() && (climateMode == 1);

        if (inCabin && heaterActive) {
            targetVolume = 0.15F;
        } else {
            targetVolume = 0.0F;
        }

        this.volume = Mth.lerp(0.12F, this.volume, targetVolume);
        if (this.volume < 0.01F && targetVolume == 0.0F) {
            stop();
        }
    }
}
