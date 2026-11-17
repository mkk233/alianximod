我先完整读取这份任务说明，再按其中的范围和约束执行。任务是只读定位：我只查水牢客户端渲染入口和同目录的透明渲染写法，不改文件、不编译。水牢可见效果只有客户端 `PrisonShellRenderer.onRender` 这一处。它现在画的是原版静水六面盒，没有帧动画。后续只需改这个渲染器，并新增一张游戏贴图；`PrisonClientEntry` 和 `PrisonClientState` 的命中、湿度、持续和生命周期都不用动。

## 现有入口

`PrisonShellRenderer` 用 `@Mod.EventBusSubscriber(Dist.CLIENT)` 自注册，没有第二处登记。

```33:57:src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        // ...
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        // ...
        buffer.endBatch(RenderType.entityTranslucent(TEXTURE));
    }
```

| 符号 | 行 | 现状 |
|---|---|---|
| `TEXTURE` | 26 | `minecraft:textures/block/water_still.png`，整张 UV `0–1` |
| `CROWD_DISTANCE` | 27 | 相机距某一面 0.4 格以内就丢掉该面 |
| `onRender` | 33–57 | 阶段 `AFTER_ENTITIES`；`level == null` 或 `visible()` 为空则返回 |
| `draw` | 59–94 | 用快照 `bounds()` 画六面；相机在盒内时 alpha 72，否则 128；顶点色 `(170, 220, 255)`；光照 `0x00F000F0` |
| `nearFace` | 96–102 | 只服务六面留空 |
| `sheet` | 104–114 | 同一面再画一张反向面 |
| `face` / `vertex` | 116–133 | 四个顶点，UV 固定铺满 |

`entityTranslucent` 会写深度，也开背面剔除。现在靠 `sheet()` 正反各画一次，才能从两边看见。新平面不能再这么画，否则透明会叠两次。

数据从 `PrisonClientState.visible()` 来（87–96 行）。`current()`（203–212 行）只保留当前维度、且 `gameTime < endTick` 的快照。`purge()`（162–184 行）在没世界时 `clear()`，换维或到期就删。`visible()` 不过滤 `RELEASED`：合法传送只放开本地锁，画面仍留到删除包。这个差别要保留。

快照字段在 234–245 行：`targetId`、`targetNetworkId`、`anchor`、`bounds`、`lockedTier`、`birthTick`、`endTick`。包由 `PrisonSyncPacket.handle`（140 行）进 `PrisonClientBridge.accept`，再进 `PrisonClientState.accept`（31–63 行）。`PrisonClientEntry` 只做三件事：静态绑定这个 sink（14–16 行）、客户端 tick 末尾 `purge()`（21–27 行）、登出 `clear()`（29–32 行）。

`bounds` 不是实时碰撞箱。服务端 `PrisonShell.expand` 在施放时把目标包围盒外扩 `0.20`，再按聚能倍率冻结。死亡、登出、换维会 `sendRemove`；区块卸载（`UNLOADED_TO_CHUNK`）不结束水牢，也不发删除包。客户端到期和换维仍会自己清掉。

## 最小替换

只改 `PrisonShellRenderer.java`。`PrisonClientEntry.java`、`PrisonClientState.java`、网络包、mixin、服务端牢逻辑和制作表都不改。

游戏资源里还没有这张图，必须另加一份，且不要加 `.mcmeta` 动画，否则贴图管理器会先切帧，渲染器再切 V，等于播两次：

`src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_bubble_v3_strip.png`

从已确认的 `docs/资源库/过程归档/水牢泡泡_2D_v3_斜向错层-20261027/water_prison_bubble_v3_strip.png` 原样复制。96×1536，16 格，每格 96×96。

渲染器里用本文件已有的写法：

`new ResourceLocation(XueXiMod.MOD_ID, "textures/entity/xueximod/water_prison_bubble_v3_strip.png")`

`RenderType` 做成 `static final`，`getBuffer` 和 `endBatch` 必须是同一个对象。状态对齐 `StillSheetMesh.of`（17–36 行）和 `WaterCurtainRenderer.curtainMesh`（87–104 行）：

- `DefaultVertexFormat.NEW_ENTITY`，`QUADS`，缓冲 256
- `RENDERTYPE_ENTITY_TRANSLUCENT_SHADER`
- `TextureStateShard(texture, false, false)`：最近邻，无 mipmap
- `TRANSLUCENT_TRANSPARENCY`（`SrcAlpha, OneMinusSrcAlpha`）
- `NO_CULL`
- `LIGHTMAP` + `OVERLAY`
- `LEQUAL_DEPTH_TEST`
- `COLOR_WRITE`（只写颜色）
- 顶点色 `(255, 255, 255, 255)`，`uv2(0x00F000F0)`，overlay `0`

仍在 `AFTER_ENTITIES`。此时不透明地形和实体已经写入深度。深度测试会挡住墙后的片；关闭深度写入后，它不会变成隔墙永远可见，也不会把后面的半透明物硬挡住。片比目标最近角再靠近相机 0.08 格，所以水纹盖在目标前方。只提交一张四边形，不要再调用现在的 `sheet()`。

`nearFace`、`CROWD_DISTANCE`、六面循环和蓝色顶点色都可以删掉。

## 相机正对的一张面

`RenderLevelStageEvent` 的 `PoseStack` 已经含相机旋转（含受伤和步行摇摆），不含相机平移。保持现在的 `pose.translate(-camera)`，顶点用世界坐标。世界坐标先在 double 里减去相机，再转 float，不要把绝对世界坐标直接塞进 float。

朝向只用本客户端相机，含俯仰，不读实体 yaw，也不同步：

- 前：`camera.getLookVector()`
- 上：`camera.getUpVector()`
- 左：`camera.getLeftVector()`
- 右：左向量取反

1.20.1 里 `getLookVector()` 是局部 `(0,0,-1)` 转出来的，指向视线前方。四边形法线指向相机，也就是 look 的反方向。四个角在视线的右轴和上轴上，半边长相同。从相机看去逆时针：左下、右下、右上、左上。整张面位于同一个视空间深度，不要只把盒心沿视线推 0.08。

投影用 `event.getProjectionMatrix()`。JOML 列主序：`m00()`、`m11()` 对应预览脚本里的 `elements[0]`、`elements[5]`；`m30()`、`m31()` 对应 `elements[12]`、`elements[13]`。原版透视近平面是 `0.05`。先把 `camera + look` 乘进当前 pose，确认它落在视空间的哪一侧，后面都跟这个符号走，不要写死假设。

## 等比缩放和前移

尺寸采目标实时碰撞箱，不采冻结的牢壳。

1. `level.getEntities().get(snapshot.targetId())`。本仓库还没有这个 UUID 调用，但 1.20.1 的 `LevelEntityGetter.get(UUID)` 就是这个用途。水牢不限玩家，不能只用 `getPlayerByUUID`。
2. 找不到且 `targetNetworkId >= 0` 时，用已有先例 `level.getEntity(int)`（`ActionClientPlayback` 246 行）。
3. 找到且存活：用 `getBoundingBox()`，再按 `partialTick` 平移。平移量和 `GuardRingRenderer` 59–61 行一样：`Mth.lerp(partialTick, xo, getX()) - getX()`，Y/Z 同样。`partialTick` 取 `event.getPartialTick()`。
4. 盒子任一边小于 `1.0E-4`，或坐标非有限：跳过。这是现在 62–64 行的退化判断。

然后取 8 个角：

- 深度不大于 0，或投影后 `clipW <= 1.0e-4` 的角丢掉。相机后方的角做透视除法会爆。
- 有效角不足，或投影宽高不是有限正数：跳过。
- `nearestDepth` 取有效角里最小的正向深度。
- 平面深度 `d = max(0.05 + 0.05, nearestDepth - 0.08)`，也就是不近于 `0.10`。校验记录里平面深度恰好比最近角小 `0.08`。
- 把有效角投到 NDC。中心是 NDC 包围盒中心，不是碰撞箱中心的投影。
- `bodyW = (highNdcX - lowNdcX) * (d / m00)`，`bodyH` 用 `m11`。
- `size = hypot(bodyW * 96/70, bodyH * 96/74) * 1.04`。宽和高都等于 `size`。
- 若 `m30` 或 `m31` 明显非 0，视空间 X/Y 再减去 `m30/m00`、`m31/m11`。普通视角这两项为 0；望远镜缩放时投影矩阵会带上它们。

`96/70` 和 `96/74` 只出现在已确认的 `preview-billboard.js` 第 21 行，用来让画心而不是整张 96 像素外框包住投影。不要改成碰撞箱宽高分开缩放。聚能继续只影响服务端壳和锁定，不放大这张图。

## 16 帧 UV

不要复用 `StillSheetClock.frameIndex`。那个时钟播完就停，水牢要循环。V 的算法可以沿用 `StillSheetClock.textureV`（44–49 行）：帧顶 `localV = 0`，帧底 `localV = 1`。

```text
frame = floorMod(Util.getMillis(), 1600) / 100    // 0..15
topV = frame / 16
bottomV = (frame + 1) / 16
```

U 始终是 `0` 到 `1`。画面上方的两个顶点用 `topV`，下方用 `bottomV`。不在帧之间插值。`birthTick` 不参与播帧；各客户端相位独立，符合不同步朝向的要求。

## 近距离、死亡、卸载

| 情况 | 处理 |
|---|---|
| 相机在采样盒内，或第一人称且目标就是 `cameraEntity` | 不画。旧注释 21–22 行就是为了避免第一人称被一整面挡住；单张公告板无法再留一面空 |
| 人在盒外但很近 | 靠 `d >= 0.10`。整张面在同一深度上，近平面不会切穿它 |
| 目标死亡 | 服务端 `EndReason.DEATH` 后会 `sendRemove`。包到达前若实体还在且 `!isAlive()` 或 `isRemoved()`，这一帧不画 |
| 实体暂时取不到，快照仍 `current` | 用 `snapshot.bounds()` 顶上。区块卸载不会删牢。这只是外扩过的冻结壳，会比碰撞箱大约 `0.20`，还可能带聚能倍率 |
| 到期、换维、登出、删除包 | 继续只靠 `visible()` / `purge()` / `clear()` / `bury()`，渲染器不另写寿命 |
| `RELEASED` | 继续画，直到权威删除包 |

墙的遮挡靠 `LEQUAL` 和已有深度缓冲，不靠把片藏进盒心。0.08 格小于一格方块；相机和目标之间有不透明方块时，方块更近，片会被丢掉。

## 后续作者可改的位置

允许改：

- `src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java`：替换 26–27 行材质，重写 33–133 行的绘制。入口仍是 `onRender`。
- 上面那张新 PNG。只复制，不改像素。

不要改：

- `PrisonClientEntry.java`、`PrisonClientState.java`
- `network/prison/**`、`shu/technique/release/prison/**`、两个 prison mixin
- 制作表，以及体积壳、流带模型

新局部变量按名称门禁：至少 3 个字符，驼峰段不能出现 `min`、`max`、`pos`、`len`。现有代码用 `low` / `high`，可以沿用 `lowNdc`、`highNdc`、`planeDepth`、`bodyWidth`。

## 定向检查

- 渲染阶段仍是 `AFTER_ENTITIES`，每个牢只有 4 个顶点。
- `RenderType` 是半透明、`LEQUAL`、`COLOR_WRITE`、`NO_CULL`，且是同一个 static 实例。
- 宽高始终相等；缩放来自实时碰撞箱的 8 角投影，不是 `bounds` 的三轴。
- 帧是 100ms 循环，V 为 `frame/16` 到 `(frame+1)/16`，没有 mcmeta。
- 第一人称被困者、死亡实体、退化盒子不画；卸载时才退回 `bounds`。
- `accept`、`purge`、`holdLocal`、`noteServerTeleport`、湿度、聚能和协议没有改动。

本次没有改文件，也没有编译。

## 不确定项

- `96/70`、`96/74` 和 `1.04` 没有美术文档，只在预览脚本里。应对齐已确认预览，不要重新量边。
- 100ms 按墙钟 `Util.getMillis()` 计算，暂停时仍会播。若必须跟游戏 tick 暂停，应改成 `(gameTime + partialTick) / 2`，服务器掉 tick 时帧会变慢。现有牢渲染没有帧时钟。
- 视空间 Z 的符号应以 `camera + look` 乘进当前 pose 的结果为准。1.20.1 通常在前方是负 Z，但不要脱离这次 pose 写死。
- 近平面按原版 `0.05` 估，因此平面不近于 `0.10`。本次没有拆开工程里的 `GameRenderer` 源码核对。
- 实体不在客户端时退回 `bounds`，画面会比碰撞箱大一圈，聚能倍率也会留在这只退路里。主路径不跟随聚能放大。
- 多个牢重叠时，关闭深度写入后混合顺序未排序。旧六面盒也没有排序。