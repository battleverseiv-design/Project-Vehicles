package com.evgen.cardecor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DirectObjModel {

    private float[] positions;
    private float[] uvs;
    private float[] normals;
    private int vertexCount = 0;

    public DirectObjModel(ResourceLocation location) {
        load(location);
    }

    private void load(ResourceLocation location) {
        String path = "/assets/" + location.getNamespace() + "/" + location.getPath();
        InputStream in = null;
        try {
            if (net.minecraft.client.Minecraft.getInstance() != null &&
                net.minecraft.client.Minecraft.getInstance().getResourceManager() != null) {
                in = net.minecraft.client.Minecraft.getInstance().getResourceManager().open(location);
            }
        } catch (Throwable ignored) {}

        if (in == null) {
            in = DirectObjModel.class.getResourceAsStream(path);
        }
        if (in == null) {
            String altPath = "assets/" + location.getNamespace() + "/" + location.getPath();
            in = DirectObjModel.class.getClassLoader().getResourceAsStream(altPath);
        }

        if (in == null) {
            System.err.println("DirectObjModel: Resource not found: " + path);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<Float> rawV = new ArrayList<>(10000);
            List<Float> rawVt = new ArrayList<>(10000);
            List<Float> rawVn = new ArrayList<>(10000);

            List<Float> finalV = new ArrayList<>(30000);
            List<Float> finalVt = new ArrayList<>(20000);
            List<Float> finalVn = new ArrayList<>(30000);

            boolean flipV = false;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) {
                    String lower = line.toLowerCase();
                    if (lower.contains("blockbench") || lower.contains("cardecor") || lower.contains("immersive vehicles") || lower.contains("wavefront obj")) {
                        flipV = true;
                    }
                    continue;
                }
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    rawV.add(Float.parseFloat(p[1]));
                    rawV.add(Float.parseFloat(p[2]));
                    rawV.add(Float.parseFloat(p[3]));
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    rawVt.add(Float.parseFloat(p[1]));
                    rawVt.add(Float.parseFloat(p[2]));
                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    rawVn.add(Float.parseFloat(p[1]));
                    rawVn.add(Float.parseFloat(p[2]));
                    rawVn.add(Float.parseFloat(p[3]));
                } else if (line.startsWith("f ")) {
                    String[] p = line.split("\\s+");
                    int faceVerts = p.length - 1;
                    int[] vIndices = new int[faceVerts];
                    int[] vtIndices = new int[faceVerts];
                    int[] vnIndices = new int[faceVerts];

                    for (int i = 0; i < faceVerts; i++) {
                        String[] parts = p[i + 1].split("/");
                        vIndices[i] = Integer.parseInt(parts[0]) - 1;
                        vtIndices[i] = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) - 1 : 0;
                        vnIndices[i] = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) - 1 : 0;
                    }

                    for (int i = 1; i < faceVerts - 1; i++) {
                        int v0i = vIndices[0] * 3;
                        int v1i = vIndices[i] * 3;
                        int v2i = vIndices[i + 1] * 3;

                        // Analytical geometric normal from triangle edges: E1 x E2
                        float e1x = rawV.get(v1i) - rawV.get(v0i);
                        float e1y = rawV.get(v1i + 1) - rawV.get(v0i + 1);
                        float e1z = rawV.get(v1i + 2) - rawV.get(v0i + 2);

                        float e2x = rawV.get(v2i) - rawV.get(v0i);
                        float e2y = rawV.get(v2i + 1) - rawV.get(v0i + 1);
                        float e2z = rawV.get(v2i + 2) - rawV.get(v0i + 2);

                        float fnx = e1y * e2z - e1z * e2y;
                        float fny = e1z * e2x - e1x * e2z;
                        float fnz = e1x * e2y - e1y * e2x;
                        float fnLen = (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz);
                        if (fnLen > 1e-6f) {
                            fnx /= fnLen;
                            fny /= fnLen;
                            fnz /= fnLen;
                        } else {
                            fnx = 0.0f; fny = 1.0f; fnz = 0.0f;
                        }

                        int[] tri = new int[]{0, i, i + 1};
                        for (int idx : tri) {
                            int vi = vIndices[idx] * 3;
                            int vti = vtIndices[idx] * 2;
                            int vni = vnIndices[idx] * 3;

                            finalV.add(rawV.get(vi));
                            finalV.add(rawV.get(vi + 1));
                            finalV.add(rawV.get(vi + 2));

                            if (vti + 1 < rawVt.size()) {
                                finalVt.add(rawVt.get(vti));
                                finalVt.add(flipV ? 1.0f - rawVt.get(vti + 1) : rawVt.get(vti + 1));
                            } else {
                                finalVt.add(0.0f);
                                finalVt.add(0.0f);
                            }

                            if (vni + 2 < rawVn.size()) {
                                finalVn.add(rawVn.get(vni));
                                finalVn.add(rawVn.get(vni + 1));
                                finalVn.add(rawVn.get(vni + 2));
                            } else {
                                finalVn.add(fnx);
                                finalVn.add(fny);
                                finalVn.add(fnz);
                            }
                        }
                    }
                }
            }

            vertexCount = finalV.size() / 3;
            positions = new float[finalV.size()];
            for (int i = 0; i < finalV.size(); i++) positions[i] = finalV.get(i);

            uvs = new float[finalVt.size()];
            for (int i = 0; i < finalVt.size(); i++) uvs[i] = finalVt.get(i);

            normals = new float[finalVn.size()];
            for (int i = 0; i < finalVn.size(); i++) normals[i] = finalVn.get(i);

            System.out.println("DirectObjModel: Loaded " + vertexCount + " vertices from " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        if (vertexCount == 0 || positions == null) return;

        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        for (int i = 0; i < vertexCount; i++) {
            int pIdx = i * 3;
            int uvIdx = i * 2;
            int nIdx = i * 3;

            consumer.vertex(poseMatrix, positions[pIdx], positions[pIdx + 1], positions[pIdx + 2])
                    .color(255, 255, 255, 255)
                    .uv(uvs[uvIdx], uvs[uvIdx + 1])
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normalMatrix, normals[nIdx], normals[nIdx + 1], normals[nIdx + 2])
                    .endVertex();
        }
    }
}
