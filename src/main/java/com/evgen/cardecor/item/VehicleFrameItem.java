package com.evgen.cardecor.item;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleFrameItem extends Item {

    public VehicleFrameItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            DrivablePackVehicleEntity vehicle = new DrivablePackVehicleEntity(ModEntities.DRIVABLE_PACK_VEHICLE.get(), level);
            vehicle.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            vehicle.setVehicleId("vintage_pickup");
            vehicle.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F);

            // Frame only: no wheels, no engine, 0 fuel
            for (int i = 0; i < 4; i++) {
                vehicle.setWheelInstalled(i, false);
            }
            vehicle.setInstalledEngine("none");
            vehicle.setEngineCondition(0.0F);
            vehicle.setEngineOil(0.0F);
            vehicle.setFuel(0.0F);
            vehicle.setBatteryCharge(0.0F);
            vehicle.setVehicleHealth(100.0F);

            level.addFreshEntity(vehicle);

            level.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Несущая стальная рама автомобиля (скелет).").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§eПКМ по земле: §fустановить раму на площадку.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§8Основа для сборки автомобиля с нуля.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
