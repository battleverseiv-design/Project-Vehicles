package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;

public class VehicleDamageSparksPacket {
    public final double x, y, z;

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
}
