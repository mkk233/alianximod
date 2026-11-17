# 握柄快拔 v6 运行映射

游戏只加载姿态关键帧。bbmodel、图集、GIF、Aseprite、预览图和原版铁剑参照留在本目录，不进入 `assets`。旧 v5 运行 JSON 仍保留。

| 运行资源 | 源数据 | 适配 |
| --- | --- | --- |
| `assets/xueximod/actions/space_cut_grip_fast_v6.json` | `animation-keyframes.json` | 时长 0.5 秒，欧拉 ZYX。骨骼为 Body、Head、ArmRight、ForearmRight、SwordHand、ArmLeft、ForearmLeft、LegRight、LegLeft。Body、LegRight、LegLeft 保留位移，其余只保留旋转。时间是原 1.6 秒采样乘 0.3125。 |
| `xueximod:space_cut` | 同一条动作的播放时钟 | `OriginalHumanoid.registerSpaceCut` 登记 0.5 秒一次、0.06 秒混合，以及 0.15 秒的视觉通知 `draw-release`。共享五骨剪辑保持单位旋转，不提供拔刀姿态。 |
| 客户端 `SpaceCutSampler` | 上面的运行 JSON | 资源路径改为 v6。父级合成、`(qx, -qy, qz, qw)` 和 `(x, 24-y, z)` 未改。起始右手掌心仍按 Blockbench `[5.3, 15.5, -3.6]` 检查。 |

刀不进运行资源。动作期间若主手或副手有 `SwordItem`，那一份真实物品挂到右手 `SwordHand`。没有剑时不生成示意刀。

挂点先把右手 handheld 展示旋转后的刃宽、斜向刀身 `normalize(1,1,0)` 和 `+Z` 法线对准剑骨，再让 `renderItem` 乘回同一次右手展示旋转、缩放和 `-0.5` 居中。平移同时扣掉展示平移和缩放后的缠把 `(3.5, 3.5, 8)/16`，所以柄点落到掌心，柄到尖与剑骨刀轴同向。第三人称缩放 0.85、平移 `(0, 4, 0.5)/16`；第一人称缩放 0.68、平移 `(1.13, 3.2, 1.13)/16`。自定义模型若覆盖了 handheld 展示，或网格不是这把生成剑的斜向缠把，这次不读那份覆盖。
