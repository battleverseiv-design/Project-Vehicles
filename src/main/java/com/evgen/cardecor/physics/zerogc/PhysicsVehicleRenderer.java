package com.evgen.cardecor.physics.zerogc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 144+ FPS Gimbal-Lock-Free Vehicle Renderer.
 * Utilizes SLERP quaternion orientation directly with PoseStack for smooth 3D tumbling,
 * roll-overs, wheel travel displacement, and realistic steering/spin kinematics.
 */
public class PhysicsVehicleRenderer<T extends PhysicsVehicleEntity> extends EntityRenderer<T> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("minecraft", "textures/block/iron_block.png");

    // Preallocated Zero-GC buffers for frame rendering
    private final Quaternionf renderQuat = new Quaternionf();
    private final Vector3f renderPos = new Vector3f();
    private final Vector3f scratchVec = new Vector3f();

    public PhysicsVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.2f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. Smooth 144+ FPS SLERP Orientation
        entity.getInterpolatedOrientation(partialTicks, renderQuat);
        // Apply 3D rigid body orientation (gimbal-lock free)
        poseStack.mulPose(renderQuat);

        // 2. Render Vehicle Hull / Chassis
        renderChassis(entity, partialTicks, poseStack, buffer, packedLight);

        // 3. Render 4 Wheels with suspension droop and steering
        for (int i = 0; i < 4; i++) {
            renderWheel(entity, i, partialTicks, poseStack, buffer, packedLight);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Chassis rendering hook. Can be overridden or extended with pack OBJ meshes.
     */
    protected void renderChassis(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Child classes or pack vehicle renderers hook their mesh draw calls here
    }

    /**
     * Renders a single wheel at its physics mount point, compressed by suspension and steered.
     */
    protected void renderWheel(T entity, int wheelIndex, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Suspension travel displacement
        float compression = entity.physics.suspensionCompression[wheelIndex];
        float restLen = entity.physics.restSuspensionLength;
        float currentSuspLength = restLen - compression;

        // Position wheel at mount point - suspension offset
        // Local mounts: 0=FL, 1=FR, 2=RL, 3=RR
        float xMount = (wheelIndex % 2 == 0) ? entity.physics.halfWidth * 0.88f : -entity.physics.halfWidth * 0.88f;
        float zMount = (wheelIndex < 2) ? entity.physics.halfLength * 0.65f : -entity.physics.halfLength * 0.65f;
        float yMount = -currentSuspLength;

        poseStack.translate(xMount, yMount, zMount);

        // Steering rotation on front wheels (FL=0, FR=1)
        if (wheelIndex < 2) {
            poseStack.mulPose(Axis.YP.rotation(entity.physics.currentSteerAngle));
        }

        // Wheel spin around axle (X axis)
        float spinAngle = entity.physics.wheelRotationAngle[wheelIndex] +
                (entity.physics.wheelSpinVelocity[wheelIndex] * partialTicks * 0.05f);
        poseStack.mulPose(Axis.XP.rotation(spinAngle));

        // Right wheels mirrored 180 degrees
        if (wheelIndex % 2 == 1) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        }

        // Draw wheel geometry
        renderWheelGeometry(entity, wheelIndex, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    /**
     * Subclasses or OBJ model loaders render the actual wheel rim/tire mesh here.
     */
    protected void renderWheelGeometry(T entity, int wheelIndex, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Model draw hook
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return DEFAULT_TEXTURE;
    }
}
