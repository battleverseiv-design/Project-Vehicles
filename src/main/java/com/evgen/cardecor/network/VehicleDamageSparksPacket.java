package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleDamageSparksPacket {
    private final double x, y, z;

    public VehicleDamageSparksPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VehicleDamageSparksPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.evgen.cardecor.client.ClientPacketHandler.handleDamageSparks(x, y, z);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
