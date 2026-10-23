package com.evgen.cardecor.physics.dynamics;

import com.evgen.cardecor.physics.engine.RigidBodyOBB;
import org.joml.Vector3f;

/**
 * Aerodynamics, rolling resistance, and dynamic mass solver based on
 * Marco Monster ("Car Physics for Games") and BeamNG Lite principles.
 *
 * Implements:
 * 1. Quadratic aerodynamic drag (Air Drag) that naturally bounds maximum vehicle speed.
 * 2. High-speed aerodynamic downforce pushing the chassis down along -localUp.
 * 3. Rolling resistance scaling with total vehicle weight and surface roughness.
 * 4. Dynamic passenger and cargo mass aggregation.
 */
public class MarcoMonsterAero {

    public static final float AIR_DENSITY = 1.225f; // Sea-level air density rho (kg/m^3)
    public static final float GRAVITY = 18.5f;       // Effective Minecraft vehicle gravity (m/s^2)
    public static final float PASSENGER_MASS_KG = 85.0f;

    /**
     * Calculates the dynamic combined mass of chassis, driver, passengers, and cargo.
     */
    public static float calculateTotalMass(float baseMassKg, int passengerCount, float cargoMassKg) {
        return Math.max(250.0f, baseMassKg + Math.max(0, passengerCount) * PASSENGER_MASS_KG + Math.max(0.0f, cargoMassKg));
    }

    /**
     * Applies quadratic air drag, downforce, and rolling resistance to a RigidBodyOBB.
     */
    public static void applyAeroAndRollingResistance(
            RigidBodyOBB body,
            Vector3f localUp,
            Vector3f localForward,
            float frontalAreaM2,
            float dragCd,
            float downforceCl,
            float groundRollResist
    ) {
        float vx = body.linearVelocity.x;
        float vy = body.linearVelocity.y;
        float vz = body.linearVelocity.z;
        float speedSq = vx * vx + vy * vy + vz * vz;

        if (speedSq < 0.001f) return;
        float speed = (float) Math.sqrt(speedSq);

        // 1. Aerodynamic Drag: F_drag = -0.5 * rho * Cd * A * |v| * v
        float cDrag = 0.5f * AIR_DENSITY * dragCd * frontalAreaM2;
        float dragForceMag = -cDrag * speed;
        body.forceAccum.add(vx * dragForceMag, vy * dragForceMag, vz * dragForceMag);

        // 2. Aerodynamic Downforce: F_downforce = 0.5 * rho * Cl * A * v_fwd^2
        float fwdSpeed = body.linearVelocity.dot(localForward);
        float cDownforce = 0.5f * AIR_DENSITY * downforceCl * frontalAreaM2;
        float downforceN = cDownforce * (fwdSpeed * fwdSpeed);
        body.forceAccum.fma(-downforceN, localUp);

        // 3. Tire Rolling Resistance: F_rr = -C_rr * m * g * sign(v_fwd)
        if (body.groundedWheelsCount > 0 && Math.abs(fwdSpeed) > 0.05f) {
            float normalWeightN = body.mass * GRAVITY;
            float rrForce = -Math.signum(fwdSpeed) * groundRollResist * normalWeightN;
            body.forceAccum.fma(rrForce, localForward);
        }
    }

    /**
     * Zero-GC calculation for ODE4J or raw physics systems.
     * Computes the net aerodynamic and rolling resistance force to be applied at COM.
     */
    public static void computeAeroAndRollingResistance(
            Vector3f linVel,
            Vector3f localUp,
            Vector3f localForward,
            float totalMass,
            boolean isGrounded,
            float frontalAreaM2,
            float dragCd,
            float downforceCl,
            float groundRollResist,
            Vector3f outForce
    ) {
        outForce.zero();
        float speedSq = linVel.lengthSquared();
        if (speedSq < 0.001f) return;

        float speed = (float) Math.sqrt(speedSq);

        // 1. Air Drag
        float cDrag = 0.5f * AIR_DENSITY * dragCd * frontalAreaM2;
        float dragMag = -cDrag * speed;
        outForce.add(linVel.x * dragMag, linVel.y * dragMag, linVel.z * dragMag);

        // 2. Downforce
        float fwdSpeed = linVel.dot(localForward);
        float cDownforce = 0.5f * AIR_DENSITY * downforceCl * frontalAreaM2;
        float downforceN = cDownforce * (fwdSpeed * fwdSpeed);
        outForce.fma(-downforceN, localUp);

        // 3. Rolling Resistance
        if (isGrounded && Math.abs(fwdSpeed) > 0.05f) {
            float normalWeightN = totalMass * GRAVITY;
            float rrForce = -Math.signum(fwdSpeed) * groundRollResist * normalWeightN;
            outForce.fma(rrForce, localForward);
        }
    }
}
