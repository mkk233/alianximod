package com.bslx.XueXiMod.shu.compute;

/**
 * 作者 B 要实现的 JNI 声明。库文件名固定为 xueximod_compute，由绝对路径加载，不走库搜索路径。
 * 类名故意没有下划线，JNI 导出名因此不含 _1 转义。
 */
final class XuexiComputeNative {
    private XuexiComputeNative() {
    }

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake */
    static native int handshake(int[] sink);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch */
    static native int trajectoryBatch(int count, int[] header, double[] input, double[] output);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch */
    static native int turnBatch(int count, int[] header, double[] input, double[] output);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch */
    static native int trackingStepBatch(int count, int[] header, double[] input, double[] output);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch */
    static native int corridorOverlapsBatch(int count, double[] input, int[] output);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch */
    static native int corridorContactBatch(int count, double[] input, double[] output);

    /** Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch */
    static native int corridorBlockedBatch(int count, double[] input, int[] output);
}
