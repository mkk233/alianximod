package com.bslx.XueXiMod.shu.technique.release.clone.water;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureContract;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureMode;
import com.bslx.XueXiMod.shu.technique.release.capture.CaptureCarrierMode;

import java.util.Set;

/**
 * 水幻形的释放定义。由 ReleaseDefinitions 登记。
 * 存活刻度只占释放接口，身体寿命另读专项配置。
 */
public final class WaterPhantomDefinition implements ReleaseDefinition {
    public static final String TECHNIQUE_ID = "water_phantom";
    private static final ActorCaptureContract CAPTURE = new ActorCaptureContract(
            ActorCaptureMode.NEXT_SINGLE_ATTACK,
            ChainRole.ATTACK,
            1,
            ShuAttribute.WATER,
            true,
            true,
            true,
            CaptureCarrierMode.STAFF_ONLY,
            Set.of(TechniqueType.SWORD));

    @Override
    public String id() {
        return TECHNIQUE_ID;
    }

    @Override
    public ReleaseKind kind() {
        return ReleaseKind.ACTION;
    }

    @Override
    public ReleaseTag tag() {
        return ReleaseTag.TRIGGER;
    }

    @Override
    public Trajectory trajectory() {
        return Trajectory.STRAIGHT;
    }

    @Override
    public boolean moving() {
        return false;
    }

    @Override
    public int lifetimeTicks() {
        return 1;
    }

    @Override
    public float speed() {
        return 0F;
    }

    @Override
    public float thickness() {
        return 0F;
    }

    @Override
    public ChainRole role() {
        return ChainRole.ACTION;
    }

    @Override
    public int explicitManaCost() {
        return WaterPhantomBalance.MANA_COST;
    }

    @Override
    public boolean requiresFreshPress() {
        return true;
    }

    @Override
    public ActorCaptureContract actorCapture() {
        return CAPTURE;
    }
}
