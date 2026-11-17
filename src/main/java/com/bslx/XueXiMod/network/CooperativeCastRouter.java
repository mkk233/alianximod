package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCarrier;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCastBuild;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCastRecipe;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeRuntime;
import com.bslx.XueXiMod.shu.technique.release.cooperative.FamilyExecutionService;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentInput;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentResult;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DeploymentSource;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DeploymentStatus;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DomainLink;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareSlot;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.StandaloneDeploymentKind;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 玩家释放入口。没有本批能力时返回 null，调用方继续旧路径。
 */
public final class CooperativeCastRouter {
    private CooperativeCastRouter() {
    }

    public static @Nullable CastTiming route(ServerPlayer player, Skill skill, ItemStack weapon, int skillSlot,
                                             double cooldownReduction) {
        CastTiming standalone = routeStandalone(player, skill, weapon, skillSlot);
        if (standalone != null) {
            return standalone;
        }
        CooperativeCastBuild build = CooperativeChainCompiler.prepare(
                player, skill, weapon, skillSlot, cooldownReduction, false, null, null);
        if (build == null) {
            return null;
        }
        return openBuild(player, skill, weapon, skillSlot, build, true, null);
    }

    public static boolean launchPrepaid(ServerPlayer player, Skill skill, ItemStack weapon, Vec3 direction, Vec3 origin,
                                        @Nullable UUID domainInstanceId, @Nullable String domainTechniqueId,
                                        @Nullable GroupBudget fundedBudget) {
        DomainLink link = null;
        if (domainInstanceId != null && domainTechniqueId != null && player != null) {
            link = new DomainLink(domainInstanceId, domainTechniqueId, player.getUUID());
        }
        FamilyExecutionService.beginForcedAnchor(origin);
        try {
            CooperativeCastBuild build = CooperativeChainCompiler.prepare(
                    player, skill, weapon, 0, 0.0D, true, direction, link);
            if (build == null || fundedBudget == null || fundedBudget.receipt == null) {
                return false;
            }
            int quoted = quotedShares(build);
            if (quoted > fundedBudget.openShare) {
                return false;
            }
            CastTiming timing = openBuild(player, skill, weapon, 0, build, false, fundedBudget.receipt);
            if (timing != null && timing.accepted()) {
                fundedBudget.detach(quoted);
                return true;
            }
            return false;
        } finally {
            FamilyExecutionService.endForcedAnchor();
        }
    }

    public static List<com.bslx.XueXiMod.shu.technique.release.bind.BoundMember> stampDomain(
            Skill frozenSkill, List<com.bslx.XueXiMod.shu.technique.release.bind.BoundMember> members) {
        CooperativeCastRecipe recipe = CooperativeChainCompiler.describe(frozenSkill);
        if (recipe == null || members == null || members.isEmpty()) {
            return members;
        }
        boolean assigned = false;
        List<com.bslx.XueXiMod.shu.technique.release.bind.BoundMember> stamped = new java.util.ArrayList<>();
        for (com.bslx.XueXiMod.shu.technique.release.bind.BoundMember member : members) {
            if (!assigned) {
                stamped.add(member.withCooperative(recipe, false));
                assigned = true;
            } else {
                stamped.add(member.withCooperative(null, true));
            }
        }
        return List.copyOf(stamped);
    }

    private static int quotedShares(CooperativeCastBuild build) {
        int quoted = 0;
        if (build.plan() == null) {
            return 0;
        }
        for (ShareSlot slot : build.plan().shares().slots()) {
            quoted += Math.max(0, slot.amount());
        }
        return quoted;
    }

    private static CastTiming openBuild(ServerPlayer player, Skill skill, ItemStack weapon, int skillSlot,
                                        CooperativeCastBuild build, boolean armDash,
                                        @Nullable ManaPaymentReceipt fundedReceipt) {
        if (build.refusalKey() != null) {
            player.displayClientMessage(Component.translatable(build.refusalKey()), true);
            return CastTiming.rejected();
        }
        if (build.insufficientMana()) {
            return CastTiming.insufficient();
        }
        FamilyExecutionService.Opened opened = fundedReceipt == null
                ? CooperativeRuntime.service().open(player, weapon, skillSlot, build)
                : CooperativeRuntime.service().open(player, weapon, skillSlot, build, fundedReceipt);
        if (opened.insufficient()) {
            return CastTiming.insufficient();
        }
        if (opened.rejected()) {
            if (opened.refusalKey() != null) {
                player.displayClientMessage(Component.translatable(opened.refusalKey()), true);
            }
            return CastTiming.rejected();
        }
        if (armDash && build.recipe() != null && build.recipe().gateKind() == ReleaseGateKind.CONTACT && skill != null) {
            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(
                    skill, new ChainSegment(build.recipe().actionChainIndex(), List.of()));
            CooperativeRuntime.service().armDash(build.plan().family(), player, weapon, enchantments);
        }
        int nowTick = player.getServer().getTickCount();
        if (opened.deferred()) {
            return CastTiming.deferred(nowTick, opened.fired());
        }
        int replayTick = nowTick + Math.max(opened.cooldownTicks(), 0);
        return CastTiming.accepted(nowTick, nowTick, replayTick, opened.fired());
    }

    private static @Nullable CastTiming routeStandalone(ServerPlayer player, Skill skill, ItemStack weapon, int skillSlot) {
        List<Technique> techniques = skill.getTechniques();
        boolean containsBody = false;
        for (Technique technique : techniques) {
            if (technique != null && CooperativeTechniqueIds.CLONE_BODY.equals(technique.getId())) {
                containsBody = true;
                break;
            }
        }
        if (!containsBody) {
            return null;
        }
        if (techniques.size() != 1) {
            player.displayClientMessage(Component.translatable(CooperativeChainCompiler.CLONE_MIXED_KEY), true);
            return CastTiming.rejected();
        }
        boolean staffSlot = StaffItem.holds(weapon) && skillSlot == 0;
        if (!staffSlot) {
            player.displayClientMessage(Component.translatable(CooperativeChainCompiler.CLONE_SLOT_KEY), true);
            return CastTiming.rejected();
        }
        Technique body = TechniqueManager.getById(CooperativeTechniqueIds.CLONE_BODY);
        if (player.isShiftKeyDown()) {
            CooperativeRuntime.deployment().dismissStandalone(player, StandaloneDeploymentKind.NORMAL);
            int nowTick = player.getServer().getTickCount();
            return CastTiming.accepted(nowTick, nowTick, nowTick, body == null ? List.of() : List.of(body));
        }
        com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenCarrierCredential credential =
                CooperativeCarrier.freeze(player, skillSlot);
        if (!credential.matches(CooperativeCarrier.current(player, skillSlot))) {
            player.displayClientMessage(Component.translatable(CooperativeChainCompiler.CLONE_SLOT_KEY), true);
            return CastTiming.rejected();
        }
        CloneBalance.CloneSpawnQuote quote = CloneBalance.readSpawn(
                com.bslx.XueXiMod.shu.balance.BalanceSide.SERVER, CloneKind.NORMAL);
        CloneDeploymentResult result = CooperativeRuntime.deployment().deployStandalone(
                player, new CloneDeploymentInput(StandaloneDeploymentKind.NORMAL, quote, credential, DeploymentSource.PLAYER_STANDALONE));
        if (result.status() != DeploymentStatus.SUCCESS) {
            if (result.status() == DeploymentStatus.PAYMENT_FAILED) {
                return CastTiming.insufficient();
            }
            player.displayClientMessage(Component.translatable(refusalOf(result)), true);
            return CastTiming.rejected();
        }
        int nowTick = player.getServer().getTickCount();
        int replayTick = nowTick + Math.max(quote.cooldownTicks(), 1);
        return CastTiming.accepted(nowTick, nowTick, replayTick, body == null ? List.of() : List.of(body));
    }

    private static String refusalOf(CloneDeploymentResult result) {
        if (result.refusalKey() != null && result.refusalKey().contains(".")) {
            return result.refusalKey();
        }
        return CooperativeChainCompiler.CLONE_SLOT_KEY;
    }
}
