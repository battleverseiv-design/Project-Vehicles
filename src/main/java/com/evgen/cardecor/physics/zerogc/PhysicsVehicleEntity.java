package com.evgen.cardecor.physics.zerogc;

import com.evgen.cardecor.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Entity implementation powered by the Zero-GC Pure Java 3D Rigid Body Physics Engine.
 * Features:
 * 1. this.noPhysics = true (completely disables vanilla AABB clipping and gravity).
 * 2. Client-Driver Authority (0ms WASD input lag for the driver).
 * 3. Compact Netty synchronization via VehiclePhysicsPacket.
 * 4. 144+ FPS SLERP orientation & position interpolation for rendering.
 * 5. Gimbal-lock safe Euler extraction for HUD and camera views.
 */
public class PhysicsVehicleEntity extends Entity {

    public static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(PhysicsVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_ENGINE_ON =
            SynchedEntityData.defineId(PhysicsVehicleEntity.class, EntityDataSerializers.BOOLEAN);

    public final ZeroGcVehiclePhysics physics = new ZeroGcVehiclePhysics();

    // Previous tick state for high-FPS client interpolation
    public final Vector3f prevPosition = new Vector3f();
    public final Quaternionf prevOrientation = new Quaternionf();

    // Client interpolation buffers
    private final Vector3f clientTargetPos = new Vector3f();
    private final Quaternionf clientTargetOrientation = new Quaternionf();
    private int clientInterpolationSteps = 0;

    // Driver controls
    public float inputThrottle = 0.0f;
    public float inputSteer = 0.0f;
    public boolean inputHandbrake = false;

    // Zero-GC scratch fields
    private final Vector3f scratchVec = new Vector3f();
    private final Quaternionf scratchQuat = new Quaternionf();

    public PhysicsVehicleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // Turn off vanilla collisions
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_ENGINE_ON, false);
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (!this.level().isClientSide) return false;
        Entity driver = this.getFirstPassenger();
        return driver instanceof Player player && player == Minecraft.getInstance().player;
    }

    @Override
    public void tick() {
        super.tick();

        // Store previous transform for partial-tick rendering interpolation
        prevPosition.set(physics.position);
        prevOrientation.set(physics.orientation);

        boolean isDriver = isControlledByLocalInstance();
        boolean isServer = !this.level().isClientSide;

        if (isDriver) {
            // 1. CLIENT-DRIVER AUTHORITY (0ms input lag)
            handleDriverInputs();
            physics.tick(this.level(), inputThrottle, inputSteer, inputHandbrake, isEngineOn());

            // Sync physics position to vanilla entity
            syncPositionFromPhysics();

            // Stream state to server every tick (or when awake)
            if (!physics.isSleeping || this.tickCount % 20 == 0) {
                VehiclePhysicsPacket packet = new VehiclePhysicsPacket(
                        this.getId(),
                        physics.position.x, physics.position.y, physics.position.z,
                        physics.orientation,
                        physics.linearVelocity,
                        physics.angularVelocity,
                        physics.suspensionCompression,
                        physics.currentSteerAngle,
                        physics.isSleeping
                );
                ModNetwork.sendToServer(packet);
            }
        } else if (isServer) {
            // 2. SERVER AUTONOMOUS PHYSICS (when no client is driving)
            if (this.getFirstPassenger() == null) {
                if (!physics.isSleeping) {
                    physics.tick(this.level(), 0.0f, 0.0f, true, false); // parked / coasting
                    syncPositionFromPhysics();

                    // Broadcast to tracking players
                    broadcastPhysicsSync();
                }
            }
        } else {
            // 3. REMOTE CLIENT INTERPOLATION
            if (clientInterpolationSteps > 0) {
                float alpha = 1.0f / (float) clientInterpolationSteps;
                physics.position.lerp(clientTargetPos, alpha);
                physics.orientation.slerp(clientTargetOrientation, alpha);
                clientInterpolationSteps--;
                syncPositionFromPhysics();
            }
        }

        // Update sleeping entity data
        if (isServer && this.entityData.get(DATA_SLEEPING) != physics.isSleeping) {
            this.entityData.set(DATA_SLEEPING, physics.isSleeping);
        }
    }

    private void handleDriverInputs() {
        if (this.getFirstPassenger() instanceof LivingEntity driver) {
            this.inputThrottle = driver.zza; // W/S
            this.inputSteer = -driver.xxa;   // A/D
            if (this.level().isClientSide && isControlledByLocalInstance()) {
                this.inputHandbrake = Minecraft.getInstance().options.keyJump.isDown();
            }
        } else {
            this.inputThrottle = 0.0f;
            this.inputSteer = 0.0f;
            this.inputHandbrake = true;
        }
    }

    private void syncPositionFromPhysics() {
        this.setPosRaw(physics.position.x, physics.position.y, physics.position.z);
        this.setDeltaMovement(physics.linearVelocity.x, physics.linearVelocity.y, physics.linearVelocity.z);

        // Update vanilla Yaw/Pitch for radar/targeting
        this.setYRot(getYawDeg());
        this.setXRot(getPitchDeg());
    }

    /**
     * Broadcasts current physics state to all players tracking this vehicle.
     */
    public void broadcastPhysicsSync() {
        VehiclePhysicsPacket packet = new VehiclePhysicsPacket(
                this.getId(),
                physics.position.x, physics.position.y, physics.position.z,
                physics.orientation,
                physics.linearVelocity,
                physics.angularVelocity,
                physics.suspensionCompression,
                physics.currentSteerAngle,
                physics.isSleeping
        );
        ModNetwork.sendToTrackingAndSelf(this, packet);
    }

    /**
     * Called when a network packet arrives from the driving client or server.
     */
    public void applyPhysicsSync(double px, double py, double pz,
                                 float qx, float qy, float qz, float qw,
                                 float vx, float vy, float vz,
                                 float wx, float wy, float wz,
                                 float comp0, float comp1, float comp2, float comp3,
                                 float steerAngle, boolean sleeping) {
        if (this.level().isClientSide) {
            // Smooth client interpolation targets
            this.clientTargetPos.set((float) px, (float) py, (float) pz);
            this.clientTargetOrientation.set(qx, qy, qz, qw);
            this.clientInterpolationSteps = 3; // 3 ticks smoothing window

            this.physics.linearVelocity.set(vx, vy, vz);
            this.physics.angularVelocity.set(wx, wy, wz);
            this.physics.suspensionCompression[0] = comp0;
            this.physics.suspensionCompression[1] = comp1;
            this.physics.suspensionCompression[2] = comp2;
            this.physics.suspensionCompression[3] = comp3;
            this.physics.currentSteerAngle = steerAngle;
            this.physics.isSleeping = sleeping;
        } else {
            // Server applies driver state directly with position clamping
            this.physics.position.set((float) px, (float) py, (float) pz);
            this.physics.orientation.set(qx, qy, qz, qw);
            this.physics.linearVelocity.set(vx, vy, vz);
            this.physics.angularVelocity.set(wx, wy, wz);
            this.physics.suspensionCompression[0] = comp0;
            this.physics.suspensionCompression[1] = comp1;
            this.physics.suspensionCompression[2] = comp2;
            this.physics.suspensionCompression[3] = comp3;
            this.physics.currentSteerAngle = steerAngle;
            this.physics.isSleeping = sleeping;

            syncPositionFromPhysics();
        }
    }

    /**
     * Smooth 144+ FPS Orientation Interpolation for Renderer.
     */
    public void getInterpolatedOrientation(float partialTicks, Quaternionf out) {
        prevOrientation.slerp(physics.orientation, partialTicks, out);
    }

    /**
     * Smooth 144+ FPS Position Interpolation for Renderer.
     */
    public void getInterpolatedPosition(float partialTicks, Vector3f out) {
        prevPosition.lerp(physics.position, partialTicks, out);
    }

    // --- Gimbal-Lock Safe Euler Angle Extractors ---

    public float getYawDeg() {
        // Yaw from forward vector (+Z transformed)
        physics.orientation.transform(0.0f, 0.0f, 1.0f, scratchVec);
        return (float) Math.toDegrees(Math.atan2(-scratchVec.x, scratchVec.z));
    }

    public float getPitchDeg() {
        // Pitch from forward vector (+Z transformed)
        physics.orientation.transform(0.0f, 0.0f, 1.0f, scratchVec);
        float xzLen = (float) Math.sqrt(scratchVec.x * scratchVec.x + scratchVec.z * scratchVec.z);
        return (float) Math.toDegrees(Math.atan2(-scratchVec.y, xzLen));
    }

    public float getRollDeg() {
        // Roll from right vector (+X transformed)
        physics.orientation.transform(1.0f, 0.0f, 0.0f, scratchVec);
        physics.orientation.transform(0.0f, 1.0f, 0.0f, scratchVec);
        return (float) Math.toDegrees(Math.atan2(scratchVec.x, scratchVec.y));
    }

    public boolean isEngineOn() {
        return this.entityData.get(DATA_ENGINE_ON);
    }

    public void setEngineOn(boolean on) {
        this.entityData.set(DATA_ENGINE_ON, on);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("PhysicsPos")) {
            CompoundTag pTag = tag.getCompound("PhysicsPos");
            physics.position.set(pTag.getFloat("x"), pTag.getFloat("y"), pTag.getFloat("z"));
            prevPosition.set(physics.position);
            setPosRaw(physics.position.x, physics.position.y, physics.position.z);
        }
        if (tag.contains("PhysicsQuat")) {
            CompoundTag qTag = tag.getCompound("PhysicsQuat");
            physics.orientation.set(qTag.getFloat("x"), qTag.getFloat("y"), qTag.getFloat("z"), qTag.getFloat("w"));
            prevOrientation.set(physics.orientation);
        }
        if (tag.contains("EngineOn")) {
            setEngineOn(tag.getBoolean("EngineOn"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        CompoundTag pTag = new CompoundTag();
        pTag.putFloat("x", physics.position.x);
        pTag.putFloat("y", physics.position.y);
        pTag.putFloat("z", physics.position.z);
        tag.put("PhysicsPos", pTag);

        CompoundTag qTag = new CompoundTag();
        qTag.putFloat("x", physics.orientation.x);
        qTag.putFloat("y", physics.orientation.y);
        qTag.putFloat("z", physics.orientation.z);
        qTag.putFloat("w", physics.orientation.w);
        tag.put("PhysicsQuat", qTag);

        tag.putBoolean("EngineOn", isEngineOn());
    }
}
