# compileJava 失败诊断

交回 `SkillChainPlan.java` 的作者。该文件在多重释放任务（会话「查看项目当前状态」，`01a15fdc-4130-7c92-bece-27530e8c54be`）的源码范围里。纯计算作者 A/B 的文件没有出现在这 4 个错误中。本单没有改源码。

## 命令

`D:\alianximod`，`2026-10-30T08:47:04.5781116Z` 至 `2026-10-30T08:47:27.2744045Z`，退出码 1。

```text
.\gradlew.bat compileJava --offline --no-daemon
```

`:compileJava` 实际执行后失败，不是 UP-TO-DATE。日志还有 34 条过时 API 警告，失败原因是下面 4 个错误。

## 错误

声明在第 1121 行：

```text
private static MulticastExpansion expansionAtRule(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques)
```

第二参数是 `int`，第三参数是 `List<Technique>`。下面 4 处把这两个实参写反了，编译器报告 `List<Technique>` 无法转换为 `int`：

| 行 | 调用 |
|---|---|
| 367 | `expansionAtRule(segments, techniques, plan.dualRuleSegment)` |
| 909 | `expansionAtRule(segments, techniques, ruleIndex)` |
| 1057 | `expansionAtRule(segments, techniques, ruleIndex)` |
| 1107 | `expansionAtRule(segments, techniques, ruleIndex)` |

文件：`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillChainPlan.java`。

编译前后 `compile-source-freeze.json` 的 712 项路径、长度和 UTC ticks 都没有变化。这是冻结源码本身编译失败，不是编译期间输入被改写。完整输出在 `compile.log`。
