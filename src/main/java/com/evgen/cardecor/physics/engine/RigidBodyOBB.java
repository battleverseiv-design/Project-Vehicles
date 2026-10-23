package com.evgen.cardecor.physics.engine;

import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.physics.dynamics.BulletWheelFriction;
import com.evgen.cardecor.physics.dynamics.MarcoMonsterAero;
import com.evgen.cardecor.physics.dynamics.PowertrainSpec;
import com.evgen.cardecor.physics.dynamics.PowertrainState;
import com.evgen.cardecor.physics.dynamics.SurfaceProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 6-DOF Zero-GC Rigid Body with Hooke's Law Suspension, Continuous Voxel Raycasting,
 * and Center of Mass (COM) Dynamics.
 *
 * Integrated strictly with:
 * 1. Jolt Physics Powertrain (flywheel inertia, clutch slip, RPM-driven shifting).
 * 2. Bullet bilateral tire friction (resolveSingleBilateral, physical m_rollInfluence).
 * 3. Marco Monster Aerodynamics and rolling resistance.
 */
public class RigidBodyOBB {
    public static final int SUB_STEPS = 4;            // 80 Hz
    public static final float DT = 0.05f / SUB_STEPS; // 0.0125 sec
    public static final float GRAVITY = -18.5f;

    public float mass = 1650.0f;
    public float invMass = 1.0f / 1650.0f;

    // Powertrain and transmission
    public final PowertrainSpec powertrainSpec = PowertrainSpec.createFamilySedan();
    public final PowertrainState powertrain = new PowertrainState(powertrainSpec);
    public String drivetrain = "rwd";

    // Center of Mass (COM) pivot in model space:
    public final Vector3f modelComPivot = new Vector3f(0.0f, 0.65f, -0.401f);
    public float comHeightFromGround = 0.65f;

    // 6-DOF dynamic state
    public final Vector3f position = new Vector3f();
    public final Quaternionf rotation = new Quaternionf();
    public final Vector3f linearVelocity = new Vector3f();
    public final Vector3f angularVelocity = new Vector3f();

    public final Vector3f prevPosition = new Vector3f();
    public final Quaternionf prevRotation = new Quaternionf();

    public final Matrix3f rotMatrix = new Matrix3f();
    public final Vector3f invInertiaLocal = new Vector3f();
    public final Matrix3f invInertiaWorld = new Matrix3f();

    public final Vector3f forceAccum = new Vector3f();
    public final Vector3f torqueAccum = new Vector3f();

    // Inputs for physics world dispatcher
    public float throttleInput = 0.0f;
    public float steerInput = 0.0f;
    public float currentSteerRad = 0.0f;
    public boolean handBrakeInput = false;

    // Suspension mount points relative to Center of Mass (COM = 0, 0, 0)
    public final Vector3f[] wheelOffsetsCOM = new Vector3f[]{
            new Vector3f(-0.973f, 0.02f, -1.656f), // FL (-X is Left, -Z is Front)
            new Vector3f( 0.987f, 0.02f, -1.656f), // FR (+X is Right, -Z is Front)
            new Vector3f(-0.974f, 0.02f,  1.656f), // RL (-X is Left, +Z is Rear)
            new Vector3f( 0.986f, 0.02f,  1.656f)  // RR (+X is Right, +Z is Rear)
    };
    public final float[] wheelSuspensionLength = new float[]{0.45f, 0.45f, 0.45f, 0.45f};
    public final float[] wheelAngularVel = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    public final float[] wheelSlipRatio = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    private final BlockState[] wheelGroundBlock = new BlockState[4];

    public float wheelRadius = 0.38f;
    public float suspensionRestLength = 0.45f;
    public float springStiffness = 46000.0f;
    public float springDamping = 4000.0f;

    // 8 OBB hull collision corners relative to COM
    public final Vector3f[] hullCornersCOM = new Vector3f[8];

    // Sleeping system
    public boolean isSleeping = false;
    public int sleepTimer = 0;
    public int groundedWheelsCount = 0;
    public boolean isGrounded = false;
    public float maxDrivenWheelSlip = 0.0f;
    public float avgDrivenWheelAngularVel = 0.0f;

    // Zero-GC preallocated registers
    private final Vector3f localRight = new Vector3f();
    private final Vector3f localUp = new Vector3f();
    private final Vector3f localForward = new Vector3f();
    private final Vector3f wheelFwd = new Vector3f();
    private final Vector3f wheelRight = new Vector3f();
    private final Vector3f tempMount = new Vector3f();
    private final Vector3f tempContactArm = new Vector3f();
    private final Vector3f pointVel = new Vector3f();
    private final Vector3f tempCross = new Vector3f();
    private final Vector3f tempVec = new Vector3f();
    private final Vector3f tempAdjustedArm = new Vector3f();
    private final Matrix3f tempMat = new Matrix3f();
    private final Quaternionf deltaQuat = new Quaternionf();
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public RigidBodyOBB() {
        initDefaultGeometry();
    }

    public RigidBodyOBB(float mass, float halfW, float halfH, float halfL) {
        this.mass = mass;
        initDefaultGeometry();
    }

    public void initDefaultGeometry() {
        this.invMass = 1.0f / Math.max(100.0f, this.mass);

        float w = 2.0f, h = 1.2f, l = 4.4f;
        float ix = (1f / 12f) * mass * (h * h + l * l);
        float iy = (1f / 12f) * mass * (w * w + l * l);
        float iz = (1f / 12f) * mass * (w * w + h * h);
        invInertiaLocal.set(1.0f / ix, 1.0f / iy, 1.0f / iz);

        float halfW = 0.98f, minY = -0.28f, maxY = 0.85f, halfL = 2.15f;
        int idx = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = 0; sy <= 1; sy++) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    hullCornersCOM[idx++] = new Vector3f(
                            sx * halfW,
                            (sy == 0) ? minY : maxY,
                            sz * halfL
                    );
                }
            }
        }
    }

    public void recomputeInertia() {
        this.invMass = 1.0f / Math.max(100.0f, this.mass);
        float w = 2.0f, h = 1.2f, l = 4.4f;
        float ix = (1f / 12f) * mass * (h * h + l * l);
        float iy = (1f / 12f) * mass * (w * w + l * l);
        float iz = (1f / 12f) * mass * (w * w + h * h);
        invInertiaLocal.set(1.0f / ix, 1.0f / iy, 1.0f / iz);
    }

    public void updateDynamicMass(int passengers, float cargoKg) {
        float newMass = MarcoMonsterAero.calculateTotalMass(powertrainSpec.baseMassKg, passengers, cargoKg);
        if (Math.abs(newMass - this.mass) > 1.0f) {
            this.mass = newMass;
            recomputeInertia();
        }
    }

    /**
     * Reconfigures body geometry and wheel mount points dynamically from pack definition.
     */
    public void configureFromDefinition(VehicleDefinition def) {
        if (def == null) return;
        if (def.physics != null && def.physics.mass > 100.0) {
            this.powertrainSpec.baseMassKg = (float) def.physics.mass;
            this.mass = this.powertrainSpec.baseMassKg;
            this.invMass = 1.0f / this.mass;
        }

        if (def.wheels != null) {
            float[] fl = def.wheels.frontLeft;
            float[] fr = def.wheels.frontRight;
            float[] rl = def.wheels.rearLeft;
            float[] rr = def.wheels.rearRight;

            if (fl != null && rl != null) {
                float midZ = (fl[2] + rl[2]) * 0.5f;
                float midY = Math.max(0.40f, (fl[1] + rl[1]) * 0.5f + 0.20f);
                this.modelComPivot.set(0.0f, midY, midZ);
                this.comHeightFromGround = midY;

                float mountY = 0.02f;
                wheelOffsetsCOM[0].set(fl[0] - modelComPivot.x, mountY, fl[2] - modelComPivot.z);
                wheelOffsetsCOM[1].set(fr[0] - modelComPivot.x, mountY, fr[2] - modelComPivot.z);
                wheelOffsetsCOM[2].set(rl[0] - modelComPivot.x, mountY, rl[2] - modelComPivot.z);
                wheelOffsetsCOM[3].set(rr[0] - modelComPivot.x, mountY, rr[2] - modelComPivot.z);
            }

            if (def.wheels.radius > 0.1f) {
                this.wheelRadius = (float) def.wheels.radius;
                this.powertrainSpec.wheelRadius = this.wheelRadius;
            }
        }

        if (def.engine != null) {
            VehicleDefinition.EngineConfig ec = def.engine;
            if (ec.idleRpm > 400.0) this.powertrainSpec.minRpm = (float) ec.idleRpm;
            if (ec.redlineRpm > 2000.0) {
                this.powertrainSpec.maxRpm = (float) ec.redlineRpm;
                this.powertrainSpec.revLimiterRpm = (float) (ec.redlineRpm + 250.0);
            }
            if (ec.torqueNm > 50.0) this.powertrainSpec.maxTorqueNm = (float) ec.torqueNm;
        }

        if (def.physics != null) {
            this.springStiffness = (float) (def.physics.suspensionStiffness * 44000.0);
            this.springDamping = (float) (def.physics.suspensionDamping * 4200.0);
            if (def.physics.drivetrain != null && !def.physics.drivetrain.isEmpty()) {
                this.drivetrain = def.physics.drivetrain.toLowerCase(java.util.Locale.ROOT);
            }
            if (def.physics.horsepower > 10 && this.powertrainSpec.maxTorqueNm < 60.0f) {
                this.powertrainSpec.maxTorqueNm = Math.max(180.0f, ((float) def.physics.horsepower * 7127.0f) / this.powertrainSpec.peakTorqueRpm);
            }
        }

        recomputeInertia();
    }

    /**
     * Executes 1 sub-step of 6-DOF simulation (80 Hz, DT = 0.0125f).
     */
    public void stepSubTick(Level level, float throttle, float steer, boolean handBrake) {
        // 1. Orientation matrix and world inverse inertia tensor: I_world^-1 = R * I_local^-1 * R^T
        rotation.get(rotMatrix);
        tempMat.set(rotMatrix).scale(invInertiaLocal.x, invInertiaLocal.y, invInertiaLocal.z);
        rotMatrix.transpose(invInertiaWorld);
        tempMat.mul(invInertiaWorld, invInertiaWorld);

        // 2. Extract model axes:
        // Col 0 is +X (Right), Col 1 is +Y (Up), Col 2 is +Z (Back)
        // Therefore:
        // Forward (-Z) = -Col 2
        // Up (+Y)      = +Col 1
        // Right (+X)   = +Col 0
        localForward.set(-rotMatrix.m20(), -rotMatrix.m21(), -rotMatrix.m22());
        localUp.set(rotMatrix.m10(), rotMatrix.m11(), rotMatrix.m12());
        localRight.set(rotMatrix.m00(), rotMatrix.m01(), rotMatrix.m02());

        // Gravity acts directly at Center of Mass
        forceAccum.set(0f, mass * GRAVITY, 0f);
        torqueAccum.zero();

        // 3. Marco Monster Aerodynamics and Rolling Resistance
        MarcoMonsterAero.applyAeroAndRollingResistance(
                this,
                localUp,
                localForward,
                powertrainSpec.frontalAreaM2,
                powertrainSpec.dragCd,
                powertrainSpec.downforceCl,
                powertrainSpec.groundRollResist
        );

        // 4. Driven Wheel Angular Velocity & Maximum Slip
        float sumDrivenOmega = 0.0f;
        int drivenCount = 0;
        float maxSlip = 0.0f;
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);
            if (isDriven) {
                sumDrivenOmega += wheelAngularVel[i];
                drivenCount++;
                maxSlip = Math.max(maxSlip, wheelSlipRatio[i]);
            }
        }
        this.avgDrivenWheelAngularVel = drivenCount > 0 ? (sumDrivenOmega / drivenCount) : 0.0f;
        this.maxDrivenWheelSlip = maxSlip;

        // 5. Jolt Powertrain Simulation (Purely RPM-driven)
        float fwdSpeed = linearVelocity.dot(localForward);
        float totalWheelTorqueNm = powertrain.updatePowertrain(
                DT,
                throttle,
                fwdSpeed,
                avgDrivenWheelAngularVel,
                groundedWheelsCount > 0,
                maxDrivenWheelSlip
        );

        float driveTorquePerWheel = drivenCount > 0 ? (totalWheelTorqueNm / drivenCount) : 0.0f;
        float baseDriveForce = driveTorquePerWheel / Math.max(0.1f, wheelRadius);

        // Argon's smooth speed-sensitive curve:
        // 0 km/h -> 38°, 60 km/h -> 26°, 100 km/h -> 20°, 140+ km/h -> 18°
        float speedKmh = Math.abs(fwdSpeed) * 3.6f;
        float t = Mth.clamp(speedKmh / 120.0f, 0.0f, 1.0f);
        float maxAngleDeg = Mth.lerp(t, 38.0f, 18.0f);

        if (maxDrivenWheelSlip > 0.25f || Math.abs(wheelSlipRatio[2]) > 0.30f || Math.abs(wheelSlipRatio[3]) > 0.30f) {
            maxAngleDeg = 38.0f;
        }

        float targetSteerRad = steer * (maxAngleDeg * Mth.DEG_TO_RAD);
        boolean isReturningOrCountering = (Math.abs(steer) < 0.01f)
                || (Math.signum(targetSteerRad) != Math.signum(this.currentSteerRad));

        float steerSpeedRadPerSec = isReturningOrCountering
                ? (float) Math.toRadians(280.0)
                : (float) Math.toRadians(145.0);

        float maxDelta = steerSpeedRadPerSec * DT;
        this.currentSteerRad = Mth.clamp(targetSteerRad, this.currentSteerRad - maxDelta, this.currentSteerRad + maxDelta);
        float steerRad = this.currentSteerRad;
        groundedWheelsCount = 0;

        // 6. 4-WHEEL SUSPENSION & BULLET BILATERAL FRICTION
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);

            rotMatrix.transform(wheelOffsetsCOM[i], tempMount);
            float wx = position.x + tempMount.x;
            float wy = position.y + tempMount.y;
            float wz = position.z + tempMount.z;

            // Correct orthogonal wheel orientation
            if (isFront && Math.abs(steerRad) > 0.001f) {
                float cos = Mth.cos(steerRad), sin = Mth.sin(steerRad);
                wheelFwd.set(localForward.x * cos + localRight.x * sin, localForward.y * cos + localRight.y * sin, localForward.z * cos + localRight.z * sin);
                wheelRight.set(localRight.x * cos - localForward.x * sin, localRight.y * cos - localForward.y * sin, localRight.z * cos - localForward.z * sin);
            } else {
                wheelFwd.set(localForward);
                wheelRight.set(localRight);
            }

            float maxRay = suspensionRestLength + wheelRadius;
            float hitDist = castRayAlongSuspension(level, wx, wy, wz, -localUp.x, -localUp.y, -localUp.z, maxRay, i);

            if (hitDist < maxRay) {
                groundedWheelsCount++;
                SurfaceProfile surface = (wheelGroundBlock[i] != null) ? SurfaceProfile.get(wheelGroundBlock[i]) : SurfaceProfile.ASPHALT;
                boolean isWet = level.isRainingAt(mutablePos);

                float curSuspLen = Math.max(0.04f, hitDist - wheelRadius);
                wheelSuspensionLength[i] = curSuspLen;
                float compression = suspensionRestLength - curSuspLen;

                // Contact arm relative to Center of Mass
                tempContactArm.set(tempMount).fma(-hitDist, localUp);

                // Suspension velocity at mount
                angularVelocity.cross(tempMount, pointVel).add(linearVelocity);
                float suspVel = pointVel.dot(localUp);

                float springForce = Math.max(0f, (springStiffness * compression) - (springDamping * suspVel));
                addForceAtArm(localUp.x * springForce, localUp.y * springForce, localUp.z * springForce, tempMount);

                // Drive / Brake Force
                boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);
                float vPatchFwd = pointVel.dot(wheelFwd);
                float wheelDriveForce = 0.0f;

                if (handBrake && !isFront) {
                    wheelDriveForce = -Math.signum(vPatchFwd) * Math.min(Math.abs(vPatchFwd) * 5000.0f, springForce * 0.85f);
                } else if (powertrain.isBraking) {
                    wheelDriveForce = -Math.signum(vPatchFwd) * Math.min(Math.abs(vPatchFwd) * 4500.0f, springForce * 0.80f);
                } else if (isDriven) {
                    wheelDriveForce = baseDriveForce;
                } else {
                    wheelDriveForce = -Math.signum(vPatchFwd) * Math.min(Math.abs(vPatchFwd) * 300.0f, 250.0f);
                }

                // Bullet m_rollInfluence vertical moment arm adjustment
                float currentRollInfluence = powertrainSpec.rollInfluence;

                // Bullet Bilateral Friction (resolveSingleBilateral + Kamm circle + m_rollInfluence)
                float slipRatio = BulletWheelFriction.applyBulletWheelFriction(
                        this,
                        isFront,
                        handBrake,
                        surface,
                        isWet,
                        tempContactArm,
                        wheelRight,
                        wheelFwd,
                        localUp,
                        springForce,
                        wheelDriveForce,
                        currentRollInfluence,
                        DT,
                        pointVel,
                        tempCross,
                        tempAdjustedArm
                );

                wheelSlipRatio[i] = slipRatio;

                if (slipRatio < 0.20f) {
                    wheelAngularVel[i] = vPatchFwd / Math.max(0.05f, wheelRadius);
                } else {
                    float wheelInertia = 1.6f;
                    float netTorque = (isDriven ? driveTorquePerWheel : 0.0f) - (wheelDriveForce * wheelRadius);
                    wheelAngularVel[i] += (netTorque / wheelInertia) * DT;
                }
            } else {
                wheelSuspensionLength[i] = suspensionRestLength;
                wheelSlipRatio[i] = 0.0f;
            }
        }
        isGrounded = (groundedWheelsCount > 0);

        // 7. SYMPLECTIC EULER INTEGRATION
        linearVelocity.fma(DT * invMass, forceAccum);

        invInertiaWorld.transform(torqueAccum, tempVec);
        angularVelocity.fma(DT, tempVec);

        linearVelocity.mul(0.999f);
        angularVelocity.mul(groundedWheelsCount > 0 ? 0.992f : 0.998f);

        position.fma(DT, linearVelocity);

        float wx = angularVelocity.x, wy = angularVelocity.y, wz = angularVelocity.z;
        float wMag = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (wMag > 0.00001f) {
            deltaQuat.setAngleAxis(wMag * DT, wx / wMag, wy / wMag, wz / wMag);
            deltaQuat.mul(rotation, rotation);
            rotation.normalize();
        }

        // 8. ANTI-SLEEP PROTECTION
        boolean hasUnbalancedForces = torqueAccum.lengthSquared() > 50.0f || forceAccum.lengthSquared() > 100.0f;
        if (groundedWheelsCount < 3 || hasUnbalancedForces) {
            wakeUp();
            return;
        }

        if (linearVelocity.lengthSquared() < 0.002f && angularVelocity.lengthSquared() < 0.002f) {
            if (++sleepTimer > 10) {
                isSleeping = true;
                linearVelocity.zero();
                angularVelocity.zero();
            }
        } else {
            sleepTimer = 0;
        }
    }

    private void addForceAtArm(float fx, float fy, float fz, Vector3f arm) {
        forceAccum.add(fx, fy, fz);
        tempCross.set(
                arm.y * fz - arm.z * fy,
                arm.z * fx - arm.x * fz,
                arm.x * fy - arm.y * fx
        );
        torqueAccum.add(tempCross);
    }

    private float castRayAlongSuspension(Level level, float ox, float oy, float oz, float dx, float dy, float dz, float maxDist, int wheelIdx) {
        int samples = 8;
        float step = maxDist / samples;
        for (int s = 1; s <= samples; s++) {
            float d = s * step;
            float px = ox + dx * d, py = oy + dy * d, pz = oz + dz * d;
            int bx = Mth.floor(px), by = Mth.floor(py), bz = Mth.floor(pz);

            mutablePos.set(bx, by, bz);
            BlockState state = level.getBlockState(mutablePos);
            if (!state.isAir()) {
                VoxelShape shape = state.getCollisionShape(level, mutablePos);
                if (!shape.isEmpty()) {
                    double minX = bx + shape.min(Direction.Axis.X), maxX = bx + shape.max(Direction.Axis.X);
                    double minZ = bz + shape.min(Direction.Axis.Z), maxZ = bz + shape.max(Direction.Axis.Z);
                    double maxY = by + shape.max(Direction.Axis.Y);

                    if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ && py <= maxY && oy >= maxY - 0.25) {
                        wheelGroundBlock[wheelIdx] = state;
                        return Math.max(0.01f, (float) ((oy - maxY) / Math.max(0.2f, -dy)));
                    }
                }
            }
        }
        wheelGroundBlock[wheelIdx] = null;
        return maxDist;
    }

    public void wakeUp() {
        this.isSleeping = false;
        this.sleepTimer = 0;
    }

    public float extractMinecraftYawDegrees() {
        float fwdX = -2.0f * (rotation.x * rotation.z + rotation.w * rotation.y);
        float fwdZ = -(1.0f - 2.0f * (rotation.x * rotation.x + rotation.y * rotation.y));
        return (float) Math.atan2(-fwdX, fwdZ) * Mth.RAD_TO_DEG;
    }

    public float extractMinecraftPitchDegrees() {
        float fwdY = -2.0f * (rotation.y * rotation.z - rotation.w * rotation.x);
        return (float) -Math.asin(Mth.clamp(fwdY, -1.0f, 1.0f)) * Mth.RAD_TO_DEG;
    }

    public float extractMinecraftRollDegrees() {
        rotation.get(rotMatrix);
        rotMatrix.getColumn(0, localRight);
        rotMatrix.getColumn(1, localUp);
        return (float) Math.atan2(-localRight.y, localUp.y) * Mth.RAD_TO_DEG;
    }
}
