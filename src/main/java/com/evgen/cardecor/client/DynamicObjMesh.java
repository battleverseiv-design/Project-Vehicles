package com.evgen.cardecor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DynamicObjMesh {

    public static class Vertex {
        public float x, y, z;
        public float u, v;
        public float nx, ny, nz;

        public Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            this.x = x; this.y = y; this.z = z;
            this.u = u; this.v = v;
            this.nx = nx; this.ny = ny; this.nz = nz;
        }
    }

    public final List<Vertex> vertices = new ArrayList<>();
    public DynamicObjMesh lodMesh;
    public DynamicObjMesh lodFarMesh;
    public DynamicObjMesh lodUltraFarMesh;
    public String name;
    public int index;
    public String materialName;
    public final List<String> aliases = new ArrayList<>();

    public boolean matches(String query) {
        if (query == null) return false;
        if (name != null && name.equalsIgnoreCase(query)) return true;
        if (("part_" + index).equalsIgnoreCase(query)) return true;
        for (String a : aliases) {
            if (a.equalsIgnoreCase(query)) return true;
        }
        return false;
    }

    public static DynamicObjMesh load(File file) {
        return loadAndSplit(file, null)[0];
    }

    public static boolean isPickupSpareWheelFace(float[][] pts) {
        if (pts == null || pts.length == 0) return false;
        for (float[] p : pts) {
            if (p == null) return false;
            // Exact bounding box of pickup spare tire: X in [-0.46, 0.40], Y in [0.90, 1.80], Z in [0.16, 0.60]
            if (p[0] < -0.46F || p[0] > 0.40F ||
                p[1] < 0.90F  || p[1] > 1.80F ||
                p[2] < 0.16F  || p[2] > 0.60F) {
                return false;
            }
        }
        return true;
    }



    public static DynamicObjMesh[] loadAndSplit(File file, java.util.function.Predicate<float[][]> isSpareFace) {
        DynamicObjMesh mesh = new DynamicObjMesh();
        mesh.lodMesh = new DynamicObjMesh();
        mesh.lodFarMesh = new DynamicObjMesh();
        mesh.lodUltraFarMesh = new DynamicObjMesh();
        DynamicObjMesh spareMesh = (isSpareFace != null) ? new DynamicObjMesh() : null;
        if (spareMesh != null) {
            spareMesh.lodMesh = new DynamicObjMesh();
            spareMesh.lodFarMesh = new DynamicObjMesh();
            spareMesh.lodUltraFarMesh = new DynamicObjMesh();
        }
        if (!file.exists()) return new DynamicObjMesh[]{mesh, spareMesh};

        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        boolean flipV = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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

                String[] parts = line.split("\\s+");
                if (parts[0].equals("v") && parts.length >= 4) {
                    positions.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                    });
                } else if (parts[0].equals("vt") && parts.length >= 3) {
                    uvs.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2])
                    });
                } else if (parts[0].equals("vn") && parts.length >= 4) {
                    normals.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                    });
                } else if (parts[0].equals("usemtl") && parts.length >= 2) {
                    mesh.materialName = parts[1];
                    if (spareMesh != null) spareMesh.materialName = parts[1];
                } else if (parts[0].equals("f") && parts.length >= 4) {
                    int vertCount = parts.length - 1;
                    int[] pIdx = new int[vertCount];
                    int[] uvIdx = new int[vertCount];
                    int[] nIdx = new int[vertCount];
                    float[][] facePts = new float[vertCount][];

                    for (int i = 0; i < vertCount; i++) {
                        String[] fparts = parts[i + 1].split("/");
                        int pi = Integer.parseInt(fparts[0]);
                        pIdx[i] = (pi > 0) ? pi - 1 : positions.size() + pi;
                        if (pIdx[i] >= 0 && pIdx[i] < positions.size()) {
                            facePts[i] = positions.get(pIdx[i]);
                        } else {
                            facePts[i] = new float[]{0, 0, 0};
                        }

                        if (fparts.length > 1 && !fparts[1].isEmpty()) {
                            int uvi = Integer.parseInt(fparts[1]);
                            uvIdx[i] = (uvi > 0) ? uvi - 1 : uvs.size() + uvi;
                        } else {
                            uvIdx[i] = -1;
                        }

                        if (fparts.length > 2 && !fparts[2].isEmpty()) {
                            int ni = Integer.parseInt(fparts[2]);
                            nIdx[i] = (ni > 0) ? ni - 1 : normals.size() + ni;
                        } else {
                            nIdx[i] = -1;
                        }
                    }

                    boolean isSpare = (isSpareFace != null && isSpareFace.test(facePts));
                    DynamicObjMesh targetMesh = (isSpare && spareMesh != null) ? spareMesh : mesh;

                    // Compute average 3D position of face vertices
                    float avgX = 0, avgY = 0, avgZ = 0;
                    for (int i = 0; i < vertCount; i++) {
                        float[] p = facePts[i];
                        avgX += p[0]; avgY += p[1]; avgZ += p[2];
                    }
                    avgX /= vertCount; avgY /= vertCount; avgZ /= vertCount;

                    // Compute true 3D surface area of the entire face (preserves quads and polygons intact)
                    float faceArea = 0.0F;
                    for (int i = 1; i < vertCount - 1; i++) {
                        int i0 = 0, i1 = i, i2 = i + 1;
                        float[] p0 = positions.get(pIdx[i0]);
                        float[] p1 = positions.get(pIdx[i1]);
                        float[] p2 = positions.get(pIdx[i2]);
                        float ex1 = p1[0] - p0[0], ey1 = p1[1] - p0[1], ez1 = p1[2] - p0[2];
                        float ex2 = p2[0] - p0[0], ey2 = p2[1] - p0[1], ez2 = p2[2] - p0[2];
                        float fnx = ey1 * ez2 - ez1 * ey2;
                        float fny = ez1 * ex2 - ex1 * ez2;
                        float fnz = ex1 * ey2 - ey1 * ex2;
                        faceArea += (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz) * 0.5F;
                    }

                    boolean isWheel = file.getName().toLowerCase().contains("wheel");
                    boolean keepInLod1;
                    boolean keepInLod2;
                    boolean keepInLod3;

                    if (isWheel) {
                        // For wheels: avgX > 0.06 is the inner hub facing inside the dark underside of the car
                        boolean isInnerHub = (avgX > 0.06F);
                        keepInLod1 = !isInnerHub;
                        keepInLod2 = !isInnerHub && (faceArea >= 0.0010F || Math.abs(avgX) > 0.05F);
                        keepInLod3 = !isInnerHub && (faceArea >= 0.0030F || Math.abs(avgX) > 0.05F);
                    } else {
                        // Keep full vehicle body geometry intact across all LODs — zero holes, zero cut cabins or missing panels
                        keepInLod1 = true;
                        keepInLod2 = true;
                        keepInLod3 = true;
                    }

                    // Fan triangulation for polygons into triangles
                    for (int i = 1; i < vertCount - 1; i++) {
                        int i0 = 0, i1 = i, i2 = i + 1;
                        float[] p0 = positions.get(pIdx[i0]);
                        float[] p1 = positions.get(pIdx[i1]);
                        float[] p2 = positions.get(pIdx[i2]);

                        // Compute true geometric normal for this triangle
                        float ex1 = p1[0] - p0[0], ey1 = p1[1] - p0[1], ez1 = p1[2] - p0[2];
                        float ex2 = p2[0] - p0[0], ey2 = p2[1] - p0[1], ez2 = p2[2] - p0[2];
                        float fnx = ey1 * ez2 - ez1 * ey2;
                        float fny = ez1 * ex2 - ex1 * ez2;
                        float fnz = ex1 * ey2 - ey1 * ex2;
                        float len = (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz);
                        if (len > 1e-6F) {
                            fnx /= len; fny /= len; fnz /= len;
                        } else {
                            fnx = 0; fny = 1; fnz = 0;
                        }

                        targetMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i0], uvIdx[i0], nIdx[i0], fnx, fny, fnz, flipV);
                        targetMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i1], uvIdx[i1], nIdx[i1], fnx, fny, fnz, flipV);
                        targetMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i2], uvIdx[i2], nIdx[i2], fnx, fny, fnz, flipV);

                        if (keepInLod1 && targetMesh.lodMesh != null) {
                            targetMesh.lodMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i0], uvIdx[i0], nIdx[i0], fnx, fny, fnz, flipV);
                            targetMesh.lodMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i1], uvIdx[i1], nIdx[i1], fnx, fny, fnz, flipV);
                            targetMesh.lodMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i2], uvIdx[i2], nIdx[i2], fnx, fny, fnz, flipV);
                        }
                        if (keepInLod2 && targetMesh.lodFarMesh != null) {
                            targetMesh.lodFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i0], uvIdx[i0], nIdx[i0], fnx, fny, fnz, flipV);
                            targetMesh.lodFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i1], uvIdx[i1], nIdx[i1], fnx, fny, fnz, flipV);
                            targetMesh.lodFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i2], uvIdx[i2], nIdx[i2], fnx, fny, fnz, flipV);
                        }
                        if (keepInLod3 && targetMesh.lodUltraFarMesh != null) {
                            targetMesh.lodUltraFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i0], uvIdx[i0], nIdx[i0], fnx, fny, fnz, flipV);
                            targetMesh.lodUltraFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i1], uvIdx[i1], nIdx[i1], fnx, fny, fnz, flipV);
                            targetMesh.lodUltraFarMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i2], uvIdx[i2], nIdx[i2], fnx, fny, fnz, flipV);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to parse OBJ mesh from " + file.getName() + ": " + e.getMessage());
        }

        mesh.bake();
        if (mesh.lodMesh != null) {
            if (!mesh.lodMesh.vertices.isEmpty()) {
                mesh.lodMesh.bake();
            } else {
                mesh.lodMesh = null;
            }
        }
        if (mesh.lodFarMesh != null) {
            if (!mesh.lodFarMesh.vertices.isEmpty()) {
                mesh.lodFarMesh.bake();
            } else {
                mesh.lodFarMesh = null;
            }
        }
        if (mesh.lodUltraFarMesh != null) {
            if (!mesh.lodUltraFarMesh.vertices.isEmpty()) {
                mesh.lodUltraFarMesh.bake();
            } else {
                mesh.lodUltraFarMesh = null;
            }
        }
        if (spareMesh != null) {
            spareMesh.bake();
            if (spareMesh.lodMesh != null) {
                if (!spareMesh.lodMesh.vertices.isEmpty()) {
                    spareMesh.lodMesh.bake();
                } else {
                    spareMesh.lodMesh = null;
                }
            }
            if (spareMesh.lodFarMesh != null) {
                if (!spareMesh.lodFarMesh.vertices.isEmpty()) {
                    spareMesh.lodFarMesh.bake();
                } else {
                    spareMesh.lodFarMesh = null;
                }
            }
            if (spareMesh.lodUltraFarMesh != null) {
                if (!spareMesh.lodUltraFarMesh.vertices.isEmpty()) {
                    spareMesh.lodUltraFarMesh.bake();
                } else {
                    spareMesh.lodUltraFarMesh = null;
                }
            }
        }
        return new DynamicObjMesh[]{mesh, spareMesh};
    }

    public static java.util.Map<String, DynamicObjMesh> loadGroups(File file) {
        return loadGroups(file, false);
    }

    public static java.util.Map<String, DynamicObjMesh> loadGroups(File file, boolean forceFlipV) {
        java.util.Map<String, DynamicObjMesh> groups = new java.util.LinkedHashMap<>();
        if (!file.exists()) return groups;

        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        int objectIndex = 0;
        String currentMaterial = null;
        DynamicObjMesh currentMesh = null;
        boolean flipV = forceFlipV;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) {
                    if (line.toLowerCase().contains("blockbench")) {
                        flipV = true;
                    }
                    continue;
                }

                if (line.startsWith("o ") || line.startsWith("g ")) {
                    String g = line.substring(2).trim();
                    if (!g.isEmpty()) {
                        String key = "part_" + objectIndex;
                        currentMesh = new DynamicObjMesh();
                        currentMesh.name = g;
                        currentMesh.index = objectIndex;
                        currentMesh.materialName = currentMaterial;
                        currentMesh.aliases.add(g);
                        currentMesh.aliases.add(g + "_" + objectIndex);
                        currentMesh.aliases.add(key);
                        groups.put(key, currentMesh);
                        objectIndex++;
                    }
                    continue;
                }

                if (line.startsWith("usemtl ")) {
                    String newMat = line.substring(7).trim();
                    if (currentMesh != null && !currentMesh.vertices.isEmpty() && newMat != null && !newMat.equals(currentMaterial)) {
                        String baseName = currentMesh.name != null ? currentMesh.name : "part";
                        String key = "part_" + objectIndex;
                        currentMesh = new DynamicObjMesh();
                        currentMesh.name = baseName;
                        currentMesh.index = objectIndex;
                        currentMesh.materialName = newMat;
                        currentMesh.aliases.add(baseName);
                        currentMesh.aliases.add(key);
                        groups.put(key, currentMesh);
                        objectIndex++;
                    } else if (currentMesh != null) {
                        currentMesh.materialName = newMat;
                    }
                    currentMaterial = newMat;
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts[0].equals("v") && parts.length >= 4) {
                    positions.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                    });
                } else if (parts[0].equals("vt") && parts.length >= 3) {
                    uvs.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2])
                    });
                } else if (parts[0].equals("vn") && parts.length >= 4) {
                    normals.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                    });
                } else if (parts[0].equals("f") && parts.length >= 4) {
                    if (currentMesh == null) {
                        String key = "part_" + objectIndex;
                        currentMesh = new DynamicObjMesh();
                        currentMesh.name = "body";
                        currentMesh.index = objectIndex;
                        currentMesh.materialName = currentMaterial;
                        currentMesh.aliases.add("body");
                        currentMesh.aliases.add(key);
                        groups.put(key, currentMesh);
                        objectIndex++;
                    }

                    int vertCount = parts.length - 1;
                    int[] pIdx = new int[vertCount];
                    int[] uvIdx = new int[vertCount];
                    int[] nIdx = new int[vertCount];

                    for (int i = 0; i < vertCount; i++) {
                        String[] fparts = parts[i + 1].split("/");
                        int pi = Integer.parseInt(fparts[0]);
                        pIdx[i] = (pi > 0) ? pi - 1 : positions.size() + pi;

                        if (fparts.length > 1 && !fparts[1].isEmpty()) {
                            int uvi = Integer.parseInt(fparts[1]);
                            uvIdx[i] = (uvi > 0) ? uvi - 1 : uvs.size() + uvi;
                        } else {
                            uvIdx[i] = -1;
                        }

                        if (fparts.length > 2 && !fparts[2].isEmpty()) {
                            int ni = Integer.parseInt(fparts[2]);
                            nIdx[i] = (ni > 0) ? ni - 1 : normals.size() + ni;
                        } else {
                            nIdx[i] = -1;
                        }
                    }

                    for (int i = 1; i < vertCount - 1; i++) {
                        int i0 = 0, i1 = i, i2 = i + 1;
                        float[] p0 = positions.get(pIdx[i0]);
                        float[] p1 = positions.get(pIdx[i1]);
                        float[] p2 = positions.get(pIdx[i2]);

                        float ex1 = p1[0] - p0[0], ey1 = p1[1] - p0[1], ez1 = p1[2] - p0[2];
                        float ex2 = p2[0] - p0[0], ey2 = p2[1] - p0[1], ez2 = p2[2] - p0[2];
                        float fnx = ey1 * ez2 - ez1 * ey2;
                        float fny = ez1 * ex2 - ex1 * ez2;
                        float fnz = ex1 * ey2 - ey1 * ex2;
                        float len = (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz);
                        if (len > 1e-6F) {
                            fnx /= len; fny /= len; fnz /= len;
                        } else {
                            fnx = 0; fny = 1; fnz = 0;
                        }

                        currentMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i0], uvIdx[i0], nIdx[i0], fnx, fny, fnz, flipV);
                        currentMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i1], uvIdx[i1], nIdx[i1], fnx, fny, fnz, flipV);
                        currentMesh.addVertexWithNormal(positions, uvs, normals, pIdx[i2], uvIdx[i2], nIdx[i2], fnx, fny, fnz, flipV);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to parse OBJ groups from " + file.getName() + ": " + e.getMessage());
        }

        groups.entrySet().removeIf(e -> e.getValue().vertices.isEmpty());

        for (DynamicObjMesh gMesh : groups.values()) {
            gMesh.bake();
        }
        return groups;
    }

    private void addVertexWithNormal(List<float[]> pos, List<float[]> uvs, List<float[]> norms, int pI, int uvI, int nI, float fnx, float fny, float fnz, boolean flipV) {
        float x = 0, y = 0, z = 0;
        if (pI >= 0 && pI < pos.size()) {
            float[] p = pos.get(pI);
            x = p[0]; y = p[1]; z = p[2];
        }

        float u = 0, v = 0;
        if (uvI >= 0 && uvI < uvs.size()) {
            float[] uv = uvs.get(uvI);
            u = uv[0];
            v = flipV ? (1.0F - uv[1]) : uv[1];
        }

        float nx = fnx, ny = fny, nz = fnz;
        if (nI >= 0 && nI < norms.size()) {
            float[] n = norms.get(nI);
            nx = n[0]; ny = n[1]; nz = n[2];
        }

        vertices.add(new Vertex(x, y, z, u, v, nx, ny, nz));
    }

    public float[] packedData = null;
    public float[] originalPos = null;
    public int vertexCount = 0;

    public void bake() {
        this.vertexCount = vertices.size();
        this.packedData = new float[vertexCount * 8];
        this.originalPos = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            Vertex v = vertices.get(i);
            int base = i * 8;
            packedData[base]     = v.x;
            packedData[base + 1] = v.y;
            packedData[base + 2] = v.z;
            packedData[base + 3] = v.u;
            packedData[base + 4] = v.v;
            packedData[base + 5] = v.nx;
            packedData[base + 6] = v.ny;
            packedData[base + 7] = v.nz;

            int oBase = i * 3;
            originalPos[oBase]     = v.x;
            originalPos[oBase + 1] = v.y;
            originalPos[oBase + 2] = v.z;
        }
    }

    public DynamicObjMesh createCopy() {
        DynamicObjMesh copy = new DynamicObjMesh();
        for (Vertex v : this.vertices) {
            copy.vertices.add(new Vertex(v.x, v.y, v.z, v.u, v.v, v.nx, v.ny, v.nz));
        }
        if (this.packedData != null) {
            copy.packedData = this.packedData.clone();
            copy.vertexCount = this.vertexCount;
            if (this.originalPos != null) {
                copy.originalPos = this.originalPos.clone();
            } else {
                copy.originalPos = new float[copy.vertexCount * 3];
                for (int i = 0; i < copy.vertexCount; i++) {
                    copy.originalPos[i * 3]     = copy.packedData[i * 8];
                    copy.originalPos[i * 3 + 1] = copy.packedData[i * 8 + 1];
                    copy.originalPos[i * 3 + 2] = copy.packedData[i * 8 + 2];
                }
            }
        } else {
            copy.bake();
        }
        return copy;
    }

    public void applyDent(float localX, float localY, float localZ, float dirX, float dirY, float dirZ, float radius, float depth) {
        if (radius <= 0.48F) {
            // Bullet deformation disabled for now
            return;
        }
        if (this.packedData == null) {
            bake();
        }
        if (this.packedData == null || this.vertexCount == 0) return;

        float radSq = radius * radius;
        float dirLen = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLen > 1e-5F) {
            dirX /= dirLen; dirY /= dirLen; dirZ /= dirLen;
        } else {
            dirX = 0; dirY = 0; dirZ = -1;
        }

        float[] data = this.packedData;
        int len = this.vertexCount * 8;

        for (int i = 0; i < len; i += 8) {
            float vx = data[i];
            float dx = vx - localX;
            if (dx > radius || dx < -radius) continue;

            float vz = data[i + 2];
            float dz = vz - localZ;
            if (dz > radius || dz < -radius) continue;

            float vy = data[i + 1];
            float dy = vy - localY;
            if (dy > radius || dy < -radius) continue;

            // Strictly protect interior cabin (seats, steering wheel, dashboard, center console, pedals)
            if (Math.abs(vx) <= 0.55F && vy >= 0.42F && vy <= 1.80F && vz >= -0.90F && vz <= 0.65F) {
                continue;
            }

            int vertIdx = i / 8;
            int oBase = vertIdx * 3;
            float origX = (this.originalPos != null && oBase + 2 < this.originalPos.length) ? this.originalPos[oBase] : vx;
            float origY = (this.originalPos != null && oBase + 2 < this.originalPos.length) ? this.originalPos[oBase + 1] : vy;
            float origZ = (this.originalPos != null && oBase + 2 < this.originalPos.length) ? this.originalPos[oBase + 2] : vz;

            float curDispX = vx - origX;
            float curDispY = vy - origY;
            float curDispZ = vz - origZ;
            float curDispSq = curDispX * curDispX + curDispY * curDispY + curDispZ * curDispZ;

            boolean isBulletDent = (radius <= 0.48F);
            boolean isBumperRegion = (Math.abs(vz) >= 1.50F);
            // Subtle authentic deformation cap: 4 cm for bullets, 24 cm for bumpers/crumple zones, 14 cm for side panels
            float maxAllowedDisp = isBulletDent ? 0.040F : (isBumperRegion ? 0.24F : 0.14F);

            if (curDispSq >= maxAllowedDisp * maxAllowedDisp) {
                continue;
            }

            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < radSq) {
                float dist = (float) Math.sqrt(distSq);
                float rNorm = dist / radius;

                if (isBulletDent) {
                    // Smooth, authentic circular bullet dimple / crater profile:
                    // (1 - r^2)^2 produces a clean spherical depression with zero derivative at center & rim
                    float factor = 1.0F - (rNorm * rNorm);
                    factor = factor * factor;

                    // Push metal strictly inward into the body perpendicular to the panel using vertex normal
                    float vnx = data[i + 5];
                    float vny = data[i + 6];
                    float vnz = data[i + 7];
                    float normLenSq = vnx * vnx + vny * vny + vnz * vnz;

                    float pDirX, pDirY, pDirZ;
                    if (normLenSq > 0.05F) {
                        float dot = dirX * vnx + dirY * vny + dirZ * vnz;
                        // Push into the sheet metal (opposite normal if shot from outside)
                        float sign = (dot <= 0.0F) ? -1.0F : 1.0F;
                        // 88% pure inward normal + 12% bullet trajectory vector
                        pDirX = vnx * sign * 0.88F + dirX * 0.12F;
                        pDirY = vny * sign * 0.88F + dirY * 0.12F;
                        pDirZ = vnz * sign * 0.88F + dirZ * 0.12F;
                        float pLen = (float) Math.sqrt(pDirX * pDirX + pDirY * pDirY + pDirZ * pDirZ);
                        if (pLen > 1e-4F) {
                            pDirX /= pLen; pDirY /= pLen; pDirZ /= pLen;
                        } else {
                            pDirX = dirX; pDirY = dirY; pDirZ = dirZ;
                        }
                    } else {
                        pDirX = dirX; pDirY = dirY; pDirZ = dirZ;
                    }

                    float push = depth * factor;
                    float newX = vx + pDirX * push;
                    float newY = vy + pDirY * push;
                    float newZ = vz + pDirZ * push;

                    // Clamp to maximum allowed cumulative displacement from pristine shape
                    float propDispX = newX - origX;
                    float propDispY = newY - origY;
                    float propDispZ = newZ - origZ;
                    float propDispLen = (float) Math.sqrt(propDispX * propDispX + propDispY * propDispY + propDispZ * propDispZ);
                    if (propDispLen > maxAllowedDisp) {
                        float scale = maxAllowedDisp / propDispLen;
                        newX = origX + propDispX * scale;
                        newY = origY + propDispY * scale;
                        newZ = origZ + propDispZ * scale;
                    }

                    // Intrusion firewall: prevent exterior metal from intruding into the passenger compartment
                    if (vx > 0.86F && newX < 0.86F) newX = 0.86F;
                    if (vx < -0.86F && newX > -0.86F) newX = -0.86F;
                    if (vz < -1.25F && newZ > -1.20F) newZ = -1.20F;
                    if (vz > 0.65F && newZ < 0.65F) newZ = 0.65F;
                    if (vy > 1.92F && newY < 1.92F) newY = 1.92F;

                    data[i]     = newX;
                    data[i + 1] = newY;
                    data[i + 2] = newZ;
                } else {
                    // Collision crumple: sheet metal folding with smooth distributed crumple & wrinkle noise
                    float falloff = 1.0F - rNorm;
                    float factor = falloff * (0.35F + 0.65F * falloff);
                    float noise = (float) (Math.sin(i * 12.9898F + vx * 78.233F) * 0.014F * factor);
                    float push = ((depth * 0.45F) * factor) + (noise * 0.35F);

                    float newX = vx + dirX * push;
                    float newY = vy + dirY * push;
                    float newZ = vz + dirZ * push;

                    // Clamp to maximum allowed cumulative displacement from pristine shape
                    float propDispX = newX - origX;
                    float propDispY = newY - origY;
                    float propDispZ = newZ - origZ;
                    float propDispLen = (float) Math.sqrt(propDispX * propDispX + propDispY * propDispY + propDispZ * propDispZ);
                    if (propDispLen > maxAllowedDisp) {
                        float scale = maxAllowedDisp / propDispLen;
                        newX = origX + propDispX * scale;
                        newY = origY + propDispY * scale;
                        newZ = origZ + propDispZ * scale;
                    }

                    // Intrusion firewall: prevent crumpled outer panels from penetrating passenger cabin seats
                    if (vx > 0.55F && newX < 0.55F) newX = 0.55F;
                    if (vx < -0.55F && newX > -0.55F) newX = -0.55F;
                    if (vz < -1.25F && newZ > -1.20F) newZ = -1.20F;
                    if (vz > 0.65F && newZ < 0.65F) newZ = 0.65F;
                    if (vy > 1.92F && newY < 1.92F) newY = 1.92F;

                    data[i]     = newX;
                    data[i + 1] = newY;
                    data[i + 2] = newZ;
                }
            }
        }
    }

    public void restoreOriginal(DynamicObjMesh original) {
        if (original != null && original.packedData != null && this.packedData != null) {
            System.arraycopy(original.packedData, 0, this.packedData, 0, Math.min(original.packedData.length, this.packedData.length));
        }
    }

    public void mergeFrom(DynamicObjMesh other) {
        if (other == null) return;
        if (other.vertices != null && !other.vertices.isEmpty()) {
            for (Vertex v : other.vertices) {
                this.vertices.add(new Vertex(v.x, v.y, v.z, v.u, v.v, v.nx, v.ny, v.nz));
            }
        } else if (other.packedData != null && other.vertexCount > 0) {
            for (int i = 0; i < other.vertexCount * 8; i += 8) {
                this.vertices.add(new Vertex(
                    other.packedData[i],
                    other.packedData[i + 1],
                    other.packedData[i + 2],
                    other.packedData[i + 3],
                    other.packedData[i + 4],
                    other.packedData[i + 5],
                    other.packedData[i + 6],
                    other.packedData[i + 7]
                ));
            }
        }
    }

    public static DynamicObjMesh buildCombinedWheels(DynamicObjMesh wheelMesh, float[] fl, float[] fr, float[] rl, float[] rr) {
        if (wheelMesh == null) return null;
        if (fl == null || fr == null || rl == null || rr == null) return null;

        DynamicObjMesh combined = new DynamicObjMesh();
        combined.name = "combined_wheels";

        List<Vertex> source = wheelMesh.vertices;
        if ((source == null || source.isEmpty()) && wheelMesh.packedData != null) {
            source = new ArrayList<>();
            for (int i = 0; i < wheelMesh.vertexCount * 8; i += 8) {
                source.add(new Vertex(
                    wheelMesh.packedData[i], wheelMesh.packedData[i + 1], wheelMesh.packedData[i + 2],
                    wheelMesh.packedData[i + 3], wheelMesh.packedData[i + 4],
                    wheelMesh.packedData[i + 5], wheelMesh.packedData[i + 6], wheelMesh.packedData[i + 7]
                ));
            }
        }
        if (source == null || source.isEmpty()) return null;

        // 1. Front-Left: translated by fl
        for (Vertex v : source) {
            combined.vertices.add(new Vertex(
                v.x + fl[0], v.y + fl[1], v.z + fl[2],
                v.u, v.v,
                v.nx, v.ny, v.nz
            ));
        }

        // 2. Front-Right: rotated 180 deg around Y (-x, y, -z), then translated by fr
        for (Vertex v : source) {
            combined.vertices.add(new Vertex(
                -v.x + fr[0], v.y + fr[1], -v.z + fr[2],
                v.u, v.v,
                -v.nx, v.ny, -v.nz
            ));
        }

        // 3. Rear-Left: translated by rl
        for (Vertex v : source) {
            combined.vertices.add(new Vertex(
                v.x + rl[0], v.y + rl[1], v.z + rl[2],
                v.u, v.v,
                v.nx, v.ny, v.nz
            ));
        }

        // 4. Rear-Right: rotated 180 deg around Y (-x, y, -z), then translated by rr
        for (Vertex v : source) {
            combined.vertices.add(new Vertex(
                -v.x + rr[0], v.y + rr[1], -v.z + rr[2],
                v.u, v.v,
                -v.nx, v.ny, -v.nz
            ));
        }

        combined.bake();
        if (wheelMesh.lodMesh != null && (wheelMesh.lodMesh.packedData != null || !wheelMesh.lodMesh.vertices.isEmpty())) {
            combined.lodMesh = buildCombinedWheels(wheelMesh.lodMesh, fl, fr, rl, rr);
        }
        if (wheelMesh.lodFarMesh != null && (wheelMesh.lodFarMesh.packedData != null || !wheelMesh.lodFarMesh.vertices.isEmpty())) {
            combined.lodFarMesh = buildCombinedWheels(wheelMesh.lodFarMesh, fl, fr, rl, rr);
        }
        if (wheelMesh.lodUltraFarMesh != null && (wheelMesh.lodUltraFarMesh.packedData != null || !wheelMesh.lodUltraFarMesh.vertices.isEmpty())) {
            combined.lodUltraFarMesh = buildCombinedWheels(wheelMesh.lodUltraFarMesh, fl, fr, rl, rr);
        }
        return combined;
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int r, int g, int b, int a) {
        if (packedData == null) {
            if (vertices.isEmpty()) return;
            bake();
        }

        float[] data = this.packedData;
        int len = this.vertexCount * 8;
        if (data == null || len == 0) return;

        Matrix4f posMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        float nm00 = normMat.m00, nm10 = normMat.m10, nm20 = normMat.m20;
        float nm01 = normMat.m01, nm11 = normMat.m11, nm21 = normMat.m21;
        float nm02 = normMat.m02, nm12 = normMat.m12, nm22 = normMat.m22;

        float pm00 = posMat.m00(), pm10 = posMat.m10(), pm20 = posMat.m20(), pm30 = posMat.m30();
        float pm01 = posMat.m01(), pm11 = posMat.m11(), pm21 = posMat.m21(), pm31 = posMat.m31();
        float pm02 = posMat.m02(), pm12 = posMat.m12(), pm22 = posMat.m22(), pm32 = posMat.m32();

        for (int i = 0; i < len; i += 8) {
            float vx = data[i];
            float vy = data[i + 1];
            float vz = data[i + 2];
            float vu = data[i + 3];
            float vv = data[i + 4];

            float nx = nm00 * data[i + 5] + nm10 * data[i + 6] + nm20 * data[i + 7];
            float ny = nm01 * data[i + 5] + nm11 * data[i + 6] + nm21 * data[i + 7];
            float nz = nm02 * data[i + 5] + nm12 * data[i + 6] + nm22 * data[i + 7];

            float tx = pm00 * vx + pm10 * vy + pm20 * vz + pm30;
            float ty = pm01 * vx + pm11 * vy + pm21 * vz + pm31;
            float tz = pm02 * vx + pm12 * vy + pm22 * vz + pm32;

            buffer.vertex((double) tx, (double) ty, (double) tz)
                  .color(r, g, b, a)
                  .uv(vu, vv)
                  .overlayCoords(OverlayTexture.NO_OVERLAY)
                  .uv2(packedLight)
                  .normal(nx, ny, nz)
                  .endVertex();
        }
    }
}
