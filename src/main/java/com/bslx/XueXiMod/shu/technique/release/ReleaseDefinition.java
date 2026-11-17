package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastSpec;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;

/**
 * 一条术式的释放合同。登记后由链、费用和效果读取，具体实现可覆盖默认值。
 * 接口本身不保存世界状态，也不执行释放。
 */
public interface ReleaseDefinition {
    /** 术式标识。注册表用它保存和查找。 */
    String id();

    /** 动作、命中体、规则或自身状态。 */
    ReleaseKind kind();

    /** 持续或触发。 */
    ReleaseTag tag();

    /** 基础弹道种类。是否允许被链改写另看覆盖开关。 */
    Trajectory trajectory();

    /** 为真时效果按移动体处理。 */
    boolean moving();

    /** 命中体或动作使用的寿命刻。 */
    int lifetimeTicks();

    /** 移动速度。0 表示定义里不提供速度。 */
    float speed();

    /** 命中厚度。出生半径默认由它推导。 */
    float thickness();

    /** 出生时的水平半径。默认是厚度的一半，本批载体在自己的登记里覆盖。 */
    default double birthHorizontalRadius() {
        return Math.max(0.0D, thickness() * 0.5D);
    }

    /** 出生时的竖向半高。地表净空另加 0.125，不要和水平半径混用。 */
    default double birthVerticalHalf() {
        return Math.max(0.0D, thickness() * 0.5D);
    }

    /** 规则向后额外拉取的段数。负值由调用方按可用后续段处理。 */
    default int pullAfterA() {
        return 0;
    }

    /** 为真时，前面的索敌可以持续改这条释放的目标方向。 */
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

    /** 为真时，前面的垂落标记可以改这条释放的路径。 */
    default boolean acceptsPathOverride() {
        return false;
    }

    /** 为真时，向后给下一条可接受的方向释放打上索敌。 */
    default boolean setsTargetForNextDirectional() {
        return false;
    }

    /** 为真时，向后给下一条可接受的方向释放打上垂落。 */
    default boolean setsDropForNextDirectional() {
        return false;
    }

    /** 链角色。未覆盖时是攻击。 */
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

    /**
     * 显式独立链兼容类型。默认是普通组合。
     * 不能根据自身状态、窗口角色或空手标志推断这个类型。
     */
    default ChainCompatibility chainCompatibility() {
        return ChainCompatibility.ORDINARY;
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
     * 显式领域宿主。默认关闭。
     * 正伤害攻击也可以当绑定宿主，不必把基础伤害压成 0，也不必挂上旧的领域定义。
     * 结构编译、运行调度和可绑定攻击资格都只读这个能力。
     */
    default boolean explicitDomainHost() {
        return false;
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

    /** 场地生成时另加的费用。0 表示不加。 */
    default int fieldSpawnCost() {
        return 0;
    }

    /** 场地每拍维持费用。0 表示定义里不另扣。 */
    default int fieldUpkeepCost() {
        return 0;
    }

    /** 场地维持拍的间隔。默认 20 刻。 */
    default int fieldUpkeepPeriod() {
        return 20;
    }

    /** 场地寿命。默认 200 刻。 */
    default int fieldLifetimeTicks() {
        return 200;
    }

    /** 定义自带的治疗量。0 表示不从定义加。 */
    default float healAmount() {
        return 0f;
    }

    /** 为真时治疗打在施法者身上，不打在被命中的目标上。 */
    default boolean healCaster() {
        return false;
    }

    /** 定义自带的拉拽强度。0 表示不从定义加。 */
    default float pullStrength() {
        return 0f;
    }

    /** 命中或场地授予的潮湿值。0 表示不授予。 */
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

    /** 场地使用的压力脉冲。0 表示没有。 */
    default float pressurePulse() {
        return 0f;
    }

    /** 场地使用的侵蚀脉冲。0 表示没有。 */
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

    /** 函数弹道标识。空字符串表示没有函数轨迹。 */
    default String functionId() {
        return "";
    }

    /** 粗形状。默认盒子；球体由效果改成长宽高。 */
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

    /**
     * 落点指定和三维交错只改生成位置或刀面，不要求弹道可变。
     * 斩击在弹道固定时仍交给斩击几何；可变弹道交给弹丸几何。
     * 屏障、领域、护体、场地和条件释放默认不接，需要时覆盖这个能力。
     */
    default boolean acceptsPlacementModifier() {
        if (role() != ChainRole.ATTACK || kind() != ReleaseKind.HIT_VOLUME) {
            return false;
        }
        if (barrier() != null || domain() != null || guard() != null || fieldRadius() > 0f
                || conditionalEffect() != null) {
            return false;
        }
        return true;
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

    /**
     * 多重读取声明。没有声明时保持原来的单段路径。
     * 固定正整数用 {@link MulticastSpec#fixed(int)}，从当前游标读到链尾用 {@link MulticastSpec#remaining()}。
     * 只描述本层直接读取的格数，不含玩家、报价、目标或随机结果。
     */
    default MulticastSpec multicastSpec() {
        return null;
    }

    /**
     * 一变多复制声明。没有声明时这一格不复制后续宿主。
     * 直接面对多重时忽略整段复制。面对 {@link #acceptsReplication()} 的宿主时留下复制下标。
     */
    default ReplicationSpec replication() {
        return null;
    }

    /**
     * 能否承接前面的复制声明。屏障、领域、护体、场地、条件释放和命中体积攻击都可以。
     * 动作、多重本身和复制声明不算。不按基础伤害是否大于 0 一刀切。
     */
    default boolean acceptsReplication() {
        if (multicastSpec() != null || replication() != null || role() == ChainRole.ACTION) {
            return false;
        }
        if (domain() != null || barrier() != null || guard() != null || fieldRadius() > 0f
                || conditionalEffect() != null) {
            return true;
        }
        return kind() == ReleaseKind.HIT_VOLUME && role() == ChainRole.ATTACK;
    }

    /**
     * 结构计划缓存资格。默认 false：未知实现不进入缓存，整条链回退原路径。
     * <p>
     * 显式返回 true 才承担契约。结构解析所读的 getter 必须是无副作用的纯配置读取，
     * 与世界、玩家和时钟无关。分类或结构配置在变化完成之后必须调用
     * {@link com.bslx.XueXiMod.shu.technique.ChainStructureEpoch#changed()}。
     * getter 返回的行为对象仍只在运行期使用，不能放入共享计划。
     */
    default boolean supportsStructuralPlanCache() {
        return false;
    }

    /**
     * 本批新报价。默认关闭。新附魔或新规则返回真后，整组改走新费用，不按编号分支。
     * 法杖 32 槽本身不调用这里。
     */
    default boolean declaresBatchQuote() {
        return false;
    }

    /** 抛物线或喷涌的运动参数。没有声明时，这两种弹道用批准的首版常数，不退回直线。 */
    default com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile motionProfile() {
        return com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile.NONE;
    }

    /** 领域等宿主要显式接受地表锚点，否则含地表定位的新链在编译期拒绝。 */
    default boolean acceptsSurfaceAnchor() {
        return false;
    }

    /** 领域、条件等要显式接受命中接续，否则该组合在编译期拒绝。 */
    default boolean acceptsHitContinue() {
        return false;
    }

    /** 领域、条件、屏障、护体、场地要显式接受后才能进入本批新链。 */
    default boolean acceptsBatchPlan() {
        return false;
    }

    /** 为真时，这条命中体在同一扫掠上比较最早的实体和地形。默认仍走原来的命中盒。 */
    default boolean prefersEarliestContact() {
        return false;
    }

    /** 宿主自己的新产种基数。火苗是 2。旧点火、光束和火球不写在这里。 */
    default int bodyScaledSeedBase() {
        return 0;
    }

    /**
     * 把下一段攻击收成自己的载荷。默认没有这种能力，旧链继续走原来的编译。
     */
    default com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureContract actorCapture() {
        return com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureContract.NONE;
    }
}
