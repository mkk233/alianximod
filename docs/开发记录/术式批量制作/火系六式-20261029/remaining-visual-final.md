先读这份授权范围，再按里面写明的边界做剩余视觉接入。确认版是透明竖条，按现有燃烧计划和炉心状态接到客户端，不改伤害、费用和范围。燎原 v2、焚天 v2 和炉心三式 v1 已按确认竖条接入。伤害、魔力、冷却和范围仍是实现报告里的原值。待统一编译。未追加正式实现复审。

## 完成范围

燎原从链上的实际原点向外放一圈火浪，火海和炭纹贴在真实可见地表上，半径、200 tick、上下 3 格和 1 格火层仍用原来的几何。不放原版火，不改方块。被消费火种的附着火焰跟那条燃烧计划走，离开火场后继续显示到计划结束。

焚天只画在真正挂上焚天燃烧计划的目标身上。金白火柱和赤红外焰按该目标自己的段数、4 tick 首段延迟和余量尾段取样。自然打完最后一段后才播 12 帧余烬，余烬不造成伤害。死亡或计划被清掉时画面一起停。

炉心普通档循环低密度暖焰，过载档循环强焰。成功进入或升级时从第一帧再播一次过载，升级不重播升温。只有成功的主动冷却播一次蒸汽。失败、死亡和被动移除不播蒸汽。

确认素材是平面透明精灵，用现有方片和朝向相机的牌就能放进空间，因此没有新开三维模型。

## 资源映射

游戏目录只有这 7 张透明竖条，SHA256 与归档一致。预览、灰色人物、参考卡和 Aseprite 源留在资源库。

| 归档 | 游戏文件 | 资源标识 |
| --- | --- | --- |
| `liaoyuan-ground-flame-v2-strip.png` 32×960，24×40 | `textures/effect/liaoyuan_v2/ground_flame.png` | `xueximod:textures/effect/liaoyuan_v2/ground_flame.png` |
| `liaoyuan-ground-embers-v2-strip.png` 64×1536，24×64 | `textures/effect/liaoyuan_v2/ground_embers.png` | `xueximod:textures/effect/liaoyuan_v2/ground_embers.png` |
| `liaoyuan-spread-wave-v2-strip.png` 128×1536，12×128 | `textures/effect/liaoyuan_v2/spread_wave.png` | `xueximod:textures/effect/liaoyuan_v2/spread_wave.png` |
| `fentian-conflagration-v2-strip.png` 128×7488，52×144 | `textures/effect/fentian_v2/conflagration.png` | `xueximod:textures/effect/fentian_v2/conflagration.png` |
| `heat_up-v1-strip.png` 64×1024，16×64 | `textures/effect/furnace_v1/heat_up.png` | `xueximod:textures/effect/furnace_v1/heat_up.png` |
| `overdrive-v1-strip.png` 64×1024 | `textures/effect/furnace_v1/overdrive.png` | `xueximod:textures/effect/furnace_v1/overdrive.png` |
| `cool-v1-strip.png` 64×1024 | `textures/effect/furnace_v1/cool.png` | `xueximod:textures/effect/furnace_v1/cool.png` |

同步走新的客户端包，通道序号 11。它只报告已经发生的火场、已挂燃烧和炉心成功结果。`SkillUsePacket` 里没有按术式 id 新开的结算分支。

## 空间与生命周期

这是作者实现说明。

燎原火浪在前 12 tick 播完一次，方片边长对到真实半径，环在贴图里由小变大。火海半径在这 12 tick 内从原点扩到全半径，之后停在该半径上直到 200 tick。每一格先找上下 3 格内、上方可站立的固体顶面，再铺炭纹，并立一块高度约 0.78 格的低矮火焰。附着火焰用同一张地火竖条，大小按消费快照的阶和余量，24 帧循环，节奏独立于焚天的十拍。

焚天帧号与确认标签一致：进度 1–3 是聚火，之后每 4 tick 一拍。1 段火种只到第 4 帧，再接余烬 41–52，不读后面的九拍。10 段才到第 40 帧。余量多出来的那一段用下一拍，尺寸按余量缩小。施法者被现有伤害许可排除，没有火种的目标不会挂计划，也就不会画。

炉心维持跟着已同步的炉心效果：放大器 0 是升温循环，放大器 1 是过载循环。帧长 75 毫秒，16 帧后循环，状态本身仍然无限持续。成功冷却单独播 16 帧后停止。

`GroundFireFields` 里原来有一处未声明的 `feetPos`。该方法里已有 `feetBlock`，固体判断现已指向它。半径、寿命、层高和伤害数字没有改。

实现报告里的其余默认仍在：炉心复杂度 0，登录会学会；三式终极复杂度 20；六式弹速 0；焚天半径 8，段数和 1.2 的指数不吃能量倍率。

## 改动文件

| SHA256 | LastWriteTime | 文件 |
| --- | --- | --- |
| `C149CD44ECB37B90944679855C1672E7D1D812F0BA95158FAC253759622A94F9` | 2026-10-29 03:31:04 | `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java` |
| `5C5A4E30C459860445F6AE127B22A5A973B8D15BA97BE34D4B008A7744D365E3` | 2026-10-29 03:31:05 | `src/main/java/com/bslx/XueXiMod/network/FireVisualBridge.java` |
| `6E97ED2C65C0F7AD5EA7085F1EDB6978B2CCCED29F034AD908D25B96F2201C54` | 2026-10-29 03:31:14 | `src/main/java/com/bslx/XueXiMod/network/FireVisualPacket.java` |
| `850F4B52BEC59A344E20536ABD9B113094B51BC1F224FB6EFF4BB7614883AD7E` | 2026-10-29 03:34:20 | `src/main/java/com/bslx/XueXiMod/network/ModNetwork.java` |
| `478F82CC06967047B4223D7FA090D9377B0FD942F13D9E14EF31651085474B00` | 2026-10-29 03:33:48 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireVisualRelay.java` |
| `7585456592D5608FC6A5F8073C05AEB99F172EF6128A254DE647035EE826033B` | 2026-10-29 03:35:43 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireRuntimeEvents.java` |
| `2A26757824AB103C61B2FC1D70B490E0098D96AEA8CDD936C9BE296DAE16826E` | 2026-10-29 03:34:19 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireConsumption.java` |
| `8FFE32BA2344C5E9EF27E073671DDC8CDD1132229EF039CB3F2B74ED4A260A42` | 2026-10-29 03:34:05 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java` |
| `56627DD628257B3775DCA4266A761FCAE27078E68190BA9B10784C9B6C550C5F` | 2026-10-29 03:34:17 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java` |
| `EA198B55ABC352D4A47ED22E8FDB358982BB70780617BC49E05C07C6ABEBF967` | 2026-10-29 03:34:11 | `src/main/java/com/bslx/XueXiMod/shu/technique/effects/FentianEffect.java` |
| `81D080873024A2A5370A513FABCC511A1197C7AEA2B68C7E70E0AD7135E03F18` | 2026-10-29 03:33:47 | `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` |
| `AA68AAA0516D116756B646D39E4E5FFDD93DF4252C7DE130676A359A0FF15DC0` | 2026-10-29 03:34:20 | `src/main/java/com/bslx/XueXiMod/XueXiMod.java` |
| `4DBEB0D3DDD94A0E52B4B81220D207F048825E9B87957ABF5D4E4BC4FEB3D7A7` | 2026-10-29 03:36:00 | `src/test/java/com/bslx/XueXiMod/visual/fire/FireVisualLayoutCheck.java` |
| `55E721DB3D53A7082EDA25D9797C28972CD3B3C2E946EA5A2BEC39C5F0A8789E` | 2026-10-29 03:14:59 | `.../liaoyuan_v2/ground_flame.png` |
| `4A3297FCBF2F1DC13A58C022CB0E314BFC52DF5F68588F2D9C28FD46BA55D1CF` | 2026-10-29 03:14:59 | `.../liaoyuan_v2/ground_embers.png` |
| `538194B29FB572F0CD6BA4E9DA1DEFE734D11953CA41714B4A87D1B4973B7351` | 2026-10-29 03:14:59 | `.../liaoyuan_v2/spread_wave.png` |
| `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76` | 2026-10-29 03:09:11 | `.../fentian_v2/conflagration.png` |
| `F4B646BD75B88A9D35C597538C6957D8DA1F177C745A637C7784210F89D2708A` | 2026-10-29 02:31:42 | `.../furnace_v1/heat_up.png` |
| `16CA0D000DB5A5F42AB57F65126536C3DDDF8B431DD1CACD55BAD09454A604AE` | 2026-10-29 02:31:42 | `.../furnace_v1/overdrive.png` |
| `45AB51CB6B86B9C159B1708BD82C43A61BF4C0AC798FFED39A1C3740C0CAC135` | 2026-10-29 02:31:42 | `.../furnace_v1/cool.png` |
| `6CAF79B884A783B52C2E2F21E6EAC7F1A2DB90A18F3D28C36E69E669AA1FA7DB` | 2026-10-29 03:36:19 | `docs/开发记录/术式批量制作/火系六式-20261029/remaining-visual-name-gate.txt` |
| `42E879D9FD198FE6310CF324E8AB3F27A0D58A7E9053BC7B7E0BC9264213CF98` | 2026-10-29 03:36:21 | `docs/开发记录/术式批量制作/火系六式-20261029/remaining-visual-self-check.txt` |

离线检查还留下 `remaining-visual-check-classes/`，只给下面的 `java` 命令用，游戏不加载。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次文件没有新违规。剩下两处是旧文件，没有改名：

- `OriginalHumanoid.java:165` 变量 `i`
- `OriginalHumanoid.java:202` 变量 `to`

定向检查：

```text
javac --release 17 -encoding UTF-8 -d docs/开发记录/术式批量制作/火系六式-20261029/remaining-visual-check-classes src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java src/test/java/com/bslx/XueXiMod/visual/fire/FireVisualLayoutCheck.java
javac-exit=0
java -cp docs/开发记录/术式批量制作/火系六式-20261029/remaining-visual-check-classes com.bslx.XueXiMod.visual.fire.FireVisualLayoutCheck
fire visual layout check passed
java-exit=0
```

检查覆盖火浪一次播完、火海停在真实半径、低火种不进入后续拍、余烬从第 41 帧起、升级走过载、冷却 16 帧后停止，以及 7 张贴图的哈希、尺寸和游戏目录里没有预览。它不编译渲染器，也不代替 Gradle 或游戏内画面。

## 不足

统一编译还没跑，游戏里也还没看过。火浪是原点地表上的一张平盘，起伏地面可能穿插；火海按每一列的真实顶面铺。身上的焚天和炉心是朝向相机的平面牌，第一人称可能裁进镜头。燃烧快照若连续 40 tick 没再到，客户端会先收起，目标重新被跟踪时按剩余计划再出现。

所有写入已经停止，文件占用已经释放。
