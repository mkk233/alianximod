package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;

/** 当前视角帧上的只读蒙皮上下文。关闭权只属于视角栈。 */
public interface SkinBinding {
    ActionSampleContext sample();

    boolean skipRigidWriter();

    void close();
}
