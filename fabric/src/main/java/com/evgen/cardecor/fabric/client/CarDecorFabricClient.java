package com.evgen.cardecor.fabric.client;

import com.evgen.cardecor.client.DrivablePackVehicleRenderer;
import com.evgen.cardecor.client.ModKeyBindings;
import com.evgen.cardecor.init.ModEntities;
import com.evgen.cardecor.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CarDecorFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetwork.registerClient();

        EntityRendererRegistry.register(ModEntities.DRIVABLE_PACK_VEHICLE.get(), DrivablePackVehicleRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
            com.evgen.cardecor.init.ModBlockEntities.ENGINE_CRANE.get(),
            com.evgen.cardecor.client.render.EngineCraneBlockEntityRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
            com.evgen.cardecor.init.ModBlockEntities.TIRE_CHANGER_STATION.get(),
            com.evgen.cardecor.client.render.TireChangerBlockEntityRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
            com.evgen.cardecor.init.ModBlockEntities.ENGINE_WORKBENCH.get(),
            com.evgen.cardecor.client.render.EngineWorkbenchBlockEntityRenderer::new
        );

        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_START_ENGINE);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_HEADLIGHTS);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_SHIFT_UP);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_SHIFT_DOWN);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_CLUTCH);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_CABIN_LIGHT);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_HORN);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_BOOST);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_SWITCH_SEAT);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_HANDBRAKE);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_PARKING_BRAKE);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_DISMOUNT);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_SOUND_SETTINGS);
        KeyBindingHelper.registerKeyBinding(ModKeyBindings.KEY_RADIAL_MENU);

        FabricClientEvents.init();
        com.evgen.cardecor.fabric.pack.FabricVehiclePackLoader.init();

        System.out.println("[CarDecor-Fabric-Client] Initialized client renderer, keys, packs, and networking.");
    }
}
