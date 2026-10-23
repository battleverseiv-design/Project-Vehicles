package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.block.EngineCraneBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModBlockEntities {
    private static BlockEntityType<EngineCraneBlockEntity> ENGINE_CRANE_BE;
    public static final Supplier<BlockEntityType<EngineCraneBlockEntity>> ENGINE_CRANE = () -> ENGINE_CRANE_BE;

    private static BlockEntityType<com.evgen.cardecor.block.TireChangerBlockEntity> TIRE_CHANGER_STATION_BE;
    public static final Supplier<BlockEntityType<com.evgen.cardecor.block.TireChangerBlockEntity>> TIRE_CHANGER_STATION = () -> TIRE_CHANGER_STATION_BE;

    private static BlockEntityType<com.evgen.cardecor.block.EngineWorkbenchBlockEntity> ENGINE_WORKBENCH_BE;
    public static final Supplier<BlockEntityType<com.evgen.cardecor.block.EngineWorkbenchBlockEntity>> ENGINE_WORKBENCH = () -> ENGINE_WORKBENCH_BE;

    public static void register() {
        ENGINE_CRANE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            new ResourceLocation(CarDecorMod.MODID, "engine_crane"),
            BlockEntityType.Builder.of(EngineCraneBlockEntity::new, ModBlocks.ENGINE_CRANE.get()).build(null)
        );

        TIRE_CHANGER_STATION_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            new ResourceLocation(CarDecorMod.MODID, "tire_changer_station"),
            BlockEntityType.Builder.of(com.evgen.cardecor.block.TireChangerBlockEntity::new, ModBlocks.TIRE_CHANGER_STATION.get()).build(null)
        );

        ENGINE_WORKBENCH_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            new ResourceLocation(CarDecorMod.MODID, "engine_workbench"),
            BlockEntityType.Builder.of(com.evgen.cardecor.block.EngineWorkbenchBlockEntity::new, ModBlocks.ENGINE_WORKBENCH.get()).build(null)
        );
    }
}
