package com.evgen.cardecor.client.gui;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.network.VehicleRefillFluidPacket;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class VehicleMechanicsScreen extends Screen {

    private final DrivableVehicleEntity vehicle;

    // Sections
    public static final int SECTION_ENGINE        = 0;
    public static final int SECTION_CABIN         = 1;
    public static final int SECTION_WHEELS        = 2;
    public static final int SECTION_BODY          = 3;
    public static final int SECTION_FUEL_EXHAUST  = 4;

    private int selectedSection = SECTION_ENGINE;
    private int selectedWheel = 0; // 0=FL, 1=FR, 2=RL, 3=RR

    // Palette (Minimalist Blueprint Aesthetic)
    private static final int COLOR_OVERLAY     = 0xC006090D;
    private static final int PANEL_BG          = 0xF80B0F15;
    private static final int PANEL_BORDER      = 0xFF2A3441;
    private static final int CARD_BG           = 0xD8121822;
    private static final int CARD_BORDER       = 0xFF1D2633;

    private static final int ACCENT_GOLD       = 0xFFE5A93C;
    private static final int ACCENT_GREEN      = 0xFF38B264;
    private static final int ACCENT_RED        = 0xFFE5534B;
    private static final int ACCENT_BLUE       = 0xFF58A6FF;
    private static final int ACCENT_CYAN       = 0xFF00D8F6;
    private static final int TEXT_WHITE        = 0xFFF0F6FC;
    private static final int TEXT_MUTED        = 0xFF8B949E;

    private int panelWidth = 470;
    private int panelHeight = 252;
    private int panelX;
    private int panelY;

    // Interactive Buttons
    private MinimalButton btnEngineMount;
    private MinimalButton btnRefillOil;
    private MinimalButton btnRefillCoolant;
    private MinimalButton btnWheelFL;
    private MinimalButton btnWheelFR;
    private MinimalButton btnWheelRL;
    private MinimalButton btnWheelRR;

    public VehicleMechanicsScreen(DrivableVehicleEntity vehicle) {
        super(Component.literal("Механика транспорта"));
        this.vehicle = vehicle;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        int rightX = panelX + 192;
        int detailsY = panelY + 47;

        // --- Action Buttons for SECTION_ENGINE ---
        btnEngineMount = new MinimalButton(
            rightX + 180, detailsY + 12, 76, 14,
            Component.literal(vehicle.hasEngine() ? "Снять" : "Установить"),
            btn -> {
                int action = vehicle.hasEngine() ? com.evgen.cardecor.network.VehicleEngineActionPacket.ACTION_DISMOUNT : com.evgen.cardecor.network.VehicleEngineActionPacket.ACTION_INSTALL;
                ModNetwork.sendToServer(new com.evgen.cardecor.network.VehicleEngineActionPacket(vehicle.getId(), action));
            }
        );
        addRenderableWidget(btnEngineMount);

        btnRefillOil = new MinimalButton(
            rightX + 196, detailsY + 84, 60, 14,
            Component.literal("Залить"),
            btn -> ModNetwork.sendToServer(new VehicleRefillFluidPacket(vehicle.getId(), VehicleRefillFluidPacket.TYPE_OIL))
        );
        addRenderableWidget(btnRefillOil);

        btnRefillCoolant = new MinimalButton(
            rightX + 196, detailsY + 108, 60, 14,
            Component.literal("Залить"),
            btn -> ModNetwork.sendToServer(new VehicleRefillFluidPacket(vehicle.getId(), VehicleRefillFluidPacket.TYPE_COOLANT))
        );
        addRenderableWidget(btnRefillCoolant);

        // --- Wheel selector buttons for SECTION_WHEELS ---
        int wheelBtnW = 58;
        int wheelBtnGap = 4;
        btnWheelFL = new MinimalButton(rightX + 12 + 0 * (wheelBtnW + wheelBtnGap), detailsY + 12, wheelBtnW, 14, Component.literal("П. Лев"), btn -> { selectedWheel = 0; updateButtonState(); });
        btnWheelFR = new MinimalButton(rightX + 12 + 1 * (wheelBtnW + wheelBtnGap), detailsY + 12, wheelBtnW, 14, Component.literal("П. Прав"), btn -> { selectedWheel = 1; updateButtonState(); });
        btnWheelRL = new MinimalButton(rightX + 12 + 2 * (wheelBtnW + wheelBtnGap), detailsY + 12, wheelBtnW, 14, Component.literal("З. Лев"), btn -> { selectedWheel = 2; updateButtonState(); });
        btnWheelRR = new MinimalButton(rightX + 12 + 3 * (wheelBtnW + wheelBtnGap), detailsY + 12, wheelBtnW, 14, Component.literal("З. Прав"), btn -> { selectedWheel = 3; updateButtonState(); });
        addRenderableWidget(btnWheelFL);
        addRenderableWidget(btnWheelFR);
        addRenderableWidget(btnWheelRL);
        addRenderableWidget(btnWheelRR);

        // --- Close Button [✕] in Title Bar ---
        addRenderableWidget(new MinimalButton(
            panelX + panelWidth - 22, panelY + 6, 16, 14,
            Component.literal("✕"),
            btn -> this.onClose()
        ));

        updateButtonState();
    }

    private void updateButtonState() {
        boolean isEng = (selectedSection == SECTION_ENGINE);
        btnRefillOil.visible = isEng && vehicle.hasEngine();
        btnRefillCoolant.visible = isEng;
        btnEngineMount.visible = isEng;
        btnEngineMount.setMessage(Component.literal(vehicle.hasEngine() ? "Снять" : "Установить"));

        boolean isWheels = (selectedSection == SECTION_WHEELS);
        btnWheelFL.visible = isWheels;
        btnWheelFR.visible = isWheels;
        btnWheelRL.visible = isWheels;
        btnWheelRR.visible = isWheels;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click
            int cardX = panelX + 10;
            int cardY = panelY + 28;
            int carX = cardX + 53;
            int carY = cardY + 26;

            // 1. Check Wheels Click
            if (isInRect(mouseX, mouseY, carX - 14, carY + 12, 12, 28)) { // FL
                selectedSection = SECTION_WHEELS; selectedWheel = 0; updateButtonState(); return true;
            }
            if (isInRect(mouseX, mouseY, carX + 66, carY + 12, 12, 28)) { // FR
                selectedSection = SECTION_WHEELS; selectedWheel = 1; updateButtonState(); return true;
            }
            if (isInRect(mouseX, mouseY, carX - 14, carY + 100, 12, 28)) { // RL
                selectedSection = SECTION_WHEELS; selectedWheel = 2; updateButtonState(); return true;
            }
            if (isInRect(mouseX, mouseY, carX + 66, carY + 100, 12, 28)) { // RR
                selectedSection = SECTION_WHEELS; selectedWheel = 3; updateButtonState(); return true;
            }

            // 2. Check Hood / Engine Click
            if (isInRect(mouseX, mouseY, carX, carY + 6, 64, 38)) {
                selectedSection = SECTION_ENGINE;
                updateButtonState();
                return true;
            }
            // 3. Check Cabin Click
            if (isInRect(mouseX, mouseY, carX, carY + 54, 64, 44)) {
                selectedSection = SECTION_CABIN;
                updateButtonState();
                return true;
            }
            // 4. Check Fuel Tank Click
            if (isInRect(mouseX, mouseY, carX, carY + 106, 64, 18)) {
                selectedSection = SECTION_FUEL_EXHAUST;
                updateButtonState();
                return true;
            }
            // 5. Check Trunk / Body Click
            if (isInRect(mouseX, mouseY, carX, carY + 124, 64, 20)) {
                selectedSection = SECTION_BODY;
                updateButtonState();
                return true;
            }

            // Quick Nav tabs on the right header (row panelY + 28)
            int tabY = panelY + 28;
            int tx = panelX + 192;
            int[] tabWidths = {54, 48, 52, 50, 52};
            int curX = tx + 4;
            for (int i = 0; i < tabWidths.length; i++) {
                if (mouseY >= tabY && mouseY <= tabY + 15 && mouseX >= curX && mouseX < curX + tabWidths[i]) {
                    selectedSection = i;
                    updateButtonState();
                    return true;
                }
                curX += tabWidths[i] + 2;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // Dim background
        g.fillGradient(0, 0, this.width, this.height, COLOR_OVERLAY, COLOR_OVERLAY);

        // Main Window Panel
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        drawBoxBorder(g, panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);

        // Title Row: Vehicle name directly without redundant [ АВТОМЕХАНИКА ]
        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
        String carName = (def != null && def.name != null) ? def.name : vehicle.getVehicleId();
        g.drawString(this.font, "§f" + carName, panelX + 12, panelY + 9, TEXT_WHITE, false);

        // Overall Condition
        float overall = calculateOverallCondition();
        String overallLabel = String.format("Общее состояние: %.0f%%", overall);
        int labelW = this.font.width(overallLabel);
        int overallX = panelX + panelWidth - 32 - 60 - labelW;
        g.drawString(this.font, overallLabel, overallX, panelY + 9, getZoneColor(overall), false);
        drawMiniProgressBar(g, overallX + labelW + 6, panelY + 10, 50, 6, overall / 100.0F, getZoneColor(overall));

        // Separator below title
        g.fill(panelX + 10, panelY + 23, panelX + panelWidth - 10, panelY + 24, 0x4030363D);

        // Render Left Panel: Technical Car Blueprint (Clean, no header text)
        renderCarBlueprint(g, mouseX, mouseY, def);

        // Render Right Panel: Tabs and Active Section Details
        renderRightPanel(g, mouseX, mouseY);

        // Render widgets (buttons)
        super.render(g, mouseX, mouseY, partialTicks);

        // Render Tooltip on hover over car schematic
        renderBlueprintTooltip(g, mouseX, mouseY);
    }

    private float calculateOverallCondition() {
        float sum = 0.0F;
        sum += vehicle.getEngineCondition();
        sum += vehicle.getBatteryCharge();
        sum += (vehicle.getFuel() / DrivableVehicleEntity.MAX_FUEL) * 100.0F;
        sum += vehicle.getMufflerHealth();
        sum += vehicle.getBumperHealth();
        sum += vehicle.getHoodHealth();
        sum += vehicle.getRadiatorHealth();
        sum += (vehicle.getVehicleHealth() / DrivableVehicleEntity.MAX_HP) * 100.0F;
        for (int i = 0; i < 4; i++) {
            sum += vehicle.getTireCondition(i);
        }
        return sum / 12.0F;
    }

    private void renderCarBlueprint(GuiGraphics g, int mouseX, int mouseY, VehicleDefinition def) {
        int cardX = panelX + 10;
        int cardY = panelY + 28;
        int cardW = 175;
        int cardH = 214;

        // Clean Blueprint Box without redundant "ЧЕРТЁЖ" header
        drawSimpleBox(g, cardX, cardY, cardW, cardH);

        int carX = cardX + 53;
        int carY = cardY + 34;

        boolean isPickup = (def != null && ((def.name != null && def.name.toLowerCase().contains("пикап"))
            || (def.id != null && def.id.toLowerCase().contains("pickup"))
            || (def.seats != null && def.seats.size() <= 2)));

        // --- Blueprint Grid Background inside Chassis ---
        g.fill(carX, carY, carX + 64, carY + 146, 0xFF0D1219);
        drawBoxBorder(g, carX, carY, 64, 146, 0xFF273547);

        // Front Bumper with Headlights
        g.fill(carX + 4, carY + 1, carX + 60, carY + 6, 0xFF17202D);
        drawBoxBorder(g, carX + 4, carY + 1, 56, 5, 0xFF35445C);
        // Headlights (Warm Yellow)
        g.fill(carX + 6, carY + 2, carX + 14, carY + 5, 0xFFFFF176);
        g.fill(carX + 50, carY + 2, carX + 58, carY + 5, 0xFFFFF176);
        // Radiator Grille ribs
        g.fill(carX + 20, carY + 3, carX + 44, carY + 4, 0xFF4A5568);

        // --- 1. Hood / Engine Bay Section ---
        boolean hoverHood = isInRect(mouseX, mouseY, carX, carY + 6, 64, 38);
        boolean selHood = (selectedSection == SECTION_ENGINE);
        int hoodCol = vehicle.hasEngine() ? getZoneColor(vehicle.getEngineCondition()) : 0xFF7A1C1C;
        renderBlueprintZone(g, carX, carY + 6, 64, 38, hoodCol, hoverHood, selHood);
        // Schematic Engine Block & Radiator details inside hood
        g.fill(carX + 16, carY + 8, carX + 48, carY + 11, 0xFF2A384C); // Radiator top
        if (vehicle.hasEngine()) {
            g.fill(carX + 22, carY + 16, carX + 42, carY + 36, 0xFF1C2737); // Engine Block
            drawBoxBorder(g, carX + 22, carY + 16, 20, 20, 0xFF3B4E68);
            g.fill(carX + 26, carY + 19, carX + 38, carY + 33, 0xFF2E3D52); // Intake Manifold
        } else {
            drawBoxBorder(g, carX + 22, carY + 16, 20, 20, 0x403B4E68);
            g.drawCenteredString(this.font, "§8✕", carX + 32, carY + 22, 0xFF58687E);
        }
        // Battery block in corner
        int batCol = getZoneColor(vehicle.getBatteryCharge());
        g.fill(carX + 46, carY + 14, carX + 58, carY + 23, 0xFF151E2B);
        drawBoxBorder(g, carX + 46, carY + 14, 12, 9, batCol);
        g.fill(carX + 48, carY + 16, carX + 50, carY + 18, ACCENT_RED); // + terminal

        // --- Windshield ---
        g.fill(carX + 3, carY + 44, carX + 61, carY + 54, 0xFF15263F);
        drawBoxBorder(g, carX + 3, carY + 44, 58, 10, 0xFF27436B);
        // Glare line
        g.fill(carX + 10, carY + 46, carX + 24, carY + 47, 0x8058A6FF);
        g.fill(carX + 20, carY + 49, carX + 42, carY + 50, 0x4058A6FF);

        // --- 2. Cabin / Interior Section ---
        boolean hoverCabin = isInRect(mouseX, mouseY, carX, carY + 54, 64, 44);
        boolean selCabin = (selectedSection == SECTION_CABIN);
        int cabinCol = vehicle.isKeyInIgnition() ? ACCENT_GREEN : ACCENT_BLUE;
        renderBlueprintZone(g, carX, carY + 54, 64, 44, cabinCol, hoverCabin, selCabin);

        // Steering wheel
        g.fill(carX + 14, carY + 58, carX + 22, carY + 66, 0xFF27384E);
        drawBoxBorder(g, carX + 14, carY + 58, 8, 8, 0xFF4F6889);
        // Front bucket seats (Driver & Passenger)
        drawBlueprintSeat(g, carX + 10, carY + 62, 18, 16);
        drawBlueprintSeat(g, carX + 36, carY + 62, 18, 16);

        if (isPickup) {
            // PICKUP: Rear bulkhead and open Cargo Bed!
            g.fill(carX + 2, carY + 80, carX + 62, carY + 82, 0xFF35445C); // Cabin back wall
            // Cargo bed interior (ribbed truck bed lines)
            g.fill(carX + 4, carY + 83, carX + 60, carY + 138, 0xFF121822);
            drawBoxBorder(g, carX + 4, carY + 83, 56, 55, 0xFF2E3E54);
            // Bed ribs
            for (int ry = carY + 88; ry < carY + 135; ry += 7) {
                g.fill(carX + 8, ry, carX + 56, ry + 1, 0xFF1E2838);
            }
        } else {
            // SEDAN: Rear seats & Rear window
            drawBlueprintSeat(g, carX + 10, carY + 82, 18, 12);
            drawBlueprintSeat(g, carX + 36, carY + 82, 18, 12);

            // Rear Window
            g.fill(carX + 4, carY + 98, carX + 60, carY + 106, 0xFF15263F);
            drawBoxBorder(g, carX + 4, carY + 98, 56, 8, 0xFF27436B);
            // Defroster lines
            g.fill(carX + 10, carY + 101, carX + 54, carY + 102, 0x40E5A93C);
            g.fill(carX + 10, carY + 103, carX + 54, carY + 104, 0x40E5A93C);
        }

        // --- 3. Fuel Tank Section ---
        boolean hoverFuel = isInRect(mouseX, mouseY, carX, carY + 106, 64, 18);
        boolean selFuel = (selectedSection == SECTION_FUEL_EXHAUST);
        int fuelCol = getZoneColor((vehicle.getFuel() / DrivableVehicleEntity.MAX_FUEL) * 100.0F);
        renderBlueprintZone(g, carX, carY + 106, 64, 18, fuelCol, hoverFuel, selFuel);
        // Fuel Tank Outline
        g.fill(carX + 16, carY + 109, carX + 48, carY + 121, 0xFF192534);
        drawBoxBorder(g, carX + 16, carY + 109, 32, 12, fuelCol);

        // --- 4. Trunk & Rear Bumper Section ---
        boolean hoverTrunk = isInRect(mouseX, mouseY, carX, carY + 124, 64, 20);
        boolean selTrunk = (selectedSection == SECTION_BODY);
        int trunkCol = getZoneColor(vehicle.getVehicleHealth() / DrivableVehicleEntity.MAX_HP * 100.0F);
        renderBlueprintZone(g, carX, carY + 124, 64, 20, trunkCol, hoverTrunk, selTrunk);
        // Tail lights (Ruby Red)
        g.fill(carX + 6, carY + 140, carX + 14, carY + 143, 0xFFE53E3E);
        g.fill(carX + 50, carY + 140, carX + 58, carY + 143, 0xFFE53E3E);
        // Rear bumper / Tailgate
        g.fill(carX + 4, carY + 143, carX + 60, carY + 146, 0xFF17202D);
        // Exhaust tip on left
        g.fill(carX + 8, carY + 145, carX + 12, carY + 148, 0xFF718096);

        // --- 5. 4 Wheels (Real tire aesthetics) ---
        renderBlueprintWheel(g, carX - 14, carY + 12, 12, 28, 0, mouseX, mouseY);
        renderBlueprintWheel(g, carX + 66, carY + 12, 12, 28, 1, mouseX, mouseY);
        renderBlueprintWheel(g, carX - 14, carY + 100, 12, 28, 2, mouseX, mouseY);
        renderBlueprintWheel(g, carX + 66, carY + 100, 12, 28, 3, mouseX, mouseY);
    }

    private void renderBlueprintZone(GuiGraphics g, int zx, int zy, int zw, int zh, int statusColor, boolean hover, boolean selected) {
        int bg = selected ? 0x5000D8F6 : (hover ? 0x38E5A93C : 0x181A2636);
        g.fill(zx + 1, zy + 1, zx + zw - 1, zy + zh - 1, bg);
        int borderCol = selected ? ACCENT_CYAN : (hover ? ACCENT_GOLD : (statusColor & 0x00FFFFFF) | 0x60000000);
        drawBoxBorder(g, zx, zy, zw, zh, borderCol);
    }

    private void drawBlueprintSeat(GuiGraphics g, int sx, int sy, int sw, int sh) {
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF141D29);
        drawBoxBorder(g, sx, sy, sw, sh, 0xFF2A3A4E);
        // Headrest
        g.fill(sx + 3, sy + 1, sx + sw - 3, sy + 4, 0xFF1D2838);
    }

    private void renderBlueprintWheel(GuiGraphics g, int wx, int wy, int ww, int wh, int index, int mouseX, int mouseY) {
        boolean installed = vehicle.isWheelInstalled(index);
        float cond = vehicle.getTireCondition(index);
        boolean hover = isInRect(mouseX, mouseY, wx, wy, ww, wh);
        boolean selected = (selectedSection == SECTION_WHEELS && selectedWheel == index);

        if (!installed) {
            // Missing wheel hub
            g.fill(wx, wy, wx + ww, wy + wh, 0x40E5534B);
            drawBoxBorder(g, wx, wy, ww, wh, ACCENT_RED);
            g.drawCenteredString(this.font, "§c✕", wx + (ww / 2), wy + (wh / 2) - 4, ACCENT_RED);
            return;
        }

        int wheelCol;
        if (cond < 15.0F) {
            wheelCol = (System.currentTimeMillis() % 400 < 200) ? ACCENT_RED : 0xFFFFFFFF;
        } else {
            wheelCol = getZoneColor(cond);
        }

        // Tire rubber body (dark textured rubber)
        int rubberBg = selected ? 0xFF193247 : (hover ? 0xFF2B281B : 0xFF161A22);
        g.fill(wx, wy, wx + ww, wy + wh, rubberBg);

        // Tread rib grooves along the sides
        for (int i = wy + 3; i < wy + wh - 3; i += 5) {
            g.fill(wx + 1, i, wx + 3, i + 2, 0xFF283140);
            g.fill(wx + ww - 3, i, wx + ww - 1, i + 2, 0xFF283140);
        }

        // Metallic Wheel Rim & Hub
        g.fill(wx + 3, wy + 5, wx + ww - 3, wy + wh - 5, 0xFF354254);
        drawBoxBorder(g, wx + 3, wy + 5, ww - 6, wh - 10, 0xFF53667E);
        // Hub center dot
        g.fill(wx + (ww / 2) - 1, wy + (wh / 2) - 1, wx + (ww / 2) + 1, wy + (wh / 2) + 1, wheelCol);

        // Outer status outline
        int borderCol = selected ? ACCENT_CYAN : (hover ? ACCENT_GOLD : wheelCol);
        drawBoxBorder(g, wx, wy, ww, wh, borderCol);
    }

    private void renderBlueprintTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int cardX = panelX + 10;
        int cardY = panelY + 28;
        int carX = cardX + 53;
        int carY = cardY + 34;

        List<Component> tooltip = null;

        // Check Wheels
        if (isInRect(mouseX, mouseY, carX - 14, carY + 12, 12, 28)) {
            tooltip = createWheelTooltip(0, "П. левое");
        } else if (isInRect(mouseX, mouseY, carX + 66, carY + 12, 12, 28)) {
            tooltip = createWheelTooltip(1, "П. правое");
        } else if (isInRect(mouseX, mouseY, carX - 14, carY + 100, 12, 28)) {
            tooltip = createWheelTooltip(2, "З. левое");
        } else if (isInRect(mouseX, mouseY, carX + 66, carY + 100, 12, 28)) {
            tooltip = createWheelTooltip(3, "З. правое");
        } else if (isInRect(mouseX, mouseY, carX, carY + 6, 64, 38)) {
            tooltip = new ArrayList<>();
            if (vehicle.hasEngine()) {
                tooltip.add(Component.literal("§6Двигатель и АКБ"));
                tooltip.add(Component.literal("§7Состояние: " + getZonePrefix(vehicle.getEngineCondition()) + String.format("%.0f%%", vehicle.getEngineCondition())));
                tooltip.add(Component.literal("§7АКБ: " + getZonePrefix(vehicle.getBatteryCharge()) + String.format("%.0f%%", vehicle.getBatteryCharge())));
                tooltip.add(Component.literal("§7Температура: §f" + String.format("%.1f °C", vehicle.getEngineTemperature())));
                tooltip.add(Component.literal("§7Масло: " + getZonePrefix(vehicle.getEngineOil()) + String.format("%.0f%%", vehicle.getEngineOil()) +
                    " §7| Антифриз: " + getZonePrefix(vehicle.getCoolantLevel()) + String.format("%.0f%%", vehicle.getCoolantLevel())));
            } else {
                tooltip.add(Component.literal("§cДвигатель не установлен"));
                tooltip.add(Component.literal("§7АКБ: " + getZonePrefix(vehicle.getBatteryCharge()) + String.format("%.0f%%", vehicle.getBatteryCharge())));
                tooltip.add(Component.literal("§8Смонтируйте мотор под капот."));
            }
        } else if (isInRect(mouseX, mouseY, carX, carY + 54, 64, 44)) {
            tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6Салон"));
            String ign = vehicle.isKeyInIgnition() ? "§aЕсть ключ" : "§cНет ключа";
            tooltip.add(Component.literal("§7Зажигание: " + ign));
            tooltip.add(Component.literal("§7Качество сборки: §f" + String.format("%.0f%%", vehicle.getEngineQuality())));
        } else if (isInRect(mouseX, mouseY, carX, carY + 106, 64, 18)) {
            tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6Топливо"));
            tooltip.add(Component.literal("§7Уровень: §f" + String.format("%.0f / %.0f л", vehicle.getFuel(), DrivableVehicleEntity.MAX_FUEL)));
            tooltip.add(Component.literal("§7Глушитель: " + getZonePrefix(vehicle.getMufflerHealth()) + String.format("%.0f%%", vehicle.getMufflerHealth())));
        } else if (isInRect(mouseX, mouseY, carX, carY + 124, 64, 20)) {
            tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6Кузов"));
            tooltip.add(Component.literal("§7Прочность: §f" + String.format("%.0f / %.0f HP", vehicle.getVehicleHealth(), DrivableVehicleEntity.MAX_HP)));
            tooltip.add(Component.literal("§7Бампер: §f" + String.format("%.0f%%", vehicle.getBumperHealth()) + " §7| Капот: §f" + String.format("%.0f%%", vehicle.getHoodHealth())));
            if (vehicle.getDirtLevel() > 0.0F || vehicle.getBloodLevel() > 0.0F) {
                tooltip.add(Component.literal("§7Грязь: §e" + String.format("%.0f%%", vehicle.getDirtLevel()) + " §7| Кровь: §c" + String.format("%.0f%%", vehicle.getBloodLevel())));
            }
        }

        if (tooltip != null) {
            drawCustomBlueprintTooltip(g, tooltip, mouseX, mouseY);
        }
    }

    private void drawCustomBlueprintTooltip(GuiGraphics g, List<Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;

        int padX = 8;
        int padY = 6;
        int lineHeight = 11;

        int maxTextWidth = 0;
        for (Component c : lines) {
            int w = this.font.width(c);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int boxWidth = maxTextWidth + padX * 2;
        int boxHeight = lines.size() * lineHeight + padY * 2;

        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;

        if (tooltipX + boxWidth > this.width - 4) {
            tooltipX = mouseX - boxWidth - 8;
        }
        if (tooltipY + boxHeight > this.height - 4) {
            tooltipY = this.height - boxHeight - 4;
        }
        if (tooltipY < 4) {
            tooltipY = 4;
        }
        if (tooltipX < 4) {
            tooltipX = 4;
        }

        // Dark matte slate background
        g.fill(tooltipX, tooltipY, tooltipX + boxWidth, tooltipY + boxHeight, 0xF60A0F15);
        // Slate blueprint border
        drawBoxBorder(g, tooltipX, tooltipY, boxWidth, boxHeight, 0xFF2A3A4E);
        // Cyan top glow accent line
        g.fill(tooltipX + 1, tooltipY + 1, tooltipX + boxWidth - 1, tooltipY + 2, ACCENT_CYAN);

        // Thin divider below title line if multi-line
        if (lines.size() > 1) {
            int sepY = tooltipY + padY + lineHeight - 1;
            g.fill(tooltipX + padX - 2, sepY, tooltipX + boxWidth - padX + 2, sepY + 1, 0x3058A6FF);
        }

        int curY = tooltipY + padY + 1;
        for (Component line : lines) {
            g.drawString(this.font, line, tooltipX + padX, curY, TEXT_WHITE, false);
            curY += lineHeight;
        }
    }

    private List<Component> createWheelTooltip(int index, String name) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("§6" + name));
        if (!vehicle.isWheelInstalled(index)) {
            list.add(Component.literal("§cКолесо снято"));
        } else {
            float cond = vehicle.getTireCondition(index);
            list.add(Component.literal("§7Шина: " + getZonePrefix(cond) + String.format("%.0f%%", cond)));
            if (cond < 15.0F) {
                list.add(Component.literal("§c⚠ Пробита!"));
            }
        }
        return list;
    }

    private String getZonePrefix(float val) {
        if (val >= 75.0F) return "§a";
        if (val >= 30.0F) return "§e";
        return "§c";
    }

    private void renderRightPanel(GuiGraphics g, int mouseX, int mouseY) {
        int rightX = panelX + 192;
        int rightY = panelY + 28;
        int rightW = 268;
        int rightH = 214;

        // Render Tabs row directly above details card
        renderTopTabs(g);

        // Details Card
        int detailsY = rightY + 19;
        int detailsH = rightH - 19;

        switch (selectedSection) {
            case SECTION_ENGINE -> renderEngineSection(g, rightX, detailsY, rightW, detailsH);
            case SECTION_CABIN -> renderCabinSection(g, rightX, detailsY, rightW, detailsH);
            case SECTION_WHEELS -> renderWheelsSection(g, rightX, detailsY, rightW, detailsH);
            case SECTION_BODY -> renderBodySection(g, rightX, detailsY, rightW, detailsH);
            case SECTION_FUEL_EXHAUST -> renderFuelExhaustSection(g, rightX, detailsY, rightW, detailsH);
        }
    }

    private void renderTopTabs(GuiGraphics g) {
        int tabY = panelY + 28;
        int tx = panelX + 192;
        String[] tabs = {"Двигатель", "Салон", "Колёса", "Кузов", "Топливо"};
        int[] tabWidths = {54, 48, 52, 50, 52};

        int curX = tx + 4;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (selectedSection == i);
            int tabW = tabWidths[i];

            g.fill(curX, tabY, curX + tabW, tabY + 16, active ? 0xFF192534 : 0x900D1219);
            drawBoxBorder(g, curX, tabY, tabW, 16, active ? ACCENT_GOLD : 0xFF253344);

            int tw = this.font.width(tabs[i]);
            g.drawString(this.font, tabs[i], curX + (tabW - tw) / 2, tabY + 4, active ? ACCENT_GOLD : TEXT_MUTED, false);

            curX += tabW + 2;
        }
    }

    private void renderEngineSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSimpleBox(g, x, y, w, h);

        int dy = y + 14;
        boolean hasEng = vehicle.hasEngine();
        String engName = hasEng ? vehicle.getInstalledEngine() : "§c[Отсутствует]";
        g.drawString(this.font, "§7Мотор: §f" + engName, x + 12, dy, TEXT_WHITE, false);

        btnEngineMount.setX(x + w - 12 - 76);
        btnEngineMount.setY(dy - 2);
        btnEngineMount.setWidth(76);
        btnEngineMount.setMessage(Component.literal(hasEng ? "Снять" : "Установить"));
        btnEngineMount.visible = true;

        dy += 18;

        // 1. Engine resource
        float engCond = hasEng ? vehicle.getEngineCondition() : 0.0F;
        drawStatRow(g, x + 12, dy, w - 24, "Состояние", engCond, 100.0F, "%", hasEng ? getZoneColor(engCond) : 0xFF7A1C1C);
        dy += 24;

        // 2. Battery
        float battery = vehicle.getBatteryCharge();
        drawStatRow(g, x + 12, dy, w - 24, "Аккумулятор", battery, 100.0F, "%", getZoneColor(battery));
        dy += 24;

        // 3. Temperature with Gauge
        float temp = vehicle.getEngineTemperature();
        int tempCol;
        String tempStatus;
        if (temp > 105.0F) {
            tempCol = (System.currentTimeMillis() % 400 < 200) ? ACCENT_RED : 0xFFFFFFFF;
            tempStatus = "§cПЕРЕГРЕВ";
        } else if (temp >= 85.0F && temp <= 95.0F) {
            tempCol = ACCENT_GREEN;
            tempStatus = "§aНорма";
        } else if (temp > 95.0F) {
            tempCol = ACCENT_GOLD;
            tempStatus = "§eВысокая";
        } else {
            tempCol = ACCENT_BLUE;
            tempStatus = "§bПрогрев";
        }
        g.drawString(this.font, "§7Температура: §f" + String.format("%.1f °C", temp), x + 12, dy, TEXT_WHITE, false);
        int statW = this.font.width(tempStatus);
        g.drawString(this.font, tempStatus, x + w - 12 - statW, dy, tempCol, false);
        float tempFrac = Mth.clamp((temp - 20.0F) / (130.0F - 20.0F), 0.0F, 1.0F);
        drawProgressBar(g, x + 12, dy + 10, w - 24, 5, tempFrac, tempCol);
        dy += 24;

        // 4. Motor Oil (Inline with Action Button)
        float oil = vehicle.getEngineOil();
        int btnW = 60;
        int barW = w - 24 - btnW - 6;
        btnRefillOil.setX(x + 12 + barW + 6);
        btnRefillOil.setY(dy + 5);
        btnRefillOil.setWidth(btnW);
        drawStatRowWithInlineButton(g, x + 12, dy, barW, "Масло", oil, 100.0F, "%", getZoneColor(oil));
        dy += 24;

        // 5. Coolant (Inline with Action Button)
        float coolant = vehicle.getCoolantLevel();
        btnRefillCoolant.setX(x + 12 + barW + 6);
        btnRefillCoolant.setY(dy + 5);
        btnRefillCoolant.setWidth(btnW);
        drawStatRowWithInlineButton(g, x + 12, dy, barW, "Антифриз", coolant, 100.0F, "%", getZoneColor(coolant));
        dy += 24;

        // 6. Radiator & Hood twin mini-bars
        int halfW = (w - 24 - 8) / 2;
        float rad = vehicle.getRadiatorHealth();
        float hood = vehicle.getHoodHealth();

        // Radiator mini-bar
        g.drawString(this.font, "§7Радиатор: " + String.format("%.0f%%", rad), x + 12, dy, getZoneColor(rad), false);
        drawMiniProgressBar(g, x + 12, dy + 10, halfW, 5, rad / 100.0F, getZoneColor(rad));

        // Hood mini-bar
        g.drawString(this.font, "§7Капот: " + String.format("%.0f%%", hood), x + 12 + halfW + 8, dy, getZoneColor(hood), false);
        drawMiniProgressBar(g, x + 12 + halfW + 8, dy + 10, halfW, 5, hood / 100.0F, getZoneColor(hood));
    }

    private void renderCabinSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSimpleBox(g, x, y, w, h);

        int dy = y + 16;
        // Ignition
        boolean hasKey = vehicle.isKeyInIgnition();
        String ignStatus = hasKey ? "§aЕсть ключ" : "§cНет ключа";
        g.drawString(this.font, "§7Зажигание: " + ignStatus, x + 12, dy, TEXT_WHITE, false);
        dy += 26;

        // Factory Quality
        float quality = vehicle.getEngineQuality();
        drawStatRow(g, x + 12, dy, w - 24, "Качество сборки", quality, 100.0F, "%", TEXT_WHITE);
    }

    private void renderWheelsSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSimpleBox(g, x, y, w, h);

        int dy = y + 36;
        String[] wheelNames = {"Переднее Левое", "Переднее Правое", "Заднее Левое", "Заднее Правое"};
        String currentWheelName = wheelNames[selectedWheel];

        g.drawString(this.font, "§eКолесо: §f" + currentWheelName, x + 12, dy, ACCENT_GOLD, false);
        dy += 18;

        boolean installed = vehicle.isWheelInstalled(selectedWheel);
        float cond = vehicle.getTireCondition(selectedWheel);

        if (!installed) {
            g.drawString(this.font, "§cКолесо снято с оси!", x + 12, dy, ACCENT_RED, false);
        } else {
            drawStatRow(g, x + 12, dy, w - 24, "Шина", cond, 100.0F, "%", getZoneColor(cond));
            dy += 26;

            if (cond < 15.0F) {
                boolean blink = (System.currentTimeMillis() % 400 < 200);
                int col = blink ? ACCENT_RED : 0xFFFFFFFF;
                g.drawString(this.font, "§c⚠ ШИНА ПРОБИТА!", x + 12, dy, col, false);
            }
        }
        dy += 26;

        // Spare wheel
        boolean hasSpare = vehicle.hasSpareWheel();
        g.drawString(this.font, "§7Запаска: " + (hasSpare ? "§aЕсть" : "§8Нет"), x + 12, dy, TEXT_WHITE, false);
    }

    private void renderBodySection(GuiGraphics g, int x, int y, int w, int h) {
        drawSimpleBox(g, x, y, w, h);

        int dy = y + 14;
        float hp = vehicle.getVehicleHealth();
        drawStatRow(g, x + 12, dy, w - 24, "Кузов", hp, DrivableVehicleEntity.MAX_HP, String.format("/%.0f", DrivableVehicleEntity.MAX_HP), getZoneColor(hp / DrivableVehicleEntity.MAX_HP * 100.0F));
        dy += 24;

        float bumper = vehicle.getBumperHealth();
        drawStatRow(g, x + 12, dy, w - 24, "1. Бампер", bumper, 100.0F, "%", getZoneColor(bumper));
        dy += 22;

        float hood = vehicle.getHoodHealth();
        drawStatRow(g, x + 12, dy, w - 24, "2. Капот", hood, 100.0F, "%", getZoneColor(hood));
        dy += 22;

        float rad = vehicle.getRadiatorHealth();
        drawStatRow(g, x + 12, dy, w - 24, "3. Радиатор", rad, 100.0F, "%", getZoneColor(rad));
        dy += 22;

        float eng = vehicle.getEngineCondition();
        drawStatRow(g, x + 12, dy, w - 24, "4. Двигатель", eng, 100.0F, "%", getZoneColor(eng));
        dy += 20;

        float dirt = vehicle.getDirtLevel();
        int dirtColor = dirt > 70.0F ? 0xFFE53E3E : (dirt > 30.0F ? 0xFFECC94B : 0xFF48BB78);
        drawStatRow(g, x + 12, dy, w - 24, "5. Грязь", dirt, 100.0F, "%", dirtColor);
        dy += 20;

        float blood = vehicle.getBloodLevel();
        int bloodColor = blood > 50.0F ? 0xFFE53E3E : (blood > 15.0F ? 0xFFDD6B20 : 0xFF48BB78);
        drawStatRow(g, x + 12, dy, w - 24, "6. Кровь", blood, 100.0F, "%", bloodColor);
    }

    private void renderFuelExhaustSection(GuiGraphics g, int x, int y, int w, int h) {
        drawSimpleBox(g, x, y, w, h);

        int dy = y + 16;
        float fuel = vehicle.getFuel();
        float maxFuel = DrivableVehicleEntity.MAX_FUEL;
        drawStatRow(g, x + 12, dy, w - 24, "Топливо", fuel, maxFuel, " л", getZoneColor((fuel / maxFuel) * 100.0F));
        dy += 26;

        // Exhaust
        float muffler = vehicle.getMufflerHealth();
        drawStatRow(g, x + 12, dy, w - 24, "Глушитель", muffler, 100.0F, "%", getZoneColor(muffler));
        dy += 24;

        float baseNoise = vehicle.getBaseLoudness();
        float noiseRadius = baseNoise * (1.0F + (100.0F - muffler) / 100.0F * 2.0F);
        String noiseWarn = noiseRadius > 50.0F ? " §c⚠ Шум привлекает мобов!" : "";
        g.drawString(this.font, "§7Шум: §e" + String.format("%.0f бл.", noiseRadius) + noiseWarn, x + 12, dy, TEXT_WHITE, false);
    }

    private void drawSimpleBox(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD_BG);
        drawBoxBorder(g, x, y, w, h, CARD_BORDER);
    }

    private void drawStatRow(GuiGraphics g, int x, int y, int totalWidth, String label, float current, float max, String valueSuffix, int color) {
        // Line 1: Label left, exact percentage right
        g.drawString(this.font, "§7" + label, x, y, TEXT_MUTED, false);
        String valStr = (valueSuffix != null && !valueSuffix.isEmpty()) ? String.format("%.0f%s", current, valueSuffix) : String.format("%.0f", current);
        int valW = this.font.width(valStr);
        g.drawString(this.font, valStr, x + totalWidth - valW, y, color, false);

        // Line 2: Progress bar across entire row
        drawProgressBar(g, x, y + 10, totalWidth, 6, current / max, color);
    }

    private void drawStatRowWithInlineButton(GuiGraphics g, int x, int y, int barWidth, String label, float current, float max, String valueSuffix, int color) {
        // Line 1: Label left, value right before button column
        g.drawString(this.font, "§7" + label, x, y, TEXT_MUTED, false);
        String valStr = (valueSuffix != null && !valueSuffix.isEmpty()) ? String.format("%.0f%s", current, valueSuffix) : String.format("%.0f", current);
        int valW = this.font.width(valStr);
        g.drawString(this.font, valStr, x + barWidth - valW, y, color, false);

        // Line 2: Progress bar (aligned on same row as button)
        drawProgressBar(g, x, y + 9, barWidth, 6, current / max, color);
    }

    private void drawProgressBar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        g.fill(x, y, x + width, y + height, 0xFF0B0F15);
        drawBoxBorder(g, x, y, width, height, 0xFF273547);

        float pct = Mth.clamp(fraction, 0.0F, 1.0F);
        int filledW = (int) ((width - 2) * pct);
        if (filledW > 0) {
            g.fill(x + 1, y + 1, x + 1 + filledW, y + height - 1, color);
        }
    }

    private void drawMiniProgressBar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        g.fill(x, y, x + width, y + height, 0xFF0D1219);
        drawBoxBorder(g, x, y, width, height, 0xFF283649);
        float pct = Mth.clamp(fraction, 0.0F, 1.0F);
        int filled = (int) ((width - 2) * pct);
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
        }
    }

    private int getZoneColor(float val) {
        if (val >= 75.0F) return ACCENT_GREEN;
        if (val >= 30.0F) return ACCENT_GOLD;
        return (System.currentTimeMillis() % 600 < 300) ? ACCENT_RED : 0xFF7A1C1C;
    }

    private void drawBoxBorder(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private boolean isInRect(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }
}
