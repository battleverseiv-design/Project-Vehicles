package com.evgen.cardecor.physics.dynamics;

/**
 * Конфигурация поведения машины, трансмиссии, устойчивости и кинематографичных переворотов.
 */
public class VehicleHandlingPreset {

    public enum DriveType {
        RWD, // Задний привод (0% перед / 100% зад) — силовой занос газом
        FWD, // Передний привод (100% перед / 0% зад) — снос носа под газом
        AWD  // Полный привод (35% перед / 65% зад) — занос всеми 4 колесами
    }

    // Трансмиссия
    public DriveType driveType = DriveType.RWD;
    public float awdFrontSplit = 0.35f;
    public float awdRearSplit = 0.65f;       // Доля тяги на передней оси для AWD
    public float engineTorque = 2200.0f;      // Крутящий момент мотора (Н*м)
    public float gearRatio = 9.2f;            // Общее передаточное число
    public float wheelInertia = 1.8f;         // Момент инерции колеса (кг*м^2)
    public float wheelRadius = 0.42f;         // Радиус колеса (м)

    // Подвеска: раздельная амортизация (Сжатие vs Отбой)
    public float springStiffness = 46000.0f;  // Жесткость пружины (Н/м)
    public float bumpDamping = 4400.0f;       // Демпфирование на сжатие (гасит удары)
    public float reboundDamping = 3200.0f;    // Демпфирование на отбой (мягче на отскок -> раскачка кузова)
    public float antiRollStiffness = 11000.0f;// Жесткость стабилизатора поперечной устойчивости (Н/м)
    public float minSuspLen = 0.08f;          // Ход до резинового отбойника (м)

    // Рычаг опрокидывания и Rollover Mechanics
    public float rollLeverHeight = 0.68f;     // Вертикальное расстояние от COM до пятна контакта
    public float rimBiteSpeedThreshold = 6.0f;// Мин. боковая скорость для закусывания шины (м/с)
    public float rimBitePower = 1.85f;        // Множитель бокового удара при подломе шины (до 2.2x)
    public float curbTripSensitivity = 1.0f;  // Чувствительность к боковым ударам о ступени/бордюры

    // Шины (Pacejka-Lite & Круг Камма)
    public float peakSlipAngleRad = (float) Math.toRadians(8.5); // Пик бокового сцепления
    public float slideFrictionRatio = 0.74f;  // Полка сцепления в скольжении
    public float frontCorneringStiffness = 16500.0f; // Угловая жесткость передней оси (Н/рад)
    public float rearCorneringStiffness = 14000.0f;  // Угловая жесткость задней оси (Н/рад)
    public float scrubDragCoeff = 0.22f;      // Сопротивление бокового волочения шин

    // ------------------------------------------------------------------------
    // ГОТОВЫЕ ПРЕСЕТЫ
    // ------------------------------------------------------------------------

    /**
     * Дрифт-купе (RWD).
     * Низкий центр масс, свободный занос газом, высокая устойчивость на ровной дороге.
     */
    public static VehicleHandlingPreset createDriftCoupeRwd() {
        VehicleHandlingPreset p = new VehicleHandlingPreset();
        p.driveType = DriveType.RWD;
        p.rollLeverHeight = 0.52f; // Низкий рычаг -> почти невозможно перевернуть на асфальте
        p.springStiffness = 48000.0f;
        p.bumpDamping = 4600.0f;
        p.reboundDamping = 3800.0f;
        p.antiRollStiffness = 13500.0f;
        p.rearCorneringStiffness = 13000.0f; // Мягче зад -> легче сорвать в занос
        p.rimBitePower = 1.25f;
        return p;
    }

    /**
     * Раллийный Хот-хэтч (FWD / AWD).
     * Отзывчивая перекладка веса (Scandinavian Flick), заброс кормы под сброс газа,
     * возможность встать на два колеса при резком контр-смещении.
     */
    public static VehicleHandlingPreset createRallyHatch() {
        VehicleHandlingPreset p = new VehicleHandlingPreset();
        p.driveType = DriveType.AWD;
        p.awdFrontSplit = 0.40f;
        p.rollLeverHeight = 0.68f;
        p.springStiffness = 44000.0f;
        p.bumpDamping = 4200.0f;
        p.reboundDamping = 2900.0f; // Мягкий отбой -> сильная раскачка кузова при перекладке
        p.antiRollStiffness = 9500.0f;
        p.rimBitePower = 1.65f;
        return p;
    }

    /**
     * Голливудский трюковой Внедорожник (Hollywood Stunt SUV).
     * Высокий центр масс, длинноходная мягкая подвеска, легко встает на 2 колеса
     * при резком вираже на скорости и кувыркается через крышу при боковом ударе о бордюр.
     */
    public static VehicleHandlingPreset createHollywoodStuntSuv() {
        VehicleHandlingPreset p = new VehicleHandlingPreset();
        p.driveType = DriveType.RWD;
        p.rollLeverHeight = 0.88f; // Огромный рычаг опрокидывания
        p.springStiffness = 38000.0f;
        p.bumpDamping = 3800.0f;
        p.reboundDamping = 2400.0f; // Сильная резонансная отдача подвески
        p.antiRollStiffness = 6500.0f; // Мягкий стабилизатор -> глубокие крены кузова
        p.rimBiteSpeedThreshold = 5.0f;
        p.rimBitePower = 2.20f;     // Мощный подброс при боковом закусывании диска
        p.curbTripSensitivity = 1.4f;
        return p;
    }
}
