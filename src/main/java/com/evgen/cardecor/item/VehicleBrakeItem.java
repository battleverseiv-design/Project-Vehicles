package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.BrakeDefinition;
import com.evgen.cardecor.pack.ModularWheelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleBrakeItem extends Item {

    public VehicleBrakeItem(Properties properties) {
        super(properties);
    }

    public static String getBrakeId(ItemStack stack) {
        if (stack != null && stack.hasTag() && stack.getTag().contains("BrakeId")) {
            return stack.getTag().getString("BrakeId");
        }
        return "brake_02_stock_vented";
    }

    public static void setBrakeId(ItemStack stack, String brakeId) {
        if (stack != null) {
            stack.getOrCreateTag().putString("BrakeId", brakeId != null ? brakeId : "brake_02_stock_vented");
        }
    }

    public static ItemStack create(String brakeId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_BRAKE.get());
        setBrakeId(stack, brakeId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String brakeId = getBrakeId(stack);
        BrakeDefinition def = ModularWheelRegistry.getBrake(brakeId);
        if (def != null && def.name != null && !def.name.isEmpty()) {
            return Component.literal(def.name);
        }
        return Component.literal("Тормозной механизм");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String brakeId = getBrakeId(stack);
        BrakeDefinition def = ModularWheelRegistry.getBrake(brakeId);
        if (def != null) {
            tooltip.add(Component.literal("§7Модель: §f" + def.name).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§7Сила торможения: §a" + (int)(def.brakeForceMult * 100) + "%").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§7Стойкость к перегреву: §e" + (int)(def.fadeResistance * 100) + "%").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8Тип: " + (def.isDrum ? "Барабанный" : "Дисковый с суппортом") + " | Масса: " + def.massKg + " кг").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("§eПКМ по пустой ступице колеса: §fустановить тормоз.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eShift + Гаечный ключ по ступице: §fснять тормоз.").withStyle(ChatFormatting.YELLOW));
    }
}
