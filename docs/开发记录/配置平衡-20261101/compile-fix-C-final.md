先读编译修复任务书，按其中写明的范围处理。报错是 `settled.get()` 得到装箱的 `Double`，不能直接写成 `(float)`。先存修前字节，再只改这一处拆箱。# 配置平衡 compile-fix C 最终报告

作者 C，模型 grok-4.7-high-fast。这是统一编译唯一报错的定点修复。设计 2/2、实现 3/3 已用尽，本单不追加正确性复核，也不是正式复审。未再编译、未测试、未启动游戏、未改表、未提交。

## 具体修复

`compile-final.md` 记录 `javac` 在 `SteamBurst.java` 第 55 行失败：`settled.get()` 的类型是 `java.lang.Double`，`(float) settled.get()` 不能把装箱对象直接收成 `float`。

该行现为：

```java
float steamDamage = settled.get().floatValue();
```

`floatValue()` 先拆箱再按原 double 收成 float。后面的 `steamDamage <= 0.0f`、超限提前返回、敌对与距离检查、0 金额不调用 `hurt` 仍击退、正数金额仍走中性帧并 `hurt`，都保持原样。只改了这一处，没有动其他生产代码。

修前字节在 `docs/开发记录/配置平衡-20261101/pre-compile-fix/C/`。`pre-C/` 与 `pre-repair-1/C/` 都还在。

## 命名门禁

`tools/check-name-gate.ps1` 退出码 1。只有未改文件 `animation/runtime/OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。这两处没有改名。`SteamBurst.java` 没有新的命名问题。

## 写入与编译

唯一改动的生产文件是 `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/SteamBurst.java`。该路径写入已关闭，所有权已释放。

尚待对收束后的源码重新执行 `.\gradlew.bat compileJava --offline --no-daemon`。本单没有跑这条命令。
