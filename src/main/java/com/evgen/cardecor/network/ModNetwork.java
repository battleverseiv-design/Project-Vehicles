package com.evgen.cardecor.network;

import com.evgen.cardecor.CarDecorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CarDecorMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object packet) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void register() {
        CHANNEL.registerMessage(
            packetId++,
            VehicleInputPacket.class,
            VehicleInputPacket::toBytes,
            VehicleInputPacket::new,
            VehicleInputPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleUpdatePacket.class,
            VehicleUpdatePacket::toBytes,
            VehicleUpdatePacket::new,
            VehicleUpdatePacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleChangeSeatPacket.class,
            VehicleChangeSeatPacket::toBytes,
            VehicleChangeSeatPacket::new,
            VehicleChangeSeatPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleDismountPacket.class,
            VehicleDismountPacket::toBytes,
            VehicleDismountPacket::new,
            VehicleDismountPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleHornPacket.class,
            VehicleHornPacket::toBytes,
            VehicleHornPacket::new,
            VehicleHornPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleHeadlightPacket.class,
            VehicleHeadlightPacket::toBytes,
            VehicleHeadlightPacket::new,
            VehicleHeadlightPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleDamageSparksPacket.class,
            VehicleDamageSparksPacket::toBytes,
            VehicleDamageSparksPacket::new,
            VehicleDamageSparksPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleSaveConfigPacket.class,
            VehicleSaveConfigPacket::toBytes,
            VehicleSaveConfigPacket::new,
            VehicleSaveConfigPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            OpenTuningGuiPacket.class,
            OpenTuningGuiPacket::toBytes,
            OpenTuningGuiPacket::new,
            OpenTuningGuiPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleExhaustFlashPacket.class,
            VehicleExhaustFlashPacket::toBytes,
            VehicleExhaustFlashPacket::new,
            VehicleExhaustFlashPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleImpactDeformPacket.class,
            VehicleImpactDeformPacket::toBytes,
            VehicleImpactDeformPacket::new,
            VehicleImpactDeformPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleRepairPacket.class,
            VehicleRepairPacket::toBytes,
            VehicleRepairPacket::new,
            VehicleRepairPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleSeatActionPacket.class,
            VehicleSeatActionPacket::toBytes,
            VehicleSeatActionPacket::new,
            VehicleSeatActionPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleClimatePacket.class,
            VehicleClimatePacket::toBytes,
            VehicleClimatePacket::new,
            VehicleClimatePacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleChangeEnginePacket.class,
            VehicleChangeEnginePacket::toBytes,
            VehicleChangeEnginePacket::new,
            VehicleChangeEnginePacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            OpenVehicleMechanicsPacket.class,
            OpenVehicleMechanicsPacket::toBytes,
            OpenVehicleMechanicsPacket::new,
            OpenVehicleMechanicsPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleRefillFluidPacket.class,
            VehicleRefillFluidPacket::toBytes,
            VehicleRefillFluidPacket::new,
            VehicleRefillFluidPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            VehicleEngineActionPacket.class,
            VehicleEngineActionPacket::toBytes,
            VehicleEngineActionPacket::new,
            VehicleEngineActionPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            EngineWorkbenchActionPacket.class,
            EngineWorkbenchActionPacket::toBytes,
            EngineWorkbenchActionPacket::new,
            EngineWorkbenchActionPacket::handle
        );
        CHANNEL.registerMessage(
            packetId++,
            com.evgen.cardecor.physics.zerogc.VehiclePhysicsPacket.class,
            com.evgen.cardecor.physics.zerogc.VehiclePhysicsPacket::toBytes,
            com.evgen.cardecor.physics.zerogc.VehiclePhysicsPacket::new,
            com.evgen.cardecor.physics.zerogc.VehiclePhysicsPacket::handle
        );
    }
}
