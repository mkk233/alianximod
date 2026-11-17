# 水幻形 v2.1 接入报告（D）

用户原话「这个版本好接入吧」已按确认版接入。本轮没有自行开正式审查，没有跑 Gradle、打包或游戏。命名门禁对当前 `src` 退出码 0。写入到此停止。

## 运行资源

- 皮肤 `assets/xueximod/textures/entity/xueximod/steve_water_lines.png`，2687 字节，SHA256 `9E7E5B01A7E09C393D3FFA1800468BD61AE919F68CF628152B00D9C347793DA5`。与确认 PNG 字节相同。128×128，逻辑 UV 64。
- 水片 `assets/xueximod/textures/entity/xueximod/water_flakes.png`，608 字节，SHA256 `CEFDEE9E3E3FB7F701E19FC9CB3438C586C4841D3A50E3F8C9399A635BD4D70C`。与确认 PNG 字节相同。32×32，逻辑 UV 32。
- 网格 `assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin`，60666 字节，SHA256 `26EF5093F3FC9CAD03272F7BDFAD06DF771953E161B69310B2FF7E37B89F9025`。74 个部件：6 个 Steve 方体、48 个短片、20 个水滴（其中 `drop_00` 到 `drop_07` 是驻留高光）。
- 确认模型 SHA256 仍是 `4ADFCC75ED1E06E62ECE5FD05E6B109787FB1AC46B29CBFC5A8AF8C5425695F7`。假人、地面和整个 bbmodel 不进运行资源。
- 绘制类型是双面、不写深度的自发光半透明，顶点亮度假满。不使用僵尸皮，也不画成公告板。缩放小于 0.015 的部件不提交；确认曲线里的隐藏值是 0.001，终点淡出 0.02 仍会画。数量没有删。

## 尺寸和时间

模型 1 单位按原版 `ModelPart` 的 1 像素，等于 1/16 格。变换顺序与 Forge 1.20.1 `LivingEntityRenderer` 相同：绕 Y 转 `180 - 身体朝向`，`scale(-1, -1, 1)`，再下移 1.501 格。脚底约在实体位置上方 0.001 格，可视高度 2 格。没有用 32/1.8。

写入网格前，确认模型的 Y 向上坐标翻进原版模型空间，位移的 Y 和绕 X、绕 Z 的角度一起变号。旋转按 Z、Y、X 施加，和 `ModelPart` 一样。关键帧只有 step 和 linear，曲线没有重拟合。

驻留时钟是 `(tickCount + partialTick) / 20`，在 0 到 1.12 秒之间循环，站立姿态来自这段关键帧。生成朝向用实体已经冻结的身体朝向。

散水从动画 1.12 秒起算。经过的秒数是 `(gameTime - birthGameTime + partialTick) / 20`，相位钳在 1.96 秒，运动本身 0.84 秒走完。快照仍按原来的通知时长回收，协议和 20 tick 窗口没有改。预览开头的 1.12 秒等待不会加到反击上，99 帧 GIF 也不参与同步。服务端 tick、碰撞和伤害频率没有动。

## 驻留和移除之后

活着的 `WATER_OUTLINE` 由 `CloneRenderer` 每帧画驻留，所以 `lifetimeTicks = 0` 不会在一秒后消失。新观察者靠实体的 start-tracking 看见身体，不依赖那条 20 tick 的 MAINTAIN 快照。

实体 UUID 是 actor id。水幻形通知里的 deployment id 是另一枚 UUID，所以 `CloneEntity.arm` 把 `CloneSpec.deploymentId()` 同步出去，客户端用 `presentationDeploymentId()` 读取。没有部署 id 的旧分身仍回退到实体 UUID。受伤、碰撞、租约和付款没有改。

同一部署标识、会话相符的活实体在场时，不再画 MAINTAIN 世界快照，避免叠两层。相邻距离匹配已经去掉。散水通知把 `scatterSeen` 记下，迟到的身体不再画；散水快照留在世界上，实体 discard 之后继续播完。实体离开只结束维持。断潮仍画在原来的体积中心，主人补发、客户端 160 格重裁剪的删除、世界和断线清理、会话、256 上限和去重都还在。

立即模式仍由现有服务端在下一 tick 发出散水通知，客户端从 1.12 秒直接播。接触和受击是身体结束后散水快照独立播完。

## 缓存、预算和专用服务器

`lines.bin` 在客户端资源重载时解析一次，失败或未加载时空网格，不每帧读文件。绘制只复用三个长度为 3 的采样数组。快照上限仍是 256，没有逐帧网络包，也没有新的平衡键。

新类都在客户端包，重载监听带 `Dist.CLIENT`。`CloneEntity` 不引用客户端类。专用服务器不会为了这套网格去加载客户端渲染器。

## 文件

新增：

- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineMesh.java`
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineVisual.java`
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineReload.java`
- `src/main/resources/assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/steve_water_lines.png`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/water_flakes.png`
- `docs/资源库/特效/水流/水幻形_Steve水线_v2_1/` 下的 bbmodel、两张贴图、三张静帧、GIF、`README.md`、`visual-handoff.md`
- `docs/开发记录/水幻形术式-20261102/runtime-visual-mapping.json`
- 本报告

修改：`CloneRenderer`、`CloneOutlineVisual`、`CloneAfterimagePlayback`、`CloneEntity`，以及 `docs/资源库/README.md`。包协议、`ModNetwork`、`XueXiMod`、lang 和断潮几何没有改。

## 未跑项

这次接入之后的 `compileJava` 没有跑。游戏画面没有实测，不能把视口预览写成已经在游戏里对过。旧的非视觉编译通过不包括这批渲染代码。

已停写。
