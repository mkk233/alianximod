package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.firecast.GroundFireFields;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 燎原。同一本账只铺一片地表火，不生成命中体，也不放原版火。 */
public final class LiaoyuanEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        ReleaseBatch batch = ReleaseBatch.current();
        if (player == null || batch == null || !batch.claimSurfaceFire()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        CastFrame frame = CastFrame.current();
        Vec3 origin = player.position();
        if (frame != null && frame.overrideGeometry && frame.origin != null) {
            origin = frame.origin;
        }
        double radius = SpecialBalance.number("fire_liaoyuan", BalanceKeys.RADIUS, GroundFireFields.BASE_RADIUS);
        if (frame != null && (definition == null || definition.energyScalesLength())) {
            radius *= frame.volumeScale();
        }
        int lifeTicks = definition == null ? GroundFireFields.FIELD_LIFE : definition.lifetimeTicks();
        FireCastContext fireCast = batch.fireContext();
        GroundFireFields.open(serverLevel, fireCast, origin, radius, lifeTicks);
        boolean fieldOpened = fireCast != null && origin != null && !serverLevel.isClientSide()
                && Double.isFinite(radius) && radius > 0.0d && lifeTicks > 0;
        if (fieldOpened && attempt != null) {
            attempt.reportAccepted();
        }
    }
}
