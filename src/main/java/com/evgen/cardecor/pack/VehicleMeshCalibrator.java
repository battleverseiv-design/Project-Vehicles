package com.evgen.cardecor.pack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class VehicleMeshCalibrator {

    /**
     * Calibrates exact 3D dimensions, bumper distances, deck heights and hitboxes
     * from the 3D OBJ model of the vehicle.
     */
    public static void calibrate(VehicleDefinition def) {
        if (def == null) return;
        if (def.calibratedBounds == null) {
            def.calibratedBounds = new VehicleDefinition.CalibratedBounds();
        }

        File objFile = null;
        if (def.packDirectory != null && def.models != null && def.models.chassis != null) {
            objFile = new File(def.packDirectory, def.models.chassis);
            if (!objFile.exists()) {
                objFile = new File(def.packDirectory, "models/" + new File(def.models.chassis).getName());
            }
        }

        if (objFile != null && objFile.exists()) {
            calibrateFromObj(def, objFile);
        } else {
            calibrateFallback(def);
        }
    }

    private static void calibrateFromObj(VehicleDefinition def, File objFile) {
        List<float[]> verts = new ArrayList<>(4000);
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        try (BufferedReader reader = new BufferedReader(new FileReader(objFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ") || line.startsWith("v\t")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        verts.add(new float[]{x, y, z});

                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z;
                        if (z > maxZ) maxZ = z;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Error calibrating mesh from " + objFile.getName() + ": " + e.getMessage());
            calibrateFallback(def);
            return;
        }

        if (verts.isEmpty()) {
            calibrateFallback(def);
            return;
        }

        float scale = (def.scale > 0.001F) ? def.scale : 1.0F;
        float gY = def.groundOffsetY;

        // Determine orientation from wheel config if available
        // If wheels.frontLeft[2] > wheels.rearLeft[2], +Z is front, -Z is rear
        boolean positiveZIsFront = true;
        if (def.wheels != null && def.wheels.frontLeft != null && def.wheels.rearLeft != null) {
            positiveZIsFront = (def.wheels.frontLeft[2] >= def.wheels.rearLeft[2]);
        }

        double frontBound = (positiveZIsFront ? maxZ : -minZ) * scale;
        double rearBound = (positiveZIsFront ? -minZ : maxZ) * scale;
        double rawHalfWidth = Math.max(Math.abs(minX), Math.abs(maxX)) * scale;
        double halfWidth = Math.max(1.10, Math.min(rawHalfWidth * 0.92, 1.45));

        // Slice mesh along Z into zones to detect hood, cabin, trunk heights
        double lengthSpan = (maxZ - minZ);
        double zStep = lengthSpan / 10.0;
        double[] sliceMaxY = new double[10];
        for (int i = 0; i < 10; i++) sliceMaxY[i] = minY;

        for (float[] v : verts) {
            int sliceIdx = (int) ((v[2] - minZ) / zStep);
            if (sliceIdx < 0) sliceIdx = 0;
            if (sliceIdx >= 10) sliceIdx = 9;
            if (v[1] > sliceMaxY[sliceIdx]) {
                sliceMaxY[sliceIdx] = v[1];
            }
        }

        // Identify peak roof height
        double maxRoofY = minY;
        for (int i = 0; i < 10; i++) {
            if (sliceMaxY[i] > maxRoofY) {
                maxRoofY = sliceMaxY[i];
            }
        }

        double hoodDeckY;
        double trunkDeckY;
        double roofDeckY = maxRoofY * scale + gY;

        double cabinFrontZ;
        double cabinRearZ;

        if (positiveZIsFront) {
            // Slices 8-9 are Front/Hood, Slices 0-1 are Rear/Trunk (avoids windshield A/C-pillar bleed)
            hoodDeckY = Math.max(sliceMaxY[8], sliceMaxY[9]) * scale + gY;
            trunkDeckY = Math.max(sliceMaxY[0], sliceMaxY[1]) * scale + gY;
            cabinFrontZ = (minZ + 7.0 * zStep) * scale;
            cabinRearZ = (minZ + 3.0 * zStep) * scale;
        } else {
            // Slices 0-1 are Front/Hood, Slices 8-9 are Rear/Trunk (avoids windshield A/C-pillar bleed)
            hoodDeckY = Math.max(sliceMaxY[0], sliceMaxY[1]) * scale + gY;
            trunkDeckY = Math.max(sliceMaxY[8], sliceMaxY[9]) * scale + gY;
            cabinFrontZ = Math.abs(minZ + 3.0 * zStep) * scale;
            cabinRearZ = -Math.abs(minZ + 7.0 * zStep) * scale;
        }

        // Clamp heights to realistic vehicle proportions
        roofDeckY = Math.max(1.75, roofDeckY);
        hoodDeckY = Math.max(1.15, Math.min(roofDeckY - 0.20, hoodDeckY));
        trunkDeckY = Math.max(1.05, Math.min(roofDeckY - 0.20, trunkDeckY));

        VehicleDefinition.CalibratedBounds cb = def.calibratedBounds;
        cb.frontDist = Math.max(1.8, frontBound);
        cb.rearDist = Math.max(1.8, rearBound);
        cb.halfWidth = halfWidth;
        cb.hoodHeight = hoodDeckY;
        cb.roofHeight = roofDeckY;
        cb.trunkHeight = trunkDeckY;
        cb.bumperStepHeight = 0.55 + gY;
        cb.cabinFrontZ = cabinFrontZ;
        cb.cabinRearZ = cabinRearZ;
        cb.totalHeight = (maxY - minY) * scale;

        // Auto-update physics overhangs and track width
        if (def.physics != null) {
            def.physics.frontOverhang = Math.max(0.65, cb.frontDist - (def.physics.wheelbase * 0.5));
            def.physics.rearOverhang = Math.max(0.65, cb.rearDist - (def.physics.wheelbase * 0.5));
            def.physics.trackWidth = cb.halfWidth * 2.0;
        }

        boolean isPickup = def.hasCargoBed();

        cb.hasCargoBed = isPickup;
        cb.bedFloorHeight = isPickup ? (0.68 * scale + gY) : trunkDeckY;

        // Auto-update hitboxes
        if (def.hitboxes != null) {
            float w = (float) (cb.halfWidth * 2.0);
            def.hitboxes.chassisSize = new float[]{w, 0.65F, (float)(cb.frontDist + cb.rearDist)};
            def.hitboxes.hoodSize = new float[]{w, (float) cb.hoodHeight, (float)(cb.frontDist - cb.cabinFrontZ)};
            def.hitboxes.cabinSize = new float[]{w, (float) cb.roofHeight, (float)(cb.cabinFrontZ - cb.cabinRearZ)};
            def.hitboxes.trunkSize = new float[]{w, (float) cb.trunkHeight, (float)(Math.abs(cb.cabinRearZ) + cb.rearDist)};
        }

        System.out.println("[CarDecor] Auto-Calibrated '" + def.id + "' from 3D model (" + verts.size() + " verts): " +
            "Front=" + String.format("%.2f", cb.frontDist) + "m, Rear=" + String.format("%.2f", cb.rearDist) + "m, " +
            "Width=" + String.format("%.2f", cb.halfWidth * 2.0) + "m, Roof=" + String.format("%.2f", cb.roofHeight) + "m, " +
            "Hood=" + String.format("%.2f", cb.hoodHeight) + "m, Trunk=" + String.format("%.2f", cb.trunkHeight) + "m, " +
            "BedFloor=" + (cb.hasCargoBed ? String.format("%.2f", cb.bedFloorHeight) + "m" : "N/A"));
    }

    private static void calibrateFallback(VehicleDefinition def) {
        boolean isPickup = def.hasCargoBed();
        boolean isSedan = !isPickup && ("blue_sedan".equalsIgnoreCase(def.id) || "sedan".equalsIgnoreCase(def.category) || def.category == null);
        double gY = def.groundOffsetY;

        VehicleDefinition.CalibratedBounds cb = def.calibratedBounds;
        if (isSedan) {
            cb.frontDist = 3.25;
            cb.rearDist = 3.25;
            cb.halfWidth = 0.98;
            cb.hoodHeight = 1.48 + gY;
            cb.roofHeight = 2.45 + gY;
            cb.trunkHeight = 1.35 + gY;
            cb.bumperStepHeight = 0.55 + gY;
            cb.cabinFrontZ = 0.85;
            cb.cabinRearZ = -0.75;
            cb.totalHeight = 2.45;
            cb.hasCargoBed = false;
            cb.bedFloorHeight = 1.35 + gY;
        } else if (isPickup) {
            cb.frontDist = 2.90;
            cb.rearDist = 2.90;
            cb.halfWidth = 1.00;
            cb.hoodHeight = 1.46 + gY;
            cb.roofHeight = 2.52 + gY;
            cb.trunkHeight = 1.38 + gY;
            cb.bumperStepHeight = 0.55 + gY;
            cb.cabinFrontZ = 0.70;
            cb.cabinRearZ = -0.65;
            cb.totalHeight = 2.52;
            cb.hasCargoBed = true;
            cb.bedFloorHeight = 0.68 + gY;
        } else {
            cb.frontDist = 2.80;
            cb.rearDist = 2.80;
            cb.halfWidth = 0.95;
            cb.hoodHeight = 1.10 + gY;
            cb.roofHeight = 1.85 + gY;
            cb.trunkHeight = 1.05 + gY;
            cb.bumperStepHeight = 0.45 + gY;
            cb.cabinFrontZ = 0.60;
            cb.cabinRearZ = -0.75;
            cb.totalHeight = 1.85;
            cb.hasCargoBed = false;
            cb.bedFloorHeight = 1.05 + gY;
        }

        if (def.physics != null) {
            def.physics.frontOverhang = Math.max(0.65, cb.frontDist - (def.physics.wheelbase * 0.5));
            def.physics.rearOverhang = Math.max(0.65, cb.rearDist - (def.physics.wheelbase * 0.5));
            def.physics.trackWidth = cb.halfWidth * 2.0;
        }
    }
}
