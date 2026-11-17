# native-implement-05 最终回执

- 身份：batchId `survival-embryo-20261109`，taskId `source-owner`，attemptId `native-implement-05`。
- 代理：父代理 `01a1c134-987b-77c1-90d2-e1402336b301`。请求模型 `cursor/grok-4.7@256k`，请求推理 `high`。实际后端、提供方会话和请求 ID 未暴露，记未知。
- 用户后续说明喜鹊默认启用 Fast，且 Grok 4.7@256k 的 Fast 为 1.5 倍速。本回执不把它写成已独立验证的后端事实，也不改记请求模型。
- 输入：v1，另加用户明确要求“之前应该有部分实现了你接着做”。关联 B 为空。设计 1/2、实现正式复审 0/3，未重置、未自行复审。
- 下属 Flash：未派生。本轮跨模块事实由本执行者在已授权源码和本地 Forge 1.20.1 源码范围内核对。读取范围、终态和未决项记为不适用；没有未返回的下属代理。

## 旧作者核对

- implement-04 receipt 仍标 `running`（2026-11-10 00:10:02）。没有发现对应 Python 进程或仍在写入授权源码的子进程。
- implement-02 为 TimeoutError，implement-03 为 native Run lifetime expired（410）。三份旧 receipt 的 changes 都为空。
- 开工时五个新 Java 文件均不存在，`ShuEmbryoItem.java` 与 `xueximod.mixins.json` 仍是既有版本。旧 HTTP 工具不是原生代理，未续用。
- 现存 `java.exe` 32540 是 IntelliJ 2024.3 拉起的 Gradle 8.8 daemon，不是旧实现作者。未结束任何用户进程。
- 历史脚本、request、response、receipt 均未修改或删除。制作表未读未写。

## 落盘与沿用

- 新建 `EmbryoConversionService`：统一服务端事务。只接受 `Attribute=NONE`；产物复制原有 NBT 后写目标属性，已有属性不覆盖。
- 金：左槽 1 个无属性原胚加右槽至少 1 个金锭，产物进真实背包，零经验。普通点击和 Shift 一次一个；数字键、丢弃、拖拽、中键复制在命中该配方时取消且不消耗。
- 木：树苗或完全成熟的标准 `CropBlock` 整株换成空气，耕地保留；未成熟无效。
- 水：满水炼药锅换成空锅；未满无效。
- 火：包含全部流体的射线直接命中岩浆源才消耗该源；流动岩浆拦截，不处理后方方块。
- 土：只接受精确 `Blocks.DIRT`。
- 容量先按原版背包顺序预演，主手最后一个原胚腾出的格子可计入。空间不足、保护取消、冒险模式、距离或权限失败时完全不消耗。
- 主副手及方块/流体两个入口用同一玩家的服务器 tick 与下一 tick 去重，避免一次点击重复结算。
- 沿用既有 `shu_embryo`、`ShuEmbryoItem`、`ShuAttribute`、模型、贴图和语言。`ShuEmbryoItem.java` 未改。未改原矿、粗胚、熔炼、高炉和合成链，未做美术。
- 未完成：Gradle 编译未授权、未运行；游戏效果未实测。本回执不是实现复审通过。

## 改动文件

- `D:\alianximod\src\main\java\com\bslx\XueXiMod\item\EmbryoConversionService.java`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\event\EmbryoConversionEvents.java`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\mixin\AnvilMenuMixin.java`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\mixin\ItemCombinerMenuMixin.java`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\mixin\AbstractContainerMenuMixin.java`
- `D:\alianximod\src\main\resources\xueximod.mixins.json`
- `D:\alianximod\docs\开发记录\原胚属性获取-20261109\native-implement-05.final.md`

## 定向检查

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`
- 退出码：0。
- 结果：源码命名可读性检查通过。
- 编译：待统一编译。未打包、未运行游戏、未提交。

## 写入收束与哈希

源码写入已收束。以下 SHA-256 与时间取自最终回执写入前：

- `EmbryoConversionService.java`：`a190565268aa6e53cc0c9bd6233f41214ec8157f8709409a3700074c71bee756`，2026-11-10 01:11:12。
- `EmbryoConversionEvents.java`：`5337339480c550a0c6b687f21085a829ecd89782c1e8ea390238494a7a1d0b18`，2026-11-10 01:10:39。
- `AnvilMenuMixin.java`：`c1f44309aa7baeb093c5f270b8ad02afb58efdbe1503261bccd43aca22358af6`，2026-11-10 00:59:48。
- `ItemCombinerMenuMixin.java`：`1be18abd8c79a14881c1b1d7660b36e9ff48b665af22999a2667ef0e626830b9`，2026-11-10 00:59:26。
- `AbstractContainerMenuMixin.java`：`cf419092bb38ea6916060e92f4a3e016e5ecd5864105ee6fb94f05efa3dd7d17`，2026-11-10 00:59:27。
- `xueximod.mixins.json`：`b41c1d1f22d4d1c2bfbddfebbbdcd621dd93dda575141cccb66df4fe5bf28de3`，2026-11-10 00:59:27。
- 未改 `ShuEmbryoItem.java`：`de95703ac0a17edea7e24839908331f49dd3edc293a6da903ad5e290527acf22`，2026-09-27 03:14:23。

本回执是之后新增的最后写入。环境没有审批拒绝；旧失败类别仍为超时、410 和旧路径所有权比对失败，不是额度耗尽。

## 用户实测

- 普通点击、Shift、零级、满背包、主副手。
- 成熟与未成熟作物、树苗、耕地保留。
- 岩浆源与流动岩浆，以及岩浆后方方块不被穿透处理。
- 满水与未满炼药锅，普通泥土与草方块、砂土、缠根泥土、泥巴。
- 保护目标、冒险模式、原矿粗胚烧制链。
