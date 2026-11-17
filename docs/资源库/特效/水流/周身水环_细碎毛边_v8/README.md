# 周身水环 · 细碎水膜毛边 v8

用户于 2026-10-27 确认采用。状态：已接入游戏，编译通过，待用户游戏内验证。未打包。游戏内效果由用户测试。

贴图为 16 帧、每帧 100ms，局部水舌循环 1.6 秒。根组动画 `animation.water_saw_counterclockwise` 为 2.4 秒逆时针旋转一圈，游戏里按 48 tick 转一圈。固定斜视和俯视预览各 96 帧、每帧 50ms，4.8 秒闭环。占位人物只留在这份展示模型中，用来演示水体遮挡。

## 正式文件

- 可编辑源：[ragged_splash_outer.aseprite](ragged_splash_outer.aseprite)、[ragged_splash_inner.aseprite](ragged_splash_inner.aseprite)、[broken_saw_flow.aseprite](broken_saw_flow.aseprite)、[周身水环_细碎水膜毛边_v8.bbmodel](周身水环_细碎水膜毛边_v8.bbmodel)
- 原尺寸导出：[ragged_splash_outer.png](ragged_splash_outer.png)、[ragged_splash_inner.png](ragged_splash_inner.png) 均为 256×640，单帧 256×40；[broken_saw_flow.png](broken_saw_flow.png) 为 256×384，单帧 256×24。三张都是 16 帧、每帧 100ms。
- 预览：[固定斜视](water_ragged_fixed.gif)、[俯视](water_ragged_top.gif) 均为 720×720、96 帧、4.8 秒；[静态预览](water_ragged_preview.png)；[外缘](ragged_outer_preview.gif)、[内缘](ragged_inner_preview.gif) 均为 1024×160、16 帧、每帧 100ms。
- 文件清单：[asset_manifest.json](asset_manifest.json)

展示模型的几何、UV、动画和内嵌纹理与过程稿一致。三张水纹的外部路径改指向本目录中的同名 PNG，不再依赖过程归档。

## 游戏 runtime

三层网格，没有人物。水体 576 面，外缘 96 面，内缘 96 面，共 768 面。图集 768×640：水体从 x=0 起，单帧高 24；外缘从 x=256 起，内缘从 x=512 起，单帧高都是 40。贴图 16 帧，每帧 2 tick，32 tick 循环；与 48 tick 自转合成 96 tick 完整闭环。三层共用根旋转：先绕本地 +Y 转，再保留原模型 3° 倾斜，然后把模型 +Y 对齐到现有护体环法线。缩放后放在原来的环心。防御时长、减伤、耗魔、偏转和接触附魔仍走原来的 `water_ring`。

归档副本在 [runtime](runtime/)：

| 归档文件 | 游戏路径 |
| --- | --- |
| [water_ring_v8.png](runtime/water_ring_v8.png) | `assets/xueximod/textures/entity/xueximod/water_ring_v8.png` |
| [water_ring_v8.png.mcmeta](runtime/water_ring_v8.png.mcmeta) | `assets/xueximod/textures/entity/xueximod/water_ring_v8.png.mcmeta` |
| [water_ring_v8_mesh.json](runtime/water_ring_v8_mesh.json) | `assets/xueximod/waterring/water_ring_v8_mesh.json` |

这三份与 `src/main/resources` 中的同名文件字节相同。`compileJava --offline --no-daemon` 已通过。没有打 jar，也没有进游戏看效果。

## 过程归档

制作脚本、参考图和预览脚本留在过程稿，不复制进本目录：

- [绘制脚本](../../../过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/draw-ragged-water.lua)
- [用户参考图](../../../过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/用户参考_水环毛边.png)
- [preview-water-unlit.js](../../../过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/preview-water-unlit.js)
- [过程说明](../../../过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md)

v7 及更早版本仍留在各自目录。
