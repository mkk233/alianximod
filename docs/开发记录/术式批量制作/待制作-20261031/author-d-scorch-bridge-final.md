先读这份灼地桥接任务的完整范围，再按授权改代码并交最终报告。任务是补灼地的幽冥载荷、根账和真实切片范围。先核对 A 的交接和三方所有权，再改 D 自己的文件。A、B、C 都没有独占灼地宿主和效果类。先把这次要改的绝对路径写进所有权，再补载荷、根账和切片范围。灼地幽冥的载荷、根账和真实切片已经接上，本单写入停止。没有编译，也没有进游戏。

灼地仍用原来的贴地宿主，不新建领域，也不改伤害、几何和接触判定。`ScorchEffect` 把同一次载荷和 `BatchCastSession.currentRootId` 传进 `SurfaceFireHosts.open`，再进新的四参 `ContactEnchantState.open`。旧的两参 `open` 先走一遍，所以 `consider` 只有一次；载荷带幽冥时再 `attach`。根不在绑定表里时不新建施法账。宿主关闭时释放自己持有的那一次。

批量出生槽上的载荷和 `EnchantBonus.resolve` 用的是同一组附魔下标。没有出生槽时，用当前宿主快照的附魔标识去对法杖上的灼地段再解析。对不上就传空载荷。普通链没有批量会话时，根标识是 null。灼地原来的 `fireContext()` 仍只给旧火种记号，不当作幽冥根。

染色和魂火改按真实切片。雾雨、重压仍是一个盒子。切片之间的空隙不再染蓝。`sample(Level, x, y, z)` 的签名没变。14 号报文前缀不动，尾部追加切片分片；`ModNetwork` 没改。超过 3072 片时两边都不把外包盒染蓝，产种仍走原来的接触判定。

## 代码

| sha256 | 时间 | 文件 |
| --- | --- | --- |
| `ac26a4f0578aa3504a8e6af0fc6f8adfd72d1a9cf682db60c5beac9fcde47978` | 05:00:06 | `ContactEnchantState.java` |
| `f8953b798fafa45a20e33c1301064ecea040b92f2b85431911cb4d9ff96bfc86` | 05:00:19 | `SurfaceFireHosts.java` |
| `4e4beaaee40d4bb2e31faf5fc4a69a113b37a7f81ad71f19f6f025c0b4d074c4` | 05:00:30 | `ScorchEffect.java` |
| `14a01bda239c0fc4ed3967f3d66883de0ec6b5c085b319019f9472b80d096b5d` | 04:59:57 | `GloomVolumes.java` |
| `c4cb0878d08a1e60204ea459bf4bd54ebd0b2c18adc311e65e898b19ae47aa55` | 05:00:44 | `NetherDomainSync.java` |
| `bbed914e521b6adc28b98fdd86c6860476c6a41ff4dedb4de6e114b423f0c06e` | 05:00:53 | `NetherDomainSight.java` |
| `2d1611b34aabe1a89f69a28558858582817e8908a5d56f5317053c33f65f5d4e` | 05:01:12 | `NetherDomainCycle.java` |
| `2e82993eae9cea19073f378a89aea00ddd016a83c918668a09323e532ab1cfc5` | 05:02:06 | `NetherDomainWisps.java` |

命名门禁里，这次动过的文件没有新违规。未改的 `OriginalHumanoid.java` 仍有 `i` 和 `to` 两处旧短名。

没有新的注册请求。原版火焰粒子、燎系剑火、炉身、雾雨和重压的水面材质仍不改色。详细签名在 `author-d-interface-final.md`，报文尾部在 `author-d-registration.md`。
