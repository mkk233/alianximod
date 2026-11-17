# 水幻形 D 实现报告

日期：2026-11-02。范围只含表现协议、客户端容器和断潮整张贴图绘制。命名门禁已通过。未跑 Gradle，未进游戏。本文件写完后停止写入。

## 写入

| 文件 | 作用 |
|---|---|
| `src/main/java/com/bslx/XueXiMod/network/ClonePresentationS2CPacket.java` | 包、校验、服务端转发、去重账 |
| `src/main/java/com/bslx/XueXiMod/network/ModNetwork.java` | 登记包号 16 |
| `src/main/java/com/bslx/XueXiMod/XueXiMod.java` | 公共初始化安装接收器；客户端绑定播放 |
| `src/main/java/com/bslx/XueXiMod/client/CloneRenderer.java` | 旧人形保持；`WATER_OUTLINE` 早退 |
| `src/main/java/com/bslx/XueXiMod/client/CloneAfterimagePlayback.java` | 世界绑定、快照、终止标记、清理 |
| `src/main/java/com/bslx/XueXiMod/client/CloneAfterimageSnapshot.java` | 只读快照 |
| `src/main/java/com/bslx/XueXiMod/client/ActorVolumeVisual.java` | `TIDE_SLASH_FULL` 整张 UV |
| `src/main/java/com/bslx/XueXiMod/client/CloneOutlineVisual.java` | 维持/散水入口，未绑帧 |

未改 A/B/C 的源码，未改表、语言、资源和美术。未复制 v1/v2。未注册 `water_phantom`。

## 包号和兼容

写入时重读 `ModNetwork.register`：0–15 已占用，下一个空号是 **16**（`ModNetwork.java` 52 行）。0–15 的登记顺序未改。

通道仍是 `PROTOCOL_VERSION = "2"`（`ModNetwork.java` 18 行），两侧字符串必须全等。本次只在末尾追加 16，没有新开握手版本。同一份 `"2"` 的旧包若没有这条消息，收到 16 时无法按本布局解码。

方向固定 `PLAY_TO_CLIENT`。`handle`（112–121 行）发现发送者非空、方向不是发往客户端，或当前物理端不是客户端时，只标记已处理并返回，不入队、不回业务包。客户端路径再 `enqueueWork`。

编码布局，枚举只用序号，没有贴图路径：

1. 维度 `namespace:path`，UTF 上限 256
2. `sessionToken`、`deploymentId`、`actionToken`、`eventSerial`
3. `PresentationKey` 序号、`presentationRevision`
4. 世界坐标三个 double、视线三个 double
5. `NoticePhase` 序号、`durationTicks`、`VolumeVisualKind` 序号

序号按冻结声明顺序：`LEGACY_HUMANOID=0`、`WATER_OUTLINE=1`；`MAINTAIN=0`、`SCATTER=1`、`VOLUME=2`；`NONE=0`、`TIDE_SLASH_FULL=1`。越界序号在 `noticeOrNull` 丢弃。

`schemaAccepts`（231–261 行）还要求：维度文本非空且不超过 256；部署 UUID 不是全零；会话、动作令牌、事件序号不小于 0；表现修订号 `0..1000000`；时长 `0..200`；坐标有限且绝对值不超过 `3.0e7`；视线长度与 1 的差不超过 `1.0e-4`。不合者丢弃。

## 线程和接收者

`installSink` 在 `XueXiMod.commonSetup`（118 行）调用一次 `ClonePresentation.install`。转发方法 `relay`（181 行）只在当前 `MinecraftServer` 的服务器线程上工作；没有服务器、线程不对、维度未加载或字段不合格时直接返回。

接收者取该维度内、与通知世界坐标的三维距离不超过 160 格的玩家（`TRACKING_REACH_BLOCKS`，47 行）。键是玩家、部署、动作令牌和阶段。同一键在保留期内不第二次发送。保留期是 `max(时长, 200 tick)`。账本上限 4096，超出丢掉最旧记录。服务器 tick 末清除过期项；玩家登出删该玩家；该维卸载删该维；停服清空（300–342 行）。

冻结通知没有主人 UUID。160 格以外的同维主人因此不会被这条转发单独补上。合并时若 A 已经按合同选好接收者并保证主人在范围内，现有去重仍保证每个键只发一次。

## 客户端容器

`CloneAfterimagePlayback.bind` 在客户端初始化（`XueXiMod.java` 168 行）。播放器只在客户端线程收包。

- 快照上限 256（39 行）。超出丢掉最旧的一条。
- 终止表上限 256。过期点是收下时的客户端游戏刻加上 `max(本次时长, 200)`。200 是协议允许的最长时长，用来盖住跟踪期内还在路上的维持包。
- 同一部署同一阶段只收一次。该部署上更小的事件序号丢掉。
- 已有终止标记时，迟到的 `MAINTAIN` 丢掉，不把人形再拉起来。
- `SCATTER` 置终止标记，并立刻删掉该部署的维持快照。
- 实体离开只对 `WATER_OUTLINE` 结束维持（141–160 行）。已收下的散水和斩击快照留到各自时长结束。离开时优先用已建立的实体链接；没有链接时，用 2.5 格内最近的维持快照。
- 客户端世界对象换成另一个、当前世界为空、登出、或绑定中的世界卸载时，快照、终止表、实体链接和会话一起清空（112–120、164–171、189–196 行）。维度与当前世界不同的包直接丢掉，不清空当前世界。
- 会话：这个客户端世界第一次收下的会话被记住。之后不同会话丢掉。场上分身若已有正的 `syncedSessionToken()`，包里的会话必须和其中一具相同。小于等于 0 的同步值不参与否决，避免尚未同步的旧分身挡住独立世界坐标事件。

`LEGACY_HUMANOID` 的维持和散水不进快照，仍由 `CloneRenderer` 画僵尸。`VOLUME` 且视觉不是 `TIDE_SLASH_FULL` 的包只占去重，不建快照。

## 断潮画面

`ActorVolumeVisual`（66–90 行）把通知里的世界坐标当作眼睛原点，视线当作前向。中心是 `眼睛 + 前向 * 纵深 / 2`。右轴和上轴按合同：视线接近竖直时用 `(1,0,0)` 作临时右轴，否则视线叉乘世界上方向。半尺寸是宽、高、纵深的一半，画成这个有向盒子的六个面，每面整张 UV `0..1`，双面。贴图是 `xueximod:textures/entity/xueximod/horizontal_slash.png`。不创建 `TechniqueHitEntity`，不调用旧命中。

冻结通知没有宽、高、纵深。客户端在收下 `TIDE_SLASH_FULL` 时读一次现有断潮读口并写进快照：`SpecialBalance.number("tide_sever", beamLength, 5)`、`boxHeight` 默认 3、`ReleaseRegistry.get("tide_sever").thickness()`，定义缺失时厚度用 0.4。之后这一份快照不再重读。数值必须有限、大于 0、且不超过 `BalanceBound.SIZE_UPPER`（512）。平衡侧抛错或尺寸不合格时，这一阶段仍记为已见，但不建斩击快照。

这里的尺寸是收包当时的已生效断潮配置，用来对齐报价用的同一读口。通知本身带不走施放当时冻住的三个数。世界重开会清空客户端快照。

## 旧渲染和未绑的人形

`CloneRenderer.shouldRender`（33–38 行）和 `render`（42–45 行）：`presentationKey()==WATER_OUTLINE` 时直接返回，不画僵尸，也不读候选贴图。其它键仍走原来的僵尸模型和 `textures/entity/zombie/zombie.png`，序列变化时的挥臂逻辑还在。

`CloneOutlineVisual.drawMaintained` / `drawScattered`（15–27 行）是确认后的 Steve 水线入口。播放器会把 `WATER_OUTLINE` 的维持和散水快照送进来。两个方法现在不取帧、不绑贴图、不绑 UV、不绑模型、不绑地面水纹。

## 合并前依赖

本任务按冻结签名调用，当前源码树里这些类型还要等 A 落地后才能一起编译：

- `ClonePresentation.install` / `ClonePresentationNotice`
- `PresentationKey`、`NoticePhase`、`VolumeVisualKind`
- `CloneEntity.presentationKey()`、`CloneEntity.syncedSessionToken()`

D 没有调用 B 的 `ActorVolumeGeometry`。正交基和中心公式写在 `ActorVolumeVisual.basis`（95 行起），需要与 B 的最终几何保持同一合同。`VOLUME` 的世界坐标按眼睛原点解释；若 A 放进通知的是体积中心，斩击会再沿视线前移半个纵深。

正式水幻形仍等待用户确认的 Steve 水线交接，以及第 9 节载体门之后的注册。本任务没有声明新的平衡键。

## 验证

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未执行 `gradlew compileJava`。未启动游戏，未做客户端画面实测。统一编译要等 A/B/C 与本文件一起收束后再做；接线前的编译不能当作最终编译。
