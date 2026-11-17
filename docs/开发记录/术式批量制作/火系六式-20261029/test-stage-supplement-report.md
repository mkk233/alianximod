# 火系六式测试补充执行

本文件补第 1 轮测试核对要的三组入口。不是实现复审，也不下正式结果结论。测试正式计数仍是核对者正在进行的 1/2，本单不另计。设计 2/2、实现 2/2 不变。没有迁表，没有改 `记录.md`、生产源码、语言、资源或 Gradle 配置。没有新增火种来源。没有重跑 Gradle，没有启动游戏或服务端。

生产哈希对照 `test-stage-pre-sources.txt`：2026-10-29T11:03:45 与结束后都是 654 条一致、差异 0。沿用 10:49 那次退出码 0 的 main class。`FireHurtMixinPlugin.class` 时间仍是 09:09:18。

新增测试 `src/test/java/com/bslx/XueXiMod/network/SixFireChainSupplementCheck.java`。SHA256 `2E0563261992F2B92A48E9CCFEC9C50AC0BFA7E843E3C308205A8F33CDF1191D`，7819 字节，mtime 2026-10-29T11:05:58.8334100+08:00。名字门禁仍只有既有 `OriginalHumanoid.java` 的 `i` 和 `to`，本次文件没有新违规。

javac 使用 JDK 17、`--release 17`，classpath 是当前 `build/classes/java/main` 加上已有记录 `C:\Users\baishang\AppData\Local\CursorAgentCodex\status-targeting-prison-implementation-20261028\qa2\compile-classpath.txt`。参数文件 `test-stage-supplement-javac.arg`。11:05:36 与 11:06:07 两次 javac 退出码都是 0。类在 `build/test-stage-supplement-classes`。日志在 `test-stage-supplement-run-mixed.log`、`test-stage-supplement-run-batch.log`、`test-stage-supplement-run-commit.log`。

## 1. SkillSegmentMath.mixedChainRefusal

已执行。入口是包内真实方法 `SkillSegmentMath.mixedChainRefusal(List<Technique>)`。先 `ReleaseDefinitions.registerAll()`，再传入只带编号和基础伤害的 `Technique`。命令组 `mixed`，11:05:38.7232449 至 11:05:38.9885076，退出码 0。没有失败行。

| 预期 | 实际 |
| --- | --- |
| 升温、燃烧、冷却各自单独返回空 | 三个都是空，单式允许 |
| 三者分别再加燎原 | 都返回「自身状态不能和其他术式装配」 |
| 升温加一分为三、燃烧加双重释放、冷却加一分为三 | 同一句拒绝 |
| 一分为三加燎原 | 返回空。这组没有自身状态，函数不拒绝 |

这个方法签名不要 `ServerPlayer`。类加载用了上面的 Forge 类路径，调用时没有构造玩家。

## 2. ReleaseBatch 复制份与 fireContext

`ReleaseBatch.open(ServerPlayer, Skill)` 用空玩家、空技可以返回对象，`current()` 就是它。接着真实调用 `fireContext()`。实际异常：`NullPointerException`，信息是 `this.player` 为空，不能调用 `getUUID()`。11:06:10 的 batch 组退出码 0，这条例外被测到并打印，不是断言被改掉。

空玩家只说明这条路径要非空玩家。进一步加载玩家类：

- `Class.forName("net.minecraft.server.level.ServerPlayer")` 抛 `ExceptionInInitializerError`。原因是 `IllegalArgumentException: Not bootstrapped (called from registry ResourceKey[minecraft:root / minecraft:root])`，位置 `Bootstrap.createBootstrapException`，栈顶 `ResourceKey.createRegistryKey`。
- `net.minecraftforge.common.util.FakePlayer` 接着 `NoClassDefFoundError: Could not initialize class ServerPlayer`。
- `src/test` 里没有 `ServerPlayer`、`FakePlayer` 或现成玩家夹具。`Bootstrap` 属于启动游戏注册表，本单不能启动。

因此同目标只扣一次、新火种不被旧份再扣、不同目标隔离，都没有在 `ReleaseBatch` 上执行。按份释放在 `SkillChainPlan.firePaidPlan` 和 `fireDualSequence`，签名都要 `ServerPlayer`，并且读取 `ReleaseBatch.current()`。这两条没有调用。

同一进程里另外调用了真实计划函数，不能把它当成批次执行：`SkillChainPlan.collectModifiers` 对「一分为三 + 燎原」得到 `copies=3`、`attackSegment=1`；对「双重释放 + 燎原 + 焚天」得到 `dualRuleSegment=0`、`copies=1`，`releasedSegments` 大小是 2。这只是份数和拉段计划。没有共享 `fireContext`，也没有消费火种。

## 3. SelfStatusCommit.attempt

已调用真实 `SelfStatusCommit.attempt`。空玩家加升温定义得到 `REJECTED`。空玩家加燎原定义也得到 `REJECTED`。组 `commit`，11:05:40.9572348 至 11:05:42.5582616，退出码 0。输出写明未进入预检成功、扣费、安装和退款。

正常提交、魔力不足、安装失败后退款且状态不变，都没有执行。阻塞不是只传空就结束：

- `ServerPlayer` 类初始化停在未 bootstrap，现有夹具造不出实例。
- 过了空玩家之后，`FurnaceService.preview` 要 `player.level()`，`FurnaceThreadGate.onServerThread` 要 `level().getServer().isSameThread()`。
- `MagicPool.canAfford` 要 `PlayerShuData.get` 和主手物品。扣款和退款走 `ManaPaymentReceipt.charge/refund`。
- 安装走 `player.forceAddEffect`。没有离线夹具提供这些对象。
- 没有用 `SelfStatusOrder.decide` 或 `FurnaceInstall.plan` 代替这次事务。

## 正式实现复核记录

只核对已完成文档，没有重审代码，也没有补一份通过报告。

六式本批没有适用的正式实现复核通过记录。`记录.md` 写明底层实现正式复审额度 2/2 只是承接，本次不自动启动实现复审，执行报告不能冒充通过。实现交回写的是「未追加正式实现复审，不宣称通过」。`test-stage-prep-report.md` 写明六式本批没有一份完成的正式实现复审记录。

底座 `review-round2.md` 的结论是不通过。R2、R3 必须返修，实现额度当时已经用完。后来的 `repair-final-final.md` 有作者返修反馈，`compile-postfix-final.md` 有编译和检查，底座 `记录.md` 与 `底层规格.md` 仍写「返修结果未追加复审」。这个状态没有被后续完成记录改成通过。底座复审范围也没有把升温、燃烧、冷却、燎原、燎里、焚天标成已复核通过。

## 火种

三消费者的原始需求是只消费、不产火种，获取途径另定。正常玩法仍没有授予入口。这不是本单离线功能失败，也不牵连本来不产火种的升温、燃烧、冷却。

## 停写

测试源码和这次进程到此停写。生产哈希差异仍是 0。等待本轮结果核对，不自行开复审，不写表。
