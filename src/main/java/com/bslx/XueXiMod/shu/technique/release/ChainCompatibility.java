package com.bslx.XueXiMod.shu.technique.release;

/**
 * 释放定义上的显式独立链兼容类型。
 * 普通术式之间仍按原来的组合规则拼接。其余类型只能和同一类型拼接，不能和普通或其他专属类型混用。
 * 类型必须由定义直接声明，不能从自身状态、窗口角色或空手标志推断。
 */
public enum ChainCompatibility {
    /** 原有普通组合。 */
    ORDINARY,
    /** 操纵术式，包括列剑、幻剑纵横和架剑这类剑术窗口。 */
    MANIPULATION,
    /** 真正提交自身状态的术式。 */
    SELF_STATE,
    /** 术法触发窗口。本次只预留类型，不实现窗口并存。 */
    SPELL_WINDOW
}
