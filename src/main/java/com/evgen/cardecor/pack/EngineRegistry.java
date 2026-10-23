package com.evgen.cardecor.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EngineRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, EngineDefinition> REGISTRY = new ConcurrentHashMap<>();

    static {
        registerBuiltInEngines();
    }

    public static void init() {
        registerBuiltInEngines();
    }

    private static void registerBuiltInEngines() {
        // 1. AMC 4.0L High Output Inline-6 (Jeep)
        EngineDefinition amcI6 = new EngineDefinition();
        amcI6.id = "amc_4_0_i6";
        amcI6.name = "AMC 4.0L High Output (I6)";
        amcI6.type = "inline6";
        amcI6.author = "Battleverse";
        amcI6.physics.horsepower = 190.0;
        amcI6.physics.torqueNm = 310.0;
        amcI6.physics.idleRpm = 850.0;
        amcI6.physics.redlineRpm = 5500.0;
        amcI6.physics.maxRpm = 6200.0;
        amcI6.physics.revSpeed = 1.35;
        amcI6.physics.engineBraking = 1.1;
        amcI6.sounds.starter = "cardecor:starter_i6";
        amcI6.sounds.start = "cardecor:blue_sedan_start";
        amcI6.sounds.stop = "cardecor:engine_stop_i6";
        amcI6.sounds.layers.idle = "cardecor:amc_i6_idle";
        amcI6.sounds.layers.low  = "cardecor:amc_i6_low";
        amcI6.sounds.layers.mid  = "cardecor:amc_i6_mid";
        amcI6.sounds.layers.high = "cardecor:amc_i6_high";
        amcI6.sounds.layers.max  = "cardecor:amc_i6_max";
        amcI6.sounds.transitions.idleToLow = 1600.0F;
        amcI6.sounds.transitions.lowToMid  = 2800.0F;
        amcI6.sounds.transitions.midToHigh = 4200.0F;
        amcI6.sounds.transitions.highToMax = 5400.0F;
        register(amcI6);

        // 2. Ford 302 Windsor 5.0L V8 (Pickup)
        EngineDefinition fordV8 = new EngineDefinition();
        fordV8.id = "ford_302_v8";
        fordV8.name = "Ford 302 Windsor 5.0L (V8)";
        fordV8.type = "v8";
        fordV8.author = "Battleverse";
        fordV8.physics.horsepower = 160.0;
        fordV8.physics.torqueNm = 340.0;
        fordV8.physics.idleRpm = 650.0;
        fordV8.physics.redlineRpm = 4400.0;
        fordV8.physics.maxRpm = 4800.0;
        fordV8.physics.revSpeed = 1.15;
        fordV8.physics.engineBraking = 1.35;
        fordV8.block.cylinders = 8;
        fordV8.block.configuration = "v";
        fordV8.block.boreMm = 101.6;
        fordV8.block.strokeMm = 76.2;
        fordV8.block.compressionRatio = 8.4;
        fordV8.valvetrain.type = "ohv_pushrod";
        fordV8.valvetrain.camProfile = "truck_towing";
        fordV8.flywheel.massKg = 18.5;
        fordV8.flywheel.radiusM = 0.16;
        fordV8.limits.idleRpm = 650.0;
        fordV8.limits.redlineRpm = 4400.0;
        fordV8.limits.limiterType = "valve_float";
        fordV8.sounds.starter = "cardecor:starter_v8";
        fordV8.sounds.start = "cardecor:vintage_engine_start";
        fordV8.sounds.stop = "cardecor:engine_stop_v8";
        fordV8.sounds.kickdown = "cardecor:engine_kickdown";
        fordV8.sounds.camshaftLope = true;
        fordV8.sounds.layers.idle = "cardecor:pickup_engine_0";
        fordV8.sounds.layers.low  = "cardecor:pickup_engine_1";
        fordV8.sounds.layers.mid  = "cardecor:pickup_engine_2";
        fordV8.sounds.layers.high = "cardecor:pickup_engine_3";
        fordV8.sounds.layers.max  = "cardecor:pickup_engine_4";
        fordV8.sounds.transitions.idleToLow = 1100.0F;
        fordV8.sounds.transitions.lowToMid  = 2000.0F;
        fordV8.sounds.transitions.midToHigh = 3000.0F;
        fordV8.sounds.transitions.highToMax = 3900.0F;
        register(fordV8);

        // 3. 3.0L Inline-6 Twin Turbo (Blue Sedan)
        EngineDefinition diesel = new EngineDefinition();
        diesel.id = "om617_diesel";
        diesel.name = "3.0L Inline-6 Twin Turbo";
        diesel.type = "i6";
        diesel.author = "Battleverse";
        diesel.physics.horsepower = 100.0;
        diesel.physics.torqueNm = 280.0;
        diesel.physics.idleRpm = 850.0;
        diesel.physics.redlineRpm = 7200.0;
        diesel.physics.maxRpm = 7800.0;
        diesel.physics.revSpeed = 1.25;
        diesel.physics.engineBraking = 1.2;
        diesel.sounds.starter = "cardecor:starter_i4";
        diesel.sounds.start = "cardecor:blue_sedan_start";
        diesel.sounds.stop = "cardecor:engine_stop_i6";
        diesel.sounds.layers.idle = "cardecor:blue_sedan_idle";
        diesel.sounds.layers.low  = "cardecor:blue_sedan_low";
        diesel.sounds.layers.mid  = "cardecor:blue_sedan_mid";
        diesel.sounds.layers.high = "cardecor:blue_sedan_high";
        diesel.sounds.layers.max  = "cardecor:blue_sedan_max";
        diesel.sounds.transitions.idleToLow = 1800.0F;
        diesel.sounds.transitions.lowToMid  = 3300.0F;
        diesel.sounds.transitions.midToHigh = 4800.0F;
        diesel.sounds.transitions.highToMax = 6200.0F;
        register(diesel);
    }

    public static void register(EngineDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        REGISTRY.put(id, def);
        if (id.contains(":")) {
            String shortId = id.substring(id.indexOf(':') + 1);
            REGISTRY.putIfAbsent(shortId, def);
        }
    }

    public static EngineDefinition get(String id) {
        if (id == null || id.trim().isEmpty()) return getFallback();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        EngineDefinition def = REGISTRY.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = REGISTRY.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (EngineDefinition e : REGISTRY.values()) {
            if (e.id.equalsIgnoreCase(clean) || (e.type != null && e.type.equalsIgnoreCase(clean))) {
                return e;
            }
        }
        return getFallback();
    }

    public static EngineDefinition getFallback() {
        EngineDefinition def = REGISTRY.get("amc_4_0_i6");
        if (def != null) return def;
        return REGISTRY.values().stream().findFirst().orElseGet(EngineDefinition::new);
    }

    public static List<EngineDefinition> getAll() {
        Set<EngineDefinition> set = new LinkedHashSet<>(REGISTRY.values());
        return new ArrayList<>(set);
    }

    public static void scanPackEngines(File packDir) {
        if (packDir == null || !packDir.isDirectory()) return;
        File enginesDir = new File(packDir, "engines");
        if (!enginesDir.exists() || !enginesDir.isDirectory()) {
            File[] subs = packDir.listFiles(File::isDirectory);
            if (subs != null) {
                for (File s : subs) {
                    if (s.getName().startsWith(".")) continue;
                    File sEngines = new File(s, "engines");
                    if (sEngines.exists() && sEngines.isDirectory()) {
                        scanEnginesDirectory(sEngines, s.getName());
                    }
                }
            }
            return;
        }
        scanEnginesDirectory(enginesDir, packDir.getName());
    }

    private static void scanEnginesDirectory(File enginesDir, String packNamespace) {
        File[] jsonFiles = enginesDir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (jsonFiles == null) return;
        for (File jf : jsonFiles) {
            try {
                String content = java.nio.file.Files.readString(jf.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                EngineDefinition def = GSON.fromJson(content, EngineDefinition.class);
                if (def != null) {
                    if (def.id == null || def.id.trim().isEmpty()) {
                        def.id = jf.getName().replace(".json", "");
                    }
                    String namespacedId = packNamespace + ":" + def.id;
                    REGISTRY.put(namespacedId.toLowerCase(Locale.ROOT), def);
                    REGISTRY.put(def.id.toLowerCase(Locale.ROOT), def);
                    System.out.println("[CarDecor] Loaded modular engine: " + def.name + " (" + def.id + ") from " + packNamespace);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Error parsing engine " + jf.getName() + ": " + e.getMessage());
            }
        }
    }
}
