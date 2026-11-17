package com.bslx.XueXiMod.shu.technique.release.parting;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 一次分水操作的身份。复制序号不进入这个编号，一分为三因此共享同一次扣费。
 */
public record PartingOperationId(UUID operationKey) {
    public static PartingOperationId derive(UUID playerId, String batchMark, String techniqueId, int actionIndex, int memberIndex) {
        String material = playerId + "|" + batchMark + "|" + techniqueId + "|" + actionIndex + "|" + memberIndex;
        return new PartingOperationId(UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)));
    }
}
