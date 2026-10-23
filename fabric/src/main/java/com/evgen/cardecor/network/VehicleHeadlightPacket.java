package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VehicleHeadlightPacket {
    public final int entityId;

    public VehicleHeadlightPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleHeadlightPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
            vehicle.toggleHeadlights();
        }
    }
}
