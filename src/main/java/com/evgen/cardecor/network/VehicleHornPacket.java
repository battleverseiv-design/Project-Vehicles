package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class VehicleHornPacket {
    private final int vehicleId;
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

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                long now = System.currentTimeMillis();
                UUID vid = vehicle.getUUID();
                long last = LAST_HORN_TIME.getOrDefault(vid, 0L);
                if (now - last > 1400L) { // 1.4s anti-overlap cooldown
                    LAST_HORN_TIME.put(vid, now);
                    vehicle.level().playSound(
                        null,
                        vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        ModSounds.CAR_HORN.get(),
                        SoundSource.NEUTRAL,
                        2.0F, 1.0F
                    );
                    vehicle.pulseNoise(90.0F);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
