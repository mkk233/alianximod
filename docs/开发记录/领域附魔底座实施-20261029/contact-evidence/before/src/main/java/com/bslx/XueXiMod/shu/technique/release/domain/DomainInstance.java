package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** 一个已经落下的领域。几何固定，供给不写成玩家身上的永久状态。 */
public final class DomainInstance implements ContactCarrier, DomainBound {
    private final UUID instanceId = UUID.randomUUID();
    private final UUID ownerId;
    private final ServerLevel level;
    private final DomainGeometry geometry;
    private final DomainDefinition definition;
    private final int revision = 1;
    private final int startedAt;
    private final int expiresAt;
    private final long visualOrigin;
    private final long visualEnd;
    private final int animSeed;
    private int nextUpkeepAt;
    private boolean active = true;
    private ContactEnchantState contact;

    DomainInstance(UUID ownerId, ServerLevel level, DomainGeometry geometry, DomainDefinition definition, int startedAt,
                   int scaledLife) {
        this.ownerId = ownerId;
        this.level = level;
        this.geometry = geometry;
        this.definition = definition;
        this.startedAt = startedAt;
        this.visualOrigin = level.getGameTime();
        FrozenClock clock = FrozenClock.freeze(startedAt, this.visualOrigin, scaledLife);
        this.expiresAt = clock.expiresAt;
        this.visualEnd = clock.visualEnd;
        this.animSeed = Math.floorMod(instanceId.hashCode(), 16);
        this.nextUpkeepAt = startedAt + definition.upkeepPeriod();
    }

    /** 同一份已缩放寿命同时写下服务器到期刻和视觉结束时刻。非正寿命不进到期助手。 */
    public static final class FrozenClock {
        public final int expiresAt;
        public final long visualEnd;

        private FrozenClock(int expiresAt, long visualEnd) {
            this.expiresAt = expiresAt;
            this.visualEnd = visualEnd;
        }

        public static FrozenClock freeze(int startedAt, long origin, int scaledLife) {
            if (scaledLife <= 0) {
                return new FrozenClock(startedAt + scaledLife, origin + scaledLife);
            }
            return new FrozenClock(
                    ReleaseInput.expiryTick(startedAt, scaledLife),
                    ReleaseInput.expiryGameTime(origin, scaledLife));
        }
    }

    void bindContact(EnchantmentSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.contact = ContactEnchantState.open(this, snapshot);
    }

    void close() {
        if (!active) {
            return;
        }
        active = false;
        if (contact != null) {
            contact.close();
            contact = null;
        }
    }

    public UUID instanceId() {
        return instanceId;
    }

    public DomainGeometry geometry() {
        return geometry;
    }

    public DomainDefinition definition() {
        return definition;
    }

    public int startedAt() {
        return startedAt;
    }

    public int expiresAt() {
        return expiresAt;
    }

    public int nextUpkeepAt() {
        return nextUpkeepAt;
    }

    void setNextUpkeepAt(int tick) {
        this.nextUpkeepAt = tick;
    }

    public float auraDamage() {
        return definition.auraSpec() == null ? 0f : definition.auraSpec().bonusDamage();
    }

    @Override
    public UUID hostId() {
        return instanceId;
    }

    @Override
    public int revision() {
        return revision;
    }

    @Override
    public DomainBoundary boundary() {
        return definition.boundary();
    }

    public DomainVisual visual() {
        return definition.visual();
    }

    public long visualOrigin() {
        return visualOrigin;
    }

    public long visualEnd() {
        return visualEnd;
    }

    public int animSeed() {
        return animSeed;
    }

    @Override
    public UUID ownerId() {
        return ownerId;
    }

    @Override
    public boolean active() {
        return active;
    }

    @Override
    public boolean touches(LivingEntity target) {
        return target != null && geometry.intersects(target.getBoundingBox());
    }

    @Override
    public ServerLevel hostLevel() {
        return level;
    }

    @Override
    public AABB queryBox() {
        return geometry.bounds().inflate(1.0);
    }
}
