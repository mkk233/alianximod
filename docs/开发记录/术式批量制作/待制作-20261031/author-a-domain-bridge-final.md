先读领域桥接任务的授权范围，再按它接线并交最终报告。幽冥加旧领域会被报价标记整段拒绝。接下来只改 A 侧入口：网络号、接触门、真实根标识，以及没有领域租约时的武器账本。幽冥领域的公共入口已经接到 A 侧。`basic_staff` 仍是 6 槽，`test_staff` 仍是 32 槽。这不是正式复审，状态为待统一编译。本范围已停写。

## 已接通

1. `ModNetwork.register` 在燎里环 13 号之后登记 `NetherDomainSync.register(CHANNEL, 14)`。14 号此前空闲，仍走原来的通道。
2. `ContactCast.refusal` 和 `snapshot`：宿主是领域或灼地，且这段载荷 `gloom()` 为真时，零接触伤不再拒绝，快照也不再收成 null。快照里没有合法附魔标识时仍返回 null。屏障、护体和其他旧拒绝保持。
3. `SkillUsePacket`、`CastExecutionSession`、`SkillChainPlan` 经 `DomainWave` 传递真实载荷。有幽冥才走 `tryWaveLoaded` / `tryWaveReservedLoaded`，根标识用 `BatchCastSession.currentRootId`。没有未结束的批量会话时根是 null，不调用 `ReleaseBatch.fireContext()`，也不另造一本随机账。没有幽冥的开域仍走原来的方法。
4. `currentRootId` 只返回这名玩家唯一未结束的批量根。同时有多本不同的根时返回 null。延迟开域会先 `retain` 这一本，开域返回后再放开这次临时持有。领域自己持有的那一次，结束时由 D 释放。
5. 链上只有报价标记、宿主又只是旧领域或灼地时，`BatchReleaseCompiler` 返回 null，领域仍按原费用开。接续、地表、喷涌或抛物线仍进新编译；领域不支持接续时在编译期拒绝。
6. 普通武器只带幽冥时不再 `adopt`。留焰或崩地同时存在时仍 `adopt`，成功后不删那本账。提交失败时只释放这次新持有的账。

## 请 D 补的签名

灼地不走 `DomainService`。现有路径是 `ScorchEffect` 到 `SurfaceFireHosts.open`，再进 `ContactEnchantState.open`，最后只调用 `consider`。这三处不在本单写权限里，A 拿不到宿主，不能代调 `attach`。

请 D 增加：

```java
void ContactEnchantState.open(ContactCarrier carrier, EnchantmentSnapshot snapshot, ResourceEnchantLoad load, UUID rootCastId)
```

先按现在的方式 `consider`，再在 `load.gloom()` 时调用已有的 `attach(carrier, load, rootCastId)`。根为空或绑定表里没有时，不要新建 `FireCastContext`。`SurfaceFireHosts.open` 需要把同一次载荷和根传进去。C 的载体注册本段没有处理。

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

归属现为 60 个路径。`author-a-ownership.json` 的 sha256 是 `428aadee44f340760084d89ce397fd8579381affb0f8e48ec2eb9e3a49c5ecf2`，mtime `2026-10-31 04:48:27`。

冻结后的本段文档：

| sha256 | mtime | 文件 |
| --- | --- | --- |
| ef8d4c7e5e2c5879a570fd0624b2096c0f0b6eab9fd13239570de8409d8cc8a3 | 2026-10-31 04:48:50 | author-a-interface-v3.md |
| bd73f60f6c5355466909cc7783b88daa18e2e11232b5b25801578254b08018ec | 2026-10-31 04:48:58 | author-a-domain-bridge-report.md |
| 26a1c37b614748ed69aec039c9adb81f0d8c9c032b3519b1966f3d61b69421a6 | 2026-10-31 04:49:00 | author-a-domain-bridge-report.json |

## 检查

`tools/check-name-gate.ps1` 退出码 1。本轮文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。

## 停写

本范围写入到此停止。不再改 B、C、D、表、资源索引或记录。
