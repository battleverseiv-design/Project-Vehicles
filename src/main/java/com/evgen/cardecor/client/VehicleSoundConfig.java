package com.evgen.cardecor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

public class VehicleSoundConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static float masterVolume = 1.00F;
    public static float engineVolume = 1.00F;
    public static float insideVolume = 1.00F;
    public static float tireRollVolume = 1.00F;
    public static float brakeVolume = 1.00F;
    public static float gearShiftVolume = 1.00F;
    public static float impactVolume = 1.00F;
    public static float hornVolume = 2.00F;
    public static float starterVolume = 1.00F;
    public static float dopplerIntensity = 1.00F;

    public static void init() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        configFile = new File(configDir, "cardecor_sound_settings.json");
        load();
    }

    public static void load() {
        if (configFile == null || !configFile.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("masterVolume")) masterVolume = json.get("masterVolume").getAsFloat();
            if (json.has("engineVolume")) engineVolume = json.get("engineVolume").getAsFloat();
            if (json.has("insideVolume")) insideVolume = json.get("insideVolume").getAsFloat();
            if (json.has("tireRollVolume")) tireRollVolume = json.get("tireRollVolume").getAsFloat();
            if (json.has("brakeVolume")) brakeVolume = json.get("brakeVolume").getAsFloat();
            if (json.has("gearShiftVolume")) gearShiftVolume = json.get("gearShiftVolume").getAsFloat();
            if (json.has("impactVolume")) impactVolume = json.get("impactVolume").getAsFloat();
            if (json.has("hornVolume")) hornVolume = json.get("hornVolume").getAsFloat();
            if (json.has("starterVolume")) starterVolume = json.get("starterVolume").getAsFloat();
            if (json.has("dopplerIntensity")) dopplerIntensity = json.get("dopplerIntensity").getAsFloat();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        if (configFile == null) {
            File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
            if (!configDir.exists()) configDir.mkdirs();
            configFile = new File(configDir, "cardecor_sound_settings.json");
        }

        try (Writer writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();
            json.addProperty("masterVolume", masterVolume);
            json.addProperty("engineVolume", engineVolume);
            json.addProperty("insideVolume", insideVolume);
            json.addProperty("tireRollVolume", tireRollVolume);
            json.addProperty("brakeVolume", brakeVolume);
            json.addProperty("gearShiftVolume", gearShiftVolume);
            json.addProperty("impactVolume", impactVolume);
            json.addProperty("hornVolume", hornVolume);
            json.addProperty("starterVolume", starterVolume);
            json.addProperty("dopplerIntensity", dopplerIntensity);
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void resetDefaults() {
        masterVolume = 1.00F;
        engineVolume = 1.00F;
        insideVolume = 1.00F;
        tireRollVolume = 1.00F;
        brakeVolume = 1.00F;
        gearShiftVolume = 1.00F;
        impactVolume = 1.00F;
        hornVolume = 2.00F;
        starterVolume = 1.00F;
        dopplerIntensity = 1.00F;
        save();
    }
}
