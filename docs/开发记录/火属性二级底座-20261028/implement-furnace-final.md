我先读完整任务说明，再按其中授权范围执行。任务是作者 B 的炉心状态实现。我先读完整契约和主设计，再按 B 的文件归属落地。契约和炉心数值已经对齐。接下来对照现有效果、持久化和伤害钩子，确认炉心该接在哪里。本地 Forge 1.20.1 的伤害顺序已经核对过：抗性在护甲之后、保护附魔之前。接下来按这个钩子实现炉心状态，不改 A/C 的文件。炉心状态已按作者 B 的范围落地。普通与过载共用无限效果 `xueximod:furnace_core`，热量、属性和生命周期都在服务端运行态里，主动冷却成功后才会调用蒸汽。

## 查询与转换

`FurnaceService` 的固定查询：

| 方法 | 无炉心 | 普通 | 过载 |
|---|---|---|---|
| `fireBonus` | 0 | 当前 F，上限 0.30 | 当前 F，上限 0.60 |
| `seedMultiplier` | 1 | `1.12 + 0.48 × clamp(F/0.30, 0, 1)` | 2.2 |
| `waterIceMultiplier` | 1 | 1.4 | 1.7 |
| `active` | 仅服务端玩家且效果还在 | 同左 | 同左 |

客户端或非玩家走无炉心默认值，不写存档。火伤和水冰承伤仍由 C 在伤害事件里调用这些查询。

转换返回 `FurnaceChange`，原因在 `FurnaceMath.Outcome`。只有 `succeeded()` 为真时，以后的术式才应扣魔力或进冷却。

- 无 → 普通：`ENTERED_NORMAL`
- 无 → 过载：`ENTERED_OVERDRIVE`
- 普通 → 过载：`UPGRADED`，同一次提交保留 F 和上次更新 tick
- 重复进入、过载降普通：失败，不重置热量和时钟
- `endPassive`：退出并摘掉修饰器，不放蒸汽
- `cool`：先走成功的 `endPassive`，再调用 `SteamBurst.release(Player)`。C 的方法签名已是 `public static void release(Player)`

空玩家或客户端调用分别是 `REJECT_NULL`、`REJECT_CLIENT`。

## 状态与钩子

热量按千分位整数走：普通每拍 +0.002、150 拍到 0.30；过载每拍 +0.003、200 拍到 0.60。普通满热升级后再 100 拍到 0.60。每个在线玩家 tick 只加一拍；间隔再大也不补。登录把时基改成当前 `tickCount`，不补离线。换维不改 F，同一实体继续计时。

运行态写在 `PersistentData` 的 `xueximod_furnace_core`，字段是版本、玩家 UUID、档位、F、上次 tick、进入 tick。不放进 `PlayerPersisted`，所以死亡克隆不会自动带走。损坏、NaN、负数或认不出的版本会归零并打一条警告；高出当前档只钳制。

属性是三枚固定 UUID 的 `MULTIPLY_TOTAL`：生命 ×0.75 / ×0.60，护甲和韧性 ×0.50 / ×0.25。升级先摘再挂。进入和升级后当前生命取 `min(当前, 新上限)`；退出只恢复上限。

本地 Forge `1.20.1-47.3.33` 里，`actuallyHurt` 的顺序是 `onLivingHurt`、护甲、`getDamageAfterMagicAbsorb`、吸收、`onLivingDamage`。抗性在 `getDamageAfterMagicAbsorb` 前半段，保护附魔在后半段。`FurnaceResistanceMixin` 只让炉心玩家的抗性 `hasEffect` 返回假，不取消抗性药水，也不改保护或别人的抗性。

治疗接的是 `LivingEntity.heal` 里的 `LivingHealEvent`：普通 ×0.50，过载 ×0.30。吸收心和直接 `setHealth` 不经过这条路径。

牛奶走 `MilkBucketItem` → `curePotionEffects`，只摘治愈物列表里有的效果，默认是奶桶。炉心治愈物列表为空，所以牛奶摘不掉，也不会删了再加。`/effect clear` 和不死图腾走 `removeEffect` / `removeAllEffects`，不看治愈物，会清运行态和属性，不放蒸汽。

普通升过载用 `forceAddEffect` 换掉整条实例，避免 `MobEffectInstance.update` 在“更高档且更短”时把旧档藏进 `hiddenEffect`。命令施加会先被改成无限时长并清掉隐藏链。玩家 tick 开始早于 `tickEffects`，存档里残留的隐藏档也会在倒计时前被拆掉。`/effect` 的 amplifier 只允许 0 和 1，不能出第三档。

效果还在、运行态没有：按当前合法档用 F=0 重建并警告一次。运行态还在、效果没有：清运行态，不补效果，不放蒸汽。死亡清效果和运行态，不放蒸汽；非死亡克隆复制运行态、重新挂效果并重设时基。

## 改动文件

新增 `status/fire/furnace` 下的数学、存档、属性、效果、运行态、服务和事件，以及 `FurnaceResistanceMixin`、`FurnaceEffectAccess`。测试是 `FurnaceMathCheck`、`FurnaceStructureCheck`。

改过的既有文件只有 `ModMobEffects.java`、`xueximod.mixins.json`、`zh_cn.json`、`en_us.json`。效果名是「炉心」 / `Furnace Core`。没有改火种、火伤、术式、魔力或冷却。

## 已跑和未跑

纯 Java 检查已过：数学 50 条、结构 19 条，覆盖爬坡、离线不补涨、转换保值、清洗、倍率，以及蒸汽只出现在 `cool`、抗性钩子位置、空治愈物列表和 mixin 登记。

命名检查里，这次改过的文件没有新违规。仓库里原有的 `OriginalHumanoid.java` 仍有两处旧名字（`i`、`to`），没有顺手改。

没有跑 Gradle，也没有进游戏。`SteamBurst.release` 要等三方源码收束后的统一编译才能确认能编过。

还需要在游戏里看的有：奶桶喝下去炉心还在、`/effect clear` 能清、抗性对本玩家失效而保护附魔仍有效、自然回血和治疗药水被缩放、吸收心不被削、进入时生命不按比例变化、登录不补离线热、换维和非死亡克隆保留 F、死亡不留炉心也不喷蒸汽、普通升过载不掉 F 也不叠两层修饰器、只有主动冷却喷一次蒸汽。
