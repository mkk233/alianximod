package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadAdapter;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadRegistry;
import com.bslx.XueXiMod.skill.Skill;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 法杖载体的纯检查。编辑可以留下草稿；施放必须是法杖上的完整计划。
 * 类型例外只覆盖就绪计划里已经消费的载荷下标。
 */
public final class ActorCaptureCarrierGate {
    private ActorCaptureCarrierGate() {
    }

    public static CaptureCarrierCheck inspect(Skill candidate, boolean staffCarrier, boolean permitIncompleteDraft) {
        if (candidate == null || candidate.getTechniques() == null || candidate.getTechniques().isEmpty()) {
            return notApplicable();
        }
        ActorCaptureInspection inspection = ActorCaptureCompiler.inspect(candidate);
        if (inspection.status() == ActorCaptureStatus.NOT_APPLICABLE) {
            return notApplicable();
        }
        if (inspection.status() == ActorCaptureStatus.PLAN && inspection.plan() != null) {
            return readyOrRefuse(candidate, inspection.plan(), staffCarrier);
        }
        if (permitIncompleteDraft && singleLegalHost(candidate)) {
            if (!staffCarrier) {
                return refused(CaptureRefusal.CAPTURE_REQUIRES_STAFF, ActorCaptureTexts.STAFF_ONLY);
            }
            return new CaptureCarrierCheck(CaptureCarrierDecision.DRAFT, null,
                    CaptureRefusal.CAPTURE_MISSING_PAYLOAD, ActorCaptureTexts.MISSING_NEXT);
        }
        if (declaresCapture(candidate) && !staffCarrier) {
            return refused(CaptureRefusal.CAPTURE_REQUIRES_STAFF, ActorCaptureTexts.STAFF_ONLY);
        }
        if (inspection.refusal() == null) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        return refused(inspection.refusal(), inspection.playerText());
    }

    public static boolean permitsCapturedType(CaptureCarrierCheck checked, int originalIndex, Technique technique) {
        if (checked == null || technique == null || technique.getId() == null
                || checked.decision() != CaptureCarrierDecision.READY || checked.plan() == null) {
            return false;
        }
        CapturedStructurePlan plan = checked.plan();
        if (originalIndex < 0 || !plan.payloadOriginalIndexes().contains(originalIndex)
                || !plan.consumedOriginalIndexes().contains(originalIndex)) {
            return false;
        }
        if (!technique.getId().equals(plan.payloadTechniqueId())) {
            return false;
        }
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(technique.getId());
        if (adapter == null || adapter.version() != plan.adapterVersion()
                || adapter.techniqueId() == null || !adapter.techniqueId().equals(plan.adapterKey())
                || technique.getAttribute() != adapter.attribute()) {
            return false;
        }
        ReleaseDefinition payloadDefinition = ReleaseRegistry.get(technique.getId());
        if (payloadDefinition == null || payloadDefinition.role() != ChainRole.ATTACK) {
            return false;
        }
        if (!TechniqueAccess.allowsPlayerUse(technique)) {
            return false;
        }
        ActorCaptureContract hostContract = soleCaptureContract(adapter.attribute());
        return hostContract != null && hostContract.carrierMode() == CaptureCarrierMode.STAFF_ONLY
                && hostContract.capturedTypeExceptions().contains(technique.getType());
    }

    private static CaptureCarrierCheck readyOrRefuse(Skill candidate, CapturedStructurePlan plan, boolean staffCarrier) {
        List<Technique> techniques = candidate.getTechniques();
        if (plan.captureOriginalIndex() != 0 || plan.payloadOriginalIndexes().size() != 1
                || techniques.size() != 2) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        Technique host = techniques.get(0);
        int payloadIndex = plan.payloadOriginalIndexes().get(0);
        if (payloadIndex < 0 || payloadIndex >= techniques.size()) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD, ActorCaptureTexts.STRUCTURE);
        }
        Technique payload = techniques.get(payloadIndex);
        ReleaseDefinition hostDefinition = host == null ? null : ReleaseRegistry.get(host.getId());
        if (host == null || payload == null || hostDefinition == null || hostDefinition.actorCapture() == null) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        ActorCaptureContract declared = hostDefinition.actorCapture();
        if (declared.mode() != ActorCaptureMode.NEXT_SINGLE_ATTACK) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        if (declared.carrierMode() == CaptureCarrierMode.STAFF_ONLY && !staffCarrier) {
            return refused(CaptureRefusal.CAPTURE_REQUIRES_STAFF, ActorCaptureTexts.STAFF_ONLY);
        }
        if (!TechniqueAccess.allowsPlayerUse(host) || !TechniqueAccess.allowsPlayerUse(payload)) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD, TechniqueAccess.DENIAL_TEXT);
        }
        if (candidate.getEmbryo(0) != ShuAttribute.NONE || candidate.getEmbryo(payloadIndex) != ShuAttribute.NONE) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_EMBRYO, ActorCaptureTexts.EMBRYO);
        }
        ActorCaptureContract sole = soleCaptureContract(declared.requiredAttribute());
        if (sole == null || sole.capturedTypeExceptions() == null
                || !sole.capturedTypeExceptions().equals(declared.capturedTypeExceptions())) {
            return refused(CaptureRefusal.CAPTURE_INVALID_CARRIER_SLOT, ActorCaptureTexts.CARRIER_SLOT);
        }
        if (payload.getType() == TechniqueType.SWORD
                && !declared.capturedTypeExceptions().contains(payload.getType())) {
            return refused(CaptureRefusal.CAPTURE_INVALID_CARRIER_SLOT, ActorCaptureTexts.CARRIER_SLOT);
        }
        CaptureCarrierCheck ready = new CaptureCarrierCheck(CaptureCarrierDecision.READY, plan, null, "");
        if (payload.getType() == TechniqueType.SWORD && !permitsCapturedType(ready, payloadIndex, payload)) {
            return refused(CaptureRefusal.CAPTURE_INVALID_CARRIER_SLOT, ActorCaptureTexts.CARRIER_SLOT);
        }
        return ready;
    }

    private static boolean singleLegalHost(Skill skill) {
        List<Technique> techniques = skill.getTechniques();
        if (techniques.size() != 1) {
            return false;
        }
        Technique host = techniques.get(0);
        if (host == null || host.isEnchantment() || skill.getEmbryo(0) != ShuAttribute.NONE) {
            return false;
        }
        ActorCaptureContract contract = contractOf(host);
        return contract.mode() == ActorCaptureMode.NEXT_SINGLE_ATTACK
                && contract.carrierMode() == CaptureCarrierMode.STAFF_ONLY
                && contract.maximumEntries() == 1
                && contract.requiredAttribute() != null;
    }

    private static boolean declaresCapture(Skill skill) {
        for (Technique technique : skill.getTechniques()) {
            if (technique != null && contractOf(technique).mode() != ActorCaptureMode.NONE) {
                return true;
            }
        }
        return false;
    }

    private static ActorCaptureContract contractOf(Technique technique) {
        ReleaseDefinition definition = technique == null ? null : ReleaseRegistry.get(technique.getId());
        if (definition == null || definition.actorCapture() == null) {
            return ActorCaptureContract.NONE;
        }
        return definition.actorCapture();
    }

    private static @Nullable ActorCaptureContract soleCaptureContract(ShuAttribute requiredAttribute) {
        if (requiredAttribute == null) {
            return null;
        }
        ActorCaptureContract found = null;
        for (Technique candidate : TechniqueManager.getAll()) {
            if (candidate == null) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(candidate.getId());
            if (definition == null || definition.actorCapture() == null) {
                continue;
            }
            ActorCaptureContract contract = definition.actorCapture();
            if (contract.mode() != ActorCaptureMode.NEXT_SINGLE_ATTACK
                    || contract.carrierMode() != CaptureCarrierMode.STAFF_ONLY
                    || contract.requiredAttribute() != requiredAttribute) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = contract;
        }
        return found;
    }

    private static CaptureCarrierCheck notApplicable() {
        return new CaptureCarrierCheck(CaptureCarrierDecision.NOT_APPLICABLE, null, null, "");
    }

    private static CaptureCarrierCheck refused(CaptureRefusal refusal, String playerText) {
        String shown = playerText == null || playerText.isBlank() ? ActorCaptureTexts.STRUCTURE : playerText;
        return new CaptureCarrierCheck(CaptureCarrierDecision.REFUSED, null, refusal, shown);
    }
}
