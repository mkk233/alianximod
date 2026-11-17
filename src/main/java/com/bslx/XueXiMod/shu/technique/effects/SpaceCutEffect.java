package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
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
    static final double REACH = 3.0D;
    static final double SPAN = 5.0D;
    private static final int LIFETIME = 10;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(SpecialBalance.begin("space_cut"),
                        BalanceKeys.BOX_DEPTH, REACH, "空间切割纵深"),
                BalanceKeys.BOX_WIDTH, SPAN, "空间切割宽度"));
    }
    private static final StillSheetVisual STILL_SHEET = StillSheetVisual.once(TEXTURE, 20, 1);

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CastFrame frame = CastFrame.current();
        boolean automatic = LaunchAim.automatic(frame);
        Vec3 look = cast.getLook();
        if (automatic && frame.look != null) {
            look = frame.look;
        }
        ReleaseBatch batch = ReleaseBatch.current();
        Vec3 anchor;
        if (automatic) {
            if (!LaunchAim.spawnAllowed(frame, serverLevel)) {
                return;
            }
            Vec3 rawAnchor = frame.anchor != null ? frame.anchor : frame.origin;
            if (rawAnchor == null) {
                rawAnchor = frame.execution.origin();
            }
            anchor = frame.clampAutomaticSource(rawAnchor);
        } else if (batch != null) {
            anchor = batch.resolveAnchor(player);
        } else {
            anchor = player.getEyePosition().add(look.scale(4.0));
        }
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
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        spec.length = SpecialBalance.number("space_cut", BalanceKeys.BOX_DEPTH, REACH);
        spec.width = SpecialBalance.number("space_cut", BalanceKeys.BOX_WIDTH, SPAN);
        spec.height = 0.0;
        spec.texture = TEXTURE;
        spec.stillSheet = STILL_SHEET;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : LIFETIME;
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
