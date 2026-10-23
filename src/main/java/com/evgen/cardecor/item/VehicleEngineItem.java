package com.evgen.cardecor.item;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.pack.EngineDefinition;
import com.evgen.cardecor.pack.EngineRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleEngineItem extends Item {

    public VehicleEngineItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String engineId, float condition, float oil, float quality) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_ENGINE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("EngineId", engineId != null ? engineId : "ford_302_v8");
        tag.putFloat("Condition", Math.max(0.0F, Math.min(100.0F, condition)));
        tag.putFloat("Oil", Math.max(0.0F, Math.min(100.0F, oil)));
        tag.putFloat("Quality", Math.max(0.0F, Math.min(100.0F, quality)));
        return stack;
    }

    public static String getEngineId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "default_engine";
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("EngineId")) {
            return tag.getString("EngineId");
        }
        return "ford_302_v8";
    }

    public static float getCondition(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 100.0F;
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("Condition")) ? tag.getFloat("Condition") : 100.0F;
    }

    public static float getOil(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 100.0F;
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("Oil")) ? tag.getFloat("Oil") : 100.0F;
    }

    public static float getQuality(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 80.0F;
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("Quality")) ? tag.getFloat("Quality") : 80.0F;
    }

    public static void setCondition(ItemStack stack, float condition) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateTag().putFloat("Condition", Math.max(0.0F, Math.min(100.0F, condition)));
    }

    public static void setOil(ItemStack stack, float oil) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateTag().putFloat("Oil", Math.max(0.0F, Math.min(100.0F, oil)));
    }

    public static void setQuality(ItemStack stack, float quality) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateTag().putFloat("Quality", Math.max(0.0F, Math.min(100.0F, quality)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String engineId = getEngineId(stack);
        float cond = getCondition(stack);
        float oil = getOil(stack);
        float quality = getQuality(stack);

        EngineDefinition def = EngineRegistry.get(engineId);
        String engName = (def != null && def.name != null) ? def.name : engineId;

        tooltip.add(Component.literal("§6Модель: §f" + engName).withStyle(ChatFormatting.GOLD));

        if (def != null) {
            String typeStr = def.type != null ? def.type.toUpperCase() : "V8";
            int cyl = (def.block != null) ? def.block.cylinders : 8;
            tooltip.add(Component.literal("§7Тип: §e" + typeStr + " §7(" + cyl + " цил.)"));
            if (def.physics != null) {
                tooltip.add(Component.literal(String.format("§7Мощность: §b%.0f л.с. §7| Крутящий момент: §b%.0f Нм",
                    def.physics.horsepower, def.physics.torqueNm)));
            }
        }

        String condCol = cond >= 75.0F ? "§a" : (cond >= 30.0F ? "§e" : "§c");
        tooltip.add(Component.literal("§7Ресурс: " + condCol + String.format("%.0f%%", cond)));

        String oilCol = oil >= 50.0F ? "§a" : (oil >= 15.0F ? "§e" : "§c");
        tooltip.add(Component.literal("§7Масло: " + oilCol + String.format("%.0f%%", oil)));

        tooltip.add(Component.literal("§7Качество сборки: §f" + String.format("%.0f%%", quality)));
        tooltip.add(Component.literal("§8Монтируется под капот или в моторный верстак.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
