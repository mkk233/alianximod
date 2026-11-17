package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.network.CastCooldown;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.effects.SwordFlightEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 一把真剑的御剑承载。启动在同一服务端线程完成，失败时不留下第二把剑。
 * 玩家通过原生乘客关系站在剑上；剑自己推进、碰撞并放好乘客。
 */
public final class SwordFlightMount {
    private final UUID ownerId;
    private final int sourceSlot;
    private final int castSlot;
    private final ItemStack sourceSnapshot;
    private final ManaPaymentReceipt startupReceipt;
    private int billingTicks;
    private boolean supported = true;
    private boolean ended;
    private float unsupportedFall;
    private Vec3 velocity = Vec3.ZERO;
    private boolean fallSettled;

    private SwordFlightMount(UUID ownerId, int sourceSlot, int castSlot, ItemStack sourceSnapshot,
                             ManaPaymentReceipt startupReceipt) {
        this.ownerId = ownerId;
        this.sourceSlot = sourceSlot;
        this.castSlot = castSlot;
        this.sourceSnapshot = sourceSnapshot;
        this.startupReceipt = startupReceipt;
    }

    /** 预留启动法力、取出真剑、加入实体并建立乘客关系。任一步失败都完整撤销。 */
    public static boolean begin(ServerPlayer player, int castSlot) {
        if (player == null || player.getServer() == null || !player.getServer().isSameThread()
                || !(player.level() instanceof ServerLevel level) || !player.isAlive()) {
            return false;
        }
        int sourceSlot = SwordFlightReturn.firstRealSwordSlot(player);
        if (sourceSlot < 0) {
            return false;
        }
        Inventory inventory = player.getInventory();
        ItemStack source = inventory.getItem(sourceSlot);
        if (!(source.getItem() instanceof SwordItem) || source.getCount() != 1) {
            return false;
        }
        ItemStack snapshot = source.copy();
        ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(player, SwordFlightEffect.STARTUP_MANA, false);
        if (receipt == null) {
            return false;
        }
        inventory.setItem(sourceSlot, ItemStack.EMPTY);
        SwordEntity sword = SwordEntity.flightMount(level, player, snapshot);
        SwordFlightMount mount = new SwordFlightMount(player.getUUID(), sourceSlot, castSlot, snapshot, receipt);
        sword.attachFlightMount(mount);
        boolean added = level.addFreshEntity(sword);
        boolean riding = added && player.startRiding(sword, true);
        if (!added || !riding || player.getVehicle() != sword) {
            if (sword.isAlive()) {
                sword.discard();
            }
            sword.detachFlightMount();
            inventory.setItem(sourceSlot, snapshot.copy());
            receipt.refund(player);
            return false;
        }
        if (SwordFlightEffect.STARTUP_MANA > 0) {
            MagicPool.showBalance(player);
        }
        return true;
    }

    /** 由剑实体服务端 tick 调用。支撑中推进并放好乘客，失去支撑后改走重力。 */
    public void tick(SwordEntity sword, ServerLevel level) {
        if (this.ended || sword == null || level == null) {
            return;
        }
        ServerPlayer rider = riderOf(sword, level);
        if (!this.supported) {
            fallUnsupported(sword, level, rider);
            return;
        }
        if (rider == null) {
            loseSupport(sword, rider);
            fallUnsupported(sword, level, null);
            return;
        }
        boolean flying = flightIntent(rider);
        this.billingTicks++;
        if (this.billingTicks >= SwordFlightEffect.BILLING_PERIOD) {
            this.billingTicks = 0;
            int upkeep = flying ? SwordFlightEffect.FLIGHT_MANA : SwordFlightEffect.HOVER_MANA;
            if (!MagicPool.spendInto(this.startupReceipt, rider, upkeep)) {
                loseSupport(sword, rider);
                fallUnsupported(sword, level, rider);
                return;
            }
            if (upkeep > 0) {
                MagicPool.showBalance(rider);
            }
        }
        Vec3 desired = flying ? propelled(rider) : hovered(this.velocity);
        Vec3 allowed = collide(sword, level, desired);
        this.velocity = allowed;
        sword.setDeltaMovement(allowed);
        sword.move(MoverType.SELF, allowed);
        SwordEntityMotion.aim(sword, sword.flightLook(rider));
        placeRider(sword, rider);
        rememberClearedFall(rider);
    }

    /** 右键归还成功后结束。失败不改物品、实体或承载。 */
    public boolean returnByUse(SwordEntity sword, ServerPlayer player) {
        if (this.ended || sword == null || !this.supported || !sameOwner(player) || !player.isAlive()
                || player.level() != sword.level() || !sword.isAlive()
                || player.distanceToSqr(sword) > SwordFlightEffect.RETURN_REACH * SwordFlightEffect.RETURN_REACH) {
            return false;
        }
        if (!SwordFlightReturn.tryReturn(player, sword.swordItem())) {
            return false;
        }
        finish(sword, player, true);
        return true;
    }

    private void fallUnsupported(SwordEntity sword, ServerLevel level, ServerPlayer rider) {
        Vec3 gravity = this.velocity.add(0.0D, -0.08D, 0.0D);
        this.velocity = gravity;
        sword.setDeltaMovement(gravity);
        sword.move(MoverType.SELF, gravity);
        if (rider != null && rider.getVehicle() == sword) {
            placeRider(sword, rider);
            rememberClearedFall(rider);
        }
        if (sword.onGround() || sword.horizontalCollision && gravity.y < 0.0D && Math.abs(gravity.y) < 0.08D) {
            settleFall(rider);
            finish(sword, rider, false);
        }
    }

    private void loseSupport(SwordEntity sword, ServerPlayer rider) {
        this.supported = false;
        this.unsupportedFall = rider == null ? 0.0F : rider.fallDistance;
        sword.markRealFalling();
    }

    private void rememberClearedFall(ServerPlayer rider) {
        if (!this.supported) {
            this.unsupportedFall += rider.fallDistance;
        }
    }

    private void settleFall(ServerPlayer rider) {
        if (this.fallSettled || rider == null) {
            return;
        }
        this.fallSettled = true;
        float kept = this.supported ? rider.fallDistance : this.unsupportedFall + rider.fallDistance;
        rider.fallDistance = kept;
        if (kept > 0.0F && rider.onGround()) {
            rider.causeFallDamage(kept, 1.0F, rider.damageSources().fall());
            rider.fallDistance = 0.0F;
        }
    }

    private void finish(SwordEntity sword, ServerPlayer rider, boolean returnedToHand) {
        if (this.ended) {
            return;
        }
        this.ended = true;
        if (rider != null && rider.getVehicle() == sword) {
            rider.stopRiding();
        }
        if (!returnedToHand) {
            if (sword.level() instanceof ServerLevel serverLevel) {
                SwordEntityMotion.returnReal(sword, serverLevel);
            } else if (sword.isAlive()) {
                sword.discard();
            }
        } else if (sword.isAlive()) {
            sword.discard();
        }
        if (rider != null && rider.getServer() != null && this.castSlot >= 0 && this.castSlot <= 3) {
            CastCooldown.mark(rider, ItemStack.EMPTY, this.castSlot,
                    rider.getServer().getTickCount() + SwordFlightEffect.END_COOLDOWN_TICKS);
        }
        sword.detachFlightMount();
    }

    private static void placeRider(SwordEntity sword, ServerPlayer rider) {
        double ridingY = sword.getY() + sword.getPassengersRidingOffset() + rider.getMyRidingOffset();
        rider.setPos(sword.getX(), ridingY, sword.getZ());
        rider.setDeltaMovement(Vec3.ZERO);
        rider.setYRot(rider.getYRot());
        rider.setXRot(rider.getXRot());
        rider.fallDistance = 0.0F;
    }

    private static Vec3 propelled(ServerPlayer rider) {
        Vec3 look = rider.getLookAngle();
        if (look.lengthSqr() < 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return look.normalize().scale(SwordFlightEffect.FIREWORK_BOOST);
    }

    private static Vec3 hovered(Vec3 current) {
        return current.scale(SwordFlightEffect.HOVER_DRAG);
    }

    private static Vec3 collide(SwordEntity sword, ServerLevel level, Vec3 motion) {
        Vec3 from = sword.position();
        Vec3 target = from.add(motion);
        BlockHitResult hit = level.clip(new ClipContext(from, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sword));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return motion;
        }
        Vec3 stopped = hit.getLocation().subtract(from);
        double keep = Math.max(0.0D, stopped.length() - 0.05D);
        if (motion.lengthSqr() < 1.0E-8D || keep <= 0.0D) {
            return Vec3.ZERO;
        }
        return motion.normalize().scale(Math.min(motion.length(), keep));
    }

    private ServerPlayer riderOf(SwordEntity sword, ServerLevel level) {
        if (!(level.getPlayerByUUID(this.ownerId) instanceof ServerPlayer owner) || !owner.isAlive()
                || owner.level() != level || owner.getVehicle() != sword) {
            return null;
        }
        return owner;
    }

    private boolean sameOwner(ServerPlayer player) {
        return player != null && this.ownerId.equals(player.getUUID());
    }

    private static boolean flightIntent(ServerPlayer rider) {
        return rider.zza > 0.05F || rider.isShiftKeyDown() && rider.getXRot() < -8.0F;
    }

    /** 读档后挂回已经建立的承载。不重新扣启动法力，也不改原槽。 */
    public static SwordFlightMount restored(UUID ownerId, int sourceSlot, int castSlot, int billingTicks,
                                            boolean supported, float unsupportedFall, Vec3 velocity) {
        SwordFlightMount restored = new SwordFlightMount(ownerId, sourceSlot, castSlot, ItemStack.EMPTY, null);
        restored.billingTicks = Math.max(0, billingTicks);
        restored.supported = supported;
        restored.unsupportedFall = Math.max(0.0F, unsupportedFall);
        restored.velocity = velocity == null ? Vec3.ZERO : velocity;
        return restored;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public int sourceSlot() {
        return this.sourceSlot;
    }

    public int castSlot() {
        return this.castSlot;
    }

    public int billingTicks() {
        return this.billingTicks;
    }

    public boolean supported() {
        return this.supported;
    }

    public float unsupportedFall() {
        return this.unsupportedFall;
    }

    public Vec3 velocity() {
        return this.velocity;
    }

    public ItemStack sourceSnapshot() {
        return this.sourceSnapshot;
    }
}
