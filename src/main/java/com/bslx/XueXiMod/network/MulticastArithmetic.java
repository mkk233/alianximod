package com.bslx.XueXiMod.network;

/**
 * 多重报价的加减乘。超出 int 就返回空，调用方拒绝这次释放。
 * 不把结果截成最大整数，也不把溢出后的负数当成免费。
 */
final class MulticastArithmetic {
    static final String OVERFLOW_REFUSAL = "这次多重的份数或费用超出可表示范围，没有释放";

    private MulticastArithmetic() {
    }

    static Integer add(int left, int right) {
        long sum = (long) left + right;
        if (sum > Integer.MAX_VALUE || sum < 0L) {
            return null;
        }
        return (int) sum;
    }

    static Integer multiply(int left, int right) {
        if (left < 0 || right < 0) {
            return null;
        }
        if (left == 0 || right == 0) {
            return 0;
        }
        if (right != 0 && left > Integer.MAX_VALUE / right) {
            return null;
        }
        return left * right;
    }
}
