package com.bslx.XueXiMod.shu.technique.release.phantom;

public interface PhantomHandle {
    boolean active();

    void end(PhantomEndReason reason);
}
