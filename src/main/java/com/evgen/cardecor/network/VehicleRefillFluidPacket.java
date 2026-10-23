package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleRefillFluidPacket {

    public static final int TYPE_OIL = 1;
    public static final int TYPE_COOLANT = 2;

    private final int vehicleId;
    private final int fluidType;

    public VehicleRefillFluidPacket(int vehicleId, int fluidType) {
        this.vehicleId = vehicleId;
        this.fluidType = fluidType;
    }

    public VehicleRefillFluidPacket(FriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.fluidType = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.vehicleId);
        buf.writeInt(this.fluidType);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.level().getEntity(this.vehicleId) instanceof DrivableVehicleEntity vehicle) {
                if (player.distanceToSqr(vehicle) > 36.0) return;

                if (this.fluidType == TYPE_OIL) {
                    // Check if player has oil
                    int oilSlot = -1;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (!st.isEmpty() && st.is(ModItems.MOTOR_OIL.get())) {
                            oilSlot = i;
                            break;
                        }
                    }

                    if (player.getAbilities().instabuild || oilSlot >= 0) {
                        if (!player.getAbilities().instabuild && oilSlot >= 0) {
                            player.getInventory().removeItem(oilSlot, 1);
                        }
                        vehicle.setEngineOil(100.0F);
                        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                            SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.9F);
                        player.displayClientMessage(Component.literal("§a[Механика] §fМоторное масло успешно заменено (100%)!"), true);
                    } else {
                        player.displayClientMessage(Component.literal("§c[Механика] §fТребуется канистра моторного масла!"), true);
                    }
                } else if (this.fluidType == TYPE_COOLANT) {
                    // Check if player has coolant or water bucket
                    int coolantSlot = -1;
                    boolean isBucket = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack st = player.getInventory().getItem(i);
                        if (!st.isEmpty()) {
                            if (st.is(ModItems.COOLANT.get())) {
                                coolantSlot = i;
                                isBucket = false;
                                break;
                            } else if (st.is(Items.WATER_BUCKET)) {
                                coolantSlot = i;
                                isBucket = true;
                                break;
                            }
                        }
                    }

                    if (player.getAbilities().instabuild || coolantSlot >= 0) {
                        if (!player.getAbilities().instabuild && coolantSlot >= 0) {
                            if (isBucket) {
                                player.getInventory().setItem(coolantSlot, new ItemStack(Items.BUCKET));
                            } else {
                                player.getInventory().removeItem(coolantSlot, 1);
                            }
                        }
                        vehicle.setCoolantLevel(100.0F);
                        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                            SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                        player.displayClientMessage(Component.literal("§a[Механика] §fОхлаждающая жидкость залита (100%)!"), true);
                    } else {
                        player.displayClientMessage(Component.literal("§c[Механика] §fТребуется антифриз или ведро воды!"), true);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
