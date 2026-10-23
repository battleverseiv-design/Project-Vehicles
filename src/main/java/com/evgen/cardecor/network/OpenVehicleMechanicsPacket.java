package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenVehicleMechanicsPacket {

    private final int vehicleId;

    public OpenVehicleMechanicsPacket(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public OpenVehicleMechanicsPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.vehicleId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.evgen.cardecor.client.ClientPacketHandlers.handleOpenVehicleMechanics(this.vehicleId));
        });
        ctx.get().setPacketHandled(true);
    }
}
