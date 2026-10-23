package com.evgen.cardecor.item;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.init.ModEntities;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehiclePackKeyItem extends Item {

    public VehiclePackKeyItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createKeyFor(String vehicleId) {
        ItemStack stack = new ItemStack(com.evgen.cardecor.init.ModItems.UNIVERSAL_VEHICLE_KEY.get());
        stack.getOrCreateTag().putString("VehicleId", vehicleId);
        return stack;
    }

    public static ItemStack createKeyForVehicle(com.evgen.cardecor.entity.DrivableVehicleEntity vehicle) {
        ItemStack stack = new ItemStack(com.evgen.cardecor.init.ModItems.UNIVERSAL_VEHICLE_KEY.get());
        stack.getOrCreateTag().putString("VehicleId", vehicle.getVehicleId());
        stack.getOrCreateTag().putString("VehicleUUID", vehicle.getUUID().toString());
        return stack;
    }

    public static boolean matchesVehicle(ItemStack stack, com.evgen.cardecor.entity.DrivableVehicleEntity vehicle) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof VehiclePackKeyItem)) return false;
        if (!stack.hasTag()) return false;
        if (stack.getTag().contains("VehicleUUID")) {
            return vehicle.getUUID().toString().equalsIgnoreCase(stack.getTag().getString("VehicleUUID"));
        }
        String vid = getVehicleId(stack);
        return "universal".equalsIgnoreCase(vid) || vid.equalsIgnoreCase(vehicle.getVehicleId());
    }

    public static String getVehicleId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("VehicleId")) {
            return stack.getTag().getString("VehicleId");
        }
        return "default_vehicle";
    }

    @Override
    public Component getName(ItemStack stack) {
        String vid = getVehicleId(stack);
        VehicleDefinition def = VehiclePackManager.get(vid);
        String name = (def != null && def.name != null) ? def.name : "Автомобиль";
        if (stack.hasTag() && stack.getTag().contains("VehicleUUID")) {
            String uuid = stack.getTag().getString("VehicleUUID");
            String shortCode = uuid.length() >= 8 ? uuid.substring(0, 8).toUpperCase() : uuid;
            return Component.literal("§6Ключ: §e" + name + " §7(#" + shortCode + ")");
        }
        if (def != null) {
            return Component.literal("§6Ключ зажигания: §e" + def.name);
        }
        return Component.literal("§6Универсальный ключ зажигания");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String vid = getVehicleId(stack);
        VehicleDefinition def = VehiclePackManager.get(vid);
        if (stack.hasTag() && stack.getTag().contains("VehicleUUID")) {
            tooltip.add(Component.literal("§7VIN: §8" + stack.getTag().getString("VehicleUUID")));
            tooltip.add(Component.literal("§eПКМ по машине: §fЗапереть / Отпереть двери"));
            tooltip.add(Component.literal("§aЗамок зажигания: §fЗапуск двигателя"));
        } else if (def != null) {
            tooltip.add(Component.literal("§7ID: §8" + def.id));
            tooltip.add(Component.literal(String.format("§aСкорость: §f%.0f / %.0f км/ч", def.physics.maxSpeed, def.physics.boostSpeed)));
            tooltip.add(Component.literal(String.format("§bПередачи: §f%d ст. §8| §eМест: §f%d", def.physics.gears, def.seats.size())));
            tooltip.add(Component.literal("§7Нажмите §aПКМ по земле §7чтобы призвать"));
        } else {
            tooltip.add(Component.literal("§8(Пак: " + vid + ")"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            String vehicleId = getVehicleId(stack);
            VehicleDefinition def = VehiclePackManager.get(vehicleId);

            BlockPos clickedPos = context.getClickedPos();
            net.minecraft.core.Direction clickedFace = context.getClickedFace();
            BlockPos targetPos = clickedPos.relative(clickedFace);

            // Compute exact ground surface contact height
            net.minecraft.world.level.block.state.BlockState clickedState = level.getBlockState(clickedPos);
            net.minecraft.world.phys.shapes.VoxelShape shape = clickedState.getCollisionShape(level, clickedPos);
            double groundY;
            if (clickedFace == net.minecraft.core.Direction.UP) {
                groundY = clickedPos.getY() + (shape.isEmpty() ? 1.0 : shape.max(net.minecraft.core.Direction.Axis.Y));
            } else {
                net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(targetPos.below());
                net.minecraft.world.phys.shapes.VoxelShape belowShape = belowState.getCollisionShape(level, targetPos.below());
                groundY = targetPos.getY() - 1.0 + (belowShape.isEmpty() ? 1.0 : belowShape.max(net.minecraft.core.Direction.Axis.Y));
            }

            double spawnX = targetPos.getX() + 0.5;
            double spawnZ = targetPos.getZ() + 0.5;
            float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;

            DrivablePackVehicleEntity car = new DrivablePackVehicleEntity(ModEntities.DRIVABLE_PACK_VEHICLE.get(), level, vehicleId);
            car.moveTo(spawnX, groundY, spawnZ, yaw, 0.0F);
            car.setOldPosAndRot();
            car.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            car.keySpawnInitialized = true;
            if (car.physics != null) {
                car.physics.targetSurfaceY = groundY;
                car.physics.isGrounded = true;
                car.physics.wasGrounded = true;
                car.physics.isPhysicallySupported = true;
                car.physics.verticalSpeed = 0.0;
            }

            level.addFreshEntity(car);
            if (context.getPlayer() != null) {
                if (!context.getPlayer().isCreative()) {
                    stack.shrink(1);
                    ItemStack boundKey = createKeyForVehicle(car);
                    if (!context.getPlayer().getInventory().add(boundKey)) {
                        context.getPlayer().drop(boundKey, false);
                    }
                }
                context.getPlayer().displayClientMessage(Component.literal("§a[✔] Автомобиль создан!"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
