# D 最终报告：幽冥

模型 grok-4.7-high-fast。本单写入已经停止。没有跑 Gradle、javac 编译模组、测试、build、jar、reobf、runClient，没有启动游戏，没有改制作表或资源库 README，没有提交。

设计 2/2 已用完，本单不另开复审。实现正式复审 0/3，这里只做了定向自查。

## 做了什么

幽冥不新建领域。它挂在现有 `DomainInstance`（雾雨、重压，以及任何走 `DomainService` 的领域）和灼地宿主上。范围和寿命沿用宿主。接触伤害为 0 不会阻止登记，但旧的接触伤害扫描仍然要求 `contactDamage > 0`。

满 20 个服务器刻之后，在主世界游戏时间的 20 刻边界上，把当时覆盖同一目标的全部合格领域一次交给 `NetherSeedArbiter.grant`。窗口号是主世界 `gameTime / 20`，写进 B 已有的实体窗口键。同一次调用里择高，同值由 B 用领域标识字典序打破。不按领域分别入账，不每周期新建 `FireCastContext`，不减速，不额外伤害，不放原版魂火或灵魂沙，也不刷新燃烧时长。存活燃烧仍可按旧账本消耗新余额。

客户端蓝火只看同步来的盒子、维度和结束时刻。`FireVisualClient` 与 `FireSeedFlameClient` 只改顶点色。确认版 64×1024、16 帧条带已复制到运行路径，魂火用每帧 64×64 的紫外，不把整条贴到一个面。

根账只在 `RootCastBindings.lookup` 已经有这本账时 `retain` 一次，领域关闭、维度卸载或服务器停止时 `release`。费用结算没有被拿来销毁还活着的领域上下文。监听在 `NetherDomainEvents` 上只注册一次。

领域原本就不进存档。本单没有改这个约定。服务器停止会清租约并放开持有过的根账。

## 代码

| sha256 | mtime | 路径 |
| --- | --- | --- |
| 476df22238c99cbeb148811435fec1e24784fee97f79e35e5e1820ee8819747a | 2026-10-31 04:37:13 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/NetherDomainCycle.java` |
| 4feb45d8859b5409a6b8cfe799c7fe56e26f8a9ec5776e539e1ba5afe8c02296 | 2026-10-31 04:32:12 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/NetherDomainTint.java` |
| 2773a01b38e808649c93da1478f039001f11868a5b7c9ce45045c2aaad920c51 | 2026-10-31 04:32:17 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/NetherDomainSight.java` |
| 5cf1e6183cea2600efe4a05056672483a25354b51e61a13bedce921e6dd7acc9 | 2026-10-31 04:32:19 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/NetherDomainEvents.java` |
| d8b982181eb23f79ca03ca6839640a33be34cf11234b7cd08adacc875a309303 | 2026-10-31 04:33:17 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java` |
| 05c6e9864f7c5ee624b18c3a04e6619fc43ce372b7f6078ee6b2f895026d4823 | 2026-10-31 04:33:17 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java` |
| 8e6ed71b1111fb92ec4e158a5755a9247d35786f4a52bb0d1f326947e97c12ae | 2026-10-31 04:33:18 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/ContactEnchantState.java` |
| 1701771aeab2893c6ed906a6684b9e92d826d682fc94a82e3f5f4989ed2d6a5b | 2026-10-31 04:32:10 | `src/main/java/com/bslx/XueXiMod/network/NetherDomainSync.java` |
| 82613056f2ec20885d4797f9e763aa34d3263f1f87f25c91c869885940a28e46 | 2026-10-31 04:36:22 | `src/main/java/com/bslx/XueXiMod/client/NetherDomainWisps.java` |
| e7520a850db753fa86e6c5927a3bf84b526ac4ee17da7c53743f75640fd71553 | 2026-10-31 04:34:48 | `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` |
| fec34f797d9461276b1832309f2804c7902b5609ceda2d8fad2a715c3550a875 | 2026-10-31 04:33:29 | `src/main/java/com/bslx/XueXiMod/client/FireSeedFlameClient.java` |

运行贴图：

| sha256 | 尺寸 | 路径 |
| --- | --- | --- |
| 1E7CD18441C29D5F018D8B239D0A9A737B784C9F45BAA4202F45AEFF07F3B49F | 64×1024 | `src/main/resources/assets/xueximod/textures/effect/nether_gloom_v1/nether_gloom.png` |

与 `resource-manifest-v1` 的幽冥哈希一致。复制保留了源文件时间 2026-10-31 03:15:05。没有覆盖旧六式贴图。

## 定向自查

- 所有权与 A/B/C 三份清单没有路径重叠。
- 命名门禁扫过整个 `src`。本次新文件和改过的领域、火色调文件没有新违规。未改的 `OriginalHumanoid.java` 仍有两处旧短名（`i`、`to`），按任务没有顺手改。
- 窗口：年龄未满 20 服务器刻的领域不进 offers；同一目标一次 `grant`；窗口号用主世界游戏时间，避免服务器 tick 重启后和 B 写在实体上的旧窗口号相撞。
- 接触伤害为 0 时，`consider` 仍可登记，`ContactEnchantState` 不把这种宿主放进伤害列表。
- 没有调用 `setSecondsOnFire`、药水减速或原版火焰粒子来假装蓝火。
- 根账找不到就不 `retain`。

没有编译，所以这些是读代码核对，不是编译器或游戏结果。

## 未完成

1. `NetherDomainSync` 还没登记进 `ModNetwork`。登记前客户端不会变蓝，魂火条带也不会出现。服务端产种不依赖这包。
2. 只装幽冥、没有接触伤害时，`ContactCast.refusal` 会拒绝释放，`ContactCast.snapshot` 会给出 null。旧 `tryWave` 因此挂不上纯幽冥。叠了伤害附魔时，快照标识可以挂上，但根标识仍然要 A 传入才会 `retain`。
3. `BatchCastSession` 没有公开当前根标识。D 没有伪造一本新账。
4. 武器印记上的幽冥会由 A 的 `WeaponEnchant` `adopt`，没有领域租约时 D 不 `release`。
5. 灼地客户端色调用的是几何 `bounds()`，也就是切片的外包盒。服务端产种用 `touches`，按切片相交。包在外包盒里、却不在任何切片上的点会变色，但不会产种。
6. 原版 `ParticleTypes.FLAME` 火花、燎系剑火、炉身、雾雨和重压的水面材质没有改色。新的火苗、热波、留焰、陨岩渲染要自己调用 `NetherDomainTint.sample`。
7. 领域不存档。重启后租约消失，已写在实体上的窗口号还在，这是 B 的持久键。

## 写入停止

本单 Java、贴图和这五份报告之后不再写。
