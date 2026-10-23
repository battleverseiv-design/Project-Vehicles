package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CarDecorMod.MODID);

    public static final RegistryObject<SoundEvent> VINTAGE_ENGINE = SOUND_EVENTS.register("vintage_engine",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "vintage_engine"))
    );

    public static final RegistryObject<SoundEvent> VINTAGE_ENGINE_START = SOUND_EVENTS.register("vintage_engine_start",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "vintage_engine_start"))
    );


    public static final RegistryObject<SoundEvent> VINTAGE_ENGINE_CRANK_FAIL = SOUND_EVENTS.register("vintage_engine_crank_fail",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "vintage_engine_crank_fail"))
    );

    public static final RegistryObject<SoundEvent> TIRE_SCREECH = SOUND_EVENTS.register("tire_screech",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_screech"))
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_1 = SOUND_EVENTS.register("engine_loop_1",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_1"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_2 = SOUND_EVENTS.register("engine_loop_2",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_2"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_3 = SOUND_EVENTS.register("engine_loop_3",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_3"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_4 = SOUND_EVENTS.register("engine_loop_4",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_4"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_5 = SOUND_EVENTS.register("engine_loop_5",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_5"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_6 = SOUND_EVENTS.register("engine_loop_6",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_6"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_7 = SOUND_EVENTS.register("engine_loop_7",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_7"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_8 = SOUND_EVENTS.register("engine_loop_8",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_8"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> CAR_HORN = SOUND_EVENTS.register("car_horn",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_horn"), 150.0F)
    );

    public static final RegistryObject<SoundEvent> CAR_CRASH_HEAVY = SOUND_EVENTS.register("car_crash_heavy",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_crash_heavy"))
    );

    public static final RegistryObject<SoundEvent> CAR_LANDING_HEAVY =
        SOUND_EVENTS.register("car_landing_heavy",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_landing_heavy")));

    public static final RegistryObject<SoundEvent> CAR_LANDING_LIGHT =
        SOUND_EVENTS.register("car_landing_light",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_landing_light")));

    public static final RegistryObject<SoundEvent> CAR_IMPACT_LIGHT = SOUND_EVENTS.register("car_impact_light",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_impact_light"))
    );

    public static final RegistryObject<SoundEvent> CAR_SUSPENSION_CREAK = SOUND_EVENTS.register("car_suspension_creak",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_suspension_creak"))
    );

    public static final RegistryObject<SoundEvent> ENGINE_LOOP_9 = SOUND_EVENTS.register("engine_loop_9",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_loop_9"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> ENGINE_KICKDOWN = SOUND_EVENTS.register("engine_kickdown",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_kickdown"))
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_START = SOUND_EVENTS.register("blue_sedan_start",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_start"))
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_IDLE = SOUND_EVENTS.register("blue_sedan_idle",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_idle"), 80.0F)
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_LOW = SOUND_EVENTS.register("blue_sedan_low",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_low"), 90.0F)
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_MID = SOUND_EVENTS.register("blue_sedan_mid",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_mid"), 100.0F)
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_HIGH = SOUND_EVENTS.register("blue_sedan_high",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_high"), 110.0F)
    );

    public static final RegistryObject<SoundEvent> BLUE_SEDAN_MAX = SOUND_EVENTS.register("blue_sedan_max",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "blue_sedan_max"), 110.0F)
    );

    public static final RegistryObject<SoundEvent> TIRE_EXPLODE = SOUND_EVENTS.register("tire_explode",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_explode"))
    );

    public static final RegistryObject<SoundEvent> WHEEL_HUB_GRIND = SOUND_EVENTS.register("wheel_hub_grind",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "wheel_hub_grind"))
    );

    public static final RegistryObject<SoundEvent> TIRE_DEFLATE = SOUND_EVENTS.register("tire_deflate",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_deflate"))
    );

    public static final RegistryObject<SoundEvent> TIRE_ROLL_DIRT = SOUND_EVENTS.register("tire_roll_dirt",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_roll_dirt"), 45.0F)
    );

    public static final RegistryObject<SoundEvent> TIRE_ROLL_ROAD = SOUND_EVENTS.register("tire_roll_road",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_roll_road"), 45.0F)
    );

    public static final RegistryObject<SoundEvent> TIRE_ROLL_SNOW = SOUND_EVENTS.register("tire_roll_snow",
        () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(CarDecorMod.MODID, "tire_roll_snow"), 45.0F)
    );

    public static final RegistryObject<SoundEvent> CAR_EXPLODE = SOUND_EVENTS.register("car_explode",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_explode"))
    );

    public static final RegistryObject<SoundEvent> BRAKE_HARD = SOUND_EVENTS.register("brake_hard",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "brake_hard"))
    );

    public static final RegistryObject<SoundEvent> BRAKE_SLOW = SOUND_EVENTS.register("brake_slow",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "brake_slow"))
    );

    public static final RegistryObject<SoundEvent> GEAR_SHIFT = SOUND_EVENTS.register("gear_shift",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "gear_shift"))
    );

    public static final RegistryObject<SoundEvent> STARTER_I6 = SOUND_EVENTS.register("starter_i6",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "starter_i6"))
    );

    public static final RegistryObject<SoundEvent> STARTER_I4 = SOUND_EVENTS.register("starter_i4",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "starter_i4"))
    );

    public static final RegistryObject<SoundEvent> STARTER_V8 = SOUND_EVENTS.register("starter_v8",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "starter_v8"))
    );

    public static final RegistryObject<SoundEvent> PICKUP_ENGINE_0 = SOUND_EVENTS.register("pickup_engine_0",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pickup_engine_0"))
    );

    public static final RegistryObject<SoundEvent> PICKUP_ENGINE_1 = SOUND_EVENTS.register("pickup_engine_1",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pickup_engine_1"))
    );

    public static final RegistryObject<SoundEvent> PICKUP_ENGINE_2 = SOUND_EVENTS.register("pickup_engine_2",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pickup_engine_2"))
    );

    public static final RegistryObject<SoundEvent> PICKUP_ENGINE_3 = SOUND_EVENTS.register("pickup_engine_3",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pickup_engine_3"))
    );

    public static final RegistryObject<SoundEvent> PICKUP_ENGINE_4 = SOUND_EVENTS.register("pickup_engine_4",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pickup_engine_4"))
    );

    public static final RegistryObject<SoundEvent> ENGINE_STOP_I6 = SOUND_EVENTS.register("engine_stop_i6",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_stop_i6"))
    );

    public static final RegistryObject<SoundEvent> ENGINE_STOP_V8 = SOUND_EVENTS.register("engine_stop_v8",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "engine_stop_v8"))
    );

    public static final RegistryObject<SoundEvent> CAR_STEP_HOOD = SOUND_EVENTS.register("car_step_hood",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_step_hood"))
    );

    public static final RegistryObject<SoundEvent> CAR_STEP_ROOF = SOUND_EVENTS.register("car_step_roof",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_step_roof"))
    );

    public static final RegistryObject<SoundEvent> CAR_STEP_TRUNK = SOUND_EVENTS.register("car_step_trunk",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_step_trunk"))
    );

    public static final RegistryObject<SoundEvent> CAR_HIT_ENTITY = SOUND_EVENTS.register("car_hit_entity",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_hit_entity"))
    );

    public static final RegistryObject<SoundEvent> BULLET_METAL_HIT = SOUND_EVENTS.register("bullet_metal_hit",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "bullet_metal_hit"))
    );

    public static final RegistryObject<SoundEvent> CAR_ENTER = SOUND_EVENTS.register("car_enter",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_enter"))
    );

    public static final RegistryObject<SoundEvent> CAR_EXIT = SOUND_EVENTS.register("car_exit",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_exit"))
    );

    public static final RegistryObject<SoundEvent> TRUNK_OPEN_SEDAN = SOUND_EVENTS.register("trunk_open_sedan",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "trunk_open_sedan"))
    );

    public static final RegistryObject<SoundEvent> TRUNK_CLOSE_SEDAN = SOUND_EVENTS.register("trunk_close_sedan",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "trunk_close_sedan"))
    );

    public static final RegistryObject<SoundEvent> TRUNK_OPEN_PICKUP = SOUND_EVENTS.register("trunk_open_pickup",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "trunk_open_pickup"))
    );

    public static final RegistryObject<SoundEvent> TRUNK_CLOSE_PICKUP = SOUND_EVENTS.register("trunk_close_pickup",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "trunk_close_pickup"))
    );

    public static final RegistryObject<SoundEvent> HEADLIGHTS_ON = SOUND_EVENTS.register("headlights_on",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "headlights_on"))
    );

    public static final RegistryObject<SoundEvent> HEADLIGHTS_OFF = SOUND_EVENTS.register("headlights_off",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "headlights_off"))
    );

    public static final RegistryObject<SoundEvent> CAR_LOCK_PIN_DOWN = SOUND_EVENTS.register("car_lock_pin_down",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_lock_pin_down"))
    );

    public static final RegistryObject<SoundEvent> CAR_UNLOCK_PIN_UP = SOUND_EVENTS.register("car_unlock_pin_up",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_unlock_pin_up"))
    );

    public static final RegistryObject<SoundEvent> CAR_UNLOCK_KEY = SOUND_EVENTS.register("car_unlock_key",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_unlock_key"))
    );

    public static final RegistryObject<SoundEvent> CAR_DOOR_LOCKED_YANK = SOUND_EVENTS.register("car_door_locked_yank",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_door_locked_yank"))
    );

    public static final RegistryObject<SoundEvent> KEY_INSERT = SOUND_EVENTS.register("key_insert",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "key_insert"))
    );

    public static final RegistryObject<SoundEvent> KEY_REMOVE = SOUND_EVENTS.register("key_remove",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "key_remove"))
    );

    public static final RegistryObject<SoundEvent> BRAKE_PEDAL = SOUND_EVENTS.register("brake_pedal",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "brake_pedal"))
    );

    public static final RegistryObject<SoundEvent> CLUTCH_RELEASE = SOUND_EVENTS.register("clutch_release",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "clutch_release"))
    );

    public static final RegistryObject<SoundEvent> PEDAL_PRESS = SOUND_EVENTS.register("pedal_press",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pedal_press"))
    );

    public static final RegistryObject<SoundEvent> PEDAL_RELEASE = SOUND_EVENTS.register("pedal_release",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "pedal_release"))
    );

    public static final RegistryObject<SoundEvent> CAR_RAIN_CABIN = SOUND_EVENTS.register("car_rain_cabin",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_rain_cabin"))
    );

    public static final RegistryObject<SoundEvent> VEHICLE_ITEM_PUT = SOUND_EVENTS.register("vehicle_item_put",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "vehicle_item_put"))
    );

    public static final RegistryObject<SoundEvent> CAR_HEATER = SOUND_EVENTS.register("car_heater",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_heater"))
    );

    public static final RegistryObject<SoundEvent> CAR_HEATER_START = SOUND_EVENTS.register("car_heater_start",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_heater_start"))
    );

    public static final RegistryObject<SoundEvent> CAR_HEATER_STOP = SOUND_EVENTS.register("car_heater_stop",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CarDecorMod.MODID, "car_heater_stop"))
    );
}
