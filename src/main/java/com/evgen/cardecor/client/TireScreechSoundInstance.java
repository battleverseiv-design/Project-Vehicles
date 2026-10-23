package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class TireScreechSoundInstance extends AbstractTickableSoundInstance {

    private final DrivableVehicleEntity vehicle;
    private float smoothedDoppler = 1.0F;

    public TireScreechSoundInstance(DrivableVehicleEntity vehicle) {
        super(ModSounds.TIRE_SCREECH.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = (float) vehicle.getX();
        this.y = (float) vehicle.getY();
        this.z = (float) vehicle.getZ();
        this.attenuation = Attenuation.LINEAR;
    }

    public void stopSound() {
        this.stop();
    }

    @Override
    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.isAlive()) {
            this.volume = 0.0F;
            this.stop();
            return;
        }

        this.x = (float) this.vehicle.getX();
        this.y = (float) this.vehicle.getY();
        this.z = (float) this.vehicle.getZ();

        Minecraft mc = Minecraft.getInstance();
        double dist = (mc.player != null) ? vehicle.distanceTo(mc.player) : 0.0;
        float distFactor = VehicleEngineSoundManager.getDistanceAttenuation(dist);

        double intensity = this.vehicle.physics.tireScreechIntensity;
        float targetVol = (float) Mth.clamp(intensity * 0.85 * distFactor, 0.0, 0.90);
        this.volume = Mth.lerp(0.30F, this.volume, targetVol);

        // Apply Doppler shift for observers
        float targetDoppler = VehicleEngineSoundManager.calculateDopplerShift(vehicle, mc.player);
        smoothedDoppler = Mth.lerp(0.20F, smoothedDoppler, targetDoppler);

        float basePitch = (float) (0.88 + intensity * 0.25);
        this.pitch = basePitch * smoothedDoppler;
    }
}
