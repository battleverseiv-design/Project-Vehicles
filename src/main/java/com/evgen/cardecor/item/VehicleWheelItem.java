package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.ModularWheelRegistry;
import com.evgen.cardecor.pack.RimDefinition;
import com.evgen.cardecor.pack.TireDefinition;
import com.evgen.cardecor.pack.WheelDefinition;
import com.evgen.cardecor.pack.WheelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleWheelItem extends Item {

    public VehicleWheelItem(Properties properties) {
        super(properties);
    }

    public static String getTireId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("TireId")) {
            return stack.getTag().getString("TireId");
        }
        if (stack != null && stack.hasTag() && stack.getTag().contains("WheelId")) {
            String legacy = stack.getTag().getString("WheelId");
            if ("offroad_heavy_wheel".equalsIgnoreCase(legacy)) return "tire_01_offroad_mud";
            if ("sport_alloy_wheel".equalsIgnoreCase(legacy)) return "tire_05_low_profile";
            return "tire_03_classic_radial";
        }
        return "tire_03_classic_radial";
    }

    public static String getRimId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("RimId")) {
            return stack.getTag().getString("RimId");
        }
        if (stack != null && stack.hasTag() && stack.getTag().contains("WheelId")) {
            String legacy = stack.getTag().getString("WheelId");
            if ("offroad_heavy_wheel".equalsIgnoreCase(legacy)) return "rim_01_offroad_beadlock";
            if ("sport_alloy_wheel".equalsIgnoreCase(legacy)) return "rim_07_tuner_bbs_mesh";
            return "rim_04_stamped_steelie";
        }
        return "rim_04_stamped_steelie";
    }

    public static void setComponents(ItemStack stack, String tireId, String rimId) {
        if (stack != null) {
            stack.getOrCreateTag().putString("TireId", tireId != null ? tireId : "tire_03_classic_radial");
            stack.getOrCreateTag().putString("RimId", rimId != null ? rimId : "rim_04_stamped_steelie");
            stack.getOrCreateTag().putString("WheelId", (tireId != null ? tireId : "tire_03_classic_radial") + ";" + (rimId != null ? rimId : "rim_04_stamped_steelie"));
        }
    }

    public static String getWheelId(ItemStack stack) {
        String t = getTireId(stack);
        String r = getRimId(stack);
        return t + ";" + r;
    }

    public static void setWheelId(ItemStack stack, String wheelId) {
        if (wheelId != null && wheelId.contains(";")) {
            String[] parts = wheelId.split(";");
            setComponents(stack, parts[0], parts.length > 1 ? parts[1] : "rim_04_stamped_steelie");
        } else if ("offroad_heavy_wheel".equalsIgnoreCase(wheelId)) {
            setComponents(stack, "tire_01_offroad_mud", "rim_01_offroad_beadlock");
        } else if ("sport_alloy_wheel".equalsIgnoreCase(wheelId)) {
            setComponents(stack, "tire_05_low_profile", "rim_07_tuner_bbs_mesh");
        } else {
            setComponents(stack, "tire_03_classic_radial", "rim_04_stamped_steelie");
        }
    }

    public static ItemStack createWheel(String tireId, String rimId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_WHEEL.get());
        setComponents(stack, tireId, rimId);
        return stack;
    }

    public static ItemStack createWheel(String wheelId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_WHEEL.get());
        setWheelId(stack, wheelId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String tireId = getTireId(stack);
        String rimId = getRimId(stack);
        TireDefinition tDef = ModularWheelRegistry.getTire(tireId);
        RimDefinition rDef = ModularWheelRegistry.getRim(rimId);
        String tName = (tDef != null) ? tDef.name : "Резина";
        String rName = (rDef != null) ? rDef.name : "Диск";
        return Component.literal("Колесо [" + tName + " + " + rName + "]");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String tireId = getTireId(stack);
        String rimId = getRimId(stack);
        TireDefinition tDef = ModularWheelRegistry.getTire(tireId);
        RimDefinition rDef = ModularWheelRegistry.getRim(rimId);

        if (tDef != null) {
            tooltip.add(Component.literal("§7Покрышка: §f" + tDef.name).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8Радиус: §7" + String.format(java.util.Locale.US, "%.2f", tDef.radius) + "м | Ширина: §7" + String.format(java.util.Locale.US, "%.2f", tDef.width) + "м").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("§7Сцепление (Асфальт): §a" + (int)(tDef.gripAsphalt * 100) + "%"));
            tooltip.add(Component.literal("§7Сцепление (Грунт): §a" + (int)(tDef.gripDirt * 100) + "%"));
            tooltip.add(Component.literal("§7Сцепление (Лед): §b" + (int)(tDef.gripIce * 100) + "%"));
        }
        if (rDef != null) {
            tooltip.add(Component.literal("§7Диск: §e" + rDef.name).withStyle(ChatFormatting.GRAY));
        }

        double totalMass = (tDef != null ? tDef.massKg : 10.0) + (rDef != null ? rDef.massKg : 8.0);
        tooltip.add(Component.literal("§8Общая масса в сборе: §7" + totalMass + " кг").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§eПКМ по пустой ступице: §fустановить колесо.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eГаечным ключом по колесу: §fоткрутить и снять.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§6Шиномонтажный станок: §fразбортировка на станок.").withStyle(ChatFormatting.GOLD));
    }
}
