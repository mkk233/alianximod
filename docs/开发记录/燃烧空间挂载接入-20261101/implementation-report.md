# 燃烧空间挂载接入

模型 `burn-v3-spatial-v1` 已按 2026-11-01 确认版接入客户端。用户原话是「可以就这版接入」，手臂为「整团火焰随手臂一起转动」。本阶段没有编译，没有启动游戏，没有改制作表，也没有改 `docs/资源库/README.md`。

制作中第 4 行 `fire_burn` 的 N 仍是过载炉心规则。这次只换燃烧视觉，没有改升温、冷却、魔力、伤害、持续或服务端。

## 运行几何

`BurnSpatialMount` 读取 `models/effect/burn_spatial_v1/mount.json`。19 个特效网格、239 个四边面都来自 bbmodel。7 个示意立方（头、身、四肢、持物杆）没有写入。各组休息旋转都是 0，顶点在模型绝对坐标里，所以没有把演示抬手动画烘焙进去。

换算是交接里的那条：`x = xBB`，`y = 24 - yBB`，`z = -zBB`，再减去真实骨骼枢轴。躯干 `(0,0,0)`，宽臂 `(±5,2,0)`，腿 `(±1.9,12,0)`。示意腿枢轴是 `±2`，差出来的 0.1 留在腿局部坐标里，火簇仍落在示意的模型位置。披风下摆局部 `y` 到 24.12，对应脚底 24。披风向后弯到局部 `z` 约 9.2，不是一张零厚度平面。

本机 `ModelPart.translateAndRotate` 把枢轴除以 16，`ModelPart.Cube.compile` 把顶点除以 16。网格按同样的 `MODEL_TO_BLOCK = 0.0625` 提交（`BurnSpatialMount.java` 第 38、86–97 行）。第三人称层画在 `PlayerRenderer.scale` 的 0.9375 之后，这里不再乘一次。每个面只提交一次，走 `entityTranslucentEmissive`，最近邻、不插帧、全亮、不烘焙光照。

第三人称挂在当前骨骼上（`FurnaceBodyLayer.java` 第 51–57 行）：披风、肩腹、背后火舌和身体火星在 `body`；左髋和左右小腿在对应腿；左右手焰、拳心亮核和手部火星在对应手臂。模型里没有右髋火簇，没有另造。苗条臂用同一套网格跟实时骨骼。`setupAnim` 在非下蹲时会把手臂 `y` 写成 2，和下蹲时的 5.2，宽臂烘焙枢轴就是 2。没有改火的外形。苗条臂没有游戏实测。

## 第一人称

本机 Forge 1.20.1-47.3.33 只有 `RenderHandEvent` 和 `RenderArmEvent`，没有 Pre/Post。`ForgeHooksClient.renderSpecificFirstPersonHand` 在 `ItemInHandRenderer.renderHandsWithItems` 里、`renderArmWithItem` 之前投递 `RenderHandEvent`。不取消事件时，原版接着画手和物品。`RenderArmEvent` 只从 `PlayerRenderer.renderRightHand` / `renderLeftHand` 进来，实际只有空手主手和地图会画手臂。

燃烧因此只挂 `RenderHandEvent`（`FurnaceArmFlame.java` 第 33–46 行），不取消。升温仍只留在原来的 `RenderArmEvent`（第 49 行起）；过载时这条直接返回，避免空手路径叠两层火。第一人称不画披风和身体。

姿态跟原版同一段代码，不另放一套抬手：

- 空手主手：复刻 `renderPlayerArm`，再加手臂枢轴 `(±5, 2) / 16` 和年龄 0 的摆动 `zRot = ±0.1`。这是 `renderHand` 把 `xRot` 清零后仍留在手臂上的值（`BurnFirstPersonPose.java` 第 44–45、110–112 行）。空的副手原版不画手臂，这里也不画一团悬空火。
- 单手地图：先做那一下 10 度侧倾，再走上面的空手手臂。双手地图：先做地图倾角，再按 `renderMapHand` 画左右两只手臂上的手焰。
- 剑、法杖和其他物品：复刻弩、使用、激流、挥击和 `applyForgeHandTransform`。法杖父模型是 `item/handheld`，没有自定义手部变换时走普通持物。拳心锚在右臂局部 `(-1, 8.8, 0)`、左臂 `(1, 8.8, 0)`，除以 16 后放到 `renderItem` 之前的手部原点（第 77–78 行）。物品自己的展示旋转仍只作用在物品上。

火焰网格保持手臂局部轴向，不改成世界向上。

## 贴图

循环 13 张与确认分件或旧 `furnace_v2` 整层逐像素相同。入场披风、背后火舌、手焰整层也逐像素相同。身体七簇入场按循环遮罩拷贝，遮罩外 0 像素。火星遮罩外 95 个原像素不改色，就近归入已有火星件。旧 `furnace_v2/burn_*.png` 还在，燃烧代码已不再引用。`heat_*` 和 `cool.png` 没改。

## 检查

- bbmodel 面纹理、UV 和父骨骼已对上运行 JSON；运行文件不含「示意辅助」。
- 循环与入场整层做了逐像素对比，结果在 `docs/资源库/过程归档/燃烧空间挂载接入-20261101/archive-report.json`。
- 命名门禁扫过整个 `src`。本次炉心视觉文件没有新的命名失败。未改的 `OriginalHumanoid.java` 仍有 `i` 和 `to` 两处旧失败，这次没有顺手改。
- 没有跑 Gradle，没有进游戏。

## 还不能当成已经看见

广角、苗条臂、真实第一人称透视都没有游戏画面。持物近景是 Blockbench 示意，不是 Forge 投影。第一人称持物的火在物品展示旋转之前的手部原点上，和示意里那根杆的镜头不是同一个机位。`RenderHandEvent` 发生在原版提交手臂或物品之前，火焰和武器的遮挡顺序没有实拍。入场挂的是同一套骨骼，游戏里有没有播对也没看过。
