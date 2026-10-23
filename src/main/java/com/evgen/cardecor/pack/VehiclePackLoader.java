package com.evgen.cardecor.pack;

import com.evgen.cardecor.CarDecorMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = CarDecorMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VehiclePackLoader {

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path packsDir = FMLPaths.GAMEDIR.get().resolve("cardecor_packs");
            if (!Files.exists(packsDir)) {
                try {
                    Files.createDirectories(packsDir);
                } catch (IOException ignored) {}
            }

            event.addRepositorySource(packConsumer -> {
                VehiclePackManager.loadPacks();
                registerPacksFrom(packsDir, packConsumer);
                Path cacheDir = packsDir.resolve(".cache");
                if (Files.exists(cacheDir)) {
                    registerPacksFrom(cacheDir, packConsumer);
                }
            });
        }
    }

    private static void registerPacksFrom(Path dir, java.util.function.Consumer<Pack> packConsumer) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !entry.getFileName().toString().startsWith(".")) {
                    VehiclePackManager.ensurePackMcmeta(entry.toFile());
                    if (Files.exists(entry.resolve("pack.mcmeta"))) {
                        String packId = "cardecor_pack_" + entry.getFileName().toString();
                        Pack pack = Pack.readMetaAndCreate(
                            packId,
                            Component.literal("CarDecor Pack: " + entry.getFileName().toString()),
                            true,
                            (name) -> new PathPackResources(name, entry, true),
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                        );
                        if (pack != null) {
                            packConsumer.accept(pack);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
