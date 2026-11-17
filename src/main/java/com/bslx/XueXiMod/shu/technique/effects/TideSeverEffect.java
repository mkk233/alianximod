package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TideSeverEffect implements TechniqueEffect {
    static final double REACH = 5.0D;
    static final double SLASH_HEIGHT = 3.0D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(SpecialBalance.begin("tide_sever"),
                        BalanceKeys.BEAM_LENGTH, REACH, "断潮纵深"),
                BalanceKeys.BOX_HEIGHT, SLASH_HEIGHT, "断潮高度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0;
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        spec.length = SpecialBalance.number("tide_sever", BalanceKeys.BEAM_LENGTH, REACH);
        spec.width = definition != null ? definition.thickness() : 0.4;
        spec.height = SpecialBalance.number("tide_sever", BalanceKeys.BOX_HEIGHT, SLASH_HEIGHT);
        spec.lifetime = definition != null ? definition.lifetimeTicks() : 6;
        spec.texture = "xueximod:textures/entity/xueximod/horizontal_slash.png";
        spec.anchor = player.getEyePosition();
        spec.aim = cast.getLook();
        spec.listener = listener(trigger, triggered);
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    private static TechniqueHitSpec.HitListener listener(Consumer<Vec3> trigger, AtomicBoolean triggered) {
        return new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(target.position());
                }
            }

            @Override
            public void onExpire(Vec3 position) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(position);
                }
            }
        };
    }
}
