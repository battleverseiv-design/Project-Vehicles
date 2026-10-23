package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * High-fidelity 9-layer engine sound controller with acoustic Doppler effect for bystanders.
 */
public class VehicleEngineSoundManager {

    private static final int ENGINE_LAYERS = 5;

    private final DrivableVehicleEntity vehicle;
    private final VehicleEngineSoundInstance[] engineLayers = new VehicleEngineSoundInstance[ENGINE_LAYERS];
    private final float[] smoothedEngineGains = new float[ENGINE_LAYERS];
    private String currentEngineId = null;

    private VehicleEngineSoundInstance tireDirtSound;
    private VehicleEngineSoundInstance tireRoadSound;
    private VehicleEngineSoundInstance tireSnowSound;
    private float smoothedDirtRatio = 0.0F;
    private float smoothedRoadRatio = 1.0F;
    private float smoothedSnowRatio = 0.0F;

    public static float masterVolume = 1.00F;
    public static float insideVolume = 1.00F;  // 100% full raw loudness in cabin
    public static float outsideVolume = 1.00F; // Full raw loudness outside

    private VehicleEngineSoundInstance brakeHardSound;
    private VehicleEngineSoundInstance brakeSlowSound;
    public static float hornVolume = 1.00F;

    private float smoothedRPM = 800.0F;
    private float smoothedDoppler = 1.0F;
    private float lastThrottle = 0.0F;
    private int kickdownCooldown = 0;
    private int brakeCooldown = 0;
    private int lastGear = 1;

    private static final java.util.Map<String, SoundEvent> SOUND_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public VehicleEngineSoundManager(DrivableVehicleEntity vehicle) {
        this.vehicle = vehicle;
    }

    public static SoundEvent getSound(String soundId) {
        if (soundId == null || soundId.trim().isEmpty()) {
            return ModSounds.BLUE_SEDAN_IDLE.get();
        }
        return SOUND_CACHE.computeIfAbsent(soundId.trim(), id -> {
            try {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                if (rl != null) {
                    return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(rl)
                        .orElseGet(() -> SoundEvent.createVariableRangeEvent(rl));
                }
            } catch (Exception ignored) {}
            return ModSounds.BLUE_SEDAN_IDLE.get();
        });
    }

    private static SoundEvent getEngineLayerSound(com.evgen.cardecor.pack.EngineDefinition eng, int index) {
        if (eng == null || eng.sounds == null || eng.sounds.layers == null) {
            return getSedanSound(index);
        }
        String id = switch (index) {
            case 0 -> eng.sounds.layers.idle;
            case 1 -> eng.sounds.layers.low;
            case 2 -> eng.sounds.layers.mid;
            case 3 -> eng.sounds.layers.high;
            case 4 -> eng.sounds.layers.max;
            default -> eng.sounds.layers.idle;
        };
        return getSound(id);
    }

    private static SoundEvent getSedanSound(int i) {
        return switch (i) {
            case 0 -> ModSounds.BLUE_SEDAN_IDLE.get();
            case 1 -> ModSounds.BLUE_SEDAN_LOW.get();
            case 2 -> ModSounds.BLUE_SEDAN_MID.get();
            case 3 -> ModSounds.BLUE_SEDAN_HIGH.get();
            case 4 -> ModSounds.BLUE_SEDAN_MAX.get();
            default -> ModSounds.BLUE_SEDAN_IDLE.get();
        };
    }

    public static float getDistanceAttenuation(double dist) {
        if (dist <= 3.0) return 1.0F;
        if (dist <= 12.0) {
            float t = (float) ((dist - 3.0) / (12.0 - 3.0));
            return Mth.lerp(t, 1.0F, 0.60F);
        }
        if (dist <= 24.0) {
            float t = (float) ((dist - 12.0) / (24.0 - 12.0));
            return Mth.lerp(t, 0.60F, 0.22F);
        }
        if (dist <= 38.0) {
            float t = (float) ((dist - 24.0) / (38.0 - 24.0));
            return Mth.lerp(t, 0.22F, 0.03F);
        }
        if (dist <= 46.0) {
            float t = (float) ((dist - 38.0) / (46.0 - 38.0));
            return Mth.lerp(t, 0.03F, 0.0F);
        }
        return 0.0F;
    }

    public static float calculateDopplerShift(DrivableVehicleEntity vehicle, Entity listener) {
        // Doppler is completely removed per user request (no artificial pitch warping!)
        return 1.0F;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        boolean vehicleAlive = vehicle.isAlive() && !vehicle.isRemoved();
        double dist = (mc.player != null) ? vehicle.distanceTo(mc.player) : 0.0;
        if (dist > 105.0) vehicleAlive = false;

        if (!vehicleAlive) {
            stopAll();
            return;
        }

        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(vehicle.getVehicleId());
        String engineType = (def != null && def.sounds != null) ? def.sounds.engineType : (def != null && def.engine != null ? def.engine.type : "legacy_pickup");
        boolean is5LayerEngine = "i6".equalsIgnoreCase(engineType) || "5_layer".equalsIgnoreCase(engineType) || "sedan".equalsIgnoreCase(engineType);

        float targetRpm = vehicle.getEntityData().get(DrivableVehicleEntity.RPM);
        if (!vehicle.isEngineOn()) targetRpm = 0.0F;
        // Asymmetric tachometer smoothing: crisp, punchy throttle climb + smooth flywheel descent
        float rpmSmoothing = (targetRpm > smoothedRPM) ? 0.35F : 0.18F;
        smoothedRPM = Mth.lerp(rpmSmoothing, smoothedRPM, targetRpm);

        float curSpeedKmh = Math.abs(vehicle.getEntityData().get(DrivableVehicleEntity.SPEED)) * 3.6F;
        int currentGear = vehicle.getEntityData().get(DrivableVehicleEntity.GEAR);

        if (kickdownCooldown > 0) kickdownCooldown--;

        boolean isPassenger = (mc.player != null && vehicle.hasPassenger(mc.player));
        boolean isDriver = isPassenger && vehicle.getSeatForPlayer(mc.player) == 0;
        float currentThrottle = 0.0F;
        if (isDriver) {
            if (mc.options.keyUp.isDown()) currentThrottle = 1.0F;
            else if (mc.options.keyDown.isDown()) currentThrottle = -1.0F;
        } else {
            currentThrottle = curSpeedKmh > 5.0F ? 0.8F : 0.0F;
        }

        masterVolume = VehicleSoundConfig.masterVolume;
        insideVolume = VehicleSoundConfig.insideVolume;

        // No distance drop when sitting inside the vehicle!
        float distFactor = isPassenger ? 1.0F : getDistanceAttenuation(dist);
        float envGain = isPassenger ? insideVolume : outsideVolume;

        // Kickdown growl for pickup
        if (!is5LayerEngine && kickdownCooldown == 0 && currentThrottle >= 0.9F && lastThrottle <= 0.20F && vehicle.isEngineOn()) {
            kickdownCooldown = 40;
            mc.getSoundManager().play(new SimpleSoundInstance(
                ModSounds.ENGINE_KICKDOWN.get(),
                SoundSource.NEUTRAL,
                envGain * masterVolume * VehicleSoundConfig.engineVolume * 1.15F * distFactor,
                1.0F,
                RandomSource.create(),
                vehicle.getX(), vehicle.getY(), vehicle.getZ()
            ));
        }
        lastThrottle = currentThrottle;

        // Dynamic Gear Shift Sound (Crisp, punchy, zero-lag clunk & click)
        if (currentGear != lastGear && vehicle.isEngineOn() && (curSpeedKmh > 2.5F || Math.abs(currentThrottle) > 0.1F)) {
            mc.getSoundManager().play(new SimpleSoundInstance(
                ModSounds.GEAR_SHIFT.get(),
                SoundSource.NEUTRAL,
                masterVolume * VehicleSoundConfig.gearShiftVolume * 1.35F * distFactor,
                0.96F + (float) (Math.random() * 0.08),
                RandomSource.create(),
                vehicle.getX(), vehicle.getY(), vehicle.getZ()
            ));
        }
        lastGear = currentGear;

        float rawDoppler = calculateDopplerShift(vehicle, mc.player);
        float targetDoppler = 1.0F + (rawDoppler - 1.0F) * VehicleSoundConfig.dopplerIntensity;
        smoothedDoppler = Mth.lerp(0.20F, smoothedDoppler, targetDoppler);

        // Engine sound loops only active when engine is on
        if (vehicle.isEngineOn()) {
            tickModularEngine(mc, curSpeedKmh, currentGear, currentThrottle, isDriver, distFactor, envGain);
        } else {
            stopEngineOnly();
        }

        // Dynamic Brake Sounds and Rolling Tires ALWAYS tick even when engine is off!
        tickBrakeSounds(mc, curSpeedKmh, isDriver, distFactor, envGain);
        tickTireRollSounds(mc, curSpeedKmh, distFactor, envGain);
    }


    private void tickModularEngine(Minecraft mc, float kmh, int gear, float throttle, boolean isDriver, float distFactor, float envGain) {
        String installedId = vehicle.getInstalledEngine();
        if (currentEngineId == null || !currentEngineId.equals(installedId)) {
            // Engine was swapped! Stop previous layers cleanly
            stopEngineOnly();
            currentEngineId = installedId;
        }

        com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(installedId);
        if (eng == null) {
            eng = com.evgen.cardecor.pack.EngineRegistry.getFallback();
        }

        // Kickdown growl under sudden full throttle
        if (kickdownCooldown == 0 && throttle >= 0.9F && lastThrottle <= 0.20F && vehicle.isEngineOn()) {
            kickdownCooldown = 40;
            String kickSnd = (eng.sounds != null && eng.sounds.kickdown != null && !eng.sounds.kickdown.isEmpty())
                ? eng.sounds.kickdown : "cardecor:engine_kickdown";
            mc.getSoundManager().play(new SimpleSoundInstance(
                getSound(kickSnd),
                SoundSource.NEUTRAL,
                envGain * masterVolume * VehicleSoundConfig.engineVolume * 1.15F * distFactor,
                1.0F,
                RandomSource.create(),
                vehicle.getX(), vehicle.getY(), vehicle.getZ()
            ));
        }

        // Keep all 5 engine layer instances active
        for (int i = 0; i < ENGINE_LAYERS; i++) {
            if (engineLayers[i] == null || !mc.getSoundManager().isActive(engineLayers[i])) {
                engineLayers[i] = new VehicleEngineSoundInstance(vehicle, getEngineLayerSound(eng, i));
                mc.getSoundManager().play(engineLayers[i]);
            }
        }

        float idleRpm      = (float) ((eng.physics != null) ? eng.physics.idleRpm : 850.0F);
        float redlineRpm   = (float) ((eng.physics != null) ? eng.physics.redlineRpm : 6500.0F);
        float rpmIdleToLow = (eng.sounds != null && eng.sounds.transitions != null) ? eng.sounds.transitions.idleToLow : 1600.0F;
        float rpmLowToMid  = (eng.sounds != null && eng.sounds.transitions != null) ? eng.sounds.transitions.lowToMid  : 2800.0F;
        float rpmMidToHigh = (eng.sounds != null && eng.sounds.transitions != null) ? eng.sounds.transitions.midToHigh : 4200.0F;
        float rpmHighToMax = (eng.sounds != null && eng.sounds.transitions != null) ? eng.sounds.transitions.highToMax : 5400.0F;
        float packMasterVol = (eng.sounds != null) ? eng.sounds.masterVolume : 1.0F;
        float packPitchMult = (eng.sounds != null) ? eng.sounds.pitchMultiplier : 1.0F;
        boolean camshaftLope = (eng.sounds != null && eng.sounds.camshaftLope);

        float rpm = Mth.clamp(smoothedRPM, idleRpm, redlineRpm + 500.0F);

        float targetIdle = 0.0F;
        float targetLow  = 0.0F;
        float targetMid  = 0.0F;
        float targetHigh = 0.0F;
        float targetMax  = 0.0F;

        // If unattended without driver, strictly 100% idle loop!
        if (!vehicle.hasDriver()) {
            targetIdle = 1.0F;
        } else if (kmh < 1.0F && Math.abs(throttle) < 0.05F) {
            targetIdle = 1.0F;
        } else if (rpm < rpmIdleToLow) {
            float t = Mth.clamp((rpm - idleRpm) / Math.max(100.0F, rpmIdleToLow - idleRpm), 0.0F, 1.0F);
            float angle = t * (float) (Math.PI / 2.0);
            targetIdle = (float) Math.cos(angle);
            targetLow  = (float) Math.sin(angle);
        } else if (rpm < rpmLowToMid) {
            float t = (rpm - rpmIdleToLow) / Math.max(100.0F, rpmLowToMid - rpmIdleToLow);
            float angle = t * (float) (Math.PI / 2.0);
            targetLow = (float) Math.cos(angle);
            targetMid = (float) Math.sin(angle);
        } else if (rpm < rpmMidToHigh) {
            float t = (rpm - rpmLowToMid) / Math.max(100.0F, rpmMidToHigh - rpmLowToMid);
            float angle = t * (float) (Math.PI / 2.0);
            targetMid  = (float) Math.cos(angle);
            targetHigh = (float) Math.sin(angle);
        } else if (rpm < rpmHighToMax) {
            float t = (rpm - rpmMidToHigh) / Math.max(100.0F, rpmHighToMax - rpmMidToHigh);
            float angle = t * (float) (Math.PI / 2.0);
            targetHigh = (float) Math.cos(angle);
            targetMax  = (float) Math.sin(angle);
        } else {
            targetMax = 1.0F;
        }

        // Off-throttle overrun / coasting attenuation:
        // When foot is off the gas pedal (throttle < 0.05), attenuate loud screaming exhaust layers
        if (Math.abs(throttle) < 0.05F && vehicle.hasDriver()) {
            targetMid *= 0.20F;
            targetHigh = 0.0F;
            targetMax = 0.0F;
            targetIdle = Math.max(targetIdle, 0.70F);
        }

        // Physical frequency center points for 5-layer engine
        float cIdle = idleRpm;
        float cLow  = (idleRpm + rpmLowToMid) * 0.50F;
        float cMid  = (rpmLowToMid + rpmMidToHigh) * 0.50F;
        float cHigh = (rpmMidToHigh + rpmHighToMax) * 0.50F;
        float cMax  = (rpmHighToMax + redlineRpm) * 0.50F;

        // Dynamic throttle load pitch response
        float loadPitch = 1.0F - Mth.clamp(throttle, 0.0F, 1.0F) * (camshaftLope ? 0.030F : 0.025F);

        // Continuous physical tonality tracking for seamless layer blend
        float pIdle = Mth.clamp(rpm / cIdle, 0.70F, 1.70F) * smoothedDoppler * packPitchMult * loadPitch;
        if (camshaftLope && kmh < 1.0F && Math.abs(throttle) < 0.05F) {
            double timeSec = (System.currentTimeMillis() % 1000000L) / 1000.0;
            float lopeFlutter = (float) (Math.sin(timeSec * (2.0 * Math.PI * 2.35)) * 0.040 + Math.sin(timeSec * (2.0 * Math.PI * 4.70) + 0.6) * 0.018);
            pIdle *= (1.0F + lopeFlutter);
        }

        float pLow  = Mth.clamp(rpm / cLow,  0.65F, 1.75F) * smoothedDoppler * packPitchMult * loadPitch;
        float pMid  = Mth.clamp(rpm / cMid,  0.65F, 1.75F) * smoothedDoppler * packPitchMult * loadPitch;
        float pHigh = Mth.clamp(rpm / cHigh, 0.65F, 1.75F) * smoothedDoppler * packPitchMult * loadPitch;
        float pMax  = Mth.clamp(rpm / cMax,  0.65F, 1.80F) * smoothedDoppler * packPitchMult * loadPitch;

        float[] targets = new float[]{targetIdle, targetLow, targetMid, targetHigh, targetMax};
        float[] pitches = new float[]{pIdle, pLow, pMid, pHigh, pMax};
        float dist = (mc.player != null) ? (float) vehicle.distanceTo(mc.player) : 0.0F;
        float distMuffle = Mth.clamp((dist - 6.0F) / 34.0F, 0.0F, 1.0F);

        for (int i = 0; i < ENGINE_LAYERS; i++) {
            smoothedEngineGains[i] = Mth.lerp(0.22F, smoothedEngineGains[i], targets[i]);

            if (engineLayers[i] != null) {
                float layerDistMod = (i >= 3) ? (1.0F - distMuffle * 0.45F) : (1.0F + distMuffle * 0.15F);
                engineLayers[i].targetVolume = smoothedEngineGains[i] * distFactor * envGain * masterVolume * packMasterVol * VehicleSoundConfig.engineVolume * 1.30F * layerDistMod;
                engineLayers[i].targetPitch  = Mth.clamp(pitches[i] * (1.0F - distMuffle * 0.025F), 0.60F, 1.95F);
            }
        }
    }

    
    private void tickTireRollSounds(Minecraft mc, float kmh, float distFactor, float envGain) {
        byte wheelMask = vehicle.getEntityData().get(DrivableVehicleEntity.INSTALLED_WHEELS);
        byte tireStatus = vehicle.getEntityData().get(DrivableVehicleEntity.TIRE_STATUS);
        float steerAngle = vehicle.getEntityData().get(DrivableVehicleEntity.STEER);
        float pitch = vehicle.getEntityData().get(DrivableVehicleEntity.PITCH);
        float roll = vehicle.getEntityData().get(DrivableVehicleEntity.ROLL);

        // 1. Calculate dynamic physical contact patch for each wheel
        float totalContactPatch = 0.0F;
        int installedWheels = 0;

        for (int i = 0; i < 4; i++) {
            if ((wheelMask & (1 << i)) != 0) {
                installedWheels++;
                boolean isFlat = (tireStatus & (1 << i)) != 0;
                
                // Normal tire nominal contact patch = 1.0.
                // Flat / deflated tire flattens into a massive contact patch (+140% rubber spread!)
                float wheelPatch = isFlat ? 2.4F : 1.0F;

                // Dynamic Pitch Load Transfer (Braking shifts load forward FL=0, FR=1; Accel shifts rearward RL=2, RR=3)
                if (i == 0 || i == 1) { // Front wheels
                    wheelPatch += Mth.clamp(-pitch * 0.05F, -0.20F, 0.40F);
                } else { // Rear wheels
                    wheelPatch += Mth.clamp(pitch * 0.05F, -0.20F, 0.40F);
                }

                // Dynamic Roll Load Transfer (Outer wheels compress under lateral Gs)
                if (i == 0 || i == 2) { // Left wheels
                    wheelPatch += Mth.clamp(-roll * 0.04F, -0.25F, 0.45F);
                } else { // Right wheels
                    wheelPatch += Mth.clamp(roll * 0.04F, -0.25F, 0.45F);
                }

                totalContactPatch += Math.max(0.2F, wheelPatch);
            }
        }

        // Stop and destroy instances completely when car is stopped (< 1.5 km/h) or in the air!
        if (kmh < 1.5F || !vehicle.onGround() || installedWheels == 0) {
            if (tireDirtSound != null) {
                mc.getSoundManager().stop(tireDirtSound);
                tireDirtSound = null;
            }
            if (tireRoadSound != null) {
                mc.getSoundManager().stop(tireRoadSound);
                tireRoadSound = null;
            }
            return;
        }

        // Nominal standard 4-wheel contact patch reference is 4.0
        float contactPatchRatio = totalContactPatch / 4.0F;

        // Dynamic Lateral Scrubbing: turning wheels at speed increases contact scrub & friction sound!
        float steerScrub = Math.abs(steerAngle) * Mth.clamp(kmh / 40.0F, 0.0F, 1.2F) * 0.50F;
        float effectiveContactFactor = Mth.clamp(contactPatchRatio + steerScrub, 0.3F, 2.6F);

        // Manage active instances only while moving
        if (tireDirtSound == null || !mc.getSoundManager().isActive(tireDirtSound)) {
            tireDirtSound = new VehicleEngineSoundInstance(vehicle, ModSounds.TIRE_ROLL_DIRT.get());
            mc.getSoundManager().play(tireDirtSound);
        }
        if (tireRoadSound == null || !mc.getSoundManager().isActive(tireRoadSound)) {
            tireRoadSound = new VehicleEngineSoundInstance(vehicle, ModSounds.TIRE_ROLL_ROAD.get());
            mc.getSoundManager().play(tireRoadSound);
        }
        if (tireSnowSound == null || !mc.getSoundManager().isActive(tireSnowSound)) {
            tireSnowSound = new VehicleEngineSoundInstance(vehicle, ModSounds.TIRE_ROLL_SNOW.get());
            mc.getSoundManager().play(tireSnowSound);
        }

        // Surface detection under vehicle
        net.minecraft.core.BlockPos belowPos = vehicle.blockPosition().below();
        net.minecraft.world.level.block.state.BlockState state = vehicle.level().getBlockState(belowPos);
        if (state.isAir()) {
            belowPos = vehicle.blockPosition();
            state = vehicle.level().getBlockState(belowPos);
        }

        boolean isSnow = isSnowSurface(state);
        boolean isOffroad = !isSnow && isOffroadSurface(state);
        float targetSnow = isSnow ? 1.0F : 0.0F;
        float targetDirt = isOffroad ? 1.0F : 0.0F;
        float targetRoad = (!isSnow && !isOffroad) ? 1.0F : 0.0F;
        smoothedSnowRatio += (targetSnow - smoothedSnowRatio) * 0.18F;
        smoothedDirtRatio += (targetDirt - smoothedDirtRatio) * 0.18F;
        smoothedRoadRatio += (targetRoad - smoothedRoadRatio) * 0.18F;

        float rollSpeedRatio = Mth.clamp(kmh / 65.0F, 0.0F, 1.0F);
        // Base roll volume dynamically scaled by effective contact patch factor!
        float baseRollVol = rollSpeedRatio * 1.55F * effectiveContactFactor;

        // As contact patch widens, tire rumble deepens slightly in pitch (richer low-frequency road rumble)
        float patchPitchMod = 1.0F / (float) Math.pow(Math.max(0.6F, effectiveContactFactor), 0.20);
        float rollPitch = Mth.clamp((0.85F + rollSpeedRatio * 0.35F) * patchPitchMod, 0.70F, 1.30F) * smoothedDoppler;

        if (tireDirtSound != null) {
            tireDirtSound.targetVolume = baseRollVol * smoothedDirtRatio * distFactor * envGain * masterVolume * VehicleSoundConfig.tireRollVolume * 1.50F;
            tireDirtSound.targetPitch  = rollPitch;
        }
        if (tireRoadSound != null) {
            tireRoadSound.targetVolume = baseRollVol * smoothedRoadRatio * distFactor * envGain * masterVolume * VehicleSoundConfig.tireRollVolume * 1.45F;
            tireRoadSound.targetPitch  = rollPitch;
        }
        if (tireSnowSound != null) {
            tireSnowSound.targetVolume = baseRollVol * smoothedSnowRatio * distFactor * envGain * masterVolume * VehicleSoundConfig.tireRollVolume * 1.55F;
            tireSnowSound.targetPitch  = rollPitch;
        }
    }

    public static boolean isSnowSurface(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return false;
        return state.is(net.minecraft.tags.BlockTags.SNOW) ||
               state.is(net.minecraft.tags.BlockTags.ICE) ||
               state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW) ||
               state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK) ||
               state.is(net.minecraft.world.level.block.Blocks.SNOW);
    }

    public static boolean isOffroadSurface(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return false;
        if (state.is(net.minecraft.tags.BlockTags.DIRT) ||
            state.is(net.minecraft.tags.BlockTags.SAND) ||
            state.is(net.minecraft.world.level.block.Blocks.GRAVEL) ||
            state.is(net.minecraft.world.level.block.Blocks.MUD) ||
            state.is(net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS) ||
            state.is(net.minecraft.world.level.block.Blocks.SOUL_SAND) ||
            state.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL) ||
            state.is(net.minecraft.world.level.block.Blocks.CLAY) ||
            state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH) ||
            state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
            state.is(net.minecraft.world.level.block.Blocks.MYCELIUM) ||
            state.is(net.minecraft.world.level.block.Blocks.PODZOL) ||
            state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) ||
            state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)) {
            return true;
        }
        return false;
    }

    
    private void tickBrakeSounds(Minecraft mc, float curSpeedKmh, boolean isDriver, float distFactor, float envGain) {
        float vehicleSpeed = vehicle.getEntityData().get(DrivableVehicleEntity.SPEED);
        boolean isBraking = false;

        // True directional braking: ONLY brake if pressing opposite key while moving or handbrake!
        if (isDriver && vehicle.onGround()) {
            if (vehicleSpeed > 0.4F && mc.options.keyDown.isDown()) {
                isBraking = true; // Moving forward and pressing brake (S)
            } else if (vehicleSpeed < -0.4F && mc.options.keyUp.isDown()) {
                isBraking = true; // Moving in reverse and pressing forward/brake (W)
            } else if (Math.abs(vehicleSpeed) > 0.4F && mc.options.keyJump.isDown()) {
                isBraking = true; // Handbrake slide
            }
        }

        if (!isBraking || curSpeedKmh < 2.0F || !vehicle.onGround()) {
            // Instantly kill brake sound instances when not braking or vehicle stopped!
            if (brakeHardSound != null) {
                mc.getSoundManager().stop(brakeHardSound);
                brakeHardSound = null;
            }
            if (brakeSlowSound != null) {
                mc.getSoundManager().stop(brakeSlowSound);
                brakeSlowSound = null;
            }
            return;
        }

        if (curSpeedKmh > 18.0F) {
            if (brakeSlowSound != null) {
                mc.getSoundManager().stop(brakeSlowSound);
                brakeSlowSound = null;
            }
            if (brakeHardSound == null || !mc.getSoundManager().isActive(brakeHardSound)) {
                brakeHardSound = new VehicleEngineSoundInstance(vehicle, ModSounds.BRAKE_HARD.get());
                mc.getSoundManager().play(brakeHardSound);
            }
            brakeHardSound.targetVolume = envGain * masterVolume * VehicleSoundConfig.brakeVolume * 1.25F * distFactor;
            brakeHardSound.targetPitch  = 1.0F;
        } else {
            if (brakeHardSound != null) {
                mc.getSoundManager().stop(brakeHardSound);
                brakeHardSound = null;
            }
            if (brakeSlowSound == null || !mc.getSoundManager().isActive(brakeSlowSound)) {
                brakeSlowSound = new VehicleEngineSoundInstance(vehicle, ModSounds.BRAKE_SLOW.get());
                mc.getSoundManager().play(brakeSlowSound);
            }
            brakeSlowSound.targetVolume = envGain * masterVolume * VehicleSoundConfig.brakeVolume * 1.10F * distFactor;
            brakeSlowSound.targetPitch  = 1.0F;
        }
    }

    public void stopEngineOnly() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < ENGINE_LAYERS; i++) {
            if (engineLayers[i] != null) {
                mc.getSoundManager().stop(engineLayers[i]);
                engineLayers[i] = null;
            }
            smoothedEngineGains[i] = 0.0F;
        }
        currentEngineId = null;
    }

    public void stopAll() {
        stopEngineOnly();
        Minecraft mc = Minecraft.getInstance();
        if (tireDirtSound != null) {
            mc.getSoundManager().stop(tireDirtSound);
            tireDirtSound = null;
        }
        if (tireRoadSound != null) {
            mc.getSoundManager().stop(tireRoadSound);
            tireRoadSound = null;
        }
        if (tireSnowSound != null) {
            mc.getSoundManager().stop(tireSnowSound);
            tireSnowSound = null;
        }
        if (brakeHardSound != null) {
            mc.getSoundManager().stop(brakeHardSound);
            brakeHardSound = null;
        }
        if (brakeSlowSound != null) {
            mc.getSoundManager().stop(brakeSlowSound);
            brakeSlowSound = null;
        }
    }
}
