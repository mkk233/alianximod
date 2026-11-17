package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;

public interface ReleaseDefinition {
    String id();

    ReleaseKind kind();

    ReleaseTag tag();

    Trajectory trajectory();

    boolean moving();

    int lifetimeTicks();

    float speed();

    float thickness();

    default int pullAfterA() {
        return 0;
    }

    default boolean acceptsTargetOverride() {
        return false;
    }

    /**
     * 出手瞬间可以把瞄准收成当时的目标中心方向，不因此改生成点，也不因此持续追踪。
     * 默认与 {@link #acceptsTargetOverride()} 相同，旧术式行为不变。
     */
    default boolean acceptsInitialAim() {
        return acceptsTargetOverride();
    }

    /**
     * 出手时把方向收成当时的目标中心，之后不再跟着目标转。
     * 三者同时成立才算：接受初瞄、不接受持续索敌覆盖、弹道本身不可改。
     */
    default boolean dedicatedInitialAim() {
        return acceptsInitialAim() && !acceptsTargetOverride() && !trajectoryMutable();
    }

    /** 为真时，主手必须是剑。默认不额外要求，斧、镐、工具和三叉戟上的旧剑技保持原样。 */
    default boolean requiresHeldSword() {
        return false;
    }

    /**
     * 为真时，同一次宿主里的复制份和全局实体倍率必须整组真实生成成功后才提交一次。
     * 按登记表：只有 {@link ReleaseRegistry#submitsHostGroup(String)} 列出的术式打开。
     * 公共释放路径只读这个开关，不按术式编号分支。
     */
    default boolean submitsHostGroup() {
        return ReleaseRegistry.submitsHostGroup(id());
    }

    /** 为真时，这一发用冻结进度弧，而不是移动盒子。默认关闭，公共路径不按术式编号分支。 */
    default boolean usesArcBand() {
        return false;
    }

    /**
     * 为真时，宿主组在复制前准备一次拍号，只在整组真实提交后推进。
     * 默认关闭。公共链只读这个开关，不按术式编号分支。
     */
    default boolean tracksBeatCycle() {
        return false;
    }

    /** 未缩放的弧带半径。最终尺寸只在快照冻结时乘一次合成倍率。 */
    default float arcBandRadius() {
        return 2.4f;
    }

    /** 未缩放的弧带半宽。 */
    default float arcBandHalfWidth() {
        return 0.16f;
    }

    default boolean acceptsPathOverride() {
        return false;
    }

    default boolean setsTargetForNextDirectional() {
        return false;
    }

    default boolean setsDropForNextDirectional() {
        return false;
    }

    default ChainRole role() {
        return ChainRole.ATTACK;
    }

    /** 弹道修改能否改掉这条攻击原来的路径。不能改的只允许索敌改生成位置。 */
    default boolean trajectoryMutable() {
        return false;
    }

    /**
     * 是否为「操纵世界剑实体」类术式（幻剑术、后续召回/御剑等）。
     * 装配链含此类术式时：手持剑可放；空手仅当世界中存在该玩家的剑实体时可放。
     */
    default boolean manipulatesSwordEntities() {
        return false;
    }

    /** 这条操纵术式不能和链上的其他段一起释放。 */
    default boolean refusesMixedChain() {
        return false;
    }

    /** 来源有多大。所有属性共用，1 表示不改碰撞体积。 */
    default float energyScale() {
        return 1f;
    }

    /** 作为规则挂到后面这次攻击上的能量。1 表示这条不提供。多条相乘。 */
    default float chainEnergyScale() {
        return 1f;
    }

    /** 小于 0 时仍按伤害计算魔力。防御和治疗填自己的消耗。 */
    default int explicitManaCost() {
        return -1;
    }

    /** 小于 0 时仍按伤害和类型推装配消耗。 */
    default int explicitAssemblyCost() {
        return -1;
    }

    /**
     * 自身状态。没有声明时保持原来的攻击、场地或条件释放。
     * 公共链只读这个能力，不按术式编号分支。
     */
    default com.bslx.XueXiMod.shu.technique.release.selfstatus.SelfStatusCast selfStatus() {
        return null;
    }

    /** 按住超过本次寿命后，必须松开再按，冷却结束也不会自动再放。 */
    default boolean requiresFreshPress() {
        return false;
    }

    /** 松开释放键、换下要求的剑或施法者死亡时，提前结束这一发。 */
    default boolean endsWhenReleased() {
        return false;
    }

    /** 大于 0 才进入蓄力。打断只清场，不放收招。 */
    default int chargeTicks() {
        return 0;
    }

    /** 出手时先扣的水势。0 表示这发不扣。 */
    default float momentumSpend() {
        return 0f;
    }

    /** 场地半径。0 表示这发不是场地。 */
    default float fieldRadius() {
        return 0f;
    }

    /** 随身护体。没有声明时保持原来的攻击或场地。 */
    default GuardDefinition guard() {
        return null;
    }

    /** 固定屏障。没有声明时保持原来的攻击、场地或护体。 */
    default BarrierDefinition barrier() {
        return null;
    }

    /** 固定领域。没有声明时保持原来的攻击、场地、护体或屏障。 */
    default DomainDefinition domain() {
        return null;
    }

    /**
     * 零伤条件释放。没有声明时保持原来的攻击、场地、护体、屏障或领域。
     * 公共链只读这个能力，不按术式编号分支。
     */
    default ConditionalEffect conditionalEffect() {
        return null;
    }

    /**
     * 显式承接来袭弹。默认关闭。
     * 水幕、水环和三拍水弧在各自工厂打开。别的屏障、护体和径向斩击不会因为类型相同就接上。
     */
    default boolean deflectsProjectiles() {
        return false;
    }

    /**
     * 持续接触先记下载荷，等这一下确实打出正伤再提交。默认关闭。
     * 热光束已有命中间隔，不能靠间隔大于 0 打开这项。公共结算只读这个开关。
     */
    default boolean persistentFlowSettlement() {
        return false;
    }

    default int fieldSpawnCost() {
        return 0;
    }

    default int fieldUpkeepCost() {
        return 0;
    }

    default int fieldUpkeepPeriod() {
        return 20;
    }

    default int fieldLifetimeTicks() {
        return 200;
    }

    default float healAmount() {
        return 0f;
    }

    /** 为真时治疗打在施法者身上，不打在被命中的目标上。 */
    default boolean healCaster() {
        return false;
    }

    default float pullStrength() {
        return 0f;
    }

    default int wetnessGrant() {
        return 0;
    }

    /** 命中时熄灭目标身上的火。不改世界里的火方块。 */
    default boolean purifyOnHit() {
        return false;
    }

    /** 生成的命中体挡住移动。不改方块，也不改流体。 */
    default boolean blocksMovement() {
        return false;
    }

    default float pressurePulse() {
        return 0f;
    }

    default float erosionPulse() {
        return 0f;
    }

    /** 命中时的水平击退。0 表示不从定义里加。 */
    default float knockbackStrength() {
        return 0f;
    }

    /** 命中时向上抛。0 表示不抛。 */
    default float liftStrength() {
        return 0f;
    }

    /** 人站在这块场地里时削掉的伤害比例。0 到 1，多块场地取最大的一份。 */
    default float unloadRatio() {
        return 0f;
    }

    /** 场地里每拍刷新的减速时长。0 表示不减速。 */
    default int slowTicks() {
        return 0;
    }

    /** 场地正常到期时在中心打一次。被打断或维持失败不打。 */
    default float endBurstDamage() {
        return 0f;
    }

    /** 0 表示沿用效果自己的外观。 */
    default int bodyVisual() {
        return 0;
    }

    default String functionId() {
        return "";
    }

    default ReleaseShape shape() {
        return ReleaseShape.BOX;
    }

    /** 宽度相对厚度的倍数。 */
    default float spanScale() {
        return 1f;
    }

    /** 长度相对厚度的倍数。 */
    default float lengthScale() {
        return 1f;
    }

    /** 能量是否放大这条攻击的长度。默认和宽、高一起放大。 */
    default boolean energyScalesLength() {
        return true;
    }

    /** 能量是否放大宽度。 */
    default boolean energyScalesWidth() {
        return true;
    }

    /** 能量是否放大高度。 */
    default boolean energyScalesHeight() {
        return true;
    }

    /** 能量是否放大模型倍率。画面若另读宽高，术式可以关掉，避免乘两次。 */
    default boolean energyScalesModel() {
        return true;
    }

    /**
     * 为真时，由命中实体承载的剑术视觉在同锚点多份时分道。
     * 默认关闭。法术、幻剑和操纵已有剑的术式保持原来的画面。
     */
    default boolean separatesCohortVisuals() {
        return false;
    }

    /** 每一份有效攻击要预留的重水水势。0 表示这发不走重水。 */
    default float heavyMomentumPerCopy() {
        return 0f;
    }

    /**
     * 重压门槛。0 表示不挂重压。
     * 命中前必须已经有潮湿效果，并且潮湿值达到这个数（26 起是二档）。
     */
    default int pressureWetnessMinimum() {
        return 0;
    }
}