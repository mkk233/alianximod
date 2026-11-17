先把这次授权的任务说明完整读完，再按里面的范围处理。编译失败点已经明确：`net.minecraft.util.Util` 在这份 1.20.1 映射里不存在。我先对一下本仓库实际在用的类，再做最小修正。1.20.1 映射里这个类在 `net.minecraft.Util`，`getMillis()` 还在。只改这一处导入。缺臂尚未修复。这次只改了诊断类的导入。

1.20.1 映射源码里没有 `net.minecraft.util.Util`。`getMillis()` 在 `net.minecraft.Util`。`WaterPhantomVisualDiagnostics.java` 第 11 行已改成 `import net.minecraft.Util;`。第 133、245、367 行的调用没动，采集次数、间隔和绘制也没动。

名字门禁已通过，退出码 0。本次没有编译。待统一编译。未游戏实测。

本阶段额度已用完，返修结果未追加复审。
