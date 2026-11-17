package com.bslx.XueXiMod.shu.balance;

/**
 * 平衡 JSON 的结构版本。本阶段不读文件；版本不符时由后续加载方保留原文件并改用代码默认快照。
 */
public final class BalanceSchema {
    public static final int CURRENT_VERSION = 1;

    private BalanceSchema() {
    }

    /** 只接受当前结构版本。 */
    public static void requireSupported(int schemaVersion) {
        if (schemaVersion != CURRENT_VERSION) {
            throw new BalanceDocumentException(
                    "未知平衡配置 schemaVersion=" + schemaVersion
                            + "，本次应使用代码默认快照，并且不得改写原文件");
        }
    }
}
