先读这份实现任务的授权范围，再按它写明的边界落地。授权已经明确：点火额外 +2，热光束每次有效命中 +1，火球每个实际目标 +3，并在伤害被接受时只挂一次。接下来核对输入和现有结算入口。点数会跟着附魔数值走，并在伤害被接受时合并挂上一次。热光束的后续命中各自使用新的火种上下文，不去改消费者的去重契约。本单是作者实现，不是实现第 3 轮复审。未追加正式复审。未跑 Gradle，未改测试，未改制作表、记录、历史报告和资源库索引。未启动游戏。源码已经停写。待统一编译。

设计历史仍是 2/2，实现历史仍是 2/3。这次不把作者实现算进复审轮次。

## 点数

生产倍率固定 1。不随伤害、炉心或聚能变化。上限 100、目标身上的余额、死亡清零和消费者的一次消费去重都没改。`FireSeedAccount` 与 `FireCastContext` 的契约没改。每次挂载新建一本上下文，所以热光束下一次命中不会被同一本批次账的“已经生产过”挡住，消费者那本 `ReleaseBatch.fireContext()` 也不被占用。

| 来源 | 打中之后 |
| --- | --- |
| 热光束 | +1 |
| 火球，每个实际目标 | +3 |
| 点火附在术式或武器上 | 额外 +2 |
| 热光束带点火 | 1+2，一次挂 3 |
| 火球带点火 | 3+2，一次挂 5 |

空挥、没打中、伤害被拒绝、自己、旁观、无敌、已死、客户端都不挂。`FireSeedGrant.deposit`（17–22 行）先走 `DamagePermission`，再 `grant`。杀死目标后实体已死，现有 `grant` 要求存活，这一下不加火种。

`MagicIgniteEffect` 的视线盒子没有改，也不产种。

## 三个入口

热光束：`HeatBeamEffect.java` 36 行把规格点数写成 1。间隔仍是 10 tick，跟随准星，不因命中销毁。之前只造成伤害和附魔点燃。现在每次伤害被接受再挂 1，带点火则是 3。

火球：`FireOrbEffect.java` 43 行写成 3。直线、穿透、每个目标 `hitOnce` 都没改，没有改成爆炸，也不是只给第一个目标。之前命中点燃 4 秒并结算伤害。现在每个打中的目标再挂 3，带点火则是 5。

点火：`EnchantBonus.java` 193 行有火效果就是 2，不乘层数或时长。燃烧秒数仍按原来的 5 秒基数和时长倍率。`EnchantmentSnapshot.java` 112 行给接触快照同样的 2。武器印记多写 `SeedPoints`，消耗时机和燃烧时长没改。

原生点数和点火点数在 `TechniqueHitSpec.java` 162 与 192 行加一次，得到 `settleSeedPoints`。公共链不按这三条 id 分支。`SkillUsePacket` 和 `release/fire` 没有这三条 id。

## 只挂一次

同一次结算只调用一次 `deposit`。监听、`setSecondsOnFire`、原版燃烧 tick、载荷 `apply` 本身都不产种。没有挂全局 `LivingHurtEvent`。

- 没有账本：`TechniqueHitSettle.java` 139 行在 `hurtRoot` 为真之后挂；163 行在持续流 `hurtConfirmed` 为真之后挂。失败不挂。
- 有账本：伤害仍按原来延迟 6 tick，多份复制合成一次 `hurt`。`ReleaseBatch` 257–265 行按“目标 + 轮次”只记下第一份正点数，不乘复制份数。291 行在这次伤害被接受后挂一次。同一轮再次 flush 或重复回调见到已挂标记就不再挂。热光束下一轮是新的轮次，间隔仍大于 flush 延迟，所以下一次真实命中会再挂。
- 无潮印武器：`WeaponEnchant.java` 127–147 行只在印记那一刀 `hurt` 为真时挂一次，然后照旧删印记。点燃仍发生，不看这一刀是否打中。
- 潮印武器：`onAttack` 仍直接返回。`finish` 在伤害被接受后于 206 行挂一次。印记相等和盖章检查没改。
- 接触宿主（场地、屏障、护体、领域、牢）：`ContactEnchantState.java` 92 行在原有 `hurt` 已经为真、目标仍存活时挂一次，再执行原来的点燃。采样间隔没改。
- 操纵剑：`SwordEntityMotion.java` 91–94 行在该剑自己的 `hurtRoot` 为真时挂帧上的点火点数。剑不进命中账本，每把剑打中仍是各自的结算。一分为三的命中体若共用账本，则上面的账本路径只挂一次，不按份数再乘。

## 文案与资源

游戏刻印和装配界面读的是 `Technique.getDescription()`。语言文件按六式既有键补上同文。没有新贴图。热光束仍是视觉号 2 的程序光束，火球仍是视觉号 4。点火没有命中视觉。表里的 `O` 仍是类名。

当前表输入加上这次增量：

- `heat_beam`：持续释放一束随准星转向的火属性光束，对接触的生物造成伤害。每次打中挂 1 点火种。
- `fire_orb`：飞出火球，命中造成伤害并点燃。弹道可被改写。每个打中的目标挂 3 点火种。
- `magic_ignite`：点燃目标/方块。附在术式上，或记在武器上时，打中的那次攻击额外挂 2 点火种。

英文：

- Heat Beam: Hold a heat beam that follows the crosshair and damages what it touches. Each accepted hit mounts 1 fire seed.
- Fire Orb: Launch a fire orb that damages and ignites on hit. Its path can be rewritten. Each hit target gains 3 fire seeds.
- Ignite: Ignite a target or block. An enchanted technique, or the next weapon attack, mounts 2 extra fire seeds when that attack damage is accepted.

键：`technique.xueximod.heat_beam`、`fire_orb`、`magic_ignite` 及各自的 `.desc`。

## 名称门禁

`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。日志：`seed-source-implementation-name-gate.txt`。本次文件没有违规。只剩既有 `OriginalHumanoid.java` 165 行 `i`、202 行 `to`，未改。

## 旧检查

`FireSeedCheck` 的同上下文去重、`FireReleaseStructureCheck` 禁止 `release/fire` 出现这三条 id，都还对着未改的契约。若有检查写死这三句旧描述，或写死武器印记没有 `SeedPoints`，会和现在的文案、印记不一致。伤害、点燃、无敌帧的旧检查没有被这次改写。

## 记录

- `seed-source-implementation-report.md` SHA256 `D0DCB62C8D0DD8254BF20949AD4AD2DECE3CF46D10C71ECDF83D10B44DC67D9D`，2026-10-29 12:48:56.9047706
- `seed-source-implementation.json` SHA256 `254D7C0A9D11764EC51DC853713F1193CBDF07A864B4945334548519FB70E78E`，2026-10-29 12:48:31.5435959
- `seed-source-implementation-evidence.json` SHA256 `0ECC3FFDA4B922795E168AD98E4EB82AA453F8D0E3DEA53D71D3F008A5E68458`，2026-10-29 12:49:04.9745701
- `seed-source-implementation-name-gate.txt` SHA256 `8620E748029B57EB0F30020BAC5548CF1688413C22A7EEA01E1442A6150D3505`，2026-10-29 12:48:28.1448275

18 个改动文件的 SHA256 与 mtime 在报告和 evidence 里。读期间这些文件由本会话写入。没有发现其他作者同时改这些路径。

未追加正式复审。待统一编译。源码停写。
