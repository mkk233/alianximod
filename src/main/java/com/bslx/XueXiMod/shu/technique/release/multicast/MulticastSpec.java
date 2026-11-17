package com.bslx.XueXiMod.shu.technique.release.multicast;

/**
 * 一层多重要直接读取的格数。固定正整数，或从当前游标读到链尾。
 * 不含玩家、报价、目标或随机结果。
 */
public record MulticastSpec(Kind kind, int fixedCount) {
    public enum Kind {
        FIXED,
        REMAINING
    }

    public MulticastSpec {
        if (kind == null) {
            throw new IllegalArgumentException("多重读取种类不能为空");
        }
        if (kind == Kind.FIXED && fixedCount < 1) {
            throw new IllegalArgumentException("固定多重次数必须是正整数");
        }
        if (kind == Kind.REMAINING) {
            fixedCount = 0;
        }
    }

    public static MulticastSpec fixed(int fixedCount) {
        return new MulticastSpec(Kind.FIXED, fixedCount);
    }

    public static MulticastSpec remaining() {
        return new MulticastSpec(Kind.REMAINING, 0);
    }

    public int fixedCount() {
        if (kind != Kind.FIXED) {
            throw new IllegalStateException("读到链尾的多重没有固定次数");
        }
        return fixedCount;
    }
}
