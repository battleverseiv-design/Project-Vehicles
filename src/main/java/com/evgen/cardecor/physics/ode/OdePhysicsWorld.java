package com.evgen.cardecor.physics.ode;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * World Physics Dispatcher using ODE4J for Minecraft 1.20.1.
 * Runs 4 sub-steps per game tick (80 Hz, DT = 0.0125s) with full 6-DOF rigid-body dynamics,
 * continuous multi-ray bumper CCD, compound chassis shapes, and high-stiffness contact solver.
 */
public class OdePhysicsWorld {

    public static final int SUB_STEPS = 4;
    public static final float DT = 0.05f / SUB_STEPS; // 0.0125 seconds

    static {
        OdeHelper.initODE();
    }

    private static final Map<Level, OdePhysicsWorld> WORLDS = new WeakHashMap<>();

    public static synchronized OdePhysicsWorld get(Level level) {
        return WORLDS.computeIfAbsent(level, k -> new OdePhysicsWorld());
    }

    private final DWorld world;
    private final DHashSpace space;
    private final DJointGroup contactGroup;
    private final DContactBuffer contactBuffer = new DContactBuffer(32);

    private final List<OdeVehicleBody> vehicles = new ArrayList<>();
    private final List<DBox> blockBoxPool = new ArrayList<>();
    private int activeBlockBoxes = 0;

    // Joint feedback pool for Impact Torque Cutoff (Zero-GC)
    private final List<DJoint.DJointFeedback> feedbackPool = new ArrayList<>(64);
    private final List<DJoint> activeImpactJoints = new ArrayList<>(64);
    private final List<OdeVehicleBody> impactVehicles = new ArrayList<>(64);
    private int activeFeedbacks = 0;

    // Voxel deduplication set across vehicles
    private final LongOpenHashSet processedBlockKeys = new LongOpenHashSet(512);

    private long lastTickTime = -1;

    public OdePhysicsWorld() {
        OdeHelper.initODE();
        this.world = OdeHelper.createWorld();
        // Minecraft gravity: -18.0 blocks/s^2 gives authoritative vehicle weight and descent speed
        this.world.setGravity(0, -18.0, 0);
        this.world.setCFM(1e-5);
        this.world.setERP(0.2);
        // Optimal 14 iterations for multi-vehicle 80 Hz stability and low CPU load
        this.world.setQuickStepNumIterations(14);
        // Anti-Catapult limit: prevents explosive ejection when penetrating walls
        this.world.setContactMaxCorrectingVel(3.5);
        // 5mm contact buffer skin prevents jitter on voxel mesh boundaries
        this.world.setContactSurfaceLayer(0.005);

        this.space = OdeHelper.createHashSpace();
        this.contactGroup = OdeHelper.createJointGroup();
    }

    public DWorld getWorld() {
        return world;
    }

    public DHashSpace getSpace() {
        return space;
    }

    public void addVehicle(OdeVehicleBody body) {
        if (!vehicles.contains(body)) {
            vehicles.add(body);
        }
    }

    public void removeVehicle(OdeVehicleBody body) {
        vehicles.remove(body);
        body.destroy();
    }

    public void tickLevelOnce(Level level) {
        long time = level.getGameTime();
        if (time != lastTickTime) {
            lastTickTime = time;
            stepWorld(level);
        }
    }

    public void stepWorld(Level level) {
        if (vehicles.isEmpty()) return;

        // 1. Gather all solid Minecraft block collision shapes surrounding active vehicles
        prepareBlockColliders(level);

        // 2. Execute 4 simulation sub-steps (80 Hz)
        for (int step = 0; step < SUB_STEPS; step++) {
            // Apply CCD and 4-wheel suspension/powertrain forces
            for (int i = 0; i < vehicles.size(); i++) {
                OdeVehicleBody v = vehicles.get(i);
                if (v.body != null && v.body.isEnabled()) {
                    v.performBumperSweptCCDMulti(level, DT);
                    v.computeWheelForces(level, DT);
                }
            }

            // Chassis vs Block collision
            OdeHelper.spaceCollide(space, null, this::nearCallback);

            // Integrate ODE world
            world.quickStep(DT);

            // Impact torque cutoff after solver integration
            postSolveTorqueCutoff();

            // Clear temporary collision contact joints
            contactGroup.empty();
        }

        // 3. Deactivate pooled block colliders until next tick
        clearBlockColliders();

        // 4. Record new positions and orientations for smooth render interpolation
        for (int i = 0; i < vehicles.size(); i++) {
            vehicles.get(i).syncRenderRegisters();
        }
    }

    private void prepareBlockColliders(Level level) {
        for (int i = 0; i < activeBlockBoxes; i++) {
            blockBoxPool.get(i).disable();
        }
        activeBlockBoxes = 0;
        processedBlockKeys.clear();

        for (int i = 0; i < vehicles.size(); i++) {
            OdeVehicleBody v = vehicles.get(i);
            if (v.body == null || !v.body.isEnabled()) continue;

            DVector3C pos = v.body.getPosition();
            DVector3C vel = v.body.getLinearVel();

            // Swept displacement across full 50ms tick
            double sweepX = vel.get0() * 0.05;
            double sweepY = vel.get1() * 0.05;
            double sweepZ = vel.get2() * 0.05;

            // Full 3D bounding radius ensuring blocks are gathered even when flipped on roof or tumbled at 45 deg
            double boundRadius = Math.sqrt(v.length * v.length + v.height * v.height + v.width * v.width) * 0.5 + 1.2;
            double minX = Math.min(pos.get0(), pos.get0() + sweepX) - boundRadius;
            double maxX = Math.max(pos.get0(), pos.get0() + sweepX) + boundRadius;
            double minY = Math.min(pos.get1(), pos.get1() + sweepY) - boundRadius;
            double maxY = Math.max(pos.get1(), pos.get1() + sweepY) + boundRadius;
            double minZ = Math.min(pos.get2(), pos.get2() + sweepZ) - boundRadius;
            double maxZ = Math.max(pos.get2(), pos.get2() + sweepZ) + boundRadius;

            // Low-GC: 1 AABB per vehicle per 50ms (required by Level.getBlockCollisions API)
            AABB queryBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            Iterable<VoxelShape> shapes = level.getBlockCollisions(v.entityOwner, queryBox);
            for (VoxelShape shape : shapes) {
                if (shape.isEmpty()) continue;

                // Zero-GC box traversal using forAllBoxes
                shape.forAllBoxes((minBx, minBy, minBz, maxBx, maxBy, maxBz) -> {
                    long key = BlockPos.asLong((int) Math.floor(minBx), (int) Math.floor(minBy), (int) Math.floor(minBz))
                            ^ ((long) (minBx * 100.0) + (long) (minBy * 1000.0) * 31L + (long) (minBz * 10000.0) * 961L);
                    if (!processedBlockKeys.add(key)) {
                        return; // Already added by another vehicle or overlapping bounds
                    }

                    double lx = Math.max(0.04, maxBx - minBx);
                    double ly = Math.max(0.04, maxBy - minBy);
                    double lz = Math.max(0.04, maxBz - minBz);
                    double cx = (minBx + maxBx) * 0.5;
                    double cy = (minBy + maxBy) * 0.5;
                    double cz = (minBz + maxBz) * 0.5;

                    DBox odeBox = getOrCreateBlockBox(activeBlockBoxes++);
                    odeBox.setLengths(lx, ly, lz);
                    odeBox.setPosition(cx, cy, cz);
                    odeBox.enable();
                });
            }
        }
    }

    private DBox getOrCreateBlockBox(int index) {
        while (blockBoxPool.size() <= index) {
            DBox newBox = OdeHelper.createBox(space, 1.0, 1.0, 1.0);
            newBox.setCategoryBits(2); // Block
            newBox.setCollideBits(1);  // Vehicle
            newBox.setData(OdeVehicleBody.GeomType.BLOCK);
            blockBoxPool.add(newBox);
        }
        return blockBoxPool.get(index);
    }

    private void clearBlockColliders() {
        for (int i = 0; i < activeBlockBoxes; i++) {
            blockBoxPool.get(i).disable();
        }
    }

    private void nearCallback(Object data, DGeom g1, DGeom g2) {
        // Only test Vehicle Chassis (1) vs Block (2) or Vehicle vs Vehicle (1 vs 1)
        if ((g1.getCategoryBits() & g2.getCollideBits()) == 0 && (g2.getCategoryBits() & g1.getCollideBits()) == 0) {
            return;
        }

        // Ignore internal collision between geoms of the same rigid body
        if (g1.getBody() != null && g1.getBody() == g2.getBody()) {
            return;
        }

        int numContacts = OdeHelper.collide(g1, g2, 32, contactBuffer.getGeomBuffer());
        if (numContacts <= 0) return;

        boolean isBlockContact = (g1.getCategoryBits() == 2) || (g2.getCategoryBits() == 2);
        Object data1 = g1.getData();
        Object data2 = g2.getData();

        // Friction differentiation: 0.08 for skidplate, 0.20 for cabin roof, 0.25 for main hull, 0.50 default
        double contactMu;
        if (data1 == OdeVehicleBody.GeomType.SKIDPLATE || data2 == OdeVehicleBody.GeomType.SKIDPLATE) {
            contactMu = 0.08;
        } else if (data1 == OdeVehicleBody.GeomType.CABIN_ROOF || data2 == OdeVehicleBody.GeomType.CABIN_ROOF) {
            contactMu = 0.20;
        } else if (data1 == OdeVehicleBody.GeomType.MAIN_HULL || data2 == OdeVehicleBody.GeomType.MAIN_HULL) {
            contactMu = 0.25;
        } else {
            contactMu = 0.50;
        }

        boolean isVehicle1 = g1.getBody() != null && (data1 == OdeVehicleBody.GeomType.MAIN_HULL || data1 == OdeVehicleBody.GeomType.SKIDPLATE || data1 == OdeVehicleBody.GeomType.CABIN_ROOF);
        boolean isVehicle2 = g2.getBody() != null && (data2 == OdeVehicleBody.GeomType.MAIN_HULL || data2 == OdeVehicleBody.GeomType.SKIDPLATE || data2 == OdeVehicleBody.GeomType.CABIN_ROOF);

        for (int i = 0; i < numContacts; i++) {
            DContact contact = contactBuffer.get(i);

            if (isBlockContact) {
                // Blocks: zero bounce to prevent erratic hopping on voxel edges
                contact.surface.mode = OdeConstants.dContactSoftERP
                        | OdeConstants.dContactSoftCFM
                        | OdeConstants.dContactApprox1;
                contact.surface.bounce = 0.0;
                contact.surface.bounce_vel = 0.0;
            } else {
                // Vehicle vs Vehicle: controlled elastic collision
                contact.surface.mode = OdeConstants.dContactSoftERP
                        | OdeConstants.dContactSoftCFM
                        | OdeConstants.dContactApprox1
                        | OdeConstants.dContactBounce;
                contact.surface.bounce = 0.15;
                contact.surface.bounce_vel = 1.2;
            }

            // High stiffness constraint: recovers 85% of penetration per 12.5ms sub-step
            contact.surface.soft_erp = 0.85;
            contact.surface.soft_cfm = 0.00001;
            contact.surface.mu = contactMu;

            DJoint joint = OdeHelper.createContactJoint(world, contactGroup, contact);
            joint.attach(g1.getBody(), g2.getBody());

            // Attach feedback to detect severe frontal collisions for Torque Cutoff
            if (isVehicle1 || isVehicle2) {
                if (activeFeedbacks >= feedbackPool.size()) {
                    feedbackPool.add(new DJoint.DJointFeedback());
                }
                DJoint.DJointFeedback fb = feedbackPool.get(activeFeedbacks++);
                joint.setFeedback(fb);
                activeImpactJoints.add(joint);

                OdeVehicleBody v = isVehicle1 ? (OdeVehicleBody) g1.getBody().getData() : (OdeVehicleBody) g2.getBody().getData();
                impactVehicles.add(v);
            }
        }
    }

    public void postSolveTorqueCutoff() {
        for (int i = 0; i < activeImpactJoints.size(); i++) {
            DJoint joint = activeImpactJoints.get(i);
            DJoint.DJointFeedback fb = joint.getFeedback();
            OdeVehicleBody vehicle = impactVehicles.get(i);
            if (fb == null || vehicle == null) continue;

            DVector3 force = (joint.getBody(0) == vehicle.body) ? fb.f1 : fb.f2;
            double fLong = force.get0() * vehicle.tempLocalFwd.x
                    + force.get1() * vehicle.tempLocalFwd.y
                    + force.get2() * vehicle.tempLocalFwd.z;

            // Collision torque cutoff: ONLY on high-speed frontal impacts against obstacles/walls
            // (forward speed > 3.5 m/s, opposing impact force > mass * 15 N, e.g. > 37,500 N for 2500 kg truck)
            DVector3C lv = vehicle.body.getLinearVel();
            double fwdSpeed = lv.get0() * vehicle.tempLocalFwd.x + lv.get1() * vehicle.tempLocalFwd.y + lv.get2() * vehicle.tempLocalFwd.z;
            double impactThreshold = Math.max(25000.0, vehicle.mass * 15.0);
            if (fwdSpeed > 3.5 && fLong < -impactThreshold) {
                vehicle.impactCutoffActive = true;
                vehicle.baseDriveForce = 0.0f;
            }
        }

        activeImpactJoints.clear();
        impactVehicles.clear();
        activeFeedbacks = 0;
    }
}
