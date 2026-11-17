# 美术预览执行回执：`fire_spark_crossed_launch_preview`

## 1. 任务身份与边界核对

- **batchId**：`待制作-20261031`
- **taskId**：`fire_spark_crossed_launch_preview`
- **attemptId**：`20261113-art-spark-cross-01`
- **stage / 操作角色**：`art`（`antigravity/gemini-4-carbon`，遵循 `C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md` 及 `references/visual-handoff.md`）
- **关联术式 B**：`fire_spark`（火苗）
- **当前阶段复审计数**：设计 `2/2`、实现 `3/3`（本次三维美术预览不计入正式复审轮次、不重置计数）
- **独占写路径核验**：仅写入以下两处候选过程归档与开发回执路径，未触碰 `src/`、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`、`docs/资源库/特效/火焰/` 或 `docs/资源库/README.md`，未运行 `gradlew`，未派生子代理：
  1. `D:/alianximod/docs/资源库/过程归档/火苗_v3.1_十字穿插直射示意_20261113/`
  2. `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-3d-preview-20261113.md`

## 2. Blockbench MCP 连接与既有打开工程保护验证

- **工具发现与连接结果**：通过 `tool_search` 取得 `mcp__magpie__blockbench__*` 真实 schema 后调用 `get_capabilities(include_tools=false)`、`get_project_info`、`list_views`、`capture_screenshot`，确认本会话真实连接 **Blockbench Desktop `5.2.2`（`win32`，插件 `1.10.1`，活跃主视口 `main` `1706×1215`）**。
- **既有打开工程保护状态（全程零关闭、零改动、零覆盖）**：
  - `cool-v4.3-equal-width-steam-ground-wave-preview`（UUID `fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef`，`saved: true`，`elements: 29`）
  - `flame-trail-v1-spatial-draft-20261112`（UUID `e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5`，`saved: true`，`elements: 514`）
  - `flame-trail-v2-projectile-composite-draft-20261112`（UUID `8a58e270-0fce-3a38-f8c5-528d850c4a39`，`saved: true`，`elements: 109`）
  - `flame-trail-v3-projectile-composite`（UUID `8cdb8be1-7355-a034-3a20-7ae41a0e5af1`，`saved: true`，`elements: 80`）
- **本次新建独立工程**：`fire-spark-v3-1-crossed-launch-preview`（UUID `3cc70eec-3c0d-db2e-731f-2581790ce09d`，`free` 通用模型，`51` 个 cube，`5` 个分组，`6` 张贴图）。

## 3. 空间规格与二维确认贴图绑定说明

1. **只读复用已确认二维贴图**：
   - 路径：`D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3_strip.png`（`64×1024`，16 帧每帧 `64×64`，`5,005` bytes，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）。
   - 校验 `.bbmodel` 内嵌纹理 `fire_spark_v3_strip` 的原始 PNG 字节 SHA256 同为 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`，逐字节完全一致。
2. **空间换算与参考系（`16 模型单位 = 1 世界格`）**：
   - `示意辅助_地面1格刻度网格`：`16×16` 模型单位暗色棋盘格，顶面严格位于 `Y = 0`。
   - `示意辅助_施法玩家假人_28.8高`：站立于 `[0, 0, 0]` 面朝 `+X`，高 `28.8` 模型单位（`1.80` 格），眼高参考带 `示意辅助_dummy_eye_band_y25_92` 中心 `Y = 25.92`（`1.62` 格），右施法手臂向 `+X` 伸出。
3. **原版箭矢式 `±45°` 十字穿插火苗弹体（对齐 `BatchCarrierVisualRenderer.drawSparkCross`）**：
   - 分组 `fire_spark_crossed_projectile`（中心 `[22.0, 22.0, 0.0]`）：包含 `fire_spark_blade_plus45`（绕 `+X` 旋转 `+45°`）与 `fire_spark_blade_minus45`（绕 `+X` 旋转 `-45°`）两张零厚度双面发光面片（`12.8×12.8` 模型单位 = `0.80×0.80` 格，`halfSpan = 0.40` 格 = `6.4` 模型单位）。
   - 轴向与 UV：`+X` 前端对应贴图 `U = 1` 圆润亮黄白火头，`-X` 后端对应 `U = 0` 渐窄细尾与飞散火星；两片翼面贴图上脊（`V = topV`）均指向斜上方，中心交线汇聚亮黄白火核。
   - 朝向锁定：`CameraFacingTag.NONE`，仅在出生时按发射方向锁定一次，飞行全程直射不随相机旋转，不再出现实测反馈中的「转圈」问题。
4. **三维透视关键几何校核提示（供主会话与用户参考）**：
   - 若玩家原地静止施法，且弹体视觉中心严格位于第一人称针孔相机光轴 `eyePos + t * lookDir`（即与 `Y = 25.92, Z = 0` 完全重合，法向偏距 `Δ = 0`），则任何过发射轴的平面（含 `±45°` 双翼）在三维透视中均严格穿过相机针孔中心，会以 `0°` 掠射角退化为细线（原版 `AbstractArrow` 通过在 `eyeY - 0.10` 格眼下生成来避免该退化）。
   - 本示意将近枪口弹体置于 `Y = 22.0`（眼高下方 `3.92` 模型单位 ≈ `0.245` 格，对应胸手施法高度），在 `01-first-person-launch.png` 实测第一人称下四片 `±45°` 斜翼张角饱满清晰。若运行端实机原地静止施法发现第一人称初始几刻过薄，可在 `drawSparkCross` 或视觉锚点上保留约 `-0.15 ~ -0.25` 格沿 `-upAxis` 的眼下视觉微偏置（不改碰撞盒），即可完全复现 `01-first-person-launch.png` 的第一人称展开观感。

## 4. 交付物清单与 SHA256 校验（经 `view_image` 逐张自检通过）

目录：`D:/alianximod/docs/资源库/过程归档/火苗_v3.1_十字穿插直射示意_20261113/`

| 文件名 | 用途说明 | 字节数 | SHA256 |
| --- | --- | ---: | --- |
| `fire-spark-v3-1-crossed-launch-preview.bbmodel` | Blockbench `5.2.2` 可编辑三维空间示意源工程（默认隐藏中段对照辅助组） | 42,052 | `8E676075011EC74FC596BC0E1F046D7CAC62DACEE87A0CF3B9C79A3567B4C47B` |
| `fire-spark-v3-1-3d-01-first-person-launch.png` | 第一人称发射视角图（相机置于玩家眼位看向 `+X`，右下角见施法手臂，验证 `±45°` 十字双翼清晰展开、不退化、不转圈） | 50,738 | `A25A668C6ED21C136467E74039802B61F2B8E2FED3AD1C4634F9C86EC3FAF68D` |
| `fire-spark-v3-1-3d-02-hero-3quarter.png` | 3/4 近景透视主图（完整施法假人 + 1 格棋盘地面刻度 + 直射十字火苗空间比例，无裁切；并经官方 `capture_screenshot` 复核一致） | 107,329 | `B06516E2AB4197529B23851463050E3F5B00525EB4879FEBC47EAEB1190ACA06` |
| `fire-spark-v3-1-3d-03-axial-cross-section.png` | 迎面轴向十字截面透视图（直观展示原版箭矢式 `×` 形 `±45°` 正交双面片穿插与中心亮核） | 91,082 | `F4C48F8A7B95B38D1E98BA06F74CB6D930AC686ADB139A2E6F43C6D49CA46F07` |
| `fire-spark-v3-1-3d-04-side-view.png` | 正侧视图（正交侧视，核对 `1.8` 格身高、`1.62` 格眼高，以及火头朝 `+X` 前方、细尾留 `-X` 后方的水平直射姿态） | 31,892 | `0039AC3FB33439C300909DDBB4A9F09BB47FF67AE72327155784144783EF52A8` |
| `fire-spark-v3-1-3d-05-ray-two-stages.png` | 同射线双阶段直射对照图（近枪口 `X=22` 第 1 帧 + 中段 `X=56` 第 8 帧同轴直射，验证侧方视角下仍严格锁定发射方向、不随相机偏转） | 71,835 | `7280F8D7ACFFB13D50881AC04340E8F119DA275D6F006218FA3CC4312844C689` |
| `visual-handoff.md` | 完整视觉接入交接参数记录（含复用贴图哈希、空间换算、UV 映射、第一人称几何校核与非运行辅助边界） | 12,296 | `1AB95793AD73834B0E3B9D1BB95E49A6E805F33950690EAC2B36E1DA2B2377AB` |
| `manifest.json` | 交付物结构化清单与哈希索引 | 5,019 | `FE1B2317F8983007F84D6621B1405C630A73DB493525F6CB9F12765B5939FE99` |

## 5. 当前状态与后续流转

- **美术预览状态**：候选过程归档已完成真实 Blockbench 建模与多视角视口导出，**待用户视觉确认**。
- **下一步**：交回主会话汇总展示给用户确认受影响的三维空间表现；待用户确认后，再按流程执行正式资源库归档或运行端微调闭环。
