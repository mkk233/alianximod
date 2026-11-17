package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ContactFamilySource;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ContactReleaseAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateHandle;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.TriggerGeometry;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 把冲刺窗口的每一次接触收成一把只触发一次的子门。 */
public final class CooperativeContactBridge implements ContactReleaseAdapter {
    @Override
    public ActionWindow.Sink sink(ContactFamilySource source) {
        return new ActionWindow.Sink() {
            @Override
            public void onContact(UUID targetId, Vec3 contactPoint, Vec3 contactDirection) {
                TriggerGeometry geometry = new TriggerGeometry(contactPoint, contactDirection, targetId);
                ReleaseGateHandle handle = source.openSubFamily(geometry);
                if (handle != null) {
                    handle.trigger(geometry);
                }
            }

            @Override
            public void onEnded() {
                source.onWindowEnded();
            }

            @Override
            public void onFinished() {
                source.onWindowFinished();
            }

            @Override
            public boolean closed() {
                return source.windowClosed();
            }
        };
    }
}
