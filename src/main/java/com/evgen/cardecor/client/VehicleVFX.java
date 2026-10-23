package com.evgen.cardecor.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.Color;
import java.util.Random;

public class VehicleVFX {

    public static final boolean HAS_LODESTONE = ModList.get().isLoaded("lodestone");
    private static final Random RNG = new Random();

    /**
     * Bullet kinetic impact sparks (Micro-fine, crisp, needle-thin metal ricochet sparks)
     */
    public static void spawnBulletImpactSparks(Level level, double x, double y, double z) {
        if (HAS_LODESTONE && level.isClientSide) {
            // 1. Crisp micro-fine needle sparks (much smaller and tighter!)
            for (int i = 0; i < 7; i++) {
                double vx = (RNG.nextDouble() - 0.5) * 0.18;
                double vy = 0.06 + RNG.nextDouble() * 0.16;
                double vz = (RNG.nextDouble() - 0.5) * 0.18;

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.08F, 0.01F).build())
                    .setTransparencyData(GenericParticleData.create(1.0F, 0.0F).build())
                    .setColorData(ColorParticleData.create(new Color(255, 240, 160), new Color(255, 110, 15)).build())
                    .setLifetime(6 + RNG.nextInt(5))
                    .addMotion(vx, vy, vz)
                    .spawn(level, x, y, z);
            }

            // 2. Micro impact flash smoke
            for (int i = 0; i < 2; i++) {
                WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.06F, 0.20F).build())
                    .setTransparencyData(GenericParticleData.create(0.50F, 0.0F).build())
                    .setColorData(ColorParticleData.create(new Color(60, 60, 60), new Color(25, 25, 25)).build())
                    .setLifetime(10)
                    .addMotion((RNG.nextDouble() - 0.5) * 0.03, 0.02 + RNG.nextDouble() * 0.02, (RNG.nextDouble() - 0.5) * 0.03)
                    .spawn(level, x, y, z);
            }
        } else if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 5, 0.04, 0.04, 0.04, 0.10);
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 3, 0.03, 0.03, 0.03, 0.02);
        } else if (level.isClientSide) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.CRIT, x, y, z,
                    (RNG.nextDouble() - 0.5) * 0.15, 0.08 + RNG.nextDouble() * 0.08, (RNG.nextDouble() - 0.5) * 0.15);
            }
        }
    }

    /**
     * Tire puncture white depressurization smoke stream
     */
    public static void spawnTireSmoke(Level level, double x, double y, double z) {
        if (HAS_LODESTONE && level.isClientSide) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setScaleData(GenericParticleData.create(0.18F, 0.45F).build())
                .setTransparencyData(GenericParticleData.create(0.65F, 0.0F).build())
                .setColorData(ColorParticleData.create(new Color(230, 230, 230), new Color(180, 180, 180)).build())
                .setLifetime(20)
                .addMotion((RNG.nextDouble() - 0.5) * 0.05, 0.04 + RNG.nextDouble() * 0.03, (RNG.nextDouble() - 0.5) * 0.05)
                .spawn(level, x, y, z);
        } else if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 4, 0.05, 0.05, 0.05, 0.03);
            serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 2, 0.03, 0.03, 0.03, 0.02);
        } else {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
                    (RNG.nextDouble() - 0.5) * 0.03, 0.04, (RNG.nextDouble() - 0.5) * 0.03);
            }
        }
    }

    /**
     * Damage Stage 1 (65% - 35% HP): Radiator steam & light vapor wisps
     */
    public static void spawnDamageStage1(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        if (HAS_LODESTONE && level.isClientSide) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setScaleData(GenericParticleData.create(0.16F, 0.45F).build())
                .setTransparencyData(GenericParticleData.create(0.40F, 0.0F).build())
                .setColorData(ColorParticleData.create(new Color(240, 240, 240), new Color(190, 190, 190)).build())
                .setLifetime(16)
                .addMotion(motX + (RNG.nextDouble() - 0.5) * 0.02, motY, motZ + (RNG.nextDouble() - 0.5) * 0.02)
                .spawn(level, x, y, z);
        } else {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
                motX + (RNG.nextDouble() - 0.5) * 0.02, motY, motZ + (RNG.nextDouble() - 0.5) * 0.02);
        }
    }

    public static void spawnDamageStage1(Level level, double x, double y, double z) {
        spawnDamageStage1(level, x, y, z, 0.0, 0.08, 0.0);
    }

    /**
     * Damage Stage 2 (35% - 15% HP): Dark smoke & glowing engine embers
     */
    public static void spawnDamageStage2(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        if (HAS_LODESTONE && level.isClientSide) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setScaleData(GenericParticleData.create(0.22F, 0.70F).build())
                .setTransparencyData(GenericParticleData.create(0.65F, 0.0F).build())
                .setColorData(ColorParticleData.create(new Color(40, 40, 40), new Color(15, 15, 15)).build())
                .setLifetime(18)
                .addMotion(motX + (RNG.nextDouble() - 0.5) * 0.03, motY, motZ + (RNG.nextDouble() - 0.5) * 0.03)
                .spawn(level, x, y, z);

            if (RNG.nextFloat() < 0.25F) {
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.10F, 0.01F).build())
                    .setTransparencyData(GenericParticleData.create(1.0F, 0.0F).build())
                    .setColorData(ColorParticleData.create(new Color(255, 170, 20), new Color(220, 40, 0)).build())
                    .setLifetime(12)
                    .addMotion(motX * 0.5 + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8 + 0.03, motZ * 0.5 + (RNG.nextDouble() - 0.5) * 0.03)
                    .spawn(level, x, y + 0.05, z);
            }
        } else {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z,
                motX + (RNG.nextDouble() - 0.5) * 0.03, motY, motZ + (RNG.nextDouble() - 0.5) * 0.03);
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
                motX + (RNG.nextDouble() - 0.5) * 0.02, motY * 0.8, motZ + (RNG.nextDouble() - 0.5) * 0.02);
            if (RNG.nextFloat() < 0.20F) {
                level.addParticle(ParticleTypes.LAVA, x, y + 0.05, z,
                    motX + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8, motZ + (RNG.nextDouble() - 0.5) * 0.03);
            }
        }
    }

    public static void spawnDamageStage2(Level level, double x, double y, double z) {
        spawnDamageStage2(level, x, y, z, 0.0, 0.10, 0.0);
    }

    /**
     * Damage Stage 3 (< 15% HP): Roaring Fire Sparks and Thick Smoke
     */
    public static void spawnDamageStage3(Level level, double x, double y, double z, double motX, double motY, double motZ) {
        if (HAS_LODESTONE && level.isClientSide) {
            WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                .setScaleData(GenericParticleData.create(0.18F, 0.02F).build())
                .setTransparencyData(GenericParticleData.create(1.0F, 0.0F).build())
                .setColorData(ColorParticleData.create(new Color(255, 180, 40), new Color(240, 50, 0)).build())
                .setLifetime(12)
                .addMotion(motX * 0.4 + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8 + 0.05, motZ * 0.4 + (RNG.nextDouble() - 0.5) * 0.03)
                .spawn(level, x, y, z);

            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setScaleData(GenericParticleData.create(0.28F, 0.80F).build())
                .setTransparencyData(GenericParticleData.create(0.85F, 0.0F).build())
                .setColorData(ColorParticleData.create(new Color(25, 25, 25), new Color(5, 5, 5)).build())
                .setLifetime(20)
                .addMotion(motX + (RNG.nextDouble() - 0.5) * 0.03, motY, motZ + (RNG.nextDouble() - 0.5) * 0.03)
                .spawn(level, x, y + 0.10, z);
        } else {
            level.addParticle(ParticleTypes.FLAME, x, y, z,
                motX * 0.4 + (RNG.nextDouble() - 0.5) * 0.03, motY * 0.8 + 0.04, motZ * 0.4 + (RNG.nextDouble() - 0.5) * 0.03);
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.08, z,
                motX + (RNG.nextDouble() - 0.5) * 0.04, motY, motZ + (RNG.nextDouble() - 0.5) * 0.04);
        }
    }

    public static void spawnDamageStage3(Level level, double x, double y, double z) {
        spawnDamageStage3(level, x, y, z, 0.0, 0.12, 0.0);
    }

    /**
     * RPM-dependent exhaust puff (Tight, cohesive, compact dark smoke puff)
     */
    public static void spawnExhaustPuff(Level level, double x, double y, double z, double motX, double motY, double motZ, float rpm, float curSpeed) {
        if (!level.isClientSide) return;

        if (HAS_LODESTONE) {
            float rpmRatio = Math.max(0.0F, Math.min(1.0F, (rpm - 800.0F) / 4800.0F));

            // Compact, fast-dissipating, neat exhaust puff
            float startScale = 0.05F + rpmRatio * 0.06F;
            float endScale   = 0.14F + rpmRatio * 0.16F;
            float startAlpha = 0.70F;

            int darkVal = (int) (55 - rpmRatio * 32);
            Color c1 = new Color(darkVal, darkVal, darkVal);
            Color c2 = new Color(Math.max(10, darkVal - 18), Math.max(10, darkVal - 18), Math.max(10, darkVal - 18));

            int lifetime = 10 + (int) (rpmRatio * 8);

            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setScaleData(GenericParticleData.create(startScale, endScale).build())
                .setTransparencyData(GenericParticleData.create(startAlpha, 0.0F).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setLifetime(lifetime)
                .addMotion(motX + (RNG.nextDouble() - 0.5) * 0.005, motY + RNG.nextDouble() * 0.005, motZ + (RNG.nextDouble() - 0.5) * 0.005)
                .spawn(level, x, y, z);
        } else {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z,
                motX + (RNG.nextDouble() - 0.5) * 0.010, motY * 0.5, motZ + (RNG.nextDouble() - 0.5) * 0.010);
            level.addAlwaysVisibleParticle(ParticleTypes.SMOKE, true, x, y, z,
                motX + (RNG.nextDouble() - 0.5) * 0.010, motY * 0.5, motZ + (RNG.nextDouble() - 0.5) * 0.010);
        }
    }

    /**
     * Lodestone Authentic Exhaust Backfire Flame Tongue & Volumetric Dark Smoke Puff
     */
    public static void spawnExhaustFlashAndSmoke(Level level, double x, double y, double z, double fwdX, double fwdZ, boolean isHeavyPop) {
        if (!level.isClientSide) return;

        double backX = -fwdX * (isHeavyPop ? 0.18 : 0.12);
        double backZ = -fwdZ * (isHeavyPop ? 0.18 : 0.12);

        if (HAS_LODESTONE) {
            // 1. Soft Gas Combustion Flame Tongue (Lodestone Wisp Particle - Zero sharp sparks!)
            int wispCount = isHeavyPop ? 2 : 1;
            for (int i = 0; i < wispCount; i++) {
                double speedMult = 0.85 + i * 0.35;
                float startScale = isHeavyPop ? 0.09F : 0.07F;
                float endScale   = isHeavyPop ? 0.20F : 0.15F;

                Color cCore = new Color(255, 235, 140);
                Color cEdge = new Color(245, 80, 5);

                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(startScale, endScale).build())
                    .setTransparencyData(GenericParticleData.create(0.95F, 0.0F).build())
                    .setColorData(ColorParticleData.create(cCore, cEdge).build())
                    .setLifetime(3 + RNG.nextInt(2))
                    .addMotion(backX * speedMult, 0.01 + RNG.nextDouble() * 0.010, backZ * speedMult)
                    .spawn(level, x, y, z);
            }

            // 2. Compact Dark Exhaust Smoke Puff
            int smokeCount = isHeavyPop ? 2 : 1;
            for (int i = 0; i < smokeCount; i++) {
                double smX = backX * (0.40 + i * 0.15) + (RNG.nextDouble() - 0.5) * 0.015;
                double smY = 0.015 + RNG.nextDouble() * 0.015;
                double smZ = backZ * (0.40 + i * 0.15) + (RNG.nextDouble() - 0.5) * 0.015;

                float sScale = 0.06F + i * 0.02F;
                float eScale = isHeavyPop ? (0.20F + i * 0.04F) : (0.15F + i * 0.03F);

                int dVal = 40 + RNG.nextInt(15);
                Color sc1 = new Color(dVal, dVal, dVal);
                Color sc2 = new Color(Math.max(10, dVal - 22), Math.max(10, dVal - 22), Math.max(10, dVal - 22));

                WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setScaleData(GenericParticleData.create(sScale, eScale).build())
                    .setTransparencyData(GenericParticleData.create(0.70F, 0.0F).build())
                    .setColorData(ColorParticleData.create(sc1, sc2).build())
                    .setLifetime(10 + RNG.nextInt(5))
                    .addMotion(smX, smY, smZ)
                    .spawn(level, x, y, z);
            }
        } else {
            // Vanilla fallback if Lodestone is not loaded
            level.addParticle(ParticleTypes.FLAME, x, y, z, backX, 0.02, backZ);
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, backX * 0.5, 0.02, backZ * 0.5);
            }
        }
    }
}
