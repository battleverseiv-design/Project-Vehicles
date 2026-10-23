package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class VehicleCabinRainSoundInstance extends AbstractTickableSoundInstance {
    private final DrivableVehicleEntity vehicle;
    public float targetVolume = 0.0F;

    public VehicleCabinRainSoundInstance(DrivableVehicleEntity vehicle) {
        super(ModSounds.CAR_RAIN_CABIN.get(), SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.25F;
        this.targetVolume = 0.25F;
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

        net.minecraft.core.BlockPos pos = vehicle.blockPosition();
        net.minecraft.world.level.biome.Biome biome = vehicle.level().getBiome(pos).value();
        boolean isSnow = biome.coldEnoughToSnow(pos) || biome.getPrecipitationAt(pos) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;

        boolean raining = !isSnow && vehicle.level().isRaining()
            && (vehicle.level().canSeeSky(pos.above(2))
                || vehicle.level().isRainingAt(pos));

        if (inCabin && raining) {
            float rainLevel = vehicle.level().getRainLevel(1.0F);
            targetVolume = Math.max(0.18F, rainLevel * 0.28F);
        } else {
            targetVolume = 0.0F;
        }

        this.volume = Mth.lerp(0.08F, this.volume, targetVolume);
        if (this.volume < 0.02F && targetVolume == 0.0F) {
            stop();
        }
    }
}
