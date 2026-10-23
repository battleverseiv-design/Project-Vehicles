package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.item.CarBatteryItem;
import com.evgen.cardecor.item.VehicleEngineItem;
import com.evgen.cardecor.item.VehicleFrameItem;
import com.evgen.cardecor.item.VehiclePackKeyItem;
import com.evgen.cardecor.item.VehicleSeatItem;
import com.evgen.cardecor.item.VehicleWheelItem;
import com.evgen.cardecor.item.WrenchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, CarDecorMod.MODID);

    public static final RegistryObject<Item> UNIVERSAL_VEHICLE_KEY = ITEMS.register("generic_vehicle_key",
        () -> new VehiclePackKeyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
        () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> VEHICLE_WHEEL = ITEMS.register("vehicle_wheel",
        () -> new VehicleWheelItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> VEHICLE_RIM = ITEMS.register("vehicle_rim",
        () -> new com.evgen.cardecor.item.VehicleRimItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> VEHICLE_TIRE = ITEMS.register("vehicle_tire",
        () -> new com.evgen.cardecor.item.VehicleTireItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> VEHICLE_BRAKE = ITEMS.register("vehicle_brake",
        () -> new com.evgen.cardecor.item.VehicleBrakeItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> VEHICLE_SEAT = ITEMS.register("vehicle_seat",
        () -> new VehicleSeatItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MOTOR_OIL = ITEMS.register("motor_oil",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> COOLANT = ITEMS.register("coolant",
        () -> new Item(new Item.Properties().stacksTo(16)));

    // Modular Vehicle & Engine Items
    public static final RegistryObject<Item> VEHICLE_ENGINE = ITEMS.register("vehicle_engine",
        () -> new VehicleEngineItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CAR_BATTERY = ITEMS.register("car_battery",
        () -> new CarBatteryItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENGINE_BLOCK = ITEMS.register("engine_block",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> PISTON_SET = ITEMS.register("piston_set",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SPARK_PLUGS = ITEMS.register("spark_plugs",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ALTERNATOR_BELT = ITEMS.register("alternator_belt",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> RADIATOR_ITEM = ITEMS.register("radiator_item",
        () -> new Item(new Item.Properties().stacksTo(8)));

    public static final RegistryObject<Item> EXHAUST_MUFFLER = ITEMS.register("exhaust_muffler",
        () -> new Item(new Item.Properties().stacksTo(8)));

    public static final RegistryObject<Item> VEHICLE_FRAME = ITEMS.register("vehicle_frame",
        () -> new VehicleFrameItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> VEHICLE_WIRING = ITEMS.register("vehicle_wiring",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ENGINE_HOIST = ITEMS.register("engine_hoist",
        () -> new BlockItem(ModBlocks.ENGINE_CRANE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENGINE_CRANE = ITEMS.register("engine_crane",
        () -> new BlockItem(ModBlocks.ENGINE_CRANE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENGINE_WORKBENCH = ITEMS.register("engine_workbench",
        () -> new BlockItem(ModBlocks.ENGINE_WORKBENCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> TIRE_CHANGER_STATION = ITEMS.register("tire_changer_station",
        () -> new BlockItem(ModBlocks.TIRE_CHANGER_STATION.get(), new Item.Properties().stacksTo(1)));
}
