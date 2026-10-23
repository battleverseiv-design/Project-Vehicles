package com.evgen.cardecor.pack;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WheelRegistry {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    private static final Map<String, WheelDefinition> REGISTRY = new ConcurrentHashMap<>();

    static {
        registerBuiltInWheels();
    }

    public static void init() {
        registerBuiltInWheels();
    }

    private static void registerBuiltInWheels() {
        // 1. Standard Factory Road Wheel
        WheelDefinition std = new WheelDefinition();
        std.id = "standard_road_wheel";
        std.name = "Заводское дорожное колесо (15\")";
        std.type = "wheel";
        std.author = "CarDecor";
        std.radius = 0.38;
        std.width = 0.22;
        std.gripAsphalt = 1.0;
        std.gripDirt = 0.82;
        std.gripIce = 0.25;
        std.massKg = 18.0;
        std.durabilityHp = 150.0;
        std.model = "models/wheel.obj";
        register(std);

        // 2. Off-Road All-Terrain Wheel
        WheelDefinition offroad = new WheelDefinition();
        offroad.id = "offroad_heavy_wheel";
        offroad.name = "Внедорожное колесо All-Terrain (16\")";
        offroad.type = "offroad";
        offroad.author = "CarDecor";
        offroad.radius = 0.42;
        offroad.width = 0.26;
        offroad.gripAsphalt = 0.88;
        offroad.gripDirt = 1.25;
        offroad.gripIce = 0.45;
        offroad.massKg = 24.0;
        offroad.durabilityHp = 220.0;
        offroad.model = "models/wheel.obj";
        register(offroad);

        // 3. Sport High-Grip Alloy Wheel
        WheelDefinition sport = new WheelDefinition();
        sport.id = "sport_alloy_wheel";
        sport.name = "Спортивное низкопрофильное колесо (17\")";
        sport.type = "sport";
        sport.author = "CarDecor";
        sport.radius = 0.36;
        sport.width = 0.24;
        sport.gripAsphalt = 1.22;
        sport.gripDirt = 0.65;
        sport.gripIce = 0.15;
        sport.massKg = 15.0;
        sport.durabilityHp = 130.0;
        sport.model = "models/wheel.obj";
        register(sport);
    }

    public static void register(WheelDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        REGISTRY.put(id, def);
        if (id.contains(":")) {
            String shortId = id.substring(id.indexOf(':') + 1);
            REGISTRY.putIfAbsent(shortId, def);
        }
    }

    public static WheelDefinition get(String id) {
        if (id == null || id.trim().isEmpty()) return getFallback();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        WheelDefinition def = REGISTRY.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = REGISTRY.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (WheelDefinition w : REGISTRY.values()) {
            if (w.id.equalsIgnoreCase(clean)) {
                return w;
            }
        }
        if (clean.contains(";")) {
            String[] split = clean.split(";");
            clean = split[0];
        }
        TireDefinition tDef = ModularWheelRegistry.getTire(clean);
        if (tDef != null && (!"tire_03_classic_radial".equals(tDef.id) || "tire_03_classic_radial".equals(clean))) {
            WheelDefinition w = new WheelDefinition();
            w.id = tDef.id;
            w.name = tDef.name;
            w.radius = tDef.radius;
            w.width = tDef.width;
            w.gripAsphalt = tDef.gripAsphalt;
            w.gripDirt = tDef.gripDirt;
            w.gripIce = tDef.gripIce;
            w.massKg = tDef.massKg;
            w.durabilityHp = tDef.durabilityHp;
            w.model = tDef.model;
            w.texture = tDef.texture;
            return w;
        }
        return getFallback();
    }

    public static WheelDefinition getFallback() {
        WheelDefinition def = REGISTRY.get("standard_road_wheel");
        if (def != null) return def;
        return REGISTRY.values().stream().findFirst().orElseGet(WheelDefinition::new);
    }

    public static List<WheelDefinition> getAll() {
        Set<WheelDefinition> set = new LinkedHashSet<>(REGISTRY.values());
        return new ArrayList<>(set);
    }

    public static void scanPackWheels(File packDir) {
        if (packDir == null || !packDir.isDirectory()) return;
        File wheelsDir = new File(packDir, "wheels");
        if (!wheelsDir.exists() || !wheelsDir.isDirectory()) {
            File[] subs = packDir.listFiles(File::isDirectory);
            if (subs != null) {
                for (File s : subs) {
                    if (s.getName().startsWith(".")) continue;
                    File sWheels = new File(s, "wheels");
                    if (sWheels.exists() && sWheels.isDirectory()) {
                        scanWheelsDirectory(sWheels, s.getName());
                    }
                }
            }
            return;
        }
        scanWheelsDirectory(wheelsDir, packDir.getName());
    }

    private static void scanWheelsDirectory(File wheelsDir, String packNamespace) {
        File[] jsonFiles = wheelsDir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (jsonFiles == null) return;
        for (File jf : jsonFiles) {
            try {
                String content = java.nio.file.Files.readString(jf.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                WheelDefinition def = GSON.fromJson(content, WheelDefinition.class);
                if (def != null) {
                    def.packDirectory = wheelsDir.getParentFile();
                    if (def.id == null || def.id.trim().isEmpty()) {
                        def.id = jf.getName().replace(".json", "");
                    }
                    String namespacedId = packNamespace + ":" + def.id;
                    REGISTRY.put(namespacedId.toLowerCase(Locale.ROOT), def);
                    REGISTRY.put(def.id.toLowerCase(Locale.ROOT), def);
                    System.out.println("[CarDecor] Loaded modular wheel: " + def.name + " (" + def.id + ") from " + packNamespace);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Error parsing wheel " + jf.getName() + ": " + e.getMessage());
            }
        }
    }
}
