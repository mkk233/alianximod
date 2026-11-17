package com.bslx.XueXiMod.shu.technique.release.parting;

/**
 * 通用释放入口放进来的操作身份。只有显式读取它的效果才会分水。
 */
public final class PartingOffers {
    private static final ThreadLocal<PartingOperationId> CURRENT = new ThreadLocal<>();

    private PartingOffers() {
    }

    public static void present(PartingOperationId operationId) {
        if (operationId == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(operationId);
    }

    public static PartingOperationId current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
