我先读这份契约任务说明，确认授权范围后再改。第 81 行仍把雾雨维护锁成 8 点、20 tick。我先留下原文件快照，再把这条断言改成停用后的读数，生成费和其他检查不动。雾雨检查里的旧维护预期已改成停用。生成费仍锁 40，护体、屏障和场地的费用检查没动。写入已收束，此文件所有权已释放。未经新派发，不再写它。

## 精确变化

只改了 `ReleaseContainerEnergyCheck.java` 第 81 行。

原断言：

```java
check(mist.initialCost() == 40 && mist.upkeepCost() == 8 && mist.upkeepPeriod() == 20, "雾雨费用被改了");
```

现断言：

```java
check(mist.initialCost() == 40 && mist.upkeepCost() == 0 && mist.upkeepPeriod() == 0, "雾雨生成费被改或维护未停");
```

生成费仍要求 40。维护费和周期仍读已有的弃用接口，期望值改为 0，表示不再存续维护。没有新增生产 API，也没有把这一条拆成新检查。同方法里的寿命 200、盒子尺寸、接触点平移和能量缩放未改。深海重压仍只检查寿命 200 和生成费 40（113–114 行）。水环（132、143 行）、水幕（215 行）和场地的维护断言保持原样。

## 原始快照

`D:\alianximod\docs\开发记录\领域附魔底座实施-20261029\lifecycle-evidence\before\src\test\java\com\bslx\XueXiMod\shu\technique\release\ReleaseContainerEnergyCheck.java`

原文件时间 `2026-10-27 04:42:57.856`，22493 字节。清单已补进同目录 `file-list.txt`。

## 末次写入

`D:\alianximod\src\test\java\com\bslx\XueXiMod\shu\technique\release\ReleaseContainerEnergyCheck.java`，LastWriteTime `2026-10-29 22:04:11.447`。

三个生产文件本次未改，时间仍是 `DomainDefinition.java` `2026-10-29 22:00:22.612`、`DomainService.java` `2026-10-29 22:00:23.918`、`WaterReleaseDefinition.java` `2026-10-29 22:00:45.242`。

## name-gate

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次文件没有命中。未改文件只记录、未修：`OriginalHumanoid.java` 165 行 `i`、202 行 `to`。

未运行测试、编译或游戏。
