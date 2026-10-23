package com.evgen.cardecor.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class VehiclePartEntity extends Entity {

    public final DrivableVehicleEntity parent;
    public final String partType;
    private EntityDimensions dimensions;
    public double localX;
    public double localY;
    public double localZ;

    public VehiclePartEntity(DrivableVehicleEntity parent, String partType, float width, float height, double localX, double localY, double localZ) {
        super(parent.getType(), parent.level());
        this.parent = parent;
        this.partType = partType;
        this.dimensions = EntityDimensions.scalable(width, height);
        this.localX = localX;
        this.localY = localY;
        this.localZ = localZ;
        this.refreshDimensions();
    }

    public DrivableVehicleEntity getParent() {
        return this.parent;
    }

    public void setDimensions(float width, float height) {
        this.dimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public void setCustomBoundingBox(AABB aabb) {
        if (aabb != null) {
            super.setBoundingBox(aabb);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        if (this.isInvulnerableTo(source)) return false;
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        Entity shooter = (direct instanceof net.minecraft.world.entity.projectile.Projectile proj) ? proj.getOwner() : attacker;
        DrivableVehicleEntity parent = this.getParent();
        if (parent != null) {
            if (shooter == null && direct != null) {
                for (Entity p : parent.getPassengers()) {
                    if (p.distanceToSqr(direct) < 36.0) { shooter = p; break; }
                }
                if (shooter == null) {
                    for (int id : parent.currentDeckEntityIds) {
                        Entity ent = parent.level().getEntity(id);
                        if (ent != null && ent.distanceToSqr(direct) < 36.0) { shooter = ent; break; }
                    }
                }
                if (shooter == null) {
                    for (int id : parent.airborneDeckEntityIds) {
                        Entity ent = parent.level().getEntity(id);
                        if (ent != null && ent.distanceToSqr(direct) < 36.0) { shooter = ent; break; }
                    }
                }
            }
            if (shooter != null && parent.isShooterOnVehicle(shooter)) {
                if (!(shooter instanceof net.minecraft.world.entity.LivingEntity living && living.getXRot() >= 80.0F)) {
                    if (direct != null) {
                        com.evgen.cardecor.compat.TaczCompat.incrementBulletPierce(direct);
                    }
                    return false;
                }
            }
        }
        return this.getParent().hurtPart(this, source, amount);
    }


    @Override
    public boolean isPassengerOfSameVehicle(Entity other) {
        if (other == null) return false;
        DrivableVehicleEntity parent = this.getParent();
        if (parent != null) {
            if (parent == other || parent.hasPassenger(other) || parent.isShooterOnVehicle(other)) return true;
            if (parent.subEntities != null) {
                for (VehiclePartEntity part : parent.subEntities) {
                    if (part == other) return true;
                }
            }
        }
        return super.isPassengerOfSameVehicle(other);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return this.getParent().interactPart(this, player, hand);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.dimensions;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
