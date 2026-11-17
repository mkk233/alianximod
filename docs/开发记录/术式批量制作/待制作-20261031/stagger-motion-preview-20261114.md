# 美术预览执行回执：`rule_stagger_motion_preview`

## 1. 任务身份与边界核对

- **batchId**：`待制作-20261031`
- **taskId**：`rule_stagger_motion_preview`
- **attemptId**：`20261114-art-stagger-01`
- **stage / 操作角色**：`implementation` / `art`（遵循 `C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md` 及 `references/visual-handoff.md`）
- **真实直接父代理**：主会话
- **输入版本**：`D:/alianximod/docs/术式制作表.xlsx`（SHA256 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`，`制作中` 第 5 行）
- **关联术式 B**：`rule_stagger`（错时 / 通用 / 无 / `规则 · 触发 · 错时`）
- **当前阶段复审计数**：设计 `2/2`、实现 `3/3` 已用完（本单为三维空间时序波次运动示意预览，不计入正式复审轮次、不重置计数）
- **独占写路径核验**：仅写入以下候选过程归档目录与本回执文件，未改 `src/`、`docs/术式制作表.xlsx`、正式资源目录或 `docs/资源库/README.md`，未运行 `gradlew`：
  1. `D:/alianximod/docs/资源库/过程归档/错时_波次出手示意_20261114/`
  2. `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/stagger-motion-preview-20261114.md`

## 2. Blockbench MCP 发现、连接与既有打开工程保护结果

- **工具发现与只读能力核对**：经 `tool_search` 取得喜鹊 `mcp__magpie__blockbench__*` 真实 schema 后，先完成只读连接与工程清点，确认连接 **Blockbench Desktop `5.2.2`（`win32`，插件 `1.10.1`，活跃主视口 `main` `1706×1215`）**。
- **既有打开工程保护状态（全程零关闭、零修改、零覆盖）**：
  1. `cool-v4.3-equal-width-steam-ground-wave-preview`（UUID `fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef`，`saved: true`，`elements: 29`，`groups: 2`，`textures: 11`）— 原样保留
  2. `flame-trail-v1-spatial-draft-20261112`（UUID `e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5`，`saved: true`，`elements: 514`，`groups: 8`，`textures: 71`）— 原样保留
  3. `flame-trail-v2-projectile-composite-draft-20261112`（UUID `8a58e270-0fce-3a38-f8c5-528d850c4a39`，`saved: true`，`elements: 109`，`groups: 4`，`textures: 98`）— 原样保留
  4. `flame-trail-v3-projectile-composite`（UUID `8cdb8be1-7355-a034-3a20-7ae41a0e5af1`，`saved: true`，`elements: 80`，`groups: 5`，`textures: 21`）— 原样保留
  5. `fire-spark-v3-1-crossed-launch-preview`（UUID `3cc70eec-3c0d-db2e-731f-2581790ce09d`，`saved: false`，`elements: 51`，`groups: 5`，`textures: 6`）— 原样保留
  6. `fire-upwell-parabolic-motion-preview`（UUID `6cfd19e7-972f-1e54-9c0f-bd74cd037ea9`，`saved: false`，`elements: 153`，`groups: 9`，`textures: 11`）— 原样保留
  7. `surface-anchor-v2-inverted-triangle-preview`（UUID `19a21c6d-556d-a2cf-167b-83abca62ed0a`，`saved: false`，`elements: 40`，`groups: 5`，`textures: 11`）— 原样保留
- **本次新建独立工程**：`rule-stagger-wave-timing-preview`（UUID `3d79dbaa-5e99-e81f-8116-db817cd0cee2`，`free` 通用模型，`253` 个 cube、`0` 个 mesh、`11` 个分组、`12` 张 `16×16` 纯色示意色板贴图；全部临时离屏视口已清理，`list_views` 核验仅剩主视口 `main`）。

## 3. 空间时序波次运动示意设计说明（严守非新增贴图 / 非额外复制边界）

1. **纯时序编排示意定位**：
   - 对齐需求 `N / O / DV` 与现行代码 `BatchRulePassEffect.java`、`BatchReleasePlan.STAGGER_GAP = 10`、`BatchReleaseCompiler.java:445`：错时自身无独立新贴图，仅编排已存在副本在相对本组开始的第 `10i tick` 出手，不因落入规则槽覆盖「一分为三」，也绝不把单份攻击免费复制成多份。
2. **比例与示意辅助隔离（`16.0 模型单位 = 1.0 Minecraft 世界格`）**：
   - `示意辅助` 根组收拢全部非运行参照物：`1×1` 世界格地面网格与 `0 / +8 / +16` 格双段等距刻度尺（顶面 `Y = 0.0`）、`t = 0` 站于 `[-20, 0, 0]` 的 `1.80` 格高半透明浅青施法虚影及 `[0, 24, 0]` 八角冻结发射锚点环与 `+X` 冻结视线轴、`t = 20` 侧移后退至 `[-48, 0, -42]` 并偏转 `+38°` 的 `1.80` 格高实心玩家假人（配珊瑚红虚线 `×` 截断准星）、旁侧单份对照道的暗灰镂空虚线空槽框与地面 `×` 标；全部严禁进入运行资源。
3. **三条核心语义的 `t = 20 tick` 同刻空间对照（参考直线速度 `v = 0.8 格/tick = 12.8 单位/tick`）**：
   - **主展示道（`Z = 0.0`，`一分为三 + 错时`）**：
     - 第 0 份（`0 tick` 出生，已飞 `20 tick`）位于 **`+16.0` 格** `[256.0, 24.0, 0.0]`（`1` 道金环 + `5` 颗尾迹点）；
     - 第 1 份（`10 tick` 出生，已飞 `10 tick`）位于 **`+8.0` 格** `[128.0, 24.0, 0.0]`（`2` 道金环 + `3` 颗尾迹点）；
     - 第 2 份（`20 tick` 刚出生，已飞 `0 tick`）正从 **`0.0` 格** `[0.0, 24.0, 0.0]` 冻结发射环跃出（`3` 道金环 + 刚出膛金色八角脉冲环，`0` 颗尾迹点，自身寿命自出生刻起算）。
   - **冻结原点与不重新瞄准**：`t = 20` 实心玩家假人已离开施法原位并向侧方转头 `+38°`，但第 1、2 波次依然严格从 `t = 0` 冻结锚点 `[0.0, 24.0, 0.0]` 沿原冻结 `+X` 轴射出，不跟随新视线重瞄准。
   - **旁侧单份对照道（`Z = +48.0` = `+3.0` 格，`无复制 + 错时`）**：未装 `一分为三` 时仅 `1` 份副本在 `0 tick` 立即出手（已飞至 `+16.0` 格 `[256.0, 24.0, 48.0]`），`+8.0` 格与 `0.0` 格处均为暗灰虚线空框 + `×`，直观展示不凭空增加后续弹体。

## 4. 交付物清单与 SHA256 校验（经 `view_image` 逐张自检通过）

目录：`D:/alianximod/docs/资源库/过程归档/错时_波次出手示意_20261114/`

| 文件名 | 用途说明 | 字节数 | SHA256 |
| --- | --- | ---: | --- |
| `rule-stagger-wave-timing-preview.bbmodel` | Blockbench `5.2.2` 可编辑三维空间时序波次运动示意源工程 | 157,454 | `4995CF2CC4DFEF3B85926677BAF88A5120D1E1226E1C25AADC8FD247F15E2287` |
| `rule-stagger-01-side-wave-spacing.png` | 正交侧视图（核对 `1.80` 格假人、`0 / +8 / +16` 格三波次等距间隔与推进方向） | 52,008 | `4234A3EEBC1A7E312F906963EEC0C5222AB091B6F5140FC1A47B2C25807971E1` |
| `rule-stagger-02-top-frozen-aim-comparison.png` | 正交俯视图（核对上道 3 份错时波次 + 冻结原点 vs 侧移转头玩家假人，及下道单份立即出手无额外复制对照） | 65,194 | `0C6A20716A44AFB4A43C108198CF496F218C120400388C7C6F125A1F4A41E35B` |
| `rule-stagger-03-closeup-frozen-origin.png` | 源点近景透视图（核对 `t=0` 冻结虚影、`t=20` 刚出膛第 3 发、走开转头玩家假人、中段第 2 发与旁侧空槽） | 319,668 | `72837345A894931571BF3D53E267CE774A30B90E69B85E01C708EB3AF944C957` |
| `rule-stagger-04-hero-3quarter.png` | 3/4 全景透视主图（完整收揽两条对照道、三波次间距、刻度尺与双时刻假人对照） | 275,765 | `2B1CAAE48970688D82C0D905A83F9B8E22D80001D169CAD294D1A45EC2C5E98D` |
| `visual-handoff.md` | 完整视觉交接记录（含纯时序示意边界声明、空间换算参数、非运行辅助隔离与接入对照要点） | 12,158 | `972D64FB6E6BBAA600FA6CE149A12930B26784C9CDD19D2D36DB308B383E76FD` |
| `manifest.json` | 交付物结构化清单、既有打开工程保护快照与哈希索引 | 7,031 | `4D457EBDD83C63FB8CE553251CBD1202A05EA0ED59758215A26CE1B76F01C895` |

## 5. 当前状态

- **美术预览状态**：过程归档已完成真实 Blockbench `5.2.2` 空间时序建模与 4 视角真实视口无损导出，**待用户确认**（未归入正式资源目录，未更新 `docs/资源库/README.md`，未触碰任何源码与制作表）。
