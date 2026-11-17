# 作者 A 命名补修

只改了正式源码 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/ClonePositionPolicy.java`。没有写候选目录，没有改 B/C 文件，没有改 `OriginalHumanoid.java`。

## 改动

`MIN_SEPARATION` 重命名为 `MINIMUM_SEPARATION`。数值仍是 `1.0E-8D`，仍是 `private static final double`。两处：

- 第 13 行声明
- 第 46 行比较 `delta.lengthSqr() < MINIMUM_SEPARATION`

全仓 `src/main` 搜索 `MIN_SEPARATION` 只有这两处，都在这个文件里。没有跨作者引用。

`CloneService.java` 第 594 行仍是 `state.commitLine = false`，这次没有改它。

## 命名门禁

已真实执行：`powershell -NoProfile -File tools/check-name-gate.ps1`，工作目录 `D:\alianximod`。退出码 1。剩余 2 处，都是既有历史项，本单不改：

```
D:\alianximod\src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:165:18 i: 变量名不得少于 3 个字符
D:\alianximod\src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:202:17 to: 变量名不得少于 3 个字符
共 2 处
```

`ClonePositionPolicy` 的 `min` 缩写已不在结果里。不能把这次门禁说成通过。

## 未做

没有编译，没有打包，没有启动游戏，没有独立测试，没有提交，没有改表，没有正式复审。

## 停笔

作者 A 命名补修到此停止写入。待统一编译。游戏未测。
