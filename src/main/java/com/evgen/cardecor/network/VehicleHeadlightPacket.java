package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleHeadlightPacket {
    private final int entityId;

    public VehicleHeadlightPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleHeadlightPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                vehicle.toggleHeadlights();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
