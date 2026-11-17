# 崩地 v2r2 飞扬土尘候选回执

2026-11-16。batchId `待制作-20261031`；taskId `earth_break_ground_v2_art_preview`；attemptId `20261116-art-ground-v2-04`。

直接父级：`01a1ce06-b681-7c90-b11d-260d3c169911`。沿用原美术代理 Feynman `01a1e49b-2cc4-7390-a605-76028c8b0f08`，未另开作者、未派下属。沿用实际 Carbon attempt01、Argon attempt02 均返回 `Unknown model`、无 agent_id/产物之后的 `gpt-6-astra / max` 美术降级；本次不重试模型，不声称 Google 完成，未新增后端模型验证。

**本阶段额度已用完，返修结果未追加复审。** 设计 2/2、实现 3/3 不变。这是已授权美术返修及作者产物自检，不是正式复审。

## 终态

返修候选 **`earth-ground-airborne-dust-v2r2-20261116` 已交付，写入已收束，待用户同版确认**。

用户原话：“这个尘土效果不好没有飞扬的感觉”。旧版 `earth-ground-mouth-dust-v2-20261116` 已拒绝，本次没有接入或记录采用它。保留全部旧源、旧模型、旧预览和既有 Blockbench 项目。

## 产物位置

候选根目录：
`D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116/`

- 交接手册：`D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116/visual-handoff.md`
- 完整 SHA/尺寸/帧数/时长清单：`D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116/manifest.json`，182 个产物条目。
- 真 Aseprite 源：`source/ground_airborne_v2r2.aseprite`，144×48、3 层、24×50 ms，已只读原生复开；粗粒源 `source/visual_grit_v2r2.aseprite`。
- 透明原尺寸导出：`exports/ground_airborne_v2r2-atlas.png` 144×1152，三份 48×1152 strip、24 张逐帧，另有粗粒 24×32 alpha atlas。
- 二维动画：`previews/ground_airborne_v2r2-2d.gif`，864×288，24 不同帧，1.2 秒，最近邻。
- 原生可编辑模型：`source/earth_ground_airborne_v2r2_preview.bbmodel`，445 elements、279 groups、3 textures、1 条原生 2.3 秒时间轴，已保存复开。
- **整体动态**：`previews/earth_ground_v2r2-overall-real-blockbench.gif`，1280×668、2.3 秒；46 原生采样，36 编码帧、33 不同画面。
- **侧视动态**：`previews/earth_ground_v2r2-side-real-blockbench.gif`，1280×668、2.3 秒；46 原生采样，33 编码帧、30 不同画面。
- `overall-*.png` / `side-*.png` 给出平地、上冲、顶点、回落、稀薄尾段、散尽；`top-*.png` 为俯视；`reopened-native-viewport.png` 为原生复开截图。全部在 `previews/`。
- `evidence/` 保存完整 N/O/DV 当前和此前原文、运动配方、原生逐帧时间/世界矩阵、复开证据、保护文件哈希和工具实际错误。

## 实质返修

旧版 11 团扁平尘饼、升高 0.2375 格、0.8 秒；本版重新绘制为不规则明暗像素簇，9 条非等距分相上冲细尘，运动根峰值上升 0.56–1.64 格；另有 12 颗 0.07–0.16 格粗土视觉颗粒，沿不同抛物线向上、外散、回落，最后淡去。最高粗粒中心距原地表约 2.41 格。不是整团缩放、不是同步平面环。

候选运行预算 **1.4 秒 / 28 tick**，相对旧 0.8 秒增加 0.6 秒；单簇 1.2 秒，出生错开 0/50/100 ms，最后可见像素在触发后约 1.25 秒结束。时间变化明确属于待确认候选，未当成现行已批准参数。

仍只有土尘；粗粒仅视觉，不新增方块、掉落或伤害。DUST 的 CAMERA_PLANE、世界光照、非全亮、不幽冥染色不变。精确 23 块坑形、16 模型单位=1格、1.8格人体参照均保留。C 为 `Vec3.atCenterOf(impact)`，地表 C.y+0.5，粒子根 C.y+0.56；完整锚点、UV、相位、运动和材质边界写在交接手册及机器可读配方。

## 真实工具与检查结果

- 喜鹊 Aseprite status 成功：1.3.17/API39 batch ready，live=false。真实 Lua 绘制、原生保存/导出和源复开成功。
- 喜鹊 Blockbench capabilities 成功：desktop5.2.2/plugin1.10.1。独立新建 UUID `937be075-0517-53f5-6aa9-c7f18ce35ab8`；原生保存再载入得到 `1aa7c01f-c3a9-2d54-e824-b3331f4094e9`，3 个 texture error 均为 0。
- 真实桌面 active Preview 截图，原生 Timeline/Animator 逐帧；Python 仅最近邻二维预览、原生帧 GIF 编码与验证，不离线重画场景。无 ComputerUse、无 headless Blockbench、无 ImageGen。
- 作者实际查看二维像素阶段、整体与侧面上冲/顶点/回落/尾段、俯视露坑、复开截图和最终 GIF 解码尾段。原生粒01高度从 0.06→1.86→0.06 格，已检查不是同一姿态重复。
- 全部最终原生截图对象边界都在视口内，固定相机不自动裁边漂移。细尘末前帧最大 alpha16，末帧全透明；1.65 秒后两类粒子都不可见，露坑保留至演示结束。
- Python 准备、二维预览、GIF 编码、最终验证、manifest 脚本实际退出码均为 0。不是编译或游戏测试。
- 真实错误：历史 `functions.exec` 不受本宿主支持，改用现有 exec_command；第一次 Lua dofile 的 args 全局未注入报 nil，明确赋值后成功。取景初版自动裁边/平台下缘裁切已通过固定视口和最终 FOV 修正。无持续工具阻塞。没有重试被 native ancestor 拒绝的父聊天消息接口；本次直接 final 回传。

## 保护与表对账

只写本新候选目录和本独立回执。28 个开工前 Blockbench 项目的名称与 saved 状态均保留，旧 v2、幽冥及其他工程未关闭覆盖。旧 v2 全目录、v1源/导出/运行图与当前表合计 115 个保护文件，前后 SHA 全部一致。

当前表 SHA `D7681EAAD336B5E4719B9FFE21BEE2652D9EA74303C97E86119A1C0B1ADD3F56`，190956B；比旧 `87F22…` 变化仅本行 DV 反馈。该变化由父方已协调的唯一记录者于 `2026-11-16 22:15:59.909+08:00` 完成，并非美术作者改表。B/N/O、参数、状态不变；完整原文已经快照。

## 校验锚点

- manifest SHA256：`4AB7E17F7183A97BD3D26D9E38C682082B8365F796959A0BED5B2297E3F52E88`
- 主 Aseprite SHA256：`EE367DBB75773ED8D5FAFF7A5B1ECF1EA8C31EE69921DFB89854A6187C49C01A`
- bbmodel SHA256：`A7FF96F62EEDA989B8BDA63889D3C0D8B45250697E127C5A3A695584C060AA2C`
- 整体 GIF SHA256：`A4AF190D9DE2CD194EBDDA503506AF48A19767E4DC6074D2F9F9E02671DAF521`
- 侧视 GIF SHA256：`D7644062795DA9BB110FB210936A01B0D34724190A08405E8AC101EDF9A7251F`

## 未完成与父方下一步

功能沿用现状：已知只有陨岩直接撞地和残骸真正落地两条 EXECUTED 后播放链；普通/其他通用攻击调用缺口仍只登记，本轮未修。模型删块只是演示。

美术返修候选交付；用户同版确认未获得；正式归档/接入未做；未编译、未打包、未启动游戏、未游戏实测。现行 runtime 仍是 v1 单 billboard/16 tick，本候选九尘流、粗粒和多帧相位不是源码已经实现，不能直接覆盖旧图冒充接入。条目仍留制作中。

请父方展示本版整体与侧视 GIF，连同二维预览和候选 1.4 秒参数等待用户同版确认。确认后再安排正式归档和指定执行者接入/编译。本回执不授权迁待验收。写入全部收束；下属无。
