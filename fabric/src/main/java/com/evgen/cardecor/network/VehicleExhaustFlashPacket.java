package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;

public class VehicleExhaustFlashPacket {
    public final double x, y, z;
    public final double fwdX, fwdZ;
    public final boolean isHeavyPop;

    public VehicleExhaustFlashPacket(double x, double y, double z, double fwdX, double fwdZ, boolean isHeavyPop) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.fwdX = fwdX;
        this.fwdZ = fwdZ;
        this.isHeavyPop = isHeavyPop;
    }

    public VehicleExhaustFlashPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.fwdX = buf.readDouble();
        this.fwdZ = buf.readDouble();
        this.isHeavyPop = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(fwdX);
        buf.writeDouble(fwdZ);
        buf.writeBoolean(isHeavyPop);
    }
}
