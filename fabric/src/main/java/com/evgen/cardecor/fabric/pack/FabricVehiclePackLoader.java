package com.evgen.cardecor.fabric.pack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FabricVehiclePackLoader {

    private static boolean registered = false;

    public static void init() {
        com.evgen.cardecor.pack.VehiclePackManager.onReloadCallback = com.evgen.cardecor.client.ClientVehiclePackLoader::clearCache;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getResourcePackRepository() != null) {
            registerPackFinder(mc.getResourcePackRepository());
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (client.getResourcePackRepository() != null) {
                PackRepository repo = client.getResourcePackRepository();
                registerPackFinder(repo);

                boolean needsReload = false;
                repo.reload();
                List<String> selected = new ArrayList<>(repo.getSelectedIds());
                for (String id : repo.getAvailableIds()) {
                    if (id.startsWith("cardecor_pack_") && !selected.contains(id)) {
                        selected.add(id);
                        needsReload = true;
                    }
                }

                if (needsReload) {
                    repo.setSelected(selected);
                    client.reloadResourcePacks();
                    System.out.println("[CarDecor-Fabric] Client started: reloaded Cardecor resource packs. Selected: " + selected);
                } else {
                    System.out.println("[CarDecor-Fabric] Cardecor packs already loaded and active: " + selected);
                }
            }
        });
    }

    public static synchronized void registerPackFinder(PackRepository repository) {
        if (registered || repository == null) return;

        Path packsDir = FabricLoader.getInstance().getGameDir().resolve("cardecor_packs");
        if (!Files.exists(packsDir)) {
            try {
                Files.createDirectories(packsDir);
            } catch (IOException ignored) {}
        }

        RepositorySource source = packConsumer -> {
            com.evgen.cardecor.pack.VehiclePackManager.loadPacks();
            registerPacksFrom(packsDir, packConsumer);
            Path cacheDir = packsDir.resolve(".cache");
            if (Files.exists(cacheDir)) {
                registerPacksFrom(cacheDir, packConsumer);
            }
        };

        boolean added = false;
        for (Field field : PackRepository.class.getDeclaredFields()) {
            if (Set.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Set<RepositorySource> set = (Set<RepositorySource>) field.get(repository);
                    if (set != null) {
                        if (!(set instanceof LinkedHashSet) && !(set instanceof HashSet)) {
                            set = new LinkedHashSet<>(set);
                            field.set(repository, set);
                        }
                        set.add(source);
                        added = true;
                        System.out.println("[CarDecor-Fabric] Added Cardecor RepositorySource to PackRepository." + field.getName());
                        break;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        if (added) {
            registered = true;
        }
    }

    private static void registerPacksFrom(Path dir, java.util.function.Consumer<Pack> packConsumer) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !entry.getFileName().toString().startsWith(".")) {
                    com.evgen.cardecor.pack.VehiclePackManager.ensurePackMcmeta(entry.toFile());
                    if (Files.exists(entry.resolve("pack.mcmeta"))) {
                        String packId = "cardecor_pack_" + entry.getFileName().toString();
                        Pack pack = Pack.readMetaAndCreate(
                            packId,
                            Component.literal("CarDecor Pack: " + entry.getFileName().toString()),
                            true,
                            name -> new PathPackResources(name, entry, true),
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                        );
                        if (pack != null) {
                            packConsumer.accept(pack);
                            System.out.println("[CarDecor-Fabric] Successfully registered resource pack: " + packId);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
