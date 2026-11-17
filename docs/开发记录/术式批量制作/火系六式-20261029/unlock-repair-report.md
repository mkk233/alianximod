# 刻印台没有焚天、燎原、燎里

没有改源码。等级 20 已经满足复杂度门槛，刻印台还要求「已学会」。这三式复杂度是 20，又不是会在达级时自动学会的领域，所以 `/shu rank` 不会把它们放进已学会名单。已批准记录写明终极奖励获取途径另定、不自动发放。把达级改成自动学会是新的获取规则，这次不改。本阶段额度已用完，返修结果未追加复审。

## 原因

刻印台同时看两道门。

1. 已解锁：剑术看剑术等级，法术看法术等级，等级大于等于复杂度。`PlayerShuData.isUnlocked` 在 `PlayerShuData.java` 198–206 行。客户端同样比较在 `ShuSyncPacket.java` 175–181 行。
2. 已学会：必须在学会名单里。服务端列表在 `ImprintDeskMenu.java` 88–92 行；客户端在 77–84 行，已学会名单非空时同样要求 `hasLearned` 且 `isUnlocked`。真正刻印在 `ImprintActions.java` 33–35 行再查一次。

三式已经注册，不是没进目录。

- `ModTechniques.java` 187–198 行：`fire_liaoyuan`、`fire_fentian` 是法术，复杂度 20；`fire_liaoli` 是剑术，复杂度 20。同文件 175–186 行的升温、燃烧、冷却复杂度是 0。
- 同文件 249–254 行把三式交给 `TechniqueManager`。
- `ReleaseDefinitions.java` 38–40 行在 `ReleaseRegistry` 登记。它们走普通 `define`，没有领域声明。`ReleaseDefinition.java` 184–186 行的 `domain()` 默认是空。

等级命令只开第一道门。

- `ShuCommands.java` 480–493 行：`/shu rank` 写入等级和熟练点，调用 `DomainLearning.offerReady`，再同步到客户端。成功提示写的是「复杂度不超过该等级可以解锁」，指的是第一道门。
- `DomainLearning.java` 27–39 行只给「声明了领域，并且 `learnsWhenReady`」的术式补学。三式进不了这个循环。
- `PlayerConnectionHandler.java` 20–23 行登录时只学解锁等级为 0 的术式。复杂度 20 不会在登录时写入学会名单。
- `ShuSyncPacket.java` 147–158 行把等级和学会名单分开同步。等级变成 20 不会凭空多出三个 id。

因此法术 20、剑术 20 之后，`isUnlocked` 为真，`hasLearned` 仍为假，刻印台不列出焚天、燎原、燎里。这和「等级升了、这三式不出现」一致。升温、燃烧、冷却复杂度为 0，登录就会学会，不受这条挡住。

已批准的待制作说明写的是：终极奖励获取途径待另定，不自动发放；等级上限 20、复杂度 20、装配消耗 3 是首版默认。见本批 `test-stage-input.json` 里燎原、燎里、焚天的 `dv`。用户原话是这三式的效果，不是「满级自动出现在刻印台」。按满级自动学会会改掉这条获取约定，也会把现有的手动学习规则放宽，所以没有改代码。

## 复现和重测

需要权限等级 2。先确认跑的是已经注册这三式的包：`/shu list` 里应有 `fire_fentian`、`fire_liaoyuan`、`fire_liaoli`。没有的话，当前进程不是这份源码打出来的包，学习命令会报未知的术。

当前这份源码上，只做下面两条时，刻印台仍然没有这三式：

```text
/shu rank magic 20
/shu rank sword 20
```

要让刻印台列出并允许刻印，再执行：

```text
/shu learn fire_fentian
/shu learn fire_liaoyuan
/shu learn fire_liaoli
```

然后重新打开刻印台。焚天、燎原看法术等级，燎里看剑术等级；只升一边时，另一类即使学会也不会列出。

`/shu stone fire_fentian`（燎原、燎里同理）直接给物品，不写入学会名单，刻印台列表不会因此出现它们。

`/shu reset` 会把已注册的术全部标成已学会，范围比这三式大，不是这次的定向前置。

## 检查

没有改 `src`。`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1，只有既有的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有本次文件可修。

没有跑 Gradle，没有打包，没有启动游戏，没有提交。没有改制作表、`记录.md` 和既有报告。

## 不确定

不能从这份源码判断用户当时启动的是不是 2026-10-29 编译成功之后的包。旧包里如果还没有这三式，列表同样没有；用上面的 `/shu list` 可以分开。当前源码在新包上也会出现用户看到的现象，不需要靠旧包才能解释。

没有其他会话同时改这些文件的证据。本轮没有生产源码或测试文件写入。

## 停写

证据在 `docs/开发记录/术式批量制作/火系六式-20261029/unlock-repair-evidence.json`。本报告写完后不再写源码。本阶段额度已用完，返修结果未追加复审。
