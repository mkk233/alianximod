# 作者 A 幽冥领域桥报告

阶段：继续实现，不是正式复审。设计 2/2，实现 0/3。没有改 B、C、D 的源码。`basic_staff` 仍是 6 槽，`test_staff` 仍是 32 槽。本报告不是复审通过。

## 已接通

1. `ModNetwork.register` 在燎里环 13 号之后登记 `NetherDomainSync.register(CHANNEL, 14)`。14 号之前没有占用。没有第二条通道。
2. `ContactCast.refusal` 和 `snapshot`：宿主是领域或灼地，且这段 `EnchantBonus.resourceLoad.gloom()` 为真时，零接触伤不再拒绝，快照也不再收成 null。快照没有合法附魔标识时仍返回 null。屏障、护体和其他旧拒绝保持。
3. `SkillUsePacket`、`CastExecutionSession`、`SkillChainPlan` 经 `DomainWave` 传真实载荷。有幽冥才走 `tryWaveLoaded` / `tryWaveReservedLoaded`，根用 `BatchCastSession.currentRootId`。没有开放批量会话时根是 null，不新建随机账。没有幽冥的开域调用保持原方法。
4. 延迟领域会临时 `retain` 同一根，开域返回后放开这次临时持有。领域活着时的那一次由 D 在结束时 `release`。
5. 只有报价标记、宿主又只是旧领域或灼地时，`BatchReleaseCompiler` 返回 null，不把整段拒成缺少宿主。接续、地表、喷涌或抛物线仍进新编译；领域不支持接续时编译期拒绝。
6. 普通武器只带幽冥时不再 `adopt`。留焰或崩地同时存在时仍 `adopt`，成功后不删那本账。

## 尚未接通

灼地根标识。`consider` 不接收根，A 不能改 `SurfaceFireHosts`、`ScorchEffect` 或 `ContactEnchantState`。请求见 `author-a-interface-v3.md`。C 的载体注册本段没有处理。

## 本轮文件

| sha256 | mtime | 文件 |
| --- | --- | --- |
| f2a9f921e3ab3fb6c5db6931e15e06c866d4568ad5d073d4fe749c20fe77db47 | 2026-10-31 04:46:39 | ModNetwork.java |
| 428ea7d0d57fd7c03b514e0b40868d5df867ee759e11cb431ae2380118cc1ea1 | 2026-10-31 04:46:44 | ContactCast.java |
| af01b89d2022160861ade04aaa4b7e4092892346cf2bf796083607a3bf184521 | 2026-10-31 04:47:28 | DomainWave.java |
| a4c20c051a1ae9b1de2730bd1199bba26a2dea33137f7512f6f3ee9894795299 | 2026-10-31 04:46:47 | BatchReleaseCompiler.java |
| 2c33860d9a20d1980e48b026da842388b613980327c81264dc374f3e2dabf8b7 | 2026-10-31 04:46:48 | BatchCastSession.java |
| b71e0124a27b2ee7dd40c06e441d9bd518078f0309a59a8ea9fcbb97639b2247 | 2026-10-31 04:47:29 | SkillUsePacket.java |
| 3582d8e468ee4be11fcd0ac833a764d7ef7a0fe3dce335e48e0716ed33fa1107 | 2026-10-31 04:48:04 | CastExecutionSession.java |
| 55bc269599e901399041c790276163e1f7f4ed9108444bb7a2ff9f3ebfb62a18 | 2026-10-31 04:47:33 | SkillChainPlan.java |
| 00d3fe50ac845c2927f6edad0cfefc11e06d68d2d0b0a8bf75d5e0c82b1b5ae5 | 2026-10-31 04:46:49 | WeaponEnchant.java |

归属 60 个路径。`author-a-ownership.json` 的 sha256 是 `428aadee44f340760084d89ce397fd8579381affb0f8e48ec2eb9e3a49c5ecf2`，mtime `2026-10-31 04:48:27`。

## 检查

`tools/check-name-gate.ps1` 退出码 1。本轮文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。状态是待统一编译。

## 停写

本范围写入到此停止。不再改 B、C、D、表、资源索引或记录。
