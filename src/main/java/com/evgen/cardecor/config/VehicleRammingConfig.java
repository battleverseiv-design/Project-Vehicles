package com.evgen.cardecor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

public class VehicleRammingConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static boolean enabled = true;

    // Global multipliers
    public static float recoilDamageMultiplier = 1.0F;
    public static float damageDealtMultiplier = 1.0F;
    public static float momentumRetentionMultiplier = 1.0F;
    public static float launchVelocityMultiplier = 1.0F;
    public static float cameraTraumaMultiplier = 1.0F;
    public static float bloodSplatterMultiplier = 1.0F;

    // Speed thresholds in km/h
    public static double nudgeSpeedKmh = 18.0;
    public static double damageSpeedKmh = 14.0;
    public static double cinematicLaunchSpeedKmh = 25.0;
    public static double metalDeformSpeedKmh = 30.0;
    public static double lethalObliterateSpeedKmh = 35.0;

    // Damage tuning
    public static double biologicalRecoilDivisor = 2800.0;
    public static double metalGolemRecoilDivisor = 650.0;
    public static float maxBiologicalRecoilPerHit = 5.0F;
    public static float maxGolemRecoilPerHit = 45.0F;

    public static void load() {
        File dir = new File("config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        configFile = new File(dir, "cardecor_ramming_settings.json");
        if (!configFile.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();

            if (root.has("global_multipliers")) {
                JsonObject m = root.getAsJsonObject("global_multipliers");
                if (m.has("recoil_damage_multiplier")) recoilDamageMultiplier = m.get("recoil_damage_multiplier").getAsFloat();
                if (m.has("damage_dealt_multiplier")) damageDealtMultiplier = m.get("damage_dealt_multiplier").getAsFloat();
                if (m.has("momentum_retention_multiplier")) momentumRetentionMultiplier = m.get("momentum_retention_multiplier").getAsFloat();
                if (m.has("launch_velocity_multiplier")) launchVelocityMultiplier = m.get("launch_velocity_multiplier").getAsFloat();
                if (m.has("camera_trauma_multiplier")) cameraTraumaMultiplier = m.get("camera_trauma_multiplier").getAsFloat();
                if (m.has("blood_splatter_multiplier")) bloodSplatterMultiplier = m.get("blood_splatter_multiplier").getAsFloat();
            }

            if (root.has("speed_thresholds_kmh")) {
                JsonObject s = root.getAsJsonObject("speed_thresholds_kmh");
                if (s.has("nudge_speed")) nudgeSpeedKmh = s.get("nudge_speed").getAsDouble();
                if (s.has("damage_speed")) damageSpeedKmh = s.get("damage_speed").getAsDouble();
                if (s.has("cinematic_launch_speed")) cinematicLaunchSpeedKmh = s.get("cinematic_launch_speed").getAsDouble();
                if (s.has("metal_deform_speed")) metalDeformSpeedKmh = s.get("metal_deform_speed").getAsDouble();
                if (s.has("lethal_obliterate_speed")) lethalObliterateSpeedKmh = s.get("lethal_obliterate_speed").getAsDouble();
            }

            if (root.has("damage_tuning")) {
                JsonObject d = root.getAsJsonObject("damage_tuning");
                if (d.has("biological_recoil_divisor")) biologicalRecoilDivisor = d.get("biological_recoil_divisor").getAsDouble();
                if (d.has("metal_golem_recoil_divisor")) metalGolemRecoilDivisor = d.get("metal_golem_recoil_divisor").getAsDouble();
                if (d.has("max_biological_recoil_per_hit")) maxBiologicalRecoilPerHit = d.get("max_biological_recoil_per_hit").getAsFloat();
                if (d.has("max_golem_recoil_per_hit")) maxGolemRecoilPerHit = d.get("max_golem_recoil_per_hit").getAsFloat();
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to load cardecor_ramming_settings.json: " + e.getMessage());
        }
    }

    public static void save() {
        if (configFile == null) {
            File dir = new File("config");
            if (!dir.exists()) dir.mkdirs();
            configFile = new File(dir, "cardecor_ramming_settings.json");
        }

        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);

        JsonObject m = new JsonObject();
        m.addProperty("recoil_damage_multiplier", recoilDamageMultiplier);
        m.addProperty("damage_dealt_multiplier", damageDealtMultiplier);
        m.addProperty("momentum_retention_multiplier", momentumRetentionMultiplier);
        m.addProperty("launch_velocity_multiplier", launchVelocityMultiplier);
        m.addProperty("camera_trauma_multiplier", cameraTraumaMultiplier);
        m.addProperty("blood_splatter_multiplier", bloodSplatterMultiplier);
        root.add("global_multipliers", m);

        JsonObject s = new JsonObject();
        s.addProperty("nudge_speed", nudgeSpeedKmh);
        s.addProperty("damage_speed", damageSpeedKmh);
        s.addProperty("cinematic_launch_speed", cinematicLaunchSpeedKmh);
        s.addProperty("metal_deform_speed", metalDeformSpeedKmh);
        s.addProperty("lethal_obliterate_speed", lethalObliterateSpeedKmh);
        root.add("speed_thresholds_kmh", s);

        JsonObject d = new JsonObject();
        d.addProperty("biological_recoil_divisor", biologicalRecoilDivisor);
        d.addProperty("metal_golem_recoil_divisor", metalGolemRecoilDivisor);
        d.addProperty("max_biological_recoil_per_hit", maxBiologicalRecoilPerHit);
        d.addProperty("max_golem_recoil_per_hit", maxGolemRecoilPerHit);
        root.add("damage_tuning", d);

        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to save cardecor_ramming_settings.json: " + e.getMessage());
        }
    }
}
