package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.ModularWheelRegistry;
import com.evgen.cardecor.pack.TireDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleTireItem extends Item {

    public VehicleTireItem(Properties properties) {
        super(properties);
    }

    public static String getTireId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("TireId")) {
            return stack.getTag().getString("TireId");
        }
        return "tire_03_classic_radial";
    }

    public static void setTireId(ItemStack stack, String tireId) {
        if (stack != null) {
            stack.getOrCreateTag().putString("TireId", tireId != null ? tireId : "tire_03_classic_radial");
        }
    }

    public static ItemStack create(String tireId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_TIRE.get());
        setTireId(stack, tireId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String tireId = getTireId(stack);
        TireDefinition def = ModularWheelRegistry.getTire(tireId);
        if (def != null && def.name != null && !def.name.isEmpty()) {
            return Component.literal("Покрышка: " + def.name);
        }
        return Component.literal("Автомобильная покрышка");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String tireId = getTireId(stack);
        TireDefinition def = ModularWheelRegistry.getTire(tireId);
        if (def != null) {
            tooltip.add(Component.literal("§7Модель: §f" + def.name).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8Радиус: §7" + String.format(java.util.Locale.US, "%.2f", def.radius) + "м | Ширина: §7" + String.format(java.util.Locale.US, "%.2f", def.width) + "м | Вес: " + def.massKg + " кг").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("§7Сцепление (Асфальт): §a" + (int)(def.gripAsphalt * 100) + "%"));
            tooltip.add(Component.literal("§7Сцепление (Грунт/Бездорожье): §a" + (int)(def.gripDirt * 100) + "%"));
            tooltip.add(Component.literal("§7Сцепление (Лед/Снег): §b" + (int)(def.gripIce * 100) + "%"));
        }
        tooltip.add(Component.literal("§6Шиномонтажный станок: §fположите на стол станка и наденьте диск.").withStyle(ChatFormatting.GOLD));
    }
}
