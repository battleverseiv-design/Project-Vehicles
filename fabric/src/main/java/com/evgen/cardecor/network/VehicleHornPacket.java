package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehicleHornPacket {
    public final int vehicleId;
    private static final Map<UUID, Long> LAST_HORN_TIME = new HashMap<>();

    public VehicleHornPacket(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleHornPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.vehicleId);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            long now = System.currentTimeMillis();
            UUID vid = vehicle.getUUID();
            long last = LAST_HORN_TIME.getOrDefault(vid, 0L);
            if (now - last > 1400L) {
                LAST_HORN_TIME.put(vid, now);
                vehicle.level().playSound(
                    null,
                    vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                    ModSounds.CAR_HORN.get(),
                    SoundSource.NEUTRAL,
                    2.0F, 1.0F
                );
            }
        }
    }
}
