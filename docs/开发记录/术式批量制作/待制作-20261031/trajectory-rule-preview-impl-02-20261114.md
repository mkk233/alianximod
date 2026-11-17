# 弹道规则单独预览编译返修回执

- batchId: `待制作-20261031`（未重置额度）
- taskId: `trajectory_rule_standalone_preview`
- attemptId: `20261114-impl-trajectory-preview-02`
- stage: `implementation`（正常编译返修，未追加正式复审）
- 请求模型: `cursor/grok-4.7@500k high`
- 实际服务模型: 未知。主会话查询 magpie usage 时命令不存在，本回执不把请求名写成已验证的服务模型。
- 上一份回执 `trajectory-rule-preview-impl-20261114.md` 自称 Grok 4.6，未改、不作为实际模型证据。
- 直接父代理: 本次调用的主会话（未提供 UUID，不编造）
- 下属: 未派只读或其他子代理，无下属需要收束。
- 编译者 `01a1d99a-76d6-7f72-8987-395eefccf418` 已由主会话收束关闭；本单仍不编译、不测试、不启动游戏。

## 修复

`TrajectoryRulePreview` 在 `release` 包，不能访问 `network` 包内的 `SkillSegmentMath`。未把该类改为 public。

- `TrajectoryRulePreview.java:59` `play` 改为接收调用方已经算好的报价，仍用 `MagicPool.trySpend`。
- `SkillUsePacket.java:344` 用包内 `quotedMana` 计算费用。
- `SkillUsePacket.java:349` 用包内 `configuredBaseCooldown` 和 `scaleCooldown` 计算冷却。
- `TrajectoryRulePreview.java:48` 与 `:203` 仍在预览类内拒绝 `rule_interleave`、`rule_damage_tally`，口径与包内 `isRemovedRule` 相同，不另做弹道编号白名单。
- `ChainRole.java:54` `modifiesFollowingTrajectory()` 未改。六项分类、单独完整组、纯视觉占位、组合与例外保持。

## 源码指纹

不含本回执：

- `ChainRole.java` SHA256 `15186d387014ca5aba074654e29e80d0020a1dd5b318a12f4d55c53f52c04cc6`（与上一回执相同）
- `TrajectoryRulePreview.java` SHA256 `febeec8e43277b27b7ba4d06b408e5103b54bc7a56647042e96e188d7aeda237`
- `SkillUsePacket.java` SHA256 `5f0897b46364a2f37efbae4e8a52822787a9cf6de8757c8ea0eb62f9357bd2c8`

待原编译者重跑 `.\gradlew.bat compileJava --offline --no-daemon`。
