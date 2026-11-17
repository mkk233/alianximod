# 持续面向玩家标签与留焰弹道修正 — 统一编译回执

- batchId：`待制作-20261031`
- taskId：`camera_facing_tag_and_trail_fix`
- attemptId：`20261113-compile-facing-tag-01`
- 工作区：`D:\alianximod`
- stage：`implementation`
- 操作角色：compile（指定统一编译者）
- 请求模型：`cursor/grok-4.7@500k` · high + Fast。沿用客户端 `service_tier="priority"` 配置；宿主未单独返回后端已独立验证 Fast，本回执不把该配置写成已验证事实。
- 可观察返回身份 / 后端追踪：未暴露，记未知。
- 原生工具：本执行者直接完成，未再派生子代理。子代理标识不适用。
- 授权来源：用户已授权修复留焰游戏内拖尾未连成线，并建立统一持续面向玩家标签底座。本单只授予唯一编译：`.\gradlew.bat compileJava --offline --no-daemon`。制作表写回权限为无。
- 输入版本：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/camera-facing-tag-and-trail-fix-20261113.md`
- 依赖：`camera_facing_tag_and_trail_fix` / `20261113-impl-facing-tag-02`（前序已完成并关闭）
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`fire_flame_trail`（以及底座接入涉及的既有术式；`rule_surface_anchor` 搁置）
- 嵌套读取：无。未派 Flash，未派任何子代理。
- 额度：设计 2/2、实现 3/3，均已用完。本单未追加正式正确性复审，未重置计数。

## 结果

`compileJava` 已实际执行，退出码 0，`BUILD SUCCESSFUL`。编译前后 `src/main` 零差异。编译开始时间晚于 12 个目标 Java 文件和输入记录的最后写入。

未改任何 `src/`、资源、`docs/术式制作表.xlsx`、`记录.md` 或 `实测清单.md`。未执行 `build`、`jar`、`reobf`、`runClient`，未启动游戏。编译通过只表示这次源码编译通过，不代表美术、用户同版本确认或正式接入已完成，也不是复审通过。

## 十二个 Java 文件

时间为东八区本地时间，毫秒精度。下表在编译前核对，编译后再核对一次：字节数、mtime、SHA256 都与输入记录一致，且编译前后不变。

| 路径 | 字节 | mtime | SHA256 | 对输入记录 |
| --- | ---: | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingTag.java` | 462 | 2026-11-13 03:18:48.399 +08:00 | `BB3D55BC421B692DC8C014C2E1C0661AA55D97CFD105D561E03E99D11C1DAB4E` | 一致 |
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingEffectId.java` | 537 | 2026-11-13 03:18:56.014 +08:00 | `EB870665235054602246ED777D7A52F4D20B10BD2C9B5A59DED6711CC37B349D` | 一致 |
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 03:19:03.871 +08:00 | `7B0BFE01EA01320C60C77A713CC4FAB09604A7C23441FE6EB523E1D1C490A83E` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 5690 | 2026-11-13 03:23:45.163 +08:00 | `C9F441D2A88C8752A2BE0208A9655D7B6271BEF99256DBAB0D10413023D4D1D8` | 一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 18821 | 2026-11-13 03:19:33.403 +08:00 | `F70A0D38EE8FB10D3F3D1EAECE54235DBF8FCE195DF29C4691940E623CE15FFF` | 一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlameTrail.java` | 9765 | 2026-11-13 03:19:41.170 +08:00 | `34C0A3252FAED95FBDC3ED9127CA108D962C1A652AFB4CC52055981F33B8AAE2` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 19502 | 2026-11-13 03:20:05.058 +08:00 | `4AC10F6AD5955C1DFB42AC4CB9825E67CA724C1FF4CCE325EEC1A285E189252B` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` | 43052 | 2026-11-13 03:21:10.044 +08:00 | `49468273CF65202DC2E0F7C8030DCB4430532C9B1136386F6AD54A1422E26AAF` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/FireSeedFlameClient.java` | 10320 | 2026-11-13 03:21:10.056 +08:00 | `F1BF54647425839B2B6FC2719AD084D4026103D4C86D6238ACD1321B4A969CE1` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java` | 14272 | 2026-11-13 03:23:45.170 +08:00 | `2BE12460D2ABF72A3D82F9337FC8C710433E5A85E74638178B4613728045FB4E` | 一致 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordEntity.java` | 33178 | 2026-11-13 03:20:40.355 +08:00 | `04914D96533237D8B0DA9D762D55ACE29BB45C639487E437A723651729BBE5E3` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/SwordEntityRenderer.java` | 4253 | 2026-11-13 03:21:09.577 +08:00 | `2558BC186875E16A0FE6A99AAA24E97AEDFF34F7E6C0CD07207CCFF2B7981F3E` | 一致 |

12 个 Java 文件的最晚 mtime 是 `PrisonShellRenderer.java` 的 2026-11-13 03:23:45.170 +08:00。输入记录本身：5591 字节，mtime 2026-11-13 03:26:28.348 +08:00，SHA256 `87956921C2A645E3B4628D1407E1C41C53364F71493603DB592074210F7D4258`。该记录另列的资源库 `README.md`（4177 字节，2026-11-13 03:23:45.177 +08:00）和 `manifest.json`（7055 字节，2026-11-13 03:25:10.726 +08:00）不在这 12 个门里；本单只读抽查了它们的字节数和 mtime，没有写入。

## src/main 树

算法与既有编译取证脚本一致：取 `src/main` 下全部文件（含隐藏文件），按完整路径做 PowerShell `Sort-Object FullName`，相对路径从项目根算并改为正斜杠。每行是 `相对路径\t字节数\t小写SHA256\n`，拼成 UTF-8 无 BOM 后再做 SHA256，结果取小写。

| 时点 | 核对时间 | 文件数 | 总字节 | 树哈希 |
| --- | --- | ---: | ---: | --- |
| 编译前 | 2026-11-13 03:31:04.200 +08:00 | 1379 | 13011918 | `10bcbe583b17812a0754a64afdd80ca0ed5cce27c58c21345d69c5c6a065b311` |
| 编译后 | 2026-11-13 03:32:49.323 +08:00 | 1379 | 13011918 | `10bcbe583b17812a0754a64afdd80ca0ed5cce27c58c21345d69c5c6a065b311` |

两次相同。编译期间 `src/main` 源码零差异。

## 占用检查

编译启动前，没有正在写入本项目的 Gradle 进程。只有 IDEA 拉起的空闲 Gradle 8.8 daemon：PID 52656，父进程 `idea64.exe` PID 33864，子进程 0。命令行是 `org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.8`，不含 `alianximod`、`GradleWrapperMain`、`compileJava` 或 Forge 客户端。它不是本次编译的写入进程，没有被结束。

本次构建按日志另起一个 single-use daemon，并写明构建结束后停止。编译后仍只剩下原来的 PID 52656。

## 编译

- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 捕获方式：`cmd.exe /d /c` 运行上述命令，把标准输出和标准错误合并写入日志。没有改命令，没有加其他 Gradle 任务。
- 开始：2026-11-13 03:31:31.694 +08:00
- 结束：2026-11-13 03:31:59.878 +08:00
- 壁钟耗时：28184 ms。日志自身写 `BUILD SUCCESSFUL in 27s`。
- 退出码：0
- 是否晚于最后一次相关写入：是。开始时间晚于最晚的目标 Java 写入 2026-11-13 03:23:45.170 +08:00，也晚于输入记录 2026-11-13 03:26:28.348 +08:00。
- Java：日志写明 Java 17.0.6，JVM 17.0.6+9-LTS-190，Arch amd64。
- 任务：`:downloadMcpConfig` 执行，`:extractSrg` UP-TO-DATE，`:createMcpToSrg` UP-TO-DATE，`:compileJava` 执行。合计 4 个可执行任务，2 个执行，2 个已是最新。`compileJava` 不是 UP-TO-DATE。
- 命令含 `--offline`。日志显示 `:downloadMcpConfig` 被执行且构建成功；本回执不把这一行改写成联网下载或已跳过。
- 警告：按 CP936 解码后，结尾为「54 个警告」，都是已过时或待删除 API 的警告，没有编译错误。警告未使构建失败。本单未因编译修改这些警告。

## 日志

- 路径：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/camera-facing-compile-20261113.log`
- 字节：18959
- mtime：2026-11-13 03:31:59.444 +08:00
- SHA256：`829F4482EFFCD70BBF00B45F88FA84D0D482920ABDD0DD54C2EF23639CCEBF52`
- 编码：日志是 cmd 捕获的原始字节，中文警告为控制台代码页 CP936/GBK。上述 SHA256 对的是这些原始字节，不是转码后的文本。本单没有回写或改编码这份日志。

## 状态分界

- 功能：前序实现 `20261113-impl-facing-tag-02` 已收束。本单只编译，不重新实现，也不把编译成功写成功能复审通过。
- 美术、用户确认、正式接入：本单未核对，不改变其状态，不因编译通过判定已齐备。
- 游戏实测：未做。
- 制作表：未写。`docs/术式制作表.xlsx` 仍为 178752 字节，mtime 2026-11-13 02:22:22.978 +08:00。
- `记录.md`：未写。25090 字节，mtime 2026-11-13 02:23:05.664 +08:00。
- `实测清单.md`：未写。32198 字节，mtime 2026-11-13 01:54:40.444 +08:00。

## 收束与额度

本单写入已收束。只写了本回执和 `camera-facing-compile-20261113.log`，加上 `compileJava` 正常产生的 `build/` 产物。没有其他作者持有这两份记录路径。文件句柄在本 final 返回前关闭。

设计额度 2/2、实现额度 3/3 在开工前已用完。本编译没有借编译名义新增正式正确性复审，没有重置计数，也没有把编译退出码 0 当成复审通过。

审批拒绝、编译失败和阻塞：无。

下一步：父会话接收本编译回执后，派唯一写表记录代理同步批次记录与制作表状态。本执行者不写表。
