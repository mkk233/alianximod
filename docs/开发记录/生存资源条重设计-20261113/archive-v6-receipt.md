# 美术正式归档回执：左上角生存资源条 v6（青莲溢雾与满势流波）

## 1. 任务头与授权核对
- **工作区**：`D:/alianximod`
- **batchId**：`hud-bars-20261110`（设计 `0/2`、实现 `0/3`；本次仅做确认版美术正式归档，不计正式复审轮次）
- **taskId**：`hud_bars_v6_archive`
- **attemptId**：`20261113-v6-archive`
- **操作角色**：`art`（确认版正式归档，原代理 `01a1d41f-1c36-7941-aedc-51de8f865935` 续派）
- **用户确认依据**：2026-11-13 用户确认 `v6`，原话「这版效果很好，接入即可，记得让水势UI有发射变化时显示」。
- **正式归档版本 ID**：`resource-bars-lotus-overflow-water-fullflow-v6`
- **源过程归档目录（只读复制，零改动）**：`D:/alianximod/docs/资源库/过程归档/生存资源条_v6_青莲溢雾环花与满势流波_20261113/`
- **目标正式归档目录**：`D:/alianximod/docs/资源库/界面/生存资源条_青莲溢雾与满势流波_v6/`
- **Blockbench 适用性**：不适用。只在二维 HUD 显示，无空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动。

## 2. 独占写路径与禁区边界核对
- **本次仅写入以下三处独占路径**：
  1. `D:/alianximod/docs/资源库/界面/生存资源条_青莲溢雾与满势流波_v6/`（共 34 个文件：33 个原样复制自过程归档 `v6` + 1 份新建正式说明 `README.md`）
  2. `D:/alianximod/docs/资源库/README.md`（在「当前资源」最前追加本版条目并标明「已确认，接入进行中」；将原 `[生存资源条 · 细条 v2](界面/生存资源条_细条_v2/README.md)` 更新为「上一运行版 / 历史运行版」并完整保留链接）
  3. `D:/alianximod/docs/开发记录/生存资源条重设计-20261113/archive-v6-receipt.md`（本回执）
- **禁区零触碰确认**：
  - **未触碰 `D:/alianximod/src/`** 下任何源码或贴图，也未触碰并行接入代理独占的 `integration-v6-receipt.md`；
  - **未修改过程归档 `v6` 源文件**（经脚本核对 `v6/manifest.json` 全部记录，`PROC_V6_MISMATCH=0`）；
  - **未删除或修改旧正式目录 `D:/alianximod/docs/资源库/界面/生存资源条_细条_v2/`**（目录内原有 13 个文件完好保留）；
  - **未修改 `D:/alianximod/docs/术式制作表.xlsx`**，**未执行 `gradlew` 编译**，未重画任何像素。

## 3. 复制文件清单与过程归档 SHA-256 一致性核验（33/33 全部一致，`MISMATCH=0`）

### 3.1 二维可编辑源与构建脚本（`src/`，7 项，与过程归档 SHA-256 `100%` 一致）
- `src/mana_lotus_icon_v6.aseprite`（`134627 B`）：`e516b03496f6bd5198c6f6cc94306b1d9328adf9ce7dae4a7eeb5051fc84a6c8`（一致）
- `src/water_stream8_icon_v6.aseprite`（`36883 B`）：`25ddd4adecc9c33ff411ad7e2804a091d8878653f873830c5ceb931bec03e840`（一致）
- `src/hud_bars_v6_components.aseprite`（`565036 B`）：`c15d224ad8a909cd1a61d00f5c7ddc877ea12a564f45bdd0d932da897d508c67`（一致）
- `src/hud_context_day_night.aseprite`（`70959 B`）：`e74ab2c25ab76889ea5bb2c0e5a437642efe91d609728e6cb8255eecbcea2eb0`（一致）
- `src/states_progression.aseprite`（`77646 B`）：`7246e9c0b427015126fe9180a3f2c0c86102db361939ec5ac9b42a15f8b4303b`（一致）
- `src/lowkey_flow_preview.aseprite`（`651447 B`）：`3bff8cbcfdcc94096c1c11e4276c33bb141d8e8b119c551c5f4facffca5bcd80`（一致）
- `src/build_hud_bars_v6.lua`（`120869 B`）：`48f2206019b85851f32fb800733e6e3ca82559e66c1bc3a63c09d40ce9b7e951`（一致）

### 3.2 原尺寸导出部件与归档核对表（`export/`，19 项，与过程归档 SHA-256 `100%` 一致）
- `export/mana_icon_empty.png`（`68×58`，`3107 B`）：`0d79a4f78dd770ea8a5899b24a1f2b31d33818e7609ae30b9e6bd88d74d8a294`（一致）
- `export/mana_icon.png`（`68×58`，`5313 B`）：`fe7db559c4e39cdf8c77db83f0a3f8d845596f28e3d71b048622638b3b1a55d2`（一致）
- `export/mana_icon_strip.png`（`68×928`，`49381 B`）：`3af0f3a265e9676c483113ff41d115f498601ff9cdad9db3c7465870cd27e686`（一致）
- `export/mana_icon_full.png`（`68×58`，`5350 B`）：`17ea9de0abd84cb6254d15c32bb8b5b4be5eb35196af5abdef33dc2786fefea2`（一致）
- `export/mana_icon_full_strip.png`（`68×928`，`51614 B`）：`60547a9ff4d98631c1dea4a9cdd4b9044fc7de0b608488eecd23a26533eafab0`（一致）
- `export/mana_track.png`（`204×20`，`782 B`）：`d794835224c9a31711f9ee6c1ce1f74c19e13de526ec7f4d573c0d8543fa73ba`（一致）
- `export/mana_fill_low.png`（`180×10`，`1954 B`）：`d2740621236f816b633d8417455f10cc39bda72c6ba819ec9ae5c2b83209a3e4`（一致）
- `export/mana_fill_strip.png`（`180×160`，`31816 B`）：`e970363797aff324c054de7e1d3edc9e353c03bfd963687e4ff59dc6c17ddf06`（一致）
- `export/mana_fill_full_strip.png`（`180×160`，`30239 B`）：`95c3b5f5fb918e4b6332eefb7a5f4a1db65af5f329595bf8ef95f76720ea652d`（一致）
- `export/water_icon_idle.png`（`48×40`，`1183 B`）：`ee5ebdc5228ebc5aec99089106afa9c08b11c71b4c0faf01ab0f9eafbb1d363f`（一致）
- `export/water_icon.png`（`48×40`，`1622 B`）：`24d3ee075265497383281324c676bec2b5d176a7b16a815f620b70d1ad23b0d8`（一致）
- `export/water_icon_full.png`（`48×40`，`2062 B`）：`2f1e6b15d15935f2162cb646b82a0a88e3a09ca1a7f2aecbaf4d73592bc7dbf5`（一致）
- `export/water_icon_full_strip.png`（`48×640`，`27270 B`）：`643b760fe43e93597dbe5ed40eaf53e76f89a87ec63c1ceae53bbf4c517db91b`（一致）
- `export/water_track.png`（`204×20`，`1019 B`）：`3961ab13336acbbbbbe9f92981618085ba9b47edbe56ea2a59cd34925f0cbc11`（一致）
- `export/water_track_states.png`（`204×60`，`1597 B`）：`5e52a96274dd4a636643d72251c664d35a8fb324ec59ea3a78f8df9939a06878`（一致）
- `export/water_fill_tier1_trickle_strip.png`（`180×160`，`4255 B`）：`3974afbac4fb85b7bb56602fe651a1de5d37dd31a52981a5dfae7cb0ccc949ed`（一致）
- `export/water_fill_tier2_stream_strip.png`（`180×160`，`31261 B`）：`ecd0d6d35bdc5905b112cb8a26f3fe705856bef6b5c4fd1e5d70feeb1039f0c1`（一致）
- `export/water_fill_tier3_torrent_strip.png`（`180×160`，`28186 B`）：`d3272643309f9f44b5c5e13a717883931d71eb36906f507445276e76b23d80c4`（一致）
- `export/components_overview_sheet.png`（`576×268`，`37494 B`，已在 `README.md` 标明仅供归档核对、不作为游戏贴图）：`59c89d1e9cbed49cb4bba49380092216c943582fdc6422fc35742c3bfac79a87`（一致）

### 3.3 二维最近邻预览与交接元数据（`preview/` + 根目录，7 项，与过程归档 SHA-256 `100%` 一致）
- `preview/ref_vs_v6_gate.png`（`1680×1080`，`357057 B`）：`3eadff0f87e8c9fb15e539b2a739aac3e9907168f68ee1cebef6d90dbba17347`（一致）
- `preview/anim_frames_audit.png`（`1680×1160`，`86877 B`）：`7661bfc5b90723f8bfe6b131e8221e7fd99de13ba8964268b5b00d0de164c7e8`（一致）
- `preview/hud_context_day_night_4x.png`（`2184×1396`，`160062 B`）：`e681f0a2de194545ecb62f406dded08ee584abebfbf5e69105747aeaa95a7ccc`（一致）
- `preview/states_progression_4x.png`（`1960×1256`，`122312 B`）：`d17d556a0bcffa049c583d4e9a18b1cf74969675a402be5af10288014cb728dc`（一致）
- `preview/lowkey_flow_preview_4x.gif`（`1432×720`，`423982 B`）：`68236b9b16ced7307caaa6ee49cce302c1abbd31269bcc8cdb280159effa93f2`（一致）
- `visual-handoff-candidate.md`（`16565 B`）：`fe68710a457d33fc0d7c3879234369524765fecb6ab3bad775ca8708761614cb`（一致）
- `manifest.json`（`8295 B`）：`eca421880e463200d9e589cdf4595e061f731ca5f9f17ea1b33ff407f1196278`（一致）
- 参考原图未重复复制，已在正式目录 `README.md` 指向 `../../过程归档/生存资源条_v6_青莲溢雾环花与满势流波_20261113/ref/`。

### 3.4 新建正式目录说明与根资源库索引更新
- `D:/alianximod/docs/资源库/界面/生存资源条_青莲溢雾与满势流波_v6/README.md`（`11541 B`）：`41ecafb338a544bfd4b343cf571c9c404639feb73f1d19d3a3df25c2911e656b`
- `D:/alianximod/docs/资源库/README.md`（`28112 B`）：`ad4ce15210edd073ec44245fbcab29b1a12c25452142379190535885fd1191bc`

## 4. 状态说明
- **美术归档状态**：`resource-bars-lotus-overflow-water-fullflow-v6` 已正式归档至 `docs/资源库/界面/生存资源条_青莲溢雾与满势流波_v6/` 并同步更新 `docs/资源库/README.md`。
- **用户确认状态**：2026-11-13 已确认（「这版效果很好，接入即可，记得让水势UI有发射变化时显示」）。
- **源码接入与编译状态**：本归档代理未改 `src/`、未跑 `gradlew`；运行目录预计 `assets/xueximod/textures/gui/resource_bars_v6/`，客户端接入与 `compileJava` 由执行代理独立负责。
