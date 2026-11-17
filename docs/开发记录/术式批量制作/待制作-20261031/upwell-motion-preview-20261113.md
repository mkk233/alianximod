# 美术预览执行回执：`fire_upwell_motion_preview`

## 1. 任务身份与边界核对

- **batchId**：`待制作-20261031`
- **taskId**：`fire_upwell_motion_preview`
- **attemptId**：`20261113-art-upwell-01`
- **stage / 操作角色**：`art`（`antigravity/gemini-4-carbon`，遵循 `C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md` 及 `references/visual-handoff.md`）
- **真实直接父代理**：主会话
- **关联术式 B**：`fire_upwell`（喷涌）
- **当前阶段复审计数**：设计 `2/2`、实现 `3/3` 已用完（本单为三维空间运动示意预览，不计入正式复审轮次、不重置计数）
- **独占写路径核验**：仅写入以下候选过程归档目录与本回执文件，未改 `src/`、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`、正式资源目录或 `docs/资源库/README.md`，未运行 `gradlew`：
  1. `D:/alianximod/docs/资源库/过程归档/喷涌_抛物线运动示意_20261113/`
  2. `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/upwell-motion-preview-20261113.md`

## 2. Blockbench MCP 发现、连接与既有打开工程保护结果

- **工具发现与只读能力核对**：经 `tool_search` 取得喜鹊 `mcp__magpie__blockbench__*` 真实 schema 后，先调用 `get_capabilities(include_tools=false)`、`get_project_info`、`list_views` 完成只读核对，确认连接 **Blockbench Desktop `5.2.2`（`win32`，插件 `1.10.1`，活跃主视口 `main` `1706×1215`）**。
- **既有打开工程保护状态（全程零关闭、零修改、零覆盖）**：
  1. `cool-v4.3-equal-width-steam-ground-wave-preview`（UUID `fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef`，`saved: true`，`elements: 29`，`groups: 2`，`textures: 11`）— 原样保留
  2. `flame-trail-v1-spatial-draft-20261112`（UUID `e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5`，`saved: true`，`elements: 514`，`groups: 8`，`textures: 71`）— 原样保留
  3. `flame-trail-v2-projectile-composite-draft-20261112`（UUID `8a58e270-0fce-3a38-f8c5-528d850c4a39`，`saved: true`，`elements: 109`，`groups: 4`，`textures: 98`）— 原样保留
  4. `flame-trail-v3-projectile-composite`（UUID `8cdb8be1-7355-a034-3a20-7ae41a0e5af1`，`saved: true`，`elements: 80`，`groups: 5`，`textures: 21`）— 原样保留
  5. `fire-spark-v3-1-crossed-launch-preview`（UUID `3cc70eec-3c0d-db2e-731f-2581790ce09d`，`saved: false`，`elements: 51`，`groups: 5`，`textures: 6`）— 原样保留
- **本次新建独立工程**：`fire-upwell-parabolic-motion-preview`（UUID `6cfd19e7-972f-1e54-9c0f-bd74cd037ea9`，`free` 通用模型，`95` 个 cube + `58` 个 mesh 共 `153` 个元素，`9` 个分组，`11` 张纯色示意色板贴图；临时离屏视口已清理，仅保留主视口 `main`）。

## 3. 空间运动示意设计说明（严守非新增份数 / 非火柱边界）

1. **纯运动轨迹示意定位**：
   - 遵循需求 `N` / `O` 与现有记录「喷涌没有新素材，不另造免费攻击特效」，不新画二维投射物贴图，仅使用简单可辨认的双色梭形四棱锥箭头体表达瞬时速度切线朝向。
   - 场景内同时展示 **1 条单份竖直抛物线** 与 **3 条向上锥形分散抛物线**，明确标明为**单份模式与多份模式的运动轨迹对照示意**，**不是单次释放免费增加攻击份数，不自动生成弹体，不生成火柱、爆炸或落地火场**。
2. **比例与示意辅助隔离（`16.0 模型单位 = 1.0 世界格`）**：
   - `示意辅助` 根组：含 `1×1` 世界格棋盘地面与三向 `1` 格步进射程标尺（顶面 `Y = 0`）、站于 `[-40, 0, 0]` 的 `1.80` 格高施法假人（`28.8` 模型单位，眼高带 `Y = 25.92` = `1.62` 格）、`[0, 16, 0]` 抛射源点十字、`Y = 48` 处的向上 `25°` 初始发射锥口导引环与母线、单份最高点 `Y = 116` 方环、单份旁侧上下转向细箭头及地面 `4` 处十字落点标；全部归入 `示意辅助`，不进运行资源。
3. **抛物线物理与姿态核对（对齐 `v0 = 1.0` 格/刻、`g = 0.08` 格/刻²、向上半锥角 `25°`）**：
   - **单份竖直抛物线（中心琥珀金轨）**：自源点 `[0, 16, 0]` 沿 `+Y` 竖直上抛（`t = 4.5` 刻米白尖头朝 `+Y`），`t = 12.5` 刻抵达顶点 `Y = 116.0`（`7.25` 格，净抛高 `+6.25` 格），再受重力垂直落回 `[0, 0, 0]`（`t = 21.0` 刻冰青白尖头朝 `-Y`）；轨迹用稀疏断开采样点表示，明确区别于连续火柱。
   - **多份向上锥形分散抛物线（外围 3 条暖橙红轨，`0° / 120° / 240°`）**：绕竖直向上轴 `+Y` 按 `25°` 半锥角分配不同初始方向，`t = 4.8` 刻昂首斜上出锥（`+51.0°`），`t = 11.33` 刻在 `R = 76.60`（`4.79` 格）、`Y = 98.14`（`6.13` 格）水平过峰，随后受重力弯曲下落（`t = 19.2` 刻俯角 `-56.1°`），在 `t ≈ 23.71` 刻完整落回地面 `R = 160.34`（`10.02` 格），落点干爽无火场或火柱。
   - 附视觉对照提醒：按 N/O「多份在向上的锥形范围内分配不同初始方向」，多份锥轴基准应为世界竖直向上 `(0, 1, 0)`。

## 4. 交付物清单与 SHA256 校验（经 `view_image` 逐张自检通过）

目录：`D:/alianximod/docs/资源库/过程归档/喷涌_抛物线运动示意_20261113/`

| 文件名 | 用途说明 | 字节数 | SHA256 |
| --- | --- | ---: | --- |
| `fire-upwell-parabolic-motion-preview.bbmodel` | Blockbench `5.2.2` 可编辑三维空间运动示意源工程 | 138,198 | `DA9ECA3F9603FCFEFCA1E4D4EA3503D3AE5B15DBA8B90AB267F89D52EB9B25AF` |
| `fire-upwell-motion-01-side-view.png` | 正交侧视图（核对 `1.8` 格施法假人、中心单份竖直上抛至 `7.25` 格再重力回落，及左右向上 `25°` 锥角完整抛物线落回 `Y=0`） | 60,009 | `FB15D33EFA77FDA515AEDB0F25D5AFF703F5BE7FF50986E05D2743AD2BE9936D` |
| `fire-upwell-motion-02-top-cone-distribution.png` | 正交俯视锥形分布图（核对向上 `25°` 初始发射锥环、`0°/120°/240°` 三向分散轨迹与 `10.02` 格落点分布） | 78,470 | `A1C79BD1672F1B6CBD0C9479877F6E89F76141C669351C59E97F8DB356C0633A` |
| `fire-upwell-motion-03-closeup-cone-and-single.png` | 源点锥口与单份上下回落近景透视图（近景核对 `25°` 锥口出射仰角与单份竖直上抛/重力回落双相位朝向） | 129,472 | `68F46BD9EABF47B737C47EF7C010A95E32532C54C4C253FD52EA6ACE837BDA00` |
| `fire-upwell-motion-04-hero-3quarter.png` | 3/4 全景透视主图（完整收揽施法假人、单份竖直轨、向上锥口与 3 条分散抛物线及全部落点，经官方 `capture_screenshot` 复核一致） | 116,976 | `7169AAF7DE37825DAD16B1DFA56ADD2C851317925D024ABB0D9ECA9CB453789E` |
| `visual-handoff.md` | 完整视觉交接记录（含运动轨迹示意边界声明、空间换算参数、非运行辅助隔离与接入对照说明） | 10,363 | `5AA84FCAD966FE6C7DF8396A8B39B0698F4E59DBC3806BA1F2479F02F507B8A5` |
| `manifest.json` | 交付物结构化清单、工程保护快照与哈希索引 | 5,832 | `9F56B554A3A61D1465387699C0BA2BD1F22C14099DB67041F3D98CA8C9F0674F` |

## 5. 当前状态

- **美术预览状态**：过程归档已完成真实 Blockbench 建模与多视角视口导出，**待用户确认**（未归入正式资源目录，未更新 `docs/资源库/README.md`）。
