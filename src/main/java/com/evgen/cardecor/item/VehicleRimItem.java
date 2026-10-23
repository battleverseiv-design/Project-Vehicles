package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.ModularWheelRegistry;
import com.evgen.cardecor.pack.RimDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleRimItem extends Item {

    public VehicleRimItem(Properties properties) {
        super(properties);
    }

    public static String getRimId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("RimId")) {
            return stack.getTag().getString("RimId");
        }
        return "rim_04_stamped_steelie";
    }

    public static void setRimId(ItemStack stack, String rimId) {
        if (stack != null) {
            stack.getOrCreateTag().putString("RimId", rimId != null ? rimId : "rim_04_stamped_steelie");
        }
    }

    public static ItemStack create(String rimId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_RIM.get());
        setRimId(stack, rimId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String rimId = getRimId(stack);
        RimDefinition def = ModularWheelRegistry.getRim(rimId);
        if (def != null && def.name != null && !def.name.isEmpty()) {
            return Component.literal("Диск: " + def.name);
        }
        return Component.literal("Колесный диск");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String rimId = getRimId(stack);
        RimDefinition def = ModularWheelRegistry.getRim(rimId);
        if (def != null) {
            tooltip.add(Component.literal("§7Модель: §f" + def.name).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8Масса: §7" + def.massKg + " кг").withStyle(ChatFormatting.DARK_GRAY));
            if (Math.abs(def.deepDishOffset) > 0.01) {
                tooltip.add(Component.literal("§8Вылет (ET): §e" + String.format(java.util.Locale.US, "%.2f", def.deepDishOffset) + "м").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.literal("§6Шиномонтажный станок: §fустановите на станок с покрышкой для сборки.").withStyle(ChatFormatting.GOLD));
    }
}
