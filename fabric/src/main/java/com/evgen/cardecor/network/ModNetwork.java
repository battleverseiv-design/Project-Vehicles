package com.evgen.cardecor.network;

import com.evgen.cardecor.client.ClientPacketHandler;
import com.evgen.cardecor.client.ClientPacketHandlers;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ModNetwork {
    public static final ResourceLocation VEHICLE_INPUT = new ResourceLocation("cardecor", "vehicle_input");
    public static final ResourceLocation VEHICLE_UPDATE = new ResourceLocation("cardecor", "vehicle_update");
    public static final ResourceLocation VEHICLE_CHANGE_SEAT = new ResourceLocation("cardecor", "vehicle_change_seat");
    public static final ResourceLocation VEHICLE_DISMOUNT = new ResourceLocation("cardecor", "vehicle_dismount");
    public static final ResourceLocation VEHICLE_HORN = new ResourceLocation("cardecor", "vehicle_horn");
    public static final ResourceLocation VEHICLE_HEADLIGHT = new ResourceLocation("cardecor", "vehicle_headlight");
    public static final ResourceLocation VEHICLE_SAVE_CONFIG = new ResourceLocation("cardecor", "vehicle_save_config");
    public static final ResourceLocation VEHICLE_SEAT_ACTION = new ResourceLocation("cardecor", "vehicle_seat_action");
    public static final ResourceLocation VEHICLE_CLIMATE = new ResourceLocation("cardecor", "vehicle_climate");
    public static final ResourceLocation VEHICLE_CHANGE_ENGINE = new ResourceLocation("cardecor", "vehicle_change_engine");
    public static final ResourceLocation VEHICLE_REFILL_FLUID = new ResourceLocation("cardecor", "vehicle_refill_fluid");
    public static final ResourceLocation VEHICLE_ENGINE_ACTION = new ResourceLocation("cardecor", "vehicle_engine_action");
    public static final ResourceLocation ENGINE_WORKBENCH_ACTION = new ResourceLocation("cardecor", "engine_workbench_action");

    public static final ResourceLocation DAMAGE_SPARKS = new ResourceLocation("cardecor", "damage_sparks");
    public static final ResourceLocation OPEN_TUNING_GUI = new ResourceLocation("cardecor", "open_tuning_gui");
    public static final ResourceLocation OPEN_VEHICLE_MECHANICS = new ResourceLocation("cardecor", "open_vehicle_mechanics");
    public static final ResourceLocation EXHAUST_FLASH = new ResourceLocation("cardecor", "exhaust_flash");
    public static final ResourceLocation IMPACT_DEFORM = new ResourceLocation("cardecor", "impact_deform");
    public static final ResourceLocation VEHICLE_REPAIR = new ResourceLocation("cardecor", "vehicle_repair");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_INPUT, (server, player, handler, buf, responseSender) -> {
            VehicleInputPacket packet = new VehicleInputPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_UPDATE, (server, player, handler, buf, responseSender) -> {
            VehicleUpdatePacket packet = new VehicleUpdatePacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_CHANGE_SEAT, (server, player, handler, buf, responseSender) -> {
            VehicleChangeSeatPacket packet = new VehicleChangeSeatPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_DISMOUNT, (server, player, handler, buf, responseSender) -> {
            VehicleDismountPacket packet = new VehicleDismountPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_HORN, (server, player, handler, buf, responseSender) -> {
            VehicleHornPacket packet = new VehicleHornPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_HEADLIGHT, (server, player, handler, buf, responseSender) -> {
            VehicleHeadlightPacket packet = new VehicleHeadlightPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_SAVE_CONFIG, (server, player, handler, buf, responseSender) -> {
            VehicleSaveConfigPacket packet = new VehicleSaveConfigPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_SEAT_ACTION, (server, player, handler, buf, responseSender) -> {
            VehicleSeatActionPacket packet = new VehicleSeatActionPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_CLIMATE, (server, player, handler, buf, responseSender) -> {
            VehicleClimatePacket packet = new VehicleClimatePacket(buf);
            packet.handle(server, player, handler, responseSender);
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_CHANGE_ENGINE, (server, player, handler, buf, responseSender) -> {
            VehicleChangeEnginePacket packet = new VehicleChangeEnginePacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_REFILL_FLUID, (server, player, handler, buf, responseSender) -> {
            VehicleRefillFluidPacket packet = new VehicleRefillFluidPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VEHICLE_ENGINE_ACTION, (server, player, handler, buf, responseSender) -> {
            VehicleEngineActionPacket packet = new VehicleEngineActionPacket(buf);
            server.execute(() -> packet.handle(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(ENGINE_WORKBENCH_ACTION, (server, player, handler, buf, responseSender) -> {
            EngineWorkbenchActionPacket packet = new EngineWorkbenchActionPacket(buf);
            server.execute(() -> packet.handle(player));
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(OPEN_VEHICLE_MECHANICS, (client, handler, buf, responseSender) -> {
            OpenVehicleMechanicsPacket packet = new OpenVehicleMechanicsPacket(buf);
            client.execute(() -> packet.handleClient());
        });

        ClientPlayNetworking.registerGlobalReceiver(DAMAGE_SPARKS, (client, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            client.execute(() -> ClientPacketHandler.handleDamageSparks(x, y, z));
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_TUNING_GUI, (client, handler, buf, responseSender) -> {
            String packId = buf.readUtf();
            client.execute(() -> ClientPacketHandlers.handleOpenTuningGui(packId));
        });

        ClientPlayNetworking.registerGlobalReceiver(EXHAUST_FLASH, (client, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            double fwdX = buf.readDouble();
            double fwdZ = buf.readDouble();
            boolean isHeavyPop = buf.readBoolean();
            client.execute(() -> ClientPacketHandler.handleExhaustFlash(x, y, z, fwdX, fwdZ, isHeavyPop));
        });

        ClientPlayNetworking.registerGlobalReceiver(IMPACT_DEFORM, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            float localX = buf.readFloat();
            float localY = buf.readFloat();
            float localZ = buf.readFloat();
            float dirX = buf.readFloat();
            float dirY = buf.readFloat();
            float dirZ = buf.readFloat();
            float radius = buf.readFloat();
            float depth = buf.readFloat();
            client.execute(() -> ClientPacketHandler.handleImpactDeform(entityId, localX, localY, localZ, dirX, dirY, dirZ, radius, depth));
        });

        ClientPlayNetworking.registerGlobalReceiver(VEHICLE_REPAIR, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            client.execute(() -> ClientPacketHandler.handleVehicleRepair(entityId));
        });
    }

    public static void sendToServer(Object packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation id = null;

        if (packet instanceof VehicleInputPacket p) {
            id = VEHICLE_INPUT;
            p.toBytes(buf);
        } else if (packet instanceof VehicleUpdatePacket p) {
            id = VEHICLE_UPDATE;
            p.toBytes(buf);
        } else if (packet instanceof VehicleChangeSeatPacket p) {
            id = VEHICLE_CHANGE_SEAT;
            p.toBytes(buf);
        } else if (packet instanceof VehicleDismountPacket p) {
            id = VEHICLE_DISMOUNT;
            p.toBytes(buf);
        } else if (packet instanceof VehicleHornPacket p) {
            id = VEHICLE_HORN;
            p.toBytes(buf);
        } else if (packet instanceof VehicleHeadlightPacket p) {
            id = VEHICLE_HEADLIGHT;
            p.toBytes(buf);
        } else if (packet instanceof VehicleSaveConfigPacket p) {
            id = VEHICLE_SAVE_CONFIG;
            p.toBytes(buf);
        } else if (packet instanceof VehicleSeatActionPacket p) {
            id = VEHICLE_SEAT_ACTION;
            p.toBytes(buf);
        } else if (packet instanceof VehicleClimatePacket p) {
            id = VEHICLE_CLIMATE;
            p.toBytes(buf);
        } else if (packet instanceof VehicleChangeEnginePacket p) {
            id = VEHICLE_CHANGE_ENGINE;
            p.toBytes(buf);
        } else if (packet instanceof VehicleRefillFluidPacket p) {
            id = VEHICLE_REFILL_FLUID;
            p.toBytes(buf);
        } else if (packet instanceof VehicleEngineActionPacket p) {
            id = VEHICLE_ENGINE_ACTION;
            p.toBytes(buf);
        } else if (packet instanceof EngineWorkbenchActionPacket p) {
            id = ENGINE_WORKBENCH_ACTION;
            p.toBytes(buf);
        }

        if (id != null) {
            ClientPlayNetworking.send(id, buf);
        }
    }

    public static void sendToTrackingAndSelf(Entity entity, Object packet) {
        if (entity.level().isClientSide) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation id = encodePacket(packet, buf);
        if (id == null) return;

        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);

        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            FriendlyByteBuf pBuf = PacketByteBufs.create();
            pBuf.writeBytes(bytes);
            ServerPlayNetworking.send(player, id, pBuf);
        }

        if (entity instanceof ServerPlayer sp) {
            FriendlyByteBuf pBuf = PacketByteBufs.create();
            pBuf.writeBytes(bytes);
            ServerPlayNetworking.send(sp, id, pBuf);
        }
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation id = encodePacket(packet, buf);
        if (id != null && player != null) {
            ServerPlayNetworking.send(player, id, buf);
        }
    }

    private static ResourceLocation encodePacket(Object packet, FriendlyByteBuf buf) {
        if (packet instanceof VehicleDamageSparksPacket p) {
            p.toBytes(buf);
            return DAMAGE_SPARKS;
        } else if (packet instanceof OpenTuningGuiPacket p) {
            p.toBytes(buf);
            return OPEN_TUNING_GUI;
        } else if (packet instanceof OpenVehicleMechanicsPacket p) {
            p.toBytes(buf);
            return OPEN_VEHICLE_MECHANICS;
        } else if (packet instanceof VehicleExhaustFlashPacket p) {
            p.toBytes(buf);
            return EXHAUST_FLASH;
        } else if (packet instanceof VehicleImpactDeformPacket p) {
            p.toBytes(buf);
            return IMPACT_DEFORM;
        } else if (packet instanceof VehicleRepairPacket p) {
            p.toBytes(buf);
            return VEHICLE_REPAIR;
        }
        return null;
    }
}
