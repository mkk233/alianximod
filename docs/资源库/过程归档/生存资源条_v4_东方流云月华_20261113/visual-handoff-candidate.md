# 左上角生存资源条（东方流云月华法力 + 三叠流叶/奔流曲水水势）v4.1 精修候选 — 视觉接入交接（候选稿）

- **Blockbench 适用性**：不适用。左上角生存资源条仅在二维平面 HUD 左上角显示，无空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动姿态关系。
- **关联术式 B / 原 batch**：无 `docs/术式制作表.xlsx` 条目（HUD 界面常驻资源）；沿用批次 `hud-bars-20261110`（`taskId`: `hud_bars_v3_art_preview`，`attemptId`: `20261113-v4_1-eastern-polish`；当前正式复审计数维持：设计 `0/2`、实现 `0/3`）。
- **资源状态**：**待确认（过程归档候选版 `v4.1-candidate`）**。未经用户同版本确认前，不得列为正式采用版本，不得迁入 `docs/资源库/界面/` 正式目录，也不得复制到 `src/main/resources/assets/xueximod/` 替换现行 `resource_bars_v2`。
- **版本**：`resource-bars-eastern-moon-cloud-v4_1-candidate`
- **确认**：待用户确认（需用户确认大跨度舒展流云与浓郁月华充盈感法力表现，并在水势主图标「精修 `8` 号：三叠流叶疾水纹」与备选图标「精修 `11` 号：远近透视 S 形奔流长河」中选定图标方案，同时确认四档长流线水势表现与可选施法消耗残影）。

## 1. 二维源、导出与预览核对清单

### 1.1 参考概念图（只读保留）
- `ref/ref_sheet2_eastern_BD.png`（`1774×887`，图二 B 流云气旋条 + D 青莲灵蕊条 + 中式回纹折角细暗框）：`1fd9377bee3dae50bf00c2e8d3acb1c8cf4b6cf073ff7dfc636739c2f4e66b76`
- `ref/ref_sheet3_icons_6_8_11.png`（`1774×887`，图三 `6` 号弯月灵芒、`8` 号三道顺流水纹、`11` 号 S 形曲水激流）：`4db3d016fb8778ac79bffbb85582195baf901b2fd9f2555a85b7163e0b1b1aa1`

### 1.2 二维可编辑源（Aseprite v1.3.17，RGB）
- `src/hud_bars_v4_components.aseprite`：`286×118`，多图层（5 层：`ref_dark_backing`、`mana_icons_and_fret_track`、`mana_cloud_fills_16f`、`water_icons_and_fret_tracks`、`water_stream_tiers_16f`），16 帧，每帧 `90ms`（总周期 `1.44s` 正向无缝循环），SHA-256 `5a2dd70eb8a47a04eb81295151d30543ef474d452a35f0f8522058faab920bcd`
- `src/lowkey_flow_preview.aseprite`：`408×120`，16 帧，每帧 `90ms`（`1.44s` 正向无缝循环），SHA-256 `422b63bd526b7357bf789538f9b42407149b799898fa588b1f81ff2de31db561`
- `src/hud_context_day_night.aseprite`：`506×280`，1 帧，SHA-256 `60cec056f5ea8733407ebd780b376e69447a4747997c7180dd0d52e10d9df3c5`
- `src/icons_closeup.aseprite`：`412×58`，1 帧，SHA-256 `f05c1e10a82ff265ad5d4d0bbffb8805a46465f33c40d772432a16a4169fc2cc`
- `src/states_progression.aseprite`：`448×258`，1 帧，SHA-256 `db6afd90c506f93d35400dfe0de0e4dff126adc21e59a6f9966dd371e90abded`
- 生成脚本：`src/build_hud_bars_v4.lua`，SHA-256 `3d57a1e8845adf7d4be3293133a7b1daeec1d64ac38f2e9062a7b859e66c8c7d`

### 1.3 原尺寸（1x）候选部件导出（透明底，最近邻采样）
- **法力部件（东方流云月华：图三 `6` 号密封深靛描边清辉新月 + 图二 `D` 三瓣青莲灵蕊 + 图二 `B/D` `38px/53px` 大跨度舒展流云与浓郁月华充盈体）**：
  - `export/mana_icon_idle.png`（`18×18`，`0/max` 幽暗残月态，全封闭外描边）：`757ee34db2b49f975cdb2cc692db7d0690db66e889368b5a51778ec823ec69a7`
  - `export/mana_icon.png`（`18×18`，`1..max-1` 清辉新月抱灵蕊常驻态，全封闭外描边）：`9e94c74aa252862a6748646ac56ce753f728cd26de678e4260bbe4f6b267b9d7`
  - `export/mana_icon_full.png`（`18×18`，`max/max` 满盈灵蕊盛放微芒态，全封闭外描边）：`24aefd1af260d90492ea2c0758c5d9eac7b7bd82b63e17ad0d3ba93b4f04c4b7`
  - `export/mana_track.png`（`102×9`，深墨玄青亚光暗框 + 左右两端 `9px` 高中式回纹阶梯折角收口，主体高 `7px`，内腔 `90×5`）：`329b01ce31ade4af48c05a7e6babd229f7d944d1c955fb71f705a9ae8183ec61`
  - `export/mana_fill_low.png`（`90×5`，低竭态 `<=22%` 大跨度收敛月影云丝）：`dcea73c569adb5dea2f04a471e209fcd861e5372f202a5f77d33d7d454dcb3cb`
  - `export/mana_fill_strip.png`（`90×80`，16 帧纵向帧条，每帧 `90×5`，常驻态 `38px/53px` 大跨度舒展流云与饱满月华蓝内芯，整槽仅 2–3 处自然云峰，无等距小卷钩）：`5b83d0317401e65808bd516dae1536a96d421c0155b336a621c1f969cd52f5d1`
  - `export/mana_fill_full_strip.png`（`90×80`，16 帧纵向帧条，每帧 `90×5`，满盈态凝实月华灵气实体 + 舒展柔白冰蓝长云带）：`8a033a283c002bc213eb4149c2a2b357acf3b078abfd7f6ae6ff5e8c636635a7`
- **水势部件（主图标「精修 `8` 号：三叠流叶疾水纹」/ 备选 `alt11`「精修 `11` 号：远近透视 S 形奔流长河」+ 20/50 回纹云扣 + 四档长流线水势）**：
  - `export/water_icon_idle.png`（`18×18`，主图标精修 `8` 号静默暗青态）：`e101d6950c01db119560a11cb438322d1ed71ee746483dbb112fa73a5bdfb960`
  - `export/water_icon.png`（`18×18`，主图标精修 `8` 号「三叠流叶疾水纹」常驻态：中道水脉最宽最长上挑入槽、上道偏左拱起右掠、下道贴腹轻收 + 2 颗微水珠）：`9198ba2b6b163c7d83c960894aabdcf2c1241fa9a4ccbc59e50ae75390550698`
  - `export/water_icon_surge.png`（`18×18`，主图标精修 `8` 号 `>=50` 浪锋亮白激流态）：`30f2530d8c394793235b24889f0c28213a113addfe5fe0ae50241c2e32d4da91`
  - `export/water_icon_alt11_idle.png`（`18×18`，精修 `11` 号备选静默态）：`cf44222d8a27e921458fc503f62c0ed21c9ae4a2929dc2f214e37eb046ad4f22`
  - `export/water_icon_alt11.png`（`18×18`，精修 `11` 号备选「远近透视 S 形奔流长河」常驻态：右上远景 `1–2px` 纤细起势 → 中腰 `4px` 回折蓄劲 → 右下 `12px` 近景奔涌激流与飞沫，单股无分叉）：`e855796f9946609a8fb5985c580735ba5b4334f2d61ef3eb2068a24acc0d27f9`
  - `export/water_icon_alt11_surge.png`（`18×18`，精修 `11` 号备选 `>=50` 激流提亮态）：`6c51ae9e11d54333d560853495f71f867aac1e2bd269ecb2fc2e3a44e734f5ec`
  - `export/water_track.png`（`102×9`，`<20` 未点亮基础回纹槽）：`cd179cbd883bb97f531e9e64263109c35c2d754d99a3abbc03c62d929315ebd1`
  - `export/water_track_states.png`（`102×27`，3 态纵向合图：`v=0` 为 `<20` 未亮，`v=9` 为 `20..49.99` 点亮 `20` 上沿云扣，`v=18` 为 `>=50` 同时点亮 `20` 云扣与 `50` 上下双咬合主回纹扣）：`ff101fbf6cd50fffb6e7ad88dee32edecc3068cbb5717516d4f3593a77b848d8`
  - `export/water_fill_tier1_trickle_strip.png`（`90×80`，16 帧纵向帧条，每帧 `90×5`，`1..19` 底部 `2–3px` `32px` 长跨度柔缓细丝，`1x` 慢速）：`2805f03b8aa31318695bd5a71dac663e33a36886739daad4a744c6b25006c9ed`
  - `export/water_fill_tier2_stream_strip.png`（`90×80`，16 帧纵向帧条，每帧 `90×5`，`20..49` 满深 `5px` `36px–44px` 长跨度双叠顺流青碧水带，`1x` 速）：`bbf323a9382f6ca690cddb7045b608ab5644ca0ae7848b017940bb06b7088a62`
  - `export/water_fill_tier3_torrent_strip.png`（`90×80`，16 帧纵向帧条，每帧 `90×5`，`50..100` `40px–48px` 疾速穿梭长水锋与亮青白泡沫线，`2x` 双倍速）：`8ed77ac4b5acf6301e82b98ff12b978f6c5ebdf2826e01fe76e2d28c2fb117f9`
  - `export/components_overview_sheet.png`（`286×118`，仅部件总览核对，不进运行资源）：`1509769f9f7e3b7cc4b751b31a2734b33e1b300926714399e2dabfe10c79cb02`

### 1.4 二维最近邻预览（严禁作为游戏贴图加载）
- `preview/hud_context_day_night_4x.png`（`2024×1276`，白昼天空地形底 + 洞穴深暗底双场景实装模拟，同屏对照主图标精修 `8` 号与备选 `alt11` 精修 `11` 号，含底部 `1x` 与 `2x` GUI Scale 真实比例镶嵌小窗）：`c708d7e56921efa42e18b1939de7d0f093e9435ef3b0fd0ead1dec8bc935a765`
- `preview/icons_closeup_8x.png`（`1760×1240`，旧版 `v2` `9×9` vs 本版 `v4.1` `18×18` 异形镂空三图标三态在暗洞底与白昼浅蓝底上的 `8x` 无遮挡特写，`1x`/`2x` 原寸对照独立置于各展示框外侧下方窄栏，底部附 `3x`/`2x`/`1x` 与回纹槽口衔接对照）：`ce46c1852db3326223f79d1d8dcb703bf0986c9077f58c9fdd8f24334aa880fd`
- `preview/states_progression_4x.png`（`1792×1032`，法力五态 `0 / 14 / 48 / 48<-68 施法残影 / 80 满盈` 与水势五态 `0 / 12 涓流 / 35 涌流 / 68 激流 / 100 满势` 在暗洞与白昼双底下的阶梯全览）：`c992ac9d0b630cd92b594e79b37ad4ad6b7c6722660d629edefe3168b4147eff`
- `preview/lowkey_flow_preview_4x.gif`（`1632×480`，16 帧 × `90ms` 正向无缝循环动态预览，同屏展示法力半盈/满盈大跨度流云轻移、备选精修 `11` 号水势对照及主方案精修 `8` 号四档流速差异）：`9e0d380adf20175a07e8a18bd7e04c4f0f8d5275797c8af3023d631683222aa2`
- `.bbmodel`：不适用（纯二维平面 HUD 界面资源，无三维空间形态、厚度或挂载姿态）。
- 实际三维视口预览：不适用（原因同上）。

## 2. 左上角坐标、UV 帧布局与分档渲染规则（供用户确认后 Grok 接入参考）

- **屏幕坐标系与绘制顺序**：
  - 以 GUI 缩放后的逻辑像素左上角 `(0, 0)` 为原点，全部使用最近邻过滤（严禁双线性模糊）。
  - 绘制顺序：先画 `track`（`102×9`），随后在槽内填充腔绘制裁剪后的 `fill`（`px × 5`）及液面收口冠/水锋头，最后在左侧叠加绘制 `18×18` 异形镂空 `icon`（槽体左端 `tx=0..2` 的回纹连接桥自然垫在图标右缘 `ix=15..17` 流云气尾 / 顺流水锋之下，无缝咬合）。
- **法力条默认坐标**：
  - 图标：`(6, 3)`，尺寸 `18×18`（`current <= 0` 取 `mana_icon_idle.png`；`0 < current < max` 取 `mana_icon.png`；`current >= max` 取 `mana_icon_full.png`）。
  - 回纹槽：`(21, 7)`，尺寸 `102×9`（其中 `7px` 主体条身位于 `y=8..14`，左右回纹折角耳局部高 `9px` 位于 `y=7..15`）。
  - 填充腔：`(27, 9)`，最大尺寸 `90×5`（槽内偏移 `dx=6, dy=2`）：
    - `current <= 0`：不绘制填充。
    - `0 < current < max`：`fillWidth = max(1, min(90, floor(current / max * 90 + 0.5)))`；若 `current / max <= 0.22` 取 `mana_fill_low.png`，否则从 `mana_fill_strip.png` 按 `v = frameIndex * 5` 采样 `fillWidth × 5`，并在右端叠加圆润亮白「聚气云冠」收口。
    - `current >= max`：画满 `90×5`，从 `mana_fill_full_strip.png` 按 `v = frameIndex * 5` 采样。
  - 数值文本：`(127, 8 + (7 - font.lineHeight) / 2)`，使用游戏字体显示 `当前/上限`（建议文字色 `0xE2EEFF` 或 `0xFFFFFF`）。
- **水势条默认坐标**：
  - 图标：`(6, 22)`，尺寸 `18×18`（确认主图标精修 `8` 号则用 `water_icon*.png`，确认备选精修 `11` 号则用 `water_icon_alt11*.png`；`balance <= 0` 取 `*_idle`，`0 < balance < 50` 取常驻态，`balance >= 50` 取 `*_surge`）。
  - 回纹槽：`(21, 26)`，单态裁剪尺寸 `102×9`（`7px` 主体条身位于 `y=27..33`）。从 `water_track_states.png` 按门槛状态裁剪 `102×9`：`<20` 取 `v=0`，`20 <= balance < 50` 取 `v=9`，`balance >= 50` 取 `v=18`。
  - `20` 与 `50` 回纹云扣整除对位：内腔宽 `90px` 时，`20/100` 精确对应 `18px`（落点在槽内 `tx=23`，屏幕 `x=44`），`50/100` 精确对应 `45px`（落点在槽内 `tx=50`，屏幕 `x=71`），零舍入误差！刻度扣已绘制在槽贴图内，代码无需另画竖线。
  - 填充腔：`(27, 28)`，最大尺寸 `90×5`：
    - `balance <= 0`：不绘制填充。
    - `0 < balance < 20`（涓流）：从 `water_fill_tier1_trickle_strip.png` 采样当前帧（仅占槽底 `2–3px`，慢速 `1x`，前端低平圆润）。
    - `20 <= balance < 50`（涌流）：从 `water_fill_tier2_stream_strip.png` 采样当前帧（升满 `5px`，长跨度双叠顺流青碧水带 `1x` 速推进，前端单叠顺水尖）。
    - `balance >= 50`（激流 / 满势）：从 `water_fill_tier3_torrent_strip.png` 采样当前帧（`2x` 双倍频长水锋急湍推进，夹带亮青白泡沫线，前端双叠锋水头或 `100` 满势封顶）。
  - 数值文本：`(127, 27 + (7 - font.lineHeight) / 2)`；水势瞬时增减提示从 `y = 38` 起向下排布，总左上角包络约 `121×37`（含增减提示约 `121×43`）逻辑像素，小巧低调不遮视野。
- **时间 / 循环 / 相位**：
  - 共 `16` 帧，客户端建议按 `(System.currentTimeMillis() / 90L) % 16`（`90ms/帧`，周期 `1.44s`）采样纵向帧条 `v = frameIndex * 5`。预览帧速仅代表常驻 UI 呼吸/流动观感，与施法时长无关。
- **非静态预览可替代的运行效果**：不同显示器分辨率下的 GUI Scale（`1x`–`4x`）、动态长数字文本宽度、原版 HUD 隐藏开关及实机帧率观感，须在用户确认并由 Grok 正式接入后于游戏内实测。

## 3. 待用户确认项与后续边界
1. **法力表现确认**：「新月抱三瓣青莲灵蕊·流云徽（`18×18` 全封闭深靛墨边）」+ 中式回纹槽 + 大跨度舒展流云与浓郁月华蓝充盈内芯是否满意？是否需要实装施法扣减时的淡月华流散残影（`48<-68` 示意）？
2. **水势图标二选一**：偏好主图标 **「精修 `8` 号：三叠流叶疾水纹」**（纺锤/柳叶包络错落三水脉 + 2 颗微水珠）还是备选 **「精修 `11` 号：远近透视 S 形奔流长河」**（单股无分叉远近宽窄渐变 S 形激流）？
3. **后续流程**：待用户明确选定确认后，先归档至 `docs/资源库/界面/` 正式目录并更新索引，再交由 Grok 子代理接入 `ResourceBarLayout.java` 与 `assets/xueximod/textures/gui/` 并执行 `compileJava`。
