package com.evgen.cardecor.pack;

import com.google.gson.annotations.SerializedName;

public class EngineDefinition {
    public String id = "default_engine";
    public String name = "Standard Engine";
    public String type = "i6"; // "inline6", "v8", "diesel", "i4", "v6", etc.
    public String author = "CarDecor";

    public PhysicsConfig physics = new PhysicsConfig();
    public BlockConfig block = new BlockConfig();
    public ValvetrainConfig valvetrain = new ValvetrainConfig();
    @SerializedName(value = "fuel_system", alternate = {"fuelSystem", "fuel"})
    public FuelConfig fuelSystem = new FuelConfig();
    public FlywheelConfig flywheel = new FlywheelConfig();
    @SerializedName(value = "forced_induction", alternate = {"forcedInduction", "turbo"})
    public ForcedInductionConfig forcedInduction = new ForcedInductionConfig();
    public LimitsConfig limits = new LimitsConfig();
    public SoundsConfig sounds = new SoundsConfig();

    public static class BlockConfig {
        public int cylinders = 8;
        public String configuration = "v"; // "v", "inline", "boxer", "rotary"
        @SerializedName(value = "bank_angle_deg", alternate = {"bankAngleDeg", "bank_angle"})
        public double bankAngleDeg = 90.0;
        @SerializedName(value = "bore_mm", alternate = {"boreMm", "bore"})
        public double boreMm = 101.6;
        @SerializedName(value = "stroke_mm", alternate = {"strokeMm", "stroke"})
        public double strokeMm = 76.2;
        @SerializedName(value = "compression_ratio", alternate = {"compressionRatio", "cr"})
        public double compressionRatio = 8.4;
        @SerializedName(value = "crank_type", alternate = {"crankType", "crank"})
        public String crankType = "crossplane"; // "crossplane", "flatplane", "even_fire"
    }

    public static class ValvetrainConfig {
        public String type = "ohv_pushrod"; // "ohv_pushrod", "sohc_2v", "dohc_4v"
        @SerializedName(value = "valves_per_cylinder", alternate = {"valvesPerCylinder", "valves"})
        public int valvesPerCylinder = 2;
        @SerializedName(value = "cam_profile", alternate = {"camProfile", "cam"})
        public String camProfile = "truck_towing"; // "truck_towing", "street_stock", "race_aggressive"
        @SerializedName(value = "valve_float_rpm", alternate = {"valveFloatRpm", "valve_float"})
        public double valveFloatRpm = 4800.0;
    }

    public static class FuelConfig {
        public String type = "carburetor_2bbl"; // "carburetor_1bbl", "carburetor_2bbl", "carburetor_4bbl", "electronic_mpi", "direct_injection", "diesel_pump"
        @SerializedName(value = "fuel_octane", alternate = {"fuelOctane", "octane"})
        public double fuelOctane = 92.0;
    }

    public static class FlywheelConfig {
        @SerializedName(value = "mass_kg", alternate = {"massKg", "mass", "weight_kg"})
        public double massKg = 18.5;
        @SerializedName(value = "radius_m", alternate = {"radiusM", "radius"})
        public double radiusM = 0.16;
        @SerializedName(value = "friction_factor", alternate = {"frictionFactor"})
        public double frictionFactor = 1.0;
    }

    public static class ForcedInductionConfig {
        public String type = "na"; // "na", "turbo", "twin_turbo", "supercharger_roots", "supercharger_centrifugal"
        @SerializedName(value = "boost_target_bar", alternate = {"boostTargetBar", "boost_bar"})
        public double boostTargetBar = 0.0;
        @SerializedName(value = "spool_rpm", alternate = {"spoolRpm", "spool"})
        public double spoolRpm = 2200.0;
    }

    public static class LimitsConfig {
        @SerializedName(value = "limiter_type", alternate = {"limiterType", "limiter"})
        public String limiterType = "valve_float"; // "valve_float", "hard_cut", "soft_cut", "diesel_governor"
        @SerializedName(value = "idle_rpm", alternate = {"idleRpm", "idle"})
        public double idleRpm = 650.0;
        @SerializedName(value = "redline_rpm", alternate = {"redlineRpm", "redline"})
        public double redlineRpm = 4400.0;
        @SerializedName(value = "max_rpm", alternate = {"maxRpm", "max"})
        public double maxRpm = 4800.0;
    }

    public static class PhysicsConfig {
        public double horsepower = 180.0;
        @SerializedName(value = "torque_nm", alternate = {"torqueNm", "torque"})
        public double torqueNm = 280.0;
        @SerializedName(value = "idle_rpm", alternate = {"idleRpm", "idle"})
        public double idleRpm = 850.0;
        @SerializedName(value = "redline_rpm", alternate = {"redlineRpm", "redline"})
        public double redlineRpm = 6000.0;
        @SerializedName(value = "max_rpm", alternate = {"maxRpm", "max"})
        public double maxRpm = 6500.0;
        @SerializedName(value = "rev_speed", alternate = {"revSpeed"})
        public double revSpeed = 1.0;
        @SerializedName(value = "engine_braking", alternate = {"engineBraking"})
        public double engineBraking = 1.0;
        @SerializedName(value = "fuel_consumption", alternate = {"fuelConsumption"})
        public double fuelConsumption = 1.0;
    }

    public static class SoundsConfig {
        public String starter = "cardecor:starter_i6";
        public String start = "cardecor:blue_sedan_start";
        public String stop = "cardecor:engine_stop_i6";
        public String kickdown = "cardecor:engine_kickdown";

        public Layers layers = new Layers();
        public Transitions transitions = new Transitions();
        @SerializedName(value = "camshaft_lope", alternate = {"camshaftLope", "cam_lope"})
        public boolean camshaftLope = false;
        @SerializedName(value = "pitch_multiplier", alternate = {"pitchMultiplier"})
        public float pitchMultiplier = 1.0F;
        @SerializedName(value = "master_volume", alternate = {"masterVolume"})
        public float masterVolume = 1.0F;
    }

    public static class Layers {
        public String idle = "cardecor:blue_sedan_idle";
        public String low  = "cardecor:blue_sedan_low";
        public String mid  = "cardecor:blue_sedan_mid";
        public String high = "cardecor:blue_sedan_high";
        public String max  = "cardecor:blue_sedan_max";
    }

    public static class Transitions {
        @SerializedName(value = "idle_to_low", alternate = {"idleToLow"})
        public float idleToLow = 1600.0F;
        @SerializedName(value = "low_to_mid", alternate = {"lowToMid"})
        public float lowToMid  = 2800.0F;
        @SerializedName(value = "mid_to_high", alternate = {"midToHigh"})
        public float midToHigh = 4200.0F;
        @SerializedName(value = "high_to_max", alternate = {"highToMax"})
        public float highToMax = 5400.0F;
    }
}
