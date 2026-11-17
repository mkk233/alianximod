# 握柄与快拔实现

授权范围内改了挂载、空间切割动作时钟和 v6 运行姿态。没有跑 Gradle，没有进游戏。实现正式复核仍是 2/3，这里只做作者自查。源码写入已停止。

## 改动

- `SpaceCutItemMount.java`：右手 handheld 的刃宽、斜向刀身 `normalize(1,1,0)` 和 `+Z` 法线对准 `SwordHand`。`renderItem` 仍乘一次右手展示旋转、缩放和 `-0.5`。平移解的是缠把 `(3.5, 3.5, 8)/16` 落到掌心，同时柄到尖与剑骨刀轴同向。
- `OriginalHumanoid.registerSpaceCut`：会话、剪辑和状态时长 0.5 秒，视觉通知 `draw-release` 在 0.15 秒。混合仍是 0.06 秒。
- `SpaceCutSampler`：只把资源改到 `actions/space_cut_grip_fast_v6.json`。采样和四元数未改。
- 新增 `assets/xueximod/actions/space_cut_grip_fast_v6.json`。九骨各 161 帧，`0`、`0.15`、`0.1875`、`0.5` 都在时间轴上。旋转和位移沿用 v6 关键帧。

旧 `space_cut_left_waist_v5.json` 哈希仍是 `64b0fe3eb2f629c3fb12b90ca1d3a8c64e1664c7661dc7794c97473a8b313c46`。备份在本目录 `before-feedback/`。

主手剑、副手剑都走 `swordHand`：先主手，否则副手。`drawSword` 固定右手展示，`leftHand` 为 false。没有剑则不画。宽臂和细臂不进这套物品矩阵。

## 自查

`grip-check/check_grip.py` 用运行 JSON 的真实剑骨，在 0、0.15、0.1875、0.5 秒走完第三人称与第一人称的展示旋转、平移、缩放和居中。柄点误差最大 `1.192e-11` 格，柄到尖与剑骨刀轴夹角 `0`。第三人称剑长 `0.901561` 格（缩放 0.85），第一人称 `0.721249` 格（缩放 0.68）。方块中心离掌心仍是第三人称 `0.338085` 格、第一人称 `0.270468` 格。模型 `+Y` 与刀轴夹角 45 度。t=0 的 Blockbench 掌心偏离 `[5.3, 15.5, -3.6]` 为 `3.811e-7`。命名门禁通过。

## 边界

挂载用的是原版右手 handheld 常量。自定义模型若改了 `thirdperson_righthand` 或 `firstperson_righthand`，或缠把不在这条生成剑对角线上，这次没有读取。伤害、20 tick 冷却和 20 帧贴图未改。动作与贴图仍从同一次释放起播，贴图仍是 1 秒。
