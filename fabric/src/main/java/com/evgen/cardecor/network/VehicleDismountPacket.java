package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class VehicleDismountPacket {
    public final int entityId;

    public VehicleDismountPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleDismountPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            Vec3 safePos = vehicle.getDismountLocationForPassenger(player);
            player.stopRiding();
            player.teleportTo(safePos.x, safePos.y, safePos.z);
        } else if (player != null) {
            player.stopRiding();
        }
    }
}
