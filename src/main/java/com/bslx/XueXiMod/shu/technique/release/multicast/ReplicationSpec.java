package com.bslx.XueXiMod.shu.technique.release.multicast;

/**
 * 一变多复制能力。因子是这一格让后续适用攻击变成的份数，至少为 2。
 * 解析器只认这个声明，不认术式编号。
 */
public record ReplicationSpec(int factor) {
    public ReplicationSpec {
        if (factor < 2) {
            throw new IllegalArgumentException("复制因子至少为 2");
        }
    }

    public static ReplicationSpec of(int factor) {
        return new ReplicationSpec(factor);
    }
}
