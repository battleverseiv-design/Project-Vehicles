package com.evgen.cardecor.item;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.entity.VehiclePartEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    public static InteractionResult dismantleVehicle(DrivableVehicleEntity vehicle, Player player) {
        Level level = vehicle.level();
        if (!level.isClientSide) {
            vehicle.ejectPassengers();
            vehicle.dropTrunkContents();

            ItemStack key = VehiclePackKeyItem.createKeyFor(vehicle.getVehicleId());
            if (!player.getInventory().add(key)) {
                vehicle.spawnAtLocation(key);
            }

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WAX_OFF, vehicle.getX(), vehicle.getY() + 0.5, vehicle.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            }

            level.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                com.evgen.cardecor.init.ModSounds.GEAR_SHIFT.get(), SoundSource.PLAYERS, 1.2F, 0.9F);

            vehicle.discard();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Специальный инструмент автомеханика.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§eПКМ по колесу: §fоткрутить и снять деталь.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eПКМ по кузову: §fрихтовка вмятин и починка автомобиля.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§eShift + ПКМ по кузову: §fразобрать автомобиль в ключ.").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§8Модульная система обслуживания транспорта.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
