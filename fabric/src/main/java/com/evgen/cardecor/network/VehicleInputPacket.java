package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VehicleInputPacket {
    public final int entityId;
    public final int inputs;

    public VehicleInputPacket(int entityId, int inputs) {
        this.entityId = entityId;
        this.inputs = inputs;
    }

    public VehicleInputPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.inputs = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(inputs);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
            if (vehicle.hasPassenger(player)) {
                int seat = vehicle.getSeatForPlayer(player);
                if (seat == 0) {
                    if ((inputs & 32) != 0) {
                        vehicle.toggleEngine(player);
                    }
                    vehicle.setInput(inputs);
                }
            }
        }
    }
}
