package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class VehicleChangeEnginePacket {
    private final int entityId;
    private final String engineId;

    public VehicleChangeEnginePacket(int entityId, String engineId) {
        this.entityId = entityId;
        this.engineId = engineId != null ? engineId : "";
    }

    public VehicleChangeEnginePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.engineId = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(engineId, 256);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
            if (vehicle.hasPassenger(player) || player.distanceToSqr(vehicle) < 64.0) {
                vehicle.setInstalledEngine(engineId);
                com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(engineId);
                String engName = (eng != null && eng.name != null) ? eng.name : engineId;
                player.displayClientMessage(Component.literal("§a[🔧] Установлен двигатель: §e" + engName), true);
                vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                    com.evgen.cardecor.init.ModSounds.WHEEL_HUB_GRIND.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.85F, 1.20F);
            }
        }
    }
}
