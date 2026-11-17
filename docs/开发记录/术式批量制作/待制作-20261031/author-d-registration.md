# 幽冥接线现状

A 已经登记 14 号 `NetherDomainSync`，并接上了零接触伤幽冥、`tryWaveLoaded` 和 `BatchCastSession.currentRootId`。本单没有再改 `ModNetwork`。没有第二条通道，协议字符串没改。

## 报文尾部

`register(SimpleChannel, int)` 没变。A 登记的是 `NetherDomainSync::encode`，所以新尾部会跟着这个类走，不需要再写一行注册。

前缀仍是：

```text
boolean removed
UUID domainId
varint revision
UUID ownerId
utf dimensionId
double lowX lowY lowZ highX highY highZ
long visualEnd
```

紧接着追加：

```text
varint volumeCount
varint fragmentIndex
varint fragmentCount
varint pieceCount
pieceCount * 6 个 double
```

每片六个数是低 X、低 Y、低 Z、高 X、高 Y、高 Z。单片上限 48，总片数上限 3072。超过总上限时这一发同步成 0 片，服务端和客户端都不把外包盒当成幽冥。不截成一部分再当成完整范围。

前缀里的盒子是切片外包，不参与染色。旧的单盒解码读不完这个尾部。本构建的两边必须一起用现在的 `encode` / `decode`。

## 灼地调用

`ScorchEffect.execute` 把同一次载荷和 `BatchCastSession.currentRootId(player)` 传给 `SurfaceFireHosts.open`，再进 `ContactEnchantState.open(carrier, snapshot, load, rootCastId)`。

- 批量出生槽存在时，载荷用槽上的 `resourceLoad`。那次收集和 `EnchantBonus.resolve` 用的是同一组附魔下标。
- 没有出生槽时，用当前宿主快照的附魔标识去对法杖链上的灼地段，再 `EnchantBonus.resolve(..., 1)`。对不上就传空载荷，不另造一份。
- `currentRootId` 为 null，或绑定表里没有这本账时，不调用 `RootCastBindings.open`，也不把 `ReleaseBatch.fireContext()` 的标识拿来当幽冥根。灼地原有的火种释放记号仍走原来的 `fireContext()`，那不是幽冥根。

宿主 `close` 时 `NetherDomainCycle.drop` 释放自己 `retain` 过的那一次。维度卸载和服务器停止仍走原来的清理。

## 不需要 A 再改的部分

网络登记、领域 `tryWaveLoaded`、零接触伤放行都已经在 A 的桥里。普通链没有批量会话时根标识就是 null，这是 `currentRootId` 的结果。本单不要求 A 为了灼地再开一本随机账。
