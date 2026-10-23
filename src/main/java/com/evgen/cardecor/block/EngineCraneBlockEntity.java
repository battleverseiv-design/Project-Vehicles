package com.evgen.cardecor.block;

import com.evgen.cardecor.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EngineCraneBlockEntity extends BlockEntity {

    private ItemStack heldEngine = ItemStack.EMPTY;

    public EngineCraneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINE_CRANE.get(), pos, state);
    }

    public boolean hasEngine() {
        return heldEngine != null && !heldEngine.isEmpty();
    }

    public ItemStack getHeldEngine() {
        return heldEngine != null ? heldEngine.copy() : ItemStack.EMPTY;
    }

    public void setHeldEngine(ItemStack stack) {
        this.heldEngine = (stack != null) ? stack.copy() : ItemStack.EMPTY;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void clearEngine() {
        this.heldEngine = ItemStack.EMPTY;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hasEngine()) {
            CompoundTag engineTag = new CompoundTag();
            heldEngine.save(engineTag);
            tag.put("HeldEngine", engineTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("HeldEngine")) {
            this.heldEngine = ItemStack.of(tag.getCompound("HeldEngine"));
        } else {
            this.heldEngine = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
