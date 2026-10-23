package com.evgen.cardecor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class VehicleNoiseConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static boolean enabled = true;
    public static float engineNoiseMultiplier = 1.0F;
    public static float hornNoiseMultiplier = 1.0F;
    public static float maxNoiseRadius = 128.0F;
    public static double investigateSpeed = 1.0D;
    public static double wanderRadiusAroundSound = 8.0D;

    // Filter mode: "ALLOW_ALL", "WHITELIST", "BLACKLIST"
    public static String filterMode = "WHITELIST";
    public static Set<String> whitelist = new HashSet<>();
    public static Set<String> blacklist = new HashSet<>();
    public static boolean affectOnlyHostileMobs = true;

    static {
        // Defaults
        whitelist.add("minecraft:zombie");
        whitelist.add("minecraft:husk");
        whitelist.add("minecraft:drowned");
        whitelist.add("minecraft:zombie_villager");
        whitelist.add("minecraft:zombified_piglin");

        blacklist.add("minecraft:creeper");
        blacklist.add("minecraft:enderman");
        blacklist.add("minecraft:skeleton");
    }

    public static void load() {
        File dir = new File("config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        configFile = new File(dir, "cardecor_noise_settings.json");
        if (!configFile.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
            if (json.has("engineNoiseMultiplier")) engineNoiseMultiplier = json.get("engineNoiseMultiplier").getAsFloat();
            if (json.has("hornNoiseMultiplier")) hornNoiseMultiplier = json.get("hornNoiseMultiplier").getAsFloat();
            if (json.has("maxNoiseRadius")) maxNoiseRadius = json.get("maxNoiseRadius").getAsFloat();
            if (json.has("investigateSpeed")) investigateSpeed = json.get("investigateSpeed").getAsDouble();
            if (json.has("wanderRadiusAroundSound")) wanderRadiusAroundSound = json.get("wanderRadiusAroundSound").getAsDouble();
            if (json.has("filterMode")) filterMode = json.get("filterMode").getAsString().toUpperCase();
            if (json.has("affectOnlyHostileMobs")) affectOnlyHostileMobs = json.get("affectOnlyHostileMobs").getAsBoolean();

            if (json.has("whitelist") && json.get("whitelist").isJsonArray()) {
                whitelist.clear();
                for (JsonElement el : json.getAsJsonArray("whitelist")) {
                    whitelist.add(el.getAsString().toLowerCase());
                }
            }
            if (json.has("blacklist") && json.get("blacklist").isJsonArray()) {
                blacklist.clear();
                for (JsonElement el : json.getAsJsonArray("blacklist")) {
                    blacklist.add(el.getAsString().toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            File dir = new File("config");
            if (!dir.exists()) dir.mkdirs();
            if (configFile == null) configFile = new File(dir, "cardecor_noise_settings.json");

            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("engineNoiseMultiplier", engineNoiseMultiplier);
            json.addProperty("hornNoiseMultiplier", hornNoiseMultiplier);
            json.addProperty("maxNoiseRadius", maxNoiseRadius);
            json.addProperty("investigateSpeed", investigateSpeed);
            json.addProperty("wanderRadiusAroundSound", wanderRadiusAroundSound);
            json.addProperty("filterMode", filterMode);
            json.addProperty("affectOnlyHostileMobs", affectOnlyHostileMobs);

            JsonArray wl = new JsonArray();
            for (String id : whitelist) wl.add(id);
            json.add("whitelist", wl);

            JsonArray bl = new JsonArray();
            for (String id : blacklist) bl.add(id);
            json.add("blacklist", bl);

            try (Writer writer = new FileWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean shouldMobInvestigate(Entity mob) {
        if (!enabled) return false;
        if (mob == null || !mob.isAlive()) return false;
        if (!(mob instanceof Mob)) return false;

        if (affectOnlyHostileMobs && !(mob instanceof Enemy)) {
            return false;
        }

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().toLowerCase();

        switch (filterMode) {
            case "WHITELIST":
                return whitelist.contains(entityId);
            case "BLACKLIST":
                return !blacklist.contains(entityId);
            case "ALLOW_ALL":
            default:
                return true;
        }
    }
}
