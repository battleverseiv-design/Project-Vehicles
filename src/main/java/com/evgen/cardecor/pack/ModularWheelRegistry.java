package com.evgen.cardecor.pack;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModularWheelRegistry {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    private static final Map<String, RimDefinition> RIMS = new ConcurrentHashMap<>();
    private static final Map<String, TireDefinition> TIRES = new ConcurrentHashMap<>();
    private static final Map<String, BrakeDefinition> BRAKES = new ConcurrentHashMap<>();

    static {
        init();
    }

    public static void init() {
        registerBuiltInRims();
        registerBuiltInTires();
        registerBuiltInBrakes();
    }

    private static void registerBuiltInRims() {
        // 1. Offroad Beadlock 16-bolt
        registerRim("rim_01_offroad_beadlock", "4x4 Offroad Beadlock (16 болтов)", 0.0, 9.5, "models/wheels/rims/rim_01_offroad_beadlock.obj");
        // 2. Wrangler Canyon 5-spoke
        registerRim("rim_02_wrangler_canyon", "Jeep Canyon (5-спицевый)", 0.0, 8.5, "models/wheels/rims/rim_02_wrangler_canyon.obj");
        // 3. Muscle Torq-Thrust Mag 5
        registerRim("rim_03_muscle_torq5", "Torq-Thrust Mag 5 (Маслкар)", 0.0, 7.8, "models/wheels/rims/rim_03_muscle_torq5.obj");
        // 4. Stamped Steelie
        registerRim("rim_04_stamped_steelie", "Стальная штамповка OEM", 0.0, 9.0, "models/wheels/rims/rim_04_stamped_steelie.obj");
        // 5. Dog Dish / Baby Moon
        registerRim("rim_05_dog_dish", "Штамповка с хром-колпаком Baby Moon", 0.0, 9.2, "models/wheels/rims/rim_05_dog_dish.obj");
        // 6. Heavy Dually HD
        registerRim("rim_06_heavy_dually", "Грузовой HD диск с глубоким вылетом", -0.15, 12.5, "models/wheels/rims/rim_06_heavy_dually.obj");
        // 7. Tuner BBS Mesh
        registerRim("rim_07_tuner_bbs_mesh", "Спортивная паутинка BBS Y-Spoke", 0.0, 6.8, "models/wheels/rims/rim_07_tuner_bbs_mesh.obj");
        // 8. Rally Multispoke
        registerRim("rim_08_rally_multispoke", "Раллийный многоспицевый OZ Racing", 0.0, 7.2, "models/wheels/rims/rim_08_rally_multispoke.obj");
        // 9. Method Con6
        registerRim("rim_09_method_con6", "Внедорожный Method Con6", 0.0, 8.8, "models/wheels/rims/rim_09_method_con6.obj");
        // 10. Retro Smoothie Chrome
        registerRim("rim_10_retro_smoothie", "Хромированный кастом Smoothie", 0.0, 8.2, "models/wheels/rims/rim_10_retro_smoothie.obj");
    }

    private static void registerRim(String id, String name, double offset, double mass, String model) {
        RimDefinition rim = new RimDefinition();
        rim.id = id;
        rim.name = name;
        rim.deepDishOffset = offset;
        rim.massKg = mass;
        rim.model = model;
        rim.texture = "textures/wheel_palette.png";
        register(rim);
    }

    private static void registerBuiltInTires() {
        // 1. Offroad Mud 33"
        registerTire("tire_01_offroad_mud", "33\" Mud-Terrain (M/T)", "offroad", 0.42, 0.26, 0.85, 1.35, 0.40, 14.0, 250.0, "models/wheels/tires/tire_01_offroad_mud.obj");
        // 2. All-Terrain A/T
        registerTire("tire_02_all_terrain", "All-Terrain (A/T)", "all_terrain", 0.40, 0.24, 0.95, 1.15, 0.35, 12.0, 200.0, "models/wheels/tires/tire_02_all_terrain.obj");
        // 3. Classic Radial
        registerTire("tire_03_classic_radial", "Classic Radial (Ретро)", "classic", 0.38, 0.22, 1.00, 0.82, 0.25, 10.0, 150.0, "models/wheels/tires/tire_03_classic_radial.obj");
        // 4. Muscle Wide
        registerTire("tire_04_muscle_wide", "Muscle Wide (Широкая)", "muscle", 0.37, 0.26, 1.15, 0.70, 0.20, 11.0, 160.0, "models/wheels/tires/tire_04_muscle_wide.obj");
        // 5. Low Profile Sport
        registerTire("tire_05_low_profile", "Low Profile Sport (Низкопрофильная)", "sport", 0.35, 0.24, 1.30, 0.50, 0.15, 9.0, 130.0, "models/wheels/tires/tire_05_low_profile.obj");
        // 6. Commercial HD
        registerTire("tire_06_commercial_hd", "Commercial Heavy-Duty (Грузовая)", "commercial", 0.41, 0.25, 0.90, 0.95, 0.30, 15.0, 300.0, "models/wheels/tires/tire_06_commercial_hd.obj");
    }

    private static void registerTire(String id, String name, String type, double radius, double width, double asphalt, double dirt, double ice, double mass, double hp, String model) {
        TireDefinition tire = new TireDefinition();
        tire.id = id;
        tire.name = name;
        tire.type = type;
        tire.radius = radius;
        tire.width = width;
        tire.gripAsphalt = asphalt;
        tire.gripDirt = dirt;
        tire.gripIce = ice;
        tire.massKg = mass;
        tire.durabilityHp = hp;
        tire.model = model;
        tire.texture = "textures/wheel_palette.png";
        register(tire);
    }

    private static void registerBuiltInBrakes() {
        // 1. Classic Cast-Iron Drum
        registerBrake("brake_01_classic_drum", "Чугунный барабанный тормоз", "drum", 0.85, 0.80, true, 8.0, "models/wheels/brakes/brake_01_classic_drum.obj");
        // 2. Stock Vented Rotor + Grey Caliper
        registerBrake("brake_02_stock_vented", "Заводской вентилируемый ротор OEM", "vented", 1.00, 1.00, false, 6.0, "models/wheels/brakes/brake_02_stock_vented.obj");
        // 3. Sport Drilled Rotor + Red Brembo Caliper
        registerBrake("brake_03_sport_drilled_red", "Спортивный ротор Brembo Red", "sport", 1.40, 1.40, false, 5.5, "models/wheels/brakes/brake_03_sport_drilled_red.obj");
        // 4. Oversized Carbon-Ceramic + Gold 6-Piston Caliper
        registerBrake("brake_04_big_tuner_gold", "Карбон-керамика Gold Tuner", "ceramic", 1.75, 1.80, false, 4.5, "models/wheels/brakes/brake_04_big_tuner_gold.obj");
        // 5. Heavy Commercial Dual Cast-Iron Brake
        registerBrake("brake_05_heavy_commercial", "Усиленный коммерческий тормоз HD", "commercial", 1.55, 1.50, false, 11.0, "models/wheels/brakes/brake_05_heavy_commercial.obj");
    }

    private static void registerBrake(String id, String name, String type, double forceMult, double fadeRes, boolean isDrum, double mass, String model) {
        BrakeDefinition brake = new BrakeDefinition();
        brake.id = id;
        brake.name = name;
        brake.type = type;
        brake.brakeForceMult = forceMult;
        brake.fadeResistance = fadeRes;
        brake.isDrum = isDrum;
        brake.massKg = mass;
        brake.model = model;
        brake.texture = "textures/wheel_palette.png";
        register(brake);
    }

    // --- RIM API ---
    public static void register(RimDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        RIMS.put(id, def);
        if (id.contains(":")) {
            RIMS.putIfAbsent(id.substring(id.indexOf(':') + 1), def);
        }
    }

    public static RimDefinition getRim(String id) {
        if (id == null || id.trim().isEmpty()) return getFallbackRim();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        RimDefinition def = RIMS.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = RIMS.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (RimDefinition r : RIMS.values()) {
            if (r.id.equalsIgnoreCase(clean)) return r;
        }
        return getFallbackRim();
    }

    public static RimDefinition getFallbackRim() {
        RimDefinition def = RIMS.get("rim_04_stamped_steelie");
        if (def != null) return def;
        return RIMS.values().stream().findFirst().orElseGet(RimDefinition::new);
    }

    public static List<RimDefinition> getAllRims() {
        return new ArrayList<>(new LinkedHashSet<>(RIMS.values()));
    }

    // --- TIRE API ---
    public static void register(TireDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        TIRES.put(id, def);
        if (id.contains(":")) {
            TIRES.putIfAbsent(id.substring(id.indexOf(':') + 1), def);
        }
    }

    public static TireDefinition getTire(String id) {
        if (id == null || id.trim().isEmpty()) return getFallbackTire();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        TireDefinition def = TIRES.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = TIRES.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (TireDefinition t : TIRES.values()) {
            if (t.id.equalsIgnoreCase(clean)) return t;
        }
        return getFallbackTire();
    }

    public static TireDefinition getFallbackTire() {
        TireDefinition def = TIRES.get("tire_03_classic_radial");
        if (def != null) return def;
        return TIRES.values().stream().findFirst().orElseGet(TireDefinition::new);
    }

    public static List<TireDefinition> getAllTires() {
        return new ArrayList<>(new LinkedHashSet<>(TIRES.values()));
    }

    // --- BRAKE API ---
    public static void register(BrakeDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        BRAKES.put(id, def);
        if (id.contains(":")) {
            BRAKES.putIfAbsent(id.substring(id.indexOf(':') + 1), def);
        }
    }

    public static BrakeDefinition getBrake(String id) {
        if (id == null || id.trim().isEmpty()) return getFallbackBrake();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        BrakeDefinition def = BRAKES.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = BRAKES.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (BrakeDefinition b : BRAKES.values()) {
            if (b.id.equalsIgnoreCase(clean)) return b;
        }
        return getFallbackBrake();
    }

    public static BrakeDefinition getFallbackBrake() {
        BrakeDefinition def = BRAKES.get("brake_02_stock_vented");
        if (def != null) return def;
        return BRAKES.values().stream().findFirst().orElseGet(BrakeDefinition::new);
    }

    public static List<BrakeDefinition> getAllBrakes() {
        return new ArrayList<>(new LinkedHashSet<>(BRAKES.values()));
    }

    // --- PACK SCANNING ---
    public static void scanPack(File packDir) {
        if (packDir == null || !packDir.isDirectory()) return;
        scanRimsDir(new File(packDir, "rims"), packDir.getName());
        scanTiresDir(new File(packDir, "tires"), packDir.getName());
        scanBrakesDir(new File(packDir, "brakes"), packDir.getName());
    }

    private static void scanRimsDir(File dir, String packNamespace) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                String content = java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                RimDefinition def = GSON.fromJson(content, RimDefinition.class);
                if (def != null) {
                    def.packDirectory = dir.getParentFile();
                    if (def.id == null || def.id.trim().isEmpty()) def.id = f.getName().replace(".json", "");
                    register(def);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Failed to parse rim " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void scanTiresDir(File dir, String packNamespace) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                String content = java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                TireDefinition def = GSON.fromJson(content, TireDefinition.class);
                if (def != null) {
                    def.packDirectory = dir.getParentFile();
                    if (def.id == null || def.id.trim().isEmpty()) def.id = f.getName().replace(".json", "");
                    register(def);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Failed to parse tire " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void scanBrakesDir(File dir, String packNamespace) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                String content = java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                BrakeDefinition def = GSON.fromJson(content, BrakeDefinition.class);
                if (def != null) {
                    def.packDirectory = dir.getParentFile();
                    if (def.id == null || def.id.trim().isEmpty()) def.id = f.getName().replace(".json", "");
                    register(def);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Failed to parse brake " + f.getName() + ": " + e.getMessage());
            }
        }
    }
}
