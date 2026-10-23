package com.evgen.cardecor.init;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CarDecorMod.MODID);

    public static final RegistryObject<EntityType<DrivablePackVehicleEntity>> DRIVABLE_PACK_VEHICLE = ENTITY_TYPES.register("drivable_pack_vehicle",
        () -> EntityType.Builder.<DrivablePackVehicleEntity>of(DrivablePackVehicleEntity::new, MobCategory.MISC)
            .sized(1.00F, 0.40F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .fireImmune()
            .build("drivable_pack_vehicle")
    );
}
