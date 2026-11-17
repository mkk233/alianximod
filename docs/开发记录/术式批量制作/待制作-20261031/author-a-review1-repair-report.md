# 作者 A 首轮返修报告

本单是已批准需求下的返修，不是新的正式复审。设计 2/2，实现正式复审累计仍按派发时的 2/3，这里不宣称通过。引燃档位按第 2/3 轮裁决保持现状，没有改 B。没有运行 Gradle、javac、打包、游戏或独立测试。编译状态：待统一编译。

`basic_staff` 仍是 6 槽和原属性。`test_staff` 仍是 32 槽、备用 1000、效率 15.0（总回魔 16 倍）、冷却 0.50。

命名门禁退出码 1。本轮文件没有新违规。未改的 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to` 仍在清单里。

## 1. 共享命中钟

`HitClock.occupied`（`HitClock.java` 43 行）只查询，不写入。

`TechniqueHitSettle.sweepEarliest` 在 `classifyRespecting` 之前查询（242–244 行）。已经占钟的目标加入拒绝名单并继续扫其余候选，不再停在同一目标上每刻重打。取消不占钟，并改扫剩余候选（248–253 行）。`PENDING` 不占钟，也不当成成功（254 行）。免疫或正伤成立后才 `tryMark`（256–258 行）。

`tryHit` 同样先查这只钟（44–46 行）。没有钟时仍先走原来的 `hitOnce`。有钟时只在 `settle` 返回的裁决 `continues()` 之后占钟（80–86 行）。取消不占钟。

本体和留焰仍共用出生槽上的那一只钟，没有另建第二只。

## 2. 父段闩锁

`BatchCastSession.spawnParent` 在调用 `spawn` 之前，给有子段的父份加一闩并登记寿命回调（186–190 行）。`spawn` 里同步接续已经减过的，回来后不再加（200 行只保留）。提交失败或异常且尚未接续时，`releaseUncontinued`（214 行）减回这一份，寿命回调看到同一键就不再减。已经接续的不回滚。`noteParentFinished` 仍在 `finally`，同步回调时父份还没记完成，不会提前 `finish`。未命中的父份靠寿命回调结束。结束时仍 `budget.settle` 退未提交金额，并在未中止时开始冷却。

## 3. 出生净空

`ListedRelease` 分开登记：热波水平 5、竖向半高 1.5（64 行）；火苗 0.125 / 0.125（69 行）；陨岩 0.5 / 0.5（59 行）。`ReleaseDefinition.birthHorizontalRadius` 和 `birthVerticalHalf` 是默认能力，编译器不按术式编号写热波常数。

`BirthClearance.cover` 按 `spanScale` 覆盖父段和子段，整组取较大值（`BatchReleaseCompiler.java` 372–376 行）。`BatchCastSession.open` 92 行把两个数交给 `SurfaceAnchor.resolve`。竖向再加 0.125（`SurfaceAnchor.java` 50 行）。收费前失败仍整组拒绝。热波攻击用的内外半径和层高没有改。

## 4. 剑槽装配签名

非法杖在 `open` 114 行保存服务端 `ChainSteps.signature`，不读客户端声称的签名。`stillValid` 360–362 行只重读本次槽位的 `PlayerBoundSkills`。签名变化走现有 `cancel`：未出生份和未触发子段不再生成，未提交金额随 `budget.settle` 退回。已出生实体和残骸不在这条取消里删除。别的槽不参与比较。松键逻辑没改。剑槽仍是 4 个，链上限仍是 8，没有改旧 NBT。

## 剩余请求

- B：无。引燃 n 不封到 8。
- C：`BatchFlameTrail.sweep` 仍先 `tryMark` 再分类。请改成裁决 `continues()` 之后再占钟，取消不要占共享钟。A 没有改这个文件。陨岩取消扩圈已经收束，不恢复重复分类。
- D：无新请求。

## 本轮源码

| 文件 | sha256 | mtime |
| --- | --- | --- |
| HitClock.java | 6ffc2d3e673ff8418e46e17e15d6978f01ccd222287a9afe93ccd23ae153ab52 | 2026-10-31 05:38:30 |
| TechniqueHitSettle.java | 023d18fb4adea4148d2cf9d937494e5c9dba3526d06f00ac2776ba27e50e5a46 | 2026-10-31 05:38:45 |
| BatchCastSession.java | 8379240613cdcc49ad015d7e5c3939648368de932f59009fd0f2cb69849c26d9 | 2026-10-31 05:39:02 |
| SurfaceAnchor.java | e933fed57648620a3df9df75c68d53072a359a4022c155e7dcc140b328976b90 | 2026-10-31 05:39:03 |
| ReleaseDefinition.java | 493417911b8c36c51dda620ceb040a3713df86a7c46207acf7e577e34a153d33 | 2026-10-31 05:39:04 |
| ListedRelease.java | 836068c6051ff56f56577fc65757f165e967bf400d4c90e8ecc123580106ae52 | 2026-10-31 05:39:23 |
| BirthClearance.java | fe6c1d00f3d2b7bdb0d78ed297f0eeada0fe9b6aae54c24bac4a7d3a2ec5612d | 2026-10-31 05:39:28 |
| BatchReleasePlan.java | 350ac771678f924ae846fce6f48371ff861986f3183c6050937105fc30bc9f7a | 2026-10-31 05:39:31 |
| BatchReleaseCompiler.java | 31ef03ed27f41ea82b3eed10f84235678d6effb052dc0bd2e06fd5efbecdf30d | 2026-10-31 05:39:36 |

所有权 63 个文件，阶段 `review1-repair`。源码在本报告写下后停写。
