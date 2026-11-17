# 护体同步检查收尾

只改了 `src/test/java/com/bslx/XueXiMod/network/GuardSyncWireCheck.java`。生产源、其他测试、正式归档和 `integration-validation.md` 都没动。没有再跑 `compileJava`、全量 `compileTestJava` 或临时 `init.gradle`。原 `guard-check.log` 保留上次结果。

协议 `"2"` 及两端严格相等仍按源码静态核对。`ModNetwork.protocolVersion()` 挪到九项核心检查之后，单独捕获初始化失败，只把协议项标成 `UNEXECUTED`，不再中断后续检查。断言失败或任一核心没跑完时进程非零退出。

## 命令

工作目录 `D:\alianximod`。classpath 用已有的 `build\tide-classpath.txt`。产物目录 `build\guard-checks`。

`javac --release 17 -encoding UTF-8` 只编译 `GuardSyncWireCheck.java`，退出码 0。

`java -Dfile.encoding=UTF-8 -cp build\guard-checks;<tide-classpath> com.bslx.XueXiMod.network.GuardSyncWireCheck`

日志：`D:\alianximod\docs\资源库\过程归档\水环接入-20261027\guard-check-final.log`

## 结果

九项核心全部执行且通过：包往返、刷新年龄、乱序门控、重复姿态时钟、新身份、晚加入、旧删除、世界清理、负年龄。断言 55，失败 0。

```
PASS 9
FAIL 0
UNEXECUTED 0
PROTOCOL UNEXECUTED
WIRE_EXIT=0
```

协议项是 `UNEXECUTED protocol java.lang.ExceptionInInitializerError`。这不是全面通过。核心退出码 0 只表示核心断言没有失败，也不表示协议检查已经跑过。没有客户端渲染、发包或世界卸载事件。
