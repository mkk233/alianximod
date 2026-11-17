package com.bslx.XueXiMod.shu.technique.release.entity;

import net.minecraft.server.level.ServerPlayer;

/** 宿主组上的拍号钩子。公共提交不认识具体术式，由注册方装上自己的规则。 */
public final class HostBeatHooks {
    public interface Cycle {
        void beforeCopies(ServerPlayer player, HostGroupContext context);

        void beforeActivation(ServerPlayer player, HostGroupContext context);

        void onCommitted(ServerPlayer player, HostGroupContext context, HostGroupReceipt receipt);

        void onAbandoned(HostGroupContext context);
    }

    private static volatile Cycle cycle;

    private HostBeatHooks() {
    }

    /** 装上拍号规则。后一次安装替换前一次。 */
    public static void install(Cycle installed) {
        cycle = installed;
    }

    /** 复制份生成前交给已安装规则。没安装则跳过。 */
    public static void beforeCopies(ServerPlayer player, HostGroupContext context) {
        Cycle current = cycle;
        if (current != null) {
            current.beforeCopies(player, context);
        }
    }

    /** 激活前交给已安装规则。没安装则跳过。 */
    public static void beforeActivation(ServerPlayer player, HostGroupContext context) {
        Cycle current = cycle;
        if (current != null) {
            current.beforeActivation(player, context);
        }
    }

    /** 整组提交成功后交给已安装规则。 */
    public static void onCommitted(ServerPlayer player, HostGroupContext context, HostGroupReceipt receipt) {
        Cycle current = cycle;
        if (current != null) {
            current.onCommitted(player, context, receipt);
        }
    }

    /** 整组没提交时交给已安装规则。 */
    public static void onAbandoned(HostGroupContext context) {
        Cycle current = cycle;
        if (current != null) {
            current.onAbandoned(context);
        }
    }
}
