package com.evgen.cardecor.fabric;

import com.evgen.cardecor.command.VehicleSpawnCommand;
import com.evgen.cardecor.compat.TaczCompat;
import com.evgen.cardecor.init.*;
import com.evgen.cardecor.network.ModNetwork;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;

public class CarDecorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModEntities.register();
        ModSounds.register();
        ModCreativeTabs.register();

        ModNetwork.registerServer();
        VehiclePackManager.init();
        com.evgen.cardecor.config.VehicleNoiseConfig.load();
        com.evgen.cardecor.config.VehicleRammingConfig.load();
        TaczCompat.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VehicleSpawnCommand.register(dispatcher);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity root = entity.getRootVehicle();
            if (root instanceof com.evgen.cardecor.entity.DrivableVehicleEntity vehicle) {
                if (com.evgen.cardecor.entity.DrivableVehicleEntity.ALLOW_PASSENGER_PENETRATION.get()) {
                    boolean isArrowOrSkeleton = (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow)
                        || (source.getEntity() instanceof net.minecraft.world.entity.monster.AbstractSkeleton);
                    if (!isArrowOrSkeleton) {
                        return true;
                    }
                }
                Entity attacker = source.getEntity();
                boolean isMobAttack = (attacker instanceof net.minecraft.world.entity.Mob)
                    || (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof net.minecraft.world.entity.Mob);

                // Non-mob attacks (frostbite/freeze, starvation, etc.) damage the player directly, car takes NO damage
                if (!isMobAttack) {
                    return true;
                }

                if (source.is(DamageTypeTags.IS_FIRE)) {
                    entity.clearFire();
                    entity.setRemainingFireTicks(0);
                    return false;
                }
                if (vehicle.getExplosionStage() < 3) {
                    entity.invulnerableTime = 20;
                    float vehicleDamage = Math.max(2.0F, amount * 0.85F);
                    try {
                        com.evgen.cardecor.entity.DrivableVehicleEntity.REDIRECTING_DAMAGE.set(true);
                        vehicle.hurt(source, vehicleDamage);
                    } finally {
                        com.evgen.cardecor.entity.DrivableVehicleEntity.REDIRECTING_DAMAGE.set(false);
                    }
                    return false;
                }
            }
            return true;
        });

        System.out.println("[CarDecor-Fabric] Initialized successfully.");
    }
}
