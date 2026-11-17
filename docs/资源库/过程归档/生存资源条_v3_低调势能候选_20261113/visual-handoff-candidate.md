# 左上角生存资源条（法力 + 水势）v3 低调势能候选 — 视觉接入交接（候选稿）

- **Blockbench 适用性**：不适用。左上角生存资源条仅在二维平面 HUD 左上角显示，无空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动姿态关系。
- **关联术式 B / 原 batch**：无 `docs/术式制作表.xlsx` 条目（HUD 界面常驻资源）；承接批次 `hud-bars-20261110`（当前正式复审计数保持：设计 `0/2`、实现 `0/3`）。
- **资源状态**：**待确认（过程归档候选版 `v3-candidate`）**。未经用户明确选稿确认前，不得列为正式采用版本，不得迁入 `docs/资源库/界面/` 正式目录，也不得复制到 `src/main/resources/assets/xueximod/` 替换现行 `resource_bars_v2`。
- **版本**：`resource-bars-lowkey-v3-candidate`（含方案 A「星辉靛蓝 + 旋聚灵核」与方案 B「暖白金 + 气旋灵珠」两套法力候选，共用「向右翻卷浪头」四档动态水势）。
- **确认**：待用户确认（需用户从法力方案 A / 方案 B 中选定配色与图标方案，并确认浪头水势图标与低调四档流速表现）。

## 1. 二维源、导出与预览核对清单

### 1.1 二维可编辑源（Aseprite v1.3.17，RGB）
- `src/hud_bars_v3_components.aseprite`：`244×96`，多图层（7 层），16 帧，每帧 `90ms`（总周期 `1.44s` 正向无缝循环），SHA-256 `2dff19522e21d6222c541b6b03d563b83b479c6e1a623195eda60e1ea41474d4`
- `src/lowkey_flow_preview.aseprite`：`368×108`，16 帧，每帧 `90ms`（`1.44s` 正向无缝循环），SHA-256 `f9a0fd1d89927bd1f95271e8262c5735a9ed741292d766667fbd7b2dabe95ab6`
- `src/compare_ab_hud_context.aseprite`：`450×286`，1 帧，SHA-256 `51b29eeb871e3a6ec5e1f1da2744faee7d65e22de93b24c095de6fc58c08b4b1`
- `src/icons_closeup.aseprite`：`228×124`，1 帧，SHA-256 `c67faa93a713e759ecedff14194c6d8c2d38883d8d7b70611c415e10c2b39c43`
- `src/states_progression.aseprite`：`420×238`，1 帧，SHA-256 `6e76122bccf1c41bf1661450f5a5e8b6a4958fb982806ace00c3ac3b563d6372`
- 生成脚本：`src/build_hud_bars_v3.lua`，SHA-256 `b19c2133507a59367b9151fb0086c547652fa05ff40f1ecdc5ac116bc14b3032`

### 1.2 原尺寸（1x）候选部件导出
- **法力方案 A（星辉靛蓝 + 旋聚灵核）**：
  - `export/mana_a_icon_idle.png`（`12×12`，空槽暗敛态）：`158fde710f00858a6a3b03db75645f92448c6896debe744c0b4ca024b81521c7`
  - `export/mana_a_icon.png`（`12×12`，常驻态）：`e4a7d01ef013db1e560310a2ce8a50ab0018dc4b885a36945118a5c1ddc92249`
  - `export/mana_a_icon_full.png`（`12×12`，满盈星芒微亮态）：`e6833aa1c570af534f696f376a7b0422c1f8823838b751a2e8a1106ba1f69beb`
  - `export/mana_a_track.png`（`93×6`，午夜星靛凹槽 + 左端嵌合承托箍 + 右端倒角封口）：`cdd18a36d80e30b00b6fbb12b0f50ec2c2500b9d34935e4d290163b0c24d977a`
  - `export/mana_a_fill_low.png`（`88×4`，低竭态 `<=22%`）：`3a7f01df746d271627fac66aaa1528ca22cc9d7ab5997c1194058ad6163e56c4`
  - `export/mana_a_fill_strip.png`（`88×64`，16 帧纵向帧条，每帧 `88×4`，常驻缓慢柔光）：`bc5e2c6ad2ec206c8275f34afe9a8556e9cc4ec3832c13af6eac00e406c2aa04`
  - `export/mana_a_fill_full_strip.png`（`88×64`，16 帧纵向帧条，每帧 `88×4`，满盈通透呼吸）：`ff62348cf39ab2e60d5bb5931e46d7f4223b29fbdc8c1d0ed788d38da157e293`
- **法力方案 B（暖白金 + 气旋灵珠）**：
  - `export/mana_b_icon_idle.png`（`12×12`，空槽暗敛态）：`ca812b2eb52937ab93b32b5255afd1ffe3516e5023660a6b808090ccc6e8df6d`
  - `export/mana_b_icon.png`（`12×12`，常驻态）：`e84a0e1d0b268ec66c7eb55227fb50105f436fc2bb01118e88e1c7763990c3b0`
  - `export/mana_b_icon_full.png`（`12×12`，满盈丹珠微亮态）：`29d7fd68740fa40cdd13d36ad7a7140fc28b00d8051dbbca07d52cb368728b7f`
  - `export/mana_b_track.png`（`93×6`，曜石暖灰凹槽 + 左端嵌合承托箍 + 右端倒角封口）：`4f9d46a3d4bed4655756fd4453b9e19e9a4a132ffcb25f9c1ffbdc64854d7167`
  - `export/mana_b_fill_low.png`（`88×4`，低竭态 `<=22%`）：`4aebd157aa058435d35501ea20bd5f966255036a913630dc68bf58e85f188e38`
  - `export/mana_b_fill_strip.png`（`88×64`，16 帧纵向帧条，每帧 `88×4`，常驻缓慢柔光）：`1857f86a357399f7b088ad9bb5986282bc75f0895a3ff0e2268ae9f42f1b37dc`
  - `export/mana_b_fill_full_strip.png`（`88×64`，16 帧纵向帧条，每帧 `88×4`，满盈象牙白金呼吸）：`c9e8612850c3439e3f57c5492e19bf9341a1ccec0d6a441597cfb1fd8dd50b16`
- **水势共用部件（向右翻卷浪头 + 嵌合玄铁卡扣 + 四档水流）**：
  - `export/water_icon_idle.png`（`12×12`，`0` 静默暗青态）：`5029d2c167ebf0cad12a7c8dae88a6d1a0006e91d971213646244db69e298269`
  - `export/water_icon.png`（`12×12`，`1..49` 常驻翻浪态）：`a1a8e37677f28d1526963b9df3449cb073f1ebc7fb124f4eae62070dad025a1f`
  - `export/water_icon_surge.png`（`12×12`，`>=50` 高势白沫提亮态）：`2798c17c1d13841640d923534fd7ebe2637c3f0d6af9d6911e039c73b806295b`
  - `export/water_track.png`（`93×8`，未过线基础槽）：`69725227b56e0e71013977b6771b4f185c1a752170719d130e840371a76bd014`
  - `export/water_track_states.png`（`93×24`，3 态纵向合图：`y=0..7` 为 `<20` 未亮，`y=8..15` 为 `20..49` 点亮 `20` 上沿单卡扣，`y=16..23` 为 `>=50` 同时点亮 `20` 单卡扣与 `50` 上下双咬合卡扣）：`8a4b52e00da2557700fcf2503b95c75816fb7b0d465fc9e21b443f4a90446962`
  - `export/water_fill_tier1_trickle_strip.png`（`88×64`，16 帧纵向帧条，`1..19` 浅层微流）：`09662748ff8f420558989094d160403f973c1124533f2b8c48fd1fe9d747fe0a`
  - `export/water_fill_tier2_stream_strip.png`（`88×64`，16 帧纵向帧条，`20..49` 满深涌流）：`70bed07d6aa0c8a5663988e62fdbb4d67fdee438c28bd36b34aed1d34f9a940a`
  - `export/water_fill_tier3_torrent_strip.png`（`88×64`，16 帧纵向帧条，`50..100` 二倍频激流）：`82f1919a69b1cf69a60e93d702d9d0f2dfd0f3291c8181d1975813862df578b1`
  - `export/components_overview_sheet.png`（`244×96`，仅总览核对，不进运行资源）：`43a734b2c09aed043464f341f2f2495f3af5e2e1545694c81212b8017cdcfff0`

### 1.3 二维最近邻预览（严禁作为游戏贴图加载）
- `preview/compare_ab_hud_context_4x.png`（`1800×1288`，方案 A/B 在白昼天空地形底、洞穴深暗底及 `1x`/`2x` 真实比例镶嵌窗对照）：`075110565089a5922f10c88a2eabc3728285d6bc72705c3ac59e0b521a851426`
- `preview/icons_closeup_8x.png`（`1824×1124`，旧版 `v2` vs 新版三枚图标常驻/满盈/高势 `8x` 特写及 `1x`/`2x` 嵌合接口检查）：`51fcc50868c0d3825c5f03646b3181846259ae0eef27b98984a6d106ee6f5056`
- `preview/states_progression_4x.png`（`1680×952`，法力 A/B 五态含施法消耗残影 + 水势五阶昼夜双底阶梯对照）：`70576574b0f63832b0de4ee1e2dea47ca2b6f0c60f65c5a516857eeea037f4d7`
- `preview/lowkey_flow_preview_4x.gif`（`1472×432`，16 帧 × `90ms` 无缝循环动态对比）：`87fc5327230dce19f31e957ba8c1402197dc048469938480ca2a529ccf839f14`
- `.bbmodel`：不适用（原因见页首）。
- 实际三维视口预览：不适用（原因见页首）。

## 2. 左上角坐标、UV 帧布局与分档渲染规则（供选稿确认后接入参考）

- **屏幕坐标系**：以 GUI 缩放后的逻辑像素左上角 `(0, 0)` 为原点，全部使用最近邻采样（禁止双线性过滤）：
  - **绘制顺序**：先绘制 `track`，再在其左端绘制 `icon`（槽体左端 `tx=0` 承托暗箍与图标最右列 `x+11` 叠合 `1px`，消除散落缝隙），随后在槽内液体腔绘制裁剪后的 `fill` 及右端液面冠（meniscus）。
  - **法力条默认坐标**：
    - 图标：`(8, 5)`，尺寸 `12×12`（`0` 用 `*_icon_idle`，`1..max-1` 用 `*_icon`，`current >= max` 用 `*_icon_full`）。
    - 槽体：`(19, 8)`，尺寸 `93×6`（垂直方向恰居 `12×12` 图标中间 `6` 行 `y=8..13`）。
    - 填充：`(22, 9)`，最大尺寸 `88×4`（`fillWidth = 0` 不画；`0 < current < max` 按 `max(1, min(88, floor(current / max * 88 + 0.5)))`；`current >= max` 画满 `88`）。
    - 数值文本：`(116, 8 + (6 - font.lineHeight) / 2)`，沿用游戏字体显示 `当前/上限`（方案 A 建议文字色 `0xE0ECFF`，方案 B 建议 `0xFCF5E2` 或保持 `0xFFFFFF`）。
    - 消耗残影（可选视觉增强）：法力减少瞬间记录旧宽度并在约 `0.35s` 内线性衰减绘制浅色残影段，不阻塞主数值结算。
  - **水势条默认坐标**：
    - 图标：`(8, 20)`，尺寸 `12×12`（`balance <= 0` 用 `water_icon_idle`，`0 < balance < 50` 用 `water_icon`，`balance >= 50` 用 `water_icon_surge`）。
    - 槽体：`(19, 22)`，单态裁剪尺寸 `93×8`（其中 `6px` 条身位于 `y=23..28`，`y=22` 为上沿卡扣外包边，`y=29` 为 `50` 双咬合下卡扣外包边）。从 `water_track_states.png` 按 `claspState`（`<20` 取 `v=0`，`20..49.99` 取 `v=8`，`>=50` 取 `v=16`）裁剪 `93×8`。
    - 门槛刻度对位：`20/100` 对应填充 `18px`（落点在槽内 `tx=20`，即屏幕 `x=39`）；`50/100` 对应填充 `44px`（落点在槽内 `tx=46`，即屏幕 `x=65`）。卡扣已嵌在槽贴图内，代码无需另画悬空黄线。
    - 填充：`(22, 24)`，最大尺寸 `88×4`：
      - `balance <= 0`：不绘制填充。
      - `0 < balance < 20`（微流）：取 `water_fill_tier1_trickle_strip.png` 当前帧（顶行留 `1px` 深暗气隙表现为 `3px` 浅水慢流，右端平缓圆钝水舌，无白沫）。
      - `20 <= balance < 50`（涌流）：取 `water_fill_tier2_stream_strip.png` 当前帧（吃满 `4px` 水深，`1` 倍频前倾水脉纹，右端微卷浪唇）。
      - `balance >= 50`（激流 / 满势）：取 `water_fill_tier3_torrent_strip.png` 当前帧（`2` 倍频急湍推进，前段夹带细白水沫丝，未满 `100` 时右端钩卷翻浪尖，满 `100` 时白沫顶满封口）。
    - 数值文本：`(116, 23 + (6 - font.lineHeight) / 2)`；水势增减瞬时提示从 `y = 32` 起向下排布，不遮挡卡扣与主条。
- **时间 / 循环 / 相位**：
  - 动画共 `16` 帧，建议客户端按真实时间 `90ms/帧`（即 `(System.currentTimeMillis() / 90L) % 16` 或渲染 tick 插值等价步进，总周期 `1440ms`）采样纵向帧条 `v = frameIndex * 4`，不绑定施法时长。

## 3. 待确认项与后续衔接边界
- **待用户确认项**：
  1. 法力配色与图标二选一：**方案 A（星辉靛蓝 + 旋聚灵核）** 或 **方案 B（暖白金 + 气旋灵珠）**。
  2. 水势「向右翻卷浪头」图标、嵌合玄铁 `20`/`50` 卡扣及四档槽内流速梯度是否符合预期，是否需要微调深浅或流速。
  3. 施法消耗残影（`48<-68 CAST` 示意）是否在后续代码接入时一并实现。
- **待替换占位**：现行游戏内仍运行已确认的旧版 `assets/xueximod/textures/gui/resource_bars_v2/`；本候选稿未替换任何运行资源。
- **接入回执（待用户选稿确认并授权后由 Grok 补充）**：未接入。
- **Blockbench 不能代表的运行效果**：不同分辨率与 GUI 缩放档位（1x/2x/3x/4x）下的字体行高对齐、窄窗口裁切及原版 `F1` 隐藏 HUD 联动，待确认接入后由用户在游戏内实测。
