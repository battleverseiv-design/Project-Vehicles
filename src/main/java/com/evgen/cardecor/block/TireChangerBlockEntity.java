package com.evgen.cardecor.block;

import com.evgen.cardecor.init.ModBlockEntities;
import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.item.VehicleRimItem;
import com.evgen.cardecor.item.VehicleTireItem;
import com.evgen.cardecor.item.VehicleWheelItem;
import com.evgen.cardecor.item.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TireChangerBlockEntity extends BlockEntity {

    private ItemStack storedTire = ItemStack.EMPTY;
    private ItemStack storedRim = ItemStack.EMPTY;
    private ItemStack storedWheel = ItemStack.EMPTY;

    public TireChangerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIRE_CHANGER_STATION.get(), pos, state);
    }

    public boolean hasTire() {
        return !storedTire.isEmpty();
    }

    public boolean hasRim() {
        return !storedRim.isEmpty();
    }

    public boolean hasWheel() {
        return !storedWheel.isEmpty();
    }

    public ItemStack getStoredTire() {
        return storedTire;
    }

    public ItemStack getStoredRim() {
        return storedRim;
    }

    public ItemStack getStoredWheel() {
        return storedWheel;
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (this.level == null) return InteractionResult.PASS;

        // 1. WRENCH INTERACTION -> Demount assembled wheel
        if (held.getItem() instanceof WrenchItem || held.is(ModItems.WRENCH.get())) {
            if (hasWheel()) {
                if (!this.level.isClientSide) {
                    String tireId = VehicleWheelItem.getTireId(storedWheel);
                    String rimId = VehicleWheelItem.getRimId(storedWheel);

                    storedTire = VehicleTireItem.create(tireId);
                    storedRim = VehicleRimItem.create(rimId);
                    storedWheel = ItemStack.EMPTY;

                    this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.4F);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
                    player.displayClientMessage(Component.literal("§a[Шиномонтаж] §fКолесо успешно разбортировано на диск и резину!"), true);

                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 2. RETRIEVE ASSEMBLED WHEEL
        if (hasWheel()) {
            if (held.isEmpty() || player.isShiftKeyDown()) {
                if (!this.level.isClientSide) {
                    giveOrDrop(player, storedWheel);
                    storedWheel = ItemStack.EMPTY;
                    this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 1.0F);
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 3. MOUNT RIM INTO TIRE
        if (hasTire() && !hasRim()) {
            if (held.getItem() instanceof VehicleRimItem || held.is(ModItems.VEHICLE_RIM.get())) {
                if (!this.level.isClientSide) {
                    String tireId = VehicleTireItem.getTireId(storedTire);
                    String rimId = VehicleRimItem.getRimId(held);

                    if (!player.getAbilities().instabuild) held.shrink(1);

                    storedWheel = VehicleWheelItem.createWheel(tireId, rimId);
                    storedTire = ItemStack.EMPTY;

                    this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.6F);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§a[Шиномонтаж] §fДиск запрессован в покрышку! Колесо собрано."), true);

                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            } else if (held.isEmpty()) {
                if (!this.level.isClientSide) {
                    giveOrDrop(player, storedTire);
                    storedTire = ItemStack.EMPTY;
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        // 4. MOUNT TIRE ONTO RIM
        if (hasRim() && !hasTire()) {
            if (held.getItem() instanceof VehicleTireItem || held.is(ModItems.VEHICLE_TIRE.get())) {
                if (!this.level.isClientSide) {
                    String rimId = VehicleRimItem.getRimId(storedRim);
                    String tireId = VehicleTireItem.getTireId(held);

                    if (!player.getAbilities().instabuild) held.shrink(1);

                    storedWheel = VehicleWheelItem.createWheel(tireId, rimId);
                    storedRim = ItemStack.EMPTY;

                    this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.6F);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§a[Шиномонтаж] §fПокрышка надета на диск! Колесо собрано."), true);

                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            } else if (held.isEmpty()) {
                if (!this.level.isClientSide) {
                    giveOrDrop(player, storedRim);
                    storedRim = ItemStack.EMPTY;
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        // 5. RETRIEVE INDIVIDUAL PIECES (if both present after demounting)
        if (hasTire() && hasRim()) {
            if (held.isEmpty()) {
                if (!this.level.isClientSide) {
                    giveOrDrop(player, storedRim);
                    storedRim = ItemStack.EMPTY;
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        // 6. PLACE ITEMS ONTO EMPTY MACHINE
        if (!hasTire() && !hasRim() && !hasWheel()) {
            if (held.getItem() instanceof VehicleTireItem || held.is(ModItems.VEHICLE_TIRE.get())) {
                if (!this.level.isClientSide) {
                    storedTire = held.split(1);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§e[Шиномонтаж] §fПокрышка зафиксирована на столе станка."), true);
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
            if (held.getItem() instanceof VehicleRimItem || held.is(ModItems.VEHICLE_RIM.get())) {
                if (!this.level.isClientSide) {
                    storedRim = held.split(1);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§e[Шиномонтаж] §fДиск зафиксирован на столе станка."), true);
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
            if (held.getItem() instanceof VehicleWheelItem || held.is(ModItems.VEHICLE_WHEEL.get())) {
                if (!this.level.isClientSide) {
                    storedWheel = held.split(1);
                    this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
                    player.displayClientMessage(Component.literal("§e[Шиномонтаж] §fКолесо зафиксировано на станке. Нажмите гаечным ключом для разбортировки."), true);
                    setChanged();
                    this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
        }
    }

    public void dropAllContents() {
        if (this.level == null || this.level.isClientSide) return;
        if (!storedTire.isEmpty()) Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), storedTire);
        if (!storedRim.isEmpty()) Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), storedRim);
        if (!storedWheel.isEmpty()) Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), storedWheel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!storedTire.isEmpty()) tag.put("StoredTire", storedTire.save(new CompoundTag()));
        if (!storedRim.isEmpty()) tag.put("StoredRim", storedRim.save(new CompoundTag()));
        if (!storedWheel.isEmpty()) tag.put("StoredWheel", storedWheel.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedTire = tag.contains("StoredTire") ? ItemStack.of(tag.getCompound("StoredTire")) : ItemStack.EMPTY;
        storedRim = tag.contains("StoredRim") ? ItemStack.of(tag.getCompound("StoredRim")) : ItemStack.EMPTY;
        storedWheel = tag.contains("StoredWheel") ? ItemStack.of(tag.getCompound("StoredWheel")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
