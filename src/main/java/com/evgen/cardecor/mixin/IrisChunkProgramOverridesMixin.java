package com.evgen.cardecor.mixin;

import com.evgen.cardecor.client.CarDecorShadowFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from FlashlightMod: IrisChunkProgramOverridesMixin.
 * Suppresses Iris framebuffer hijacking during shadow pass if Oculus/Iris is installed.
 */
@Pseudo
@Mixin(targets = {"net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides"}, remap = false)
public class IrisChunkProgramOverridesMixin {
    @Inject(method = "bindFramebuffer", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void cardecor$suppressFboBind(CallbackInfo ci) {
        if (CarDecorShadowFlag.ACTIVE) {
            ci.cancel();
        }
    }

    @Inject(method = "unbindFramebuffer", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void cardecor$suppressFboUnbind(CallbackInfo ci) {
        if (CarDecorShadowFlag.ACTIVE) {
            ci.cancel();
        }
    }
}
