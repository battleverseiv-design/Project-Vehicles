package com.evgen.cardecor.climate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class VehicleClimateCompat {
    public static final UUID CAR_CLIMATE_UUID = UUID.fromString("c0a7e6b0-1234-4567-89ab-cdef01234567");

    private static boolean initialized = false;

    // Heater effects (Cold protection / warmth)
    private static MobEffect csIceResist;
    private static MobEffect tanWarmth;
    private static MobEffect tanChill;
    private static MobEffect tanClemency;
    private static MobEffect tanIceResist;
    private static MobEffect envzWarming;
    private static MobEffect envzCooling;
    private static MobEffect envzComfort;
    private static MobEffect lsoColdResist;
    private static MobEffect lsoColdImmunity;

    // A/C effects (Heat protection / cooling)
    private static MobEffect csFireResist;
    private static MobEffect lsoHeatResist;
    private static MobEffect lsoHeatImmunity;

    // LSO Reflection methods
    private static Method lsoAddTempMod;
    private static Method lsoAddColdResMod;
    private static Method lsoAddHeatResMod;
    private static boolean lsoApiAvailable = false;

    private static void init() {
        if (initialized) return;
        initialized = true;

        try {
            // Cold Sweat effects
            csIceResist = getEffect("cold_sweat", "ice_resistance");
            csFireResist = getEffect("cold_sweat", "fire_resistance");

            // Tough As Nails effects (1.20.1 actual IDs)
            tanWarmth = getEffect("toughasnails", "internal_warmth");
            tanChill = getEffect("toughasnails", "internal_chill");
            tanClemency = getEffect("toughasnails", "climate_clemency");
            tanIceResist = getEffect("toughasnails", "ice_resistance");

            // EnvironmentZ warmth, cooling and comfort
            envzWarming = getEffect("environmentz", "warming");
            envzCooling = getEffect("environmentz", "cooling");
            envzComfort = getEffect("environmentz", "comfort");

            // Legendary Survival Overhaul effects
            lsoColdResist = getEffect("legendarysurvivaloverhaul", "cold_resistance");
            lsoColdImmunity = getEffect("legendarysurvivaloverhaul", "cold_immunity");
            lsoHeatResist = getEffect("legendarysurvivaloverhaul", "heat_resistance");
            lsoHeatImmunity = getEffect("legendarysurvivaloverhaul", "heat_immunity");

            // Legendary Survival Overhaul API
            try {
                Class<?> tuClass = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil");
                lsoAddTempMod = tuClass.getMethod("addTemperatureModifier", Player.class, double.class, UUID.class);
                lsoAddColdResMod = tuClass.getMethod("addColdResistanceModifier", Player.class, double.class, UUID.class);
                lsoAddHeatResMod = tuClass.getMethod("addHeatResistanceModifier", Player.class, double.class, UUID.class);
                lsoApiAvailable = true;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
        }
    }

    private static MobEffect getEffect(String modId, String name) {
        try {
            ResourceLocation rl = new ResourceLocation(modId, name);
            if (BuiltInRegistries.MOB_EFFECT.containsKey(rl)) {
                return BuiltInRegistries.MOB_EFFECT.get(rl);
            }
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Object effectReg = forgeRegistries.getField("MOB_EFFECTS").get(null);
            Method getValue = effectReg.getClass().getMethod("getValue", ResourceLocation.class);
            return (MobEffect) getValue.invoke(effectReg, rl);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Attribute getAttribute(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            if (BuiltInRegistries.ATTRIBUTE.containsKey(rl)) {
                return BuiltInRegistries.ATTRIBUTE.get(rl);
            }
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Object attrReg = forgeRegistries.getField("ATTRIBUTES").get(null);
            Method getValue = attrReg.getClass().getMethod("getValue", ResourceLocation.class);
            return (Attribute) getValue.invoke(attrReg, rl);
        } catch (Throwable ignored) {}
        return null;
    }

    public static void apply(LivingEntity living, byte mode) {
        if (!initialized) {
            init();
        }

        if (mode == 1) { // 1 = HEATER (Печка / Обогрев)
            living.setTicksFrozen(0);

            if (living instanceof Player player) {
                if (lsoApiAvailable) {
                    try {
                        lsoAddTempMod.invoke(null, player, 12.0, CAR_CLIMATE_UUID);
                        lsoAddColdResMod.invoke(null, player, 8.0, CAR_CLIMATE_UUID);
                    } catch (Throwable ignored) {}
                } else {
                    applyAttributeModifier(player, "legendarysurvivaloverhaul:heating_temperature", 12.0);
                    applyAttributeModifier(player, "legendarysurvivaloverhaul:cold_resistance", 8.0);
                }

                // Tough As Nails direct API reflection (works on both Fabric & Forge)
                try {
                    Class<?> thClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
                    Method getTempData = thClass.getMethod("getTemperatureData", Player.class);
                    Object tempData = getTempData.invoke(null, player);
                    if (tempData != null) {
                        Class<?> tlClass = Class.forName("toughasnails.api.temperature.TemperatureLevel");
                        @SuppressWarnings("unchecked")
                        Object neutral = Enum.valueOf((Class<Enum>) tlClass, "NEUTRAL");
                        Method setLevel = tempData.getClass().getMethod("setLevel", tlClass);
                        setLevel.invoke(tempData, neutral);
                        Method setTargetLevel = tempData.getClass().getMethod("setTargetLevel", tlClass);
                        setTargetLevel.invoke(tempData, neutral);
                        Method setExtremityDelay = tempData.getClass().getMethod("setExtremityDelayTicks", int.class);
                        setExtremityDelay.invoke(tempData, 600);
                    }
                    Method setHyper = thClass.getMethod("setTicksHyperthermic", Player.class, int.class);
                    setHyper.invoke(null, player, 0);
                } catch (Throwable ignored) {}
            }

            applyInstance(living, csIceResist, 0, 60);
            applyInstance(living, tanWarmth, 1, 60);
            applyInstance(living, tanClemency, 0, 60);
            applyInstance(living, tanIceResist, 0, 60);
            applyInstance(living, envzWarming, 1, 60);
            applyInstance(living, envzComfort, 0, 60);
            applyInstance(living, lsoColdResist, 0, 60);
            applyInstance(living, lsoColdImmunity, 0, 60);

        } else if (mode == 2) { // 2 = A/C (Кондиционер / Охлаждение)
            if (living instanceof Player player) {
                if (lsoApiAvailable) {
                    try {
                        lsoAddTempMod.invoke(null, player, -12.0, CAR_CLIMATE_UUID);
                        lsoAddHeatResMod.invoke(null, player, 8.0, CAR_CLIMATE_UUID);
                    } catch (Throwable ignored) {}
                } else {
                    applyAttributeModifier(player, "legendarysurvivaloverhaul:cooling_temperature", -12.0);
                    applyAttributeModifier(player, "legendarysurvivaloverhaul:heat_resistance", 8.0);
                }

                // Tough As Nails direct API reflection
                try {
                    Class<?> thClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
                    Method getTempData = thClass.getMethod("getTemperatureData", Player.class);
                    Object tempData = getTempData.invoke(null, player);
                    if (tempData != null) {
                        Class<?> tlClass = Class.forName("toughasnails.api.temperature.TemperatureLevel");
                        @SuppressWarnings("unchecked")
                        Object neutral = Enum.valueOf((Class<Enum>) tlClass, "NEUTRAL");
                        Method setLevel = tempData.getClass().getMethod("setLevel", tlClass);
                        setLevel.invoke(tempData, neutral);
                        Method setTargetLevel = tempData.getClass().getMethod("setTargetLevel", tlClass);
                        setTargetLevel.invoke(tempData, neutral);
                    }
                    Method setHyper = thClass.getMethod("setTicksHyperthermic", Player.class, int.class);
                    setHyper.invoke(null, player, 0);
                } catch (Throwable ignored) {}
            }

            applyInstance(living, csFireResist, 0, 60);
            applyInstance(living, tanChill, 1, 60);
            applyInstance(living, tanClemency, 0, 60);
            applyInstance(living, envzCooling, 1, 60);
            applyInstance(living, envzComfort, 0, 60);
            applyInstance(living, lsoHeatResist, 0, 60);
            applyInstance(living, lsoHeatImmunity, 0, 60);
        }
    }

    private static void applyAttributeModifier(Player player, String attrId, double amount) {
        try {
            Attribute attr = getAttribute(attrId);
            if (attr != null) {
                AttributeInstance inst = player.getAttribute(attr);
                if (inst != null) {
                    inst.removeModifier(CAR_CLIMATE_UUID);
                    inst.addPermanentModifier(new AttributeModifier(CAR_CLIMATE_UUID, "CarClimateModifier", amount, AttributeModifier.Operation.ADDITION));
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void clear(LivingEntity living) {
        if (!initialized) {
            init();
        }
        if (living instanceof Player player) {
            try {
                String[] lsoAttrs = {
                    "legendarysurvivaloverhaul:heating_temperature",
                    "legendarysurvivaloverhaul:cooling_temperature",
                    "legendarysurvivaloverhaul:cold_resistance",
                    "legendarysurvivaloverhaul:heat_resistance",
                    "legendarysurvivaloverhaul:thermal_resistance"
                };
                for (String attrId : lsoAttrs) {
                    Attribute attr = getAttribute(attrId);
                    if (attr != null) {
                        AttributeInstance inst = player.getAttribute(attr);
                        if (inst != null) {
                            inst.removeModifier(CAR_CLIMATE_UUID);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void applyInstance(LivingEntity living, MobEffect effect, int amplifier, int duration) {
        if (effect != null) {
            try {
                living.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void applyInstance(LivingEntity living, MobEffect effect) {
        applyInstance(living, effect, 0, 60);
    }

    public static boolean isAnyTemperatureModLoaded() {
        if (!initialized) {
            init();
        }
        return csIceResist != null || tanWarmth != null || tanChill != null || envzWarming != null || envzCooling != null || lsoColdResist != null || lsoApiAvailable
            || hasMod("cold_sweat") || hasMod("toughasnails") || hasMod("environmentz") || hasMod("legendarysurvivaloverhaul");
    }

    private static boolean hasMod(String modId) {
        try {
            Class<?> fmlModList = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = fmlModList.getMethod("get").invoke(null);
            return (boolean) fmlModList.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (Throwable ignored) {}
        try {
            Class<?> fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoader.getMethod("getInstance").invoke(null);
            return (boolean) fabricLoader.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (Throwable ignored) {}
        return false;
    }
}
