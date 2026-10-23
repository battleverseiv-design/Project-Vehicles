package com.evgen.cardecor.client.render;

import com.evgen.cardecor.client.IrisCompat;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DeferredHeadlightRenderer {
    private static final Logger LOGGER = LogManager.getLogger(DeferredHeadlightRenderer.class);

    private static final int MAX_LIGHTS = 16;
    private static boolean initialized = false;
    private static boolean initFailed = false;

    private static int shaderProgram = 0;
    private static int vao = 0;
    private static int vbo = 0;

    private static int locDepthSampler = -1;
    private static int locSceneSampler = -1;
    private static int locMVP = -1;
    private static int locInvMVP = -1;
    private static int locInvProj = -1;
    private static int locInvView = -1;
    private static int locInvScreenSize = -1;
    private static int locLightCount = -1;
    private static int locIsHandPass = -1;
    private static int locHandLight = -1;

    private static final int[] locLightOrigin = new int[MAX_LIGHTS];
    private static final int[] locLightDir = new int[MAX_LIGHTS];
    private static final int[] locLightColor = new int[MAX_LIGHTS];
    private static final int[] locConeAngleCos = new int[MAX_LIGHTS];
    private static final int[] locRange = new int[MAX_LIGHTS];
    private static final int[] locIntensity = new int[MAX_LIGHTS];

    private static int sceneCopyFbo = -1;
    private static int sceneCopyTex = -1;
    private static int sceneCopyWidth = -1;
    private static int sceneCopyHeight = -1;

    private static final FloatBuffer MATRIX_BUF = BufferUtils.createFloatBuffer(16);
    private static long lastDiagnosticTime = 0L;

    private static class SpotLight {
        Vector3f relOrigin;
        Vector3f dir;
        Vector3f color;
        float coneAngleCos;
        float range;
        float intensity;
    }

    private static final List<SpotLight> activeSpotlights = new ArrayList<>();

    public static synchronized void init() {
        if (initialized || initFailed) return;

        try {
            String vshSource = loadShaderSource("/assets/cardecor/shaders/deferred_headlight.vsh");
            String fshSource = loadShaderSource("/assets/cardecor/shaders/deferred_headlight.fsh");

            if (vshSource == null || fshSource == null) {
                initFailed = true;
                LOGGER.error("[DeferredHeadlight] Failed to read shader files from assets!");
                return;
            }

            int vShader = compileShader(GL20.GL_VERTEX_SHADER, vshSource, "deferred_headlight.vsh");
            int fShader = compileShader(GL20.GL_FRAGMENT_SHADER, fshSource, "deferred_headlight.fsh");

            if (vShader == 0 || fShader == 0) {
                initFailed = true;
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vShader);
            GL20.glAttachShader(shaderProgram, fShader);
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(shaderProgram);
                LOGGER.error("[DeferredHeadlight] Program link failed: {}", log);
                GL20.glDeleteProgram(shaderProgram);
                shaderProgram = 0;
                initFailed = true;
                return;
            }

            GL20.glDeleteShader(vShader);
            GL20.glDeleteShader(fShader);

            // Resolve uniforms
            locDepthSampler = GL20.glGetUniformLocation(shaderProgram, "uDepthSampler");
            locSceneSampler = GL20.glGetUniformLocation(shaderProgram, "uSceneSampler");
            locMVP = GL20.glGetUniformLocation(shaderProgram, "uMVP");
            locInvMVP = GL20.glGetUniformLocation(shaderProgram, "uInvMVP");
            locInvProj = GL20.glGetUniformLocation(shaderProgram, "uInvProj");
            locInvView = GL20.glGetUniformLocation(shaderProgram, "uInvView");
            locInvScreenSize = GL20.glGetUniformLocation(shaderProgram, "uInvScreenSize");
            locLightCount = GL20.glGetUniformLocation(shaderProgram, "uLightCount");
            locIsHandPass = GL20.glGetUniformLocation(shaderProgram, "uIsHandPass");
            locHandLight = GL20.glGetUniformLocation(shaderProgram, "uHandLight");

            for (int i = 0; i < MAX_LIGHTS; i++) {
                locLightOrigin[i] = GL20.glGetUniformLocation(shaderProgram, "uLightOrigin[" + i + "]");
                locLightDir[i] = GL20.glGetUniformLocation(shaderProgram, "uLightDir[" + i + "]");
                locLightColor[i] = GL20.glGetUniformLocation(shaderProgram, "uLightColor[" + i + "]");
                locConeAngleCos[i] = GL20.glGetUniformLocation(shaderProgram, "uConeAngleCos[" + i + "]");
                locRange[i] = GL20.glGetUniformLocation(shaderProgram, "uRange[" + i + "]");
                locIntensity[i] = GL20.glGetUniformLocation(shaderProgram, "uIntensity[" + i + "]");
            }

            // Create Fullscreen Quad VAO & VBO
            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            float[] quadVertices = new float[] {
                -1.0f, -1.0f, 0.0f,  0.0f, 0.0f,
                 1.0f, -1.0f, 0.0f,  1.0f, 0.0f,
                 1.0f,  1.0f, 0.0f,  1.0f, 1.0f,
                -1.0f,  1.0f, 0.0f,  0.0f, 1.0f
            };

            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadVertices, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0); // aPos
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 20, 0L);

            GL20.glEnableVertexAttribArray(1); // aTexCoord
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 20, 12L);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            initialized = true;
            LOGGER.info("[DeferredHeadlight] Initialized screen-space headlight lighting pass successfully!");
        } catch (Throwable t) {
            LOGGER.error("[DeferredHeadlight] Initialization exception: ", t);
            initFailed = true;
        }
    }

    public static void render(Camera camera, Matrix4f projectionMatrix, float partialTick) {
        render(camera, projectionMatrix, null, partialTick);
    }

    public static void render(Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTick) {
        if (initFailed) return;
        if (!initialized) {
            init();
            if (!initialized) return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        Vec3 camPos = camera.getPosition();

        // 1. Collect all nearby vehicles with headlights ON within 75m
        AABB searchBox = new AABB(
            camPos.x - 75.0, camPos.y - 75.0, camPos.z - 75.0,
            camPos.x + 75.0, camPos.y + 75.0, camPos.z + 75.0
        );
        List<DrivableVehicleEntity> vehicles = level.getEntitiesOfClass(
            DrivableVehicleEntity.class, searchBox, v -> (v.isHeadlightsOn() && v.getHeadlightMode() > 0) || v.isBraking()
        );

        if (vehicles.isEmpty()) return;

        // Sort closest first
        vehicles.sort(Comparator.comparingDouble(v -> v.distanceToSqr(camPos)));

        activeSpotlights.clear();

        for (DrivableVehicleEntity vehicle : vehicles) {
            if (activeSpotlights.size() + 2 > MAX_LIGHTS) break;

            int mode = vehicle.getHeadlightMode();

            double vX = Mth.lerp(partialTick, vehicle.xOld, vehicle.getX());
            double vY = Mth.lerp(partialTick, vehicle.yOld, vehicle.getY());
            double vZ = Mth.lerp(partialTick, vehicle.zOld, vehicle.getZ());
            float yaw = Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot());
            float pitch = Mth.rotLerp(partialTick, vehicle.xRotO, vehicle.getXRot());

            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);
            double cosP = Math.cos(pitchRad);
            double sinP = Math.sin(pitchRad);
            double cosY = Math.cos(yawRad);
            double sinY = Math.sin(yawRad);

            // Forward, Right, Up vectors in Minecraft world space
            Vec3 fwd = new Vec3(-sinY * cosP, -sinP, cosY * cosP).normalize();
            Vec3 right = new Vec3(cosY, 0.0, sinY).normalize();
            Vec3 up = right.cross(fwd).normalize();

            VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
            if (def == null) continue;

            boolean frontLightsOn = vehicle.isHeadlightsOn() && mode > 0 && def.headlights != null;
            boolean brakeLightsOn = vehicle.isBraking();

            if (frontLightsOn) {
                List<VehicleDefinition.LightSourceDef> resolvedLights = def.headlights.getResolvedLights(def);
                if (resolvedLights != null && !resolvedLights.isEmpty()) {
                    // Ambient daylight factor:
                    float timeOfDay = level.getTimeOfDay(partialTick);
                    float sunCosine = (float) Math.cos(timeOfDay * Math.PI * 2.0);
                    float dayFactor = Mth.clamp(sunCosine * 2.0f + 0.5f, 0.0f, 1.0f);

                    net.minecraft.core.BlockPos vPos = net.minecraft.core.BlockPos.containing(vehicle.getX(), vehicle.getY() + 1.2, vehicle.getZ());
                    int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, vPos);
                    int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, vPos);
                    float sunExposure = dayFactor * (skyLight / 15.0f);
                    float blockExposure = blockLight / 15.0f;
                    float totalAmbientExposure = Math.min(1.0f, sunExposure + blockExposure * 0.70f);

                    float ambientIntensityMult = Mth.lerp(totalAmbientExposure, 1.0f, 0.10f);
                    float baseIntensity = IrisCompat.isShaderPackInUse() ? 0.45f : 0.64f;
                    float vehicleIntensity = (float) (def.headlights.intensity > 0.05 ? def.headlights.intensity : 1.0);
                    float finalIntensity = baseIntensity * ambientIntensityMult * vehicleIntensity;

                    float[] defaultCol = VehicleDefinition.HeadlightConfig.parseColor(def.headlights.color, new float[]{1.0f, 0.96f, 0.82f});

                    for (VehicleDefinition.LightSourceDef lDef : resolvedLights) {
                        if (activeSpotlights.size() >= MAX_LIGHTS) break;

                        float scale = (def.scale > 0.05f) ? def.scale : 1.0f;
                        // Model coordinates: +X is right, +Y is up, -Z is forward
                        double lx = ((lDef.pos != null && lDef.pos.length > 0) ? lDef.pos[0] : 0.0) * scale;
                        double ly = ((lDef.pos != null && lDef.pos.length > 1) ? lDef.pos[1] : 0.75) * scale + def.groundOffsetY + vehicle.physics.rideHeight;
                        double lz = ((lDef.pos != null && lDef.pos.length > 2) ? lDef.pos[2] : -2.2) * scale;

                        Vec3 lampWorld = new Vec3(vX, vY, vZ)
                            .add(right.scale(lx))
                            .add(up.scale(ly))
                            .add(fwd.scale(-lz));

                        Vec3 lampDir = fwd;
                        if (Math.abs(lDef.downwardAngle) > 0.0001) {
                            lampDir = lampDir.add(up.scale(-lDef.downwardAngle));
                        }
                        if (Math.abs(lDef.yawAngle) > 0.0001) {
                            lampDir = lampDir.add(right.scale(lDef.yawAngle));
                        }
                        lampDir = lampDir.normalize();

                        float[] col = (lDef.color != null && !lDef.color.isEmpty())
                            ? VehicleDefinition.HeadlightConfig.parseColor(lDef.color, defaultCol)
                            : defaultCol;

                        float lightRange = (float) (lDef.range > 5.0 ? lDef.range : 42.0);
                        float lightConeCos = (float) Math.cos(Math.toRadians(Mth.clamp(lDef.coneAngle, 10.0, 75.0)));
                        float lightIndivIntensity = (float) (finalIntensity * (lDef.intensity > 0.01 ? lDef.intensity : 1.0));

                        SpotLight sl = new SpotLight();
                        sl.relOrigin = new Vector3f((float)(lampWorld.x - camPos.x), (float)(lampWorld.y - camPos.y), (float)(lampWorld.z - camPos.z));
                        sl.dir = new Vector3f((float)lampDir.x, (float)lampDir.y, (float)lampDir.z);
                        sl.color = new Vector3f(col[0], col[1], col[2]);
                        sl.coneAngleCos = lightConeCos;
                        sl.range = lightRange;
                        sl.intensity = lightIndivIntensity;
                        activeSpotlights.add(sl);
                    }
                }
            }

            // Rear taillights & brake lights deferred spotlight (casts red illumination behind)
            boolean hasRearLight = (brakeLightsOn || frontLightsOn) && activeSpotlights.size() < MAX_LIGHTS;
            if (hasRearLight) {
                float scale = (def.scale > 0.05f) ? def.scale : 1.0f;
                double[] leftRear = def.getTaillightLeftPos();
                double rY = (leftRear[1] * scale) + def.groundOffsetY + vehicle.physics.rideHeight;
                double rZ = leftRear[2] * scale; // positive Z in model space is rear

                Vec3 rearOriginWorld = new Vec3(vX, vY, vZ)
                    .add(up.scale(rY))
                    .add(fwd.scale(-rZ)); // -fwd is backwards

                Vec3 rearDir = fwd.scale(-1.0).add(up.scale(-0.08)).normalize(); // pointing backwards and slightly down

                double rawIdle = (def.taillights != null) ? def.taillights.idleBrightness : 0.25;
                float idleRatio = (float) (rawIdle > 1.0 ? rawIdle / 100.0 : rawIdle);

                // Daylight factor: in daytime / sunlight, red illumination is exactly 2x weaker (0.50x), at night full 1.0x
                float timeOfDay = level.getTimeOfDay(partialTick);
                float sunCosine = (float) Math.cos(timeOfDay * Math.PI * 2.0);
                float dayFactor = Mth.clamp(sunCosine * 2.0f + 0.5f, 0.0f, 1.0f);
                net.minecraft.core.BlockPos vPos = vehicle.blockPosition();
                int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, vPos);
                float sunExposure = dayFactor * (skyLight / 15.0f);
                float dayNightRedFactor = Mth.lerp(sunExposure, 1.0f, 0.50f);

                float baseBrakeIntensity = IrisCompat.isShaderPackInUse() ? 0.65f : 0.88f;
                float brakeIntensity = (brakeLightsOn ? baseBrakeIntensity : (baseBrakeIntensity * Math.max(0.72f, idleRatio))) * dayNightRedFactor;
                float maxRange = (float) (def.taillights != null && def.taillights.range > 3.0 ? def.taillights.range : 18.0);
                float brakeRange = brakeLightsOn ? maxRange : Math.max(14.0f, maxRange * 0.85f);

                if (brakeIntensity > 0.01f) {
                    SpotLight bsl = new SpotLight();
                    bsl.relOrigin = new Vector3f((float)(rearOriginWorld.x - camPos.x), (float)(rearOriginWorld.y - camPos.y), (float)(rearOriginWorld.z - camPos.z));
                    bsl.dir = new Vector3f((float)rearDir.x, (float)rearDir.y, (float)rearDir.z);
                    bsl.color = new Vector3f(1.0f, 0.05f, 0.05f); // pure automotive brake red
                    bsl.coneAngleCos = (float) Math.cos(Math.toRadians(68.0)); // wide diffuse 136-degree spread
                    bsl.range = brakeRange;
                    bsl.intensity = brakeIntensity;
                    activeSpotlights.add(bsl);
                }
            }
        }

        if (activeSpotlights.isEmpty()) return;

        RenderTarget mainTarget = mc.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) return;

        int boundFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int currentFbo = (boundFbo > 0) ? boundFbo : mainTarget.frameBufferId;

        // Dynamic depth texture discovery
        int depthTex = -1;
        if (IrisCompat.isShaderPackInUse()) {
            depthTex = IrisCompat.getIrisDepthTexture();
        }
        if (depthTex <= 0 && currentFbo > 0) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);
            int objType = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
            );
            if (objType == GL11.GL_TEXTURE) {
                depthTex = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
                );
            }
            if (depthTex <= 0) {
                int objTypeDs = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
                );
                if (objTypeDs == GL11.GL_TEXTURE) {
                    depthTex = GL30.glGetFramebufferAttachmentParameteri(
                        GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
                    );
                }
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        }
        if (depthTex <= 0) {
            depthTex = mainTarget.getDepthTextureId();
        }

        long now = System.currentTimeMillis();
        if (now - lastDiagnosticTime > 3000L) {
            lastDiagnosticTime = now;
            LOGGER.info("[DeferredHeadlight] lights={}, shaders={}, isRenderingWorld={}, boundFbo={}, currentFbo={}, depthTex={}, w={}, h={}",
                activeSpotlights.size(), IrisCompat.isShaderPackInUse(), IrisCompat.isRenderingWorld(), boundFbo, currentFbo, depthTex, width, height);
        }

        ensureSceneCopy(width, height);

        // Copy current scene color into scratch texture via glBlitFramebuffer
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopyFbo);
        GL11.glReadBuffer(currentFbo == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);
        GL11.glDrawBuffer(currentFbo == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
        GL11.glViewport(0, 0, width, height);

        // Calculate inverse View-Projection matrix for camera-relative depth reconstruction.
        // Minecraft's exact camera-relative view matrix:
        // LevelRenderer uses Axis.XP.rotationDegrees(camera.getXRot()) * Axis.YP.rotationDegrees(camera.getYRot() + 180.0f)
        Matrix4f view;
        if (modelViewMatrix != null && !isIdentity(modelViewMatrix)
                && Math.abs(modelViewMatrix.m30()) < 1e-3f
                && Math.abs(modelViewMatrix.m31()) < 1e-3f
                && Math.abs(modelViewMatrix.m32()) < 1e-3f) {
            view = new Matrix4f(modelViewMatrix);
        } else {
            view = new Matrix4f()
                .rotationX((float) Math.toRadians(camera.getXRot()))
                .rotateY((float) Math.toRadians(camera.getYRot() + 180.0f));
        }

        Matrix4f mvp = new Matrix4f(projectionMatrix).mul(view);
        Matrix4f invMVP = new Matrix4f(mvp).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f invView = new Matrix4f(view).invert();

        // Save active shader program to restore cleanly after pass
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        // Bind program & upload uniforms
        GL20.glUseProgram(shaderProgram);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(depthTex);
        GL20.glUniform1i(locDepthSampler, 0);

        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(sceneCopyTex);
        GL20.glUniform1i(locSceneSampler, 1);

        uploadMatrix(locMVP, mvp);
        uploadMatrix(locInvMVP, invMVP);
        uploadMatrix(locInvProj, invProj);
        uploadMatrix(locInvView, invView);
        GL20.glUniform2f(locInvScreenSize, 1.0f / (float) width, 1.0f / (float) height);
        GL20.glUniform1i(locIsHandPass, 0);

        int count = activeSpotlights.size();
        GL20.glUniform1i(locLightCount, count);

        for (int i = 0; i < count; i++) {
            SpotLight sl = activeSpotlights.get(i);
            GL20.glUniform3f(locLightOrigin[i], sl.relOrigin.x, sl.relOrigin.y, sl.relOrigin.z);
            GL20.glUniform3f(locLightDir[i], sl.dir.x, sl.dir.y, sl.dir.z);
            GL20.glUniform3f(locLightColor[i], sl.color.x, sl.color.y, sl.color.z);
            GL20.glUniform1f(locConeAngleCos[i], sl.coneAngleCos);
            GL20.glUniform1f(locRange[i], sl.range);
            GL20.glUniform1f(locIntensity[i], sl.intensity);
        }

        // Render full-screen quad directly to active color buffer
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        // Restore active shader program
        GL20.glUseProgram(prevProgram);

        // Safely unbind scratch textures via GlStateManager so Blaze3D cache remains perfectly in sync
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(0);

        // Restore FBO and viewport
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        if (boundFbo == mainTarget.frameBufferId) {
            mainTarget.bindWrite(false);
        }

        // Restore RenderSystem states cleanly
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void ensureSceneCopy(int width, int height) {
        if (sceneCopyTex != -1 && sceneCopyWidth == width && sceneCopyHeight == height) {
            return;
        }

        if (sceneCopyTex != -1) {
            GL11.glDeleteTextures(sceneCopyTex);
            GL30.glDeleteFramebuffers(sceneCopyFbo);
            sceneCopyTex = -1;
            sceneCopyFbo = -1;
        }

        sceneCopyWidth = width;
        sceneCopyHeight = height;

        sceneCopyTex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneCopyTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        sceneCopyFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneCopyFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sceneCopyTex, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void uploadMatrix(int location, Matrix4f mat) {
        if (location < 0) return;
        MATRIX_BUF.clear();
        mat.get(MATRIX_BUF);
        GL20.glUniformMatrix4fv(location, false, MATRIX_BUF);
    }

    private static boolean isIdentity(Matrix4f m) {
        return Math.abs(m.m00() - 1.0f) < 1e-4f &&
               Math.abs(m.m11() - 1.0f) < 1e-4f &&
               Math.abs(m.m22() - 1.0f) < 1e-4f &&
               Math.abs(m.m33() - 1.0f) < 1e-4f &&
               Math.abs(m.m01()) < 1e-4f &&
               Math.abs(m.m02()) < 1e-4f &&
               Math.abs(m.m10()) < 1e-4f &&
               Math.abs(m.m12()) < 1e-4f &&
               Math.abs(m.m20()) < 1e-4f &&
               Math.abs(m.m21()) < 1e-4f;
    }

    private static int compileShader(int type, String source, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            LOGGER.error("[DeferredHeadlight] Error compiling shader {}: {}", name, log);
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static String loadShaderSource(String resourcePath) {
        try (InputStream is = DeferredHeadlightRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.error("[DeferredHeadlight] Could not find shader resource at: {}", resourcePath);
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("[DeferredHeadlight] Failed reading shader: {}", resourcePath, e);
            return null;
        }
    }

    public static void renderHandLighting(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        Vector3f handLight = computePlayerHandLight(mc.player, mc.level, partialTick);
        if (handLight == null || handLight.lengthSquared() < 0.0001f) return;

        init();
        if (shaderProgram == 0 || vao == 0) return;

        RenderTarget mainTarget = mc.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) return;

        int boundFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int currentFbo = (boundFbo > 0) ? boundFbo : mainTarget.frameBufferId;

        int depthTex = mainTarget.getDepthTextureId();
        if (depthTex <= 0) return;

        ensureSceneCopy(width, height);

        // Copy current color buffer to scratch texture
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopyFbo);
        GL11.glReadBuffer(currentFbo == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);
        GL11.glDrawBuffer(currentFbo == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
        GL11.glViewport(0, 0, width, height);

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(shaderProgram);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(depthTex);
        GL20.glUniform1i(locDepthSampler, 0);

        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(sceneCopyTex);
        GL20.glUniform1i(locSceneSampler, 1);

        GL20.glUniform1i(locIsHandPass, 1);
        GL20.glUniform3f(locHandLight, handLight.x, handLight.y, handLight.z);
        GL20.glUniform2f(locInvScreenSize, 1.0f / (float) width, 1.0f / (float) height);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(prevProgram);

        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        if (boundFbo == mainTarget.frameBufferId) {
            mainTarget.bindWrite(false);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static Vector3f computePlayerHandLight(Player player, ClientLevel level, float partialTick) {
        if (player == null || level == null) return null;
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vector3f accum = new Vector3f(0.0f, 0.0f, 0.0f);

        Entity vehicleEnt = player.getVehicle();
        if (vehicleEnt instanceof DrivableVehicleEntity vehicle) {
            VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
            if (def != null && vehicle.isHeadlightsOn()) {
                float[] col = (def.headlights != null && def.headlights.color != null)
                    ? VehicleDefinition.HeadlightConfig.parseColor(def.headlights.color, new float[]{1.0f, 0.95f, 0.85f})
                    : new float[]{1.0f, 0.95f, 0.85f};
                float inten = (float) (def.headlights != null ? def.headlights.intensity : 1.0f);
                // In cabin: smooth diffuse bounce from road and dashboard cluster
                accum.add(col[0] * inten * 0.55f, col[1] * inten * 0.55f, col[2] * inten * 0.55f);
            }
            if (vehicle.isBraking()) {
                accum.add(0.08f, 0.01f, 0.01f);
            }
        } else {
            AABB searchBox = player.getBoundingBox().inflate(45.0);
            List<DrivableVehicleEntity> nearby = level.getEntitiesOfClass(DrivableVehicleEntity.class, searchBox);
            for (DrivableVehicleEntity v : nearby) {
                VehicleDefinition def = VehiclePackManager.get(v.getVehicleId());
                if (def == null) continue;

                float smoothYaw = Mth.rotLerp(partialTick, v.yRotO, v.getYRot());
                float smoothPitch = Mth.lerp(partialTick, v.prevPitch, v.currentPitch);

                double vX = Mth.lerp(partialTick, v.xo, v.getX());
                double vY = Mth.lerp(partialTick, v.yo, v.getY());
                double vZ = Mth.lerp(partialTick, v.zo, v.getZ());

                Vec3 fwd = Vec3.directionFromRotation(smoothPitch, smoothYaw).normalize();
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 right = fwd.cross(up).normalize();
                up = right.cross(fwd).normalize();

                if (v.isHeadlightsOn() && def.headlights != null) {
                    List<VehicleDefinition.LightSourceDef> lights = def.headlights.getResolvedLights(def);
                    float[] defaultCol = new float[]{1.0f, 0.96f, 0.88f};
                    for (VehicleDefinition.LightSourceDef lDef : lights) {
                        float scale = (def.scale > 0.05f) ? def.scale : 1.0f;
                        double lx = ((lDef.pos != null && lDef.pos.length > 0) ? lDef.pos[0] : 0.0) * scale;
                        double ly = ((lDef.pos != null && lDef.pos.length > 1) ? lDef.pos[1] : 0.75) * scale + def.groundOffsetY + v.physics.rideHeight;
                        double lz = ((lDef.pos != null && lDef.pos.length > 2) ? lDef.pos[2] : -2.2) * scale;

                        Vec3 lampWorld = new Vec3(vX, vY, vZ)
                            .add(right.scale(lx))
                            .add(up.scale(ly))
                            .add(fwd.scale(-lz));

                        Vec3 lampDir = fwd;
                        if (Math.abs(lDef.downwardAngle) > 0.0001) lampDir = lampDir.add(up.scale(-lDef.downwardAngle));
                        if (Math.abs(lDef.yawAngle) > 0.0001) lampDir = lampDir.add(right.scale(lDef.yawAngle));
                        lampDir = lampDir.normalize();

                        Vec3 toPlayer = eyePos.subtract(lampWorld);
                        double dist = toPlayer.length();
                        float range = (float) (lDef.range > 5.0 ? lDef.range : 42.0);
                        if (dist > 0.2 && dist < range) {
                            double cosAngle = lampDir.dot(toPlayer.scale(1.0 / dist));
                            double coneCos = Math.cos(Math.toRadians(Mth.clamp(lDef.coneAngle, 10.0, 75.0)));
                            if (cosAngle > coneCos) {
                                float spot = (float) Mth.clamp((cosAngle - coneCos) / (1.0 - coneCos), 0.0, 1.0);
                                float normDist = (float) (dist / range);
                                float falloff = (1.0f - normDist * normDist);
                                falloff *= falloff;

                                float[] col = (lDef.color != null && !lDef.color.isEmpty())
                                    ? VehicleDefinition.HeadlightConfig.parseColor(lDef.color, defaultCol)
                                    : defaultCol;
                                float inten = (float) (lDef.intensity > 0.01 ? lDef.intensity : 1.0f);

                                float factor = inten * spot * falloff * 1.4f;
                                accum.add(col[0] * factor, col[1] * factor, col[2] * factor);
                            }
                        }
                    }
                }

                if ((v.isBraking() || v.isHeadlightsOn()) && def.taillights != null) {
                    double[] leftRear = def.getTaillightLeftPos();
                    float scale = (def.scale > 0.05f) ? def.scale : 1.0f;
                    double rY = (leftRear[1] * scale) + def.groundOffsetY + v.physics.rideHeight;
                    double rZ = leftRear[2] * scale;
                    Vec3 rearWorld = new Vec3(vX, vY, vZ).add(up.scale(rY)).add(fwd.scale(-rZ));
                    Vec3 toPlayer = eyePos.subtract(rearWorld);
                    double dist = toPlayer.length();
                    float range = (float) (def.taillights.range > 3.0 ? def.taillights.range : 18.0);
                    if (dist > 0.2 && dist < range) {
                        Vec3 rearDir = fwd.scale(-1.0).add(up.scale(-0.08)).normalize();
                        double cosAngle = rearDir.dot(toPlayer.scale(1.0 / dist));
                        if (cosAngle > 0.25) {
                            float spot = (float) Mth.clamp((cosAngle - 0.25) / 0.75, 0.0, 1.0);
                            float normDist = (float) (dist / range);
                            float falloff = (1.0f - normDist * normDist);
                            falloff *= falloff;
                            float redInten = v.isBraking() ? 0.90f : 0.65f;
                            accum.add(1.0f * redInten * spot * falloff, 0.06f * redInten * spot * falloff, 0.06f * redInten * spot * falloff);
                        }
                    }
                }
            }
        }

        if (accum.lengthSquared() < 0.0001f) return null;
        accum.x = Math.min(accum.x, 1.25f);
        accum.y = Math.min(accum.y, 1.25f);
        accum.z = Math.min(accum.z, 1.25f);
        return accum;
    }
}
