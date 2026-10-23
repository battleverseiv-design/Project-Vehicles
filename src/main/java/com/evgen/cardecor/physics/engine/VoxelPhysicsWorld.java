package com.evgen.cardecor.physics.engine;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * World Physics Dispatcher: runs 4 sub-steps per game tick (80 Hz) for all active RigidBodyOBBs.
 */
public class VoxelPhysicsWorld {
    public static final int SUB_STEPS = 4;
    public static final float DT = 0.05f / SUB_STEPS;

    private static final Map<Level, VoxelPhysicsWorld> WORLDS = new WeakHashMap<>();

    public static synchronized VoxelPhysicsWorld get(Level level) {
        return WORLDS.computeIfAbsent(level, k -> new VoxelPhysicsWorld());
    }

    private final List<RigidBodyOBB> bodies = new ArrayList<>();
    private long lastTickTime = -1;

    public void addBody(RigidBodyOBB body) {
        if (!bodies.contains(body)) {
            bodies.add(body);
        }
    }

    public void removeBody(RigidBodyOBB body) {
        bodies.remove(body);
    }

    public void tickLevelOnce(Level level) {
        long time = level.getGameTime();
        if (time != lastTickTime) {
            lastTickTime = time;
            stepWorld(level);
        }
    }

    public void stepWorld(Level level) {
        for (int i = 0; i < bodies.size(); i++) {
            RigidBodyOBB b = bodies.get(i);
            b.prevPosition.set(b.position);
            b.prevRotation.set(b.rotation);

            if (Math.abs(b.throttleInput) > 0.01f || Math.abs(b.steerInput) > 0.01f || b.handBrakeInput || b.groundedWheelsCount < 3) {
                b.wakeUp();
            }
        }

        for (int step = 0; step < SUB_STEPS; step++) {
            for (int i = 0; i < bodies.size(); i++) {
                RigidBodyOBB b = bodies.get(i);
                if (!b.isSleeping) {
                    b.stepSubTick(level, b.throttleInput, b.steerInput, b.handBrakeInput);
                }
            }
        }
    }
}
