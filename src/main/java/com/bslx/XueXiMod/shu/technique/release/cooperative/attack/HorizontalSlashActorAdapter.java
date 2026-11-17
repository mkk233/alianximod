package com.bslx.XueXiMod.shu.technique.release.cooperative.attack;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorFrame;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AdapterVolumeSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackPreparation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExplicitVolumeGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.LogicalCopyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedAttack;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedEmit;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ResolvedCopyGeometry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 旧横劈在新前缀下的演员适配。尺寸和寿命读横劈自己的键。
 * 无新前缀时仍走原来的效果，本类不修改那条路径。
 */
public final class HorizontalSlashActorAdapter implements ActorAttackAdapter {
    /** 与旧效果里的纵深常量相同。 */
    public static final double DEFAULT_BOX_DEPTH = 3.0D;
    /** 与旧效果里的宽度常量相同。 */
    public static final double DEFAULT_BOX_WIDTH = 5.0D;
    /** 与旧释放定义的寿命字面量相同。 */
    public static final int DEFAULT_LIFETIME_TICKS = 4;

    private static final HorizontalSlashActorAdapter INSTANCE = new HorizontalSlashActorAdapter();
    private static final ResourceLocation PRESENTATION =
            new ResourceLocation(XueXiMod.MOD_ID, CooperativeTechniqueIds.HORIZONTAL_SLASH);

    private HorizontalSlashActorAdapter() {
    }

    public static ActorAttackAdapter adapter() {
        return INSTANCE;
    }

    /**
     * 构建攻击准备时读一次。厚度不是新键：旧效果把高度写成 0，碰撞竖向因此取纵深和 1 的较大者。
     * 倾角固定为 0，不声明、也不抄斜斩的倾角键。
     */
    public static AdapterVolumeSpec serverVolume() {
        String techniqueId = CooperativeTechniqueIds.HORIZONTAL_SLASH;
        double boxDepth = ServerSpecialRead.number(techniqueId, BalanceKeys.BOX_DEPTH, DEFAULT_BOX_DEPTH);
        double boxWidth = ServerSpecialRead.number(techniqueId, BalanceKeys.BOX_WIDTH, DEFAULT_BOX_WIDTH);
        int lifetimeTicks = ServerSpecialRead.integer(
                techniqueId, BalanceKeys.LIFETIME_TICKS, DEFAULT_LIFETIME_TICKS);
        double boxThickness = Math.max(boxDepth, 1.0D);
        return new AdapterVolumeSpec(boxDepth, boxWidth, boxThickness, lifetimeTicks, 0.0D);
    }

    @Override
    public PreparedAttack prepare(AttackPreparation input) {
        ExplicitVolumeGeometry localShape = SlashVolumeLayout.horizontal(input.volume(), input.frozenDirection());
        return new PreparedAttack(input, PRESENTATION, localShape);
    }

    @Override
    public List<PreparedEmit> emit(PreparedAttack attack, LogicalCopyKey copy, ActorFrame actor,
                                   ResolvedCopyGeometry geometry) {
        if (CooperativeStructureLimits.EMIT_PER_COPY_PER_ACTOR != 1) {
            throw new IllegalStateException("横劈适配只准备每演员每个逻辑副本一个发射");
        }
        ExplicitVolumeGeometry oriented = SlashVolumeLayout.horizontal(
                attack.preparation().volume(), attack.preparation().frozenDirection());
        ExplicitVolumeGeometry placed = SlashVolumeLayout.placeActor(oriented, geometry, actor);
        PreparedEmit preparedEmit = new PreparedEmit(
                placed,
                attack.preparation().volume().lifetimeTicks(),
                attack.presentationId(),
                copy,
                actor.actorOrdinal());
        return List.of(preparedEmit);
    }
}
