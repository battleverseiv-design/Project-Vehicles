package com.evgen.cardecor.physics.zerogc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * High-precision, compact Netty synchronization packet for 3D Rigid Body physics.
 * Serializes position, quaternion orientation (gimbal-lock free), linear/angular velocity,
 * and 4-wheel suspension compressions.
 */
public class VehiclePhysicsPacket {

    private final int entityId;
    private final double posX, posY, posZ;
    private final float qx, qy, qz, qw;
    private final float vx, vy, vz;
    private final float wx, wy, wz;
    private final float comp0, comp1, comp2, comp3;
    private final float steerAngle;
    private final boolean isSleeping;

    public VehiclePhysicsPacket(int entityId,
                                double posX, double posY, double posZ,
                                Quaternionf q,
                                Vector3f linVel,
                                Vector3f angVel,
                                float[] compressions,
                                float steerAngle,
                                boolean isSleeping) {
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.qx = q.x;
        this.qy = q.y;
        this.qz = q.z;
        this.qw = q.w;
        this.vx = linVel.x;
        this.vy = linVel.y;
        this.vz = linVel.z;
        this.wx = angVel.x;
        this.wy = angVel.y;
        this.wz = angVel.z;
        this.comp0 = compressions.length > 0 ? compressions[0] : 0.0f;
        this.comp1 = compressions.length > 1 ? compressions[1] : 0.0f;
        this.comp2 = compressions.length > 2 ? compressions[2] : 0.0f;
        this.comp3 = compressions.length > 3 ? compressions[3] : 0.0f;
        this.steerAngle = steerAngle;
        this.isSleeping = isSleeping;
    }

    public VehiclePhysicsPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();
        this.qx = buf.readFloat();
        this.qy = buf.readFloat();
        this.qz = buf.readFloat();
        this.qw = buf.readFloat();
        this.vx = buf.readFloat();
        this.vy = buf.readFloat();
        this.vz = buf.readFloat();
        this.wx = buf.readFloat();
        this.wy = buf.readFloat();
        this.wz = buf.readFloat();
        this.comp0 = buf.readFloat();
        this.comp1 = buf.readFloat();
        this.comp2 = buf.readFloat();
        this.comp3 = buf.readFloat();
        this.steerAngle = buf.readFloat();
        this.isSleeping = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeFloat(qx);
        buf.writeFloat(qy);
        buf.writeFloat(qz);
        buf.writeFloat(qw);
        buf.writeFloat(vx);
        buf.writeFloat(vy);
        buf.writeFloat(vz);
        buf.writeFloat(wx);
        buf.writeFloat(wy);
        buf.writeFloat(wz);
        buf.writeFloat(comp0);
        buf.writeFloat(comp1);
        buf.writeFloat(comp2);
        buf.writeFloat(comp3);
        buf.writeFloat(steerAngle);
        buf.writeBoolean(isSleeping);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                handleServer(ctx.getSender());
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient);
            }
        });
        ctx.setPacketHandled(true);
    }

    private void handleServer(ServerPlayer sender) {
        if (sender == null) return;
        Entity target = sender.level().getEntity(entityId);
        if (target instanceof PhysicsVehicleEntity vehicle) {
            // Validate sender is the pilot
            if (vehicle.hasPassenger(sender) && vehicle.getFirstPassenger() == sender) {
                vehicle.applyPhysicsSync(posX, posY, posZ, qx, qy, qz, qw, vx, vy, vz, wx, wy, wz, comp0, comp1, comp2, comp3, steerAngle, isSleeping);
                // Relay to tracking clients
                vehicle.broadcastPhysicsSync();
            }
        }
    }

    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity target = mc.level.getEntity(entityId);
        if (target instanceof PhysicsVehicleEntity vehicle) {
            // Remote clients receive authoritative state from server
            if (!vehicle.isControlledByLocalInstance()) {
                vehicle.applyPhysicsSync(posX, posY, posZ, qx, qy, qz, qw, vx, vy, vz, wx, wy, wz, comp0, comp1, comp2, comp3, steerAngle, isSleeping);
            }
        }
    }
}
