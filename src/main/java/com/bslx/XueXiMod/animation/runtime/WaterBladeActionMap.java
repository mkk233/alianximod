package com.bslx.XueXiMod.animation.runtime;

import net.minecraft.resources.ResourceLocation;

/** 水刃三拍到动作 ID 的映射。播放器本身不认识拍号。 */
public final class WaterBladeActionMap {
    private static final ResourceLocation[] DEV_CANDIDATES = {
            ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto1"),
            ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto2"),
            ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto3")
    };
    private static final ResourceLocation[] FALLBACKS = {
            OriginalHumanoid.LEFT_ID,
            OriginalHumanoid.RIGHT_ID,
            OriginalHumanoid.HORIZONTAL_ID
    };
    private static final String[] BEAT_LABELS = {"左斜", "右斜", "水平"};

    private WaterBladeActionMap() {
    }

    public static Resolution resolve(int beat) {
        int beatIndex = Math.floorMod(beat, 3);
        ResourceLocation candidate = DEV_CANDIDATES[beatIndex];
        ActionStatus status = ActionRegistry.status(candidate);
        if (status != null && status.posePlayable()) {
            return new Resolution(candidate, true, beatIndex,
                    "水刃" + BEAT_LABELS[beatIndex] + "使用授权目录里的 " + candidate
                            + "。这是降阶或未验收的开发姿态，不是原创兜底，也没有改攻击几何。");
        }
        String blocked = status == null ? "开发目录未启用或清单里没有这条" : status.reasonText();
        return new Resolution(FALLBACKS[beatIndex], false, beatIndex,
                "水刃" + BEAT_LABELS[beatIndex] + "使用原创兜底 " + FALLBACKS[beatIndex]
                        + "。这不是上游动作。授权候选 " + candidate + " 不可用：" + blocked);
    }

    public static final class Resolution {
        private final ResourceLocation actionId;
        private final boolean authorizedDevClip;
        private final int beatIndex;
        private final String label;

        private Resolution(ResourceLocation actionId, boolean authorizedDevClip, int beatIndex, String label) {
            this.actionId = actionId;
            this.authorizedDevClip = authorizedDevClip;
            this.beatIndex = beatIndex;
            this.label = label;
        }

        public ResourceLocation actionId() {
            return this.actionId;
        }

        public boolean authorizedDevClip() {
            return this.authorizedDevClip;
        }

        public int beatIndex() {
            return this.beatIndex;
        }

        public String label() {
            return this.label;
        }
    }
}
