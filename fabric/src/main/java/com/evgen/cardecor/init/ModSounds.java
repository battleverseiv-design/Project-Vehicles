package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class ModSounds {
    private static Supplier<SoundEvent> reg(String name) {
        ResourceLocation id = new ResourceLocation(CarDecorMod.MODID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        return () -> event;
    }

    private static Supplier<SoundEvent> regFixed(String name, float range) {
        ResourceLocation id = new ResourceLocation(CarDecorMod.MODID, name);
        SoundEvent event = SoundEvent.createFixedRangeEvent(id, range);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        return () -> event;
    }

    public static final Supplier<SoundEvent> VINTAGE_ENGINE = reg("vintage_engine");
    public static final Supplier<SoundEvent> VINTAGE_ENGINE_START = reg("vintage_engine_start");
    public static final Supplier<SoundEvent> VINTAGE_ENGINE_CRANK_FAIL = reg("vintage_engine_crank_fail");
    public static final Supplier<SoundEvent> TIRE_SCREECH = reg("tire_screech");

    public static final Supplier<SoundEvent> ENGINE_LOOP_1 = regFixed("engine_loop_1", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_2 = regFixed("engine_loop_2", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_3 = regFixed("engine_loop_3", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_4 = regFixed("engine_loop_4", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_5 = regFixed("engine_loop_5", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_6 = regFixed("engine_loop_6", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_7 = regFixed("engine_loop_7", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_LOOP_8 = regFixed("engine_loop_8", 100.0F);

    public static final Supplier<SoundEvent> CAR_HORN = regFixed("car_horn", 150.0F);
    public static final Supplier<SoundEvent> CAR_CRASH_HEAVY = reg("car_crash_heavy");
    public static final Supplier<SoundEvent> CAR_LANDING_HEAVY = reg("car_landing_heavy");
    public static final Supplier<SoundEvent> CAR_LANDING_LIGHT = reg("car_landing_light");
    public static final Supplier<SoundEvent> CAR_IMPACT_LIGHT = reg("car_impact_light");
    public static final Supplier<SoundEvent> CAR_SUSPENSION_CREAK = reg("car_suspension_creak");

    public static final Supplier<SoundEvent> ENGINE_LOOP_9 = regFixed("engine_loop_9", 100.0F);
    public static final Supplier<SoundEvent> ENGINE_KICKDOWN = reg("engine_kickdown");

    public static final Supplier<SoundEvent> BLUE_SEDAN_START = reg("blue_sedan_start");
    public static final Supplier<SoundEvent> BLUE_SEDAN_IDLE = regFixed("blue_sedan_idle", 80.0F);
    public static final Supplier<SoundEvent> BLUE_SEDAN_LOW = regFixed("blue_sedan_low", 90.0F);
    public static final Supplier<SoundEvent> BLUE_SEDAN_MID = regFixed("blue_sedan_mid", 100.0F);
    public static final Supplier<SoundEvent> BLUE_SEDAN_HIGH = regFixed("blue_sedan_high", 110.0F);
    public static final Supplier<SoundEvent> BLUE_SEDAN_MAX = regFixed("blue_sedan_max", 110.0F);

    public static final Supplier<SoundEvent> TIRE_EXPLODE = reg("tire_explode");
    public static final Supplier<SoundEvent> WHEEL_HUB_GRIND = reg("wheel_hub_grind");
    public static final Supplier<SoundEvent> TIRE_DEFLATE = reg("tire_deflate");

    public static final Supplier<SoundEvent> TIRE_ROLL_DIRT = regFixed("tire_roll_dirt", 45.0F);
    public static final Supplier<SoundEvent> TIRE_ROLL_ROAD = regFixed("tire_roll_road", 45.0F);
    public static final Supplier<SoundEvent> TIRE_ROLL_SNOW = regFixed("tire_roll_snow", 45.0F);

    public static final Supplier<SoundEvent> CAR_EXPLODE = reg("car_explode");
    public static final Supplier<SoundEvent> BRAKE_HARD = reg("brake_hard");
    public static final Supplier<SoundEvent> BRAKE_SLOW = reg("brake_slow");
    public static final Supplier<SoundEvent> GEAR_SHIFT = reg("gear_shift");

    public static final Supplier<SoundEvent> STARTER_I6 = reg("starter_i6");
    public static final Supplier<SoundEvent> STARTER_I4 = reg("starter_i4");
    public static final Supplier<SoundEvent> STARTER_V8 = reg("starter_v8");

    public static final Supplier<SoundEvent> PICKUP_ENGINE_0 = reg("pickup_engine_0");
    public static final Supplier<SoundEvent> PICKUP_ENGINE_1 = reg("pickup_engine_1");
    public static final Supplier<SoundEvent> PICKUP_ENGINE_2 = reg("pickup_engine_2");
    public static final Supplier<SoundEvent> PICKUP_ENGINE_3 = reg("pickup_engine_3");
    public static final Supplier<SoundEvent> PICKUP_ENGINE_4 = reg("pickup_engine_4");

    public static final Supplier<SoundEvent> ENGINE_STOP_I6 = reg("engine_stop_i6");
    public static final Supplier<SoundEvent> ENGINE_STOP_V8 = reg("engine_stop_v8");

    public static final Supplier<SoundEvent> CAR_STEP_HOOD = reg("car_step_hood");
    public static final Supplier<SoundEvent> CAR_STEP_ROOF = reg("car_step_roof");
    public static final Supplier<SoundEvent> CAR_STEP_TRUNK = reg("car_step_trunk");
    public static final Supplier<SoundEvent> CAR_HIT_ENTITY = reg("car_hit_entity");
    public static final Supplier<SoundEvent> BULLET_METAL_HIT = reg("bullet_metal_hit");
    public static final Supplier<SoundEvent> CAR_ENTER = reg("car_enter");
    public static final Supplier<SoundEvent> CAR_EXIT = reg("car_exit");
    public static final Supplier<SoundEvent> TRUNK_OPEN_SEDAN = reg("trunk_open_sedan");
    public static final Supplier<SoundEvent> TRUNK_CLOSE_SEDAN = reg("trunk_close_sedan");
    public static final Supplier<SoundEvent> TRUNK_OPEN_PICKUP = reg("trunk_open_pickup");
    public static final Supplier<SoundEvent> TRUNK_CLOSE_PICKUP = reg("trunk_close_pickup");
    public static final Supplier<SoundEvent> HEADLIGHTS_ON = reg("headlights_on");
    public static final Supplier<SoundEvent> HEADLIGHTS_OFF = reg("headlights_off");
    public static final Supplier<SoundEvent> CAR_LOCK_PIN_DOWN = reg("car_lock_pin_down");
    public static final Supplier<SoundEvent> CAR_UNLOCK_PIN_UP = reg("car_unlock_pin_up");
    public static final Supplier<SoundEvent> CAR_UNLOCK_KEY = reg("car_unlock_key");
    public static final Supplier<SoundEvent> CAR_DOOR_LOCKED_YANK = reg("car_door_locked_yank");
    public static final Supplier<SoundEvent> KEY_INSERT = reg("key_insert");
    public static final Supplier<SoundEvent> KEY_REMOVE = reg("key_remove");
    public static final Supplier<SoundEvent> BRAKE_PEDAL = reg("brake_pedal");
    public static final Supplier<SoundEvent> CLUTCH_RELEASE = reg("clutch_release");
    public static final Supplier<SoundEvent> PEDAL_PRESS = reg("pedal_press");
    public static final Supplier<SoundEvent> PEDAL_RELEASE = reg("pedal_release");
    public static final Supplier<SoundEvent> CAR_RAIN_CABIN = reg("car_rain_cabin");
    public static final Supplier<SoundEvent> VEHICLE_ITEM_PUT = reg("vehicle_item_put");
    public static final Supplier<SoundEvent> CAR_HEATER = reg("car_heater");
    public static final Supplier<SoundEvent> CAR_HEATER_START = reg("car_heater_start");
    public static final Supplier<SoundEvent> CAR_HEATER_STOP = reg("car_heater_stop");

    public static void register() {
        // Trigger class loading to register sounds
    }
}
