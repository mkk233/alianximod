package com.bslx.XueXiMod.shu.technique.release.domain;

/**
 * 幽冥染色读取的真实范围。灼地给出当前贴地切片，不改检测几何。
 */
public interface GloomVolumes {
    /**
     * 每片六个坐标：低 X、低 Y、低 Z、高 X、高 Y、高 Z。
     * 目标数组为空时只返回片数。放不下时不写入，仍返回片数。
     */
    int exportVolumes(double[] destination);
}
