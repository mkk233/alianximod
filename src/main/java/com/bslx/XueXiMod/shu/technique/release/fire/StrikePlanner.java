package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.Optional;

/** 引爆只用一个分量。炉心仍由消费快照合入一次。 */
@FunctionalInterface
public interface StrikePlanner {
    Optional<BurnComponent> component(int amount, int tiers, int remainder);
}
