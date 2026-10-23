package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleUpdatePacket {
    private final int entityId;
    private final double posX, posY, posZ;
    private final float yRot, pitch, roll;
    private final float speed, steer, rpm;
    private final int gear;
    private final byte inputs;

    public VehicleUpdatePacket(int entityId, double posX, double posY, double posZ,
                               float yRot, float pitch, float roll,
                               float speed, float steer, float rpm, int gear, byte inputs) {
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yRot = yRot;
        this.pitch = pitch;
        this.roll = roll;
        this.speed = speed;
        this.steer = steer;
        this.rpm = rpm;
        this.gear = gear;
        this.inputs = inputs;
    }

    public VehicleUpdatePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();
        this.yRot = buf.readFloat();
        this.pitch = buf.readFloat();
        this.roll = buf.readFloat();
        this.speed = buf.readFloat();
        this.steer = buf.readFloat();
        this.rpm = buf.readFloat();
        this.gear = buf.readInt();
        this.inputs = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeFloat(yRot);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
        buf.writeFloat(speed);
        buf.writeFloat(steer);
        buf.writeFloat(rpm);
        buf.writeInt(gear);
        buf.writeByte(inputs);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                if (vehicle.hasPassenger(player) && vehicle.getFirstPassenger() == player) {
                    vehicle.applyServerSync(posX, posY, posZ, yRot, pitch, roll, speed, steer, rpm, gear, inputs);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
