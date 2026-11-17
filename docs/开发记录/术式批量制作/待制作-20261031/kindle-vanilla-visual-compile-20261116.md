# 引燃原版火焰显示编译

- batchId：`待制作-20261031`
- taskId：`fire_kindle_v2_progress`
- attemptId：`20261116-compile-kindle-vanilla-01`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier，沿用客户端 Fast `service_tier="priority"`，未独立验证后端。
- 本阶段额度已用完，返修结果未追加复审。

## 收束证据

实现回执 `kindle-vanilla-visual-impl-20261116.md` 已由父方接收。编译前磁盘与该回执一致，编译后未变：

- `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`：2026-11-16 14:28:55.528，53375 字节，`C4A09F2E7B62653D86FC0B62B02C89A4556EFAD1C9DCE7338B42340E0D67FE79`。
- `D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherSoulFireMixin.java`：2026-11-16 14:28:55.537，3191 字节，`18DE43196BC5E3BDF9BA622B24C1B4430D96CF079F14015087745359AC5121C8`。

编译前没有 `java.exe`。本轮没有改源码、表、资源索引或批次主记录，也没有读取记录者仍在写的主记录。无下属。

## 命令

工作目录 `D:/alianximod`。唯一 Gradle 命令：

```text
.\gradlew.bat compileJava --offline --no-daemon
```

开始 2026-11-16 14:34:20.478，结束 14:34:47.049，退出码 0。标准输出和标准错误由外壳合并写入日志，命令本身没有附加 Gradle 参数。

日志：`kindle-vanilla-visual-compile-20261116.log`，21552 字节，197 行，`4F2EF67AFC8F22DEF5A6380C4CB28DF9400061FAA41496BE5E43B03934ED56A0`。中文注记因控制台代码页转写出现乱码；英文、路径、`[removal]` 和构建结果可读，未为修正编码重跑。

## 结果

`BUILD SUCCESSFUL in 25s`。任务为 `:downloadMcpConfig`、`:extractSrg UP-TO-DATE`、`:createMcpToSrg UP-TO-DATE`、`:compileJava`；4 个可执行任务中 2 个执行、2 个已最新。`:compileJava` 实际执行。无 `error:` 或 `FAILED`。

有 57 条既有 `[removal]` 警告，分布在 38 个既有文件。本次 Mixin 没有警告。`FireVisualClient.java:870` 是原有 `new ResourceLocation(XueXiMod.MOD_ID, path)`，不是本次引燃判断。

编译窗口内写出且晚于源码：

- `build/classes/java/main/com/bslx/XueXiMod/client/FireVisualClient.class`：14:34:42.049，44426 字节，`8D3C308FC2AEA13EAA203C99734827B5111C1ADA7EC736E2BF869CE2E7424044`。
- `build/classes/java/main/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherSoulFireMixin.class`：14:34:43.495，3399 字节，`5505BBB4087B63A8AB1AC6099D68508FB9A7BFA1B723E6F001582F137FFAAA93`。

编译结束后没有残留 `java.exe`。

## 源码树

两份 JSON 都保存 `src/main` 每个普通文件的正斜杠相对路径、大写 SHA256、字节长度和修改时间，路径按序数排序。树摘要只连接 `路径 空格 大写SHA256`，LF 并保留末尾 LF，UTF-8 无 BOM 后再取 SHA256。写盘后重新解析复算，与记录值一致。JSON 字节不同只因阶段名和采集时间不同。

- 编译前：`kindle-vanilla-visual-compile-before-20261116.json`，14:33:54.952–14:33:55.948，416865 字节，`92B841A852088DDEC40240D58ED6228CFBBDD29169C53A267DA5C90C930C81E7`；1606 个文件，树摘要 `C685B014524BC7F8808119741BCB42325CE6CAD9AB83F0CB5E89BBC388B941F7`。
- 编译后：`kindle-vanilla-visual-compile-after-20261116.json`，14:34:58.743–14:34:59.840，416864 字节，`C6B1CCF394E5E77B44C1D4A7A6D3F91EBD20CF62C0C386873F34C82C2EA1F34F`；1606 个文件，树摘要相同。

逐文件新增、删除、内容变化、仅时间变化都是 0。两个触点编译后仍是上述 14:28:55 哈希。本次编译覆盖当时最终源码，没有同期源码写入需要排除。

## 未做

未 build、jar、reobf、runClient，未启动游戏，编译成功不是游戏内验证。未改 Java、表、资源索引或批次主记录。源码写权保持收束，无下属。下一步由父方交唯一记录者同步。
