package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.item.CarBatteryItem;
import com.evgen.cardecor.item.VehicleEngineItem;
import com.evgen.cardecor.item.VehicleFrameItem;
import com.evgen.cardecor.item.VehiclePackKeyItem;
import com.evgen.cardecor.item.VehicleSeatItem;
import com.evgen.cardecor.item.VehicleWheelItem;
import com.evgen.cardecor.item.WrenchItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {
    private static Item UNIVERSAL_VEHICLE_KEY_ITEM;
    private static Item WRENCH_ITEM;
    private static Item VEHICLE_WHEEL_ITEM;
    private static Item VEHICLE_RIM_ITEM;
    private static Item VEHICLE_TIRE_ITEM;
    private static Item VEHICLE_BRAKE_ITEM;
    private static Item VEHICLE_SEAT_ITEM;
    private static Item MOTOR_OIL_ITEM;
    private static Item COOLANT_ITEM;
    private static Item VEHICLE_ENGINE_ITEM;
    private static Item CAR_BATTERY_ITEM;
    private static Item ENGINE_BLOCK_ITEM;
    private static Item PISTON_SET_ITEM;
    private static Item SPARK_PLUGS_ITEM;
    private static Item ALTERNATOR_BELT_ITEM;
    private static Item RADIATOR_ITEM_ITEM;
    private static Item EXHAUST_MUFFLER_ITEM;
    private static Item VEHICLE_FRAME_ITEM;
    private static Item VEHICLE_WIRING_ITEM;
    private static Item ENGINE_HOIST_ITEM;
    private static Item ENGINE_CRANE_ITEM;
    private static Item ENGINE_WORKBENCH_ITEM;
    private static Item TIRE_CHANGER_STATION_ITEM;

    public static final Supplier<Item> UNIVERSAL_VEHICLE_KEY = () -> UNIVERSAL_VEHICLE_KEY_ITEM;
    public static final Supplier<Item> WRENCH = () -> WRENCH_ITEM;
    public static final Supplier<Item> VEHICLE_WHEEL = () -> VEHICLE_WHEEL_ITEM;
    public static final Supplier<Item> VEHICLE_RIM = () -> VEHICLE_RIM_ITEM;
    public static final Supplier<Item> VEHICLE_TIRE = () -> VEHICLE_TIRE_ITEM;
    public static final Supplier<Item> VEHICLE_BRAKE = () -> VEHICLE_BRAKE_ITEM;
    public static final Supplier<Item> VEHICLE_SEAT = () -> VEHICLE_SEAT_ITEM;
    public static final Supplier<Item> MOTOR_OIL = () -> MOTOR_OIL_ITEM;
    public static final Supplier<Item> COOLANT = () -> COOLANT_ITEM;
    public static final Supplier<Item> VEHICLE_ENGINE = () -> VEHICLE_ENGINE_ITEM;
    public static final Supplier<Item> CAR_BATTERY = () -> CAR_BATTERY_ITEM;
    public static final Supplier<Item> ENGINE_BLOCK = () -> ENGINE_BLOCK_ITEM;
    public static final Supplier<Item> PISTON_SET = () -> PISTON_SET_ITEM;
    public static final Supplier<Item> SPARK_PLUGS = () -> SPARK_PLUGS_ITEM;
    public static final Supplier<Item> ALTERNATOR_BELT = () -> ALTERNATOR_BELT_ITEM;
    public static final Supplier<Item> RADIATOR_ITEM = () -> RADIATOR_ITEM_ITEM;
    public static final Supplier<Item> EXHAUST_MUFFLER = () -> EXHAUST_MUFFLER_ITEM;
    public static final Supplier<Item> VEHICLE_FRAME = () -> VEHICLE_FRAME_ITEM;
    public static final Supplier<Item> VEHICLE_WIRING = () -> VEHICLE_WIRING_ITEM;
    public static final Supplier<Item> ENGINE_HOIST = () -> ENGINE_HOIST_ITEM;
    public static final Supplier<Item> ENGINE_CRANE = () -> ENGINE_CRANE_ITEM;
    public static final Supplier<Item> ENGINE_WORKBENCH = () -> ENGINE_WORKBENCH_ITEM;
    public static final Supplier<Item> TIRE_CHANGER_STATION = () -> TIRE_CHANGER_STATION_ITEM;

    public static void register() {
        UNIVERSAL_VEHICLE_KEY_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "generic_vehicle_key"),
            new VehiclePackKeyItem(new Item.Properties().stacksTo(1))
        );

        WRENCH_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "wrench"),
            new WrenchItem(new Item.Properties().stacksTo(1))
        );

        VEHICLE_WHEEL_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_wheel"),
            new VehicleWheelItem(new Item.Properties().stacksTo(16))
        );

        VEHICLE_RIM_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_rim"),
            new com.evgen.cardecor.item.VehicleRimItem(new Item.Properties().stacksTo(16))
        );

        VEHICLE_TIRE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_tire"),
            new com.evgen.cardecor.item.VehicleTireItem(new Item.Properties().stacksTo(16))
        );

        VEHICLE_BRAKE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_brake"),
            new com.evgen.cardecor.item.VehicleBrakeItem(new Item.Properties().stacksTo(16))
        );

        VEHICLE_SEAT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_seat"),
            new VehicleSeatItem(new Item.Properties().stacksTo(16))
        );

        MOTOR_OIL_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "motor_oil"),
            new Item(new Item.Properties().stacksTo(16))
        );

        COOLANT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "coolant"),
            new Item(new Item.Properties().stacksTo(16))
        );

        VEHICLE_ENGINE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_engine"),
            new VehicleEngineItem(new Item.Properties().stacksTo(1))
        );

        CAR_BATTERY_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "car_battery"),
            new CarBatteryItem(new Item.Properties().stacksTo(1))
        );

        ENGINE_BLOCK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "engine_block"),
            new Item(new Item.Properties().stacksTo(16))
        );

        PISTON_SET_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "piston_set"),
            new Item(new Item.Properties().stacksTo(16))
        );

        SPARK_PLUGS_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "spark_plugs"),
            new Item(new Item.Properties().stacksTo(16))
        );

        ALTERNATOR_BELT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "alternator_belt"),
            new Item(new Item.Properties().stacksTo(16))
        );

        RADIATOR_ITEM_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "radiator_item"),
            new Item(new Item.Properties().stacksTo(8))
        );

        EXHAUST_MUFFLER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "exhaust_muffler"),
            new Item(new Item.Properties().stacksTo(8))
        );

        VEHICLE_FRAME_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_frame"),
            new VehicleFrameItem(new Item.Properties().stacksTo(1))
        );

        VEHICLE_WIRING_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "vehicle_wiring"),
            new Item(new Item.Properties().stacksTo(16))
        );

        ENGINE_HOIST_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "engine_hoist"),
            new BlockItem(ModBlocks.ENGINE_CRANE.get(), new Item.Properties().stacksTo(1))
        );

        ENGINE_CRANE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "engine_crane"),
            new BlockItem(ModBlocks.ENGINE_CRANE.get(), new Item.Properties().stacksTo(1))
        );

        ENGINE_WORKBENCH_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "engine_workbench"),
            new BlockItem(ModBlocks.ENGINE_WORKBENCH.get(), new Item.Properties())
        );

        TIRE_CHANGER_STATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            new ResourceLocation(CarDecorMod.MODID, "tire_changer_station"),
            new BlockItem(ModBlocks.TIRE_CHANGER_STATION.get(), new Item.Properties().stacksTo(1))
        );
    }
}
