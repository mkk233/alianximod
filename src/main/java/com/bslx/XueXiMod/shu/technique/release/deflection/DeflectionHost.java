package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeLine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 显式加入偏转的水体宿主。
 * 线段终点参数在提示里叫 to，这里叫 toward：命名门禁拒绝两个字符的参数名。
 */
public interface DeflectionHost {
    /** 水势和箭归属的主人。 */
    UUID ownerId();

    /** 宿主身份。换代时用新的生成号。 */
    HostKey key();

    /** 宿主所在的服务端世界。 */
    ServerLevel level();

    /** 现在是否还能接受接触。 */
    boolean active();
    /** 弹体线段的最早接触。没有交点时为空。 */
    Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick);

    /**
     * 近战线段。缺省仍走原来的 {@link #intersect}，零长度和贴身重合直接拒绝。
     * 水环自己覆盖这个方法，用身体包络，不在查询前把零长度丢掉。
     */
    default Optional<HostContact> intersectMelee(DeflectionAttack attack, Vec3 from, Vec3 toward, long gameTick) {
        if (from == null || toward == null || !MeleeLine.usable(from.distanceToSqr(toward))) {
            return Optional.empty();
        }
        return intersect(from, toward, 0.0D, gameTick);
    }

    /**
     * 箭心加上弹体半径后是否还在这面水体里。
     * 各宿主用自己的体积，不把零长度线段交给 {@link #intersect} 去猜。
     */
    boolean covers(Vec3 point, double projectileRadius, long gameTick);

    /** 按这次接触给出宿主自己的捕获路线。 */
    CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick);

    /**
     * 报价前的身份过滤。缺省接受一切来向，不改旧宿主的几何和计价。
     * 水牢只在自己的实现里收紧为被困者本人。
     */
    default boolean eligible(DeflectionAttack context) {
        return true;
    }
}
