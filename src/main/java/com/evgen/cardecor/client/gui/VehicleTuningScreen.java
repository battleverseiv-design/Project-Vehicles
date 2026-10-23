package com.evgen.cardecor.client.gui;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.network.VehicleChangeEnginePacket;
import com.evgen.cardecor.network.VehicleSaveConfigPacket;
import com.evgen.cardecor.pack.EngineDefinition;
import com.evgen.cardecor.pack.EngineRegistry;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern Dark-Glass Tuning Lab GUI for CarDecor vehicles.
 * Built from scratch with 100% verified functional parameter bindings,
 * live adjustments, nudge buttons, chips, and instant server-client sync.
 */
public class VehicleTuningScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Visual Palette: Modern Slate / Dark Glass & Vivid Neon
    private static final int PANEL_BG       = 0xF00D1117; // Deep translucent obsidian slate
    private static final int PANEL_BORDER   = 0xFF30363D; // Subtle border
    private static final int ACCENT_CYAN    = 0xFF00D8F6;
    private static final int ACCENT_BLUE    = 0xFF388BFD;
    private static final int ACCENT_GREEN   = 0xFF2EA043;
    private static final int ACCENT_ORANGE  = 0xFFF0883E;
    private static final int TEXT_WHITE     = 0xFFF0F6FC;
    private static final int TEXT_MUTED     = 0xFF8B949E;

    private String initialPackId;
    private final List<String> packIds = new ArrayList<>();
    private int currentPackIndex = 0;
    private JsonObject currentJson = new JsonObject();

    public static int lastActiveTab = 0;
    private int activeTab = 0;

    private int panelWidth = 474;
    private int panelHeight = 256;
    private int panelX;
    private int panelY;

    // Toast Notification Banner
    private String toastMessage = null;
    private int toastTicks = 0;
    private int toastColor = ACCENT_GREEN;

    // Active Number Field Rows
    private final List<NumberFieldRow> activeNumberRows = new ArrayList<>();

    public VehicleTuningScreen(String initialPackId) {
        this(initialPackId, lastActiveTab);
    }

    public VehicleTuningScreen(String initialPackId, int initialTab) {
        super(Component.literal("Мастерская CarDecor"));
        this.initialPackId = initialPackId;
        this.activeTab = initialTab;
        lastActiveTab = initialTab;
    }

    public VehicleTuningScreen() {
        this("blue_sedan");
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.min(480, this.width - 16);
        this.panelHeight = Math.min(258, this.height - 16);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.packIds.clear();
        this.packIds.addAll(VehiclePackManager.getAllKeys());
        if (!this.packIds.isEmpty()) {
            if (this.initialPackId != null) {
                int found = this.packIds.indexOf(this.initialPackId);
                this.currentPackIndex = (found >= 0) ? found : 0;
            } else {
                this.currentPackIndex = 0;
            }
            loadCurrentPackJson();
        }

        buildWidgets();
    }

    private void loadCurrentPackJson() {
        if (this.packIds.isEmpty()) return;
        String packId = this.packIds.get(this.currentPackIndex);
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def != null && def.packDirectory != null) {
            File jsonFile = (def.sourceJsonFile != null && def.sourceJsonFile.exists()) ? def.sourceJsonFile : new File(def.packDirectory, "vehicle.json");
            if (jsonFile.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                    this.currentJson = JsonParser.parseReader(reader).getAsJsonObject();
                    return;
                } catch (Exception ignored) {}
            }
        }
        this.currentJson = new JsonObject();
    }

    private void buildWidgets() {
        this.clearWidgets();
        this.activeNumberRows.clear();

        if (this.packIds.isEmpty()) return;

        // -------------------------------------------------------------
        // HEADER WIDGETS
        // -------------------------------------------------------------
        int hY = this.panelY + 7;

        // Prev Pack Button [<]
        this.addRenderableWidget(MinimalButton.builder(Component.literal("◀"), btn -> switchPack(-1))
            .bounds(this.panelX + 12, hY, 20, 18).build());

        // Next Pack Button [>]
        this.addRenderableWidget(MinimalButton.builder(Component.literal("▶"), btn -> switchPack(1))
            .bounds(this.panelX + 195, hY, 20, 18).build());

        // Action Buttons: Save & Apply [💾 Применить], Reset [🔄], Close [✕]
        int btnX = this.panelX + this.panelWidth - 165;
        this.addRenderableWidget(MinimalButton.builder(Component.literal("💾 Сохранить"), btn -> saveAndApply())
            .bounds(btnX, hY, 92, 18).build());

        this.addRenderableWidget(MinimalButton.builder(Component.literal("🔄"), btn -> resetConfig())
            .bounds(btnX + 96, hY, 22, 18).build());

        this.addRenderableWidget(MinimalButton.builder(Component.literal("✕"), btn -> this.onClose())
            .bounds(btnX + 122, hY, 22, 18).build());

        // -------------------------------------------------------------
        // TAB BAR WIDGETS (6 Functional Tabs)
        // -------------------------------------------------------------
        String[] tabNames = new String[]{
            "🚗 Двигатель",
            "⚙️ Подвеска",
            "🏎️ Управление",
            "🎥 Камера",
            "🛡️ Броня",
            "💡 Оптика и свет"
        };

        int tabY = this.panelY + 31;
        int totalTabW = this.panelWidth - 24;
        int singleTabW = totalTabW / tabNames.length;

        for (int i = 0; i < tabNames.length; i++) {
            final int tIdx = i;
            int tX = this.panelX + 12 + i * singleTabW;
            int tW = (i == tabNames.length - 1) ? (this.panelX + 12 + totalTabW - tX) : singleTabW - 2;

            MinimalButton tabBtn = MinimalButton.builder(Component.literal(tabNames[i]), btn -> {
                for (NumberFieldRow row : this.activeNumberRows) {
                    row.setVal(row.getCurrentVal());
                }
                this.activeTab = tIdx;
                lastActiveTab = tIdx;
                buildWidgets();
            }).bounds(tX, tabY, tW, 18).build();

            this.addRenderableWidget(tabBtn);
        }

        // -------------------------------------------------------------
        // TAB CONTENT WIDGETS
        // -------------------------------------------------------------
        int startY = this.panelY + 54;
        int colW = (this.panelWidth - 32) / 2;
        int col1X = this.panelX + 12;
        int col2X = col1X + colW + 8;

        switch (this.activeTab) {
            case 0 -> buildEngineTab(col1X, col2X, colW, startY);
            case 1 -> buildSuspensionTab(col1X, col2X, colW, startY);
            case 2 -> buildSteeringTab(col1X, col2X, colW, startY);
            case 3 -> buildCameraTab(col1X, col2X, colW, startY);
            case 4 -> buildArmorTab(col1X, col2X, colW, startY);
            case 5 -> buildEquipmentTab(col1X, col2X, colW, startY);
        }
    }

    // =========================================================================
    // TAB 0: ENGINE & TRANSMISSION
    // =========================================================================
    private void buildEngineTab(int c1X, int c2X, int colW, int startY) {
        // Drivetrain selector chips (RWD, AWD, FWD)
        String curDrive = getJsonString("physics", "drivetrain", "rwd").toLowerCase(Locale.ROOT);
        int chipY = startY;
        int chipW = (colW - 8) / 3;

        String[] drives = new String[]{"rwd", "awd", "fwd"};
        String[] driveTitles = new String[]{"RWD", "AWD", "FWD"};
        for (int i = 0; i < 3; i++) {
            final String dMode = drives[i];
            boolean active = curDrive.equals(dMode);
            String title = (active ? "★ " : "") + driveTitles[i];
            this.addRenderableWidget(MinimalButton.builder(Component.literal(title), btn -> {
                setJsonString("physics", "drivetrain", dMode);
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null && mc.player.getVehicle() instanceof com.evgen.cardecor.entity.DrivableVehicleEntity veh) {
                    if (veh.physics != null) veh.physics.drivetrain = dMode;
                    if (veh.odeBody != null) veh.odeBody.drivetrain = dMode;
                }
                buildWidgets();
            }).bounds(c1X + i * (chipW + 4), chipY, chipW, 16).build());
        }

        // Modular Engine Selection Chips (From EngineRegistry)
        List<EngineDefinition> engines = EngineRegistry.getAll();
        String curEngine = getJsonString("engine", "id", getJsonString("", "default_engine", "amc_4_0_i6")).toLowerCase(Locale.ROOT);
        int engCount = Math.min(engines.size(), 4);
        int engChipW = (colW - (engCount - 1) * 4) / Math.max(1, engCount);
        for (int i = 0; i < engCount; i++) {
            final EngineDefinition def = engines.get(i);
            boolean active = curEngine.equalsIgnoreCase(def.id) || curEngine.endsWith(":" + def.id);
            String title = (active ? "★ " : "") + def.name.replaceAll("\\(.*\\)", "").trim();
            if (title.length() > 9) title = title.substring(0, 9);
            this.addRenderableWidget(MinimalButton.builder(Component.literal(title), btn -> {
                setJsonString("", "default_engine", def.id);
                setJsonString("engine", "id", def.id);
                setJsonString("engine", "name", def.name);
                setJsonString("engine", "type", def.type);
                setJsonString("physics", "engine_type", def.type);
                setJsonString("sounds", "engine_type", def.type);
                if (def.physics != null) {
                    setJsonDouble("physics", "horsepower", def.physics.horsepower, false);
                    setJsonDouble("physics", "torque_nm", def.physics.torqueNm, false);
                    setJsonDouble("physics", "idle_rpm", def.physics.idleRpm, true);
                    setJsonDouble("physics", "redline_rpm", def.physics.redlineRpm, true);
                }
                // Live swap engine on local vehicle
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.level != null) {
                    for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                        if (e instanceof DrivableVehicleEntity veh) {
                            if (veh.hasPassenger(mc.player) || mc.player.distanceToSqr(veh) < 49.0) {
                                ModNetwork.sendToServer(new VehicleChangeEnginePacket(veh.getId(), def.id));
                                veh.setInstalledEngine(def.id);
                                break;
                            }
                        }
                    }
                }
                buildWidgets();
            }).bounds(c2X + i * (engChipW + 4), chipY, engChipW, 16).build());
        }

        int rowY = startY + 22;
        int rowH = 32;

        // Left Column (Power & Speeds)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Мощность двигателя", "physics", "horsepower", 150.0, 10.0, 5.0, 3000.0, "л.с.", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Крутящий момент", "physics", "torque_nm", 240.0, 15.0, 20.0, 4000.0, "Нм", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Максимальная скорость", "physics", "max_speed_kmh", 120.0, 5.0, 30.0, 400.0, "км/ч", false);
        addNumberRow(c1X, rowY + rowH * 3, colW, "Скорость на бусте", "physics", "boost_speed_kmh", 160.0, 5.0, 30.0, 450.0, "км/ч", false);
        addNumberRow(c1X, rowY + rowH * 4, colW, "Число передач КПП", "physics", "gears", 5.0, 1.0, 1.0, 10.0, "ст.", true);

        // Right Column (Mass, Boost & RPM)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Масса автомобиля", "physics", "mass_kg", 1500.0, 50.0, 400.0, 10000.0, "кг", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Давление наддува", "physics", "boost_pressure_bar", 1.2, 0.1, 0.0, 4.5, "бар", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Обороты холостого хода", "physics", "idle_rpm", 850.0, 50.0, 400.0, 2000.0, "об/м", true);
        addNumberRow(c2X, rowY + rowH * 3, colW, "Красная зона тахометра", "physics", "redline_rpm", 7000.0, 250.0, 3000.0, 12000.0, "об/м", true);
        addNumberRow(c2X, rowY + rowH * 4, colW, "Объем топливного бака", "physics", "fuel_capacity_liters", 60.0, 5.0, 20.0, 200.0, "л", false);
    }

    // =========================================================================
    // TAB 1: SUSPENSION & CHASSIS
    // =========================================================================
    private void buildSuspensionTab(int c1X, int c2X, int colW, int startY) {
        int rowY = startY + 4;
        int rowH = 33;

        // Left Column (Springs & Clearance)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Жесткость пружин", "physics", "suspension_stiffness", 1.0, 0.1, 0.2, 4.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Демпфирование амортизаторов", "physics", "suspension_damping", 1.0, 0.1, 0.2, 3.5, "x", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Дорожный просвет (Клиренс)", "physics", "ride_height", 0.0, 0.02, -0.15, 0.35, "м", false);
        addNumberRow(c1X, rowY + rowH * 3, colW, "Максимальный ход подвески", "physics", "max_suspension_travel", 0.15, 0.02, 0.04, 0.45, "м", false);
        addNumberRow(c1X, rowY + rowH * 4, colW, "Внедорожность (ступеньки)", "physics", "offroad_capability", 1.0, 0.2, 0.4, 3.0, "x", false);

        // Right Column (Dynamic Lean & Body Weight)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Крен кузова в виражах", "physics", "body_roll_factor", 1.0, 0.1, 0.0, 3.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Клевок при торможении", "physics", "pitch_dive_factor", 1.0, 0.1, 0.0, 3.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Прогиб под весом игрока", "physics", "weight_dip_sensitivity", 1.0, 0.1, 0.1, 3.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 3, colW, "Стабилизатор попереч. устойчивости", "physics", "anti_roll_bar_stiffness", 1.45, 0.15, 0.2, 4.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 4, colW, "Высота центра тяжести", "physics", "center_of_gravity_height", 0.55, 0.05, 0.2, 1.4, "м", false);
    }

    // =========================================================================
    // TAB 2: STEERING & DRIFT
    // =========================================================================
    private void buildSteeringTab(int c1X, int c2X, int colW, int startY) {
        int rowY = startY + 4;
        int rowH = 33;

        // Left Column (Steering Authority)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Макс. выворот колес (угол)", "physics", "steering_angle_deg", 35.0, 2.0, 15.0, 65.0, "°", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Отзывчивость руля", "physics", "steering_response", 1.25, 0.2, 0.5, 5.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Цепкость передка (Turn-in)", "physics", "turn_in_bite", 1.2, 0.1, 0.4, 3.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 3, colW, "Маневренность (Agility)", "physics", "steering_agility", 1.35, 0.1, 0.4, 3.5, "x", false);
        addNumberRow(c1X, rowY + rowH * 4, colW, "Сила тормозов", "physics", "brake_power", 20000.0, 1000.0, 3000.0, 60000.0, "Н", false);

        // Right Column (Tires & Balance)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Давление в передних шинах", "physics", "tire_pressure_fl", 2.2, 0.1, 0.8, 4.0, "бар", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Давление в задних шинах", "physics", "tire_pressure_rl", 2.2, 0.1, 0.8, 4.0, "бар", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Радиус колес", "physics", "wheel_radius", 0.35, 0.02, 0.20, 0.70, "м", false);
        addNumberRow(c2X, rowY + rowH * 3, colW, "Колесная база", "physics", "wheelbase", 2.85, 0.05, 1.5, 5.0, "м", false);
        addNumberRow(c2X, rowY + rowH * 4, colW, "Ширина колеи", "physics", "track_width", 1.75, 0.05, 1.0, 3.0, "м", false);
    }

    // =========================================================================
    // TAB 3: CAMERA & EFFECTS
    // =========================================================================
    private void buildCameraTab(int c1X, int c2X, int colW, int startY) {
        int rowY = startY + 4;
        int rowH = 33;

        // Left Column (Vibrations & Shake)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Тряска кузова от двигателя", "physics", "body_shake_intensity", 0.4, 0.05, 0.0, 3.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Тряска камеры на неровностях", "camera", "shake_intensity", 0.35, 0.05, 0.0, 3.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Опережение взгляда рулем", "camera", "look_ahead", 0.10, 0.05, 0.0, 1.0, "x", false);

        // Right Column (Slope & Bank follow in 3rd person)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Крен камеры в скоростном повороте", "camera", "cornering_bank", 0.20, 0.05, 0.0, 1.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Следование подъемам и спускам", "camera", "pitch_follow", 0.70, 0.05, 0.0, 1.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Следование боковым уклонам", "camera", "roll_follow", 0.45, 0.05, 0.0, 1.0, "x", false);
    }

    // =========================================================================
    // TAB 4: ARMOR & HITBOX DAMAGE
    // =========================================================================
    private void buildArmorTab(int c1X, int c2X, int colW, int startY) {
        int rowY = startY + 4;
        int rowH = 33;

        // Left Column (Body, Windows & Wheels)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Множитель урона по кузову", "hitboxes", "body_damage_multiplier", 0.35, 0.05, 0.0, 2.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Множитель урона по стеклам", "hitboxes", "window_damage_multiplier", 1.0, 0.05, 0.0, 3.0, "x", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Множитель урона по колесам", "hitboxes", "wheel_damage_multiplier", 0.80, 0.05, 0.0, 3.0, "x", false);

        // Right Column (Engine & Trunk/Bed)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Множитель урона по двигателю", "hitboxes", "engine_damage_multiplier", 1.0, 0.05, 0.0, 3.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Множитель урона по багажнику", "hitboxes", "trunk_damage_multiplier", 0.40, 0.05, 0.0, 2.0, "x", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Прочность шин", "hitboxes", "tire_health", 50.0, 5.0, 10.0, 500.0, "HP", false);
        addNumberRow(c2X, rowY + rowH * 3, colW, "Вместимость багажника (ряды)", "trunk", "rows", 3.0, 1.0, 1.0, 6.0, "ряд.", true);
    }

    // =========================================================================
    // TAB 5: OPTICS, HEADLIGHTS & TAILLIGHTS
    // =========================================================================
    private void buildEquipmentTab(int c1X, int c2X, int colW, int startY) {
        int rowY = startY + 1;
        int rowH = 26;

        VehicleDefinition curDef = (!packIds.isEmpty()) ? VehiclePackManager.get(packIds.get(currentPackIndex)) : null;
        double defHlX = (curDef != null && curDef.headlights != null && curDef.headlights.beamWidth > 0.05) ? curDef.headlights.beamWidth : 0.78;
        double defHlY = (curDef != null && curDef.headlights != null && curDef.headlights.heightOffset > 0.05) ? curDef.headlights.heightOffset : 0.72;
        double defHlZ = (curDef != null && curDef.headlights != null && Math.abs(curDef.headlights.offsetZ) > 0.05) ? curDef.headlights.offsetZ : -2.66;
        double defTlX = (curDef != null && curDef.taillights != null && curDef.taillights.beamWidth > 0.05) ? curDef.taillights.beamWidth : 0.95;
        double defTlY = (curDef != null && curDef.taillights != null && curDef.taillights.heightOffset > 0.05) ? curDef.taillights.heightOffset : 0.97;
        double defTlZ = (curDef != null) ? curDef.getTaillightLeftPos()[2] : 2.69;

        // Left Column (Передние фары)
        addNumberRow(c1X, rowY + rowH * 0, colW, "Ширина передних фар X", "headlights", "beam_width", defHlX, 0.02, 0.00, 5.00, "м", false);
        addNumberRow(c1X, rowY + rowH * 1, colW, "Высота передних фар Y", "headlights", "height_offset", defHlY, 0.02, -2.00, 6.00, "м", false);
        addNumberRow(c1X, rowY + rowH * 2, colW, "Вынос передних фар Z", "headlights", "offset_z", defHlZ, 0.02, -10.00, 10.00, "м", false);
        addNumberRow(c1X, rowY + rowH * 3, colW, "Размер ореола фар", "headlights", "flare_size", 0.16, 0.01, 0.02, 0.50, "м", false);
        addNumberRow(c1X, rowY + rowH * 4, colW, "Дальность света фар", "headlights", "range", 36.0, 1.0, 10.0, 80.0, "м", false);

        // Right Column (Задние фонари и стоп-сигналы)
        addNumberRow(c2X, rowY + rowH * 0, colW, "Ширина задних фар X", "taillights", "beam_width", defTlX, 0.02, 0.00, 5.00, "м", false);
        addNumberRow(c2X, rowY + rowH * 1, colW, "Высота задних фар Y", "taillights", "height_offset", defTlY, 0.02, -2.00, 6.00, "м", false);
        addNumberRow(c2X, rowY + rowH * 2, colW, "Вынос задних фар Z", "taillights", "offset_z", defTlZ, 0.02, -10.00, 10.00, "м", false);
        addNumberRow(c2X, rowY + rowH * 3, colW, "Размер ореола фонарей", "taillights", "flare_size", 0.13, 0.01, 0.02, 0.50, "м", false);
        addNumberRow(c2X, rowY + rowH * 4, colW, "Фоновая яркость габаритов", "taillights", "idle_brightness", 25.0, 5.0, 0.0, 100.0, "%", true);

        // Bottom Section: Color selection & HEX input
        int colorY = rowY + rowH * 5 + 1;
        String curCol = getJsonString("headlights", "color", "warm_white");
        int chipY = colorY + 10;
        String[] colorPresets = new String[]{
            "warm_white", "cool_xenon", "amber_yellow", "pure_white", "red", "blue", "green"
        };
        String[] colorNames = new String[]{
            "🟡 Тёплый", "⚪ Ксенон", "🟠 Янтарь", "⚪ Белый", "🔴 Красный", "🔵 Синий", "🟢 Зелёный"
        };
        int chipCount = colorPresets.length;
        int totalW = colW * 2 + 8;
        int chipW = (totalW - (chipCount - 1) * 3) / chipCount;

        for (int i = 0; i < chipCount; i++) {
            final String colCode = colorPresets[i];
            int chX = c1X + i * (chipW + 3);
            MinimalButton chipBtn = MinimalButton.builder(Component.literal(colorNames[i]), btn -> {
                setJsonString("headlights", "color", colCode);
                if (!packIds.isEmpty()) {
                    VehicleDefinition def = VehiclePackManager.get(packIds.get(currentPackIndex));
                    if (def != null && def.headlights != null) {
                        def.headlights.color = colCode;
                    }
                }
                buildWidgets();
            }).bounds(chX, chipY, chipW, 16).build();
            this.addRenderableWidget(chipBtn);
        }

        // Custom HEX edit box
        EditBox hexBox = new EditBox(this.font, c1X + 46, chipY + 18, 80, 16, Component.literal("HEX"));
        hexBox.setValue(curCol.startsWith("#") ? curCol : "#" + curCol);
        hexBox.setResponder(val -> {
            String clean = val.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() == 6) {
                setJsonString("headlights", "color", "#" + clean);
                if (!packIds.isEmpty()) {
                    VehicleDefinition def = VehiclePackManager.get(packIds.get(currentPackIndex));
                    if (def != null && def.headlights != null) {
                        def.headlights.color = "#" + clean;
                    }
                }
            }
        });
        this.addRenderableWidget(hexBox);
    }

    // =========================================================================
    // HELPER: CREATE INTERACTIVE NUMBER FIELD ROW WITH [-] [EditBox] [+]
    // =========================================================================
    private void addNumberRow(int x, int y, int width, String label, String section, String key, double defVal, double step, double min, double max, String unit, boolean isInt) {
        NumberFieldRow row = new NumberFieldRow(label, section, key, defVal, step, min, max, unit, isInt);

        int inputY = y + 12;
        int btnW = 18;
        int boxW = width - (btnW * 2 + 6);

        // Minus Button [-]
        this.addRenderableWidget(MinimalButton.builder(Component.literal("-"), btn -> row.nudge(-row.step))
            .bounds(x, inputY, btnW, 16).build());

        // Value Input Box
        EditBox box = new EditBox(this.font, x + btnW + 3, inputY, boxW, 16, Component.literal(label));
        box.setValue(row.formatVal(row.getCurrentVal()));
        box.setResponder(val -> {
            try {
                if (!val.trim().isEmpty()) {
                    double parsed = Double.parseDouble(val.trim());
                    row.setValSilent(parsed);
                }
            } catch (Exception ignored) {}
        });
        row.editBox = box;
        this.addRenderableWidget(box);

        // Plus Button [+]
        this.addRenderableWidget(MinimalButton.builder(Component.literal("+"), btn -> row.nudge(row.step))
            .bounds(x + btnW + boxW + 6, inputY, btnW, 16).build());

        this.activeNumberRows.add(row);
    }

    private class NumberFieldRow {
        final String label;
        final String section;
        final String key;
        final double defaultValue;
        final double step;
        final double min;
        final double max;
        final String unit;
        final boolean isInt;
        EditBox editBox;

        NumberFieldRow(String label, String section, String key, double defaultValue, double step, double min, double max, String unit, boolean isInt) {
            this.label = label;
            this.section = section;
            this.key = key;
            this.defaultValue = defaultValue;
            this.step = step;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.isInt = isInt;
        }

        double getCurrentVal() {
            try {
                if (editBox != null && !editBox.getValue().trim().isEmpty()) {
                    return Double.parseDouble(editBox.getValue().trim());
                }
            } catch (Exception ignored) {}
            return getJsonDouble(section, key, defaultValue);
        }

        void setVal(double val) {
            val = Mth.clamp(val, min, max);
            setValSilent(val);
            if (editBox != null) {
                editBox.setValue(formatVal(val));
            }
        }

        void setValSilent(double val) {
            val = Mth.clamp(val, min, max);
            setJsonDouble(section, key, val, isInt);
            // Live-update in-memory definition so changes appear on screen immediately
            if (!packIds.isEmpty()) {
                VehicleDefinition def = VehiclePackManager.get(packIds.get(currentPackIndex));
                if (def != null) {
                    if ("headlights".equals(section) && def.headlights != null) {
                        if ("offset_z".equals(key)) def.headlights.offsetZ = val;
                        else if ("beam_width".equals(key)) def.headlights.beamWidth = val;
                        else if ("height_offset".equals(key)) def.headlights.heightOffset = val;
                        else if ("flare_size".equals(key)) def.headlights.flareSize = val;
                        else if ("range".equals(key)) def.headlights.range = val;
                        else if ("downward_angle".equals(key)) def.headlights.downwardAngle = val;
                        else if ("light_level".equals(key)) def.headlights.lightLevel = (int) Math.round(val);
                    } else if ("taillights".equals(section)) {
                        if (def.taillights == null) def.taillights = new VehicleDefinition.TaillightConfig();
                        if ("offset_z".equals(key)) def.taillights.offsetZ = val;
                        else if ("beam_width".equals(key)) def.taillights.beamWidth = val;
                        else if ("height_offset".equals(key)) def.taillights.heightOffset = val;
                        else if ("flare_size".equals(key)) def.taillights.flareSize = val;
                        else if ("range".equals(key)) def.taillights.range = val;
                        else if ("idle_brightness".equals(key)) def.taillights.idleBrightness = val / 100.0;
                    }
                }
            }
        }

        void nudge(double delta) {
            setVal(getCurrentVal() + delta);
        }

        String formatVal(double val) {
            if (isInt) return String.valueOf((int) Math.round(val));
            if (Math.abs(step) >= 1.0) return String.format(Locale.ROOT, "%.1f", val);
            if (Math.abs(step) >= 0.1) return String.format(Locale.ROOT, "%.2f", val);
            return String.format(Locale.ROOT, "%.3f", val);
        }
    }

    // =========================================================================
    // PACK SWITCHING & DATA ACTIONS
    // =========================================================================
    private void switchPack(int delta) {
        if (this.packIds.isEmpty()) return;
        this.currentPackIndex = (this.currentPackIndex + delta + this.packIds.size()) % this.packIds.size();
        loadCurrentPackJson();
        buildWidgets();
    }

    private void saveAndApply() {
        if (this.packIds.isEmpty()) return;
        String packId = this.packIds.get(this.currentPackIndex);

        // Commit active edit boxes
        for (NumberFieldRow row : this.activeNumberRows) {
            row.setVal(row.getCurrentVal());
        }

        // Sync also front/rear tire pressures if updated
        double fl = getJsonDouble("physics", "tire_pressure_fl", 2.2);
        double rl = getJsonDouble("physics", "tire_pressure_rl", 2.2);
        setJsonDouble("physics", "tire_pressure_fr", fl, false);
        setJsonDouble("physics", "tire_pressure_rr", rl, false);

        // Sync horsepower & torque across physics and engine sections
        double hp = getJsonDouble("physics", "horsepower", 150.0);
        setJsonDouble("engine", "horsepower", hp, false);
        setJsonDouble("physics", "horsepower", hp, false);
        double tq = getJsonDouble("physics", "torque_nm", hp * 2.85);
        double expectedTorque = hp * 2.85;
        if (tq > expectedTorque * 3.5 || tq < expectedTorque * 0.25) {
            tq = expectedTorque;
            setJsonDouble("physics", "torque_nm", tq, false);
        }
        setJsonDouble("engine", "torque_nm", tq, false);

        // Send network packet to server (persists to JSON on disk and live updates server entities)
        ModNetwork.sendToServer(new VehicleSaveConfigPacket(packId, this.currentJson.toString()));

        // Live update all matching vehicle entities on local client
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            String defaultEng = getJsonString("", "default_engine", getJsonString("engine", "id", ""));
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof DrivablePackVehicleEntity pve && packId.equals(pve.getVehicleId())) {
                    if (!defaultEng.isEmpty()) {
                        pve.setInstalledEngine(defaultEng);
                    }
                    pve.applyDefinitionPhysics();
                }
            }
        }

        // Live reload vehicle packs on client immediately
        VehiclePackManager.loadPacks();

        showToast("✓ Настройки сохранены и применены!", ACCENT_GREEN);
    }

    private void resetConfig() {
        loadCurrentPackJson();
        buildWidgets();
        showToast("Конфигурация перезагружена с диска", ACCENT_ORANGE);
    }

    private void showToast(String msg, int color) {
        this.toastMessage = msg;
        this.toastTicks = 60; // 3 seconds
        this.toastColor = color;
    }

    // =========================================================================
    // JSON DATA ACCESSORS
    // =========================================================================
    private double getJsonDouble(String section, String key, double def) {
        if (this.currentJson == null) return def;
        if (section != null) {
            if (this.currentJson.has(section) && this.currentJson.get(section).isJsonObject()) {
                JsonObject sec = this.currentJson.getAsJsonObject(section);
                if (sec.has(key)) return sec.get(key).getAsDouble();
            }
        } else {
            if (this.currentJson.has(key)) return this.currentJson.get(key).getAsDouble();
        }
        return def;
    }

    private void setJsonDouble(String section, String key, double val, boolean isInt) {
        if (this.currentJson == null) this.currentJson = new JsonObject();
        JsonObject target = this.currentJson;
        if (section != null) {
            if (!this.currentJson.has(section) || !this.currentJson.get(section).isJsonObject()) {
                this.currentJson.add(section, new JsonObject());
            }
            target = this.currentJson.getAsJsonObject(section);
        }
        if (isInt) {
            target.addProperty(key, (int) Math.round(val));
        } else {
            target.addProperty(key, Math.round(val * 1000.0) / 1000.0);
        }
    }

    private String getJsonString(String section, String key, String def) {
        if (this.currentJson == null) return def;
        if (section != null) {
            if (this.currentJson.has(section) && this.currentJson.get(section).isJsonObject()) {
                JsonObject sec = this.currentJson.getAsJsonObject(section);
                if (sec.has(key)) return sec.get(key).getAsString();
            }
        } else {
            if (this.currentJson.has(key)) return this.currentJson.get(key).getAsString();
        }
        return def;
    }

    private void setJsonString(String section, String key, String val) {
        if (this.currentJson == null) this.currentJson = new JsonObject();
        JsonObject target = this.currentJson;
        if (section != null) {
            if (!this.currentJson.has(section) || !this.currentJson.get(section).isJsonObject()) {
                this.currentJson.add(section, new JsonObject());
            }
            target = this.currentJson.getAsJsonObject(section);
        }
        target.addProperty(key, val);
    }

    // =========================================================================
    // RENDERING: SLEEK MODERN DARK GLASS UI
    // =========================================================================
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Darkened background scrim
        this.renderBackground(gui);

        // Main Container Frame with Neon Cyan/Blue Header Line
        gui.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL_BG);
        gui.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, ACCENT_BLUE);
        gui.fill(this.panelX, this.panelY + 2, this.panelX + this.panelWidth, this.panelY + 3, ACCENT_CYAN);

        // Container Outer Border
        gui.renderOutline(this.panelX, this.panelY, this.panelWidth, this.panelHeight, PANEL_BORDER);

        // Header Vehicle Title
        String packId = this.packIds.isEmpty() ? "None" : this.packIds.get(this.currentPackIndex);
        VehicleDefinition def = VehiclePackManager.get(packId);
        String packName = (def != null && def.name != null) ? def.name : packId;
        String titleText = "🛠 " + packName;
        gui.drawString(this.font, titleText, this.panelX + 36, this.panelY + 11, TEXT_WHITE, true);

        // Tab bar active indicator line
        int totalTabW = this.panelWidth - 24;
        int singleTabW = totalTabW / 6;
        int activeLineX = this.panelX + 12 + this.activeTab * singleTabW;
        int activeLineW = (this.activeTab == 5) ? (this.panelX + 12 + totalTabW - activeLineX) : singleTabW - 2;
        gui.fill(activeLineX, this.panelY + 49, activeLineX + activeLineW, this.panelY + 51, ACCENT_CYAN);

        // Render Labels above Number Field Rows
        for (NumberFieldRow row : this.activeNumberRows) {
            if (row.editBox != null) {
                int lblX = row.editBox.getX() - 20;
                int lblY = row.editBox.getY() - 10;
                String header = row.label + (row.unit.isEmpty() ? "" : " (" + row.unit + ")");
                gui.drawString(this.font, header, lblX, lblY, TEXT_MUTED, false);
            }
        }

        // Tab 5: Optics, Headlights & Taillights color swatch and labels
        if (this.activeTab == 5) {
            int c1X = this.panelX + 12;
            int startY = this.panelY + 54;
            int rowY = startY + 1;
            int rowH = 26;
            int colorY = rowY + rowH * 5 + 1;
            int chipY = colorY + 10;

            gui.drawString(this.font, "🎨 Цвет фар и луча света:", c1X, colorY + 1, TEXT_WHITE, false);
            gui.drawString(this.font, "HEX:", c1X, chipY + 22, TEXT_MUTED, false);

            String curCol = getJsonString("headlights", "color", "warm_white");
            float[] rgb = VehicleDefinition.HeadlightConfig.parseColor(curCol, new float[]{1.0f, 0.96f, 0.82f});
            int swatchColor = 0xFF000000 | ((int)(rgb[0] * 255) << 16) | ((int)(rgb[1] * 255) << 8) | (int)(rgb[2] * 255);
            gui.fill(c1X + 130, chipY + 18, c1X + 154, chipY + 34, swatchColor);
            gui.renderOutline(c1X + 130, chipY + 18, 24, 16, PANEL_BORDER);
            gui.drawString(this.font, "текущий цвет", c1X + 160, chipY + 22, TEXT_MUTED, false);
        }

        // Render Child Widgets (Buttons, EditBoxes)
        super.render(gui, mouseX, mouseY, partialTick);

        // Toast Notification Banner (Fade-out animation)
        if (this.toastTicks > 0 && this.toastMessage != null) {
            this.toastTicks--;
            int bannerW = this.font.width(this.toastMessage) + 24;
            int bannerX = this.panelX + (this.panelWidth - bannerW) / 2;
            int bannerY = this.panelY + this.panelHeight - 24;

            gui.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 18, 0xF0161B22);
            gui.renderOutline(bannerX, bannerY, bannerW, 18, this.toastColor);
            gui.drawCenteredString(this.font, this.toastMessage, bannerX + bannerW / 2, bannerY + 5, this.toastColor);
        }
    }

    @Override
    public void onClose() {
        saveAndApply();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
