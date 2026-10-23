package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VehicleChangeSeatPacket {
    public final int entityId;
    public final int targetSeat;

    public VehicleChangeSeatPacket(int entityId, int targetSeat) {
        this.entityId = entityId;
        this.targetSeat = targetSeat;
    }

    public VehicleChangeSeatPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetSeat = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(targetSeat);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
            if (vehicle.hasPassenger(player)) {
                if (targetSeat == -1) {
                    vehicle.requestCycleSeat(player);
                } else {
                    vehicle.requestSwitchSeat(player, targetSeat);
                }
            }
        }
    }
}
