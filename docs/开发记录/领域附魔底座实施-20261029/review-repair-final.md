我先通读返修审查说明，确认只读范围后再核对指定改动。返修范围已经明确：只核对这一处异常付款。我先读裁决、两份终稿和记录里的授权扩展，再对照返修基线。裁决只要求修「伤害已生效后异常仍退款」。我对照返修后的确认点和本地 Forge 调用顺序，核对应提交还是退款。作者把确认点放在 `setHealth` 返回之后。我对照本地 Forge 里扣血、吸收和同步回调的真实顺序，看这个点是否已经代表伤害生效。原 P2 只修上了 `setHealth` 返回之后的异常。吸收值在那之前已经写入，这段异常仍会退款。这是同一项未关严的 P2。

## 已关上的窗口

正价路径仍先 `ManaPaymentReceipt.charge`（`DomainContactBilling` 92–94 行）。`confirmTracked`（`TechniqueHits` 98–127 行）在离开前把 `healthApplied` 写入结果。`apply` 107–116 行按这个标志 `retainShare` 或交给 `finally` 退款，随后原异常继续抛出；107 行之后的 `deliverPayload` 不会执行。

本地 Forge `1.20.1-47.3.33` 里，`setHealth` 正常返回后才是第二次吸收写入、`DAMAGE_TAKEN`、`ENTITY_DAMAGE`、击退和死亡。`@At` 的 `shift = AFTER` 在这次 `invokevirtual` 返回后才调用 `noteHealthCommitted`（生物 mixin 40–51 行，玩家 mixin 18–29 行）。因此 `setHealth` 已返回后再抛出，会提交一次且不发载荷。正常返回仍只看 `hurtConfirmed`：正伤包括致死才提交，无敌、取消、事件最终数量为 0 则退款。`hurt`、`hurtRoot` 和其他 `hurtConfirmed` 调用没有改用跟踪。两条 `actuallyHurt` 由 `LivingEntity.hurt` 的 `@Redirect require = 2` 各包一次，原方法走 `this.actuallyHurt`，玩家覆盖仍动态分派一次。现有 mixin 没有第二处重定向这个调用。`FireHurtBoundary` 只包 `hurt`，不增加 `actuallyHurt`。

## 仍退款的已发生伤害

生物和玩家的 `actuallyHurt` 都在偏移 56 先调用 `setAbsorptionAmount` 并返回，然后才是 `ForgeHooks.onLivingDamage`（生物偏移 121，玩家偏移 62）。`setHealth` 更晚（生物 147，玩家 133）；生命分量为 0 时直接跳到方法末尾，根本不调用它。

生物的吸收是字段写入后立刻返回。玩家的吸收走 `SynchedEntityData.set`：先 `setValue`，再同步回调，方法返回时值已经在。被吸收的数量大于 0 时，这次返回表示吸收已经扣下。

随后伤害事件、饥饿、战斗记录或 `setHealth` 返回前抛出，`noteHealthCommitted` 不会运行。`healthApplied` 仍为假，92–116 行退款。吸收值已经变了，收据却退回。正常返回且事件数量为 0 的整段吸收仍按原「最终零伤」退款，那条不在这次漏口里。

作者证据只写了 `setHealth` 之后的那次吸收，没有写偏移 56 这一次。

## setHealth 未返回

「`setHealth` 未返回就不算扣血完成」不能证明生命值还没写上。`LivingEntity.setHealth` 只调用 `SynchedEntityData.set` 然后返回。`set` 在偏移 26 执行 `setValue`，偏移 34 才调用 `onSyncedDataUpdated`。生命值键不走 `DATA_POSE`，`LivingEntity` 也不为它处理睡觉位或存活标志；本仓没有在这个键上挂监听。写入发生在返回之前。就本仓这条生命值键而言，中间回调没有另一段会抛出的业务，不把未知子类覆盖算成新问题。

## 兼容与未验证

改动文件：`DomainContactBilling.java` 92–131 行，`TechniqueHits.java` 98–183 与 232–272 行，两个新 mixin，`xueximod.mixins.json` 33–34 行。没有活动跟踪时包装不压栈。同一收据只提交或退款一次。Mixin 尚未编译，注入是否按 `require` 接上仍未知。未跑测试和游戏。

最小修正：异常路径在「生命值 `setHealth` 已返回」之外，还要承认偏移 56 那次吸收确实减少。数值没变的写入不能当成已伤害。正常布尔结果、单次收据、不发载荷、异常继续抛出都保持。
