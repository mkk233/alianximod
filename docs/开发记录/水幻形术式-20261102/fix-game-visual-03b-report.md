# 水幻形缺手 03b

诊断版，缺臂尚未修复。这次只在真实驻留绘制上加了限量日志，没有改画面。诊断日志不是当前这次运行的测量；现在的客户端里还没有这些类，日志里也不会有标记。

左臂缺失、左腿偏浅仍是待修缺陷。静态投影不能代替这一帧的身体朝向、相机朝向和遮挡。没有把臂加宽、平移、复制、改色，也没有关掉深度测试。

## 标记和上限

日志标记是 `WATER_PHANTOM_VISUAL_DIAG_03B`。删除标记写在 `WaterPhantomVisualDiagnostics` 第 29–33 行：确认缺臂后删除该类，并去掉 `CloneRenderer`、`SteveWaterLineMesh` 里的调用。这不是玩法功能。

整个客户端进程只锁第一具水幻形。驻留绘制第 1 次立即记，之后大约每 1 秒记一次，共 4 次，然后永久停采。常量在第 37–38 行：`CAPTURE_LIMIT = 4`，`INTERVAL_MILLIS = 1000`。第 4 次之后另有一行 `closed captures=4`（第 422 行）。非采集帧只做一次身份和时间门闩，以及网格绘制里的一次布尔读取。采集帧才复制矩阵、变换已提交顶点、对四条肢体各做一次方块射线。资源哈希只在第一次采集读一遍。异常只记一条 `diagnostic-failed-once`，不改绘制，也不反复刷日志。

预计 4 条采集记录加 1 条结束记录。每条采集是一行紧凑 JSON，大约几 KB 到十几 KB，四次合计不超过约 50KB，然后停止。没有逐帧日志，没有延迟循环任务。

## 记下的字段

这些数只在下次重启后的真实绘制里产生。坐标都带空间名，不把存储坐标、世界坐标和相机坐标混写。

- 采集序号、实体 UUID、数字 id、tick、partial、驻留相位秒、客户端毫秒。
- `alignBodyYawDegrees`：实际传给 `alignVanillaSteve` 的朝向。`renderBodyYawDegrees`：`render` 参数。`entityYawDegrees`：`getYRot()`。
- 相机 yaw、pitch（度）。实体世界坐标、相机世界坐标、相对位置（实体减相机）。
- 已加载网格和皮肤的资源路径，以及资源流的 SHA-256。读失败写 `unread`。
- 六个 Steve 部件分开记。名字是加载顺序标签，不在 `lines.bin` 里：0 `steve_arm_nx`，1 `steve_arm_px`，2 `steve_body`，3 `steve_head`，4 `steve_leg_nx`，5 `steve_leg_px`。同一条记录里的枢轴可以核对这个标签。
- 每个部件的动画时间、缩放、旋转角度、位置通道、枢轴、四边面数。被缩放隐藏或没有消费者时 `submitted` 为 false，并写 `hidden-scale` 或 `no-consumer`。
- 真正提交时：传给 `vertex` 的模型视图矩阵，按行 `m00,m01,m02,m03,m10...` 记下。顶点用同一次 `matrix.transform` 得到观察空间包围盒和中心，空间名是 `view`。投影矩阵在采集开始时复制一份，再用它得到 NDC，空间名是 `ndc`。
- 只对四条肢体、只在这 4 帧，把观察空间中心按相机 yaw/pitch 的逆旋转换回世界，再从相机到该点做一次方块碰撞射线。字段是 `blockRayCandidate` 和 `derivedWorldFromView`。这是遮挡候选，不是片元深度测试。`gpuDepthUnread` 为 true。没读 GPU 状态，没有读回，没有改渲染状态。

## 没有记下的字段

没有片元是否被墙的深度测试丢掉，没有帧缓冲像素，没有散开阶段，没有第 7 个及以后的水花和水滴，没有第一具以外的幻形，也没有 4 次之间的帧。派生世界坐标只逆了相机的 yaw/pitch，视角晃动不在这次逆变换里；观察空间包围盒和那份模型视图矩阵才是直接从提交矩阵来的。

## 改动位置

- `src/main/java/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.java`：新文件。门闩第 235–247 行，打开采集第 101–115 行，结束第 117–138 行，隐藏部件第 141–157 行，已提交部件第 159–232 行，射线第 306–352 行，记录第 354–414 行。
- `src/main/java/com/bslx/XueXiMod/client/CloneRenderer.java` 第 52–67 行：只在 `WATER_OUTLINE` 驻留路径上打开和结束采集。成功时仍是 push、对齐、绘制、pop。绘制抛错时不额外 pop，只清采集标志。
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineMesh.java` 第 87–88 行给出皮肤位置；第 95–155 行在原有顶点提交之后、`pop` 之前记下六个 Steve 部件。顶点调用顺序没改。

`SteveWaterLineVisual`、`SteveWaterLineReload`、`CloneOutlineVisual` 没改。确认模型和贴图没改。

名字门禁已在项目根通过，退出码 0。没有编译。没有实机复测。

## 下次怎么拿到日志

等指定编译把这次源码编进开发客户端。然后完全重启当前开发客户端，不要用 F3+T，也不要只重进世界。进入世界后释放一具水幻形，让它在画面里停留大约 4 秒。到 `D:\alianximod\run\logs\latest.log` 搜索 `WATER_PHANTOM_VISUAL_DIAG_03B`。若进世界时已经有一具水幻形，第一笔绘制就会开始这 4 次采集。不需要额外 JVM 参数或指令。

尚未修复。未编译。未实机复测。写入停止。

本阶段额度已用完，返修结果未追加复审。
