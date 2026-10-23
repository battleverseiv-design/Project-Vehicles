package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CarBatteryItem extends Item {

    public CarBatteryItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(float charge) {
        ItemStack stack = new ItemStack(ModItems.CAR_BATTERY.get());
        setCharge(stack, charge);
        return stack;
    }

    public static float getCharge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 100.0F;
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("Charge")) ? tag.getFloat("Charge") : 100.0F;
    }

    public static void setCharge(ItemStack stack, float charge) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateTag().putFloat("Charge", Math.max(0.0F, Math.min(100.0F, charge)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float charge = getCharge(stack);
        String col = charge >= 60.0F ? "§a" : (charge >= 20.0F ? "§e" : "§c");
        tooltip.add(Component.literal("§7Заряд: " + col + String.format("%.0f%%", charge)));
        tooltip.add(Component.literal("§8Свинцово-кислотный аккумулятор 12V.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§8Устанавливается под капот автомобиля.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
