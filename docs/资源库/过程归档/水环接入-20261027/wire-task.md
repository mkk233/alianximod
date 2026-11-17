你是Cursor Grok4.7HighFast网络/生命周期writer。用户已授权Cursor改代码编译、主代理核对、不打包且用户游戏实测。Cursor DeepSeek实际调用被账户额度拒绝，采用用户此前已授权的Grok4.7HighFast替代；DeepSeek CLI已完成读报告，唯一GPT-6-astra已裁决下面方案。无需再咨询脑子或另派代理。另一位Cursor在写renderer/资源，你们文件严格互斥。
工作目录D:/alianximod。只补读本任务四个源文件及必要直接调用，不重新通读整仓、报告、Gradle规则或另选架构。请尽快开始实际实现。
允许写：
src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java
src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java（只构包与删除通知，不改玩法状态机）
src/main/java/com/bslx/XueXiMod/network/ModNetwork.java
新增一个独立客户端世界卸载清理类（如client/GuardClientLifecycle.java），不要碰GuardRingRenderer/其他渲染文件/ModEvents。
必要定向同步测试与docs/资源库/过程归档/水环接入-20261027/wire-report.md。
禁止改其他文件、禁止启动Gradle或游戏、禁止打包/commit/reset；编译等两位合并后由主控指派单一writer执行。before/中有这几个原始文件备份用于本次diff。git仅用-c safe.directory=D:/alianximod。
已确认：water_ring现有GuardStates生命周期/减伤/魔力/偏转/接触附魔均保持不变。shu/technique/release/momentum/ReleaseBaseEvents.java:204-210已有StartTracking→GuardSyncPacket.sendSnapshot；死亡登出换维已有endCast→closeOwned。不要再注册这几个入口。
共同接口（必须实现，渲染writer将调用）：
GuardSyncPacket.ClientRings.View.public double animationAge(long clientGameTick,float partialTick)
返回非负tick年龄；刷新同一次激活不断相。现有from/toward/poseStartMillis/poseTicks等姿态接口保持原状，本轮不要跨模块重构姿态时钟。
实施步骤：
1. Packet只加animationAgeTicks；服务端每次构FULL/POSE/REMOVE时以server当前tick-animationStart计算，保留原animationStart数值/含义用于激活身份。encode/decode对称；ModNetwork协议1→2，双端继续严格匹配。
2. 客户端收到年龄+本地游戏tick增量+partial推进，不能用serverTick直接减gameTime或墙钟起播。同一animationStart的重复FULL/POSE、刷新revision不归零、不倒退；新观察者按收到年龄起播。保留并完善revision/sequence门控，旧包不能覆盖新状态。新的animationStart身份不因旧高revision被永久拒绝；旧animationStart的FULL/POSE不能重建/摆动更新的环。
3. FULL补快照：from=服务端当前插值法线，toward=当前目标，poseTicks=剩余过渡tick，已完成则from/toward相同。不新增wire姿态字段，不改变服务端实际姿态/过渡参数。
4. 原关闭路径复用；客户端LevelEvent.Unload且isClientSide时clear旧world的ClientRings，原登出清理保留。可加独立client订阅类，勿改renderer文件。
5. broadcastRemove除当前owner维度观察者，还补发到BodyGuard保存的旧serverLevel观察者。REMOVE必须带真实animationStart，仅删相同激活身份且revision不落后的view。避免旧REMOVE删除后来的新环。不改实际存活规则和原持续200tick/维护费等。
6. 补定向测试（纯计算/包编解码可测试）：refresh不断相、重复/乱序FULL/POSE、新身份/旧高revision、晚加入年龄、旧REMOVE不删新环、FULL当前姿态和剩余tick、世界清理。不要伪称做过客户端运行；此轮不执行Gradle，主控会合并后统一编译测试。
完成后立即写wire-report.md（路径/做了什么/已静态核对项/待编译）并结束本轮，主控立刻派Grok独立审。不要等渲染writer，不越界改其文件。有契约问题回报，不擅自扩展API。

