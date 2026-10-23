package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleInputPacket {
    private final int entityId;
    private final int inputs; // Bitmask: 1=W, 2=S, 4=A, 8=D, 16=Space, 32=EngineToggle, 64=Boost, 128=Horn, 256=Clutch, 512=ShiftUp, 1024=ShiftDown

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                if (vehicle.hasPassenger(player)) {
                    int seat = vehicle.getSeatForPlayer(player);
                    if (seat == 0) { // STRICTLY DRIVER ONLY can control vehicle throttle, steer and engine!
                        if ((inputs & 32) != 0) {
                            vehicle.toggleEngine(player);
                        }
                        vehicle.setInput(inputs);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
