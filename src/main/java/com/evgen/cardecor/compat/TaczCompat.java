package com.evgen.cardecor.compat;

import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.entity.VehiclePartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;

public class TaczCompat {

    private static Field bulletPierceField = null;
    private static Field bulletExplosionField = null;
    private static boolean reflectionInit = false;

    public static void init() {
        if (ModList.get().isLoaded("tacz")) {
            MinecraftForge.EVENT_BUS.register(TaczListener.class);
            System.out.println("[CarDecor] TACZ detected: Registered bullet pass-through and gun hit handler.");
        }
    }

    public static void incrementBulletPierce(Entity bullet) {
        if (bullet == null) return;
        initReflection(bullet);
        if (bulletPierceField != null) {
            try {
                int cur = bulletPierceField.getInt(bullet);
                bulletPierceField.setInt(bullet, Math.max(2, cur + 1));
            } catch (Exception ignored) {
            }
        }
    }

    private static void initReflection(Entity bullet) {
        if (reflectionInit) return;
        reflectionInit = true;
        Class<?> clazz = bullet.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                if (bulletPierceField == null) {
                    bulletPierceField = clazz.getDeclaredField("pierce");
                    bulletPierceField.setAccessible(true);
                }
            } catch (NoSuchFieldException ignored) {}
            try {
                if (bulletExplosionField == null) {
                    bulletExplosionField = clazz.getDeclaredField("explosion");
                    bulletExplosionField.setAccessible(true);
                }
            } catch (NoSuchFieldException ignored) {}
            clazz = clazz.getSuperclass();
        }
    }

    public static class TaczListener {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onGunHurtPre(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre event) {
            Entity hurt = event.getHurtEntity();
            LivingEntity shooter = event.getAttacker();
            Entity bullet = event.getBullet();
            if (hurt == null) return;

            DrivableVehicleEntity vehicle = null;
            if (hurt instanceof DrivableVehicleEntity v) {
                vehicle = v;
            } else if (hurt instanceof VehiclePartEntity part) {
                vehicle = part.getParent();
            }

            if (vehicle != null) {
                if (shooter == null && bullet != null) {
                    for (Entity p : vehicle.getPassengers()) {
                        if (p instanceof LivingEntity l && p.distanceToSqr(bullet) < 36.0) { shooter = l; break; }
                    }
                    if (shooter == null) {
                        for (int id : vehicle.currentDeckEntityIds) {
                            Entity ent = vehicle.level().getEntity(id);
                            if (ent instanceof LivingEntity l && ent.distanceToSqr(bullet) < 36.0) { shooter = l; break; }
                        }
                    }
                    if (shooter == null) {
                        for (int id : vehicle.airborneDeckEntityIds) {
                            Entity ent = vehicle.level().getEntity(id);
                            if (ent instanceof LivingEntity l && ent.distanceToSqr(bullet) < 36.0) { shooter = l; break; }
                        }
                    }
                }
                if (shooter != null && vehicle.isShooterOnVehicle(shooter)) {
                    // If aiming straight down into the car (pitch >= 80°), allow intentional shot at own vehicle
                    if (shooter.getXRot() < 80.0F) {
                        event.setCanceled(true);
                        incrementBulletPierce(bullet);
                    }
                }
            }
        }
    }
}
