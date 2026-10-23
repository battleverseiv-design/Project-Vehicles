package com.evgen.cardecor.client.gui;

import com.evgen.cardecor.client.ModKeyBindings;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.climate.VehicleClimateCompat;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.network.VehicleClimatePacket;
import com.evgen.cardecor.network.VehicleHeadlightPacket;
import com.evgen.cardecor.network.VehicleHornPacket;
import com.evgen.cardecor.network.VehicleInputPacket;
import com.evgen.cardecor.network.VehicleSeatActionPacket;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/**
 * Minimalist Project Zomboid style Vehicle Interaction Radial Menu (HotKey 'V').
 * Features:
 * - 5-sector circular radial wheel: Engine, Headlights, Seats & Luggage, Horn, Sound.
 * - Minimalist monochrome icons with dynamic hover highlight.
 * - Circular central telemetry hub with clear contextual action prompts.
 * - Minimalist Cabin & Seats sub-menu with 4-slot item storage per passenger seat.
 */
public class VehicleRadialMenuScreen extends Screen {

    private final DrivableVehicleEntity vehicle;
    private boolean showingSeats = false;
    private int hoveredSector = -1;

    // Sectors count
    private static final int SECTOR_COUNT = 5;

    // Palette (Project Zomboid dark tactical theme)
    private static final int COLOR_OVERLAY       = 0x90070A0E; // Dim backdrop
    private static final int COLOR_SECTOR_BG     = 0xC0101620; // Semi-transparent dark slate
    private static final int COLOR_SECTOR_HOVER  = 0xE01C2738; // Highlighted sector fill
    private static final int COLOR_RING_BORDER   = 0x80303E4D; // Muted ring border
    private static final int COLOR_RING_HOVER    = 0xFFE5A93C; // Gold glow border
    private static final int COLOR_HUB_BG        = 0xF50C1017; // Central hub fill
    private static final int COLOR_PANEL_BG      = 0xEE111620;
    private static final int COLOR_CARD_BG       = 0xD018202C;
    private static final int COLOR_CARD_BORDER   = 0xFF2D3748;

    private static final int ACCENT_GOLD         = 0xFFE5A93C;
    private static final int ACCENT_GREEN        = 0xFF38B264;
    private static final int ACCENT_RED          = 0xFFE04343;
    private static final int ACCENT_BLUE         = 0xFF4A90E2;
    private static final int TEXT_WHITE          = 0xFFF0F4F8;
    private static final int TEXT_MUTED          = 0xFF8A99A8;

    // Radii for circular geometry (scaled down ~50% area)
    private static final float R_INNER = 33.0F;
    private static final float R_OUTER = 75.0F;
    private static final float R_MID   = 54.0F;
    private static final float R_HUB   = 29.0F;

    // Icons
    private static final ResourceLocation ICON_ENGINE     = new ResourceLocation("cardecor", "textures/gui/radial/engine.png");
    private static final ResourceLocation ICON_HEADLIGHTS = new ResourceLocation("cardecor", "textures/gui/radial/headlights.png");
    private static final ResourceLocation ICON_SEATS      = new ResourceLocation("cardecor", "textures/gui/radial/seats.png");
    private static final ResourceLocation ICON_HORN       = new ResourceLocation("cardecor", "textures/gui/radial/horn.png");
    private static final ResourceLocation ICON_SOUND      = new ResourceLocation("cardecor", "textures/gui/radial/sound.png");

    private static final ResourceLocation[] SECTOR_ICONS = {
        ICON_ENGINE,
        ICON_HEADLIGHTS,
        ICON_SEATS,
        ICON_HORN,
        ICON_SOUND
    };

    private static final String[] SECTOR_NAMES = {
        "Двигатель",
        "Фары",
        "Салон",
        "Клаксон",
        "Звук"
    };

    public VehicleRadialMenuScreen(DrivableVehicleEntity vehicle) {
        super(Component.literal("Меню автомобиля (Project Zomboid)"));
        this.vehicle = vehicle;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        if (this.showingSeats) {
            initSeatMenuWidgets();
        } else {
            initRadialQuickWidgets();
        }
    }

    private void initRadialQuickWidgets() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        boolean showClimate = VehicleClimateCompat.isAnyTemperatureModLoaded();
        int btnCount = showClimate ? 5 : 4;
        int btnW = showClimate ? 64 : 76;
        int btnH = 16;
        int spacing = 4;
        int totalW = btnW * btnCount + spacing * (btnCount - 1);
        int startX = cx - totalW / 2;
        int startY = cy + 82;

        // Button 1: Ignition Key
        boolean keyIn = vehicle.isKeyInIgnition();
        String keyText = keyIn ? "🔑 Вынуть" : "🔑 Вставить";
        this.addRenderableWidget(MinimalButton.builder(Component.literal(keyText), b -> {
            ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, keyIn ? VehicleSeatActionPacket.ACTION_REMOVE_KEY : VehicleSeatActionPacket.ACTION_INSERT_KEY));
            this.init();
        }).bounds(startX, startY, btnW, btnH).tooltip(Tooltip.create(Component.literal(keyIn ? "Забрать ключ из замка зажигания" : "Вставить ключ в замок зажигания"))).build());

        // Button 2: Door Locks
        boolean locked = vehicle.isLocked();
        String lockText = locked ? "🔒 Двери" : "🔓 Двери";
        this.addRenderableWidget(MinimalButton.builder(Component.literal(lockText), b -> {
            ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_LOCK));
            this.init();
        }).bounds(startX + btnW + spacing, startY, btnW, btnH).tooltip(Tooltip.create(Component.literal(locked ? "Двери заперты (Нажмите, чтобы отпереть)" : "Двери открыты (Нажмите, чтобы запереть)"))).build());

        // Button 3: Dedicated Trunk Lock
        boolean trunkLocked = vehicle.isTrunkLocked();
        String trunkText = trunkLocked ? "🔒 Багажник" : "🔓 Багажник";
        this.addRenderableWidget(MinimalButton.builder(Component.literal(trunkText), b -> {
            ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_TRUNK_LOCK));
            this.init();
        }).bounds(startX + (btnW + spacing) * 2, startY, btnW, btnH).tooltip(Tooltip.create(Component.literal(trunkLocked ? "Багажник заперт (Нажмите, чтобы отпереть)" : "Багажник отперт (Нажмите, чтобы запереть)"))).build());

        // Button 4: Diagnostics / Mechanics Menu
        this.addRenderableWidget(MinimalButton.builder(Component.literal("🔧 Механика"), b -> {
            this.minecraft.setScreen(new VehicleMechanicsScreen(vehicle));
        }).bounds(startX + (btnW + spacing) * 3, startY, btnW, btnH).tooltip(Tooltip.create(Component.literal("Открыть диагностику и механику капота"))).build());

        // Button 5: Climate Control (Heater / A/C) - displayed only when temperature mod is installed!
        if (showClimate) {
            byte climate = vehicle.getClimateMode();
            String climateText = switch (climate) {
                case 1 -> "♨ Печка";
                case 2 -> "❄ Кондёр";
                default -> "○ Климат";
            };
            String tooltipText = switch (climate) {
                case 1 -> vehicle.hasAirConditioning() ? "Печка ВКЛ (Нажмите для включения Кондиционера)" : "Печка ВКЛ (Нажмите для выключения)";
                case 2 -> "Кондиционер ВКЛ (Нажмите для выключения)";
                default -> "Климат-контроль: ВЫКЛ (Нажмите для включения печки)";
            };
            this.addRenderableWidget(MinimalButton.builder(Component.literal(climateText), b -> {
                ModNetwork.sendToServer(new VehicleClimatePacket(vehicle.getId()));
                if (vehicle.isEngineOn()) {
                    if (climate == 0) {
                        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(com.evgen.cardecor.init.ModSounds.CAR_HEATER_START.get(), 1.0F, 0.85F));
                    } else if (climate == 1 && !vehicle.hasAirConditioning()) {
                        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(com.evgen.cardecor.init.ModSounds.CAR_HEATER_STOP.get(), 1.0F, 0.85F));
                    }
                }
                this.init();
            }).bounds(startX + (btnW + spacing) * 4, startY, btnW, btnH).tooltip(Tooltip.create(Component.literal(tooltipText))).build());
        }
    }

    private void initSeatMenuWidgets() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        int panelW = 270;
        int panelH = 205;
        int panelX = cx - panelW / 2;
        int panelY = cy - panelH / 2;

        int seatW = 118;
        int seatH = 48;

        // 4 Seats: 2x2 layout like Project Zomboid vehicle blueprint
        int[][] seatPositions = {
            {panelX + 12, panelY + 38},                     // Seat 0: Driver (Front Left)
            {panelX + 140, panelY + 38},                    // Seat 1: Front Passenger
            {panelX + 12, panelY + 92},                     // Seat 2: Rear Left
            {panelX + 140, panelY + 92}                     // Seat 3: Rear Right
        };

        Player player = this.minecraft != null ? this.minecraft.player : null;
        int mySeat = player != null ? vehicle.getSeatForPlayer(player) : -1;
        byte itemMask = vehicle.getEntityData().get(DrivableVehicleEntity.SEAT_ITEM_STATUS);

        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
        boolean isPickup = (def != null && def.hasCargoBed());

        for (int s = 0; s < 4; s++) {
            if (s >= vehicle.getMaxSeats()) continue;

            final int seatIdx = s;
            int x = seatPositions[s][0];
            int y = seatPositions[s][1];
            int currentSeatW = seatW;

            boolean isDriver = (s == 0);
            boolean isMySeat = (s == mySeat);
            boolean isOccupiedByOther = vehicle.isSeatOccupiedByPassenger(s) && !isMySeat;
            boolean hasItems = !isDriver && ((itemMask & (1 << s)) != 0);
            boolean isBed = vehicle.isCargoBedSeat(s);

            if (isBed) {
                // Wide rear cargo bed / trunk seat (246 px)
                currentSeatW = panelW - 24;
                x = panelX + 12;
                y = panelY + 92;

                if (hasItems) {
                    // Luggage in bed: sitting is blocked!
                } else if (isMySeat) {
                    // Player is seated in bed: do not show "Вещи" button
                } else if (!isOccupiedByOther) {
                    this.addRenderableWidget(MinimalButton.builder(Component.literal("Сесть в кузов"), b -> {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), seatIdx, VehicleSeatActionPacket.ACTION_SIT));
                        this.onClose();
                    }).bounds(x + 5, y + 25, currentSeatW - 10, 18).build());
                }
            } else if (isDriver) {
                // Driver Seat: Click to sit (if free)
                if (!isMySeat && !isOccupiedByOther) {
                    this.addRenderableWidget(MinimalButton.builder(Component.literal("Сесть за руль"), b -> {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_SIT));
                        this.onClose();
                    }).bounds(x + 5, y + 25, 108, 18).build());
                }
            } else {
                // Passenger Seats (1, 2, 3)
                if (hasItems) {
                    // Items on seat: open inventory to retrieve items
                    this.addRenderableWidget(MinimalButton.builder(Component.literal("📦 Вещи (4)"), b -> {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), seatIdx, VehicleSeatActionPacket.ACTION_OPEN_INVENTORY));
                        this.onClose();
                    }).bounds(x + 5, y + 25, 108, 18).tooltip(Tooltip.create(Component.literal("Открыть вещи на сиденье"))).build());
                } else if (isMySeat) {
                    // Player seated here: "Вещи" button is hidden per user request
                } else if (!isOccupiedByOther) {
                    // Free seat: [Сесть] or [📦 Вещи] side by side
                    this.addRenderableWidget(MinimalButton.builder(Component.literal("Сесть"), b -> {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), seatIdx, VehicleSeatActionPacket.ACTION_SIT));
                        this.onClose();
                    }).bounds(x + 5, y + 25, 52, 18).build());

                    this.addRenderableWidget(MinimalButton.builder(Component.literal("📦 Вещи"), b -> {
                        ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), seatIdx, VehicleSeatActionPacket.ACTION_OPEN_INVENTORY));
                        this.onClose();
                    }).bounds(x + 60, y + 25, 53, 18).tooltip(Tooltip.create(Component.literal("Положить вещи на сиденье (4 слота)"))).build());
                }
            }
        }

        // Cabin light toggle button and Back button
        boolean cabinLight = vehicle.isCabinLightOn();
        String lightText = cabinLight ? "💡 Свет: ВКЛ" : "💡 Свет: ВЫКЛ";
        this.addRenderableWidget(MinimalButton.builder(Component.literal(lightText), b -> {
            ModNetwork.sendToServer(new VehicleSeatActionPacket(vehicle.getId(), 0, VehicleSeatActionPacket.ACTION_TOGGLE_CABIN_LIGHT));
            this.init();
        }).bounds(panelX + 18, panelY + 155, 110, 20).tooltip(Tooltip.create(Component.literal("Включить/выключить свет в салоне (Клавиша I)"))).build());

        // Back button to return to Radial Menu (Centered per user request)
        this.addRenderableWidget(MinimalButton.builder(Component.literal("← Назад"), b -> {
            this.showingSeats = false;
            this.init();
        }).bounds(panelX + 142, panelY + 155, 110, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.vehicle == null || this.vehicle.isRemoved()) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // Dim screen background
        g.fillGradient(0, 0, this.width, this.height, COLOR_OVERLAY, COLOR_OVERLAY);

        if (this.showingSeats) {
            renderSeatMenu(g, mouseX, mouseY);
        } else {
            renderRadialMenu(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    /**
     * Renders the authentic 5-sector circular radial wheel menu.
     */
    private void renderRadialMenu(GuiGraphics g, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double sectorAngle = (2.0 * Math.PI) / SECTOR_COUNT; // 72 degrees each

        // Calculate hovered sector
        // Sector 0 is centered at Top (-PI/2)
        this.hoveredSector = -1;
        if (dist >= R_INNER && dist <= (R_OUTER + 12.0F)) {
            double angle = Math.atan2(dy, dx); // [-PI, PI]
            double angleFromTop = angle - (-Math.PI / 2.0);
            while (angleFromTop < 0) angleFromTop += 2.0 * Math.PI;
            while (angleFromTop >= 2.0 * Math.PI) angleFromTop -= 2.0 * Math.PI;

            double shifted = (angleFromTop + sectorAngle / 2.0) % (2.0 * Math.PI);
            int idx = (int) (shifted / sectorAngle);
            if (idx >= 0 && idx < SECTOR_COUNT) {
                this.hoveredSector = idx;
            }
        }

        Matrix4f matrix = g.pose().last().pose();

        // 1. Draw 5 radial sectors (pie wedges)
        float gap = 0.02F; // Small angular gap between sectors
        for (int i = 0; i < SECTOR_COUNT; i++) {
            boolean hovered = (this.hoveredSector == i);
            double centerA = (-Math.PI / 2.0) + (i * sectorAngle);
            float a0 = (float) (centerA - (sectorAngle / 2.0) + gap);
            float a1 = (float) (centerA + (sectorAngle / 2.0) - gap);

            float rOut = hovered ? (R_OUTER + 4.0F) : R_OUTER;
            int bg = hovered ? COLOR_SECTOR_HOVER : COLOR_SECTOR_BG;

            drawAnnularSector(matrix, cx, cy, R_INNER, rOut, a0, a1, bg);

            // Outer border glow for sector
            int borderCol = hovered ? COLOR_RING_HOVER : COLOR_RING_BORDER;
            drawAnnularSector(matrix, cx, cy, rOut - 1.5F, rOut, a0, a1, borderCol);
            drawAnnularSector(matrix, cx, cy, R_INNER, R_INNER + 1.5F, a0, a1, borderCol);
        }

        // 2. Draw Central Hub
        renderCentralHub(g, matrix, cx, cy);

        // 3. Render Icons and Text for each sector
        for (int i = 0; i < SECTOR_COUNT; i++) {
            boolean hovered = (this.hoveredSector == i);
            double centerA = (-Math.PI / 2.0) + (i * sectorAngle);

            float rMid = hovered ? (R_MID + 2.0F) : R_MID;
            int sx = (int) (cx + Math.cos(centerA) * rMid);
            int sy = (int) (cy + Math.sin(centerA) * rMid);

            // Icon (16x16)
            ResourceLocation icon = SECTOR_ICONS[i];
            int ix = sx - 8;
            int iy = sy - 11;

            // Tint icon
            if (hovered) {
                g.setColor(1.0F, 0.85F, 0.40F, 1.0F); // Warm gold glow
            } else if (i == 0 && vehicle.isEngineOn()) {
                g.setColor(0.40F, 0.95F, 0.50F, 0.95F); // Active engine green tint
            } else if (i == 1 && vehicle.isHeadlightsOn()) {
                if (vehicle.getHeadlightMode() == 2) {
                    g.setColor(0.55F, 0.85F, 1.0F, 1.0F); // High beam crisp blue-white tint
                } else {
                    g.setColor(1.0F, 0.95F, 0.55F, 0.95F); // Low beam warm yellow tint
                }
            } else {
                g.setColor(0.80F, 0.85F, 0.90F, 0.85F); // Clean monochrome white/silver
            }

            g.blit(icon, ix, iy, 0.0F, 0.0F, 16, 16, 16, 16);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset tint

            // Sector Label
            String name = SECTOR_NAMES[i];
            int textCol = hovered ? ACCENT_GOLD : TEXT_WHITE;
            g.drawCenteredString(this.font, name, sx, sy + 6, textCol);
        }

        // Bottom hint
        g.drawCenteredString(this.font, "§7[V / ESC] Закрыть  •  [ЛКМ] Выбрать", cx, this.height - 22, TEXT_MUTED);
    }

    /**
     * Renders the sleek circular central hub.
     */
    private void renderCentralHub(GuiGraphics g, Matrix4f matrix, int cx, int cy) {
        // Draw hub circular background
        drawFilledCircle(matrix, cx, cy, R_HUB, COLOR_HUB_BG);

        // Circular border
        int hubBorder = (hoveredSector != -1) ? ACCENT_GOLD : COLOR_RING_BORDER;
        drawAnnularSector(matrix, cx, cy, R_HUB - 1.5F, R_HUB, 0.0F, (float) (2.0 * Math.PI), hubBorder);

        VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
        String name = (def != null && def.name != null) ? def.name : "Машина";

        if (hoveredSector >= 0 && hoveredSector < SECTOR_COUNT) {
            // Hover state: Action Title & Description
            g.drawCenteredString(this.font, "§6" + name, cx, cy - 18, TEXT_WHITE);

            String actionTitle = "";
            String actionDesc = "";
            switch (hoveredSector) {
                case 0 -> {
                    actionTitle = vehicle.isEngineOn() ? "§cОстановить" : "§aЗавести";
                    actionDesc = "§7Мотор";
                }
                case 1 -> {
                    actionTitle = vehicle.isHeadlightsOn() ? "§7Выключить" : "§eВключить";
                    actionDesc = "§7Фары";
                }
                case 2 -> {
                    actionTitle = "§eСалон";
                    actionDesc = "§7Места";
                }
                case 3 -> {
                    actionTitle = "§bКлаксон";
                    actionDesc = "§7Сигнал";
                }
                case 4 -> {
                    actionTitle = "§dЗвук";
                    actionDesc = "§7Настройки";
                }
            }

            g.drawCenteredString(this.font, actionTitle, cx, cy - 6, TEXT_WHITE);
            g.drawCenteredString(this.font, actionDesc, cx, cy + 5, TEXT_MUTED);
        } else {
            // Idle state: Vehicle stats
            g.drawCenteredString(this.font, "§6" + name, cx, cy - 18, TEXT_WHITE);

            boolean engOn = vehicle.isEngineOn();
            String engStr = engOn ? "§a● ВКЛ" : "§c○ ВЫКЛ";
            g.drawCenteredString(this.font, engStr, cx, cy - 9, TEXT_WHITE);

            int fuelPct = (int) (vehicle.getFuel() / DrivableVehicleEntity.MAX_FUEL * 100.0F);
            int hpPct = (int) (vehicle.getVehicleHealth() / DrivableVehicleEntity.MAX_HP * 100.0F);
            g.drawCenteredString(this.font, "§e⛽" + fuelPct + "% §c❤" + hpPct + "%", cx, cy + 1, TEXT_WHITE);

            boolean keyIn = vehicle.isKeyInIgnition();
            boolean locked = vehicle.isLocked();
            boolean trunkLocked = vehicle.isTrunkLocked();
            String keyStr = keyIn ? "§a🔑" : "§7🔑";
            String lockStr = locked ? "§c🔒" : "§a🔓";
            String trunkStr = trunkLocked ? "§c📦" : "§a📦";
            String climStr = "";
            if (VehicleClimateCompat.isAnyTemperatureModLoaded()) {
                byte clim = vehicle.getClimateMode();
                climStr = (clim == 1) ? "  §6♨" : (clim == 2) ? "  §b❄" : "  §7○";
            }
            g.drawCenteredString(this.font, keyStr + "  " + lockStr + "  " + trunkStr + climStr, cx, cy + 11, TEXT_WHITE);
        }
    }

    /**
     * Renders the minimal Cabin & Seats sub-menu dialog.
     */
    private void renderSeatMenu(GuiGraphics g, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        int panelW = 270;
        int panelH = 205;
        int panelX = cx - panelW / 2;
        int panelY = cy - panelH / 2;

        // Background panel (Dark translucent PZ tactical HUD)
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);
        renderOutline(g, panelX, panelY, panelW, panelH, COLOR_CARD_BORDER);

        // Header
        g.drawCenteredString(this.font, "§6САЛОН АВТОМОБИЛЯ", cx, panelY + 8, ACCENT_GOLD);

        // Windshield blueprint indicator
        g.fill(cx - 70, panelY + 20, cx + 70, panelY + 21, 0x8058A6FF);
        g.drawCenteredString(this.font, "§8▲ Лобовое стекло ▲  •  [ 📦 Вещи ] — положить вещи", cx, panelY + 24, TEXT_MUTED);

        int seatW = 118;
        int seatH = 48;

        int[][] seatPositions = {
            {panelX + 12, panelY + 38},                     // Seat 0: Driver
            {panelX + 140, panelY + 38},                    // Seat 1: Front Passenger
            {panelX + 12, panelY + 92},                     // Seat 2: Rear Left
            {panelX + 140, panelY + 92}                     // Seat 3: Rear Right
        };

        String[] seatTitles = {
            "0: Водитель",
            "1: Пассажир",
            "2: Сзади Л.",
            "3: Сзади П."
        };

        Player player = this.minecraft != null ? this.minecraft.player : null;
        int mySeat = player != null ? vehicle.getSeatForPlayer(player) : -1;
        byte itemMask = vehicle.getEntityData().get(DrivableVehicleEntity.SEAT_ITEM_STATUS);

        boolean isPickup = "vintage_pickup".equalsIgnoreCase(vehicle.getVehicleId())
            || (VehiclePackManager.get(vehicle.getVehicleId()) != null && VehiclePackManager.get(vehicle.getVehicleId()).id.contains("pickup"));

        for (int s = 0; s < 4; s++) {
            if (s >= vehicle.getMaxSeats()) continue;

            int x = seatPositions[s][0];
            int y = seatPositions[s][1];
            int currentSeatW = seatW;

            boolean isDriver = (s == 0);
            boolean isMySeat = (s == mySeat);
            boolean isOccupied = vehicle.isSeatOccupiedByPassenger(s);
            boolean hasItems = !isDriver && ((itemMask & (1 << s)) != 0);

            String title = seatTitles[s];
            if (isPickup && s == 2) {
                currentSeatW = panelW - 24;
                x = panelX + 12;
                y = panelY + 92;
                title = "2: Багажник (кузов)";
            }

            // Project Zomboid style Seat Card styling
            int bgCol;
            int borderCol;
            String status;

            if (isMySeat) {
                // Green: Current player's seat
                bgCol = 0x602EA043;
                borderCol = 0xFF2EA043;
                status = "§a✓ Вы здесь";
            } else if (hasItems) {
                // Amber/Gold: Luggage on seat
                bgCol = 0x608B5CF6;
                borderCol = 0xFFFFD166;
                status = (isPickup && s == 2) ? "§e📦 Вещи в багажнике" : "§e📦 Вещи";
            } else if (isOccupied) {
                // Red: Occupied by other player
                bgCol = 0x60DA3633;
                borderCol = 0xFFDA3633;
                status = "§cЗанято";
            } else {
                // Slate/Gray: Free seat
                bgCol = 0x5021262D;
                borderCol = 0xFF484F58;
                status = "§7Свободно";
            }

            g.fill(x, y, x + currentSeatW, y + seatH, bgCol);
            renderOutline(g, x, y, currentSeatW, seatH, borderCol);

            // Title & Status
            g.drawString(this.font, title, x + 5, y + 4, TEXT_WHITE);
            g.drawString(this.font, status, x + 5, y + 14, 0xFFFFFFFF);
        }
    }

    private void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /**
     * Draws a filled annular sector (pie slice of a donut ring) from angle a0 to a1.
     */
    private void drawAnnularSector(Matrix4f matrix, float cx, float cy, float rIn, float rOut, float a0, float a1, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        int segments = Math.max(8, (int) (Math.abs(a1 - a0) * 16));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float theta = a0 + (a1 - a0) * ((float) i / (float) segments);
            float cos = Mth.cos(theta);
            float sin = Mth.sin(theta);

            buffer.vertex(matrix, cx + cos * rOut, cy + sin * rOut, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, cx + cos * rIn, cy + sin * rIn, 0.0F).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    /**
     * Draws a filled circle.
     */
    private void drawFilledCircle(Matrix4f matrix, float cx, float cy, float radius, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        int segments = 40;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, cx, cy, 0.0F).color(r, g, b, a).endVertex();
        for (int i = 0; i <= segments; i++) {
            float theta = (float) (i * 2.0 * Math.PI / segments);
            buffer.vertex(matrix, cx + Mth.cos(theta) * radius, cy + Mth.sin(theta) * radius, 0.0F).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.showingSeats && this.hoveredSector >= 0) {
            if (button == 0) { // LMB: execute action
                executeSectorAction(this.hoveredSector);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void executeSectorAction(int sector) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        switch (sector) {
            case 0 -> {
                // 🔑 Toggle engine
                ModNetwork.sendToServer(new VehicleInputPacket(vehicle.getId(), (byte) 32));
            }
            case 1 -> {
                // 💡 Toggle headlights
                ModNetwork.sendToServer(new VehicleHeadlightPacket(vehicle.getId()));
            }
            case 2 -> {
                // 💺 Cabin & Seats layout
                this.showingSeats = true;
                this.init();
            }
            case 3 -> {
                // 📢 Horn
                ModNetwork.sendToServer(new VehicleHornPacket(vehicle.getId()));
            }
            case 4 -> {
                // 🔊 Sound settings
                this.minecraft.setScreen(new VehicleSoundSettingsScreen(this));
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.KEY_RADIAL_MENU.matches(keyCode, scanCode) || keyCode == 256) { // 'V' or ESC
            if (this.showingSeats) {
                this.showingSeats = false;
                this.init();
                return true;
            } else {
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
