package com.evgen.cardecor.network;

import net.minecraft.network.FriendlyByteBuf;

public class VehicleImpactDeformPacket {
    public final int entityId;
    public final float localX, localY, localZ;
    public final float dirX, dirY, dirZ;
    public final float radius, depth;

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

    public VehicleImpactDeformPacket(int entityId, float localX, float localY, float localZ, float dirX, float dirY, float dirZ, float radius) {
        this(entityId, localX, localY, localZ, dirX, dirY, dirZ, radius, 0.25F);
    }

    public VehicleImpactDeformPacket(int entityId, float localX, float localY, float localZ, float dirX, float dirY, float dirZ) {
        this(entityId, localX, localY, localZ, dirX, dirY, dirZ, 0.85F, 0.15F);
    }

    public VehicleImpactDeformPacket(int entityId, float localX, float localY, float localZ) {
        this(entityId, localX, localY, localZ, 0.0F, 0.0F, 0.0F, 0.85F, 0.15F);
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
}
