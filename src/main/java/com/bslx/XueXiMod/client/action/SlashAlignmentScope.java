package com.bslx.XueXiMod.client.action;

/**
 * 挥斩对齐 L 的作用范围。
 * LEGACY 保持旧行为，L 乘进全身，六个部件一起转。
 * UPPER_BODY_SLASH 只接管躯干、头、持剑臂，腿不写；这条路径不使用 L。
 */
public enum SlashAlignmentScope {
    LEGACY,
    UPPER_BODY_SLASH
}
