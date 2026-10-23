package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleChangeSeatPacket {
    private final int entityId;
    private final int targetSeat; // -1 = cycle next, 0 = driver, 1 = passenger, 2 = trunk

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                if (vehicle.hasPassenger(player)) {
                    if (targetSeat == -1) {
                        vehicle.requestCycleSeat(player);
                    } else {
                        vehicle.requestSwitchSeat(player, targetSeat);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
