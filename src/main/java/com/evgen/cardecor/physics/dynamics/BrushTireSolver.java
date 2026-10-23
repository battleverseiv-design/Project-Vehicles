package com.evgen.cardecor.physics.dynamics;

import net.minecraft.util.Mth;

/**
 * Честный кинематический солвер шины (Brush Tire Model + Combined Slip + Semi-Implicit Wheel Integrator).
 * Частота: 80 Гц (dt = 0.0125f), Zero-GC.
 */
public final class BrushTireSolver {

    // Константы модели протектора
    public static final float SLIP_RATIO_PEAK      = 0.12f;     // Пик продольного зацепа при 12% скольжения
    public static final float SLIP_ANGLE_TAN_PEAK  = 0.16f;     // Пик бокового зацепа при tan(9.1 deg)
    public static final float SLIDE_FRICTION_RATIO = 0.74f;     // Отношение трения скольжения к пиковому (mu_slide / mu_peak)

    public static final float V_MIN_LONG           = 1.50f;     // Порог регуляризации продольной скорости (м/с)
    public static final float V_MIN_LAT            = 1.20f;     // Порог регуляризации боковой скорости (м/с)

    public static final float NOMINAL_TIRE_LOAD    = 4050.0f;   // Номинальная нагрузка на 1 колесо для ~1650 кг (Н)
    public static final float LOAD_SENSITIVITY     = 0.14f;     // Коэффициент дегрессивности сцепления от перегрузки F_z
    public static final float BEARING_VISCOUS_DAMP = 0.25f;     // Вязкое сопротивление ступицы/редуктора (Н*м*с/рад)

    // Camber dynamics constants
    public static final float GAMMA_OPT            = -0.03f;    // Оптимальный статический развал (~ -1.7 deg)
    public static final float KAPPA_G              = 5.0f;      // Коэффициент спада сцепления от отклонения развала (рад^-2)
    public static final float R_GAMMA              = 0.08f;     // Отношение жесткости тяги развала к жесткости увода C_gamma / C_alpha

    /**
     * Преаллоцированный контейнер состояния и выходных сил одного колеса (Zero-GC).
     */
    public static final class WheelState {
        public float wheelRadius = 0.38f;
        public float wheelInertia = 1.4f;      // кг * м^2
        public float invWheelInertia = 1.0f / 1.4f;

        // Интегрируемое состояние колеса между подшагами
        public float angularVelocity = 0.0f;   // omega (рад/с)
        public float spinAngleRad = 0.0f;      // угол для рендера (рад)
        public float camberRad = 0.0f;         // динамический угол развала (рад)

        // Выходные силы в локальных осях пятна контакта (для приложения к DBody в ODE4J)
        public float outForceLongitudinal = 0.0f; // F_x (Н, вдоль направления качения колеса)
        public float outForceLateral = 0.0f;      // F_y (Н, вдоль боковой оси колеса)
        public float outCombinedSlipNorm = 0.0f;  // Нормированный индекс срыва s (> 1.0 = срыв)
        public float outSlipAngleTanNorm = 0.0f;  // Безразмерный боковой увод theta = |sy|
        public float outAligningTorque = 0.0f;    // M_z (Н*м, самовыравнивающий момент)

        public WheelState() {
        }

        public WheelState(float wheelRadius, float wheelInertia) {
            init(wheelRadius, wheelInertia);
        }

        public void init(float wheelRadius, float wheelInertia) {
            this.wheelRadius = Math.max(0.05f, wheelRadius);
            this.wheelInertia = Math.max(0.05f, wheelInertia);
            this.invWheelInertia = 1.0f / this.wheelInertia;
        }

        public void reset() {
            this.angularVelocity = 0.0f;
            this.spinAngleRad = 0.0f;
            this.outForceLongitudinal = 0.0f;
            this.outForceLateral = 0.0f;
            this.outCombinedSlipNorm = 0.0f;
        }
    }

    /**
     * Вычисляет силы в пятне контакта и интегрирует угловую скорость колеса за подшаг dt = 0.0125 с.
     *
     * @param wheel            состояние колеса (хранит omega и принимает выходные силы F_x, F_y)
     * @param normalLoadFz     нормальная реакция опоры F_z (Н) С УЧЕТОМ переноса веса от Anti-Roll Bar
     * @param vx               скорость центра пятна контакта вдоль продольной оси колеса (вперед) в м/с
     * @param vy               скорость центра пятна контакта вдоль боковой оси колеса (вправо) в м/с
     * @param driveTorqueNm    крутящий момент от трансмиссии на этом колесе (Н*м)
     * @param brakeTorqueNm    тормозной момент от тормозов и ручника (Н*м, >= 0)
     * @param effectiveMassLat эффективная масса кузова в точке контакта по боковой оси (кг)
     * @param dt               шаг интегрирования (0.0125f)
     * @param surfaceMu        базовый коэффициент сцепления покрытия под колесом
     */
    public static void solveWheelStep(
            WheelState wheel,
            float normalLoadFz,
            float vx,
            float vy,
            float driveTorqueNm,
            float brakeTorqueNm,
            float effectiveMassLat,
            float dt,
            float surfaceMu
    ) {
        solveWheelStep(wheel, normalLoadFz, NOMINAL_TIRE_LOAD, vx, vy, driveTorqueNm, brakeTorqueNm, effectiveMassLat, dt, surfaceMu, 0.0f, 1.0f);
    }

    public static void solveWheelStep(
            WheelState wheel,
            float normalLoadFz,
            float vx,
            float vy,
            float driveTorqueNm,
            float brakeTorqueNm,
            float effectiveMassLat,
            float dt,
            float surfaceMu,
            float gammaOut,
            float side
    ) {
        solveWheelStep(wheel, normalLoadFz, NOMINAL_TIRE_LOAD, vx, vy, driveTorqueNm, brakeTorqueNm, effectiveMassLat, dt, surfaceMu, gammaOut, side);
    }

    /**
     * Вычисляет силы сцепления с учетом динамического угла развала (Camber Thrust) и динамической номинальной нагрузки.
     *
     * @param gammaOut угол развала колеса наружу (рад, > 0 = верх колеса наружу)
     * @param side     сторона колеса: +1.0f для левого борта, -1.0f для правого
     */
    public static void solveWheelStep(
            WheelState wheel,
            float normalLoadFz,
            float nominalLoadFz,
            float vx,
            float vy,
            float driveTorqueNm,
            float brakeTorqueNm,
            float effectiveMassLat,
            float dt,
            float surfaceMu,
            float gammaOut,
            float side
    ) {
        final float r = wheel.wheelRadius;
        wheel.camberRad = gammaOut;

        // Если колесо в воздухе (F_z <= 0) — сцепления с дорогой нет, вращаем колесо в воздухе
        if (normalLoadFz <= 0.001f) {
            wheel.outForceLongitudinal = 0.0f;
            wheel.outForceLateral = 0.0f;
            wheel.outCombinedSlipNorm = 0.0f;
            integrateWheelOmega(wheel, driveTorqueNm, 0.0f, brakeTorqueNm, dt);
            return;
        }

        // 1. ЧУВСТВИТЕЛЬНОСТЬ К НАГРУЗКЕ И РАЗВАЛУ
        float nomLoad = (nominalLoadFz > 100.0f) ? nominalLoadFz : NOMINAL_TIRE_LOAD;
        float loadOverloadRatio = (normalLoadFz - nomLoad) / nomLoad;
        float degressiveFactor = Mth.clamp(1.0f - LOAD_SENSITIVITY * loadOverloadRatio, 0.65f, 1.15f);
        float muPeak = surfaceMu * degressiveFactor;

        // Спад сцепления от отклонения развала (mu_eff = mu * max(0.6, 1 - kappa_g * (gamma - gamma_opt)^2))
        float dg = gammaOut - GAMMA_OPT;
        muPeak *= Math.max(0.6f, 1.0f - KAPPA_G * dg * dg);
        float maxForcePeak = muPeak * normalLoadFz;

        // 2. ДВУХМЕРНЫЙ КИНЕМАТИЧЕСКИЙ ВЕКТОР СКОЛЬЖЕНИЯ С CAMBER THRUST
        float wheelGroundSpeed = wheel.angularVelocity * r;
        float denomX = Math.max((float) Math.sqrt(vx * vx + 0.25f * wheelGroundSpeed * wheelGroundSpeed), V_MIN_LONG);
        float vDen = (float) Math.sqrt(vx * vx + V_MIN_LAT * V_MIN_LAT);

        float sigmaX = (wheelGroundSpeed - vx) / denomX;

        // Camber thrust вводится как эквивалентная добавка к скольжению:
        float psiV = (vx * vx) / (vx * vx + V_MIN_LONG * V_MIN_LONG);
        float sCam = R_GAMMA * side * gammaOut * psiV;
        float syEq = -vy / vDen + sCam;

        // Нормируем относительно пиковых порогов деформации протектора
        float sx = sigmaX / SLIP_RATIO_PEAK;
        float sy = syEq / SLIP_ANGLE_TAN_PEAK;
        float s = (float) Math.sqrt(sx * sx + sy * sy);
        wheel.outCombinedSlipNorm = s;

        // 3. ЕДИНАЯ СИЛА СЦЕПЛЕНИЯ F_total(s) (Brush Model + Dynamic Slide Falloff)
        float forceMag;
        if (s <= 1.0f) {
            // Упругая зона протектора: парабола Brush с гладкой вершиной в s = 1.0
            forceMag = maxForcePeak * s * (2.0f - s);
        } else {
            // Зона срыва в скольжение (пробуксовка или занос): спад от 1.0 до SLIDE_FRICTION_RATIO (0.74)
            float slideTransition = Math.min(1.0f, (s - 1.0f) * 0.40f);
            float curveFactor = 1.0f - (1.0f - SLIDE_FRICTION_RATIO) * slideTransition;
            forceMag = maxForcePeak * curveFactor;
        }

        // 4. РАЗЛОЖЕНИЕ ПО ЭЛЛИПСУ ТРЕНИЯ НА F_x И F_y
        float fx = 0.0f;
        float fy = 0.0f;
        if (s > 1e-5f) {
            float invS = 1.0f / s;
            fx = forceMag * (sx * invS);
            fy = forceMag * (sy * invS);
        }

        // Защита от численного дрожания боковой силы на околонулевых скоростях (Bilateral Clamp):
        if (effectiveMassLat > 0.0f && Math.abs(vy) < 1.2f && s < 0.8f) {
            float maxBilateralFy = (Math.abs(syEq) * vDen * effectiveMassLat) / dt;
            fy = Mth.clamp(fy, -maxBilateralFy, maxBilateralFy);
        }

        wheel.outForceLongitudinal = fx;
        wheel.outForceLateral = fy;
        wheel.outSlipAngleTanNorm = Math.abs(sy);

        // Пневматический трейл и самовыравнивающий момент (Brush Model):
        float thetaM = Math.abs(sy);
        if (thetaM < 1.0f) {
            float oneMinusTheta = 1.0f - thetaM;
            float tp = 0.033f * (oneMinusTheta * oneMinusTheta * oneMinusTheta) / Math.max(1e-4f, 1.0f - thetaM + (1.0f / 3.0f) * thetaM * thetaM);
            wheel.outAligningTorque = -fy * tp;
        } else {
            wheel.outAligningTorque = 0.0f;
        }

        // 5. ИНТЕГРАЦИЯ УГЛОВОЙ СКОРОСТИ КОЛЕСА (с реакцией дороги T_road = F_x * r)
        float roadReactionTorque = fx * r;
        integrateWheelOmega(wheel, driveTorqueNm, roadReactionTorque, brakeTorqueNm, dt);
    }

    /**
     * Полунеявный интегратор угловой скорости колеса с защитой от осцилляций тормоза около нуля (Zero-Crossing Clamp).
     */
    public static void integrateWheelOmega(
            WheelState wheel,
            float driveTorque,
            float roadTorque,
            float brakeTorque,
            float dt
    ) {
        // А. Интегрируем активную тягу привода и реакцию от дороги
        float netActiveTorque = driveTorque - roadTorque;
        float omegaFree = wheel.angularVelocity + (netActiveTorque * wheel.invWheelInertia) * dt;

        // Б. Неявное вязкое трение ступицы
        float omegaDamped = omegaFree / (1.0f + BEARING_VISCOUS_DAMP * wheel.invWheelInertia * dt);

        // В. Применяем сухое трение тормозных колодок (Zero-Crossing Clamp)
        float maxBrakeDeltaOmega = (Math.max(0.0f, brakeTorque) * wheel.invWheelInertia) * dt;
        if (Math.abs(omegaDamped) <= maxBrakeDeltaOmega) {
            // Тормозные колодки полностью заблокировали диск (omega = 0 без перескока знака!)
            wheel.angularVelocity = 0.0f;
        } else {
            wheel.angularVelocity = omegaDamped - Math.copySign(maxBrakeDeltaOmega, omegaDamped);
        }

        // Обновляем угол вращения для рендера 3D-модели колеса
        wheel.spinAngleRad = (wheel.spinAngleRad + wheel.angularVelocity * dt) % ((float) Math.PI * 200.0f);
    }
}
