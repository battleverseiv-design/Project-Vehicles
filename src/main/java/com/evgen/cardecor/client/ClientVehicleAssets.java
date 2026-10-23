package com.evgen.cardecor.client;

import com.evgen.cardecor.pack.VehicleDefinition;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientVehicleAssets {
    public DynamicObjMesh chassisMesh;
    public DynamicObjMesh wheelMesh;
    public DynamicObjMesh steeringWheelMesh;
    public DynamicObjMesh spareWheelMesh;
    public ResourceLocation clientTexture;
    public ResourceLocation wheelTexture;
    public Map<String, DynamicObjMesh> chassisGroups;
    public final Map<String, ResourceLocation> materialTextures = new HashMap<>();

    public static class BatchedStaticMesh {
        public final ResourceLocation texture;
        public final DynamicObjMesh mesh;
        public BatchedStaticMesh(ResourceLocation texture, DynamicObjMesh mesh) {
            this.texture = texture;
            this.mesh = mesh;
        }
    }

    public static class AnimatedChassisPart {
        public final DynamicObjMesh mesh;
        public final VehicleDefinition.AnimationDef anim;
        public final ResourceLocation texture;
        public AnimatedChassisPart(DynamicObjMesh mesh, VehicleDefinition.AnimationDef anim, ResourceLocation texture) {
            this.mesh = mesh;
            this.anim = anim;
            this.texture = texture;
        }
    }

    public final List<BatchedStaticMesh> batchedStaticChassis = new ArrayList<>();
    public final List<BatchedStaticMesh> batchedInteriorChassis = new ArrayList<>();
    public final List<AnimatedChassisPart> animatedChassisParts = new ArrayList<>();
}
