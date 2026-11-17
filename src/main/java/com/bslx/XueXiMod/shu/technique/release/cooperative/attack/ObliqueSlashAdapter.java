package com.bslx.XueXiMod.shu.technique.release.cooperative.attack;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorFrame;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackPreparation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExplicitVolumeGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.LogicalCopyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedAttack;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedEmit;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ResolvedCopyGeometry;

import java.util.List;

/**
 * 斜斩的纯准备。已配置伤害留在准备记录里，零伤害也照样给出一个发射。
 * 不执行旧效果，不结算，不按全局实体倍率循环。
 */
public final class ObliqueSlashAdapter implements ActorAttackAdapter {
    private static final ObliqueSlashAdapter INSTANCE = new ObliqueSlashAdapter();

    private ObliqueSlashAdapter() {
    }

    public static ActorAttackAdapter adapter() {
        return INSTANCE;
    }

    @Override
    public PreparedAttack prepare(AttackPreparation input) {
        ExplicitVolumeGeometry localShape = SlashVolumeLayout.oblique(
                input.volume(), input.frozenDirection(), 0.0F, false);
        return new PreparedAttack(input, CooperativeActionIds.OBLIQUE_SLASH, localShape);
    }

    @Override
    public List<PreparedEmit> emit(PreparedAttack attack, LogicalCopyKey copy, ActorFrame actor,
                                   ResolvedCopyGeometry geometry) {
        if (CooperativeStructureLimits.EMIT_PER_COPY_PER_ACTOR != 1) {
            throw new IllegalStateException("斜斩适配只准备每演员每个逻辑副本一个发射");
        }
        ExplicitVolumeGeometry oriented = SlashVolumeLayout.oblique(
                attack.preparation().volume(),
                attack.preparation().frozenDirection(),
                actor.visualYaw(),
                actor.mirrored());
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
