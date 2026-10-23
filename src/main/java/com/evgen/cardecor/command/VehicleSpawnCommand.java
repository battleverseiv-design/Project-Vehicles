package com.evgen.cardecor.command;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.init.ModEntities;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.evgen.cardecor.physics.RaycastVehiclePhysics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VehicleSpawnCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PACKS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(VehiclePackManager.getAllKeys(), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STEER_MODES = (ctx, builder) ->
        SharedSuggestionProvider.suggest(new String[]{"low", "mid", "high", "global"}, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CATEGORIES = (ctx, builder) ->
        SharedSuggestionProvider.suggest(new String[]{"sedan", "pickup", "truck", "sports", "suv"}, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("car")
                .requires(source -> {
                    if (source.hasPermission(2)) return true;
                    if (source.getServer() != null && source.isPlayer()) {
                        ServerPlayer sp = source.getPlayer();
                        return sp != null && source.getServer().isSingleplayerOwner(sp.getGameProfile());
                    }
                    return false;
                })
                // /car gui [pack_id]
                .then(Commands.literal("gui")
                    .executes(ctx -> openTuningGui(ctx.getSource(), null))
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> openTuningGui(ctx.getSource(), StringArgumentType.getString(ctx, "pack")))
                    )
                )
                // /car tune [pack_id]
                .then(Commands.literal("tune")
                    .executes(ctx -> openTuningGui(ctx.getSource(), null))
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> openTuningGui(ctx.getSource(), StringArgumentType.getString(ctx, "pack")))
                    )
                )
                // /car menu [pack_id]
                .then(Commands.literal("menu")
                    .executes(ctx -> openTuningGui(ctx.getSource(), null))
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> openTuningGui(ctx.getSource(), StringArgumentType.getString(ctx, "pack")))
                    )
                )
                // /car reload
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        VehiclePackManager.loadPacks();
                        ctx.getSource().sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u0412\u0441\u0435 \u043f\u0430\u043a\u0438 \u043c\u0430\u0448\u0438\u043d \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u044b! \u041d\u0430\u0439\u0434\u0435\u043d\u043e: " + VehiclePackManager.getAll().size()), true);
                        return 1;
                    })
                )
                // /car list
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("\u00a7e[CarDecor] \u0417\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u043d\u044b\u0435 \u043f\u0430\u043a\u0438: " + String.join(", ", VehiclePackManager.getAllKeys())), false);
                        return 1;
                    })
                )
                // /car template <id> [category]
                .then(Commands.literal("template")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> createTemplate(ctx.getSource(), StringArgumentType.getString(ctx, "id"), "sedan"))
                        .then(Commands.argument("category", StringArgumentType.string()).suggests(SUGGEST_CATEGORIES)
                            .executes(ctx -> createTemplate(ctx.getSource(), StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "category")))
                        )
                    )
                )
                // /car validate [pack]
                .then(Commands.literal("validate")
                    .executes(ctx -> validatePacks(ctx.getSource(), null))
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> validatePacks(ctx.getSource(), StringArgumentType.getString(ctx, "pack")))
                    )
                )
                // /car ride <pack_id> [pos] [targets] [seat]
                .then(Commands.literal("ride")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), null, null, 0))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                            .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), Vec3Argument.getVec3(ctx, "pos"), null, 0))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), Vec3Argument.getVec3(ctx, "pos"), null, IntegerArgumentType.getInteger(ctx, "seat")))
                            )
                            .then(Commands.argument("targets", net.minecraft.commands.arguments.EntityArgument.entities())
                                .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), Vec3Argument.getVec3(ctx, "pos"), net.minecraft.commands.arguments.EntityArgument.getEntities(ctx, "targets"), 0))
                                .then(Commands.argument("seat", IntegerArgumentType.integer(0, 4))
                                    .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), Vec3Argument.getVec3(ctx, "pos"), net.minecraft.commands.arguments.EntityArgument.getEntities(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "seat")))
                                )
                            )
                        )
                        .then(Commands.argument("targets", net.minecraft.commands.arguments.EntityArgument.entities())
                            .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), null, net.minecraft.commands.arguments.EntityArgument.getEntities(ctx, "targets"), 0))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), null, net.minecraft.commands.arguments.EntityArgument.getEntities(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "seat")))
                            )
                        )
                    )
                )
                // /car enter
                .then(Commands.literal("enter")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> spawnAndRide(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), null, null, 0))
                    )
                )
                // /car spawn <pack_id> [pos]
                .then(Commands.literal("spawn")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .executes(ctx -> spawnAt(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), null))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                            .executes(ctx -> spawnAt(ctx.getSource(), StringArgumentType.getString(ctx, "pack"), Vec3Argument.getVec3(ctx, "pos")))
                        )
                    )
                )
                // /car seat <pack_id> <seat_index> <x_side> <y_height> <z_forward>
                .then(Commands.literal("seat")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .then(Commands.argument("seat_index", IntegerArgumentType.integer(0, 4))
                            .then(Commands.argument("x_side", DoubleArgumentType.doubleArg(-3.0, 3.0))
                                .then(Commands.argument("y_height", DoubleArgumentType.doubleArg(-2.0, 3.0))
                                    .then(Commands.argument("z_forward", DoubleArgumentType.doubleArg(-5.0, 5.0))
                                        .executes(ctx -> setSeatPos(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "pack"),
                                            IntegerArgumentType.getInteger(ctx, "seat_index"),
                                            (float) DoubleArgumentType.getDouble(ctx, "x_side"),
                                            (float) DoubleArgumentType.getDouble(ctx, "y_height"),
                                            (float) DoubleArgumentType.getDouble(ctx, "z_forward")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
                // /car sw <pack_id> <x> <y> <z> [tilt_angle]
                .then(Commands.literal("sw")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg(-3.0, 3.0))
                            .then(Commands.argument("y", DoubleArgumentType.doubleArg(-2.0, 3.0))
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg(-5.0, 5.0))
                                    .executes(ctx -> setSteeringWheelPos(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "pack"),
                                        (float) DoubleArgumentType.getDouble(ctx, "x"),
                                        (float) DoubleArgumentType.getDouble(ctx, "y"),
                                        (float) DoubleArgumentType.getDouble(ctx, "z"),
                                        0.0F
                                    ))
                                    .then(Commands.argument("tilt", DoubleArgumentType.doubleArg(-90.0, 90.0))
                                        .executes(ctx -> setSteeringWheelPos(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "pack"),
                                            (float) DoubleArgumentType.getDouble(ctx, "x"),
                                            (float) DoubleArgumentType.getDouble(ctx, "y"),
                                            (float) DoubleArgumentType.getDouble(ctx, "z"),
                                            (float) DoubleArgumentType.getDouble(ctx, "tilt")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
                // /car steer
                .then(Commands.literal("steer")
                    .executes(ctx -> printSteerInfo(ctx.getSource()))
                    .then(Commands.literal("info").executes(ctx -> printSteerInfo(ctx.getSource())))
                    .then(Commands.literal("set")
                        .then(Commands.argument("bracket", StringArgumentType.word()).suggests(SUGGEST_STEER_MODES)
                            .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.01, 5.0))
                                .executes(ctx -> setSteerBracket(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "bracket"),
                                    DoubleArgumentType.getDouble(ctx, "multiplier")
                                ))
                            )
                        )
                    )
                    .then(Commands.literal("curve")
                        .then(Commands.argument("low", DoubleArgumentType.doubleArg(0.01, 5.0))
                            .then(Commands.argument("mid", DoubleArgumentType.doubleArg(0.01, 5.0))
                                .then(Commands.argument("high", DoubleArgumentType.doubleArg(0.01, 5.0))
                                    .executes(ctx -> setSteerCurve(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "low"),
                                        DoubleArgumentType.getDouble(ctx, "mid"),
                                        DoubleArgumentType.getDouble(ctx, "high")
                                    ))
                                )
                            )
                        )
                    )
                    .then(Commands.literal("brackets")
                        .then(Commands.argument("lowSpeed", DoubleArgumentType.doubleArg(1.0, 150.0))
                            .then(Commands.argument("highSpeed", DoubleArgumentType.doubleArg(5.0, 300.0))
                                .executes(ctx -> setSpeedBrackets(
                                    ctx.getSource(),
                                    DoubleArgumentType.getDouble(ctx, "lowSpeed"),
                                    DoubleArgumentType.getDouble(ctx, "highSpeed")
                                ))
                            )
                        )
                    )
                )
                // /car sound <pack_id> <idle_to_low> <low_to_mid> <mid_to_high> <high_to_max>
                .then(Commands.literal("sound")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .then(Commands.argument("idle_low", DoubleArgumentType.doubleArg(1.0, 100.0))
                            .then(Commands.argument("low_mid", DoubleArgumentType.doubleArg(5.0, 150.0))
                                .then(Commands.argument("mid_high", DoubleArgumentType.doubleArg(10.0, 200.0))
                                    .then(Commands.argument("high_max", DoubleArgumentType.doubleArg(20.0, 300.0))
                                        .executes(ctx -> setSoundThresholds(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "pack"),
                                            (float) DoubleArgumentType.getDouble(ctx, "idle_low"),
                                            (float) DoubleArgumentType.getDouble(ctx, "low_mid"),
                                            (float) DoubleArgumentType.getDouble(ctx, "mid_high"),
                                            (float) DoubleArgumentType.getDouble(ctx, "high_max")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
                // /car starter <pack_id> <start_chance> <crank_ticks>
                .then(Commands.literal("starter")
                    .then(Commands.argument("pack", StringArgumentType.string()).suggests(SUGGEST_PACKS)
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                            .then(Commands.argument("ticks", IntegerArgumentType.integer(5, 120))
                                .executes(ctx -> setStarterConfig(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "pack"),
                                    DoubleArgumentType.getDouble(ctx, "chance"),
                                    IntegerArgumentType.getInteger(ctx, "ticks")
                                ))
                            )
                        )
                    )
                )
        );
    }

    private static int openTuningGui(CommandSourceStack source, String packId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player only command"));
            return 0;
        }

        String targetPack = packId;
        if (targetPack == null) {
            if (player.getVehicle() instanceof DrivablePackVehicleEntity pv) {
                targetPack = pv.getVehicleId();
            } else {
                targetPack = "blue_sedan";
            }
        }

        final String finalPack = targetPack;
        com.evgen.cardecor.network.ModNetwork.sendToPlayer(
            player,
            new com.evgen.cardecor.network.OpenTuningGuiPacket(finalPack)
        );
        source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u041e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u043c \u043c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u0443\u044e \u0442\u044e\u043d\u0438\u043d\u0433\u0430 \u0434\u043b\u044f: \u00a7f" + finalPack), false);
        return 1;
    }

    private static int setSeatPos(CommandSourceStack source, String packId, int seatIdx, float x, float y, float z) {
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null || def.packDirectory == null) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041f\u0430\u043a '" + packId + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!"));
            return 0;
        }

        File jsonFile = new File(def.packDirectory, "vehicle.json");
        try {
            JsonObject root;
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonArray seatsArr = root.has("seats") ? root.getAsJsonArray("seats") : new JsonArray();
            while (seatsArr.size() <= seatIdx) {
                JsonObject sObj = new JsonObject();
                sObj.addProperty("seatIndex", seatsArr.size());
                sObj.addProperty("name", "Seat " + seatsArr.size());
                JsonArray pArr = new JsonArray();
                pArr.add(0.0F); pArr.add(0.0F); pArr.add(0.0F);
                sObj.add("pos", pArr);
                seatsArr.add(sObj);
            }

            JsonObject targetObj = seatsArr.get(seatIdx).getAsJsonObject();
            JsonArray posArr = new JsonArray();
            posArr.add(x); posArr.add(y); posArr.add(z);
            targetObj.add("pos", posArr);
            root.add("seats", seatsArr);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            VehiclePackManager.loadPacks();
            source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u041c\u0435\u0441\u0442\u043e \u00a7e" + seatIdx + "\u00a7a \u0434\u043b\u044f \u00a7f" + packId + "\u00a7a \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e: \u00a7bX=" + x + ", Y=" + y + ", Z=" + z), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f: " + e.getMessage()));
            return 0;
        }
    }

    private static int setSteeringWheelPos(CommandSourceStack source, String packId, float x, float y, float z, float tilt) {
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null || def.packDirectory == null) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041f\u0430\u043a '" + packId + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!"));
            return 0;
        }

        File jsonFile = new File(def.packDirectory, "vehicle.json");
        try {
            JsonObject root;
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject sw = root.has("steering_wheel") ? root.getAsJsonObject("steering_wheel") : new JsonObject();
            sw.addProperty("enabled", true);
            JsonArray posArr = new JsonArray();
            posArr.add(x); posArr.add(y); posArr.add(z);
            sw.add("pos", posArr);
            sw.addProperty("tilt_angle_deg", tilt);
            root.add("steering_wheel", sw);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            VehiclePackManager.loadPacks();
            source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u0420\u0443\u043b\u044c \u0434\u043b\u044f \u00a7f" + packId + "\u00a7a \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d: \u00a7bX=" + x + ", Y=" + y + ", Z=" + z + ", \u041d\u0430\u043a\u043b\u043e\u043d=" + tilt + "\u00b0"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f: " + e.getMessage()));
            return 0;
        }
    }

    private static int printSteerInfo(CommandSourceStack source) {
        String msg = "[CarDecor] Steer: Low=" + String.format("%.2f", RaycastVehiclePhysics.steerLow) + "x, Mid=" + String.format("%.2f", RaycastVehiclePhysics.steerMid) + "x, High=" + String.format("%.2f", RaycastVehiclePhysics.steerHigh) + "x, Global=" + String.format("%.2f", RaycastVehiclePhysics.steerGlobal) + "x";
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int setSteerBracket(CommandSourceStack source, String bracket, double val) {
        switch (bracket.toLowerCase()) {
            case "low" -> RaycastVehiclePhysics.steerLow = val;
            case "mid" -> RaycastVehiclePhysics.steerMid = val;
            case "high" -> RaycastVehiclePhysics.steerHigh = val;
            case "global" -> RaycastVehiclePhysics.steerGlobal = val;
            case "angle" -> {
                // val in degrees
                RaycastVehiclePhysics.steerGlobal = 1.0;
            }
            default -> {
                source.sendFailure(Component.literal("Unknown bracket: low, mid, high, global"));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal("[CarDecor] Steer " + bracket + " set to " + String.format("%.2f", val) + "x"), true);
        return 1;
    }

    private static int setSteerCurve(CommandSourceStack source, double low, double mid, double high) {
        RaycastVehiclePhysics.steerLow = low;
        RaycastVehiclePhysics.steerMid = mid;
        RaycastVehiclePhysics.steerHigh = high;
        source.sendSuccess(() -> Component.literal("[CarDecor] Steer curve: Low=" + String.format("%.2f", low) + "x, Mid=" + String.format("%.2f", mid) + "x, High=" + String.format("%.2f", high) + "x"), true);
        return 1;
    }

    private static int setSpeedBrackets(CommandSourceStack source, double lowSpeed, double highSpeed) {
        source.sendSuccess(() -> Component.literal("[CarDecor] Use /car gui to adjust speed brackets per vehicle!"), false);
        return 1;
    }

    private static int spawnAt(CommandSourceStack source, String packId, Vec3 targetPos) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player only command"));
            return 0;
        }

        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null) {
            source.sendFailure(Component.literal("[CarDecor] Pack not found: " + packId));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Vec3 spawnPos;
        if (targetPos != null) {
            spawnPos = targetPos;
        } else {
            Vec3 look = player.getLookAngle();
            double sx = player.getX() + look.x * 3.5;
            double sz = player.getZ() + look.z * 3.5;
            BlockPos bp = BlockPos.containing(sx, player.getY(), sz);
            net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(bp);
            net.minecraft.world.phys.shapes.VoxelShape shape = bs.getCollisionShape(level, bp);
            double sy = bp.getY() + (shape.isEmpty() ? 0.0 : shape.max(net.minecraft.core.Direction.Axis.Y));
            if (shape.isEmpty()) {
                BlockPos belowBp = bp.below();
                net.minecraft.world.level.block.state.BlockState belowBs = level.getBlockState(belowBp);
                net.minecraft.world.phys.shapes.VoxelShape belowShape = belowBs.getCollisionShape(level, belowBp);
                sy = belowBp.getY() + (belowShape.isEmpty() ? 1.0 : belowShape.max(net.minecraft.core.Direction.Axis.Y));
            }
            spawnPos = new Vec3(sx, sy, sz);
        }

        DrivablePackVehicleEntity vehicle = new DrivablePackVehicleEntity(ModEntities.DRIVABLE_PACK_VEHICLE.get(), level, packId);
        vehicle.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0F);
        vehicle.setOldPosAndRot();
        vehicle.setDeltaMovement(Vec3.ZERO);
        vehicle.keySpawnInitialized = true;
        if (vehicle.physics != null) {
            vehicle.physics.targetSurfaceY = spawnPos.y;
            vehicle.physics.isGrounded = true;
            vehicle.physics.wasGrounded = true;
            vehicle.physics.isPhysicallySupported = true;
            vehicle.physics.verticalSpeed = 0.0;
        }
        level.addFreshEntity(vehicle);

        source.sendSuccess(() -> Component.literal("[CarDecor] Spawned " + def.name), true);
        return 1;
    }

    private static int spawnAndRide(CommandSourceStack source, String packId, Vec3 targetPos, java.util.Collection<? extends net.minecraft.world.entity.Entity> targets, int seatIndex) {
        ServerPlayer player = source.getPlayer();
        ServerLevel level = source.getLevel();

        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null) {
            source.sendFailure(Component.literal("[CarDecor] Pack not found: " + packId));
            return 0;
        }

        Vec3 spawnPos;
        float spawnYaw = 0.0F;
        if (targetPos != null) {
            spawnPos = targetPos;
            if (player != null) spawnYaw = player.getYRot();
        } else if (player != null) {
            spawnPos = player.position();
            spawnYaw = player.getYRot();
        } else if (targets != null && !targets.isEmpty()) {
            net.minecraft.world.entity.Entity first = targets.iterator().next();
            spawnPos = first.position();
            spawnYaw = first.getYRot();
        } else {
            spawnPos = source.getPosition();
        }

        DrivablePackVehicleEntity vehicle = new DrivablePackVehicleEntity(ModEntities.DRIVABLE_PACK_VEHICLE.get(), level, packId);
        vehicle.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        vehicle.setYRot(spawnYaw);
        level.addFreshEntity(vehicle);

        int maxSeats = vehicle.getMaxSeats();
        int mountedCount = 0;

        java.util.Collection<? extends net.minecraft.world.entity.Entity> riders = targets;
        if (riders == null || riders.isEmpty()) {
            if (player != null) {
                riders = java.util.Collections.singletonList(player);
            }
        }

        if (riders != null) {
            int currentSeat = seatIndex;
            for (net.minecraft.world.entity.Entity ent : riders) {
                if (mountedCount >= maxSeats) break;

                if (ent instanceof ServerPlayer sp) {
                    sp.teleportTo(spawnPos.x, spawnPos.y, spawnPos.z);
                } else {
                    ent.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                }

                ent.startRiding(vehicle, true);

                int passIdx = vehicle.getPassengers().indexOf(ent);
                if (passIdx >= 0) {
                    int assignedSeat = (currentSeat + mountedCount) % maxSeats;
                    int mask = vehicle.getEntityData().get(DrivablePackVehicleEntity.SEAT_SLOTS);
                    mask &= ~(0xF << (passIdx * 4));
                    mask |= (assignedSeat & 0xF) << (passIdx * 4);
                    vehicle.getEntityData().set(DrivablePackVehicleEntity.SEAT_SLOTS, mask);
                }

                mountedCount++;
            }
        }

        final int finalCount = mountedCount;
        source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u0421\u043e\u0437\u0434\u0430\u043d \u0438 \u043f\u043e\u0441\u0430\u0436\u0435\u043d: \u00a7f" + def.name + " \u00a7e(\u043f\u0430\u0441\u0441\u0430\u0436\u0438\u0440\u043e\u0432: " + finalCount + ")"), true);
        return mountedCount;
    }

    private static int setSoundThresholds(CommandSourceStack source, String packId, float iL, float lM, float mH, float hM) {
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null || def.packDirectory == null) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041f\u0430\u043a '" + packId + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!"));
            return 0;
        }
        File jsonFile = new File(def.packDirectory, "vehicle.json");
        try {
            JsonObject root;
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonObject s = root.has("sounds") ? root.getAsJsonObject("sounds") : new JsonObject();
            s.addProperty("speed_idle_to_low", iL);
            s.addProperty("speed_low_to_mid", lM);
            s.addProperty("speed_mid_to_high", mH);
            s.addProperty("speed_high_to_max", hM);
            root.add("sounds", s);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            VehiclePackManager.loadPacks();
            source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0437\u0432\u0443\u043a\u043e\u0432\u044b\u0445 \u043f\u043e\u0440\u043e\u0433\u043e\u0432 \u0434\u043b\u044f " + packId + " \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u044b! (" + iL + " -> " + lM + " -> " + mH + " -> " + hM + " \u043a\u043c/\u0447)"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041e\u0448\u0438\u0431\u043a\u0430: " + e.getMessage()));
            return 0;
        }
    }

    private static int setStarterConfig(CommandSourceStack source, String packId, double chance, int ticks) {
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null || def.packDirectory == null) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041f\u0430\u043a '" + packId + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!"));
            return 0;
        }
        File jsonFile = new File(def.packDirectory, "vehicle.json");
        try {
            JsonObject root;
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonObject es = root.has("engine_start") ? root.getAsJsonObject("engine_start") : new JsonObject();
            es.addProperty("start_chance", chance);
            es.addProperty("crank_ticks", ticks);
            es.addProperty("fail_allowed", chance < 0.999);
            root.add("engine_start", es);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            VehiclePackManager.loadPacks();
            source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0441\u0442\u0430\u0440\u0442\u0435\u0440\u0430 \u0434\u043b\u044f " + packId + " \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u044b! \u0428\u0430\u043d\u0441: " + (int)(chance*100) + "%, \u0422\u0438\u043a\u0438: " + ticks), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041e\u0448\u0438\u0431\u043a\u0430: " + e.getMessage()));
            return 0;
        }
    }

    private static JsonArray createVec3Arr(double x, double y, double z) {
        JsonArray arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }

    private static String formatDisplayName(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private static int createTemplate(CommandSourceStack source, String id, String category) {
        String safeId = id.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        File packsDir = VehiclePackManager.getPacksDir();
        File packDir = new File(packsDir, safeId);
        if (packDir.exists()) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041f\u0430\u043f\u043a\u0430 '" + safeId + "' \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442 \u0432 cardecor_packs!"));
            return 0;
        }

        try {
            File vehiclesDir = new File(packDir, "vehicles");
            File modelsDir = new File(packDir, "assets/" + safeId + "/models");
            File texturesDir = new File(packDir, "assets/" + safeId + "/textures/vehicle");

            vehiclesDir.mkdirs();
            modelsDir.mkdirs();
            texturesDir.mkdirs();

            VehiclePackManager.ensurePackMcmeta(packDir);

            JsonObject root = new JsonObject();
            root.addProperty("id", safeId);
            root.addProperty("display_name", formatDisplayName(safeId));
            boolean isPickup = "pickup".equalsIgnoreCase(category) || "truck".equalsIgnoreCase(category);
            root.addProperty("category", isPickup ? "pickup" : "sedan");
            root.addProperty("scale", 1.0);
            root.addProperty("ground_offset_y", 0.0);

            JsonObject models = new JsonObject();
            models.addProperty("chassis", "assets/" + safeId + "/models/" + safeId + "_chassis.obj");
            models.addProperty("wheel", "assets/" + safeId + "/models/wheel.obj");
            models.addProperty("steering_wheel", "assets/" + safeId + "/models/steering_wheel.obj");
            if (isPickup) {
                models.addProperty("spare_wheel", "assets/" + safeId + "/models/spare_wheel.obj");
            }
            root.add("models", models);

            JsonObject textures = new JsonObject();
            textures.addProperty("default", "assets/" + safeId + "/textures/vehicle/" + safeId + ".png");
            root.add("textures", textures);

            JsonObject physics = new JsonObject();
            physics.addProperty("mass", isPickup ? 1650.0 : 1350.0);
            physics.addProperty("top_speed", isPickup ? 115.0 : 140.0);
            physics.addProperty("horsepower", isPickup ? 120.0 : 150.0);
            physics.addProperty("drivetrain", isPickup ? "4WD" : "RWD");
            physics.addProperty("wheelbase", isPickup ? 2.9 : 2.7);
            physics.addProperty("turning_radius", 5.5);
            physics.addProperty("suspension_stiffness", 22.0);
            root.add("physics", physics);

            JsonObject wheels = new JsonObject();
            wheels.addProperty("radius", 0.42);
            wheels.addProperty("width", 0.32);
            wheels.add("front_left", createVec3Arr(-0.90, -0.42, 1.35));
            wheels.add("front_right", createVec3Arr(0.90, -0.42, 1.35));
            wheels.add("rear_left", createVec3Arr(-0.90, -0.42, -1.35));
            wheels.add("rear_right", createVec3Arr(0.90, -0.42, -1.35));
            root.add("wheels", wheels);

            if (isPickup) {
                root.add("spare_wheel_pos", createVec3Arr(0.0, 0.45, -1.85));
            }

            JsonArray seats = new JsonArray();
            JsonObject driverSeat = new JsonObject();
            driverSeat.add("pos", createVec3Arr(-0.42, 0.25, 0.20));
            driverSeat.addProperty("door_name", "\u0414\u0432\u0435\u0440\u044c \u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044f");
            seats.add(driverSeat);

            JsonObject passSeat = new JsonObject();
            passSeat.add("pos", createVec3Arr(0.42, 0.25, 0.20));
            passSeat.addProperty("door_name", "\u041f\u0430\u0441\u0441\u0430\u0436\u0438\u0440\u0441\u043a\u0430\u044f \u0434\u0432\u0435\u0440\u044c");
            seats.add(passSeat);

            if (isPickup) {
                JsonObject bedSeat = new JsonObject();
                bedSeat.add("pos", createVec3Arr(0.0, 0.30, -1.20));
                bedSeat.addProperty("door_name", "\u041a\u0443\u0437\u043e\u0432");
                bedSeat.addProperty("is_cargo_bed", true);
                seats.add(bedSeat);
            }
            root.add("seats", seats);

            JsonObject sounds = new JsonObject();
            sounds.addProperty("engine_type", "i6");
            sounds.addProperty("idle_pitch", 1.0);
            sounds.addProperty("max_pitch", 1.25);
            root.add("sounds", sounds);

            File vehicleJson = new File(vehiclesDir, safeId + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(vehicleJson), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            VehiclePackManager.loadPacks();

            source.sendSuccess(() -> Component.literal("\u00a7a[CarDecor] \u0423\u0441\u043f\u0435\u0448\u043d\u043e \u0441\u043e\u0437\u0434\u0430\u043d \u0448\u0430\u0431\u043b\u043e\u043d \u0430\u0434\u0434\u043e\u043d\u0430: \u00a7e" + safeId), true);
            source.sendSuccess(() -> Component.literal("\u00a77\u041f\u0443\u0442\u044c: cardecor_packs/" + safeId + "/"), false);
            source.sendSuccess(() -> Component.literal("\u00a771. \u041f\u043e\u043c\u0435\u0441\u0442\u0438\u0442\u0435 \u043c\u043e\u0434\u0435\u043b\u044c \u0432 assets/" + safeId + "/models/"), false);
            source.sendSuccess(() -> Component.literal("\u00a772. \u041f\u043e\u043c\u0435\u0441\u0442\u0438\u0442\u0435 \u0442\u0435\u043a\u0441\u0442\u0443\u0440\u0443 \u0432 assets/" + safeId + "/textures/vehicle/" + safeId + ".png"), false);
            source.sendSuccess(() -> Component.literal("\u00a773. \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u0442\u0435 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b \u0432 vehicles/" + safeId + ".json"), false);
            source.sendSuccess(() -> Component.literal("\u00a7a\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 /car reload \u0434\u043b\u044f \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f \u0431\u0435\u0437 \u043f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u043a\u0430!"), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[CarDecor] \u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0438 \u0448\u0430\u0431\u043b\u043e\u043d\u0430: " + e.getMessage()));
            return 0;
        }
    }

    private static int validatePacks(CommandSourceStack source, String targetPack) {
        Collection<VehicleDefinition> packsToValidate;
        if (targetPack != null) {
            VehicleDefinition def = VehiclePackManager.get(targetPack);
            if (def == null) {
                source.sendFailure(Component.literal("\u00a7c[CarDecor] \u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u044c '" + targetPack + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0432 \u0440\u0435\u0435\u0441\u0442\u0440\u0435!"));
                return 0;
            }
            packsToValidate = Collections.singletonList(def);
        } else {
            packsToValidate = VehiclePackManager.getAll();
        }

        if (packsToValidate.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7e[CarDecor] \u041d\u0435\u0442 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u043d\u044b\u0445 \u0430\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u0435\u0439 \u0434\u043b\u044f \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0438!"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("\u00a76--- [CarDecor] \u0412\u0430\u043b\u0438\u0434\u0430\u0446\u0438\u044f \u0430\u0434\u0434\u043e\u043d\u043e\u0432 (" + packsToValidate.size() + ") ---"), false);

        int totalErrors = 0;
        for (VehicleDefinition def : packsToValidate) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (def.packDirectory == null || !def.packDirectory.exists()) {
                errors.add("\u0414\u0438\u0440\u0435\u043a\u0442\u043e\u0440\u0438\u044f \u043f\u0430\u043a\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430 \u043d\u0430 \u0434\u0438\u0441\u043a\u0435");
            } else {
                File dir = def.packDirectory;
                if (def.models == null || def.models.chassis == null || def.models.chassis.isEmpty()) {
                    errors.add("\u041d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d\u0430 \u043c\u043e\u0434\u0435\u043b\u044c \u0448\u0430\u0441\u0441\u0438 (models.chassis)");
                } else if (!new File(dir, def.models.chassis).exists()) {
                    warnings.add("\u0424\u0430\u0439\u043b \u043c\u043e\u0434\u0435\u043b\u0448\u0430\u0441\u0441\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + def.models.chassis);
                }

                if (def.models != null && def.models.wheel != null && !def.models.wheel.isEmpty()) {
                    if (!new File(dir, def.models.wheel).exists()) {
                        warnings.add("\u0424\u0430\u0439\u043b \u043c\u043e\u0434\u0435\u043b\u0438 \u043a\u043e\u043b\u0435\u0441\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + def.models.wheel);
                    }
                }

                if (def.textures == null || def.textures.chassis == null || def.textures.chassis.isEmpty()) {
                    warnings.add("\u041d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d\u0430 \u0442\u0435\u043a\u0441\u0442\u0443\u0440\u0430 (textures.chassis)");
                } else if (!new File(dir, def.textures.chassis).exists()) {
                    warnings.add("\u0424\u0430\u0439\u043b \u0442\u0435\u043a\u0441\u0442\u0443\u0440\u044b \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + def.textures.chassis);
                }

                if (def.wheels == null) {
                    errors.add("\u0421\u0435\u043a\u0446\u0438\u044f wheels \u043e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442");
                } else {
                    if (def.wheels.radius <= 0.05) errors.add("\u0420\u0430\u0434\u0438\u0443\u0441 \u043a\u043e\u043b\u0435\u0441 wheels.radius <= 0.05");
                    if (def.wheels.frontLeft == null || def.wheels.frontRight == null || def.wheels.rearLeft == null || def.wheels.rearRight == null) {
                        errors.add("\u041d\u0435 \u0432\u0441\u0435 4 \u043f\u043e\u0437\u0438\u0446\u0438\u0438 \u043a\u043e\u043b\u0435\u0441 (front_left, front_right, rear_left, rear_right) \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0435\u043d\u044b");
                    }
                }

                if (def.seats == null || def.seats.isEmpty()) {
                    errors.add("\u041d\u0435\u0442 \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0435\u043d\u043d\u044b\u0445 \u043c\u0435\u0441\u0442 (seats[])");
                } else {
                    if (def.seats.get(0).pos == null || def.seats.get(0).pos.length < 3) {
                        errors.add("\u041c\u0435\u0441\u0442\u043e \u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044f (seats[0]) \u043d\u0435 \u0438\u043c\u0435\u0435\u0442 \u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u043e\u0439 \u043f\u043e\u0437\u0438\u0446\u0438\u0438 pos: [x,y,z]");
                    }
                }

                if (def.physics != null) {
                    if (def.physics.maxSpeed <= 10.0) warnings.add("max_speed \u043e\u0447\u0435\u043d\u044c \u043d\u0438\u0437\u043a\u0430\u044f: " + def.physics.maxSpeed);
                    if (def.physics.horsepower <= 10.0) warnings.add("horsepower \u043e\u0447\u0435\u043d\u044c \u043d\u0438\u0437\u043a\u0430\u044f: " + def.physics.horsepower);
                }
            }

            if (errors.isEmpty() && warnings.isEmpty()) {
                source.sendSuccess(() -> Component.literal("\u00a7a\u2714 [" + def.id + "] \u0412\u0430\u043b\u0438\u0434\u0430\u0446\u0438\u044f \u0443\u0441\u043f\u0435\u0448\u043d\u0430! \u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b \u0438 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b \u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b."), false);
            } else {
                totalErrors += errors.size();
                source.sendSuccess(() -> Component.literal((errors.isEmpty() ? "\u00a7e\u26a0 [" : "\u00a7c\u2716 [") + def.id + "] \u041e\u0448\u0438\u0431\u043e\u043a: " + errors.size() + ", \u041f\u0440\u0435\u0434\u0443\u043f\u0440\u0435\u0436\u0434\u0435\u043d\u0438\u0439: " + warnings.size()), false);
                for (String err : errors) {
                    source.sendSuccess(() -> Component.literal("  \u00a7c\u2716 " + err), false);
                }
                for (String warn : warnings) {
                    source.sendSuccess(() -> Component.literal("  \u00a7e\u26a0 " + warn), false);
                }
            }
        }

        int finalTotal = totalErrors;
        source.sendSuccess(() -> Component.literal(finalTotal == 0 ? "\u00a7a\u2714 \u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430: \u043a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043e\u0448\u0438\u0431\u043e\u043a \u043d\u0435\u0442!" : "\u00a7c\u2716 \u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430: \u043d\u0430\u0439\u0434\u0435\u043d\u043e " + finalTotal + " \u043a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043e\u0448\u0438\u0431\u043e\u043a!"), false);
        return 1;
    }
}
