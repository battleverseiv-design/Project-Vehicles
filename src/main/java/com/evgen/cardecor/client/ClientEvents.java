package com.evgen.cardecor.client;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.event.ForgeEvents;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.network.VehicleChangeSeatPacket;
import com.evgen.cardecor.network.VehicleInputPacket;
import com.evgen.cardecor.network.VehicleSeatActionPacket;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CarDecorMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static int lastFlags = -1;
    private static int lastSentTick = 0;
    private static int lastHornTick = 0;
    private static int lastDirectGearKey = -99;
    private static int shiftUpLatchTicks = 0;
    private static int shiftDownLatchTicks = 0;
    private static boolean lastShiftUpKeyDown = false;
    private static boolean lastShiftDownKeyDown = false;
    private static final int SHIFT_LATCH_TICKS = 4;
    private static float cameraShakeDecay = 0.0F;
    private static float lastVehicleVisualYaw = Float.NaN;
    private static int lastVehicleId = -1;
    private static boolean lastGasPressed = false;
    private static boolean lastBrakePressed = false;
    private static int lastDriverGear = 0;
    private static int spaceHoldTicks = 0;
    private static VehicleCabinRainSoundInstance cabinRainSound = null;
    private static VehicleCabinHeaterSoundInstance cabinHeaterSound = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (event.phase == TickEvent.Phase.START) {
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
                                int act = targetWheel.isSpare ? VehicleSeatActionPacket.ACTION_REMOVE_SPARE_WHEEL : VehicleSeatActionPacket.ACTION_UNINSTALL_WHEEL;
                                ModNetwork.CHANNEL.sendToServer(new VehicleSeatActionPacket(targetWheel.vehicle.getId(), targetWheel.wheelIndex, act));
                            }
                            return;
                        }
                    }

                    DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                    if (targetDoor != null) {
                        while (mc.options.keyInventory.consumeClick()) {
                            handleDoorEnter(mc, targetDoor);
                        }
                        return;
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
            }
            return;
        }

        if (event.phase != TickEvent.Phase.END) return;
        com.evgen.cardecor.pack.VehiclePackManager.checkHotReload();
        VehicleCabinWeatherFilter.tickRainFilter(mc);

        if (mc.screen != null) return;

        if (ModKeyBindings.KEY_RADIAL_MENU.consumeClick()) {
            DrivableVehicleEntity targetVehicle = getTargetOrNearbyVehicle(mc);
            if (targetVehicle != null) {
                mc.setScreen(new com.evgen.cardecor.client.gui.VehicleRadialMenuScreen(targetVehicle));
                return;
            }
        }

        if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            int mySeat = vehicle.getSeatForPlayer(mc.player);
            boolean inCabin = !vehicle.isCargoBedSeat(mySeat);
            net.minecraft.core.BlockPos pos = vehicle.blockPosition();
            net.minecraft.world.level.biome.Biome biome = vehicle.level().getBiome(pos).value();
            boolean isSnow = biome.coldEnoughToSnow(pos) || biome.getPrecipitationAt(pos) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
            boolean raining = !isSnow && vehicle.level().isRaining()
                && (vehicle.level().canSeeSky(pos.above(2))
                    || vehicle.level().isRainingAt(pos));
            if (inCabin && raining) {
                if (cabinRainSound == null || !mc.getSoundManager().isActive(cabinRainSound)) {
                    cabinRainSound = new VehicleCabinRainSoundInstance(vehicle);
                    mc.getSoundManager().play(cabinRainSound);
                }
            }

            byte climate = vehicle.getClimateMode();
            boolean heaterActive = vehicle.isEngineOn() && (climate == 1);
            if (inCabin && heaterActive) {
                if (cabinHeaterSound == null || !mc.getSoundManager().isActive(cabinHeaterSound)) {
                    cabinHeaterSound = new VehicleCabinHeaterSoundInstance(vehicle);
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

            int flags = 0;

            if (mySeat == 0) {
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

                if (vehicle.getCurrentGear() != lastDriverGear) {
                    lastDriverGear = vehicle.getCurrentGear();
                    // UI clutch sound removed - 3D gear clunk handled by VehicleEngineSoundManager
                }

                if (gasDown)    flags |= 1;
                if (brakeDown)  flags |= 2;
                if (mc.options.keyLeft.isDown())  flags |= 4;
                if (mc.options.keyRight.isDown()) flags |= 8;
                if (mc.options.keyJump.isDown())  flags |= 16;

                while (ModKeyBindings.KEY_START_ENGINE.consumeClick()) {
                    flags |= 32;
                }
                long winKeys = mc.getWindow().getWindow();
                boolean clutchDown = ModKeyBindings.KEY_CLUTCH.isDown() || InputConstants.isKeyDown(winKeys, GLFW.GLFW_KEY_LEFT_ALT);
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
                    ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleHeadlightPacket(vehicle.getId()));
                }
                while (ModKeyBindings.KEY_CABIN_LIGHT.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleSeatActionPacket(vehicle.getId(), 0, com.evgen.cardecor.network.VehicleSeatActionPacket.ACTION_TOGGLE_CABIN_LIGHT));
                }
                while (ModKeyBindings.KEY_PARKING_BRAKE.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleSeatActionPacket(vehicle.getId(), 0, com.evgen.cardecor.network.VehicleSeatActionPacket.ACTION_TOGGLE_PARKING_BRAKE));
                }

                float curSpeedKmh = Math.abs(vehicle.getSpeed());
                boolean spaceDown = mc.options.keyJump.isDown() || ModKeyBindings.KEY_HANDBRAKE.isDown();
                if (spaceDown && curSpeedKmh < 2.0F) {
                    spaceHoldTicks++;
                    if (spaceHoldTicks == 14) {
                        ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleSeatActionPacket(vehicle.getId(), 0, com.evgen.cardecor.network.VehicleSeatActionPacket.ACTION_TOGGLE_PARKING_BRAKE));
                    }
                } else if (!spaceDown) {
                    spaceHoldTicks = 0;
                }

                long win = mc.getWindow().getWindow();
                boolean hornPressed = ModKeyBindings.KEY_HORN.isDown() || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_H) || ModKeyBindings.KEY_HORN.consumeClick();
                if (hornPressed && (mc.player.tickCount - lastHornTick >= 28)) {
                    lastHornTick = mc.player.tickCount;
                    ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleHornPacket(vehicle.getId()));
                }

                // PUBG-style Boost: Shift (Left/Right) OR Sprint Key while accelerating
                long window = mc.getWindow().getWindow();
                boolean leftShift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT);
                boolean rightShift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
                boolean sprintKey = mc.options.keySprint.isDown();
                boolean sneakKey = mc.options.keyShift.isDown();

                boolean boostActive = leftShift || rightShift || sprintKey || sneakKey;
                if (boostActive && mc.options.keyUp.isDown()) {
                    flags |= 64; // BOOST flag
                }
            } else {
                while (ModKeyBindings.KEY_START_ENGINE.consumeClick()) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "\u00a7c[\u041f\u0430\u0441\u0441\u0430\u0436\u0438\u0440] \u0423\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u043c\u0430\u0448\u0438\u043d\u043e\u0439 \u043c\u043e\u0436\u0435\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c!"), true);
                }
            }

            // In-vehicle seat switching: hold Ctrl and aim at a seat to switch
            long win = mc.getWindow().getWindow();
            boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (ctrlHeld && mc.screen == null) {
                VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
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

            // Suppress attack/use clicks for cab occupants holding guns
            if (mySeat == 0 || mySeat == 1) {
                if (ForgeEvents.isGunItem(mc.player.getMainHandItem()) || ForgeEvents.isGunItem(mc.player.getOffhandItem())) {
                    mc.options.keyAttack.setDown(false);
                    mc.options.keyUse.setDown(false);
                }
            }

            // Send continuous 20 TPS input packet to server for driver (Zero dropped inputs / Zero launch lag!)
            if (mySeat == 0) {
                vehicle.clientLocalDriver = true;
                vehicle.inputFlags = flags;
                if (flags != lastFlags || flags != 0) {
                    ModNetwork.CHANNEL.sendToServer(new VehicleInputPacket(vehicle.getId(), flags));
                    lastFlags = flags;
                }
            } else {
                vehicle.clientLocalDriver = false;
            }
        } else {
            lastFlags = -1;
            lastGasPressed = false;
            lastBrakePressed = false;
            lastVehicleVisualYaw = Float.NaN;
            lastVehicleId = -1;
        }
    }

    /**
     * Clean, compact, minimalist vehicle HUD (no stickers/emojis).
     */
    /**
     * Completely eliminates FOV pulsating / screen throbbing while riding in the vehicle.
     */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            float kmh = Math.abs(vehicle.getSpeed());
            float speedFov = Math.min(0.22F, (kmh / 180.0F) * 0.18F);

            // Impact head-jolt FOV punch (punchier, visceral impulse)
            float trauma = vehicle.getEntityData().get(DrivableVehicleEntity.IMPACT_TRAUMA);
            float impactFovJolt = -trauma * 0.055F;

            event.setNewFovModifier(Math.max(0.65F, 1.0F + speedFov + impactFovJolt));
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
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
        float partialTicks = (float) event.getPartialTick();

        // =========================================================================
        // IMMERSIVE VEHICLES STYLE CAMERA ROTATION (Follows vehicle turn smoothly)
        // =========================================================================
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

        double speed = Math.abs(vehicle.getSpeed());
        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
        double pitchFollowCfg = (def != null && def.camera != null) ? def.camera.pitchFollow : 0.70;
        double rollFollowCfg  = (def != null && def.camera != null) ? def.camera.rollFollow : 0.45;
        double corneringBankCfg = (def != null && def.camera != null) ? def.camera.corneringBank : 0.25;
        double lookAheadCfg   = (def != null && def.camera != null) ? def.camera.lookAhead : 0.10;
        double shakeMultCfg   = (def != null && def.camera != null) ? def.camera.shakeIntensity : 0.35;
        float shakeMult = (float) shakeMultCfg;

        long ticks = mc.player.tickCount;
        float time = (float) ticks + partialTicks;
        float kmh = (float) speed;

        float shakePitch = 0.0F;
        float shakeRoll = 0.0F;

        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        if (shakeMult > 0.001F) {
            // Engine Vibration & Road Bumpiness (Controlled by pack's camera settings)
            if (vehicle.isEngineOn()) {
                float rpm = vehicle.getEngineRPM();
                float idleFactor = (speed < 0.5) ? 0.22F : 0.08F;
                float engineVibe = (float) Math.sin(time * 1.6F) * idleFactor * (rpm / 3200.0F);
                shakePitch += engineVibe * 0.35F;
                if (isFirstPerson) {
                    shakeRoll += (float) Math.cos(time * 1.8F) * idleFactor * 0.25F;
                }
            }
            if (speed > 0.8) {
                float speedShake = (float) Math.min(1.0F, (kmh / 100.0F) * 0.35F);
                float bumpNoise = (float) (Math.sin(time * 3.2F) * Math.cos(time * 2.1F)) * speedShake;
                shakePitch += bumpNoise * 0.40F;
                if (isFirstPerson) {
                    shakeRoll += (float) Math.sin(time * 2.8F) * speedShake * 0.30F;
                }
            }
        }

        // =========================================================================
        // REALISTIC IMPACT TRAUMA CAMERA SHAKE (Hit mob, crash into wall, heavy landing)
        // =========================================================================
        float trauma = vehicle.getEntityData().get(DrivableVehicleEntity.IMPACT_TRAUMA);
        float traumaPitchKick = 0.0F;
        float traumaRollKick = 0.0F;
        if (trauma > 0.005F) {
            float traumaIntensity = isFirstPerson ? (trauma * 3.2F) : (trauma * 1.5F);
            traumaPitchKick = -(float) (Math.sin(time * 12.0) * traumaIntensity);
            if (isFirstPerson) {
                traumaRollKick = (float) (Math.cos(time * 10.0) * (traumaIntensity * 0.70F));
            }
        }

        // Dynamic Terrain Slope & Cornering Bank Tilt (Smart Camera)
        float smoothPitchRad = net.minecraft.util.Mth.lerp(partialTicks, vehicle.prevPitch, vehicle.currentPitch);
        float smoothRollRad  = net.minecraft.util.Mth.lerp(partialTicks, vehicle.prevRoll,  vehicle.currentRoll);
        float smoothSteerRad = net.minecraft.util.Mth.lerp(partialTicks, vehicle.prevSteer, vehicle.currentSteer);
        float carPitchDeg = (float) Math.toDegrees(smoothPitchRad);
        float carRollDeg  = (float) Math.toDegrees(smoothRollRad);

        // Looking around with the mouse inside the vehicle must NEVER tilt the horizon or sway the camera
        float slopeCameraPitch = 0.0F;
        float slopeCameraRoll  = 0.0F;

        float effectiveShake = isFirstPerson ? (shakeMult * 0.85F) : (shakeMult * 0.40F);
        event.setPitch(event.getPitch() + (shakePitch * effectiveShake) + slopeCameraPitch + traumaPitchKick);
        event.setRoll(event.getRoll() + (shakeRoll * effectiveShake) + slopeCameraRoll + traumaRollKick);

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
            ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleSeatActionPacket(
                targetDoor.vehicle.getId(), targetDoor.seatIndex, com.evgen.cardecor.network.VehicleSeatActionPacket.ACTION_SIT
            ));
        }
    }

    private static void handleSeatSwitchKey(Minecraft mc, DrivableVehicleEntity vehicle) {
        if (mc.player == null) return;
        if (mc.player.tickCount == lastSeatSwitchTick) return;
        lastSeatSwitchTick = mc.player.tickCount;

        int mySeat = vehicle.getSeatForPlayer(mc.player);
        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
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
                ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleChangeSeatPacket(vehicle.getId(), targetedSeat));
            }
        }
    }

    private static void handleWheelAction(Minecraft mc, DrivableVehicleEntity.WheelTarget targetWheel) {
        if (mc.player == null) return;
        if (mc.player.tickCount == lastWheelActionTick) return;
        lastWheelActionTick = mc.player.tickCount;

        int act = targetWheel.isSpare ? VehicleSeatActionPacket.ACTION_REMOVE_SPARE_WHEEL : VehicleSeatActionPacket.ACTION_UNINSTALL_WHEEL;
        ModNetwork.CHANNEL.sendToServer(new VehicleSeatActionPacket(targetWheel.vehicle.getId(), targetWheel.wheelIndex, act));
    }

    @SubscribeEvent
    public static void onScreenOpening(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        event.setCanceled(true);
                        handleSeatSwitchKey(mc, vehicle);
                    }
                } else {
                    long win = mc.getWindow().getWindow();
                    boolean ctrlHeld = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
                    if (ctrlHeld) {
                        DrivableVehicleEntity.WheelTarget targetWheel = DrivableVehicleEntity.findTargetedWheel(mc.player);
                        if (targetWheel != null) {
                            event.setCanceled(true);
                            handleWheelAction(mc, targetWheel);
                            return;
                        }
                    }

                    DrivableVehicleEntity.DoorTarget targetDoor = DrivableVehicleEntity.findTargetedDoor(mc.player);
                    if (targetDoor != null) {
                        event.setCanceled(true);
                        handleDoorEnter(mc, targetDoor);
                        return;
                    }

                    DrivableVehicleEntity.HoodTarget targetHood = DrivableVehicleEntity.findTargetedHood(mc.player);
                    if (targetHood != null) {
                        event.setCanceled(true);
                        targetHood.vehicle.openMechanicsMenu(mc.player);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.FROSTBITE.type()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
                int seat = vehicle.getSeatForPlayer(mc.player);
                boolean inCabin = !vehicle.isCargoBedSeat(seat);
                if (inCabin) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

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

            // 3. Doors & Pickup bed prompt (Doors always take strict precedence!)
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
        String gearText = (gear < 0) ? "R" : (gear == 0 ? "N" : String.valueOf(gear));

        // Expanded informative dashboard badge in bottom-right corner
        int hudW = 120;
        int hudH = 46;
        int hudX = screenWidth - hudW - 10;
        int hudY = screenHeight - hudH - 8;

        // Dark glass background panel with border
        guiGraphics.fill(hudX, hudY, hudX + hudW, hudY + hudH, 0x90000000);
        guiGraphics.fill(hudX, hudY, hudX + hudW, hudY + 1, 0x60FFFFFF);

        // Row 1: Speed (Big) & Gear
        String speedStr = String.format("%.0f", speedKmh) + " km/h";
        guiGraphics.drawString(mc.font, "§f" + speedStr, hudX + 4, hudY + 4, 0xFFFFFF, false);
        if (vehicle.isParkingBrakeOn()) {
            guiGraphics.drawString(mc.font, "§c(P)", hudX + 76, hudY + 4, 0xFFFF4444, false);
        }
        guiGraphics.drawString(mc.font, "§e[" + gearText + "]", hudX + 96, hudY + 4, 0xFFFF55, false);

        // Row 2: Live Engine RPM with dynamic color
        int rpmColor = (rpm > 6500) ? 0xFFFF3333 : (rpm > 4500) ? 0xFFFFAA00 : (rpm > 2500) ? 0xFF55FF55 : 0xFFAAAAAA;
        String rpmStr = String.format("%.0f", rpm) + " RPM";
        guiGraphics.drawString(mc.font, rpmStr, hudX + 4, hudY + 15, rpmColor, false);

        // Boost indicator if forced induction active
        if (vehicle.physics.liveBoostPressure > 0.05) {
            String boostStr = String.format("§b⚡%.1f bar", vehicle.physics.liveBoostPressure);
            guiGraphics.drawString(mc.font, boostStr, hudX + 68, hudY + 15, 0x55FFFF, false);
        }

        // Row 3: Slim Health Bar
        float hp = vehicle.getVehicleHealth();
        float maxHp = DrivableVehicleEntity.MAX_HP;
        int barW = hudW - 8;
        int fillW = (int) (barW * Math.max(0.0F, Math.min(1.0F, hp / maxHp)));
        int barY = hudY + 26;
        guiGraphics.fill(hudX + 4, barY, hudX + 4 + barW, barY + 3, 0xFF2A2A2A);
        float ratio = hp / maxHp;
        int barColor = (ratio > 0.50F) ? 0xFF44BB44 : (ratio > 0.35F) ? 0xFFDDAA22 : (ratio > 0.15F) ? 0xFFEE6622 : 0xFFFF2222;
        guiGraphics.fill(hudX + 4, barY, hudX + 4 + fillW, barY + 3, barColor);

        // Row 4: Fuel Info
        float fuel = vehicle.getFuel();
        int fuelPercent = (int) ((fuel / DrivableVehicleEntity.MAX_FUEL) * 100.0F);
        int fuelColor = (fuelPercent > 45) ? 0xFFAAAA00 : (fuelPercent > 20) ? 0xFFFF7700 : 0xFFFF2222;
        guiGraphics.drawString(mc.font, "GAS: " + fuelPercent + "%", hudX + 4, hudY + 32, fuelColor, false);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Sound Settings hotkey (Default: F8)
        if (mc.screen == null && (ModKeyBindings.KEY_SOUND_SETTINGS.consumeClick() || event.getKey() == GLFW.GLFW_KEY_F8)) {
            mc.setScreen(new com.evgen.cardecor.client.gui.VehicleSoundSettingsScreen(null));
            return;
        }

        if (!(mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle)) return;

        // 1. Dedicated Keybind for seat cycling (Default: X)
        if (ModKeyBindings.KEY_SWITCH_SEAT.consumeClick() || event.getKey() == GLFW.GLFW_KEY_X) {
            ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleChangeSeatPacket(vehicle.getId(), -1));
            return;
        }

        long window = mc.getWindow().getWindow();
        boolean ctrlHeld = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                           InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

        int key = event.getKey();

        // 2. Direct seat hotkeys: Ctrl + 1..5
        if (ctrlHeld) {
            int targetSeat = -2;
            if (key == GLFW.GLFW_KEY_1 || key == GLFW.GLFW_KEY_KP_1) {
                targetSeat = 0; // Seat 1 (Driver)
            } else if (key == GLFW.GLFW_KEY_2 || key == GLFW.GLFW_KEY_KP_2) {
                targetSeat = 1; // Seat 2 (Front Passenger)
            } else if (key == GLFW.GLFW_KEY_3 || key == GLFW.GLFW_KEY_KP_3) {
                targetSeat = 2; // Seat 3 (Rear Left / Bed)
            } else if (key == GLFW.GLFW_KEY_4 || key == GLFW.GLFW_KEY_KP_4) {
                targetSeat = 3; // Seat 4 (Rear Center)
            } else if (key == GLFW.GLFW_KEY_5 || key == GLFW.GLFW_KEY_KP_5) {
                targetSeat = 4; // Seat 5 (Rear Right)
            }

            if (targetSeat >= 0) {
                ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleChangeSeatPacket(vehicle.getId(), targetSeat));
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            int seat = vehicle.getSeatForPlayer(mc.player);
            if (seat == 0 || seat == 1) {
                if (ForgeEvents.isGunItem(mc.player.getMainHandItem()) || ForgeEvents.isGunItem(mc.player.getOffhandItem())) {
                    if (event.getButton() == 0 || event.getButton() == 1) { // 0 = Left click, 1 = Right click
                        event.setCanceled(true);
                        mc.options.keyAttack.setDown(false);
                        mc.options.keyUse.setDown(false);
                        while (mc.options.keyAttack.consumeClick()) {}
                        while (mc.options.keyUse.consumeClick()) {}
                        mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                "\u00a7c[\u041e\u0440\u0443\u0436\u0438\u0435] \u0421\u0442\u0440\u0435\u043b\u044f\u0442\u044c \u0438\u0437 \u043a\u0430\u0431\u0438\u043d\u044b \u043d\u0435\u043b\u044c\u0437\u044f! \u0421\u0442\u0440\u0435\u043b\u044f\u0442\u044c \u043c\u043e\u0436\u043d\u043e \u0442\u043e\u043b\u044c\u043a\u043e \u0441 \u043a\u0443\u0437\u043e\u0432\u0430 (\u0432\u043d\u0435\u0448\u043d\u0435\u0433\u043e \u043c\u0435\u0441\u0442\u0430)."),
                            true
                        );
                    }
                }
            }
        }
    }

    /**
     * Vehicle Exit on Left Alt key & cancels default Shift dismount.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            // Cancel vanilla Shift dismount so Shift is dedicated to nitro boost & controls
            event.getInput().shiftKeyDown = false;

            // Dismount key (KEY_DISMOUNT, default U).
            // NEVER trigger when typing in chat or inside any screen / GUI!
            if (mc.screen == null) {
                boolean dismountPressed = ModKeyBindings.KEY_DISMOUNT.consumeClick();
                if (dismountPressed) {
                    ModNetwork.CHANNEL.sendToServer(new com.evgen.cardecor.network.VehicleDismountPacket(vehicle.getId()));
                    mc.player.unRide();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // Standard Minecraft Sound Options Screen ONLY (Настройки -> Звуки)
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.SoundOptionsScreen soundScreen) {
            int bottomY = soundScreen.height - 27;
            int btnW = 150;
            int btnH = 20;

            // Shift existing Done button to right half
            for (var listener : event.getListenersList()) {
                if (listener instanceof net.minecraft.client.gui.components.Button btn) {
                    if (btn.getY() >= soundScreen.height - 35) {
                        btn.setX(soundScreen.width / 2 + 5);
                        btn.setWidth(btnW);
                    }
                }
            }

            // Add standard styled "Звуки транспорта..." button on the left half
            event.addListener(net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.literal("Звуки транспорта..."),
                btn -> mc.setScreen(new com.evgen.cardecor.client.gui.VehicleSoundSettingsScreen(soundScreen))
            ).bounds(soundScreen.width / 2 - 155, bottomY, btnW, btnH).build());
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(net.minecraftforge.client.event.RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            net.minecraft.commands.Commands.literal("carsound")
                .executes(ctx -> {
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new com.evgen.cardecor.client.gui.VehicleSoundSettingsScreen(null)));
                    return 1;
                })
        );
        event.getDispatcher().register(
            net.minecraft.commands.Commands.literal("cardecor_sounds")
                .executes(ctx -> {
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new com.evgen.cardecor.client.gui.VehicleSoundSettingsScreen(null)));
                    return 1;
                })
        );
    }

    private static final java.util.Set<Integer> tiltedRiderIds = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            applyVehiclePassengerRotation(event.getPoseStack(), vehicle, (float) event.getPartialTick());
            tiltedRiderIds.add(player.getId());
        } else {
            DrivableVehicleEntity deckVehicle = DrivableVehicleEntity.getVehicleCarrying(player);
            if (deckVehicle != null) {
                boolean hasWalkInput = Math.abs(player.zza) > 0.05F || Math.abs(player.xxa) > 0.05F;
                if (!hasWalkInput) {
                    player.walkAnimation.setSpeed(0.0F);
                    player.walkAnimation.update(0.0F, 1.0F);
                    player.walkDist = player.walkDistO;
                    player.bob = player.oBob;
                }
                applyVehiclePassengerRotation(event.getPoseStack(), deckVehicle, (float) event.getPartialTick());
                tiltedRiderIds.add(player.getId());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (tiltedRiderIds.remove(player.getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player) return; // Handled separately in onRenderPlayerPre
        if (event.getEntity().getVehicle() instanceof DrivableVehicleEntity vehicle) {
            applyVehiclePassengerRotation(event.getPoseStack(), vehicle, (float) event.getPartialTick());
            tiltedRiderIds.add(event.getEntity().getId());
        } else {
            DrivableVehicleEntity deckVehicle = DrivableVehicleEntity.getVehicleCarrying(event.getEntity());
            if (deckVehicle != null) {
                event.getEntity().walkAnimation.setSpeed(0.0F);
                event.getEntity().walkAnimation.update(0.0F, 1.0F);
                applyVehiclePassengerRotation(event.getPoseStack(), deckVehicle, (float) event.getPartialTick());
                tiltedRiderIds.add(event.getEntity().getId());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player) return;
        if (tiltedRiderIds.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            org.joml.Matrix4f modelView = (event.getPoseStack() != null)
                ? event.getPoseStack().last().pose() : null;
            com.evgen.cardecor.client.render.DeferredHeadlightRenderer.render(
                event.getCamera(), event.getProjectionMatrix(), modelView, event.getPartialTick()
            );
        }
    }

    private static void applyVehiclePassengerRotation(PoseStack poseStack, DrivableVehicleEntity vehicle, float partialTicks) {
        poseStack.pushPose();

        float smoothRollRad = Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.currentRoll);
        float smoothPitchRad = Mth.lerp(partialTicks, vehicle.prevPitch, vehicle.currentPitch);
        float rollDeg = (float) Math.toDegrees(smoothRollRad);
        float pitchDeg = (float) Math.toDegrees(smoothPitchRad);
        if (Math.abs(rollDeg) > 0.02F || Math.abs(pitchDeg) > 0.02F) {
            float smoothCarYaw = Mth.rotLerp(partialTicks, vehicle.yRotO, vehicle.getYRot());

            // Pivot at rider sitting center / hips (~0.35m above player foot base)
            poseStack.translate(0.0, 0.35, 0.0);

            // 1. Enter car local coordinate frame
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - smoothCarYaw));

            // 2. Apply car's exact 3D Roll and Pitch tilt
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));

            // 3. Return from car local space back to world space
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - smoothCarYaw)));

            poseStack.translate(0.0, -0.35, 0.0);
        }
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
}
