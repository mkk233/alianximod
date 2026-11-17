先读返修任务说明，按里面写明的范围改。基础死亡句改成只含受害者占位符，并让检查分别格式化单参、多传攻击者、`.player` 和 `.item`。R6 的单参死亡句缺口已经补完。本次源码写入已经结束。

基础键 `death.attack.fire_resource` 和 `death.attack.steam_burst` 现在只含 `%1$s`：

- 中文：`%1$s 被余烬吞没`、`%1$s 被蒸汽灼伤`
- 英文：`%1$s was swallowed by embers`、`%1$s was scalded by steam`

原版多传攻击者时，多余参数不进入显示。`.player` 仍是 `%1$s` 和 `%2$s`，`.item` 仍是 `%1$s`、`%2$s` 和 `%3$s`。没有改 `DamageSource`。

`FireDeathMessageCheck` 按原版 `%n$s` 规则实际格式化。中英文、两类伤害各覆盖：基础句一个实参、基础句两个实参且结果与单参相同、`.player` 两参、`.item` 三参。代入后不留 `%`。结果是 18 条断言，0 失败。没有跑其余检查，没有跑 Gradle，也没有启动游戏。
