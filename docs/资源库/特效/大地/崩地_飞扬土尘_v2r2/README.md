# 崩地·飞扬土尘 v2r2（已采用，运行接入进行中）

2026-11-16 用户原话：「这个版本可以接入」。确认对象是 `earth-ground-airborne-dust-v2r2-20261116`，绑定过程目录 [崩地_飞扬土尘_v2r2_20261116](../../../过程归档/崩地_飞扬土尘_v2r2_20261116/) 的同版 Aseprite、原尺寸导出、二维 GIF、真实 Blockbench 模型和整体/侧视动画。这是正式采用和接入授权，不再标为未确认。

现有正式分类是 `特效/大地`，没有 `特效/土系`。本目录因此放在大地分类，不新建分类，也不覆盖 [崩地_v1](../崩地_v1/README.md) 或被拒绝的坑口扬尘过程稿。

## 本目录

183 份过程文件按字节复制，没有重画，也没有改模型。过程 `manifest.json` 的逐字节副本是 `来源-manifest.json`，SHA256 `4AB7E17F7183A97BD3D26D9E38C682082B8365F796959A0BED5B2297E3F52E88`。其中“待用户同版确认”只保留确认前原文。本目录 `manifest.json` 是确认后的路径索引，不替代该哈希。`visual-handoff.md` 同样是确认前原文副本；用户后来的采用原话以本说明为准。

- 主源 `source/ground_airborne_v2r2.aseprite`，SHA256 `EE367DBB75773ED8D5FAFF7A5B1ECF1EA8C31EE69921DFB89854A6187C49C01A`。粗粒源 `source/visual_grit_v2r2.aseprite`。
- 原尺寸图集 `exports/ground_airborne_v2r2-atlas.png`，三条 48×1152 竖条，以及 24 张逐帧。
- 二维预览 `previews/ground_airborne_v2r2-2d.gif`，SHA256 `D1390C8B5E53377E1EE5FB987FAEE5380856555958B5F69E5FC1F7E68B1D78A9`。
- 模型 `source/earth_ground_airborne_v2r2_preview.bbmodel`，SHA256 `A7FF96F62EEDA989B8BDA63889D3C0D8B45250697E127C5A3A695584C060AA2C`。
- 整体 GIF `A4AF190D9DE2CD194EBDDA503506AF48A19767E4DC6074D2F9F9E02671DAF521`，侧视 GIF `D7644062795DA9BB110FB210936A01B0D34724190A08405E8AC101EDF9A7251F`。两份都是 2.3 秒真实视口示意，不是施法时长。

模型内的相对贴图路径仍指向原过程目录。过程目录没有移动，从原目录打开时回链有效。

## 运行与未完成

已批准的视觉预算是 1.4 秒、28 tick。这只批准本版播放时长，不改挖坑、伤害、燃料或地形规则。运行接入仍在进行。完成前，现行运行仍是 `assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png` 的 16 tick 单面片，不能把本目录当成已经替换。

已记录的功能缺口仍在：目前只有陨岩直接撞地和残骸真正接地、并且破坏结果为 EXECUTED 后播放土尘。普通及其他通用攻击没有已完成的播放通路。该缺口阻碍按流程第 8 节迁入待验收；不能因本次美术确认或日后编译成功就当作整项功能已齐。本轮不修。

未编译，未启动游戏，未游戏实测。旧过程候选和崩地 v1 保留为历史。本阶段额度已用完，返修结果未追加复审。
