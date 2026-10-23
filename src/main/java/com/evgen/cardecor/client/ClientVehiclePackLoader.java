package com.evgen.cardecor.client;

import com.evgen.cardecor.pack.VehicleDefinition;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientVehiclePackLoader {

    private static final Map<String, ClientVehicleAssets> ASSETS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, DynamicObjMesh> WHEEL_MESH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, DynamicObjMesh> SEAT_MESH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> WHEEL_TEXTURE_CACHE = new ConcurrentHashMap<>();
    public static final ResourceLocation FALLBACK_TIRE_TEXTURE = new ResourceLocation("cardecor", "textures/misc/tire_rubber.png");
    public static final ResourceLocation MODULAR_WHEEL_PALETTE = new ResourceLocation("cardecor", "textures/wheel_palette.png");
    public static final ResourceLocation WHEEL_PALETTE_CLEAN = new ResourceLocation("cardecor", "textures/wheels/wheel_palette_clean.png");
    public static final ResourceLocation WHEEL_PALETTE_WORN = new ResourceLocation("cardecor", "textures/wheels/wheel_palette_worn.png");
    public static final ResourceLocation WHEEL_PALETTE_RUSTY = new ResourceLocation("cardecor", "textures/wheels/wheel_palette_rusty.png");
    public static final ResourceLocation WHEEL_PALETTE_MUDDY = new ResourceLocation("cardecor", "textures/wheels/wheel_palette_muddy.png");
    public static final ResourceLocation WHEEL_PALETTE_BARNFIND = new ResourceLocation("cardecor", "textures/wheels/wheel_palette_barnfind.png");

    public static ResourceLocation getDynamicWheelPalette(com.evgen.cardecor.entity.DrivableVehicleEntity entity, int wheelIndex) {
        if (entity == null) return MODULAR_WHEEL_PALETTE;
        float dirt = entity.getDirtLevel();
        if (dirt >= 35.0F) {
            return WHEEL_PALETTE_MUDDY;
        }
        float condition;
        if (wheelIndex >= 0 && wheelIndex < 4) {
            condition = entity.getClientTireCondition(wheelIndex);
        } else {
            condition = (entity.getClientTireCondition(0) + entity.getClientTireCondition(1)) * 0.5F;
            if (dirt >= 20.0F) {
                return WHEEL_PALETTE_WORN;
            }
        }

        if (condition >= 80.0F && dirt < 15.0F) {
            return WHEEL_PALETTE_CLEAN;
        } else if (condition >= 50.0F) {
            return WHEEL_PALETTE_WORN;
        } else if (condition >= 20.0F) {
            return WHEEL_PALETTE_RUSTY;
        } else {
            return WHEEL_PALETTE_BARNFIND;
        }
    }

    private static final Map<String, DirectObjModel> DIRECT_OBJ_MODELS = new ConcurrentHashMap<>();

    public static DirectObjModel getRimModel(com.evgen.cardecor.pack.RimDefinition def) {
        if (def == null || def.model == null || def.model.isEmpty()) return null;
        return DIRECT_OBJ_MODELS.computeIfAbsent("rim:" + def.id, k -> {
            String path = def.model.startsWith("models/") ? def.model : "models/" + def.model;
            return new DirectObjModel(new ResourceLocation("cardecor", path));
        });
    }

    public static DirectObjModel getTireModel(com.evgen.cardecor.pack.TireDefinition def) {
        if (def == null || def.model == null || def.model.isEmpty()) return null;
        return DIRECT_OBJ_MODELS.computeIfAbsent("tire:" + def.id, k -> {
            String path = def.model.startsWith("models/") ? def.model : "models/" + def.model;
            return new DirectObjModel(new ResourceLocation("cardecor", path));
        });
    }

    public static DirectObjModel getBrakeModel(com.evgen.cardecor.pack.BrakeDefinition def) {
        if (def == null || def.model == null || def.model.isEmpty()) return null;
        return DIRECT_OBJ_MODELS.computeIfAbsent("brake:" + def.id, k -> {
            String path = def.model.startsWith("models/") ? def.model : "models/" + def.model;
            return new DirectObjModel(new ResourceLocation("cardecor", path));
        });
    }

    public static DynamicObjMesh getWheelMesh(com.evgen.cardecor.pack.WheelDefinition def) {
        if (def == null || def.model == null || def.model.isEmpty()) return null;
        String key = (def.id != null ? def.id : "") + ":" + def.model;
        return WHEEL_MESH_CACHE.computeIfAbsent(key, k -> {
            if (def.packDirectory != null) {
                return VehicleAutoBaker.getOrBakeMesh(def.packDirectory, def.model, true);
            }
            return null;
        });
    }

    public static ResourceLocation getWheelTexture(com.evgen.cardecor.pack.WheelDefinition def) {
        if (def == null) return FALLBACK_TIRE_TEXTURE;
        String idKey = (def.id != null ? def.id : "default") + ":" + (def.texture != null ? def.texture : "") + ":" + (def.model != null ? def.model : "");
        return WHEEL_TEXTURE_CACHE.computeIfAbsent(idKey, k -> {
            if (def.packDirectory != null) {
                // 1. Check explicit texture field in WheelDefinition JSON
                if (def.texture != null && !def.texture.trim().isEmpty()) {
                    File texFile = new File(def.packDirectory, def.texture.trim());
                    if (!texFile.exists()) {
                        texFile = new File(new File(def.packDirectory, "textures"), new File(def.texture.trim()).getName());
                    }
                    if (texFile.exists()) {
                        ResourceLocation resLoc = loadDynamicTexture("wheel_" + def.id, "tex", texFile);
                        if (resLoc != null) return resLoc;
                    }
                }

                // 2. Check MTL file of the wheel model if present
                if (def.model != null && !def.model.trim().isEmpty()) {
                    String modelName = def.model.trim();
                    int dotIdx = modelName.lastIndexOf('.');
                    String mtlName = (dotIdx != -1 ? modelName.substring(0, dotIdx) : modelName) + ".mtl";
                    File mtlFile = new File(def.packDirectory, mtlName);
                    if (!mtlFile.exists()) {
                        mtlFile = new File(new File(def.packDirectory, "models"), new File(mtlName).getName());
                    }
                    if (mtlFile.exists()) {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(mtlFile))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.startsWith("map_Kd ")) {
                                    String texName = line.substring(7).trim();
                                    if (texName.startsWith("\"") && texName.endsWith("\"")) {
                                        texName = texName.substring(1, texName.length() - 1);
                                    }
                                    File texFile = new File(def.packDirectory, texName);
                                    if (!texFile.exists()) {
                                        texFile = new File(new File(def.packDirectory, "textures"), new File(texName).getName());
                                    }
                                    if (texFile.exists()) {
                                        ResourceLocation resLoc = loadDynamicTexture("wheel_" + def.id, "mtl", texFile);
                                        if (resLoc != null) return resLoc;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[CarDecor] Failed to parse wheel MTL file " + mtlFile.getName() + ": " + e.getMessage());
                        }
                    }

                    // 3. Auto-detect texture matching wheel model base name in textures/
                    String baseName = new File(modelName).getName();
                    int bDot = baseName.lastIndexOf('.');
                    String rawBase = (bDot != -1 ? baseName.substring(0, bDot) : baseName);
                    File autoTexFile = new File(new File(def.packDirectory, "textures"), rawBase + ".png");
                    if (autoTexFile.exists()) {
                        ResourceLocation resLoc = loadDynamicTexture("wheel_" + def.id, "auto", autoTexFile);
                        if (resLoc != null) return resLoc;
                    }
                }
            }
            return FALLBACK_TIRE_TEXTURE;
        });
    }

    public static DynamicObjMesh getSeatMesh(com.evgen.cardecor.pack.SeatDefinition def) {
        if (def == null || def.model == null || def.model.isEmpty()) return null;
        String key = (def.id != null ? def.id : "") + ":" + def.model;
        return SEAT_MESH_CACHE.computeIfAbsent(key, k -> {
            if (def.packDirectory != null) {
                return VehicleAutoBaker.getOrBakeMesh(def.packDirectory, def.model, false);
            }
            return null;
        });
    }

    public static ClientVehicleAssets getAssets(String vehicleId) {
        if (vehicleId == null) return null;
        VehicleDefinition def = com.evgen.cardecor.pack.VehiclePackManager.get(vehicleId);
        return getAssets(def);
    }

    public static ClientVehicleAssets getAssets(VehicleDefinition def) {
        if (def == null || def.id == null) return null;
        ClientVehicleAssets assets = ASSETS_CACHE.get(def.id);
        if (assets == null && def.packDirectory != null) {
            loadClientAssets(def, def.packDirectory);
            assets = ASSETS_CACHE.get(def.id);
        }
        return assets;
    }

    public static void loadClientAssets(VehicleDefinition def, File packDir) {
        if (def == null || packDir == null) return;
        try {
            ClientVehicleAssets assets = new ClientVehicleAssets();

            // 1. Parse MTL file if present to resolve all material textures
            File mtlFile = null;
            if (def.models != null && def.models.chassis != null) {
                String chassisName = def.models.chassis;
                int dotIdx = chassisName.lastIndexOf('.');
                String mtlName = (dotIdx != -1 ? chassisName.substring(0, dotIdx) : chassisName) + ".mtl";
                File f = new File(packDir, mtlName);
                if (f.exists()) {
                    mtlFile = f;
                }
            }
            if (mtlFile == null) {
                File[] mtlFiles = packDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mtl"));
                if (mtlFiles != null && mtlFiles.length > 0) {
                    mtlFile = mtlFiles[0];
                }
            }

            if (mtlFile != null && mtlFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(mtlFile))) {
                    String line;
                    String curMtl = null;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (line.startsWith("newmtl ")) {
                            curMtl = line.substring(7).trim();
                        } else if (line.startsWith("map_Kd ") && curMtl != null) {
                            String texName = line.substring(7).trim();
                            if (texName.startsWith("\"") && texName.endsWith("\"")) {
                                texName = texName.substring(1, texName.length() - 1);
                            }
                            File texFile = new File(packDir, texName);
                            if (!texFile.exists()) {
                                texFile = new File(new File(packDir, "textures"), texName);
                            }
                            if (texFile.exists()) {
                                ResourceLocation resLoc = loadDynamicTexture(def.id, curMtl, texFile);
                                if (resLoc != null) {
                                    assets.materialTextures.put(curMtl, resLoc);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CarDecor] Failed to parse MTL file " + mtlFile.getName() + ": " + e.getMessage());
                }
            }

            // 2. Load primary texture
            if (def.textures != null && def.textures.chassis != null) {
                File texFile = new File(packDir, def.textures.chassis);
                if (texFile.exists()) {
                    assets.clientTexture = loadDynamicTexture(def.id, "chassis", texFile);
                }
            }
            if (assets.clientTexture == null && !assets.materialTextures.isEmpty()) {
                assets.clientTexture = assets.materialTextures.values().iterator().next();
            }

            if (def.textures != null && def.textures.wheel != null) {
                File wheelTexFile = new File(packDir, def.textures.wheel);
                if (wheelTexFile.exists()) {
                    assets.wheelTexture = loadDynamicTexture(def.id, "wheel", wheelTexFile);
                }
            }

            // 3. Load & Auto-Bake meshes with modular spare wheel support
            if (def.models != null) {
                if (def.models.spareWheel != null && !def.models.spareWheel.isEmpty()) {
                    assets.chassisMesh = VehicleAutoBaker.getOrBakeMesh(packDir, def.models.chassis, false);
                    assets.spareWheelMesh = VehicleAutoBaker.getOrBakeMesh(packDir, def.models.spareWheel, false);
                } else if (def.hasCargoBed()) {
                    DynamicObjMesh[] chassisParts = VehicleAutoBaker.getOrBakeChassisAndSpare(packDir, def.models.chassis, true);
                    assets.chassisMesh = chassisParts[0];
                    assets.spareWheelMesh = chassisParts[1];
                } else {
                    assets.chassisMesh = VehicleAutoBaker.getOrBakeMesh(packDir, def.models.chassis, false);
                    assets.spareWheelMesh = null;
                }
                assets.wheelMesh = VehicleAutoBaker.getOrBakeMesh(packDir, def.models.wheel, true);
                assets.steeringWheelMesh = VehicleAutoBaker.getOrBakeMesh(packDir, def.models.steeringWheel, false);

                // Load OBJ groups if chassis model exists
                if (def.models.chassis != null) {
                    File chassisFile = new File(packDir, def.models.chassis);
                    if (chassisFile.exists()) {
                        assets.chassisGroups = DynamicObjMesh.loadGroups(chassisFile, def.models.flipV);
                        buildBatchedChassis(def, assets);
                    }
                }
            }

            ASSETS_CACHE.put(def.id, assets);
        } catch (Exception e) {
            System.err.println("[CarDecor] Error loading client assets for " + def.id + ": " + e.getMessage());
        }
    }

    private static void buildBatchedChassis(VehicleDefinition def, ClientVehicleAssets assets) {
        if (assets.chassisGroups == null || assets.chassisGroups.isEmpty()) return;

        assets.batchedStaticChassis.clear();
        assets.batchedInteriorChassis.clear();
        assets.animatedChassisParts.clear();

        ResourceLocation defaultTex = assets.clientTexture != null ? assets.clientTexture : new ResourceLocation("minecraft", "textures/block/iron_block.png");
        java.util.Map<ResourceLocation, DynamicObjMesh> staticBatches = new java.util.LinkedHashMap<>();
        java.util.Map<ResourceLocation, DynamicObjMesh> interiorBatches = new java.util.LinkedHashMap<>();

        for (DynamicObjMesh mesh : assets.chassisGroups.values()) {
            if (mesh == null || (mesh.packedData == null && mesh.vertices.isEmpty())) continue;

            // Check if this part has an animation
            VehicleDefinition.AnimationDef matchedAnim = null;
            if (def.animations != null) {
                for (VehicleDefinition.AnimationDef a : def.animations) {
                    if (mesh.matches(a.part)) {
                        matchedAnim = a;
                        break;
                    }
                }
            }

            ResourceLocation partTex = defaultTex;
            if (mesh.materialName != null && assets.materialTextures.containsKey(mesh.materialName)) {
                partTex = assets.materialTextures.get(mesh.materialName);
            }

            if (matchedAnim != null) {
                assets.animatedChassisParts.add(new ClientVehicleAssets.AnimatedChassisPart(mesh, matchedAnim, partTex));
            } else {
                String gName = (mesh.name != null ? mesh.name : "").toLowerCase();
                boolean isInterior = gName.contains("seat") || gName.contains("pedal") || gName.contains("dash")
                    || gName.contains("gauge") || gName.contains("interior") || gName.contains("shifter")
                    || gName.contains("lever") || gName.contains("carpet") || gName.contains("console")
                    || gName.contains("column") || gName.contains("mirror") || gName.contains("handle");

                if (isInterior) {
                    DynamicObjMesh batch = interiorBatches.computeIfAbsent(partTex, k -> new DynamicObjMesh());
                    batch.mergeFrom(mesh);
                } else {
                    DynamicObjMesh batch = staticBatches.computeIfAbsent(partTex, k -> new DynamicObjMesh());
                    batch.mergeFrom(mesh);
                }
            }
        }

        for (java.util.Map.Entry<ResourceLocation, DynamicObjMesh> entry : staticBatches.entrySet()) {
            entry.getValue().bake();
            assets.batchedStaticChassis.add(new ClientVehicleAssets.BatchedStaticMesh(entry.getKey(), entry.getValue()));
        }

        for (java.util.Map.Entry<ResourceLocation, DynamicObjMesh> entry : interiorBatches.entrySet()) {
            entry.getValue().bake();
            assets.batchedInteriorChassis.add(new ClientVehicleAssets.BatchedStaticMesh(entry.getKey(), entry.getValue()));
        }
    }

    public static ResourceLocation loadDynamicTexture(String vehicleId, String key, File texFile) {
        if (!texFile.exists()) return null;
        try (InputStream is = new FileInputStream(texFile)) {
            NativeImage image = NativeImage.read(is);
            VehicleTexture vTex = new VehicleTexture(image);
            ResourceLocation resLoc = new ResourceLocation("cardecor_dynamic", "pack_" + vehicleId.toLowerCase() + "_" + key.toLowerCase().replaceAll("[^a-z0-9_]", "_") + "_" + System.currentTimeMillis());
            Minecraft.getInstance().getTextureManager().register(resLoc, vTex);
            return resLoc;
        } catch (Exception e) {
            System.err.println("[CarDecor] Failed to load dynamic texture " + texFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public static void clearCache() {
        ASSETS_CACHE.clear();
        WHEEL_TEXTURE_CACHE.clear();
        WHEEL_MESH_CACHE.clear();
        SEAT_MESH_CACHE.clear();
    }
}
