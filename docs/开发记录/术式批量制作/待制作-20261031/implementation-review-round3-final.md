我先完整读取第3轮复审任务说明，确认授权范围后再对最终代码做只读核对。第3轮只复核首轮四项返修。我先读裁决和 A/C 最终报告，再对照最终代码核对这些路径。报告只作索引。接下来对照最终代码核四项：共享命中钟、父段闩锁、地表净空和剑槽签名。实现第 3/3 轮复核结论：**四项都通过。没有新的直接相关阻断，没有仍需返修的清单。** 引燃封 8 维持第 2 轮裁决，不再当作缺陷。本轮没有编译，也没有进游戏。源码没有写入。

开始与结束时对停写文件做了两次 SHA-256，结果相同，并与 A、C 停写哈希一致：`HitClock` `6FFC2D3E…AB52`（05:38:30）、`TechniqueHitSettle` `023D18FB…5A46`（05:38:45）、`BatchCastSession` `83792406…26D9`（05:39:02）、`SurfaceAnchor` `E933FED5…6B90`（05:39:03）、`ReleaseDefinition` `49341791…3D33`（05:39:04）、`ListedRelease` `836068C6…AE52`（05:39:23）、`BirthClearance` `FE6C1D00…612D`（05:39:28）、`BatchReleasePlan` `350AC771…9F7A`（05:39:31）、`BatchReleaseCompiler` `31EF03ED…F30D`（05:39:36）、`BatchFlameTrail` `2A5E61C0…FFB9`（05:43:56）。`FireOrbEffect` 仍是首轮 `6C9EC0BD…B8C4`（05:01:43），`ModItems` 仍是 `50FF76E5…B336`。

## 1. 共享命中钟：通过

本体扫掠在伤害前先问 `occupied`。已占用的目标只放进 `rejectedTargets` 并继续扫下一个，不会每刻卡在同一个目标上。

```241:258:src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java
        if (hitEntity.hitClock != null
                && hitEntity.hitClock.occupied(target.getUUID(), level.getGameTime(), hitEntity.hitInterval)) {
            hitEntity.rejectedTargets.add(target.getUUID());
            return sweepEarliest(hitEntity, level);
        }
        // ...
            if (verdict == HitVerdict.INVALID_OR_CANCELLED) {
                hitEntity.rejectedTargets.add(target.getUUID());
                return sweepEarliest(hitEntity, level);
            }
            return false;
        }
        if (hitEntity.hitClock != null) {
            hitEntity.hitClock.tryMark(...)
```

取消不调用 `tryMark`，共享机会留给留焰或其他候选。`PENDING` 直接返回，不占钟、不当成功。`classifyRespecting` 本身不返回 `PENDING`；免疫走 `ESTABLISHED_NO_DAMAGE`，`continues()` 为真，会占钟并允许命中接续。

留焰用同一只钟：先 `occupied(..., 0)`，再分类，只有 `continues()` 才 `tryMark`。取消只 `continue`，不占钟。火球 `attachTrail` 把 `record.hitClock` 设成出生槽那只钟，`BirthLayout` 把同一只写进实体。火球、火苗、陨岩都没有改 `hitInterval`，默认 0，本体和留焰都走 `seenTargets`，不是两套钟。热光束的间隔 10 不在留焰宿主里。

## 2. 同步父闩锁：通过

有子段时，`openLatches` 先加一，寿命回调先挂上，然后才 `install` 和 `spawn`。热波在 `spawn` 里同步回调时，闩已经在。

成功且 `committed` 时 `retainLatch` 为真，`finally` 不再减。拒绝、未提交或异常走 `releaseUncontinued`：`continued` 只加一次，计数用 `max(0, -1)`，不会后加，也不会减成负数。同步回调里已经 `continued` 的，`finally` 和到期回调都不会再减。`noteParentFinished` 在 `spawn` 返回后的 `finally` 里，子段当场 `maybeFinish` 不能提前结束会话。`cancel` 把闩清零再 `finish`。`finish` 里 `budget.settle` 退未提交份额，未中止才写冷却，并释放载体锁和根租约。多个父段用 `分支:副本` 做键。延迟子段另有根租约 `retain/release`。没有用固定延时去掩盖闩锁。

## 3. 地表净空：通过

登记是最终几何，不是厚度的一半硬套：陨岩水平/竖向半高 `0.5/0.5`，热波 `5.0/1.5`，火苗 `0.125/0.125`。攻击用的 `thickness` 仍是陨岩 `1.0`、热波 `5.0`、火苗 `0.25`，形状没有被净空字段改掉。

`BirthClearance.cover` 按 `spanScale`（至少 1）取父段和子段的较大值。`BatchCastSession.open` 在 `GroupBudget.reserve` 之前用这两个值调用 `SurfaceAnchor.resolve`。竖向再加 `0.125`，水平不加垫。不够则整组拒绝，不扣费、不进冷却。地表落点被收成同一个冻结点，多份复制不另铺开出生位置。方块扫描是 `ceil` 后的方形，比圆更严，不是漏检。

## 4. 非法杖剑槽签名：通过

出手时非法杖保存的是当时 `ChainSteps.signature(skill)`，只含该槽术式 id。之后 `stillValid` 只重读这一槽的 `PlayerBoundSkills`。签名变了，未到时的父段在 `spawnParent` 入口返回，未触发子段在 `spawnChild` 入口返回。已出生实体和残骸都没有被清掉。无关槽不参与比较。`HoldRepeat` 不调用会话取消，松键不会误取消。

未提交金额在 `finish` → `GroupBudget.settle` → `ManaPaymentReceipt.refundShare` 退回原收据：玩家池回原玩家，法杖池按收据里的 `staffIdentity` 找，不看当前主手。`basic_staff` 仍是 `StaffData(6, 0.20, 0.10, 100)`，`test_staff` 仍是 `StaffData(32, 15.0, 0.50, 1000)`，`Skill.CHAIN_LIMIT` 仍是 8。

签名失效没有走 `cancel()`，所以 `aborted` 仍是 false。一组都还没出生时，结束仍会写冷却，同时全额退款。死亡取消会跳过冷却。这不否定「未出生份被取消、原收据被退回」。

## 对其余条目

第 2 轮已定：引燃 `n = floor(余额/10)`，上限是 `TIER_CAP` 10；余额 100 为 140 tick。`PLAN_CAP` 8 只是并存计划上限。`KindlePlan` 仍按 10 封顶，本轮不改判，也不重审 B。

这四项原先挡住的路径都已解开：留焰、火苗、陨岩、火球共用钟；热波作命中接续父段时的会话收束；地表定位对热波/火苗/陨岩的真实体积；非法杖剑槽换装配。13 项加测试法杖，不再因这四条代码缺陷留在实现返修里。

编译没有跑。游戏没有跑。没有游戏内证据的部分不能写成已验证。本轮是实现第 3/3 轮，额度已用完，不建议再开第四轮复审。本次没有修改源码、制作表或记录。
