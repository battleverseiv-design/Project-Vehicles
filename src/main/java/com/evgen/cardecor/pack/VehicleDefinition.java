package com.evgen.cardecor.pack;

import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VehicleDefinition {
    public String id;
    public String name;
    public String category = "car";
    public String defaultEngine = null;
    public String defaultWheel = "standard_road_wheel";
    public String defaultSeat = "standard_seat";
    @com.google.gson.annotations.SerializedName(value = "default_dirt_level", alternate = {"defaultDirtLevel"})
    public float defaultDirtLevel = 0.0F;
    @com.google.gson.annotations.SerializedName(value = "default_tire_condition", alternate = {"defaultTireCondition"})
    public float defaultTireCondition = 100.0F;

    public Models models = new Models();
    public Textures textures = new Textures();
    public Physics physics = new Physics();
    public Wheels wheels = new Wheels();
    public SteeringWheel steeringWheel = new SteeringWheel();
    public HitboxConfig hitboxes = new HitboxConfig();
    public List<SeatDef> seats = new ArrayList<>();
    public SoundConfig sounds = new SoundConfig();
    public EngineStartConfig engineStart = new EngineStartConfig();
    public ExhaustConfig exhaust = new ExhaustConfig();
    public CameraConfig camera = new CameraConfig();
    public CalibratedBounds calibratedBounds = new CalibratedBounds();
    public TrunkConfig trunk = new TrunkConfig();
    public HeadlightConfig headlights = new HeadlightConfig();
    public TaillightConfig taillights = new TaillightConfig();
    public DurabilityConfig durability = new DurabilityConfig();
    public RammingConfig ramming = new RammingConfig();
    @com.google.gson.annotations.SerializedName(value = "traversal", alternate = {"terrain_traversal", "terrainTraversal"})
    public com.evgen.cardecor.physics.dynamics.TerrainTraversalSpec traversal = new com.evgen.cardecor.physics.dynamics.TerrainTraversalSpec();
    public List<AnimationDef> animations = new ArrayList<>();

    public static class AnimationDef {
        public String part;
        public String type = "rotation"; // "rotation" or "translation"
        public String variable;          // "door_left", "door_right", "trunk", "steer", "speed", "rpm", "gear", "handbrake", "headlights"
        public float[] axis = new float[]{0.0F, 1.0F, 0.0F};
        public float[] center = new float[]{0.0F, 0.0F, 0.0F};
        public float factor = 1.0F;
        public float offset = 0.0F;
        public float min = -360.0F;
        public float max = 360.0F;
    }

    public static class TrunkConfig {
        public int rows = 3; // 1 to 6 (9 to 54 slots)
    }

    public static class LightSourceDef {
        public double[] pos = new double[]{0.0, 0.75, -2.2}; // [x, y, z] in model coordinates (+X right, +Y up, -Z fwd)
        public double range = 42.0; // individual light range in meters/blocks
        public double coneAngle = 32.0; // beam half-angle in degrees (15.0 to 60.0)
        public double intensity = 1.0; // individual brightness multiplier
        public double flareSize = 0.18; // size of glowing lens sprite in meters
        public double downwardAngle = 0.0; // downward slope
        public double yawAngle = 0.0; // yaw angle (positive = right, negative = left)
        public String color = null; // null = inherit from vehicle headlight color, or custom hex/name
    }

    public static class HeadlightConfig {
        public double range = 42.0; // default beam range (10.0 to 100.0 blocks)
        public double highBeamRange = 75.0; // legacy compatibility field
        public double beamWidth = 0.65; // lateral spacing between headlights (meters)
        public double heightOffset = 0.75; // height of headlights above vehicle origin (meters)
        public double downwardAngle = 0.0; // default straight forward beam (0 = level with vehicle)
        public int lightLevel = 15; // 1 to 15 brightness
        public double offsetZ = 0.0; // custom Z offset (0 = auto from frontDist)
        public double flareSize = 0.16; // size of glowing lamp
        public double intensity = 1.0; // global brightness multiplier (0.1 to 3.0)
        public String color = "warm_white"; // "warm_white", "cool_xenon", "amber_yellow", or "#HEX"
        public List<LightSourceDef> lights = new ArrayList<>(); // custom lights list

        public List<LightSourceDef> getResolvedLights(VehicleDefinition def) {
            if (lights != null && !lights.isEmpty()) {
                return lights;
            }
            // Auto-generate standard left & right lamps for 100% backward compatibility
            List<LightSourceDef> autoLights = new ArrayList<>(2);
            float scale = (def != null && def.scale > 0.05f) ? def.scale : 1.0f;
            float frontReach = (float) ((def != null && def.calibratedBounds != null && def.calibratedBounds.frontDist > 0.5)
                ? (def.calibratedBounds.frontDist * 0.92f) / scale
                : ((def != null && def.wheels != null && def.wheels.frontLeft != null && def.wheels.frontLeft.length > 2)
                    ? (Math.abs(def.wheels.frontLeft[2]) + 0.60f)
                    : 2.65f));
            double hlZ = (Math.abs(this.offsetZ) > 0.05) ? this.offsetZ : -frontReach;
            double hlY = (this.heightOffset > 0.05) ? this.heightOffset : ((def != null && def.calibratedBounds != null) ? (def.calibratedBounds.bumperStepHeight + 0.25) / scale : 0.72);
            double hlX = (this.beamWidth > 0.05) ? this.beamWidth : ((def != null && def.calibratedBounds != null) ? (def.calibratedBounds.halfWidth * 0.78) / scale : 0.78);

            LightSourceDef left = new LightSourceDef();
            left.pos = new double[]{-hlX, hlY, hlZ};
            left.range = this.range > 10.0 ? this.range : 42.0;
            left.coneAngle = 38.0;
            left.intensity = this.intensity > 0.05 ? this.intensity : 1.0;
            left.flareSize = this.flareSize > 0.02 ? this.flareSize : 0.18;
            left.downwardAngle = this.downwardAngle;
            left.yawAngle = 0.0;
            left.color = this.color;
            autoLights.add(left);

            LightSourceDef right = new LightSourceDef();
            right.pos = new double[]{hlX, hlY, hlZ};
            right.range = this.range > 10.0 ? this.range : 42.0;
            right.coneAngle = 38.0;
            right.intensity = this.intensity > 0.05 ? this.intensity : 1.0;
            right.flareSize = this.flareSize > 0.02 ? this.flareSize : 0.18;
            right.downwardAngle = this.downwardAngle * 0.85;
            right.yawAngle = 0.02; // slight roadside spread
            right.color = this.color;
            autoLights.add(right);

            return autoLights;
        }

        public static float[] parseColor(String colStr, float[] fallback) {
            if (colStr == null || colStr.isEmpty()) return fallback;
            String s = colStr.trim().toLowerCase();
            if (s.startsWith("#")) s = s.substring(1);
            if (s.length() == 6) {
                try {
                    int r = Integer.parseInt(s.substring(0, 2), 16);
                    int g = Integer.parseInt(s.substring(2, 4), 16);
                    int b = Integer.parseInt(s.substring(4, 6), 16);
                    return new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
                } catch (Exception ignored) {}
            }
            return switch (s) {
                case "cool_xenon", "xenon" -> new float[]{0.84f, 0.92f, 1.00f};
                case "amber_yellow", "yellow", "amber" -> new float[]{1.00f, 0.82f, 0.20f};
                case "pure_white", "white" -> new float[]{1.00f, 1.00f, 1.00f};
                case "warm_white", "warm", "vintage_halogen", "halogen" -> new float[]{1.00f, 0.96f, 0.82f};
                case "red" -> new float[]{1.00f, 0.15f, 0.15f};
                case "blue" -> new float[]{0.15f, 0.45f, 1.00f};
                case "green" -> new float[]{0.20f, 1.00f, 0.30f};
                case "purple", "violet" -> new float[]{0.85f, 0.20f, 1.00f};
                case "cyan" -> new float[]{0.20f, 0.95f, 1.00f};
                case "orange" -> new float[]{1.00f, 0.55f, 0.10f};
                default -> fallback;
            };
        }
    }

    public static class TaillightConfig {
        public double beamWidth = 0.95;
        public double heightOffset = 0.97;
        public double offsetZ = 0.0; // 0.0 = auto from calibrated bounds
        public double flareSize = 0.13;
        public double range = 14.0;
        public double idleBrightness = 0.25; // 0.0 to 1.0 (running lights brightness)
        public double brakeBrightness = 1.00; // 100% full brake light
        public String color = "red";
    }

    public double[] getTaillightLeftPos() {
        float s = (this.scale > 0.05f) ? this.scale : 1.0f;
        double rearReach;
        if (this.calibratedBounds != null && this.calibratedBounds.rearDist > 0.5) {
            rearReach = (this.calibratedBounds.rearDist * 0.93) / s;
        } else if (this.wheels != null && this.wheels.rearLeft != null && this.wheels.rearLeft.length > 2) {
            double rOverhang = (this.physics != null && this.physics.rearOverhang > 0.1) ? this.physics.rearOverhang : 0.95;
            rearReach = Math.abs(this.wheels.rearLeft[2]) + rOverhang;
        } else {
            rearReach = 2.65;
        }
        double minRearZ = (this.wheels != null && this.wheels.rearLeft != null && this.wheels.rearLeft.length > 2)
            ? Math.abs(this.wheels.rearLeft[2]) + 0.20 : 1.2;
        double tz = (this.taillights != null && Math.abs(this.taillights.offsetZ) > 0.05) ? this.taillights.offsetZ : rearReach;
        double ty = (this.taillights != null && this.taillights.heightOffset > 0.05) ? this.taillights.heightOffset : 0.97;
        double tx = (this.taillights != null && this.taillights.beamWidth > 0.05) ? this.taillights.beamWidth : 0.95;
        return new double[]{-tx, ty, tz};
    }

    public double[] getTaillightRightPos() {
        double[] left = getTaillightLeftPos();
        return new double[]{-left[0], left[1], left[2]};
    }

    public static class CalibratedBounds {
        public double frontDist = 3.25;
        public double rearDist = 3.25;
        public double halfWidth = 0.98;
        public double hoodHeight = 1.48;
        public double roofHeight = 2.45;
        public double trunkHeight = 1.38;
        public double bumperStepHeight = 0.55;
        public double cabinFrontZ = 0.70;
        public double cabinRearZ = -0.80;
        public double totalHeight = 2.45;
        public boolean hasCargoBed = false;
        public double bedFloorHeight = 0.68;
        public boolean hasRoof = true;
        public double windshieldBottomZ = 0.0;
        public double windshieldTopZ = 0.0;
        public double windshieldBottomY = 0.0;
        public double windshieldTopY = 0.0;
    }

    public static class DurabilityConfig {
        @com.google.gson.annotations.SerializedName(value = "max_bumper_health", alternate = {"maxBumperHealth", "bumper_health", "bumperHealth"})
        public float maxBumperHealth = 100.0F;

        @com.google.gson.annotations.SerializedName(value = "max_hood_health", alternate = {"maxHoodHealth", "hood_health", "hoodHealth"})
        public float maxHoodHealth = 100.0F;

        @com.google.gson.annotations.SerializedName(value = "max_radiator_health", alternate = {"maxRadiatorHealth", "radiator_health", "radiatorHealth"})
        public float maxRadiatorHealth = 100.0F;
    }

    public static class RammingConfig {
        @com.google.gson.annotations.SerializedName(value = "recoil_damage_mult", alternate = {"recoilDamageMult"})
        public float recoilDamageMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "damage_dealt_mult", alternate = {"damageDealtMult"})
        public float damageDealtMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "momentum_retention", alternate = {"momentumRetention"})
        public float momentumRetention = 0.95F;

        @com.google.gson.annotations.SerializedName(value = "hood_launch_vertical_mult", alternate = {"hoodLaunchVerticalMult"})
        public float hoodLaunchVerticalMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "hood_launch_forward_mult", alternate = {"hoodLaunchForwardMult"})
        public float hoodLaunchForwardMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "wheel_crush_drag_mult", alternate = {"wheelCrushDragMult"})
        public float wheelCrushDragMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "side_swipe_spin_mult", alternate = {"sideSwipeSpinMult"})
        public float sideSwipeSpinMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "camera_trauma_mult", alternate = {"cameraTraumaMult"})
        public float cameraTraumaMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "blood_accumulation_mult", alternate = {"bloodAccumulationMult"})
        public float bloodAccumulationMult = 1.0F;

        @com.google.gson.annotations.SerializedName(value = "can_deform_metal", alternate = {"canDeformMetal"})
        public boolean canDeformMetal = true;
    }

    public static class CameraConfig {
        public double pitchFollow = 0.70;      // Slope climb & dive follow
        public double rollFollow = 0.45;       // Side slope & terrain banking
        public double corneringBank = 0.25;    // Turn centrifugal banking lean (softened!)
        public double lookAhead = 0.10;        // Steering look-into-turn yaw
        public double shakeIntensity = 0.35;   // Engine vibration / bumps
    }

    public static class ExhaustConfig {
        public float[] pos = new float[]{1.08f, 0.38f, -2.22f}; // [X (side/flank), Y (height), Z (forward/rear)]
        public float scale = 1.0f;
    }

    public float scale = 1.0F;
    public float groundOffsetY = 0.0F;
    public double wheelbase = 3.2;

    public File packDirectory;
    public File sourceJsonFile;

    public float[] spareWheelPos = null;

    public boolean hasCargoBed() {
        if (this.calibratedBounds != null && this.calibratedBounds.hasCargoBed) return true;
        if ("pickup".equalsIgnoreCase(this.category) || "truck".equalsIgnoreCase(this.category)) return true;
        return this.id != null && this.id.contains("pickup");
    }

    public static class Models {
        public String chassis = "models/chassis.obj";
        public String wheel = "models/wheel.obj";
        public String steeringWheel = "models/steering_wheel.obj";
        public String spareWheel = null;
        public boolean flipV = false;
    }

    public static class Textures {
        public String chassis = "textures/skin.png";
        public String wheel;
    }

    public static class Physics {
        public double mass = 1400.0;               // kg
        public double horsepower = 150.0;          // hp
        public double torqueNm = 220.0;            // Nm
        public double maxSpeed = 75.0;             // km/h
        public double boostSpeed = 95.0;           // km/h
        public int gears = 5;
        public double finalDrive = 0.0;            // Differential / axle ratio (e.g. 4.10, 3.55)
        public double[] gearRatios = null;         // Transmission gear ratios [1st, 2nd, 3rd, ...]
        public double reverseRatio = 0.0;          // Reverse gear ratio (e.g. 3.80)
        public double enginePower = 11000.0;       // N
        public String drivetrain = "rwd";          // rwd, fwd, awd
        public String engineType = "inline6";      // v8_muscle, inline6, v4_economy, turbo_sports, diesel

        // Aerodynamics
        public double dragCoefficient = 0.35;
        public double frontalArea = 2.20;
        public double downforceCoefficient = 0.15;
        public boolean autoMaxSpeed = true;

        // Forced Induction & RPM
        public String aspirationType = "na";
        public double boostPressureBar = 1.20;
        public double turboSpoolRpm = 2200.0;
        public double turboPeakRpm = 4600.0;
        public double superchargerPulley = 1.80;
        public double idleRpm = 850.0;
        public double redlineRpm = 7200.0;

        // Geometry & 4 Tires
        public double wheelbase = 2.85;
        public double trackWidth = 1.75;
        public double wheelRadius = 0.35;
        public double frontOverhang = 0.85;
        public double rearOverhang = 0.95;
        public double weightDistributionFront = 0.54;
        public double centerOfGravityHeight = 0.55;
        public double bodyShakeIntensity = 1.0;
        public double cameraShakeIntensity = 1.0;
        public double tirePressureFL = 2.20;
        public double tirePressureFR = 2.20;
        public double tirePressureRL = 2.20;
        public double tirePressureRR = 2.20;

        public float maxSteerAngle = 38.0F;        // degrees
        public float steeringResponse = 1.25F;     // 0.5 (lazy) to 2.5 (razor sharp)
        public float turnInBite = 1.20F;           // front grip sharpness
        public double steeringAgility = 1.35;      // master turnability multiplier (0.5 to 3.5)
        public double steerCurveSpeed = 36.0;      // inflection speed (km/h)
        public double steerCurvePower = 1.35;      // drop-off exponent
        public double steerMinRatio = 0.12;        // high-speed floor ratio (0.05 - 0.35)
        public double offroadCapability = 1.0;     // 0.4 (low supercar) to 2.2 (4x4 offroad truck)
        public float brakePower = 18000.0F;        // braking force
        public float suspensionStiffness = 1.0F;   // 0.4 (bouncy/soft) to 3.0 (stiff race)
        public float suspensionDamping = 1.0F;     // 0.3 (bouncy oscillations) to 2.5 (critically damped)
        public double maxSuspensionTravel = 0.15;  // 0.05 to 0.35m (max dip / travel)
        public float bodyRollFactor = 1.0F;        // body roll lean in turns (0.1 to 2.5)
        public float pitchDiveFactor = 1.0F;       // dive on braking & squat on launch (0.1 to 2.5)
        public double antiRollBarStiffness = 1.45; // ARB anti-roll stiffness (0.4 to 3.5)
        public double rideHeight = 0.0;            // ground clearance offset (-0.15 to +0.25m)
        public double weightDipSensitivity = 1.0;  // chassis dip sensitivity under player weight (0.2 to 3.0)
        public double fuelCapacityLiters = 60.0;
    }

    public static class Wheels {
        public double radius = 0.40;
        public double wheelHp = 150.0;
        public float[] frontLeft  = new float[]{ 0.95F, 0.38F,  1.50F };
        public float[] frontRight = new float[]{-0.95F, 0.38F,  1.50F };
        public float[] rearLeft   = new float[]{ 0.95F, 0.38F, -1.50F };
        public float[] rearRight  = new float[]{-0.95F, 0.38F, -1.50F };
    }

    public static class SteeringWheel {
        public boolean enabled = true;
        public float[] pos = new float[]{-0.45F, 0.95F, 0.20F};
        public float tiltAngleDeg = 0.0F;
        public float rotationMultiplier = 15.0F; // ~1.5 - 1.7 full turns each way (lock-to-lock ~3.2 turns, classic non-power-steering)
    }

    public static class SeatDef {
        public int seatIndex = 0;
        public String name = "Seat";
        public float[] pos = new float[]{0.0F, 0.22F, 0.0F};
        public boolean isDriver = false;
        public boolean allowWeapons = true;
        public boolean lockYaw = true;               // false = 360 free camera/turret aim
        public float yawOffsetDeg = 0.0F;            // Rotation offset (e.g. 180 for back-facing seats)
        public float[] dismountOffset = new float[]{1.2F, 0.0F, 0.0F}; // Safe dismount exit position
        public boolean canDismount = true;
        public boolean isCargoBed = false;
    }

    public static class HitboxConfig {
        public double bodyDamageMultiplier = 0.55;    // Урон по кузову/раме машины
        public double wheelDamageMultiplier = 0.12;   // Минимальный урон по ХП машины при попадании в колесо (пробивает шину, но наносит мало урона самой машине)
        public double windowDamageMultiplier = 1.00;  // 100% урон через окно/кабину
        public double engineDamageMultiplier = 0.85;  // Урон по двигателю
        public double trunkDamageMultiplier = 0.45;   // Урон по багажнику
        public boolean passDamageToPassengers = true; // Прострел стекла наносит урон пассажирам
        public boolean instantTirePopOnBullet = true; // Мгновенный прокол при попадании в шину
        public double tireHealth = 15.0;              // Запас прочности шины
        public double wheelHp = 150.0;                // Запас прочности колеса до отрыва

        // Element scale & hitbox box dimensions [width, height, length]
        public float[] cabinSize = new float[]{1.70F, 0.85F, 1.30F};
        public float[] hoodSize = new float[]{1.70F, 0.70F, 1.40F};
        public float[] trunkSize = new float[]{1.70F, 0.70F, 1.20F};
        public float[] chassisSize = new float[]{1.85F, 0.55F, 3.80F};
    }

    public float getMaxWheelHp() {
        if (this.hitboxes != null && this.hitboxes.wheelHp > 0) return (float) this.hitboxes.wheelHp;
        if (this.wheels != null && this.wheels.wheelHp > 0) return (float) this.wheels.wheelHp;
        return 150.0F;
    }

    public static class SoundConfig {
        public String engineType = "i6"; // "i6", "legacy_pickup"
        public float masterVolume = 1.0F;
        public float pitchMultiplier = 1.0F;

        // RPM transition points (RPM-driven sound architecture!)
        public float rpmIdleToLow = 1800.0F;
        public float rpmLowToMid = 3300.0F;
        public float rpmMidToHigh = 4800.0F;
        public float rpmHighToMax = 6200.0F;

        // Speed transition points (fallback / legacy)
        public float speedIdleToLow = 22.0F;
        public float speedLowToMid = 62.0F;
        public float speedMidToHigh = 95.0F;
        public float speedHighToMax = 130.0F;

        public String crossfadeCurve = "equal_power";
        public float temporalSmoothing = 0.22F;

        // Tire blowout & explosion sound settings (+30%)
        public String tireExplodeSound = "cardecor:tire_explode";
        public float tireExplodeVolume = 1.65F;
        public float tireExplodePitch = 1.0F;

        // Tire gradual deflation sound settings (+30%)
        public String tireDeflateSound = "cardecor:tire_deflate";
        public float tireDeflateVolume = 1.25F;
        public float tireDeflatePitch = 1.0F;

        // Custom interaction sounds
        public String trunkOpen = null;
        public String trunkClose = null;
        public String horn = null;
    }

    public static class EngineStartConfig {
        public double startChance = 1.0; // 1.0 = 100% first time start, 0.65 = 65% chance
        public int crankTicks = 18;       // Duration of cranking before engine catches
        public boolean failAllowed = false;
    }

    public EngineConfig engine = new EngineConfig();

    public static class EngineConfig {
        public String id = "default_engine";
        public String name = "Standard Engine";
        public String type = "i6"; // "i6", "legacy_pickup", "v8", "v6", "i4", "diesel", "electric"
        public double horsepower = 200.0;
        public double torqueNm = 300.0;
        public double idleRpm = 850.0;
        public double redlineRpm = 7200.0;
        public String aspiration = "naturally_aspirated"; // "na", "turbo", "supercharger", "twin_turbo"
        public double boostPressureBar = 1.2;
        public double turboSpoolRpm = 2500.0;
        public double turboPeakRpm = 4800.0;
        public double superchargerPulley = 1.8;
        public double fuelConsumption = 1.0;

        public EngineStartConfig starter = new EngineStartConfig();
        public SoundConfig sounds = new SoundConfig();
        public AspirationSounds aspirationSounds = new AspirationSounds();
    }

    public static class AspirationSounds {
        public String type = "none"; // "turbo", "supercharger", "none"
        public float turboVolume = 0.85F;
        public float superchargerVolume = 0.90F;
        public float blowOffVolume = 1.0F;
        public boolean enableBlowOff = true;
        public boolean enableWhine = true;
    }
}
