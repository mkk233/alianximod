package com.bslx.XueXiMod.status;

/** 一种可由术式挂上的属性状态。精确数值和药水档位分开存。 */
public final class AttributeStateDefinition {
    private final String identifier;
    private final int upperBound;

    public AttributeStateDefinition(String identifier, int upperBound) {
        this.identifier = identifier;
        this.upperBound = upperBound;
    }

    public String identifier() {
        return this.identifier;
    }

    public int upperBound() {
        return this.upperBound;
    }
}
