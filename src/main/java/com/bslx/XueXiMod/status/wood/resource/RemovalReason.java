package com.bslx.XueXiMod.status.wood.resource;

/**
 * 调用方可以声明的移除原因。没有自然凋谢。
 * 自然入账只由服务端 tick 内部结算，任何外部原因都不发岁华。
 */
public enum RemovalReason {
    EXTERNAL,
    DEATH,
    LOGOUT,
    DIMENSION_CHANGE,
    CHUNK_UNLOAD,
    LEVEL_UNLOAD,
    RESTART
}
