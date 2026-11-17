我先读这份任务说明，按里面写明的授权范围执行。任务范围已明确：只做幽冥领域，先核对所有权与已收束接口，再动手实现。接口和需求已经对齐。接下来核对领域宿主、幽冥仲裁 API，以及现有火色调入口，确认所有权没有重叠后再写代码。所有权已核对：领域宿主和现有火色调入口与 A/B/C 的文件不重叠。开始按已收束的幽冥合同实现周期、同步和色调查询。幽冥已经挂到现有领域上，本单写入停止。没有编译，也没有进游戏。

幽冥不新建领域。它挂在 `DomainInstance`（雾雨、重压，以及所有走领域服务的领域）和灼地宿主上，范围和寿命沿用宿主。接触伤害为 0 也会登记，但旧的接触伤害扫描仍然只处理伤害大于 0 的快照。

满 20 个服务器刻之后，在主世界游戏时间的 20 刻边界上，把当时覆盖同一目标的全部合格领域一次交给 `NetherSeedArbiter.grant`。窗口号是主世界 `gameTime / 20`。择高和同值用领域标识字典序都在已有仲裁器里完成。不按领域分别入账，不每周期新建施法，不减速，不额外伤害，不放原版魂火或灵魂沙，也不刷新燃烧时长。

客户端只按同步来的盒子、维度和结束时刻变蓝。`FireVisualClient` 和 `FireSeedFlameClient` 只改顶点色。确认版 64×1024、16 帧条带已放到 `textures/effect/nether_gloom_v1/nether_gloom.png`，哈希 `1E7CD18441C29D5F018D8B239D0A9A737B784C9F45BAA4202F45AEFF07F3B49F`，魂火按每帧 64×64 取紫外。

根账只有在 `RootCastBindings` 里已经存在时才 `retain` 一次，领域关闭、维度卸载或服务器停止时 `release`。领域本来就不存档，这一点没有改。

## 色调接口

```java
NetherDomainTint.Sample NetherDomainTint.sample(Level level, double worldX, double worldY, double worldZ)
```

不在幽冥里是 `255,255,255`，`tintKey` 为空。在幽冥里是 `90,160,255`，`tintKey` 为 `"blue"`。火苗、热波、留焰、陨岩的新渲染类自己调用即可。本单没有读那些类。

## 代码

| sha256 | 时间 | 路径 |
| --- | --- | --- |
| `476df22238c99cbeb148811435fec1e24784fee97f79e35e5e1820ee8819747a` | 04:37:13 | `NetherDomainCycle.java` |
| `4feb45d8859b5409a6b8cfe799c7fe56e26f8a9ec5776e539e1ba5afe8c02296` | 04:32:12 | `NetherDomainTint.java` |
| `2773a01b38e808649c93da1478f039001f11868a5b7c9ce45045c2aaad920c51` | 04:32:17 | `NetherDomainSight.java` |
| `5cf1e6183cea2600efe4a05056672483a25354b51e61a13bedce921e6dd7acc9` | 04:32:19 | `NetherDomainEvents.java` |
| `d8b982181eb23f79ca03ca6839640a33be34cf11234b7cd08adacc875a309303` | 04:33:17 | `DomainService.java` |
| `05c6e9864f7c5ee624b18c3a04e6619fc43ce372b7f6078ee6b2f895026d4823` | 04:33:17 | `DomainInstance.java` |
| `8e6ed71b1111fb92ec4e158a5755a9247d35786f4a52bb0d1f326947e97c12ae` | 04:33:18 | `ContactEnchantState.java` |
| `1701771aeab2893c6ed906a6684b9e92d826d682fc94a82e3f5f4989ed2d6a5b` | 04:32:10 | `NetherDomainSync.java` |
| `82613056f2ec20885d4797f9e763aa34d3263f1f87f25c91c869885940a28e46` | 04:36:22 | `NetherDomainWisps.java` |
| `e7520a850db753fa86e6c5927a3bf84b526ac4ee17da7c53743f75640fd71553` | 04:34:48 | `FireVisualClient.java` |
| `fec34f797d9461276b1832309f2804c7902b5609ceda2d8fad2a715c3550a875` | 04:33:29 | `FireSeedFlameClient.java` |

所有权与 A、B、C 的文件没有重叠。命名门禁里，这次动过的文件没有新违规。未改的 `OriginalHumanoid.java` 仍有 `i` 和 `to` 两处旧短名，没有顺手改。

## 还没通的部分

1. `ModNetwork.register` 需要加上 `NetherDomainSync.register(CHANNEL, 14)`。登记前产种会跑，客户端不会变蓝，魂火条带也不会出现。
2. 只装幽冥时，`ContactCast.refusal` 会因为接触伤害为 0 拒绝整次释放，`ContactCast.snapshot` 也会变成 null。旧的 `tryWave` 因此挂不上纯幽冥。和伤害附魔叠在同一段时可以靠快照标识挂上，但根标识仍要改用 `tryWaveLoaded` / `tryWaveReservedLoaded` 传入。
3. `BatchCastSession` 的根标识是私有的，没有现成查询。找不到现成根账时不会新建一本。
4. 武器印记上的幽冥会被现有代码 `adopt`，没有领域租约时这里不会替它 `release`。
5. 灼地的客户端色用切片外包盒，产种按切片相交。落在外包盒里、却不在任何切片上的点会变色，但不会产种。
6. 原版火焰粒子、燎系剑火、炉身、雾雨和重压的水面材质没有改色。

详细调用点在 `docs/开发记录/术式批量制作/待制作-20261031/author-d-registration.md`，色调签名在 `author-d-interface.md`。
