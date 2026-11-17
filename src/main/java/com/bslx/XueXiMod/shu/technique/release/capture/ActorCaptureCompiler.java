package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadAdapter;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.List;

/**
 * 按释放定义上的捕获声明检查原始整链。不读主人、位置或费用，也不按术式编号分支。
 */
public final class ActorCaptureCompiler {
    private ActorCaptureCompiler() {
    }

    public static ActorCaptureInspection inspect(Skill skill) {
        if (skill == null || skill.getTechniques() == null || skill.getTechniques().isEmpty()) {
            return notApplicable();
        }
        List<Technique> techniques = skill.getTechniques();
        int captureCount = 0;
        int captureIndex = -1;
        ActorCaptureContract captureContract = null;
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            if (technique == null) {
                continue;
            }
            ActorCaptureContract contract = contractOf(technique);
            if (contract.mode() == ActorCaptureMode.NONE) {
                continue;
            }
            captureCount++;
            captureIndex = techniqueIndex;
            captureContract = contract;
        }
        if (captureCount == 0) {
            return notApplicable();
        }
        PerformanceRecorder.add(PerfCounter.CAPTURE_PLAN_COMPILES, 1L);
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (techniques.get(techniqueIndex) == null) {
                return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
            }
            if (skill.getEmbryo(techniqueIndex) != ShuAttribute.NONE) {
                return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_EMBRYO, ActorCaptureTexts.EMBRYO);
            }
        }
        if (captureCount != 1 || captureContract == null || captureIndex != 0
                || captureContract.mode() != ActorCaptureMode.NEXT_SINGLE_ATTACK) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        if (techniques.size() != 2) {
            if (techniques.size() < 2) {
                return refused(CaptureRefusal.CAPTURE_MISSING_PAYLOAD, ActorCaptureTexts.STRUCTURE);
            }
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        if (captureContract.maximumEntries() != 1 || captureContract.rejectNesting() && captureCount != 1) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        Technique host = techniques.get(0);
        Technique payload = techniques.get(1);
        if (host.isEnchantment() || payload.isEnchantment()
                || (captureContract.rejectModifiers() && (host.isEnchantment() || payload.isEnchantment()))) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        if (!captureContract.requireExplicitAdapter()) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD, ActorCaptureTexts.STRUCTURE);
        }
        ReleaseDefinition payloadDefinition = ReleaseRegistry.get(payload.getId());
        if (payloadDefinition == null || payloadDefinition.role() != captureContract.capturedRole()
                || payload.getAttribute() != captureContract.requiredAttribute()) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD, ActorCaptureTexts.STRUCTURE);
        }
        if (contractOf(payload).mode() != ActorCaptureMode.NONE) {
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN, ActorCaptureTexts.STRUCTURE);
        }
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(payload.getId());
        if (adapter == null || adapter.version() < 1 || adapter.attribute() != captureContract.requiredAttribute()
                || adapter.techniqueId() == null || adapter.techniqueId().isBlank()) {
            return refused(CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD, ActorCaptureTexts.STRUCTURE);
        }
        CapturedStructurePlan plan = new CapturedStructurePlan(
                0,
                List.of(1),
                List.of(0, 1),
                List.of(0, 1),
                2,
                payload.getId(),
                ShuAttribute.NONE,
                adapter.techniqueId(),
                adapter.version(),
                ChainStructureEpoch.current());
        return new ActorCaptureInspection(ActorCaptureStatus.PLAN, plan, null, "");
    }

    private static ActorCaptureContract contractOf(Technique technique) {
        if (technique == null || technique.getId() == null) {
            return ActorCaptureContract.NONE;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || definition.actorCapture() == null) {
            return ActorCaptureContract.NONE;
        }
        return definition.actorCapture();
    }

    private static ActorCaptureInspection notApplicable() {
        return new ActorCaptureInspection(ActorCaptureStatus.NOT_APPLICABLE, null, null, "");
    }

    private static ActorCaptureInspection refused(CaptureRefusal refusal, String playerText) {
        return new ActorCaptureInspection(ActorCaptureStatus.REFUSED, null, refusal, playerText);
    }
}
