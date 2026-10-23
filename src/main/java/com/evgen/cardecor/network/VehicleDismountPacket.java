package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleDismountPacket {
    private final int entityId;

    public VehicleDismountPacket(int entityId) {
        this.entityId = entityId;
    }

    public VehicleDismountPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                Vec3 safePos = vehicle.getDismountLocationForPassenger(player);
                vehicle.setAllowedToDismount(true);
                player.stopRiding();
                vehicle.setAllowedToDismount(false);
                player.teleportTo(safePos.x, safePos.y, safePos.z);
            } else if (player != null) {
                player.stopRiding();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
