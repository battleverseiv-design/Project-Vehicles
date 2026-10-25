package com.evgen.cardecor.physics.ode;

import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.physics.dynamics.BrushTireSolver;
import com.evgen.cardecor.physics.dynamics.JoltStylePowertrain;
import com.evgen.cardecor.physics.dynamics.BulletWheelFriction;
import com.evgen.cardecor.physics.dynamics.MarcoMonsterAero;
import com.evgen.cardecor.physics.dynamics.PowertrainSpec;
import com.evgen.cardecor.physics.dynamics.PowertrainSolver;
import com.evgen.cardecor.physics.dynamics.PowertrainState;
import com.evgen.cardecor.physics.dynamics.SurfaceProfile;
import com.evgen.cardecor.physics.dynamics.SteeringRack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.evgen.cardecor.physics.dynamics.TerrainTraversalSpec;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

/**
 * 6-DOF Vehicle Rigid Body powered by ODE4J (Open Dynamics Engine in Pure Java).
 *
 * Integrated strictly with:
 * 1. Jolt Physics Powertrain & Transmission (flywheel inertia, clutch slip, RPM-driven shifting).
 * 2. Bullet Physics bilateral tire friction (resolveSingleBilateral, exact stop impulse, physical m_rollInfluence).
 * 3. Marco Monster / BeamNG Lite quadratic aero drag, downforce, and rolling resistance.
 * 4. Dynamic mass and inertia tensor updating from passengers and trunk cargo.
 */
public class OdeVehicleBody {

    public enum GeomType {
        MAIN_HULL,
        SKIDPLATE,
        CABIN_ROOF,
        BLOCK
    }

    public net.minecraft.world.entity.Entity entityOwner;
    public DBody body;
    public DBox chassisBox;
    public DBox mainHullBox;
    public DBox skidplateBox;
    public DBox cabinRoofBox;
    public float baseDriveForce = 0.0f;
    public boolean impactCutoffActive = false;

    // Vehicle physical dimensions
    public float mass = 1650.0f;
    public float width = 1.95f;
    public float height = 1.25f;
    public float length = 4.30f;
    public float comHeight = 0.65f;

    // Powertrain and transmission
    public final PowertrainSpec powertrainSpec = PowertrainSpec.createFamilySedan();
    public final PowertrainSolver powertrain = new PowertrainSolver(powertrainSpec);
    public final SteeringRack steeringRack = new SteeringRack();
    public String drivetrain = "rwd";

    // 4 Wheel mount points relative to Center of Mass (COM)
    // Model convention: -Z is Front, +Z is Rear, -X is Left, +X is Right, +Y is Up
    public final Vector3f[] wheelMountsLocal = new Vector3f[]{
            new Vector3f(-0.95f, -0.22f, -1.65f), // Front Left (FL)
            new Vector3f( 0.95f, -0.22f, -1.65f), // Front Right (FR)
            new Vector3f(-0.95f, -0.22f,  1.65f), // Rear Left (RL)
            new Vector3f( 0.95f, -0.22f,  1.65f)  // Rear Right (RR)
    };

    public float wheelRadius = 0.38f;
    public float suspensionRestLength = 0.38f;
    public float springStiffness = 45000.0f;
    public float springDamping = 4000.0f;
    public double spawnBaseY = 0.0;

    // Multi-ray probe and parametric suspension specification
    public static final double[] PROBE_THETA = new double[]{
            0.0,
            Math.toRadians(28.0),
            Math.toRadians(-28.0),
            Math.toRadians(55.0),
            Math.toRadians(-55.0)
    };
    public final SuspSpec[] suspSpecs = new SuspSpec[4];
    public final WheelProbe[] wheelProbes = new WheelProbe[4];
    public final float[] wheelCamberRad = new float[4];
    private final double[] tempProbeDistances = new double[5];

    // Client visual smoothing filters (Critically damped 15 Hz followers)
    public final CritDamp[] clientHubFilters = new CritDamp[]{
            new CritDamp(), new CritDamp(), new CritDamp(), new CritDamp()
    };
    public final CritDamp[] clientCamberFilters = new CritDamp[]{
            new CritDamp(), new CritDamp(), new CritDamp(), new CritDamp()
    };

    // Dynamic wheel states
    public final BrushTireSolver.WheelState[] wheelStates = new BrushTireSolver.WheelState[]{
            new BrushTireSolver.WheelState(),
            new BrushTireSolver.WheelState(),
            new BrushTireSolver.WheelState(),
            new BrushTireSolver.WheelState()
    };
    public final float[] wheelSpringForce = new float[4];
    public final float[] wheelNormalLoadFz = new float[4];
    public final float[] wheelCompression = new float[4];
    public final SurfaceProfile[] wheelSurface = new SurfaceProfile[]{
            SurfaceProfile.ASPHALT, SurfaceProfile.ASPHALT, SurfaceProfile.ASPHALT, SurfaceProfile.ASPHALT
    };
    public final boolean[] wheelSurfaceWet = new boolean[4];
    public final double[] wheelHitDist = new double[4];
    public float lastRoadReactionTorque = 0.0f;
    private final float[][] wheelMountWorld = new float[4][3];
    private final Vector3f[] wheelFwdVec = new Vector3f[]{ new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };
    private final Vector3f[] wheelRightVec = new Vector3f[]{ new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };
    public final TerrainTraversalSpec traversal = new TerrainTraversalSpec();
    public final WheelStepUpProbe[] stepUpProbes = new WheelStepUpProbe[]{
            new WheelStepUpProbe(), new WheelStepUpProbe(),
            new WheelStepUpProbe(), new WheelStepUpProbe()
    };
    public float prevFrontLateralForce = 0.0f;
    public final Vector3f[] wheelContactNormal = new Vector3f[]{
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f)
    };
    public float weightDistributionFront = 0.52f;

    public final boolean[] wheelGrounded = new boolean[4];
    public final float[] wheelSuspensionLength = new float[]{0.38f, 0.38f, 0.38f, 0.38f};
    public final float[] wheelAngularVel = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    public final float[] wheelSlipRatio = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    public final float[] tireCondition = new float[]{100.0f, 100.0f, 100.0f, 100.0f};
    public int groundedWheelsCount = 0;
    public float avgDrivenWheelAngularVel = 0.0f;
    public float maxDrivenWheelSlip = 0.0f;

    // Inputs
    public float throttleInput = 0.0f;
    public float steerInput = 0.0f;
    public float currentSteerRad = 0.0f;
    public boolean handbrakeInput = false;

    // Dynamic loading
    public int passengerCount = 0;
    public float dynamicCargoKg = 0.0f;

    // Inertia tensors
    public final Vector3f invInertiaLocal = new Vector3f();
    public final Matrix3f invInertiaWorld = new Matrix3f();
    private final Matrix3f rotMat = new Matrix3f();
    private final Matrix3f tempMat = new Matrix3f();

    // Render Interpolation Registers
    public final Vector3f prevPosition = new Vector3f();
    public final Quaternionf prevRotation = new Quaternionf();
    public final Vector3f currentPosition = new Vector3f();
    public final Quaternionf currentRotation = new Quaternionf();

    // Scratch registers for Zero-GC
    private final DVector3 hubVel = new DVector3();
    private final DVector3 contactVel = new DVector3();
    private final Vector3f wheelFwd = new Vector3f();
    private final Vector3f wheelRight = new Vector3f();
    private final Vector3f tempLinVel = new Vector3f();
    private final Vector3f tempAngVel = new Vector3f();
    public final Vector3f tempLocalUp = new Vector3f();
    public final Vector3f tempLocalFwd = new Vector3f();
    public final Vector3f tempLocalRight = new Vector3f();
    public float frontSurfaceMuScale = 1.0f;
    private final Vector3f tempContactArm = new Vector3f();
    private final Vector3f tempAdjustedArm = new Vector3f();
    private final Vector3f tempBulletForce = new Vector3f();
    private final Vector3f tempAeroForce = new Vector3f();
    private final Vector3f tempVel = new Vector3f();
    private final Vector3f tempCross = new Vector3f();

    public OdeVehicleBody() {
        for (int i = 0; i < 4; i++) {
            wheelProbes[i] = new WheelProbe(this.wheelRadius, PROBE_THETA);
        }
        rebuildSuspensionSpecs();
    }

    public void rebuildSuspensionSpecs() {
        double dt = 0.0125; // 80 Hz
        double lMin = 0.08;
        double droopFrac = 0.25;
        double hDrop = 2.0 * this.suspensionRestLength;
        double frontSharePerWheel = this.weightDistributionFront * 0.5;
        double rearSharePerWheel = (1.0 - this.weightDistributionFront) * 0.5;
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            double share = isFront ? frontSharePerWheel : rearSharePerWheel;
            double zetaC = 0.30;
            double zetaR = 0.50;
            // Масштабируем жесткость пружины пропорционально доле массы на ось:
            // k_axle = k_base * (share / 0.25) => статический прогиб x_static одинаков спереди и сзади!
            double kAxle = this.springStiffness * (share / 0.25);
            suspSpecs[i] = new SuspSpec(
                    this.mass,
                    share,
                    kAxle,
                    zetaC,
                    zetaR,
                    this.suspensionRestLength,
                    lMin,
                    droopFrac,
                    hDrop,
                    dt
            );
            if (wheelProbes[i] == null || Math.abs(wheelProbes[i].r - this.wheelRadius) > 0.01) {
                wheelProbes[i] = new WheelProbe(this.wheelRadius, PROBE_THETA);
            }
        }
    }

    public static double camberOut(double x, double lSa, double gamma0,
                                   double ax, double ay, double az, double nx, double ny, double nz) {
        double dot = Math.max(-1.0, Math.min(1.0, ax * nx + ay * ny + az * nz));
        double g = gamma0 - x / lSa - Math.asin(dot);
        return Math.max(-0.26, Math.min(0.26, g)); // clamp to ±15 deg
    }

    public void init(DWorld world, DSpace space, net.minecraft.world.entity.Entity entity, double startX, double startY, double startZ, float yawDeg) {
        this.entityOwner = entity;
        this.spawnBaseY = startY;
        if (body != null) {
            destroy();
        }

        body = OdeHelper.createBody(world);
        body.setData(this);

        recomputeInertia();
        DMass m = OdeHelper.createMass();
        m.setBoxTotal(this.mass, this.width, this.height, this.length);
        body.setMass(m);

        body.setPosition(startX, startY + comHeight, startZ);

        float yawRad = (float) Math.toRadians(-yawDeg);
        org.ode4j.math.DQuaternion q = new org.ode4j.math.DQuaternion();
        q.set(Math.cos(yawRad * 0.5f), 0, Math.sin(yawRad * 0.5f), 0);
        body.setQuaternion(q);

        initCompoundGeometries(space, body);

        // ODE4J damping: Честная физика без искусственного линейного торможения!
        // Сопротивление воздуха (Marco Monster) и трение качения шин моделируются отдельно.
        body.setLinearDamping(0.0);
        body.setAngularDamping(0.0005);
        body.setLinearDampingThreshold(0.02);
        body.setAngularDampingThreshold(0.01);

        prevPosition.set((float) startX, (float) (startY + comHeight), (float) startZ);
        currentPosition.set(prevPosition);
        currentRotation.identity();
        prevRotation.identity();

        float initialWheelInertia = 0.5f * 22.0f * (this.wheelRadius * this.wheelRadius);
        for (int i = 0; i < 4; i++) {
            this.wheelStates[i].init(this.wheelRadius, initialWheelInertia);
        }
    }

    public void initCompoundGeometries(DSpace space, DBody body) {
        // 1. Нижний подрамник / бамперы (Skidplate)
        // Клиренс подрамника: минимум 0.24м от земли для свободного хода подвески
        double skidHeight = 0.24;
        double skidplateBottom = Math.max(0.24, this.wheelRadius * 0.65);
        double skidOffsetY = (skidplateBottom + skidHeight * 0.5) - this.comHeight;
        skidplateBox = OdeHelper.createBox(space, this.width * 0.86, skidHeight, this.length * 0.96);
        skidplateBox.setBody(body);
        skidplateBox.setOffsetPosition(0.0, skidOffsetY, 0.0);
        skidplateBox.setCategoryBits(1);
        skidplateBox.setCollideBits(2);
        skidplateBox.setData(GeomType.SKIDPLATE);

        // 2. Основной корпус (Капот, поясная линия, багажник)
        // Гарантированный зазор 0.02м (2 см) над подрамником: mainHullBottom = +0.04 + 0.02 = +0.06м
        double hullHeight = this.height * 0.55;
        double hullOffsetY = (skidplateBottom + skidHeight) + (hullHeight * 0.5) - this.comHeight;
        mainHullBox = OdeHelper.createBox(space, this.width * 0.90, hullHeight, this.length * 0.88);
        mainHullBox.setBody(body);
        mainHullBox.setOffsetPosition(0.0, hullOffsetY, 0.0);
        mainHullBox.setCategoryBits(1);
        mainHullBox.setCollideBits(2);
        mainHullBox.setData(GeomType.MAIN_HULL);

        // 3. Кабина / крыша (Greenhouse / Roof)
        double roofHeight = this.height * 0.45;
        double roofOffsetY = hullOffsetY + (hullHeight * 0.5) + (roofHeight * 0.5);
        cabinRoofBox = OdeHelper.createBox(space, this.width * 0.82, roofHeight, this.length * 0.48);
        cabinRoofBox.setBody(body);
        cabinRoofBox.setOffsetPosition(0.0, roofOffsetY, -this.length * 0.08);
        cabinRoofBox.setCategoryBits(1);
        cabinRoofBox.setCollideBits(2);
        cabinRoofBox.setData(GeomType.CABIN_ROOF);

        this.chassisBox = this.mainHullBox; // Для обратной совместимости
    }

    public void updateCompoundGeometries(VehicleDefinition def) {
        if (body == null || skidplateBox == null || mainHullBox == null || cabinRoofBox == null) return;

        double skidHeight = 0.24;
        double skidplateBottom = Math.max(0.24, this.wheelRadius * 0.65); // от уровня земли
        double skidOffsetY = (skidplateBottom + skidHeight * 0.5) - this.comHeight;

        double fullW = this.width;
        double fullL = this.length;
        double fullH = this.height;

        // Skidplate protects the underbody strictly between axles, not extending past the wheels
        double wheelbase = Math.abs(wheelMountsLocal[0].z - wheelMountsLocal[2].z);
        double skidLen = (wheelbase > 1.0) ? (wheelbase * 0.90) : (fullL * 0.65);
        double skidCenterZ = (wheelMountsLocal[0].z + wheelMountsLocal[2].z) * 0.5;

        skidplateBox.setLengths(fullW * 0.82, skidHeight, skidLen);
        skidplateBox.setOffsetPosition(0.0, skidOffsetY, skidCenterZ);

        if (def != null && def.calibratedBounds != null) {
            VehicleDefinition.CalibratedBounds cb = def.calibratedBounds;
            double scale = (def.scale > 0.05) ? def.scale : 1.0;

            double hoodH = (cb.hoodHeight > 0.2 ? cb.hoodHeight : fullH * 0.55) / scale;
            double roofH = (cb.roofHeight > 0.4 ? cb.roofHeight : fullH) / scale;
            double cabinFrontZ = (cb.cabinFrontZ != 0 ? cb.cabinFrontZ : -fullL * 0.35) / scale;
            double cabinRearZ = (cb.cabinRearZ != 0 ? cb.cabinRearZ : fullL * 0.15) / scale;

            if (cabinFrontZ > cabinRearZ) {
                double temp = cabinFrontZ;
                cabinFrontZ = cabinRearZ;
                cabinRearZ = temp;
            }

            // 1. Main Hull (от верха подрамника до капота)
            double hullBottom = skidplateBottom + skidHeight; // ~0.40м от земли
            double hullTop = Math.max(hullBottom + 0.20, hoodH);
            double hullHeight = hullTop - hullBottom;
            double hullOffsetY = (hullBottom + hullTop) * 0.5 - this.comHeight;

            mainHullBox.setLengths(fullW * 0.90, hullHeight, fullL * 0.88);
            mainHullBox.setOffsetPosition(0.0, hullOffsetY, 0.0);

            // 2. Cabin / Roof (от капота до верхней точки крыши)
            if (cb.hasRoof && roofH > hullTop + 0.10) {
                double cabinHeight = roofH - hullTop;
                double cabinOffsetY = (hullTop + roofH) * 0.5 - this.comHeight;
                double cabinLen = Math.max(0.60, Math.abs(cabinRearZ - cabinFrontZ) * 0.92);
                double cabinOffsetZ = (cabinFrontZ + cabinRearZ) * 0.5;

                cabinRoofBox.setLengths(fullW * 0.84, cabinHeight, cabinLen);
                cabinRoofBox.setOffsetPosition(0.0, cabinOffsetY, cabinOffsetZ);
                cabinRoofBox.enable();
            } else {
                cabinRoofBox.setLengths(0.1, 0.1, 0.1);
                cabinRoofBox.disable();
            }
        } else {
            double hullHeight = fullH * 0.55;
            double hullOffsetY = (skidplateBottom + skidHeight) + (hullHeight * 0.5) - this.comHeight;
            mainHullBox.setLengths(fullW * 0.90, hullHeight, fullL * 0.88);
            mainHullBox.setOffsetPosition(0.0, hullOffsetY, 0.0);

            double roofHeight = fullH * 0.45;
            double roofOffsetY = hullOffsetY + (hullHeight * 0.5) + (roofHeight * 0.5);
            cabinRoofBox.setLengths(fullW * 0.82, roofHeight, fullL * 0.48);
            cabinRoofBox.setOffsetPosition(0.0, roofOffsetY, -fullL * 0.08);
            cabinRoofBox.enable();
        }
    }

    public void recomputeInertia() {
        float ix = (1.0f / 12.0f) * this.mass * (height * height + length * length);
        float iy = (1.0f / 12.0f) * this.mass * (width * width + length * length);
        float iz = (1.0f / 12.0f) * this.mass * (width * width + height * height);
        this.invInertiaLocal.set(1.0f / Math.max(1.0f, ix), 1.0f / Math.max(1.0f, iy), 1.0f / Math.max(1.0f, iz));
    }

    public void destroy() {
        if (cabinRoofBox != null) {
            cabinRoofBox.destroy();
            cabinRoofBox = null;
        }
        if (mainHullBox != null) {
            mainHullBox.destroy();
            mainHullBox = null;
        }
        if (skidplateBox != null) {
            skidplateBox.destroy();
            skidplateBox = null;
        }
        chassisBox = null;
        if (body != null) {
            body.destroy();
            body = null;
        }
    }

    public void configureFromDefinition(VehicleDefinition def) {
        if (def == null) return;

        if (def.calibratedBounds != null) {
            if (def.calibratedBounds.halfWidth > 0.2) this.width = (float) (def.calibratedBounds.halfWidth * 2.0);
            if (def.calibratedBounds.totalHeight > 0.5) this.height = (float) def.calibratedBounds.totalHeight;
            double totalLen = def.calibratedBounds.frontDist + def.calibratedBounds.rearDist;
            if (totalLen > 1.0) this.length = (float) totalLen;
        }

        // 1. Process physics specification first to know mass, spring stiffness, and powertrain
        if (def.physics != null) {
            if (def.physics.weightDistributionFront > 0.1 && def.physics.weightDistributionFront < 0.9) {
                this.weightDistributionFront = (float) def.physics.weightDistributionFront;
            }
            this.springStiffness = (float) Math.max(10000.0, def.physics.suspensionStiffness * 45000.0);
            this.springDamping = (float) Math.max(1000.0, def.physics.suspensionDamping * 4000.0);

            if (def.physics.mass > 100.0) {
                this.powertrainSpec.baseMassKg = (float) def.physics.mass;
                this.mass = this.powertrainSpec.baseMassKg;
            }

            if (def.physics.torqueNm > 20.0) {
                this.powertrainSpec.maxTorqueNm = (float) def.physics.torqueNm;
            } else if (def.physics.horsepower > 10) {
                float hp = (float) def.physics.horsepower;
                this.powertrainSpec.maxTorqueNm = Math.max(180.0f, (hp * 7127.0f) / this.powertrainSpec.peakTorqueRpm);
            }

            if (def.physics.dragCoefficient > 0.05) {
                this.powertrainSpec.dragCd = (float) def.physics.dragCoefficient;
            }
            if (def.physics.frontalArea > 0.5) {
                this.powertrainSpec.frontalAreaM2 = (float) def.physics.frontalArea;
            } else {
                this.powertrainSpec.frontalAreaM2 = Math.max(1.5f, (this.width * 0.82f) * (this.height * 0.75f));
            }

            if (def.physics.downforceCoefficient >= 0.0) {
                this.powertrainSpec.downforceCl = (float) def.physics.downforceCoefficient;
            }

            if (def.physics.idleRpm > 400.0) {
                this.powertrainSpec.minRpm = (float) def.physics.idleRpm;
            }
            if (def.physics.redlineRpm > 2000.0) {
                this.powertrainSpec.maxRpm = (float) def.physics.redlineRpm;
                this.powertrainSpec.revLimiterRpm = (float) (def.physics.redlineRpm + 250.0);
            }

            if (def.physics.finalDrive > 0.5) {
                this.powertrainSpec.differentialRatio = (float) def.physics.finalDrive;
            }
            if (def.physics.gearRatios != null && def.physics.gearRatios.length > 0) {
                this.powertrainSpec.gearRatios = new float[def.physics.gearRatios.length];
                for (int g = 0; g < def.physics.gearRatios.length; g++) {
                    this.powertrainSpec.gearRatios[g] = (float) def.physics.gearRatios[g];
                }
            }
            if (def.physics.reverseRatio > 0.5) {
                this.powertrainSpec.reverseGearRatio = -(float) Math.abs(def.physics.reverseRatio);
            }

            if (def.physics.drivetrain != null && !def.physics.drivetrain.isEmpty()) {
                this.drivetrain = def.physics.drivetrain.toLowerCase(java.util.Locale.ROOT);
            }

            if (def.physics.maxSteerAngle > 5.0) {
                this.steeringRack.maxWheelSteerAngleRad = (float) Math.toRadians(def.physics.maxSteerAngle);
            }
            if (def.physics.steeringResponse > 0.1) {
                this.steeringRack.steeringDamping = (float) (0.28 / def.physics.steeringResponse);
            }
            if (def.physics.turnInBite > 0.1) {
                this.steeringRack.mechanicalTrail = (float) (0.025 * def.physics.turnInBite);
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

        if (def.wheels != null && def.wheels.radius > 0.05f) {
            this.wheelRadius = (float) def.wheels.radius;
            this.powertrainSpec.wheelRadius = this.wheelRadius;
        }

        this.powertrain.configureFromSpec(this.powertrainSpec);

        // 2. Wheel positions & dynamic suspension mounts based on static sag
        if (def.wheels != null) {
            float[] fl = def.wheels.frontLeft;
            float[] fr = def.wheels.frontRight;
            float[] rl = def.wheels.rearLeft;
            float[] rr = def.wheels.rearRight;

            if (fl != null && rl != null) {
                float midZ = (fl[2] + rl[2]) * 0.5f;
                float midY = Math.max(0.40f, (fl[1] + rl[1]) * 0.5f + 0.22f);
                this.comHeight = midY;

                // Dynamic mount Y: strut top is located (L_rest - x_static) above the wheel axle center
                float quarterMass = this.mass * 0.25f;
                float xStaticFront = (quarterMass * 9.80665f) / Math.max(1000.0f, this.springStiffness);
                float xStaticRear = (quarterMass * 9.80665f) / Math.max(1000.0f, this.springStiffness);

                float mountYFront = (fl[1] - this.comHeight) + (this.suspensionRestLength - xStaticFront);
                float mountYRear = (rl[1] - this.comHeight) + (this.suspensionRestLength - xStaticRear);

                wheelMountsLocal[0].set(fl[0], mountYFront, fl[2] - midZ);
                wheelMountsLocal[1].set(fr[0], mountYFront, fr[2] - midZ);
                wheelMountsLocal[2].set(rl[0], mountYRear, rl[2] - midZ);
                wheelMountsLocal[3].set(rr[0], mountYRear, rr[2] - midZ);
            }

            if (def.wheels.radius > 0.1f) {
                this.wheelRadius = (float) def.wheels.radius;
                this.powertrainSpec.wheelRadius = this.wheelRadius;
                float confWheelInertia = 0.5f * 22.0f * (this.wheelRadius * this.wheelRadius);
                for (int i = 0; i < 4; i++) {
                    this.wheelStates[i].init(this.wheelRadius, confWheelInertia);
                }
            }
        }

        recomputeInertia();
        if (body != null) {
            DMass m = OdeHelper.createMass();
            m.setBoxTotal(this.mass, this.width, this.height, this.length);
            body.setMass(m);

            // Re-align ODE body COM height with true spawn elevation
            DVector3C curP = body.getPosition();
            body.setPosition(curP.get0(), this.spawnBaseY + this.comHeight, curP.get2());
            currentPosition.set((float) curP.get0(), (float) (this.spawnBaseY + this.comHeight), (float) curP.get2());
            prevPosition.set(currentPosition);
        }

        if (def.traversal != null) {
            this.traversal.copyFrom(def.traversal);
        }
        updateCompoundGeometries(def);
        rebuildSuspensionSpecs();
    }

    /**
     * Updates dynamic vehicle mass and rebuilds the inertia tensor based on driver, passengers, and cargo.
     */
    public void updateDynamicMass(int passengers, float cargoKg) {
        this.passengerCount = passengers;
        this.dynamicCargoKg = cargoKg;
        float newMass = MarcoMonsterAero.calculateTotalMass(powertrainSpec.baseMassKg, passengers, cargoKg);
        if (Math.abs(newMass - this.mass) > 1.0f) {
            this.mass = newMass;
            recomputeInertia();
            if (body != null) {
                DMass m = OdeHelper.createMass();
                m.setBoxTotal(this.mass, this.width, this.height, this.length);
                body.setMass(m);
            }
            rebuildSuspensionSpecs();
        }
    }

    public float updateSteeringAngleRad(float rawSteerInput, float dt) {
        float trackW = Math.max(0.8f, Math.abs(wheelMountsLocal[0].x - wheelMountsLocal[1].x));
        float wheelB = Math.max(1.0f, Math.abs(wheelMountsLocal[0].z - wheelMountsLocal[2].z));

        steeringRack.update(
                rawSteerInput,
                wheelStates[0].outForceLateral,
                wheelStates[1].outForceLateral,
                wheelStates[0].outSlipAngleTanNorm,
                wheelStates[1].outSlipAngleTanNorm,
                trackW,
                wheelB,
                dt
        );

        this.currentSteerRad = steeringRack.getCenterWheelAngle();

        // Apply physical kingpin reaction torque to vehicle chassis
        if (body != null) {
            Vector3f rt = steeringRack.chassisReactionTorque;
            body.addTorque(rt.x, rt.y, rt.z);
        }

        return this.currentSteerRad;
    }

    /**
     * Called on each physics sub-tick to compute 4-wheel suspension, powertrain, and tire forces.
     */
    public void computeWheelForces(Level level, float dt) {
        if (body == null || !body.isEnabled()) return;

        DVector3C pos = body.getPosition();
        DMatrix3C rot = body.getRotation();
        DVector3C linVelD = body.getLinearVel();
        DVector3C angVelD = body.getAngularVel();

        tempLinVel.set((float) linVelD.get0(), (float) linVelD.get1(), (float) linVelD.get2());
        tempAngVel.set((float) angVelD.get0(), (float) angVelD.get1(), (float) angVelD.get2());

        // Local body axes in world space:
        // Col 0 is +X (Right), Col 1 is +Y (Up), Col 2 is +Z (Back)
        // Therefore:
        // Forward (-Z) = -(Col 2)
        // Up (+Y)      = +(Col 1)
        // Right (+X)   = +(Col 0)
        float upX = (float) rot.get01(), upY = (float) rot.get11(), upZ = (float) rot.get21();
        float fwdX = -(float) rot.get02(), fwdY = -(float) rot.get12(), fwdZ = -(float) rot.get22();
        float rightX = (float) rot.get00(), rightY = (float) rot.get10(), rightZ = (float) rot.get20();

        tempLocalUp.set(upX, upY, upZ);
        tempLocalFwd.set(fwdX, fwdY, fwdZ);
        tempLocalRight.set(rightX, rightY, rightZ);

        // Compute world inverse inertia matrix: I_world^-1 = R * I_local^-1 * R^T
        rotMat.setRow(0, (float) rot.get00(), (float) rot.get01(), (float) rot.get02());
        rotMat.setRow(1, (float) rot.get10(), (float) rot.get11(), (float) rot.get12());
        rotMat.setRow(2, (float) rot.get20(), (float) rot.get21(), (float) rot.get22());

        tempMat.set(rotMat).scale(invInertiaLocal.x, invInertiaLocal.y, invInertiaLocal.z);
        rotMat.transpose(invInertiaWorld);
        tempMat.mul(invInertiaWorld, invInertiaWorld);

        // 1. Aerodynamics (Marco Monster Air Drag, Downforce, Rolling Resistance)
        MarcoMonsterAero.computeAeroAndRollingResistance(
                tempLinVel,
                tempLocalUp,
                tempLocalFwd,
                this.mass,
                groundedWheelsCount > 0,
                powertrainSpec.frontalAreaM2,
                powertrainSpec.dragCd,
                powertrainSpec.downforceCl,
                powertrainSpec.groundRollResist,
                tempAeroForce
        );
        body.addForce(tempAeroForce.x, tempAeroForce.y, tempAeroForce.z);

        // 2. Average Driven Wheel Angular Velocity & Maximum Slip
        float sumDrivenOmega = 0.0f;
        int drivenCount = 0;
        float maxSlip = 0.0f;
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);
            if (isDriven) {
                sumDrivenOmega += wheelStates[i].angularVelocity;
                drivenCount++;
                maxSlip = Math.max(maxSlip, wheelStates[i].outCombinedSlipNorm);
            }
        }
        this.avgDrivenWheelAngularVel = drivenCount > 0 ? (sumDrivenOmega / drivenCount) : 0.0f;
        this.maxDrivenWheelSlip = maxSlip;

        float wheelMass = 22.0f;
        float wheelBaseInertia = 0.5f * wheelMass * (wheelRadius * wheelRadius);
        float totalDrivenWheelInertia = drivenCount * wheelBaseInertia + (this.mass * 0.45f) * (wheelRadius * wheelRadius);

        // 3. Jolt Physics Powertrain Update (Engine RPM, Clutch, Multi-gear Transmission)
        float forwardSpeedMps = tempLinVel.dot(tempLocalFwd);
        float totalWheelTorqueNm = powertrain.isEngineOn ? powertrain.step(
                dt,
                throttleInput,
                powertrain.clutchPedalPressed,
                avgDrivenWheelAngularVel,
                powertrain.currentGear
        ) : 0.0f;

        float driveTorquePerWheel = drivenCount > 0 ? (totalWheelTorqueNm / drivenCount) : 0.0f;
        if (this.impactCutoffActive) {
            driveTorquePerWheel = 0.0f;
            this.impactCutoffActive = false;
        }
        this.baseDriveForce = driveTorquePerWheel / Math.max(0.1f, wheelRadius);

        // Динамический расчет честной инерции колес с непрерывным couplingFactor
        float effectiveDrivenInertia = powertrain.getEffectiveDrivenWheelInertia(wheelBaseInertia, powertrain.currentGear, drivenCount);

        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);

            float effectiveInertia = isDriven ? effectiveDrivenInertia : wheelBaseInertia;
            wheelStates[i].wheelInertia = effectiveInertia;
            wheelStates[i].invWheelInertia = 1.0f / Math.max(0.01f, effectiveInertia);
            wheelStates[i].isClutchLocked = isDriven && powertrain.isClutchLocked;
            wheelStates[i].maxAllowedOmega = Float.MAX_VALUE;
        }

        groundedWheelsCount = 0;
        float steerAngleRad = updateSteeringAngleRad(this.steerInput, dt);
        float cosSteerL = (float) Math.cos(steeringRack.leftWheelAngle);
        float sinSteerL = (float) Math.sin(steeringRack.leftWheelAngle);
        float cosSteerR = (float) Math.cos(steeringRack.rightWheelAngle);
        float sinSteerR = (float) Math.sin(steeringRack.rightWheelAngle);

        float absSpeed = Math.abs(forwardSpeedMps);
        float moveSign = (forwardSpeedMps < -0.2f || (absSpeed <= 0.2f && this.throttleInput < -0.05f)) ? -1.0f : 1.0f;
        float moveDirX = fwdX * moveSign;
        float moveDirZ = fwdZ * moveSign;
        boolean hasDriveIntent = (moveSign > 0.0f) ? (this.throttleInput > 0.05f) : (this.throttleInput < -0.05f);

        // 4. PASS 1: Multi-Ray Envelope Suspension & Parametric Bump/Droop Solver (Zero-GC, 80 Hz)
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            Vector3f mount = wheelMountsLocal[i];

            // Local wheel orientation in world space (orthogonal basis)
            Vector3f wf = wheelFwdVec[i];
            Vector3f wr = wheelRightVec[i];
            if (isFront) {
                float cosS = (i == 0) ? cosSteerL : cosSteerR;
                float sinS = (i == 0) ? sinSteerL : sinSteerR;
                wf.set(fwdX * cosS + rightX * sinS, fwdY * cosS + rightY * sinS, fwdZ * cosS + rightZ * sinS);
                wr.set(rightX * cosS - fwdX * sinS, rightY * cosS - fwdY * sinS, rightZ * cosS - fwdZ * sinS);
            } else {
                wf.set(fwdX, fwdY, fwdZ);
                wr.set(rightX, rightY, rightZ);
            }

            // Mount position relative to COM in world space:
            float mx = (float) (rot.get00() * mount.x + rot.get01() * mount.y + rot.get02() * mount.z);
            float my = (float) (rot.get10() * mount.x + rot.get11() * mount.y + rot.get12() * mount.z);
            float mz = (float) (rot.get20() * mount.x + rot.get21() * mount.y + rot.get22() * mount.z);

            wheelMountWorld[i][0] = mx;
            wheelMountWorld[i][1] = my;
            wheelMountWorld[i][2] = mz;

            double wx = pos.get0() + mx;
            double wy = pos.get1() + my;
            double wz = pos.get2() + mz;

            SuspSpec spec = suspSpecs[i];
            if (spec == null) {
                rebuildSuspensionSpecs();
                spec = suspSpecs[i];
            }
            WheelProbe probe = wheelProbes[i];
            if (probe == null) {
                probe = new WheelProbe(wheelRadius, PROBE_THETA);
                wheelProbes[i] = probe;
            }

            // Wheel step-up virtual ramp probe (Zero-GC, 80 Hz)
            float wheelBottomY = (float) wy - wheelRadius;
            float wheelDirX = wf.x * moveSign;
            float wheelDirZ = wf.z * moveSign;
            float wheelDirLen = (float) Math.sqrt(wheelDirX * wheelDirX + wheelDirZ * wheelDirZ);
            if (wheelDirLen > 0.001f) {
                wheelDirX /= wheelDirLen;
                wheelDirZ /= wheelDirLen;
            }

            float rampLift = stepUpProbes[i].updateRampLift(
                    level,
                    wx,
                    wheelBottomY,
                    wz,
                    wheelDirX,
                    wheelDirZ,
                    absSpeed,
                    hasDriveIntent,
                    wheelRadius,
                    dt
            );

            double rayUpOffset = 0.45;
            double rayUp = rayUpOffset + rampLift;
            double castDist = suspensionRestLength + spec.xDroop + wheelRadius;
            BlockHitResult bestHit = null;

            for (int j = 0; j < probe.getRayCount(); j++) {
                double offsetFwd = wheelRadius * probe.getSinT(j);
                double rx = wx + wf.x * offsetFwd;
                double ry = wy + wf.y * offsetFwd;
                double rz = wz + wf.z * offsetFwd;

                Vec3 rayStart = new Vec3(rx + upX * rayUp, ry + upY * rayUp, rz + upZ * rayUp);
                Vec3 rayEnd = new Vec3(rayStart.x - upX * (castDist + rayUpOffset), rayStart.y - upY * (castDist + rayUpOffset), rayStart.z - upZ * (castDist + rayUpOffset));
                BlockHitResult hit = level.clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.entityOwner));

                int reroute = 2;
                while (hit != null && hit.getType() == HitResult.Type.BLOCK && !WheelStepUpProbe.isClimbableSolid(level, hit.getBlockPos(), level.getBlockState(hit.getBlockPos())) && reroute-- > 0) {
                    Vec3 nextStart = hit.getLocation().add(-upX * 0.05, -upY * 0.05, -upZ * 0.05);
                    if (nextStart.distanceTo(rayEnd) < 0.1) {
                        hit = null;
                        break;
                    }
                    hit = level.clip(new ClipContext(nextStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.entityOwner));
                }

                boolean hitGround = (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getDirection() != Direction.DOWN);
                if (hitGround) {
                    double dFromStart = rayStart.distanceTo(hit.getLocation());
                    double dFromMount = Math.max(0.0, dFromStart - rayUpOffset);

                    // Проекция наклонного луча на нормаль поверхности:
                    // Устраняет ложное удлинение луча при дифференте (d = h / cos(theta)),
                    // предотвращая прогрессирующее проваливание носа в землю.
                    Direction hitDir = hit.getDirection();
                    double nX = hitDir.getStepX(), nY = hitDir.getStepY(), nZ = hitDir.getStepZ();
                    double dotN = upX * nX + upY * nY + upZ * nZ;
                    if (dotN > 0.1) {
                        dFromMount *= dotN;
                    }

                    tempProbeDistances[j] = dFromMount;
                    if (bestHit == null || j == 0) {
                        bestHit = hit;
                    }
                } else {
                    tempProbeDistances[j] = castDist;
                }
            }

            // Wheel hub velocities in world space:
            float hubVx = tempLinVel.x + (tempAngVel.y * mz - tempAngVel.z * my);
            float hubVy = tempLinVel.y + (tempAngVel.z * mx - tempAngVel.x * mz);
            float hubVz = tempLinVel.z + (tempAngVel.x * my - tempAngVel.y * mx);

            // Horizontal velocity in wheel heading plane:
            double vH = hubVx * wf.x + hubVy * wf.y + hubVz * wf.z;
            // Velocity along suspension compression axis (+up towards chassis):
            double vAxis = hubVx * upX + hubVy * upY + hubVz * upZ;

            probe.update(tempProbeDistances, suspensionRestLength, vH, vAxis, spec, dt);

            float cond = (i < tireCondition.length) ? tireCondition[i] : 100.0f;
            boolean isPopped = cond <= 0.0f;

            float comp = (float) probe.x;
            if (isPopped) {
                comp = Math.max(0.0f, comp - 0.15f);
            }
            wheelCompression[i] = comp;
            wheelSuspensionLength[i] = (float) Math.max(0.05, suspensionRestLength - probe.x);

            SurfaceProfile surface = SurfaceProfile.ASPHALT;
            boolean isWet = false;
            float contactNx = 0.0f, contactNy = 1.0f, contactNz = 0.0f;

            if (bestHit != null) {
                BlockPos hitPos = bestHit.getBlockPos();
                BlockState groundBlockState = level.getBlockState(hitPos);
                surface = SurfaceProfile.get(groundBlockState);
                isWet = level.isRainingAt(hitPos);
                Direction dir = bestHit.getDirection();
                contactNx = dir.getStepX();
                contactNy = dir.getStepY();
                contactNz = dir.getStepZ();
            }
            wheelContactNormal[i].set(contactNx, contactNy, contactNz);

            wheelSurface[i] = surface;
            wheelSurfaceWet[i] = isWet;
            if (isFront) {
                frontSurfaceMuScale = surface.muLatScale;
            }

            // Kinematic Camber Calculation:
            // Swing arm length: McPherson for front (lambda=1.2), Multilink for rear (lambda=1.6)
            double trackWidth = Math.max(0.8, Math.abs(wheelMountsLocal[0].x - wheelMountsLocal[1].x));
            double lSa = isFront ? (1.2 * trackWidth) : (1.6 * trackWidth);
            double gamma0 = isFront ? Math.toRadians(-1.5) : Math.toRadians(-1.0);
            float side = (i == 0 || i == 2) ? 1.0f : -1.0f;
            float aOutX = wr.x * side;
            float aOutY = wr.y * side;
            float aOutZ = wr.z * side;
            double gammaOut = camberOut(probe.x, lSa, gamma0, aOutX, aOutY, aOutZ, contactNx, contactNy, contactNz);
            wheelCamberRad[i] = (float) gammaOut;

            if (probe.grounded) {
                groundedWheelsCount++;
                wheelGrounded[i] = true;
                wheelHitDist[i] = Math.max(0.0, suspensionRestLength - probe.x + wheelRadius);

                double fSusp = spec.force(probe.x, probe.v, spec.mC, dt);
                if (isPopped) {
                    fSusp *= 0.40;
                }
                float compRatio = (float) (probe.x / this.suspensionRestLength);
                if (compRatio > 0.65f) {
                    fSusp *= (1.0f + (compRatio - 0.65f) * 3.8f);
                }
                float maxWheelForce = (this.mass * 18.0f) * 1.5f;
                fSusp = Mth.clamp((float) fSusp, 0.0f, maxWheelForce);
                fSusp = BulletWheelFriction.applySafeSurfaceRoughness((float) fSusp, surface, (float) wx, (float) wz, forwardSpeedMps);
                wheelSpringForce[i] = (float) fSusp;

                // Step-Up Assist: Мягкий накат вдоль виртуального пандуса на блок (до 1.05м)
                if (rampLift > 0.02f) {
                    float climbAssistForce = (this.mass * 9.81f * 0.32f) * (rampLift / WheelStepUpProbe.MAX_STEP_HEIGHT);
                    body.addForce(
                            moveDirX * climbAssistForce * 0.4f,
                            climbAssistForce,
                            moveDirZ * climbAssistForce * 0.4f
                    );
                }
            } else {
                wheelGrounded[i] = false;
                wheelHitDist[i] = castDist;
                wheelSpringForce[i] = 0.0f;
            }
        }

        // 5. PASS 2: Anti-Roll Bar Weight Transfer & Suspension Chassis Reaction
        // Parametric ARB stiffness scaled with vehicle spring stiffness
        float arbFrontStiff = Math.max(2000.0f, this.springStiffness * 0.31f);
        float arbRearStiff  = Math.max(1500.0f, this.springStiffness * 0.22f);

        // Front ARB (wheels 0 and 1)
        float deltaArbFront = 0.0f;
        if (wheelGrounded[0] || wheelGrounded[1]) {
            deltaArbFront = arbFrontStiff * (wheelCompression[0] - wheelCompression[1]);
            deltaArbFront = Mth.clamp(deltaArbFront, -wheelSpringForce[0], wheelSpringForce[1]);
        }
        wheelNormalLoadFz[0] = Math.max(0.0f, wheelSpringForce[0] + deltaArbFront);
        wheelNormalLoadFz[1] = Math.max(0.0f, wheelSpringForce[1] - deltaArbFront);

        // Rear ARB (wheels 2 and 3)
        float deltaArbRear = 0.0f;
        if (wheelGrounded[2] || wheelGrounded[3]) {
            deltaArbRear = arbRearStiff * (wheelCompression[2] - wheelCompression[3]);
            deltaArbRear = Mth.clamp(deltaArbRear, -wheelSpringForce[2], wheelSpringForce[3]);
        }
        wheelNormalLoadFz[2] = Math.max(0.0f, wheelSpringForce[2] + deltaArbRear);
        wheelNormalLoadFz[3] = Math.max(0.0f, wheelSpringForce[3] - deltaArbRear);

        // Apply normal forces to chassis at suspension mount points
        // Normal force difference across the track width naturally produces the anti-roll torque
        for (int i = 0; i < 4; i++) {
            if (wheelGrounded[i] && wheelNormalLoadFz[i] > 0.001f) {
                float fn = wheelNormalLoadFz[i];
                // Реакция опоры прикладывается вдоль нормали контакта (а не наклонного кузова):
                // Исключает паразитную продольную тягу и обеспечивает честное удержание кузова против силы тяжести.
                Vector3f cN = wheelContactNormal[i];
                float fsX = cN.x * fn, fsY = cN.y * fn, fsZ = cN.z * fn;
                float mx = wheelMountWorld[i][0];
                float my = wheelMountWorld[i][1];
                float mz = wheelMountWorld[i][2];
                body.addForce(fsX, fsY, fsZ);
                body.addTorque(my * fsZ - mz * fsY, mz * fsX - mx * fsZ, mx * fsY - my * fsX);
            }
        }

        // 6. PASS 3: Brush Tire Model & Ground Friction (Combined Slip, Zero-Crossing Clamp)
        float frontLateralForceSum = 0.0f;
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);
            float cond = (i < tireCondition.length) ? tireCondition[i] : 100.0f;
            boolean isPopped = cond <= 0.0f;

            if (!wheelGrounded[i]) {
                wheelSlipRatio[i] = 0.0f;
                wheelStates[i].wheelInertia = isDriven ? effectiveDrivenInertia : wheelBaseInertia;
                wheelStates[i].invWheelInertia = 1.0f / Math.max(0.01f, wheelStates[i].wheelInertia);
                wheelStates[i].maxAllowedOmega = Float.MAX_VALUE;
                float freeDriveT = isDriven ? (isPopped ? driveTorquePerWheel * 0.70f : driveTorquePerWheel) : 0.0f;
                float freeBrakeT = (handbrakeInput && !isFront) ? 4500.0f : (powertrain.isBraking ? 2500.0f : 0.0f);
                BrushTireSolver.solveWheelStep(wheelStates[i], 0.0f, 0.0f, 0.0f, freeDriveT, freeBrakeT, 0.0f, dt, 1.0f);
                wheelAngularVel[i] = wheelStates[i].angularVelocity;
                continue;
            }

            // На земле: честная инерция колеса (без фейковой массы кузова carInertiaPerDrivenWheel)
            float effectiveInertia = isDriven ? effectiveDrivenInertia : wheelBaseInertia;
            wheelStates[i].wheelInertia = effectiveInertia;
            wheelStates[i].invWheelInertia = 1.0f / Math.max(0.01f, effectiveInertia);
            wheelStates[i].isClutchLocked = isDriven && powertrain.isClutchLocked;
            wheelStates[i].maxAllowedOmega = Float.MAX_VALUE;

            Vector3f curWheelFwd = wheelFwdVec[i];
            Vector3f curWheelRight = wheelRightVec[i];

            // Contact patch position relative to COM:
            float mx = wheelMountWorld[i][0];
            float my = wheelMountWorld[i][1];
            float mz = wheelMountWorld[i][2];
            float hitD = (float) wheelHitDist[i];

            float cx = mx - upX * hitD;
            float cy = my - upY * hitD;
            float cz = mz - upZ * hitD;
            tempContactArm.set(cx, cy, cz);

            // Contact patch linear velocity in world coordinates: v_patch = v_lin + omega x r_contact
            float patchVx = tempLinVel.x + (tempAngVel.y * cz - tempAngVel.z * cy);
            float patchVy = tempLinVel.y + (tempAngVel.z * cx - tempAngVel.x * cz);
            float patchVz = tempLinVel.z + (tempAngVel.x * cy - tempAngVel.y * cx);

            // Project onto wheel local forward and right directions:
            float vx = patchVx * curWheelFwd.x + patchVy * curWheelFwd.y + patchVz * curWheelFwd.z;
            float vy = patchVx * curWheelRight.x + patchVy * curWheelRight.y + patchVz * curWheelRight.z;

            // Torques (Authentic E-Brake disc lock and Powertrain drive torque)
            float driveT = 0.0f;
            float brakeT = 0.0f;

            if (handbrakeInput && !isFront) {
                driveT = 0.0f;
                brakeT = 4500.0f; // Handbrake clamps rear disc on 100%
            } else if (powertrain.isBraking) {
                driveT = 0.0f;
                brakeT = isFront ? 3500.0f : 2500.0f; // Front-biased service brakes
            } else if (isDriven) {
                driveT = isPopped ? (driveTorquePerWheel * 0.70f) : driveTorquePerWheel;
                brakeT = (Math.abs(throttleInput) < 0.01f) ? (Math.abs(forwardSpeedMps) > 0.1f ? (isFront ? 35.0f : 65.0f) : 150.0f) : 0.0f;
            } else {
                driveT = 0.0f;
                // Non-driven wheels rotate freely when accelerating or rolling;
                // when stopped (< 0.1 m/s) and off-throttle, apply holding brake to prevent creep
                brakeT = (Math.abs(throttleInput) < 0.01f) ? (Math.abs(forwardSpeedMps) > 0.1f ? 15.0f : 120.0f) : 0.0f;
            }

            // Гарантируем отсутствие паразитного торможения на ВСЕХ 4 колесах при нажатом газе
            if (Math.abs(throttleInput) > 0.01f && !handbrakeInput) {
                brakeT = 0.0f;
            }

            // Effective inverse mass along lateral axis (from Bullet resolveSingleBilateral)
            tempContactArm.cross(curWheelRight, tempCross);
            invInertiaWorld.transform(tempCross);
            tempCross.cross(tempContactArm);
            float angularDenominator = tempCross.dot(curWheelRight);
            float effectiveInvMass = (1.0f / this.mass) + Math.max(0.0f, angularDenominator);
            float effectiveMassLat = 1.0f / Math.max(1e-5f, effectiveInvMass);

            // Surface friction coefficient & offroad traversal modifiers
            SurfaceProfile surface = wheelSurface[i];
            float offroadGrip = (surface != SurfaceProfile.ASPHALT) ? this.traversal.offroadGripMultiplier : 1.0f;
            float offroadDrag = (surface != SurfaceProfile.ASPHALT) ? this.traversal.offroadDragMultiplier : 1.0f;

            float weatherFactor = wheelSurfaceWet[i] ? surface.rainGripFactor : 1.0f;
            float surfaceMu = surface.muLatScale * weatherFactor * offroadGrip;
            if (isFront) {
                surfaceMu *= 0.96f; // Front-soft understeer bias (Kus > 0)
            }
            if (isPopped) {
                surfaceMu *= 0.35f;
            }

            // 80 Hz Brush Tire Solver step with dynamic camber thrust and dynamic nominal load
            float nominalFz = (this.mass * 9.80665f) * 0.25f;
            BrushTireSolver.solveWheelStep(
                    wheelStates[i],
                    wheelNormalLoadFz[i],
                    nominalFz,
                    vx,
                    vy,
                    driveT,
                    brakeT,
                    effectiveMassLat,
                    dt,
                    surfaceMu,
                    wheelCamberRad[i],
                    (i == 0 || i == 2) ? 1.0f : -1.0f
            );

            float fx = wheelStates[i].outForceLongitudinal;
            float fy = wheelStates[i].outForceLateral;

            if (isPopped) {
                // Честное сопротивление качению стального обода: 14% от нормальной нагрузки этого колеса
                float effectiveNormalLoad = wheelNormalLoadFz[i];
                float rimRollingDrag = effectiveNormalLoad * 0.14f;
                float maxStopForce = Math.abs(vx) * (this.mass * 0.25f) / dt;
                float finalRimDrag = -Math.signum(vx) * Math.min(rimRollingDrag, maxStopForce);
                fx += finalRimDrag;
            } else if (Math.abs(vx) > 0.01f) {
                // Честное поколесное сопротивление качению шины по Кулону (с ограничением по импульсу массы)
                float maxStopForce = Math.abs(vx) * (this.mass * 0.25f) / dt;
                float smoothVelFactor = (float) Math.tanh(vx / 0.35f);
                float effectiveRollRes = surface.rollingResistance * offroadDrag;
                float rrForce = -smoothVelFactor * Math.min(wheelNormalLoadFz[i] * effectiveRollRes, maxStopForce);
                fx += rrForce;
            }

            wheelSlipRatio[i] = wheelStates[i].outCombinedSlipNorm;
            wheelAngularVel[i] = wheelStates[i].angularVelocity;

            if (isFront) {
                frontLateralForceSum += fy;
            }

            // Bullet m_rollInfluence vertical moment arm adjustment
            float vertDist = tempContactArm.dot(tempLocalUp);
            tempAdjustedArm.set(tempContactArm).fma(-vertDist * (1.0f - powertrainSpec.rollInfluence), tempLocalUp);

            // Transform local tire forces to world space and apply to ODE4J body
            float fwX = curWheelFwd.x * fx + curWheelRight.x * fy;
            float fwY = curWheelFwd.y * fx + curWheelRight.y * fy;
            float fwZ = curWheelFwd.z * fx + curWheelRight.z * fy;

            body.addForce(fwX, fwY, fwZ);
            body.addTorque(
                    tempAdjustedArm.y * fwZ - tempAdjustedArm.z * fwY,
                    tempAdjustedArm.z * fwX - tempAdjustedArm.x * fwZ,
                    tempAdjustedArm.x * fwY - tempAdjustedArm.y * fwX
            );
        }

        // 7. Accumulate driven wheel road reaction torque for powertrain LCP on next tick:
        float sumRoadTorque = 0.0f;
        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isDriven = drivetrain.equals("fwd") ? isFront : (drivetrain.equals("awd") || drivetrain.equals("4wd") ? true : !isFront);
            if (isDriven && wheelGrounded[i]) {
                sumRoadTorque += wheelStates[i].outForceLongitudinal * this.wheelRadius;
            }
        }
        this.lastRoadReactionTorque = sumRoadTorque;

        // Store front lateral force for caster self-aligning torque on next sub-tick
        this.prevFrontLateralForce = frontLateralForceSum;
    }

    public void syncRenderRegisters() {
        if (body == null) return;
        DVector3C pos = body.getPosition();
        DQuaternionC q = body.getQuaternion();

        prevPosition.set(currentPosition);
        prevRotation.set(currentRotation);

        currentPosition.set((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
        currentRotation.set((float) q.get1(), (float) q.get2(), (float) q.get3(), (float) q.get0());
    }

    public Quaternionf getSmoothRotation(float partialTick, Quaternionf dest) {
        return prevRotation.slerp(currentRotation, partialTick, dest);
    }

    public Vector3f getSmoothPosition(float partialTick, Vector3f dest) {
        return prevPosition.lerp(currentPosition, partialTick, dest);
    }

    public float getYaw() {
        if (body == null) return 0.0f;
        DMatrix3C rot = body.getRotation();
        float fwdX = -(float) rot.get02();
        float fwdZ = -(float) rot.get22();
        return (float) Math.toDegrees(Math.atan2(-fwdX, fwdZ));
    }

    public float getPitch() {
        if (body == null) return 0.0f;
        DMatrix3C rot = body.getRotation();
        float fwdY = -(float) rot.get12();
        return (float) Math.toDegrees(-Math.asin(Mth.clamp(fwdY, -1.0f, 1.0f)));
    }

    public float getRoll() {
        if (body == null) return 0.0f;
        DMatrix3C rot = body.getRotation();
        float rightY = (float) rot.get10(); // +Col 0 is TrueRight
        float upY = (float) rot.get11();
        return (float) Math.toDegrees(Math.atan2(rightY, upY));
    }

    public double getSpeedKmh() {
        if (body == null) return 0.0;
        DVector3C vel = body.getLinearVel();
        return Math.sqrt(vel.get0() * vel.get0() + vel.get1() * vel.get1() + vel.get2() * vel.get2()) * 3.6;
    }

    public void wakeUp() {
        if (body != null) {
            body.enable();
        }
    }
}
