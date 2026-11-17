我先读任务说明，按里面写明的范围做完再交完整报告。任务要求先核对批次记录，确认 A/B/C 和文档都已收束，再做只读复核。记录显示 A、B、C 和文档都已最终交回并释放写入。接下来按合同和作者终稿核对实际改动。作者终稿已齐。我按七项合同核对登记、加载、伤害、附魔、费用和文档是否真落地。结论：**需返修**。实现第 1/3 轮只读复核。A（A2+A3）、B（B2）、C（C2）和文档都已最终交回，写入已收束，本轮可以审。未自行改代码。

统一编译没跑，游戏没测。命名门禁本轮没有重跑；三份作者报告一致，只有未改的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本次改过的文件没有新的命名问题要扩修。

## 问题

**1. 高 — 作者 A。** 接触附魔的伤害倍率收成 0 时，整次释放被拒绝，领域接触绑定不会发生。

触发：接触段里原始 `magicDamage` 大于 0，但 `EnchantmentSnapshot.damageOf` 已按各格术式做成已配置金额，合计小于等于 0。默认倍率是 1 时不会走到这里。

`ContactCast.refusal` 在显式接触宿主和普通接触宿主上都把这个合计当成「没有接触附加」：

```36:47:src/main/java/com/bslx/XueXiMod/network/ContactCast.java
                if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f
                        && !gloomOnDomainHost(skill, host, segment.enchantIndexes())) {
                    return "没有附加伤害的接触附魔暂不支持，这次没有释放";
                }
                continue;
            }
            // ...
            if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f
                    && !gloomOnDomainHost(skill, host, segment.enchantIndexes())) {
                return "没有附加伤害的接触附魔暂不支持，这次没有释放";
            }
```

`snapshot` 同样在 `contactDamage <= 0` 时返回空（第 112–116 行）。已配置金额来自 `EnchantmentSnapshot.damageOf` 第 85–92 行。幽冥零伤例外只放行幽冥，不放行倍率被收成 0 的普通接触附魔。

影响：这次释放在扣费前被取消。0 倍率不再只是一笔 0 伤害的攻击贡献。

最小修复：结构门改看原始加数是否大于 0。已配置金额只交给随后的命中。不要用配置后的 0 拒绝释放。

同一作者的 `TechniqueHits.hurt` 第 51 行在金额小于等于 0 时直接返回失败。`classifyRespecting` 第 95–96 行已经把它记成 `ESTABLISHED_NO_DAMAGE`。走 `hurt` / `hurtRoot` 的命中确认状态会被这次 0 金额丢掉。最小修复是与 `classifyRespecting` 使用同一种「成立但无正伤」结果，不要在进伤害前把贡献作废。

**2. 高 — 作者 C。** 火伤在缩放之后才接受检查；引爆会先扣种，再因 0 或超过 10000 而打不出去。

触发有两种。一是 `fire_detonate`、`fire_liaoyuan`、`fire_fentian`、`fire_kindle`、`fire_liaoli`、`fire_scorch_field` 或 `fire_cool` 的伤害倍率，或全局伤害倍率，把该笔收成 0。二是原始公式结果不超过 10000，缩放后超过 `FireLimits.DAMAGE_CAP`。倍率上限 1000，引爆余额和炉心都在合法范围内时可以跨过这条线。

`DetonatePlan.strike` 先用未缩放的 `planned` 做检查，扣完全部火种后才打击：

```36:48:src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/DetonatePlan.java
        if (balance > 0 && !FireLayer.acceptableDamage(planned)) {
            return Outcome.notReady();
        }
        double settledDamage = balance > 0 ? SpecialBalance.settle(ResourceEffectKeys.DETONATE, planned) : 0.0d;
        // ...
        int spent = FireSeedState.draw(target, balance);
        double fireBase = ResourceEnchantNumbers.DETONATE_FACTOR * (double) balance;
        boolean struck = FireDamage.strikeOnce(target, cast.casterId(), settledDamage, fireBase, true);
```

`FireLayer.acceptableDamage` 第 12–14 行只接受大于 0 且不超过 10000。`FireDamage.strikeOnce` 第 39–44 行因此拒绝 0 和超限金额。`SharedSpec.of` 第 33–34 行用同一道门拒绝挂载，引燃、燎原火种段和焚天段在倍率收成 0 或段伤超过 10000 时不会挂上。

影响：引爆把火种花掉，伤害没有打出。延迟燃烧计划直接不成立。炉心采样和 `furnaceSettled == true` 本身与合同一致，问题出在接受检查用的是缩放前的数，而且 0 被当成非法贡献。

最小修复：先 `settle`，再对已配置金额做检查；不接受就按未就绪返回，不要 `draw`。账本上限仍可拒绝超过 10000 的已配置金额。倍率收成 0 的那一笔要保留为已结算的 0 贡献，不要当成非法伤害丢掉。

**3. 中 — 作者 B。** 领域开域费和水幕生成费按份数相乘时没有溢出检查。乘积溢出成负数后，这次释放不扣魔力。

触发：单价已经按术式费用和全局魔力倍率缩放过，再乘份数。费用键上限 1000000，全局魔力倍率上限 1000，单价可以到 1000000000。一分为三的份数是 3 时，`1000000000 * 3` 溢出为负数。默认费用 40 或 8 不会溢出。

```153:157:src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java
        int unitCost = definition.balanceId().isEmpty()
                ? definition.initialCost()
                : SpecialBalance.mana(definition.balanceId(), BalanceKeys.MANA_COST, definition.initialCost());
        int bill = unitCost * count;
        if (!reserved && !MagicPool.canAfford(player, bill)) {
```

`BarrierService.releasePlace` 第 81–85 行对 `spawnManaCost` 做了同样的 `unitCost * count`。`MagicPool.canAfford` 和 `trySpend` 把小于等于 0 的金额当成可支付。编译后的预留路径用了精确乘法，这条直接放置路径没有。

影响：高费用、高全局魔力倍率、多份领域或水幕可以免费放下。水牢成功单价是逐个目标加到收据上的，退款用的是同一次单价，不在这条乘积里。

最小修复：份数相乘使用现有的精确乘法。溢出就拒绝这一次放置，不要扣成 0，也不要改成负数账单。

**4. 低 — 文档。** 指南对 0 倍率的完成声明和文首已知缺口互相矛盾。

`docs/配置平衡指南.md` 第 7–8 行写明 `ContactCast` 和 `FireLayer` 仍会拒绝 0。第 99 行又写成倍率收成 0 时仍是攻击贡献，不会拆掉领域绑定。测试若按第 99 行验收，会把上面两个代码缺口当成已经落地。

最小修复：第 99 行改成与文首相同的现状，写明这两道门还没放开。代码按问题 1 和 2 改完后，再把这句写回合同语义。

## 已核对、未列为问题的部分

标准键由 `BalanceCatalog.sealAfterContent` 在外部内容之后补上，然后各调用一次 B、C，再封存。同键重复声明会拒绝。原始默认计算器在封存时求值，并禁止读有效快照。世界文件已存在时只读；模板缺失才生成；整份损坏或 schema 不是 1 时保留原文件并安装代码默认。单个坏字段、未知术式和未知键只警告。别名与正式编号同时存在时正式编号优先。

客户端只装同步来的已接受覆盖值。断线只清客户端。`BalanceClientHooks` 和 `BalanceClientLogout` 都标了客户端侧，通用类不引用它们。

普通、批次、自动和后续链的段伤害都进 `configuredSegmentDamage`，宿主、首次共享附加和潮印各缩放一次。`carriedDamage` 没有生产调用。领域光环在 `DomainAuraResolver` 加一次。幻剑写入 `copiedDamage` 后，读档直接用 `Damage`。无潮印武器的 `Attack` 只走 `globalDamageContribution`；主刀潮印只加已保存金额，横扫不加。燎原本休先按当时炉心合成再缩放，并以已结算炉心进入打击。幽冥开窗、窗口号和首次等待都读 `NetherContract.periodTicks()`。水牢成功费用同一次单价收费和退款。水幕、水环的宿主预扣不读 `spawnManaCost`。

指南里的 `heat_beam` 示例键 `beamLength`、`hitIntervalTicks` 有声明和读取。`sword_thrust.damageMultiplier` 在标准伤害名单里。文首已写明待统一编译、游戏未测。

本轮没有逐行对比 B 的全部效果文件和 pre-B。上面几项是按登记键、消费点和跨作者合同抽查的。
