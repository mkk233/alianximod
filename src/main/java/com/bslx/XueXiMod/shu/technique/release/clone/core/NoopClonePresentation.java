package com.bslx.XueXiMod.shu.technique.release.clone.core;

final class NoopClonePresentation implements ClonePresentationSink {
    static final NoopClonePresentation INSTANCE = new NoopClonePresentation();

    private NoopClonePresentation() {
    }

    @Override
    public void accept(ClonePresentationNotice notice) {
    }
}
