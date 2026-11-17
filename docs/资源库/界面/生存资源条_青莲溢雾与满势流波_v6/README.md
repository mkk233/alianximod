# 生存资源条 · 青莲溢雾与满势流波 v6

2026-11-13 用户确认「这版效果很好，接入即可，记得让水势UI有发射变化时显示」，并补充明确语义「我的需求是水势条不要常驻改成隐藏和字统一」。默认放在屏幕左上角，采用 `2x` 高精像素配客户端 `pose.scale(0.5F, 0.5F, 1.0F)` 半像素锐利绘制（条身内腔逻辑高 5 像素、外框逻辑高 10 像素）。**法力条常驻**于上方，采用盛放冰蓝青莲「花内溢雾 + 上肩/中腰/底托三层环花流云」；**水势图标、槽、填充和数值默认隐藏**，只在发射或结算导致水势变化且绝对值大于 `0.05` 时显示 `4` 秒，最多 `4` 行，过期后整组隐藏，登录同步不显示水势条。显示期间水势采用三道 S 形顺流水纹图标（`0` 静默、`1–99` 图标静态、满 `100/100` 图标切换为动态流动），水势双阶梯回纹槽标出 `20` 与 `50` 嵌入式云扣，条内大跨度流云与三档水流填充动画完整保留。水势数值为四舍五入整数，显示 `current/100`，颜色与法力数值一致；正负变化行仍用青色与琥珀色。此版本取代上一运行版 [生存资源条 · 细条 v2](../生存资源条_细条_v2/README.md)（未采用的粗条 [生存资源条 v1](../生存资源条_v1/README.md) 亦保留）。

Blockbench 适用性：不适用。只在二维 HUD 显示，无空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动。

版本：resource-bars-lotus-overflow-water-fullflow-v6

确认：用户，2026-11-13，原话「这版效果很好，接入即可，记得让水势UI有发射变化时显示」「我的需求是水势条不要常驻改成隐藏和字统一」

参考原图与局部裁切标准保留在过程归档 [ref/](../../过程归档/生存资源条_v6_青莲溢雾环花与满势流波_20261113/ref/)（`crop_ref_D_lotus.png`、`crop_ref_8_water.png`、`ref_sheet2_eastern_BD.png`、`ref_sheet3_icons_6_8_11.png`），不进游戏运行资源。

二维源（均由 Aseprite v1.3.17 与 [src/build_hud_bars_v6.lua](src/build_hud_bars_v6.lua) 批处理生成）：

- [src/mana_lotus_icon_v6.aseprite](src/mana_lotus_icon_v6.aseprite)：68×58，RGB，6 图层纯透明底，16 帧 @ 90ms（1.44s 循环）。SHA-256 `e516b03496f6bd5198c6f6cc94306b1d9328adf9ce7dae4a7eeb5051fc84a6c8`。
- [src/water_stream8_icon_v6.aseprite](src/water_stream8_icon_v6.aseprite)：48×40，RGB，3 图层纯透明底，16 帧 @ 90ms（满水势 100/100 动态源）。SHA-256 `25ddd4adecc9c33ff411ad7e2804a091d8878653f873830c5ceb931bec03e840`。
- [src/hud_bars_v6_components.aseprite](src/hud_bars_v6_components.aseprite)：576×268，RGB，4 图层，16 帧 @ 90ms。SHA-256 `c15d224ad8a909cd1a61d00f5c7ddc877ea12a564f45bdd0d932da897d508c67`。
- [src/hud_context_day_night.aseprite](src/hud_context_day_night.aseprite)：1092×570，1 帧。SHA-256 `e74ab2c25ab76889ea5bb2c0e5a437642efe91d609728e6cb8255eecbcea2eb0`。
- [src/states_progression.aseprite](src/states_progression.aseprite)：980×628，1 帧。SHA-256 `7246e9c0b427015126fe9180a3f2c0c86102db361939ec5ac9b42a15f8b4303b`。
- [src/lowkey_flow_preview.aseprite](src/lowkey_flow_preview.aseprite)：716×360，16 帧 @ 90ms。SHA-256 `3bff8cbcfdcc94096c1c11e4276c33bb141d8e8b119c551c5f4facffca5bcd80`。
- [src/build_hud_bars_v6.lua](src/build_hud_bars_v6.lua)：120869 字节。SHA-256 `48f2206019b85851f32fb800733e6e3ca82559e66c1bc3a63c09d40ce9b7e951`。

二维导出与候选运行部件（`2x` 原尺寸透明底）：

- [export/mana_icon_empty.png](export/mana_icon_empty.png) 68×58（`0/max` 灵蕊熄暗无云雾静默态），`0d79a4f78dd770ea8a5899b24a1f2b31d33818e7609ae30b9e6bd88d74d8a294`
- [export/mana_icon.png](export/mana_icon.png) 68×58（`1..max-1` 常驻首帧单图），`fe7db559c4e39cdf8c77db83f0a3f8d845596f28e3d71b048622638b3b1a55d2`
- [export/mana_icon_strip.png](export/mana_icon_strip.png) 68×928（`1..max-1` 16 帧竖条：花喉瓣谷向外漫溢灵雾 + 三层环花流云），`3af0f3a265e9676c483113ff41d115f498601ff9cdad9db3c7465870cd27e686`
- [export/mana_icon_full.png](export/mana_icon_full.png) 68×58（`max/max` 满法力首帧单图），`17ea9de0abd84cb6254d15c32bb8b5b4be5eb35196af5abdef33dc2786fefea2`
- [export/mana_icon_full_strip.png](export/mana_icon_full_strip.png) 68×928（`max/max` 16 帧竖条：满法力丰盈花内溢雾与三层环花流云），`60547a9ff4d98631c1dea4a9cdd4b9044fc7de0b608488eecd23a26533eafab0`
- [export/mana_track.png](export/mana_track.png) 204×20（双阶梯中式回字纹桥接槽），`d794835224c9a31711f9ee6c1ce1f74c19e13de526ec7f4d573c0d8543fa73ba`
- [export/mana_fill_low.png](export/mana_fill_low.png) 180×10（`<=22%` 低竭收敛月影云丝），`d2740621236f816b633d8417455f10cc39bda72c6ba819ec9ae5c2b83209a3e4`
- [export/mana_fill_strip.png](export/mana_fill_strip.png) 180×160（16 帧 × 10px 常驻大跨度月华流云充盈动画），`e970363797aff324c054de7e1d3edc9e353c03bfd963687e4ff59dc6c17ddf06`
- [export/mana_fill_full_strip.png](export/mana_fill_full_strip.png) 180×160（16 帧 × 10px 满法力凝实流云辉光动画），`95c3b5f5fb918e4b6332eefb7a5f4a1db65af5f329595bf8ef95f76720ea652d`
- [export/water_icon_idle.png](export/water_icon_idle.png) 48×40（`balance <= 0` 深海暗蓝静默静态图），`ee5ebdc5228ebc5aec99089106afa9c08b11c71b4c0faf01ab0f9eafbb1d363f`
- [export/water_icon.png](export/water_icon.png) 48×40（`1..99` 未满水势**纯静态**单图，未满 100 时全程静止），`24d3ee075265497383281324c676bec2b5d176a7b16a815f620b70d1ad23b0d8`
- [export/water_icon_full.png](export/water_icon_full.png) 48×40（`balance >= 100` 满水势首帧代表图），`2f1e6b15d15935f2162cb646b82a0a88e3a09ca1a7f2aecbaf4d73592bc7dbf5`
- [export/water_icon_full_strip.png](export/water_icon_full_strip.png) 48×640（`balance >= 100` 满水势 16 帧竖条：三道 S 水脉正弦行波起伏 + 白炽浪锋 + 顺流飞沫滑翔），`643b760fe43e93597dbe5ed40eaf53e76f89a87ec63c1ceae53bbf4c517db91b`
- [export/water_track.png](export/water_track.png) 204×20（`<20` 基础回纹暗扣槽），`3961ab13336acbbbbbe9f92981618085ba9b47edbe56ea2a59cd34925f0cbc11`
- [export/water_track_states.png](export/water_track_states.png) 204×60（3 态纵向合图：`v=0` 为 `<20`，`v=20` 点亮 `tx=53` 即 `20` 云扣，`v=40` 点亮 `20` 与 `tx=107` 即 `50` 双咬合主回纹扣），`5e52a96274dd4a636643d72251c664d35a8fb324ec59ea3a78f8df9939a06878`
- [export/water_fill_tier1_trickle_strip.png](export/water_fill_tier1_trickle_strip.png) 180×160（16 帧 × 10px，`1..19` 浅层底部涓流慢丝），`3974afbac4fb85b7bb56602fe651a1de5d37dd31a52981a5dfae7cb0ccc949ed`
- [export/water_fill_tier2_stream_strip.png](export/water_fill_tier2_stream_strip.png) 180×160（16 帧 × 10px，`20..49` 满深双叠顺流青碧长带），`ecd0d6d35bdc5905b112cb8a26f3fe705856bef6b5c4fd1e5d70feeb1039f0c1`
- [export/water_fill_tier3_torrent_strip.png](export/water_fill_tier3_torrent_strip.png) 180×160（16 帧 × 10px，`50..100` 双倍频激流白沫长水锋），`d3272643309f9f44b5c5e13a717883931d71eb36906f507445276e76b23d80c4`

部件核对总览表 [export/components_overview_sheet.png](export/components_overview_sheet.png)（576×268，SHA-256 `59c89d1e9cbed49cb4bba49380092216c943582fdc6422fc35742c3bfac79a87`）只供归档核对查看，不作为游戏贴图加载。交接参数与元数据亦保留同目录 [visual-handoff-candidate.md](visual-handoff-candidate.md)（`fe68710a457d33fc0d7c3879234369524765fecb6ab3bad775ca8708761614cb`）与 [manifest.json](manifest.json)（`eca421880e463200d9e589cdf4595e061f731ca5f9f17ea1b33ff407f1196278`）。

二维最近邻预览（仅供视觉核验，严禁作为游戏贴图）：

- [preview/ref_vs_v6_gate.png](preview/ref_vs_v6_gate.png) 1680×1080（1:1 参考对照与暗底/白昼天空底门禁图），`3eadff0f87e8c9fb15e539b2a739aac3e9907168f68ee1cebef6d90dbba17347`
- [preview/anim_frames_audit.png](preview/anim_frames_audit.png) 1680×1160（F01/F05/F09/F13 四帧位移审计图），`7661bfc5b90723f8bfe6b131e8221e7fd99de13ba8964268b5b00d0de164c7e8`
- [preview/hud_context_day_night_4x.png](preview/hud_context_day_night_4x.png) 2184×1396（白昼与深洞双场景 HUD 模拟及 GUI Scale 2/3 镶嵌窗），`e681f0a2de194545ecb62f406dded08ee584abebfbf5e69105747aeaa95a7ccc`
- [preview/states_progression_4x.png](preview/states_progression_4x.png) 1960×1256（法力与水势五态昼夜阶梯全览），`d17d556a0bcffa049c583d4e9a18b1cf74969675a402be5af10288014cb728dc`
- [preview/lowkey_flow_preview_4x.gif](preview/lowkey_flow_preview_4x.gif) 1432×720（16 帧 @ 90ms 循环动态预览），`68236b9b16ced7307caaa6ee49cce302c1abbd31269bcc8cdb280159effa93f2`

.bbmodel：不适用。原因同上。

实际预览：不适用。没有 Blockbench 场景。

## 画面、状态与坐标

客户端绘制条身与图标前执行 `pose.pushPose(); pose.scale(0.5F, 0.5F, 1.0F);`，在 `2x` 高精像素坐标系下按整数坐标 `blit`（先画槽，再画裁剪后的内腔填充，最后叠画左侧图标使其与回纹左舌自然咬合），完成后 `pose.popPose()`，数值与增减提示仍使用常规 `1.0F` 游戏字体绘制：

- **法力条（常驻显示，`2x` 坐标，括号内为等效 `0.5F` 逻辑 GUI 坐标）**：
  - 青莲图标：`2x (8, 3)`（逻辑 `(4, 1.5)`），68×58（逻辑 34×29）。`current <= 0` 取 `mana_icon_empty.png`；`0 < current < max` 从 `mana_icon_strip.png` 按 `v = frameIndex * 58` 采样花内溢雾与三层环花流云动画；`current >= max` 从 `mana_icon_full_strip.png` 按 `v = frameIndex * 58` 采样满法力丰盈溢雾环花动画。
  - 回纹槽：`2x (62, 18)`（逻辑 `(31, 9)`），204×20（逻辑 102×10）。
  - 内腔填充：`2x (80, 23)`（槽内偏移 `dx=18, dy=5`），最多 180×10（逻辑 90×5）。`<=22%` 取静态 `mana_fill_low.png`，常驻取 `mana_fill_strip.png`，满额取 `mana_fill_full_strip.png`。
  - 数值文字：逻辑 `(137, 10)`，显示 `当前/上限`。
- **水势条（默认隐藏、变化时触发显示 `4` 秒，上限 `WATER_UPPER_BOUND = 100`）**：
  - 显隐规则：水势图标、槽、填充和数值默认隐藏，只在发射或结算导致水势变化且绝对值大于 `0.05` 时显示 `4` 秒，变化提示最多 `4` 行，过期后整组隐藏；登录同步不显示水势条。
  - 三叠水纹图标：`2x (16, 62)`（逻辑 `(8, 31)`），48×40（逻辑 24×20）。**`balance <= 0` 取静态 `water_icon_idle.png`；`0 < balance < 100`（`1–99`）取纯静态 `water_icon.png`（未满 100 时图标保持安静静止）；`balance >= 100`（`100/100` 满水势）切换为 `water_icon_full_strip.png` 按 `v = frameIndex * 40` 采样 16 帧动态流动**。
  - 回纹槽：`2x (62, 72)`（逻辑 `(31, 36)`），从 `water_track_states.png` 按 `<20` / `20..49.99` / `>=50` 取 `v = 0 / 20 / 40`（`20` 与 `50` 门槛云扣已嵌在槽贴图上）。
  - 内腔填充：`2x (80, 77)`（槽内偏移 `dx=18, dy=5`），最多 180×10（逻辑 90×5），按 `<20`、`20..49.99`、`>=50` 分取三档流速帧条。
  - 数值与变化行：水势数值在逻辑 `(137, 37)`，为四舍五入整数，显示 `current/100`，颜色与法力数值一致；条下方（逻辑 `(40, 49)` 起，最多 `4` 行）正负变化行仍分别使用青色与琥珀色。
- **帧采样**：全部 16 帧动态竖条统一按 `(System.currentTimeMillis() / 90L) % 16`（90ms/帧，周期 1.44s）取样，最近邻显示。

运行目录预计为：`assets/xueximod/textures/gui/resource_bars_v6/`。**注意：客户端接入与代码修改由执行代理独立进行，本正式归档仅完成确认版美术入库，不代表已接入源码或已完成编译。**

静态与二维动态预览不能替代的运行效果：各窗口分辨率与 GUI 缩放（1x–4x）下的实机像素对齐、长数字动态宽度、原版 HUD 隐藏联动及发射/结算水势变化（`|delta| > 0.05`）触发 4 秒显隐的实机节奏，游戏效果由用户实测。
