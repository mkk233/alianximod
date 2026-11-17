我先通读这份任务说明，确认授权范围和要做的事，再按它执行。任务是本批非视觉实现的只读复核。我先读用户决定、设计和四份实现报告，再对照合同核对实际代码。四份报告已读完。接下来对照合同核对核心、捕获、装配和客户端的实际代码，先定位关键入口和跨组调用。**结论：FAIL。** 这是水幻形非视觉实现的第 1/3 轮只读复核。命中体积和断潮贴图相差半个纵深，同维主人离开 160 格后收不到表现包，同一进程换世界后部署节流表仍会误拒。未编译，未进游戏。视觉 v2、正式 `PLAYER` 注册和专项声明仍未接线，这些不记为缺陷。

## 问题

### 1. 高 — 断潮画面比实际斩击再往前半个纵深

作者：A、D。

服务端把体积中心放进通知。`CapturedResponseRuntime.volumeCenter` 已是眼睛沿冻结视线前移 `depth/2`：

```337:340:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CapturedResponseRuntime.java
    private static Vec3 volumeCenter(CapturedResponseTicket ticket) {
        Vec3 look = ticket.actorFrame().lookUnit();
        return ticket.actorFrame().eyeOrigin().add(look.scale(ticket.payload().depthBlocks() * 0.5D));
    }
```

该点由同文件 252–253 行交给 `emitVolume`。客户端把这个坐标原样存成锚点（`CloneAfterimagePlayback.java` 290–295 行），绘制时又加一次半纵深：

```83:83:src/main/java/com/bslx/XueXiMod/client/ActorVolumeVisual.java
        Vec3 center = snapshot.anchor().add(forwardUnit.scale(snapshot.depthBlocks() * 0.5d));
```

触发：任意一次成功的断潮体积，默认纵深 5 格时画面中心比命中盒中心超前 2.5 格。命中仍从眼睛覆盖到前方 5 格，画面却覆盖大约 2.5 到 7.5 格。设计第 9 节要求画面变换与精确判定一致。

最小修复：`VOLUME` 的 `worldPosition` 已经是中心，`ActorVolumeVisual.draw` 直接用 `snapshot.anchor()` 作中心，不要再加 `depth/2`。维持和散水仍用脚底，不要改那两条。

### 2. 中 — 同维主人离开 160 格后看不到散水和斩击

作者：D；A 的通知里没有主人标识，接收者被交给按坐标再筛选的转发。

`ClonePresentation.audiencePresent`（`ClonePresentation.java` 151–164 行）把同维主人算作“有人可发”，因此通知会发出。`ClonePresentationS2CPacket.relay` 随后只发给距锚点 160 格内的玩家：

```209:212:src/main/java/com/bslx/XueXiMod/network/ClonePresentationS2CPacket.java
        for (ServerPlayer viewer : serverLevel.players()) {
            if (viewer.distanceToSqr(anchor.x, anchor.y, anchor.z) > reachSquared) {
                continue;
            }
```

通知字段没有主人 UUID。驻留寿命为 0 时身体可以一直留在原地。主人跑出 160 格后，接触或受击仍会在原处结算，但散水和斩击包不会发给这名主人。160 格内的其他玩家可以收到。立即模式生成在主人脚边，这一条不会漏掉站在原地的施放者。

最小修复：不改包布局。服务端 `relay` 按 `deploymentId` 从 A 的部署表取出主人，同维且在线就额外发一份；现有去重键继续保证同一玩家、同一动作、同一阶段只发一次。

### 3. 中 — 成功部署的 tick 表是静态的，换世界后会误拒

作者：C。

```41:41:src/main/java/com/bslx/XueXiMod/shu/technique/release/capture/ActorCaptureDispatcher.java
    private static final Map<UUID, Integer> ACCEPTED_TICK = new ConcurrentHashMap<>();
```

成功时在 174 行写入 `ownerId → serverGameTick`，54–56 行按相等拒绝。没有任何登出、停服或换世界清理。同一 JVM 里新建集成服务器后，`tickCount` 从 0 重新增加。上一局曾在 tick `T` 部署成功的玩家，新局 tick 再次到达 `T` 时会被拒绝，提示「这一刻已经部署过水幻形」，而且不收费。同一局内这个表的行为是对的。专用服务器整进程重启会清掉静态字段，这条主要打在不重启进程的换世界上。

最小修复：停服和主人登出时删掉对应项。只保留当前这一局、这一 tick 的成功记录。

## 八项里核对过、不另开缺陷的部分

1. 结构检查在 `BatchReleaseCompiler.compile` 之前。`NOT_APPLICABLE` 返回空并继续旧释放，不解除普通长按。未知编号和坏原胚在 `castRefusal` / `rawEntryFault` 里拒绝，不靠 `asSkill` 跳过后再变成两格计划。施放使用 `permitIncompleteDraft=false`，半链、反序、草稿不能释放。
2. 类型例外只给法杖槽 0、就绪计划里已消费的载荷下标。`castRefusal`、`offerNow`、`routeCapture`、分发器、法杖追加和菜单替换后的紧凑候选用的是同一个门。捕获写回失败会恢复法杖和匣子标签，或退回齿轮与物品。普通法杖裸剑术、剑技槽法术拒绝还在。`water_phantom` 未注册，所以现有旧链仍是不适用。
3. 付款只有 `spawnCaptured` 里一次 `ManaPaymentReceipt.charge`。发布前失败退一次；发布成功不退。魔力和冷却先按 `long` 相加，溢出拒绝。伤害、魔力、冷却各读一次，玩家冷却再 `scaleCooldown` 一次。成功时 `fired()` 只有宿主。拒绝和计划都会 `HoldRepeat.drop`。
4. `PreparedCapture` 只接受 `PLAYER_CAPTURED_CAST`，网络包不能反序列化成它。身体、票、体积共用同一份 4/128 名额，转出后记在脱离计数里，直到 `release()`。旧队列和捕获票共用每 tick 32 次提交。空转要身体、旧队列、主人记录、捕获表和体积都空。登出、死亡、换维、世界卸载和停服会取消票和体积。旧 `spawn` 仍走开发门。除上面第 3 条的 tick 表外，体积表和发送账会在停服清掉。
5. 接触和 `push` 走同一套资格，排除主人、友方、宠物、同队和任意 `CloneEntity`。立即模式 `cosmeticOnly` 不碰撞、不受击、不诱敌。致死门只对 `BREAK_ON_TRIGGER` 且租约仍是 `BODY` 打开。`super.hurt` 的真实返回值保存后再决定是否转票；Forge 取消且返回 false 时留在就绪。硬移除、主人死亡和异常的 `finally` 都会收口，不重复转票。
6. 断潮体积不调用旧效果、`spawnReported`、`tryHit`，也不读 `CastFrame`、宿主组或 `Tally`。粗筛是外接 AABB，命中要 OBB。阻挡用主人作射线实体，`COLLIDER` / `Fluid.NONE`。结算前查权限并排除分身，再 `classifyRespecting(..., NONE)`。每次结算重新打开来源作用域并在 `finally` 关闭。版本不对或提交失败会结束租约。
7. 性能枚举已在末尾：五个计数和一个 `ACTOR_VOLUME_UPDATE`。脉冲显式 `full()`，内置适配器只有断潮。捕获能力来自定义，不靠术式编号开门。客户端播放类标了客户端侧，表现包在非客户端直接返回，专用服务器不会为了收包去执行客户端播放。
8. 包是发往客户端，校验维度、会话、有限坐标、时长和枚举。客户端入队后处理。快照和终止表各上限 256。实体离开只结束维持；已收下的散水保留。迟到维持在终止后丢弃。换世界和断线清空。立即模式不依赖实体已被追踪，160 格内的玩家仍能收到世界坐标事件。断潮只画现有 `horizontal_slash.png`，不生成 `TechniqueHitEntity`。画面位置错误见问题 1。

报告里的额外上限，按已声明边界不会误伤合法断潮。尺寸上界是 512。三个方向都取 512 时，外接区域约 56×56 个区块列，低于 4096，不会被当成未加载而结束。眼睛到相交点在这个尺寸下约 627 格，低于 1024，不会因此跳过射线。冷却超过 100 万会被报价拒绝；标准冷却上界是 120 万。这是合同第 3.2 节写明的闭区间，实现没有再收紧，所以不另记一条实现缺陷。

## 未跑与仍未完成

未运行 `compileJava`，未打包，未进游戏，未跑命名门禁。正式 `PLAYER` 注册、`ReleaseRegistry.register`、`WaterPhantomBalance.declareSpecials()` 都还没有调用方。Steve 水线 v2 仍待确认，新人形和散水没有绑定帧。这些问题不记成这轮代码缺陷，完整术式也还不能算交付。
