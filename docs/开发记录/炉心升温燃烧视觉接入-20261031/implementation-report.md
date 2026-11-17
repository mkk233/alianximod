# 炉心升温 / 燃烧视觉接入报告

阶段：实现与必要自查。本轮停止写入。未运行 Gradle，未启动游戏。制作表未改，留给后续 `heat-burn-sheet-writer`。

## 需求来源

制作表「制作中」：`fire_heat_up` 第 3 行，`fire_burn` 第 4 行。N 仍是炉心进入/升级的服务端规则，本轮没有改。旧 O 是短促火星和热浪；用户已批准的 v2 / v3 替换该视觉描述。

- 升温源：`D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v2/`
- 燃烧源：`D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/`
- 批准原件 SHA256 与两份 `manifest.json` 仍一致，本轮没有改这些原件。

## 运行时引用

目录：`D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2/`

升温维持四层由 `heat_up-v2.aseprite` 导出，入场四层由 `heat_up-v2-entry.aseprite` 导出。燃烧维持五张与正式 `*-strip.png` 逐字节相同。燃烧入场五层由 `overdrive-v3-entry.aseprite` 导出。

不加载：预览、人偶、`heat_up-v2-strip.png`、`overdrive-v3-combined-strip.png`、过程归档里的空 `overdrive-v3-combined.aseprite`。

冷却仍引用 `textures/effect/furnace_v1/cool.png`（`FireVisualLayout.COOL_SHEET`）。`furnace_v1/heat_up.png` 与 `overdrive.png` 还在磁盘上，升温/燃烧路径不再引用。

代码引用在 `FurnaceBodyVisual` 第 48–69 行。骨骼窗口在第 72–88 行：图像左侧是角色右手；肩锚点贴图像素 y=40 对模型原点，髋锚点 y=70。比例是人偶躯干 24 像素对玩家躯干 8 模型单位。

## 行为

- 成功提示仍走现有 `FireVisualPacket.FURNACE`。`FireVisualClient` 第 127–129 行交给 `FurnaceBodyVisual.noteCue`（第 96 行）。`HEAT_LOOP` 播升温入场一次再循环；`OVERDRIVE_LOOP`（含从普通升级）只播燃烧入场，不重播升温。入场 8×60ms，维持 20×80ms。见 `playback` 第 115 行。
- 没有成功提示、但身上已有炉心时，只播对应维持，不补入场。
- 效果消失且不是冷却提示时，`paintCool`（第 141 行）清掉提示，身体层不再画。
- 冷却提示仍是原来的 16 帧 × 75ms 蒸汽片，`drawFurnaces` 第 520–523 行只转给 `paintCool`。登出时 `forget` 第 687 行一并清掉。
- 第三人称和其他玩家：`FurnaceBodyLayer.render` 第 29 行。手火跟随左右臂（含行走、挥臂和持物造成的手臂姿态）。肩、胸和髋的火挂在身体，腿部火挂在左右腿。披风是身体背后的竖面（`CAPE_Z`），随身体朝向，不是镜头公告板，也不是地面环。
- 苗条手臂用 `AbstractClientPlayer.getModelName()` 为 `slim` 时改拳头中心和外侧面，不读 `PlayerModel.slim`（该字段是 private）。
- 第一人称：身体层不画。`FurnaceArmFlame.onArm` 第 31 行只在 `RenderArmEvent` 的手臂上画对应侧的手火或前臂热流，不铺全屏遮罩，不调用原版着火。
- 层注册在 `FurnaceBodyLayer.Registration.onAddLayers` 第 159 行，没有改 `XueXiMod`。
- 没有改 `FurnaceService`、`FurnaceMath`、费用、冷却、火种、链或命中。

## 已做检查

- 批准源 6+17 个文件 SHA256 与 manifest 一致。
- 燃烧维持五张运行图与正式条带 SHA256 相同。
- 入场与升温层按源叠合后，可见 alpha 与批准条带一致；全透明像素上残留的 RGB 不同，不影响显示。
- `check-part-coverage.py` 退出码 0：每个不透明像素都落在对应窗口内。维持首尾帧都有像素。入场第 1 帧 alpha 为 0，最后一帧 alpha 等于维持第 1 帧。这与源里第一帧强度为 0、最后一帧展开到维持首帧一致。
- 命名门禁退出码 1，仅 `OriginalHumanoid.java` 第 165、202 行两处旧名字。本轮没有改那个文件，也没有改那些名字。本次新文件和 `FireVisualClient` 没有报出违规。

## 未做

- 未运行 `gradlew` / `javac`。待统一编译。
- 未进游戏，没有看过画面。第一人称范围、披风侧面和余烬位置都还要实测。

## 缺口

- 披风是单片背后竖面。正背和斜向能看到宽度；纯侧面会看成薄片。
- 余烬挂在身体前后，不跟手。亮核和火舌跟左右臂。
- 升温躯干侧的连续热流挂在身体上，下段不跟腿骨。前臂热流跟手臂。
- 第一人称只有手臂局部火，没有披风和躯干。
- 冷却蒸汽仍是原来的镜头朝向片，本轮没有改它的形状。

## 供写回的资源映射

升温 O 建议写运行路径：`textures/effect/furnace_v2/heat_rear.png`、`heat_rear_tongues.png`、`heat_front_tongues.png`、`heat_embers.png`，以及 `heat_entry_rear.png`、`heat_entry_rear_tongues.png`、`heat_entry_front_tongues.png`、`heat_entry_embers.png`。

燃烧 O 建议写：`textures/effect/furnace_v2/burn_cape.png`、`burn_rear_fire.png`、`burn_body_fire.png`、`burn_hand_fire.png`、`burn_embers.png`，以及 `burn_entry_cape.png`、`burn_entry_rear_fire.png`、`burn_entry_body_fire.png`、`burn_entry_hand_fire.png`、`burn_entry_embers.png`。

索引待合并说明在 `index-update.md`。本报告不改制作表，也不把这两项标成完成。

## 文件哈希

| 路径 | SHA256 | mtime | 字节 |
|---|---|---|---|
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` | `d1065af29d147ccd09b385c5f8a03d8fb58d8106df0778f4df523a6a34197497` | 2026-10-31 03:46:02 | 41605 |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyVisual.java` | `adfe6ea2c74a8c07a136d67a819bf2c4e59997d7c62dde2a78e46cfaf4e546db` | 2026-10-31 03:48:02 | 20677 |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyLayer.java` | `05a8167f06a08ff0e27b00248f783b31439a113bd582cf48f8cb191d78b09339` | 2026-10-31 03:49:50 | 10470 |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceArmFlame.java` | `79b845a283f84edfae7cd9fa8f4de9efa1e543d609a8d8ffcb61fbc167130e70` | 2026-10-31 03:48:05 | 4916 |
| `.../furnace_v2/burn_cape.png` | `86d239c18d17e9bc28e3281994e87df927148d87af5caf34859126e8dc7fd446` | 2026-10-31 03:16:00 | 26704 |
| `.../furnace_v2/burn_rear_fire.png` | `28ad2a6958f75739e579cd7621dace0a5d9e350bfe986fc4a3829af849f3eaba` | 2026-10-31 03:16:00 | 5552 |
| `.../furnace_v2/burn_body_fire.png` | `869ce595693fec5469e1f756bd6eb507bafb86b0a8686a1223e601cd00758b5d` | 2026-10-31 03:16:00 | 12438 |
| `.../furnace_v2/burn_hand_fire.png` | `e25810ecf701daf097e1fedd6ba5b6811b17bcc7daa41dd108f085b34697ed2b` | 2026-10-31 03:16:00 | 16442 |
| `.../furnace_v2/burn_embers.png` | `3c09e9c9969327f0c2cf5ddbadc1810d871318053355be9571e41e54362f4295` | 2026-10-31 03:16:00 | 6217 |
| `.../furnace_v2/burn_entry_cape.png` | `fa247f117e3c0e9b343e0024098c45ec89c9d2bf28ff837a74be76e013a5927a` | 2026-10-31 03:38:19 | 7042 |
| `.../furnace_v2/burn_entry_rear_fire.png` | `180a37498b16c55c2ff82a45f270898b1a7ad5b1284bc63e1c7cd605347a4c08` | 2026-10-31 03:38:19 | 2346 |
| `.../furnace_v2/burn_entry_body_fire.png` | `972de9adb6d94c5717cc33120901b9f417d37a3767d94ae548ca47029a604136` | 2026-10-31 03:38:19 | 5450 |
| `.../furnace_v2/burn_entry_hand_fire.png` | `be6256d53c845037c96b930bbdd2c15762fef71bcdcfc76e4d8a5a39ef24a284` | 2026-10-31 03:38:19 | 6379 |
| `.../furnace_v2/burn_entry_embers.png` | `8e16a153ff4544b712fb628e9680258b2328724452ba4ac22ceefa192b507fcd` | 2026-10-31 03:38:19 | 3104 |
| `.../furnace_v2/heat_rear.png` | `e444322cbe696c9d2b2c012fb85ce98627122aa04099cc4bd7a2231bb814d2dd` | 2026-10-31 03:38:19 | 10015 |
| `.../furnace_v2/heat_rear_tongues.png` | `78fbf70a1463a27d914897ce128b719f27efc19199ca2cf9eeda6a58f526d54b` | 2026-10-31 03:38:19 | 9401 |
| `.../furnace_v2/heat_front_tongues.png` | `679030ad080d6751c896e3d20368604d57927d362f046f70322f1f68c1aaddcd` | 2026-10-31 03:38:19 | 8931 |
| `.../furnace_v2/heat_embers.png` | `ad9f8d2b2d2c69dbad3a31629655bf4accf700fd45d76ec06575f07585265370` | 2026-10-31 03:38:19 | 2187 |
| `.../furnace_v2/heat_entry_rear.png` | `b72511ab498b6be9af941f41cb1eff774fd395faa8d572282c087ff53ed7ccd7` | 2026-10-31 03:38:19 | 3099 |
| `.../furnace_v2/heat_entry_rear_tongues.png` | `e710fc9915ea217ee2f5a0449ec499e5d2b7aa6c798a7f0a00177bf535003125` | 2026-10-31 03:38:19 | 3420 |
| `.../furnace_v2/heat_entry_front_tongues.png` | `dc5014ce4c9905bb74b2262bf3490f9fddd0e92ca7894828f34cb36a4c9d3d4a` | 2026-10-31 03:38:19 | 2712 |
| `.../furnace_v2/heat_entry_embers.png` | `ddee2b63d888cfb8395000846d65c29b2839f4ee93bff53675345983f83649e3` | 2026-10-31 03:38:19 | 1250 |

贴图绝对目录见上。其余说明文件的哈希在 `implementation-result.json`。

停止写入。
