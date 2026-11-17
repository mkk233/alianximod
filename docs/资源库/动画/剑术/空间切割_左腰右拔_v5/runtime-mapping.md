# 左腰右拔 v5 运行映射

游戏只加载姿态关键帧。bbmodel、图集、GIF、Aseprite 和预览图留在本目录，不进入 `assets`。

| 运行资源 | 源数据 | 适配 |
| --- | --- | --- |
| `assets/xueximod/actions/space_cut_left_waist_v5.json` | `animation-keyframes.json` | 时长 1.6 秒，欧拉 ZYX，五位小数。骨骼为 Body、Head、ArmRight、ForearmRight、SwordHand、ArmLeft、ForearmLeft、LegRight、LegLeft。Body、LegRight、LegLeft 保留位移，其余只保留旋转。 |
| `xueximod:space_cut` | 同一条动作的播放时钟 | `OriginalHumanoid.registerSpaceCut` 只登记 1.6 秒一次、0.06 秒混合，以及 0.48 秒的视觉通知 `draw-release`。共享五骨剪辑保持单位旋转，不提供拔刀姿态。 |
| 客户端 `SpaceCutSampler` | 上面的运行 JSON | 父级先在 Blockbench 空间合成。合成后的旋转写成 `(qx, -qy, qz, qw)`，平移写成 `(x, 24-y, z)`。起始右手掌心偏离 `[5.3, 15.5, -3.6]` 超过 0.05 时拒绝这套姿态。 |

刀不进运行资源。动作期间若主手或副手有 `SwordItem`，把那一份真实物品挂到右手 `SwordHand`。没有剑时不生成示意刀。

挂点线性部分是 `T·R展示⁻¹`。`renderItem` 仍使用右手 `ItemDisplayContext`，再乘一次原版 handheld 旋转、缩放和 `-0.5` 居中后，刀轴与扁平面法线落到 `SwordHand`。掌心平移扣掉右手展示平移：第三人称 `(0, 4, 0.5)/16`，第一人称 `(1.13, 3.2, 1.13)/16`。剑长仍跟物品缩放。自定义模型若覆盖了 handheld 展示，这次不读那份覆盖。
