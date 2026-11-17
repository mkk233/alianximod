# 左上角生存资源条（青莲花内溢雾+多层环花流云 + 水势未满静态/满势流波）v6 精修候选 — 视觉接入交接（候选稿）

- **Blockbench 适用性**：不适用。左上角生存资源条仅在二维平面 HUD 左上角显示，无三维空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动姿态关系。
- **关联术式 B / 原 batch**：无 `docs/术式制作表.xlsx` 条目（HUD 界面常驻资源）；沿用批次 `hud-bars-20261110`（`taskId`: `hud_bars_v3_art_preview`，`attemptId`: `20261113-v6-lotus-overflow-water-fullflow`；当前正式复审计数维持：设计 `0/2`、实现 `0/3`）。
- **资源状态**：**待确认（过程归档候选版 `v6-candidate`）**。未经用户同版本确认前，不得列为正式采用版本，不得迁入 `docs/资源库/界面/` 正式目录，也不得复制到 `src/main/resources/assets/xueximod/` 替换现行 `resource_bars_v2`。
- **版本**：`resource-bars-lotus-overflow-water-fullflow-v6-candidate`
- **确认**：待用户确认（确认 `v6` 两项定向优化：① 法力青莲图标新增「从花杯深处与瓣谷向外漫溢翻涌的灵雾」+「上肩/中腰/底托三层环绕花朵流云」；② 水势 `8` 号图标改为 `<100` 纯静态、满 `100/100`（`WATER_UPPER_BOUND = 100`）切换为三道 S 水脉正弦行波+白炽浪锋+顺流飞沫跃动的明显动态流动；并保留已认可的双阶梯中式回字纹槽体及条内流云/三档水流填充动画）。

## 1. 二维源、导出与预览核对清单

### 1.1 参考概念与局部特写标准图（只读复刻自 `v5/ref/`，SHA-256 完全一致）
- `ref/crop_ref_D_lotus.png`（`426×257`，`171299 B`，图二 D 局部放大标准图）：`8d558063d688b4d8f7f166127e50c6e51aaad89ea86aa27cd60ca4841a59c907`
- `ref/crop_ref_8_water.png`（`302×373`，`150671 B`，图三 `8` 号局部放大标准图）：`78233d54c9e22fb40ff2f120932a2b3ff5556d8a792b88f148a4ad18ea17d38c`
- `ref/ref_sheet2_eastern_BD.png`（`1774×887`，`1591053 B`，图二原版总表）：`1fd9377bee3dae50bf00c2e8d3acb1c8cf4b6cf073ff7dfc636739c2f4e66b76`
- `ref/ref_sheet3_icons_6_8_11.png`（`1774×887`，`1506425 B`，图三原版总表）：`4db3d016fb8778ac79bffbb85582195baf901b2fd9f2555a85b7163e0b1b1aa1`

### 1.2 二维可编辑源（Aseprite v1.3.17，RGB）
- `src/mana_lotus_icon_v6.aseprite`：`68×58`（较 `v5` 微扩 `4×2px` 偶数安全边距，彻底消除左右环花卷云硬切边），纯透明底 6 图层（`mirror_ring_and_pendants`、`rear_embracing_clouds`、`back_crown_and_wing_petals`、`inner_cup_overflow_mist`、`front_cupping_petals_and_core`、`front_overflow_and_embracing_clouds`），16 帧 @ `90ms`（周期 `1.44s` 无缝循环），`134627 B`，SHA-256 `e516b03496f6bd5198c6f6cc94306b1d9328adf9ce7dae4a7eeb5051fc84a6c8`
- `src/water_stream8_icon_v6.aseprite`：`48×40`，纯透明底 3 图层（`clean_sapphire_aura_cushion`、`three_undulating_s_streams`、`whitewater_crests_and_spray`），16 帧 @ `90ms`（展示 `100/100` 满水势正弦行波动态源），`36883 B`，SHA-256 `25ddd4adecc9c33ff411ad7e2804a091d8878653f873830c5ceb931bec03e840`
- `src/hud_bars_v6_components.aseprite`：`576×268`，4 图层（`preview_dark_backing`、`fretwork_tracks_and_clasps`、`cloud_and_water_fills_16f`、`lotus_and_stream8_icons_16f`），16 帧 @ `90ms`，`565036 B`，SHA-256 `c15d224ad8a909cd1a61d00f5c7ddc877ea12a564f45bdd0d932da897d508c67`
- `src/hud_context_day_night.aseprite`：`1092×570`，1 帧（`2x` 原生精度昼夜实装双列视口源），`70959 B`，SHA-256 `e74ab2c25ab76889ea5bb2c0e5a437642efe91d609728e6cb8255eecbcea2eb0`
- `src/states_progression.aseprite`：`980×628`，1 帧（`2x` 原生精度五态阶梯昼夜全览源），`77646 B`，SHA-256 `7246e9c0b427015126fe9180a3f2c0c86102db361939ec5ac9b42a15f8b4303b`
- `src/lowkey_flow_preview.aseprite`：`716×360`，16 帧 @ `90ms`（`2x` 原生精度动态对比源），`651447 B`，SHA-256 `3bff8cbcfdcc94096c1c11e4276c33bb141d8e8b119c551c5f4facffca5bcd80`
- 生成脚本：`src/build_hud_bars_v6.lua`（`120869 B`），SHA-256 `48f2206019b85851f32fb800733e6e3ca82559e66c1bc3a63c09d40ce9b7e951`

### 1.3 原尺寸（`2x` 高精像素，HUD `0.5F` 半像素绘制）候选部件导出（干净透明底）
- **法力部件（`68×58` 青莲图标：暖金白/淡紫霞花喉内涌灵雾 + 瓣谷与前唇越肩溢雾 + 上肩 C 卷云/中腰环抱云带/底托双弧云丝三层环花流云，零灰黑阴影气垫；条身与填充完整保留 `v5`/`v4.1`）**：
  - `export/mana_icon_empty.png`（`68×58`，`3107 B`，`0/max` 灵蕊熄暗无溢雾无环云静默态）：`0d79a4f78dd770ea8a5899b24a1f2b31d33818e7609ae30b9e6bd88d74d8a294`
  - `export/mana_icon.png`（`68×58`，`5313 B`，`1..max-1` 常驻溢雾环花首帧代表单图）：`fe7db559c4e39cdf8c77db83f0a3f8d845596f28e3d71b048622638b3b1a55d2`
  - `export/mana_icon_strip.png`（`68×928`，`49381 B`，16 帧 × `58px` 纵向帧条：莲蕊轻柔呼吸，乳白→冰青灵雾自花心与瓣谷向上及左右漫溢而出，三层环花流云舒展流转）：`3af0f3a265e9676c483113ff41d115f498601ff9cdad9db3c7465870cd27e686`
  - `export/mana_icon_full.png`（`68×58`，`5350 B`，`max/max` 满法力丰盈溢雾环花首帧单图）：`17ea9de0abd84cb6254d15c32bb8b5b4be5eb35196af5abdef33dc2786fefea2`
  - `export/mana_icon_full_strip.png`（`68×928`，`51614 B`，16 帧 × `58px` 满法力纵向帧条：金白紫霞灵蕊更饱满，花内溢雾量感与三层环花云带更丰沛清亮）：`60547a9ff4d98631c1dea4a9cdd4b9044fc7de0b608488eecd23a26533eafab0`
  - `export/mana_track.png`（`204×20`，`782 B`，双阶梯中式回字纹桥接槽，与 `v5` 逐字节一致）：`d794835224c9a31711f9ee6c1ce1f74c19e13de526ec7f4d573c0d8543fa73ba`
  - `export/mana_fill_low.png`（`180×10`，`1954 B`，`<=22%` 低竭收敛月影云丝，与 `v5` 逐字节一致）：`d2740621236f816b633d8417455f10cc39bda72c6ba819ec9ae5c2b83209a3e4`
  - `export/mana_fill_strip.png`（`180×160`，`31816 B`，16 帧 × `10px` 常驻大跨度月华流云充盈帧条，与 `v5` 逐字节一致）：`e970363797aff324c054de7e1d3edc9e353c03bfd963687e4ff59dc6c17ddf06`
  - `export/mana_fill_full_strip.png`（`180×160`，`30239 B`，16 帧 × `10px` 满法力凝实流云辉光帧条，与 `v5` 逐字节一致）：`95c3b5f5fb918e4b6332eefb7a5f4a1db65af5f329595bf8ef95f76720ea652d`
- **水势部件（`48×40` 图三 `8` 号三道顺流水纹：严格改为 `<100` 纯静态、满 `100/100` 切换 16 帧行波动态；移除旧 `stream_strip`/`surge_strip`；升级清透深海钴蓝低 Alpha 气垫；槽体与三档流速填充完整保留）**：
  - `export/water_icon_idle.png`（`48×40`，`1183 B`，`balance <= 0` 即 `0/100` **纯静态**深海暗蓝静默态，无白炽锋刃与飞沫）：`ee5ebdc5228ebc5aec99089106afa9c08b11c71b4c0faf01ab0f9eafbb1d363f`
  - `export/water_icon.png`（`48×40`，`1622 B`，`0 < balance < 100` 即 `1..99/100` **纯静态单图**，清晰还原图三 `8` 号三道 S 形水纹与静态飞沫，未满时全程保持静止）：`24d3ee075265497383281324c676bec2b5d176a7b16a815f620b70d1ad23b0d8`
  - `export/water_icon_full.png`（`48×40`，`2062 B`，`balance >= 100` 满水势首帧代表单图）：`2f1e6b15d15935f2162cb646b82a0a88e3a09ca1a7f2aecbaf4d73592bc7dbf5`
  - `export/water_icon_full_strip.png`（`48×640`，`27270 B`，16 帧 × `40px` `balance >= 100` 满水势纵向动态帧条：三道 S 水脉错位正弦行波起伏 `±1.38px` + 背脊推进浪刺 + 纯白激流浪锋贯穿 + 顺流飞沫水珠 `9px` 滑翔新生）：`643b760fe43e93597dbe5ed40eaf53e76f89a87ec63c1ceae53bbf4c517db91b`
  - `export/water_track.png`（`204×20`，`1019 B`，`<20` 基础嵌入式回纹暗扣槽，与 `v5` 逐字节一致）：`3961ab13336acbbbbbe9f92981618085ba9b47edbe56ea2a59cd34925f0cbc11`
  - `export/water_track_states.png`（`204×60`，`1597 B`，3 态纵向合图：`v=0` 为 `<20`，`v=20` 点亮 `tx=53` 即 `20` 门槛云扣，`v=40` 同时点亮 `20` 云扣与 `tx=107` 即 `50` 双咬合主回纹扣，与 `v5` 逐字节一致）：`5e52a96274dd4a636643d72251c664d35a8fb324ec59ea3a78f8df9939a06878`
  - `export/water_fill_tier1_trickle_strip.png`（`180×160`，`4255 B`，16 帧 × `10px` `1..19` 浅层底部慢丝，与 `v5` 逐字节一致）：`3974afbac4fb85b7bb56602fe651a1de5d37dd31a52981a5dfae7cb0ccc949ed`
  - `export/water_fill_tier2_stream_strip.png`（`180×160`，`31261 B`，16 帧 × `10px` `20..49` 满深双叠顺流青碧长带，与 `v5` 逐字节一致）：`ecd0d6d35bdc5905b112cb8a26f3fe705856bef6b5c4fd1e5d70feeb1039f0c1`
  - `export/water_fill_tier3_torrent_strip.png`（`180×160`，`28186 B`，16 帧 × `10px` `50..100` `2x` 双倍频激流白沫长水锋，与 `v5` 逐字节一致）：`d3272643309f9f44b5c5e13a717883931d71eb36906f507445276e76b23d80c4`
  - `export/components_overview_sheet.png`（`576×268`，`37494 B`，仅部件总览核对，不进运行资源）：`59c89d1e9cbed49cb4bba49380092216c943582fdc6422fc35742c3bfac79a87`

### 1.4 二维最近邻预览（严禁作为游戏贴图加载）
- `preview/ref_vs_v6_gate.png`（`1680×1080`，`357057 B`，原参考图 `crop_ref_D_lotus.png` / `crop_ref_8_water.png` vs `v6` 暗底 vs 白昼天空底 `#9EC2E6` + GUI Scale 2 真实 `0.5x` 比例 1:1 严苛门禁对照图）：`3eadff0f87e8c9fb15e539b2a739aac3e9907168f68ee1cebef6d90dbba17347`
- `preview/anim_frames_audit.png`（`1680×1160`，`86877 B`，第 `F01 / F05 / F09 / F13` 四帧并排放大抽帧位移核验图：直接验证青莲花内涌雾/环花流云在暗底与白昼底的位移，以及满水势 `100/100` 图标行波起伏 vs 未满 `68/100` 图标完全静止对照）：`7661bfc5b90723f8bfe6b131e8221e7fd99de13ba8964268b5b00d0de164c7e8`
- `preview/hud_context_day_night_4x.png`（`2184×1396`，`160062 B`，白昼天空草地 + 洞穴深暗双场景、左列 `54/80 + 68/100 STATIC` 与右列 `80/80 + 100/100 FLOW` 四视口实装模拟，底部附 GUI Scale 2 `1:1` 与 GUI Scale 3 `1.5x` 真实 `0.5F` 渲染比例镶嵌窗）：`e681f0a2de194545ecb62f406dded08ee584abebfbf5e69105747aeaa95a7ccc`
- `preview/states_progression_4x.png`（`1960×1256`，`122312 B`，法力五态 `0 / 14 / 54 / 48<-68 施法残影 / 80` 与水势五态 `0 IDLE / 12,35,68 ICON STATIC / 100 ICON FLOWING` 昼夜双底阶梯全览）：`d17d556a0bcffa049c583d4e9a18b1cf74969675a402be5af10288014cb728dc`
- `preview/lowkey_flow_preview_4x.gif`（`1432×720`，`423982 B`，16 帧 × `90ms` 无缝循环动态验收 GIF：同屏对比 `MANA 54/80 (CUP OVERFLOW)`、`MANA 80/80 FULL (EMBRACE)`、`WATER 35/100 (ICON STATIC)`、`WATER 68/100 (ICON STATIC)` 与 `WATER 100/100 FULL (ICON FLOWING)`，右侧附 `LOTUS 80/80 ZOOM` 与 `68 STATIC VS 100 FLOW` 局部放大镜）：`68236b9b16ced7307caaa6ee49cce302c1abbd31269bcc8cdb280159effa93f2`
- `.bbmodel`：不适用（纯二维平面 HUD 界面资源，无三维空间形态、厚度或挂载姿态）。
- 实际三维视口预览：不适用（原因同上）。

## 2. 左上角 `0.5F` 半像素高清绘制坐标、UV 帧布局与分档渲染规则（供用户确认后 Grok 接入参考）

- **渲染矩阵与最近邻原则**：
  - 客户端 `GuiGraphics` 在绘制条部件与图标前执行 `pose.pushPose(); pose.scale(0.5F, 0.5F, 1.0F);`，直接按下方整数 `2x` 像素坐标调用 `blit` 完成绘制后 `pose.popPose()`；数值文本仍按常规 `1.0F` 逻辑字体绘制。在 GUI Scale 2 / 3 / 4 下 `100%` 锐利对齐物理像素，逻辑占地小巧克制（法力图标逻辑尺寸 `34×29`、水势图标 `24×20`、回纹槽 `102×10`、内腔 `90×5`）。
  - 绘制顺序：先画 `track`（`204×20`），再在槽内填充腔 `(trackX + 18, trackY + 5)` 绘制裁剪后的 `fill`（`fillPx × 10`，`fillPx = 0..180`），最后在左侧叠加绘制 `icon`（使青莲右翼瓣与中/下层环花流云自然跨接在回纹左端 `6px` 横舌之上无缝咬合）。
- **法力条默认坐标（`2x` 缩放层内整数像素坐标，括号内为等效 `0.5F` 逻辑 GUI 坐标）**：
  - 图标：`2x` 坐标 `(8, 3)`（逻辑 `(4, 1.5)`，因画布由 `64×56` 对称微扩 `4×2px` 至 `68×58`，左上角较 `v5` 的 `(10, 4)` 左移 `2px`、上移 `1px`，使莲花视觉中心与回纹槽保持原位不变！），尺寸 `68×58`（逻辑 `34×29`）：
    - `current <= 0`：绘制单图 `mana_icon_empty.png`（`68×58`）。
    - `0 < current < max`：从 `mana_icon_strip.png`（总尺寸 `68×928`）按 `u = 0, v = frameIndex * 58` 采样 `68×58`。
    - `current >= max`：从 `mana_icon_full_strip.png`（总尺寸 `68×928`）按 `u = 0, v = frameIndex * 58` 采样 `68×58`。
  - 回纹槽：`2x` 坐标 `(62, 18)`（逻辑 `(31, 9)`），尺寸 `204×20`（逻辑 `102×10`）。
  - 填充腔：`2x` 坐标 `(80, 23)`（槽内偏移 `dx=18, dy=5`），最大尺寸 `180×10`（逻辑 `90×5`）：
    - `current <= 0`：不绘制填充。
    - `0 < current < max`：`fillPx = max(1, min(180, floor(current / max * 180 + 0.5)))`；`current / max <= 0.22` 取 `mana_fill_low.png`，否则从 `mana_fill_strip.png`（`180×160`）按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `current >= max`：从 `mana_fill_full_strip.png`（`180×160`）按 `v = frameIndex * 10` 采样满宽 `180×10`。
  - 数值文本：逻辑坐标 `(137, 10)`，显示 `当前/上限`（建议颜色 `0xE2EEFF` 带阴影）。
- **水势条默认坐标与 `WATER_UPPER_BOUND = 100` 严格状态规则**：
  - 图标：`2x` 坐标 `(16, 62)`（逻辑 `(8, 31)`），尺寸 `48×40`（逻辑 `24×20`）：
    - **`balance <= 0`（`0/100` 空水势）**：绘制**纯静态单图** `water_icon_idle.png`（`48×40`）。
    - **`0 < balance < 100`（`1..99/100` 未满水势，含 T1/T2/T3 未满段）**：绘制**纯静态单图** `water_icon.png`（`48×40`）—— **未满 `100` 时水势图标全程静止，不随帧索引变化！**
    - **`balance >= 100`（`>= ResourceBarLayout.WATER_UPPER_BOUND` 满水势）**：切换为从纵向动态帧条 `water_icon_full_strip.png`（总尺寸 `48×640`）按 `u = 0, v = frameIndex * 40` 采样 `48×40`（16 帧顺流行波动态）！
  - 回纹槽：`2x` 坐标 `(62, 72)`（逻辑 `(31, 36)`），从 `water_track_states.png`（`204×60`）按门槛裁剪 `204×20`：`balance < 20` 取 `v=0`，`20 <= balance < 50` 取 `v=20`（点亮 `tx=53` 处 `20` 云扣），`balance >= 50` 取 `v=40`（同时点亮 `tx=53` 与 `tx=107` 处 `50` 双咬合主扣）。
  - 填充腔：`2x` 坐标 `(80, 77)`（槽内偏移 `dx=18, dy=5`），最大尺寸 `180×10`：
    - `balance <= 0`：不绘制填充。
    - `0 < balance < 20`（T1 涓流）：从 `water_fill_tier1_trickle_strip.png`（`180×160`）按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `20 <= balance < 50`（T2 涌流）：从 `water_fill_tier2_stream_strip.png`（`180×160`）按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `balance >= 50`（T3 激流 / 满势）：从 `water_fill_tier3_torrent_strip.png`（`180×160`）按 `v = frameIndex * 10` 采样 `fillPx × 10`（`balance >= 100` 时 `fillPx = 180` 满腔）。
  - 数值文本：逻辑坐标 `(137, 37)`；水势瞬时增减提示从逻辑 `(40, 49)` 起向下排布。
- **时间 / 循环 / 相位**：
  - 全套动态帧条统一为 `16` 帧，客户端按 `int frameIndex = (int) ((System.currentTimeMillis() / 90L) % 16L)`（`90ms/帧`，总周期 `1.44s`）采样。
- **非静态预览可替代的运行效果**：不同窗口分辨率与 GUI Scale（`1x`–`4x`）下的实机像素对齐、长数字动态排版及帧率表现，须在用户确认并由 Grok 正式接入后于游戏内实测。

## 3. 待用户确认项与后续边界
1. **法力青莲「花内溢雾 + 环花流云」确认**：`68×58` 青莲从花心/瓣谷源源漫溢而出的乳白冰青灵雾，以及上肩/中腰/底托三层环花流云（暗底与白昼天空底均纯净无灰边）是否符合预期？
2. **水势 `8` 号图标「未满静态、满 `100/100` 动态流动」确认**：`1..99` 保持安静静态、达到 `100/100` 后切换为三道 S 水脉正弦行波+白炽浪锋+顺流飞沫动态流动的表现是否符合预期？
3. **后续流程**：待用户明确同版本确认后，先归档至 `docs/资源库/界面/` 正式目录并更新 `docs/资源库/README.md`，再交由 Grok 子代理按上述 `0.5F` 坐标与状态规则接入客户端 HUD 并执行 `compileJava`。
