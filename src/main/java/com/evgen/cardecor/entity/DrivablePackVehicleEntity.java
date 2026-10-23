package com.evgen.cardecor.entity;

import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DrivablePackVehicleEntity extends DrivableVehicleEntity {

    private static final EntityDataAccessor<String> VEHICLE_ID =
        SynchedEntityData.defineId(DrivablePackVehicleEntity.class, EntityDataSerializers.STRING);

    public DrivablePackVehicleEntity(EntityType<? extends DrivableVehicleEntity> type, Level level) {
        super(type, level);
    }

    public DrivablePackVehicleEntity(EntityType<? extends DrivableVehicleEntity> type, Level level, String vehicleId) {
        super(type, level);
        this.setVehicleId(vehicleId);
        applyDefinitionPhysics();
        if (this.physics != null) {
            this.physics.targetSurfaceY = this.getY();
            this.physics.isGrounded = true;
            this.physics.wasGrounded = true;
            this.physics.isPhysicallySupported = true;
        }
        VehicleDefinition def = VehiclePackManager.get(vehicleId);
        if (def != null) {
            if (def.defaultDirtLevel > 0.0F) {
                this.setDirtLevel(def.defaultDirtLevel);
            }
            if (def.defaultTireCondition < 100.0F) {
                for (int w = 0; w < 4; w++) {
                    this.setTireCondition(w, def.defaultTireCondition);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VEHICLE_ID, "vintage_pickup");
    }

    public String getVehicleId() {
        return this.entityData.get(VEHICLE_ID);
    }

    public void setVehicleId(String id) {
        String vid = (id != null) ? id : "vintage_pickup";
        this.entityData.set(VEHICLE_ID, vid);
        applyDefinitionPhysics();
        VehicleDefinition def = VehiclePackManager.get(vid);
        if (def != null) {
            if (def.defaultDirtLevel > 0.0F) {
                this.setDirtLevel(def.defaultDirtLevel);
            }
            if (def.defaultTireCondition < 100.0F) {
                for (int w = 0; w < 4; w++) {
                    this.setTireCondition(w, def.defaultTireCondition);
                }
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VEHICLE_ID.equals(key)) {
            applyDefinitionPhysics();
            this.refreshDimensions();
            this.setBoundingBox(this.makeBoundingBox());
        }
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.tickCount % 10 == 0) {
            applyDefinitionPhysics();
        }
        super.tick();
    }

    public void applyDefinitionPhysics() {
        VehicleDefinition def = VehiclePackManager.get(getVehicleId());
        if (def != null && def.physics != null) {
            double hp = (def.physics.horsepower > 0.0) ? def.physics.horsepower : (def.engine != null ? def.engine.horsepower : 150.0);
            double baseTq = (def.physics.torqueNm > 0.0) ? def.physics.torqueNm : (def.engine != null && def.engine.torqueNm > 0.0 ? def.engine.torqueNm : 240.0);
            double hpRatio = hp / 150.0;
            double scaledTorque = baseTq * Math.pow(hpRatio, 0.95);
            this.physics.mass = Math.max(200.0, def.physics.mass);
            this.physics.horsepower = hp;
            this.physics.torqueNm = Math.max(12.0, scaledTorque);
            this.physics.drivetrain = (def.physics.drivetrain != null) ? def.physics.drivetrain : "rwd";
            this.physics.engineType = (def.physics.engineType != null) ? def.physics.engineType : "inline6";
            this.physics.maxNormalSpeedKmh = def.physics.maxSpeed;
            this.physics.maxBoostSpeedKmh = def.physics.boostSpeed;
            this.physics.maxGears = def.physics.gears;
            this.physics.finalDrive = def.physics.finalDrive;
            this.physics.customGearRatios = def.physics.gearRatios;
            this.physics.customReverseRatio = def.physics.reverseRatio;
            this.physics.maxEngineForce = hp * 180.0;
            this.physics.maxSteerAngle = Math.toRadians(def.physics.maxSteerAngle);
            this.physics.steeringResponse = def.physics.steeringResponse;
            this.physics.turnInBite = def.physics.turnInBite;
            this.physics.steeringAgility = Math.max(0.2, def.physics.steeringAgility);
            this.physics.steerCurveSpeed = def.physics.steerCurveSpeed;
            this.physics.steerCurvePower = def.physics.steerCurvePower;
            this.physics.steerMinRatio = def.physics.steerMinRatio;
            this.physics.offroadCapability = Math.max(0.2, def.physics.offroadCapability);
            this.setMaxUpStep((float) Math.max(1.35F, 1.35F * (float) def.physics.offroadCapability));
            this.physics.suspensionStiffness = def.physics.suspensionStiffness;
            this.physics.suspensionDamping = def.physics.suspensionDamping;
            this.physics.maxSuspensionTravel = def.physics.maxSuspensionTravel;
            this.physics.bodyRollFactor = def.physics.bodyRollFactor;
            this.physics.pitchDiveFactor = def.physics.pitchDiveFactor;
            this.physics.antiRollBarStiffness = def.physics.antiRollBarStiffness;
            this.physics.rideHeight = def.physics.rideHeight;
            this.physics.weightDipSensitivity = def.physics.weightDipSensitivity;
            this.physics.brakePower = def.physics.brakePower;
            this.physics.maxBrakeForce = def.physics.brakePower;
            this.physics.wheelbase = def.physics.wheelbase;
            this.physics.trackWidth = def.physics.trackWidth;
            this.physics.wheelRadius = def.physics.wheelRadius;
            this.physics.frontOverhang = def.physics.frontOverhang;
            this.physics.rearOverhang = def.physics.rearOverhang;

            if (def.calibratedBounds != null) {
                this.physics.frontBumperDist = def.calibratedBounds.frontDist;
                this.physics.rearBumperDist = def.calibratedBounds.rearDist;
                this.physics.halfWidth = def.calibratedBounds.halfWidth;
            } else {
                this.physics.frontBumperDist = (def.physics.wheelbase * 0.5) + def.physics.frontOverhang;
                this.physics.rearBumperDist = (def.physics.wheelbase * 0.5) + def.physics.rearOverhang;
                this.physics.halfWidth = def.physics.trackWidth * 0.52;
            }

            this.physicsBody.configureFromDefinition(def);
            this.physics.weightDistributionFront = def.physics.weightDistributionFront;
            this.physics.centerOfGravityHeight = def.physics.centerOfGravityHeight;
            this.physics.bodyShakeIntensity = def.physics.bodyShakeIntensity;
            this.physics.cameraShakeIntensity = def.physics.cameraShakeIntensity;
            this.physics.tirePressureFL = def.physics.tirePressureFL;
            this.physics.tirePressureFR = def.physics.tirePressureFR;
            this.physics.tirePressureRL = def.physics.tirePressureRL;
            this.physics.tirePressureRR = def.physics.tirePressureRR;
            this.physics.aspirationType = def.physics.aspirationType;
            this.physics.boostPressureBar = def.physics.boostPressureBar;
            this.physics.turboSpoolRpm = def.physics.turboSpoolRpm;
            this.physics.turboPeakRpm = def.physics.turboPeakRpm;
            this.physics.superchargerPulley = def.physics.superchargerPulley;
            this.physics.idleRpm = def.physics.idleRpm;
            this.physics.redlineRpm = def.physics.redlineRpm;
            this.physics.autoMaxSpeed = def.physics.autoMaxSpeed;
            this.physics.dragCoefficient = def.physics.dragCoefficient;
            this.physics.frontalArea = def.physics.frontalArea;
            this.physics.downforceCoefficient = def.physics.downforceCoefficient;

            this.entityData.set(BODY_SHAKE, (float) def.physics.bodyShakeIntensity);
            this.entityData.set(CAMERA_SHAKE, (float) def.physics.cameraShakeIntensity);
        }

        applyInstalledEnginePhysics();

        if (def != null && def.wheels != null) {
            float wRad = (float) Math.max(0.25, def.wheels.radius);
            float wWidth = 0.55F;
            float wHeight = wRad * 2.0F;

            this.partWheelFL.localX = def.wheels.frontLeft[0];
            this.partWheelFL.localY = def.wheels.frontLeft[1];
            this.partWheelFL.localZ = def.wheels.frontLeft[2];
            this.partWheelFL.setDimensions(wWidth, wHeight);

            this.partWheelFR.localX = def.wheels.frontRight[0];
            this.partWheelFR.localY = def.wheels.frontRight[1];
            this.partWheelFR.localZ = def.wheels.frontRight[2];
            this.partWheelFR.setDimensions(wWidth, wHeight);

            this.partWheelRL.localX = def.wheels.rearLeft[0];
            this.partWheelRL.localY = def.wheels.rearLeft[1];
            this.partWheelRL.localZ = def.wheels.rearLeft[2];
            this.partWheelRL.setDimensions(wWidth, wHeight);

            this.partWheelRR.localX = def.wheels.rearRight[0];
            this.partWheelRR.localY = def.wheels.rearRight[1];
            this.partWheelRR.localZ = def.wheels.rearRight[2];
            this.partWheelRR.setDimensions(wWidth, wHeight);

            double frontZ = Math.min(def.wheels.frontLeft[2], def.wheels.frontRight[2]);
            double rearZ = Math.max(def.wheels.rearLeft[2], def.wheels.rearRight[2]);
            this.partHood.localZ = frontZ * 0.95;
            this.partTrunk.localZ = rearZ * 0.95;
            this.partCabin.localZ = (frontZ + rearZ) * 0.5 * 0.3;
        }
    }

    @Override
    public int getMaxSeats() {
        VehicleDefinition def = VehiclePackManager.get(getVehicleId());
        if (def != null && !def.seats.isEmpty()) {
            return def.seats.size();
        }
        return 3;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("VehiclePackId", getVehicleId());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("VehiclePackId")) {
            setVehicleId(tag.getString("VehiclePackId"));
        }
    }
}
