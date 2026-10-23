package com.evgen.cardecor;

import com.evgen.cardecor.command.VehicleSpawnCommand;
import com.evgen.cardecor.init.ModBlocks;
import com.evgen.cardecor.init.ModCreativeTabs;
import com.evgen.cardecor.init.ModEntities;
import com.evgen.cardecor.init.ModItems;
import com.evgen.cardecor.init.ModSounds;
import com.evgen.cardecor.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CarDecorMod.MODID)
public class CarDecorMod {
    public static final String MODID = "cardecor";

    public CarDecorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        com.evgen.cardecor.init.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addPackFinders);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        event.enqueueWork(com.evgen.cardecor.pack.VehiclePackManager::loadPacks);
        event.enqueueWork(com.evgen.cardecor.config.VehicleNoiseConfig::load);
        event.enqueueWork(com.evgen.cardecor.config.VehicleRammingConfig::load);
        event.enqueueWork(com.evgen.cardecor.compat.TaczCompat::init);
    }

    private void addPackFinders(net.minecraftforge.event.AddPackFindersEvent event) {
        if (event.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            java.io.File packsDir = com.evgen.cardecor.pack.VehiclePackManager.getPacksDir();
            if (packsDir != null && packsDir.exists() && packsDir.isDirectory()) {
                java.io.File[] packFolders = packsDir.listFiles(java.io.File::isDirectory);
                if (packFolders != null) {
                    for (java.io.File pf : packFolders) {
                        java.io.File mcmeta = new java.io.File(pf, "pack.mcmeta");
                        if (!mcmeta.exists()) {
                            try {
                                java.nio.file.Files.writeString(mcmeta.toPath(),
                                    "{\"pack\":{\"pack_format\":15,\"description\":\"" + pf.getName() + "\"}}");
                            } catch (Exception ignored) {}
                        }

                        String packId = "cardecor_pack_" + pf.getName().toLowerCase();
                        net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.literal("Vehicle Pack: " + pf.getName());
                        net.minecraft.server.packs.repository.Pack.ResourcesSupplier supplier = (id) -> new net.minecraft.server.packs.PathPackResources(id, pf.toPath(), false);

                        net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                            packId,
                            title,
                            true, // Always required/active
                            supplier,
                            net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                            net.minecraft.server.packs.repository.Pack.Position.TOP,
                            net.minecraft.server.packs.repository.PackSource.BUILT_IN
                        );

                        if (pack != null) {
                            event.addRepositorySource((consumer) -> consumer.accept(pack));
                            System.out.println("[CarDecor] Registered dynamic resource pack for sounds & assets: " + pf.getName());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VehicleSpawnCommand.register(event.getDispatcher());
    }
}
