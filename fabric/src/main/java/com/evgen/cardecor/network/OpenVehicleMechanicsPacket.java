package com.evgen.cardecor.network;

import com.evgen.cardecor.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;

public class OpenVehicleMechanicsPacket {
    private final int vehicleId;

    public OpenVehicleMechanicsPacket(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public OpenVehicleMechanicsPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
    }

    public void handleClient() {
        ClientPacketHandlers.handleOpenVehicleMechanics(this.vehicleId);
    }
}
