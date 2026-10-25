package com.evgen.cardecor.entity;



import com.evgen.cardecor.init.ModBlocks;

import com.evgen.cardecor.init.ModItems;

import com.evgen.cardecor.init.ModSounds;

import com.evgen.cardecor.physics.RaycastVehiclePhysics;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;

import net.minecraft.core.particles.BlockParticleOption;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.chat.Component;

import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.network.syncher.EntityDataSerializers;

import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.sounds.SoundSource;

import net.minecraft.util.Mth;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;

import net.minecraft.tags.DamageTypeTags;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import com.evgen.cardecor.init.ModItems;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.MoverType;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class DrivableVehicleEntity extends Entity {

    public static final ThreadLocal<Boolean> REDIRECTING_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    public static final ThreadLocal<Boolean> ALLOW_PASSENGER_PENETRATION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final java.util.concurrent.atomic.AtomicInteger ENTITY_COUNTER_REF;
    static {
        java.util.concurrent.atomic.AtomicInteger counter = null;
        try {
            for (java.lang.reflect.Field f : Entity.class.getDeclaredFields()) {
                if (java.util.concurrent.atomic.AtomicInteger.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    counter = (java.util.concurrent.atomic.AtomicInteger) f.get(null);
                    break;
                }
            }
        } catch (Throwable ignored) {}
        ENTITY_COUNTER_REF = counter;
    }

    private int lastDismountPlayerId = -1;

    private int dismountImmunityTicks = 0;



    public static final EntityDataAccessor<Boolean> ENGINE_ON =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);

        public static final EntityDataAccessor<Float> BODY_SHAKE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> CAMERA_SHAKE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> IMPACT_TRAUMA =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SPEED =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> STEER =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> ROLL =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> PITCH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> SERVER_YAW =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> QUAT_X =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> QUAT_Y =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> QUAT_Z =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> QUAT_W =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);


    public static final EntityDataAccessor<Float> TORQUE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> RPM =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> HEADLIGHTS_ON =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> HEADLIGHT_MODE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> SCREECH =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> SEAT_SLOTS =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Byte> TIRE_STATUS =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BYTE);

    public static final EntityDataAccessor<Float> IMPACT_SHAKE =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> VEHICLE_HP =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> GEAR =

        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> FUEL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Byte> INSTALLED_WHEELS =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BYTE);

    public static final EntityDataAccessor<Float> CHASSIS_DIP_Y =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> CHASSIS_DIP_PITCH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> CHASSIS_DIP_ROLL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> HAS_SPARE_WHEEL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> EXPLOSION_STAGE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DOOR_LEFT_ANGLE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DOOR_RIGHT_ANGLE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TRUNK_ANGLE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Byte> SEAT_ITEM_STATUS =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Boolean> LOCKED =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> KEY_IN_IGNITION =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Byte> CLIMATE_MODE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Boolean> TRUNK_LOCKED =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CABIN_LIGHT_ON =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> INSTALLED_ENGINE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> INSTALLED_WHEELS_DATA =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> INSTALLED_BRAKES_DATA =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> INSTALLED_SEATS_DATA =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> BRAKING =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> PARKING_BRAKE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> BATTERY_CHARGE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_QUALITY =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_CONDITION =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> IS_HOTWIRED =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> BUMPER_HEALTH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> HOOD_HEALTH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> RADIATOR_HEALTH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> BASE_LOUDNESS =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MUFFLER_HEALTH =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_OIL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COOLANT_LEVEL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_TEMPERATURE =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DIRT_LEVEL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> BLOOD_LEVEL =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> TIRE_CONDITIONS_PACKED =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> TRANSMISSION_CONDITION =
        SynchedEntityData.defineId(DrivableVehicleEntity.class, EntityDataSerializers.FLOAT);

    public boolean clientLocalDriver = false;

    private ItemStack ignitionKey = ItemStack.EMPTY;
    public boolean keySpawnInitialized = false;

    public static final float MAX_HP = 400.0F;
    public static final float MAX_FUEL = 100.0F;

    public float[] wheelHealth = new float[]{150.0F, 150.0F, 150.0F, 150.0F};
    public float[] tireCondition = new float[]{100.0F, 100.0F, 100.0F, 100.0F};

    public float getMaxWheelHp() {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        return (def != null) ? def.getMaxWheelHp() : 150.0F;
    }



    public final RaycastVehiclePhysics physics = new RaycastVehiclePhysics();
    public final com.evgen.cardecor.physics.engine.RigidBodyOBB physicsBody = new com.evgen.cardecor.physics.engine.RigidBodyOBB(1500f, 1.0f, 0.55f, 2.1f);
    public final com.evgen.cardecor.physics.ode.OdeVehicleBody odeBody = new com.evgen.cardecor.physics.ode.OdeVehicleBody();
    public boolean odeBodyRegistered = false;
    public final org.joml.Quaternionf currentRotationQuat = new org.joml.Quaternionf(0, 0, 0, 1);
    public final org.joml.Quaternionf prevRotationQuat = new org.joml.Quaternionf(0, 0, 0, 1);



    public float currentRoll = 0.0F;

    public float prevRoll = 0.0F;

    public float currentPitch = 0.0F;
    public float prevPitch = 0.0F;

    public float currentSteer = 0.0F;
    public float prevSteer = 0.0F;

    public float currentChassisDipY = 0.0F;
    public float prevChassisDipY = 0.0F;
    public float currentChassisDipPitch = 0.0F;
    public float prevChassisDipPitch = 0.0F;
    public float currentChassisDipRoll = 0.0F;
    public float prevChassisDipRoll = 0.0F;



    public float prevWheelRotation = 0.0F;
    public float currentWheelRotation = 0.0F;
    public float[] prevWheelRotations = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    public float[] currentWheelRotations = new float[]{0.0F, 0.0F, 0.0F, 0.0F};



    private int lerpSteps;

    private double lerpX;

    private double lerpY;

    private double lerpZ;

    private double lerpYRot;
    private float prevReceivedServerYaw = Float.NaN;
    private float clientYawVelocity = 0.0F;



    public int inputFlags = 0;

    public long lastHornTime = 0;

    // Server Deep-Sleep Optimization for parked / unoperated vehicles
    public boolean isDormant = false;
    private int dormantTicks = 0;

    public void wakeUp() {
        if (this.isDormant) {
            this.isDormant = false;
        }
        this.dormantTicks = 0;
        if (this.odeBody != null) {
            this.odeBody.wakeUp();
        }
        if (this.physicsBody != null) {
            this.physicsBody.wakeUp();
        }
    }

    private int waterSubmergedTicks = 0;

    private int crankTimer = 0;

    private boolean isExploding = false;
    private int explosionTicks = 0;
    private int lastExplosionTick = -1;
    private DamageSource lastExplosionSource = null;

    public int getExplosionStage() {
        return this.entityData.get(EXPLOSION_STAGE);
    }

    // Door & trunk animation fields (smooth interpolation on client, timer on server)
    public float prevDoorLeftAngle = 0.0F;
    public float prevDoorRightAngle = 0.0F;
    public float currentDoorLeftAngle = 0.0F;
    public float currentDoorRightAngle = 0.0F;
    public int doorLeftCloseTimer = 0;
    public int doorRightCloseTimer = 0;
    public float prevTrunkAngle = 0.0F;
    public float currentTrunkAngle = 0.0F;
    public int trunkCloseTimer = 0;

    public float getDoorLeftAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevDoorLeftAngle, this.currentDoorLeftAngle);
    }

    public float getDoorRightAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevDoorRightAngle, this.currentDoorRightAngle);
    }

    public float getTrunkAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevTrunkAngle, this.currentTrunkAngle);
    }

    public boolean isParkingBrakeOn() {
        return this.entityData.get(PARKING_BRAKE);
    }

    public void setParkingBrake(boolean on) {
        this.entityData.set(PARKING_BRAKE, on);
    }

    public void toggleParkingBrake() {
        setParkingBrake(!isParkingBrakeOn());
    }

    public boolean isHandbrakeActive() {
        return !hasDriver() || (Math.abs(this.entityData.get(SPEED)) < 0.05F && !isEngineOn());
    }

    public boolean isBraking() {
        if (!hasDriver()) return false;
        // Direct zero-latency input check (works for driver on client and on server)
        if ((this.inputFlags & 16) != 0) return true;
        float curSpeed = this.entityData.get(SPEED);
        boolean keyW = (this.inputFlags & 1) != 0;
        boolean keyS = (this.inputFlags & 2) != 0;
        if (keyS && curSpeed > 0.05F) return true;
        if (keyW && curSpeed < -0.05F) return true;
        if (keyS && Math.abs(curSpeed) <= 0.05F) return true;

        if (this.level().isClientSide()) {
            if (this.clientLocalDriver) {
                return false;
            }
            return this.entityData.get(BRAKING);
        }
        return false;
    }

    private final int[] seatSwitchTimer = new int[]{0, 0, 0, 0, 0};

    private final int[] pendingTargetSeat = new int[]{-1, -1, -1, -1, -1};
    private final java.util.Set<Integer> prevDeckEntityIds = new java.util.HashSet<>();
    public final java.util.Set<Integer> currentDeckEntityIds = new java.util.HashSet<>();
    public final java.util.Set<Integer> airborneDeckEntityIds = new java.util.HashSet<>();
    private final java.util.Map<Integer, Integer> rammedEntityCooldowns = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> lastDeckLandTicks = new java.util.HashMap<>();
    private final java.util.Map<Integer, Vec3> prevDeckLocalPos = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> deckStepCounters = new java.util.HashMap<>();
    private final java.util.Map<Integer, Double> deckMaxAirborneY = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> mobAttackCooldowns = new java.util.HashMap<>();
    private long lastDeformTick = -1;
    private int deformPacketsThisTick = 0;



    private Object soundManagerInstance;

    public final VehiclePartEntity[] subEntities;
    public final VehiclePartEntity partChassis;
    public final VehiclePartEntity partCabin;
    public final VehiclePartEntity partHood;
    public final VehiclePartEntity partTrunk;
    public final VehiclePartEntity partWheelFL;
    public final VehiclePartEntity partWheelFR;
    public final VehiclePartEntity partWheelRL;
    public final VehiclePartEntity partWheelRR;
    public final VehiclePartEntity partSpareWheel;
    public final VehiclePartEntity partRoof;

    public DrivableVehicleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
        this.setMaxUpStep(1.60F);
        this.noCulling = true;

        this.partChassis = new VehiclePartEntity(this, "chassis", 1.85F, 0.40F, 0.0, 0.20, 0.0);
        this.partCabin = new VehiclePartEntity(this, "cabin", 1.70F, 0.85F, 0.0, 0.80, 0.10);
        this.partHood = new VehiclePartEntity(this, "hood", 1.70F, 0.70F, 0.0, 0.55, -1.45);
        this.partTrunk = new VehiclePartEntity(this, "trunk", 1.70F, 0.70F, 0.0, 0.55, 1.35);
        this.partWheelFL = new VehiclePartEntity(this, "wheel_fl", 0.60F, 0.65F, 0.95, 0.35, -1.40);
        this.partWheelFR = new VehiclePartEntity(this, "wheel_fr", 0.60F, 0.65F, -0.95, 0.35, -1.40);
        this.partWheelRL = new VehiclePartEntity(this, "wheel_rl", 0.60F, 0.65F, 0.95, 0.35, 1.40);
        this.partWheelRR = new VehiclePartEntity(this, "wheel_rr", 0.60F, 0.65F, -0.95, 0.35, 1.40);
        this.partSpareWheel = new VehiclePartEntity(this, "spare_wheel", 0.85F, 0.85F, 0.03, 1.36, -0.38);
        this.partRoof = new VehiclePartEntity(this, "roof", 1.70F, 0.35F, 0.0, 2.35, 0.10);

        this.subEntities = new VehiclePartEntity[]{
            this.partChassis, this.partCabin, this.partHood, this.partTrunk,
            this.partWheelFL, this.partWheelFR, this.partWheelRL, this.partWheelRR,
            this.partSpareWheel, this.partRoof
        };

        if (ENTITY_COUNTER_REF != null) {
            this.setId(ENTITY_COUNTER_REF.getAndAdd(this.subEntities.length + 1) + 1);
        }
        for (int i = 0; i < this.subEntities.length; i++) {
            this.subEntities[i].setId(this.getId() + i + 1);
        }
    }



    @Override

    protected void defineSynchedData() {

        this.entityData.define(ENGINE_ON, false);

                this.entityData.define(BODY_SHAKE, 1.0F);
        this.entityData.define(CAMERA_SHAKE, 1.0F);
        this.entityData.define(IMPACT_TRAUMA, 0.0F);
        this.entityData.define(SPEED, 0.0F);

        this.entityData.define(STEER, 0.0F);

        this.entityData.define(ROLL, 0.0F);

        this.entityData.define(PITCH, 0.0F);
        this.entityData.define(SERVER_YAW, 0.0F);
        this.entityData.define(QUAT_X, 0.0F);
        this.entityData.define(QUAT_Y, 0.0F);
        this.entityData.define(QUAT_Z, 0.0F);
        this.entityData.define(QUAT_W, 1.0F);

        this.entityData.define(RPM, 0.0F);
        this.entityData.define(TORQUE, 0.0F);

        this.entityData.define(SCREECH, 0.0F);

        this.entityData.define(HEADLIGHTS_ON, false);
        this.entityData.define(HEADLIGHT_MODE, 0);

        this.entityData.define(SEAT_SLOTS, 0);

        this.entityData.define(TIRE_STATUS, (byte) 0);

        this.entityData.define(IMPACT_SHAKE, 0.0F);

        this.entityData.define(VEHICLE_HP, MAX_HP);

        this.entityData.define(GEAR, 0);
        this.entityData.define(FUEL, MAX_FUEL);
        this.entityData.define(SEAT_ITEM_STATUS, (byte) 0);
        this.entityData.define(INSTALLED_WHEELS, (byte) 15);
        this.entityData.define(CHASSIS_DIP_Y, 0.0F);
        this.entityData.define(CHASSIS_DIP_PITCH, 0.0F);
        this.entityData.define(CHASSIS_DIP_ROLL, 0.0F);
        this.entityData.define(HAS_SPARE_WHEEL, true);
        this.entityData.define(EXPLOSION_STAGE, 0);
        this.entityData.define(DOOR_LEFT_ANGLE, 0.0F);
        this.entityData.define(DOOR_RIGHT_ANGLE, 0.0F);
        this.entityData.define(TRUNK_ANGLE, 0.0F);
        this.entityData.define(LOCKED, false);
        this.entityData.define(KEY_IN_IGNITION, false);
        this.entityData.define(CLIMATE_MODE, (byte) 0);
        this.entityData.define(TRUNK_LOCKED, false);
        this.entityData.define(CABIN_LIGHT_ON, false);
        this.entityData.define(INSTALLED_ENGINE, "");
        this.entityData.define(INSTALLED_WHEELS_DATA, "");
        this.entityData.define(INSTALLED_BRAKES_DATA, "brake_02_stock_vented,brake_02_stock_vented,brake_02_stock_vented,brake_02_stock_vented");
        this.entityData.define(INSTALLED_SEATS_DATA, "");
        this.entityData.define(BRAKING, false);
        this.entityData.define(PARKING_BRAKE, false);
        this.entityData.define(BATTERY_CHARGE, 100.0F);
        this.entityData.define(ENGINE_QUALITY, 65.0F);
        this.entityData.define(ENGINE_CONDITION, 100.0F);
        this.entityData.define(IS_HOTWIRED, false);
        this.entityData.define(BUMPER_HEALTH, 100.0F);
        this.entityData.define(HOOD_HEALTH, 100.0F);
        this.entityData.define(RADIATOR_HEALTH, 100.0F);
        this.entityData.define(BASE_LOUDNESS, 24.0F);
        this.entityData.define(MUFFLER_HEALTH, 100.0F);
        this.entityData.define(ENGINE_OIL, 100.0F);
        this.entityData.define(COOLANT_LEVEL, 100.0F);
        this.entityData.define(ENGINE_TEMPERATURE, 20.0F);
        this.entityData.define(DIRT_LEVEL, 0.0F);
        this.entityData.define(BLOOD_LEVEL, 0.0F);
        this.entityData.define(TIRE_CONDITIONS_PACKED, packTireConditions(100.0F, 100.0F, 100.0F, 100.0F));
        this.entityData.define(TRANSMISSION_CONDITION, 100.0F);
    }

    public float getDirtLevel() {
        return this.entityData.get(DIRT_LEVEL);
    }

    public void setDirtLevel(float val) {
        this.entityData.set(DIRT_LEVEL, Math.max(0.0F, Math.min(100.0F, val)));
    }

    public float getBloodLevel() {
        return this.entityData.get(BLOOD_LEVEL);
    }

    public void setBloodLevel(float val) {
        this.entityData.set(BLOOD_LEVEL, Math.max(0.0F, Math.min(100.0F, val)));
    }

    public static double getMobMass(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.animal.Chicken
                || target instanceof net.minecraft.world.entity.animal.Rabbit
                || target instanceof net.minecraft.world.entity.monster.Silverfish
                || target instanceof net.minecraft.world.entity.ambient.Bat
                || target instanceof net.minecraft.world.entity.animal.Bee) {
            return 10.0;
        }
        if (target instanceof net.minecraft.world.entity.animal.IronGolem
                || target instanceof net.minecraft.world.entity.monster.warden.Warden
                || target instanceof net.minecraft.world.entity.monster.Ravager) {
            return 1500.0;
        }
        if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return 2500.0;
        }
        if (target instanceof net.minecraft.world.entity.animal.Cow
                || target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse
                || target instanceof net.minecraft.world.entity.monster.hoglin.Hoglin
                || target instanceof net.minecraft.world.entity.animal.PolarBear) {
            return 450.0;
        }
        if (target instanceof net.minecraft.world.entity.animal.Pig
                || target instanceof net.minecraft.world.entity.animal.Sheep
                || target instanceof net.minecraft.world.entity.animal.Wolf) {
            return 70.0;
        }
        double vol = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        double estimatedKg = vol * 130.0;
        return Math.max(15.0, Math.min(3000.0, estimatedKg));
    }

    public float getEngineTemperature() {
        return this.entityData.get(ENGINE_TEMPERATURE);
    }

    public void setEngineTemperature(float val) {
        this.entityData.set(ENGINE_TEMPERATURE, Math.max(20.0F, Math.min(130.0F, val)));
    }

    public float getEngineOil() {
        return this.entityData.get(ENGINE_OIL);
    }

    public void setEngineOil(float val) {
        this.entityData.set(ENGINE_OIL, Math.max(0.0F, Math.min(100.0F, val)));
    }

    public float getCoolantLevel() {
        return this.entityData.get(COOLANT_LEVEL);
    }

    public void setCoolantLevel(float val) {
        this.entityData.set(COOLANT_LEVEL, Math.max(0.0F, Math.min(100.0F, val)));
    }

    public float getBaseLoudness() {
        return this.entityData.get(BASE_LOUDNESS);
    }

    public void setBaseLoudness(float val) {
        this.entityData.set(BASE_LOUDNESS, Math.max(0.0F, val));
    }

    public float getMufflerHealth() {
        return this.entityData.get(MUFFLER_HEALTH);
    }

    public void setMufflerHealth(float val) {
        this.entityData.set(MUFFLER_HEALTH, Math.max(0.0F, Math.min(100.0F, val)));
    }

    public void pulseNoise(float extraRadius) {
        if (this.level().isClientSide) return;
        if (!com.evgen.cardecor.config.VehicleNoiseConfig.enabled) return;

        float base = getBaseLoudness();
        // Muffler condition: at 100% -> 1.0x, at 0% -> 3.0x noise radius
        float mufflerFactor = 1.0F + ((100.0F - getMufflerHealth()) / 100.0F) * 2.0F;
        float radiusMultiplier = (extraRadius > 0.0F)
            ? com.evgen.cardecor.config.VehicleNoiseConfig.hornNoiseMultiplier
            : com.evgen.cardecor.config.VehicleNoiseConfig.engineNoiseMultiplier;
        float radius = ((base * mufflerFactor) + extraRadius) * radiusMultiplier;
        radius = Math.min(radius, com.evgen.cardecor.config.VehicleNoiseConfig.maxNoiseRadius);

        // Vanilla GameEvent (triggers sculk sensors, warden, etc.)
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.NOTE_BLOCK_PLAY, this);

        // Mobs investigate the sound location with random scatter
        net.minecraft.world.phys.AABB searchBox = this.getBoundingBox().inflate(radius);
        java.util.List<net.minecraft.world.entity.Mob> mobs =
            this.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, searchBox);

        double originX = this.getX();
        double originY = this.getY();
        double originZ = this.getZ();
        double wanderSpread = com.evgen.cardecor.config.VehicleNoiseConfig.wanderRadiusAroundSound;
        double speed = com.evgen.cardecor.config.VehicleNoiseConfig.investigateSpeed;

        for (net.minecraft.world.entity.Mob mob : mobs) {
            if (!com.evgen.cardecor.config.VehicleNoiseConfig.shouldMobInvestigate(mob)) continue;

            // Only direct mobs that are NOT already fighting someone (no direct aggro on vehicle/driver)
            if (mob.getTarget() == null) {
                double targetX = originX + (this.random.nextDouble() - 0.5D) * (wanderSpread * 2.0D);
                double targetZ = originZ + (this.random.nextDouble() - 0.5D) * (wanderSpread * 2.0D);
                mob.getNavigation().moveTo(targetX, originY, targetZ, speed);
            }
        }
    }

    public static int packTireConditions(float c0, float c1, float c2, float c3) {
        int b0 = Math.max(0, Math.min(100, Math.round(c0)));
        int b1 = Math.max(0, Math.min(100, Math.round(c1)));
        int b2 = Math.max(0, Math.min(100, Math.round(c2)));
        int b3 = Math.max(0, Math.min(100, Math.round(c3)));
        return (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
    }

    public void syncTireConditionsToEntityData() {
        if (!this.level().isClientSide) {
            int packed = packTireConditions(tireCondition[0], tireCondition[1], tireCondition[2], tireCondition[3]);
            this.entityData.set(TIRE_CONDITIONS_PACKED, packed);
        }
    }

    public float getTireCondition(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 100.0F;
        return this.tireCondition[wheelIndex];
    }

    public float getClientTireCondition(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 100.0F;
        if (!this.level().isClientSide) {
            return this.tireCondition[wheelIndex];
        }
        int packed = this.entityData.get(TIRE_CONDITIONS_PACKED);
        int b = (packed >> (wheelIndex * 8)) & 0xFF;
        return (float) b;
    }

    public void setTireCondition(int wheelIndex, float condition) {
        if (wheelIndex >= 0 && wheelIndex < 4) {
            this.tireCondition[wheelIndex] = Math.max(0.0F, Math.min(100.0F, condition));
            if (this.physics != null && this.physics.tireCondition != null) {
                this.physics.tireCondition[wheelIndex] = this.tireCondition[wheelIndex];
            }
            syncTireConditionsToEntityData();
        }
    }

    public float getMaxBumperHealth() {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        return (def != null && def.durability != null && def.durability.maxBumperHealth > 0.0F) ? def.durability.maxBumperHealth : 100.0F;
    }

    public float getBumperHealth() {
        return this.entityData.get(BUMPER_HEALTH);
    }

    public void setBumperHealth(float hp) {
        this.entityData.set(BUMPER_HEALTH, Math.max(0.0F, Math.min(getMaxBumperHealth(), hp)));
    }

    public float getMaxHoodHealth() {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        return (def != null && def.durability != null && def.durability.maxHoodHealth > 0.0F) ? def.durability.maxHoodHealth : 100.0F;
    }

    public float getHoodHealth() {
        return this.entityData.get(HOOD_HEALTH);
    }

    public void setHoodHealth(float hp) {
        this.entityData.set(HOOD_HEALTH, Math.max(0.0F, Math.min(getMaxHoodHealth(), hp)));
    }

    public float getMaxRadiatorHealth() {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        return (def != null && def.durability != null && def.durability.maxRadiatorHealth > 0.0F) ? def.durability.maxRadiatorHealth : 100.0F;
    }

    public float getRadiatorHealth() {
        return this.entityData.get(RADIATOR_HEALTH);
    }

    public void setRadiatorHealth(float hp) {
        this.entityData.set(RADIATOR_HEALTH, Math.max(0.0F, Math.min(getMaxRadiatorHealth(), hp)));
    }

    public void applyFrontalImpactDamage(float incomingDamage) {
        if (incomingDamage <= 0.0F || this.level().isClientSide) return;

        float remaining = incomingDamage;

        // 1. Layer 1 — Bumper (absorbs 100% until broken)
        float curBumper = getBumperHealth();
        if (curBumper > 0.0F) {
            float absorbed = Math.min(curBumper, remaining);
            setBumperHealth(curBumper - absorbed);
            remaining -= absorbed;
            if (getBumperHealth() <= 0.0F) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.8F, 0.75F);
            }
        }

        if (remaining <= 0.0F) return;

        // 2. Layer 2 — Hood (absorbs until crushed)
        float curHood = getHoodHealth();
        if (curHood > 0.0F) {
            float absorbed = Math.min(curHood, remaining);
            setHoodHealth(curHood - absorbed);
            remaining -= absorbed;
            if (getHoodHealth() <= 0.0F) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.NEUTRAL, 2.0F, 0.65F);
            }
        }

        if (remaining <= 0.0F) return;

        // 3. Layer 3 — Radiator & Engine
        float curRad = getRadiatorHealth();
        if (curRad > 0.0F) {
            float radDmg = remaining * 0.75F;
            setRadiatorHealth(curRad - radDmg);
            if (getRadiatorHealth() <= 0.0F) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 2.2F, 1.1F);
            }
        }

        // Kinetic impact crushes the engine block and undercarriage/muffler
        float curEngine = getEngineCondition();
        float engineDamage = remaining * 0.60F;
        setEngineCondition(Math.max(0.0F, curEngine - engineDamage));

        float curMuffler = getMufflerHealth();
        if (curMuffler > 0.0F) {
            setMufflerHealth(Math.max(0.0F, curMuffler - remaining * 0.35F));
        }

        // Excess kinetic shock deducts from overall hull HP
        setVehicleHealth(Math.max(0.0F, getVehicleHealth() - remaining * 0.40F));
    }

    public float getBatteryCharge() {
        return this.entityData.get(BATTERY_CHARGE);
    }

    public void setBatteryCharge(float charge) {
        this.entityData.set(BATTERY_CHARGE, Math.max(0.0F, Math.min(100.0F, charge)));
    }

    public float getEngineQuality() {
        return this.entityData.get(ENGINE_QUALITY);
    }

    public void setEngineQuality(float quality) {
        this.entityData.set(ENGINE_QUALITY, Math.max(0.0F, Math.min(100.0F, quality)));
    }

    public float getEngineCondition() {
        return this.entityData.get(ENGINE_CONDITION);
    }

    public void setEngineCondition(float condition) {
        this.entityData.set(ENGINE_CONDITION, Math.max(0.0F, Math.min(100.0F, condition)));
    }

    public boolean isHotwired() {
        return this.entityData.get(IS_HOTWIRED);
    }

    public void setHotwired(boolean hotwired) {
        this.entityData.set(IS_HOTWIRED, hotwired);
    }

    public boolean hasEngine() {
        String engineId = this.entityData.get(INSTALLED_ENGINE);
        return !"none".equalsIgnoreCase(engineId);
    }

    public String getInstalledEngine() {
        String engineId = this.entityData.get(INSTALLED_ENGINE);
        if ("none".equalsIgnoreCase(engineId)) {
            return "none";
        }
        if (engineId != null && !engineId.trim().isEmpty()) {
            return engineId;
        }
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.defaultEngine != null && !def.defaultEngine.trim().isEmpty()) {
            return def.defaultEngine;
        }
        String vId = this.getVehicleId();
        if (vId != null && vId.contains("pickup")) return "ford_302_v8";
        if (vId != null && vId.contains("jeep")) return "amc_4_0_i6";
        return "om617_diesel";
    }

    public void setInstalledEngine(String engineId) {
        this.entityData.set(INSTALLED_ENGINE, engineId != null ? engineId : "");
        applyInstalledEnginePhysics();
    }

    public void applyInstalledEnginePhysics() {
        String engineId = getInstalledEngine();
        com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(engineId);
        if (eng != null && this.physics != null) {
            this.physics.applyEngineDefinition(eng);
            if (eng.physics != null) {
                this.physics.horsepower = eng.physics.horsepower;
                this.physics.torqueNm = eng.physics.torqueNm;
                this.physics.idleRpm = eng.physics.idleRpm;
                this.physics.redlineRpm = eng.physics.redlineRpm;
                this.physics.maxEngineForce = eng.physics.horsepower * 180.0;
            }
            if (eng.type != null && !eng.type.isEmpty()) {
                this.physics.engineType = eng.type;
            }
        }
    }

    public String getInstalledWheelId(int index) {
        if (index < 0 || index >= 4) return "standard_road_wheel";
        String raw = this.entityData.get(INSTALLED_WHEELS_DATA);
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            if (index < parts.length && !parts[index].trim().isEmpty()) {
                return parts[index].trim();
            }
        }
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.defaultWheel != null && !def.defaultWheel.trim().isEmpty()) {
            return def.defaultWheel;
        }
        return "standard_road_wheel";
    }

    public String getInstalledTireId(int index) {
        String wId = getInstalledWheelId(index);
        if (wId != null && wId.contains(";")) {
            return wId.split(";")[0];
        }
        if ("offroad_heavy_wheel".equalsIgnoreCase(wId)) return "tire_01_offroad_mud";
        if ("sport_alloy_wheel".equalsIgnoreCase(wId)) return "tire_05_low_profile";
        if (com.evgen.cardecor.pack.ModularWheelRegistry.getTire(wId) != null && (!"tire_03_classic_radial".equals(com.evgen.cardecor.pack.ModularWheelRegistry.getTire(wId).id) || "tire_03_classic_radial".equals(wId))) {
            return wId;
        }
        return "tire_03_classic_radial";
    }

    public String getInstalledRimId(int index) {
        String wId = getInstalledWheelId(index);
        if (wId != null && wId.contains(";")) {
            String[] parts = wId.split(";");
            return parts.length > 1 ? parts[1] : "rim_04_stamped_steelie";
        }
        if ("offroad_heavy_wheel".equalsIgnoreCase(wId)) return "rim_01_offroad_beadlock";
        if ("sport_alloy_wheel".equalsIgnoreCase(wId)) return "rim_07_tuner_bbs_mesh";
        return "rim_04_stamped_steelie";
    }

    public String getInstalledBrakeId(int index) {
        if (index < 0 || index >= 4) return "brake_02_stock_vented";
        String raw = this.entityData.get(INSTALLED_BRAKES_DATA);
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            if (index < parts.length && !parts[index].trim().isEmpty()) {
                return parts[index].trim();
            }
        }
        return "brake_02_stock_vented";
    }

    public void setInstalledBrakeId(int index, String brakeId) {
        if (index < 0 || index >= 4) return;
        String curRaw = this.entityData.get(INSTALLED_BRAKES_DATA);
        String[] parts = new String[4];
        for (int i = 0; i < 4; i++) parts[i] = "brake_02_stock_vented";
        if (curRaw != null && !curRaw.trim().isEmpty()) {
            String[] split = curRaw.split(",");
            for (int i = 0; i < Math.min(split.length, 4); i++) {
                if (!split[i].trim().isEmpty()) parts[i] = split[i].trim();
            }
        }
        parts[index] = (brakeId != null && !brakeId.trim().isEmpty()) ? brakeId.trim() : "brake_02_stock_vented";
        String joined = String.join(",", parts);
        this.entityData.set(INSTALLED_BRAKES_DATA, joined);
        applyBrakesPhysics();
    }

    public void applyBrakesPhysics() {
        if (this.physics == null) return;
        float avgBrake = 0.0F;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            String bId = getInstalledBrakeId(i);
            if (bId != null && !bId.isEmpty()) {
                com.evgen.cardecor.pack.BrakeDefinition bDef = com.evgen.cardecor.pack.ModularWheelRegistry.getBrake(bId);
                if (bDef != null) {
                    avgBrake += (float) bDef.brakeForceMult;
                    count++;
                }
            }
        }
        this.physics.brakePowerMultiplier = (count > 0) ? (avgBrake / count) : 1.0F;
    }

    public void setInstalledWheelId(int index, String wheelId) {
        if (index < 0 || index >= 4) return;
        String curRaw = this.entityData.get(INSTALLED_WHEELS_DATA);
        String[] parts = new String[4];
        String defWheel = "standard_road_wheel";
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.defaultWheel != null && !def.defaultWheel.trim().isEmpty()) {
            defWheel = def.defaultWheel;
        }
        for (int i = 0; i < 4; i++) {
            parts[i] = defWheel;
        }
        if (curRaw != null && !curRaw.trim().isEmpty()) {
            String[] split = curRaw.split(",");
            for (int i = 0; i < Math.min(split.length, 4); i++) {
                if (!split[i].trim().isEmpty()) {
                    parts[i] = split[i].trim();
                }
            }
        }
        parts[index] = (wheelId != null && !wheelId.trim().isEmpty()) ? wheelId.trim() : defWheel;
        String joined = String.join(",", parts);
        this.entityData.set(INSTALLED_WHEELS_DATA, joined);
        applyAllWheelsPhysics();
    }

    public void applyAllWheelsPhysics() {
        if (this.physics == null) return;
        float avgAsphalt = 0.0F;
        float avgDirt = 0.0F;
        float avgIce = 0.0F;
        double avgRadius = 0.0;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (isWheelInstalled(i)) {
                String tireId = getInstalledTireId(i);
                com.evgen.cardecor.pack.TireDefinition tDef = com.evgen.cardecor.pack.ModularWheelRegistry.getTire(tireId);
                if (tDef != null) {
                    avgAsphalt += (float) tDef.gripAsphalt;
                    avgDirt += (float) tDef.gripDirt;
                    avgIce += (float) tDef.gripIce;
                    avgRadius += tDef.radius;
                    count++;
                } else {
                    com.evgen.cardecor.pack.WheelDefinition wDef = com.evgen.cardecor.pack.WheelRegistry.get(getInstalledWheelId(i));
                    if (wDef != null) {
                        avgAsphalt += (float) wDef.gripAsphalt;
                        avgDirt += (float) wDef.gripDirt;
                        avgIce += (float) wDef.gripIce;
                        avgRadius += wDef.radius;
                        count++;
                    }
                }
            }
        }
        if (count > 0) {
            this.physics.tireGripAsphalt = avgAsphalt / count;
            this.physics.tireGripDirt = avgDirt / count;
            this.physics.tireGripIce = avgIce / count;
            this.physics.wheelRadius = avgRadius / count;
        } else {
            this.physics.tireGripAsphalt = 0.05F;
            this.physics.tireGripDirt = 0.05F;
            this.physics.tireGripIce = 0.05F;
        }
        applyBrakesPhysics();
    }

    public String getInstalledSeatId(int index) {
        if (index < 0) return "standard_seat";
        String raw = this.entityData.get(INSTALLED_SEATS_DATA);
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            if (index < parts.length && !parts[index].trim().isEmpty()) {
                return parts[index].trim();
            }
        }
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.defaultSeat != null && !def.defaultSeat.trim().isEmpty()) {
            return def.defaultSeat;
        }
        return "standard_seat";
    }

    public boolean isSeatInstalled(int index) {
        String id = getInstalledSeatId(index);
        return id != null && !id.isEmpty() && !"none".equalsIgnoreCase(id) && !"empty".equalsIgnoreCase(id);
    }

    public void setInstalledSeatId(int index, String seatId) {
        if (index < 0 || index >= 16) return;
        int max = Math.max(index + 1, getMaxSeats());
        String curRaw = this.entityData.get(INSTALLED_SEATS_DATA);
        String[] parts = new String[max];
        String defSeat = "standard_seat";
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.defaultSeat != null && !def.defaultSeat.trim().isEmpty()) {
            defSeat = def.defaultSeat;
        }
        for (int i = 0; i < max; i++) {
            parts[i] = defSeat;
        }
        if (curRaw != null && !curRaw.trim().isEmpty()) {
            String[] split = curRaw.split(",");
            for (int i = 0; i < Math.min(split.length, max); i++) {
                if (!split[i].trim().isEmpty()) {
                    parts[i] = split[i].trim();
                }
            }
        }
        parts[index] = (seatId != null && !seatId.trim().isEmpty()) ? seatId.trim() : "none";
        String joined = String.join(",", parts);
        this.entityData.set(INSTALLED_SEATS_DATA, joined);
    }

    public boolean hasSpareWheel() {
        return this.entityData.get(HAS_SPARE_WHEEL);
    }

    public void setSpareWheel(boolean has) {
        this.entityData.set(HAS_SPARE_WHEEL, has);
    }

    public void applyChassisImpulse(float dipY, float dipPitchDeg, float dipRollDeg) {
        wakeUp();
        if (this.physics != null) {
            double yVel = dipY * 16.0;
            this.physics.applyChassisImpulse(dipY, yVel, Math.toRadians(dipPitchDeg), Math.toRadians(dipRollDeg));
            this.entityData.set(CHASSIS_DIP_Y, (float) this.physics.chassisDipY);
            this.entityData.set(CHASSIS_DIP_PITCH, (float) Math.toDegrees(this.physics.chassisDipPitch));
            this.entityData.set(CHASSIS_DIP_ROLL, (float) Math.toDegrees(this.physics.chassisDipRoll));
        } else {
            float curY = this.entityData.get(CHASSIS_DIP_Y);
            float curP = this.entityData.get(CHASSIS_DIP_PITCH);
            float curR = this.entityData.get(CHASSIS_DIP_ROLL);

            this.entityData.set(CHASSIS_DIP_Y, Mth.clamp(curY + dipY, -0.12F, 0.06F));
            this.entityData.set(CHASSIS_DIP_PITCH, Mth.clamp(curP + dipPitchDeg, -4.5F, 4.5F));
            this.entityData.set(CHASSIS_DIP_ROLL, Mth.clamp(curR + dipRollDeg, -5.0F, 5.0F));
        }
    }







    private final Set<BlockPos> activeHeadlightBlocks = new HashSet<>();
    private BlockPos activeCabinLightPos = null;
    private Vec3 lastHeadlightPos = null;
    private float lastHeadlightYaw = 0.0F;
    private int lastHeadlightMode = 0;
    private boolean allowedToDismount = false;

    public boolean isAllowedToDismount() {
        return allowedToDismount;
    }

    public void setAllowedToDismount(boolean allowed) {
        this.allowedToDismount = allowed;
    }

    private final SimpleContainer trunkInventory = new SimpleContainer(54) {
        @Override
        public boolean stillValid(Player player) {
            return !isRemoved() && player.distanceToSqr(DrivableVehicleEntity.this) < 64.0;
        }
    };

    public SimpleContainer getTrunkInventory() {
        return this.trunkInventory;
    }

    public void dropTrunkContents() {
        if (!this.level().isClientSide && this.trunkInventory != null) {
            Containers.dropContents(this.level(), this, this.trunkInventory);
        }
        dropSeatContents();
    }

    // Passenger seat storage inventories: 4 slots max per seat (seats 1, 2, 3; seat 0 is driver only)
    private final SimpleContainer[] seatInventories = new SimpleContainer[]{
        null, // Seat 0 is driver: cannot put items
        createSeatContainer(1),
        createSeatContainer(2),
        createSeatContainer(3)
    };

    private SimpleContainer createSeatContainer(int seatIdx) {
        return new SimpleContainer(9) {
            @Override
            public boolean stillValid(Player player) {
                return !isRemoved() && player.distanceToSqr(DrivableVehicleEntity.this) < 64.0;
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot < 4; // strictly max 4 slots!
            }
        };
    }

    public boolean hasCargoBed() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        return def != null && def.hasCargoBed();
    }

    public boolean isCargoBedSeat(int seat) {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        if (def == null) return false;
        if (def.seats != null && seat < def.seats.size()) {
            if (def.seats.get(seat).isCargoBed) return true;
        }
        return def.hasCargoBed() && seat == 2;
    }

    public boolean supportsSpareWheel() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        if (def == null) return false;
        if (def.models != null && def.models.spareWheel != null && !def.models.spareWheel.isEmpty()) return true;
        if (def.spareWheelPos != null) return true;
        return def.hasCargoBed();
    }

    public boolean isSeatBlockedByItems(int seat) {
        if (seat <= 0) return false;

        if (isCargoBedSeat(seat)) {
            if (this.trunkInventory != null && !this.trunkInventory.isEmpty()) return true;
        }

        if (seat >= this.seatInventories.length) return false;
        SimpleContainer inv = this.seatInventories[seat];
        if (inv == null) return false;
        for (int i = 0; i < 4; i++) {
            if (!inv.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    public int getSeatItemCount(int seat) {
        if (seat <= 0) return 0;

        if (isCargoBedSeat(seat) && this.trunkInventory != null) {
            int count = 0;
            for (int i = 0; i < this.trunkInventory.getContainerSize(); i++) {
                if (!this.trunkInventory.getItem(i).isEmpty()) count++;
            }
            return count;
        }

        if (seat >= this.seatInventories.length) return 0;
        SimpleContainer inv = this.seatInventories[seat];
        if (inv == null) return 0;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (!inv.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    public SimpleContainer getSeatInventory(int seat) {
        if (seat <= 0 || seat >= this.seatInventories.length) return null;
        return this.seatInventories[seat];
    }

    public void dropSeatContents() {
        if (!this.level().isClientSide) {
            for (int s = 1; s < this.seatInventories.length; s++) {
                SimpleContainer inv = this.seatInventories[s];
                if (inv != null) {
                    for (int i = 0; i < 4; i++) {
                        ItemStack stack = inv.getItem(i);
                        if (!stack.isEmpty()) {
                            this.spawnAtLocation(stack.copy());
                            inv.setItem(i, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }

    public void openSeatInventory(Player player, int seat) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
        if (seat <= 0 || seat >= getMaxSeats() || seat >= this.seatInventories.length) {
            player.displayClientMessage(Component.literal("§c[Салон] На это сиденье нельзя класть вещи!"), true);
            return;
        }
        if (isSeatOccupiedByPassenger(seat)) {
            player.displayClientMessage(Component.literal("§c[Салон] На этом сиденье сидит игрок!"), true);
            return;
        }

        SimpleContainer inv = this.seatInventories[seat];
        if (inv == null) return;

        // Visual locked indicators in slots 4..8
        for (int i = 4; i < 9; i++) {
            ItemStack barrier = new ItemStack(net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
            barrier.setHoverName(Component.literal("§8[Заблокировано (макс. 4 слота)]"));
            inv.setItem(i, barrier);
        }

        String seatName = switch (seat) {
            case 1 -> "Переднее пассажирское";
            case 2 -> "Заднее левое";
            case 3 -> "Заднее правое";
            default -> "Сиденье " + seat;
        };

        sp.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInv, inv, 1) {
                @Override
                public boolean stillValid(Player pl) {
                    return !isRemoved() && pl.distanceToSqr(DrivableVehicleEntity.this) < 64.0;
                }

                @Override
                public ItemStack quickMoveStack(Player player, int index) {
                    if (index >= 4 && index < 9) return ItemStack.EMPTY;
                    ItemStack res = super.quickMoveStack(player, index);
                    if (!res.isEmpty() && index >= 9) {
                        DrivableVehicleEntity.this.level().playSound(null,
                            DrivableVehicleEntity.this.getX(), DrivableVehicleEntity.this.getY(), DrivableVehicleEntity.this.getZ(),
                            ModSounds.VEHICLE_ITEM_PUT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
                    }
                    return res;
                }

                @Override
                public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                    if (slotId >= 4 && slotId < 9) return;
                    ItemStack before = (slotId >= 0 && slotId < 4) ? this.slots.get(slotId).getItem().copy() : ItemStack.EMPTY;
                    super.clicked(slotId, button, clickType, player);
                    if (slotId >= 0 && slotId < 4) {
                        ItemStack after = this.slots.get(slotId).getItem();
                        if (after.getCount() > before.getCount() || (before.isEmpty() && !after.isEmpty())) {
                            DrivableVehicleEntity.this.level().playSound(null,
                                DrivableVehicleEntity.this.getX(), DrivableVehicleEntity.this.getY(), DrivableVehicleEntity.this.getZ(),
                                ModSounds.VEHICLE_ITEM_PUT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
                        }
                    }
                }
            },
            Component.literal("Вещи на сиденье: " + seatName)
        ));
    }

    public boolean sitPlayerInSeat(Player player, int seat) {
        if (player == null || isRemoved() || player.distanceToSqr(this) > 36.0) return false;
        if (seat < 0 || seat >= getMaxSeats()) return false;
        if (isSeatOccupiedByPassenger(seat)) {
            player.displayClientMessage(Component.literal("§c[Салон] Это место уже занято!"), true);
            return false;
        }
        if (isSeatBlockedByItems(seat)) {
            player.displayClientMessage(Component.literal("§c[Салон] На этом сиденье лежат вещи!"), true);
            return false;
        }

        if (isLocked() && !this.hasPassenger(player)) {
            player.displayClientMessage(Component.literal("§c[Салон] Автомобиль заперт!"), true);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CAR_DOOR_LOCKED_YANK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            return false;
        }

        if (this.hasPassenger(player)) {
            requestSeatSwitch(player, seat);
            return true;
        }

        if (this.getPassengers().size() >= this.getMaxSeats()) {
            player.displayClientMessage(Component.literal("§c[Салон] В машине нет свободных мест!"), true);
            return false;
        }

        int passIdx = this.getPassengers().size();
        player.startRiding(this, true);

        int mask = this.entityData.get(SEAT_SLOTS);
        mask &= ~(0xF << (passIdx * 4));
        mask |= (seat & 0xF) << (passIdx * 4);
        this.entityData.set(SEAT_SLOTS, mask);

        float rollDip = (seat == 0 ? 0.45F : (seat == 1 ? -0.45F : 0.0F));
        float pitchDip = (seat >= 2 ? 0.35F : -0.35F);
        applyChassisImpulse(-0.025F, pitchDip, rollDip);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.CAR_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.PLAYERS, 0.85F, 0.95F + this.random.nextFloat() * 0.12F);

        return true;
    }

    public boolean isLocked() {
        return this.entityData.get(LOCKED);
    }

    public void setLocked(boolean locked) {
        this.entityData.set(LOCKED, locked);
    }

    public boolean isTrunkLocked() {
        return this.entityData.get(TRUNK_LOCKED);
    }

    public void setTrunkLocked(boolean locked) {
        this.entityData.set(TRUNK_LOCKED, locked);
    }

    public boolean isKeyInIgnition() {
        return this.entityData.get(KEY_IN_IGNITION);
    }

    public ItemStack getIgnitionKey() {
        return this.ignitionKey;
    }

    public void setIgnitionKey(ItemStack key) {
        this.ignitionKey = (key != null) ? key : ItemStack.EMPTY;
        this.entityData.set(KEY_IN_IGNITION, !this.ignitionKey.isEmpty());
    }

    public boolean hasMatchingKeyInInventory(Player player) {
        if (player == null) return false;
        for (ItemStack st : player.getInventory().items) {
            if (com.evgen.cardecor.item.VehiclePackKeyItem.matchesVehicle(st, this)) return true;
        }
        for (ItemStack st : player.getInventory().offhand) {
            if (com.evgen.cardecor.item.VehiclePackKeyItem.matchesVehicle(st, this)) return true;
        }
        return false;
    }

    public ItemStack findMatchingKeyInInventory(Player player) {
        if (player == null) return ItemStack.EMPTY;
        for (ItemStack st : player.getInventory().items) {
            if (com.evgen.cardecor.item.VehiclePackKeyItem.matchesVehicle(st, this)) return st;
        }
        for (ItemStack st : player.getInventory().offhand) {
            if (com.evgen.cardecor.item.VehiclePackKeyItem.matchesVehicle(st, this)) return st;
        }
        return ItemStack.EMPTY;
    }

    public boolean insertKeyFromPlayer(Player player) {
        if (this.level().isClientSide || player == null) return false;
        if (isKeyInIgnition()) {
            player.displayClientMessage(Component.literal("§e[🔑] Ключ уже в замке зажигания!"), true);
            return false;
        }
        ItemStack key = findMatchingKeyInInventory(player);
        if (key.isEmpty()) {
            player.displayClientMessage(Component.literal("§c[!] У вас нет ключа от этого автомобиля!"), true);
            return false;
        }
        ItemStack inserted = key.split(1);
        setIgnitionKey(inserted);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.KEY_INSERT.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("§a[🔑] Ключ вставлен в замок зажигания."), true);
        return true;
    }

    public boolean removeKeyToPlayer(Player player) {
        if (this.level().isClientSide || player == null) return false;
        if (!isKeyInIgnition() || this.ignitionKey.isEmpty()) {
            player.displayClientMessage(Component.literal("§e[🔑] В замке зажигания нет ключа."), true);
            return false;
        }
        ItemStack key = this.ignitionKey.copy();
        setIgnitionKey(ItemStack.EMPTY);
        if (!player.getInventory().add(key)) {
            this.spawnAtLocation(key);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.KEY_REMOVE.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("§e[🔑] Ключ забран из замка зажигания."), true);
        return true;
    }

    public byte getClimateMode() {
        return this.entityData.get(CLIMATE_MODE);
    }

    public void setClimateMode(byte mode) {
        this.entityData.set(CLIMATE_MODE, mode);
    }

    public boolean hasAirConditioning() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        if (def == null) return true;
        if (def.category != null && (def.category.equalsIgnoreCase("vintage") || def.category.equalsIgnoreCase("classic") || def.category.equalsIgnoreCase("legacy"))) return false;
        if (def.hasCargoBed() && (def.id != null && def.id.contains("vintage"))) return false;
        return true;
    }

    public void toggleClimate(Player player) {
        if (!this.isEngineOn()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("§c[Климат-контроль] Заведите двигатель!"), true);
            }
            return;
        }
        byte current = getClimateMode();
        byte next;
        if (hasAirConditioning()) {
            next = (byte) ((current + 1) % 3); // 0 = OFF, 1 = HEATER, 2 = AC
        } else {
            next = (current == 0) ? (byte) 1 : (byte) 0; // Pickup: only HEATER
        }
        setClimateMode(next);
        if (next == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CAR_HEATER_START.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        } else if (next == 0 && current != 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CAR_HEATER_STOP.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        }
        if (player != null) {
            String msg = switch (next) {
                case 1 -> "§6[Климат-контроль] Печка ВКЛ (Обогрев салона)";
                case 2 -> "§b[Климат-контроль] Кондиционер ВКЛ (Охлаждение салона)";
                default -> "§7[Климат-контроль] ВЫКЛ";
            };
            player.displayClientMessage(Component.literal(msg), true);
        }
    }

    public static class DoorTarget {
        public final DrivableVehicleEntity vehicle;
        public final int seatIndex;
        public final String doorName;
        public final boolean isBlockedByItems;
        public final boolean isOccupied;
        public final boolean isLocked;

        public DoorTarget(DrivableVehicleEntity vehicle, int seatIndex, String doorName, boolean isBlockedByItems, boolean isOccupied, boolean isLocked) {
            this.vehicle = vehicle;
            this.seatIndex = seatIndex;
            this.doorName = doorName;
            this.isBlockedByItems = isBlockedByItems;
            this.isOccupied = isOccupied;
            this.isLocked = isLocked;
        }
    }

    public static class WheelTarget {
        public final DrivableVehicleEntity vehicle;
        public final int wheelIndex;
        public final String wheelName;
        public final boolean isSpare;

        public WheelTarget(DrivableVehicleEntity vehicle, int wheelIndex, String wheelName, boolean isSpare) {
            this.vehicle = vehicle;
            this.wheelIndex = wheelIndex;
            this.wheelName = wheelName;
            this.isSpare = isSpare;
        }
    }

    public static class TrunkTarget {
        public final DrivableVehicleEntity vehicle;
        public final boolean isLocked;

        public TrunkTarget(DrivableVehicleEntity vehicle, boolean isLocked) {
            this.vehicle = vehicle;
            this.isLocked = isLocked;
        }
    }

    public static class HoodTarget {
        public final DrivableVehicleEntity vehicle;

        public HoodTarget(DrivableVehicleEntity vehicle) {
            this.vehicle = vehicle;
        }
    }

    public static class PushTarget {
        public final DrivableVehicleEntity vehicle;
        public final boolean isRear;

        public PushTarget(DrivableVehicleEntity vehicle, boolean isRear) {
            this.vehicle = vehicle;
            this.isRear = isRear;
        }
    }

    /**
     * Determines which vehicle door the player is looking at from the outside.
     * Returns null if player is not within reach (~3.3m) or not aiming at a door.
     */
    public DoorTarget getTargetedDoor(Player player) {
        if (player == null || player.isPassenger()) return null;
        if (player.distanceToSqr(this) > 20.0) return null;

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = (def != null && def.hasCargoBed());

        double halfWidth   = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (isPickup ? 1.15 : 1.25);
        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? cb.cabinFrontZ : (isPickup ? 1.05 : 0.85);
        double cabinRearZ  = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : (isPickup ? -0.75 : -0.75);
        double hoodHeight  = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (isPickup ? 1.46 : 1.48);
        double rearDist    = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (isPickup ? 2.90 : 3.25);
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : (isPickup ? 1.38 : 1.35);

        float carYaw = this.getYRot();
        float yawRad = (float) Math.toRadians(carYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double fwdX = -sinY;
        double fwdZ = cosY;
        double rightX = cosY;
        double rightZ = sinY;

        double carX = this.getX();
        double carY = this.getY();
        double carZ = this.getZ();

        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(2.8));

        double sDx = eye.x - carX, sDy = eye.y - carY, sDz = eye.z - carZ;
        double eDx = reach.x - carX, eDy = reach.y - carY, eDz = reach.z - carZ;
        Vec3 lStart = new Vec3(sDx * rightX + sDz * rightZ, sDy, sDx * fwdX + sDz * fwdZ);
        Vec3 lEnd   = new Vec3(eDx * rightX + eDz * rightZ, eDy, eDx * fwdX + eDz * fwdZ);

        double doorBaseY = 0.35;
        double doorTopY = Math.min(1.15, hoodHeight - 0.10);

        int maxSeats = this.getMaxSeats();
        double closestDistSq = Double.MAX_VALUE;
        int bestSeat = -1;
        String bestName = "";

        for (int s = 0; s < maxSeats; s++) {
            VehicleDefinition.SeatDef sDef = (def != null && def.seats != null && s < def.seats.size()) ? def.seats.get(s) : null;
            AABB doorBox;
            String name;

            if (isCargoBedSeat(s)) {
                // Cargo bed / trunk seat
                doorBox = new AABB(-halfWidth * 0.90, doorBaseY, -rearDist, halfWidth * 0.90, trunkHeight + 0.45, cabinRearZ);
                name = (sDef != null && sDef.name != null) ? sDef.name : "Багажник";
            } else {
                Vec3 sPos = getSeatWorldPosition(s);
                double sRelX = (sPos.x - carX) * rightX + (sPos.z - carZ) * rightZ;
                double sRelZ = (sPos.x - carX) * fwdX + (sPos.z - carZ) * fwdZ;
                boolean isLeftSide = (sRelX < 0.0);
                double flankMinX = isLeftSide ? (-halfWidth - 0.40) : (halfWidth - 0.25);
                double flankMaxX = isLeftSide ? (-halfWidth + 0.25) : (halfWidth + 0.40);
                double doorZMin = sRelZ - 0.60;
                double doorZMax = sRelZ + 0.60;
                doorBox = new AABB(flankMinX, doorBaseY, doorZMin, flankMaxX, doorTopY, doorZMax);
                name = (sDef != null && sDef.name != null) ? sDef.name : ("Место " + (s + 1));
            }

            Optional<Vec3> clip = doorBox.clip(lStart, lEnd);
            if (clip.isPresent()) {
                double d = lStart.distanceToSqr(clip.get());
                if (d < closestDistSq) {
                    closestDistSq = d;
                    bestSeat = s;
                    bestName = name;
                }
            }
        }

        if (bestSeat >= 0) {
            boolean blocked = isSeatBlockedByItems(bestSeat);
            boolean occupied = isSeatOccupiedByPassenger(bestSeat);
            boolean locked = isCargoBedSeat(bestSeat) ? isTrunkLocked() : isLocked();
            return new DoorTarget(this, bestSeat, bestName, blocked, occupied, locked);
        }

        return null;
    }

    public static DoorTarget findTargetedDoor(Player player) {
        if (player == null || player.isPassenger()) return null;
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(4.0);
        java.util.List<DrivableVehicleEntity> vehicles = player.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        for (DrivableVehicleEntity v : vehicles) {
            DoorTarget target = v.getTargetedDoor(player);
            if (target != null) return target;
        }
        return null;
    }

    public WheelTarget getTargetedWheel(Player player) {
        if (player == null || player.isPassenger()) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(3.2));
        double closestDistSq = Double.MAX_VALUE;
        WheelTarget best = null;

        String[] wheelNames = {"Переднее левое", "Переднее правое", "Заднее левое", "Заднее правое"};
        for (int i = 0; i < 4; i++) {
            if (!isWheelInstalled(i)) continue;
            Vec3 wp = getWheelWorldPos(i);
            if (eye.distanceTo(wp) > 3.2) continue;
            AABB box = new AABB(wp.x - 0.45, wp.y - 0.45, wp.z - 0.45, wp.x + 0.45, wp.y + 0.45, wp.z + 0.45);
            Optional<Vec3> clip = box.clip(eye, reach);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d < closestDistSq) {
                    closestDistSq = d;
                    best = new WheelTarget(this, i, wheelNames[i], false);
                }
            }
        }

        if (supportsSpareWheel() && hasSpareWheel()) {
            Vec3 spPos = getSpareWheelWorldPos();
            if (eye.distanceTo(spPos) <= 3.2) {
                AABB spBox = new AABB(spPos.x - 0.45, spPos.y - 0.45, spPos.z - 0.45, spPos.x + 0.45, spPos.y + 0.45, spPos.z + 0.45);
                Optional<Vec3> clip = spBox.clip(eye, reach);
                if (clip.isPresent()) {
                    double d = eye.distanceToSqr(clip.get());
                    if (d < closestDistSq) {
                        closestDistSq = d;
                        best = new WheelTarget(this, 4, "Запасное", true);
                    }
                }
            }
        }

        return best;
    }

    public static WheelTarget findTargetedWheel(Player player) {
        if (player == null || player.isPassenger()) return null;
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(4.0);
        java.util.List<DrivableVehicleEntity> vehicles = player.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        WheelTarget closestTarget = null;
        double closestDist = Double.MAX_VALUE;
        for (DrivableVehicleEntity v : vehicles) {
            WheelTarget target = v.getTargetedWheel(player);
            if (target != null) {
                double d = player.distanceToSqr(v);
                if (d < closestDist) {
                    closestDist = d;
                    closestTarget = target;
                }
            }
        }
        return closestTarget;
    }

    public TrunkTarget getTargetedTrunk(Player player) {
        if (player == null || player.isPassenger()) return null;
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        if (def != null && def.hasCargoBed()) return null; // Pickup bed / cargo is handled by DoorTarget

        double rearDist = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : 3.25;
        double cabinRearZ = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : -0.75;
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : 1.25;
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : 1.35;

        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(3.2));
        float yawR = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawR);
        double fwdZ = Math.cos(yawR);
        double rightX = Math.cos(yawR);
        double rightZ = Math.sin(yawR);

        double sDx = eye.x - this.getX(), sDy = eye.y - this.getY(), sDz = eye.z - this.getZ();
        double eDx = reach.x - this.getX(), eDy = reach.y - this.getY(), eDz = reach.z - this.getZ();
        Vec3 lStart = new Vec3(sDx * rightX + sDz * rightZ, sDy, sDx * fwdX + sDz * fwdZ);
        Vec3 lEnd   = new Vec3(eDx * rightX + eDz * rightZ, eDy, eDx * fwdX + eDz * fwdZ);

        AABB trunkBox = new AABB(-halfWidth * 0.90, 0.25, -rearDist, halfWidth * 0.90, trunkHeight + 0.35, cabinRearZ);
        if (trunkBox.clip(lStart, lEnd).isPresent()) {
            return new TrunkTarget(this, isTrunkLocked());
        }
        return null;
    }

    public static TrunkTarget findTargetedTrunk(Player player) {
        if (player == null || player.isPassenger()) return null;
        AABB box = player.getBoundingBox().inflate(4.0);
        List<DrivableVehicleEntity> vehicles = player.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        for (DrivableVehicleEntity v : vehicles) {
            TrunkTarget target = v.getTargetedTrunk(player);
            if (target != null) return target;
        }
        return null;
    }

    public HoodTarget getTargetedHood(Player player) {
        if (player == null || player.isPassenger()) return null;
        if (this.getTargetedDoor(player) != null) return null; // Door always takes strict priority!

        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(3.2));

        // 1. Check partHood entity bounding box if available
        if (this.partHood != null) {
            AABB phBox = this.partHood.getBoundingBox().inflate(0.15);
            if (phBox.clip(eye, reach).isPresent()) {
                return new HoodTarget(this);
            }
        }

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean hasCargoBed = hasCargoBed();
        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (hasCargoBed ? 2.90 : 3.25);
        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? cb.cabinFrontZ : (hasCargoBed ? 0.65 : 0.85);
        double hoodHeight = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (hasCargoBed ? 1.46 : 1.48);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (hasCargoBed ? 1.15 : 1.25);

        float yawR = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawR);
        double fwdZ = Math.cos(yawR);
        double rightX = Math.cos(yawR);
        double rightZ = Math.sin(yawR);

        double sDx = eye.x - this.getX(), sDy = eye.y - this.getY(), sDz = eye.z - this.getZ();
        double eDx = reach.x - this.getX(), eDy = reach.y - this.getY(), eDz = reach.z - this.getZ();
        Vec3 lStart = new Vec3(sDx * rightX + sDz * rightZ, sDy, sDx * fwdX + sDz * fwdZ);
        Vec3 lEnd   = new Vec3(eDx * rightX + eDz * rightZ, eDy, eDx * fwdX + eDz * fwdZ);

        double hoodMinZ = cabinFrontZ + 0.05;
        double hoodMaxZ = frontDist + 0.35;
        AABB hoodBox = new AABB(-halfWidth * 0.90, 0.20, hoodMinZ, halfWidth * 0.90, hoodHeight + 0.40, hoodMaxZ);
        if (hoodBox.clip(lStart, lEnd).isPresent()) {
            return new HoodTarget(this);
        }

        Vec3 look = player.getViewVector(1.0F);
        double lookFwd = look.x * fwdX + look.z * fwdZ;
        if (lStart.z >= hoodMinZ && lStart.z <= frontDist + 2.0 && Math.abs(lStart.x) <= halfWidth * 0.90 && lookFwd < -0.20) {
            return new HoodTarget(this);
        }

        return null;
    }

    public static HoodTarget findTargetedHood(Player player) {
        if (player == null || player.isPassenger()) return null;
        AABB box = player.getBoundingBox().inflate(5.5);
        List<DrivableVehicleEntity> vehicles = player.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        for (DrivableVehicleEntity v : vehicles) {
            HoodTarget target = v.getTargetedHood(player);
            if (target != null) return target;
        }
        List<com.evgen.cardecor.entity.VehiclePartEntity> parts = player.level().getEntitiesOfClass(com.evgen.cardecor.entity.VehiclePartEntity.class, box);
        for (com.evgen.cardecor.entity.VehiclePartEntity part : parts) {
            if (part.getParent() != null) {
                HoodTarget target = part.getParent().getTargetedHood(player);
                if (target != null) return target;
            }
        }
        return null;
    }

    public PushTarget getPushTarget(Player player) {
        if (player == null || player.isPassenger()) return null;
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));
        double rearDist = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (isPickup ? 2.90 : 3.25);
        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (isPickup ? 2.50 : 2.50);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (isPickup ? 1.15 : 1.25);

        float radY = (float) Math.toRadians(this.getYRot());
        double fX = -Math.sin(radY);
        double fZ = Math.cos(radY);
        double rX = Math.cos(radY);
        double rZ = Math.sin(radY);

        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dy = Math.abs(player.getY() - this.getY());
        if (dy > 1.8) return null;

        double localLat = dx * rX + dz * rZ;
        double localFwd = dx * fX + dz * fZ;
        if (Math.abs(localLat) > halfWidth + 0.35) return null;

        Vec3 look = player.getViewVector(1.0F);
        double lookDotFwd = look.x * fX + look.z * fZ;

        // Near rear bumper facing forward
        if (localFwd >= -rearDist - 1.25 && localFwd <= -rearDist + 0.40 && lookDotFwd > 0.20) {
            return new PushTarget(this, true);
        }
        // Near front bumper facing backward
        if (localFwd <= frontDist + 1.25 && localFwd >= frontDist - 0.40 && lookDotFwd < -0.20) {
            return new PushTarget(this, false);
        }
        return null;
    }

    public static PushTarget findPushTarget(Player player) {
        if (player == null || player.isPassenger()) return null;
        AABB box = player.getBoundingBox().inflate(3.5);
        List<DrivableVehicleEntity> vehicles = player.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        for (DrivableVehicleEntity v : vehicles) {
            PushTarget target = v.getPushTarget(player);
            if (target != null) return target;
        }
        return null;
    }

    public void openMechanicsMenu(Player player) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.9F, 1.2F);
        if (player instanceof ServerPlayer sp) {
            com.evgen.cardecor.network.ModNetwork.sendToPlayer(sp, new com.evgen.cardecor.network.OpenVehicleMechanicsPacket(this.getId()));
        } else if (this.level().isClientSide) {
            com.evgen.cardecor.client.ClientPacketHandlers.handleOpenVehicleMechanics(this.getId());
        }
    }

    public void openTrunk(Player player) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
        boolean isOccupant = (player.getVehicle() == this) || this.hasPassenger(player);
        if (isOccupant) {
            player.displayClientMessage(Component.literal("§c[Багажник] Багажник открывается только снаружи!"), true);
            return;
        }
        if (isTrunkLocked()) {
            player.displayClientMessage(Component.literal("§c[Багажник] Багажник заперт на замок!"), true);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CAR_DOOR_LOCKED_YANK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            return;
        }

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        boolean hasBed = (def != null && def.hasCargoBed());
        final int rows = (def != null && def.trunk != null) ? Mth.clamp(def.trunk.rows, 1, 6) : (hasBed ? 6 : 3);

        MenuType<?> menuType = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };

        String title = "Багажник (" + (def != null && def.name != null ? def.name : "Автомобиль") + ")";

        SoundEvent openSound = hasBed ? ModSounds.TRUNK_OPEN_PICKUP.get() : ModSounds.TRUNK_OPEN_SEDAN.get();
        SoundEvent closeSound = hasBed ? ModSounds.TRUNK_CLOSE_PICKUP.get() : ModSounds.TRUNK_CLOSE_SEDAN.get();

        sp.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new ChestMenu(menuType, syncId, playerInv, this.trunkInventory, rows) {
                @Override
                public ItemStack quickMoveStack(Player player, int index) {
                    ItemStack res = super.quickMoveStack(player, index);
                    if (!res.isEmpty() && index >= rows * 9) {
                        DrivableVehicleEntity.this.level().playSound(null,
                            DrivableVehicleEntity.this.getX(), DrivableVehicleEntity.this.getY(), DrivableVehicleEntity.this.getZ(),
                            ModSounds.VEHICLE_ITEM_PUT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
                    }
                    return res;
                }

                @Override
                public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                    ItemStack before = (slotId >= 0 && slotId < rows * 9) ? this.slots.get(slotId).getItem().copy() : ItemStack.EMPTY;
                    super.clicked(slotId, button, clickType, player);
                    if (slotId >= 0 && slotId < rows * 9) {
                        ItemStack after = this.slots.get(slotId).getItem();
                        if (after.getCount() > before.getCount() || (before.isEmpty() && !after.isEmpty())) {
                            DrivableVehicleEntity.this.level().playSound(null,
                                DrivableVehicleEntity.this.getX(), DrivableVehicleEntity.this.getY(), DrivableVehicleEntity.this.getZ(),
                                ModSounds.VEHICLE_ITEM_PUT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
                        }
                    }
                }

                @Override
                public void removed(Player pClosing) {
                    super.removed(pClosing);
                    if (!DrivableVehicleEntity.this.isRemoved()) {
                        DrivableVehicleEntity.this.entityData.set(TRUNK_ANGLE, 0.0F);
                        DrivableVehicleEntity.this.level().playSound(null,
                            DrivableVehicleEntity.this.getX(), DrivableVehicleEntity.this.getY(), DrivableVehicleEntity.this.getZ(),
                            closeSound, SoundSource.PLAYERS, 1.10F, 1.0F);
                    }
                }
            },
            Component.literal(title)
        ));

        this.entityData.set(TRUNK_ANGLE, 65.0F);
        this.trunkCloseTimer = 0; // Stays open while menu is open

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            openSound, SoundSource.PLAYERS, 1.10F, 1.0F);
    }

    private void handleMobAttackingVehicle(net.minecraft.world.entity.Mob mob) {
        long now = this.level().getGameTime();
        long lastHit = mobAttackCooldowns.getOrDefault(mob.getId(), 0L);
        if (now - lastHit >= 22) { // ~1.1 second attack rate per mob
            mobAttackCooldowns.put(mob.getId(), now);
            mob.swing(InteractionHand.MAIN_HAND);

            // Metal attack sound (banging on car body/door)
            this.level().playSound(null, mob.getX(), mob.getY() + 0.5, mob.getZ(),
                SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 0.70F, 0.85F + this.random.nextFloat() * 0.30F);

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.15, 0.15, 0.15, 0.05);
            }

            // Low chance (20%) to inflict 1-2 HP damage to vehicle HP
            if (this.random.nextFloat() < 0.20F) {
                float dmg = 1.0F + this.random.nextFloat(); // 1.0 to 2.0 HP
                float curHp = getVehicleHealth();
                if (curHp > 0.0F) {
                    setVehicleHealth(Math.max(0.0F, curHp - dmg));
                }
            }
        }
    }

    private void updateHeadlightDynamicIllumination() {
        // Blocks.LIGHT block spawning is permanently removed in favor of client-side deferred depth-reconstructed shaders.
        // Clean up any lingering legacy light blocks if present.
        if (!activeHeadlightBlocks.isEmpty()) {
        clearDynamicLight();
        }
    }

    private void clearDynamicLight() {
        if (!this.level().isClientSide) {
            for (BlockPos pos : activeHeadlightBlocks) {
                if (this.level().hasChunkAt(pos) && this.level().getBlockState(pos).is(Blocks.LIGHT)) {
                    this.level().removeBlock(pos, false);
                }
            }
            activeHeadlightBlocks.clear();
            lastHeadlightPos = null;
        }
    }

    private void updateCabinLight() {
        if (this.level().isClientSide) return;
        BlockPos targetPos = BlockPos.containing(this.getX(), this.getY() + 1.25, this.getZ());
        if (activeCabinLightPos != null && !activeCabinLightPos.equals(targetPos)) {
            clearCabinLight();
        }
        if (this.level().hasChunkAt(targetPos)) {
            BlockState state = this.level().getBlockState(targetPos);
            if (state.isAir()) {
                this.level().setBlock(targetPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 8), 3);
                activeCabinLightPos = targetPos;
            } else if (state.is(Blocks.LIGHT)) {
                if (state.getValue(LightBlock.LEVEL) != 8) {
                    this.level().setBlock(targetPos, state.setValue(LightBlock.LEVEL, 8), 3);
                }
                activeCabinLightPos = targetPos;
            }
        }
    }

    private void clearCabinLight() {
        if (!this.level().isClientSide && activeCabinLightPos != null) {
            if (this.level().hasChunkAt(activeCabinLightPos) && this.level().getBlockState(activeCabinLightPos).is(Blocks.LIGHT)) {
                this.level().removeBlock(activeCabinLightPos, false);
            }
            activeCabinLightPos = null;
        }
    }

    public boolean physicsBodyRegistered = false;

    public void ensurePhysicsBodyRegistered() {
        if (!this.odeBodyRegistered && this.level() != null && !this.level().isClientSide) {
            com.evgen.cardecor.physics.ode.OdePhysicsWorld world = com.evgen.cardecor.physics.ode.OdePhysicsWorld.get(this.level());
            this.odeBody.init(world.getWorld(), world.getSpace(), this, getX(), getY(), getZ(), getYRot());
            VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
            if (def != null) {
                this.odeBody.configureFromDefinition(def);
                if (def.physics != null && def.physics.drivetrain != null && !def.physics.drivetrain.isEmpty()) {
                    this.physics.drivetrain = def.physics.drivetrain;
                }
            }
            world.addVehicle(this.odeBody);
            this.odeBodyRegistered = true;
        }
    }

    public org.joml.Quaternionf getSmoothRotation(float partialTick, org.joml.Quaternionf dest) {
        if (this.level().isClientSide) {
            return this.prevRotationQuat.slerp(this.currentRotationQuat, partialTick, dest);
        } else {
            return this.odeBody.getSmoothRotation(partialTick, dest);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.odeBodyRegistered && !this.level().isClientSide) {
            com.evgen.cardecor.physics.ode.OdePhysicsWorld.get(this.level()).removeVehicle(this.odeBody);
            this.odeBodyRegistered = false;
        }
        clearDynamicLight();
        clearCabinLight();
        super.remove(reason);
    }

    public int getHeadlightMode() {
        return this.entityData.get(HEADLIGHT_MODE);
    }

    public void setHeadlightMode(int mode) {
        int m = Mth.clamp(mode, 0, 2);
        this.entityData.set(HEADLIGHT_MODE, m);
        this.entityData.set(HEADLIGHTS_ON, m > 0);
    }

    public boolean isHeadlightsOn() {
        return getHeadlightMode() > 0 || this.entityData.get(HEADLIGHTS_ON);
    }

    public void toggleHeadlights() {
        int cur = getHeadlightMode();
        int next = (cur > 0) ? 0 : 1;
        setHeadlightMode(next);
        clearDynamicLight();

        if (next == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.HEADLIGHTS_OFF.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        } else {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.HEADLIGHTS_ON.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        }
    }

    public boolean isCabinLightOn() {
        return this.entityData.get(CABIN_LIGHT_ON);
    }

    public void setCabinLightOn(boolean on) {
        this.entityData.set(CABIN_LIGHT_ON, on);
        if (!on) {
            clearCabinLight();
        } else {
            updateCabinLight();
        }
    }

    public void toggleCabinLight() {
        setCabinLightOn(!isCabinLightOn());
    }



    public boolean isEngineOn() {
        return this.entityData.get(ENGINE_ON);
    }

    public void setEngineOn(boolean on) {
        this.entityData.set(ENGINE_ON, on);
    }

    public boolean isTirePunctured(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return false;
        return (this.entityData.get(TIRE_STATUS) & (1 << wheelIndex)) != 0;
    }

    public void setTirePunctured(int wheelIndex, boolean punctured) {
        if (wheelIndex < 0 || wheelIndex >= 4) return;
        byte t = this.entityData.get(TIRE_STATUS);
        if (punctured) {
            t = (byte) (t | (1 << wheelIndex));
            if (this.physics != null && this.physics.tireAir != null) {
                this.physics.tireAir[wheelIndex] = 0.0f;
            }
        } else {
            t = (byte) (t & ~(1 << wheelIndex));
            if (this.physics != null && this.physics.tireAir != null) {
                this.physics.tireAir[wheelIndex] = 1.0f;
            }
        }
        this.entityData.set(TIRE_STATUS, t);
    }



    public byte getTireStatus() {

        return this.entityData.get(TIRE_STATUS);

    }



    public float getImpactShake() {

        return this.entityData.get(IMPACT_SHAKE);

    }



    public int getCurrentGear() {

        return this.entityData.get(GEAR);

    }



    public float getFuel() {

        return this.entityData.get(FUEL);

    }



    public void setFuel(float fuel) {
        this.entityData.set(FUEL, Mth.clamp(fuel, 0.0F, MAX_FUEL));
    }

    public float getSuspensionCompression(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0.0F;
        if (this.odeBody != null) {
            return this.odeBody.wheelCompression[wheelIndex];
        }
        return (float) this.physics.suspensionCompressions[wheelIndex];
    }

    public float getSmoothedSuspensionCompression(int wheelIndex, float dtFrame) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0.0F;
        if (this.odeBody != null) {
            float target = this.odeBody.wheelCompression[wheelIndex];
            return (float) this.odeBody.clientHubFilters[wheelIndex].step(target, 2.0 * Math.PI * 15.0, dtFrame);
        }
        return getSuspensionCompression(wheelIndex);
    }

    public float getWheelCamberRad(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0.0F;
        if (this.odeBody != null) {
            return this.odeBody.wheelCamberRad[wheelIndex];
        }
        return 0.0F;
    }

    public float getSmoothedCamberRad(int wheelIndex, float dtFrame) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0.0F;
        if (this.odeBody != null) {
            float target = this.odeBody.wheelCamberRad[wheelIndex];
            return (float) this.odeBody.clientCamberFilters[wheelIndex].step(target, 2.0 * Math.PI * 15.0, dtFrame);
        }
        return 0.0F;
    }

    public float getWheelContactPatch(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0.0F;
        return (float) this.physics.getWheelContactPatch(wheelIndex);
    }

    public boolean isWheelLifted(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return false;
        return this.physics.isWheelLifted(wheelIndex);
    }



    public float getVehicleHealth() {

        return this.entityData.get(VEHICLE_HP);

    }



    public static boolean isExplosiveDamage(DamageSource source) {
        if (source == null) return false;

        if (source.is(DamageTypeTags.IS_EXPLOSION)) return true;

        String msgId = source.getMsgId().toLowerCase();
        if (msgId.contains("explosion") || msgId.contains("blast") || msgId.contains("bomb") ||
            msgId.contains("grenade") || msgId.contains("rocket") || msgId.contains("missile")) {
            return true;
        }

        Entity direct = source.getDirectEntity();
        if (direct != null) {
            String directType = direct.getType().getDescriptionId().toLowerCase();
            String directClass = direct.getClass().getName().toLowerCase();

            if (directType.contains("rpg") || directType.contains("rocket") || directType.contains("missile") ||
                directType.contains("grenade") || directType.contains("explosive") || directType.contains("c4") ||
                directType.contains("bomb") ||
                directClass.contains("rpg") || directClass.contains("rocket") || directClass.contains("missile") ||
                directClass.contains("grenade") || directClass.contains("explosive") || directClass.contains("bomb")) {
                return true;
            }

            // Timeless and Classics: Zero (TACZ) projectile support
            if (directClass.contains("tacz") || directClass.contains("entitykineticbullet")) {
                try {
                    // Check getGunId()
                    java.lang.reflect.Method mGun = direct.getClass().getMethod("getGunId");
                    Object gunId = mGun.invoke(direct);
                    if (gunId != null) {
                        String gStr = gunId.toString().toLowerCase();
                        if (gStr.contains("rpg") || gStr.contains("rocket") || gStr.contains("missile") ||
                            gStr.contains("grenade") || gStr.contains("m320") || gStr.contains("gp25") ||
                            gStr.contains("explosive") || gStr.contains("gl_") || gStr.contains("_gl")) {
                            return true;
                        }
                    }
                    // Check getAmmoId()
                    java.lang.reflect.Method mAmmo = direct.getClass().getMethod("getAmmoId");
                    Object ammoId = mAmmo.invoke(direct);
                    if (ammoId != null) {
                        String aStr = ammoId.toString().toLowerCase();
                        if (aStr.contains("rpg") || aStr.contains("rocket") || aStr.contains("missile") ||
                            aStr.contains("grenade") || aStr.contains("40mm") || aStr.contains("explosive") ||
                            aStr.contains("he") || aStr.contains("frag")) {
                            return true;
                        }
                    }
                    // Check boolean explosion field
                    java.lang.reflect.Field fExplosion = direct.getClass().getDeclaredField("explosion");
                    fExplosion.setAccessible(true);
                    if (fExplosion.getBoolean(direct)) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        Entity attacker = source.getEntity();
        if (attacker != null) {
            String attackerClass = attacker.getClass().getName().toLowerCase();
            if (attackerClass.contains("tnt") || attackerClass.contains("creeper") || attackerClass.contains("missile")) {
                return true;
            }
        }

        return false;
    }



    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        wakeUp();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        Entity shooter = (direct instanceof net.minecraft.world.entity.projectile.Projectile proj) ? proj.getOwner() : attacker;
        if (shooter == null && direct != null) {
            for (Entity p : this.getPassengers()) {
                if (p.distanceToSqr(direct) < 36.0) { shooter = p; break; }
            }
            if (shooter == null) {
                for (int id : this.currentDeckEntityIds) {
                    Entity ent = this.level().getEntity(id);
                    if (ent != null && ent.distanceToSqr(direct) < 36.0) { shooter = ent; break; }
                }
            }
            if (shooter == null) {
                for (int id : this.airborneDeckEntityIds) {
                    Entity ent = this.level().getEntity(id);
                    if (ent != null && ent.distanceToSqr(direct) < 36.0) { shooter = ent; break; }
                }
            }
        }
        if (shooter != null && this.isShooterOnVehicle(shooter)) {
            if (!(shooter instanceof LivingEntity living && living.getXRot() >= 80.0F)) {
                if (direct != null) {
                    com.evgen.cardecor.compat.TaczCompat.incrementBulletPierce(direct);
                }
                return false;
            }
        }
        return this.processBallisticHit(source, amount, null);
    }

    public boolean hurtPart(VehiclePartEntity part, DamageSource source, float amount) {
        return this.processBallisticHit(source, amount, part);
    }

    public boolean processBallisticHit(DamageSource source, float amount, VehiclePartEntity hitPart) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        if (this.isInvulnerableTo(source) || this.isRemoved()) return false;
        if (this.level().isClientSide) return true;

        // 1. Explosive damage handling
        if (isExplosiveDamage(source)) {
            Vec3 pos = (hitPart != null) ? hitPart.position() : ((source.getSourcePosition() != null) ? source.getSourcePosition() : this.position());
            return this.handleExplosionDamage(source, amount, pos);
        }

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        VehicleDefinition.HitboxConfig cfg = (def != null && def.hitboxes != null) ? def.hitboxes : new VehicleDefinition.HitboxConfig();
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));

        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (isPickup ? 2.90 : 3.25);
        double rearDist  = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (isPickup ? 2.90 : 3.25);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (isPickup ? 1.15 : 1.25);
        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? cb.cabinFrontZ : (isPickup ? 1.05 : 0.85);
        double cabinRearZ  = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : (isPickup ? -0.75 : -0.75);
        double hoodHeight  = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (isPickup ? 1.46 : 1.48);
        double roofHeight  = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (isPickup ? 2.48 : 2.45);
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : (isPickup ? 1.38 : 1.35);

        float yawRad = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double carX = this.getX();
        double carY = this.getY();
        double carZ = this.getZ();

        Entity attacker = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        // Occupant shooting protection: firing weapons outward from cargo bed or seats does NOT damage vehicle
        Entity shooter = (directEntity instanceof net.minecraft.world.entity.projectile.Projectile proj) ? proj.getOwner() : attacker;
        if (shooter == null && directEntity != null) {
            for (Entity p : this.getPassengers()) {
                if (p.distanceToSqr(directEntity) < 36.0) { shooter = p; break; }
            }
            if (shooter == null) {
                for (int id : this.currentDeckEntityIds) {
                    Entity ent = this.level().getEntity(id);
                    if (ent != null && ent.distanceToSqr(directEntity) < 36.0) { shooter = ent; break; }
                }
            }
            if (shooter == null) {
                for (int id : this.airborneDeckEntityIds) {
                    Entity ent = this.level().getEntity(id);
                    if (ent != null && ent.distanceToSqr(directEntity) < 36.0) { shooter = ent; break; }
                }
            }
        }
        if (shooter != null && this.isShooterOnVehicle(shooter)) {
            // Allow damage only if shooting almost straight down (xRot > 80°) - practically impossible from normal combat aim
            if (!(shooter instanceof LivingEntity living && living.getXRot() >= 80.0F)) {
                if (directEntity != null) {
                    com.evgen.cardecor.compat.TaczCompat.incrementBulletPierce(directEntity);
                }
                return false; // Outward shot passes through without hitting or damaging own vehicle
            }
        }

        int hitZone = -1; // 0..3: Wheels, 4: Roof, 5: Window, 6: Hood, 7: Trunk, 8: Doors/Cabin, 9: Chassis, 10: Spare Wheel
        Vec3 worldHitPoint = null;

        // 1. Raycast trajectory from projectile or attacker
        Vec3 rayStart = null;
        Vec3 rayEnd = null;

        if (directEntity != null && directEntity != attacker) {
            Vec3 projPos = directEntity.position();
            Vec3 projVel = directEntity.getDeltaMovement();
            if (projVel.lengthSqr() > 0.001) {
                rayStart = projPos.subtract(projVel.scale(5.0));
                rayEnd = projPos.add(projVel.scale(3.0));
            } else {
                rayStart = projPos.subtract(0, 0.25, 0);
                rayEnd = projPos.add(0, 0.25, 0);
            }
        } else if (attacker instanceof LivingEntity living) {
            Vec3 eye = living.getEyePosition();
            Vec3 look = living.getViewVector(1.0F);
            rayStart = eye;
            rayEnd = eye.add(look.scale(120.0));
        }

        // 2. High-Precision 3D Local OBB Raycast across all vehicle components
        if (rayStart != null && rayEnd != null) {
            double sDx = rayStart.x - carX, sDy = rayStart.y - carY, sDz = rayStart.z - carZ;
            double eDx = rayEnd.x - carX,   eDy = rayEnd.y - carY,   eDz = rayEnd.z - carZ;
            Vec3 lStart = new Vec3(sDx * rightX + sDz * rightZ, sDy, sDx * fwdX + sDz * fwdZ);
            Vec3 lEnd   = new Vec3(eDx * rightX + eDz * rightZ, eDy, eDx * fwdX + eDz * fwdZ);

            double minDistanceSq = Double.MAX_VALUE;
            Vec3 closestLocalHit = null;

            // 4 Wheels: exact world coordinates projected into vehicle local frame
            // Guarantees zero coordinate inversion bugs!
            float r = (def != null && def.wheels != null) ? (float) def.wheels.radius : 0.42F;
            float halfTireW = 0.28F;
            float rBox = r * 1.15F;

            for (int i = 0; i < 4; i++) {
                Vec3 wp = this.getWheelWorldPos(i);
                double wDx = wp.x - carX;
                double wDy = wp.y - carY;
                double wDz = wp.z - carZ;
                double localWheelX = wDx * rightX + wDz * rightZ;
                double localWheelY = wDy;
                double localWheelZ = wDx * fwdX + wDz * fwdZ;

                AABB wBox = new AABB(localWheelX - halfTireW, localWheelY - rBox, localWheelZ - rBox, localWheelX + halfTireW, localWheelY + rBox, localWheelZ + rBox);
                Optional<Vec3> clip = wBox.clip(lStart, lEnd);
                if (clip.isPresent()) {
                    double d = lStart.distanceToSqr(clip.get());
                    if (d < minDistanceSq) {
                        minDistanceSq = d;
                        hitZone = i;
                        closestLocalHit = clip.get();
                    }
                }
            }

            // Spare Wheel (if pickup)
            if (isPickup) {
                AABB spBox = new AABB(0.03 - 0.44, 1.36 - 0.44, -0.38 - 0.44, 0.03 + 0.44, 1.36 + 0.44, -0.38 + 0.44);
                Optional<Vec3> spClip = spBox.clip(lStart, lEnd);
                if (spClip.isPresent()) {
                    double d = lStart.distanceToSqr(spClip.get());
                    if (d < minDistanceSq) {
                        minDistanceSq = d;
                        hitZone = 10;
                        closestLocalHit = spClip.get();
                    }
                }
            }

            // Roof
            AABB roofBox = new AABB(-halfWidth * 0.90, roofHeight - 0.24, cabinRearZ, halfWidth * 0.90, roofHeight + 0.12, cabinFrontZ);
            Optional<Vec3> roofClip = roofBox.clip(lStart, lEnd);
            if (roofClip.isPresent()) {
                double d = lStart.distanceToSqr(roofClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 4; closestLocalHit = roofClip.get(); }
            }

            // Window Glass
            AABB winBox = new AABB(-halfWidth * 0.94, hoodHeight - 0.05, cabinRearZ, halfWidth * 0.94, roofHeight - 0.24, cabinFrontZ);
            Optional<Vec3> winClip = winBox.clip(lStart, lEnd);
            if (winClip.isPresent()) {
                double d = lStart.distanceToSqr(winClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 5; closestLocalHit = winClip.get(); }
            }

            // Hood
            AABB hoodBox = new AABB(-halfWidth * 0.94, 0.20, cabinFrontZ, halfWidth * 0.94, hoodHeight + 0.06, frontDist);
            Optional<Vec3> hoodClip = hoodBox.clip(lStart, lEnd);
            if (hoodClip.isPresent()) {
                double d = lStart.distanceToSqr(hoodClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 6; closestLocalHit = hoodClip.get(); }
            }

            // Trunk / Cargo Bed
            // For pickup trucks: only the FLOOR of the cargo bed is a solid panel (thin slab).
            // The open air space above the floor is completely clear — no invisible block!
            double bedFloorHeight = (cb != null && cb.bedFloorHeight > 0.1) ? cb.bedFloorHeight : 0.78;
            boolean hasCargoBed = isPickup || (cb != null && cb.hasCargoBed);
            double trunkBoxTop = hasCargoBed ? (bedFloorHeight + 0.18) : (trunkHeight + 0.06);
            double trunkBoxBottom = hasCargoBed ? 0.20 : 0.20;
            AABB trunkBox = new AABB(-halfWidth * 0.94, trunkBoxBottom, -rearDist, halfWidth * 0.94, trunkBoxTop, cabinRearZ);
            Optional<Vec3> trunkClip = trunkBox.clip(lStart, lEnd);
            if (trunkClip.isPresent()) {
                double d = lStart.distanceToSqr(trunkClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 7; closestLocalHit = trunkClip.get(); }
            }

            // Doors / Cabin
            AABB doorsBox = new AABB(-halfWidth, 0.20, cabinRearZ, halfWidth, hoodHeight - 0.05, cabinFrontZ);
            Optional<Vec3> doorsClip = doorsBox.clip(lStart, lEnd);
            if (doorsClip.isPresent()) {
                double d = lStart.distanceToSqr(doorsClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 8; closestLocalHit = doorsClip.get(); }
            }

            // Chassis
            AABB chassisBox = new AABB(-halfWidth * 0.75, 0.05, -rearDist + 0.3, halfWidth * 0.75, 0.35, frontDist - 0.3);
            Optional<Vec3> chassisClip = chassisBox.clip(lStart, lEnd);
            if (chassisClip.isPresent()) {
                double d = lStart.distanceToSqr(chassisClip.get());
                if (d < minDistanceSq) { minDistanceSq = d; hitZone = 9; closestLocalHit = chassisClip.get(); }
            }

            if (closestLocalHit != null) {
                worldHitPoint = new Vec3(carX + closestLocalHit.x * rightX + closestLocalHit.z * fwdX, carY + closestLocalHit.y, carZ + closestLocalHit.x * rightZ + closestLocalHit.z * fwdZ);
            }
        }

        // 3. Strict Boundary Miss Check: If projectile missed all vehicle boxes, deal NO damage!
        boolean isProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) || (directEntity != null && directEntity != attacker);
        if (hitZone < 0 && isProjectile) {
            return false;
        }

        // 4. Direct part fallback for melee / close interaction
        if (hitZone < 0) {
            if (hitPart != null) {
                worldHitPoint = hitPart.position();
                String pType = hitPart.partType != null ? hitPart.partType : "";
                int idxFL = getPhysicalWheelIndex(true, true);
                int idxFR = getPhysicalWheelIndex(true, false);
                int idxRL = getPhysicalWheelIndex(false, true);
                int idxRR = getPhysicalWheelIndex(false, false);

                if (hitPart == this.partWheelFL || pType.equals("wheel_fl")) hitZone = idxFL;
                else if (hitPart == this.partWheelFR || pType.equals("wheel_fr")) hitZone = idxFR;
                else if (hitPart == this.partWheelRL || pType.equals("wheel_rl")) hitZone = idxRL;
                else if (hitPart == this.partWheelRR || pType.equals("wheel_rr")) hitZone = idxRR;
                else if (hitPart == this.partSpareWheel || pType.equals("spare_wheel")) hitZone = 10;
                else if (hitPart == this.partRoof || pType.equals("roof")) hitZone = 4;
                else if (hitPart == this.partHood || pType.equals("hood")) hitZone = 6;
                else if (hitPart == this.partTrunk || pType.equals("trunk")) hitZone = 7;
                else if (hitPart == this.partCabin || pType.equals("cabin")) hitZone = 8;
                else if (hitPart == this.partChassis || pType.equals("chassis")) hitZone = 9;
            } else if (attacker != null) {
                worldHitPoint = attacker.getEyePosition().add(attacker.getViewVector(1.0F).scale(1.2));
                double fDx = worldHitPoint.x - carX, fDy = worldHitPoint.y - carY, fDz = worldHitPoint.z - carZ;
                double locLat = fDx * rightX + fDz * rightZ;
                double locY   = fDy;
                double locFwd = fDx * fwdX + fDz * fwdZ;

                // Strict boundary check: If melee swing is in empty air outside the vehicle hull, do NOT deal damage!
                if (Math.abs(locLat) > halfWidth + 0.30 || locFwd < -rearDist - 0.30 || locFwd > frontDist + 0.30 || locY < -0.20 || locY > roofHeight + 0.40) {
                    return false;
                }

                if (locFwd >= cabinRearZ && locFwd <= cabinFrontZ) {
                    if (locY >= roofHeight - 0.22) hitZone = 4;
                    else if (locY >= hoodHeight - 0.10) hitZone = 5;
                    else hitZone = 8;
                } else if (locFwd > cabinFrontZ) {
                    hitZone = 6;
                } else {
                    hitZone = 7;
                }
            } else {
                hitZone = 6;
                worldHitPoint = this.position().add(0, 0.6, 0);
            }
        }

        if (worldHitPoint == null) {
            worldHitPoint = this.position().add(0, 0.6, 0);
        }

        // Bare hand punch check
        boolean isBareHandPunch = (attacker instanceof Player p && p.getMainHandItem().isEmpty()
            && !source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
            && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION));
        if (isBareHandPunch) {
            this.level().playSound(null, worldHitPoint.x, worldHitPoint.y, worldHitPoint.z,
                SoundEvents.LANTERN_HIT, SoundSource.PLAYERS, 0.85F, 1.3F);
            amount = Math.max(1.0F, amount);
        }

        // =========================================================================
        // ROUTE DAMAGE TO SPECIFIC HIT ZONE
        // =========================================================================

        // 1. WHEEL HIT (Zones 0..3)
        if (hitZone >= 0 && hitZone <= 3) {
            int wIdx = hitZone;
            float modifiedDamage = amount * (float) cfg.wheelDamageMultiplier;
            if (!isWheelInstalled(wIdx)) {
                return this.applyVehicleDamage(source, modifiedDamage * 1.35F, getWheelWorldPos(wIdx));
            }

            wheelHealth[wIdx] -= amount;

            if (!this.physics.isTireFlat(wIdx)) {
                this.physics.shootTire(wIdx);
                this.physics.tireAir[wIdx] = 0.0F;

                byte t = (byte) (this.entityData.get(TIRE_STATUS) | (1 << wIdx));
                this.entityData.set(TIRE_STATUS, t);

                if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    Vec3 wp = getWheelWorldPos(wIdx);
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, wp.x, wp.y + 0.3, wp.z, 8, 0.1, 0.1, 0.1, 0.05);
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, wp.x, wp.y + 0.3, wp.z, 1, 0.0, 0.0, 0.0, 0.0);
                }

                float sVol = (def != null && def.sounds != null) ? def.sounds.tireExplodeVolume : 1.5F;
                float sPitch = (def != null && def.sounds != null) ? def.sounds.tireExplodePitch : 1.0F;
                Vec3 wp = getWheelWorldPos(wIdx);
                this.level().playSound(null, wp.x, wp.y, wp.z, ModSounds.TIRE_EXPLODE.get(), SoundSource.NEUTRAL, sVol, sPitch * (0.92F + this.random.nextFloat() * 0.16F));
                this.level().playSound(null, wp.x, wp.y, wp.z, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.8F, 0.65F);

                Vec3 wpName = getWheelWorldPos(wIdx);
                double nlX = (wpName.x - carX) * rightX + (wpName.z - carZ) * rightZ;
                double nlZ = (wpName.x - carX) * fwdX + (wpName.z - carZ) * fwdZ;
                String wheelName = (nlZ >= 0 ? "Переднее " : "Заднее ") + (nlX >= 0 ? "левое" : "правое");
                if (this.getFirstPassenger() instanceof Player driver) {
                    driver.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Шины] §e" + wheelName + " колесо пробито!"), true);
                }
            } else {
                Vec3 wp = getWheelWorldPos(wIdx);
                this.level().playSound(null, wp.x, wp.y, wp.z, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
                this.level().playSound(null, wp.x, wp.y, wp.z, SoundEvents.WOOL_HIT, SoundSource.NEUTRAL, 0.8F, 0.85F);
            }

            if (wheelHealth[wIdx] <= 0.0F || (amount >= 50.0F && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))) {
                breakOffWheel(wIdx);
            }

            return this.applyVehicleDamage(source, modifiedDamage, getWheelWorldPos(wIdx), false);
        }

        // 2. SPARE WHEEL (Zone 10)
        if (hitZone == 10) {
            this.level().playSound(null, worldHitPoint.x, worldHitPoint.y, worldHitPoint.z,
                ModSounds.TIRE_EXPLODE.get(), SoundSource.NEUTRAL, 1.2F, 1.0F);
            return this.applyVehicleDamage(source, amount * 0.5F, worldHitPoint, true);
        }

        // 3. ROOF HIT (Zone 4)
        if (hitZone == 4) {
            float modifiedDamage = amount * 0.35F;
            this.level().playSound(null, worldHitPoint.x, worldHitPoint.y, worldHitPoint.z,
                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.90F, 1.6F);
            return this.applyVehicleDamage(source, modifiedDamage, worldHitPoint, true);
        }

        // 4. WINDOW APERTURE (Zone 5)
        if (hitZone == 5) {
            isProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || (directEntity != null && directEntity != attacker);

            // Only high-speed ballistic projectiles (bullets) can pierce window glass to injure passengers!
            // Arrows, skeleton attacks, melee mobs (slimes, zombies), punches, or attacks redirected from vehicle hull cannot reach inside.
            boolean isArrowOrSkeleton = (directEntity instanceof net.minecraft.world.entity.projectile.AbstractArrow)
                || (attacker instanceof net.minecraft.world.entity.monster.AbstractSkeleton);
            if (isProjectile && !isArrowOrSkeleton && !REDIRECTING_DAMAGE.get()) {
                try {
                    ALLOW_PASSENGER_PENETRATION.set(true);
                    for (Entity p : this.getPassengers()) {
                        if (p instanceof LivingEntity living) {
                            living.hurt(source, amount * 1.25F);
                            living.invulnerableTime = 0;
                        }
                    }
                } finally {
                    ALLOW_PASSENGER_PENETRATION.set(false);
                }
            }
            this.level().playSound(null, worldHitPoint.x, worldHitPoint.y, worldHitPoint.z,
                SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.2F, 1.1F);
            float modifiedDamage = amount * (float) cfg.windowDamageMultiplier;
            return this.applyVehicleDamage(source, modifiedDamage, worldHitPoint, false);
        }

        // 5. ENGINE HOOD (Zone 6)
        if (hitZone == 6) {
            float modifiedDamage = amount * (float) cfg.engineDamageMultiplier;
            this.applyFrontalImpactDamage(modifiedDamage);
            return this.applyVehicleDamage(source, modifiedDamage * 0.25F, worldHitPoint, true);
        }

        // 6. TRUNK (Zone 7)
        if (hitZone == 7) {
            float modifiedDamage = amount * (float) cfg.trunkDamageMultiplier;
            return this.applyVehicleDamage(source, modifiedDamage, worldHitPoint, true);
        }

        // 7. CABIN / DOORS (Zone 8) & CHASSIS (Zone 9)
        float modifiedDamage = amount * (float) cfg.bodyDamageMultiplier;
        return this.applyVehicleDamage(source, modifiedDamage, worldHitPoint, true);
    }

    public boolean handleExplosionDamage(DamageSource source, float amount, Vec3 hitPoint) {
        if (this.tickCount == this.lastExplosionTick && (this.lastExplosionSource == source || (source != null && this.lastExplosionSource != null && source.getMsgId().equals(this.lastExplosionSource.getMsgId()) && source.getEntity() == this.lastExplosionSource.getEntity()))) {
            float extraDamage = Math.max(15.0F, amount * 0.8F);
            setVehicleHealth(getVehicleHealth() - extraDamage);
            return true;
        }
        this.lastExplosionTick = this.tickCount;
        this.lastExplosionSource = source;

        Vec3 blastCenter = source.getSourcePosition();
        if (blastCenter == null) {
            if (source.getDirectEntity() != null) {
                blastCenter = source.getDirectEntity().position();
            } else if (source.getEntity() != null) {
                blastCenter = source.getEntity().position();
            } else if (hitPoint != null) {
                blastCenter = hitPoint;
            } else {
                blastCenter = this.position().add(0, 0.5, 0);
            }
        }

        Vec3 toVehicle = this.position().add(0, 0.8, 0).subtract(blastCenter);
        double blastDist = toVehicle.length();
        Vec3 blastDir = (blastDist > 0.001) ? toVehicle.normalize() : new Vec3(0, 1, 0);

        float yawRad = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double localFwd = -(blastDir.x * fwdX + blastDir.z * fwdZ);
        double localRight = -(blastDir.x * rightX + blastDir.z * rightZ);

        float impulseStrength = (float) Math.min(2.5, Math.max(0.4, (amount / 20.0) / Math.max(1.0, blastDist)));

        // Violent chassis pitch and roll from explosive pressure wave
        float chassisRoll = (float) (localRight * 14.0F * impulseStrength);
        float chassisPitch = (float) (-localFwd * 10.0F * impulseStrength);
        float chassisLift = (float) Math.max(0.08F, 0.16F * impulseStrength);
        applyChassisImpulse(chassisLift, chassisPitch, chassisRoll);

        // Bodily push vehicle away from the detonation
        Vec3 blastPush = blastDir.scale(0.32 * impulseStrength).add(0, 0.18 * impulseStrength, 0);
        this.setDeltaMovement(this.getDeltaMovement().add(blastPush));

        // Screen shake & concussive trauma to driver/passengers
        this.entityData.set(IMPACT_TRAUMA, (float) Math.min(1.0, 0.35 + impulseStrength * 0.45));
        float passengerDamage = (float) Math.min(22.0F, Math.max(3.0F, amount * 0.45F / Math.max(1.0, blastDist)));
        try {
            ALLOW_PASSENGER_PENETRATION.set(true);
            for (Entity p : this.getPassengers()) {
                if (p instanceof LivingEntity living) {
                    living.hurt(this.damageSources().explosion(this, source.getEntity()), passengerDamage);
                }
            }
        } finally {
            ALLOW_PASSENGER_PENETRATION.set(false);
        }

        // Tires ruptured / blown out by blast overpressure within 4.5m
        for (int w = 0; w < 4; w++) {
            if (!isWheelInstalled(w)) continue;
            double side = (w == 0 || w == 2) ? 1.0 : -1.0;
            double end = (w == 0 || w == 1) ? 1.35 : -1.35;
            Vec3 wheelPos = this.position().add(rightX * side * 0.95 + fwdX * end, 0.35, rightZ * side * 0.95 + fwdZ * end);
            double distToWheel = wheelPos.distanceTo(blastCenter);
            if (distToWheel <= 4.5) {
                if (!isTirePunctured(w)) {
                    setTirePunctured(w, true);
                    this.level().playSound(null, wheelPos.x, wheelPos.y, wheelPos.z,
                        ModSounds.TIRE_EXPLODE.get(), SoundSource.BLOCKS, 3.5F, 0.90F + this.random.nextFloat() * 0.15F);
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, wheelPos.x, wheelPos.y + 0.2, wheelPos.z, 6, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                if ((distToWheel <= 2.2 || amount >= 45.0F) && this.random.nextFloat() < 0.65F) {
                    breakOffWheel(w);
                }
            }
        }

        // Engine disrupted / stalled by concussion
        if (blastDist <= 5.5) {
            this.setEngineOn(false);
        }

        // Radiator punctured if blast came from the front
        if (localFwd > 0.35 && this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            double hX = this.getX() + fwdX * 1.35;
            double hY = this.getY() + 0.75;
            double hZ = this.getZ() + fwdZ * 1.35;
            sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, hX, hY, hZ, 12, 0.3, 0.2, 0.3, 0.06);
            this.level().playSound(null, hX, hY, hZ, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2.5F, 0.70F);
        }

        // Broad concave inward dishing on exterior body facing the explosion
        if (!this.level().isClientSide) {
            double dx = blastCenter.x - this.getX();
            double dy = blastCenter.y - this.getY();
            double dz = blastCenter.z - this.getZ();
            VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
            float scale = (def != null && def.scale > 0.001F) ? def.scale : 1.0F;
            float groundOffsetY = (def != null) ? def.groundOffsetY : 0.0F;
            float rideHeight = (float) this.physics.rideHeight;

            float localMeshX = (float) (-(dx * rightX + dz * rightZ) / scale);
            float localMeshY = (float) ((dy - groundOffsetY - rideHeight) / scale);
            float localMeshZ = (float) (-(dx * fwdX + dz * fwdZ) / scale);

            localMeshX = Mth.clamp(localMeshX, -1.15F, 1.15F);
            localMeshY = Mth.clamp(localMeshY, 0.40F, 1.40F);
            localMeshZ = Mth.clamp(localMeshZ, -2.40F, 2.20F);

            float dirMeshX = -localMeshX;
            float dirMeshY = 0.0F;
            float dirMeshZ = -localMeshZ;
            float dLen = (float) Math.sqrt(dirMeshX * dirMeshX + dirMeshZ * dirMeshZ);
            if (dLen > 1e-4F) {
                dirMeshX /= dLen;
                dirMeshZ /= dLen;
            }

            com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                this,
                new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                    this.getId(),
                    localMeshX, localMeshY, localMeshZ,
                    dirMeshX, dirMeshY, dirMeshZ,
                    0.85F, 0.14F
                )
            );
        }

        // Vehicle damage reaction sound (custom heavy crash without vanilla anvil)
        this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
            ModSounds.CAR_CRASH_HEAVY.get(), SoundSource.BLOCKS, 3.0F, 0.85F);
        this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
            ModSounds.BULLET_METAL_HIT.get(), SoundSource.BLOCKS, 2.0F, 0.65F);

        float finalDamage = Math.max(90.0F, amount * 2.25F);
        return this.applyVehicleDamage(source, finalDamage, null, false);
    }

    public boolean applyVehicleDamage(DamageSource source, float finalDamage, Vec3 hitPoint) {
        return applyVehicleDamage(source, finalDamage, hitPoint, true);
    }

    public boolean applyVehicleDamage(DamageSource source, float finalDamage, Vec3 hitPoint, boolean playMetalSound) {
        float newHP = getVehicleHealth() - finalDamage;
        setVehicleHealth(newHP);

        Entity directEntity = source.getDirectEntity();
        Entity attacker = source.getEntity();
        boolean isExternalAttack = (directEntity != null && directEntity != this && !this.hasPassenger(directEntity))
            || (attacker != null && !this.hasPassenger(attacker));

        if (isExternalAttack && hitPoint != null) {
            if (!this.level().isClientSide) {
                com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                    this,
                    new com.evgen.cardecor.network.VehicleDamageSparksPacket(hitPoint.x, hitPoint.y, hitPoint.z)
                );
            } else {
                com.evgen.cardecor.client.VehicleVFX.spawnBulletImpactSparks(this.level(), hitPoint.x, hitPoint.y, hitPoint.z);
            }
        }

        if (hitPoint != null && playMetalSound) {
            float pitchMod = 1.30F + this.random.nextFloat() * 0.40F;
            this.level().playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.25F, pitchMod);
            this.level().playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.70F, 1.80F + this.random.nextFloat() * 0.15F);
            try {
                this.level().playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                    ModSounds.BULLET_METAL_HIT.get(), SoundSource.PLAYERS, 1.8F, 1.0F);
            } catch (Throwable ignored) {}
        }

        if (newHP <= 0.0F) {
            int currentStage = this.entityData.get(EXPLOSION_STAGE);
            if (currentStage == 0) {
                if (isExplosiveDamage(source)) {
                    // Massive direct high-yield blast triggers instant detonation
                    this.entityData.set(EXPLOSION_STAGE, 2);
                    this.explosionTicks = 81;
                } else {
                    // Initiate Stage 1: Engine Catastrophe & Smokeout
                    this.entityData.set(EXPLOSION_STAGE, 1);
                    this.setEngineOn(false);
                    this.explosionTicks = 0;

                    this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 2.5F, 0.65F);
                    this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2.2F, 0.70F);
                    this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                        SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 2.2F, 0.50F);

                    applyChassisImpulse(-0.05F, -3.0F, (this.random.nextFloat() - 0.5F) * 4.0F);
                }
            } else if (isExplosiveDamage(source) && currentStage < 3) {
                // Burning vehicle struck by an explosive immediately detonates
                this.explosionTicks = 81;
            }
        }

        return true;
    }



    public void setVehicleHealth(float hp) {

        this.entityData.set(VEHICLE_HP, Mth.clamp(hp, 0.0F, MAX_HP));

    }



    public void applyServerSync(double x, double y, double z, float yRot, float pitch, float roll, float speed, float steer, float rpm, int gear, byte inputs) {

        this.setPos(x, y, z);

        this.setYRot(yRot);

        this.currentPitch = pitch;

        this.currentRoll = roll;

        this.physics.speed = speed;

        this.physics.steerAngle = steer;

        this.physics.engineRPM = rpm;

        this.physics.currentGear = gear;

        this.inputFlags = inputs;



        this.entityData.set(SPEED, speed);

        this.entityData.set(STEER, steer);

        this.entityData.set(PITCH, pitch);

        this.entityData.set(ROLL, roll);

        this.entityData.set(RPM, rpm);

        this.entityData.set(GEAR, gear);

    }



    public float getTransmissionCondition() {
        return this.entityData.get(TRANSMISSION_CONDITION);
    }

    public void setTransmissionCondition(float condition) {
        this.entityData.set(TRANSMISSION_CONDITION, Math.max(0.0F, Math.min(100.0F, condition)));
    }

    public void setInput(int inputs) {
        if (inputs != 0) {
            wakeUp();
        }

        if (this.odeBody != null) {
            this.odeBody.powertrain.clutchPedalPressed = (inputs & 256) != 0;
        }

        // Попытка переключить передачу без сцепления (Alt) -> скрежет и износ
        if ((inputs & 2048) != 0) {
            handleClutchlessShiftAttempt();
        }

        // Прямой выбор передачи через Alt + цифры (биты 16..23)
        int directCode = (inputs >> 16) & 0xFF;
        if (directCode >= 1) {
            int targetGear = directCode - 2;
            selectDirectGear(targetGear);
        }

        boolean prevShiftUp = (this.inputFlags & 512) != 0;
        boolean newShiftUp = (inputs & 512) != 0;
        if (newShiftUp && !prevShiftUp) {
            triggerShiftUp();
        }

        boolean prevShiftDown = (this.inputFlags & 1024) != 0;
        boolean newShiftDown = (inputs & 1024) != 0;
        if (newShiftDown && !prevShiftDown) {
            triggerShiftDown();
        }

        this.inputFlags = inputs;
        if (!this.level().isClientSide()) {
            this.entityData.set(BRAKING, isBraking());
        }
    }

    public void setInput(byte inputs) {
        setInput((int) inputs);
    }

    public void selectDirectGear(int gear) {
        if (this.odeBody != null) {
            this.odeBody.powertrain.selectDirectGear(gear);
            if (this.odeBody.powertrain.justShifted) {
                this.odeBody.powertrain.justShifted = false;
                this.entityData.set(GEAR, this.odeBody.powertrain.currentGear);
                net.minecraft.server.level.ServerPlayer driverPlayer = (this.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer sp) ? sp : null;
                this.level().playSound(driverPlayer, this.getX(), this.getY(), this.getZ(),
                    ModSounds.GEAR_SHIFT.get(), SoundSource.PLAYERS, 0.75F, 0.95F + this.random.nextFloat() * 0.10F);
            }
        }
    }

    private long lastClutchlessShiftMs = 0L;
    private static final long CLUTCHLESS_COOLDOWN_MS = 400L;

    public void handleClutchlessShiftAttempt() {
        if (!this.level().isClientSide) {
            long now = System.currentTimeMillis();
            if (now - lastClutchlessShiftMs < CLUTCHLESS_COOLDOWN_MS) return;
            lastClutchlessShiftMs = now;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.CAR_IMPACT_LIGHT.get(), SoundSource.PLAYERS, 0.85F, 0.60F);
            setTransmissionCondition(getTransmissionCondition() - 3.5F);
            if (this.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§cСкрежет шестерен! Переключение невозможно без сцепления (Alt)!"), true);
            }
        }
    }

    public void triggerShiftUp() {
        if (this.odeBody != null) {
            this.odeBody.powertrain.shiftUp();
            if (this.odeBody.powertrain.justShifted) {
                this.odeBody.powertrain.justShifted = false;
                this.entityData.set(GEAR, this.odeBody.powertrain.currentGear);
                net.minecraft.server.level.ServerPlayer driverPlayer = (this.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer sp) ? sp : null;
                this.level().playSound(driverPlayer, this.getX(), this.getY(), this.getZ(),
                    ModSounds.GEAR_SHIFT.get(), SoundSource.PLAYERS, 0.75F, 0.95F + this.random.nextFloat() * 0.10F);
            }
        }
    }

    public void triggerShiftDown() {
        if (this.odeBody != null) {
            this.odeBody.powertrain.shiftDown();
            if (this.odeBody.powertrain.justShifted) {
                this.odeBody.powertrain.justShifted = false;
                this.entityData.set(GEAR, this.odeBody.powertrain.currentGear);
                net.minecraft.server.level.ServerPlayer driverPlayer = (this.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer sp) ? sp : null;
                this.level().playSound(driverPlayer, this.getX(), this.getY(), this.getZ(),
                    ModSounds.GEAR_SHIFT.get(), SoundSource.PLAYERS, 0.75F, 0.90F + this.random.nextFloat() * 0.10F);
            }
        }
    }



    public void requestSwitchSeat(Player player, int targetSeat) {

        requestSeatSwitch(player, targetSeat);

    }



    public void requestCycleSeat(Player player) {

        int current = getSeatForPlayer(player);

        int next = (current + 1) % 3;

        for (int i = 0; i < this.seatSwitchTimer.length; i++) {

            if (!isSeatOccupied(next)) {

                requestSeatSwitch(player, next);

                return;

            }

            next = (next + 1) % 3;

        }

    }



    public void requestSeatSwitch(Player player, int targetSeat) {
        int currentSeat = getSeatForPlayer(player);
        if (currentSeat == targetSeat || currentSeat == -1) return;

        if (isSeatBlockedByItems(targetSeat)) {
            player.displayClientMessage(Component.literal("§c[Салон] На этом сиденье лежат вещи!"), true);
            return;
        }
        if (isSeatOccupiedByPassenger(targetSeat)) {
            player.displayClientMessage(Component.literal("§c[Салон] Это место уже занято!"), true);
            return;
        }

        completeSeatSwitch(player, targetSeat);
    }

    private void completeSeatSwitch(Player player, int targetSeat) {
        int passIdx = this.getPassengers().indexOf(player);
        if (passIdx < 0) return;
        if (isSeatOccupied(targetSeat)) return;

        int mask = this.entityData.get(SEAT_SLOTS);
        mask &= ~(0xF << (passIdx * 4));
        mask |= (targetSeat & 0xF) << (passIdx * 4);
        this.entityData.set(SEAT_SLOTS, mask);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.9F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.PLAYERS, 0.80F, 0.95F + this.random.nextFloat() * 0.12F);
    }

    public void toggleEngine(Player player) {
        wakeUp();
        if (this.level().isClientSide) return;

        if (!isEngineOn()) {
            if (!hasEngine()) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("§c[!] Двигатель отсутствует! Смонтируйте мотор под капот."), true);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.85F, 1.4F);
                return;
            }

            boolean hasKey = (player != null && player.isCreative()) || isKeyInIgnition();
            if (!hasKey) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("§c[!] Вставьте ключ в замок зажигания!"), true);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.85F, 1.4F);
                return;
            }

            if (getBatteryCharge() < 5.0F && (player == null || !player.isCreative())) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("§c[!] Аккумулятор разряжен (" + String.format("%.1f", getBatteryCharge()) + "%)!"), true);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.9F, 0.6F);
                return;
            }
        }

        if (!isEngineOn() && getFuel() <= 0.1F) {
            return;
        }

        if (this.isInWater() && waterSubmergedTicks > 30) {
            this.entityData.set(ENGINE_ON, false);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.8F);
            return;
        }

        if (isEngineOn()) {
            this.entityData.set(ENGINE_ON, false);

            com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(this.getInstalledEngine());
            net.minecraft.sounds.SoundEvent stopSnd = ModSounds.ENGINE_STOP_I6.get();
            if (eng != null && eng.sounds != null && eng.sounds.stop != null && !eng.sounds.stop.isEmpty()) {
                try {
                    stopSnd = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(eng.sounds.stop));
                } catch (Exception ignored) {}
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                stopSnd, SoundSource.NEUTRAL, 1.25F, 1.0F);

            crankTimer = 0;
        } else {
            if (crankTimer > 0) return;

            // Starter drains ~0.3% battery per attempt
            if (player == null || !player.isCreative()) {
                setBatteryCharge(getBatteryCharge() - 0.3F);
            }

            com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
            int baseCrank = (def != null && def.engineStart != null) ? def.engineStart.crankTicks : 28;
            float qualityFactor = Math.max(0.5F, (140.0F - getEngineQuality()) / 80.0F);
            int crankTicks = Math.max(12, Math.round(baseCrank * qualityFactor));
            crankTimer = crankTicks;

            com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(this.getInstalledEngine());
            net.minecraft.sounds.SoundEvent startSnd = ModSounds.STARTER_I6.get();
            if (eng != null && eng.sounds != null && eng.sounds.starter != null && !eng.sounds.starter.isEmpty()) {
                try {
                    startSnd = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(eng.sounds.starter));
                } catch (Exception ignored) {}
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                startSnd, SoundSource.NEUTRAL, 1.20F, 1.0F);
        }
    }



    @Override
    public AABB getBoundingBoxForCulling() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));
        double maxLen = (cb != null ? Math.max(cb.frontDist, cb.rearDist) : (isPickup ? 2.90 : 3.25)) + 0.35;
        double roofHeight = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (isPickup ? 2.52 : 2.45);
        return new AABB(
            this.getX() - maxLen, this.getY() - 0.25, this.getZ() - maxLen,
            this.getX() + maxLen, this.getY() + roofHeight + 0.35, this.getZ() + maxLen
        );
    }

    @Override
    protected AABB makeBoundingBox() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));
        double halfW = (cb != null && cb.halfWidth > 0.3) ? cb.halfWidth : 1.15;
        double frontL = (cb != null && cb.frontDist > 0.3) ? cb.frontDist : (isPickup ? 2.30 : 2.20);
        double rearL = (cb != null && cb.rearDist > 0.3) ? cb.rearDist : (isPickup ? 2.50 : 2.20);
        double roofH = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (isPickup ? 2.20 : 1.75);

        float rad = (float) Math.toRadians(this.getYRot());
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));
        double boundX = halfW * cos + Math.max(frontL, rearL) * sin;
        double boundZ = halfW * sin + Math.max(frontL, rearL) * cos;
        return new AABB(
            this.getX() - boundX, this.getY() - 0.10, this.getZ() - boundZ,
            this.getX() + boundX, this.getY() + roofH + 0.20, this.getZ() + boundZ
        );
    }

    @Override
    public boolean isInvisible() {
        return super.isInvisible();
    }

    @Override
    public boolean isInvisibleTo(net.minecraft.world.entity.player.Player player) {
        return super.isInvisibleTo(player);
    }

    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public float getEngineRPM() {
        return this.entityData.get(RPM);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }



    public boolean hasDriver() {
        return isSeatOccupiedByPassenger(0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }



    @Override

    public boolean isAttackable() {

        return true;

    }











    @Override

    protected void removePassenger(Entity passenger) {

        if (passenger instanceof Player p) {

            this.lastDismountPlayerId = p.getId();
            this.clientLocalDriver = false;

            this.dismountImmunityTicks = 60; // 3.0 seconds of complete ramming immunity for exiting player

            // Car keeps its natural rolling inertia and coasts smoothly forward!

            // Open door when player exits, auto-close after 1.5 seconds
            if (!this.level().isClientSide) {
                int seatIdx = getSeatForPlayer(p);
                boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (this.getVehicleId() != null && this.getVehicleId().contains("pickup"));
                if (seatIdx == 0 || (seatIdx == 2 && !isPickup)) { // Driver or rear-left (not pickup bed!)
                    this.entityData.set(DOOR_LEFT_ANGLE, 65.0F);
                    this.doorLeftCloseTimer = 30;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                } else if (seatIdx == 1 || seatIdx == 3) { // Passenger or rear-right
                    this.entityData.set(DOOR_RIGHT_ANGLE, 65.0F);
                    this.doorRightCloseTimer = 30;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }

        com.evgen.cardecor.climate.VehicleClimateManager.onPassengerDismount(passenger);
        super.removePassenger(passenger);

    }



    @Override

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float carRot = this.getYRot();
        if (Float.isNaN(carRot) || Float.isInfinite(carRot)) carRot = 0.0F;
        float yawRad = (float) Math.toRadians(carRot);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        int seatIndex = 0;
        if (passenger instanceof Player p) {
            seatIndex = getSeatForPlayer(p);
            if (seatIndex < 0) seatIndex = 0;
        }

        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        com.evgen.cardecor.pack.VehicleDefinition.SeatDef sDef = (def != null && def.seats != null && seatIndex < def.seats.size()) ? def.seats.get(seatIndex) : null;
        float scale = (def != null && def.scale > 0.001F) ? def.scale : 1.0F;

        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));
        boolean hasCargoBed = isPickup || (cb != null && cb.hasCargoBed);
        double bedFloorHeight = (cb != null && cb.bedFloorHeight > 0.1) ? cb.bedFloorHeight : 0.78;
        double cabinRearZ = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : (isPickup ? -0.75 : -0.75);

        // Cargo bed seats dismount directly onto the cargo bed floor!
        boolean isCargoBedSeat = hasCargoBed && sDef != null && (
            (sDef.pos != null && sDef.pos.length >= 3 && sDef.pos[2] < cabinRearZ - 0.15) || 
            (sDef.name != null && (sDef.name.toLowerCase().contains("кузов") || sDef.name.toLowerCase().contains("bed") || sDef.name.toLowerCase().contains("trunk"))) ||
            (isPickup && seatIndex == 2)
        );

        if (isCargoBedSeat) {
            double sX = (sDef.pos != null && sDef.pos.length >= 3) ? sDef.pos[0] : 0.0;
            double sZ = (sDef.pos != null && sDef.pos.length >= 3) ? sDef.pos[2] : (cabinRearZ - 0.60);
            double lx = sX * scale;
            double lz = sZ * scale;
            double exitX = this.getX() + (-lx * cosY + lz * sinY);
            double exitZ = this.getZ() + (-lx * sinY - lz * cosY);
            double exitY = this.getY() + bedFloorHeight + 0.12;
            if (Double.isNaN(exitX) || Double.isInfinite(exitX)) exitX = this.getX();
            if (Double.isNaN(exitY) || Double.isInfinite(exitY)) exitY = this.getY() + 0.5;
            if (Double.isNaN(exitZ) || Double.isInfinite(exitZ)) exitZ = this.getZ();
            return new Vec3(exitX, exitY, exitZ);
        }

        double doorSide;
        double doorForward;

        if (sDef != null && sDef.dismountOffset != null && sDef.dismountOffset.length >= 3) {
            doorSide = sDef.dismountOffset[0];
            doorForward = sDef.dismountOffset[2];
        } else if (sDef != null) {
            doorSide = Math.signum(sDef.pos[0]) * 1.35;
            if (Math.abs(doorSide) < 0.1) doorSide = (seatIndex % 2 == 0) ? -1.35 : 1.35;
            doorForward = sDef.pos[2];
        } else {
            doorSide = (seatIndex == 0 ? -1.35 : 1.35);
            doorForward = 0.20;
        }

        double lx = doorSide * scale;
        double lz = doorForward * scale;

        double exitX = this.getX() + (-lx * cosY + lz * sinY);
        double exitZ = this.getZ() + (-lx * sinY - lz * cosY);
        double exitY = this.getY();

        // Sample solid ground height at exit position
        if (!Double.isNaN(exitX) && !Double.isNaN(exitZ) && !Double.isInfinite(exitX) && !Double.isInfinite(exitZ)) {
            BlockPos bp = BlockPos.containing(exitX, exitY + 0.5, exitZ);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos testPos = bp.above(dy);
                if (this.level().getBlockState(testPos).isSolid() && this.level().getBlockState(testPos.above()).isAir()) {
                    exitY = testPos.getY() + 1.0;
                    break;
                }
            }
        }

        if (Double.isNaN(exitX) || Double.isInfinite(exitX)) exitX = this.getX() + 1.5;
        if (Double.isNaN(exitY) || Double.isInfinite(exitY)) exitY = this.getY();
        if (Double.isNaN(exitZ) || Double.isInfinite(exitZ)) exitZ = this.getZ();

        return new Vec3(exitX, exitY, exitZ);

    }



    public boolean shouldRiderSit() {

        return true;

    }



    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.getMaxSeats();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // Dynamic vehicle physics handled smoothly in tick() without Minecraft stone-wall freezing!
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public int getMaxSeats() {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def != null && def.seats != null && !def.seats.isEmpty()) {
            return def.seats.size();
        }
        return 4;
    }



    public boolean isSeatOccupiedByPassenger(int seat) {
        int mask = this.entityData.get(SEAT_SLOTS);
        for (int i = 0; i < this.getPassengers().size(); i++) {
            if (((mask >> (i * 4)) & 0xF) == seat) return true;
        }
        return false;
    }

    public boolean isSeatOccupied(int seat) {
        if (isSeatOccupiedByPassenger(seat)) return true;
        if (!isSeatInstalled(seat) && !isCargoBedSeat(seat)) return true;
        return isSeatBlockedByItems(seat);
    }



    public int getSeatForPlayer(Player player) {

        int passIdx = this.getPassengers().indexOf(player);

        if (passIdx < 0) return -1;

        int mask = this.entityData.get(SEAT_SLOTS);

        return (mask >> (passIdx * 4)) & 0xF;

    }



    public int getFirstAvailableSeat() {

        for (int s = 0; s < getMaxSeats(); s++) {

            if (!isSeatOccupied(s)) return s;

        }

        return 0;

    }



    @Override

    public void tick() {
        ensurePhysicsBodyRegistered();
        super.tick();

        if (!this.level().isClientSide && !this.keySpawnInitialized) {
            this.keySpawnInitialized = true;
            if (this.ignitionKey.isEmpty() && !this.entityData.get(KEY_IN_IGNITION)) {
                this.ignitionKey = com.evgen.cardecor.item.VehiclePackKeyItem.createKeyForVehicle(this);
                this.entityData.set(KEY_IN_IGNITION, true);
            }
        }

        if (dismountImmunityTicks > 0) dismountImmunityTicks--;



        this.prevRoll = this.currentRoll;
        this.prevPitch = this.currentPitch;
        this.prevSteer = this.currentSteer;
        this.prevWheelRotation = this.currentWheelRotation;
        for (int i = 0; i < 4; i++) {
            this.prevWheelRotations[i] = this.currentWheelRotations[i];
        }

        this.currentRoll = this.entityData.get(ROLL);
        this.currentPitch = this.entityData.get(PITCH);
        float targetSteer = this.entityData.get(STEER);
        if (this.level().isClientSide) {
            this.prevRotationQuat.set(this.currentRotationQuat);
            this.currentRotationQuat.set(
                this.entityData.get(QUAT_X),
                this.entityData.get(QUAT_Y),
                this.entityData.get(QUAT_Z),
                this.entityData.get(QUAT_W)
            );
            this.currentSteer += (targetSteer - this.currentSteer) * 0.50F;

            int stage = this.entityData.get(EXPLOSION_STAGE);
            if (stage == 1) {
                // Stage 1: Billowing thick black smoke from hood
                float yawR = (float) Math.toRadians(this.getYRot());
                double fX = -Math.sin(yawR);
                double fZ = Math.cos(yawR);
                double hX = this.getX() + fX * 1.35 + (this.random.nextDouble() - 0.5) * 0.4;
                double hY = this.getY() + 0.75 + this.random.nextDouble() * 0.2;
                double hZ = this.getZ() + fZ * 1.35 + (this.random.nextDouble() - 0.5) * 0.4;
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, hX, hY, hZ, (Math.random() - 0.5) * 0.04, 0.08, (Math.random() - 0.5) * 0.04);
                if (this.random.nextFloat() < 0.35F) {
                    this.level().addParticle(ParticleTypes.LAVA, hX, hY, hZ, (Math.random() - 0.5) * 0.04, 0.05, (Math.random() - 0.5) * 0.04);
                }
            } else if (stage == 2) {
                // Stage 2: Whole car engulfed in flames and dense smoke
                double cX = this.getX() + (this.random.nextDouble() - 0.5) * 1.8;
                double cY = this.getY() + 0.3 + this.random.nextDouble() * 0.7;
                double cZ = this.getZ() + (this.random.nextDouble() - 0.5) * 1.8;
                this.level().addParticle(ParticleTypes.FLAME, cX, cY, cZ, (Math.random() - 0.5) * 0.02, 0.06, (Math.random() - 0.5) * 0.02);
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, cX, cY + 0.2, cZ, 0, 0.10, 0);
                if (this.random.nextFloat() < 0.25F) {
                    this.level().addParticle(ParticleTypes.LAVA, cX, cY, cZ, 0, 0.04, 0);
                }
            }
        } else {
            this.currentSteer = targetSteer;
        }

        float currentSpeedEntity = this.entityData.get(SPEED);
        double wRad = Math.max(0.25, this.physics.wheelRadius);
        float exactWheelDeg = (float) ((currentSpeedEntity * 0.05 / wRad) * (180.0 / Math.PI));
        this.currentWheelRotation += exactWheelDeg;

        if (!this.level().isClientSide) {
            // Server: exact ground physics per-wheel rotation
            if (this.physics != null && this.physics.wheelRotation != null) {
                for (int i = 0; i < 4; i++) {
                    this.currentWheelRotations[i] = (float) this.physics.wheelRotation[i];
                }
            }
        } else {
            // Client: independent 4-wheel rotation with slip, drivetrain, and parking brake lock
            boolean pBrake = isParkingBrakeOn();
            boolean hBrake = ((this.inputFlags & 16) != 0) || pBrake;

            if (pBrake) {
                // Parking brake firmly locks all wheels: zero rotation!
            } else {
                // Sample surface block under vehicle
                BlockPos sPos = BlockPos.containing(this.getX(), this.getY() - 0.20, this.getZ());
                BlockState sState = this.level().getBlockState(sPos);
                if (sState.isAir()) sState = this.level().getBlockState(sPos.below());
                com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType sType = 
                    com.evgen.cardecor.physics.RaycastVehiclePhysics.getSurfaceType(sState);

                float curRpm = this.entityData.get(RPM);
                boolean keyW = (this.inputFlags & 1) != 0;
                boolean keyS = (this.inputFlags & 2) != 0;
                boolean gasDown = keyW || keyS;
                String dt = (this.physics != null && this.physics.drivetrain != null) 
                    ? this.physics.drivetrain.toLowerCase(java.util.Locale.ROOT) : "rwd";

                float wheelspinSlip = 0.0F;
                if (isEngineOn() && gasDown) {
                    float slipFactor = 0.0F;
                    if (sType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.ICE) {
                        slipFactor = 0.90F;
                    } else if (sType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.SNOW) {
                        slipFactor = 0.65F;
                    } else if (sType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.MUD) {
                        slipFactor = 0.50F;
                    } else if (this.entityData.get(SCREECH) > 0.05F) {
                        slipFactor = Math.min(1.0F, this.entityData.get(SCREECH) * 0.80F);
                    }
                    if (slipFactor > 0.0F && curRpm > 1000.0F) {
                        float spinDegPerTick = (curRpm / 1100.0F) * 26.0F * slipFactor;
                        wheelspinSlip = keyS ? -spinDegPerTick : spinDegPerTick;
                    }
                }

                if (dt.equals("fwd")) {
                    // Front wheels driven: spin with slip
                    this.currentWheelRotations[0] += exactWheelDeg + wheelspinSlip;
                    this.currentWheelRotations[1] += exactWheelDeg + wheelspinSlip;
                    if (!hBrake) {
                        this.currentWheelRotations[2] += exactWheelDeg;
                        this.currentWheelRotations[3] += exactWheelDeg;
                    }
                } else if (dt.equals("awd") || dt.equals("4wd")) {
                    // All-wheel drive: all 4 wheels spin
                    this.currentWheelRotations[0] += exactWheelDeg + wheelspinSlip * 0.85F;
                    this.currentWheelRotations[1] += exactWheelDeg + wheelspinSlip * 0.85F;
                    if (!hBrake || wheelspinSlip != 0.0F) {
                        this.currentWheelRotations[2] += exactWheelDeg + wheelspinSlip * 0.85F;
                        this.currentWheelRotations[3] += exactWheelDeg + wheelspinSlip * 0.85F;
                    }
                } else {
                    // RWD (default): Front wheels roll free, rear wheels driven
                    this.currentWheelRotations[0] += exactWheelDeg;
                    this.currentWheelRotations[1] += exactWheelDeg;
                    if (!hBrake || wheelspinSlip != 0.0F) {
                        this.currentWheelRotations[2] += exactWheelDeg + wheelspinSlip;
                        this.currentWheelRotations[3] += exactWheelDeg + wheelspinSlip;
                    }
                }
            }
        }



        if (!this.level().isClientSide) {

            for (int i = 0; i < this.seatSwitchTimer.length; i++) {

                if (seatSwitchTimer[i] > 0) {

                    seatSwitchTimer[i]--;

                    if (seatSwitchTimer[i] == 0 && pendingTargetSeat[i] != -1) {

                        if (i < this.getPassengers().size() && this.getPassengers().get(i) instanceof Player p) {

                            completeSeatSwitch(p, pendingTargetSeat[i]);

                        }

                        pendingTargetSeat[i] = -1;

                    }

                }

            }



            float cDipY = this.entityData.get(CHASSIS_DIP_Y);
            float cDipP = this.entityData.get(CHASSIS_DIP_PITCH);
            float cDipR = this.entityData.get(CHASSIS_DIP_ROLL);

            if (Math.abs(cDipY) > 0.0002F || Math.abs(cDipP) > 0.002F || Math.abs(cDipR) > 0.002F) {
                cDipY *= 0.86F;
                cDipP *= 0.86F;
                cDipR *= 0.86F;
                if (Math.abs(cDipY) < 0.0002F) cDipY = 0.0F;
                if (Math.abs(cDipP) < 0.002F) cDipP = 0.0F;
                if (Math.abs(cDipR) < 0.002F) cDipR = 0.0F;
                this.entityData.set(CHASSIS_DIP_Y, cDipY);
                this.entityData.set(CHASSIS_DIP_PITCH, cDipP);
                this.entityData.set(CHASSIS_DIP_ROLL, cDipR);
            }

            float curHp = getVehicleHealth();
            int expStage = this.entityData.get(EXPLOSION_STAGE);

            if (curHp <= 0.0F && expStage == 0) {
                expStage = 1;
                this.entityData.set(EXPLOSION_STAGE, 1);
                this.setEngineOn(false);
                this.explosionTicks = 0;
            }

            if (this.isOnFire()) {
                this.clearFire();
            }
            if (expStage < 2) {
                for (Entity p : this.getPassengers()) {
                    if (p.isOnFire() || p.getRemainingFireTicks() > 0) {
                        p.clearFire();
                        p.setRemainingFireTicks(0);
                    }
                }
            }

            if (expStage > 0) {
                this.explosionTicks++;
                this.setEngineOn(false);

                float yawR = (float) Math.toRadians(this.getYRot());
                double fwdX = -Math.sin(yawR);
                double fwdZ = Math.cos(yawR);
                double rightX = Math.cos(yawR);
                double rightZ = Math.sin(yawR);

                double hoodX = this.getX() + fwdX * 1.35;
                double hoodY = this.getY() + 0.75;
                double hoodZ = this.getZ() + fwdZ * 1.35;

                double tankX = this.getX() - fwdX * 1.30;
                double tankY = this.getY() + 0.65;
                double tankZ = this.getZ() - fwdZ * 1.30;

                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    if (this.explosionTicks <= 40) {
                        // STAGE 1 (Ticks 1-40, ~2.0s): Fatal damage, engine dead, billowing thick black smoke from hood, radiator hissing
                        if (expStage != 1) this.entityData.set(EXPLOSION_STAGE, 1);

                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, hoodX, hoodY, hoodZ, 3, 0.25, 0.15, 0.25, 0.04);
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, hoodX, hoodY, hoodZ, 4, 0.30, 0.20, 0.30, 0.03);
                        if (this.random.nextFloat() < 0.45F) {
                            serverLevel.sendParticles(ParticleTypes.LAVA, hoodX, hoodY, hoodZ, 2, 0.2, 0.1, 0.2, 0.02);
                        }

                        // Sputtering, hissing steam & engine dying
                        if (this.explosionTicks % 10 == 0) {
                            this.level().playSound(null, hoodX, hoodY, hoodZ,
                                SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.65F + this.random.nextFloat() * 0.25F);
                        }
                        if (this.explosionTicks % 16 == 0) {
                            this.level().playSound(null, hoodX, hoodY, hoodZ,
                                SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 1.6F, 0.50F + this.random.nextFloat() * 0.2F);
                        }
                        if (this.explosionTicks % 8 == 0) {
                            applyChassisImpulse(-0.03F, (this.random.nextFloat() - 0.5F) * 2.5F, (this.random.nextFloat() - 0.5F) * 2.5F);
                        }
                    } else if (this.explosionTicks <= 80) {
                        // STAGE 2 (Ticks 41-80, ~2.0s): Spreading inferno, tires exploding under intense heat, cabin burning
                        if (expStage != 2) {
                            this.entityData.set(EXPLOSION_STAGE, 2);
                            this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                                SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 3.5F, 0.70F);
                        }

                        // Inferno engulfing vehicle
                        serverLevel.sendParticles(ParticleTypes.FLAME, hoodX, hoodY, hoodZ, 6, 0.35, 0.2, 0.35, 0.05);
                        serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.6, this.getZ(), 8, 0.6, 0.3, 0.6, 0.06);
                        serverLevel.sendParticles(ParticleTypes.FLAME, tankX, tankY, tankZ, 6, 0.35, 0.2, 0.35, 0.05);
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 5, 0.6, 0.4, 0.6, 0.08);

                        // Fire roar
                        if (this.explosionTicks % 8 == 0) {
                            this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 3.0F, 0.85F + this.random.nextFloat() * 0.2F);
                        }

                        // Burn players refusing to evacuate
                        try {
                            ALLOW_PASSENGER_PENETRATION.set(true);
                            for (Entity p : this.getPassengers()) {
                                p.hurt(this.damageSources().inFire(), 2.0F);
                                p.setRemainingFireTicks(60);
                            }
                        } finally {
                            ALLOW_PASSENGER_PENETRATION.set(false);
                        }

                        // Front tire blowout at Tick 52
                        if (this.explosionTicks == 52) {
                            int popWheel1 = this.isWheelInstalled(0) ? 0 : (this.isWheelInstalled(1) ? 1 : -1);
                            if (popWheel1 >= 0 && !isTirePunctured(popWheel1)) {
                                setTirePunctured(popWheel1, true);
                                this.level().playSound(null, this.getX(), this.getY() + 0.3, this.getZ(),
                                    ModSounds.TIRE_EXPLODE.get(), SoundSource.BLOCKS, 4.0F, 0.90F);
                                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                    this.getX() + (popWheel1 == 0 ? rightX : -rightX) * 0.9, this.getY() + 0.3, this.getZ() + (popWheel1 == 0 ? rightZ : -rightZ) * 0.9,
                                    2, 0.2, 0.2, 0.2, 0.05);
                                applyChassisImpulse(-0.06F, -3.0F, (popWheel1 == 0 ? 7.0F : -7.0F));
                            }
                        }

                        // Rear tire blowout at Tick 70
                        if (this.explosionTicks == 70) {
                            int popWheel2 = this.isWheelInstalled(3) ? 3 : (this.isWheelInstalled(2) ? 2 : -1);
                            if (popWheel2 >= 0 && !isTirePunctured(popWheel2)) {
                                setTirePunctured(popWheel2, true);
                                this.level().playSound(null, this.getX(), this.getY() + 0.3, this.getZ(),
                                    ModSounds.TIRE_EXPLODE.get(), SoundSource.BLOCKS, 4.0F, 0.82F);
                                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                    this.getX() + (popWheel2 == 2 ? rightX : -rightX) * 0.9, this.getY() + 0.3, this.getZ() + (popWheel2 == 2 ? rightZ : -rightZ) * 0.9,
                                    2, 0.2, 0.2, 0.2, 0.05);
                                applyChassisImpulse(-0.06F, 3.0F, (popWheel2 == 2 ? 7.0F : -7.0F));
                            }
                        }

                        if (this.explosionTicks % 6 == 0) {
                            applyChassisImpulse(-0.04F, (this.random.nextFloat() - 0.5F) * 3.5F, (this.random.nextFloat() - 0.5F) * 3.5F);
                        }
                    } else {
                        // STAGE 3 (Tick 81+): CATASTROPHIC FUEL TANK DETONATION!
                        this.entityData.set(EXPLOSION_STAGE, 3);
                        this.isExploding = true;

                        // Thunderous car explosion sound (using ONLY custom ModSounds.CAR_EXPLODE, no vanilla lightning or anvil)
                        this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                            ModSounds.CAR_EXPLODE.get(), SoundSource.BLOCKS, 10.0F, 0.92F + this.random.nextFloat() * 0.12F);

                        // Visual particle eruption
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.8, this.getZ(), 6, 0.8, 0.6, 0.8, 0.1);
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 50, 1.6, 1.2, 1.6, 0.15);
                        serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.8, this.getZ(), 80, 2.2, 1.5, 2.2, 0.28);
                        serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.8, this.getZ(), 35, 1.4, 0.9, 1.4, 0.22);

                        // Server-side explosion without ClientboundExplodePacket to eliminate vanilla explode audio packet
                        net.minecraft.world.level.Explosion explosion = new net.minecraft.world.level.Explosion(
                            serverLevel, this, null, null,
                            this.getX(), this.getY() + 0.8, this.getZ(),
                            4.2F, true, net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                        explosion.explode();
                        explosion.finalizeExplosion(false);

                        // Violent passenger blast ejection
                        try {
                            ALLOW_PASSENGER_PENETRATION.set(true);
                            for (Entity p : new java.util.ArrayList<>(this.getPassengers())) {
                                p.stopRiding();
                                Vec3 blastVec = p.position().subtract(this.position());
                                if (blastVec.lengthSqr() < 0.001) blastVec = new Vec3(0, 1, 0);
                                Vec3 blastVel = blastVec.normalize().scale(0.85).add(0, 0.6, 0);
                                p.setDeltaMovement(p.getDeltaMovement().add(blastVel));
                                p.hurt(this.damageSources().explosion(this, null), 24.0F);
                            }
                        } finally {
                            ALLOW_PASSENGER_PENETRATION.set(false);
                        }

                        // Scrap drops
                        if (this.random.nextFloat() < 0.6F) {
                            this.spawnAtLocation(com.evgen.cardecor.init.ModItems.VEHICLE_WHEEL.get());
                        }
                        this.spawnAtLocation(Items.IRON_INGOT, 3 + this.random.nextInt(4));

                        this.dropTrunkContents();
                        this.discard();
                        return;
                    }
                }
            } else if (curHp <= 60.0F && this.tickCount % 4 == 0) {
                this.level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, this.getX(), this.getY() + 0.9, this.getZ(), 0, 0.08, 0);
            }



            // Starter crank timer with authentic vintage carburetor catch chance
            if (crankTimer > 0) {
                crankTimer--;
                if (crankTimer == 0) {
                    if (this.entityData.get(EXPLOSION_STAGE) > 0 || getVehicleHealth() <= 0.0F) {
                        this.entityData.set(ENGINE_ON, false);
                    } else if (this.isInWater() && waterSubmergedTicks > 30) {
                        this.entityData.set(ENGINE_ON, false);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.8F);
                    } else if (getFuel() <= 0.1F) {
                        this.entityData.set(ENGINE_ON, false);
                    } else {
                        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
                        double baseStartChance = (def != null && def.engineStart != null) ? def.engineStart.startChance : 0.65;
                        boolean failAllowed = (def != null && def.engineStart != null) ? def.engineStart.failAllowed : true;

                        // Project Zomboid style: quality and condition influence starter catch rate
                        double qualityMult = 0.4 + (getEngineQuality() / 100.0) * 0.6;
                        double conditionMult = 0.3 + (getEngineCondition() / 100.0) * 0.7;
                        double finalStartChance = baseStartChance * qualityMult * conditionMult;

                        if (!failAllowed || (baseStartChance >= 0.999) || (this.random.nextDouble() <= finalStartChance)) {
                            this.entityData.set(ENGINE_ON, true);
                            com.evgen.cardecor.pack.EngineDefinition eng = com.evgen.cardecor.pack.EngineRegistry.get(this.getInstalledEngine());
                            if (eng != null && eng.sounds != null && eng.sounds.start != null && !eng.sounds.start.isEmpty()) {
                                try {
                                    net.minecraft.sounds.SoundEvent startRoar = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(eng.sounds.start));
                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), startRoar, SoundSource.NEUTRAL, 1.25F, 1.0F);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }

            // Alternator charges battery when engine is running (~0.05% per second = +0.0025% per tick)
            if (this.isEngineOn()) {
                if (getBatteryCharge() < 100.0F) {
                    setBatteryCharge(getBatteryCharge() + 0.0025F);
                }
                // Low condition engine stall chance
                if (getEngineCondition() < 30.0F && this.tickCount % 20 == 0) {
                    float stallRisk = (30.0F - getEngineCondition()) / 100.0F * (1.0F - getEngineQuality() / 150.0F);
                    if (this.random.nextFloat() < stallRisk) {
                        this.entityData.set(ENGINE_ON, false);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.7F);
                    }
                }
            } else {
                // Headlights drain battery when engine is off (~0.02% per second = 0.001% per tick)
                if (isHeadlightsOn()) {
                    if (getBatteryCharge() > 0.0F) {
                        setBatteryCharge(getBatteryCharge() - 0.001F);
                    } else {
                        // Dead battery turns off lights
                        setHeadlightMode(0);
                    }
                }
            }

            // Radiator damage: steam leak and engine thermal degradation
            if (getRadiatorHealth() < 50.0F && this.tickCount % 4 == 0) {
                float yawR = (float) Math.toRadians(this.getYRot());
                double fX = -Math.sin(yawR);
                double fZ = Math.cos(yawR);
                double hoodX = this.getX() + fX * 1.6;
                double hoodZ = this.getZ() + fZ * 1.6;
                this.level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    hoodX, this.getY() + 0.9, hoodZ, 0.0, 0.08, 0.0);

                if (getRadiatorHealth() <= 0.0F && this.isEngineOn() && this.tickCount % 40 == 0) {
                    setEngineCondition(Math.max(0.0F, getEngineCondition() - 0.4F));
                    this.level().playSound(null, hoodX, this.getY() + 0.8, hoodZ,
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 0.8F, 1.3F);
                }
            }

            // Water submersion
            if (this.isInWater() || this.isUnderWater() || physics.waterDepth > 0.4) {
                waterSubmergedTicks++;
                if (this.isEngineOn() && waterSubmergedTicks >= 70) {
                    this.entityData.set(ENGINE_ON, false);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.2F, 0.75F);
                }
            } else {
                waterSubmergedTicks = Math.max(0, waterSubmergedTicks - 2);
            }

            double throttle = 0.0;
            double steer = 0.0;
            boolean hasDriver = false;

            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof Player p) {
                    int seat = getSeatForPlayer(p);
                    if (seat == 0) { // Driver
                        hasDriver = true;
                        if ((inputFlags & 4) != 0) steer -= 1.0;
                        if ((inputFlags & 8) != 0) steer += 1.0;

                        if (isEngineOn()) {
                            boolean keyW = (inputFlags & 1) != 0;
                            boolean keyS = (inputFlags & 2) != 0;
                            boolean keySpace = (inputFlags & 16) != 0;

                            if (keyW && isParkingBrakeOn()) {
                                setParkingBrake(false);
                            }

                            if (keyW && (keyS || keySpace) && Math.abs(physics.speed) < 1.8) {
                                throttle = 1.0;
                            } else {
                                if (keyW) throttle += 1.0;
                                if (keyS) throttle -= 1.0;
                            }
                        }
                    }
                }
            }

            boolean handbrake = hasDriver && (inputFlags & 16) != 0;
            boolean boostInput = hasDriver && (inputFlags & 64) != 0;
            boolean hornInput = hasDriver && ((inputFlags & -128) != 0 || (inputFlags & 128) != 0);

            // Horn acoustic pulse: alerts mobs up to 85 blocks
            if (hornInput && !this.level().isClientSide && this.tickCount % 10 == 0) {
                pulseNoise(60.0F);
            }

            if (isEngineOn()) {
                float fuelUse = 0.0075F + (float)(Math.abs(throttle) * 0.018F) + (boostInput ? 0.038F : 0.0F);
                float curFuel = getFuel() - fuelUse;
                if (curFuel <= 0.0F) {
                    curFuel = 0.0F;
                    this.entityData.set(ENGINE_ON, false);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 0.8F);
                }
                setFuel(curFuel);

                // Project Zomboid Acoustics & Noise: pulse noise to hostile mobs every 20 ticks (1s)
                if (!this.level().isClientSide && this.tickCount % 20 == 0) {
                    float throttleExtra = (float)(Math.abs(throttle) * 12.0);
                    pulseNoise(throttleExtra);
                }
            }

            // Project Zomboid Thermodynamic Simulation, Oil Seizure & Fluids
            if (!this.level().isClientSide) {
                float curTemp = getEngineTemperature();
                float ambient = 20.0F;

                if (isEngineOn()) {
                    // Oil wear rate: 5x faster if oil < 15%
                    float curOil = getEngineOil();
                    float oilMult = (curOil < 15.0F) ? 5.0F : 1.0F;
                    if (curOil > 0.0F) {
                        setEngineOil(Math.max(0.0F, curOil - 0.0003F * oilMult));
                    } else {
                        // 0% Oil: Instant Engine Seizure!
                        this.entityData.set(ENGINE_ON, false);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 1.6F, 0.5F);
                        setEngineCondition(Math.min(getEngineCondition(), 5.0F));
                        Entity driverEnt = this.getControllingPassenger();
                        if (driverEnt instanceof Player dp) {
                            dp.displayClientMessage(Component.literal("§c[!] ДВИГАТЕЛЬ ЗАКЛИНИЛО: Отсутствует моторное масло (0%)!"), true);
                        }
                    }

                    // Coolant leak if radiator damaged
                    float curCoolant = getCoolantLevel();
                    float radHp = getRadiatorHealth();
                    if (radHp < 50.0F && curCoolant > 0.0F) {
                        float leakRate = (50.0F - radHp) * 0.0008F;
                        setCoolantLevel(Math.max(0.0F, curCoolant - leakRate));
                    }

                    // Temperature calculation
                    float coolantRatio = getCoolantLevel() / 100.0F;
                    float radiatorRatio = getRadiatorHealth() / 100.0F;
                    float coolingEff = coolantRatio * radiatorRatio;
                    float speedFactor = (float) Math.min(1.0, Math.abs(physics.speed) / 25.0);

                    // Heat produced: idle base (0.035) + throttle load (up to 0.065), friction extra if oil < 15%
                    float heat = (0.035F + (float) Math.abs(throttle) * 0.065F) * (curOil < 15.0F ? 1.6F : 1.0F);
                    // Cooling: radiator + airflow
                    float cooling = coolingEff * 0.05F + speedFactor * 0.025F;

                    // Equilibrium: if cooling >= heat, settles around 88-92°C. If cooling fails, target climbs up to 130°C!
                    float targetTemp = 90.0F + (heat - cooling) * 350.0F;
                    targetTemp = Math.max(70.0F, Math.min(130.0F, targetTemp));

                    curTemp += (targetTemp - curTemp) * 0.012F;

                    // Critical Overheating (> 105 °C)
                    if (curTemp >= 105.0F) {
                        if (this.tickCount % 4 == 0) {
                            float yawR = (float) Math.toRadians(this.getYRot());
                            double fX = -Math.sin(yawR), fZ = Math.cos(yawR);
                            this.level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                                this.getX() + fX * 1.6, this.getY() + 0.95, this.getZ() + fZ * 1.6, 0.0, 0.08, 0.0);
                        }
                        if (this.tickCount % 20 == 0) {
                            float curEng = getEngineCondition();
                            setEngineCondition(Math.max(0.0F, curEng - 0.45F));

                            // Severe Overheating stall chance (> 115 °C)
                            if (curTemp >= 115.0F && this.random.nextFloat() < 0.08F) {
                                this.entityData.set(ENGINE_ON, false);
                                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.2F, 0.7F);
                                Entity driverEnt = this.getControllingPassenger();
                                if (driverEnt instanceof Player dp) {
                                    dp.displayClientMessage(Component.literal("§c[!] Двигатель заглох от критического перегрева (" + String.format("%.0f", curTemp) + "°C)!"), true);
                                }
                            }
                        }
                    }

                    // Low oil wear (< 15%)
                    if (curOil <= 15.0F && this.tickCount % 20 == 0) {
                        float curEng = getEngineCondition();
                        setEngineCondition(Math.max(0.0F, curEng - 0.35F));
                    }
                } else {
                    // Engine OFF: cool towards ambient
                    curTemp += (ambient - curTemp) * 0.003F;
                }
                setEngineTemperature(curTemp);
            }

            if (this.tickCount % 5 == 0) {
                byte climate = this.getClimateMode();
                if (climate != 0) {
                    if (this.isEngineOn()) {
                        com.evgen.cardecor.climate.VehicleClimateManager.tickVehicleClimate(this, climate);
                    } else {
                        this.setClimateMode((byte) 0);
                        com.evgen.cardecor.climate.VehicleClimateManager.clearVehicleClimate(this);
                    }
                }
            }

            this.physics.isBeingPushed = false;

            this.physics.hasDriver = this.hasDriver();
            this.physics.installedWheelsMask = this.entityData.get(INSTALLED_WHEELS);

            // Server Deep-Sleep Optimization: Parked & unoperated vehicles hibernate to save massive TPS
            // Vehicles within 10 blocks of a player wake up and stay active in terms of physics
            boolean nearPlayer = this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 10.0);
            boolean canSleep = !nearPlayer
                && !hasDriver 
                && this.getPassengers().isEmpty() 
                && !this.isEngineOn() 
                && !this.physics.isBeingPushed 
                && Math.abs(this.physics.speed) < 0.005 
                && Math.abs(this.physics.lateralSlipVel) < 0.005 
                && this.physics.isPhysicallySupported 
                && this.physicsBody.groundedWheelsCount >= 3
                && this.physicsBody.isSleeping 
                && this.physics.reboundTimer <= 0.0 
                && this.physics.crashCooldown <= 0.0 
                && doorLeftCloseTimer <= 0 
                && doorRightCloseTimer <= 0 
                && trunkCloseTimer <= 0;

            if (canSleep) {
                this.dormantTicks++;
                if (this.dormantTicks > 10) {
                    this.isDormant = true;
                }
            } else {
                this.wakeUp();
            }

            if (this.isDormant) {
                if (nearPlayer) {
                    this.wakeUp();
                } else {
                    // Ground support sanity check every 20 ticks (1 sec)
                    if (this.tickCount % 20 == 0) {
                        net.minecraft.core.BlockPos belowPos = this.blockPosition().below();
                        if (this.level().getBlockState(belowPos).isAir()) {
                            this.wakeUp();
                        }
                    }
                    this.setDeltaMovement(Vec3.ZERO);
                    this.setOnGround(true);
                    return;
                }
            }

            float currentYaw = this.getYRot();
            if (Float.isNaN(currentYaw) || Float.isInfinite(currentYaw)) {
                currentYaw = (Float.isNaN(this.yRotO) || Float.isInfinite(this.yRotO)) ? 0.0F : this.yRotO;
                this.setYRot(currentYaw);
            }

            Vec3 deltaMotion = Vec3.ZERO;
            if (this.odeBody == null || this.odeBody.body == null) {
                this.physics.tireCondition = this.tireCondition;
                this.physics.isParkingBrake = isParkingBrakeOn();
                deltaMotion = physics.step(this.level(), this.position(), currentYaw, throttle, steer, handbrake, boostInput, this.onGround(), this.isEngineOn(), 0.05);
            }

            if (!this.level().isClientSide && Math.abs(this.physics.speed) > 0.4) {
                float wearPerTick = 0.0005F;
                if (this.physics.isDrifting) wearPerTick += 0.0035F;
                if (handbrake) wearPerTick += 0.0025F;

                for (int i = 0; i < 4; i++) {
                    if (isWheelInstalled(i) && tireCondition[i] > 0.0F) {
                        tireCondition[i] = Math.max(0.0F, tireCondition[i] - wearPerTick);
                    }
                }
                if (this.tickCount % 20 == 0) {
                    syncTireConditionsToEntityData();
                }
            }

            // ANTI-NAN SAFETY FALLBACK: Prevent math explosions that trap players
            if (Double.isNaN(deltaMotion.x) || Double.isNaN(deltaMotion.y) || Double.isNaN(deltaMotion.z) ||
                Double.isInfinite(deltaMotion.x) || Double.isInfinite(deltaMotion.y) || Double.isInfinite(deltaMotion.z)) {
                deltaMotion = Vec3.ZERO;
                physics.speed = 0.0;
                physics.lateralSlipVel = 0.0;
                physics.verticalSpeed = 0.0;
                physics.liveYawRate = 0.0;
                physics.deltaYaw = 0.0;
                physics.acceleration = 0.0;
                physics.chassisDipY = 0.0;
                physics.chassisDipPitch = 0.0;
                physics.chassisDipRoll = 0.0;
                physics.chassisPitchVel = 0.0;
                physics.chassisRollVel = 0.0;
                physics.chassisYVel = 0.0;
                physics.steerAngle = 0.0;
                physics.roll = 0.0;
                physics.pitch = 0.0;
                physics.groundRoll = 0.0;
                physics.groundPitch = 0.0;
                physics.targetSurfaceY = this.getY();
            }

            if (Double.isNaN(physics.deltaYaw) || Double.isInfinite(physics.deltaYaw)) {
                physics.deltaYaw = 0.0;
                physics.liveYawRate = 0.0;
            }

            float newYaw = Mth.wrapDegrees((float) (this.getYRot() + physics.deltaYaw));
            if (Float.isNaN(newYaw) || Float.isInfinite(newYaw)) {
                newYaw = (Float.isNaN(this.yRotO) || Float.isInfinite(this.yRotO)) ? 0.0F : this.yRotO;
            }
            this.setYRot(newYaw);

            if (this.odeBody != null && this.odeBody.body != null) {
                if (this.physics != null && this.physics.drivetrain != null) {
                    this.odeBody.drivetrain = this.physics.drivetrain.toLowerCase(java.util.Locale.ROOT);
                }
                this.odeBody.throttleInput = (float) throttle;
                this.odeBody.steerInput = (float) steer;
                this.odeBody.handbrakeInput = handbrake;
                this.odeBody.powertrain.isEngineOn = this.isEngineOn();
                this.odeBody.powertrain.clutchPedalPressed = hasDriver && (this.inputFlags & 256) != 0;
                this.odeBody.powertrain.transmissionCondition = this.getTransmissionCondition();
                for (int i = 0; i < 4; i++) {
                    this.odeBody.tireCondition[i] = isWheelInstalled(i) ? this.tireCondition[i] : 0.0f;
                }

                // Dynamic passenger and trunk cargo mass
                float cargoKg = 0.0f;
                if (this.trunkInventory != null) {
                    for (int s = 0; s < this.trunkInventory.getContainerSize(); s++) {
                        net.minecraft.world.item.ItemStack st = this.trunkInventory.getItem(s);
                        if (!st.isEmpty()) cargoKg += st.getCount() * 1.5f;
                    }
                }
                this.odeBody.updateDynamicMass(this.getPassengers().size(), cargoKg);

                // Step ODE 6-DOF physics world
                com.evgen.cardecor.physics.ode.OdePhysicsWorld.get(this.level()).tickLevelOnce(this.level());

                // Read authoritative 6-DOF state from ODE body
                double nextX = this.odeBody.currentPosition.x;
                double nextY = this.odeBody.currentPosition.y - this.odeBody.comHeight + 0.04;
                double nextZ = this.odeBody.currentPosition.z;

                this.setPos(nextX, nextY, nextZ);

                org.ode4j.math.DVector3C vel = this.odeBody.body.getLinearVel();
                Vec3 odeDeltaMotion = new Vec3(vel.get0() * 0.05, vel.get1() * 0.05, vel.get2() * 0.05);
                this.setDeltaMovement(odeDeltaMotion);
                this.setOnGround(this.odeBody.groundedWheelsCount > 0);

                float curSpeedKmh = (float) this.odeBody.getSpeedKmh();
                this.entityData.set(SPEED, curSpeedKmh);
                this.physics.speed = curSpeedKmh / 3.6;

                float centerSteer = this.odeBody.steeringRack.getCenterWheelAngle();
                this.entityData.set(STEER, centerSteer);
                this.physics.steerAngle = Math.toDegrees(centerSteer);

                this.entityData.set(RPM, this.odeBody.powertrain.currentRpm);
                this.entityData.set(GEAR, this.odeBody.powertrain.currentGear);
                this.setTransmissionCondition(this.odeBody.powertrain.transmissionCondition);

                if (this.odeBody.powertrain.isClutchBurning && !this.level().isClientSide && this.tickCount % 2 == 0) {
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
                            this.getY() + 0.35,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                            2, 0.1, 0.05, 0.1, 0.02);
                    }
                }

                if (this.odeBody.powertrain.isEngineStalled && this.isEngineOn()) {
                    this.entityData.set(ENGINE_ON, false);
                    this.odeBody.powertrain.isEngineStalled = false;
                }

                newYaw = this.odeBody.getYaw();
                float newPitch = this.odeBody.getPitch();
                float newRoll = this.odeBody.getRoll();

                this.setYRot(newYaw);
                this.setXRot(newPitch);
                this.currentPitch = (float) Math.toRadians(newPitch);
                this.currentRoll = (float) Math.toRadians(newRoll);

                this.entityData.set(SERVER_YAW, newYaw);
                this.entityData.set(PITCH, this.currentPitch);
                this.entityData.set(ROLL, this.currentRoll);

                for (int i = 0; i < 4; i++) {
                    this.physics.suspensionCompressions[i] = this.odeBody.suspensionRestLength - this.odeBody.wheelSuspensionLength[i];
                }

                this.entityData.set(QUAT_X, this.odeBody.currentRotation.x);
                this.entityData.set(QUAT_Y, this.odeBody.currentRotation.y);
                this.entityData.set(QUAT_Z, this.odeBody.currentRotation.z);
                this.entityData.set(QUAT_W, this.odeBody.currentRotation.w);
            } else {
                double nextX = this.getX() + deltaMotion.x;
                double nextY = this.getY() + deltaMotion.y;
                double nextZ = this.getZ() + deltaMotion.z;

                this.setPos(nextX, nextY, nextZ);
                this.setDeltaMovement(deltaMotion);
                this.setOnGround(this.physics.isPhysicallySupported);

                this.currentPitch = (float) this.physics.pitch;
                this.currentRoll = (float) this.physics.roll;
                this.setXRot((float) Math.toDegrees(this.physics.pitch));
            }

            // Wake up nearby dormant vehicles within 10 blocks of this moving vehicle
            if (!this.level().isClientSide && (Math.abs(physics.speed) > 0.05 || this.physics.isBeingPushed)) {
                List<DrivableVehicleEntity> nearbyCars = this.level().getEntitiesOfClass(
                    DrivableVehicleEntity.class, 
                    this.getBoundingBox().inflate(10.0), 
                    v -> v != this && v.isDormant
                );
                for (DrivableVehicleEntity car : nearbyCars) {
                    car.wakeUp();
                }
            }

            if (!this.level().isClientSide) {
                // Environmental washing: Rain and Water
                if (this.isInWater()) {
                    if (getDirtLevel() > 0.0F) setDirtLevel(getDirtLevel() - 0.20F);
                    if (getBloodLevel() > 0.0F) setBloodLevel(getBloodLevel() - 0.20F);
                } else if (this.level().isRainingAt(this.blockPosition().above())) {
                    if (getDirtLevel() > 0.0F) setDirtLevel(getDirtLevel() - 0.02F);
                    if (getBloodLevel() > 0.0F) setBloodLevel(getBloodLevel() - 0.02F);
                }

                // Dirt accumulation from ground surfaces when rolling
                if (Math.abs(this.physics.speed) > 0.05 && this.physics.isGrounded) {
                    BlockPos underPos = BlockPos.containing(this.getX(), this.getY() - 0.20, this.getZ());
                    BlockState underState = this.level().getBlockState(underPos);
                    if (underState.isAir()) underState = this.level().getBlockState(underPos.below());

                    if (!underState.isAir()) {
                        if (underState.is(net.minecraft.world.level.block.Blocks.MUD)
                                || underState.is(net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS)
                                || underState.is(net.minecraft.world.level.block.Blocks.SOUL_SAND)
                                || underState.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL)) {
                            setDirtLevel(getDirtLevel() + 0.04F);
                        } else if (underState.is(net.minecraft.tags.BlockTags.DIRT)
                                || underState.is(net.minecraft.tags.BlockTags.SAND)
                                || underState.is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
                            setDirtLevel(getDirtLevel() + 0.006F);
                        } else {
                            setDirtLevel(getDirtLevel() + 0.001F);
                        }
                    }
                }
            }

            // Sparks and scraping sound when driving on bare hub without wheel
            if (Math.abs(physics.speed) > 0.4 && physics.isGrounded) {
                byte wheelMask = this.entityData.get(INSTALLED_WHEELS);
                if (wheelMask != 15) {
                    for (int i = 0; i < 4; i++) {
                        if ((wheelMask & (1 << i)) == 0) {
                            Vec3 wPos = getWheelWorldPos(i);
                            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, wPos.x, wPos.y, wPos.z, 3, 0.1, 0.05, 0.1, 0.12);
                                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, wPos.x, wPos.y, wPos.z, 1, 0.05, 0.05, 0.05, 0.02);
                            }
                        }
                    }
                    if (this.tickCount % 16 == 0) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.WHEEL_HUB_GRIND.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.50F, 0.95F + (float)(Math.min(0.3, Math.abs(physics.speed) * 0.04)));
                    }
                }
            }

            if (this.odeBody == null && physics.justShifted) {
                physics.justShifted = false;
                this.entityData.set(GEAR, physics.currentGear);
                ServerPlayer driverPlayer = (this.getFirstPassenger() instanceof ServerPlayer sp) ? sp : null;
                this.level().playSound(driverPlayer, this.getX(), this.getY(), this.getZ(),
                    ModSounds.GEAR_SHIFT.get(), SoundSource.PLAYERS, 0.75F, 0.95F + this.random.nextFloat() * 0.10F);

                if (physics.shiftPopTriggered) {
                    physics.shiftPopTriggered = false;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.ENGINE_KICKDOWN.get(), SoundSource.PLAYERS, 0.85F, 1.05F + this.random.nextFloat() * 0.15F);
                    Vec3 exhPos = getExhaustWorldPos();
                    float yRad = (float) Math.toRadians(this.getYRot());
                    double fX = -Math.sin(yRad);
                    double fZ = Math.cos(yRad);
                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                        this,
                        new com.evgen.cardecor.network.VehicleExhaustFlashPacket(exhPos.x, exhPos.y, exhPos.z, fX, fZ, true)
                    );
                }
            }

            if (physics.isWallHit) {
                physics.isWallHit = false;
                double spdKmh = physics.lastImpactSpeedKmh;
                if (spdKmh >= 14.0) {
                    // High-speed collision: loud, heavy crunch with strong speed-based volume scaling!
                    float vol = (float) Mth.clamp(3.0 + ((spdKmh - 14.0) / 20.0) * 2.5, 3.0, 5.5) * 1.6F;
                    float pitch = (float) Mth.clamp(0.90 - ((spdKmh - 14.0) / 40.0) * 0.28, 0.62, 0.92);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_CRASH_HEAVY.get(), SoundSource.PLAYERS, vol, pitch + this.random.nextFloat() * 0.06F);
                } else if (spdKmh >= 5.0) {
                    // Moderate bump: audible and punchy, clearly tied to impact force!
                    float vol = (float) Mth.clamp(1.80 + ((spdKmh - 5.0) / 9.0) * 1.20, 1.80, 3.0) * 1.6F;
                    float pitch = (float) Mth.clamp(1.05 - ((spdKmh - 5.0) / 15.0) * 0.18, 0.87, 1.08);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_IMPACT_LIGHT.get(), SoundSource.PLAYERS, vol, pitch + this.random.nextFloat() * 0.06F);
                } else if (spdKmh >= 1.5) {
                    // Low-speed tap: still clearly audible metallic tap!
                    float vol = (float) Mth.clamp(1.10 + ((spdKmh - 1.5) / 3.5) * 0.70, 1.10, 1.80) * 1.6F;
                    float pitch = (float) Mth.clamp(1.15 - ((spdKmh - 1.5) / 5.0) * 0.12, 1.03, 1.20);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_IMPACT_LIGHT.get(), SoundSource.PLAYERS, vol, pitch + this.random.nextFloat() * 0.06F);
                }

                // Dynamic Mesh Crumple Zone Deformation on Impact! (Raised threshold: >= 18.0 km/h)
                if (spdKmh >= 18.0) {
                    this.entityData.set(IMPACT_TRAUMA, (float) Math.min(1.0, 0.45 + (spdKmh / 35.0) * 0.55));
                    float kickIntensity = (float) Mth.clamp(spdKmh / 40.0, 0.4, 1.8);
                    float kickPitch = (physics.speed >= 0 ? -4.5F : 4.5F) * kickIntensity;
                    applyChassisImpulse(-0.09F * kickIntensity, kickPitch, (this.random.nextFloat() - 0.5F) * 3.5F * kickIntensity);
                    float dentRad = (float) Mth.clamp(0.65 + (spdKmh / 40.0) * 0.45, 0.65, 1.25);
                    float dentDepth = (float) Mth.clamp(0.06 + (spdKmh / 60.0) * 0.12, 0.06, 0.20);
                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                        this,
                        new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                            this.getId(),
                            (float) physics.impactLocalX, (float) physics.impactLocalY, (float) physics.impactLocalZ,
                            (float) physics.impactDirX, (float) physics.impactDirY, (float) physics.impactDirZ,
                            dentRad, dentDepth
                        )
                    );
                }
            }

            if (physics.justLandedHeavy) {
                physics.justLandedHeavy = false;
                this.entityData.set(IMPACT_TRAUMA, (float) Math.min(1.0, physics.landingImpactForce * 0.55));
                float vol = (float) Math.max(2.2, physics.landingImpactForce * 1.6);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CAR_LANDING_HEAVY.get(), SoundSource.NEUTRAL, vol, 0.95F + this.random.nextFloat() * 0.10F);
            } else if (physics.justLandedLight) {
                physics.justLandedLight = false;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CAR_LANDING_LIGHT.get(), SoundSource.NEUTRAL, 1.25F, 0.95F + this.random.nextFloat() * 0.10F);
            }

            if (physics.justSuspensionCreak) {
                physics.justSuspensionCreak = false;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.NEUTRAL, 0.80F, 0.92F + this.random.nextFloat() * 0.16F);
            }

            // Dynamic World Lighting ahead of vehicle when headlights are ON
            if (isHeadlightsOn()) {
                if (this.tickCount % 2 == 0) {
                    updateHeadlightDynamicIllumination();
                }
            } else if (!activeHeadlightBlocks.isEmpty()) {
        clearDynamicLight();
            }

            // Cabin interior illumination
            if (isCabinLightOn()) {
                if (this.tickCount % 4 == 0) {
                    updateCabinLight();
                }
            } else if (activeCabinLightPos != null) {
                clearCabinLight();
            }





            // DynamX-style Continuous Heavy Rigid-Body Pushing (Multi-sphere hull detection & shove)
            AABB carPushBox = this.getBoundingBox().inflate(3.50, 0.50, 3.50);
            List<DrivableVehicleEntity> otherCars = this.level().getEntitiesOfClass(DrivableVehicleEntity.class, carPushBox,
                other -> other != this && other.isAlive());

            float radA = (float) Math.toRadians(this.getYRot());
            double fwdAx = -Math.sin(radA);
            double fwdAz = Math.cos(radA);
            double[] sphereOffsets = {-1.80, 0.0, 1.80};
            double sphereRadius = 1.10;
            double sphereContactThresh = sphereRadius * 2.0;

            for (DrivableVehicleEntity other : otherCars) {
                float radB = (float) Math.toRadians(other.getYRot());
                double fwdBx = -Math.sin(radB);
                double fwdBz = Math.cos(radB);
                double rightBx = Math.cos(radB);
                double rightBz = Math.sin(radB);

                double bestOverlap = 0.0;
                double bestNx = 0.0;
                double bestNz = 0.0;
                double bestContactLocalFwdB = 0.0;

                for (double offA : sphereOffsets) {
                    double ax = this.getX() + fwdAx * offA;
                    double az = this.getZ() + fwdAz * offA;

                    for (double offB : sphereOffsets) {
                        double bx = other.getX() + fwdBx * offB;
                        double bz = other.getZ() + fwdBz * offB;

                        double ddx = bx - ax;
                        double ddz = bz - az;
                        double dist = Math.sqrt(ddx * ddx + ddz * ddz);

                        if (dist < sphereContactThresh) {
                            double overlap = sphereContactThresh - dist;
                            if (overlap > bestOverlap) {
                                bestOverlap = overlap;
                                if (dist > 0.001) {
                                    bestNx = ddx / dist;
                                    bestNz = ddz / dist;
                                } else {
                                    bestNx = fwdAx;
                                    bestNz = fwdAz;
                                }
                                bestContactLocalFwdB = offB;
                            }
                        }
                    }
                }

                if (bestOverlap > 0.001) {
                    double nx = bestNx;
                    double nz = bestNz;
                    double overlap = bestOverlap;

                    double speedA = this.physics.speed;
                    double pushDot = fwdAx * nx + fwdAz * nz;

                    // Moving car pushes stationary/other car forward & spins it realistically
                    if (Math.abs(speedA) > 0.05 && pushDot > 0.05) {
                        double shoveDist = Math.max(overlap * 0.75, Math.abs(speedA) * 0.04 * pushDot);
                        Vec3 shoveVec = new Vec3(nx * shoveDist, 0.0, nz * shoveDist);
                        other.move(MoverType.SELF, shoveVec);
                        other.setDeltaMovement(shoveVec);
                        other.hasImpulse = true;

                        // Impart rolling speed to pushed car
                        other.physics.speed = other.physics.speed * 0.30 + speedA * pushDot * 0.45;

                        // Heavy resistance while pushing another car (drag & quickly decelerate!)
                        this.physics.speed *= 0.72;

                        // Lateral push force in Car B's local coordinate frame
                        double latPush = (fwdAx * rightBx + fwdAz * rightBz) * speedA;

                        // Torque = lever arm * lateral force (front pushed right turns right; rear pushed right spins car left!)
                        if (Math.abs(bestContactLocalFwdB) > 0.25 && Math.abs(latPush) > 0.10) {
                            double torqueYaw = bestContactLocalFwdB * latPush * 4.5;
                            double spinImpulse = Mth.clamp(torqueYaw, -20.0, 20.0);
                            other.setYRot((float)(other.getYRot() + spinImpulse));
                            other.physics.deltaYaw += spinImpulse * 0.35;
                        }

                        // Collision / Impact Sound when pushing or ramming another car
                        if (Math.abs(speedA) >= 1.0 && this.tickCount % 6 == 0) {
                            if (Math.abs(speedA) >= 4.0) {
                                this.level().playSound(null, other.getX(), other.getY(), other.getZ(),
                                    ModSounds.CAR_CRASH_HEAVY.get(), SoundSource.NEUTRAL, 4.0F, 0.88F + this.random.nextFloat() * 0.12F);
                            } else {
                                this.level().playSound(null, other.getX(), other.getY(), other.getZ(),
                                    ModSounds.CAR_IMPACT_LIGHT.get(), SoundSource.NEUTRAL, 2.88F, 1.05F + this.random.nextFloat() * 0.15F);
                            }
                        }
                    } else {
                        // Soft static separation
                        double sep = overlap * 0.50;
                        other.move(MoverType.SELF, new Vec3(nx * sep, 0.0, nz * sep));
                        other.hasImpulse = true;
                    }

                    // Ramming kinetic impact at speed (> 15 km/h / 4.2 m/s)
                    if (Math.abs(speedA) > 4.2) {
                        float ramDmg = (float) (Math.pow(Math.abs(speedA), 1.45) * 1.5);
                        other.hurt(this.damageSources().generic(), ramDmg);
                        this.hurt(this.damageSources().generic(), ramDmg * 0.45F);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.CAR_CRASH_HEAVY.get(), SoundSource.NEUTRAL, 4.8F, 0.88F);

                        // Dynamic 3D Mesh Crumple Zones for both vehicles!
                        com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                            this,
                            new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                                this.getId(), 0.0F, 0.75F, -2.85F, 0.0F, 0.10F, 1.0F, 1.25F, 0.42F
                            )
                        );
                        float otherDefZ = (bestContactLocalFwdB > 0.5) ? 2.85F : (bestContactLocalFwdB < -0.5 ? -2.85F : 0.0F);
                        float otherDefDirZ = (bestContactLocalFwdB > 0.5) ? -1.0F : (bestContactLocalFwdB < -0.5 ? 1.0F : 0.0F);
                        float otherDefX = (otherDefZ == 0.0F) ? (nx > 0 ? 1.15F : -1.15F) : 0.0F;
                        float otherDefDirX = (otherDefZ == 0.0F) ? (nx > 0 ? -1.0F : 1.0F) : 0.0F;
                        com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                            other,
                            new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                                other.getId(), otherDefX, 0.75F, otherDefZ, otherDefDirX, 0.05F, otherDefDirZ, 1.15F, 0.38F
                            )
                        );
                    }
                }
            }



            // FRONT/REAR BUMPER RAMMING DAMAGE & CINEMATIC ROADKILL PHYSICS
            double currentSpeed = Math.abs(physics.speed);
            double currentSpeedKmh = currentSpeed * 3.6;

            // Decrement ram cooldowns
            this.rammedEntityCooldowns.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });

            if (currentSpeed >= 1.0 && Math.abs(physics.speed) > 0.22) {
                boolean isReversing = (physics.speed < 0.0);
                float yawR = (float) Math.toRadians(this.getYRot());
                double fX = -Math.sin(yawR);
                double fZ = Math.cos(yawR);

                // Active bumper position: in front when driving forward, at rear when in reverse
                double fwdBumper = Math.max(2.10, this.physics.frontBumperDist > 0.5 ? this.physics.frontBumperDist : 2.35);
                double rearBumper = Math.max(2.10, this.physics.rearBumperDist > 0.5 ? this.physics.rearBumperDist : 2.35);
                double bumperDist = isReversing ? -rearBumper : fwdBumper;
                double activeBumperX = this.getX() + fX * bumperDist;
                double activeBumperZ = this.getZ() + fZ * bumperDist;

                AABB activeBumperBox = new AABB(
                    activeBumperX - 1.25, this.getY() - 0.20, activeBumperZ - 1.25,
                    activeBumperX + 1.25, this.getY() + 1.30, activeBumperZ + 1.25
                );

                List<LivingEntity> hitEntities = this.level().getEntitiesOfClass(LivingEntity.class, activeBumperBox,
                    target -> target.getVehicle() == null && !this.hasPassenger(target) && target.isAlive()
                              && !(target.getId() == lastDismountPlayerId && dismountImmunityTicks > 0));

                for (LivingEntity target : hitEntities) {
                    if (this.rammedEntityCooldowns.containsKey(target.getId())) {
                        continue;
                    }
                    this.rammedEntityCooldowns.put(target.getId(), 14); // 0.7 second cooldown per entity to prevent multi-tick speed drain

                    if (currentSpeedKmh < com.evgen.cardecor.config.VehicleRammingConfig.damageSpeedKmh) {
                        // Low-speed (crawling / parking / walking pace): gentle nudge aside without damage, blood or camera shock!
                        Vec3 moveDir = this.getDeltaMovement();
                        double len = moveDir.length();
                        Vec3 pushDir = (len > 0.01) ? moveDir.normalize() : new Vec3(fX * (isReversing ? -1.0 : 1.0), 0, fZ * (isReversing ? -1.0 : 1.0));

                        // Relative lateral coordinate of contact (-1.0 to +1.0 across car width)
                        double dx = target.getX() - this.getX();
                        double dz = target.getZ() - this.getZ();
                        double localLat = dx * Math.cos(yawR) + dz * Math.sin(yawR);
                        double sideSign = (localLat >= 0) ? 1.0 : -1.0;
                        double rightX = Math.cos(yawR);
                        double rightZ = Math.sin(yawR);

                        // Gentle outward push away from bumper and slightly along car direction
                        double pushFwdMag = 0.08 + (currentSpeedKmh / 12.0) * 0.08;
                        double pushLatMag = 0.10;
                        double pushX = pushDir.x * pushFwdMag + rightX * sideSign * pushLatMag;
                        double pushZ = pushDir.z * pushFwdMag + rightZ * sideSign * pushLatMag;

                        target.push(pushX, 0.02, pushZ);
                        target.setPos(target.getX() + pushX * 0.40, target.getY(), target.getZ() + pushZ * 0.40);
                        continue;
                    }

                    double carMass = 1500.0;
                    com.evgen.cardecor.pack.VehicleDefinition vDef = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
                    if (vDef != null && vDef.physics != null && vDef.physics.mass > 100.0) {
                        carMass = vDef.physics.mass;
                    }
                    double targetMass = getMobMass(target);

                    // Speed in m/s & Kinetic energy of collision
                    double vMs = currentSpeedKmh / 3.6;
                    double mEff = (carMass * targetMass) / (carMass + targetMass);
                    double kineticEnergy = 0.5 * mEff * vMs * vMs;

                    // Pack ramming config & global server multipliers (zero hardcode)
                    com.evgen.cardecor.pack.VehicleDefinition.RammingConfig packRam = (vDef != null) ? vDef.ramming : null;
                    float packDmgMult = (packRam != null) ? packRam.damageDealtMult : 1.0F;
                    float serverDmgMult = com.evgen.cardecor.config.VehicleRammingConfig.damageDealtMultiplier;

                    // Realistic kinetic damage scaled to impact energy
                    float ramDamage = (float) Math.max(2.0, (kineticEnergy / 150.0) * packDmgMult * serverDmgMult);
                    if (currentSpeedKmh >= com.evgen.cardecor.config.VehicleRammingConfig.lethalObliterateSpeedKmh && targetMass <= 150.0) {
                        ramDamage = Math.max(ramDamage, 250.0F * packDmgMult * serverDmgMult); // Guaranteed cinematic lethal obliteration
                    }

                    DamageSource ds = this.damageSources().generic();
                    target.invulnerableTime = 0; // Penetrate vanilla invulnerability frames
                    boolean hurtSuccess = target.hurt(ds, ramDamage);

                    if (hurtSuccess || target.isAlive()) {
                        Vec3 moveDir = this.getDeltaMovement();
                        double len = moveDir.length();
                        Vec3 knockbackDir = (len > 0.01) ? moveDir.normalize() : new Vec3(fX * (isReversing ? -1.0 : 1.0), 0, fZ * (isReversing ? -1.0 : 1.0));

                        // Hit Intensity scaled by speed and mob mass
                        double hitIntensity = Math.min(1.0, (currentSpeedKmh / 40.0) * 0.55 + Math.min(1.5, targetMass / 150.0) * 0.45);

                        // Relative lateral coordinate of contact (-1.0 to +1.0 across car width)
                        double dx = target.getX() - this.getX();
                        double dz = target.getZ() - this.getZ();
                        double localLat = dx * Math.cos(yawR) + dz * Math.sin(yawR);
                        double absLat = Math.abs(localLat);

                        target.hurtMarked = true;

                        // 1. Momentum Retention (allows plowing cleanly through zombie hordes like in movies)
                        double rawRetain = carMass / (carMass + targetMass);
                        double packRetain = (packRam != null) ? packRam.momentumRetention : 0.95;
                        double retainFactor = Math.max(rawRetain, packRetain) * com.evgen.cardecor.config.VehicleRammingConfig.momentumRetentionMultiplier;
                        physics.speed *= Math.min(1.0, Math.max(0.1, retainFactor));

                        // Mob classification for contextual hit effects & damage
                        boolean isGolemOrMetal = (target instanceof net.minecraft.world.entity.animal.IronGolem);
                        boolean isSkeleton = (target instanceof net.minecraft.world.entity.monster.AbstractSkeleton);
                        boolean isSlimeOrCreeper = (target instanceof net.minecraft.world.entity.monster.Slime || target instanceof net.minecraft.world.entity.monster.Creeper);
                        boolean isSpider = (target instanceof net.minecraft.world.entity.monster.Spider);

                        // Recoil damage to car front based on impact energy (Moderate: bumper absorbs 15-25 zombies)
                        if (!isReversing && currentSpeedKmh >= com.evgen.cardecor.config.VehicleRammingConfig.damageSpeedKmh) {
                            double recoilDivisor = isGolemOrMetal ? com.evgen.cardecor.config.VehicleRammingConfig.metalGolemRecoilDivisor : com.evgen.cardecor.config.VehicleRammingConfig.biologicalRecoilDivisor;
                            float maxHitRecoil = isGolemOrMetal ? com.evgen.cardecor.config.VehicleRammingConfig.maxGolemRecoilPerHit : com.evgen.cardecor.config.VehicleRammingConfig.maxBiologicalRecoilPerHit;
                            float packRecoilMult = (packRam != null) ? packRam.recoilDamageMult : 1.0F;

                            float rawRecoil = (float) Math.min(maxHitRecoil, kineticEnergy / recoilDivisor);
                            float recoilDmg = rawRecoil * packRecoilMult * com.evgen.cardecor.config.VehicleRammingConfig.recoilDamageMultiplier;
                            applyFrontalImpactDamage(recoilDmg);
                        }

                        // 2. Cockpit Camera trauma (visceral, punchy cockpit shock only at speed)
                        float traumaVal = 0.0F;
                        if (currentSpeedKmh >= com.evgen.cardecor.config.VehicleRammingConfig.nudgeSpeedKmh) {
                            float packTraumaMult = (packRam != null) ? packRam.cameraTraumaMult : 1.0F;
                            traumaVal = (float) Math.min(0.95, (currentSpeedKmh - com.evgen.cardecor.config.VehicleRammingConfig.nudgeSpeedKmh) / 28.0 * 0.65 + hitIntensity * 0.25)
                                * packTraumaMult * com.evgen.cardecor.config.VehicleRammingConfig.cameraTraumaMultiplier;
                        }
                        this.entityData.set(IMPACT_TRAUMA, traumaVal);

                        // 3. Authentic Heavy Car Impact SFX & Splatter
                        if (isGolemOrMetal) {
                            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                net.minecraft.sounds.SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.1F, 0.85F);
                        } else if (isSkeleton) {
                            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                net.minecraft.sounds.SoundEvents.SKELETON_HURT, SoundSource.NEUTRAL, 1.0F, 0.8F);
                        } else {
                            float hitVol = (currentSpeedKmh < 18.0)
                                ? (float) Mth.clamp(0.12 + (currentSpeedKmh / 18.0) * 0.20, 0.12, 0.32)
                                : (float) Mth.clamp(0.35 + (currentSpeedKmh / 35.0) * 0.90, 0.35, 1.25);
                            float hitPitch = (currentSpeedKmh < 18.0)
                                ? (float) (0.80 + Math.random() * 0.08)
                                : (float) (0.92 + Math.random() * 0.16);
                            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                ModSounds.CAR_HIT_ENTITY.get(), SoundSource.NEUTRAL, hitVol, hitPitch);

                            // Blood splatter accumulation on vehicle body & windshield
                            if (!isSlimeOrCreeper) {
                                float packBloodMult = (packRam != null) ? packRam.bloodAccumulationMult : 1.0F;
                                float bloodGain = isSpider ? 1.5F : (float) Math.min(30.0, (currentSpeedKmh / 20.0) * (targetMass / 70.0) * 6.5);
                                setBloodLevel(getBloodLevel() + bloodGain * packBloodMult * com.evgen.cardecor.config.VehicleRammingConfig.bloodSplatterMultiplier);
                            }
                        }

                        // 4. Server-Side Particle Explosion based on mob type
                        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            if (isGolemOrMetal) {
                                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6, target.getZ(), 30, 0.4, 0.4, 0.4, 0.4);
                                serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                            } else if (isSkeleton) {
                                serverLevel.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.6, target.getZ(), 20, 0.3, 0.3, 0.3, 0.15);
                            } else if (isSlimeOrCreeper) {
                                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 0.6, target.getZ(), 24, 0.35, 0.35, 0.35, 0.2);
                            } else if (isSpider) {
                                serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 0.4, target.getZ(), 16, 0.3, 0.3, 0.3, 0.1);
                            } else {
                                int critCount = currentSpeedKmh >= 25.0 ? 28 : (currentSpeedKmh >= 18.0 ? 12 : 4);
                                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6, target.getZ(), critCount, 0.35, 0.35, 0.35, 0.32);
                                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 0.8, target.getZ(), Math.min(12, (int)(ramDamage * 0.5)), 0.25, 0.25, 0.25, 0.15);
                            }
                            if (currentSpeedKmh >= 20.0) {
                                serverLevel.sendParticles(ParticleTypes.POOF, activeBumperX, this.getY() + 0.35, activeBumperZ, 14, 0.5, 0.2, 0.5, 0.08);
                                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, activeBumperX, this.getY() + 0.25, activeBumperZ, 6, 0.3, 0.1, 0.3, 0.03);
                            }
                        }

                        // 5. THREE REALISTIC CINEMATIC KINEMATIC SCENARIOS:
                        float launchGlobalMult = com.evgen.cardecor.config.VehicleRammingConfig.launchVelocityMultiplier;
                        boolean canDeformMetal = (packRam == null || packRam.canDeformMetal);

                        if (absLat >= 0.35 && absLat <= 1.05) {
                            // -------------------------------------------------------------
                            // SCENARIO A: WHEELS RUN OVER THE BODY (Колёсный наезд / Переехал)
                            // -------------------------------------------------------------
                            if (currentSpeedKmh < com.evgen.cardecor.config.VehicleRammingConfig.nudgeSpeedKmh) {
                                // Gentle low-speed push along the asphalt
                                target.setDeltaMovement(
                                    knockbackDir.x * 0.06 * currentSpeed,
                                    0.01,
                                    knockbackDir.z * 0.06 * currentSpeed
                                );
                            } else {
                                boolean isLeftWheel = localLat > 0;
                                float bumpY = (float) (0.075 * hitIntensity);
                                float bumpRoll = isLeftWheel ? (float) (-0.095 * hitIntensity) : (float) (0.095 * hitIntensity);
                                applyChassisImpulse(bumpY, 0.025F, bumpRoll);

                                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.NEUTRAL, 0.90F, 0.85F);

                                float crushMult = (packRam != null ? packRam.wheelCrushDragMult : 1.0F) * launchGlobalMult;
                                target.setDeltaMovement(
                                    knockbackDir.x * 0.12 * currentSpeed * crushMult,
                                    -0.30 * crushMult,
                                    knockbackDir.z * 0.12 * currentSpeed * crushMult
                                );
                                target.setXRot(85.0F); // Ragdoll pitch flat on ground
                            }
                        } else if (absLat < 0.35) {
                            // -------------------------------------------------------------
                            // SCENARIO B: BODY ROLLS ACROSS HOOD / TAILGATE (Прокатился по капоту)
                            // -------------------------------------------------------------
                            if (currentSpeedKmh < com.evgen.cardecor.config.VehicleRammingConfig.nudgeSpeedKmh) {
                                // Gentle low-speed shove forward/backward without launching in air
                                target.setDeltaMovement(
                                    knockbackDir.x * 0.08 * currentSpeed,
                                    0.02,
                                    knockbackDir.z * 0.08 * currentSpeed
                                );
                            } else {
                                float diveY = (float) (-0.11 * hitIntensity);
                                float divePitch = isReversing ? (float) (4.2 * hitIntensity) : (float) (-4.5 * hitIntensity);
                                applyChassisImpulse(diveY, divePitch, 0.0F);

                                float vLaunchMult = (packRam != null ? packRam.hoodLaunchVerticalMult : 1.0F) * launchGlobalMult;
                                float fLaunchMult = (packRam != null ? packRam.hoodLaunchForwardMult : 1.0F) * launchGlobalMult;

                                // Target gets launched up, tumbles and backflips over the hood & windshield
                                double vKnock = Math.min(0.35 + currentSpeed * 0.065, 1.55) * vLaunchMult;
                                target.setDeltaMovement(
                                    knockbackDir.x * 0.26 * currentSpeed * fLaunchMult,
                                    vKnock,
                                    knockbackDir.z * 0.26 * currentSpeed * fLaunchMult
                                );
                                target.setYRot((float) (target.getYRot() + 75.0F + Math.random() * 90.0F));
                                target.setXRot((float) (-45.0F - Math.random() * 30.0F));

                                // Dent on bumper / hood in direction of travel
                                if (canDeformMetal && currentSpeedKmh >= com.evgen.cardecor.config.VehicleRammingConfig.metalDeformSpeedKmh && !this.level().isClientSide) {
                                    double hitLocalX = Mth.clamp(localLat, -0.85, 0.85);
                                    double hitZ = isReversing ? 2.80 : -2.80;
                                    double dirZ = isReversing ? -1.0 : 1.0;
                                    float deformRadius = (float) Math.min(1.10, 0.65 + currentSpeedKmh * 0.010);
                                    float deformDepth  = (float) Math.min(0.16, 0.05 + currentSpeedKmh * 0.003);
                                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                                        this,
                                        new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                                             this.getId(),
                                             (float) hitLocalX, 0.55F, (float) hitZ,
                                             0.0F, 0.0F, (float) dirZ,
                                             deformRadius, deformDepth
                                        )
                                    );
                                }
                            }
                        } else {
                            // -------------------------------------------------------------
                            // SCENARIO C: GLANCING BLOW / SIDE SWIPE (Чиркаш по борту)
                            // -------------------------------------------------------------
                            double sideX = Math.cos(yawR) * Math.signum(localLat);
                            double sideZ = Math.sin(yawR) * Math.signum(localLat);
                            if (currentSpeedKmh < com.evgen.cardecor.config.VehicleRammingConfig.nudgeSpeedKmh) {
                                target.setDeltaMovement(
                                    (knockbackDir.x * 0.5 + sideX * 0.7) * 0.06 * currentSpeed,
                                    0.02,
                                    (knockbackDir.z * 0.5 + sideZ * 0.7) * 0.06 * currentSpeed
                                );
                            } else {
                                float sideJolt = (float) (Math.signum(localLat) * -0.075 * hitIntensity);
                                applyChassisImpulse(0.0F, 0.0F, sideJolt);

                                float sideSpinMult = (packRam != null ? packRam.sideSwipeSpinMult : 1.0F) * launchGlobalMult;
                                target.setDeltaMovement(
                                    (knockbackDir.x * 0.5 + sideX * 0.7) * 0.28 * currentSpeed * sideSpinMult,
                                    0.25 * sideSpinMult,
                                    (knockbackDir.z * 0.5 + sideZ * 0.7) * 0.28 * currentSpeed * sideSpinMult
                                );
                                target.setYRot((float) (target.getYRot() + Math.signum(localLat) * 90.0F * sideSpinMult));

                                // Side door panel crumple on side swipe
                                if (canDeformMetal && currentSpeedKmh >= com.evgen.cardecor.config.VehicleRammingConfig.metalDeformSpeedKmh && !this.level().isClientSide) {
                                    float sideXPos = (localLat > 0) ? 1.05F : -1.05F;
                                    float pushInDirX = (localLat > 0) ? -1.0F : 1.0F;
                                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                                        this,
                                        new com.evgen.cardecor.network.VehicleImpactDeformPacket(
                                            this.getId(),
                                            sideXPos, 0.65F, 0.0F,
                                            pushInDirX, 0.05F, 0.0F,
                                            0.85F, 0.08F
                                        )
                                    );
                                }
                            }
                        }
                    }
                }
            }

            // Foliage bulldozing removed as requested (vehicle no longer breaks grass/crops)

            // Solid Obstacle Impact Sound, Deceleration & Frontal Cascading Damage
            if (this.horizontalCollision && currentSpeed >= 4.5) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CAR_CRASH_HEAVY.get(), SoundSource.NEUTRAL, 1.28F, 1.1F);
                physics.speed *= 0.75;
                if (this.physics.speed > 0.0) {
                    float blockImpactDamage = (float) Math.min(50.0, (currentSpeed - 3.5) * 7.5);
                    applyFrontalImpactDamage(blockImpactDamage);
                }
            }



            float impactShake = 0.0F;

            int activeFlats = 0;
            for (int i = 0; i < 4; i++) {
                if (isWheelInstalled(i) && physics.isTireFlat(i)) activeFlats++;
            }
            int missingWh = 4 - Integer.bitCount(this.entityData.get(INSTALLED_WHEELS) & 0xF);

            // Shudder vibration and thumping sound from flat tires or bare scraping hubs
            if (physics.isGrounded && Math.abs(physics.speed) > 0.15) {
                float flatVibe = (float) (activeFlats * 0.045 + missingWh * 0.080) * (float) Math.min(1.5, Math.abs(physics.speed) * 0.2);
                impactShake = Math.max(impactShake, Math.min(0.35F, flatVibe));

                if (activeFlats > 0 && Math.abs(physics.speed) > 0.4) {
                    int slapInterval = Math.max(3, (int) (14.0 / Math.max(0.5, Math.abs(physics.speed))));
                    if (this.tickCount % slapInterval == 0) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, net.minecraft.sounds.SoundSource.NEUTRAL, 0.65F, 0.70F);
                    }
                }
            }

            if (physics.speed > 0.1 && physics.speed < 4.2 && physics.engineRPM > 2400.0 && this.getFirstPassenger() != null) {
                // Subtle launch shudder vibration
                impactShake = Math.max(impactShake, 0.05F);
            }

            if (!physics.wasGrounded && physics.isGrounded && physics.airborneTime >= 0.18) {
                double fell = physics.lastFallBlocks;
                if (fell >= 1.2) {
                    impactShake = (float) Mth.clamp((fell - 1.0) * 0.035, 0.05, 0.40);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_IMPACT_LIGHT.get(), SoundSource.NEUTRAL, 1.12F, 1.0F);
                }
            }



            if (this.odeBody == null) {
                float spd = (float) physics.speed;
                if (Float.isNaN(spd) || Float.isInfinite(spd)) spd = 0.0F;
                this.entityData.set(SPEED, spd);

                float stAngle = (float) physics.steerAngle;
                if (Float.isNaN(stAngle) || Float.isInfinite(stAngle)) stAngle = 0.0F;
                this.entityData.set(STEER, stAngle);

                float rAngle = this.currentRoll;
                if (Float.isNaN(rAngle) || Float.isInfinite(rAngle)) rAngle = 0.0F;
                this.entityData.set(ROLL, rAngle);

                float pAngle = this.currentPitch;
                if (Float.isNaN(pAngle) || Float.isInfinite(pAngle)) pAngle = 0.0F;
                this.entityData.set(PITCH, pAngle);

                float sYaw = this.getYRot();
                if (Float.isNaN(sYaw) || Float.isInfinite(sYaw)) sYaw = 0.0F;
                this.entityData.set(SERVER_YAW, sYaw);

                float rpmVal = (float) physics.engineRPM;
                if (Float.isNaN(rpmVal) || Float.isInfinite(rpmVal)) rpmVal = 0.0F;
                this.entityData.set(RPM, rpmVal);

                double tThrust = physics.torqueNm * (physics.engineRPM / 4500.0) * (1.0 + physics.liveBoostPressure) * (this.physics.isBurnoutActive ? 1.0 : Math.max(0.2, Math.abs(throttle)));
                if (!this.isEngineOn() || Double.isNaN(tThrust) || Double.isInfinite(tThrust)) tThrust = 0.0;
                this.entityData.set(TORQUE, (float) tThrust);

                this.entityData.set(SCREECH, (float) physics.tireScreechIntensity);
                this.entityData.set(GEAR, physics.currentGear);
            }
            this.entityData.set(IMPACT_SHAKE, impactShake);
            this.entityData.set(BRAKING, isBraking());

            byte tireBits = 0;
            for (int i = 0; i < 4; i++) {
                if (physics.isTireFlat(i)) tireBits |= (byte) (1 << i);
            }
            this.entityData.set(TIRE_STATUS, tireBits);

            float dY = (float) physics.chassisDipY;
            if (Float.isNaN(dY) || Float.isInfinite(dY)) dY = 0.0F;
            this.entityData.set(CHASSIS_DIP_Y, dY);

            float dP = (float) Math.toDegrees(physics.chassisDipPitch);
            if (Float.isNaN(dP) || Float.isInfinite(dP)) dP = 0.0F;
            this.entityData.set(CHASSIS_DIP_PITCH, dP);

            float dR = (float) Math.toDegrees(physics.chassisDipRoll);
            if (Float.isNaN(dR) || Float.isInfinite(dR)) dR = 0.0F;
            this.entityData.set(CHASSIS_DIP_ROLL, dR);

            byte seatItemBits = 0;
            for (int s = 1; s < 4; s++) {
                if (isSeatBlockedByItems(s)) seatItemBits |= (byte) (1 << s);
            }
            this.entityData.set(SEAT_ITEM_STATUS, seatItemBits);

            float curTrauma = this.entityData.get(IMPACT_TRAUMA);
            if (curTrauma > 0.005F) {
                this.entityData.set(IMPACT_TRAUMA, curTrauma * 0.82F);
            } else if (curTrauma > 0.0F) {
                this.entityData.set(IMPACT_TRAUMA, 0.0F);
            }
        } else {
            tickLerp();

            // High-precision smooth yaw tracking & angular velocity continuation (Eliminates stuttering / ragged turns)
            float targetYaw = this.entityData.get(SERVER_YAW);
            if (Float.isNaN(targetYaw) || Float.isInfinite(targetYaw)) {
                targetYaw = (Float.isNaN(this.yRotO) || Float.isInfinite(this.yRotO)) ? 0.0F : this.yRotO;
            }
            if (Float.isNaN(this.getYRot()) || Float.isInfinite(this.getYRot())) {
                this.setYRot(targetYaw);
            }
            if (Float.isNaN(this.prevReceivedServerYaw)) {
                this.prevReceivedServerYaw = targetYaw;
                this.setYRot(targetYaw);
                this.yRotO = targetYaw;
            } else {
                float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
                if (Math.abs(yawDiff) > 60.0F) {
                    this.setYRot(targetYaw);
                    this.clientYawVelocity = 0.0F;
                    this.prevReceivedServerYaw = targetYaw;
                } else {
                    boolean isLocalDriver = (this.getFirstPassenger() == net.minecraft.client.Minecraft.getInstance().player);
                    if (isLocalDriver) {
                        this.setYRot(targetYaw);
                        this.clientYawVelocity = 0.0F;
                        this.prevReceivedServerYaw = targetYaw;
                    } else {
                        float serverStep = Mth.wrapDegrees(targetYaw - this.prevReceivedServerYaw);
                        if (Math.abs(serverStep) > 0.001F) {
                            this.clientYawVelocity = serverStep;
                            this.prevReceivedServerYaw = targetYaw;
                        } else {
                            this.clientYawVelocity *= 0.82F;
                        }
                        float step = this.clientYawVelocity * 0.65F + yawDiff * 0.35F;
                        this.setYRot(this.getYRot() + step);
                    }
                }
            }

            tickClientSounds();
            spawnClientParticles();
        }

        this.prevChassisDipY = this.currentChassisDipY;
        this.prevChassisDipPitch = this.currentChassisDipPitch;
        this.prevChassisDipRoll = this.currentChassisDipRoll;

        float targetDipY = this.entityData.get(CHASSIS_DIP_Y);
        float targetDipP = this.entityData.get(CHASSIS_DIP_PITCH);
        float targetDipR = this.entityData.get(CHASSIS_DIP_ROLL);

        this.currentChassisDipY += (targetDipY - this.currentChassisDipY) * 0.40F;
        this.currentChassisDipPitch += (targetDipP - this.currentChassisDipPitch) * 0.40F;
        this.currentChassisDipRoll += (targetDipR - this.currentChassisDipRoll) * 0.40F;

        // Smooth door & trunk angle interpolation (client visual)
        this.prevDoorLeftAngle = this.currentDoorLeftAngle;
        this.prevDoorRightAngle = this.currentDoorRightAngle;
        this.prevTrunkAngle = this.currentTrunkAngle;
        this.currentDoorLeftAngle += (this.entityData.get(DOOR_LEFT_ANGLE) - this.currentDoorLeftAngle) * 0.18F;
        this.currentDoorRightAngle += (this.entityData.get(DOOR_RIGHT_ANGLE) - this.currentDoorRightAngle) * 0.18F;
        this.currentTrunkAngle += (this.entityData.get(TRUNK_ANGLE) - this.currentTrunkAngle) * 0.18F;

        // Server-side door close timers
        if (!this.level().isClientSide) {
            if (doorLeftCloseTimer > 0 && --doorLeftCloseTimer == 0) {
                this.entityData.set(DOOR_LEFT_ANGLE, 0.0F);
            }
            if (doorRightCloseTimer > 0 && --doorRightCloseTimer == 0) {
                this.entityData.set(DOOR_RIGHT_ANGLE, 0.0F);
            }
            if (trunkCloseTimer > 0 && --trunkCloseTimer == 0) {
                this.entityData.set(TRUNK_ANGLE, 0.0F);
            }
        }

        updatePartPositions();
        processProfessionalHullCollisions();
    }



    private void spawnClientParticles() {
        if (!this.level().isClientSide) return;
        if (!this.isEngineOn() && Math.abs(this.entityData.get(SPEED)) < 0.01F) return;

        float yawRad = (float) Math.toRadians(this.getYRot());

        double fwdX = -Math.sin(yawRad);

        double fwdZ = Math.cos(yawRad);

        double rightX = Math.cos(yawRad);

        double rightZ = Math.sin(yawRad);



        float curSpeed = Math.abs(this.entityData.get(SPEED));

        float curSpeedKmh = curSpeed * 3.6F;

        float rpm = this.entityData.get(RPM);



        // 1. Volumetric Exhaust Smoke & Dynamic Lodestone Backfire Flashes
        if (isEngineOn()) {
            Vec3 exhPos = getExhaustWorldPos();
            double sideRearPipeX = exhPos.x;
            double sideRearPipeY = exhPos.y;
            double sideRearPipeZ = exhPos.z;

            int puffInterval = (rpm > 2800.0F) ? 1 : ((rpm > 1400.0F) ? 1 : 2);
            if (this.tickCount % puffInterval == 0) {
                int puffCount = (rpm > 3600.0F) ? 2 : 1;
                for (int p = 0; p < puffCount; p++) {
                    double motX = rightX * 0.055 - fwdX * (0.01 + curSpeed * 0.03) + (random.nextDouble() - 0.5) * 0.008;
                    double motZ = rightZ * 0.055 - fwdZ * (0.01 + curSpeed * 0.03) + (random.nextDouble() - 0.5) * 0.008;
                    double motY = 0.018 + (rpm / 6400.0F) * 0.014;

                    com.evgen.cardecor.client.VehicleVFX.spawnExhaustPuff(
                        this.level(),
                        sideRearPipeX + (random.nextDouble() - 0.5) * 0.012,
                        sideRearPipeY,
                        sideRearPipeZ + (random.nextDouble() - 0.5) * 0.012,
                        motX, motY, motZ, rpm, curSpeed
                    );
                }
            }
        }

        double frontLeftX  = this.getX() + fwdX * 1.55 + rightX * 0.75;
        double frontLeftZ  = this.getZ() + fwdZ * 1.55 + rightZ * 0.75;
        double frontRightX = this.getX() + fwdX * 1.55 - rightX * 0.75;
        double frontRightZ = this.getZ() + fwdZ * 1.55 - rightZ * 0.75;

        double rearLeftX   = this.getX() - fwdX * 1.25 + rightX * 0.75;
        double rearLeftZ   = this.getZ() - fwdZ * 1.25 + rightZ * 0.75;
        double rearRightX  = this.getX() - fwdX * 1.25 - rightX * 0.75;
        double rearRightZ  = this.getZ() - fwdZ * 1.25 - rightZ * 0.75;

        double tireContactY = this.getY() + 0.05;

        // Sample surface block beneath vehicle
        BlockPos surfacePos = BlockPos.containing(this.getX(), this.getY() - 0.20, this.getZ());
        BlockState surfaceState = this.level().getBlockState(surfacePos);
        if (surfaceState.isAir()) {
            surfaceState = this.level().getBlockState(surfacePos.below());
        }
        com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType surfaceType = 
            com.evgen.cardecor.physics.RaycastVehiclePhysics.getSurfaceType(surfaceState);

        boolean isGasDown = (this.inputFlags & 1) != 0 || (this.inputFlags & 2) != 0;
        boolean isSlipperySurface = (surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.ICE ||
                                     surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.SNOW ||
                                     surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.MUD);
        boolean isWheelspinning = isEngineOn() && isGasDown && (isSlipperySurface || rpm > 2000.0F);

        // 2. Ground Block Debris from wheels (rolling at > 3.5 km/h or active wheelspin/burnout)
        if (curSpeedKmh <= 3.5F && !this.physics.isBurnoutActive && !isWheelspinning) return;

        boolean isLaunching = (curSpeedKmh > 0.5F && curSpeedKmh < 18.0F && rpm > 2000.0F) || isWheelspinning;

        boolean isDrivingFast = (curSpeedKmh >= 14.0F);



        if (!surfaceState.isAir()) {

            if (isLaunching) {

                // Launch Wheelspin: Heavy shower of dirt/grass/gravel on REAR wheels + kick from FRONT wheels

                int rearCount = 4;

                for (int i = 0; i < rearCount; i++) {

                    double kickX = -fwdX * (0.04 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.10;

                    double kickZ = -fwdZ * (0.04 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.10;

                    double kickY = 0.05 + random.nextDouble() * 0.06;



                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        rearLeftX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearLeftZ + (random.nextDouble() - 0.5) * 0.22,

                        kickX, kickY, kickZ);

                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        rearRightX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearRightZ + (random.nextDouble() - 0.5) * 0.22,

                        kickX, kickY, kickZ);

                }



                // Front wheels turning launch dust

                for (int i = 0; i < 2; i++) {

                    double kickFrontX = -fwdX * (0.02 + random.nextDouble() * 0.02) + (random.nextDouble() - 0.5) * 0.06;

                    double kickFrontZ = -fwdZ * (0.02 + random.nextDouble() * 0.02) + (random.nextDouble() - 0.5) * 0.06;

                    double kickFrontY = 0.03 + random.nextDouble() * 0.04;



                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        frontLeftX + (random.nextDouble() - 0.5) * 0.18, tireContactY, frontLeftZ + (random.nextDouble() - 0.5) * 0.18,

                        kickFrontX, kickFrontY, kickFrontZ);

                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        frontRightX + (random.nextDouble() - 0.5) * 0.18, tireContactY, frontRightZ + (random.nextDouble() - 0.5) * 0.18,

                        kickFrontX, kickFrontY, kickFrontZ);

                }

            } else if (isDrivingFast && this.tickCount % 2 == 0) {

                // High speed rolling road dust from all 4 wheels

                double sprayRearX = -fwdX * (0.025 + curSpeed * 0.025) + (random.nextDouble() - 0.5) * 0.05;

                double sprayRearZ = -fwdZ * (0.025 + curSpeed * 0.025) + (random.nextDouble() - 0.5) * 0.05;

                double sprayY = 0.03 + random.nextDouble() * 0.025;



                // Rear wheels (dense spray)

                for (int i = 0; i < 2; i++) {

                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        rearLeftX + (random.nextDouble() - 0.5) * 0.15, tireContactY, rearLeftZ + (random.nextDouble() - 0.5) * 0.15,

                        sprayRearX, sprayY, sprayRearZ);

                    this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                        rearRightX + (random.nextDouble() - 0.5) * 0.15, tireContactY, rearRightZ + (random.nextDouble() - 0.5) * 0.15,

                        sprayRearX, sprayY, sprayRearZ);

                }



                // Front wheels (subtle spray)

                double sprayFrontX = -fwdX * (0.015 + curSpeed * 0.015) + (random.nextDouble() - 0.5) * 0.04;

                double sprayFrontZ = -fwdZ * (0.015 + curSpeed * 0.015) + (random.nextDouble() - 0.5) * 0.04;

                this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                    frontLeftX, tireContactY, frontLeftZ, sprayFrontX, sprayY * 0.8, sprayFrontZ);

                this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,

                    frontRightX, tireContactY, frontRightZ, sprayFrontX, sprayY * 0.8, sprayFrontZ);

            }

        }

        // 3. Kinetic Drift / Power Slide / Burnout Tire Smoke & Loose Surface Spray
        float screech = this.entityData.get(SCREECH);
        boolean isBurnout = this.physics.isBurnoutActive;
        boolean isDrifting = this.physics.isDrifting && Math.abs(this.physics.driftAngleDeg) > 10.0;
        boolean isSpinout = this.physics.isSpinningOut;
        boolean isHardSlide = (screech > 0.70F && Math.abs(this.physics.lateralSlipVel) > 0.50);

        boolean hasTireSlip = isBurnout || isSpinout || isDrifting || isHardSlide || isWheelspinning;

        if (hasTireSlip) {

            boolean isPaved = (surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.ASPHALT);
            boolean isGrass = (surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.GRASS);
            boolean isSnowOrIce = (surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.SNOW || surfaceType == com.evgen.cardecor.physics.RaycastVehiclePhysics.SurfaceType.ICE);

            if (isPaved) {
                // Realistic Paved Tire Smoke: Fine wispy SMOKE only during true drift, burnout, or spinout!
                // (Never in ordinary smooth turns!)
                if (this.tickCount % 2 == 0) {
                    int puffs = (isBurnout || isSpinout) ? 2 : 1;
                    for (int p = 0; p < puffs; p++) {
                        double smkMotY = 0.025 + random.nextDouble() * 0.035;
                        double smkMotX = -fwdX * (curSpeed * 0.015) + (random.nextDouble() - 0.5) * 0.025;
                        double smkMotZ = -fwdZ * (curSpeed * 0.015) + (random.nextDouble() - 0.5) * 0.025;

                        this.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, true,
                            rearLeftX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, rearLeftZ + (random.nextDouble() - 0.5) * 0.20,
                            smkMotX, smkMotY, smkMotZ);
                        this.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, true,
                            rearRightX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, rearRightZ + (random.nextDouble() - 0.5) * 0.20,
                            smkMotX, smkMotY, smkMotZ);

                        if (isSpinout) {
                            this.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, true,
                                frontLeftX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, frontLeftZ + (random.nextDouble() - 0.5) * 0.20,
                                smkMotX, smkMotY, smkMotZ);
                            this.level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, true,
                                frontRightX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, frontRightZ + (random.nextDouble() - 0.5) * 0.20,
                                smkMotX, smkMotY, smkMotZ);
                        }
                    }
                }
            } else if (isGrass) {
                // Grass / Moss: NO SMOKE! Instead, shredded grass clumps, dirt flecks, and light ground poof!
                if (!surfaceState.isAir() && this.tickCount % 2 == 0) {
                    int debrisCount = (isBurnout || isSpinout) ? 3 : 2;
                    for (int d = 0; d < debrisCount; d++) {
                        double kickX = -fwdX * 0.035 + (random.nextDouble() - 0.5) * 0.08;
                        double kickZ = -fwdZ * 0.035 + (random.nextDouble() - 0.5) * 0.08;
                        double kickY = 0.06 + random.nextDouble() * 0.06;

                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            rearLeftX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearLeftZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            rearRightX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearRightZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                    }
                    if (random.nextFloat() < 0.35F) {
                        this.level().addAlwaysVisibleParticle(ParticleTypes.POOF, true,
                            rearLeftX, tireContactY + 0.05, rearLeftZ, 0.0, 0.02, 0.0);
                        this.level().addAlwaysVisibleParticle(ParticleTypes.POOF, true,
                            rearRightX, tireContactY + 0.05, rearRightZ, 0.0, 0.02, 0.0);
                    }
                }
            } else if (isSnowOrIce) {
                // Snow / Ice: NO SMOKE! Snowflakes and ice/snow spray from driven wheels!
                if (this.tickCount % 2 == 0) {
                    double kickX = -fwdX * 0.04 + (random.nextDouble() - 0.5) * 0.08;
                    double kickZ = -fwdZ * 0.04 + (random.nextDouble() - 0.5) * 0.08;
                    String dt = (this.physics != null && this.physics.drivetrain != null) 
                        ? this.physics.drivetrain.toLowerCase(java.util.Locale.ROOT) : "rwd";
                    boolean sprayRear = !dt.equals("fwd");
                    boolean sprayFront = !dt.equals("rwd");

                    if (sprayRear) {
                        this.level().addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE, true,
                            rearLeftX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, rearLeftZ + (random.nextDouble() - 0.5) * 0.20,
                            kickX, 0.05, kickZ);
                        this.level().addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE, true,
                            rearRightX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, rearRightZ + (random.nextDouble() - 0.5) * 0.20,
                            kickX, 0.05, kickZ);
                        if (!surfaceState.isAir()) {
                            this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                                rearLeftX, tireContactY, rearLeftZ, kickX * 0.5, 0.04, kickZ * 0.5);
                            this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                                rearRightX, tireContactY, rearRightZ, kickX * 0.5, 0.04, kickZ * 0.5);
                        }
                    }

                    if (sprayFront) {
                        this.level().addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE, true,
                            frontLeftX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, frontLeftZ + (random.nextDouble() - 0.5) * 0.20,
                            kickX, 0.05, kickZ);
                        this.level().addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE, true,
                            frontRightX + (random.nextDouble() - 0.5) * 0.20, tireContactY + 0.05, frontRightZ + (random.nextDouble() - 0.5) * 0.20,
                            kickX, 0.05, kickZ);
                        if (!surfaceState.isAir()) {
                            this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                                frontLeftX, tireContactY, frontLeftZ, kickX * 0.5, 0.04, kickZ * 0.5);
                            this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                                frontRightX, tireContactY, frontRightZ, kickX * 0.5, 0.04, kickZ * 0.5);
                        }
                    }
                }
            } else {
                // Loose surfaces (Dirt, Gravel, Sand, Mud): NO SMOKE! Heavy ground block spray from driven wheels!
                if (!surfaceState.isAir() && this.tickCount % 2 == 0) {
                    double kickX = -fwdX * 0.04 + (random.nextDouble() - 0.5) * 0.09;
                    double kickZ = -fwdZ * 0.04 + (random.nextDouble() - 0.5) * 0.09;
                    double kickY = 0.07 + random.nextDouble() * 0.05;
                    String dt = (this.physics != null && this.physics.drivetrain != null) 
                        ? this.physics.drivetrain.toLowerCase(java.util.Locale.ROOT) : "rwd";
                    boolean sprayRear = !dt.equals("fwd");
                    boolean sprayFront = !dt.equals("rwd");

                    if (sprayRear) {
                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            rearLeftX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearLeftZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            rearRightX + (random.nextDouble() - 0.5) * 0.22, tireContactY, rearRightZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                    }
                    if (sprayFront) {
                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            frontLeftX + (random.nextDouble() - 0.5) * 0.22, tireContactY, frontLeftZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                        this.level().addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, surfaceState), true,
                            frontRightX + (random.nextDouble() - 0.5) * 0.22, tireContactY, frontRightZ + (random.nextDouble() - 0.5) * 0.22,
                            kickX, kickY, kickZ);
                    }
                }
            }
        }







        // 4. Damaged Engine Smoke - Strictly depends ONLY on damage/HP (100% constant buoyancy!)

        float hpRatio = getVehicleHealth() / MAX_HP;

        if (hpRatio <= 0.65F) {

            double hoodX = this.getX() + fwdX * 1.95;

            double hoodY = this.getY() + 0.72;

            double hoodZ = this.getZ() + fwdZ * 1.95;



            // Completely fixed thermal rise (zero speed or RPM influence!)

            double upflowY = 0.06;

            double backflowX = 0.0;

            double backflowZ = 0.0;



            if (hpRatio <= 0.65F && hpRatio > 0.35F) {

                // Stage 1: Radiator steam every 4 ticks

                if (this.tickCount % 4 == 0) {

                    com.evgen.cardecor.client.VehicleVFX.spawnDamageStage1(this.level(), hoodX, hoodY, hoodZ, backflowX, upflowY, backflowZ);

                }

            } else if (hpRatio <= 0.35F && hpRatio > 0.15F) {

                // Stage 2: Steady dark smoke every 2 ticks

                if (this.tickCount % 2 == 0) {

                    com.evgen.cardecor.client.VehicleVFX.spawnDamageStage2(this.level(), hoodX, hoodY, hoodZ, backflowX, upflowY, backflowZ);

                }

            } else {

                // Stage 3: Roaring fire and smoke

                com.evgen.cardecor.client.VehicleVFX.spawnDamageStage3(this.level(), hoodX, hoodY, hoodZ, backflowX, upflowY, backflowZ);

            }

        }

    }



    private void tickClientSounds() {
        if (!this.isEngineOn() && Math.abs(this.entityData.get(SPEED)) < 0.01F) {
            if (soundManagerInstance instanceof com.evgen.cardecor.client.VehicleEngineSoundManager sm) {
                sm.stopAll();
            }
            return;
        }

        if (soundManagerInstance == null) {
            soundManagerInstance = new com.evgen.cardecor.client.VehicleEngineSoundManager(this);
        }
        ((com.evgen.cardecor.client.VehicleEngineSoundManager) soundManagerInstance).tick();
    }



    @Override

    public void onClientRemoval() {

        if (this.odeBodyRegistered && !this.level().isClientSide) {
            com.evgen.cardecor.physics.ode.OdePhysicsWorld.get(this.level()).removeVehicle(this.odeBody);
            this.odeBodyRegistered = false;
        }

        super.onClientRemoval();

        if (soundManagerInstance instanceof com.evgen.cardecor.client.VehicleEngineSoundManager sm) {

            sm.stopAll();

        }

    }



    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        wakeUp();
        if (this.hasPassenger(player) || player.isPassenger()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Cleaning: Sponge or Water Bucket
        if (held.is(net.minecraft.world.item.Items.SPONGE) || held.is(net.minecraft.world.item.Items.WET_SPONGE)) {
            if (!this.level().isClientSide) {
                if (getDirtLevel() > 0.0F || getBloodLevel() > 0.0F) {
                    setDirtLevel(0.0F);
                    setBloodLevel(0.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.9F, 1.2F);
                    player.displayClientMessage(Component.literal("§a[Кузов] §fАвтомобиль отмыт до блеска!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§e[Кузов] §fАвтомобиль уже абсолютно чистый."), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (held.is(net.minecraft.world.item.Items.WATER_BUCKET) && (getDirtLevel() > 0.0F || getBloodLevel() > 0.0F)) {
            if (getTargetedHood(player) == null || getCoolantLevel() >= 100.0F) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    }
                    setDirtLevel(0.0F);
                    setBloodLevel(0.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[Кузов] §fАвтомобиль вымыт водой!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // 0. Hood / Mechanics interaction (ПКМ по капоту открывает диагностику и механику)
        if (getTargetedHood(player) != null) {
            if (held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get())) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setEngineOil(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fМоторное масло успешно заменено (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) || held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        if (held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                            player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                        } else {
                            held.shrink(1);
                        }
                    }
                    setCoolantLevel(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fОхлаждающая жидкость залита (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            openMechanicsMenu(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Wrench tool: Dismantle into key item (Shift + RMB) or Seat removal / Bodywork repair (RMB)
        if (held.getItem() instanceof com.evgen.cardecor.item.WrenchItem || held.is(com.evgen.cardecor.init.ModItems.WRENCH.get())) {
            if (player.isShiftKeyDown()) {
                return com.evgen.cardecor.item.WrenchItem.dismantleVehicle(this, player);
            } else {
                int maxSeats = getMaxSeats();
                for (int i = 0; i < maxSeats; i++) {
                    Vec3 sp = getSeatWorldPos(i);
                    if (player.getEyePosition().distanceTo(sp) < 2.2 && isSeatInstalled(i)) {
                        return uninstallSeat(i, player);
                    }
                }
                if (!this.level().isClientSide) {
                    float curHp = this.entityData.get(VEHICLE_HP);
                    if (curHp < MAX_HP) {
                        this.entityData.set(VEHICLE_HP, Math.min(MAX_HP, curHp + 60.0F));
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.85F, 1.2F);
                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                        this,
                        new com.evgen.cardecor.network.VehicleRepairPacket(this.getId())
                    );
                    player.displayClientMessage(Component.literal("§aКузов отремонтирован, вмятины выправлены!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Refueling with fuel items (Lava bucket, Coal, Charcoal, Coal Block, Blaze Rod/Powder)
        if (!held.isEmpty()) {

            float addFuel = 0.0F;

            boolean returnBucket = false;



            if (held.is(Items.LAVA_BUCKET)) {

                addFuel = 45.0F;

                returnBucket = true;

            } else if (held.is(Items.COAL) || held.is(Items.CHARCOAL)) {

                addFuel = 12.0F;

            } else if (held.is(Items.COAL_BLOCK)) {

                addFuel = 95.0F;

            } else if (held.is(Items.BLAZE_ROD)) {

                addFuel = 28.0F;

            } else if (held.is(Items.BLAZE_POWDER)) {

                addFuel = 14.0F;

            }



            if (addFuel > 0.0F) {
                float cur = getFuel();
                if (cur >= MAX_FUEL - 0.5F) {
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }

                if (!this.level().isClientSide) {
                    setFuel(cur + addFuel);
                    if (!player.isCreative()) {
                        held.shrink(1);
                        if (returnBucket) {
                            player.getInventory().add(new ItemStack(Items.BUCKET));
                        }
                    }

                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.1F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Key interaction: right-clicking vehicle with matching ignition key toggles door or trunk lock
        if (com.evgen.cardecor.item.VehiclePackKeyItem.matchesVehicle(held, this)) {
            // Check if player is aiming at trunk area
            VehicleDefinition defK = VehiclePackManager.get(this.getVehicleId());
            VehicleDefinition.CalibratedBounds cbK = (defK != null) ? defK.calibratedBounds : null;
            boolean isTrunkPK = "vintage_pickup".equalsIgnoreCase(this.getVehicleId())
                || (defK != null && defK.id != null && defK.id.contains("pickup"));
            double rDistK = (cbK != null && cbK.rearDist > 0.5) ? cbK.rearDist : (isTrunkPK ? 2.90 : 3.25);
            double cRearZK = (cbK != null && Math.abs(cbK.cabinRearZ) > 0.1) ? cbK.cabinRearZ : (isTrunkPK ? -0.75 : -0.75);
            double hWidthK = (cbK != null && cbK.halfWidth > 0.5) ? cbK.halfWidth : (isTrunkPK ? 1.15 : 1.25);
            double tHeightK = (cbK != null && cbK.trunkHeight > 0.5) ? cbK.trunkHeight : (isTrunkPK ? 1.38 : 1.35);

            Vec3 eyeK = player.getEyePosition();
            Vec3 reachK = eyeK.add(player.getViewVector(1.0F).scale(3.2));
            float yawRK = (float) Math.toRadians(this.getYRot());
            double fX = -Math.sin(yawRK), fZ = Math.cos(yawRK);
            double rX = Math.cos(yawRK), rZ = Math.sin(yawRK);
            double sX = eyeK.x - this.getX(), sY = eyeK.y - this.getY(), sZ = eyeK.z - this.getZ();
            double eX = reachK.x - this.getX(), eY = reachK.y - this.getY(), eZ = reachK.z - this.getZ();
            Vec3 lStartK = new Vec3(sX * rX + sZ * rZ, sY, sX * fX + sZ * fZ);
            Vec3 lEndK = new Vec3(eX * rX + eZ * rZ, eY, eX * fX + eZ * fZ);
            AABB tBoxK = new AABB(-hWidthK * 0.88, 0.25, -rDistK, hWidthK * 0.88, tHeightK + 0.05, cRearZK);

            if (tBoxK.clip(lStartK, lEndK).isPresent()) {
                if (!this.level().isClientSide) {
                    boolean newTrunkLock = !isTrunkLocked();
                    setTrunkLocked(newTrunkLock);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        newTrunkLock ? ModSounds.CAR_LOCK_PIN_DOWN.get() : ModSounds.CAR_UNLOCK_KEY.get(),
                        SoundSource.PLAYERS, 0.9F, 1.0F);
                    player.displayClientMessage(Component.literal(
                        newTrunkLock ? "§c[🔒] Багажник заперт!" : "§a[🔓] Багажник отперт!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            // Otherwise aiming at doors / chassis: toggle central door lock
            if (!this.level().isClientSide) {
                boolean newLock = !isLocked();
                setLocked(newLock);
                if (newLock) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_LOCK_PIN_DOWN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
                    player.displayClientMessage(Component.literal("§c[🔒] Автомобиль заперт!"), true);
                } else {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CAR_UNLOCK_KEY.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[🔓] Автомобиль открыт!"), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 1. Spare wheel interaction when right-clicking near the spare wheel mount
        if (supportsSpareWheel()) {
            Vec3 spPos = getSpareWheelWorldPos();
            if (player.getEyePosition().distanceTo(spPos) < 3.2) {
                Vec3 look = player.getViewVector(1.0F);
                AABB spBox = new AABB(spPos.x - 0.50, spPos.y - 0.50, spPos.z - 0.50, spPos.x + 0.50, spPos.y + 0.50, spPos.z + 0.50);
                if (spBox.clip(player.getEyePosition(), player.getEyePosition().add(look.scale(3.5))).isPresent()) {
                    if (!hasSpareWheel() && held.is(ModItems.VEHICLE_WHEEL.get())) {
                        return installSpareWheel(player, held);
                    }
                }
            }
        }

        // 2. Wheel replacement when holding VEHICLE_WHEEL and looking near any wheel
        if (held.is(ModItems.VEHICLE_WHEEL.get())) {
            for (int i = 0; i < 4; i++) {
                Vec3 wp = getWheelWorldPos(i);
                if (player.getEyePosition().distanceTo(wp) < 2.8) {
                    if (!isWheelInstalled(i) || this.physics.isTireFlat(i) || wheelHealth[i] < getMaxWheelHp() || (this.entityData.get(TIRE_STATUS) & (1 << i)) != 0) {
                        return installWheel(i, player, held);
                    }
                }
            }
        }

        // 2b. Seat installation when holding VEHICLE_SEAT and looking near any seat slot
        if (held.is(ModItems.VEHICLE_SEAT.get())) {
            int maxS = getMaxSeats();
            for (int i = 0; i < maxS; i++) {
                Vec3 sp = getSeatWorldPos(i);
                if (player.getEyePosition().distanceTo(sp) < 2.8) {
                    if (!isSeatInstalled(i)) {
                        return installSeat(i, player, held);
                    }
                }
            }
        }

        // 3. Trunk interaction: right-clicking trunk area opens trunk container (works on Fabric & Forge)
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean isTrunkPickup = (def != null && def.hasCargoBed());
        double rearDist = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (isTrunkPickup ? 2.90 : 3.25);
        double cabinRearZ = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : (isTrunkPickup ? -0.10 : -0.75);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (isTrunkPickup ? 1.15 : 1.25);
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : (isTrunkPickup ? 1.38 : 1.35);

        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(4.8));
        float yawR = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawR);
        double fwdZ = Math.cos(yawR);
        double rightX = Math.cos(yawR);
        double rightZ = Math.sin(yawR);

        double sDx = eye.x - this.getX(), sDy = eye.y - this.getY(), sDz = eye.z - this.getZ();
        double eDx = reach.x - this.getX(), eDy = reach.y - this.getY(), eDz = reach.z - this.getZ();
        Vec3 lStart = new Vec3(sDx * rightX + sDz * rightZ, sDy, sDx * fwdX + sDz * fwdZ);
        Vec3 lEnd   = new Vec3(eDx * rightX + eDz * rightZ, eDy, eDx * fwdX + eDz * fwdZ);

        double bedEndZ = isTrunkPickup ? -0.10 : cabinRearZ;
        AABB localTrunkBox = new AABB(-halfWidth * 0.90, 0.25, -rearDist, halfWidth * 0.90, trunkHeight + 0.45, bedEndZ);
        if (localTrunkBox.clip(lStart, lEnd).isPresent()) {
            if (!this.level().isClientSide) {
                openTrunk(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 4. Hood / Engine interaction: right-clicking hood area opens mechanics menu or refills oil/coolant
        boolean hasCargoBed = hasCargoBed();
        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (hasCargoBed ? 2.90 : 3.25);
        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? cb.cabinFrontZ : (hasCargoBed ? 0.65 : 0.85);
        double hoodHeight = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (hasCargoBed ? 1.46 : 1.48);
        double hWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (hasCargoBed ? 1.15 : 1.25);

        AABB localHoodBox = new AABB(-hWidth * 1.05, 0.10, cabinFrontZ - 0.20, hWidth * 1.05, hoodHeight + 0.60, frontDist + 0.80);
        boolean lookingAtFront = localHoodBox.clip(lStart, lEnd).isPresent()
            || (lStart.z > cabinFrontZ && lEnd.z < lStart.z && Math.abs(lStart.x) < hWidth * 1.6);

        if (lookingAtFront) {
            if (held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get())) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setEngineOil(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fМоторное масло успешно заменено (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) || held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        if (held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                            player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                        } else {
                            held.shrink(1);
                        }
                    }
                    setCoolantLevel(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fОхлаждающая жидкость залита (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            openMechanicsMenu(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Mounting via RMB (ПКМ) is disabled; players enter by pressing 'E' while aiming at a door.
        return InteractionResult.PASS;
    }

    public boolean isWheelInstalled(int index) {
        if (index < 0 || index > 3) return false;
        byte mask = this.entityData.get(INSTALLED_WHEELS);
        return (mask & (1 << index)) != 0;
    }

    public void setWheelInstalled(int index, boolean installed) {
        if (index < 0 || index > 3) return;
        byte mask = this.entityData.get(INSTALLED_WHEELS);
        if (installed) {
            mask |= (byte) (1 << index);
        } else {
            mask &= (byte) ~(1 << index);
        }
        this.entityData.set(INSTALLED_WHEELS, mask);
        if (this.physics != null) {
            this.physics.installedWheelsMask = mask;
        }
    }

    public Vec3 getWheelWorldPos(int wheelIndex) {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        if (def == null || def.wheels == null) return this.position();

        float[] wLocal = switch (wheelIndex) {
            case 0 -> def.wheels.frontLeft;
            case 1 -> def.wheels.frontRight;
            case 2 -> def.wheels.rearLeft;
            case 3 -> def.wheels.rearRight;
            default -> null;
        };
        if (wLocal == null) return this.position();

        float scale = (def.scale > 0.001F) ? def.scale : 1.0F;
        float gY = def.groundOffsetY;

        double lx = wLocal[0] * scale;
        double ly = (wLocal[1] + this.getSuspensionCompression(wheelIndex)) * scale + gY;
        double lz = wLocal[2] * scale;

        float smoothRollRad = this.currentRoll;
        float smoothPitchRad = this.currentPitch;

        double cR = Math.cos(smoothRollRad);
        double sR = Math.sin(smoothRollRad);
        double cP = Math.cos(smoothPitchRad);
        double sP = Math.sin(smoothPitchRad);

        // Exact 3D transform matching OpenGL: ZP(roll) then XP(pitch)
        double x1 = lx * cR - ly * sR;
        double y1 = lx * sR + ly * cR;
        double z1 = lz;

        double x2 = x1;
        double y2 = y1 * cP - z1 * sP;
        double z2 = y1 * sP + z1 * cP;

        float yawRad = (float) Math.toRadians(this.getYRot());
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        double wx = this.getX() + (-x2 * cosY + z2 * sinY);
        double wy = this.getY() + y2;
        double wz = this.getZ() + (-x2 * sinY - z2 * cosY);
        return new Vec3(wx, wy, wz);
    }

    public Vec3 getSeatWorldPosition(int seatIndex) {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.SeatDef sDef = (def != null && def.seats != null && seatIndex < def.seats.size()) ? def.seats.get(seatIndex) : null;
        double side = (sDef != null) ? sDef.pos[0] : (seatIndex == 0 ? -0.45 : (seatIndex == 1 ? 0.45 : -0.45));
        double height = (sDef != null) ? sDef.pos[1] : 0.22;
        double forward = (sDef != null) ? sDef.pos[2] : (seatIndex < 2 ? -0.75 : -1.55);

        float scale = (def != null && def.scale > 0.001F) ? def.scale : 1.0F;
        float gY = (def != null) ? def.groundOffsetY : 0.0F;

        double lx = side * scale;
        double ly = (height + 0.25) * scale + gY;
        double lz = forward * scale;

        float smoothRollRad = this.currentRoll;
        float smoothPitchRad = this.currentPitch;

        double cR = Math.cos(smoothRollRad);
        double sR = Math.sin(smoothRollRad);
        double cP = Math.cos(smoothPitchRad);
        double sP = Math.sin(smoothPitchRad);

        double x1 = lx * cR - ly * sR;
        double y1 = lx * sR + ly * cR;
        double z1 = lz;

        double x2 = x1;
        double y2 = y1 * cP - z1 * sP;
        double z2 = y1 * sP + z1 * cP;

        float yawRad = (float) Math.toRadians(this.getYRot());
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        double wx = this.getX() + (-x2 * cosY + z2 * sinY);
        double wy = this.getY() + y2;
        double wz = this.getZ() + (-x2 * sinY - z2 * cosY);
        return new Vec3(wx, wy, wz);
    }

    public int getPhysicalWheelIndex(boolean isFront, boolean isLeft) {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad), rightZ = Math.sin(yawRad);
        double carX = this.getX(), carZ = this.getZ();

        for (int i = 0; i < 4; i++) {
            Vec3 wp = this.getWheelWorldPos(i);
            double lX = (wp.x - carX) * rightX + (wp.z - carZ) * rightZ;
            double lZ = (wp.x - carX) * fwdX + (wp.z - carZ) * fwdZ;
            boolean matchFwd = isFront ? (lZ >= 0) : (lZ < 0);
            boolean matchSide = isLeft ? (lX >= 0) : (lX < 0);
            if (matchFwd && matchSide) return i;
        }
        return isFront ? (isLeft ? 0 : 1) : (isLeft ? 2 : 3);
    }

    public Vec3 getSpareWheelWorldPos() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        float scale = (def != null && def.scale > 0.001F) ? def.scale : 1.0F;
        float gY = (def != null) ? def.groundOffsetY : 0.0F;
        float rideHeight = (float) this.physics.rideHeight;

        double spLat = (def != null && def.spareWheelPos != null) ? (def.spareWheelPos[0] * scale) : (0.03 * scale);
        double spY = (def != null && def.spareWheelPos != null) ? (def.spareWheelPos[1] * scale + gY + rideHeight) : (1.36 * scale + gY + rideHeight);
        double spFwd = (def != null && def.spareWheelPos != null) ? (def.spareWheelPos[2] * scale) : (-0.38 * scale);

        double wx = this.getX() + rightX * spLat + fwdX * spFwd;
        double wy = this.getY() + spY;
        double wz = this.getZ() + rightZ * spLat + fwdZ * spFwd;
        return new Vec3(wx, wy, wz);
    }

    public InteractionResult removeSpareWheel(Player player) {
        if (!this.level().isClientSide) {
            setSpareWheel(false);

            ItemStack wheelItem = new ItemStack(ModItems.VEHICLE_WHEEL.get());
            if (!player.getInventory().add(wheelItem)) {
                Vec3 pos = getSpareWheelWorldPos();
                ItemEntity drop = new ItemEntity(this.level(), pos.x, pos.y, pos.z, wheelItem);
                this.level().addFreshEntity(drop);
            }

            Vec3 pos = getSpareWheelWorldPos();
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.1F);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);

            player.displayClientMessage(Component.literal("§a[Запаска] §fЗапасное колесо снято с кузова!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult installSpareWheel(Player player, ItemStack stack) {
        if (!this.level().isClientSide) {
            setSpareWheel(true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            Vec3 pos = getSpareWheelWorldPos();
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.6F);

            player.displayClientMessage(Component.literal("§a[Запаска] §fЗапасное колесо установлено на кузов!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult interactPart(VehiclePartEntity part, Player player, InteractionHand hand) {
        if (this.hasPassenger(player) || player.isPassenger()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Cleaning: Sponge or Water Bucket
        if (held.is(net.minecraft.world.item.Items.SPONGE) || held.is(net.minecraft.world.item.Items.WET_SPONGE)) {
            if (!this.level().isClientSide) {
                if (getDirtLevel() > 0.0F || getBloodLevel() > 0.0F) {
                    setDirtLevel(0.0F);
                    setBloodLevel(0.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.9F, 1.2F);
                    player.displayClientMessage(Component.literal("§a[Кузов] §fАвтомобиль отмыт до блеска!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§e[Кузов] §fАвтомобиль уже абсолютно чистый."), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        boolean isHood = (part == this.partHood || "hood".equals(part.partType) || (getTargetedHood(player) != null && !part.partType.startsWith("wheel_")));
        if (held.is(net.minecraft.world.item.Items.WATER_BUCKET) && (getDirtLevel() > 0.0F || getBloodLevel() > 0.0F)) {
            if (!isHood || getCoolantLevel() >= 100.0F) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    }
                    setDirtLevel(0.0F);
                    setBloodLevel(0.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[Кузов] §fАвтомобиль вымыт водой!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // 0. HOOD / MECHANICS INTERACTION (Always opens mechanics when aiming at or clicking hood)
        if (isHood) {
            if (held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get())) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setEngineOil(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.9F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fМоторное масло успешно заменено (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) || held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        if (held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                            player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                        } else {
                            held.shrink(1);
                        }
                    }
                    setCoolantLevel(100.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§a[Механика] §fОхлаждающая жидкость залита (100%)!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            openMechanicsMenu(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 1. WRENCH INTERACTION (Unscrew individual wheel or dismantle whole car)
        if (held.getItem() instanceof com.evgen.cardecor.item.WrenchItem || held.is(ModItems.WRENCH.get())) {
            if (part.partType.startsWith("wheel_")) {
                int wheelIndex = switch (part.partType) {
                    case "wheel_fl" -> 0;
                    case "wheel_fr" -> 1;
                    case "wheel_rl" -> 2;
                    case "wheel_rr" -> 3;
                    default -> -1;
                };
                if (wheelIndex >= 0) {
                    if (isWheelInstalled(wheelIndex)) {
                        return uninstallWheel(wheelIndex, player);
                    } else if (player.isShiftKeyDown()) {
                        return uninstallBrake(wheelIndex, player);
                    } else {
                        return InteractionResult.sidedSuccess(this.level().isClientSide);
                    }
                }
            } else if (player.isShiftKeyDown()) {
                return com.evgen.cardecor.item.WrenchItem.dismantleVehicle(this, player);
            } else if (part == this.partCabin) {
                int maxSeats = getMaxSeats();
                for (int i = 0; i < maxSeats; i++) {
                    Vec3 sp = getSeatWorldPos(i);
                    if (player.getEyePosition().distanceTo(sp) < 2.5 && isSeatInstalled(i)) {
                        return uninstallSeat(i, player);
                    }
                }
                // Bodywork repair & un-denting with wrench
                if (!this.level().isClientSide) {
                    float curHp = this.entityData.get(VEHICLE_HP);
                    if (curHp < MAX_HP) {
                        this.entityData.set(VEHICLE_HP, Math.min(MAX_HP, curHp + 60.0F));
                    }
                    float curTrans = getTransmissionCondition();
                    if (curTrans < 100.0F) {
                        setTransmissionCondition(Math.min(100.0F, curTrans + 45.0F));
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.85F, 1.2F);
                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                        this,
                        new com.evgen.cardecor.network.VehicleRepairPacket(this.getId())
                    );
                    player.displayClientMessage(Component.literal("§aКузов и трансмиссия отремонтированы!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                // Bodywork repair & un-denting with wrench
                if (!this.level().isClientSide) {
                    float curHp = this.entityData.get(VEHICLE_HP);
                    if (curHp < MAX_HP) {
                        this.entityData.set(VEHICLE_HP, Math.min(MAX_HP, curHp + 60.0F));
                    }
                    float curTrans = getTransmissionCondition();
                    if (curTrans < 100.0F) {
                        setTransmissionCondition(Math.min(100.0F, curTrans + 45.0F));
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.85F, 1.2F);
                    com.evgen.cardecor.network.ModNetwork.sendToTrackingAndSelf(
                        this,
                        new com.evgen.cardecor.network.VehicleRepairPacket(this.getId())
                    );
                    player.displayClientMessage(Component.literal("§aКузов и трансмиссия отремонтированы!"), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // 2. SPARE WHEEL PART INTERACTION (Remove or Install spare wheel on pickup)
        if (part == this.partSpareWheel || "spare_wheel".equals(part.partType)) {
            boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId())
                || (VehiclePackManager.get(this.getVehicleId()) != null && VehiclePackManager.get(this.getVehicleId()).id.contains("pickup"));
            if (isPickup) {
                if (!hasSpareWheel() && held.is(ModItems.VEHICLE_WHEEL.get())) {
                    return installSpareWheel(player, held);
                }
            }
        }

        // 3. VEHICLE WHEEL ITEM INTERACTION (Mount / Install / Replace wheel on wheel hub)
        if (held.is(ModItems.VEHICLE_WHEEL.get())) {
            if (part.partType.startsWith("wheel_")) {
                int wheelIndex = switch (part.partType) {
                    case "wheel_fl" -> 0;
                    case "wheel_fr" -> 1;
                    case "wheel_rl" -> 2;
                    case "wheel_rr" -> 3;
                    default -> -1;
                };
                if (wheelIndex >= 0) {
                    // Install if missing, broken, or flat tire!
                    if (!isWheelInstalled(wheelIndex) || this.physics.isTireFlat(wheelIndex) || wheelHealth[wheelIndex] < getMaxWheelHp() || (this.entityData.get(TIRE_STATUS) & (1 << wheelIndex)) != 0) {
                        return installWheel(wheelIndex, player, held);
                    } else {
                        return InteractionResult.sidedSuccess(this.level().isClientSide);
                    }
                }
            }
        }

        // 3a. VEHICLE BRAKE ITEM INTERACTION (Mount / Upgrade brake on empty hub)
        if (held.getItem() instanceof com.evgen.cardecor.item.VehicleBrakeItem || held.is(ModItems.VEHICLE_BRAKE.get())) {
            if (part.partType.startsWith("wheel_")) {
                int wheelIndex = switch (part.partType) {
                    case "wheel_fl" -> 0;
                    case "wheel_fr" -> 1;
                    case "wheel_rl" -> 2;
                    case "wheel_rr" -> 3;
                    default -> -1;
                };
                if (wheelIndex >= 0) {
                    if (!isWheelInstalled(wheelIndex)) {
                        return installBrake(wheelIndex, player, held);
                    } else {
                        if (!this.level().isClientSide) {
                            player.displayClientMessage(Component.literal("§c[!] Сначала снимите колесо гаечным ключом, чтобы получить доступ к тормозам!"), true);
                        }
                        return InteractionResult.sidedSuccess(this.level().isClientSide);
                    }
                }
            }
        }

        // 3b. VEHICLE ENGINE ITEM INTERACTION (Mount engine under hood)
        if (held.getItem() instanceof com.evgen.cardecor.item.VehicleEngineItem || held.is(ModItems.VEHICLE_ENGINE.get())) {
            float yawR = (float) Math.toRadians(this.getYRot());
            double fwdX = -Math.sin(yawR);
            double fwdZ = Math.cos(yawR);
            double pLocalZ = (player.getX() - this.getX()) * fwdX + (player.getZ() - this.getZ()) * fwdZ;
            if (pLocalZ > 0.35 || part == this.partChassis) {
                if (!this.level().isClientSide) {
                    if (hasEngine()) {
                        player.displayClientMessage(Component.literal("§c[!] В автомобиле уже установлен двигатель!"), true);
                    } else {
                        String engId = com.evgen.cardecor.item.VehicleEngineItem.getEngineId(held);
                        float cond = com.evgen.cardecor.item.VehicleEngineItem.getCondition(held);
                        float oil = com.evgen.cardecor.item.VehicleEngineItem.getOil(held);
                        float quality = com.evgen.cardecor.item.VehicleEngineItem.getQuality(held);

                        setInstalledEngine(engId);
                        setEngineCondition(cond);
                        setEngineOil(oil);
                        setEngineQuality(quality);

                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                        }

                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 0.8F);

                        player.displayClientMessage(Component.literal("§a[Двигатель] §fДвигатель смонтирован в подкапотное пространство!"), true);
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // 3c. VEHICLE SEAT ITEM INTERACTION (Mount / Install seat on cabin slot)
        if (held.is(ModItems.VEHICLE_SEAT.get())) {
            if (part == this.partCabin || part == this.partChassis) {
                int maxSeats = getMaxSeats();
                for (int i = 0; i < maxSeats; i++) {
                    Vec3 sp = getSeatWorldPos(i);
                    if (player.getEyePosition().distanceTo(sp) < 3.0 && !isSeatInstalled(i)) {
                        return installSeat(i, player, held);
                    }
                }
            }
        }

        // 4. TRUNK INTERACTION (Open trunk container)
        if (part == this.partTrunk || "trunk".equals(part.partType)) {
            if (!this.level().isClientSide) {
                openTrunk(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 5. DOOR TOGGLE INTERACTION (Shift + Right click to open/close door manually)
        if (player.isShiftKeyDown() && held.isEmpty() && (part == this.partCabin || part == this.partChassis)) {
            if (!this.level().isClientSide) {
                Vec3 rel = player.position().subtract(this.position());
                float rad = (float) Math.toRadians(-this.getYRot());
                double localX = rel.x * Math.cos(rad) - rel.z * Math.sin(rad);
                if (localX < 0) {
                    float cur = this.entityData.get(DOOR_LEFT_ANGLE);
                    this.entityData.set(DOOR_LEFT_ANGLE, cur > 5.0F ? 0.0F : 65.0F);
                    this.doorLeftCloseTimer = (cur > 5.0F) ? 0 : 120;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        cur > 5.0F ? ModSounds.CAR_ENTER.get() : ModSounds.CAR_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    float cur = this.entityData.get(DOOR_RIGHT_ANGLE);
                    this.entityData.set(DOOR_RIGHT_ANGLE, cur > 5.0F ? 0.0F : 65.0F);
                    this.doorRightCloseTimer = (cur > 5.0F) ? 0 : 120;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        cur > 5.0F ? ModSounds.CAR_ENTER.get() : ModSounds.CAR_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 6. MOTOR OIL & COOLANT REFILL AT HOOD
        if (held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get()) && (part == this.partHood || "hood".equals(part.partType) || part == this.partChassis)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                setEngineOil(100.0F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.9F);
                player.displayClientMessage(Component.literal("§a[Механика] §fМоторное масло успешно заменено (100%)!"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if ((held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) || held.is(net.minecraft.world.item.Items.WATER_BUCKET)) && (part == this.partHood || "hood".equals(part.partType) || part == this.partChassis)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    if (held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    } else {
                        held.shrink(1);
                    }
                }
                setCoolantLevel(100.0F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§a[Механика] §fОхлаждающая жидкость залита (100%)!"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 7. HOOD INTERACTION: Open mechanics / diagnostics menu (Project Zomboid style)
        if (part == this.partHood || "hood".equals(part.partType)) {
            openMechanicsMenu(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (part == this.partChassis) {
            float yawR = (float) Math.toRadians(this.getYRot());
            double fwdX = -Math.sin(yawR);
            double fwdZ = Math.cos(yawR);
            double pLocalZ = (player.getX() - this.getX()) * fwdX + (player.getZ() - this.getZ()) * fwdZ;
            if (pLocalZ > 0.35) {
                openMechanicsMenu(player);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        return this.interact(player, hand);
    }

    public InteractionResult installWheel(int wheelIndex, Player player, ItemStack stack) {
        if (!this.level().isClientSide) {
            String tireId = com.evgen.cardecor.item.VehicleWheelItem.getTireId(stack);
            String rimId = com.evgen.cardecor.item.VehicleWheelItem.getRimId(stack);
            String wheelCompound = tireId + ";" + rimId;
            setInstalledWheelId(wheelIndex, wheelCompound);
            setWheelInstalled(wheelIndex, true);

            com.evgen.cardecor.pack.TireDefinition tDef = com.evgen.cardecor.pack.ModularWheelRegistry.getTire(tireId);
            com.evgen.cardecor.pack.RimDefinition rDef = com.evgen.cardecor.pack.ModularWheelRegistry.getRim(rimId);
            wheelHealth[wheelIndex] = (tDef != null && tDef.durabilityHp > 0) ? (float) tDef.durabilityHp : getMaxWheelHp();
            tireCondition[wheelIndex] = 100.0F;
            physics.tireAir[wheelIndex] = 1.0F;
            physics.tireDeflateRate[wheelIndex] = 0.0F;
            if (physics.tireCondition != null) {
                physics.tireCondition[wheelIndex] = 100.0F;
            }

            byte t = (byte) (this.entityData.get(TIRE_STATUS) & ~(1 << wheelIndex));
            this.entityData.set(TIRE_STATUS, t);
            applyAllWheelsPhysics();

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            Vec3 pos = getWheelWorldPos(wheelIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.6F);

            String[] wheelNames = {"Переднее левое", "Переднее правое", "Заднее левое", "Заднее правое"};
            String wName = (tDef != null ? tDef.name : "Покрышка") + " + " + (rDef != null ? rDef.name : "Диск");
            player.displayClientMessage(Component.literal("§a[Колеса] §e" + wheelNames[wheelIndex] + " §f(" + wName + ") установлено!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult uninstallWheel(int wheelIndex, Player player) {
        if (!this.level().isClientSide) {
            String tireId = getInstalledTireId(wheelIndex);
            String rimId = getInstalledRimId(wheelIndex);
            setWheelInstalled(wheelIndex, false);
            wheelHealth[wheelIndex] = 0.0F;
            tireCondition[wheelIndex] = 0.0F;
            if (physics.tireCondition != null) {
                physics.tireCondition[wheelIndex] = 0.0F;
            }
            applyAllWheelsPhysics();

            ItemStack wheelItem = com.evgen.cardecor.item.VehicleWheelItem.createWheel(tireId, rimId);
            if (!player.getInventory().add(wheelItem)) {
                Vec3 pos = getWheelWorldPos(wheelIndex);
                ItemEntity drop = new ItemEntity(this.level(), pos.x, pos.y, pos.z, wheelItem);
                this.level().addFreshEntity(drop);
            }

            Vec3 pos = getWheelWorldPos(wheelIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.1F);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);

            String[] wheelNames = {"Переднее левое", "Переднее правое", "Заднее левое", "Заднее правое"};
            String wName = (wheelIndex >= 0 && wheelIndex < 4) ? wheelNames[wheelIndex] : "Колесо";
            player.displayClientMessage(Component.literal("§e[Колеса] §f" + wName + " колесо снято!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult installBrake(int wheelIndex, Player player, ItemStack stack) {
        if (!this.level().isClientSide) {
            String brakeId = com.evgen.cardecor.item.VehicleBrakeItem.getBrakeId(stack);
            String oldBrakeId = getInstalledBrakeId(wheelIndex);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (oldBrakeId != null && !oldBrakeId.isEmpty() && !"brake_02_stock_vented".equals(oldBrakeId)) {
                ItemStack oldBrakeItem = com.evgen.cardecor.item.VehicleBrakeItem.create(oldBrakeId);
                if (!player.getInventory().add(oldBrakeItem)) {
                    Vec3 pos = getWheelWorldPos(wheelIndex);
                    this.level().addFreshEntity(new ItemEntity(this.level(), pos.x, pos.y, pos.z, oldBrakeItem));
                }
            }

            setInstalledBrakeId(wheelIndex, brakeId);

            Vec3 pos = getWheelWorldPos(wheelIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, net.minecraft.sounds.SoundSource.PLAYERS, 1.1F, 1.0F);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.5F);

            com.evgen.cardecor.pack.BrakeDefinition bDef = com.evgen.cardecor.pack.ModularWheelRegistry.getBrake(brakeId);
            String bName = (bDef != null && bDef.name != null) ? bDef.name : "Тормоз";
            String[] wheelNames = {"Передний левый", "Передний правый", "Задний левый", "Задний правый"};
            player.displayClientMessage(Component.literal("§a[Тормоза] §e" + wheelNames[wheelIndex] + ": §fустановлен " + bName + "!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult uninstallBrake(int wheelIndex, Player player) {
        if (!this.level().isClientSide) {
            String oldBrakeId = getInstalledBrakeId(wheelIndex);
            if (oldBrakeId == null || oldBrakeId.isEmpty()) {
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            setInstalledBrakeId(wheelIndex, "");

            ItemStack oldBrakeItem = com.evgen.cardecor.item.VehicleBrakeItem.create(oldBrakeId);
            if (!player.getInventory().add(oldBrakeItem)) {
                Vec3 pos = getWheelWorldPos(wheelIndex);
                this.level().addFreshEntity(new ItemEntity(this.level(), pos.x, pos.y, pos.z, oldBrakeItem));
            }

            Vec3 pos = getWheelWorldPos(wheelIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);

            String[] wheelNames = {"Передний левый", "Передний правый", "Задний левый", "Задний правый"};
            player.displayClientMessage(Component.literal("§e[Тормоза] §f" + wheelNames[wheelIndex] + " тормоз снят со ступицы!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public void breakOffWheel(int wheelIndex) {
        if (!this.level().isClientSide) {
            String wheelId = getInstalledWheelId(wheelIndex);
            setWheelInstalled(wheelIndex, false);
            wheelHealth[wheelIndex] = 0.0F;
            tireCondition[wheelIndex] = 0.0F;
            if (physics.tireCondition != null) {
                physics.tireCondition[wheelIndex] = 0.0F;
            }
            applyAllWheelsPhysics();

            ItemStack wheelItem = com.evgen.cardecor.item.VehicleWheelItem.createWheel(wheelId);
            Vec3 partPos = getWheelWorldPos(wheelIndex);
            ItemEntity drop = new ItemEntity(this.level(), partPos.x, partPos.y, partPos.z, wheelItem);
            drop.setDeltaMovement(
                (this.random.nextDouble() - 0.5) * 0.4 + this.getDeltaMovement().x,
                0.35 + this.random.nextDouble() * 0.2,
                (this.random.nextDouble() - 0.5) * 0.4 + this.getDeltaMovement().z
            );
            drop.setDefaultPickUpDelay();
            this.level().addFreshEntity(drop);

            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, partPos.x, partPos.y + 0.2, partPos.z, 12, 0.2, 0.2, 0.2, 0.05);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, partPos.x, partPos.y + 0.2, partPos.z, 8, 0.3, 0.3, 0.3, 0.1);
            }

            com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
            float sVol = (def != null && def.sounds != null) ? def.sounds.tireExplodeVolume : 1.35F;
            float sPitch = (def != null && def.sounds != null) ? def.sounds.tireExplodePitch : 1.0F;

            this.level().playSound(null, partPos.x, partPos.y, partPos.z,
                ModSounds.TIRE_EXPLODE.get(), net.minecraft.sounds.SoundSource.NEUTRAL, sVol * 1.20F, sPitch * (0.85F + this.random.nextFloat() * 0.15F));
        }
    }

    public Vec3 getSeatWorldPos(int seatIndex) {
        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        if (def == null || def.seats == null || seatIndex >= def.seats.size()) return this.position().add(0, 0.5, 0);
        com.evgen.cardecor.pack.VehicleDefinition.SeatDef sDef = def.seats.get(seatIndex);
        float[] sLocal = sDef.pos;
        float scale = (def.scale > 0.001F) ? def.scale : 1.0F;
        float gY = def.groundOffsetY;

        double lx = sLocal[0] * scale;
        double ly = sLocal[1] * scale + gY;
        double lz = sLocal[2] * scale;

        float yawRad = (float) Math.toRadians(this.getYRot());
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        double worldX = this.getX() + (-lx * cosY + lz * sinY);
        double worldY = this.getY() + ly;
        double worldZ = this.getZ() + (-lx * sinY - lz * cosY);
        return new Vec3(worldX, worldY, worldZ);
    }

    public InteractionResult installSeat(int seatIndex, Player player, ItemStack stack) {
        if (seatIndex < 0 || seatIndex >= getMaxSeats()) return InteractionResult.PASS;
        if (isCargoBedSeat(seatIndex)) {
            player.displayClientMessage(Component.literal("§c[Салон] В кузов нельзя установить стандартное сиденье!"), true);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            String seatId = com.evgen.cardecor.item.VehicleSeatItem.getSeatId(stack);
            setInstalledSeatId(seatIndex, seatId);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            Vec3 pos = getSeatWorldPos(seatIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            com.evgen.cardecor.pack.SeatDefinition sDef = com.evgen.cardecor.pack.SeatRegistry.get(seatId);
            String name = (sDef != null && sDef.name != null) ? sDef.name : "Сиденье";
            player.displayClientMessage(Component.literal("§a[Салон] §e" + name + " §fустановлено на место #" + (seatIndex + 1) + "!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult uninstallSeat(int seatIndex, Player player) {
        if (seatIndex < 0 || seatIndex >= getMaxSeats()) return InteractionResult.PASS;
        if (isSeatOccupiedByPassenger(seatIndex)) {
            player.displayClientMessage(Component.literal("§c[Салон] Сначала пассажир должен выйти из кресла!"), true);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!isSeatInstalled(seatIndex)) {
            player.displayClientMessage(Component.literal("§e[Салон] Здесь нет установленного сиденья!"), true);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            String seatId = getInstalledSeatId(seatIndex);
            setInstalledSeatId(seatIndex, "none");
            ItemStack seatItem = com.evgen.cardecor.item.VehicleSeatItem.createSeat(seatId);
            if (!player.getInventory().add(seatItem)) {
                Vec3 pos = getSeatWorldPos(seatIndex);
                ItemEntity drop = new ItemEntity(this.level(), pos.x, pos.y, pos.z, seatItem);
                this.level().addFreshEntity(drop);
            }
            Vec3 pos = getSeatWorldPos(seatIndex);
            this.level().playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
            com.evgen.cardecor.pack.SeatDefinition sDef = com.evgen.cardecor.pack.SeatRegistry.get(seatId);
            String name = (sDef != null && sDef.name != null) ? sDef.name : "Сиденье";
            player.displayClientMessage(Component.literal("§e[Салон] §f" + name + " демонтировано!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunction) {

        if (this.hasPassenger(passenger)) {

            int passIdx = this.getPassengers().indexOf(passenger);

            int seatMask = this.entityData.get(SEAT_SLOTS);

            int seatIndex = (seatMask >> (passIdx * 4)) & 0xF;

            com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
            com.evgen.cardecor.pack.VehicleDefinition.SeatDef sDef = null;
            if (def != null && def.seats != null && !def.seats.isEmpty()) {
                if (seatIndex < def.seats.size()) {
                    sDef = def.seats.get(seatIndex);
                } else {
                    sDef = def.seats.get(0);
                }
            }

            float carYaw = this.getYRot();
            if (Float.isNaN(carYaw) || Float.isInfinite(carYaw)) carYaw = 0.0F;
            float yawRad = (float) Math.toRadians(carYaw);
            double cosY = Math.cos(yawRad);
            double sinY = Math.sin(yawRad);

            double sideOffset = 0.0;
            double heightOffset = 0.22;
            double forwardOffset = 0.18;
            boolean lockYaw = true;
            float yawOffset = 0.0F;

            if (sDef != null) {
                sideOffset = sDef.pos[0];
                heightOffset = sDef.pos[1];
                forwardOffset = sDef.pos[2];
                lockYaw = sDef.lockYaw;
                yawOffset = sDef.yawOffsetDeg;
            } else {
                if (seatIndex == 0) {
                    sideOffset = -0.45;
                    forwardOffset = -0.75;
                } else if (seatIndex == 1) {
                    sideOffset = 0.45;
                    forwardOffset = -0.75;
                } else {
                    sideOffset = (seatIndex % 2 == 0 ? -0.45 : 0.45);
                    forwardOffset = -1.55;
                    lockYaw = true;
                }
            }

            com.evgen.cardecor.pack.SeatDefinition modularSeat = com.evgen.cardecor.pack.SeatRegistry.get(getInstalledSeatId(seatIndex));
            if (modularSeat != null) {
                heightOffset += modularSeat.sittingHeightOffset;
            }

            float scale = (def != null) ? def.scale : 1.0F;
            float gY = (def != null) ? def.groundOffsetY : 0.0F;

            float cDipY = this.entityData.get(CHASSIS_DIP_Y);
            float cDipPitchDeg = this.entityData.get(CHASSIS_DIP_PITCH);
            float cDipRollDeg = this.entityData.get(CHASSIS_DIP_ROLL);

            double lx = sideOffset * scale;
            double ly = (heightOffset * scale) + gY + cDipY;
            double lz = forwardOffset * scale;

            double worldX;
            double worldY;
            double worldZ;

            if (this.odeBody != null || this.entityData.get(QUAT_W) != 1.0F || this.entityData.get(QUAT_X) != 0.0F || this.entityData.get(QUAT_Y) != 0.0F || this.entityData.get(QUAT_Z) != 0.0F) {
                // 6-DOF Quaternion 3D Transformation around Center of Mass (COM)
                float comY = (this.odeBody != null) ? this.odeBody.comHeight : 0.65F;
                org.joml.Vector3f relSeat = new org.joml.Vector3f((float) lx, (float) (ly - comY), (float) lz);
                org.joml.Quaternionf q = getSmoothRotation(1.0F, new org.joml.Quaternionf());
                q.transform(relSeat);

                worldX = this.getX() + relSeat.x;
                worldY = this.getY() + comY + relSeat.y;
                worldZ = this.getZ() + relSeat.z;
            } else {
                double rollRad = this.currentRoll + Math.toRadians(cDipRollDeg);
                double pitchRad = this.currentPitch + Math.toRadians(cDipPitchDeg);

                double cR = Math.cos(rollRad);
                double sR = Math.sin(rollRad);
                double cP = Math.cos(pitchRad);
                double sP = Math.sin(pitchRad);

                // Fallback Euler rotation
                double x1 = lx * cR - ly * sR;
                double y1 = lx * sR + ly * cR;
                double z1 = lz;

                double x2 = x1;
                double y2 = y1 * cP - z1 * sP;
                double z2 = y1 * sP + z1 * cP;

                worldX = this.getX() + (-x2 * cosY + z2 * sinY);
                worldY = this.getY() + y2;
                worldZ = this.getZ() + (-x2 * sinY - z2 * cosY);
            }

            if (Double.isNaN(worldX) || Double.isInfinite(worldX)) worldX = this.getX();
            if (Double.isNaN(worldY) || Double.isInfinite(worldY)) worldY = this.getY() + 0.5;
            if (Double.isNaN(worldZ) || Double.isInfinite(worldZ)) worldZ = this.getZ();

            moveFunction.accept(passenger, worldX, worldY, worldZ);

            if (passenger instanceof LivingEntity living) {
                float rYaw = carYaw + yawOffset;
                if (!Float.isNaN(rYaw) && !Float.isInfinite(rYaw)) {
                    if (lockYaw) {
                        living.setYBodyRot(rYaw);
                        if (!(passenger instanceof Player)) {
                            living.setYRot(rYaw);
                            living.setYHeadRot(rYaw);
                        }
                    }
                }
            }

        }

    }





    /**
     * Professional Multi-Box OBB (Oriented Bounding Box) Compound Collision & Solid Platform Engine.
     * Supports:
     * 1. Walking, sprinting, crouching, and lying down (prone / crawling / sniping) on Hood, Roof, Trunk, and Bed.
     * 2. Smooth climbing / step-up onto bumpers, hood, bed, and roof.
     * 3. Solid perimeter boundary walls preventing clipping / walking through vehicle textures.
     * 4. Moving / rotating platform physics with full inertia.
     */
    public void processProfessionalHullCollisions() {
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;

        boolean isPickup = "vintage_pickup".equalsIgnoreCase(this.getVehicleId()) || (def != null && def.id != null && def.id.contains("pickup"));

        // Exact calibrated vehicle geometry in meters (tight to visual model - full hood & trunk coverage!)
        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (isPickup ? 2.90 : 3.25);
        double rearDist = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (isPickup ? 2.90 : 3.25);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (isPickup ? 1.15 : 1.25);

        // Stepped cabin boundaries in vehicle local space (Z > 0 is FRONT/Hood, Z < 0 is REAR/Trunk)
        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? Math.abs(cb.cabinFrontZ) : (isPickup ? 1.05 : 0.85);
        double cabinRearZ = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? -Math.abs(cb.cabinRearZ) : (isPickup ? -0.35 : -0.75);

        // Exact calibrated deck heights (top walk/lie surfaces)
        double hoodHeight = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (isPickup ? 1.46 : 1.48);
        double roofHeight = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (isPickup ? 2.48 : 2.45);
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : (isPickup ? 1.38 : 1.35);
        double bumperStepHeight = (cb != null) ? cb.bumperStepHeight : 0.55;

        float yawRad = (float) Math.toRadians(this.getYRot());
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double fwdX = -sinY;
        double fwdZ = cosY;
        double rightX = cosY;
        double rightZ = sinY;

        double maxFwdReach = Math.max(frontDist, rearDist) + 1.2;
        AABB broadphase = new AABB(
            this.getX() - maxFwdReach, this.getY() - 1.5, this.getZ() - maxFwdReach,
            this.getX() + maxFwdReach, this.getY() + roofHeight + 3.5, this.getZ() + maxFwdReach
        );
        List<Entity> nearbyEntities = this.level().getEntitiesOfClass(Entity.class, broadphase,
            ent -> ent != this && !(ent instanceof VehiclePartEntity) && !(ent instanceof DrivableVehicleEntity) && !this.hasPassenger(ent) && ent.getVehicle() == null && ent.getRootVehicle() != this && ent.isAlive() && !ent.noPhysics);

        this.currentDeckEntityIds.clear();
        float totalDeckStaticDipY = 0.0F;
        float totalDeckStaticPitch = 0.0F;
        float totalDeckStaticRoll = 0.0F;
        int deckEntityCount = 0;

        for (Entity ent : nearbyEntities) {
            if (ent == this || this.hasPassenger(ent) || ent.getVehicle() != null || ent.getRootVehicle() == this) {
                continue;
            }

            boolean wasAlreadyOnDeck = this.prevDeckEntityIds.contains(ent.getId());

            // Frame-to-frame coordinate continuity:
            // If the entity was already standing on the deck in the previous tick, compute relative
            // coordinates using the car's PREVIOUS tick position/yaw (where the entity was placed),
            // eliminating the 1-tick latency error that causes 20 Hz platform jitter/shaking!
            double refX = wasAlreadyOnDeck ? this.xo : this.getX();
            double refY = wasAlreadyOnDeck ? this.yo : this.getY();
            double refZ = wasAlreadyOnDeck ? this.zo : this.getZ();
            float refYawRad = (float) Math.toRadians(wasAlreadyOnDeck ? this.yRotO : this.getYRot());
            double refFwdX = -Math.sin(refYawRad);
            double refFwdZ = Math.cos(refYawRad);
            double refRightX = Math.cos(refYawRad);
            double refRightZ = Math.sin(refYawRad);

            double dx = ent.getX() - refX;
            double dy = ent.getY() - refY;
            double dz = ent.getZ() - refZ;

            // Transform world relative coordinate into vehicle local space
            // localFwd > 0: FRONT (Hood), localFwd < 0: REAR (Trunk/Bed)
            // localLat > 0: LEFT (Driver side), localLat < 0: RIGHT (Passenger side)
            double localFwd = dx * refFwdX + dz * refFwdZ;
            double localLat = dx * refRightX + dz * refRightZ;
            double localY = dy; // feet Y relative to car origin

            double entRadius = ent.getBbWidth() * 0.50;

            // Dynamic body half width at entity's local forward position (doors are snug, tires slightly wider)
            double currentBodyHalfW;
            double wheelFrontLocalZ = isPickup ? 2.05 : 2.00;
            double wheelRearLocalZ = isPickup ? -1.25 : -1.65;
            boolean nearWheelFwd = Math.abs(localFwd - wheelFrontLocalZ) < 0.55 || Math.abs(localFwd - wheelRearLocalZ) < 0.55;
            if (nearWheelFwd) {
                currentBodyHalfW = Math.max(halfWidth * 1.02, 1.22); // outer tire edge
            } else {
                currentBodyHalfW = halfWidth * 1.02; // door panels, hood, trunk
            }

            double mobExtraMargin = (ent instanceof net.minecraft.world.entity.Mob) ? 0.22 : 0.0;

            // Check if within vehicle bounding footprint (tight bounds matching car body)
            boolean withinLength = (localFwd >= -rearDist - 0.20 - mobExtraMargin && localFwd <= frontDist + 0.20 + mobExtraMargin);
            boolean withinWidth = (Math.abs(localLat) <= currentBodyHalfW + 0.20 + mobExtraMargin);

            if (!withinLength || !withinWidth) {
                continue;
            }

            // Continuous Deck Height Profile across vehicle length:
            // Hood -> Windshield ramp -> Roof -> Rear window ramp -> Trunk
            double windshieldStart = (cb != null && Math.abs(cb.windshieldBottomZ) > 0.1) ? Math.abs(cb.windshieldBottomZ) : (cabinFrontZ + 0.35);
            double windshieldEnd   = (cb != null && Math.abs(cb.windshieldTopZ) > 0.1) ? Math.abs(cb.windshieldTopZ) : (cabinFrontZ - 0.20);
            double rearWindowStart = cabinRearZ + 0.20;
            double rearWindowEnd   = cabinRearZ - 0.50;

            boolean hasCargoBed = isPickup || (cb != null && cb.hasCargoBed);
            double bedFloorHeight = (cb != null && cb.bedFloorHeight > 0.1) ? cb.bedFloorHeight : 0.78;

            double bedFrontZ = isPickup ? -0.32 : (cb != null && Math.abs(cb.cabinRearZ) > 0.1 ? cb.cabinRearZ : -0.32);
            boolean isNearSpareWheel = isPickup && (localFwd >= -0.65 && localFwd <= -0.15 && Math.abs(localLat) <= 0.50);

            // Inside cargo bed floor cavity (down inside the bed, between rear bulkhead and tailgate)
            // Note: If player jumps, mantles or stands at roof/spare wheel/side-rail elevation, they are NOT inside the bed floor cavity!
            boolean inBedLength = (localFwd <= bedFrontZ && localFwd >= -rearDist + 0.15);
            boolean inBedZone = hasCargoBed && inBedLength && (Math.abs(localLat) <= halfWidth - 0.10)
                && (localY >= bedFloorHeight - 0.45 && localY <= bedFloorHeight + 0.60);
            boolean isInsideCargoBed = inBedZone;

            // Automatically absorb thrown items into pickup cargo bed / trunk
            if (!this.level().isClientSide && ent instanceof net.minecraft.world.entity.item.ItemEntity itemEntity && itemEntity.isAlive()) {
                if (isInsideCargoBed) {
                    ItemStack st = itemEntity.getItem();
                    if (!st.isEmpty()) {
                        ItemStack rem = this.trunkInventory.addItem(st);
                        if (rem.getCount() < st.getCount()) {
                            this.level().playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                ModSounds.VEHICLE_ITEM_PUT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
                        }
                        if (rem.isEmpty()) {
                            itemEntity.discard();
                        } else {
                            itemEntity.setItem(rem);
                            // Full cargo bed: softly roll/drop remaining item out behind tailgate onto the ground
                            double ejectX = -refFwdX * 0.25 + (this.random.nextDouble() - 0.5) * 0.12;
                            double ejectZ = -refFwdZ * 0.25 + (this.random.nextDouble() - 0.5) * 0.12;
                            itemEntity.setDeltaMovement(ejectX, 0.12, ejectZ);
                        }
                    }
                }
                continue;
            }

            double effectiveTrunkHeight = trunkHeight;
            if (isInsideCargoBed) {
                effectiveTrunkHeight = bedFloorHeight;
            }

            double deckHeight;
            if (hasCargoBed) {
                // PICKUP TRUCK CONTINUOUS DECK PROFILE (Gapless solid coverage across bumper, bed, spare tire, cab and roof):
                if (localFwd > frontDist - 0.15) {
                    deckHeight = bumperStepHeight;
                } else if (localFwd > windshieldStart) {
                    // Hood is flat and solid all the way to the front grille / bullbar!
                    deckHeight = hoodHeight;
                } else if (localFwd >= windshieldEnd) {
                    // Windshield slope from hoodHeight to roofHeight
                    double t = Mth.clamp((windshieldStart - localFwd) / (windshieldStart - windshieldEnd), 0.0, 1.0);
                    deckHeight = hoodHeight + (roofHeight - hoodHeight) * t;
                } else if (localY < 1.20 && localFwd <= bedFrontZ) {
                    // Bed floor is 100% solid all the way forward to the front bulkhead / spare tire!
                    deckHeight = bedFloorHeight;
                } else if (isNearSpareWheel && localY >= 1.15 && localY <= 2.10) {
                    // Top surface of mounted spare tire — solid platform for standing/mantling!
                    deckHeight = 1.78;
                } else if (localFwd > -0.35 && (localY >= 1.60 || (wasAlreadyOnDeck && localY >= roofHeight - 0.75))) {
                    // Cabin roof
                    deckHeight = roofHeight;
                } else if (localFwd >= -0.70 && (localY >= 1.15 || (wasAlreadyOnDeck && localY >= roofHeight - 0.85))) {
                    // Climbing ramp / mantle step between cargo bed/spare tire and cabin roof
                    double t = Mth.clamp((localFwd - (-0.70)) / 0.35, 0.0, 1.0);
                    double baseStep = isNearSpareWheel ? 1.78 : (bedFloorHeight + 0.50);
                    deckHeight = baseStep + (roofHeight - baseStep) * t;
                } else {
                    // Bed floor fallback
                    deckHeight = bedFloorHeight;
                }
            } else {
                // Standard sedan / coupe profile with sloped rear glass:
                if (localFwd > frontDist - 0.15) {
                    deckHeight = bumperStepHeight;
                } else if (localFwd > windshieldStart) {
                    // Hood is flat and solid all the way to the front bumper edge!
                    deckHeight = hoodHeight;
                } else if (localFwd >= windshieldEnd) {
                    // Windshield slope: ramps smoothly from hoodHeight up to roofHeight
                    double t = Mth.clamp((windshieldStart - localFwd) / (windshieldStart - windshieldEnd), 0.0, 1.0);
                    deckHeight = hoodHeight + (roofHeight - hoodHeight) * t;
                } else if (localFwd > rearWindowStart) {
                    // Cabin Roof
                    deckHeight = roofHeight;
                } else if (localFwd >= rearWindowEnd) {
                    // Rear window slope: ramps smoothly from roofHeight down to trunkHeight
                    double t = Mth.clamp((localFwd - rearWindowEnd) / (rearWindowStart - rearWindowEnd), 0.0, 1.0);
                    deckHeight = effectiveTrunkHeight + (roofHeight - effectiveTrunkHeight) * t;
                } else if (localFwd <= -rearDist + 0.35) {
                    deckHeight = bumperStepHeight;
                } else if (localFwd < -rearDist + 0.65) {
                    double t = Mth.clamp((localFwd - (-rearDist + 0.35)) / 0.30, 0.0, 1.0);
                    deckHeight = bumperStepHeight + (effectiveTrunkHeight - bumperStepHeight) * t;
                } else {
                    // Trunk / tailgate is flat and solid all the way to the rear bumper edge!
                    deckHeight = effectiveTrunkHeight;
                }
            }

            // 3D dynamic tilt transformation (pitch & roll lean when tires are flat, missing, or on slopes)
            float cDipY = this.entityData.get(CHASSIS_DIP_Y);
            float cDipP = (float) Math.toRadians(this.entityData.get(CHASSIS_DIP_PITCH));
            float cDipR = (float) Math.toRadians(this.entityData.get(CHASSIS_DIP_ROLL));

            double tiltedDeckLocalY = deckHeight + cDipY + localFwd * Math.sin(this.currentPitch + cDipP) - localLat * Math.sin(this.currentRoll + cDipR);
            double targetWorldY = this.getY() + tiltedDeckLocalY;
            
            // 1. SOLID DECK PLATFORM & ROOFTOP PARKOUR (Standing, Running, Sprinting, Jumping, Crouching, Prone)
            double lowestDeckSurface = hasCargoBed ? bedFloorHeight : Math.min(bumperStepHeight, Math.min(hoodHeight, effectiveTrunkHeight));
            boolean isPlayer = ent instanceof Player;

            // Only players can mount deck / stand on car! Mobs NEVER mount deck.
            boolean canMountDeck = false;
            if (isPlayer) {
                Player playerEnt = (Player) ent;
                boolean isHoldingShiftOnGround = playerEnt.isShiftKeyDown() && !wasAlreadyOnDeck && !isInsideCargoBed;

                if (!isHoldingShiftOnGround) {
                    boolean isNearCarBounds = (localFwd >= -rearDist - 0.45 && localFwd <= frontDist + 0.45)
                        && (Math.abs(localLat) <= halfWidth + 0.45);

                    if (wasAlreadyOnDeck) {
                        // Walking or moving while already on top of the car
                        canMountDeck = localY >= lowestDeckSurface - 0.60;
                    } else if (isInsideCargoBed) {
                        // Standing in pickup cargo bed
                        canMountDeck = true;
                    } else {
                        // Stepping, mantling, or landing onto vehicle surface from any height / phase of jump:
                        canMountDeck = isNearCarBounds && localY >= (deckHeight - 0.45);
                    }
                }
            }

            if (canMountDeck) {
                Vec3 curMotion = ent.getDeltaMovement();
                float entryFallDist = ent.fallDistance;
                double entryPrevY = ent.yo;
                double deltaY = ent.getY() - ent.yo;

                Vec3 prevLPos = this.prevDeckLocalPos.get(ent.getId());
                double dLF = (prevLPos != null && wasAlreadyOnDeck) ? (localFwd - prevLPos.x) : 0.0;
                double dLL = (prevLPos != null && wasAlreadyOnDeck) ? (localLat - prevLPos.z) : 0.0;
                double localMoveSq = dLF * dLF + dLL * dLL;
                boolean hasRelativeMovement = wasAlreadyOnDeck && (localMoveSq > 0.0006);

                boolean hasWalkInput = false;
                if (ent instanceof Player player) {
                    hasWalkInput = Math.abs(player.zza) > 0.05F || Math.abs(player.xxa) > 0.05F;
                }
                boolean isMovingOnDeck = hasRelativeMovement || (hasWalkInput && (localMoveSq > 0.00008 || ent.getDeltaMovement().horizontalDistanceSqr() > 0.001));

                boolean wasAirborne = this.airborneDeckEntityIds.contains(ent.getId());
                boolean isJumpingTakeoff = (curMotion.y > 0.05) || (deltaY > 0.05);
                boolean isAirborneAbove = isJumpingTakeoff || (wasAirborne && ent.getY() > targetWorldY + 0.08);

                if (isJumpingTakeoff) {
                    this.airborneDeckEntityIds.add(ent.getId());
                    double curApex = this.deckMaxAirborneY.getOrDefault(ent.getId(), ent.getY());
                    this.deckMaxAirborneY.put(ent.getId(), Math.max(curApex, Math.max(ent.getY(), entryPrevY)));
                }

                this.currentDeckEntityIds.add(ent.getId());
                deckEntityCount++;

                // Physical deck load under player weight (smooth continuous dynamic tilt across entire vehicle surface):
                double sens = this.physics.weightDipSensitivity;
                float dipY = (float) ((isInsideCargoBed ? 0.032 : 0.024) * sens);
                totalDeckStaticDipY -= dipY;
                totalDeckStaticPitch += (float) (localFwd * -0.018 * sens);
                totalDeckStaticRoll += (float) (localLat * 0.020 * sens);
                if (isInsideCargoBed && this.physics != null && this.physics.suspensionCompressions != null && this.physics.suspensionCompressions.length >= 4) {
                    this.physics.suspensionCompressions[2] = (float) Math.min(0.12, this.physics.suspensionCompressions[2] + 0.035 * sens);
                    this.physics.suspensionCompressions[3] = (float) Math.min(0.12, this.physics.suspensionCompressions[3] + 0.035 * sens);
                }

                // Jump Takeoff: Crisp spring creak when launching into a jump from the deck!
                if (ent instanceof Player player && isJumpingTakeoff && wasAlreadyOnDeck && !wasAirborne) {
                    float jumpCreakPitch = 1.02F + (float) (Math.random() * 0.14);
                    applyChassisImpulse(0.036F, (float) (localFwd * 2.8F), (float) (localLat * -2.4F));
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(),
                            ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.PLAYERS, 0.95F, jumpCreakPitch);
                    }
                }

                // SOLID DECK CATCH & LANDING:
                // If entity is descending or level with deck, catch and hold firmly on solid deck!
                boolean isDescendingOrLevel = (curMotion.y <= 0.05 && deltaY <= 0.05);
                boolean inVerticalCatchRange = wasAlreadyOnDeck
                    ? (ent.getY() <= targetWorldY + 0.45 && ent.getY() >= targetWorldY - 0.85)
                    : (ent.getY() <= targetWorldY + 0.25 && ent.getY() >= targetWorldY - 0.55);

                boolean shouldLandNow = wasAirborne && isDescendingOrLevel && (ent.getY() <= targetWorldY + 0.15 && ent.getY() >= targetWorldY - 0.50);
                if (shouldLandNow) {
                    wasAirborne = false;
                    isAirborneAbove = false;
                    this.airborneDeckEntityIds.remove(ent.getId());
                    double apexY = this.deckMaxAirborneY.getOrDefault(ent.getId(), Math.max(ent.getY(), entryPrevY));
                    this.deckMaxAirborneY.remove(ent.getId());
                    long lastLand = this.lastDeckLandTicks.getOrDefault(ent.getId(), 0L);
                    boolean landCooldownPassed = (this.tickCount - lastLand) >= 4;

                    float fallD = (float) Math.max(entryFallDist, Math.max(apexY - targetWorldY, entryPrevY - targetWorldY));
                    if (landCooldownPassed) {
                        this.lastDeckLandTicks.put(ent.getId(), (long) this.tickCount);
                        net.minecraft.sounds.SoundEvent landSound;
                        if (localFwd > windshieldStart) {
                            landSound = ModSounds.CAR_STEP_HOOD.get();
                        } else if (localFwd > rearWindowStart) {
                            landSound = ModSounds.CAR_STEP_ROOF.get();
                        } else {
                            landSound = ModSounds.CAR_STEP_TRUNK.get();
                        }

                        float landVol = Math.min(1.25F, 0.85F + fallD * 0.15F);
                        float landPitch = (float) Mth.clamp(0.72F - fallD * 0.04F + (Math.random() * 0.06), 0.58F, 0.82F);
                        float creakVol = (float) Mth.clamp(0.70F + fallD * 0.18F, 0.60F, 1.25F);
                        float creakPitch = 0.92F + (float) (Math.random() * 0.15);

                        float jumpDip = (float) Math.min(0.075, (0.025 + Math.min(2.5, fallD) * 0.022) * this.physics.weightDipSensitivity);
                        float fwdP = (float) (localFwd * -2.2F * (1.0F + Math.min(2.0F, fallD) * 0.25F));
                        float latR = (float) (localLat * 2.0F * (1.0F + Math.min(2.0F, fallD) * 0.25F));
                        applyChassisImpulse(-jumpDip, fwdP, latR);

                        if (!this.level().isClientSide) {
                            this.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(),
                                landSound, SoundSource.PLAYERS, landVol, landPitch);
                            this.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(),
                                ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.PLAYERS, creakVol, creakPitch);
                        }
                    }
                }

                if (!wasAirborne && !isJumpingTakeoff && inVerticalCatchRange) {

                    double maxBedLat = halfWidth - 0.12;
                    boolean isJumpingUp = (curMotion.y > 0.12 || localY > bedFloorHeight + 0.45);
                    boolean isClimbingToRoof = hasCargoBed && (localFwd > -0.70) && (localY > bedFloorHeight + 0.40 || (isJumpingUp && localFwd > -0.65));

                    if (isInsideCargoBed && !isClimbingToRoof) {
                        // PICKUP BED CARRIER & RETENTION:
                        // Holds the player securely inside the open cargo bed cavity!
                        // Player can stand without pressing any keys, smoothly carried and turned with the vehicle.
                        boolean isNearTailgate = (localFwd < -rearDist + 0.55);
                        boolean isNearSideRail = (Math.abs(localLat) > maxBedLat - 0.25);
                        boolean isPassingTailgate = isNearTailgate && (isMovingOnDeck || isJumpingUp);
                        boolean isPassingSideRail = isNearSideRail && (isMovingOnDeck || isJumpingUp);
                        // Also allow exit if player is jumping AND actively walking in any direction near bed boundary
                        boolean isJumpExiting = isJumpingUp && isMovingOnDeck && (isNearTailgate || isNearSideRail);

                        if (isJumpingUp && (isPassingTailgate || isPassingSideRail || isJumpExiting)) {
                            // Player is hopping out of the bed! Move with car horizontally, allow free exit and vertical falling
                            double carSpeed = Math.abs(this.physics.speed);
                            Vec3 carMotion = this.getDeltaMovement();
                            if (carSpeed > 0.05 && carMotion.lengthSqr() > 0.0001) {
                                double dYawRad = Math.toRadians(this.physics.deltaYaw);
                                double sinDY = Math.sin(dYawRad);
                                double cosDY = Math.cos(dYawRad);
                                double relX = ent.getX() - this.getX();
                                double relZ = ent.getZ() - this.getZ();
                                double rotRelX = relX * cosDY - relZ * sinDY;
                                double rotRelZ = relX * sinDY + relZ * cosDY;
                                ent.setPos(this.getX() + rotRelX, ent.getY(), this.getZ() + rotRelZ);
                                if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                    ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                    ent.yRotO += (float) this.physics.deltaYaw;
                                }
                            }
                            ent.setDeltaMovement(curMotion.x, curMotion.y, curMotion.z);
                        } else {
                            // Comfortably and safely held inside the cargo bed while walking or standing
                            double bedMaxFwd = (isNearSpareWheel && localY < 1.10) ? -0.55 : (bedFrontZ - 0.05);
                            double clampedLocalFwd = Mth.clamp(localFwd, -rearDist + 0.35, bedMaxFwd);
                            double clampedLocalLat = Mth.clamp(localLat, -maxBedLat, maxBedLat);

                            double bedTiltedY = bedFloorHeight + cDipY + clampedLocalFwd * Math.sin(this.currentPitch + cDipP) - clampedLocalLat * Math.sin(this.currentRoll + cDipR);
                            double targetBedWorldX = this.getX() + clampedLocalFwd * fwdX + clampedLocalLat * rightX;
                            double targetBedWorldY = this.getY() + bedTiltedY;
                            double targetBedWorldZ = this.getZ() + clampedLocalFwd * fwdZ + clampedLocalLat * rightZ;

                            ent.setPos(targetBedWorldX, targetBedWorldY, targetBedWorldZ);
                            ent.setOnGround(true);
                            ent.fallDistance = 0.0F;
                            ent.setDeltaMovement(0.0, Math.max(0.0, curMotion.y), 0.0);
                            if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                ent.yRotO += (float) this.physics.deltaYaw;
                            }
                        }
                        // NOTE: deltaYaw is already applied in each branch above; no duplicate application here!

                        if (!isMovingOnDeck) {
                            if (ent instanceof LivingEntity living) {
                                living.walkAnimation.setSpeed(0.0F);
                                living.walkAnimation.update(0.0F, 1.0F);
                            }
                            if (ent instanceof Player player) {
                                player.walkDist = player.walkDistO;
                                player.bob = player.oBob;
                            }
                        }
                    } else {
                        // Standard roof / hood / trunk surface
                        double carSpeed = Math.abs(this.physics.speed);
                        Vec3 carMotion = this.getDeltaMovement();
                        if (carSpeed > 0.05 || Math.abs(this.physics.deltaYaw) > 0.001) {
                            double dYawRad = Math.toRadians(this.physics.deltaYaw);
                            double sinDY = Math.sin(dYawRad);
                            double cosDY = Math.cos(dYawRad);
                            double relX = ent.getX() - this.getX();
                            double relZ = ent.getZ() - this.getZ();
                            double rotRelX = relX * cosDY - relZ * sinDY;
                            double rotRelZ = relX * sinDY + relZ * cosDY;
                            if (ent.getY() <= targetWorldY + 0.06) {
                                ent.setPos(this.getX() + rotRelX, targetWorldY, this.getZ() + rotRelZ);
                                if (curMotion.y < 0.0) {
                                    ent.setDeltaMovement(curMotion.x, 0.0, curMotion.z);
                                }
                                ent.setOnGround(true);
                                ent.fallDistance = 0.0F;
                            } else {
                                ent.setPos(this.getX() + rotRelX, ent.getY(), this.getZ() + rotRelZ);
                            }
                            if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                ent.yRotO += (float) this.physics.deltaYaw;
                            }
                        } else {
                            // Stationary car: solid support platform without suction or jump cancellation!
                            if (ent.getY() <= targetWorldY + 0.06) {
                                ent.setPos(ent.getX(), targetWorldY, ent.getZ());
                                if (curMotion.y < 0.0) {
                                    ent.setDeltaMovement(curMotion.x, 0.0, curMotion.z);
                                }
                                ent.setOnGround(true);
                                ent.fallDistance = 0.0F;
                            }
                        }

                        if (!isMovingOnDeck) {
                            if (ent instanceof LivingEntity living) {
                                living.walkAnimation.setSpeed(0.0F);
                                living.walkAnimation.update(0.0F, 1.0F);
                            }
                            if (ent instanceof Player player) {
                                player.walkDist = player.walkDistO;
                                player.bob = player.oBob;
                            }
                        }
                    }
                } else if (isAirborneAbove || wasAirborne) {
                    // GALILEAN INERTIAL CARRIER (Train effect mid-air jumping):
                    // When jumping or airborne above the moving vehicle, translate X, Z and yaw synchronously
                    // with the vehicle so the entity does NOT fall behind or get smeared!
                    double carSpeed = Math.abs(this.physics.speed);
                    Vec3 carMotion = this.getDeltaMovement();

                    if (isInsideCargoBed) {
                        double maxBedLat = halfWidth - 0.12;
                        boolean isOutsideBedBounds = (localFwd < -rearDist + 0.30) || (localFwd > cabinRearZ - 0.10)
                            || (Math.abs(localLat) > maxBedLat + 0.10);

                        if (isOutsideBedBounds) {
                            // Player has jumped past bed boundary — let them exit freely (Galilean carry without clamping)
                            if (carSpeed > 0.05 && carMotion.lengthSqr() > 0.0001) {
                                double dYawRad = Math.toRadians(this.physics.deltaYaw);
                                double sinDY = Math.sin(dYawRad);
                                double cosDY = Math.cos(dYawRad);
                                double relX = ent.getX() - this.getX();
                                double relZ = ent.getZ() - this.getZ();
                                double rotRelX = relX * cosDY - relZ * sinDY;
                                double rotRelZ = relX * sinDY + relZ * cosDY;
                                ent.setPos(this.getX() + rotRelX, ent.getY(), this.getZ() + rotRelZ);
                                if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                    ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                    ent.yRotO += (float) this.physics.deltaYaw;
                                }
                            }
                            ent.setDeltaMovement(carMotion.x, curMotion.y, carMotion.z);
                        } else {
                            if (carSpeed > 0.05 && carMotion.lengthSqr() > 0.0001) {
                                double clampedLocalFwd = Mth.clamp(localFwd, -rearDist + 0.35, cabinRearZ - 0.10);
                                double clampedLocalLat = Mth.clamp(localLat, -maxBedLat, maxBedLat);

                                double targetAirX = this.getX() + clampedLocalFwd * fwdX + clampedLocalLat * rightX;
                                double targetAirZ = this.getZ() + clampedLocalFwd * fwdZ + clampedLocalLat * rightZ;

                                ent.setPos(targetAirX, ent.getY(), targetAirZ);
                                ent.fallDistance = 0.0F;
                                ent.setDeltaMovement(carMotion.x, curMotion.y, carMotion.z);

                                if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                    ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                    ent.yRotO += (float) this.physics.deltaYaw;
                                }
                            }
                        }
                    } else {
                        // Standard roof / hood / trunk jump
                        if (carSpeed > 0.05 && carMotion.lengthSqr() > 0.0001) {
                            double dYawRad = Math.toRadians(this.physics.deltaYaw);
                            double sinDY = Math.sin(dYawRad);
                            double cosDY = Math.cos(dYawRad);
                            double relX = ent.getX() - this.getX();
                            double relZ = ent.getZ() - this.getZ();
                            double rotRelX = relX * cosDY - relZ * sinDY;
                            double rotRelZ = relX * sinDY + relZ * cosDY;
                            ent.setPos(this.getX() + rotRelX, ent.getY(), this.getZ() + rotRelZ);

                            if (Math.abs(this.physics.deltaYaw) > 0.001) {
                                ent.setYRot((float) (ent.getYRot() + this.physics.deltaYaw));
                                ent.yRotO += (float) this.physics.deltaYaw;
                            }
                            ent.fallDistance = 0.0F;
                            ent.setDeltaMovement(carMotion.x, curMotion.y, carMotion.z);
                        }
                    }
                }

                // Play authentic metal footsteps sound when player walks/runs on deck (Cadence tuned: 11 walk / 7 sprint / 16 crouch)
                if (ent instanceof Player player && Math.abs(ent.getY() - targetWorldY) <= 0.45) {
                    if (isMovingOnDeck) {
                        int stepInterval = player.isSprinting() ? 7 : (player.isCrouching() ? 16 : 11);
                        long stepCount = this.deckStepCounters.compute(ent.getId(), (k, v) -> (v == null ? 0L : v + 1L));
                        if (stepCount % stepInterval == 0) {
                            net.minecraft.sounds.SoundEvent stepSound;
                            if (localFwd > windshieldStart) {
                                stepSound = ModSounds.CAR_STEP_HOOD.get();
                            } else if (localFwd > rearWindowStart) {
                                stepSound = ModSounds.CAR_STEP_ROOF.get();
                            } else {
                                stepSound = ModSounds.CAR_STEP_TRUNK.get();
                            }
                            float vol = player.isSprinting() ? 0.85F : (player.isCrouching() ? 0.40F : 0.65F);
                            float pitch = 0.95F + (float) (Math.random() * 0.10);

                            if (!this.level().isClientSide) {
                                this.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(),
                                    stepSound, SoundSource.PLAYERS, vol, pitch);

                                // Subtle suspension creak under shifting human weight on the chassis
                                if (this.random.nextFloat() < 0.40F) {
                                    this.level().playSound(null, ent.getX(), ent.getY(), ent.getZ(),
                                        ModSounds.CAR_SUSPENSION_CREAK.get(), SoundSource.PLAYERS, 0.58F, 0.98F + this.random.nextFloat() * 0.14F);
                                }
                            }
                        }
                    }
                }

                this.prevDeckLocalPos.put(ent.getId(), new Vec3(localFwd, localY, localLat));
                continue;
            }

            // 2. SOLID PERIMETER WALL COLLISION (Pushing entity away from car doors/sides)
            // For mobs: push away at all heights below totalHeight so they can NEVER enter cabin or mount car!
            // For players: only push if firmly on the ground below bumper level. Never push players during jumps or on deck!
            boolean isGroundObstacle = (ent instanceof net.minecraft.world.entity.Mob)
                ? (localY < roofHeight + 0.35)
                : (!wasAlreadyOnDeck && !isInsideCargoBed && !inBedZone && !canMountDeck && (localY < bumperStepHeight - 0.10));

            if (isGroundObstacle) {
                double targetDistFwd = (localFwd > 0 ? frontDist : rearDist) + entRadius + mobExtraMargin;
                double targetDistLat = currentBodyHalfW + entRadius + mobExtraMargin;

                double overlapFwd = targetDistFwd - Math.abs(localFwd);
                double overlapLat = targetDistLat - Math.abs(localLat);

                if (overlapFwd > 0.0 && overlapLat > 0.0) {
                    double pushLocalFwd = 0.0;
                    double pushLocalLat = 0.0;

                    if (overlapFwd < overlapLat) {
                        pushLocalFwd = Math.signum(localFwd) * overlapFwd;
                    } else {
                        pushLocalLat = Math.signum(localLat) * overlapLat;
                    }

                    double pushWorldX = pushLocalFwd * fwdX + pushLocalLat * rightX;
                    double pushWorldZ = pushLocalFwd * fwdZ + pushLocalLat * rightZ;

                    if (ent instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setPos(mob.getX() + pushWorldX, mob.getY(), mob.getZ() + pushWorldZ);
                        Vec3 m = mob.getDeltaMovement();
                        double mDotPush = m.x * pushWorldX + m.z * pushWorldZ;
                        if (mDotPush < 0.0) {
                            double pLenSq = pushWorldX * pushWorldX + pushWorldZ * pushWorldZ;
                            if (pLenSq > 0.0001) {
                                double factor = mDotPush / pLenSq;
                                mob.setDeltaMovement(m.x - pushWorldX * factor, m.y, m.z - pushWorldZ * factor);
                            }
                        }

                        // Project Zomboid Mob Car Attack logic:
                        if (!this.level().isClientSide && mob.getTarget() != null) {
                            handleMobAttackingVehicle(mob);
                        }
                    } else {
                        ent.move(MoverType.SELF, new Vec3(pushWorldX, 0.0, pushWorldZ));
                    }

                    if (ent instanceof Player player && player.isShiftKeyDown() && !canMountDeck && localY < deckHeight + 0.50) {
                        this.hasImpulse = true;
                    }

                    // Moving car gentle nudge / push
                    double curSpeed = this.physics.speed;
                    if (Math.abs(curSpeed) > 0.3) {
                        double speedPush = Math.signum(curSpeed) * 0.08;
                        ent.push(fwdX * speedPush, 0.0, fwdZ * speedPush);
                    }
                }
            }
        }



        // Smoothly interpolate static passenger deck load into physics spring equilibrium (Server-authoritative only)
        if (!this.level().isClientSide) {
            if (deckEntityCount > 0) {
                float avgPitch = totalDeckStaticPitch / (float) deckEntityCount;
                float avgRoll = totalDeckStaticRoll / (float) deckEntityCount;
                float clampedDipY = Math.max(-0.095F, totalDeckStaticDipY);
                this.physics.extraStaticDipY += (clampedDipY - this.physics.extraStaticDipY) * 0.35;
                this.physics.extraStaticPitch += (avgPitch - this.physics.extraStaticPitch) * 0.35;
                this.physics.extraStaticRoll += (avgRoll - this.physics.extraStaticRoll) * 0.35;
            } else {
                this.physics.extraStaticDipY *= 0.70;
                this.physics.extraStaticPitch *= 0.70;
                this.physics.extraStaticRoll *= 0.70;
                if (Math.abs(this.physics.extraStaticDipY) < 0.001) this.physics.extraStaticDipY = 0.0;
                if (Math.abs(this.physics.extraStaticPitch) < 0.001) this.physics.extraStaticPitch = 0.0;
                if (Math.abs(this.physics.extraStaticRoll) < 0.001) this.physics.extraStaticRoll = 0.0;
            }
        }

        this.prevDeckEntityIds.clear();
        this.prevDeckEntityIds.addAll(this.currentDeckEntityIds);
        this.airborneDeckEntityIds.removeIf(id -> !this.currentDeckEntityIds.contains(id));
        this.prevDeckLocalPos.keySet().removeIf(id -> !this.currentDeckEntityIds.contains(id));
        this.deckStepCounters.keySet().removeIf(id -> !this.currentDeckEntityIds.contains(id));
        this.deckMaxAirborneY.keySet().removeIf(id -> !this.currentDeckEntityIds.contains(id));
    }

    public boolean isEntityOnDeck(Entity ent) {
        if (ent == null) return false;
        return this.currentDeckEntityIds.contains(ent.getId()) || this.airborneDeckEntityIds.contains(ent.getId());
    }

    public boolean isShooterOnVehicle(Entity shooter) {
        if (shooter == null) return false;
        if (this.hasPassenger(shooter) || shooter.getRootVehicle() == this || shooter.getVehicle() == this) {
            return true;
        }
        if (this.isEntityOnDeck(shooter)) {
            return true;
        }

        // Comprehensive 3D bounding envelope check covering the ENTIRE vehicle footprint + margins
        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean hasBed = hasCargoBed();

        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (hasBed ? 2.90 : 3.25);
        double rearDist  = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (hasBed ? 2.90 : 3.25);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (hasBed ? 1.15 : 1.25);
        double roofHeight = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (hasBed ? 2.48 : 2.45);

        double dy = shooter.getY() - this.getY();
        // Check vertical range: from below vehicle (-0.8m) to above roof (+4.0m)
        if (dy >= -0.8 && dy <= roofHeight + 3.0) {
            float yawRad = (float) Math.toRadians(this.getYRot());
            double fwdX = -Math.sin(yawRad);
            double fwdZ = Math.cos(yawRad);
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            double dx = shooter.getX() - this.getX();
            double dz = shooter.getZ() - this.getZ();

            double localFwd = dx * fwdX + dz * fwdZ;
            double localLat = dx * rightX + dz * rightZ;

            // Generous margin (+1.2m front/rear, +0.8m sides) to include anyone on hood, bed, roof, or bumpers
            if (localFwd >= -rearDist - 1.2 && localFwd <= frontDist + 1.2 && Math.abs(localLat) <= halfWidth + 0.8) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isPassengerOfSameVehicle(Entity other) {
        if (other == null) return false;
        if (this.hasPassenger(other) || this.isShooterOnVehicle(other)) return true;
        if (this.subEntities != null) {
            for (VehiclePartEntity part : this.subEntities) {
                if (part == other) return true;
            }
        }
        return super.isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean is(Entity entity) {
        if (this == entity) return true;
        if (this.subEntities != null) {
            for (VehiclePartEntity part : this.subEntities) {
                if (part == entity) return true;
            }
        }
        return super.is(entity);
    }

    public static DrivableVehicleEntity getVehicleCarrying(Entity ent) {
        if (ent == null || ent.level() == null) return null;
        net.minecraft.world.phys.AABB box = ent.getBoundingBox().inflate(2.5, 4.5, 2.5);
        java.util.List<DrivableVehicleEntity> vehicles = ent.level().getEntitiesOfClass(DrivableVehicleEntity.class, box);
        for (DrivableVehicleEntity v : vehicles) {
            if (v.isEntityOnDeck(ent)) {
                return v;
            }
        }
        return null;
    }



    public float getWheelRotation(int wheelIndex, float partialTicks) {
        if (wheelIndex >= 0 && wheelIndex < 4) {
            return Mth.lerp(partialTicks, prevWheelRotations[wheelIndex], currentWheelRotations[wheelIndex]);
        }
        return getWheelRotation(partialTicks);
    }

    public float getWheelRotation(float partialTicks) {
        return Mth.lerp(partialTicks, prevWheelRotation, currentWheelRotation);
    }



    private void tickLerp() {
        if (this.lerpSteps > 0) {
            double d0 = this.getX() + (this.lerpX - this.getX()) / (double) this.lerpSteps;
            double d1 = this.getY() + (this.lerpY - this.getY()) / (double) this.lerpSteps;
            double d2 = this.getZ() + (this.lerpZ - this.getZ()) / (double) this.lerpSteps;
            --this.lerpSteps;
            this.setPos(d0, d1, d2);
        } else {
            this.reapplyPosition();
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (teleport) {
            this.setPos(x, y, z);
            this.setYRot(yRot);
            this.lerpSteps = 0;
            return;
        }
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpSteps = Math.min(3, Math.max(2, steps));
    }

    public boolean isMultipartEntity() {
        return true;
    }

    public VehiclePartEntity[] getParts() {
        return this.subEntities;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.subEntities != null) {
            for (int i = 0; i < this.subEntities.length; i++) {
                this.subEntities[i].setId(id + i + 1);
            }
        }
    }

    public String getVehicleId() {
        return "blue_sedan";
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        this.setBoundingBox(this.makeBoundingBox());
    }

    public void updatePartPositions() {
        if (this.subEntities == null || this.subEntities.length == 0) return;
        this.setBoundingBox(this.makeBoundingBox());

        VehicleDefinition def = VehiclePackManager.get(this.getVehicleId());
        VehicleDefinition.CalibratedBounds cb = (def != null) ? def.calibratedBounds : null;
        boolean hasCargoBed = hasCargoBed();
        boolean hasBed = hasCargoBed;

        float r = 0.42F;
        float halfTireWidth = 0.16F;
        if (def != null && def.wheels != null) {
            r = (float) def.wheels.radius;
            float wheelDia = Math.max(0.65F, r * 2.0F);

            this.partWheelFL.localX = def.wheels.frontLeft[0];
            this.partWheelFL.localY = def.wheels.frontLeft[1];
            this.partWheelFL.localZ = def.wheels.frontLeft[2];
            this.partWheelFL.setDimensions(halfTireWidth * 2.0F, wheelDia);

            this.partWheelFR.localX = def.wheels.frontRight[0];
            this.partWheelFR.localY = def.wheels.frontRight[1];
            this.partWheelFR.localZ = def.wheels.frontRight[2];
            this.partWheelFR.setDimensions(halfTireWidth * 2.0F, wheelDia);

            this.partWheelRL.localX = def.wheels.rearLeft[0];
            this.partWheelRL.localY = def.wheels.rearLeft[1];
            this.partWheelRL.localZ = def.wheels.rearLeft[2];
            this.partWheelRL.setDimensions(halfTireWidth * 2.0F, wheelDia);

            this.partWheelRR.localX = def.wheels.rearRight[0];
            this.partWheelRR.localY = def.wheels.rearRight[1];
            this.partWheelRR.localZ = def.wheels.rearRight[2];
            this.partWheelRR.setDimensions(halfTireWidth * 2.0F, wheelDia);
        }

        // Exact calibrated vehicle geometry matching processProfessionalHullCollisions
        double frontDist = (cb != null && cb.frontDist > 0.5) ? cb.frontDist : (hasBed ? 2.90 : 3.25);
        double rearDist  = (cb != null && cb.rearDist > 0.5) ? cb.rearDist : (hasBed ? 2.90 : 3.25);
        double halfWidth = (cb != null && cb.halfWidth > 0.5) ? cb.halfWidth : (hasBed ? 1.15 : 1.25);

        double cabinFrontZ = (cb != null && Math.abs(cb.cabinFrontZ) > 0.1) ? cb.cabinFrontZ : (hasBed ? 1.05 : 0.85);
        double cabinRearZ  = (cb != null && Math.abs(cb.cabinRearZ) > 0.1) ? cb.cabinRearZ : (hasBed ? -0.75 : -0.75);

        double hoodHeight  = (cb != null && cb.hoodHeight > 0.5) ? cb.hoodHeight : (hasBed ? 1.46 : 1.48);
        double roofHeight  = (cb != null && cb.roofHeight > 0.5) ? cb.roofHeight : (hasBed ? 2.48 : 2.45);
        double trunkHeight = (cb != null && cb.trunkHeight > 0.5) ? cb.trunkHeight : (hasBed ? 1.38 : 1.35);

        double effCabinRearZ = hasBed ? (cabinRearZ + 0.25) : cabinRearZ;

        // 1. Hood: from cabinFrontZ (+1.05) to frontDist (+2.90)
        double hoodLen = frontDist - cabinFrontZ;
        double hoodHalfL = hoodLen * 0.5;
        double hoodCenterFwd = cabinFrontZ + hoodHalfL;
        double hoodBaseY = 0.25;
        double hoodH = Math.max(0.60, hoodHeight - hoodBaseY);
        double hoodHalfW = halfWidth * 0.96;

        // 2. Cabin (Doors, Pillars, Windows, Cockpit): from cabinRearZ to cabinFrontZ (+1.05)
        // For pickups, cabin stays strictly forward so its rotated AABB never intrudes into cargo bed
        double cabinLen = cabinFrontZ - effCabinRearZ;
        double cabinHalfL = cabinLen * 0.5;
        double cabinCenterFwd = effCabinRearZ + cabinHalfL;
        double cabinBaseY = 0.25;
        double cabinH = Math.max(1.45, roofHeight - 0.12 - cabinBaseY);
        double cabinHalfW = hasCargoBed ? (halfWidth * 0.88) : (halfWidth * 1.02);

        // 3. Trunk: from -rearDist (-2.90) to cabinRearZ (-0.75)
        double trunkLen = rearDist + cabinRearZ;
        double trunkHalfL = trunkLen * 0.5;
        double trunkCenterFwd = -rearDist + trunkHalfL;
        double bedFloorH = (cb != null && cb.bedFloorHeight > 0.1) ? cb.bedFloorHeight : 0.78;
        double trunkBaseY = 0.20;
        // For cargo beds: only the floor slab (not full height walls) — air inside is completely clear for shooting!
        double trunkH = hasCargoBed ? Math.max(0.20, Math.min(0.32, bedFloorH - trunkBaseY)) : Math.max(0.60, trunkHeight - trunkBaseY);
        double trunkHalfW = halfWidth * 0.96;

        // 4. Chassis: central underbody frame under cabin
        double chassisLen = cabinLen;
        double chassisHalfL = chassisLen * 0.5;
        double chassisCenterFwd = cabinCenterFwd;
        double chassisBaseY = 0.05;
        double chassisH = 0.35;
        double chassisHalfW = halfWidth * 0.72;

        // 5. Roof: solid roof layer at the top of the cabin
        double roofLen = cabinLen;
        double roofHalfL = roofLen * 0.5;
        double roofCenterFwd = cabinCenterFwd;
        double roofBaseY = roofHeight - 0.32;
        double roofH = 0.26;
        double roofHalfW = hasCargoBed ? (halfWidth * 0.86) : (halfWidth * 0.92);

        this.partHood.setDimensions((float)(hoodHalfW * 2.0), (float)hoodH);
        this.partCabin.setDimensions((float)(cabinHalfW * 2.0), (float)cabinH);
        this.partTrunk.setDimensions((float)(trunkHalfW * 2.0), (float)trunkH);
        this.partChassis.setDimensions((float)(chassisHalfW * 2.0), (float)chassisH);
        this.partRoof.setDimensions((float)(roofHalfW * 2.0), (float)roofH);

        float yawRad = (float) Math.toRadians(this.getYRot());
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double fwdX = -sinY;
        double fwdZ = cosY;
        double rightX = cosY;
        double rightZ = sinY;

        double carX = this.getX();
        double carY = this.getY();
        double carZ = this.getZ();

        for (VehiclePartEntity part : this.subEntities) {
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.xOld = part.getX();
            part.yOld = part.getY();
            part.zOld = part.getZ();

            if (part == this.partWheelFL || part == this.partWheelFR || part == this.partWheelRL || part == this.partWheelRR) {
                int wIdx = (part == this.partWheelFL) ? getPhysicalWheelIndex(true, true)
                         : (part == this.partWheelFR) ? getPhysicalWheelIndex(true, false)
                         : (part == this.partWheelRL) ? getPhysicalWheelIndex(false, true)
                         : getPhysicalWheelIndex(false, false);
                Vec3 wp = getWheelWorldPos(wIdx);
                part.setPos(wp.x, wp.y, wp.z);

                // Exact rotated AABB: matches wheel orientation and front wheel steering angle!
                boolean isFrontWheel = (part == this.partWheelFL || part == this.partWheelFR);
                float steerDeg = isFrontWheel ? -(float) Math.toDegrees(this.currentSteer) : 0.0F;
                float wheelYawRad = (float) Math.toRadians(this.getYRot() + steerDeg);
                float cosW = (float) Math.abs(Math.cos(wheelYawRad));
                float sinW = (float) Math.abs(Math.sin(wheelYawRad));
                float effectiveTireW = Math.max(0.26F, halfTireWidth);
                float effectiveR = r * 1.12F;
                float halfEx = cosW * effectiveTireW + sinW * effectiveR;
                float halfEz = sinW * effectiveTireW + cosW * effectiveR;

                AABB wheelAabb = new AABB(wp.x - halfEx, wp.y - effectiveR, wp.z - halfEz, wp.x + halfEx, wp.y + effectiveR, wp.z + halfEz);
                part.setBoundingBox(wheelAabb);
            } else if (part == this.partSpareWheel) {
                if (supportsSpareWheel()) {
                    Vec3 spPos = getSpareWheelWorldPos();
                    part.setPos(spPos.x, spPos.y, spPos.z);
                    float sr = 0.44F;
                    part.setBoundingBox(new AABB(spPos.x - sr, spPos.y - sr, spPos.z - sr, spPos.x + sr, spPos.y + sr, spPos.z + sr));
                } else {
                    part.setPos(carX, carY - 500.0, carZ);
                    part.setBoundingBox(new AABB(carX, carY - 500.0, carZ, carX, carY - 500.0, carZ));
                }
            } else {
                double partCenterFwd, partBaseY, partHalfW, partHalfL, partH;
                if (part == this.partHood) {
                    partCenterFwd = hoodCenterFwd; partBaseY = hoodBaseY; partHalfW = hoodHalfW; partHalfL = hoodHalfL; partH = hoodH;
                } else if (part == this.partCabin) {
                    partCenterFwd = cabinCenterFwd; partBaseY = cabinBaseY; partHalfW = cabinHalfW; partHalfL = cabinHalfL; partH = cabinH;
                } else if (part == this.partTrunk) {
                    partCenterFwd = trunkCenterFwd; partBaseY = trunkBaseY; partHalfW = trunkHalfW; partHalfL = trunkHalfL; partH = trunkH;
                } else if (part == this.partRoof) {
                    partCenterFwd = roofCenterFwd; partBaseY = roofBaseY; partHalfW = roofHalfW; partHalfL = roofHalfL; partH = roofH;
                } else { // Chassis
                    partCenterFwd = chassisCenterFwd; partBaseY = chassisBaseY; partHalfW = chassisHalfW; partHalfL = chassisHalfL; partH = chassisH;
                }

                double worldX = carX + fwdX * partCenterFwd;
                double worldY = carY + partBaseY;
                double worldZ = carZ + fwdZ * partCenterFwd;
                part.setPos(worldX, worldY + partH * 0.5, worldZ);

                float absCos = (float) Math.abs(cosY);
                float absSin = (float) Math.abs(sinY);
                float bodyEx = (float)(absCos * partHalfW + absSin * partHalfL);
                float bodyEz = (float)(absSin * partHalfW + absCos * partHalfL);
                AABB bodyAabb = new AABB(worldX - bodyEx, worldY, worldZ - bodyEz, worldX + bodyEx, worldY + partH, worldZ + bodyEz);
                part.setBoundingBox(bodyAabb);
            }
        }
    }



    @Override

    protected void readAdditionalSaveData(CompoundTag tag) {

        this.physics.speed = tag.getDouble("Speed");

        if (tag.contains("EngineOn")) {

            this.entityData.set(ENGINE_ON, tag.getBoolean("EngineOn"));

        }

        if (tag.contains("Fuel")) {

            setFuel(tag.getFloat("Fuel"));

        } else {

            setFuel(MAX_FUEL);

        }

        if (tag.contains("Health")) {

            setVehicleHealth(tag.getFloat("Health"));

        } else {

            setVehicleHealth(MAX_HP);

        }

        if (tag.contains("TireStatus")) {
            byte t = tag.getByte("TireStatus");
            this.entityData.set(TIRE_STATUS, t);
            for (int i = 0; i < 4; i++) {
                if ((t & (1 << i)) != 0) {
                    this.physics.tireAir[i] = 0.0f;
                }
            }
        }

        if (tag.contains("InstalledWheels")) {
            this.entityData.set(INSTALLED_WHEELS, tag.getByte("InstalledWheels"));
        } else {
            this.entityData.set(INSTALLED_WHEELS, (byte) 15);
        }

        if (tag.contains("WheelHealthFL")) wheelHealth[0] = tag.getFloat("WheelHealthFL");
        if (tag.contains("WheelHealthFR")) wheelHealth[1] = tag.getFloat("WheelHealthFR");
        if (tag.contains("WheelHealthRL")) wheelHealth[2] = tag.getFloat("WheelHealthRL");
        if (tag.contains("WheelHealthRR")) wheelHealth[3] = tag.getFloat("WheelHealthRR");
        if (tag.contains("HasSpareWheel")) {
            this.entityData.set(HAS_SPARE_WHEEL, tag.getBoolean("HasSpareWheel"));
        } else {
            this.entityData.set(HAS_SPARE_WHEEL, true);
        }
        if (tag.contains("ExplosionStage")) {
            this.entityData.set(EXPLOSION_STAGE, tag.getInt("ExplosionStage"));
        }
        if (tag.contains("TrunkItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            this.trunkInventory.fromTag(tag.getList("TrunkItems", net.minecraft.nbt.Tag.TAG_COMPOUND));
        }
        if (tag.contains("HeadlightMode")) {
            setHeadlightMode(tag.getInt("HeadlightMode"));
        } else if (tag.contains("HeadlightsOn")) {
            setHeadlightMode(tag.getBoolean("HeadlightsOn") ? 1 : 0);
        }
        if (tag.contains("CabinLightOn")) {
            this.entityData.set(CABIN_LIGHT_ON, tag.getBoolean("CabinLightOn"));
        }
        for (int s = 1; s < 4; s++) {
            if (tag.contains("Seat_" + s + "_Items", net.minecraft.nbt.Tag.TAG_LIST) && this.seatInventories[s] != null) {
                net.minecraft.nbt.ListTag list = tag.getList("Seat_" + s + "_Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int j = 0; j < list.size(); j++) {
                    CompoundTag it = list.getCompound(j);
                    int slot = it.getByte("Slot") & 255;
                    if (slot < 4) {
                        this.seatInventories[s].setItem(slot, ItemStack.of(it));
                    }
                }
            }
        }

        if (tag.contains("VehicleLocked")) {
            setLocked(tag.getBoolean("VehicleLocked"));
        }
        if (tag.contains("TrunkLocked")) {
            setTrunkLocked(tag.getBoolean("TrunkLocked"));
        }
        if (tag.contains("ParkingBrake")) {
            setParkingBrake(tag.getBoolean("ParkingBrake"));
        }
        if (tag.contains("IgnitionKey")) {
            this.ignitionKey = ItemStack.of(tag.getCompound("IgnitionKey"));
            this.entityData.set(KEY_IN_IGNITION, !this.ignitionKey.isEmpty());
        } else if (tag.contains("KeyInIgnition")) {
            this.entityData.set(KEY_IN_IGNITION, tag.getBoolean("KeyInIgnition"));
        }
        if (tag.contains("InstalledEngine")) {
            setInstalledEngine(tag.getString("InstalledEngine"));
        } else {
            applyInstalledEnginePhysics();
        }
        if (tag.contains("InstalledWheelsData")) {
            this.entityData.set(INSTALLED_WHEELS_DATA, tag.getString("InstalledWheelsData"));
        }
        if (tag.contains("InstalledBrakesData")) {
            this.entityData.set(INSTALLED_BRAKES_DATA, tag.getString("InstalledBrakesData"));
        }
        if (tag.contains("InstalledSeatsData")) {
            this.entityData.set(INSTALLED_SEATS_DATA, tag.getString("InstalledSeatsData"));
        }
        if (tag.contains("BatteryCharge")) {
            setBatteryCharge(tag.getFloat("BatteryCharge"));
        } else {
            setBatteryCharge(100.0F);
        }
        if (tag.contains("EngineQuality")) {
            setEngineQuality(tag.getFloat("EngineQuality"));
        } else {
            setEngineQuality(35.0F + this.random.nextFloat() * 60.0F);
        }
        if (tag.contains("EngineCondition")) {
            setEngineCondition(tag.getFloat("EngineCondition"));
        } else {
            setEngineCondition(100.0F);
        }
        if (tag.contains("IsHotwired")) {
            setHotwired(tag.getBoolean("IsHotwired"));
        } else {
            setHotwired(false);
        }
        if (tag.contains("BumperHealth")) {
            setBumperHealth(tag.getFloat("BumperHealth"));
        } else {
            setBumperHealth(100.0F);
        }
        if (tag.contains("HoodHealth")) {
            setHoodHealth(tag.getFloat("HoodHealth"));
        } else {
            setHoodHealth(100.0F);
        }
        if (tag.contains("RadiatorHealth")) {
            setRadiatorHealth(tag.getFloat("RadiatorHealth"));
        } else {
            setRadiatorHealth(100.0F);
        }
        if (tag.contains("BaseLoudness")) {
            setBaseLoudness(tag.getFloat("BaseLoudness"));
        } else {
            setBaseLoudness(24.0F);
        }
        if (tag.contains("MufflerHealth")) {
            setMufflerHealth(tag.getFloat("MufflerHealth"));
        } else {
            setMufflerHealth(100.0F);
        }
        if (tag.contains("EngineOil")) {
            setEngineOil(tag.getFloat("EngineOil"));
        } else {
            setEngineOil(100.0F);
        }
        if (tag.contains("CoolantLevel")) {
            setCoolantLevel(tag.getFloat("CoolantLevel"));
        } else {
            setCoolantLevel(100.0F);
        }
        if (tag.contains("EngineTemperature")) {
            setEngineTemperature(tag.getFloat("EngineTemperature"));
        } else {
            setEngineTemperature(20.0F);
        }
        if (tag.contains("DirtLevel")) {
            setDirtLevel(tag.getFloat("DirtLevel"));
        } else {
            setDirtLevel(0.0F);
        }
        if (tag.contains("BloodLevel")) {
            setBloodLevel(tag.getFloat("BloodLevel"));
        } else {
            setBloodLevel(0.0F);
        }
        for (int i = 0; i < 4; i++) {
            if (tag.contains("TireCondition_" + i)) {
                this.tireCondition[i] = tag.getFloat("TireCondition_" + i);
            } else {
                this.tireCondition[i] = 100.0F;
            }
        }
        if (this.physics != null) {
            this.physics.tireCondition = this.tireCondition;
        }
        applyAllWheelsPhysics();
        syncTireConditionsToEntityData();
        this.keySpawnInitialized = true;
    }

    public Vec3 getExhaustWorldPos() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        com.evgen.cardecor.pack.VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(this.getVehicleId());
        float[] exh = (def != null && def.exhaust != null && def.exhaust.pos != null && def.exhaust.pos.length >= 3) ? def.exhaust.pos : new float[]{1.08f, 0.38f, -2.22f};

        double worldX = this.getX() + rightX * exh[0] + fwdX * exh[2];
        double worldY = this.getY() + exh[1];
        double worldZ = this.getZ() + rightZ * exh[0] + fwdZ * exh[2];

        return new Vec3(worldX, worldY, worldZ);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        clearDynamicLight();

        tag.putDouble("Speed", this.physics.speed);

        tag.putBoolean("EngineOn", isEngineOn());

        tag.putFloat("Fuel", getFuel());

        tag.putFloat("Health", getVehicleHealth());

        tag.putByte("TireStatus", getTireStatus());
        tag.putByte("InstalledWheels", this.entityData.get(INSTALLED_WHEELS));
        tag.putBoolean("HasSpareWheel", hasSpareWheel());
        tag.putFloat("WheelHealthFL", wheelHealth[0]);
        tag.putFloat("WheelHealthFR", wheelHealth[1]);
        tag.putFloat("WheelHealthRL", wheelHealth[2]);
        tag.putFloat("WheelHealthRR", wheelHealth[3]);
        tag.putInt("ExplosionStage", this.entityData.get(EXPLOSION_STAGE));
        tag.put("TrunkItems", this.trunkInventory.createTag());
        tag.putInt("HeadlightMode", getHeadlightMode());
        tag.putBoolean("HeadlightsOn", isHeadlightsOn());
        tag.putBoolean("CabinLightOn", isCabinLightOn());
        tag.putBoolean("VehicleLocked", isLocked());
        tag.putBoolean("TrunkLocked", isTrunkLocked());
        tag.putBoolean("KeyInIgnition", isKeyInIgnition());
        tag.putBoolean("ParkingBrake", isParkingBrakeOn());
        tag.putString("InstalledEngine", this.getInstalledEngine());
        tag.putString("InstalledWheelsData", this.entityData.get(INSTALLED_WHEELS_DATA));
        tag.putString("InstalledBrakesData", this.entityData.get(INSTALLED_BRAKES_DATA));
        tag.putString("InstalledSeatsData", this.entityData.get(INSTALLED_SEATS_DATA));
        tag.putFloat("BatteryCharge", getBatteryCharge());
        tag.putFloat("EngineQuality", getEngineQuality());
        tag.putFloat("EngineCondition", getEngineCondition());
        tag.putBoolean("IsHotwired", isHotwired());
        tag.putFloat("BumperHealth", getBumperHealth());
        tag.putFloat("HoodHealth", getHoodHealth());
        tag.putFloat("RadiatorHealth", getRadiatorHealth());
        tag.putFloat("BaseLoudness", getBaseLoudness());
        tag.putFloat("MufflerHealth", getMufflerHealth());
        tag.putFloat("EngineOil", getEngineOil());
        tag.putFloat("CoolantLevel", getCoolantLevel());
        tag.putFloat("EngineTemperature", getEngineTemperature());
        tag.putFloat("DirtLevel", getDirtLevel());
        tag.putFloat("BloodLevel", getBloodLevel());
        for (int i = 0; i < 4; i++) {
            tag.putFloat("TireCondition_" + i, this.tireCondition[i]);
        }
        if (!this.ignitionKey.isEmpty()) {
            tag.put("IgnitionKey", this.ignitionKey.save(new CompoundTag()));
        }

        for (int s = 1; s < 4; s++) {
            if (this.seatInventories[s] != null) {
                net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
                for (int i = 0; i < 4; i++) {
                    ItemStack st = this.seatInventories[s].getItem(i);
                    if (!st.isEmpty()) {
                        CompoundTag it = new CompoundTag();
                        it.putByte("Slot", (byte) i);
                        st.save(it);
                        list.add(it);
                    }
                }
                tag.put("Seat_" + s + "_Items", list);
            }
        }
    }

}

