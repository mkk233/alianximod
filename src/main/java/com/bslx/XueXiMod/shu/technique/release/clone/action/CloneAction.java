package com.bslx.XueXiMod.shu.technique.release.clone.action;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneActionContext;

/**
 * 分身在一次服务器执行里能准备的动作。
 * 准备不造成伤害、不扣费、不改次数。
 */
public interface CloneAction {
    String actionKey();

    ClonePreparedAction prepare(CloneActionContext actionContext);
}
