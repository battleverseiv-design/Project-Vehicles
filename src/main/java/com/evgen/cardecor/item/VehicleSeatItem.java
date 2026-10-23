package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.SeatDefinition;
import com.evgen.cardecor.pack.SeatRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleSeatItem extends Item {

    public VehicleSeatItem(Properties properties) {
        super(properties);
    }

    public static String getSeatId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("SeatId")) {
            return stack.getTag().getString("SeatId");
        }
        return "standard_seat";
    }

    public static void setSeatId(ItemStack stack, String seatId) {
        if (stack != null) {
            stack.getOrCreateTag().putString("SeatId", seatId != null ? seatId : "standard_seat");
        }
    }

    public static ItemStack createSeat(String seatId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_SEAT.get());
        setSeatId(stack, seatId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String seatId = getSeatId(stack);
        SeatDefinition def = SeatRegistry.get(seatId);
        if (def != null && def.name != null && !def.name.isEmpty()) {
            return Component.literal(def.name);
        }
        return Component.literal("Автомобильное сиденье");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String seatId = getSeatId(stack);
        SeatDefinition def = SeatRegistry.get(seatId);
        if (def != null) {
            tooltip.add(Component.literal("§7Модель: §f" + def.name).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8Комфорт: §7" + (int)(def.comfortFactor * 100) + "% | Вес: " + def.massKg + " кг").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("§eПКМ по салону/креслу: §fустановить кресло.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eГаечным ключом по креслу: §fдемонтировать в инвентарь.").withStyle(ChatFormatting.YELLOW));
    }
}
