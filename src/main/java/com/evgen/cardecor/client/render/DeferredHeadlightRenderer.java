package com.evgen.cardecor.client.render;

import com.evgen.cardecor.client.CarDecorShadowFlag;
import com.evgen.cardecor.client.IrisCompat;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
    private static int locCookieLow = -1;
    private static int locCookieHigh = -1;
    private static int locHasCookieLow = -1;
    private static int locHasCookieHigh = -1;
    private static int locInvMVP = -1;
    private static int locInvScreenSize = -1;
    private static int locLightCount = -1;

    private static final int[] locLightOrigin = new int[MAX_LIGHTS];
    private static final int[] locLightDir = new int[MAX_LIGHTS];
    private static final int[] locLightColor = new int[MAX_LIGHTS];
    private static final int[] locLightRight = new int[MAX_LIGHTS];
    private static final int[] locLightUp = new int[MAX_LIGHTS];
    private static final int[] locConeAngleCos = new int[MAX_LIGHTS];
    private static final int[] locRange = new int[MAX_LIGHTS];
    private static final int[] locIntensity = new int[MAX_LIGHTS];
    private static final int[] locCookieType = new int[MAX_LIGHTS];
    private static final int[] locShadowSlot = new int[MAX_LIGHTS];

    private static final int SHADOW_SLOTS = 2;
    private static final int SHADOW_MAP_SIZE = 1024;
    private static final int[] shadowFbos = new int[SHADOW_SLOTS];
    private static final int[] shadowDepthTexs = new int[SHADOW_SLOTS];
    private static final Matrix4f[] slotLightViewProjs = new Matrix4f[SHADOW_SLOTS];
    private static final float[] slotTanHalfFovY = new float[SHADOW_SLOTS];
    static {
        java.util.Arrays.fill(shadowFbos, -1);
        java.util.Arrays.fill(shadowDepthTexs, -1);
        for (int i = 0; i < SHADOW_SLOTS; i++) {
            slotLightViewProjs[i] = new Matrix4f();
        }
    }

    private static final int[] locShadowMap = new int[SHADOW_SLOTS];
    private static final int[] locLightViewProj = new int[SHADOW_SLOTS];
    private static final int[] locShadowTanHalfFov = new int[SHADOW_SLOTS];
    private static int locShadowTexelSize = -1;
    private static int locShadowProjA = -1;
    private static int locShadowProjB = -1;
    private static int locShadowBiasNormal = -1;
    private static int locShadowBiasSlope = -1;

    private static int cookieLowTexId = -1;
    private static int cookieHighTexId = -1;
    private static final ResourceLocation LOW_BEAM_COOKIE = new ResourceLocation("cardecor", "textures/misc/headlight_low_beam.png");
    private static final ResourceLocation HIGH_BEAM_COOKIE = new ResourceLocation("cardecor", "textures/misc/headlight_high_beam.png");

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
        Vector3f right;
        Vector3f up;
        float coneAngleCos;
        float range;
        float intensity;
        int cookieType; // 0 = Low Beam (Cutoff), 1 = High Beam, 2 = Taillight/Omni
        int shadowSlot = -1; // 0 or 1, or -1 if unshadowed
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
            locCookieLow = GL20.glGetUniformLocation(shaderProgram, "uCookieLow");
            locCookieHigh = GL20.glGetUniformLocation(shaderProgram, "uCookieHigh");
            locHasCookieLow = GL20.glGetUniformLocation(shaderProgram, "uHasCookieLow");
            locHasCookieHigh = GL20.glGetUniformLocation(shaderProgram, "uHasCookieHigh");

            locInvMVP = GL20.glGetUniformLocation(shaderProgram, "uInvMVP");
            locInvScreenSize = GL20.glGetUniformLocation(shaderProgram, "uInvScreenSize");
            locLightCount = GL20.glGetUniformLocation(shaderProgram, "uLightCount");

            for (int s = 0; s < SHADOW_SLOTS; s++) {
                locShadowMap[s] = GL20.glGetUniformLocation(shaderProgram, "uShadowMap[" + s + "]");
                locLightViewProj[s] = GL20.glGetUniformLocation(shaderProgram, "uLightViewProj[" + s + "]");
                locShadowTanHalfFov[s] = GL20.glGetUniformLocation(shaderProgram, "uShadowTanHalfFov[" + s + "]");
            }
            locShadowTexelSize = GL20.glGetUniformLocation(shaderProgram, "uShadowTexelSize");
            locShadowProjA = GL20.glGetUniformLocation(shaderProgram, "uShadowProjA");
            locShadowProjB = GL20.glGetUniformLocation(shaderProgram, "uShadowProjB");
            locShadowBiasNormal = GL20.glGetUniformLocation(shaderProgram, "uShadowBiasNormal");
            locShadowBiasSlope = GL20.glGetUniformLocation(shaderProgram, "uShadowBiasSlope");

            for (int i = 0; i < MAX_LIGHTS; i++) {
                locLightOrigin[i] = GL20.glGetUniformLocation(shaderProgram, "uLightOrigin[" + i + "]");
                locLightDir[i] = GL20.glGetUniformLocation(shaderProgram, "uLightDir[" + i + "]");
                locLightColor[i] = GL20.glGetUniformLocation(shaderProgram, "uLightColor[" + i + "]");
                locLightRight[i] = GL20.glGetUniformLocation(shaderProgram, "uLightRight[" + i + "]");
                locLightUp[i] = GL20.glGetUniformLocation(shaderProgram, "uLightUp[" + i + "]");
                locConeAngleCos[i] = GL20.glGetUniformLocation(shaderProgram, "uConeAngleCos[" + i + "]");
                locRange[i] = GL20.glGetUniformLocation(shaderProgram, "uRange[" + i + "]");
                locIntensity[i] = GL20.glGetUniformLocation(shaderProgram, "uIntensity[" + i + "]");
                locCookieType[i] = GL20.glGetUniformLocation(shaderProgram, "uCookieType[" + i + "]");
                locShadowSlot[i] = GL20.glGetUniformLocation(shaderProgram, "uShadowSlot[" + i + "]");
            }

            // Create Fullscreen Quad VAO & VBO
            int prevInitVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
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

            GL30.glBindVertexArray(prevInitVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            initialized = true;
            LOGGER.info("[DeferredHeadlight] Initialized deferred lighting pipeline with dynamic entity shadow mapping");
        } catch (Throwable t) {
            LOGGER.error("[DeferredHeadlight] Failed to initialize pipeline: {}", t.getMessage(), t);
            initFailed = true;
        }
    }

    private static int getOrLoadCookieTexture(ResourceLocation loc) {
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.getTextureManager().bindForSetup(loc);
            AbstractTexture tex = mc.getTextureManager().getTexture(loc);
            if (tex != null) {
                int id = tex.getId();
                if (id > 0) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    return id;
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[DeferredHeadlight] Failed to load cookie texture: {}", loc, t);
        }
        return -1;
    }

    private static boolean allocateShadowSlot(int s) {
        if (s < 0 || s >= SHADOW_SLOTS) return false;
        if (shadowDepthTexs[s] != -1 && shadowFbos[s] != -1) return true;

        if (shadowDepthTexs[s] != -1) {
            GL11.glDeleteTextures(shadowDepthTexs[s]);
            shadowDepthTexs[s] = -1;
        }
        if (shadowFbos[s] != -1) {
            GL30.glDeleteFramebuffers(shadowFbos[s]);
            shadowFbos[s] = -1;
        }

        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, new float[]{1.0f, 1.0f, 1.0f, 1.0f});
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, tex, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            LOGGER.error("[DeferredHeadlight] Shadow slot {} FBO incomplete (status={})", s, status);
            GL11.glDeleteTextures(tex);
            GL30.glDeleteFramebuffers(fbo);
            return false;
        }

        shadowDepthTexs[s] = tex;
        shadowFbos[s] = fbo;
        return true;
    }

    private static void renderShadowPass(int slot, SpotLight sl, Camera camera, ClientLevel level, DrivableVehicleEntity sourceVehicle, float partialTick) {
        if (!allocateShadowSlot(slot)) return;

        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufSource = mc.renderBuffers().bufferSource();

        Vector3f dir = sl.dir;
        float ux = 0.0f;
        float uy = 1.0f;
        float uz = 0.0f;
        if (Math.abs(dir.y) > 0.99f) {
            ux = 1.0f;
            uy = 0.0f;
        }

        Matrix4f lightView = new Matrix4f().lookAt(
            sl.relOrigin.x, sl.relOrigin.y, sl.relOrigin.z,
            sl.relOrigin.x + dir.x, sl.relOrigin.y + dir.y, sl.relOrigin.z + dir.z,
            ux, uy, uz
        );

        float coneHalf = (float) Math.acos(Mth.clamp(sl.coneAngleCos, -1.0f, 1.0f));
        float fovY = Math.max(coneHalf * 2.0f * 1.15f, (float) Math.toRadians(10.0));
        float shadowFar = Math.min(sl.range, 55.0f);
        Matrix4f lightProj = new Matrix4f().perspective(fovY, 1.0f, 0.05f, shadowFar);

        slotTanHalfFovY[slot] = (float) Math.tan(fovY * 0.5);
        slotLightViewProjs[slot].set(lightProj).mul(lightView);

        Vec3 camPos = camera.getPosition();
        Frustum shadowFrustum = new Frustum(lightView, lightProj);
        shadowFrustum.prepare(camPos.x, camPos.y, camPos.z);

        int boundFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] savedViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        try {
            CarDecorShadowFlag.ACTIVE = true;
            IrisCompat.suspendIrisShaderOverrides();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowFbos[slot]);
            GL11.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            RenderSystem.setProjectionMatrix(lightProj, VertexSorting.DISTANCE_TO_ORIGIN);

            PoseStack poseStack = new PoseStack();
            poseStack.mulPoseMatrix(lightView);

            double lightWX = camPos.x + sl.relOrigin.x;
            double lightWY = camPos.y + sl.relOrigin.y;
            double lightWZ = camPos.z + sl.relOrigin.z;

            for (Entity entity : level.entitiesForRendering()) {
                if (entity.isRemoved()) continue;
                if (entity == sourceVehicle) continue; // source vehicle can't shadow its own outward beam
                if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                if (entity.isPassenger() && entity.getVehicle() == sourceVehicle) continue;

                AABB bb = entity.getBoundingBox();
                if (bb == null || !shadowFrustum.isVisible(bb.inflate(0.5))) continue;
                if (entity.distanceToSqr(lightWX, lightWY, lightWZ) > (shadowFar * shadowFar)) continue;

                double ex = Mth.lerp(partialTick, entity.xOld, entity.getX());
                double ey = Mth.lerp(partialTick, entity.yOld, entity.getY());
                double ez = Mth.lerp(partialTick, entity.zOld, entity.getZ());
                float yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

                try {
                    dispatcher.render(entity, ex - camPos.x, ey - camPos.y, ez - camPos.z, yRot, partialTick, poseStack, bufSource, LightTexture.pack(15, 15));
                } catch (Throwable ignored) {}
            }

            try {
                bufSource.endBatch();
            } catch (Throwable ignored) {}
        } finally {
            IrisCompat.resumeIrisShaderOverrides();
            CarDecorShadowFlag.ACTIVE = false;
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.DISTANCE_TO_ORIGIN);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
            GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
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

        // Sort vehicles by distance to camera
        vehicles.sort(Comparator.comparingDouble(v -> v.distanceToSqr(camPos)));

        DrivableVehicleEntity primaryVehicle = vehicles.get(0);

        activeSpotlights.clear();
        int assignedShadowSlots = 0;

        for (DrivableVehicleEntity vehicle : vehicles) {
            if (activeSpotlights.size() >= MAX_LIGHTS) break;

            int mode = vehicle.getHeadlightMode();
            boolean frontLightsOn = vehicle.isHeadlightsOn() && mode > 0;
            boolean brakeLightsOn = vehicle.isBraking();
            if (!frontLightsOn && !brakeLightsOn) continue;

            double vX = Mth.lerp(partialTick, vehicle.xOld, vehicle.getX());
            double vY = Mth.lerp(partialTick, vehicle.yOld, vehicle.getY());
            double vZ = Mth.lerp(partialTick, vehicle.zOld, vehicle.getZ());
            float yaw = Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot());
            float smoothPitchRad = Mth.lerp(partialTick, vehicle.prevPitch, vehicle.currentPitch);
            float smoothRollRad = Mth.lerp(partialTick, vehicle.prevRoll, vehicle.currentRoll);

            double yawRad = Math.toRadians(yaw);
            double cosY = Math.cos(yawRad);
            double sinY = Math.sin(yawRad);

            Vec3 baseFwd = new Vec3(-sinY, 0.0, cosY);
            Vec3 baseRight = new Vec3(-cosY, 0.0, -sinY);
            Vec3 baseUp = new Vec3(0.0, 1.0, 0.0);

            double cosP = Math.cos(smoothPitchRad);
            double sinP = Math.sin(smoothPitchRad);
            Vec3 fwd = baseFwd.scale(cosP).add(baseUp.scale(-sinP)).normalize();
            Vec3 up = baseFwd.scale(sinP).add(baseUp.scale(cosP)).normalize();

            double cosR = Math.cos(smoothRollRad);
            double sinR = Math.sin(smoothRollRad);
            Vec3 right = baseRight.scale(cosR).add(up.scale(sinR)).normalize();
            up = right.cross(fwd).normalize();

            VehicleDefinition def = VehiclePackManager.get(vehicle.getVehicleId());
            if (def == null) continue;

            if (frontLightsOn && def.headlights != null) {
                List<VehicleDefinition.LightSourceDef> resolvedLights = def.headlights.getResolvedLights(def);
                if (resolvedLights != null && !resolvedLights.isEmpty()) {
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
                        double lx = ((lDef.pos != null && lDef.pos.length > 0) ? lDef.pos[0] : 0.0) * scale;
                        double ly = ((lDef.pos != null && lDef.pos.length > 1) ? lDef.pos[1] : 0.75) * scale + def.groundOffsetY + vehicle.physics.rideHeight;
                        double lz = ((lDef.pos != null && lDef.pos.length > 2) ? lDef.pos[2] : -2.2) * scale;

                        double forwardPush = Math.max(0.12, 0.05 * scale);
                        Vec3 lampWorld = new Vec3(vX, vY, vZ)
                            .add(right.scale(lx))
                            .add(up.scale(ly))
                            .add(fwd.scale(-lz + forwardPush));

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

                        Vec3 lRight = lampDir.cross(up).lengthSqr() > 1e-4 ? lampDir.cross(up).normalize() : right;
                        Vec3 lUp = lRight.cross(lampDir).normalize();

                        SpotLight sl = new SpotLight();
                        sl.relOrigin = new Vector3f((float)(lampWorld.x - camPos.x), (float)(lampWorld.y - camPos.y), (float)(lampWorld.z - camPos.z));
                        sl.dir = new Vector3f((float)lampDir.x, (float)lampDir.y, (float)lampDir.z);
                        sl.right = new Vector3f((float)lRight.x, (float)lRight.y, (float)lRight.z);
                        sl.up = new Vector3f((float)lUp.x, (float)lUp.y, (float)lUp.z);
                        sl.color = new Vector3f(col[0], col[1], col[2]);
                        sl.coneAngleCos = lightConeCos;
                        sl.range = lightRange;
                        sl.intensity = lightIndivIntensity;
                        sl.cookieType = (mode == 2) ? 1 : 0;
                        sl.shadowSlot = -1;
                        activeSpotlights.add(sl);
                    }
                }
            }

            // Brake lights deferred spotlight (ONLY when explicitly braking, never during normal driving)
            boolean hasBrakeLight = brakeLightsOn && activeSpotlights.size() < MAX_LIGHTS;
            if (hasBrakeLight) {
                float scale = (def.scale > 0.05f) ? def.scale : 1.0f;
                double[] leftRear = def.getTaillightLeftPos();
                double rY = (leftRear[1] * scale) + def.groundOffsetY + vehicle.physics.rideHeight;
                double rZ = leftRear[2] * scale;

                double rearPush = Math.max(0.12, 0.05 * scale);
                Vec3 rearOriginWorld = new Vec3(vX, vY, vZ)
                    .add(up.scale(rY))
                    .add(fwd.scale(-rZ - rearPush));

                Vec3 rearDir = fwd.scale(-1.0).add(up.scale(-0.08)).normalize();

                float timeOfDay = level.getTimeOfDay(partialTick);
                float sunCosine = (float) Math.cos(timeOfDay * Math.PI * 2.0);
                float dayFactor = Mth.clamp(sunCosine * 2.0f + 0.5f, 0.0f, 1.0f);
                net.minecraft.core.BlockPos vPos = vehicle.blockPosition();
                int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, vPos);
                float sunExposure = dayFactor * (skyLight / 15.0f);
                float dayNightRedFactor = Mth.lerp(sunExposure, 1.0f, 0.50f);

                float baseBrakeIntensity = IrisCompat.isShaderPackInUse() ? 0.65f : 0.88f;
                float brakeIntensity = baseBrakeIntensity * dayNightRedFactor;
                float maxRange = (float) (def.taillights != null && def.taillights.range > 3.0 ? def.taillights.range : 18.0);

                if (brakeIntensity > 0.01f) {
                    SpotLight bsl = new SpotLight();
                    bsl.relOrigin = new Vector3f((float)(rearOriginWorld.x - camPos.x), (float)(rearOriginWorld.y - camPos.y), (float)(rearOriginWorld.z - camPos.z));
                    bsl.dir = new Vector3f((float)rearDir.x, (float)rearDir.y, (float)rearDir.z);
                    bsl.right = new Vector3f((float)right.x, (float)right.y, (float)right.z);
                    bsl.up = new Vector3f((float)up.x, (float)up.y, (float)up.z);
                    bsl.color = new Vector3f(1.0f, 0.05f, 0.05f);
                    bsl.coneAngleCos = (float) Math.cos(Math.toRadians(68.0));
                    bsl.range = maxRange;
                    bsl.intensity = brakeIntensity;
                    bsl.cookieType = 2; // Taillight/Omni
                    bsl.shadowSlot = -1;
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
        }
        if (depthTex <= 0) {
            depthTex = mainTarget.getDepthTextureId();
        }
        if (depthTex <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastDiagnosticTime > 3000L) {
            lastDiagnosticTime = now;
            LOGGER.info("[DeferredHeadlight] lights={}, shaders={}, boundFbo={}, currentFbo={}, depthTex={}, w={}, h={}",
                activeSpotlights.size(), IrisCompat.isShaderPackInUse(), boundFbo, currentFbo, depthTex, width, height);
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

        // Camera View & MVP matrix
        Matrix4f view = IrisCompat.isShaderPackInUse()
            ? (modelViewMatrix != null ? new Matrix4f(modelViewMatrix) : new Matrix4f())
            : new Matrix4f().rotateX((float) Math.toRadians(camera.getXRot())).rotateY((float) Math.toRadians(camera.getYRot() + 180.0f));

        Matrix4f mvp = new Matrix4f(projectionMatrix).mul(view);
        Matrix4f invMVP = new Matrix4f(mvp).invert();

        // Save active shader program to restore cleanly after pass
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        // Render shadow maps for headlights assigned to shadow slots
        for (SpotLight sl : activeSpotlights) {
            if (sl.shadowSlot >= 0 && sl.shadowSlot < SHADOW_SLOTS) {
                renderShadowPass(sl.shadowSlot, sl, camera, level, primaryVehicle, partialTick);
            }
        }

        // Bind program & upload uniforms
        GL20.glUseProgram(shaderProgram);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(depthTex);
        GL20.glUniform1i(locDepthSampler, 0);

        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(sceneCopyTex);
        GL20.glUniform1i(locSceneSampler, 1);

        if (cookieLowTexId <= 0) cookieLowTexId = getOrLoadCookieTexture(LOW_BEAM_COOKIE);
        if (cookieHighTexId <= 0) cookieHighTexId = getOrLoadCookieTexture(HIGH_BEAM_COOKIE);

        GlStateManager._activeTexture(GL13.GL_TEXTURE2);
        GlStateManager._bindTexture(cookieLowTexId > 0 ? cookieLowTexId : 0);
        GL20.glUniform1i(locCookieLow, 2);

        GlStateManager._activeTexture(GL13.GL_TEXTURE3);
        GlStateManager._bindTexture(cookieHighTexId > 0 ? cookieHighTexId : 0);
        GL20.glUniform1i(locCookieHigh, 3);

        if (locHasCookieLow >= 0) GL20.glUniform1i(locHasCookieLow, cookieLowTexId > 0 ? 1 : 0);
        if (locHasCookieHigh >= 0) GL20.glUniform1i(locHasCookieHigh, cookieHighTexId > 0 ? 1 : 0);

        GlStateManager._activeTexture(GL13.GL_TEXTURE4);
        GlStateManager._bindTexture(shadowDepthTexs[0] > 0 ? shadowDepthTexs[0] : 0);
        GL20.glUniform1i(locShadowMap[0], 4);

        GlStateManager._activeTexture(GL13.GL_TEXTURE5);
        GlStateManager._bindTexture(shadowDepthTexs[1] > 0 ? shadowDepthTexs[1] : 0);
        GL20.glUniform1i(locShadowMap[1], 5);

        for (int s = 0; s < SHADOW_SLOTS; s++) {
            uploadMatrix(locLightViewProj[s], slotLightViewProjs[s]);
            if (locShadowTanHalfFov[s] >= 0) {
                GL20.glUniform1f(locShadowTanHalfFov[s], slotTanHalfFovY[s]);
            }
        }
        GL20.glUniform1f(locShadowTexelSize, 1.0f / (float) SHADOW_MAP_SIZE);

        float sf = 55.0f;
        if (locShadowProjA >= 0) GL20.glUniform1f(locShadowProjA, 0.05f * sf / (sf - 0.05f));
        if (locShadowProjB >= 0) GL20.glUniform1f(locShadowProjB, sf / (sf - 0.05f));
        if (locShadowBiasNormal >= 0) GL20.glUniform1f(locShadowBiasNormal, 1.5f);
        if (locShadowBiasSlope >= 0) GL20.glUniform1f(locShadowBiasSlope, 2.0f);

        uploadMatrix(locInvMVP, invMVP);
        GL20.glUniform2f(locInvScreenSize, 1.0f / (float) width, 1.0f / (float) height);

        int count = activeSpotlights.size();
        GL20.glUniform1i(locLightCount, count);

        for (int i = 0; i < count; i++) {
            SpotLight sl = activeSpotlights.get(i);
            GL20.glUniform3f(locLightOrigin[i], sl.relOrigin.x, sl.relOrigin.y, sl.relOrigin.z);
            GL20.glUniform3f(locLightDir[i], sl.dir.x, sl.dir.y, sl.dir.z);
            GL20.glUniform3f(locLightColor[i], sl.color.x, sl.color.y, sl.color.z);
            GL20.glUniform3f(locLightRight[i], sl.right.x, sl.right.y, sl.right.z);
            GL20.glUniform3f(locLightUp[i], sl.up.x, sl.up.y, sl.up.z);
            GL20.glUniform1f(locConeAngleCos[i], sl.coneAngleCos);
            GL20.glUniform1f(locRange[i], sl.range);
            GL20.glUniform1f(locIntensity[i], sl.intensity);
            GL20.glUniform1i(locCookieType[i], sl.cookieType);
            GL20.glUniform1i(locShadowSlot[i], sl.shadowSlot);
        }

        // Render full-screen quad directly to active color buffer
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();

        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(prevVao);

        // Restore active shader program
        GL20.glUseProgram(prevProgram);

        // Safely unbind scratch textures via GlStateManager so Blaze3D cache remains perfectly in sync
        GlStateManager._activeTexture(GL13.GL_TEXTURE5);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE4);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE3);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE2);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(0);

        // Ensure current framebuffer remains bound for any subsequent passes
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

    private static void ensureSceneCopy(int width, int height) {
        if (sceneCopyTex != -1 && sceneCopyFbo != -1 && sceneCopyWidth == width && sceneCopyHeight == height) {
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
}
