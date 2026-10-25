package com.evgen.cardecor.physics.ode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Zero-GC зонд ступенек (Virtual Ramp Step-Up Assist) для 80 Hz цикла.
 * Превращает вертикальную грань блока (до 1.05 м) в плавный наклонный пандус для луча подвески.
 */
public final class WheelStepUpProbe {
    public static final float MIN_TRIGGER_SPEED = 0.8f;   // Минимальная скорость начала наката (м/с)
    public static final float MAX_STEP_HEIGHT   = 1.05f;  // Максимальная высота преодолеваемой ступеньки (м)
    public static final float PROBE_LOOKAHEAD   = 0.75f;  // Длина виртуального пандуса перед колесом (м)
    public static final float SMOOTH_UP_RATE    = 6.5f;   // Скорость подъема пандуса (1/с)
    public static final float SMOOTH_DOWN_RATE  = 4.0f;   // Скорость спада пандуса (1/с)

    private float smoothedRampLift = 0.0f;
    private float lastTargetLift = 0.0f;
    private int subTickCounter = 0;
    private final BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

    /**
     * Обновляет высоту виртуального пандуса для одного колеса.
     *
     * @param level          мир Minecraft
     * @param wheelX         мировая X центра колеса
     * @param wheelBottomY   мировая Y нижней точки колеса (wheelY - radius)
     * @param wheelZ         мировая Z центра колеса
     * @param dirX           нормализованный вектор движения колеса по X (с учетом знака скорости вперед/назад!)
     * @param dirZ           нормализованный вектор движения колеса по Z
     * @param absSpeedMps    модуль продольной скорости машины (м/с)
     * @param hasDriveIntent нажата ли педаль газа в сторону движения (для старта в упор к блоку)
     * @param wheelRadius    радиус колеса (м)
     * @param dt             шаг физики (0.0125f)
     * @return плавное смещение точки контакта подвески вверх [0.0 .. MAX_STEP_HEIGHT]
     */
    public float updateRampLift(
            Level level,
            double wheelX,
            double wheelBottomY,
            double wheelZ,
            float dirX,
            float dirZ,
            float absSpeedMps,
            boolean hasDriveIntent,
            float wheelRadius,
            float dt
    ) {
        if (absSpeedMps < MIN_TRIGGER_SPEED && !hasDriveIntent) {
            smoothedRampLift = Math.max(0.0f, smoothedRampLift - SMOOTH_DOWN_RATE * dt);
            lastTargetLift = 0.0f;
            return smoothedRampLift;
        }

        // Опрашиваем воксельную сетку раз в 2 подшага (40 Гц) для экономии CPU
        if ((subTickCounter++ & 1) == 0) {
            lastTargetLift = sampleVoxelRampHeight(level, wheelX, wheelBottomY, wheelZ, dirX, dirZ, wheelRadius);
        }

        // Скорость подъема пандуса масштабируется от скорости машины, чтобы не было удара в подвеску
        float dynamicUpRate = SMOOTH_UP_RATE * Mth.clamp(absSpeedMps * 0.25f, 0.7f, 2.2f);
        float rate = (lastTargetLift > smoothedRampLift) ? dynamicUpRate : SMOOTH_DOWN_RATE;
        smoothedRampLift += (lastTargetLift - smoothedRampLift) * Mth.clamp(rate * dt, 0.0f, 1.0f);

        if (smoothedRampLift < 0.005f) smoothedRampLift = 0.0f;
        return smoothedRampLift;
    }

    private float sampleVoxelRampHeight(
            Level level,
            double wx,
            double bottomY,
            double wz,
            float dirX,
            float dirZ,
            float radius
    ) {
        // Проверяем 2 дистанции перед колесом (ближняя кромка и начало пандуса):
        float maxRampY = 0.0f;
        for (int step = 1; step <= 2; step++) {
            float dist = radius + (step == 1 ? 0.25f : PROBE_LOOKAHEAD);
            double checkX = wx + dirX * dist;
            double checkZ = wz + dirZ * dist;
            int bx = Mth.floor(checkX);
            int by = Mth.floor(bottomY + 0.08);
            int bz = Mth.floor(checkZ);

            mPos.set(bx, by, bz);
            BlockState state = level.getBlockState(mPos);
            if (!isClimbableSolid(level, mPos, state)) continue;

            VoxelShape shape = state.getCollisionShape(level, mPos);
            if (shape.isEmpty()) continue;

            double blockTopY = by + shape.max(Direction.Axis.Y);
            float stepDelta = (float) (blockTopY - bottomY);

            // Если ступенька в пределах [0.05 .. MAX_STEP_HEIGHT]:
            if (stepDelta > 0.05f && stepDelta <= MAX_STEP_HEIGHT) {
                // Проверяем, свободно ли пространство НАД ступенькой (чтобы не лезть в стену высотой 2+ блока!)
                mPos.set(bx, by + 1, bz);
                BlockState aboveState = level.getBlockState(mPos);
                if (!isBlockingWall(level, mPos, aboveState)) {
                    // Чем ближе колесо к грани блока, тем выше поднимается виртуальный пандус (от 55% до 100% высоты)
                    float rampFactor = (step == 1) ? 1.0f : 0.55f;
                    maxRampY = Math.max(maxRampY, stepDelta * rampFactor);
                }
            }
        }
        return maxRampY;
    }

    /**
     * Фильтр мягких блоков: игнорируем листву, траву, цветы, паутину и тонкий снег,
     * чтобы машина не застревала в текстурах растительности.
     */
    public static boolean isClimbableSolid(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) return false;
        if (state.is(Blocks.COBWEB) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) return false;
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS) && state.getValue(SnowLayerBlock.LAYERS) < 4) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isBlockingWall(Level level, BlockPos pos, BlockState state) {
        if (!isClimbableSolid(level, pos, state)) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) > 0.25;
    }

    public float getSmoothedRampLift() {
        return smoothedRampLift;
    }
}
