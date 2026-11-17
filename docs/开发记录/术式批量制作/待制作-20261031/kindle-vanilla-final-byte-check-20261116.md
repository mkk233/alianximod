# 引燃原版火焰最终字节对账

- batchId：`待制作-20261031`
- taskId：`fire_kindle_v2_progress`
- attemptId：`20261116-compile-kindle-vanilla-02`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier，沿用客户端 Fast `service_tier="priority"`，未独立验证后端。
- 本阶段额度已用完，返修结果未追加复审。

这是编译覆盖的字节对账，不是正式复审。未重跑 Gradle，未改 Java、表、资源索引或批次主记录，也未覆盖旧回执。对账时没有 `java.exe`。

## 磁盘原文

当前两个源码由 `Get-FileHash -Algorithm SHA256` 读取：

- `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`：53375 字节，2026-11-16 14:28:55.528，`C4A09F2E7B62653D86FC0B62B02C89A4556EFAD1C9DCE7338B42340E0D67FE79`。
- `D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherSoulFireMixin.java`：3191 字节，2026-11-16 14:28:55.537，`18DE43196BC5E3BDF9BA622B24C1B4430D96CF079F14015087745359AC5121C8`。

UTF-8 原文精确检索到同一个 `FireVisualClient.java` 哈希：

- `kindle-vanilla-visual-impl-20261116.md:19`，文件哈希 `167EEE1765D97DDCE147FC82D72AF8D58B2109CAACAAFCBE83827077127E84C0`。
- `kindle-vanilla-visual-compile-20261116.md:14`，文件哈希 `BBEBD200E578E92232C847F94883B7525E4A8523E0C7EF67212CC80E72087200`。
- `kindle-vanilla-visual-compile-before-20261116.json` 字节偏移 23246，条目长度 53375、时间 14:28:55.528。
- `kindle-vanilla-visual-compile-after-20261116.json` 字节偏移 23245，条目长度和时间相同。

Mixin 哈希同样只出现一次：实现回执偏移 1196、编译回执偏移 683、编译前 JSON 偏移 83407、编译后 JSON 偏移 83406。两份 JSON 的 Mixin 条目都是 3191 字节、14:28:55.537。

## 树与覆盖

编译前、后 JSON 都是 1606 个普通文件。按原算法从保存条目重算，两份树摘要都是记录值 `C685B014524BC7F8808119741BCB42325CE6CAD9AB83F0CB5E89BBC388B941F7`，与 `kindle-vanilla-visual-compile-20261116.md:48` 相同。

当前两个源码字节等于编译前条目，也等于编译后条目。既有编译日志显示 `:compileJava` 实际执行，不是 `UP-TO-DATE`；对应 class 写于 14:34:42.049 和 14:34:43.495，都晚于 14:28:55 的源码。因此 14:34 的编译已经覆盖当前最终字节，不需要重编译。

聊天中出现的两个 `FireVisualClient.java` 哈希在本次实收任务文本里逐字符相同，并等于磁盘值 `C4A09F2E7B62653D86FC0B62B02C89A4556EFAD1C9DCE7338B42340E0D67FE79`。磁盘记录不存在 `C4A09F2E...` 或其他不同的 64 位十六进制哈希。这是聊天转写不一致，不是源码变化。

## 收束

未 build、jar、reobf、runClient，未启动游戏，也未做游戏内验证。源码写权保持收束，无下属。下一步由父方交 Rawls 同步；表仍由记录者处理。
