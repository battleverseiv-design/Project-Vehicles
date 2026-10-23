package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class VehicleEngineSoundInstance extends AbstractTickableSoundInstance {

    private final DrivableVehicleEntity vehicle;
    public float targetVolume = 0.0F;
    public float targetPitch  = 1.0F;
    private boolean stopping = false;

    public VehicleEngineSoundInstance(DrivableVehicleEntity vehicle, SoundEvent sound) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay   = 0;
        this.volume  = 0.0F;
        this.pitch   = 1.0F;
        this.x = (float) vehicle.getX();
        this.y = (float) vehicle.getY();
        this.z = (float) vehicle.getZ();
        this.attenuation = Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved() || !vehicle.isAlive() || stopping) {
            this.volume = 0.0F;
            stop();
            return;
        }

        // Fast, responsive volume & pitch tracking (eliminating sluggish double-lag)
        this.volume = Mth.lerp(0.35F, this.volume, targetVolume);
        this.pitch  = Mth.lerp(0.25F, this.pitch,  targetPitch);

        this.x = (float) vehicle.getX();
        this.y = (float) vehicle.getY();
        this.z = (float) vehicle.getZ();
    }

    public float getCurrentVolume() {
        return this.volume;
    }

    public void markStopping() {
        this.stopping = true;
        this.targetVolume = 0.0F;
    }

    @Override
    public boolean canPlaySound() {
        return !vehicle.isRemoved() && vehicle.isAlive();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
