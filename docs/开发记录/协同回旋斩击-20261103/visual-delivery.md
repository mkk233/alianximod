# 协同回旋斩击：客户端与确认美术交付

版本：`cooperative-slash-visual-v1-aligned`。本文件是 V 的实现说明。没有编译，没有游戏验证，没有正式实现复审。

## 给 I 的工厂

网络已在 `ModNetwork.register` 第 53 行登记为通道序号 17。不要再次 `register`。

```java
ActorActionPublisher actions = CooperativeActionPublisher.publisher();
actions.start(playback);
actions.stop(family, actorId, sequence);
```

`CooperativeActionPublisher` 自己监听登录、开始跟踪、登出、换维、停服和服务器刻。登录与 `StartTracking` 会补发未结束快照。I 只在动作开始和停止时调用上面两个方法。

斜斩体积要让客户端认得出表现，命中实体需带已同步字段：

- `techniqueId` = `sword_oblique_slash`，或 `texture` = `xueximod:textures/entity/xueximod/oblique_slash_v1.png`
- 表现标识常量是 `CooperativeSlashPresentations.PRESENTATION`（`xueximod:oblique_slash_v1`）。把它写进 `texture` 也会命中
- 实体位置是刀面中心。眼前 1.5 格由服务端原点完成，客户端不再加一次
- `length` 沿瞄准作纵深，`width` 作刀面宽度。`height` 只留作逻辑厚度，平面不挤出
- `yRot` / `xRot` 沿用 `TechniqueHitEntity.placeAt` 的瞄准，`roll` 是刀面倾角
- 横劈 `sword_horizontal_slash` 即使贴图字符串相同也不走这张斩光

没有新的 mixin 配置。`xueximod.mixins.json` 不用改。`LivingEntityRendererBodyMixin` 未改，空间切割补臂和水幻形绘制顺序保持原样。

## 动作协议

`CooperativeActionS2CPacket` 只有开始、停止、跟踪三种。不逐刻广播骨骼。

服务端缓存每个演员一条，上限 `ACTIVE_FAMILY_CAP * ACTORS_PER_COOPERATION`（256）。旧序号的停止不清除更新的动作。回旋未结束时，`oblique_slash` 的 `start` 被丢掉，避免出刀姿态盖掉回旋。客户端同样拒绝这种迟到包。

客户端按 `serverStartTick` 和当前世界刻计算已经走过的时间，迟到和中途跟踪都从中间采样。结束、卸载、世代不符、换维、断线会清缓存。缓存上限同样 256。未见实体时先等 20 刻，避免出生包晚于动作包。

`ActionRuntime.open` 拒绝这两个动作标识，旧姿态通道不会再播一份。`OriginalHumanoid` 只登记空保持剪辑，通知列表为空，不能用来触发出刀。

时长：斜斩创作曲线是 0 秒前摇 + 0.40 秒收势；回旋是 0.30 + 0.30 秒。服务器冻结的前摇和收势刻数分别拉伸这两段。

## 确认资产

同版六文件已核对 SHA256：

| 文件 | SHA256 |
| --- | --- |
| `slash_arc_v1.png` | `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7` |
| `cooperative_slash_v1.bbmodel` | `4021566A08D74C34E761ADBDF9EBB96E56638B836F2C660224020B066A09DD7A` |
| `solo_aligned_v1.gif` | `B51EB79FFD66DBFEEF5FCC4E43806646A31B04CB24EDF02DFFD6D36534CC461E` |
| `spin_aligned_v1.gif` | `AD023E06D437DE97D9492DC42A9E7234BD4E9A32E0CC788816F8D3D797649EA8` |
| `cooperative_aligned_v1.gif` | `07CF2D86508B6E52F3B50CEADBA0513F25F5588752BFCFF1B6E82ACF72E8F192` |

运行时 PNG 与确认 PNG 逐字节相同，路径 `assets/xueximod/textures/entity/xueximod/oblique_slash_v1.png`。Blockbench 纹理名是 `slash_arc_plane_v1`。

动作 JSON：

- `assets/xueximod/actions/cooperative_v1/oblique_slash.json` 来自 `animation.preview_solo`
- `assets/xueximod/actions/cooperative_v1/spin.json` 来自 `animation.preview_spin`

`preview_spin` 的骨骼角与 `animation.preview_cooperative` 去掉前 0.10 秒后一致。正式回旋不再保存这段归位位移。`normal` 取 player 演员，`mirrored` 取 clone 演员。斩光组、地面、假人、占位剑都没有进动作。

`models/effect/cooperative_v1/slash_plane_v1.json` 只记录平面和相位，游戏不加载它。

## 坐标

Blockbench：Y 朝上，面向 -Z，16 单位为 1 格，欧拉角 XYZ，单位度。

模型部件在原版 Y 朝下的空间里使用相反的三个角：`xRot = -bbX`，`yRot = -bbY`，`zRot = -bbZ`。根骨骼只有 Y 转。它先乘到各骨骼旋转上，再绕模型原点转枢轴，不写实体位置、碰撞盒、相机或实体朝向。

冻结 `visualYaw` 与当前身体朝向的差，加进同一根偏航，实体的 yaw 字段不动。

握柄：剑枢轴 `[-6,13,0]`，右臂枢轴 `[-5,22,0]`，父空间 `[-1,-9,0]`。翻到模型 Y 朝下后是 `[-1,9,0]` 像素。剑组没有旋转关键帧。分身用这个偏移画不可交互物品副本。玩家仍用自己手上的物品，第一人称只额外转动物品。

斩光相位（相对出刀，秒 → 缩放）：0 为 0.92，0.04 为 1，0.14 为 1，0.20 为 0.001。默认 4 刻窗口就是这 0.20 秒；实体寿命不同时按比例拉开。平面双面，全图 UV，全亮，没有厚度。缩放低于 0.02 不再画。

## 和确认预览的差异

- 单人回旋预览里分身被缩到几乎看不见。运行时镜像轨道仍用那套旋转，需要镜像时按正常大小画。
- 协同开头 0.10 秒的侧向归位不在动作里。脚点移动只来自分身真实位移。
- GIF 末帧多停的 0.65 秒不是施法硬直，运行时到服务器时长就清姿态。
- 斩光不挂在回旋或斜斩动作上，只跟实际发出的斜斩体积走。只发出一份就不会画第二条。
- 第一人称没有 Blockbench 手臂网格，只把主手或副手的手臂欧拉角乘到已有手持物上。第三人称写的是原版人体骨骼。
- 青灰假人、Steve 预览皮、占位钢剑和地面不进运行时。普通分身仍用现有外观。
- 刀面 ±35 度不写死在网格里，用实体已同步的 roll。正对刀面边缘时会变窄，不是镜头公告板。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未运行 `compileJava`。未启动游戏，不能声称观感已经验证。
