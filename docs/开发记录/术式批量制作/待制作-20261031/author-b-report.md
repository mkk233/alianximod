# B 最终报告：新火种资源 API 与六个附魔第一阶段

阶段是实现，不是正式复审。设计额度 2/2 已用完，本阶段没有重做设计。实现复审 0/3，本次自查不计入、也不代替复审。没有运行 Gradle，没有编译模组，没有测试、打包或启动游戏。写入到此停止。

## 结论

六个新附魔已经作为独立载荷登记：留焰、种火、引燃、引爆、幽冥、崩地。它们的魔力价格是 2，直接伤害是 0，效果键不是旧的 `fire`，所以不会继承固定 2 点火种、原版着火或 2 点附加伤害。旧五元素的数值分支保持原样。

同一次确认命中的新基数先相加，再乘产种倍率并向下取整一次；旧固定点数按原语义加在后面，然后进入同一个 100 上限余额。根施法上下文里每个目标只占一次机会，满额导致增加量为 0 时仍然占掉这次机会。

引燃、引爆和幽冥窗口仲裁可以按接口调用。留焰和崩地只交付了宿主合同与浅碗形状，没有第二套轨迹或地形执行。命中、武器、注册、领域和改块仍待作者 A、C 接线。

## 已实现

- `EnchantmentDefinitions.registerAll` 在原有五元素之后登记六个新定义。`magicDamage` 为 0。折叠进共享结算时伤害和速度倍率保持 1。
- `EnchantBonus.resolve` 仍按 `fire`、`wood`、`water`、`earth`、`metal` 计算旧数字。新载荷放在 `resourceLoad`。旧火的 `seedPoints` 仍只在效果键为 `fire` 时写成 2，种火的 10 不写进这个字段。
- `SeedMerge.commit` 使用调用方给的根 `FireCastContext`。例：本体 2 与种火 10 合成 12，倍率 1.12 时请求为 13，再和旧固定点数相加，最后钳到 100。它不读取 `hasConsumed`。
- `FireSeedGrant.deposit` 与 `FireSeedProduction.produce` 仍每次新建上下文，旧调用点的基数和倍率没有改。
- 引燃：`n = floor(余额 / 10)`，不足 1 档不挂。期限 `40 + 10n` tick，段数等于期限除以 10，首跳使用共享轨道的完整 10 tick 等待，不走当拍首段。伤害是 `2 * (1 + 启动时 fireBonus)`，资源增量是 0。满 8 层在扣燃料前拒绝。同一 `castId + target + fire_kindle` 成功挂上后不能再挂。燃料归零仍由原有共享泵丢弃未到期层，不另建燃料池，补种不刷新旧层。
- 余数轮换在 `RemainderRotation`。比例向下取整与 `FuelShare.divide` 的前半段相同，余数按层标识稳定次序、以世界 tick 为起点轮转。没有任何引燃层到期，或本拍还有旧共享层一起到期时，`pump` 仍调用原来的 `FuelShare.divide`。
- 引爆只 `draw` 当前余额，不调用会写下 `hasConsumed` 的旧 `consume`。伤害是 `0.2 * S * (1 + 当时 fireBonus)`。`FireDamage.strikeOnce` 传入炉心已结算。`FireRuntimeEvents` 没有改，现有的 `furnaceSettled` 会挡住通用火伤再加一次 F。
- 同一次 `ResourceHitCommit.commit` 的顺序是合并产种、挂引燃、引爆。引爆会抽空刚刚挂上的引燃层所依赖的共享余额，下一拍燃烧泵按原规则结束没有燃料的层。
- 幽冥 `NetherSeedArbiter.grant` 在一个窗口里只入账最高请求，同值取领域标识字典序较小者。它不占用命中产种标记。窗口记在目标的 `xueximod_nether_seed_window`。
- 崩地形状枚举得到 23 个相对方块，先近后远，不超过 32。这是纯坐标，没有改世界。

## 尚未接入

作者 A 需要登记术式、改魔力报价、把根上下文和 `resourceLoad` 送进命中，并在现有武器印记上调用 `commit`。现有 `Technique.manaCost` 会把 `magicDamage == 0` 收成 1，在报价改到 `explicitManaPrice` 之前，新附魔不会按 2 收费。`WeaponEnchant.imprint` 会丢掉只有新载荷的印记。本阶段没有改这些文件。

作者 C 需要按 `TrailContract`、`NetherContract` 和 `BreakGroundShape` 做轨迹、领域周期、蓝火和实际破块。接触附魔仍要求接触伤害大于 0，幽冥不能走那条旧采样。统一破坏开关不在本阶段的代码里。

因此现在还不能在游戏里放出这六个附魔。接口说明见同目录 `author-b-interface.md`。

## 检查

`tools/check-name-gate.ps1` 已运行，退出码 1。本次写过的文件没有报出短名或禁止缩写。仅有的两处在未改文件 `animation/runtime/OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。按门禁要求，没有顺手改那个文件。

名称门禁会解析源码，这不是模组编译。`compileJava --offline --no-daemon` 没有运行，待统一编译。

浅碗格数用独立枚举核对为 23，没有跑项目测试。

## 源码哈希

SHA-256 与本地修改时间如下。

| SHA-256 | 修改时间 | 路径 |
| --- | --- | --- |
| 3010454BF5FC38FD93B617D41DFEF4CE3A589C19E6CAB71DE5F7BAA844866272 | 2026-10-31 03:36:22 | src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentDefinitions.java |
| DFCAC88C28EAD75BDE13F038A22E497721107D1EECA4E474F7AC80ACED6514EA | 2026-10-31 03:38:15 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/EnchantBonus.java |
| BEA61CB9FF6B7E0B0C2629357014098C16DB8020263F0238E5DBA430C6D326DF | 2026-10-31 03:36:01 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireCastContext.java |
| 14CE60BB828515D7194024620BF874E0A6654B43541F36CC0655ECABFE855488 | 2026-10-31 03:36:03 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedAccount.java |
| 4FEB61F8C2B5CF4FD085F9BECCA80473D08E173FAF58E0E3E39BE6BDAF6DAF77 | 2026-10-31 03:36:05 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java |
| BACCDCD1F098C6C99986C7DF1536A16F82FB5C8E8FE13CF188BDA4A6EB72AA71 | 2026-10-31 03:36:07 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedGrant.java |
| 36E2A345AF15FD15D05AC676C6AFFF44ECB80194D06F1A92C143661ED526E8A4 | 2026-10-31 03:36:07 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedProduction.java |
| 0C6124AD26EC90517D4EC506AA3E4B41BC2434509F833E244CA632E61A9BDF29 | 2026-10-31 03:35:58 | src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedMath.java |
| DC00ADD776623E3D90EA67873981D9B71F90286989694709DBE42F2F2265A31E | 2026-10-31 03:39:12 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/BurnLedger.java |
| 8AEAAA1395A74A29BE62148C4DDDBB93033CD32DFDA1704743763BFD05086638 | 2026-10-31 03:37:54 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireConsumption.java |
| D3D49D6D5C016E137D4CCB9AC855BF47BD4AE1C7CDB5733DC2C73F80C9CDEB2B | 2026-10-31 03:34:42 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantNumbers.java |
| DF90543B4B89F99A238AC4AFA58C9F859F34D690AAD24D12AB98F97FF7F4E5BE | 2026-10-31 03:34:48 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantSlice.java |
| 0B44F14F6EA429A88460A8EBA68BDF57A254936066187DE450BB5EB4C88E3EEF | 2026-10-31 03:34:51 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantLoad.java |
| E47FFCCD5E6F2CF315DA1E68925F6AD4FD42C23363CF2F8991950960268EAFE2 | 2026-10-31 03:34:53 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantDefinition.java |
| C12519A675B032C46F3D7F0041F05849F8F3B4C889E7BC2686CC8EFE9E9B42C1 | 2026-10-31 03:34:58 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantCatalog.java |
| EDD1E7AE1E99EC50913D69BD2F7A7F54CF2AC91F2AD08E9AFA8C16FABE90A8A9 | 2026-10-31 03:34:44 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/TrailContract.java |
| AC7DD17C608FFD68EBF06B19BF59C75A152F02B7FDC66CEF1F8E4D86BBFCD3D0 | 2026-10-31 03:37:51 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/NetherContract.java |
| DA503F4CB976BC5DB8BEDC29FE47D107E813D8D62BF6F6054C78EE90BA039EF7 | 2026-10-31 03:35:13 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/BreakGroundShape.java |
| C17201E47BA20BE420C8F214EDE9EB860D9091ACDF157A097701CCC4881C743A | 2026-10-31 03:38:15 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceHitCommit.java |
| 53F23B321DB6458834FFAFB7893D3111BE0BFC6EAE1987034093D7176A8BC78B | 2026-10-31 03:35:35 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/HitResourceResult.java |
| FBE01BC460EE718622328C93CCE2AD1B6B4048B682E979DE6EB8580FF0ED6A05 | 2026-10-31 03:34:41 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/ResourceEffectKeys.java |
| A83565742C7EEB42DD123FDA05FCCB1146FF98E528C5F122F049D5F345170706 | 2026-10-31 03:35:56 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/KindlePlan.java |
| DBE3FFACABA625DC77FB26F5E7F926890E80B84ED4E8DB21ED093AF345FAB333 | 2026-10-31 03:35:22 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/DetonatePlan.java |
| 1450C182256088E47AACE409E078667F226DC8625EC1FA756FFEBDD5BA2361CD | 2026-10-31 03:35:56 | src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/RemainderRotation.java |
| 1A7CE34B0D00F307405662D18CE52649702C9DFF795542AA9DB94EC989D5E392 | 2026-10-31 03:35:04 | src/main/java/com/bslx/XueXiMod/status/fire/seed/SeedMergeResult.java |
| 6256882E035F96E9FCF3018B039EDF741C32B55B0D7E4E919E105C3EB55CC017 | 2026-10-31 03:35:06 | src/main/java/com/bslx/XueXiMod/status/fire/seed/SeedMerge.java |
| 3B568BD0523897112377B6D4DE10AC31E8752A848435BEA1AD2A13B0637539D5 | 2026-10-31 03:35:32 | src/main/java/com/bslx/XueXiMod/status/fire/seed/NetherWindowResult.java |
| D8F3053812579DDF44571F7AAE6FE6185FADB2C5576D00856113B21F6CEBABE9 | 2026-10-31 03:36:06 | src/main/java/com/bslx/XueXiMod/status/fire/seed/NetherSeedArbiter.java |
| C68E73E656D52CB4E8B5A456F262A7579DC338704FFFAE67D71B7077042E602B | 2026-10-31 03:38:18 | docs/开发记录/术式批量制作/待制作-20261031/author-b-ownership.json |

未改但属于只读接缝的文件：`FurnaceService.java`、`FurnaceMath.java`。未改的独占文件：`FireLimits.java`、`FireRuntimeEvents.java`。

## 停止

源码和本阶段允许的四份记录已经写完。不再改表、资源、语言或其他作者的文件。等待统一编译和后续接线。
