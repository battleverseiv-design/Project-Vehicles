package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleExhaustFlashPacket {
    private final double x, y, z;
    private final double fwdX, fwdZ;
    private final boolean isHeavyPop;

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.evgen.cardecor.client.ClientPacketHandler.handleExhaustFlash(x, y, z, fwdX, fwdZ, isHeavyPop);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
