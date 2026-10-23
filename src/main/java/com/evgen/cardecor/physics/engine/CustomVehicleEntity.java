package com.evgen.cardecor.physics.engine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

/**
 * Base Vehicle Entity connected to VoxelPhysicsWorld & RigidBodyOBB.
 */
public abstract class CustomVehicleEntity extends Entity {

    public final RigidBodyOBB physicsBody;
    public float roll;
    public float prevRoll;

    public CustomVehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.physicsBody = new RigidBodyOBB(1500f, 1.0f, 0.55f, 2.1f);
    }

    protected VoxelPhysicsWorld getPhysicsWorld() {
        return VoxelPhysicsWorld.get(this.level());
    }

    private boolean bodyRegistered = false;

    public void ensurePhysicsBodyRegistered() {
        if (!bodyRegistered && this.level() != null) {
            physicsBody.position.set((float) getX(), (float) getY() + physicsBody.comHeightFromGround, (float) getZ());
            physicsBody.prevPosition.set(physicsBody.position);
            physicsBody.rotation.rotationY(-getYRot() * Mth.DEG_TO_RAD);
            physicsBody.prevRotation.set(physicsBody.rotation);
            getPhysicsWorld().addBody(physicsBody);
            bodyRegistered = true;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bodyRegistered) {
            getPhysicsWorld().removeBody(physicsBody);
            bodyRegistered = false;
        }
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        if (bodyRegistered) {
            getPhysicsWorld().removeBody(physicsBody);
            bodyRegistered = false;
        }
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        ensurePhysicsBodyRegistered();
        this.prevRoll = this.roll;
        super.tick();

        getPhysicsWorld().tickLevelOnce(this.level());

        if (!physicsBody.isSleeping) {
            this.setPos(
                    physicsBody.position.x,
                    physicsBody.position.y - physicsBody.comHeightFromGround,
                    physicsBody.position.z
            );

            this.setYRot(physicsBody.extractMinecraftYawDegrees());
            this.setXRot(physicsBody.extractMinecraftPitchDegrees());
        }
    }

    public Quaternionf getSmoothRotation(float partialTick, Quaternionf dest) {
        return physicsBody.prevRotation.slerp(physicsBody.rotation, partialTick, dest);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
