# 燎里命中火环交付准备

只准备记录和需求快照。制作表未改，`fire_liaoli` 仍在制作中第 6 行。源表 SHA256 仍是 `ECE5C0FAFAF440A70481463AC790F0E03BF83B33433AFF2A2495E78108EF78D3`。尚未编译，本单不迁待验收，也不是已完成或审核通过。

完整 N/O/DV 在 `liaoli-hit-seed-delivery-snapshot.json`。正文沿用退回制作中时的最新已批准原文，只追加火苗外观确认，没有按源码作者的建议重写。拟显示 N 沿用既有游戏内一句：「持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理」。本次没有改语言资源。源码作者的多句拟显示 N 不采用，新视觉留在完整快照。

## 所有权

实际写入源码和运行时火苗的是 `23ef4660-da4b-47a8-9341-8ec236ec5cc3`，并已停止全部写入。`1f4155a4-e537-4518-a3f7-893f09c5daff` 只读实施，没有写入，也已停止。工作簿和记录仍是 `d6f84d0b-4ea1-4368-a576-8acc20cc2afd`。资源归档和索引归主会话。统一编译者 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 尚未派发。

证据文件最后写入 2026-10-30 01:53:06.1970426。作者声明报告、检查日志和证据写完后停止，不再改这 10 个 Java、火苗图、制作表、记录或资源库索引。

| 路径 | SHA256 | 最后写入 |
| --- | --- | --- |
| client/LiaoliV6Renderer.java | 7AF3E679E944679B4F503090F0EC924A28171FCA1CA127405125C7E0A0688A02 | 2026-10-30 01:46:50.2298008 |
| shu/technique/effects/LiaoliEffect.java | F30E7267C724E8C6A47601C5542681718B171C72F2AF28DCCBA8087AE0DC675E | 2026-10-30 01:46:40.6588343 |
| shu/technique/release/entity/TechniqueHitEntity.java | 4B817CB956FF283D5024FFEF0886C20654B2308736B58C3DA98106FAC7D4A6F2 | 2026-10-30 01:46:34.4555018 |
| status/fire/seed/FireSeedState.java | 4F751ACE9355F5185B632A9E8FC3BA9A87956CEBE1DCA708E843E6F77733F230 | 2026-10-30 01:47:54.2186654 |
| status/fire/seed/FireSeedVisualRelay.java | 42946CF97C5CDD65E0BFB9ECDE3AC8485269480AC8591344349A96176A1F7A18 | 2026-10-30 01:49:17.4081153 |
| client/FireSeedFlameClient.java | 657D3A2662C592DF00987A60E91DA5A7F016A128B4C180F8363A435765DB1B5E | 2026-10-30 01:48:18.8441063 |
| client/FireVisualClient.java | 6EB0BE5C21A8EDBD97351EDDD767F383B77D3FA239836690506F6F2B66368DAF | 2026-10-30 01:47:53.5939711 |
| network/FireSeedVisualPacket.java | A81D9B74C15745D6C994CBDBFD13A9C35E5AEB6A3E557A2E23C3984C35AB634F | 2026-10-30 01:48:00.9906426 |
| network/FireSeedVisualBridge.java | B0D03396F3ADB06C249AF5B96690CD808272080152F048D5961E9DB3C6092BBD | 2026-10-30 01:47:58.5675820 |
| network/ModNetwork.java | B30CBAD533958DEF8FF18473293B6CCDC08971E03814AE44CD69C9B31C258D25 | 2026-10-30 01:47:53.5939711 |
| textures/effect/fire_seed/fire_seed_flame.png | 106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B | 2026-10-30 01:26:29.1386861 |

以上 Java 路径以 `src/main/java/com/bslx/XueXiMod/` 为根，PNG 在 `src/main/resources/assets/xueximod/`。火苗哈希与确认档相同。`LiaoliV6Layout.java`、`ClientTrack.java`、`FollowCasterMotion.java`、`AttributeStateData.java`、`FireVisualPacket.java` 和语言文件没有改。

## 已知限制

绑定身份不可用时仍保持站立。服务端命中体仍按 tick 跟随，画面可以领先大约一拍。滑翔没有完整速度偏航，激流没有持续旋转，游泳没有原版额外平移。名称门禁退出码 1，只剩 `OriginalHumanoid.java` 第 165 行的 `i` 和第 202 行的 `to`。未游戏实测。

## 实测四行

1. 编号 `fire_liaoli`，名称燎里。
2. 前置：学会并手持剑。伤害和火种公式不变。火苗用已确认的 8 帧图。本次尚未编译。
3. 步骤：扣除前火种分别用 50 和 51 做有效命中，并空挥一次。让余额跨过 10 的倍数，再离开重进，或让新跟踪者看同一实体。两名玩家靠近时，施法者高速移动。
4. 预期：50 和空挥不出后环，51 的有效命中播出一轮现有三道后扩环。火苗按每 10 点一朵，低于 10 没有，100 是 10 朵，余额变化后跟着变。离开重进和新跟踪者看到当时数量。高速移动时特效留在施法者身上，不闪到身后，也不绑到旁边的人。

用户反馈：待填。

## 编译

等性能优化批次三名作者和检查收束后，由本聊天指定 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 统一执行 `compileJava`。对方明确不另开 Gradle。当前还没有收到可以编译的通知。本单不等待该通知。最终迁移另单。

设计 2/2、实现 3/3 不变。本阶段额度已用完，返修结果未追加复审。

本单停止写入。未改制作表，未改 src，未跑 Gradle，未改资源库，未操作游戏，未提交。
