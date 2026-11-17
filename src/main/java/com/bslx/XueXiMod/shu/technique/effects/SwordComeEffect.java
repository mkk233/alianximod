package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SwordComeEffect implements TechniqueEffect {
    public static final double SEARCH_RANGE = 20.0D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("sword_come"), BalanceKeys.SEARCH_RANGE, SEARCH_RANGE, "剑来搜索半径"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        Vec3 spot = player.getEyePosition();
        double searchRange = SpecialBalance.number("sword_come", BalanceKeys.SEARCH_RANGE, SEARCH_RANGE);
        AABB box = new AABB(spot, spot).inflate(searchRange);
        for (SwordEntity sword : level.getEntitiesOfClass(SwordEntity.class, box, SwordEntity::isAlive)) {
            sword.startReturning(spot);
            if (attempt != null) {
                attempt.reportAccepted();
            }
        }
    }
}
