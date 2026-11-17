# 冷却 v4.1 美术接入回执（2026-11-12）

## 身份

- 批次：沿用 `docs/开发记录/术式批量制作/火系六式-20261029`。旧记录没有 ASCII 机器 batchId；本轮台账别名 `fire-six-20261029` 只是同一旧批次的兼容称呼，不是新批次，也不新开额度。
- taskId：`implement`；子范围 `fire_cool-v4.1`；attemptId：`20261112-native-integration`。
- 角色：本轮唯一源码作者。不是正式复审者。设计 2/2、实现 3/3 已耗尽，未启动 Astra，未做独立或正式复审，也未重新判断旧玩法正确性。
- 请求：`cursor/grok-4.7@256k`、`high`。当前工具没有 tier 字段；Fast 沿用流程记录的客户端 `service_tier="priority"` 配置。实际后端未暴露，记未知。
- 真实直接父代理/唯一接收者：当前主聊天 `01a1ce06-b681-7c90-b11d-260d3c169911`。聊天 ID 不是 agent ID。
- 原生返回 agent ID：`01a1ce12-fe43-76f0-8c8e-5d1b41a102eb`（Leibniz）。这是本次工具返回值，不用自报提供方 ID 替代。
- 下属 Flash：本任务没有派发。此前由主聊天取得的事实包只作输入，不重复封装。下属读取：不适用。

## 完成

已把确认版 v4.1 的四张分层竖条接入运行资源，并把主动冷却的旧全身广告牌替换成随身体朝向的前后蒸汽和世界水平冲击波。

- 后蒸汽：`textures/effect/cool_v41/cool_v4_1_rear_strip.png`，64×1024 RGBA，16 帧。
- 前蒸汽：`textures/effect/cool_v41/cool_v4_1_front_strip.png`，64×1024 RGBA，16 帧。
- 碎点：`textures/effect/cool_v41/cool_v4_1_fragments_strip.png`，64×1024 RGBA，16 帧。
- 冲击波：`textures/effect/cool_v41/cool_v4_1_shockwave_strip.png`，96×1536 RGBA，16 帧。
- 四张贴图各有同名 `.mcmeta`：`blur=false`、`clamp=true`。帧序仍由渲染按竖条手动取样，不依赖动画 mcmeta。
- 四张 PNG 的 SHA-256 与正式 manifest 对应条目一致，字节也与正式目录逐一相同。未复制 aseprite、bbmodel、GIF、空间预览或假人贴图。
- 仍由现有 `COOL_ONCE` 提示触发，16×75ms 共 1.2 秒，播完清除。第 2 帧（75ms）才有蒸汽像素，第 7 帧（450ms）才有冲击波像素，第 16 帧全透明。三层蒸汽和冲击波取同一个帧号。

## 尺寸、轴与跟随

16 模型单位等于 1 参考格。蒸汽贴片宽 42.66666、高 42.66666 模型单位，即左右 ±21.33333、底 -5.33333、顶 37.33333。像素按 2/3 模型单位。三层深度分别是后 -2.5、前 2.7、碎点 2.9 模型单位。

贴片随玩家脚点插值，并绕 Y 轴按身体朝向旋转。Blockbench 把贴图放在 +Z 朝向的面上，所以本地 +Z 对准玩家面向，不跟镜头俯仰。冲击波是世界水平面，半边 32 模型单位（贴片边长 64），高度 1.2 模型单位（0.075 参考格），不随镜头倾斜。四张贴图均按 Blockbench 的双面设置画正反两面，透明像素保留。

本地玩家使用客户端帧插值；同一渲染帧里的其他玩家没有独立 partial tick，沿用当前逻辑位置。旧的炉心全身广告牌已删除。

## 未改

未改玩法、伤害、范围、配置、ChainRole、公共结算、语言文件、制作表、资源库索引、正式美术 README/manifest 或批次记录。`FireVisualClient.java` 曾短暂加过插值参数，已恢复原四参数调用，最终无差异。

N 游戏简句建议沿用现有 `zh_cn.json`：`technique.xueximod.fire_cool` 为「冷却」，`technique.xueximod.fire_cool.desc` 为「主动冷却炉心，并放出一次蒸汽」。本回执不覆盖完整 N/O/DV，也不改表。

## 检查与收束

- 定向检查：PNG 尺寸、模式、SHA-256、与正式源逐字节一致；常量引用、旧 `furnace_v1/cool.png` 不再被冷却绘制引用；冲击波不读取镜头俯仰。不是正式复审，也不补审旧玩法。
- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。报告共 14 处，全部位于 `BatchCarrierVisualRenderer.java`、`NetherDomainWisps.java`、`HeatUpSpatialMount.java`，均非本次文件，按授权保留。第一次运行曾因本次文件中的字面换行报解析错误，该局部问题已修；最终一次不再报告本次文件。
- 编译：待统一编译。未跑 `compileJava`、未打包、未启动游戏。
- 游戏表现未验证：肩部到腰腿的遮挡、冲击波贴地与地形、透明排序、第一人称、其他玩家插值，以及原有冷却触发和伤害，都留实测。
- 写入已收束。本任务没有下属代理。

## 最终文件指纹

- `FurnaceBodyVisual.java`：SHA-256 `689062B8CC9282655F1039B35D520C3277F5ED564DAFDDCFA526928142D3797C`，14283 字节，mtime `2026-11-12T13:17:22.6841844+08:00`。
- `FireVisualLayout.java`：SHA-256 `3845368C24FC0571AD507F17F0820DB4A587BF77FF12118EF91E29C082796ADB`，8004 字节，mtime `2026-11-12T13:02:35.3625118+08:00`。
- `cool_v4_1_rear_strip.png`：`31259CCD0F7A3913CFEBD8878B54FED2C1EC4813B4E955B8E0A64EBF84253E21`，4458 字节，mtime `2026-11-12T10:32:48.3339787+08:00`。
- `cool_v4_1_front_strip.png`：`484839BA59E66C1080DB45629DA1CA47D20BC0E7848AC70BA88AE6B233FFC549`，2980 字节，mtime `2026-11-12T10:32:48.4023496+08:00`。
- `cool_v4_1_fragments_strip.png`：`5F5D4D387DE9767E759CA77681A41B47A791B71FE5FCC1558C0EA4BB1770C5C0`，792 字节，mtime `2026-11-12T10:32:48.4731171+08:00`。
- `cool_v4_1_shockwave_strip.png`：`DF4419173D8CBD7B65C91BD2CE9D04B7B6B21009773D2FE5EF0581FC2AF0F7C6`，11156 字节，mtime `2026-11-12T10:32:48.2642428+08:00`。
- 四份 `.mcmeta` 相同：`B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`，64 字节，mtime `2026-11-12T13:07:16.8923174+08:00` 至 `.8958230+08:00`。

## 统一编译（2026-11-12）

编译授权由主方单独续派。attempt 另记 `20261112-final-compile`，不替换实现 attempt `20261112-native-integration`。设计 2/2、实现 3/3 已用完；本阶段只执行编译，不是复审，未启动 Astra，也未改 Java、资源或 Gradle 配置。

编译前最小确认：工作区没有正在运行的 `java`/`javaw`/`gradle`。报告中的两份 Java、四张 PNG 和四份 mcmeta 哈希与字节数都未变；两份 Java 的 mtime 仍是 `2026-11-12T13:17:22.684184+08:00` 和 `2026-11-12T13:02:35.362512+08:00`。`FireVisualClient.java` 仍是撤回后的 `E7520A850DB753FA86E6C5927A3BF84B526AC4EE17DA7C53743F75640FD71553`、`43049` 字节。现有 `gradle.properties` 已是 `org.gradle.jvmargs=-Xmx3G` 和 `org.gradle.daemon=false`，未改。

命令：在 `D:/alianximod` 执行 `.\gradlew.bat compileJava --offline --no-daemon`。开始 `2026-11-12T13:21:15.2485955+08:00`，结束 `2026-11-12T13:21:41.0475154+08:00`。退出码 `0`。结果：`BUILD SUCCESSFUL in 24s`；4 个任务中 `:compileJava` 与 `:downloadMcpConfig` 实际执行，`:extractSrg` 与 `:createMcpToSrg` 为 UP-TO-DATE。日志有 54 条既有 API 过时注记，不是编译失败；日志里的中文因控制台编码显示为乱码，不改写日志。完整输出：`docs/开发记录/术式批量制作/火系六式-20261029/cool-v41-compile-20261112.log`。

编译后两份源码哈希与编译前相同，mtime 也未变。对应 class 在本次编译窗口内重新写出，晚于源码：

- `FurnaceBodyVisual.class`：14243 字节，mtime `2026-11-12T13:21:36.414290+08:00`。
- `FireVisualLayout.class`：5015 字节，mtime `2026-11-12T13:21:40.270967+08:00`。

因此这次 `compileJava` 覆盖的是收束后的最终源码字节。编译进程已结束，结束后没有残留 `java`/`javaw`/`gradle`。本次写入只有本回执追加和编译日志，写入收束。

未跑：游戏、打包、`build`/`jar`/`reobf`/`runClient`，以及额外测试或复审。遮挡、贴地、透明排序和冷却触发仍留实测。上一轮 name-gate 全仓 14 处既有问题仍在 `BatchCarrierVisualRenderer.java`、`NetherDomainWisps.java`、`HeatUpSpatialMount.java`；本次没改这些文件，也没有重跑或修全仓。
