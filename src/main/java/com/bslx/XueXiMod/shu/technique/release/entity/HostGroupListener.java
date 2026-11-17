package com.bslx.XueXiMod.shu.technique.release.entity;

/**
 * 宿主组整组提交成功后通知一次。
 * 后续拍号和姿态从这里接。这一批不声明动画接口，也不表示动画已经完成。
 */
public interface HostGroupListener {
    void onCommitted(HostGroupReceipt receipt);
}
