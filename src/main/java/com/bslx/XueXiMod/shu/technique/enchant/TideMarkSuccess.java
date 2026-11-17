package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.status.WetnessState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 一次潮印成功结算的不可变贡献。伤害数字另算，这里只记条数和施术者。
 * 湿 5、固定水势 2 都按条数乘。公共实际掉血回势不在这里抵扣。
 */
public final class TideMarkSuccess {
    public static final TideMarkSuccess NONE = new TideMarkSuccess(0, null);

    private final int marks;
    private final UUID casterId;

    private TideMarkSuccess(int marks, UUID casterId) {
        this.marks = marks;
        this.casterId = casterId;
    }

    public static TideMarkSuccess of(int marks, UUID casterId) {
        if (marks <= 0) {
            return NONE;
        }
        return new TideMarkSuccess(marks, casterId);
    }

    public int marks() {
        return this.marks;
    }

    public UUID casterId() {
        return this.casterId;
    }

    public int wetness() {
        return this.marks * 5;
    }

    public int momentum() {
        return this.marks * 2;
    }

    public float flatDamage() {
        return this.marks * 2f;
    }

    /** 死亡目标不维持潮湿。施术者离线时不把固定水势转给目标或领域主人。 */
    public void grant(LivingEntity target) {
        if (this.marks <= 0) {
            return;
        }
        if (target != null && target.isAlive()) {
            WetnessState.grant(target, this.wetness());
        }
        if (this.casterId == null || target == null || target.level().isClientSide()) {
            return;
        }
        MinecraftServer server = target.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(this.casterId);
        if (caster == null) {
            return;
        }
        WaterMomentum.credit(caster, this.momentum());
    }
}
