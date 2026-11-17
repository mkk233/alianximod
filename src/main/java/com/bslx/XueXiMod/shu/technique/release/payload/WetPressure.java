package com.bslx.XueXiMod.shu.technique.release.payload;

import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 旧重压控制已经停用。新的附着不再登记，命中后的结算也不再给缓慢或下压。
 * 公开方法签名保留，命中链仍可调用。这里不碰深海领域自己的减速、施湿和耗氧。
 */
public final class WetPressure {
    private WetPressure() {
    }

    public static void attach(Entity entity, UUID groupId, boolean heavy, int minimum) {
    }

    public static void note(UUID entityId, ReleaseBatch.Tally tally, LivingEntity target) {
    }

    public static void afterDamage(UUID entityId, LivingEntity target, boolean landed) {
    }

    public static void afterFlushed(ReleaseBatch.Tally tally, LivingEntity target, boolean landed) {
    }

    public static void clear(Entity entity) {
    }
}
