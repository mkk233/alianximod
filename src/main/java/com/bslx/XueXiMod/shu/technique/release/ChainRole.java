package com.bslx.XueXiMod.shu.technique.release;

/**
 * 底座类型。新术式只要登记成同一类，就走这一类的收集和结算。
 * 链编译读取它决定索敌、弹道、规则或攻击位置。它不保存目标、落点或费用。
 */
public enum ChainRole {
    ATTACK,
    ACTION,
    SEEK_SPACE,
    SEEK_ENTITY,
    TRAJECTORY_TRACKING,
    TRAJECTORY_DROP,
    TRAJECTORY_SCATTER,
    RULE_TRIPLE,
    RULE_DUAL,
    RULE_DOMAIN_BIND,
    /** 视线内第一个向上承托面。失败则整组不收费、不冷却。 */
    SEEK_SURFACE,
    /** 喷涌。与垂落、风暴不能出现在同一分支。 */
    TRAJECTORY_UPWELL,
    /** 平行弹道。只覆盖可改弹道，方向取施放朝向的水平投影。 */
    TRAJECTORY_PARALLEL,
    /** 第 i 份在分支基准时刻后再晚 10i 刻。不代替多重成员的 3 刻间隔。 */
    RULE_STAGGER,
    /** 单层父子接续。父段之后、子段之前。 */
    RULE_HIT_CONTINUE,
    /** 协同规则。本身不造成伤害，把下一段已适配攻击交给普通分身一起打。 */
    RULE_COOPERATIVE,
    /** 可执行宿主。开一段有限检测窗口，不修饰后面的弹道或攻击。 */
    EVENT_RESPONSE,
    /** 可执行宿主。操纵已经存在的弹丸，不改写尚未生成的下一段。 */
    TRAJECTORY_MANIPULATION,
    /** 附魔没有链角色。不参与攻击、弹道或规则解析。 */
    NOT_APPLICABLE;

    /** 返回该角色的界面语言键。不查找实际翻译文本。 */
    public String languageKey() {
        return "gui.xueximod.role." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 本批新链角色。出现即让整组走新报价，不按术式编号判断。 */
    public boolean batchLifecycle() {
        return this == SEEK_SURFACE
                || this == TRAJECTORY_UPWELL
                || this == RULE_STAGGER
                || this == RULE_HIT_CONTINUE;
    }

    /**
     * 改写后续尚未生成的弹道。
     * 不覆盖错时、命中接续、索敌、攻击和已经存在的弹丸操纵。
     */
    public boolean modifiesFollowingTrajectory() {
        return this == TRAJECTORY_UPWELL
                || this == TRAJECTORY_DROP
                || this == TRAJECTORY_PARALLEL
                || this == TRAJECTORY_TRACKING
                || this == TRAJECTORY_SCATTER;
    }
}
