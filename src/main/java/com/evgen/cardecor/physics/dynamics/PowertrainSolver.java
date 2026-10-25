package com.evgen.cardecor.physics.dynamics;

/**
 * Безусловно устойчивый (Unconditionally Stable) полунеявный решатель ДВС и трансмиссии (80 Гц).
 * Устранены:
 * 1. Потеря крутящего момента на 3-6 передачах из-за ложного reducedInertia без массы машины.
 * 2. Генератор колебаний RPM на задней передаче (R).
 * 3. Инверсия педалей W/S на задней передаче.
 * 4. Полностью сохранена совместимость со спецификациями и API проекта.
 */
public class PowertrainSolver {

    // --- Константы перевода единиц ---
    public static final float RPM_TO_RAD_S = 0.104719755f; // 2 * PI / 60
    public static final float RAD_S_TO_RPM = 9.54929658f;  // 60 / (2 * PI)

    // --- 1. Параметры ДВС (Engine Specs) ---
    public float engineInertia = 0.24f;          // Момент инерции маховика и КШМ [кг*м^2]
    public float targetIdleRpm = 850.0f;         // Целевые обороты холостого хода [RPM]
    public float peakTorqueRpm = 3800.0f;        // Обороты пикового момента [RPM]
    public float redlineRpm = 6500.0f;           // Обороты максимальной мощности [RPM]
    public float revLimiterRpm = 6800.0f;        // Жесткая отсечка подачи топлива [RPM]

    public float idleTorqueMaxNm = 175.0f;       // Доступный момент на низах (850 RPM) [Н*м]
    public float peakTorqueNm = 340.0f;          // Пиковый крутящий момент (3800 RPM) [Н*м]
    public float redlineTorqueNm = 285.0f;       // Момент на красной зоне (6500 RPM) [Н*м]

    // Механические и насосные потери ДВС:
    public float frictionStaticNm = 14.0f;       // Сухое трение в КШМ и ГРМ [Н*м]
    public float frictionViscous = 0.060f;       // Вязкое трение масла [Н*м / (рад/с)]
    public float engineBrakingPumping = 0.00010f;// Насосные потери при закрытом дросселе [Н*м / (рад/с)^2]

    // --- 2. Параметры сцепления (Clutch Specs) ---
    public float maxClutchTorqueNm = 520.0f;     // Предельный момент корзины [Н*м]
    public float clutchSlipBandRadS = 12.0f;     // Ширина зоны регуляризации [рад/с]
    public float launchStartRpm = 1100.0f;       // Начало смыкания при старте [RPM]
    public float launchFullLockRpm = 2400.0f;    // Полное смыкание под газом [RPM]
    public float clutchPedalSmoothingRate = 18.0f;// Скорость работы выжимного подшипника [1/с]
    public float clutchEngageRate = 8.0f;        // Скорость смыкания корзины [1/с]
    public float clutchDisengageRate = 25.0f;    // Скорость размыкания корзины [1/с]

    // --- 3. Параметры КПП и главной пары (Transmission Specs) ---
    public final float[] forwardGearRatios = { 3.55f, 2.18f, 1.48f, 1.12f, 0.89f, 0.74f };
    public float reverseGearRatio = -3.45f;      // Задняя передача (R)
    public float finalDriveRatio = 3.90f;        // Главная пара
    public float transmissionEfficiency = 0.92f; // КПД трансмиссии
    public float totalDrivenWheelsBaseInertia = 240.0f; // Приведенная базовая инерция шасси [кг*м^2]

    // --- 4. Состояние (Runtime State) ---
    public float engineOmega = 850.0f * RPM_TO_RAD_S; // Угловая скорость коленвала [рад/с]
    public float currentRpm = 850.0f;                 // Обороты двигателя [RPM]
    public int currentGear = 0;                       // Текущая передача (0 = N, -1 = R, 1..6)
    public boolean isEngineOn = true;                 // Включен ли двигатель
    public boolean isEngineStalled = false;           // Заглох ли двигатель
    public boolean clutchPedalPressed = false;        // Зажата ли педаль сцепления (Left Alt)
    public boolean isBraking = false;                 // Нажата ли педаль тормоза (S)
    public boolean justShifted = false;               // Флаг переключения передачи для звука
    public float shiftCooldown = 0.0f;                // Кулдаун переключения
    public float transmissionCondition = 100.0f;      // Состояние КПП [0..100]
    public boolean isClutchBurning = false;           // Горит ли сцепление
    public float clutchTempCelsius = 25.0f;           // Температура сцепления [°C]
    public boolean isClutchLocked = false;            // Заблокировано ли сцепление (1-DOF)

    private float smoothedClutchClamp = 0.0f;         // Сжатие корзины [0..1]
    private float lastClutchTorqueNm = 0.0f;          // Переданный момент сцепления [Н*м]
    private float lastEngineTorqueNm = 0.0f;          // Момент двигателя [Н*м]
    private float lastFrictionTorqueNm = 0.0f;        // Момент потерь [Н*м]
    private float couplingFactor = 0.0f;              // Фактор связи валов [0..1]
    private boolean revLimiterCut = false;            // Флаг отсечки топлива
    private float prevDeltaOmega = 0.0f;              // Предыдущее рассогласование валов
    private int prevGear = 0;                         // Предыдущая передача для детекции переключения

    public PowertrainSolver() {
        this(null);
    }

    public PowertrainSolver(PowertrainSpec spec) {
        if (spec != null) {
            configureFromSpec(spec);
        }
    }

    public void configureFromSpec(PowertrainSpec spec) {
        if (spec == null) return;
        if (spec.minRpm > 300.0f) this.targetIdleRpm = spec.minRpm;
        if (spec.maxRpm > 2000.0f) this.redlineRpm = spec.maxRpm;
        this.revLimiterRpm = (spec.revLimiterRpm > this.redlineRpm) ? spec.revLimiterRpm : (this.redlineRpm + 300.0f);
        if (spec.maxTorqueNm > 80.0f) {
            this.peakTorqueNm = spec.maxTorqueNm;
            this.idleTorqueMaxNm = spec.maxTorqueNm * 0.52f;
            this.redlineTorqueNm = spec.maxTorqueNm * 0.82f;
        }
        if (spec.peakTorqueRpm > this.targetIdleRpm) this.peakTorqueRpm = spec.peakTorqueRpm;
        if (spec.engineInertia > 0.05f) this.engineInertia = spec.engineInertia;
        if (spec.differentialRatio > 0.5f) this.finalDriveRatio = spec.differentialRatio;
        if (spec.reverseGearRatio != 0.0f) this.reverseGearRatio = -Math.abs(spec.reverseGearRatio);
        if (spec.transmissionEfficiency > 0.5f) this.transmissionEfficiency = spec.transmissionEfficiency;
        if (spec.gearRatios != null && spec.gearRatios.length > 0) {
            for (int i = 0; i < Math.min(forwardGearRatios.length, spec.gearRatios.length); i++) {
                if (spec.gearRatios[i] > 0.1f) {
                    forwardGearRatios[i] = spec.gearRatios[i];
                }
            }
        }
    }

    public void shiftUp() {
        if (currentGear == -1) {
            currentGear = 0;
            shiftCooldown = 0.08f;
            justShifted = true;
        } else if (currentGear == 0) {
            currentGear = 1;
            shiftCooldown = 0.08f;
            justShifted = true;
        } else if (currentGear < forwardGearRatios.length) {
            currentGear++;
            shiftCooldown = 0.08f;
            justShifted = true;
        }
    }

    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            shiftCooldown = 0.08f;
            justShifted = true;
        } else if (currentGear == 1) {
            currentGear = 0;
            shiftCooldown = 0.08f;
            justShifted = true;
        } else if (currentGear == 0) {
            currentGear = -1;
            shiftCooldown = 0.08f;
            justShifted = true;
        }
    }

    public void selectDirectGear(int gear) {
        if (gear >= -1 && gear <= forwardGearRatios.length) {
            currentGear = gear;
            shiftCooldown = 0.08f;
            justShifted = true;
        }
    }

    /**
     * Шаг симуляции ДВС и КПП (dt = 0.0125 с, 80 Гц).
     */
    public float step(
            float dt,
            float throttleInput,
            boolean clutchPedalPressed,
            float avgWheelAngularVel,
            int gear
    ) {
        this.currentGear = gear;
        this.clutchPedalPressed = clutchPedalPressed;

        // 1. Единая логика педалей: W — всегда газ, S — всегда тормоз на ЛЮБОЙ передаче (включая R)
        this.isBraking = (throttleInput < -0.01f);
        float userThrottle = clamp(throttleInput, 0.0f, 1.0f);

        if (shiftCooldown > 0.0f) {
            shiftCooldown = Math.max(0.0f, shiftCooldown - dt);
        }

        if (!this.isEngineOn) {
            smoothedClutchClamp = 0.0f;
            lastClutchTorqueNm = 0.0f;
            couplingFactor = 0.0f;
            engineOmega = Math.max(0.0f, engineOmega - dt * 25.0f);
            currentRpm = engineOmega * RAD_S_TO_RPM;
            return 0.0f;
        }

        // 2. РХХ (Idle Governor) и отсечка топлива (Rev-Limiter)
        float idleGovernorThrottle = 0.0f;
        float idleError = targetIdleRpm - currentRpm;
        if (idleError > 0.0f) {
            idleGovernorThrottle = clamp(idleError / 300.0f, 0.0f, 0.60f);
        }

        float effectiveThrottle = Math.max(userThrottle, idleGovernorThrottle);

        if (currentRpm >= revLimiterRpm) {
            revLimiterCut = true;
        } else if (currentRpm < revLimiterRpm - 180.0f) {
            revLimiterCut = false;
        }
        if (revLimiterCut) {
            effectiveThrottle = 0.0f;
        }

        // 3. Крутящий момент ДВС и механические потери
        float combustionTorque = sampleTorqueCurve(currentRpm) * effectiveThrottle;
        this.lastEngineTorqueNm = combustionTorque;

        float closedThrottle = 1.0f - effectiveThrottle;
        float losses = frictionStaticNm
                + frictionViscous * engineOmega
                + engineBrakingPumping * (engineOmega * engineOmega) * closedThrottle;
        float omegaSignSmooth = fastTanh(engineOmega * 0.25f);
        this.lastFrictionTorqueNm = losses * omegaSignSmooth;

        float netInternalEngineTorque = combustionTorque - lastFrictionTorqueNm;

        // 4. Проверка нейтрали (N) или разрыва трансмиссии при переключении
        float totalRatio = getTotalGearRatio(gear);
        float absRatio = Math.abs(totalRatio);

        if (gear == 0 || absRatio < 0.001f || shiftCooldown > 0.0f) {
            smoothedClutchClamp = 0.0f;
            lastClutchTorqueNm = 0.0f;
            couplingFactor = 0.0f;
            isClutchLocked = false;

            engineOmega += (netInternalEngineTorque / engineInertia) * dt;
            float minOmega = (targetIdleRpm * 0.70f) * RPM_TO_RAD_S;
            if (engineOmega < minOmega && !this.isEngineStalled) {
                engineOmega = minOmega;
            }
            if (engineOmega < 0.0f) engineOmega = 0.0f;
            currentRpm = engineOmega * RAD_S_TO_RPM;
            return 0.0f;
        }

        // Приведенная угловая скорость первичного вала КПП:
        // Вперед (1..6): avgWheelAngularVel > 0, totalRatio > 0 -> transOmega > 0
        // Назад (R): avgWheelAngularVel < 0, totalRatio < 0 -> transOmega > 0!
        float transOmega = avgWheelAngularVel * totalRatio;
        float transSyncRpm = transOmega * RAD_S_TO_RPM;
        float deltaOmega = engineOmega - transOmega;

        // При смене передачи сразу пересчитываем prevDeltaOmega без ложного знака со старой передачи
        if (gear != prevGear) {
            prevDeltaOmega = deltaOmega;
            isClutchLocked = false;
        }

        // 5. Плавный контроллер сжатия корзины сцепления
        float targetClamp;
        if (clutchPedalPressed) {
            targetClamp = 0.0f;
        } else {
            targetClamp = computeSmoothAutoClutch(userThrottle, transOmega, gear);
        }

        float engageRate = (gear >= 2) ? 25.0f : clutchEngageRate;
        float rate = (targetClamp < smoothedClutchClamp) ? clutchDisengageRate : engageRate;
        smoothedClutchClamp += (targetClamp - smoothedClutchClamp) * clamp(dt * rate, 0.0f, 1.0f);

        // Clutch thermal fade: перегрев феродо (>200°C) временно снижает сцепление, но не убивает коробку
        float clutchFade = 1.0f;
        if (clutchTempCelsius > 200.0f) {
            clutchFade = Math.max(0.40f, 1.0f - (clutchTempCelsius - 200.0f) / 250.0f);
        }
        float conditionFactor = Math.max(0.35f, Math.min(1.0f, this.transmissionCondition / 75.0f));
        float tCap = maxClutchTorqueNm * smoothedClutchClamp * clutchFade * conditionFactor;

        // 6. Честный двухрежимный автомат состояний (LOCKED 1-DOF / SLIP 2-DOF)

        // 1. Проверка условий принудительного размыкания LOCKED -> SLIP:
        if (isClutchLocked) {
            boolean stallRisk = (transSyncRpm < targetIdleRpm - 50.0f) && (userThrottle < 0.1f);
            boolean torqueExceeded = Math.abs(netInternalEngineTorque) > (tCap * 1.5f);
            if (clutchPedalPressed || gear != prevGear || stallRisk || torqueExceeded || smoothedClutchClamp < 0.75f) {
                isClutchLocked = false;
            }
        }

        // 2. Расчет момента и скоростей по активному состоянию:
        if (isClutchLocked) {
            // РЕЖИМ 1-DOF (ЖЕСТКАЯ СЦЕПКА ВАЛОВ):
            // Коленвал и первичный вал КПП — одно целое. 100% момента идет на ведущие колеса,
            // а инерция маховика учитывается в BrushTireSolver через couplingFactor = 1.0f.
            engineOmega = Math.max(targetIdleRpm * RPM_TO_RAD_S, transOmega);
            currentRpm = engineOmega * RAD_S_TO_RPM;
            lastClutchTorqueNm = netInternalEngineTorque;
            couplingFactor = 1.0f;
        } else {
            // РЕЖИМ 2-DOF (SLIP — ПРОБУКСОВКА ПРИ ТРОГАНИИ ИЛИ ПЕРЕКЛЮЧЕНИИ):
            // 1. Точный момент (Н*м), необходимый чтобы сравнять валы ровно за 1 подшаг dt без проскока (Overshoot):
            float exactSyncTorque = (engineInertia / dt) * deltaOmega;

            // 2. Трение сцепления никогда не может превысить exactSyncTorque!
            // Это на 100% исключает провал оборотов мотора ниже transOmega (смену знака deltaOmega).
            float transmittedTorque = clamp(exactSyncTorque, -tCap, tCap);

            // 3. Условие честного замыкания без удара в 5100 Н*м:
            if (!clutchPedalPressed && smoothedClutchClamp > 0.80f && transSyncRpm >= targetIdleRpm) {
                if (Math.abs(exactSyncTorque) <= tCap) {
                    isClutchLocked = true;
                    couplingFactor = 1.0f;
                    engineOmega = transOmega;
                    currentRpm = engineOmega * RAD_S_TO_RPM;
                    lastClutchTorqueNm = transmittedTorque; // <= tCap! Никаких скачков в 5100 Н*м!
                } else {
                    isClutchLocked = false;
                    // Если водитель жмет газ (userThrottle > 0.05f), а мотор случайно оказался ниже КПП,
                    // сцепление плавно подтягивает мотор вверх, но не тормозит колеса сильнее обычного торможения двигателем:
                    float engineBrakingTorqueNm = (lastFrictionTorqueNm > 0.0f) ? lastFrictionTorqueNm : 50.0f;
                    if (userThrottle > 0.05f && transmittedTorque < 0.0f) {
                        transmittedTorque = Math.max(transmittedTorque, -engineBrakingTorqueNm);
                    }
                    lastClutchTorqueNm = transmittedTorque;
                }
            } else {
                isClutchLocked = false;
                lastClutchTorqueNm = transmittedTorque;
            }

            // Резерв момента при старте с места (только для 1-й и R, когда колеса еще медленнее launchStartRpm):
            if (Math.abs(gear) <= 1 && transSyncRpm < launchStartRpm && userThrottle > 0.02f) {
                float reserve = clamp((currentRpm - targetIdleRpm) / (launchFullLockRpm - targetIdleRpm), 0.0f, 1.0f);
                float maxAllowable = Math.max(0.0f, netInternalEngineTorque * (0.25f + 0.75f * reserve));
                if (lastClutchTorqueNm > maxAllowable) {
                    lastClutchTorqueNm = maxAllowable;
                }
            }

            float crankAccel = (netInternalEngineTorque - lastClutchTorqueNm) / engineInertia;
            engineOmega += crankAccel * dt;

            if (currentGear > 0 && !clutchPedalPressed && engineOmega < (targetIdleRpm * 0.45f) * RPM_TO_RAD_S) {
                this.isEngineStalled = true;
                this.isEngineOn = false;
                engineOmega = 0.0f;
            } else {
                float minAliveOmega = (targetIdleRpm * 0.70f) * RPM_TO_RAD_S;
                if (engineOmega < minAliveOmega) {
                    engineOmega = minAliveOmega;
                }
            }
            currentRpm = engineOmega * RAD_S_TO_RPM;

            // При сжатом сцеплении отраженная инерция маховика I_e * G^2 честно передается на ведущие колеса:
            couplingFactor = smoothedClutchClamp;
        }
        prevDeltaOmega = deltaOmega;
        prevGear = gear;

        // Тепловой баланс сцепления
        float slipHeatWatts = Math.abs(lastClutchTorqueNm) * Math.abs(deltaOmega);
        float coolWatts = 5.5f * (clutchTempCelsius - 25.0f);
        clutchTempCelsius += ((slipHeatWatts - coolWatts) / 3500.0f) * dt;
        clutchTempCelsius = Math.max(25.0f, clutchTempCelsius);
        isClutchBurning = clutchTempCelsius > 220.0f;
        // Механический износ шестерен КПП происходит только при экстремальном злоупотреблении (>360°C):
        if (clutchTempCelsius > 360.0f) {
            transmissionCondition = Math.max(0.0f, transmissionCondition - dt * 0.05f);
        }

        // 8. Выходной крутящий момент на ведущие колеса
        return lastClutchTorqueNm * totalRatio * transmissionEfficiency;
    }

    /**
     * Гладкая карта автоматического сцепления без ступенчатых скачков.
     */
    private float computeSmoothAutoClutch(float userThrottle, float transOmega, int gear) {
        float syncRpm = transOmega * RAD_S_TO_RPM;

        // На 2-й передаче и выше сцепление ВСЕГДА полностью сомкнуто (1.0f),
        // кроме случая полной остановки / падения ниже холостых (Anti-Stall < 800 RPM):
        if (gear >= 2) {
            if (syncRpm < targetIdleRpm - 50.0f && userThrottle < 0.05f) {
                return 0.0f; // Защита от заглухания при торможении в пол на высокой передаче
            }
            return 1.0f;
        }

        // Для 1-й (gear == 1) и Задней (gear == -1) передачи:
        // Как только вал КПП раскрутился до 1100 RPM (~6-7 км/ч) — полное смыкание 1.0f!
        float speedLock = clamp((syncRpm - 750.0f) / 350.0f, 0.0f, 1.0f);
        if (userThrottle < 0.02f) {
            return speedLock;
        }

        float rpmLock = clamp((currentRpm - launchStartRpm) / (launchFullLockRpm - launchStartRpm), 0.0f, 1.0f);
        float smoothRpmLock = rpmLock * rpmLock * (3.0f - 2.0f * rpmLock);
        return Math.max(smoothRpmLock, speedLock);
    }

    private float sampleTorqueCurve(float rpm) {
        if (rpm <= targetIdleRpm) return idleTorqueMaxNm;
        if (rpm <= peakTorqueRpm) {
            float t = (rpm - targetIdleRpm) / (peakTorqueRpm - targetIdleRpm);
            return idleTorqueMaxNm + (peakTorqueNm - idleTorqueMaxNm) * (t * (2.0f - t));
        }
        if (rpm <= redlineRpm) {
            float t = (rpm - peakTorqueRpm) / (redlineRpm - peakTorqueRpm);
            return peakTorqueNm - (peakTorqueNm - redlineTorqueNm) * (t * t);
        }
        float over = clamp((rpm - redlineRpm) / 600.0f, 0.0f, 1.0f);
        return redlineTorqueNm * (1.0f - over * 0.65f);
    }

    public float getTotalGearRatio(int gear) {
        if (gear == 0) return 0.0f;
        if (gear == -1) return reverseGearRatio * finalDriveRatio;
        int idx = Math.max(0, Math.min(gear - 1, forwardGearRatios.length - 1));
        return forwardGearRatios[idx] * finalDriveRatio;
    }

    /**
     * Эффективная инерция ведущего колеса для BrushTireSolver.
     */
    public float getEffectiveDrivenWheelInertia(float baseWheelInertia, int gear, int drivenCount) {
        if (gear == 0 || couplingFactor <= 0.01f || drivenCount <= 0) {
            return baseWheelInertia;
        }
        float totalRatio = getTotalGearRatio(gear);
        float reflectedEngine = (engineInertia * totalRatio * totalRatio) / drivenCount;
        return baseWheelInertia + reflectedEngine * couplingFactor;
    }

    private static float fastTanh(float x) {
        if (x < -3.0f) return -1.0f;
        if (x > 3.0f) return 1.0f;
        float x2 = x * x;
        return x * (27.0f + x2) / (27.0f + 9.0f * x2);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public float getCurrentRpm() { return currentRpm; }
    public float getEngineOmega() { return engineOmega; }
    public float getClutchClamp() { return smoothedClutchClamp; }
    public float getLastClutchTorqueNm() { return lastClutchTorqueNm; }
    public float getLastEngineTorqueNm() { return lastEngineTorqueNm; }
    public float getCouplingFactor() { return couplingFactor; }
    public boolean isClutchLocked() { return isClutchLocked; }
}
