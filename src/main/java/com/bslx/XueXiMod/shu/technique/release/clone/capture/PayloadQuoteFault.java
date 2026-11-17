package com.bslx.XueXiMod.shu.technique.release.clone.capture;

/**
 * 冻结报价失败的稳定原因。调用方映射成捕获拒绝码，不在这里扣款。
 */
public enum PayloadQuoteFault {
    UNSUPPORTED,
    INVALID_QUOTE,
    GEOMETRY,
    ADAPTER_MISMATCH
}
