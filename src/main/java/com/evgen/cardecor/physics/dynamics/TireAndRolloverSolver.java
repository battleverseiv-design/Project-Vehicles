package com.evgen.cardecor.physics.dynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * 6-DOF Солвер сцепления шин, дифференциала и кинематографичных переворотов (Zero-GC).
 */
public class TireAndRolloverSolver {

    public final VehicleHandlingPreset preset;

    // Состояние колес (0: FL, 1: FR, 2: RL, 3: RR)
    public final float[] wheelAngularVel = new float[4];
    public final float[] wheelSpinAngle = new float[4];
    public final float[] wheelSkidIntensity = new float[4]; // 0.0..1.0 для частиц дыма и звуков
    public boolean isTwoWheelStunt = false;                  // Флаг балансирования на двух колесах

    // Выходные мировые векторы сил:
    public final Vector3f[] outSpringForces = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    public final Vector3f[] outTireForces   = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public TireAndRolloverSolver(VehicleHandlingPreset preset) {
        this.preset = preset != null ? preset : VehicleHandlingPreset.createDriftCoupeRwd();
    }

    /**
     * Расчет всех сил за один подшаг (dt = 0.0125с, 80 Гц).
     */
    public void solve(
            Level level,
            float dt,
            float throttle,
            float brake,
            boolean handBrake,
            float vehicleMass,
            float rollAngleDeg,
            boolean[] wheelGrounded,
            float[] wheelSuspLen,
            float suspensionRestLen,
            Vector3f[] wheelMountsWorld,
            Vector3f[] hubVelWorld,
            Vector3f[] wheelFwdWorld,
            Vector3f[] wheelRightWorld,
            Vector3f localUpWorld
    ) {
        float r = preset.wheelRadius;
        float invInertia = 1.0f / preset.wheelInertia;
        float totalDriveTorque = (Math.abs(throttle) > 0.01f) ? (throttle * preset.engineTorque * preset.gearRatio) : 0.0f;

        // Распределение тяги мотора по осям
        float frontShare = 0.0f, rearShare = 0.0f;
        switch (preset.driveType) {
            case FWD -> frontShare = 0.5f;
            case RWD -> rearShare = 0.5f;
            case AWD -> {
                frontShare = preset.awdFrontSplit * 0.5f;
                rearShare = preset.awdRearSplit * 0.5f;
            }
        }

        int groundedLeft = 0;
        int groundedRight = 0;

        for (int i = 0; i < 4; i++) {
            boolean isFront = (i < 2);
            boolean isLeftWheel = (i == 0 || i == 2);
            Vector3f fwd = wheelFwdWorld[i];
            Vector3f right = wheelRightWorld[i];
            Vector3f springOut = outSpringForces[i];
            Vector3f tireOut = outTireForces[i];
            springOut.zero();
            tireOut.zero();

            if (!wheelGrounded[i]) {
                // Колесо в воздухе: раскрутка мотором или затухание
                float driveT = isFront ? (totalDriveTorque * frontShare) : (totalDriveTorque * rearShare);
                wheelAngularVel[i] += ((driveT - wheelAngularVel[i] * 3.5f) * invInertia) * dt;
                wheelSpinAngle[i] += wheelAngularVel[i] * dt;
                wheelSkidIntensity[i] = 0.0f;
                continue;
            }

            if (isLeftWheel) groundedLeft++; else groundedRight++;

            // 1. ПОДВЕСКА: Раздельное демпфирование (Bump vs Rebound) + Прогрессивный отбойник
            float curSusp = wheelSuspLen[i];
            float compression = suspensionRestLen - curSusp;
            Vector3f hVel = hubVelWorld[i];
            float suspVel = hVel.dot(localUpWorld); // Скорость сжатия (>0 сжатие, <0 отбой)

            float dampingCoeff = (suspVel >= 0.0f) ? preset.bumpDamping : preset.reboundDamping;
            float fSpring = (preset.springStiffness * compression) - (dampingCoeff * suspVel);

            // Жесткий отбойник при пробитии подвески
            if (curSusp < preset.minSuspLen) {
                fSpring += (preset.minSuspLen - curSusp) * (preset.springStiffness * 4.5f);
            }
            fSpring = Math.max(0.0f, fSpring);
            springOut.set(localUpWorld).mul(fSpring);

            // 2. ДИНАМИКА ПОВЕРХНОСТИ И ВРАЩЕНИЯ КОЛЕСА
            Vector3f mount = wheelMountsWorld[i];
            float surfaceMu = detectSurfaceMu(level, mount.x, mount.y - curSusp - r, mount.z);
            float maxFriction = fSpring * surfaceMu;

            float vLong = hVel.dot(fwd);
            float vLat = hVel.dot(right);

            float driveT = isFront ? (totalDriveTorque * frontShare) : (totalDriveTorque * rearShare);

            float brakeT = 0.0f;
            if (handBrake && !isFront) {
                // Ручник мгновенно блокирует задние колеса!
                wheelAngularVel[i] = 0.0f;
            } else if (brake > 0.01f) {
                float bias = isFront ? 0.65f : 0.35f;
                brakeT = brake * 18000.0f * bias * r;
            }

            // 3. ПРОДОЛЬНАЯ СИЛА (Тяга / Торможение / Блокировка)
            float desiredLong;
            if (handBrake && !isFront) {
                desiredLong = -Math.signum(vLong) * Math.min(Math.abs(vLong) * 4500.0f, maxFriction);
            } else {
                float driveForce = driveT / r;
                float brakeForce = -Math.signum(vLong) * Math.min(Math.abs(vLong) * 5000.0f, brakeT / r);
                desiredLong = driveForce + brakeForce;

                // Интеграция угловой скорости колеса
                float netTorque = driveT - (Math.signum(wheelAngularVel[i]) * brakeT) - (desiredLong * r);
                wheelAngularVel[i] += (netTorque * invInertia) * dt;
            }
            wheelSpinAngle[i] += wheelAngularVel[i] * dt;

            // 4. БОКОВОЕ СЦЕПЛЕНИЕ ПО УГЛУ УВОДА (Pacejka-Lite)
            float slipAngle = (float) Math.atan2(Math.abs(vLat), Math.max(Math.abs(vLong), 1.3f));
            float cAlpha = isFront ? preset.frontCorneringStiffness : preset.rearCorneringStiffness;
            float latGrip = 1.0f;
            if (slipAngle > preset.peakSlipAngleRad) {
                float over = (slipAngle - preset.peakSlipAngleRad) / (float) Math.toRadians(18.0);
                latGrip = Mth.lerp(Mth.clamp(over, 0.0f, 1.0f), 1.0f, preset.slideFrictionRatio);
            }
            float desiredLat = -Math.signum(vLat) * Math.min(Math.abs(vLat) * cAlpha, maxFriction * latGrip);

            // 5. КРУГ ТРЕНИЯ КАММА С ПРИОРИТЕТОМ БУКСА (RWD / FWD / AWD)
            float totalFsq = desiredLong * desiredLong + desiredLat * desiredLat;
            float maxFsq = maxFriction * maxFriction;
            if (totalFsq > maxFsq && totalFsq > 0.001f) {
                float tot = (float) Math.sqrt(totalFsq);
                float scale = (maxFriction * preset.slideFrictionRatio) / tot;

                // При буксе продольная тяга забирает приоритет, а боковой зацеп резко падает
                desiredLong *= Math.min(1.0f, scale * 1.25f);
                desiredLat  *= scale * 0.70f;

                wheelSkidIntensity[i] = Mth.clamp((tot - maxFriction) / (maxFriction + 100.0f), 0.0f, 1.0f);
            } else {
                wheelSkidIntensity[i] = 0.0f;
            }

            // 6. МЕХАНИКА КИНЕМАТОГРАФИЧНЫХ ПЕРЕВОРОТОВ (Rim Bite & Tripping Rollover)
            boolean isOutsideWheel = (isLeftWheel && vLat > 0.0f) || (!isLeftWheel && vLat < 0.0f);
            float compressionRatio = compression / suspensionRestLen;

            float rolloverBiteMult = 1.0f;
            if (isOutsideWheel && Math.abs(vLat) > preset.rimBiteSpeedThreshold && compressionRatio > 0.75f) {
                // А. Подлом шины и врезание обода в грунт (Rim Dig-In):
                float overload = Mth.clamp((compressionRatio - 0.75f) / 0.25f, 0.0f, 1.0f);
                rolloverBiteMult = 1.0f + overload * (preset.rimBitePower - 1.0f);
            }

            // Б. Боковой удар о ступень/бордюр (Curb Trip):
            if (isOutsideWheel && Math.abs(vLat) > 4.5f && checkCurbCollision(level, mount.x, mount.y - curSusp, mount.z, right, vLat)) {
                rolloverBiteMult = Math.max(rolloverBiteMult, 2.4f * preset.curbTripSensitivity);
            }

            // Применяем подбрасывающий боковой множитель
            desiredLat *= rolloverBiteMult;

            // 7. Сопротивление волочения шин (Tire Scrub Drag)
            float vPlanarSq = vLong * vLong + vLat * vLat;
            if (vPlanarSq > 0.04f) {
                float sin2A = (vLat * vLat) / (vPlanarSq + 0.001f);
                float scrub = preset.scrubDragCoeff * sin2A * fSpring;
                float vPl = (float) Math.sqrt(vPlanarSq);
                desiredLong -= scrub * (vLong / vPl);
                desiredLat  -= scrub * (vLat / vPl);
            }

            // Итоговая сила трения в пятно контакта шины
            tireOut.set(
                    fwd.x * desiredLong + right.x * desiredLat,
                    fwd.y * desiredLong + right.y * desiredLat,
                    fwd.z * desiredLong + right.z * desiredLat
            );
        }

        // Фиксация трюка "езда на двух боковых колесах"
        isTwoWheelStunt = (groundedLeft == 0 && groundedRight >= 2) || (groundedRight == 0 && groundedLeft >= 2);
    }

    /**
     * Проверка бокового удара колеса о край блока/бордюр (Curb Trip Check).
     */
    private boolean checkCurbCollision(Level level, float x, float y, float z, Vector3f right, float vLat) {
        float checkDir = Math.signum(vLat);
        float sideX = x + right.x * checkDir * 0.45f;
        float sideY = y + 0.25f; // Высота бордюра / ступени
        float sideZ = z + right.z * checkDir * 0.45f;

        mutablePos.set(Mth.floor(sideX), Mth.floor(sideY), Mth.floor(sideZ));
        BlockState state = level.getBlockState(mutablePos);
        if (!state.isAir()) {
            VoxelShape shape = state.getCollisionShape(level, mutablePos);
            return !shape.isEmpty();
        }
        return false;
    }

    /**
     * Коэффициент сцепления покрытия.
     */
    public static float detectSurfaceMu(Level level, float x, float y, float z) {
        if (level == null) return 1.15f;
        BlockPos pos = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) s = level.getBlockState(pos.below());

        if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE)) return 0.18f;
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.MUD)) return 0.65f;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.DIRT)) return 0.85f;
        return 1.15f; // Асфальт/камень
    }
}
