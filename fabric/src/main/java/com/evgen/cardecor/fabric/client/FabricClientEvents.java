package com.evgen.cardecor.fabric.client;

import com.evgen.cardecor.client.ModKeyBindings;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.network.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class FabricClientEvents {
    private static int lastFlags = -1;
    private static int lastHornTick = 0;
    private static boolean lastGasPressed = false;
    private static boolean lastBrakePressed = false;
    private static int lastDriverGear = 0;
    private static int spaceHoldTicks = 0;
    private static com.evgen.cardecor.client.VehicleCabinRainSoundInstance cabinRainSound = null;
    private static com.evgen.cardecor.client.VehicleCabinHeaterSoundInstance cabinHeaterSound = null;
    private static int lastVehicleId = -1;
    private static float lastVehicleVisualYaw = Float.NaN;
    private static int lastDirectGearKey = -99;
    private static int shiftUpLatchTicks = 0;
    private static int shiftDownLatchTicks = 0;
    private static boolean lastShiftUpKeyDown = false;
    private static boolean lastShiftDownKeyDown = false;
    private static final int SHIFT_LATCH_TICKS = 4;

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.player == null) {
                lastVehicleId = -1;
                lastVehicleVisualYaw = Float.NaN;
                return;
            }

            if (mc.screen == null) {
                if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        while (mc.options.keyInventory.consumeClick()) {
                            handleSeatSwitchKey(mc, vehicle);
                        }
                    }
                } else {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        DrivableVehicleEntity.WheelTarget targetWheel = DrivableVehicleEntity.findTargetedWheel(mc.player);
                        if (targetWheel != null) {
                            while (mc.options.keyInventory.consumeClick()) {
                                handleWheelAction(mc, targetWheel);
                                return;
                            }
                        }
                    }

                    DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                    if (targetDoor != null) {
                        while (mc.options.keyInventory.consumeClick()) {
                            handleDoorEnter(mc, targetDoor);
                            return;
                        }
                    }

                    DrivableVehicleEntity.HoodTarget targetHood = DrivableVehicleEntity.findTargetedHood(mc.player);
                    if (targetHood != null) {
                        while (mc.options.keyInventory.consumeClick()) {
                            targetHood.vehicle.openMechanicsMenu(mc.player);
                            return;
                        }
                        while (mc.options.keyUse.consumeClick()) {
                            ItemStack held = mc.player.getMainHandItem();
                            if (!held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get()) &&
                                !held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) &&
                                !held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                                targetHood.vehicle.openMechanicsMenu(mc.player);
                                return;
                            }
                        }
                    }
                }
            } else if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
                if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        mc.setScreen(null);
                        handleSeatSwitchKey(mc, vehicle);
                    }
                } else {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        DrivableVehicleEntity.WheelTarget targetWheel = DrivableVehicleEntity.findTargetedWheel(mc.player);
                        if (targetWheel != null) {
                            mc.setScreen(null);
                            handleWheelAction(mc, targetWheel);
                            return;
                        }
                    }

                    DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                    if (targetDoor != null) {
                        mc.setScreen(null);
                        handleDoorEnter(mc, targetDoor);
                        return;
                    }

                    DrivableVehicleEntity.HoodTarget targetHood = DrivableVehicleEntity.findTargetedHood(mc.player);
                    if (targetHood != null) {
                        mc.setScreen(null);
                        targetHood.vehicle.openMechanicsMenu(mc.player);
                        return;
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            com.evgen.cardecor.pack.VehiclePackManager.checkHotReload();
            com.evgen.cardecor.client.VehicleCabinWeatherFilter.tickRainFilter(mc);

            // Intercept if inventory was opened during tick by vanilla handleKeyBinds
            if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
                if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        mc.setScreen(null);
                        handleSeatSwitchKey(mc, vehicle);
                    }
                } else {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        DrivableVehicleEntity.WheelTarget targetWheel = DrivableVehicleEntity.findTargetedWheel(mc.player);
                        if (targetWheel != null) {
                            mc.setScreen(null);
                            handleWheelAction(mc, targetWheel);
                            return;
                        }
                    }

                    DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                    if (targetDoor != null) {
                        mc.setScreen(null);
                        handleDoorEnter(mc, targetDoor);
                        return;
                    }

                    DrivableVehicleEntity.HoodTarget targetHood = DrivableVehicleEntity.findTargetedHood(mc.player);
                    if (targetHood != null) {
                        mc.setScreen(null);
                        targetHood.vehicle.openMechanicsMenu(mc.player);
                        return;
                    }
                }
            }

            if (mc.screen == null && ModKeyBindings.KEY_RADIAL_MENU.consumeClick()) {
                DrivableVehicleEntity targetVehicle = getTargetOrNearbyVehicle(mc);
                if (targetVehicle != null) {
                    mc.setScreen(new com.evgen.cardecor.client.gui.VehicleRadialMenuScreen(targetVehicle));
                    return;
                }
            }

            if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                int seat = vehicle.getSeatForPlayer(mc.player);
                boolean inCabin = !vehicle.isCargoBedSeat(seat);
                net.minecraft.core.BlockPos pos = vehicle.blockPosition();
                net.minecraft.world.level.biome.Biome biome = vehicle.level().getBiome(pos).value();
                boolean isSnow = biome.coldEnoughToSnow(pos) || biome.getPrecipitationAt(pos) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
                boolean raining = !isSnow && vehicle.level().isRaining()
                    && (vehicle.level().canSeeSky(pos.above(2))
                        || vehicle.level().isRainingAt(pos));
                if (inCabin && raining) {
                    if (cabinRainSound == null || !mc.getSoundManager().isActive(cabinRainSound)) {
                        cabinRainSound = new com.evgen.cardecor.client.VehicleCabinRainSoundInstance(vehicle);
                        mc.getSoundManager().play(cabinRainSound);
                    }
                }

                byte climate = vehicle.getClimateMode();
                boolean heaterActive = vehicle.isEngineOn() && (climate == 1);
                if (inCabin && heaterActive) {
                    if (cabinHeaterSound == null || !mc.getSoundManager().isActive(cabinHeaterSound)) {
                        cabinHeaterSound = new com.evgen.cardecor.client.VehicleCabinHeaterSoundInstance(vehicle);
                        mc.getSoundManager().play(cabinHeaterSound);
                    }
                }

                if (inCabin) {
                    mc.player.setTicksFrozen(0);
                    // Suppress Tough As Nails freezing/hyperthermia on client
                    try {
                        Class<?> thClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
                        Object tempData = thClass.getMethod("getTemperatureData", net.minecraft.world.entity.player.Player.class).invoke(null, mc.player);
                        if (tempData != null) {
                            Class<?> tlClass = Class.forName("toughasnails.api.temperature.TemperatureLevel");
                            @SuppressWarnings("unchecked")
                            Object neutral = Enum.valueOf((Class<Enum>) tlClass, "NEUTRAL");
                            tempData.getClass().getMethod("setLevel", tlClass).invoke(tempData, neutral);
                            tempData.getClass().getMethod("setTargetLevel", tlClass).invoke(tempData, neutral);
                        }
                        thClass.getMethod("setTicksHyperthermic", net.minecraft.world.entity.player.Player.class, int.class).invoke(null, mc.player, 0);
                    } catch (Throwable ignored) {}

                    // Suppress EnvironmentZ cold veil on client
                    try {
                        Class<?> ezClient = Class.forName("net.environmentz.EnvironmentZClient");
                        java.lang.reflect.Field field = ezClient.getField("smoothFreezingRendering");
                        field.setFloat(null, 0.0f);
                    } catch (Throwable ignored) {}
                    try {
                        Class<?> paClass = Class.forName("net.environmentz.access.PlayerAccess");
                        if (paClass.isInstance(mc.player)) {
                            paClass.getMethod("setPlayerColdProtectionAmount", int.class).invoke(mc.player, 100);
                        }
                    } catch (Throwable ignored) {}
                }

                // Cancel vanilla shift dismount
                mc.player.input.shiftKeyDown = false;

                // Dismount key (KEY_DISMOUNT, default U)
                if (mc.screen == null) {
                    boolean dismountPressed = ModKeyBindings.KEY_DISMOUNT.consumeClick();
                    if (dismountPressed) {
                        ModNetwork.sendToServer(new VehicleDismountPacket(vehicle.getId()));
                        mc.player.stopRiding();
                        return;
                    }

                    if (ModKeyBindings.KEY_SWITCH_SEAT.consumeClick()) {
                        ModNetwork.sendToServer(new VehicleChangeSeatPacket(vehicle.getId(), -1));
                    }
                }

                if (mc.screen == null && ModKeyBindings.KEY_SOUND_SETTINGS.consumeClick()) {
                    mc.setScreen(new com.evgen.cardecor.client.gui.VehicleSoundSettingsScreen(null));
                }

                if (mc.screen == null) {
                    while (ModKeyBindings.KEY_CABIN_LIGHT.consumeClick()) {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_CABIN_LIGHT));
                    }
                }

                int mySeat = vehicle.getSeatForPlayer(mc.player);
                int flags = 0;

                // In-vehicle seat switching: hold Ctrl and aim at a seat to switch
                long winCtrl = mc.getWindow().getWindow();
                boolean ctrlHeld = InputConstants.isKeyDown(winCtrl, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(winCtrl, GLFW.GLFW_KEY_RIGHT_CONTROL);
                if (ctrlHeld && mc.screen == null) {
                    com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(vehicle.getVehicleId());
                    int maxSeats = (def != null && def.seats != null) ? def.seats.size() : vehicle.getMaxSeats();
                    Vec3 eye = mc.player.getEyePosition();
                    Vec3 look = mc.player.getViewVector(1.0F);

                    int targetedSeat = -1;
                    double bestDist = 0.85;

                    for (int s = 0; s < maxSeats; s++) {
                        if (s == mySeat) continue;
                        Vec3 seatPos = vehicle.getSeatWorldPosition(s);
                        for (double dy = 0.15; dy <= 0.75; dy += 0.30) {
                            Vec3 p = seatPos.add(0.0, dy, 0.0);
                            Vec3 toP = p.subtract(eye);
                            double distAlongRay = toP.dot(look);
                            if (distAlongRay > 0.1 && distAlongRay < 3.5) {
                                Vec3 proj = eye.add(look.scale(distAlongRay));
                                double d = proj.distanceTo(p);
                                if (d < bestDist) {
                                    bestDist = d;
                                    targetedSeat = s;
                                }
                            }
                        }
                    }

                    if (targetedSeat >= 0) {
                        byte seatItemBits = vehicle.getEntityData().get(DrivableVehicleEntity.SEAT_ITEM_STATUS);
                        boolean blockedByItems = (seatItemBits & (1 << targetedSeat)) != 0;
                        boolean occupiedByPlayer = vehicle.isSeatOccupiedByPassenger(targetedSeat);
                        String sName = (def != null && def.seats != null && targetedSeat < def.seats.size() && def.seats.get(targetedSeat).name != null)
                            ? def.seats.get(targetedSeat).name : ("Место " + (targetedSeat + 1));

                        if (blockedByItems) {
                            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] На сиденье лежат вещи!"), true);
                        } else if (occupiedByPlayer) {
                            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] Место уже занято!"), true);
                        } else {
                            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[Ctrl + E] Пересесть: " + sName), true);
                        }
                    }
                }

                if (mySeat == 0 && mc.screen == null) {
                    boolean gasDown = mc.options.keyUp.isDown();
                    boolean brakeDown = mc.options.keyDown.isDown();

                    if (gasDown && !lastGasPressed) {
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            com.evgen.cardecor.init.ModSounds.PEDAL_PRESS.get(), 1.0F, 0.8F));
                    } else if (!gasDown && lastGasPressed) {
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            com.evgen.cardecor.init.ModSounds.PEDAL_RELEASE.get(), 1.0F, 0.8F));
                    }
                    lastGasPressed = gasDown;

                    if (brakeDown && !lastBrakePressed) {
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            com.evgen.cardecor.init.ModSounds.BRAKE_PEDAL.get(), 1.0F, 0.8F));
                    }
                    lastBrakePressed = brakeDown;

                    if (gasDown)    flags |= 1;
                    if (brakeDown)  flags |= 2;
                    if (mc.options.keyLeft.isDown())  flags |= 4;
                    if (mc.options.keyRight.isDown()) flags |= 8;
                    if (mc.options.keyJump.isDown() || ModKeyBindings.KEY_HANDBRAKE.isDown())  flags |= 16;

                    while (ModKeyBindings.KEY_START_ENGINE.consumeClick()) {
                        flags |= 32;
                    }
                    long winKeys = mc.getWindow().getWindow();
                    boolean clutchDown = ModKeyBindings.KEY_CLUTCH.isDown()
                            || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_LEFT_ALT)
                            || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_RIGHT_ALT);
                    if (clutchDown) {
                        flags |= 256;
                    }

                    boolean directTriggeredThisTick = false;
                    // Прямой выбор передачи через Alt + 0..6
                    if (clutchDown) {
                        int directTarget = -99;
                        if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_GRAVE_ACCENT) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_9) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_SUBTRACT)) directTarget = -1;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_0) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_0)) directTarget = 0;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_1) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_1)) directTarget = 1;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_2) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_2)) directTarget = 2;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_3) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_3)) directTarget = 3;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_4) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_4)) directTarget = 4;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_5) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_5)) directTarget = 5;
                        else if (InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_6) || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_KP_6)) directTarget = 6;

                        if (directTarget != -99) {
                            if (directTarget != lastDirectGearKey) {
                                lastDirectGearKey = directTarget;
                                int gearCode = directTarget + 2;
                                flags |= (gearCode << 16);
                                directTriggeredThisTick = true;
                            }
                        } else {
                            lastDirectGearKey = -99;
                        }
                    } else {
                        lastDirectGearKey = -99;
                    }

                    boolean shiftUpClicked = false;
                    while (ModKeyBindings.KEY_SHIFT_UP.consumeClick()) {
                        shiftUpClicked = true;
                    }
                    boolean shiftDownClicked = false;
                    while (ModKeyBindings.KEY_SHIFT_DOWN.consumeClick()) {
                        shiftDownClicked = true;
                    }

                    // Прямой опрос клавиш через GLFW для 100% надежного переключения при любом зажатом Alt
                    boolean rawUp = InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_R);
                    if (rawUp && !lastShiftUpKeyDown) {
                        shiftUpClicked = true;
                    }
                    lastShiftUpKeyDown = rawUp;

                    boolean rawDown = InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_F);
                    if (rawDown && !lastShiftDownKeyDown) {
                        shiftDownClicked = true;
                    }
                    lastShiftDownKeyDown = rawDown;

                    if (shiftUpClicked) {
                        shiftUpLatchTicks = SHIFT_LATCH_TICKS;
                    }
                    if (shiftDownClicked) {
                        shiftDownLatchTicks = SHIFT_LATCH_TICKS;
                    }

                    // Передача переключается четко и надежно (хоть с зажатым Alt, хоть без него!)
                    if (shiftUpLatchTicks > 0) {
                        if (!directTriggeredThisTick) {
                            flags |= 512;
                        }
                        shiftUpLatchTicks--;
                    }

                    if (shiftDownLatchTicks > 0) {
                        if (!directTriggeredThisTick) {
                            flags |= 1024;
                        }
                        shiftDownLatchTicks--;
                    }

                    while (ModKeyBindings.KEY_HEADLIGHTS.consumeClick()) {
                        ModNetwork.sendToServer(new VehicleHeadlightPacket(vehicle.getId()));
                    }
                    while (ModKeyBindings.KEY_PARKING_BRAKE.consumeClick()) {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_PARKING_BRAKE));
                    }

                    float curSpeedKmh = Math.abs(vehicle.getSpeed());
                    boolean spaceDown = mc.options.keyJump.isDown() || ModKeyBindings.KEY_HANDBRAKE.isDown();
                    if (spaceDown && curSpeedKmh < 2.0F) {
                        spaceHoldTicks++;
                        if (spaceHoldTicks == 14) {
                            ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_PARKING_BRAKE));
                        }
                    } else if (!spaceDown) {
                        spaceHoldTicks = 0;
                    }

                    long win = mc.getWindow().getWindow();
                    boolean hornPressed = ModKeyBindings.KEY_HORN.isDown() || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_H) || ModKeyBindings.KEY_HORN.consumeClick();
                    if (hornPressed && (mc.player.tickCount - lastHornTick >= 28)) {
                        lastHornTick = mc.player.tickCount;
                        ModNetwork.sendToServer(new VehicleHornPacket(vehicle.getId()));
                    }

                    boolean leftShift = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_SHIFT);
                    boolean rightShift = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_SHIFT);
                    boolean sprintKey = mc.options.keySprint.isDown();
                    boolean sneakKey = mc.options.keyShift.isDown();

                    boolean boostActive = leftShift || rightShift || sprintKey || sneakKey || ModKeyBindings.KEY_BOOST.isDown();
                    if (boostActive && mc.options.keyUp.isDown()) {
                        flags |= 64; // BOOST flag
                    }
                }

                if (mySeat == 0) {
                    vehicle.clientLocalDriver = true;
                    vehicle.inputFlags = flags;
                    if (flags != lastFlags || flags != 0) {
                        ModNetwork.sendToServer(new VehicleInputPacket(vehicle.getId(), flags));
                        lastFlags = flags;
                    }
                } else {
                    vehicle.clientLocalDriver = false;
                }
            } else {
                lastFlags = -1;
                lastGasPressed = false;
                lastBrakePressed = false;
                lastDriverGear = 0;
            }
        });

        WorldRenderEvents.START.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                lastVehicleVisualYaw = Float.NaN;
                lastVehicleId = -1;
                return;
            }
            if (!(mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle)) {
                lastVehicleVisualYaw = Float.NaN;
                lastVehicleId = -1;
                return;
            }
            float partialTicks = context.tickDelta();
            float carVisualYaw = net.minecraft.util.Mth.rotLerp(partialTicks, vehicle.yRotO, vehicle.getYRot());
            if (lastVehicleId != vehicle.getId() || Float.isNaN(lastVehicleVisualYaw)) {
                lastVehicleId = vehicle.getId();
                lastVehicleVisualYaw = carVisualYaw;
            } else {
                float deltaCarYaw = net.minecraft.util.Mth.wrapDegrees(carVisualYaw - lastVehicleVisualYaw);
                lastVehicleVisualYaw = carVisualYaw;
                if (Math.abs(deltaCarYaw) > 0.0001F && Math.abs(deltaCarYaw) < 65.0F) {
                    mc.player.setYRot(mc.player.getYRot() + deltaCarYaw);
                    mc.player.yRotO += deltaCarYaw;
                    mc.player.setYHeadRot(mc.player.getYHeadRot() + deltaCarYaw);
                    mc.player.yHeadRotO += deltaCarYaw;
                }
            }
        });

        WorldRenderEvents.END.register(context -> {
            org.joml.Matrix4f modelView = (context.matrixStack() != null)
                ? context.matrixStack().last().pose() : null;
            com.evgen.cardecor.client.render.DeferredHeadlightRenderer.render(
                context.camera(), context.projectionMatrix(), modelView, context.tickDelta()
            );
        });

        // Speedometer & Door Prompt HUD
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // Vehicle interaction HUD prompts when outside
            if (mc.player.getVehicle() == null && mc.screen == null) {
                long win = mc.getWindow().getWindow();
                boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);

                // 1. Wheel removal prompt ONLY when holding Ctrl
                if (ctrlHeld) {
                    DrivableVehicleEntity.WheelTarget targetWheel = DrivableVehicleEntity.findTargetedWheel(mc.player);
                    if (targetWheel != null) {
                        String prompt = "§e[E] §fСнять колесо: " + targetWheel.wheelName;
                        guiGraphics.drawCenteredString(mc.font, prompt, screenWidth / 2, screenHeight / 2 + 14, 0xFFFFFFFF);
                        return;
                    }
                }

                // 2. Sedan Trunk interaction prompt
                DrivableVehicleEntity.TrunkTarget targetTrunk = DrivableVehicleEntity.findTargetedTrunk(mc.player);
                if (targetTrunk != null) {
                    String prompt = targetTrunk.isLocked ? "§c[Заперто] Багажник заперт" : "§e[ПКМ] §fОткрыть багажник";
                    guiGraphics.drawCenteredString(mc.font, prompt, screenWidth / 2, screenHeight / 2 + 14, 0xFFFFFFFF);
                    return;
                }

                // 3. Doors & Pickup bed prompt (Doors always take strict priority!)
                DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                if (targetDoor != null) {
                    boolean isPickupBed = targetDoor.vehicle.isCargoBedSeat(targetDoor.seatIndex);

                    String prompt;
                    if (isPickupBed) {
                        if (targetDoor.isLocked) {
                            prompt = "§c[Заперто] Багажник заперт";
                        } else if (targetDoor.isBlockedByItems) {
                            prompt = "§c[E] Заблокировано вещами §7| §e[ПКМ] §fОткрыть багажник";
                        } else if (targetDoor.isOccupied) {
                            prompt = "§e[E] Место занято §7| §e[ПКМ] §fОткрыть багажник";
                        } else {
                            prompt = "§e[E] §fСесть в кузов §7| §e[ПКМ] §fОткрыть багажник";
                        }
                    } else {
                        prompt = targetDoor.isLocked ? "§c[E] Заперто" :
                                        targetDoor.isBlockedByItems ? "§c[E] Заблокировано вещами" :
                                        targetDoor.isOccupied ? "§e[E] Место занято" :
                                        "§e[E] §f" + targetDoor.doorName + " §7(Сесть)";
                    }
                    guiGraphics.drawCenteredString(mc.font, prompt, screenWidth / 2, screenHeight / 2 + 14, 0xFFFFFFFF);
                    return;
                }

                // 4. Hood / Engine mechanics prompt (Only when not looking at doors)
                DrivableVehicleEntity.HoodTarget targetHood = DrivableVehicleEntity.findTargetedHood(mc.player);
                if (targetHood != null) {
                    ItemStack held = mc.player.getMainHandItem();
                    String prompt;
                    if (held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get())) {
                        prompt = "§e[E] §fКапот (Автомеханика) §7| §6[ПКМ] §fЗалить масло";
                    } else if (held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) || held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                        prompt = "§e[E] §fКапот (Автомеханика) §7| §6[ПКМ] §fЗалить охлаждающую жидкость";
                    } else {
                        prompt = "§e[E] §fКапот (Автомеханика)";
                    }
                    guiGraphics.drawCenteredString(mc.font, prompt, screenWidth / 2, screenHeight / 2 + 14, 0xFFFFFFFF);
                    return;
                }

                return;
            }

            if (!(mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle)) return;

            float speedVal = vehicle.getEntityData().get(DrivableVehicleEntity.SPEED);
            double speedKmh = Math.abs(speedVal);
            if (speedKmh < 0.1 && mc.player != null) {
                Vec3 m = vehicle.getDeltaMovement();
                speedKmh = Math.sqrt(m.x * m.x + m.z * m.z) * 72.0;
            }
            float rpm = vehicle.getEntityData().get(DrivableVehicleEntity.RPM);
            int gear = vehicle.getCurrentGear();
            String gearText = (gear <= 0) ? "R" : String.valueOf(gear);

            int hudW = 120;
            int hudH = 46;
            int hudX = screenWidth - hudW - 10;
            int hudY = screenHeight - hudH - 8;

            guiGraphics.fill(hudX, hudY, hudX + hudW, hudY + hudH, 0x90000000);
            guiGraphics.fill(hudX, hudY, hudX + hudW, hudY + 1, 0x60FFFFFF);

            String speedStr = String.format("%.0f", speedKmh) + " km/h";
            guiGraphics.drawString(mc.font, "§f" + speedStr, hudX + 4, hudY + 4, 0xFFFFFF, false);
            if (vehicle.isParkingBrakeOn()) {
                guiGraphics.drawString(mc.font, "§c(P)", hudX + 76, hudY + 4, 0xFFFF4444, false);
            }
            guiGraphics.drawString(mc.font, "§e[" + gearText + "]", hudX + 96, hudY + 4, 0xFFFF55, false);

            int rpmColor = (rpm > 6500) ? 0xFFFF3333 : (rpm > 4500) ? 0xFFFFAA00 : (rpm > 2500) ? 0xFF55FF55 : 0xFFAAAAAA;
            String rpmStr = String.format("%.0f", rpm) + " RPM";
            guiGraphics.drawString(mc.font, rpmStr, hudX + 4, hudY + 15, rpmColor, false);

            if (vehicle.physics.liveBoostPressure > 0.05) {
                String boostStr = String.format("§b⚡%.1f bar", vehicle.physics.liveBoostPressure);
                guiGraphics.drawString(mc.font, boostStr, hudX + 68, hudY + 15, 0x55FFFF, false);
            }

            float hp = vehicle.getVehicleHealth();
            float maxHp = DrivableVehicleEntity.MAX_HP;
            int barW = hudW - 8;
            int fillW = (int) (barW * Math.max(0.0F, Math.min(1.0F, hp / maxHp)));
            int barY = hudY + 26;
            guiGraphics.fill(hudX + 4, barY, hudX + 4 + barW, barY + 3, 0xFF2A2A2A);
            float ratio = hp / maxHp;
            int barColor = (ratio > 0.50F) ? 0xFF44BB44 : (ratio > 0.35F) ? 0xFFDDAA22 : (ratio > 0.15F) ? 0xFFEE6622 : 0xFFFF2222;
            guiGraphics.fill(hudX + 4, barY, hudX + 4 + fillW, barY + 3, barColor);

            float fuel = vehicle.getFuel();
            int fuelPercent = (int) ((fuel / DrivableVehicleEntity.MAX_FUEL) * 100.0F);
            int fuelColor = (fuelPercent > 45) ? 0xFFAAAA00 : (fuelPercent > 20) ? 0xFFFF7700 : 0xFFFF2222;
            guiGraphics.drawString(mc.font, "GAS: " + fuelPercent + "%", hudX + 4, hudY + 32, fuelColor, false);
        });
    }

    public static DrivableVehicleEntity getTargetOrNearbyVehicle(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        if (mc.player.getVehicle() instanceof DrivableVehicleEntity v) {
            return v;
        }
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
            if (ehr.getEntity() instanceof DrivableVehicleEntity v) return v;
            if (ehr.getEntity() instanceof com.evgen.cardecor.entity.VehiclePartEntity part) return part.getParent();
        }
        net.minecraft.world.phys.AABB box = mc.player.getBoundingBox().inflate(4.8);
        java.util.List<DrivableVehicleEntity> list = mc.level.getEntitiesOfClass(DrivableVehicleEntity.class, box);
        DrivableVehicleEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (DrivableVehicleEntity v : list) {
            double d = mc.player.distanceToSqr(v);
            if (d < closestDist) {
                closestDist = d;
                closest = v;
            }
        }
        return closest;
    }

    private static int lastDoorEnterTick = -1;
    private static int lastSeatSwitchTick = -1;
    private static int lastWheelActionTick = -1;

    private static void handleDoorEnter(Minecraft mc, DrivableVehicleEntity.DoorTarget targetDoor) {
        if (mc.player == null) return;
        if (mc.player.tickCount == lastDoorEnterTick) return;
        lastDoorEnterTick = mc.player.tickCount;

        if (targetDoor.isLocked) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Салон] Автомобиль заперт!"), true);
            mc.player.playSound(com.evgen.cardecor.init.ModSounds.CAR_DOOR_LOCKED_YANK.get(), 0.9F, 1.0F);
            return;
        }
        if (targetDoor.isBlockedByItems) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Салон] На этом сиденье лежат вещи!"), true);
            mc.player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
        } else if (targetDoor.isOccupied) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Салон] Это место уже занято!"), true);
            mc.player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
        } else {
            ModNetwork.sendToServer(new VehicleSeatActionPacket(
                targetDoor.vehicle.getId(), targetDoor.seatIndex, VehicleSeatActionPacket.ACTION_SIT
            ));
        }
    }

    private static void handleWheelAction(Minecraft mc, DrivableVehicleEntity.WheelTarget targetWheel) {
        if (mc.player == null) return;
        if (mc.player.tickCount == lastWheelActionTick) return;
        lastWheelActionTick = mc.player.tickCount;

        int act = targetWheel.isSpare ? VehicleSeatActionPacket.ACTION_REMOVE_SPARE_WHEEL : VehicleSeatActionPacket.ACTION_UNINSTALL_WHEEL;
        ModNetwork.sendToServer(new VehicleSeatActionPacket(targetWheel.vehicle.getId(), targetWheel.wheelIndex, act));
    }

    private static void handleSeatSwitchKey(Minecraft mc, DrivableVehicleEntity vehicle) {
        if (mc.player == null) return;
        if (mc.player.tickCount == lastSeatSwitchTick) return;
        lastSeatSwitchTick = mc.player.tickCount;
        int mySeat = vehicle.getSeatForPlayer(mc.player);
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(vehicle.getVehicleId());
        int maxSeats = (def != null && def.seats != null) ? def.seats.size() : vehicle.getMaxSeats();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1.0F);

        int targetedSeat = -1;
        double bestDist = 0.85;

        for (int s = 0; s < maxSeats; s++) {
            if (s == mySeat) continue;
            Vec3 seatPos = vehicle.getSeatWorldPosition(s);
            for (double dy = 0.15; dy <= 0.75; dy += 0.30) {
                Vec3 p = seatPos.add(0.0, dy, 0.0);
                Vec3 toP = p.subtract(eye);
                double distAlongRay = toP.dot(look);
                if (distAlongRay > 0.1 && distAlongRay < 3.5) {
                    Vec3 proj = eye.add(look.scale(distAlongRay));
                    double d = proj.distanceTo(p);
                    if (d < bestDist) {
                        bestDist = d;
                        targetedSeat = s;
                    }
                }
            }
        }

        if (targetedSeat >= 0) {
            byte seatItemBits = vehicle.getEntityData().get(DrivableVehicleEntity.SEAT_ITEM_STATUS);
            boolean blockedByItems = (seatItemBits & (1 << targetedSeat)) != 0;
            boolean occupiedByPlayer = vehicle.isSeatOccupiedByPassenger(targetedSeat);
            if (blockedByItems) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] На сиденье лежат вещи!"), true);
                mc.player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
            } else if (occupiedByPlayer) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] Место уже занято!"), true);
                mc.player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
            } else {
                ModNetwork.sendToServer(new VehicleChangeSeatPacket(vehicle.getId(), targetedSeat));
            }
        }
    }
}
