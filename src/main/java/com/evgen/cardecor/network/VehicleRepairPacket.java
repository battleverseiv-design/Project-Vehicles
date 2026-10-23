package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleRepairPacket {
    private final int entityId;

    public VehicleRepairPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleRepairPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.evgen.cardecor.client.ClientPacketHandler.handleVehicleRepair(entityId);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
