# 周身水环 v8 正式归档结果

2026-10-27。只复制整理资源，没有改游戏代码，没有编译，没有提交 git，没有删除旧版本。

## 位置

- 正式目录：`docs/资源库/特效/水流/周身水环_细碎毛边_v8/`
- 正式说明：[README.md](../../特效/水流/周身水环_细碎毛边_v8/README.md)
- 清单：[asset_manifest.json](../../特效/水流/周身水环_细碎毛边_v8/asset_manifest.json)
- 资源库索引：[docs/资源库/README.md](../../README.md)
- 过程稿：[周身水环3D验证_v8_细碎水膜毛边-20261027](../周身水环3D验证_v8_细碎水膜毛边-20261027/README.md)

状态写为「美术正式采用，游戏接入进行中」。没有写成已完成、已编译或已游戏内实测。

## 复制

从过程稿复制 12 个文件到正式目录。另写正式 README 与 `asset_manifest.json`，正式目录共 14 个文件。

11 个贴图、源文件和预览与来源 SHA256 一致：

- `ragged_splash_outer.aseprite` / `.png`
- `ragged_splash_inner.aseprite` / `.png`
- `broken_saw_flow.aseprite` / `.png`
- `water_ragged_fixed.gif`
- `water_ragged_top.gif`
- `water_ragged_preview.png`
- `ragged_outer_preview.gif`
- `ragged_inner_preview.gif`

`周身水环_细碎水膜毛边_v8.bbmodel` 不是整文件相同。字节对照只有 3 处插入，分别是三张水纹的外部 `path`，指向正式目录中的同名 PNG。去掉这三处 `path` 后，与来源 JSON 一致，几何、UV、动画和内嵌纹理未改。占位人物仍在展示模型里。来源模型原本没有外部路径，因此这次是补上正式目录路径，过程稿里的模型文件未改。

正式模型 SHA256：`efe2b6a13259584dc3d57d966606159146cc4e6ffc42ffa2be475d9cbc4a5c4b`。来源模型 SHA256：`af8292a734a37034d1b6e4ac03f8be6b9eea3d29671429f5f4c8f90c3a5e5784`。

清单内 13 条记录的大小和 SHA256 已与磁盘重新核对，全部一致。清单自身 SHA256：`cb2d0bbfb2c70f85de5ea3c3f6fe81d795de125be9b74bff6ca38a82af3ace7a`，7419 字节。

## 未复制

过程脚本、参考图和截图仍只在过程稿：`draw-ragged-water.lua`、`用户参考_水环毛边.png`、`preview-water-unlit.js`、`export-previews.ps1`、`validation.json`、`frames-fixed/`、`frames-top/`。v7 及更早版本未动。
