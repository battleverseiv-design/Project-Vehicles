package com.evgen.cardecor.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Random;

public class VehicleVFX {

    public static final boolean HAS_LODESTONE = false;
    private static final Random RNG = new Random();

    public static void spawnBulletImpactSparks(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 5, 0.04, 0.04, 0.04, 0.10);
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 3, 0.03, 0.03, 0.03, 0.02);
        } else if (level.isClientSide) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.CRIT, x, y, z,
                    (RNG.nextDouble() - 0.5) * 0.15, 0.08 + RNG.nextDouble() * 0.08, (RNG.nextDouble() - 0.5) * 0.15);
            }
        }
    }

    public static void spawnTireSmoke(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 4, 0.05, 0.05, 0.05, 0.03);
            serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 2, 0.03, 0.03, 0.03, 0.02);
        } else {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
                    (RNG.nextDouble() - 0.5) * 0.03, 0.04, (RNG.nextDouble() - 0.5) * 0.03);
            }
        }
    }

    public static void spawnDamageStage1(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
            motX + (RNG.nextDouble() - 0.5) * 0.02, motY, motZ + (RNG.nextDouble() - 0.5) * 0.02);
    }

    public static void spawnDamageStage1(Level level, double x, double y, double z) {
        spawnDamageStage1(level, x, y, z, 0.0, 0.08, 0.0);
    }

    public static void spawnDamageStage2(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z,
            motX + (RNG.nextDouble() - 0.5) * 0.03, motY, motZ + (RNG.nextDouble() - 0.5) * 0.03);
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
            motX + (RNG.nextDouble() - 0.5) * 0.02, motY * 0.8, motZ + (RNG.nextDouble() - 0.5) * 0.02);
        if (RNG.nextFloat() < 0.20F) {
            level.addParticle(ParticleTypes.LAVA, x, y + 0.05, z,
                motX + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8, motZ + (RNG.nextDouble() - 0.5) * 0.03);
        }
    }

    public static void spawnDamageStage2(Level level, double x, double y, double z) {
        spawnDamageStage2(level, x, y, z, 0.0, 0.10, 0.0);
    }

    public static void spawnDamageStage3(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        level.addParticle(ParticleTypes.FLAME, x, y, z,
            motX * 0.4 + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8 + 0.04, motZ * 0.4 + (RNG.nextDouble() - 0.5) * 0.03);
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.08, z,
            motX + (RNG.nextDouble() - 0.5) * 0.04, motY, motZ + (RNG.nextDouble() - 0.5) * 0.04);
    }

    public static void spawnDamageStage3(Level level, double x, double y, double z) {
        spawnDamageStage3(level, x, y, z, 0.0, 0.12, 0.0);
    }

    public static void spawnExhaustPuff(Level level, double x, double y, double z, double motX, double motY, double motZ, float rpm, float curSpeed) {
        if (!level.isClientSide) return;
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z,
            motX + (RNG.nextDouble() - 0.5) * 0.010, motY * 0.5, motZ + (RNG.nextDouble() - 0.5) * 0.010);
        level.addAlwaysVisibleParticle(ParticleTypes.SMOKE, true, x, y, z,
            motX + (RNG.nextDouble() - 0.5) * 0.010, motY * 0.5, motZ + (RNG.nextDouble() - 0.5) * 0.010);
    }

    public static void spawnExhaustFlashAndSmoke(Level level, double x, double y, double z, double fwdX, double fwdZ, boolean isHeavyPop) {
        if (!level.isClientSide) return;
        double backX = -fwdX * (isHeavyPop ? 0.18 : 0.12);
        double backZ = -fwdZ * (isHeavyPop ? 0.18 : 0.12);
        level.addParticle(ParticleTypes.FLAME, x, y, z, backX, 0.02, backZ);
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, backX * 0.5, 0.02, backZ * 0.5);
        }
    }
}
