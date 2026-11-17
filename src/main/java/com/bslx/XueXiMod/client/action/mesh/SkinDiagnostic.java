package com.bslx.XueXiMod.client.action.mesh;

/** 单份网格或一次蒙皮失败。调用方应关掉该 skin 后端，不要让它打断动作 runtime。 */
public final class SkinDiagnostic extends IllegalArgumentException {
    private final String reasonCode;

    public SkinDiagnostic(String reasonCode, String message) {
        super(message == null ? "skin 数据不可用" : message);
        this.reasonCode = reasonCode == null || reasonCode.isBlank() ? "skin" : reasonCode;
    }

    public String reasonCode() {
        return this.reasonCode;
    }
}
