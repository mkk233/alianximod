package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 一次领域成员的空间结果。接触点优先于空间落点，空间落点优先于脚底。
 * 锁定实体不改生成点。垂落不把竖轴体积放倒。
 * 这是不可变落点状态；球形场地锚在身体中点，护体不使用这份空间结果。
 */
public final class DomainCastContext {
    private final UUID groupId;
    private final int declarationSlot;
    private final boolean spaceLock;
    private final boolean scatter;
    private final boolean entityLock;
    private final boolean dropPath;
    private final boolean explicitContact;
    private final @Nullable Vec3 contactPoint;

    /** 冻结这一发的分组、槽位和落点选择。空组用槽位补标识，不扣资源。 */
    public DomainCastContext(UUID groupId, int declarationSlot, boolean spaceLock, boolean scatter,
                             boolean entityLock, boolean dropPath, boolean explicitContact,
                             @Nullable Vec3 contactPoint) {
        this.groupId = groupId == null ? new UUID(0L, declarationSlot) : groupId;
        this.declarationSlot = declarationSlot;
        this.spaceLock = spaceLock;
        this.scatter = scatter;
        this.entityLock = entityLock;
        this.dropPath = dropPath;
        this.explicitContact = explicitContact;
        this.contactPoint = contactPoint;
    }

    public UUID groupId() {
        return groupId;
    }

    public int declarationSlot() {
        return declarationSlot;
    }

    public boolean scatter() {
        return scatter;
    }

    /** 竖轴体积始终直立。垂落标记只说明这次没有把柱体放倒。 */
    public boolean verticalAxis() {
        return !dropPath || dropPath;
    }

    /** 选择这一发原点。显式接触点优先；没有可用点时退回玩家位置，不扣资源。 */
    public Vec3 origin(ServerPlayer player) {
        if (explicitContact && contactPoint != null) {
            return contactPoint;
        }
        if (entityLock && !spaceLock) {
            if (contactPoint != null) {
                return contactPoint;
            }
            return player.position();
        }
        if (spaceLock) {
            return ReleaseBatch.sharedLanding(player, true);
        }
        if (contactPoint != null) {
            return contactPoint;
        }
        return player.position();
    }
}
