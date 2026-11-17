# 一分为三出生序号接回几何取样

status：`implemented_pending_compile`

本阶段额度已用完，返修结果未追加复审。这次作者检查不是正式复审。没有编译，没有打包，没有启动或停止游戏。

会话：`c944df97-5698-4cb3-ae72-8adf3edc9b0a`。

用户已确认这条链装了垂落。复现链是落点指定、一分为三、垂落、陨岩。三份出生仍各提交一次。取样前把出生槽的序号写回当前帧，落点立方和垂落斜线因此读到 0、1、2。没有新的散布或高度。

## 改动

只改 `TechniqueHitSpec.java`。

- 第 350 行：在涡旋相位和落点 `volumePoint` 之前调用 `adoptBirthOrdinal`。
- 第 647–658 行：有 `BirthLayout.Slot` 时，`copyIndex` 取 `copyOrdinal`，`copyCount` 取 `Math.max(1, spreadCount)`。槽不存在时帧序号不动。
- 第 357 行 `volumePoint` 和第 674–675 行 `DropTilt.placement` 都读这个帧序号。`BirthLayout.apply` 仍在第 406 行，结算序号仍是出生序号。

`copyOrdinal` 为 0 是第一份，会写入帧，不当成缺省。`spreadCount` 为 3 时帧份数是 3；小于 1 时帧份数保持 1。旧路径没有出生槽，继续用调用方原来的序号。`BatchCastSession.spawn` 仍每次提交 1 份，不会再乘成 9 份。

命中后续飞、首次结算、安放搜索、伤害和火种都没有改。

## 命名门禁

命令：`powershell -NoProfile -File tools/check-name-gate.ps1`

退出码 1。只报未改文件里的旧名字：

- `OriginalHumanoid.java:165` 变量 `i`
- `OriginalHumanoid.java:202` 变量 `to`

本次改动的 `TechniqueHitSpec.java` 没有新的命名问题，所以没有改那两处旧名字。

## 最后写入

`TechniqueHitSpec.java`：`2026-11-02T02:52:38.1607953+08:00`，SHA256 `827a767db942ce2e58dd9fade5dc77694e217e37df55cb63a6e594d4b4fb488c`，33040 字节。

未跑 `compileJava`。游戏待用户实测。本报告、`implementation-result.json` 和 `implementation-ownership.json` 写完后停止写入。
