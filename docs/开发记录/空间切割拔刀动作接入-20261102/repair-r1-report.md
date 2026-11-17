# 空间切割左腰右拔返修 r1

只改了 `SpaceCutItemMount.java`。待统一编译。游戏画面未验证。

## 改动

挂点线性部分从 `T·R展示` 改成 `T·R展示⁻¹`。`renderItem` 仍用右手展示上下文，再乘一次旋转后得到 `T`。缩放和物品模型不动。

平移按完整手持变换扣掉。`ItemTransform.apply` 先平移再 `rotationXYZ` 再缩放，`ItemRenderer.render` 再 `translate(-0.5,-0.5,-0.5)`，所以模型中心落到展示平移上。第三人称扣 `(0, 4, 0.5)/16`，第一人称扣 `(1.13, 3.2, 1.13)/16`。扣掉之后中心和刀轴线都在掌心。

第一人称在原有 `XP(lookPitch)`、`YP(bodyYaw-lookYaw)` 之前，按进入绘制时的顺序取逆：先 `YP(-(viewYaw-yBob)·0.1)`，再 `XP(-(viewPitch-xBob)·0.1)`。身体进相机的变换保留。

主手优先、副手其次、主手在左仍用右手展示、非挂载物品留在该侧腕部，这些分支没有改。

## 自查

脚本 `repair-r1-check/check_mount.py`，输出 `check_mount.txt`。姿态取运行 JSON 的 0、0.48、0.60、1.60 秒，展示旋转用 `rotationXYZ(0,-90,55/25)`，并乘物品缩放。

| 结果 | 第三人称 | 第一人称 |
| --- | --- | --- |
| 修正后刀轴 / 法线 / 中心 / 轴线 | 四个时刻都是 0 | 四个时刻都是 0 |
| 旧公式刀轴 / 法线 | 70.792520° / 125° | 34.775437° / 155° |
| 旋转已对、平移未扣时的轴线距离 | 3.563396 像素 | 2.631479 像素 |

外层 bob：俯仰 60° 且偏航残差为 0 时，正反顺序都会消掉。俯仰 -30°、偏航 120°、bob `(10,-20)`、身体 40°，以及俯仰 85°、偏航 -170°、bob `(40,15)`、身体 90°，按实际顺序偏差 0°，顺序反了分别剩 0.955987° 和 0.876191°。

`powershell -NoProfile -File tools/check-name-gate.ps1` 通过。未跑 Gradle，未打包，未进游戏。

## 文件

| 文件 | sha256 | mtime |
| --- | --- | --- |
| `SpaceCutItemMount.java` | `cdc6a81aaa2a9554fb8a63560fa268aec3c6d2bb49352f31bb52b82a6d0b51fe` | 2026-11-02T05:32:23 |
| `repair-r1-check/check_mount.py` | `6eeb262cd37396f11e0154772559cd95ad8b56f2b5c0672be096289fe3bd7954` | 2026-11-02T05:33:42 |
| `repair-r1-check/check_mount.txt` | `3e1bd9dd8a2043008fc4ba9c7f59d2cfca795d51cf7ff9d5313c6f8e3e5eb263` | 2026-11-02T05:33:48 |

## 限制

标准右手 handheld 的旋转和平移被抵消。物品自己的模型 JSON 如果覆盖了这份展示，运行时不会再读那份覆盖。剑长仍跟展示缩放。数值检查不是游戏画面。

停止全部源码写入。
