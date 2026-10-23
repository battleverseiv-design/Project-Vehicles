package com.evgen.cardecor.client;

import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class DrivablePackVehicleRenderer extends EntityRenderer<DrivablePackVehicleEntity> {

    public static final java.util.Map<Integer, DynamicObjMesh> deformedChassisMeshes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("minecraft", "textures/block/iron_block.png");
    private static final ResourceLocation HEADLIGHT_FLARE_TEXTURE = new ResourceLocation("cardecor", "textures/misc/headlight_flare.png");
    private static final ResourceLocation BLOOD_SPLATTER_TEXTURE = new ResourceLocation("cardecor", "textures/misc/blood_splatter.png");
    private static final ResourceLocation DIRT_SPLATTER_TEXTURE = new ResourceLocation("cardecor", "textures/misc/dirt_splatter.png");
    private static final org.joml.Quaternionf TEMP_QUAT = new org.joml.Quaternionf();
    private static long lastFrameNanoTime = 0L;

    private static class BloodDecalTemplate {
        final int zone; // 0 = bumper, 1 = hood, 2 = windshield
        final float relX;
        final float relParam; // bumper: relY (0..1); hood: relZ (0..1); windshield: progress (0..1)
        final float size;
        final int quadrant;
        final float rotDeg;

        BloodDecalTemplate(int zone, float relX, float relParam, float size, int quadrant, float rotDeg) {
            this.zone = zone;
            this.relX = relX;
            this.relParam = relParam;
            this.size = size;
            this.quadrant = quadrant;
            this.rotDeg = rotDeg;
        }
    }

    private static final BloodDecalTemplate[] BLOOD_TEMPLATES = new BloodDecalTemplate[]{
        // Zone 0: Front Bumper & Lower Grille (8 impacts)
        new BloodDecalTemplate(0, -0.25f, 0.50f, 0.32f, 0, 12.0f),
        new BloodDecalTemplate(0,  0.35f, 0.40f, 0.36f, 1, -25.0f),
        new BloodDecalTemplate(0,  0.05f, 0.15f, 0.28f, 2, 45.0f),
        new BloodDecalTemplate(0, -0.65f, 0.30f, 0.30f, 3, -70.0f),
        new BloodDecalTemplate(0,  0.60f, 0.35f, 0.34f, 0, 95.0f),
        new BloodDecalTemplate(0, -0.38f, 0.75f, 0.26f, 1, 150.0f),
        new BloodDecalTemplate(0,  0.18f, 0.80f, 0.30f, 2, -110.0f),
        new BloodDecalTemplate(0, -0.05f, 0.65f, 0.38f, 3, 30.0f),

        // Zone 1: Front Hood (8 pools/streaks)
        new BloodDecalTemplate(1,  0.15f, 0.10f, 0.35f, 0, -15.0f),
        new BloodDecalTemplate(1, -0.45f, 0.25f, 0.40f, 1, 60.0f),
        new BloodDecalTemplate(1, -0.10f, 0.45f, 0.38f, 2, -85.0f),
        new BloodDecalTemplate(1,  0.50f, 0.35f, 0.28f, 3, 110.0f),
        new BloodDecalTemplate(1, -0.28f, 0.70f, 0.42f, 0, -40.0f),
        new BloodDecalTemplate(1,  0.32f, 0.65f, 0.36f, 1, 75.0f),
        new BloodDecalTemplate(1,  0.08f, 0.85f, 0.32f, 2, -135.0f),
        new BloodDecalTemplate(1, -0.60f, 0.50f, 0.30f, 3, 20.0f),

        // Zone 2: Windshield (8 realistic organic splatters & downward gravity smears)
        new BloodDecalTemplate(2, -0.42f, 0.35f, 0.18f, 0,  15.0f),
        new BloodDecalTemplate(2,  0.08f, 0.50f, 0.20f, 1,   0.0f), // Downward drip
        new BloodDecalTemplate(2,  0.45f, 0.30f, 0.17f, 2, -18.0f),
        new BloodDecalTemplate(2, -0.18f, 0.20f, 0.19f, 3,  10.0f),
        new BloodDecalTemplate(2, -0.55f, 0.70f, 0.16f, 0,  -8.0f),
        new BloodDecalTemplate(2,  0.22f, 0.75f, 0.18f, 1,   2.0f), // Downward drip
        new BloodDecalTemplate(2,  0.58f, 0.65f, 0.17f, 2,  22.0f),
        new BloodDecalTemplate(2, -0.05f, 0.82f, 0.15f, 3, -12.0f)
    };

    private static class DirtDecalTemplate {
        final int zone; // 0 = front fender, 1 = door/rocker, 2 = rear quarter, 3 = tailgate/rear bumper
        final int side; // -1 = right (-X), +1 = left (+X), 0 = rear
        final float relParam1; // fender/quarter: delta Z from wheel; door: progress FL->RL; rear: relX
        final float relParam2; // height or relY
        final float size;
        final int quadrant;
        final float rotDeg;

        DirtDecalTemplate(int zone, int side, float relParam1, float relParam2, float size, int quadrant, float rotDeg) {
            this.zone = zone;
            this.side = side;
            this.relParam1 = relParam1;
            this.relParam2 = relParam2;
            this.size = size;
            this.quadrant = quadrant;
            this.rotDeg = rotDeg;
        }
    }

    private static final DirtDecalTemplate[] DIRT_TEMPLATES = new DirtDecalTemplate[]{
        // Zone 0: Front Wheel Arches & Fenders (8 decals)
        new DirtDecalTemplate(0,  1, -0.30f, 0.48f, 0.35f, 0,  25.0f),
        new DirtDecalTemplate(0,  1,  0.00f, 0.75f, 0.32f, 1, -40.0f),
        new DirtDecalTemplate(0,  1,  0.35f, 0.58f, 0.38f, 2,  85.0f),
        new DirtDecalTemplate(0,  1,  0.15f, 0.88f, 0.28f, 3, -115.0f),
        new DirtDecalTemplate(0, -1, -0.25f, 0.50f, 0.36f, 0, -20.0f),
        new DirtDecalTemplate(0, -1,  0.05f, 0.78f, 0.30f, 1,  55.0f),
        new DirtDecalTemplate(0, -1,  0.30f, 0.55f, 0.40f, 2, -75.0f),
        new DirtDecalTemplate(0, -1,  0.10f, 0.85f, 0.26f, 3, 130.0f),

        // Zone 1: Rocker Panels & Lower Doors (8 decals)
        new DirtDecalTemplate(1,  1,  0.25f, 0.42f, 0.38f, 0, -15.0f),
        new DirtDecalTemplate(1,  1,  0.50f, 0.52f, 0.42f, 1,  70.0f),
        new DirtDecalTemplate(1,  1,  0.75f, 0.45f, 0.36f, 2, -95.0f),
        new DirtDecalTemplate(1,  1,  0.60f, 0.68f, 0.28f, 3,  40.0f),
        new DirtDecalTemplate(1, -1,  0.30f, 0.44f, 0.36f, 0,  30.0f),
        new DirtDecalTemplate(1, -1,  0.55f, 0.50f, 0.40f, 1, -65.0f),
        new DirtDecalTemplate(1, -1,  0.80f, 0.46f, 0.38f, 2, 110.0f),
        new DirtDecalTemplate(1, -1,  0.45f, 0.65f, 0.30f, 3, -35.0f),

        // Zone 2: Rear Wheel Arches & Quarter Panels (8 decals)
        new DirtDecalTemplate(2,  1, -0.35f, 0.52f, 0.38f, 0,  45.0f),
        new DirtDecalTemplate(2,  1,  0.00f, 0.80f, 0.34f, 1, -80.0f),
        new DirtDecalTemplate(2,  1,  0.35f, 0.60f, 0.42f, 2, 120.0f),
        new DirtDecalTemplate(2,  1,  0.55f, 0.45f, 0.32f, 3, -25.0f),
        new DirtDecalTemplate(2, -1, -0.30f, 0.50f, 0.36f, 0, -50.0f),
        new DirtDecalTemplate(2, -1,  0.05f, 0.82f, 0.32f, 1,  75.0f),
        new DirtDecalTemplate(2, -1,  0.40f, 0.62f, 0.44f, 2, -110.0f),
        new DirtDecalTemplate(2, -1,  0.50f, 0.43f, 0.30f, 3,  35.0f),

        // Zone 3: Tailgate & Rear Bumper (8 decals)
        new DirtDecalTemplate(3,  0, -0.55f, 0.45f, 0.38f, 0, -30.0f),
        new DirtDecalTemplate(3,  0,  0.05f, 0.50f, 0.44f, 1,  85.0f),
        new DirtDecalTemplate(3,  0,  0.60f, 0.42f, 0.36f, 2, -70.0f),
        new DirtDecalTemplate(3,  0, -0.10f, 0.75f, 0.42f, 3, 135.0f),
        new DirtDecalTemplate(3,  0, -0.45f, 0.80f, 0.34f, 0,  20.0f),
        new DirtDecalTemplate(3,  0,  0.40f, 0.78f, 0.36f, 1, -115.0f),
        new DirtDecalTemplate(3,  0,  0.00f, 0.60f, 0.30f, 2,  45.0f),
        new DirtDecalTemplate(3,  0,  0.20f, 0.95f, 0.28f, 3, -150.0f)
    };


    public DrivablePackVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F; // Disable vanilla circular block-raycast shadow to save thousands of block queries
    }

    @Override
    public boolean shouldRender(DrivablePackVehicleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }



    @Override
    public ResourceLocation getTextureLocation(DrivablePackVehicleEntity entity) {
        String packId = entity.getVehicleId();
        VehicleDefinition def = VehiclePackManager.get(packId);
        ClientVehicleAssets assets = ClientVehiclePackLoader.getAssets(def);
        if (assets != null && assets.clientTexture != null) {
            return assets.clientTexture;
        }
        return FALLBACK_TEXTURE;
    }

    @Override
    public void render(DrivablePackVehicleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String packId = entity.getVehicleId();
        VehicleDefinition def = VehiclePackManager.get(packId);
        if (def == null) return;

        // Camera distance LOD checks
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = (mc.gameRenderer.getMainCamera() != null) ? mc.gameRenderer.getMainCamera().getPosition() : Vec3.ZERO;
        double camDistSqr = (mc.gameRenderer.getMainCamera() != null) ?
            camPos.distanceToSqr(entity.position()) : 0.0;
        if (camDistSqr >= 6400.0) return; // Beyond 80 blocks: skip entity render!

        boolean isShadowPass = IrisCompat.isRenderingShadowPass();
        if (isShadowPass && camDistSqr >= 900.0) return; // Beyond 30m in shadow pass: skip shadow casting entirely!

        // Smooth, non-jarring LOD distances:
        // 0 to 45 blocks (2025 m^2): close cockpit range (steering wheel, animated components)
        boolean isVeryClose = !isShadowPass && camDistSqr < 2025.0;
        // 0 to 64 blocks (4096 m^2): detailed range (mirrors, handles, seats, dashboard, pristine exterior)
        boolean isClose = !isShadowPass && camDistSqr < 4096.0;
        // 64 to 75 blocks (5625 m^2): LOD 1
        boolean isMediumRange = camDistSqr < 5625.0;
        // 75 to 80 blocks (6400 m^2): LOD 2 (outer silhouette)
        boolean isFarRange = camDistSqr < 6400.0;

        poseStack.pushPose();

        // 6-DOF Authoritative Quaternion Rotation around Center of Mass
        org.joml.Quaternionf smoothQuat = entity.getSmoothRotation(partialTicks, TEMP_QUAT);
        float comY = (float) entity.odeBody.comHeight;
        poseStack.translate(0.0F, comY, 0.0F);
        poseStack.mulPose(smoothQuat);
        poseStack.translate(0.0F, -comY, 0.0F);

        // World lighting: use packedLight directly with safety floor, avoiding CPU level light queries
        int blockLight = Math.max(packedLight & 0xFFFF, 12);
        int skyLight   = Math.max((packedLight >> 16) & 0xFFFF, 12);
        int safePackedLight = (skyLight << 16) | blockLight;

        float scale = def.scale;
        float groundOffsetY = def.groundOffsetY;

        poseStack.translate(0.0, groundOffsetY, 0.0);
        poseStack.scale(scale, scale, scale);

        float smoothRollRad = Mth.lerp(partialTicks, entity.prevRoll, entity.currentRoll);
        float smoothPitchRad = Mth.lerp(partialTicks, entity.prevPitch, entity.currentPitch);
        float rollDeg = (float) Math.toDegrees(smoothRollRad);
        float pitchDeg = (float) Math.toDegrees(smoothPitchRad);

        // Dynamic Engine & Body Shake based on bodyShakeIntensity and Idle RPM (culled beyond 24 blocks, skipped when engine off)
        float bodyShakeMult = entity.getEntityData().get(DrivableVehicleEntity.BODY_SHAKE);
        float idleVibeRoll = 0.0F;
        float idleVibePitch = 0.0F;
        float idleVibeY = 0.0F;

        if (isClose && entity.isEngineOn() && bodyShakeMult > 0.001F) {
            double time = System.currentTimeMillis();
            double curRpm = entity.getEngineRPM();
            double idleSpeedRatio = Math.max(0.4, curRpm / 850.0);
            float vibeFreq = (float) (0.032 * idleSpeedRatio);
            float isIdle = (Math.abs(entity.physics.speed) < 0.20) ? 1.0F : 0.35F;
            float vibeAmp = isIdle * bodyShakeMult * 0.35F; // Strong, visceral vintage body vibration
            idleVibeRoll = (float) Math.sin(time * vibeFreq) * vibeAmp;
            idleVibePitch = (float) Math.cos(time * (vibeFreq * 0.85F)) * (vibeAmp * 0.65F);
            idleVibeY = (float) Math.sin(time * (vibeFreq * 1.7F)) * (0.0035F * vibeAmp);
        }

        ClientVehicleAssets assets = ClientVehiclePackLoader.getAssets(def);
        ResourceLocation tex = (assets != null && assets.clientTexture != null) ? assets.clientTexture : getTextureLocation(entity);
        VertexConsumer vertexConsumer = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(tex));

        float smoothSteerRad = Mth.lerp(partialTicks, entity.prevSteer, entity.currentSteer);
        float steerAngleDeg = -(float) Math.toDegrees(smoothSteerRad);
        float flSpin = -entity.getWheelRotation(0, partialTicks);
        float frSpin = -entity.getWheelRotation(1, partialTicks);
        float rlSpin = -entity.getWheelRotation(2, partialTicks);
        float rrSpin = -entity.getWheelRotation(3, partialTicks);

        // =========================================================================
        // 1. 4 ANIMATED WHEELS (With Dynamic Suspension Camber, Caster Tilt & Wheel Lift)
        // =========================================================================
        float chassisDipY = Mth.lerp(partialTicks, entity.prevChassisDipY, entity.currentChassisDipY);
        float chassisDipPitch = Mth.lerp(partialTicks, entity.prevChassisDipPitch, entity.currentChassisDipPitch);
        float chassisDipRoll = Mth.lerp(partialTicks, entity.prevChassisDipRoll, entity.currentChassisDipRoll);

        poseStack.pushPose();
        if (Math.abs(chassisDipPitch) > 0.01F || Math.abs(chassisDipRoll) > 0.01F || Math.abs(chassisDipY) > 0.001F) {
            poseStack.translate(0.0F, chassisDipY, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(chassisDipPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(chassisDipRoll));
        }

        float[] fl = def.wheels.frontLeft;
        float[] fr = def.wheels.frontRight;
        float[] rl = def.wheels.rearLeft;
        float[] rr = def.wheels.rearRight;

        long currentNanoTime = System.nanoTime();
        float dtFrame = (lastFrameNanoTime > 0) ? (float) ((currentNanoTime - lastFrameNanoTime) * 1e-9) : 0.016f;
        if (dtFrame > 0.0001f) {
            lastFrameNanoTime = currentNanoTime;
        }
        dtFrame = Mth.clamp(dtFrame, 0.001f, 0.1f);

        float compFL = entity.getSmoothedSuspensionCompression(0, dtFrame);
        float compFR = entity.getSmoothedSuspensionCompression(1, dtFrame);
        float compRL = entity.getSmoothedSuspensionCompression(2, dtFrame);
        float compRR = entity.getSmoothedSuspensionCompression(3, dtFrame);

        double baseRadius = (def.wheels != null && def.wheels.radius > 0.05) ? def.wheels.radius : 0.38;

        for (int i = 0; i < 4; i++) {
            float[] hub = switch (i) {
                case 0 -> new float[]{fl[0], fl[1] + compFL, fl[2]};
                case 1 -> new float[]{fr[0], fr[1] + compFR, fr[2]};
                case 2 -> new float[]{rl[0], rl[1] + compRL, rl[2]};
                default -> new float[]{rr[0], rr[1] + compRR, rr[2]};
            };
            float spin = switch (i) {
                case 0 -> flSpin;
                case 1 -> frSpin;
                case 2 -> rlSpin;
                default -> rrSpin;
            };
            boolean isFront = (i == 0 || i == 1);
            boolean isRight = (i == 1 || i == 3);

            float camberRad = entity.getSmoothedCamberRad(i, dtFrame);
            float camberDeg = (float) Math.toDegrees(camberRad);

            // 1. RENDER MODULAR BRAKE (Visible behind rim spokes AND on bare hub when wheel is removed)
            String bId = entity.getInstalledBrakeId(i);
            if (bId != null && !bId.isEmpty()) {
                com.evgen.cardecor.pack.BrakeDefinition bDef = com.evgen.cardecor.pack.ModularWheelRegistry.getBrake(bId);
                DirectObjModel bModel = (bDef != null) ? ClientVehiclePackLoader.getBrakeModel(bDef) : null;
                if (bModel != null) {
                    poseStack.pushPose();
                    poseStack.translate(hub[0], hub[1], hub[2]);
                    if (isFront) {
                        poseStack.mulPose(Axis.YP.rotationDegrees(steerAngleDeg));
                    }
                    poseStack.mulPose(Axis.ZP.rotationDegrees(camberDeg));
                    if (isRight) {
                        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    }
                    float mScale = (float) ((def.wheels != null && def.wheels.radius > 0.05) ? (def.wheels.radius / 0.375) * 1.15F : 1.2F);
                    poseStack.scale(mScale, mScale, mScale);
                    VertexConsumer brakeVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(ClientVehiclePackLoader.MODULAR_WHEEL_PALETTE));
                    bModel.render(poseStack, brakeVc, safePackedLight, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }

            // 2. RENDER WHEEL (TIRE + RIM)
            if (!entity.isWheelInstalled(i)) continue;

            String tireId = entity.getInstalledTireId(i);
            String rimId = entity.getInstalledRimId(i);
            com.evgen.cardecor.pack.TireDefinition tDef = com.evgen.cardecor.pack.ModularWheelRegistry.getTire(tireId);
            com.evgen.cardecor.pack.RimDefinition rDef = com.evgen.cardecor.pack.ModularWheelRegistry.getRim(rimId);

            DirectObjModel tireModel = (tDef != null) ? ClientVehiclePackLoader.getTireModel(tDef) : null;
            DirectObjModel rimModel = (rDef != null) ? ClientVehiclePackLoader.getRimModel(rDef) : null;

            if (tireModel != null || rimModel != null) {
                // Modular Rim & Tire rendering with shared master palette (dynamically selects clean, worn, rusty, or muddy)
                ResourceLocation dynPalette = ClientVehiclePackLoader.getDynamicWheelPalette(entity, i);
                VertexConsumer wheelVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(dynPalette));
                poseStack.pushPose();
                poseStack.translate(hub[0], hub[1], hub[2]);
                if (isFront) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(steerAngleDeg));
                }
                poseStack.mulPose(Axis.ZP.rotationDegrees(camberDeg));
                poseStack.mulPose(Axis.XP.rotationDegrees(spin));
                if (isRight) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                }
                float mScale = (float) ((def.wheels != null && def.wheels.radius > 0.05) ? (def.wheels.radius / 0.375) * 1.15F : 1.2F);
                poseStack.scale(mScale, mScale, mScale);
                if (tireModel != null) {
                    tireModel.render(poseStack, wheelVc, safePackedLight, OverlayTexture.NO_OVERLAY);
                }
                if (rimModel != null) {
                    rimModel.render(poseStack, wheelVc, safePackedLight, OverlayTexture.NO_OVERLAY);
                }
                poseStack.popPose();
            } else {
                // Legacy Vehicle Pack Wheel fallback
                String wId = entity.getInstalledWheelId(i);
                com.evgen.cardecor.pack.WheelDefinition wDef = com.evgen.cardecor.pack.WheelRegistry.get(wId);
                DynamicObjMesh customMesh = (wDef != null) ? ClientVehiclePackLoader.getWheelMesh(wDef) : null;
                DynamicObjMesh activeMesh = (customMesh != null && !customMesh.vertices.isEmpty()) ? customMesh : ((assets != null) ? assets.wheelMesh : null);
                if (activeMesh == null || activeMesh.vertices.isEmpty()) continue;

                ResourceLocation customWheelTex = (wDef != null) ? ClientVehiclePackLoader.getWheelTexture(wDef) : null;
                ResourceLocation wheelTex = (customWheelTex != null && customWheelTex != ClientVehiclePackLoader.FALLBACK_TIRE_TEXTURE)
                        ? customWheelTex
                        : ((assets != null && assets.wheelTexture != null)
                        ? assets.wheelTexture
                        : ((activeMesh.materialName != null && assets != null && assets.materialTextures.containsKey(activeMesh.materialName))
                        ? assets.materialTextures.get(activeMesh.materialName)
                        : (customWheelTex != null ? customWheelTex : ClientVehiclePackLoader.FALLBACK_TIRE_TEXTURE)));
                VertexConsumer wheelVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(wheelTex));

                float radScale = (wDef != null && wDef.radius > 0.05) ? (float) (wDef.radius / baseRadius) : 1.0F;
                float widthScale = (wDef != null && wDef.width > 0.05) ? (float) (wDef.width / 0.22) : 1.0F;

                poseStack.pushPose();
                poseStack.translate(hub[0], hub[1], hub[2]);
                if (isFront) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(steerAngleDeg));
                }
                poseStack.mulPose(Axis.ZP.rotationDegrees(camberDeg));
                poseStack.mulPose(Axis.XP.rotationDegrees(spin));
                if (isRight) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                }
                poseStack.scale(widthScale, radScale, radScale);
                activeMesh.render(poseStack, wheelVc, safePackedLight, 255, 255, 255, 255);
                poseStack.popPose();
            }
        }
        poseStack.popPose(); // End Wheels Pose

        // =========================================================================
        // 2. SPRUNG CHASSIS & COCKPIT (Engine pulse, body shake, and dynamic suspension dip)
        // =========================================================================
        poseStack.pushPose();
        poseStack.translate(0.0, idleVibeY + chassisDipY + entity.physics.rideHeight, 0.0);
        float totalRoll = idleVibeRoll + chassisDipRoll;
        float totalPitch = idleVibePitch + chassisDipPitch;
        if (Math.abs(totalRoll) > 0.01F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(totalRoll));
        }
        if (Math.abs(totalPitch) > 0.01F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(totalPitch));
        }

        if (assets != null && ((assets.batchedStaticChassis != null && !assets.batchedStaticChassis.isEmpty()) || (assets.animatedChassisParts != null && !assets.animatedChassisParts.isEmpty()))) {
            renderAnimatedChassis(entity, def, assets, partialTicks, steerAngleDeg, poseStack, buffer, safePackedLight, tex, isClose);
        } else if (assets != null && assets.chassisMesh != null && (assets.chassisMesh.packedData != null || !assets.chassisMesh.vertices.isEmpty())) {
            DynamicObjMesh meshToRender = assets.chassisMesh;
            if (isShadowPass && assets.chassisMesh.lodUltraFarMesh != null) {
                meshToRender = assets.chassisMesh.lodUltraFarMesh;
            } else if (!isClose) {
                if (!isFarRange && assets.chassisMesh.lodUltraFarMesh != null) {
                    meshToRender = assets.chassisMesh.lodUltraFarMesh;
                } else if (!isMediumRange && assets.chassisMesh.lodFarMesh != null) {
                    meshToRender = assets.chassisMesh.lodFarMesh;
                } else if (assets.chassisMesh.lodMesh != null) {
                    meshToRender = assets.chassisMesh.lodMesh;
                }
            }
            meshToRender.render(poseStack, vertexConsumer, safePackedLight, 255, 255, 255, 255);
        }

        // Optional Spare Wheel (Mounted on bed/chassis, e.g. Vintage Pickup) - culled beyond 45 blocks or in shadow pass
        if (!isShadowPass && isFarRange && entity.hasSpareWheel()) {
            String tireId = entity.getInstalledTireId(0);
            String rimId = entity.getInstalledRimId(0);
            com.evgen.cardecor.pack.TireDefinition tDef = com.evgen.cardecor.pack.ModularWheelRegistry.getTire(tireId);
            com.evgen.cardecor.pack.RimDefinition rDef = com.evgen.cardecor.pack.ModularWheelRegistry.getRim(rimId);

            DirectObjModel tireModel = (tDef != null) ? ClientVehiclePackLoader.getTireModel(tDef) : null;
            DirectObjModel rimModel = (rDef != null) ? ClientVehiclePackLoader.getRimModel(rDef) : null;

            if (tireModel != null || rimModel != null) {
                // High-detail Modular Spare Wheel mounted in truck bed / chassis
                ResourceLocation sparePalette = ClientVehiclePackLoader.getDynamicWheelPalette(entity, -1);
                VertexConsumer spareVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(sparePalette));
                poseStack.pushPose();
                // Exact measured anchor of pickup bed spare mount: [-0.027, 1.369, 0.401]
                poseStack.translate(-0.027F, 1.369F, 0.401F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F)); // Rotate to stand vertically against bulkhead
                float mScale = (float) ((def.wheels != null && def.wheels.radius > 0.05) ? (def.wheels.radius / 0.375) * 1.15F : 1.2F);
                poseStack.scale(mScale, mScale, mScale);
                if (tireModel != null) {
                    tireModel.render(poseStack, spareVc, safePackedLight, OverlayTexture.NO_OVERLAY);
                }
                if (rimModel != null) {
                    rimModel.render(poseStack, spareVc, safePackedLight, OverlayTexture.NO_OVERLAY);
                }
                poseStack.popPose();
            } else if (assets != null && assets.spareWheelMesh != null && (assets.spareWheelMesh.packedData != null || !assets.spareWheelMesh.vertices.isEmpty())) {
                DynamicObjMesh spareToRender = assets.spareWheelMesh;
                if (!isClose) {
                    if (!isMediumRange && spareToRender.lodFarMesh != null) {
                        spareToRender = spareToRender.lodFarMesh;
                    } else if (spareToRender.lodMesh != null) {
                        spareToRender = spareToRender.lodMesh;
                    }
                }
                ResourceLocation spareTex = (assets != null && assets.wheelTexture != null)
                        ? assets.wheelTexture
                        : ((spareToRender.materialName != null && assets != null && assets.materialTextures.containsKey(spareToRender.materialName))
                        ? assets.materialTextures.get(spareToRender.materialName)
                        : ClientVehiclePackLoader.FALLBACK_TIRE_TEXTURE);
                VertexConsumer spareVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(spareTex));
                spareToRender.render(poseStack, spareVc, safePackedLight, 255, 255, 255, 255);
            }
        }

        // Animated Steering Wheel (rendered only if not already animated as a chassis group)
        boolean hasAnimatedSteerInChassis = false;
        if (def.animations != null) {
            for (VehicleDefinition.AnimationDef a : def.animations) {
                if ("steer".equalsIgnoreCase(a.variable) || "steering".equalsIgnoreCase(a.variable)) {
                    hasAnimatedSteerInChassis = true;
                    break;
                }
            }
        }

        DynamicObjMesh swMesh = (assets != null) ? assets.steeringWheelMesh : null;
        if (!isShadowPass && isVeryClose && !hasAnimatedSteerInChassis && def.steeringWheel.enabled && swMesh != null && !swMesh.vertices.isEmpty()) {
            float swX = def.steeringWheel.pos[0];
            float swY = def.steeringWheel.pos[1];
            float swZ = def.steeringWheel.pos[2];
            float swTilt = def.steeringWheel.tiltAngleDeg;
            float swMult = def.steeringWheel.rotationMultiplier;

            ResourceLocation swTex = tex;
            if (swMesh.materialName != null && assets != null && assets.materialTextures.containsKey(swMesh.materialName)) {
                swTex = assets.materialTextures.get(swMesh.materialName);
            }
            VertexConsumer swVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(swTex));

            poseStack.pushPose();
            poseStack.translate(swX, swY, swZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(swTilt));
            poseStack.mulPose(Axis.ZP.rotationDegrees(steerAngleDeg * swMult));
            swMesh.render(poseStack, swVc, safePackedLight, 255, 255, 255, 255);
            poseStack.popPose();
        }

        // Modular Cabin Seats (rendered if custom seat model is present and seat is installed)
        if (!isShadowPass && isClose && def.seats != null) {
            for (int sIdx = 0; sIdx < def.seats.size(); sIdx++) {
                if (entity.isSeatInstalled(sIdx)) {
                    String seatId = entity.getInstalledSeatId(sIdx);
                    com.evgen.cardecor.pack.SeatDefinition sDef = com.evgen.cardecor.pack.SeatRegistry.get(seatId);
                    if (sDef != null && sDef.model != null && !sDef.model.isEmpty()) {
                        DynamicObjMesh seatMesh = ClientVehiclePackLoader.getSeatMesh(sDef);
                        if (seatMesh != null && !seatMesh.vertices.isEmpty()) {
                            VehicleDefinition.SeatDef seatSlot = def.seats.get(sIdx);
                            float sx = seatSlot.pos[0];
                            float sy = seatSlot.pos[1];
                            float sz = seatSlot.pos[2];
                            float sYaw = seatSlot.yawOffsetDeg;

                            ResourceLocation seatTex = tex;
                            if (seatMesh.materialName != null && assets != null && assets.materialTextures.containsKey(seatMesh.materialName)) {
                                seatTex = assets.materialTextures.get(seatMesh.materialName);
                            }
                            VertexConsumer seatVc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(seatTex));

                            poseStack.pushPose();
                            poseStack.translate(sx, sy, sz);
                            if (Math.abs(sYaw) > 0.01F) {
                                poseStack.mulPose(Axis.YP.rotationDegrees(sYaw));
                            }
                            seatMesh.render(poseStack, seatVc, safePackedLight, 255, 255, 255, 255);
                            poseStack.popPose();
                        }
                    }
                }
            }
        }

        // Headlight Lens Glow Flares
        if (!isShadowPass && entity.isHeadlightsOn() && def.headlights != null) {
            renderHeadlightFlares(entity, def, partialTicks, poseStack, buffer, camPos, safePackedLight);
        }

        // Taillight & Brake Light Lens Glow Flares
        if (!isShadowPass && (entity.isBraking() || entity.isHeadlightsOn())) {
            renderTaillightFlares(entity, def, partialTicks, poseStack, buffer, camPos);
        }

        // Contamination Splatters (Organic blood droplets on bumper/hood/windshield & mud sprays along rocker panels/fenders/tailgate)
        if (!isShadowPass && isClose) {
            renderContaminationSplatters(entity, def, poseStack, buffer, safePackedLight);
        }

        poseStack.popPose(); // End Chassis Pose

        poseStack.popPose(); // End Scaled Root Pose

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderAnimatedChassis(DrivablePackVehicleEntity entity, VehicleDefinition def,
                                       ClientVehicleAssets assets,
                                       float partialTicks, float steerAngleDeg,
                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                       ResourceLocation defaultTex, boolean isClose) {
        if (assets.batchedStaticChassis != null && !assets.batchedStaticChassis.isEmpty()) {
            // Fast path 1: Draw batched static chassis groups (exterior, 1-2 draw calls total!)
            for (ClientVehicleAssets.BatchedStaticMesh batch : assets.batchedStaticChassis) {
                if (batch.mesh == null || (batch.mesh.packedData == null && batch.mesh.vertices.isEmpty())) continue;
                VertexConsumer vc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(batch.texture));
                batch.mesh.render(poseStack, vc, packedLight, 255, 255, 255, 255);
            }

            // Fast path 1b: Draw batched interior chassis groups ONLY when close to player (< 24 blocks)
            if (isClose && assets.batchedInteriorChassis != null && !assets.batchedInteriorChassis.isEmpty()) {
                for (ClientVehicleAssets.BatchedStaticMesh batch : assets.batchedInteriorChassis) {
                    if (batch.mesh == null || (batch.mesh.packedData == null && batch.mesh.vertices.isEmpty())) continue;
                    VertexConsumer vc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(batch.texture));
                    batch.mesh.render(poseStack, vc, packedLight, 255, 255, 255, 255);
                }
            }

            // Fast path 2: Draw animated chassis parts (e.g. steering wheel, doors)
            for (ClientVehicleAssets.AnimatedChassisPart part : assets.animatedChassisParts) {
                DynamicObjMesh mesh = part.mesh;
                if (mesh == null || (mesh.packedData == null && mesh.vertices.isEmpty())) continue;
                VehicleDefinition.AnimationDef anim = part.anim;
                VertexConsumer vc = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(part.texture));

                if (anim != null && isClose) {
                    float varVal = getAnimationVariableValue(entity, anim.variable, partialTicks, steerAngleDeg);
                    float finalVal = Mth.clamp(varVal * anim.factor + anim.offset, anim.min, anim.max);

                    poseStack.pushPose();
                    float cx = (anim.center != null && anim.center.length >= 3) ? anim.center[0] : 0.0F;
                    float cy = (anim.center != null && anim.center.length >= 3) ? anim.center[1] : 0.0F;
                    float cz = (anim.center != null && anim.center.length >= 3) ? anim.center[2] : 0.0F;

                    poseStack.translate(cx, cy, cz);
                    if ("translation".equalsIgnoreCase(anim.type)) {
                        float ax = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[0] : 0.0F;
                        float ay = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[1] : 1.0F;
                        float az = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[2] : 0.0F;
                        poseStack.translate(ax * finalVal, ay * finalVal, az * finalVal);
                    } else {
                        float ax = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[0] : 0.0F;
                        float ay = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[1] : 1.0F;
                        float az = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[2] : 0.0F;
                        float rad = (float) Math.toRadians(finalVal);
                        TEMP_QUAT.rotationAxis(rad, ax, ay, az);
                        poseStack.mulPose(TEMP_QUAT);
                    }
                    poseStack.translate(-cx, -cy, -cz);

                    mesh.render(poseStack, vc, packedLight, 255, 255, 255, 255);
                    poseStack.popPose();
                } else {
                    mesh.render(poseStack, vc, packedLight, 255, 255, 255, 255);
                }
            }
            return;
        }

        // Fallback for non-batched groups
        if (assets.chassisGroups != null) {
            for (java.util.Map.Entry<String, DynamicObjMesh> entry : assets.chassisGroups.entrySet()) {
                DynamicObjMesh mesh = entry.getValue();
                if (mesh == null || (mesh.packedData == null && mesh.vertices.isEmpty())) continue;

                VehicleDefinition.AnimationDef anim = null;
                if (def.animations != null) {
                    for (VehicleDefinition.AnimationDef a : def.animations) {
                        if (mesh.matches(a.part)) {
                            anim = a;
                            break;
                        }
                    }
                }

                ResourceLocation partTex = defaultTex;
                if (mesh.materialName != null && assets.materialTextures.containsKey(mesh.materialName)) {
                    partTex = assets.materialTextures.get(mesh.materialName);
                }
                VertexConsumer vertexConsumer = buffer.getBuffer(VehicleRenderTypes.getVehicleTriangles(partTex));

                if (anim != null) {
                    float varVal = getAnimationVariableValue(entity, anim.variable, partialTicks, steerAngleDeg);
                    float finalVal = Mth.clamp(varVal * anim.factor + anim.offset, anim.min, anim.max);

                    poseStack.pushPose();
                    float cx = (anim.center != null && anim.center.length >= 3) ? anim.center[0] : 0.0F;
                    float cy = (anim.center != null && anim.center.length >= 3) ? anim.center[1] : 0.0F;
                    float cz = (anim.center != null && anim.center.length >= 3) ? anim.center[2] : 0.0F;

                    poseStack.translate(cx, cy, cz);
                    if ("translation".equalsIgnoreCase(anim.type)) {
                        float ax = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[0] : 0.0F;
                        float ay = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[1] : 1.0F;
                        float az = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[2] : 0.0F;
                        poseStack.translate(ax * finalVal, ay * finalVal, az * finalVal);
                    } else {
                        float ax = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[0] : 0.0F;
                        float ay = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[1] : 1.0F;
                        float az = (anim.axis != null && anim.axis.length >= 3) ? anim.axis[2] : 0.0F;
                        float rad = (float) Math.toRadians(finalVal);
                        TEMP_QUAT.rotationAxis(rad, ax, ay, az);
                        poseStack.mulPose(TEMP_QUAT);
                    }
                    poseStack.translate(-cx, -cy, -cz);

                    mesh.render(poseStack, vertexConsumer, packedLight, 255, 255, 255, 255);
                    poseStack.popPose();
                } else {
                    mesh.render(poseStack, vertexConsumer, packedLight, 255, 255, 255, 255);
                }
            }
        }
    }

    private float getAnimationVariableValue(DrivablePackVehicleEntity entity, String variable, float partialTicks, float steerAngleDeg) {
        if (variable == null) return 0.0F;
        return switch (variable.toLowerCase()) {
            case "door_left", "door_fl" -> entity.getDoorLeftAngle(partialTicks);
            case "door_right", "door_fr" -> entity.getDoorRightAngle(partialTicks);
            case "trunk", "door_back" -> entity.getTrunkAngle(partialTicks);
            case "steer", "steering" -> steerAngleDeg;
            case "speed" -> Math.abs(entity.getEntityData().get(DrivableVehicleEntity.SPEED)) * 3.6F;
            case "rpm" -> (float) entity.getEngineRPM();
            case "gear" -> (float) entity.getCurrentGear();
            case "handbrake", "e_break" -> (entity.isHandbrakeActive() ? 1.0F : 0.0F);
            case "headlights" -> (entity.isHeadlightsOn() ? 1.0F : 0.0F);
            default -> 0.0F;
        };
    }

    private void renderHeadlightFlares(DrivablePackVehicleEntity entity, VehicleDefinition def,
                                       float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                                       Vec3 camPos, int packedLight) {
        List<VehicleDefinition.LightSourceDef> resolvedLights = def.headlights.getResolvedLights(def);
        if (resolvedLights == null || resolvedLights.isEmpty()) return;

        // Determine view angle relative to vehicle forward direction
        float smoothYaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        double yawRad = Math.toRadians(smoothYaw);
        Vec3 vehicleFwd = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 toCam = camPos.subtract(entity.position()).normalize();
        double frontFacing = toCam.dot(vehicleFwd);

        // If viewed mostly from behind, fade out to prevent bleeding through body
        if (frontFacing <= -0.15) return;
        float viewFade = (float) Mth.clamp((frontFacing + 0.15) / 0.65, 0.0, 1.0);

        // Ambient daylight & block light contrast attenuation
        float sunAngle = (entity.level() != null) ? entity.level().getTimeOfDay(partialTicks) : 0.0f;
        float dayCos = (float) Math.cos(sunAngle * Math.PI * 2.0);
        float dayFactor = Mth.clamp(dayCos * 2.0f + 0.5f, 0.0f, 1.0f);
        int bLight = packedLight & 0xFFFF;
        int sLight = (packedLight >> 16) & 0xFFFF;
        float ambientBrightness = Math.min(1.0f, (dayFactor * (sLight / 15.0f)) + (bLight / 15.0f) * 0.70f);
        float flareContrastFactor = Mth.lerp(ambientBrightness, 1.0f, 0.25f);

        float[] defaultCol = VehicleDefinition.HeadlightConfig.parseColor(def.headlights.color, new float[]{1.0f, 0.96f, 0.82f});
        VertexConsumer vc = buffer.getBuffer(VehicleRenderTypes.getHeadlightFlare(HEADLIGHT_FLARE_TEXTURE));
        Matrix4f m = poseStack.last().pose();

        for (VehicleDefinition.LightSourceDef lDef : resolvedLights) {
            float lx = (float) ((lDef.pos != null && lDef.pos.length > 0) ? lDef.pos[0] : 0.0);
            float ly = (float) ((lDef.pos != null && lDef.pos.length > 1) ? lDef.pos[1] : 0.75);
            float lz = (float) ((lDef.pos != null && lDef.pos.length > 2) ? lDef.pos[2] : -2.2) - 0.015f;

            float r = (float) (lDef.flareSize > 0.02 ? lDef.flareSize : 0.18);
            float[] col = (lDef.color != null && !lDef.color.isEmpty())
                ? VehicleDefinition.HeadlightConfig.parseColor(lDef.color, defaultCol)
                : defaultCol;

            int red = (int) (col[0] * 255.0f);
            int green = (int) (col[1] * 255.0f);
            int blue = (int) (col[2] * 255.0f);

            int coreAlpha = (int) (240 * viewFade * flareContrastFactor);
            int haloAlpha = (int) (110 * viewFade * flareContrastFactor);

            // Core intense lens glow
            drawFlareQuad(vc, m, lx - r, ly - r, lx + r, ly + r, lz, red, green, blue, coreAlpha);

            // Soft outer halo
            float hr = r * 1.65f;
            drawFlareQuad(vc, m, lx - hr, ly - hr, lx + hr, ly + hr, lz - 0.005f, red, green, blue, haloAlpha);
        }
    }

    private static void drawFlareQuad(VertexConsumer vc, Matrix4f m, float minX, float minY, float maxX, float maxY, float z, int r, int g, int b, int a) {
        vc.vertex(m, minX, minY, z).color(r, g, b, a).uv(0.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, -1.0f).endVertex();
        vc.vertex(m, maxX, minY, z).color(r, g, b, a).uv(1.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, -1.0f).endVertex();
        vc.vertex(m, maxX, maxY, z).color(r, g, b, a).uv(1.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, -1.0f).endVertex();
        vc.vertex(m, minX, maxY, z).color(r, g, b, a).uv(0.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, -1.0f).endVertex();
    }

    private void renderTaillightFlares(DrivablePackVehicleEntity entity, VehicleDefinition def,
                                       float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                                       Vec3 camPos) {
        // Determine view angle relative to vehicle rear direction
        float smoothYaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        double yawRad = Math.toRadians(smoothYaw);
        Vec3 vehicleRear = new Vec3(Math.sin(yawRad), 0.0, -Math.cos(yawRad));
        Vec3 toCam = camPos.subtract(entity.position()).normalize();
        double rearFacing = toCam.dot(vehicleRear);

        // If viewed from the front, fade out to prevent bleeding through vehicle body
        if (rearFacing <= -0.35) return;
        float viewFade = (float) Mth.clamp((rearFacing + 0.35) / 0.70, 0.0, 1.0);

        boolean isBraking = entity.isBraking();
        double rawIdle = (def.taillights != null) ? def.taillights.idleBrightness : 0.25;
        float idleRatio = (float) (rawIdle > 1.0 ? rawIdle / 100.0 : rawIdle);
        float brakeRatio = (float) ((def.taillights != null && def.taillights.brakeBrightness > 0.05) ? def.taillights.brakeBrightness : 1.0f);
        // Clear, vibrant background glow for running taillights (idle) and full power on brake
        float intensityMult = isBraking ? brakeRatio : (entity.isHeadlightsOn() ? Math.max(0.75f, idleRatio) : 0.0f);
        if (intensityMult <= 0.001f) return;

        double[] leftPos = def.getTaillightLeftPos();
        double[] rightPos = def.getTaillightRightPos();
        double[][] taillightPositions = new double[][]{leftPos, rightPos};

        float r = (float) (def.taillights != null && def.taillights.flareSize > 0.02 ? def.taillights.flareSize : 0.14);
        int red = 255;
        int green = isBraking ? 25 : 15;
        int blue = isBraking ? 25 : 15;

        int coreAlpha = (int) (250 * viewFade * intensityMult);
        int haloAlpha = (int) (145 * viewFade * intensityMult);

        VertexConsumer vc = buffer.getBuffer(VehicleRenderTypes.getHeadlightFlare(HEADLIGHT_FLARE_TEXTURE));
        Matrix4f m = poseStack.last().pose();

        for (double[] pos : taillightPositions) {
            float lx = (float) pos[0];
            float ly = (float) pos[1];
            float lz = (float) pos[2] + 0.035f; // offset slightly backwards (+Z in model space) to prevent body clipping

            // Core intense lens glow
            drawTaillightQuad(vc, m, lx - r, ly - r, lx + r, ly + r, lz, red, green, blue, coreAlpha);

            // Soft outer halo
            float hr = r * (isBraking ? 1.75f : 1.30f);
            drawTaillightQuad(vc, m, lx - hr, ly - hr, lx + hr, ly + hr, lz + 0.008f, red, green, blue, haloAlpha);
        }
    }

    private static void drawTaillightQuad(VertexConsumer vc, Matrix4f m, float minX, float minY, float maxX, float maxY, float z, int r, int g, int b, int a) {
        vc.vertex(m, maxX, minY, z).color(r, g, b, a).uv(0.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, 1.0f).endVertex();
        vc.vertex(m, minX, minY, z).color(r, g, b, a).uv(1.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, 1.0f).endVertex();
        vc.vertex(m, minX, maxY, z).color(r, g, b, a).uv(1.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, 1.0f).endVertex();
        vc.vertex(m, maxX, maxY, z).color(r, g, b, a).uv(0.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0f, 0.0f, 1.0f).endVertex();
    }

    private void renderContaminationSplatters(DrivablePackVehicleEntity entity, VehicleDefinition def,
                                              PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float bloodLevel = entity.getBloodLevel();
        float dirtLevel = entity.getDirtLevel();
        if (bloodLevel <= 0.001F && dirtLevel <= 0.001F) return;

        float scale = (def.scale > 0.05F) ? def.scale : 1.0F;
        VehicleDefinition.CalibratedBounds cb = def.calibratedBounds;

        float frontZ = -(cb != null ? (float)(cb.frontDist / scale) : 2.85f);
        float rearZ  =  (cb != null ? (float)(cb.rearDist / scale)  : 2.85f);
        float halfW  =  (cb != null ? (float)(cb.halfWidth / scale) : 1.15f);
        float hoodY  =  (cb != null ? (float)(cb.hoodHeight / scale) : 1.05f);
        float roofY  =  (cb != null ? (float)(cb.roofHeight / scale) : 1.85f);
        float bumperY = (cb != null ? (float)(cb.bumperStepHeight / scale) : 0.55f);
        float cabinFrontZ = (cb != null) ? (float)(cb.cabinFrontZ / scale) : -1.05f;
        if (cabinFrontZ > 0.0f) cabinFrontZ = -cabinFrontZ; // Model space: negative Z is front
        float cabinZ = cabinFrontZ;
        float trunkY = (cb != null ? (float)(cb.trunkHeight / scale) : hoodY);
        if (trunkY < bumperY + 0.30f) trunkY = bumperY + 0.60f;

        float zFL = (def.wheels != null && def.wheels.frontLeft != null) ? def.wheels.frontLeft[2] : frontZ * 0.68f;
        float zRL = (def.wheels != null && def.wheels.rearLeft != null) ? def.wheels.rearLeft[2] : rearZ * 0.60f;
        float wheelX = (def.wheels != null && def.wheels.frontLeft != null) ? Math.abs(def.wheels.frontLeft[0]) : 1.05f;
        halfW = Math.max(halfW, wheelX + 0.04f);

        float zFrontTip = Math.min(frontZ, zFL - 0.70f);
        float zRearTip  = Math.max(rearZ,  zRL + 0.70f);

        Matrix4f m = poseStack.last().pose();

        // 1. Organic Blood Splatters (Impacts on Bumper, Hood Pools/Streaks, Windshield Splatters)
        if (bloodLevel > 0.01F) {
            VertexConsumer bloodVc = buffer.getBuffer(VehicleRenderTypes.getDecal(BLOOD_SPLATTER_TEXTURE));
            float bloodProgress = (bloodLevel / 100.0F) * BLOOD_TEMPLATES.length;
            int bloodCount = Math.min(BLOOD_TEMPLATES.length, (int) Math.ceil(bloodProgress));

            // Windshield geometry vectors
            float wsZ0 = (cb != null && Math.abs(cb.windshieldBottomZ) > 0.05) ? (float)(cb.windshieldBottomZ / scale) : cabinZ;
            float wsY0 = (cb != null && Math.abs(cb.windshieldBottomY) > 0.05) ? (float)(cb.windshieldBottomY / scale) : (hoodY + 0.08f);
            float wsZ1 = (cb != null && Math.abs(cb.windshieldTopZ) > 0.05) ? (float)(cb.windshieldTopZ / scale) : (cabinZ + 0.65f);
            float wsY1 = (cb != null && Math.abs(cb.windshieldTopY) > 0.05) ? (float)(cb.windshieldTopY / scale) : (roofY - 0.15f);
            float dz = wsZ1 - wsZ0;
            float dy = wsY1 - wsY0;
            float len = (float) Math.sqrt(dy * dy + dz * dz);
            if (len < 0.01f) len = 1.0f;
            float wsVx = 0.0f, wsVy = dy / len, wsVz = dz / len;
            float wsNx = 0.0f, wsNy = dz / len, wsNz = -dy / len;

            for (int i = 0; i < bloodCount; i++) {
                BloodDecalTemplate t = BLOOD_TEMPLATES[i];

                int alpha = 245;
                if (i == bloodCount - 1) {
                    float remainder = bloodProgress - (bloodCount - 1);
                    alpha = Math.max(15, Math.min(245, (int)(remainder * 245)));
                }

                float cx, cy, cz;
                float bux, buy, buz;
                float bvx, bvy, bvz;
                float bnx, bny, bnz;

                switch (t.zone) {
                    case 0 -> {
                        // Front Bumper / Grille: facing -Z (forward)
                        cx = t.relX * halfW * 0.82f;
                        cy = bumperY + t.relParam * (hoodY - bumperY) * 0.75f;
                        cz = zFrontTip - 0.015f;
                        bnx = 0.0f; bny = 0.0f; bnz = -1.0f;
                        bux = -1.0f; buy = 0.0f; buz = 0.0f;
                        bvx = 0.0f; bvy = 1.0f; bvz = 0.0f;
                    }
                    case 1 -> {
                        // Hood: facing +Y (upward), strictly bounded between bumper/grille tip and windshield base
                        cx = t.relX * halfW * 0.75f;
                        cy = hoodY + 0.015f;
                        float hStart = zFrontTip + 0.15f;
                        float hEnd = cabinFrontZ - 0.10f;
                        cz = hStart + t.relParam * (hEnd - hStart);
                        bnx = 0.0f; bny = 1.0f; bnz = 0.0f;
                        bux = -1.0f; buy = 0.0f; buz = 0.0f;
                        bvx = 0.0f; bvy = 0.0f; bvz = 1.0f;
                    }
                    default -> {
                        // Windshield: slanted up-forward, strictly on the windshield glass plane
                        cx = t.relX * halfW * 0.68f;
                        float outOffset = 0.035f;
                        cy = wsY0 + t.relParam * (wsY1 - wsY0) + wsNy * outOffset;
                        cz = wsZ0 + t.relParam * (wsZ1 - wsZ0) + wsNz * outOffset;
                        bnx = wsNx; bny = wsNy; bnz = wsNz;
                        bux = -1.0f; buy = 0.0f; buz = 0.0f;
                        bvx = wsVx; bvy = wsVy; bvz = wsVz;
                    }
                }

                // Systemic Cabin Interior Exclusion: reject exterior decals clipping inside cabin
                if (t.zone != 2 && isInsideCabin(cx, cy, cz, def)) {
                    continue;
                }

                float rad = (float) Math.toRadians(t.rotDeg);
                float cos = Mth.cos(rad);
                float sin = Mth.sin(rad);
                float rux = bux * cos + bvx * sin;
                float ruy = buy * cos + bvy * sin;
                float ruz = buz * cos + bvz * sin;
                float rvx = -bux * sin + bvx * cos;
                float rvy = -buy * sin + bvy * cos;
                float rvz = -buz * sin + bvz * cos;

                drawDecalQuad(bloodVc, m, cx, cy, cz, rux, ruy, ruz, rvx, rvy, rvz, bnx, bny, bnz, t.size * 0.5f, t.quadrant, packedLight, alpha);
            }
        }

        // 2. Dirt & Mud Sprays (Wheel Arches, Rocker Panels/Lower Doors, Quarter Panels, Tailgate)
        if (dirtLevel > 0.01F) {
            VertexConsumer dirtVc = buffer.getBuffer(VehicleRenderTypes.getDecal(DIRT_SPLATTER_TEXTURE));
            float dirtProgress = (dirtLevel / 100.0F) * DIRT_TEMPLATES.length;
            int dirtCount = Math.min(DIRT_TEMPLATES.length, (int) Math.ceil(dirtProgress));

            for (int i = 0; i < dirtCount; i++) {
                DirtDecalTemplate t = DIRT_TEMPLATES[i];
                int alpha = 245;
                if (i == dirtCount - 1) {
                    float remainder = dirtProgress - (dirtCount - 1);
                    alpha = Math.max(15, Math.min(245, (int)(remainder * 245)));
                }

                float cx, cy, cz;
                float bux, buy, buz;
                float bvx, bvy, bvz;
                float bnx, bny, bnz;

                switch (t.zone) {
                    case 0 -> {
                        // Front Wheel Arch / Fender
                        float s = (float) t.side;
                        cx = (halfW + 0.015f) * s;
                        cy = t.relParam2;
                        cz = zFL + t.relParam1;
                        bnx = s; bny = 0.0f; bnz = 0.0f;
                        bux = 0.0f; buy = 0.0f; buz = -s;
                        bvx = 0.0f; bvy = 1.0f; bvz = 0.0f;
                    }
                    case 1 -> {
                        // Rocker Panel / Lower Door
                        float s = (float) t.side;
                        cx = (halfW + 0.015f) * s;
                        cy = t.relParam2;
                        cz = zFL + t.relParam1 * (zRL - zFL);
                        bnx = s; bny = 0.0f; bnz = 0.0f;
                        bux = 0.0f; buy = 0.0f; buz = -s;
                        bvx = 0.0f; bvy = 1.0f; bvz = 0.0f;
                    }
                    case 2 -> {
                        // Rear Wheel Arch / Quarter Panel
                        float s = (float) t.side;
                        cx = (halfW + 0.015f) * s;
                        cy = t.relParam2;
                        cz = zRL + t.relParam1;
                        bnx = s; bny = 0.0f; bnz = 0.0f;
                        bux = 0.0f; buy = 0.0f; buz = -s;
                        bvx = 0.0f; bvy = 1.0f; bvz = 0.0f;
                    }
                    default -> {
                        // Tailgate / Rear Bumper: facing +Z (rearward)
                        cx = t.relParam1 * halfW * 0.82f;
                        cy = bumperY + t.relParam2 * (trunkY - bumperY) * 0.85f;
                        cz = zRearTip + 0.015f;
                        bnx = 0.0f; bny = 0.0f; bnz = 1.0f;
                        bux = 1.0f; buy = 0.0f; buz = 0.0f;
                        bvx = 0.0f; bvy = 1.0f; bvz = 0.0f;
                    }
                }

                float rad = (float) Math.toRadians(t.rotDeg);
                float cos = Mth.cos(rad);
                float sin = Mth.sin(rad);
                float rux = bux * cos + bvx * sin;
                float ruy = buy * cos + bvy * sin;
                float ruz = buz * cos + bvz * sin;
                float rvx = -bux * sin + bvx * cos;
                float rvy = -buy * sin + bvy * cos;
                float rvz = -buz * sin + bvz * cos;

                drawDecalQuad(dirtVc, m, cx, cy, cz, rux, ruy, ruz, rvx, rvy, rvz, bnx, bny, bnz, t.size * 0.5f, t.quadrant, packedLight, alpha);
            }
        }
    }

    private static boolean isInsideCabin(float cx, float cy, float cz, VehicleDefinition def) {
        if (def == null) return false;
        VehicleDefinition.CalibratedBounds cb = def.calibratedBounds;
        float scale = (def.scale > 0.05F) ? def.scale : 1.0F;

        // 1. Geometric 3D Cabin Bounding Box Exclusion
        if (cb != null) {
            float cFront = (float)(cb.cabinFrontZ / scale);
            float cRear = (float)(cb.cabinRearZ / scale);
            float minZ = Math.min(cFront, cRear);
            float maxZ = Math.max(cFront, cRear);
            float halfW = (float)(cb.halfWidth / scale) * 0.95f;
            float floorY = (float)((cb.bumperStepHeight > 0.1 ? cb.bumperStepHeight : 0.55f) / scale);
            float roofY = (float)(cb.roofHeight / scale) - 0.05f;

            if (Math.abs(cx) <= halfW && cy >= floorY && cy <= roofY && cz > (minZ + 0.05f) && cz < (maxZ - 0.05f)) {
                return true;
            }
        }

        // 2. Check steering wheel proximity
        if (def.steeringWheel != null && def.steeringWheel.enabled && def.steeringWheel.pos != null && def.steeringWheel.pos.length >= 3) {
            float sx = def.steeringWheel.pos[0];
            float sy = def.steeringWheel.pos[1];
            float sz = def.steeringWheel.pos[2];
            float dx = cx - sx;
            float dy = cy - sy;
            float dz = cz - sz;
            if ((dx * dx + dy * dy + dz * dz) < 0.28f) { // 0.53m exclusion radius
                return true;
            }
        }
        // Check driver / front passenger seats proximity
        if (def.seats != null) {
            for (VehicleDefinition.SeatDef seat : def.seats) {
                if (seat.isCargoBed) continue;
                if (seat.pos != null && seat.pos.length >= 3) {
                    float sx = seat.pos[0];
                    float sy = seat.pos[1] + 0.45f;
                    float sz = seat.pos[2];
                    float dx = cx - sx;
                    float dy = cy - sy;
                    float dz = cz - sz;
                    if ((dx * dx + dy * dy + dz * dz) < 0.35f) { // 0.59m exclusion radius
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void drawDecalQuad(VertexConsumer vc, Matrix4f m,
                                      float cx, float cy, float cz,
                                      float ux, float uy, float uz,
                                      float vx, float vy, float vz,
                                      float nx, float ny, float nz,
                                      float halfSize, int quadrant, int light, int alpha) {
        float u0 = (quadrant % 2) * 0.5f;
        float v0 = (quadrant / 2) * 0.5f;
        float u1 = u0 + 0.5f;
        float v1 = v0 + 0.5f;

        float x0 = cx - ux * halfSize - vx * halfSize;
        float y0 = cy - uy * halfSize - vy * halfSize;
        float z0 = cz - uz * halfSize - vz * halfSize;

        float x1 = cx + ux * halfSize - vx * halfSize;
        float y1 = cy + uy * halfSize - vy * halfSize;
        float z1 = cz + uz * halfSize - vz * halfSize;

        float x2 = cx + ux * halfSize + vx * halfSize;
        float y2 = cy + uy * halfSize + vy * halfSize;
        float z2 = cz + uz * halfSize + vz * halfSize;

        float x3 = cx - ux * halfSize + vx * halfSize;
        float y3 = cy - uy * halfSize + vy * halfSize;
        float z3 = cz - uz * halfSize + vz * halfSize;

        vc.vertex(m, x0, y0, z0).color(255, 255, 255, alpha).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(255, 255, 255, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(255, 255, 255, alpha).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(255, 255, 255, alpha).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
    }
}
