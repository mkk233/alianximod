# 候选查询复用：采用共享编译结果

采用领域绑定任务已完成的最终 `compileJava`。本任务没有再跑 Gradle，没有改 Java、Mixin 或构建配置，没有新增正确性复审。说明文档验证段已更新，写入权释放。

## 采用的编译

- 报告：`docs/开发记录/领域绑定规则-20261030/compile-final-report.md`
- 会话记录：`compile-run.jsonl` 首条 `system/init`，`session_id` 为 `0f148371-69d4-4d14-b38a-ddeb6222087f`，`model` 字段为 `Grok 4.7 256K High Fast`，工作目录 `D:\alianximod`
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 第 1 次未启动（沙箱 Gradle 目录没有发行包）。采用第 2 次：`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，Gradle 报告 Java 17.0.6
- 开始 `2026-10-30T05:49:55.4584153Z`，结束 `2026-10-30T05:50:23.5243287Z`
- Gradle 退出码 0，外层退出码 0（`compile-run-exit.txt`）。`:compileJava` 有独立任务行，不是 UP-TO-DATE。`BUILD SUCCESSFUL`
- 结构化结果以 `compile-evidence/result.json` 为准。`facts.json` 内容是 `null`

候选查询 A/B、统计说明和领域绑定相关作者均已释放，编译槽已释放。

## 覆盖

对照 `compile-source-freeze.json`（`2026-10-30T05:44:18.4472147Z`，688 个路径的长度和 UTC 写入刻度）。当前仍是 687 个主 Java 加 `xueximod.mixins.json`，集合、长度、写入刻度全部一致。最晚写入是 `SwordEntity.java` 的 `2026-10-30T05:30:33.8728955Z`，mixin 配置是 `2026-10-30T04:13:54.0495718Z`，都早于编译开始。编译开始之后没有这些文件的写入。

这不是本任务自己的 SHA-256 比对。外部报告里的前后树散列同为 `25e6db62231e5a4c2582b9ceb0fe47327aeadac815411156f198d9c6d818a458`，`source-diff.txt` 为 `UNCHANGED`，只作为该编译任务的第三方证据。

## 限制

未打包。未做游戏或性能实测，不声称 TPS 收益。运行期 Mixin 未验证。本步骤此前的静态结论只覆盖本步骤自己的实现。
