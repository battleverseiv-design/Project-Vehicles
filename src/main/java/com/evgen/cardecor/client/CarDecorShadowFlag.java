package com.evgen.cardecor.client;

/**
 * Global flag active exclusively during the real-time shadow depth pass.
 * Used by RenderTargetShadowPassMixin and IrisChunkProgramOverridesMixin
 * to prevent Minecraft and shader mods from hijacking the framebuffer.
 */
public final class CarDecorShadowFlag {
    public static volatile boolean ACTIVE = false;

    private CarDecorShadowFlag() {}
}
