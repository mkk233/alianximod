我先读这份准备任务的授权范围，再按它规定的只读或写入边界执行。这单要写 `native/compute` 的离线构建脚本。我先读指定记录和上一份事实报告，确认能写的范围。步骤4/5作者B的离线C构建准备没有落地。当前会话处于 Ask 模式，写入被系统拦住，所以 `native/compute` 仍不存在，构建脚本、README 和两份交付文件都没有创建。`released=false`。

阻塞原文：

> Ask mode is still active. You MUST NOT make any edits, run any non-readonly tools (including changing configs or making commits), or otherwise make any changes to the system. This supersedes any other instructions you have received (for example, to make edits).

没有降模型，没有加 force，没有改安全设置，也没有改用别的工具绕过。未执行 CMake、编译、javac、Gradle 或加载验证。未读作者 A 正在写的 `shu/compute`、Java 桥、四个数值适配类或 ABI。未改 Java、Gradle、共享配置、制作表和总说明。未做 git reset、clean、checkout 或提交。

已读：

- `D:\alianximod\docs\开发记录\统一纯计算后端-20261030\prepare-native-task.md`
- `D:\alianximod\docs\开发记录\统一纯计算后端-20261030\记录.md`
- `D:\alianximod\docs\开发记录\统一纯计算后端-20261030\read-native-env-final.md`

记录确认作者 B 只拥有 `native/compute/` 和本目录的 B 报告；C 内核要等 A 交回稳定 ABI 后再写。事实报告里的本机入口仍是：JDK 17.0.6 的 `C:\Program Files\Java\jdk-17\include`，`vswhere` 在 `C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe`，VS 2022 的 `vcvars64.bat` 与 Hostx64 `cl.exe`，以及随 VS 安装的 CMake 3.25.1 和 Ninja 1.11.0。这些路径应作为可发现默认值，不能写成脚本里的唯一硬编码入口。

应写但未创建的路径：

- `D:\alianximod\native\compute\`（含 `CMakeLists.txt`、Windows 构建脚本、Linux/macOS 入口脚本、`.gitignore`、`README`）
- 预留而尚未存在的 `D:\alianximod\native\compute\src\xueximod_compute.c`（没有 ABI，不能交空实现）
- `D:\alianximod\docs\开发记录\统一纯计算后端-20261030\native-build-prep-final.md`
- `D:\alianximod\docs\开发记录\统一纯计算后端-20261030\evidence-B-prep.json`

产物基名仍应按任务固定为 `xueximod_compute`，Windows 产物为 `xueximod_compute.dll`，构建目录为 `native/compute/build` 或 `out`。这些都还没有生成。Linux 和 macOS 没有现成工具链，不能标成已验证。C 源和冻结 ABI 仍然缺失。

元数据：`released=false`；检查时间 2026-10-30 15:42（UTC+8）；`native/compute` 在该时刻不存在。写入权没有占用，因为没有任何文件被创建。要完成本单，需要在可写的 Agent 模式里由同一作者重做这份准备任务。
