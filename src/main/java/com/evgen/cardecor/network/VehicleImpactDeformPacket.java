package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleImpactDeformPacket {
    private final int entityId;
    private final float localX, localY, localZ;
    private final float dirX, dirY, dirZ;
    private final float radius, depth;

    public VehicleImpactDeformPacket(int entityId, float localX, float localY, float localZ, float dirX, float dirY, float dirZ, float radius, float depth) {
        this.entityId = entityId;
        this.localX = localX;
        this.localY = localY;
        this.localZ = localZ;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.radius = radius;
        this.depth = depth;
    }

    public VehicleImpactDeformPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.localX = buf.readFloat();
        this.localY = buf.readFloat();
        this.localZ = buf.readFloat();
        this.dirX = buf.readFloat();
        this.dirY = buf.readFloat();
        this.dirZ = buf.readFloat();
        this.radius = buf.readFloat();
        this.depth = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(localX);
        buf.writeFloat(localY);
        buf.writeFloat(localZ);
        buf.writeFloat(dirX);
        buf.writeFloat(dirY);
        buf.writeFloat(dirZ);
        buf.writeFloat(radius);
        buf.writeFloat(depth);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.evgen.cardecor.client.ClientPacketHandler.handleImpactDeform(entityId, localX, localY, localZ, dirX, dirY, dirZ, radius, depth);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
