package com.evgen.cardecor.physics.dynamics;

import net.minecraft.util.Mth;

/**
 * Мультипликаторы проходимости для адаптации 6-DOF физики под ванильный воксельный ландшафт Minecraft.
 * Могут загружаться из JSON конкретной машины и дополнительно умножаться на глобальный конфиг мода.
 */
public class TerrainTraversalSpec {

    // 1. Максимальная высота вертикального уступа (в блоках/метрах):
    // 0.55f = только полублоки/ступени; 1.05f = заезжает на полные блоки (1x1x1) ванильных холмов!
    public float maxStepHeight = 1.05f;

    // 2. Помощь при заезде на уступ (наклон вектора тяги вверх на ступеньку, 0.0 .. 2.5):
    public float stepClimbAssist = 1.35f;

    // 3. Усиление подъемного импульса подвески при виде ступеньки перед колесом (1.0 .. 3.5):
    public float suspensionStepBoost = 2.20f;

    // 4. Компенсация гравитации на крутых ванильных подъемах в 45 градусов (1.0 .. 2.5):
    public float slopeClimbBoost = 1.60f;

    // 5. Множитель штрафа вязкости грунта (0.0 = везде как по асфальту, 0.35 = легкий внедорожник, 1.0 = хардкор):
    public float offroadDragMultiplier = 0.35f;

    // 6. Дополнительный бонус к сцеплению (muLat / muLong) на бездорожье (трава, песок, снег):
    public float offroadGripMultiplier = 1.25f;

    // 7. Пролом листвы, кустов и мелких препятствий в ванильном лесу без остановки:
    public boolean foliageBulldozer = true;

    // --- Пресеты для быстрой настройки разных классов машин ---
    public static TerrainTraversalSpec createVanillaAllTerrainPreset() {
        return new TerrainTraversalSpec(); // По умолчанию идеально настроен под ванильный мир (1 блок)
    }

    public static TerrainTraversalSpec createStreetSportPreset() {
        TerrainTraversalSpec s = new TerrainTraversalSpec();
        s.maxStepHeight = 0.55f;         // Только плиты и ступеньки
        s.stepClimbAssist = 0.65f;
        s.suspensionStepBoost = 1.25f;
        s.slopeClimbBoost = 1.10f;
        s.offroadDragMultiplier = 0.85f; // Сильно вязнет в песке и грязи
        s.offroadGripMultiplier = 0.90f;
        s.foliageBulldozer = false;
        return s;
    }

    public static TerrainTraversalSpec createHeavyCrawlerPreset() {
        TerrainTraversalSpec s = new TerrainTraversalSpec();
        s.maxStepHeight = 1.25f;         // Заезжает даже на блок с полублоком / заборы
        s.stepClimbAssist = 1.85f;
        s.suspensionStepBoost = 2.80f;
        s.slopeClimbBoost = 2.10f;
        s.offroadDragMultiplier = 0.15f; // Прет через болото как танк
        s.offroadGripMultiplier = 1.45f;
        s.foliageBulldozer = true;
        return s;
    }

    public void copyFrom(TerrainTraversalSpec other) {
        if (other == null) return;
        this.maxStepHeight = other.maxStepHeight;
        this.stepClimbAssist = other.stepClimbAssist;
        this.suspensionStepBoost = other.suspensionStepBoost;
        this.slopeClimbBoost = other.slopeClimbBoost;
        this.offroadDragMultiplier = other.offroadDragMultiplier;
        this.offroadGripMultiplier = other.offroadGripMultiplier;
        this.foliageBulldozer = other.foliageBulldozer;
    }
}
