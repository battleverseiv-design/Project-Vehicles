package com.evgen.cardecor.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleRenderTypes extends RenderType {

    private static final Map<ResourceLocation, RenderType> CACHE = new ConcurrentHashMap<>();

    private VehicleRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType getVehicleTriangles(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false)) // Sharp crisp pixel texture (GL_NEAREST)
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL) // Double-sided faces
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);

            return create("cardecor_vehicle_sharp_" + tex.getPath().replace('/', '_'),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                262144,
                true,
                false,
                state
            );
        });
    }

    public static RenderType getHeadlightFlare(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false);

            return create("cardecor_headlight_flare_" + tex.getPath().replace('/', '_'),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                262144,
                false,
                true,
                state
            );
        });
    }

    public static RenderType getDecal(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false);

            return create("cardecor_decal_" + tex.getPath().replace('/', '_'),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                262144,
                false,
                true,
                state
            );
        });
    }
}
