# 燎里命中火环、常驻火苗与高速跟随

`fire_liaoli` 仍在制作中第 6 行。本单只改源码和一张运行时火苗图，没有写制作表、`记录.md` 或资源库索引。设计 2/2、实现 3/3 承接。本阶段额度已用完，返修结果未追加复审。这里没有新增审核通过。

待统一编译。未游戏实测。上次姿态编译不作为本次完成。

## 做了什么

后环不再跟着施法空播。有效命中且这次成功扣掉的火种严格大于 50 时，命中体记下当前世界时刻。客户端用这个时刻播放现有 `rear_ring.png` 的一轮 24 拍：三道环仍按原来的相位同时扩大，并向后扩散。一轮结束就停，不拆命中体。之后没有合格命中就不会自动再播。另一个世界刻里的合格命中会改掉开始时刻，从而重播一轮。同一时刻重复记下的还是这一轮，逐帧读取不会把头再掐回去。

火苗按当前共享余额显示。朵数是 `floor(S/10)`，0 到 9 不显示，100 是 10 朵。所有带着火种的实体都算，不只是燎里打中的目标。余额在服务端入账后发给正在看这个实体的客户端；玩家开始跟踪某个实体、登录、重生或换维度时也会收到当时的余额，0 也发，用来清掉离开视野时错过的扣除。客户端用实体编号加唯一标识核对，标识对不上就拆掉，避免编号被复用后串到别的生物上。不每拍扫描世界，也不每拍把全部实体再广播一遍。客户端只画，不写回账本，生产和消耗公式没有改。

燎里画面改绑同步下来的施法者。渲染原点用该玩家这一帧插值后的眼睛，视线偏航和身体姿态也取这个玩家，不再用 0.75 格去猜，也不拿滞后的命中体坐标代替已经绑定的人。对不上这个编号就保持站立，并且不绑到旁边的人。v7 剑阵、六张 v6 贴图、刺击剑、火流和后环的相对朝向没改。`ClientTrack` 和 `FollowCasterMotion` 没改，其他命中体的移动插值照旧。

## 改过的文件

| 路径 | 字节 | SHA256 | 最后写入 |
| --- | --- | --- | --- |
| src/main/java/com/bslx/XueXiMod/client/LiaoliV6Renderer.java | 18557 | 7AF3E679E944679B4F503090F0EC924A28171FCA1CA127405125C7E0A0688A02 | 2026-10-30 01:46:50.2298008 |
| src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java | 4989 | F30E7267C724E8C6A47601C5542681718B171C72F2AF28DCCBA8087AE0DC675E | 2026-10-30 01:46:40.6588343 |
| src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java | 36586 | 4B817CB956FF283D5024FFEF0886C20654B2308736B58C3DA98106FAC7D4A6F2 | 2026-10-30 01:46:34.4555018 |
| src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java | 6199 | 4F751ACE9355F5185B632A9E8FC3BA9A87956CEBE1DCA708E843E6F77733F230 | 2026-10-30 01:47:54.2186654 |
| src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedVisualRelay.java | 2789 | 42946CF97C5CDD65E0BFB9ECDE3AC8485269480AC8591344349A96176A1F7A18 | 2026-10-30 01:49:17.4081153 |
| src/main/java/com/bslx/XueXiMod/client/FireSeedFlameClient.java | 9932 | 657D3A2662C592DF00987A60E91DA5A7F016A128B4C180F8363A435765DB1B5E | 2026-10-30 01:48:18.8441063 |
| src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java | 28674 | 6EB0BE5C21A8EDBD97351EDDD767F383B77D3FA239836690506F6F2B66368DAF | 2026-10-30 01:47:53.5939711 |
| src/main/java/com/bslx/XueXiMod/network/FireSeedVisualPacket.java | 1871 | A81D9B74C15745D6C994CBDBFD13A9C35E5AEB6A3E557A2E23C3984C35AB634F | 2026-10-30 01:48:00.9906426 |
| src/main/java/com/bslx/XueXiMod/network/FireSeedVisualBridge.java | 566 | B0D03396F3ADB06C249AF5B96690CD808272080152F048D5961E9DB3C6092BBD | 2026-10-30 01:47:58.5675820 |
| src/main/java/com/bslx/XueXiMod/network/ModNetwork.java | 1842 | B30CBAD533958DEF8FF18473293B6CCDC08971E03814AE44CD69C9B31C258D25 | 2026-10-30 01:47:53.5939711 |
| src/main/resources/assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png | 1080 | 106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B | 2026-10-30 01:26:29.1386861 |

火苗图的哈希与确认档 `docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png` 相同。复制时保留了归档文件的写入时间，像素没有改。没有配动画 mcmeta，避免和手动切帧叠成两次换帧。

## 没有改的相关文件

`LiaoliV6Layout.java` 仍是 `98169A38537D34EA19B95DC9482A863D7A85E95562A2BC44AF2C49D599394C35`，2026-10-30 00:46:47.1639184。v7 站立尖朝下、柄近尖远、横身到腿部并前移 `[0,-12,0]` 都还在这里。

`ClientTrack.java` 仍是 `1C9F36BFA6C03F2C0F40F1C385AF0D31E05A89EEC0513169D2FA1026F58AA029`，2026-10-20 01:42:50.3034957。

`FollowCasterMotion.java` 仍是 `A8687BF126E0AE2A41C360D862B79670A57074189A1EF35C9938A2019C6362B3`，2026-10-20 01:50:21.6913587。

`AttributeStateData.java` 仍是 `A70A3496B3D0574E37ED8A771237837E184F732B3F3793B3A99AF71A31FB460F`，2026-10-21 23:33:46.2615055。火种写入本来就集中在 `FireSeedState`，所以账本文件没有动。

`FireVisualPacket.java` 没有改。语言文件没有改。

## 资源映射

| 用途 | 运行时路径 | SHA256 |
| --- | --- | --- |
| 未点燃剑 | textures/effect/liaoli_v6/sword_unlit.png | 2FB695A061875B6C88C102716F36E5B3791288F1EF9734E5EC7D847EFF7119B8 |
| 点燃剑 | textures/effect/liaoli_v6/sword_hot.png | 155FA83DFA7FD54F4F7040C3A2684F0DF622E17BE6DB76BB74FD328BAEAA42D6 |
| 背后点燃 | textures/effect/liaoli_v6/back_ignite.png | A83D034983C36F4EEB1B492CB6B269937BBCBFFD0DEB4FB0729E1E031E250CB2 |
| 背后火流 | textures/effect/liaoli_v6/back_flow.png | 254E8F1F63BE6F13017AC192FD4AF496B3ADB960133E55F65535A802B95FA6E0 |
| 刺击回流 | textures/effect/liaoli_v6/thrust_backflow.png | 2D57C54F8D99AC3E451B35A70009D99B2885B924242349A6C1B6BFE120BD329F |
| 后环 | textures/effect/liaoli_v6/rear_ring.png | 857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE |
| 火种火苗 | textures/effect/fire_seed/fire_seed_flame.png | 106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B |
| 剑阵姿态 | docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel | 未改，不进游戏贴图 |

六张 v6 的写入时间仍是 2026-10-29，和上一份姿态报告里的哈希一致。火苗是 32×256、8 帧、每帧 32×32。播放按 100 毫秒一帧手选图集行，相邻火苗错开相位，间隔仍是 100 毫秒，整圈 0.8 秒。

## 行为边界

后环看的是这次 `detonate` 成功快照里的点数。全部火种消费时，这个点数就是扣款前的余额。不在扣完以后再读余额。空挥没有命中回调。伤害权限没过就直接返回。余额不超过 50，或者扣款没有成功，都不记后环。物理仍是基础 6 点，火伤仍是 `0.36×S×(1+F)`，每个目标仍只结算一次。

同一世界刻里多次合格命中只记下同一个开始时刻，画面上就是刚开始的那一轮。之后不同世界刻的合格命中会重播。环画在命中体上：刺击寿命到了或收招时，命中体消失，环跟着停；环自己播完不会把命中体拆掉。迟加入的人如果这一轮还没播完，会从剩余进度看；已经播完的那一轮不会再自动放一遍。

火苗约 0.22 格见方，散在身体四周，跟着实体插值位置和身体朝向，不铺满整个身体。死亡、离开世界或余额降到 10 点以下就拆掉。换世界时客户端清掉旧画面，跟踪或换维包再按当时余额补上。

`TechniqueHitEntity` 给所有命中体多同步了施法者唯一标识，以及一个默认是 -1 的后环开始时刻。只有燎里的合格命中会改这个时刻。其他术式的运动、`ClientTrack` 插值和各自的渲染器都不读它。客户端 `casterUuid()` 现在能读到这个标识；`getCaster()` 在客户端仍返回空，服务端找人的路径没改。

燎里画面贴着玩家这一帧的眼睛，服务端命中体积仍按原来的跟随每拍放到眼睛。高速时画面可以比客户端上的命中体位置超前大约一拍。这是故意不改命中体插值的结果，不是新的判定规则。滑翔仍没有完整速度偏航，激流仍没有持续转圈，游泳仍没有原版那一下额外平移。

## 名称门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只剩 `OriginalHumanoid.java` 第 165 行的 `i` 和第 202 行的 `to`。本次文件没有新的命名问题，那两处没有改。日志是 `liaoli-hit-seed-implement-check.txt`。

## 用户实测四行

1. 触发阈值：空挥、没有伤害权限、扣除前火种小于等于 50，都不出后环。扣除前火种大于 50 的有效命中，播出一轮三道后扩环；环停了刺击还在就继续，不要提前拆掉命中体。过一会儿再打中另一个合格目标，会再播一轮。
2. 余额变化：带火种的实体显示 `floor(当前余额/10)` 朵，不满 10 点没有，100 点是 10 朵。消耗、补上、死亡、卸载或换世界后，朵数减少或消失。中途进来的人看到的是当时还剩的数量。
3. 高速跟随：自己冲刺、游泳或鞘翅时，剑阵、火流和后环留在身上，不闪到身后。
4. 多人跟随：两个人靠在一起时，特效只跟着施法者本人，不绑到旁边的人身上。

## 完整需求与拟显示

详细需求沿用姿态快照里的共享燃料正文和已确认 v7 姿态，再加上这次已批准的三项。下面是建议写入的详细 N。展示用的短句不能代替这段。

引爆侧终极剑术，必须手持剑。按住释放维持一次近战刺击，最长40tick（2秒），松开、换下剑或施法者死亡时提前收招；持续按住超过40tick不会自动再起一发，需松开后重新触发。出手时刺出，随后剑前刺击区域随施法者移动和准星方向保持，长度3格、宽0.6格、高0.6格；不生成远程飞剑、不自动突进、不穿实心方块。
40tick内接触有效目标即结算一次基础6点刺击伤害，并读取、一次消耗该目标全部火种S，追加0.36×S点即时火属性引爆伤害。100点追加36点，合计42点（不计其他加成与减伤）；无火种只造成基础刺击。使用线性转换，不套焚天的1.2指数。炉心只对火伤部分按既有同层规则增幅，不把物理刺击整体变为火伤。
同一次出手可以接触不同目标，但对每个目标最多刺击、消费和引爆各一次；命中后保持余下刺击时间与剑上火焰，不能逐tick重复命中同一个目标。引爆只伤被刺中的目标，不产生二次范围爆炸，不点燃地形、不破坏方块，也不补发持续燃烧。
首版实际魔力40、冷却300tick，从成功起手算；40tick内不按段重复收费。刺击命中长度与宽高可沿用已有几何放大，2秒上限固定，火种消费与附加收益不可被份数重复领取。
共享燃料补充：S是该次伤害被接受时目标的共享余额，不是挂载时预付的私有池。公式仍是基础6点物理，另加0.36×S×(1+F)即时火伤。若燃烧已经在同一余额上扣过，本式只消费当时剩余；若本式先清余额，正在运行的新式燃烧失去后续燃料。同一次出手对每个目标仍最多引爆一次。不改成0.2×S。
2026-10-30背后八剑：站立时剑尖朝下，剑柄靠近玩家，剑尖更远，左右镜像；横身俯冲时剑阵随身体转动并整体移到腿部，采用确认版微微前移。六张liaoli_v6贴图继续使用。
2026-10-30命中与跟随：现有三道后扩环只在有效命中、且成功扣除前的共享火种严格大于50时出现。50不触发，51才触发。沿用rear_ring.png和三道同时扩大、向后扩散的形态，按该次命中播放现有一个24拍周期；之后没有合格命中不自动续播，另一个时刻的合格命中可以重播。同一次命中不因逐帧数据反复从头播放。环结束不缩短命中体寿命。
火种常驻火苗不是新术式，不新增编号。所有带火种的实体显示floor(当前S/10)朵，低于10为0，100为10。火苗小而分散，跟随实体移动，不遮挡整个实体。扣除、余额变化、死亡、卸载或换世界后减少或消失。新跟踪到的玩家看到当前数量。客户端只显示，不改生产、消耗和伤害。确认图是火种常驻火苗_v1。
高速移动时按命中体同步的施法者身份绑定该玩家，用该玩家每帧插值的眼睛位置和转角渲染，连续跟随，不闪到身后，也不绑到旁边的人。保留上述v7姿态、六张v6贴图，以及刺击剑、火流、后环的相对朝向。

建议详细 O：

起手时剑身被由剑柄流向剑尖的橙红火焰覆盖，快速向前刺出一道短促亮痕。维持期间火舌持续从剑身与剑尖冒出，方向跟随剑身，不脱离成飞行弹。接触有火种目标时在刺入点形成向内聚拢后炸开的短促火团与火星，强度随消耗量增加；无火种只有普通命中亮痕。收招时剑上火焰迅速熄灭。
背后八剑沿用现有六张liaoli_v6贴图。站立时剑尖朝下，剑柄靠近玩家，剑尖更远，左右镜像。横身俯冲时剑阵随身体转动，整体移到腿部，并采用确认版微微前移。灰蓝角色和红柄金刃不进入游戏。
三道后扩环只在有效命中且扣除前余额严格大于50时出现，播放现有一轮后扩，不换rear_ring.png。火种火苗用确认版32×256图集，运行路径textures/effect/fire_seed/fire_seed_flame.png，数量为floor(S/10)。高速移动时剑阵和现有特效连续跟随施法者。

拟显示 N：

持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理。扣除前火种大于50的有效命中才播一轮后环。带火种的实体按余额显示火苗。

拟显示 O：

assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png
assets/xueximod/textures/effect/liaoli_v6/sword_hot.png
assets/xueximod/textures/effect/liaoli_v6/back_ignite.png
assets/xueximod/textures/effect/liaoli_v6/back_flow.png
assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png
assets/xueximod/textures/effect/liaoli_v6/rear_ring.png
assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png
docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel

## 停止写入

本文件、`liaoli-hit-seed-implement-check.txt` 和 `liaoli-hit-seed-implement-evidence.json` 写完后，本单源码作者停止写入。不再改上面的 Java、火苗图、制作表、记录或资源库索引。

待统一编译。未游戏实测。本阶段额度已用完，返修结果未追加复审。
