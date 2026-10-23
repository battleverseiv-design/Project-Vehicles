package com.evgen.cardecor.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VehiclePackManager {

    private static final Map<String, VehicleDefinition> REGISTRY = new ConcurrentHashMap<>();
    private static File packsDir;
    public static Runnable onReloadCallback = null;

    public static void init() {
        packsDir = new File("cardecor_packs");
        if (!packsDir.exists()) {
            packsDir.mkdirs();
        }
        loadPacks();
    }

    public static File getPacksDir() {
        if (packsDir == null) {
            packsDir = new File("cardecor_packs");
        }
        return packsDir;
    }

    public static void ensurePackMcmeta(File folder) {
        if (folder == null || !folder.isDirectory() || folder.getName().startsWith(".")) return;
        File mcmeta = new File(folder, "pack.mcmeta");
        if (!mcmeta.exists()) {
            try {
                String content = "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"CarDecor Addon: " + folder.getName() + "\"\n  }\n}\n";
                java.nio.file.Files.writeString(mcmeta.toPath(), content, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
    }

    private static void syncZipPacks(File dir) {
        File cacheDir = new File(dir, ".cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
        if (zipFiles != null) {
            for (File zipFile : zipFiles) {
                FILE_TIMESTAMPS.put(zipFile.getAbsolutePath(), zipFile.lastModified());
                String packName = zipFile.getName().substring(0, zipFile.getName().lastIndexOf('.'));
                File targetDir = new File(cacheDir, packName);
                File marker = new File(targetDir, ".extracted_stamp");
                if (!targetDir.exists() || !marker.exists() || marker.lastModified() < zipFile.lastModified()) {
                    extractZip(zipFile, targetDir);
                    ensurePackMcmeta(targetDir);
                    try {
                        if (!marker.exists()) {
                            marker.createNewFile();
                        }
                        marker.setLastModified(zipFile.lastModified());
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static void extractZip(File zipFile, File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile, StandardCharsets.UTF_8)) {
            Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                File dest = new File(targetDir, entry.getName());
                String canonicalDest = dest.getCanonicalPath();
                if (!canonicalDest.startsWith(targetDir.getCanonicalPath() + File.separator) && !canonicalDest.equals(targetDir.getCanonicalPath())) {
                    continue; // Skip zip-slip attempt
                }
                if (entry.isDirectory()) {
                    dest.mkdirs();
                } else {
                    File parent = dest.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (InputStream in = zip.getInputStream(entry);
                         OutputStream out = new FileOutputStream(dest)) {
                        in.transferTo(out);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Error unpacking " + zipFile.getName() + ": " + e.getMessage());
        }
    }

    private static void scanPackFolder(File pf, Map<String, VehicleDefinition> loadedPacks) {
        if (pf == null || !pf.isDirectory() || pf.getName().startsWith(".")) return;
        ensurePackMcmeta(pf);
        EngineRegistry.scanPackEngines(pf);
        WheelRegistry.scanPackWheels(pf);
        SeatRegistry.scanPackSeats(pf);

        // 1. Check for dedicated "vehicles" or "cars"
        File vehiclesDir = new File(pf, "vehicles");
        if (!vehiclesDir.exists() || !vehiclesDir.isDirectory()) {
            vehiclesDir = new File(pf, "cars");
        }
        if (vehiclesDir.exists() && vehiclesDir.isDirectory()) {
            File[] vFiles = vehiclesDir.listFiles((d, name) -> name.endsWith(".json"));
            if (vFiles != null) {
                for (File vf : vFiles) {
                    loadVehicleFile(vf, pf, loadedPacks);
                }
            }
            return;
        }

        // Check if inside a single root folder from zip (e.g. pf/<innerFolder>/vehicles/)
        File[] subdirs = pf.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File sub : subdirs) {
                if (sub.getName().startsWith(".")) continue;
                File subVehicles = new File(sub, "vehicles");
                if (!subVehicles.exists() || !subVehicles.isDirectory()) {
                    subVehicles = new File(sub, "cars");
                }
                if (subVehicles.exists() && subVehicles.isDirectory()) {
                    ensurePackMcmeta(sub);
                    File[] vFiles = subVehicles.listFiles((d, name) -> name.endsWith(".json"));
                    if (vFiles != null) {
                        for (File vf : vFiles) {
                            loadVehicleFile(vf, sub, loadedPacks);
                        }
                    }
                    return;
                }
            }
        }

        // 2. Root *.json
        File[] rootJsonFiles = pf.listFiles((d, name) -> name.endsWith(".json") && !name.equalsIgnoreCase("pack.mcmeta"));
        if (rootJsonFiles != null) {
            for (File rjf : rootJsonFiles) {
                loadVehicleFile(rjf, pf, loadedPacks);
            }
        }
    }

    public static synchronized void loadPacks() {
        File dir = getPacksDir();
        if (!dir.exists() || !dir.isDirectory()) return;

        syncZipPacks(dir);

        Map<String, VehicleDefinition> loadedPacks = new HashMap<>();

        // Regular folders (exclude hidden like .cache)
        File[] packFolders = dir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
        if (packFolders != null) {
            for (File pf : packFolders) {
                scanPackFolder(pf, loadedPacks);
            }
        }

        // Cached zip folders
        File cacheDir = new File(dir, ".cache");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] cachedFolders = cacheDir.listFiles(File::isDirectory);
            if (cachedFolders != null) {
                for (File cf : cachedFolders) {
                    scanPackFolder(cf, loadedPacks);
                }
            }
        }

        // Atomic swap - NEVER leave registry empty during a render frame!
        REGISTRY.putAll(loadedPacks);
        REGISTRY.keySet().retainAll(loadedPacks.keySet());

        if (onReloadCallback != null) {
            try {
                onReloadCallback.run();
            } catch (Throwable ignored) {}
        }
    }

    private static void loadVehicleFile(File jsonFile, File packDir, Map<String, VehicleDefinition> loadedPacks) {
        if (!jsonFile.exists()) return;
        FILE_TIMESTAMPS.put(jsonFile.getAbsolutePath(), jsonFile.lastModified());
        try {
            String content = java.nio.file.Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            com.google.gson.JsonElement elem = JsonParser.parseString(content);
            if (elem.isJsonObject()) {
                JsonObject root = elem.getAsJsonObject();
                if (root.has("vehicles") && root.get("vehicles").isJsonArray()) {
                    for (com.google.gson.JsonElement vElem : root.getAsJsonArray("vehicles")) {
                        if (vElem.isJsonObject()) {
                            registerDefinition(vElem.getAsJsonObject(), packDir, jsonFile, jsonFile.getName().replace(".json", ""), loadedPacks);
                        }
                    }
                } else {
                    registerDefinition(root, packDir, jsonFile, jsonFile.getName().replace(".json", ""), loadedPacks);
                }
            } else if (elem.isJsonArray()) {
                for (com.google.gson.JsonElement vElem : elem.getAsJsonArray()) {
                    if (vElem.isJsonObject()) {
                        registerDefinition(vElem.getAsJsonObject(), packDir, jsonFile, jsonFile.getName().replace(".json", ""), loadedPacks);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Error loading " + jsonFile.getName() + " in " + packDir.getName() + ": " + e.getMessage());
        }
    }

    private static void registerDefinition(JsonObject root, File packDir, File jsonFile, String fallbackId, Map<String, VehicleDefinition> loadedPacks) {
        try {
            VehicleDefinition def = parseDefinition(root, fallbackId);
            def.packDirectory = packDir;
            def.sourceJsonFile = jsonFile;

            loadedPacks.put(def.id, def);
            System.out.println("[CarDecor] Loaded dynamic vehicle pack: " + def.id + " ('" + def.name + "') from " + packDir.getName());
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to parse vehicle in " + packDir.getName() + ": " + e.getMessage());
        }
    }

    private static VehicleDefinition parseDefinition(JsonObject json, String defaultId) {
        VehicleDefinition def = new VehicleDefinition();
        def.id = (json.has("id") && !json.get("id").isJsonNull()) ? json.get("id").getAsString() : defaultId;
        def.name = (json.has("name") && !json.get("name").isJsonNull()) ? json.get("name").getAsString() : defaultId;
        def.category = (json.has("category") && !json.get("category").isJsonNull()) ? json.get("category").getAsString() : "car";

        if (json.has("default_engine") && !json.get("default_engine").isJsonNull()) {
            def.defaultEngine = json.get("default_engine").getAsString();
        } else if (json.has("defaultEngine") && !json.get("defaultEngine").isJsonNull()) {
            def.defaultEngine = json.get("defaultEngine").getAsString();
        } else if (json.has("engine") && json.get("engine").isJsonObject() && json.getAsJsonObject("engine").has("id")) {
            def.defaultEngine = json.getAsJsonObject("engine").get("id").getAsString();
        }

        if (json.has("models") && json.get("models").isJsonObject()) {
            JsonObject m = json.getAsJsonObject("models");
            if (m.has("chassis")) def.models.chassis = m.get("chassis").isJsonNull() ? null : m.get("chassis").getAsString();
            if (m.has("wheel")) def.models.wheel = m.get("wheel").isJsonNull() ? null : m.get("wheel").getAsString();
            if (m.has("steering_wheel")) def.models.steeringWheel = m.get("steering_wheel").isJsonNull() ? null : m.get("steering_wheel").getAsString();
            else if (m.has("steeringWheel")) def.models.steeringWheel = m.get("steeringWheel").isJsonNull() ? null : m.get("steeringWheel").getAsString();
            if (m.has("spare_wheel")) def.models.spareWheel = m.get("spare_wheel").isJsonNull() ? null : m.get("spare_wheel").getAsString();
            else if (m.has("spareWheel")) def.models.spareWheel = m.get("spareWheel").isJsonNull() ? null : m.get("spareWheel").getAsString();
            if (m.has("flip_v")) def.models.flipV = m.get("flip_v").getAsBoolean();
            else if (m.has("flipV")) def.models.flipV = m.get("flipV").getAsBoolean();
        }
        if (json.has("flip_v")) def.models.flipV = json.get("flip_v").getAsBoolean();
        else if (json.has("flipV")) def.models.flipV = json.get("flipV").getAsBoolean();

        if (json.has("spare_wheel_pos") && json.get("spare_wheel_pos").isJsonArray()) {
            def.spareWheelPos = parseVec3(json.getAsJsonArray("spare_wheel_pos"));
        } else if (json.has("spare_wheel") && json.get("spare_wheel").isJsonObject()) {
            JsonObject swObj = json.getAsJsonObject("spare_wheel");
            if (swObj.has("pos") && swObj.get("pos").isJsonArray()) def.spareWheelPos = parseVec3(swObj.getAsJsonArray("pos"));
            if (swObj.has("model") && !swObj.get("model").isJsonNull()) def.models.spareWheel = swObj.get("model").getAsString();
        }

        if (json.has("textures") && json.get("textures").isJsonObject()) {
            JsonObject t = json.getAsJsonObject("textures");
            if (t.has("chassis")) def.textures.chassis = t.get("chassis").isJsonNull() ? null : t.get("chassis").getAsString();
        }

        if (json.has("scale")) def.scale = json.get("scale").getAsFloat();
        if (json.has("ground_offset_y")) def.groundOffsetY = json.get("ground_offset_y").getAsFloat();
        if (json.has("wheelbase")) def.wheelbase = json.get("wheelbase").getAsDouble();

        // Check root level fallbacks
        if (json.has("horsepower")) {
            double hp = json.get("horsepower").getAsDouble();
            def.physics.horsepower = hp;
            def.engine.horsepower = hp;
            def.physics.enginePower = hp * 110.0;
        } else if (json.has("hp")) {
            double hp = json.get("hp").getAsDouble();
            def.physics.horsepower = hp;
            def.engine.horsepower = hp;
            def.physics.enginePower = hp * 110.0;
        }
        if (json.has("torque_nm")) {
            double tq = json.get("torque_nm").getAsDouble();
            def.physics.torqueNm = tq;
            def.engine.torqueNm = tq;
        } else if (json.has("torque")) {
            double tq = json.get("torque").getAsDouble();
            def.physics.torqueNm = tq;
            def.engine.torqueNm = tq;
        }
        if (json.has("offroad_capability")) {
            def.physics.offroadCapability = json.get("offroad_capability").getAsDouble();
        } else if (json.has("offroad")) {
            def.physics.offroadCapability = json.get("offroad").getAsDouble();
        }

        if (json.has("physics")) {
            JsonObject phys = json.getAsJsonObject("physics");
            if (phys.has("mass_kg")) def.physics.mass = phys.get("mass_kg").getAsDouble();
            else if (phys.has("mass")) def.physics.mass = phys.get("mass").getAsDouble();
            else if (phys.has("weight_kg")) def.physics.mass = phys.get("weight_kg").getAsDouble();
            else if (phys.has("weight")) def.physics.mass = phys.get("weight").getAsDouble();

            if (phys.has("horsepower")) {
                double hp = phys.get("horsepower").getAsDouble();
                def.physics.horsepower = hp;
                def.engine.horsepower = hp;
                def.physics.enginePower = hp * 110.0;
            } else if (phys.has("hp")) {
                double hp = phys.get("hp").getAsDouble();
                def.physics.horsepower = hp;
                def.engine.horsepower = hp;
                def.physics.enginePower = hp * 110.0;
            } else if (phys.has("power")) {
                double hp = phys.get("power").getAsDouble();
                def.physics.horsepower = hp;
                def.engine.horsepower = hp;
                def.physics.enginePower = hp * 110.0;
            }

            if (phys.has("torque_nm")) {
                double tq = phys.get("torque_nm").getAsDouble();
                def.physics.torqueNm = tq;
                def.engine.torqueNm = tq;
            } else if (phys.has("torque")) {
                double tq = phys.get("torque").getAsDouble();
                def.physics.torqueNm = tq;
                def.engine.torqueNm = tq;
            }

            if (phys.has("engine_power")) def.physics.enginePower = phys.get("engine_power").getAsDouble();
            if (phys.has("drivetrain")) def.physics.drivetrain = phys.get("drivetrain").getAsString().toLowerCase();
            if (phys.has("engine_type")) def.physics.engineType = phys.get("engine_type").getAsString().toLowerCase();
            if (phys.has("max_speed_kmh")) def.physics.maxSpeed = phys.get("max_speed_kmh").getAsDouble();
            else if (phys.has("max_speed")) def.physics.maxSpeed = phys.get("max_speed").getAsDouble();
            if (phys.has("boost_speed_kmh")) def.physics.boostSpeed = phys.get("boost_speed_kmh").getAsDouble();
            else if (phys.has("boost_speed")) def.physics.boostSpeed = phys.get("boost_speed").getAsDouble();
            if (phys.has("gears")) def.physics.gears = phys.get("gears").getAsInt();
            if (phys.has("final_drive")) def.physics.finalDrive = phys.get("final_drive").getAsDouble();
            else if (phys.has("главная_пара")) def.physics.finalDrive = phys.get("главная_пара").getAsDouble();
            if (phys.has("reverse_ratio")) def.physics.reverseRatio = phys.get("reverse_ratio").getAsDouble();

            if (phys.has("gear_ratios") && phys.get("gear_ratios").isJsonArray()) {
                com.google.gson.JsonArray grArr = phys.getAsJsonArray("gear_ratios");
                double[] ratios = new double[grArr.size()];
                for (int i = 0; i < grArr.size(); i++) {
                    ratios[i] = grArr.get(i).getAsDouble();
                }
                def.physics.gearRatios = ratios;
                if (ratios.length > 0) {
                    def.physics.gears = ratios.length;
                }
            }
            if (phys.has("steering_angle_deg")) def.physics.maxSteerAngle = phys.get("steering_angle_deg").getAsFloat();
            if (phys.has("steering_response")) def.physics.steeringResponse = phys.get("steering_response").getAsFloat();
            if (phys.has("turn_in_bite")) def.physics.turnInBite = phys.get("turn_in_bite").getAsFloat();
            if (phys.has("steering_agility")) def.physics.steeringAgility = phys.get("steering_agility").getAsDouble();
            if (phys.has("steer_curve_speed")) def.physics.steerCurveSpeed = phys.get("steer_curve_speed").getAsDouble();
            if (phys.has("steer_curve_power")) def.physics.steerCurvePower = phys.get("steer_curve_power").getAsDouble();
            if (phys.has("steer_min_ratio")) def.physics.steerMinRatio = phys.get("steer_min_ratio").getAsDouble();
            if (phys.has("offroad_capability")) def.physics.offroadCapability = phys.get("offroad_capability").getAsDouble();
            else if (phys.has("offroad")) def.physics.offroadCapability = phys.get("offroad").getAsDouble();
            else if (phys.has("passability")) def.physics.offroadCapability = phys.get("passability").getAsDouble();
            if (phys.has("brake_power")) def.physics.brakePower = phys.get("brake_power").getAsFloat();
            if (phys.has("suspension_stiffness")) def.physics.suspensionStiffness = phys.get("suspension_stiffness").getAsFloat();
            if (phys.has("suspension_damping")) def.physics.suspensionDamping = phys.get("suspension_damping").getAsFloat();
            if (phys.has("max_suspension_travel")) def.physics.maxSuspensionTravel = phys.get("max_suspension_travel").getAsDouble();
            else if (phys.has("suspension_max_dip")) def.physics.maxSuspensionTravel = phys.get("suspension_max_dip").getAsDouble();
            if (phys.has("body_roll_factor")) def.physics.bodyRollFactor = phys.get("body_roll_factor").getAsFloat();
            else if (phys.has("крен_кузова")) def.physics.bodyRollFactor = phys.get("крен_кузова").getAsFloat();
            if (phys.has("pitch_dive_factor")) def.physics.pitchDiveFactor = phys.get("pitch_dive_factor").getAsFloat();
            else if (phys.has("pitch_factor")) def.physics.pitchDiveFactor = phys.get("pitch_factor").getAsFloat();
            if (phys.has("anti_roll_bar_stiffness")) def.physics.antiRollBarStiffness = phys.get("anti_roll_bar_stiffness").getAsDouble();
            else if (phys.has("anti_roll_bar")) def.physics.antiRollBarStiffness = phys.get("anti_roll_bar").getAsDouble();
            else if (phys.has("стабилизатор")) def.physics.antiRollBarStiffness = phys.get("стабилизатор").getAsDouble();
            if (phys.has("ride_height")) def.physics.rideHeight = phys.get("ride_height").getAsDouble();
            if (phys.has("weight_dip_sensitivity")) def.physics.weightDipSensitivity = phys.get("weight_dip_sensitivity").getAsDouble();
            if (phys.has("fuel_capacity_liters")) def.physics.fuelCapacityLiters = phys.get("fuel_capacity_liters").getAsDouble();

            // Aerodynamics
            if (phys.has("drag_coefficient")) def.physics.dragCoefficient = phys.get("drag_coefficient").getAsDouble();
            if (phys.has("frontal_area")) def.physics.frontalArea = phys.get("frontal_area").getAsDouble();
            if (phys.has("downforce_coefficient")) def.physics.downforceCoefficient = phys.get("downforce_coefficient").getAsDouble();
            if (phys.has("auto_max_speed")) def.physics.autoMaxSpeed = phys.get("auto_max_speed").getAsBoolean();

            // Forced Induction & Engine RPM
            if (phys.has("aspiration_type")) def.physics.aspirationType = phys.get("aspiration_type").getAsString().toLowerCase();
            if (phys.has("boost_pressure_bar")) def.physics.boostPressureBar = phys.get("boost_pressure_bar").getAsDouble();
            if (phys.has("turbo_spool_rpm")) def.physics.turboSpoolRpm = phys.get("turbo_spool_rpm").getAsDouble();
            if (phys.has("turbo_peak_rpm")) def.physics.turboPeakRpm = phys.get("turbo_peak_rpm").getAsDouble();
            if (phys.has("supercharger_pulley")) def.physics.superchargerPulley = phys.get("supercharger_pulley").getAsDouble();
            if (phys.has("idle_rpm")) def.physics.idleRpm = phys.get("idle_rpm").getAsDouble();
            if (phys.has("redline_rpm")) def.physics.redlineRpm = phys.get("redline_rpm").getAsDouble();

            // Chassis, Wheelbase & 4-Tire Pressures
            if (phys.has("wheelbase")) def.physics.wheelbase = phys.get("wheelbase").getAsDouble();
            if (phys.has("track_width")) def.physics.trackWidth = phys.get("track_width").getAsDouble();
            if (phys.has("wheel_radius")) def.physics.wheelRadius = phys.get("wheel_radius").getAsDouble();
            if (phys.has("front_overhang")) def.physics.frontOverhang = phys.get("front_overhang").getAsDouble();
            if (phys.has("rear_overhang")) def.physics.rearOverhang = phys.get("rear_overhang").getAsDouble();
            if (phys.has("weight_distribution_front")) def.physics.weightDistributionFront = phys.get("weight_distribution_front").getAsDouble();
            else if (phys.has("weight_distribution")) def.physics.weightDistributionFront = phys.get("weight_distribution").getAsDouble();
            else if (phys.has("weight_balance")) def.physics.weightDistributionFront = phys.get("weight_balance").getAsDouble();
            else if (phys.has("развесовка")) def.physics.weightDistributionFront = phys.get("развесовка").getAsDouble();

            if (phys.has("center_of_gravity_height")) def.physics.centerOfGravityHeight = phys.get("center_of_gravity_height").getAsDouble();
            else if (phys.has("cg_height")) def.physics.centerOfGravityHeight = phys.get("cg_height").getAsDouble();
            else if (phys.has("center_of_gravity")) def.physics.centerOfGravityHeight = phys.get("center_of_gravity").getAsDouble();
            else if (phys.has("центр_тяжести")) def.physics.centerOfGravityHeight = phys.get("центр_тяжести").getAsDouble();
            if (phys.has("body_shake_intensity")) def.physics.bodyShakeIntensity = phys.get("body_shake_intensity").getAsDouble();
            if (phys.has("camera_shake_intensity")) def.physics.cameraShakeIntensity = phys.get("camera_shake_intensity").getAsDouble();
            if (phys.has("tire_pressure_fl")) def.physics.tirePressureFL = phys.get("tire_pressure_fl").getAsDouble();
            if (phys.has("tire_pressure_fr")) def.physics.tirePressureFR = phys.get("tire_pressure_fr").getAsDouble();
            if (phys.has("tire_pressure_rl")) def.physics.tirePressureRL = phys.get("tire_pressure_rl").getAsDouble();
            if (phys.has("tire_pressure_rr")) def.physics.tirePressureRR = phys.get("tire_pressure_rr").getAsDouble();
        }

        if (json.has("wheels")) {
            JsonObject w = json.getAsJsonObject("wheels");
            if (w.has("radius")) def.wheels.radius = w.get("radius").getAsDouble();
            if (w.has("wheel_hp")) def.wheels.wheelHp = w.get("wheel_hp").getAsDouble();
            if (w.has("health")) def.wheels.wheelHp = w.get("health").getAsDouble();
            if (w.has("front_left")) def.wheels.frontLeft = parseVec3(w.getAsJsonArray("front_left"));
            if (w.has("front_right")) def.wheels.frontRight = parseVec3(w.getAsJsonArray("front_right"));
            if (w.has("rear_left")) def.wheels.rearLeft = parseVec3(w.getAsJsonArray("rear_left"));
            if (w.has("rear_right")) def.wheels.rearRight = parseVec3(w.getAsJsonArray("rear_right"));
            if (!json.has("wheelbase")) {
                def.wheelbase = Math.abs(def.wheels.frontLeft[2] - def.wheels.rearLeft[2]);
            }
        }

        if (json.has("steering_wheel")) {
            JsonObject sw = json.getAsJsonObject("steering_wheel");
            if (sw.has("enabled")) def.steeringWheel.enabled = sw.get("enabled").getAsBoolean();
            if (sw.has("pos")) def.steeringWheel.pos = parseVec3(sw.getAsJsonArray("pos"));
            if (sw.has("tilt_angle_deg")) def.steeringWheel.tiltAngleDeg = sw.get("tilt_angle_deg").getAsFloat();
            if (sw.has("rotation_multiplier")) def.steeringWheel.rotationMultiplier = sw.get("rotation_multiplier").getAsFloat();
        }

        if (json.has("exhaust")) {
            JsonObject exh = json.getAsJsonObject("exhaust");
            if (exh.has("pos")) def.exhaust.pos = parseVec3(exh.getAsJsonArray("pos"));
            if (exh.has("scale")) def.exhaust.scale = exh.get("scale").getAsFloat();
        }

        if (json.has("seats")) {
            JsonArray seatsArr = json.getAsJsonArray("seats");
            for (int i = 0; i < seatsArr.size(); i++) {
                JsonObject sObj = seatsArr.get(i).getAsJsonObject();
                VehicleDefinition.SeatDef sDef = new VehicleDefinition.SeatDef();
                sDef.seatIndex = sObj.has("seatIndex") ? sObj.get("seatIndex").getAsInt() : (sObj.has("seat") ? sObj.get("seat").getAsInt() : i);
                if (sObj.has("name")) sDef.name = sObj.get("name").getAsString();
                if (sObj.has("pos")) sDef.pos = parseVec3(sObj.getAsJsonArray("pos"));
                if (sObj.has("is_driver")) sDef.isDriver = sObj.get("is_driver").getAsBoolean();
                else sDef.isDriver = (sDef.seatIndex == 0);

                if (sObj.has("allow_weapons")) {
                    sDef.allowWeapons = sObj.get("allow_weapons").getAsBoolean();
                } else {
                    sDef.allowWeapons = !sDef.isDriver; // Driver default is false, passengers default is true
                }

                if (sObj.has("lock_yaw")) sDef.lockYaw = sObj.get("lock_yaw").getAsBoolean();
                else sDef.lockYaw = (sDef.seatIndex < 2); // 3rd seat or turret default is unlocked

                if (sObj.has("yaw_offset_deg")) sDef.yawOffsetDeg = sObj.get("yaw_offset_deg").getAsFloat();
                if (sObj.has("dismount_offset")) sDef.dismountOffset = parseVec3(sObj.getAsJsonArray("dismount_offset"));
                if (sObj.has("can_dismount")) sDef.canDismount = sObj.get("can_dismount").getAsBoolean();
                if (sObj.has("is_cargo_bed")) sDef.isCargoBed = sObj.get("is_cargo_bed").getAsBoolean();
                else if (sObj.has("cargo_bed")) sDef.isCargoBed = sObj.get("cargo_bed").getAsBoolean();

                def.seats.add(sDef);
            }
        }

        if (json.has("hitboxes")) {
            JsonObject hb = json.getAsJsonObject("hitboxes");
            if (hb.has("body_damage_multiplier")) def.hitboxes.bodyDamageMultiplier = hb.get("body_damage_multiplier").getAsDouble();
            if (hb.has("wheel_damage_multiplier")) def.hitboxes.wheelDamageMultiplier = hb.get("wheel_damage_multiplier").getAsDouble();
            if (hb.has("window_damage_multiplier")) def.hitboxes.windowDamageMultiplier = hb.get("window_damage_multiplier").getAsDouble();
            if (hb.has("engine_damage_multiplier")) def.hitboxes.engineDamageMultiplier = hb.get("engine_damage_multiplier").getAsDouble();
            if (hb.has("trunk_damage_multiplier")) def.hitboxes.trunkDamageMultiplier = hb.get("trunk_damage_multiplier").getAsDouble();
            if (hb.has("pass_damage_to_passengers")) def.hitboxes.passDamageToPassengers = hb.get("pass_damage_to_passengers").getAsBoolean();
            if (hb.has("instant_tire_pop_on_bullet")) def.hitboxes.instantTirePopOnBullet = hb.get("instant_tire_pop_on_bullet").getAsBoolean();
            if (hb.has("tire_health")) def.hitboxes.tireHealth = hb.get("tire_health").getAsDouble();
            if (hb.has("cabin_size")) def.hitboxes.cabinSize = parseVec3(hb.getAsJsonArray("cabin_size"));
            if (hb.has("hood_size")) def.hitboxes.hoodSize = parseVec3(hb.getAsJsonArray("hood_size"));
            if (hb.has("trunk_size")) def.hitboxes.trunkSize = parseVec3(hb.getAsJsonArray("trunk_size"));
            if (hb.has("chassis_size")) def.hitboxes.chassisSize = parseVec3(hb.getAsJsonArray("chassis_size"));
        }

        if (json.has("sounds")) {
            JsonObject s = json.getAsJsonObject("sounds");
            if (s.has("engine_type")) def.sounds.engineType = s.get("engine_type").getAsString();
            if (s.has("master_volume")) def.sounds.masterVolume = s.get("master_volume").getAsFloat();
            if (s.has("pitch_multiplier")) def.sounds.pitchMultiplier = s.get("pitch_multiplier").getAsFloat();
            if (s.has("rpm_idle_to_low")) def.sounds.rpmIdleToLow = s.get("rpm_idle_to_low").getAsFloat();
            if (s.has("rpm_low_to_mid")) def.sounds.rpmLowToMid = s.get("rpm_low_to_mid").getAsFloat();
            if (s.has("rpm_mid_to_high")) def.sounds.rpmMidToHigh = s.get("rpm_mid_to_high").getAsFloat();
            if (s.has("rpm_high_to_max")) def.sounds.rpmHighToMax = s.get("rpm_high_to_max").getAsFloat();
            if (s.has("speed_idle_to_low")) def.sounds.speedIdleToLow = s.get("speed_idle_to_low").getAsFloat();
            if (s.has("speed_low_to_mid")) def.sounds.speedLowToMid = s.get("speed_low_to_mid").getAsFloat();
            if (s.has("speed_mid_to_high")) def.sounds.speedMidToHigh = s.get("speed_mid_to_high").getAsFloat();
            if (s.has("crossfade_curve")) def.sounds.crossfadeCurve = s.get("crossfade_curve").getAsString();
            if (s.has("temporal_smoothing")) def.sounds.temporalSmoothing = s.get("temporal_smoothing").getAsFloat();
            if (s.has("tire_explode_sound")) def.sounds.tireExplodeSound = s.get("tire_explode_sound").getAsString();
            if (s.has("tire_explode_volume")) def.sounds.tireExplodeVolume = s.get("tire_explode_volume").getAsFloat();
            if (s.has("tire_explode_pitch")) def.sounds.tireExplodePitch = s.get("tire_explode_pitch").getAsFloat();
            if (s.has("tire_deflate_sound")) def.sounds.tireDeflateSound = s.get("tire_deflate_sound").getAsString();
            if (s.has("tire_deflate_volume")) def.sounds.tireDeflateVolume = s.get("tire_deflate_volume").getAsFloat();
            if (s.has("tire_deflate_pitch")) def.sounds.tireDeflatePitch = s.get("tire_deflate_pitch").getAsFloat();
            if (s.has("trunk_open")) def.sounds.trunkOpen = s.get("trunk_open").getAsString();
            if (s.has("trunk_close")) def.sounds.trunkClose = s.get("trunk_close").getAsString();
            if (s.has("horn")) def.sounds.horn = s.get("horn").getAsString();
        }

        if (json.has("engine_start")) {
            JsonObject es = json.getAsJsonObject("engine_start");
            if (es.has("start_chance")) def.engineStart.startChance = es.get("start_chance").getAsDouble();
            if (es.has("crank_ticks")) def.engineStart.crankTicks = es.get("crank_ticks").getAsInt();
            if (es.has("fail_allowed")) def.engineStart.failAllowed = es.get("fail_allowed").getAsBoolean();
        }

        if (json.has("engine")) {
            JsonObject eng = json.getAsJsonObject("engine");
            if (eng.has("id")) def.engine.id = eng.get("id").getAsString();
            if (eng.has("name")) def.engine.name = eng.get("name").getAsString();
            if (eng.has("type")) def.engine.type = eng.get("type").getAsString();
            if (eng.has("horsepower")) {
                double hp = eng.get("horsepower").getAsDouble();
                def.engine.horsepower = hp;
                if (!json.has("physics") || (!json.getAsJsonObject("physics").has("horsepower") && !json.getAsJsonObject("physics").has("hp"))) {
                    def.physics.horsepower = hp;
                    def.physics.enginePower = hp * 110.0;
                }
            } else if (eng.has("hp")) {
                double hp = eng.get("hp").getAsDouble();
                def.engine.horsepower = hp;
                if (!json.has("physics") || (!json.getAsJsonObject("physics").has("horsepower") && !json.getAsJsonObject("physics").has("hp"))) {
                    def.physics.horsepower = hp;
                    def.physics.enginePower = hp * 110.0;
                }
            } else if (eng.has("power")) {
                double hp = eng.get("power").getAsDouble();
                def.engine.horsepower = hp;
                if (!json.has("physics") || (!json.getAsJsonObject("physics").has("horsepower") && !json.getAsJsonObject("physics").has("hp"))) {
                    def.physics.horsepower = hp;
                    def.physics.enginePower = hp * 110.0;
                }
            }

            if (eng.has("torque_nm")) {
                double tq = eng.get("torque_nm").getAsDouble();
                def.engine.torqueNm = tq;
                if (!json.has("physics") || (!json.getAsJsonObject("physics").has("torque_nm") && !json.getAsJsonObject("physics").has("torque"))) {
                    def.physics.torqueNm = tq;
                }
            } else if (eng.has("torque")) {
                double tq = eng.get("torque").getAsDouble();
                def.engine.torqueNm = tq;
                if (!json.has("physics") || (!json.getAsJsonObject("physics").has("torque_nm") && !json.getAsJsonObject("physics").has("torque"))) {
                    def.physics.torqueNm = tq;
                }
            }

            if (eng.has("idle_rpm")) def.engine.idleRpm = eng.get("idle_rpm").getAsDouble();
            if (eng.has("redline_rpm")) def.engine.redlineRpm = eng.get("redline_rpm").getAsDouble();
            if (eng.has("aspiration")) def.engine.aspiration = eng.get("aspiration").getAsString();
            if (eng.has("boost_pressure_bar")) def.engine.boostPressureBar = eng.get("boost_pressure_bar").getAsDouble();
            if (eng.has("turbo_spool_rpm")) def.engine.turboSpoolRpm = eng.get("turbo_spool_rpm").getAsDouble();
            if (eng.has("turbo_peak_rpm")) def.engine.turboPeakRpm = eng.get("turbo_peak_rpm").getAsDouble();
            if (eng.has("supercharger_pulley")) def.engine.superchargerPulley = eng.get("supercharger_pulley").getAsDouble();
            if (eng.has("fuel_consumption")) def.engine.fuelConsumption = eng.get("fuel_consumption").getAsDouble();

            if (eng.has("starter")) {
                JsonObject es = eng.getAsJsonObject("starter");
                if (es.has("start_chance")) def.engineStart.startChance = es.get("start_chance").getAsDouble();
                if (es.has("crank_ticks")) def.engineStart.crankTicks = es.get("crank_ticks").getAsInt();
                if (es.has("fail_allowed")) def.engineStart.failAllowed = es.get("fail_allowed").getAsBoolean();
                def.engine.starter = def.engineStart;
            }

            if (eng.has("sounds")) {
                JsonObject s = eng.getAsJsonObject("sounds");
                if (s.has("engine_type")) def.sounds.engineType = s.get("engine_type").getAsString();
                if (s.has("master_volume")) def.sounds.masterVolume = s.get("master_volume").getAsFloat();
                if (s.has("pitch_multiplier")) def.sounds.pitchMultiplier = s.get("pitch_multiplier").getAsFloat();
                if (s.has("rpm_idle_to_low")) def.sounds.rpmIdleToLow = s.get("rpm_idle_to_low").getAsFloat();
                if (s.has("rpm_low_to_mid")) def.sounds.rpmLowToMid = s.get("rpm_low_to_mid").getAsFloat();
                if (s.has("rpm_mid_to_high")) def.sounds.rpmMidToHigh = s.get("rpm_mid_to_high").getAsFloat();
                if (s.has("rpm_high_to_max")) def.sounds.rpmHighToMax = s.get("rpm_high_to_max").getAsFloat();
                if (s.has("speed_idle_to_low")) def.sounds.speedIdleToLow = s.get("speed_idle_to_low").getAsFloat();
                if (s.has("speed_low_to_mid")) def.sounds.speedLowToMid = s.get("speed_low_to_mid").getAsFloat();
                if (s.has("speed_mid_to_high")) def.sounds.speedMidToHigh = s.get("speed_mid_to_high").getAsFloat();
                if (s.has("speed_high_to_max")) def.sounds.speedHighToMax = s.get("speed_high_to_max").getAsFloat();
                if (s.has("crossfade_curve")) def.sounds.crossfadeCurve = s.get("crossfade_curve").getAsString();
                if (s.has("temporal_smoothing")) def.sounds.temporalSmoothing = s.get("temporal_smoothing").getAsFloat();
                def.engine.sounds = def.sounds;
            }

            if (eng.has("aspiration_sounds")) {
                JsonObject as = eng.getAsJsonObject("aspiration_sounds");
                if (as.has("type")) def.engine.aspirationSounds.type = as.get("type").getAsString();
                if (as.has("turbo_volume")) def.engine.aspirationSounds.turboVolume = as.get("turbo_volume").getAsFloat();
                if (as.has("supercharger_volume")) def.engine.aspirationSounds.superchargerVolume = as.get("supercharger_volume").getAsFloat();
                if (as.has("blow_off_volume")) def.engine.aspirationSounds.blowOffVolume = as.get("blow_off_volume").getAsFloat();
                if (as.has("enable_blow_off")) def.engine.aspirationSounds.enableBlowOff = as.get("enable_blow_off").getAsBoolean();
                if (as.has("enable_whine")) def.engine.aspirationSounds.enableWhine = as.get("enable_whine").getAsBoolean();
            }
        }

        if (json.has("camera")) {
            JsonObject c = json.getAsJsonObject("camera");
            if (c.has("pitch_follow")) def.camera.pitchFollow = c.get("pitch_follow").getAsDouble();
            if (c.has("roll_follow")) def.camera.rollFollow = c.get("roll_follow").getAsDouble();
            if (c.has("cornering_bank")) def.camera.corneringBank = c.get("cornering_bank").getAsDouble();
            if (c.has("look_ahead")) def.camera.lookAhead = c.get("look_ahead").getAsDouble();
            if (c.has("shake_intensity")) def.camera.shakeIntensity = c.get("shake_intensity").getAsDouble();
        }
        if (json.has("physics") && json.getAsJsonObject("physics").has("camera_shake_intensity")) {
            def.camera.shakeIntensity = json.getAsJsonObject("physics").get("camera_shake_intensity").getAsDouble();
        }

        if (json.has("trunk")) {
            JsonObject tr = json.getAsJsonObject("trunk");
            if (tr.has("rows")) def.trunk.rows = tr.get("rows").getAsInt();
        }

        if (json.has("headlights")) {
            JsonObject hl = json.getAsJsonObject("headlights");
            if (hl.has("range")) def.headlights.range = hl.get("range").getAsDouble();
            if (hl.has("beam_width")) def.headlights.beamWidth = hl.get("beam_width").getAsDouble();
            if (hl.has("height_offset")) def.headlights.heightOffset = hl.get("height_offset").getAsDouble();
            if (hl.has("downward_angle")) def.headlights.downwardAngle = hl.get("downward_angle").getAsDouble();
            if (hl.has("light_level")) def.headlights.lightLevel = hl.get("light_level").getAsInt();
            if (hl.has("offset_z")) def.headlights.offsetZ = hl.get("offset_z").getAsDouble();
            if (hl.has("flare_size")) def.headlights.flareSize = hl.get("flare_size").getAsDouble();
            if (hl.has("intensity")) def.headlights.intensity = hl.get("intensity").getAsDouble();
            if (hl.has("color")) def.headlights.color = hl.get("color").getAsString();

            if (hl.has("lights") && hl.get("lights").isJsonArray()) {
                JsonArray lightsArr = hl.getAsJsonArray("lights");
                for (JsonElement el : lightsArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject lo = el.getAsJsonObject();
                    VehicleDefinition.LightSourceDef lDef = new VehicleDefinition.LightSourceDef();
                    if (lo.has("pos") && lo.get("pos").isJsonArray()) {
                        JsonArray pa = lo.getAsJsonArray("pos");
                        if (pa.size() >= 3) {
                            lDef.pos = new double[]{pa.get(0).getAsDouble(), pa.get(1).getAsDouble(), pa.get(2).getAsDouble()};
                        }
                    }
                    if (lo.has("range")) lDef.range = lo.get("range").getAsDouble();
                    else lDef.range = def.headlights.range;

                    if (lo.has("cone_angle")) lDef.coneAngle = lo.get("cone_angle").getAsDouble();
                    if (lo.has("intensity")) lDef.intensity = lo.get("intensity").getAsDouble();
                    else lDef.intensity = def.headlights.intensity;

                    if (lo.has("flare_size")) lDef.flareSize = lo.get("flare_size").getAsDouble();
                    else lDef.flareSize = def.headlights.flareSize;

                    if (lo.has("downward_angle")) lDef.downwardAngle = lo.get("downward_angle").getAsDouble();
                    else lDef.downwardAngle = def.headlights.downwardAngle;

                    if (lo.has("yaw_angle")) lDef.yawAngle = lo.get("yaw_angle").getAsDouble();
                    if (lo.has("color")) lDef.color = lo.get("color").getAsString();

                    def.headlights.lights.add(lDef);
                }
            }
        }

        JsonObject tl = null;
        if (json.has("taillights") && json.get("taillights").isJsonObject()) {
            tl = json.getAsJsonObject("taillights");
        } else if (json.has("brake_lights") && json.get("brake_lights").isJsonObject()) {
            tl = json.getAsJsonObject("brake_lights");
        }
        if (tl != null) {
            if (tl.has("beam_width")) def.taillights.beamWidth = tl.get("beam_width").getAsDouble();
            if (tl.has("height_offset")) def.taillights.heightOffset = tl.get("height_offset").getAsDouble();
            if (tl.has("offset_z")) def.taillights.offsetZ = tl.get("offset_z").getAsDouble();
            if (tl.has("flare_size")) def.taillights.flareSize = tl.get("flare_size").getAsDouble();
            if (tl.has("range")) def.taillights.range = tl.get("range").getAsDouble();
            if (tl.has("color")) def.taillights.color = tl.get("color").getAsString();
            if (tl.has("idle_brightness")) {
                double ib = tl.get("idle_brightness").getAsDouble();
                def.taillights.idleBrightness = (ib > 1.0) ? (ib / 100.0) : ib;
            }
            if (tl.has("brake_brightness")) {
                double bb = tl.get("brake_brightness").getAsDouble();
                def.taillights.brakeBrightness = (bb > 1.0) ? (bb / 100.0) : bb;
            }
        }

        // Final synchronization: ensure physics and engine always agree on horsepower & torque
        if (def.physics.horsepower > 0.0) {
            def.engine.horsepower = def.physics.horsepower;
        } else if (def.engine.horsepower > 0.0) {
            def.physics.horsepower = def.engine.horsepower;
        }
        if (def.physics.torqueNm > 0.0) {
            def.engine.torqueNm = def.physics.torqueNm;
        } else if (def.engine.torqueNm > 0.0) {
            def.physics.torqueNm = def.engine.torqueNm;
        }

        // Auto-calibrate 3D mesh hitboxes & boundary dimensions from 3D model geometry
        VehicleMeshCalibrator.calibrate(def);

        return def;
    }

    private static float[] parseVec3(JsonArray arr) {
        if (arr == null || arr.size() < 3) return new float[]{0, 0, 0};
        return new float[]{
            arr.get(0).getAsFloat(),
            arr.get(1).getAsFloat(),
            arr.get(2).getAsFloat()
        };
    }

    private static final Map<String, Long> FILE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static long lastCheckTime = 0;

    public static synchronized void checkHotReload() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 800) return; // Check at most every 0.8s
        lastCheckTime = now;

        boolean changed = false;
        for (Map.Entry<String, Long> entry : FILE_TIMESTAMPS.entrySet()) {
            File f = new File(entry.getKey());
            if (f.exists() && f.lastModified() != entry.getValue()) {
                changed = true;
                break;
            }
        }
        if (changed) {
            loadPacks();
        }
    }

    public static Set<String> getAllKeys() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    public static Collection<VehicleDefinition> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static VehicleDefinition get(String id) {
        if (id == null) return null;
        return REGISTRY.get(id);
    }
}
