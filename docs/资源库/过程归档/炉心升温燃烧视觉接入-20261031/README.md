# 炉心升温燃烧视觉接入过程

批准原件没有改。脚本只读 Aseprite，把分层写成游戏竖条。

- `export-furnace-layers.lua`：导出升温维持、升温入场、燃烧入场的各层。燃烧维持五张条带是正式 `*-strip.png` 的逐字节复制，不经此脚本重编码。
- `export-log.txt`：层名和帧时长。升温维持与燃烧维持为 20×80ms，两份入场为 8×60ms。
- `check-part-coverage.py`：核对运行贴图里每个不透明像素都落在对应骨骼窗口内，并打印首尾帧 alpha。

运行贴图目录：`src/main/resources/assets/xueximod/textures/effect/furnace_v2/`。预览、合成整图和过程目录里的空 `overdrive-v3-combined.aseprite` 不复制进游戏。
