# 用户追加的一轮实现复核：最终原文

本轮发现 **3 项可操作问题**，均为 P2；未发现可直接确认的 P0/P1 问题。以下结论来自静态代码与原始备份对照，没有运行数值实验。

1. **[P2] 非法动态库路径会抛异常，无法按约定回退 Java。**

   位置：[ComputeSelection.java:104](D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/compute/ComputeSelection.java:104)。

   Windows 下设置 `backend=native`，将 library 写成 `C:\bad|name\xueximod_compute.dll`，`Path.of(...)` 会抛出 `InvalidPathException`。该调用位于加载异常处理之外，异常直接穿过首次轨迹、转向或走廊计算入口；`ensureResolved()` 也到不了第 30 行的 `resolved = true`，后续调用仍会重复失败。

   这不是计算业务异常，而是后端配置失败。最小修复：仅在路径解析处捕获 `InvalidPathException`，记录明确原因并固定回退 Java；不要扩大为吞掉全部运行异常。

2. **[P2] 默认 Java 路径改变了龙卷停驻阶段允许空 origin 的既有行为。**

   位置：[ComputeCalls.java:38](D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/compute/ComputeCalls.java:38)。原行为依据：[原始 TrajectorySampler.java:92](D:/alianximod/docs/开发记录/统一纯计算后端-20261030/backup-A/src/main/java/com/bslx/XueXiMod/shu/technique/release/TrajectorySampler.java:92)。

   最小输入：`trajectory=VORTEX`、`origin=null`、`forward=(0,0,1)`、`lifetimeTicks=12`、`ageTicks=8`。原实现计算 `spiralTicks=8`，进入停驻分支，返回位置为 `null`、方向为 `(0,0,1)` 的 Sample，没有解引用 origin。新实现统一检查 origin，只放行 FOLLOW_LOOK 的特殊情况，因此默认 Java 和 native 两种模式都会先抛 `NullPointerException`。

   最小修复：保留原 VORTEX 停驻分支的空位置语义，同时保持尚在旋转阶段的原异常行为；不能仅移除检查后把空位置变成 `(0,0,0)`。这是确定的公开方法行为变化；**本轮没有确认现有游戏调用会传入该空值**，不宣称已经发生游戏崩溃。

3. **[P2] Windows 构建脚本用 ASCII 写入命令文件，破坏已接受的中文路径。**

   位置：[build-windows.ps1:337](D:/alianximod/native/compute/build-windows.ps1:337)、[build-windows.ps1:407](D:/alianximod/native/compute/build-windows.ps1:407)。

   仓库、JDK 或工具安装目录包含中文时，例如 `D:\术开发\native\compute`，脚本前面的路径检查允许该路径，但生成 `_xueximod_invoke.cmd` 时使用 `-Encoding Ascii`，非 ASCII 字符被替换为 `?`。随后 cmd 执行的 `-S`、`-B`、JDK 或工具路径已经不是原路径，配置或构建失败。

   最小修复：让生成文件的编码与执行它的 cmd 代码页明确匹配并保留 Unicode，或避免通过有损编码的临时批处理传递路径。当前 `D:\alianximod` 的成功编译不受此条件影响，不能据此否定已有成功记录。

本轮同时检查了六类计算的 Java/C 公式、四个实际适配、JNI 声明与七个导出、批量长度与别名检查、数组获取失败时的释放、同输入 Java 回退、线程缓冲和互斥计数。未确认其他实质缺陷；没有把已披露的超越函数 ULP 差异直接判成错误，也没有把旧构建失败当成当前失败。

本轮未写文件，未编译、测试、加载 DLL 或启动游戏。已有 Java/C 编译通过记录不等于 JNI 运行、数值一致性或性能已经验证；Linux/macOS 构建也仍未验证。上述修复可交原固定 Cursor 作者执行并编译。

**用户追加的一轮已用完，累计实现4轮；修复后未授权再审。**
