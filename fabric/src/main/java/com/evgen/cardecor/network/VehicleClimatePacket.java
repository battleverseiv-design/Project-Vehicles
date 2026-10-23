package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class VehicleClimatePacket {
    private final int entityId;

    public VehicleClimatePacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleClimatePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender responseSender) {
        server.execute(() -> {
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                if (vehicle.hasPassenger(player) || player.distanceToSqr(vehicle) < 49.0) {
                    vehicle.toggleClimate(player);
                }
            }
        });
    }
}
