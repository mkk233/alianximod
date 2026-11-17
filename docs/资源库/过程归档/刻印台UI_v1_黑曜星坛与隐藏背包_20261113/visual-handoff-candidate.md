# 术式刻印台 UI v1（黑曜紫晶星坛 + 放射方块技能树 + 双态参悟/刻印 + 按需隐藏背包）视觉接入交接候选单

## 1. 核心状态与 Blockbench 适用性声明

- **Blockbench 适用性**：**不适用**。术式刻印台界面（`ImprintDeskScreen`）属于纯二维屏幕容器 GUI 界面；正中央黑曜石砖与紫晶符文祭坛虽采用 2.5D 等轴像素插画表现立体厚度，但运行时完全通过二维 `GuiGraphics` 平面贴图与九宫格/帧条切图绘制，不涉及游戏世界内的三维模型、空间朝向、厚度遮挡、实体挂载或三维骨骼姿态运动。
- **关联任务 / batchId**：`imprint-desk-ui-20261113`（`taskId = imprint_desk_ui_art_preview`，`attemptId = 20261113-v1-altar-tree-preview`）
- **资源状态**：**待确认候选版（`pending_user_confirmation`）**；当前归档于 `docs/资源库/过程归档/刻印台UI_v1_黑曜星坛与隐藏背包_20261113/`，未写入 `src/main/resources/`，待用户确认同版本二维预览后，再正式归档至 `docs/资源库/界面/` 并交执行子代理接入 `ImprintDeskScreen` / `ImprintTreeView` / `ModItems`。
- **版本**：`v1`（`2026-11-13`）
- **确认**：待用户确认本版同版本二维静动态预览（`ref_vs_v1_altar_gate.png`、`preview_study_collapsed_4x.png`、`preview_imprint_open_inv_4x.png`、`altar_subtle_glow_3x.gif`）。
- **`.bbmodel`**：不适用（纯二维 GUI 界面，见上文原因）。
- **实际三维视口预览**：不适用（纯二维 GUI 界面，绑定下述同版本二维导出与最近邻预览）。

---

## 2. 二维源工程、1x 原生导出与二维预览清单（含 SHA-256）

### 2.1 多图层可编辑 `.aseprite` 源（`src/`）
1. `src/imprint_altar_hub_v1.aseprite` — `184×192`，RGBA，8 图层，8 帧 @ `120ms` 循环（总时长 `960ms`）  
   SHA-256：`1d8e5e679080937128f7d9870c9a79b28fb5882cfff828b2324f77bfb65927e6`
2. `src/imprint_study_slips_v1.aseprite` — `16×16`，RGBA，3 图层，3 帧（帧 1 凡胎研习简 / 帧 2 通玄残简 / 帧 3 化境孤本）  
   SHA-256：`f08e677c17b47b11e3acf6cc62a6d5c6caa236c04c43791556d43950efb3a3ce`
3. `src/imprint_altar_widgets_v1.aseprite` — `64×32`，RGBA，4 图层，1 帧  
   SHA-256：`e18ed328d7bb007ddeef27018b2c78a07b91a5751ba5b8ce72237f18e56de3d7`
4. `src/imprint_buttons_v1.aseprite` — `60×60`，RGBA，3 图层，1 帧（纵向 3 态 × `20px`）  
   SHA-256：`cec6195afb4177d5c18e53780dd2a6f41c4be6b935cae3fee5b7cebc83b5319d`
5. `src/imprint_corner_buttons_v1.aseprite` — `52×54`，RGBA，2 图层，1 帧（纵向 3 态 × `18px`）  
   SHA-256：`60b1bf28c675b89d2c3d1d8954c07105738f2e8d79a950ad5e861d5cf531ac97`
6. `src/imprint_widgets_and_drawer_v1.aseprite` — `178×104`，RGBA，6 图层，1 帧  
   SHA-256：`f4e5cb456f17e1f8ed0f4823ca2814e86674b21b2bf5ac90b3c361c992fd65af`
7. `src/imprint_tree_nodes_v1.aseprite` — `192×56`，RGBA，5 图层，1 帧  
   SHA-256：`d8babecb4a2f4b4e61f4f94d877a32431e9f6bfcd397c04a255fd4ccb69f8b01`
8. `src/imprint_screen_border_v1.aseprite` — `48×24`，RGBA，2 图层，1 帧  
   SHA-256：`5d9cdc4f8568a9a846a6316937fb0c13781a369e2c45ff7f09f4740a1b7a81e3`

### 2.2 原尺寸 `1x` 透明 PNG 候选运行导出（`export/`）
| 导出文件 | 像素尺寸 | 说明 | SHA-256 |
| --- | --- | --- | --- |
| `export/altar_hub.png` | `184×192` | 中央黑曜紫晶祭坛静态基准底图 | `9a74f052a8c8d72587b9c99df7d0c345e733c23ad731717778ec1baefa975550` |
| `export/altar_hub_glow_strip.png` | `184×1536` | 中央祭坛 8 帧纵向呼吸发光条（单帧高 `192px`，`120ms`/帧） | `1f6a251c81e570e04a3814f48154ccfe2af1acb3858f064824eb75f656b95630` |
| `export/altar_widgets.png` | `64×32` | 刻印 `→`、参悟 `+`、状态角标 `✓`/`!`/`×`、虚影预览槽紫框合图 | `10e4c074eaafb9534f22cef58e3e8732f6f6c59aa2cc31eef63955fcda388d13` |
| `export/confirm_button_states.png` | `60×60` | 祭坛主操作钮纵向三态（`v=0` 常态、`v=20` 悬停/就绪辉光、`v=40` 禁用） | `fb8e248de9aca6bf781b5100aa05d834dcbf241163a6132572fea0bc402bfded` |
| `export/corner_button_states.png` | `52×54` | 右上角钮纵向三态（`v=0` 常态、`v=18` 悬停、`v=36` 展开激活双层亮紫辉光） | `ae63fa8408ba0c719346acfe2b39477ab8303d34184175c06264b555055502a2` |
| `export/inventory_drawer.png` | `178×104` | 按需展开黑曜紫晶隐藏背包面板（`3×9` 暗岩格 + `4px` 横梁 + `1×9` 快捷栏） | `dc9b9ae2025e8cf3c1e161366d005c1b115fdcedad026704464c341d06abd193` |
| `export/tree_nodes_sheet.png` | `192×56` | 8 种 `22×22` 立体倒角属性石砖 + 3 种 `26×26` 交互框 + 竹简/锁头/节点钉 | `2220e8c24d6c47d30acb189d9e2074b271f4127c0ea01ffc6803e28f1802d709` |
| `export/screen_border_9slice.png` | `24×24` | 全屏冷灰钢石细外框九宫格（`8×8` 角切分） | `7175b9dd90d2f6bf88003bda470495aaf02d1cd98fcf491193e3180613cf1375` |
| `export/tooltip_frame_9slice.png` | `24×24` | 紫晶符文详情悬浮窗九宫格（`8×8` 角切分） | `45a3619aa9eb071e8bf69e8350584d073b7694800c7b5a3b1f59f2e552cc79b9` |
| `export/slip_mortal_icon.png` | `16×16` | 凡胎研习简专属像素图标（古朴斜卷竹简配青绳） | `b49e3d2662f7c405919b01868f4b94cb3ac9820d3a92f22b7c8eb119cc528e38` |
| `export/slip_mystic_icon.png` | `16×16` | 通玄残简专属像素图标（玄纹玉封竹简配紫金绳微光） | `84f2869bc79bec40015388c7c5fca2d90b0c1ce57bc1b07e380e746f5f89bc82` |
| `export/slip_transcendent_icon.png` | `16×16` | 化境孤本专属像素图标（暗金包角紫晶封典） | `0ac64dd82492812a89b9c0bd28afedd0ba1db48562f173cdde1553ae550af088` |

### 2.3 最近邻放大预览交付物（`preview/`）
1. `preview/ref_vs_v1_altar_gate.png`（`1920×1080`）— 目标参考图 vs 本版 Aseprite 组件 1:1 门禁对照合图  
   SHA-256：`f77f4f669d7ad5e86dddef0ded0842971da08a473a9fd9c99fb207433927c6a3`
2. `preview/preview_study_collapsed_4x.png`（`1920×1080`，逻辑 `480×270` @ `4x` 最近邻）— 平时隐藏背包收起态 · 参悟模式全屏预览  
   SHA-256：`305b2e14f4e6be185a993379d7a829b99bba8255e757d8b1c45a29445d395f6c`
3. `preview/preview_imprint_open_inv_4x.png`（`1920×1080`，逻辑 `480×270` @ `4x` 最近邻）— 点击展开隐藏背包态 · 刻印与五行相生预览  
   SHA-256：`863b9c7dd6d0c7406d31634ec3f04404217fd8f89f6aea498b194193c9ffef4f`
4. `preview/altar_subtle_glow_3x.gif`（`840×708`，逻辑 `280×236` @ `3x` 最近邻，8 帧 @ `120ms` 无缝循环）— 祭坛与可参悟节点克制微光呼吸预览  
   SHA-256：`325da32bda321e4c111254fcb816e0fbe8f279bccd2a017b7ea8662c775da495`

---

## 3. 精确像素坐标、槽位偏移与 UV 切图规范（`1x` GUI 逻辑像素）

### 3.1 中央黑曜紫晶祭坛（`altar_hub.png` / `altar_hub_glow_strip.png`）
- **贴图宽高**：静态图 `184×192`；动态呼吸纵向条 `184×1536`（共 `8` 帧，每帧 `u = 0, v = frameIndex * 192, w = 184, h = 192, texW = 184, texH = 1536`）。
- **时间与相位**：每帧 `120ms`（约 `2.4 client ticks`，`frameIndex = (int)((Util.getMillis() / 120L) % 8L)`），余弦平滑循环，仅顶端八面体紫晶、四座元素塔晶簇、四角微晶灯与能量导管柔和呼吸。
- **祭坛中心锚点**：贴图局部中心 `(92, 96)`。设屏幕祭坛左上角为 `(ax, ay) = (hubCenterX - 92, hubCenterY - 96)`：
  - **嵌入式暗岩操作台面外框**：局部 `(46, 56)`，宽高 `92×68`（顶部雕花门楣延伸至 `y = 51..55`）。
  - **顶部标题横匾区**：局部 `(53, 59)`，宽高 `78×12`；标题文字水平居中于 `ax + 92`，推荐基线/顶边 `ay + 61`（绘制 `"原胚刻印"` 或 `"参悟 · 凡胎/通玄/化境"`）。
  - **左输入凹槽（外框 `18×18` / 内腔 `16×16`）**：
    - `18×18` 暗岩凹槽左上角：局部 `(63, 73)`；
    - **`16×16` `Slot 0`（原胚输入槽）精确坐标**：局部 **`(64, 74)`**，相对祭坛中心 `(92, 96)` 偏移 **`(dx = -28, dy = -22)`**！
  - **中轴双模式连接符区（`→` 或 `+`）**：
    - 局部桥接区 `(81, 73)`，宽高 `22×18`（中心 `(92, 82)`）：
    - 刻印态箭头（`16×12`）：贴于局部 `(84, 76)`（相对中心 `(dx = -8, dy = -20)`）；
    - 参悟态十字号（`14×14`）：贴于局部 `(85, 75)`（相对中心 `(dx = -7, dy = -21)`）。
  - **右预览/成品凹槽（外框 `18×18` / 内腔 `16×16`）**：
    - `18×18` 暗岩凹槽左上角：局部 `(103, 73)`；
    - **`16×16` `Slot 1`（刻印成品槽 / 参悟研习简虚拟核对槽）精确坐标**：局部 **`(104, 74)`**，相对祭坛中心 `(92, 96)` 偏移 **`(dx = +12, dy = -22)`**！
  - **底部紫晶符文主操作按钮槽（`60×20`）**：
    - 局部 **`(62, 96)`**，宽高 `60×20`，相对祭坛中心 `(92, 96)` 偏移 **`(dx = -30, dy = 0)`**！
- **四方元素塔柱技能树连线起点（相对祭坛中心 `(hubCenterX, hubCenterY)` 的推荐锚点）**：
  - 左上翠木晶塔：`(-42, -66)`
  - 左中紫曜晶塔：`(-64, -26)` 与左下台座 `(-56, +36)`
  - 右上炽火晶塔：`(+42, -66)`
  - 右中寒水晶塔：`(+64, -20)` 与右下侧塔 `(+64, +14)`
  - 下方土/金阶梯台座：`(-24, +68)` 与 `(+24, +66)`
- **祭坛非透明像素安全包络**：距 `(hubCenterX, hubCenterY)` 水平 `|dx| >= 98`（两侧塔外）、对角 `|dx| >= 78, |dy| >= 66`、上下 `|dy| >= 98` 之外不与祭坛任何塔尖重叠。

### 3.2 祭坛局部叠加组件（`altar_widgets.png`，`texW = 64, texH = 32`）
| 子件名 | UV `(u, v)` | 宽高 `(w, h)` | 用途 |
| --- | --- | --- | --- |
| `mode_arrow_idle` | `(0, 0)` | `16×12` | 刻印态未放入有效原胚/未选节点时的暗石右箭头 `→` |
| `mode_arrow_active` | `(16, 0)` | `16×12` | 刻印态满足刻印条件时的紫金发光右箭头 `→` |
| `mode_plus_idle` | `(32, 0)` | `14×14` | 参悟态材料未齐时的暗石十字连接符 `+` |
| `mode_plus_active` | `(48, 0)` | `14×14` | 参悟态原胚与研习简齐备时的亮紫发光 `+` |
| `badge_check` | `(0, 16)` | `7×7` | 槽位右下角 `(slotX + 10, slotY + 10)` 或悬浮窗前置达成绿勾 `✓` |
| `badge_warn` | `(8, 16)` | `7×7` | 材料属性不匹配或等阶不足提示叹号 `!` |
| `badge_cross` | `(16, 16)` | `7×7` | 缺失必需材料红叉 `×` |
| `slot_ghost_aura` | `(32, 16)` | `16×16` | 刻印态右槽为空但已选已学会节点时，叠在半透明（Alpha `0.55~0.60`）成品技能石预览底部的紫晶虚影内框 |

### 3.3 按钮三态纵向合图
1. **`confirm_button_states.png`（`texW = 60, texH = 60`，单态 `60×20`）**：
   - `u = 0, v = 0`：常态（深紫黑曜石面 + 左右竖排亮紫符文）；
   - `u = 0, v = 20`：悬停 / 材料齐备高亮态（紫晶辉光提亮）；
   - `u = 0, v = 40`：禁用态（暗灰玄石面、无发光，文字建议用 `#828094`）。
2. **`corner_button_states.png`（`texW = 52, texH = 54`，单态 `52×18`）**：
   - 推荐摆放在屏幕右上角：`[归位]` 位于 `(width - 118, 8)`，`[背包 / 收起背包]` 位于 `(width - 60, 8)`；
   - `u = 0, v = 0`：常态低调暗黑曜紫石钮；
   - `u = 0, v = 18`：鼠标悬停态；
   - `u = 0, v = 36`：隐藏背包展开激活态（双层亮紫霓虹发光边框，对应文案切换为 `"收起背包"`）。

### 3.4 按需展开的隐藏背包抽屉（`inventory_drawer.png`，`178×104`）
- **摆放位置**：右上按钮正下方 `(drawerX, drawerY) = (width - 184, 28)`（在 `480×270` 窗口下即 `(296, 28)`）。
- **防遮挡响应式偏移规则（`ImprintDeskScreen` 接入重点）**：
  - 收起态（默认 `inventoryOpen == false`）：玩家背包 `36` 个 `Slot` 全部移出屏幕或置为非激活隐藏（`ImprintDeskMenu` 端同步抑制隐藏态槽位点击），`hubBaseCenterX = width / 2`（`480×270` 下为 `240`）；
  - 展开态（`inventoryOpen == true`）：当窗口逻辑宽度 `< 600`（例如 `1920×1080` 下 `guiScale = 4` 的 `480×270`）时，将祭坛与技能树基准中心平滑左移 `DRAWER_OPEN_ALTAR_SHIFT_X = -32`（例如 `236 → 204`），使祭坛右塔外沿停在 `x = 286`，与 `x = 296` 展开的背包抽屉保留 `10px` 纯净星空裂隙，彻底杜绝背包盖死中央祭坛！当 `width >= 600`（`guiScale <= 3`）时无需偏移或仅微移即可完全分离。
- **内部 `18×18` 暗岩物品格精确槽位坐标（相对 `drawerX, drawerY`）**：
  - **主背包 `3×9`（行 `row = 0..2`，列 `col = 0..8`）**：
    - `18×18` 凹槽左上角：`(8 + col * 18, 14 + row * 18)`；
    - **`16×16` `Slot` 物品坐标**：**`(drawerX + 9 + col * 18, drawerY + 15 + row * 18)`**！
  - **紫黑石雕分隔横梁**：局部 `y = 68..71`（高 `4px`）。
  - **快捷栏 `1×9`（列 `col = 0..8`）**：
    - `18×18` 凹槽左上角：`(8 + col * 18, 72)`；
    - **`16×16` `Slot` 物品坐标**：**`(drawerX + 9 + col * 18, drawerY + 73)`**！

### 3.5 放射状立体倒角方块技能树节点表（`tree_nodes_sheet.png`，`texW = 192, texH = 56`）
- **第 0 行（`v = 0`，横向步长 `stride = 24px`，共 8 枚 `22×22` 立体倒角石砖，中心嵌 `16×16` 图标于 `(tileX + 3, tileY + 2)`）**：
  - `index = 0`（`u = 0`）：`WOOD（木）` — 翠绿苔石砖（主体 `#4E9E3E`）
  - `index = 1`（`u = 24`）：`FIRE（火）` — 炽橙熔岩石砖（主体 `#D96B1B`）
  - `index = 2`（`u = 48`）：`WATER（水）` — 深蔚海晶石砖（主体 `#2878C8`）
  - `index = 3`（`u = 72`）：`EARTH（土）` — 赭褐岩土砖（主体 `#8C5828`）
  - `index = 4`（`u = 96`）：`METAL（金）` — 古铜赤金砖（主体 `#C89B3C`）
  - `index = 5`（`u = 120`）：`NONE（无）` — 幽紫玄石砖（主体 `#68528C`）
  - `index = 6`（`u = 144`）：`RULE（规则/动作）` — 琥珀星铜砖（主体 `#A67838`，四角带星铜铆点）
  - `index = 7`（`u = 168`）：`LOCKED（未解锁前置）` — 暗灰封锁石砖（主体 `#2A2B36`，图标以 `0.35` Alpha 暗化绘制）
- **第 1 行（`v = 26`，交互状态叠加框与角标）**：
  - `selected_frame`：`(u = 0, v = 26, w = 26, h = 26)` — 选中态暖金白亮框（相对 `22×22` 节点左上角偏移 `(-2, -2)`）；
  - `studyable_frame`：`(u = 28, v = 26, w = 26, h = 26)` — **可参悟态亮紫青双色符文外框**（偏移 `(-2, -2)`）；
  - `hover_frame`：`(u = 56, v = 26, w = 26, h = 26)` — 悬停微亮白框（偏移 `(-2, -2)`）；
  - `study_scroll_badge`：`(u = 88, v = 26, w = 10, h = 10)` — **可参悟迷你竹简角标**（贴于节点中心右上 `(nodeCenterX + 5, nodeCenterY - 15)`）；
  - `lock_badge`：`(u = 102, v = 26, w = 8, h = 9)` — 未解锁像素小锁头（贴于 `22×22` 暗砖右下 `(tileX + 14, tileY + 12)`）；
  - `node_pip`：`(u = 114, v = 26, w = 8, h = 8)` — 分支转折石钉。

### 3.6 九宫格边框与色值常量
- **`screen_border_9slice.png`（`24×24`，角切分 `corner = 8px`）**：全屏四周冷灰钢石细包边。
- **`tooltip_frame_9slice.png`（`24×24`，角切分 `corner = 8px`）**：详情悬浮框外框，内部底板填充 `#F60E0B1A`（深紫黑半透明底）。
- **推荐 UI 色值常量（ARGB Hex）**：
  - 深空星图底色：`0xFF070A12`（中心微星云柔光 `0xFF171530`）
  - 分支连线色：木 `0xFF5CC64C`、火 `0xFFF07E26`、水 `0xFF3E9EF2`、土 `0xFFB0733A`、金 `0xFFDEB24C`、无 `0xFF9474CE`、规则 `0xFFCC964A`、未解锁 `0xDC42465A`
  - 悬浮窗标题金：`0xFFFFE284`；五行相生增益绿：`0xFF8AFFA6`；参悟消耗紫：`0xFFE8C6FF`

---

## 4. 计划运行资源路径与接入回执占位

- **待替换占位**：现行 `ImprintDeskScreen` 程序化纯色矩形面板与原版灰白槽位底色；现行三档研习简（`study_slip_mortal` / `study_slip_mystic` / `study_slip_transcendent`）暂借的原版纸贴图（若用户一并确认三枚 `16×16` 研习简像素图标）。
- **计划运行资源路径（待用户确认后复制）**：
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/altar_hub.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/altar_hub_glow_strip.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/altar_widgets.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/confirm_button_states.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/corner_button_states.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/inventory_drawer.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/tree_nodes_sheet.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/screen_border_9slice.png`
  - `src/main/resources/assets/xueximod/textures/gui/imprint_desk/tooltip_frame_9slice.png`
  - （可选同步接入物品贴图）`src/main/resources/assets/xueximod/textures/item/study_slip_mortal.png`、`study_slip_mystic.png`、`study_slip_transcendent.png`
- **接入回执（由 Grok 接入者补充）**：**未接入**（本次任务仅制作本地候选像素美术与二维预览，等待用户确认）。
- **迁待验收**：本美术候选交接单不能单独代替迁页证据；待用户确认预览并由执行子代理完成正式归档、代码接入与 `compileJava` 验证后推进。
