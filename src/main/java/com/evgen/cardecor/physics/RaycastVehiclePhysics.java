package com.evgen.cardecor.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * Universal Vehicle Physics Engine for CarDecor.
 * Designed from the ground up using principles from:
 * - Minecraft Transport Simulator (MTS): Dual-regime road adhesion vs kinetic skid friction, clean vector integration.
 * - DynamX: Direct responsive steering rack, four-wheel normal load distribution, suspension droop/travel.
 * - Assetto Corsa: Dynamic 3D longitudinal & lateral weight transfer, trail-braking front-axle bite,
 *   friction circle lateral acceleration limits (omega_max = mu * g / |v|).
 *
 * All specific vehicle characteristics are configured purely through external vehicle JSON packs (cardecor_packs).
 */
public class RaycastVehiclePhysics {

    public enum SurfaceType {
        ASPHALT(1.00, 1.00, 1.00, 1.00, 0.95), // Asphalt, Concrete, Stone, Brick, Road
        DIRT(0.72, 0.70, 0.65, 0.75, 0.15),    // Dirt, Path, Farmland, Coarse Dirt, Podzol
        GRAVEL(0.65, 0.60, 0.55, 0.70, 0.10),  // Gravel, Loose rock
        SAND(0.58, 0.55, 0.50, 1.45, 0.05),    // Sand, Red Sand, Soul Sand
        MUD(0.50, 0.50, 0.45, 1.65, 0.05),     // Mud, Muddy mangrove
        SNOW(0.45, 0.40, 0.35, 0.90, 0.05),    // Snow, Snow block, Powder snow
        ICE(0.38, 0.32, 0.25, 0.50, 0.02),     // Ice, Packed Ice, Blue Ice, Frosted Ice
        GRASS(0.82, 0.80, 0.75, 1.05, 0.15);   // Grass, Moss, Leaves

        public final double gripMult;       // Longitudinal grip (acceleration & braking)
        public final double lateralGrip;    // Cornering bite / resistance to sliding
        public final double driftFriction;  // Lateral friction during slide (higher = more scrub, recovers faster)
        public final double rollingDrag;    // Rolling resistance
        public final double screechFactor;  // Tire screech sound multiplier

        SurfaceType(double gripMult, double lateralGrip, double driftFriction, double rollingDrag, double screechFactor) {
            this.gripMult = gripMult;
            this.lateralGrip = lateralGrip;
            this.driftFriction = driftFriction;
            this.rollingDrag = rollingDrag;
            this.screechFactor = screechFactor;
        }
    }

    public SurfaceType currentSurface = SurfaceType.ASPHALT;

    // Static Steer Tuners for command adjustments (/vehicle_steer)
    public static double steerLow = 1.0;
    public static double steerMid = 1.0;
    public static double steerHigh = 1.0;
    public static double steerGlobal = 1.0;

    // Live Yaw & Drift Telemetry
    public double deltaYaw = 0.0;
    public double liveYawRate = 0.0;
    public double lateralSlipVel = 0.0;
    public boolean isDrifting = false;
    public boolean isSpinningOut = false;
    public boolean isBeingPushed = false;
    public boolean hasDriver = false;
    public double driftAngleDeg = 0.0;
    public double continuousDriftTime = 0.0;

    // General Dimensions & Physics (Driven from JSON packs)
    public double mass = 1400.0;
    public double horsepower = 150.0;
    public double torqueNm = 240.0;
    public double enginePower = 16500.0;
    public double maxEngineForce = 16500.0;
    public double brakePower = 28000.0;
    public double maxBrakeForce = 28000.0;
    public double brakePowerMultiplier = 1.0;
    public double maxNormalSpeedKmh = 120.0;
    public double maxBoostSpeedKmh = 190.0;
    public double boostSpeedKmh = 190.0;
    public int maxGears = 5;
    public int gears = 5;
    public double finalDrive = 0.0;
    public double[] customGearRatios = null;
    public double customReverseRatio = 0.0;

    // Steering Kinematics & Agility
    public double maxSteerAngle = Math.toRadians(38.0);
    public double steeringResponse = 1.0;
    public double turnInBite = 1.0;
    public double steeringAgility = 1.0;
    public double steerCurveSpeed = 1.0;
    public double steerCurvePower = 1.0;
    public double steerMinRatio = 0.5;

    // Suspension & Body Dynamics
    public double offroadCapability = 1.0;
    public double suspensionStiffness = 1.0;
    public double suspensionDamping = 1.0;
    public double maxSuspensionTravel = 0.15;
    public double bodyRollFactor = 1.0;
    public double pitchDiveFactor = 1.0;
    public double rideHeight = 0.0;
    public double weightDipSensitivity = 1.0;
    public double fuelCapacityLiters = 65.0;

    // Aerodynamics
    public double dragCoefficient = 0.35;
    public double frontalArea = 2.2;
    public double downforceCoefficient = 0.15;
    public boolean autoMaxSpeed = true;

    // Forced Induction & Engine Spec
    public String aspirationType = "na"; // na, turbo, twin_turbo, supercharger
    public double boostPressureBar = 1.2;
    public double liveBoostPressure = 0.0;
    public double currentBoostBar = 0.0;
    public double turboSpoolRpm = 2200.0;
    public double turboPeakRpm = 4600.0;
    public double superchargerPulley = 1.80;
    public double idleRpm = 850.0;
    public double redlineRpm = 7200.0;

    // Physical / Thermodynamic Engine Specification (BeamNG / Assetto Corsa style)
    public int engineCylinders = 8;
    public double engineBoreMm = 101.6;
    public double engineStrokeMm = 76.2;
    public double engineCompressionRatio = 8.4;
    public String engineValvetrainType = "ohv_pushrod"; // ohv_pushrod, sohc_2v, dohc_4v
    public String engineCamProfile = "truck_towing"; // truck_towing, street_stock, race_aggressive
    public double valveFloatRpm = 4800.0;
    public double flywheelMassKg = 18.5;
    public double flywheelRadiusM = 0.16;
    public double flywheelInertia = 0.35; // kg * m^2
    public String engineLimiterType = "valve_float"; // valve_float, hard_cut, soft_cut, diesel_governor
    public double displacementLiters = 4.942;
    public double thermalEfficiency = 0.505;
    public double torquePerBarBmep = 39.33; // Nm per bar
    public double hardCutTimer = 0.0;

    // Modular Tires Grip Properties
    public double tireGripAsphalt = 1.0;
    public double tireGripDirt = 0.85;
    public double tireGripIce = 0.70;

    public String drivetrain = "rwd"; // rwd, fwd, awd, 4wd
    public String engineType = "inline6";

    // Chassis Geometry & Bounds
    public double wheelbase = 2.85;
    public double trackWidth = 1.75;
    public double wheelRadius = 0.35;
    public double frontOverhang = 0.85;
    public double rearOverhang = 0.95;
    public double frontBumperDist = 2.85;
    public double rearBumperDist = 2.85;
    public double halfWidth = 1.25;
    public double weightDistributionFront = 0.52;
    public double centerOfGravityHeight = 0.55;
    public double bodyShakeIntensity = 1.0;
    public double cameraShakeIntensity = 1.0;
    public double cockpitShakeIntensity = 1.0;

    // Suspension Droop, ARB & Wheel Air
    public double[] wheelCompression = new double[]{0, 0, 0, 0};
    public double antiRollBarStiffness = 1.45;
    public boolean blowOffValveTriggered = false;
    public double lastBoostPressure = 0.0;

    public double tirePressureFL = 2.2;
    public double tirePressureFR = 2.2;
    public double tirePressureRL = 2.2;
    public double tirePressureRR = 2.2;

    public float[] tireAir = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    public float[] tireDeflateRate = new float[]{0, 0, 0, 0};
    public float[] tireCondition = new float[]{100.0f, 100.0f, 100.0f, 100.0f};
    public byte installedWheelsMask = (byte) 15; // bitmask: 1=FL, 2=FR, 4=RL, 8=RR

    public boolean isWheelInstalled(int index) {
        if (index < 0 || index > 3) return false;
        return (installedWheelsMask & (1 << index)) != 0;
    }

    // Live Dynamic State
    public double speed = 0.0;
    public double lateralSpeed = 0.0;
    public double driveWheelLinearSpeed = 0.0;
    public double steerAngle = 0.0;
    public double roll = 0.0;
    public double pitch = 0.0;
    public double groundRoll = 0.0;
    public double groundPitch = 0.0;
    public double terrainPitch = 0.0;
    public double rollVelocity = 0.0;
    public double pitchVelocity = 0.0;

    public double engineRPM = 0.0;
    public float enginePitch = 1.0f;
    public float tireScreechIntensity = 0.0f;
    public int currentGear = 1;
    public int prevGear = 1;
    public double gearShiftTimer = 0.0;
    public double totalShiftTime = 0.20;
    public double shiftHoldTimer = 0.0;
    public double upshiftDwellTimer = 0.0;
    public int pendingUpshiftGear = 0;
    public double shiftSurgeTimer = 0.0;
    public boolean justShifted = false;
    public boolean shiftPopTriggered = false;
    public double boost = 0.0;

    public boolean isFlipped = false;
    public boolean isBurnoutActive = false;
    public boolean isParkingBrake = false;
    public boolean hasPowerToBreakTraction = false;
    public boolean hasBurnoutPower = false;
    public double prevSteerInput = 0.0;
    public double flickSteerHold = 0.0;
    public double flickTimer = 0.0;
    public boolean isFlickActive = false;
    public boolean inWater = false;
    public double waterDepth = 0.0;
    public boolean isGrounded = true;
    public boolean wasGrounded = true;
    public int groundedWheelCount = 4;
    public boolean isPhysicallySupported = true;

    public double slipMotionX = 0.0;
    public double slipMotionY = 0.0;
    public double slipMotionZ = 0.0;

    public double suspensionClimbLift = 0.0;
    public double verticalSpeed = 0.0;
    public double airborneTime = 0.0;
    public double fallStartHeight = 0.0;
    public double lastFallBlocks = 0.0;
    public double acceleration = 0.0;
    public double suspensionCompression = 0.0;
    public double shiftJoltPitch = 0.0;
    public boolean isWallHit = false;
    public boolean wallInFront = false;
    public boolean isFrontObstacle = false;
    public boolean isRearObstacle = false;
    public double targetSurfaceY = -999.0;
    public double crashCooldown = 0.0;
    public double reboundTimer = 0.0;
    public double lastImpactSpeedKmh = 0.0;
    public Vec3 lastImpactPoint = Vec3.ZERO;
    public boolean justLandedHeavy = false;
    public boolean justLandedLight = false;
    public boolean justSuspensionCreak = false;
    public double landingImpactForce = 0.0;
    public double suspensionCreakCooldown = 0.0;
    private double prevHeightDelta = 0.0;
    private double prevRollHeightDelta = 0.0;
    private final double[] prevSuspensionCompressions = new double[]{0, 0, 0, 0};

    // Sprung Chassis 2nd-Order Spring-Damper Dynamics (Dive, Squat, Body Sway)
    public double chassisDipY = 0.0;
    public double chassisDipPitch = 0.0;
    public double chassisDipRoll = 0.0;
    public double extraStaticDipY = 0.0;
    public double extraStaticPitch = 0.0;
    public double extraStaticRoll = 0.0;
    public double chassisPitchVel = 0.0;
    public double chassisRollVel = 0.0;
    public double chassisYVel = 0.0;

    public void applyChassisImpulse(double deltaY, double deltaPitchRad, double deltaRollRad) {
        applyChassisImpulse(deltaY, deltaY * 16.0, deltaPitchRad, deltaRollRad);
    }

    public void applyChassisImpulse(double deltaY, double deltaYVel, double deltaPitchRad, double deltaRollRad) {
        if (Double.isNaN(deltaY) || Double.isInfinite(deltaY)) deltaY = 0.0;
        if (Double.isNaN(deltaYVel) || Double.isInfinite(deltaYVel)) deltaYVel = 0.0;
        if (Double.isNaN(deltaPitchRad) || Double.isInfinite(deltaPitchRad)) deltaPitchRad = 0.0;
        if (Double.isNaN(deltaRollRad) || Double.isInfinite(deltaRollRad)) deltaRollRad = 0.0;
        double maxDip = Math.max(0.05, this.maxSuspensionTravel);
        this.chassisDipY = Mth.clamp(this.chassisDipY + deltaY, -maxDip, maxDip * 0.5);
        this.chassisYVel = Mth.clamp(this.chassisYVel + deltaYVel, -4.0, 4.0);
        this.chassisDipPitch = Mth.clamp(this.chassisDipPitch + deltaPitchRad, -maxDip * 0.8, maxDip * 0.8);
        this.chassisPitchVel = Mth.clamp(this.chassisPitchVel + deltaPitchRad * 14.0 * Math.sqrt(Math.max(0.2, this.suspensionStiffness)), -3.0, 3.0);
        this.chassisDipRoll = Mth.clamp(this.chassisDipRoll + deltaRollRad, -maxDip * 0.8, maxDip * 0.8);
        this.chassisRollVel = Mth.clamp(this.chassisRollVel + deltaRollRad * 14.0 * Math.sqrt(Math.max(0.2, this.suspensionStiffness)), -3.0, 3.0);
    }

    // Detailed Local Impact Coordinates for Mesh Deformation
    public double impactLocalX = 0.0;
    public double impactLocalY = 0.0;
    public double impactLocalZ = 0.0;
    public double impactDirX = 0.0;
    public double impactDirY = 0.0;
    public double impactDirZ = 0.0;

    // 4-Wheel Contact & Normal Force Arrays
    public double[] wheelRotation = new double[]{0, 0, 0, 0};
    public double[] suspensionCompressions = new double[]{0, 0, 0, 0};
    public double[] wheelNormalForces = new double[]{0, 0, 0, 0};
    public double[] wheelContactPatches = new double[]{1.0, 1.0, 1.0, 1.0};
    public boolean[] wheelLifted = new boolean[]{false, false, false, false};

    public double getWheelNormalForce(int index) {
        if (index < 0 || index >= 4) return 0.0;
        return wheelNormalForces[index];
    }

    public double getWheelContactPatch(int index) {
        if (index < 0 || index >= 4) return 0.0;
        return wheelContactPatches[index];
    }

    public boolean isWheelLifted(int index) {
        if (index < 0 || index >= 4) return false;
        return wheelLifted[index];
    }

    private double cachedHFL = 0.0;
    private double cachedHFR = 0.0;
    private double cachedHRL = 0.0;
    private double cachedHRR = 0.0;
    private double cachedHMid = 0.0;
    private double cachedHBelly = 0.0;
    private int groundSampleTick = 0;

    public double getEffectiveHorsepower() {
        return Math.max(5.0, this.horsepower);
    }

    public double getFinalDriveRatio() {
        if (this.finalDrive > 0.0) {
            return this.finalDrive;
        }
        // Mechanical engineering derivation: calculate axle ratio to reach top speed at engine power peak
        double rWheel = Math.max(0.20, this.wheelRadius);
        double maxSpeedMs = Math.max(10.0, this.maxNormalSpeedKmh) / 3.6;
        double powerPeakRpm = this.idleRpm + (this.redlineRpm - this.idleRpm) * 0.60;
        double topTotalRatio = (powerPeakRpm * 2.0 * Math.PI * rWheel) / (maxSpeedMs * 60.0);
        int maxG = Math.max(1, this.maxGears);
        double topGearRatio = (maxG <= 4) ? 1.00 : 0.78;
        double calculatedDiff = topTotalRatio / topGearRatio;
        return Mth.clamp(calculatedDiff, 2.40, 5.20);
    }

    public double getTotalGearRatio(int gear) {
        double finalDriveRatio = getFinalDriveRatio();
        if (gear == -1) {
            if (this.customReverseRatio > 0.0) {
                return -finalDriveRatio * this.customReverseRatio;
            }
            return -Math.abs(getTotalGearRatio(1)) * 0.95;
        }

        // 1. Author-configured custom gear ratios from vehicle JSON
        if (this.customGearRatios != null && this.customGearRatios.length > 0) {
            int gIdx = Mth.clamp(gear, 1, this.customGearRatios.length) - 1;
            return this.customGearRatios[gIdx] * finalDriveRatio;
        }

        // 2. Physical automotive mechanical derivation (Geometric Progression):
        // If ratios are not specified in JSON, calculate authentic mechanical progression
        int maxG = Math.max(1, this.maxGears);
        int curG = Mth.clamp(gear, 1, maxG);

        double rWheel = Math.max(0.20, this.wheelRadius);
        double maxSpeedMs = Math.max(10.0, this.maxNormalSpeedKmh) / 3.6;
        double powerPeakRpm = this.idleRpm + (this.redlineRpm - this.idleRpm) * 0.60;
        double topTotalRatio = (powerPeakRpm * 2.0 * Math.PI * rWheel) / (maxSpeedMs * 60.0);

        if (maxG == 1) {
            return topTotalRatio;
        }

        // Mechanical transmission ratio spread (R_spread = i_1 / i_N)
        double spread = 1.90 + (maxG - 1) * 0.62;
        double exponent = (double) (maxG - curG) / (double) (maxG - 1);
        return topTotalRatio * Math.pow(spread, exponent);
    }

    public double getGearRatio(int gear) {
        return getTotalGearRatio(gear) / getFinalDriveRatio();
    }

    public double getKinematicRpm(double speedMs, int gear) {
        double rWheel = Math.max(0.20, this.wheelRadius);
        double totalRatio = Math.abs(getTotalGearRatio(gear));
        // Kinematic equation: RPM = (v * 60 * i_total) / (2 * pi * r_wheel)
        return (Math.abs(speedMs) * 60.0 * totalRatio) / (2.0 * Math.PI * rWheel);
    }

    public void applyEngineDefinition(com.evgen.cardecor.pack.EngineDefinition eng) {
        if (eng == null) return;
        if (eng.type != null && !eng.type.isEmpty()) {
            this.engineType = eng.type;
        }
        if (eng.physics != null) {
            this.horsepower = eng.physics.horsepower;
            this.torqueNm = eng.physics.torqueNm;
            this.idleRpm = eng.physics.idleRpm;
            this.redlineRpm = eng.physics.redlineRpm;
            this.maxEngineForce = eng.physics.horsepower * 180.0;
        }
        if (eng.block != null) {
            this.engineCylinders = Math.max(1, eng.block.cylinders);
            this.engineBoreMm = Math.max(30.0, eng.block.boreMm);
            this.engineStrokeMm = Math.max(30.0, eng.block.strokeMm);
            this.engineCompressionRatio = Math.max(6.0, eng.block.compressionRatio);
        }
        if (eng.valvetrain != null) {
            if (eng.valvetrain.type != null) this.engineValvetrainType = eng.valvetrain.type;
            if (eng.valvetrain.camProfile != null) this.engineCamProfile = eng.valvetrain.camProfile;
            this.valveFloatRpm = Math.max(2000.0, eng.valvetrain.valveFloatRpm);
        }
        if (eng.flywheel != null) {
            this.flywheelMassKg = Math.max(3.0, eng.flywheel.massKg);
            this.flywheelRadiusM = Math.max(0.08, eng.flywheel.radiusM);
        }
        if (eng.limits != null) {
            if (eng.limits.limiterType != null) this.engineLimiterType = eng.limits.limiterType;
            if (eng.limits.idleRpm > 0) this.idleRpm = eng.limits.idleRpm;
            if (eng.limits.redlineRpm > 0) this.redlineRpm = eng.limits.redlineRpm;
            if (eng.limits.maxRpm > 0) this.valveFloatRpm = eng.limits.maxRpm;
        }
        if (eng.forcedInduction != null) {
            if (eng.forcedInduction.type != null) this.aspirationType = eng.forcedInduction.type;
            if (eng.forcedInduction.boostTargetBar > 0) this.boostPressureBar = eng.forcedInduction.boostTargetBar;
            if (eng.forcedInduction.spoolRpm > 0) this.turboSpoolRpm = eng.forcedInduction.spoolRpm;
        }

        // Precompute physical constants:
        // Vd = Cylinders * (pi/4) * bore^2 * stroke (in liters)
        double boreM = this.engineBoreMm / 1000.0;
        double strokeM = this.engineStrokeMm / 1000.0;
        this.displacementLiters = this.engineCylinders * (Math.PI / 4.0) * boreM * boreM * strokeM * 1000.0;
        if (this.displacementLiters < 0.5) this.displacementLiters = 4.942;

        // Thermal efficiency: eta_th = 1 - r_c^(-0.33)
        this.thermalEfficiency = 1.0 - Math.pow(this.engineCompressionRatio, -0.33);

        // Flywheel rotational inertia: I = 0.5 * m * r^2 * 1.35 (includes crank web inertia)
        this.flywheelInertia = 0.5 * this.flywheelMassKg * (this.flywheelRadiusM * this.flywheelRadiusM) * 1.35;

        // Torque factor: C_tau = (10^5 * Vd_m3) / (4 * pi) [Nm per bar BMEP]
        double vdM3 = this.displacementLiters / 1000.0;
        this.torquePerBarBmep = (100000.0 * vdM3) / (4.0 * Math.PI);
    }

    public double getRotatingMassFactor(int gear) {
        double rWheel = Math.max(0.20, this.wheelRadius);
        double mr2 = Math.max(10.0, this.mass * rWheel * rWheel);
        double iWheels = 4.0 * (0.5 * 18.0 * rWheel * rWheel); // 4 wheels moment of inertia
        double iEngine = Math.max(0.08, this.flywheelInertia); // engine flywheel + crankshaft rotational inertia
        double totalRatio = Math.abs(getTotalGearRatio(gear));
        // Classical Gillespie formula: delta = 1 + I_wheels/(m*r^2) + I_engine * i_total^2 / (m*r^2)
        double delta = 1.0 + (iWheels / mr2) + (iEngine * totalRatio * totalRatio) / mr2;
        return Mth.clamp(delta, 1.0, 1.25);
    }

    public double getEffectiveTorqueNm() {
        if (this.torqueNm > 0.0) {
            return this.torqueNm;
        }
        double netHp = getEffectiveHorsepower();
        // Standard internal combustion engine: T = P * 735.5 / (2 * pi * RPM / 60)
        // With peak torque typically around 3800-4200 RPM
        return (netHp * 735.5) / (2.0 * Math.PI * 4000.0 / 60.0);
    }

    public double getTorqueAtRpm(double rpm) {
        double idle = Math.max(500.0, this.idleRpm);
        double redline = Math.max(idle + 1500.0, this.redlineRpm);

        // 1. Mean Piston Speed: v_p = (stroke_mm * RPM) / 30000.0 (m/s)
        double vp = (this.engineStrokeMm * Math.max(idle, rpm)) / 30000.0;

        // 2. Volumetric Efficiency (eta_v) based on Valvetrain & Cam Profile:
        double ve;
        if (this.engineValvetrainType.equalsIgnoreCase("ohv_pushrod")) {
            // Pushrod 2-valve: High low-end gas velocity, peak around 6.0 - 8.5 m/s, drops off past 10 m/s
            if (this.engineCamProfile.equalsIgnoreCase("truck_towing")) {
                // Peak VE ~0.90 at 6.0 m/s (~2350 RPM on 76.2mm stroke), maintaining 0.86 at 1000 RPM (2.54 m/s)
                if (vp <= 6.0) {
                    double p = Math.max(0.0, (vp - 1.5) / 4.5);
                    ve = 0.84 + 0.06 * Math.sin(p * Math.PI * 0.5);
                } else {
                    double p = (vp - 6.0) / 5.5;
                    ve = 0.90 - 0.28 * Math.pow(Math.max(0.0, p), 1.6);
                }
            } else if (this.engineCamProfile.equalsIgnoreCase("race_aggressive")) {
                // High duration cam: lower low-end VE (~0.72), peaks higher at 10.5 m/s (~4100 RPM)
                if (vp <= 10.5) {
                    double p = Math.max(0.0, (vp - 2.0) / 8.5);
                    ve = 0.72 + 0.22 * Math.sin(p * Math.PI * 0.5);
                } else {
                    double p = (vp - 10.5) / 4.0;
                    ve = 0.94 - 0.25 * Math.pow(Math.max(0.0, p), 1.5);
                }
            } else { // street_stock OHV
                if (vp <= 7.5) {
                    double p = Math.max(0.0, (vp - 1.8) / 5.7);
                    ve = 0.81 + 0.08 * Math.sin(p * Math.PI * 0.5);
                } else {
                    double p = (vp - 7.5) / 5.0;
                    ve = 0.89 - 0.26 * Math.pow(Math.max(0.0, p), 1.5);
                }
            }
        } else if (this.engineValvetrainType.equalsIgnoreCase("dohc_4v")) {
            // DOHC 4-valve: Free breathing at high piston speeds (13-17 m/s, 5000-6500 RPM)
            if (vp <= 14.5) {
                double p = Math.max(0.0, (vp - 2.0) / 12.5);
                ve = 0.74 + 0.22 * Math.sin(p * Math.PI * 0.5);
            } else {
                double p = (vp - 14.5) / 5.0;
                ve = 0.96 - 0.20 * Math.pow(Math.max(0.0, p), 1.4);
            }
        } else { // sohc_2v or standard
            if (vp <= 9.5) {
                double p = Math.max(0.0, (vp - 1.8) / 7.7);
                ve = 0.78 + 0.12 * Math.sin(p * Math.PI * 0.5);
            } else {
                double p = (vp - 9.5) / 5.0;
                ve = 0.90 - 0.22 * Math.pow(Math.max(0.0, p), 1.5);
            }
        }
        ve = Mth.clamp(ve, 0.40, 1.10);

        // 3. Friction Mean Effective Pressure (FMEP):
        // FMEP = 0.45 + 0.035 * vp + 0.003 * vp^2 (bar)
        double fmep = 0.45 + 0.035 * vp + 0.003 * (vp * vp);

        // 4. Indicated Mean Effective Pressure (IMEP):
        // IMEP = ve * (eta_th / 0.50) * 10.6 bar
        double etaRatio = Math.max(0.85, this.thermalEfficiency / 0.50);
        double imep = ve * etaRatio * 10.6;

        // 5. Brake Mean Effective Pressure (BMEP):
        double bmep = Math.max(0.0, imep - fmep);

        // 6. Thermodynamic Torque: T = BMEP * C_tau
        double thermoTorque = bmep * this.torquePerBarBmep;

        // Calibration scaling: if torqueNm was explicitly specified in config, calibrate curve to peak
        double resultTorque;
        if (this.torqueNm > 0.0) {
            double calibFactor = this.torqueNm / Math.max(10.0, 8.8 * this.torquePerBarBmep);
            resultTorque = thermoTorque * calibFactor;
        } else {
            resultTorque = thermoTorque;
        }

        // 7. Rev Limiter:
        if (rpm > redline) {
            if (this.engineLimiterType.equalsIgnoreCase("valve_float")) {
                // Valve float: valve bounce causes progressive pressure drop
                double floatRange = Math.max(150.0, this.valveFloatRpm - redline);
                double overRev = (rpm - redline) / floatRange;
                double floatCut = Math.max(0.12, 1.0 - overRev * 1.1);
                resultTorque *= floatCut;
            } else if (this.engineLimiterType.equalsIgnoreCase("hard_cut")) {
                // Hard cut: ignition/fuel cut completely
                resultTorque = 0.0;
            } else { // soft_cut
                double overRev = (rpm - redline) / 350.0;
                resultTorque *= Math.max(0.0, 1.0 - overRev);
            }
        }

        return Math.max(0.0, resultTorque);
    }

    public double getDrivenAxleMaxGripForce() {
        double totalWeight = this.mass * 9.81;
        double axleWeight;
        if (this.drivetrain.equalsIgnoreCase("awd") || this.drivetrain.equalsIgnoreCase("4wd")) {
            axleWeight = totalWeight;
        } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
            axleWeight = totalWeight * Mth.clamp(this.weightDistributionFront, 0.35, 0.70);
        } else { // RWD default
            axleWeight = totalWeight * (1.0 - Mth.clamp(this.weightDistributionFront, 0.35, 0.70));
            if (this.acceleration > 0.0) {
                double hCg = Math.max(0.20, this.centerOfGravityHeight);
                double effWb = Math.max(1.0, this.wheelbase);
                double dFz = (this.mass * this.acceleration * hCg) / effWb;
                axleWeight += dFz;
            }
        }
        double tireMu = 1.15;
        double surfaceGrip = Math.max(0.20, this.currentSurface.gripMult);
        return axleWeight * tireMu * surfaceGrip;
    }

    public double getFirstGearDriveForce() {
        double tEng = getEffectiveTorqueNm();
        double firstGearTotalRatio = getTotalGearRatio(1);
        double efficiency = 0.86;
        double rWheel = Math.max(0.20, this.wheelRadius);
        return (tEng * firstGearTotalRatio * efficiency) / rWheel;
    }

    // =========================================================================
    // MAIN VEHICLE PHYSICS STEP
    // =========================================================================
    public Vec3 step(Level level, Vec3 pos, float yRot, double throttle, double steer, boolean handbrake, boolean boostInput, boolean onGround, boolean engineOn, double dt) {
        dt = Math.max(0.001, Math.min(dt, 0.10));
        groundSampleTick++;

        // Tire Deflation update
        for (int i = 0; i < 4; i++) {
            if (tireDeflateRate[i] > 0) {
                tireAir[i] = Math.max(0.0f, tireAir[i] - (float)(tireDeflateRate[i] * dt));
            }
        }

        float yawRad = (float) Math.toRadians(yRot);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double forwardX = -sinYaw;
        double forwardZ = cosYaw;
        double leftX = cosYaw;
        double leftZ = sinYaw;

        // 1. TERRAIN RAYCAST SAMPLING FOR ALL 4 WHEELS + PREDICTIVE BUMPER PROBES
        double halfW = Math.max(0.65, this.halfWidth > 0.1 ? this.halfWidth * 0.70 : this.trackWidth * 0.5);
        double lFront = Math.max(0.8, this.wheelbase * (1.0 - this.weightDistributionFront));
        double lRear = Math.max(0.8, this.wheelbase * this.weightDistributionFront);

        double wheelFwdLookahead = (this.speed > 0.05 || throttle > 0.05) ? 0.35 : 0.0;
        double wheelRearLookahead = (this.speed < -0.05 || throttle < -0.05) ? 0.35 : 0.0;

        // Actual wheel positions (leftX is Left (+X local), -leftX is Right (-X local))
        double flX = pos.x + leftX * halfW + forwardX * (lFront + wheelFwdLookahead);
        double flZ = pos.z + leftZ * halfW + forwardZ * (lFront + wheelFwdLookahead);
        double frX = pos.x - leftX * halfW + forwardX * (lFront + wheelFwdLookahead);
        double frZ = pos.z - leftZ * halfW + forwardZ * (lFront + wheelFwdLookahead);
        double rlX = pos.x + leftX * halfW - forwardX * (lRear + wheelRearLookahead);
        double rlZ = pos.z + leftZ * halfW - forwardZ * (lRear + wheelRearLookahead);
        double rrX = pos.x - leftX * halfW - forwardX * (lRear + wheelRearLookahead);
        double rrZ = pos.z - leftZ * halfW - forwardZ * (lRear + wheelRearLookahead);

        double maxClimbStep = Math.max(1.15, 1.15 + 0.35 * Math.max(0.0, this.offroadCapability - 1.0));

        double bumperFrontDist = Math.max(lFront + 0.50, this.frontBumperDist > 0.5 ? this.frontBumperDist : lFront + this.frontOverhang);
        double bumperRearDist  = Math.max(lRear + 0.50, this.rearBumperDist > 0.5 ? this.rearBumperDist : lRear + this.rearOverhang);
        double probeHalfW = Math.max(0.65, this.halfWidth > 0.1 ? this.halfWidth * 0.78 : this.trackWidth * 0.45);

        double midFwdX = pos.x + forwardX * (lFront + wheelFwdLookahead);
        double midFwdZ = pos.z + forwardZ * (lFront + wheelFwdLookahead);
        double midProbeX = pos.x + forwardX * bumperFrontDist;
        double midProbeZ = pos.z + forwardZ * bumperFrontDist;
        double bellyX = pos.x;
        double bellyZ = pos.z;

        boolean isParkedAtRest = (Math.abs(this.speed) < 0.005 && Math.abs(throttle) < 0.01 && Math.abs(steer) < 0.01 && this.isPhysicallySupported && this.isGrounded && this.cachedHFL > -900.0);
        double hProbeFL = cachedHFL;
        double hProbeMid = cachedHMid;
        double hProbeFR = cachedHFR;
        double hProbeRL = cachedHRL;
        double hProbeRR = cachedHRR;

        if (!isParkedAtRest || (groundSampleTick % 20 == 0)) {
            cachedHFL = sampleGroundFast(level, flX, pos.y, flZ, maxClimbStep);
            cachedHFR = sampleGroundFast(level, frX, pos.y, frZ, maxClimbStep);
            cachedHRL = sampleGroundFast(level, rlX, pos.y, rlZ, maxClimbStep);
            cachedHRR = sampleGroundFast(level, rrX, pos.y, rrZ, maxClimbStep);
            cachedHMid = sampleGroundFast(level, midFwdX, pos.y, midFwdZ, maxClimbStep);
            cachedHBelly = sampleGroundFast(level, bellyX, pos.y, bellyZ, maxClimbStep);
            this.currentSurface = getSurfaceType(sampleSurfaceBlock(level, pos.x, pos.y, pos.z));

            if (Math.abs(this.speed) > 0.05 || Math.abs(throttle) > 0.05) {
                hProbeFL = sampleGroundFast(level, pos.x + leftX * probeHalfW + forwardX * bumperFrontDist, pos.y, pos.z + leftZ * probeHalfW + forwardZ * bumperFrontDist, maxClimbStep);
                hProbeMid = sampleGroundFast(level, midProbeX, pos.y, midProbeZ, maxClimbStep);
                hProbeFR = sampleGroundFast(level, pos.x - leftX * probeHalfW + forwardX * bumperFrontDist, pos.y, pos.z - leftZ * probeHalfW + forwardZ * bumperFrontDist, maxClimbStep);
                hProbeRL = sampleGroundFast(level, pos.x + leftX * probeHalfW - forwardX * bumperRearDist, pos.y, pos.z + leftZ * probeHalfW - forwardZ * bumperRearDist, maxClimbStep);
                hProbeRR = sampleGroundFast(level, pos.x - leftX * probeHalfW - forwardX * bumperRearDist, pos.y, pos.z - leftZ * probeHalfW - forwardZ * bumperRearDist, maxClimbStep);
            } else {
                hProbeFL = cachedHFL;
                hProbeMid = cachedHMid;
                hProbeFR = cachedHFR;
                hProbeRL = cachedHRL;
                hProbeRR = cachedHRR;
            }
        }

        // 2. OBSTACLE PROBES: Differentiate between a climbable slope/stairs and an impassable vertical wall
        double frontGroundY = Math.max(cachedHFL, Math.max(cachedHFR, cachedHMid));
        if (frontGroundY < -900.0) frontGroundY = pos.y;
        double rearGroundY = Math.max(cachedHRL, cachedHRR);
        if (rearGroundY < -900.0) rearGroundY = pos.y;

        double approachFL = (hProbeFL > -900.0) ? Math.max(frontGroundY, hProbeFL) : frontGroundY;
        double approachFR = (hProbeFR > -900.0) ? Math.max(frontGroundY, hProbeFR) : frontGroundY;
        double approachMid = (hProbeMid > -900.0) ? Math.max(frontGroundY, hProbeMid) : frontGroundY;

        double lookaheadFwd = Math.max(0.04, Math.min(0.40, Math.abs(this.speed) * dt + 0.02));
        double lookaheadRear = Math.max(0.04, Math.min(0.40, Math.abs(this.speed) * dt + 0.02));
        double crashFwdDist = bumperFrontDist + (this.speed > 0.08 ? lookaheadFwd : 0.04);
        double crashRearDist = bumperRearDist + (this.speed < -0.08 ? lookaheadRear : 0.04);

        // Directional Obstacle Probes: only probe bumper sweeps in the active movement direction
        boolean checkFront = (this.speed > 0.05 || throttle > 0.05);
        boolean checkRear = (this.speed < -0.05 || throttle < -0.05);

        boolean hitFL_Far = false, hitFL_Mid = false, hitFL_In = false, hitFM_Cen = false;
        boolean hitFR_In = false, hitFR_Mid = false, hitFR_Far = false;
        if (checkFront) {
            hitFL_Far = isSolidObstacle(level, pos.x + leftX * probeHalfW + forwardX * crashFwdDist, pos.y, pos.z + leftZ * probeHalfW + forwardZ * crashFwdDist, maxClimbStep, approachFL);
            hitFL_Mid = isSolidObstacle(level, pos.x + leftX * (probeHalfW * 0.66) + forwardX * crashFwdDist, pos.y, pos.z + leftZ * (probeHalfW * 0.66) + forwardZ * crashFwdDist, maxClimbStep, approachFL);
            hitFL_In  = isSolidObstacle(level, pos.x + leftX * (probeHalfW * 0.33) + forwardX * crashFwdDist, pos.y, pos.z + leftZ * (probeHalfW * 0.33) + forwardZ * crashFwdDist, maxClimbStep, approachFL);
            hitFM_Cen = isSolidObstacle(level, pos.x + forwardX * crashFwdDist, pos.y, pos.z + forwardZ * crashFwdDist, maxClimbStep, approachMid);
            hitFR_In  = isSolidObstacle(level, pos.x - leftX * (probeHalfW * 0.33) + forwardX * crashFwdDist, pos.y, pos.z - leftZ * (probeHalfW * 0.33) + forwardZ * crashFwdDist, maxClimbStep, approachFR);
            hitFR_Mid = isSolidObstacle(level, pos.x - leftX * (probeHalfW * 0.66) + forwardX * crashFwdDist, pos.y, pos.z - leftZ * (probeHalfW * 0.66) + forwardZ * crashFwdDist, maxClimbStep, approachFR);
            hitFR_Far = isSolidObstacle(level, pos.x - leftX * probeHalfW + forwardX * crashFwdDist, pos.y, pos.z - leftZ * probeHalfW + forwardZ * crashFwdDist, maxClimbStep, approachFR);
        }

        boolean hitRL_Far = false, hitRL_Mid = false, hitRL_In = false, hitRM_Cen = false;
        boolean hitRR_In = false, hitRR_Mid = false, hitRR_Far = false;
        if (checkRear) {
            hitRL_Far = isSolidObstacle(level, pos.x + leftX * probeHalfW - forwardX * crashRearDist, pos.y, pos.z + leftZ * probeHalfW - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRL_Mid = isSolidObstacle(level, pos.x + leftX * (probeHalfW * 0.66) - forwardX * crashRearDist, pos.y, pos.z + leftZ * (probeHalfW * 0.66) - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRL_In  = isSolidObstacle(level, pos.x + leftX * (probeHalfW * 0.33) - forwardX * crashRearDist, pos.y, pos.z + leftZ * (probeHalfW * 0.33) - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRM_Cen = isSolidObstacle(level, pos.x - forwardX * crashRearDist, pos.y, pos.z - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRR_In  = isSolidObstacle(level, pos.x - leftX * (probeHalfW * 0.33) - forwardX * crashRearDist, pos.y, pos.z - leftZ * (probeHalfW * 0.33) - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRR_Mid = isSolidObstacle(level, pos.x - leftX * (probeHalfW * 0.66) - forwardX * crashRearDist, pos.y, pos.z - leftZ * (probeHalfW * 0.66) - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
            hitRR_Far = isSolidObstacle(level, pos.x - leftX * probeHalfW - forwardX * crashRearDist, pos.y, pos.z - leftZ * probeHalfW - forwardZ * crashRearDist, maxClimbStep, rearGroundY);
        }

        boolean hitSideL = isSolidObstacle(level, pos.x + leftX * (probeHalfW + 0.05), pos.y, pos.z + leftZ * (probeHalfW + 0.05), maxClimbStep, pos.y);
        boolean hitSideR = isSolidObstacle(level, pos.x - leftX * (probeHalfW + 0.05), pos.y, pos.z - leftZ * (probeHalfW + 0.05), maxClimbStep, pos.y);

        boolean hitFrontLeft = (hitFL_Far || hitFL_Mid || hitFL_In);
        boolean hitFrontRight = (hitFR_Far || hitFR_Mid || hitFR_In);
        boolean hitFrontCenter = (hitFM_Cen || hitFL_In || hitFR_In);
        this.wallInFront = (hitFrontCenter || hitFrontLeft || hitFrontRight);
        this.isFrontObstacle = this.wallInFront;

        boolean hitRearLeft = (hitRL_Far || hitRL_Mid || hitRL_In);
        boolean hitRearRight = (hitRR_Far || hitRR_Mid || hitRR_In);
        boolean hitRearCenter = (hitRM_Cen || hitRL_In || hitRR_In);
        this.isRearObstacle = (hitRearCenter || hitRearLeft || hitRearRight);

        // Wheel contact detection across slopes and stairs
        double maxDroop = Math.max(1.20, maxClimbStep + 0.10);
        boolean flContact = (cachedHFL > pos.y - maxDroop && cachedHFL <= pos.y + maxClimbStep + 0.05);
        boolean frContact = (cachedHFR > pos.y - maxDroop && cachedHFR <= pos.y + maxClimbStep + 0.05);
        boolean rlContact = (cachedHRL > pos.y - maxDroop && cachedHRL <= pos.y + maxClimbStep + 0.05);
        boolean rrContact = (cachedHRR > pos.y - maxDroop && cachedHRR <= pos.y + maxClimbStep + 0.05);
        boolean midContact = (cachedHMid > pos.y - maxDroop && cachedHMid <= pos.y + maxClimbStep + 0.05);
        boolean bellyContact = (cachedHBelly > pos.y - maxDroop && cachedHBelly <= pos.y + maxClimbStep + 0.05);

        int totalContacts = (flContact ? 1 : 0) + (frContact ? 1 : 0) + (rlContact ? 1 : 0) + (rrContact ? 1 : 0);

        boolean probeStepFL = (hProbeFL > frontGroundY + 0.04 && hProbeFL <= frontGroundY + maxClimbStep);
        boolean probeStepFR = (hProbeFR > frontGroundY + 0.04 && hProbeFR <= frontGroundY + maxClimbStep);
        boolean probeStepMid = (hProbeMid > frontGroundY + 0.04 && hProbeMid <= frontGroundY + maxClimbStep);
        boolean hasClimbableStep = (probeStepFL || probeStepFR || probeStepMid);

        // A step <= maxClimbStep with headroom is a climbable slope, not an impassable wall
        if (hasClimbableStep) {
            this.wallInFront = false;
            this.isFrontObstacle = false;
        }

        boolean isTrulyOnGround = (totalContacts >= 1) || (midContact || bellyContact) || hasClimbableStep;
        this.groundedWheelCount = Math.max(totalContacts, (midContact || bellyContact) ? 2 : 0);
        this.isPhysicallySupported = isTrulyOnGround;
        this.isGrounded = isTrulyOnGround;

        if (isTrulyOnGround) {
            double sumGroundedY = 0.0;
            int cnt = 0;
            if (flContact) { sumGroundedY += cachedHFL; cnt++; }
            if (frContact) { sumGroundedY += cachedHFR; cnt++; }
            if (rlContact) { sumGroundedY += cachedHRL; cnt++; }
            if (rrContact) { sumGroundedY += cachedHRR; cnt++; }
            if (cnt == 0 && midContact) { sumGroundedY += cachedHMid; cnt++; }
            if (cnt == 0 && bellyContact) { sumGroundedY += cachedHBelly; cnt++; }

            double baseGroundY = (cnt > 0) ? (sumGroundedY / cnt) : pos.y;

            // When an axle or side wheels hang over an edge into air, drop that contact toward true ground or cliff tilt
            double maxCliffDrop = Math.max(1.5, this.wheelbase * 1.25);
            double airFrontH = baseGroundY - maxCliffDrop;
            double airRearH = baseGroundY - maxCliffDrop;

            double effHFL = (flContact ? cachedHFL : (probeStepFL ? hProbeFL : (probeStepMid ? hProbeMid : (cachedHFL > -900.0 ? Math.max(cachedHFL, airFrontH) : airFrontH))));
            double effHFR = (frContact ? cachedHFR : (probeStepFR ? hProbeFR : (probeStepMid ? hProbeMid : (cachedHFR > -900.0 ? Math.max(cachedHFR, airFrontH) : airFrontH))));
            if (probeStepFL && hProbeFL > effHFL) effHFL = hProbeFL;
            if (probeStepFR && hProbeFR > effHFR) effHFR = hProbeFR;
            if (probeStepMid && hProbeMid > effHFL && hProbeMid > effHFR) {
                effHFL = Math.max(effHFL, hProbeMid);
                effHFR = Math.max(effHFR, hProbeMid);
            }
            double effHRL = (rlContact ? cachedHRL : (cachedHRL > -900.0 ? Math.max(cachedHRL, airRearH) : airRearH));
            double effHRR = (rrContact ? cachedHRR : (cachedHRR > -900.0 ? Math.max(cachedHRR, airRearH) : airRearH));

            double flatDrop = 0.14;
            if (isWheelInstalled(0) && isTireFlat(0)) effHFL -= flatDrop;
            if (isWheelInstalled(1) && isTireFlat(1)) effHFR -= flatDrop;
            if (isWheelInstalled(2) && isTireFlat(2)) effHRL -= flatDrop;
            if (isWheelInstalled(3) && isTireFlat(3)) effHRR -= flatDrop;

            double hubDrop = Math.max(0.24, this.wheelRadius);
            if (!isWheelInstalled(0)) effHFL -= hubDrop;
            if (!isWheelInstalled(1)) effHFR -= hubDrop;
            if (!isWheelInstalled(2)) effHRL -= hubDrop;
            if (!isWheelInstalled(3)) effHRR -= hubDrop;

            double frontAxleH = (effHFL + effHFR) * 0.5;
            double rearAxleH  = (effHRL + effHRR) * 0.5;
            double leftSideH  = (effHFL + effHRL) * 0.5;
            double rightSideH = (effHFR + effHRR) * 0.5;

            double weightFront = Mth.clamp(this.weightDistributionFront, 0.35, 0.65);
            this.targetSurfaceY = frontAxleH * (1.0 - weightFront) + rearAxleH * weightFront;

            if (cachedHBelly > -900.0 && cachedHBelly > this.targetSurfaceY) {
                this.targetSurfaceY = cachedHBelly;
            }
            if (cachedHMid > -900.0 && cachedHMid > this.targetSurfaceY) {
                this.targetSurfaceY = Math.max(this.targetSurfaceY, cachedHMid);
            }

            double tFrontH = ((flContact ? cachedHFL : baseGroundY) + (frContact ? cachedHFR : baseGroundY)) * 0.5;
            double tRearH  = ((rlContact ? cachedHRL : baseGroundY) + (rrContact ? cachedHRR : baseGroundY)) * 0.5;
            double terrainHeightDelta = tFrontH - tRearH;
            double targetTerrainPitch = Math.atan2(terrainHeightDelta, Math.max(1.2, this.wheelbase));
            targetTerrainPitch = Mth.clamp(targetTerrainPitch, -1.45, 1.45);
            this.terrainPitch += (targetTerrainPitch - this.terrainPitch) * Math.min(1.0, 10.0 * dt);
            if (Double.isNaN(this.terrainPitch) || Double.isInfinite(this.terrainPitch)) this.terrainPitch = 0.0;

            double heightDelta = frontAxleH - rearAxleH;
            double targetPitchAngle = Math.atan2(heightDelta, Math.max(1.2, this.wheelbase));
            targetPitchAngle = Mth.clamp(targetPitchAngle, -1.45, 1.45);

            double rollHeightDelta = leftSideH - rightSideH;
            double targetRollAngle = Math.atan2(rollHeightDelta, Math.max(1.0, this.trackWidth));
            targetRollAngle = Mth.clamp(targetRollAngle, -1.45, 1.45);

            groundPitch += (targetPitchAngle - groundPitch) * Math.min(1.0, 10.0 * dt);
            groundRoll  += (targetRollAngle - groundRoll) * Math.min(1.0, 6.0 * dt);
            if (Double.isNaN(groundPitch) || Double.isInfinite(groundPitch)) groundPitch = 0.0;
            if (Double.isNaN(groundRoll) || Double.isInfinite(groundRoll)) groundRoll = 0.0;

            double halfWVal = Math.max(0.5, this.trackWidth * 0.5);
            for (int w = 0; w < 4; w++) {
                double gH = (w == 0 ? effHFL : w == 1 ? effHFR : w == 2 ? effHRL : effHRR);
                double fOff = (w < 2 ? lFront : -lRear);
                double sOff = (w % 2 == 0 ? halfWVal : -halfWVal);
                double restWorldY = baseGroundY + fOff * Math.sin(groundPitch) - sOff * Math.sin(groundRoll);
                double groundDelta = (gH > -900.0) ? (gH - restWorldY) : 0.0;

                double chassisMountDelta = this.chassisDipY + fOff * Math.sin(this.chassisDipPitch) - sOff * Math.sin(this.chassisDipRoll);
                double clearance = chassisMountDelta - groundDelta;

                double maxSuspDroop = 0.14;
                double targetComp;
                if (clearance > maxSuspDroop) {
                    double liftHeight = clearance - maxSuspDroop;
                    targetComp = groundDelta + liftHeight;
                    this.wheelLifted[w] = true;
                } else {
                    targetComp = groundDelta;
                    this.wheelLifted[w] = (this.wheelNormalForces[w] <= 0.0 && clearance > 0.07);
                }

                double compRate = (targetComp > this.suspensionCompressions[w]) ? 24.0 : 14.0;
                this.suspensionCompressions[w] += (Mth.clamp(targetComp, -0.16, 0.28) - this.suspensionCompressions[w]) * Math.min(1.0, compRate * dt);
            }

            if (airborneTime > 0.12 || !wasGrounded) {
                if (lastFallBlocks >= 1.6 || verticalSpeed <= -9.0) {
                    this.justLandedHeavy = true;
                    this.landingImpactForce = Math.min(1.4, Math.max(0.6, lastFallBlocks * 0.35));
                    this.speed *= Math.max(0.45, 1.0 - (this.landingImpactForce * 0.30));
                } else if (lastFallBlocks >= 0.45 || verticalSpeed <= -3.5) {
                    this.justLandedLight = true;
                    this.speed *= 0.92;
                }
            }

            double dHeightDelta = heightDelta - prevHeightDelta;
            double dRollDelta = rollHeightDelta - prevRollHeightDelta;
            prevHeightDelta = heightDelta;
            prevRollHeightDelta = rollHeightDelta;

            double maxCompDelta = 0.0;
            for (int w = 0; w < 4; w++) {
                double dComp = Math.abs(this.suspensionCompressions[w] - this.prevSuspensionCompressions[w]);
                if (dComp > maxCompDelta) {
                    maxCompDelta = dComp;
                }
                this.prevSuspensionCompressions[w] = this.suspensionCompressions[w];
            }

            if (this.suspensionCreakCooldown > 0.0) {
                this.suspensionCreakCooldown -= dt;
            } else if (Math.abs(speed) > 0.6 && (Math.abs(dHeightDelta) > 0.055 || Math.abs(dRollDelta) > 0.055 || maxCompDelta > 0.030)) {
                this.justSuspensionCreak = true;
                this.suspensionCreakCooldown = 0.45;
            }

            airborneTime = 0.0;
            verticalSpeed = 0.0;
            fallStartHeight = pos.y;
        } else {
            this.targetSurfaceY = -999.0;
            if (wasGrounded) fallStartHeight = pos.y;
            airborneTime += dt;
            lastFallBlocks = Math.max(0.0, fallStartHeight - pos.y);
            verticalSpeed -= 14.0 * dt;
            verticalSpeed = Math.max(verticalSpeed, -24.0);

            double targetAirPitch = Math.atan2(verticalSpeed, Math.max(5.0, Math.abs(speed)));
            targetAirPitch = Mth.clamp(targetAirPitch, -1.25, 0.65);
            groundPitch += (targetAirPitch - groundPitch) * Math.min(1.0, 2.0 * dt);
            groundRoll  += (0.0 - groundRoll) * Math.min(1.0, 2.0 * dt);
            if (Double.isNaN(groundPitch) || Double.isInfinite(groundPitch)) groundPitch = 0.0;
            if (Double.isNaN(groundRoll) || Double.isInfinite(groundRoll)) groundRoll = 0.0;
        }

        if (inWater) {
            verticalSpeed -= 3.2 * dt;
            verticalSpeed = Math.max(verticalSpeed, -1.5);
            speed *= Math.exp(-2.4 * dt);
            lateralSpeed *= Math.exp(-4.5 * dt);
            airborneTime = 0.0;
        }

        wasGrounded = isTrulyOnGround;
        isGrounded = isTrulyOnGround;

        // Direct Responsive Steering Rack (Classical Steering Kinematics)
        double maxSteerRad = Math.toRadians(Math.max(30.0, Math.toDegrees(this.maxSteerAngle)));
        double targetSteerAngle = Mth.clamp(steer * maxSteerRad, -maxSteerRad, maxSteerRad);

        if (Math.abs(steer) > 0.05) {
            double rackRate = 9.5 * Mth.clamp(this.steeringResponse, 0.5, 2.5);
            steerAngle += (targetSteerAngle - steerAngle) * (1.0 - Math.exp(-rackRate * dt));
        } else {
            double returnRate = 10.5 * Mth.clamp(this.steeringResponse, 0.5, 2.5);
            steerAngle += (0.0 - steerAngle) * (1.0 - Math.exp(-returnRate * dt));
            if (Math.abs(steerAngle) < 0.001) {
                steerAngle = 0.0;
            }
        }
        steerAngle = Mth.clamp(steerAngle, -maxSteerRad, maxSteerRad);
        if (Double.isNaN(steerAngle) || Double.isInfinite(steerAngle)) {
            steerAngle = 0.0;
        }

        if (this.shiftHoldTimer > 0.0) {
            this.shiftHoldTimer -= dt;
        }

        // =========================================================================
        // 4. TRANSMISSION & SHIFT SCHEDULE
        // =========================================================================
        this.justShifted = false;
        this.shiftPopTriggered = false;

        double roadSpeed = Math.abs(speed);
        boolean needsStallDownshift = (currentGear > 1 && getKinematicRpm(roadSpeed, currentGear) < this.idleRpm * 1.08);
        if (gearShiftTimer <= 0.0 && (shiftHoldTimer <= 0.0 || needsStallDownshift || currentGear == -1)) {
            double signedSpeed = (speed < -0.05 || (Math.abs(speed) <= 0.05 && throttle < -0.05)) ? -roadSpeed : roadSpeed;
            int tGear = selectGear(signedSpeed, currentGear, throttle, this.engineRPM);
            if (tGear != currentGear) {
                this.pendingUpshiftGear = 0;
                this.upshiftDwellTimer = 0.0;
                this.prevGear = currentGear;
                this.currentGear = tGear;
                boolean isUpshift = (tGear > currentGear && currentGear > 0);
                boolean isLaunchFromStop = (roadSpeed < 0.35 && (tGear == 1 || tGear == -1));
                if (isLaunchFromStop) {
                    this.totalShiftTime = 0.0;
                    this.gearShiftTimer = 0.0;
                    this.shiftHoldTimer = 0.25;
                } else {
                    this.totalShiftTime = isUpshift ? 0.20 : 0.16;
                    this.gearShiftTimer = this.totalShiftTime;
                    this.shiftHoldTimer = isUpshift ? 1.40 : 1.10;
                }
                this.justShifted = true;

                if (isUpshift && (throttle > 0.65 || this.currentBoostBar > 0.35) && this.engineRPM > 3800.0) {
                    this.shiftPopTriggered = true;
                }
            } else {
                this.pendingUpshiftGear = 0;
                this.upshiftDwellTimer = 0.0;
            }
        }

        double clutchEngagement = 1.0;
        if (roadSpeed < 0.35 && (this.currentGear == 1 || this.currentGear == -1)) {
            clutchEngagement = 1.0;
        } else if (gearShiftTimer > 0.0) {
            gearShiftTimer -= dt;
            double shiftProgress = 1.0 - (gearShiftTimer / Math.max(0.01, this.totalShiftTime));

            if (shiftProgress < 0.45) {
                clutchEngagement = 0.0;
                shiftJoltPitch = -0.055 * Math.min(1.0, Math.abs(speed) / 10.0);
            } else {
                double reengage = (shiftProgress - 0.45) / 0.55;
                clutchEngagement = reengage;
                shiftJoltPitch = 0.040 * Math.abs(throttle);
            }

            if (gearShiftTimer <= 0.0) {
                if (throttle > 0.30 && this.currentGear > this.prevGear) {
                    this.shiftSurgeTimer = 0.36;
                }
            }
        } else if (this.shiftSurgeTimer > 0.0) {
            this.shiftSurgeTimer -= dt;
            double surgeFactor = Math.max(0.0, this.shiftSurgeTimer / 0.36);
            shiftJoltPitch = 0.055 * surgeFactor * Math.abs(throttle);
        } else {
            shiftJoltPitch = 0.0;
        }

        // =========================================================================
        // 4.5 BURNOUT DYNAMICS
        // =========================================================================
        boolean isSupercharged = this.aspirationType.equalsIgnoreCase("supercharger");
        double firstGearThrust = getFirstGearDriveForce();
        double drivenGrip = getDrivenAxleMaxGripForce();
        this.hasBurnoutPower = (firstGearThrust > drivenGrip * 1.05);

        boolean isBrakeStand = (throttle > 0.55) && (throttle < -0.30);
        boolean isHandbrakeBurnout = handbrake && (throttle > 0.55) && this.drivetrain.equalsIgnoreCase("fwd");
        boolean wantsBurnout = engineOn && this.hasBurnoutPower && (Math.abs(speed) < 2.0) && (isBrakeStand || isHandbrakeBurnout);
        this.isBurnoutActive = wantsBurnout && onGround;

        if (this.isBurnoutActive) {
            speed *= Math.exp(-12.0 * dt);
            double spinDeg = (isSupercharged ? 85.0 : 65.0) * Math.min(2.0, this.horsepower / 200.0);
            if (this.drivetrain.equalsIgnoreCase("rwd")) {
                wheelRotation[2] += spinDeg;
                wheelRotation[3] += spinDeg;
            } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
                wheelRotation[0] += spinDeg;
                wheelRotation[1] += spinDeg;
            } else {
                wheelRotation[0] += spinDeg;
                wheelRotation[1] += spinDeg;
                wheelRotation[2] += spinDeg;
                wheelRotation[3] += spinDeg;
            }
            tireScreechIntensity = 1.0f;
        }

        // =========================================================================
        // 5. POWERTRAIN, INDUCTION & WHEEL FORCE
        // =========================================================================
        if (boostInput && throttle > 0.05 && onGround && !handbrake) {
            boost += (1.0 - boost) * 4.5 * dt;
        } else {
            boost += (0.0 - boost) * 3.5 * dt;
        }

        double driveForce = 0.0;
        double brakeForce = 0.0;

        if (!isFlipped && engineOn) {
            double netHp = Math.max(5.0, this.horsepower);
            double currentRpm = Math.max(this.idleRpm, this.engineRPM);
            double inductionMultiplier = 1.0;
            this.currentBoostBar = 0.0;

            if (this.aspirationType.equalsIgnoreCase("turbo") || this.aspirationType.equalsIgnoreCase("twin_turbo")) {
                if (currentRpm < this.turboSpoolRpm) {
                    this.currentBoostBar = 0.0;
                    inductionMultiplier = 0.90;
                } else if (currentRpm <= this.turboPeakRpm) {
                    double span = Math.max(300.0, this.turboPeakRpm - this.turboSpoolRpm);
                    double progress = Mth.clamp((currentRpm - this.turboSpoolRpm) / span, 0.0, 1.0);
                    double spool = Math.pow(progress, 1.6);
                    this.currentBoostBar = this.boostPressureBar * spool;
                    inductionMultiplier = 1.0 + this.currentBoostBar * 0.70;
                } else {
                    double redSpan = Math.max(400.0, this.redlineRpm - this.turboPeakRpm);
                    double progress = Mth.clamp((currentRpm - this.turboPeakRpm) / redSpan, 0.0, 1.0);
                    double taperDrop = Math.max(0.65, 1.0 - 0.35 * Math.pow(progress, 1.2));
                    this.currentBoostBar = this.boostPressureBar * taperDrop;
                    inductionMultiplier = (1.0 + this.boostPressureBar * 0.70) * taperDrop;
                }
            } else if (this.aspirationType.equalsIgnoreCase("supercharger")) {
                // Mechanical positive-displacement supercharger (Roots/Twin-Screw):
                // Instantaneous low-end boost off idle (reaches full boost by idle + 600 RPM)
                double boostFactor = (currentRpm <= this.idleRpm) ? 0.35 : Mth.clamp(0.35 + 0.65 * ((currentRpm - this.idleRpm) / 600.0), 0.35, 1.0);
                this.currentBoostBar = this.boostPressureBar * boostFactor;
                inductionMultiplier = 1.0 + this.currentBoostBar * 0.30;
            }

            if (boost > 0.05) {
                this.currentBoostBar += boost * 0.45;
                inductionMultiplier *= (1.0 + boost * 0.35);
            }
            double transmissionEfficiency = 0.86;
            double netWatts = netHp * 735.5 * transmissionEfficiency * inductionMultiplier;

            // Physical torque at current operating RPM (Leiderman curve)
            double liveTorque = getTorqueAtRpm(currentRpm) * inductionMultiplier;

            double absSpeed = Math.abs(speed);
            int curG = (this.currentGear == -1) ? -1 : Mth.clamp(this.currentGear, 1, Math.max(1, this.maxGears));
            double totalRatio = Math.abs(getTotalGearRatio(curG));
            double rWheel = Math.max(0.20, this.wheelRadius);

            // Hydrodynamic torque converter / clutch coupling:
            double kinematicRpm = getKinematicRpm(absSpeed, curG);
            double speedRatio = Mth.clamp(kinematicRpm / Math.max(1.0, currentRpm), 0.0, 1.0);
            // Authentic automotive torque converter stall torque multiplication (up to 1.85x at stall):
            double stallMultiplier = 1.0 + 0.85 * (1.0 - speedRatio) * Math.min(1.0, Math.abs(throttle));
            double clutchFactor = stallMultiplier;

            // Real wheel tractive effort: F = (T * i_total * eta * clutch) / r_wheel
            double torqueThrust = (liveTorque * totalRatio * transmissionEfficiency * clutchFactor) / rWheel;
            // Physical power envelope: F_max = Power / v
            double powerThrust = (absSpeed > 1.2) ? (netWatts / absSpeed) : torqueThrust;
            double calculatedThrust = (absSpeed > 2.5) ? Math.min(torqueThrust, powerThrust) : torqueThrust;

            // Maximum traction force of the driven axle (including dynamic weight transfer)
            double drivenAxleMaxGrip = getDrivenAxleMaxGripForce();
            calculatedThrust = Math.min(calculatedThrust, drivenAxleMaxGrip);

            boolean hasFL = isWheelInstalled(0);
            boolean hasFR = isWheelInstalled(1);
            boolean hasRL = isWheelInstalled(2);
            boolean hasRR = isWheelInstalled(3);

            double drivenWheelFactor = 0.0;
            boolean isAWD = this.drivetrain.equalsIgnoreCase("awd") || this.drivetrain.equalsIgnoreCase("4wd");
            if (isAWD) {
                int count = (hasFL ? 1 : 0) + (hasFR ? 1 : 0) + (hasRL ? 1 : 0) + (hasRR ? 1 : 0);
                drivenWheelFactor = count > 0 ? Math.max(0.40, count / 4.0) : 0.0;
            } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
                int count = (hasFL ? 1 : 0) + (hasFR ? 1 : 0);
                drivenWheelFactor = count == 2 ? 1.0 : (count == 1 ? 0.70 : 0.0);
            } else { // RWD default
                int count = (hasRL ? 1 : 0) + (hasRR ? 1 : 0);
                drivenWheelFactor = count == 2 ? 1.0 : (count == 1 ? 0.70 : 0.0);
            }

            int totalInstalled = Integer.bitCount(this.installedWheelsMask & 0xF);
            if (totalInstalled == 0 || drivenWheelFactor <= 0.001) {
                calculatedThrust = 0.0;
                if (totalInstalled == 0) {
                    speed = 0.0;
                    driveForce = 0.0;
                }
            } else {
                calculatedThrust *= (drivenWheelFactor * clutchEngagement);

                if (this.shiftSurgeTimer > 0.0 && throttle > 0.20) {
                    double surgeFactor = Math.max(0.0, this.shiftSurgeTimer / 0.36);
                    double surgeMult = 1.0 + 0.12 * surgeFactor * Math.pow(throttle, 1.15);
                    calculatedThrust *= surgeMult;
                }
            }

            double effectiveMaxBrake = maxBrakeForce * Math.max(0.20, this.brakePowerMultiplier);
            if (this.reboundTimer > 0.0) {
                driveForce = 0.0;
                brakeForce = 0.0;
            } else if (throttle > 0.05) {
                if (speed < -0.1) {
                    brakeForce = effectiveMaxBrake * Math.pow(throttle, 1.35);
                } else {
                    if (this.currentGear < 1) {
                        this.currentGear = 1;
                    }
                    driveForce = calculatedThrust * Math.pow(throttle, 1.10);
                    if (this.isDrifting && Math.abs(speed) > 1.2) {
                        double angleFactor = Math.max(0.18, Math.cos(Math.toRadians(this.driftAngleDeg)));
                        driveForce *= angleFactor;
                    }
                }
            } else if (throttle < -0.05) {
                if (speed > 0.1) {
                    brakeForce = effectiveMaxBrake * Math.pow(-throttle, 1.35);
                } else {
                    currentGear = -1;
                    driveForce = -calculatedThrust * Math.pow(-throttle, 1.10);
                }
            }

            // Anti-Stuck Belly Rocking / High-Center Assist:
            // When resting on belly or middle chassis with limited wheel contact,
            // driver sawing steering or pulsing throttle produces dynamic rocking impulse to unstick
            if ((bellyContact || midContact) && totalContacts <= 2 && Math.abs(throttle) > 0.08) {
                double rockFactor = 0.25 + 0.35 * Math.abs(steer);
                double rockingForce = this.mass * 9.81 * 0.20 * rockFactor;
                driveForce += Math.signum(throttle) * rockingForce;
            }

            if (!handbrake) {
                if (this.isBeingPushed || !this.hasDriver || Math.abs(throttle) > 0.02) {
                    // Under throttle or when pushed: zero engine braking so engine delivers instant torque
                    brakeForce = 0.0;
                } else {
                    // Overrun coasting engine braking (throttle released):
                    double engineBraking = (Math.abs(speed) > 1.0) ? (mass * 9.81 * (currentGear <= 2 ? 0.040 : 0.018)) : (mass * 0.15);
                    brakeForce = engineBraking;
                }
            }
        }

        int totalInstalledWheels = Integer.bitCount(this.installedWheelsMask & 0xF);
        if (totalInstalledWheels == 0) {
            speed = 0.0;
            lateralSlipVel = 0.0;
            driveForce = 0.0;
            brakeForce = mass * 9.81 * 10.0;
        }

        double effectiveMaxBrake = maxBrakeForce * Math.max(0.20, this.brakePowerMultiplier);
        boolean effectiveParkingBrake = this.isParkingBrake || (handbrake && Math.abs(speed) < 0.25);
        if (effectiveParkingBrake) {
            brakeForce = Math.max(brakeForce, mass * 9.81 * 8.0);
            if (Math.abs(speed) < 0.35) {
                speed = 0.0;
                lateralSlipVel = 0.0;
                liveYawRate = 0.0;
            }
        } else if (handbrake) {
            if (Math.abs(speed) > 1.2) {
                brakeForce += mass * 9.81 * 0.72 * Math.max(0.40, this.currentSurface.gripMult);
            } else {
                brakeForce += effectiveMaxBrake * 1.50;
                if (Math.abs(speed) < 0.2) lateralSlipVel = 0.0;
            }
        }

        // =========================================================================
        // 6. AERODYNAMIC RESISTANCE & DRAG
        // =========================================================================
        double airDensity = 1.225;
        double cd = this.dragCoefficient;
        double fa = this.frontalArea;
        double drag = 0.5 * airDensity * cd * fa * speed * Math.abs(speed);

        int flatTireCount = 0;
        for (int i = 0; i < 4; i++) {
            if (isWheelInstalled(i) && isTireFlat(i)) flatTireCount++;
        }
        int totalMissingWheels = 4 - Integer.bitCount(this.installedWheelsMask & 0xF);

        double avgTirePressure = (this.tirePressureFL + this.tirePressureFR + this.tirePressureRL + this.tirePressureRR) * 0.25;
        double pressureDragMult = Math.pow(2.2 / Math.max(0.4, avgTirePressure), 0.65);
        double flatTireDragMult = 1.0 + (flatTireCount * 2.2);
        double crr = 0.020 * pressureDragMult * flatTireDragMult;
        double rollingResistance = crr * mass * 9.81 * Math.tanh(speed * 3.0);
        double missingHubDrag = totalMissingWheels * (mass * 9.81 * 0.08) * Math.tanh(speed * 3.0);

        double slopeForce = 0.0;
        if (isTrulyOnGround && this.groundedWheelCount > 0 && !effectiveParkingBrake) {
            double sinSlope = Math.sin(this.terrainPitch);
            double staticRollThreshold = (flatTireCount > 0 || totalMissingWheels > 0) ? 0.08 : 0.04;
            if (Math.abs(sinSlope) > staticRollThreshold) {
                slopeForce = mass * 9.81 * sinSlope * 0.75;
            }
        } else {
            driveForce = 0.0;
            slopeForce = 0.0;
        }

        double passiveForce = drag + rollingResistance + missingHubDrag + slopeForce;
        double netDrive = driveForce - passiveForce;

        // Classical Gillespie equation: effective accelerating mass accounting for rotating inertia of wheels and powertrain
        int activeGear = (this.currentGear == -1) ? 1 : Math.max(1, this.currentGear);
        double rotatingMassFactor = getRotatingMassFactor(activeGear);
        double effectiveAcceleratingMass = mass * rotatingMassFactor;

        if (!isTrulyOnGround) {
            speed *= Math.exp(-0.25 * dt);
            this.acceleration = 0.0;
        } else {
            double prevSpeed = speed;
            double driveAcc = netDrive / effectiveAcceleratingMass;
            speed += driveAcc * dt;

            if (brakeForce > 0.0) {
                double maxBrakeDelta = (brakeForce / effectiveAcceleratingMass) * dt;
                if (Math.abs(speed) <= maxBrakeDelta && Math.abs(driveForce) < brakeForce) {
                    speed = 0.0;
                } else if (speed > 0.0) {
                    speed -= maxBrakeDelta;
                } else if (speed < 0.0) {
                    speed += maxBrakeDelta;
                }
            }

            double restHaltThreshold = (flatTireCount > 0 || totalMissingWheels > 0) ? 0.08 : 0.025;
            if (!this.isBeingPushed && Math.abs(speed) < restHaltThreshold && Math.abs(driveForce) < 50.0) {
                speed = 0.0;
            }
            this.acceleration = (speed - prevSpeed) / dt;
        }

        double baseMaxKmh = maxNormalSpeedKmh;

        double maxNormalSpeed = baseMaxKmh / 3.6;
        double baseBoostKmh = boostSpeedKmh * (baseMaxKmh / Math.max(40.0, maxNormalSpeedKmh));
        double maxBoostSpeed = baseBoostKmh / 3.6;
        double effectiveMaxSpeed = maxNormalSpeed + (maxBoostSpeed - maxNormalSpeed) * Math.min(1.0, boost + this.currentBoostBar * 0.4);

        double flatSpeedLimitMult = Math.max(0.18, 1.0 - (flatTireCount * 0.22));
        if (totalMissingWheels > 0) {
            double missingSpeedCap = Math.max(0.0, 1.0 - (totalMissingWheels * 0.32));
            flatSpeedLimitMult = Math.min(flatSpeedLimitMult, missingSpeedCap);
        }
        effectiveMaxSpeed *= flatSpeedLimitMult;

        // Project Zomboid: Speed penalty when tire condition is below 20%
        double conditionSpeedPenalty = 1.0;
        for (int i = 0; i < 4; i++) {
            if (isWheelInstalled(i) && tireCondition[i] < 20.0F) {
                double wearSeverity = (20.0F - Math.max(0.0F, tireCondition[i])) / 20.0F;
                conditionSpeedPenalty *= (1.0 - wearSeverity * 0.20);
            }
        }
        effectiveMaxSpeed *= conditionSpeedPenalty;

        if (this.autoMaxSpeed && Math.abs(speed) > effectiveMaxSpeed) {
            speed = Math.signum(speed) * effectiveMaxSpeed;
        }

        // =========================================================================
        // 7. DYNAMIC WEIGHT TRANSFER & TIRE NORMAL FORCE (Assetto Corsa Model)
        // =========================================================================
        double effWheelbase = Math.max(1.0, this.wheelbase);
        double effTrackWidth = Math.max(1.0, this.trackWidth);
        double fFront = Mth.clamp(this.weightDistributionFront, 0.35, 0.70);
        double hCg = Math.max(0.20, this.centerOfGravityHeight);

        double totalWeight = this.mass * 9.81;
        double staticFzFront = totalWeight * fFront * 0.5;
        double staticFzRear = totalWeight * (1.0 - fFront) * 0.5;

        // Dynamic longitudinal weight transfer:
        // Deceleration (acceleration < 0, like trail braking or releasing throttle)
        // transfers weight onto the front axle, directly increasing front tire normal force!
        double dFzLong = (this.mass * this.acceleration * hCg) / effWheelbase;
        dFzLong = Mth.clamp(dFzLong, -staticFzRear * 1.8, staticFzFront * 1.8);

        // Dynamic lateral weight transfer:
        double latAccel = speed * this.liveYawRate;
        double dFzLatTotal = (this.mass * latAccel * hCg) / effTrackWidth;
        double dFzLatFront = dFzLatTotal * fFront * 0.5;
        double dFzLatRear = dFzLatTotal * (1.0 - fFront) * 0.5;

        // Drivetrain reaction torque (Propshaft twist rolls live axle under heavy acceleration):
        double dFzTorque = 0.0;
        if (engineOn && Math.abs(throttle) > 0.1) {
            double effectiveDriveForce = Math.abs(this.acceleration) * this.mass;
            dFzTorque = (effectiveDriveForce * (hCg / effTrackWidth) * 0.18) * Math.signum(throttle);
        }

        double rawFz0 = staticFzFront - dFzLong * 0.5 + dFzLatFront - (this.drivetrain.equalsIgnoreCase("fwd") ? dFzTorque * 0.5 : 0.0);
        double rawFz1 = staticFzFront - dFzLong * 0.5 - dFzLatFront + (this.drivetrain.equalsIgnoreCase("fwd") ? dFzTorque * 0.5 : 0.0);
        double rawFz2 = staticFzRear + dFzLong * 0.5 + dFzLatRear - (this.drivetrain.equalsIgnoreCase("rwd") ? dFzTorque : dFzTorque * 0.5);
        double rawFz3 = staticFzRear + dFzLong * 0.5 - dFzLatRear + (this.drivetrain.equalsIgnoreCase("rwd") ? dFzTorque : dFzTorque * 0.5);

        double[] rawFz = new double[]{rawFz0, rawFz1, rawFz2, rawFz3};
        boolean[] hasGroundContact = new boolean[]{flContact, frContact, rlContact, rrContact};

        for (int w = 0; w < 4; w++) {
            boolean installed = isWheelInstalled(w);
            boolean flat = isTireFlat(w);
            boolean grounded = isTrulyOnGround && hasGroundContact[w];

            if (!installed) {
                this.wheelNormalForces[w] = 0.0;
                this.wheelContactPatches[w] = 0.0;
                this.wheelLifted[w] = true;
            } else if (!grounded || rawFz[w] <= 0.0) {
                this.wheelNormalForces[w] = 0.0;
                this.wheelContactPatches[w] = 0.0;
                this.wheelLifted[w] = true;
                double staticBase = (w < 2) ? staticFzFront : staticFzRear;
                double unweightRatio = Math.min(1.0, Math.max(0.0, -rawFz[w] / Math.max(100.0, staticBase)));
                double liftAir = 0.06 + unweightRatio * 0.10;
                this.suspensionCompressions[w] = Math.max(this.suspensionCompressions[w], liftAir);
            } else {
                double fz = rawFz[w];
                if (flat) fz *= 0.40;
                this.wheelNormalForces[w] = fz;
                double staticBase = (w < 2) ? staticFzFront : staticFzRear;
                this.wheelContactPatches[w] = Mth.clamp(fz / Math.max(100.0, staticBase), 0.05, 2.0);
                this.wheelLifted[w] = false;
            }
        }

        double tireSurfaceMult = switch (this.currentSurface) {
            case ASPHALT -> this.tireGripAsphalt;
            case DIRT, GRAVEL, SAND, MUD, GRASS -> this.tireGripDirt;
            case ICE, SNOW -> this.tireGripIce;
        };
        double surfaceMu = this.currentSurface.gripMult * tireSurfaceMult;
        double totalFzFront = this.wheelNormalForces[0] + this.wheelNormalForces[1];
        double totalFzRear = this.wheelNormalForces[2] + this.wheelNormalForces[3];

        double maxFrictionRear = surfaceMu * totalFzRear;
        boolean handbrakeLocked = handbrake && isTrulyOnGround;

        // =========================================================================
        // 7. CLEAN CONTINUOUS ACKERMANN CORNERING WITH DYNAMIC WEIGHT TRANSFER
        // =========================================================================
        effWheelbase = Math.max(1.0, this.wheelbase);
        double curvature = Math.tan(Mth.clamp(steerAngle, -1.05, 1.05)) / effWheelbase;

        // Dynamic weight transfer: throttle lift or braking weights the front axle for a crisp, alive turn-in bite!
        double weightTransferBite = 1.0 + (Math.abs(throttle) < 0.15 ? 0.12 : 0.0) + (brakeForce > 200.0 ? 0.16 : 0.0);
        // Suspension & ARB influence on steering responsiveness, sharpness, and lateral stability
        double suspensionSteerAgility = (0.75 + 0.25 * this.suspensionStiffness) * (0.80 + 0.20 * (this.antiRollBarStiffness / 1.45));
        double rollLag = 1.0 - Math.min(0.25, Math.abs(this.chassisDipRoll) * 1.2 * (this.bodyRollFactor / 1.0));

        double absSpeed = Math.abs(speed);
        double baseSteerYaw = 0.0;
        if (absSpeed > 0.05) {
            // Speed-proportional Ackermann yaw rate: omega = (v / L) * tan(delta)
            // Blends in swiftly above 0.05 m/s to prevent zero-speed snaps while giving immediate response
            double speedRamp = Math.min(1.0, (absSpeed - 0.05) / 0.20);
            // Gentle low-speed maneuver agility gives natural turning on Minecraft block roads without oversteer
            double lowSpeedAgility = 1.0 + 0.30 * Math.max(0.0, 1.0 - absSpeed / 5.0);
            // Speed-sensitive steering stabilization: smooth high-speed stability prevents twitchy oversteering
            double speedSteerStability = 1.0 / (1.0 + Math.max(0.0, absSpeed - 5.5) * 0.05);
            baseSteerYaw = speed * curvature * steerGlobal * Math.min(1.05, Math.max(0.75, this.turnInBite)) * weightTransferBite * suspensionSteerAgility * rollLag * speedRamp * lowSpeedAgility * speedSteerStability;
        }

        double targetYawRate = baseSteerYaw;

        // Realistic Asymmetrical Pulling Torque (Увод в сторону пробитого / отсутствующего колеса)
        double steerPull = 0.0;
        if (!isWheelInstalled(0)) steerPull -= 0.65;
        else if (isTireFlat(0)) steerPull -= 0.38;

        if (!isWheelInstalled(1)) steerPull += 0.65;
        else if (isTireFlat(1)) steerPull += 0.38;

        if (!isWheelInstalled(2)) steerPull -= 0.28;
        else if (isTireFlat(2)) steerPull -= 0.16;

        if (!isWheelInstalled(3)) steerPull += 0.28;
        else if (isTireFlat(3)) steerPull += 0.16;

        targetYawRate += (steerPull * Math.min(2.2, absSpeed * 0.18));


        // Front Grip: Missing or flat front tires cause massive UNDERSTEER
        int frontWheelsInstalled = (isWheelInstalled(0) ? 1 : 0) + (isWheelInstalled(1) ? 1 : 0);
        int frontFlats = (isWheelInstalled(0) && isTireFlat(0) ? 1 : 0) + (isWheelInstalled(1) && isTireFlat(1) ? 1 : 0);
        int frontWornCount = (isWheelInstalled(0) && tireCondition[0] < 20.0F ? 1 : 0) + (isWheelInstalled(1) && tireCondition[1] < 20.0F ? 1 : 0);
        if (frontWheelsInstalled == 0) {
            targetYawRate = 0.0; // Zero front grip = cannot steer
        } else if (frontWheelsInstalled == 1) {
            targetYawRate *= 0.40;
        } else if (frontFlats == 2) {
            targetYawRate *= 0.48; // Both front flat = severe understeer
        } else if (frontFlats == 1) {
            targetYawRate *= 0.72;
        } else if (frontWornCount > 0) {
            targetYawRate *= Math.max(0.35, 1.0 - frontWornCount * 0.30); // Project Zomboid: front tire wear causes understeer push
        }

        // Rear Grip: Missing or flat rear tires cause OVERSTEER (loose rear end)
        int rearFlats = (isWheelInstalled(2) && isTireFlat(2) ? 1 : 0) + (isWheelInstalled(3) && isTireFlat(3) ? 1 : 0);
        int rearMissing = (isWheelInstalled(2) ? 0 : 1) + (isWheelInstalled(3) ? 0 : 1);
        int rearWornCount = (isWheelInstalled(2) && tireCondition[2] < 20.0F ? 1 : 0) + (isWheelInstalled(3) && tireCondition[3] < 20.0F ? 1 : 0);
        if (rearMissing > 0 || rearFlats > 0 || rearWornCount > 0) {
            if (absSpeed > 1.2 && Math.abs(steer) > 0.05) {
                double looseRearMult = 1.0 + (rearFlats * 0.40) + (rearMissing * 0.75) + (rearWornCount * 0.45);
                targetYawRate *= looseRearMult;
            }
        }

        // Authentic Continuous Drift with Mass Inertia, Drivetrain Dynamics & Realistic Handling:
        double absSlip = Math.abs(this.lateralSlipVel);
        this.driftAngleDeg = Math.toDegrees(Math.atan2(absSlip, Math.max(0.40, absSpeed)));
        double currentSlipAngleDeg = this.driftAngleDeg;

        // 1. SCANDINAVIAN FLICK (Weight Transfer Inertia Drift Entry)
        double steerRate = (steer - this.prevSteerInput) / dt;
        if (Math.abs(steer) > 0.35 && absSpeed > 5.0) {
            this.flickSteerHold = steer;
            this.flickTimer = 0.40; // 400ms window
        } else if (this.flickTimer > 0.0) {
            this.flickTimer -= dt;
            if (this.flickTimer <= 0.0) {
                this.flickSteerHold = 0.0;
            }
        }

        boolean flickTriggered = false;
        if (this.flickTimer > 0.0 && (steer * this.flickSteerHold < -0.18) && Math.abs(steerRate) > 2.2 && absSpeed > 5.0) {
            flickTriggered = true;
            this.flickTimer = 0.0;
            this.flickSteerHold = 0.0;
            this.isFlickActive = true;
            double flickDir = Math.signum(steer);
            this.lateralSlipVel += flickDir * 2.8 * dt;
        }

        if (this.isFlickActive) {
            if (currentSlipAngleDeg < 6.0 || Math.abs(steer) < 0.08 || absSpeed < 3.5) {
                this.isFlickActive = false;
            }
        }
        this.prevSteerInput = steer;

        // 2. DRIFT INITIATION & HANDBRAKE TRACTION LOSS
        double minDriftSpeed = Math.max(1.0, 1.8 * this.currentSurface.lateralGrip);
        if (handbrake && absSpeed > minDriftSpeed) {
            double steerDir = Math.abs(steer) > 0.03 ? Math.signum(steer) : (Math.abs(this.liveYawRate) > 0.02 ? Math.signum(this.liveYawRate) : 0.0);
            if (steerDir != 0.0) {
                double angleLimitFactor = Math.max(0.0, 1.0 - (currentSlipAngleDeg / 75.0));
                double surfaceYawBonus = 1.0 + (1.0 - this.currentSurface.lateralGrip) * 0.45;
                double handbrakeTurnRate = steerDir * (0.85 + Math.min(0.35, absSpeed * 0.03)) * surfaceYawBonus * angleLimitFactor;
                this.liveYawRate += (handbrakeTurnRate - this.liveYawRate) * (1.0 - Math.exp(-6.5 * dt));
                this.lateralSlipVel += steerDir * 2.6 * surfaceYawBonus * angleLimitFactor * dt;
            }
        }

        // 3. CONTINUOUS DRIFT STATE TRACKING (Independent of holding handbrake!)
        boolean isHandbrakeSlide = handbrake && absSpeed > minDriftSpeed && currentSlipAngleDeg < 78.0;
        double slipAngleThreshold = Math.max(2.5, 7.5 * this.currentSurface.lateralGrip);
        double slipVelThreshold = Math.max(0.25, 0.65 * this.currentSurface.lateralGrip);

        // Power Oversteer: Wheel drive force exceeds available tire traction!
        double latG = Math.abs(speed * this.liveYawRate) / 9.81;
        double latGripFraction = Mth.clamp(latG / Math.max(0.2, this.currentSurface.lateralGrip), 0.0, 0.88);
        double availableLongitudinalGrip = getDrivenAxleMaxGripForce() * Math.sqrt(Math.max(0.05, 1.0 - latGripFraction * latGripFraction));
        firstGearThrust = getFirstGearDriveForce();
        this.hasPowerToBreakTraction = (firstGearThrust * Math.abs(throttle) > availableLongitudinalGrip);

        boolean isPowerOversteer = false;
        if (this.drivetrain.equalsIgnoreCase("rwd") && engineOn && this.hasPowerToBreakTraction && throttle > 0.80) {
            // Power oversteer requires real tire lateral grip saturation (latG > 0.70 mu) or slippery terrain
            if (this.currentSurface.lateralGrip < 0.55 && Math.abs(steer) > 0.15 && absSpeed > 3.0) {
                isPowerOversteer = true;
            } else if (latG > this.currentSurface.lateralGrip * 0.70 && Math.abs(steer) > 0.20 && absSpeed > 6.0) {
                isPowerOversteer = true;
            }
        }
        if (!this.drivetrain.equalsIgnoreCase("rwd") && engineOn && throttle > 0.90 && Math.abs(steer) > 0.30 && absSpeed > 8.0) {
            if (this.currentSurface.lateralGrip < 0.45 && this.hasPowerToBreakTraction) {
                isPowerOversteer = true;
            }
        }

        // 4. Kinetic Slide / Cornering Limits:
        double corneringGripLimit = this.currentSurface.lateralGrip * (this.drivetrain.equalsIgnoreCase("awd") ? 1.05 : 0.88);
        boolean isSlickSurfaceSlide = (this.currentSurface.lateralGrip < 0.68) && (latG > corneringGripLimit * 0.82) && (absSpeed > 4.0);
        boolean isExtremeSpeedTireScrub = (latG > corneringGripLimit * 1.15) && (absSpeed > 9.0);
        boolean isWornTireSlide = (rearWornCount > 0 || frontWornCount > 0) && (latG > corneringGripLimit * 0.45) && (absSpeed > 2.0);

        boolean isDriftInitiated = isHandbrakeSlide || flickTriggered || this.isFlickActive || isPowerOversteer || isSlickSurfaceSlide || isExtremeSpeedTireScrub || isWornTireSlide;
        boolean canSustainDrift = this.isDrifting && (currentSlipAngleDeg > slipAngleThreshold) && (absSlip > slipVelThreshold) && (absSpeed > minDriftSpeed * 0.75);

        this.isDrifting = isDriftInitiated || canSustainDrift;
        if (this.isDrifting) {
            this.continuousDriftTime += dt;
        } else {
            this.continuousDriftTime = Math.max(0.0, this.continuousDriftTime - dt * 2.0);
        }

        // 3. TARGET YAW RATE (Ackermann base + Oversteer / Counter-Steer / Drivetrain dynamics)
        if (this.isDrifting) {
            double speedFactor = Mth.clamp((absSpeed - 1.5) / 6.0, 0.0, 1.0);

            // Counter-steering: driver steers OPPOSITE to current yaw rotation (opposite lock)
            boolean isCounterSteering = (steer * this.liveYawRate < -0.02);
            // Over-steering: driver keeps steering DEEP into the turn while already sliding
            boolean isOverSteering = (steer * this.liveYawRate > 0.04);

            double maxCounterSteerLock = (this.currentSurface.lateralGrip < 0.70) ? 45.0 : 38.0;

            if (isCounterSteering) {
                if (currentSlipAngleDeg > maxCounterSteerLock && throttle > 0.85) {
                    targetYawRate = Math.signum(this.liveYawRate) * (0.85 + speedFactor * 0.35);
                    this.isSpinningOut = true;
                } else {
                    double counterAuthority = 1.30 * (0.8 + 0.2 * this.turnInBite);
                    targetYawRate = (this.liveYawRate * 0.40) + (steer * counterAuthority * (0.45 + 0.55 * speedFactor));
                    this.isSpinningOut = false;
                }
            } else if (isOverSteering && currentSlipAngleDeg > 35.0) {
                targetYawRate = Math.signum(this.liveYawRate) * (0.80 + speedFactor * 0.40);
                this.isSpinningOut = true;
            } else if (!isCounterSteering && currentSlipAngleDeg > 42.0) {
                targetYawRate = Math.signum(this.liveYawRate) * (0.75 + speedFactor * 0.35);
                this.isSpinningOut = true;
            } else {
                targetYawRate = baseSteerYaw + Math.signum(this.liveYawRate) * 0.12 * speedFactor;
            }

            // Drivetrain dynamics on yaw
            if (engineOn && throttle > 0.05) {
                if (this.drivetrain.equalsIgnoreCase("rwd")) {
                    double pwrRatio = availableLongitudinalGrip > 0.0 ? (firstGearThrust * throttle / availableLongitudinalGrip) : 1.0;
                    double pwrFactor = this.hasPowerToBreakTraction ? Mth.clamp(pwrRatio - 1.0, 0.2, 2.2) : 0.0;
                    double powerOversteerTorque = throttle * 0.35 * pwrFactor;
                    if (isCounterSteering) {
                        targetYawRate += Math.signum(this.liveYawRate) * powerOversteerTorque * 0.50;
                    } else {
                        targetYawRate += Math.signum(this.liveYawRate) * powerOversteerTorque * 0.90;
                        if (this.hasPowerToBreakTraction && throttle > 0.75 && currentSlipAngleDeg > 36.0 && pwrRatio > 1.45) {
                            this.isSpinningOut = true;
                        }
                    }
                } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
                    double frontPull = throttle * 0.75;
                    targetYawRate = targetYawRate * (1.0 - frontPull * 0.60) + baseSteerYaw * frontPull;
                    this.isSpinningOut = false;
                } else if (this.drivetrain.equalsIgnoreCase("awd")) {
                    targetYawRate = targetYawRate * 0.75 + baseSteerYaw * 0.25;
                }
            }

            if (handbrake) {
                this.isSpinningOut = false;
                if (currentSlipAngleDeg > 110.0 && Math.abs(steer) < 0.1) {
                    double decay = Math.max(0.0, 1.0 - (currentSlipAngleDeg - 110.0) / 30.0);
                    targetYawRate *= decay;
                    this.liveYawRate *= Math.exp(-4.0 * dt);
                }
            }
        } else {
            targetYawRate = baseSteerYaw;
            this.isSpinningOut = false;
        }

        if (this.isSpinningOut && absSpeed < 1.6) {
            this.isSpinningOut = false;
        }

        // Tire physical lateral grip ceiling: omega_max = (mu * g) / |v|
        if (absSpeed > 0.1) {
            if (handbrake || this.isDrifting) {
                targetYawRate = Mth.clamp(targetYawRate, -1.6, 1.6);
            } else {
                double maxPhysicalYawRate = (surfaceMu * 9.81) / Math.max(2.2, absSpeed);
                double yawCeiling = Math.max(1.25, maxPhysicalYawRate * 1.55);
                targetYawRate = Mth.clamp(targetYawRate, -yawCeiling, yawCeiling);
            }
        } else {
            targetYawRate = 0.0;
        }

        // Natural chassis inertia & steering compliance
        double steerResponsiveness = (Math.abs(steer) > 0.05) ? 7.2 : 6.0;
        double baseTransition = steerResponsiveness * (0.70 + 0.30 * this.suspensionStiffness);
        if (handbrake) {
            if (absSpeed < 0.2) {
                targetYawRate = 0.0;
                this.liveYawRate = 0.0;
            } else {
                baseTransition = 8.5;
                this.liveYawRate = Mth.clamp(this.liveYawRate, -1.6, 1.6);
            }
        }

        if (!isTrulyOnGround) {
            targetYawRate = 0.0;
            this.liveYawRate *= Math.exp(-1.5 * dt);
            this.lateralSlipVel *= Math.exp(-2.5 * dt);
            this.isDrifting = false;
            this.isSpinningOut = false;
        } else if (absSpeed < 0.05) {
            targetYawRate = 0.0;
            this.liveYawRate = 0.0;
        } else {
            this.liveYawRate += (targetYawRate - this.liveYawRate) * (1.0 - Math.exp(-baseTransition * dt));
        }

        if (absSpeed < 0.05) {
            this.liveYawRate = 0.0;
        }
        this.liveYawRate = Mth.clamp(this.liveYawRate, -1.6, 1.6);
        if (Double.isNaN(this.liveYawRate) || Double.isInfinite(this.liveYawRate)) {
            this.liveYawRate = 0.0;
        }
        double deltaYawRad = this.liveYawRate * dt;
        this.deltaYaw = Math.toDegrees(deltaYawRad);
        if (Double.isNaN(this.deltaYaw) || Double.isInfinite(this.deltaYaw)) {
            this.deltaYaw = 0.0;
        }

        // 5. LATERAL SLIP VELOCITY (Smooth kinetic sliding without abrupt braking)
        double driftGrip;
        double targetSlip = 0.0;

        if (this.isDrifting) {
            double speedDecay = Mth.clamp((absSpeed - 1.5) / 4.0, 0.0, 1.0);
            driftGrip = (2.0 + (1.0 - speedDecay) * 4.8) * this.currentSurface.driftFriction;
            double surfaceSlipMultiplier = (1.35 - 0.25 * this.currentSurface.lateralGrip);
            targetSlip = this.liveYawRate * Math.min(3.8, absSpeed * 0.36) * speedDecay * surfaceSlipMultiplier;

            if (engineOn && throttle > 0.05) {
                if (this.drivetrain.equalsIgnoreCase("rwd")) {
                    driftGrip = Math.max(0.65, driftGrip - throttle * 1.2 * this.currentSurface.driftFriction);
                    targetSlip += Math.signum(this.liveYawRate) * throttle * 0.85 * surfaceSlipMultiplier;
                } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
                    driftGrip += throttle * 6.5 * this.currentSurface.gripMult;
                    targetSlip *= (1.0 - throttle * 0.60);
                } else if (this.drivetrain.equalsIgnoreCase("awd")) {
                    targetSlip = this.liveYawRate * Math.min(3.2, absSpeed * 0.28) * speedDecay * surfaceSlipMultiplier;
                }
            }
        } else {
            driftGrip = 13.0 * this.currentSurface.lateralGrip;
            targetSlip = 0.0;
            int totalWorn = (rearWornCount + frontWornCount);
            if (totalWorn > 0 && absSpeed > 1.2 && Math.abs(this.liveYawRate) > 0.03) {
                driftGrip *= Math.max(0.35, 1.0 - totalWorn * 0.18);
                targetSlip = this.liveYawRate * Math.min(2.8, absSpeed * 0.28) * 0.65;
            }
        }

        if (isTrulyOnGround) {
            this.lateralSlipVel += (targetSlip - this.lateralSlipVel) * (1.0 - Math.exp(-driftGrip * dt));
        }
        this.lateralSlipVel = Mth.clamp(this.lateralSlipVel, -15.0, 15.0);
        if (Double.isNaN(this.lateralSlipVel) || Double.isInfinite(this.lateralSlipVel)) {
            this.lateralSlipVel = 0.0;
        }
        if (!this.isDrifting || absSpeed < 2.0) {
            this.lateralSlipVel = 0.0;
        }

        // Lateral tire scrub (realistic kinetic energy scrub when sliding sideways)
        if (this.isDrifting && absSpeed > 1.0) {
            double angleRad = Math.toRadians(currentSlipAngleDeg);
            double baseScrub = (Math.sin(angleRad) * 2.8 + Math.pow(currentSlipAngleDeg / 60.0, 2.0) * 2.6);
            if (this.continuousDriftTime > 2.5) {
                baseScrub *= (1.0 + Math.min(1.2, (this.continuousDriftTime - 2.5) * 0.35));
            }
            double scrubDecel = baseScrub * this.currentSurface.driftFriction * dt;
            if (speed > 0.1) speed = Math.max(0.0, speed - scrubDecel);
            else if (speed < -0.1) speed = Math.min(0.0, speed + scrubDecel);
        }

        // Surface rolling drag (Sand, Mud bog down rolling speed when not accelerating)
        if (this.currentSurface.rollingDrag > 1.0 && Math.abs(throttle) < 0.15 && absSpeed > 0.3) {
            double extraDrag = (this.currentSurface.rollingDrag - 1.0) * 0.40 * dt;
            if (speed > 0.1) speed = Math.max(0.0, speed - extraDrag);
            else if (speed < -0.1) speed = Math.min(0.0, speed + extraDrag);
        }

        // Screech Audio Intensity scaled by surface screech factor (Only on hard slides or drift!)
        latG = Math.abs(speed * this.liveYawRate) / 9.81;
        float baseScreech = (float) Mth.clamp((latG - 0.65) / 0.35, 0.0, 1.0);
        if (this.isDrifting && (Math.abs(steer) > 0.04 || currentSlipAngleDeg > 12.0)) {
            baseScreech = Math.max(baseScreech, (float) Math.min(1.0, 0.55 + currentSlipAngleDeg * 0.008));
        }
        tireScreechIntensity = (this.isBurnoutActive ? 1.0f : (this.isDrifting ? baseScreech : 0.0f)) * (float) this.currentSurface.screechFactor;

        // =========================================================================
        // 10. CHASSIS ROLL & PITCH TRACKING TERRAIN
        // =========================================================================
        roll += (groundRoll - roll) * 10.0 * dt;
        pitch += (groundPitch - pitch) * 10.0 * dt;
        roll = Mth.clamp(roll, -1.45, 1.45);
        pitch = Mth.clamp(pitch, -1.45, 1.45);
        if (Double.isNaN(roll) || Double.isInfinite(roll)) roll = 0.0;
        if (Double.isNaN(pitch) || Double.isInfinite(pitch)) pitch = 0.0;

        // 2nd-Order Sprung Mass Dynamics (Dive, Squat, Body Roll)
        double maxDip = Math.max(0.05, this.maxSuspensionTravel);

        double longAcc = this.acceleration;
        double longInertiaPitch;
        if (speed >= 0.0) {
            if (longAcc < 0.0) {
                longInertiaPitch = longAcc * 0.0105 * this.pitchDiveFactor; // negative angle = nose down on braking!
            } else {
                longInertiaPitch = longAcc * 0.0075 * this.pitchDiveFactor; // positive angle = nose up on acceleration!
            }
        } else {
            if (longAcc < 0.0) {
                longInertiaPitch = -Math.abs(longAcc) * 0.0065 * this.pitchDiveFactor;
            } else {
                longInertiaPitch = Math.abs(longAcc) * 0.0085 * this.pitchDiveFactor;
            }
        }
        double joltPitch = shiftJoltPitch * 0.85 * this.pitchDiveFactor;
        double targetPitchDip = Mth.clamp(longInertiaPitch + joltPitch + this.extraStaticPitch, -0.12, 0.12);
        double kSpringPitch = 75.0 * this.suspensionStiffness;
        double cDampPitch = 14.0 * this.suspensionDamping;
        this.chassisPitchVel = (this.chassisPitchVel + (targetPitchDip - this.chassisDipPitch) * kSpringPitch * dt) / (1.0 + cDampPitch * dt);
        this.chassisPitchVel = Mth.clamp(this.chassisPitchVel, -3.0, 3.0);
        this.chassisDipPitch = Mth.clamp(this.chassisDipPitch + this.chassisPitchVel * dt, -0.20, 0.20);
        if (Double.isNaN(this.chassisDipPitch) || Double.isInfinite(this.chassisDipPitch)) {
            this.chassisDipPitch = 0.0;
            this.chassisPitchVel = 0.0;
        }

        double lateralG = Mth.clamp((speed * this.liveYawRate) / 9.81, -1.2, 1.2);
        double targetRollSway = Mth.clamp((lateralG * (this.centerOfGravityHeight / 0.55) * 0.045 * this.bodyRollFactor) + this.extraStaticRoll, -0.085, 0.085);
        double kSpringRoll = 220.0 * this.suspensionStiffness + 160.0 * (this.antiRollBarStiffness / 1.45);
        double cDampRoll = 2.0 * Math.sqrt(kSpringRoll) * 1.35 * this.suspensionDamping;
        this.chassisRollVel = (this.chassisRollVel + (targetRollSway - this.chassisDipRoll) * kSpringRoll * dt) / (1.0 + cDampRoll * dt);
        this.chassisRollVel = Mth.clamp(this.chassisRollVel, -2.5, 2.5);
        this.chassisDipRoll += this.chassisRollVel * dt;
        this.chassisDipRoll = Mth.clamp(this.chassisDipRoll, -0.09, 0.09);
        if (Double.isNaN(this.chassisDipRoll) || Double.isInfinite(this.chassisDipRoll)) {
            this.chassisDipRoll = 0.0;
            this.chassisRollVel = 0.0;
        }

        double avgComp = (this.suspensionCompressions[0] + this.suspensionCompressions[1] + this.suspensionCompressions[2] + this.suspensionCompressions[3]) * 0.25;
        double targetChassisY = Mth.clamp(avgComp + this.extraStaticDipY, -maxDip, maxDip * 0.5);
        double kSpringY = 120.0 * this.suspensionStiffness;
        double cDampY = 22.0 * this.suspensionDamping;
        this.chassisYVel = (this.chassisYVel + (targetChassisY - this.chassisDipY) * kSpringY * dt) / (1.0 + cDampY * dt);
        this.chassisYVel = Mth.clamp(this.chassisYVel, -4.0, 4.0);
        this.chassisDipY = Mth.clamp(this.chassisDipY + this.chassisYVel * dt, -maxDip, maxDip * 0.5);
        if (Double.isNaN(this.chassisDipY) || Double.isInfinite(this.chassisDipY)) {
            this.chassisDipY = 0.0;
            this.chassisYVel = 0.0;
        }

        // =========================================================================
        // 11. GROUND-SYNCHRONIZED PER-WHEEL ROTATION (With Individual Slip & Handbrake)
        // =========================================================================
        double rEff = Math.max(0.20, this.wheelRadius);
        double roadDeltaDeg = effectiveParkingBrake ? 0.0 : (speed * dt / rEff) * (180.0 / Math.PI);

        // 1. Calculate per-wheel slip from engine torque vs wheel contact patch grip limit:
        double[] wheelSlipDeg = new double[]{0.0, 0.0, 0.0, 0.0};
        if (engineOn && Math.abs(throttle) > 0.05 && this.isPhysicallySupported && !effectiveParkingBrake) {
            int driveGear = (this.currentGear == -1) ? -1 : Math.max(1, this.currentGear);
            double totalGearRatio = Math.abs(getTotalGearRatio(driveGear));
            double rWheel = Math.max(0.20, this.wheelRadius);
            double liveEngineTorque = getTorqueAtRpm(this.engineRPM);
            double transEfficiency = 0.86;
            // Delivered drivetrain tractive force: F_drive = (T_engine * i_total * eta * throttle) / r_wheel
            double axleTorque = liveEngineTorque * totalGearRatio * transEfficiency * Math.abs(throttle);
            double totalDriveTractiveForce = axleTorque / rWheel;

            // Physical wheel rotational inertia: I_w = 0.5 * m_wheel * r^2 (~1.2 kg*m^2)
            double wheelInertia = Math.max(0.60, 0.5 * 25.0 * rWheel * rWheel);

            if (this.drivetrain.equalsIgnoreCase("rwd")) {
                double fDrivePerWheel = totalDriveTractiveForce * 0.5;
                double gripRL = this.wheelNormalForces[2] * surfaceMu;
                double gripRR = this.wheelNormalForces[3] * surfaceMu;

                double excessRL = Math.max(0.0, fDrivePerWheel - gripRL);
                double excessRR = Math.max(0.0, fDrivePerWheel - gripRR);

                // Euler's rotational equation: alpha = Delta_Tau / I_w
                if (excessRL > 0.0 || (this.isDrifting && Math.abs(throttle) > 0.15)) {
                    double excessTorqueRL = (excessRL > 0.0 ? excessRL : (fDrivePerWheel * 0.40)) * rWheel;
                    double angAccRL = excessTorqueRL / wheelInertia;
                    double slipOmegaRL = angAccRL * dt;
                    wheelSlipDeg[2] = slipOmegaRL * dt * (180.0 / Math.PI);
                }
                if (excessRR > 0.0 || (this.isDrifting && Math.abs(throttle) > 0.15)) {
                    double excessTorqueRR = (excessRR > 0.0 ? excessRR : (fDrivePerWheel * 0.40)) * rWheel;
                    double angAccRR = excessTorqueRR / wheelInertia;
                    double slipOmegaRR = angAccRR * dt;
                    wheelSlipDeg[3] = slipOmegaRR * dt * (180.0 / Math.PI);
                }
            } else if (this.drivetrain.equalsIgnoreCase("fwd")) {
                double fDrivePerWheel = totalDriveTractiveForce * 0.5;
                double gripFL = this.wheelNormalForces[0] * surfaceMu;
                double gripFR = this.wheelNormalForces[1] * surfaceMu;

                double excessFL = Math.max(0.0, fDrivePerWheel - gripFL);
                double excessFR = Math.max(0.0, fDrivePerWheel - gripFR);

                if (excessFL > 0.0) {
                    double excessTorqueFL = excessFL * rWheel;
                    double angAccFL = excessTorqueFL / wheelInertia;
                    wheelSlipDeg[0] = (angAccFL * dt) * dt * (180.0 / Math.PI);
                }
                if (excessFR > 0.0) {
                    double excessTorqueFR = excessFR * rWheel;
                    double angAccFR = excessTorqueFR / wheelInertia;
                    wheelSlipDeg[1] = (angAccFR * dt) * dt * (180.0 / Math.PI);
                }
            } else { // AWD / 4WD
                double fDrivePerWheel = totalDriveTractiveForce * 0.25;
                double excessFL = Math.max(0.0, fDrivePerWheel - (this.wheelNormalForces[0] * surfaceMu));
                double excessFR = Math.max(0.0, fDrivePerWheel - (this.wheelNormalForces[1] * surfaceMu));
                double excessRL = Math.max(0.0, fDrivePerWheel - (this.wheelNormalForces[2] * surfaceMu));
                double excessRR = Math.max(0.0, fDrivePerWheel - (this.wheelNormalForces[3] * surfaceMu));

                if (excessFL > 0.0) wheelSlipDeg[0] = ((excessFL * rWheel) / wheelInertia) * dt * dt * (180.0 / Math.PI);
                if (excessFR > 0.0) wheelSlipDeg[1] = ((excessFR * rWheel) / wheelInertia) * dt * dt * (180.0 / Math.PI);
                if (excessRL > 0.0) wheelSlipDeg[2] = ((excessRL * rWheel) / wheelInertia) * dt * dt * (180.0 / Math.PI);
                if (excessRR > 0.0) wheelSlipDeg[3] = ((excessRR * rWheel) / wheelInertia) * dt * dt * (180.0 / Math.PI);
            }
        }

        // Calculate physical linear drive wheel speed from wheel rotation & slip
        double avgDriveSlipDeg;
        if (this.drivetrain.equalsIgnoreCase("fwd")) {
            avgDriveSlipDeg = 0.5 * (wheelSlipDeg[0] + wheelSlipDeg[1]);
        } else if (this.drivetrain.equalsIgnoreCase("rwd")) {
            avgDriveSlipDeg = 0.5 * (wheelSlipDeg[2] + wheelSlipDeg[3]);
        } else {
            avgDriveSlipDeg = 0.25 * (wheelSlipDeg[0] + wheelSlipDeg[1] + wheelSlipDeg[2] + wheelSlipDeg[3]);
        }
        double instantSlipSpeed = (avgDriveSlipDeg / Math.max(0.001, dt)) * (Math.PI / 180.0) * rEff;
        double targetDriveSpeed = absSpeed + instantSlipSpeed;
        double inertiaRate = Math.min(1.0, 16.0 * (1.0 / Math.max(0.5, this.flywheelInertia * 2.2)) * dt);
        this.driveWheelLinearSpeed += (targetDriveSpeed - this.driveWheelLinearSpeed) * inertiaRate;
        if (this.driveWheelLinearSpeed < absSpeed) {
            this.driveWheelLinearSpeed = absSpeed;
        }

        // 2. Front wheels (FL=0, FR=1) roll with road speed + any FWD/AWD slip:
        if (!effectiveParkingBrake) {
            wheelRotation[0] += roadDeltaDeg + wheelSlipDeg[0];
            wheelRotation[1] += roadDeltaDeg + wheelSlipDeg[1];
        }

        // 3. Rear wheels (RL=2, RR=3) lock completely on handbrake or parking brake:
        if (effectiveParkingBrake || handbrake) {
            // Handbrake or parking brake locks rear wheels: no rotation!
        } else {
            wheelRotation[2] += roadDeltaDeg + wheelSlipDeg[2];
            wheelRotation[3] += roadDeltaDeg + wheelSlipDeg[3];
        }

        // =========================================================================
        // 12. TRAJECTORY INTEGRATION WITH ADAPTIVE TERRAIN CLIMBING & REBOUND
        // =========================================================================
        double midYawRad = Math.toRadians(yRot + this.deltaYaw * 0.5);
        double cosMid = Math.cos(midYawRad);
        double sinMid = Math.sin(midYawRad);
        double mX = (-sinMid * speed + cosMid * this.lateralSlipVel) * dt;
        double mZ = (cosMid * speed + sinMid * this.lateralSlipVel) * dt;
        double mY = 0.0;

        if (this.crashCooldown > 0.0) {
            this.crashCooldown -= dt;
        }
        if (this.reboundTimer > 0.0) {
            this.reboundTimer -= dt;
        }

        boolean forwardCrash = (throttle > 0.05 || (speed > 0.08 && throttle >= -0.02)) && this.isFrontObstacle;
        boolean reverseCrash = (throttle < -0.05 || (speed < -0.08 && throttle <= 0.02)) && this.isRearObstacle;

        if (forwardCrash) {
            double impactKmh = Math.max(0.0, Math.abs(speed) * 3.6);
            double sideOffset = hitFrontLeft ? probeHalfW : (hitFrontRight ? -probeHalfW : 0.0);
            if (this.crashCooldown <= 0.0) {
                this.isWallHit = true;
                this.lastImpactSpeedKmh = impactKmh;
                this.crashCooldown = 0.35;
                this.lastImpactPoint = new Vec3(pos.x + leftX * sideOffset + forwardX * bumperFrontDist, pos.y + 0.5, pos.z + leftZ * sideOffset + forwardZ * bumperFrontDist);

                this.impactLocalX = (hitFrontLeft ? 0.45 : (hitFrontRight ? -0.45 : 0.0));
                this.impactLocalY = 0.75;
                this.impactLocalZ = -2.85;
                this.impactDirX = (hitFrontLeft ? 0.35 : (hitFrontRight ? -0.35 : 0.0));
                this.impactDirY = 0.10;
                this.impactDirZ = 1.0;
            }

            if (impactKmh < 10.0) {
                double reboundSpeed = -Math.min(0.10, Math.abs(speed) * 0.05);
                this.speed = reboundSpeed;
                this.reboundTimer = 0.05;
                double pushDist = 0.04 + Math.min(0.04, impactKmh * 0.003);
                mX = -forwardX * pushDist;
                mZ = -forwardZ * pushDist;
                mY = 0.0;
            } else {
                double restitution = 0.16 + Math.min(0.12, impactKmh * 0.002);
                double reboundSpeed = -Math.min(1.20, Math.abs(speed) * restitution);
                this.speed = reboundSpeed;
                this.reboundTimer = 0.16;
                double pushDist = 0.08 + Math.min(0.16, impactKmh * 0.003);
                mX = -forwardX * pushDist;
                mZ = -forwardZ * pushDist;
                mY = 0.02;
            }

            if (hitFrontLeft && !hitFrontRight) {
                this.liveYawRate += Math.min(2.0, impactKmh * 0.08);
            } else if (hitFrontRight && !hitFrontLeft) {
                this.liveYawRate -= Math.min(2.0, impactKmh * 0.08);
            }
        } else if (reverseCrash) {
            double impactKmh = Math.max(0.0, Math.abs(speed) * 3.6);
            double sideOffset = hitRearLeft ? probeHalfW : (hitRearRight ? -probeHalfW : 0.0);
            if (this.crashCooldown <= 0.0) {
                this.isWallHit = true;
                this.lastImpactSpeedKmh = impactKmh;
                this.crashCooldown = 0.35;
                this.lastImpactPoint = new Vec3(pos.x + leftX * sideOffset - forwardX * bumperRearDist, pos.y + 0.5, pos.z + leftZ * sideOffset - forwardZ * bumperRearDist);

                this.impactLocalX = (hitRearLeft ? 0.45 : (hitRearRight ? -0.45 : 0.0));
                this.impactLocalY = 0.75;
                this.impactLocalZ = 2.85;
                this.impactDirX = (hitRearLeft ? 0.35 : (hitRearRight ? -0.35 : 0.0));
                this.impactDirY = 0.10;
                this.impactDirZ = -1.0;
            }

            if (impactKmh < 10.0) {
                double reboundSpeed = Math.min(0.10, Math.abs(speed) * 0.05);
                this.speed = reboundSpeed;
                this.reboundTimer = 0.05;
                double pushDist = 0.04 + Math.min(0.04, impactKmh * 0.003);
                mX = forwardX * pushDist;
                mZ = forwardZ * pushDist;
                mY = 0.0;
            } else {
                double restitution = 0.16 + Math.min(0.12, impactKmh * 0.002);
                double reboundSpeed = Math.min(1.20, Math.abs(speed) * restitution);
                this.speed = reboundSpeed;
                this.reboundTimer = 0.16;
                double pushDist = 0.08 + Math.min(0.16, impactKmh * 0.003);
                mX = forwardX * pushDist;
                mZ = forwardZ * pushDist;
                mY = 0.02;
            }

            if (hitRearLeft && !hitRearRight) {
                this.liveYawRate -= Math.min(1.8, impactKmh * 0.07);
            } else if (hitRearRight && !hitRearLeft) {
                this.liveYawRate += Math.min(1.8, impactKmh * 0.07);
            }
        }

        if (hitSideL || hitSideR) {
            double lateralDeflect = 0.05;
            if (hitSideL) {
                mX -= leftX * lateralDeflect;
                mZ -= leftZ * lateralDeflect;
            }
            if (hitSideR) {
                mX += leftX * lateralDeflect;
                mZ += leftZ * lateralDeflect;
            }
            double lateralSpeedKmh = Math.abs(this.liveYawRate * 2.0 + (this.isDrifting ? speed * 1.8 : 0.0)) * 3.6;
            if ((lateralSpeedKmh >= 8.0 || Math.abs(speed * 3.6) >= 15.0) && this.crashCooldown <= 0.0) {
                this.isWallHit = true;
                this.lastImpactSpeedKmh = Math.max(lateralSpeedKmh, Math.abs(speed * 3.6) * 0.7);
                this.crashCooldown = 0.35;
                this.lastImpactPoint = new Vec3(pos.x + (hitSideL ? leftX : -leftX) * probeHalfW, pos.y + 0.5, pos.z + (hitSideL ? leftZ : -leftZ) * probeHalfW);
                this.impactLocalX = hitSideL ? 0.95 : -0.95;
                this.impactLocalY = 0.70;
                this.impactLocalZ = 0.0;
                this.impactDirX = hitSideL ? -1.0 : 1.0;
                this.impactDirY = 0.05;
                this.impactDirZ = 0.0;
            }
        }

        if (forwardCrash || reverseCrash) {
            // mY rebound bounce preserved
        } else if (isTrulyOnGround) {
            if (Double.isNaN(this.targetSurfaceY) || Double.isInfinite(this.targetSurfaceY) || this.targetSurfaceY < -900.0) {
                this.targetSurfaceY = pos.y;
            }
            double diffY = this.targetSurfaceY - pos.y;

            if (diffY > 0.0) {
                double climbRate = Math.min(10.0, Math.max(3.5, (Math.abs(speed) * 4.0 + Math.abs(throttle) * 6.0) * Math.max(1.0, this.offroadCapability)));
                double maxLift = Math.min(1.40, Math.max(0.85, maxClimbStep + 0.15));
                mY = Math.min(diffY, climbRate * dt);
                mY = Math.min(mY, maxLift);
                this.verticalSpeed = 0.0;
            } else {
                double descendRate = Math.min(10.0, Math.max(3.5, (Math.abs(speed) * 4.0 + 3.0) * Math.max(1.0, this.offroadCapability)));
                double maxDrop = Math.min(1.40, Math.max(0.85, maxClimbStep + 0.15));
                mY = Math.max(diffY, -descendRate * dt);
                mY = Math.max(mY, -maxDrop);
                this.verticalSpeed = 0.0;
            }
        } else {
            mY = verticalSpeed * dt;
            // Anti-Tunneling: if falling vertically would penetrate solid ground beneath vehicle, land immediately
            double airborneFloor = -999.0;
            if (cachedHBelly > -900.0) airborneFloor = Math.max(airborneFloor, cachedHBelly);
            if (cachedHMid > -900.0) airborneFloor = Math.max(airborneFloor, cachedHMid);
            double avgWheels = 0.0;
            int countW = 0;
            if (cachedHFL > -900.0) { avgWheels += cachedHFL; countW++; }
            if (cachedHFR > -900.0) { avgWheels += cachedHFR; countW++; }
            if (cachedHRL > -900.0) { avgWheels += cachedHRL; countW++; }
            if (cachedHRR > -900.0) { avgWheels += cachedHRR; countW++; }
            if (countW > 0) airborneFloor = Math.max(airborneFloor, avgWheels / countW);

            if (airborneFloor > -900.0 && (pos.y + mY) <= airborneFloor) {
                mY = airborneFloor - pos.y;
                this.targetSurfaceY = airborneFloor;
                this.isGrounded = true;
                this.isPhysicallySupported = true;
                this.verticalSpeed = 0.0;
            }
        }

        mY = Mth.clamp(mY, -2.5, 1.45);
        if (Double.isNaN(mY) || Double.isInfinite(mY)) mY = 0.0;
        if (Double.isNaN(mX) || Double.isInfinite(mX)) mX = 0.0;
        if (Double.isNaN(mZ) || Double.isInfinite(mZ)) mZ = 0.0;

        // =========================================================================
        // 13. LIVE ENGINE RPM CALCULATION
        // =========================================================================
        double effectivePowertrainSpeed = Math.abs(speed);
        this.engineRPM = calculateRPM(effectivePowertrainSpeed, this.currentGear, Math.abs(throttle), this.gearShiftTimer, this.boost, engineOn, dt);
        this.enginePitch = (float) (0.50 + (this.engineRPM / Math.max(3000.0, this.redlineRpm)) * 1.10);

        return new Vec3(mX, mY, mZ);
    }

    private double calculateRPM(double s, int g, double th, double shiftT, double b, boolean engineOn, double dt) {
        if (!engineOn) {
            this.engineRPM += (0.0 - this.engineRPM) * Math.min(1.0, 10.0 * dt);
            if (this.engineRPM < 5.0) this.engineRPM = 0.0;
            return this.engineRPM;
        }

        if (this.isBurnoutActive) {
            double target = this.redlineRpm * (0.80 + th * 0.15);
            this.engineRPM += (target - this.engineRPM) * Math.min(1.0, 10.0 * dt);
            return this.engineRPM;
        }

        int activeGear = (g == -1) ? -1 : Math.max(1, g);
        double kinematicRpm = getKinematicRpm(s, activeGear);

        double targetRPM;
        double revRate;

        // Flywheel rotational inertia scaling: heavier flywheel accelerates with authentic momentum
        double inertiaRevMult = 1.0 / Math.max(0.5, this.flywheelInertia * 2.2);

        if (shiftT > 0.0 && s > 0.35) {
            // Clutch disengaged during in-motion gear change: revs settle smoothly towards next gear kinematic RPM
            targetRPM = Math.max(this.idleRpm, kinematicRpm);
            revRate = 10.0 * inertiaRevMult;
        } else if (!this.hasDriver || (s < 0.25 && th < 0.05)) {
            // Neutral / Stopped idle with classic cam lope oscillation
            double timeSec = (System.currentTimeMillis() % 1000000L) / 1000.0;
            double lopePrimary = Math.sin(timeSec * (2.0 * Math.PI * 2.35));
            double lopeSecondary = Math.sin(timeSec * (2.0 * Math.PI * 4.70) + 0.6) * 0.40;
            double lopeTertiary = Math.sin(timeSec * (2.0 * Math.PI * 1.15) + 1.2) * 0.25;
            double camLope = (lopePrimary + lopeSecondary + lopeTertiary) * 55.0;
            targetRPM = this.idleRpm + camLope;
            revRate = (this.engineRPM > this.idleRpm + 250.0) ? (5.0 * inertiaRevMult) : 7.0;
        } else if (th < 0.05) {
            // Engine Braking (Overrun / Coasting in gear at speed):
            // Mechanically locked to road speed through transmission ratios; never drops to idle while rolling!
            targetRPM = Math.max(this.idleRpm, kinematicRpm);
            revRate = 8.0 * inertiaRevMult;
        } else {
            // Driving under throttle:
            if (activeGear == 1 || activeGear == -1) {
                // Launch gear (1st or Reverse):
                // Immediate V8 torque converter stall flare: engine rapidly jumps to the converter stall plateau
                // (~2000-2200 RPM on full throttle) delivering instantaneous punch off the line!
                double stallPlateau = this.idleRpm + th * Math.min(1350.0, (this.redlineRpm - this.idleRpm) * 0.35);
                targetRPM = Math.max(kinematicRpm, stallPlateau);
                double climbRevRate = (th > 0.65) ? (22.0 * inertiaRevMult) : (14.0 * inertiaRevMult);
                revRate = (targetRPM > this.engineRPM) ? climbRevRate : (9.0 * inertiaRevMult);
            } else {
                // Higher gears (2, 3, 4, 5...):
                // Mechanically coupled with subtle fluid coupling compliance
                double fluidSlip = th * 95.0;
                targetRPM = Math.max(this.idleRpm, kinematicRpm + fluidSlip);
                revRate = (targetRPM > this.engineRPM) ? (7.5 * inertiaRevMult) : (9.0 * inertiaRevMult);
            }
        }

        // Rev Limiter
        if (this.engineLimiterType.equalsIgnoreCase("hard_cut") && this.engineRPM >= this.redlineRpm) {
            this.hardCutTimer = 0.06;
        }
        if (this.hardCutTimer > 0.0) {
            this.hardCutTimer -= dt;
            targetRPM = this.redlineRpm - 200.0;
            revRate = 20.0;
        }

        this.engineRPM += (targetRPM - this.engineRPM) * Math.min(1.0, revRate * dt);
        this.engineRPM = Mth.clamp(this.engineRPM, this.idleRpm * 0.75, this.redlineRpm * 1.05);
        return this.engineRPM;
    }

    public BlockState sampleSurfaceBlock(Level level, double x, double currentY, double z) {
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int by = Mth.floor(currentY);
        int bz = Mth.floor(z);
        for (int dy = 1; dy >= -5; dy--) {
            mutPos.set(bx, by + dy, bz);
            BlockState st = level.getBlockState(mutPos);
            if (!st.isAir() && (st.isCollisionShapeFullBlock(level, mutPos) || !st.getCollisionShape(level, mutPos).isEmpty())) {
                return st;
            }
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    public static SurfaceType getSurfaceType(BlockState state) {
        if (state == null || state.isAir()) return SurfaceType.ASPHALT;
        if (state.is(BlockTags.ICE)) return SurfaceType.ICE;
        if (state.is(BlockTags.SNOW)) return SurfaceType.SNOW;
        if (state.is(BlockTags.SAND)) return SurfaceType.SAND;

        Block block = state.getBlock();
        ResourceLocation regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        String path = regName != null ? regName.getPath().toLowerCase(Locale.ROOT) : "";

        if (path.contains("ice")) return SurfaceType.ICE;
        if (path.contains("snow")) return SurfaceType.SNOW;
        if (path.contains("sand")) return SurfaceType.SAND;
        if (path.contains("gravel")) return SurfaceType.GRAVEL;
        if (path.contains("mud")) return SurfaceType.MUD;
        if (state.is(BlockTags.DIRT) || path.contains("dirt") || path.contains("path") || path.contains("podzol") || path.contains("farmland")) {
            return SurfaceType.DIRT;
        }
        if (path.contains("grass") || path.contains("moss") || state.is(BlockTags.LEAVES) || path.contains("leaves")) {
            return SurfaceType.GRASS;
        }
        return SurfaceType.ASPHALT;
    }

    private double sampleGroundFast(Level level, double x, double currentY, double z, double maxStepUp) {
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int by = Mth.floor(currentY);
        int bz = Mth.floor(z);

        int maxDy = Math.min(4, (int) Math.ceil(maxStepUp + 1.20));
        for (int dy = maxDy; dy >= -14; dy--) {
            mutPos.set(bx, by + dy, bz);
            BlockState st = level.getBlockState(mutPos);
            if (st.isAir()) continue;

            double topY;
            if (st.isCollisionShapeFullBlock(level, mutPos)) {
                topY = (by + dy) + 1.0;
            } else {
                VoxelShape shape = st.getCollisionShape(level, mutPos);
                if (shape.isEmpty()) continue;
                topY = (by + dy) + shape.max(Direction.Axis.Y);
            }

            boolean upperBlocked = false;
            BlockPos.MutableBlockPos hPos = new BlockPos.MutableBlockPos();
            int minCheckY = (int) Math.floor(topY + 0.05);
            int maxCheckY = (int) Math.floor(topY + 1.55);
            for (int cy = minCheckY; cy <= maxCheckY; cy++) {
                hPos.set(bx, cy, bz);
                BlockState checkSt = level.getBlockState(hPos);
                if (!checkSt.isAir()) {
                    VoxelShape cShape = checkSt.getCollisionShape(level, hPos);
                    if (!cShape.isEmpty()) {
                        double shapeMinY = cy + cShape.min(Direction.Axis.Y);
                        double shapeMaxY = cy + cShape.max(Direction.Axis.Y);
                        if (shapeMaxY > topY + 0.05 && shapeMinY < topY + 1.55) {
                            upperBlocked = true;
                            break;
                        }
                    }
                }
            }
            if (!upperBlocked) {
                return topY;
            }
        }
        return -999.0;
    }

    public boolean isSolidObstacle(Level level, double x, double baselineY, double z, double maxStepUp) {
        return isSolidObstacle(level, x, baselineY, z, maxStepUp, baselineY);
    }

    public boolean isSolidObstacle(Level level, double x, double baselineY, double z, double maxStepUp, double approachStepY) {
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int by = Mth.floor(baselineY);
        double relX = x - bx;
        double relZ = z - bz;

        double highestObstacleTopY = -999.0;
        boolean hasSolidInBodyZone = false;

        for (int dy = -1; dy <= 4; dy++) {
            mutPos.set(bx, by + dy, bz);
            BlockState st = level.getBlockState(mutPos);
            if (st.isAir()) continue;

            VoxelShape shape = st.getCollisionShape(level, mutPos);
            if (shape.isEmpty()) continue;

            boolean intersectsShape = false;
            for (AABB box : shape.toAabbs()) {
                if (relX >= box.minX - 0.04 && relX <= box.maxX + 0.04 &&
                    relZ >= box.minZ - 0.04 && relZ <= box.maxZ + 0.04) {
                    intersectsShape = true;
                    break;
                }
            }
            if (!intersectsShape) continue;

            double blockTopY = (by + dy) + shape.max(Direction.Axis.Y);
            double blockBottomY = (by + dy) + shape.min(Direction.Axis.Y);

            if (blockTopY > highestObstacleTopY) {
                highestObstacleTopY = blockTopY;
            }

            if (blockTopY > baselineY + 0.25 && blockBottomY < baselineY + 1.95) {
                hasSolidInBodyZone = true;
            }
        }

        if (!hasSolidInBodyZone || highestObstacleTopY < -900.0) {
            return false;
        }

        double stepRise = highestObstacleTopY - approachStepY;

        if (stepRise <= 0.60) {
            return false;
        }

        boolean hasHeadroom = true;
        BlockPos.MutableBlockPos hPos = new BlockPos.MutableBlockPos();
        int minCheckY = (int) Math.floor(highestObstacleTopY + 0.10);
        int maxCheckY = (int) Math.floor(highestObstacleTopY + 1.55);
        for (int cy = minCheckY; cy <= maxCheckY; cy++) {
            hPos.set(bx, cy, bz);
            BlockState checkSt = level.getBlockState(hPos);
            if (!checkSt.isAir()) {
                VoxelShape cShape = checkSt.getCollisionShape(level, hPos);
                if (!cShape.isEmpty()) {
                    double shapeMinY = cy + cShape.min(Direction.Axis.Y);
                    double shapeMaxY = cy + cShape.max(Direction.Axis.Y);
                    if (shapeMaxY > highestObstacleTopY + 0.05 && shapeMinY < highestObstacleTopY + 1.55) {
                        hasHeadroom = false;
                        break;
                    }
                }
            }
        }

        if (hasHeadroom && stepRise <= maxStepUp + 0.15) {
            return false;
        }

        return true;
    }

    public void shootTire(int index) {
        if (index >= 0 && index < 4) {
            tireDeflateRate[index] = 1.0f / 6.0f;
        }
    }

    public boolean isTireFlat(int index) {
        if (index < 0 || index >= 4) return false;
        double p = (index == 0) ? tirePressureFL : (index == 1) ? tirePressureFR : (index == 2) ? tirePressureRL : tirePressureRR;
        return p < 0.8 || tireAir[index] < 0.05f;
    }

    public void rightVehicle() {
        this.roll = 0.0;
        this.pitch = 0.0;
        this.groundRoll = 0.0;
        this.groundPitch = 0.0;
        this.terrainPitch = 0.0;
    }

    public int selectGear(double s, int cur, double th, double rpm) {
        // 1. Directional gear management (Reverse vs Forward):
        if (s > 0.20) {
            // Forward motion at speed: strictly forward gears only
            if (cur < 1) cur = 1;
        } else if (s < -0.20) {
            // Backward motion at speed: reverse gear
            if (cur != -1 && th < -0.05) return -1;
        } else {
            // Stopped or crawling (-0.20 <= s <= 0.20)
            if (cur == -1) {
                // In reverse: shift to forward if throttle forward is pressed or rolling forward
                if (th > 0.05 || s > 0.05) {
                    return 1;
                }
                return -1;
            } else {
                // In forward: shift to reverse only if driver holds reverse at near stop
                if (th < -0.05 && s <= 0.05) {
                    return -1;
                }
            }
        }

        double absSpeed = Math.abs(s);
        int maxG = Math.max(1, this.maxGears);
        double idleStallRpm = this.idleRpm * 1.08;

        // 2. Physical downshift (engine stall protection & kickdown):
        // If current gear's kinematic RPM at this road speed drops below stall limit, downshift
        if (cur > 1) {
            double currentKinematicRpm = getKinematicRpm(absSpeed, cur);
            if (currentKinematicRpm < idleStallRpm) {
                for (int g = cur - 1; g >= 1; g--) {
                    if (g == 1 || getKinematicRpm(absSpeed, g) >= idleStallRpm) {
                        return g;
                    }
                }
                return 1;
            }

            // Real load-demand downshift & kickdown (Governor vs Throttle Modulator equilibrium):
            // Kickdown requires aggressive throttle (th > 0.75) and that engine is truly bogged down (< 40% redline)
            double thRatio = Mth.clamp(Math.abs(th), 0.0, 1.0);
            if (thRatio > 0.75) {
                double targetDownshiftPowerband = this.idleRpm + (this.redlineRpm - this.idleRpm) * 0.40;
                if (currentKinematicRpm < targetDownshiftPowerband) {
                    double lowerGearRpm = getKinematicRpm(absSpeed, cur - 1);
                    if (lowerGearRpm < this.redlineRpm * 0.82) {
                        return cur - 1;
                    }
                }
            }
        }

        // 3. Physical upshift with Grade Logic (Incline anti-hunting):
        // Shift up when current gear's kinematic RPM reaches target shift point:
        // - On hills/inclines (terrainPitch > 0): raises shift point to hold gear and prevent shift cycling
        // - At light cruise throttle (th ~ 0.15): shifts early to save fuel
        // - At wide-open-throttle (th = 1.0): holds gear until 94% of redline for maximum power
        if (cur >= 1 && cur < maxG) {
            double climbIncline = Math.max(0.0, this.terrainPitch);
            double gradeHoldFactor = Math.min(0.25, climbIncline * 0.45);

            double cruiseShiftRpm = this.idleRpm + (this.redlineRpm - this.idleRpm) * (0.35 + gradeHoldFactor);
            double wotShiftRpm = this.idleRpm + (this.redlineRpm - this.idleRpm) * 0.94;
            double thRatio = Mth.clamp(Math.abs(th), 0.0, 1.0);
            double targetShiftRpm = cruiseShiftRpm + Math.pow(thRatio, 1.25) * (wotShiftRpm - cruiseShiftRpm);

            double curKinematicRpm = getKinematicRpm(absSpeed, cur);
            double nextGearKinematicRpm = getKinematicRpm(absSpeed, cur + 1);

            // Anti-hunting hysteresis band: next gear must land well above kickdown threshold
            double minNextGearRpm = Math.max(this.idleRpm * 1.25, this.idleRpm + (this.redlineRpm - this.idleRpm) * 0.32);
            if (curKinematicRpm >= targetShiftRpm && nextGearKinematicRpm >= minNextGearRpm) {
                return cur + 1;
            }
        }

        return cur;
    }

    public int selectGear(double s, int cur) {
        return selectGear(s, cur, 0.5, this.engineRPM);
    }
}