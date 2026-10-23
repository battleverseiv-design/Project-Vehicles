package com.evgen.cardecor.physics.dynamics;

/**
 * Specifications and tuning configurations for vehicle powertrain, transmission,
 * aerodynamics, and mass properties.
 *
 * Based on Jolt Physics (VehicleEngineSettings, VehicleTransmissionSettings)
 * and Marco Monster ("Car Physics for Games").
 */
public class PowertrainSpec {

    // ------------------------------------------------------------------------
    // Engine Parameters (Jolt VehicleEngineSettings)
    // ------------------------------------------------------------------------
    public float minRpm = 850.0f;               // Idle RPM
    public float peakTorqueRpm = 4200.0f;        // RPM at maximum torque output
    public float maxTorqueNm = 420.0f;           // Peak torque (N*m)
    public float redlineTorqueNm = 310.0f;       // Torque at redline
    public float maxRpm = 6800.0f;               // Redline RPM
    public float revLimiterRpm = 7000.0f;        // Fuel cutoff / rev limiter RPM
    public float engineInertia = 0.22f;          // Flywheel inertia (kg*m^2)
    public float angularDamping = 0.15f;         // Internal engine friction damping
    public float engineBrakingCoeff = 120.0f;    // Drag torque on throttle lift-off

    // ------------------------------------------------------------------------
    // Transmission & Clutch Parameters (Jolt VehicleTransmissionSettings)
    // ------------------------------------------------------------------------
    public float[] gearRatios = { 3.50f, 2.15f, 1.45f, 1.10f, 0.85f, 0.70f };
    public float reverseGearRatio = -3.40f;
    public float differentialRatio = 3.90f;      // Final Drive ratio
    public float clutchStrength = 2.0f;          // Slip-mode clutch plate stiffness (Euler stable at 80 Hz)
    public float clutchFrictionCoeff = 0.38f;    // mu [-] (Coulomb friction coefficient)
    public float clutchEffectiveRadius = 0.100f; // R_eff [m] (effective friction radius)
    public float clutchMaxClampingForce = 9500.0f; // F_n,max [N] (diaphragm spring max clamp)
    public float clutchCoolingH_A = 4.5f;        // W/K (convective clutch cooling)
    public static final float CLUTCH_THERMAL_MASS = 2500.0f; // J/K (flywheel + clutch basket ~5.5 kg cast iron)
    public int clutchPlatesCount = 2;            // z [-] (friction surfaces count)
    public float engineStallRpm = 380.0f;        // Stall threshold below which combustion fails
    public float shiftDurationUp = 0.18f;        // Upshift clutch disengagement (seconds)
    public float shiftDurationDown = 0.12f;      // Downshift disengagement (seconds)
    public float transmissionEfficiency = 0.90f; // Mechanical drivetrain efficiency
    public boolean isAutomatic = false;

    // ------------------------------------------------------------------------
    // Aerodynamics & Rolling Resistance (Marco Monster)
    // ------------------------------------------------------------------------
    public float frontalAreaM2 = 2.2f;           // Frontal cross-sectional area A (m^2)
    public float dragCd = 0.35f;                 // Drag coefficient Cd (dimensionless)
    public float downforceCl = 0.15f;            // Aerodynamic downforce coefficient Cl
    public float groundRollResist = 0.015f;      // Rolling resistance coefficient Crr

    // ------------------------------------------------------------------------
    // Mass & Geometry
    // ------------------------------------------------------------------------
    public float baseMassKg = 1650.0f;
    public float wheelRadius = 0.38f;            // Wheel radius in meters
    public float rollInfluence = 0.55f;          // Bullet Physics roll influence factor

    // ------------------------------------------------------------------------
    // Factory Presets
    // ------------------------------------------------------------------------

    /**
     * Preset 1: Light Sport Coupe.
     * 1250 kg, 450 Nm naturally aspirated high-revving engine, 6-speed close-ratio transmission,
     * low aerodynamic drag, high top speed achieved naturally via power vs drag balance.
     */
    public static PowertrainSpec createLightSportTurbo() {
        PowertrainSpec s = new PowertrainSpec();
        s.baseMassKg = 1250.0f;
        s.frontalAreaM2 = 1.95f;
        s.dragCd = 0.30f;
        s.downforceCl = 0.35f;
        s.minRpm = 950.0f;
        s.peakTorqueRpm = 4500.0f;
        s.maxTorqueNm = 450.0f;
        s.redlineTorqueNm = 360.0f;
        s.maxRpm = 7200.0f;
        s.revLimiterRpm = 7450.0f;
        s.engineInertia = 0.38f;
        s.gearRatios = new float[] { 3.65f, 2.25f, 1.55f, 1.18f, 0.92f, 0.74f };
        s.reverseGearRatio = -3.45f;
        s.differentialRatio = 3.85f;
        s.clutchStrength = 2.0f;
        s.rollInfluence = 0.58f;
        return s;
    }

    /**
     * Preset 2: Family Sedan / Daily Cruiser.
     * 1650 kg, 240 Nm, 5-speed smooth transmission,
     * realistic progressive acceleration without artificial clamps.
     */
    public static PowertrainSpec createFamilySedan() {
        PowertrainSpec s = new PowertrainSpec();
        s.baseMassKg = 1650.0f;
        s.frontalAreaM2 = 2.20f;
        s.dragCd = 0.34f;
        s.downforceCl = 0.10f;
        s.minRpm = 800.0f;
        s.peakTorqueRpm = 3800.0f;
        s.maxTorqueNm = 240.0f;
        s.redlineTorqueNm = 185.0f;
        s.maxRpm = 6200.0f;
        s.revLimiterRpm = 6400.0f;
        s.engineInertia = 0.22f;
        s.gearRatios = new float[] { 3.42f, 2.05f, 1.35f, 0.98f, 0.76f };
        s.reverseGearRatio = -3.35f;
        s.differentialRatio = 3.95f;
        s.clutchStrength = 2.0f;
        s.rollInfluence = 0.50f;
        return s;
    }

    /**
     * Preset 3: Heavy Armored Truck / Off-Road Hauler.
     * 5500 kg, 1100 Nm low-rev diesel, 6 heavy torque-multiplying gears,
     * high aerodynamic drag and rolling resistance.
     */
    public static PowertrainSpec createHeavyArmoredTruck() {
        PowertrainSpec s = new PowertrainSpec();
        s.baseMassKg = 5500.0f;
        s.frontalAreaM2 = 4.80f;
        s.dragCd = 0.62f;
        s.downforceCl = 0.05f;
        s.groundRollResist = 0.022f;
        s.minRpm = 650.0f;
        s.peakTorqueRpm = 1800.0f;
        s.maxTorqueNm = 1100.0f;
        s.redlineTorqueNm = 720.0f;
        s.maxRpm = 3600.0f;
        s.revLimiterRpm = 3800.0f;
        s.engineInertia = 0.35f;
        s.gearRatios = new float[] { 4.95f, 3.15f, 2.10f, 1.42f, 1.00f, 0.72f };
        s.reverseGearRatio = -4.60f;
        s.differentialRatio = 4.55f;
        s.clutchStrength = 2.5f;
        s.rollInfluence = 0.75f;
        return s;
    }
}
