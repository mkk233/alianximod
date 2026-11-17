先读完整任务说明，再按它划定的范围修编译问题。三处调用都把锁定布尔传进了 `Consumer<Vec3>`。先备份 `SkillSegmentRunner.java`，再补一个与无回调重载同序、末尾带上下文的重载。编译诊断里的三处重载错误已经按最小改动补上。全部写入已经停止，文件已释放。没有编译，也没有新增正式复审，不能把这次修复说成编译已通过。

开始 `2026-10-30T03:18:35.549Z`，收束 `2026-10-30T03:22:21.667Z`。记录在 `docs/开发记录/术式链计划缓存-20261030/repair-A-compile-final.md`，证据在同目录 `evidence-A-compile-fix.json`（`released=true`）。

## 故障

`SkillSegmentRunner.commitHostGroup` 里，接受 `CastPlanContext` 的重载在附魔解析之后先放 `Consumer<Vec3> onFirstTrigger`，再放两个布尔。三处调用都把锁定布尔放在了回调那个位置：

1. `SkillChainPlan.java:291`：`plan.entityLock && (mutable || dedicatedAim)`
2. `CastExecutionSession.java:337`：`lockThisCast(...)`
3. `SkillUsePacket.java:850`：同样是 `lockThisCast(...)`

只带两个布尔和 `ReleaseInput` 的旧重载没有上下文，匹配不上末尾同时传入链输入和上下文的调用。

## 修复

只改了 `SkillSegmentRunner.java`。在无回调、带 `ReleaseInput` 的重载之后增加同序重载，位置是 280–291 行，末尾多一个 `CastPlanContext`。它把 `onFirstTrigger` 传成 null，再转到已有的完整实现。锁定、垂落、链输入和上下文的顺序与这三处调用一致，三处调用本身没有改。

旧的无回调重载仍在。不带上下文的那一档仍把上下文传成 null。带回调的重载和 `commitLooseHost` 未改。`SkillChainPlan.java`、`CastExecutionSession.java`、`SkillUsePacket.java` 的哈希与修复前相同。原 `backup-A/` 未覆盖。

修前备份：`backup-A-compile-fix/src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java`，SHA256 `54F1649A6C316378CF74555C5EDB2043B9C8DED4FD1687D3C23759C6856A8DC4`，UTC `2026-10-30T02:48:56.646Z`。

修后 `SkillSegmentRunner.java`：SHA256 `472AA960083F1AC2A09644F58089BEAF64CB523C28075DA7BC16C86D6955FD6D`，UTC `2026-10-30T03:21:34.517Z`。

名称门禁已跑。本次改动的文件没有新的短名。未改动的 `OriginalHumanoid.java` 仍有两处旧短名，没有顺手改。没有运行 `gradlew`、测试、打包或游戏。编译仍交给指定的独立会话。
