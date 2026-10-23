package com.evgen.cardecor.block;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.item.VehicleEngineItem;
import com.evgen.cardecor.item.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EngineCraneBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

    public EngineCraneBlock() {
        super(BlockBehaviour.Properties.of()
            .strength(3.5F, 8.0F)
            .sound(SoundType.ANVIL)
            .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineCraneBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EngineCraneBlockEntity crane) {
                if (crane.hasEngine()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, crane.getHeldEngine());
                    crane.clearEngine();
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EngineCraneBlockEntity crane)) {
            return InteractionResult.PASS;
        }

        ItemStack heldStack = player.getItemInHand(hand);

        // 1. Wrench interaction: Mount engine to car, or dismount engine from car
        if (heldStack.getItem() instanceof WrenchItem) {
            List<DrivableVehicleEntity> nearby = level.getEntitiesOfClass(
                DrivableVehicleEntity.class,
                new AABB(pos).inflate(3.5, 2.5, 3.5)
            );

            DrivableVehicleEntity targetVehicle = null;
            double minDist = Double.MAX_VALUE;
            for (DrivableVehicleEntity v : nearby) {
                double dist = v.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                if (dist < minDist) {
                    minDist = dist;
                    targetVehicle = v;
                }
            }

            if (targetVehicle == null) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cПоблизости нет автомобиля для работы с двигателем!"), true);
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Case A: Crane has engine -> Install into vehicle
            if (crane.hasEngine()) {
                if (targetVehicle.hasEngine()) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.literal("§cВ автомобиле уже установлен двигатель! Сначала демонтируйте его."), true);
                        level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                if (!level.isClientSide) {
                    ItemStack engineToInstall = crane.getHeldEngine();
                    String engineId = VehicleEngineItem.getEngineId(engineToInstall);
                    float cond = VehicleEngineItem.getCondition(engineToInstall);
                    float oil = VehicleEngineItem.getOil(engineToInstall);
                    float qual = VehicleEngineItem.getQuality(engineToInstall);

                    targetVehicle.setInstalledEngine(engineId);
                    targetVehicle.getEntityData().set(DrivableVehicleEntity.ENGINE_CONDITION, cond);
                    targetVehicle.getEntityData().set(DrivableVehicleEntity.ENGINE_OIL, oil);
                    targetVehicle.getEntityData().set(DrivableVehicleEntity.ENGINE_QUALITY, qual);

                    crane.clearEngine();

                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.1F);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§aДвигатель §e" + engineId + " §aуспешно установлен в автомобиль!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Case B: Crane empty -> Dismount engine from vehicle onto crane
            if (!targetVehicle.hasEngine()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cВ автомобиле нет двигателя для демонтажа!"), true);
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide) {
                String engineId = targetVehicle.getInstalledEngine();
                float cond = targetVehicle.getEntityData().get(DrivableVehicleEntity.ENGINE_CONDITION);
                float oil = targetVehicle.getEntityData().get(DrivableVehicleEntity.ENGINE_OIL);
                float qual = targetVehicle.getEntityData().get(DrivableVehicleEntity.ENGINE_QUALITY);

                ItemStack extracted = VehicleEngineItem.create(engineId, cond, oil, qual);
                crane.setHeldEngine(extracted);

                targetVehicle.setInstalledEngine("none");

                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§eДвигатель §6" + engineId + " §eдемонтирован на крюк крана!"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. Hang engine from hand onto crane
        if (heldStack.getItem() instanceof VehicleEngineItem) {
            if (crane.hasEngine()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cНа крюке крана уже подвешен двигатель!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide) {
                ItemStack toHang = heldStack.copy();
                toHang.setCount(1);
                crane.setHeldEngine(toHang);

                if (!player.isCreative()) {
                    heldStack.shrink(1);
                }

                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.1F);
                player.displayClientMessage(Component.literal("§aДвигатель подвешен на крюк крана."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. Take engine off the crane (Sneak + Right Click or Empty Hand)
        if (crane.hasEngine() && (player.isShiftKeyDown() || heldStack.isEmpty())) {
            if (!level.isClientSide) {
                ItemStack retrieved = crane.getHeldEngine();
                crane.clearEngine();

                if (!player.getInventory().add(retrieved)) {
                    Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), retrieved);
                }

                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§eДвигатель снят с крана в инвентарь."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!crane.hasEngine() && heldStack.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§7[Кран] Подвесьте двигатель ПКМ или используйте гаечный ключ для монтажа."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
