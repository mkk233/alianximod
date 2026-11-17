先读返修任务说明，按里面写明的范围改。七项裁决已经对齐。开始改坐标、快照分片、接触记录、槽位配置、零伤拒绝、周期清理和缺席节拍。命名门禁只剩未改过的两处旧违规。我核对七项裁决的落点和行号，再取文件时间并写最终报告。灼地实现返修 1 已按授权范围写完并停止写入。设计仍是 2/2，实现复审仍是 1/3。这次没有新开复审，也没有重审设计。

## 七项裁决

1. **碰撞盒先换成世界坐标。** `SurfaceContactField` 第 60–61 行在方形裁剪、顶面过滤和净空扣减之前，用 `box.move(columnX, layerY, columnZ)` 把碰撞盒平移到方块的世界坐标，平移方式和立地表面一致。第 77 行之后才按参考高度上下 3 格过滤，再扣净空。已去掉 128 片截断。

2. **接触和采样保留全部合法片；画面分片传输。** 服务端接触、采样用合并后的完整列表，没有静默丢片。同高度、共边、并集仍是矩形的片可以合并，面积不减；部分重叠的不同片保留。画面上限在 `SurfaceSnapshotFrames`：
   - 单包最多 48 片，坐标正文 48×6×8 = 2304 字节，单包预算 2560 字节
   - 最多 64 个分片，总片数 3072，总字节 163840
   - 传输字节按「片数 × 48 + 分片数 × 128」计算
   - 客户端未收齐最多等 80 刻，同时最多记 16 个宿主的未完成快照
   - 寿命跨度必须正好 100 刻

   每个 `SURFACE` 包带宿主 id、几何版本、分片下标和分片总数。这些写进既有整数字段：`remainder`、`interval`、`segmentCount`。切片字节只加在 `SURFACE` 后面，`GROUND` 的固定头和绘制不变。客户端只有同一版本的 0 到 count-1 全部到齐才替换画面，不把新旧片混在一起。已有画面只换切片，出生和结束时刻保持不变；同一版本的重发不重置。结束包和客户端清空会丢掉未收齐的快照。超出上限时 `SurfaceFireHosts` 第 356–361 行记一条警告并拒绝整版，不截断，也不把这次当成同步成功。地形签名变化才升版本；40 刻重发沿用同一版本。空几何发 1 个 0 片分片，表示清空成功。

   覆盖依据是算术，不是实测：边长 4 的区间每轴最多跨 5 列，共 25 列；参考高度上下 3 格最多 9 层，共 225 个采样方块。3072 片约合每个采样方块 13.6 片。按每方块 4 个碰撞盒、不做合并估算是 900 片，低于上限。没有在楼梯建筑上点数，也没有进游戏。若合并后仍超过 3072 片，接触判定仍用全部片，客户端不更新。

3. **每个宿主记自己见过的目标。** `SurfaceFireHosts` 第 265–275 行：`seenTargets.add` 为真才是这个宿主第一次见到该目标，这时只尝试共享的首次奖励；无论成功失败，这一刻都不提交这个宿主的周期。已经见过且仍相交的目标才走周期。离开不清除集合。第 425 行只在宿主结束、移出宿主表之后清空。不靠 `claimFirst` 的返回值区分，也不靠 `HashMap` 顺序。同一槽的复制份共用首次令牌和周期门，但各自的「见过」集合独立，所以同一刻都算初见，都不会记周期。

4. **计划和附魔快照按真实槽位记。** 键是「通道 + actionIndex」。普通链通道是 `chain:` 加本次按键身份；条件组通道是 `group:` 加按键身份和组 id，记在那一组自己的释放账上。`explicitOffer` 只读不删。`SkillSegmentRunner` 第 741 行在执行当前段之前把已解析配置放进线程局部，第 755 行在原有的 `finally` 里清掉。`ScorchEffect` 第 30–31 行读这份配置，不再按术式 id 查找。没有配置时火种仍结算，只是没有绑定计划和附魔快照。同一槽的复制份共用这个下标。链语义没有改。

5. **零接触伤害的纯效果附魔沿用原拒绝。** `ContactCast` 第 30–38 行：只对显式接触宿主（`explicitDomainHost`）在伤害不大于 0 时返回原句「没有附加伤害的接触附魔暂不支持，这次没有释放」。`SkillUsePacket` 第 372–386 行仍在扣费和开启执行会话之前拒绝。普通正伤害攻击仍跳过这项检查。正伤害附魔的点燃和火种路径没有改，原生 1 点伤害加首次 20、周期 15 不走那条路。无敌帧保持原样，没有强制同一刻打两次。

6. **到期周期门每服务端刻清理。** `SurfaceFireHosts.onServerTick` 第 109 行在复制宿主表之前调用 `ScorchSeedLedger.discardMaturePeriods(now)`。宿主表为空也调用。离开、单个宿主结束和维度卸载不清理共享门。未满 20 刻的门保留。

7. **只有新宿主在主人缺席时跳拍。** `HostView.skipsAbsentOwnerBeats` 默认返回 false。灼地宿主在第 451–452 行返回 true。`DomainBindingRuntime` 第 88–93 行：主人死亡、离线或在另一维度时，若该标记为真，就把节拍推到按出生时刻算出的下一拍后返回，不执行、不收费，也不留一轮补发。旧领域不推进，回来后仍是原来的补一轮。

## 改过的文件

| 文件 | LastWriteTime |
| --- | --- |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\firecast\SurfaceContactField.java` | 2026-10-30 21:18:57 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\firecast\SurfaceFireHosts.java` | 2026-10-30 21:20:56 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\firecast\SurfaceSnapshotFrames.java` | 2026-10-30 21:22:37 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\domain\HostView.java` | 2026-10-30 21:18:58 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\domain\DomainBindingRuntime.java` | 2026-10-30 21:19:01 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseBatch.java` | 2026-10-30 21:19:38 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\ContactCast.java` | 2026-10-30 21:19:03 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\DomainOpening.java` | 2026-10-30 21:19:16 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\ExplicitHostOffers.java` | 2026-10-30 21:19:21 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillUsePacket.java` | 2026-10-30 21:19:21 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\CastExecutionSession.java` | 2026-10-30 21:19:22 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillSegmentRunner.java` | 2026-10-30 21:19:25 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\FireVisualPacket.java` | 2026-10-30 21:20:41 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\effects\ScorchEffect.java` | 2026-10-30 21:19:31 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\SurfaceSnapshotAssembly.java` | 2026-10-30 21:22:37 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\FireVisualClient.java` | 2026-10-30 21:20:46 |

后两个是新增类。制作表、`记录.md`、燎原行为和资源文件都没有改。

## 接口与资源

新增 `SurfaceSnapshotFrames`、`SurfaceSnapshotAssembly`。`FireVisualPacket.surface` 增加几何版本、分片下标和分片总数。`HostView` 增加默认方法 `skipsAbsentOwnerBeats`。`ReleaseBatch.rememberExplicitHost` 增加通道参数，并增加 `explicitOffer`、`bindActiveOffer`、`activePlan`、`activeSnapshot`；`ExplicitHostOffer` 改为公开记录。`ExplicitHostOffers.capture` 和 `DomainOpening.laneToken` 是包内接口。资源映射没变，仍用燎原的 `ground_flame.png` 和 `ground_embers.png`。样式仍是橙色。地表定位、幽冥和蓝火没有做。

## 命名门禁

在 `D:\alianximod` 运行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。本次改过的文件没有命中。剩下两处是未改文件里的旧违规，没有动：

- `D:\alianximod\src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:165` 变量 `i`
- 同文件 `:202` 变量 `to`

## 尚未做的事

没有编译，没有进游戏。统一编译仍待主会话执行 `.\gradlew.bat compileJava --offline --no-daemon`。跟随脉冲仍可能再开一个宿主，这是原有链行为，这次没有改。`DomainOpening` 为空时不记计划和快照。同一通道里若两个段共用一个 actionIndex，后写入的配置覆盖先写入的；同一段的复制份共用下标是故意的。正伤害附魔仍要先打中再按倍率 1 记火种，并可能点燃，也可能因无敌帧错过出生刻。损坏的超长 `SURFACE` 包会被拒绝，而且多出来的 double 不读取，这种包可能让后续字节错位；本模组自己的发送端保持在每包 48 片以内。
