package com.evgen.cardecor.network;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class VehicleSaveConfigPacket {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final String packId;
    public final String jsonPayload;

    public VehicleSaveConfigPacket(String packId, String jsonPayload) {
        this.packId = packId;
        this.jsonPayload = jsonPayload;
    }

    public VehicleSaveConfigPacket(FriendlyByteBuf buf) {
        this.packId = buf.readUtf();
        this.jsonPayload = buf.readUtf(262144);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.packId);
        buf.writeUtf(this.jsonPayload);
    }

    public void handle(ServerPlayer player) {
        if (player == null) return;

        boolean isAllowed = player.hasPermissions(2) || (player.getServer() != null && (player.getServer().isSingleplayer() || player.getServer().isSingleplayerOwner(player.getGameProfile())));
        if (!isAllowed) {
            player.displayClientMessage(Component.literal("§c[CarDecor] Только администраторы (OP) могут изменять конфигурации машин!"), false);
            return;
        }

        VehicleDefinition def = VehiclePackManager.get(this.packId);
        if (def == null || def.packDirectory == null) {
            player.displayClientMessage(Component.literal("§c[CarDecor] Пак машины '" + this.packId + "' не найден на сервере!"), false);
            return;
        }

        File jsonFile = (def.sourceJsonFile != null && def.sourceJsonFile.exists()) ? def.sourceJsonFile : new File(def.packDirectory, "vehicle.json");
        try {
            JsonObject root = JsonParser.parseString(this.jsonPayload).getAsJsonObject();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            VehiclePackManager.loadPacks();

            if (player.getServer() != null) {
                for (ServerLevel sl : player.getServer().getAllLevels()) {
                    for (net.minecraft.world.entity.Entity e : sl.getAllEntities()) {
                        if (e instanceof DrivablePackVehicleEntity pv) {
                            pv.applyDefinitionPhysics();
                        }
                    }
                }
            }

            player.displayClientMessage(Component.literal("§a[CarDecor] Настройки для §f" + def.name + "§a успешно сохранены и применены на сервере!"), false);
        } catch (Exception e) {
            player.displayClientMessage(Component.literal("§c[CarDecor] Ошибка сохранения: " + e.getMessage()), false);
        }
    }
}
