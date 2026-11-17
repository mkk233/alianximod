# 水幻形缺手 03c

只修了 03b 诊断类的编译错误。缺臂尚未修复，画面没有改。

`compileJava` 上次退出码 1，唯一报错是 `WaterPhantomVisualDiagnostics.java` 第 11 行找不到 `net.minecraft.util.Util`，诊断类没有生成。本仓库 1.20.1 映射源码里，这个类是 `net.minecraft.Util`，`public static long getMillis()` 仍在该类。`net.minecraft.util` 下没有 `Util`。

第 11 行导入已改为 `import net.minecraft.Util;`。三处调用仍是 `Util.getMillis()`，分别在第 133、245、367 行，没有改采集次数、间隔、字段或绘制。

名字门禁已在项目根通过，退出码 0。本次没有编译。待统一编译。未游戏实测。

本阶段额度已用完，返修结果未追加复审。
