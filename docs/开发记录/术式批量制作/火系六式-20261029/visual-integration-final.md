先读视觉接入任务的完整说明，再按里面写明的范围做。确认版是带透明通道的竖条贴图，尺寸和说明一致。接下来只把这六张贴到游戏资源里，并用现有的平面网格按玩家朝向摆剑、火流和火环。燎里 v6 已按确认版接入现有命中体的客户端渲染。八柄扇形剑、身后起火、朝后双流和三道后移火环都是空间透明片，没有新的三维模型，也没有改服务端公共链。待统一编译。其余五式视觉待确认。未追加正式实现复审。

## 完成范围

已接入的只有用户确认的燎里 v6。命中、伤害、火种消费、40 tick 寿命、松开、换剑和死亡收束仍走原来的命中体。八柄剑和三道环不另造实体，不增加命中、伤害或种子消耗，也不生成飞剑或地火。

其余五式仍用各自的占位粒子，没有把草稿贴图复制进游戏。制作表、本批 `记录.md`、正式归档源和资源库索引都没有改。

## 资源映射

游戏目录只有这 6 个透明贴图，与 `docs/资源库/特效/火焰/燎里_v6/` 的 SHA256 一致。灰色人物、箭头、参考卡和带背景预览没有复制。

| 归档文件 | 游戏文件 | 资源标识 |
| --- | --- | --- |
| `sword_unlit.png` 64×20 | `textures/effect/liaoli_v6/sword_unlit.png` | `xueximod:textures/effect/liaoli_v6/sword_unlit.png` |
| `sword_hot.png` 64×20 | 同名 | `xueximod:textures/effect/liaoli_v6/sword_hot.png` |
| `back_ignite.png` 128×576，12×48 | 同名 | `xueximod:textures/effect/liaoli_v6/back_ignite.png` |
| `back_flow.png` 128×1152，24×48 | 同名 | `xueximod:textures/effect/liaoli_v6/back_flow.png` |
| `thrust_backflow.png` 128×1536，24×64 | 同名 | `xueximod:textures/effect/liaoli_v6/thrust_backflow.png` |
| `rear_ring.png` 96×2304，24×96 | 同名 | `xueximod:textures/effect/liaoli_v6/rear_ring.png` |

常量在 `LiaoliV6Layout`。渲染类型是 `entityTranslucentEmissive`，全亮，顶点为白色，金芯和赤焰不被染色。视觉编号是 `9`。`LiaoliEffect` 把命中体的 `spec.visual` 从旧占位 `4` 改成这个编号；`TechniqueHitRenderer` 按编号绘制后返回，因此不再走视觉 `4` 的火花，`clientDecor` 也只在视觉 `4` 时喷原版火焰粒子。

## 空间方向与生命周期

这是作者实现说明。像素按确认预览换算：64 像素对应玩家身高 1.8 格，眼睛约在预览 y=58。正深度朝前，负深度朝后，横向正方向与正面预览的画面右侧一致。只用水平朝向，抬头低头不会把扇面放平到地面上。

1. 八柄剑角度为 -180、-154、-128、-102、-78、-52、-26、0，步长 26°，成对之和为 -180。柄在深度 -18，尖在 -6，柄比尖更靠后。右侧剑翻转贴图纵向，与确认版成对。
2. 外焰从身后中线（深度 -18）铺到扇面外端，前 12 tick 用点燃条，之后用 24 帧流动条；左右成对的剑共用相位，错开 3 帧。金芯按 `hotReach` 从柄向尖覆盖，第 12 tick 起整柄点亮。
3. 手持剑从深度 10 指向深度 73。`thrust_backflow.png` 本身是左右分流，贴成一条朝后的宽带：贴图局部 +x 从靠近剑尖（深度 63）指向身后（深度 -60），与刺击方向相反。
4. 三道环同时存在，相位差 1/3，进度回绕。半径从 14 像素增到 57 像素，圆心深度从 -17 再向后移动 65 像素。环面垂直于前后轴。贴图环线约在 32/96 处，方片半边按 48/32 放大，使环线落在空间半径上。
5. 画面只在这一个命中体还活着时绘制。该体原本就是跟随准星、固定寿命，并在松开、换剑或死亡时丢弃。丢弃同步到客户端后渲染停止。视锥在视觉 `9` 时向外扩 8 格，避免身后扇面被裁掉。

确认素材是平面透明精灵，用现有方片即可放进空间，因此没有新开 Blockbench 模型，也没有把整幅人物预览挂到玩家身上。侧面看单片会变薄，这是平面片的朝向结果。

## 改动文件

| SHA256 | LastWriteTime | 文件 |
| --- | --- | --- |
| `98558537AB9FE6F284F576EC5CB9E6FE5C1925036E9606EC7BFAE2AD275F3D6C` | 2026-10-29 03:12:30 | `src/main/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6Layout.java` |
| `5A90BCE8870F15F2446587C2E9F9689819473D13EE54B6B34F4AA49F1BC0F915` | 2026-10-29 03:14:25 | `src/main/java/com/bslx/XueXiMod/client/LiaoliV6Renderer.java` |
| `B8C44C1CFE6C9BC36E144AB9A60785EEBAF7AAB30A2372A4362232FEE4B3F9EE` | 2026-10-29 03:13:18 | `src/main/java/com/bslx/XueXiMod/client/TechniqueHitRenderer.java` |
| `D60C8021134459605975438F264CAF31AB0D299F6B02E6F7CB6630A33A05CD4F` | 2026-10-29 03:13:18 | `src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java` |
| `A61F24393B236421C89BBDC24B8A75B5DCCB78D2A3F75E3BB4A55BD1F5C2329F` | 2026-10-29 03:13:52 | `src/test/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6LayoutCheck.java` |
| `2FB695A061875B6C88C102716F36E5B3791288F1EF9734E5EC7D847EFF7119B8` | 2026-10-29 02:46:19 | `src/main/resources/assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png` |
| `155FA83DFA7FD54F4F7040C3A2684F0DF622E17BE6DB76BB74FD328BAEAA42D6` | 2026-10-29 02:55:34 | `.../sword_hot.png` |
| `A83D034983C36F4EEB1B492CB6B269937BBCBFFD0DEB4FB0729E1E031E250CB2` | 2026-10-29 02:55:35 | `.../back_ignite.png` |
| `254E8F1F63BE6F13017AC192FD4AF496B3ADB960133E55F65535A802B95FA6E0` | 2026-10-29 02:55:35 | `.../back_flow.png` |
| `2D57C54F8D99AC3E451B35A70009D99B2885B924242349A6C1B6BFE120BD329F` | 2026-10-29 02:55:35 | `.../thrust_backflow.png` |
| `857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE` | 2026-10-29 03:00:23 | `.../rear_ring.png` |
| `6CAF79B884A783B52C2E2F21E6EAC7F1A2DB90A18F3D28C36E69E669AA1FA7DB` | 2026-10-29 03:14:57 | `docs/开发记录/术式批量制作/火系六式-20261029/visual-name-gate.txt` |
| `4CE414330D90636347FDD6B23034D935562585BFE8260D75378A22C9736D7FB3` | 2026-10-29 03:14:59 | `docs/开发记录/术式批量制作/火系六式-20261029/visual-self-check.txt` |

离线检查还在同目录留下 `visual-check-classes/`，只供下面的 `java` 命令使用，游戏不加载。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次改过的文件没有新违规。剩下两处是旧文件，没有改名：

- `OriginalHumanoid.java:165` 变量 `i`
- `OriginalHumanoid.java:202` 变量 `to`

定向检查命令：

```text
javac --release 17 -encoding UTF-8 -d docs/开发记录/术式批量制作/火系六式-20261029/visual-check-classes src/main/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6Layout.java src/test/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6LayoutCheck.java
javac-exit=0
java -cp docs/开发记录/术式批量制作/火系六式-20261029/visual-check-classes com.bslx.XueXiMod.visual.liaoli.LiaoliV6LayoutCheck
liaoli v6 layout check passed
java-exit=0
```

检查覆盖镜像角度、柄比尖更靠后、火焰不从尖端起、贴图 +x 朝后、三环错相并持续变大后移、金芯在第 0 tick 为 0、第 12 tick 为 1，以及游戏目录只有这 6 张贴图且哈希与归档相同。它不编译渲染器，也不代替 Gradle 或游戏内画面。

## 不足

没有运行 `compileJava`，没有进游戏看第一人称和侧面厚度。比例来自确认预览的身高换算，还没有按游戏镜头量过。没有阻塞项，也没有需要停下来另做的 Blockbench 模型。

所有写入已经停止，文件占用已经释放。
