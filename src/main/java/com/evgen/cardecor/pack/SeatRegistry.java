package com.evgen.cardecor.pack;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SeatRegistry {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    private static final Map<String, SeatDefinition> REGISTRY = new ConcurrentHashMap<>();

    static {
        registerBuiltInSeats();
    }

    public static void init() {
        registerBuiltInSeats();
    }

    private static void registerBuiltInSeats() {
        // 1. Standard Factory Seat
        SeatDefinition std = new SeatDefinition();
        std.id = "standard_seat";
        std.name = "Заводское сиденье";
        std.type = "seat";
        std.author = "CarDecor";
        std.comfortFactor = 1.0;
        std.sittingHeightOffset = 0.0F;
        std.massKg = 12.0;
        register(std);

        // 2. Luxury Leather Comfort Seat
        SeatDefinition luxury = new SeatDefinition();
        luxury.id = "leather_luxury_seat";
        luxury.name = "Кожаное комфортное кресло";
        luxury.type = "leather";
        luxury.author = "CarDecor";
        luxury.comfortFactor = 1.4;
        luxury.sittingHeightOffset = 0.02F;
        luxury.massKg = 16.0;
        register(luxury);

        // 3. Sport Bucket Racing Seat
        SeatDefinition sport = new SeatDefinition();
        sport.id = "sport_bucket_seat";
        sport.name = "Спортивный ковш (Racing)";
        sport.type = "bucket";
        sport.author = "CarDecor";
        sport.comfortFactor = 0.9;
        sport.sittingHeightOffset = -0.04F;
        sport.massKg = 8.5;
        register(sport);
    }

    public static void register(SeatDefinition def) {
        if (def == null || def.id == null || def.id.trim().isEmpty()) return;
        String id = def.id.trim().toLowerCase(Locale.ROOT);
        REGISTRY.put(id, def);
        if (id.contains(":")) {
            String shortId = id.substring(id.indexOf(':') + 1);
            REGISTRY.putIfAbsent(shortId, def);
        }
    }

    public static SeatDefinition get(String id) {
        if (id == null || id.trim().isEmpty()) return getFallback();
        String clean = id.trim().toLowerCase(Locale.ROOT);
        SeatDefinition def = REGISTRY.get(clean);
        if (def != null) return def;
        if (clean.contains(":")) {
            def = REGISTRY.get(clean.substring(clean.indexOf(':') + 1));
            if (def != null) return def;
        }
        for (SeatDefinition s : REGISTRY.values()) {
            if (s.id.equalsIgnoreCase(clean)) {
                return s;
            }
        }
        return getFallback();
    }

    public static SeatDefinition getFallback() {
        SeatDefinition def = REGISTRY.get("standard_seat");
        if (def != null) return def;
        return REGISTRY.values().stream().findFirst().orElseGet(SeatDefinition::new);
    }

    public static List<SeatDefinition> getAll() {
        Set<SeatDefinition> set = new LinkedHashSet<>(REGISTRY.values());
        return new ArrayList<>(set);
    }

    public static void scanPackSeats(File packDir) {
        if (packDir == null || !packDir.isDirectory()) return;
        File seatsDir = new File(packDir, "seats");
        if (!seatsDir.exists() || !seatsDir.isDirectory()) {
            File[] subs = packDir.listFiles(File::isDirectory);
            if (subs != null) {
                for (File s : subs) {
                    if (s.getName().startsWith(".")) continue;
                    File sSeats = new File(s, "seats");
                    if (sSeats.exists() && sSeats.isDirectory()) {
                        scanSeatsDirectory(sSeats, s.getName());
                    }
                }
            }
            return;
        }
        scanSeatsDirectory(seatsDir, packDir.getName());
    }

    private static void scanSeatsDirectory(File seatsDir, String packNamespace) {
        File[] jsonFiles = seatsDir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (jsonFiles == null) return;
        for (File jf : jsonFiles) {
            try {
                String content = java.nio.file.Files.readString(jf.toPath(), StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);
                SeatDefinition def = GSON.fromJson(content, SeatDefinition.class);
                if (def != null) {
                    def.packDirectory = seatsDir.getParentFile();
                    if (def.id == null || def.id.trim().isEmpty()) {
                        def.id = jf.getName().replace(".json", "");
                    }
                    String namespacedId = packNamespace + ":" + def.id;
                    REGISTRY.put(namespacedId.toLowerCase(Locale.ROOT), def);
                    REGISTRY.put(def.id.toLowerCase(Locale.ROOT), def);
                    System.out.println("[CarDecor] Loaded modular seat: " + def.name + " (" + def.id + ") from " + packNamespace);
                }
            } catch (Exception e) {
                System.err.println("[CarDecor] Error parsing seat " + jf.getName() + ": " + e.getMessage());
            }
        }
    }
}
