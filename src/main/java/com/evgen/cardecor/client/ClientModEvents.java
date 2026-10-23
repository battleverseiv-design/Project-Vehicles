package com.evgen.cardecor.client;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CarDecorMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRIVABLE_PACK_VEHICLE.get(), DrivablePackVehicleRenderer::new);
        event.registerBlockEntityRenderer(com.evgen.cardecor.init.ModBlockEntities.ENGINE_CRANE.get(), com.evgen.cardecor.client.render.EngineCraneBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.evgen.cardecor.init.ModBlockEntities.TIRE_CHANGER_STATION.get(), com.evgen.cardecor.client.render.TireChangerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.evgen.cardecor.init.ModBlockEntities.ENGINE_WORKBENCH.get(), com.evgen.cardecor.client.render.EngineWorkbenchBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.KEY_START_ENGINE);
        event.register(ModKeyBindings.KEY_HEADLIGHTS);
        event.register(ModKeyBindings.KEY_SHIFT_UP);
        event.register(ModKeyBindings.KEY_SHIFT_DOWN);
        event.register(ModKeyBindings.KEY_CLUTCH);
        event.register(ModKeyBindings.KEY_CABIN_LIGHT);
        event.register(ModKeyBindings.KEY_HORN);
        event.register(ModKeyBindings.KEY_BOOST);
        event.register(ModKeyBindings.KEY_HANDBRAKE);
        event.register(ModKeyBindings.KEY_PARKING_BRAKE);
        event.register(ModKeyBindings.KEY_SWITCH_SEAT);
        event.register(ModKeyBindings.KEY_DISMOUNT);
        event.register(ModKeyBindings.KEY_SOUND_SETTINGS);
        event.register(ModKeyBindings.KEY_RADIAL_MENU);
        VehicleSoundConfig.init();
        com.evgen.cardecor.pack.VehiclePackManager.onReloadCallback = ClientVehiclePackLoader::clearCache;
    }
}
