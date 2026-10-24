package com.evgen.cardecor.physics.dynamics;

/**
 * Физически строгий, численно безусловный (Unconditionally Stable) решатель ДВС,
 * сцепления и КПП для частоты 80 Гц (dt = 0.0125 с).
 * Полностью Zero-GC (только примитивы float/int).
 */
public class PowertrainSolver {

    // --- Константы перевода единиц ---
    public static final float RPM_TO_RAD_S = 0.104719755f; // 2 * PI / 60
    public static final float RAD_S_TO_RPM = 9.54929658f;  // 60 / (2 * PI)

    // --- 1. Параметры ДВС (Engine Specs) ---
    public float engineInertia = 0.22f;          // Момент инерции маховика и КШМ [кг*м^2]
    public float targetIdleRpm = 850.0f;         // Целевые обороты холостого хода [RPM]
    public float peakTorqueRpm = 3800.0f;        // Обороты пикового момента [RPM]
    public float redlineRpm = 6500.0f;           // Обороты максимальной мощности [RPM]
    public float revLimiterRpm = 6800.0f;        // Жесткая отсечка подачи топлива [RPM]

    public float idleTorqueMaxNm = 165.0f;       // Доступный момент на низах (850 RPM) [Н*м]
    public float peakTorqueNm = 320.0f;          // Пиковый крутящий момент (3800 RPM) [Н*м]
    public float redlineTorqueNm = 265.0f;       // Момент на красной зоне (6500 RPM) [Н*м]

    // Механические и насосные потери ДВС (Pumping & Friction Losses):
    // T_friction = c0 + c1 * omega + c2 * omega^2
    public float frictionStaticNm = 14.0f;       // Сухое трение в КШМ и ГРМ [Н*м]
    public float frictionViscous = 0.065f;       // Вязкое трение масла [Н*м / (рад/с)]
    public float engineBrakingPumping = 0.00011f;// Насосные потери при закрытом дросселе [Н*м / (рад/с)^2]

    // --- 2. Параметры сцепления (Clutch Specs) ---
    public float maxClutchTorqueNm = 480.0f;     // Максимальный момент корзины сцепления [Н*м]
    public float clutchSlipBandRadS = 12.0f;     // Ширина зоны регуляризации Кулона (tanh) [рад/с]
    public float launchStartRpm = 1150.0f;       // Обороты начала схватывания авто-сцепления [RPM]
    public float launchFullLockRpm = 2550.0f;    // Обороты полного зажатия при старте под газом [RPM]
    public float clutchPedalSmoothingRate = 18.0f;// Скорость работы привода выжимного подшипника [1/с]

    // --- 3. Параметры КПП и главной пары (Transmission Specs) ---
    // Индексы: [0] = 1-я, [1] = 2-я, [2] = 3-я, [3] = 4-я, [4] = 5-я, [5] = 6-я
    public final float[] forwardGearRatios = { 3.55f, 2.18f, 1.48f, 1.12f, 0.89f, 0.74f };
    public float reverseGearRatio = -3.45f;      // Задняя передача (R)
    public float finalDriveRatio = 3.90f;        // Главная пара редуктора
    public float transmissionEfficiency = 0.90f; // КПД трансмиссии (потери в шестернях)

    // Оценка суммарной собственной инерции ведущих колес со ступицами и полуосями [кг*м^2]
    // Используется только для расчета порога анти-осцилляционного демпфирования валов
    public float totalDrivenWheelsBaseInertia = 240.0f; // 2 колеса + приведенная масса кузова ~1650 кг на плече R_w^2

    // --- 4. Динамическое состояние (Runtime State) ---
    public float engineOmega = 850.0f * RPM_TO_RAD_S; // Текущая угловая скорость коленвала [рад/с]
    public float currentRpm = 850.0f;                 // Текущие обороты [RPM]
    public int currentGear = 0;                       // Текущая передача (0 = N, -1 = R, 1..6 = передние)
    public boolean isEngineOn = true;                 // Зажигание / работа двигателя
    public boolean isEngineStalled = false;           // Флаг заглохания
    public boolean clutchPedalPressed = false;        // Left Alt выжат
    public boolean isBraking = false;                 // Педаль тормоза (S)
    public boolean justShifted = false;               // Флаг для звука переключения передач
    public float shiftCooldown = 0.0f;                // Кулдаун переключения
    public float transmissionCondition = 100.0f;      // Состояние КПП [0..100]
    public boolean isClutchBurning = false;           // Флаг перегрева сцепления
    public float clutchTempCelsius = 25.0f;           // Температура сцепления

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
        this.targetIdleRpm = spec.minRpm;
        this.redlineRpm = spec.maxRpm;
        this.revLimiterRpm = spec.revLimiterRpm;
        this.peakTorqueNm = spec.maxTorqueNm;
        this.peakTorqueRpm = spec.peakTorqueRpm;
        this.engineInertia = spec.engineInertia;
        this.finalDriveRatio = spec.differentialRatio;
        this.reverseGearRatio = spec.reverseGearRatio;
        this.transmissionEfficiency = spec.transmissionEfficiency;
        if (spec.gearRatios != null && spec.gearRatios.length > 0) {
            for (int i = 0; i < Math.min(forwardGearRatios.length, spec.gearRatios.length); i++) {
                forwardGearRatios[i] = spec.gearRatios[i];
            }
        }
    }

    private float smoothedClutchClamp = 0.0f;          // Фактический прижим корзины [0..1]
    private float lastClutchTorqueNm = 0.0f;           // Переданный момент сцепления [Н*м]
    private float lastEngineTorqueNm = 0.0f;           // Момент сгорания ДВС [Н*м]
    private float lastFrictionTorqueNm = 0.0f;         // Момент потерь ДВС [Н*м]
    private float couplingFactor = 0.0f;               // Степень жесткости сцепки валов [0 = SLIP, 1 = LOCKED]
    private boolean revLimiterCut = false;             // Гистерезисный триггер отсечки

    public void shiftUp() {
        if (currentGear == -1) {
            currentGear = 0;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear == 0) {
            currentGear = 1;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear < forwardGearRatios.length) {
            currentGear++;
            shiftCooldown = 0.15f;
            justShifted = true;
        }
    }

    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear == 1) {
            currentGear = 0;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear == 0) {
            currentGear = -1;
            shiftCooldown = 0.15f;
            justShifted = true;
        }
    }

    public void selectDirectGear(int gear) {
        if (gear >= -1 && gear <= forwardGearRatios.length) {
            currentGear = gear;
            shiftCooldown = 0.15f;
            justShifted = true;
        }
    }

    /**
     * Главный шаг интегрирования силовой установки (вызывать с частотой 80 Hz, dt = 0.0125 с).
     *
     * @param dt                 шаг времени симуляции [с] (0.0125f)
     * @param throttleInput      положение педали газа от игрока [0.0 .. 1.0] (клавиша W)
     * @param clutchPedalPressed true, если зажат Left Alt (сцепление выжато = разомкнуто)
     * @param avgWheelAngularVel средняя угловая скорость ведущих колес из шинного солвера [рад/с]
     * @param currentGear        текущая передача (-1 = R, 0 = N, 1..6 = передние передачи)
     * @return суммарный крутящий момент на ведущих колесах T_wheels [Н*м]
     */
    public float step(
            float dt,
            float throttleInput,
            boolean clutchPedalPressed,
            float avgWheelAngularVel,
            int currentGear
    ) {
        this.currentGear = currentGear;
        this.clutchPedalPressed = clutchPedalPressed;

        float userThrottle;
        if (currentGear == -1) {
            // На задней передаче: клавиша S (throttleInput < -0.01) — это газ назад!
            if (throttleInput < -0.01f) {
                userThrottle = Math.abs(throttleInput);
                this.isBraking = false;
            } else if (throttleInput > 0.01f) {
                // Клавиша W на заднем ходу — это рабочий тормоз
                userThrottle = 0.0f;
                this.isBraking = true;
            } else {
                userThrottle = 0.0f;
                this.isBraking = false;
            }
        } else {
            // Передние передачи и нейтраль: W — газ, S — тормоз
            this.isBraking = (throttleInput < -0.01f);
            userThrottle = clamp(throttleInput, 0.0f, 1.0f);
        }

        if (shiftCooldown > 0.0f) {
            shiftCooldown = Math.max(0.0f, shiftCooldown - dt);
        }

        // Если двигатель заглушен — плавное затухание оборотов
        if (!this.isEngineOn) {
            smoothedClutchClamp = 0.0f;
            lastClutchTorqueNm = 0.0f;
            couplingFactor = 0.0f;
            engineOmega = Math.max(0.0f, engineOmega - dt * 25.0f);
            currentRpm = engineOmega * RAD_S_TO_RPM;
            return 0.0f;
        }

        // ---------------------------------------------------------------------
        // ШАГ 1: РЕГУЛЯТОР ХОЛОСТОГО ХОДА (Idle Governor / РХХ) И ОТСЕЧКА (Rev Limiter)
        // ---------------------------------------------------------------------
        float idleGovernorThrottle = 0.0f;
        float idleRpmError = targetIdleRpm - currentRpm;
        if (idleRpmError > 0.0f) {
            // Пропорционально-нелинейный РХХ: плавно открывает дроссельную заслонку,
            // если обороты проседают ниже 850 RPM (компенсирует трение и плавное касание сцепления)
            idleGovernorThrottle = clamp(idleRpmError / 320.0f, 0.0f, 0.55f);
        }

        // Итоговое открытие дросселя — максимум между педалью водителя и каналом РХХ
        float effectiveThrottle = Math.max(userThrottle, idleGovernorThrottle);

        // Гистерезисная отсечка топлива (Fuel Cut Rev-Limiter):
        // Вырубает подачу топлива на 6800 RPM и включает обратно на 6680 RPM
        if (currentRpm >= revLimiterRpm) {
            revLimiterCut = true;
        } else if (currentRpm < revLimiterRpm - 120.0f) {
            revLimiterCut = false;
        }
        if (revLimiterCut) {
            effectiveThrottle = 0.0f;
        }

        // ---------------------------------------------------------------------
        // ШАГ 2: РАСЧЕТ МОМЕНТА СГОРАНИЯ (T_engine) И МЕХАНИЧЕСКИХ ПОТЕРЬ (T_friction)
        // ---------------------------------------------------------------------
        float maxAvailableTorque = sampleTorqueCurve(currentRpm);
        lastEngineTorqueNm = maxAvailableTorque * effectiveThrottle;

        // Механические потери + насосные потери торможения двигателем (растут при закрытом дросселе)
        float closedThrottleFactor = 1.0f - effectiveThrottle;
        float viscousAndStatic = frictionStaticNm + frictionViscous * engineOmega;
        float pumpingBrake = engineBrakingPumping * (engineOmega * engineOmega) * closedThrottleFactor;

        // Трение действует строго против вращения коленвала и затухает около 0 рад/с
        float omegaSignSmooth = fastTanh(engineOmega * 0.25f);
        lastFrictionTorqueNm = (viscousAndStatic + pumpingBrake) * omegaSignSmooth;

        // Чистый внутренний момент на коленвале до корзины сцепления
        float netInternalEngineTorque = lastEngineTorqueNm - lastFrictionTorqueNm;

        // ---------------------------------------------------------------------
        // ШАГ 3: КИНЕМАТИКА КПП И РЕЖИМ НЕЙТРАЛИ (N)
        // ---------------------------------------------------------------------
        float totalRatio = getTotalGearRatio(currentGear);
        float absRatio = Math.abs(totalRatio);

        // Если включена Нейтраль (N, gear == 0) или идет смена передачи: связь разорвана, мотор свободно крутит маховик
        if (currentGear == 0 || absRatio < 0.001f || shiftCooldown > 0.0f) {
            smoothedClutchClamp = 0.0f;
            lastClutchTorqueNm = 0.0f;
            couplingFactor = 0.0f;

            engineOmega += (netInternalEngineTorque / engineInertia) * dt;
            if (engineOmega < (targetIdleRpm * 0.85f) * RPM_TO_RAD_S && !this.isEngineStalled) {
                engineOmega = (targetIdleRpm * 0.85f) * RPM_TO_RAD_S;
            }
            if (engineOmega < 0.0f) engineOmega = 0.0f;
            currentRpm = engineOmega * RAD_S_TO_RPM;
            return 0.0f;
        }

        // Скорость первичного вала КПП (приведенная от колес): omega_trans = omega_wheels * G_total
        float transOmega = avgWheelAngularVel * totalRatio;
        // Скорость скольжения дисков сцепления: delta_omega = omega_engine - omega_trans
        float deltaOmega = engineOmega - transOmega;

        // ---------------------------------------------------------------------
        // ШАГ 4: ПРИЖИМ КОРЗИНЫ СЦЕПЛЕНИЯ (T_cap) И ANTI-STALL LAUNCH КОНТРОЛЛЕР
        // ---------------------------------------------------------------------
        float targetClamp;
        if (clutchPedalPressed) {
            // Водитель зажал Left Alt — выжимной подшипник полностью разжал диски
            targetClamp = 0.0f;
        } else {
            // Left Alt отпущен: рассчитываем естественный центробежный/анти-столл прижим для клавиатуры
            targetClamp = computeKeyboardAutoClutchClamp(userThrottle, transOmega);
        }

        // Плавная отработка гидравлики сцепления (без ступенчатых ударов за 1 тик)
        float alphaClamp = clamp(dt * clutchPedalSmoothingRate, 0.0f, 1.0f);
        // Размыкание при нажатии Alt или падении оборотов происходит быстрее, чем смыкание
        if (targetClamp < smoothedClutchClamp) {
            alphaClamp = clamp(dt * clutchPedalSmoothingRate * 2.2f, 0.0f, 1.0f);
        }
        smoothedClutchClamp += (targetClamp - smoothedClutchClamp) * alphaClamp;

        float conditionFactor = Math.max(0.35f, Math.min(1.0f, this.transmissionCondition / 75.0f));
        float tCap = maxClutchTorqueNm * smoothedClutchClamp * conditionFactor;

        // ---------------------------------------------------------------------
        // ШАГ 5: НЕПРЕРЫВНЫЙ КУЛОНОВСКИЙ СОЛВЕР СЦЕПЛЕНИЯ БЕЗ ДЕДЛОКОВ
        // ---------------------------------------------------------------------
        if (tCap <= 0.01f) {
            lastClutchTorqueNm = 0.0f;
            couplingFactor = 0.0f;
        } else {
            // 5.1. Гладкий закон сухого трения Кулона с регуляризацией через гиперболический тангенс:
            // T_raw = T_cap * tanh(delta_omega / slip_band)
            float slipNormalized = deltaOmega / clutchSlipBandRadS;
            float coulombRawTorque = tCap * fastTanh(slipNormalized);

            // 5.2. Аналитический предел импульса за шаг dt (защита от численного перелета deltaOmega через 0):
            // Приведенная инерция колес на первичном валу КПП: J_trans = I_wheels / G^2
            float invRatio = 1.0f / totalRatio;
            float reflectedWheelInertia = Math.max(0.005f, totalDrivenWheelsBaseInertia * invRatio * invRatio);
            float reducedInertia = (engineInertia * reflectedWheelInertia) / (engineInertia + reflectedWheelInertia);

            // Максимальный момент, который требуется, чтобы полностью синхронизировать валы ровно за шаг dt
            // с учетом текущего ускорения коленвала (без какого-либо жесткого зануления скорости!):
            float freeEngineAccel = netInternalEngineTorque / engineInertia;
            float maxSyncTorque = reducedInertia * ((deltaOmega / dt) + freeEngineAccel);

            // Если требуемый для синхронизации момент укладывается в емкость корзины tCap:
            // Валы сомкнуты (LOCKED) — передаем полный рабочий момент ДВС без искусственной просадки в ноль!
            // Если нагрузка выше tCap — сцепление пробуксовывает (SLIP) с предельным моментом Кулона.
            if (Math.abs(maxSyncTorque) <= tCap && (maxSyncTorque * deltaOmega >= -1.0f)) {
                lastClutchTorqueNm = maxSyncTorque;
            } else {
                lastClutchTorqueNm = coulombRawTorque;
            }

            // Дополнительно гарантируем физический предел корзины [-tCap .. +tCap]
            lastClutchTorqueNm = clamp(lastClutchTorqueNm, -tCap, tCap);

            // 5.3. Защита от удушения мотора стоящими колесами (Anti-Deadlock Torque Reserve):
            // Если колеса еще почти стоят, а водитель жмет газ, не позволяем сцеплению отбирать > 80% момента
            if (userThrottle > 0.05f && Math.abs(transOmega) < targetIdleRpm * RPM_TO_RAD_S && currentRpm < launchFullLockRpm) {
                float maxAllowedLoadOnLaunch = Math.max(0.0f, netInternalEngineTorque * 0.80f);
                if (Math.abs(lastClutchTorqueNm) > maxAllowedLoadOnLaunch) {
                    lastClutchTorqueNm = Math.copySign(maxAllowedLoadOnLaunch, lastClutchTorqueNm);
                }
            }

            // Гладкий коэффициент жесткости связи валов [0..1] для шинного солвера:
            float absDelta = Math.abs(deltaOmega);
            couplingFactor = smoothedClutchClamp * (1.0f - clamp(absDelta / (clutchSlipBandRadS * 1.5f), 0.0f, 1.0f));
        }

        // Термодинамика сцепления
        float slipHeatWatts = Math.abs(lastClutchTorqueNm) * Math.abs(deltaOmega);
        float coolWatts = 4.5f * (clutchTempCelsius - 25.0f);
        clutchTempCelsius += ((slipHeatWatts - coolWatts) / 2500.0f) * dt;
        clutchTempCelsius = Math.max(25.0f, clutchTempCelsius);
        isClutchBurning = clutchTempCelsius > 220.0f;
        if (clutchTempCelsius > 280.0f) {
            transmissionCondition = Math.max(0.0f, transmissionCondition - dt * 0.5f);
        }

        // ---------------------------------------------------------------------
        // ШАГ 6: ИНТЕГРИРОВАНИЕ УРАВНЕНИЯ ДВИЖЕНИЯ МАХОВИКА ДВС
        // I_engine * (d(omega_engine)/dt) = T_engine - T_friction - T_clutch
        // ---------------------------------------------------------------------
        float totalTorqueOnCrankshaft = netInternalEngineTorque - lastClutchTorqueNm;
        engineOmega += (totalTorqueOnCrankshaft / engineInertia) * dt;

        // Физическое заглохание: если на высокой передаче без сцепления обороты упали критически низко
        if (currentGear > 0 && !clutchPedalPressed && engineOmega < (targetIdleRpm * 0.45f) * RPM_TO_RAD_S) {
            this.isEngineStalled = true;
            this.isEngineOn = false;
            engineOmega = 0.0f;
        } else {
            // Физический пол оборотов при работающем зажигании
            float minAliveOmega = (targetIdleRpm * 0.65f) * RPM_TO_RAD_S;
            if (engineOmega < minAliveOmega) {
                engineOmega = minAliveOmega;
            }
        }

        currentRpm = engineOmega * RAD_S_TO_RPM;

        // ---------------------------------------------------------------------
        // ШАГ 7: МОМЕНТ НА ВЕДУЩИЕ КОЛЕСА
        // T_wheels = T_clutch * totalRatio * transmissionEfficiency
        // ---------------------------------------------------------------------
        return lastClutchTorqueNm * totalRatio * transmissionEfficiency;
    }

    /**
     * Вычисляет естественный прижим сцепления (0..1) при отпущенной клавише Alt.
     * Позволяет плавно трогаться одной клавишей W без спама сцеплением и не глохнуть на месте.
     */
    private float computeKeyboardAutoClutchClamp(float userThrottle, float transOmega) {
        float transSyncRpm = Math.abs(transOmega) * RAD_S_TO_RPM;

        // 1. Если машина уже едет быстрее скорости холостого хода на данной передаче (например > 1100 RPM по валу КПП),
        // сцепление полностью сомкнуто (1.0f), обеспечивая 100% передачу тяги и честное торможение двигателем.
        if (transSyncRpm > launchStartRpm + 250.0f) {
            return 1.0f;
        }

        // 2. Если машина стоит или ползет, и водитель НЕ жмет газ (W отпущена):
        // Авто-выжим размыкает сцепление в 0.0f, чтобы машина стояла на передаче на холостых (850 RPM) и не глохла.
        if (userThrottle < 0.02f && transSyncRpm < launchStartRpm) {
            return 0.0f;
        }

        // 3. РЕЖИМ ТРОГАНИЯ С МЕСТА (Launch): водитель нажал W.
        // По мере раскрутки двигателя от launchStartRpm (1150) до launchFullLockRpm (2550)
        // корзина плавно сжимает диски по кубическому закону Smoothstep.
        float rpmProgress = clamp((currentRpm - launchStartRpm) / (launchFullLockRpm - launchStartRpm), 0.0f, 1.0f);
        float centrifugalClamp = rpmProgress * rpmProgress * (3.0f - 2.0f * rpmProgress);

        // Также учитываем меру синхронизации скоростей валов (если колеса уже догнали мотор — дожимаем до 1.0)
        float speedMatchProgress = clamp(transSyncRpm / launchStartRpm, 0.0f, 1.0f);

        return Math.max(centrifugalClamp, speedMatchProgress);
    }

    /**
     * Гладкая интерполяция внешней скоростной характеристики ДВС (кривая момента).
     * Пик 320 Н*м на 3800 RPM, спад до 265 Н*м к 6500 RPM.
     */
    private float sampleTorqueCurve(float rpm) {
        if (rpm <= targetIdleRpm) {
            return idleTorqueMaxNm;
        }
        if (rpm <= peakTorqueRpm) {
            float t = (rpm - targetIdleRpm) / (peakTorqueRpm - targetIdleRpm);
            // Гладкий подъем момента (выпуклая парабола полки момента)
            float curve = t * (2.0f - t);
            return idleTorqueMaxNm + (peakTorqueNm - idleTorqueMaxNm) * curve;
        }
        if (rpm <= redlineRpm) {
            float t = (rpm - peakTorqueRpm) / (redlineRpm - peakTorqueRpm);
            float curve = t * t;
            return peakTorqueNm - (peakTorqueNm - redlineTorqueNm) * curve;
        }
        // Выше красной зоны момент быстро падает из-за предела наполнения цилиндров (VE drop-off)
        float overRevT = clamp((rpm - redlineRpm) / 600.0f, 0.0f, 1.0f);
        return redlineTorqueNm * (1.0f - overRevT * 0.65f);
    }

    /**
     * Возвращает полное передаточное число трансмиссии (КПП * Главная пара).
     */
    public float getTotalGearRatio(int gear) {
        if (gear == 0) return 0.0f;
        if (gear == -1) return reverseGearRatio * finalDriveRatio;
        int idx = clampInt(gear - 1, 0, forwardGearRatios.length - 1);
        return forwardGearRatios[idx] * finalDriveRatio;
    }

    /**
     * Эффективная инерция ОДНОГО ведущего колеса для BrushTireSolver.
     *
     * КЛЮЧЕВОЕ ТРЕБОВАНИЕ:
     * Когда сцепление пробуксовывает в режиме SLIP (couplingFactor -> 0), колесо имеет ТОЛЬКО
     * собственную инерцию (1.4 кг*м^2) и легко раскручивается!
     * Отраженная инерция маховика подключается плавно только при полной синхронизации валов.
     *
     * @param baseWheelInertia собственная инерция колеса со ступицей (например, 1.4f) [кг*м^2]
     * @param currentGear      текущая передача
     * @param drivenWheelCount число ведущих колес (2 для RWD/FWD, 4 для AWD)
     */
    public float getEffectiveDrivenWheelInertia(float baseWheelInertia, int currentGear, int drivenWheelCount) {
        if (currentGear == 0 || couplingFactor <= 0.01f || drivenWheelCount <= 0) {
            return baseWheelInertia;
        }
        float totalRatio = getTotalGearRatio(currentGear);
        float reflectedEngineInertia = (engineInertia * totalRatio * totalRatio) / drivenWheelCount;
        return baseWheelInertia + (reflectedEngineInertia * couplingFactor);
    }

    // --- Быстрые математические утилиты без аллокаций ---

    /**
     * Быстрая и точная аппроксимация гиперболического тангенса tanh(x) Паде (0 вызовов Math.exp).
     */
    private static float fastTanh(float x) {
        if (x < -3.0f) return -1.0f;
        if (x > 3.0f) return 1.0f;
        float x2 = x * x;
        return x * (27.0f + x2) / (27.0f + 9.0f * x2);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    // --- Геттеры для телеметрии, звука и HUD ---
    public float getCurrentRpm() { return currentRpm; }
    public float getEngineOmega() { return engineOmega; }
    public float getClutchClamp() { return smoothedClutchClamp; }
    public float getLastClutchTorqueNm() { return lastClutchTorqueNm; }
    public float getLastEngineTorqueNm() { return lastEngineTorqueNm; }
    public float getCouplingFactor() { return couplingFactor; }
}
