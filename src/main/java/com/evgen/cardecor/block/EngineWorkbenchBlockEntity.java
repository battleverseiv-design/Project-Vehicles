package com.evgen.cardecor.block;

import com.evgen.cardecor.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EngineWorkbenchBlockEntity extends BlockEntity {

    public EngineWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINE_WORKBENCH.get(), pos, state);
    }
}
