package com.evgen.cardecor.client;

import java.io.File;

public class VehicleAutoBaker {

    public static DynamicObjMesh getOrBakeMesh(File packDir, String relModelPath, boolean isWheel) {
        if (packDir == null || relModelPath == null || relModelPath.isEmpty()) return new DynamicObjMesh();

        File modelFile = new File(packDir, relModelPath);
        if (!modelFile.exists()) {
            File direct = new File(packDir, "models/" + new File(relModelPath).getName());
            if (direct.exists()) {
                modelFile = direct;
            } else {
                return new DynamicObjMesh();
            }
        }

        // Load 100% exact raw OBJ geometry without altering original 3D anchor points
        return DynamicObjMesh.load(modelFile);
    }

    public static DynamicObjMesh[] getOrBakeChassisAndSpare(File packDir, String relModelPath, boolean isPickup) {
        if (packDir == null || relModelPath == null || relModelPath.isEmpty()) {
            return new DynamicObjMesh[]{new DynamicObjMesh(), null};
        }

        File modelFile = new File(packDir, relModelPath);
        if (!modelFile.exists()) {
            File direct = new File(packDir, "models/" + new File(relModelPath).getName());
            if (direct.exists()) {
                modelFile = direct;
            } else {
                return new DynamicObjMesh[]{new DynamicObjMesh(), null};
            }
        }

        if (isPickup) {
            return DynamicObjMesh.loadAndSplit(modelFile, DynamicObjMesh::isPickupSpareWheelFace);
        } else {
            return new DynamicObjMesh[]{DynamicObjMesh.load(modelFile), null};
        }
    }
}
