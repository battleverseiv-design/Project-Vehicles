package com.evgen.cardecor.mixin;

import com.evgen.cardecor.client.CarDecorShadowFlag;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from FlashlightMod: RenderTargetShadowPassMixin.
 * Suppresses Minecraft's RenderTarget.bindWrite() during shadow map rendering.
 * In vanilla, RenderType.setupRenderState() invokes Minecraft.getInstance().getMainRenderTarget().bindWrite(true),
 * which would steal the FBO and redirect entity draw calls away from our shadow map FBO back to the main window.
 */
@Mixin(RenderTarget.class)
public class RenderTargetShadowPassMixin {
    @Inject(method = "bindWrite(Z)V", at = @At("HEAD"), cancellable = true)
    private void cardecor$suppressDuringShadowPass(boolean setViewport, CallbackInfo ci) {
        if (CarDecorShadowFlag.ACTIVE && (Object) this == Minecraft.getInstance().getMainRenderTarget()) {
            ci.cancel();
        }
    }
}
