package com.bslx.XueXiMod.shu.technique.release.cooperative.attack;

import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;

/**
 * 专项数值的服务端读取。未封存或键尚未声明时退回代码默认。
 * 封存后只读 {@link BalanceSide#SERVER}，不跟着当前线程猜客户端快照。
 */
public final class ServerSpecialRead {
    private ServerSpecialRead() {
    }

    public static double number(String techniqueId, String parameterKey, double rawDefault) {
        if (!declared(techniqueId, parameterKey)) {
            return rawDefault;
        }
        return BalanceService.number(BalanceSide.SERVER, techniqueId, parameterKey);
    }

    public static int integer(String techniqueId, String parameterKey, int rawDefault) {
        if (!declared(techniqueId, parameterKey)) {
            return rawDefault;
        }
        return BalanceService.intValue(BalanceSide.SERVER, techniqueId, parameterKey);
    }

    private static boolean declared(String techniqueId, String parameterKey) {
        return BalanceRegistry.isSealed() && BalanceRegistry.isDeclared(techniqueId, parameterKey);
    }
}
