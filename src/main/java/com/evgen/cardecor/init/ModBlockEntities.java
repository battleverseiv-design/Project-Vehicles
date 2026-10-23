package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.block.EngineCraneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CarDecorMod.MODID);

    public static final RegistryObject<BlockEntityType<EngineCraneBlockEntity>> ENGINE_CRANE =
        BLOCK_ENTITIES.register("engine_crane",
            () -> BlockEntityType.Builder.of(EngineCraneBlockEntity::new, ModBlocks.ENGINE_CRANE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.evgen.cardecor.block.TireChangerBlockEntity>> TIRE_CHANGER_STATION =
        BLOCK_ENTITIES.register("tire_changer_station",
            () -> BlockEntityType.Builder.of(com.evgen.cardecor.block.TireChangerBlockEntity::new, ModBlocks.TIRE_CHANGER_STATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.evgen.cardecor.block.EngineWorkbenchBlockEntity>> ENGINE_WORKBENCH =
        BLOCK_ENTITIES.register("engine_workbench",
            () -> BlockEntityType.Builder.of(com.evgen.cardecor.block.EngineWorkbenchBlockEntity::new, ModBlocks.ENGINE_WORKBENCH.get()).build(null));
}
