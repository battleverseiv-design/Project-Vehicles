package com.evgen.cardecor.event;

import com.evgen.cardecor.CarDecorMod;
import com.evgen.cardecor.entity.DrivablePackVehicleEntity;
import com.evgen.cardecor.entity.DrivableVehicleEntity;
import com.evgen.cardecor.pack.VehicleDefinition;
import com.evgen.cardecor.pack.VehiclePackManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CarDecorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

    public static boolean isGunItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        String ns = id.getNamespace().toLowerCase();
        String path = id.getPath().toLowerCase();
        String cls = stack.getItem().getClass().getName().toLowerCase();

        if (ns.contains("tacz") || ns.contains("tac") || ns.contains("timeless") ||
            ns.contains("superbwarfare") || ns.contains("mw") || ns.contains("cgm") ||
            ns.contains("modernwarfare") || ns.contains("gensosenkyo") || ns.contains("gun") ||
            ns.contains("vikingsweapons") || ns.contains("pointblank") || ns.contains("vice") ||
            ns.contains("ewewukek")) {
            return true;
        }

        if (cls.contains("gun") || cls.contains("firearm") || cls.contains("tacz") || cls.contains("shooter")) {
            return true;
        }

        if (path.contains("gun") || path.contains("rifle") || path.contains("pistol") ||
            path.contains("shotgun") || path.contains("sniper") || path.contains("smg") ||
            path.contains("rpg") || path.contains("launcher") || path.contains("revolver") ||
            path.contains("bow") || path.contains("crossbow")) {
            return true;
        }

        return false;
    }

    public static boolean isWeaponRestrictedSeat(Player player) {
        if (player == null) return false;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof DrivablePackVehicleEntity packVehicle) {
            int seatIdx = packVehicle.getSeatForPlayer(player);
            VehicleDefinition def = VehiclePackManager.get(packVehicle.getVehicleId());
            if (def != null && seatIdx >= 0 && seatIdx < def.seats.size()) {
                return !def.seats.get(seatIdx).allowWeapons;
            }
            return (seatIdx == 0); // Default: driver restricted
        } else if (vehicle instanceof DrivableVehicleEntity devVehicle) {
            int seat = devVehicle.getSeatForPlayer(player);
            return seat == 0; // Default: driver restricted
        }
        return false;
    }



    public static final ThreadLocal<Boolean> REDIRECTING_DAMAGE = DrivableVehicleEntity.REDIRECTING_DAMAGE;
    public static final ThreadLocal<Boolean> ALLOW_PASSENGER_PENETRATION = DrivableVehicleEntity.ALLOW_PASSENGER_PENETRATION;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (REDIRECTING_DAMAGE.get()) {
            return;
        }

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Shooter on weapon-restricted seat cannot shoot out
        if (attacker instanceof Player shooter && isWeaponRestrictedSeat(shooter)) {
            event.setCanceled(true);
            return;
        }

        LivingEntity target = event.getEntity();
        if (target != null && target.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            boolean isMobAttack = (attacker instanceof net.minecraft.world.entity.Mob)
                || (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof net.minecraft.world.entity.Mob);

            // ONLY intercept attacks from MOBS. All environmental damage (cold/freeze, hunger, drowning, etc.) affects player directly!
            if (!isMobAttack) {
                return;
            }

            if (ALLOW_PASSENGER_PENETRATION.get()) {
                boolean isArrowOrSkeleton = (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow)
                    || (attacker instanceof net.minecraft.world.entity.monster.AbstractSkeleton);
                if (!isArrowOrSkeleton) {
                    return;
                }
            }

            if (vehicle.getExplosionStage() >= 3) {
                return;
            }

            // Mobs hit the car frame instead of the passenger:
            event.setCanceled(true);
            target.invulnerableTime = 20;

            float rawDamage = event.getAmount();
            float vehicleDamage = Math.max(2.0F, rawDamage * 0.85F);
            try {
                REDIRECTING_DAMAGE.set(true);
                vehicle.hurt(source, vehicleDamage);
            } finally {
                REDIRECTING_DAMAGE.set(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (ALLOW_PASSENGER_PENETRATION.get()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            LivingEntity target = event.getEntity();
            if (target != null && target.getVehicle() instanceof DrivableVehicleEntity) {
                target.clearFire();
                target.setRemainingFireTicks(0);
                event.setCanceled(true);
            }
            return;
        }
        if (source.getEntity() instanceof DrivableVehicleEntity || source.getDirectEntity() instanceof DrivableVehicleEntity) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target != null && target.getVehicle() instanceof DrivableVehicleEntity vehicle) {
            Entity attacker = source.getEntity();
            boolean isMobAttack = (attacker instanceof net.minecraft.world.entity.Mob)
                || (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof net.minecraft.world.entity.Mob);
            if (!isMobAttack) {
                return;
            }
            if (vehicle.getExplosionStage() >= 3) return;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        DrivableVehicleEntity vehicle = null;
        if (target instanceof DrivableVehicleEntity v) {
            vehicle = v;
        } else if (target instanceof com.evgen.cardecor.entity.VehiclePartEntity part) {
            vehicle = part.getParent();
        }

        if (vehicle != null && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            Player player = event.getEntity();
            if (vehicle.getTargetedHood(player) != null) {
                ItemStack held = event.getItemStack();
                if (!held.is(com.evgen.cardecor.init.ModItems.MOTOR_OIL.get()) &&
                    !held.is(com.evgen.cardecor.init.ModItems.COOLANT.get()) &&
                    !held.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                    vehicle.openMechanicsMenu(player);
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isWeaponRestrictedSeat(event.getEntity()) && isGunItem(event.getItemStack())) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isWeaponRestrictedSeat(event.getEntity()) && isGunItem(event.getItemStack())) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isWeaponRestrictedSeat(event.getEntity()) && isGunItem(event.getEntity().getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            Entity projectile = event.getProjectile();
            Entity shooter = (projectile instanceof net.minecraft.world.entity.projectile.Projectile p) ? p.getOwner() : null;

            if (projectile != null) {
                DrivableVehicleEntity vehicle = null;
                if (target instanceof DrivableVehicleEntity veh) {
                    vehicle = veh;
                } else if (target instanceof com.evgen.cardecor.entity.VehiclePartEntity part) {
                    vehicle = part.getParent();
                }

                if (vehicle != null) {
                    if (shooter == null) {
                        for (Entity p : vehicle.getPassengers()) {
                            if (p.distanceToSqr(projectile) < 36.0) { shooter = p; break; }
                        }
                        if (shooter == null) {
                            for (int id : vehicle.currentDeckEntityIds) {
                                Entity ent = vehicle.level().getEntity(id);
                                if (ent != null && ent.distanceToSqr(projectile) < 36.0) { shooter = ent; break; }
                            }
                        }
                        if (shooter == null) {
                            for (int id : vehicle.airborneDeckEntityIds) {
                                Entity ent = vehicle.level().getEntity(id);
                                if (ent != null && ent.distanceToSqr(projectile) < 36.0) { shooter = ent; break; }
                            }
                        }
                    }

                    if (shooter != null && vehicle.isShooterOnVehicle(shooter)) {
                        // Projectiles pass cleanly through the vehicle unless deliberately aiming straight down at the floor
                        if (!(shooter instanceof LivingEntity living && living.getXRot() >= 80.0F)) {
                            event.setCanceled(true);
                            com.evgen.cardecor.compat.TaczCompat.incrementBulletPierce(projectile);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VehiclePackManager.checkHotReload();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.evgen.cardecor.command.VehicleSpawnCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEntityMount(net.minecraftforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof DrivableVehicleEntity vehicle) {
            if (event.getEntityMounting() instanceof Player player && player.isAlive() && !player.isCreative()) {
                if (!vehicle.isAllowedToDismount()) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
