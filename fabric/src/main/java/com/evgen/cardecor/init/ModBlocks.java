package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.block.EngineWorkbenchBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class ModBlocks {
    private static Block ENGINE_WORKBENCH_BLOCK;
    public static final Supplier<Block> ENGINE_WORKBENCH = () -> ENGINE_WORKBENCH_BLOCK;

    private static Block ENGINE_CRANE_BLOCK;
    public static final Supplier<Block> ENGINE_CRANE = () -> ENGINE_CRANE_BLOCK;

    private static Block TIRE_CHANGER_STATION_BLOCK;
    public static final Supplier<Block> TIRE_CHANGER_STATION = () -> TIRE_CHANGER_STATION_BLOCK;

    public static void register() {
        ENGINE_WORKBENCH_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            new ResourceLocation(CarDecorMod.MODID, "engine_workbench"),
            new EngineWorkbenchBlock()
        );

        ENGINE_CRANE_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            new ResourceLocation(CarDecorMod.MODID, "engine_crane"),
            new com.evgen.cardecor.block.EngineCraneBlock()
        );

        TIRE_CHANGER_STATION_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            new ResourceLocation(CarDecorMod.MODID, "tire_changer_station"),
            new com.evgen.cardecor.block.TireChangerBlock()
        );
    }
}
