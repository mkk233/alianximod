# 元素法术资格收口

当前只支持元素法术消费聚能。无属性法术不支持。这是按「聚能只对元素术式生效，剑术无法使用聚能」收口，不是用户从选项里点过一项。

## 工厂

`SkillChainPlan.consumesElementMagic` 要同时满足：技不为空且是 `SkillType.MAGIC`，索引合法，成员不为空且是 `TechniqueType.MAGIC`，属性不为空且不是 `ShuAttribute.NONE`，定义存在且 `ReleaseKind.HIT_VOLUME`，成员不是附魔。任一不成立，`frozenChainInput` 返回 `ReleaseInput.BASE`，不扫描链倍率。

没有用正伤害过滤，所以零伤的元素法术命中体积仍会扫描。聚能源自己的类型、属性和原胚不参与这名成员的资格。倍率恰好为 1 时仍是 `new ReleaseInput(gatheredEnergy)`，没有收成单例。`ChainRole.ATTACK` 没改。

## 注册和装配

`ModTechniques.RULE_GATHER_ENERGY` 从 `TechniqueType.UNIVERSAL` 改为 `TechniqueType.MAGIC`。释放定义仍是规则、`chainEnergyScale` 为 2。普通规则和动作的注册类型没改。`before` 和 `before-eligibility` 里已有的 `ModTechniques` 快照没有覆盖。

现行服务端菜单是 `SpellAssemblyMenu`，不是旧的 `SkillAssemblyPacket.put`。剑技槽 `rejectReason` 在成员为法术且 `SkillCarrier.allowsMagic` 为假时返回「法术不能装入剑技槽」。`allowsMagic` 只在手持法杖时为真。`GearSlot.mayPlace` 要求这个原因为空，`set` 有原因就提示并直接返回。因此新的法术聚能进不了剑技槽，靠的是已有类型兼容，没有另加术式 id。法杖分支只拒绝剑术，聚能仍可装上法杖。

旧 `put` 的非法杖分支先做同一条法术检查。后面的 `suppliesChainEnergy` 拒绝还在，但法术聚能会先被类型检查返回，到不了那一行。

已有旧剑技链不靠装配补救，工厂对剑技整条返回基准倍率。

## 检查

`compileJava --offline --no-daemon` 退出码 0，`compileJava-final.log`。`RouteReleaseInputCheck` 46 条，失败 0，退出码 0，`check-route-final.log`。类路径是 `checks-integration`、`build\classes\java\main`、`build\tide-classpath.txt`。生产类来自这次编译。

覆盖：元素法术正伤 2 和 4，零伤容器 2 和 4，无属性、元素剑术、剑技槽伪造法术、动作、通用、规则种类、附魔、空技、空列表、负索引、越界、空成员、空定义都是倍率 1。第二名成员倍率是 1，不是第一名的 2。缺月自身 1.35 仍是 1.35。注册类型断言为法术，且不能用于剑技类型。

菜单方法要玩家，这次没有启动，不能写成菜单执行通过。实体、水牢、`ReleaseInput` 契约的生产代码没改，没有重跑。容器和墙宿主仍未跑完。没有打包，没有进游戏。`SkillFollowCast` 没改。
