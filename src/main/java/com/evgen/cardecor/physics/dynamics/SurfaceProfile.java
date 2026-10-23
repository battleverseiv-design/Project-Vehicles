package com.evgen.cardecor.physics.dynamics;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public enum SurfaceProfile {
    //             muLat, muLong, relaxScale, rollRes, viscousDrag, roughnessAmp, roughnessFreq, rainGripFactor
    ASPHALT      ( 1.05f, 1.05f,  1.00f,      0.012f,  0.00f,       0.000f,       1.0f,          0.85f ), // Бетон, гладкий камень
    STONE_ROUGH  ( 0.98f, 1.00f,  0.95f,      0.018f,  0.00f,       0.035f,       4.5f,          0.85f ), // Булыжник, кирпич
    WOOD         ( 0.90f, 0.90f,  0.90f,      0.016f,  0.00f,       0.015f,       3.0f,          0.75f ), // Доски, мосты
    DIRT_GRASS   ( 0.80f, 0.85f,  0.80f,      0.035f,  0.02f,       0.030f,       2.8f,          0.68f ), // Трава, земля (раскисает в дождь)
    GRAVEL       ( 0.68f, 0.75f,  0.70f,      0.065f,  0.05f,       0.055f,       6.0f,          0.90f ), // Гравий (сильная тряска и увод)
    SAND         ( 0.62f, 0.68f,  0.65f,      0.090f,  0.12f,       0.025f,       1.8f,          1.05f ), // Песок
    MUD_SWAMP    ( 0.50f, 0.58f,  0.60f,      0.160f,  0.35f,       0.045f,       2.2f,          0.75f ), // Грязь, песок душ (вязнет!)
    SNOW         ( 0.42f, 0.48f,  0.55f,      0.110f,  0.18f,       0.020f,       1.5f,          0.85f ), // Снег
    ICE          ( 0.14f, 0.14f,  0.35f,      0.008f,  0.00f,       0.000f,       1.0f,          0.80f ), // Обычный и плотный лед
    BLUE_ICE     ( 0.05f, 0.05f,  0.25f,      0.004f,  0.00f,       0.000f,       1.0f,          1.00f ); // Синий лед (чистый каток)

    public final float muLatScale;
    public final float muLongScale;
    public final float relaxationScale;
    public final float rollingResistance; // Доля от нормальной нагрузки N
    public final float viscousDrag;       // Вязкое сопротивление (Н на 1 м/с скорости колеса)
    public final float roughnessAmp;      // Амплитуда микро-кочек (в метрах)
    public final float roughnessFreq;     // Частота кочек
    public final float rainGripFactor;    // Множитель зацепа под дождем

    SurfaceProfile(float muLatScale, float muLongScale, float relaxationScale,
                   float rollingResistance, float viscousDrag,
                   float roughnessAmp, float roughnessFreq, float rainGripFactor) {
        this.muLatScale = muLatScale;
        this.muLongScale = muLongScale;
        this.relaxationScale = relaxationScale;
        this.rollingResistance = rollingResistance;
        this.viscousDrag = viscousDrag;
        this.roughnessAmp = roughnessAmp;
        this.roughnessFreq = roughnessFreq;
        this.rainGripFactor = rainGripFactor;
    }

    // Zero-GC кэш по ссылке на объект Block (O(1) доступ)
    private static final Reference2ObjectOpenHashMap<Block, SurfaceProfile> CACHE = new Reference2ObjectOpenHashMap<>(512);

    public static SurfaceProfile get(BlockState state) {
        if (state == null || state.isAir()) return ASPHALT;
        Block block = state.getBlock();
        SurfaceProfile cached = CACHE.get(block);
        if (cached != null) return cached;

        // Если блок встретился впервые (включая блоки из других модов) — классифицируем и кэшируем:
        SurfaceProfile resolved = classifyBlock(state, block);
        CACHE.put(block, resolved);
        return resolved;
    }

    private static SurfaceProfile classifyBlock(BlockState state, Block block) {
        if (block == Blocks.BLUE_ICE) return BLUE_ICE;
        if (state.is(BlockTags.ICE)) return ICE;
        if (state.is(BlockTags.SNOW)) return SNOW;
        if (block == Blocks.MUD || block == Blocks.SOUL_SAND || block == Blocks.HONEY_BLOCK || block == Blocks.SLIME_BLOCK) {
            return MUD_SWAMP;
        }
        if (state.is(BlockTags.SAND)) return SAND;
        if (block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL) return GRAVEL;
        if (state.is(BlockTags.DIRT) || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT_PATH) return DIRT_GRASS;
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) return WOOD;

        // Авто-определение для модовых блоков по SoundType:
        SoundType sound = state.getSoundType();
        if (sound == SoundType.SAND) return SAND;
        if (sound == SoundType.GRAVEL) return GRAVEL;
        if (sound == SoundType.MUD || sound == SoundType.SOUL_SAND || sound == SoundType.SLIME_BLOCK) return MUD_SWAMP;
        if (sound == SoundType.SNOW || sound == SoundType.POWDER_SNOW) return SNOW;
        if (sound == SoundType.GRASS || sound == SoundType.ROOTED_DIRT) return DIRT_GRASS;
        if (sound == SoundType.WOOD || sound == SoundType.BAMBOO_WOOD) return WOOD;

        // Разделяем гладкий бетон/асфальт и тряский булыжник
        if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE || block == Blocks.COBBLED_DEEPSLATE) {
            return STONE_ROUGH;
        }
        return ASPHALT;
    }
}
