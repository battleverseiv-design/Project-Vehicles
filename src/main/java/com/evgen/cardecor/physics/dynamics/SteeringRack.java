package com.evgen.cardecor.physics.dynamics;

import org.joml.Vector3f;

/**
 * Физическая динамическая система рулевого управления (2-й порядок).
 * Уравнение движения:
 * I_sw * d2(delta)/dt2 + c_sw * d(delta)/dt = Tau_driver + Tau_tire + Tau_kpi.
 *
 * Особенности реализации:
 * 1. Никакого lerp() угла руля к нулю. Самовозврат обеспечивается строго физическим моментом
 *    кастора и пневматического трейла шины.
 * 2. Аналитический пневматический трейл t_p(theta) выведен из параболического распределения
 *    давления щеточной модели шины (Brush Tire Model).
 * 3. Точная замкнутая геометрия Аккермана: cot(delta_L) - cot(delta_R) = track / wheelbase.
 * 4. Неголономное ограничение максимального угла руля через контактный импульс упора рейки.
 * 5. Вектор момента реакции шкворня на подрамник кузова для передачи в ODE4J rigid body.
 * 6. Внутренний 2-этапный substepping для абсолютной численной устойчивости на dt = 0.0125 с.
 */
public final class SteeringRack {

    // ------------------------------------------------------------------------
    // Физические константы механизма
    // ------------------------------------------------------------------------
    public float steeringInertia = 0.045f;      // I_sw [кг*м^2]
    public float steeringDamping = 0.28f;       // c_sw [Н*м*с/рад] (вязкое сопротивление рейки/ГУР)
    public float steeringRatio = 16.0f;         // SR [-]
    public float maxWheelSteerAngleRad = (float) Math.toRadians(34.0); // Лимит угла колеса [рад]
    public float mechanicalTrail = 0.025f;      // t_m [м] (плечо кастора)
    public float pneumaticTrailZero = 0.033f;   // t_p0 = a / 3 [м] (стат. пневмотрейл)
    public float casterAngleRad = (float) Math.toRadians(5.5); // Угол кастора [рад]
    public float kpiAngleRad = (float) Math.toRadians(11.0);    // Угол KPI [рад]
    public float maxDriverTorque = 14.0f;       // Предельный крутящий момент рук водителя [Н*м]

    // ------------------------------------------------------------------------
    // Динамические переменные состояния
    // ------------------------------------------------------------------------
    public float steeringWheelAngle = 0.0f;     // delta_sw [рад]
    public float steeringWheelVelocity = 0.0f;  // omega_sw [рад/с]

    // Выходные углы передних колес (Аккерман)
    public float leftWheelAngle = 0.0f;         // delta_L [рад]
    public float rightWheelAngle = 0.0f;        // delta_R [рад]

    // Вектор момента реакции на подрамник кузова в локальных осях автомобиля
    public final Vector3f chassisReactionTorque = new Vector3f();

    /**
     * Возвращает средний угол передних колес (виртуального центрального колеса) в радианах.
     */
    public float getCenterWheelAngle() {
        return this.steeringWheelAngle / this.steeringRatio;
    }

    /**
     * Точный физический вывод пневматического трейла t_p(theta) из параболического распределения давления Brush Model:
     * t_p(theta) = t_p0 * (1 - theta)^3 / (1 - theta + (1/3)*theta^2) при theta < 1,
     * t_p(theta) = 0 при theta >= 1.
     *
     * @param theta безразмерная деформация щетинок (s_y в BrushTireSolver)
     */
    public float calculatePneumaticTrail(float theta) {
        float absTheta = Math.abs(theta);
        if (absTheta >= 1.0f) {
            return 0.0f; // При полном скольжении центр сил строго совпадает с центром пятна контакта
        }
        float oneMinusTheta = 1.0f - absTheta;
        float numerator = oneMinusTheta * oneMinusTheta * oneMinusTheta;
        float denominator = 1.0f - absTheta + (1.0f / 3.0f) * absTheta * absTheta;
        return this.pneumaticTrailZero * (numerator / Math.max(1e-4f, denominator));
    }

    /**
     * Обновляет динамику рулевой колонки за шаг dt.
     *
     * @param driverInput   управляющее воздействие водителя [-1.0 ... +1.0]
     * @param fyLeft        боковая сила переднего левого колеса [Н]
     * @param fyRight       боковая сила переднего правого колеса [Н]
     * @param thetaLeft     безразмерный сдвиг щетинок FL
     * @param thetaRight    безразмерный сдвиг щетинок FR
     * @param trackWidth    колея [м]
     * @param wheelbase     колесная база [м]
     * @param dt            шаг моделирования (0.0125 с)
     */
    public void update(float driverInput, float fyLeft, float fyRight,
                       float thetaLeft, float thetaRight,
                       float trackWidth, float wheelbase, float dt) {

        // 1. Момент водителя с насыщением по силе рук (без мгновенной установки угла)
        float tauDriver = Math.max(-this.maxDriverTorque, Math.min(this.maxDriverTorque, driverInput * this.maxDriverTorque));

        // 2. Пневматический трейл из Brush Model
        float tpL = calculatePneumaticTrail(thetaLeft);
        float tpR = calculatePneumaticTrail(thetaRight);

        // 3. Восстанавливающие моменты вокруг шкворней (Kingpin Torque)
        float tauKpL = -fyLeft * (tpL + this.mechanicalTrail);
        float tauKpR = -fyRight * (tpR + this.mechanicalTrail);

        // Момент от шин, приведенный к рулевому валу
        float tauTireTotal = (tauKpL + tauKpR) / this.steeringRatio;

        // 4. Внутренний substepping (2 подшага) для безусловной устойчивости при резких ударах
        int subSteps = 2;
        float subDt = dt / subSteps;
        float maxSwAngle = this.maxWheelSteerAngleRad * this.steeringRatio;

        for (int step = 0; step < subSteps; step++) {
            // Полунеявный симплектический шаг (Semi-Implicit Euler с вязким затуханием)
            float totalTorque = tauDriver + tauTireTotal;
            float dampingFactor = 1.0f + (this.steeringDamping * subDt) / this.steeringInertia;

            this.steeringWheelVelocity = (this.steeringWheelVelocity + (totalTorque / this.steeringInertia) * subDt) / dampingFactor;

            // Физиологическое насыщение скорости вращения руля руками водителя (~8.0 рад/с ≈ 458°/с)
            float maxSwVelocity = 8.0f;
            this.steeringWheelVelocity = Math.max(-maxSwVelocity, Math.min(maxSwVelocity, this.steeringWheelVelocity));

            this.steeringWheelAngle += this.steeringWheelVelocity * subDt;

            // 5. Неголономное ограничение максимального угла (Impulse-based Steering Lock)
            if (Math.abs(this.steeringWheelAngle) > maxSwAngle) {
                float normal = Math.signum(this.steeringWheelAngle);
                float restitution = 0.05f; // Упругость резинового буфера упора

                if (this.steeringWheelVelocity * normal > 0.0f) {
                    this.steeringWheelVelocity = -restitution * this.steeringWheelVelocity;
                }
                this.steeringWheelAngle = normal * maxSwAngle;
            }
        }

        // 6. Аналитический Аккерман для раздельных углов левого и правого колес
        float centerWheelAngle = this.steeringWheelAngle / this.steeringRatio;
        solveAckermann(centerWheelAngle, trackWidth, wheelbase);

        // 7. Вектор момента реакции на подрамник кузова: Tau_reaction = -(Tau_kpL + Tau_kpR) * u_kingpin
        float totalKpTorque = tauKpL + tauKpR;
        float sinKpi = (float) Math.sin(this.kpiAngleRad);
        float cosKpi = (float) Math.cos(this.kpiAngleRad);
        float sinCaster = (float) Math.sin(this.casterAngleRad);
        float cosCaster = (float) Math.cos(this.casterAngleRad);

        this.chassisReactionTorque.set(
                -totalKpTorque * sinKpi,
                -totalKpTorque * (cosKpi * cosCaster),
                totalKpTorque * sinCaster
        );
    }

    /**
     * Аналитическое замкнутое решение уравнения Аккермана без сингулярностей при нулевом угле.
     */
    private void solveAckermann(float deltaC, float trackWidth, float wheelbase) {
        if (Math.abs(deltaC) < 1e-5f) {
            this.leftWheelAngle = 0.0f;
            this.rightWheelAngle = 0.0f;
            return;
        }

        float sinC = (float) Math.sin(deltaC);
        float cosC = (float) Math.cos(deltaC);
        float ackermannFactor = (trackWidth * 0.5f) / Math.max(0.5f, wheelbase);

        this.leftWheelAngle = (float) Math.atan2(sinC, cosC + ackermannFactor * sinC);
        this.rightWheelAngle = (float) Math.atan2(sinC, cosC - ackermannFactor * sinC);
    }
}
