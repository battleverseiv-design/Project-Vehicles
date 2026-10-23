package com.evgen.cardecor.client.gui;

import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.item.VehicleEngineItem;
import com.evgen.cardecor.network.EngineWorkbenchActionPacket;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.pack.EngineDefinition;
import com.evgen.cardecor.pack.EngineRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EngineWorkbenchScreen extends Screen {

    private final BlockPos pos;

    private static final int PANEL_BG     = 0xF80B0F15;
    private static final int PANEL_BORDER = 0xFF2A3441;
    private static final int CARD_BG      = 0xD8121822;
    private static final int CARD_BORDER  = 0xFF1D2633;

    private static final int ACCENT_GOLD  = 0xFFE5A93C;
    private static final int ACCENT_GREEN = 0xFF38B264;
    private static final int ACCENT_RED   = 0xFFE5534B;
    private static final int ACCENT_BLUE  = 0xFF58A6FF;
    private static final int ACCENT_CYAN  = 0xFF00D8F6;
    private static final int TEXT_WHITE   = 0xFFF0F6FC;
    private static final int TEXT_MUTED   = 0xFF8B949E;

    private int panelWidth = 380;
    private int panelHeight = 220;
    private int panelX;
    private int panelY;

    private MinimalButton btnAction;

    public EngineWorkbenchScreen(BlockPos pos) {
        super(Component.literal("Моторный стенд"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        // Close Button
        addRenderableWidget(new MinimalButton(
            panelX + panelWidth - 22, panelY + 6, 16, 14,
            Component.literal("✕"),
            btn -> this.onClose()
        ));

        // Action button
        btnAction = new MinimalButton(
            panelX + 170, panelY + panelHeight - 32, 196, 18,
            Component.literal("Выполнить"),
            btn -> handleActionButton()
        );
        addRenderableWidget(btnAction);
    }

    private ItemStack findEngineInInventory() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.getItem() instanceof VehicleEngineItem) {
                return st;
            }
        }
        return ItemStack.EMPTY;
    }

    private void handleActionButton() {
        ItemStack engine = findEngineInInventory();
        if (!engine.isEmpty()) {
            ModNetwork.sendToServer(new EngineWorkbenchActionPacket(pos, EngineWorkbenchActionPacket.ACTION_REPAIR));
        } else {
            ModNetwork.sendToServer(new EngineWorkbenchActionPacket(pos, EngineWorkbenchActionPacket.ACTION_BUILD_NEW));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);

        // Panel Background & Border
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        drawBoxBorder(g, panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);

        // Header Title
        g.drawString(this.font, "§fМоторный верстак (Сборка и ремонт)", panelX + 12, panelY + 9, TEXT_WHITE, false);
        g.fill(panelX + 10, panelY + 23, panelX + panelWidth - 10, panelY + 24, 0x4030363D);

        // Check Inventory
        Player player = Minecraft.getInstance().player;
        boolean hasWrench = false;
        boolean hasBlock = false;
        boolean hasPistons = false;
        boolean hasPlugs = false;
        boolean hasBelt = false;
        boolean hasOil = false;

        if (player != null) {
            hasWrench = player.getAbilities().instabuild;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                if (st.is(ModItems.WRENCH.get()) || st.getItem() instanceof com.evgen.cardecor.item.WrenchItem) hasWrench = true;
                if (st.is(ModItems.ENGINE_BLOCK.get())) hasBlock = true;
                if (st.is(ModItems.PISTON_SET.get())) hasPistons = true;
                if (st.is(ModItems.SPARK_PLUGS.get())) hasPlugs = true;
                if (st.is(ModItems.ALTERNATOR_BELT.get())) hasBelt = true;
                if (st.is(ModItems.MOTOR_OIL.get())) hasOil = true;
            }
        }

        ItemStack engine = findEngineInInventory();

        // Left Panel: Technical Schematic of Engine Stand
        int leftX = panelX + 10;
        int leftY = panelY + 28;
        int leftW = 146;
        int leftH = panelHeight - 38;
        drawSimpleBox(g, leftX, leftY, leftW, leftH);
        renderEngineStandBlueprint(g, leftX + 23, leftY + 28, !engine.isEmpty());

        // Right Panel: Details and Maintenance
        int rightX = panelX + 162;
        int rightY = panelY + 28;
        int rightW = panelWidth - 172;
        int rightH = panelHeight - 38;
        drawSimpleBox(g, rightX, rightY, rightW, rightH);

        int dy = rightY + 12;

        if (!engine.isEmpty()) {
            // Existing Engine Repair
            String engId = VehicleEngineItem.getEngineId(engine);
            float cond = VehicleEngineItem.getCondition(engine);
            float oil = VehicleEngineItem.getOil(engine);
            float quality = VehicleEngineItem.getQuality(engine);

            EngineDefinition def = EngineRegistry.get(engId);
            String name = (def != null && def.name != null) ? def.name : engId;

            g.drawString(this.font, "§6Двигатель на стенде:", rightX + 12, dy, ACCENT_GOLD, false);
            dy += 14;
            g.drawString(this.font, "§f" + name, rightX + 12, dy, TEXT_WHITE, false);
            dy += 18;

            drawStatRow(g, rightX + 12, dy, rightW - 24, "Состояние", cond, 100.0F, "%", getZoneColor(cond));
            dy += 22;
            drawStatRow(g, rightX + 12, dy, rightW - 24, "Масло", oil, 100.0F, "%", getZoneColor(oil));
            dy += 22;
            drawStatRow(g, rightX + 12, dy, rightW - 24, "Качество сборки", quality, 100.0F, "%", TEXT_WHITE);
            dy += 24;

            g.drawString(this.font, "§7Для переборки требуются:", rightX + 12, dy, TEXT_MUTED, false);
            dy += 14;

            String pText = "• Поршни: " + (hasPistons ? "§aЕсть" : "§cНет");
            String oText = "• Масло: " + (hasOil ? "§aЕсть" : "§cНет");
            String wText = "• Ключ: " + (hasWrench ? "§aЕсть" : "§cНет");

            g.drawString(this.font, pText, rightX + 12, dy, TEXT_WHITE, false);
            g.drawString(this.font, oText, rightX + 80, dy, TEXT_WHITE, false);
            g.drawString(this.font, wText, rightX + 140, dy, TEXT_WHITE, false);

            btnAction.setMessage(Component.literal("Капитальный ремонт двигателя"));
            btnAction.active = hasWrench && (hasPistons || (player != null && player.isCreative()));

        } else {
            // New Engine Assembly
            g.drawString(this.font, "§eСтенд пуст. Сборка нового V8:", rightX + 12, dy, ACCENT_GOLD, false);
            dy += 18;

            g.drawString(this.font, "• Блок цилиндров: " + (hasBlock ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);
            dy += 14;
            g.drawString(this.font, "• Комплект поршней: " + (hasPistons ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);
            dy += 14;
            g.drawString(this.font, "• Свечи зажигания: " + (hasPlugs ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);
            dy += 14;
            g.drawString(this.font, "• Ремень генератора: " + (hasBelt ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);
            dy += 14;
            g.drawString(this.font, "• Моторное масло: " + (hasOil ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);
            dy += 14;
            g.drawString(this.font, "• Гаечный ключ: " + (hasWrench ? "§aВ наличии" : "§cОтсутствует"), rightX + 12, dy, TEXT_WHITE, false);

            btnAction.setMessage(Component.literal("Собрать новый Ford 302 V8"));
            boolean canBuild = (hasBlock && hasPistons && hasPlugs && hasBelt && hasOil && hasWrench) || (player != null && player.isCreative());
            btnAction.active = canBuild;
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    private void renderEngineStandBlueprint(GuiGraphics g, int bx, int by, boolean hasEngine) {
        // Stand base & vertical pillar
        g.fill(bx + 15, by + 85, bx + 85, by + 92, 0xFF192534);
        drawBoxBorder(g, bx + 15, by + 85, 70, 7, 0xFF35445C);
        g.fill(bx + 45, by + 40, bx + 55, by + 85, 0xFF2A3A4E);
        drawBoxBorder(g, bx + 45, by + 40, 10, 45, 0xFF4A5D78);

        // Mounting head
        g.fill(bx + 35, by + 35, bx + 65, by + 43, 0xFF35485E);
        drawBoxBorder(g, bx + 35, by + 35, 30, 8, ACCENT_CYAN);

        if (hasEngine) {
            // Engine block on stand
            g.fill(bx + 25, by + 8, bx + 75, by + 35, 0xFF1C2737);
            drawBoxBorder(g, bx + 25, by + 8, 50, 27, 0xFF3B4E68);

            // V-shaped cylinder banks
            g.fill(bx + 20, by + 12, bx + 32, by + 32, 0xFF28364A);
            drawBoxBorder(g, bx + 20, by + 12, 12, 20, 0xFF485E7E);
            g.fill(bx + 68, by + 12, bx + 80, by + 32, 0xFF28364A);
            drawBoxBorder(g, bx + 68, by + 12, 12, 20, 0xFF485E7E);

            // Intake Manifold on top
            g.fill(bx + 35, by + 3, bx + 65, by + 12, 0xFF2E3D52);
            drawBoxBorder(g, bx + 35, by + 3, 30, 9, ACCENT_GOLD);

            // Pulley wheel on front
            g.fill(bx + 44, by + 22, bx + 56, by + 34, 0xFF17202D);
            drawBoxBorder(g, bx + 44, by + 22, 12, 12, ACCENT_CYAN);
        } else {
            // Dotted empty outline
            drawBoxBorder(g, bx + 25, by + 8, 50, 27, 0x603B4E68);
            g.drawCenteredString(this.font, "§8[ПУСТО]", bx + 50, by + 17, TEXT_MUTED);
        }
    }

    private void drawSimpleBox(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD_BG);
        drawBoxBorder(g, x, y, w, h, CARD_BORDER);
    }

    private void drawStatRow(GuiGraphics g, int x, int y, int totalWidth, String label, float current, float max, String valueSuffix, int color) {
        g.drawString(this.font, "§7" + label, x, y, TEXT_MUTED, false);
        String valStr = (valueSuffix != null && !valueSuffix.isEmpty()) ? String.format("%.0f%s", current, valueSuffix) : String.format("%.0f", current);
        int valW = this.font.width(valStr);
        g.drawString(this.font, valStr, x + totalWidth - valW, y, color, false);
        drawProgressBar(g, x, y + 10, totalWidth, 6, current / max, color);
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
}
