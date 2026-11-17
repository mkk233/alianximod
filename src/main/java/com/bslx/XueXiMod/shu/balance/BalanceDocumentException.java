package com.bslx.XueXiMod.shu.balance;

/**
 * 配置文档本身不可采用，例如未知的 schema 版本。调用方应保留原文件并安装代码默认快照。
 */
public final class BalanceDocumentException extends BalanceException {
    public BalanceDocumentException(String detail) {
        super(detail);
    }
}
