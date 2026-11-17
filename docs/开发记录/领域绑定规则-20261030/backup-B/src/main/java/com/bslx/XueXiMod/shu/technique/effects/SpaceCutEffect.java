package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupContext;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetVisual;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SpaceCutEffect implements TechniqueEffect {
    private static final String TEXTURE = "xueximod:textures/entity/xueximod/dimension_willow_v8.png";
    private static final int LIFETIME = 10;
    private static final StillSheetVisual STILL_SHEET = StillSheetVisual.once(TEXTURE, 20, 1);

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = cast.getLook();
        ReleaseBatch batch = ReleaseBatch.current();
        Vec3 anchor = batch != null
                ? batch.resolveAnchor(player)
                : player.getEyePosition().add(look.scale(4.0));
        Vec3 normal = horizontal(look);
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        HostGroupContext group = HostGroupContext.current();
        if (group != null) {
            group.registerAction(OriginalHumanoid.SPACE_CUT_ID);
        }

        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0;
        spec.length = 3.0;
        spec.width = 5.0;
        spec.height = 0.0;
        spec.texture = TEXTURE;
        spec.stillSheet = STILL_SHEET;
        spec.lifetime = LIFETIME;
        spec.hitInterval = 0;
        spec.lockThisCast = false;
        spec.dropThisCast = false;
        spec.centered = true;
        spec.anchor = anchor;
        spec.aim = normal;
        spec.listener = new TechniqueHitSpec.HitListener() {
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
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    private static Vec3 horizontal(Vec3 look) {
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(look.x / length, 0.0, look.z / length);
    }
}
