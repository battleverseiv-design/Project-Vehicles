package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleSeatActionPacket {
    public static final int ACTION_SIT = 0;
    public static final int ACTION_OPEN_INVENTORY = 1;
    public static final int ACTION_OPEN_TRUNK = 2;
    public static final int ACTION_TOGGLE_LOCK = 3;
    public static final int ACTION_INSERT_KEY = 4;
    public static final int ACTION_REMOVE_KEY = 5;
    public static final int ACTION_TOGGLE_TRUNK_LOCK = 6;
    public static final int ACTION_UNINSTALL_WHEEL = 7;
    public static final int ACTION_REMOVE_SPARE_WHEEL = 8;
    public static final int ACTION_TOGGLE_CABIN_LIGHT = 9;
    public static final int ACTION_HOTWIRE = 10;
    public static final int ACTION_TOGGLE_PARKING_BRAKE = 11;

    private final int entityId;
    private final int seat;
    private final int action;

    public VehicleSeatActionPacket(int entityId, int seat, int action) {
        this.entityId = entityId;
        this.seat = seat;
        this.action = action;
    }

    public VehicleSeatActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.seat = buf.readInt();
        this.action = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(seat);
        buf.writeInt(action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().getEntity(entityId) instanceof DrivableVehicleEntity vehicle) {
                if (action == ACTION_OPEN_INVENTORY) {
                    vehicle.openSeatInventory(player, seat);
                } else if (action == ACTION_OPEN_TRUNK) {
                    vehicle.openTrunk(player);
                } else if (action == ACTION_SIT) {
                    vehicle.sitPlayerInSeat(player, seat);
                } else if (action == ACTION_TOGGLE_LOCK) {
                    boolean newLock = !vehicle.isLocked();
                    vehicle.setLocked(newLock);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        newLock ? "§c[🔒] Автомобиль заперт!" : "§a[🔓] Автомобиль открыт!"), true);
                    vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        newLock ? com.evgen.cardecor.init.ModSounds.CAR_LOCK_PIN_DOWN.get() : com.evgen.cardecor.init.ModSounds.CAR_UNLOCK_PIN_UP.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.0F);
                } else if (action == ACTION_INSERT_KEY) {
                    vehicle.insertKeyFromPlayer(player);
                } else if (action == ACTION_REMOVE_KEY) {
                    vehicle.removeKeyToPlayer(player);
                } else if (action == ACTION_TOGGLE_TRUNK_LOCK) {
                    boolean newTrunkLock = !vehicle.isTrunkLocked();
                    vehicle.setTrunkLocked(newTrunkLock);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        newTrunkLock ? "§c[🔒] Багажник заперт!" : "§a[🔓] Багажник отперт!"), true);
                    vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        newTrunkLock ? com.evgen.cardecor.init.ModSounds.CAR_LOCK_PIN_DOWN.get() : com.evgen.cardecor.init.ModSounds.CAR_UNLOCK_PIN_UP.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.0F);
                } else if (action == ACTION_UNINSTALL_WHEEL) {
                    if (seat >= 0 && seat < 4 && vehicle.isWheelInstalled(seat)) {
                        vehicle.uninstallWheel(seat, player);
                    }
                } else if (action == ACTION_REMOVE_SPARE_WHEEL) {
                    if (vehicle.hasSpareWheel()) {
                        vehicle.removeSpareWheel(player);
                    }
                } else if (action == ACTION_TOGGLE_CABIN_LIGHT) {
                    vehicle.toggleCabinLight();
                    boolean on = vehicle.isCabinLightOn();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        on ? "§e[💡] Свет в салоне: ВКЛ" : "§7[💡] Свет в салоне: ВЫКЛ"), true);
                } else if (action == ACTION_TOGGLE_PARKING_BRAKE) {
                    if (vehicle.hasPassenger(player) && vehicle.getSeatForPlayer(player) == 0) {
                        boolean newState = !vehicle.isParkingBrakeOn();
                        vehicle.setParkingBrake(newState);
                        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                            net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.PLAYERS, 0.85F, newState ? 0.9F : 1.2F);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            newState ? "§e[Ручник] Стояночный тормоз включен" : "§7[Ручник] Стояночный тормоз выключен"), true);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
