package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntities {
    private static EntityType<DrivablePackVehicleEntity> DRIVABLE_PACK_VEHICLE_TYPE;
    public static final Supplier<EntityType<DrivablePackVehicleEntity>> DRIVABLE_PACK_VEHICLE = () -> DRIVABLE_PACK_VEHICLE_TYPE;

    public static void register() {
        DRIVABLE_PACK_VEHICLE_TYPE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(CarDecorMod.MODID, "drivable_pack_vehicle"),
            FabricEntityTypeBuilder.<DrivablePackVehicleEntity>create(MobCategory.MISC, DrivablePackVehicleEntity::new)
                .dimensions(EntityDimensions.scalable(1.00F, 0.40F))
                .trackRangeChunks(16)
                .trackedUpdateRate(1)
                .forceTrackedVelocityUpdates(true)
                .fireImmune()
                .build()
        );
    }
}
