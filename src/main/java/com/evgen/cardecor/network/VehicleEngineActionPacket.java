package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.item.VehicleEngineItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleEngineActionPacket {
    public static final int ACTION_DISMOUNT = 1;
    public static final int ACTION_INSTALL = 2;

    private final int vehicleId;
    private final int action;

    public VehicleEngineActionPacket(int vehicleId, int action) {
        this.vehicleId = vehicleId;
        this.action = action;
    }

    public VehicleEngineActionPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.action = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.vehicleId);
        buf.writeInt(this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(this.vehicleId);
            if (!(entity instanceof DrivableVehicleEntity vehicle)) return;
            if (player.distanceToSqr(vehicle) > 64.0) return;

            handleAction(player, vehicle, this.action);
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleAction(ServerPlayer player, DrivableVehicleEntity vehicle, int action) {
        if (action == ACTION_DISMOUNT) {
            if (!vehicle.hasEngine()) {
                player.displayClientMessage(Component.literal("§c[!] В автомобиле нет установленного двигателя!"), true);
                return;
            }
            if (vehicle.isEngineOn()) {
                player.displayClientMessage(Component.literal("§c[!] Сначала заглушите двигатель!"), true);
                return;
            }

            boolean hasWrench = player.getAbilities().instabuild;
            if (!hasWrench) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack st = player.getInventory().getItem(i);
                    if (!st.isEmpty() && (st.is(ModItems.WRENCH.get()) || st.getItem() instanceof com.evgen.cardecor.item.WrenchItem)) {
                        hasWrench = true;
                        break;
                    }
                }
            }

            if (!hasWrench) {
                player.displayClientMessage(Component.literal("§c[!] Для демонтажа двигателя необходим Гаечный ключ!"), true);
                return;
            }

            String engId = vehicle.getInstalledEngine();
            float cond = vehicle.getEngineCondition();
            float oil = vehicle.getEngineOil();
            float quality = vehicle.getEngineQuality();

            ItemStack engineStack = VehicleEngineItem.create(engId, cond, oil, quality);

            vehicle.setInstalledEngine("none");
            vehicle.setEngineCondition(0.0F);
            vehicle.setEngineOil(0.0F);

            if (!player.getInventory().add(engineStack)) {
                player.drop(engineStack, false);
            }

            vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.2F);
            vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 0.9F);

            player.displayClientMessage(Component.literal("§e[Двигатель] §fДвигатель успешно демонтирован из автомобиля!"), true);

        } else if (action == ACTION_INSTALL) {
            if (vehicle.hasEngine()) {
                player.displayClientMessage(Component.literal("§c[!] В автомобиле уже установлен двигатель!"), true);
                return;
            }

            int engineSlot = -1;
            ItemStack engineStack = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (!st.isEmpty() && st.getItem() instanceof VehicleEngineItem) {
                    engineSlot = i;
                    engineStack = st;
                    break;
                }
            }

            if (engineSlot < 0 || engineStack.isEmpty()) {
                player.displayClientMessage(Component.literal("§c[!] У вас нет двигателя в инвентаре для установки!"), true);
                return;
            }

            String engId = VehicleEngineItem.getEngineId(engineStack);
            float cond = VehicleEngineItem.getCondition(engineStack);
            float oil = VehicleEngineItem.getOil(engineStack);
            float quality = VehicleEngineItem.getQuality(engineStack);

            vehicle.setInstalledEngine(engId);
            vehicle.setEngineCondition(cond);
            vehicle.setEngineOil(oil);
            vehicle.setEngineQuality(quality);

            if (!player.getAbilities().instabuild) {
                engineStack.shrink(1);
            }

            vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 0.8F);

            player.displayClientMessage(Component.literal("§a[Двигатель] §fДвигатель смонтирован в подкапотное пространство!"), true);
        }
    }
}
