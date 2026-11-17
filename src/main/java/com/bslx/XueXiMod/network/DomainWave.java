package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainCastContext;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 领域入口。有幽冥时带上真实载荷和同一本根账；没有就不改旧的开域。 */
final class DomainWave {
    private DomainWave() {
    }

    static Held hold(ServerPlayer player, Skill skill, Technique source, List<Integer> enchantIndexes) {
        List<Integer> covered = coverSource(skill, source, enchantIndexes);
        EnchantBonus bonus = EnchantBonus.resolve(skill, covered, source == null ? 0f : source.getBaseDamage(), 1);
        ResourceEnchantLoad load = bonus.resourceLoad == null ? ResourceEnchantLoad.EMPTY : bonus.resourceLoad;
        UUID rootCastId = null;
        boolean braced = false;
        if (load.gloom()) {
            rootCastId = BatchCastSession.currentRootId(player);
            if (rootCastId != null) {
                RootCastBindings.retain(rootCastId);
                braced = true;
            }
        }
        return new Held(load, rootCastId, braced);
    }

    /** 分段上的附魔下标加上这个动作自己。单独幽冥没有前置下标，载荷仍要认出它。 */
    private static List<Integer> coverSource(Skill skill, Technique source, List<Integer> enchantIndexes) {
        ArrayList<Integer> covered = new ArrayList<>();
        if (enchantIndexes != null) {
            covered.addAll(enchantIndexes);
        }
        if (skill == null || source == null || skill.getTechniques() == null) {
            return covered;
        }
        int matched = indexOf(skill.getTechniques(), source);
        if (matched >= 0 && !covered.contains(matched)) {
            covered.add(matched);
        }
        return covered;
    }

    private static int indexOf(List<Technique> techniques, Technique source) {
        int cursor = 0;
        while (cursor < techniques.size()) {
            if (techniques.get(cursor) == source) {
                return cursor;
            }
            cursor++;
        }
        String sourceId = source.getId();
        if (sourceId == null) {
            return -1;
        }
        cursor = 0;
        while (cursor < techniques.size()) {
            Technique listed = techniques.get(cursor);
            if (listed != null && sourceId.equals(listed.getId())) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    static void release(Held held) {
        if (held != null && held.braced && held.rootCastId != null) {
            RootCastBindings.release(held.rootCastId);
            held.braced = false;
        }
    }

    static boolean open(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                        EnchantmentSnapshot snapshot, ReleaseInput input,
                        @Nullable BoundReleasePlan plan, @Nullable String claim, Held held,
                        @Nullable DomainCastContext castContext) {
        if (held != null && held.load.gloom()) {
            return DomainService.tryWaveLoaded(player, source, definition, copies, snapshot, input, plan, claim,
                    held.load, held.rootCastId, castContext);
        }
        return DomainService.tryWaveLoaded(player, source, definition, copies, snapshot, input, plan, claim,
                null, null, castContext);
    }

    static boolean openReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                EnchantmentSnapshot snapshot, Vec3 feet, String claim, ReleaseInput input,
                                @Nullable BoundReleasePlan plan, Held held, @Nullable DomainCastContext castContext) {
        if (held != null && held.load.gloom()) {
            return DomainService.tryWaveReservedLoaded(player, source, definition, copies, snapshot, feet, claim,
                    input, plan, held.load, held.rootCastId, castContext);
        }
        return DomainService.tryWaveReservedLoaded(player, source, definition, copies, snapshot, feet, claim,
                input, plan, null, null, castContext);
    }

    static final class Held {
        final ResourceEnchantLoad load;
        final UUID rootCastId;
        boolean braced;

        private Held(ResourceEnchantLoad load, UUID rootCastId, boolean braced) {
            this.load = load;
            this.rootCastId = rootCastId;
            this.braced = braced;
        }
    }
}
