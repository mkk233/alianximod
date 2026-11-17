package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneServerPorts;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneStandaloneDeployment;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ServerSessionLookup;

/**
 * 协同会话、分身端口和执行服务共用这一份实例。
 */
public final class CooperativeRuntime {
    public enum Room {
        ACCEPT,
        SOLO,
        REFUSE
    }

    private static final CooperativeServerSessions SESSIONS = new CooperativeServerSessions();
    private static final CloneServerPorts PORTS = CloneServerPorts.create(SESSIONS);
    private static final FamilyExecutionService SERVICE = new FamilyExecutionService(PORTS, SESSIONS);

    private CooperativeRuntime() {
    }

    public static void install() {
        PORTS.getClass();
    }

    public static FamilyExecutionService service() {
        return SERVICE;
    }

    public static ServerSessionLookup sessions() {
        return SESSIONS;
    }

    public static CloneStandaloneDeployment deployment() {
        return PORTS;
    }
}
