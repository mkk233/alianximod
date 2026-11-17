我先读任务说明和当前执行流程，确认授权范围后再动手。任务授权我做燎里命中火环、常驻火苗和高速跟随的实现。我先读本批输入和定位结论，再对着现有代码接线。定位结论和确认图已经对齐。接下来读命中、火种同步和燎里渲染的现有接线，按授权范围改。定位已经对上：后环现在空挥也会播，客户端没有火种余额，施法者只靠 0.75 格距离去猜。接下来按确认范围改命中触发、火苗同步和施法者身份跟随。详细需求里有一句和已批准原文不一致。我先改回原文，再更新证据里的哈希。燎里后环、常驻火苗和高速跟随已经按授权接上。`fire_liaoli` 仍在制作中第 6 行。本单没有写制作表、`记录.md` 或资源库索引。

待统一编译。未游戏实测。本阶段额度已用完，返修结果未追加复审。没有新增审核通过。

## 行为

后环只在有效命中、并且这次成功扣掉的火种严格大于 50 时出现。点数取扣款快照，不在扣完后再读余额。空挥、没有伤害权限、余额不超过 50、扣款没成功，都不记后环。画面仍是 `rear_ring.png` 的三道同时扩大、向后扩散，按现有 24 拍播一轮。一轮结束就停，命中体继续活到原来的寿命。另一个世界刻的合格命中会改掉开始时刻并重播；同一世界刻里的多次命中仍是刚开始的这一轮，逐帧读取不会把头掐回去。

火苗数量是 `floor(当前共享余额/10)`。0 到 9 不显示，100 是 10 朵。所有带着火种的实体都显示。余额入账后发给正在看这个实体的客户端；开始跟踪、登录、重生或换维度时再发当时余额，0 也发，用来清掉离开视野时错过的扣除。包里带实体编号和唯一标识，标识对不上就拆掉。不每拍扫描世界，也不每拍重发全部实体。客户端只画，不写回账本。物理仍是 6 点，火伤仍是 `0.36×S×(1+F)`。

燎里画面按同步下来的施法者标识绑定该玩家，用这一帧插值后的眼睛、视线偏航和身体姿态来画。不再用 0.75 格猜人，也不用滞后的命中体坐标代替已经绑定的玩家。对不上这个标识就保持站立，并且不绑到旁边的人。v7 剑阵、六张 v6 贴图、刺击剑、火流和后环的相对朝向没改。

## 改过的文件

| 路径 | 字节 | SHA256 | 最后写入 |
| --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 36586 | `4B817CB956FF283D5024FFEF0886C20654B2308736B58C3DA98106FAC7D4A6F2` | 2026-10-30 01:46:34.4555018 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java` | 4989 | `F30E7267C724E8C6A47601C5542681718B171C72F2AF28DCCBA8087AE0DC675E` | 2026-10-30 01:46:40.6588343 |
| `src/main/java/com/bslx/XueXiMod/client/LiaoliV6Renderer.java` | 18557 | `7AF3E679E944679B4F503090F0EC924A28171FCA1CA127405125C7E0A0688A02` | 2026-10-30 01:46:50.2298008 |
| `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` | 28674 | `6EB0BE5C21A8EDBD97351EDDD767F383B77D3FA239836690506F6F2B66368DAF` | 2026-10-30 01:47:53.5939711 |
| `src/main/java/com/bslx/XueXiMod/network/ModNetwork.java` | 1842 | `B30CBAD533958DEF8FF18473293B6CCDC08971E03814AE44CD69C9B31C258D25` | 2026-10-30 01:47:53.5939711 |
| `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java` | 6199 | `4F751ACE9355F5185B632A9E8FC3BA9A87956CEBE1DCA708E843E6F77733F230` | 2026-10-30 01:47:54.2186654 |
| `src/main/java/com/bslx/XueXiMod/network/FireSeedVisualBridge.java` | 566 | `B0D03396F3ADB06C249AF5B96690CD808272080152F048D5961E9DB3C6092BBD` | 2026-10-30 01:47:58.5675820 |
| `src/main/java/com/bslx/XueXiMod/network/FireSeedVisualPacket.java` | 1871 | `A81D9B74C15745D6C994CBDBFD13A9C35E5AEB6A3E557A2E23C3984C35AB634F` | 2026-10-30 01:48:00.9906426 |
| `src/main/java/com/bslx/XueXiMod/client/FireSeedFlameClient.java` | 9932 | `657D3A2662C592DF00987A60E91DA5A7F016A128B4C180F8363A435765DB1B5E` | 2026-10-30 01:48:18.8441063 |
| `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedVisualRelay.java` | 2789 | `42946CF97C5CDD65E0BFB9ECDE3AC8485269480AC8591344349A96176A1F7A18` | 2026-10-30 01:49:17.4081153 |
| `src/main/resources/assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png` | 1080 | `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B` | 2026-10-30 01:26:29.1386861 |

火苗图哈希与确认档相同。复制时保留了归档文件的写入时间，像素没有改，也没有加动画 mcmeta。

收尾文档：

- `liaoli-hit-seed-implement-report.md`，14741 字节，`076284AAA9E246F580BE4C84EFA50C43E91C3F378AB9B310C68C2CAAA228A9A4`，2026-10-30 01:52:56.6147159
- `liaoli-hit-seed-implement-check.txt`，611 字节，`6FD9811D9DE9DBC562254989806312C41A161AC2FADA19704B0FAAB8C8E32D73`，2026-10-30 01:51:37.6006056
- `liaoli-hit-seed-implement-evidence.json`，4709 字节，`87D4A1780FFDC932E0D576011311199F5BD2256ACFE0AC0ED23EF64F1EBBAA28`，2026-10-30 01:53:06.1970426

本单源码、火苗图和上述三份文档到此停止写入。

## 没改的边界

`LiaoliV6Layout.java` 仍是 `98169A38537D34EA19B95DC9482A863D7A85E95562A2BC44AF2C49D599394C35`。`ClientTrack.java`、`FollowCasterMotion.java`、`AttributeStateData.java` 都没动。火种写入本来就集中在 `FireSeedState`。

所有命中体多同步了施法者标识，以及一个默认是 -1 的后环开始时刻。只有燎里的合格命中会改这个时刻。其他术式的运动和插值不读它。客户端 `casterUuid()` 能读到标识；`getCaster()` 在客户端仍返回空。

燎里画面贴着玩家这一帧的眼睛。服务端命中体积仍按原来的跟随每拍放到眼睛，所以高速时画面可以比客户端上的命中体超前大约一拍。滑翔仍没有完整速度偏航，激流仍没有持续转圈，游泳仍没有原版那一下额外平移。

名称门禁退出码 1，只剩 `OriginalHumanoid.java` 第 165 行的 `i` 和第 202 行的 `to`。本次文件没有新的命名问题，那两处没有改。没有跑 Gradle。

## 资源映射

| 用途 | 路径 | SHA256 |
| --- | --- | --- |
| 未点燃剑 | `textures/effect/liaoli_v6/sword_unlit.png` | `2FB695A061875B6C88C102716F36E5B3791288F1EF9734E5EC7D847EFF7119B8` |
| 点燃剑 | `textures/effect/liaoli_v6/sword_hot.png` | `155FA83DFA7FD54F4F7040C3A2684F0DF622E17BE6DB76BB74FD328BAEAA42D6` |
| 背后点燃 | `textures/effect/liaoli_v6/back_ignite.png` | `A83D034983C36F4EEB1B492CB6B269937BBCBFFD0DEB4FB0729E1E031E250CB2` |
| 背后火流 | `textures/effect/liaoli_v6/back_flow.png` | `254E8F1F63BE6F13017AC192FD4AF496B3ADB960133E55F65535A802B95FA6E0` |
| 刺击回流 | `textures/effect/liaoli_v6/thrust_backflow.png` | `2D57C54F8D99AC3E451B35A70009D99B2885B924242349A6C1B6BFE120BD329F` |
| 后环 | `textures/effect/liaoli_v6/rear_ring.png` | `857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE` |
| 火种火苗 | `textures/effect/fire_seed/fire_seed_flame.png` | `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B` |

火苗是 32×256、8 帧、每帧 32×32。按 100 毫秒手选一行，相邻火苗错开相位，整圈 0.8 秒。六张 v6 的写入时间仍是 2026-10-29。剑阵模型 `docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel` 未改，也不进游戏贴图。

## 用户实测四行

1. 触发阈值：空挥、没有伤害权限、扣除前火种小于等于 50，都不出后环。扣除前火种大于 50 的有效命中，播出一轮三道后扩环；环停了刺击还在就继续，不要提前拆掉命中体。过一会儿再打中另一个合格目标，会再播一轮。
2. 余额变化：带火种的实体显示 `floor(当前余额/10)` 朵，不满 10 点没有，100 点是 10 朵。消耗、补上、死亡、卸载或换世界后，朵数减少或消失。中途进来的人看到的是当时还剩的数量。
3. 高速跟随：自己冲刺、游泳或鞘翅时，剑阵、火流和后环留在身上，不闪到身后。
4. 多人跟随：两个人靠在一起时，特效只跟着施法者本人，不绑到旁边的人身上。

## 拟显示

拟显示 N：

持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理。扣除前火种大于50的有效命中才播一轮后环。带火种的实体按余额显示火苗。

拟显示 O：

```
assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png
assets/xueximod/textures/effect/liaoli_v6/sword_hot.png
assets/xueximod/textures/effect/liaoli_v6/back_ignite.png
assets/xueximod/textures/effect/liaoli_v6/back_flow.png
assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png
assets/xueximod/textures/effect/liaoli_v6/rear_ring.png
assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png
docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel
```

建议详细 N：姿态快照里的共享燃料正文和 v7 姿态保持原句，末尾加上这次三项。伤害段仍是「追加 0.36×S 点……100 点追加 36 点，合计 42 点」，`(1+F)` 留在共享燃料补充句里。

引爆侧终极剑术，必须手持剑。按住释放维持一次近战刺击，最长40tick（2秒），松开、换下剑或施法者死亡时提前收招；持续按住超过40tick不会自动再起一发，需松开后重新触发。出手时刺出，随后剑前刺击区域随施法者移动和准星方向保持，长度3格、宽0.6格、高0.6格；不生成远程飞剑、不自动突进、不穿实心方块。
40tick内接触有效目标即结算一次基础6点刺击伤害，并读取、一次消耗该目标全部火种S，追加0.36×S点即时火属性引爆伤害。100点追加36点，合计42点（不计其他加成与减伤）；无火种只造成基础刺击。使用线性转换，不套焚天的1.2指数。炉心只对火伤部分按既有同层规则增幅，不把物理刺击整体变为火伤。
同一次出手可以接触不同目标，但对每个目标最多刺击、消费和引爆各一次；命中后保持余下刺击时间与剑上火焰，不能逐tick重复命中同一个目标。引爆只伤被刺中的目标，不产生二次范围爆炸，不点燃地形、不破坏方块，也不补发持续燃烧。
首版实际魔力40、冷却300tick，从成功起手算；40tick内不按段重复收费。刺击命中长度与宽高可沿用已有几何放大，2秒上限固定，火种消费与附加收益不可被份数重复领取。
共享燃料补充：S是该次伤害被接受时目标的共享余额，不是挂载时预付的私有池。公式仍是基础6点物理，另加0.36×S×(1+F)即时火伤。若燃烧已经在同一余额上扣过，本式只消费当时剩余；若本式先清余额，正在运行的新式燃烧失去后续燃料。同一次出手对每个目标仍最多引爆一次。不改成0.2×S。
2026-10-30背后八剑：站立时剑尖朝下，剑柄靠近玩家，剑尖更远，左右镜像；横身俯冲时剑阵随身体转动并整体移到腿部，采用确认版微微前移。六张liaoli_v6贴图继续使用。
2026-10-30命中与跟随：现有三道后扩环只在有效命中、且成功扣除前的共享火种严格大于50时出现。50不触发，51才触发。沿用rear_ring.png和三道同时扩大、向后扩散的形态，按该次命中播放现有一个24拍周期；之后没有合格命中不自动续播，另一个时刻的合格命中可以重播。同一次命中不因逐帧数据反复从头播放。环结束不缩短命中体寿命。
火种常驻火苗不是新术式，不新增编号。所有带火种的实体显示floor(当前S/10)朵，低于10为0，100为10。火苗小而分散，跟随实体移动，不遮挡整个实体。扣除、余额变化、死亡、卸载或换世界后减少或消失。新跟踪到的玩家看到当前数量。客户端只显示，不改生产、消耗和伤害。确认图是火种常驻火苗_v1。
高速移动时按命中体同步的施法者身份绑定该玩家，用该玩家每帧插值的眼睛位置和转角渲染，连续跟随，不闪到身后，也不绑到旁边的人。保留上述v7姿态、六张v6贴图，以及刺击剑、火流、后环的相对朝向。

建议详细 O：起手火焰、维持火舌、引爆火团和收招熄灭不变。背后八剑沿用六张 v6 贴图；站立尖朝下、柄近尖远；横置时到腿部并采用确认版前移。三道后扩环只在有效命中且扣除前余额严格大于 50 时播放现有一轮。火苗用确认版图集，路径 `textures/effect/fire_seed/fire_seed_flame.png`，数量为 `floor(S/10)`。高速移动时剑阵和现有特效连续跟随施法者。
