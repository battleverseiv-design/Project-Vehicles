package com.evgen.cardecor.physics.dynamics;

import net.minecraft.util.Mth;

/**
 * Authentic Vehicle Engine and Transmission Simulator ported strictly from Jolt Physics
 * (WheeledVehicleController.cpp and VehicleEngine.cpp).
 *
 * Implements:
 * 1. Flywheel angular inertia integration with realistic RPM spooling: I * dw/dt = T_engine - T_clutch - D * w.
 * 2. Clutch plate friction torque model (prevents engine stall on launch).
 * 3. Smart automatic transmission based purely on engine RPM (no hardcoded speed gates).
 * 4. Engine braking on throttle lift-off.
 * 5. Fuel cutoff and rev-limiter stutter at redline.
 * 6. Deterministic clutch thermodynamics and gear fatigue mechanics.
 */
public class JoltStylePowertrain {

    public static final float RPM_TO_RAD_PER_SEC = 0.104719755f;
    public static final float RAD_PER_SEC_TO_RPM = 9.54929658f;

    public final PowertrainSpec spec;

    // Dynamic runtime state
    public float currentRpm;
    public int currentGear = 0;              // -1 = Reverse, 0 = Neutral, 1..N = Forward gears
    public boolean isEngineOn = false;       // Флаг работы двигателя (зажигание)
    public float shiftCooldown = 0.0f;       // Clutch disengagement timer during shifts (seconds)
    public float clutchFriction = 0.0f;      // Telemetry: 0.0 (open) to 1.0 (fully locked)
    public float currentWheelTorqueNm = 0.0f;// Output torque delivered to driven wheels
    public float currentHorsepower = 0.0f;   // Telemetry output
    public boolean isBraking = false;        // True if reverse input is acting as footbrake
    public boolean isEngineStalled = false;  // Physical engine stall from LCP load
    public boolean clutchPedalPressed = false; // Left Alt held: выжим педали сцепления
    public float clutchPedalPosition = 0.0f;   // 0.0 = сцепление замкнуто, 1.0 = выжато в пол
    public boolean justShifted = false;
    public float transmissionCondition = 100.0f; // Состояние коробки/сцепления: 100% -> 0%
    public boolean isClutchBurning = false;       // Флаг перегрева сцепления (дым)

    // Термодинамика сцепления
    public float clutchTempCelsius = 30.0f;
    public boolean isClutchLocked = false;       // LCP состояние: сцепление жестко замкнуто
    private int telemetryLogCooldown = 0;
    public float prevDeltaOmega = 0.0f;          // Для детекции zero-crossing перехода в LOCKED
    public static final float CLUTCH_THERMAL_MASS = 2500.0f; // J/K (чугунный маховик + корзина ~5.5 кг)
    public static final float CLUTCH_COOLING_H_A = 4.5f;    // Вт/К (конвективное охлаждение)
    public static final float CLUTCH_AMBIENT = 30.0f;
    public static final float CLUTCH_GLAZE_TEMP = 480.0f;   // порог задымления фрикциона
    public static final float CLUTCH_BURN_TEMP = 620.0f;    // порог термодеструкции связующего
    public float clutchFrictionMultiplier = 1.0f;

    // Детерминированная усталостная модель выбивания передач под нагрузкой
    public float gearToothFatigue = 0.0f;

    public void selectDirectGear(int gear) {
        if (gear >= -1 && gear <= spec.gearRatios.length) {
            if (this.currentGear != gear) {
                this.shiftCooldown = (gear > this.currentGear) ? spec.shiftDurationUp : spec.shiftDurationDown;
                this.currentGear = gear;
                this.justShifted = true;
            }
        }
    }

    public void shiftUp() {
        if (currentGear == -1) {
            currentGear = 0;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear == 0) {
            currentGear = 1;
            shiftCooldown = 0.15f;
            justShifted = true;
        } else if (currentGear < spec.gearRatios.length) {
            currentGear++;
            shiftCooldown = spec.shiftDurationUp;
            justShifted = true;
        }
    }

    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            shiftCooldown = spec.shiftDurationDown;
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

    /**
     * Характеристика хода выжимного подшипника (двухступенчатая кривая с зоной схватывания Bite Point).
     */
    public static float pedalToReleaseBearing(float pedal) {
        if (pedal < 0.15f) return 0.0f;      // Свободный ход педали
        if (pedal < 0.85f) return (pedal - 0.15f) / 0.70f; // Рабочий линейный ход
        return 1.0f;                         // Зажим корзины
    }

    public void updateClutchThermal(float clutchTorqueNm, float deltaOmega, float dt) {
        float pHeat = Math.abs(clutchTorqueNm) * Math.abs(deltaOmega);
        float pCool = CLUTCH_COOLING_H_A * (clutchTempCelsius - CLUTCH_AMBIENT);
        float dT = ((pHeat - pCool) / CLUTCH_THERMAL_MASS) * dt;
        clutchTempCelsius = Math.max(CLUTCH_AMBIENT, clutchTempCelsius + dT);

        // Падение коэффициента трения при перегреве
        float tempMuFactor = 1.0f - 0.4f * Mth.clamp((clutchTempCelsius - 200.0f) / 300.0f, 0.0f, 1.0f);
        this.clutchFrictionMultiplier = tempMuFactor;

        // Деградация коробки/сцепления при критической температуре
        if (clutchTempCelsius > CLUTCH_BURN_TEMP) {
            this.transmissionCondition = Math.max(0.0f,
                this.transmissionCondition - dt * (clutchTempCelsius - CLUTCH_BURN_TEMP) * 0.015f);
        }
        this.isClutchBurning = clutchTempCelsius > CLUTCH_GLAZE_TEMP;
    }

    public JoltStylePowertrain() {
        this(PowertrainSpec.createFamilySedan());
    }

    public JoltStylePowertrain(PowertrainSpec spec) {
        this.spec = spec != null ? spec : new PowertrainSpec();
        this.currentRpm = this.spec.minRpm;
    }

    /**
     * Executes one 80 Hz sub-step of the powertrain simulation (dt = 0.0125s).
     */
    public float stepEngineAndTransmission(
            float dt,
            float throttleInput,
            float forwardSpeedMps,
            float avgWheelAngularVel,
            boolean isGrounded,
            float maxWheelSlip
    ) {
        return stepEngineAndTransmission(dt, throttleInput, forwardSpeedMps, avgWheelAngularVel, isGrounded, maxWheelSlip, 0.0f, 0.0f);
    }

    /**
     * Вычисляем коэффициент смыкания сцепления (от 0.0 до 1.0).
     * Исправляет баг, когда сцепление "висело" в полувыжатом состоянии и не давало машине ехать.
     */
    private float computeAutoClutchEngagement(float throttle, float avgWheelOmega, float totalRatio) {
        if (currentGear == 0) return 0.0f;
        if (Math.abs(avgWheelOmega) < 1.0f) {
            return Mth.clamp(throttle * 2.5f, 0.15f, 1.0f);
        }
        return 1.0f;
    }

    /**
     * Executes one 80 Hz sub-step of the powertrain simulation (dt = 0.0125s) with road load feedback.
     */
    public float stepEngineAndTransmission(
            float dt,
            float throttleInput,
            float forwardSpeedMps,
            float avgWheelAngularVel,
            boolean isGrounded,
            float maxWheelSlip,
            float roadReactionTorque,
            float totalWheelInertia
    ) {
        this.isBraking = false;
        float driveThrottle = 0.0f;

        // 1. Direction and Gear Selection (Pure Manual Transmission)
        if (spec.isAutomatic) {
            // Automatic transmission logic
            if (currentGear > 0) {
                if (throttleInput >= -0.01f) {
                    driveThrottle = Mth.clamp(throttleInput, 0.0f, 1.0f);
                } else if (forwardSpeedMps > 0.5f) {
                    this.isBraking = true;
                    driveThrottle = 0.0f;
                } else {
                    currentGear = -1;
                    shiftCooldown = spec.shiftDurationDown;
                    driveThrottle = Mth.clamp(-throttleInput, 0.0f, 1.0f);
                }
            } else if (currentGear == -1) {
                if (throttleInput <= 0.01f) {
                    driveThrottle = Mth.clamp(-throttleInput, 0.0f, 1.0f);
                } else if (forwardSpeedMps < -0.5f) {
                    this.isBraking = true;
                    driveThrottle = 0.0f;
                } else {
                    currentGear = 1;
                    shiftCooldown = spec.shiftDurationUp;
                    driveThrottle = Mth.clamp(throttleInput, 0.0f, 1.0f);
                }
            } else {
                driveThrottle = Math.abs(throttleInput);
                if (driveThrottle > 0.1f) {
                    currentGear = (throttleInput >= 0.0f) ? 1 : -1;
                }
            }
        } else {
            // Manual Transmission: Gears are strictly controlled by the driver!
            if (currentGear > 0) {
                // Forward gears (1..N): W = gas, S = footbrake
                if (throttleInput > 0.01f) {
                    driveThrottle = throttleInput;
                } else if (throttleInput < -0.01f) {
                    this.isBraking = true;
                    driveThrottle = 0.0f;
                }
            } else if (currentGear == -1) {
                // Reverse gear: W = gas in reverse, S = footbrake
                if (throttleInput > 0.01f) {
                    driveThrottle = throttleInput;
                } else if (throttleInput < -0.01f) {
                    this.isBraking = true;
                    driveThrottle = 0.0f;
                }
            } else {
                // Neutral (0): W = free revving, S = footbrake
                if (throttleInput > 0.01f) {
                    driveThrottle = throttleInput;
                } else if (throttleInput < -0.01f) {
                    this.isBraking = true;
                }
            }
        }

        // 2. Compute Total Gear Ratio (Current Gear * Differential / Final Drive)
        float gearRatio = 0.0f;
        if (currentGear == -1) {
            gearRatio = spec.reverseGearRatio * spec.differentialRatio;
        } else if (currentGear > 0 && currentGear <= spec.gearRatios.length) {
            gearRatio = spec.gearRatios[currentGear - 1] * spec.differentialRatio;
        }

        // 3. Automatic Transmission Gear Shifter with Dynamic Gear-Step Hysteresis
        if (shiftCooldown > 0.0f) {
            shiftCooldown = Math.max(0.0f, shiftCooldown - dt);
        } else if (spec.isAutomatic && currentGear > 0) {
            // Upshift calculation tied to next gear ratio:
            float upshiftRpm = spec.maxRpm * 0.85f;
            int nextGear = currentGear + 1;
            boolean hasNextGear = nextGear <= spec.gearRatios.length;

            float rpmAfterUpshift = hasNextGear
                    ? (upshiftRpm * (spec.gearRatios[nextGear - 1] / spec.gearRatios[currentGear - 1]))
                    : 0.0f;

            boolean canShiftUp = isGrounded
                    && maxWheelSlip < 0.85f
                    && hasNextGear
                    && (rpmAfterUpshift > spec.maxRpm * 0.42f);

            if (currentRpm > upshiftRpm && canShiftUp) {
                currentGear++;
                shiftCooldown = spec.shiftDurationUp; // Momentary clutch disengagement
                if (currentGear <= spec.gearRatios.length) {
                    gearRatio = spec.gearRatios[currentGear - 1] * spec.differentialRatio;
                }
            } else if (currentGear > 1) {
                // Downshift threshold dynamically tied to previous gear's upshift landing RPM minus hysteresis band:
                float prevGearUpshiftLandingRpm = upshiftRpm * (spec.gearRatios[currentGear - 1] / spec.gearRatios[currentGear - 2]);
                float downshiftRpm = Math.max(spec.minRpm * 1.3f, prevGearUpshiftLandingRpm - 250.0f);
                if (driveThrottle < 0.3f) {
                    downshiftRpm = Math.min(downshiftRpm, spec.maxRpm * 0.36f);
                }

                if (currentRpm < downshiftRpm) {
                    currentGear--;
                    shiftCooldown = spec.shiftDurationDown;
                    gearRatio = spec.gearRatios[currentGear - 1] * spec.differentialRatio;
                }
            }
        }

        // Интегрирование хода педали сцепления
        if (clutchPedalPressed) {
            clutchPedalPosition = Math.min(1.0f, clutchPedalPosition + dt * 14.0f);
        } else {
            clutchPedalPosition = Math.max(0.0f, clutchPedalPosition - dt * 6.5f);
        }

        // 4. Diaphragm Belleville Spring Clamping Capacity (Bite Point curve)
        float totalRatio = gearRatio;
        float engagement = computeAutoClutchEngagement(driveThrottle, avgWheelAngularVel, totalRatio);
        if (clutchPedalPressed || clutchPedalPosition > 0.05f) {
            engagement *= Math.max(0.0f, 1.0f - clutchPedalPosition);
        }
        if (shiftCooldown > 0.0f) {
            engagement = 0.0f;
        }

        float maxClutchTorque = spec.clutchPlatesCount * spec.clutchFrictionCoeff * spec.clutchMaxClampingForce * spec.clutchEffectiveRadius;
        float conditionFactor = Math.max(0.35f, Math.min(1.0f, this.transmissionCondition / 75.0f));
        float mCap = maxClutchTorque * engagement * conditionFactor * this.clutchFrictionMultiplier;
        if (!Float.isFinite(mCap) || mCap < 0.0f) {
            mCap = 0.0f;
        }

        // 0. Если зажигание выключено — двигатель заглушен, момент на колеса = 0
        if (!this.isEngineOn) {
            this.isClutchLocked = false;
            this.currentWheelTorqueNm = 0.0f;
            this.currentRpm = Math.max(0.0f, this.currentRpm - dt * 2500.0f);
            return 0.0f;
        }

        // 5. Engine Torque Curve
        float normalizedRpm = currentRpm / Math.max(1.0f, spec.maxRpm);
        float torqueCurveFactor;
        if (normalizedRpm < 0.60f) {
            torqueCurveFactor = 0.62f + (normalizedRpm / 0.60f) * 0.38f;
        } else {
            torqueCurveFactor = 1.0f - ((normalizedRpm - 0.60f) / 0.40f) * 0.28f;
        }

        // Base drive torque
        float grossTorque = driveThrottle * spec.maxTorqueNm * torqueCurveFactor;

        // Engine Braking on throttle lift-off
        float rpmLiftFactor = Mth.clamp((currentRpm - spec.minRpm) / Math.max(1.0f, spec.maxRpm - spec.minRpm), 0.0f, 1.0f);
        float engineBrakingTorque = -spec.engineBrakingCoeff * rpmLiftFactor;
        float engineTorque = Mth.lerp(driveThrottle, engineBrakingTorque, grossTorque);

        // Rev Limiter Cutoff
        if (currentRpm >= spec.revLimiterRpm) {
            engineTorque = -spec.engineBrakingCoeff * 1.6f;
        }

        // Если нейтраль, нулевое передаточное число или идет переключение передач — момент сцепления = 0
        if (currentGear == 0 || Math.abs(totalRatio) < 0.001f || shiftCooldown > 0.0f || engagement <= 0.001f) {
            this.isClutchLocked = false;
            float clutchTorque = 0.0f;
            this.currentWheelTorqueNm = 0.0f;
            float engineOmega = currentRpm * RPM_TO_RAD_PER_SEC;
            engineOmega += (engineTorque / spec.engineInertia) * dt;
            if (engineOmega < spec.minRpm * RPM_TO_RAD_PER_SEC && !this.isEngineStalled) {
                engineOmega = spec.minRpm * RPM_TO_RAD_PER_SEC;
            }
            this.currentRpm = engineOmega * RAD_PER_SEC_TO_RPM;
            if (!this.isClutchLocked && this.currentRpm < spec.minRpm && !this.isEngineStalled) {
                this.currentRpm = spec.minRpm;
            }
            return 0.0f;
        }

        // ==================== 6. РЕШАТЕЛЬ СЦЕПЛЕНИЯ (LCP) ====================
        float engineOmega = currentRpm * RPM_TO_RAD_PER_SEC;
        float transOmega = avgWheelAngularVel * totalRatio;
        float deltaOmega = engineOmega - transOmega;

        // Приведенная инерция колес и нагрузка дороги на первичный вал КПП
        float invRatio = 1.0f / totalRatio;
        float baseTotalWheelInertia = (totalWheelInertia > 0.05f)
                ? totalWheelInertia
                : (2.0f * 0.5f * 22.0f * (spec.wheelRadius * spec.wheelRadius) + (spec.baseMassKg * 0.45f) * (spec.wheelRadius * spec.wheelRadius));
        float reflectedWheelInertia = baseTotalWheelInertia * (invRatio * invRatio);
        float reflectedRoadLoad = roadReactionTorque * invRatio;

        // 4. LCP Условие замыкания валов (LOCKED vs SLIP)
        float invIe = 1.0f / Math.max(0.01f, spec.engineInertia);
        float invJt = 1.0f / Math.max(0.001f, reflectedWheelInertia);
        float reducedMass = 1.0f / (invIe + invJt);

        float freeRelAccel = (engineTorque * invIe) - (-reflectedRoadLoad * invJt);
        float targetLockTorque = reducedMass * (freeRelAccel + (deltaOmega / dt));

        boolean canLock = Math.abs(targetLockTorque) <= mCap;
        boolean zeroCrossing = (prevDeltaOmega != 0.0f && deltaOmega * prevDeltaOmega <= 0.0f);

        float clutchTorque;
        if (canLock || zeroCrossing || (this.isClutchLocked && Math.abs(targetLockTorque) <= mCap * 1.35f)) {
            // Сцепление жестко заблокировано: валы крутятся как единое целое
            this.isClutchLocked = true;
            clutchTorque = targetLockTorque;
            engineOmega = transOmega;
            // Холостой ход двигателя не должен толкать машину вперед на передачах выше 1-й
            if (driveThrottle < 0.05f && currentGear > 1 && clutchTorque > 0.0f) {
                clutchTorque = 0.0f;
            }
        } else {
            // Сцепление пробуксовывает (SLIP): передаем максимальный момент mCap по знаку дельты скоростей
            this.isClutchLocked = false;
            clutchTorque = Math.copySign(mCap, deltaOmega);
            // ГЛАВНОЕ УРАВНЕНИЕ УСКОРЕНИЯ КОЛЕНВАЛА: Из момента ДВС вычитается момент сцепления!
            float netEngineTorque = engineTorque - clutchTorque;
            engineOmega += (netEngineTorque * invIe) * dt;
        }

        // ==================== 7. ВЫДАЧА МОМЕНТА И ТЕРМОДИНАМИКА ====================
        // Передаем момент на колёса (для ODE4J) с учетом КПД трансмиссии
        this.currentWheelTorqueNm = clutchTorque * totalRatio * spec.transmissionEfficiency;

        // Считаем нагрев сцепления ТОЛЬКО если есть реальная разница скоростей
        float realDeltaOmega = this.isClutchLocked ? 0.0f : Math.abs(engineOmega - transOmega);
        float pHeat = Math.abs(clutchTorque) * realDeltaOmega;

        // Интегрируем температуру (теперь с нормальной теплоемкостью 2500 Дж/К)
        float pCool = spec.clutchCoolingH_A * (clutchTempCelsius - CLUTCH_AMBIENT);
        clutchTempCelsius += ((pHeat - pCool) / CLUTCH_THERMAL_MASS) * dt;
        clutchTempCelsius = Math.max(CLUTCH_AMBIENT, clutchTempCelsius);

        // Падение коэффициента трения при перегреве
        float tempMuFactor = 1.0f - 0.4f * Mth.clamp((clutchTempCelsius - 200.0f) / 300.0f, 0.0f, 1.0f);
        this.clutchFrictionMultiplier = tempMuFactor;
        this.isClutchBurning = clutchTempCelsius > CLUTCH_GLAZE_TEMP;
        if (clutchTempCelsius > CLUTCH_BURN_TEMP) {
            this.transmissionCondition = Math.max(0.0f,
                this.transmissionCondition - dt * (clutchTempCelsius - CLUTCH_BURN_TEMP) * 0.015f);
        }

        // Телеметрия и диагностическое логирование
        if (currentGear != 0 && driveThrottle > 0.05f) {
            if (--this.telemetryLogCooldown <= 0) {
                this.telemetryLogCooldown = 20; // 4 раза в секунду при 80 Гц
                System.out.printf(java.util.Locale.ROOT,
                    "[ДИАГНОСТИКА] Gear=%d | RPM=%.0f | Speed=%.1f km/h | TotalRatio=%.2f | mCap=%.1f | ClTorque=%.1f | WheelTorque=%.1f | State=%s%n",
                    currentGear, (engineOmega * RAD_PER_SEC_TO_RPM), (forwardSpeedMps * 3.6f), totalRatio, mCap,
                    clutchTorque, this.currentWheelTorqueNm, (this.isClutchLocked ? "LOCKED" : "SLIP"));
            }
        }

        // Физика старта с высокой передачи, тепловой баланс и детерминированный Engine Stall
        boolean isStopped = Math.abs(forwardSpeedMps) < 0.5f && Math.abs(avgWheelAngularVel) < 1.0f;
        float stallOmegaThreshold = spec.engineStallRpm * RPM_TO_RAD_PER_SEC;

        if (isStopped && currentGear >= 3 && clutchPedalPosition < 0.60f && driveThrottle < 0.65f) {
            // Заглохание от непосильной нагрузки на высокой передаче
            engineOmega = Math.max(0.0f, engineOmega - dt * 45.0f);
            if (engineOmega <= 5.0f) {
                engineOmega = 0.0f;
                this.isEngineStalled = true;
            }
        } else if (currentGear > 0 && clutchPedalPosition < 0.60f && engineOmega < stallOmegaThreshold && driveThrottle < 0.20f) {
            // Естественное заглохание от нагрузки через компрессию и трение
            engineOmega = Math.max(0.0f, engineOmega - dt * 30.0f);
            if (engineOmega <= 5.0f) {
                engineOmega = 0.0f;
                this.isEngineStalled = true;
            }
        } else if (!this.isClutchLocked && engineOmega < spec.minRpm * RPM_TO_RAD_PER_SEC && !this.isEngineStalled) {
            engineOmega = spec.minRpm * RPM_TO_RAD_PER_SEC;
        }

        // Обновляем RPM
        this.currentRpm = engineOmega * RAD_PER_SEC_TO_RPM;
        if (!this.isClutchLocked && this.currentRpm < spec.minRpm && !this.isEngineStalled) {
            this.currentRpm = spec.minRpm;
        }

        // Запоминаем deltaOmega для детекции zero-crossing на следующем шаге
        this.prevDeltaOmega = engineOmega - transOmega;

        // 8. Детерминированное выбивание передачи в нейтраль при механическом износе (< 40%)
        if (transmissionCondition < 40.0f && currentGear > 0 && Math.abs(clutchTorque) > 90.0f) {
            float overload = Math.abs(clutchTorque) - 90.0f;
            gearToothFatigue += dt * overload * 1.5e-3f;
        }
        gearToothFatigue *= Math.max(0.0f, 1.0f - dt * 0.04f);

        if (gearToothFatigue > 1.0f) {
            currentGear = 0; // Срыв передачи в нейтраль!
            gearToothFatigue = 0.0f;
            justShifted = true;
            shiftCooldown = 0.30f;
        }

        this.clutchFriction = (mCap > 1.0f) ? Math.min(1.0f, Math.abs(clutchTorque) / mCap) : 0.0f;
        this.currentWheelTorqueNm = clutchTorque * gearRatio * spec.transmissionEfficiency;

        if (currentRpm > spec.revLimiterRpm) {
            currentRpm = spec.revLimiterRpm - 90.0f; // Rev-limiter bounce (stutter!)
        }

        // Telemetry output
        this.currentHorsepower = Math.max(0.0f, (engineTorque * currentRpm) / 7127.0f);

        return this.currentWheelTorqueNm;
    }
}
