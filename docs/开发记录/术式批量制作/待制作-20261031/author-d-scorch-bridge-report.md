# 作者 D 灼地桥报告

阶段：继续实现，不是正式复审。设计 2/2，实现 0/3。没有改 A、B、C 的文件，没有读 C 的新渲染类。`basic_staff` 仍是 6 槽。本报告不是复审通过。

## 已接通

1. `ContactEnchantState` 增加 `open(carrier, snapshot, load, rootCastId)`。它先调用旧的两参 `open`，所以 `consider` 只有一次；`load.gloom()` 为真时再 `attach`。接触伤害小于等于 0 时仍不进入旧的伤害扫描。
2. `ScorchEffect` 把同一次载荷和 `BatchCastSession.currentRootId` 传给 `SurfaceFireHosts.open`。批量出生槽上的 `resourceLoad` 优先。没有出生槽时，用当前宿主快照的附魔标识对上法杖上的灼地段，再 `EnchantBonus.resolve`。对不上就传空载荷。根不在绑定表里时不新建 `FireCastContext`。
3. 灼地宿主关闭时 `NetherDomainCycle.drop`，释放自己持有的那一次根。切片在 `settle` 之后经 `sync` 写入租约。
4. 幽冥查询和客户端魂火改按真实切片。雾雨、重压仍是一个盒子。灼地切片之间的空隙不再染蓝。`sample(Level, x, y, z)` 签名没变。
5. 14 号报文前缀不动，尾部追加切片分片。`ModNetwork` 没改。超过 3072 片时两边都不同步成外包盒，这一发不染蓝。产种仍按原来的 `touches`。

## 根和范围

普通链没有批量会话时，`currentRootId` 返回 null，幽冥租约不 `retain`。批量会话里的根，找到绑定后 `retain` 一次，宿主结束、维度卸载或服务器停止时 `release` 一次。费用结算不在这里删领域。

灼地原来的 `batch.fireContext()` 仍只给旧火种释放记号，不当作幽冥根。

服务端 `contains` 和客户端 `probe` 都用低边界包含、高边界排除的切片。外包盒不参与染色。魂火条带画在每片里面，不铺满外包盒。

## 本轮文件

| sha256 | mtime | 文件 |
| --- | --- | --- |
| ac26a4f0578aa3504a8e6af0fc6f8adfd72d1a9cf682db60c5beac9fcde47978 | 2026-10-31 05:00:06 | ContactEnchantState.java |
| f8953b798fafa45a20e33c1301064ecea040b92f2b85431911cb4d9ff96bfc86 | 2026-10-31 05:00:19 | SurfaceFireHosts.java |
| 4e4beaaee40d4bb2e31faf5fc4a69a113b37a7f81ad71f19f6f025c0b4d074c4 | 2026-10-31 05:00:30 | ScorchEffect.java |
| 14a01bda239c0fc4ed3967f3d66883de0ec6b5c085b319019f9472b80d096b5d | 2026-10-31 04:59:57 | GloomVolumes.java |
| c4cb0878d08a1e60204ea459bf4bd54ebd0b2c18adc311e65e898b19ae47aa55 | 2026-10-31 05:00:44 | NetherDomainSync.java |
| bbed914e521b6adc28b98fdd86c6860476c6a41ff4dedb4de6e114b423f0c06e | 2026-10-31 05:00:53 | NetherDomainSight.java |
| 2d1611b34aabe1a89f69a28558858582817e8908a5d56f5317053c33f65f5d4e | 2026-10-31 05:01:12 | NetherDomainCycle.java |
| 2e82993eae9cea19073f378a89aea00ddd016a83c918668a09323e532ab1cfc5 | 2026-10-31 05:02:06 | NetherDomainWisps.java |

贴图未改，仍是 `nether_gloom.png`，sha256 `1E7CD18441C29D5F018D8B239D0A9A737B784C9F45BAA4202F45AEFF07F3B49F`。

## 检查

`tools/check-name-gate.ps1` 退出码 1。本轮文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。

## 仍未覆盖

武器上的原版火焰粒子、燎系剑火、炉身、雾雨和重压的水面材质没有改色。法杖技能对不上当前快照、又没有批量出生槽时，灼地不会编一份假幽冥载荷。切片超过 3072 时这一发不染蓝，产种仍走旧判定。

没有剩余的 A 注册请求。

## 停写

本范围写入到此停止。不再改 A、B、C、表、资源索引或记录。
