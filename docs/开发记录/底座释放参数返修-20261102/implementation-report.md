# 底座释放参数返修实现

状态：`implemented_pending_compile`。作者 `c944df97-5698-4cb3-ae72-8adf3edc9b0a`，模型 grok-4.7-high-fast。本阶段额度已用完，返修结果未追加复审。

本次没有编译，也没有进游戏。不能把 2026-11-02 早些时候陨岩续飞或三分序号那两次 `compileJava` 当成这次修复的编译结果。

表作者已经停写。`fire_shock_ring` 在制作中第 16 行，`fire_meteorite` 在制作中第 17 行。本轮没有改表、资源、平衡配置、注册器或 `调度记录.md`。`CastFrame.java` 未改。`ReleaseBatch.java` 未改。写前目标文件哈希与上一轮收束一致，没有外部写入冲突。

## 四处落实

1. 父段没有接触点、也没有地表冻结点时，`BatchCastSession.spawn`（约 332–346 行）用现有 `ReleaseBatch.open(owner, skill)` 取出选点对象，立刻 `bind` 回原来的当前批次，再把这个对象传给 `commitHostGroup`。`SkillSegmentRunner.runSegment`（约 583–601、668–676 行）只把它用于 `crosshairCenter` 和 `resolveAnchor`。有 `point` 的子段接触点和地表冻结点不创建这个对象，接触原点仍覆盖生成位置。垂落继续吃这次选出来的中心，再走原有抬高和按份倾斜。没有按术式 id 分支。

2. 同一方法在 `CastFrame.forAttack` 之前（约 684–693 行）读取 `BirthLayout.Slot`。有槽时 `copyOrdinal` 写入该帧份序号，`spreadCount` 写入几何份数，0 是第一份。幻剑读帧上的生成点和份序号，不可变斩击在 `forAttack` 里按这个序号抖动，锁定实体加交错的方向也在建帧时按这个序号取样。没有出生槽时仍用调用方原来的序号和份数。

3. `BatchShockRingEffect.resolvePoint`（约 147–154 行）顺序是强制原点、各份 `frame.origin`、共享 `anchor`、施法位置、眼睛。环和伤害都用这个点。地表尘埃标记也改到同一个点（约 66–68 行）。圆环贴图、半径和高度没改。

4. `TechniqueHitSpec.adoptBirthOrdinal` 保留（调用约 350 行，方法约 643–658 行）。建帧前已经写入同一序号，这里再写一次，让后面的体积重取样和垂落斜线读到 0、1、2。它不再把 0 号采样结果改成另一个序号。没有把采样挪到序号写入之前。

## 几何批次和结算

几何对象不绑定为 `ReleaseBatch.current()`，不打开 `Tally`，不改伤害倍率、费用和结算份序号。`tally` 参数仍是 `null`。分卷标记、显式附魔报价和批次恢复都看结算批次；父段这条几何路径上结算批次是 `null`，仍走原来的 `loose-` 标记。重水分组键也用结算批次，身份哈希与原先传入 `null` 时一样。

份数没有乘开。`spawn` 仍提交 `copyCount = 1`。`commitHostGroup` 循环仍是 `copyIndex < safeCopies`（约 393 行），每个父体只进一次 `runSegment`。几何份数只进 `CastFrame`，不进宿主组循环，也不进重水计划份数。一分为三仍是三次父体出生，不会变成九次。

## 兼容与配置

没有出生槽的旧链不建几何批次，选点和绑定保持原样。已有陨岩、热波配置键和默认值没动。没有新增平衡键。陨岩命中后续飞、首命中一次、触地残留和狭窄补放没改。

## 命名门禁

命令：`powershell -NoProfile -File tools/check-name-gate.ps1`。退出码 1。本次改过的文件没有新违规。仅有的两处是未改文件 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to`。

## 写前 / 写后

| 文件 | 写前 SHA256 | 写后 SHA256 | 写后 mtime |
| --- | --- | --- | --- |
| `BatchCastSession.java` | `7b25438fdc64d15adaeae7143a2f1bbf6511feaa4b994dc52f8d470278779a88` | `e958c35c223e37fab2256f65df3c87585ebc041ac26bb15dd0d5f9529e83d44f` | 2026-11-02T03:50:53.9570425+08:00 |
| `SkillSegmentRunner.java` | `e594abfa1ca163e5f237bc379bc2fe9d8df6cc0db996a83f26178d16b74b5643` | `665f4bcbdaef562731d6d804c865bcc9935b7360cef2f9d0307864c9c85a335d` | 2026-11-02T03:51:20.4871488+08:00 |
| `BatchShockRingEffect.java` | `ba7bf93056eae3cad35bd8d61ad676c99f7aac5132690af4fb709675c904acf1` | `48af3e6b3d44222af44b4def51dfd3ff2e5e9f97f0866354387cdfdea87d71a2` | 2026-11-02T03:51:00.3990663+08:00 |
| `TechniqueHitSpec.java` | `827a767db942ce2e58dd9fade5dc77694e217e37df55cb63a6e594d4b4fb488c` | `fd70ab2637ca0b33091f0771740eeb49c85985571045a14ea7f17e57d534fd8e` | 2026-11-02T03:51:01.9955679+08:00 |
| `CastFrame.java` | `d73f7937249e81b5981903087b472469396cb50f9453680015eb1e4ab1f8b9d2` | 未改 | 2026-11-02T00:39:37.1272389+08:00 |

## 实测链

下面四行都还没进游戏，只列出编译后要看的链。

1. 落点指定 + 一分为三 + 垂落 + 陨岩：三块陨岩应出现在准星方块或准星附近实体处，并按各份抬高、倾斜后落下；落地前就能看出三份分开。未实测。
2. 落点指定 + 一分为三 + 热波：三个环应分开出现在准星落点附近，环心和伤害点相同；带强制原点时仍收成该点。未实测。
3. 落点指定 + 一分为三 + 留焰 + 幻剑：同一拍三把剑应分开，而不是叠在同一点。未实测。
4. 落点指定 + 一分为三 + 留焰 + 空间切割：同一拍三道斩击应分开。未实测。

另有一条同属第 4 项根因、本次未单列成实测行：锁定实体 + 三维交错 + 一分为三 + 陨岩，三份应从一个区域朝不同方向走出抛物线，锁定仍不开立方散布。未实测。

`writes_stopped=true`。`needs_compile=true`。`blockers=[]`。
