# 术式刻印台 UI · 黑曜星坛与隐藏背包 v1

2026-11-13 用户确认「可以按照这版接入」，对应需求「现在重新设计一下刻印台的UI按照最新刻印台功能做适配」「我希望按照现在的思路再刻印界面做成隐藏式的背包栏，玩家需要时再点按钮查看背包」「最终效果应该接近这个」。全屏采用深空星图暗底与冷灰钢石九宫格细外框，正中央锚定 `184×192` 黑曜石砖与紫晶符文 2.5D 等轴星坛（含 8 帧 @ `120ms` 克制紫晶微光呼吸），四周呈放射状展开 `22×22` 立体倒角属性石砖技能树；祭坛台面内嵌左原胚输入槽与右成品预览/核对槽，同台适配「原胚刻印（`→` + 五行相生增益预览）」与「参悟（`+` + 凡胎/通玄/化境研习简核对）」双模式；玩家背包栏默认收起隐藏，点击右上角 `[背包]` 按钮按需展开 `178×104` 黑曜紫晶隐藏背包抽屉（文案切换为 `[收起背包]`，窄逻辑屏 `width < 600` 展开时祭坛与技能树基准自动左移 `-32px` 避免遮挡）；配套提供凡胎研习简、通玄残简、化境孤本 3 枚 `16×16` 专属像素物品图标，取代原程序化纯色矩形面板、原版灰白槽位底色及暂借原版纸贴图。

Blockbench 适用性：不适用。纯二维屏幕容器 GUI 界面（`ImprintDeskScreen`），中央祭坛虽采用 2.5D 等轴像素建筑插画表现立体厚度，但运行时完全通过二维 `GuiGraphics` 平面贴图与九宫格/帧条切图绘制，不涉及三维模型、空间朝向、厚度遮挡、实体挂载或三维姿态运动。

版本：imprint-desk-ui-altar-tree-drawer-v1

确认：用户，2026-11-13，原话「可以按照这版接入」（对应需求「现在重新设计一下刻印台的UI按照最新刻印台功能做适配」「我希望按照现在的思路再刻印界面做成隐藏式的背包栏，玩家需要时再点按钮查看背包」「最终效果应该接近这个」）

参考概念图与目标风格原图保留在过程归档 [../../过程归档/刻印台UI重设计_概念图_20261113/](../../过程归档/刻印台UI重设计_概念图_20261113/)（`user_target_ref_altar_tree.jpg` 及 `concept_A`～`concept_F` 共 7 张探索与参考图），不进游戏运行资源。

二维源与构建脚本（均由 Aseprite v1.3.17 与 [scripts/gen_aseprite_assets.lua](scripts/gen_aseprite_assets.lua) 批处理生成，实装预览由 [scripts/build_previews.py](scripts/build_previews.py) 最近邻合成）：

- [src/imprint_altar_hub_v1.aseprite](src/imprint_altar_hub_v1.aseprite)：184×192，RGBA，8 图层，8 帧 @ 120ms 循环（总时长 960ms，254768 字节）。SHA-256 `1d8e5e679080937128f7d9870c9a79b28fb5882cfff828b2324f77bfb65927e6`。
- [src/imprint_study_slips_v1.aseprite](src/imprint_study_slips_v1.aseprite)：16×16，RGBA，3 图层，3 帧（帧 1 凡胎研习简 / 帧 2 通玄残简 / 帧 3 化境孤本，2061 字节）。SHA-256 `f08e677c17b47b11e3acf6cc62a6d5c6caa236c04c43791556d43950efb3a3ce`。
- [src/imprint_altar_widgets_v1.aseprite](src/imprint_altar_widgets_v1.aseprite)：64×32，RGBA，4 图层，1 帧（1967 字节）。SHA-256 `e18ed328d7bb007ddeef27018b2c78a07b91a5751ba5b8ce72237f18e56de3d7`。
- [src/imprint_buttons_v1.aseprite](src/imprint_buttons_v1.aseprite)：60×60，RGBA，3 图层，1 帧（纵向 3 态 × 20px，3516 字节）。SHA-256 `cec6195afb4177d5c18e53780dd2a6f41c4be6b935cae3fee5b7cebc83b5319d`。
- [src/imprint_corner_buttons_v1.aseprite](src/imprint_corner_buttons_v1.aseprite)：52×54，RGBA，2 图层，1 帧（纵向 3 态 × 18px，1970 字节）。SHA-256 `60b1bf28c675b89d2c3d1d8954c07105738f2e8d79a950ad5e861d5cf531ac97`。
- [src/imprint_widgets_and_drawer_v1.aseprite](src/imprint_widgets_and_drawer_v1.aseprite)：178×104，RGBA，6 图层，1 帧（4126 字节）。SHA-256 `f4e5cb456f17e1f8ed0f4823ca2814e86674b21b2bf5ac90b3c361c992fd65af`。
- [src/imprint_tree_nodes_v1.aseprite](src/imprint_tree_nodes_v1.aseprite)：192×56，RGBA，5 图层，1 帧（3680 字节）。SHA-256 `d8babecb4a2f4b4e61f4f94d877a32431e9f6bfcd397c04a255fd4ccb69f8b01`。
- [src/imprint_screen_border_v1.aseprite](src/imprint_screen_border_v1.aseprite)：48×24，RGBA，2 图层，1 帧（1415 字节）。SHA-256 `5d9cdc4f8568a9a846a6316937fb0c13781a369e2c45ff7f09f4740a1b7a81e3`。
- [scripts/gen_aseprite_assets.lua](scripts/gen_aseprite_assets.lua)：55582 字节。SHA-256 `e0cae75cb0c0da9fe8bcf72fbf92d738caaa5826935a5269a00e80447ec50c36`。
- [scripts/build_previews.py](scripts/build_previews.py)：36988 字节。SHA-256 `2ddbbb54eff97a644dd94b5c4e8abf7b73a1e83e57d63bf0872049fc753c41bc`。

二维导出与候选运行部件（`1x` 原尺寸透明底 PNG，共 12 份，含 9 张刻印台 GUI 贴图与 3 张 `16×16` 三档研习简图标）：

- [export/altar_hub.png](export/altar_hub.png) 184×192（中央黑曜紫晶祭坛静态基准底图），`9a74f052a8c8d72587b9c99df7d0c345e733c23ad731717778ec1baefa975550`
- [export/altar_hub_glow_strip.png](export/altar_hub_glow_strip.png) 184×1536（中央祭坛 8 帧纵向呼吸发光条，单帧高 192px，120ms/帧），`1f6a251c81e570e04a3814f48154ccfe2af1acb3858f064824eb75f656b95630`
- [export/altar_widgets.png](export/altar_widgets.png) 64×32（刻印 `→`、参悟 `+`、状态角标 `✓`/`!`/`×`、虚影预览槽紫框合图），`10e4c074eaafb9534f22cef58e3e8732f6f6c59aa2cc31eef63955fcda388d13`
- [export/confirm_button_states.png](export/confirm_button_states.png) 60×60（祭坛主操作钮纵向三态：`v=0` 常态、`v=20` 悬停/就绪辉光、`v=40` 禁用），`fb8e248de9aca6bf781b5100aa05d834dcbf241163a6132572fea0bc402bfded`
- [export/corner_button_states.png](export/corner_button_states.png) 52×54（右上角钮纵向三态：`v=0` 常态、`v=18` 悬停、`v=36` 展开激活双层亮紫辉光），`ae63fa8408ba0c719346acfe2b39477ab8303d34184175c06264b555055502a2`
- [export/inventory_drawer.png](export/inventory_drawer.png) 178×104（按需展开黑曜紫晶隐藏背包面板：`3×9` 暗岩格 + `4px` 横梁 + `1×9` 快捷栏），`dc9b9ae2025e8cf3c1e161366d005c1b115fdcedad026704464c341d06abd193`
- [export/tree_nodes_sheet.png](export/tree_nodes_sheet.png) 192×56（8 种 `22×22` 立体倒角属性石砖 + 3 种 `26×26` 交互状态外框 + 竹简/锁头/节点钉），`2220e8c24d6c47d30acb189d9e2074b271f4127c0ea01ffc6803e28f1802d709`
- [export/screen_border_9slice.png](export/screen_border_9slice.png) 24×24（全屏冷灰钢石细外框九宫格，`8×8` 角切分），`7175b9dd90d2f6bf88003bda470495aaf02d1cd98fcf491193e3180613cf1375`
- [export/tooltip_frame_9slice.png](export/tooltip_frame_9slice.png) 24×24（紫晶符文详情悬浮窗九宫格，`8×8` 角切分），`45a3619aa9eb071e8bf69e8350584d073b7694800c7b5a3b1f59f2e552cc79b9`
- [export/slip_mortal_icon.png](export/slip_mortal_icon.png) 16×16（凡胎研习简专属像素图标：古朴斜卷竹简配青绳），`b49e3d2662f7c405919b01868f4b94cb3ac9820d3a92f22b7c8eb119cc528e38`
- [export/slip_mystic_icon.png](export/slip_mystic_icon.png) 16×16（通玄残简专属像素图标：玄纹玉封竹简配紫金绳微光），`84f2869bc79bec40015388c7c5fca2d90b0c1ce57bc1b07e380e746f5f89bc82`
- [export/slip_transcendent_icon.png](export/slip_transcendent_icon.png) 16×16（化境孤本专属像素图标：暗金包角紫晶封典），`0ac64dd82492812a89b9c0bd28afedd0ba1db48562f173cdde1553ae550af088`

交接参数与元数据亦保留同目录 [visual-handoff-candidate.md](visual-handoff-candidate.md)（`b7bb498a019a9e3452e9376917a91525c691b858ee98210e9e8749b3cecdc49f`）与 [manifest.json](manifest.json)（`05047494dd6d52127ec713da66f3e54a9cd059e5187b35e48a78d629d293df27`）。

二维最近邻预览（仅供视觉核验，严禁作为游戏贴图）：

- [preview/ref_vs_v1_altar_gate.png](preview/ref_vs_v1_altar_gate.png) 1920×1080（目标参考图 vs 本版 Aseprite 组件 1:1 门禁对照合图），`f77f4f669d7ad5e86dddef0ded0842971da08a473a9fd9c99fb207433927c6a3`
- [preview/preview_study_collapsed_4x.png](preview/preview_study_collapsed_4x.png) 1920×1080（逻辑 `480×270` @ `4x` 最近邻：平时隐藏背包收起态 · 参悟模式全屏预览），`305b2e14f4e6be185a993379d7a829b99bba8255e757d8b1c45a29445d395f6c`
- [preview/preview_imprint_open_inv_4x.png](preview/preview_imprint_open_inv_4x.png) 1920×1080（逻辑 `480×270` @ `4x` 最近邻：点击展开隐藏背包态 · 原胚刻印与五行相生预览），`863b9c7dd6d0c7406d31634ec3f04404217fd8f89f6aea498b194193c9ffef4f`
- [preview/altar_subtle_glow_3x.gif](preview/altar_subtle_glow_3x.gif) 840×708（逻辑 `280×236` @ `3x` 最近邻，8 帧 @ `120ms` 无缝循环：祭坛与可参悟节点克制微光呼吸预览），`325da32bda321e4c111254fcb816e0fbe8f279bccd2a017b7ea8662c775da495`

.bbmodel：不适用。原因同上。

实际预览：不适用。没有 Blockbench 场景。

## 画面、状态与坐标

所有 GUI 组件按 `1x` 逻辑像素整数坐标 `blit` 最近邻绘制，不启用线性过滤：

- **中央黑曜紫晶祭坛（`altar_hub.png` `184×192` / `altar_hub_glow_strip.png` `184×1536`）**：
  - 中心锚点与微光帧条：贴图局部中心 `(92, 96)`，设屏幕左上角 `(ax, ay) = (hubCenterX - 92, hubCenterY - 96)`；纵向 8 帧呼吸条单帧 `184×192`，按 `frameIndex = (int)((Util.getMillis() / 120L) % 8L)`（`120ms`/帧，周期 `960ms`）取 `u = 0, v = frameIndex * 192`，仅顶端八面体紫晶、四座元素晶塔、四角微晶灯与能量导管柔和呼吸。
  - 嵌入式操作台面与标题横匾：暗岩操作台面外框局部 `(46, 56)`、宽高 `92×68`（顶部雕花门楣延伸至 `y = 51..55`）；标题横匾局部 `(53, 59)`、宽高 `78×12`，标题文字水平居中于 `ax + 92`、推荐顶边/基线 `ay + 61`（显示 `"原胚刻印"` 或 `"参悟 · 凡胎/通玄/化境"`）。
  - 左输入槽 `Slot 0`（原胚输入槽）：`18×18` 暗岩凹槽局部 `(63, 73)`；`16×16` 物品槽局部 **`(64, 74)`**，相对祭坛中心 `(92, 96)` 偏移 **`(-28, -22)`**。
  - 中轴双模式连接符区与 `altar_widgets.png`（`64×32`）：桥接区局部 `(81, 73)`、宽高 `22×18`；刻印态箭头 `16×12` 贴于局部 `(84, 76)`（相对中心 `(-8, -20)`，常态取 `(0, 0, 16, 12)`、就绪发光取 `(16, 0, 16, 12)`），参悟态十字号 `14×14` 贴于局部 `(85, 75)`（相对中心 `(-7, -21)`，常态取 `(32, 0, 14, 14)`、齐备发光取 `(48, 0, 14, 14)`）；状态角标 `✓` `(0, 16, 7, 7)`、`!` `(8, 16, 7, 7)`、`×` `(16, 16, 7, 7)` 贴于槽位右下 `(slotX + 10, slotY + 10)` 或悬浮提示；刻印态右槽为空但已选已学会节点时叠画紫晶虚影内框 `(32, 16, 16, 16)` 并配 `0.55~0.60` Alpha 成品预览。
  - 右成品预览 / 参悟核对槽 `Slot 1`：`18×18` 暗岩凹槽局部 `(103, 73)`；`16×16` 物品槽局部 **`(104, 74)`**，相对祭坛中心 `(92, 96)` 偏移 **`(+12, -22)`**。
  - 底部主操作按钮（`confirm_button_states.png` `60×60`，单态 `60×20`）：贴于局部 **`(62, 96)`**，相对祭坛中心 `(92, 96)` 偏移 **`(-30, 0)`**；`v = 0` 常态、`v = 20` 悬停/就绪辉光、`v = 40` 禁用态（禁用文字建议 `#828094`）。
  - 四方元素塔连线起点与安全包络：相对 `(hubCenterX, hubCenterY)` 推荐连线锚点为左上翠木塔 `(-42, -66)`、左中紫曜塔 `(-64, -26)` 与左下台座 `(-56, +36)`、右上炽火塔 `(+42, -66)`、右中寒水塔 `(+64, -20)` 与右下侧塔 `(+64, +14)`、下方土/金阶梯台座 `(-24, +68)` 与 `(+24, +66)`；距中心水平 `|dx| >= 98`、对角 `|dx| >= 78` 且 `|dy| >= 66`、上下 `|dy| >= 98` 之外不与祭坛塔尖重叠。
- **右上角功能按钮（`corner_button_states.png` `52×54`，单态 `52×18`）**：
  - `[归位]` 位于 `(width - 118, 8)`，`[背包 / 收起背包]` 位于 `(width - 60, 8)`；`v = 0` 常态暗紫石钮、`v = 18` 悬停、`v = 36` 隐藏背包展开激活态（双层亮紫辉光边框，按钮文案切换为 `"收起背包"`）。
- **按需展开的隐藏背包抽屉（`inventory_drawer.png` `178×104`）**：
  - 显隐与窄屏防遮挡偏移规则：默认收起（`inventoryOpen == false`），玩家 36 个背包槽移出屏幕或置为非激活隐藏，`hubBaseCenterX = width / 2`；点击展开（`inventoryOpen == true`）时抽屉置于右上按钮下方 `(drawerX, drawerY) = (width - 184, 28)`，当窗口逻辑宽度 `width < 600`（如 `1920×1080` 下 `guiScale = 4` 的 `480×270`）时将祭坛与技能树基准中心左移 `-32px`，使祭坛右塔外沿与抽屉左缘保留约 `10px` 星空裂隙，避免遮挡中央祭坛。
  - 内部物品格坐标（步长 `18px`）：主背包 `3×9`（`row = 0..2, col = 0..8`）的 `18×18` 凹槽起点 `(8 + col * 18, 14 + row * 18)`、`16×16` `Slot` 坐标 **`(drawerX + 9 + col * 18, drawerY + 15 + row * 18)`**；局部 `(8, 68, 162, 4)` 为紫黑石雕分隔横梁；快捷栏 `1×9`（`col = 0..8`）的 `18×18` 凹槽起点 `(8 + col * 18, 72)`、`16×16` `Slot` 坐标 **`(drawerX + 9 + col * 18, drawerY + 73)`**。
- **放射方块技能树节点表（`tree_nodes_sheet.png` `192×56`）**：
  - 第 0 行（`v = 0`，横向步长 `24px`，8 种 `22×22` 立体倒角属性石砖，`16×16` 术式图标嵌于 `(tileX + 3, tileY + 2)`）：依次为 `u = 0` 木（`#4E9E3E`）、`u = 24` 火（`#D96B1B`）、`u = 48` 水（`#2878C8`）、`u = 72` 土（`#8C5828`）、`u = 96` 金（`#C89B3C`）、`u = 120` 无（`#68528C`）、`u = 144` 规则/动作（`#A67838` 星铜铆点）、`u = 168` 未解锁（`#2A2B36` 暗砖，图标 `0.35` Alpha 暗化）。
  - 第 1 行（`v = 26`，交互状态外框与角标）：选中暖金白框 `(0, 26, 26, 26)`、可参悟亮紫青符文框 `(28, 26, 26, 26)`、悬停微亮白框 `(56, 26, 26, 26)` 均相对 `22×22` 节点左上角偏移 `(-2, -2)`；可参悟迷你竹简角标 `(88, 26, 10, 10)` 贴于节点中心右上 `(nodeCenterX + 5, nodeCenterY - 15)`；未解锁小锁头 `(102, 26, 8, 9)` 贴于 `(tileX + 14, tileY + 12)`；分支转折石钉 `(114, 26, 8, 8)`。
- **九宫格边框与推荐色值**：
  - `screen_border_9slice.png`（`24×24`）与 `tooltip_frame_9slice.png`（`24×24`）均按 `corner = 8px` 九宫格切分，悬浮窗底板推荐 `#F60E0B1A`；深空星图底色 `0xFF070A12`（中心星云柔光 `0xFF171530`），分支连线色为木 `0xFF5CC64C`、火 `0xFFF07E26`、水 `0xFF3E9EF2`、土 `0xFFB0733A`、金 `0xFFDEB24C`、无 `0xFF9474CE`、规则 `0xFFCC964A`、未解锁 `0xDC42465A`；悬浮窗标题金 `0xFFFFE284`、五行相生增益绿 `0xFF8AFFA6`、参悟消耗紫 `0xFFE8C6FF`。

计划运行目录为：`assets/xueximod/textures/gui/imprint_desk/` 与三档研习简物品贴图 `assets/xueximod/textures/item/study_slip_mortal.png`、`assets/xueximod/textures/item/study_slip_mystic.png`、`assets/xueximod/textures/item/study_slip_transcendent.png`。**注意：客户端接入与代码编译由并行执行代理独立进行，本正式归档仅完成确认版美术入库，不代表已接入源码或已完成编译。**

静态与二维动态预览不能替代的运行效果：各窗口分辨率与 GUI 缩放（`1x–4x`）下的实机像素对齐、窄屏展开背包抽屉左移过渡、双模式槽位交互与成品虚影核对、技能树平移缩放拖拽及三档研习简背包/手持显示效果，游戏效果由用户实测。
