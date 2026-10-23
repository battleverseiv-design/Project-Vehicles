package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.block.EngineWorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, CarDecorMod.MODID);

    public static final RegistryObject<Block> ENGINE_WORKBENCH = BLOCKS.register("engine_workbench",
        EngineWorkbenchBlock::new);

    public static final RegistryObject<Block> ENGINE_CRANE = BLOCKS.register("engine_crane",
        com.evgen.cardecor.block.EngineCraneBlock::new);

    public static final RegistryObject<Block> TIRE_CHANGER_STATION = BLOCKS.register("tire_changer_station",
        com.evgen.cardecor.block.TireChangerBlock::new);
}
