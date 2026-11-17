package com.bslx.XueXiMod.shu.balance;

import com.bslx.XueXiMod.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端启动时装入并冻结世界快照。客户端只接收这份权威结果。
 */
public final class BalanceLifecycle {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BalanceLifecycle() {
    }

    /** 没有世界文件时从模板复制一份，再解析并绑定服务端。读失败时改用代码默认值，原文件不改。 */
    public static void loadWorld(MinecraftServer server) {
        try {
            BalanceDocuments.writeTemplateIfAbsent();
            Path worldFile = BalanceDocuments.worldPath(server);
            if (!Files.exists(worldFile)) {
                Files.createDirectories(worldFile.getParent());
                Files.copy(BalanceDocuments.templatePath(), worldFile);
            }
            String text = Files.readString(worldFile, StandardCharsets.UTF_8);
            installParsed(BalanceSide.SERVER, BalanceDocuments.parse(text));
            BalanceService.bind(BalanceSide.SERVER);
        } catch (Exception failure) {
            LOGGER.error("平衡配置无法读取，改用代码默认值。世界文件保持原样。", failure);
            installCodeDefaults(BalanceSide.SERVER);
            BalanceService.bind(BalanceSide.SERVER);
        }
    }

    /** 清掉服务端快照；当前线程还绑在服务端时一并解开。 */
    public static void unloadWorld() {
        BalanceService.clear(BalanceSide.SERVER);
        if (BalanceService.boundSide() == BalanceSide.SERVER) {
            BalanceService.unbind();
        }
    }

    /** 把服务端已经收下的覆盖值发给这位玩家。 */
    public static void sendTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        BalanceSnapshot snapshot = BalanceService.view(BalanceSide.SERVER);
        ModNetwork.sendToPlayer(player, new BalanceSyncPacket(BalanceDocuments.encodeAccepted(snapshot)));
    }

    /** 解析服务端发来的文本并绑定客户端。解析失败时改用代码默认值。 */
    public static void applyClientPayload(byte[] payload) {
        try {
            String text = new String(payload == null ? new byte[0] : payload, StandardCharsets.UTF_8);
            installParsed(BalanceSide.CLIENT, BalanceDocuments.parse(text));
            BalanceService.bind(BalanceSide.CLIENT);
        } catch (RuntimeException failure) {
            LOGGER.error("平衡同步无法应用，客户端改用代码默认值", failure);
            installCodeDefaults(BalanceSide.CLIENT);
            BalanceService.bind(BalanceSide.CLIENT);
        }
    }

    private static void installParsed(BalanceSide side, BalanceDocuments.Parsed parsed) {
        List<BalanceWarning> warnings = new ArrayList<>(parsed.warnings);
        BalanceSnapshot snapshot = BalanceSnapshot.resolve(parsed.globalOverrides, parsed.techniqueOverrides, warnings);
        for (BalanceWarning warning : warnings) {
            LOGGER.warn("平衡配置：{}", warning);
        }
        BalanceService.install(side, snapshot);
    }

    private static void installCodeDefaults(BalanceSide side) {
        BalanceService.install(side, BalanceSnapshot.defaults());
    }
}
