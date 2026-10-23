package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;

public class VehicleRepairPacket {
    public final int entityId;

    public VehicleRepairPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleRepairPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }
}
