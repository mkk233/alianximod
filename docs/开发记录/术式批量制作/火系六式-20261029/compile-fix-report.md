# 编译错误修复

同一作者只改编译器已经报告的那一处：`FireVisualPacket` 的访问器 `wait()` 与 `Object.wait()` 重名。设计 2/2、实现 2/2 不重置。本阶段额度已用完，返修结果未追加复审。这次没有重跑 Gradle。待指定编译者重跑。

## 做了

访问器改名为 `waitTicks()`，仍返回原来的 `wait` 字段。该字段是燃烧视觉的等待 tick 数。`encode` 仍在同一位置 `writeVarInt(this.wait)`，`decode` 仍按原顺序读入构造参数。字段名、单位、网络字节顺序和玩法没有改。

直接调用只有 `FireVisualClient.accept` 里的 `packet.waitTicks()`。没有检查源码引用旧访问器，所以没有改检查。

## 没做

- 没有跑 `compileJava`、`build`、`jar`、`reobf`、`runClient`，没有启动游戏。
- 没有改 32 条弃用警告。
- 没有改制作表、资源、语言、`compile-report.md`、`compile-gradle.log` 和其他已有工作。原编译失败结果保留。
- 名称门禁仍只报 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。这两处不是本次文件，没有改。

## 改动文件

| SHA256 | 最后写入 | 文件 |
| --- | --- | --- |
| `FA2390975180F20165893AEF8F4F9E68DD2A7B08B40085E0A715B0440EFE5132` | 2026-10-29 04:13:24.4129322 | `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\FireVisualPacket.java` |
| `423AF8D09CD9DBCC79B6F816B5AB997BCD0626D6D13F2BB2639619D071615822` | 2026-10-29 04:13:24.4117342 | `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\FireVisualClient.java` |
| `85AA1CB5CE985389CC8E6FF1DBDF798BD16245B49E42CD26BA9D238A0787BA50` | 2026-10-29 04:13:53.2145582 | `D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\compile-fix-name-gate.txt` |

## 检查

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。输出只有上述两处旧名字。本次两个源码文件没有新的命名违规。

没有跑 Gradle。

## 停写

本报告写完后，这次授权范围内的写入已经停止，文件占用已经释放。待指定编译者重跑。本阶段额度已用完，返修结果未追加复审。
