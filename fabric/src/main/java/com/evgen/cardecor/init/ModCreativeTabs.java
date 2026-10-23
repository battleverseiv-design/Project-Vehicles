package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.item.VehiclePackKeyItem;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ModCreativeTabs {
    private static CreativeModeTab CAR_DECOR_TAB_INSTANCE;
    public static final Supplier<CreativeModeTab> CAR_DECOR_TAB = () -> CAR_DECOR_TAB_INSTANCE;

    public static void register() {
        CAR_DECOR_TAB_INSTANCE = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(CarDecorMod.MODID, "cardecor_tab"),
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.UNIVERSAL_VEHICLE_KEY.get()))
                .title(Component.translatable("itemGroup.cardecor_tab"))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.WRENCH.get());
                    output.accept(ModItems.ENGINE_WORKBENCH.get());
                    output.accept(ModItems.TIRE_CHANGER_STATION.get());
                    output.accept(ModItems.VEHICLE_FRAME.get());
                    output.accept(ModItems.VEHICLE_ENGINE.get());
                    output.accept(ModItems.CAR_BATTERY.get());
                    output.accept(ModItems.VEHICLE_WHEEL.get());

                    // Modular Rims
                    for (com.evgen.cardecor.pack.RimDefinition r : com.evgen.cardecor.pack.ModularWheelRegistry.getAllRims()) {
                        output.accept(com.evgen.cardecor.item.VehicleRimItem.create(r.id));
                    }
                    // Modular Tires
                    for (com.evgen.cardecor.pack.TireDefinition t : com.evgen.cardecor.pack.ModularWheelRegistry.getAllTires()) {
                        output.accept(com.evgen.cardecor.item.VehicleTireItem.create(t.id));
                    }
                    // Modular Brakes
                    for (com.evgen.cardecor.pack.BrakeDefinition b : com.evgen.cardecor.pack.ModularWheelRegistry.getAllBrakes()) {
                        output.accept(com.evgen.cardecor.item.VehicleBrakeItem.create(b.id));
                    }

                    output.accept(ModItems.VEHICLE_SEAT.get());
                    output.accept(ModItems.MOTOR_OIL.get());
                    output.accept(ModItems.COOLANT.get());
                    output.accept(ModItems.ENGINE_BLOCK.get());
                    output.accept(ModItems.PISTON_SET.get());
                    output.accept(ModItems.SPARK_PLUGS.get());
                    output.accept(ModItems.ALTERNATOR_BELT.get());
                    output.accept(ModItems.RADIATOR_ITEM.get());
                    output.accept(ModItems.EXHAUST_MUFFLER.get());
                    output.accept(ModItems.VEHICLE_WIRING.get());
                    output.accept(ModItems.ENGINE_HOIST.get());
                    output.accept(ModItems.ENGINE_CRANE.get());

                    for (VehicleDefinition def : VehiclePackManager.getAll()) {
                        output.accept(VehiclePackKeyItem.createKeyFor(def.id));
                    }
                })
                .build()
        );
    }
}
