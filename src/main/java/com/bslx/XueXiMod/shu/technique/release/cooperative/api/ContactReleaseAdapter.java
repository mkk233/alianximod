package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.ActionWindow;

/** 把既有接触窗适配到 {@link ContactFamilySource}。实现不在本阶段。 */
public interface ContactReleaseAdapter {
    ActionWindow.Sink sink(ContactFamilySource source);
}
