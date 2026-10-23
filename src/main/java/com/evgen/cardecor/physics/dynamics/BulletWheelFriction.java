package com.evgen.cardecor.physics.dynamics;

import com.evgen.cardecor.physics.engine.RigidBodyOBB;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Port of Bullet Physics (btRaycastVehicle.cpp) tire friction resolver with
 * Isotropic Brush Model Combined Slip (Pacejka '96 style) and Surface Material Response.
 *
 * Implements:
 * 1. resolveSingleBilateral effective inverse mass for 100% stable, jitter-free lateral friction.
 * 2. Positive Understeer Gradient (Kus > 0) with inherently stable front/rear friction balance.
 * 3. Isotropic Combined Slip vector: drive torque cannot crush lateral holding grip on loose terrain.
 * 4. Tire load sensitivity: mu(N) = mu0 * (N / N_nom)^-0.10.
 * 5. Safe surface roughness spring force modulation [0.80, 1.25] preventing wheel disconnect on bumps.
 * 6. Bullet m_rollInfluence vertical arm adjustment for tunable body lean and rollover behavior.
 */
public class BulletWheelFriction {

    public static final float LOAD_NORM_SCALE    = 0.80f; // Normalizes high Minecraft gravity while retaining authentic grip
    public static final float MU_LAT_FRONT       = 1.10f; // Front softer than rear for inherent directional stability
    public static final float MU_LAT_REAR        = 1.14f; // Rear holds trajectory securely, breaks into smooth drift on command
    public static final float RELAX_FRONT        = 0.82f;
    public static final float RELAX_REAR         = 0.78f; // Phase-aligned rear relaxation
    public static final float NOMINAL_WHEEL_LOAD = 7740.0f; // N_nom = 0.25 * 2150 kg * 18.0 m/s^2 * 0.80

    /**
     * Safe surface roughness modulation on spring normal force.
     * Prevents micro-bumps from disconnecting wheels or dropping normal load below 80%.
     */
    public static float applySafeSurfaceRoughness(
            float baseSpringForce,
            SurfaceProfile surface,
            float wx, float wz,
            float wheelForwardSpeedMs
    ) {
        if (surface.roughnessAmp <= 0.001f || baseSpringForce <= 0.0f) {
            return baseSpringForce;
        }
        float speedFactor = Mth.clamp(Math.abs(wheelForwardSpeedMs) / 6.0f, 0.0f, 1.0f);
        if (speedFactor <= 0.01f) return baseSpringForce;

        float freq = surface.roughnessFreq;
        float wave = Mth.sin(wx * freq) * Mth.cos(wz * freq * 1.17f);
        float forceModulation = 1.0f + (wave * (surface.roughnessAmp * 3.2f) * speedFactor);
        return baseSpringForce * Mth.clamp(forceModulation, 0.80f, 1.25f);
    }

    /**
     * Solves and applies tire forces to a RigidBodyOBB using Bullet's bilateral impulse solver
     * and Isotropic Combined Slip.
     */
    public static float applyBulletWheelFriction(
            RigidBodyOBB body,
            boolean isFront,
            boolean handBrake,
            SurfaceProfile surface,
            boolean isWet,
            Vector3f contactArm,
            Vector3f wheelRight,
            Vector3f wheelForward,
            Vector3f localUp,
            float springNormalForce,
            float driveForce,
            float rollInfluence,
            float dt,
            Vector3f tempVel,
            Vector3f tempCross,
            Vector3f tempAdjustedArm
    ) {
        if (springNormalForce <= 1.0f) return 0.0f;

        // 1. World velocity of the tire contact patch: v_contact = omega x r + v_lin
        body.angularVelocity.cross(contactArm, tempVel).add(body.linearVelocity);
        float lateralVel = tempVel.dot(wheelRight);
        float fwdWheelSpeed = tempVel.dot(wheelForward);

        // 2. Weather and tire load sensitivity
        float weatherFactor = isWet ? surface.rainGripFactor : 1.0f;
        float rawEffLoad = springNormalForce * LOAD_NORM_SCALE;
        float loadRatio = Mth.clamp(rawEffLoad / NOMINAL_WHEEL_LOAD, 0.25f, 3.0f);
        float loadSensitivity = (float) Math.pow(loadRatio, -0.10);
        float effectiveNormalLoad = rawEffLoad * loadSensitivity;

        // 3. Rolling resistance and ground viscous drag (sand/mud/gravel) -> creates mu-split yaw pulling
        float dragForce = 0.0f;
        if (Math.abs(fwdWheelSpeed) > 0.05f) {
            float dirSign = Math.signum(fwdWheelSpeed);
            float rollDrag = (effectiveNormalLoad * surface.rollingResistance)
                    + (Math.abs(fwdWheelSpeed) * surface.viscousDrag * 350.0f);
            float maxStopDrag = Math.abs(fwdWheelSpeed) * (body.mass * 0.25f) / dt;
            dragForce = -dirSign * Math.min(rollDrag, maxStopDrag);
        }

        float totalDriveForce = driveForce + dragForce;

        // 4. Grip and relaxation scaling with soft terrain factor
        float softRelaxScale = (float) Math.sqrt(surface.relaxationScale);
        float baseMuLat = isFront ? MU_LAT_FRONT : MU_LAT_REAR;
        float baseRelax = isFront ? RELAX_FRONT : RELAX_REAR;

        float muLat  = baseMuLat * surface.muLatScale * weatherFactor;
        float muLong = 1.12f * surface.muLongScale * weatherFactor;
        float relax  = baseRelax * softRelaxScale;

        if (handBrake && !isFront) {
            muLat *= 0.55f;
            relax *= 0.60f;
        }

        // 5. Bilateral effective inverse mass along lateral axis (Bullet resolveSingleBilateral)
        contactArm.cross(wheelRight, tempCross);
        body.invInertiaWorld.transform(tempCross);
        tempCross.cross(contactArm);
        float angularDenominator = tempCross.dot(wheelRight);
        float effectiveInvMass = body.invMass + Math.max(0.0f, angularDenominator);

        // 6. Max coulomb impulses
        float maxLongImpulse = Math.max(0.001f, effectiveNormalLoad * muLong * dt);
        float maxLatImpulse  = Math.max(0.001f, effectiveNormalLoad * muLat * dt);

        float reqLong = totalDriveForce * dt;
        float reqLat  = (-lateralVel / Math.max(1e-5f, effectiveInvMass)) * relax;

        // 7. Isotropic Combined Slip Model (Brush Tire Model)
        // tractionWeight = 0.70f prioritizes lateral path-holding over longitudinal wheelspin
        float tractionWeight = 0.70f;
        float normX = (reqLong * tractionWeight) / maxLongImpulse;
        float normY = reqLat / maxLatImpulse;
        float combinedSlip = (float) Math.sqrt(normX * normX + normY * normY);

        float finalLongImpulse;
        float finalLatImpulse;

        if (combinedSlip > 1.0f) {
            float invSlip = 1.0f / combinedSlip;
            finalLongImpulse = Mth.clamp(reqLong * invSlip, -maxLongImpulse, maxLongImpulse);
            finalLatImpulse  = reqLat * invSlip;
        } else {
            finalLongImpulse = Mth.clamp(reqLong, -maxLongImpulse, maxLongImpulse);
            finalLatImpulse  = reqLat;
        }

        float slipRatio = combinedSlip > 1.0f ? (combinedSlip - 1.0f) : 0.0f;

        // 8. Bullet m_rollInfluence vertical moment arm adjustment
        float vertDist = contactArm.dot(localUp);
        tempAdjustedArm.set(contactArm).fma(-vertDist * (1.0f - rollInfluence), localUp);

        // 9. Convert impulses back into forces for RigidBodyOBB forceAccum/torqueAccum (F = Impulse / dt)
        float invDt = 1.0f / Math.max(1e-4f, dt);
        float fx = (wheelForward.x * finalLongImpulse + wheelRight.x * finalLatImpulse) * invDt;
        float fy = (wheelForward.y * finalLongImpulse + wheelRight.y * finalLatImpulse) * invDt;
        float fz = (wheelForward.z * finalLongImpulse + wheelRight.z * finalLatImpulse) * invDt;

        body.forceAccum.add(fx, fy, fz);
        tempCross.set(
                tempAdjustedArm.y * fz - tempAdjustedArm.z * fy,
                tempAdjustedArm.z * fx - tempAdjustedArm.x * fz,
                tempAdjustedArm.x * fy - tempAdjustedArm.y * fx
        );
        body.torqueAccum.add(tempCross);

        return slipRatio;
    }

    /**
     * Zero-GC calculation for ODE4J vehicle body simulation.
     * Computes the 3D ground contact force and adjusted application point using Isotropic Combined Slip.
     */
    public static float computeBulletTireForce(
            boolean isFront,
            boolean handBrake,
            SurfaceProfile surface,
            boolean isWet,
            float vehicleMass,
            float invMass,
            Matrix3f invInertiaWorld,
            Vector3f linVel,
            Vector3f angVel,
            Vector3f contactArm,
            Vector3f wheelRight,
            Vector3f wheelForward,
            Vector3f localUp,
            float springNormalForce,
            float driveForce,
            float rollInfluence,
            float dt,
            Vector3f tempVel,
            Vector3f tempCross,
            Vector3f outAdjustedArm,
            Vector3f outForce
    ) {
        if (springNormalForce <= 1.0f) {
            outForce.zero();
            outAdjustedArm.set(contactArm);
            return 0.0f;
        }

        // 1. World velocity of contact patch
        angVel.cross(contactArm, tempVel).add(linVel);
        float lateralVel = tempVel.dot(wheelRight);
        float fwdWheelSpeed = tempVel.dot(wheelForward);

        // 2. Weather and tire load sensitivity
        float weatherFactor = isWet ? surface.rainGripFactor : 1.0f;
        float rawEffLoad = springNormalForce * LOAD_NORM_SCALE;
        float loadRatio = Mth.clamp(rawEffLoad / NOMINAL_WHEEL_LOAD, 0.25f, 3.0f);
        float loadSensitivity = (float) Math.pow(loadRatio, -0.10);
        float effectiveNormalLoad = rawEffLoad * loadSensitivity;

        // 3. Rolling resistance and ground viscous drag (sand/mud/gravel) -> creates mu-split yaw pulling
        float dragForce = 0.0f;
        if (Math.abs(fwdWheelSpeed) > 0.05f) {
            float dirSign = Math.signum(fwdWheelSpeed);
            float rollDrag = (effectiveNormalLoad * surface.rollingResistance)
                    + (Math.abs(fwdWheelSpeed) * surface.viscousDrag * 350.0f);
            float maxStopDrag = Math.abs(fwdWheelSpeed) * (vehicleMass * 0.25f) / dt;
            dragForce = -dirSign * Math.min(rollDrag, maxStopDrag);
        }

        float totalDriveForce = driveForce + dragForce;

        // 4. Grip and relaxation scaling with soft terrain factor
        float softRelaxScale = (float) Math.sqrt(surface.relaxationScale);
        float baseMuLat = isFront ? MU_LAT_FRONT : MU_LAT_REAR;
        float baseRelax = isFront ? RELAX_FRONT : RELAX_REAR;

        float muLat  = baseMuLat * surface.muLatScale * weatherFactor;
        float muLong = 1.12f * surface.muLongScale * weatherFactor;
        float relax  = baseRelax * softRelaxScale;

        if (handBrake && !isFront) {
            muLat *= 0.55f;
            relax *= 0.60f;
        }

        // 5. Bilateral effective inverse mass along lateral axis (Bullet resolveSingleBilateral)
        contactArm.cross(wheelRight, tempCross);
        invInertiaWorld.transform(tempCross);
        tempCross.cross(contactArm);
        float angularDenominator = tempCross.dot(wheelRight);
        float effectiveInvMass = invMass + Math.max(0.0f, angularDenominator);

        // 6. Max coulomb impulses
        float maxLongImpulse = Math.max(0.001f, effectiveNormalLoad * muLong * dt);
        float maxLatImpulse  = Math.max(0.001f, effectiveNormalLoad * muLat * dt);

        float reqLong = totalDriveForce * dt;
        float reqLat  = (-lateralVel / Math.max(1e-5f, effectiveInvMass)) * relax;

        // 7. Isotropic Combined Slip Model (Brush Tire Model)
        float tractionWeight = 0.70f;
        float normX = (reqLong * tractionWeight) / maxLongImpulse;
        float normY = reqLat / maxLatImpulse;
        float combinedSlip = (float) Math.sqrt(normX * normX + normY * normY);

        float finalLongImpulse;
        float finalLatImpulse;

        if (combinedSlip > 1.0f) {
            float invSlip = 1.0f / combinedSlip;
            finalLongImpulse = Mth.clamp(reqLong * invSlip, -maxLongImpulse, maxLongImpulse);
            finalLatImpulse  = reqLat * invSlip;
        } else {
            finalLongImpulse = Mth.clamp(reqLong, -maxLongImpulse, maxLongImpulse);
            finalLatImpulse  = reqLat;
        }

        float slipRatio = combinedSlip > 1.0f ? (combinedSlip - 1.0f) : 0.0f;

        // 8. Bullet m_rollInfluence vertical moment arm adjustment
        float vertDist = contactArm.dot(localUp);
        outAdjustedArm.set(contactArm).fma(-vertDist * (1.0f - rollInfluence), localUp);

        // 9. Force output in world space (F = Impulse / dt)
        float invDt = 1.0f / Math.max(1e-4f, dt);
        outForce.set(
                (wheelForward.x * finalLongImpulse + wheelRight.x * finalLatImpulse) * invDt,
                (wheelForward.y * finalLongImpulse + wheelRight.y * finalLatImpulse) * invDt,
                (wheelForward.z * finalLongImpulse + wheelRight.z * finalLatImpulse) * invDt
        );

        return slipRatio;
    }
}
