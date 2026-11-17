package com.bslx.XueXiMod.shu.technique.release.bind;

/** 结构捕获的一条失败。下标是原始装配链上的位置。 */
public record StructureFault(int index, String message) {
}
