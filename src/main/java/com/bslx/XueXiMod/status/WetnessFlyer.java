package com.bslx.XueXiMod.status;

/** 显式加入潮湿下降。玩家、乘客、落地和实际流体仍由下降入口排除。 */
public interface WetnessFlyer {
    default boolean wetnessDescentActive() {
        return true;
    }
}
