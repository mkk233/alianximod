package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.CarrierHooks;

/**
 * 效果和结算监听的安装点。不自己挂模组总线。
 * A 在术式登记完成之后调用一次。重复调用不会再注册第二份。
 */
public final class BatchCarrierBootstrap {
    private static boolean installed;

    private BatchCarrierBootstrap() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        TechniqueEffectRegistry.register(BatchCarrierIds.SPARK, new BatchSparkEffect());
        TechniqueEffectRegistry.register(BatchCarrierIds.SHOCK_RING, new BatchShockRingEffect());
        TechniqueEffectRegistry.register(BatchCarrierIds.METEORITE, new BatchMeteoriteEffect());
        TechniqueEffectRegistry.register(BatchCarrierIds.SURFACE_ANCHOR, new BatchSurfaceAnchorEffect());
        BatchRulePassEffect pass = new BatchRulePassEffect();
        TechniqueEffectRegistry.register(BatchCarrierIds.UPWELL, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.STAGGER, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.HIT_CONTINUE, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.FLAME_TRAIL, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.SEED_SOW, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.KINDLE, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.DETONATE, pass);
        TechniqueEffectRegistry.register(BatchCarrierIds.BREAK_GROUND, pass);
        CarrierHooks.addVerdictListener(new CarrierHooks.VerdictListener() {
            @Override
            public void onVerdict(com.bslx.XueXiMod.shu.technique.release.batch.HostHandoff handoff,
                                  com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict) {
            }

            @Override
            public void onResolved(com.bslx.XueXiMod.shu.technique.release.batch.ResolvedHit hit) {
                BatchFlightBook.onResolved(hit);
            }
        });
    }
}
