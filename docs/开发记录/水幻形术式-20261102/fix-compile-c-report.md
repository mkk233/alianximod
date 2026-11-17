# 水幻形 C 组编译错误修复报告

日期：2026-11-02。只修本次 javac 报给 C 的两处错误。没有编译，没有进游戏。本报告写完后停止写源码。

依据是 `compile-attempt-1/compile-nonvisual-report.md` 和 `compile-attempt-1/compile-nonvisual-log-utf8.txt`。日志里 C 的两条是 `SkillUsePacket.java:632` 的 `captured` 重复定义，以及 `ActorCaptureDispatcher.java:152` 找不到 `UUID`。

## 改动

| 路径 | 行号 | 作用 |
|---|---|---|
| `network/SkillUsePacket.java` | 335–337 | 捕获路由的局部变量改为 `captureTiming` |
| `shu/technique/release/capture/ActorCaptureDispatcher.java` | 30 | 补上 `import java.util.UUID` |

`executeSkill` 里 632 行原来的 `SkillChainPlan.ModifierPlan captured` 没改名，它的后续引用也没动。新名字只替换捕获路由那一次赋值、空判断和返回。非空仍直接返回该 `CastTiming`，空则继续原来的批量编译。

`UUID.randomUUID()` 仍在快照构造里，现在是 154 行。没有改成字符串，节流账和会话判断没动。

没有改其余文件，没有注册术式，没有声明平衡键。

命名门禁：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。

未运行 Gradle，未 `compileJava`，未打包，未进游戏。C 组到此停止写入。
