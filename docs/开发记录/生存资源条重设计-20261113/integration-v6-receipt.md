# 生存资源条 v6 接入回执

- batchId：`hud-bars-20261110`（设计 0/2、实现 0/3；本单是已确认美术的接入与编译，不是正式复审，不记复审轮次）
- taskId：`hud_bars_v6_integrate`
- attemptId：`20261113-v6-integrate`
- 确认范围：用户 2026-11-13 确认的 v6「青莲溢雾环花 + 水势未满静态、满 100/100 图标动态流动」

## 改动文件

- 改：`src/main/java/com/bslx/XueXiMod/client/ResourceBarLayout.java`
  - 运行贴图改为 `textures/gui/resource_bars_v6/`。
  - 部件先 `pushPose`、`scale(0.5F, 0.5F, 1.0F)`，按 2x 坐标 blit，再 `popPose`。
  - 法力数字在逻辑 `(137, 10)`，水势数字在逻辑 `(137, 37)`，都在半缩放矩阵之外。
  - 16 帧，`(System.currentTimeMillis() / 90L) % 16`。法力图标帧高 58，满水势图标帧高 40，填充帧高 10。
  - 法力：`current <= 0` 用静态空图标且不画填充；未满时图标用 `mana_icon_strip`，比值 `<= 0.22` 用静态 `mana_fill_low`，否则用 `mana_fill_strip`；满则图标和填充都用 full strip，填充宽 180。
  - 水势：`<= 0` 静态 idle、不画填充、槽 `v=0`；`0 < balance < 20` 静态图标、tier1、槽 `v=0`；`20 <= balance < 50` 静态图标、tier2、槽 `v=20`；`50 <= balance < 100` 静态图标、tier3、槽 `v=40`；`balance >= 100` 才播 `water_icon_full_strip`，tier3 满宽 180，槽 `v=40`。未满不播水势图标动画。
  - `BAR_CAPACITY = 87` 改为 `FILL_WIDTH = 180`。全仓只有 `ResourceBarLayout` 使用旧常量；`filledWidth` 仍是 public，现按 180 计算。没有其他调用点，因此没有旧宽度需要另留。`WATER_UPPER_BOUND` 仍是 100。
  - `waterChangeTop` 返回 `max(52, 37 + font.lineHeight + 1)`。原版字体行高 9 时结果是 52。
- 未改：`MagicHudOverlay.java`。法力仍是缓存加主手法杖存量，上限含法杖加成且至少为 1；文字仍是 `current + "/" + upperBound`，颜色仍由调用处传入 `0xFFFFFF`。
- 未改：`WaterMomentumOverlay.java`。变化行横坐标本来就是 20，纵坐标本来就读 `waterChangeTop`，所以位置跟着新布局走，不必改这个文件。
- 未改：`WaterMomentumNotice.java`、`WaterMomentum.java`、网络协议。
- 新增目录：`src/main/resources/assets/xueximod/textures/gui/resource_bars_v6/`。没有 `.mcmeta`，没有 `blur=true`，没有改成线性过滤。
- 保留：`resource_bars_v2/` 未删除。

## 复制的运行贴图

只从 v6 `export/` 复制下列 14 张，文件名不变，SHA-256 与交接稿一致：

- `mana_icon_empty.png` `0d79a4f78dd770ea8a5899b24a1f2b31d33818e7609ae30b9e6bd88d74d8a294`
- `mana_icon_strip.png` `3af0f3a265e9676c483113ff41d115f498601ff9cdad9db3c7465870cd27e686`
- `mana_icon_full_strip.png` `60547a9ff4d98631c1dea4a9cdd4b9044fc7de0b608488eecd23a26533eafab0`
- `mana_track.png` `d794835224c9a31711f9ee6c1ce1f74c19e13de526ec7f4d573c0d8543fa73ba`
- `mana_fill_low.png` `d2740621236f816b633d8417455f10cc39bda72c6ba819ec9ae5c2b83209a3e4`
- `mana_fill_strip.png` `e970363797aff324c054de7e1d3edc9e353c03bfd963687e4ff59dc6c17ddf06`
- `mana_fill_full_strip.png` `95c3b5f5fb918e4b6332eefb7a5f4a1db65af5f329595bf8ef95f76720ea652d`
- `water_icon_idle.png` `ee5ebdc5228ebc5aec99089106afa9c08b11c71b4c0faf01ab0f9eafbb1d363f`
- `water_icon.png` `24d3ee075265497383281324c676bec2b5d176a7b16a815f620b70d1ad23b0d8`
- `water_icon_full_strip.png` `643b760fe43e93597dbe5ed40eaf53e76f89a87ec63c1ceae53bbf4c517db91b`
- `water_track_states.png` `5e52a96274dd4a636643d72251c664d35a8fb324ec59ea3a78f8df9939a06878`
- `water_fill_tier1_trickle_strip.png` `3974afbac4fb85b7bb56602fe651a1de5d37dd31a52981a5dfae7cb0ccc949ed`
- `water_fill_tier2_stream_strip.png` `ecd0d6d35bdc5905b112cb8a26f3fe705856bef6b5c4fd1e5d70feeb1039f0c1`
- `water_fill_tier3_torrent_strip.png` `d3272643309f9f44b5c5e13a717883931d71eb36906f507445276e76b23d80c4`

未复制预览图、GIF、总览表、aseprite，也未复制单帧代表图 `mana_icon.png`、`mana_icon_full.png`、`water_icon_full.png`、`water_track.png`。

## 水势变化行

水势条仍常驻。`WaterMomentumOverlay.render` 仍先调用 `WaterMomentumNotice.ClientNotice.freshChanges()`，再画结余，然后逐行画增减。

- 入行阈值仍是绝对值大于 0.05，显示 4 秒，最多 4 行。这些都在未改的 `WaterMomentumNotice` 里。
- 正值颜色 `0x7AD7FF`，负值颜色 `0xF2C14E`，格式仍是现有 `signed()` / `plain()`。
- 行坐标仍是逻辑 `x=20`，首行 `y=ResourceBarLayout.waterChangeTop`。新图标逻辑占地约 `x=8..32`、`y=31..51`；交接稿的逻辑 `y=49` 会压住图标，没有采用。首行不小于 52，行距仍是 `font.lineHeight + 1`。
- 变化行在图标下沿之下，不盖法力条、水势条和水势数字。生存和创造都显示；原版 HUD 隐藏或玩家为空时两个 overlay 仍直接返回。

## 编译

- 命令：在 `D:\alianximod` 执行 `.\gradlew.bat compileJava --offline --no-daemon`
- 退出码：`0`
- 结果：`BUILD SUCCESSFUL in 28s`。输出里的 `ResourceLocation` 等 removal 警告来自既有文件，不是本单新代码。
- 未跑游戏，未 `runClient`，未打包。
- 未改 `docs/资源库/`（含 README 和界面正式归档），未改 `docs/术式制作表.xlsx`，未改 v3/v4/v5/v6 过程归档。

## 返修：水势整组改为变化时短暂显示

- attemptId：`20261113-v6-integrate-hide-on-change`
- 用户原话：水势条不要常驻，改成和字一起隐藏；水势数字与法力统一为整数。
- 上文「水势条仍常驻」只记录第一次接入。本节起生效。

### 隐藏条件

- 法力条仍常驻。
- `WaterMomentumOverlay.render` 仍先调 `WaterMomentumNotice.ClientNotice.freshChanges()`。返回空列表就直接返回，不画水势图标、槽、填充、数值和变化行。
- 非空时才画当前 `balance`。档位仍是：`<= 0` 静默；`0 < balance < 20` 流水填充；`20 <= balance < 50` 点亮 20 扣；`50 <= balance < 100` 点亮 20/50 扣且图标静态；`balance >= 100` 图标动态流动。v6 贴图、0.5 缩放和状态规则都没有回退到 `resource_bars_v2`。
- 入行阈值仍是绝对值大于 0.05，显示 4 秒，最多 4 行。最后一条过期后，下一帧 `freshChanges()` 为空，水势整组自动隐藏。
- 登录、重连或 `change` 为 0 的纯余额同步不会入行，因此不会让水势条出现。未改服务端、`WaterMomentumNotice`、`WaterMomentum.java` 和网络协议。
- 变化行仍在水势整组下方：逻辑 `x=20`，首行 `waterChangeTop`（不小于 52），行距 `font.lineHeight + 1`。正值 `0x7AD7FF`，负值 `0xF2C14E`，格式仍是现有 `signed()`。

### 整数规则

- 水势数值用 `Math.round(balance)` 四舍五入到最近整数，文本是 `shown + "/100"`，不再显示小数。
- 水势数值颜色改为法力同款的 `0xFFFFFF`。法力文字仍是整数 `current/upperBound`，没有改成小数。
- 填充宽度和档位仍按未舍入的 `balance` 计算，不按显示用的整数重算。

### 本次改动与编译

- 改：`src/main/java/com/bslx/XueXiMod/client/WaterMomentumOverlay.java`。
- 未改：`ResourceBarLayout.java`。现有 `waterChangeTop` 已经足够，v6 坐标不用动。
- 未改贴图、法力取值、资源库索引和制作表。
- 命令：在 `D:\alianximod` 执行 `.\gradlew.bat compileJava --offline --no-daemon`
- 退出码：`0`
- 结果：`BUILD SUCCESSFUL in 24s`。既有 removal 警告仍在，不是本次引入。
- 上一次接入编译仍是退出码 `0`、`BUILD SUCCESSFUL in 28s`，没有改写成失败。
- 未跑游戏，未打包。
