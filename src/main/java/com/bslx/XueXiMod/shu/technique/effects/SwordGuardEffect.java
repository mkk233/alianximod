package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.event.ResponseWindowService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 架剑。跟随施术者开一个 10 tick 窗口，朝向随玩家实时变化。
 * 这里只开窗；实际近战和直接命中的箭由命中入口询问。
 * 法力、冷却和装配只留命名常量，本次不注册。
 */
public final class SwordGuardEffect implements TechniqueEffect {
    public static final String TECHNIQUE_ID = "sword_guard";
    public static final float MANA_COST = 4.0F;
    public static final int COOLDOWN_TICKS = 30;
    public static final int ASSEMBLY_COST = 1;
    /** 服务端区间长度。实际区间是 {@code [start, start + WINDOW_TICKS)}。 */
    public static final int WINDOW_TICKS = 10;
    /** 身前水平左右各 60 度，合计 120 度。 */
    public static final double HALF_ANGLE_DEGREES = 60.0D;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || !(player.level() instanceof ServerLevel serverLevel)
                || !TECHNIQUE_ID.equals(technique.getId())) {
            return;
        }
        long startTick = serverLevel.getGameTime();
        ResponseWindowService.open(player.getUUID(), TECHNIQUE_ID, startTick, WINDOW_TICKS,
                () -> casterReady(player, serverLevel), null);
    }

    /** 这次实际近战是否落在架剑窗口和身前角度内。 */
    public static boolean coversMelee(ServerPlayer player, Entity attacker) {
        return covers(player, attacker);
    }

    /** 这支直接命中的箭是否落在架剑窗口和身前角度内。 */
    public static boolean coversArrow(ServerPlayer player, AbstractArrow arrow) {
        return covers(player, arrow);
    }

    /**
     * 实际近战入口里取消这一次攻击。调用方必须来自 Player.attack 或 Mob.doHurtTarget。
     * 成功时玩家不受伤，因此不会进入水环减伤；不额外伤害攻击者。
     */
    public static boolean parryActualMelee(Entity attacker, Entity target) {
        if (!(attacker instanceof LivingEntity livingAttacker) || !(target instanceof ServerPlayer player)) {
            return false;
        }
        return coversMelee(player, livingAttacker);
    }

    /**
     * 补充入口已经确认是直接怪物近战时取消这一次攻击。
     * 这里不再看伤害来源，准入仍由补充入口负责。
     */
    public static boolean parryDirectMobMelee(LivingEntity attacker, LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        return coversMelee(player, attacker);
    }

    /**
     * 直接命中的普通箭或光灵箭取消这一次伤害。三叉戟和其他非箭不处理。
     * 成功时不进入水环减伤，也不自行定义箭的反射归属。
     */
    public static boolean parryDirectArrow(AbstractArrow arrow, Entity target) {
        if (arrow == null
                || (arrow.getClass() != Arrow.class && arrow.getClass() != SpectralArrow.class)) {
            return false;
        }
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        return coversArrow(player, arrow);
    }

    private static boolean covers(ServerPlayer player, Entity incoming) {
        if (player == null || incoming == null || incoming == player
                || !(player.level() instanceof ServerLevel serverLevel)
                || player.level() != incoming.level()
                || !ResponseWindowService.open(player.getUUID(), serverLevel.getGameTime())) {
            return false;
        }
        return withinFront(player.getLookAngle(), incoming.position().subtract(player.position()));
    }

    static boolean withinFront(Vec3 look, Vec3 incoming) {
        if (!finite(look) || !finite(incoming)) {
            return false;
        }
        double lookX = look.x;
        double lookZ = look.z;
        double incomingX = incoming.x;
        double incomingZ = incoming.z;
        double lookSpan = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double incomingSpan = Math.sqrt(incomingX * incomingX + incomingZ * incomingZ);
        if (lookSpan <= 1.0E-8D || incomingSpan <= 1.0E-8D) {
            return false;
        }
        double cosine = (lookX * incomingX + lookZ * incomingZ) / (lookSpan * incomingSpan);
        cosine = Math.max(-1.0D, Math.min(1.0D, cosine));
        return Math.toDegrees(Math.acos(cosine)) <= HALF_ANGLE_DEGREES + 1.0E-6D;
    }

    private static boolean casterReady(ServerPlayer player, ServerLevel serverLevel) {
        return player.isAlive() && !player.isRemoved() && player.level() == serverLevel;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
