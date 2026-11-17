package com.bslx.XueXiMod.shu.technique.release.selfstatus;

/**
 * 自身状态的提交顺序。预检或二次校验不过，不扣款；扣款后安装没成功则退款；两样都成功才提交。
 * 三参数版本保持原来的预检、付款、安装结果顺序，供已有调用继续使用。
 */
public final class SelfStatusOrder {
    public enum Step {
        /** 第一次预检没过。 */
        PREVIEW_REJECT,
        /** 扣款前的再次确认没过。 */
        RECHECK_REJECT,
        /** 预检通过但没付成。 */
        UNPAID,
        /** 已付款但安装没成功，应退款。 */
        REFUND,
        /** 付款和安装都成功。 */
        COMMIT
    }

    private SelfStatusOrder() {
    }

    /** 按旧的三步顺序决定提交、拒绝或退款。 */
    public static Step decide(boolean previewReady, boolean paid, boolean furnaceReady) {
        if (!previewReady) {
            return Step.PREVIEW_REJECT;
        }
        if (!paid) {
            return Step.UNPAID;
        }
        if (!furnaceReady) {
            return Step.REFUND;
        }
        return Step.COMMIT;
    }

    /** 把扣款前复核插进旧顺序。安装没成功仍是退款。 */
    public static Step decide(boolean previewReady, boolean recheckReady, boolean paid, boolean furnaceReady) {
        if (!previewReady) {
            return Step.PREVIEW_REJECT;
        }
        if (!recheckReady) {
            return Step.RECHECK_REJECT;
        }
        if (!paid) {
            return Step.UNPAID;
        }
        if (!furnaceReady) {
            return Step.REFUND;
        }
        return Step.COMMIT;
    }
}
