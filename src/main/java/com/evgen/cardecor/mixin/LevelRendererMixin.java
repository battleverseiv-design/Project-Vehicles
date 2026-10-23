package com.evgen.cardecor.mixin;

import com.evgen.cardecor.client.WeatherOcclusionHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1000)
public abstract class LevelRendererMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void cardecor$onRenderSnowAndRainHead(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        if (this.level != null) {
            WeatherOcclusionHelper.prepareFrame(this.level, camX, camY, camZ);
        }
    }

    @Redirect(
        method = "renderSnowAndRain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
        )
    )
    private int cardecor$redirectRainHeight(Level lvl, Heightmap.Types types, int x, int z) {
        int originalHeight = lvl.getHeight(types, x, z);
        return WeatherOcclusionHelper.getOccludedHeight(lvl, x, z, originalHeight);
    }

    @Inject(method = "tickRain", at = @At("HEAD"))
    private void cardecor$onTickRainHead(Camera camera, CallbackInfo ci) {
        if (this.level != null && camera != null) {
            WeatherOcclusionHelper.prepareFrame(this.level, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        }
    }

    @Redirect(
        method = "tickRain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelReader;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"
        )
    )
    private BlockPos cardecor$redirectTickRainHeightmapPos(LevelReader reader, Heightmap.Types types, BlockPos pos) {
        BlockPos originalPos = reader.getHeightmapPos(types, pos);
        if (this.level != null) {
            return WeatherOcclusionHelper.getOccludedHeightmapPos(this.level, pos, originalPos);
        }
        return originalPos;
    }

    @Redirect(
        method = "tickRain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        )
    )
    private void cardecor$redirectTickRainParticle(ClientLevel lvl, net.minecraft.core.particles.ParticleOptions particle,
                                                   double x, double y, double z,
                                                   double vx, double vy, double vz) {
        if (WeatherOcclusionHelper.isInsideVehicleCabin(x, y, z)) {
            return;
        }
        double adjustedY = WeatherOcclusionHelper.getVehicleSplashY(x, y, z);
        lvl.addParticle(particle, x, adjustedY, z, vx, vy, vz);
    }
}
