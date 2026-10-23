package com.evgen.cardecor.network;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.item.VehicleEngineItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EngineWorkbenchActionPacket {
    public static final int ACTION_REPAIR = 1;
    public static final int ACTION_BUILD_NEW = 2;

    private final BlockPos pos;
    private final int action;

    public EngineWorkbenchActionPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public EngineWorkbenchActionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.action = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 49.0) return;

            handleAction(player, pos, this.action);
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleAction(ServerPlayer player, BlockPos pos, int action) {
        if (action == ACTION_REPAIR) {
            // Find engine to repair in inventory
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
                player.displayClientMessage(Component.literal("§c[Моторный стенд] §fВ вашем инвентаре нет двигателя для переборки!"), true);
                return;
            }

            boolean hasWrench = player.getAbilities().instabuild;
            int pistonSlot = -1;
            int oilSlot = -1;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                if (st.is(ModItems.WRENCH.get()) || st.getItem() instanceof com.evgen.cardecor.item.WrenchItem) hasWrench = true;
                if (st.is(ModItems.PISTON_SET.get()) && pistonSlot < 0) pistonSlot = i;
                if (st.is(ModItems.MOTOR_OIL.get()) && oilSlot < 0) oilSlot = i;
            }

            if (!hasWrench) {
                player.displayClientMessage(Component.literal("§c[Моторный стенд] §fТребуется Гаечный ключ!"), true);
                return;
            }
            if (!player.getAbilities().instabuild && pistonSlot < 0) {
                player.displayClientMessage(Component.literal("§c[Моторный стенд] §fТребуется Комплект поршней!"), true);
                return;
            }

            if (!player.getAbilities().instabuild) {
                if (pistonSlot >= 0) player.getInventory().removeItem(pistonSlot, 1);
                if (oilSlot >= 0) player.getInventory().removeItem(oilSlot, 1);
            }

            VehicleEngineItem.setCondition(engineStack, 100.0F);
            VehicleEngineItem.setOil(engineStack, 100.0F);
            float curQ = VehicleEngineItem.getQuality(engineStack);
            VehicleEngineItem.setQuality(engineStack, Math.min(100.0F, curQ + 5.0F));

            player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
            player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.2F, 0.9F);

            player.displayClientMessage(Component.literal("§a[Моторный стенд] §fДвигатель успешно перебран! Состояние: 100%, Масло: 100%"), true);

        } else if (action == ACTION_BUILD_NEW) {
            boolean hasWrench = player.getAbilities().instabuild;
            int blockSlot = -1;
            int pistonSlot = -1;
            int sparkSlot = -1;
            int beltSlot = -1;
            int oilSlot = -1;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                if (st.is(ModItems.WRENCH.get()) || st.getItem() instanceof com.evgen.cardecor.item.WrenchItem) hasWrench = true;
                if (st.is(ModItems.ENGINE_BLOCK.get()) && blockSlot < 0) blockSlot = i;
                if (st.is(ModItems.PISTON_SET.get()) && pistonSlot < 0) pistonSlot = i;
                if (st.is(ModItems.SPARK_PLUGS.get()) && sparkSlot < 0) sparkSlot = i;
                if (st.is(ModItems.ALTERNATOR_BELT.get()) && beltSlot < 0) beltSlot = i;
                if (st.is(ModItems.MOTOR_OIL.get()) && oilSlot < 0) oilSlot = i;
            }

            if (!hasWrench) {
                player.displayClientMessage(Component.literal("§c[Моторный стенд] §fТребуется Гаечный ключ!"), true);
                return;
            }

            if (!player.getAbilities().instabuild) {
                if (blockSlot < 0 || pistonSlot < 0 || sparkSlot < 0 || beltSlot < 0 || oilSlot < 0) {
                    player.displayClientMessage(Component.literal("§c[Моторный стенд] §fНужны: Блок цилиндров, Поршни, Свечи, Ремень и Масло!"), true);
                    return;
                }
                player.getInventory().removeItem(blockSlot, 1);
                player.getInventory().removeItem(pistonSlot, 1);
                player.getInventory().removeItem(sparkSlot, 1);
                player.getInventory().removeItem(beltSlot, 1);
                player.getInventory().removeItem(oilSlot, 1);
            }

            float quality = 85.0F + (float) (Math.random() * 15.0F);
            ItemStack newEngine = VehicleEngineItem.create("ford_302_v8", 100.0F, 100.0F, quality);

            if (!player.getInventory().add(newEngine)) {
                player.drop(newEngine, false);
            }

            player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.2F, 0.8F);
            player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 1.2F);

            player.displayClientMessage(Component.literal("§a[Моторный стенд] §fНовый двигатель Ford 302 V8 успешно собран с нуля! (Качество: " + String.format("%.0f", quality) + "%)"), true);
        }
    }
}
